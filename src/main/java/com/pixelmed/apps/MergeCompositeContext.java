/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.apps;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.ClinicalTrialsAttributes;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.MediaImporter;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;

import com.pixelmed.dose.CompositeInstanceContext;

import com.pixelmed.utils.MessageLogger;
import com.pixelmed.utils.PrintStreamMessageLogger;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * <p>A class containing an application for merging the composite context of multiple instances for consistency.</p>
 *
 * <p>Patient identity is determined by being within the same study or referencing each others SOP Instance UIDs.</p>
 *
 * <p>Various known dummy values are treated as if they were zero length or absent if conflicting with non-dummy values.</p>
 *
 * @author	dclunie
 */
public class MergeCompositeContext {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/apps/MergeCompositeContext.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";
	
	protected String ourAETitle = "OURAETITLE";
	
	protected String dstFolderName;
	
	public class Group extends TreeSet<String> {
		String identity;
		CompositeInstanceContext context;

		Group() {
			super();
			identity = UUID.randomUUID().toString();
			context = null;
		}
		
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Group ");
			buffer.append(identity);
			buffer.append(":\n");
			for (String sopInstanceUID : this) {
				buffer.append("\t");
				buffer.append(sopInstanceUID);
				buffer.append("\n");
			}
			buffer.append(context.toString());
			return buffer.toString();
		}
	}
	
	protected Set<Group> groups = new HashSet<Group>();
	
	
	protected String dumpGroups() {
		StringBuffer buffer = new StringBuffer();
		int count = 0;
		for (Group group : groups) {
			buffer.append(group.toString());
		}
		return buffer.toString();
	}

	//protected Map<String,CompositeInstanceContext> mapOfGroupIdentityToMergedPatientContext = new HashMap<String,CompositeInstanceContext>();
	
	protected Map<String,String> mapOfSOPInstanceUIDToStudyInstanceUID = new HashMap<String,String>();
	
	protected CompositeInstanceContext mergePatientContext(Group group,CompositeInstanceContext newContext) {
		if (group.context == null) {
//System.err.println("mergePatientContext(): creating new context for group");
			group.context = newContext; 
		}
		else {
			AttributeList groupList = group.context.getAttributeList();
			Iterator<Attribute> newListIterator = newContext.getAttributeList().values().iterator();
			while (newListIterator.hasNext()) {
				Attribute a = newListIterator.next();
				AttributeTag tag = a.getTag();
				String groupValue = Attribute.getSingleStringValueOrEmptyString(groupList,tag);
				String newValue = a.getSingleStringValueOrEmptyString();
				if (!newValue.equals(groupValue)) {
System.err.println("mergePatientContext(): for "+tag+" values differ between existing group value "+groupValue+" and new value "+newValue);
					if (groupValue.length() == 0 && newValue.length() > 0 && !newValue.equals("DUMMY")) {
System.err.println("mergePatientContext(): for "+tag+" replacing absent/empty existing group value with new value "+newValue);
						groupList.put(a);
					}
				}
			}
		}
		return group.context;
	}
	
	protected Group findGroupContainingSOPInstanceUID(String sopInstanceUID) {
		Group found = null;
		for (Group group : groups) {
			if (group.contains(sopInstanceUID)) {
				found = group;
				break;
			}
		}
		return found;
	}
	
	protected Group findGroupContainingSOPInstanceUIDWithStudyInstanceUID(String studyInstanceUID) {
		Group found = null;
		for (Group group : groups) {
			for (String sopInstanceUID : group) {
				String studyInstanceUIDForSOPInstanceUID = mapOfSOPInstanceUIDToStudyInstanceUID.get(sopInstanceUID);
				if (studyInstanceUIDForSOPInstanceUID != null && studyInstanceUIDForSOPInstanceUID.equals(studyInstanceUID)) {
					found = group;
					return found;
				}
			}
		}
		return found;
	}
	
	protected Group addToGroups(AttributeList list) throws DicomException {
		Group group = null;
		String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
		if (sopInstanceUID.length() > 0) {
//System.err.println("addToGroups(): checking "+sopInstanceUID);
			group = findGroupContainingSOPInstanceUID(sopInstanceUID);
			Set<String> referencedSOPInstanceUIDs = findAllReferencedSOPInstanceUIDs(list);
			if (group == null) {
				// not already there, so before creating a new group ...
				String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
				if (studyInstanceUID.length() > 0) {
					group = findGroupContainingSOPInstanceUIDWithStudyInstanceUID(studyInstanceUID);
					if (group == null) {
						// no group with instance in same study, so now try to put it in the same group as any referenced instances
						if (referencedSOPInstanceUIDs != null) {
							for (String referencedSOPInstanceUID : referencedSOPInstanceUIDs) {
								group = findGroupContainingSOPInstanceUID(referencedSOPInstanceUID);
								if (group != null) {
									break;
								}
							}
						}
					}
				}
				else {
					throw new DicomException("Missing StudyInstanceUID");
				}

					
				if (group == null) {				// i.e., no references or did not find any of the references in existing groups
					group = new Group();	// might as well keep sorted for dump
					groups.add(group);
				}
			}
			group.add(sopInstanceUID);
			if (referencedSOPInstanceUIDs != null) {
				group.addAll(referencedSOPInstanceUIDs);
			}
		}
		else {
			throw new DicomException("Missing SOPInstanceUID");
		}
		return group;
	}
	
	protected static Set<String> findAllReferencedSOPInstanceUIDs(AttributeList list,Set<String> setOfReferencedSOPInstanceUIDs) {
		Iterator it = list.values().iterator();
		while (it.hasNext()) {
			Attribute a = (Attribute)it.next();
			if (a != null) {
				if (a instanceof SequenceAttribute) {
					Iterator is = ((SequenceAttribute)a).iterator();
					while (is.hasNext()) {
						SequenceItem item = (SequenceItem)is.next();
						if (item != null) {
							AttributeList itemList = item.getAttributeList();
							if (itemList != null) {
								findAllReferencedSOPInstanceUIDs(itemList,setOfReferencedSOPInstanceUIDs);
							}
						}
					}
				}
				else if (a.getTag().equals(TagFromName.ReferencedSOPInstanceUID)) {
					String referencedSOPInstanceUID = a.getSingleStringValueOrEmptyString();
					if (referencedSOPInstanceUID.length() > 0) {
//System.err.println("findAllReferencedSOPInstanceUIDs(): adding "+referencedSOPInstanceUID);
						setOfReferencedSOPInstanceUIDs.add(referencedSOPInstanceUID);
					}
				}
			}
		}
		return setOfReferencedSOPInstanceUIDs;
	}
	
	protected static Set<String> findAllReferencedSOPInstanceUIDs(AttributeList list) {
		return findAllReferencedSOPInstanceUIDs(list,new HashSet<String>());
	}
	
	protected class OurFirstPassMediaImporter extends MediaImporter {
		public OurFirstPassMediaImporter(MessageLogger logger) {
			super(logger);
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			//logLn("OurFirstPassMediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				
				String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
				if (sopInstanceUID.length() > 0) {
					CompositeInstanceContext cic = new CompositeInstanceContext(list);
					// remove all except patient context ...
					cic.removeStudy();
					cic.removeSeries();
					cic.removeEquipment();
					cic.removeFrameOfReference();
					cic.removeInstance();
					cic.removeSRDocumentGeneral();

					//mapOfSOPInstanceUIDToCompositeInstanceContext.put(sopInstanceUID,cic);
									
					String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
					if (studyInstanceUID.length() > 0) {
						mapOfSOPInstanceUIDToStudyInstanceUID.put(sopInstanceUID,studyInstanceUID);
					}
					else {
						throw new DicomException("Missing StudyInstanceUID");
					}
				
					Group group = addToGroups(list);
//System.err.println("group = "+group);
					mergePatientContext(group,cic);
				}
				else {
					throw new DicomException("Missing SOPInstanceUID");
				}
			}
			catch (Exception e) {
				logLn("Error: File "+mediaFileName+" exception "+e);
			}
		}
	}
	
	
	protected class OurSecondPassMediaImporter extends MediaImporter {
		public OurSecondPassMediaImporter(MessageLogger logger) {
			super(logger);
		}
		
		protected void doSomethingWithDicomFileOnMedia(String mediaFileName) {
			//logLn("OurFirstPassMediaImporter.doSomethingWithDicomFile(): "+mediaFileName);
			try {
				DicomInputStream i = new DicomInputStream(new File(mediaFileName));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				
				String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID);
				if (sopInstanceUID.length() > 0) {
					Group group = findGroupContainingSOPInstanceUID(sopInstanceUID);
					if (group != null) {
						if (group.context != null) {
							list.putAll(group.context.getAttributeList());					// overwrite all patient context in list that was read in
						}
						else {
							throw new DicomException("Missing group context for SOPInstanceUID on second pass");	// should not be possible
						}
						
						ClinicalTrialsAttributes.addContributingEquipmentSequence(list,true,new CodedSequenceItem("109103","DCM","Modifying Equipment"),
							"PixelMed",														// Manufacturer
							"PixelMed",														// Institution Name
							"Software Development",											// Institutional Department Name
							"Bangor, PA",													// Institution Address
							null,															// Station Name
							"com.pixelmed.apps.MergeCompositeContext.main()",				// Manufacturer's Model Name
							null,															// Device Serial Number
							"Vers. 20110304",												// Software Version(s)
							"Merged patient context",
							DateTimeAttribute.getFormattedString(new java.util.Date()));
								
						list.removeGroupLengthAttributes();
						list.removeMetaInformationHeaderAttributes();
						list.remove(TagFromName.DataSetTrailingPadding);
						FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,ourAETitle);
						// no change to SOP Instance UIDs
						File dstFile = new File(dstFolderName,sopInstanceUID+".dcm");
						list.write(dstFile,TransferSyntax.ExplicitVRLittleEndian,true,true);
					}
					else {
						throw new DicomException("Missing group for SOPInstanceUID on second pass");	// should not be possible
					}
				}
				else {
					throw new DicomException("Missing SOPInstanceUID");
				}
			}
			catch (Exception e) {
				logLn("Error: File "+mediaFileName+" exception "+e);
			}
		}
	}
	
	public MergeCompositeContext(String src,String dstFolderName,MessageLogger logger) throws IOException, DicomException {
		this.dstFolderName = dstFolderName;
		MediaImporter firstPassImporter = new OurFirstPassMediaImporter(logger);
		firstPassImporter.importDicomFiles(src);
System.err.print(dumpGroups());
		MediaImporter secondPassImporter = new OurSecondPassMediaImporter(logger);
		secondPassImporter.importDicomFiles(src);

	}

	/**
	 * <p>Rotating and/or flipping a set of images and updating the other attributes accordingly.</p>
	 *
	 * @param	arg		array of 2 strings - ource folder or DICOMDIR, destination folder
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 2) {
				MessageLogger logger = new PrintStreamMessageLogger(System.err);
				new MergeCompositeContext(arg[0],arg[1],logger);
			}
			else {
				System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.apps.MergeCompositeContext srcdir|DICOMDIR dstdir");
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
	}
}


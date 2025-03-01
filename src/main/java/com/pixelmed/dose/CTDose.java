/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.pixelmed.dicom.*;

public class CTDose implements RadiationDoseStructuredReport, RadiationDoseStructuredReportFactory {
	
	protected String dlpTotal;
	protected int totalNumberOfIrradiationEvents;
	protected ScopeOfDoseAccummulation scopeOfDoseAccummulation;
	protected String scopeUID;
	protected ArrayList<CTDoseAcquisition> acquisitions;
	protected CommonDoseObserverContext observerContext;
	protected CompositeInstanceContext compositeInstanceContext;
	protected String startDateTime;
	protected String endDateTime;
	protected String description;
	protected String sourceSOPInstanceUID;	// e.g., of the RDSR file, or the dose screen file
	
	protected StructuredReport sr;
	protected AttributeList list;
	
	public RadiationDoseStructuredReport makeRadiationDoseStructuredReportInstance(StructuredReport sr) throws DicomException {
		return new CTDose(sr);
	}
	
	public RadiationDoseStructuredReport makeRadiationDoseStructuredReportInstance(StructuredReport sr,AttributeList list) throws DicomException {
		return new CTDose(sr,list);
	}

	public RadiationDoseStructuredReport makeRadiationDoseStructuredReportInstance(AttributeList list) throws DicomException {
		return new CTDose(list);
	}
	
	public CTDose(StructuredReport sr) throws DicomException {
		this.sr = sr;
		this.list = null;
		acquisitions = new ArrayList<CTDoseAcquisition>();
		parseSRContent();
	}

	public CTDose(StructuredReport sr,AttributeList list) throws DicomException {
		this.sr = sr;
		this.list = list;
		acquisitions = new ArrayList<CTDoseAcquisition>();
		parseSRContent();
	}
	
	public CTDose(AttributeList list) throws DicomException {
		this.list = list;
		this.sr = new StructuredReport(list);
		acquisitions = new ArrayList<CTDoseAcquisition>();
		parseSRContent();
	}
	
	public CTDose(String dlpTotal,int totalNumberOfIrradiationEvents,ScopeOfDoseAccummulation scopeOfDoseAccummulation,String scopeUID,String startDateTime,String endDateTime,String description) {
		this.observerContext = null;
		this.compositeInstanceContext = null;
		this.dlpTotal = dlpTotal;
		this.totalNumberOfIrradiationEvents = totalNumberOfIrradiationEvents;
		this.scopeOfDoseAccummulation = scopeOfDoseAccummulation;
		this.scopeUID = scopeUID;
		acquisitions = new ArrayList<CTDoseAcquisition>();
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.description = description;
	}
	
	public CTDose(ScopeOfDoseAccummulation scopeOfDoseAccummulation,String scopeUID,String startDateTime,String endDateTime,String description) {
		this.observerContext = null;
		this.compositeInstanceContext = null;
		this.dlpTotal = null;
		this.totalNumberOfIrradiationEvents = 0;
		this.scopeOfDoseAccummulation = scopeOfDoseAccummulation;
		this.scopeUID = scopeUID;
		acquisitions = new ArrayList<CTDoseAcquisition>();
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.description = description;
	}
	
	protected void parseSRContent() throws DicomException {
		if (sr != null) {
			ContentItem root = (ContentItem)(sr.getRoot());
			if (root != null) {
				if (root instanceof ContentItemFactory.ContainerContentItem && root.getConceptNameCodingSchemeDesignator().equals("DCM") && root.getConceptNameCodeValue().equals("113701")) {	// "X-Ray Radiation Dose Report"
					ContentItem procedureReported = root.getNamedChild("DCM","121058");
					if (procedureReported != null && procedureReported instanceof ContentItemFactory.CodeContentItem) {
						CodedSequenceItem procedureReportedCode = ((ContentItemFactory.CodeContentItem)procedureReported).getConceptCode();
						if (procedureReportedCode != null && procedureReportedCode.getCodingSchemeDesignator().equals("SRT") && procedureReportedCode.getCodeValue().equals("P5-08000")) {		// "Computed Tomography X-Ray"
							// should extract RecordingDeviceObserverContext at this point :(
							
							startDateTime = ContentItem.getSingleStringValueOrNullOfNamedChild(root,"DCM","113809");	// "Start of X-Ray Irradiation"
							endDateTime   = ContentItem.getSingleStringValueOrNullOfNamedChild(root,"DCM","113810");	// "End of X-Ray Irradiation"
							
							ContentItem ctAccumulatedDoseData =  root.getNamedChild("DCM","113811");					// "CT Accumulated Dose Data"
							if (ctAccumulatedDoseData != null && ctAccumulatedDoseData instanceof ContentItemFactory.ContainerContentItem) {
//System.err.println("CTDose.parseSRContent(): CT Accumulated Dose Data parsing");
								ContentItem ctDoseLengthProductTotal =  ctAccumulatedDoseData.getNamedChild("DCM","113813");	// "CT Dose Length Product Total"
								if (ctDoseLengthProductTotal != null && ctDoseLengthProductTotal instanceof ContentItemFactory.NumericContentItem) {
//System.err.println("CTDose.parseSRContent(): CT Dose Length Product Total parsing");
									CodedSequenceItem unit = ((ContentItemFactory.NumericContentItem)ctDoseLengthProductTotal).getUnits();
									if (CTDoseAcquisition.checkUnitIs_mGycm(unit)) {
//System.err.println("CTDose.parseSRContent(): CT Accumulated Dose Data DLP units are OK");
										dlpTotal = ((ContentItemFactory.NumericContentItem)ctDoseLengthProductTotal).getNumericValue();
									}
									else {
										System.err.println("CT Accumulated Dose Data DLP units are not mGy.cm - ignoring value");		// do not throw exception, since want to parse rest of content
									}
								}
								else {
									System.err.println("CT Accumulated Dose Data DLP not found");		// do not throw exception, since want to parse rest of content
								}
							}
							else {
								throw new DicomException("SR does not contain CT Accumulated Dose Data");
							}

							ContentItem soa = root.getNamedChild("DCM","113705");	// "Scope of Accumulation"
							if (soa != null && soa instanceof ContentItemFactory.CodeContentItem) {
								scopeOfDoseAccummulation = ScopeOfDoseAccummulation.selectFromCode(((ContentItemFactory.CodeContentItem)soa).getConceptCode());
								if (scopeOfDoseAccummulation != null) {
									CodedSequenceItem uidConcept = scopeOfDoseAccummulation.getCodedSequenceItemForUIDConcept();
									ContentItem uidItem = soa.getNamedChild(uidConcept);
									if (uidItem != null && uidItem instanceof ContentItemFactory.UIDContentItem) {
										scopeUID = ((ContentItemFactory.UIDContentItem)uidItem).getConceptValue();
									}
								}
							}
							
							{
								int n = root.getChildCount();
								for (int i=0; i<n; ++i) {
									ContentItem node = (ContentItem)(root.getChildAt(i));
									if (node != null && node.getConceptNameCodingSchemeDesignator().equals("DCM") && node.getConceptNameCodeValue().equals("113819")) {	// "CT Acquisition"
										addAcquisition(new CTDoseAcquisition(scopeUID,node));
									}
								}
							}
						}
						else {
							throw new DicomException("SR procedure reported is not CT");
						}
					}
					else {
						throw new DicomException("SR procedure reported is missing or not correctly encoded");
					}
				}
				else {
					throw new DicomException("SR document title is not X-Ray Radiation Dose Report");
				}
			}
			else {
				throw new DicomException("No SR root node");
			}
			if (list == null) {
				getAttributeList();
			}
			if (list != null) {
				description = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDescription);
				sourceSOPInstanceUID = Attribute.getSingleStringValueOrNull(list,TagFromName.SOPInstanceUID);
			}
		}
		else {
			throw new DicomException("No SR content");
		}
	}
	
	public void addAcquisition(CTDoseAcquisition acquisition) {
		acquisitions.add(acquisition);
	}
	
	public CommonDoseObserverContext getObserverContext() { return observerContext; }
	
	public void setObserverContext(CommonDoseObserverContext observerContext) { this.observerContext = observerContext; }
	
	public CompositeInstanceContext getCompositeInstanceContext() { return compositeInstanceContext; }
	
	public void setCompositeInstanceContext(CompositeInstanceContext compositeInstanceContext) { this.compositeInstanceContext = compositeInstanceContext; }
	
	public String getDLPTotal() { return dlpTotal; }
	
	public void setDLPTotal(String dlpTotal) { this.dlpTotal = dlpTotal; }
	
	public int getTotalNumberOfIrradiationEvents() { return totalNumberOfIrradiationEvents ==  0 ? acquisitions.size() : totalNumberOfIrradiationEvents; }
	
	public ScopeOfDoseAccummulation getScopeOfDoseAccummulation() { return scopeOfDoseAccummulation; }
	
	public String getScopeUID() { return scopeUID; }
	
	public int getNumberOfAcquisitions() { return acquisitions.size(); }
	
	public CTDoseAcquisition getAcquisition(int i) { return acquisitions.get(i); }
	
	public String getDLPTotalFromAcquisitions() throws NumberFormatException {
		double dlpTotalFromAcquisitions = 0;
		for (CTDoseAcquisition a : acquisitions) {
			if (a != null) {
				String aDLP = a.getDLP();
				if (aDLP != null && aDLP.length() > 0) {	// check for zero length else NumberFromatException
					try {
						dlpTotalFromAcquisitions+=Double.parseDouble(aDLP);
					}
					catch (NumberFormatException e) {
						// do nothing
					}
				}
			}
		}
		java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance());
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		formatter.setDecimalSeparatorAlwaysShown(true);		// i.e., a period even if fraction is zero
		formatter.setGroupingUsed(false);					// i.e., no comma at thousands
		String formatted = formatter.format(dlpTotalFromAcquisitions);
//System.err.println("CTDose.getDLPTotalFromAcquisitions(): returns formatted string "+formatted+" for "+Double.toString(dlpTotalFromAcquisitions));
		return formatted;
	}
	
	public boolean specifiedDLPTotalMatchesDLPTotalFromAcquisitions() {
		return (dlpTotal != null && dlpTotal.equals(getDLPTotalFromAcquisitions())) || (dlpTotal == null && getNumberOfAcquisitions() == 0);	// could check "0.00".equals()
	}
	
	public String getStartDateTime() { return startDateTime; }

	public String getEndDateTime() { return endDateTime; }

	public String getDescription() { return description; }
	
	public String getSourceSOPInstanceUID() { return sourceSOPInstanceUID; }
	
	public void setSourceSOPInstanceUID(String sourceSOPInstanceUID) { this.sourceSOPInstanceUID = sourceSOPInstanceUID; }

	public String toString() {
		return toString(true,false);
	}
	
	public String toString(boolean detail,boolean pretty) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Dose");
		{
			String patientID = "";
			String patientName = "";
			String patientSex = "";
			String patientBirthDate = "";
			String patientAge = "";
			String patientWeight = "";
			String patientSize= "";		// height
			String accessionNumber = "";
			if (compositeInstanceContext != null) {
				AttributeList contextList =  compositeInstanceContext.getAttributeList();
				patientID = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientID);
				patientName = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientName);
				patientSex = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientSex);
				patientBirthDate = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientBirthDate);
				patientAge = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientAge);
				patientWeight = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientWeight);
				patientSize = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientSize);
				accessionNumber = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.AccessionNumber);
			}
			buffer.append("\t");
			buffer.append("Patient ID=");
			buffer.append(patientID);
			buffer.append("\tName=");
			buffer.append(patientName);
			buffer.append("\tSex=");
			buffer.append(patientSex);
			buffer.append("\tDOB=");
			buffer.append(patientBirthDate);
			buffer.append("\tAge=");
			buffer.append(patientAge);
			buffer.append("\tWeight=");
			buffer.append(patientWeight);
			buffer.append(" kg");
			buffer.append("\tHeight=");
			buffer.append(patientSize);
			buffer.append(" m");

			buffer.append("\tAccession=");
			buffer.append(accessionNumber);
		}
		if (detail || startDateTime != null) {
			buffer.append("\t");
			if (!pretty) {
				buffer.append("Start=");
			}
			if (pretty && startDateTime != null && startDateTime.length() > 0) {
				try {
					java.util.Date dateTime = new java.text.SimpleDateFormat("yyyyMMddHHmmss").parse(startDateTime);
					String formattedDate = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(dateTime);
					buffer.append(formattedDate);
				}
				catch (java.text.ParseException e) {
					e.printStackTrace(System.err);
				}
			}
			else {
				buffer.append(startDateTime);
			}
		}
		if (detail && !pretty) {
			buffer.append("\tEnd=");
			buffer.append(endDateTime);
		}
		buffer.append("\t");
		if (!pretty) {
			buffer.append("Modality=");
		}
		buffer.append("CT");
		buffer.append("\t");
		if (!pretty) {
			buffer.append("Description=");
		}
		buffer.append(description);
		if (detail && !pretty) {
			buffer.append("\tScope=");
			buffer.append(scopeOfDoseAccummulation);
		}
		if (detail && !pretty) {
			buffer.append("\tUID=");
			buffer.append(scopeUID);
		}
		if (detail && !pretty) {
			buffer.append("\tEvents=");
			buffer.append(Integer.toString(getTotalNumberOfIrradiationEvents()));
		}
		buffer.append("\tDLP Total=");
		buffer.append(dlpTotal == null ? getDLPTotalFromAcquisitions() : dlpTotal);
		buffer.append(" mGy.cm");

		buffer.append("\n");

		if (detail) {
			for (int i=0; i<acquisitions.size(); ++i) {
				buffer.append(acquisitions.get(i).toString(pretty));
			}
		}
		return buffer.toString();
	}

	public static String getHTMLTableHeaderRow() {
		return	 "<tr>"
				+"<th>ID</th>"
				+"<th>Name</th>"
				+"<th>Sex</th>"
				+"<th>DOB</th>"
				+"<th>Age</th>"
				+"<th>Weight kg</th>"
				+"<th>Height m</th>"
				+"<th>Accession</th>"
				+"<th>Date</th>"
				+"<th>Modality</th>"
				+"<th>Description</th>"
				+"<th>DLP Total mGy.cm</th>"
				+"</tr>\n";
	}

	public String getHTMLTableRow(boolean detail) {
		StringBuffer buffer = new StringBuffer();
		if (detail) {
			buffer.append(getHTMLTableHeaderRow());
		}
		buffer.append("<tr>");
		{
			String patientID = "";
			String patientName = "";
			String patientSex = "";
			String patientBirthDate = "";
			String patientAge = "";
			String patientWeight = "";
			String patientSize= "";		// height
			String accessionNumber = "";
			if (compositeInstanceContext != null) {
				AttributeList contextList =  compositeInstanceContext.getAttributeList();
				patientID = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientID);
				patientName = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientName);
				patientSex = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientSex);
				patientBirthDate = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientBirthDate);
				patientAge = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientAge);
				patientWeight = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientWeight);
				patientSize = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.PatientSize);
				accessionNumber = Attribute.getSingleStringValueOrEmptyString(contextList,TagFromName.AccessionNumber);
			}
			buffer.append("<td>");
			buffer.append(patientID);
			buffer.append("</td><td>");
			buffer.append(patientName);
			buffer.append("</td><td>");
			buffer.append(patientSex);
			buffer.append("</td><td>");
			buffer.append(patientBirthDate);
			buffer.append("</td><td align=right>");
			buffer.append(patientAge);
			buffer.append("</td><td align=right>");
			buffer.append(patientWeight);
			//buffer.append(" kg");
			buffer.append("</td><td align=right>");
			buffer.append(patientSize);
			//buffer.append(" m");
			buffer.append("</td><td>");
			buffer.append(accessionNumber);
			buffer.append("</td>");
		}
		{
			buffer.append("<td>");
			String formattedDate = ""; 
			if (startDateTime != null && startDateTime.length() > 0) {
				try {
					java.util.Date dateTime = new java.text.SimpleDateFormat("yyyyMMddHHmmss").parse(startDateTime);
					formattedDate = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(dateTime);
				}
				catch (java.text.ParseException e) {
					e.printStackTrace(System.err);
				}
			}
			buffer.append(formattedDate);
			buffer.append("</td>");
		}
		buffer.append("<td>CT</td>");

		buffer.append("<td>");
		buffer.append(description);
		buffer.append("</td>");

		buffer.append("<td align=right>");
		buffer.append(dlpTotal == null ? getDLPTotalFromAcquisitions() : dlpTotal);
		//buffer.append(" mGy.cm");
		buffer.append("</td>");
		buffer.append("</tr>\n");

		if (detail && acquisitions != null) {
			buffer.append("<tr><td colspan=2></td><td colspan=10><table>");
			String header = null;
			for (int i=0; i<acquisitions.size(); ++i) {
				if (header == null) {
					header = acquisitions.get(i).getHTMLTableHeaderRow();	// regardless
				}
				else if (acquisitions.get(i).getAcquisitionParameters() != null) {
					header = acquisitions.get(i).getHTMLTableHeaderRow();	// use the longer form that includes the parameter, and can stop looking
					break;
				}
			}
			buffer.append(header);
			for (int i=0; i<acquisitions.size(); ++i) {
				buffer.append(acquisitions.get(i).getHTMLTableRow());
			}
			buffer.append("</table></td></tr>\n");
		}

		return buffer.toString();
	}

	public StructuredReport getStructuredReport() throws DicomException {
		if (sr == null) {
			ContentItemFactory cif = new ContentItemFactory();
			ContentItem root = cif.new ContainerContentItem(
				null/*no parent since root*/,null/*no relationshipType since root*/,
				new CodedSequenceItem("113701","DCM","X-Ray Radiation Dose Report"),
				true/*continuityOfContentIsSeparate*/,
				"DCMR","10011");
			ContentItem procedureReported = cif.new CodeContentItem(root,"HAS CONCEPT MOD",new CodedSequenceItem("121058","DCM","Procedure reported"),new CodedSequenceItem("P5-08000","SRT","Computed Tomography X-Ray"));
			cif.new CodeContentItem(procedureReported,"HAS CONCEPT MOD",new CodedSequenceItem("G-C0E8","SRT","Has Intent"),new CodedSequenceItem("R-408C3","SRT","Diagnostic Intent"));
		
			if (observerContext != null) {
				Map<RecordingDeviceObserverContext.Key,ContentItem> cimap = observerContext.getRecordingDeviceObserverContext().getStructuredReportFragment();
				Iterator<RecordingDeviceObserverContext.Key> i = cimap.keySet().iterator();
				while (i.hasNext()) {
					root.addChild(cimap.get(i.next()));
				}
			}
		
			if (startDateTime != null && startDateTime.trim().length() > 0) {
				cif.new DateTimeContentItem(root,"HAS OBS CONTEXT",new CodedSequenceItem("113809","DCM","Start of X-Ray Irradiation"),startDateTime);
			}
			if (endDateTime != null && endDateTime.trim().length() > 0) {
				cif.new DateTimeContentItem(root,"HAS OBS CONTEXT",new CodedSequenceItem("113810","DCM","End of X-Ray Irradiation"),endDateTime);
			}
		
			ContentItem ctAccumulatedDoseData = cif.new ContainerContentItem(root,"CONTAINS",new CodedSequenceItem("113811","DCM","CT Accumulated Dose Data"),true,"DCMR","10012");
			cif.new NumericContentItem(ctAccumulatedDoseData,"CONTAINS",new CodedSequenceItem("113812","DCM","Total Number of Irradiation Events"),Integer.toString(acquisitions.size()),new CodedSequenceItem("{events}","UCUM","1.8","events"));
			cif.new NumericContentItem(ctAccumulatedDoseData,"CONTAINS",new CodedSequenceItem("113813","DCM","CT Dose Length Product Total"),
				(dlpTotal == null ? getDLPTotalFromAcquisitions() : dlpTotal),
				new CodedSequenceItem("mGy.cm","UCUM","1.8","mGy.cm"));

			for (CTDoseAcquisition a : acquisitions) {
				if (a != null) {
					ContentItem aci = a.getStructuredReportFragment(root);
					if (aci != null && observerContext != null) {
						DeviceParticipant dp = observerContext.getDeviceParticipant();
						if (dp != null) {
							aci.addChild(dp.getStructuredReportFragment());
						}
						PersonParticipant padmin = observerContext.getPersonParticipantAdministering();
						if (padmin != null) {
							aci.addChild(padmin.getStructuredReportFragment());
						}
					}
				}
			}

			ContentItem scope = cif.new CodeContentItem(root,"HAS OBS CONTEXT",new CodedSequenceItem("113705","DCM","Scope of Accumulation"),scopeOfDoseAccummulation.getCodedSequenceItemForScopeConcept());
			cif.new UIDContentItem(scope,"HAS PROPERTIES",scopeOfDoseAccummulation.getCodedSequenceItemForUIDConcept(),scopeUID);
		
			cif.new CodeContentItem(root,"CONTAINS",new CodedSequenceItem("113854","DCM","Source of Dose Information"),new CodedSequenceItem("113856","DCM","Automated Data Collection"));
			
			if (observerContext != null) {
				PersonParticipant pauth = observerContext.getPersonParticipantAuthorizing();
				if (pauth != null) {
					root.addChild(pauth.getStructuredReportFragment());
				}
			}

			sr = new StructuredReport(root);
			list = null;	// any list previously populated is invalidated by newly generated SR tree; fluche cached version
		}
//System.err.println("CTDose.getStructuredReport():  sr =\n"+sr);
		return sr;
	}
	
	public AttributeList getAttributeList() throws DicomException {
//System.err.println("CTDose.getAttributeList(): compositeInstanceContext.getAttributeList() =\n"+(compositeInstanceContext == null ? "" : compositeInstanceContext.getAttributeList()));
		if (list == null) {
			getStructuredReport();
			list = sr.getAttributeList();
			if (compositeInstanceContext != null) {
//System.err.println("CTDose.getAttributeList(): compositeInstanceContext.getAttributeList() =\n"+compositeInstanceContext.getAttributeList());
				list.putAll(compositeInstanceContext.getAttributeList());
			}
			if (description != null && list.get(TagFromName.StudyDescription) == null) {
				Attribute a = new LongStringAttribute(TagFromName.StudyDescription); a.addValue(description); list.put(a);
			}
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SOPClassUID); a.addValue(SOPClass.XRayRadiationDoseSRStorage); list.put(a); }
			{ Attribute a = new CodeStringAttribute(TagFromName.Modality); a.addValue("SR"); list.put(a); }
		}
//System.err.println("CTDose.getStructuredReport(): AttributeList =\n"+list);
		return list;
	}
	
	public void write(String filename,String aet) throws DicomException, IOException {
		getAttributeList();
		{
			java.util.Date currentDateTime = new java.util.Date();
			{ Attribute a = new DateAttribute(TagFromName.InstanceCreationDate); a.addValue(new java.text.SimpleDateFormat("yyyyMMdd").format(currentDateTime)); list.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.InstanceCreationTime); a.addValue(new java.text.SimpleDateFormat("HHmmss.SSS").format(currentDateTime)); list.put(a); }
			{ Attribute a = new UniqueIdentifierAttribute(TagFromName.InstanceCreatorUID); a.addValue(VersionAndConstants.instanceCreatorUID); list.put(a); }
			
		}
		list.insertSuitableSpecificCharacterSetForAllStringValues();
		list.removeMetaInformationHeaderAttributes();
		FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,aet);
        list.write(filename);
	}
	
	public void write(String filename) throws DicomException, IOException {
		write(filename,"OURAETITLE");
	}

}

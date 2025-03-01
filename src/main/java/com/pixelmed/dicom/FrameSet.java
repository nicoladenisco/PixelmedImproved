/* Copyright (c) 2001-2005, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.geometry.GeometryOfSlice;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <p>A class to describe a set of frames sharing common characteristics suitable for display or analysis as an entity.</p>
 *
 * @author	dclunie
 */
class FrameSet {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/dicom/FrameSet.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";
	
	private Map sharedAttributes;
	private ArrayList perFrameAttributes;
	
	private static final String Rows = "Rows";
	private static final String Columns = "Columns";
	private static final String Modality = "Modality";
	private static final String SOPClassUID = "SOPClassUID";
	private static final String SOPInstanceUID = "SOPInstanceUID";
	private static final String SeriesInstanceUID = "SeriesInstanceUID";
	private static final String StudyInstanceUID = "StudyInstanceUID";
	private static final String FrameOfReferenceUID = "FrameOfReferenceUID";
	private static final String SliceThickness = "SliceThickness";
	private static final String BodyPartExamined = "BodyPartExamined";
	
	private static final String ImageOrientationPatientRowX = "ImageOrientationPatientRowX";
	private static final String ImageOrientationPatientRowY = "ImageOrientationPatientRowY";
	private static final String ImageOrientationPatientRowZ = "ImageOrientationPatientRowZ";
	private static final String ImageOrientationPatientColumnX = "ImageOrientationPatientColumnX";
	private static final String ImageOrientationPatientColumnY = "ImageOrientationPatientColumnY";
	private static final String ImageOrientationPatientColumnZ = "ImageOrientationPatientColumnZ";
	private static final String ImageOrientationPatientNormalX = "ImageOrientationPatientNormalX";
	private static final String ImageOrientationPatientNormalY = "ImageOrientationPatientNormalY";
	private static final String ImageOrientationPatientNormalZ = "ImageOrientationPatientNormalZ";
	private static final String ImagePositionPatientX = "ImagePositionPatientX";
	private static final String ImagePositionPatientY = "ImagePositionPatientY";
	private static final String ImagePositionPatientZ = "ImagePositionPatientZ";
	private static final String VoxelSpacingX = "VoxelSpacingX";
	private static final String VoxelSpacingY = "VoxelSpacingY";
	private static final String VoxelSpacingZ = "VoxelSpacingZ";

	
	/**
	 * <p>Extract the attributes and values that are required to be shared by all members of this frame set.</p>
	 *
	 * @param	list	a lists of DICOM attributes
	 * @return		a Map of the attributes and values required to be the same for membership in this frame set
	 */
	private static Map getSharedAttributes(AttributeList list) {
		Map map = new HashMap();
		map.put(FrameSet.Rows, new Integer(Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Rows,0)));
		map.put(FrameSet.Columns, new Integer(Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Columns,0)));
		map.put(FrameSet.Modality, Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality));
		map.put(FrameSet.SOPClassUID, Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID));
		map.put(FrameSet.SeriesInstanceUID, Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesInstanceUID));
		map.put(FrameSet.StudyInstanceUID, Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID));
		map.put(FrameSet.FrameOfReferenceUID, Attribute.getSingleStringValueOrEmptyString(list,TagFromName.FrameOfReferenceUID));
		map.put(FrameSet.BodyPartExamined, Attribute.getSingleStringValueOrEmptyString(list,TagFromName.BodyPartExamined));
		
		double[] rowArray = null;
		double[] columnArray = null;
		double[] normalArray = null;
		double[] voxelSpacingArray = null;
		double sliceThickness = 0;

		try {
			GeometryOfSliceFromAttributeList geometryOfSlice = new GeometryOfSliceFromAttributeList(list);
			rowArray = geometryOfSlice.getRowArray();
			columnArray = geometryOfSlice.getColumnArray();
			normalArray = geometryOfSlice.getNormalArray();
			voxelSpacingArray = geometryOfSlice.getVoxelSpacingArray();
			sliceThickness = geometryOfSlice.getSliceThickness();
		}
		catch (DicomException e) {	// may not be a cross-sectional image
		}
		
		if (rowArray != null && rowArray.length == 3) {
			map.put(FrameSet.ImageOrientationPatientRowX, new Double(rowArray[0]));
			map.put(FrameSet.ImageOrientationPatientRowY, new Double(rowArray[1]));
			map.put(FrameSet.ImageOrientationPatientRowZ, new Double(rowArray[2]));
		}
		if (columnArray != null && columnArray.length == 3) {
			map.put(FrameSet.ImageOrientationPatientColumnX, new Double(columnArray[0]));
			map.put(FrameSet.ImageOrientationPatientColumnY, new Double(columnArray[1]));
			map.put(FrameSet.ImageOrientationPatientColumnZ, new Double(columnArray[2]));
		}
		// don't really need to compare normal array, but useful to have around for later
		if (normalArray != null && normalArray.length == 3) {
			map.put(FrameSet.ImageOrientationPatientNormalX, new Double(normalArray[0]));
			map.put(FrameSet.ImageOrientationPatientNormalY, new Double(normalArray[1]));
			map.put(FrameSet.ImageOrientationPatientNormalZ, new Double(normalArray[2]));
		}
		if (voxelSpacingArray != null && voxelSpacingArray.length == 3) {
			map.put(FrameSet.VoxelSpacingX, new Double(voxelSpacingArray[0]));
			map.put(FrameSet.VoxelSpacingY, new Double(voxelSpacingArray[1]));
			map.put(FrameSet.VoxelSpacingZ, new Double(voxelSpacingArray[2]));
		}
		
		map.put(FrameSet.SliceThickness, new Double(sliceThickness));

		return map;
	}
		
	/**
	 * <p>Extract the attributes and values that are expected to be different for all members of this frame set.</p>
	 *
	 * @param	list	a lists of DICOM attributes
	 * @return		a Map of the attributes and values that are expected to be different for each member of this frame set
	 */
	private static Map getPerFrameAttributes(AttributeList list) {
		Map map = new HashMap();
		map.put(FrameSet.SOPInstanceUID, Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID));

		double[] tlhcArray = null;

		try {
			GeometryOfSliceFromAttributeList geometryOfSlice = new GeometryOfSliceFromAttributeList(list);
			tlhcArray = geometryOfSlice.getTLHCArray();
		}
		catch (DicomException e) {	// may not be a cross-sectional image
		}
		
		if (tlhcArray != null && tlhcArray.length == 3) {
			map.put(FrameSet.ImagePositionPatientX, new Double(tlhcArray[0]));
			map.put(FrameSet.ImagePositionPatientY, new Double(tlhcArray[1]));
			map.put(FrameSet.ImagePositionPatientZ, new Double(tlhcArray[2]));
		}
		return map;
	}
		
	/**
	 * <p>Check to see if a single or multi-frame object is a potential member of the current frame set.</p>
	 *
	 * @param	list	a lists of DICOM attributes for the object to be checked
	 * @return		true if the attribute list matches the criteria for membership in this frame set
	 */
	boolean eligible(AttributeList list) {
		Map tryMap = getSharedAttributes(list);
		return tryMap.equals(sharedAttributes);
	}
	
	/**
	 * <p>Insert the single or multi-frame object into the current frame set.</p>
	 *
	 * <p>It is assumed that the object has already been determined to be eligible.</p>
	 *
	 * @param	list	a lists of DICOM attributes for the object to be inserted
	 */
	void insert(AttributeList list) {
		perFrameAttributes.add(getPerFrameAttributes(list));
	}
	
	/**
	 * <p>Create a new frame set using the single or multi-frame object.</p>
	 *
	 * @param	list	a lists of DICOM attributes for the object from which the frame set is to be created
	 */
	FrameSet(AttributeList list) {
		sharedAttributes = getSharedAttributes(list);
		perFrameAttributes = new ArrayList();
		insert(list);
	}
	
	/**
	 * <p>Return a String representing a Map.Entry's value.</p>
	 *
	 * @param	entry	a key-value pair from a Map
	 * @return	a string representation of the value of this object
	 */
	private static String toString(Map.Entry entry) {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append((String)(entry.getKey()));
		strbuf.append(" = ");
		strbuf.append(entry.getValue().toString());
		return strbuf.toString();
	}
	
	/**
	 * <p>Return a String representing this object's value.</p>
	 *
	 * @return	a string representation of the value of this object
	 */
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		if (sharedAttributes != null) {
			strbuf.append("\tShared:\n");
			Set set = sharedAttributes.entrySet();
			Iterator i = set.iterator();
			while (i.hasNext()) {
				strbuf.append("\t\t");
				strbuf.append(toString((Map.Entry)(i.next())));
				strbuf.append("\n");
			}
		}
		if (perFrameAttributes != null) {
			int j = 0;
			Iterator f = perFrameAttributes.iterator();
			while (f.hasNext()) {
				strbuf.append("\tFrame [");
				strbuf.append(Integer.toString(j));
				strbuf.append("]:\n");
				Map map = (Map)(f.next());
				if (map != null) {
					Set set = map.entrySet();
					Iterator i = set.iterator();
					while (i.hasNext()) {
						strbuf.append("\t\t\t");
						strbuf.append(toString((Map.Entry)(i.next())));
						strbuf.append("\n");
					}
				}
				++j;
			}
		}
		return strbuf.toString();
	}
}


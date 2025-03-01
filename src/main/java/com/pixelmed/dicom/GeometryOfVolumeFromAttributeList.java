/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import javax.vecmath.*;
import com.pixelmed.geometry.*;

/**
 * <p>A class to extract and describe the spatial geometry of an entire volume of contiguous cross-sectional image slices, given a list of DICOM attributes.</p>
 *
 * @author	dclunie
 */
public class GeometryOfVolumeFromAttributeList extends GeometryOfVolume {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/dicom/GeometryOfVolumeFromAttributeList.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";

	/**
	 * <p>Construct the geometry from the Per-frame and Shared Functional Group Sequences
	 * of a multi-frame object, or from the Image Plane Module and related attributes,
	 * if there is only a single frame of a non-multi-frame object.</p>
	 *
	 * @param	list	the list of DICOM attributes
	 */
	public GeometryOfVolumeFromAttributeList(AttributeList list) throws DicomException {
//System.err.println("GeometryOfVolumeFromAttributeList:");
		frames=null;
		isVolume=false;
		
		int rows = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Rows,0);
		int columns = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Columns,0);
		
		SequenceAttribute sharedFunctionalGroupsSequence = (SequenceAttribute)(list.get(TagFromName.SharedFunctionalGroupsSequence));
		SequenceAttribute perFrameFunctionalGroupsSequence = (SequenceAttribute)(list.get(TagFromName.PerFrameFunctionalGroupsSequence));
		int numberOfFrames = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,1);
		if (numberOfFrames == 1 && sharedFunctionalGroupsSequence == null && perFrameFunctionalGroupsSequence == null && list.containsKey(TagFromName.ImagePositionPatient)) {
//System.err.println("GeometryOfVolumeFromAttributeList: single frame with no functional groups and ImagePositionPatient");
			// possibly old-fashioned single frame DICOM image
			GeometryOfSlice frame = null;
			try {
				frame = new GeometryOfSliceFromAttributeList(list);
			}
			catch (Exception e) {
				// don't print exception, because it is legitimate for (some or all) images to be missing this information
				//e.printStackTrace(System.err);
				frame=null;
			}
			if (frame != null) {
				frames = new GeometryOfSlice[1];
				frames[0] = frame;
			}
		}
		else if (numberOfFrames > 0 && sharedFunctionalGroupsSequence != null && perFrameFunctionalGroupsSequence != null) {
//System.err.println("GeometryOfVolumeFromAttributeList: multi frame with functional groups");
			SequenceAttribute sharedPlaneOrientationSequence = (SequenceAttribute)(SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(
					sharedFunctionalGroupsSequence,TagFromName.PlaneOrientationSequence));
			SequenceAttribute sharedPlanePositionSequence = (SequenceAttribute)(SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(
					sharedFunctionalGroupsSequence,TagFromName.PlanePositionSequence));
			SequenceAttribute sharedPixelMeasuresSequence = (SequenceAttribute)(SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(
					sharedFunctionalGroupsSequence,TagFromName.PixelMeasuresSequence));
			
			frames = new GeometryOfSlice[numberOfFrames];
			for (int i=0; i<numberOfFrames; ++i) {
				Attribute aImageOrientationPatient = null;
				SequenceAttribute usePlaneOrientationSequence = sharedPlaneOrientationSequence;
				if (usePlaneOrientationSequence == null) {
					usePlaneOrientationSequence = (SequenceAttribute)(
						perFrameFunctionalGroupsSequence.getItem(i).getAttributeList().get(TagFromName.PlaneOrientationSequence));
				}
				if (usePlaneOrientationSequence != null) {
					aImageOrientationPatient = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(
						usePlaneOrientationSequence,TagFromName.ImageOrientationPatient);
//System.err.println("GeometryOfVolumeFromAttributeList: "+aImageOrientationPatient);
				}
				
				Attribute aImagePositionPatient = null;
				SequenceAttribute usePlanePositionSequence = sharedPlanePositionSequence;
				if (usePlanePositionSequence == null) {
					usePlanePositionSequence = (SequenceAttribute)(
						perFrameFunctionalGroupsSequence.getItem(i).getAttributeList().get(TagFromName.PlanePositionSequence));
				}
				if (usePlanePositionSequence != null) {
					aImagePositionPatient = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(
						usePlanePositionSequence,TagFromName.ImagePositionPatient);
//System.err.println("GeometryOfVolumeFromAttributeList: "+aImagePositionPatient);
				}
				
				Attribute aPixelSpacing = null;
				Attribute aSliceThickness = null;
				SequenceAttribute usePixelMeasuresSequence = sharedPixelMeasuresSequence;
				if (usePixelMeasuresSequence == null) {
					usePixelMeasuresSequence = (SequenceAttribute)(
						perFrameFunctionalGroupsSequence.getItem(i).getAttributeList().get(TagFromName.PixelMeasuresSequence));
				}
				if (usePixelMeasuresSequence != null) {
					aPixelSpacing = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(
						usePixelMeasuresSequence,TagFromName.PixelSpacing);
					aSliceThickness = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(
						usePixelMeasuresSequence,TagFromName.SliceThickness);
//System.err.println("GeometryOfVolumeFromAttributeList: "+aPixelSpacing);
//System.err.println("GeometryOfVolumeFromAttributeList: "+aSliceThickness);
				}

				if (aImagePositionPatient != null && aPixelSpacing != null && aImageOrientationPatient != null) {
					double[]               tlhc = aImagePositionPatient.getDoubleValues();
				
					double [] pixelSpacingArray = aPixelSpacing.getDoubleValues();
					double [] voxelSpacingArray = new double[3];
					       voxelSpacingArray[0] = pixelSpacingArray[0];
					       voxelSpacingArray[1] = pixelSpacingArray[1];
					       voxelSpacingArray[2] = 0;	// set later by checkAndSetVolumeSampledRegularlyAlongFrameDimension() IFF a volume
					double       sliceThickness = (aSliceThickness == null ? 0.0 : aSliceThickness.getSingleDoubleValueOrDefault(0.0));

					double[]        orientation = aImageOrientationPatient.getDoubleValues();
					double[]                row = new double[3];    row[0]=orientation[0];    row[1]=orientation[1];    row[2]=orientation[2];
					double[]             column = new double[3]; column[0]=orientation[3]; column[1]=orientation[4]; column[2]=orientation[5];
				
					double[]         dimensions = new double[3];
						      dimensions[0] = rows;
						      dimensions[1] = columns;
						      dimensions[2] = 1;

					frames[i] =  new GeometryOfSlice(row,column,tlhc,voxelSpacingArray,sliceThickness,dimensions);
				}
				else {
					//frames[i] = null;
					frames = null;		// abandon effort to extract volume geometry if all frames can't be used
					break;
				}
			}
		}
		checkAndSetVolumeSampledRegularlyAlongFrameDimension();
	}

	/**
	 * <p>Retrieve the ImageOrientationPatient values if the same for all frames or a single frame conventional object.</p>
	 *
	 * @param	list	the top level attribute list for the object
	 * @return		a double array of six values, or null if not present or not shared
	 */
	public static double[] getImageOrientationPatientFromAttributeList(AttributeList list) {
//System.err.println("GeometryOfVolumeFromAttributeList.getImageOrientationPatientFromAttributeList():");
		double[] vImageOrientationPatient = null;
		try {
			Attribute aImageOrientationPatient = null;
			SequenceAttribute sharedFunctionalGroupsSequence = (SequenceAttribute)(list.get(TagFromName.SharedFunctionalGroupsSequence));
			int numberOfFrames = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.NumberOfFrames,0);
			if (numberOfFrames == 1 && sharedFunctionalGroupsSequence == null) {
				// possibly old-fashioned single frame DICOM image
				aImageOrientationPatient=list.get(TagFromName.ImageOrientationPatient);
			}
			else if (numberOfFrames > 0 && sharedFunctionalGroupsSequence != null) {
				SequenceAttribute sharedPlaneOrientationSequence = (SequenceAttribute)(SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(
						sharedFunctionalGroupsSequence,TagFromName.PlaneOrientationSequence));
				if (sharedPlaneOrientationSequence != null) {
					aImageOrientationPatient = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(
						sharedPlaneOrientationSequence,TagFromName.ImageOrientationPatient);
				}
			}
//System.err.println("GeometryOfVolumeFromAttributeList.getImageOrientationPatientFromAttributeList(): "+aImageOrientationPatient);
			if (aImageOrientationPatient != null) {
				vImageOrientationPatient = aImageOrientationPatient.getDoubleValues();
			}
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);
		}
		return vImageOrientationPatient;
	}
}

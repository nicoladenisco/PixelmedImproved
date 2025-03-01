/* Copyright (c) 2001-2009, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;
import java.text.SimpleDateFormat;
import com.pixelmed.utils.FloatFormatter;

/**
 * <p>A transformation constructed from a DICOM attribute list that extracts
 * those attributes which describe how stored pixel values are translated
 * into PET SUV values.</p>
 *
 * @author	dclunie
 */
public class SUVTransform {

	/***/
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/dicom/SUVTransform.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
	
	private static long getTimeInMilliSecondsSinceEpoch(String dateTime) {
		long time;
		try {
			time = dateFormat.parse(dateTime).getTime();
		}
		catch (java.text.ParseException e) {
			time = 0;
		}
		return time;
	}
	
	private static long getTimeInMilliSecondsSinceEpoch(AttributeList list,AttributeTag dateTag,AttributeTag timeTag) {
		return getTimeInMilliSecondsSinceEpoch(Attribute.getSingleStringValueOrEmptyString(list,dateTag) + Attribute.getSingleStringValueOrEmptyString(list,timeTag));
	}
		
	/***/
	private class SingleSUVTransform {
	
		/***/
		double rescaleIntercept;
		/***/
		boolean haveSUVbw;
		/***/
		double scaleFactorSUVbw;
		/***/
		String unitsSUVbw;
		
		/**
		 * @param	list		PixelValueTransformationSequence item attributes
		 */
		SingleSUVTransform(AttributeList list) {
			if (list != null) {
				String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
				if (sopClassUID.equals(SOPClass.PETImageStorage)) {
//System.err.println("have PET SOP Class");
					String correctedImage = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.CorrectedImage);
					if (correctedImage.contains("ATTN") && correctedImage.contains("DECY")) {
						String units = Attribute.getSingleStringValueOrNull(list,TagFromName.Units);
						double rescaleSlope = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.RescaleSlope,1.0);
//System.err.println("rescaleSlope = "+rescaleSlope);
						rescaleIntercept = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.RescaleIntercept,0.0);		// should be zero for PET
//System.err.println("rescaleIntercept = "+rescaleIntercept);
						if (units.equals("BQML")) {
//System.err.println("have units BQML");
							String decayCorrection = Attribute.getSingleStringValueOrNull(list,TagFromName.DecayCorrection);
//System.err.println("decayCorrection = "+decayCorrection);
							double weight = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.PatientWeight,0.0); // in kg
//System.err.println("weight = "+weight+" kg");
							Attribute aInjectedDose = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(list,TagFromName.RadiopharmaceuticalInformationSequence,TagFromName.RadionuclideTotalDose);
//System.err.println("have injected dose = "+aInjectedDose);
							Attribute aHalfLife = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(list,TagFromName.RadiopharmaceuticalInformationSequence,TagFromName.RadionuclideHalfLife);
//System.err.println("have half life = "+aHalfLife);
							Attribute aStartTime = SequenceAttribute.getNamedAttributeFromWithinSequenceWithSingleItem(list,TagFromName.RadiopharmaceuticalInformationSequence,TagFromName.RadiopharmaceuticalStartTime);
//System.err.println("have start time = "+aStartTime);

							String sSeriesDate = Attribute.getSingleStringValueOrNull(list,TagFromName.SeriesDate);
							long seriesDateTime = getTimeInMilliSecondsSinceEpoch(list,TagFromName.SeriesDate,TagFromName.SeriesTime);
							long acquisitionDateTime = getTimeInMilliSecondsSinceEpoch(list,TagFromName.AcquisitionDate,TagFromName.AcquisitionTime);
							long scanDateTime = seriesDateTime;
//System.err.println("scanDateTime = "+scanDateTime+" mS");
							String sScanDate = sSeriesDate;	// start Date is not explicit … assume same as Series Date; but consider spanning midnight
							if (seriesDateTime > acquisitionDateTime) {
//System.err.println("have series date time after acquisition date time");
								// per GE docs, may have been updated during post-processing into new series
								String privateCreator = Attribute.getSingleStringValueOrEmptyString(list,new AttributeTag(0x0009,0x0010)).trim();
								String privateScanDateTime = Attribute.getSingleStringValueOrNull(list,new AttributeTag(0x0009,0x100d));
								if (privateCreator.equals("GEMS_PETD_01") && privateScanDateTime != null) {
//System.err.println("use GE private scan date time");
									scanDateTime = getTimeInMilliSecondsSinceEpoch(privateScanDateTime);
									sScanDate = privateScanDateTime.substring(0,8);
								}
								else {
									scanDateTime = 0;	// cannot proceed
									sScanDate = null;
								}
							}
						
							if (decayCorrection.equals("START") && aInjectedDose != null && aHalfLife != null && aStartTime != null && sScanDate != null && scanDateTime != 0 && weight != 0) {
//System.err.println("have all we need");
								long startDateTime = getTimeInMilliSecondsSinceEpoch(sScanDate+aStartTime.getSingleStringValueOrEmptyString());
//System.err.println("startDateTime = "+startDateTime+" mS");
								{
									double decayTime = (scanDateTime - startDateTime) / 1000.0;	// seconds
//System.err.println("decayTime = "+decayTime+" secs");
									double halfLife = aHalfLife.getSingleDoubleValueOrDefault(0.0);
//System.err.println("halfLife = "+halfLife+" secs");
									double injectedDose = aInjectedDose.getSingleDoubleValueOrDefault(0.0);
//System.err.println("injectedDose = "+injectedDose+" Bq");
									double decayedDose = injectedDose * Math.pow(2, -decayTime / halfLife);
//System.err.println("decayedDose = "+decayedDose);
									scaleFactorSUVbw = (weight * 1000 / decayedDose);
//System.err.println("scaleFactorSUVbw (before including rescaleSlope) = "+scaleFactorSUVbw);
									scaleFactorSUVbw = scaleFactorSUVbw * rescaleSlope;
//System.err.println("scaleFactorSUVbw (including rescaleSlope) = "+scaleFactorSUVbw);
									haveSUVbw = true;
									unitsSUVbw = "g/ml";
								}
							}
						}
						else if (units.equals("CNTS")) {
//System.err.println("have units CNTS");
							String privateCreator = Attribute.getSingleStringValueOrEmptyString(list,new AttributeTag(0x7053,0x0010)).trim();
//System.err.println("privateCreator = "+privateCreator);
							double privateSUVbwsScaleFactor = Attribute.getSingleDoubleValueOrDefault(list,new AttributeTag(0x7053,0x1000),0.0);
//System.err.println("privateSUVbwsScaleFactor = "+privateSUVbwsScaleFactor);
							if (privateCreator.equals("Philips PET Private Group") && privateSUVbwsScaleFactor != 0.0) {
//System.err.println("scaleFactorSUVbw (before including rescaleSlope) (Philips private) = "+privateSUVbwsScaleFactor);
								scaleFactorSUVbw = privateSUVbwsScaleFactor * rescaleSlope;
//System.err.println("scaleFactorSUVbw (including rescaleSlope) = "+scaleFactorSUVbw);
								haveSUVbw = true;
								unitsSUVbw = "g/ml";
							}
							// could also check for presence of (0x7053,0x1009) scale factor to Bq/ml, and run as if Units were BQML :(
						}
						else if (units.equals("GML")) {
							scaleFactorSUVbw = rescaleSlope;
							haveSUVbw = true;
							unitsSUVbw = "g/ml";
						}
						
						{
						}
					}
				}
			}
		}
		
		boolean isValidSUVbw() {
			return haveSUVbw;
		}
		
		double getSUVbwValue(int storedValue) {
			return (storedValue + rescaleIntercept) * scaleFactorSUVbw;		// rescale intercept should always be zero, but just in case.
		}
		
		String getSUVbwUnits() {
			return unitsSUVbw;
		}
	}
	

	private SingleSUVTransform useTransform;
	
	/**
	 * @param	list	the dataset of an image object to be searched for transformations
	 */
	public SUVTransform(AttributeList list) {
//System.err.println("SUVTransform:");
		useTransform = new SingleSUVTransform(list);
	}

	private final int precisionToDisplayDouble = 4;
	private final int maximumIntegerDigits = 8;
	private final int maximumMaximumFractionDigits = 6;

	/**
	 * Given a stored pixel value, return a string containing a description of all
	 * known SUV that can be derived from it.
	 *
	 * @param	frame		numbered from zero; needed to select which transform if frame-specific
	 * @param	storedValue	the actual stored pixel value to look up
	 */
	public String toString(int frame,int storedValue) {
		StringBuffer sbuf = new StringBuffer();
		SingleSUVTransform t = useTransform;
		if (t.isValidSUVbw()) {
			sbuf.append("SUVbw = ");
			double value=t.getSUVbwValue(storedValue);
			sbuf.append(FloatFormatter.toString(value));
			sbuf.append(" ");
			sbuf.append(t.getSUVbwUnits());
		}
		return sbuf.toString();
	}
}


/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.anatproc.CTAnatomy;
import com.pixelmed.anatproc.DisplayableAnatomicConcept;

import com.pixelmed.dicom.*;

import com.pixelmed.doseocr.ExposureDoseSequence;
import com.pixelmed.doseocr.OCR;

import com.pixelmed.utils.FileUtilities;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

public class CTIrradiationEventDataFromImages {

	private UIDGenerator u = new UIDGenerator();
	
	private ArrayList<String> doseScreenFilenames = new ArrayList<String>();
	
	private ArrayList<String> doseStructuredReportFilenames = new ArrayList<String>();
	
	public ArrayList<String> getDoseScreenOrStructuredReportFilenames() {
		return getDoseScreenOrStructuredReportFilenames(true,true);
	}
	
	public ArrayList<String> getDoseScreenOrStructuredReportFilenames(boolean includeScreen,boolean includeSR) {
		ArrayList<String> doseScreenOrStructuredReportFilenames;
		if (includeScreen && includeSR) {
			doseScreenOrStructuredReportFilenames = new ArrayList<String>(doseScreenFilenames);
			doseScreenOrStructuredReportFilenames.addAll(doseStructuredReportFilenames);
		}
		else if (includeScreen) {
			doseScreenOrStructuredReportFilenames = doseScreenFilenames;
		}
		else if (includeSR) {
			doseScreenOrStructuredReportFilenames = doseStructuredReportFilenames;
		}
		else {
			doseScreenOrStructuredReportFilenames = new ArrayList<String>();
		}
		return doseScreenOrStructuredReportFilenames;
	}
	
	private ArrayList<Slice> slices = new ArrayList<Slice>();
	
	boolean organized = false;
	boolean extracted = false;

	//private Map<String,String> generatedIrradiationEventUIDByAcquisitionNumberAndStudyInstanceUID = new TreeMap<String,String>();
	private Map<String,String> generatedIrradiationEventUIDByAcquisitionTimeAndSeriesNumberAndStudyInstanceUID = new TreeMap<String,String>();
		
	//private Set<String> irradiationEventUIDs = new TreeSet<String>();
	
	private Map<String,List<Slice>> slicesByIrradiationEventUID = new HashMap<String,List<Slice>>();
	
	private String patientAge;
	private boolean patientAgeIsClean = true;
	public String getPatientAge() { return patientAgeIsClean ? patientAge : null; }
	
	private String patientSex;
	private boolean patientSexIsClean = true;
	public String getPatientSex() { return patientSexIsClean ? patientSex : null; }
	
	private String patientWeight;
	private boolean patientWeightIsClean = true;
	public String getPatientWeight() { return patientWeightIsClean ? patientWeight : null; }
	
	private String patientSize;
	private boolean patientSizeIsClean = true;
	public String getPatientSize() { return patientSizeIsClean ? patientSize : null; }
	
	private Map<String,String>  studyInstanceUIDByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> studyInstanceUIDByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  imageTypeByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> imageTypeByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  acquisitionNumberByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> acquisitionNumberByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  seriesNumberByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> seriesNumberByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  seriesDescriptionByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> seriesDescriptionByEventIsClean = new TreeMap<String,Boolean>();
		
	private Map<String,String>  imageTypeValue3ByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> imageTypeValue3ByEventIsClean = new TreeMap<String,Boolean>();
		
	private Map<String,String>  exposureTimeByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> exposureTimeByEventIsClean = new TreeMap<String,Boolean>();

	private Map<String,String>  kvpByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> kvpByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  tubeCurrentByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> tubeCurrentByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,Double> tubeCurrentTotalByEvent = new TreeMap<String,Double>();
	private Map<String,Double> tubeCurrentCountByEvent = new TreeMap<String,Double>();
	private Map<String,Double> tubeCurrentMaximumByEvent = new TreeMap<String,Double>();
	
	private Map<String,Double> midScanTimeCountByEvent = new TreeMap<String,Double>();
	private Map<String,Double> midScanTimeMinimumByEvent = new TreeMap<String,Double>();
	private Map<String,Double> midScanTimeMaximumByEvent = new TreeMap<String,Double>();
	
	private Map<String,String>  exposureTimePerRotationByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> exposureTimePerRotationByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  nominalSingleCollimationWidthInMMByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> nominalSingleCollimationWidthInMMByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  nominalTotalCollimationWidthInMMByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> nominalTotalCollimationWidthInMMByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String>  pitchFactorByEvent = new TreeMap<String,String>();
	private Map<String,Boolean> pitchFactorByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,CodedSequenceItem> anatomyByEvent = new TreeMap<String,CodedSequenceItem>();
	private Map<String,Boolean> anatomyByEventIsClean = new TreeMap<String,Boolean>();
	
	private Map<String,String> startAcquisitionDateTimeByEvent = new TreeMap<String,String>();
	private Map<String,String> endAcquisitionDateTimeByEvent = new TreeMap<String,String>();
	
	private Map<String,Double> lowestSliceLocationByEvent = new TreeMap<String,Double>();
	private Map<String,Double> highestSliceLocationByEvent = new TreeMap<String,Double>();
	
	private Map<String,CTAcquisitionParameters> acquisitionParametersBySeriesNumberAndScanRangeAndStudyInstanceUIDKey = null;
	private Map<String,CTAcquisitionParameters> acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey = null;

	private Map<String,String> overallEarliestAcquisitionDateTimeByStudy = new TreeMap<String,String>();
	private Map<String,String> overallLatestAcquisitionDateTimeByStudy   = new TreeMap<String,String>();
	
	public String getOverallEarliestAcquisitionDateTimeForStudy(String studyInstanceUID) { return overallEarliestAcquisitionDateTimeByStudy.get(studyInstanceUID); }
	public String getOverallLatestAcquisitionDateTimeForStudy(String studyInstanceUID)   { return overallLatestAcquisitionDateTimeByStudy.get(studyInstanceUID); }
	
	private static void putCodedSequenceItemByStringIndexIfNotDifferentElseFlagAsUnclean(Map<String,CodedSequenceItem> map,Map<String,Boolean> mapIsCleanForThisKey,String key,CodedSequenceItem newValue) {
//System.err.println("CTIrradiationEventDataFromImages.putCodedSequenceItemByStringIndexIfNotDifferentElseFlagAsUnclean(): newValue="+newValue);
		if (newValue != null) {
			Boolean clean = mapIsCleanForThisKey.get(key);
			if (clean == null) {
				clean = Boolean.valueOf(true);				// not new Boolean(true) ... see javadoc
				mapIsCleanForThisKey.put(key,clean);
			}
			CodedSequenceItem existingValue = map.get(key);
			if (existingValue == null) {
				map.put(key,newValue);
				// leave clean alone ... will either have been just added as new, or will already be true and replacing existing empty value with a non-empty value is still true
			}
			else {
				// already there
				if (!existingValue.equals(newValue)) {
//System.err.println("CTIrradiationEventDataFromImages.putCodedSequenceItemByStringIndexIfNotDifferentElseFlagAsUnclean(): different values newValue="+newValue+" existing value="+existingValue+" for key="+key);
					clean = Boolean.valueOf(false);			// not new Boolean(false) ... see javadoc
					mapIsCleanForThisKey.put(key,clean);	// replace old one, can't change the value of a Boolean
				}
				// else do nothing ... is same so OK
			}
		}
		// else do nothing ... pretend we never saw it if no value
	}

	//private static void putNumericStringValueByStringIndexInStringMapIfNumericSortIsEarlier(Map<String,String> map,String key,String newValueString) {
	//	if (newValueString != null && !newValueString.equals("")) {
	//		try {
	//			Double newValue = new Double(newValueString);
	//			String existingValueString = map.get(key);
	//			if (existingValueString == null) {
	//				map.put(key,newValueString);
	//			}
	//			else {
	//				Double existingValue = new Double(existingValueString);
	//				if (newValue.compareTo(existingValue) < 0) {
	//					map.put(key,newValueString);
	//				}
	//			}
	//		}
	//		catch (NumberFormatException e) {
	//			// do nothing
	//		}
	//	}
	//}
	
	private static void putNumericStringValueByStringIndexIfNumericSortIsEarlier(Map<String,Double> map,String key,String newValueString) {
		if (newValueString != null && !newValueString.equals("")) {
			try {
				Double newValue = new Double(newValueString);
				Double existingValue = map.get(key);
				if (existingValue == null) {
					map.put(key,newValue);
				}
				else {
					if (newValue.compareTo(existingValue) < 0) {
						map.put(key,newValue);
					}
				}
			}
			catch (NumberFormatException e) {
				// do nothing
			}
		}
	}
	
	private static void putNumericStringValueByStringIndexIfNumericSortIsLater(Map<String,Double> map,String key,String newValueString) {
		if (newValueString != null && !newValueString.equals("")) {
			try {
				Double newValue = new Double(newValueString);
				Double existingValue = map.get(key);
				if (existingValue == null) {
					map.put(key,newValue);
				}
				else {
					if (newValue.compareTo(existingValue) > 0) {
						map.put(key,newValue);
					}
				}
			}
			catch (NumberFormatException e) {
				// do nothing
			}
		}
	}
	
	private static void putStringValueByStringIndexIfLexicographicSortIsEarlier(Map<String,String> map,String key,String newValue) {
		if (newValue != null && !newValue.equals("")) {
			String existingValue = map.get(key);
			if (existingValue == null || existingValue.equals("")) {
				map.put(key,newValue);
			}
			else {
				if (newValue.compareTo(existingValue) < 0) {
					map.put(key,newValue);
				}
			}
		}
	}
	
	private static void putStringValueByStringIndexIfLexicographicSortIsLater(Map<String,String> map,String key,String newValue) {
		if (newValue != null && !newValue.equals("")) {
			String existingValue = map.get(key);
			if (existingValue == null || existingValue.equals("")) {
				map.put(key,newValue);
			}
			else {
				if (newValue.compareTo(existingValue) > 0) {
					map.put(key,newValue);
				}
			}
		}
	}
	
	private static void putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean(Map<String,String> map,Map<String,Boolean> mapIsCleanForThisKey,String key,String newValue) {
//System.err.println("CTIrradiationEventDataFromImages.putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean(): newValue="+newValue);
		if (newValue != null && !newValue.equals("")) {
			Boolean clean = mapIsCleanForThisKey.get(key);
			if (clean == null) {
				clean = Boolean.valueOf(true);				// not new Boolean(true) ... see javadoc
				mapIsCleanForThisKey.put(key,clean);
			}
			String existingValue = map.get(key);
			if (existingValue == null || existingValue.equals("")) {
				map.put(key,newValue);
				// leave clean alone ... will either have been just added as new, or will already be true and replacing existing empty value with a non-empty value is still true
			}
			else {
				// already there
				if (!existingValue.equals(newValue)) {
//System.err.println("CTIrradiationEventDataFromImages.putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean(): different values newValue="+newValue+" existing value="+existingValue+" for key="+key);
					clean = Boolean.valueOf(false);			// not new Boolean(false) ... see javadoc
					mapIsCleanForThisKey.put(key,clean);	// replace old one, can't change the value of a Boolean
				}
				// else do nothing ... is same so OK
			}
		}
		// else do nothing ... pretend we never saw it if no value
	}
	
	private static boolean booleanExistsInMapAndIsTrue(Map<String,Boolean>map,String key) {
		Boolean flag = map.get(key);
		return flag != null && flag.booleanValue();
	}
	
	public CTAcquisitionParameters getAcquisitionParametersForIrradiationEvent(String uid) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): uid "+uid);		
		if (!organized) {
			organizeSlicesIntoIrradiationEvents();
		}
		if (!extracted) {
			extractConsistentParametersWithinIrradiationEvents();
		}

		java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance());
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		formatter.setDecimalSeparatorAlwaysShown(true);		// i.e., a period even if fraction is zero
		formatter.setGroupingUsed(false);					// i.e., no comma at thousands

		CTScanType useScanType = CTScanType.UNKNOWN;
		{
			if (booleanExistsInMapAndIsTrue(imageTypeValue3ByEventIsClean,uid)) {
				String imageTypeValue3 = imageTypeValue3ByEvent.get(uid);
				if (imageTypeValue3 != null && imageTypeValue3.toUpperCase().equals("LOCALIZER")) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): setting ScanType of LOCALIZER because of Image Type Value 3 for uid "+uid);
					useScanType = CTScanType.LOCALIZER;
				}
				// else other legal value is AXIAL, which doesn't actually mean AXIAL in the CTScanType sense, just not LOCALIZER
			}
		}
		String usePitchFactor = booleanExistsInMapAndIsTrue(pitchFactorByEventIsClean,uid) ? pitchFactorByEvent.get(uid) : null;
		if (!useScanType.equals(CTScanType.LOCALIZER)) {
			if (usePitchFactor != null && usePitchFactor.length() > 0) {
				try {
					double pitchFactorValue = Double.parseDouble(usePitchFactor);
					if (pitchFactorValue == 0) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): setting ScanType of STATIONARY because not LOCALIZER and pitch factor is zero for uid "+uid);
						useScanType = CTScanType.STATIONARY;
					}
				}
				catch (NumberFormatException e) {
					// else ignore it
				}
			}
		}

		boolean isLocalizer = useScanType.equals(CTScanType.LOCALIZER);
		
		String useTotalExposureTime =  null;
		if (isLocalizer) {
			if (booleanExistsInMapAndIsTrue(exposureTimeByEventIsClean,uid)) {
				useTotalExposureTime = exposureTimeByEvent.get(uid);
			}
		}
		else {
			//  ExposureTime is per rotation so ignore it
			Double count   = midScanTimeCountByEvent.get(uid);
			Double minimum = midScanTimeMinimumByEvent.get(uid);
			Double maximum = midScanTimeMaximumByEvent.get(uid);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): midScanTimeCountByEvent "+count);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): midScanTimeMinimumDouble "+minimum);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): midScanTimeMaximumDouble "+maximum);		
			if (count != null && minimum != null && maximum != null) {
				double countValue   = count.doubleValue();
				double minimumValue = minimum.doubleValue();
				double maximumValue = maximum.doubleValue();
				double totalExposureTime = (maximumValue - minimumValue)*(countValue+1)/countValue;	// attempt to compensate for time before midScan for 1st and after for last slice
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): totalExposureTime "+totalExposureTime);		
				if (totalExposureTime > 0) {
					useTotalExposureTime = formatter.format(totalExposureTime);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): useTotalExposureTime "+useTotalExposureTime);
				}
			}
		}
		
		String useKVP = booleanExistsInMapAndIsTrue(kvpByEventIsClean,uid) ? kvpByEvent.get(uid) : null;
		
		String useTubeCurrent = null;
		String useTubeCurrentMaximum = null;
		if (booleanExistsInMapAndIsTrue(tubeCurrentByEventIsClean,uid)) {
			useTubeCurrent = tubeCurrentByEvent.get(uid);
			useTubeCurrentMaximum = useTubeCurrent;
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): constant TubeCurrent "+useTubeCurrent);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): constant TubeCurrentMaximum "+useTubeCurrentMaximum);
		}
		else {
			Double total = tubeCurrentTotalByEvent.get(uid);
			Double count = tubeCurrentCountByEvent.get(uid);
			Double maximum = tubeCurrentMaximumByEvent.get(uid);
			if (total != null && count != null) {
				double countValue = count.doubleValue();
				if (countValue > 0) {
					double mean = total.doubleValue()/countValue;
					useTubeCurrent = formatter.format(mean);
				}
			}
			if (maximum != null) {
				useTubeCurrentMaximum = formatter.format(maximum.doubleValue());
			}
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): computed TubeCurrent "+useTubeCurrent);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersForIrradiationEvent(): computed TubeCurrentMaximum "+useTubeCurrentMaximum);
		}
		String useExposureTimePerRotation = null;
		if (!isLocalizer) {
			useExposureTimePerRotation = booleanExistsInMapAndIsTrue(exposureTimePerRotationByEventIsClean,uid) ? exposureTimePerRotationByEvent.get(uid) : null;
			// the Exposure Time attribute in old image IODs is actually the exposure time per rotation (and has already been converted from milliseconds to seconds)
			if (useExposureTimePerRotation == null || useExposureTimePerRotation.trim().length() == 0) {
				if (booleanExistsInMapAndIsTrue(exposureTimeByEventIsClean,uid)) {
					useExposureTimePerRotation = exposureTimeByEvent.get(uid);
				}
			}
		}
		
		CodedSequenceItem useAnatomy = booleanExistsInMapAndIsTrue(anatomyByEventIsClean,uid) ? anatomyByEvent.get(uid) : null;
				
		String useNominalSingleCollimationWidthInMM = booleanExistsInMapAndIsTrue(nominalSingleCollimationWidthInMMByEventIsClean,uid) ? nominalSingleCollimationWidthInMMByEvent.get(uid) : null;
		String useNominalTotalCollimationWidthInMM  = booleanExistsInMapAndIsTrue(nominalTotalCollimationWidthInMMByEventIsClean ,uid) ?  nominalTotalCollimationWidthInMMByEvent.get(uid) : null;
		
		return new CTAcquisitionParameters(uid,useScanType,useAnatomy,useTotalExposureTime,null/*scanningLength is filled in later from DLP/CTDIvol*/,
						useNominalSingleCollimationWidthInMM,useNominalTotalCollimationWidthInMM,usePitchFactor,
						useKVP,useTubeCurrent,useTubeCurrentMaximum,useExposureTimePerRotation);
	}
	
	public CTAcquisitionParameters getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(String seriesNumberAndScanRangeAndStudyInstanceUIDKey) {
		if (acquisitionParametersBySeriesNumberAndScanRangeAndStudyInstanceUIDKey == null) {
			if (!organized) {
				organizeSlicesIntoIrradiationEvents();
			}
			if (!extracted) {
				extractConsistentParametersWithinIrradiationEvents();
			}
			acquisitionParametersBySeriesNumberAndScanRangeAndStudyInstanceUIDKey = new TreeMap<String,CTAcquisitionParameters>();
			for (String uid : slicesByIrradiationEventUID.keySet()) {
				String useSeriesNumber     = booleanExistsInMapAndIsTrue(seriesNumberByEventIsClean,uid) ? seriesNumberByEvent.get(uid) : "";
				String useStartLocation    = getLocationAsString(highestSliceLocationByEvent.get(uid));
				String useEndLocation      = getLocationAsString(lowestSliceLocationByEvent.get(uid));
				String useStudyInstanceUID = booleanExistsInMapAndIsTrue(studyInstanceUIDByEventIsClean,uid) ? studyInstanceUIDByEvent.get(uid) : "";
				String key = useSeriesNumber+"+"+useStartLocation+"+"+useEndLocation+"+"+useStudyInstanceUID;
				if (!key.equals("+++")) {
					CTAcquisitionParameters ap = getAcquisitionParametersForIrradiationEvent(uid);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(): adding key="+key+" with parameters="+ap);
					acquisitionParametersBySeriesNumberAndScanRangeAndStudyInstanceUIDKey.put(key,ap);
				}
			}
		}
		CTAcquisitionParameters ap = acquisitionParametersBySeriesNumberAndScanRangeAndStudyInstanceUIDKey.get(seriesNumberAndScanRangeAndStudyInstanceUIDKey);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(): looking for key="+seriesNumberScanRangeKey+" found parameters="+ap);
		return ap;
	}
	
	public CTAcquisitionParameters getAcquisitionParametersByAcquisitionNumberAndStudyInstanceUID(String acquisitionNumberAndStudyInstanceUIDKey) {
		if (acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey == null) {
			if (!organized) {
				organizeSlicesIntoIrradiationEvents();
			}
			if (!extracted) {
				extractConsistentParametersWithinIrradiationEvents();
			}
			acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey = new TreeMap<String,CTAcquisitionParameters>();
			for (String uid : slicesByIrradiationEventUID.keySet()) {
				String useAcquisitionNumber = booleanExistsInMapAndIsTrue(acquisitionNumberByEventIsClean,uid) ? acquisitionNumberByEvent.get(uid) : "";
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): useAcquisitionNumber="+useAcquisitionNumber+" for event uid="+uid);
				String useStudyInstanceUID = booleanExistsInMapAndIsTrue(studyInstanceUIDByEventIsClean,uid) ? studyInstanceUIDByEvent.get(uid) : "";
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByStudyInstanceUID(): useStudyInstanceUID="+useStudyInstanceUID+" for event uid="+uid);
				String key = useAcquisitionNumber+"+"+useStudyInstanceUID;
				if (!key.equals("+")) {
					boolean addOrReplace = true;
					CTAcquisitionParameters ap = getAcquisitionParametersForIrradiationEvent(uid);
					CTAcquisitionParameters apAlreadyThere = acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey.get(key);
					if (apAlreadyThere != null) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): Aaargh ! key="+key+" already exists in map ...");
//System.err.print("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): already there = "+apAlreadyThere);
//System.err.print("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): new = "+ap);
						if (apAlreadyThere.equalsApartFromIrradiationEventUID(ap)) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): OK - are equal apart from event uid");
						}
						else {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): are not equal");
							addOrReplace = false;
							CTScanType scanTypeNew = ap.getScanType();
							CTScanType scanTypeAlreadyThere = apAlreadyThere.getScanType();
							// if same event contains localizer and non-localizers, we want the non-localizer information
							if (scanTypeNew != null && scanTypeAlreadyThere != null) {
								boolean isLocalizerNew = scanTypeNew.equals(CTScanType.LOCALIZER);
								boolean isLocalizerAlreadyThere = scanTypeAlreadyThere.equals(CTScanType.LOCALIZER);
								if (isLocalizerAlreadyThere && !isLocalizerNew) {
									addOrReplace = true;	
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): OK - replacing localizer information with non-localizer information ");
								}
								else if (!isLocalizerAlreadyThere && isLocalizerNew) {
									addOrReplace = false;	// if same event contains localizer and non-localizers, we want the non-localizer information
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): OK - not replacing non-localizer information with localizer information ");
								}
								// else are both of same type but not equal ... e.g., different reconstructed series with different current mean/max ... ignore one of them
							}
						}
					}
//System.err.println("");
					if (addOrReplace) {
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): adding key="+key+" with parameters="+ap);
						acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey.put(key,ap);
					}
				}
			}
		}
		CTAcquisitionParameters ap = acquisitionParametersByAcquisitionNumberAndStudyInstanceUIDKey.get(acquisitionNumberAndStudyInstanceUIDKey);
//System.err.println("CTIrradiationEventDataFromImages.getAcquisitionParametersByAcquisitionNumber(): looking for key="+acquisitionNumberAndStudyInstanceUIDKey+" found parameters="+ap);
		return ap;
	}
	
	public CTIrradiationEventDataFromImages() {
	}
	
	public CTIrradiationEventDataFromImages(String path) {
		add(path);
	}
	
	public CTIrradiationEventDataFromImages(Vector<String> paths) {
		for (int j=0; j< paths.size(); ++j) {
			add(paths.get(j));
		}
	}
	
	public void add(String path) {
		add(new File(path));
	}
		
	public void add(File file) {
//System.err.println("CTIrradiationEventDataFromImages.add(): add() file "+file);
		if (file.exists()) {
//System.err.println("CTIrradiationEventDataFromImages.add(): file exists "+file);
			if (file.isDirectory()) {
				ArrayList<File> files = FileUtilities.listFilesRecursively(file);
				for (File f : files) {
					add(f);
				}
			}
			else if (file.isFile() && file.getName().toUpperCase().equals("DICOMDIR")) {
//System.err.println("CTIrradiationEventDataFromImages.add(): Doing DICOMDIR from "+file);
				try {
					AttributeList list = new AttributeList();
					list.read(file.getCanonicalPath());
					DicomDirectory dicomDirectory = new DicomDirectory(list);
					HashMap allDicomFiles = dicomDirectory.findAllContainedReferencedFileNamesAndTheirRecords(file.getParentFile().getCanonicalPath());
//System.err.println("CTIrradiationEventDataFromImages.add(): Referenced files: "+allDicomFiles);
					Iterator it = allDicomFiles.keySet().iterator();
					while (it.hasNext()) {
						String doFileName = (String)it.next();
						if (doFileName != null) {
							add(doFileName);
						}
					}
				}
				catch (IOException e) {
					e.printStackTrace(System.err);
				}
				catch (DicomException e) {
					e.printStackTrace(System.err);
				}
			}
			else if (file.isFile() && DicomFileUtilities.isDicomOrAcrNemaFile(file)) {
//System.err.println("CTIrradiationEventDataFromImages.add(): Doing file "+file);
				try {
					AttributeList list = new AttributeList();
					list.read(file.getCanonicalPath(),null,true,true,TagFromName.PixelData);
					String irradiationEventUID = "";
					CodedSequenceItem srDocumentTitle = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.ConceptNameCodeSequence);
					if (OCR.isDoseScreenInstance(list)
					 || ExposureDoseSequence.isPhilipsDoseScreenInstance(list)
					) {
System.err.println("CTIrradiationEventDataFromImages.add(): Found dose screen in file "+file);
						doseScreenFilenames.add(file.getCanonicalPath());
					}
					else {
						String sopClassUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID);
						if (SOPClass.isStructuredReport(sopClassUID)
							&& srDocumentTitle != null
							&& srDocumentTitle.getCodingSchemeDesignator().equals("DCM")
							&& srDocumentTitle.getCodeValue().equals("113701")		// "X-Ray Radiation Dose Report"
						) {
//System.err.println("CTIrradiationEventDataFromImages.add(): Found dose SR in file "+file);
							doseStructuredReportFilenames.add(file.getCanonicalPath());
						}
						else if (sopClassUID.equals(SOPClass.CTImageStorage)) {
							organized = false;	// reset this if anything is added, even if previously organized and extracted
							extracted = false;
							slices.add(new Slice(list));
						}
						else {
//System.err.println("CTIrradiationEventDataFromImages.add(): Ignoring unwanted SOP Class UID "+sopClassUID+" file "+file);
						}
					}
				}
				catch (Exception e) {
					// probably wasn't a DICOM file after all, so don't sweat it
					e.printStackTrace(System.err);
				}
			}
			else {
				// wasn't a DICOM file after all, so don't sweat it
//System.err.println("CTIrradiationEventDataFromImages.add(): Not doing non-DICOM file "+file);
			}
		}
	}

	private void organizeSlicesIntoIrradiationEvents() {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents():");
		if (!slices.isEmpty()) {
			ArrayList<Slice> slicesWithoutExplicitIrradiationEvent = new ArrayList<Slice>();
			for (Slice s : slices) {
				if (s.irradiationEventUID.length() > 0) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Using supplied IrradiationEventUID "+s.irradiationEventUID+" for AcquisitionDateTime = "+s.acquisitionDateTime+" SliceLocation = "+s.sliceLocation+" SeriesNumber = "+s.seriesNumber+" AcquisitionNumber = "+s.acquisitionNumber);
					//irradiationEventUIDs.add(s.irradiationEventUID);
					List<Slice> event = slicesByIrradiationEventUID.get(s.irradiationEventUID);
					if (event == null) {
						event = new ArrayList<Slice>();
						slicesByIrradiationEventUID.put(s.irradiationEventUID,event);
					}
					event.add(s);
				}
				else {
					slicesWithoutExplicitIrradiationEvent.add(s);
				}
			}
			if (!slicesWithoutExplicitIrradiationEvent.isEmpty()) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): have instances without IrradiationEventUID");
				Map<String,List> slicesSeparatedByStudyAndSeriesAndAcquisition =  new HashMap<String,List>();
				for (Slice s : slicesWithoutExplicitIrradiationEvent) {
					String key = s.studyInstanceUID + "+" + s.seriesInstanceUID;	// do NOT include AcquisitionNumber, else over splits for GE
					List<Slice> instances = slicesSeparatedByStudyAndSeriesAndAcquisition.get(key);
					if (instances == null) {
						instances = new ArrayList<Slice>();
						slicesSeparatedByStudyAndSeriesAndAcquisition.put(key,instances);
					}
					instances.add(s);
				}
				//for (Collection<Slice> acquisition : slicesSeparatedByStudyAndSeriesAndAcquisition.values()) {
				for (String key : slicesSeparatedByStudyAndSeriesAndAcquisition.keySet()) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Processing study+series+acquisition = "+key);
					Collection<Slice> acquisition =  slicesSeparatedByStudyAndSeriesAndAcquisition.get(key);
					// partition by AcquisitionDateTime ... this works whether all slices have same acquisition, some are in batches, or even if every slice has a different acquisition time
					SortedMap<String,List<Slice>> separatedByAcquisitionDateTime = new TreeMap<String,List<Slice>>();
					for (Slice s : acquisition) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Check for same AcquisitionDateTime = "+s.acquisitionDateTime+" SliceLocation = "+s.sliceLocation);
						List<Slice> instancesWithSameAcquisitionDateTime = separatedByAcquisitionDateTime.get(s.acquisitionDateTime);
						if (instancesWithSameAcquisitionDateTime == null) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Creating new group for AcquisitionDateTime = "+s.acquisitionDateTime);
							instancesWithSameAcquisitionDateTime = new ArrayList<Slice>();
							separatedByAcquisitionDateTime.put(s.acquisitionDateTime,instancesWithSameAcquisitionDateTime);
						}
						instancesWithSameAcquisitionDateTime.add(s);
					}
					// then walk through each AcquisitionDateTime group to merge them unless there is spatial overlap ...
					List<List<Slice>> events = new ArrayList<List<Slice>>();
					{
						List<Slice> event = new ArrayList<Slice>();
						events.add(event);
						double  lowestSliceLocationInCurrentGroup = 0;	// intializer avoids warning, but is never used (sliceLocationLimitsInCurrentGroupNotYetInitialized)
						double highestSliceLocationInCurrentGroup = 0;
						boolean sliceLocationLimitsInCurrentGroupNotYetInitialized = true;
						double  lowestSliceLocationInPreviousGroup = 0;
						double highestSliceLocationInPreviousGroup = 0;
						boolean sliceLocationLimitsInPreviousGroupNotYetInitialized = true;
						for (List<Slice> instancesWithSameAcquisitionDateTime : separatedByAcquisitionDateTime.values()) {	// value set will be returned in AcquisitionDateTime order
							{
								for (Slice s : instancesWithSameAcquisitionDateTime) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): AcquisitionDateTime = "+s.acquisitionDateTime+" SliceLocation = "+s.sliceLocation);
									if (s.sliceLocation != null && s.sliceLocation.length() > 0) {
										try {
											double thisSliceLocation = Double.parseDouble(s.sliceLocation);
											if (sliceLocationLimitsInCurrentGroupNotYetInitialized) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): starting new range: SliceLocation = "+s.sliceLocation);
												sliceLocationLimitsInCurrentGroupNotYetInitialized = false;
												lowestSliceLocationInCurrentGroup = thisSliceLocation;
												highestSliceLocationInCurrentGroup = thisSliceLocation;
											}
											else if (thisSliceLocation < lowestSliceLocationInCurrentGroup) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): extending lower limit of range: SliceLocation = "+s.sliceLocation);
												lowestSliceLocationInCurrentGroup = thisSliceLocation;
											}
											else if (thisSliceLocation > highestSliceLocationInCurrentGroup) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): extending upper limit of range: SliceLocation = "+s.sliceLocation);
												highestSliceLocationInCurrentGroup = thisSliceLocation;
											}
											else {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): within existing range SliceLocation = "+s.sliceLocation);
												// within existing range so do nothing
											}
										}
										catch (NumberFormatException e) {
											System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Bad SliceLocation in SOP Instance "+s.sopInstanceUID);
											e.printStackTrace(System.err);
										}
									}
									else {
										//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Missing SliceLocation in SOP Instance "+s.sopInstanceUID);
									}
								}
							}
							// now have range for current acquisitionDateTime group ... compare with prior to see if overlap
							if (sliceLocationLimitsInCurrentGroupNotYetInitialized) {
								// couldn't initialize range ... ignore
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): could not determine SliceLocation range");
								//event.addAll(instancesWithSameAcquisitionDateTime);
							}
							else {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): lowest SliceLocation = "+lowestSliceLocationInCurrentGroup+" highest SliceLocation = "+highestSliceLocationInCurrentGroup);
								if (sliceLocationLimitsInPreviousGroupNotYetInitialized) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): no previous group to compare with");
									event.addAll(instancesWithSameAcquisitionDateTime);
									sliceLocationLimitsInCurrentGroupNotYetInitialized = true;
									sliceLocationLimitsInPreviousGroupNotYetInitialized = false;
									 lowestSliceLocationInPreviousGroup = lowestSliceLocationInCurrentGroup;
									highestSliceLocationInPreviousGroup = highestSliceLocationInCurrentGroup;
								}
								else {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): comparing with previous group: lowest SliceLocation = "+lowestSliceLocationInPreviousGroup+" highest SliceLocation = "+highestSliceLocationInPreviousGroup);
									// check for overlap
									if (highestSliceLocationInPreviousGroup < lowestSliceLocationInCurrentGroup
									 || highestSliceLocationInCurrentGroup  < lowestSliceLocationInPreviousGroup) {
										// no overlap, so expand range to merge current and previous group
										event.addAll(instancesWithSameAcquisitionDateTime);
										sliceLocationLimitsInCurrentGroupNotYetInitialized = true;
										sliceLocationLimitsInPreviousGroupNotYetInitialized = false;
										 lowestSliceLocationInPreviousGroup =  lowestSliceLocationInPreviousGroup <  lowestSliceLocationInCurrentGroup ?  lowestSliceLocationInPreviousGroup :  lowestSliceLocationInCurrentGroup;
										highestSliceLocationInPreviousGroup = highestSliceLocationInPreviousGroup > highestSliceLocationInCurrentGroup ? highestSliceLocationInPreviousGroup : highestSliceLocationInCurrentGroup;
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): expanding range to merge current and previous: now lowest SliceLocation = "+lowestSliceLocationInPreviousGroup+" highest SliceLocation = "+highestSliceLocationInPreviousGroup);
									}
									else {
										// overlap, so begin new event
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): overlap, so creating new event");
										event = new ArrayList<Slice>();
										events.add(event);
										event.addAll(instancesWithSameAcquisitionDateTime);
										sliceLocationLimitsInCurrentGroupNotYetInitialized = true;
										sliceLocationLimitsInPreviousGroupNotYetInitialized = false;
										 lowestSliceLocationInPreviousGroup =  lowestSliceLocationInCurrentGroup;
										highestSliceLocationInPreviousGroup = highestSliceLocationInCurrentGroup;
									}
								}
							}
						}
					}
					// now we have the events we need
					Iterator<List<Slice>> eventsIterator = events.iterator();	// use explicit Iterator rather than for loop since need Iterator.remove() to avoid ConcurrentModificationException
					//for (List<Slice> event : events) {
					while (eventsIterator.hasNext()) {
						List<Slice> event = eventsIterator.next();
						if (event.isEmpty()) {
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): Dropping empty event");
							eventsIterator.remove();	// e.g., had no SliceLocation information (such as Siemens MPR)
						}
						else {
							String irradiationEventUID = "";
							try {
								irradiationEventUID = u.getAnotherNewUID();
							}
							catch (DicomException e) {
								e.printStackTrace(System.err);
							}
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): IrradiationEventUID = "+irradiationEventUID);
							//irradiationEventUIDs.add(irradiationEventUID);
							slicesByIrradiationEventUID.put(irradiationEventUID,event);
							for (Slice s : event) {
								s.irradiationEventUID = irradiationEventUID;
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): \tAcquisitionDateTime = "+s.acquisitionDateTime+" SliceLocation = "+s.sliceLocation+" SeriesNumber = "+s.seriesNumber+" AcquisitionNumber = "+s.acquisitionNumber);
//System.err.println("CTIrradiationEventDataFromImages.organizeSlicesIntoIrradiationEvents(): \tanatomy = "+s.anatomyCodedSequenceItem);
							}
						}
					}
				}
			}
		}
		organized = true;
	}
	
	private class Slice {
		String irradiationEventUID;
		String patientAge;
		String patientSex;
		String patientWeight;
		String patientSize;
		String studyInstanceUID;
		String seriesInstanceUID;
		String sopInstanceUID;
		String seriesNumber;
		String acquisitionNumber;
		String seriesDescription;
		
		String imageType;
		String imageTypeValue3;
		
		String exposureTimeInSeconds;
		String kvp;
		String tubeCurrent;
		String midScanTime;
		String exposureTimePerRotation;
		String nominalSingleCollimationWidth;
		String nominalTotalCollimationWidth;
		String pitchFactor;
		
		String acquisitionDateTime;
		String sliceLocation;
		
		CodedSequenceItem anatomyCodedSequenceItem;
				
		Slice(AttributeList list) {
			irradiationEventUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.IrradiationEventUID);
			patientAge = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.PatientAge);
			patientSex = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.PatientSex);
			patientWeight = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.PatientWeight);
			patientSize = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.PatientSize);
			studyInstanceUID = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.StudyInstanceUID);
			seriesInstanceUID = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SeriesInstanceUID);
			sopInstanceUID = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SOPInstanceUID);
			seriesNumber = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SeriesNumber);
			acquisitionNumber = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.AcquisitionNumber);
			seriesDescription = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SeriesDescription);
			
			imageType = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.ImageType);
			try {
				imageTypeValue3 = "";
				Attribute aImageType = list.get(TagFromName.ImageType);
				if (aImageType != null && aImageType.getVM() >= 3) {
					String[] vImageType = aImageType.getStringValues();
					imageTypeValue3 = vImageType[2];
				}
			}
			catch (DicomException e) {
				e.printStackTrace(System.err);
			}
			
			{
				String exposureTimeInMilliSeconds = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.ExposureTime);
				if (!exposureTimeInMilliSeconds.equals("")) {
					exposureTimeInSeconds = "";
					try {
						exposureTimeInSeconds = new Double(new Double(exposureTimeInMilliSeconds).doubleValue()/1000).toString();
					}
					catch (NumberFormatException e) {
						// do nothing
					}
				}
			}

			kvp = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.KVP);
			tubeCurrent = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.XRayTubeCurrent);

			midScanTime="";
			if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0019,0x0010)).equals("GEMS_ACQU_01")) {
				midScanTime = Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0019,0x1024));
			}

			exposureTimePerRotation = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.RevolutionTime);
			if (exposureTimePerRotation.equals("")) {
				if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0019,0x0010)).equals("GEMS_ACQU_01")) {
					exposureTimePerRotation = Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0019,0x1027));	//  Rotation Speed (Gantry Period)
				}
			}

			nominalSingleCollimationWidth = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SingleCollimationWidth);
			if (nominalSingleCollimationWidth.equals("")) {
				if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0045,0x0010)).equals("GEMS_HELIOS_01")) {
					nominalSingleCollimationWidth = Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0045,0x1002));	//   Macro width at ISO Center
					if (nominalSingleCollimationWidth.contains("?")) {
						nominalSingleCollimationWidth = "";
					}
				}
			}

			nominalTotalCollimationWidth = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.TotalCollimationWidth);
			if (nominalTotalCollimationWidth.equals("") && !nominalSingleCollimationWidth.equals("")) {
				if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0045,0x0010)).equals("GEMS_HELIOS_01")) {
					try {
						double dNumberOfMacroRowsInDetector = Attribute.getSingleDoubleValueOrDefault(list,new AttributeTag(0x0045,0x1001),0d);	//   Number of Macro Rows in Detector
						double dNominalSingleCollimationWidth = Double.valueOf(nominalSingleCollimationWidth).doubleValue();
						if (dNumberOfMacroRowsInDetector > 0 && dNominalSingleCollimationWidth > 0) {
							double dnominalTotalCollimationWidth = dNumberOfMacroRowsInDetector * dNominalSingleCollimationWidth;
							nominalTotalCollimationWidth = Double.toString(dnominalTotalCollimationWidth);
						}
					}
					catch (NumberFormatException e) {
						e.printStackTrace(System.err);
					}
				}
			}

			pitchFactor = Attribute.getDelimitedStringValuesOrEmptyString(list,TagFromName.SpiralPitchFactor);
			if (pitchFactor.equals("")) {
				if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0043,0x0010)).equals("GEMS_PARM_01")) {
					pitchFactor = Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0043,0x1027));	//   Scan Pitch Ratio in the form "n.nnn:1"
					pitchFactor = pitchFactor.trim().replace(":1","");
				}
				if (pitchFactor.equals("") && !nominalTotalCollimationWidth.equals("")) {
					// Pitch Factor: For Spiral Acquisition, the Pitch Factor is the ratio of the Table Feed per Rotation
					// to the Nominal Total Collimation Width. For Sequenced Acquisition, the Pitch Factor is the ratio
					// of the Table Feed per single sequenced scan to the Nominal Total Collimation Width.
					try {
						double dTableFeedPerRotation = Attribute.getSingleDoubleValueOrDefault(list,TagFromName.TableFeedPerRotation,0d);
						if (dTableFeedPerRotation == 0) {
							if (Attribute.getDelimitedStringValuesOrEmptyString(list,new AttributeTag(0x0045,0x0010)).equals("GEMS_ACQU_01")) {
								dTableFeedPerRotation = Attribute.getSingleDoubleValueOrDefault(list,new AttributeTag(0x0019,0x1023),0d);	// Table Speed [mm/rotation]
							}
						}
						if (dTableFeedPerRotation > 0) {
							double dNominalTotalCollimationWidth = Double.valueOf(nominalTotalCollimationWidth).doubleValue();
							if (dNominalTotalCollimationWidth > 0) {
								double dPitchFactor = dTableFeedPerRotation / dNominalTotalCollimationWidth;
								pitchFactor = Double.toString(dPitchFactor);
							}
						}
					}
					catch (NumberFormatException e) {
						e.printStackTrace(System.err);
					}
				}
			}

			// handles midnight crossing, but not robust if one or the other is sometimes missing in the set of files
			acquisitionDateTime = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.AcquisitionDateTime);
			if (acquisitionDateTime.equals("")) {
				acquisitionDateTime = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.AcquisitionDate) + Attribute.getSingleStringValueOrEmptyString(list,TagFromName.AcquisitionTime);
			}

			sliceLocation = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SliceLocation);

			try {
				anatomyCodedSequenceItem = null;
				DisplayableAnatomicConcept anatomy = CTAnatomy.findAnatomicConcept(list);
				if (anatomy != null) {
					anatomyCodedSequenceItem = anatomy.getCodedSequenceItem();
				}
			}
			catch (DicomException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	private void extractConsistentParametersWithinIrradiationEvents() {
//System.err.println("CTIrradiationEventDataFromImages.extractConsistentParametersWithinIrradiationEvents():");
		for (String irradiationEventUID : slicesByIrradiationEventUID.keySet()) {
			List<Slice> event = slicesByIrradiationEventUID.get(irradiationEventUID);
			for (Slice s : event) {
				// handle attributes that are global (i.e., not indexed by event), and assume that the entire set is one patient
				// the common logic is not refactored into a utility method, since need to return two values, the string and the cleanliness of it
				if (patientAgeIsClean) {
					String newValue = s.patientAge;
					if (!newValue.equals("")) {
						if (patientAge == null || patientAge.equals("")) {
							patientAge = newValue;
						}
						else if (!patientAge.equals(newValue)) {
							patientAgeIsClean = false;
						}
					}
				}
				if (patientSexIsClean) {
					String newValue = s.patientSex;
					if (!newValue.equals("")) {
						if (patientSex == null || patientSex.equals("")) {
							patientSex = newValue;
						}
						else if (!patientSex.equals(newValue)) {
							patientSexIsClean = false;
						}
					}
				}
				if (patientWeightIsClean) {
					String newValue = s.patientWeight;
					if (!newValue.equals("")) {
						if (patientWeight == null || patientWeight.equals("")) {
							patientWeight = newValue;
						}
						else if (!patientWeight.equals(newValue)) {
							patientWeightIsClean = false;
						}
					}
				}
				if (patientSizeIsClean) {
					String newValue = s.patientSize;
					if (!newValue.equals("")) {
						if (patientSize == null || patientSize.equals("")) {
							patientSize = newValue;
						}
						else if (!patientSize.equals(newValue)) {
							patientSizeIsClean = false;
						}
					}
				}

				// now handle values that are event-specific
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (studyInstanceUIDByEvent,                  studyInstanceUIDByEventIsClean,                  irradiationEventUID,s.studyInstanceUID);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (imageTypeByEvent,                         imageTypeByEventIsClean,                         irradiationEventUID,s.imageType);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (imageTypeValue3ByEvent,                   imageTypeValue3ByEventIsClean,                   irradiationEventUID,s.imageTypeValue3);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (acquisitionNumberByEvent,                 acquisitionNumberByEventIsClean,                 irradiationEventUID,s.acquisitionNumber);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (seriesNumberByEvent,                      seriesNumberByEventIsClean,                      irradiationEventUID,s.seriesNumber);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (seriesDescriptionByEvent,                 seriesDescriptionByEventIsClean,                 irradiationEventUID,s.seriesDescription);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (pitchFactorByEvent,                       pitchFactorByEventIsClean,                       irradiationEventUID,s.pitchFactor);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (exposureTimeByEvent,                      exposureTimeByEventIsClean,                      irradiationEventUID,s.exposureTimeInSeconds);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (kvpByEvent,                               kvpByEventIsClean,                               irradiationEventUID,s.kvp);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (exposureTimePerRotationByEvent,           exposureTimePerRotationByEventIsClean,           irradiationEventUID,s.exposureTimePerRotation);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (nominalSingleCollimationWidthInMMByEvent, nominalSingleCollimationWidthInMMByEventIsClean, irradiationEventUID,s.nominalSingleCollimationWidth);
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (nominalTotalCollimationWidthInMMByEvent,  nominalTotalCollimationWidthInMMByEventIsClean,  irradiationEventUID,s.nominalTotalCollimationWidth);
		
				putStringValueByStringIndexIfNotDifferentElseFlagAsUnclean      (tubeCurrentByEvent,                       tubeCurrentByEventIsClean,                       irradiationEventUID,s.tubeCurrent);
				if (s.tubeCurrent.length() > 0) {
					try {
						Double tubeCurrentDouble = new Double(s.tubeCurrent);
						Double tubeCurrentCountDouble = tubeCurrentCountByEvent.get(irradiationEventUID);
						if (tubeCurrentCountDouble == null) {
							// first time
							tubeCurrentCountByEvent.put(irradiationEventUID,new Double(1));
							tubeCurrentTotalByEvent.put(irradiationEventUID,tubeCurrentDouble);
							tubeCurrentMaximumByEvent.put(irradiationEventUID,tubeCurrentDouble);
						}
						else {
							double tubeCurrentValue = tubeCurrentDouble.doubleValue();
										
							Double tubeCurrentTotalDouble = tubeCurrentTotalByEvent.get(irradiationEventUID);
							double tubeCurrentTotalValue  = tubeCurrentTotalDouble.doubleValue();
							tubeCurrentTotalValue += tubeCurrentValue;
							tubeCurrentTotalByEvent.put(irradiationEventUID,new Double(tubeCurrentTotalValue));
										
							Double tubeCurrentMaximumDouble = tubeCurrentMaximumByEvent.get(irradiationEventUID);
							double tubeCurrentMaximumValue  = tubeCurrentMaximumDouble.doubleValue();
							if (tubeCurrentValue > tubeCurrentMaximumValue) {
								tubeCurrentMaximumValue = tubeCurrentValue;
								tubeCurrentMaximumByEvent.put(irradiationEventUID,new Double(tubeCurrentMaximumValue));
							}
										
							double tubeCurrentCountValue = tubeCurrentCountDouble.doubleValue();
							tubeCurrentCountValue+=1;
							tubeCurrentCountByEvent.put(irradiationEventUID,new Double(tubeCurrentCountValue));
						}
					}
					catch (NumberFormatException e) {
						// do nothing
					}
				}

				if (s.midScanTime.length() > 0) {
					Double midScanTimeDouble = new Double(s.midScanTime);
					Double midScanTimeCountDouble = midScanTimeCountByEvent.get(irradiationEventUID);
					if (midScanTimeCountDouble == null) {
						// first time
						midScanTimeCountByEvent.put(irradiationEventUID,new Double(1));
						midScanTimeMinimumByEvent.put(irradiationEventUID,midScanTimeDouble);
						midScanTimeMaximumByEvent.put(irradiationEventUID,midScanTimeDouble);
					}
					else {
						try {
							double midScanTimeValue = midScanTimeDouble.doubleValue();
										
							Double midScanTimeMaximumDouble = midScanTimeMaximumByEvent.get(irradiationEventUID);
							double midScanTimeMaximumValue  = midScanTimeMaximumDouble.doubleValue();
							if (midScanTimeValue > midScanTimeMaximumValue) {
								midScanTimeMaximumValue = midScanTimeValue;
								midScanTimeMaximumByEvent.put(irradiationEventUID,new Double(midScanTimeMaximumValue));
							}
										
							Double midScanTimeMinimumDouble = midScanTimeMinimumByEvent.get(irradiationEventUID);
							double midScanTimeMinimumValue  = midScanTimeMinimumDouble.doubleValue();
							if (midScanTimeValue < midScanTimeMinimumValue) {
								midScanTimeMinimumValue = midScanTimeValue;
								midScanTimeMinimumByEvent.put(irradiationEventUID,new Double(midScanTimeMinimumValue));
							}
										
							double midScanTimeCountValue = midScanTimeCountDouble.doubleValue();
							midScanTimeCountValue+=1;
							midScanTimeCountByEvent.put(irradiationEventUID,new Double(midScanTimeCountValue));
						}
						catch (NumberFormatException e) {
							// do nothing
						}
					}
				}

				putCodedSequenceItemByStringIndexIfNotDifferentElseFlagAsUnclean(anatomyByEvent,          anatomyByEventIsClean,           irradiationEventUID,s.anatomyCodedSequenceItem);

				putStringValueByStringIndexIfLexicographicSortIsEarlier(startAcquisitionDateTimeByEvent,irradiationEventUID,s.acquisitionDateTime);
				putStringValueByStringIndexIfLexicographicSortIsLater  (  endAcquisitionDateTimeByEvent,irradiationEventUID,s.acquisitionDateTime);
				{
					String overallEarliestAcquisitionDateTime = overallEarliestAcquisitionDateTimeByStudy.get(s.studyInstanceUID);
					if (overallEarliestAcquisitionDateTime == null || s.acquisitionDateTime.compareTo(overallEarliestAcquisitionDateTime) < 0) {
						overallEarliestAcquisitionDateTimeByStudy.put(s.studyInstanceUID,s.acquisitionDateTime);
					}
					String overallLatestAcquisitionDateTime = overallLatestAcquisitionDateTimeByStudy.get(s.studyInstanceUID);
					if (overallLatestAcquisitionDateTime == null || s.acquisitionDateTime.compareTo(overallLatestAcquisitionDateTime) > 0) {
						overallLatestAcquisitionDateTimeByStudy.put(s.studyInstanceUID,s.acquisitionDateTime);
					}
				}


				putNumericStringValueByStringIndexIfNumericSortIsEarlier(lowestSliceLocationByEvent,irradiationEventUID,s.sliceLocation);
				putNumericStringValueByStringIndexIfNumericSortIsLater(highestSliceLocationByEvent,irradiationEventUID,s.sliceLocation);
			}
		}
		extracted = true;
	}
	
	private static String toStringCodedSequenceItem(String name,Map<String,CodedSequenceItem> map,Map<String,Boolean> cleanMap,String uid) {
		String value;
		Boolean clean = cleanMap.get(uid);
		if (clean == null) {
			value = "-- not found --";
		}
		else {
			if (clean.booleanValue()) {
				CodedSequenceItem item = map.get(uid);
				if (item == null) {
					value = "-- not found --";
				}
				else {
					value = item.toString();
				}
			}
			else {
				value = "-- inconsistent values for event --";
			}
		}
		return "\t\t"+name+" = "+value+"\n";
	}
	
	private static String toString(String name,String value,boolean isClean) {
		if (isClean) {
			if (value == null) {
				value = "-- not found --";
			}
		}
		else {
			value = "-- inconsistent values for event --";
		}
		return "\t\t"+name+" = "+value+"\n";
	}
	
	private static String toString(String name,Map<String,String> valueMap,Map<String,Boolean> cleanMap,String uid) {
		String value;
		Boolean clean = cleanMap.get(uid);
		if (clean == null) {
			value = "-- not found --";
		}
		else {
			if (clean.booleanValue()) {
				value = valueMap.get(uid);
				if (value == null) {
					value = "-- not found --";
				}
			}
			else {
				value = "-- inconsistent values for event --";
			}
		}
		return "\t\t"+name+" = "+value+"\n";
	}
	
	private static String stringValueToString(String name,Map<String,String> valueMap,String uid) {
		String value = valueMap.get(uid);
		if (value == null) {
			value = "-- not found --";
		}
		return "\t\t"+name+" = "+value+"\n";
	}
	
	private static String doubleValueToString(String name,Map<String,Double> map,String uid) {
		String value;
		Double dvalue = map.get(uid);
		if (dvalue == null) {
			value = "-- not found --";
		}
		else {
			value = dvalue.toString();
		}
		return "\t\t"+name+" = "+value+"\n";
	}
	
	private static String getLocationAsString(Double dValue) {
		String value;
		if (dValue == null) {
			value = "";
		}
		else {
			//value = dValue.toString();
			java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance());
			formatter.setGroupingUsed(false);
			formatter.setMinimumFractionDigits(3);
			formatter.setMaximumFractionDigits(3);
			value=formatter.format(dValue.doubleValue());
			if (value.startsWith("-")) {
				value = "I"+value.substring(1);
			}
			else {
				value = "S"+value;
			}
		}
		return value;
	}
	
	//private static String toStringLocation(String name,Map<String,Double> map,String uid) {
	//	String value = getLocationAsString(map.get(uid));
	//	if (value.equals("")) {
	//		value = "-- not found --";
	//	}
	//	return "\t\t"+name+" = "+value+"\n";
	//}
	
	public String toString() {
		if (!organized) {
			organizeSlicesIntoIrradiationEvents();
		}
		if (!extracted) {
			extractConsistentParametersWithinIrradiationEvents();
		}
		
		StringBuffer buf = new StringBuffer();

		buf.append("\tCommon:\n");
		buf.append(toString("PatientAge",patientAge,patientAgeIsClean));
		buf.append(toString("PatientSex",patientSex,patientSexIsClean));
		buf.append(toString("PatientWeight",patientWeight,patientWeightIsClean));
		buf.append(toString("PatientSize",patientSize,patientSizeIsClean));

		for (String irradiationEventUID : slicesByIrradiationEventUID.keySet()) {
			buf.append("\tIrradiationEventUID = "+irradiationEventUID+"\n");
			//buf.append(toString("StudyInstanceUID",studyInstanceUIDByEvent,studyInstanceUIDByEventIsClean,irradiationEventUID));
			buf.append(toString("ImageType",imageTypeByEvent,imageTypeByEventIsClean,irradiationEventUID));
			buf.append(toString("AcquisitionNumber",acquisitionNumberByEvent,acquisitionNumberByEventIsClean,irradiationEventUID));
			buf.append(toString("SeriesNumber",seriesNumberByEvent,seriesNumberByEventIsClean,irradiationEventUID));
			buf.append(toString("SeriesDescription",seriesDescriptionByEvent,seriesDescriptionByEventIsClean,irradiationEventUID));
			buf.append(toStringCodedSequenceItem("Anatomy",anatomyByEvent,anatomyByEventIsClean,irradiationEventUID));
			
			buf.append(stringValueToString("StartAcquisitionDateTime",startAcquisitionDateTimeByEvent,irradiationEventUID));
			buf.append(stringValueToString("EndAcquisitionDateTime",endAcquisitionDateTimeByEvent,irradiationEventUID));
			buf.append(doubleValueToString("LowestSliceLocation",lowestSliceLocationByEvent,irradiationEventUID));
			buf.append(doubleValueToString("HighestSliceLocation",highestSliceLocationByEvent,irradiationEventUID));
			
			buf.append(toString("ExposureTime",exposureTimeByEvent,exposureTimeByEventIsClean,irradiationEventUID));
			buf.append(toString("KVP",kvpByEvent,kvpByEventIsClean,irradiationEventUID));
			buf.append(toString("TubeCurrent",tubeCurrentByEvent,tubeCurrentByEventIsClean,irradiationEventUID));
			buf.append(doubleValueToString("TubeCurrentTotal",tubeCurrentTotalByEvent,irradiationEventUID));
			buf.append(doubleValueToString("TubeCurrentCount",tubeCurrentCountByEvent,irradiationEventUID));
			buf.append(doubleValueToString("TubeCurrentMaximum",tubeCurrentMaximumByEvent,irradiationEventUID));

			buf.append(doubleValueToString("MidScanTimeCount",midScanTimeCountByEvent,irradiationEventUID));
			buf.append(doubleValueToString("MidScanTimeMinimum",midScanTimeMinimumByEvent,irradiationEventUID));
			buf.append(doubleValueToString("MidScanTimeMaximum",midScanTimeMaximumByEvent,irradiationEventUID));

			buf.append(toString("ExposureTimePerRotation",exposureTimePerRotationByEvent,exposureTimePerRotationByEventIsClean,irradiationEventUID));
			
			buf.append(toString("NominalSingleCollimationWidthInMM",nominalSingleCollimationWidthInMMByEvent,nominalSingleCollimationWidthInMMByEventIsClean,irradiationEventUID));
			buf.append(toString("NominalTotalCollimationWidthInMM",nominalTotalCollimationWidthInMMByEvent,nominalTotalCollimationWidthInMMByEventIsClean,irradiationEventUID));
			buf.append(toString("PitchFactor",pitchFactorByEvent,pitchFactorByEventIsClean,irradiationEventUID));
		}

		for (String studyInstanceUID : overallEarliestAcquisitionDateTimeByStudy.keySet()) {
			buf.append("\tStudyInstanceUID = "+studyInstanceUID+"\n");
			buf.append("\t\tEarliest AcquisitionDateTime = "+overallEarliestAcquisitionDateTimeByStudy.get(studyInstanceUID)+"\n");
			buf.append("\t\tLatest   AcquisitionDateTime = "+overallLatestAcquisitionDateTimeByStudy.get(studyInstanceUID)+"\n");
		}
		
		return buf.toString();
	}
	
	public static final void main(String arg[]) {
		try {
			CTIrradiationEventDataFromImages eventDataFromImages = new CTIrradiationEventDataFromImages(arg[0]);
System.err.print(eventDataFromImages);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
}


/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DicomException;

public class CTScanType {
	
	private String description;
	
	private CTScanType() {};
	
	private CTScanType(String description) {
		this.description = description;
	};
	
	public static final CTScanType LOCALIZER = new CTScanType("Localizer");
	
	public static final CTScanType HELICAL = new CTScanType("Helical");
	
	public static final CTScanType AXIAL = new CTScanType("Axial");
	
	public static final CTScanType STATIONARY = new CTScanType("Stationary");
	
	public static final CTScanType FREE = new CTScanType("Free");
	
	public static final CTScanType UNKNOWN = new CTScanType("Unknown");
	
	public String toString() { return description; }
	
	public static CTScanType selectFromDescription(String description) {
		CTScanType found = UNKNOWN;
		if (description != null) {
			description = description.trim().toUpperCase();
			if (description.equals(HELICAL.toString().toUpperCase())
			 || description.equals("SPIRAL")
			) {
				found = HELICAL;
			}
			else if (description.equals(AXIAL.toString().toUpperCase())
			 ||  description.equals("SEQUENCED")
			) {
				found = AXIAL;
			}
			else if (description.equals(FREE.toString().toUpperCase())
			 || description.equals("SMARTVIEW")		// GE's fluoroscopy mode
			) {
				found = FREE;
			}
			else if (description.equals(STATIONARY.toString().toUpperCase())
			 ||  description.equals("CINE")			// when GE uses CINE in the screen, they set STATIONARY in their SRs
			) {
				found = STATIONARY;
			}
			else if (description.equals(LOCALIZER.toString().toUpperCase())
			 || description.equals("SCOUT")
			 || description.equals("CONSTANT_ANGLE")
			 || description.equals("TOPOGRAM")
			) {
				found = LOCALIZER;
			}
		}
		return found;
	}
	
	public static CTScanType selectFromCode(CodedSequenceItem csi) {
		CTScanType found = UNKNOWN;
		if (csi != null) {
			String cv = csi.getCodeValue();
			String csd = csi.getCodingSchemeDesignator();
			if (csd.equals("SRT") && cv.equals("P5-08001")) {		// "Spiral Acquisition"
				found = HELICAL;
			}
			else if (csd.equals("DCM") && cv.equals("113804")) {	// "Sequenced Acquisition"
				found = AXIAL;
			}
			else if (csd.equals("DCM") && cv.equals("113805")) {	// "Constant Angle Acquisition"
				found = LOCALIZER;
			}
			else if (csd.equals("DCM") && cv.equals("113806")) {	// "Stationary Acquisition"
				found = STATIONARY;
			}
			else if (csd.equals("DCM") && cv.equals("113807")) {	// "Free Acquisition"
				found = FREE;
			}
		}
		return found;
	}
	
	public static CodedSequenceItem getCodedSequenceItem(CTScanType scanType) throws DicomException {
		CodedSequenceItem csi = null;
		if (scanType != null) {
			if (scanType.equals(CTScanType.LOCALIZER)) {
				csi = new CodedSequenceItem("113805","DCM","Constant Angle Acquisition");
			}
			else if (scanType.equals(CTScanType.HELICAL)) {
				csi = new CodedSequenceItem("P5-08001","SRT","Spiral Acquisition");
			}
			else if (scanType.equals(CTScanType.AXIAL)) {
				csi = new CodedSequenceItem("113804","DCM","Sequenced Acquisition");
			}
			else if (scanType.equals(CTScanType.STATIONARY)) {
				csi = new CodedSequenceItem("113806","DCM","Stationary Acquisition");
			}
			else if (scanType.equals(CTScanType.FREE)) {
				csi = new CodedSequenceItem("113807","DCM","FREE Acquisition");
			}
			// else if UNKNOWN return nothing
		}
		return csi;
	}
	
	public CodedSequenceItem getCodedSequenceItem() throws DicomException {
		return getCodedSequenceItem(this);
	}
	
}
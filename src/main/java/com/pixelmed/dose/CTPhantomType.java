/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DicomException;

public class CTPhantomType {
	
	private String description;
	
	private CTPhantomType() {};
	
	private CTPhantomType(String description) {
		this.description = description;
	};
	
	public static final CTPhantomType HEAD16 = new CTPhantomType("HEAD16");
	
	public static final CTPhantomType BODY32 = new CTPhantomType("BODY32");
		
	public String toString() { return description; }
	
	public static CTPhantomType selectFromDescription(String description) {
		CTPhantomType found = null;
		if (description != null) {
			description = description.trim().toUpperCase();
			if (description.equals(HEAD16.toString())) {
				found = HEAD16;
			}
			else if (description.equals(BODY32.toString())) {
				found = BODY32;
			}
		}
		return found;
	}
	
	public static CTPhantomType selectFromCode(CodedSequenceItem csi) {
		CTPhantomType found = null;
		if (csi != null) {
			String cv = csi.getCodeValue();
			String csd = csi.getCodingSchemeDesignator();
			if (csd.equals("DCM") && cv.equals("113690")) {			// "IEC Head Dosimetry Phantom"
				found = HEAD16;
			}
			else if (csd.equals("DCM") && cv.equals("113691")) {	// "IEC Body Dosimetry Phantom"
				found = BODY32;
			}
		}
		return found;
	}
	
	public static CodedSequenceItem getCodedSequenceItem(CTPhantomType phantomType) throws DicomException {
		CodedSequenceItem csi = null;
		if (phantomType != null) {
			if (phantomType.equals(CTPhantomType.HEAD16)) {
				csi = new CodedSequenceItem("113690","DCM","IEC Head Dosimetry Phantom");
			}
			else if (phantomType.equals(CTPhantomType.BODY32)) {
				csi = new CodedSequenceItem("113691","DCM","IEC Body Dosimetry Phantom");
			}
		}
		return csi;
	}
	
	public CodedSequenceItem getCodedSequenceItem() throws DicomException {
		return getCodedSequenceItem(this);
	}
	
}

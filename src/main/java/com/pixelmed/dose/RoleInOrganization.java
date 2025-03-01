/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DicomException;

public class RoleInOrganization {
	private String description;
	
	private RoleInOrganization() {};
	
	private RoleInOrganization(String description) {
		this.description = description;
	};
	
	public static final RoleInOrganization PHYSICIAN = new RoleInOrganization("Physician");
	public static final RoleInOrganization TECHNOLOGIST = new RoleInOrganization("Technologist");
	public static final RoleInOrganization RADIATION_PHYSICIST = new RoleInOrganization("Radiation Physicist");
	
	public String toString() { return description; }
	
	public static CodedSequenceItem getCodedSequenceItem(RoleInOrganization role) throws DicomException {
		CodedSequenceItem csi = null;
		if (role != null) {
			if (role.equals(RoleInOrganization.PHYSICIAN)) {
				csi = new CodedSequenceItem("121081","DCM","Physician");
			}
			else if (role.equals(RoleInOrganization.TECHNOLOGIST)) {
				csi = new CodedSequenceItem("121083","DCM","Technologist");
			}
			else if (role.equals(RoleInOrganization.RADIATION_PHYSICIST)) {
				csi = new CodedSequenceItem("121105","DCM","Radiation Physicist");
			}
		}
		return csi;
	}
	//DCM 121081 Physician
	//DCM 121082 Nurse
	//DCM 121083 Technologist
	//DCM 121084 Radiographer
	//DCM 121085 Intern
	//DCM 121086 Resident
	//DCM 121087 Registrar
	//DCM 121088 Fellow
	//DCM 121089 Attending [Consultant]
	//DCM 121090 Scrub nurse
	//DCM 121091 Surgeon
	//DCM 121092 Sonologist
	//DCM 121093 Sonographer
	//DCM 121105 Radiation Physicist
	
	public CodedSequenceItem getCodedSequenceItem() throws DicomException {
		return getCodedSequenceItem(this);
	}
}

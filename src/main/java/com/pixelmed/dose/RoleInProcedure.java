/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.DicomException;

public class RoleInProcedure {
	private String description;
	
	private RoleInProcedure() {};
	
	private RoleInProcedure(String description) {
		this.description = description;
	};
	
	public static final RoleInProcedure IRRADIATION_ADMINISTERING = new RoleInProcedure("Irradiation Administering");
	public static final RoleInProcedure IRRADIATION_AUTHORIZING = new RoleInProcedure("Irradiation Authorizing");
	
	public String toString() { return description; }
	
	public static CodedSequenceItem getCodedSequenceItem(RoleInProcedure role) throws DicomException {
		CodedSequenceItem csi = null;
		if (role != null) {
			if (role.equals(RoleInProcedure.IRRADIATION_ADMINISTERING)) {
				csi = new CodedSequenceItem("113851","DCM","Irradiation Administering");
			}
			else if (role.equals(RoleInProcedure.IRRADIATION_AUTHORIZING)) {
				csi = new CodedSequenceItem("113850","DCM","Irradiation Authorizing");
			}
		}
		return csi;
	}
	
	public CodedSequenceItem getCodedSequenceItem() throws DicomException {
		return getCodedSequenceItem(this);
	}
}


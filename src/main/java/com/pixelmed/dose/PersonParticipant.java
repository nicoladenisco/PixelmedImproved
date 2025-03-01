/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.ContentItem;
import com.pixelmed.dicom.ContentItemFactory;
import com.pixelmed.dicom.DicomException;

public class PersonParticipant {
	
	protected String name;
	protected RoleInProcedure roleInProcedure;
	protected String id;
	protected String idIssuer;
	protected String organization;
	protected RoleInOrganization roleInOrganization;
	
	public PersonParticipant(String name,RoleInProcedure roleInProcedure,String id,String idIssuer,String organization,RoleInOrganization roleInOrganization) {
		this.name = name;
		this.roleInProcedure = roleInProcedure;
		this.id = id;
		this.idIssuer = idIssuer;
		this.organization = organization;
		this.roleInOrganization = roleInOrganization;
	}
	
	public String getName() { return name; }
	public RoleInProcedure getRoleInProcedure() { return roleInProcedure; }
	public String getId() { return id; }
	public String getIdIssuer() { return idIssuer; }
	public String getOrganization() { return organization; }
	public RoleInOrganization getRoleInOrganization() { return roleInOrganization; }
	
	public ContentItem getStructuredReportFragment() throws DicomException {
		ContentItemFactory cif = new ContentItemFactory();
		ContentItem root = cif.new PersonNameContentItem(null,"CONTAINS",new CodedSequenceItem("113870","DCM","Person Name"),(name == null || name.trim().length() == 0 ? "Nobody" : name));		// name content item cannot be left out even if empty, since parent
		if (roleInProcedure != null)                                  { cif.new CodeContentItem(root,"HAS PROPERTIES",new CodedSequenceItem("113875","DCM","Person Role in Procedure"),roleInProcedure.getCodedSequenceItem()); }
		if (id != null && id.trim().length() > 0)                     { cif.new TextContentItem(root,"HAS PROPERTIES",new CodedSequenceItem("113871","DCM","Person ID"),id); }
		if (idIssuer != null && idIssuer.trim().length() > 0)         { cif.new TextContentItem(root,"HAS PROPERTIES",new CodedSequenceItem("113872","DCM","Person ID Issuer"),idIssuer); }
		if (organization != null && organization.trim().length() > 0) { cif.new TextContentItem(root,"HAS PROPERTIES",new CodedSequenceItem("113873","DCM","Organization Name"),organization); }
		if (roleInOrganization != null)                               { cif.new CodeContentItem(root,"HAS PROPERTIES",new CodedSequenceItem("113874","DCM","Person Role in Organization"),roleInOrganization.getCodedSequenceItem()); }
		return root;
	}
	
}

/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>Various pre-defined constants for identifying this software.</p>
 *
 * @author	dclunie
 */
public class VersionAndConstants {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/dicom/VersionAndConstants.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";
	
	/***/
	public static final String softwareVersion = "001";	// must be [A-Z0-9_] and <= 4 chars else screws up ImplementationVersionName

	/***/
	public static final String implementationVersionName = "PIXELMEDJAVA"+softwareVersion;

	public static final String uidRoot = "1.3.6.1.4.1.5962";
	/***/
	public static final String uidQualifierForThisToolkit = "99";
	/***/
	public static final String uidQualifierForUIDGenerator = "1";
	/***/
	public static final String uidQualifierForImplementationClassUID = "2";
	/***/
	public static final String uidQualifierForInstanceCreatorUID = "3";
	/***/
	public static final String implementationClassUID = uidRoot+"."+uidQualifierForThisToolkit+"."+uidQualifierForImplementationClassUID;
	/***/
	public static final String instanceCreatorUID = uidRoot+"."+uidQualifierForThisToolkit+"."+uidQualifierForInstanceCreatorUID;
}

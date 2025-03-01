/* Copyright (c) 2001-2003, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * @author	dclunie
 */
public class DicomException extends Exception {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/dicom/DicomException.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";

	/**
	 * @param	msg
	 */
	public DicomException(String msg) {
		super(msg);
	}
}



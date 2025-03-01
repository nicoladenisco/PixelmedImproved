/* Copyright (c) 2001-2007, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

/**
 * @author	dclunie
 */
public class PdfException extends Exception {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/utils/PdfException.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	/**
	 * @param	msg
	 */
	public PdfException(String msg) {
		super(msg);
	}
}



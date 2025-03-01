/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.ftp;

/**
 * @author	dclunie
 */
public class FTPException extends Exception {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/ftp/FTPException.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	/**
	 * @param	msg
	 */
	public FTPException(String msg) {
		super(msg);
	}
}




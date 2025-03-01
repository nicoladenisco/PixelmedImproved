/* Copyright (c) 2001-2003, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

/**
 * @author	dclunie
 */
public class DicomNetworkException extends Exception {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/DicomNetworkException.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	/**
	 * @param	msg
	 */
	public DicomNetworkException(String msg) {
		super(msg);
	}
}




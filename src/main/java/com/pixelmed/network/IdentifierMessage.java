/* Copyright (c) 2001-2003, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;

import java.util.LinkedList;
import java.io.*;

/**
 * @author	dclunie
 */
public class IdentifierMessage {
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/IdentifierMessage.java,v 1.2 2011-08-10 18:14:19 nicola Exp $";

	private byte bytes[];

	/**
	 * @param	list
	 * @param	transferSyntaxUID
	 * @exception	IOException
	 * @exception	DicomException
	 */
	public IdentifierMessage(AttributeList list,String transferSyntaxUID) throws DicomException, IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DicomOutputStream dout = new DicomOutputStream(bout,null/* no meta-header */,transferSyntaxUID);
		list.write(dout);
		if (bout.size() % 2 != 0) {
			// How could this happen ? The use of deflate or bzip2 transfer syntaxes can result in an odd number of bytes written by AttributeList.write() (000525)
			// which should pad with null but doesn't, so do it here just in case (000524) :(
			bout.write(0);
		}
		bytes = bout.toByteArray();
//System.err.println("IdentifierMessage: bytes="+HexDump.dump(bytes));
	}

	/***/
	public byte[] getBytes() { return bytes; }
}

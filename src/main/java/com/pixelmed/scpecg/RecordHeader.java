/* Copyright (c) 2001-2003, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.scpecg;

import java.io.IOException;

import com.pixelmed.dicom.BinaryInputStream;

/**
 * <p>A class to encapsulate an SCP-ECG record header.</p>
 *
 * @author	dclunie
 */
public class RecordHeader {
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/scpecg/RecordHeader.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	private int crc;
	private long recordLength;
	
	public int getCRC() { return crc; }
	public long getRecordLength() { return recordLength; }
		
	/**
	 * <p>Read a header from a stream.</p>
	 *
	 * @param	i	the input stream
	 */
	public long read(BinaryInputStream i) throws IOException {
		long bytesRead=0;
		crc = i.readUnsigned16();
		bytesRead+=2;		
		recordLength = i.readUnsigned32();
		bytesRead+=4;		
		return bytesRead;
	}

	/**
	 * <p>Dump the record header as a <code>String</code>.</p>
	 *
	 * @return		the header as a <code>String</code>
	 */
	public String toString() {
		return "CRC = "+crc+" dec (0x"+Integer.toHexString(crc)+")\n"
		     + "Record Length = "+recordLength+" dec (0x"+Long.toHexString(recordLength)+")\n";
	}
}


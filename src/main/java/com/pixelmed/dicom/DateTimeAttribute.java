/* Copyright (c) 2001-2008, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A concrete class specializing {@link com.pixelmed.dicom.Attribute Attribute} for
 * Date Time (DT) attributes.</p>
 *
 * <p>Though an instance of this class may be created
 * using its constructors, there is also a factory class, {@link com.pixelmed.dicom.AttributeFactory AttributeFactory}.</p>
 *
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeFactory
 * @see com.pixelmed.dicom.AttributeList
 *
 * @author	dclunie
 */
public class DateTimeAttribute extends StringAttribute {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/dicom/DateTimeAttribute.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";

	/**
	 * <p>Construct an (empty) attribute.</p>
	 *
	 * @param	t	the tag of the attribute
	 */
	public DateTimeAttribute(AttributeTag t) {
		super(t);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @exception	IOException
	 * @exception	DicomException
	 */
	public DateTimeAttribute(AttributeTag t,long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl,i);
	}

	/**
	 * <p>Read an attribute from an input stream.</p>
	 *
	 * @param	t			the tag of the attribute
	 * @param	vl			the value length of the attribute
	 * @param	i			the input stream
	 * @exception	IOException
	 * @exception	DicomException
	 */
	public DateTimeAttribute(AttributeTag t,Long vl,DicomInputStream i) throws IOException, DicomException {
		super(t,vl,i);
	}

	/**
	 * <p>Get the value representation of this attribute (DT).</p>
	 *
	 * @return	'D','T' in ASCII as a two byte array; see {@link com.pixelmed.dicom.ValueRepresentation ValueRepresentation}
	 */
	public byte[] getVR() { return ValueRepresentation.DT; }
	
	
	/**
	 * <p>Get a DICOM format DT <code>String</code> value from a Java <code>Date</code>.</p>
	 *
	 * @param	date			the date to format
	 * @return					a DICOM formatted DT value
	 */
	public static String getFormattedString(java.util.Date date) {
		return new java.text.SimpleDateFormat("yyyyMMddHHmmss.SSSZ").format(date);
	}
}


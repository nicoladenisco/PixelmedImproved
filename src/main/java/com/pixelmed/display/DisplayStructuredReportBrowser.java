/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.util.*;

import com.pixelmed.dicom.*;

/**
 * @author	dclunie
 */
public class DisplayStructuredReportBrowser extends StructuredReportBrowser {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/DisplayStructuredReportBrowser.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";

	private int frameWidthWanted;
	private int frameHeightWanted;

	private Map mapOfSOPInstanceUIDToReferencedFileName;

	/**
	 * @param	list
	 * @param	mapOfSOPInstanceUIDToReferencedFileName
	 * @param	frameWidthWanted
	 * @param	frameHeightWanted
	 * @exception	DicomException
	 */
	public DisplayStructuredReportBrowser(AttributeList list,Map mapOfSOPInstanceUIDToReferencedFileName,
			int frameWidthWanted,int frameHeightWanted) throws DicomException {
		super(list);
		this.mapOfSOPInstanceUIDToReferencedFileName=mapOfSOPInstanceUIDToReferencedFileName;
		this.frameWidthWanted=frameWidthWanted;
		this.frameHeightWanted=frameHeightWanted;
	}

	/**
	 * @param	list
	 * @param	mapOfSOPInstanceUIDToReferencedFileName
	 * @param	frameWidthWanted
	 * @param	frameHeightWanted
	 * @param	title
	 * @exception	DicomException
	 */
	public DisplayStructuredReportBrowser(AttributeList list,Map mapOfSOPInstanceUIDToReferencedFileName,
			int frameWidthWanted,int frameHeightWanted,String title) throws DicomException {
		super(list,title);
		this.mapOfSOPInstanceUIDToReferencedFileName=mapOfSOPInstanceUIDToReferencedFileName;
		this.frameWidthWanted=frameWidthWanted;
		this.frameHeightWanted=frameHeightWanted;
	}

	/**
	 * @param	instances
	 */
	protected void doSomethingWithSelectedSOPInstances(Vector instances) {
		DicomBrowser.loadAndDisplayImagesFromSOPInstances(instances,mapOfSOPInstanceUIDToReferencedFileName,
			frameWidthWanted,frameHeightWanted);
	}
}



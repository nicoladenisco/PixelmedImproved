/* Copyright (c) 2001-2003, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*; 
import java.awt.color.*; 
import java.util.*; 
import java.io.*; 
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.dicom.*;

/**
 * @author	dclunie
 */
class DisplayDicomDirectoryBrowser extends DicomDirectoryBrowser {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/DisplayDicomDirectoryBrowser.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";

	private int frameWidthWanted;
	private int frameHeightWanted;

	/**
	 * @param	list
	 * @param	parentFilePath
	 * @param	frame
	 * @param	frameWidthWanted
	 * @param	frameHeightWanted
	 * @exception	DicomException
	 */
	public DisplayDicomDirectoryBrowser(AttributeList list,String parentFilePath,JFrame frame,
			int frameWidthWanted,int frameHeightWanted) throws DicomException {
		super(list,parentFilePath,frame);
		this.frameWidthWanted=frameWidthWanted;
		this.frameHeightWanted=frameHeightWanted;
	}

	/**
	 * @param	paths
	 */
	protected void doSomethingWithSelectedFiles(Vector paths) {
		DicomBrowser.loadAndDisplayImagesFromDicomFiles(paths,
			getDicomDirectory().getMapOfSOPInstanceUIDToReferencedFileName(getParentFilePath()),
			frameWidthWanted,frameHeightWanted);
	}
}



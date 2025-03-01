/* Copyright (c) 2001-2005, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import java.util.HashSet;
import java.util.Iterator;

/**
 * <p>A class to describe a set of frame sets, each of which shares common characteristics suitable for display or analysis as an entity.</p>
 *
 * @author	dclunie
 */
class SetOfFrameSets extends HashSet {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/dicom/SetOfFrameSets.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";

	/**
	 * <p>Insert a single or multi-frame object into the set of existing frame sets,
	 * creating new frame sets as necessary.</p>
	 *
	 * @param	list	a list of DICOM attributes for an object
	 */
	void insertIntoFrameSets(AttributeList list) {
		// partition by rows and columns and SOP Class and Modality
		
		boolean found = false;
		Iterator i = iterator();
		while (i.hasNext()) {
			FrameSet tryFrameSet = (FrameSet)(i.next());
			if (tryFrameSet.eligible(list)) {
				tryFrameSet.insert(list);
				found=true;
				break;				// only insert it in the first frame set that matches
			}
		}
		if (!found) {
			add(new FrameSet(list));
		}
	}
	
	/**
	 * <p>Return a String representing this object's value.</p>
	 *
	 * @return	a string representation of the value of this object
	 */
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		int j = 0;
		Iterator i =iterator();
		while (i.hasNext()) {
			strbuf.append("Frame set [");
			strbuf.append(Integer.toString(j));
			strbuf.append("]:\n");
			strbuf.append(((FrameSet)i.next()).toString());
			strbuf.append("\n");
			++j;
		}
		return strbuf.toString();
	}

	/**
	 * <p>For testing, read all DICOM files and partition them.</p>
	 *
	 * @param	arg	the filenames containing the images to partition
	 */
	public static void main(String arg[]) {
		SetOfFrameSets setOfFrameSets = new SetOfFrameSets();
		for (int a=0; a<arg.length; ++a) {
			String dicomFileName = arg[a];
			try {
				DicomInputStream i = new DicomInputStream(new BufferedInputStream(new FileInputStream(dicomFileName)));
				AttributeList list = new AttributeList();
				list.read(i,TagFromName.PixelData);
				i.close();
				setOfFrameSets.insertIntoFrameSets(list);
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
				System.exit(0);
			}
		}
		System.out.println(setOfFrameSets.toString());
	}
}


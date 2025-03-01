/* Copyright (c) 2001-2003, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.*; 

/**
 * <p>A class to encapsulate a text annotation at a location on an image.</p>
 *
 * @see com.pixelmed.display.SingleImagePanel
 * @see com.pixelmed.display.DicomBrowser
 *
 * @author	dclunie
 */
class TextAnnotation {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/TextAnnotation.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	private String string;
	private Point anchorPoint;

	/**
	 * @param	string	the annotation
	 * @param	x	the horizontal location
	 * @param	y	the vertical location
	 */
	public TextAnnotation(String string,int x,int y) {
		this.string=string;
		this.anchorPoint=new Point(x,y);
	}

	/**
	 * @param	string		the annotation
	 * @param	anchorPoint	the location on the image
	 */
	public TextAnnotation(String string,Point anchorPoint) {
		this.string=string;
		this.anchorPoint=anchorPoint;
	}

	/**
	 * <p>Get the text of the annotation.</p>
	 *
	 * @return	the annotation
	 */
	public String getString() { return string; }

	/**
	 * <p>Get the location.</p>
	 *
	 * @return	the location
	 */
	public Point getAnchorPoint() { return anchorPoint; }

	/**
	 * <p>Get the horizontal location.</p>
	 *
	 * @return	the horizontal location
	 */
	public int getAnchorPointXAsInt() { return (int)anchorPoint.getX(); }

	/**
	 * <p>Get the vertical location.</p>
	 *
	 * @return	the vertical location
	 */
	public int getAnchorPointYAsInt() { return (int)anchorPoint.getY(); }
}


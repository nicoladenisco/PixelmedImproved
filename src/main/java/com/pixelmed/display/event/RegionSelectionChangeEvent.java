/* Copyright (c) 2001-2004, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class RegionSelectionChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/event/RegionSelectionChangeEvent.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	private int centerX;
	private int centerY;
	private int tlhcX;
	private int tlhcY;
	private int brhcX;
	private int brhcY;

	/**
	 * @param	eventContext
	 * @param	centerX
	 * @param	centerY
	 * @param	tlhcX
	 * @param	tlhcY
	 * @param	brhcX
	 * @param	brhcY
	 */
	public RegionSelectionChangeEvent(EventContext eventContext,
			int centerX,int centerY,int tlhcX,int tlhcY,int brhcX,int brhcY) {
		super(eventContext);
		this.centerX=centerX;
		this.centerY=centerY;
		this.tlhcX=tlhcX;
		this.tlhcY=tlhcY;
		this.brhcX=brhcX;
		this.brhcY=brhcY;
	}

	/***/
	public int getCenterX() { return centerX; }
	/***/
	public int getCenterY() { return centerY; }
	/***/
	public int getTLHCX() { return tlhcX; }
	/***/
	public int getTLHCY() { return tlhcY; }
	/***/
	public int getBRHCX() { return brhcX; }
	/***/
	public int getBRHCY() { return brhcY; }
}


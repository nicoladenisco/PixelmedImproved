/* Copyright (c) 2001-2004, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class WindowCenterAndWidthChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/event/WindowCenterAndWidthChangeEvent.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	double windowCenter;
	double windowWidth;

	/**
	 * @param	eventContext
	 * @param	center
	 * @param	width
	 */
	public WindowCenterAndWidthChangeEvent(EventContext eventContext,double center,double width) {
		super(eventContext);
		windowCenter=center;
		windowWidth=width;
	}

	/***/
	public double getWindowCenter() { return windowCenter; }
	/***/
	public double getWindowWidth()  { return windowWidth; }
}


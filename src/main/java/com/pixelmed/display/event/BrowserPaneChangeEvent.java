/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class BrowserPaneChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/event/BrowserPaneChangeEvent.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	/***/
	public static final int IMAGE = 1;
	/***/
	public static final int DICOMDIR = 2;
	/***/
	public static final int DATABASE = 3;
	/***/
	public static final int SPECTROSCOPY = 4;
	/***/
	public static final int SR = 5;
	
	private int browserPaneType;

	/**
	 * @param	eventContext
	 * @param	browserPaneType
	 */
	public BrowserPaneChangeEvent(EventContext eventContext,int browserPaneType) {
		super(eventContext);
		this.browserPaneType=browserPaneType;
	}

	/***/
	public int getType() { return browserPaneType; }
}


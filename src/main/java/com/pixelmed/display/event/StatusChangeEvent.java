/* Copyright (c) 2001-2003, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;

/**
 * @author	dclunie
 */
public class StatusChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/event/StatusChangeEvent.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	private String statusMessage;

	/**
	 * @param	statusMessage
	 */
	public StatusChangeEvent(String statusMessage) {
		super();
		this.statusMessage=statusMessage;
	}

	/***/
	public String getStatusMessage() { return statusMessage; }
}


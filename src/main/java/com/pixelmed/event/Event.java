/* Copyright (c) 2001-2004, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.event;

//import java.util.EventObject;

/**
 * @author	dclunie
 */
public class Event /*extends EventObject*/ {	// why bother extending java.util.EventObject

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/event/Event.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";
	
	/***/
	private EventContext eventContext;			// may be null

	/**
	 */
	public Event() {
		//super(source);		// EventObject throws exception if source is null
	}

	/**
	 * @param	source
	 */
	public Event(Object source) {
		//super(source);		// EventObject throws exception if source is null
	}

	/**
	 * @param	source
	 * @param	eventContext
	 */
	public Event(Object source,EventContext eventContext) {
		//super(source);		// EventObject throws exception if source is null
		this.eventContext=eventContext;
	}

	/**
	 * @param	eventContext
	 */
	public Event(EventContext eventContext) {
		//super(source);		// EventObject throws exception if source is null
		this.eventContext=eventContext;
	}

	/**
	 * @return	the context of the event
	 */
	public final EventContext getEventContext() {
		return eventContext;
	}
}


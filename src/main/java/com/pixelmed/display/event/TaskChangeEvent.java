/* Copyright (c) 2001-2006, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class TaskChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/event/TaskChangeEvent.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";
	
	protected static int NEXT = 1;
	protected static int PREVIOUS = 2;

	protected int task;

	public final String toString() {
		String s = "unrecognized";
		if (task == NEXT) {
			s="NEXT";
		}
		else if (task == PREVIOUS) {
			s="PREVIOUS";
		}
		return s;
	}
	
	/**
	 * @param	eventContext
	 * @param	task
	 */
	protected TaskChangeEvent(EventContext eventContext,int task) {
		super(eventContext);
		this.task=task;
//System.err.println("TaskChangeEvent() "+toString());
	}

	/**
	 * @param	eventContext
	 */
	public static TaskChangeEvent newNextTaskChangeEvent(EventContext eventContext) {
		return new TaskChangeEvent(eventContext,NEXT);
	}

	/**
	 * @param	eventContext
	 */
	public static TaskChangeEvent newPreviousTaskChangeEvent(EventContext eventContext) {
		return new TaskChangeEvent(eventContext,PREVIOUS);
	}

	/***/
	public boolean isNext() { return task ==NEXT; }

	/***/
	public boolean isPrevious() { return task == PREVIOUS; }
}


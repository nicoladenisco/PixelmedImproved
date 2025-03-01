/* Copyright (c) 2001-2007, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

/**
 * <p>This class represents a concpet that has a coded representation.</p>
 * 
 * @author	dclunie
 */
public class Concept {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/anatproc/Concept.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";
	
	protected String conceptUniqueIdentifier;		// UMLS CUID ?
	
	public Concept(String conceptUniqueIdentifier) {
		this.conceptUniqueIdentifier=conceptUniqueIdentifier;
	}
	
	protected Concept() {};
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("\tCUI: ");
		buf.append(conceptUniqueIdentifier);
		buf.append("\n");
		return buf.toString();
	}
}


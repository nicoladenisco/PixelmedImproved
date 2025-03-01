/* Copyright (c) 2001-2007, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.anatproc;

import com.pixelmed.dicom.CodedSequenceItem;

/**
 * 
 * @author	dclunie
 */
public class SchemeAndValuePair {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/anatproc/SchemeAndValuePair.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";
	
	protected String codeValue;
	protected String codingSchemeDesignator;

	public SchemeAndValuePair(String codeValue,String codingSchemeDesignator) {
		this.codeValue=codeValue;
		this.codingSchemeDesignator=codingSchemeDesignator;
	}
		
	public SchemeAndValuePair(CodedSequenceItem item) {
		this.codeValue=item.getCodeValue();
		this.codingSchemeDesignator=item.getCodingSchemeDesignator();
	}
		
	public boolean equals(Object o) {
//System.err.println("SchemeAndValuePair.equals(): comparing "+this+" with "+o);
		if (o != null && o instanceof SchemeAndValuePair) {
			SchemeAndValuePair osvp = (SchemeAndValuePair)o;
//System.err.println("SchemeAndValuePair.equals(): comparing "+this+" with "+osvp);
			return codingSchemeDesignator != null && codingSchemeDesignator.equals(osvp.codingSchemeDesignator) && codeValue != null && codeValue.equals(osvp.codeValue);
		}
		else {
			return super.equals(o);
		}
	}
	
	public int hashCode() {
		return codeValue.hashCode() + codingSchemeDesignator.hashCode();
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("(");
		buf.append(codeValue);
		buf.append(",");
		buf.append(codingSchemeDesignator);
		buf.append(")");
		return buf.toString();
	}
	
}

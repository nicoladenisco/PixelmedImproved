/* Copyright (c) 2001-2005, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.validate;

// JAXP packages
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamSource;

/**
 * <p>make a translet from any XSL-T source file.</p>
 *
 * @author	dclunie
 */
public class CompileXSLTIntoTranslet {
	/***/
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/validate/CompileXSLTIntoTranslet.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	/**
	 * <p>Read the XSLT-C file specified on the command line and make a translate with the same name but a .class extension.</p>
	 *
	 * @param	arg	the name of the file containing the XSLT-C source
	 */
	public static void main(String arg[]) {
		try {
			StreamSource xslt = new StreamSource(arg[0]);
			TransformerFactory factory = TransformerFactory.newInstance();
			factory.setAttribute("generate-translet", Boolean.TRUE);
			Templates templates = factory.newTemplates(xslt);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}


/* Copyright (c) 2001-2008, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * <p>A class of static methods for handling veterinary (animal) data.</p>
 *
 * @author	dclunie
 */
abstract public class Veterinary {

	/***/
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/dicom/Veterinary.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";

	public static boolean isPatientAnAnimal(AttributeList list) {
		boolean isAnimal = false;
		CodedSequenceItem iPatientSpeciesCodeSequence = CodedSequenceItem.getSingleCodedSequenceItemOrNull(list,TagFromName.PatientSpeciesCodeSequence);
		if (iPatientSpeciesCodeSequence == null) {
			String vPatientSpeciesDescription = Attribute.getSingleStringValueOrNull(list,TagFromName.PatientSpeciesDescription);
			if (vPatientSpeciesDescription == null
			 || vPatientSpeciesDescription.trim().length() == 0
			 || vPatientSpeciesDescription.toLowerCase().contains("homo sapien")
			 || vPatientSpeciesDescription.toLowerCase().contains("human")) {
				isAnimal = false;
			}
			else {
				isAnimal = true;
			}
		}
		else {
			String codeValue = iPatientSpeciesCodeSequence.getCodeValue();
			String codingSchemeDesignator = iPatientSpeciesCodeSequence.getCodingSchemeDesignator();
			String codeMeaning = iPatientSpeciesCodeSequence.getCodeMeaning();
			if (codeValue != null && codingSchemeDesignator != null) {
				if ((codingSchemeDesignator.equals("SRT") || codingSchemeDesignator.equals("SNM3")) && codeValue.equals("L-85B00")) {
					isAnimal = false;
				}
				else if (codeMeaning != null && (codeMeaning.toLowerCase().contains("homo sapien") || codeMeaning.toLowerCase().contains("human"))) {
					isAnimal = false;
				}
				else {
					isAnimal = true;
				}
			}
		}
		return isAnimal;
	}
}


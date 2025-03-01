/* Copyright (c) 2001-2008, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import com.pixelmed.dicom.SOPClass;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * <p>Accept only SOP Classes for storage of composite instances and verification SOP Classes.</p>
 *
 * @author	dclunie
 */
public class CompositeInstanceStoreAbstractSyntaxSelectionPolicy implements AbstractSyntaxSelectionPolicy {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/CompositeInstanceStoreAbstractSyntaxSelectionPolicy.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";
	
	/**
	 * Accept or reject Abstract Syntaxes (SOP Classes).
	 *
	 * Only SOP Classes for storage of composite instances and verification SOP Classes are accepted.
	 *
	 * Should be called before Transfer Syntax selection is performed.
	 *
	 * @param	presentationContexts	a java.util.LinkedList of {@link PresentationContext PresentationContext} objects,
	 *			each of which contains an Abstract Syntax (SOP Class UID)
	 * @param	associationNumber	for debugging messages
	 * @param	debugLevel
	 * @return		the java.util.LinkedList of {@link PresentationContext PresentationContext} objects,
	 *			as supplied but with the result/reason field set to either "acceptance" or
	 *			"abstract syntax not supported (provider rejection)"
	 */
	public LinkedList applyAbstractSyntaxSelectionPolicy(LinkedList presentationContexts,int associationNumber,int debugLevel) {
		ListIterator pcsi = presentationContexts.listIterator();
		while (pcsi.hasNext()) {
			PresentationContext pc = (PresentationContext)(pcsi.next());
			String abstractSyntaxUID = pc.getAbstractSyntaxUID();
			pc.setResultReason(
				SOPClass.isImageStorage(abstractSyntaxUID)
			     || SOPClass.isNonImageStorage(abstractSyntaxUID)
			     || SOPClass.isVerification(abstractSyntaxUID)
				? (byte)0 : (byte)3);	// acceptance :  abstract syntax not supported (provider rejection)			      
		}
		return presentationContexts;
	}
}

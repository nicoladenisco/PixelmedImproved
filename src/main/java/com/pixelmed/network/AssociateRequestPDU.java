/* Copyright (c) 2001-2008, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;

import java.util.LinkedList;

/**
 * @author	dclunie
 */
class AssociateRequestPDU extends AssociateRequestAcceptPDU {
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/AssociateRequestPDU.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	/**
	 * @param	calledAETitle
	 * @param	callingAETitle
	 * @param	implementationClassUID
	 * @param	implementationVersionName
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	presentationContexts
	 * @param	scuSCPRoleSelections
	 * @param	userIdentityType			0 == do not send user identity negotiation subitem
	 * @param	userIdentityPrimaryField	may be null as appropriate to userIdentityType
	 * @param	userIdentitySecondaryField	may be null as appropriate to userIdentityType
	 * @exception	DicomNetworkException
	 */
	public AssociateRequestPDU(String calledAETitle,String callingAETitle, String implementationClassUID, String implementationVersionName,
			int ourMaximumLengthReceived,
			LinkedList presentationContexts,
			LinkedList scuSCPRoleSelections,
			int userIdentityType,String userIdentityPrimaryField,String userIdentitySecondaryField) throws DicomNetworkException {
		super(0x01,calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,ourMaximumLengthReceived,presentationContexts,scuSCPRoleSelections,
			userIdentityType,userIdentityPrimaryField,userIdentitySecondaryField);
	}

	/**
	 * @param	calledAETitle
	 * @param	callingAETitle
	 * @param	implementationClassUID
	 * @param	implementationVersionName
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	presentationContexts
	 * @param	scuSCPRoleSelections
	 * @param	userIdentityType			0 == do not send user identity negotiation subitem
	 * @param	userIdentityPrimaryField	may be null as appropriate to userIdentityType
	 * @param	userIdentitySecondaryField	may be null as appropriate to userIdentityType
	 * @exception	DicomNetworkException
	 */
	public AssociateRequestPDU(String calledAETitle,String callingAETitle, String implementationClassUID, String implementationVersionName,
			int ourMaximumLengthReceived,
			LinkedList presentationContexts,
			LinkedList scuSCPRoleSelections,
			int userIdentityType,byte[] userIdentityPrimaryField,byte[] userIdentitySecondaryField) throws DicomNetworkException {
		super(0x01,calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,ourMaximumLengthReceived,presentationContexts,scuSCPRoleSelections,
			userIdentityType,userIdentityPrimaryField,userIdentitySecondaryField);
	}

	/**
	 * @param	calledAETitle
	 * @param	callingAETitle
	 * @param	implementationClassUID
	 * @param	implementationVersionName
	 * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
	 * @param	presentationContexts
	 * @param	scuSCPRoleSelections
	 * @exception	DicomNetworkException
	 */
	public AssociateRequestPDU(String calledAETitle,String callingAETitle, String implementationClassUID, String implementationVersionName,
			int ourMaximumLengthReceived,
			LinkedList presentationContexts,
			LinkedList scuSCPRoleSelections) throws DicomNetworkException {
		super(0x01,calledAETitle,callingAETitle,implementationClassUID,implementationVersionName,ourMaximumLengthReceived,presentationContexts,scuSCPRoleSelections,0,(byte[])null,(byte[])null);
	}

	/**
	 * @param	pdu
	 * @exception	DicomNetworkException
	 */
	public AssociateRequestPDU(byte[] pdu) throws DicomNetworkException {
		super(pdu);
		if (pduType != 0x01) throw new DicomNetworkException("Unexpected PDU type 0x"+Integer.toHexString(pduType)+" when expecting A-ASSOCIATE-RQ");
	}
}




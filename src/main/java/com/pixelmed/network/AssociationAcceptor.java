/* Copyright (c) 2001-2008, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */
package com.pixelmed.network;

import com.pixelmed.utils.ByteArray;
import com.pixelmed.utils.StringUtilities;

import java.util.LinkedList;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/**
 * @author	dclunie
 */
class AssociationAcceptor extends Association
{
  private static final String identString =
     "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/AssociationAcceptor.java,v 1.4 2011-10-06 17:54:20 nicola Exp $";
  protected PresentationContextSelectionPolicy presentationContextSelectionPolicy;

  protected AssociationAcceptor(int debugLevel)
  {
    super(debugLevel);
  }

  /**
   * Accepts an association on the supplied open transport connection.
   *
   * The default Implementation Class UID, Implementation Version and Maximum PDU Size
   * of the toolkit are used.
   *
   * The open association is left in state 6 - Data Transfer.
   *
   * @param	socket				already open transport connection on which the association is to be accepted
   * @param	calledAETitle			the AE Title of the local (our) end of the association
   * @param	implementationClassUID		the Implementation Class UID of the local (our) end of the association supplied as a User Information Sub-item
   * @param	implementationVersionName	the Implementation Class UID of the local (our) end of the association supplied as a User Information Sub-item
   * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
   * @param	socketReceiveBufferSize		the TCP socket receive buffer size to set (if possible), 0 means leave at the default
   * @param	socketSendBufferSize		the TCP socket send buffer size to set (if possible), 0 means leave at the default
   * @param	presentationContextSelectionPolicy	which SOP Classes and Transfer Syntaxes to accept and reject
   * @param	debugLevel			0 for no debugging, > 0 for increasingly verbose debugging
   * @exception	IOException
   * @exception	DicomNetworkException		thrown for A-ABORT and A-P-ABORT indications
   */
  public void negoziateAssociation(Socket socket, String calledAETitle,
     String implementationClassUID, String implementationVersionName,
     int ourMaximumLengthReceived, int socketReceiveBufferSize, int socketSendBufferSize,
     PresentationContextSelectionPolicy presentationContextSelectionPolicy,
     ArrayList<AssociationListener> arAssocListner)
     throws DicomNetworkException, IOException
  {
    this.socket = socket;
    this.calledAETitle = calledAETitle;
    this.callingAETitle = null;
    this.presentationContexts = null;
    this.presentationContextSelectionPolicy = presentationContextSelectionPolicy;

    try
    {
      // AE-5    - TP Connect Indication
      // State 2 - Transport connection open (Awaiting A-ASSOCIATE-RQ PDU)
      setSocketOptions(socket, ourMaximumLengthReceived, socketReceiveBufferSize, socketSendBufferSize, debugLevel);

      //         - Transport connection confirmed
      in = socket.getInputStream();
      out = socket.getOutputStream();

      byte[] startBuffer = new byte[6];
      //in.read(startBuffer,0,6);	// block for type and length of PDU
      readInsistently(in, startBuffer, 0, 6, "type and length of PDU");
      int pduType = startBuffer[0] & 0xff;
      int pduLength = ByteArray.bigEndianToUnsignedInt(startBuffer, 2, 4);

      if(debugLevel > 1)
        System.err.println("Association[" + associationNumber + "]: Them: PDU Type: 0x" + Integer.toHexString(pduType) + " (length 0x" + Integer.toHexString(pduLength) + ")");

      if(pduType == 0x01)
      {							//           - A-ASSOCIATE-RQ PDU
        // AE-6      - Stop ARTIM and send A-ASSOCIATE indication primitive
        AssociateRequestPDU arq = new AssociateRequestPDU(getRestOfPDU(in, startBuffer, pduLength));
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Them:\n" + arq);

        presentationContexts = arq.getRequestedPresentationContexts();
        maximumLengthReceived = arq.getMaximumLengthReceived();
        callingAETitle =
           StringUtilities.removeLeadingOrTrailingWhitespaceOrISOControl(arq.getCallingAETitle());
        String requestAETitle =
           StringUtilities.removeLeadingOrTrailingWhitespaceOrISOControl(arq.getCalledAETitle());

        // now that we know callingAETitle ...
        // calledAETitle = null mean don't control remote AETitle, all is OK (but report correctly: read it now)
        if(calledAETitle == null)
        {
          this.calledAETitle = calledAETitle = requestAETitle;
        }

        // test the AETitle
        boolean accept = calledAETitle.equals(requestAETitle);

        // if have listner, ask them to accept association
        if(accept && arAssocListner != null)
        {
          for(AssociationListener l : arAssocListner)
          {
            accept = l.acceptAssociation(requestAETitle, callingAETitle, presentationContexts);
            if(!accept)
              break;
          }
        }

        if(!accept)
        {
          //	     - Implicit A-ASSOCIATE response primitive reject
          // AE-8      - Send A-ASSOCIATE-RJ PDU
          AssociateRejectPDU arj = new AssociateRejectPDU(1, 1, 7);	// rejected permanent, user, called AE title not recognized
          out.write(arj.getBytes());
          out.flush();						// State 13

          // At this point AA-6, AA-7, AA-2, AR-5 or AA-7 could be needed,
          // however let's just close the connection and be done with it
          // without worrying about whether the other end is doing the same
          // or has sent a PDU that really should trigger us to send an A-ABORT first
          // and we don't have a timmer to stop
          socket.close();

          // No "indication" is defined in the standard here, but send our own to communicate rejection
          throw new DicomNetworkException("Called AE title requested (" + requestAETitle +
             ") doesn't match ours (" + calledAETitle + ") - rejecting association");
        }

        //	     - Implicit A-ASSOCIATE response primitive accept
        // AE-7      - Send A-ASSOCIATE-AC PDU
        presentationContextSelectionPolicy.applyPresentationContextSelectionPolicy(presentationContexts, associationNumber, debugLevel);

        // we now have presentation contexts with 1 AS, 1TS if any accepted, and a result/reason
        LinkedList presentationContextsForAssociateAcceptPDU =
           AssociateAcceptPDU.sanitizePresentationContextsForAcceptance(presentationContexts);

        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Presentation contexts for A-ASSOCIATE-AC:\n" + presentationContextsForAssociateAcceptPDU);

        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: OurMaximumLengthReceived=" + ourMaximumLengthReceived);

        // just return any selections asked for, assuming that we support them (e.g. SCP role for C-STOREs for C-GET) ...
        LinkedList scuSCPRoleSelections = arq.getSCUSCPRoleSelections();

        AssociateAcceptPDU aac =
           new AssociateAcceptPDU(calledAETitle, callingAETitle, implementationClassUID, implementationVersionName,
           ourMaximumLengthReceived, presentationContextsForAssociateAcceptPDU, scuSCPRoleSelections);

        out.write(aac.getBytes());
        out.flush();						// State 6
      }
      else if(pduType == 0x07)
      {						//           - A-ABORT PDU
        AAbortPDU aab = new AAbortPDU(getRestOfPDU(in, startBuffer, pduLength));
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Them:\n" + aab);

        socket.close();							// AA-2      - Stop ARTIM, close transport connection and indicate abort
        throw new DicomNetworkException("A-ABORT indication - " + aab.getInfo());
        // State 1   - Idle
      }
      else
      {									//           - Invalid or unrecognized PDU received
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Aborting");

        AAbortPDU aab = new AAbortPDU(0, 0);				// AA-1      - Send A-ABORT PDU (service user source, reserved), and start (or restart) ARTIM
        out.write(aab.getBytes());
        out.flush();
        //             issue an A-P-ABORT indication and start ARTIM
        // State 13  - Awaiting Transport connection close
        // should wait for ARTIM but ...
        socket.close();
        throw new DicomNetworkException("A-P-ABORT indication - " + aab.getInfo());
        // State 1   - Idle
      }
    }
    catch(IOException e)
    {								//           - Transport connection closed (or other error)
      throw new DicomNetworkException("A-P-ABORT indication - " + e);		// AA-5      - Stop ARTIM
      // State 1   - Idle
    }

    // falls through only from State 6 - Data Transfer
  }

  /*
   * Returns a string representation of the object.
   *
   * @return	a string representation of the object
   */
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    //sb.append("Association["+associationNumber+"]: Port: "); sb.append(port); sb.append("\n");
    sb.append(super.toString());
    return sb.toString();
  }
}

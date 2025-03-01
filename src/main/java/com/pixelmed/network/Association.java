/* Copyright (c) 2001-2005, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */
package com.pixelmed.network;

import com.pixelmed.utils.ByteArray;
import com.pixelmed.dicom.TransferSyntax;

import java.util.LinkedList;
import java.util.ListIterator;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;

import java.util.Arrays;
import java.util.Date;

/**
 * @author	dclunie
 */
public class Association
{
  private static final String identString =
     "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/Association.java,v 1.2 2011-06-16 11:23:00 nicola Exp $";
  /***/
  private static int associationReleaseToTransportConnectionCloseTimeoutInMilliseconds = 5000;	// should be a property :(
  /***/
  private static int associationCounter = 0;
  /***/
  protected int associationNumber = associationCounter++;		// for use in distinguishing associations in debugging messages
  /***/
  protected int debugLevel;
  /***/
  protected String calledAETitle;
  /***/
  protected String callingAETitle;
  /***/
  protected LinkedList presentationContexts;
  /***/
  protected LinkedList scuSCPRoleSelections;
  /***/
  protected int maximumLengthReceived;
  /***/
  protected Socket socket;
  /***/
  protected InputStream in;
  /***/
  protected OutputStream out;
  protected Date createDate = new Date();
  private ReceivedDataHandler receivedDataHandler;

  /**
   * <p>Set the socket options for either initiator or acceptor.</p>
   *
   * <p>Must be called before using the socket or the options won't set.</p>
   *
   * @param	socket				the socket whose options to set
   * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
   * @param	socketReceiveBufferSize		the TCP socket receive buffer size to set (if possible), 0 means leave at the default
   * @param	socketSendBufferSize		the TCP socket send buffer size to set (if possible), 0 means leave at the default
   * @param	debugLevel			0 for no debugging, > 0 for increasingly verbose debugging
   * @exception	IOException
   */
  protected void setSocketOptions(Socket socket,
     int ourMaximumLengthReceived, int socketReceiveBufferSize, int socketSendBufferSize,
     int debugLevel)
     throws IOException
  {
    if(debugLevel > 1)
      System.err.println("Association[" + associationNumber + "].setSocketOptions(): getReceiveBufferSize() = " + socket.getReceiveBufferSize());
    if(debugLevel > 1)
      System.err.println("Association[" + associationNumber + "].setSocketOptions(): getSendBufferSize() = " + socket.getSendBufferSize());
    if(debugLevel > 1)
      System.err.println("Association[" + associationNumber + "].setSocketOptions(): getSoLinger() = " + socket.getSoLinger());
    if(debugLevel > 1)
      System.err.println("Association[" + associationNumber + "].setSocketOptions(): getSoTimeout() = " + socket.getSoTimeout());
    if(debugLevel > 1)
      System.err.println("Association[" + associationNumber + "].setSocketOptions(): getTcpNoDelay() = " + socket.getTcpNoDelay());

    // Changing the Windows default of 8192 improves performance for
    // sending from glacial to acceptable
    // Linux and Mac send performance were both already acceptable
    // this might have been less of a problem if not using a max pdu size of as high as 16384

    // We do not know yet what their max PDU size is, and once we do we can't change the
    // socket options, so let's assume they can handle a lot to create really big buffers

    // NB. allow to reduce size as well as increase it, to allow for performance tests ...

    if(socketReceiveBufferSize != 0 && socket.getReceiveBufferSize() != socketReceiveBufferSize)
    {
      if(debugLevel > 1)
        System.err.println("Association[" + associationNumber + "].setSocketOptions(): asking to change receiveBufferSize to = " + socketReceiveBufferSize);

      socket.setReceiveBufferSize(socketReceiveBufferSize);

      if(debugLevel > 1)
        System.err.println("Association[" + associationNumber + "].setSocketOptions(): receiveBufferSize changed to = " + socket.getReceiveBufferSize());
    }

    if(socketSendBufferSize != 0 && socket.getSendBufferSize() != socketSendBufferSize)
    {
      if(debugLevel > 1)
        System.err.println("Association[" + associationNumber + "].setSocketOptions(): asking to change sendBufferSize to = " + socketSendBufferSize);

      socket.setSendBufferSize(socketSendBufferSize);

      if(debugLevel > 1)
        System.err.println("Association[" + associationNumber + "].setSocketOptions(): sendBufferSize changed to = " + socket.getSendBufferSize());
    }

    // there is probably no point in downsizing the max PDU size offered,
    // since there is no waiting for acknowledgement anyway, i.e. the
    // PDU fragmentation at the DICOM level (probably) does not interact
    // with the socket buffer or window sizes anyway (??? :( )

    //if (socket.getReceiveBufferSize() < ourMaximumLengthReceived) {
    //	ourMaximumLengthReceived = socket.getReceiveBufferSize();	// reduce the max PDU size offered to what will fit in socket receive buffer
//if (debugLevel > 1) System.err.println("Association["+associationNumber+"].setSocketOptions(): ourMaximumLengthReceived reduced to fit in receive buffer to = "+ourMaximumLengthReceived);
    //}

    //if (!socket.getTcpNoDelay()) {			// don't know if turning this on (Nagle off) really helps or not
    //	socket.setTcpNoDelay(true);
//if (debugLevel > 1) System.err.println("Association["+associationNumber+"].setSocketOptions(): getTcpNoDelay() now = "+socket.getTcpNoDelay());
    //}
  }

  /**
   * @param	in
   * @param	b
   * @param	offset
   * @param	length
   * @param	what
   * @exception	IOException
   * @exception	DicomNetworkException
   */
  protected static void readInsistently(InputStream in, byte[] b, int offset, int length, String what)
     throws DicomNetworkException, IOException
  {
    while(length > 0)
    {
//System.err.println("Association["+associationNumber+"].readInsistently(): looping offset="+offset+" length="+length);
      int bytesReceived = in.read(b, offset, length);
//System.err.println("Association["+associationNumber+"].readInsistently(): asked for ="+length+" received="+bytesReceived);
      if(bytesReceived == -1)
        throw new DicomNetworkException("Connection closed while reading " + what);
      length -= bytesReceived;
      offset += bytesReceived;
    }
  }

  /**
   * @param	in
   * @param	startBuffer
   * @param	pduLength
   * @exception	IOException
   * @exception	DicomNetworkException
   */
  protected static byte[] getRestOfPDU(InputStream in, byte[] startBuffer, int pduLength)
     throws DicomNetworkException, IOException
  {
//System.err.println("Association["+associationNumber+"].getRestOfPDU(): startBuffer.length="+startBuffer.length+" pduLength="+pduLength);
    int lsb = startBuffer.length;
    byte[] b = new byte[pduLength + lsb];
    int offset = 0;
    while(offset < lsb)
    {
      b[offset] = startBuffer[offset];
      ++offset;
    }
    readInsistently(in, b, offset, pduLength, "PDU");
    return b;
  }

  /***/
  protected Association(int debugLevel)
  {
    this.debugLevel = debugLevel;
  }

  /**
   * Send an A-RELEASE-RQ.
   *
   * This is a confirmed service, so a normal return is the A-RELEASE confirmation primitive.
   *
   * @exception	DicomNetworkException
   */
  public void release()
     throws DicomNetworkException
  {
    // State 6   - Data Transfer
    //             A-RELEASE request primitive
    try
    {
      if(debugLevel > 1)
        System.err.println("Association[" + associationNumber + "]: Us: A-RELEASE-RQ");
      AReleasePDU ar = new AReleasePDU(0x05);					// AR-1      - Send A-RELEASE-RQ PDU
      out.write(ar.getBytes());
      out.flush();
      // State 7   - Awaiting A-RELEASE-RP
      byte[] startBuffer = new byte[6];
      //in.read(startBuffer,0,6);	// block for type and length of PDU
      readInsistently(in, startBuffer, 0, 6, "type and length of PDU");
      int pduType = startBuffer[0] & 0xff;
      int pduLength = ByteArray.bigEndianToUnsignedInt(startBuffer, 2, 4);

      if(debugLevel > 1)
        System.err.println("Association[" + associationNumber + "]: Them: PDU Type: 0x" + Integer.toHexString(pduType) + " (length 0x" + Integer.toHexString(pduLength) + ")");

      if(pduType == 0x06)
      {							//           - A-RELEASE-RP PDU
        AReleasePDU arr = new AReleasePDU(getRestOfPDU(in, startBuffer, pduLength));
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Them:\n" + arr);
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Us: close transport connection");
        socket.close();							// AR-3      - Issue A-RELEASE confirmation primitive and close transport connection
        // fall through to normal return
        // State 1   - Idle
      }
      // else if (pduType == 0x04) {}	// P-DATA PDU ... should issue P-DATA indication, then remain in State 7 ... i.e. continue to handle data if it comes
      else if(pduType == 0x05)
      {						//           - A-RELEASE-RQ PDU ... release request collision
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Them: A-RELEASE-RQ (collision)\n");
        // AR-8      - we were the association requester, so go to State 9
        // State 9   -
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Us: A-RELEASE-RP");
        AReleasePDU arr = new AReleasePDU(0x06);			// AR-9      - send a A-RELEASE-RP (wouldn;t if we were the association acceptor)
        out.write(arr.getBytes());
        out.flush();
        // State 11  -
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Us: close transport connection");
        socket.close();							// AR-3      - Issue A-RELEASE confirmation primitive and close transport connection
        // fall through to normal return
      }
      else if(pduType == 0x07)
      {						//           - A-ABORT PDU
        AAbortPDU aab = new AAbortPDU(getRestOfPDU(in, startBuffer, pduLength));
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Them:\n" + aab);
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Us: close transport connection");
        socket.close();							// AA-3      - Close transport connection and indicate abort
        throw new DicomNetworkException("A-ABORT indication - " + aab.getInfo());
        // State 1   - Idle
      }
      else
      {									//           - Invalid or unrecognized PDU received
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Aborting");
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Us: A-ABORT");
        AAbortPDU aab = new AAbortPDU(2, 2);				// AA-8      - Send A-ABORT PDU (service provider source, unexpected PDU)
        out.write(aab.getBytes());
        out.flush();
        //             issue an A-P-ABORT indication and start ARTIM
        waitForARTIMBeforeTransportConnectionClose();			//             start ARTIM
        // State 13  - Awaiting Transport connection close
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Us: close transport connection");
        socket.close();
        throw new DicomNetworkException("A-P-ABORT indication - " + aab.getInfo());
        // State 1   - Idle
      }
    }
    catch(Exception e)
    {								//           - Transport connection closed (or other error)
      e.printStackTrace(System.err);
      try
      {
        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Us: close transport connection");
        socket.close();							// Just in case not already closed
      }
      catch(IOException e2)
      {
      }
      throw new DicomNetworkException("A-P-ABORT indication - " + e);		// AA-4      - indicate A-P-ABORT
      // State 1   - Idle
    }
    // normal return is A-RELEASE confirmation primitive
  }

  /**
   * Send an A-ABORT-RQ.
   *
   * This is an unconfirmed service, so a normal return is expected.
   *
   * @exception	DicomNetworkException
   */
  public void abort()
     throws DicomNetworkException
  {
    // State 6   - Data Transfer
    //             A-ABORT request primitive
    try
    {
      if(debugLevel > 1)
        System.err.println("Association[" + associationNumber + "]: Us: A-ABORT");
      AAbortPDU aab = new AAbortPDU(1, 0);					// AA-1      - Send A-ABORT PDU (service user source, no reason)
      out.write(aab.getBytes());
      out.flush();
      waitForARTIMBeforeTransportConnectionClose();				//             start ARTIM
      // State 13  - Awaiting Transport connection close
      if(debugLevel > 1)
        System.err.println("Association[" + associationNumber + "]: Us: close transport connection");
      socket.close();
      // State 1   - Idle
    }
    catch(Exception e)
    {								//           - Transport connection closed (or other error)
      e.printStackTrace(System.err);
      try
      {
        socket.close();							// Just in case not already closed
      }
      catch(IOException e2)
      {
      }
      throw new DicomNetworkException("A-P-ABORT indication - " + e);		// AA-4      - indicate A-P-ABORT
      // State 1   - Idle
    }
    // is an unconfirmed service
  }

  /**
   * Send a command and/or data in a single PDU, each PDV with the last fragment flag set.
   *
   * @param	presentationContextID	included in the header of each PDU
   * @param	command			the command PDV payload, or null if none
   * @param	data			the data PDV payload, or null if none
   * @exception	DicomNetworkException
   */
  public void send(byte presentationContextID, byte[] command, byte[] data)
     throws DicomNetworkException
  {
    // let's build a single command PDV and a single data PDV (if needed) in a single PDU

    LinkedList listOfPDVs = new LinkedList();
    // 2004/06/08 DAC resolved [bugs.mrmf] (000114) Failing to set last fragment on command when sending command and data in same PDU
    if(command != null)
      listOfPDVs.add(new PresentationDataValue(presentationContextID, command, true, true));	// command, last
    if(data != null)
      listOfPDVs.add(new PresentationDataValue(presentationContextID, data, false, true));		// data, last
    try
    {
      PDataPDU pdu = new PDataPDU(listOfPDVs);
      if(debugLevel > 1)
        System.err.println("Association[" + associationNumber + "].send(): Us: P-DATA-TF=\n" + pdu);
      // should check size less than maximumLengthReceived (maximum PDU size receiver can handle) :(
      byte[] bytes = pdu.getBytes();
      if(bytes.length % 2 != 0)
      {
        // better to catch this internal (i.e. our fault) error here and close than leave it to the discretion of the other end (000524)
        socket.close();
        throw new DicomNetworkException("A-P-ABORT indication - internal error - illegal odd length PDU write requested");	// AA-4      - indicate A-P-ABORT
      }
      out.write(bytes);
      out.flush();
    }
    catch(IOException e)
    {								//           - Transport connection closed (or other error)
      throw new DicomNetworkException("A-P-ABORT indication - " + e);		// AA-4      - indicate A-P-ABORT
      // State 1   - Idle
    }
  }

  /**
   * A factory method to build an {@link AssociationOutputStream AssociationOutputStream}
   * for this Association, on which to send data which is fragmented as appropriate
   * into PDUs.
   *
   * @param	presentationContextID	included in the header of each PDU
   * @exception	DicomNetworkException
   */
  public AssociationOutputStream getAssociationOutputStream(byte presentationContextID)
     throws DicomNetworkException
  {
    return new AssociationOutputStream(out, maximumLengthReceived, presentationContextID, debugLevel);
  }

  /**
   * Register a {@link ReceivedDataHandler ReceivedDataHandler} to handle each PDU
   * as it is received.
   *
   * @param	h	an implementation of the abstract class {@link ReceivedDataHandler ReceivedDataHandler}
   * @exception	DicomNetworkException
   */
  public void setReceivedDataHandler(ReceivedDataHandler h)
     throws DicomNetworkException
  {
    receivedDataHandler = h;
  }

  /**
   * Implement the ARTIM, in order to not close the transport connection immediately after
   * sending an a A-RELEASE-RP or A-ABORT PDU.
   *
   * (E.g., ADW 3.1 reports that a preceding send failed if transport connection is immediately closed).
   *
   * The method is synchronized only in order to allow access to wait().
   *
   */
  private synchronized void waitForARTIMBeforeTransportConnectionClose()
     throws java.lang.InterruptedException
  {
    if(debugLevel > 1)
      System.err.println("Association[" + associationNumber + "]: Waiting to close transport connection.");
    wait(associationReleaseToTransportConnectionCloseTimeoutInMilliseconds);
    if(debugLevel > 1)
      System.err.println("Association[" + associationNumber + "]: Closing transport connection.");
  }

  /**
   * Continue to transfer data (remain in State 6) until the specified number of PDUs have been
   * received or the specified conditions are satisfied.
   *
   * The registered receivedDataHandler is sent a PDataIndication.
   *
   * @param	count				the number of PDUs to be transferred, or -1 if no limit (stop only when conditions satisfied)
   * @param	stopAfterLastFragmentOfCommand	stop after the last fragment of a command has been received
   * @param	stopAfterLastFragmentOfData	stop after the last fragment of data has been received
   * @param	stopAfterHandlerReportsDone	stop after data handler reports that it is done
   * @exception	DicomNetworkException		A-ABORT or A-P-ABORT indication
   * @exception	AReleaseException		A-RELEASE indication; transport connection is closed
   */
  public void waitForPDataPDUs(int count, boolean stopAfterLastFragmentOfCommand,
     boolean stopAfterLastFragmentOfData, boolean stopAfterHandlerReportsDone)
     throws DicomNetworkException, AReleaseException
  {
    while((count == -1 || count-- > 0))
    {						// -1 is flag to loop forever
      // State 6   - Data Transfer
      try
      {
        byte[] startBuffer = new byte[6];
        //in.read(startBuffer,0,6);	// block for type and length of PDU
        readInsistently(in, startBuffer, 0, 6, "type and length of PDU");
        int pduType = startBuffer[0] & 0xff;
        int pduLength = ByteArray.bigEndianToUnsignedInt(startBuffer, 2, 4);

        if(debugLevel > 1)
          System.err.println("Association[" + associationNumber + "]: Them: PDU Type: 0x" + Integer.toHexString(pduType) + " (length " + pduLength + " dec 0x" + Integer.toHexString(pduLength) + ")");

        if(pduType == 0x04)
        {							//           - P-DATA PDU
          PDataPDU pdata = new PDataPDU(getRestOfPDU(in, startBuffer, pduLength));
          if(debugLevel > 2)
            System.err.println("Association[" + associationNumber + "]: Them:\n" + pdata);
          receivedDataHandler.sendPDataIndication(pdata, this);		// DT-2      - send P-DATA indication primitive
          // State 6   - Data Transfer
          if(debugLevel > 2)
            System.err.println("Association[" + associationNumber + "]: stopAfterLastFragmentOfCommand=" + stopAfterLastFragmentOfCommand);
          if(debugLevel > 2)
            System.err.println("Association[" + associationNumber + "]: pdata.containsLastCommandFragment()=" + pdata.containsLastCommandFragment());
          if(debugLevel > 2)
            System.err.println("Association[" + associationNumber + "]: stopAfterLastFragmentOfData=" + stopAfterLastFragmentOfData);
          if(debugLevel > 2)
            System.err.println("Association[" + associationNumber + "]: pdata.containsLastDataFragment()=" + pdata.containsLastDataFragment());
          if(debugLevel > 2)
            System.err.println("Association[" + associationNumber + "]: stopAfterHandlerReportsDone=" + stopAfterHandlerReportsDone);
          if(debugLevel > 2)
            System.err.println("Association[" + associationNumber + "]: receivedDataHandler.isDone()=" + receivedDataHandler.isDone());
          if((stopAfterLastFragmentOfCommand && pdata.containsLastCommandFragment())
             || (stopAfterLastFragmentOfData && pdata.containsLastDataFragment())
             || (stopAfterHandlerReportsDone && receivedDataHandler.isDone()))
          {
            if(debugLevel > 2)
              System.err.println("Association[" + associationNumber + "]: waitForPDataPDUs is stopping");
            break;
          }
        }
        else if(pduType == 0x05)
        {						//           - A-RELEASE-RQ PDU
          if(debugLevel > 1)
            System.err.println("Association[" + associationNumber + "]: Them: A-RELEASE-RQ");
          // AR-2      - Issue A-RELEASE indication primitive
          // State 8   - Awaiting local A-RELEASE response primitive
          //           - Assume local A-RELEASE response primitive
          if(debugLevel > 1)
            System.err.println("Association[" + associationNumber + "]: Us: A-RELEASE-RP");
          AReleasePDU arr = new AReleasePDU(0x06);			// AR-4      - send a A-RELEASE-RP (and start ARTIM)
          out.write(arr.getBytes());
          out.flush();
          if(debugLevel > 1)
            System.err.println("Association[" + associationNumber + "]: Awaiting Transport connection close");
          // State 13  - Awaiting Transport connection close
          waitForARTIMBeforeTransportConnectionClose();
          if(debugLevel > 1)
            System.err.println("Association[" + associationNumber + "]: Us: close transport connection");
          socket.close();							// AR-4      - Issue A-RELEASE confirmation primitive and close transport connection
          throw new AReleaseException("A-RELEASE indication while waiting for P-DATA");
        }
        else if(pduType == 0x07)
        {						//           - A-ABORT PDU
          AAbortPDU aab = new AAbortPDU(getRestOfPDU(in, startBuffer, pduLength));
          if(debugLevel > 1)
            System.err.println("Association[" + associationNumber + "]: Them:\n" + aab);
          if(debugLevel > 1)
            System.err.println("Association[" + associationNumber + "]: Us: close transport connection");
          socket.close();							// AA-3      - Close transport connection and indicate abort
          throw new DicomNetworkException("A-ABORT indication - " + aab.getInfo());
          // State 1   - Idle
        }
        else
        {									//           - Invalid or unrecognized PDU received
          if(debugLevel > 1)
            System.err.println("Association[" + associationNumber + "]: Aborting");
          if(debugLevel > 1)
            System.err.println("Association[" + associationNumber + "]: Us: A-ABORT");

          AAbortPDU aab = new AAbortPDU(2, 2);				// AA-8      - Send A-ABORT PDU (service provider source, unexpected PDU)
          out.write(aab.getBytes());
          out.flush();
          //             issue an A-P-ABORT indication and start ARTIM
          // State 13  - Awaiting Transport connection close
          waitForARTIMBeforeTransportConnectionClose();
          if(debugLevel > 1)
            System.err.println("Association[" + associationNumber + "]: Us: close transport connection");
          socket.close();
          
          throw new DicomNetworkException("A-P-ABORT indication - " + aab.getInfo());
          // State 1   - Idle
        }
      }
      catch(AReleaseException e)
      {
        // quiet ... this is the normal association release ... propagate it upwards
        throw new AReleaseException(e.toString());				// State 1   - Idle
      }
      catch(Exception e)
      {								//           - Transport connection closed (or other error)
        e.printStackTrace(System.err);
        try
        {
          socket.close();							// Just in case not already closed
        }
        catch(IOException e2)
        {
        }
        throw new DicomNetworkException("A-P-ABORT indication - " + e);		// AA-4      - indicate A-P-ABORT
        // State 1   - Idle
      }
      // normal return is after all requested P-DATA PDUs have been received, still in State 6
    }
  }

  /**
   * Continue to transfer data (remain in State 6) until one PDU has been
   * received.
   *
   * The registered receivedDataHandler is sent a PDataIndication.
   *
   * @exception	DicomNetworkException		A-ABORT or A-P-ABORT indication
   * @exception	AReleaseException		A-RELEASE indication; transport connection is closed
   */
  public void waitForOnePDataPDU() throws DicomNetworkException, AReleaseException
  {
    waitForPDataPDUs(1, false, false, false);
  }

  /**
   * Continue to transfer data (remain in State 6) until the last fragment of a command has been received.
   *
   * The registered receivedDataHandler is sent a PDataIndication.
   *
   * @exception	DicomNetworkException		A-ABORT or A-P-ABORT indication
   * @exception	AReleaseException		A-RELEASE indication; transport connection is closed
   */
  public void waitForCommandPDataPDUs() throws DicomNetworkException, AReleaseException
  {
    waitForPDataPDUs(-1, true, false, false);
  }

  /**
   * Continue to transfer data (remain in State 6) until the last fragment of data has been received.
   *
   * The registered receivedDataHandler is sent a PDataIndication.
   *
   * @exception	DicomNetworkException		A-ABORT or A-P-ABORT indication
   * @exception	AReleaseException		A-RELEASE indication; transport connection is closed
   */
  public void waitForDataPDataPDUs() throws DicomNetworkException, AReleaseException
  {
    waitForPDataPDUs(-1, false, true, false);
  }

  /**
   * Continue to transfer data (remain in State 6) until the data handler reports that it is done.
   *
   * The registered receivedDataHandler is sent a PDataIndication.
   *
   * @exception	DicomNetworkException		A-ABORT or A-P-ABORT indication
   * @exception	AReleaseException		A-RELEASE indication; transport connection is closed
   */
  public void waitForPDataPDUsUntilHandlerReportsDone() throws DicomNetworkException, AReleaseException
  {
    waitForPDataPDUs(-1, false, false, true);
  }

  /**
   * Find a Presentation Context for the a particular SOP Class UID, using any
   * available Transfer Syntax but preferring compressed then, Explicit VR Little Endian, then
   * any Explicit VR, over Implicit VR.
   *
   * @param	abstractSyntaxUID	the SOP Class UID for which to find a suitable Presentation Context
   * @return				the Presentation Context ID of a suitable Presentation Context
   * @exception	DicomNetworkException	thrown if no suitable Presentation Context
   */
  public byte getSuitablePresentationContextID(String abstractSyntaxUID) throws DicomNetworkException
  {
    ListIterator i = null;
    byte useID = 0;
    // First try and find a compressed transfer syntax ...
    if(useID == 0)
    {
      i = presentationContexts.listIterator();
      while(i.hasNext())
      {
        PresentationContext pc = (PresentationContext) i.next();
        if(pc.getAbstractSyntaxUID().equals(abstractSyntaxUID))
        {
          String uid = pc.getTransferSyntaxUID();
          if(uid != null && uid.equals(TransferSyntax.PixelMedBzip2ExplicitVRLittleEndian))
          {
            useID = pc.getIdentifier();
          }
        }
      }
    }
    if(useID == 0)
    {
      i = presentationContexts.listIterator();
      while(i.hasNext())
      {
        PresentationContext pc = (PresentationContext) i.next();
        if(pc.getAbstractSyntaxUID().equals(abstractSyntaxUID))
        {
          String uid = pc.getTransferSyntaxUID();
          if(uid != null && uid.equals(TransferSyntax.DeflatedExplicitVRLittleEndian))
          {
            useID = pc.getIdentifier();
          }
        }
      }
    }
    // Else try and find an Explicit VR Little Endian transfer syntax ...
    if(useID == 0)
    {
      i = presentationContexts.listIterator();
      while(i.hasNext())
      {
        PresentationContext pc = (PresentationContext) i.next();
        if(pc.getAbstractSyntaxUID().equals(abstractSyntaxUID))
        {
          String uid = pc.getTransferSyntaxUID();
          if(uid != null && TransferSyntax.isExplicitVR(uid) && TransferSyntax.isLittleEndian(uid))
          {
            useID = pc.getIdentifier();
          }
        }
      }
    }
    // else try and find an Explicit VR transfer syntax of any kind (we wouldn't have accepted those we don't support by now) ...
    if(useID == 0)
    {
      i = presentationContexts.listIterator();
      while(i.hasNext())
      {
        PresentationContext pc = (PresentationContext) i.next();
        if(pc.getAbstractSyntaxUID().equals(abstractSyntaxUID))
        {
          String uid = pc.getTransferSyntaxUID();
          if(uid != null && TransferSyntax.isExplicitVR(uid))
          {
            useID = pc.getIdentifier();
          }
        }
      }
    }
    // Else take whatever we can get ...
    if(useID == 0)
    {
      i = presentationContexts.listIterator();
      while(i.hasNext())
      {
        PresentationContext pc = (PresentationContext) i.next();
        if(pc.getAbstractSyntaxUID().equals(abstractSyntaxUID))
        {
          useID = pc.getIdentifier();
        }
      }
    }
    if(useID != 0)
    {
      return useID;
    }
    else
    {
      throw new DicomNetworkException("No presentation context for Abstract Syntax " + abstractSyntaxUID);
    }
  }

  /**
   * Find a Presentation Context for a particular combination of SOP Class UID and Transfer Syntax.
   *
   * @param	abstractSyntaxUID	the SOP Class UID for which to find a suitable Presentation Context
   * @param	transferSyntaxUID	the Transfer Syntax UID for which to find a suitable Presentation Context
   * @return				the Presentation Context ID of a suitable Presentation Context
   * @exception	DicomNetworkException	thrown if no suitable Presentation Context
   */
  public byte getSuitablePresentationContextID(String abstractSyntaxUID, String transferSyntaxUID) throws DicomNetworkException
  {
    ListIterator i = presentationContexts.listIterator();
    while(i.hasNext())
    {
      PresentationContext pc = (PresentationContext) i.next();
      if(pc.getAbstractSyntaxUID().equals(abstractSyntaxUID) && pc.getTransferSyntaxUID().equals(transferSyntaxUID))
      {
        return pc.getIdentifier();
      }
    }
    throw new DicomNetworkException("No presentation context for Abstract Syntax " + abstractSyntaxUID + " and Transfer Syntax " + transferSyntaxUID);
  }

  /**
   * Get the Transfer Syntax UID of the Presentation Context specified by the Presentation Context ID.
   *
   * @param	identifier		the Presentation Context ID
   * @return				the only or first Transfer Syntax UID
   * @exception	DicomNetworkException	thrown if no such Presentation Context or no Transfer Syntax for that Presentation Context (e.g. it was rejected)
   */
  public String getTransferSyntaxForPresentationContextID(byte identifier) throws DicomNetworkException
  {
    ListIterator i = presentationContexts.listIterator();
    while(i.hasNext())
    {
      PresentationContext pc = (PresentationContext) i.next();
      if(pc.getIdentifier() == identifier)
      {
        return pc.getTransferSyntaxUID();	// the first if more than one
      }
    }
    throw new DicomNetworkException("No such presentation context as " + Integer.toHexString(identifier & 0xff));
  }

  /***/
  public int getAssociationNumber()
  {
    return associationNumber;
  }

  /***/
  public String getCalledAETitle()
  {
    return calledAETitle;
  }

  /***/
  public String getCallingAETitle()
  {
    return callingAETitle;
  }

  public Date getCreateTime()
  {
    return createDate;
  }

  /*
   * Returns a string representation of the object.
   *
   * @return	a string representation of the object
   */
  @Override
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("Association[" + associationNumber + "]: Called AE Title:  ");
    sb.append(calledAETitle);
    sb.append("\n");
    sb.append("Association[" + associationNumber + "]: Calling AE Title: ");
    sb.append(callingAETitle);
    sb.append("\n");
    sb.append(presentationContexts);
    return sb.toString();
  }
  // TLS stuff ...
  private static final String cipherSuiteForAES = "TLS_RSA_WITH_AES_128_CBC_SHA";
  //private static final String cipherSuiteFor3DES = "TLS_RSA_WITH_3DES_EDE_CBC_SHA";
  private static final String cipherSuiteFor3DES = "SSL_RSA_WITH_3DES_EDE_CBC_SHA";			// request the cipher suite named SSL_ even though only TLSv1 will be used ... does actually work

  static final String[] getCipherSuitesToEnable(String suites[])
  {
    String[] enable = null;
    if(suites != null)
    {
      //for (int i=0; i<suites.length; ++i) {
      //	System.err.println("getCipherSuitesToEnable() supported suite "+suites[i]);
      //}
      boolean useAES = Arrays.asList(suites).contains(cipherSuiteForAES);
      System.err.println("getCipherSuitesToEnable() useAES " + useAES);
      boolean use3DES = Arrays.asList(suites).contains(cipherSuiteFor3DES);
      System.err.println("getCipherSuitesToEnable() use3DES " + use3DES);
      if(useAES && !use3DES)
      {
        String[] s =
        {
          cipherSuiteForAES
        };
        enable = s;
      }
      else if(!useAES && use3DES)
      {
        String[] s =
        {
          cipherSuiteFor3DES
        };
        enable = s;
      }
      else if(useAES && use3DES)
      {
        String[] s =
        {
          cipherSuiteForAES, cipherSuiteFor3DES
        };
        enable = s;
      }
    }
    return enable;
  }
  private static final String protocolForTLS = "TLSv1";

  static final String[] getProtocolsToEnable(String protocols[])
  {
    String[] enable = null;
    if(protocols != null)
    {
      //for (int i=0; i<protocols.length; ++i) {
      //	System.err.println("getProtocolsToEnable() supported protocol "+protocols[i]);
      //}
      boolean useTLS = Arrays.asList(protocols).contains(protocolForTLS);
      System.err.println("getProtocolsToEnable() useTLS " + useTLS);
      if(useTLS)
      {
        String[] s =
        {
          protocolForTLS
        };
        enable = s;
      }
    }
    return enable;
  }
}

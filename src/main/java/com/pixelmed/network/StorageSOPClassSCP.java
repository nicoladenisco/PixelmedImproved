/* Copyright (c) 2001-2006, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */
package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;
import com.pixelmed.query.QueryResponseGenerator;
import com.pixelmed.query.QueryResponseGeneratorFactory;
import com.pixelmed.query.RetrieveResponseGenerator;
import com.pixelmed.query.RetrieveResponseGeneratorFactory;

import java.util.ListIterator;
import java.util.LinkedList;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/**
 * <p>This class implements the SCP role of SOP Classes of the Storage Service Class,
 * the Study Root Query Retrieve Information Model Find, Get and Move SOP Classes,
 * and the Verification SOP Class.</p>
 *
 * <p>The class has a constructor and a <code>run()</code> method. The
 * constructor is passed a socket on which has been received a transport
 * connection open indication. The <code>run()</code> method waits for an association to be initiated
 * (i.e. acts as an association acceptor), then waits for storage or
 * verification commands, storing data sets in Part 10 files in the specified folder.</p>
 *
 * <p>Debugging messages with a varying degree of verbosity can be activated.</p>
 *
 * <p>This class is not normally used directly, but rather is instantiated by the
 * {@link com.pixelmed.network.StorageSOPClassSCPDispatcher StorageSOPClassSCPDispatcher},
 * which takes care of listening for transport connection open indications, and
 * creates new threads and starts them to handle each incoming association request.</p>
 *
 * @see com.pixelmed.network.StorageSOPClassSCPDispatcher
 *
 * @author	dclunie
 */
public class StorageSOPClassSCP extends SOPClass implements Runnable
{
  /***/
  private static final String identString =
     "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/StorageSOPClassSCP.java,v 1.4 2011-10-03 18:50:08 nicola Exp $";
  /***/
  protected ArrayList<AssociationListener> arAssocListner = new ArrayList<AssociationListener>();

  /***/
  private class CompositeCommandReceivedPDUHandler extends ReceivedDataHandler
  {
    /***/
    private int command;
    /***/
    private byte[] commandReceived;
    /***/
    private AttributeList commandList;
    /***/
    private byte[] dataReceived;
    /***/
    private AttributeList dataList;
    /***/
    private OutputStream out;
    /***/
    private CStoreRequestCommandMessage csrq;
    /***/
    private CEchoRequestCommandMessage cerq;
    /***/
    private CFindRequestCommandMessage cfrq;
    /***/
    private CMoveRequestCommandMessage cmrq;
    /***/
    private CGetRequestCommandMessage cgrq;
    /***/
    private byte[] response;
    /***/
    private byte presentationContextIDUsed;
    //private Association association;
    /***/
    private File receivedFile;
    /***/
    private File temporaryReceivedFile;
    /***/
    private File savedImagesFolder;
    /***/
    private QueryResponseGeneratorFactory queryResponseGeneratorFactory;
    /***/
    private RetrieveResponseGeneratorFactory retrieveResponseGeneratorFactory;

    /**
     * @exception	IOException
     * @exception	DicomException
     */
    private void buildCEchoResponse() throws DicomException, IOException
    {
      response = new CEchoResponseCommandMessage(
         cerq.getAffectedSOPClassUID(),
         cerq.getMessageID(),
         ResponseStatus.Success).getBytes();
    }

    /**
     * @exception	IOException
     * @exception	DicomException
     */
    private void buildCStoreResponse() throws DicomException, IOException
    {
      response = new CStoreResponseCommandMessage(
         csrq.getAffectedSOPClassUID(),
         csrq.getAffectedSOPInstanceUID(),
         csrq.getMessageID(),
         ResponseStatus.Success).getBytes();
    }

    /**
     * @param	savedImagesFolder		null if we do not want to actually save received data (i.e., we want to discard it for testing)
     * @param	queryResponseGeneratorFactory		a factory to make handlers to generate query responses from a supplied query message
     * @param	retrieveResponseGeneratorFactory	a factory to make handlers to generate retrieve responses from a supplied retrieve message
     * @param	debugLevel
     */
    public CompositeCommandReceivedPDUHandler(File savedImagesFolder, QueryResponseGeneratorFactory queryResponseGeneratorFactory, RetrieveResponseGeneratorFactory retrieveResponseGeneratorFactory, int debugLevel)
    {
      super(debugLevel);
      command = MessageServiceElementCommand.NOCOMMAND;
      commandReceived = null;
      commandList = null;
      dataReceived = null;
      dataList = null;
      out = null;
      csrq = null;
      receivedFile = null;
      this.savedImagesFolder = savedImagesFolder;
      this.queryResponseGeneratorFactory = queryResponseGeneratorFactory;
      this.retrieveResponseGeneratorFactory = retrieveResponseGeneratorFactory;
    }

    private class CMovePendingResponseSender extends MultipleInstanceTransferStatusHandler
    {
      private Association association;
      private CMoveRequestCommandMessage cmrq;
      int nRemaining;
      int nCompleted;
      int nFailed;
      int nWarning;

      CMovePendingResponseSender(Association association, CMoveRequestCommandMessage cmrq)
      {
        this.association = association;
        this.cmrq = cmrq;
        nRemaining = 0;
        nCompleted = 0;
        nFailed = 0;
        nWarning = 0;
      }

      public void updateStatus(int nRemaining, int nCompleted, int nFailed, int nWarning, String sopInstanceUID)
      {
        this.nRemaining = nRemaining;
        this.nCompleted = nCompleted;
        this.nFailed = nFailed;
        this.nWarning = nWarning;
        if(debugLevel > 0)
          System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.CMovePendingResponseSender.updateStatus(): Bulding C-MOVE pending response");
        if(nRemaining > 0)
        {
          try
          {
            byte cMovePendingResponseCommandMessage[] = new CMoveResponseCommandMessage(
               cmrq.getAffectedSOPClassUID(),
               cmrq.getMessageID(),
               ResponseStatus.SubOperationsAreContinuing, // status is pending
               false, // no dataset
               nRemaining, nCompleted, nFailed, nWarning).getBytes();
            if(debugLevel > 1)
              System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.CMovePendingResponseSender.updateStatus(): C-MOVE pending response = " + CompositeResponseHandler.dumpAttributeListFromCommandOrData(cMovePendingResponseCommandMessage, TransferSyntax.Default));

            byte presentationContextIDForResponse =
               association.getSuitablePresentationContextID(cmrq.getAffectedSOPClassUID());
            association.send(presentationContextIDForResponse, cMovePendingResponseCommandMessage, null);
          }
          catch(DicomNetworkException e)
          {
            e.printStackTrace(System.err);
          }
          catch(DicomException e)
          {
            e.printStackTrace(System.err);
          }
          catch(IOException e)
          {
            e.printStackTrace(System.err);
          }
        }
        // else do not send pending message if nothing remaining; just update counts
      }
    }

    private class CGetPendingResponseSender extends MultipleInstanceTransferStatusHandler
    {
      private Association association;
      private CGetRequestCommandMessage cgrq;
      int nRemaining;
      int nCompleted;
      int nFailed;
      int nWarning;

      CGetPendingResponseSender(Association association, CGetRequestCommandMessage cgrq)
      {
        this.association = association;
        this.cgrq = cgrq;
        nRemaining = 0;
        nCompleted = 0;
        nFailed = 0;
        nWarning = 0;
      }

      public void updateStatus(int nRemaining, int nCompleted, int nFailed, int nWarning, String sopInstanceUID)
      {
        this.nRemaining = nRemaining;
        this.nCompleted = nCompleted;
        this.nFailed = nFailed;
        this.nWarning = nWarning;
        if(debugLevel > 0)
          System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.CGetPendingResponseSender.updateStatus(): Bulding C-GET pending response");
        if(nRemaining > 0)
        {
          try
          {
            byte cGetPendingResponseCommandMessage[] = new CGetResponseCommandMessage(
               cgrq.getAffectedSOPClassUID(),
               cgrq.getMessageID(),
               ResponseStatus.SubOperationsAreContinuing, // status is pending
               false, // no dataset
               nRemaining, nCompleted, nFailed, nWarning).getBytes();
            if(debugLevel > 1)
              System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.CGetPendingResponseSender.updateStatus(): C-GET pending response = " + CompositeResponseHandler.dumpAttributeListFromCommandOrData(cGetPendingResponseCommandMessage, TransferSyntax.Default));

            byte presentationContextIDForResponse =
               association.getSuitablePresentationContextID(cgrq.getAffectedSOPClassUID());
            association.send(presentationContextIDForResponse, cGetPendingResponseCommandMessage, null);
          }
          catch(DicomNetworkException e)
          {
            e.printStackTrace(System.err);
          }
          catch(DicomException e)
          {
            e.printStackTrace(System.err);
          }
          catch(IOException e)
          {
            e.printStackTrace(System.err);
          }
        }
        // else do not send pending message if nothing remaining; just update counts
      }
    }

//long startReceivedFile;
//long wroteMetaReceivedFile;
//long wroteLastFragmentReceivedFile;
//long endReceivedFile;
//long accumulatedWritePDVTime;
    /**
     * @param	pdata
     * @param	association
     * @exception	IOException
     * @exception	DicomException
     * @exception	DicomNetworkException
     */
    public void sendPDataIndication(PDataPDU pdata, Association association)
       throws DicomNetworkException, DicomException, IOException
    {
      if(debugLevel > 0)
        System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): sendPDataIndication()");
      if(debugLevel > 2)
        super.dumpPDVList(pdata.getPDVList());
      if(debugLevel > 2)
        System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): finished dumping PDV list from PDU");
      // append to command ...
      LinkedList pdvList = pdata.getPDVList();
      ListIterator i = pdvList.listIterator();
      while(i.hasNext())
      {
        if(debugLevel > 0)
          System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): have another fragment");

        PresentationDataValue pdv = (PresentationDataValue) i.next();
        presentationContextIDUsed = pdv.getPresentationContextID();

        if(pdv.isCommand())
        {
          receivedFile = null;
          commandReceived = ByteArray.concatenate(commandReceived, pdv.getValue());	// handles null cases
          if(pdv.isLastFragment())
          {
            if(debugLevel > 0)
              System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): last fragment of data seen");
            if(debugLevel > 0)
              System.err.println(HexDump.dump(commandReceived));
            commandList = new AttributeList();
            commandList.read(new DicomInputStream(new ByteArrayInputStream(commandReceived), TransferSyntax.Default, false));
            if(debugLevel > 0)
              System.err.print(commandList);

            command =
               Attribute.getSingleIntegerValueOrDefault(commandList, TagFromName.CommandField, 0xffff);

            try
            {
              for(AssociationListener l : arAssocListner)
                l.notifyRequestCommand(association, command);
            }
            catch(AReleaseException ex)
            {
              command = 0xffff;
            }

            if(command == MessageServiceElementCommand.C_ECHO_RQ)
            {	// C-ECHO-RQ
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): C-ECHO-RQ");
              cerq = new CEchoRequestCommandMessage(commandList);
              buildCEchoResponse();
              setDone(true);
              setRelease(false);
            }
            else if(command == MessageServiceElementCommand.C_STORE_RQ)
            {
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): C-STORE-RQ");
              csrq = new CStoreRequestCommandMessage(commandList);
            }
            else if(command == MessageServiceElementCommand.C_FIND_RQ && queryResponseGeneratorFactory != null)
            {
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): C-FIND-RQ");
              cfrq = new CFindRequestCommandMessage(commandList);
            }
            else if(command == MessageServiceElementCommand.C_MOVE_RQ && retrieveResponseGeneratorFactory != null)
            {
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): C-MOVE-RQ");
              cmrq = new CMoveRequestCommandMessage(commandList);
            }
            else if(command == MessageServiceElementCommand.C_GET_RQ && retrieveResponseGeneratorFactory != null)
            {
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): C-GET-RQ");
              cgrq = new CGetRequestCommandMessage(commandList);
            }
            else
            {
              throw new DicomNetworkException("Unexpected command 0x" + Integer.toHexString(command) + " " + MessageServiceElementCommand.toString(command));
            }

            // 2004/06/08 DAC removed break that was here to resolve [bugs.mrmf] (000113) StorageSCP failing when data followed command in same PDU
            if(debugLevel > 0 && i.hasNext())
              System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler: Data after command in same PDU");
          }
        }
        else
        {
          try
          {
            for(AssociationListener l : arAssocListner)
              l.notifyRequestCommand(association, command);
          }
          catch(AReleaseException ex)
          {
            command = 0xffff;
          }

          if(command == MessageServiceElementCommand.C_STORE_RQ)
          {
            if(debugLevel > 0)
              System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Storing data fragment");
            if(out == null && savedImagesFolder != null)
            {		// lazy opening
//startReceivedFile=System.currentTimeMillis();
//accumulatedWritePDVTime=0;
              FileMetaInformation fmi = new FileMetaInformation(
                 csrq.getAffectedSOPClassUID(),
                 csrq.getAffectedSOPInstanceUID(),
                 association.getTransferSyntaxForPresentationContextID(presentationContextIDUsed),
                 association.getCallingAETitle());
              temporaryReceivedFile =
                 new File(savedImagesFolder, FileUtilities.makeTemporaryFileName());
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Receiving and storing " + receivedFile);
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Receiving and storing into temporary " + temporaryReceivedFile);
              out = new BufferedOutputStream(new FileOutputStream(temporaryReceivedFile));
              DicomOutputStream dout =
                 new DicomOutputStream(out, TransferSyntax.ExplicitVRLittleEndian, null);
              fmi.getAttributeList().write(dout);
              dout.flush();
//wroteMetaReceivedFile=System.currentTimeMillis();
            }
            if(out != null)
            {
//long startWritePDV=System.currentTimeMillis();
              out.write(pdv.getValue());
//accumulatedWritePDVTime+=(System.currentTimeMillis()-startWritePDV);
            }
            if(pdv.isLastFragment())
            {
//wroteLastFragmentReceivedFile=System.currentTimeMillis();
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Finished storing data");
              if(out != null)
              {
                out.close();

                receivedFile = storedFilePathStrategy.makeReliableStoredFilePathWithFoldersCreated(association,
                   savedImagesFolder, csrq.getAffectedSOPInstanceUID());

                if(!temporaryReceivedFile.renameTo(receivedFile))
                {
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Could not move temporary file into place ... copying instead");
                  CopyStream.copy(temporaryReceivedFile, receivedFile);
                  temporaryReceivedFile.delete();
                }
                out = null;
//endReceivedFile=System.currentTimeMillis();
//System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): time until metaheader written    "+(wroteMetaReceivedFile-startReceivedFile)+" ms");
//System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): time until last fragment written "+(wroteLastFragmentReceivedFile-startReceivedFile)+" ms");
//System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): time until file stored           "+(endReceivedFile-startReceivedFile)+" ms");
//System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): accumulated write PDV time       "+accumulatedWritePDVTime+" ms");
              }
              buildCStoreResponse();
              setDone(true);
              setRelease(false);
            }
          }
          else if(command == MessageServiceElementCommand.C_FIND_RQ && queryResponseGeneratorFactory != null)
          {
            QueryResponseGenerator queryResponseGenerator =
               queryResponseGeneratorFactory.newInstance();
            dataReceived = ByteArray.concatenate(dataReceived, pdv.getValue());	// handles null cases
            if(pdv.isLastFragment())
            {
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): last fragment of data seen");
              if(debugLevel > 0)
                System.err.println(HexDump.dump(dataReceived));
              dataList = new AttributeList();
              dataList.read(new DicomInputStream(new ByteArrayInputStream(dataReceived),
                 association.getTransferSyntaxForPresentationContextID(presentationContextIDUsed), false));
              if(debugLevel > 0)
                System.err.print(dataList);
              queryResponseGenerator.performQuery(cfrq.getAffectedSOPClassUID(), dataList, false/*relational*/);
              int status = queryResponseGenerator.getStatus();
              if(status != ResponseStatus.Success)
              {
                if(debugLevel > 0)
                  System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Query failed, status = 0x" + Integer.toHexString(status));
                response = new CFindResponseCommandMessage(
                   cfrq.getAffectedSOPClassUID(),
                   cfrq.getMessageID(),
                   status,
                   false, // no dataset
                   queryResponseGenerator.getOffendingElement(),
                   null // no ErrorComment
                   ).getBytes();
                queryResponseGenerator.close();
              }
              else
              {
                AttributeList responseIdentifierList;
                while((responseIdentifierList = queryResponseGenerator.next()) != null)
                {
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Building and sending pending response " + responseIdentifierList.toString());
                  byte presentationContextIDForResponse =
                     association.getSuitablePresentationContextID(cfrq.getAffectedSOPClassUID());
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Using context ID for response " + presentationContextIDForResponse);
                  byte cFindResponseCommandMessage[] = new CFindResponseCommandMessage(
                     cfrq.getAffectedSOPClassUID(),
                     cfrq.getMessageID(),
                     (queryResponseGenerator.allOptionalKeysSuppliedWereSupported()
                      ? ResponseStatus.MatchesAreContinuingOptionalKeysSupported
                      : ResponseStatus.MatchesAreContinuingOptionalKeysNotSupported), // pending
                     //ResponseStatus.MatchesAreContinuingOptionalKeysSupported,	// pending ... temporary workaround for [bugs.mrmf] (000213) K-PACS freaked out by valid unsupported optional keys pending response during C-FIND
                     true // dataset present
                     ).getBytes();
                  byte cFindIdentifier[] =
                     new IdentifierMessage(
                     responseIdentifierList,
                     association.getTransferSyntaxForPresentationContextID(presentationContextIDForResponse)).getBytes();
                  //association.setReceivedDataHandler(new CXXXXResponseHandler(debugLevel));
                  association.send(presentationContextIDForResponse, cFindResponseCommandMessage, null);
                  association.send(presentationContextIDForResponse, null, cFindIdentifier);
                }
                queryResponseGenerator.close();
                if(debugLevel > 0)
                  System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Bulding final C-FIND success response");
                response = new CFindResponseCommandMessage(
                   cfrq.getAffectedSOPClassUID(),
                   cfrq.getMessageID(),
                   ResponseStatus.Success, // success status matching is complete
                   false // no dataset
                   ).getBytes();
              }
              setDone(true);
              setRelease(false);
            }
          }
          else if(command == MessageServiceElementCommand.C_MOVE_RQ && retrieveResponseGeneratorFactory != null && applicationEntityMap != null)
          {
            RetrieveResponseGenerator retrieveResponseGenerator =
               retrieveResponseGeneratorFactory.newInstance();
            dataReceived = ByteArray.concatenate(dataReceived, pdv.getValue());	// handles null cases
            if(pdv.isLastFragment())
            {
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): last fragment of data seen");
              if(debugLevel > 0)
                System.err.println(HexDump.dump(dataReceived));
              dataList = new AttributeList();
              dataList.read(new DicomInputStream(new ByteArrayInputStream(dataReceived),
                 association.getTransferSyntaxForPresentationContextID(presentationContextIDUsed), false));
              if(debugLevel > 0)
                System.err.print(dataList);
              retrieveResponseGenerator.performRetrieve(cmrq.getAffectedSOPClassUID(), dataList, false/*relational*/);
              SetOfDicomFiles dicomFiles = retrieveResponseGenerator.getDicomFiles();
              int status = retrieveResponseGenerator.getStatus();
              retrieveResponseGenerator.close();
              if(status != ResponseStatus.Success || dicomFiles == null)
              {
                if(debugLevel > 0)
                  System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): retrieve failed or contains nothing, status = 0x" + Integer.toHexString(status));
                response = new CMoveResponseCommandMessage(
                   cmrq.getAffectedSOPClassUID(),
                   cmrq.getMessageID(),
                   status,
                   false, // no dataset
                   retrieveResponseGenerator.getOffendingElement(),
                   null // no ErrorComment
                   ).getBytes();
              }
              else
              {
                CMovePendingResponseSender pendingResponseSender =
                   new CMovePendingResponseSender(association, cmrq);
                pendingResponseSender.nRemaining = dicomFiles.size();		// in case fails immediately with no status updates

                String moveDestinationAETitle = cmrq.getMoveDestination();
                PresentationAddress moveDestinationPresentationAddress =
                   applicationEntityMap.getPresentationAddress(moveDestinationAETitle);
                if(moveDestinationPresentationAddress != null)
                {
                  String moveDestinationHostname = moveDestinationPresentationAddress.getHostname();
                  int moveDestinationPort = moveDestinationPresentationAddress.getPort();
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): moveDestinationAETitle=" + moveDestinationAETitle);
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): moveDestinationHostname=" + moveDestinationHostname);
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): moveDestinationPort=" + moveDestinationPort);
                  {
                    new StorageSOPClassSCU(
                       moveDestinationHostname,
                       moveDestinationPort,
                       moveDestinationAETitle, // the C-STORE called AET
                       calledAETitle, // use ourselves (the C-MOVE called AET) as the C-STORE calling AET
                       dicomFiles,
                       0, // compressionLevel
                       pendingResponseSender,
                       calledAETitle, // use ourselves (the C-MOVE called AET) as the MoveOriginatorApplicationEntityTitle
                       cmrq.getMessageID(), // MoveOriginatorMessageID
                       debugLevel);
                  }
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): after all stored: nRemaining=" + pendingResponseSender.nRemaining + " nCompleted=" + pendingResponseSender.nCompleted + " nFailed=" + pendingResponseSender.nFailed + " nWarning=" + pendingResponseSender.nWarning);
                  if(pendingResponseSender.nRemaining > 0)
                  {
                    pendingResponseSender.nFailed += pendingResponseSender.nRemaining;
                    pendingResponseSender.nRemaining = 0;
                  }
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): after setting remaining to zero: nRemaining=" + pendingResponseSender.nRemaining + " nCompleted=" + pendingResponseSender.nCompleted + " nFailed=" + pendingResponseSender.nFailed + " nWarning=" + pendingResponseSender.nWarning);
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Bulding final C-MOVE success response");
                  response = new CMoveResponseCommandMessage(
                     cmrq.getAffectedSOPClassUID(),
                     cmrq.getMessageID(),
                     pendingResponseSender.nFailed > 0
                     ? ResponseStatus.SubOperationsCompleteOneOrMoreFailures
                     : ResponseStatus.SubOperationsCompleteNoFailures,
                     false, // no dataset, unless there was failure, then add Failed SOP Instance UID List (0008,0058) :(
                     pendingResponseSender.nRemaining,
                     pendingResponseSender.nCompleted,
                     pendingResponseSender.nFailed,
                     pendingResponseSender.nWarning).getBytes();
                }
                else
                {
                  status = ResponseStatus.RefusedMoveDestinationUnknown;
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Unrecognized move destination " + moveDestinationAETitle + ", status = 0x" + Integer.toHexString(status));
                  response = new CMoveResponseCommandMessage(
                     cmrq.getAffectedSOPClassUID(),
                     cmrq.getMessageID(),
                     status,
                     false, // no dataset
                     null, // no OffendingElement
                     moveDestinationAETitle // ErrorComment
                     ).getBytes();
                }
              }
              setDone(true);
              setRelease(false);
            }
          }
          else if(command == MessageServiceElementCommand.C_GET_RQ && retrieveResponseGeneratorFactory != null)
          {
            RetrieveResponseGenerator retrieveResponseGenerator =
               retrieveResponseGeneratorFactory.newInstance();
            dataReceived = ByteArray.concatenate(dataReceived, pdv.getValue());	// handles null cases
            if(pdv.isLastFragment())
            {
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): last fragment of data seen");
              if(debugLevel > 0)
                System.err.println(HexDump.dump(dataReceived));
              dataList = new AttributeList();
              dataList.read(new DicomInputStream(new ByteArrayInputStream(dataReceived),
                 association.getTransferSyntaxForPresentationContextID(presentationContextIDUsed), false));
              if(debugLevel > 0)
                System.err.print(dataList);
              retrieveResponseGenerator.performRetrieve(cgrq.getAffectedSOPClassUID(), dataList, false/*relational*/);
              SetOfDicomFiles dicomFiles = retrieveResponseGenerator.getDicomFiles();
              int status = retrieveResponseGenerator.getStatus();
              retrieveResponseGenerator.close();
              if(status != ResponseStatus.Success || dicomFiles == null)
              {
                if(debugLevel > 0)
                  System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): retrieve failed or contains nothing, status = 0x" + Integer.toHexString(status));
                response = new CGetResponseCommandMessage(
                   cgrq.getAffectedSOPClassUID(),
                   cgrq.getMessageID(),
                   status,
                   false, // no dataset
                   retrieveResponseGenerator.getOffendingElement(),
                   null // no ErrorComment
                   ).getBytes();
              }
              else
              {
                CGetPendingResponseSender pendingResponseSender =
                   new CGetPendingResponseSender(association, cgrq);
                pendingResponseSender.nRemaining = dicomFiles.size();		// in case fails immediately with no status updates
                {
                  // WARNING - StorageSOPClassSCU will override the current ReceivedDataHandler set on the association
                  // do NOT send MoveOriginatorApplicationEntityTitle or MoveOriginatorMessageID - that is only for C-MOVE
                  new StorageSOPClassSCU(
                     association,
                     dicomFiles,
                     pendingResponseSender,
                     debugLevel);
                  association.setReceivedDataHandler(this);	// re-establish ourselves as the handler to send done response
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): after all stored: nRemaining=" + pendingResponseSender.nRemaining + " nCompleted=" + pendingResponseSender.nCompleted + " nFailed=" + pendingResponseSender.nFailed + " nWarning=" + pendingResponseSender.nWarning);
                  if(pendingResponseSender.nRemaining > 0)
                  {
                    pendingResponseSender.nFailed += pendingResponseSender.nRemaining;
                    pendingResponseSender.nRemaining = 0;
                  }
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): after setting remaining to zero: nRemaining=" + pendingResponseSender.nRemaining + " nCompleted=" + pendingResponseSender.nCompleted + " nFailed=" + pendingResponseSender.nFailed + " nWarning=" + pendingResponseSender.nWarning);
                  if(debugLevel > 0)
                    System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Bulding final C-GET success response");
                  response = new CGetResponseCommandMessage(
                     cgrq.getAffectedSOPClassUID(),
                     cgrq.getMessageID(),
                     pendingResponseSender.nFailed > 0
                     ? ResponseStatus.SubOperationsCompleteOneOrMoreFailures
                     : ResponseStatus.SubOperationsCompleteNoFailures,
                     false, // no dataset, unless there was failure, then add Failed SOP Instance UID List (0008,0058)
                     pendingResponseSender.nRemaining,
                     pendingResponseSender.nCompleted,
                     pendingResponseSender.nFailed,
                     pendingResponseSender.nWarning).getBytes();
                }
              }
              if(debugLevel > 0)
                System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Setting done flag for C-GET response");
              setDone(true);
              setRelease(false);
            }
          }
          else
          {
            if(debugLevel > 0)
              System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): Unexpected data fragment for command 0x" + Integer.toHexString(command) + " " + MessageServiceElementCommand.toString(command) + " - ignoring");
          }
        }
      }
      if(debugLevel > 0)
        System.err.println("StorageSOPClassSCP.CompositeCommandReceivedPDUHandler.sendPDataIndication(): finished; isDone()=" + isDone());
    }

    /***/
    public AttributeList getCommandList()
    {
      return commandList;
    }

    /***/
    public byte[] getResponse()
    {
      return response;
    }

    /***/
    public byte getPresentationContextIDUsed()
    {
      return presentationContextIDUsed;
    }

    /***/
    public File getReceivedFile()
    {
      return receivedFile;
    }

    /***/
    public String getReceivedFileName()
    {
      return receivedFile == null ? null : receivedFile.getPath();
    }
  }

  /**
   * @param	association
   * @exception	IOException
   * @exception	AReleaseException
   * @exception	DicomException
   * @exception	DicomNetworkException
   */
  private boolean receiveAndProcessOneRequestMessage(Association association)
     throws AReleaseException, DicomNetworkException, DicomException, IOException
  {
    if(debugLevel > 0)
      System.err.println("StorageSOPClassSCP.receiveAndProcessOneRequestMessage(): start");

    CompositeCommandReceivedPDUHandler receivedPDUHandler =
       new CompositeCommandReceivedPDUHandler(savedImagesFolder, queryResponseGeneratorFactory, retrieveResponseGeneratorFactory, debugLevel);
    association.setReceivedDataHandler(receivedPDUHandler);

    if(debugLevel > 0)
      System.err.println("StorageSOPClassSCP.receiveAndProcessOneRequestMessage(): waitForPDataPDUsUntilHandlerReportsDone");

    association.waitForPDataPDUsUntilHandlerReportsDone();	// throws AReleaseException if release request instead

    if(debugLevel > 0)
      System.err.println("StorageSOPClassSCP.receiveAndProcessOneRequestMessage(): back from waitForPDataPDUsUntilHandlerReportsDone");

    String receivedFileName = receivedPDUHandler.getReceivedFileName();	// null if C-ECHO
    if(receivedFileName != null)
    {
      byte pcid = receivedPDUHandler.getPresentationContextIDUsed();
      String ts = association.getTransferSyntaxForPresentationContextID(pcid);
      String callingAE = association.getCallingAETitle();
      receivedObjectHandler.sendReceivedObjectIndication(receivedFileName, ts, callingAE);

      for(AssociationListener l : arAssocListner)
        l.notifyReceived(association, receivedFileName, ts, callingAE);
    }

    if(debugLevel > 0)
      System.err.println("StorageSOPClassSCP.receiveAndProcessOneRequestMessage(): sending (final) response");

    byte[] response = receivedPDUHandler.getResponse();

    if(debugLevel > 1)
      System.err.println("StorageSOPClassSCP.receiveAndProcessOneRequestMessage(): response = " + CompositeResponseHandler.dumpAttributeListFromCommandOrData(response, TransferSyntax.Default));

    association.send(receivedPDUHandler.getPresentationContextIDUsed(), response, null);

    if(debugLevel > 0)
      System.err.println("StorageSOPClassSCP.receiveAndProcessOneRequestMessage(): end");

    boolean moreExpected;
    if(receivedPDUHandler.isToBeReleased())
    {
      if(debugLevel > 0)
        System.err.println("StorageSOPClassSCP.receiveAndProcessOneRequestMessage(): explicitly releasing association");
      association.release();
      moreExpected = false;
    }
    else
    {
      moreExpected = true;
    }
    return moreExpected;
  }
  /***/
  private Socket socket;
  /***/
  private String calledAETitle;
  /***/
  private int ourMaximumLengthReceived;
  /***/
  private int socketReceiveBufferSize;
  /***/
  private int socketSendBufferSize;
  /***/
  private File savedImagesFolder;
  /***/
  protected StoredFilePathStrategy storedFilePathStrategy;
  /***/
  private ReceivedObjectHandler receivedObjectHandler;
  /***/
  private QueryResponseGeneratorFactory queryResponseGeneratorFactory;
  /***/
  private RetrieveResponseGeneratorFactory retrieveResponseGeneratorFactory;
  /***/
  private ApplicationEntityMap applicationEntityMap;
  /***/
  private PresentationContextSelectionPolicy presentationContextSelectionPolicy;
  /***/
  private int debugLevel;

  /**
   * <p>Construct an instance of an association acceptor and storage, query, retrieve and verification SCP
   * to be passed to the constructor of a thread that will be started.</p>
   *
   * @param	socket								the socket on which a transport connection open indication has been received
   * @param	calledAETitle						our AE Title
   * @param	ourMaximumLengthReceived			the maximum PDU length that we will offer to receive
   * @param	socketReceiveBufferSize				the TCP socket receive buffer size to set (if possible), 0 means leave at the default
   * @param	socketSendBufferSize				the TCP socket send buffer size to set (if possible), 0 means leave at the default
   * @param	savedImagesFolder					the folder in which to store received data sets (may be null, to ignore received data for testing)
   * @param	storedFilePathStrategy			the strategy to use for naming received files and folders
   * @param	receivedObjectHandler				the handler to call after each data set has been received and stored
   * @param	queryResponseGeneratorFactory		a factory to make handlers to generate query responses from a supplied query message
   * @param	retrieveResponseGeneratorFactory	a factory to make handlers to generate retrieve responses from a supplied retrieve message
   * @param	applicationEntityMap				a map of application entity titles to presentation addresses
   * @param	presentationContextSelectionPolicy	which SOP Classes and Transfer Syntaxes to accept and reject
   * @param	debugLevel							zero for no debugging messages, higher values more verbose messages
   * @exception	IOException
   * @exception	DicomException
   * @exception	DicomNetworkException
   */
  public StorageSOPClassSCP(Socket socket, String calledAETitle,
     int ourMaximumLengthReceived, int socketReceiveBufferSize, int socketSendBufferSize,
     File savedImagesFolder, StoredFilePathStrategy storedFilePathStrategy, ReceivedObjectHandler receivedObjectHandler,
     QueryResponseGeneratorFactory queryResponseGeneratorFactory, RetrieveResponseGeneratorFactory retrieveResponseGeneratorFactory,
     ApplicationEntityMap applicationEntityMap,
     PresentationContextSelectionPolicy presentationContextSelectionPolicy,
     int debugLevel) throws DicomNetworkException, DicomException, IOException
  {
//System.err.println("StorageSOPClassSCP()");
    this.socket = socket;
    this.calledAETitle = calledAETitle;
    this.ourMaximumLengthReceived = ourMaximumLengthReceived;
    this.socketReceiveBufferSize = socketReceiveBufferSize;
    this.socketSendBufferSize = socketSendBufferSize;
    this.savedImagesFolder = savedImagesFolder;
    this.storedFilePathStrategy = storedFilePathStrategy;
    this.receivedObjectHandler = receivedObjectHandler;
    this.queryResponseGeneratorFactory = queryResponseGeneratorFactory;
    this.retrieveResponseGeneratorFactory = retrieveResponseGeneratorFactory;
    this.applicationEntityMap = applicationEntityMap;
    this.presentationContextSelectionPolicy = presentationContextSelectionPolicy;
    this.debugLevel = debugLevel;
    storedFilePathStrategy.setDebugLevel(debugLevel);
  }

  /**
   * <p>Waits for an association to be initiated (acts as an association acceptor), then waits for storage or
   * verification commands, storing data sets in Part 10 files in the specified folder, until the association
   * is released or the transport connection closes.</p>
   */
  public void run()
  {
    Association association = null;

    try
    {
      association = AssociationFactory.createNewAssociation(socket, calledAETitle,
         ourMaximumLengthReceived, socketReceiveBufferSize, socketSendBufferSize,
         presentationContextSelectionPolicy, arAssocListner,
         debugLevel);

      if(debugLevel > 1)
        System.err.println(association);

      try
      {
        for(AssociationListener l : arAssocListner)
          l.beginAssociation(association);

        while(receiveAndProcessOneRequestMessage(association))
        {
          for(AssociationListener l : arAssocListner)
            l.notifyOperationDone(association);
        }
      }
      catch(AReleaseException e)
      {
      }

      for(AssociationListener l : arAssocListner)
        l.endAssociation(association);
    }
    catch(Exception e)
    {
      for(AssociationListener l : arAssocListner)
        l.errorAssociation(association, e);
      
      e.printStackTrace(System.err);
    }
  }

  public void addListner(AssociationListener l)
  {
    arAssocListner.add(l);
  }

  public void removeListner(AssociationListener l)
  {
    arAssocListner.remove(l);
  }
}

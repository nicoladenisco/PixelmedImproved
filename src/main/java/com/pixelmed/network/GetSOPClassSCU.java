/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.network;
import com.pixelmed.dicom.*;
import com.pixelmed.utils.ByteArray;
import com.pixelmed.utils.CopyStream;
import com.pixelmed.utils.FileUtilities;
import com.pixelmed.utils.HexDump;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>This class implements the SCU role of C-GET SOP Classes.</p>
 *
 * <p>The class has no methods other than the constructor (and a main method for testing). The
 * constructor establishes an association, sends the C-GET request, and releases the
 * association. Any identifiers received are handled by the supplied
 * {@link com.pixelmed.network.IdentifierHandler IdentifierHandler}. Any objects received are handled by the supplied
 * {@link com.pixelmed.network.ReceivedObjectHandler ReceivedObjectHandler}.</p>
 *
 * <p>Note the need to supply a list of supported Storage SOP Classes to be negotiated during
 * association establishment - if the list is known a priori it should be supplied, otherwise
 * a list of all known SOP Classes can be supplied; however, since the latter is rather large
 * it can easily exceed the maximum number of presentation contexts allowed by the standard
 * (128), hence considerable control is provided over the number of transfer syntaxes
 * proposed, should it be necessary; when in doubt allow the SCP to choose from a list
 * of multiple transfer syntaxes for each abstract syntax which consumes only a single
 * presentation context.</p>
 *
 * <p>Debugging messages with a varying degree of verbosity can be activated.</p>
 *
 * <p>The main method is also useful in its own right as a command-line
 * utility, which will store requested files in a specified directory.</p>
 *
 * <p>For example, to have GETSCU command GETSCP to get a single image instance of unknown SOP Class to GETSCU using the transfer syntax of the SCP's choice:</p>
 * <pre>
try {
    AttributeList identifier = new AttributeList();
    { AttributeTag t = TagFromName.QueryRetrieveLevel; Attribute a = new CodeStringAttribute(t); a.addValue("IMAGE"); identifier.put(t,a); }
    { AttributeTag t = TagFromName.StudyInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue("1.3.6.1.4.1.5962.1.2.0.1064923879.2077.3232235877"); identifier.put(t,a); }
    { AttributeTag t = TagFromName.SeriesInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue("1.3.6.1.4.1.5962.1.3.0.0.1064923879.2077.3232235877"); identifier.put(t,a); }
    { AttributeTag t = TagFromName.SOPInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue("1.3.6.1.4.1.5962.1.1.0.0.0.1064923879.2077.3232235877"); identifier.put(t,a); }
    new GetSOPClassSCU("theirhost","104","GETSCP","GETSCU",SOPClass.StudyRootQueryRetrieveInformationModelGet,identifier,new IdentifierHandler(),"/tmp",new OurReceivedObjectHandler(),SOPClass.getSetOfStorageSOPClasses(),true,false,false,1);
}
catch (Exception e) {
    e.printStackTrace(System.err);
}
 * </pre>
 *
 * @see com.pixelmed.network.IdentifierHandler
 * @see com.pixelmed.network.ReceivedObjectHandler
 * @see com.pixelmed.network.PresentationContextListFactory
 *
 * @author	dclunie
 */
public class GetSOPClassSCU extends SOPClass {

	/***/
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/GetSOPClassSCU.java,v 1.2 2011-06-16 11:23:00 nicola Exp $";

	/***/
	private int debugLevel;

	/***/
	private class CGetResponseOrCStoreRequestHandler extends ReceivedDataHandler {
	
		/***/
		private int command;
		/***/
		private byte[] commandReceived;
		/***/
		private byte[] dataReceived;
		/***/
		protected boolean success;
		/***/
		private AttributeList commandList;
		/***/
		private AttributeList dataList;
		/***/
		private OutputStream out;
		/***/
		private CStoreRequestCommandMessage csrq;
		/***/
		private CGetResponseCommandMessage cgrsp;
		/***/
		private byte presentationContextIDUsed;
		/***/
		private ReceivedObjectHandler receivedObjectHandler;
		/***/
		private IdentifierHandler identifierHandler;
		/***/
		private File receivedFile;
		/***/
		private File temporaryReceivedFile;
		/***/
		private File savedImagesFolder;
		/***/
		protected StoredFilePathStrategy storedFilePathStrategy;
		
		CGetResponseOrCStoreRequestHandler(IdentifierHandler identifierHandler,File savedImagesFolder,StoredFilePathStrategy storedFilePathStrategy,ReceivedObjectHandler receivedObjectHandler,int debugLevel) {
			super(debugLevel);
			this.savedImagesFolder=savedImagesFolder;
			this.storedFilePathStrategy=storedFilePathStrategy;
			this.identifierHandler=identifierHandler;
			this.receivedObjectHandler=receivedObjectHandler;
		}

		public void sendPDataIndication(PDataPDU pdata,Association association) throws DicomNetworkException, DicomException, IOException {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): sendPDataIndication()");
if (debugLevel > 2) super.dumpPDVList(pdata.getPDVList());
if (debugLevel > 2) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): finished dumping PDV list from PDU");
			// append to command ...
			LinkedList pdvList = pdata.getPDVList();
			ListIterator i = pdvList.listIterator();
			while (i.hasNext()) {
				PresentationDataValue pdv = (PresentationDataValue)i.next();
				presentationContextIDUsed = pdv.getPresentationContextID();
				if (pdv.isCommand()) {
					receivedFile=null;
					commandReceived=ByteArray.concatenate(commandReceived,pdv.getValue());	// handles null cases
					if (pdv.isLastFragment()) {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): last fragment of data seen");
if (debugLevel > 0) System.err.println(HexDump.dump(commandReceived));
						commandList = new AttributeList();
						commandList.read(new DicomInputStream(new ByteArrayInputStream(commandReceived),TransferSyntax.Default,false));
						commandReceived=null;
if (debugLevel > 0) System.err.print(commandList);
						command = Attribute.getSingleIntegerValueOrDefault(commandList,TagFromName.CommandField,0xffff);
						if (command == MessageServiceElementCommand.C_STORE_RQ) {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): C-STORE-RQ");
							csrq = new CStoreRequestCommandMessage(commandList);
						}
						else if (command == MessageServiceElementCommand.C_GET_RSP) {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): C-STORE-RQ");
							cgrsp = new CGetResponseCommandMessage(commandList);
							evaluateStatusAndSetSuccess(commandList);
						}
						else {
							throw new DicomNetworkException("Unexpected command 0x"+Integer.toHexString(command)+" "+MessageServiceElementCommand.toString(command));
						}
						// 2004/06/08 DAC removed break that was here to resolve [bugs.mrmf] (000113) StorageSCP failing when data followed command in same PDU
if (debugLevel > 0 && i.hasNext()) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication: Data after command in same PDU");
					}
				}
				else {
					 if (command == MessageServiceElementCommand.C_STORE_RQ) {
						// From StorageSOPClassSCP ...
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): Storing data fragment");
						if (out == null && savedImagesFolder != null) {		// lazy opening
							FileMetaInformation fmi = new FileMetaInformation(
								csrq.getAffectedSOPClassUID(),
								csrq.getAffectedSOPInstanceUID(),
								association.getTransferSyntaxForPresentationContextID(presentationContextIDUsed),
								association.getCallingAETitle());
							receivedFile=storedFilePathStrategy.makeReliableStoredFilePathWithFoldersCreated(
                               association, savedImagesFolder,csrq.getAffectedSOPInstanceUID());
							//temporaryReceivedFile=File.createTempFile("PMP",null);
							temporaryReceivedFile=new File(savedImagesFolder,FileUtilities.makeTemporaryFileName());
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): Receiving and storing "+receivedFile);
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): Receiving and storing into temporary "+temporaryReceivedFile);
							out = new BufferedOutputStream(new FileOutputStream(temporaryReceivedFile));
							DicomOutputStream dout = new DicomOutputStream(out,TransferSyntax.ExplicitVRLittleEndian,null);
							fmi.getAttributeList().write(dout);
							dout.flush();
						}
						if (out != null) {
							out.write(pdv.getValue());
						}
						if (pdv.isLastFragment()) {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): Finished storing data in C_STORE_RQ");
							if (out != null) {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): Closing out put stream to temporary file");
								out.close();
								if (receivedFile.exists()) {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): Deleting pre-existing file for same SOPInstanceUID");
									receivedFile.delete();		// prior to rename of temporary file, in case might cause renameTo() fail
								}
								if (!temporaryReceivedFile.renameTo(receivedFile)) {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): Could not move temporary file into place ... copying instead");
									CopyStream.copy(temporaryReceivedFile,receivedFile);
									temporaryReceivedFile.delete();
								}
								out=null;
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): temporaryReceivedFile exists (should be false) = "+temporaryReceivedFile.exists());
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): receivedFile exists (should be true) = "+receivedFile.exists());
								if (!receivedFile.exists()) {
									throw new DicomException("Failed to move or copy received file into place");
								}
							}
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): Send successful C-STORE-RSP");
							{
								CStoreResponseCommandMessage csrsp = new CStoreResponseCommandMessage(
									csrq.getAffectedSOPClassUID(),
									csrq.getAffectedSOPInstanceUID(),
									csrq.getMessageID(),
									0x0000				// success status
									);
								byte presentationContextIDForResponse = association.getSuitablePresentationContextID(csrsp.getAffectedSOPClassUID());
								association.send(presentationContextIDForResponse,csrsp.getBytes(),null);
							}
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): Notify receivedObjectHandler");
							if (receivedFile != null && receivedFile.exists() && receivedObjectHandler != null) {
								String receivedFileName=receivedFile.getPath();
								// Modified from StorageSOPClassSCP.receiveAndProcessOneRequestMessage() ...
								if (receivedFileName != null) {
									String ts = association.getTransferSyntaxForPresentationContextID(presentationContextIDUsed);
									String calledAE = association.getCalledAETitle();	// not calling, since roles reversed
									receivedObjectHandler.sendReceivedObjectIndication(receivedFileName,ts,calledAE);
								}
							}
						}
					}
					else if (command == MessageServiceElementCommand.C_GET_RSP) {
						// From CompositeResponseHandler ...
						// data fragment is always allowed, so not allowData flag
						dataReceived=ByteArray.concatenate(dataReceived,pdv.getValue());	// handles null cases
						if (pdv.isLastFragment()) {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.sendPDataIndication(): last fragment of data in C_GET_RSP seen");
							AttributeList list = CompositeResponseHandler.getAttributeListFromCommandOrData(dataReceived,
								association.getTransferSyntaxForPresentationContextID(pdv.getPresentationContextID()),debugLevel);
							makeUseOfDataSet(list);
							dataReceived=null;
						}
					}
					else {
						// shouldn't happen ... exception should have been thrown whilst handling command, not data
						throw new DicomNetworkException("Unexpected command 0x"+Integer.toHexString(command)+" "+MessageServiceElementCommand.toString(command));
					}
				}
			}
		}
		
		/**
		 * @param	list
		 */
		protected void evaluateStatusAndSetSuccess(AttributeList list) {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseHandler.evaluateStatusAndSetSuccess:");
if (debugLevel > 0) System.err.println(list);
			// could check all sorts of things, like:
			// - AffectedSOPClassUID is what we sent
			// - CommandField is 0x8020 C-Find-RSP
			// - MessageIDBeingRespondedTo is what we sent
			// - DataSetType is 0101 for success (no data set) or other for pending
			// - Status is success and consider associated elements
			//
			// for now just treat success or warning as success (and absence as failure)
			int status = Attribute.getSingleIntegerValueOrDefault(list,TagFromName.Status,0xffff);
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseHandler.evaluateStatusAndSetSuccess: status = 0x"+Integer.toHexString(status));
			// possible statuses at this point are:
			// A700 Refused - Out of Resources	
			// A900 Failed - Identifier does not match SOP Class	
			// Cxxx Failed - Unable to process	
			// FE00 Cancel - Matching terminated due to Cancel request	
			// 0000 Success - Matching is complete - No final Identifier is supplied.	
			// FF00 Pending - Matches are continuing - Current Match is supplied and any Optional Keys were supported in the same manner as Required Keys.
			// FF01 Pending - Matches are continuing - Warning that one or more Optional Keys were not supported for existence and/or matching for this Identifier.

			success = status == 0x0000;	// success
			
			if (status != 0xFF00 && status != 0xFF01) {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseHandler.evaluateStatusAndSetSuccess: status no longer pending, so stop");
				setDone(true);
			}
		}

		/**
		 * @param	list
		 */
		protected void makeUseOfDataSet(AttributeList list) {
if (debugLevel > 0) System.err.println("GetSOPClassSCU.CGetResponseOrCStoreRequestHandler.makeUseOfDataSet:");
if (debugLevel > 0) System.err.print(list);
			try {
				if (identifierHandler != null) {
					identifierHandler.doSomethingWithIdentifier(list);
				}
			}
			catch (DicomException e) {
				// do not stop ... other identifiers may be OK
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * @param	hostname		their hostname or IP address
	 * @param	port			their port
	 * @param	calledAETitle		their AE Title
	 * @param	callingAETitle		our AE Title
	 * @param	affectedSOPClass	the SOP Class defining which query model, e.g. {@link com.pixelmed.dicom.SOPClass#StudyRootQueryRetrieveInformationModelGet SOPClass.StudyRootQueryRetrieveInformationModelGet}
	 * @param	identifier		the list of unique keys and move level
	 * @param	identifierHandler	the handler to use for each returned identifier
	 * @param	savedImagesFolder	the folder in which to store received data sets (may be null, to ignore received data for testing)
	 * @param	storedFilePathStrategy	the strategy to use for naming received files and folders
	 * @param	receivedObjectHandler	the handler to call after each data set has been received and stored
	 * @param	setOfStorageSOPClasses	the <code>Set</code> of Storage SOP Class UIDs (as <code>String</code>) that are acceptable
	 * @param	theirChoice		propose a single presentation context with all transfer syntaxes to allow them to choose
	 * @param	ourChoice		propose separate presentation contexts for each transfer syntax to allow us to choose
	 * @param	asEncoded		propose a separate presentation context for the specified transfer syntax in which the data set is known to be encoded
	 * @param	debugLevel		zero for no debugging messages, higher values more verbose messages
	 * @exception	IOException
	 * @exception	DicomException
	 * @exception	DicomNetworkException
	 */
	public GetSOPClassSCU(String hostname,int port,String calledAETitle,String callingAETitle,
			String affectedSOPClass,AttributeList identifier,IdentifierHandler identifierHandler,
			File savedImagesFolder,StoredFilePathStrategy storedFilePathStrategy,ReceivedObjectHandler receivedObjectHandler,
			Set setOfStorageSOPClasses,boolean theirChoice,boolean ourChoice,boolean asEncoded,
			int debugLevel) throws DicomNetworkException, DicomException, IOException {
		this.debugLevel=debugLevel;
		
if (debugLevel > 0) System.err.println("GetSOPClassSCU(): request identifier");
if (debugLevel > 0) System.err.print(identifier);

		Set setOfSOPClassUIDs = new HashSet();
		setOfSOPClassUIDs.add(affectedSOPClass);
		setOfSOPClassUIDs.addAll(setOfStorageSOPClasses);
		LinkedList presentationContexts = PresentationContextListFactory.createNewPresentationContextList(setOfSOPClassUIDs,0/*compressionLevel*/,theirChoice,ourChoice,asEncoded);
		LinkedList scuSCPRoleSelections = new LinkedList();
		{
			Iterator i = setOfStorageSOPClasses.iterator();
			while (i.hasNext()) {
				String abstractSyntaxUID = (String)i.next();
				scuSCPRoleSelections.add(new SCUSCPRoleSelection(abstractSyntaxUID,false/*SCU role supported*/,true/*SCP role supported*/));
			}
		}

		Association association = AssociationFactory.createNewAssociation(hostname,port,calledAETitle,callingAETitle,presentationContexts,scuSCPRoleSelections,false,debugLevel);
if (debugLevel > 0) System.err.println(association);
		// Decide which presentation context we are going to use ...
		byte usePresentationContextID = association.getSuitablePresentationContextID(affectedSOPClass);
if (debugLevel > 0) System.err.println("Using context ID "+usePresentationContextID);
		byte cGetRequestCommandMessage[] = new CGetRequestCommandMessage(affectedSOPClass).getBytes();
		byte cGetIdentifier[] = new IdentifierMessage(identifier,association.getTransferSyntaxForPresentationContextID(usePresentationContextID)).getBytes();
		association.setReceivedDataHandler(new CGetResponseOrCStoreRequestHandler(identifierHandler,savedImagesFolder,storedFilePathStrategy,receivedObjectHandler,debugLevel));
		// for some reason association.send(usePresentationContextID,cGetRequestCommandMessage,cGetIdentifier) fails with Oldenburg imagectn
		// so send the command and the identifier separately ...
		// (was probably because wasn't setting the last fragment flag on the command in Association.send() DAC. 2004/06/10)
		// (see [bugs.mrmf] (000114) Failing to set last fragment on command when sending command and data in same PDU)
		association.send(usePresentationContextID,cGetRequestCommandMessage,null);
		association.send(usePresentationContextID,null,cGetIdentifier);
if (debugLevel > 0) System.err.println("GetSOPClassSCU: waiting for PDUs");
		try {
			association.waitForPDataPDUsUntilHandlerReportsDone();
if (debugLevel > 0) System.err.println("GetSOPClassSCU: got PDU, now releasing association");
			// State 6
			association.release();
		}
		catch (AReleaseException e) {
			// State 1
			// the other end released and didn't wait for us to do it
		}
	}

	/***/
	private class OurReceivedObjectHandler extends ReceivedObjectHandler {
		/**
		 * @param	fileName
		 * @param	transferSyntax		the transfer syntax in which the data set was received and is stored
		 * @param	callingAETitle		the AE title of the caller who sent the data set
		 * @exception	IOException
		 * @exception	DicomException
		 * @exception	DicomNetworkException
		 */
		public void sendReceivedObjectIndication(String fileName,String transferSyntax,String callingAETitle)
				throws DicomNetworkException, DicomException, IOException {
System.err.println("GetSOPClassSCU.OurReceivedObjectHandler.sendReceivedObjectIndication() fileName: "+fileName+" from "+callingAETitle+" in "+transferSyntax);
		}
	}
	
	private GetSOPClassSCU() {
	}

	private final void doGet(String arg[]) {
		try {
			final String savedImagesFolderName = arg[4];
			File savedImagesFolder = null;
			if (savedImagesFolderName != null && savedImagesFolderName.length() > 0) {
				savedImagesFolder = new File(savedImagesFolderName);
			}
			final AttributeList identifier = new AttributeList();
			{ AttributeTag t = TagFromName.QueryRetrieveLevel; Attribute a = new CodeStringAttribute(t); a.addValue(arg[5]); identifier.put(t,a); }
			{ AttributeTag t = TagFromName.StudyInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(arg[6]); identifier.put(t,a); }
			if (arg.length > 7) { AttributeTag t = TagFromName.SeriesInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(arg[7]); identifier.put(t,a); }
			if (arg.length > 8) { AttributeTag t = TagFromName.SOPInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); a.addValue(arg[8]); identifier.put(t,a); }
			new GetSOPClassSCU(arg[0],Integer.parseInt(arg[1]),arg[2],arg[3],SOPClass.StudyRootQueryRetrieveInformationModelGet,
				identifier,new IdentifierHandler(),
				savedImagesFolder,StoredFilePathStrategy.getDefaultStrategy(),new OurReceivedObjectHandler(),
				SOPClass.getSetOfStorageSOPClasses(),true,false,false,
				0/*debugLevel*/);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
	}
	
	/**
	 * <p>For testing, establish an association to the specified AE and perform a retrieve using a C-GET request.</p>
	 *
	 * @param	arg	array of seven, eight or nine strings - their hostname, their port, their AE Title, our AE Title, the directory in which to store the incoming files,
	 *			the retrieve level (STUDY, SERIES or IMAGE), then the Study Instance UID,
	 *			optionally the Series Instance UID (if SERIES) and optionally the SOP Instance UID (if IMAGE)
	 */
	public static void main(String arg[]) {
		new GetSOPClassSCU().doGet(arg);	// need to invoke non-static in order to new OurReceivedObjectHandler()
	}
}





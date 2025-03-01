package com.pixelmed.network;

import com.pixelmed.utils.*;
import com.pixelmed.dicom.*;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Set;
import java.io.*;

/**
 * <p>This class implements the SCU role of SOP Classes of the Delete Service Class.</p>
 *
 *
 * @author Nicola De Nisco
 */
public class DeleteSOPClassSCU extends SOPClass
{
  /***/
  private static final String identString =
     "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/DeleteSOPClassSCU.java,v 1.2 2011-06-13 12:34:14 nicola Exp $";
  /***/
  protected int debugLevel;
  /***/
  protected boolean trappedExceptions;

  /**
   * @return	true if in multiple instance constructors exceptions were trapped, e.g., connection or association failure before transfers attempyed
   */
  public boolean encounteredTrappedExceptions()
  {
    return trappedExceptions;
  }

  /***/
  protected class NDeleteResponseHandler extends CompositeResponseHandler
  {
    /**
     * @param	debugLevel
     */
    NDeleteResponseHandler(int debugLevel)
    {
      super(debugLevel);
    }

    /**
     * @param	list
     */
    protected void evaluateStatusAndSetSuccess(AttributeList list)
    {
      // could check all sorts of things, like:
      // - AffectedSOPClassUID is what we sent
      // - CommandField is 0x8001 C-STORE-RSP
      // - MessageIDBeingRespondedTo is what we sent
      // - DataSetType is 0101 (no data set)
      // - Status is success and consider associated elements
      // - AffectedSOPInstanceUID is what we sent
      //
      // for now just treat success or warning as success (and absence as failure)
      int status = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.Status, 0xffff);
      success = status == 0x0000 // success
         || status == 0xB000 // coercion of data element
         || status == 0xB007 // data set does not match SOP Class
         || status == 0xB006;	// element discarded
    }
  }

  /**
   * @param	association
   * @param	affectedSOPClass
   * @param	affectedSOPInstance
   * @param	inputTransferSyntaxUID
   * @param	din
   * @param	presentationContextID
   * @param	outputTransferSyntaxUID
   * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
   * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
   * @exception	IOException
   * @exception	DicomException
   * @exception	DicomNetworkException
   * @exception	AReleaseException
   */
  protected boolean deleteOneSOPInstance(Association association,
     String affectedSOPClass, String affectedSOPInstance,
     byte presentationContextID)
     throws AReleaseException, DicomNetworkException, DicomException, IOException
  {
    byte nDeleteRequestCommandMessage[] =
       new NDeleteRequestCommandMessage(affectedSOPClass, affectedSOPInstance).getBytes();

    NDeleteResponseHandler receivedDataHandler = new NDeleteResponseHandler(debugLevel);
    association.setReceivedDataHandler(receivedDataHandler);
    association.send(presentationContextID, nDeleteRequestCommandMessage, null);

    if(debugLevel > 0)
      System.err.println("DeleteSOPClassSCU.sendOneSOPInstance(): about to wait for PDUs");

    association.waitForCommandPDataPDUs();
    return receivedDataHandler.wasSuccessful();
  }

  /**
   * <p>Dummy constructor allows testing subclasses to use different constructor.</p>
   *
   */
  public DeleteSOPClassSCU()
     throws DicomNetworkException, DicomException, IOException
  {
  }

  /**
   * <p>Establish an association to the specified AE, delete the instances, and release the association.</p>
   *
   * @param	hostname								their hostname or IP address
   * @param	port									their port
   * @param	calledAETitle							their AE Title
   * @param	callingAETitle							our AE Title
   * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
   * @param	debugLevel								zero for no debugging messages, higher values more verbose messages
   */
  public void delete(String hostname, int port, String calledAETitle, String callingAETitle,
     SetOfDicomFiles dicomFiles, int debugLevel)
     throws DicomNetworkException, IOException
  {
    this.debugLevel = debugLevel;
    try
    {
      LinkedList presentationContexts =
         PresentationContextListFactory.createNewPresentationContextList(dicomFiles, 0);

      Association association = AssociationFactory.createNewAssociation(hostname, port,
         calledAETitle, callingAETitle, presentationContexts, null, false, debugLevel);

      deleteMultipleSOPInstances(association, dicomFiles);

      association.release();
    }
    catch(AReleaseException e)
    {
      // State 1
      // the other end released and didn't wait for us to do it
    }
  }

  /**
   * <p>Delete the specified instances over an existing association.</p>
   *
   * @param	association								already existing association to SCP
   * @param	dicomFiles								the set of DICOM files containing names, SOP Class UIDs, SOP Instance UIDs and optionally Transfer Syntaxes
   * @exception	AReleaseException
   * @exception	DicomNetworkException
   * @exception	IOException
   */
  protected void deleteMultipleSOPInstances(Association association, SetOfDicomFiles dicomFiles)
     throws AReleaseException, DicomNetworkException, IOException
  {
    int nRemaining = dicomFiles.size();
    int nCompleted = 0;
    int nFailed = 0;
    int nWarning = 0;
    {
      if(debugLevel > 0)
        System.err.println(association);

      Iterator fi = dicomFiles.iterator();
      while(fi.hasNext())
      {
        --nRemaining;
        ++nCompleted;
        SetOfDicomFiles.DicomFile dicomFile = (SetOfDicomFiles.DicomFile) (fi.next());
        String fileName = dicomFile.getFileName();
        if(debugLevel > 0)
          System.err.println("Deleting " + fileName);
        boolean success = false;
        String affectedSOPClass = dicomFile.getSOPClassUID();
        String affectedSOPInstance = dicomFile.getSOPInstanceUID();

        try
        {
          // Decide which presentation context we are going to use ...
          byte usePresentationContextID = association.getSuitablePresentationContextID(affectedSOPClass);
          //int usePresentationContextID = association.getSuitablePresentationContextID(SOPClass.Verification,TransferSyntax.Default);

          if(debugLevel > 0)
            System.err.println("Using context ID " + usePresentationContextID);

          success = deleteOneSOPInstance(association, affectedSOPClass, affectedSOPInstance, usePresentationContextID);
          // State 6
        }
        catch(DicomNetworkException e)
        {
          e.printStackTrace(System.err);
          success = false;
        }
        catch(DicomException e)
        {
          e.printStackTrace(System.err);
          success = false;
        }
        catch(IOException e)
        {
          e.printStackTrace(System.err);
          success = false;
        }
        if(!success)
        {
          ++nFailed;
        }

        if(debugLevel > 0)
          System.err.println("Delete " + fileName + " " + (success ? "succeeded" : "failed"));
      }

      if(debugLevel > 0)
        System.err.println("DeleteSOPClassSCU(): Finished delete all files nRemaining=" + nRemaining + " nCompleted=" + nCompleted + " nFailed=" + nFailed + " nWarning=" + nWarning);
    }
  }
}

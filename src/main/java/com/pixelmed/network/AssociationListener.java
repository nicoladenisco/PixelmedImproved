package com.pixelmed.network;

import java.util.LinkedList;

/**
 * Notify to user code the incoming association and operation.
 *
 * @author Nicola De Nisco
 */
public interface AssociationListener
{
  /**
   * Notify association negotiation from client and server.
   * Other tests are executed before and afther this call.
   * The listner can only reject an accepted association.
   * @param calledAETitle the client (calling) AETitle
   * @param remoteAETitle the server (called) AETitle
   * @param presentationContexts the presentation context
   * @return true to accept association
   */
  public boolean acceptAssociation(String calledAETitle, String remoteAETitle, LinkedList presentationContexts);

  /**
   * Notify begin association.
   * At this time the Association is already accepted from the server.
   * Throw exception quit the association, but is not correct; perform
   * better controls in acceptAssociation().
   * @param as the association descriptor
   * @throws com.pixelmed.network.AssociationListener.AbortException to abort the incoming association
   */
  public void beginAssociation(Association as) throws AReleaseException;

  /**
   * Notify end of association.
   * @param as the association descriptor
   */
  public void endAssociation(Association as);

  /**
   * Notify saved file received (StroreSCU).
   * @param as the association descriptor
   * @param fileName
   * @param transferSyntax
   * @param callingAETitle
   * @throws com.pixelmed.network.AssociationListener.AbortException
   */
  public void notifyReceived(Association as,
     String fileName, String transferSyntax, String callingAETitle) throws AReleaseException;

  /**
   * Notify an incoming request with the specified command.
   * @param as the association descriptor
   * @param command the command request (ECHOSCU, STORESCU, FINDSCU, MOVESCU, GETSCU)
   * @throws AReleaseException
   */
  public void notifyRequestCommand(Association as, int command)  throws AReleaseException;

  /**
   * Notify a completed request.
   * @param as the association descriptor
   * @throws AReleaseException
   */
  public void notifyOperationDone(Association as) throws AReleaseException;

  /**
   * Notifica associazione chiusa con errore.
   * Se l'errore si verifica prima della creazione dell'associazione
   * allora as sarà null.
   * @param as the association descriptor
   * @param error
   */
  public void errorAssociation(Association as, Throwable error);
}

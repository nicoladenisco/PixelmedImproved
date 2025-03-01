/* Copyright (c) 2001-2009, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */
package com.pixelmed.network;

import com.pixelmed.database.DatabaseApplicationProperties;
import com.pixelmed.dicom.*;
import com.pixelmed.utils.FileUtilities;
import com.pixelmed.query.QueryResponseGeneratorFactory;
import com.pixelmed.query.RetrieveResponseGeneratorFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import java.util.ArrayList;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.util.Properties;

/**
 * <p>This class waits for incoming connections and association requests for
 * the SCP role of SOP Classes of the Storage Service Class,
 * the Study Root Query Retrieve Information Model Find, Get and Move SOP Classes,
 * and the Verification SOP Class.</p>
 *
 * <p>The class has a constructor and a <code>run()</code> method. The
 * constructor is passed a socket on which to listen for transport
 * connection open indications. The <code>run()</code> method waits
 * for transport connection open indications, then instantiates
 * {@link com.pixelmed.network.StorageSOPClassSCP StorageSOPClassSCP}
 * to accept an association and wait for storage or verification commands, storing
 * data sets in Part 10 files in the specified folder.</p>
 *
 * <p>An instance of {@link com.pixelmed.network.ReceivedObjectHandler ReceivedObjectHandler}
 * can be supplied in the constructor to process the received data set stored in the file
 * when it has been completely received.</p>
 *
 * <p>For example:</p>
 * <pre>
try {
new Thread(new StorageSOPClassSCPDispatcher("104","STORESCP","/tmp",new OurReceivedObjectHandler(),0)).start();
}
catch (IOException e) {
e.printStackTrace(System.err);
}
 * </pre>
 *
 * <p>If it is necessary to shutdown the StorageSOPClassSCPDispatcher, for example after changing the
 * properties that define the listening port or AE Title, the
 * {@link com.pixelmed.network.StorageSOPClassSCPDispatcher#shutdown() shutdown()}
 * method can be called.</p>
 *
 * <p>Debugging messages with a varying degree of verbosity can be activated.</p>
 *
 * <p>The main method is also useful in its own right as a command-line Storage
 * SCP utility, which will store incoming files in a specified directory.</p>
 *
 * <p>For example, on Unix:</p>
 * <pre>
% java -cp ./pixelmed.jar com.pixelmed.network.StorageSOPClassSCPDispatcher "104" "STORESCP" "/tmp" 0
 * </pre>
 *
 * <p>On Windows, the classpath syntax would use a different separator, e.g. <code>.\pixelmed.jar</code></p>
 *
 * <p>Note that the main method can also be used without command line arguments, in which case it looks
 * for a properties file or uses defaults (refer to the main() method documentation for details).</p>
 *
 * @see com.pixelmed.network.StorageSOPClassSCP
 * @see com.pixelmed.network.ReceivedObjectHandler
 *
 * @author	dclunie
 */
public class StorageSOPClassSCPDispatcher implements Runnable
{
  /***/
  private static final String identString =
     "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/StorageSOPClassSCPDispatcher.java,v 1.3 2011-07-05 14:16:49 nicola Exp $";
  /***/
  private int timeoutBeforeCheckingForInterrupted = 1000;	// in mS ... should be a property :(
  /***/
  protected ArrayList<AssociationListener> arAssocListner = new ArrayList<AssociationListener>();

  /***/
  private class DefaultReceivedObjectHandler extends ReceivedObjectHandler
  {
    /**
     * @param	fileName
     * @param	transferSyntax		the transfer syntax in which the data set was received and is stored
     * @param	callingAETitle		the AE title of the caller who sent the data set
     * @exception	IOException
     * @exception	DicomException
     * @exception	DicomNetworkException
     */
    public void sendReceivedObjectIndication(String fileName, String transferSyntax, String callingAETitle)
       throws DicomNetworkException, DicomException, IOException
    {
      System.err.println("StorageSOPClassSCPDispatcher.DefaultReceivedObjectHandler.sendReceivedObjectIndication() fileName: " + fileName + " from " + callingAETitle + " in " + transferSyntax);
    }
  }
  /***/
  private int port;
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
  private NetworkApplicationInformation networkApplicationInformation;
  /***/
  private boolean secureTransport;
  /***/
  private PresentationContextSelectionPolicy presentationContextSelectionPolicy;
  /***/
  private int debugLevel;
  /***/
  private boolean wantToShutdown;

  /**
   * <p>Construct an instance of dispatcher that will wait for transport
   * connection open indications, and handle associations and commands.</p>
   *
   * @param	port			the port on which to listen for connections
   * @param	calledAETitle		our AE Title
   * @param	savedImagesFolder	the folder in which to store received data sets (may be null, to ignore received data for testing)
   * @param	receivedObjectHandler	the handler to call after each data set has been received and stored
   * @param	debugLevel		zero for no debugging messages, higher values more verbose messages
   * @exception	IOException
   */
  public StorageSOPClassSCPDispatcher(int port, String calledAETitle, File savedImagesFolder, ReceivedObjectHandler receivedObjectHandler, int debugLevel) throws IOException
  {
    this.port = port;
    this.calledAETitle = calledAETitle;
    this.ourMaximumLengthReceived = AssociationFactory.getDefaultMaximumLengthReceived();
    this.socketReceiveBufferSize = AssociationFactory.getDefaultReceiveBufferSize();
    this.socketSendBufferSize = AssociationFactory.getDefaultSendBufferSize();
    this.savedImagesFolder = savedImagesFolder;
    this.storedFilePathStrategy = StoredFilePathStrategy.getDefaultStrategy();
    this.receivedObjectHandler = receivedObjectHandler;
    this.queryResponseGeneratorFactory = null;
    this.retrieveResponseGeneratorFactory = null;
    this.networkApplicationInformation = null;
    this.debugLevel = debugLevel;
    this.secureTransport = false;
    this.presentationContextSelectionPolicy =
       new UnencapsulatedExplicitStorePresentationContextSelectionPolicy();
  }

  /**
   * <p>Construct an instance of dispatcher that will wait for transport
   * connection open indications, and handle associations and commands.</p>
   *
   * @param	port							the port on which to listen for connections
   * @param	calledAETitle					our AE Title
   * @param	savedImagesFolder				the folder in which to store received data sets (may be null, to ignore received data for testing)
   * @param	storedFilePathStrategy			the strategy to use for naming received files and folders
   * @param	receivedObjectHandler			the handler to call after each data set has been received and stored
   * @param	debugLevel						zero for no debugging messages, higher values more verbose messages
   * @exception	IOException
   */
  public StorageSOPClassSCPDispatcher(int port, String calledAETitle, File savedImagesFolder, StoredFilePathStrategy storedFilePathStrategy, ReceivedObjectHandler receivedObjectHandler, int debugLevel) throws IOException
  {
    this.port = port;
    this.calledAETitle = calledAETitle;
    this.ourMaximumLengthReceived = AssociationFactory.getDefaultMaximumLengthReceived();
    this.socketReceiveBufferSize = AssociationFactory.getDefaultReceiveBufferSize();
    this.socketSendBufferSize = AssociationFactory.getDefaultSendBufferSize();
    this.savedImagesFolder = savedImagesFolder;
    this.storedFilePathStrategy = storedFilePathStrategy;
    this.receivedObjectHandler = receivedObjectHandler;
    this.queryResponseGeneratorFactory = null;
    this.retrieveResponseGeneratorFactory = null;
    this.networkApplicationInformation = null;
    this.debugLevel = debugLevel;
    this.secureTransport = false;
    this.presentationContextSelectionPolicy =
       new UnencapsulatedExplicitStorePresentationContextSelectionPolicy();
  }

  /**
   * <p>Construct an instance of dispatcher that will wait for transport
   * connection open indications, and handle associations and commands.</p>
   *
   * @param	port				the port on which to listen for connections
   * @param	calledAETitle			our AE Title
   * @param	ourMaximumLengthReceived	the maximum PDU length that we will offer to receive
   * @param	socketReceiveBufferSize		the TCP socket receive buffer size to set (if possible), 0 means leave at the default
   * @param	socketSendBufferSize		the TCP socket send buffer size to set (if possible), 0 means leave at the default
   * @param	savedImagesFolder		the folder in which to store received data sets (may be null, to ignore received data for testing)
   * @param	receivedObjectHandler		the handler to call after each data set has been received and stored, or null for the default that prints the file name
   * @param	queryResponseGeneratorFactory		the factory to make handlers to generate query responses from a supplied query message
   * @param	retrieveResponseGeneratorFactory	the factory to make handlers to generate retrieve responses from a supplied retrieve message
   * @param	networkApplicationInformation	from which to obtain a map of application entity titles to presentation addresses
   * @param	secureTransport		true if to use secure transport protocol
   * @param	debugLevel			zero for no debugging messages, higher values more verbose messages
   * @exception	IOException
   */
  public StorageSOPClassSCPDispatcher(int port, String calledAETitle,
     int ourMaximumLengthReceived, int socketReceiveBufferSize, int socketSendBufferSize,
     File savedImagesFolder, ReceivedObjectHandler receivedObjectHandler,
     QueryResponseGeneratorFactory queryResponseGeneratorFactory, RetrieveResponseGeneratorFactory retrieveResponseGeneratorFactory,
     NetworkApplicationInformation networkApplicationInformation, boolean secureTransport, int debugLevel) throws IOException
  {
    this.port = port;
    this.calledAETitle = calledAETitle;
    this.ourMaximumLengthReceived = ourMaximumLengthReceived;
    this.socketReceiveBufferSize = socketReceiveBufferSize;
    this.socketSendBufferSize = socketSendBufferSize;
    this.savedImagesFolder = savedImagesFolder;
    this.storedFilePathStrategy = StoredFilePathStrategy.getDefaultStrategy();
    this.receivedObjectHandler = receivedObjectHandler == null ? new DefaultReceivedObjectHandler()
                                 : receivedObjectHandler;
    this.queryResponseGeneratorFactory = queryResponseGeneratorFactory;
    this.retrieveResponseGeneratorFactory = retrieveResponseGeneratorFactory;
    this.networkApplicationInformation = networkApplicationInformation;
    this.debugLevel = debugLevel;
    this.secureTransport = secureTransport;
    this.presentationContextSelectionPolicy =
       new UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy();
  }

  /**
   * <p>Construct an instance of dispatcher that will wait for transport
   * connection open indications, and handle associations and commands.</p>
   *
   * @param	port								the port on which to listen for connections
   * @param	calledAETitle						our AE Title
   * @param	ourMaximumLengthReceived			the maximum PDU length that we will offer to receive
   * @param	socketReceiveBufferSize				the TCP socket receive buffer size to set (if possible), 0 means leave at the default
   * @param	socketSendBufferSize				the TCP socket send buffer size to set (if possible), 0 means leave at the default
   * @param	savedImagesFolder					the folder in which to store received data sets (may be null, to ignore received data for testing)
   * @param	storedFilePathStrategy				the strategy to use for naming received files and folders
   * @param	receivedObjectHandler				the handler to call after each data set has been received and stored, or null for the default that prints the file name
   * @param	queryResponseGeneratorFactory		the factory to make handlers to generate query responses from a supplied query message
   * @param	retrieveResponseGeneratorFactory	the factory to make handlers to generate retrieve responses from a supplied retrieve message
   * @param	networkApplicationInformation		from which to obtain a map of application entity titles to presentation addresses
   * @param	secureTransport						true if to use secure transport protocol
   * @param	debugLevel							zero for no debugging messages, higher values more verbose messages
   * @exception	IOException
   */
  public StorageSOPClassSCPDispatcher(int port, String calledAETitle,
     int ourMaximumLengthReceived, int socketReceiveBufferSize, int socketSendBufferSize,
     File savedImagesFolder, StoredFilePathStrategy storedFilePathStrategy, ReceivedObjectHandler receivedObjectHandler,
     QueryResponseGeneratorFactory queryResponseGeneratorFactory, RetrieveResponseGeneratorFactory retrieveResponseGeneratorFactory,
     NetworkApplicationInformation networkApplicationInformation, boolean secureTransport, int debugLevel) throws IOException
  {
    this.port = port;
    this.calledAETitle = calledAETitle;
    this.ourMaximumLengthReceived = ourMaximumLengthReceived;
    this.socketReceiveBufferSize = socketReceiveBufferSize;
    this.socketSendBufferSize = socketSendBufferSize;
    this.savedImagesFolder = savedImagesFolder;
    this.storedFilePathStrategy = storedFilePathStrategy;
    this.receivedObjectHandler = receivedObjectHandler == null ? new DefaultReceivedObjectHandler()
                                 : receivedObjectHandler;
    this.queryResponseGeneratorFactory = queryResponseGeneratorFactory;
    this.retrieveResponseGeneratorFactory = retrieveResponseGeneratorFactory;
    this.networkApplicationInformation = networkApplicationInformation;
    this.debugLevel = debugLevel;
    this.secureTransport = secureTransport;
    this.presentationContextSelectionPolicy =
       new UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy();
  }

  /**
   * <p>Construct an instance of dispatcher that will wait for transport
   * connection open indications, and handle associations and commands.</p>
   *
   * @param	port				the port on which to listen for connections
   * @param	calledAETitle			our AE Title
   * @param	savedImagesFolder		the folder in which to store received data sets (may be null, to ignore received data for testing)
   * @param	receivedObjectHandler		the handler to call after each data set has been received and stored, or null for the default that prints the file name
   * @param	queryResponseGeneratorFactory		the factory to make handlers to generate query responses from a supplied query message
   * @param	retrieveResponseGeneratorFactory	the factory to make handlers to generate retrieve responses from a supplied retrieve message
   * @param	networkApplicationInformation	from which to obtain a map of application entity titles to presentation addresses
   * @param	secureTransport		true if to use secure transport protocol
   * @param	debugLevel			zero for no debugging messages, higher values more verbose messages
   * @exception	IOException
   */
  public StorageSOPClassSCPDispatcher(int port, String calledAETitle, File savedImagesFolder, ReceivedObjectHandler receivedObjectHandler,
     QueryResponseGeneratorFactory queryResponseGeneratorFactory, RetrieveResponseGeneratorFactory retrieveResponseGeneratorFactory,
     NetworkApplicationInformation networkApplicationInformation, boolean secureTransport, int debugLevel) throws IOException
  {
    this.port = port;
    this.calledAETitle = calledAETitle;
    this.ourMaximumLengthReceived = AssociationFactory.getDefaultMaximumLengthReceived();
    this.socketReceiveBufferSize = AssociationFactory.getDefaultReceiveBufferSize();
    this.socketSendBufferSize = AssociationFactory.getDefaultSendBufferSize();
    this.savedImagesFolder = savedImagesFolder;
    this.storedFilePathStrategy = StoredFilePathStrategy.getDefaultStrategy();
    this.receivedObjectHandler = receivedObjectHandler == null ? new DefaultReceivedObjectHandler()
                                 : receivedObjectHandler;
    this.queryResponseGeneratorFactory = queryResponseGeneratorFactory;
    this.retrieveResponseGeneratorFactory = retrieveResponseGeneratorFactory;
    this.networkApplicationInformation = networkApplicationInformation;
    this.debugLevel = debugLevel;
    this.secureTransport = secureTransport;
    this.presentationContextSelectionPolicy =
       new UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy();
  }

  /**
   * <p>Construct an instance of dispatcher that will wait for transport
   * connection open indications, and handle associations and commands.</p>
   *
   * @param	port								the port on which to listen for connections
   * @param	calledAETitle						our AE Title
   * @param	savedImagesFolder					the folder in which to store received data sets (may be null, to ignore received data for testing)
   * @param	storedFilePathStrategy				the strategy to use for naming received files and folders
   * @param	receivedObjectHandler				the handler to call after each data set has been received and stored, or null for the default that prints the file name
   * @param	queryResponseGeneratorFactory		the factory to make handlers to generate query responses from a supplied query message
   * @param	retrieveResponseGeneratorFactory	the factory to make handlers to generate retrieve responses from a supplied retrieve message
   * @param	networkApplicationInformation		from which to obtain a map of application entity titles to presentation addresses
   * @param	secureTransport						true if to use secure transport protocol
   * @param	debugLevel							zero for no debugging messages, higher values more verbose messages
   * @exception	IOException
   */
  public StorageSOPClassSCPDispatcher(int port, String calledAETitle, File savedImagesFolder, StoredFilePathStrategy storedFilePathStrategy, ReceivedObjectHandler receivedObjectHandler,
     QueryResponseGeneratorFactory queryResponseGeneratorFactory, RetrieveResponseGeneratorFactory retrieveResponseGeneratorFactory,
     NetworkApplicationInformation networkApplicationInformation, boolean secureTransport, int debugLevel) throws IOException
  {
    this.port = port;
    this.calledAETitle = calledAETitle;
    this.ourMaximumLengthReceived = AssociationFactory.getDefaultMaximumLengthReceived();
    this.socketReceiveBufferSize = AssociationFactory.getDefaultReceiveBufferSize();
    this.socketSendBufferSize = AssociationFactory.getDefaultSendBufferSize();
    this.savedImagesFolder = savedImagesFolder;
    this.storedFilePathStrategy = storedFilePathStrategy;
    this.receivedObjectHandler = receivedObjectHandler == null ? new DefaultReceivedObjectHandler()
                                 : receivedObjectHandler;
    this.queryResponseGeneratorFactory = queryResponseGeneratorFactory;
    this.retrieveResponseGeneratorFactory = retrieveResponseGeneratorFactory;
    this.networkApplicationInformation = networkApplicationInformation;
    this.debugLevel = debugLevel;
    this.secureTransport = secureTransport;
    this.presentationContextSelectionPolicy =
       new UnencapsulatedExplicitStoreFindMoveGetPresentationContextSelectionPolicy();
  }

  /**
   * <p>Construct an instance of dispatcher that will wait for transport
   * connection open indications, and handle associations and commands.</p>
   *
   * @param	port								the port on which to listen for connections
   * @param	calledAETitle						our AE Title
   * @param	savedImagesFolder					the folder in which to store received data sets (may be null, to ignore received data for testing)
   * @param	storedFilePathStrategy				the strategy to use for naming received files and folders
   * @param	receivedObjectHandler				the handler to call after each data set has been received and stored, or null for the default that prints the file name
   * @param	queryResponseGeneratorFactory		the factory to make handlers to generate query responses from a supplied query message
   * @param	retrieveResponseGeneratorFactory	the factory to make handlers to generate retrieve responses from a supplied retrieve message
   * @param	networkApplicationInformation		from which to obtain a map of application entity titles to presentation addresses
   * @param	presentationContextSelectionPolicy	which SOP Classes and Transfer Syntaxes to accept and reject
   * @param	secureTransport						true if to use secure transport protocol
   * @param	debugLevel							zero for no debugging messages, higher values more verbose messages
   * @exception	IOException
   */
  public StorageSOPClassSCPDispatcher(int port, String calledAETitle, File savedImagesFolder, StoredFilePathStrategy storedFilePathStrategy, ReceivedObjectHandler receivedObjectHandler,
     QueryResponseGeneratorFactory queryResponseGeneratorFactory, RetrieveResponseGeneratorFactory retrieveResponseGeneratorFactory,
     NetworkApplicationInformation networkApplicationInformation,
     PresentationContextSelectionPolicy presentationContextSelectionPolicy,
     boolean secureTransport, int debugLevel) throws IOException
  {
    this.port = port;
    this.calledAETitle = calledAETitle;
    this.ourMaximumLengthReceived = AssociationFactory.getDefaultMaximumLengthReceived();
    this.socketReceiveBufferSize = AssociationFactory.getDefaultReceiveBufferSize();
    this.socketSendBufferSize = AssociationFactory.getDefaultSendBufferSize();
    this.savedImagesFolder = savedImagesFolder;
    this.storedFilePathStrategy = storedFilePathStrategy;
    this.receivedObjectHandler = receivedObjectHandler == null ? new DefaultReceivedObjectHandler()
                                 : receivedObjectHandler;
    this.queryResponseGeneratorFactory = queryResponseGeneratorFactory;
    this.retrieveResponseGeneratorFactory = retrieveResponseGeneratorFactory;
    this.networkApplicationInformation = networkApplicationInformation;
    this.debugLevel = debugLevel;
    this.secureTransport = secureTransport;
    this.presentationContextSelectionPolicy = presentationContextSelectionPolicy;
  }

  /**
   * <p>Request the dispatcher to stop listening and exit the thread.</p>
   */
  public void shutdown()
  {
    wantToShutdown = true;
  }

  /**
   * <p>Waits for a transport connection indications, then spawns
   * new threads to act as association acceptors, which then wait for storage or
   * verification commands, storing data sets in Part 10 files in the specified folder, until the associations
   * are released or the transport connections are closed.</p>
   */
  public void run()
  {
    wantToShutdown = false;
    ServerSocket serverSocket = null;

    try
    {
      if(debugLevel > 1)
        System.err.println("StorageSOPClassSCPDispatcher.run(): Trying to bind to port " + port);

      if(secureTransport)
      {
        SSLServerSocketFactory sslserversocketfactory =
           (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket sslserversocket =
           (SSLServerSocket) sslserversocketfactory.createServerSocket(port);

        String[] suites =
           Association.getCipherSuitesToEnable(sslserversocket.getSupportedCipherSuites());
        if(suites != null)
        {
          sslserversocket.setEnabledCipherSuites(suites);
        }

        String[] protocols = Association.getProtocolsToEnable(sslserversocket.getEnabledProtocols());
        if(protocols != null)
        {
          sslserversocket.setEnabledProtocols(protocols);
        }

        //sslserversocket.setNeedClientAuth(true);
        serverSocket = sslserversocket;
      }
      else
      {
        serverSocket = new ServerSocket(port);
      }

      // set socket timeout
      serverSocket.setSoTimeout(timeoutBeforeCheckingForInterrupted);

      while(!wantToShutdown)
      {
        try
        {
          Socket socket = serverSocket.accept();

          if(debugLevel > 2)
            System.err.println("StorageSOPClassSCPDispatcher.run(): returned from accept");

          //setSocketOptions(socket,ourMaximumLengthReceived,socketReceiveBufferSize,socketSendBufferSize,debugLevel);
          // defer loading applicationEntityMap until each incoming connection, since may have been updated
          ApplicationEntityMap applicationEntityMap = null;
          if(networkApplicationInformation != null)
          {
            applicationEntityMap = networkApplicationInformation.getApplicationEntityMap();
          }
          if(applicationEntityMap == null)
          {
            applicationEntityMap = new ApplicationEntityMap();
          }

          // add ourselves to AET map, if not already there, in case we want to C-MOVE to ourselves
          if(calledAETitle != null)
          {
            InetAddress ourAddress = serverSocket.getInetAddress();
            if(ourAddress != null && applicationEntityMap.get(calledAETitle) == null)
            {
              applicationEntityMap.put(calledAETitle, new PresentationAddress(ourAddress.getHostAddress(), port),
                 NetworkApplicationProperties.StudyRootQueryModel/*hmm... :(*/, null/*primaryDeviceType*/);
            }
          }

          if(debugLevel > 1)
            System.err.println("StorageSOPClassSCPDispatcher:run(): applicationEntityMap = " + applicationEntityMap);

          try
          {
            StorageSOPClassSCP sscp = new StorageSOPClassSCP(socket, calledAETitle,
               ourMaximumLengthReceived, socketReceiveBufferSize, socketSendBufferSize, savedImagesFolder, storedFilePathStrategy,
               receivedObjectHandler, queryResponseGeneratorFactory, retrieveResponseGeneratorFactory,
               applicationEntityMap,
               presentationContextSelectionPolicy,
               debugLevel);

            // add all listner
            sscp.arAssocListner.addAll(arAssocListner);

            Thread th = new Thread(sscp);
            th.setName("Client " + socket.getInetAddress());
            th.setDaemon(true);
            th.start();
          }
          catch(Exception e)
          {
            e.printStackTrace(System.err);
          }
        }
        catch(SocketTimeoutException e)
        {
          if(debugLevel > 2)
            System.err.println("StorageSOPClassSCPDispatcher.run(): timed out in accept");
        }
      }
    }
    catch(IOException e)
    {
      e.printStackTrace(System.err);
    }
    try
    {
      if(serverSocket != null)
      {
        serverSocket.close();
      }
    }
    catch(Exception e)
    {
      e.printStackTrace(System.err);
    }

    if(debugLevel > 1)
      System.err.println("StorageSOPClassSCPDispatcher.run(): has shutdown and is no longer listening");
  }

  public void addListner(AssociationListener l)
  {
    arAssocListner.add(l);
  }

  public void removeListner(AssociationListener l)
  {
    arAssocListner.remove(l);
  }

  /**
   * <p>For testing.</p>
   *
   * <p>Wait for connections, accept associations and store received files in the specified folder.</p>
   *
   * @param	arg	array of zero, four, five or eight strings - our port, our AE Title,
   *			optionally the max PDU size, socket receive and send buffer sizes,
   *			the folder in which to stored received files (zero length if want to ignore received data),
   *			optionally a string flag valued SECURE or other,
   *			and the debugging level;
   *			if no arguments are supplied the properties in "~/.com.pixelmed.network.StorageSOPClassSCPDispatcher.properties" will be used if present,
   *			otherwise the defaults (11112,STORESCP,~/tmp,debug level 0) will be used - in this mode the service will also be self-registered with dns-sd if possible
   */
  public static void main(String arg[])
  {
    try
    {
      StorageSOPClassSCPDispatcher dispatcher = null;
      File savedImagesFolder = null;
      if(arg.length == 0)
      {
        Properties properties = new Properties();
        try
        {
          String propertiesFileName =
             ".com.pixelmed.network.StorageSOPClassSCPDispatcher.properties";
          properties.load(new FileInputStream(FileUtilities.makePathToFileInUsersHomeDirectory(propertiesFileName)));
        }
        catch(IOException e)
        {
          //e.printStackTrace(System.err);
          properties.put(NetworkApplicationProperties.propertyName_DicomListeningPort, "11112");
          properties.put(NetworkApplicationProperties.propertyName_DicomCalledAETitle, "STORESCP");
          properties.put(NetworkApplicationProperties.propertyName_DicomCallingAETitle, "STORESCP");
          properties.put(NetworkApplicationProperties.propertyName_PrimaryDeviceType, "ARCHIVE");
          properties.put(NetworkApplicationProperties.propertyName_StorageSCPDebugLevel, "0");
          properties.put(NetworkApplicationProperties.propertyName_NetworkDynamicConfigurationDebugLevel, "0");
          properties.put(DatabaseApplicationProperties.propertyName_SavedImagesFolderName, "tmp");
        }
        NetworkApplicationProperties networkApplicationProperties =
           new NetworkApplicationProperties(properties);
//System.err.println("NetworkApplicationProperties ="+properties);
        int port = networkApplicationProperties.getListeningPort();
        String calledAETitle = networkApplicationProperties.getCalledAETitle();
        int storageSCPDebugLevel = networkApplicationProperties.getStorageSCPDebugLevel();

        NetworkApplicationInformationFederated federatedNetworkApplicationInformation =
           new NetworkApplicationInformationFederated();
        federatedNetworkApplicationInformation.startupAllKnownSourcesAndRegister(networkApplicationProperties);
//System.err.println("NetworkApplicationInformationFederated ...\n"+federatedNetworkApplicationInformation);

        savedImagesFolder =
           new DatabaseApplicationProperties(properties).getSavedImagesFolderCreatingItIfNecessary();

        System.err.println("StorageSOPClassSCPDispatcher.main(): listening on port " + port + " AE " + calledAETitle + " storing into " + savedImagesFolder + " debugging level " + storageSCPDebugLevel);
        dispatcher = new StorageSOPClassSCPDispatcher(
           port,
           calledAETitle,
           savedImagesFolder,
           null, null, null, null,
           false,
           storageSCPDebugLevel);
      }
      else if(arg.length == 4)
      {
        String savedImagesFolderName = arg[2];
        if(savedImagesFolderName != null && savedImagesFolderName.length() > 0)
        {
          savedImagesFolder = new File(savedImagesFolderName);
        }
        dispatcher = new StorageSOPClassSCPDispatcher(Integer.parseInt(arg[0]), arg[1], savedImagesFolder,
           null, null, null, null,
           false, Integer.parseInt(arg[3])/*debugLevel*/);
      }
      else if(arg.length == 5)
      {
        String savedImagesFolderName = arg[2];
        if(savedImagesFolderName != null && savedImagesFolderName.length() > 0)
        {
          savedImagesFolder = new File(savedImagesFolderName);
        }
        dispatcher = new StorageSOPClassSCPDispatcher(Integer.parseInt(arg[0]), arg[1], savedImagesFolder,
           null, null, null, null,
           arg[3].toUpperCase().equals("SECURE"), Integer.parseInt(arg[4])/*debugLevel*/);
      }
      else if(arg.length == 8)
      {
        String savedImagesFolderName = arg[5];
        if(savedImagesFolderName != null && savedImagesFolderName.length() > 0)
        {
          savedImagesFolder = new File(savedImagesFolderName);
        }
        dispatcher = new StorageSOPClassSCPDispatcher(Integer.parseInt(arg[0]), arg[1],
           Integer.parseInt(arg[2]), Integer.parseInt(arg[3]), Integer.parseInt(arg[4]),
           savedImagesFolder,
           null, null, null, null,
           arg[6].toUpperCase().equals("SECURE"), Integer.parseInt(arg[7])/*debugLevel*/);
      }
      else
      {
        System.err.println("Usage: java -cp ./pixelmed.jar com.pixelmed.network.StorageSOPClassSCPDispatcher [port AET [ maxpdusize recbufsize sendbufsize ] folder [SECURE] debuglevel]");
      }
      new Thread(dispatcher).start();
    }
    catch(Exception e)
    {
      e.printStackTrace(System.err);
      System.exit(0);
    }
  }
}

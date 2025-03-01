/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */
package com.pixelmed.ftp;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import org.apache.commons.net.PrintCommandListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.UUID;

import java.security.NoSuchAlgorithmException;

import javax.swing.JProgressBar;

import com.pixelmed.display.event.StatusChangeEvent;

import com.pixelmed.event.ApplicationEventDispatcher;

import com.pixelmed.utils.MessageLogger;

/**
 * <p>
 * A class to send files via FTP or secure FTP over TLS.</p>
 *
 * @author	dclunie
 */
public class FTPFileSender
{
  /**
   * <p>
   * Construct an ftp connection to send a list of files to a remote server.</p>
   *
   * <p>
   * Sends a list of files to a single remote directory. Note that if the supplied local file names
   * have the same base name (same name in different local directories) then they wil overwrite each
   * other in the single remote directory; hence the option to generate random remote names.</p>
   *
   * @param	remoteHost	the characteristics of the remote host
   * @param	files	a String array of local filenames to send
   * @param	generateRandomRemoteFileNames	whether or not to generate random remote file names or to use the basename of the supplied local filename
   * @param	debugLevel	if greater than zero, debugging messages will be sent to stderr
   * @param	logger	where to send routine logging messages (may be null)
   * @param	progressBar	where to send progress updates (may be null)
   */
  public FTPFileSender(FTPRemoteHost remoteHost, String[] files, boolean generateRandomRemoteFileNames, int debugLevel, MessageLogger logger, JProgressBar progressBar)
     throws NoSuchAlgorithmException, IOException, Exception
  {
    this(remoteHost.getHost(), remoteHost.getUser(), remoteHost.getPassword(), remoteHost.getDirectory(), files, remoteHost.getSecurity().equals(FTPSecurityType.TLS), generateRandomRemoteFileNames, debugLevel, logger, progressBar);
  }

  /**
   * <p>
   * Construct an ftp connection to send a list of files to a remote server.</p>
   *
   * <p>
   * Sends a list of files to a single remote directory. Note that if the supplied local file names
   * have the same base name (same name in different local directories) then they wil overwrite each
   * other in the single remote directory; hence the option to generate random remote names.</p>
   *
   * @param	server	the hostname or IP address of the server
   * @param	username	the username for login
   * @param	password	the password for login
   * @param	remoteDirectory	the remote directory to upload the files to (may be null if the root directory is to be used)
   * @param	files	a String array of local filenames to send
   * @param	secure	whether or not to use secure ftp over tls, or ordinary ftp
   * @param	generateRandomRemoteFileNames	whether or not to generate random remote file names or to use the basename of the supplied local filename
   * @param	debugLevel	if greater than zero, debugging messages will be sent to stderr
   */
  public FTPFileSender(String server, String username, String password, String remoteDirectory, String[] files, boolean secure, boolean generateRandomRemoteFileNames, int debugLevel)
     throws NoSuchAlgorithmException, IOException, FTPException
  {
    this(server, username, password, remoteDirectory, files, secure, generateRandomRemoteFileNames, debugLevel, null, null);
  }

  /**
   * <p>
   * Construct an ftp connection to send a list of files to a remote server.</p>
   *
   * <p>
   * Sends a list of files to a single remote directory. Note that if the supplied local file names
   * have the same base name (same name in different local directories) then they wil overwrite each
   * other in the single remote directory; hence the option to generate random remote names.</p>
   *
   * @param	server	the hostname or IP address of the server
   * @param	username	the username for login
   * @param	password	the password for login
   * @param	remoteDirectory	the remote directory to upload the files to (may be null if the root directory is to be used)
   * @param	files	a String array of local filenames to send
   * @param	secure	whether or not to use secure ftp over tls, or ordinary ftp
   * @param	generateRandomRemoteFileNames	whether or not to generate random remote file names or to use the basename of the supplied local filename
   * @param	debugLevel	if greater than zero, debugging messages will be sent to stderr
   * @param	logger	where to send routine logging messages (may be null)
   * @param	progressBar	where to send progress updates (may be null)
   */
  public FTPFileSender(String server, String username, String password, String remoteDirectory, String[] files, boolean secure, boolean generateRandomRemoteFileNames, int debugLevel, MessageLogger logger, JProgressBar progressBar)
     throws NoSuchAlgorithmException, IOException, FTPException
  {
    FTPClient ftp = secure ? new FTPSClient("TLS", false/*isImplicit*/) : new FTPClient();
    if(debugLevel > 0)
      ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.err)));

    try
    {
      int reply;
      if(secure)
      {
        try
        {
          if(debugLevel > 0)
            System.err.println("Trying to connect in explicit mode to " + server);
          ftp.connect(server);
        }
        catch(Exception e)
        {
          if(debugLevel > 0)
            e.printStackTrace(System.err);
          if(debugLevel > 0)
            System.err.println("Failed to connect in explicit mode to " + server + " so trying again in implicit mode on port 990");
          // failed so try implicit mode on port 990
          ftp = new FTPSClient("TLS", true/*isImplicit*/);
          ftp.setDefaultPort(990);
          if(debugLevel > 0)
            ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.err)));
          ftp.connect(server);
        }
      }
      else
      {
        // Let any failure fall through with no retry ...
        ftp.connect(server);
      }
      if(debugLevel > 0)
        System.err.println("Connected to " + server);
      if(debugLevel > 0)
        System.err.print(ftp.getReplyString());
      reply = ftp.getReplyCode();
      if(!FTPReply.isPositiveCompletion(reply))
      {
        ftp.disconnect();
        throw new FTPException("FTP server " + server + " refused connection");
      }
      if(secure)
      {
        ((FTPSClient) ftp).execPBSZ(0);			// required, but only value permitted is 0
        ((FTPSClient) ftp).execPROT("P");		// otherwise transfers will be unencrypted
      }
      if(!ftp.login(username, password))
      {
        ftp.disconnect();
        throw new FTPException("FTP server " + server + " login failed");
      }
      // transfer files

      ftp.enterLocalPassiveMode();

      if(remoteDirectory != null && remoteDirectory.length() > 0)
      {
        if(!ftp.changeWorkingDirectory(remoteDirectory))
        {
          ftp.disconnect();
          throw new FTPException("FTP server " + server + " cwd to " + remoteDirectory + " failed");
        }
        if(debugLevel > 0)
          System.err.println("Working directory is now " + ftp.printWorkingDirectory());
      }

      if(!ftp.setFileType(FTP.BINARY_FILE_TYPE))
      {
        ftp.disconnect();
        throw new FTPException("FTP server " + server + " set file type to Binary failed");
      }

      ApplicationEventDispatcher applicationEventDispatcher = ApplicationEventDispatcher.getApplicationEventDispatcher();
      int maximum = files.length;
      if(progressBar != null)
      {
        progressBar.setValue(0);
        progressBar.setMaximum(maximum);
        progressBar.setStringPainted(true);
        progressBar.repaint();
      }
      int done = 0;
      for(String localFilename : files)
      {
        if(progressBar != null)
        {
          progressBar.setValue(done);
          progressBar.setStringPainted(true);
          progressBar.repaint();
        }
        File localFile = new File(localFilename);
        InputStream i = new FileInputStream(localFile);
        String remoteFilename = generateRandomRemoteFileNames ? UUID.randomUUID().toString() : localFile.getName();
        if(debugLevel > 0)
          System.err.println("Attempting to store local " + localFilename + " to remote " + remoteFilename);
        if(!ftp.storeFile(remoteFilename, i))
        {
          ftp.disconnect();
          throw new FTPException("FTP server " + server + " file store of local " + localFilename + " to remote " + remoteFilename + " failed");
        }
        i.close();
        if(debugLevel > 0)
          System.err.println("Successfully stored local " + localFilename + " to remote " + remoteFilename);
        if(logger != null)
        {
          logger.sendLn("Successfully stored local " + localFilename + " to remote " + remoteFilename);
        }
        if(progressBar != null)
        {
          progressBar.setValue(maximum);
          progressBar.setStringPainted(true);
          progressBar.repaint();
        }
        if(applicationEventDispatcher != null)
        {
          applicationEventDispatcher.processEvent(new StatusChangeEvent("Sent " + localFilename + " to Registry"));
        }
        ++done;
      }

      ftp.logout();
    }
    finally
    {
      if(ftp.isConnected())
      {
        if(debugLevel > 0)
          System.err.println("FTPFileSender(): finally so disconnect");
        ftp.disconnect();
      }
    }
  }

  public static void main(String arg[])
  {
    try
    {
      String server = arg[0];
      String username = arg[1];
      String password = arg[2];
      String remoteDirectory = arg[3];
      if(remoteDirectory.equals("-") || remoteDirectory.equals("."))
      {
        remoteDirectory = null;
      }
      boolean secure = arg[4].toUpperCase().trim().equals("SECURE");
      boolean generateRandomRemoteFileNames = arg[5].toUpperCase().trim().equals("RANDOM");
      int debugLevel = Integer.parseInt(arg[6]);

      int numberOfFiles = arg.length - 7;
      String[] files = new String[numberOfFiles];
      System.arraycopy(arg, 7, files, 0, numberOfFiles);
      FTPFileSender sender = new FTPFileSender(server,
         username, password, remoteDirectory, files,
         secure, generateRandomRemoteFileNames, debugLevel);
    }
    catch(Exception e)
    {
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }
}

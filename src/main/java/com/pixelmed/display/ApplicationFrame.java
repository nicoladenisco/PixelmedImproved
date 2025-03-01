/* Copyright (c) 2001-2008, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */
package com.pixelmed.display;

import com.pixelmed.utils.FileUtilities;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;

//import com.pixelmed.display.event.*;
/**
 * <p>This class provides the infrastructure for creating applications (which extend
 * this class) and provides them with utilities for creating a main window with a
 * title and default close and dispose behavior, as well as access to properties,
 * and a window snapshot function.</p>
 *
 * @author	dclunie
 */
public class ApplicationFrame extends JFrame
{
  private static final String identString =
     "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/ApplicationFrame.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";
  protected static String releaseString = "General Release";	// build date read in at run time

  /**
   * <p>Get the release string for this application.</p>
   *
   * @return	 the release string
   */
  protected String getReleaseString()
  {
    return releaseString;
  }

  /**
   * <p>Get the date the package was built.</p>
   *
   * @return	 the build date
   */
  protected String getBuildDate()
  {
    String buildDate = "";
    try
    {
      buildDate = (new BufferedReader(new InputStreamReader(
         ApplicationFrame.class.getResourceAsStream("/BUILDDATE")))).readLine();
    }
    catch(Exception e)
    {
    }
    return buildDate;
  }
  protected StatusBarManager statusBarManager;		// maintain a strong reference else weak reference to listener gets nulled when garbage collected

  /**
   * <p>Setup a StatusBarManager and return its StatusBar.</p>
   *
   * <p>The initial string in the StatusBar is composed of the build date and release string.</p>
   *
   * @return	 the StatusBar
   */
  protected JLabel getStatusBar()
  {
    statusBarManager = new StatusBarManager(getBuildDate() + " " + getReleaseString());		// maintain a strong reference else weak reference to listener gets nulled when garbage collected
    return statusBarManager.getStatusBar();
  }

  /**
   * <p>Given a file name, such as the properties file name, make a path to it in the user's home directory.</p>
   *
   * @param	fileName	 the file name to make a path to
   */
  protected static String makePathToFileInUsersHomeDirectory(String fileName)
  {
    return FileUtilities.makePathToFileInUsersHomeDirectory(fileName);
  }
  private Properties applicationProperties;
  private String applicationPropertyFileName;

  /**
   * <p>Store the properties from the current properties file.</p>
   */
  protected void loadProperties()
  {
    applicationProperties = new Properties(/*defaultProperties*/);
    if(applicationPropertyFileName != null)
    {
      String whereFrom = makePathToFileInUsersHomeDirectory(applicationPropertyFileName);
      try
      {
        // load properties from last invocation
        FileInputStream in = new FileInputStream(whereFrom);
        applicationProperties.load(in);
        in.close();
      }
      catch(IOException e)
      {
        System.err.println(e);
      }
    }
  }

  /**
   * <p>Store the current properties in the current properties file.</p>
   *
   * @param	comment		the description to store as the header of the properties file
   * @exception	IOException
   */
  protected void storeProperties(String comment) throws IOException
  {
    if(applicationPropertyFileName == null)
    {
      throw new IOException("asked to store properties but no applicationPropertyFileName was ever set");
    }
    else
    {
      String whereTo = makePathToFileInUsersHomeDirectory(applicationPropertyFileName);
      FileOutputStream out = new FileOutputStream(whereTo);
      applicationProperties.store(out, comment);
      out.close();
    }
  }

  /**
   * <p>Get the properties for the application that have already been loaded (see {@link #loadProperties() loadProperties()}).</p>
   *
   * @return	the properties
   */
  protected Properties getProperties()
  {
    return applicationProperties;
  }

  /**
   * <p>Get the name of the property file set for the application.</p>
   *
   * @return	the property file name
   */
  protected String getApplicationPropertyFileName()
  {
    return applicationPropertyFileName;
  }

  /**
   * <p>Set the name of the property file set for the application.</p>
   *
   * @param	applicationPropertyFileName	the property file name
   */
  protected void setApplicationPropertyFileName(String applicationPropertyFileName)
  {
    this.applicationPropertyFileName = applicationPropertyFileName;
  }

  /**
   * <p>Searches for the property with the specified key in the specified property list, insisting on a value.</p>
   *
   * @param	properties	the property list to search
   * @param	key		the property name
   * @throws	Exception	if there is no such property or it has no value
   */
  static public String getPropertyInsistently(Properties properties, String key) throws Exception
  {
    String value = properties.getProperty(key);
    if(value == null || value.length() == 0)
    {
      throw new Exception("Properties do not contain value for " + key);
    }
    return value;
  }

  /**
   * <p>Searches for the property with the specified key in this application's property list, insisting on a value.</p>
   *
   * @param	key		the property name
   * @throws	Exception	if there is no such property or it has no value
   */
  public String getPropertyInsistently(String key) throws Exception
  {
    return getPropertyInsistently(applicationProperties, key);
  }

  /**
   * <p>Store a JPEG snapshot of the specified window in the user's home directory.</p>
   *
   * @param	extent		the rectangle to take a snapshot of (typically <code>this.getBounds()</code> for whole application)
   */
  protected File takeSnapShot(Rectangle extent)
  {
    File snapShotFile = null;
    try
    {
      snapShotFile = File.createTempFile("snap", ".jpg", new File(System.getProperty("user.home")));
      java.awt.image.BufferedImage snapShotImage = new Robot().createScreenCapture(extent);
      ImageIO.write(snapShotImage, "jpeg", snapShotFile);
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
    }
    return snapShotFile;
  }

  /**
   * <p>Construct a window with the default size and title and no property source.</p>
   *
   * <p>Does not show the window.</p>
   *
   */
  public ApplicationFrame()
  {
    //this("Application Frame",640,480);
    this("Application Frame", null);
  }

  /**
   * <p>Construct a window with the default size, specified title and no property source.</p>
   *
   * <p>Does not show the window.</p>
   *
   * @param	title				the title for the top bar decoration
   */
  public ApplicationFrame(String title)
  {
    //this(title,640,480);
    this(title, null);
  }

  /**
   * <p>Construct a window with the default size, and specified title and property sources.</p>
   *
   * <p>Does not show the window.</p>
   *
   * @param	title				the title for the top bar decoration
   * @param	applicationPropertyFileName	the name of the properties file
   */
  public ApplicationFrame(String title, String applicationPropertyFileName)
  {
    setApplicationPropertyFileName(applicationPropertyFileName);
    loadProperties();
    if(title != null)
      setTitle(title);
    createGUI();
    //setSize(640,480);
  }

  /**
   * <p>Construct a window with the specified size, title and property sources.</p>
   *
   * <p>Does not show the window.</p>
   *
   * @param	title				the title for the top bar decoration
   * @param	applicationPropertyFileName	the name of the properties file
   * @param	w				width
   * @param	h				height
   */
  public ApplicationFrame(String title, String applicationPropertyFileName, int w, int h)
  {
    setApplicationPropertyFileName(applicationPropertyFileName);
    loadProperties();
    if(title != null)
      setTitle(title);
    createGUI();
    setSize(w, h);
  }

  /**
   * <p>Setup internationalized fonts if possible.</p>
   */
  protected void setInternationalizedFontsForGUI()
  {
//System.err.println("ApplicationFrame.setInternationalizedFontsForGUI()");

    Font font = new Font("Arial Unicode MS", Font.PLAIN, 12);
    if(font == null || !font.getFamily().equals("Arial Unicode MS"))
    {
      font = new Font("Bitstream Cyberbit", Font.PLAIN, 13);
      if(font == null || !font.getFamily().equals("Bitstream Cyberbit"))
      {
        font = null;
      }
    }
    if(font == null)
    {
      System.err.println("Warning: couldn't set internationalized font: non-Latin values may not display properly");
    }
    else
    {
      System.err.println("Using internationalized font " + font);
      UIManager.put("Tree.font", font);
      UIManager.put("Table.font", font);
      //UIManager.put("Label.font",font);
    }

  }

  /**
   * <p>Do what is necessary to build an application window that closes when told.</p>
   */
  protected void createGUI()
  {
//System.err.println("ApplicationFrame.createGUI()");

    setBackground(Color.lightGray);

    setInternationalizedFontsForGUI();

    //WindowEventHandler winAda = new WindowEventHandler();
    //addWindowListener(winAda);

    //setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
//System.err.println("ApplicationFrame.WindowAdapter.windowClosing()");
        dispose();
        //System.exit(0);
      }
    });

    //AppMenuBar AppMenuBar = new AppMenuBar();
    //setJMenuBar((JMenuBar)AppMenuBar);
  }

  /**
   * <p>For testing.</p>
   *
   * <p>Shows an empty default sized window.</p>
   *
   * @param	arg	ignored
   */
  public static void main(String arg[])
  {
    ApplicationFrame af = new ApplicationFrame();
    af.setVisible(true);
  }
}

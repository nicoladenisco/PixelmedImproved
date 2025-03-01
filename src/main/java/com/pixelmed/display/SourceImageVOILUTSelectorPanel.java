/* Copyright (c) 2001-2005, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */
package com.pixelmed.display;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;

import com.pixelmed.display.event.VOIFunctionChangeEvent;
//import com.pixelmed.dicom.*;
import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class SourceImageVOILUTSelectorPanel extends JPanel
{
  private static final String identString =
     "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/SourceImageVOILUTSelectorPanel.java,v 1.2 2011-05-21 18:41:49 nicola Exp $";
  /***/
  protected EventContext eventContext;
  /***/
  protected ButtonGroup voiLUTShapeButtons;
  /***/
  protected JRadioButton linearButton;
  /***/
  protected JRadioButton logisticButton;

  /***/
  public class VOILUTShapeActionListener implements ActionListener
  {
    /**
     */
    public VOILUTShapeActionListener()
    {
    }

    /**
     * @param	event
     */
    public void actionPerformed(ActionEvent event)
    {
//System.err.println("VOILUTShapeActionListener.VOILUTShapeActionListener.actionPerformed()");
      sendEventCorrespondingToCurrentButtonState();
    }
  }

  public void sendEventCorrespondingToCurrentButtonState()
  {
    String choice = voiLUTShapeButtons.getSelection().getActionCommand();
    try
    {
      ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(
         new VOIFunctionChangeEvent(eventContext, choice));
    }
    catch(Exception e)
    {
      e.printStackTrace(System.err);
    }
  }

  protected SourceImageVOILUTSelectorPanel()
  {
  }
  
  /**
   * @param	eventContext
   */
  public SourceImageVOILUTSelectorPanel(EventContext eventContext)
  {
    this.eventContext = eventContext;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    JPanel voiLUTControlsPanel = new JPanel();
    add(voiLUTControlsPanel);

    voiLUTControlsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    voiLUTControlsPanel.add(new JLabel("Use VOI LUT shape:"));

    voiLUTShapeButtons = new ButtonGroup();
    VOILUTShapeActionListener listener = new VOILUTShapeActionListener();

    linearButton = new JRadioButton("linear", true);
    linearButton.setActionCommand(VOIFunctionChangeEvent.linearFunction);
    linearButton.setToolTipText("Use linear ramp");
    linearButton.addActionListener(listener);
    voiLUTShapeButtons.add(linearButton);
    voiLUTControlsPanel.add(linearButton);

    logisticButton = new JRadioButton("logistic", false);
    logisticButton.setActionCommand(VOIFunctionChangeEvent.logisticFunction);
    logisticButton.setToolTipText("Use logistic curve with window center and width parameters");
    logisticButton.addActionListener(listener);
    voiLUTShapeButtons.add(logisticButton);
    voiLUTControlsPanel.add(logisticButton);
  }
}

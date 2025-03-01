/* Copyright (c) 2001-2006, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */
package com.pixelmed.display;

import com.pixelmed.event.EventContext;
import com.pixelmed.event.SelfRegisteringListener;
import com.pixelmed.display.event.SourceImageSelectionChangeEvent;

/**
 * @author	dclunie
 */
public class SourceImageSortOrderPanel extends SourceInstanceSortOrderPanel
{
  private static final String identString =
     "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/SourceImageSortOrderPanel.java,v 1.2 2011-05-21 18:41:49 nicola Exp $";

  // implement SourceImageSelectionChangeListener ...
  protected OurSourceImageSelectionChangeListener ourSourceImageSelectionChangeListener;

  public class OurSourceImageSelectionChangeListener extends SelfRegisteringListener
  {
    /**
     * @param	eventContext
     */
    public OurSourceImageSelectionChangeListener(EventContext eventContext)
    {
      super("com.pixelmed.display.event.SourceImageSelectionChangeEvent", eventContext);
//System.err.println("SourceImageSortOrderPanel.OurSourceImageSelectionChangeListener():");
    }

    /**
     * @param	e
     */
    public void changed(com.pixelmed.event.Event e)
    {
      SourceImageSelectionChangeEvent sis = (SourceImageSelectionChangeEvent) e;
      byFrameOrderButton.setSelected(true);
      nSrcInstances = sis.getNumberOfBufferedImages();			// sets in parent, else Slider won't appear when we update it later
      currentSrcInstanceAttributeList = sis.getAttributeList();
      replaceListOfDimensions(buildListOfDimensionsFromAttributeList(currentSrcInstanceAttributeList));
      currentSrcInstanceSortOrder = sis.getSortOrder();
      currentSrcInstanceIndex = sis.getIndex();
      updateCineSlider(1, nSrcInstances, currentSrcInstanceIndex + 1);
//System.err.println("SourceImageSortOrderPanel.OurSourceImageSelectionChangeListener.changed(): on exit nSrcInstances = "+nSrcInstances);
//System.err.println("SourceImageSortOrderPanel.OurSourceImageSelectionChangeListener.changed(): on exit currentSrcInstanceIndex = "+currentSrcInstanceIndex);
//System.err.println("SourceImageSortOrderPanel.OurSourceImageSelectionChangeListener.changed(): on exit currentSrcInstanceSortOrder = "+currentSrcInstanceSortOrder);
    }
  }

  protected SourceImageSortOrderPanel()
  {
  }
  
  /**
   * @param	typeOfPanelEventContext
   */
  public SourceImageSortOrderPanel(EventContext typeOfPanelEventContext)
  {
    super(typeOfPanelEventContext);
    ourSourceImageSelectionChangeListener =
       new OurSourceImageSelectionChangeListener(typeOfPanelEventContext);
  }
}

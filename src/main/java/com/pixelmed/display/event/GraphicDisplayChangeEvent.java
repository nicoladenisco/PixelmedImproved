/* Copyright (c) 2001-2008, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */
package com.pixelmed.display.event;

import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class GraphicDisplayChangeEvent extends Event
{
  private static final String identString =
     "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/event/GraphicDisplayChangeEvent.java,v 1.2 2011-09-30 19:48:34 nicola Exp $";
  private boolean overlays;

  /**
   * @param	eventContext
   * @param	overlays
   */
  public GraphicDisplayChangeEvent(EventContext eventContext, boolean overlays)
  {
    super(eventContext);
    this.overlays = overlays;
  }

  /***/
  public boolean showOverlays()
  {
    return overlays;
  }
}

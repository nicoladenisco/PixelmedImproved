package com.pixelmed.display;

/**
 * A listner to notify change in selected image in IconListBrowser.
 *
 * @author Nicola De Nisco
 */
public interface IconListBrowserListener
{
  public void selectionChange(int clickCount, int currItem, int[] selectedItems);
}

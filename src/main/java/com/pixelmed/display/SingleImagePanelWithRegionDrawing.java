/* Copyright (c) 2001-2007, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.Point;
import java.awt.Shape;

import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.EventContext;

import com.pixelmed.display.event.RegionSelectionChangeEvent; 

import com.pixelmed.geometry.GeometryOfSlice;
import com.pixelmed.geometry.GeometryOfVolume;

/**
 * <p>Implements a component that extends a SingleImagePanel to also draw regions.</p>
 *
 * @see com.pixelmed.display.SourceImage
 *
 * @author	dclunie
 */
public class SingleImagePanelWithRegionDrawing extends SingleImagePanel {

	/***/
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/SingleImagePanelWithRegionDrawing.java,v 1.2 2011-05-21 18:41:49 nicola Exp $";

	// Constructors ...

	public SingleImagePanelWithRegionDrawing(SourceImage sImg,EventContext typeOfPanelEventContext,int[] sortOrder,Vector preDefinedShapes,Vector preDefinedText,GeometryOfVolume imageGeometry) {
		super(sImg,typeOfPanelEventContext,sortOrder,preDefinedShapes,preDefinedText,imageGeometry);
	}

	public SingleImagePanelWithRegionDrawing(SourceImage sImg,EventContext typeOfPanelEventContext,GeometryOfVolume imageGeometry) {
		super(sImg,typeOfPanelEventContext,imageGeometry);
	}

	public SingleImagePanelWithRegionDrawing(SourceImage sImg,EventContext typeOfPanelEventContext) {
		super(sImg,typeOfPanelEventContext);
	}

	public SingleImagePanelWithRegionDrawing(SourceImage sImg) {
		super(sImg);
	}

	// Region selection stuff (set by right mouse drag) ...
	
	private int regionSelectionCenterX;
	private int regionSelectionCenterY;
	private int regionSelectionTLHCX;
	private int regionSelectionTLHCY;
	private int regionSelectionBRHCX;
	private int regionSelectionBRHCY;

	/**
	 * @param	centerX
	 * @param	centerY
	 * @param	oneCornerX
	 * @param	oneCornerY
	 * @param	otherCornerX
	 * @param	otherCornerY
	 */
	private void setRegionSelection(int centerX,int centerY,int oneCornerX,int oneCornerY,int otherCornerX,int otherCornerY) {
//System.err.println("SingleImagePanelWithRegionDrawing.setRegionSelection() event: centerX="+centerX+" centerY="+centerY+" oneCornerX="+oneCornerX+" oneCornerY="+oneCornerY+" otherCornerX="+otherCornerX+" otherCornerY="+otherCornerY);
		regionSelectionCenterX = centerX;
		regionSelectionCenterY = centerY;
		if (oneCornerX < otherCornerX) {
			regionSelectionTLHCX=oneCornerX;
			regionSelectionBRHCX=otherCornerX;
		}
		else {
			regionSelectionTLHCX=otherCornerX;
			regionSelectionBRHCX=oneCornerX;
		}
		if (oneCornerY < otherCornerY) {
			regionSelectionTLHCY=oneCornerY;
			regionSelectionBRHCY=otherCornerY;
		}
		else {
			regionSelectionTLHCY=otherCornerY;
			regionSelectionBRHCY=oneCornerY;
		}
	}

	// Event stuff ...

	/**
	 * @param	e
	 */
	public void keyPressed(KeyEvent e) {
//System.err.println("Key pressed event"+e);
		if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
//System.err.println("Delete or backspace pressed");
			setSelectedDrawingShapes(null);
			repaint();
		}
		else {
			super.keyPressed(e);
		}
	}

	/**
	 * @param	e
	 */
	public void mouseClicked(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right clicked "+e.getX()+" "+e.getY());
			checkForHitOnPersistentShapes(e.getX(),e.getY());
		}
		else {
			super.mouseClicked(e);
		}
	}


	/**
	 * @param	e
	 */
	public void mouseDragged(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right dragged "+e.getX()+" "+e.getY());
			dragInteractiveDrawing(e.getX(),e.getY());
		}
		else {
			super.mouseDragged(e);
		}
	}

	/**
	 * @param	e
	 */
	public void mouseMoved(MouseEvent e) {
//System.err.println(e.getX()+" "+e.getY());
		super.mouseMoved(e);
	}

	/**
	 * @param	e
	 */
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right pressed "+e.getX()+" "+e.getY());
			startInteractiveDrawing(e.getX(),e.getY());
		}
		else {
			super.mousePressed(e);
		}
	}

	/**
	 * @param	e
	 */
	public void mouseReleased(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
//System.err.println("Right released "+e.getX()+" "+e.getY());
			endInteractiveDrawing(e.getX(),e.getY());	// sets region selection parameters to propagate in change event
			ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new RegionSelectionChangeEvent(typeOfPanelEventContext,
				regionSelectionCenterX,regionSelectionCenterY,regionSelectionTLHCX,regionSelectionTLHCY,regionSelectionBRHCX,regionSelectionBRHCY));
		}
		else {
			super.mouseReleased(e);
		}
	}

	// stuff to handle drawing ...
	
	/**
	 * <p>Construct a new shape from the specified coordinates.<p>
	 *
	 * <p>The default is a rectangle - override this method in a sub-class to use a different shape (e.g., an ellipse).<p>
	 *
	 * @param	x
	 * @param	y
	 * @param	width
	 * @param	height
	 */
	protected Shape makeNewDrawingShape(int tlhcX,int tlhcY,int width,int height) {
		return new Rectangle2D.Double(tlhcX,tlhcY,width,height);
	}

	/***/
	protected Point2D startPoint;
	/***/
	static final int crossSize = 5;		// actually just one arm of the cross

	/**
	 * @param	x
	 * @param	y
	 */
	protected void startInteractiveDrawing(int x,int y) {
		startPoint = getImageCoordinateFromWindowCoordinate(x,y);
	}

	/**
	 * @param	x
	 * @param	y
	 */
	protected void dragInteractiveDrawing(int x,int y) {
		int startX = (int)(startPoint.getX());
		int startY = (int)(startPoint.getY());
		Point endPoint = getImageCoordinateFromWindowCoordinate(x,y);
		int endX = (int)(endPoint.getX());
		int endY = (int)(endPoint.getY());
		if (startX != endX || startY != endY) {
			interactiveDrawingShapes = new Vector();
			interactiveDrawingShapes.add(new Line2D.Float(startPoint,endPoint));
			int tlhcX=startX;
			int width = endX - startX;
			if (width < 0) {
				width=-width;
				tlhcX=endX;
			}
			int tlhcY=startY;
			int height = endY - startY;
			if (height < 0) {
				height=-height;
				tlhcY=endY;
			}
			interactiveDrawingShapes.add(makeNewDrawingShape(tlhcX,tlhcY,width,height));
			repaint();
		}
		// else ignore, was click, not drag
	}
	
	/**
	 * @param	x
	 * @param	y
	 */
	protected void endInteractiveDrawing(int x,int y) {
		int startX = (int)(startPoint.getX());
		int startY = (int)(startPoint.getY());
		Point endPoint = getImageCoordinateFromWindowCoordinate(x,y);
		int endX = (int)(endPoint.getX());
		int endY = (int)(endPoint.getY());
		if (startX != endX || startY != endY) {
			int tlhcX=startX;
			int width = endX - startX;
			if (width < 0) {
				width=-width;
				tlhcX=endX;
			}
			int tlhcY=startY;
			int height = endY - startY;
			if (height < 0) {
				height=-height;
				tlhcY=endY;
			}
			setRegionSelection((tlhcX+width)/2,(tlhcY+height)/2,tlhcX,tlhcY,tlhcX+width,tlhcY+height);
			interactiveDrawingShapes = null;
			if (persistentDrawingShapes == null) {
				persistentDrawingShapes = new Vector();
			}
			persistentDrawingShapes.add(makeNewDrawingShape(tlhcX,tlhcY,width,height));
			repaint();
		}
		// else ignore, was click, not drag
	}

	/**
	 * @param	x
	 * @param	y
	 */
	protected void checkForHitOnPersistentShapes(int x,int y) {
		Point testPoint = getImageCoordinateFromWindowCoordinate(x,y);
		int testX = (int)(testPoint.getX());
		int testY = (int)(testPoint.getY());
		boolean changedSomething = false;
		Vector doneShapes = new Vector();
		// check previously selected shapes to toggle selection off if selected again ...
		if (selectedDrawingShapes != null) {
			Iterator i = selectedDrawingShapes.iterator();
			while (i.hasNext()) {
				Shape shape = (Shape)i.next();
				if (!doneShapes.contains(shape)) {
					doneShapes.add(shape);
					if (shape.contains(testX,testY)) {
//System.err.println("De-select shape "+shape);
						doneShapes.add(shape);
						if (persistentDrawingShapes == null) {
							persistentDrawingShapes = new Vector();
						}
						persistentDrawingShapes.add(shape);
						selectedDrawingShapes.remove(shape);
						i = selectedDrawingShapes.iterator();		// restart with new selector, since modified vector
						changedSomething=true;
					}
				}
			}
		}
		// not previously selected ... select any hit shape without undoing any de-selection from the previous step
		if (persistentDrawingShapes != null) {
			Iterator i = persistentDrawingShapes.iterator();
			while (i.hasNext()) {
				Shape shape = (Shape)i.next();
				if (!doneShapes.contains(shape)) {
					doneShapes.add(shape);
					if (shape.contains(testX,testY)) {
//System.err.println("Select shape "+shape);
						if (selectedDrawingShapes == null) {
							selectedDrawingShapes = new Vector();
						}
						selectedDrawingShapes.add(shape);
						persistentDrawingShapes.remove(shape);
						i = persistentDrawingShapes.iterator();		// restart with new selector, since modified vector
						changedSomething=true;
					}
				}
			}
		}
		if (changedSomething) {
			repaint();
		}
	}
}






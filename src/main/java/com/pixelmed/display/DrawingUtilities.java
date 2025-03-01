/* Copyright (c) 2001-2007, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

import java.util.Vector;

/**
 * <p>A class to provide various static methods for drawing.</p>
 *
 * @author	dclunie
 */
public class DrawingUtilities {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/display/DrawingUtilities.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";
			
	/**
	 * @param	string	the string to be drawn
	 * @param	x	x position
	 * @param	y	y position
	 * @param	g2d	the drawing context
	 */
	public static void drawShadowedString(String string,int x,int y,Graphics2D g2d) {
		Color holdColor = g2d.getColor();
		g2d.setColor(Color.black);
		g2d.drawString(string,x+1,y+1);
		g2d.setColor(holdColor);
		g2d.drawString(string,x,y);
	}

	/**
	 * @param	shape	the shape to be drawn
	 * @param	g2d	the drawing context
	 */
	public static void drawShadowedShape(Shape shape,Graphics2D g2d) {
		Color holdColor = g2d.getColor();
		g2d.setColor(Color.black);
		AffineTransform holdTransform = g2d.getTransform();
		g2d.translate(1.0d,1.0d);
		g2d.draw(shape);
		g2d.setColor(holdColor);
		g2d.setTransform(holdTransform);
		g2d.draw(shape);
	}

	/**
	 * Draw a diagonal cross at a specified location with a gap around the center
	 *
	 * @param	shapes		a vector of Shape to add to
	 * @param	x			the x cross center
	 * @param	y			the y cross center
	 * @param	crossSize	the length of one arm of the cross from end to center
	 * @param	crossGap	the gap in one arm of the cross from end to center (included in crossSize)
	 */
	public static void addDiagonalCross(Vector shapes,int x,int y,int crossSize,int crossGap) {
		shapes.add(new Line2D.Float(new Point(x-crossSize,y-crossSize),new Point(x-crossGap,y-crossGap)));
		shapes.add(new Line2D.Float(new Point(x+crossGap,y+crossGap),new Point(x+crossSize,y+crossSize)));
		shapes.add(new Line2D.Float(new Point(x+crossSize,y-crossSize),new Point(x+crossGap,y-crossGap)));
		shapes.add(new Line2D.Float(new Point(x-crossGap,y+crossGap),new Point(x-crossSize,y+crossSize)));
	}

	/**
	 * Draw a vertical cross at a specified location with a gap around the center
	 *
	 * @param	shapes		a vector of Shape to add to
	 * @param	x			the x cross center
	 * @param	y			the y cross center
	 * @param	crossSize	the length of one arm of the cross from end to center
	 * @param	crossGap	the gap in one arm of the cross from end to center (included in crossSize)
	 */
	public static void addVerticalCross(Vector shapes,int x,int y,int crossSize,int crossGap) {
		shapes.add(new Line2D.Float(new Point(x-crossSize,y),new Point(x-crossGap,y)));
		shapes.add(new Line2D.Float(new Point(x+crossGap,y),new Point(x+crossSize,y)));
		shapes.add(new Line2D.Float(new Point(x,y-crossSize),new Point(x,y-crossGap)));
		shapes.add(new Line2D.Float(new Point(x,y+crossGap),new Point(x,y+crossSize)));
	}
}


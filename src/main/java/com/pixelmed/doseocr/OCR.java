/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.doseocr;

import com.pixelmed.dose.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import java.util.Arrays;
import java.util.List;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.pixelmed.dicom.*;
import com.pixelmed.display.ConsumerFormatImageMaker;

/**
 * <p>A class for OCR of GE and Siemens modality dose report screen saves.</p>
 *
 * @author	dclunie
 * @author	giwarden
 */

public class OCR {
	
	public static String defaultFileNameOfKnownGlyphs = "OCR_Glyphs_DoseScreen.xml";
	
	private static int debugLevel;
	
	private static int maximumNumberOfConnections = 1200;

	private static int defaultGEHorizontalGapTolerance = 6; 
	private static int defaultGEVerticalGapTolerance = 4;
	
	private static int defaultSiemensHorizontalGapTolerance = 6; 
	private static int defaultSiemensVerticalGapTolerance = 2;		// giwarden - Siemens screens have less vertical spacing
	
	class ConnectednessException extends Exception {
		public ConnectednessException() {
			super("Exceeded maximum number of connections ... probably not a validly thresholded image");
		}
	}
	
	private BufferedImage image;
	private int height;
	private int width;
	private BitSet thresholdedPixels;
	private BitSet processedPixels;
	private Map<Glyph,String> mapOfGlyphsToStrings;
	private Map<Location,Glyph> mapOfRecognizedLocationsToGlyphs;
	private Map<Location,Glyph> mapOfUnrecognizedLocationsToGlyphs;
	private boolean trainingMode;
	
	private AttributeList list;
	
	public AttributeList getAttributeList() { return list; }
	
	private static final int getBitSetIndex(int x,int y,int width) { return y*width+x; }
	private static final int getXFromBitSetIndex(int bit,int width) { return bit%width; }
	private static final int getYFromBitSetIndex(int bit,int width) { return bit/width; }
	
	private final int getBitSetIndex(int x,int y) { return y*width+x; }
	private final int getXFromBitSetIndex(int bit) { return bit%width; }
	private final int getYFromBitSetIndex(int bit) { return bit/width; }
	
	private final boolean isPixelOn(int x,int y) { return thresholdedPixels.get(getBitSetIndex(x,y,width)); }
	
	private final boolean isProcessed(int x,int y) { return processedPixels.get(getBitSetIndex(x,y,width)); }
	
	private final void setProcessed(int x,int y) { processedPixels.set(getBitSetIndex(x,y,width)); }
	
	class Location implements Comparable {
		int x;
		int y;

		public int getX() { return x; }
		public int getY() { return y; }

		Location(int x,int y) {
			this.x = x;
			this.y = y;
		}
		
		// order is y then x
		public int compareTo(Object o) {
if (debugLevel > 3) System.err.println("Location.compareTo(): comparing "+this+" to "+o);
			int result = -1;
			if (o instanceof Location) {
				Location ol = (Location)o;
				if (y == ol.getY()) {
					result = x - ol.getX();
				}
				else {
					result = y - ol.getY();
				}
			}
if (debugLevel > 3) System.err.println("Location.compareTo(): result = "+result);
			return result;	
		}
		
		public boolean equals(Object o) {
if (debugLevel > 3) System.err.println("Location.equals(): comparing "+this+" to "+o);
			return compareTo(o) == 0;
		}
		
		public int hashCode() {
			return x+y;	// sufficient to implement equals() contract
		}

		public String toString() {
			return "("+x+","+y+")";
		}
	}
	
	class Glyph {
		BitSet set;
		int width;
		int height;
		boolean wasKnown;
		
		public BitSet getSet() { return set; }
		public int getWidth() { return width; }
		public int getHeight() { return height; }
		public boolean getWasKnown() { return wasKnown; }
		
		public String getString() { return mapOfGlyphsToStrings.get(this); }
		
		Glyph(BitSet srcSet,int srcSetWidth,boolean wasKnown) throws IllegalArgumentException {
if (debugLevel > 1) System.err.println("Glyph.Glyph(): srcSet = "+srcSet);
if (debugLevel > 1) System.err.println("Glyph.Glyph(): srcSetWidth = "+srcSetWidth);
			if(srcSet.isEmpty()) {
				throw new IllegalArgumentException("Cannot create Glyph from empty BitSet");
			}
			int tlhcX = findLowestXSetInBitSet(srcSet,srcSetWidth);
			int tlhcY = findLowestYSetInBitSet(srcSet,srcSetWidth);
			int brhcX = findHighestXSetInBitSet(srcSet,srcSetWidth);
			int brhcY = findHighestYSetInBitSet(srcSet,srcSetWidth);
if (debugLevel > 1) System.err.println("Glyph.Glyph(): srcSet TLHC ("+tlhcX+","+tlhcY+"), BRHC = ("+brhcX+","+brhcY+")");
			height=brhcY-tlhcY+1;
			width=brhcX-tlhcX+1;
if (debugLevel > 1) System.err.println("Glyph.Glyph(): new width = "+width+", height = "+height);
			set = new BitSet();
			int srcY=tlhcY;
			for (int dstY=0; dstY<height; ++dstY,++srcY) {
				int srcX=tlhcX;
				for (int dstX=0; dstX<width; ++dstX,++srcX) {
					if (srcSet.get(getBitSetIndex(srcX,srcY,srcSetWidth))) {
						set.set(getBitSetIndex(dstX,dstY,width));
					}
				}
			}
			this.wasKnown = wasKnown;
		}
		
		public boolean equals(Object o) {
			boolean result = false;
			if (o instanceof Glyph) {
				Glyph og = (Glyph)o;
				result = set.equals(og.getSet()) && width == og.getWidth() && height == og.getHeight();
				// do NOT compare string, since may not be set in one or the other
			}
			return result;
		}
		
		public int hashCode() {
			return set.hashCode();	// sufficient to implement equals() contract
		}
		
		public String toString() {
if (debugLevel > 1) System.err.println("Set = "+set);
			StringBuffer buf = new StringBuffer();
			for (int y=0; y < height; ++y ){
				for (int x=0; x < width; ++x) {
					if (set.get(getBitSetIndex(x,y,width))) {
						buf.append("# ");
					}
					else {
						buf.append(". ");
					}
				}
				buf.append("\n");
			}
			String string = getString();
			if (string != null) {
				buf.append("String: \""+string+"\"\n");
			}
			return buf.toString();
		}
		
		public String toXML() {
			StringBuffer buf = new StringBuffer();
			String string = getString();
			if (string != null && string.length() > 0) {
				buf.append("\t<glyph>\n");
				buf.append("\t\t<bits>\n");
				int length = set.length();
				if (length > 0) {
					for (int index=0; index < length; ++index) {
						if (set.get(index)) {
							buf.append("\t\t\t<bit>"+index+"</bit>\n");
						}
					}
				}
				buf.append("\t\t</bits>\n");
				buf.append("\t\t<width>"+width+"</width>\n");
				buf.append("\t\t<string>"+string+"</string>\n");
				buf.append("\t</glyph>\n");
			}
			return buf.toString();
		}
		
		public String toSourceCode() {
			StringBuffer buf = new StringBuffer();
			String string = getString();
			if (string != null && string.length() > 0) {
				buf.append("\t{\n");
				buf.append("\t\tBitSet set = new BitSet();\n");
				int length = set.length();
				if (length > 0) {
					for (int index=0; index < length; ++index) {
						if (set.get(index)) {
							buf.append("\t\tset.set("+index+");\n");
						}
					}
				}
				buf.append("\t\tmapOfGlyphsToStrings.put(new Glyph(set,"+width+",true),\""+string+"\");\n");
				buf.append("\t}\n");
			}
			return buf.toString();
		}
	}
	
	/**
	 * <p>Find the lowest X set in a BitSet.</p>
	 *
	 * @param	set
	 * @param	width
	 * return				the lowest X value, or -1 if no bit is set
	 */
	private static int findLowestXSetInBitSet(BitSet set,int width) {
		int found = Integer.MAX_VALUE;
		int count = set.cardinality();
		if (count > 0) {
			int length = set.length();
			assert(length > 0);
			int index = length-1;
			for (int counted=0; counted < count; --index) {
				if (set.get(index)) {
					int x = getXFromBitSetIndex(index,width);
					++counted;
					if (x < found) {
						found = x;
					}
				}
			}
		}
		else {
			found = -1;
		}
		return found;
	}
	
	/**
	 * <p>Find the lowest Y set in a BitSet.</p>
	 *
	 * @param	set
	 * @param	width
	 * return				the lowest Y value, or -1 if no bit is set
	 */
	private static int findLowestYSetInBitSet(BitSet set,int width) {
		int found = Integer.MAX_VALUE;
		int count = set.cardinality();
		if (count > 0) {
			int length = set.length();
			assert(length > 0);
			int index = length-1;
			for (int counted=0; counted < count; --index) {
				if (set.get(index)) {
					int y = getYFromBitSetIndex(index,width);
					++counted;
					if (y < found) {
						found = y;
					}
				}
			}
		}
		else {
			found = -1;
		}
		return found;
	}
	
	
	/**
	 * <p>Find the highest X set in a BitSet.</p>
	 *
	 * @param	set
	 * @param	width
	 * return				the highest X value, or -1 if no bit is set
	 */
	private static int findHighestXSetInBitSet(BitSet set,int width) {
if (debugLevel > 1) System.err.println("findHighestXSetInBitSet(): width = "+width+", set ="+set);
		int found = Integer.MIN_VALUE;
		int count = set.cardinality();
if (debugLevel > 1) System.err.println("findHighestXSetInBitSet(): cardinality = "+count);
		if (count > 0) {
			int length = set.length();
if (debugLevel > 1) System.err.println("findHighestXSetInBitSet(): length = "+length);
			assert(length > 0);
			int index = length-1;
			for (int counted=0; counted < count; --index) {
				if (set.get(index)) {
					int x = getXFromBitSetIndex(index,width);
if (debugLevel > 1) System.err.println("findHighestXSetInBitSet(): testing x = "+x);
					++counted;
					if (x > found) {
if (debugLevel > 1) System.err.println("findHighestXSetInBitSet(): found x = "+x);
						found = x;
					}
				}
			}
		}
		else {
			found = -1;
		}
if (debugLevel > 1) System.err.println("findHighestXSetInBitSet(): returning x = "+found);
		return found;
	}
	
	/**
	 * <p>Find the highest Y set in a BitSet.</p>
	 *
	 * @param	set
	 * @param	width
	 * return				the highest Y value, or -1 if no bit is set
	 */
	private static int findHighestYSetInBitSet(BitSet set,int width) {
		int found = Integer.MIN_VALUE;
		int count = set.cardinality();
		if (count > 0) {
			int length = set.length();
			assert(length > 0);
			int index = length-1;
			for (int counted=0; counted < count; --index) {
				if (set.get(index)) {
					int y = getYFromBitSetIndex(index,width);
					++counted;
					if (y > found) {
						found = y;
					}
				}
			}
		}
		else {
			found = -1;
		}
		return found;
	}
	
	private static BitSet threshold(BufferedImage image) {
		int bitsPerPixel = image.getColorModel().getPixelSize();
if (debugLevel > 2) System.err.println("OCR.threshold(): image pixel size (bpp) = "+bitsPerPixel);
		int thresholdValue = bitsPerPixel > 1 ? 127 : 0;	// just in case was lossy compressed in 8 bit case, but handle single bit image (e.g., from overlay)
if (debugLevel > 2) System.err.println("OCR.threshold(): thresholdValue = "+thresholdValue);
		int height = image.getHeight();
		int width = image.getWidth();
		Raster raster = image.getRaster();
		BitSet thresholdedPixels = new BitSet(height*width);
		int[] pixelValues = new int[1];
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				raster.getPixel(x,y,pixelValues);		// no need to assign retured value, since fills supplied array
if (debugLevel > 3) System.err.println("("+x+","+y+") pixelValue = "+pixelValues[0]);
				if (pixelValues[0] > thresholdValue) {
if (debugLevel > 1) System.err.println("Setting ("+x+","+y+") for pixelValue = "+pixelValues[0]);
					thresholdedPixels.set(getBitSetIndex(x,y,width));	// could optimize just with incremented index :(
				}
			}
		}
		return thresholdedPixels;
	}
	
	private int checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(int x,int y,BitSet set,int horizontalGapTolerance,int verticalGapTolerance,int numberOfConnections) throws ConnectednessException {
		if (numberOfConnections > maximumNumberOfConnections) {
			throw new ConnectednessException();
		}
if (debugLevel > 2) System.err.println("Check\t("+x+","+y+")");
if (debugLevel > 2) System.err.println("\t("+x+","+y+") in bounds="+(x > 0 && x < width && y > 0 && y < height));
if (debugLevel > 2) System.err.println("\t("+x+","+y+") isProcessed()="+isProcessed(x,y));
if (debugLevel > 2) System.err.println("\t("+x+","+y+") isPixelOn()="+isPixelOn(x,y));
		if (x > 0 && x < width && y > 0 && y < height && !isProcessed(x,y) && isPixelOn(x,y)) {
if (debugLevel > 2) System.err.println("Doing\t("+x+","+y+")");
			setProcessed(x,y);
			set.set(getBitSetIndex(x,y));
			++numberOfConnections;
			numberOfConnections = walkConnectionsRecordingThem(x,y,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);	// recurse
		}
		return numberOfConnections;
	}
	
	private int walkConnectionsRecordingThem(int x,int y,BitSet set,int horizontalGapTolerance,int verticalGapTolerance,int numberOfConnections) throws ConnectednessException {
		// walk each cardinal and oblique direction ...
		for (int yDelta=1; yDelta <= verticalGapTolerance; ++yDelta) {
			for (int xDelta=1; xDelta <= horizontalGapTolerance; ++xDelta) {
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x-xDelta,y-yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x-xDelta,y       ,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x-xDelta,y+yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x       ,y-yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x       ,y       ,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x       ,y+yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x+xDelta,y-yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x+xDelta,y       ,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
				numberOfConnections = checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x+xDelta,y+yDelta,set,horizontalGapTolerance,verticalGapTolerance,numberOfConnections);
			}
		}
		return numberOfConnections;
	}
	
	private String processCandidate(int x,int y,int blockY,int horizontalGapTolerance,int verticalGapTolerance,boolean recordLocationWhenRecognized) throws IOException, ConnectednessException {
		String matched = null;
		if (!isProcessed(x,y) && isPixelOn(x,y)) {
if (debugLevel > 1) System.err.println("Candidate at ("+x+","+y+")");
if (debugLevel > 1) System.err.println("\t("+x+","+y+") isProcessed()="+isProcessed(x,y));
if (debugLevel > 1) System.err.println("\t("+x+","+y+") isPixelOn()="+isPixelOn(x,y));
			Location start = new Location(x,y);
			BitSet set = new BitSet();	// size expands as we walk
			try {
				checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse(x,y,set,horizontalGapTolerance,verticalGapTolerance,0);	// repeat check on bounds and processed and is on redundant for this first pixel
			}
			catch (ConnectednessException e) {
				// don't want (huge) stacktrace in this case, re-throw it, since we do actually want to stop processing the file
				throw new ConnectednessException();
			}
			if (set.isEmpty()) {
if (debugLevel > 1) System.err.println("\tduring connectedness search, get empty set back from checkInBoundsAndNotProcessedAndPixelIsOnAndIfSoRecordAndRecurse() for candidate at ("+x+","+y+")");
			}
			else {
				// note that the location we need (TLHC) may have moved due to connecting to the left or upwards ...
				int lowestX = findLowestXSetInBitSet(set,width);
				if (lowestX < 0) {
if (debugLevel > 1) System.err.println("\tduring connectedness search, lowestX for TLHC is out of bounds = "+lowestX+", using 0");
					lowestX = 0;
				}
				int lowestY = findLowestYSetInBitSet(set,width);
				if (lowestY < 0) {
if (debugLevel > 1) System.err.println("\tduring connectedness search, lowestY for TLHC is out of bounds = "+lowestY+", using 0");
					lowestY = 0;
				}
				Location tlhc = new Location(lowestX,lowestY);
				if (!tlhc.equals(start)) {
if (debugLevel > 1) System.err.println("\tduring connectedness search, TLHC moved from "+start+" to "+tlhc);
				}
				Glyph glyph = new Glyph(set,width,false);
if (debugLevel > 1) System.err.print(glyph);
				matched = mapOfGlyphsToStrings.get(glyph);
				if (matched != null) {
if (debugLevel > 1) System.err.println("Recognized "+matched);
					if (recordLocationWhenRecognized) {
						mapOfRecognizedLocationsToGlyphs.put(tlhc,glyph);
					}
				}
				else {
					if (trainingMode) {
						// don't have java.io.Console in JRE 5 :( so do it manually
						System.out.print(glyph+"Please enter string match: ");
						matched = new BufferedReader(new InputStreamReader(System.in)).readLine();
					}
					if (matched != null && matched.length() > 0) {
						mapOfGlyphsToStrings.put(glyph,matched);
if (debugLevel > 1) System.err.println("Map "+matched+" = \n"+glyph);
						if (recordLocationWhenRecognized) {
							mapOfRecognizedLocationsToGlyphs.put(tlhc,glyph);
						}
					}
					else {
						matched = null; // empty string becomes null for return flag
if (debugLevel > 1) System.err.println("Adding unrecognized glyph at location "+tlhc+", width = "+glyph.getWidth()+", BRHC = ("+(tlhc.getX()+glyph.getWidth()-1)+","+(tlhc.getY()+glyph.getHeight()-1)+")\n"+glyph);
						mapOfUnrecognizedLocationsToGlyphs.put(tlhc,glyph);
					}
				}
			}
		}
		return matched;
	}
	
	private boolean findConnectedCandidatesAnywhereInImage(int horizontalGapTolerance,int verticalGapTolerance) throws IOException, ConnectednessException {
		boolean foundAnything = false;
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				if (processCandidate(x,y,y,horizontalGapTolerance,verticalGapTolerance,true/*recordLocationWhenRecognized*/) != null) {
					foundAnything = true;
				}
			}
		}
		return foundAnything;
	}
	
	private boolean findConnectedCandidatesWithinUnrecognizedGlyphs(int horizontalGapTolerance,int verticalGapTolerance) throws IOException, ConnectednessException {
		boolean foundAnything = false;
		Location[] locations = mapOfUnrecognizedLocationsToGlyphs.keySet().toArray(new Location[0]);
		for (int i=0; i<locations.length; ++i) {
			Location l = locations[i];
			Glyph glyph = mapOfUnrecognizedLocationsToGlyphs.get(l);
			mapOfUnrecognizedLocationsToGlyphs.remove(l);	// since once we have started processing it a) we don't need it and b) we may add a new, smaller unrecognized glyph at same location
			String matched = "";
			int tlhcX = l.getX();
			int tlhcY = l.getY();
			int brhcX = tlhcX + glyph.getWidth() - 1;
			int brhcY = tlhcY + glyph.getHeight() - 1;
			int blockY = brhcY;
if (debugLevel > 1) System.err.println("findConnectedCandidatesWithinUnrecognizedGlyphs(): scan within box from TLHC ("+tlhcX+","+tlhcY+") to BRHC ("+brhcX+","+brhcY+")");
			// scan horizontally, then vertically, since want to treat new glyphs found as left to right unless two separate in same column
			for (int x = tlhcX; x <= brhcX; ++x) {
				for (int y = tlhcY; y <= brhcY; ++y) {
					// may theoretically stray outside boundary if connected, but should have been found when block (unrecognized glyph) detected last time
					String partialMatch = processCandidate(x,y,blockY,horizontalGapTolerance,verticalGapTolerance,false/*recordLocationWhenRecognized*/);
					if (partialMatch != null) {
						matched = matched + partialMatch;	// no spaces; treats everything in block as single "word"
					}
				}
			}
			if (matched.length() > 0) {
				foundAnything = true;
				mapOfGlyphsToStrings.put(glyph,matched);
				mapOfRecognizedLocationsToGlyphs.put(l,glyph);
			}
		}
		return foundAnything;
	}

	private void flagAsProcessedLocationsAlreadyRecognized() {
		Location[] locations = mapOfRecognizedLocationsToGlyphs.keySet().toArray(new Location[0]);
		for (int i=0; i<locations.length; ++i) {
			Location l = locations[i];
			Glyph glyph = mapOfRecognizedLocationsToGlyphs.get(l);
			int tlhcX = l.getX();
			int tlhcY = l.getY();
			int brhcX = tlhcX + glyph.getWidth() - 1;
			int brhcY = tlhcY + glyph.getHeight() - 1;
if (debugLevel > 1) System.err.println("flagAsProcessedLocationsAlreadyRecognized box from TLHC ("+tlhcX+","+tlhcY+") to BRHC ("+brhcX+","+brhcY+")");
			for (int y = tlhcY; y <= brhcY; ++y) {
				for (int x = tlhcX; x <= brhcX; ++x) {
					setProcessed(x,y);
				}
			}
		}
	}
	
	private String dumpGlyphsAsXML(boolean onlyNew,boolean queryEachNew) throws IOException {
		Iterator<Glyph>i = mapOfGlyphsToStrings.keySet().iterator();
		StringBuffer buf = new StringBuffer();
		buf.append("<glyphs>\n");
		while (i.hasNext()) {
			Glyph glyph = i.next();
			if (!onlyNew || !glyph.getWasKnown()) {
				boolean doIt = false;
				if (queryEachNew && !glyph.getWasKnown()) {
					// don't have java.io.Console in JRE 5 :( so do it manually
					System.out.print(glyph+"Record it in dictionary, Y or N [N]: ");
					String response = new BufferedReader(new InputStreamReader(System.in)).readLine();
					if (response != null && response.length() > 0 && response.trim().toUpperCase().equals("Y")) {
						doIt = true;
					}
				}
				else {
					doIt = true;
				}
				if (doIt) {
System.err.println("Recorded \""+glyph.getString()+"\"");
					//buf.append(glyph.toSourceCode());
					buf.append(glyph.toXML());
				}
			}
		}
		buf.append("</glyphs>\n");
		return buf.toString();
	}
	
	private String dumpStrings() {
		String[] values = mapOfGlyphsToStrings.values().toArray(new String[0]);
		Arrays.sort(values);
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<values.length; ++i) {
			buf.append(values[i]);
			buf.append("\n");
		}
		return buf.toString();
	}
	
	private String dumpLocations() {
		Location[] locations = mapOfRecognizedLocationsToGlyphs.keySet().toArray(new Location[0]);
		Arrays.sort(locations);
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<locations.length; ++i) {
			Location l = locations[i];
			buf.append(l);
			buf.append(": \"");
			buf.append(mapOfRecognizedLocationsToGlyphs.get(l).getString());
			buf.append("\"\n");
		}
		return buf.toString();
	}
	
	private String dumpLines(boolean showLocation) {
		Location[] locations = mapOfRecognizedLocationsToGlyphs.keySet().toArray(new Location[0]);
		Arrays.sort(locations);
		StringBuffer buf = new StringBuffer();
		int lastLine = -1;
		int lastX = 0;
		for (int i=0; i<locations.length; ++i) {
			Location l = locations[i];
			if (l.getY() != lastLine) {
				if (lastLine != -1) {
					if (showLocation) {
						buf.append("\"");
					}
					buf.append("\n");
				}
				lastLine=l.getY();
				if (showLocation) {
					buf.append(lastLine);
					buf.append(": \"");
				}
				lastX = 0;
			}
			if (l.getX() - lastX > 5) {		// character spacing factor to split strings
				buf.append("\t");
			}
			Glyph glyph = mapOfRecognizedLocationsToGlyphs.get(l);
			buf.append(glyph.getString());
			lastX = l.getX() + glyph.getWidth();
		}
		if (showLocation) {
			buf.append("\"");
		}
		buf.append("\n");
		return buf.toString();
	}
	
	private void initializeGlyphsFromFile(String filename) throws IOException, ParserConfigurationException, SAXException {
		InputStream in;
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			in = classLoader.getResourceAsStream("com/pixelmed/doseocr/"+filename);		// needs to be fully qualified, and needs to use "/" not "." as separator !
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			in = null;
		}
		if (in == null) {
if (debugLevel > 1) System.err.println("initializeGlyphsFromFile(): could not get from class loader as resource, loading from file system");
			in = new FileInputStream(filename);
		}
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		Node glyphsNode = document.getDocumentElement();
		if (glyphsNode.getNodeType() == Node.ELEMENT_NODE && glyphsNode.getNodeName().toLowerCase().equals("glyphs")) {
if (debugLevel > 1) System.err.print("initializeGlyphsFromFile(): got glyphs node");
			Node glyphNode = glyphsNode.getFirstChild();
			while (glyphNode != null) {
				if (glyphNode.getNodeType() == Node.ELEMENT_NODE && glyphNode.getNodeName().toLowerCase().equals("glyph")) {
if (debugLevel > 1) System.err.print("initializeGlyphsFromFile(): got glyph node");
					int width = 0;
					String string = "";
					BitSet set = new BitSet();
					Node glyphNodeChild = glyphNode.getFirstChild();
					while (glyphNodeChild != null) {
						if (glyphNodeChild.getNodeType() == Node.ELEMENT_NODE) {
							String glyphNodeChildName = glyphNodeChild.getNodeName().toLowerCase();
							if (glyphNodeChildName.equals("bits")) {
								Node bitNode = glyphNodeChild.getFirstChild();
								while (bitNode != null) {
									if (bitNode.getNodeType() == Node.ELEMENT_NODE && bitNode.getNodeName().toLowerCase().equals("bit")) {
										set.set(Integer.parseInt(bitNode.getTextContent().trim()));
									}
									bitNode = bitNode.getNextSibling();
								}
							}
							else if (glyphNodeChildName.equals("width")) {
								width = Integer.parseInt(glyphNodeChild.getTextContent().trim());
							}
							else if (glyphNodeChildName.equals("string")) {
								string = glyphNodeChild.getTextContent().trim();
							}
						}
						glyphNodeChild = glyphNodeChild.getNextSibling();
					}
					if (width > 0 && string.length() > 0 && !set.isEmpty()) {
						Glyph glyph = new Glyph(set,width,true);
						mapOfGlyphsToStrings.put(glyph,string);
if (debugLevel > 1) System.err.print("initializeGlyphsFromFile(): stored glyph\n"+glyph+"\n");
					}
				}
				glyphNode = glyphNode.getNextSibling();
			}
		}
	}
	
	public static BufferedImage getEightBitImageSuitableForThresholding(AttributeList list,int debugLevel) throws DicomException {
if (debugLevel > 1) System.err.println("OCR(): supplied WindowWidth "+Attribute.getSingleStringValueOrNull(list,TagFromName.WindowWidth));
if (debugLevel > 1) System.err.println("OCR(): supplied WindowCenter "+Attribute.getSingleStringValueOrNull(list,TagFromName.WindowCenter));
		// the correct window values are vital to thresholding
		// the GE values are usually a width of 1 and a center of -2, for a 16 bit signed image with a rescale intercept of -1024,
		// and pixel values of 0x0000 for black and 0x03ff (90123 dec) for white; other patterns are seen but the width of 1 is
		// usually a reliable signal that these have not been messed with
		//
		// if the window values are removed, the statistically derived values work fine, as long as the pixels have not been edited,
		// but if DicomCleaner or similar has been used to blackout identity, then maximum -ve pixel values like 0x8000 will
		// cause the statistically derived values to be broad and hence the thresholding to fail
								
if (debugLevel > 1) System.err.println("OCR(): supplied BitsStored "+Attribute.getSingleStringValueOrNull(list,TagFromName.BitsStored));
if (debugLevel > 1) System.err.println("OCR(): supplied PixelRepresentation "+Attribute.getSingleStringValueOrNull(list,TagFromName.PixelRepresentation));
if (debugLevel > 1) System.err.println("OCR(): supplied RescaleIntercept "+Attribute.getSingleStringValueOrNull(list,TagFromName.RescaleIntercept));
		
		if (Attribute.getSingleIntegerValueOrDefault(list,TagFromName.WindowWidth,0)  != 1) {
if (debugLevel > 1) System.err.println("OCR(): window width is not 1, removing window values and leaving to statistical default");
			list.remove(TagFromName.WindowWidth);	// these may have been inserted by downstream software (e.g., PACS), be incorrect and statistically derived values work better
			list.remove(TagFromName.WindowCenter);

			// have encountered screen saved series 10999 GE images that have a pixel padding value of -32768,
			// and window width != 1, which screws up the statistical windowing, so "hide" the padding values
			// when computing statistical windowing
			if (list.get(TagFromName.PixelPaddingValue) == null) {
if (debugLevel > 1) System.err.println("OCR(): no pixel padding value, so putting one in just in case");
				Attribute aPixelPaddingValue = null;
				if (Attribute.getSingleIntegerValueOrDefault(list,TagFromName.PixelRepresentation,0) == 0) {
					aPixelPaddingValue = new UnsignedShortAttribute(TagFromName.PixelPaddingValue);
				}
				else {
					aPixelPaddingValue = new SignedShortAttribute(TagFromName.PixelPaddingValue);
				}
				aPixelPaddingValue.addValue(-32768);
				list.put(aPixelPaddingValue);
			}
		}
		else {
if (debugLevel > 1) System.err.println("OCR(): window width is 1, so leaving window values alone");
		}
		return ConsumerFormatImageMaker.makeEightBitImage(list,debugLevel);	// handles all the signedness and photometric interpretation nastiness
	}
	
	public OCR(String inputFilename) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(inputFilename,defaultFileNameOfKnownGlyphs,null,0);
	}
	
	public OCR(String inputFilename,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
if (debugLevel > 0) System.err.println("OCR(): file "+inputFilename);
		BufferedImage image = null;
		if (DicomFileUtilities.isDicomOrAcrNemaFile(inputFilename)) {
			AttributeList list = new AttributeList();
			list.read(inputFilename);	
			doCommonConstructorStuff(list,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs,debugLevel);
		}
		else {
			list = null;
			image = ImageIO.read(new File(inputFilename));
			doCommonConstructorStuff(image,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs,debugLevel);
		}
	}
	
	public OCR(AttributeList list) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(list,defaultFileNameOfKnownGlyphs,null,0);
	}
	
	public OCR(AttributeList list,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		this(list,defaultFileNameOfKnownGlyphs,null,debugLevel);
	}
	
	public OCR(AttributeList list,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		doCommonConstructorStuff(list,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs,debugLevel);
	}
	
	public OCR(BufferedImage image,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		doCommonConstructorStuff(image,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs,debugLevel);
	}
	
	protected void doCommonConstructorStuff(AttributeList list,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		this.list = list;
		BufferedImage image = null;
		Overlay overlay = new Overlay(list);
		if (overlay.getNumberOfOverlays(0) > 0) {
if (debugLevel > 0) System.err.println("OCR(): using overlay rather than pixel data");
			image = overlay.getOverlayAsBinaryBufferedImage(0,0);
		}
		else {
			image = getEightBitImageSuitableForThresholding(list,debugLevel);
		}
		doCommonConstructorStuff(image,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs,debugLevel);
	}

	protected void doCommonConstructorStuff(BufferedImage image,String fileNameOfKnownGlyphs,String fileNameToRecordNewGlyphs,int debugLevel) throws IOException, ParserConfigurationException, SAXException, Exception {
		this.debugLevel = debugLevel;
		this.image = image;
		this.height = image.getHeight();
		this.width = image.getWidth();
		this.thresholdedPixels = threshold(image);
		processedPixels = new BitSet(height*width);
		mapOfGlyphsToStrings = new HashMap<Glyph,String>();
		mapOfRecognizedLocationsToGlyphs = new HashMap<Location,Glyph>();
		mapOfUnrecognizedLocationsToGlyphs = new HashMap<Location,Glyph>();
		if (fileNameOfKnownGlyphs != null && fileNameOfKnownGlyphs.length() > 0) {
			initializeGlyphsFromFile(fileNameOfKnownGlyphs);
		}
		trainingMode = fileNameToRecordNewGlyphs != null && fileNameToRecordNewGlyphs.length() > 0;
		
		// giwarden - Siemens screens have less vertical spacing
		if (isGEDoseScreenInstance(this.list)) {
			findConnectedCandidatesAnywhereInImage(defaultGEHorizontalGapTolerance,defaultGEVerticalGapTolerance);
		} else if (isSiemensDoseScreenInstance(this.list)) {
			findConnectedCandidatesAnywhereInImage(defaultSiemensHorizontalGapTolerance,defaultSiemensVerticalGapTolerance);
		}

		processedPixels.clear();
		//flagAsProcessedLocationsAlreadyRecognized();
		findConnectedCandidatesWithinUnrecognizedGlyphs(1,1);
if (debugLevel > 1) System.err.print(dumpStrings());
if (debugLevel > 1) System.err.print(dumpGlyphsAsXML(true/*onlyNew*/,false/*queryEach*/));

		if (trainingMode) {
			FileWriter out = new FileWriter(fileNameToRecordNewGlyphs);
			out.write(dumpGlyphsAsXML(false/*onlyNew*/,true/*queryEach*/));
			out.close();
		}
if (debugLevel > 1) System.err.print(dumpLocations());
if (debugLevel > 0) System.err.print(dumpLines(true));
	}

	public String toString() {
		return dumpLines(false/*showLocation*/);
	}
	
	protected static boolean isGEDoseScreenSeriesNumber(String seriesNumber) {
//System.err.println("OCR.isGEDoseScreenSeriesNumber(): checking "+seriesNumber);
		return seriesNumber.equals("999") || seriesNumber.equals("10999");
	}
	
	public static boolean isPossiblyGEDoseScreenSeries(String modality,String seriesNumber,String seriesDescription) {
		String useSeriesNumber = seriesNumber == null ? "" : seriesNumber.trim();
		return modality != null && modality.equals("CT") && isGEDoseScreenSeriesNumber(useSeriesNumber);
	}
	
	public static boolean isPossiblyGEDoseScreenSeries(AttributeList list) {
		return isPossiblyGEDoseScreenSeries(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality),Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber).trim(),null);
	}
	
	public static boolean isPossiblyGEDoseScreenInstance(String sopClassUID,String imageType) {
		return imageType != null && imageType.trim().startsWith("DERIVED\\SECONDARY\\SCREEN SAVE");
	}
	
	public static boolean isPossiblyGEDoseScreenInstance(AttributeList list) {
		return isPossiblyGEDoseScreenInstance(null,Attribute.getDelimitedStringValuesOrDefault(list,TagFromName.ImageType,""));
	}
	
	public static boolean isGEDoseScreenInstance(AttributeList list) {
		return isPossiblyGEDoseScreenInstance(list) && isGEDoseScreenSeriesNumber(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber).trim());
	}

	// giwarden
	public static boolean isPossiblySiemensDoseScreenSeries(String modality,String seriesNumber,String seriesDescription) {
		return modality != null && modality.equals("CT") && seriesNumber != null && seriesNumber.equals("501");
	}
	
	public static boolean isPossiblySiemensDoseScreenSeries(AttributeList list) {
		return isPossiblySiemensDoseScreenSeries(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality),Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber),null);
	}
	
	// giwarden
	public static boolean isPossiblySiemensDoseScreenInstance(String sopClassUID,String imageType) {
		return imageType != null && imageType.trim().equals("DERIVED\\SECONDARY\\OTHER\\CT_SOM5 PROT");
	}
	
	public static boolean isPossiblySiemensDoseScreenInstance(AttributeList list) {
		return isPossiblySiemensDoseScreenInstance(null,Attribute.getDelimitedStringValuesOrDefault(list,TagFromName.ImageType,""));
	}
	
	// giwarden
	public static boolean isSiemensDoseScreenInstance(AttributeList list) {
		return isPossiblySiemensDoseScreenInstance(list) && Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber).trim().equals("501");
	}
	
	public static boolean isPossiblyDoseScreenSeries(String modality,String seriesNumber,String seriesDescription) {
		return isPossiblyGEDoseScreenSeries(modality,seriesNumber,seriesDescription) || isPossiblySiemensDoseScreenSeries(modality,seriesNumber,seriesDescription);
	}
	
	public static boolean isPossiblyDoseScreenSeries(AttributeList list) {
		return isPossiblyGEDoseScreenSeries(list) || isPossiblySiemensDoseScreenSeries(list);
	}
	
	public static boolean isPossiblyDoseScreenInstance(String sopClassUID,String imageType) {
		return isPossiblyGEDoseScreenInstance(sopClassUID,imageType) || isPossiblySiemensDoseScreenInstance(sopClassUID,imageType);
	}
	
	public static boolean isPossiblyDoseScreenInstance(AttributeList list) {
		return isPossiblyGEDoseScreenInstance(list) || isPossiblySiemensDoseScreenInstance(list);
	}
	
	public static boolean isDoseScreenInstance(AttributeList list) {
		return isGEDoseScreenInstance(list) || isSiemensDoseScreenInstance(list);
	}
	
	public static CTDose getCTDoseFromOCROfGEDoseScreen(OCR ocr,int debugLevel,String startDateTime,String endDateTime,CTIrradiationEventDataFromImages eventDataFromImages,boolean buildSR) throws IOException {
		AttributeList list = ocr.getAttributeList();
		if (startDateTime == null || startDateTime.trim().length() == 0 && list != null) {
			startDateTime = Attribute.getSingleStringValueOrNull(list,TagFromName.StudyDate);
			if (startDateTime != null && startDateTime.length() == 8) {
				startDateTime = startDateTime + Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyTime);
			}
		}
		String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
		String studyDescription = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDescription);
		CTDose ctDose = new CTDose(ScopeOfDoseAccummulation.STUDY,studyInstanceUID,startDateTime,endDateTime,studyDescription);
		{
			//2	HELICAL	S19.250-I658.250	17.95	1299.58	BODY	32
			Pattern pEvent = Pattern.compile("[ \t]*([0-9]+)[ \t]+(HELICAL|AXIAL|SMARTVIEW|CINE)[ \t]+([SI])([0-9]*[.]*[0-9]*)[-]([SI])([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+(.*)[ \t]*");
			//Total	Exam DLP:	1299.58
			Pattern pTotal = Pattern.compile("[ \t]*TOTAL[ \t]*EXAM[ \t]*DLP:[ \t]*([0-9]*[.]*[0-9]*)[ \t]*");
			BufferedReader r = new BufferedReader(new StringReader(ocr.toString()));
			String line = null;
			while ((line=r.readLine()) != null) {
				line=line.toUpperCase();
				if (line.contains("HELICAL") || line.contains("AXIAL") || line.contains("SMARTVIEW") || line.contains("CINE")) {
if (debugLevel > 0) System.err.println(line);
					Matcher m = pEvent.matcher(line);
					if (m.matches()) {
if (debugLevel > 0) System.err.println("matches");
						int groupCount = m.groupCount();
if (debugLevel > 0) System.err.println("groupCount = "+groupCount);
						if (groupCount >= 9) {
							String series = m.group(1);		// first group is not 0, which is the entire match
if (debugLevel > 0) System.err.println("series = "+series);
							String scanType = m.group(2);
if (debugLevel > 0) System.err.println("scanType = "+scanType);
							String rangeFromSI = m.group(3);
							String rangeFromLocation = m.group(4);
							String rangeToSI = m.group(5);
							String rangeToLocation = m.group(6);
if (debugLevel > 0) System.err.println("range from = "+rangeFromSI+" "+rangeFromLocation+" mm to "+rangeToSI+" "+rangeToLocation+" mm");
							String CTDIvol = m.group(7);
if (debugLevel > 0) System.err.println("CTDIvol = "+CTDIvol+" mGy");
							String DLP = m.group(8);
if (debugLevel > 0) System.err.println("DLP = "+DLP+" mGy-cm");
							String phantom = m.group(9).replaceAll("[ \t]+","").trim();
if (debugLevel > 0) System.err.println("phantom = "+phantom);
							CTScanType recognizedScanType = CTScanType.selectFromDescription(scanType);
							if (recognizedScanType != null && !recognizedScanType.equals(CTScanType.LOCALIZER)) {
								ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,true/*isSeries*/,series,recognizedScanType,new ScanRange(rangeFromSI,rangeFromLocation,rangeToSI,rangeToLocation),CTDIvol,DLP,CTPhantomType.selectFromDescription(phantom)));
							}
						}
					}
				}
				else if (line.contains("TOTAL")) {
if (debugLevel > 0) System.err.println(line);
					Matcher m = pTotal.matcher(line);
					if (m.matches()) {
if (debugLevel > 0) System.err.println("matches");
						int groupCount = m.groupCount();
if (debugLevel > 0) System.err.println("groupCount = "+groupCount);
						if (groupCount >= 1) {
							String totalDLP = m.group(1);
if (debugLevel > 0) System.err.println("Total DLP = "+totalDLP+" mGy-cm");
							ctDose.setDLPTotal(totalDLP);
						}
					}
				}
			}
		}
		if (eventDataFromImages != null) {
			for (int ai = 0; ai<ctDose.getNumberOfAcquisitions(); ++ai) {
				CTDoseAcquisition acq = ctDose.getAcquisition(ai);
				if (acq != null) {
					ScanRange scanRange = acq.getScanRange();
					// This will work as long as there are not more than one series with the same number and scan range :(
					String key = acq.getSeriesOrAcquisitionNumber()
						+"+"+scanRange.getStartDirection()+scanRange.getStartLocation()
						+"+"+scanRange.getEndDirection()+scanRange.getEndLocation()
						+"+"+acq.getScopeUID();
					CTAcquisitionParameters ap = eventDataFromImages.getAcquisitionParametersBySeriesNumberScanRangeAndStudyInstanceUID(key);
					if (ap != null) {
						ap.deriveScanningLengthFromDLPAndCTDIVol(acq.getDLP(),acq.getCTDIvol());
						acq.setAcquisitionParameters(ap);
					}
				}
			}
		}
		
		if (buildSR) {
			GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(list,ctDose,eventDataFromImages);
		}

		return ctDose;
	}
	
	// based on patterns intially supplied and tested by giwarden
	public static CTDose getCTDoseFromOCROfSiemensDoseScreen(OCR ocr,int debugLevel,String startDateTime,String endDateTime,CTIrradiationEventDataFromImages eventDataFromImages,boolean buildSR) throws IOException {
		AttributeList list = ocr.getAttributeList();
		if (startDateTime == null || startDateTime.trim().length() == 0 && list != null) {
			startDateTime = Attribute.getSingleStringValueOrNull(list,TagFromName.StudyDate);
			if (startDateTime != null && startDateTime.length() == 8) {
				startDateTime = startDateTime + Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyTime);
			}
		}
		String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
		String studyDescription = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDescription);
		CTDose ctDose = new CTDose(ScopeOfDoseAccummulation.STUDY,studyInstanceUID,startDateTime,endDateTime,studyDescription);
		{
			//Scan	KV	mAs	/	ref.	CTDIvol	DLP	TI	cSL
			// topograms only have 5 columns (description, scan#, kV, TI and cSL)
			// others have either 9 or sometimes 8 columns ("/ref" is absent)
			//Topogram	1	130	8.5	0.6
			//ThorRout.ine	2	130	110	/120	12.32	312	0.6	1.2
			//PreMon.itor.ing	4	130	20	2.08	2	0.6	5.0
			
			// sometimes the acquisition number is a hyphenated range
			//BaseSeq	2-15	110	220	33.51	141	1.5	3.0
			
			// sometimes the phantom is specified this way
			// ABD/PEL	2	120	203	/	240	13.76(a)	700.21	0.5	1.2
			// PhantomType(a)32cm(b)16cm

			// sometimes the phantom is specified this way instead, and the topogram has phantom and dose (surprisingly)
			// Topogram	1	120	35mA	0.13L	10	7.8	0.6
			// ThorAbd	4	140	12	/174	1.29L	50	0.5	0.6
			// ..L--32cmS--16cm

			Pattern pEventLocalizerWithDoseAndPhantomType	= Pattern.compile("(.*TOPOGRAM.*)[ \t]+([0-9A-Z-]+)[ \t]+([0-9]+)[ \t]+([0-9]+)[ \t]*MA[ \t]+([0-9]*[.]*[0-9]*)[(]*([ABLS])[)]*[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*).*");
			Pattern pEventWithRefExposureAndPhantomType		= Pattern.compile("(.*)[ \t]+([0-9A-Z-]+)[ \t]+([0-9]+)[ \t]+([0-9]+)[ \t]+/[ \t]*([0-9]+)[ \t]+([0-9]*[.]*[0-9]*)[(]*([ABLS])[)]*[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*).*");
			Pattern pEventWithRefExposure					= Pattern.compile("(.*)[ \t]+([0-9A-Z-]+)[ \t]+([0-9]+)[ \t]+([0-9]+)[ \t]+/[ \t]*([0-9]+)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*).*");
			Pattern pEventWithoutRefExposure				= Pattern.compile("(.*)[ \t]+([0-9A-Z-]+)[ \t]+([0-9]+)[ \t]+([0-9]+)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*)[ \t]+([0-9]*[.]*[0-9]*).*");
			
			//TotalmAs2341    TotalDLP1234      - Siemens examples
			//TotalmAs2123    TotalDLP313mGy*cm
			//TotalmAs3832	  TotalDLP192 mGy*cm
			//mAstotal3996	DLPtotal1169

			Pattern pTotal1 = Pattern.compile("[ \t]*TOTAL[ \t]*MAS[ \t]*([0-9]*[.]*[0-9]*)[ \t]+TOTAL[ \t]*DLP[ \t]*([0-9]*[.]*[0-9]*).*");	// sometimes the space is packed up, sometimes not; ignore units at the end (if present)
			Pattern pTotal2 = Pattern.compile("[ \t]*MAS[ \t]*TOTAL[ \t]*([0-9]*[.]*[0-9]*)[ \t]+DLP[ \t]*TOTAL[ \t]*([0-9]*[.]*[0-9]*).*");	// sometimes the words are swapped
			// and sometimes there is no Total DLP at all (just Total mAs on the Operator line
			
			BufferedReader r = new BufferedReader(new StringReader(ocr.toString()));
			String line = null;
			while ((line=r.readLine()) != null) {
				line=line.toUpperCase();
if (debugLevel > 0) System.err.println(line);
				if (line.contains("TOTALDLP")) {
					Matcher m = pTotal1.matcher(line);
					if (m.matches()) {
if (debugLevel > 0) System.err.println("matches pTotal1");
						int groupCount = m.groupCount();
						for (int i=1; i<=groupCount; i++) {
if (debugLevel > 0) System.err.println("m.group("+i+"):"+m.group(i));					
						}
if (debugLevel > 0) System.err.println("groupCount = "+groupCount);
						if (groupCount >= 1) {
							String totalmAs = m.group(1);
if (debugLevel > 0) System.err.println("Total mAs = "+totalmAs);
							String totalDLP = m.group(2);
							if (!totalDLP.contains(".")) {
								totalDLP += ".00"; // Siemens DLP is often an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in total check :( (DAC)
							}
if (debugLevel > 0) System.err.println("Total DLP = "+totalDLP+" mGy-cm");
							ctDose.setDLPTotal(totalDLP);
						}
					}
				}
				else if (line.contains("DLPTOTAL")) {
					Matcher m = pTotal2.matcher(line);
					if (m.matches()) {
if (debugLevel > 0) System.err.println("matches pTotal2");
						int groupCount = m.groupCount();
						for (int i=1; i<=groupCount; i++) {
if (debugLevel > 0) System.err.println("m.group("+i+"):"+m.group(i));					
						}
if (debugLevel > 0) System.err.println("groupCount = "+groupCount);
						if (groupCount >= 1) {
							String totalmAs = m.group(1);
if (debugLevel > 0) System.err.println("Total mAs = "+totalmAs);
							String totalDLP = m.group(2);
							if (!totalDLP.contains(".")) {
								totalDLP += ".00"; // Siemens DLP is often an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in total check :( (DAC)
							}
if (debugLevel > 0) System.err.println("Total DLP = "+totalDLP+" mGy-cm");
							ctDose.setDLPTotal(totalDLP);
						}
					}
				}
				else {
					Matcher mEventWithRefExposureAndPhantomType = pEventWithRefExposureAndPhantomType.matcher(line);
					if (mEventWithRefExposureAndPhantomType.matches()) {
if (debugLevel > 0) System.err.println("matches pEventWithRefExposureAndPhantomType");
						int groupCount = mEventWithRefExposureAndPhantomType.groupCount();
if (debugLevel > 0) System.err.println("groupCount = "+groupCount);
						for (int i=1; i<=groupCount; i++) {
if (debugLevel > 0) System.err.println("mEventWithRefExposureAndPhantomType.group("+i+"):"+mEventWithRefExposureAndPhantomType.group(i));					
						}
						if (groupCount >= 10) {
							// first group is not 0, which is the entire match
							String protocol = mEventWithRefExposureAndPhantomType.group(1);				// Does not match ProtocolName in image headers; seems to match first part of SeriesDescription up to first whitespace
if (debugLevel > 0) System.err.println("protocol = "+protocol);
							String acquisitionNumber = mEventWithRefExposureAndPhantomType.group(2).replaceAll("[A-Z]","");		// Is NOT series number, and sometimes contains a suffix to be removed
if (debugLevel > 0) System.err.println("acquisitionNumber = "+acquisitionNumber);
							String scanType = null;
if (debugLevel > 0) System.err.println("scanType = "+scanType);
							String KV = mEventWithRefExposureAndPhantomType.group(3);
if (debugLevel > 0) System.err.println("KV = "+KV);
							String mAs = mEventWithRefExposureAndPhantomType.group(4);
if (debugLevel > 0) System.err.println("mAs = "+mAs);
							String ref = mEventWithRefExposureAndPhantomType.group(5);
if (debugLevel > 0) System.err.println("ref = "+ref);
							String CTDIvol = mEventWithRefExposureAndPhantomType.group(6);
if (debugLevel > 0) System.err.println("CTDIvol = "+CTDIvol+" mGy");
							String phantom = mEventWithRefExposureAndPhantomType.group(7);
if (debugLevel > 0) System.err.println("phantom = "+phantom);
							CTPhantomType recognizedPhantomType = null;
							if (phantom.equals("A") || phantom.equals("L")) {
								recognizedPhantomType = CTPhantomType.BODY32;
							}
							else if (phantom.equals("B") || phantom.equals("S")) {
								recognizedPhantomType = CTPhantomType.HEAD16;
							}
							String DLP = mEventWithRefExposureAndPhantomType.group(8);
							if (!DLP.contains(".")) {
								DLP += ".00"; // Siemens DLP is an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in acquisition check :( (DAC)
							}
if (debugLevel > 0) System.err.println("DLP = "+DLP+" mGy-cm");
							String TI = mEventWithRefExposureAndPhantomType.group(9);
if (debugLevel > 0) System.err.println("TI = "+TI);
							String cSL = mEventWithRefExposureAndPhantomType.group(10);
if (debugLevel > 0) System.err.println("cSL = "+cSL);
							CTScanType recognizedScanType = CTScanType.selectFromDescription(scanType);
							ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,acquisitionNumber,recognizedScanType,null,CTDIvol,DLP,recognizedPhantomType));
						}
					}
					else {
						Matcher mEventWithRefExposure = pEventWithRefExposure.matcher(line);
						if (mEventWithRefExposure.matches()) {
if (debugLevel > 0) System.err.println("matches pEventWithRefExposure");
							int groupCount = mEventWithRefExposure.groupCount();
if (debugLevel > 0) System.err.println("groupCount = "+groupCount);
							for (int i=1; i<=groupCount; i++) {
if (debugLevel > 0) System.err.println("mEventWithRefExposure.group("+i+"):"+mEventWithRefExposure.group(i));					
							}
							if (groupCount >= 9) {
								// first group is not 0, which is the entire match
								String protocol = mEventWithRefExposure.group(1);				// Does not match ProtocolName in image headers; seems to match first part of SeriesDescription up to first whitespace
if (debugLevel > 0) System.err.println("protocol = "+protocol);
								String acquisitionNumber = mEventWithRefExposure.group(2).replaceAll("[A-Z]","");		// Is NOT series number, and sometimes contains a suffix to be removed
if (debugLevel > 0) System.err.println("acquisitionNumber = "+acquisitionNumber);
								String scanType = null;
if (debugLevel > 0) System.err.println("scanType = "+scanType);
								String KV = mEventWithRefExposure.group(3);
if (debugLevel > 0) System.err.println("KV = "+KV);
								String mAs = mEventWithRefExposure.group(4);
if (debugLevel > 0) System.err.println("mAs = "+mAs);
								String ref = mEventWithRefExposure.group(5);
if (debugLevel > 0) System.err.println("ref = "+ref);
								String CTDIvol = mEventWithRefExposure.group(6);
if (debugLevel > 0) System.err.println("CTDIvol = "+CTDIvol+" mGy");
								String DLP = mEventWithRefExposure.group(7);
								if (!DLP.contains(".")) {
									DLP += ".00"; // Siemens DLP is an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in acquisition check :( (DAC)
								}
if (debugLevel > 0) System.err.println("DLP = "+DLP+" mGy-cm");
								String TI = mEventWithRefExposure.group(8);
if (debugLevel > 0) System.err.println("TI = "+TI);
								String cSL = mEventWithRefExposure.group(9);
if (debugLevel > 0) System.err.println("cSL = "+cSL);
								String phantom = "Unknown";
								CTScanType recognizedScanType = CTScanType.selectFromDescription(scanType);
								ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,acquisitionNumber,recognizedScanType,null,CTDIvol,DLP,CTPhantomType.selectFromDescription(phantom)));
							}
						}
						else {
							Matcher mEventWithoutRefExposure = pEventWithoutRefExposure.matcher(line);
							if (mEventWithoutRefExposure.matches()) {
if (debugLevel > 0) System.err.println("matches pEventWithoutRefExposure");
								int groupCount = mEventWithoutRefExposure.groupCount();
if (debugLevel > 0) System.err.println("groupCount = "+groupCount);
								for (int i=1; i<=groupCount; i++) {
if (debugLevel > 0) System.err.println("mEventWithoutRefExposure.group("+i+"):"+mEventWithoutRefExposure.group(i));					
								}
								if (groupCount >= 8) {
									String protocol = mEventWithoutRefExposure.group(1);				// Does not match ProtocolName in image headers; seems to match first part of SeriesDescription up to first whitespace
if (debugLevel > 0) System.err.println("protocol = "+protocol);
									String acquisitionNumber = mEventWithoutRefExposure.group(2).replaceAll("[A-Z]","");		// Is NOT series number, and sometimes contains a suffix to be removed
if (debugLevel > 0) System.err.println("acquisitionNumber = "+acquisitionNumber);
									String scanType = null;
if (debugLevel > 0) System.err.println("scanType = "+scanType);
									String KV = mEventWithoutRefExposure.group(3);
if (debugLevel > 0) System.err.println("KV = "+KV);
									String mAs = mEventWithoutRefExposure.group(4);
if (debugLevel > 0) System.err.println("mAs = "+mAs);
									String CTDIvol = mEventWithoutRefExposure.group(5);
if (debugLevel > 0) System.err.println("CTDIvol = "+CTDIvol+" mGy");
									String DLP = mEventWithoutRefExposure.group(6);
									if (!DLP.contains(".")) {
										DLP += ".00"; // Siemens DLP is an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in acquisition check :( (DAC)
									}
if (debugLevel > 0) System.err.println("DLP = "+DLP+" mGy-cm");
									String TI = mEventWithoutRefExposure.group(7);
if (debugLevel > 0) System.err.println("TI = "+TI);
									String cSL = mEventWithoutRefExposure.group(8);
if (debugLevel > 0) System.err.println("cSL = "+cSL);
									String phantom = "Unknown";
									CTScanType recognizedScanType = CTScanType.selectFromDescription(scanType);
									ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,acquisitionNumber,recognizedScanType,null,CTDIvol,DLP,CTPhantomType.selectFromDescription(phantom)));
								}
							}
							else {
								Matcher mEventLocalizerWithDoseAndPhantomType = pEventLocalizerWithDoseAndPhantomType.matcher(line);
								if (mEventLocalizerWithDoseAndPhantomType.matches()) {
if (debugLevel > 0) System.err.println("matches mEventLocalizerWithDoseAndPhantomType");
									int groupCount = mEventLocalizerWithDoseAndPhantomType.groupCount();
if (debugLevel > 0) System.err.println("groupCount = "+groupCount);
									for (int i=1; i<=groupCount; i++) {
if (debugLevel > 0) System.err.println("mEventLocalizerWithDoseAndPhantomType.group("+i+"):"+mEventLocalizerWithDoseAndPhantomType.group(i));					
									}
									if (groupCount >= 9) {
										// first group is not 0, which is the entire match
										String protocol = mEventLocalizerWithDoseAndPhantomType.group(1);				// Does not match ProtocolName in image headers; seems to match first part of SeriesDescription up to first whitespace
if (debugLevel > 0) System.err.println("protocol = "+protocol);
										String acquisitionNumber = mEventLocalizerWithDoseAndPhantomType.group(2).replaceAll("[A-Z]","");		// Is NOT series number, and sometimes contains a suffix to be removed
if (debugLevel > 0) System.err.println("acquisitionNumber = "+acquisitionNumber);
										String scanType = null;
if (debugLevel > 0) System.err.println("scanType = "+scanType);
										String KV = mEventLocalizerWithDoseAndPhantomType.group(3);
if (debugLevel > 0) System.err.println("KV = "+KV);
										String mAs = mEventLocalizerWithDoseAndPhantomType.group(4);
if (debugLevel > 0) System.err.println("mAs = "+mAs);
										String CTDIvol = mEventLocalizerWithDoseAndPhantomType.group(5);
if (debugLevel > 0) System.err.println("CTDIvol = "+CTDIvol+" mGy");
										String phantom = mEventLocalizerWithDoseAndPhantomType.group(6);
if (debugLevel > 0) System.err.println("phantom = "+phantom);
										CTPhantomType recognizedPhantomType = null;
										if (phantom.equals("A") || phantom.equals("L")) {
											recognizedPhantomType = CTPhantomType.BODY32;
										}
										else if (phantom.equals("B") || phantom.equals("S")) {
											recognizedPhantomType = CTPhantomType.HEAD16;
										}
										String DLP = mEventLocalizerWithDoseAndPhantomType.group(7);
										if (!DLP.contains(".")) {
											DLP += ".00"; // Siemens DLP is an integer, adding 2 sig digits to be consistent with GE pattern --giwarden (need to get rid of dependency on this in acquisition check :( (DAC)
										}
if (debugLevel > 0) System.err.println("DLP = "+DLP+" mGy-cm");
										String TI = mEventLocalizerWithDoseAndPhantomType.group(8);
if (debugLevel > 0) System.err.println("TI = "+TI);
										String cSL = mEventLocalizerWithDoseAndPhantomType.group(9);
if (debugLevel > 0) System.err.println("cSL = "+cSL);
										CTScanType recognizedScanType = CTScanType.LOCALIZER;
										ctDose.addAcquisition(new CTDoseAcquisition(studyInstanceUID,false/*isSeries*/,acquisitionNumber,recognizedScanType,null,CTDIvol,DLP,recognizedPhantomType));
									}
								}
							}
						}
					}
				}
			}
		}
		if (eventDataFromImages != null) {
			for (int ai = 0; ai<ctDose.getNumberOfAcquisitions(); ++ai) {
				CTDoseAcquisition acq = ctDose.getAcquisition(ai);
				if (acq != null) {
					// No scan range for Siemens dose screens, and uses AcquisitionNUmber not SeriesNumber
					// This will work as long as there are not more than one acquisition with the same number :(
					// ScopeUID was set to StudyInstanceUID in CTDoseAcquisition() constructor 
					String key = acq.getSeriesOrAcquisitionNumber()
						+"+"+acq.getScopeUID();
					CTAcquisitionParameters ap = eventDataFromImages.getAcquisitionParametersByAcquisitionNumberAndStudyInstanceUID(key);
					if (ap != null) {
						ap.deriveScanningLengthFromDLPAndCTDIVol(acq.getDLP(),acq.getCTDIvol());
						acq.setAcquisitionParameters(ap);
					}
				}
			}
		}
		
		if (buildSR) {
			GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(list,ctDose,eventDataFromImages);
		}

		return ctDose;
	}
	
	public static CTDose getCTDoseFromOCROfDoseScreen(OCR ocr,int debugLevel,CTIrradiationEventDataFromImages eventDataFromImages,boolean buildSR) throws IOException {
		CTDose ctDose = null;
		AttributeList list = ocr.getAttributeList();
		String studyInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyInstanceUID);
		String startDateTime =null;
		String endDateTime = null;
		if (eventDataFromImages != null) {
			startDateTime = eventDataFromImages.getOverallEarliestAcquisitionDateTimeForStudy(studyInstanceUID);
			endDateTime = eventDataFromImages.getOverallLatestAcquisitionDateTimeForStudy(studyInstanceUID);
		}
		if (ocr.isGEDoseScreenInstance(list)) {
			ctDose = getCTDoseFromOCROfGEDoseScreen(ocr,debugLevel,startDateTime,endDateTime,eventDataFromImages,buildSR);
		} else if (ocr.isSiemensDoseScreenInstance(list)) {
			ctDose = getCTDoseFromOCROfSiemensDoseScreen(ocr,debugLevel,startDateTime,endDateTime,eventDataFromImages,buildSR);
		}
		return ctDose;
	}
	
	/**
	 * <p>Extract the CT dose information in a screen save image using optical character recognition, correlate it with any acquired CT slice images.</p>
	 *
	 * @param	arg		an array of 1 to 5 strings - the file name of the dose screen save image (or "-" if to search for dose screen amongst acquired images),
	 *					then optionally the path to a DICOMDIR or folder containing acquired CT slice images (or "-" if none and more arguments)
	 *					then optionally the name of Dose SR file to write  (or "-" if none and more arguments)
	 *					then optionally the file containing the text glyphs to use during recognition rather than the default (or "-" if none and more arguments),
	 *					then optionally the name of a file to write any newly trained glyphs to
	 *					then optionally the debug level
	 */
	public static final void main(String arg[]) {
		try {
			String screenFilename            = arg.length > 0  && !arg[0].equals("-") ? arg[0] : null;
			String acquiredImagesPath        = arg.length > 1  && !arg[1].equals("-") ? arg[1] : null;
			String srOutputFilename          = arg.length > 2 && !arg[2].equals("-") ? arg[2] : null;
			String fileNameOfKnownGlyphs     = arg.length > 3 && !arg[3].equals("-") ? arg[3] : defaultFileNameOfKnownGlyphs;
			String fileNameToRecordNewGlyphs = arg.length > 4 && !arg[4].equals("-") ? arg[4] : null;
			int    debugLevel                = arg.length > 5 ? Integer.parseInt(arg[5]) : -1;
		
			String startDateTime = null;
			String endDateTime = null;
			CTIrradiationEventDataFromImages eventDataFromImages = null;
			if (acquiredImagesPath != null) {
				eventDataFromImages = new CTIrradiationEventDataFromImages(acquiredImagesPath);
System.err.print(eventDataFromImages);
				if (screenFilename == null) {
					List<String> screenFilenames = eventDataFromImages.getDoseScreenOrStructuredReportFilenames(true/*includeScreen*/,false/*includeSR*/);
					if (screenFilenames.isEmpty()) {
						System.err.println("############ No dose screen files found");
					}
					else if (screenFilenames.size() == 1) {
						screenFilename = screenFilenames.get(0);
					}
					else {
						System.err.println("############ Found more than one dose screen ... doing nothing");
					}
				}
			}
			if (screenFilename != null) {
				OCR ocr = new OCR(screenFilename,fileNameOfKnownGlyphs,fileNameToRecordNewGlyphs,debugLevel);
if (debugLevel > 0) System.err.print(ocr);
				CTDose ctDose = getCTDoseFromOCROfDoseScreen(ocr,debugLevel,eventDataFromImages,srOutputFilename != null);
System.err.print(ctDose.toString(true,true));
				if (!ctDose.specifiedDLPTotalMatchesDLPTotalFromAcquisitions()) {
					System.err.println("############ specified DLP total ("+ctDose.getDLPTotal()+") does not match DLP total from acquisitions ("+ctDose.getDLPTotalFromAcquisitions()+")");
				}
			
				if (srOutputFilename != null) {
					ctDose.write(srOutputFilename);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}


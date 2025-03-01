/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

public class ScanRange {
	
	protected String startDirection;
	protected String startLocation;
	protected String endDirection;
	protected String endLocation;
	protected String absoluteRange;

	public ScanRange(String startDirection,String startLocation,String endDirection,String endLocation) {
		this.startDirection = startDirection;
		this.startLocation = startLocation;
		this.endDirection = endDirection;
		this.endLocation = endLocation;
		this.absoluteRange = null;
	}
	
	public String getStartDirection() { return startDirection; }
	public String getStartLocation() { return startLocation; }
	public String getEndDirection() { return endDirection; }
	public String getEndLocation() { return endLocation; }
	
	public String getAbsoluteRange() {
		if (absoluteRange == null) {
			double start = Double.parseDouble(startLocation);
			if (startDirection == "I") {
				start = -start;
			}
			double end = Double.parseDouble(endLocation);
			if (endDirection == "I") {
				end = -end;
			}
			double r = start - end;
			if (r < 0) {
				r = -r;
			}
			java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance());
			formatter.setMaximumFractionDigits(3);
			formatter.setMinimumFractionDigits(3);
			formatter.setDecimalSeparatorAlwaysShown(true);		// i.e., a period even if fraction is zero
			formatter.setGroupingUsed(false);					// i.e., no comma at thousands
			absoluteRange = formatter.format(r);
//System.err.println("ScanRange.getDLPTotalFromAcquisitions(): returns formatted string "+absoluteRange+" for "+Double.toString(r));
		}
		return absoluteRange;
	}
	
	public String toString() {
		return startDirection + startLocation + "-" + endDirection + endLocation;
	}
	
	public boolean equals(Object o) {
		//System.err.println("Location.equals(): comparing "+this+" to "+o);
		boolean isEqual = false;
		if (o instanceof ScanRange) {
			ScanRange osr = (ScanRange)o;
			isEqual = osr.getStartDirection().equals(this.getStartDirection())
				   && osr.getStartLocation().equals(this.getStartLocation())
				   && osr.getEndDirection().equals(this.getEndDirection())
				   && osr.getEndLocation().equals(this.getEndLocation());
		}
		else {
			isEqual = false;
		}
		return isEqual;
	}
	
	public int hashCode() {
		return getStartDirection().hashCode()
			 + getStartLocation().hashCode()
			 + getEndDirection().hashCode()
			 + getEndLocation().hashCode();	// sufficient to implement equals() contract
	}
}

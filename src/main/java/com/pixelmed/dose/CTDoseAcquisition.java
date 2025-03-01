/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.*;

public class CTDoseAcquisition {
	
	protected String scopeUID;						// assume Acquisition and Series Number uniqueness only within this scope
	protected boolean seriesOrAcquisitionNumberIsSeries;
	protected String seriesOrAcquisitionNumber;
	protected CTScanType scanType;
	protected ScanRange scanRange;
	protected String CTDIvol;
	protected String DLP;
	protected CTPhantomType phantomType;
	
	protected CTAcquisitionParameters acquisitionParameters;
	
	protected ContentItem contentItemFragment;
	
	public CTDoseAcquisition(String scopeUID,boolean seriesOrAcquisitionNumberIsSeries,String seriesOrAcquisitionNumber,CTScanType scanType,ScanRange scanRange,String CTDIvol,String DLP,CTPhantomType phantomType) {
		this.scopeUID = scopeUID;
		this.seriesOrAcquisitionNumberIsSeries = seriesOrAcquisitionNumberIsSeries;
		this.seriesOrAcquisitionNumber = seriesOrAcquisitionNumber;
		this.scanType = scanType;
		this.scanRange = scanRange;
		this.CTDIvol = CTDIvol;
		this.DLP = DLP;
		this.phantomType = phantomType;
		this.acquisitionParameters = null;
		this.contentItemFragment = null;
	}
	
	public CTDoseAcquisition(String scopeUID,ContentItem parent) {
		this.scopeUID = scopeUID;
		ContentItem ctat = parent.getNamedChild("DCM","113820");	// "CT Acquisition Type"
		if (ctat != null && ctat instanceof ContentItemFactory.CodeContentItem) {
			scanType = CTScanType.selectFromCode(((ContentItemFactory.CodeContentItem)ctat).getConceptCode());
		}
		
		ContentItem ctap = parent.getNamedChild("DCM","113822");	// "CT Acquisition Parameters"
		if (ctap != null) {
			acquisitionParameters = new CTAcquisitionParameters(ctap);
		}
		
		ContentItem dose = parent.getNamedChild("DCM","113829");	// "CT Dose"
		if (dose != null) {
			ContentItem meanCTDIvol =  dose.getNamedChild("DCM","113830");	// "Mean CTDIvol"
			if (meanCTDIvol != null && meanCTDIvol instanceof ContentItemFactory.NumericContentItem) {
				CodedSequenceItem unit = ((ContentItemFactory.NumericContentItem)meanCTDIvol).getUnits();
				if (checkUnitIs_mGy(unit)) {
					CTDIvol = ((ContentItemFactory.NumericContentItem)meanCTDIvol).getNumericValue();
				}
				else {
					System.err.println("CT Dose Acquisition Mean CTDIvol units are not mGy - ignoring value");		// do not throw exception, since want to parse rest of content
				}
			}
			else {
				System.err.println("CT Dose Acquisition Mean CTDIvol not found");		// do not throw exception, since want to parse rest of content
			}

			ContentItem doseLengthProduct =  dose.getNamedChild("DCM","113838");	// "DLP"
			if (doseLengthProduct != null && doseLengthProduct instanceof ContentItemFactory.NumericContentItem) {
				CodedSequenceItem unit = ((ContentItemFactory.NumericContentItem)doseLengthProduct).getUnits();
				if (checkUnitIs_mGycm(unit)) {
					DLP = ((ContentItemFactory.NumericContentItem)doseLengthProduct).getNumericValue();
				}
				else {
					System.err.println("CT Dose Acquisition DLP units are not mGy.cm - ignoring value");		// do not throw exception, since want to parse rest of content
				}
			}
			else {
				System.err.println("CT Dose Acquisition DLP not found");		// do not throw exception, since want to parse rest of content
			}

			ContentItem pt = dose.getNamedChild("DCM","113835");					// "CTDIw Phantom Type"
			if (pt != null && pt instanceof ContentItemFactory.CodeContentItem) {
				phantomType = CTPhantomType.selectFromCode(((ContentItemFactory.CodeContentItem)pt).getConceptCode());
			}
		}
	}
	
	public static boolean checkUnitIs_mGy(CodedSequenceItem unit) {
		boolean correct = false;
		if (unit != null) {
			String cv = unit.getCodeValue();
			String csd = unit.getCodingSchemeDesignator();
			if (cv != null && cv.equals("mGy") && csd != null && csd.equals("UCUM")) {
				correct = true;
			}
		}
		return correct;
	}
	
	public static boolean checkUnitIs_mGycm(CodedSequenceItem unit) {
		boolean correct = false;
		if (unit != null) {
			String cv = unit.getCodeValue();
			String csd = unit.getCodingSchemeDesignator();
			if (cv != null && (cv.equals("mGy.cm") || cv.equals("mGycm")) && csd != null && csd.equals("UCUM")) {	// allow (incorrect UCUM but published) pre CP 1114 form
				correct = true;
			}
		}
		return correct;
	}
	
	public String getScopeUID() { return scopeUID; }
	public boolean isSeriesNumberNotAcquisitionNumber() { return seriesOrAcquisitionNumberIsSeries; }
	public String getSeriesOrAcquisitionNumber() { return seriesOrAcquisitionNumber; }
	public CTScanType getScanType() { return scanType; }
	public ScanRange getScanRange() { return scanRange; }
	public String getCTDIvol() { return CTDIvol; }
	public String getDLP() { return DLP; }
	public CTPhantomType getPhantomType () { return phantomType; }
	
	public void setAcquisitionParameters(CTAcquisitionParameters acquisitionParameters) {
		this.acquisitionParameters = acquisitionParameters;
		if (scanType.equals(CTScanType.UNKNOWN) && acquisitionParameters != null) {
			CTScanType apScanType = acquisitionParameters.getScanType();
			if (apScanType != null) {
//System.err.println("CTDoseAcquisition.setAcquisitionParameters(): overriding unknown scan type with value from CTAcquisitionParameters, which is "+apScanType);
				scanType = apScanType;
			}
		}
	}
	
	public CTAcquisitionParameters getAcquisitionParameters() { return acquisitionParameters; }

	public boolean equals(Object o) {
//System.err.println("CTDoseAcquisition.equals(): comparing "+this+" to "+o);
		boolean isEqual = false;
		if (o instanceof CTDoseAcquisition) {
			CTDoseAcquisition oda = (CTDoseAcquisition)o;
			isEqual =
			     oda.isSeriesNumberNotAcquisitionNumber() == this.isSeriesNumberNotAcquisitionNumber()
			&& ((oda.getSeriesOrAcquisitionNumber() == null && this.getSeriesOrAcquisitionNumber() == null) || (oda.getSeriesOrAcquisitionNumber().equals(this.getSeriesOrAcquisitionNumber())))
			&& ((oda.getScopeUID() == null && this.getScopeUID() == null) || (oda.getScopeUID().equals(this.getScopeUID())))
			&& ((oda.getScanType() == null && this.getScanType() == null) || (oda.getScanType().equals(this.getScanType())))
			&& ((oda.getScanRange() == null && this.getScanRange() == null) || (oda.getScanRange().equals(this.getScanRange())))
			&& ((oda.getCTDIvol() == null && this.getCTDIvol() == null) || (oda.getCTDIvol().equals(this.getCTDIvol())))
			&& ((oda.getDLP() == null && this.getDLP() == null) || (oda.getDLP().equals(this.getDLP())))
			&& ((oda.getPhantomType() == null && this.getPhantomType() == null) || (oda.getPhantomType().equals(this.getPhantomType())));
			// do NOT check acquisitionParameters !
		}
		else {
			isEqual = false;
		}
		return isEqual;
	}
	
	public int hashCode() {
		return getSeriesOrAcquisitionNumber().hashCode()
		+ getScopeUID().hashCode()
		+ getScanType().hashCode()
		+ getScanRange().hashCode();	// sufficient to implement equals() contract
	}

	public String getDLPFromRangeAndCTDIvol() {	// NB. Will NOT match specified DLP, due to overscan (helical) and slice thickness/gap (axial)
		String formatted = null;
		if (scanRange != null && CTDIvol != null && CTDIvol.length() > 0) {
			try {
				double dlpFromRangeAndCTDIvol = Double.parseDouble(scanRange.getAbsoluteRange()) * Double.parseDouble(CTDIvol) / 10;		// in cm not mm
				java.text.DecimalFormat formatter = (java.text.DecimalFormat)(java.text.NumberFormat.getInstance());
				formatter.setMaximumFractionDigits(2);
				formatter.setMinimumFractionDigits(2);
				formatter.setDecimalSeparatorAlwaysShown(true);		// i.e., a period even if fraction is zero
				formatter.setGroupingUsed(false);					// i.e., no comma at thousands
				formatted = formatter.format(dlpFromRangeAndCTDIvol);
//System.err.println("CTDoseAcquisition.getDLPFromRangeAndCTDIvol(): returns formatted string "+formatted+" for "+Double.toString(dlpFromRangeAndCTDIvol));	
			}
			catch (NumberFormatException e) {
				e.printStackTrace(System.err);
			}
		}
		return formatted;
	}
	
	public boolean specifiedDLPMatchesDLPFromRangeAndCTDIvol() {
		String computedDLP = getDLPFromRangeAndCTDIvol();
//System.err.println("CTDoseAcquisition.specifiedDLPMatchesDLPFromRangeAndCTDIvol(): comparing specified DLP "+DLP+" with computed "+computedDLP);
		return DLP != null && computedDLP != null && DLP.equals(computedDLP);
	}
	
	public String toString() {
		return toString(false);
	}
	
	public String toString(boolean pretty) {
		StringBuffer buffer = new StringBuffer();

		// do not re-enable printing of scope when not pretty unless tests are also modified to expect this
		//if (!pretty) {
		//	buffer.append("Scope UID=");
		//	buffer.append(scopeUID);
		//}

		buffer.append("\t");
		buffer.append(pretty && (seriesOrAcquisitionNumber == null || seriesOrAcquisitionNumber.trim().length() == 0) ? "-" : ((seriesOrAcquisitionNumberIsSeries ? "Series" : "Acq") + "=" + seriesOrAcquisitionNumber));
		
		buffer.append("\t");
		buffer.append(scanType);
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("Range=");
		}
		buffer.append(pretty && scanRange == null ? "-" : (scanRange + " mm"));
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("CTDIvol=");
		}
		buffer.append(pretty && (CTDIvol == null || CTDIvol.trim().length() == 0) ? "-" : (CTDIvol + " mGy"));
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("DLP=");
		}
		buffer.append(pretty && (DLP == null || DLP.trim().length() == 0) ? "-" : (DLP + " mGy.cm"));
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("Phantom=");
		}
		buffer.append(pretty && phantomType == null ? "-" : phantomType);

		if (acquisitionParameters != null) {
			buffer.append("\n\t");
			buffer.append(acquisitionParameters.toString(pretty));
		}
		else {
			buffer.append("\n");
		}
		return buffer.toString();
	}
	
	public String getHTMLTableHeaderRow() {
		return	 "<tr>"
				+"<th>Number</th>"
				+"<th>Type</th>"
				+"<th>Range mm</th>"
				+"<th>CTDIvol mGy</th>"
				+"<th>DLP mGy.cm</th>"
				+"<th>Phantom</th>"
				+(acquisitionParameters == null ? "" : CTAcquisitionParameters.getHTMLTableHeaderRowFragment())
				+"</tr>\n";
	}

	public String getHTMLTableRow() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<tr>");

		buffer.append("<td>");
		if (seriesOrAcquisitionNumber != null && seriesOrAcquisitionNumber.trim().length() > 0) {
			buffer.append(seriesOrAcquisitionNumberIsSeries ? "Series" : "Acq");
			buffer.append("=");
			buffer.append(seriesOrAcquisitionNumber);
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (scanType != null) {
			buffer.append(scanType);
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (scanRange != null) {
			buffer.append(scanRange);
			//buffer.append(" mm");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (CTDIvol != null && CTDIvol.trim().length() > 0) {
			buffer.append(CTDIvol);
			//buffer.append(" mGy");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (DLP != null && DLP.trim().length() > 0) {
			buffer.append(DLP);
			//buffer.append(" mGy.cm");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (phantomType != null) {
			buffer.append(phantomType);
		}
		buffer.append("</td>");

		if (acquisitionParameters != null) {
			buffer.append(acquisitionParameters.getHTMLTableRowFragment());
		}
		
		buffer.append("</tr>\n");

		return buffer.toString();
	}

	public ContentItem getStructuredReportFragment(ContentItem root) throws DicomException {
		if (contentItemFragment == null) {
			ContentItemFactory cif = new ContentItemFactory();
			contentItemFragment = cif.new ContainerContentItem(root,"CONTAINS",new CodedSequenceItem("113819","DCM","CT Acquisition"),true/*continuityOfContentIsSeparate*/,"DCMR","10013");
//System.err.println("CTDoseAcquisition.getStructuredReportFragment(): acquisitionParameters=\n"+acquisitionParameters);
			{
				CodedSequenceItem targetRegion = acquisitionParameters == null ? null : acquisitionParameters.getAnatomy();
				cif.new CodeContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("123014","DCM","Target Region"),(targetRegion == null ? new CodedSequenceItem("T-D0010","SRT","Entire body") : targetRegion));
			}
			{
				String irradiationEventUID = acquisitionParameters == null ? null : acquisitionParameters.getIrradiationEventUID();
				cif.new UIDContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113769","DCM","Irradiation Event UID"),(irradiationEventUID == null ? new UIDGenerator().getNewUID() : irradiationEventUID));
			}
			{
				// scanType is never null, but it may be unknown
				CodedSequenceItem ctat = scanType.getCodedSequenceItem();
				if (ctat != null) {
					cif.new CodeContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113820","DCM","CT Acquisition Type"),ctat);
				}
			}
			if (acquisitionParameters != null) {
				acquisitionParameters.getStructuredReportFragment(contentItemFragment);
			}
			{
				// regardless of whether we have anything to put in it, add a CT Acquisition Parameters container if there were no acquisitionParameters set
				ContentItem ctap = contentItemFragment.getNamedChild("DCM","113822");
				if (ctap == null) {
//System.err.println("CTDoseAcquisition.getStructuredReportFragment(): making CT Acquisition Parameters content item because no acquisitionParameters");
					ctap = cif.new ContainerContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113822","DCM","CT Acquisition Parameters"),true/*continuityOfContentIsSeparate*/);
				}
				// use the scanning range derived from the dose screen if there is nothing already there from the acquisitionParameters
				if (scanRange != null) {
//System.err.println("CTDoseAcquisition.getStructuredReportFragment(): have scanRange");
					String scanningLengthInMM = ctap.getSingleStringValueOrNullOfNamedChild("DCM","113825");	// "Scanning Length"
					if (scanningLengthInMM == null || scanningLengthInMM.length() == 0) {
//System.err.println("CTDoseAcquisition.getStructuredReportFragment(): using scanRange");
						cif.new NumericContentItem(ctap,"CONTAINS",new CodedSequenceItem("113825","DCM","Scanning Length"),scanRange.getAbsoluteRange(),new CodedSequenceItem("mm","UCUM","1.8","mm"));
					}
				}
			}
			if ((CTDIvol != null && CTDIvol.trim().length() > 0) || phantomType != null || (DLP != null && DLP.trim().length() > 0)) {	// per CP 1075, can send this for constant angle aquisitions ... send container if we have some content
				ContentItem dose = cif.new ContainerContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113829","DCM","CT Dose"),true/*continuityOfContentIsSeparate*/);
				if (CTDIvol != null && CTDIvol.trim().length() > 0) {
					cif.new NumericContentItem(dose,"CONTAINS",new CodedSequenceItem("113830","DCM","Mean CTDIvol"),CTDIvol,new CodedSequenceItem("mGy","UCUM","1.8","mGy"));
				}
				if (phantomType != null) {
					cif.new CodeContentItem   (dose,"CONTAINS",new CodedSequenceItem("113835","DCM","CTDIw Phantom Type"),phantomType.getCodedSequenceItem());
				}
				if (DLP != null && DLP.trim().length() > 0) {
					cif.new NumericContentItem(dose,"CONTAINS",new CodedSequenceItem("113838","DCM","DLP"),DLP,new CodedSequenceItem("mGy.cm","UCUM","1.8","mGy.cm"));
				}
			}
		}
		return contentItemFragment;
	}
}

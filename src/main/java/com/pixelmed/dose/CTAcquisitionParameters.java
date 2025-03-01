/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.*;
import com.pixelmed.utils.FloatFormatter;

public class CTAcquisitionParameters {
	
	protected String irradiationEventUID;
	protected CTScanType scanType;
	protected CodedSequenceItem anatomy;
	protected String exposureTimeInSeconds;
	protected String scanningLengthInMM;
	protected String nominalSingleCollimationWidthInMM;
	protected String nominalTotalCollimationWidthInMM;
	protected String pitchFactor;
	protected String kvp;
	protected String tubeCurrent;
	protected String tubeCurrentMaximum;
	protected String exposureTimePerRotation;
	
	protected ContentItem contentItemFragment;

	public boolean equals(Object o) {
//System.err.println("CTAcquisitionParameters.equals(): comparing "+this+" to "+o);
		boolean isEqual = false;
		if (o instanceof CTAcquisitionParameters) {
			CTAcquisitionParameters oap = (CTAcquisitionParameters)o;
			isEqual =
			   ((oap.getIrradiationEventUID() == null && this.getIrradiationEventUID() == null) || (oap.getIrradiationEventUID().equals(this.getIrradiationEventUID())))
			&& ((oap.getScanType() == null && this.getScanType() == null) || (oap.getScanType().equals(this.getScanType())))
			&& ((oap.getAnatomy() == null && this.getAnatomy() == null) || (oap.getAnatomy().equals(this.getAnatomy())))
			&& ((oap.getExposureTimeInSeconds() == null && this.getExposureTimeInSeconds() == null) || (oap.getExposureTimeInSeconds().equals(this.getExposureTimeInSeconds())))
			&& ((oap.getScanningLengthInMM() == null && this.getScanningLengthInMM() == null) || (oap.getScanningLengthInMM().equals(this.getScanningLengthInMM())))
			&& ((oap.getNominalSingleCollimationWidthInMM() == null && this.getNominalSingleCollimationWidthInMM() == null) || (oap.getNominalSingleCollimationWidthInMM().equals(this.getNominalSingleCollimationWidthInMM())))
			&& ((oap.getNominalTotalCollimationWidthInMM() == null && this.getNominalTotalCollimationWidthInMM() == null) || (oap.getNominalTotalCollimationWidthInMM().equals(this.getNominalTotalCollimationWidthInMM())))
			&& ((oap.getPitchFactor() == null && this.getPitchFactor() == null) || (oap.getPitchFactor().equals(this.getPitchFactor())))
			&& ((oap.getKVP() == null && this.getKVP() == null) || (oap.getKVP().equals(this.getKVP())))
			&& ((oap.getTubeCurrent() == null && this.getTubeCurrent() == null) || (oap.getTubeCurrent().equals(this.getTubeCurrent())))
			&& ((oap.getTubeCurrentMaximum() == null && this.getTubeCurrentMaximum() == null) || (oap.getTubeCurrentMaximum().equals(this.getTubeCurrentMaximum())))
			&& ((oap.getExposureTimePerRotation() == null && this.getExposureTimePerRotation() == null) || (oap.getExposureTimePerRotation().equals(this.getExposureTimePerRotation())))
			;
		}
		else {
			isEqual = false;
		}
		return isEqual;
	}

	public boolean equalsApartFromIrradiationEventUID(Object o) {
//System.err.println("CTAcquisitionParameters.equalsApartFromIrradiationEventUID(): comparing "+this+" to "+o);
		boolean isEqual = false;
		if (o instanceof CTAcquisitionParameters) {
			CTAcquisitionParameters oap = (CTAcquisitionParameters)o;
			isEqual =
			   ((oap.getScanType() == null && this.getScanType() == null) || (oap.getScanType().equals(this.getScanType())))
			&& ((oap.getAnatomy() == null && this.getAnatomy() == null) || (oap.getAnatomy().equals(this.getAnatomy())))
			&& ((oap.getExposureTimeInSeconds() == null && this.getExposureTimeInSeconds() == null) || (oap.getExposureTimeInSeconds().equals(this.getExposureTimeInSeconds())))
			&& ((oap.getScanningLengthInMM() == null && this.getScanningLengthInMM() == null) || (oap.getScanningLengthInMM().equals(this.getScanningLengthInMM())))
			&& ((oap.getNominalSingleCollimationWidthInMM() == null && this.getNominalSingleCollimationWidthInMM() == null) || (oap.getNominalSingleCollimationWidthInMM().equals(this.getNominalSingleCollimationWidthInMM())))
			&& ((oap.getNominalTotalCollimationWidthInMM() == null && this.getNominalTotalCollimationWidthInMM() == null) || (oap.getNominalTotalCollimationWidthInMM().equals(this.getNominalTotalCollimationWidthInMM())))
			&& ((oap.getPitchFactor() == null && this.getPitchFactor() == null) || (oap.getPitchFactor().equals(this.getPitchFactor())))
			&& ((oap.getKVP() == null && this.getKVP() == null) || (oap.getKVP().equals(this.getKVP())))
			&& ((oap.getTubeCurrent() == null && this.getTubeCurrent() == null) || (oap.getTubeCurrent().equals(this.getTubeCurrent())))
			&& ((oap.getTubeCurrentMaximum() == null && this.getTubeCurrentMaximum() == null) || (oap.getTubeCurrentMaximum().equals(this.getTubeCurrentMaximum())))
			&& ((oap.getExposureTimePerRotation() == null && this.getExposureTimePerRotation() == null) || (oap.getExposureTimePerRotation().equals(this.getExposureTimePerRotation())))
			;
		}
		else {
			isEqual = false;
		}
		return isEqual;
	}

	public CTAcquisitionParameters(String irradiationEventUID,CTScanType scanType,CodedSequenceItem anatomy,String exposureTimeInSeconds,String scanningLengthInMM,
				String nominalSingleCollimationWidthInMM,String nominalTotalCollimationWidthInMM,String pitchFactor,
				String kvp,String tubeCurrent,String tubeCurrentMaximum,String exposureTimePerRotation) {
		this.irradiationEventUID = irradiationEventUID;
		this.scanType = scanType;
		this.anatomy = anatomy;
		this.exposureTimeInSeconds = exposureTimeInSeconds;
		this.scanningLengthInMM = scanningLengthInMM;
		this.nominalSingleCollimationWidthInMM = nominalSingleCollimationWidthInMM;
		this.nominalTotalCollimationWidthInMM = nominalTotalCollimationWidthInMM;
		this.pitchFactor = pitchFactor;
		this.kvp = kvp;
		this.tubeCurrent = tubeCurrent;
		this.tubeCurrentMaximum = tubeCurrentMaximum;
		this.exposureTimePerRotation = exposureTimePerRotation;
	}
	
	public CTAcquisitionParameters(ContentItem parametersNode) {
		if (parametersNode != null) {
			// extract information from siblings ...
			ContentItem parent = (ContentItem)(parametersNode.getParent());
			if (parent != null) {
				irradiationEventUID = parent.getSingleStringValueOrNullOfNamedChild("DCM","113769");	// "Irradiation Event UID"

				ContentItem ctat = parent.getNamedChild("DCM","113820");	// "CT Acquisition Type"
				if (ctat != null && ctat instanceof ContentItemFactory.CodeContentItem) {
					scanType = CTScanType.selectFromCode(((ContentItemFactory.CodeContentItem)ctat).getConceptCode());
				}
				
				ContentItem targetRegion = parent.getNamedChild("DCM","123014");		// "Target Region"
				if (targetRegion != null && targetRegion instanceof ContentItemFactory.CodeContentItem) {
					anatomy = ((ContentItemFactory.CodeContentItem)targetRegion).getConceptCode();
				}
			}
		
			// extract information from children ...
			exposureTimeInSeconds             = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113824");	// "Exposure Time"	... should really check units are seconds :(
			scanningLengthInMM                = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113825");	// "Scanning Length"	... should really check units are mm :(
			nominalSingleCollimationWidthInMM = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113826");	// "Nominal Single Collimation Width"	... should really check units are mm :(
			nominalTotalCollimationWidthInMM  = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113827");	// "Nominal Total Collimation Width"	... should really check units are mm :(
			pitchFactor                       = parametersNode.getSingleStringValueOrNullOfNamedChild("DCM","113828");	// "Pitch Factor"	... should really check units are ratio :(

			// extract x-ray source informtion, assuming only one
			ContentItem source = parametersNode.getNamedChild("DCM","113831");		// "CT X-Ray Source Parameters"
			if (source != null) {
				kvp                     = source.getSingleStringValueOrNullOfNamedChild("DCM","113733");	// "KVP"	... should really check units are kV :(
				tubeCurrent             = source.getSingleStringValueOrNullOfNamedChild("DCM","113734");	// "X-Ray Tube Current"	... should really check units are mA :(
				tubeCurrentMaximum		= source.getSingleStringValueOrNullOfNamedChild("DCM","113833");	// "Maximum X-Ray Tube Current"	... should really check units are mA :(
				exposureTimePerRotation = source.getSingleStringValueOrNullOfNamedChild("DCM","113834");	// "Exposure Time per Rotation"	... should really check units are seconds :(
			}
		}
	}
	
	public String getIrradiationEventUID() { return irradiationEventUID; }
	
	public CTScanType getScanType() { return scanType; }
	
	public CodedSequenceItem getAnatomy() { return anatomy; }
	
	public String getExposureTimeInSeconds() { return exposureTimeInSeconds; }
	
	public String getScanningLengthInMM() { return scanningLengthInMM; }
	
	public String getNominalSingleCollimationWidthInMM() { return nominalSingleCollimationWidthInMM; }
	
	public String getNominalTotalCollimationWidthInMM() { return nominalTotalCollimationWidthInMM; }
	
	public String getPitchFactor() { return pitchFactor; }
	
	public String getKVP() { return kvp; }
	
	public String getTubeCurrent() { return tubeCurrent; }
	
	public String getTubeCurrentMaximum() { return tubeCurrentMaximum; }
	
	public String getExposureTimePerRotation() { return exposureTimePerRotation; }
	
	public void deriveScanningLengthFromDLPAndCTDIVol(String dlp,String ctdiVol) {
		if (dlp != null && dlp.length() > 0 && ctdiVol != null && ctdiVol.length() > 0) {
			try {
				double dDLP = new Double(dlp).doubleValue();
				double dCTDIVol = new Double(ctdiVol).doubleValue();
				if (dDLP > 0 && dCTDIVol > 0) {	// don't want division by zero to produce NaN, and no point in producing length if no dose information ...
					scanningLengthInMM = FloatFormatter.toString(dDLP/dCTDIVol*10);	// DLP is in mGy.cm not mm
				}
			}
			catch (NumberFormatException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	public String toString() {
		return toString(false);
	}

	public String toString(boolean pretty) {
		StringBuffer buffer = new StringBuffer();
		if (!pretty) {
			buffer.append("\tIrradiationEventUID=");
			buffer.append(irradiationEventUID);
		}
		
		buffer.append("\t");
		buffer.append(scanType);

		buffer.append("\t");
		if (!pretty) {
			buffer.append("Anatomy=");
			buffer.append(anatomy);
		}
		else if (anatomy != null) {
			buffer.append(anatomy.getCodeMeaning());
		}
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("ScanningLength=");
		}
		if (!pretty || (scanningLengthInMM != null && scanningLengthInMM.trim().length() > 0)) {
			buffer.append(scanningLengthInMM);
			buffer.append(" mm");
		}
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("Collimation single/total=");
		}
		if (!pretty || (nominalSingleCollimationWidthInMM != null && nominalSingleCollimationWidthInMM.length() > 0) || (nominalTotalCollimationWidthInMM != null && nominalTotalCollimationWidthInMM.length() > 0)) {
			buffer.append(nominalSingleCollimationWidthInMM == null ? "" : nominalSingleCollimationWidthInMM);
			buffer.append("/");
			buffer.append(nominalTotalCollimationWidthInMM == null ? "" : nominalTotalCollimationWidthInMM);
			buffer.append(" mm");
		}
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("PitchFactor=");
		}
		if (!pretty || (pitchFactor != null && pitchFactor.trim().length() > 0)) {
			buffer.append(pitchFactor);
			buffer.append(":1");
		}
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("KVP=");
		}
		if (!pretty || (kvp != null && kvp.trim().length() > 0)) {
			buffer.append(kvp);
			buffer.append(" kVP");
		}
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("TubeCurrent/Max=");
		}
		if (!pretty || (tubeCurrent != null && tubeCurrent.trim().length() > 0) || (tubeCurrentMaximum != null && tubeCurrentMaximum.trim().length() > 0)) {
			buffer.append(tubeCurrent);
			buffer.append("/");
			buffer.append(tubeCurrentMaximum);
			buffer.append(" mA");
		}
		
		buffer.append("\t");
		if (!pretty) {
			buffer.append("Exposure time/per rotation=");
		}
		if (!pretty || (exposureTimeInSeconds != null && exposureTimeInSeconds.trim().length() > 0) || (exposureTimePerRotation != null && exposureTimePerRotation.trim().length() > 0)) {
			buffer.append(exposureTimeInSeconds == null ? "" : exposureTimeInSeconds);
			buffer.append("/");
			buffer.append(exposureTimePerRotation == null ? "" : exposureTimePerRotation);
			buffer.append(" s");
		}

		buffer.append("\n");

		return buffer.toString();
	}
	
	public static String getHTMLTableHeaderRowFragment() {
		return	 "<th>Type</th>"
				+"<th>Anatomy</th>"
				+"<th>Scanning Length mm</th>"
				+"<th>Collimation Single/Total mm</th>"
				+"<th>Pitch Factor</th>"
				+"<th>kVP</th>"
				+"<th>Tube Current Mean/Max mA</th>"
				+"<th>Exposure Time/Per Rotation s</th>";
	}
	
	public String getHTMLTableRowFragment() {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("<td>");
		if (scanType != null) {
			buffer.append(scanType);
		}
		buffer.append("</td>");

		buffer.append("<td>");
		if (anatomy != null) {
			buffer.append(anatomy.getCodeMeaning());
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (scanningLengthInMM != null && scanningLengthInMM.trim().length() > 0) {
			buffer.append(scanningLengthInMM);
			//buffer.append(" mm");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if ((nominalSingleCollimationWidthInMM != null && nominalSingleCollimationWidthInMM.length() > 0) || (nominalTotalCollimationWidthInMM != null && nominalTotalCollimationWidthInMM.length() > 0)) {
			buffer.append(nominalSingleCollimationWidthInMM == null ? "" : nominalSingleCollimationWidthInMM);
			buffer.append("/");
			buffer.append(nominalTotalCollimationWidthInMM == null ? "" : nominalTotalCollimationWidthInMM);
			//buffer.append(" mm");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (pitchFactor != null && pitchFactor.trim().length() > 0) {
			buffer.append(pitchFactor);
			buffer.append(":1");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if (kvp != null && kvp.trim().length() > 0) {
			buffer.append(kvp);
			//buffer.append(" kVP");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if ((tubeCurrent != null && tubeCurrent.trim().length() > 0) || (tubeCurrentMaximum != null && tubeCurrentMaximum.trim().length() > 0)) {
			buffer.append(tubeCurrent);
			buffer.append("/");
			buffer.append(tubeCurrentMaximum);
			//buffer.append(" mA");
		}
		buffer.append("</td>");
		
		buffer.append("<td>");
		if ((exposureTimeInSeconds != null && exposureTimeInSeconds.trim().length() > 0) || (exposureTimePerRotation != null && exposureTimePerRotation.trim().length() > 0)) {
			buffer.append(exposureTimeInSeconds == null ? "" : exposureTimeInSeconds);
			buffer.append("/");
			buffer.append(exposureTimePerRotation == null ? "" : exposureTimePerRotation);
			//buffer.append(" s");
		}
		buffer.append("</td>");

		return buffer.toString();
	}
	
	public ContentItem getStructuredReportFragment(ContentItem root) throws DicomException {
		if (contentItemFragment == null) {
			ContentItemFactory cif = new ContentItemFactory();
			contentItemFragment = cif.new ContainerContentItem(root,"CONTAINS",new CodedSequenceItem("113822","DCM","CT Acquisition Parameters"),true/*continuityOfContentIsSeparate*/);
			if (exposureTimeInSeconds != null && exposureTimeInSeconds.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113824","DCM","Exposure Time"),exposureTimeInSeconds,new CodedSequenceItem("s","UCUM","1.8","s"));
			}
			if (scanningLengthInMM != null && scanningLengthInMM.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113825","DCM","Scanning Length"),scanningLengthInMM,new CodedSequenceItem("mm","UCUM","1.8","mm"));
			}
			if (nominalSingleCollimationWidthInMM != null && nominalSingleCollimationWidthInMM.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113826","DCM","Nominal Single Collimation Width"),nominalSingleCollimationWidthInMM,new CodedSequenceItem("mm","UCUM","1.8","mm"));
			}
			if (nominalTotalCollimationWidthInMM != null && nominalTotalCollimationWidthInMM.trim().length() > 0) {
				cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113827","DCM","Nominal Total Collimation Width"),nominalTotalCollimationWidthInMM,new CodedSequenceItem("mm","UCUM","1.8","mm"));
			}
			if (pitchFactor != null && pitchFactor.trim().length() > 0) {
				if (scanType == null || scanType.equals(CTScanType.AXIAL) || scanType.equals(CTScanType.HELICAL) || scanType.equals(CTScanType.UNKNOWN)) {	// i.e., not if known to be stationary
					cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113828","DCM","Pitch Factor"),pitchFactor,new CodedSequenceItem("{ratio}","UCUM","1.8","ratio"));
				}
			}
			cif.new NumericContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113823","DCM","Number of X-Ray Sources"),"1",new CodedSequenceItem("{X-Ray sources}","UCUM","1.8","X-Ray sources"));
			ContentItem source = cif.new ContainerContentItem(contentItemFragment,"CONTAINS",new CodedSequenceItem("113831","DCM","CT X-Ray Source Parameters"),true/*continuityOfContentIsSeparate*/);
			cif.new TextContentItem(source,"CONTAINS",new CodedSequenceItem("113832","DCM","Identification of the X-Ray Source"),"1");
			if (kvp != null && kvp.trim().length() > 0) {
				cif.new NumericContentItem(source,"CONTAINS",new CodedSequenceItem("113733","DCM","KVP"),kvp,new CodedSequenceItem("kV","UCUM","1.8","kV"));
			}
			if (tubeCurrentMaximum != null && tubeCurrentMaximum.trim().length() > 0) {
				cif.new NumericContentItem(source,"CONTAINS",new CodedSequenceItem("113833","DCM","Maximum X-Ray Tube Current"),tubeCurrentMaximum,new CodedSequenceItem("mA","UCUM","1.8","mA"));
			}
			if (tubeCurrent != null && tubeCurrent.trim().length() > 0) {
				cif.new NumericContentItem(source,"CONTAINS",new CodedSequenceItem("113734","DCM","X-Ray Tube Current"),tubeCurrent,new CodedSequenceItem("mA","UCUM","1.8","mA"));
			}
			if (exposureTimePerRotation != null && exposureTimePerRotation.trim().length() > 0) {
				cif.new NumericContentItem(source,"CONTAINS",new CodedSequenceItem("113834","DCM","Exposure Time per Rotation"),exposureTimePerRotation,new CodedSequenceItem("s","UCUM","1.8","s"));
			}
			//>>>	CONTAINS	NUM	EV (113821, DCM, "X-ray Filter Aluminum Equivalent")	1	U		Units = EV (mm, UCUM, "mm")
		
		}
		return contentItemFragment;
	}
}


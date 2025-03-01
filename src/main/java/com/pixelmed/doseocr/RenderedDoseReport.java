package com.pixelmed.doseocr;

import com.pixelmed.dicom.*;
import com.pixelmed.dose.*;

import java.util.ArrayList;
import java.util.Vector;

public class RenderedDoseReport {

	/**
	 * <p>Extract dose information from a screen or report, correlate it with any acquired CT slice images as required, and generate a human-readable report.</p>
	 *
	 * @param	paths		a Vector of String paths to a DICOMDIR or folder or list of files containing dose screens, reports and acquired CT slices
	 * @param	summary		if true generate a summary only, otherwise tabulate the acquisition and technique data
	 */
	public static String generateDoseReportInformationFromFiles(Vector paths,boolean summary) {
		return generateDoseReportInformationFromFiles(paths,summary,null);
	}

	/**
	 * <p>Extract dose information from a screen or report, correlate it with any acquired CT slice images as required, and generate a human-readable report.</p>
	 *
	 * @param	paths		a Vector of String paths to a DICOMDIR or folder or list of files containing dose screens, reports and acquired CT slices
	 * @param	summary		if true generate a summary only, otherwise tabulate the acquisition and technique data
	 * @param	contentType	the type of text content to be generated, e.g., "text/html"; will be plain text if null or unrecognized
	 */
	public static String generateDoseReportInformationFromFiles(Vector paths,boolean summary,String contentType) {
		String report = "";
		boolean doHTML = false;
		if (contentType != null) {
			String useContentType = contentType.trim().toLowerCase();
			if (useContentType.equals("text/html")) {
				doHTML = true;
			}
			// anything else, e.g., "text/plain" is just default
		}
		
		CTIrradiationEventDataFromImages eventDataFromImages = new CTIrradiationEventDataFromImages(paths);
//System.err.print(eventDataFromImages);

		ArrayList<String> screenFilenames = eventDataFromImages.getDoseScreenOrStructuredReportFilenames();
		for (String screenFilename : screenFilenames) {
//System.err.println("RenderedDoseReport.generateDoseReportInformation(): "+screenFilename);
			try {
				AttributeList list = new AttributeList();
				list.read(screenFilename);
//System.err.print(list);
				CTDose ctDose = null;
				if (SOPClass.isStructuredReport(Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPClassUID))) {
//System.err.println("RenderedDoseReport.generateDoseReportInformation(): isStructuredReport");
					ctDose = new CTDose(list);
//System.err.print(ctDose.toString(true,true));
				}
				else if (OCR.isDoseScreenInstance(list)) {
//System.err.println("RenderedDoseReport.generateDoseReportInformation(): isDoseScreenInstance");
					OCR ocr = new OCR(list,0/*debugLevel*/);
//System.err.print(ocr);
					ctDose = ocr.getCTDoseFromOCROfDoseScreen(ocr,0/*debugLevel*/,eventDataFromImages,true);
				}
				else if (ExposureDoseSequence.isPhilipsDoseScreenInstance(list)) {
					ctDose = ExposureDoseSequence.getCTDoseFromPhilipsDoseScreen(list,0/*debugLevel*/,eventDataFromImages,true);
				}
				if (ctDose != null) {
//System.err.println("RenderedDoseReport.generateDoseReportInformation(): have ctDose");
					if (list != null) {
						CompositeInstanceContext cic = new CompositeInstanceContext(list);
						cic.updateFromSource(eventDataFromImages);	// in case patient characteristics are missing from source list but present in other instances
						ctDose.setCompositeInstanceContext(cic);
					}
//System.err.print(ctDose.toString(true,true));
					if (doHTML) {
						report += ctDose.getHTMLTableRow(!summary);
					}
					else {
						// plain text
						report += ctDose.toString(!summary,true/*pretty*/);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		return report;
	}

	
	/**
	 * <p>Extract dose information from a screen or report, correlate it with any acquired CT slice images as required, and generate a human-readable report.</p>
	 *
	 * @param	arg		one or more paths to a DICOMDIR or folder or dose screens, reports and acquired CT slices
	 */
	public static final void main(String arg[]) {
		try {
			Vector paths = new Vector();
			for (int i=0; i<arg.length; ++i) {
				paths.add(arg[i]);
			}
			String report = generateDoseReportInformationFromFiles(paths,false);
			System.err.println(report);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}


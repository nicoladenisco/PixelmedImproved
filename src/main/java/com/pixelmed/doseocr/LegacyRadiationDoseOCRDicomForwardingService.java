/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.doseocr;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.dose.CTDose;

import com.pixelmed.doseocr.ExposureDoseSequence;
import com.pixelmed.doseocr.OCR;

import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;
import com.pixelmed.network.StorageSOPClassSCU;

//import com.pixelmed.utils.FileUtilities;

import java.io.BufferedInputStream;
//import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
//import java.io.InputStreamReader;
import java.io.IOException;




/**
 * <p>A class to wait for incoming dose screen images, perform OCR to create Radiation Dose SRs and send RDSRs to a pre-configured DICOM destination.</p>
 *
 * <p>The class has no public methods other than the constructor and a main method that is useful as a utility.</p>
 *
 * @author	dclunie
 */
public class LegacyRadiationDoseOCRDicomForwardingService {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/doseocr/LegacyRadiationDoseOCRDicomForwardingService.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";
	
	protected int debugLevel;
	
	protected String theirHost;
	protected int theirPort;
	protected String theirAETitle;
	
	protected String ourAETitle;
	
	protected class ReceivedFileProcessor implements Runnable {
		String receivedFileName;
		AttributeList list;
		File ctDoseSRFile;
		String ctDoseSRFileName;
		
		ReceivedFileProcessor(String receivedFileName) {
			this.receivedFileName = receivedFileName;
		}
		
		public void run() {
			try {
if (debugLevel > 1) System.err.println("LegacyRadiationDoseOCRDicomForwardingService.ReceivedFileProcessor.run(): receivedFileName = "+receivedFileName);
				FileInputStream fis = new FileInputStream(receivedFileName);
				DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
				AttributeList list = new AttributeList();
				list.read(i);
				i.close();
				fis.close();
				
				// copied from DoseUtility ... should refactor :(
				{
					CTDose ctDose = null;
					if (OCR.isDoseScreenInstance(list)) {
if (debugLevel > 0) System.err.println("LegacyRadiationDoseOCRDicomForwardingService.ReceivedFileProcessor.run(): isDoseScreenInstance");
						OCR ocr = new OCR(list,0/*debugLevel*/);
//System.err.print(ocr);
						ctDose = ocr.getCTDoseFromOCROfDoseScreen(ocr,debugLevel,null/*eventDataFromImages*/,true/*buildSR*/);
					}
					else if (ExposureDoseSequence.isPhilipsDoseScreenInstance(list)) {
if (debugLevel > 0) System.err.println("LegacyRadiationDoseOCRDicomForwardingService.ReceivedFileProcessor.run(): isPhilipsDoseScreenInstance");
						ctDose = ExposureDoseSequence.getCTDoseFromPhilipsDoseScreen(list,debugLevel,null/*eventDataFromImages*/,true/*buildSR*/);
					}
					
					if (ctDose != null) {
						AttributeList ctDoseList = ctDose.getAttributeList();
						ctDoseSRFile = File.createTempFile("ocrrdsr",".dcm");
						ctDoseSRFileName = ctDoseSRFile.getCanonicalPath();
if (debugLevel > 0) System.err.println("LegacyRadiationDoseOCRDicomForwardingService.ReceivedFileProcessor.run(): adding our own newly created SR file = "+ctDoseSRFileName);
						ctDose.write(ctDoseSRFileName,ourAETitle);	// has side effect of updating list returned by ctDose.getAttributeList(); uncool :(
						new StorageSOPClassSCU(theirHost,theirPort,theirAETitle,ourAETitle,ctDoseSRFileName,
							Attribute.getSingleStringValueOrNull(ctDoseList,TagFromName.SOPClassUID),
							Attribute.getSingleStringValueOrNull(ctDoseList,TagFromName.SOPInstanceUID),
							0/*compressionLevel*/,
							debugLevel);
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
			if (ctDoseSRFile != null) {
				try {
					if (!ctDoseSRFile.delete()) {
						throw new DicomException("Failed to delete RDSR file that we created "+ctDoseSRFileName);
					}
				}
				catch  (Exception e) {
					e.printStackTrace(System.err);
				}
			}
			if (receivedFileName != null) {
				try {
					if (!new File(receivedFileName).delete()) {
						throw new DicomException("Failed to delete received file that we have successfully extracted from "+receivedFileName);
					}
				}
				catch  (Exception e) {
					e.printStackTrace(System.err);
				}
			}
		}
	}
	
	/**
	 *
	 */
	protected class OurReceivedObjectHandler extends ReceivedObjectHandler {
		/**
		 * @param	dicomFileName
		 * @param	transferSyntax
		 * @param	callingAETitle
		 * @exception	IOException
		 * @exception	DicomException
		 * @exception	DicomNetworkException
		 */
		public void sendReceivedObjectIndication(String dicomFileName,String transferSyntax,String callingAETitle)
				throws DicomNetworkException, DicomException, IOException {
			if (dicomFileName != null) {
if (debugLevel > 0) System.err.println("Received: "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax);
				try {
					new Thread(new ReceivedFileProcessor(dicomFileName)).start();		// on separate thread, else will block and the C-STORE response will be delayed
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}

		}
	}
	
	/**
	 * <p>Wait for incoming dose screen images, perform OCR to create Radiation Dose SRs and send RDSRs to specified DICOM destination.</p>
	 *
	 * @param	ourPort
	 * @param	ourAETitle
	 * @param	theirHost
	 * @param	theirPort
	 * @param	theirAETitle
	 * @param	savedImagesFolder
	 * @param	debugLevel
	 */
	public LegacyRadiationDoseOCRDicomForwardingService(int ourPort,String ourAETitle,String theirHost,int theirPort,String theirAETitle,File savedImagesFolder,int debugLevel) throws IOException {
		this.ourAETitle   = ourAETitle;
		this.theirHost    = theirHost;
		this.theirPort    = theirPort;
		this.theirAETitle = theirAETitle;
		this.debugLevel   = debugLevel;
		// Start up DICOM association listener in background for receiving images  ...
if (debugLevel > 1) System.err.println("Starting up DICOM association listener ...");
		new Thread(new StorageSOPClassSCPDispatcher(ourPort,ourAETitle,savedImagesFolder,new OurReceivedObjectHandler(),debugLevel)).start();
	}

	/**
	 * <p>Wait for incoming dose screen images, perform OCR to create Radiation Dose SRs and send RDSRs to specified DICOM destination.</p>
	 *
	 * @param	arg	array of six strings - our port, our AE Title, their hostname, their port, their AE Title,
	 *			and the debugging level
	 */
	public static void main(String arg[]) {
		try {
			int ourPort;
			String ourAETitle;
			String theirHost;
			int theirPort;
			String theirAETitle;
			int debugLevel;
			if (arg.length == 6) {
				        ourPort=Integer.parseInt(arg[0]);
				    ourAETitle=arg[1];
				     theirHost=arg[2];
				     theirPort=Integer.parseInt(arg[3]);
				  theirAETitle=arg[4];
				    debugLevel=Integer.parseInt(arg[5]);
			}
			else {
				throw new Exception("Argument list must be 6 values");
			}
			File savedImagesFolder = new File(System.getProperty("java.io.tmpdir"));
			new LegacyRadiationDoseOCRDicomForwardingService(ourPort,ourAETitle,theirHost,theirPort,theirAETitle,savedImagesFolder,debugLevel);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
	}
}


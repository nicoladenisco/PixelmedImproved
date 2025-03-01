/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.database.PatientStudySeriesConcatenationInstanceModel;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.network.DicomNetworkException;
import com.pixelmed.network.ReceivedObjectHandler;
import com.pixelmed.network.StorageSOPClassSCPDispatcher;

/**
 * <p>The {@link com.pixelmed.web.RequestTypeServer RequestTypeServer} implements an HTTP server that responds
 * to requests of a specified type and dispatches the further
 * handling to a derived class corresponding to the request type.</p>
 *
 * <p>Requests are of the form "?requestType=XXXX" where XXXX is the request type.</p>
 *
 * <p>This includes responding to WADO GET requests
 * as defined by DICOM PS 3.18 (ISO 17432), which provides a standard web (http) interface through which to retrieve DICOM objects either
 * as DICOM files or as derived JPEG images.</p>
 *
 * <p>In addition to servicing WADO requests, it provides lists of patients, studies and series that link to WADO URLs.</p>
 *
 * <p>It extends extends {@link com.pixelmed.web.HttpServer HttpServer} and implements
 * {@link Worker#generateResponseToGetRequest(String,OutputStream) generateResponseToGetRequest()}.</p>
 *
 * <p>The main method is also useful in its own right as a command-line DICOM Storage
 * SCP utility, which will store incoming files in a specified directory and database
 * and server them up via WADO.</p>
 *
 * <p>For example:</p>
 * <pre>
% java -server -Djava.awt.headless=true -Xms128m -Xmx512m -cp ./pixelmed.jar:./hsqldb.jar:./excalibur-bzip2-1.0.jar:./vecmath1.2-1.14.jar:./commons-codec-1.3.jar com.pixelmed.web.RequestTypeServer ./testwadodb ./testwadoimages 4007 WADOTEST 7091 "192.168.1.100" IMAGEDISPLAY 
 * </pre>
 *
 * @see com.pixelmed.web.WadoServer
 *
 * @author	dclunie
 */
public class RequestTypeServer extends HttpServer {
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/web/RequestTypeServer.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	private static final String imageDisplayTemplateFileName = "ImageDisplayTemplate.tpl";
	private static final String appletDisplayTemplateFileName = "AppletDisplayTemplate.tpl";

	private String rootURL;
	private String stylesheetPath;
	private String requestTypeToUseForInstances;
	private DatabaseInformationModel databaseInformationModel;
	
	protected class RequestTypeWorker extends Worker {
		private PathRequestHandler pathRequestHandler = null;
		private WadoRequestHandler wadoRequestHandler = null;
		private PatientListRequestHandler patientListRequestHandler = null;
		private StudyListRequestHandler studyListRequestHandler = null;
		private SeriesListRequestHandler seriesListRequestHandler = null;
		private InstanceListRequestHandler instanceListRequestHandler = null;
		private ImageDisplayRequestHandler imageDisplayRequestHandler = null;
		private AppletDisplayRequestHandler appletDisplayRequestHandler = null;
		
		protected void generateResponseToGetRequest(String requestURI,OutputStream out) throws IOException {
if (webServerDebugLevel > 0) System.err.println("RequestTypeServer.RequestTypeWorker.generateResponseToGetRequest(): Requested URI: "+requestURI);
			try {
				WebRequest request = new WebRequest(requestURI);
				String requestType = request.getRequestType();
				if (requestType == null) {
					if (pathRequestHandler == null) {
						pathRequestHandler = new PathRequestHandler(stylesheetPath,webServerDebugLevel);
					}
					pathRequestHandler.generateResponseToGetRequest(null,null,null,request,null,out);
				}
				else if (requestType.equals("WADO")) {
					if (wadoRequestHandler == null) {
						wadoRequestHandler = new WadoRequestHandler(null,webServerDebugLevel);
					}
					wadoRequestHandler.generateResponseToGetRequest(databaseInformationModel,null,null,request,null,out);
				}
				else if (requestType.equals("PATIENTLIST")) {
					if (patientListRequestHandler == null) {
						patientListRequestHandler = new PatientListRequestHandler(stylesheetPath,webServerDebugLevel);
					}
					patientListRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,null,null,out);
				}
				else if (requestType.equals("STUDYLIST")) {
					if (studyListRequestHandler == null) {
						studyListRequestHandler = new StudyListRequestHandler(stylesheetPath,webServerDebugLevel);
					}
					studyListRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,request,null,out);
				}
				else if (requestType.equals("SERIESLIST")) {
					if (seriesListRequestHandler == null) {
						seriesListRequestHandler = new SeriesListRequestHandler(stylesheetPath,requestTypeToUseForInstances,webServerDebugLevel);
					}
					seriesListRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,request,null,out);
				}
				else if (requestType.equals("INSTANCELIST")) {
					if (instanceListRequestHandler == null) {
						instanceListRequestHandler = new InstanceListRequestHandler(stylesheetPath,webServerDebugLevel);
					}
					instanceListRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,request,null,out);
				}
				else if (requestType.equals("IMAGEDISPLAY")) {
					if (imageDisplayRequestHandler == null) {
						imageDisplayRequestHandler = new ImageDisplayRequestHandler(stylesheetPath,imageDisplayTemplateFileName,webServerDebugLevel);
					}
					imageDisplayRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,request,null,out);
				}
				else if (requestType.equals("APPLETDISPLAY")) {
					if (appletDisplayRequestHandler == null) {
						appletDisplayRequestHandler = new AppletDisplayRequestHandler(stylesheetPath,appletDisplayTemplateFileName,webServerDebugLevel);
					}
					appletDisplayRequestHandler.generateResponseToGetRequest(databaseInformationModel,rootURL,null,request,null,out);
				}
				else {
					throw new Exception("Unrecognized requestType \""+requestType+"\"");
				}
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
if (webServerDebugLevel > 0) System.err.println("RequestTypeServer.RequestTypeWorker.generateResponseToGetRequest(): Sending 404 Not Found");
				RequestHandler.send404NotFound(out,e.getMessage());
			}
		}
	}
	
	/***/
	protected Worker createWorker() {
		return new RequestTypeWorker();
	}
	
	/***/
	private class OurReceivedObjectHandler extends ReceivedObjectHandler {
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
if (webServerDebugLevel > 0) System.err.println("Received: "+dicomFileName+" from "+callingAETitle+" in "+transferSyntax);
				try {
					FileInputStream fis = new FileInputStream(dicomFileName);
					DicomInputStream i = new DicomInputStream(new BufferedInputStream(fis));
					AttributeList list = new AttributeList();
					list.read(i,TagFromName.PixelData);
					i.close();
					fis.close();
					databaseInformationModel.insertObject(list,dicomFileName,DatabaseInformationModel.FILE_COPIED);
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}

		}
	}
	
	public RequestTypeServer(String dataBaseFileName,String savedImagesFolderName,int dicomPort,String calledAETitle,int storageSCPDebugLevel,
			int wadoPort,int webServerDebugLevel,String rootURL,String stylesheetPath,String requestTypeToUseForInstances) {
		super(webServerDebugLevel);
		try {
			databaseInformationModel = new PatientStudySeriesConcatenationInstanceModel(dataBaseFileName);
			File savedImagesFolder = new File(savedImagesFolderName);
			if (!savedImagesFolder.exists()) {
				savedImagesFolder.mkdirs();
			}
			new Thread(new StorageSOPClassSCPDispatcher(dicomPort,calledAETitle,savedImagesFolder,new OurReceivedObjectHandler(),null,null,null,false,storageSCPDebugLevel)).start();
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
		doCommonConstructorStuff(databaseInformationModel,wadoPort,rootURL,stylesheetPath,requestTypeToUseForInstances);
	}
	
	public RequestTypeServer(DatabaseInformationModel databaseInformationModel,WebServerApplicationProperties webServerApplicationProperties) {
		super(webServerApplicationProperties.getWebServerDebugLevel());
		doCommonConstructorStuff(databaseInformationModel,
			webServerApplicationProperties.getListeningPort(),
			webServerApplicationProperties.getRootURL(),
			webServerApplicationProperties.getStylesheetPath(),
			webServerApplicationProperties.getRequestTypeToUseForInstances()
		);
	}
	
	public RequestTypeServer(DatabaseInformationModel databaseInformationModel,int wadoPort,int webServerDebugLevel,String rootURL,String stylesheetPath,String requestTypeToUseForInstances) {
		super(webServerDebugLevel);
		doCommonConstructorStuff(databaseInformationModel,wadoPort,rootURL,stylesheetPath,requestTypeToUseForInstances);
	}
	
	private void doCommonConstructorStuff(DatabaseInformationModel databaseInformationModel,int port,String rootURL,String stylesheetPath,String requestTypeToUseForInstances) {
		this.rootURL=rootURL;
if (webServerDebugLevel > 1) System.err.println("RequestTypeServer.doCommonConstructorStuff(): rootURL = "+rootURL);
		this.stylesheetPath=stylesheetPath;
if (webServerDebugLevel > 1) System.err.println("RequestTypeServer.doCommonConstructorStuff(): stylesheetPath = "+stylesheetPath);
		this.databaseInformationModel = databaseInformationModel;
		this.requestTypeToUseForInstances = requestTypeToUseForInstances;
		try {
if (webServerDebugLevel > 1) System.err.println("RequestTypeServer.doCommonConstructorStuff(): port = "+port);
			super.initializeThreadPool(port);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
if (webServerDebugLevel > 0) System.err.println("RequestTypeServer(): ready");
	}

	/**
	 * <p>Wait for http connections and process requests; also wait for DICOM associations and store received files in the database.</p>
	 *
	 * @param	arg	array of seven or nine strings - the database filename, the saved images folder, the DICOM port, the DICOM AET, the HTTP port, the host address to build the root URL,
	 *			the request type to use for instances (one of INSTANCELIST, IMAGEDISPLAY, or APPLETDISPLAY),
	 *			and optionally, the storage SCP and web server debug levels
	 */
	public static void main(String arg[]) {
		if (arg.length != 7 && arg.length != 9) {
			System.err.println("Usage: database imagefolder DICOMport DICOMAET HTTPport hostAddress requesttype [storageSCPDebugLevel [webServerDebugLevel]]");
			System.exit(0);
		}
		String stylesheetPath = "stylesheet.css";
		//String dataBaseFileName = "/tmp/testwadodb";
		String dataBaseFileName = arg[0];
		//String savedImagesFolderName = "/tmp/testwadoimages";
		String savedImagesFolderName = arg[1];
		//String dicomPort = "4007";
		int dicomPort = Integer.parseInt(arg[2]);
		//String calledAETitle = "WADOTEST";
		String calledAETitle = arg[3];
		//int wadoPort = 7091;
		int wadoPort = Integer.parseInt(arg[4]);
		String rootURL = "http://" + arg[5] + ":" + arg[4] + "/";
		String requestTypeToUseForInstances = arg[6];
		int storageSCPDebugLevel = arg.length > 7 ? Integer.parseInt(arg[7]) : 0;
		int webServerDebugLevel = arg.length > 8 ? Integer.parseInt(arg[8]) : 0;
		new Thread(new RequestTypeServer(dataBaseFileName,savedImagesFolderName,dicomPort,calledAETitle,storageSCPDebugLevel,wadoPort,webServerDebugLevel,rootURL,stylesheetPath,requestTypeToUseForInstances)).start();
	}
}


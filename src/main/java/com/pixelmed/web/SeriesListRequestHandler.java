/* Copyright (c) 2004-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.web;

import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import com.pixelmed.database.DatabaseInformationModel;
import com.pixelmed.dicom.InformationEntity;

/**
 * <p>The {@link com.pixelmed.web.SeriesListRequestHandler SeriesListRequestHandler} creates a response to an HHTP request for
 * a list of series for a specified study.</p>
 *
 * @author	dclunie
 */
class SeriesListRequestHandler extends RequestHandler {
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/web/SeriesListRequestHandler.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	protected String requestTypeToUseForInstances;
	
	protected SeriesListRequestHandler(String stylesheetPath,String requestTypeToUseForInstances,int webServerDebugLevel) {
		super(stylesheetPath,webServerDebugLevel);
		this.requestTypeToUseForInstances = requestTypeToUseForInstances;
	}

	private class CompareDatabaseAttributesBySeriesNumber implements Comparator {
		public int compare(Object o1,Object o2) {
			int returnValue = 0;
			String si1 = (String)(((Map)o1).get("SERIESNUMBER"));
			String si2 = (String)(((Map)o2).get("SERIESNUMBER"));
			try {
				int i1 = Integer.parseInt(si1);
				int i2 = Integer.parseInt(si2);
				returnValue = i1 - i2;
			}
			catch (NumberFormatException e) {
				e.printStackTrace(System.err);
			}
			return returnValue;
		}
	}
	
	private Comparator compareDatabaseAttributesBySeriesNumber = new CompareDatabaseAttributesBySeriesNumber();

	protected void generateResponseToGetRequest(DatabaseInformationModel databaseInformationModel,String rootURL,String requestURI,WebRequest request,String requestType,OutputStream out) throws IOException {
		try {
			Map parameters = request.getParameters();
			if (parameters == null) {
				throw new Exception("Missing parameters for requestType \""+requestType+"\"");
			}
			String parentPrimaryKey = (String)(parameters.get("primaryKey"));
			if (parentPrimaryKey == null || parentPrimaryKey.length() == 0) {
				throw new Exception("Missing primaryKey parameter for requestType \""+requestType+"\"");
			}
			String studyInstanceUID = (String)(parameters.get("studyUID"));
			if (studyInstanceUID == null || studyInstanceUID.length() == 0) {
				throw new Exception("Missing studyUID parameter for requestType \""+requestType+"\"");
			}
			
			StringBuffer strbuf = new StringBuffer();
			strbuf.append("<html>");
			strbuf.append("<head>");
			strbuf.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
			if (stylesheetPath != null) {
				strbuf.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
				strbuf.append(rootURL);
				strbuf.append(stylesheetPath);
				strbuf.append("\">");
			}
			strbuf.append("</head>\r\n");
			strbuf.append("<body><table>\r\n");
			strbuf.append("<tr><th>Series #</th><th>Series Date</th><th>Series Time</th><th>Modality</th><th>Series Description</th></tr>\r\n");
			String primaryKeyColumnName = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.SERIES);
			ArrayList listOfSeries = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(
				InformationEntity.SERIES,parentPrimaryKey);

			Collections.sort(listOfSeries,compareDatabaseAttributesBySeriesNumber);
			
			int numberOfSeries = listOfSeries.size();
			for (int s=0; s<numberOfSeries; ++s) {
				Map series = (Map)(listOfSeries.get(s));
				String seriesNumber = (String)(series.get("SERIESNUMBER"));
				String seriesDate = (String)(series.get("SERIESDATE"));
				String seriesTime = (String)(series.get("SERIESTIME"));
				String modality = (String)(series.get("MODALITY"));
				String seriesDescription = (String)(series.get("SERIESDESCRIPTION"));
				String seriesInstanceUID = (String)(series.get("SERIESINSTANCEUID"));
				String primaryKey = (String)(series.get(primaryKeyColumnName));
				strbuf.append("<tr>");
				strbuf.append("<td class=\"centered\">");
				strbuf.append("<a href=\"");
				strbuf.append(rootURL);
				//strbuf.append("?requestType=INSTANCELIST");
				//strbuf.append("?requestType=IMAGEDISPLAY");
				//strbuf.append("?requestType=APPLETDISPLAY");
				strbuf.append("?requestType=");
				strbuf.append(requestTypeToUseForInstances);
				strbuf.append("&primaryKey=");
				strbuf.append(primaryKey);
				strbuf.append("&studyUID=");
				strbuf.append(studyInstanceUID);
				strbuf.append("&seriesUID=");
				strbuf.append(seriesInstanceUID);
				//strbuf.append("\" target=\"navigationWindow\">");
				strbuf.append("\">");
				strbuf.append(seriesNumber == null || seriesNumber.length() == 0 ? "NONE" : seriesNumber);	// need something to click on !
				strbuf.append("</a>");
				strbuf.append("</td>");
				strbuf.append("<td class=\"centered\">");
				strbuf.append(seriesDate == null ? "&nbsp;" : seriesDate);
				strbuf.append("</span>");
				strbuf.append("</td>");
				strbuf.append("<td class=\"centered\">");
				strbuf.append(seriesTime == null ? "&nbsp;" : seriesTime);
				strbuf.append("</td>");
				strbuf.append("<td class=\"centered\">");
				strbuf.append(modality == null ? "&nbsp;" : modality);
				strbuf.append("</td>");
				strbuf.append("<td>");
				strbuf.append(seriesDescription == null ? "&nbsp;" : seriesDescription);
				strbuf.append("</td>");
				strbuf.append("</tr>");
			}
			strbuf.append("</table></body></html>\r\n");
			String responseBody = strbuf.toString();
			sendHeaderAndBodyText(out,responseBody,"series.html","text/html");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
if (webServerDebugLevel > 0) System.err.println("SeriesListRequestHandler.generateResponseToGetRequest(): Sending 404 Not Found");
			send404NotFound(out,e.getMessage());
		}
	}
}


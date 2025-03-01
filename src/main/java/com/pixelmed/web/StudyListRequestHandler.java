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
 * <p>The {@link com.pixelmed.web.StudyListRequestHandler StudyListRequestHandler} creates a response to an HTTP request for
 * a list of studies for a specified patient.</p>
 *
 * @author	dclunie
 */
class StudyListRequestHandler extends RequestHandler {
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/web/StudyListRequestHandler.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	protected StudyListRequestHandler(String stylesheetPath,int webServerDebugLevel) {
		super(stylesheetPath,webServerDebugLevel);
	}

	private class CompareDatabaseAttributesByStudyDate implements Comparator {
		public int compare(Object o1,Object o2) {
			int returnValue = 0;
			String si1 = (String)(((Map)o1).get("STUDYDATE"));
			String si2 = (String)(((Map)o2).get("STUDYDATE"));
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
	
	private Comparator compareDatabaseAttributesByStudyDate = new CompareDatabaseAttributesByStudyDate();

	protected void generateResponseToGetRequest(DatabaseInformationModel databaseInformationModel,String rootURL,String requestURI,WebRequest request,String requestType,OutputStream out) throws IOException {
		try {
			Map parameters = request.getParameters();
			if (parameters == null) {
				throw new Exception("Missing parameters for requestType \""+requestType+"\"");
			}
			String parentPrimaryKey = (String)(parameters.get("primaryKey"));
			if (parentPrimaryKey == null) {
				throw new Exception("Missing primaryKey parameter for requestType \""+requestType+"\"");
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
			strbuf.append("<tr><th>Study ID</th><th>Accession #</th><th>Study Date</th><th>Study Time</th><th>Study Description</th></tr>\r\n");
			String primaryKeyColumnName = databaseInformationModel.getLocalPrimaryKeyColumnName(InformationEntity.STUDY);
			ArrayList studies = databaseInformationModel.findAllAttributeValuesForAllRecordsForThisInformationEntityWithSpecifiedParent(
				InformationEntity.STUDY,parentPrimaryKey);

			Collections.sort(studies,compareDatabaseAttributesByStudyDate);
			
			int numberOfStudies = studies.size();
			for (int s=0; s<numberOfStudies; ++s) {
				Map study = (Map)(studies.get(s));
				String studyID = (String)(study.get("STUDYID"));
				String accessionNumber = (String)(study.get("ACCESSIONNUMBER"));
				String studyDate = (String)(study.get("STUDYDATE"));
				String studyTime = (String)(study.get("STUDYTIME"));
				String studyDescription = (String)(study.get("STUDYDESCRIPTION"));
				String studyInstanceUID = (String)(study.get("STUDYINSTANCEUID"));
				String primaryKey = (String)(study.get(primaryKeyColumnName));
				strbuf.append("<tr>");
				strbuf.append("<td class=\"centered\">");
				strbuf.append("<a href=\"");
				strbuf.append(rootURL);
				strbuf.append("?requestType=SERIESLIST");
				strbuf.append("&primaryKey=");
				strbuf.append(primaryKey);
				strbuf.append("&studyUID=");
				strbuf.append(studyInstanceUID);
				//strbuf.append("\" target=\"navigationWindow\">");
				strbuf.append("\">");
				strbuf.append(studyID == null || studyID.length() == 0 ? "NONE" : studyID);	// need something to click on !
				strbuf.append("</a>");
				strbuf.append("</td>");
				strbuf.append("<td class=\"centered\">");
				strbuf.append(accessionNumber == null ? "&nbsp;" : accessionNumber);
				strbuf.append("</td>");
				strbuf.append("<td class=\"centered\">");
				strbuf.append(studyDate == null ? "&nbsp;" : studyDate);
				strbuf.append("</span>");
				strbuf.append("</td>");
				strbuf.append("<td class=\"centered\">");
				strbuf.append(studyTime == null ? "&nbsp;" : studyTime);
				strbuf.append("</td>");
				strbuf.append("<td>");
				strbuf.append(studyDescription == null ? "&nbsp;" : studyDescription);
				strbuf.append("</td>");
				strbuf.append("</tr>");
			}
			strbuf.append("</table></body></html>\r\n");
			String responseBody = strbuf.toString();
			sendHeaderAndBodyText(out,responseBody,"studies.html","text/html");
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
if (webServerDebugLevel > 0) System.err.println("StudyListRequestHandler.generateResponseToGetRequest(): Sending 404 Not Found");
			send404NotFound(out,e.getMessage());
		}
	}
}


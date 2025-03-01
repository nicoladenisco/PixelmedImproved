/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.FileUtilities;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * <p>This class provides a main method that recursively searches the supplied paths for DICOM files and moves them into a folder hierarchy based on their attributes.</p>
 *
 * @author	dclunie
 */
public class MoveDicomFilesIntoHierarchy {

	static protected String hierarchicalFolderName = "Sorted";
	static protected String duplicatesFolderNamePrefix = "Duplicates";

	static protected void processFilesRecursively(File file,String suffix) throws SecurityException, IOException, DicomException, NoSuchAlgorithmException {
		if (file != null && file.exists()) {
			if (file.isFile() && (suffix == null || suffix.length() == 0 || file.getName().endsWith(suffix))) {
				//System.err.println("\""+file+"\": MD5 "+FileUtilities.md5(file.getCanonicalPath()));
				doSomethingWithEachFile(file);
			}
			else if (file.isDirectory()) {
				{
					File[] filesAndDirectories = file.listFiles();
					if (filesAndDirectories != null && filesAndDirectories.length > 0) {
						for (int i=0; i<filesAndDirectories.length; ++i) {
							processFilesRecursively(filesAndDirectories[i],suffix);
						}
					}
				}
			}
			// else what else could it be
		}
	}

	static protected boolean doSomethingWithEachFile(File file) throws IOException, DicomException, NoSuchAlgorithmException {
		boolean success = false;
		if (DicomFileUtilities.isDicomOrAcrNemaFile(file)) {
			AttributeList list = new AttributeList();
			list.read(file,TagFromName.PixelData);
			String newFileName = "";
			String sopInstanceUID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SOPInstanceUID).replaceAll("[^0-9.]","").trim();
			if (sopInstanceUID.length() == 0) {
				System.err.println("\""+file+"\": no SOP Instance UID - doing nothing");
			}
			else {
				String patientID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientID)
					.replaceAll("[^A-Za-z0-9 ]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]*"," ");
				if (patientID.length() == 0) { patientID = "NOID"; }
				
				String patientName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientName)
					.replaceAll("[^A-Za-z0-9 ^=,.]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]*"," ").replaceAll("^[.]","_");
				if (patientName.length() == 0) { patientName = "NONAME"; }
				
				String studyDate = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDate).replaceAll("[^0-9]","").trim();
				if (studyDate.length() == 0) { studyDate = "19000101"; }
				while (studyDate.length() < 8) { studyDate = studyDate + "0"; }
				
				String studyTime = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyTime).replaceFirst("[.].*$","").replaceAll("[^0-9]","");
				while (studyTime.length() < 6) { studyTime = studyTime + "0"; }
				
				String studyID = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyID)
					.replaceAll("[^A-Za-z0-9 ]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]*"," ");
				
				String studyDescription = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StudyDescription)
					.replaceAll("[^A-Za-z0-9 ]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]*"," ");

				String seriesNumber = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesNumber).replaceAll("[^0-9]","");
				while (seriesNumber.length() < 3) { seriesNumber = "0" + seriesNumber; }
				
				String seriesDescription = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SeriesDescription)
					.replaceAll("[^A-Za-z0-9 ]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]*"," ");
				
				String modality = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.Modality)
					.replaceAll("[^A-Za-z0-9 ]","_").replaceAll("^[ _]*","").replaceAll("[ _]*$","").replaceAll("[ ][ ]*"," ").replaceAll("[_][_]*","_").replaceAll("[_][ ]*"," ")
					.toUpperCase();
								
				String studyLabel = "";
				if (studyID.length() == 0) {
					if (studyDescription.length() == 0) {
						studyLabel = studyDate + " " + studyTime;
					}
					else {
						studyLabel = studyDate + " " + studyTime + " [ - " + studyDescription + "]";
					}
				}
				else {
					if (studyDescription.length() == 0) {
						studyLabel = studyDate + " " + studyTime + " [" + studyID + "]";
					}
					else {
						studyLabel = studyDate + " " + studyTime + " [" + studyID + " - " + studyDescription + "]";
					}
				}
				
				String seriesLabel = "";
				if (modality.length() == 0) {
					if (seriesDescription.length() == 0) {
						seriesLabel = "Series " + seriesNumber + " []";
					}
					else {
						seriesLabel = "Series " + seriesNumber + " [ - " + seriesDescription + "]";
					}
				}
				else {
					if (seriesDescription.length() == 0) {
						seriesLabel = "Series " + seriesNumber + " [" + modality + "]";
					}
					else {
						seriesLabel = "Series " + seriesNumber + " [" + modality + " - " + seriesDescription + "]";
					}
				}
				
				newFileName =
					  patientName + " [" + patientID + "]"
					+ "/" + studyLabel
					+ "/" + seriesLabel
					+ "/" + sopInstanceUID + ".dcm";
				//System.err.println(newFileName);
			}
			if (newFileName.length() > 0) {
				File newFile = new File(hierarchicalFolderName,newFileName);
				if (file.getCanonicalPath().equals(newFile.getCanonicalPath())) {		// Note that file.equals(newFile) is NOT sufficient, and if used will lead to deletion when hash values match below
					System.err.println("\""+file+"\": source and destination same - doing nothing");
				}
				else {
					int duplicateCount=0;
					boolean proceed = false;
					boolean skipMove = false;
					while (!proceed) {
						File newParentDirectory = newFile.getParentFile();
						if (newParentDirectory != null && !newParentDirectory.exists()) {
							if (!newParentDirectory.mkdirs()) {
								System.err.println("\""+file+"\": parent directory creation failed for \""+newFile+"\"");
								// don't suppress move; might still succeed
							}
						}
						if (newFile.exists()) {
							if (FileUtilities.md5(file.getCanonicalPath()).equals(FileUtilities.md5(newFile.getCanonicalPath()))) {
								System.err.println("\""+file+"\": destination exists and is identical - not overwriting - removing original \""+newFile+"\"");
								if (!file.delete()) {
									System.err.println("\""+file+"\": deletion of duplicate original unsuccessful");
								}
								skipMove=true;
								proceed=true;
							}
							else {
								System.err.println("\""+file+"\": destination exists and is different - not overwriting - move duplicate elsewhere \""+newFile+"\"");
								boolean foundNewHome = false;
								newFile = new File(duplicatesFolderNamePrefix+"_"+Integer.toString(++duplicateCount),newFileName);
								// loop around rather than proceed
							}
						}
						else {
							proceed=true;
						}
					}
					if (!skipMove) {
						if (file.renameTo(newFile)) {
							success = true;
							System.err.println("\""+file+"\" moved to \""+newFile+"\"");
						}
						else {
							System.err.println("\""+file+"\": move attempt failed to \""+newFile+"\"");
						}
					}
				}
			}
		}
		else {
			System.err.println("\""+file+"\": not a DICOM file - doing nothing");
		}
		return success;
	}
	
	/**
	 * <p>Recursively search the supplied paths for DICOM files and move them into a folder hierarchy based on their attributes.</p>
	 *
	 * <p>Creates a folder structure in the current working directory of the form:</p>
	 *
	 * <pre>Sorted/PatientName [PatientID]/StudyDate StudyTime [StudyID - StudyDescription]/Series SeriesNumber [Modality - Series Description]/SOPInstanceUID.dcm</pre>
	 *
	 * <p>If the destination file already exists and is identical in content, the original is removed.</p>
	 *
	 * <p>If the destination file already exists and is different in content, it is not overwritten, and the duplicate is moved into a separate Duplicates_n folder.</p>
	 *
	 * @param	arg	array of one or more file or directory names
	 */
	public static void main(String[] arg) {
		try {
			for (int i=0; i<arg.length; ++i) {
				processFilesRecursively(new File(arg[i]),null);
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}	
}

/* Copyright (c) 2001-2011, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.TagFromName;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import java.lang.reflect.Constructor;

/**
 * <p>This class allows the reconstruction of a database from the stored instance files,
 * such as when the database schema model has been changed.</p>
 *
 * @author	dclunie
 */
public class RebuildDatabaseFromInstanceFiles {

	/***/
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/database/RebuildDatabaseFromInstanceFiles.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";
	
	private static long filesProcessed;
	
	private static void processFileOrDirectory(DatabaseInformationModel databaseInformationModel,File file) {
//System.err.println("RebuildDatabaseFromInstanceFiles.processFileOrDirectory(): "+file);
		String fileNameAsUpperCase = file.getName().toUpperCase();
		if (file.isDirectory()
		 && !fileNameAsUpperCase.equals("CDVIEWER")
		 && !fileNameAsUpperCase.equals("CVS")
		 && !fileNameAsUpperCase.equals("ICONS")
		 && !fileNameAsUpperCase.equals("JRE")
		 && !fileNameAsUpperCase.equals("LOCALE")
		 && !fileNameAsUpperCase.equals("RESOURCES")
		 && !fileNameAsUpperCase.equals("THUMBNAILS")
		 && !fileNameAsUpperCase.endsWith(".APP")
		) {
System.err.println("Recursing into directory "+file);
			try {
				File listOfFiles[] = file.listFiles();
				for (int i=0; i<listOfFiles.length; ++i) {	
					processFileOrDirectory(databaseInformationModel,listOfFiles[i]);
				}
			}
			catch (Exception e) {
				//System.err.println(e);
				e.printStackTrace(System.err);
			}
		}
		else if (file.isFile()) {
			if (!file.isHidden()
			 && !fileNameAsUpperCase.endsWith(".APP")
			 && !fileNameAsUpperCase.endsWith(".BAK")
			 && !fileNameAsUpperCase.endsWith(".BAT")
			 && !fileNameAsUpperCase.endsWith(".BDT")
			 && !fileNameAsUpperCase.endsWith(".BIN")
			 && !fileNameAsUpperCase.endsWith(".BMP")
			 && !fileNameAsUpperCase.endsWith(".BZ2")
			 && !fileNameAsUpperCase.endsWith(".CAB")
			 && !fileNameAsUpperCase.endsWith(".CFG")
			 && !fileNameAsUpperCase.endsWith(".CHM")
			 && !fileNameAsUpperCase.endsWith(".CM")
			 && !fileNameAsUpperCase.endsWith(".CNF")
			 && !fileNameAsUpperCase.endsWith(".CNT")
			 && !fileNameAsUpperCase.endsWith(".COL")
			 && !fileNameAsUpperCase.endsWith(".CONFIG")
			 && !fileNameAsUpperCase.endsWith(".CRT.MANIFEST")
			 && !fileNameAsUpperCase.endsWith(".CSS")
			 && !fileNameAsUpperCase.endsWith(".CUR")
			 && !fileNameAsUpperCase.endsWith(".DAT")
			 && !fileNameAsUpperCase.endsWith(".DB")
			 && !fileNameAsUpperCase.endsWith(".DCT")
			 && !fileNameAsUpperCase.endsWith(".DLL")
			 && !fileNameAsUpperCase.endsWith(".DOC")
			 && !fileNameAsUpperCase.endsWith(".DTD")
			 && !fileNameAsUpperCase.endsWith(".EXE")
			 && !fileNameAsUpperCase.endsWith(".EXE.MANIFEST")
			 && !fileNameAsUpperCase.endsWith(".GIF")
			 && !fileNameAsUpperCase.endsWith(".HDR")
			 && !fileNameAsUpperCase.endsWith(".HLP")
			 && !fileNameAsUpperCase.endsWith(".HQX")
			 && !fileNameAsUpperCase.endsWith(".HTC")
			 && !fileNameAsUpperCase.endsWith(".HTF")
			 && !fileNameAsUpperCase.endsWith(".HTM")
			 && !fileNameAsUpperCase.endsWith(".HTML")
			 && !fileNameAsUpperCase.endsWith(".IBT")
			 && !fileNameAsUpperCase.endsWith(".ICO")
			 && !fileNameAsUpperCase.endsWith(".IDX")
			 && !fileNameAsUpperCase.endsWith(".INF")
			 && !fileNameAsUpperCase.endsWith(".INI")
			 && !fileNameAsUpperCase.endsWith(".INX")
			 && !fileNameAsUpperCase.endsWith(".ISO")
			 && !fileNameAsUpperCase.endsWith(".JAR")
			 && !fileNameAsUpperCase.endsWith(".JPEG")
			 && !fileNameAsUpperCase.endsWith(".JPG")
			 && !fileNameAsUpperCase.endsWith(".JS")
			 && !fileNameAsUpperCase.endsWith(".JSE")
			 && !fileNameAsUpperCase.endsWith(".LNK")
			 && !fileNameAsUpperCase.endsWith(".LOG")
			 && !fileNameAsUpperCase.endsWith(".LST")
			 && !fileNameAsUpperCase.endsWith(".MDB")
			 && !fileNameAsUpperCase.endsWith(".MFC.MANIFEST")
			 && !fileNameAsUpperCase.endsWith(".MO")
			 && !fileNameAsUpperCase.endsWith(".MSG")
			 && !fileNameAsUpperCase.endsWith(".MSI")
			 && !fileNameAsUpperCase.endsWith(".MSO")
			 && !fileNameAsUpperCase.endsWith(".NIB")
			 && !fileNameAsUpperCase.endsWith(".OCX")
			 && !fileNameAsUpperCase.endsWith(".ORG")
			 && !fileNameAsUpperCase.endsWith(".PAL")
			 && !fileNameAsUpperCase.endsWith(".PDF")
			 && !fileNameAsUpperCase.endsWith(".PFL")
			 && !fileNameAsUpperCase.endsWith(".PNG")
			 && !fileNameAsUpperCase.endsWith(".PRO")
			 && !fileNameAsUpperCase.endsWith(".PROPERTIES")
			 && !fileNameAsUpperCase.endsWith(".RAR")
			 && !fileNameAsUpperCase.endsWith(".RES")
			 && !fileNameAsUpperCase.endsWith(".RTC")
			 && !fileNameAsUpperCase.endsWith(".RTF")
			 && !fileNameAsUpperCase.endsWith(".SIT")
			 && !fileNameAsUpperCase.endsWith(".SRV")
			 && !fileNameAsUpperCase.endsWith(".SWF")
			 && !fileNameAsUpperCase.endsWith(".TB2")
			 && !fileNameAsUpperCase.endsWith(".TIF")
			 && !fileNameAsUpperCase.endsWith(".TPL")
			 && !fileNameAsUpperCase.endsWith(".TXT")
			 && !fileNameAsUpperCase.endsWith(".XML")
			 && !fileNameAsUpperCase.endsWith(".XSL")
			 && !fileNameAsUpperCase.endsWith(".ZIP")
			 && !fileNameAsUpperCase.endsWith("ABOUT.HTA")
			 && !fileNameAsUpperCase.endsWith("ACRCODES.ACR")
			 && !fileNameAsUpperCase.endsWith("ACRCODES-EN.ACR")
			 && !fileNameAsUpperCase.endsWith("CDINFO")
			 && !fileNameAsUpperCase.endsWith("DATABASE.BACKUP")
			 && !fileNameAsUpperCase.endsWith("DATABASE.DATA")
			 && !fileNameAsUpperCase.endsWith("DATABASE.SCRIPT")
			 && !fileNameAsUpperCase.endsWith("DICOMDIR")
			 && !fileNameAsUpperCase.endsWith("JAR.OLD")
			 && !fileNameAsUpperCase.endsWith("LOGGER")
			 && !fileNameAsUpperCase.endsWith("NAVIGATOR.HTA")
			 && !fileNameAsUpperCase.endsWith("NOTES")
			 && !fileNameAsUpperCase.endsWith("README")
			 && !fileNameAsUpperCase.endsWith("TOOLBARCONFIGURATION.OLD")
			) {
System.err.println("Doing file "+file);
				try {
					DicomInputStream dfi = new DicomInputStream(new BufferedInputStream(new FileInputStream(file)));
					AttributeList list = new AttributeList();
//System.err.println("Starting read "+file);
					list.read(dfi,TagFromName.PixelData);
//System.err.println("Finished read "+file);
					dfi.close();
					//d.extendTablesAsNecessary(list);		// doesn't work with Hypersonic ... ALTER command not supported
					databaseInformationModel.insertObject(list,file.getAbsolutePath(),DatabaseInformationModel.FILE_COPIED);
					++filesProcessed;
				}
				catch (Exception e) {
					//System.err.println(e);
					e.printStackTrace(System.err);
				}
			}
			else {
System.err.println("Skipping hidden or unwanted "+file);
			}
		}
		else {
System.err.println("Not a directory (that we want) or file "+file);
		}
	}

	/**
	 * <p>Read the DICOM files listed on the command line, load them into the specified model and store
	 * the database files in the specified location.</p>
	 *
	 * @param	arg	the class name of the model, the (full) path of the database file prefix, and a list of DICOM file names or directories
	 */
	public static void main(String arg[]) {
		RebuildDatabaseFromInstanceFiles ourselves = new RebuildDatabaseFromInstanceFiles();
		if (arg.length >= 3) {
			String databaseModelClassName = arg[0];
			String databaseFileName = arg[1];
		
			if (databaseModelClassName.indexOf('.') == -1) {					// not already fully qualified
				databaseModelClassName="com.pixelmed.database."+databaseModelClassName;
			}
//System.err.println("Class name = "+databaseModelClassName);

			//DatabaseInformationModel databaseInformationModel = new PatientStudySeriesConcatenationInstanceModel(makePathToFileInUsersHomeDirectory(dataBaseFileName));
			DatabaseInformationModel databaseInformationModel = null;
			try {
				Class classToUse = Thread.currentThread().getContextClassLoader().loadClass(databaseModelClassName);
				Class[] parameterTypes = { databaseFileName.getClass() };
				Constructor constructorToUse = classToUse.getConstructor(parameterTypes);
				Object[] args = { databaseFileName };
				databaseInformationModel = (DatabaseInformationModel)(constructorToUse.newInstance(args));
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
				System.exit(0);
			}
			long startOfRebuild=System.currentTimeMillis();
			filesProcessed=0;
			int i = 2;		// start with 3rd argument
			while (i<arg.length) {
				String name = arg[i++];
				File file = new File(name);
				processFileOrDirectory(databaseInformationModel,file);
			}
			long durationOfRebuild = System.currentTimeMillis() - startOfRebuild;
			double rate = ((double)filesProcessed)/(((double)durationOfRebuild)/1000);
			System.err.println("Processed "+filesProcessed+" files in "+durationOfRebuild+" ms, "+rate+" files/s");
		}
		else {
			System.err.println("Usage: java com.pixelmed.database.RebuildDatabaseFromInstanceFiles databaseModelClassName databaseFilePathPrefix databaseFileName path(s)");
		}
	}
}


/* Copyright (c) 2001-2005, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.query.RetrieveResponseGenerator;
import com.pixelmed.query.RetrieveResponseGeneratorFactory;

class DicomDatabaseRetrieveResponseGeneratorFactory implements RetrieveResponseGeneratorFactory {
	/***/
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/database/DicomDatabaseRetrieveResponseGeneratorFactory.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";
	/***/
	private int debugLevel;
	/***/
	private DatabaseInformationModel databaseInformationModel;

	DicomDatabaseRetrieveResponseGeneratorFactory(DatabaseInformationModel databaseInformationModel,int debugLevel) {
//System.err.println("DicomDatabaseRetrieveResponseGeneratorFactory():");
		this.debugLevel=debugLevel;
		this.databaseInformationModel=databaseInformationModel;
	}
	
	public RetrieveResponseGenerator newInstance() {
		return new DicomDatabaseRetrieveResponseGenerator(databaseInformationModel,debugLevel);
	}

}


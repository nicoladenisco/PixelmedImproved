/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.query;

import com.pixelmed.dicom.*;

import java.util.*;

/**
 * <p>The {@link com.pixelmed.query.StudyRootQueryInformationModel StudyRootQueryInformationModel}
 * supports query and retrieval using the DICOM Study Root information model.</p>
 *
 * <p>Specifically, all patient attributes are included at the level of the study, and below
 * the study level are series and instance (image) levels.</p>
 *
 * <p>For example, an application might instantiate a
 * {@link com.pixelmed.query.StudyRootQueryInformationModel StudyRootQueryInformationModel}, and
 * then actually perform a query (with debugging messages on) using a list of attributes as follows:</p>
 *
 * <pre>
 * 	final QueryInformationModel model = new StudyRootQueryInformationModel("remotehost",104,"THEIRAET","OURAET",1);
 * 	final QueryTreeModel tree = model.performHierarchicalQuery(identifier);
 *	System.err.println("Tree="+tree);
 * </pre>
 *
 * <p>The attribute list supplied must contain both matching and response keys. By way of
 * example, one could construct an identifier for a query of all the instances for a particular patient
 * named "Smith^Mary" as follows
 *
 * <pre>
 * 	AttributeList identifier = new AttributeList();
 * 	{ AttributeTag t = TagFromName.PatientName; Attribute a = new PersonNameAttribute(t,specificCharacterSet); a.addValue("Smith^Mary"); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.PatientID; Attribute a = new ShortStringAttribute(t,specificCharacterSet); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.PatientBirthDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.PatientSex; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
 *
 * 	{ AttributeTag t = TagFromName.StudyID; Attribute a = new ShortStringAttribute(t,specificCharacterSet); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.StudyDescription; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.ModalitiesInStudy; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.PatientAge; Attribute a = new AgeStringAttribute(t); filter.put(t,a); }
 *
 * 	{ AttributeTag t = TagFromName.SeriesDescription; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.SeriesNumber; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.Modality; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.SeriesDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.SeriesTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
 *
 * 	{ AttributeTag t = TagFromName.InstanceNumber; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.ContentDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.ContentTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.ImageType; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.NumberOfFrames; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
 *
 * 	{ AttributeTag t = TagFromName.StudyInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.SeriesInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.SOPInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.SOPClassUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.SpecificCharacterSet; Attribute a = new CodeStringAttribute(t); filter.put(t,a); a.addValue("ISO_IR 100"); }
 * </pre>
 *
 * <p>The resulting tree will contain all the returned information at the study, series and
 * instance (image) returned which match a PatientName of "Smith^Mary". If one wanted to
 * filer the list of studies and series returned by a Modality of CT, then one could
 * instead have created an identifier containing:</p>
 *
 * <pre>
 * 	{ AttributeTag t = TagFromName.ModalitiesInStudy; Attribute a = new CodeStringAttribute(t); a.addValue("CT"); filter.put(t,a); }
 * 	{ AttributeTag t = TagFromName.Modality; Attribute a = new CodeStringAttribute(t); a.addValue("CT"); filter.put(t,a); }
 * </pre>
 *
 * <p>Note that since ModalitiesInStudy is an optional matching key in DICOM, not all
 * SCPs will support it, so one should also filter at the series level with Modality.
 * In those cases "empty" study responses will be included when the study matches
 * but there are no modality-specific series.</p>
 *
 * <p>Note also that no "client side" filtering is performed ... that is the values
 * for matching keys will be sent to the SCP, but if the SCP doesn't match on them
 * and returns responses anyway, this class does not filter out those responses
 * before returning them to the user (including them in the returned tree).</p>
 *
 * <p>In a real application, the {@link com.pixelmed.query.FilterPanel FilterPanel}
 * class can be used to provide a user interface for editing the values in the
 * request identifier attribute list.</p>
 *
 * @see com.pixelmed.query.QueryInformationModel
 * @see com.pixelmed.query.QueryTreeModel
 * @see com.pixelmed.query.FilterPanel
 * @see com.pixelmed.dicom.AttributeList
 * @see com.pixelmed.dicom.Attribute
 * @see com.pixelmed.dicom.AttributeTag
 * @see com.pixelmed.dicom.TagFromName
 *
 * @author	dclunie
 */
public class StudyRootQueryInformationModel extends QueryInformationModel {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/query/StudyRootQueryInformationModel.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";
	
	private static HashSet useForStudy;
	private static HashSet useForSeries;
	private static HashSet useForInstance;

	/**
	 * <p>Construct a study root query information model.</p>
	 *
	 * @param	hostname		their hostname or IP address
	 * @param	port			their port number
	 * @param	calledAETitle		their AE title
	 * @param	callingAETitle		our AE title (both when we query or retrieve and where we are listening as a storage SCP)
	 * @param	debugLevel		0 is no debugging (silent), > 0 more verbose levels of debugging
	 */
	public StudyRootQueryInformationModel(String hostname,int port,String calledAETitle,String callingAETitle,int debugLevel) {
		super(hostname,port,calledAETitle,callingAETitle,debugLevel);
		if (useForStudy == null) {
			useForStudy=new HashSet();
			useForStudy.add(InformationEntity.PATIENT);
			useForStudy.add(InformationEntity.STUDY);
		}
		if (useForSeries == null) {
			useForSeries=new HashSet();
			useForSeries.add(InformationEntity.SERIES);
			useForSeries.add(InformationEntity.PROCEDURESTEP);
		}
		if (useForInstance == null) {
			useForInstance=new HashSet();
			useForInstance.add(InformationEntity.INSTANCE);
			useForInstance.add(InformationEntity.CONCATENATION);
		}
	}
	
	/***/
	protected InformationEntity getRoot() { return InformationEntity.STUDY; }

	/**
	 * @param	ie
	 */
	protected InformationEntity getChildTypeForParent(InformationEntity ie) {
		if      (ie == InformationEntity.STUDY)         return InformationEntity.SERIES;
		else if (ie == InformationEntity.SERIES)        return InformationEntity.INSTANCE;
		else return null;
	}

	/**
	 * @param	ie
	 */
	protected HashSet getAllInformationEntitiesToIncludeAtThisQueryLevel(InformationEntity ie) {
		if      (ie == InformationEntity.STUDY)         return useForStudy;
		else if (ie == InformationEntity.SERIES)        return useForSeries;
		else if (ie == InformationEntity.INSTANCE)      return useForInstance;
		else return null;
	}
	
	/***/
	protected String getFindSOPClassUID() { return SOPClass.StudyRootQueryRetrieveInformationModelFind; }
	/***/
	protected String getMoveSOPClassUID() { return SOPClass.StudyRootQueryRetrieveInformationModelMove; }
	
	/**
	 * @param	ie
	 * @param	responseIdentifier
	 */
	protected String getStringValueForTreeFromResponseIdentifier(InformationEntity ie,AttributeList responseIdentifier) {
		StringBuffer buf = new StringBuffer();
		String useIEName=ie.toString();
		if (ie == InformationEntity.INSTANCE) {
			// SOPClassUID is not a required key and is often absent in the response, but try anyway ...
			String sopClassUID = Attribute.getSingleStringValueOrNull(responseIdentifier,TagFromName.SOPClassUID);
//System.err.println("sopClassUID="+sopClassUID);
			if (sopClassUID != null) {
				if      (SOPClass.isImageStorage(sopClassUID)) useIEName="Image";
				else if (SOPClass.isStructuredReport(sopClassUID)) useIEName="SR Document";
				else if (SOPClass.isWaveform(sopClassUID)) useIEName="Waveform";
				else if (SOPClass.isSpectroscopy(sopClassUID)) useIEName="Spectra";
				else if (SOPClass.isRawData(sopClassUID)) useIEName="Raw Data";
			}
		}
		if      (ie == InformationEntity.STUDY) {
			buf.append(InformationEntity.PATIENT.toString());
			buf.append(" ");
			buf.append(Attribute.getSingleStringValueOrEmptyString(responseIdentifier,TagFromName.PatientName));
			buf.append(" ");
			buf.append(Attribute.getSingleStringValueOrEmptyString(responseIdentifier,TagFromName.PatientID));
			buf.append(" ");
			buf.append(InformationEntity.STUDY.toString());
			buf.append(" ");
			buf.append(Attribute.getSingleStringValueOrEmptyString(responseIdentifier,TagFromName.StudyDate));	// [bugs.mrmf] (000111) Studies in browser not sorted by date but ID, and don't display date
			buf.append(" ");
			buf.append(Attribute.getSingleStringValueOrEmptyString(responseIdentifier,TagFromName.StudyID));
			buf.append(" ");
			buf.append(Attribute.getSingleStringValueOrEmptyString(responseIdentifier,TagFromName.StudyDescription));
		}
		else if (ie == InformationEntity.SERIES) {
			buf.append(useIEName);
			buf.append(" ");
			buf.append(DescriptionFactory.makeSeriesDescription(responseIdentifier));
		}
		else if (ie == InformationEntity.INSTANCE) {
			buf.append(useIEName);
			buf.append(" ");
			// useIEName will rarely have a value, since SOPClassUID is not a required key and often absent in the response
			//if (useIEName.equals("Image")) {
				buf.append(DescriptionFactory.makeImageDescription(responseIdentifier));
			//}
			//else {
			//	buf.append(Attribute.getSingleStringValueOrEmptyString(responseIdentifier,TagFromName.InstanceNumber));
			//	buf.append(" ");
			//	buf.append(Attribute.getSingleStringValueOrEmptyString(responseIdentifier,TagFromName.ImageComments));
			//}
		}
		else {
			buf.append(useIEName);
		}
		return buf.toString();
	}
	
	/**
	 * <p>Unit testing.</p>
	 *
	 * @param	arg	an array of four strings, hostname, port, calledAETitle, callingAETitle
	 */
	public static void main(String arg[]) {
		try {
			String[] characterSets = { "ISO_IR 100" };
			SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet(characterSets);
			AttributeList filter = new AttributeList();
			{ AttributeTag t = TagFromName.PatientName; Attribute a = new PersonNameAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.PatientID; Attribute a = new ShortStringAttribute(t,specificCharacterSet); filter.put(t,a); }

			{ AttributeTag t = TagFromName.PatientBirthDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.PatientSex; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.PatientBirthTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
			//kills Leonardo ... { AttributeTag t = TagFromName.OtherPatientIDs; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			//kills Leonardo ... { AttributeTag t = TagFromName.OtherPatientNames; Attribute a = new PersonNameAttribute(t,specificCharacterSet); filter.put(t,a); }
			//kills Leonardo ... { AttributeTag t = TagFromName.EthnicGroup; Attribute a = new ShortStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.PatientComments; Attribute a = new LongTextAttribute(t,specificCharacterSet); filter.put(t,a); }

			{ AttributeTag t = TagFromName.StudyID; Attribute a = new ShortStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.StudyDescription; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }

			{ AttributeTag t = TagFromName.ModalitiesInStudy; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.StudyDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.StudyTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.ReferringPhysicianName; Attribute a = new PersonNameAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.AccessionNumber; Attribute a = new ShortStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.PhysiciansOfRecord; Attribute a = new PersonNameAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.NameOfPhysiciansReadingStudy; Attribute a = new PersonNameAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.AdmittingDiagnosesDescription; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.PatientAge; Attribute a = new AgeStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.PatientSize; Attribute a = new DecimalStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.PatientWeight; Attribute a = new DecimalStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.Occupation; Attribute a = new ShortStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.AdditionalPatientHistory; Attribute a = new LongTextAttribute(t,specificCharacterSet); filter.put(t,a); }

			{ AttributeTag t = TagFromName.SeriesDescription; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.SeriesNumber; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.Modality; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }

			{ AttributeTag t = TagFromName.SeriesDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.SeriesTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.PerformingPhysicianName; Attribute a = new PersonNameAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.ProtocolName; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.OperatorsName; Attribute a = new PersonNameAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.Laterality; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.BodyPartExamined; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.Manufacturer; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.ManufacturerModelName; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.StationName; Attribute a = new ShortStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.InstitutionName; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.InstitutionalDepartmentName; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }

			{ AttributeTag t = TagFromName.InstanceNumber; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.ImageComments; Attribute a = new LongTextAttribute(t,specificCharacterSet); filter.put(t,a); }

			{ AttributeTag t = TagFromName.ContentDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.ContentTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.ImageType; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.AcquisitionNumber; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.AcquisitionDate; Attribute a = new DateAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.AcquisitionTime; Attribute a = new TimeAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.AcquisitionDateTime; Attribute a = new DateTimeAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.DerivationDescription; Attribute a = new ShortTextAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.QualityControlImage; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.BurnedInAnnotation; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.LossyImageCompression; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.LossyImageCompressionRatio; Attribute a = new DecimalStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.LossyImageCompressionMethod; Attribute a = new CodeStringAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.ContrastBolusAgent; Attribute a = new LongStringAttribute(t,specificCharacterSet); filter.put(t,a); }
			{ AttributeTag t = TagFromName.NumberOfFrames; Attribute a = new IntegerStringAttribute(t); filter.put(t,a); }

			{ AttributeTag t = TagFromName.StudyInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.SeriesInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.SOPInstanceUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.SOPClassUID; Attribute a = new UniqueIdentifierAttribute(t); filter.put(t,a); }
			{ AttributeTag t = TagFromName.SpecificCharacterSet; Attribute a = new CodeStringAttribute(t); filter.put(t,a); a.addValue(characterSets[0]); }
			QueryInformationModel model = new StudyRootQueryInformationModel(arg[0],Integer.parseInt(arg[1]),arg[2],arg[3],1);
			QueryTreeModel tree = model.performHierarchicalQuery(filter);
System.err.println("Tree="+tree);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(0);
		}
	}
}



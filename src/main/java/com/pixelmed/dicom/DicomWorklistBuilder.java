/*
 *  DicomWorklistBuilder.java
 *  Creato il 7-dic-2011, 18.53.40
 *
 *  Copyright (C) 2011 WinSOFT di Nicola De Nisco
 */
package com.pixelmed.dicom;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Costruttore di worklist DICOM.
 *
 * @author Nicola De Nisco
 */
public class DicomWorklistBuilder
{
  public static final String UID_FINDModalityWorklistInformationModel = "1.2.840.10008.5.1.4.31";
  public static final String UID_DetachedStudyManagementSOPClass = "1.2.840.10008.3.1.2.3.1";
  //
  protected Date studyDate = null, patBirth = null;
  protected String aetitle, modality, stepID, stepDescr, procedureID, procedureDescr,
     studyUID, accessionNumber, admissionID, issuerAdmissionID,
     patName, patID, patSex, alerts, allergies, ethnicGroup, patHistory, pregnancyStatus,
     patState, patLocation, patInformation,
     stationName, stepLocation,
     institutionName;
  protected String codSchema = "CODSCHEMA";

  /**
   * Create an attribute list suitable to become a worklist record.
   *
   * According to the
   * DICOM standard part 4 annex K, the following attributes are type 1 attributes in
   * C-Find RSP messages:
   * Attribute Tag Return Key Type
   * SpecificCharacterSet (0008,0005) 1C (will be checked in WlmDataSourceFileSystem::StartFindRequest(...); this attribute does not have to be checked here)
   * ScheduledProcedureStepSequence (0040,0100) 1
   * > ScheduledStationAETitle (0040,0001) 1
   * > ScheduledProcedureStepStartDate (0040,0002) 1
   * > ScheduledProcedureStepStartTime (0040,0003) 1
   * > Modality (0008,0060) 1
   * > ScheduledProcedureStepDescription (0040,0007) 1C (The ScheduledProcedureStepDescription (0040,0007) or the ScheduledProtocolCodeSequence (0040,0008) or both shall be supported by the SCP; we
   * actually support both, so we have to check if at least one of the two attributes contains valid information.)
   * > ScheduledProtocolCodeSequence (0040,0008) 1C (see abobve)
   * > > CodeValue (0008,0100) 1
   * > > CodingSchemeDesignator (0008,0102) 1
   * > ScheduledProcedureStepID (0040,0009) 1
   * RequestedProcedureID (0040,1001) 1
   * RequestedProcedureDescription (0032,1060) 1C (The RequestedProcedureDescription (0032,1060) or the RequestedProcedureCodeSequence (0032,1064) or both shall be supported by the SCP; we actually
   * support both, so we have to check if at least one of the two attributes contains valid information.)
   * RequestedProcedureCodeSequence (0032,1064) 1C (see abobve)
   * > > CodeValue (0008,0100) 1
   * > > CodingSchemeDesignator (0008,0102) 1
   * StudyInstanceUID (0020,000D) 1
   * ReferencedStudySequence (0008,1110) 2
   * > ReferencedSOPClassUID (0008,1150) 1C (Required if a sequence item is present)
   * > ReferencedSOPInstanceUID (0008,1155) 1C (Required if a sequence item is present)
   * ReferencedPatientSequence (0008,1120) 2
   * > ReferencedSOPClassUID (0008,1150) 1C (Required if a sequence item is present)
   * > ReferencedSOPInstanceUID (0008,1155) 1C (Required if a sequence item is present)
   * PatientsName (0010,0010) 1
   * PatientID (0010,0020) 1
   *
   * @return a populated attribute list
   * @throws java.lang.Exception
   */
  public synchronized DicomAttributeList createDicomWorklistEntry()
     throws Exception
  {
    DicomAttributeList dataset = new DicomAttributeList();
    dataset.putValue(TagFromName.SpecificCharacterSet, "ISO_IR 100");

    SequenceAttribute spss = new SequenceAttribute(TagFromName.ScheduledProcedureStepSequence);
    dataset.put(spss);

    DicomAttributeList alSpss = new DicomAttributeList();
    alSpss.putValue(TagFromName.ScheduledStationAETitle, aetitle);
    alSpss.putValue(TagFromName.ScheduledProcedureStepStartDate, studyDate);
    alSpss.putValue(TagFromName.ScheduledProcedureStepStartTime, studyDate);
    alSpss.putValue(TagFromName.Modality, modality);

    alSpss.putValue(TagFromName.ScheduledProcedureStepID, stepID);
    alSpss.putValue(TagFromName.ScheduledProcedureStepDescription, stepDescr);

    SequenceAttribute spcs = new SequenceAttribute(TagFromName.ScheduledProtocolCodeSequence);
    alSpss.put(spcs);

    DicomAttributeList alSpcs = new DicomAttributeList();
    alSpcs.putValue(TagFromName.CodeValue, stepID);
    alSpcs.putValue(TagFromName.CodingSchemeDesignator, codSchema);
    alSpcs.putValue(TagFromName.CodeMeaning, stepID);
    spcs.addItem(alSpcs);

    alSpss.putValue(TagFromName.ScheduledStationName, stationName);
    alSpss.putValue(TagFromName.ScheduledProcedureStepLocation, stepLocation);
    spss.addItem(alSpss);

    dataset.putValue(TagFromName.RequestedProcedureID, procedureID);
    dataset.putValue(TagFromName.RequestedProcedureDescription, procedureDescr);

    SequenceAttribute rpcs = new SequenceAttribute(TagFromName.RequestedProcedureCodeSequence);
    dataset.put(rpcs);

    DicomAttributeList alRpcs = new DicomAttributeList();
    alRpcs.putValue(TagFromName.CodeValue, procedureID);
    alRpcs.putValue(TagFromName.CodingSchemeDesignator, codSchema);
    alSpcs.putValue(TagFromName.CodeMeaning, stepID);
    rpcs.addItem(alRpcs);

    dataset.putValue(TagFromName.StudyInstanceUID, studyUID);

    SequenceAttribute rss = new SequenceAttribute(TagFromName.ReferencedStudySequence);
    dataset.put(rss);

    DicomAttributeList alRss = new DicomAttributeList();
    alRss.putValue(TagFromName.ReferencedSOPClassUID, UID_DetachedStudyManagementSOPClass);
    alRss.putValue(TagFromName.ReferencedSOPInstanceUID, studyUID);
    rss.addItem(alRss);

    SequenceAttribute rps = new SequenceAttribute(TagFromName.ReferencedPatientSequence);
    dataset.put(rps);

//    DicomAttributeList alRps = new DicomAttributeList();
//    alRps.putValue(TagFromName.ReferencedSOPClassUID, "");
//    alRps.putValue(TagFromName.ReferencedSOPInstanceUID, "");
//    rps.addItem(alRps);
    dataset.putValue(TagFromName.PatientName, patName);
    dataset.putValue(TagFromName.PatientID, patID);
    dataset.putValue(TagFromName.IssuerOfPatientID, "");
    dataset.putValue(TagFromName.PatientBirthDate, patBirth);
    dataset.putValue(TagFromName.PatientSex, patSex);
    dataset.putValue(TagFromName.RegionOfResidence, "");

    dataset.putValue(TagFromName.AdmissionID, admissionID);
    dataset.putValue(TagFromName.IssuerOfAdmissionID, issuerAdmissionID);
    dataset.putValue(TagFromName.PatientState, patState);
    dataset.putValue(TagFromName.CurrentPatientLocation, patLocation);

    dataset.putValue(TagFromName.AccessionNumber, accessionNumber);
    dataset.putValue(TagFromName.InstitutionName, institutionName);

    dataset.putValue(TagFromName.MedicalAlerts, alerts);
    dataset.putValue(TagFromName.Allergies, allergies);
    dataset.putValue(TagFromName.EthnicGroup, ethnicGroup);
    dataset.putValue(TagFromName.AdditionalPatientHistory, patHistory);
    dataset.putValue(TagFromName.PregnancyStatus, pregnancyStatus);
    dataset.putValue(TagFromName.PatientComments, patInformation);

    return dataset;
  }

  /**
   * Create a file with the worklist record information.
   * The file have a meta header and a dataset with
   * all field populated with createDicomWorklistEntry().
   * @param fileWl file to write
   * @return attribute list written in file
   * @throws Exception
   */
  public synchronized DicomAttributeList createDicomWorklistFile(File fileWl)
     throws Exception
  {
    UIDGenerator ug = new UIDGenerator();

    if(studyUID == null)
      studyUID = ug.getNewUID();

    DicomAttributeList al = createDicomWorklistEntry();

    String sopInstanceUID = ug.getNewUID();
    al.putValue(TagFromName.SOPClassUID, UID_FINDModalityWorklistInformationModel);
    al.putValue(TagFromName.SOPInstanceUID, sopInstanceUID);

    FileMetaInformation fmi = new FileMetaInformation(
       UID_FINDModalityWorklistInformationModel,
       sopInstanceUID,
       TransferSyntax.ExplicitVRLittleEndian,
       null);

    try(OutputStream out = new BufferedOutputStream(new FileOutputStream(fileWl));
       DicomOutputStream dout = new DicomOutputStream(out,
          TransferSyntax.ExplicitVRLittleEndian, TransferSyntax.ExplicitVRLittleEndian))
    {
      fmi.getAttributeList().write(dout, true);
      al.write(dout, false);
      dout.flush();
    }

    return al;
  }
}

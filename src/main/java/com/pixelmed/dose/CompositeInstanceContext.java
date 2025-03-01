/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.AgeStringAttribute;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.DecimalStringAttribute;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.TagFromName;

public class CompositeInstanceContext {
	
	protected AttributeList list;
	
	public AttributeList getAttributeList() { return list; }
	
	protected void addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(AttributeList srcList,AttributeTag tag) {
		Attribute a = srcList.get(tag);
		if (a != null) {
			if (a.getVM() > 0 || (a instanceof SequenceAttribute && ((SequenceAttribute)a).getNumberOfItems() > 0)) {
				if (list.get(tag) == null) {
					list.put(tag,a);	// make sure that an empty attribute is add if not already there
				}
				// else leave existing (possibly empty) value alone
			}
			else {
				list.put(tag,a);	// adds, or replaces existing
			}
		}
	}
	
	public CompositeInstanceContext() {
		list = new AttributeList();
	}
	
	public CompositeInstanceContext(AttributeList srcList) {
		list = new AttributeList();
		updateFromSource(srcList);
	}
	
	protected AttributeTag[] patientModuleAttributeTags = {
		TagFromName.PatientName,
		TagFromName.PatientID,
		//Macro IssuerOfPatientIDMacro
		TagFromName.IssuerOfPatientID,
		TagFromName.IssuerOfPatientIDQualifiersSequence,
		//EndMacro IssuerOfPatientIDMacro
		TagFromName.PatientBirthDate,
		TagFromName.PatientSex,
		TagFromName.PatientBirthTime,
		TagFromName.ReferencedPatientSequence,
		TagFromName.OtherPatientIDs,
		TagFromName.OtherPatientIDsSequence,
		TagFromName.OtherPatientNames,
		TagFromName.EthnicGroup,
		TagFromName.PatientComments,
		TagFromName.PatientSpeciesDescription,
		TagFromName.PatientSpeciesCodeSequence,
		TagFromName.PatientBreedDescription,
		TagFromName.PatientBreedCodeSequence,
		TagFromName.BreedRegistrationSequence,
		TagFromName.ResponsiblePerson,
		TagFromName.ResponsiblePersonRole,
		TagFromName.ResponsibleOrganization,
		TagFromName.PatientIdentityRemoved,
		TagFromName.DeidentificationMethod,
		TagFromName.DeidentificationMethodCodeSequence
	};
	
	protected AttributeTag[] generalStudyModuleAttributeTags = {
		TagFromName.StudyInstanceUID,
		TagFromName.StudyDate,
		TagFromName.StudyTime,
		TagFromName.ReferringPhysicianName,
		TagFromName.ReferringPhysicianIdentificationSequence,
		TagFromName.StudyID,
		TagFromName.AccessionNumber,
		TagFromName.IssuerOfAccessionNumberSequence,
		TagFromName.StudyDescription,
		TagFromName.PhysiciansOfRecord,
		TagFromName.PhysiciansOfRecordIdentificationSequence,
		TagFromName.NameOfPhysiciansReadingStudy,
		TagFromName.PhysiciansReadingStudyIdentificationSequence,
		TagFromName.RequestingServiceCodeSequence,
		TagFromName.ReferencedStudySequence,
		TagFromName.ProcedureCodeSequence,
		TagFromName.ReasonForPerformedProcedureCodeSequence
	};

	protected AttributeTag[] patientStudyModuleAttributeTags = {
		TagFromName.AdmittingDiagnosesDescription,
		TagFromName.AdmittingDiagnosesCodeSequence,
		TagFromName.PatientAge,
		TagFromName.PatientSize,
		TagFromName.PatientWeight,
		TagFromName.PatientSizeCodeSequence,
		TagFromName.Occupation,
		TagFromName.AdditionalPatientHistory,
		TagFromName.AdmissionID,
		TagFromName.IssuerOfAdmissionID,
		TagFromName.IssuerOfAdmissionIDSequence,
		TagFromName.ServiceEpisodeID,
		TagFromName.IssuerOfServiceEpisodeIDSequence,
		TagFromName.ServiceEpisodeDescription,
		TagFromName.PatientSexNeutered
	};
	
	protected AttributeTag[] generalSeriesModuleAttributeTags = {
		TagFromName.Modality,
		TagFromName.SeriesInstanceUID,
		TagFromName.SeriesNumber,
		TagFromName.Laterality,
		TagFromName.SeriesDate,
		TagFromName.SeriesTime,
		TagFromName.PerformingPhysicianName,
		TagFromName.PerformingPhysicianIdentificationSequence,
		TagFromName.ProtocolName,
		TagFromName.SeriesDescription,
		TagFromName.SeriesDescriptionCodeSequence,
		TagFromName.OperatorsName,
		TagFromName.OperatorIdentificationSequence,
		TagFromName.ReferencedPerformedProcedureStepSequence,
		TagFromName.RelatedSeriesSequence,
		TagFromName.BodyPartExamined,
		TagFromName.PatientPosition,
		//TagFromName.SmallestPixelValueInSeries,
		//TagFromName.LargestPixelValueInSeries,
		TagFromName.RequestAttributesSequence,
		//Macro PerformedProcedureStepSummaryMacro
		TagFromName.PerformedProcedureStepID,
		TagFromName.PerformedProcedureStepStartDate,
		TagFromName.PerformedProcedureStepStartTime,
		TagFromName.PerformedProcedureStepDescription,
		TagFromName.PerformedProtocolCodeSequence,
		TagFromName.CommentsOnThePerformedProcedureStep,
		//EndMacro PerformedProcedureStepSummaryMacro
		TagFromName.AnatomicalOrientationType
	};
	
	protected AttributeTag[] generalEquipmentModuleAttributeTags = {
		TagFromName.Manufacturer,
		TagFromName.InstitutionName,
		TagFromName.InstitutionAddress,
		TagFromName.StationName,
		TagFromName.InstitutionalDepartmentName,
		TagFromName.ManufacturerModelName,
		TagFromName.DeviceSerialNumber,
		TagFromName.SoftwareVersions,
		TagFromName.GantryID,
		TagFromName.SpatialResolution,
		TagFromName.DateOfLastCalibration,
		TagFromName.TimeOfLastCalibration,
		TagFromName.PixelPaddingValue
	};
	
	protected AttributeTag[] frameOfReferenceModuleAttributeTags = {
		TagFromName.FrameOfReferenceUID,
		TagFromName.PositionReferenceIndicator
	};
	
	protected AttributeTag[] sopCommonModuleAttributeTags = {
		TagFromName.SOPClassUID,
		TagFromName.SOPInstanceUID,
		//TagFromName.SpecificCharacterSet,
		TagFromName.InstanceCreationDate,
		TagFromName.InstanceCreationTime,
		TagFromName.InstanceCreatorUID,
		TagFromName.RelatedGeneralSOPClassUID,
		TagFromName.OriginalSpecializedSOPClassUID,
		TagFromName.CodingSchemeIdentificationSequence,
		TagFromName.TimezoneOffsetFromUTC,
		TagFromName.ContributingEquipmentSequence,
		TagFromName.InstanceNumber,
		TagFromName.SOPInstanceStatus,
		TagFromName.SOPAuthorizationDateTime,
		TagFromName.SOPAuthorizationComment,
		TagFromName.AuthorizationEquipmentCertificationNumber,
		//Macro DigitalSignaturesMacro
		//TagFromName.MACParametersSequence,
		//TagFromName.DigitalSignaturesSequence,
		//EndMacro DigitalSignaturesMacro
		//TagFromName.EncryptedAttributesSequence,
		TagFromName.OriginalAttributesSequence,
		TagFromName.HL7StructuredDocumentReferenceSequence
	};
	
	protected AttributeTag[] srDocumentGeneralModuleAttributeTags = {
		TagFromName.ReferencedRequestSequence,		// cw. RequestAttributesSequence in GeneralSeries
		TagFromName.PerformedProcedureCodeSequence	// cw. ProcedureCodeSequence in GeneralStudy
	};

	public void updateFromSource(AttributeList srcList) {
		for (AttributeTag t : patientModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : generalStudyModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : patientStudyModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : generalSeriesModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : generalEquipmentModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : frameOfReferenceModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : sopCommonModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }
		for (AttributeTag t : srDocumentGeneralModuleAttributeTags) { addOrReplaceIfNotEmptyOtherwiseLeaveUnchanged(srcList,t); }

		// handle population of SRDocumentGeneralModule specific attributes from image equivalents
		{
			Attribute referencedRequestSequence = list.get(TagFromName.ReferencedRequestSequence);
			Attribute requestAttributesSequence = list.get(TagFromName.RequestAttributesSequence);
			if (referencedRequestSequence == null || !(referencedRequestSequence instanceof SequenceAttribute) || ((SequenceAttribute)referencedRequestSequence).getNumberOfItems() == 0) {
				if (requestAttributesSequence != null && requestAttributesSequence instanceof SequenceAttribute) {
					SequenceAttribute sRequestAttributesSequence = (SequenceAttribute)requestAttributesSequence;
					int nItems = sRequestAttributesSequence.getNumberOfItems();
					if (nItems > 0) {
						SequenceAttribute sReferencedRequestSequence = new SequenceAttribute(TagFromName.ReferencedRequestSequence);
						for (int i=0; i<nItems; ++i) {
							SequenceItem item = sRequestAttributesSequence.getItem(i);
//System.err.println("CompositeInstanceContext.updateFromSource(): copying RequestAttributesSequence to ReferencedRequestSequence item "+item);
							sReferencedRequestSequence.addItem(item);			// re-use of same item without cloning it is fine
						}
						list.put(sReferencedRequestSequence);
					}
				}
			}
		}
		{
			Attribute performedProcedureCodeSequence = list.get(TagFromName.PerformedProcedureCodeSequence);
			Attribute procedureCodeSequence = list.get(TagFromName.ProcedureCodeSequence);
			if (performedProcedureCodeSequence == null || !(performedProcedureCodeSequence instanceof SequenceAttribute) || ((SequenceAttribute)performedProcedureCodeSequence).getNumberOfItems() == 0) {
				if (procedureCodeSequence != null && procedureCodeSequence instanceof SequenceAttribute) {
					SequenceAttribute sProcedureCodeSequence = (SequenceAttribute)procedureCodeSequence;
					int nItems = sProcedureCodeSequence.getNumberOfItems();
					if (nItems > 0) {
						SequenceAttribute sPerformedProcedureCodeSequence = new SequenceAttribute(TagFromName.PerformedProcedureCodeSequence);
						for (int i=0; i<nItems; ++i) {
							SequenceItem item = sProcedureCodeSequence.getItem(i);
//System.err.println("CompositeInstanceContext.updateFromSource(): copying ProcedureCodeSequence to PerformedProcedureCodeSequence item "+item);
							sPerformedProcedureCodeSequence.addItem(item);			// re-use of same item without cloning it is fine
						}
						list.put(sPerformedProcedureCodeSequence);
					}
				}
			}
		}
	}
	
	public void updateFromSource(CTIrradiationEventDataFromImages eventDataFromImages) {
		if (eventDataFromImages != null) {
			try {
				// in case the patient sex, age, weight or size (height) were not in the source instance, use what was found in the other instances, if it is consistent ...
				String patientAge = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientAge);
				if (patientAge.length() == 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): no PatientAge in list");
					patientAge = eventDataFromImages.getPatientAge();
					if (patientAge != null && patientAge.length() > 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): found PatientAge in eventDataFromImages");
						{ Attribute a = new AgeStringAttribute(TagFromName.PatientAge); a.addValue(patientAge); list.put(a); }
					}
				}
			}
			catch (DicomException e) {
				e.printStackTrace(System.err);
			}
			try {
				String patientSex = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientSex);
				if (patientSex.length() == 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): no PatientSex in list");
					patientSex = eventDataFromImages.getPatientSex();
					if (patientSex != null && patientSex.length() > 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): found PatientSex in eventDataFromImages");
						{ Attribute a = new CodeStringAttribute(TagFromName.PatientSex); a.addValue(patientSex); list.put(a); }
					}
				}
			}
			catch (DicomException e) {
				e.printStackTrace(System.err);
			}
			try {
				String patientWeight = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientWeight);
				if (patientWeight.length() == 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): no PatientWeight in list");
					patientWeight = eventDataFromImages.getPatientWeight();
					if (patientWeight != null && patientWeight.length() > 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): found PatientWeight in eventDataFromImages");
						{ Attribute a = new DecimalStringAttribute(TagFromName.PatientWeight); a.addValue(patientWeight); list.put(a); }
					}
				}
			}
			catch (DicomException e) {
				e.printStackTrace(System.err);
			}
			try {
				String patientSize = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.PatientSize);
				if (patientSize.length() == 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): no PatientSize in list");
					patientSize = eventDataFromImages.getPatientSize();
					if (patientSize != null && patientSize.length() > 0) {
//System.err.println("GenerateRadiationDoseStructuredReport.createContextForNewRadiationDoseStructuredReportFromExistingInstance(): found PatientSize in eventDataFromImages");
						{ Attribute a = new DecimalStringAttribute(TagFromName.PatientSize); a.addValue(patientSize); list.put(a); }
					}
				}
			}
			catch (DicomException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	public void removePatient() {
		for (AttributeTag t : patientModuleAttributeTags) { list.remove(t); }
	}
	
	public void removeStudy() {
		for (AttributeTag t : generalStudyModuleAttributeTags) { list.remove(t); }
		for (AttributeTag t : patientStudyModuleAttributeTags) { list.remove(t); }
	}
	
	public void removeSeries() {
		for (AttributeTag t : generalSeriesModuleAttributeTags) { list.remove(t); }
	}
	
	public void removeEquipment() {
		for (AttributeTag t : generalEquipmentModuleAttributeTags) { list.remove(t); }
	}
	
	public void removeFrameOfReference() {
		for (AttributeTag t : frameOfReferenceModuleAttributeTags) { list.remove(t); }
	}
	
	public void removeInstance() {
		for (AttributeTag t : sopCommonModuleAttributeTags) { list.remove(t); }
	}
	
	public void removeSRDocumentGeneral() {
		for (AttributeTag t : srDocumentGeneralModuleAttributeTags) { list.remove(t); }
	}
	
	public void put(Attribute a) {
		list.put(a);
	}
	
	public void putAll(AttributeList srcList) {
		list.putAll(srcList);
	}
	
	public String toString() {
		return list.toString();
	}

}


/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

public class CommonDoseObserverContext {

	RecordingDeviceObserverContext recordingDeviceObserverContext;
	DeviceParticipant deviceParticipant;
	PersonParticipant personParticipantAdministering;
	PersonParticipant personParticipantAuthorizing;

	public CommonDoseObserverContext(String uid,String name,String manufacturer,String modelName,String serialNumber,String location,
			String operatorName,String operatorID,String physicianName,String physicianID,String idIssuer,String organization
	) {
		recordingDeviceObserverContext = new RecordingDeviceObserverContext(uid,name,manufacturer,modelName,serialNumber,location);
		deviceParticipant = new DeviceParticipant(manufacturer,modelName,serialNumber);
		personParticipantAdministering = new PersonParticipant(operatorName, RoleInProcedure.IRRADIATION_ADMINISTERING,operatorID, idIssuer,organization,RoleInOrganization.TECHNOLOGIST);
		personParticipantAuthorizing   = new PersonParticipant(physicianName,RoleInProcedure.IRRADIATION_AUTHORIZING,  physicianID,idIssuer,organization,RoleInOrganization.PHYSICIAN);
	}
	
	public RecordingDeviceObserverContext getRecordingDeviceObserverContext() { return recordingDeviceObserverContext; }
	public DeviceParticipant getDeviceParticipant() { return deviceParticipant; }
	public PersonParticipant getPersonParticipantAdministering() { return personParticipantAdministering; }
	public PersonParticipant getPersonParticipantAuthorizing() { return personParticipantAuthorizing; }
	
}

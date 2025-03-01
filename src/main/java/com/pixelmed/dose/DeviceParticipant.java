/* Copyright (c) 2001-2010, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dose;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.ContentItem;
import com.pixelmed.dicom.ContentItemFactory;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.TagFromName;

import com.pixelmed.utils.HexDump;

import java.io.UnsupportedEncodingException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DeviceParticipant {

	protected String manufacturer;
	protected String modelName;
	protected String serialNumber;
	
	public DeviceParticipant(String manufacturer,String modelName,String serialNumber) {
		this.manufacturer = manufacturer;
		this.modelName = modelName;
		this.serialNumber = serialNumber;
	}

	public String getManufacturer() { return manufacturer; }
	public String getModelName() { return modelName; }
	public String getSerialNumber() { return serialNumber; }
	
	public ContentItem getStructuredReportFragment() throws DicomException {
		ContentItemFactory cif = new ContentItemFactory();
		ContentItem root = cif.new CodeContentItem(null,"CONTAINS",new CodedSequenceItem("113876","DCM","Device Role in Procedure"),new CodedSequenceItem("113859","DCM","Irradiating Device"));
		if (manufacturer != null && manufacturer.trim().length() > 0) { cif.new TextContentItem(root,"HAS PROPERTIES",new CodedSequenceItem("113878","DCM","Device Manufacturer"),manufacturer); }
		if (modelName != null && modelName.trim().length() > 0)       { cif.new TextContentItem(root,"HAS PROPERTIES",new CodedSequenceItem("113879","DCM","Device Model Name"),modelName); }
		if (serialNumber != null && serialNumber.trim().length() > 0) { cif.new TextContentItem(root,"HAS PROPERTIES",new CodedSequenceItem("113880","DCM","Device Serial Number"),serialNumber); }
		return root;
	}
	
	// static convenience methods
	
	/**
	 * <p>Extract the device serial number information from a list of attributes, or some suitable alternate if available.</p>
	 *
	 * <p>Makes a hash of StationName and Institution as an alternate, if either or both present and not empty.</p>
	 *
	 * @param	list						the list of attributes
	 * @param	insertAlternateBackInList	if true, when there is no DeviceSerialNumber or it is empty, add the alterate created back to the supplied list (side effect of call)
	 * @return								a string containing either the DeviceSerialNumber from the list or a suitable alternate if available, else null 
	 */
	public static String getDeviceSerialNumberOrSuitableAlternative(AttributeList list,boolean insertAlternateBackInList) {
		String useDeviceSerialNumber = Attribute.getSingleStringValueOrNull(list,TagFromName.DeviceSerialNumber);
		if (useDeviceSerialNumber == null || useDeviceSerialNumber.trim().length() == 0) {
			String institutionName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.InstitutionName);
			String stationName = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.StationName);
			if (institutionName.length() > 0 || stationName.length() > 0) {
				try {
					byte[] b = (institutionName+"|"+stationName).getBytes("UTF8");
					useDeviceSerialNumber = HexDump.byteArrayToHexString(MessageDigest.getInstance("SHA").digest(b));
					if (insertAlternateBackInList) {
						Attribute a = new LongStringAttribute(TagFromName.DeviceSerialNumber);
						a.addValue(useDeviceSerialNumber);
						list.put(a);
					}
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace(System.err);
					useDeviceSerialNumber = null;
				}
				catch (NoSuchAlgorithmException e) {
					e.printStackTrace(System.err);
					useDeviceSerialNumber = null;
				}
				catch (DicomException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		return useDeviceSerialNumber;
	}

}

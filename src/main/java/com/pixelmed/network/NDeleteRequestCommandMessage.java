package com.pixelmed.network;

import com.pixelmed.dicom.*;

import java.io.*;

/**
 * @author Nicola De Nisco
 */
class NDeleteRequestCommandMessage extends RequestCommandMessage
{
  private static final String identString =
     "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/NDeleteRequestCommandMessage.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";
  private byte bytes[];
  private static final AttributeTag groupLengthTag = new AttributeTag(0x0000, 0x0000);
  private int groupLength;
  private String requestedSOPClassUID;		// unpadded
  private int commandField;
  private int messageID;
  private int priority;
  private String requestedSOPInstanceUID;		// unpadded

  /**
   * @param	list
   * @exception	IOException
   * @exception	DicomException
   */
  public NDeleteRequestCommandMessage(AttributeList list)
     throws DicomException, IOException
  {
    groupLength = Attribute.getSingleIntegerValueOrDefault(list, groupLengthTag, 0xffff);
    requestedSOPClassUID = Attribute.getSingleStringValueOrNull(list, TagFromName.RequestedSOPClassUID);
    commandField = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.CommandField, 0xffff);
    messageID = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.MessageID, 0xffff);
    priority = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.Priority, 0xffff);
    requestedSOPInstanceUID = Attribute.getSingleStringValueOrNull(list, TagFromName.RequestedSOPInstanceUID);

    create();
  }

  /**
   * @param	requestedSOPClassUID
   * @param	requestedSOPInstanceUID
   * @param	moveOriginatorApplicationEntityTitle	the AET of the C-MOVE that originated this C-STORE, or null if none
   * @param	moveOriginatorMessageID					the MessageID of the C-MOVE that originated this C-STORE, or -1 if none
   * @exception	IOException
   * @exception	DicomException
   */
  public NDeleteRequestCommandMessage(String requestedSOPClassUID, String requestedSOPInstanceUID)
     throws DicomException, IOException
  {
    this.requestedSOPClassUID = requestedSOPClassUID;
    this.requestedSOPInstanceUID = requestedSOPInstanceUID;

    commandField = MessageServiceElementCommand.N_DELETE_RQ;
    messageID = super.getNextAvailableMessageID();
    priority = 0x0000;	// MEDIUM

    create();
  }

  public void create()
     throws DicomException, IOException
  {
    int dataSetType = 0x0101;	// N-DELETE-RQ non ha un dataset

    AttributeList list = new AttributeList();
    {
      AttributeTag t = groupLengthTag;
      Attribute a = new UnsignedLongAttribute(t);
      a.addValue(0);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.RequestedSOPClassUID;
      Attribute a = new UniqueIdentifierAttribute(t);
      a.addValue(requestedSOPClassUID);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.CommandField;
      Attribute a = new UnsignedShortAttribute(t);
      a.addValue(commandField);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.MessageID;
      Attribute a = new UnsignedShortAttribute(t);
      a.addValue(messageID);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.Priority;
      Attribute a = new UnsignedShortAttribute(t);
      a.addValue(priority);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.CommandDataSetType;
      Attribute a = new UnsignedShortAttribute(t);
      a.addValue(dataSetType);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.RequestedSOPInstanceUID;
      Attribute a = new UniqueIdentifierAttribute(t);
      a.addValue(requestedSOPInstanceUID);
      list.put(t, a);
    }

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    DicomOutputStream dout = new DicomOutputStream(bout,
       null/* no meta-header */, TransferSyntax.ImplicitVRLittleEndian);
    list.write(dout);
    bytes = bout.toByteArray();

    groupLength = bytes.length - 12;
    bytes[8] = (byte) groupLength;					// little endian
    bytes[9] = (byte) (groupLength >> 8);
    bytes[10] = (byte) (groupLength >> 16);
    bytes[11] = (byte) (groupLength >> 24);
//System.err.println("NDeleteRequestCommandMessage: bytes="+HexDump.dump(bytes));
  }

  /***/
  public int getGroupLength()
  {
    return groupLength;
  }

  /***/
  public String getAffectedSOPClassUID()
  {
    return requestedSOPClassUID;
  }		// unpadded

  /***/
  public int getCommandField()
  {
    return commandField;
  }

  /***/
  public int getMessageID()
  {
    return messageID;
  }

  /***/
  public int getPriority()
  {
    return priority;
  }

  /***/
  public String getAffectedSOPInstanceUID()
  {
    return requestedSOPInstanceUID;
  }		// unpadded

  /***/
  public byte[] getBytes()
  {
    return bytes;
  }
}

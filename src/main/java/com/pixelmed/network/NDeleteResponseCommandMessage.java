package com.pixelmed.network;

import com.pixelmed.dicom.*;

import java.io.*;

/**
 * @author Nicola De Nisco
 */
class NDeleteResponseCommandMessage implements CommandMessage
{
  private static final String identString =
     "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/network/NDeleteResponseCommandMessage.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";
  private byte bytes[];
  private static final AttributeTag groupLengthTag = new AttributeTag(0x0000, 0x0000);
  private int groupLength;
  private String affectedSOPClassUID;		// unpadded
  private int commandField;
  private int messageIDBeingRespondedTo;
  private int status;
  private String affectedSOPInstanceUID;		// unpadded

  /**
   * @param	list
   * @exception	IOException
   * @exception	DicomException
   */
  public NDeleteResponseCommandMessage(AttributeList list)
     throws DicomException, IOException
  {
    groupLength = Attribute.getSingleIntegerValueOrDefault(list, groupLengthTag, 0xffff);
    affectedSOPClassUID = Attribute.getSingleStringValueOrNull(list, TagFromName.AffectedSOPClassUID);
    commandField = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.CommandField, 0xffff);
    messageIDBeingRespondedTo =
       Attribute.getSingleIntegerValueOrDefault(list, TagFromName.MessageIDBeingRespondedTo, 0xffff);
    status = Attribute.getSingleIntegerValueOrDefault(list, TagFromName.Status, 0xffff);
    affectedSOPInstanceUID = Attribute.getSingleStringValueOrNull(list, TagFromName.AffectedSOPInstanceUID);
    create();
  }

  /**
   * @param	affectedSOPClassUID
   * @param	affectedSOPInstanceUID
   * @param	messageIDBeingRespondedTo
   * @param	status
   * @exception	IOException
   * @exception	DicomException
   */
  public NDeleteResponseCommandMessage(String affectedSOPClassUID, String affectedSOPInstanceUID,
     int messageIDBeingRespondedTo, int status)
     throws DicomException, IOException
  {
    this.affectedSOPClassUID = affectedSOPClassUID;
    this.affectedSOPInstanceUID = affectedSOPInstanceUID;
    this.messageIDBeingRespondedTo = messageIDBeingRespondedTo;
    this.status = status;

    commandField = MessageServiceElementCommand.N_DELETE_RSP;
    create();
  }

  public void create()
     throws DicomException, IOException
  {
    int dataSetType = 0x0101;	// no data set

    AttributeList list = new AttributeList();
    {
      AttributeTag t = groupLengthTag;
      Attribute a = new UnsignedLongAttribute(t);
      a.addValue(0);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.AffectedSOPClassUID;
      Attribute a = new UniqueIdentifierAttribute(t);
      a.addValue(affectedSOPClassUID);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.CommandField;
      Attribute a = new UnsignedShortAttribute(t);
      a.addValue(commandField);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.MessageIDBeingRespondedTo;
      Attribute a = new UnsignedShortAttribute(t);
      a.addValue(messageIDBeingRespondedTo);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.CommandDataSetType;
      Attribute a = new UnsignedShortAttribute(t);
      a.addValue(dataSetType);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.Status;
      Attribute a = new UnsignedShortAttribute(t);
      a.addValue(status);
      list.put(t, a);
    }
    {
      AttributeTag t = TagFromName.AffectedSOPInstanceUID;
      Attribute a = new UniqueIdentifierAttribute(t);
      a.addValue(affectedSOPInstanceUID);
      list.put(t, a);
    }

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    DicomOutputStream dout =
       new DicomOutputStream(bout, null/* no meta-header */, TransferSyntax.ImplicitVRLittleEndian);
    list.write(dout);
    bytes = bout.toByteArray();

    groupLength = bytes.length - 12;
    bytes[8] = (byte) groupLength;					// little endian
    bytes[9] = (byte) (groupLength >> 8);
    bytes[10] = (byte) (groupLength >> 16);
    bytes[11] = (byte) (groupLength >> 24);
//System.err.println("NDeleteResponseCommandMessage: bytes="+HexDump.dump(bytes));
  }

  /***/
  public int getGroupLength()
  {
    return groupLength;
  }

  /***/
  public String getAffectedSOPClassUID()
  {
    return affectedSOPClassUID;
  }			// unpadded

  /***/
  public int getCommandField()
  {
    return commandField;
  }

  /***/
  public int getMessageIDBeingRespondedTo()
  {
    return messageIDBeingRespondedTo;
  }

  /***/
  public int getStatus()
  {
    return status;
  }

  /***/
  public String getAffectedSOPInstanceUID()
  {
    return affectedSOPInstanceUID;
  }		// unpadded

  /***/
  public byte[] getBytes()
  {
    return bytes;
  }
}

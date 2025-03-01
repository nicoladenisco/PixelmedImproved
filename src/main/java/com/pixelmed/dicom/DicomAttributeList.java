package com.pixelmed.dicom;

import com.pixelmed.utils.StringUtilities;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A simple helper for AttributeList early usage.
 *
 * @author Nicola De Nisco
 */
public class DicomAttributeList extends AttributeList
{
  public final DateFormat dfDicom = new SimpleDateFormat("yyyyMMdd");
  public final DateFormat tfDicom = new SimpleDateFormat("HHmmss");

  public void putValue(AttributeTag tag, String value)
     throws Exception
  {
    Attribute a = AttributeFactory.newAttribute(tag);
    put(tag, a);
    if((value = StringUtilities.okStr(value, null)) != null)
      a.setValue(value);
  }

  public void putValue(AttributeTag tag, long value)
     throws Exception
  {
    Attribute a = AttributeFactory.newAttribute(tag);
    put(tag, a);
    a.setValue(value);
  }

  public void putValue(AttributeTag tag, double value)
     throws Exception
  {
    Attribute a = AttributeFactory.newAttribute(tag);
    put(tag, a);
    a.setValue(value);
  }

  public void putValue(AttributeTag tag, Date value)
     throws Exception
  {
    Attribute a = AttributeFactory.newAttribute(tag);
    put(tag, a);
    if(value != null)
    {
      switch(a.getVRAsString())
      {
        case "DA":
        case "DT":
          a.setValue(dfDicom.format(value));
          break;

        case "TM":
          a.setValue(tfDicom.format(value));
          break;
      }
    }
  }

  public String getStringValue(AttributeTag atag)
  {
    return getStringValue(atag, null);
  }

  public String getStringValue(AttributeTag atag, String defval)
  {
    Attribute at = get(atag);
    return at == null ? defval : at.getDelimitedStringValuesOrDefault(defval);
  }

  public String getStringValue(String stag, String defval)
     throws DicomException
  {
    return getStringValue(getTagFromString(stag), defval);
  }

  public Date getDateValue(AttributeTag atag)
  {
    return getDateValue(atag, null);
  }

  public Date getDateValue(AttributeTag atag, Date defval)
  {
    String s;
    Attribute at = get(atag);
    if(at == null)
      return defval;

    try
    {
      if((s = at.getSingleStringValueOrNull()) == null || s.trim().length() < 8)
        return defval;

      return dfDicom.parse(s.trim().substring(0, 8));
    }
    catch(ParseException ex)
    {
      return defval;
    }
  }

  public Date getDateValue(String stag, Date defval)
     throws DicomException
  {
    return getDateValue(getTagFromString(stag), defval);
  }

  public Date getTimeValue(AttributeTag atag)
  {
    return getTimeValue(atag, null);
  }

  public Date getTimeValue(AttributeTag atag, Date defval)
  {
    String s;
    Attribute at = get(atag);
    if(at == null)
      return defval;

    try
    {
      if((s = at.getSingleStringValueOrNull()) == null || s.trim().length() < 6)
        return defval;

      return tfDicom.parse(s.trim().substring(0, 6));
    }
    catch(ParseException ex)
    {
      return defval;
    }
  }

  public Date getTimeValue(String stag, Date defval)
     throws DicomException
  {
    return getTimeValue(getTagFromString(stag), defval);
  }

  public Date getDateTimeValue(AttributeTag dtag, AttributeTag ttag)
  {
    return getDateTimeValue(dtag, ttag, null);
  }

  public Date getDateTimeValue(AttributeTag dtag, AttributeTag ttag, Date defval)
  {
    Date d = getDateValue(dtag, null);
    if(d == null)
      return defval;

    Date t = getTimeValue(ttag, null);
    if(t == null)
      return d;

    return StringUtilities.mergeDateTime(d, t);
  }

  public Date getDateTimeValue(String dtag, String ttag, Date defval)
     throws DicomException
  {
    return getDateTimeValue(getTagFromString(dtag), getTagFromString(ttag), defval);
  }

  public Date getPatientBirth()
  {
    return getDateTimeValue(TagFromName.PatientBirthDate, TagFromName.PatientBirthTime);
  }

  public Date getStudyDate()
  {
    return getDateTimeValue(TagFromName.StudyDate, TagFromName.StudyTime);
  }

  public Date getSeriesDate()
  {
    return getDateTimeValue(TagFromName.SeriesDate, TagFromName.SeriesTime);
  }

  public Date getAcquisitionDate()
  {
    return getDateTimeValue(TagFromName.AcquisitionDate, TagFromName.AcquisitionTime);
  }

  public int getIntValue(AttributeTag atag)
  {
    return (int) getLongValue(atag, 0L);
  }

  public int getIntValue(AttributeTag atag, int defval)
  {
    return (int) getLongValue(atag, defval);
  }

  public int geIntValue(String stag, int defval)
     throws DicomException
  {
    return getIntValue(getTagFromString(stag), defval);
  }

  public long getLongValue(AttributeTag atag)
  {
    return getLongValue(atag, 0L);
  }

  public long getLongValue(AttributeTag atag, long defval)
  {
    Attribute at = get(atag);
    return at == null ? defval : at.getSingleLongValueOrDefault(defval);
  }

  public long getLongValue(String atag, long defval)
     throws DicomException
  {
    return getLongValue(getTagFromString(atag), defval);
  }

  public double getDoubleValue(AttributeTag atag)
  {
    return getDoubleValue(atag, 0.0);
  }

  public double getDoubleValue(AttributeTag atag, double defval)
  {
    Attribute at = get(atag);
    return at == null ? defval : at.getSingleDoubleValueOrDefault(defval);
  }

  public double getDoubleValue(String atag, double defval)
     throws DicomException
  {
    return getDoubleValue(getTagFromString(atag), defval);
  }

  /**
   * Ritorna il tag corrispondente alla stringa.
   * La stringa può essere il nome del tag (PatientName) oppure la rappresentazione esa (0x0010, 0x0010).
   * @param stag stringa tag
   * @return tag corrispondente
   * @throws com.pixelmed.dicom.DicomException in caso di stringa non riconosciuta
   */
  public AttributeTag getTagFromString(String stag)
     throws DicomException
  {
    AttributeTag tag = getDictionary().getTagFromName(stag);
    if(tag == null)
      tag = new AttributeTag(stag);
    return tag;
  }
}

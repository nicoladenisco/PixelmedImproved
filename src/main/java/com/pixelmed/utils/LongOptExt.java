/*
 *  Copyright (C) 2011 WinSOFT di Nicola De Nisco
 *
 *  Questo software è proprietà di Nicola De Nisco.
 *  I termini di ridistribuzione possono variare in base
 *  al tipo di contratto in essere fra Nicola De Nisco e
 *  il fruitore dello stesso.
 *
 *  Fare riferimento alla documentazione associata al contratto
 *  di committenza per ulteriori dettagli.
 */
package com.pixelmed.utils;

import gnu.getopt.LongOpt;

/**
 * Opzione da linea di comando con visualizzazione automatica dell'help.
 *
 * @author Nicola De Nisco
 */
public class LongOptExt extends LongOpt
{
  protected String helpMsg = null;

  public LongOptExt(String name, int has_arg, StringBuffer flag, int val, String helpMsg)
     throws IllegalArgumentException
  {
    super(name, has_arg, flag, val);
    this.helpMsg = helpMsg;
  }

  public String getHelpMsg()
  {
    String sArg = "";
    switch(getHasArg())
    {
      case NO_ARGUMENT:
        sArg = "";
        break;
      case OPTIONAL_ARGUMENT:
        sArg = " [val]";
        break;
      case REQUIRED_ARGUMENT:
        sArg = " <val>";
        break;
    }

    return String.format("  -%c --%-25.25s %s",
       getVal(), getName() + sArg, helpMsg);
  }

  public static String getOptstring(LongOpt[] opts)
  {
    String rv = "";
    for(LongOpt l : opts)
    {
      rv += (char) l.getVal();
      switch(l.getHasArg())
      {
        case OPTIONAL_ARGUMENT:
          rv += ';';
          break;
        case REQUIRED_ARGUMENT:
          rv += ':';
          break;
      }
    }
    return rv;
  }
}

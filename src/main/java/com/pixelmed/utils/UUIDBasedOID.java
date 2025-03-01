/* Copyright (c) 2001-2009, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.utils;

import java.math.BigInteger;
import java.util.UUID;

import java.util.HashSet;		// for main() testing for uniqueness

/**
 * <p>A class for creating and convertin UUID based OIDs.</p>
 *
 * <p>See <a href="http://www.itu.int/ITU-T/studygroups/com17/oid/X.667-E.pdf">ITU X.667 Information technology - Open Systems Interconnection - Procedures for the operation of OSI Registration Authorities: Generation and registration of Universally Unique Identifiers (UUIDs) and their use as ASN.1 Object Identifier components</a>.</p>
 *
 * @author	dclunie
 */

public class UUIDBasedOID {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/utils/UUIDBasedOID.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	protected static final String OID_PREFIX = "2.25";	// {joint-iso-itu-t uuid(25) <uuid-single-integer-value>}
	protected static final String OID_PREFIX_REMOVAL_REGEX = "^"+OID_PREFIX+".";
	
	protected UUID uuid;
	protected String oid;
	
	/**
	 * <p>Construct a new OID with a new random UUID.</p>
	 */
	public UUIDBasedOID() {
		uuid = UUID.randomUUID();
		oid = createOIDFromUUIDCanonicalHexString(uuid.toString());
	}
	
	/**
	 * <p>Construct an OID from an existing string representation of an OID.</p>
	 *
	 * @param		oid	a String of dotted numeric values in OID form {joint-iso-itu-t uuid(25) <uuid-single-integer-value>} 
	 */
	public UUIDBasedOID(String oid) throws IllegalArgumentException, NumberFormatException {
		this.oid = oid;
		uuid = parseUUIDFromOID(oid);
	}
	
	/**
	 * <p>Get the string representation of the OID.</p>
	 *
	 * @return	the string representation of the OID
	 */
	public String getOID() { return oid; }
	
	/**
	 * <p>Get the UUID of the OID.</p>
	 *
	 * @return	the UUID
	 */
	public UUID getUUID() { return uuid; }
	
	/**
	 * <p>Extract the UUID from a UUID-based OID.</p>
	 *
	 * @param		oid							a String of dotted numeric values in OID form {joint-iso-itu-t uuid(25) <uuid-single-integer-value>} 
	 * @return									the UUID
	 * @exception	IllegalArgumentException	if the OID is not in the {joint-iso-itu-t uuid(25)} arc
	 * @exception	NumberFormatException		if the OID does not contain a uuid-single-integer-value
	 */
	public static UUID parseUUIDFromOID(String oid) throws IllegalArgumentException, NumberFormatException {
		if (oid == null || ! oid.startsWith(OID_PREFIX)) {
			throw new IllegalArgumentException("OID "+oid+" does not start with "+OID_PREFIX);
		}
		String decimalString = oid.replaceFirst(OID_PREFIX_REMOVAL_REGEX,"");
		return parseUUIDFromDecimalString(decimalString);
	}
	
	/**
	 * <p>Extract the UUID from its single integer value decimal string representation.</p>
	 *
	 * @param		decimalString				single integer value decimal string representation 
	 * @return									the UUID
	 * @exception	NumberFormatException		if the OID does not contain a uuid-single-integer-value
	 */
	public static UUID parseUUIDFromDecimalString(String decimalString) throws NumberFormatException {
		BigInteger decimalValue = new BigInteger(decimalString);
		long leastSignificantBits = decimalValue.longValue();
		long mostSignificantBits  = decimalValue.shiftRight(64).longValue();
		return new UUID(mostSignificantBits,leastSignificantBits);
	}
	
	/**
	 * <p>Convert an unsigned value in a long to a BigInteger.</p>
	 *
	 * @param		unsignedLongValue			an unsigned long value (i.e., the sign bit is treated as part of the value rather than a sign) 
	 * @return									the BigInteger
	 */
	public static BigInteger makeBigIntegerFromUnsignedLong(long unsignedLongValue) {
//System.err.println("makeBigIntegerFromUnsignedLong(): unsignedLongValue = "+Long.toHexString(unsignedLongValue));
		BigInteger bigValue;
		if (unsignedLongValue < 0) {
			unsignedLongValue = unsignedLongValue & Long.MAX_VALUE;
			bigValue = BigInteger.valueOf(unsignedLongValue);
			bigValue = bigValue.setBit(63);
		}
		else {
			bigValue = BigInteger.valueOf(unsignedLongValue);
		}
//System.err.println("makeBigIntegerFromUnsignedLong(): bigValue = "+com.pixelmed.utils.HexDump.dump(bigValue.toByteArray()));
		return bigValue;
	}
	
	/**
	 * <p>Create an OID from the canonical hex string form of a UUID.</p>
	 *
	 * @param		hexString					canonical hex string form of a UUID 
	 * @return									the OID
	 * @exception	IllegalArgumentException	if name does not conform to the string representation
	 */
	public static String createOIDFromUUIDCanonicalHexString(String hexString) throws IllegalArgumentException {
		UUID uuid = UUID.fromString(hexString);
		long leastSignificantBits = uuid.getLeastSignificantBits();
		long mostSignificantBits  = uuid.getMostSignificantBits();
		BigInteger decimalValue = makeBigIntegerFromUnsignedLong(mostSignificantBits);
		decimalValue = decimalValue.shiftLeft(64);
		BigInteger bigValueOfLeastSignificantBits = makeBigIntegerFromUnsignedLong(leastSignificantBits);
		decimalValue = decimalValue.or(bigValueOfLeastSignificantBits);	// not add() ... do not want to introduce question of signedness of long
		return OID_PREFIX+"."+decimalValue.toString();
	}
	
	/**
	 * <p>Test UUID to OID conversions.</p>
	 *
	 */
	public static final void main(String arg[]) {
		try {
			int count = Integer.parseInt(arg[0]);

			String testOIDString = "2.25.329800735698586629295641978511506172918";
			UUID uuidFromOID = parseUUIDFromOID(testOIDString);
			System.err.println(uuidFromOID);	// "f81d4fae-7dec-11d0-a765-00a0c91e6bf6" "http://www.digipedia.pl/man/uuid.3ossp.html"
			UUID uuidFromCanonicalHexString = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
			System.err.println("Roundtrip from string parseUUIDFromOID are equal "+(uuidFromOID.equals(uuidFromCanonicalHexString)));
			
			UUIDBasedOID oid = new UUIDBasedOID(testOIDString);
			uuidFromOID = oid.getUUID();
			System.err.println(uuidFromOID);
			System.err.println("Roundtrip from string constructor of UUID are equal "+(uuidFromOID.equals(uuidFromCanonicalHexString)));
			String oidString = oid.getOID();
			System.err.println("Roundtrip from string constructor of OID are equal "+(oidString.equals(testOIDString)));

			System.err.println("Want "+testOIDString);
			String oidFromCanonicalHexString = createOIDFromUUIDCanonicalHexString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
			System.err.println(oidFromCanonicalHexString);
			System.err.println("Are equal "+(oidFromCanonicalHexString.equals(testOIDString)));
			
			String testUUIDString = "0b7827e3-35f0-46c0-a2e9-eaf4dadc899b";
			oidFromCanonicalHexString = createOIDFromUUIDCanonicalHexString(testUUIDString);
			System.err.println(oidFromCanonicalHexString);
			String uuidString = parseUUIDFromOID(oidFromCanonicalHexString).toString();
			System.err.println(uuidString);
			System.err.println("Are equal "+(uuidString.equals(testUUIDString)));

			// Check are all unique
			boolean uniquenessCheck = true;
			boolean lengthCheck = true;
			HashSet set = new HashSet();
			for (int i=0; i<count; ++i) {
				oid = new UUIDBasedOID();
				//uuidFromOID = oid.getUUID();
				//System.err.println(uuidFromOID);
				oidString = oid.getOID();
				int length = oidString.length();
				//System.err.println(oidString+" (length = "+length+")");
				if (set.contains(oidString)) {
					System.err.println("Error - not unique - \""+oidString+"\"");
					uniquenessCheck = false;
				}
				if (length > 64) {
					System.err.println("Error - too long - \""+oidString+"\" (length = "+length+")");
					lengthCheck = false;
				}
			}
			System.err.println("Uniqueness check "+(uniquenessCheck ? "passes" : "fails"));
			System.err.println("Length check "+(lengthCheck ? "passes" : "fails"));
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
}



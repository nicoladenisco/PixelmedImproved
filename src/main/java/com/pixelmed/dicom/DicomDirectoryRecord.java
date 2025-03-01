/* Copyright (c) 2001-2008, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.util.*;
import javax.swing.tree.*;

/**
 * @author	dclunie
 */
public abstract class DicomDirectoryRecord implements Comparable, TreeNode {

	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/dicom/DicomDirectoryRecord.java,v 1.1.1.1 2011-05-21 10:08:46 nicola Exp $";

	DicomDirectoryRecord parent;
	Collection children;
	TreeNode[] array;
	AttributeList list;
	
	protected String uid;
	protected String stringValue;
	protected int integerValue;

	// Methods to implement Comparable (allows parent to sort)

	/**
	 * @param	o
	 */
	public int compareTo(Object o) {
		return 0;	// no particular order unless class is specialized
	}

	/**
	 * @param	o
	 */
	public boolean equals(Object o) {
		return compareTo(o) == 0;
	}

	// Methods to help with Comparable support
	
	/***/
	abstract protected void makeStringValue();

	/***/
	abstract protected void makeIntegerValue();

	/***/
	protected String getStringValue() {
		return stringValue;
	}

	/***/
	protected int getIntegerValue() {
		return integerValue;
	}

	/***/
	protected final String getUIDForComparison() { return uid; }

	/**
	 * @param	record
	 */
	protected final int compareToByStringValue(DicomDirectoryRecord record) {
		if (this.getClass().equals(record.getClass())) {
			int uidComparison = getUIDForComparison().compareTo(record.getUIDForComparison());
			//if (uidComparison == 0) {
			//	return 0;				// always equal if same UID
			///}
			//else {
			{
				int strComparison = toString().compareTo(record.toString());
				if (strComparison == 0) {
					return uidComparison;	// same string but different UID; distinguish and order consistently
				}
				else {
					return strComparison;
				}
			}
		}
		else {
			return toString().compareTo(record.toString());	// includes name of record type
		}
	}

	/**
	 * @param	record
	 */
	protected final int compareToByIntegerValue(DicomDirectoryRecord record) {
//System.err.println("DicomDirectoryRecord.compareToByIntegerValue(): comparing classes "+this.getClass()+" with "+record.getClass());
		if (this.getClass().equals(record.getClass())) {
//System.err.println("DicomDirectoryRecord.compareToByIntegerValue(): comparing UIDs "+getUIDForComparison()+" with "+record.getUIDForComparison());
			int uidComparison = getUIDForComparison().compareTo(record.getUIDForComparison());
			if (uidComparison == 0) {
				return 0;				// always equal if same UID
			}
			else {
				int intComparison = getIntegerValue() - record.getIntegerValue();
				if (intComparison == 0) {
					int strComparison = toString().compareTo(record.toString());
					if (strComparison == 0) {
						return uidComparison;	// same integer & string values but different UID; distinguish and order consistently
					}
					else {
						return strComparison;	// same integer values but different string; distinguish and order consistently
					}
				}
				else {
					return intComparison;
				}
			}
		}
		else {
			return toString().compareTo(record.toString());	// includes name of record type
		}
	}

	// Methods to implement TreeNode ...

	/**
	 * <p>Returns the parent node of this node.</p>
	 *
	 * @return	the parent node, or null if the root
	 */
	public TreeNode getParent() {
		return parent;
	}

	/**
	 * <p>Returns the child at the specified index.</p>
	 *
	 * @param	index	the index of the child to be returned, numbered from 0
	 * @return		the child <code>TreeNode</code> at the specified index
	 */
	public TreeNode getChildAt(int index) {
		int n=children.size();
		if (array == null) {
			array=(TreeNode[])(children.toArray(new TreeNode[n]));	// explicitly allocated to set returned array type correctly
		}
		return index < n ? array[index] : null;
	}

	/**
	 * <p>Returns the index of the specified child from amongst this node's children, if present.</p>
	 *
	 * @param	child	the child to search for amongst this node's children
	 * @return		the index of the child, or -1 if not present
	 */
	public int getIndex(TreeNode child) {
//System.err.println("getIndexOfChild: looking for "+child);
		if (children != null) {
			int n=children.size();
			if (array == null) {
				array=(TreeNode[])(children.toArray(new TreeNode[n]));	// explicitly allocated to set returned array type correctly
			}
			for (int i=0; i<n; ++i) {
				if (getChildAt(i).equals(child)) {	// expensive comparison ? :(
//System.err.println("getIndexOfChild: found "+child);
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * <p> Always returns true, since children may always be added.</p>
	 *
	 * @return	always true
	 */
	public boolean getAllowsChildren() {
		return true;
	}

	/**
	 * <p> Returns true if the receiver is a leaf (has no children).</p>
	 *
	 * @return	true if the receiver is a leaf
	 */
	public boolean isLeaf() {
		return getChildCount() == 0;
	}

	/**
	 * <p>Return the number of children that this node contains.</p>
	 *
	 * @return	the number of children, 0 if none
	 */
	public int getChildCount() {
		return children == null ? 0 : children.size();
	}

	/**
	 * <p>Returns the children of this node as an {@link java.util.Enumeration Enumeration}.</p>
	 *
	 * @return	the children of this node
	 */
	public Enumeration children() {
		return children == null ? null : new Vector(children).elements();
	}

	// Methods specific to this kind of node ...

	/**
	 * @param	p
	 * @param	l
	 */
	public DicomDirectoryRecord(DicomDirectoryRecord p,AttributeList l) {
		parent=p;
		list=l;
		makeIntegerValue();
		makeStringValue();
	}

	/**
	 * @param	child
	 */
	public void addChild(DicomDirectoryRecord child) {
		if (children == null) children=new TreeSet();	// is sorted
		children.add(child);
		array=null;					// cache is dirty
	}

	/**
	 * @param	child
	 */
	public void removeChild(DicomDirectoryRecord child) {
		children.remove(child);
		array=null;					// cache is dirty
	}

	/**
	 * @param	sibling
	 * @exception	DicomException
	 */
	public void addSibling(DicomDirectoryRecord sibling) throws DicomException {
		if (parent == null) {
			throw new DicomException("Internal error - root node with sibling");
		}
		else {
			parent.addChild(sibling);
		}
	}

	/**
	 * <p>Set the parent node of this node.</p>
	 *
	 * @param	parent
	 */
	public void setParent(DicomDirectoryRecord parent) {
		this.parent = parent;
	}

	/***/
	public AttributeList getAttributeList() { return list; }
}




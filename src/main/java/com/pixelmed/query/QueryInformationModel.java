/* Copyright (c) 2001-2007, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.query;

import com.pixelmed.dicom.*;
import com.pixelmed.network.*;

import java.io.*;
import java.util.*;

/**
 * <p>The {@link com.pixelmed.query.QueryInformationModel QueryInformationModel} class is an abstract class that contains the core
 * functionality for performing DICOM query and retrieval over the network.</p>
 *
 * <p>It hides the underlying DICOM network implementation.</p>
 *
 * <p>Concrete sub-classes implement the behavior for specific query models, such as
 * {@link com.pixelmed.query.StudyRootQueryInformationModel StudyRootQueryInformationModel},
 * the description of which contains an exampleof building an identifier and performing
 * a query.</p>
 *
 * <p>The majority of methods are protected and are for the benefit of those implementing their
 * own query models as concrete sub-classes. The public methods of primary interest to application
 * builders are:</p>
 * <ul>
 * <li> {@link #QueryInformationModel(String,int,String,String,int) QueryInformationModel()}
 * <li> {@link #performHierarchicalQuery(AttributeList) performHierarchicalQuery()}
 * <li> {@link #performHierarchicalMove(AttributeList) performHierarchicalMove()}
 * <li> {@link #performHierarchicalMoveFrom(AttributeList,String) performHierarchicalMoveFrom()}
 * <li> {@link #performHierarchicalMoveTo(AttributeList,String) performHierarchicalMoveTo()}
 * <li> {@link #performHierarchicalMoveFromTo(AttributeList,String,String) performHierarchicalMoveFromTo()}
 * </ul>
 *
 * @see com.pixelmed.query.StudyRootQueryInformationModel
 *
 * @author	dclunie
 */
abstract public class QueryInformationModel {

	/***/
	private static final String identString = "@(#) $Header: /var/cvs-rep/pmedim/src/com/pixelmed/query/QueryInformationModel.java,v 1.1.1.1 2011-05-21 10:08:47 nicola Exp $";

	/***/
	private String hostname;
	/***/
	private int port;
	/***/
	private String calledAETitle;
	/***/
	private String callingAETitle;
	/***/
	protected int debugLevel;
	
	/***/
	public final String getCalledAETitle() { return calledAETitle; }

	/***/
	protected abstract InformationEntity getRoot();
	/**
	 * @param	ie
	 */
	protected abstract InformationEntity getChildTypeForParent(InformationEntity ie);
	/**
	 * @param	ie
	 */
	protected abstract HashSet getAllInformationEntitiesToIncludeAtThisQueryLevel(InformationEntity ie);
	/***/
	protected abstract String getFindSOPClassUID();
	/***/
	protected abstract String getMoveSOPClassUID();
	/**
	 * @param	ie
	 * @param	responseIdentifier
	 */
	protected abstract String getStringValueForTreeFromResponseIdentifier(InformationEntity ie,AttributeList responseIdentifier);

	/**
	 * @param	ie
	 */
	public String getQueryLevelName(InformationEntity ie) {
		if      (ie == InformationEntity.PATIENT)       return "PATIENT";
		else if (ie == InformationEntity.STUDY)         return "STUDY";
		else if (ie == InformationEntity.SERIES)        return "SERIES";
		else if (ie == InformationEntity.INSTANCE)      return "IMAGE";
		else return null;
	}
	
	/**
	 * @param	ie
	 */
	public AttributeTag getUniqueKeyForInformationEntity(InformationEntity ie) {
		if      (ie == InformationEntity.PATIENT)       return TagFromName.PatientID;
		else if (ie == InformationEntity.STUDY)         return TagFromName.StudyInstanceUID;
		else if (ie == InformationEntity.SERIES)        return TagFromName.SeriesInstanceUID;
		else if (ie == InformationEntity.INSTANCE)      return TagFromName.SOPInstanceUID;
		else return null;
	}
	
	/**
	 * @param	ie
	 */
	protected AttributeTag getAttributeTagOfCountOfChildren(InformationEntity ie) {
		if      (ie == InformationEntity.PATIENT)       return TagFromName.NumberOfPatientRelatedStudies;
		else if (ie == InformationEntity.STUDY)         return TagFromName.NumberOfStudyRelatedSeries;
		else if (ie == InformationEntity.SERIES)        return TagFromName.NumberOfSeriesRelatedInstances;
		else return null;
	}
	
	/**
	 * @param	oldList
	 * @param	parentUniqueKeys
	 * @param	queryLevel
	 * @exception	DicomException
	 */
	private AttributeList makeIdentifierFromAttributesAtThisQueryLevel(AttributeList oldList,AttributeList parentUniqueKeys,InformationEntity queryLevel) throws DicomException {
if (debugLevel > 1) System.err.println("makeIdentifierFromAttributesAtThisQueryLevel: queryLevel="+queryLevel);
		HashSet allInformationEntitiesToIncludeAtThisQueryLevel = getAllInformationEntitiesToIncludeAtThisQueryLevel(queryLevel);
//System.err.println("makeIdentifierFromAttributesAtThisQueryLevel: allInformationEntitiesToIncludeAtThisQueryLevel="+allInformationEntitiesToIncludeAtThisQueryLevel);
		DicomDictionary dictionary = oldList.getDictionary();
		AttributeList newList = new AttributeList();
		Iterator i = oldList.values().iterator();
		while (i.hasNext()) {
			Attribute a = (Attribute)i.next();
			AttributeTag tag = a.getTag();
			InformationEntity ie = dictionary.getInformationEntityFromTag(tag);
//System.err.println("makeIdentifierFromAttributesAtThisQueryLevel: checking "+a);
			if (tag.equals(TagFromName.SpecificCharacterSet)
			 || (ie != null && allInformationEntitiesToIncludeAtThisQueryLevel.contains(ie))
			) {
//System.err.println("makeIdentifierFromAttributesAtThisQueryLevel: adding "+a);
				newList.put(tag,a);
			}
		}
		{ AttributeTag t = TagFromName.QueryRetrieveLevel; Attribute a = new CodeStringAttribute(t); a.addValue(getQueryLevelName(queryLevel)); newList.put(t,a); }
		if (parentUniqueKeys!= null) newList.putAll(parentUniqueKeys);
		AttributeTag uniqueKeyTagFromThisLevel = getUniqueKeyForInformationEntity(queryLevel);
		if (!newList.containsKey(uniqueKeyTagFromThisLevel)) {
			byte[] vr = dictionary.getValueRepresentationFromTag(uniqueKeyTagFromThisLevel);
			if (vr != null) {
				if (ValueRepresentation.isShortStringVR(vr)) {
					newList.put(uniqueKeyTagFromThisLevel,new ShortStringAttribute(uniqueKeyTagFromThisLevel,null));
				}
				else if (ValueRepresentation.isUniqueIdentifierVR(vr)) {
					newList.put(uniqueKeyTagFromThisLevel,new UniqueIdentifierAttribute(uniqueKeyTagFromThisLevel));
				}
				else {
					throw new DicomException("Internal error: cannot get suitable VR for unique key in query "+uniqueKeyTagFromThisLevel);
				}
			}
		}
if (debugLevel > 1) System.err.println("makeIdentifierFromAttributesAtThisQueryLevel: identifier="+newList);
		return newList;
	}
	/***/
	public String toString() {
		return	"host="+hostname
			+" port="+port
			+" calledAETitle="+calledAETitle
			+" callingAETitle="+callingAETitle;
	}

	
	/**
	 * <p>Perform a single level query and return the response to the specified handler.</p>
	 *
	 * @param       filter                  	the query request identifier as a list of DICOM attributes
	 * @param       parentUniqueKeys        	the unique keys of the parents of this level
	 * @param       queryLevel              	the level of the query
	 * @param       responseIdentifierHandler	the tree to add the response results to
	 * @exception   IOException			thrown if there is an generic IO problem
	 * @exception   DicomException          	thrown if there is a problem performing or parsing the query
	 * @exception   DicomNetworkException   	thrown if there is a problem with the DICOM network protocol
	 */
	void performQuery(AttributeList filter,AttributeList parentUniqueKeys,InformationEntity queryLevel,IdentifierHandler responseIdentifierHandler) throws IOException, DicomException, DicomNetworkException {
if (debugLevel > 0) System.err.println("QueryInformationModel.performQuery(): queryLevel="+queryLevel);
if (debugLevel > 0) System.err.println("QueryInformationModel.performQuery(): parentUniqueKeys="+parentUniqueKeys);
		AttributeList requestIdentifier = makeIdentifierFromAttributesAtThisQueryLevel(filter,parentUniqueKeys,queryLevel);
if (debugLevel > 1) System.err.println("QueryInformationModel.performQuery(): requestIdentifier="+requestIdentifier);
		new FindSOPClassSCU(hostname,port,calledAETitle,callingAETitle,getFindSOPClassUID(),requestIdentifier,responseIdentifierHandler,debugLevel);
	}

	/**
	 * <p>Perform a hierarchical query and return the response as a tree.</p>
	 *
	 * <p>Performs a query recursively from the requested level of the information model
	 * down to the lowest level of the query model, using the matching keys present in the
	 * supplied identifier (filter) (if any), requesting the return keys listed in the
	 * request identifier.</p>
	 *
	 * <p>It starts out at the highest level of the model, and for each response returned
	 * at that level, uses each unique key returned at that level of the response to perform
	 * another query at the next level down, and so on, recursively.</p>
	 *
	 * <p>The actual queries at lower levels may be deferred and not performed until
	 * the tree is actually expanded whilst browsing, to avoid delays in making the top level nodes
	 * available.</p>
	 *
	 * <p>If the filter contains the attributes to count the number of subsidiary entities (e.g.,
	 * NumberOfStudyRelatedSeries), then an immediate subsidiary query to determine the presence
	 * and number of a node's children will be avoided, or at least deferred until that node
	 * is expanded when browsing.</p>
	 *
	 * @param	filter			the query request identifier as a list of DICOM attributes
	 * @return				the results of query as a tree suitable for browing
	 * @exception	IOException		thrown if there is an generic IO problem
	 * @exception	DicomException		thrown if there is a problem performing or parsing the query
	 * @exception	DicomNetworkException	thrown if there is a problem with the DICOM network protocol
	 */
	public QueryTreeModel performHierarchicalQuery(AttributeList filter) throws IOException, DicomException, DicomNetworkException {
		return new QueryTreeModel(this,filter,debugLevel);
	}
	
	/**
	 * <p>Retrieve DICOM object(s).</p>
	 *
	 * <p>Assumes that the objects are available at the<code>calledAETitle</code>
	 * specified in the constructor in this class instance.</p>
	 *
	 * <p>Assumes that we have a storage SCP listening as the <code>callingAETitle</code>
	 * specified in the constructor in this class instance.</p>
	 *
	 * @param		identifier				the move request identifier as a list of DICOM attributes
	 * @exception	IOException				thrown if there is an generic IO problem
	 * @exception	DicomException			thrown if there is a problem performing or parsing the query
	 * @exception	DicomNetworkException	thrown if there is a problem with the DICOM network protocol
	 */
	public void performHierarchicalMove(AttributeList identifier) throws IOException, DicomException, DicomNetworkException {
		new MoveSOPClassSCU(hostname,port,calledAETitle,callingAETitle,callingAETitle,getMoveSOPClassUID(),identifier,debugLevel);
	}
	
	/**
	 * @deprecated	See {@link #performHierarchicalMoveFrom(AttributeList,String) performHierarchicalMoveFrom()}.
	 */
	public void performHierarchicalMove(AttributeList identifier,String retrieveAE) throws IOException, DicomException, DicomNetworkException {
		performHierarchicalMoveFrom(identifier,retrieveAE);
	}
	
	/**
	 * <p>Retrieve DICOM object(s) from the specified location.</p>
	 *
	 * <p>Assumes that we have a storage SCP listening as the <code>callingAETitle</code>
	 * specified in the constructor in this class instance.</p>
	 *
	 * <p>Note that the <code>retrieveAE</code> argument may differ from the
	 * <code>calledAETitle</code> used in the constructor of this class instance.</p>
	 *
	 * @param		identifier				the move request identifier as a list of DICOM attributes
	 * @param		retrieveAE				the AE title of where to move the object(s) from
	 * @exception	IOException				thrown if there is an generic IO problem
	 * @exception	DicomException			thrown if there is a problem performing or parsing the query
	 * @exception	DicomNetworkException	thrown if there is a problem with the DICOM network protocol
	 */
	public void performHierarchicalMoveFrom(AttributeList identifier,String retrieveAE) throws IOException, DicomException, DicomNetworkException {
		new MoveSOPClassSCU(hostname,port,retrieveAE,callingAETitle,callingAETitle,getMoveSOPClassUID(),identifier,debugLevel);
	}
	
	/**
	 * <p>Retrieve DICOM object(s) to the specified location.</p>
	 *
	 * <p>Assumes that the objects are available at the <code>calledAETitle</code>
	 * specified in the constructor in this class instance.</p>
	 *
	 * <p>Further assumes that <code>calledAETitle</code> knows how to resolve the <code>moveDestination</code>
	 * into a presentation address (hostname and port number).</p>
	 *
	 * @param		identifier				the move request identifier as a list of DICOM attributes
	 * @param		moveDestination			the AE title of where to move the object(s) to
	 * @exception	IOException				thrown if there is an generic IO problem
	 * @exception	DicomException			thrown if there is a problem performing or parsing the query
	 * @exception	DicomNetworkException	thrown if there is a problem with the DICOM network protocol
	 */
	public void performHierarchicalMoveTo(AttributeList identifier,String moveDestination) throws IOException, DicomException, DicomNetworkException {
		new MoveSOPClassSCU(hostname,port,calledAETitle,callingAETitle,moveDestination,getMoveSOPClassUID(),identifier,debugLevel);
	}
	
	/**
	 * <p>Retrieve DICOM object(s) from the specified location to the specified location.</p>
	 *
	 * @param		identifier				the move request identifier as a list of DICOM attributes
	 * @param		retrieveAE				the AE title of where to move the object(s) from
	 * @param		moveDestination			the AE title of where to move the object(s) to
	 * @exception	IOException				thrown if there is an generic IO problem
	 * @exception	DicomException			thrown if there is a problem performing or parsing the query
	 * @exception	DicomNetworkException	thrown if there is a problem with the DICOM network protocol
	 */
	public void performHierarchicalMoveFromTo(AttributeList identifier,String retrieveAE,String moveDestination) throws IOException, DicomException, DicomNetworkException {
		new MoveSOPClassSCU(hostname,port,retrieveAE,callingAETitle,moveDestination,getMoveSOPClassUID(),identifier,debugLevel);
	}
	
	/**
	 * <p>Construct a query information model.</p>
	 *
	 * <p>Does not actually open an association or perform a query or retrieval; for that see:</p>
	 * <ul>
	 * <li> {@link #performHierarchicalQuery(AttributeList) performHierarchicalQuery()}
	 * <li> {@link #performHierarchicalMove(AttributeList) performHierarchicalMove()}
	 * <li> {@link #performHierarchicalMoveFrom(AttributeList,String) performHierarchicalMoveFrom()}
	 * <li> {@link #performHierarchicalMoveTo(AttributeList,String) performHierarchicalMoveTo()}
	 * <li> {@link #performHierarchicalMoveFromTo(AttributeList,String,String) performHierarchicalMoveFromTo()}
	 * </ul>
	 *
	 * @param	hostname		their hostname or IP address
	 * @param	port			their port number
	 * @param	calledAETitle		their AE title
	 * @param	callingAETitle		our AE title (both when we query or retrieve and where we are listening as a storage SCP)
	 * @param	debugLevel		0 is no debugging (silent), > 0 more verbose levels of debugging
	 */
	public QueryInformationModel(String hostname,int port,String calledAETitle,String callingAETitle,int debugLevel) {
		this.hostname=hostname;
		this.port=port;
		this.calledAETitle=calledAETitle;
		this.callingAETitle=callingAETitle;
		this.debugLevel=debugLevel;
	}

}

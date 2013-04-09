/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uka.ipd.idaho.plugins.bibRefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import de.uka.ipd.idaho.easyIO.streams.CharSequenceReader;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.XsltUtils;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefTypeSystem.BibRefType;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * TODO add sensible class description
 * 
 * @author sautter
 */
public class BibRefUtils implements BibRefConstants {
	
	/**
	 * Container for the attributes of a bibliographic reference, facilitating
	 * centralized and thus unified generation of reference strings and MODS
	 * records for reference data.
	 * 
	 * @author sautter
	 */
	public static class RefData {
		private HashMap attributes = new HashMap();
		
		private LinkedHashMap identifiers = new LinkedHashMap();
		private static final String ID_PREFIX = (PUBLICATION_IDENTIFIER_ANNOTATION_TYPE + "-");
		
		/**
		 * Constructor
		 */
		public RefData() {}
		
		/**
		 * Add a pair of attribute and value to the reference data set. Each
		 * attribute can be added multiple times to accomodate, for instance,
		 * multiple author names. If the argument attribute name starts with
		 * 'ID-', the attribute value is added as an identifier, using the part
		 * after the dash as the identifier type. There can only be one
		 * identifier of the same type in a reference data set.
		 * @param name the name of the attribute
		 * @param value the attribute value
		 */
		public void addAttribute(String name, String value) {
			if (name.startsWith(ID_PREFIX)) {
				this.setIdentifier(name.substring(ID_PREFIX.length()), value);
				return;
			}
			else if (PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(name))
				return;
			Object nValue = this.attributes.get(name);
			if (nValue == null)
				this.attributes.put(name, value);
			else if (nValue instanceof ArrayList)
				((ArrayList) nValue).add(value);
			else {
				ArrayList values = new ArrayList(3);
				values.add(nValue);
				values.add(value);
				this.attributes.put(name, values);
			}
		}
		
		/**
		 * Set an attribute of the reference data set to a specific value,
		 * replacing all present values. If the value should be appended to any
		 * existing ones rather than replacing them, use the addAttribute()
		 * method. If the argument attribute name starts with 'ID-', the
		 * attribute value is added as an identifier, using the part after the
		 * dash as the identifier type. There can only be one identifier of the
		 * same type in a reference data set.
		 * @param name the name of the attribute
		 * @param value the attribute value
		 */
		public void setAttribute(String name, String value) {
			if (name.startsWith(ID_PREFIX))
				this.setIdentifier(name.substring(ID_PREFIX.length()), value);
			else if (!PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(name))
				this.attributes.put(name, value);
		}
		
		/**
		 * Add an identifier of a specific type to the reference data set,
		 * possibly replacing an existing identifier of the same type.
		 * @param type the type of the identifier
		 * @param id the identifier
		 */
		public void setIdentifier(String type, String id) {
			if (this.identifiers == null)
				this.identifiers = new LinkedHashMap();
			this.identifiers.put(type, id);
			this.attributes.put(PUBLICATION_IDENTIFIER_ANNOTATION_TYPE, this.identifiers);
		}
		
		/**
		 * Rename an attribute, e.g. for attribute name unification. If an
		 * attribute with the new name is already present, the values of the
		 * renamed attribute are appended to its existing values. The identifier
		 * attribute cannot be renamed, so if either of the two argument strings
		 * equals 'ID' or starts with 'ID-', this method does nothing.
		 * @param name the name of the attribute to rename
		 * @param newName the new attribute name
		 * @return true if the attribute is present and was renamed, false
		 *         otherwise
		 */
		public boolean renameAttribute(String name, String newName) {
			if (newName.startsWith(ID_PREFIX))
				return ((this.identifiers != null) && this.identifiers.containsKey(newName.substring(ID_PREFIX.length())));
			else if (PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(newName))
				return (this.identifiers != null);
			else if (PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(name) || name.startsWith(ID_PREFIX))
				return false;
			
			if (name.equals(newName))
				return this.hasAttribute(name);
			Object nValue = this.attributes.remove(name);
			if (nValue == null)
				return false;
			if (this.hasAttribute(newName)) {
				if (nValue instanceof String)
					this.addAttribute(newName, ((String) nValue));
				else for (int v = 0; v < ((ArrayList) nValue).size(); v++)
					this.addAttribute(newName, ((String) ((ArrayList) nValue).get(v)));
			}
			else this.attributes.put(newName, nValue);
			return true;
		}
		
		/**
		 * Remove an attribute. If the argument attribute name starts with
		 * 'ID-', the part after the dash is used as the identifier type,
		 * removing the identifier of this very type.
		 * @param name the attribute name
		 */
		public void removeAttribute(String name) {
			if (name.startsWith(ID_PREFIX))
				this.removeIdentifier(name.substring(ID_PREFIX.length()));
			else if (!PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(name))
				this.attributes.remove(name);
		}
		
		/**
		 * Remove an identifier.
		 * @param type the type of the identifier
		 */
		public void removeIdentifier(String type) {
			if (this.identifiers == null)
				return;
			this.identifiers.remove(type);
			if (this.identifiers.isEmpty()) {
				this.attributes.remove(PUBLICATION_IDENTIFIER_ANNOTATION_TYPE);
				this.identifiers = null;
			}
		}
		
		/**
		 * Clear all attributes, including identifiers.
		 */
		public void clearAttributes() {
			this.attributes.clear();
			this.identifiers.clear();
			this.identifiers = null;
		}
		
		/**
		 * Check whether or not an attribute is present. If the argument
		 * attribute name equals 'ID', this method checks if the reference data
		 * set has any identifiers, regardless of their type. If the argument
		 * attribute name starts with 'ID-', the part after the dash is used as
		 * the identifier type, and a respective check is performed.
		 * @param name the attribute name
		 * @return true if the attribute is present, false otherwise
		 */
		public boolean hasAttribute(String name) {
			return (PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(name) ? (this.identifiers != null) : this.attributes.containsKey(name));
		}
		
		/**
		 * Retrieve the first value of an attribute. If there is no value for an
		 * attribute, this method returns null. If the argument attribute name
		 * equals 'ID', this method checks if the reference data set has any
		 * identifiers, regardless of their type, and returns the first one. If
		 * the argument attribute name starts with 'ID-', the part after the
		 * dash is used as the identifier type, and the respective identifier is
		 * returned.
		 * @param name the name of the attribute
		 * @return the first attribute value
		 */
		public String getAttribute(String name) {
			if (name.startsWith(ID_PREFIX))
				return ((this.identifiers == null) ? null : ((String) this.identifiers.get(name.substring(ID_PREFIX.length()))));
			else if (PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(name))
				return ((this.identifiers == null) ? null : ((String) this.identifiers.values().iterator().next()));
			
			Object value = this.attributes.get(name);
			if (value == null)
				return null;
			else if (value instanceof String)
				return ((String) value);
			else return ((String) ((ArrayList) value).get(0));
		}
		
		/**
		 * Retrieve all values of an attribute. If there is no value for an
		 * attribute, this method returns null. Thus, any returned array has at
		 * least one element.
		 * @param name the name of the attribute
		 * @return the attribute values
		 */
		public String[] getAttributeValues(String name) {
			if (name.startsWith(ID_PREFIX)) {
				if (this.identifiers == null)
					return null;
				String id = ((String) this.identifiers.get(name.substring(ID_PREFIX.length())));
				if (id == null)
					return null;
				String[] ids = {id};
				return ids;
			}
			else if (PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(name)) {
				if (this.identifiers == null)
					return null;
				return ((String[]) this.identifiers.values().toArray(new String[this.identifiers.size()]));
			}
			
			Object value = this.attributes.get(name);
			if (value == null)
				return null;
			else if (value instanceof String) {
				String[] values = {((String) value)};
				return values;
			}
			else return ((String[]) ((ArrayList) value).toArray(new String[((ArrayList) value).size()]));
		}
		
		/**
		 * Retrieve all values of an attribute, concatenated in a single string
		 * with a custom separator added in between. The agrument separator is
		 * used as it is; it has to include any whitespace desired between
		 * them and the attribute values. If there is no value for an attribute,
		 * this method returns null. Thus, any returned string is non-empty.
		 * This method is intended for attributes like author or editor names,
		 * but can be used for all attributes.
		 * @param name the name of the attribute
		 * @param separator the separator string to insert between attribute
		 *            values
		 * @return the concatenated attribute values
		 */
		public String getAttributeValueString(String name, String separator) {
			return this.getAttributeValueString(name, separator);
		}
		
		/**
		 * Retrieve all values of an attribute, concatenated in a single string
		 * with custom separators added in between. The agrument separators are
		 * used as they are; they have to include any whitespace desired between
		 * them and the attribute values. If there is no value for an attribute,
		 * this method returns null. Thus, any returned string is non-empty.
		 * This method is intended for attributes like author or editor names,
		 * but can be used for all attributes.
		 * @param name the name of the attribute
		 * @param separator the separator string to insert between all attribute
		 *            values except between the last two
		 * @param lastSeparator the separator string to insert between the last
		 *            two attribute values
		 * @return the concatenated attribute values
		 */
		public String getAttributeValueString(String name, String separator, String lastSeparator) {
			String[] values = this.getAttributeValues(name);
			if (values == null)
				return null;
			if (values.length == 1)
				return values[0];
			StringBuffer valueString = new StringBuffer();
			for (int v = 0; v < values.length; v++) {
				if (v != 0) {
					if ((v + 1) == values.length)
						valueString.append(lastSeparator);
					else valueString.append(separator);
				}
				valueString.append(values[v]);
			}
			return valueString.toString();
		}
		
		/**
		 * Retrieve an identifier of a specific type.
		 * @param type the identifier type
		 * @return the identifier
		 */
		public String getIdentifier(String type) {
			return ((this.identifiers == null) ? null : ((String) this.identifiers.get(type)));
		}
		
		/**
		 * Retrieve the names of all present attributes.
		 * @return the attribute names
		 */
		public String[] getAttributeNames() {
			return ((String[]) this.attributes.keySet().toArray(new String[this.attributes.size()]));
		}
		
		/**
		 * Retrieve the types of all present identifiers.
		 * @return the identifier types
		 */
		public String[] getIdentifierTypes() {
			if (this.identifiers == null)
				return new String[0];
			return ((String[]) this.identifiers.keySet().toArray(new String[this.identifiers.size()]));
		}
		
		/**
		 * Create an XML representation of the reference data set, e.g. for XSL
		 * transformation.
		 * @return an XML representation of the reference data
		 */
		public String toXML() {
			
			//	get type
			String type = this.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
			
			//	put reference attributes in string
			StringBuffer xml = new StringBuffer("<" + BIBLIOGRAPHIC_REFERENCE_TYPE);
			if (type != null)
				xml.append(" " + PUBLICATION_TYPE_ATTRIBUTE + "=\"" + type + "\"");
			xml.append(">");
			for (int a = 0; a < refDataAttributeNames.length; a++)
				this.appendAttribute(xml, refDataAttributeNames[a]);
			this.appendIdentifiers(xml);
			xml.append("</" + BIBLIOGRAPHIC_REFERENCE_TYPE + ">");
			
			//	finally ...
			return xml.toString();
		}
		
		private void appendAttribute(StringBuffer xml, String attributeName) {
			String[] values = this.getAttributeValues(attributeName);
			if ((values == null) && PAGINATION_ANNOTATION_TYPE.equals(attributeName))
				values = this.getAttributeValues("pageData");
			if (values == null)
				return;
			for (int v = 0; v < values.length; v++) {
				xml.append("<" + attributeName + ">");
				xml.append(escaper.escape(values[v]));
				xml.append("</" + attributeName + ">");
			}
		}
		private void appendIdentifiers(StringBuffer xml) {
			if (this.identifiers == null)
				return;
			for (Iterator tit = this.identifiers.keySet().iterator(); tit.hasNext();) {
				String type = ((String) tit.next());
				String id = ((String) this.identifiers.get(type));
				xml.append("<" + PUBLICATION_IDENTIFIER_ANNOTATION_TYPE + " " + TYPE_ATTRIBUTE + "=\"" + escaper.escape(type) + "\">");
				xml.append(escaper.escape(id));
				xml.append("</" + PUBLICATION_IDENTIFIER_ANNOTATION_TYPE + ">");
			}
		}
	}
	
	/**
	 * Transform a bibliographic reference annotation in generic format (like it
	 * comes out of RefParse, for instance) into a reference data object. This
	 * method splits publisher and location from one another if necessary.
	 * @param genericRef the annotation to transform
	 * @return a reference data object representing the argument reference
	 */
	public static RefData genericXmlToRefData(QueriableAnnotation genericRef) {
		RefData rd = new RefData();
		boolean gotPartDesignator = false;
		
		//	transfer attributes
		for (int a = 0; a < genericAttributeNames.length; a++) {
			
			//	get details
			Annotation[] details = genericRef.getAnnotations(genericAttributeNames[a]);
			
			//	check for pageData in addition to pagination, so to properly handle legacy data
			if ((details.length == 0) && PAGINATION_ANNOTATION_TYPE.equals(genericAttributeNames[a]))
				details = genericRef.getAnnotations("pageData");
			
			//	check for hostTitle in addition to volumeTitle, so to properly handle legacy data
			if ((details.length == 0) && VOLUME_TITLE_ANNOTATION_TYPE.equals(genericAttributeNames[a]))
				details = genericRef.getAnnotations("hostTitle");
			
			//	no detail annotations found, try attributes as fallback
			if (details.length == 0) {
				if (genericRef.hasAttribute(genericAttributeNames[a])) {
					if (AUTHOR_ANNOTATION_TYPE.equals(genericAttributeNames[a])) {
						String[] authors = ((String) genericRef.getAttribute(genericAttributeNames[a])).split("\\s*\\&\\s*");
						for (int d = 0; d < authors.length; d++)
							rd.addAttribute(genericAttributeNames[a], authors[d]);
					}
					else rd.addAttribute(genericAttributeNames[a], ((String) genericRef.getAttribute(genericAttributeNames[a])));
				}
				continue;
			}
			
			//	handle authors
			if (AUTHOR_ANNOTATION_TYPE.equals(genericAttributeNames[a])) {
				for (int d = 0; d < details.length; d++) {
					if (details[d].hasAttribute(REPEATED_AUTHORS_ATTRIBUTE)) {
						String[] rAuthors = ((String) details[d].getAttribute(REPEATED_AUTHORS_ATTRIBUTE)).split("\\s*\\&\\s*");
						for (int r = 0; r < rAuthors.length; r++)
							rd.addAttribute(genericAttributeNames[a], rAuthors[r]);
					}
					else rd.addAttribute(genericAttributeNames[a], TokenSequenceUtils.concatTokens(details[d], true, true));
				}
				continue;
			}
			
			//	transfer part designators type specifically
			if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(genericAttributeNames[a])) {
				for (int d = 0; d < details.length; d++) {
					String pdType = ((String) details[d].getAttribute(TYPE_ATTRIBUTE));
					if (pdType == null) {
						pdType = VOLUME_DESIGNATOR_ANNOTATION_TYPE;
					}
					if (VOLUME_DESIGNATOR_ANNOTATION_TYPE.equals(pdType)) {
						rd.setAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE, details[d].getValue());
						gotPartDesignator = true;
					}
					else if (ISSUE_DESIGNATOR_ANNOTATION_TYPE.equals(pdType)) {
						rd.setAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE, details[d].getValue());
						gotPartDesignator = true;
					}
					else if (NUMERO_DESIGNATOR_ANNOTATION_TYPE.equals(pdType)) {
						rd.setAttribute(NUMERO_DESIGNATOR_ANNOTATION_TYPE, details[d].getValue());
						gotPartDesignator = true;
					}
				}
				continue;
			}
			else if (VOLUME_DESIGNATOR_ANNOTATION_TYPE.equals(genericAttributeNames[a]) || ISSUE_DESIGNATOR_ANNOTATION_TYPE.equals(genericAttributeNames[a]) || NUMERO_DESIGNATOR_ANNOTATION_TYPE.equals(genericAttributeNames[a]))
				gotPartDesignator = true;
			
			//	handle journal/publisher
			if (JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE.equals(genericAttributeNames[a])) {
				
				//	journal name
				if (gotPartDesignator)
					rd.setAttribute(JOURNAL_NAME_ANNOTATION_TYPE, TokenSequenceUtils.concatTokens(details[0], true, true));
				
				//	split publisher from location
				else {
					int split = TokenSequenceUtils.indexOf(details[0], ":");
					if (split == -1)
						split = TokenSequenceUtils.indexOf(details[0], ",");
					if (split == -1)
						rd.setAttribute(PUBLISHER_ANNOTATION_TYPE, TokenSequenceUtils.concatTokens(details[0], true, true));
					else if (":".equals(details[0].valueAt(split))) {
						rd.setAttribute(PUBLISHER_ANNOTATION_TYPE, TokenSequenceUtils.concatTokens(details[0], (split+1), (details[0].size() - (split+1)), true, true));
						rd.setAttribute(LOCATION_ANNOTATION_TYPE, TokenSequenceUtils.concatTokens(details[0], 0, split, true, true));
					}
					else {
						rd.setAttribute(LOCATION_ANNOTATION_TYPE, TokenSequenceUtils.concatTokens(details[0], (split+1), (details[0].size() - (split+1)), true, true));
						rd.setAttribute(PUBLISHER_ANNOTATION_TYPE, TokenSequenceUtils.concatTokens(details[0], 0, split, true, true));
					}
				}
				
				continue;
			}
			
			//	handle other details
			for (int d = 0; d < details.length; d++)
				rd.addAttribute(genericAttributeNames[a], TokenSequenceUtils.concatTokens(details[d], true, true));
		}
		
		//	get type, induce if neccessary
		String type = ((String) genericRef.getAttribute(PUBLICATION_TYPE_ATTRIBUTE));
		if ((type == null) || (type.length() == 0))
			BibRefUtils.classify(rd);
		else rd.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, type);
		
		//	finally ...
		return rd;
	}
	
	/**
	 * Transform a bibliographic reference in MODS XML format into a reference
	 * data object.
	 * @param modsRef the annotation to transform
	 * @return a reference data object representing the argument reference
	 */
	public static RefData modsXmlToRefData(QueriableAnnotation modsRef) {
		//	TODO copy document
		
		//	TODO remove namespace prefix from annotations
		
		//	do data extraction
		RefData rd = new RefData();
		extractDetails(modsRef, AUTHOR_ANNOTATION_TYPE, authorsPath, rd);
		QueriableAnnotation[] originInfo = originInfoPath.evaluate(modsRef, null);
		if (originInfo.length != 0)
			extractDetails(originInfo[0], originInfoDetailPathsByType, rd);
		QueriableAnnotation[] hostItem = hostItemPath.evaluate(modsRef, null);
		if (hostItem.length != 0)
			extractDetails(hostItem[0], hostItemDetailPathsByType, rd);
		extractDetails(modsRef, TITLE_ANNOTATION_TYPE, titlePath, rd);
		
		//	get publication URL
		Annotation[] urlAnnots = publicationUrlPath.evaluate(modsRef, null);
		if ((urlAnnots != null) && (urlAnnots.length != 0))
			rd.setAttribute(PUBLICATION_URL_ANNOTATION_TYPE, TokenSequenceUtils.concatTokens(urlAnnots[0], false, true).replaceAll("\\s+", ""));
		
		//	store identifiers
		Annotation[] idAnnots = modsRef.getAnnotations("mods:identifier");
		for (int i = 0; i < idAnnots.length; i++) {
			String type = ((String) idAnnots[i].getAttribute("type"));
			if (type != null)
				rd.setIdentifier(type, idAnnots[i].getValue());
		}
		
		//	get reference type
		Annotation[] typeAnnots = modsRef.getAnnotations("mods:classification");
		String type = ((typeAnnots.length == 0) ? "" : typeAnnots[0].getValue().toLowerCase());
		
		//	set reference type attribute
		if (type.length() != 0)
			rd.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, type);
		
		//	handle host item title
		if (type.startsWith("journal") || rd.hasAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE) || rd.hasAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE) || rd.hasAttribute(NUMERO_DESIGNATOR_ANNOTATION_TYPE)) {
			rd.renameAttribute("hostTitle", JOURNAL_NAME_ANNOTATION_TYPE);
			rd.renameAttribute("hostVolumeTitle", VOLUME_TITLE_ANNOTATION_TYPE);
		}
		else {
			rd.renameAttribute("hostTitle", VOLUME_TITLE_ANNOTATION_TYPE);
			rd.renameAttribute("hostVolumeTitle", VOLUME_TITLE_ANNOTATION_TYPE);
		}
		
		//	unify pagination
		String fp = rd.getAttribute("firstPage");
		rd.removeAttribute("firstPage");
		String lp = rd.getAttribute("lastPage");
		rd.removeAttribute("lastPage");
		if (fp != null)
			rd.setAttribute(PAGINATION_ANNOTATION_TYPE, (fp + (((lp == null) || lp.equals(fp)) ? "" : ("-" + lp))));
		
		//	finally ...
		return rd;
	}
	private static void extractDetails(QueriableAnnotation data, HashMap detailPathsByType, RefData rd) {
		for (Iterator tit = detailPathsByType.keySet().iterator(); tit.hasNext();) {
			String type = ((String) tit.next());
			extractDetails(data, type, ((GPath) detailPathsByType.get(type)), rd);
		}
	}
	private static void extractDetails(QueriableAnnotation data, String type, GPath path, RefData rd) {
		Annotation[] details = path.evaluate(data, null);
		for (int d = 0; d < details.length; d++) {
			rd.addAttribute(type, TokenSequenceUtils.concatTokens(details[d], true, true));
//			System.out.println("GOT DETAIL " + type + ": '" + TokenSequenceUtils.concatTokens(details[d], true, true) + "'");
			if (TITLE_ANNOTATION_TYPE.equals(type))
				break;
		}
	}
	
	//	TODO remove 'mods:' namespace prefix from paths
	
	private static final GPath titlePath = new GPath("//mods:titleInfo/mods:title");
	
	private static final GPath authorsPath = new GPath("//mods:name[.//mods:roleTerm = 'Author']/mods:namePart");
	
	private static final GPath publicationUrlPath = new GPath("//mods:location/mods:url");
	
	private static final GPath hostItemPath = new GPath("//mods:relatedItem[./@type = 'host']");
	private static final GPath hostItem_titlePath = new GPath("//mods:titleInfo/mods:title");
	private static final GPath hostItem_volumeNumberPath = new GPath("//mods:part/mods:detail[(./@type = 'volume') or (./@type = 'issue') or (./@type = 'numero')]/mods:number");
	private static final GPath hostItem_volumeTitlePath = new GPath("//mods:part/mods:detail[./@type = 'title']/mods:title");
	private static final GPath hostItem_startPagePath = new GPath("//mods:part/mods:extent[./@unit = 'page']/mods:start");
	private static final GPath hostItem_endPagePath = new GPath("//mods:part/mods:extent[./@unit = 'page']/mods:end");
	private static final GPath hostItem_datePath = new GPath("//mods:part/mods:date");
	private static final GPath hostItem_editorsPath = new GPath("//mods:name[.//mods:roleTerm = 'Editor']/mods:namePart");
	
	private static final GPath hostItem_volumePath = new GPath("//mods:part/mods:detail[./@type = 'volume']/mods:number");
	private static final GPath hostItem_issuePath = new GPath("//mods:part/mods:detail[./@type = 'issue']/mods:number");
	private static final GPath hostItem_numeroPath = new GPath("//mods:part/mods:detail[./@type = 'numero']/mods:number");
	
	private static final GPath originInfoPath = new GPath("//mods:originInfo");
	private static final GPath originInfo_publisherNamePath = new GPath("//mods:publisher");
	private static final GPath originInfo_publisherLocationPath = new GPath("//mods:place/mods:placeTerm");
	private static final GPath originInfo_issueDatePath = new GPath("//mods:dateIssued");
	
	private static LinkedHashMap baseDetailPathsByType = new LinkedHashMap();
	static {
		baseDetailPathsByType.put(TITLE_ANNOTATION_TYPE, titlePath);
		baseDetailPathsByType.put(AUTHOR_ANNOTATION_TYPE, authorsPath);
	}
	private static LinkedHashMap hostItemDetailPathsByType = new LinkedHashMap();
	static {
		hostItemDetailPathsByType.put(PART_DESIGNATOR_ANNOTATION_TYPE, hostItem_volumeNumberPath);
		hostItemDetailPathsByType.put(YEAR_ANNOTATION_TYPE, hostItem_datePath);
		hostItemDetailPathsByType.put(EDITOR_ANNOTATION_TYPE, hostItem_editorsPath);
		hostItemDetailPathsByType.put("hostTitle", hostItem_titlePath);
		hostItemDetailPathsByType.put("hostVolumeTitle", hostItem_volumeTitlePath);
		hostItemDetailPathsByType.put(VOLUME_DESIGNATOR_ANNOTATION_TYPE, hostItem_volumePath);
		hostItemDetailPathsByType.put(ISSUE_DESIGNATOR_ANNOTATION_TYPE, hostItem_issuePath);
		hostItemDetailPathsByType.put(NUMERO_DESIGNATOR_ANNOTATION_TYPE, hostItem_numeroPath);
		hostItemDetailPathsByType.put("firstPage", hostItem_startPagePath);
		hostItemDetailPathsByType.put("lastPage", hostItem_endPagePath);
	}
	private static LinkedHashMap originInfoDetailPathsByType = new LinkedHashMap();
	static {
		originInfoDetailPathsByType.put(YEAR_ANNOTATION_TYPE, originInfo_issueDatePath);
		originInfoDetailPathsByType.put(PUBLISHER_ANNOTATION_TYPE, originInfo_publisherNamePath);
		originInfoDetailPathsByType.put(LOCATION_ANNOTATION_TYPE, originInfo_publisherLocationPath);
	}
	
//	private static class CountingTokenSequence {
//		String type;
//		private String plain;
//		private StringIndex counts = new StringIndex(true);
//		private StringIndex rCounts = new StringIndex(true);
//		private ArrayList tokens = new ArrayList();
//		private LinkedList rTokens = new LinkedList();
//		public CountingTokenSequence(String type, TokenSequence tokens) {
//			this.type = type;
//			this.plain = TokenSequenceUtils.concatTokens(tokens, true, true);
//			for (int t = 0; t < tokens.size(); t++) {
//				String token = tokens.valueAt(t);
////				if (!Gamta.isPunctuation(token)) {
//					this.counts.add(token);
//					this.tokens.add(token);
////				}
//			}
//		}
//		public String toString() {
//			return this.plain;
//		}
////		public boolean contains(String token) {
////			return (this.rCounts.getCount(token) < this.counts.getCount(token));
////		}
//		public boolean remove(String token) {
//			if (this.rCounts.getCount(token) < this.counts.getCount(token)) {
//				this.rCounts.add(token);
//				this.rTokens.addLast(token);
//				return true;
//			}
//			else return false;
//		}
////		public int matched() {
////			return this.rCounts.size();
////		}
//		public int remaining() {
//			return (this.counts.size() - this.rCounts.size());
//		}
//		public String next() {
//			return ((this.rTokens.size() < this.tokens.size()) ? ((String) this.tokens.get(this.rTokens.size())) : null);
//		}
//		public void reset() {
//			this.rCounts.clear();
//			this.rTokens.clear();
//		}
//	}
//	
//	private void annotate(MutableAnnotation bibRef, CountingTokenSequence cts) {
//		System.out.println("Matching " + cts.type + " '" + cts.toString() + "'");
//		
//		//	try full sequential match
//		for (int t = 0; t < bibRef.size(); t++) {
//			if (bibRef.tokenAt(t).hasAttribute("C"))
//				continue;
//			String token = bibRef.valueAt(t);
//			if (!token.equals(cts.next()))
//				continue;
//			
//			//	got anchor, attempt match
//			cts.remove(token);
//			System.out.println(" - found sequence anchor '" + token + "', " + cts.remaining() + " tokens remaining");
//			
//			//	found end of one-sized sequence, match successful
//			if (cts.remaining() == 0) {
//				Annotation a = bibRef.addAnnotation(cts.type, t, 1);
//				a.firstToken().setAttribute("C", "C");
//				System.out.println("   ==> single-token match: " + a.toXML());
//				return;
//			}
//			
//			//	continue matching
//			for (int l = (t+1); l < bibRef.size(); l++) {
//				token = bibRef.valueAt(l);
//				
//				//	next token continues match
//				if (token.equals(cts.next())) {
//					cts.remove(token);
//					System.out.println("   - found continuation '" + token + "', " + cts.remaining() + " tokens remaining");
//					
//					//	found end of sequence, match successful
//					if (cts.remaining() == 0) {
//						Annotation a = bibRef.addAnnotation(cts.type, t, (l-t+1));
//						for (int c = 0; c < a.size(); c++)
//							a.tokenAt(c).setAttribute("C", "C");
//						System.out.println("   ==> sequence match: " + a.toXML());
//						return;
//					}
//				}
//				
//				//	next token is punctuation, ignore it
//				else if (Gamta.isPunctuation(token)) {
//					System.out.println("   - ignoring punctuation '" + token + "'");
//					continue;
//				}
//				
//				//	next token does not match, reset matcher and start over
//				else {
//					System.out.println("   ==> cannot continue with '" + token + "'");
//					cts.reset();
//					break;
//				}
//			}
//		}
//	}
//	
//	private void mergeAnnotation(MutableAnnotation bibRef, String mType1, String mType2, String type) {
//		Annotation[] mAnnots1 = bibRef.getAnnotations(mType1);
//		if (mAnnots1.length == 0)
//			return;
//		Annotation[] mAnnots2 = bibRef.getAnnotations(mType2);
//		if (mAnnots2.length == 0)
//			return;
//		for (int ma1 = 0; ma1 < mAnnots1.length; ma1++) {
//			if (mAnnots1[ma1] == null)
//				continue;
//			for (int ma2 = 0; ma2 < mAnnots2.length; ma2++) {
//				if (mAnnots2[ma2] == null)
//					continue;
//				if (mAnnots2[ma2].getEndIndex() <= mAnnots1[ma1].getStartIndex()) {
//					boolean canMerge = true;
//					for (int t = mAnnots2[ma2].getEndIndex(); t < mAnnots1[ma1].getStartIndex(); t++)
//						if (!Gamta.isPunctuation(bibRef.valueAt(t))) {
//							canMerge = false;
//							break;
//						}
//					if (canMerge) {
//						Annotation merged = bibRef.addAnnotation(type, mAnnots2[ma2].getStartIndex(), (mAnnots1[ma1].getEndIndex() - mAnnots2[ma2].getStartIndex()));
//						bibRef.removeAnnotation(mAnnots2[ma2]);
//						bibRef.removeAnnotation(mAnnots1[ma1]);
//						mAnnots1[ma1] = (mType1.equals(type) ? merged : null);
//						mAnnots2[ma2] = (mType2.equals(type) ? merged : null);
//						break;
//					}
//				}
//				else if (mAnnots1[ma1].getEndIndex() <= mAnnots2[ma2].getStartIndex()) {
//					boolean canMerge = true;
//					for (int t = mAnnots1[ma1].getEndIndex(); t < mAnnots2[ma2].getStartIndex(); t++)
//						if (!Gamta.isPunctuation(bibRef.valueAt(t))) {
//							canMerge = false;
//							break;
//						}
//					if (canMerge) {
//						Annotation merged = bibRef.addAnnotation(type, mAnnots1[ma1].getStartIndex(), (mAnnots2[ma2].getEndIndex() - mAnnots1[ma1].getStartIndex()));
//						bibRef.removeAnnotation(mAnnots2[ma2]);
//						bibRef.removeAnnotation(mAnnots1[ma1]);
//						mAnnots1[ma1] = (mType1.equals(type) ? merged : null);
//						mAnnots2[ma2] = (mType2.equals(type) ? merged : null);
//						break;
//					}
//				}
//			}
//		}
//	}
	
	/**
	 * Transform a reference data object into a reference string in the default
	 * style.
	 * @param ref the annotation to transform
	 * @return a reference data object representing the argument reference
	 */
	public static String toRefString(RefData ref) {
		return escaper.unescape(transform(refStringBuilder, ref.toXML()));
	}
	
	/**
	 * Transform a reference data object into a reference string. If no
	 * reference string style is registered for the argument name, this method
	 * returns null. The names of the registered reference string styles can be
	 * obtained via the getRefStringStyles() method.
	 * @param ref the annotation to transform
	 * @param style the name of the reference string style (treated case
	 *            insensitive)
	 * @return a reference data object representing the argument reference
	 */
	public static String toRefString(RefData ref, String style) {
		RefFormat rf = ((RefFormat) refStringStyles.get(style.toLowerCase()));
		return ((rf == null) ? null : rf.format(ref));
	}
	
	/**
	 * Retrieve the names of the registered reference string styles.
	 * @return an array holding the style names
	 */
	public static String[] getRefStringStyles() {
		String[] rfs = new String[refStringStyles.size()];
		int rssi = 0;
		for (Iterator rssit = refStringStyles.values().iterator(); rssit.hasNext();)
			rfs[rssi++] = ((RefFormat) rssit.next()).name;
		return rfs;
	}
	
	/**
	 * Add a reference string style.
	 * @param name the name of the reference string style
	 * @param xslt the XSL stransformer generating the reference style from the
	 *            XML representation of reference data sets
	 * @return true if the style was newly added
	 */
	public static boolean addRefStringStyle(String name, Transformer xslt) {
		return ((name != null) && (xslt != null) && (refStringStyles.put(name.toLowerCase(), new RefFormat(name, xslt, false)) == null));
	}
	
	/**
	 * Transform a reference data object into an external data object
	 * representation, e.g. BibTeX or RIS. If no reference object type is
	 * registered for the argument name, this method returns null. The names of
	 * the registered reference object types can be obtained via the
	 * getRefObjectTypes() method.
	 * @param ref the annotation to transform
	 * @param type the name of the reference object type (treated case
	 *            insensitive)
	 * @return a reference data object representing the argument reference
	 */
	public static String toRefObject(RefData ref, String type) {
		RefFormat rf = ((RefFormat) refObjectTypes.get(type.toLowerCase()));
		return ((rf == null) ? null : rf.format(ref));
	}
	
	/**
	 * Retrieve the names of the registered reference object types.
	 * @return an array holding the type names
	 */
	public static String[] getRefObjectTypes() {
		String[] rfs = new String[refObjectTypes.size()];
		int rssi = 0;
		for (Iterator rssit = refObjectTypes.values().iterator(); rssit.hasNext();)
			rfs[rssi++] = ((RefFormat) rssit.next()).name;
		return rfs;
	}
	
	/**
	 * Add a reference object type. If the argument flag is set to indicate the
	 * output of the argument transformer is not XML, it will be unescaped.
	 * @param name the name of the reference object type
	 * @param xslt the XSL stransformer generating the reference objects from
	 *            the XML representation of reference data sets
	 * @param outputIsXml is the output of the transformer XML?
	 * @return true if the type was newly added
	 */
	public boolean addRefObjectType(String name, Transformer xslt, boolean outputIsXml) {
		return ((name != null) && (xslt != null) && (refObjectTypes.put(name.toLowerCase(), new RefFormat(name, xslt, outputIsXml)) == null));
	}
	
	/**
	 * Transform a reference data set into MODS XML, using the default reference
	 * type system. If the argument reference does not have a 'type' attribute,
	 * this method calls the classify() method before doing the actual
	 * transformation and adds the type to the data set.
	 * @param ref the reference data set to transform
	 * @return the MODS XML representation of the argument reference data set
	 */
	public static String toModsXML(RefData ref) {
		return BibRefTypeSystem.getDefaultInstance().toModsXML(ref);
	}
	
	//	custom XSLTs
	private static TreeMap refStringStyles = new TreeMap();
	private static TreeMap refObjectTypes = new TreeMap();
	private static class RefFormat {
		String name;
		Transformer xslt;
		boolean outputIsXml;
		RefFormat(String name, Transformer xslt, boolean outputIsXml) {
			this.name = name;
			this.xslt = xslt;
			this.outputIsXml = outputIsXml;
		}
		String format(RefData ref) {
			String fRef = transform(this.xslt, ref.toXML());
			if (this.outputIsXml)
				return fRef;
			else return escaper.unescape(fRef);
		}
	}
	
	//	TODO statically provide default styles, soon as samples come from Dauvit
	
	//	default XSLTs
	private static Transformer refStringBuilder;
	static {
		try {
			refStringBuilder = loadTransformer("refString.xslt");
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}
	
	/**
	 * Transform the content of some char sequence through some XLS transformer.
	 * If an error occurs, this method returns null.
	 * @param xslt the transformer to use
	 * @param refXml the char sequence to transform
	 * @return the transformation result
	 */
	public static final String transform(Transformer xslt, CharSequence refXml) {
		
		//	we don't have a transformer, cannot do it
		if (xslt == null)
			return null;
		
		//	transform it
		StringWriter refString = new StringWriter();
		CharSequenceReader csr = new CharSequenceReader(refXml);
		try {
			xslt.transform(new StreamSource(csr), new StreamResult(refString));
			return refString.toString();
		}
		catch (TransformerException te) {
			te.printStackTrace(System.out);
			return null;
		}
	}
	
	private static Transformer loadTransformer(String name) throws IOException {
		String rdfrn = BibRefUtils.class.getName().replaceAll("\\.", "/");
		InputStream tis = BibRefUtils.class.getClassLoader().getResourceAsStream(rdfrn.substring(0, rdfrn.lastIndexOf('/')) + "/" + name);
		BufferedReader tr = new BufferedReader(new InputStreamReader(tis, "UTF-8"));
		Transformer t = XsltUtils.getTransformer(("BibRefUtils:" + name), tr);
		tr.close();
		return t;
	}
	
	private static final Grammar escaper = new StandardGrammar();
	private static final String[] refDataAttributeNames = {
		AUTHOR_ANNOTATION_TYPE,
		YEAR_ANNOTATION_TYPE,
		TITLE_ANNOTATION_TYPE,
		JOURNAL_NAME_ANNOTATION_TYPE,
		PUBLISHER_ANNOTATION_TYPE,
		LOCATION_ANNOTATION_TYPE,
		PAGINATION_ANNOTATION_TYPE,
		VOLUME_DESIGNATOR_ANNOTATION_TYPE,
		ISSUE_DESIGNATOR_ANNOTATION_TYPE,
		NUMERO_DESIGNATOR_ANNOTATION_TYPE,
		VOLUME_TITLE_ANNOTATION_TYPE,
		EDITOR_ANNOTATION_TYPE,
		PUBLICATION_URL_ANNOTATION_TYPE,
		PUBLICATION_DOI_ANNOTATION_TYPE,
		PUBLICATION_ISBN_ANNOTATION_TYPE,
	};
	private static final String[] genericAttributeNames = {
		AUTHOR_ANNOTATION_TYPE,
		YEAR_ANNOTATION_TYPE,
		TITLE_ANNOTATION_TYPE,
		PART_DESIGNATOR_ANNOTATION_TYPE,
		VOLUME_DESIGNATOR_ANNOTATION_TYPE,
		ISSUE_DESIGNATOR_ANNOTATION_TYPE,
		NUMERO_DESIGNATOR_ANNOTATION_TYPE,
		JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE,
		JOURNAL_NAME_ANNOTATION_TYPE,
		PUBLISHER_ANNOTATION_TYPE,
		LOCATION_ANNOTATION_TYPE,
		PAGINATION_ANNOTATION_TYPE,
		VOLUME_TITLE_ANNOTATION_TYPE,
		EDITOR_ANNOTATION_TYPE,
		PUBLICATION_URL_ANNOTATION_TYPE,
		PUBLICATION_DOI_ANNOTATION_TYPE,
		PUBLICATION_ISBN_ANNOTATION_TYPE,
	};
	
	/**
	 * Check if the pagination of a reference is actually that of a part of a
	 * larger volume, or if it consists of information about a whole volume,
	 * e.g. the total number of pages it contains. If this method finds the
	 * latter to be true, it removes the pagination and adjusts the reference
	 * type to match the reference to a whole volume. If the former is true,
	 * this method makes sure the pagination is a single page number or page
	 * range, so to simplify parsing later on.
	 * @param ref the reference data to check
	 */
	public static void checkPaginationAndType(RefData ref) {
		String pageString = ref.getAttribute(PAGINATION_ANNOTATION_TYPE);
		if (pageString == null)
			return;
		pageString = pageString.trim();
		
		StringBuffer nPageString = new StringBuffer();
		for (int c = 0; c < pageString.length(); c++) {
			char ch = pageString.charAt(c);
			if (Character.isLetterOrDigit(ch))
				nPageString.append(ch);
			else nPageString.append(StringUtils.getBaseChar(ch));
		}
		pageString = nPageString.toString();
		
		if (pageCountPattern.matcher(pageString).find())
			ref.removeAttribute(PAGINATION_ANNOTATION_TYPE);
		else {
			String type = ref.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
			String pages = null;
			if (pages == null) {
				Matcher prm = pageRangePattern.matcher(pageString);
				while (prm.find()) {
					String[] pnrs = prm.group().split("\\s*\\-+\\s*");
					if (pnrs.length == 2) try {
						int fpn = Integer.parseInt(pnrs[0]);
						int lpn = Integer.parseInt(pnrs[1]);
						if ((0 < fpn) && (fpn < lpn)) {
							pages = (fpn + "-" + lpn);
							break;
						}
					} catch (NumberFormatException nfe) {}
				}
			}
			if (pages == null) {
				Matcher pnm = pageNumberPattern.matcher(pageString);
				while (pnm.find()) {
					String pnr = pnm.group();
					try {
						int pn = Integer.parseInt(pnr);
						if (0 < pn) {
							pages = ("" + pn);
							break;
						}
					} catch (NumberFormatException nfe) {}
				}
			}
			
			//	no pages found
			if (pages == null)
				ref.removeAttribute(PAGINATION_ANNOTATION_TYPE);
			
			//	likely simply the whole book or journal
			else if ((BOOK_REFERENCE_TYPE.equals(type) || JOURNAL_VOLUME_REFERENCE_TYPE.equals(type)) && pages.matches("[1]\\-[2-9][0-9]{2,}"))
				ref.removeAttribute(PAGINATION_ANNOTATION_TYPE);
			
			//	found pages, adjust publication type if secure
			else {
				ref.setAttribute(PAGINATION_ANNOTATION_TYPE, pages);
				if (pages.indexOf('-') != -1) {
					if (BOOK_REFERENCE_TYPE.equals(type) && ref.hasAttribute(VOLUME_TITLE_ANNOTATION_TYPE))
						type = BOOK_CHAPTER_REFERENCE_TYPE;
					else if (JOURNAL_VOLUME_REFERENCE_TYPE.equals(type) && ref.hasAttribute(JOURNAL_NAME_ANNOTATION_TYPE))
						type = JOURNAL_ARTICEL_REFERENCE_TYPE;
				}
			}
			
			//	set back type
			ref.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, type);
		}
	}
	
	private static Pattern pageRangePattern = Pattern.compile("[1-9][0-9]*+\\s*\\-+\\s*[1-9][0-9]*+");
	private static Pattern pageNumberPattern = Pattern.compile("[1-9][0-9]*+");
	private static Pattern pageCountPattern = Pattern.compile("[1-9][0-9]*\\s*pp", Pattern.CASE_INSENSITIVE);
	
	/**
	 * Find out which type of work a bibliographic reference refers to. This
	 * method loops through to the classify() method of the default reference
	 * type system. It sets the 'type' attribute of the argument reference data
	 * set if a type is found. Client code may implement its own classification
	 * mechanism.
	 * @param ref the reference data set to classify
	 * @return the type of the referenced work
	 */
	public static String classify(RefData ref) {
		String type = BibRefTypeSystem.getDefaultInstance().classify(ref);
		if (type != null)
			ref.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, type);
		return type;
	}
	
	/**
	 * Read document meta data from a given MODS header and add them as
	 * attributes to a given document. If the argument document is a
	 * DocumentRoot instance, the meta data is additionally deposited in
	 * document properties.
	 * @param doc the documents to add the attributes to
	 * @param mods the MODS header to take the attributes from
	 */
	public static void setDocAttributes(MutableAnnotation doc, MutableAnnotation mods) {
		setDocAttributes(doc, modsXmlToRefData(mods));
	}
	
	/**
	 * Add document meta data from a reference data set as attributes to a given
	 * document. If the argument document is a DocumentRoot instance, the meta
	 * data is additionally deposited in document properties.
	 * @param doc the documents to add the attributes to
	 * @param ref the MODS data set to take the attributes from
	 */
	public static void setDocAttributes(MutableAnnotation doc, RefData ref) {
		String[] idTypes = ref.getIdentifierTypes();
		for (int i = 0; i < idTypes.length; i++) {
			if (!AnnotationUtils.isValidAnnotationType(idTypes[i]))
				continue;
			String id = ref.getIdentifier(idTypes[i]);
			if ((id != null) && (id.trim().length() != 0))
				setAttribute(doc, ("ID-" + idTypes[i]), id.trim());
		}
		
		setAttribute(doc, DOCUMENT_AUTHOR_ATTRIBUTE, ref, AUTHOR_ANNOTATION_TYPE, false);
		setAttribute(doc, DOCUMENT_TITLE_ATTRIBUTE, ref, TITLE_ANNOTATION_TYPE, true);
		setAttribute(doc, DOCUMENT_DATE_ATTRIBUTE, ref, YEAR_ANNOTATION_TYPE, true);
		setAttribute(doc, DOCUMENT_SOURCE_LINK_ATTRIBUTE, ref, PUBLICATION_URL_ANNOTATION_TYPE, true);
		
		if (ref.hasAttribute(PAGINATION_ANNOTATION_TYPE) || ref.hasAttribute(VOLUME_TITLE_ANNOTATION_TYPE)) {
			String origin = BibRefTypeSystem.getDefaultInstance().getOrigin(ref);
			if ((origin != null) && (origin.trim().length() != 0))
				setAttribute(doc, DOCUMENT_ORIGIN_ATTRIBUTE, origin.trim());
			String pagination = ref.getAttribute(PAGINATION_ANNOTATION_TYPE);
			if (pagination != null) {
				String[] pns = pagination.trim().split("\\s*\\-+\\s*");
				if (pns.length == 1)
					doc.setAttribute(PAGE_NUMBER_ATTRIBUTE, pns[0]);
				else if (pns.length == 2) {
					doc.setAttribute(PAGE_NUMBER_ATTRIBUTE, pns[0]);
					doc.setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, pns[1]);
				}
			}
		}
	}
	
	private static void setAttribute(MutableAnnotation doc, String dan, RefData ref, String ran, boolean onlyFirst) {
		String[] values = ref.getAttributeValues(ran);
		if (values == null)
			return;
		String value;
		if (onlyFirst || (values.length == 1))
			value = values[0];
		else {
			StringBuffer valueBuilder = new StringBuffer();
			for (int v = 0; v < values.length; v++) {
				if (v != 0)
					valueBuilder.append(" & ");
				valueBuilder.append(values[v]);
			}
			value = valueBuilder.toString();
		}
		setAttribute(doc, dan, value);
	}
	
	private static void setAttribute(MutableAnnotation doc, String name, String value) {
		doc.setAttribute(name, value);
		if (doc instanceof DocumentRoot)
			((DocumentRoot) doc).setDocumentProperty(name, value);
	}
	
	/**
	 * Clean out a MODS document header, i.e., remove all annotations that do
	 * not belong to the MODS namespace, and set the namesace URI.
	 * @param mods the MODS XML to clean
	 */
	public static void cleanModsXML(MutableAnnotation mods) {
		Annotation[] inMods = mods.getAnnotations();
		for (int i = 0; i < inMods.length; i++) {
			if (!inMods[i].getType().startsWith("mods:") && (inMods[i].size() < mods.size()))
				mods.removeAnnotation(inMods[i]);
			else {
				inMods[i].lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
				if ("mods:mods".equals(inMods[i].getType()))
					inMods[i].setAttribute("xmlns:mods", "http://www.loc.gov/mods/v3");
			}
		}
		if ("mods:mods".equals(mods.getType()))
			mods.setAttribute("xmlns:mods", "http://www.loc.gov/mods/v3");

	}
//	/**
//	 * Find out which type of work a bibliographic reference refers to. This
//	 * method checks for pagination, part designators (volume or issue numbers),
//	 * and volume titles starting with 'Proc.' or 'Proceedings', in this order.
//	 * Client code may implement its own classification mechanism.
//	 * @param ref the reference data set to classify
//	 * @return the type of the referenced work
//	 */
//	public static String classify(RefData ref) {
//		
//		//	got pagination ==> part of something
//		boolean gotPagination = ref.hasAttribute(PAGINATION_ANNOTATION_TYPE);
//		
//		//	got volume, issue, or number designator
//		boolean gotPartDesignator = (ref.hasAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE) || ref.hasAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE) || ref.hasAttribute(NUMERO_DESIGNATOR_ANNOTATION_TYPE) || ref.hasAttribute(PART_DESIGNATOR_ANNOTATION_TYPE));
//		
//		//	check journal name
//		boolean gotJournalName = ref.hasAttribute(JOURNAL_NAME_ANNOTATION_TYPE);
//		
//		//	clearly a journal or part of one
//		if (gotJournalName && gotPartDesignator)
//			return (gotPagination ? JOURNAL_ARTICEL_REFERENCE_TYPE : JOURNAL_VOLUME_REFERENCE_TYPE);
//		
////		//	check publisher name
////		boolean gotPublisher = ref.hasAttribute(PUBLISHER);
////		
//		//	check title
//		boolean gotTitle = ref.hasAttribute(TITLE_ANNOTATION_TYPE);
//		
//		//	check volume title
//		boolean gotVolumeTitle = ref.hasAttribute(VOLUME_TITLE_ANNOTATION_TYPE);
//		
//		//	check for proceedings
//		boolean isProceedings = false;
//		if (!isProceedings && gotJournalName && !gotPartDesignator && ref.getAttribute(JOURNAL_NAME_ANNOTATION_TYPE).toLowerCase().startsWith("proc"))
//			isProceedings = true;
//		if (!isProceedings && gotVolumeTitle && !gotPartDesignator && ref.getAttribute(VOLUME_TITLE_ANNOTATION_TYPE).toLowerCase().startsWith("proc"))
//			isProceedings = true;
//		if (!isProceedings && gotTitle && !gotPagination && ref.getAttribute(TITLE_ANNOTATION_TYPE).toLowerCase().startsWith("proc"))
//			isProceedings = true;
//		
//		//	part of book or proceedings
//		if (gotPagination)
//			return (isProceedings ? PROCEEDINGS_PAPER_REFERENCE_TYPE : BOOK_CHAPTER_REFERENCE_TYPE);
//		
//		//	part of proceedings with missing page data
//		if (isProceedings && gotVolumeTitle)
//			return PROCEEDINGS_PAPER_REFERENCE_TYPE;
//		
//		//	book or proceedings
//		else return (isProceedings ? PROCEEDINGS_REFERENCE_TYPE : BOOK_REFERENCE_TYPE); 
//	}
	
	public static void main(String[] args) throws Exception {
		BibRefTypeSystem brtSys = BibRefTypeSystem.getDefaultInstance();
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/SMNK-Projekt/EcologyTestbed/Taylor_Wolters_2005.normalized.xml"));
		AnnotationFilter.renameAnnotations(doc, CITATION_TYPE, BIBLIOGRAPHIC_REFERENCE_TYPE);
		AnnotationFilter.renameAnnotations(doc, "hostTitle", VOLUME_TITLE_ANNOTATION_TYPE);
		AnnotationFilter.renameAnnotationAttribute(doc, BIBLIOGRAPHIC_REFERENCE_TYPE, "hostTitle", VOLUME_TITLE_ANNOTATION_TYPE);
		MutableAnnotation[] bibRefs = doc.getMutableAnnotations(BIBLIOGRAPHIC_REFERENCE_TYPE);
		for (int r = 0; r < bibRefs.length; r++) try {
			System.out.println(TokenSequenceUtils.concatTokens(bibRefs[r], true, true));
			RefData rd = genericXmlToRefData(bibRefs[r]);
			String type = brtSys.classify(rd);
//			if (type == null) {
//				BibRefType[] brts = brtSys.getBibRefTypes();
//				for (int t = 0; t < brts.length; t++) {
//					String[] errors = brts[t].getErrors(rd);
//					if (errors == null) {
//						type = brts[t].name;
//						break;
//					}
//					else {
//						System.out.println(" === " + brts[t].name + " ===");
//						for (int e = 0; e < errors.length; e++)
//							System.out.println(errors[e]);
//					}
//				}
//			}
			System.out.println(" ==> " + type);
			if (type == null)
				continue;
			BibRefType brt = brtSys.getBibRefType(type);
			String origin = brt.getOrigin(rd);
			System.out.println(origin);
			if (origin.startsWith(","))
				AnnotationUtils.writeXML(bibRefs[r], new PrintWriter(System.out));
			System.out.println(rd.toXML());
//			System.out.println(toModsXML(rd));
//			System.out.println(toRefString(rd));
		}
		catch (Exception e) {
			AnnotationUtils.writeXML(bibRefs[r], new PrintWriter(System.out));
			throw e;
		}
	}
}
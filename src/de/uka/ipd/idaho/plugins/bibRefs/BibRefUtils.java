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
 *     * Neither the name of the Universitaet Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITAET KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import de.uka.ipd.idaho.easyIO.streams.CharSequenceReader;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathFunction;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.InvalidArgumentsException;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathAnnotationSet;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathNumber;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathString;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.XsltUtils;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
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
	public static class RefData implements BibRefConstants {
		private HashMap attributes = new HashMap();
		
		private LinkedHashMap identifiers = new LinkedHashMap();
		static final String ID_PREFIX = (PUBLICATION_IDENTIFIER_ANNOTATION_TYPE + "-");
		
		/**
		 * Constructor
		 */
		public RefData() {}
		
		/**
		 * Constructor
		 * @param model the bibliographic reference data object to copy
		 */
		public RefData(RefData model) {
			for (Iterator anit = model.attributes.keySet().iterator(); anit.hasNext();) {
				String an = ((String) anit.next());
				Object av = model.attributes.get(an);
				if (AUTHOR_ANNOTATION_TYPE.equals(an)) {
					if (av instanceof ArrayList) {
						ArrayList mavs = ((ArrayList) av);
						ArrayList avs = new ArrayList(mavs.size());
						for (int v = 0; v < mavs.size(); v++)
							avs.add(new AuthorData((AuthorData) mavs.get(v)));
						this.attributes.put(an, avs);
					}
					else this.attributes.put(an, new AuthorData((AuthorData) av));
				}
				else {
					if (av instanceof ArrayList)
						this.attributes.put(an, new ArrayList((ArrayList) av));
					else this.attributes.put(an, av);
				}
			}
			this.identifiers.putAll(model.identifiers);
		}
		
		/**
		 * Add a pair of attribute and value to the reference data set. Each
		 * attribute can be added multiple times to accommodate, for instance,
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
			
			Object valueObj;
			if (AUTHOR_ANNOTATION_TYPE.equals(name))
				valueObj = new AuthorData(value);
			else valueObj = value;
			Object exValue = this.attributes.get(name);
			if (exValue == null)
				this.attributes.put(name, valueObj);
			else if (exValue instanceof ArrayList)
				((ArrayList) exValue).add(valueObj);
			else {
				ArrayList values = new ArrayList(3);
				values.add(exValue);
				values.add(valueObj);
				this.attributes.put(name, values);
			}
//			Object aValue = this.attributes.get(name);
//			if (aValue == null)
//				this.attributes.put(name, value);
//			else if (aValue instanceof ArrayList)
//				((ArrayList) aValue).add(value);
//			else {
//				ArrayList values = new ArrayList(3);
//				values.add(aValue);
//				values.add(value);
//				this.attributes.put(name, values);
//			}
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
			else if (PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(name))
				return;
			
			Object valueObj;
			if (AUTHOR_ANNOTATION_TYPE.equals(name))
				valueObj = new AuthorData(value);
			else valueObj = value;
			this.attributes.put(name, valueObj);
//			this.attributes.put(name, value);
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
		 * Set a detail attribute for an author. If no author with the argument
		 * name exists in this reference dataset, and if the argument attribute
		 * value is not null, the author will be added.
		 * @param authorName the author name to set the attribute for
		 * @param name the name of the attribute
		 * @param value the attribute value
		 */
		public void setAuthorAttribute(String authorName, String name, String value) {
			Object authorObj = this.attributes.get(AUTHOR_ANNOTATION_TYPE);
			if (authorObj == null) {
				if (value == null)
					return;
				authorObj = new AuthorData(authorName);
				this.attributes.put(AUTHOR_ANNOTATION_TYPE, authorObj);
			}
			else if (authorObj instanceof AuthorData) {
				if (((AuthorData) authorObj).matches(authorName, 0)) {}
				else {
					ArrayList authorList = new ArrayList(3);
					authorList.add(authorObj);
					this.attributes.put(AUTHOR_ANNOTATION_TYPE, authorList);
					authorObj = new AuthorData(authorName);
					authorList.add(authorObj);
				}
			}
			else {
				ArrayList authorList = ((ArrayList) authorObj);
				authorObj = null;
				for (int a = 0; a < authorList.size(); a++)
					if (((AuthorData) authorList.get(a)).matches(authorName, 0)) {
						authorObj = authorList.get(a);
						break;
					}
				if (authorObj == null) {
					if (value == null)
						return;
					authorObj = new AuthorData(authorName);
					authorList.add(authorObj);
				}
			}
			((AuthorData) authorObj).setAttribute(name, value);
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
			Object value = this.attributes.remove(name);
			if (value == null)
				return false;
			if (AUTHOR_ANNOTATION_TYPE.equals(name)) {
				if (value instanceof AuthorData)
					this.addAttribute(newName, ((AuthorData) value).name);
				else for (int v = 0; v < ((ArrayList) value).size(); v++)
					this.addAttribute(newName, ((AuthorData) ((ArrayList) value).get(v)).name);
			}
			else {
				if (value instanceof String)
					this.addAttribute(newName, ((String) value));
				else for (int v = 0; v < ((ArrayList) value).size(); v++)
					this.addAttribute(newName, ((String) ((ArrayList) value).get(v)));
			}
			return true;
		}
		
		/**
		 * Remove an attribute. If the argument attribute name starts with
		 * 'ID-', the part after the dash is used as the identifier type,
		 * removing the identifier of this very type.
		 * @param name the attribute name
		 * @return true if the attribute was present and was removed, false
		 *         otherwise
		 */
		public boolean removeAttribute(String name) {
			if (name.startsWith(ID_PREFIX))
				return this.removeIdentifier(name.substring(ID_PREFIX.length()));
			else if (PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(name))
				return false;
			else return (this.attributes.remove(name) != null);
		}
		
		/**
		 * Remove an identifier.
		 * @param type the type of the identifier
		 * @return true if the identifier was present and was removed, false
		 *         otherwise
		 */
		public boolean removeIdentifier(String type) {
			if (this.identifiers == null)
				return false;
			boolean removed = (this.identifiers.remove(type) != null);
			if (this.identifiers.isEmpty()) {
				this.attributes.remove(PUBLICATION_IDENTIFIER_ANNOTATION_TYPE);
				this.identifiers = null;
			}
			return removed;
		}
		
		/**
		 * Remove a detail attribute from an author. If no author with the
		 * argument name exists in this reference dataset, this method does
		 * not modify anything.
		 * @param authorName the author name to remove the attribute from
		 * @param name the attribute name
		 * @return true if the attribute was present and was removed, false
		 *         otherwise
		 */
		public boolean removeAuthorAttribute(String authorName, String name) {
			Object authorObj = this.attributes.get(AUTHOR_ANNOTATION_TYPE);
			if (authorObj == null)
				return false;
			//	TODO remove from all authors for null author name
			else if (authorObj instanceof AuthorData) {
				if (((AuthorData) authorObj).matches(authorName, 0))
					return ((AuthorData) authorObj).removeAttribute(name);
				else return false;
			}
			else {
				ArrayList authorList = ((ArrayList) authorObj);
				for (int a = 0; a < authorList.size(); a++) {
					if (((AuthorData) authorList.get(a)).matches(authorName, 0))
						return ((AuthorData) authorList.get(a)).removeAttribute(name);
				}
				return false;
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
			if (value instanceof ArrayList)
				value = ((ArrayList) value).get(0);
			if (value instanceof AuthorData)
				return ((AuthorData) value).name;
			else return ((String) value);
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
			if (value instanceof String) {
				String[] values = {((String) value)};
				return values;
			}
			else if (value instanceof AuthorData) {
				String[] values = {((AuthorData) value).name};
				return values;
			}
			else {
				ArrayList valueList = ((ArrayList) value);
				String[] values = new String[valueList.size()];
				for (int v = 0; v < valueList.size(); v++) {
					value = valueList.get(v);
					if (value instanceof String)
						values[v] = ((String) value);
					else if (value instanceof AuthorData)
						values[v] = ((AuthorData) value).name;
				}
				return values;
			}
		}
		
		/**
		 * Retrieve all values of an attribute, concatenated in a single string
		 * with a custom separator added in between. The argument separator is
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
			return this.getAttributeValueString(name, separator, separator);
		}
		
		/**
		 * Retrieve all values of an attribute, concatenated in a single string
		 * with custom separators added in between. The argument separators are
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
		 * Retrieve a detail attribute of an author. If no author with the
		 * argument name exists in this reference dataset, this method returns
		 * null.
		 * @param authorName the author name to get the attribute for
		 * @param name the name of the attribute
		 * @return the attribute value
		 */
		public String getAuthorAttribute(String authorName, String name) {
			Object authorObj = this.attributes.get(AUTHOR_ANNOTATION_TYPE);
			if (authorObj == null)
				return null;
			else if (authorObj instanceof AuthorData) {
				if (((AuthorData) authorObj).matches(authorName, 0))
					return ((AuthorData) authorObj).getAttribute(name);
				else return null;
			}
			else {
				ArrayList authorList = ((ArrayList) authorObj);
				for (int a = 0; a < authorList.size(); a++) {
					if (((AuthorData) authorList.get(a)).matches(authorName, 0))
						return ((AuthorData) authorList.get(a)).getAttribute(name);
				}
				return null;
			}
		}
		
		/**
		 * Retrieve the detail attributes of an author. If no author with the
		 * argument name exists in this reference dataset, this method returns
		 * null.
		 * @param authorName the author name to get the attributes for
		 * @return the author data object holding the detail attributes
		 */
		public AuthorData getAuthorData(String authorName) {
			Object authorObj = this.attributes.get(AUTHOR_ANNOTATION_TYPE);
			if (authorObj == null)
				return null;
			else if (authorObj instanceof AuthorData) {
				if (((AuthorData) authorObj).matches(authorName, 0))
					return ((AuthorData) authorObj);
				else return null;
			}
			else {
				ArrayList authorList = ((ArrayList) authorObj);
				for (int a = 0; a < authorList.size(); a++) {
					if (((AuthorData) authorList.get(a)).matches(authorName, 0))
						return ((AuthorData) authorList.get(a));
				}
				return null;
			}
		}
		
		/**
		 * Retrieve the detail attributes of all authors.
		 * @return the author data objects holding the detail attributes
		 */
		public AuthorData[] getAuthorDatas() {
			Object authorObj = this.attributes.get(AUTHOR_ANNOTATION_TYPE);
			if (authorObj == null)
				return null;
			else if (authorObj instanceof AuthorData) {
				AuthorData[] ads = {((AuthorData) authorObj)};
				return ads;
			}
			else {
				ArrayList authorList = ((ArrayList) authorObj);
				return ((AuthorData[]) authorList.toArray(new AuthorData[authorList.size()]));
			}
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
		 * Retrieve the names of all present author detail attributes.
		 * @return the author detail attribute names
		 */
		public String[] getAuthorAttributeNames() {
			Object authorObj = this.attributes.get(AUTHOR_ANNOTATION_TYPE);
			if (authorObj == null)
				return new String[0];
			if (authorObj instanceof AuthorData)
				return ((AuthorData) authorObj).getAttributeNames();
			ArrayList authorList = ((ArrayList) authorObj);
			TreeSet ans = new TreeSet();
			ans.add(AuthorData.AUTHOR_NAME_ATTRIBUTE);
			for (int a = 0; a < authorList.size(); a++) {
				if (((AuthorData) authorList.get(a)).attributes != null)
					ans.addAll(((AuthorData) authorList.get(a)).attributes.keySet());
			}
			return ((String[]) ans.toArray(new String[ans.size()]));
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
		
		private void appendAttribute(StringBuffer xml, String name) {
			Object valueObj = this.attributes.get(name);
			if (valueObj == null) {
				if (PAGINATION_ANNOTATION_TYPE.equals(name))
					valueObj = this.attributes.get("pageData");
				else if (this.getAttribute(PUBLICATION_TYPE_ATTRIBUTE) != null) {
					String type = this.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
					if (JOURNAL_NAME_ANNOTATION_TYPE.equals(name) && type.startsWith("journal"))
						valueObj = this.attributes.get(JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE);
					else if (PUBLISHER_ANNOTATION_TYPE.equals(name) && !type.startsWith("journal"))
						valueObj = this.attributes.get(JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE);
				}
			}
			if (valueObj == null)
				return;
			if (valueObj instanceof String)
				this.appendAttribute(xml, name, ((String) valueObj));
			else if (valueObj instanceof AuthorData)
				this.appendAuthor(xml, ((AuthorData) valueObj));
			else {
				ArrayList values = ((ArrayList) valueObj);
				for (int v = 0; v < values.size(); v++) {
					valueObj = values.get(v);
					if (valueObj instanceof String)
						this.appendAttribute(xml, name, ((String) valueObj));
					else if (valueObj instanceof AuthorData)
						this.appendAuthor(xml, ((AuthorData) valueObj));
				}
			}
//			String[] values = this.getAttributeValues(attributeName);
//			if (values == null) {
//				if (PAGINATION_ANNOTATION_TYPE.equals(attributeName))
//					values = this.getAttributeValues("pageData");
//				else if (this.getAttribute(PUBLICATION_TYPE_ATTRIBUTE) != null) {
//					String type = this.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
//					if (JOURNAL_NAME_ANNOTATION_TYPE.equals(attributeName) && type.startsWith("journal"))
//						values = this.getAttributeValues(JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE);
//					else if (PUBLISHER_ANNOTATION_TYPE.equals(attributeName) && !type.startsWith("journal"))
//						values = this.getAttributeValues(JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE);
//				}
//			}
//			if (values == null)
//				return;
//			for (int v = 0; v < values.length; v++) {
//				xml.append("<" + attributeName + ">");
//				xml.append(escaper.escape(values[v]));
//				xml.append("</" + attributeName + ">");
//			}
		}
		private void appendAttribute(StringBuffer xml, String name, String value) {
			xml.append("<" + name + ">");
			xml.append(escaper.escape(value));
			xml.append("</" + name + ">");
		}
		private void appendAuthor(StringBuffer xml, AuthorData value) {
			xml.append("<" + AUTHOR_ANNOTATION_TYPE);
			if (value.attributes != null)
				for (Iterator anit = value.attributes.keySet().iterator(); anit.hasNext();) {
					String an = ((String) anit.next());
					xml.append(" " + an + "=\"" + escaper.escape((String) value.attributes.get(an)) + "\"");
				}
			xml.append(">");
			xml.append(escaper.escape(value.name));
			xml.append("</" + AUTHOR_ANNOTATION_TYPE + ">");
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
	 * Container for special attributes (affiliation, identifiers) of an author
	 * in a bibliographic reference, facilitating centralized and thus unified
	 * generation of MODS records for reference data.
	 * 
	 * @author sautter
	 */
	public static class AuthorData {
		
		/** the attribute for generic access to the name of an author, namely 'name' */
		public static final String AUTHOR_NAME_ATTRIBUTE = "name";
		
		/** the attribute for storing the affiliation of an author, namely 'affiliation' */
		public static final String AUTHOR_AFFILIATION_ATTRIBUTE = "affiliation";
		
		final String name;
		private HashMap attributes = null;
		//	constructor for normal creation
		AuthorData(String name) {
			this.name = name;
		}
		//	copy constructor
		AuthorData(AuthorData model) {
			this.name = model.name;
			if (model.attributes != null)
				this.attributes = new HashMap(model.attributes);
		}
		//	constructor for name changes
		AuthorData(String name, AuthorData model) {
			this.name = name;
			if (model.attributes != null)
				this.attributes = new HashMap(model.attributes);
		}
		
		/**
		 * Retrieve an attribute of the author.
		 * @param name the name of the attribute
		 * @return the attribute value
		 */
		public String getAttribute(String name) {
			if (AUTHOR_NAME_ATTRIBUTE.equals(name))
				return this.name;
			else if (this.attributes == null)
				return null;
			else return ((String) this.attributes.get(name));
		}
		
		/**
		 * Set an attribute for the author. Setting an attribute to null will
		 * remove it. The author name cannot be set this way.
		 * @param name the name of the attribute
		 * @param value the attribute value
		 */
		public void setAttribute(String name, String value) {
			if (AUTHOR_NAME_ATTRIBUTE.equals(name))
				return;
			if (value == null) {
				this.attributes.remove(name);
				if (this.attributes.isEmpty())
					this.attributes = null;
			}
			else {
				if (this.attributes == null)
					this.attributes = new HashMap();
				this.attributes.put(name, value);
			}
		}
		
		/**
		 * Remove an attribute from the author.
		 * @param name the name of the attribute
		 * @return true if the attribute was present and was removed, false
		 *         otherwise
		 */
		public boolean removeAttribute(String name) {
			if (AUTHOR_NAME_ATTRIBUTE.equals(name))
				return false;
			if (this.attributes == null)
				return false;
			return (this.attributes.remove(name) != null);
		}
		
		/**
		 * Retrieve the names of all present attributes.
		 * @return the attribute names
		 */
		public String[] getAttributeNames() {
			TreeSet ans = new TreeSet();
			ans.add(AUTHOR_NAME_ATTRIBUTE);
			if (this.attributes != null)
				ans.addAll(this.attributes.keySet());
			return ((String[]) ans.toArray(new String[ans.size()]));
		}
		
		static final int IGNORE_CASE = 1;
		static final int IGNORE_ACCENTS = 2;
		static final int IGNORE_NON_LETTERS = 4;
		static final int IGNORE_TOKEN_ORDER = 8;
		//	TODO use this fuzzy match !!!
		boolean matches(String name, int mode) {
			String tName = this.name;
			if ((mode & IGNORE_CASE) != 0) {
				name = name.toLowerCase();
				tName = tName.toLowerCase();
			}
			if ((mode & IGNORE_ACCENTS) != 0) {
				name = StringUtils.normalizeString(name);
				tName = StringUtils.normalizeString(tName);
			}
			if ((mode & IGNORE_NON_LETTERS) != 0) {
				name = retainLetters(name);
				tName = retainLetters(tName);
			}
			if ((mode & IGNORE_TOKEN_ORDER) != 0) {
				name = orderTokens(name);
				tName = orderTokens(tName);
			}
			return tName.equals(name);
		}
		private static String retainLetters(String str) {
			StringBuffer lStr = new StringBuffer();
			for (int c = 0; c < str.length(); c++) {
				char ch = str.charAt(c);
				if (Character.isLetterOrDigit(ch))
					lStr.append(ch);
				else if (StringUtils.DASHES.indexOf(ch) != -1)
					lStr.append(ch);
				else if (StringUtils.SPACES.indexOf(ch) != -1)
					lStr.append(' ');
			}
			return lStr.toString();
		}
		private static String orderTokens(String str) {
			TokenSequence tsStr = Gamta.newTokenSequence(str, null);
			if (tsStr.size() < 2)
				return str;
			String[] taStr = new String[tsStr.size()];
			for (int t = 0; t < tsStr.size(); t++)
				taStr[t] = tsStr.valueAt(t);
			Arrays.sort(taStr);
			StringBuffer stStr = new StringBuffer(taStr[0]);
			for (int t = 1; t < taStr.length; t++) {
				stStr.append(' ');
				stStr.append(taStr[t]);
			}
			return stStr.toString();
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
					if (AUTHOR_ANNOTATION_TYPE.equals(genericAttributeNames[a]) || EDITOR_ANNOTATION_TYPE.equals(genericAttributeNames[a])) {
						String[] authorsOrEditors = ((String) genericRef.getAttribute(genericAttributeNames[a])).split("\\s*\\&\\s*");
						for (int d = 0; d < authorsOrEditors.length; d++)
							rd.addAttribute(genericAttributeNames[a], authorsOrEditors[d]);
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
					else {
						String authorName = TokenSequenceUtils.concatTokens(details[d], true, true);
						rd.addAttribute(genericAttributeNames[a], authorName);
						String[] aans = details[d].getAttributeNames();
						for (int aa = 0; aa < aans.length; aa++)
							rd.setAuthorAttribute(authorName, aans[aa], ((String) details[d].getAttribute(aans[aa])));
					}
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
		
		//	get type, induce if necessary
		String type = ((String) genericRef.getAttribute(PUBLICATION_TYPE_ATTRIBUTE));
		if ((type == null) || (type.length() == 0))
			classify(rd);
		else rd.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, type);
		
		//	finally ...
		return rd;
	}
	
	/**
	 * Create a reference data object from qualified attributes of an attribute
	 * bearing object, namely from attributes whose name is prefixed 'mods:'.
	 * This method splits publisher and location from one another if necessary.
	 * @param modsRef the bearer of the MODS attributes
	 * @return a reference data object
	 */
	public static RefData modsAttributesToRefData(Attributed modsRef) {
		return prefixedAttributesToRefData(modsRef, "mods:");
	}
	
	/**
	 * Create a reference data object from qualified attributes of an attribute
	 * bearing object, specifically from attributes whose name bears a given
	 * prefixed. This method splits publisher and location from one another if
	 * necessary. The argument prefix does not necessarily have to be an XML
	 * namespace qualifier.
	 * @param attrRef the bearer of the metadata attributes
	 * @param prefix the prefix identifying the metadata attributes
	 * @return a reference data object
	 */
	public static RefData prefixedAttributesToRefData(Attributed attrRef, String prefix) {
		RefData rd = new RefData();
		boolean gotPartDesignator = false;
		String journalOrPublisher = null;
		
		//	get attribute names
		String[] attributeNames = attrRef.getAttributeNames();
		Arrays.sort(attributeNames); // make damn sure authors come up before associated details
		
		//	transfer attributes
		String[] authorNames = null;
		for (int a = 0; a < attributeNames.length; a++) {
			
			//	this one isn't for us
			if (!attributeNames[a].startsWith(prefix))
				continue;
			
			//	get attribute
			String attributeValue = ((String) attrRef.getAttribute(attributeNames[a]));
			if (attributeValue == null)
				continue;
			attributeValue = attributeValue.trim();
			if (attributeValue.length() == 0)
				continue;
			
			//	trim attribute name for further handling
			String attributeName = attributeNames[a].substring(prefix.length());
			
			//	handle authors and editors separately
			if (AUTHOR_ANNOTATION_TYPE.equals(attributeName) || EDITOR_ANNOTATION_TYPE.equals(attributeName)) {
				String[] authorsOrEditors = attributeValue.split("\\s*\\&\\s*");
				for (int d = 0; d < authorsOrEditors.length; d++)
					rd.addAttribute(attributeName, authorsOrEditors[d]);
				if (AUTHOR_ANNOTATION_TYPE.equals(attributeName))
					authorNames = authorsOrEditors;
				continue;
			}
			
			//	handle author details
			if (attributeName.startsWith(AUTHOR_ANNOTATION_TYPE + "-")) {
				if (authorNames == null)
					continue;
				String authorDetailName = attributeName.substring((AUTHOR_ANNOTATION_TYPE + "-").length());
				String[] authorDetails = attributeValue.split("\\s*\\&\\&\\&\\s*");
				for (int d = 0; d < Math.min(authorNames.length, authorDetails.length); d++) {
					if (authorDetails[d].length() != 0)
						rd.setAuthorAttribute(authorNames[d], authorDetailName, authorDetails[d]);
				}
				continue;
			}
			
			//	transfer part designators type specifically
			if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(attributeName)) {
				gotPartDesignator = true;
				if (!rd.hasAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE))
					rd.setAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE, attributeValue);
				else if (!rd.hasAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE))
					rd.setAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE, attributeValue);
				else if (!rd.hasAttribute(NUMERO_DESIGNATOR_ANNOTATION_TYPE))
					rd.setAttribute(NUMERO_DESIGNATOR_ANNOTATION_TYPE, attributeValue);
				continue;
			}
			
			//	remember we have a part designator
			if (VOLUME_DESIGNATOR_ANNOTATION_TYPE.equals(attributeName) || ISSUE_DESIGNATOR_ANNOTATION_TYPE.equals(attributeName) || NUMERO_DESIGNATOR_ANNOTATION_TYPE.equals(attributeName))
				gotPartDesignator = true;
			
			//	handle journal/publisher later (part designators might come only after this attribute !!!)
			if (JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE.equals(attributeName)) {
				journalOrPublisher = attributeValue;
				continue;
			}
			
			//	handle IDs
			if (attributeName.startsWith(RefData.ID_PREFIX)) {
				rd.setIdentifier(attributeName.substring(RefData.ID_PREFIX.length()), attributeValue);
				continue;
			}
			
			//	handle other details
			rd.addAttribute(attributeName, attributeValue);
		}
		
		//	handle journal/publisher only now that we know through the end if there is a part designator
		if (journalOrPublisher != null) {
			
			//	journal name
			if (gotPartDesignator)
				rd.setAttribute(JOURNAL_NAME_ANNOTATION_TYPE, journalOrPublisher);
			
			//	split publisher from location
			else {
				int split = journalOrPublisher.indexOf(":");
				if (split == -1)
					split = journalOrPublisher.indexOf(",");
				if (split == -1)
					rd.setAttribute(PUBLISHER_ANNOTATION_TYPE, journalOrPublisher);
				else if (journalOrPublisher.charAt(split) == ':') {
					rd.setAttribute(PUBLISHER_ANNOTATION_TYPE, journalOrPublisher.substring(split + ":".length()).trim());
					rd.setAttribute(LOCATION_ANNOTATION_TYPE, journalOrPublisher.substring(0, split).trim());
				}
				else {
					rd.setAttribute(LOCATION_ANNOTATION_TYPE, journalOrPublisher.substring(split + ",".length()).trim());
					rd.setAttribute(PUBLISHER_ANNOTATION_TYPE, journalOrPublisher.substring(0, split).trim());
				}
			}
		}
		
		//	get type, induce if necessary
		String type = ((String) attrRef.getAttribute(prefix + PUBLICATION_TYPE_ATTRIBUTE));
		if ((type == null) || (type.length() == 0))
			classify(rd);
		else rd.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, type);
		
		//	finally ...
		return rd;
	}
	
	/**
	 * Store details of a bibliographic reference in its object representation
	 * in qualified attributes of an attribute bearing object. The attributes
	 * of the argument reference are stored in equally-named attributes to the
	 * argument attribute bearer, prefixed with 'mods:'. Further, any 'mods:'
	 * prefixed attributes that are not present in the argument reference
	 * object are removed.
	 * @param target the object to add the reference attributes to
	 * @param ref the bibliographic reference to store
	 */
	public static void toModsAttributes(RefData ref, Attributed target) {
		toPrefixedAttributes(ref, target, "mods:");
	}
	
	/**
	 * Store details of a bibliographic reference in its object representation
	 * in qualified attributes of an attribute bearing object. The attributes
	 * of the argument reference are stored in equally-named attributes to the
	 * argument attribute bearer, prefixed with the argument prefix. Further,
	 * any attributes with the same prefix that are not present in the argument
	 * reference object are removed. The argument prefix does not necessarily
	 * have to be an XML namespace qualifier, but must form valid XML attribute
	 * names in combination with the generic bibliographic attribute names.
	 * @param target the object to add the reference attributes to
	 * @param ref the bibliographic reference to store
	 * @param prefix the prefix to use for identifying the attribute names
	 */
	public static void toPrefixedAttributes(RefData ref, Attributed target, String prefix) {
		
		//	collect existing attributes
		String[] targetAttributeNames = target.getAttributeNames();
		HashSet spuriousTargetAttributeNames = new HashSet();
		for (int n = 0; n < targetAttributeNames.length; n++) {
			if (targetAttributeNames[n].startsWith(prefix))
				spuriousTargetAttributeNames.add(targetAttributeNames[n].substring(prefix.length()));
		}
		
		//	set ID attributes
		String[] idTypes = ref.getIdentifierTypes();
		for (int i = 0; i < idTypes.length; i++) {
			if (!AnnotationUtils.isValidAnnotationType(idTypes[i]))
				continue;
			String id = ref.getIdentifier(idTypes[i]);
			if ((id != null) && (id.trim().length() != 0)) {
				target.setAttribute((prefix + RefData.ID_PREFIX + idTypes[i]), id.trim());
				spuriousTargetAttributeNames.remove(RefData.ID_PREFIX + idTypes[i]);
			}
		}
		
		//	store all attributes explicitly
		String[] refAttributeNames = ref.getAttributeNames();
		for (int n = 0; n < refAttributeNames.length; n++) {
			String attributeValue;
			if (AUTHOR_ANNOTATION_TYPE.equals(refAttributeNames[n]) || EDITOR_ANNOTATION_TYPE.equals(refAttributeNames[n]))
				attributeValue = ref.getAttributeValueString(refAttributeNames[n], " & ");
			else attributeValue = ref.getAttribute(refAttributeNames[n]);
			if (attributeValue == null)
				continue;
			attributeValue = attributeValue.trim();
			if (attributeValue.length() == 0)
				continue;
			target.setAttribute((prefix + refAttributeNames[n]), attributeValue);
			spuriousTargetAttributeNames.remove(refAttributeNames[n]);
		}
		
		//	persist author details
		String[] refAuthorNames = ref.getAttributeValues(AUTHOR_ANNOTATION_TYPE);
		if (refAuthorNames != null) {
			String[] authorAttributeNames = ref.getAuthorAttributeNames();
			for (int n = 0; n < authorAttributeNames.length; n++) {
				if (AuthorData.AUTHOR_NAME_ATTRIBUTE.equals(authorAttributeNames[n]))
					continue;
				StringBuffer attributeValue = new StringBuffer();
				for (int a = 0; a < refAuthorNames.length; a++) {
					if (a != 0)
						attributeValue.append(" &&& ");
					String authorAttributeValue = ref.getAuthorAttribute(refAuthorNames[a], authorAttributeNames[n]);
					if (authorAttributeValue != null)
						attributeValue.append(authorAttributeValue);
				}
				target.setAttribute((prefix + AUTHOR_ANNOTATION_TYPE + "-" + authorAttributeNames[n]), attributeValue.toString());
				spuriousTargetAttributeNames.remove(AUTHOR_ANNOTATION_TYPE + "-" + authorAttributeNames[n]);
			}
		}
		
		//	remove spurious attributes
		for (Iterator sanit = spuriousTargetAttributeNames.iterator(); sanit.hasNext();) {
			String spuriousDocAttributeName = ((String) sanit.next());
			target.removeAttribute(prefix + spuriousDocAttributeName);
		}
	}
	
	/**
	 * Transform a bibliographic reference in MODS XML format into a reference
	 * data object.
	 * @param modsRef the annotation to transform
	 * @return a reference data object representing the argument reference
	 */
	public static RefData modsXmlToRefData(QueriableAnnotation modsRef) {
		
		//	copy document
		modsRef = Gamta.copyDocument(modsRef);
		
		//	remove namespace prefix from annotations
		Annotation[] annotations = modsRef.getAnnotations();
		for (int a = 0; a < annotations.length; a++) {
			String annotationType = annotations[a].getType();
			if (annotationType.indexOf(':') != -1)
				annotations[a].changeTypeTo(annotationType.substring(annotationType.indexOf(':') + ":".length()));
		}
		
		//	do data extraction
		RefData rd = new RefData();
		extractAuthors(modsRef, rd);
		QueriableAnnotation[] originInfo = originInfoPath.evaluate(modsRef, null);
		if (originInfo.length != 0)
			extractDetails(originInfo[0], originInfoDetailPathsByType, rd);
		QueriableAnnotation[] hostItem = hostItemPath.evaluate(modsRef, null);
		if (hostItem.length != 0)
			extractDetails(hostItem[0], hostItemDetailPathsByType, rd);
		extractDetails(modsRef, TITLE_ANNOTATION_TYPE, titlePath, rd);
		extractDetails(modsRef, BOOK_CONTENT_INFO_ANNOTATION_TYPE, bookContentInfoPath, rd);
		
		//	get publication URL
		Annotation[] urlAnnots = publicationUrlPath.evaluate(modsRef, null);
		if ((urlAnnots != null) && (urlAnnots.length != 0))
			rd.setAttribute(PUBLICATION_URL_ANNOTATION_TYPE, TokenSequenceUtils.concatTokens(urlAnnots[0], false, true).replaceAll("\\s+", ""));
		
		//	store identifiers
		Annotation[] idAnnots = modsRef.getAnnotations("identifier");
		for (int i = 0; i < idAnnots.length; i++) {
			String type = ((String) idAnnots[i].getAttribute("type"));
			if (type != null)
				rd.setIdentifier(type, idAnnots[i].getValue());
		}
		
		//	get reference type
		Annotation[] typeAnnots = modsRef.getAnnotations("classification");
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
		
		//	handle book content info
		if ("book".equals(type)) {
			Annotation[] infoAnnots = modsRef.getAnnotations("note");
			for (int i = 0; i < infoAnnots.length; i++)
				rd.addAttribute(BOOK_CONTENT_INFO_ANNOTATION_TYPE, infoAnnots[i].getValue());
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
			rd.addAttribute(type, TokenSequenceUtils.concatTokens(details[d], !PUBLICATION_DATE_ANNOTATION_TYPE.equals(type), true));
			if (TITLE_ANNOTATION_TYPE.equals(type))
				break;
		}
	}
	private static void extractAuthors(QueriableAnnotation modsRef, RefData rd) {
		QueriableAnnotation[] authors = authorsPath.evaluate(modsRef, null);
		for (int a = 0; a < authors.length; a++) {
			Annotation[] nameParts = authors[a].getAnnotations("namePart");
			if (nameParts.length == 0)
				continue;
			
			String authorName = TokenSequenceUtils.concatTokens(nameParts[0], true, true);
			rd.addAttribute(AUTHOR_ANNOTATION_TYPE, authorName);
			
			Annotation[] affiliations = authors[a].getAnnotations("affiliation");
			if (affiliations.length != 0) {
				StringBuffer affiliation = new StringBuffer();
				for (int f = 0; f < affiliations.length; f++) {
					if (f != 0)
						affiliation.append(" & ");
					affiliation.append(TokenSequenceUtils.concatTokens(affiliations[f], true, true));
				}
				rd.setAuthorAttribute(authorName, AuthorData.AUTHOR_AFFILIATION_ATTRIBUTE, affiliation.toString());
			}
			
			Annotation[] nameIdentifiers = authors[a].getAnnotations("nameIdentifier");
			for (int i = 0; i < nameIdentifiers.length; i++) {
				String type = ((String) nameIdentifiers[i].getAttribute("type"));
				if (type != null)
					rd.setAuthorAttribute(authorName, type, nameIdentifiers[i].getValue());
			}
		}
	}
	
	private static final GPath titlePath = new GPath("//titleInfo/title");
	
	private static final GPath authorsPath = new GPath("//name[.//roleTerm = 'Author']");
	
	private static final GPath bookContentInfoPath = new GPath("//note");
	
	private static final GPath publicationUrlPath = new GPath("//location/url");
	
	private static final GPath hostItemPath = new GPath("//relatedItem[./@type = 'host']");
	private static final GPath hostItem_titlePath = new GPath("//titleInfo/title");
	private static final GPath hostItem_volumeNumberPath = new GPath("//part/detail[(./@type = 'volume') or (./@type = 'issue') or (./@type = 'numero')]/number");
	private static final GPath hostItem_volumeTitlePath = new GPath("//part/detail[./@type = 'title']/title");
	private static final GPath hostItem_startPagePath = new GPath("//part/extent[./@unit = 'page']/start");
	private static final GPath hostItem_endPagePath = new GPath("//part/extent[./@unit = 'page']/end");
	private static final GPath hostItem_datePath = new GPath("//part/date");
	private static final GPath hostItem_pubDatePath = new GPath("//part/detail[./@type = 'pubDate']/number");
	private static final GPath hostItem_editorsPath = new GPath("//name[.//roleTerm = 'Editor']/namePart");
	
	private static final GPath hostItem_volumePath = new GPath("//part/detail[./@type = 'volume']/number");
	private static final GPath hostItem_issuePath = new GPath("//part/detail[./@type = 'issue']/number");
	private static final GPath hostItem_numeroPath = new GPath("//part/detail[./@type = 'numero']/number");
	
	private static final GPath originInfoPath = new GPath("//originInfo");
	private static final GPath originInfo_publisherNamePath = new GPath("//publisher");
	private static final GPath originInfo_publisherLocationPath = new GPath("//place/placeTerm");
	private static final GPath originInfo_issueDatePath = new GPath("//dateIssued");
	private static final GPath originInfo_pubDatePath = new GPath("//dateOther[./@type = 'pubDate']");
	private static final GPath originInfo_accessDatePath = new GPath("//dateCaptured");
	
//	private static LinkedHashMap baseDetailPathsByType = new LinkedHashMap();
//	static {
//		baseDetailPathsByType.put(TITLE_ANNOTATION_TYPE, titlePath);
//		baseDetailPathsByType.put(AUTHOR_ANNOTATION_TYPE, authorsPath);
//		baseDetailPathsByType.put(BOOK_CONTENT_INFO_ANNOTATION_TYPE, bookContentInfoPath);
//	}
	private static LinkedHashMap hostItemDetailPathsByType = new LinkedHashMap();
	static {
		hostItemDetailPathsByType.put(PART_DESIGNATOR_ANNOTATION_TYPE, hostItem_volumeNumberPath);
		hostItemDetailPathsByType.put(YEAR_ANNOTATION_TYPE, hostItem_datePath);
		hostItemDetailPathsByType.put(PUBLICATION_DATE_ANNOTATION_TYPE, hostItem_pubDatePath);
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
		originInfoDetailPathsByType.put(PUBLICATION_DATE_ANNOTATION_TYPE, originInfo_pubDatePath);
		originInfoDetailPathsByType.put(ACCESS_DATE_ANNOTATION_TYPE, originInfo_accessDatePath);
		originInfoDetailPathsByType.put(PUBLISHER_ANNOTATION_TYPE, originInfo_publisherNamePath);
		originInfoDetailPathsByType.put(LOCATION_ANNOTATION_TYPE, originInfo_publisherLocationPath);
	}
	
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
	 * @param xslt the XSL transformer generating the reference style from the
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
	 * @param xslt the XSL transformer generating the reference objects from
	 *            the XML representation of reference data sets
	 * @param outputIsXml is the output of the transformer XML?
	 * @return true if the type was newly added
	 */
	public static boolean addRefObjectType(String name, Transformer xslt, boolean outputIsXml) {
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
		PUBLICATION_DATE_ANNOTATION_TYPE,
		ACCESS_DATE_ANNOTATION_TYPE,
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
		BOOK_CONTENT_INFO_ANNOTATION_TYPE,
	};
	private static final String[] genericAttributeNames = {
		AUTHOR_ANNOTATION_TYPE,
		YEAR_ANNOTATION_TYPE,
		PUBLICATION_DATE_ANNOTATION_TYPE,
		ACCESS_DATE_ANNOTATION_TYPE,
		TITLE_ANNOTATION_TYPE,
		VOLUME_DESIGNATOR_ANNOTATION_TYPE,
		ISSUE_DESIGNATOR_ANNOTATION_TYPE,
		NUMERO_DESIGNATOR_ANNOTATION_TYPE,
		PART_DESIGNATOR_ANNOTATION_TYPE,
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
		BOOK_CONTENT_INFO_ANNOTATION_TYPE,
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
	 * attributes to a given attributed object. If the argument attributed
	 * object is a <code>DocumentRoot</code> instance, the meta data is
	 * additionally deposited in document properties.
	 * @param data the attributed object to add the attributes to
	 * @param mods the MODS header to take the attributes from
	 */
	public static void setDocAttributes(Attributed data, QueriableAnnotation mods) {
		setDocAttributes(data, modsXmlToRefData(mods));
	}
	
	/**
	 * Add document meta data from a reference data set as attributes to a
	 * given attributed object. If the argument attributed object is a
	 * <code>DocumentRoot</code> instance, the meta data is additionally
	 * deposited in document properties.
	 * @param data the documents to add the attributes to
	 * @param ref the reference data set to take the attributes from
	 */
	public static void setDocAttributes(Attributed data, RefData ref) {
		String[] idTypes = ref.getIdentifierTypes();
		for (int i = 0; i < idTypes.length; i++) {
			if (!AnnotationUtils.isValidAnnotationType(idTypes[i]))
				continue;
			String id = ref.getIdentifier(idTypes[i]);
			if ((id != null) && (id.trim().length() != 0))
				setAttribute(data, ("ID-" + idTypes[i]), id.trim());
		}
		
		setAttribute(data, DOCUMENT_AUTHOR_ATTRIBUTE, ref, AUTHOR_ANNOTATION_TYPE, false);
		setAttribute(data, DOCUMENT_TITLE_ATTRIBUTE, ref, TITLE_ANNOTATION_TYPE, true);
		setAttribute(data, DOCUMENT_DATE_ATTRIBUTE, ref, YEAR_ANNOTATION_TYPE, true);
		setAttribute(data, DOCUMENT_SOURCE_LINK_ATTRIBUTE, ref, PUBLICATION_URL_ANNOTATION_TYPE, true);
		
		if (ref.hasAttribute(PAGINATION_ANNOTATION_TYPE) || ref.hasAttribute(VOLUME_TITLE_ANNOTATION_TYPE)) {
			String origin = BibRefTypeSystem.getDefaultInstance().getOrigin(ref);
			if ((origin != null) && (origin.trim().length() != 0))
				setAttribute(data, DOCUMENT_ORIGIN_ATTRIBUTE, origin.trim());
			String pagination = ref.getAttribute(PAGINATION_ANNOTATION_TYPE);
			if (pagination != null) {
				String[] pns = pagination.trim().split("\\s*\\-+\\s*");
				if (pns.length == 1)
					data.setAttribute(PAGE_NUMBER_ATTRIBUTE, pns[0]);
				else if (pns.length == 2) {
					data.setAttribute(PAGE_NUMBER_ATTRIBUTE, pns[0]);
					data.setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, pns[1]);
				}
			}
		}
	}
	
	private static void setAttribute(Attributed doc, String dan, RefData ref, String ran, boolean onlyFirst) {
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
	
	private static void setAttribute(Attributed doc, String name, String value) {
		doc.setAttribute(name, value);
		if (doc instanceof DocumentRoot)
			((DocumentRoot) doc).setDocumentProperty(name, value);
	}
	
	/**
	 * Clean out a MODS document header, i.e., remove all annotations that do
	 * not belong to the MODS namespace, and set the namespace URI.
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
	
	private static class BibRefStringFunction implements GPathFunction {
		public GPathObject execute(Annotation contextAnnotation, int contextPosition, int contextSize, GPathObject[] args) throws GPathException {
			int argPos = 0;
			
			//	get reference data bearer
			Annotation refAnnotation;
			if (args.length == argPos)
				refAnnotation = contextAnnotation;
			else if (args[argPos] instanceof GPathAnnotationSet)
				refAnnotation = ((GPathAnnotationSet) args[argPos++]).getFirst();
			else refAnnotation = contextAnnotation;
			
			//	get reference style (optional)
			String refStyle;
			if (args.length == argPos)
				refStyle = null;
			else if (args[argPos] instanceof GPathString)
				refStyle = ((GPathString) args[argPos++]).value;
			else refStyle = null;
			
			//	check arguments
			if (argPos != args.length)
				throw new InvalidArgumentsException("The function 'bibRefString' requires at most 2 arguments, one of type GPathAnnotationSet and one of type GPathString.");
			
			//	nothing to work with (have to check arguments first, though)
			if (refAnnotation == null)
				return new GPathString("");
			
			//	get reference data
			RefData ref = null;
			if ((ref == null) && (refAnnotation instanceof QueriableAnnotation) && (refAnnotation.getType().equals("mods") || refAnnotation.getType().endsWith(":mods")))
				ref = checkRefData(modsXmlToRefData((QueriableAnnotation) refAnnotation));
			if ((ref == null) && (refAnnotation instanceof QueriableAnnotation))
				ref = checkRefData(genericXmlToRefData((QueriableAnnotation) refAnnotation));
			if (ref == null)
				ref = checkRefData(modsAttributesToRefData(refAnnotation));
			if (ref == null)
				return new GPathString("");
			
			//	generate and return reference string
			String refString = (((refStyle != null) && refStringStyles.containsKey(refStyle)) ? toRefString(ref, refStyle) : toRefString(ref));
			return new GPathString((refString == null) ? "" : refString.trim());
		}
	}
	
	private static class BibRefPersonsFunction implements GPathFunction {
		private String personType;
		BibRefPersonsFunction(String personType) {
			this.personType = personType;
		}
		public GPathObject execute(Annotation contextAnnotation, int contextPosition, int contextSize, GPathObject[] args) throws GPathException {
			int argPos = 0;
			
			//	get reference data bearer
			Annotation refAnnotation;
			if (args.length == argPos)
				refAnnotation = contextAnnotation;
			else if (args[argPos] instanceof GPathAnnotationSet)
				refAnnotation = ((GPathAnnotationSet) args[argPos++]).getFirst();
			else refAnnotation = contextAnnotation;
			
			//	get author count limit
			int pCountLimit;
			if (args.length == argPos)
				pCountLimit = 0;
			else if (args[argPos] instanceof GPathNumber)
				pCountLimit = ((int) ((GPathNumber) args[argPos++]).value);
			else pCountLimit = 0;
			if (pCountLimit <= 0)
				pCountLimit = Integer.MAX_VALUE;
			
			//	get output length limit
			int pStringLengthLimit;
			if (args.length == argPos)
				pStringLengthLimit = 0;
			else if (args[argPos] instanceof GPathNumber)
				pStringLengthLimit = ((int) ((GPathNumber) args[argPos++]).value);
			else pStringLengthLimit = 0;
			if (pStringLengthLimit <= 0)
				pStringLengthLimit = Integer.MAX_VALUE;
			
			//	get separator
			String pSeparator;
			if (args.length == argPos)
				pSeparator = " & ";
			else if (args[argPos] instanceof GPathString)
				pSeparator = ((GPathString) args[argPos++]).value;
			else pSeparator = " & ";
			
			//	get last separator
			String pLastSeparator;
			if (args.length == argPos)
				pLastSeparator = pSeparator;
			else if (args[argPos] instanceof GPathString)
				pLastSeparator = ((GPathString) args[argPos++]).value;
			else pLastSeparator = pSeparator;
			
			//	check arguments
			if (argPos != args.length)
				throw new InvalidArgumentsException("The function 'bibRef" + this.personType.substring(0, 1).toUpperCase() + this.personType.substring(1) + "s' requires at most 5 arguments, one of type GPathAnnotationSet and two of type GPathNumber, and two of type GPathString.");
			
			//	nothing to work with (have to check arguments first, though)
			if (refAnnotation == null)
				return new GPathString("");
			
			//	get reference data
			RefData ref = null;
			if ((ref == null) && (refAnnotation instanceof QueriableAnnotation) && (refAnnotation.getType().equals("mods") || refAnnotation.getType().endsWith(":mods")))
				ref = checkRefData(modsXmlToRefData((QueriableAnnotation) refAnnotation));
			if ((ref == null) && (refAnnotation instanceof QueriableAnnotation))
				ref = checkRefData(genericXmlToRefData((QueriableAnnotation) refAnnotation));
			if (ref == null)
				ref = checkRefData(modsAttributesToRefData(refAnnotation));
			if (ref == null)
				return new GPathString("");
			
			//	get authors
			String[] refPersons = ref.getAttributeValues(this.personType);
			if (refPersons.length == 0)
				return new GPathString("");
			
			//	enforce limits on data
			int pStringLength = refPersons[0].length();
			for (int p = 1; p < Math.min(pCountLimit, refPersons.length); p++) {
				/* count separator, and count terminal separator first, will
				 * always occur exactly once, and this way, we don't have to
				 * fiddle around with when exactly it occurs */
				int pSeparatorLength = ((p == 1) ? pLastSeparator.length() : pSeparator.length());
				
				//	count in author plus preceding separator
				pStringLength += (pSeparatorLength + refPersons[p].length());
				
				//	string length limit exceeded, we have to stop one earlier
				if (pStringLength > pStringLengthLimit) {
					pCountLimit = p;
					break;
				}
			}
			
			//	generate and return author string
			StringBuffer pString = new StringBuffer(refPersons[0]);
			for (int p = 1; p < Math.min(pCountLimit, refPersons.length); p++) {
				if ((p+1) == Math.min(pCountLimit, refPersons.length))
					pString.append(pLastSeparator);
				else pString.append(pSeparator);
				pString.append(refPersons[p]);
			}
			return new GPathString(pString.toString());
		}
	}
	private static RefData checkRefData(RefData ref) {
		if (ref == null)
			return ref;
		String type = ref.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		if (type == null)
			type = classify(ref);
		return ((type == null) ? null : ref);
	}
	static {
		GPath.addFunction("bibRefString", new BibRefStringFunction());
		GPath.addFunction("bibRefAuthors", new BibRefPersonsFunction(AUTHOR_ANNOTATION_TYPE));
		GPath.addFunction("bibRefEditors", new BibRefPersonsFunction(EDITOR_ANNOTATION_TYPE));
	}
//	
//	public static void main(String[] args) throws Exception {
//		RefData rd = new RefData();
//		rd.addAttribute(AUTHOR_ANNOTATION_TYPE, "Doe, John");
//		rd.setAuthorAttribute("Doe, John", "affiliation", "John Doe Group");
//		rd.setAuthorAttribute("Doe, John", "ORCID", "123");
//		rd.setAuthorAttribute("Doe, Jane", "affiliation", "John Doe Group");
//		rd.setAuthorAttribute("Doe, Jane", "ORCID", "456");
//		System.out.println(Arrays.toString(rd.getAttributeValues(AUTHOR_ANNOTATION_TYPE)));
//		System.out.println(rd.getAttributeValueString(AUTHOR_ANNOTATION_TYPE, " & "));
////		rd.renameAttribute(AUTHOR_ANNOTATION_TYPE, "A");
////		rd.renameAttribute("A", AUTHOR_ANNOTATION_TYPE);
//		rd.setAttribute(YEAR_ANNOTATION_TYPE, "2020");
//		rd.setAttribute(TITLE_ANNOTATION_TYPE, "This is the title");
//		rd.setAttribute(JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE, "The Journal");
//		rd.setAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE, "8");
//		rd.setAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE, "15");
//		rd.setAttribute(PAGINATION_ANNOTATION_TYPE, "8-15");
//		String xml = rd.toXML();
//		System.out.println(xml);
//		RefData xrd = genericXmlToRefData(SgmlDocumentReader.readDocument(new StringReader(xml)));
//		System.out.println(xrd.toXML());
//		
//		System.out.println();
//		System.out.println();
//		String mods = toModsXML(rd);
//		System.out.println(mods);
//		RefData mrd = modsXmlToRefData(SgmlDocumentReader.readDocument(new StringReader(mods)));
//		System.out.println(mrd.toXML());
//	}
//	
//	public static void main(String[] args) throws Exception {
//		BibRefTypeSystem brtSys = BibRefTypeSystem.getDefaultInstance();
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/SMNK-Projekt/EcologyTestbed/Taylor_Wolters_2005.normalized.xml"));
//		AnnotationFilter.renameAnnotations(doc, CITATION_TYPE, BIBLIOGRAPHIC_REFERENCE_TYPE);
//		AnnotationFilter.renameAnnotations(doc, "hostTitle", VOLUME_TITLE_ANNOTATION_TYPE);
//		AnnotationFilter.renameAnnotationAttribute(doc, BIBLIOGRAPHIC_REFERENCE_TYPE, "hostTitle", VOLUME_TITLE_ANNOTATION_TYPE);
//		MutableAnnotation[] bibRefs = doc.getMutableAnnotations(BIBLIOGRAPHIC_REFERENCE_TYPE);
//		for (int r = 0; r < bibRefs.length; r++) try {
//			System.out.println(TokenSequenceUtils.concatTokens(bibRefs[r], true, true));
//			RefData rd = genericXmlToRefData(bibRefs[r]);
//			String type = brtSys.classify(rd);
////			if (type == null) {
////				BibRefType[] brts = brtSys.getBibRefTypes();
////				for (int t = 0; t < brts.length; t++) {
////					String[] errors = brts[t].getErrors(rd);
////					if (errors == null) {
////						type = brts[t].name;
////						break;
////					}
////					else {
////						System.out.println(" === " + brts[t].name + " ===");
////						for (int e = 0; e < errors.length; e++)
////							System.out.println(errors[e]);
////					}
////				}
////			}
//			System.out.println(" ==> " + type);
//			if (type == null)
//				continue;
//			BibRefType brt = brtSys.getBibRefType(type);
//			String origin = brt.getOrigin(rd);
//			System.out.println(origin);
//			if (origin.startsWith(","))
//				AnnotationUtils.writeXML(bibRefs[r], new PrintWriter(System.out));
//			System.out.println(rd.toXML());
////			System.out.println(toModsXML(rd));
////			System.out.println(toRefString(rd));
//		}
//		catch (Exception e) {
//			AnnotationUtils.writeXML(bibRefs[r], new PrintWriter(System.out));
//			throw e;
//		}
//	}
}
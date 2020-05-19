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
 *     * Neither the name of the Universitaet Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.gamta.util.markupScript;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed;
import de.uka.ipd.idaho.gamta.util.markupScript.MarkupScriptTypes.MsArray;

/**
 * Type library for MarkupScript, providing conversion and wrapper functions.
 * 
 * @author sautter
 */
public class MarkupScriptTypes {
	
	public static final String OBJECT = "var";
	
	public static final String BOOLEAN = "boolean";
	
	public static final String NUMBER = "number";
	
	public static final String STRING = "string";
	
	public static final String MAP = "map";
	
	public static final String ARRAY = "array";
	
	public static final String TOKEN = "token";
	
	public static final String TOKEN_SEQUENCE = "tokseq";
	
	public static final String ANNOTATION = "annot";
	
	public static final String DOCUMENT = "doc";
	
	/**
	 * Root of the type system, enforcing conversion along the lines of XPath.
	 * 
	 * @author sautter
	 */
	public static interface MsObject {

		/**
		 * Get the object as a boolean value (need this for XPath)
		 * @return the object as a boolean
		 */
		public abstract MsBoolean asBoolean();

		/**
		 * Get the object as a number (need this for XPath)
		 * @return the object as a number
		 */
		public abstract MsNumber asNumber();

		/**
		 * Get the object as a string (need this for XPath)
		 * @return the object as a string
		 */
		public abstract MsString asString();
		
		/**
		 * Get the underlying object. This method is exclusively for Java based
		 * system function providers to work with.
		 * @return the underlying object
		 */
		public abstract Object getNativeObject();
	}
	
	/**
	 * Wrap an object into a MarkupScript object.
	 * @param obj the object to wrap
	 * @return the wrapped object
	 */
	public static MsObject wrapObject(Object obj) {
		if (obj instanceof MsObject)
			return ((MsObject) obj);
		
		else if (obj instanceof MutableAnnotation)
			return wrapDocument((MutableAnnotation) obj);
		else if (obj instanceof Annotation)
			return wrapAnnotation((Annotation) obj);
		else if (obj instanceof TokenSequence)
			return wrapTokenSequence((TokenSequence) obj);
		else if (obj instanceof Token)
			return wrapToken((Token) obj);
		else if (obj instanceof Attributed)
			return wrapMap((Attributed) obj);
		
		else if (obj instanceof CharSequence)
			return wrapString((CharSequence) obj);
		
		else if (obj instanceof Map)
			return wrapMap((Map) obj);
		
		else if (obj instanceof List)
			return wrapList((List) obj);
		
		else if (obj instanceof Double)
			return wrapDouble(((Double) obj).doubleValue());
		else if (obj instanceof Float)
			return wrapDouble(((Float) obj).doubleValue());
		else if (obj instanceof Number)
			return wrapInteger(((Number) obj).intValue());
		
		else if (obj instanceof Boolean)
			return wrapBoolean(((Boolean) obj).booleanValue());
		
		else if (obj == null)
			return null;
		else return wrapString(obj.toString());
	}
	
	/**
	 * A boolean value, true or false
	 * 
	 * @author sautter
	 */
	public static interface MsBoolean extends MsObject {
		
		/**
		 * Get the boolean value of the boolean, true or false
		 * @return the boolean value
		 */
		public abstract boolean getValue();
		
		/**
		 * Get the underlying boolean. This method is exclusively for Java based
		 * system function providers to work with.
		 * @return the underlying boolean
		 */
		public abstract Boolean getNativeBoolean();
	}
	
	/**
	 * Wrap a boolean into a MarkupScript object.
	 * @param b the boolean to wrap
	 * @return the wrapped boolean
	 */
	public static MsBoolean wrapBoolean(boolean b) {
		return (b ? TRUE : FALSE);
	}
	
	private static final DefaultBoolean TRUE = new DefaultBoolean(true);
	private static final DefaultBoolean FALSE = new DefaultBoolean(false);
	
	private static class DefaultBoolean implements MsBoolean {
		private boolean value;
		DefaultBoolean(boolean value) {
			this.value = value;
		}
		public MsBoolean asBoolean() {
			return this;
		}
		public MsNumber asNumber() {
			return wrapInteger(this.value ? 1 : 0);
		}
		public MsString asString() {
			return wrapString(Boolean.toString(this.value));
		}
		public Object getNativeObject() {
			return this.getNativeBoolean();
		}
		public boolean getValue() {
			return this.value;
		}
		public Boolean getNativeBoolean() {
			return (this.value ? Boolean.TRUE : Boolean.FALSE); // avoids creating new object
		}
	}
	
	/**
	 * A number
	 * 
	 * @author sautter
	 */
	public static interface MsNumber extends MsObject {
		
		/**
		 * Get the (maybe rounded) integer value of the number
		 * @return the integer value of the number, maybe rounded
		 */
		public abstract int intValue();
		
		/**
		 * Get the double value of the number
		 * @return the double value
		 */
		public abstract double getValue();
		
		/**
		 * Get the underlying number. This method is exclusively for Java based
		 * system function providers to work with.
		 * @return the underlying number
		 */
		public abstract Number getNativeNumber();
	}
	
	/**
	 * Wrap an integer into a MarkupScript number.
	 * @param i the integer to wrap
	 * @return the wrapped integer
	 */
	public static MsNumber wrapInteger(Integer i) {
		return new DefaultNumber((i == null) ? 0 : i.intValue());
	}
	
	/**
	 * Wrap a double into a MarkupScript number.
	 * @param d the double to wrap
	 * @return the wrapped double
	 */
	public static MsNumber wrapDouble(Double d) {
		return new DefaultNumber((d == null) ? 0.0 : d.doubleValue());
	}
	
	private static class DefaultNumber implements MsNumber {
		private double value;
		private boolean isInt;
		DefaultNumber(int value) {
			this.value = value;
			this.isInt = true;
		}
		DefaultNumber(double value) {
			this.value = value;
			this.isInt = false;
		}
		public MsBoolean asBoolean() {
			return wrapBoolean(this.value > 0);
		}
		public MsNumber asNumber() {
			return this;
		}
		public MsString asString() {
			return wrapString(this.isInt ? Integer.toString(this.intValue()) : Double.toString(this.value));
		}
		public Object getNativeObject() {
			return this.getNativeNumber();
		}
		public double getValue() {
			return this.value;
		}
		public int intValue() {
			return ((int) Math.round(this.value));
		}
		public Number getNativeNumber() {
			return (this.isInt ? new Integer(this.intValue()) : new Double(this.value));
		}
	}
	
	/**
	 * A string, i.e., a sequence of characters
	 * 
	 * @author sautter
	 */
	public static interface MsString extends MsObject {
		
		/**
		 * Get the plain value of the string
		 * @return the value
		 */
		public abstract String getValue();
		
		/**
		 * Get the underlying string. This method is exclusively for Java based
		 * system function providers to work with.
		 * @return the underlying string
		 */
		public abstract CharSequence getNativeString();
	}
	
	/**
	 * Wrap a character sequence into a MarkupScript string.
	 * @param cs the character sequence to wrap
	 * @return the wrapped character sequence
	 */
	public static MsString wrapString(CharSequence cs) {
		return new DefaultString((cs == null) ? "" : cs);
	}
	
	private static class DefaultString implements MsString {
		private CharSequence value;
		DefaultString(CharSequence value) {
			this.value = value;
		}
		public MsBoolean asBoolean() {
			return wrapBoolean(this.value.length() != 0);
		}
		public MsNumber asNumber() {
			//	TODO try integer first?
			try {
				double d = Double.parseDouble(this.value.toString());
				return wrapDouble(d);
			} catch (NumberFormatException nfe) {
				return new DefaultNumber(Double.NaN);
			}
		}
		public MsString asString() {
			return this;
		}
		public Object getNativeObject() {
			return this.getNativeString();
		}
		public String getValue() {
			return this.value.toString();
		}
		public CharSequence getNativeString() {
			return this.value;
		}
	}
	
	/**
	 * An array of objects, corresponding to a JSON array
	 * 
	 * @author sautter
	 */
	public static interface MsArray extends MsObject {
		
		/**
		 * Get the object at some index.
		 * @param index the index
		 * @return the object at the argument index
		 */
		public abstract MsObject getObject(MsNumber index);
		
		/**
		 * Add an object to the end of the array.
		 * @param obj the object to add
		 */
		public abstract void addObject(MsObject obj);
		
		/**
		 * Add all elements of a given array to the end of the array.
		 * @param array the array whose elements to add
		 */
		public abstract void addObjects(MsArray array);
		
		/**
		 * Set the object at some index.
		 * @param index the index
		 * @param obj the value to set at the argument index
		 * @return the object previously located at the argument index
		 */
		public abstract MsObject setObject(MsNumber index, MsObject obj);
		
		/**
		 * Remove the object at some index.
		 * @param index the index
		 * @return the object formerly located at the argument index
		 */
		public abstract MsObject removeObjectAt(MsNumber index);
		
		/**
		 * Remove some object from the array.
		 * @param obj the object to remove
		 */
		public abstract void removeObject(MsObject value);
		
		/**
		 * Remove all the elements of a given array from the array.
		 * @param array the array whose elements to remove
		 */
		public abstract void removeObjects(MsArray array);
		
		/**
		 * Get the number of objects in the array.
		 * @return the number of objects
		 */
		public abstract MsNumber getObjectCount();
		
		/**
		 * Get the underlying List. This method is exclusively for Java based
		 * system function providers to work with.
		 * @return the underlying List
		 */
		public abstract List getNativeList();
	}
	
	/**
	 * Wrap a list into a MarkupScript array.
	 * @param l the list to wrap
	 * @return the wrapped list
	 */
	public static MsArray wrapList(List l) {
		return new DefaultArray(l);
	}
	
	private static class DefaultArray implements MsArray {
		private List value;
		DefaultArray(List value) {
			this.value = value;
		}
		public MsBoolean asBoolean() {
			return wrapBoolean(this.value.size() != 0);
		}
		public MsNumber asNumber() {
			return this.asString().asNumber();
		}
		public MsString asString() {
			return (this.value.isEmpty() ? wrapString("") : wrapObject(this.value.get(0)).asString());
		}
		public Object getNativeObject() {
			return this.getNativeList();
		}
		public MsObject getObject(MsNumber index) {
			return wrapObject(this.value.get(index.intValue()));
		}
		public void addObject(MsObject obj) {
			this.value.add(obj);
		}
		public void addObjects(MsArray array) {
			this.value.addAll(array.getNativeList());
		}
		public MsObject setObject(MsNumber index, MsObject obj) {
			return wrapObject(this.value.set(index.intValue(), obj));
		}
		public MsObject removeObjectAt(MsNumber index) {
			return wrapObject(this.value.remove(index.intValue()));
		}
		public void removeObject(MsObject value) {
			if (!this.value.remove(value))
				this.value.remove(value.getNativeObject());
		}
		public void removeObjects(MsArray array) {
			this.value.removeAll(array.getNativeList());
		}
		public MsNumber getObjectCount() {
			return wrapInteger(this.value.size());
		}
		public List getNativeList() {
			return this.value;
		}
	}
	
	/**
	 * A mapping from MsStrings to MsObjects, corresponding to a JSON object
	 * 
	 * @author sautter
	 */
	public static interface MsMap extends MsObject {
		
		/**
		 * Check if a given key maps to some value.
		 * @param key the key to check
		 * @return true if the argument key maps to a value, false otherwise
		 */
		public abstract MsBoolean hasValue(MsString key);
		
		/**
		 * Retrieve a value by its key.
		 * @param key the key
		 * @return the value
		 */
		public abstract MsObject getValue(MsString key);
		
		/**
		 * Map a key to a value.
		 * @param key the key
		 * @param value the value
		 * @return the value previously assigned to the argument key
		 */
		public abstract MsObject setValue(MsString key, MsObject value);
		
		/**
		 * Add all elements of a given map to the map.
		 * @param map the map whose key-value-pairs to add
		 * @param overwrite overwrite existing values?
		 */
		public abstract void setValues(MsMap map, MsBoolean overwrite);
		
		/**
		 * Remove a key.
		 * @param key the key
		 * @return the value formerly assigned to the argument key
		 */
		public abstract MsObject removeValue(MsString key);
		
		/**
		 * Remove all keys of a given map from the map.
		 * @param map the map whose keys to remove
		 */
		public abstract void removeValues(MsMap map);
		
		/**
		 * Remove all keys in a given array from the map.
		 * @param array the map holding the keys to remove
		 */
		public abstract void removeKeys(MsArray array);
//		
//		/**
//		 * Get the number of key/value pairs in the map.
//		 * @return the number of keys
//		 */
//		public abstract MsNumber getValueCount();
//		WHO ASKS THE SIZE (PROPERTY COUNT) OF A JSON OBJECT ???
		
		/**
		 * Get an array containing the keys in the map. The returned array will
		 * have strings as its only entries.
		 * @return an array containing the keys
		 */
		public abstract MsArray getValueKeys();
		
		/**
		 * Get the underlying attribute Map. This method is exclusively for Java
		 * based system function providers to work with.
		 * @return the underlying Map
		 */
		public abstract Attributed getNativeMap();
	}
	
	/**
	 * Wrap a map into a MarkupScript map.
	 * @param m the map to wrap
	 * @return the wrapped map
	 */
	//	whatever comes in here should be a JSON map anyway, i.e., string -> <any wrappable object>
	public static MsMap wrapMap(Map m) {
		Attributed a = new AbstractAttributed();
		for (Iterator kit = m.keySet().iterator(); kit.hasNext();) {
			String key = kit.next().toString();
			Object value = m.get(key);
			if (value != null)
				a.setAttribute(key, value);
		}
		return new DefaultMap(a);
	}
	
	/**
	 * Wrap an attributed object into a MarkupScript map.
	 * @param a the attributed object to wrap
	 * @return the wrapped attributed object
	 */
	public static MsMap wrapMap(Attributed a) {
		return new DefaultMap(a);
	}
	
	private static class DefaultMap implements MsMap {
		private Attributed value;
		DefaultMap(Attributed value) {
			this.value = value;
		}
		public MsBoolean asBoolean() {
			return wrapBoolean(this.value.getAttributeNames().length != 0); // very expensive, but should be rarely used
		}
		public MsNumber asNumber() {
			return wrapInteger(this.value.getAttributeNames().length); // very expensive, but should be rarely used
		}
		public MsString asString() {
			return wrapString(""); // makes no real sense, but this conversion is a bit contrived anyway
		}
		public Object getNativeObject() {
			return this.getNativeMap();
		}
		public MsBoolean hasValue(MsString key) {
			return wrapBoolean(this.value.hasAttribute(key.getValue()));
		}
		public MsObject getValue(MsString key) {
			return wrapObject(this.value.getAttribute(key.getValue()));
		}
		public MsObject setValue(MsString key, MsObject value) {
			return wrapObject(this.value.setAttribute(key.getValue(), value.getNativeObject()));
		}
		public void setValues(MsMap map, MsBoolean overwrite) {
			AttributeUtils.copyAttributes(map.getNativeMap(), this.value, (overwrite.getValue() ? AttributeUtils.SET_ATTRIBUTE_COPY_MODE : AttributeUtils.ADD_ATTRIBUTE_COPY_MODE));
		}
		public MsObject removeValue(MsString key) {
			return wrapObject(this.value.removeAttribute(key.getValue()));
		}
		public void removeValues(MsMap map) {
			String[] toRemove = map.getNativeMap().getAttributeNames();
			for (int r = 0; r < toRemove.length; r++)
				this.value.removeAttribute(toRemove[r]);
		}
		public void removeKeys(MsArray array) {
			List toRemove = array.getNativeList();
			for (int r = 0; r < toRemove.size(); r++)
				this.value.removeAttribute(toRemove.get(r).toString());
		}
		public MsArray getValueKeys() {
			return wrapList(Arrays.asList(this.value.getAttributeNames()));
		}
		public Attributed getNativeMap() {
			return this.value;
		}
	}
	
	/**
	 * A token in a sequence, basically a whitespace free string with attributes
	 * 
	 * @author sautter
	 */
	public static interface MsToken extends MsString, MsMap {
		
		/**
		 * Get the underlying token. This method is exclusively for Java based
		 * system function providers to work with.
		 * @return the underlying token
		 */
		public abstract Token getNativeToken();
	}
	
	/**
	 * Wrap a token into a MarkupScript token.
	 * @param ts the token to wrap
	 * @return the wrapped token
	 */
	public static MsToken wrapToken(Token t) {
		return new DefaultToken(t);
	}
	
	private static class DefaultToken implements MsToken {
		private Token value;
		DefaultToken(Token value) {
			this.value = value;
		}
		public MsBoolean asBoolean() {
			return this.asString().asBoolean();
		}
		public MsNumber asNumber() {
			return this.asString().asNumber();
		}
		public MsString asString() {
			return wrapString(this.value.getValue());
		}
		public Object getNativeObject() {
			return this.getNativeToken();
		}
		public String getValue() {
			return this.value.getValue();
		}
		public CharSequence getNativeString() {
			return this.value;
		}
		public MsBoolean hasValue(MsString key) {
			return wrapBoolean(this.value.hasAttribute(key.getValue()));
		}
		public MsObject getValue(MsString key) {
			return wrapObject(this.value.getAttribute(key.getValue()));
		}
		public MsObject setValue(MsString key, MsObject value) {
			return wrapObject(this.value.setAttribute(key.getValue(), value.getNativeObject()));
		}
		public void setValues(MsMap map, MsBoolean overwrite) {
			AttributeUtils.copyAttributes(map.getNativeMap(), this.value, (overwrite.getValue() ? AttributeUtils.SET_ATTRIBUTE_COPY_MODE : AttributeUtils.ADD_ATTRIBUTE_COPY_MODE));
		}
		public MsObject removeValue(MsString key) {
			return wrapObject(this.value.removeAttribute(key.getValue()));
		}
		public void removeValues(MsMap map) {
			String[] toRemove = map.getNativeMap().getAttributeNames();
			for (int r = 0; r < toRemove.length; r++)
				this.value.removeAttribute(toRemove[r]);
		}
		public void removeKeys(MsArray array) {
			List toRemove = array.getNativeList();
			for (int r = 0; r < toRemove.size(); r++)
				this.value.removeAttribute(toRemove.get(r).toString());
		}
		public MsArray getValueKeys() {
			return wrapList(Arrays.asList(this.value.getAttributeNames()));
		}
		public Attributed getNativeMap() {
			return this.value;
		}
		public Token getNativeToken() {
			return this.value;
		}
	}
	
	/**
	 * A sequence of tokens, basically a tokenized string whose tokens are
	 * accessible via indices. Implementations for get() are expected to return
	 * MsToken objects. The array is immutable, i.e., neither of the remove()
	 * and set() methods should do anything.
	 * 
	 * @author sautter
	 */
	public static interface MsTokenSequence extends MsString, MsArray {
		
		/**
		 * Get the underlying token sequence. This method is exclusively for
		 * Java based system function providers to work with.
		 * @return the underlying token sequence
		 */
		public abstract TokenSequence getNativeTokenSequence();
	}
	
	/**
	 * Wrap a token sequence into a MarkupScript token sequence.
	 * @param ts the token sequence to wrap
	 * @return the wrapped token sequence
	 */
	public static MsTokenSequence wrapTokenSequence(TokenSequence ts) {
		return new DefaultTokenSequence(ts);
	}
	
	private static class DefaultTokenSequence implements MsTokenSequence {
		private TokenSequence value;
		DefaultTokenSequence(TokenSequence value) {
			this.value = value;
		}
		public MsBoolean asBoolean() {
			return wrapBoolean(this.value.size() > 0);
		}
		public MsNumber asNumber() {
			return this.asString().asNumber();
		}
		public MsString asString() {
			return wrapString(TokenSequenceUtils.concatTokens(this.value, false, true));
		}
		public Object getNativeObject() {
			return this.getNativeTokenSequence();
		}
		public String getValue() {
			return TokenSequenceUtils.concatTokens(this.value, false, true);
		}
		public CharSequence getNativeString() {
			return this.value;
		}
		public MsObject getObject(MsNumber index) {
			return wrapString(this.value.tokenAt(index.intValue()));
		}
		public void addObject(MsObject obj) {
			throw new UnsupportedOperationException("Cannot add value to token sequence.");
		}
		public void addObjects(MsArray array) {
			throw new UnsupportedOperationException("Cannot add values to token sequence.");
		}
		public MsObject setObject(MsNumber index, MsObject obj) {
			throw new UnsupportedOperationException("Cannot set value in token sequence.");
		}
		public MsObject removeObjectAt(MsNumber index) {
			throw new UnsupportedOperationException("Cannot remove value from token sequence.");
		}
		public void removeObject(MsObject value) {
			throw new UnsupportedOperationException("Cannot remove value from token sequence.");
		}
		public void removeObjects(MsArray array) {
			throw new UnsupportedOperationException("Cannot remove values from token sequence.");
		}
		public MsNumber getObjectCount() {
			return wrapInteger(this.value.size());
		}
		public List getNativeList() {
			return new TokenSequenceList(this.value);
		}
		public TokenSequence getNativeTokenSequence() {
			return this.value;
		}
	}
	
	private static class TokenSequenceList extends AbstractList {
		private TokenSequence value;
		TokenSequenceList(TokenSequence value) {
			this.value = value;
		}
		public Object get(int index) {
			return this.value.tokenAt(index);
		}
		public int size() {
			return this.value.size();
		}
	}
	
	/**
	 * A token sequence that has a meaning (and respective attributes) as a
	 * whole.
	 * 
	 * @author sautter
	 */
	public static interface MsAnnotation extends MsTokenSequence, MsMap {
		
		/** the name of the attribute to retrieve an Annotations's start index, namely 'startIndex' */
		public static final String START_INDEX_ATTRIBUTE = "startIndex";
		
		/** the name of the attribute to retrieve an Annotations's end index, namely 'endIndex' */
		public static final String END_INDEX_ATTRIBUTE = "endIndex";
		
		/** the name of the attribute to retrieve an Annotations's size, namely 'size' */
		public static final String SIZE_ATTRIBUTE = "startIndex";
		
		/** the name of the attribute to retrieve an Annotations's type, namely 'type' */
		public static final String TYPE_ATTRIBUTE = "type";
		
		//	TODO switch to all-caps as in GPath?
		
		/**
		 * Get the index of the first token spanned by the annotation in the
		 * underlying token sequence it refers to.
		 * @return the index of the first annotated token
		 */
		public abstract MsNumber getStartIndex();
		
		/**
		 * Get the index of the first token not spanned any more by the
		 * annotation in the underlying token sequence it refers to. This
		 * is basically the exclusive index of the last token.
		 * @return the index of the first non-annotated token
		 */
		public abstract MsNumber getEndIndex();
		
		/**
		 * Get the type of the annotation, i.e., the string naming the meaning
		 * of the token sequence it marks and provides attributes for.
		 * @return the annotation type
		 */
		public abstract MsString getType();
		
		/**
		 * Get the underlying annotation. This method is exclusively for Java
		 * based system function providers to work with.
		 * @return the underlying annotation
		 */
		public abstract Annotation getNativeAnnotation();
	}
	
	/**
	 * Wrap an annotation into a MarkupScript annotation.
	 * @param a the annotation to wrap
	 * @return the wrapped annotation
	 */
	public static MsAnnotation wrapAnnotation(Annotation a) {
		return new DefaultAnnotation(a);
	}
	
	private static class DefaultAnnotation extends DefaultTokenSequence implements MsAnnotation {
		private Annotation value;
		DefaultAnnotation(Annotation value) {
			super(value);
			this.value = value;
		}
		public Object getNativeObject() {
			return this.getNativeAnnotation();
		}
		public MsBoolean hasValue(MsString key) {
			return wrapBoolean(this.value.hasAttribute(key.getValue()));
		}
		public MsObject getValue(MsString key) {
			//	TODO catch start and end index, size, and type
			return wrapObject(this.value.getAttribute(key.getValue()));
		}
		public MsObject setValue(MsString key, MsObject value) {
			//	TODO catch type
			return wrapObject(this.value.setAttribute(key.getValue(), value.getNativeObject()));
		}
		public void setValues(MsMap map, MsBoolean overwrite) {
			AttributeUtils.copyAttributes(map.getNativeMap(), this.value, (overwrite.getValue() ? AttributeUtils.SET_ATTRIBUTE_COPY_MODE : AttributeUtils.ADD_ATTRIBUTE_COPY_MODE));
		}
		public MsObject removeValue(MsString key) {
			return wrapObject(this.value.removeAttribute(key.getValue()));
		}
		public void removeValues(MsMap map) {
			String[] toRemove = map.getNativeMap().getAttributeNames();
			for (int r = 0; r < toRemove.length; r++)
				this.value.removeAttribute(toRemove[r]);
		}
		public void removeKeys(MsArray array) {
			List toRemove = array.getNativeList();
			for (int r = 0; r < toRemove.size(); r++)
				this.value.removeAttribute(toRemove.get(r).toString());
		}
		public MsArray getValueKeys() {
			return wrapList(Arrays.asList(this.value.getAttributeNames()));
		}
		public Attributed getNativeMap() {
			return this.value;
		}
		public MsNumber getStartIndex() {
			return wrapInteger(this.value.getStartIndex());
		}
		public MsNumber getEndIndex() {
			return wrapInteger(this.value.getEndIndex());
		}
		public MsString getType() {
			return wrapString(this.value.getType());
		}
		public Annotation getNativeAnnotation() {
			return this.value;
		}
	}
	
	/**
	 * An annotation that represents a whole document to process. Annotations
	 * can be added to a document, and removed from it. Also, existing
	 * annotations can be retrieved from a document as respective arrays.
	 * 
	 * @author sautter
	 */
	public static interface MsDocument extends MsAnnotation {
		
		/**
		 * Add an annotation to the document, starting at the token indicated
		 * by the argument start index (inclusively) and ending at the argument
		 * end index (exclusively).
		 * @param type the type of the annotation to add
		 * @param startIndex the index of the token the for annotation to start
		 *            at (inclusively)
		 * @param endIndex the index of the token the for annotation to end at
		 *            (exclusively)
		 * @return the added annotation, which can be annotated further
		 */
		public abstract MsDocument addAnnotation(MsString type, MsNumber startIndex, MsNumber endIndex);
		
		/**
		 * Remove a given annotation from the document. The returned annotation
		 * is a document independent representation of the annotation that was
		 * just removed.
		 * @param annot the annotation to remove
		 * @return the removed annotation
		 */
		public abstract MsAnnotation removeAnnotation(MsAnnotation annot);
		
		/**
		 * Get all the annotations that exist for the document. The objects in
		 * the returned array are documents themselves, so they can be processed
		 * further.
		 * @return an array holding the annotations
		 */
		public abstract MsArray getAnnotations();
		
		/**
		 * Get all the annotations of a given type that exist for the document.
		 * The objects in the returned array are documents themselves, so they
		 * can be processed further.
		 * @return an array holding the annotations
		 */
		public abstract MsArray getAnnotations(MsString type);
		
		/**
		 * Get the underlying document. This method is exclusively for Java
		 * based system function providers to work with.
		 * @return the underlying annotation
		 */
		public abstract MutableAnnotation getNativeDocument();
	}
	
	/**
	 * Wrap a mutable annotation into a MarkupScript document.
	 * @param ma the mutable annotation to wrap
	 * @return the wrapped mutable annotation
	 */
	public static MsDocument wrapDocument(MutableAnnotation ma) {
		return new DefaultDocument(ma);
	}
	
	private static class DefaultDocument extends DefaultAnnotation implements MsDocument {
		private MutableAnnotation value;
		DefaultDocument(MutableAnnotation value) {
			super(value);
			this.value = value;
		}
		public Object getNativeObject() {
			return this.getNativeDocument();
		}
		public MsArray getAnnotations() {
			return wrapList(Arrays.asList(this.value.getMutableAnnotations()));
		}
		public MsArray getAnnotations(MsString type) {
			return wrapList(Arrays.asList(this.value.getMutableAnnotations(type.getValue())));
		}
		public MsDocument addAnnotation(MsString type, MsNumber startIndex, MsNumber endIndex) {
			return wrapDocument(this.value.addAnnotation(type.getValue(), startIndex.intValue(), (endIndex.intValue() - startIndex.intValue())));
			//	TODO this or start index / size approach
		}
		public MsAnnotation removeAnnotation(MsAnnotation annot) {
			return wrapAnnotation(this.value.removeAnnotation(annot.getNativeAnnotation()));
		}
		public MutableAnnotation getNativeDocument() {
			return this.value;
		}
	}
	
	static String getObjectType(MsObject obj) {
		if (obj instanceof MsDocument)
			return DOCUMENT;
		else if (obj instanceof MsAnnotation)
			return ANNOTATION;
		else if (obj instanceof MsToken)
			return TOKEN;
		else if (obj instanceof MsMap)
			return MAP;
		
		else if (obj instanceof MsTokenSequence)
			return TOKEN_SEQUENCE;
		else if (obj instanceof MsArray)
			return ARRAY;
		
		else if (obj instanceof MsString)
			return STRING;
		else if (obj instanceof MsNumber)
			return NUMBER;
		else if (obj instanceof MsBoolean)
			return BOOLEAN;
		
		else return OBJECT;
	}
	
	static boolean equals(MsObject left, MsObject right) {
		if (left == null)
			return (right == null);
		else if (right == null)
			return false;
		String leftType = getObjectType(left);
		String rightType = getObjectType(right);
		//	TODO observe type inheritance
		if (leftType.equals(rightType)) {
			if ("boolean".equals(leftType))
				return (left.asBoolean().getValue() == right.asBoolean().getValue());
			else if ("number".equals(leftType))
				return (left.asNumber().getValue() == right.asNumber().getValue());
			else if ("array".equals(leftType)) {
				MsArray leftArray = ((MsArray) left);
				MsArray rightArray = ((MsArray) right);
				if (leftArray.getObjectCount().intValue() != rightArray.getObjectCount().intValue())
					return false;
				for (int i = 0; i < leftArray.getObjectCount().intValue(); i++) {
					if (!equals(leftArray.getObjectAt(i), ((MsObject) rightArray.values.get(i))))
						return false;
				}
				return true;
			}
			else if ("map".equals(leftType)) {
				MsMap leftMap = ((MsMap) left);
				MsMap rightMap = ((MsMap) right);
				if (leftMap.values.size() != rightMap.values.size())
					return false;
				if (leftMap.values.keySet().containsAll(rightMap.values.keySet()))
					return false;
				if (rightMap.values.keySet().containsAll(leftMap.values.keySet()))
					return false;
				for (Iterator kit = leftMap.values.keySet().iterator(); kit.hasNext();) {
					String key = ((String) kit.next());
					if (!equals(((MsObject) leftMap.values.get(key)), ((MsObject) rightMap.values.get(key))))
						return false;
				}
				return true;
			}
		}
		return left.asString().getValue().equals(right.asString().getValue());
	}
	
	static int compare(MsObject left, MsObject right) {
		if (left == null)
			return 1; // null is less than anything
		else if (right == null)
			return -1; // null is less than anything
		String leftType = getObjectType(left);
		String rightType = getObjectType(right);
		//	TODO observe type inheritance
		if (leftType.equals(rightType)) {
			if ("boolean".equals(leftType)) {
				if (left.asBoolean().getValue())
					return (right.asBoolean().getValue() ? 0 : 1);
				else return (right.asBoolean().getValue() ? -1 : 0);
			}
			else if ("number".equals(leftType)) {
				double diff = (left.asNumber().getValue() - right.asNumber().getValue());
				if (diff < 0)
					return -1;
				else if (diff == 0)
					return 0;
				else return 1;
			}
			else if ("array".equals(leftType))
				return (((MsArray) left).getNativeList().size() - ((MsArray) right).getNativeList().size());
			else if ("map".equals(leftType))
				return compare(((MsMap) left).getValueCount(), ((MsMap) right).getValueCount());
		}
		if ("map".equals(leftType) || "array".equals(leftType))
			throw new IllegalArgumentException("Cannot compare " + leftType + " to " + rightType);
		return left.asString().getValue().toString().compareTo(right.asString().getValue().toString());
	}
	
	private static String getComparisonType(String leftType, String rightType) {
		if (BOOLEAN.equals(leftType))
			return BOOLEAN;
		else if (NUMBER.equals(leftType)) {
			if (BOOLEAN.equals(rightType))
				return BOOLEAN;
			else return NUMBER;
		}
		else if (STRING.equals(leftType)) {
			
		}
		else if (ARRAY.equals(leftType)) {
			
		}
		else if (TOKEN_SEQUENCE.equals(leftType)) {
			
		}
		else if (MAP.equals(leftType)) {
			
		}
		else if (TOKEN.equals(leftType)) {
			
		}
		else if (ANNOTATION.equals(leftType)) {
			
		}
		else if (DOCUMENT.equals(leftType)) {
			
		}
		else /* if (OBJECT.equals(leftType)) */ {
			
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
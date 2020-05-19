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
package de.uka.ipd.idaho.plugins.abbreviations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Constant bearer for abbreviation handling analyzers
 * 
 * @author sautter
 */
public interface AbbreviationConstants extends LiteratureConstants {
	
	/**
	 * Annotation type for marking the actual abbreviations inside an
	 * abbreviationData annotation.
	 */
	public static final String ABBREVIATION_ANNOTATION_TYPE = "abbreviation";
	
	/**
	 * Annotation type for marking range definitions of actual abbreviations
	 * inside an abbreviationData annotation.
	 */
	public static final String ABBREVIATION_RANGE_ANNOTATION_TYPE = "abbreviationRange";
	
	/**
	 * Name of the attribute marking the level of a given abbreviation in an
	 * abbreviation hierarchy.
	 */
	public static final String ABBREVIATION_LEVEL_ATTTRIBUTE = "level";
	
	/**
	 * Name of the attribute storing the parent of a given abbreviation in an
	 * abbreviation hierarchy.
	 */
	public static final String PARENT_ABBREVIATION_ATTTRIBUTE = "parent";
	
	/**
	 * Name of the attribute storing the textual definition of an abbreviation,
	 * including the definition of any parent abbreviations.
	 */
	public static final String IMPLIED_TEXT_ATTTRIBUTE = "impliedText";
	
	/**
	 * Annotation type for marking the data an abbreviation stands for. If
	 * nested in another abbreviationData annotation, inherits the data from the
	 * latter.
	 */
	public static final String ABBREVIATION_DATA_ANNOTATION_TYPE = "abbreviationData";
	
	/**
	 * Annotation type for marking abbreviations outside an abbreviationData
	 * annotation, i.e., references to the data representd by abbreviations.
	 */
	public static final String ABBREVIATION_REFERENCE_ANNOTATION_TYPE = "abbreviationReference";
	
	/**
	 * Attribute holding the textual content of an annotation in the data an
	 * abbreviation stands for.
	 */
	public static final String ANNOTATED_VALUE_ATTRIBUTE = "annotationValue";
	
	/**
	 * Convenience class for accessing the data implied by an abbreviation. This
	 * class wraps access to the attributes set with an abbreviation during the
	 * augmentation process. This class is strictly read-only, none of the
	 * modification methods is implemented to have any effect. Furthermore, the
	 * offsets and indices of objects of this class do not reflect the positions
	 * of the original annotations the backing abbreviation implies.
	 * 
	 * @author sautter
	 */
	public static class ImpliedAnnotation implements Annotation {
		private Attributed abbreviationData;
		private String type;
		private int number;
		private String attributePrefix;
		private StringVector attributeNames = new StringVector();
		private TokenSequence tokens;
		private String id = Gamta.getAnnotationID();
		
		/**
		 * Constructor
		 * @param abbreviationData the representation of the backing
		 *            abbreviation holding the actual data
		 * @param type the type of the abbreviated annotation
		 * @param tokenizer the tokenizer of the backing annotation
		 */
		public ImpliedAnnotation(Attributed abbreviationData, String type, Tokenizer tokenizer) {
			this(abbreviationData, type, -1, tokenizer);
		}
		
		/**
		 * Constructor
		 * @param abbreviationData the representation of the backing
		 *            abbreviation holding the actual data
		 * @param type the type of the abbreviated annotation
		 * @param number the order number of the abbreviated annotation, in case
		 *            the data represented by the backing annotation contains
		 *            more than one annotation of the specified type
		 * @param tokenizer the tokenizer of the backing annotation
		 */
		public ImpliedAnnotation(Attributed abbreviationData, String type, int number, Tokenizer tokenizer) {
			this.abbreviationData = abbreviationData;
			this.type = type;
			this.number = number;
			this.attributePrefix = this.type + (((this.number < 0) ? "" : ("." + this.number)) + ".");
			String[] attributeNames = this.abbreviationData.getAttributeNames();
			for (int a = 0; a < attributeNames.length; a++) {
				if (attributeNames[a].startsWith(this.attributePrefix))
					this.attributeNames.addElementIgnoreDuplicates(attributeNames[a].substring(this.attributePrefix.length()));
			}
			String abbreviatedValue = ((String) this.abbreviationData.getAttribute(this.attributePrefix + ANNOTATED_VALUE_ATTRIBUTE));
			if (abbreviatedValue == null)
				abbreviatedValue = this.type;
			this.tokens = Gamta.newTokenSequence(abbreviatedValue, tokenizer);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Attributed#getAttributeNames()
		 */
		public String[] getAttributeNames() {
			return this.attributeNames.toStringArray();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#hasAttribute(java.lang.String)
		 */
		public boolean hasAttribute(String name) {
			return this.attributeNames.contains(name);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			return this.abbreviationData.getAttribute(this.attributePrefix + name);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#getAttribute(java.lang.String, java.lang.Object)
		 */
		public Object getAttribute(String name, Object def) {
			return this.abbreviationData.getAttribute((this.attributePrefix + name), def);
		}
		
		/**
		 * This implementation does not change anything, as this does not make
		 * sense in this context.
		 * @param name the name for the attribute
		 * @see de.uka.ipd.idaho.gamta.Annotation#setAttribute(java.lang.String)
		 */
		public void setAttribute(String name) {}
		
		/**
		 * This implementation does not change anything, as this does not make
		 * sense in this context.
		 * @param name the name for the attribute
		 * @param value the value of the attribute
		 * @return the value attempted to set
		 * @see de.uka.ipd.idaho.gamta.Annotation#setAttribute(java.lang.String, java.lang.Object)
		 */
		public Object setAttribute(String name, Object value) {
			return value;
		}
		
		/**
		 * This implementation does not change anything, as this does not make
		 * sense in this context.
		 * @param name the name of the attribute to be removed
		 * @return the value of the attribute, but without removing it
		 * @see de.uka.ipd.idaho.gamta.Attributed#removeAttribute(java.lang.String)
		 */
		public Object removeAttribute(String name) {
			return this.getAttribute(name);
		}
		
		/**
		 * This implementation does not change anything, as this does not make
		 * sense in this context.
		 * @param source the object to copy the attributes from
		 * @see de.uka.ipd.idaho.gamta.Attributed#copyAttributes(de.uka.ipd.idaho.gamta.Attributed)
		 */
		public void copyAttributes(Attributed source) {}
		
		/**
		 * This implementation does not change anything, as this does not make
		 * sense in this context.
		 * @see de.uka.ipd.idaho.gamta.Attributed#clearAttributes()
		 */
		public void clearAttributes() {}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.TokenSequence#tokenAt(int)
		 */
		public Token tokenAt(int index) {
			return this.tokens.tokenAt(index);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.TokenSequence#firstToken()
		 */
		public Token firstToken() {
			return this.tokens.firstToken();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.TokenSequence#lastToken()
		 */
		public Token lastToken() {
			return this.tokens.lastToken();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.TokenSequence#valueAt(int)
		 */
		public String valueAt(int index) {
			return this.tokens.valueAt(index);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.TokenSequence#firstValue()
		 */
		public String firstValue() {
			return this.tokens.firstValue();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.TokenSequence#lastValue()
		 */
		public String lastValue() {
			return this.tokens.lastValue();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.TokenSequence#getLeadingWhitespace()
		 */
		public String getLeadingWhitespace() {
			return this.tokens.getLeadingWhitespace();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.TokenSequence#getWhitespaceAfter(int)
		 */
		public String getWhitespaceAfter(int index) {
			return this.tokens.getWhitespaceAfter(index);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.TokenSequence#size()
		 */
		public int size() {
			return this.tokens.size();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.TokenSequence#getTokenizer()
		 */
		public Tokenizer getTokenizer() {
			return this.tokens.getTokenizer();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.TokenSequence#getSubsequence(int, int)
		 */
		public TokenSequence getSubsequence(int start, int size) {
			return this.tokens.getSubsequence(start, size);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.CharSequence#length()
		 */
		public int length() {
			return this.tokens.length();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.CharSequence#charAt(int)
		 */
		public char charAt(int index) {
			return this.tokens.charAt(index);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.CharSequence#subSequence(int, int)
		 */
		public CharSequence subSequence(int start, int end) {
			return this.tokens.subSequence(start, end);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.CharSpan#getStartOffset()
		 */
		public int getStartOffset() {
			return 0;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.CharSpan#getEndOffset()
		 */
		public int getEndOffset() {
			return this.length();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if (o instanceof ImpliedAnnotation) {
				ImpliedAnnotation ia = ((ImpliedAnnotation) o);
				return (this.type.equals(ia.type) ? (this.number - ia.number) : this.type.compareTo(ia.type));
			}
			else return -1;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#getStartIndex()
		 */
		public int getStartIndex() {
			return 0;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#getEndIndex()
		 */
		public int getEndIndex() {
			return this.size();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#getType()
		 */
		public String getType() {
			return this.type;
		}
		
		/**
		 * This implementation returns the specified type and does not change
		 * the object it belongs to, as a type change does not make sense in
		 * this context.
		 * @param newType the new type for the Annotation (specifying null or an
		 *            empty String will not change anything)
		 * @return the type of the Annotation
		 * @see de.uka.ipd.idaho.gamta.Annotation#changeTypeTo(java.lang.String)
		 */
		public String changeTypeTo(String newType) {
			return this.getType();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#getAnnotationID()
		 */
		public String getAnnotationID() {
			return this.id;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#getValue()
		 */
		public String getValue() {
			return TokenSequenceUtils.concatTokens(this.tokens, true, true);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#toXML()
		 */
		public String toXML() {
			return (AnnotationUtils.produceStartTag(this) + AnnotationUtils.escapeForXml(this.getValue()) + AnnotationUtils.produceEndTag(this));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#getDocument()
		 */
		public QueriableAnnotation getDocument() {
			return ((this.abbreviationData instanceof Annotation) ? ((Annotation) this.abbreviationData).getDocument() : null);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#getDocumentProperty(java.lang.String)
		 */
		public String getDocumentProperty(String propertyName) {
			return ((this.abbreviationData instanceof Annotation) ? ((Annotation) this.abbreviationData).getDocumentProperty(propertyName) : null);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#getDocumentProperty(java.lang.String, java.lang.String)
		 */
		public String getDocumentProperty(String propertyName, String defaultValue) {
			return ((this.abbreviationData instanceof Annotation) ? ((Annotation) this.abbreviationData).getDocumentProperty(propertyName, defaultValue) : defaultValue);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.Annotation#getDocumentPropertyNames()
		 */
		public String[] getDocumentPropertyNames() {
			return ((this.abbreviationData instanceof Annotation) ? ((Annotation) this.abbreviationData).getDocumentPropertyNames() : new String[0]);
		}
		
		/**
		 * Wrap an abbreviation so that the annotations it implies become
		 * accessible as used to. This wrapping also works hierarchically to
		 * cover cases of abbreviations subsuming other abbreviations.
		 * @param abbreviation the abbreviation to wrap
		 * @param type the type of the implied annotations to retrieve
		 * @return an array holding the implied annotations of the specified
		 *         type
		 */
		public static ImpliedAnnotation[] getImpliedAnnotations(Annotation abbreviation, String type) {
			
			//	check how many implied abbreviations of the specified type there are and collect respective numbers
			String[] masterAttributeNames = abbreviation.getAttributeNames();
			String typePrefix = (type + ".");
			TreeSet numbers = new TreeSet();
			for (int m = 0; m < masterAttributeNames.length; m++) {
				
				//	this one is none of the sought
				if (!masterAttributeNames[m].startsWith(typePrefix))
					continue;
				
				//	extract attribute name
				String attributeName = masterAttributeNames[m].substring(typePrefix.length());
				
				//	this one has a number, so we're dealing with multiple implied annotations of the specified type
				if (attributeName.matches("[1-9][0-9]*\\..*"))
					numbers.add(new Integer(attributeName.substring(0, attributeName.indexOf('.'))));
				
				//	this one is not numbered, so there is only one implied annotation of the specified type
				else {
					numbers.add(new Integer(-1));
					break;
				}
			}
			
			//	no implied annotation of the specified type
			if (numbers.isEmpty())
				return new ImpliedAnnotation[0];
			
			//	generate and return implied annotations
			ArrayList iaList = new ArrayList();
			for (Iterator nit = numbers.iterator(); nit.hasNext();)
				iaList.add(new ImpliedAnnotation(abbreviation, type, ((Integer) nit.next()).intValue(), abbreviation.getTokenizer()));
			return ((ImpliedAnnotation[]) iaList.toArray(new ImpliedAnnotation[iaList.size()]));
		}
	}
}
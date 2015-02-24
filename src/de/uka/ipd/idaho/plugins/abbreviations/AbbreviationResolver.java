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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.plugins.abbreviations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationListener;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.GenericAnnotationWrapper;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringIterator;

/**
 * An abbreviation resolver is a centralized index for the abbreviations defined
 * and used in a document. In particular, it allows for accessing the annotation
 * defining an abbreviation and holding all the data it implies by means of the
 * abbreviation string. Using this class makes actual sense mostly after
 * abbreviations have been augmented, i.e., after the data they imply has been
 * added to the defining annotations as attributes, e.g. by the
 * AbbreviationAugmenter Analyzer.
 * 
 * @author sautter
 */
public class AbbreviationResolver implements AbbreviationConstants {
	
	private static final String abbreviationResolverIdName = "abbreviationResolverId";
	
	private static HashMap resolvers = new HashMap();
	
	/**
	 * Create an abbreviation resolver for a given document or part of a
	 * document. First, this method checks if there is already an abbreviation
	 * resolver for the specified document or the document the specified part
	 * belongs to, using the abbreviationResolverId document property and the ID
	 * of the argument document, in this order. If there is none, a new
	 * abbreviation resolver is created and indexed for the argument document's
	 * or document part's annotation ID. If the argument is a document root,
	 * this method sets the abbreviationResolverId document property.
	 * @param doc the document to create an abbreviation resolver for
	 * @return an abbreviation resolver for the argument document
	 */
	public static AbbreviationResolver getResolver(MutableAnnotation doc) {
		return getResolver(doc, false);
	}
	
	/**
	 * Create an abbreviation resolver for a given document or part of a
	 * document. If the forceCreate argument is false, this method first checks
	 * if there is already an abbreviation resolver for the specified document
	 * or the document the specified part belongs to, using the
	 * abbreviationResolverId document property and the ID of the argument
	 * document, in this order. If there is none, or if the forceCreate argument
	 * is true, a new abbreviation resolver is created and indexed for the
	 * argument document's or document part's annotation ID. If the argument is
	 * a document root, this method sets the abbreviationResolverId document
	 * property. Setting the forceCreate argument to true is helpful mostly
	 * after the abbreviations in a document have been modified, i.e., after
	 * abbreviations have been removed or newly annotated.
	 * @param doc the document to create an abbreviation resolver for
	 * @param forceCreate force a new abbreviation resolver to be created?
	 * @return an abbreviation resolver for the argument document
	 */
	public static AbbreviationResolver getResolver(MutableAnnotation doc, boolean forceCreate) {
		AbbreviationResolver ar = null;
		if (!forceCreate) {
			ar = ((AbbreviationResolver) resolvers.get(doc.getDocumentProperty(abbreviationResolverIdName)));
			if (ar == null)
				ar = ((AbbreviationResolver) resolvers.get(doc.getAnnotationID()));
		}
		if (ar == null) {
			ar = new AbbreviationResolver(doc);
			resolvers.put(doc.getAnnotationID(), ar);
			if (doc instanceof DocumentRoot)
				((DocumentRoot) doc).setDocumentProperty(abbreviationResolverIdName, doc.getAnnotationID());
		}
		return ar;
	}
	
	private static class AbbreviationData {
		String string;
		TokenSequence stringTokens;
		Annotation annot = null;
		ArrayList sourceAnnotations = new ArrayList(1);
		HashSet sourceAnnotationKeys = new HashSet(2);
		AbbreviationData(String string, Tokenizer tok) {
			this.string = string;
			this.stringTokens = tok.tokenize(this.string);
		}
		void addSourceAnnotation(Annotation source) {
			if (!this.sourceAnnotationKeys.add(source.getAnnotationID()))
				return;
			this.sourceAnnotations.add(source);
			if (this.annot != null)
				this.annot.copyAttributes(source);
		}
		void removeSourceAnnotation(Annotation source) {
			if (!this.sourceAnnotationKeys.remove(source.getAnnotationID()))
				return;
			for (int s = 0; s < this.sourceAnnotations.size(); s++) {
				if (((Annotation) this.sourceAnnotations.get(s)).getAnnotationID().equals(source.getAnnotationID())) {
					this.sourceAnnotations.remove(s);
					break;
				}
			}
			this.refreshAttributes();
		}
		Annotation asAnnotation() {
			if (this.annot == null) {
				this.annot = Gamta.newAnnotation(this.stringTokens, ABBREVIATION_ANNOTATION_TYPE, 0, this.stringTokens.size());
				this.refreshAttributes();
			}
			return this.annot;
		}
		void refreshAttributes() {
			if (this.annot == null)
				return;
			this.annot.clearAttributes();
			for (int s = 0; s < this.sourceAnnotations.size(); s++)
				this.annot.copyAttributes((Annotation) this.sourceAnnotations.get(s));
		}
	}
	
	private static class AbbreviationRange {
		String prefix;
		String numberingScheme;
		int minValue;
		int maxValue;
		StringIndex valueCounts = new StringIndex();
		AbbreviationRange(String prefix, String numberingScheme, int minValue, int maxValue) {
			this.prefix = prefix;
			this.numberingScheme = numberingScheme;
			this.minValue = minValue;
			this.maxValue = maxValue;
		}
		void addNumber(int n) {
			this.valueCounts.add("" + n);
			this.minValue = Math.min(this.minValue, n);
			this.maxValue = Math.max(this.maxValue, n);
		}
		boolean removeNumber(int n) {
			if (!this.valueCounts.remove("" + n))
				return false;
			int min = -1;
			int max = -1;
			for (int v = this.minValue; v < this.maxValue; v++)
				if (this.valueCounts.contains("" + v)) {
					if (min == -1)
						min = v;
					max = v;
				}
			this.minValue = min;
			this.maxValue = max;
			return (this.minValue != -1);
		}
		String[] formatNumber(int n) {
			if ("Arabic".equals(this.numberingScheme)) {
				String[] nStrings = {("" + n)};
				return nStrings;
			}
			else if (this.numberingScheme.startsWith("Roman")) {
				String nString = Gamta.asRomanNumber(n, false);
				if (this.numberingScheme.endsWith("LC"))
					nString = nString.toLowerCase();
				else if (this.numberingScheme.endsWith("UC"))
					nString = nString.toUpperCase();
				String nStringQo = Gamta.asRomanNumber(n, true);
				if (this.numberingScheme.endsWith("LC"))
					nStringQo = nStringQo.toLowerCase();
				else if (this.numberingScheme.endsWith("UC"))
					nStringQo = nStringQo.toUpperCase();
				if (nStringQo.equals(nString)) {
					String[] nStrings = {nString};
					return nStrings;
				}
				else {
					String[] nStrings = {nString, nStringQo};
					return nStrings;
				}
			}
			else if (this.numberingScheme.startsWith("Letter")) {
				String nString = ("" + ((char) (n + 'A' - 1)));
				if (this.numberingScheme.endsWith("LC"))
					nString = nString.toLowerCase();
				else if (this.numberingScheme.endsWith("UC"))
					nString = nString.toUpperCase();
				String[] nStrings = {nString};
				return nStrings;
			}
			else return new String[0];
		}
	}
	
	private TreeMap abbreviationData = new TreeMap();
	private HashMap abbreviationDataLc = new HashMap();
	
	private HashMap abbreviationRanges = new HashMap();
	private HashMap abbreviationRangesLc = new HashMap();
	
	private AbbreviationResolver() {}
	
	private AbbreviationResolver(MutableAnnotation doc) {
		
		//	index individual abbreviations
		Annotation[] abbreviations = doc.getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
		for (int a = 0; a < abbreviations.length; a++) {
			String aString = TokenSequenceUtils.concatTokens(abbreviations[a], true, true);
			this.addAbbreviation(aString, abbreviations[a]);
		}
		
		//	index abbreviation ranges
		Annotation[] abbreviationRanges = doc.getAnnotations(ABBREVIATION_RANGE_ANNOTATION_TYPE);
		for (int a = 0; a < abbreviationRanges.length; a++)
			this.addAbbreviationRange(abbreviationRanges[a]);
		
		//	listen for changes
		doc.addAnnotationListener(new AnnotationListener() {
			public void annotationAttributeChanged(QueriableAnnotation doc, Annotation annotation, String attributeName, Object oldValue) {
				if (ABBREVIATION_ANNOTATION_TYPE.equals(annotation.getType()))
					this.abbreviationUpdated(annotation);
				else if (ABBREVIATION_RANGE_ANNOTATION_TYPE.equals(annotation.getType()))
					this.abbreviationRangeUpdated(annotation);
			}
			public void annotationTypeChanged(QueriableAnnotation doc, Annotation annotation, String oldType) {
				if (ABBREVIATION_ANNOTATION_TYPE.equals(annotation.getType()))
					this.abbreviationAdded(annotation);
				else if (ABBREVIATION_ANNOTATION_TYPE.equals(oldType))
					this.abbreviationRemoved(annotation);
				if (ABBREVIATION_RANGE_ANNOTATION_TYPE.equals(annotation.getType()))
					this.abbreviationRangeAdded(annotation);
				else if (ABBREVIATION_RANGE_ANNOTATION_TYPE.equals(oldType))
					this.abbreviationRangeRemoved(annotation);
			}
			public void annotationRemoved(QueriableAnnotation doc, Annotation annotation) {
				if (ABBREVIATION_ANNOTATION_TYPE.equals(annotation.getType()))
					this.abbreviationRemoved(annotation);
				else if (ABBREVIATION_RANGE_ANNOTATION_TYPE.equals(annotation.getType()))
					this.abbreviationRangeRemoved(annotation);
			}
			public void annotationAdded(QueriableAnnotation doc, Annotation annotation) {
				if (ABBREVIATION_ANNOTATION_TYPE.equals(annotation.getType()))
					this.abbreviationAdded(annotation);
				else if (ABBREVIATION_RANGE_ANNOTATION_TYPE.equals(annotation.getType()))
					this.abbreviationRangeAdded(annotation);
			}
			private void abbreviationAdded(Annotation abbreviation) {
				String aString = TokenSequenceUtils.concatTokens(abbreviation, true, true);
				AbbreviationResolver.this.addAbbreviation(aString, abbreviation);
			}
			private void abbreviationUpdated(Annotation abbreviation) {
				String aString = TokenSequenceUtils.concatTokens(abbreviation, true, true);
				AbbreviationResolver.this.updateAbbreviation(aString, abbreviation);
			}
			private void abbreviationRemoved(Annotation abbreviation) {
				String aString = TokenSequenceUtils.concatTokens(abbreviation, true, true);
				AbbreviationResolver.this.removeAbbreviation(aString, abbreviation);
			}
			private void abbreviationRangeAdded(Annotation abbreviationRange) {
				AbbreviationResolver.this.addAbbreviationRange(abbreviationRange);
			}
			private void abbreviationRangeUpdated(Annotation abbreviationRange) {
				AbbreviationResolver.this.updateAbbreviationRange(abbreviationRange);
			}
			private void abbreviationRangeRemoved(Annotation abbreviationRange) {
				AbbreviationResolver.this.removeAbbreviationRange(abbreviationRange);
			}
		});
	}
	
	private void addAbbreviationRange(Annotation abbreviationRange) {
		
		//	get basic data
		String prefix = ((String) abbreviationRange.getAttribute("prefix"));
		if (prefix == null)
			return;
		String numberingScheme = ((String) abbreviationRange.getAttribute("type"));
		if (numberingScheme == null)
			return;
		int value;
		try {
			value = Integer.parseInt((String) abbreviationRange.getAttribute("value"));
		}
		catch (Exception e) {
			return;
		}
		int minValue = value;
		try {
			minValue = Integer.parseInt((String) abbreviationRange.getAttribute("valueMin"));
		} catch (Exception e) {}
		int maxValue = value;
		try {
			maxValue = Integer.parseInt((String) abbreviationRange.getAttribute("valueMax"));
		} catch (Exception e) {}
		
		AbbreviationRange ar = ((AbbreviationRange) this.abbreviationRanges.get(prefix));
		if (ar == null) {
			ar = new AbbreviationRange(prefix, numberingScheme, minValue, maxValue);
			this.abbreviationRanges.put(prefix, ar);
			this.abbreviationRangesLc.put(prefix.toLowerCase(), ar);
		}
		else {
			ar.minValue = Math.min(minValue, ar.minValue);
			ar.maxValue = Math.max(maxValue, ar.maxValue);
		}
		
		//	parse actual values
		String valuesString = ((String) abbreviationRange.getAttribute("values", ("" + value)));
		String[] valueRanges = valuesString.split("\\s*\\,\\s*");
		if (valueRanges.length == 0)
			return;
		
		//	work off individual value ranges
		HashSet handledValues = new HashSet();
		for (int r = 0; r < valueRanges.length; r++) {
			String[] bounds = valueRanges[r].split("\\s*\\-\\s*");
			if (bounds.length == 0)
				continue;
			
			//	get range bounds
			int min = Integer.parseInt(bounds[0]);
			int max = ((bounds.length == 1) ? min : Integer.parseInt(bounds[1]));
			
			//	add all abbreviations in range
			for (int v = min; v <= max; v++) {
				if (!handledValues.add(new Integer(v)))
					continue;
				ar.addNumber(v);
				String[] nStrings = ar.formatNumber(v);
				for (int n = 0; n < nStrings.length; n++)
					this.addAbbreviation((prefix + " " + nStrings[n]), abbreviationRange);
			}
		}
		
		//	TODO consider synonymizing prefixes if one is a dotted abbreviation or a plural form of the other
	}
	
	private void updateAbbreviationRange(Annotation abbreviationRange) {
		
		//	get basic data
		String prefix = ((String) abbreviationRange.getAttribute("prefix"));
		if (prefix == null)
			return;
		AbbreviationRange ar = ((AbbreviationRange) this.abbreviationRanges.get(prefix));
		if (ar == null)
			return;
		int value;
		try {
			value = Integer.parseInt((String) abbreviationRange.getAttribute("value"));
		}
		catch (Exception e) {
			return;
		}
		
		//	parse actual values
		String valuesString = ((String) abbreviationRange.getAttribute("values", ("" + value)));
		String[] valueRanges = valuesString.split("\\s*\\,\\s*");
		if (valueRanges.length == 0)
			return;
		
		//	work off individual value ranges
		HashSet handledValues = new HashSet();
		for (int r = 0; r < valueRanges.length; r++) {
			String[] bounds = valueRanges[r].split("\\s*\\-\\s*");
			if (bounds.length == 0)
				continue;
			
			//	get range bounds
			int min = Integer.parseInt(bounds[0]);
			int max = ((bounds.length == 1) ? min : Integer.parseInt(bounds[1]));
			
			//	add all abbreviations in range
			for (int v = min; v <= max; v++) {
				if (!handledValues.add(new Integer(v)))
					continue;
				String[] nStrings = ar.formatNumber(v);
				for (int n = 0; n < nStrings.length; n++)
					this.addAbbreviation((prefix + " " + nStrings[n]), abbreviationRange);
			}
		}
	}
	
	private void removeAbbreviationRange(Annotation abbreviationRange) {
		
		//	get basic data
		String prefix = ((String) abbreviationRange.getAttribute("prefix"));
		if (prefix == null)
			return;
		AbbreviationRange ar = ((AbbreviationRange) this.abbreviationRanges.get(prefix));
		if (ar == null)
			return;
		int value;
		try {
			value = Integer.parseInt((String) abbreviationRange.getAttribute("value"));
		}
		catch (Exception e) {
			return;
		}
		
		//	parse actual values
		String valuesString = ((String) abbreviationRange.getAttribute("values", ("" + value)));
		String[] valueRanges = valuesString.split("\\s*\\,\\s*");
		if (valueRanges.length == 0)
			return;
		
		//	work off individual value ranges
		HashSet handledValues = new HashSet();
		boolean removeAbbreviationRange = false;
		for (int r = 0; r < valueRanges.length; r++) {
			String[] bounds = valueRanges[r].split("\\s*\\-\\s*");
			if (bounds.length == 0)
				continue;
			
			//	get range bounds
			int min = Integer.parseInt(bounds[0]);
			int max = ((bounds.length == 1) ? min : Integer.parseInt(bounds[1]));
			
			//	add all abbreviations in range
			for (int v = min; v <= max; v++) {
				if (!handledValues.add(new Integer(v)))
					continue;
				if (ar.removeNumber(v))
					removeAbbreviationRange = true;
				String[] nStrings = ar.formatNumber(v);
				for (int n = 0; n < nStrings.length; n++)
					this.removeAbbreviation((prefix + " " + nStrings[n]), abbreviationRange);
			}
		}
		
		//	remove range completely if required
		if (removeAbbreviationRange) {
			this.abbreviationRanges.remove(prefix);
			this.abbreviationRangesLc.remove(prefix.toLowerCase());
		}
	}
	
	private void addAbbreviation(String aString, Annotation abbreviation) {
		AbbreviationData aData = ((AbbreviationData) this.abbreviationData.get(aString));
		if (aData == null) {
			aData = new AbbreviationData(aString, abbreviation.getTokenizer());
			this.abbreviationData.put(aString, aData);
		}
		aData.addSourceAnnotation(abbreviation);
		if (!aString.equals(aString.toLowerCase())) {
			String aStringLc = aString.toLowerCase();
			AbbreviationData aDataLc = ((AbbreviationData) this.abbreviationDataLc.get(aStringLc));
			if (aDataLc == null) {
				aDataLc = new AbbreviationData(aStringLc, abbreviation.getTokenizer());
				this.abbreviationDataLc.put(aStringLc, aDataLc);
			}
			aDataLc.addSourceAnnotation(abbreviation);
		}
	}
	
	private void updateAbbreviation(String aString, Annotation abbreviation) {
		AbbreviationData aData = ((AbbreviationData) this.abbreviationData.get(aString));
		if (aData != null)
			aData.refreshAttributes();
		if (!aString.equals(aString.toLowerCase())) {
			String aStringLc = aString.toLowerCase();
			AbbreviationData aDataLc = ((AbbreviationData) this.abbreviationDataLc.get(aStringLc));
			if (aDataLc != null)
				aDataLc.refreshAttributes();
		}
	}
	
	private void removeAbbreviation(String aString, Annotation abbreviation) {
		AbbreviationData aData = ((AbbreviationData) this.abbreviationData.get(aString));
		if (aData != null) {
			aData.removeSourceAnnotation(abbreviation);
			if (aData.sourceAnnotations.isEmpty())
				this.abbreviationData.remove(aString);
		}
		if (!aString.equals(aString.toLowerCase())) {
			String aStringLc = aString.toLowerCase();
			AbbreviationData aDataLc = ((AbbreviationData) this.abbreviationDataLc.get(aStringLc));
			if (aDataLc != null) {
				aDataLc.removeSourceAnnotation(abbreviation);
				if (aDataLc.sourceAnnotations.isEmpty())
					this.abbreviationDataLc.remove(aStringLc);
			}
		}
	}
	
	/**
	 * Resolve an abbreviation to its definition. The annotations returned by
	 * this method are immutable, any attempt to modify them, including
	 * attribute modifications, results in an exception being thrown.
	 * @param abbreviation the abbreviation to resolve
	 * @return the annotation defining the argument string as an abbreviation
	 */
	public Annotation resolveAbbreviation(String abbreviation) {
		AbbreviationData ad = ((AbbreviationData) this.abbreviationData.get(abbreviation));
		if (ad == null)
			ad = ((AbbreviationData) this.abbreviationDataLc.get(abbreviation.toLowerCase()));
		return ((ad == null) ? null : new ResolvedAbbreviation(ad.asAnnotation()));
	}
	
	private static class ResolvedAbbreviation extends GenericAnnotationWrapper {
		ResolvedAbbreviation(Annotation data) {
			super(data);
		}
		public String changeTypeTo(String newType) {
			throw new RuntimeException("Illegal modification of annotation type.");
		}
		public Object setAttribute(String name, Object value) {
			throw new RuntimeException("Illegal modification of annotation attributes.");
		}
		public void copyAttributes(Attributed source) {
			throw new RuntimeException("Illegal modification of annotation attributes.");
		}
		public Object removeAttribute(String name) {
			throw new RuntimeException("Illegal modification of annotation attributes.");
		}
		public void clearAttributes() {
			throw new RuntimeException("Illegal modification of annotation attributes.");
		}
	}
	
	/**
	 * Obtain a dictionary containing full abbreviations, including ones implied
	 * by abbreviation ranges.
	 * @return an abbreviation dictionary
	 */
	public Dictionary getAbbreviationDictionary() {
		return new Dictionary() {
			public StringIterator getEntryIterator() {
				final Iterator eit = abbreviationData.keySet().iterator();
				return new StringIterator() {
					public boolean hasNext() {
						return eit.hasNext();
					}
					public boolean hasMoreStrings() {
						return eit.hasNext();
					}
					public Object next() {
						return eit.next();
					}
					public String nextString() {
						return ((String) eit.next());
					}
					public void remove() {}
				};
			}
			public boolean isDefaultCaseSensitive() {
				return true;
			}
			public boolean lookup(String string, boolean caseSensitive) {
				if (abbreviationData.containsKey(string))
					return true;
				else if (caseSensitive)
					return false;
				else return abbreviationDataLc.containsKey(string.toLowerCase());
			}
			public boolean lookup(String string) {
				return this.lookup(string, this.isDefaultCaseSensitive());
			}
			public boolean isEmpty() {
				return abbreviationData.isEmpty();
			}
			public int size() {
				return abbreviationData.size();
			}
		};
	}
	
	/**
	 * Obtain a dictionary containing prefixes of abbreviation ranges.
	 * @return an abbreviation range prefix dictionary
	 */
	public Dictionary getAbbreviationRangePrefixDictionary() {
		return new Dictionary() {
			public StringIterator getEntryIterator() {
				final Iterator eit = abbreviationRanges.keySet().iterator();
				return new StringIterator() {
					public boolean hasNext() {
						return eit.hasNext();
					}
					public boolean hasMoreStrings() {
						return eit.hasNext();
					}
					public Object next() {
						return eit.next();
					}
					public String nextString() {
						return ((String) eit.next());
					}
					public void remove() {}
				};
			}
			public boolean isDefaultCaseSensitive() {
				return true;
			}
			public boolean lookup(String string, boolean caseSensitive) {
				if (abbreviationRanges.containsKey(string))
					return true;
				else if (caseSensitive)
					return false;
				else return abbreviationRangesLc.containsKey(string.toLowerCase());
			}
			public boolean lookup(String string) {
				return this.lookup(string, this.isDefaultCaseSensitive());
			}
			public boolean isEmpty() {
				return abbreviationRanges.isEmpty();
			}
			public int size() {
				return abbreviationRanges.size();
			}
		};
	}
	
	/**
	 * Retrieve the numbering scheme used with a given abbreviation range
	 * prefix, namely 'Arabic', 'Roman', 'RomanUC', 'RomanLC', 'Letter',
	 * 'LetterUC', or 'LetterLC'. If the argument string is not an abbreviation
	 * range prefix, this method returns null.
	 * @param abbreviationRangePrefix the prefix
	 * @return the numbering scheme for the argument prefix
	 */
	public String getNumberingScheme(String abbreviationRangePrefix) {
		AbbreviationRange ar = ((AbbreviationRange) this.abbreviationRanges.get(abbreviationRangePrefix));
		return ((ar == null) ? null : ar.numberingScheme);
	}
	
	/**
	 * Retrieve the lowest number used with a given abbreviation range prefix,
	 * independent of the numbering scheme in use. If the argument string is not
	 * an abbreviation range prefix, this method returns -1.
	 * @param abbreviationRangePrefix the prefix
	 * @return the minimum number used with the argument prefix
	 */
	public int getMinValue(String abbreviationRangePrefix) {
		AbbreviationRange ar = ((AbbreviationRange) this.abbreviationRanges.get(abbreviationRangePrefix));
		return ((ar == null) ? -1 : ar.minValue);
	}
	
	/**
	 * Retrieve the highest number used with a given abbreviation range prefix,
	 * independent of the numbering scheme in use. If the argument string is not
	 * an abbreviation range prefix, this method returns -1.
	 * @param abbreviationRangePrefix the prefix
	 * @return the maximum number used with the argument prefix
	 */
	public int getMaxValue(String abbreviationRangePrefix) {
		AbbreviationRange ar = ((AbbreviationRange) this.abbreviationRanges.get(abbreviationRangePrefix));
		return ((ar == null) ? -1 : ar.maxValue);
	}
	
	/**
	 * Check if a given string matches the numbering scheme associated with a
	 * given abbreviation range prefix.
	 * @param token the string to test
	 * @param abbreviationRangePrefix the prefix
	 * @param checkRange also check min and max values?
	 * @return true if the string matched the numbering scheme, false otherwise
	 */
	public boolean isValidValue(String token, String abbreviationRangePrefix, boolean checkRange) {
		return (this.getIntValue(token, abbreviationRangePrefix, checkRange) != -1);
	}
	
	/**
	 * Obtain the int value of a given string in the numbering scheme associated
	 * with a given abbreviation range prefix. If the value cannot be determined
	 * because the argument prefix is invalid, the argument string does not fit
	 * in the numbering scheme, or the int value is outside the boundaries for
	 * the argument prefix, this method returns -1.
	 * @param token the string to parse
	 * @param abbreviationRangePrefix the prefix
	 * @param checkRange also check min and max values?
	 * @return the int value of the string
	 */
	public int getIntValue(String token, String abbreviationRangePrefix, boolean checkRange) {
		if (token == null)
			return -1;
		AbbreviationRange ar = ((AbbreviationRange) this.abbreviationRanges.get(abbreviationRangePrefix));
		if (ar == null)
			return -1;
		int value = -1;
		if ("Arabic".equals(ar.numberingScheme)) {
			if (!token.matches("[1-9][0-9]*"))
				return -1;
			value = Integer.parseInt(token);
		}
		else if (ar.numberingScheme.startsWith("Roman")) {
			if (!Gamta.isRomanNumber(token))
				return -1;
			if (ar.numberingScheme.endsWith("UC") && !token.equals(token.toUpperCase()))
				return -1;
			else if (ar.numberingScheme.endsWith("LC") && !token.equals(token.toLowerCase()))
				return -1;
			value = Gamta.parseRomanNumber(token);
		}
		else if (ar.numberingScheme.startsWith("Letter")) {
			if (!token.matches("[A-Za-z]"))
				return -1;
			if (ar.numberingScheme.endsWith("UC") && !token.equals(token.toUpperCase()))
				return -1;
			else if (ar.numberingScheme.endsWith("LC") && !token.equals(token.toLowerCase()))
				return -1;
			value = (token.toUpperCase().charAt(0) - 'A' + 1);
		}
		return ((!checkRange || ((ar.minValue <= value) && (value <= ar.maxValue))) ? value : -1);
	}
	
	/**
	 * Obtain the string values of a given int in the numbering scheme
	 * associated with a given abbreviation range prefix (can be two different
	 * ones for Roman numbers). If the value cannot be determined because the
	 * argument prefix is invalid or the int value is outside the boundaries for
	 * the argument prefix, this method returns null.
	 * @param the int to format
	 * @param abbreviationRangePrefix the prefix
	 * @param checkRange also check min and max values?
	 * @return an array holding the string values of the int
	 */
	public String[] getStringValues(int v, String abbreviationRangePrefix, boolean checkRange) {
		AbbreviationRange ar = ((AbbreviationRange) this.abbreviationRanges.get(abbreviationRangePrefix));
		return (((ar != null) && (!checkRange || ((ar.minValue <= v) && (v <= ar.maxValue)))) ? ar.formatNumber(v) : null);
	}
}
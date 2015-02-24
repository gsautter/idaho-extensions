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
package de.uka.ipd.idaho.plugins.abbreviations.analyzers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.plugins.abbreviations.AbbreviationConstants;
import de.uka.ipd.idaho.plugins.abbreviations.AbbreviationResolver;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 */
public class AbbreviationReferenceResolver extends AbstractConfigurableAnalyzer implements AbbreviationConstants {
	
	private StringVector rangeInfixes = new StringVector();
	private StringVector enumerationInfixes = new StringVector();
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			this.rangeInfixes = this.loadList(ABBREVIATION_RANGE_ANNOTATION_TYPE + "Infixes.list.txt");
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
		try {
			this.enumerationInfixes = this.loadList(ABBREVIATION_RANGE_ANNOTATION_TYPE + "EnumerationInfixes.list.txt");
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	work paragraph wise
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	get abbreviation resolver
		final AbbreviationResolver abbreviationResolver = AbbreviationResolver.getResolver(data);
		final Properties fuzzyAbbreviationMappings = new Properties();
		
		//	determine abbreviation layout and prepare fuzzy matching
		int maxAbbreviationSize = 0;
		boolean maxAbbbreviationSizeDotted = true;
		final Dictionary abbreviationDictionary = abbreviationResolver.getAbbreviationDictionary();
		for (StringIterator ait = abbreviationDictionary.getEntryIterator(); ait.hasMoreStrings();) {
			String abbreviationString = ait.nextString();
			System.out.println("Indexing abbreviation '" + abbreviationString + "'");
			Annotation abbreviation = abbreviationResolver.resolveAbbreviation(abbreviationString);
			if (abbreviation.size() >= maxAbbreviationSize) {
				maxAbbreviationSize = abbreviation.size();
				maxAbbbreviationSizeDotted = (maxAbbbreviationSizeDotted && (abbreviationString.indexOf(".") != -1));
			}
			if (abbreviationString.indexOf('.') == -1)
				continue;
			String fAbbreviationString = abbreviationString.replaceAll("\\.", "");
			if (abbreviationDictionary.lookup(fAbbreviationString))
				continue;
			fuzzyAbbreviationMappings.put(fAbbreviationString, abbreviationString);
		}
		
		//	set up fuzzy matching
		Dictionary fuzzyAbbreviationStrings = new Dictionary() {
			public boolean lookup(String string) {
				System.out.println("Abbreviation lookup (CS): '" + string + "'");
				if (abbreviationDictionary.lookup(string))
					return true;
				if (fuzzyAbbreviationMappings.containsKey(string))
					return true;
				if (string.indexOf('.') == -1)
					return false;
				String fString = string.replaceAll("\\.", "");
				if (abbreviationDictionary.lookup(fString)) {
//					fuzzyAbbreviationMappings.setProperty(fString, string);
					fuzzyAbbreviationMappings.setProperty(string, fString);
					return true;
				}
				return false;
			}
			public boolean lookup(String string, boolean caseSensitive) {
				System.out.println("Abbreviation lookup (" + (caseSensitive ? "CS" : "CI") + "): '" + string + "'");
				if (abbreviationDictionary.lookup(string, caseSensitive))
					return true;
				if (fuzzyAbbreviationMappings.containsKey(string))
					return true;
				if (string.indexOf('.') == -1)
					return false;
				String fString = string.replaceAll("\\.", "");
				if (abbreviationDictionary.lookup(fString, caseSensitive)) {
//					fuzzyAbbreviationMappings.setProperty(fString, string);
					fuzzyAbbreviationMappings.setProperty(string, fString);
					return true;
				}
				return false;
			}
			public boolean isDefaultCaseSensitive() {
				return abbreviationDictionary.isDefaultCaseSensitive();
			}
			public boolean isEmpty() {
				return abbreviationDictionary.isEmpty();
			}
			public int size() {
				return abbreviationDictionary.size();
			}
			public StringIterator getEntryIterator() {
				return abbreviationDictionary.getEntryIterator();
			}
		};
		if (!maxAbbbreviationSizeDotted)
			maxAbbreviationSize += (maxAbbreviationSize / 2);
		System.out.println("Max abbreviation size is " + maxAbbreviationSize);
		
		//	tag abbreviation references in individual paragraphs
		for (int p = 0; p < paragraphs.length; p++) {
			
			//	check paragraph (have to exclude abbreviation definitions ...)
			Annotation[] abbreviations = paragraphs[p].getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
			if (abbreviations.length != 0)
				continue;
			Annotation[] abbreviationDatas = paragraphs[p].getAnnotations(ABBREVIATION_DATA_ANNOTATION_TYPE);
			if (abbreviationDatas.length != 0)
				continue;
			
			//	tag references
			HashSet abbreviationTokens = new HashSet();
			this.annotateAbbreviationReferences(paragraphs[p], abbreviationResolver, fuzzyAbbreviationStrings, maxAbbreviationSize, fuzzyAbbreviationMappings, abbreviationTokens);
			this.annotateAbbreviationReferenceRanges(paragraphs[p], abbreviationResolver, abbreviationTokens);
		}
	}
	
	private void annotateAbbreviationReferenceRanges(MutableAnnotation paragraph, AbbreviationResolver abbreviationResolver, HashSet abbreviationTokens) {
		
		//	TODO maybe also try prefix or abbreviation match, as prefix might not always be written out in full
		
		//	store enum and range infix flags (helps deal with multi-token infixes)
		boolean[] isEnumInfix = new boolean[paragraph.size()];
		Arrays.fill(isEnumInfix, false);
		Annotation[] enumInfixes = Gamta.extractAllContained(paragraph, this.enumerationInfixes);
		for (int i = 0; i < enumInfixes.length; i++) {
			for (int t = enumInfixes[i].getStartIndex(); t < enumInfixes[i].getEndIndex(); t++)
				isEnumInfix[t] = true;
		}
		boolean[] isRangeInfix = new boolean[paragraph.size()];
		Arrays.fill(isRangeInfix, false);
		Annotation[] rangeInfixes = Gamta.extractAllContained(paragraph, this.rangeInfixes);
		for (int i = 0; i < rangeInfixes.length; i++) {
			for (int t = rangeInfixes[i].getStartIndex(); t < rangeInfixes[i].getEndIndex(); t++)
				isRangeInfix[t] = true;
		}
		
		//	extract prefixes
		Annotation[] prefixes = Gamta.extractAllContained(paragraph, abbreviationResolver.getAbbreviationRangePrefixDictionary());
		for (int p = 0; p < prefixes.length; p++) {
			String prefix = TokenSequenceUtils.concatTokens(prefixes[p], true, true);
			String numberingScheme = abbreviationResolver.getNumberingScheme(prefix);
			if (numberingScheme == null)
				continue;
			for (int t = prefixes[p].getEndIndex(); t < paragraph.size(); t++) {
				String token = paragraph.valueAt(t);
				if (isEnumInfix[t] || isRangeInfix[t])
					continue;
				int tValue = abbreviationResolver.getIntValue(token, prefix, true);
				if (tValue == -1) {
					if (abbreviationTokens.contains(new Integer(t)) && abbreviationResolver.isValidValue(token, prefix, false))
						continue;
					else break;
				}
				
				//	number range
				if (((t+2) < paragraph.size()) && this.rangeInfixes.contains(paragraph.valueAt(t+1)) && abbreviationResolver.isValidValue(paragraph.valueAt(t+2), prefix, true)) {
					
					//	get and check boundaries
					int minValue = tValue;
					int maxValue = abbreviationResolver.getIntValue(paragraph.valueAt(t+2), prefix, true);
					if ((maxValue != -1) && (minValue <= maxValue)) {
						
						//	annotate all abbreviations in range
						for (int v = minValue; v <= maxValue; v++) {
							
							//	get abbreviation string
							String[] nStrings = abbreviationResolver.getStringValues(v, prefix, true);
							if (nStrings == null)
								continue;
							
							//	annotate everything we have
							for (int n = 0; n < nStrings.length; n++) {
								String aString = (prefix + " " + nStrings[n]);
								System.out.println("Resolving abbreviation reference: " + aString);
								Annotation abbreviation = abbreviationResolver.resolveAbbreviation(aString);
								if (abbreviation != null) {
									Annotation abbreviationReference;
									if (t == prefixes[p].getEndIndex())
										abbreviationReference = paragraph.addAnnotation(ABBREVIATION_REFERENCE_ANNOTATION_TYPE, prefixes[p].getStartIndex(), ((t+3) - prefixes[p].getStartIndex()));
									else abbreviationReference = paragraph.addAnnotation(ABBREVIATION_REFERENCE_ANNOTATION_TYPE, t, 3);
									abbreviationReference.setAttribute("abbreviationString", aString);
									abbreviationReference.copyAttributes(abbreviation);
									System.out.println(" - resolved");
									for (int at = abbreviationReference.getStartIndex(); at < abbreviationReference.getEndIndex(); at++)
										abbreviationTokens.add(new Integer(at));
								}
								else System.out.println(" - references abbreviation not found");
							}
						}
						
						//	jump to end of range
						t += 2;
						continue;
					}
				}
				
				//	get abbreviation string
				String[] nStrings = abbreviationResolver.getStringValues(tValue, prefix, true);
				if (nStrings == null)
					break;
				
				//	annotate everything we have
				for (int n = 0; n < nStrings.length; n++) {
					String aString = (prefix + " " + nStrings[n]);
					System.out.println("Resolving abbreviation reference: " + aString);
					Annotation abbreviation = abbreviationResolver.resolveAbbreviation(aString);
					if (abbreviation != null) {
						Annotation abbreviationReference;
						if (t == prefixes[p].getEndIndex())
							abbreviationReference = paragraph.addAnnotation(ABBREVIATION_REFERENCE_ANNOTATION_TYPE, prefixes[p].getStartIndex(), ((t+1) - prefixes[p].getStartIndex()));
						else abbreviationReference = paragraph.addAnnotation(ABBREVIATION_REFERENCE_ANNOTATION_TYPE, t, 1);
						abbreviationReference.setAttribute("abbreviationString", aString);
						abbreviationReference.copyAttributes(abbreviation);
						System.out.println(" - resolved");
						for (int at = abbreviationReference.getStartIndex(); at < abbreviationReference.getEndIndex(); at++)
							abbreviationTokens.add(new Integer(at));
					}
					else System.out.println(" - references abbreviation not found");
				}
			}
		}
	}
	
	private void annotateAbbreviationReferences(MutableAnnotation paragraph, AbbreviationResolver abbreviationResolver, Dictionary abbreviationStrings, int maxAbbreviationTokens, Properties fuzzyAbbreviationMappings, HashSet abbreviationTokens) {
		
		//	TODO include abbreviation prefixes like "No" or "Nr" in matches if first value numeric, even if not given explicitly in definitions
		
		//	tag abbreviation references
		//	we cannot normalize here, as then the dictionary is replaced with a normalized version inside the extraction method
		ArrayList abbreviationReferences = new ArrayList(Arrays.asList(Gamta.extractAllContained(paragraph, abbreviationStrings, maxAbbreviationTokens, true, false, false)));
		
		//	sort out plain number abbreviation references at the start of the paragraph
		for (int a = 0; a < abbreviationReferences.size(); a++) {
			Annotation abbreviationReference = ((Annotation) abbreviationReferences.get(a));
			System.out.println("Got possible abbreviation reference: " + abbreviationReference);
			if (abbreviationReference.getStartIndex() > 0)
				continue;
			if (abbreviationReference.size() > 1)
				continue;
			if (!Gamta.isNumber(abbreviationReference.firstValue()))
				continue;
			if ((paragraph.size() == 1) || (".,".indexOf(paragraph.valueAt(1)) != -1))
				abbreviationReferences.remove(a--);
		}
		
		//	anything left?
		if (abbreviationReferences.isEmpty())
			return;
		
		//	collect (indices of) tokens nested in abbreviations
		Annotation[] abbreviations = paragraph.getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
		HashSet excludeTokens = new HashSet();
		for (int a = 0; a < abbreviations.length; a++) {
			for (int t = 0; t < abbreviations[a].size(); t++)
				excludeTokens.add(new Integer(abbreviations[a].getStartIndex() + t));
		}
		excludeTokens.addAll(abbreviationTokens);
		
		//	sort out abbreviation references overlapping with abbreviations
		for (int a = 0; a < abbreviationReferences.size(); a++) {
			Annotation abbreviationReference = ((Annotation) abbreviationReferences.get(a));
			for (int t = 0; t < abbreviationReference.size(); t++)
				if (excludeTokens.contains(new Integer(abbreviationReference.getStartIndex() + t))) {
					abbreviationReference = null;
					break;
				}
			if (abbreviationReference == null)
				abbreviationReferences.remove(a--);
		}
		
		//	collect environments of abbreviation references that are not plain numbers
		HashSet beforeTokens = new HashSet();
		HashSet afterTokens = new HashSet();
		for (int a = 0; a < abbreviationReferences.size(); a++) {
			Annotation abbreviationReference = ((Annotation) abbreviationReferences.get(a));
			if ((abbreviationReference.size() == 1) && Gamta.isNumber(abbreviationReference.firstValue()))
				continue;
			if (abbreviationReference.getStartIndex() != 0)
				beforeTokens.add(paragraph.valueAt(abbreviationReference.getStartIndex()-1));
			if (abbreviationReference.getEndIndex() < paragraph.size())
				afterTokens.add(paragraph.valueAt(abbreviationReference.getEndIndex()));
		}
		
		//	do we have anything to check against?
		if ((beforeTokens.size() != 0) && (afterTokens.size() != 0))
			
			//	sort out plain number abbreviation references with unusual environment around them
			for (int a = 0; a < abbreviationReferences.size(); a++) {
				Annotation abbreviationReference = ((Annotation) abbreviationReferences.get(a));
				
				//	we cannot test this one
				if ((abbreviationReference.getStartIndex() == 0) && (abbreviationReference.getEndIndex() == paragraph.size()))
					continue;
				
				//	we need not test this one
				if ((abbreviationReference.size() > 1) || !Gamta.isNumber(abbreviationReference.firstValue()))
					continue;
				
				//	this one's fine
				if (beforeTokens.contains(paragraph.valueAt(abbreviationReference.getStartIndex()-1)) || afterTokens.contains(paragraph.valueAt(abbreviationReference.getEndIndex())))
					continue;
				
				//	this one's suspect
				System.out.println("Excluding abbreviation reference for environment '" + paragraph.valueAt(abbreviationReference.getStartIndex()-1) + "' and '" + paragraph.valueAt(abbreviationReference.getEndIndex()) + "': " + abbreviationReference.toXML());
				abbreviationReferences.remove(a--);
			}
		
		//	transfer data
		for (int a = 0; a < abbreviationReferences.size(); a++) {
			Annotation abbreviationReference = ((Annotation) abbreviationReferences.get(a));
			String abbreviationString = TokenSequenceUtils.concatTokens(abbreviationReference, true, true);
			System.out.println("Resolving abbreviation reference: " + abbreviationString);
			Annotation abbreviation = abbreviationResolver.resolveAbbreviation(abbreviationString);
			if (abbreviation == null) {
				abbreviationString = fuzzyAbbreviationMappings.getProperty(abbreviationString, abbreviationString);
				System.out.println(" - fuzzy matched to: " + abbreviationString);
				abbreviation = abbreviationResolver.resolveAbbreviation(abbreviationString);
			}
			if (abbreviation != null) {
				abbreviationReference = paragraph.addAnnotation(ABBREVIATION_REFERENCE_ANNOTATION_TYPE, abbreviationReference.getStartIndex(), abbreviationReference.size());
				abbreviationReference.copyAttributes(abbreviation);
				System.out.println(" - resolved");
				for (int t = abbreviationReference.getStartIndex(); t < abbreviationReference.getEndIndex(); t++)
					abbreviationTokens.add(new Integer(t));
			}
			else System.out.println(" - references abbreviation not found");
		}
	}
}
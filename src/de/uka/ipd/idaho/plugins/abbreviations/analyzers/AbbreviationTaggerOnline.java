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
package de.uka.ipd.idaho.plugins.abbreviations.analyzers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.JDialog;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel;
import de.uka.ipd.idaho.plugins.abbreviations.AbbreviationConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class AbbreviationTaggerOnline extends AbstractConfigurableAnalyzer implements AbbreviationConstants {
	
	private String[] strictAbbreviationPatters = {
			"[1-9][0-9]{0,2}\\s?[a-z]+((\\s?[a-zA-Z])|(\\s?[1-9][0-9]{0,2}))?",
//			"[1-9][0-9]{0,2}(\\s?[a-z]+((\\s?[a-zA-Z])|(\\s?[1-9][0-9]{0,2}))?)?",
			"(No|Nr)\\.?\\s?[1-9][0-9]{0,2}(\\s?[a-z]+)?",
			"[A-Z]{2,3}(\\s?[1-9][0-9]{0,2})?",
		};
	private String[] relaxedAbbreviationPatters = {
//			"[1-9][0-9]{0,2}\\s?[a-z]+((\\s?[a-zA-Z])|(\\s?[1-9][0-9]{0,2}))?",
			"[1-9][0-9]{0,2}(\\s?[a-z]+((\\s?[a-zA-Z])|(\\s?[1-9][0-9]{0,2}))?)?",
			"(No|Nr)\\.?\\s?[1-9][0-9]{0,2}(\\s?[a-z]+((\\s?[a-zA-Z])|(\\s?[1-9][0-9]{0,2}))?)?",
//			"[A-Z]{2,3}(\\s?[1-9][0-9]{0,2})?",
		};
	private String paragraphNumberingTerminators = ".:;";
	private String abbreviationTerminators = ":;";
	private String abbreviationPrefixes = "Nr;No";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		// TODO load regular expression patterns and the other stuff
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	tag sequential numberings at the start of paragraphs
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		int[] paragraphNumbers = new int[paragraphs.length];
		Arrays.fill(paragraphNumbers, -1);
		boolean[] paragraphNumberRoman = new boolean[paragraphs.length];
		Arrays.fill(paragraphNumberRoman, false);
		
		//	collect numberings, remembering style
		for (int p = 0; p < paragraphs.length; p++) {
			if (paragraphs[p].size() < 4)
				continue;
			if (this.paragraphNumberingTerminators.indexOf(paragraphs[p].valueAt(1)) == -1)
				continue;
			String number = paragraphs[p].firstValue();
			if (number.matches("[1-9][0-9]*"))
				paragraphNumbers[p] = Integer.parseInt(number);
			else if (Gamta.isRomanNumber(number)) {
				paragraphNumbers[p] = Gamta.parseRomanNumber(number);
				paragraphNumberRoman[p] = true;
			}
		}
		
		//	find number sequences of matching style
		for (int p = 0; p < paragraphs.length; p++) {
			if (paragraphNumbers[p] == -1)
				continue;
			
			//	found start, measure out sequence
			int start = p;
			int end = (p+1);
			String terminator = paragraphs[p].valueAt(1);
			while (end < paragraphs.length) {
				if (paragraphNumbers[end] == -1)
					break;
				if (paragraphNumbers[end] != (paragraphNumbers[end-1] + 1))
					break;
				if (paragraphNumberRoman[end] != paragraphNumberRoman[end-1])
					break;
				if (!terminator.equals(paragraphs[end].valueAt(1)))
					break;
				end++;
			}
			
			/* here, p is either behind the last paragraph, or on the first non-matching one */
			
			//	sequence too short
			if ((end - 1) == start)
				continue;
			
			//	annotate abbreviations
			for (int a = start; a < end; a++) {
				Annotation abbreviation = paragraphs[a].addAnnotation(ABBREVIATION_ANNOTATION_TYPE, 0, 1);
				abbreviation.setAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE, "1");
				System.out.println("Got abbreviation: " + abbreviation.toXML());
			}
			
			//	set p to last element of abbreviation sequence to compensate loop increment
			p = (end-1);
		}
		
		//	tag abbreviations using regular expression patterns
		for (int sp = 0; sp < this.strictAbbreviationPatters.length; sp++) {
			Annotation[] abbreviations = Gamta.extractAllMatches(data, this.strictAbbreviationPatters[sp], false, false, true);
			for (int a = 0; a < abbreviations.length; a++)
				if ((abbreviations[a].getEndIndex() < data.size()) && (this.abbreviationTerminators.indexOf(data.valueAt(abbreviations[a].getEndIndex())) != -1)) {
					Annotation abbreviation = data.addAnnotation(ABBREVIATION_ANNOTATION_TYPE, abbreviations[a].getStartIndex(), abbreviations[a].size());
					int prefixSize = this.getPrefixSize(abbreviation);
					abbreviation.setAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE, ("" + (abbreviation.size() - prefixSize)));
					System.out.println("Got abbreviation: " + abbreviation.toXML());
				}
		}
		
		//	clean up
		AnnotationFilter.removeDuplicates(data, ABBREVIATION_ANNOTATION_TYPE);
		AnnotationFilter.removeContained(data, ABBREVIATION_ANNOTATION_TYPE, ABBREVIATION_ANNOTATION_TYPE);
		
		//	tag further abbreviations using prefixes of the ones found in previous step (paragraph internally only)
		for (int p = 0; p < paragraphs.length; p++) {
			Annotation[] abbreviations = paragraphs[p].getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
			if (abbreviations.length == 0)
				continue;
			
			StringVector abbreviationPrefixes = new StringVector();
			for (int a = 0; a < abbreviations.length; a++) {
				String abbreviationString = TokenSequenceUtils.concatTokens(abbreviations[a], true, true);
				for (int c = (abbreviationString.length()-1); c > 0; c--)
					abbreviationPrefixes.addElementIgnoreDuplicates(abbreviationString.substring(0, c).trim());
				if (this.abbreviationPrefixes.indexOf(abbreviations[a].firstValue()) != -1) {
					abbreviationString = TokenSequenceUtils.concatTokens(abbreviations[a], 1, (abbreviations[a].size() - 1), true, true);
					for (int c = (abbreviationString.length()-1); c > 0; c--)
						abbreviationPrefixes.addElementIgnoreDuplicates(abbreviationString.substring(0, c).trim());
				}
			}
			
			abbreviations = Gamta.extractAllContained(paragraphs[p], abbreviationPrefixes, true, false, true);
			for (int a = 0; a < abbreviations.length; a++)
				if ((abbreviations[a].size() > 1) || ((abbreviations[a].getEndIndex() < paragraphs[p].size()) && (this.abbreviationTerminators.indexOf(paragraphs[p].valueAt(abbreviations[a].getEndIndex())) != -1))) {
					Annotation abbreviation = paragraphs[p].addAnnotation(ABBREVIATION_ANNOTATION_TYPE, abbreviations[a].getStartIndex(), abbreviations[a].size());
					int prefixSize = this.getPrefixSize(abbreviation);
					abbreviation.setAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE, ("" + (abbreviation.size() - prefixSize)));
					System.out.println("Got prefix abbreviation: " + abbreviation.toXML());
				}
			
			//	clean up
			AnnotationFilter.removeContained(paragraphs[p], ABBREVIATION_ANNOTATION_TYPE, ABBREVIATION_ANNOTATION_TYPE);
		}
		
		//	count abbreviations per paragraph
		int[] paragraphAbbreviationCounts = new int[paragraphs.length];
		for (int p = 0; p < paragraphs.length; p++)
			paragraphAbbreviationCounts[p] = paragraphs[p].getAnnotations(ABBREVIATION_ANNOTATION_TYPE).length;
		
		//	find paragraphs surrounded by ones with abbreviations in them (likely to also include abbreviation, but none recognized so far)
		for (int p = 1; p < (paragraphs.length - 1); p++) {
			if (paragraphAbbreviationCounts[p] != 0)
				continue;
			if ((paragraphAbbreviationCounts[p-1] == 0) || (paragraphAbbreviationCounts[p+1] == 0))
				continue;
			System.out.println("Bridging paragraph: " + paragraphs[p].toXML());
			for (int rp = 0; rp < this.relaxedAbbreviationPatters.length; rp++) {
				Annotation[] abbreviations = Gamta.extractAllMatches(paragraphs[p], this.relaxedAbbreviationPatters[rp], false, false, true);
				for (int a = 0; a < abbreviations.length; a++) {
					if (abbreviations[a].getEndIndex() >= paragraphs[p].size())
						continue;
					if (this.abbreviationTerminators.indexOf(paragraphs[p].valueAt(abbreviations[a].getEndIndex())) == -1)
						continue;
					Annotation abbreviation = paragraphs[p].addAnnotation(ABBREVIATION_ANNOTATION_TYPE, abbreviations[a].getStartIndex(), abbreviations[a].size());
					int prefixSize = this.getPrefixSize(abbreviation);
					abbreviation.setAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE, ("" + (abbreviation.size() - prefixSize)));
					System.out.println("Got bridged abbreviation: " + abbreviation.toXML());
				}
			}
			
			//	clean up
			AnnotationFilter.removeContained(paragraphs[p], ABBREVIATION_ANNOTATION_TYPE, ABBREVIATION_ANNOTATION_TYPE);
			paragraphAbbreviationCounts[p] = paragraphs[p].getAnnotations(ABBREVIATION_ANNOTATION_TYPE).length;
		}
		
		//	collect abbreviations found so far
		Annotation[] abbreviations = data.getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
		StringVector abbreviationStrings = new StringVector();
		HashMap abbreviationsByString = new HashMap();
		for (int a = 0; a < abbreviations.length; a++) {
			String abbreviationString = TokenSequenceUtils.concatTokens(abbreviations[a], true, true);
			abbreviationStrings.addElementIgnoreDuplicates(abbreviationString);
			abbreviationsByString.put(abbreviationString, abbreviations[a]);
			int prefixSize = this.getPrefixSize(abbreviations[a]);
			if (prefixSize != 0) {
				abbreviationString = TokenSequenceUtils.concatTokens(abbreviations[a], prefixSize, (abbreviations[a].size() - prefixSize), true, true);
				abbreviationStrings.addElementIgnoreDuplicates(abbreviationString);
				abbreviationsByString.put(abbreviationString, abbreviations[a]);
			}
		}
		
		//	filter out level 2 or lower abbreviations appearing in a paragraph without their parent (unlikely to occur where abbreviations are introduced)
		for (int p = 0; p < paragraphs.length; p++) {
			abbreviations = paragraphs[p].getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
			if (abbreviations.length == 0)
				continue;
			System.out.println("Got abbreviations in paragraph: " + paragraphs[p].toXML());
			StringVector paragraphAbbreviationStrings = new StringVector();
			for (int a = 0; a < abbreviations.length; a++) {
				System.out.println(" - " + abbreviations[a].toXML());
				String abbreviationString = TokenSequenceUtils.concatTokens(abbreviations[a], true, true);
				paragraphAbbreviationStrings.addElementIgnoreDuplicates(abbreviationString);
				int prefixSize = this.getPrefixSize(abbreviations[a]);
				if (prefixSize != 0) {
					abbreviationString = TokenSequenceUtils.concatTokens(abbreviations[a], prefixSize, (abbreviations[a].size() - prefixSize), true, true);
					paragraphAbbreviationStrings.addElementIgnoreDuplicates(abbreviationString);
				}
			}
			for (int a = 0; a < abbreviations.length; a++) {
				if ("1".equals(abbreviations[a].getAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE)) || (abbreviations[a].size() == 1))
					continue;
				String parentAbbreviationString = TokenSequenceUtils.concatTokens(abbreviations[a], 0, (abbreviations[a].size() - 1), true, true);
				if (!paragraphAbbreviationStrings.contains(parentAbbreviationString) && abbreviationStrings.contains(parentAbbreviationString)) {
					System.out.println("Removing abbreviation for missing parent: " + abbreviations[a].toXML());
					paragraphs[p].removeAnnotation(abbreviations[a]);
				}
			}
			paragraphAbbreviationCounts[p] = paragraphs[p].getAnnotations(ABBREVIATION_ANNOTATION_TYPE).length;
		}
		
		//	reference tagged abbreviation to their parents
		for (int p = 0; p < paragraphs.length; p++) {
			abbreviations = paragraphs[p].getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
			if (abbreviations.length == 0)
				continue;
			
			for (int a = 0; a < abbreviations.length; a++) {
				if ("1".equals(abbreviations[a].getAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE)))
					continue;
				
				String abbreviationString = TokenSequenceUtils.concatTokens(abbreviations[a], true, true);
				for (int c = (abbreviationString.length()-1); c > 0; c--) {
					String parentAbbreviationString = abbreviationString.substring(0, c).trim();
					Annotation parentAbbreviation = ((Annotation) abbreviationsByString.get(parentAbbreviationString));
					if (parentAbbreviation != null) {
						abbreviations[a].setAttribute(PARENT_ABBREVIATION_ATTTRIBUTE, TokenSequenceUtils.concatTokens(parentAbbreviation, true, true));
//						String parentLevel = ((String) parentAbbreviation.getAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE));
//						if (parentLevel.equals(abbreviations[a].getAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE)))
//							abbreviations[a].setAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE, ("" + (Integer.parseInt(parentLevel) + 1)));
						int parentLevel = Integer.parseInt((String) parentAbbreviation.getAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE));
						abbreviations[a].setAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE, ("" + (parentLevel + 1)));
						c = 0;
					}
				}
				if (!abbreviations[a].hasAttribute(PARENT_ABBREVIATION_ATTTRIBUTE))
					abbreviations[a].setAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE, "1");
			}
		}
		
//		//	for tagging abbreviation references, remove level 1 abbreviations that have children from lookup list
//		for (int p = 0; p < paragraphs.length; p++) {
//			Annotation[] paragraphAbbreviations = paragraphs[p].getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
//			if (paragraphAbbreviations.length == 0)
//				continue;
//			StringVector paragraphAbbreviationStrings = new StringVector();
//			for (int a = 0; a < paragraphAbbreviations.length; a++)
//				paragraphAbbreviationStrings.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(paragraphAbbreviations[a], true, true));
//			
//			for (int a = 0; a < paragraphAbbreviations.length; a++) {
//				if (!"1".equals(paragraphAbbreviations[a].getAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE)) || (paragraphAbbreviations[a].size() > 1))
//					continue;
//				for (int s = 0; s < paragraphAbbreviationStrings.size(); s++) {
//					String paragraphAbbeviationString = paragraphAbbreviationStrings.get(s);
//					if (!paragraphAbbeviationString.equals(paragraphAbbreviations[a].firstValue()) && paragraphAbbeviationString.startsWith(paragraphAbbreviations[a].firstValue())) {
//						System.out.println("Excluding abbreviation for children: " + paragraphAbbreviations[a].toXML());
//						abbreviationStrings.removeAll(paragraphAbbreviations[a].firstValue());
//					}
//				}
//			}
//		}
//		
//		//	TODO_maybe_later find paragraphs containing references to abbreviations tagged so far, extract further possible abbreviations from them with more relaxed patterns, and use these values to find further abbreviations in paragraphs surrounded by ones with abbreviations in them
//		//	check paragraphs for abbreviation references
//		for (int p = 0; p < paragraphs.length; p++)
//			if ((paragraphAbbreviationCounts[p] == 0) && ((p == 0) || (paragraphAbbreviationCounts[p-1] == 0)) && (((p+1) == paragraphAbbreviationCounts.length) || (paragraphAbbreviationCounts[p+1] == 0))) {
//				Annotation[] referencedAbbreviations = Gamta.extractAllContained(paragraphs[p], abbreviationStrings, true, false, true);
//				while ((referencedAbbreviations.length > 0) && (referencedAbbreviations[0].getEndIndex() == 1) && Gamta.isNumber(referencedAbbreviations[0].firstValue()) && ((paragraphs[p].size() == 1) || (".,".indexOf(paragraphs[p].valueAt(1)) != -1))) {
//					Annotation[] newReferencedAbbreviations = new Annotation[referencedAbbreviations.length-1];
//					System.arraycopy(referencedAbbreviations, 1, newReferencedAbbreviations, 0, newReferencedAbbreviations.length);
//					referencedAbbreviations = newReferencedAbbreviations;
//				}
//				if (referencedAbbreviations.length == 0)
//					continue;
//				
//				System.out.println("Got abbreviation references in paragraph: " + paragraphs[p].toXML());
//				StringVector abbreviationReferenceStrings = new StringVector();
//				for (int a = 0; a < referencedAbbreviations.length; a++) {
//					System.out.println(" - " + referencedAbbreviations[a].toXML());
//					abbreviationReferenceStrings.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(referencedAbbreviations[a], true, true));
//				}
////				for (int rp = 0; rp < this.relaxedAbbreviationPatters.length; rp++) {
////					Annotation[] abbreviations = Gamta.extractAllMatches(paragraphs[p], this.relaxedAbbreviationPatters[rp], false, false, true);
////					for (int a = 0; a < abbreviations.length; a++)
////						if ((abbreviations[a].getEndIndex() < paragraphs[p].size()) && (this.abbreviationTerminators.indexOf(paragraphs[p].valueAt(abbreviations[a].getEndIndex())) != -1)) {
////							Annotation abbreviation = paragraphs[p].addAnnotation(ABBREVIATION_ANNOTATION_TYPE, abbreviations[a].getStartIndex(), abbreviations[a].size());
////							abbreviation.setAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE, ("" + abbreviation.size()));
////							System.out.println("Got bridged abbreviation: " + abbreviation.toXML());
////						}
////				}
////				
////				//	clean up
////				AnnotationFilter.removeContained(paragraphs[p], ABBREVIATION_ANNOTATION_TYPE, ABBREVIATION_ANNOTATION_TYPE);
////				paragraphAbbreviationCounts[p] = paragraphs[p].getAnnotations(ABBREVIATION_ANNOTATION_TYPE).length;
//			}
		
		/*	TODO get feedback for paragraphs containing abbreviations
		 *  include paragraphs without own abbreviations in feedback if surrounding paragraphs include abbreviations
		 *  use AnnotationEditorFeedbackPanel to let users annotate abbbreviations
		 *  rename abbreviations according to hierarchy level ==> facilitates using different colors
		 */
		
		//	chunk paragraphs with abbreviations into abbreviationData sections, and compile the textual definition of each abbreviation
		String terminators = (this.abbreviationTerminators + this.paragraphNumberingTerminators);
		for (int p = 0; p < paragraphs.length; p++) {
			abbreviations = paragraphs[p].getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
			if (abbreviations.length == 0)
				continue;
			
			//	pre-extract abbreviation levels to simplify comparison
			int[] abbreviationLevels = new int[abbreviations.length];
			for (int a = 0; a < abbreviations.length; a++)
				abbreviationLevels[a] = Integer.parseInt((String) abbreviations[a].getAttribute(ABBREVIATION_LEVEL_ATTTRIBUTE));
			
			//	compile textual definition
			for (int a = 0; a < abbreviations.length; a++) {
				int startIndex = abbreviations[a].getEndIndex();
				while (terminators.indexOf(paragraphs[p].valueAt(startIndex)) != -1)
					startIndex++;
				int endIndex = (((a+1) == abbreviations.length) ? paragraphs[p].size() : abbreviations[a+1].getStartIndex());
				String impliedText = "";
				if (startIndex < endIndex)
					impliedText = TokenSequenceUtils.concatTokens(paragraphs[p], startIndex, (endIndex - startIndex), false, true);
				if (abbreviationLevels[a] > 1) {
					for (int l = (a-1); l > -1; l--)
						if (abbreviationLevels[l] < abbreviationLevels[a]) {
							impliedText = ((abbreviations[l].getAttribute(IMPLIED_TEXT_ATTTRIBUTE)) + " " + impliedText);
							break;
						}
				}
				abbreviations[a].setAttribute(IMPLIED_TEXT_ATTTRIBUTE, impliedText);
			}
			
			//	mark abbreviation data section
			for (int a = 0; a < abbreviations.length; a++) {
				int dataStart = abbreviations[a].getStartIndex();
				int dataEnd = paragraphs[p].size();
				for (int l = (a+1); l < abbreviations.length; l++)
					if (abbreviationLevels[a] >= abbreviationLevels[l]) {
						dataEnd = abbreviations[l].getStartIndex();
						l = abbreviations.length;
					}
				if (dataStart < dataEnd)
					paragraphs[p].addAnnotation(ABBREVIATION_DATA_ANNOTATION_TYPE, dataStart, (dataEnd-dataStart));
			}
		}
	}
	
	private final int getPrefixSize(Annotation abbreviation) {
		return ((this.abbreviationPrefixes.indexOf(abbreviation.firstValue()) == -1) ? 0 : (".".equals(abbreviation.valueAt(1)) ? 2 : 1));
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#getConfigTitle()
	 */
	protected String getConfigTitle() {
		return "Configure Abbreviation Tagger";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#getConfigPanels(javax.swing.JDialog)
	 */
	protected AnalyzerConfigPanel[] getConfigPanels(JDialog dialog) {
		//	TODO use RegExConfigPanel for abbreviation patterns
		//	TODO use KeyPropertyConfigPanel for colors and maximum lenght (in tokens) of abbreviations of individual levels
		return super.getConfigPanels(dialog);
	}
}
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.NamedEntityConstants;
import de.uka.ipd.idaho.plugins.abbreviations.AbbreviationConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 */
public class AbbreviationRangeTagger extends AbstractConfigurableAnalyzer implements AbbreviationConstants, NamedEntityConstants {
	
	private String[] prefixPatterns = {};
	private StringVector rangeInfixes = new StringVector();
	private StringVector enumerationInfixes = new StringVector();
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			StringVector prefixPatterns = this.loadList(ABBREVIATION_RANGE_ANNOTATION_TYPE + "PrefixPatterns.regEx.txt");
			this.prefixPatterns = prefixPatterns.toStringArray();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
		
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
		
		//	get paragraphs
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	collect abbreviation ranges and their prefixes
		Annotation[][] abbreviationRanges = new Annotation[paragraphs.length][];
		HashSet prefixesWithRanges = new HashSet();
		for (int p = 0; p < paragraphs.length; p++) {
			abbreviationRanges[p] = null;
			
			//	get prefixes
			Annotation[] prefixes = this.getNumberingPrefixes(paragraphs[p]);
			if (prefixes.length == 0)
				continue;
			
			//	get sequences of numbers and number ranges
			Annotation[] numberings = this.getNumberings(paragraphs[p]);
			if (numberings.length == 0)
				continue;
			
			//	combine prefixes with numbering
			abbreviationRanges[p] = this.buildAbbreviationRanges(paragraphs[p], prefixes, numberings);
			if (abbreviationRanges[p].length == 0) {
				abbreviationRanges[p] = null;
				continue;
			}
			
			//	collect prefixes from true abbreviation ranges
			for (int a = 0; a < abbreviationRanges[p].length; a++) {
				Object valMin = abbreviationRanges[p][a].getAttribute(VALUE_ATTRIBUTE + "Min");
				Object valMax = abbreviationRanges[p][a].getAttribute(VALUE_ATTRIBUTE + "Max");
				if ((valMin != null) && (valMax != null) && !valMin.equals(valMax))
					prefixesWithRanges.add(abbreviationRanges[p][a].getAttribute("prefix"));
			}
		}
		
		//	annotate abbreviation ranges in paragraphs
		for (int p = 0; p < paragraphs.length; p++) {
			
			//	nothing to do in this paragraph
			if (abbreviationRanges[p] == null)
				continue;
			
			//	annotate abbreviation ranges
			for (int a = 0; a < abbreviationRanges[p].length; a++) {
				
				//	this one does not really have an abbreviation range prefix, ignore it
				if (!prefixesWithRanges.contains(abbreviationRanges[p][a].getAttribute("prefix")))
					continue;
				
				//	add annotation
				Annotation ar = paragraphs[p].addAnnotation(ABBREVIATION_RANGE_ANNOTATION_TYPE, abbreviationRanges[p][a].getStartIndex(), abbreviationRanges[p][a].size());
				ar.copyAttributes(abbreviationRanges[p][a]);
			}
			
			//	refresh abbreviation range annotations (we might have filtered a few)
			abbreviationRanges[p] = paragraphs[p].getAnnotations(ABBREVIATION_RANGE_ANNOTATION_TYPE);
			if (abbreviationRanges[p].length == 0) {
				abbreviationRanges[p] = null;
				continue;
			}
			
			//	mark data associated with abbreviation ranges
			for (int a = 0; a < abbreviationRanges[p].length; a++) {
				int dataStart = abbreviationRanges[p][a].getStartIndex();
				int dataEnd = (((a+1) == abbreviationRanges[p].length) ? paragraphs[p].size() : abbreviationRanges[p][a+1].getStartIndex());
				if (dataStart < dataEnd)
					paragraphs[p].addAnnotation(ABBREVIATION_DATA_ANNOTATION_TYPE, dataStart, (dataEnd-dataStart));
			}
		}
	}
	
	private Annotation[] buildAbbreviationRanges(MutableAnnotation paragraph, Annotation[] prefixes, Annotation[] numberings) {
		ArrayList abbreviationRangeList = new ArrayList();
		for (int p = 0; p < prefixes.length; p++) {
			for (int n = 0; n < numberings.length; n++) {
				if (numberings[n].getStartIndex() < prefixes[p].getEndIndex())
					continue;
				if (numberings[n].getStartIndex() > prefixes[p].getEndIndex())
					break;
				
				Annotation abbreviationRange = Gamta.newAnnotation(paragraph, null, prefixes[p].getStartIndex(), (numberings[n].getEndIndex() - prefixes[p].getStartIndex()));
				abbreviationRange.copyAttributes(numberings[n]);
				if (!abbreviationRange.hasAttribute(VALUE_ATTRIBUTE + "Min"))
					abbreviationRange.setAttribute((VALUE_ATTRIBUTE + "Min"), abbreviationRange.getAttribute(VALUE_ATTRIBUTE));
				if (!abbreviationRange.hasAttribute(VALUE_ATTRIBUTE + "Max"))
					abbreviationRange.setAttribute((VALUE_ATTRIBUTE + "Max"), abbreviationRange.getAttribute(VALUE_ATTRIBUTE));
				if (!abbreviationRange.hasAttribute(VALUE_ATTRIBUTE + "s")) {
					int min = Integer.parseInt((String) abbreviationRange.getAttribute(VALUE_ATTRIBUTE + "Min"));
					int max = Integer.parseInt((String) abbreviationRange.getAttribute(VALUE_ATTRIBUTE + "Max"));
					abbreviationRange.setAttribute((VALUE_ATTRIBUTE + "s"), ((min == max) ? ("" + min) : (min + "-" + max)));
				}
				abbreviationRange.setAttribute("prefix", TokenSequenceUtils.concatTokens(prefixes[p], true, true));
				abbreviationRangeList.add(abbreviationRange);
			}
		}
		return ((Annotation[]) abbreviationRangeList.toArray(new Annotation[abbreviationRangeList.size()]));
	}
	
	private Annotation[] getNumberingPrefixes(MutableAnnotation paragraph) {
		ArrayList prefixList = new ArrayList();
		HashSet prefixKeys = new HashSet();
		for (int pp = 0; pp < this.prefixPatterns.length; pp++) {
			Annotation[] prefixes = Gamta.extractAllMatches(paragraph,  this.prefixPatterns[pp], 7, true);
			for (int p = 0; p < prefixes.length; p++) {
				if (prefixKeys.add(prefixes[p].getStartIndex() + "-" + prefixes[p].getEndIndex()))
					prefixList.add(prefixes[p]);
			}
		}
		return ((Annotation[]) prefixList.toArray(new Annotation[prefixList.size()]));
	}
	
	private Annotation[] getNumberings(MutableAnnotation paragraph) {
		
		//	distinguish numbering styles
		ArrayList numbersArabic = new ArrayList();
		ArrayList numbersRoman = new ArrayList();
		ArrayList numbersLetter = new ArrayList();
		
		//	extract individual numbers
		for (int t = 0; t < paragraph.size(); t++) {
			String token = paragraph.valueAt(t);
			
			//	Arabic number
			if (token.matches("[1-9][0-9]*")) {
				Annotation number = Gamta.newAnnotation(paragraph, null, t, 1);
				number.setAttribute(VALUE_ATTRIBUTE, token);
				numbersArabic.add(number);
			}
			
			//	Roman number
			else if (Gamta.isRomanNumber(token)) {
				Annotation number = Gamta.newAnnotation(paragraph, null, t, 1);
				number.setAttribute(VALUE_ATTRIBUTE, ("" + Gamta.parseRomanNumber(token)));
				numbersRoman.add(number);
			}
			
			//	Letter number
			else if (token.matches("[A-Za-z]")) {
				Annotation number = Gamta.newAnnotation(paragraph, null, t, 1);
				number.setAttribute(VALUE_ATTRIBUTE, ("" + (token.toUpperCase().charAt(0) - 'A' + 1)));
				numbersLetter.add(number);
			}
		}
		
		//	get range infixes
		Annotation[] rangeInfixes = Gamta.extractAllContained(paragraph, this.rangeInfixes);
		
		//	conbine numbers into ranges if possible
		if (rangeInfixes.length != 0) {
			this.buildNumberCombinations(numbersArabic, paragraph, rangeInfixes, false);
			this.buildNumberCombinations(numbersRoman, paragraph, rangeInfixes, false);
			this.buildNumberCombinations(numbersLetter, paragraph, rangeInfixes, false);
		}
		
		//	get enumeration infixes
		Annotation[] enumInfixes = Gamta.extractAllContained(paragraph, this.enumerationInfixes);
		
		//	combine numbers and ranges into sequences
		if (enumInfixes.length != 0) {
			this.buildNumberCombinations(numbersArabic, paragraph, enumInfixes, true);
			this.buildNumberCombinations(numbersRoman, paragraph, enumInfixes, true);
			this.buildNumberCombinations(numbersLetter, paragraph, enumInfixes, true);
		}
		
		//	set number types
		this.setType(numbersArabic, "Arabic", false);
		this.setType(numbersRoman, "Roman", true);
		this.setType(numbersLetter, "Letter", true);
		
		//	score individual types
		int scoreArabic = this.score(numbersArabic);
		int scoreRoman = this.score(numbersRoman);
		int scoreLetter = this.score(numbersLetter);
		int maxScore = Math.max(scoreArabic, Math.max(scoreRoman, scoreLetter));
		
		//	combine lists of sufficiently high scoring types
		ArrayList numberings = new ArrayList();
		if ((scoreArabic * 5) > maxScore)
			numberings.addAll(numbersArabic);
		if ((scoreRoman * 5) > maxScore)
			numberings.addAll(numbersRoman);
		if ((scoreLetter * 5) > maxScore)
			numberings.addAll(numbersLetter);
		
		//	sort numberings
		Collections.sort(numberings);
		
		//	finally ...
		return ((Annotation[]) numberings.toArray(new Annotation[numberings.size()]));
	}
	
	private int singleScore = 1;
	private int rangeScore = 10;
	private int enumScore = 20;
	private int score(ArrayList numbers) {
		int score = 0;
		for (int n = 0; n < numbers.size(); n++) {
			Annotation number = ((Annotation) numbers.get(n));
			String min = ((String) number.getAttribute(VALUE_ATTRIBUTE + "Min"));
			String max = ((String) number.getAttribute(VALUE_ATTRIBUTE + "Max"));
			if ((min == null) || min.equals(max)) {
				score += this.singleScore;
				continue;
			}
			String values = ((String) number.getAttribute(VALUE_ATTRIBUTE + "s"));
			if (values == null) {
				score += this.rangeScore;
				continue;
			}
			if (values.indexOf('-') != -1)
				score += this.rangeScore;
			if (values.indexOf(',') != -1)
				score += this.enumScore;
		}
		return score;
	}
	
	private void setType(ArrayList numbers, String type, boolean determineCase) {
		for (int n = 0; n < numbers.size(); n++) {
			Annotation number = ((Annotation) numbers.get(n));
			if (determineCase) {
				boolean isUpperCase = true;
				boolean isLowerCase = true;
				for (int t = 0; t < number.size(); t++) {
					String token = number.valueAt(t);
					if (!token.equals(token.toLowerCase()))
						isLowerCase = false;
					if (!token.equals(token.toUpperCase()))
						isUpperCase = false;
				}
				if (isUpperCase)
					number.setAttribute("type", (type + "UC"));
				else if (isLowerCase)
					number.setAttribute("type", (type + "LC"));
				else number.setAttribute("type", type);
			}
			else number.setAttribute("type", type);
		}
	}
	
	private void buildNumberCombinations(ArrayList numbers, MutableAnnotation paragraph, Annotation[] infixes, boolean isEnums) {
		if (numbers.size() < 2)
			return;
		int infixIndex = 0;
		for (int n = 1; n < numbers.size(); n++) {
			Annotation fn = ((Annotation) numbers.get(n-1));
			while ((infixIndex < infixes.length) && (infixes[infixIndex].getStartIndex() < fn.getEndIndex()))
				infixIndex++;
			if (infixIndex == infixes.length)
				return;
			
			Annotation sn = ((Annotation) numbers.get(n));
			while ((infixIndex < infixes.length) && (infixes[infixIndex].getStartIndex() == fn.getEndIndex()) && (infixes[infixIndex].getEndIndex() > sn.getStartIndex()))
				infixIndex++;
			if (infixIndex == infixes.length)
				return;
			if (infixes[infixIndex].getStartIndex() != fn.getEndIndex())
				continue;
			if (infixes[infixIndex].getEndIndex() != sn.getStartIndex())
				continue;
			
			int fnvMin = Integer.parseInt((String) fn.getAttribute(VALUE_ATTRIBUTE));
			int fnvMax = Integer.parseInt((String) fn.getAttribute((VALUE_ATTRIBUTE + "Max"), ("" + fnvMin)));
			int snvMin = Integer.parseInt((String) sn.getAttribute(VALUE_ATTRIBUTE));
			int snvMax = Integer.parseInt((String) sn.getAttribute((VALUE_ATTRIBUTE + "Max"), ("" + snvMin)));
			if (fnvMax >= snvMin)
				continue;
			
			Annotation numberCombination = Gamta.newAnnotation(paragraph, null, fn.getStartIndex(), (sn.getEndIndex() - fn.getStartIndex()));
			numberCombination.setAttribute(VALUE_ATTRIBUTE, ("" + fnvMin));
			numberCombination.setAttribute((VALUE_ATTRIBUTE + "Min"), ("" + fnvMin));
			numberCombination.setAttribute((VALUE_ATTRIBUTE + "Max"), ("" + snvMax));
			if (isEnums)
				numberCombination.setAttribute((VALUE_ATTRIBUTE + "s"), (fn.getAttribute((VALUE_ATTRIBUTE + "s"), ((fnvMin == fnvMax) ? ("" + fnvMin) : (fnvMin + "-" + fnvMax))) + "," + ((snvMin == snvMax) ? ("" + snvMin) : (snvMin + "-" + snvMax))));
			numbers.set((n-1), numberCombination);
			numbers.remove(n);
			if (isEnums)
				n--;
		}
	}
}
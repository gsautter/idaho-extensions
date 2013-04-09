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
package de.uka.ipd.idaho.gamta.util.imaging.analyzers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * @author sautter
 */
public class PageNumberer extends AbstractConfigurableAnalyzer implements ImagingConstants {
	
	private static final String PAGE_NUMBER_CANDIDATE_TYPE = (PAGE_NUMBER_TYPE + "Candidate");
	private static final boolean DEBUG = true;
	
	private static class PageNumber {
		final int pageId;
		final int value;
		final int fuzzyness;
		final int ambiguity;
		int score = 0;
		//	!!! this constructor is for set lookups only !!!
		PageNumber(int value) {
			this.value = value;
			this.pageId = -1;
			this.fuzzyness = 1;
			this.ambiguity = 1;
		}
		PageNumber(int pageId, int value, int fuzzyness, int ambiguity) {
			this.value = value;
			this.pageId = pageId;
			this.fuzzyness = fuzzyness;
			this.ambiguity = ambiguity;
		}
		public boolean equals(Object obj) {
			return (((PageNumber) obj).value == this.value);
		}
		public int hashCode() {
			return this.value;
		}
		public String toString() {
			return ("" + this.value);
		}
		boolean isConsistentWith(PageNumber pn) {
			return ((this.value - pn.value) == (this.pageId - pn.pageId));
		}
		int getAdjustedScore() {
			return (((this.fuzzyness == Integer.MAX_VALUE) || (this.ambiguity == Integer.MAX_VALUE)) ? 0 : (this.score / (this.fuzzyness + this.ambiguity)));
		}
	}
	
	private String pageNumberPatternFuzzy;
	private String pageNumberPatternStrict;
	private HashMap pageNumberCharacterTranslations = new HashMap();
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			InputStream is = this.dataProvider.getInputStream("pageNumberCharacters.txt");
			StringVector characterLines = StringVector.loadList(is);
			is.close();
			
			TreeSet pageNumberCharacters = new TreeSet();
			for (int l = 0; l < characterLines.size(); l++) {
				String characterLine = characterLines.get(l).trim();
				if ((characterLine.length() != 0) && !characterLine.startsWith("//")) {
					int split = characterLine.indexOf(' ');
					if (split == -1)
						continue;
					String digit = characterLine.substring(0, split).trim();
					String characters = characterLine.substring(split).trim();
					if (Gamta.isNumber(digit)) {
						for (int c = 0; c < characters.length(); c++) {
							String character = characters.substring(c, (c+1));
							pageNumberCharacters.add(character);
							ArrayList characterTranslations = ((ArrayList) this.pageNumberCharacterTranslations.get(character));
							if (characterTranslations == null) {
								characterTranslations = new ArrayList(2);
								this.pageNumberCharacterTranslations.put(character, characterTranslations);
							}
							characterTranslations.add(digit);
						}
						pageNumberCharacters.add(digit);
						ArrayList characterTranslations = ((ArrayList) this.pageNumberCharacterTranslations.get(digit));
						if (characterTranslations == null) {
							characterTranslations = new ArrayList(2);
							this.pageNumberCharacterTranslations.put(digit, characterTranslations);
						}
						characterTranslations.add(digit);
					}
				}
			}
			for (int d = 0; d < Gamta.DIGITS.length(); d++)
				pageNumberCharacters.add(Gamta.DIGITS.substring(d, (d+1)));
			
			StringBuffer pncPatternBuilder = new StringBuffer();
			for (Iterator cit = pageNumberCharacters.iterator(); cit.hasNext();) {
				String pnc = ((String) cit.next());
				pncPatternBuilder.append(pnc);
				ArrayList characterTranslations = ((ArrayList) this.pageNumberCharacterTranslations.get(pnc));
				if (characterTranslations == null)
					continue;
				int[] cts = new int[characterTranslations.size()];
				for (int t = 0; t < characterTranslations.size(); t++)
					cts[t] = Integer.parseInt((String) characterTranslations.get(t));
				this.pageNumberCharacterTranslations.put(pnc, cts);
			}
			String pncPattern = ("[" + RegExUtils.escapeForRegEx(pncPatternBuilder.toString()) + "]");
			this.pageNumberPatternFuzzy = (pncPattern + "+(\\s?" + pncPattern + "+)*");
		}
		catch (IOException ioe) {
			this.pageNumberPatternFuzzy = "[0-9]++";
		}
		
		this.pageNumberPatternStrict = "[1-9][0-9]++";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get pages
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		
		//	process lines in each page & collect page number data
		int[] pageIds = new int[pages.length];
		HashSet[] pageNumberSets = new HashSet[pages.length];
		for (int p = 0; p < pages.length; p++) {
			
			//	store page ID
			pageIds[p] = Integer.parseInt((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE, ("" + p)));
			
			//	are we handling OCR output?
			boolean isOcred = pages[p].hasAttribute(IS_OCRED_ATTRIBUTE);
			
			//	get all candidates
			ArrayList pageNumberList = new ArrayList();
			
			//	extract page numbers from STR attribute of words instead
			Annotation[] words = pages[p].getAnnotations(WORD_ANNOTATION_TYPE);
			for (int w = 0; w < words.length; w++) {
				String str = ((String) words[w].getAttribute(STRING_ATTRIBUTE));
				if (str == null)
					continue;
				TokenSequence strTs = pages[p].getTokenizer().tokenize(str);
				if (isOcred) {
					Annotation[] pageNumberCandidates = Gamta.extractAllMatches(strTs, this.pageNumberPatternFuzzy, 3, true, false, true);
					for (int c = 0; c < pageNumberCandidates.length; c++) {
						Annotation wpn = pages[p].addAnnotation(PAGE_NUMBER_CANDIDATE_TYPE, words[w].getStartIndex(), 1);
						wpn.setAttribute(STRING_ATTRIBUTE, pageNumberCandidates[c].getValue().replaceAll("\\s++", ""));
						pageNumberList.add(wpn);
					}
				}
				else {
					Annotation[] pageNumberCandidates = Gamta.extractAllMatches(strTs, this.pageNumberPatternStrict, 3, true, false, true);
					for (int c = 0; c < pageNumberCandidates.length; c++) {
						Annotation wpn = pages[p].addAnnotation(PAGE_NUMBER_CANDIDATE_TYPE, words[w].getStartIndex(), 1);
						wpn.setAttribute(STRING_ATTRIBUTE, pageNumberCandidates[c].getValue().replaceAll("\\s++", ""));
						pageNumberList.add(wpn);
					}
				}
			}
			
			//	process candidates
			Annotation[] pageNumberCandidates = ((Annotation[]) pageNumberList.toArray(new Annotation[pageNumberList.size()]));
			pageNumberList.clear();
			for (int n = 0; n < pageNumberCandidates.length; n++) {
				
				//	use only top and bottom of page
				if ((pageNumberCandidates[n].getStartIndex() > 25) && (pageNumberCandidates[n].getEndIndex() < (pages[p].size() - 25)))
					continue;
				
				if (DEBUG) System.out.println("Possible page number on page " + pageIds[p] + ": " + pageNumberCandidates[n].getAttribute(STRING_ATTRIBUTE, pageNumberCandidates[n].getValue()));
				
				//	get page number string
				String pageNumberString = ((String) pageNumberCandidates[n].getAttribute(STRING_ATTRIBUTE));
				int fuzzyness = 0;
				int ambiguity = 1;
				
				//	if we have a born-digital document, there's no need to care about OCR errors
				if (!isOcred) {
					pageNumberCandidates[n].changeTypeTo(PAGE_NUMBER_CANDIDATE_TYPE);
					pageNumberCandidates[n].setAttribute("fuzzyness", ("" + fuzzyness));
					pageNumberCandidates[n].setAttribute("ambiguity", ("" + ambiguity));
					String pageNumberValue = pageNumberString.replaceAll("\\s+", "");
					pages[p].addAnnotation(pageNumberCandidates[n]).setAttribute("value", pageNumberValue.toString());
					pageNumberList.add(new PageNumber(pageIds[p], Integer.parseInt(pageNumberValue.toString()), fuzzyness, ambiguity));
					if (DEBUG) System.out.println(" ==> value is " + pageNumberValue.toString());
					continue;
				}
				
				//	extract possible interpretations, computing fuzzyness and ambiguity along the way
				int [][] pageNumberDigits = new int[pageNumberString.length()][];
				for (int c = 0; c < pageNumberString.length(); c++) {
					String pnc = pageNumberString.substring(c, (c+1));
					pageNumberDigits[c] = ((int[]) this.pageNumberCharacterTranslations.get(pnc));
					if (pageNumberDigits[c] == null) {
						pageNumberDigits = null;
						break;
					}
					ambiguity *= pageNumberDigits[c].length;
					if (pageNumberDigits[c].length > 1)
						fuzzyness++;
				}
				if (pageNumberDigits == null)
					continue;
				
				//	the first digit is never zero ...
				if ((pageNumberDigits[0].length == 1) && (pageNumberDigits[0][0] == 0)) {
					if (DEBUG) System.out.println(" ==> ignoring zero start");
					continue;
				}
				
				//	page numbers with six or more digits are rather improbable ...
				if (pageNumberDigits.length >= 6) {
					if (DEBUG) System.out.println(" ==> ignoring for over-length");
					continue;
				}
				
				//	set type and base attributes
				pageNumberCandidates[n].changeTypeTo(PAGE_NUMBER_CANDIDATE_TYPE);
				pageNumberCandidates[n].setAttribute("fuzzyness", ("" + fuzzyness));
				if (DEBUG) System.out.println(" - fuzzyness is " + fuzzyness);
				pageNumberCandidates[n].setAttribute("ambiguity", ("" + ambiguity));
				if (DEBUG) System.out.println(" - ambiguity is " + ambiguity);
				
				//	this one is clear, annotate it right away
				if (ambiguity == 1) {
					StringBuffer pageNumberValue = new StringBuffer();
					for (int d = 0; d < pageNumberDigits.length; d++)
						pageNumberValue.append("" + pageNumberDigits[d][0]);
					pages[p].addAnnotation(pageNumberCandidates[n]).setAttribute("value", pageNumberValue.toString());
					pageNumberList.add(new PageNumber(pageIds[p], Integer.parseInt(pageNumberValue.toString()), fuzzyness, ambiguity));
					if (DEBUG) System.out.println(" ==> value is " + pageNumberValue.toString());
				}
				
				//	deal with ambiguity
				else {
					TreeSet values = new TreeSet();
					int[] digits = new int[pageNumberDigits.length];
					getAllPossibleValues(pageNumberDigits, digits, 0, values);
					for (Iterator vit = values.iterator(); vit.hasNext();) {
						String pageNumberValue = ((String) vit.next());
						pages[p].addAnnotation(pageNumberCandidates[n]).setAttribute("value", pageNumberValue);
						pageNumberList.add(new PageNumber(pageIds[p], Integer.parseInt(pageNumberValue.toString()), fuzzyness, ambiguity));
						if (DEBUG) System.out.println(" ==> possible value is " + pageNumberValue);
					}
				}
			}
			
			//	aggregate page numbers
			Collections.sort(pageNumberList, new Comparator() {
				public int compare(Object o1, Object o2) {
					PageNumber pn1 = ((PageNumber) o1);
					PageNumber pn2 = ((PageNumber) o2);
					return ((pn1.value == pn2.value) ? (pn1.fuzzyness - pn2.fuzzyness) : (pn1.value - pn2.value));
				}
			});
			pageNumberSets[p] = new HashSet();
			for (int n = 0; n < pageNumberList.size(); n++)
				pageNumberSets[p].add(pageNumberList.get(n));
		}
		
		//	score page numbers
		for (int p = 0; p < pages.length; p++) {
			for (Iterator pnit = pageNumberSets[p].iterator(); pnit.hasNext();) {
				PageNumber pn = ((PageNumber) pnit.next());
				if (DEBUG) System.out.println("Scoring page number " + pn + " for page " + pn.pageId);
				
				//	look forward
				int fMisses = 0;
				for (int l = 1; (p+l) < pages.length; l++) {
					if (pageNumberSets[p+l].contains(new PageNumber(pn.value + (pageIds[p+l] - pageIds[p]))))
						pn.score += l;
					else {
						fMisses++;
						if (fMisses == 3)
							l = pages.length;
					}
				}
				
				//	look backward
				int bMisses = 0;
				for (int l = 1; l <= p; l++) {
					if (pageNumberSets[p-l].contains(new PageNumber(pn.value + (pageIds[p-l] - pageIds[p]))))
						pn.score += l;
					else {
						bMisses++;
						if (bMisses == 3)
							l = (p+1);
					}
				}
				
				if (DEBUG) System.out.println(" ==> score is " + pn.score);
			}
		}
		
		//	select page number for each page
		PageNumber[] pageNumbers = new PageNumber[pages.length];
		for (int p = 0; p < pages.length; p++) {
			int bestPageNumberScore = 0;
			for (Iterator pnit = pageNumberSets[p].iterator(); pnit.hasNext();) {
				PageNumber pn = ((PageNumber) pnit.next());
				if (pn.score > bestPageNumberScore) {
					pageNumbers[p] = pn;
					bestPageNumberScore = pn.score;
				}
			}
			if (DEBUG) {
				if (pageNumbers[p] == null)
					System.out.println("Could not determine page number of page " + pageIds[p] + ".");
				else System.out.println("Determined page number of page " + pageIds[p] + " as " + pageNumbers[p] + " (score " + pageNumbers[p].score + ")");
			}
		}
		
		//	fill in sequence gaps
		for (int p = 0; p < pages.length; p++) {
			if (pageNumbers[p] == null)
				pageNumbers[p] = new PageNumber(pageIds[p], -1, Integer.MAX_VALUE, Integer.MAX_VALUE);
		}
		
		//	do sequence base correction
		for (int p = 1; p < (pages.length - 1); p++) {
			if (pageNumbers[p+1].isConsistentWith(pageNumbers[p-1]) && !pageNumbers[p+1].isConsistentWith(pageNumbers[p])) {
				int beforeScore = pageNumbers[p-1].score;
				int ownScore = pageNumbers[p].score;
				int afterScore = pageNumbers[p+1].score;
				if ((beforeScore + afterScore) > (ownScore * 3)) {
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, (pageNumbers[p-1].value + (pageNumbers[p].pageId - pageNumbers[p-1].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE);
					pageNumbers[p].score = ((beforeScore + afterScore) / 3);
					if (DEBUG) System.out.println("Corrected page number of page " + pageIds[p] + " to " + pageNumbers[p] + " (score " + ((beforeScore + afterScore) / 2) + " over " + ownScore + ")");
				}
			}
		}
		
		//	do backward sequence extrapolation
		for (int p = (pages.length - 2); p >= 0; p--) {
			if (!pageNumbers[p+1].isConsistentWith(pageNumbers[p])) {
				int ownScore = pageNumbers[p].score;
				int afterScore = pageNumbers[p+1].score;
				if (afterScore > (ownScore * 2)) {
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, (pageNumbers[p+1].value - (pageNumbers[p+1].pageId - pageNumbers[p].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE);
					pageNumbers[p].score = (afterScore / 2);
					if (DEBUG) System.out.println("Extrapolated (backward) page number of page " + pageIds[p] + " to " + pageNumbers[p] + " (score " + afterScore + " over " + ownScore + ")");
				}
			}
		}
		
		//	do forward sequence extrapolation
		for (int p = 1; p < pages.length; p++) {
			if (!pageNumbers[p].isConsistentWith(pageNumbers[p-1])) {
				int beforeScore = pageNumbers[p-1].score;
				int ownScore = pageNumbers[p].score;
				if (beforeScore > (ownScore * 2)) {
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, (pageNumbers[p-1].value + (pageNumbers[p].pageId - pageNumbers[p-1].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE);
					pageNumbers[p].score = (beforeScore / 2);
					if (DEBUG) System.out.println("Extrapolated (forward) page number of page " + pageIds[p] + " to " + pageNumbers[p] + " (score " + beforeScore + " over " + ownScore + ")");
				}
			}
		}
		
		//	disambiguate page numbers that occur on multiple pages (using fuzzyness and ambiguity)
		HashMap allPageNumbers = new HashMap();
		for (int p = 0; p < pages.length; p++) {
			if (allPageNumbers.containsKey(pageNumbers[p])) {
				PageNumber pn = ((PageNumber) allPageNumbers.get(pageNumbers[p]));
				if (pn.getAdjustedScore() < pageNumbers[p].getAdjustedScore()) {
					if (DEBUG) System.out.println("Ousted page number " + pn.value + " of page " + pn.pageId + " (score " + pn.getAdjustedScore() + " against " + pageNumbers[p].getAdjustedScore() + ")");
					allPageNumbers.put(pageNumbers[p], pageNumbers[p]);
					pn.score = 0;
					pageNumbers[pn.pageId] = new PageNumber(pn.pageId, -1, Integer.MAX_VALUE, Integer.MAX_VALUE);
				}
				else {
					if (DEBUG) System.out.println("Ousted page number " + pageNumbers[p].value + " of page " + pageNumbers[p].pageId + " (score " + pageNumbers[p].getAdjustedScore() + " against " + pn.getAdjustedScore() + ")");
					pageNumbers[p].score = 0;
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, -1, Integer.MAX_VALUE, Integer.MAX_VALUE);
				}
			}
			else if (pageNumbers[p].value != -1) allPageNumbers.put(pageNumbers[p], pageNumbers[p]);
		}
		
		if (DEBUG) for (int p = 0; p < pages.length; p++)
			System.out.println("Page number of page " + pageNumbers[p].pageId + " is " + pageNumbers[p] + " (score " + pageNumbers[p].score + ")");
		
		//	annotate page numbers and set attributes
		for (int p = 0; p < pages.length; p++) {
			String pageNumberValue = ("" + pageNumbers[p].value);
			
			//	set page attribute
			pages[p].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumberValue);
			
			//	deal with page number candidates
			Annotation[] pageNumberCandidates = pages[p].getAnnotations(PAGE_NUMBER_CANDIDATE_TYPE);
			
			//	if value is -1, we only need to clean up
			if (pageNumbers[p].value == -1) {
				for (int n = 0; n < pageNumberCandidates.length; n++)
					pages[p].removeAnnotation(pageNumberCandidates[n]);
				continue;
			}
			
			//	find (most probable) original annotation otherwise
			int minFuzzyness = (pageNumberValue.length() + 1);
			Annotation leastFuzzyPageNumber = null;
			for (int n = 0; n < pageNumberCandidates.length; n++) {
				
				//	value matches page number value
				if (pageNumberValue.equals(pageNumberCandidates[n].getAttribute("value"))) {
					int fuzzyness = Integer.parseInt((String) pageNumberCandidates[n].getAttribute("fuzzyness"));
					
					//	too risky, could be some single letter in the middle of a line of text
					if ((fuzzyness == pageNumberValue.length()) && (pageNumberValue.length() == 1) && !Gamta.isNumber(pageNumberValue))
						pages[p].removeAnnotation(pageNumberCandidates[n]);
					
					//	we have a new best match
					else if (fuzzyness < minFuzzyness) {
						if (leastFuzzyPageNumber != null)
							pages[p].removeAnnotation(leastFuzzyPageNumber);
						leastFuzzyPageNumber = pageNumberCandidates[n];
						minFuzzyness = fuzzyness;
					}
					
					//	clean up
					else pages[p].removeAnnotation(pageNumberCandidates[n]);
				}
				
				//	no match, clean up
				else pages[p].removeAnnotation(pageNumberCandidates[n]);
			}
			
			//	we have found a page number, promote annotation
			if (leastFuzzyPageNumber != null) {
				leastFuzzyPageNumber.changeTypeTo(PAGE_NUMBER_TYPE);
				leastFuzzyPageNumber.setAttribute("score", ("" + pageNumbers[p].score));
				if (DEBUG) System.out.println("Annotated page number of page " + pageNumbers[p].pageId + " as " + leastFuzzyPageNumber.toXML() + " at " + leastFuzzyPageNumber.getStartIndex());
			}
		}
	}
	
	private static final void getAllPossibleValues(int[][] baseDigits, int[] digits, int pos, TreeSet values) {
		if (pos == digits.length) {
			StringBuffer value = new StringBuffer();
			for (int d = 0; d < digits.length; d++)
				value.append("" + digits[d]);
			values.add(value.toString());
		}
		else for (int d = 0; d < baseDigits[pos].length; d++) {
			digits[pos] = baseDigits[pos][d];
			getAllPossibleValues(baseDigits, digits, (pos+1), values);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	// TODO Auto-generated method stub
	
	}
}

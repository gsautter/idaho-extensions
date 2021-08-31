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
package de.uka.ipd.idaho.plugins.bibRefs.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.accessories.MapDictionary;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringRelation;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * In-memory representation of a thesaurus of journal names and their
 * individual tokens, including abbreviation to full form relationships,
 * frequency of token pairs occurring in sequence, occurrence of tokens at the
 * start or end of a journal name, stop words, etc.
 * 
 * @author sautter
 */
public class JournalNameThesaurus {
	private HashMap termData = new HashMap();
	private static class TermData {
		final String term;
		CountingSet abbreviations = null;
		CountingSet fullForms = null;
		CountingSet successors = null;
		int count;
		int startCount;
		int endCount;
		int inBracketCount;
		int afterCommaCount;
		int stopCount;
		TermData(String term) {
			this.term = term;
		}
		
		int getSuccessorCount() {
			return ((this.successors == null) ? 0 : this.successors.size());
		}
		int getSuccessorCount(TermData successor) {
			return ((this.successors == null) ? 0 : this.successors.getCount(successor));
		}
		void addSuccessor(TermData successor, int frequency) {
			if (successor == null)
				return;
			if (this.successors == null)
				this.successors = new CountingSet(new HashMap(4));
			this.successors.add(successor, frequency);
		}
		String[] getSuccessors() {
			if (this.successors == null)
				return new String[0];
			TreeSet ss = new TreeSet();
			for (Iterator sit = this.successors.iterator(); sit.hasNext();) {
				TermData s = ((TermData) sit.next());
				ss.add(s.term);
			}
			return ((String[]) ss.toArray(new String[ss.size()]));
		}
		
		void addAbbreviation(TermData abbreviation, int frequency) {
			if (abbreviation == null)
				return;
			if (this.abbreviations == null)
				this.abbreviations = new CountingSet(new HashMap(4));
			this.abbreviations.add(abbreviation, frequency);
		}
		void addFullForm(TermData fullForm, int frequency) {
			if (fullForm == null)
				return;
			if (this.fullForms == null)
				this.fullForms = new CountingSet(new HashMap(4));
			this.fullForms.add(fullForm, frequency);
		}
		
		boolean isAbbreviation() {
			return (this.fullForms != null);
		}
		String getFullForm() {
			if (this.fullForms == null)
				return this.term;
			return ((TermData) this.fullForms.max()).getFullForm();
		}
		String[] getFullForms(boolean transitive, boolean includeSelf) {
			if (this.fullForms == null) {
				String[] ff = {this.term};
				return ff;
			}
			TreeSet ffs = new TreeSet();
			if (includeSelf)
				ffs.add(this.term);
			for (Iterator ffit = this.fullForms.iterator(); ffit.hasNext();) {
				TermData ff = ((TermData) ffit.next());
				if (transitive)
					ff.addFullForms(ffs, includeSelf);
				else ffs.add(ff.term);
			}
			return ((String[]) ffs.toArray(new String[ffs.size()]));
		}
		private void addFullForms(TreeSet ffs, boolean includeSelf) {
			if (this.fullForms == null) {
				ffs.add(this.term);
				return;
			}
			if (includeSelf)
				ffs.add(this.term);
			for (Iterator ffit = this.fullForms.iterator(); ffit.hasNext();) {
				TermData ff = ((TermData) ffit.next());
				ff.addFullForms(ffs, includeSelf);
			}
		}
		int getFullFormFrequency(TermData fullForm) {
			return (((this.fullForms == null) || (fullForm == null)) ? 0 : this.fullForms.getCount(fullForm));
		}
		
		boolean isFullForm() {
			return (this.fullForms == null);
		}
		String[] getAbbreviations(boolean transitive) {
			if (this.abbreviations == null)
				return new String[0];
			TreeSet abs = new TreeSet();
			for (Iterator abit = this.abbreviations.iterator(); abit.hasNext();) {
				TermData ab = ((TermData) abit.next());
				if (transitive)
					ab.addAbbreviations(abs);
				else abs.add(ab.term);
			}
			return ((String[]) abs.toArray(new String[abs.size()]));
		}
		int getAbbreviationFrequency(TermData abbreviation) {
			return (((this.abbreviations == null) || (abbreviation == null)) ? 0 : this.abbreviations.getCount(abbreviation));
		}
		private void addAbbreviations(TreeSet abs) {
			abs.add(this.term);
			if (this.abbreviations == null)
				return;
			for (Iterator abit = this.abbreviations.iterator(); abit.hasNext();) {
				TermData ab = ((TermData) abit.next());
				ab.addAbbreviations(abs);
			}
		}
		
		boolean isAfterCommaPart() {
			if (this.afterCommaCount != 0)
				return true;
			if (this.fullForms == null)
				return false;
			for (Iterator ffit = this.fullForms.iterator(); ffit.hasNext();) {
				TermData ff = ((TermData) ffit.next());
				if (ff.isAfterCommaPart())
					return true;
			}
			return false;
		}
		
		boolean isInBracketPart() {
			if (this.inBracketCount != 0)
				return true;
			if (this.fullForms == null)
				return false;
			for (Iterator ffit = this.fullForms.iterator(); ffit.hasNext();) {
				TermData ff = ((TermData) ffit.next());
				if (ff.isInBracketPart())
					return true;
			}
			return false;
		}
		
		boolean isStopWord() {
			return (this.count < (this.stopCount * 2));
		}
		boolean isStopWordOnly() {
			return (this.count == this.stopCount);
		}
	}
	
	private int totalCount = 0;
	private int totalStopCount = 0;
	private int totalStartCount = 0;
	private int totalSuccessorCount = 0;
	
	/* TODO
Add "resolve" argument to getSuccessors() ...
... and set to false on storing
==> gets accurate count
==> facilitates restoring exact graph
==> unresolved lookup should also help with abbreviation sequences

Add getSuccessorCount() method to thesaurus

Add getPredecessorCount() method as well:
- link on linking successors
- makes edges traversable in both directions (if at cost of another CountingSet)
	 */
	
	TermData getTermData(String string) {
		return ((string == null) ? null : ((TermData) this.termData.get(string.toLowerCase())));
	}
	TermData getOrCreateTermData(String string) {
		if (string == null)
			return null;
		TermData td = this.getTermData(string);
		if (td == null) {
			td = new TermData(string);
			this.termData.put(string.toLowerCase(), td);
		}
		return td;
	}
	
	StringIterator getTermIterator() {
		final Iterator it = this.termData.keySet().iterator();
		return new StringIterator() {
			public boolean hasNext() {
				return this.hasMoreStrings();
			}
			public Object next() {
				return this.nextString();
			}
			public void remove() {
				it.remove();
			}
			public boolean hasMoreStrings() {
				return it.hasNext();
			}
			public String nextString() {
				TermData td = getTermData((String) it.next());
				//	TODO anything to filter ???
				return td.term;
			}
		};
	}
	
	void addTerm(String term, boolean isStopWord, int frequency) {
		TermData td = this.getOrCreateTermData(term);
		td.count += frequency;
		this.totalCount += frequency;
		if (isStopWord) {
			td.stopCount += frequency;
			this.totalStopCount += frequency;
		}
	}
	
	/**
	 * Retrieve a dictionary containing all journal name tokens that are not
	 * exclusively stop words (for tagging).
	 * @param caseSensitive create a case sensitive dictionary?
	 * @return the dictionary
	 */
	public Dictionary getMultigramPartDictionary(boolean caseSensitive) {
		return this.getMultigramPartDictionary(true, true);
	}
	
	/**
	 * Retrieve a dictionary containing all journal name tokens that are not
	 * exclusively stop words (for tagging).
	 * @param caseSensitive create a case sensitive dictionary?
	 * @param includeAbbreviations include abbreviations in the dictionary?
	 * @return the dictionary
	 */
	public Dictionary getMultigramPartDictionary(boolean caseSensitive, final boolean includeAbbreviations) {
		final boolean dictCaseSensitive = caseSensitive;
		return new MapDictionary(this.termData, true) {
			public boolean lookup(String string, boolean caseSensitive) {
				if ((string == null) || (string.length() < 1))
					return false;
				if (dictCaseSensitive && (Character.toUpperCase(string.charAt(0)) != string.charAt(0)))
					return false;
				TermData td = getTermData(string);
				if ((td == null) || td.isStopWordOnly())
					return false;
				return (includeAbbreviations || td.isFullForm());
			}
			public StringIterator getEntryIterator() {
				return new UnWrappingStringIterator(super.getEntryIterator()) {
					boolean accept(TermData td) {
						return (!td.isStopWordOnly() && (includeAbbreviations || td.isFullForm()));
					}
				};
			}
		};
	}
	
	void addAbbreviation(String abbreviation, String term, int frequency) {
		if (abbreviation.equalsIgnoreCase(term))
			return;
		TermData ttd = this.getTermData(term);
		if (ttd == null)
			return;
		TermData atd = this.getOrCreateTermData(abbreviation);
		ttd.addAbbreviation(atd, frequency);
		atd.addFullForm(ttd, frequency);
	}
	
	/**
	 * Test whether or not a given token is an abbreviation of one or more
	 * longer tokens, i.e., if it is mapped to one or more full forms.
	 * @param string the token to check.
	 * @return true if the argument token is an abbreviation of other tokens
	 */
	public boolean isAbbreviation(String string) {
		TermData td = this.getTermData(string);
		return ((td != null) && td.isAbbreviation());
	}
	
	/**
	 * Retrieve all abbreviations for a given token.
	 * @param term the token
	 * @return an array holding the abbreviations
	 */
	public String[] getAbbreviations(String term) {
		return this.getAbbreviations(term, true);
	}
	
	/**
	 * Retrieve all abbreviations for a given token.
	 * @param term the token
	 * @param transitive resolve transitively?
	 * @return an array holding the abbreviations
	 */
	public String[] getAbbreviations(String term, boolean transitive) {
		TermData td = this.getTermData(term);
		return ((td == null) ? null : td.getAbbreviations(transitive));
	}
	
	/**
	 * Retrieve the absolute number of times a given term is an abbreviation of
	 * a given other term. Note that this depends on the training data, i.e.,
	 * this method may return 0 even if the argument abbreviation technically
	 * can be an abbreviation of the argument full form.
	 * @param term the full form term to check the abbreviation against
	 * @param abbreviation the abbreviation to check
	 * @return
	 */
	public int getAbbreviationFrequency(String term, String abbreviation) {
		TermData td = this.getTermData(term);
		return ((td == null) ? 0 : td.getAbbreviationFrequency(this.getTermData(abbreviation)));
	}
	
	/**
	 * Retrieve the full form of an abbreviation. If the argument string is a
	 * full form in itself, it is returned. If the argument string can be an
	 * abbreviation for multiple different full forms, the most frequent full
	 * form is returned.
	 * @param abbreviation the abbreviation to get the full form for
	 * @return the (most frequent) full form of the argument string
	 */
	public String getFullForm(String abbreviation) {
		TermData td = this.getTermData(abbreviation);
		return ((td == null) ? null : td.getFullForm());
	}
	
	/**
	 * Retrieve the all possible full forms of an abbreviation. If the argument
	 * string is a full form in itself, it is the only element in the returned
	 * array. If the argument string can be an abbreviation for multiple
	 * different full forms, the returned array contains them all.
	 * @param abbreviation the abbreviation to get the full forms for
	 * @return an array holding the full forms of the argument string
	 */
	public String[] getFullForms(String abbreviation) {
		return this.getFullForms(abbreviation, true);
	}
	
	/**
	 * Retrieve the all possible full forms of an abbreviation. If the argument
	 * string is a full form in itself, it is the only element in the returned
	 * array. If the argument string can be an abbreviation for multiple
	 * different full forms, the returned array contains them all.
	 * @param abbreviation the abbreviation to get the full forms for
	 * @param transitive resolve transitively?
	 * @return an array holding the full forms of the argument string
	 */
	public String[] getFullForms(String abbreviation, boolean transitive) {
		TermData td = this.getTermData(abbreviation);
		return ((td == null) ? null : td.getFullForms(transitive, false));
	}
	
	/**
	 * Retrieve the all possible full forms of an abbreviation. If the argument
	 * string is a full form in itself, it is the only element in the returned
	 * array. If the argument string can be an abbreviation for multiple
	 * different full forms, the returned array contains them all. The returned
	 * array contains all possible extensions of the argument abbreviations,
	 * even ones that are themselves abbreviations of longer terms. This is
	 * equivalent to getting the transitively resolved full forms, but also
	 * holding on to intermediate ones.
	 * @param abbreviation the abbreviation to get the full forms for
	 * @return an array holding the full forms of the argument string
	 */
	public String[] getFullFormsAll(String abbreviation) {
		TermData td = this.getTermData(abbreviation);
		return ((td == null) ? null : td.getFullForms(true, true));
	}
	
	/**
	 * Retrieve the absolute frequency of a token.
	 * @param string the token whose frequency to check
	 * @return the frequency of the argument token
	 */
	public int getFrequency(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : td.count);
	}
	
	/**
	 * Retrieve the relative frequency of a token, relative to all tokens in
	 * the thesaurus.
	 * @param string the token whose frequency to check
	 * @return the normalized frequency of the argument token
	 */
	public float getFrequencyNorm(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : (((float) td.count) / this.totalCount));
	}
	
	/**
	 * Retrieve the smoothed relative frequency of a token, relative to all
	 * tokens in the thesaurus. If the argument token has a normalized
	 * frequency of 0, this method smoothes that to 1/2 occurrence in the set
	 * of all token occurrences in the thesaurus. This is helpful to prevent
	 * the product of the frequencies of a sequence of tokens from becoming
	 * flat out 0 for a single unknown token.
	 * @param string the token whose frequency to check
	 * @return the smoothed frequency of the argument token
	 */
	public float getFrequencySmooth(String string) {
		float fn = this.getFrequencyNorm(string);
		return ((fn == 0) ? (0.5f / this.totalCount) : fn);
	}
	
	/**
	 * Retrieve the absolute frequency a given token is an abbreviation of
	 * another given token.
	 * @param term the token to check
	 * @param fullForm the full form to check the token against
	 * @return the number of times the argument token abbreviates the argument
	 *            full form
	 */
	public int getFullFormFrequency(String term, String fullForm) {
		TermData td = this.getTermData(term);
		return ((td == null) ? 0 : td.getFullFormFrequency(this.getTermData(fullForm)));
	}
	
	void addStartOccurrence(String term) {
		TermData td = this.getTermData(term);
		if (td != null)
			td.startCount++;
	}
	void setStartFrequency(String term, int frequency) {
		TermData td = this.getTermData(term);
		this.totalStartCount += frequency;
		if (td != null)
			td.startCount = frequency;
	}
	
	/**
	 * Retrieve the absolute frequency of a token at the start of a journal
	 * name.
	 * @param string the token to check
	 * @return the number of times the argument token occurs at the start of a
	 *            journal name
	 */
	public int getStartFrequency(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : td.startCount);
	}
	
	/**
	 * Retrieve the normalized frequency of a token at the start of a journal
	 * name, i.e., the fraction of its overall occurrences that lie at the
	 * start of a journal name.
	 * @param string the token to check
	 * @return the normalized frequency of the argument token at the start of a
	 *            journal name
	 */
	public float getStartFrequencyNorm(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : (((float) td.startCount) / td.count));
	}
	
	/**
	 * Retrieve the smoothed frequency of a token at the start of a journal
	 * name, i.e., the fraction of its overall occurrences that lie at the
	 * start of a journal name. If the argument token has a normalized start
	 * frequency of 0, this method smoothes that to 1/2 occurrence in the set
	 * of all its occurrences in the thesaurus. This is helpful to prevent the
	 * product of the frequencies of a sequence of tokens from becoming flat
	 * out 0 for a single unknown token.
	 * @param string the token to check
	 * @return the smoothed frequency of the argument token at the start of a
	 *            journal name
	 */
	public float getStartFrequencySmooth(String string) {
		float fn = this.getStartFrequencyNorm(string);
		return ((fn == 0) ? (0.5f / this.totalCount) : fn);
	}
	
	void addEndOccurrence(String term) {
		TermData td = this.getTermData(term);
		if (td != null)
			td.endCount++;
	}
	void setEndFrequency(String term, int frequency) {
		TermData td = this.getTermData(term);
		if (td != null)
			td.endCount = frequency;
	}
	
	/**
	 * Retrieve the absolute frequency of a token at the end of a journal name.
	 * @param string the token to check
	 * @return the number of times the argument token occurs at the end of a
	 *            journal name
	 */
	public int getEndFrequency(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : td.endCount);
	}
	
	/**
	 * Retrieve the normalized frequency of a token at the end of a journal
	 * name, i.e., the fraction of its overall occurrences that lie at the end
	 * of a journal name.
	 * @param string the token to check
	 * @return the normalized frequency of the argument token at the end of a
	 *            journal end
	 */
	public float getEndFrequencyNorm(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : (((float) td.endCount) / td.count));
	}
	
	/**
	 * Retrieve the smoothed frequency of a token at the end of a journal name,
	 * i.e., the fraction of its overall occurrences that lie at the end of a
	 * journal name. If the argument token has a normalized end frequency of 0,
	 * this method smoothes that to 1/2 occurrence in the set of all its
	 * occurrences in the thesaurus. This is helpful to prevent the product of
	 * the frequencies of a sequence of tokens from becoming flat out 0 for a
	 * single unknown token.
	 * @param string the token to check
	 * @return the smoothed frequency of the argument token at the end of a
	 *            journal end
	 */
	public float getEndFrequencySmooth(String string) {
		float fn = this.getEndFrequencyNorm(string);
		return ((fn == 0) ? (0.5f / this.totalCount) : fn);
	}
	
	void addPairOccurrence(String term1, String term2, int frequency) {
//		TermData td = this.getTermData(term1);
//		if (td != null)
//			td.addSuccessor(this.getTermData(term2), frequency);
		TermData td1 = this.getTermData(term1);
		if (td1 == null)
			return;
		TermData td2 = this.getTermData(term2);
		if (td2 == null)
			return;
		td1.addSuccessor(td2, frequency);
		this.totalSuccessorCount += frequency;
	}
	
	/**
	 * Retrieve all tokens that can occur as a successor to a given token.
	 * @param string the token whose successors to retrieve
	 * @return an array holding the successors
	 */
	public String[] getSuccessors(String string) {
		TermData td = this.getTermData(string);
		return ((td == null)  ? null : td.getSuccessors());
	}
	
	/**
	 * Retrieve the number of distinct successors of a given token.
	 * @param string the token whose successors to count
	 * @return the number of successors of the argument token
	 */
	public int getSuccessorCount(String string) {
		TermData td = this.getTermData(string);
		return ((td == null)  ? null : td.getSuccessorCount());
	}
	
	/**
	 * Retrieve the absolute frequency of times two given tokens occur in
	 * immediate succession, i.e. one right after the other.
	 * @param string1 the first token to check
	 * @param string2 the second token to check
	 * @return the number of times the two argument tokens occur right after
	 *            one another
	 */
	public int getPairFrequency(String string1, String string2) {
		TermData td = this.getTermData(string1);
		return ((td == null)  ? 0 : td.getSuccessorCount(this.getTermData(string2)));
	}
	
	/**
	 * Retrieve the normalized frequency of two tokens occurring in immediate
	 * succession, i.e. one right after the other. This is the conditional
	 * frequency of the second token to occur given the occurrence of the first
	 * token. If the first token is null, this method returns the frequency of
	 * the second token given the (empty) start of a journal name. If the
	 * second token is null, this method returns the frequency with which a
	 * journal name ends, given the occurrence of the first token.
	 * @param string1 the first token to check
	 * @param string2 the second token to check
	 * @return the normalized pair frequency of the the argument tokens
	 */
	public float getPairFrequencyNorm(String string1, String string2) {
		TermData td1 = this.getTermData(string1);
		TermData td2 = this.getTermData(string2);
		if ((td1 == null) && (td2 == null))
			return 0;
		if (td1 == null)
			return ((string1 == null) ? (((float) td2.startCount) / this.totalStartCount) : 0);
		if (td2 == null)
			return ((string2 == null) ? (((float) td1.endCount) / td1.count) : 0);
		int ssc = td1.getSuccessorCount(td2);
		if (ssc == 0)
			return 0;
		return (((float) ssc) / td1.getSuccessorCount());
//		float nsc = (((float) ssc) / td1.getSuccessorCount());
//		float ntc = this.getFrequencyNorm(string2);
//		return (nsc / ntc);
	}
	
	/**
	 * Retrieve the smoothed frequency of two tokens occurring in immediate
	 * succession, i.e. one right after the other. This is the conditional
	 * frequency of the second token to occur given the occurrence of the first
	 * token. If the first token is null, this method returns the frequency of
	 * the second token given the (empty) start of a journal name. If the
	 * second token is null, this method returns the frequency with which a
	 * journal name ends, given the occurrence of the first token. If the
	 * argument tokens have a normalized pair frequency of 0, this method
	 * smoothes that to 1/2 occurrence in the set of all its occurrences in the
	 * thesaurus. This is helpful to prevent the product of the frequencies of
	 * a sequence of tokens from becoming flat out 0 for a single unknown
	 * token.
	 * @param string1 the first token to check
	 * @param string2 the second token to check
	 * @return the smoothed pair frequency of the the argument tokens
	 */
	public float getPairFrequencySmooth(String string1, String string2) {
		float fn = this.getPairFrequencyNorm(string1, string2);
		return ((fn == 0) ? (0.5f / this.totalCount) : fn);
	}
	
	void setInBracketFrequency(String term, int frequency) {
		TermData td = this.getTermData(term);
		if (td != null)
			td.inBracketCount = frequency;
	}
	
	/**
	 * Retrieve a dictionary of tokens that can occur in brackets. The
	 * returned dictionary contains all terms with at least one occurrence
	 * inside brackets.
	 * @return a dictionary containing all the terms that can occur in
	 *            brackets
	 */
	public Dictionary getInBracketPartDictionary() {
		return new MapDictionary(this.termData, true) {
			public boolean lookup(String string, boolean caseSensitive) {
				if ((string == null) || (string.length() < 1))
					return false;
				TermData td = getTermData(string);
				return ((td != null) && !td.isStopWordOnly() && td.isInBracketPart());
			}
			public StringIterator getEntryIterator() {
				return new UnWrappingStringIterator(super.getEntryIterator()) {
					boolean accept(TermData td) {
						return (!td.isStopWordOnly() && td.isInBracketPart());
					}
				};
			}
		};
	}
	
	/**
	 * Retrieve the absolute frequency of a token inside brackets.
	 * @param string the token to check
	 * @return the number of times the argument token occurs inside brackets
	 */
	public int getInBracketFrequency(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : td.inBracketCount);
	}
	
	/**
	 * Retrieve the normalized frequency of a token inside brackets, i.e., the
	 * fraction of its overall occurrences that lie inside brackets.
	 * @param string the token to check
	 * @return the normalized frequency of the argument token inside brackets
	 */
	public float getInBracketFrequencyNorm(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : (((float) td.inBracketCount) / td.count));
	}
	
	/**
	 * Retrieve the normalized frequency of a token inside brackets, i.e., the
	 * fraction of its overall occurrences that lie inside brackets. If the
	 * argument token has a normalized in-brackets frequency of 0, this method
	 * smoothes that to 1/2 occurrence in the set of all its occurrences in the
	 * thesaurus. This is helpful to prevent the product of the frequencies of
	 * a sequence of tokens from becoming flat out 0 for a single unknown
	 * token.
	 * @param string the token to check
	 * @return the smoothed frequency of the argument token inside brackets
	 */
	public float getInBracketFrequencySmooth(String string) {
		float fn = this.getInBracketFrequencyNorm(string);
		return ((fn == 0) ? (0.5f / this.totalCount) : fn);
	}
	
//	void addAfterCommaOccurrence(String term) {
//		TermData td = this.getTermData(term, false);
//		if (td != null)
//			td.afterCommaCount++;
//	}
	void setAfterCommaFrequency(String term, int frequency) {
		TermData td = this.getTermData(term);
		if (td != null)
			td.afterCommaCount = frequency;
	}
	
	/**
	 * Retrieve a dictionary of tokens that can occur after a comma. The
	 * returned dictionary contains all terms with at least one occurrence
	 * after a comma.
	 * @return a dictionary containing all the terms that can occur after a
	 *            comma
	 */
	public Dictionary getAfterCommaPartDictionary() {
		return new MapDictionary(this.termData, true) {
			public boolean lookup(String string, boolean caseSensitive) {
				if ((string == null) || (string.length() < 1))
					return false;
				TermData td = getTermData(string);
				return ((td != null) && !td.isStopWordOnly() && td.isAfterCommaPart());
			}
			public StringIterator getEntryIterator() {
				return new UnWrappingStringIterator(super.getEntryIterator()) {
					boolean accept(TermData td) {
						return (!td.isStopWordOnly() && td.isAfterCommaPart());
					}
				};
			}
		};
	}
	
	/**
	 * Retrieve the absolute frequency of a token after a comma.
	 * @param string the token to check
	 * @return the number of times the argument token occurs after a comma
	 */
	public int getAfterCommaFrequency(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : td.afterCommaCount);
	}
	
	/**
	 * Retrieve the normalized frequency of a token after a comma, i.e., the
	 * fraction of its overall occurrences that lie after the (first) comma in
	 * a journal name.
	 * @param string the token to check
	 * @return the normalized frequency of the argument token after a comma
	 */
	public float getAfterCommaFrequencyNorm(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : (((float) td.afterCommaCount) / td.count));
	}
	
	/**
	 * Retrieve the normalized frequency of a token after a comma, i.e., the
	 * fraction of its overall occurrences that lie after the (first) comma in
	 * a journal name. If the argument token has a normalized after-comma
	 * frequency of 0, this method smoothes that to 1/2 occurrence in the set
	 * of all its occurrences in the thesaurus. This is helpful to prevent the
	 * product of the frequencies of a sequence of tokens from becoming flat
	 * out 0 for a single unknown token.
	 * @param string the token to check
	 * @return the smoothed frequency of the argument token after a comma
	 */
	public float getAfterCommaFrequencySmooth(String string) {
		float fn = this.getAfterCommaFrequencyNorm(string);
		return ((fn == 0) ? (0.5f / this.totalCount) : fn);
	}
	
	/**
	 * Retrieve a dictionary of stop words. The returned dictionary contains
	 * all terms whose majority of occurrences is in the stop word role.
	 * @return a dictionary containing all the terms that (mainly) occur as
	 *            stop words
	 */
	public Dictionary getStopWordDictionary() {
		return new MapDictionary(this.termData, true) {
			public boolean lookup(String string, boolean caseSensitive) {
				if ((string == null) || (string.length() < 1))
					return false;
				TermData td = getTermData(string);
				return ((td != null) && td.isStopWord());
			}
			public StringIterator getEntryIterator() {
				return new UnWrappingStringIterator(super.getEntryIterator()) {
					boolean accept(TermData td) {
						return td.isStopWord();
					}
				};
			}
		};
	}
	
	/**
	 * Test whether or not a given token is a stop word in the majority of its
	 * occurrences.
	 * @param string the token to check.
	 * @return true if the argument token is mainly a stop word
	 */
	public boolean isStopWord(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? false : td.isStopWord());
	}
	
	/**
	 * Retrieve the absolute frequency of a token as a stop word.
	 * @param string the token to check
	 * @return the number of times the argument token occurs as a stop word
	 */
	public int getStopWordFrequency(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : td.stopCount);
	}
	
	/**
	 * Retrieve the normalized frequency of a token as a stop word, i.e., its
	 * frequency as a stop word in comparison to all stop words.
	 * @param string the token to check
	 * @return the normalized frequency of the argument token as a stop word
	 */
	public float getStopWordFrequencyNorm(String string) {
		TermData td = this.getTermData(string);
		return ((td == null) ? 0 : (((float) td.stopCount) / this.totalStopCount));
	}
	
	/**
	 * Retrieve the smoothed frequency of a token as a stop word, i.e., its
	 * frequency as a stop word in comparison to all stop words. If the
	 * argument token has a normalized stop word frequency of 0, this method
	 * smoothes that to 1/2 occurrence in the set of all its occurrences in the
	 * thesaurus. This is helpful to prevent the product of the frequencies of
	 * a sequence of tokens from becoming flat out 0 for a single unknown
	 * token.
	 * @param string the token to check
	 * @return the smoothed frequency of the argument token as a stop word
	 */
	public float getStopWordFrequencySmooth(String string) {
		float fn = this.getStopWordFrequencyNorm(string);
		return ((fn == 0) ? (0.5f / this.totalCount) : fn);
	}
	
	private abstract class UnWrappingStringIterator implements StringIterator {
		private StringIterator si;
		private String next;
		UnWrappingStringIterator(StringIterator si) {
			this.si = si;
			this.hasMoreStrings();
		}
		public boolean hasNext() {
			return this.hasMoreStrings();
		}
		public Object next() {
			return this.nextString();
		}
		public void remove() {
			this.si.remove();
		}
		public boolean hasMoreStrings() {
			if (this.next != null)
				return true;
			while (this.si.hasMoreStrings()) {
				TermData td = getTermData(this.si.nextString());
				if (this.accept(td)) {
					this.next = td.term;
					return true;
				}
			}
			return false;
		}
		public String nextString() {
			if (this.hasMoreStrings()) {
				String next = this.next;
				this.next = null;
				return next;
			}
			else return null;
		}
		abstract boolean accept(TermData td);
	}
	
	/**
	 * Write the contents of the journal thesaurus to a Writer. This method
	 * outputs a tab separated (TSV) representation of the data, with column
	 * names in the first row.
	 * @param out the writer to write to
	 * @throws IOException
	 */
	public void writeData(Writer out) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		
		//	write keys
		bw.write("term");
		bw.write("\t");
		bw.write("frequency");
		bw.write("\t");
		bw.write("stopFrequency");
		bw.write("\t");
		bw.write("startFrequency");
		bw.write("\t");
		bw.write("endFrequency");
		bw.write("\t");
		bw.write("inBracketFrequency");
		bw.write("\t");
		bw.write("afterCommaFrequency");
		bw.write("\t");
		bw.write("abbreviations");
		bw.write("\t");
		bw.write("fullForms");
		bw.write("\t");
		bw.write("successors");
		bw.newLine();
		
		//	write terms and their data
		ArrayList terms = new ArrayList();
		for (StringIterator tit = this.getTermIterator(); tit.hasMoreStrings();)
			terms.add(tit.nextString());
		Collections.sort(terms, String.CASE_INSENSITIVE_ORDER);
		for (int p = 0; p < terms.size(); p++) {
			String mp = ((String) terms.get(p));
			bw.write(mp);
			bw.write("\t");
			bw.write("" + this.getFrequency(mp));
			bw.write("\t");
			bw.write("" + this.getStopWordFrequency(mp));
			bw.write("\t");
			bw.write("" + this.getStartFrequency(mp));
			bw.write("\t");
			bw.write("" + this.getEndFrequency(mp));
			bw.write("\t");
			bw.write("" + this.getInBracketFrequency(mp));
			bw.write("\t");
			bw.write("" + this.getAfterCommaFrequency(mp));
			bw.write("\t");
			String[] abbrevs = this.getAbbreviations(mp, false);
			if (abbrevs != null)
				for (int a = 0; a < abbrevs.length; a++) {
					if (a != 0)
						bw.write(" ");
					bw.write(abbrevs[a] + ":" + this.getAbbreviationFrequency(mp, abbrevs[a]));
				}
			bw.write("\t");
			String[] fullForms = this.getFullForms(mp, false);
			if ((fullForms != null) && !mp.equals(fullForms[0]))
				for (int f = 0; f < fullForms.length; f++) {
					if (f != 0)
						bw.write(" ");
					bw.write(fullForms[f] + ":" + this.getFullFormFrequency(mp, fullForms[f]));
				}
			bw.write("\t");
			String[] successors = this.getSuccessors(mp);
			if (successors != null)
				for (int s = 0; s < successors.length; s++) {
					if (s != 0)
						bw.write(" ");
					bw.write(successors[s] + ":" + this.getPairFrequency(mp, successors[s]));
				}
			bw.newLine();
		}
		
		//	finally ...
		if (bw != out)
			bw.flush();
	}
	
	/**
	 * Create an instance of this class and populate it with the data provided
	 * by a Reader. This method reads the argument Reader to its end; it
	 * expects the data to be the same tab separated representation output
	 * by the <code>writeData()</code> method of instances of this class. If
	 * the data is in CSV format instead, the fields must be enclosed in double
	 * quotes.
	 * @param in the reader to read from
	 * @return the loaded journal name thesaurus
	 * @throws IOException
	 */
	public static JournalNameThesaurus loadJournalNameThesaurus(Reader in) throws IOException {
		JournalNameThesaurus thesaurus = new JournalNameThesaurus();
		fillJournalNameThesaurus(in, thesaurus);
		return thesaurus;
	}
	
	/**
	 * Populate an instance of this class with the data provided by a Reader.
	 * This method reads the argument Reader to its end; it expects the data to
	 * be the same tab separated representation output by the
	 * <code>writeData()</code> method of instances of this class. If the data
	 * is in CSV format instead, the fields must be enclosed in double quotes.
	 * @param in the reader to read from
	 * @param thesaurus the journal name thesaurus to fill
	 * @throws IOException
	 */
	public static void fillJournalNameThesaurus(Reader in, JournalNameThesaurus thesaurus) throws IOException {
		BufferedReader br = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
		StringRelation data = StringRelation.readCsvData(br, StringRelation.GUESS_SEPARATOR, '"');
//		StringVector keys = data.getKeys();
//		System.out.println("\"" + keys.concatStrings("\";\"") + "\"");
		
		//	load terms and positional information
		for (int d = 0; d < data.size(); d++) {
			StringTupel st = data.get(d);
//			System.out.println(st.toCsvString(';', '"', keys));
			String term = st.getValue("term");
			int frequency = Integer.parseInt(st.getValue("frequency"));
			int stopFrequency = Integer.parseInt(st.getValue("stopFrequency"));
			if (stopFrequency < frequency)
				thesaurus.addTerm(term, false, (frequency - stopFrequency));
			if (stopFrequency != 0)
				thesaurus.addTerm(term, true, stopFrequency);
			thesaurus.setStartFrequency(term, Integer.parseInt(st.getValue("startFrequency")));
			thesaurus.setEndFrequency(term, Integer.parseInt(st.getValue("endFrequency")));
			thesaurus.setInBracketFrequency(term, Integer.parseInt(st.getValue("inBracketFrequency")));
			thesaurus.setAfterCommaFrequency(term, Integer.parseInt(st.getValue("afterCommaFrequency")));
		}
		
		//	add term relationships (now that all terms are loaded)
		for (int d = 0; d < data.size(); d++) {
			StringTupel st = data.get(d);
//			System.out.println(st.toCsvString(';', '"', keys));
			String term = st.getValue("term");
			String abbreviationDataStr = st.getValue("abbreviations", "");
			if (abbreviationDataStr.length() != 0) {
				String[] abbreviationDatas = abbreviationDataStr.split("\\s+");
				for (int a = 0; a < abbreviationDatas.length; a++) {
					String abbreviation = abbreviationDatas[a].substring(0, abbreviationDatas[a].indexOf(':'));
					int frequency = Integer.parseInt(abbreviationDatas[a].substring(abbreviationDatas[a].indexOf(':') + ":".length()));
					thesaurus.addAbbreviation(abbreviation, term, frequency);
				}
			}
			//	no need to add full forms explicitly ... implicitly added with abbreviations in reverse direction
			String successorDataStr = st.getValue("successors", "");
			if (successorDataStr.length() != 0) {
				String[] successorDatas = successorDataStr.split("\\s+");
				for (int s = 0; s < successorDatas.length; s++) {
					String successor = successorDatas[s].substring(0, successorDatas[s].indexOf(':'));
					int frequency = Integer.parseInt(successorDatas[s].substring(successorDatas[s].indexOf(':') + ":".length()));
					thesaurus.addPairOccurrence(term, successor, frequency);
				}
			}
		}
	}
}

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
package de.uka.ipd.idaho.plugins.bibRefs.reFinder.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class JournalThesaurusBuilder {
	
	private static class JournalName implements Comparable {
		final String raw;
		final String normalized;
		final String lower;
		final String alpha;
		final String lowerAlpha;
		final String nonParenAlpha;
		final String lowerNonParenAlpha;
		final String[] acronymTokens;
		final String acronym;
		final String[] nonParenAcronymTokens;
		final String nonParenAcronym;
		JournalName(String raw) {
			this.raw = raw.replaceAll("\\.", ". ").replaceAll("\\,", ", ").replaceAll("\\s+", " ").replaceAll("\\s\\)", ")").replaceAll("\\s\\]", "]").replaceAll("\\s\\}", "}").trim();
			this.normalized = StringUtils.normalizeString(this.raw).replaceAll("\\s+", " ").trim();
			this.lower = this.normalized.toLowerCase();
			this.alpha = this.normalized.replaceAll("[^A-Za-z\\s]", " ").replaceAll("\\s+", " ").trim();
			this.lowerAlpha = this.alpha.toLowerCase();
			this.nonParenAlpha = this.normalized.replaceAll("\\([^\\)]+\\)", "").replaceAll("\\[[^\\]]+\\]", "").replaceAll("\\{[^\\}]+\\}", "").replaceAll("[^A-Za-z\\s]", " ").replaceAll("\\s+", " ").trim();
			this.lowerNonParenAlpha = this.nonParenAlpha.toLowerCase();
			this.acronymTokens = getAcronymTokens(this.alpha);
			this.acronym = buildAcronym(this.acronymTokens);
			this.nonParenAcronymTokens = getAcronymTokens(this.nonParenAlpha);
			this.nonParenAcronym = buildAcronym(this.nonParenAcronymTokens);
		}
		private static final Pattern acronymTokenPattern = Pattern.compile("[A-Z][a-zA-Z]+");
		private static String[] getAcronymTokens(String str) {
			StringVector acronymTokens = new StringVector();
			for (Matcher m = acronymTokenPattern.matcher(str); m.find();)
				acronymTokens.addElement(m.group());
			return acronymTokens.toStringArray();
		}
		private static String buildAcronym(String[] ats) {
			StringBuffer acronym = new StringBuffer();
			for (int t = 0; t < ats.length; t++)
				acronym.append(ats[t].charAt(0));
			return acronym.toString();
		}
//		private static String buildAcronym(String str) {
//			StringBuffer acronym = new StringBuffer();
//			boolean lastWasLetter = false;
//			for (int c = 0; c < str.length(); c++) {
//				char ch = str.charAt(c);
//				if (Character.isLetter(ch)) {
//					if (!lastWasLetter && Character.isUpperCase(ch))
//						acronym.append(ch);
//					lastWasLetter = true;
//				}
//				else lastWasLetter = false;
//			}
//			return acronym.toString();
//		}
		public String toString() {
			return this.raw;
		}
		public int compareTo(Object obj) {
			return this.raw.compareTo(((JournalName) obj).raw);
		}
	}
	
	private static class IndexMap extends LinkedHashMap {
		public Object put(Object key, Object value) {
			ArrayList values = ((ArrayList) this.get(key));
			if (values == null) {
				values = new ArrayList(3);
				super.put(key, values);
			}
			values.add(value);
			return values.get(0);
		}
		void putAll(Object key, ArrayList keyValues) {
			ArrayList values = ((ArrayList) this.get(key));
			if (values == null) {
				values = new ArrayList(3);
				super.put(key, values);
			}
			values.addAll(keyValues);
		}
		ArrayList getAll(Object key) {
			return ((ArrayList) this.get(key));
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
//		File source = new File("E:/Temp/journalNames/PlaziJournalNames.articles.txt");
		File source = new File("E:/Temp/journalNames/PlaziJournalNames.bibRefs.3.txt");
//		File source = new File("E:/Temp/journalNames/PlaziJournalNames.bibRefs.2.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(source)), "UTF-8"));
		IndexMap rawIndex = new IndexMap();
		IndexMap normIndex = new IndexMap();
		IndexMap lowerIndex = new IndexMap();
		IndexMap alphaIndex = new IndexMap();
		IndexMap lowerAlphaIndex = new IndexMap();
		IndexMap nonParenAlphaIndex = new IndexMap();
		IndexMap lowerNonParenAlphaIndex = new IndexMap();
//		IndexMap acronymIndex = new IndexMap();
//		IndexMap nonParenAcronymIndex = new IndexMap();
		for (String jnStr; (jnStr = br.readLine()) != null;) {
			String[] jnData = jnStr.split("\\t");
			if (jnData.length < 4)
				continue;
			JournalName jn = new JournalName(jnData[3]);
			rawIndex.put(jn.raw, jn);
			normIndex.put(jn.normalized, jn);
			lowerIndex.put(jn.lower, jn);
			alphaIndex.put(jn.alpha, jn);
			lowerAlphaIndex.put(jn.lowerAlpha, jn);
			nonParenAlphaIndex.put(jn.nonParenAlpha, jn);
			lowerNonParenAlphaIndex.put(jn.lowerNonParenAlpha, jn);
//			acronymIndex.put(jn.acronym, jn);
//			nonParenAcronymIndex.put(jn.nonParenAcronym, jn);
		}
		br.close();
		System.out.println("Got " + rawIndex.size() + " raw joural names");
		showClusters(rawIndex);
		System.out.println("Got " + normIndex.size() + " normalized joural names");
		showClusters(normIndex);
		System.out.println("Got " + lowerIndex.size() + " case insensitive joural names");
		showClusters(lowerIndex);
		System.out.println("Got " + alphaIndex.size() + " joural names by letters only");
		showClusters(alphaIndex);
		System.out.println("Got " + lowerAlphaIndex.size() + " case insensitive joural names by letters only");
		showClusters(lowerAlphaIndex);
		System.out.println("Got " + nonParenAlphaIndex.size() + " joural names by letters only without parts in parentheses");
		showClusters(nonParenAlphaIndex);
		System.out.println("Got " + lowerNonParenAlphaIndex.size() + " case insensitive joural names by letters only without parts in parentheses");
		showClusters(lowerNonParenAlphaIndex);
		
		//	TODOne assign case insensitive alpha clusters to longest acronym in cluster (helps abstract from capitalization issues)
		ArrayList lowerNonParenAlphaKeys = new ArrayList(lowerNonParenAlphaIndex.keySet());
		Collections.sort(lowerNonParenAlphaKeys);
		IndexMap acronymIndex = new IndexMap();
		IndexMap nonParenAcronymIndex = new IndexMap();
		for (int k = 0; k < lowerNonParenAlphaKeys.size(); k++) {
			String key = ((String) lowerNonParenAlphaKeys.get(k));
			ArrayList values = lowerNonParenAlphaIndex.getAll(key);
			if (values.size() < 2) {
				JournalName jn = ((JournalName) values.get(0));
				acronymIndex.put(jn.acronym, jn);
				nonParenAcronymIndex.put(jn.nonParenAcronym, jn);
				continue;
			}
			String maxAcronym = "";
			CountingSet acronyms = new CountingSet(new TreeMap());
			String maxNonParenAcronym = "";
			CountingSet nonParenAcronyms = new CountingSet(new TreeMap());
			for (int v = 0; v < values.size(); v++) {
				JournalName jn = ((JournalName) values.get(v));
				if (jn.acronym.length() > maxAcronym.length())
					maxAcronym = jn.acronym;
				acronyms.add(jn.acronym);
				if (jn.nonParenAcronym.length() > maxNonParenAcronym.length())
					maxNonParenAcronym = jn.nonParenAcronym;
				nonParenAcronyms.add(jn.nonParenAcronym);
			}
			System.out.println(" - got " + values.size() + " raw joural names conflated under " + key);
			System.out.println("   maximum acronyms are " + maxAcronym + " and " + maxNonParenAcronym);
			acronymIndex.putAll(maxAcronym, values);
			nonParenAcronymIndex.putAll(maxNonParenAcronym, values);
			maxAcronym = ((String) acronyms.max(true));
			maxNonParenAcronym = ((String) nonParenAcronyms.max(true));
			System.out.println("   most frequent acronyms are " + maxAcronym + " and " + maxNonParenAcronym);
//			acronymIndex.putAll(maxAcronym, values);
//			nonParenAcronymIndex.putAll(maxNonParenAcronym, values);
		}
		System.out.println("Got " + acronymIndex.size() + " joural names by acronym");
		showClusters(acronymIndex);
		System.out.println("Got " + nonParenAcronymIndex.size() + " joural names by acronym without parts in parentheses");
		showClusters(nonParenAcronymIndex);
		
		//	TODOne align journal names inside clusters to find common abbreviations
		ArrayList nonParenAcronymKeys = new ArrayList(nonParenAcronymIndex.keySet());
		Collections.sort(nonParenAcronymKeys);
		IndexMap jnPartAbbrevs = new IndexMap();
		for (int k = 0; k < nonParenAcronymKeys.size(); k++) {
			String key = ((String) nonParenAcronymKeys.get(k));
			if (key.length() < 2)
				continue;
			ArrayList values = nonParenAcronymIndex.getAll(key);
			if (values.size() < 2)
				continue;
			HashSet abbrevPairs = new HashSet();
			for (int v = 0; v < values.size(); v++) {
				JournalName jn = ((JournalName) values.get(v));
				String jnNs = jn.nonParenAlpha.replaceAll("\\s", "");
				String capJn = jn.nonParenAlpha.replaceAll("\\s[a-z]+\\s", " ").replaceAll("\\s+", " ");
				String capJnParts[] = capJn.split("\\s+");
				adjustCase(capJnParts);
				for (int cv = (v+1); cv < values.size(); cv++) {
					JournalName cjn = ((JournalName) values.get(cv));
					if (jn.nonParenAlpha.equalsIgnoreCase(cjn.nonParenAlpha))
						continue;
					String cjnNs = cjn.nonParenAlpha.replaceAll("\\s", "");
					if (jnNs.equalsIgnoreCase(cjnNs))
						continue;
					String cCapJn = cjn.nonParenAlpha.replaceAll("\\s[a-z]+\\s", " ").replaceAll("\\s+", " ");
					if (capJn.equalsIgnoreCase(cCapJn))
						continue;
					String cCapJnParts[] = cCapJn.split("\\s+");
					if (cCapJnParts.length != capJnParts.length)
						continue;
					adjustCase(cCapJnParts);
					boolean jnIsAbbrev = true;
					boolean cjnIsAbbrev = true;
					for (int p = 0; p < capJnParts.length; p++) {
						if (capJnParts[p].equalsIgnoreCase(cCapJnParts[p])) {}
						else if (capJnParts[p].toLowerCase().startsWith(cCapJnParts[p].toLowerCase())) {
							jnIsAbbrev = false;
							if (!cjnIsAbbrev)
								break;
						}
						else if (cCapJnParts[p].toLowerCase().startsWith(capJnParts[p].toLowerCase())) {
							cjnIsAbbrev = false;
							if (!jnIsAbbrev)
								break;
						}
						else {
							jnIsAbbrev = false;
							cjnIsAbbrev = false;
							break;
						}
					}
					if (cjnIsAbbrev) {
						if (!abbrevPairs.add(cjn.nonParenAlpha + " ==> " + jn.nonParenAlpha))
							continue;
						System.out.println("Found abbreviation " + cjn.nonParenAlpha + " for " + jn.nonParenAlpha);
						for (int p = 0; p < capJnParts.length; p++) {
							if ((capJnParts[p].length() - cCapJnParts[p].length()) > 1)
								jnPartAbbrevs.put(capJnParts[p], cCapJnParts[p]);
						}
					}
					else if (jnIsAbbrev) {
						if (!abbrevPairs.add(jn.nonParenAlpha + " ==> " + cjn.nonParenAlpha))
							continue;
						System.out.println("Found abbreviation " + jn.nonParenAlpha + " for " + cjn.nonParenAlpha);
						for (int p = 0; p < capJnParts.length; p++) {
							if ((cCapJnParts[p].length() - capJnParts[p].length()) > 1)
								jnPartAbbrevs.put(cCapJnParts[p], capJnParts[p]);
						}
					}
				}
			}
		}
		
		//	TODOne what do we have?
		ArrayList jnPartKeys = new ArrayList(jnPartAbbrevs.keySet());
		Collections.sort(jnPartKeys);
		IndexMap jnPartAbbrevParts = new IndexMap();
		for (int k = 0; k < jnPartKeys.size(); k++) {
			String key = ((String) jnPartKeys.get(k));
			ArrayList values = jnPartAbbrevs.getAll(key);
			CountingSet distValues = new CountingSet(new TreeMap());
			distValues.addAll(values);
			System.out.println("Got " + distValues.elementCount() + " abbreviations for " + key);
			for (Iterator dvit = distValues.iterator(); dvit.hasNext();) {
				String distValue = ((String) dvit.next());
				System.out.println(" - " + distValue + ": " + distValues.getCount(distValue) + " times");
				jnPartAbbrevParts.put(distValue, key);
			}
		}
		
		//	augment part-to-abbreviation mappings transitively
		ArrayList jnPartAbbrevKeys = new ArrayList(jnPartAbbrevParts.keySet());
		Collections.sort(jnPartAbbrevKeys);
		for (int k = 0; k < jnPartAbbrevKeys.size(); k++) {
			String key = ((String) jnPartAbbrevKeys.get(k));
			ArrayList values = jnPartAbbrevParts.getAll(key);
			System.out.println("Got " + values.size() + " meanings for abbreviation " + key);
			System.out.println("  " + values);
			if (key.length() < 2)
				continue; // too ambiguous
			//	TODOne add full forms of meanings that are abbreviations themselves
			for (int v = 0; v < values.size(); v++) {
				String value = ((String) values.get(v));
				if (jnPartAbbrevParts.containsKey(value)) {
					ArrayList transValues = jnPartAbbrevParts.getAll(value);
					for (int t = 0; t < transValues.size(); t++) {
						String transValue = ((String) transValues.get(t));
						if (values.contains(transValue))
							continue;
						values.add(transValue);
						jnPartAbbrevs.put(transValue, key);
					}
				}
				ArrayList valueAbbrevs = jnPartAbbrevs.getAll(value);
				for (int a = 0; a < valueAbbrevs.size(); a++) {
					String valueAbbrev = ((String) valueAbbrevs.get(a));
					if (key.equals(valueAbbrev))
						continue;
					if (valueAbbrev.length() < 2)
						continue; // too ambiguous
					ArrayList transValues = jnPartAbbrevParts.getAll(valueAbbrev);
					for (int t = 0; t < transValues.size(); t++) {
						String transValue = ((String) transValues.get(t));
						if (values.contains(transValue))
							continue;
						if (key.equalsIgnoreCase(transValue))
							continue;
						if (StringUtils.isAbbreviationOf(transValue, key, false)) {
							values.add(transValue);
							jnPartAbbrevs.put(transValue, key);
						}
					}
				}
			}
			System.out.println("  ==> " + values);
		}
		
		//	TODOne what do we have now?
		jnPartKeys = new ArrayList(jnPartAbbrevs.keySet());
		Collections.sort(jnPartKeys);
		for (int k = 0; k < jnPartKeys.size(); k++) {
			String key = ((String) jnPartKeys.get(k));
			ArrayList values = jnPartAbbrevs.getAll(key);
			CountingSet distValues = new CountingSet(new TreeMap());
			distValues.addAll(values);
			System.out.println("Got " + distValues.elementCount() + " abbreviations for " + key);
			for (Iterator dvit = distValues.iterator(); dvit.hasNext();) {
				String distValue = ((String) dvit.next());
				System.out.println(" - " + distValue + ": " + distValues.getCount(distValue) + " times");
			}
		}
		
		//	TODO for multi-part journal names, assess which parts can occur where
		ArrayList rawKeys = new ArrayList(rawIndex.keySet());
		Collections.sort(rawKeys);
		HashMap tokenPosIndex = new HashMap();
		CountingSet tokenMultigrams = new CountingSet(new TreeMap());
		for (int k = 0; k < rawKeys.size(); k++) {
			String key = ((String) rawKeys.get(k));
			ArrayList values = rawIndex.getAll(key);
			for (int v = 0; v < values.size(); v++) {
				JournalName jn = ((JournalName) values.get(v));
				if (jn.nonParenAcronymTokens.length < 2)
					continue;
				adjustCase(jn.nonParenAcronymTokens);
				for (int t = 0; t < jn.nonParenAcronymTokens.length; t++) {
					if (!jnPartAbbrevs.containsKey(jn.nonParenAcronymTokens[t]) && !jnPartAbbrevParts.containsKey(jn.nonParenAcronymTokens[t]))
						continue;
					CountingSet tokenPos = ((CountingSet) tokenPosIndex.get(jn.nonParenAcronymTokens[t]));
					if (tokenPos == null) {
						tokenPos = new CountingSet(new TreeMap());
						tokenPosIndex.put(jn.nonParenAcronymTokens[t], tokenPos);
					}
					tokenPos.add(new Integer(t));
					tokenPos.add(new Integer(t - jn.nonParenAcronymTokens.length));
				}
				for (int p = 0; p < tokenMultigramPatterns.length; p++) {
					Matcher m = tokenMultigramPatterns[p].matcher(jn.normalized);
//					System.out.println("Getting multi-grams from " + jn.normalized);
					for (int s = 0; m.find(s);) {
						tokenMultigrams.add(adjustTokenCase(m.group()));
//						System.out.println(" - " + m.group());
						s = (m.start() + m.group().indexOf(' '));
					}
				}
			}
		}
		ArrayList tokenKeys = new ArrayList(tokenPosIndex.keySet());
		Collections.sort(tokenKeys);
		for (int k = 0; k < tokenKeys.size(); k++) {
			String key = ((String) tokenKeys.get(k));
			CountingSet tokenPos = ((CountingSet) tokenPosIndex.get(key));
			System.out.println("Positions for '" + key + "': " + tokenPos);
		}
		
		//	TODOne filter prefixes and suffixes of equally frequent larger multi-grams
		ArrayList tokenMultigramKeys = new ArrayList(tokenMultigrams);
		Collections.sort(tokenMultigramKeys);
		HashSet filterTokenMultigrams = new HashSet();
		for (int m = 0; m < tokenMultigramKeys.size(); m++) {
			String tm = ((String) tokenMultigramKeys.get(m));
			if (tm.indexOf(' ') == tm.lastIndexOf(' '))
				continue;
			String tmp = tm.substring(0, tm.lastIndexOf(' '));
			if (tokenMultigrams.getCount(tmp) <= tokenMultigrams.getCount(tm)) {
				System.out.println("Filtering prefix '" + tmp + "' of equally frequent multi-gram '" + tm + "'");
				filterTokenMultigrams.add(tmp);
			}
			String tms = tm.substring(tm.indexOf(' ') + " ".length());
			if (tokenMultigrams.getCount(tms) <= tokenMultigrams.getCount(tm)) {
				System.out.println("Filtering suffix '" + tms + "' of equally frequent multi-gram '" + tm + "'");
				filterTokenMultigrams.add(tms);
			}
		}
//		int maxMultigramTokens = 0;
//		String maxTokenMultigram = "";
		for (Iterator mgit = tokenMultigrams.iterator(); mgit.hasNext();) {
			String tm = ((String) mgit.next());
			if (tokenMultigrams.getCount(tm) < 2)
				continue;
			if (filterTokenMultigrams.contains(tm))
				continue;
			System.out.println("Multi-gram '" + tm + "': " + tokenMultigrams.getCount(tm) + " times");
//			String[] tmts = tm.split("\\s+");
//			if (tmts.length > maxMultigramTokens) {
//				maxMultigramTokens = tmts.length;
//				maxTokenMultigram = tm;
//			}
		}
//		System.out.println("Maximum multi-gram is '" + maxTokenMultigram + "'");
	}
	
	private static final Pattern[] tokenMultigramPatterns = {
		Pattern.compile("[A-Z][a-zA-Z]+(\\s[A-Z][a-zA-Z]+){1}"),
		Pattern.compile("[A-Z][a-zA-Z]+(\\s[A-Z][a-zA-Z]+){2}"),
		Pattern.compile("[A-Z][a-zA-Z]+(\\s[A-Z][a-zA-Z]+){3}"),
		Pattern.compile("[A-Z][a-zA-Z]+(\\s[A-Z][a-zA-Z]+){4}"),
		Pattern.compile("[A-Z][a-zA-Z]+(\\s[A-Z][a-zA-Z]+){5}"),
		Pattern.compile("[A-Z][a-zA-Z]+(\\s[A-Z][a-zA-Z]+){6}"),
		Pattern.compile("[A-Z][a-zA-Z]+(\\s[A-Z][a-zA-Z]+){7}"),
		Pattern.compile("[A-Z][a-zA-Z]+(\\s[A-Z][a-zA-Z]+){8}"),
		Pattern.compile("[A-Z][a-zA-Z]+(\\s[A-Z][a-zA-Z]+){9}"),
	};
	
	private static void adjustCase(String[] strs) {
		for (int s = 0; s < strs.length; s++)
			strs[s] = (strs[s].substring(0, 1).toUpperCase() + strs[s].substring(1).toLowerCase());
	}
	
	private static String adjustTokenCase(String str) {
		String[] strs = str.split("\\s+");
		adjustCase(strs);
		StringBuffer adjusted = new StringBuffer(strs[0]);
		for (int s = 1; s < strs.length; s++) {
			adjusted.append(" ");
			adjusted.append(strs[s]);
		}
		return adjusted.toString();
	}
	
	private static void showClusters(IndexMap im) {
		ArrayList keys = new ArrayList(im.keySet());
		Collections.sort(keys);
		int clusterCount = 0;
		for (int k = 0; k < keys.size(); k++) {
			String key = ((String) keys.get(k));
			ArrayList values = im.getAll(key);
			if (values.size() < 2)
				continue;
			Collections.sort(values);
			System.out.println(" - got " + values.size() + " raw joural names conflated under " + key);
			for (int v = 0; v < values.size(); v++)
				System.out.println("   - " + values.get(v));
			clusterCount++;
		}
		System.out.println(" ==> " + clusterCount + " clusters");
	}
}

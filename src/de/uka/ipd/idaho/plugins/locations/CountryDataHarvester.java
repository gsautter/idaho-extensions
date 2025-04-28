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
package de.uka.ipd.idaho.plugins.locations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.easyIO.streams.CharSequenceReader;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * This class harvests data on countries in three steps:<ul>
 * <li>Obtain a list of country names from CIA World Factbook</li>
 * <li>Obtain local and former names for each country from CIA World Factbook</li>
 * <li>Obtain names in other languages for each country from English Wikipedia</li>
 * </ul>
 * All you have to possible edit in the code below is the working directory.
 * 
 * @author sautter
 */
public class CountryDataHarvester {
	private static Grammar xml = new StandardGrammar();
	private static Parser xmlParser = new Parser(xml);
	private static Grammar html = new Html();
	private static Parser htmlParser = new Parser(html);
	
	private static File dataPath = new File("E:/Projektdaten/CountryData/");
	private static boolean downloadRun = false;
	public static void main(String[] args) throws Exception {
		dataPath.mkdirs();
		
		//	this is the master list for translations
		final TreeSet isoCountries = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		TreeMap countries = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
			public Object put(Object key, Object value) {
				if (isoCountries.contains(key) && !key.equals(value)) {
					System.out.println("GOTCHA: wrongful mapping of ISO country " + key + " to " + value);
					return value;
				}
				else return super.put(key, value);
			}
		};
		TreeMap countryLangs = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
			public Object get(Object key) {
				Object value = super.get(key);
				if (value == null) {
					value = new TreeSet(String.CASE_INSENSITIVE_ORDER);
					this.put(key, value);
				}
				return value;
			}
		};
		TreeMap countriesToIso = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		TreeMap isoToCountries = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		TreeMap iso3toIso2 = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		
		//	get ISO 3166 data from Wikipedia
		TreeMap countryIsoData = getCountryIsoData(false);
		isoCountries.addAll(countryIsoData.keySet());
		for (Iterator cnit = countryIsoData.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next());
			TreeMap cData = ((TreeMap) countryIsoData.get(cName));
			String cAlpha2 = ((String) cData.get("alpha2"));
			String cAlpha3 = ((String) cData.get("alpha3"));
			countries.put(cName, cName);
			((TreeSet) countryLangs.get(cName)).add("ISO official");
			countriesToIso.put(cName, cAlpha3);
			countriesToIso.put(normalizeKey(cName), cAlpha3);
			iso3toIso2.put(cAlpha3, cAlpha2);
			System.out.println("ISO: " + cName + " (" + cAlpha3 + ")");
			if (cName.startsWith("Saint ") || cName.startsWith("Sint ") || cName.startsWith("Sankt ") || cName.startsWith("Sant ")) {
				String acName = ("St." + cName.substring(cName.indexOf(' ')));
				countries.put(acName, cName);
				((TreeSet) countryLangs.get(acName)).add("ISO abbreviated");
				countriesToIso.put(acName, cAlpha3);
				countriesToIso.put(normalizeKey(acName), cAlpha3);
			}
			TreeSet altNames = ((TreeSet) cData.get("altNames"));
			if (altNames != null) {
				System.out.println(" - alt names:");
				for (Iterator anit = altNames.iterator(); anit.hasNext();) {
					String altName = ((String) anit.next());
					countries.put(altName, cName);
					((TreeSet) countryLangs.get(altName)).add("ISO alternative");
					countriesToIso.put(altName, cAlpha3);
					countriesToIso.put(normalizeKey(altName), cAlpha3);
					System.out.println("   - " + altName);
					if (altName.startsWith("Saint ") || altName.startsWith("Sint ") || altName.startsWith("Sankt ") || altName.startsWith("Sant ")) {
						String aAltName = ("St." + altName.substring(altName.indexOf(' ')));
						countries.put(aAltName, cName);
						((TreeSet) countryLangs.get(aAltName)).add("ISO abbreviated");
						countriesToIso.put(aAltName, cAlpha3);
						countriesToIso.put(normalizeKey(aAltName), cAlpha3);
					}
				}
			}
			TreeSet fspNames = ((TreeSet) cData.get("formerlySovereignParts"));
			if (fspNames != null) {
				System.out.println(" - formerly sovereign parts:");
				for (Iterator fspit = fspNames.iterator(); fspit.hasNext();) {
					String fspName = ((String) fspit.next());
					countries.put(fspName, cName);
					((TreeSet) countryLangs.get(fspName)).add("ISO former");
					countriesToIso.put(fspName, cAlpha3);
					countriesToIso.put(normalizeKey(fspName), cAlpha3);
					System.out.println("   - " + fspName);
					if (fspName.startsWith("Saint ") || fspName.startsWith("Sint ") || fspName.startsWith("Sankt ") || fspName.startsWith("Sant ")) {
						String aAltName = ("St." + fspName.substring(fspName.indexOf(' ')));
						countries.put(aAltName, cName);
						((TreeSet) countryLangs.get(aAltName)).add("ISO abbreviated");
						countriesToIso.put(aAltName, cAlpha3);
						countriesToIso.put(normalizeKey(aAltName), cAlpha3);
					}
				}
			}
			String cDisplayName = ((String) cData.get("displayName"));
			if (cDisplayName == null)
				cDisplayName = cName;
			else if (cDisplayName.startsWith("Saint ") || cDisplayName.startsWith("Sint ") || cDisplayName.startsWith("Sankt ") || cDisplayName.startsWith("Sant ")) {
				String acDisplayName = ("St." + cDisplayName.substring(cDisplayName.indexOf(' ')));
				countries.put(acDisplayName, cName);
				((TreeSet) countryLangs.get(acDisplayName)).add("ISO abbreviated");
				countriesToIso.put(acDisplayName, cAlpha3);
				countriesToIso.put(normalizeKey(acDisplayName), cAlpha3);
			}
			System.out.println(" - display name: " + cDisplayName);
			isoToCountries.put(cAlpha3, cDisplayName);
			countriesToIso.put(cDisplayName, cAlpha3);
			countriesToIso.put(normalizeKey(cDisplayName), cAlpha3);
			countries.put(cDisplayName, cName);
			((TreeSet) countryLangs.get(cName)).add("ISO display");
		}
//		for (Iterator cit = countries.keySet().iterator(); cit.hasNext();) {
//			String cName = ((String) cit.next());
//			String cNameIso = ((String) countries.get(cName));
//			while (countries.containsKey(cNameIso) && !cNameIso.equals(countries.get(cNameIso)))
//				cNameIso = ((String) countries.get(cNameIso));
//			System.out.println(cName + " --> " + cNameIso);
//		}
//		if (true)
//			return;
		
		//	get country list
		TreeMap countryList = getCountryList();
		for (Iterator cnit = countryList.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next());
			String cListName = cName;
			while (countries.containsKey(cName) && !cName.equals(countries.get(cName))) {
				String ncName = ((String) countries.get(cName));
				if (countries.containsKey(ncName) && cName.equals(countries.get(ncName))) {
					System.out.println(" - not normalizing " + cName + " to " + ncName + ", normalizes right back");
					break;
				}
				else {
					System.out.println(" - normalizing " + cName + " to " + ncName);
					cName = ncName;
				}
			}
			countries.put(cListName, cName);
			((TreeSet) countryLangs.get(cListName)).add("WFB official");
			System.out.println("LIST: " + cName + " (" + cListName + ")");
		}
//		for (Iterator cit = countries.keySet().iterator(); cit.hasNext();) {
//			String cName = ((String) cit.next());
//			String cNameIso = ((String) countries.get(cName));
//			while (countries.containsKey(cNameIso) && !cNameIso.equals(countries.get(cNameIso)))
//				cNameIso = ((String) countries.get(cNameIso));
//			System.out.println(cName + " --> " + cNameIso);
//		}
//		if (true)
//			return;
		
		//	get data about each country
		TreeMap countryData = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (Iterator cnit = countryList.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next());
			String cDataLink = ((String) countryList.get(cName));
			TreeMap cData = getCountryData(cName, cDataLink);
			countryData.put(cName, cData);
		}
		
		//	get names of regions (states, provinces, counties, etc.) for each country
		TreeSet regionTypes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (Iterator cnit = countryList.keySet().iterator(); downloadRun && cnit.hasNext();) {
			String cName = ((String) cnit.next());
			String cDataLink = ((String) countryList.get(cName));
			String cIso = ((String) countriesToIso.get(normalizeKey(cName)));
			if ((cIso == null) && (cName.indexOf(',') != -1))
				cIso = ((String) countriesToIso.get(normalizeKey(cName.substring(0, cName.indexOf(',')).trim())));
			if (cIso == null)
				cIso = ("NI-" + cName.replaceAll("[^A-Z]", ""));
			getCountryRegions(cName, cDataLink, cIso.toLowerCase(), regionTypes);
		}
		System.out.println("REGION TYPES: " + regionTypes);
		for (Iterator rtit = regionTypes.iterator(); rtit.hasNext();) {
			String rt = ((String) rtit.next());
			String rtsng;
			if (rt.endsWith("ies"))
				rtsng = rt.substring(0, (rt.length() - "ies".length())) + "y";
			else if (rt.endsWith("shes"))
				rtsng = rt.substring(0, (rt.length() - "es".length()));
			else if (rt.endsWith("s"))
				rtsng = rt.substring(0, (rt.length() - "s".length()));
			else rtsng = rt;
			System.out.println(" - " + rt + " (" + rtsng + ")");
		}
		
		File countryDataFile = new File(dataPath, "countries.xml");
		if (countryDataFile.exists())
			return;
		
		//	extract names of countries
		for (Iterator cnit = countryData.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next());
			System.out.println("WFB: " + cName);
			String cOrigName = cName;
			TreeMap cData = ((TreeMap) countryData.get(cName));
			while (countries.containsKey(cName) && !cName.equals(countries.get(cName))) {
				String ncName = ((String) countries.get(cName));
				if (countries.containsKey(ncName) && cName.equals(countries.get(ncName))) {
					System.out.println(" - not normalizing " + cName + " to " + ncName + ", normalizes right back");
					break;
				}
				else {
					System.out.println(" - normalizing " + cName + " to " + ncName);
					cName = ncName;
				}
			}
			countries.put(cOrigName, cName);
			((TreeSet) countryLangs.get(cOrigName)).add("WFB official");
			System.out.println("WFB: " + cName + " ('" + cOrigName + "')");
			String clf = ((String) cData.get("Country_name/conventional_long_form"));
			if ((clf != null) && !"none".equalsIgnoreCase(clf)) {
				System.out.println(" - CLF: " + clf);
				if (clf.indexOf(';') == -1) {
					countries.put(clf, cName);
					((TreeSet) countryLangs.get(clf)).add("WFB conventional long");
				}
				String[] clfParts = clf.split("(\\s+and\\s+)|(\\s*[\\/\\;]\\s*)");
				if (clfParts.length > 1)
					for (int p = 0; p < clfParts.length; p++) {
						System.out.println("   - " + clfParts[p]);
						countries.put(clfParts[p], cName);
						((TreeSet) countryLangs.get(clfParts[p])).add("WFB conventional long");
					}
			}
			String csf = ((String) cData.get("Country_name/conventional_short_form"));
			if ((csf != null) && !"none".equalsIgnoreCase(csf)) {
				System.out.println(" - CSF: " + csf);
				if (csf.indexOf(';') == -1) {
					countries.put(csf, cName);
					((TreeSet) countryLangs.get(csf)).add("WFB conventional short");
				}
				String[] csfParts = csf.split("(\\s+and\\s+)|(\\s*[\\/\\;]\\s*)");
				if (csfParts.length > 1)
					for (int p = 0; p < csfParts.length; p++) {
						System.out.println("   - " + csfParts[p]);
						countries.put(csfParts[p], cName);
						((TreeSet) countryLangs.get(csfParts[p])).add("WFB conventional short");
					}
			}
			String llf = ((String) cData.get("Country_name/local_long_form"));
			if ((llf != null) && !"none".equalsIgnoreCase(llf)) {
				System.out.println(" - LLF: " + llf);
				String[] llfParts = llf.split("\\s*[\\/\\;]\\s*");
				if (llfParts.length > 1)
					for (int p = 0; p < llfParts.length; p++) {
						System.out.println("   - " + llfParts[p]);
						countries.put(llfParts[p], cName);
						((TreeSet) countryLangs.get(llfParts[p])).add("WFB local long");
					}
				else {
					countries.put(llf, cName);
					((TreeSet) countryLangs.get(llf)).add("WFB local long");
				}
			}
			String lsf = ((String) cData.get("Country_name/local_short_form"));
			if ((lsf != null) && !"none".equalsIgnoreCase(lsf)) {
				System.out.println(" - LSF: " + lsf);
				String[] lsfParts = lsf.split("\\s*[\\/\\;]\\s*");
				if (lsfParts.length > 1)
					for (int p = 0; p < lsfParts.length; p++) {
						System.out.println("   - " + lsfParts[p]);
						countries.put(lsfParts[p], cName);
						((TreeSet) countryLangs.get(lsfParts[p])).add("WFB local short");
					}
				else {
					countries.put(lsf, cName);
					((TreeSet) countryLangs.get(lsf)).add("WFB local short");
				}
			}
			String abr = ((String) cData.get("Country_name/abbreviation"));
			if (abr != null) {
				System.out.println(" - ABBR: " + abr);
				String[] abrs = abr.split("\\s+or\\s+");
				if (abrs.length > 1)
					for (int a = 0; a < abrs.length; a++) {
						System.out.println("   - " + abrs[a]);
						countries.put(abrs[a], cName);
						((TreeSet) countryLangs.get(abrs[a])).add("WFB abbreviation");
					}
				else {
					countries.put(abr, cName);
					((TreeSet) countryLangs.get(abr)).add("WFB abbreviation");
				}
			}
			String frm = ((String) cData.get("Country_name/former"));
			if (frm != null) {
				System.out.println(" - FORMER: " + frm);
				String[] frms = frm.split("\\s*(\\[|\\]|(\\s+or\\s+)|\\,|\\;)+\\s*");
				for (int f = 0; f < frms.length; f++) {
					if (countries.containsKey(frms[f]))
						System.out.println("   - " + frms[f] + " ALREADY MAPPED to " + countries.get(frms[f]));
					else {
						System.out.println("   - " + frms[f]);
						countries.put(frms[f], cName);
						((TreeSet) countryLangs.get(frms[f])).add("WFB former");
					}
				}
			}
		}
//		for (Iterator cit = countries.keySet().iterator(); cit.hasNext();) {
//			String cName = ((String) cit.next());
//			String cNameIso = ((String) countries.get(cName));
//			while (countries.containsKey(cNameIso) && !cNameIso.equals(countries.get(cNameIso)))
//				cNameIso = ((String) countries.get(cNameIso));
//			System.out.println(cName + " --> " + cNameIso);
//		}
//		if (true)
//			return;
		
		//	extract names of dependent territories
		for (Iterator cnit = countryData.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next());
			System.out.println("WFB: " + cName);
			String cOrigName = cName;
			TreeMap cData = ((TreeMap) countryData.get(cName));
			while (countries.containsKey(cName) && !cName.equals(countries.get(cName))) {
				String ncName = ((String) countries.get(cName));
				if (countries.containsKey(ncName) && cName.equals(countries.get(ncName))) {
					System.out.println(" - not normalizing " + cName + " to " + ncName + ", normalizes right back");
					break;
				}
				else {
					System.out.println(" - normalizing " + cName + " to " + ncName);
					cName = ncName;
				}
			}
			System.out.println("WFB: " + cName + " ('" + cOrigName + "')");
			String dpa = ((String) cData.get("Dependent_areas/value/0"));
			if (dpa != null) {
				System.out.println(" - DEPAS: " + dpa);
				String[] dpas = dpa.split("\\s*\\,\\s*");
				for (int d = 0; d < dpas.length; d++) {
					if (countries.containsKey(dpas[d]))
						System.out.println("   - " + dpas[d] + " OWN ISO as " + countries.get(dpas[d]) + ": " + countriesToIso.get(dpas[d]));
					else {
						System.out.println("   - " + dpas[d]);
						countries.put(dpas[d], cName);
						((TreeSet) countryLangs.get(dpas[d])).add("WFB dependency");
					}
				}
			}
		}
//		for (Iterator cit = countries.keySet().iterator(); cit.hasNext();) {
//			String cName = ((String) cit.next());
//			String cNameIso = ((String) countries.get(cName));
//			while (countries.containsKey(cNameIso) && !cNameIso.equals(countries.get(cNameIso)))
//				cNameIso = ((String) countries.get(cNameIso));
//			System.out.println(cName + " --> " + cNameIso);
//		}
//		if (true)
//			return;
		
		//	get list of former colonies from Wikipedia
		TreeMap colonyData = getColonyData();
		for (Iterator cnit = colonyData.keySet().iterator(); cnit.hasNext();) {
			String colName = ((String) cnit.next());
			String cName = ((String) colonyData.get(colName));
			while (countries.containsKey(cName) && !cName.equals(countries.get(cName))) {
				String ncName = ((String) countries.get(cName));
				if (countries.containsKey(ncName) && cName.equals(countries.get(ncName))) {
					System.out.println(" - not normalizing " + cName + " to " + ncName + ", normalizes right back");
					break;
				}
				else {
					System.out.println(" - normalizing " + cName + " to " + ncName);
					cName = ncName;
				}
			}
			countries.put(colName, cName);
			((TreeSet) countryLangs.get(colName)).add("English");
			System.out.println("COL: " + colName + " -> " + cName);
		}
//		for (Iterator cit = countries.keySet().iterator(); cit.hasNext();) {
//			String cName = ((String) cit.next());
//			String cNameIso = ((String) countries.get(cName));
//			while (countries.containsKey(cNameIso) && !cNameIso.equals(countries.get(cNameIso)))
//				cNameIso = ((String) countries.get(cNameIso));
//			System.out.println(cName + " --> " + cNameIso);
//		}
//		if (true)
//			return;
		
		//	get list of country renamings from Wikipedia
		TreeMap formerCountryNames = getFormerCountryNames();
		for (Iterator cnit = formerCountryNames.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next());
			System.out.println("FORMER: " + cName);
			TreeSet fcns = ((TreeSet) formerCountryNames.get(cName));
			while (countries.containsKey(cName) && !cName.equals(countries.get(cName))) {
				String ncName = ((String) countries.get(cName));
				if (countries.containsKey(ncName) && cName.equals(countries.get(ncName))) {
					System.out.println(" - not normalizing " + cName + " to " + ncName + ", normalizes right back");
					break;
				}
				else {
					System.out.println(" - normalizing " + cName + " to " + ncName);
					cName = ncName;
				}
			}
			for (Iterator fcnit = fcns.iterator(); fcnit.hasNext();) {
				String fcName = ((String) fcnit.next());
				countries.put(fcName, cName);
				((TreeSet) countryLangs.get(fcName)).add("English");
				System.out.println(" - " + fcName);
			}
		}
//		for (Iterator cit = countries.keySet().iterator(); cit.hasNext();) {
//			String cName = ((String) cit.next());
//			String cNameIso = ((String) countries.get(cName));
//			while (countries.containsKey(cNameIso) && !cNameIso.equals(countries.get(cNameIso)))
//				cNameIso = ((String) countries.get(cNameIso));
//			System.out.println(cName + " --> " + cNameIso);
//		}
//		if (true)
//			return;
		
		//	translate all country names, store individually
		File translationFolder = new File(dataPath, "trans");
		translationFolder.mkdirs();
		System.out.println("Translating " + countries.size() + " country names");
		TreeMap countriesTranslated = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (Iterator cnit = countries.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next());
			File translationDataFile = new File(translationFolder, ("t." + cName.replaceAll("[^a-zA-Z0-9]", "_") + ".xml"));
			if (translationDataFile.exists())
				loadCountryTranslations(cName, translationDataFile, isoToCountries, countryLangs, countriesTranslated);
			else if (downloadRun)
				downloadCountryTranslations(cName, translationDataFile, isoToCountries, countryLangs, countriesTranslated);
		}
		countries.putAll(countriesTranslated);
		
		//	get country names in languages from Wikipedia
		TreeMap langCountryNames = getLangCountryNames();
		for (Iterator cnit = langCountryNames.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next());
			System.out.println("LANG: " + cName);
			TreeMap lcns = ((TreeMap) langCountryNames.get(cName));
			if (!countries.containsKey(cName))
				for (Iterator lcnit = lcns.keySet().iterator(); lcnit.hasNext();) {
					String lcName = ((String) lcnit.next());
					if (countries.containsKey(lcName)) {
						cName = ((String) countries.get(lcName));
						break;
					}
				}
			while (countries.containsKey(cName) && !cName.equals(countries.get(cName))) {
				String ncName = ((String) countries.get(cName));
				if (countries.containsKey(ncName) && cName.equals(countries.get(ncName))) {
					System.out.println(" - not normalizing " + cName + " to " + ncName + ", normalizes right back");
					break;
				}
				else {
					System.out.println(" - normalizing " + cName + " to " + ncName);
					cName = ncName;
				}
			}
			for (Iterator lcnit = lcns.keySet().iterator(); lcnit.hasNext();) {
				String lcName = ((String) lcnit.next());
				countries.put(lcName, cName);
				TreeSet cNameLangs = (TreeSet) lcns.get(lcName);
				((TreeSet) countryLangs.get(lcName)).addAll(cNameLangs);
				System.out.println(" - " + lcName + " (" + lcns.get(lcName) + ")");
				if ((lcName.indexOf('.') != -1) && (cNameLangs.contains("initialism") || cNameLangs.contains("initialisms") || cNameLangs.contains("English initialism"))) {
					String asLcName;
					asLcName = lcName.replaceAll("\\.\\s*", "").trim();
					countries.put(asLcName, cName);
					((TreeSet) countryLangs.get(asLcName)).addAll(cNameLangs);
					asLcName = lcName.replaceAll("\\.\\s*", " ").trim();
					countries.put(asLcName, cName);
					((TreeSet) countryLangs.get(asLcName)).addAll(cNameLangs);
					asLcName = lcName.replaceAll("\\.\\s*", ".").trim();
					countries.put(asLcName, cName);
					((TreeSet) countryLangs.get(asLcName)).addAll(cNameLangs);
					asLcName = lcName.replaceAll("\\.\\s*", ". ").trim();
					countries.put(asLcName, cName);
					((TreeSet) countryLangs.get(asLcName)).addAll(cNameLangs);
				}
			}
		}
//		for (Iterator cit = countries.keySet().iterator(); cit.hasNext();) {
//			String cName = ((String) cit.next());
//			String cNameIso = ((String) countries.get(cName));
//			while (countries.containsKey(cNameIso) && !cNameIso.equals(countries.get(cNameIso)))
//				cNameIso = ((String) countries.get(cNameIso));
//			System.out.println(cName + " --> " + cNameIso);
//		}
//		if (true)
//			return;
		
		//	report result
		System.out.println("Got " + countries.size() + " country names");
		for (Iterator cnit = countries.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next());
			String ccName = cName;
			while (countries.containsKey(ccName) && !ccName.equals(countries.get(ccName))) {
				String nccName = ((String) countries.get(ccName));
				if (countries.containsKey(nccName) && ccName.equals(countries.get(nccName))) {
					System.out.println(" - not normalizing " + ccName + " to " + nccName + ", normalizes right back");
					break;
				}
				else {
					System.out.println(" - normalizing " + ccName + " to " + nccName);
					ccName = nccName;
				}
			}
			String cIso3 = ((String) countriesToIso.get(normalizeKey(ccName)));
			if (cIso3 == null)
				System.out.println(" - '" + cName + "' is '" + ccName + "' (NOT RESOLVED) in " + countryLangs.get(cName));
			else {
				String cIsoName = ((String) isoToCountries.get(cIso3));
				System.out.println(" - '" + cName + "' is '" + ccName + "' (" + cIso3 + ", " + cIsoName + ") in " + countryLangs.get(cName));
			}
		}
		
		//	map country display names to maps from country names to sets of languages
		TreeMap countryDataList = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
			public Object get(Object key) {
				Object value = super.get(key);
				if (value == null) {
					value = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
						public Object get(Object key) {
							Object value = super.get(key);
							if (value == null) {
								value = new TreeSet(String.CASE_INSENSITIVE_ORDER);
								this.put(key, value);
							}
							return value;
						}
					};
					this.put(key, value);
				}
				return value;
			}
		};
		for (Iterator cnit = countries.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next());
			String rcName = cName;
			while (countries.containsKey(rcName) && !rcName.equals(countries.get(rcName))) {
				String nrcName = ((String) countries.get(rcName));
				if (countries.containsKey(nrcName) && rcName.equals(countries.get(nrcName))) {
					System.out.println(" - not normalizing " + rcName + " to " + nrcName + ", normalizes right back");
					break;
				}
				else {
					System.out.println(" - normalizing " + rcName + " to " + nrcName);
					rcName = nrcName;
				}
			}
			String cIso3 = ((String) countriesToIso.get(normalizeKey(rcName)));
			String cIsoName = ((cIso3 == null) ? rcName : ((String) isoToCountries.get(cIso3)));
			TreeMap cLangNames = ((TreeMap) countryDataList.get(cIsoName));
			((TreeSet) cLangNames.get(cName)).addAll((TreeSet) countryLangs.get(cName));
		}
		
		//	cast that in XML
		countryDataFile.createNewFile();
		final BufferedWriter cdw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(countryDataFile), "UTF-8"));
		cdw.write("<countries>");
		cdw.newLine();
		for (Iterator cnit = countryDataList.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next());
			String cIso3 = ((String) countriesToIso.get(normalizeKey(cName)));
			String cIso2 = ((cIso3 == null) ? null : ((String) iso3toIso2.get(cIso3)));
			String cIsoName = ((cIso3 == null) ? cName : ((String) isoToCountries.get(cIso3)));
			cdw.write("\t<country name=\"" + cIsoName + "\"" + ((cIso2 == null) ? "" : (" iso2=\"" + cIso2 + "\"")) + ((cIso3 == null) ? "" : (" iso3=\"" + cIso3 + "\"")) + ">");
			cdw.newLine();
			TreeMap cLangNames = ((TreeMap) countryDataList.get(cName));
			for (Iterator clnit = cLangNames.keySet().iterator(); clnit.hasNext();) {
				String cLangName = ((String) clnit.next());
				cdw.write("\t\t<name normalized=\"" + xml.escape(normalizeString(cLangName)) + "\" languages=\"");
				TreeSet cNameLangs = ((TreeSet) cLangNames.get(cLangName));
				for (Iterator lit = cNameLangs.iterator(); lit.hasNext();)
					cdw.write(xml.escape(normalizeString((String) lit.next())) + (lit.hasNext() ? ";" : ""));
				cdw.write("\">");
				cdw.write(xml.escape(cLangName));
				cdw.write("</name>");
				cdw.newLine();
			}
			cdw.write("\t</country>");
			cdw.newLine();
		}
		cdw.write("</countries>");
		cdw.newLine();
		cdw.flush();
		cdw.close();
	}
	
	private static void getCountryRegions(String cName, String cDataLink, String cIso, TreeSet allRegionTypes) throws IOException {
		File cRegionFile = new File(dataPath, ("regions." + cIso + ".xml"));
		if (cRegionFile.exists())
			return;
		
		TreeMap cData = getCountryData(cName, cDataLink);
		System.out.println("WFB: " + cName);
		
		for (Iterator kit = cData.keySet().iterator(); kit.hasNext();) {
			String key = ((String) kit.next());
			if (!key.startsWith("Administrative_divisions/"))
				continue;
			String value = ((String) cData.get(key));
			System.out.println(" - " + key.substring("Administrative_divisions/".length()) + ": " + value);
		}
		
		boolean translationInParentheses = true;
		String remark = ((String) cData.get("Administrative_divisions/"));
		if ((remark != null) && (remark.indexOf("in parentheses") != -1))
			translationInParentheses = false;
		
		TreeMap regionList = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
			public Object get(Object key) {
				Object value = super.get(key);
				if (value == null) {
					value = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
						public Object get(Object key) {
							Object value = super.get(key);
							if (!"variants".equals(key))
								return value;
							else if (value == null) {
								value = new TreeSet(String.CASE_INSENSITIVE_ORDER);
								this.put(key, value);
							}
							return value;
						}
					};
					this.put(key, value);
				}
				return value;
			}
		};
		
		String value0 = ((String) cData.get("Administrative_divisions/value/0"));
		if (value0 == null) {
			for (Iterator kit = cData.keySet().iterator(); kit.hasNext();) {
				String key = ((String) kit.next());
				if (!key.startsWith("Administrative_divisions/"))
					continue;
				String lKey = key.substring("Administrative_divisions/".length());
				if (!lKey.matches("[A-Z][a-z]+(\\_[A-Z][a-z]+)?"))
					continue;
				String value = ((String) cData.get(key));
				System.out.println(" - " + key.substring("Administrative_divisions/".length()) + ": " + value);
				getCountryRegions(cName, cData, value, translationInParentheses, allRegionTypes, regionList);
			}
		}
		else getCountryRegions(cName, cData, value0, translationInParentheses, allRegionTypes, regionList);
		
		//	print region list
		for (Iterator rit = regionList.keySet().iterator(); rit.hasNext();) {
			String r = ((String) rit.next());
			TreeMap rData = ((TreeMap) regionList.get(r));
			System.out.println(" - " + r + " (" + rData.get("type") + ", " + rData.get("typeTranslated") + "): " + rData.get("variants"));
		}
		
		//	translate regions
		String crTranslationFileFolder = cDataLink;
		crTranslationFileFolder = crTranslationFileFolder.substring(crTranslationFileFolder.lastIndexOf('/') + 1);
		crTranslationFileFolder = crTranslationFileFolder.substring(0, crTranslationFileFolder.lastIndexOf('.'));
		File translationFolder = new File(dataPath, ("trans-" + crTranslationFileFolder));
		translationFolder.mkdirs();
		System.out.println("Translating " + regionList.size() + " region names");
		TreeMap cRegionNames = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
			public Object get(Object key) {
				Object value = super.get(key);
				if (value == null) {
					value = new TreeSet(String.CASE_INSENSITIVE_ORDER);
					this.put(key, value);
				}
				return value;
			}
		};
		TreeMap cRegionNameLangs = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
			public Object get(Object key) {
				Object value = super.get(key);
				if (value == null) {
					value = new TreeSet(String.CASE_INSENSITIVE_ORDER);
					this.put(key, value);
				}
				return value;
			}
		};
		for (Iterator rit = regionList.keySet().iterator(); rit.hasNext();) {
			final String rName = ((String) rit.next());
			TreeMap rData = ((TreeMap) regionList.get(rName));
			TreeSet rVariants = ((TreeSet) rData.get("variants"));
			((TreeSet) cRegionNameLangs.get(rName)).add("English");
			((TreeSet) cRegionNameLangs.get(rName)).add("official");
			for (Iterator vit = rVariants.iterator(); vit.hasNext();) {
				final String rVariant = ((String) vit.next());
				((TreeSet) cRegionNames.get(rName)).add(rVariant);
				((TreeSet) cRegionNameLangs.get(rVariant)).add("English");
				((TreeSet) cRegionNameLangs.get(rVariant)).add("official");
				File translationDataFile = new File(translationFolder, ("t." + rVariant.replaceAll("[^a-zA-Z0-9]", "_") + ".xml"));
				TreeMap rVariantTranslations = null;
				if (translationDataFile.exists()) {
					final TreeMap rVariantTranslationsLoad = new TreeMap(String.CASE_INSENSITIVE_ORDER);
					BufferedReader tdfr = new BufferedReader(new InputStreamReader(new FileInputStream(translationDataFile), "UTF-8"));
					xmlParser.stream(tdfr, new TokenReceiver() {
						String language = null;
						public void storeToken(String token, int treeDepth) throws IOException {
							if (xml.isTag(token)) {
								if ("translation".equals(xml.getType(token)) && !xml.isEndTag(token)) {
									TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xml);
									this.language = tnas.getAttribute("lang");
								}
								else this.language = null;
							}
							else if (this.language != null)
								rVariantTranslationsLoad.put(this.language, xml.unescape(token));
						}
						public void close() throws IOException {}
					});
					tdfr.close();
					rVariantTranslations = new TreeMap(String.CASE_INSENSITIVE_ORDER);
					rVariantTranslations.putAll(rVariantTranslationsLoad);
					for (Iterator lit = rVariantTranslations.keySet().iterator(); lit.hasNext();) {
						String lang = ((String) lit.next());
						String trans = ((String) rVariantTranslations.get(lang));
						((TreeSet) cRegionNameLangs.get(trans)).add(lang);
						((TreeSet) cRegionNames.get(rName)).add(trans);
					}
					continue;
				}
				if (!downloadRun)
					continue;
				if (rVariantTranslations == null) try {
					rVariantTranslations = getWikipediaTranslations(rVariant + ", " + cName);
				}
				catch (IOException ioe) {
					System.out.println("Could not get translations for '" + rVariant + ", " + cName + "': " + ioe.getMessage());
					ioe.printStackTrace(System.out);
				}
				if (rVariantTranslations == null) try {
					rVariantTranslations = getWikipediaTranslations(rVariant + " (" + cName + ")");
				}
				catch (IOException ioe) {
					System.out.println("Could not get translations for '" + rVariant + " (" + cName + ")" + "': " + ioe.getMessage());
					ioe.printStackTrace(System.out);
				}
				if (rVariantTranslations == null) try {
					rVariantTranslations = getWikipediaTranslations(rVariant);
				}
				catch (IOException ioe) {
					System.out.println("Could not get translations for '" + rVariant + "': " + ioe.getMessage());
					ioe.printStackTrace(System.out);
				}
				if (rVariantTranslations != null) try {
					BufferedWriter tdw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(translationDataFile), "UTF-8"));
					tdw.write("<term value=\"" + xml.escape(rVariant) + "\">");
					tdw.newLine();
					for (Iterator lit = rVariantTranslations.keySet().iterator(); lit.hasNext();) {
						String lang = ((String) lit.next());
						String lrName = ((String) rVariantTranslations.get(lang));
						lrName = lrName.replaceAll("\\,\\s+.*", "");
						lrName = lrName.replaceAll("\\s+\\(.*", "");
						tdw.write("\t<translation lang=\"" + xml.escape(lang) + "\">" + xml.escape(lrName) + "</translation>");
						tdw.newLine();
					}
					tdw.write("</term>");
					tdw.newLine();
					tdw.flush();
					tdw.close();
					for (Iterator lit = rVariantTranslations.keySet().iterator(); lit.hasNext();) {
						String lang = ((String) lit.next());
						String trans = ((String) rVariantTranslations.get(lang));
						((TreeSet) cRegionNames.get(rName)).add(trans);
						((TreeSet) cRegionNameLangs.get(trans)).add(lang);
					}
				}
				catch (IOException ioe) {
					System.out.println("Could not get translations for '" + cName + "': " + ioe.getMessage());
					ioe.printStackTrace(System.out);
				}
			}
		}
		
		cRegionFile.createNewFile();
		BufferedWriter crw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(cRegionFile), "UTF-8"));
		crw.write("<regions country=\"" + cName + "\">");
		crw.newLine();
		for (Iterator rit = regionList.keySet().iterator(); rit.hasNext();) {
			String rName = ((String) rit.next());
			TreeMap rData = ((TreeMap) regionList.get(rName));
			crw.write("\t<region name=\"" + rName + "\" type=\"" + ((String) rData.get("type")) + "\" typeTranslated=\"" + ((String) rData.get("typeTranslated")) + "\">");
			crw.newLine();
			TreeSet rNames = ((TreeSet) cRegionNames.get(rName));
			for (Iterator rnit = rNames.iterator(); rnit.hasNext();) {
				String rLangName = ((String) rnit.next());
				TreeSet rNameLangs = ((TreeSet) cRegionNameLangs.get(rLangName));
				crw.write("\t\t<name normalized=\"" + xml.escape(normalizeString(rLangName)) + "\" languages=\"");
				for (Iterator lit = rNameLangs.iterator(); lit.hasNext();)
					crw.write(xml.escape(normalizeString((String) lit.next())) + (lit.hasNext() ? ";" : ""));
				crw.write("\">");
				crw.write(xml.escape(rLangName));
				crw.write("</name>");
				crw.newLine();
			}
			crw.write("\t</region>");
			crw.newLine();
		}
		crw.write("</regions>");
		crw.newLine();
		crw.flush();
		crw.close();
	}
	
	private static void getCountryRegions(String cName, TreeMap cData, String value0, boolean translationInParentheses, TreeSet allRegionTypes, TreeMap regionList) throws IOException {
		String regionDataString = value0;
		String note = null;
		if (regionDataString.indexOf("; note") != -1) {
			note = regionDataString.substring(regionDataString.indexOf("; note") + "; note".length());
			regionDataString = regionDataString.substring(0, regionDataString.indexOf("; note"));
			System.out.println("NOTE: " + note);
//			note = note.replaceAll("\\sone\\s", " 1 ");
			note = note.replaceAll("\\stwo\\s", " 2 ");
			note = note.replaceAll("\\sthree\\s", " 3 ");
			note = note.replaceAll("\\sfour\\s", " 4 ");
			note = note.replaceAll("\\sfive\\s", " 5 ");
		}
		String regionTypeString = value0;
		String[] regions = {};
		
		Pattern numberPattern = Pattern.compile("[1-9][0-9]*");
		Matcher numberMatcher = numberPattern.matcher(regionDataString);
		int numberStart = (numberMatcher.find() ? numberMatcher.start() : -1);
		while ((regionDataString.indexOf(';') < numberStart) && (regionDataString.indexOf(';') != -1)) {
			numberStart -= (regionDataString.indexOf(';') + 1);
			regionDataString = regionDataString.substring(regionDataString.indexOf(';') + 1).trim();
		}
		
		if (regionDataString.endsWith(";"))
			regionDataString = regionDataString.substring(0, (regionDataString.length() - ";".length())).trim();
		if (regionDataString.indexOf("*:") != -1)
			regionDataString = regionDataString.replaceAll("\\*\\:", "*;");
		
		if (regionDataString.matches(".*\\([^\\)]+\\;[^\\)]+\\).*")) {
			System.out.println("BULLSHIT: " + regionDataString);
			regionDataString = regionDataString.replaceAll("\\([^\\)]+\\;[^\\)]+\\)", "");
			System.out.println("CLEANUP: " + regionDataString);
			regions = regionDataString.split("\\s*[\\,\\;]\\s+");
		}
		
		else if (regionDataString.indexOf("));") != -1) {
			regionTypeString = regionDataString.substring(0, regionDataString.indexOf("));")).trim();
			regionDataString = regionDataString.substring(regionDataString.indexOf("));") + "));".length()).trim();
			regions = regionDataString.split("\\s*[\\,\\;]\\s+");
		}
		
		else if (regionDataString.indexOf(");") != -1) {
			regionTypeString = regionDataString.substring(0, regionDataString.indexOf(");")).trim();
			regionDataString = regionDataString.substring(regionDataString.indexOf(");") + ");".length()).trim();
			regions = regionDataString.split("\\s*[\\,\\;]\\s+");
		}
		
		else if (regionDataString.indexOf(';') != -1) {
			regionTypeString = regionDataString.substring(0, regionDataString.lastIndexOf(";")).trim();
			regionDataString = regionDataString.substring(regionDataString.lastIndexOf(';') + 1).trim();
			regions = regionDataString.split("\\s*\\,\\s+");
		}
		
		regionTypeString = regionTypeString.replaceAll("\\s\\([^\\)]+\\)\\*", "*");
		
		//	extract keys (comma separated list, after number, and before opening bracket)
		LinkedList regionTypes = new LinkedList();
		Pattern regionTypePattern = Pattern.compile("[1-9][0-9]*(\\s+[La-z\\-]+)+[\\*]*");
		for (Matcher rtm = regionTypePattern.matcher(regionTypeString); rtm.find();) {
			String regionType = rtm.group().trim();
			if (regionType.indexOf(' ') > 3)
				continue;
			regionType = regionType.substring(regionType.indexOf(' ')).trim();
			if (regionType.endsWith(" and"))
				regionType = regionType.substring(0, (regionType.length() - " and".length()));
			if (regionType.endsWith(" of"))
				regionType = regionType.substring(0, (regionType.length() - " of".length()));
			if ((regionType.indexOf(" the ") != -1) || (regionType.indexOf(" its ") != -1))
				continue;
			System.out.println(" - region type: " + regionType);
			regionTypes.add(regionType);
			allRegionTypes.add(regionType.replaceAll("\\*", ""));
		}
		
		
		//	get country-local region types
		TreeMap regionTypeTranslations = new TreeMap(String.CASE_INSENSITIVE_ORDER) {
			public Object get(Object key) {
				Object value = super.get(key);
				return ((value == null) ? key : value);
			}
		};
		Pattern regionTypeTranslationPattern = Pattern.compile("\\(" +
				"(" +
					"[^\\)\\(]" +
					"|" +
					"(\\([^\\)\\(]+\\))" +
				")+" +
			"\\)");
		for (Iterator rtit = regionTypes.iterator(); rtit.hasNext();) {
			String rt = ((String) rtit.next());
			System.out.println(" - region type: " + rt);
			if (value0.indexOf(rt) == -1) {
				System.out.println(" --> no translation available (1)");
				continue;
			}
			Matcher rttm = regionTypeTranslationPattern.matcher(value0);
			if (rttm.find(value0.indexOf(rt) + rt.length())) {
				if (((rttm.start() - (value0.indexOf(rt) + rt.length())) < 3) && !rttm.group().startsWith("(including")) {
					System.out.println(" --> " + rttm.group());
					String rtt = rttm.group();
					rtt = rtt.substring("(".length(), (rtt.length() - ")".length())).trim();
					rtt = rtt.replaceAll("[\\*]+", "").trim();
					rtt = rtt.replaceAll("\\s*(\\([^\\)]+\\))\\s*", " ").trim();
					rtt = rtt.replaceAll("\\s*in\\s+[A-Z][a-z]+\\s*", " ").trim();
					rtt = rtt.replaceAll("(\\s*\\-)?\\s*singular\\s*(\\-|(and\\s+plural))?\\s*", " ").trim();
					while (rtt.startsWith(";") || rtt.startsWith(","))
						rtt = rtt.substring(";".length());
					while (rtt.endsWith(";") || rtt.endsWith(","))
						rtt = rtt.substring(0, (rtt.length() - ";".length()));
					System.out.println("   - " + rtt);
					String[] rttps = rtt.split("\\s*[\\;\\,]\\s*");
					if (rttps.length == 0)
						System.out.println("   --> BULLSHIT");
					else {
						System.out.println("   --> " + rttps[rttps.length-1]);
						if (rt.endsWith("ies")) {
							regionTypeTranslations.put(rt, (rttps[rttps.length-1] + "s"));
							rt = rt.substring(0, (rt.length() - "ies".length())) + "y";
						}
						else if (rt.endsWith("shes")) {
							regionTypeTranslations.put(rt, (rttps[rttps.length-1] + "s"));
							rt = rt.substring(0, (rt.length() - "es".length()));
						}
						else if (rt.endsWith("s")) {
							regionTypeTranslations.put(rt, (rttps[rttps.length-1] + "s"));
							rt = rt.substring(0, (rt.length() - "s".length()));
						}
						regionTypeTranslations.put(rt, rttps[rttps.length-1]);
					}
				}
				else System.out.println(" --> " + rttm.group() + " at distance of " + (rttm.start() - (value0.indexOf(rt) + rt.length())));
			}
			else System.out.println(" --> no translation available (2)");
		}
		
		//	handle cases with everything in line
		filterRegions(regions, regionTypes, regionTypeTranslations, translationInParentheses, cName, regionList);
		
		//	handle cases with individual bulletin points
		for (Iterator rtit = regionTypes.iterator(); rtit.hasNext();) {
			String rt = ((String) rtit.next());
			String prt = rt.replaceAll("\\*", "");
			String rtk = prt.replaceAll("\\s", "_");
			String rtv;
			if (cData.containsKey("Administrative_divisions/" + rtk)) {
				rtv = ((String) cData.get("Administrative_divisions/" + rtk));
				System.out.println(" - " + rt + " --> " + rtv);
				regions = rtv.split("\\s*[\\,\\;]\\s+");
				filterRegions(regions, prt, regionTypeTranslations, translationInParentheses, cName, regionList);
			}
			else if (cData.containsKey("Administrative_divisions/" + rtk + "/0")) {
				for (int vnr = 0; cData.containsKey("Administrative_divisions/" + rtk + "/" + vnr); vnr++) {
					rtv = ((String) cData.get("Administrative_divisions/" + rtk + "/" + vnr));
					System.out.println(" - " + rt + "/" + vnr + " --> " + rtv);
					regions = rtv.split("\\s*[\\,\\;]\\s+");
					filterRegions(regions, prt, regionTypeTranslations, translationInParentheses, cName, regionList);
				}
			}
			else if (cData.containsKey("Administrative_divisions/" + regionTypeTranslations.get(prt))) {
				rtv = ((String) cData.get("Administrative_divisions/" + regionTypeTranslations.get(prt)));
				System.out.println(" - " + rt + " (" + regionTypeTranslations.get(prt) + ") --> " + rtv);
				regions = rtv.split("\\s*[\\,\\;]\\s+");
				filterRegions(regions, prt, regionTypeTranslations, translationInParentheses, cName, regionList);
			}
			else if (rtk.endsWith("s")) {
				if (rtk.endsWith("ies"))
					rtk = rtk.substring(0, (rtk.length() - "ies".length())) + "y";
				else if (rtk.endsWith("shes"))
					rtk = rtk.substring(0, (rtk.length() - "es".length()));
				else if (rtk.endsWith("s"))
					rtk = rtk.substring(0, (rtk.length() - "s".length()));
				if (cData.containsKey("Administrative_divisions/" + rtk)) {
					rtv = ((String) cData.get("Administrative_divisions/" + rtk));
					System.out.println(" - " + rt + " --> " + rtv);
					regions = rtv.split("\\s*[\\,\\;]\\s+");
					filterRegions(regions, prt, regionTypeTranslations, translationInParentheses, cName, regionList);
				}
			}
		}
		
		//	check note
		if (note != null) {
			Matcher rtm = regionTypePattern.matcher(note);
			boolean gotRt = rtm.find();
			while (gotRt) {
				String regionType = rtm.group().trim();
				if (regionType.indexOf(' ') > 3) {
					gotRt = rtm.find();
					continue;
				}
				regionType = regionType.substring(regionType.indexOf(' ')).trim();
				if (regionType.indexOf(" - ") != -1)
					regionType = regionType.substring(0, regionType.indexOf(" - "));
				if (regionType.endsWith(" and"))
					regionType = regionType.substring(0, (regionType.length() - " and".length()));
				if (regionType.endsWith(" -"))
					regionType = regionType.substring(0, (regionType.length() - " -".length()));
				if (regionType.endsWith(" of"))
					regionType = regionType.substring(0, (regionType.length() - " of".length()));
				if ((regionType.indexOf(" the ") != -1) || (regionType.indexOf(" its ") != -1)) {
					gotRt = rtm.find();
					continue;
				}
				System.out.println(" - NOTE region type: " + regionType);
				regionTypes.add(regionType);
				allRegionTypes.add(regionType.replaceAll("\\*", ""));
				int start = rtm.end();
				int end = note.length();
				gotRt = rtm.find();
				if (gotRt)
					end = rtm.start();
				System.out.println("   - start: " + start);
				System.out.println("   - end: " + end);
				for (Matcher rm = Pattern.compile("\\([^\\)]+\\)").matcher(note); rm.find();) {
					System.out.println(" - (" + rm.start() + " to " + rm.end() + ") " + rm.group());
					if (rm.start() < start) {
						System.out.println(" --> too early, skipped");
						continue;
					}
					if (rm.end() > end) {
						System.out.println(" --> too late, we're done");
						break;
					}
					String regionString = rm.group();
					regionString = regionString.substring(1, (regionString.length() - 1));
					regions = regionString.split("\\s*\\,\\s+");
					filterRegions(regions, regionType, regionTypeTranslations, translationInParentheses, cName, regionList);
				}
			}
			
		}
	}
	
	private static void filterRegions(String[] regions, String regionType, TreeMap regionTypeTranslations, boolean translationInParentheses, String country, TreeMap regionList) {
		LinkedList regionTypes = new LinkedList();
		regionTypes.add(regionType);
		filterRegions(regions, regionTypes, regionTypeTranslations, translationInParentheses, country, regionList);
	}
	
	private static void filterRegions(String[] regions, LinkedList regionTypes, TreeMap regionTypeTranslations, boolean translationInParentheses, String country, TreeMap regionList) {
		
		//	index region types by asterisks
		TreeMap regionTypeMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (Iterator rtit = regionTypes.iterator(); rtit.hasNext();) {
			String rt = ((String) rtit.next());
			String rtk;
			int as = rt.indexOf('*');
			if (as == -1)
				rtk = "";
			else {
				rtk = rt.substring(as);
				rt = rt.substring(0, as);
			}
			if (rt.endsWith("ies"))
				rt = rt.substring(0, (rt.length() - "ies".length())) + "y";
			else if (rt.endsWith("shes"))
				rt = rt.substring(0, (rt.length() - "es".length()));
			else if (rt.endsWith("s"))
				rt = rt.substring(0, (rt.length() - "s".length()));
			regionTypeMap.put(rtk, rt);
		}
		
		//	extract translations (may be in round or square brackets)
		for (int r = 0; r < regions.length; r++) {
			if (regions[r].matches("[1-9][0-9]*.*"))
				continue;
			regions[r] = regions[r].trim();
			String rtk = regions[r].replaceAll("[^\\*]+", "");
			String rt = ((String) regionTypeMap.get(rtk));
			if (rt == null)
				rt = ((String) regionTypeMap.get(""));
			String rtt = ((rt == null) ? null : ((String) regionTypeTranslations.get(rt)));
			regions[r] = regions[r].replaceAll("[\\*]+", "");
			if (!translationInParentheses)
				regions[r] = regions[r].replaceAll("\\s*\\([^\\)]+\\)\\s*", " ");
			while (regions[r].matches("[^A-Z]+\\s+.*"))
				regions[r] = regions[r].substring(regions[r].indexOf(' ')).trim();
			regions[r] = regions[r].replaceAll("\\s*\\((city|county|Arabic|Aranese|Basque|Castilian|(Dutch(\\s.*form)?)|(Finnish(\\sand\\Swedish)?)|(French(\\s.*form)?)|German|Kurdish|Swedish|Valencian)\\)\\s*", " ");
			regions[r] = regions[r].trim();
			if ("US Government".equals(regions[r]))
				continue;
			String[] rps = regions[r].split("\\s*([\\(\\)\\[\\]]|(\\sor\\s))\\s*");
			if (rps.length == 1) {
				System.out.println(" - " + regions[r] + " --- " + rt + " (" + rtt + ")");
				TreeMap rData = ((TreeMap) regionList.get(regions[r]));
				if (rt != null) {
					rData.put("type", rt);
					rData.put("typeTranslated", ((rtt == null) ? rt : rtt));
				}
				TreeSet rVariants = ((TreeSet) rData.get("variants"));
				rVariants.add(regions[r]);
				String[] regionVariants = getRegionVariants(regions[r], rt, rtt, country);
				for (int v = 0; v < regionVariants.length; v++) {
					rVariants.add(regionVariants[v]);
					System.out.println(" -> " + regionVariants[v]);
				}
				continue;
			}
			System.out.println(" - " + regions[r] + " --- " + rt + " (" + rtt + ")");
			TreeMap rData = ((TreeMap) regionList.get(rps[0]));
			if (rt != null) {
				rData.put("type", rt);
				rData.put("typeTranslated", ((rtt == null) ? rt : rtt));
			}
			TreeSet rVariants = ((TreeSet) rData.get("variants"));
			for (int p = 0; p < rps.length; p++) {
				rps[p] = rps[p].replaceAll("\\*", "").trim();
				while (rps[p].matches("[^A-Z]+\\s+.*"))
					rps[p] = rps[p].substring(rps[p].indexOf(' ')).trim();
				if (rps[p].length() == 0)
					continue;
				rVariants.add(rps[p]);
				System.out.println("   - " + rps[p]);
				String[] regionVariants = getRegionVariants(rps[p], rt, rtt, country);
				for (int v = 0; v < regionVariants.length; v++) {
					rVariants.add(regionVariants[v]);
					System.out.println("   -> " + regionVariants[v]);
				}
			}
		}
	}
	
	private static String[] getRegionVariants(String region, String regionType, String regionTypeTranslation, String country) {
		TreeSet regionVariants = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		
		if (region.indexOf('/') != -1) {
			String[] rps = region.split("\\s*\\/\\s*");
			for (int p = 0; p < rps.length; p++)
				if (rps[p].length() != 0) {
					regionVariants.add(rps[p]);
					regionVariants.addAll(Arrays.asList(getRegionVariants(rps[p], regionType, regionTypeTranslation, country)));
				}
		}
		
		StringBuffer nRegion = new StringBuffer();
		for (int c = 0; c < region.length(); c++) {
			char ch = region.charAt(c);
			if (Character.isLetter(ch) || (ch == ' ')) {
				nRegion.append(ch);
				continue;
			}
			if ((nRegion.length() == 0) || !Character.isLetter(nRegion.charAt(nRegion.length()-1)))
				continue;
			if (ch != '\'') {
				nRegion.append(ch);
				continue;
			}
			if (((c+1) == region.length()) || ("AEHIJOUY".indexOf(region.charAt(c+1)) == -1))
				continue;
			nRegion.append(ch);
		}
		if (!region.equals(nRegion.toString())) {
			regionVariants.add(nRegion.toString());
			regionVariants.addAll(Arrays.asList(getRegionVariants(nRegion.toString(), regionType, regionTypeTranslation, country)));
		}
		
		if ((regionType == null) || ("area;borough;county;department;district;division;governorate;island;kray;parish;prefecture;province;quarter;raion;rayon;region;republic;territory;unit;ward;zone".indexOf(regionType) == -1))
			return ((String[]) regionVariants.toArray(new String[regionVariants.size()]));
		
		String regionStem = region.replaceAll("\\-", "");
		int rsl;
		int english = 0;
		int french = 0;
		do {
			rsl = regionStem.length();
			regionStem = regionStem.replaceAll("(Far|Upper|Lower|Inner|Outer|Central|Center|Northern|North|Southern|South|Eastern|East|Western|West)", "");
			regionStem = regionStem.replaceAll("\\s+", " ").trim();
			if (regionStem.length() < rsl) {
				english += (rsl - regionStem.length());
				continue;
			}
			regionStem = regionStem.replaceAll("(Extreme|Centre|Nord|Sud|Est|Ouest)", "");
			regionStem = regionStem.replaceAll("\\s+", " ").trim();
			if (regionStem.length() < rsl) {
				french += (rsl - regionStem.length());
				continue;
			}
		}
		while (regionStem.length() < rsl);
		
		String regionTypeProper = getProperForm(regionType);
		if ((regionTypeProper != null) && (region.indexOf(regionTypeProper) != -1))
			regionTypeProper = null;
		String regionTypeTranslationProper = getProperForm(regionTypeTranslation);
		if ((regionTypeTranslationProper != null) && (region.indexOf(regionTypeTranslationProper) != -1))
			regionTypeTranslationProper = null;
		
		if ((regionStem.length() != 0) && !regionStem.equals(country)) {
			if (region.endsWith(" North") || region.endsWith(" South") || region.endsWith(" East") || region.endsWith(" West")) {
				regionVariants.add(region.substring(region.lastIndexOf(' ')).trim() + " " + region.substring(0, region.lastIndexOf(' ')).trim());
				regionVariants.add(region.substring(0, region.lastIndexOf(' ')).trim());
				if ((regionTypeProper != null) && (french <= english)) {
					regionVariants.add(region.substring(region.lastIndexOf(' ')).trim() + " " + region.substring(0, region.lastIndexOf(' ')).trim() + " " + regionTypeProper);
					regionVariants.add(region.substring(0, region.lastIndexOf(' ')).trim() + " " + regionTypeProper);
				}
				if ((regionTypeTranslationProper != null) && (french <= english)) {
					regionVariants.add(region.substring(region.lastIndexOf(' ')).trim() + " " + region.substring(0, region.lastIndexOf(' ')).trim() + " " + regionTypeTranslationProper);
					regionVariants.add(region.substring(0, region.lastIndexOf(' ')).trim() + " " + regionTypeTranslationProper);
				}
			}
			if (region.startsWith("North ") || region.startsWith("South ") || region.startsWith("East ") || region.startsWith("West ") || region.startsWith("Northern ") || region.startsWith("Southern ") || region.startsWith("Eastern ") || region.startsWith("Western ") || region.startsWith("Inner ") || region.startsWith("Outer ") || region.startsWith("Upper ") || region.startsWith("Lower ")) {
				regionVariants.add(region.substring(region.indexOf(' ')).trim());
				if ((regionTypeProper != null) && (french <= english))
					regionVariants.add(region.substring(region.indexOf(' ')).trim() + " " + regionTypeProper);
				if ((regionTypeTranslationProper != null) && (french <= english))
					regionVariants.add(region.substring(region.indexOf(' ')).trim() + " " + regionTypeTranslationProper);
			}
		}
		
		if (regionTypeProper != null) {
			if (french <= english)
				regionVariants.add(region + " " + regionTypeProper);
			else {
				regionVariants.add(regionTypeProper + " " + region);
				if (regionStem.length() == 0) {
					if (region.startsWith("Nord") || region.startsWith("Sud") || region.startsWith("Centre"))
						regionVariants.add(regionTypeProper + " du " + region);
					else if (region.startsWith("Est") || region.startsWith("Ouest"))
						regionVariants.add(regionTypeProper + " de l'" + region);
				}
			}
		}
		
		if ((regionTypeTranslationProper != null) && (french <= english))
			regionVariants.add(region + " " + regionTypeTranslationProper);
		
		if (regionType != null)
			regionVariants.remove(regionType);
		if (regionTypeTranslation != null)
			regionVariants.remove(regionTypeTranslation);
		
		return ((String[]) regionVariants.toArray(new String[regionVariants.size()]));
	}
	
	private static String getProperForm(String str) {
		if (str == null)
			return null;
		StringBuffer pstr = new StringBuffer();
		for (int c = 0; c < str.length(); c++) {
			if ((c == 0) || (str.charAt(c-1) < 33))
				pstr.append(Character.toUpperCase(str.charAt(c)));
			else pstr.append(str.charAt(c));
		}
		return pstr.toString();
	}
	
	private static String normalizeKey(String str) {
		StringBuffer nStr = new StringBuffer();
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			if (ch != ',')
				nStr.append(StringUtils.getNormalForm(ch));
		}
		return nStr.toString();
	}
	
	private static String normalizeString(String str) {
		StringBuffer nStr = new StringBuffer();
		for (int c = 0; c < str.length(); c++)
			nStr.append(StringUtils.getNormalForm(str.charAt(c)));
		return nStr.toString();
	}
	
	private static String wpBaseUrl = "http://en.wikipedia.org/wiki/";
	
	private static String wikipediaTranslate(String term, String lang) throws IOException {
		TreeMap translations = getWikipediaTranslations(term);
		return ((String) translations.get(lang));
	}
	
	private static TreeMap getWikipediaTranslations(String term) throws IOException {
		final String original = term;
		final TreeMap translations = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		if (term.indexOf(' ') != -1)
			term = term.replaceAll("\\s+", "_");
		//	TODO prepare term
		BufferedReader wr = new BufferedReader(new InputStreamReader((new URL(wpBaseUrl + term)).openStream(), "UTF-8"));
		htmlParser.stream(wr, new TokenReceiver() {
			boolean inData = false;
			boolean inTitle = false;
			public void storeToken(String token, int treeDepth) throws IOException {
				if (html.isTag(token)) {
					String type = html.getType(token);
					if ("div".equals(type)) {
						if (html.isEndTag(token))
							this.inData = false;
						else {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
							if ("p-lang".equals(tnas.getAttribute("id")))
								this.inData = true;
						}
					}
					else if ("title".equals(type))
						this.inTitle = !html.isEndTag(token);
					else if (!this.inData)
						return;
					else if ("a".equals(type) && !html.isEndTag(token)) {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
						String href = tnas.getAttribute("href");
//						System.out.println("HREF: " + href);
						String title = tnas.getAttribute("title");
//						System.out.println("TITLE :" + title);
						if ((href == null) || (title == null) || (href.indexOf("/wiki/") == -1))
							return;
						String[] titleParts = title.split("\\s+[\\-\\u2012\\u2013\\u2014\\u2015]\\s+");
						if (titleParts.length != 2)
							return;
						String translation = href.substring(href.indexOf("/wiki/") + "/wiki/".length());
						translation = URLDecoder.decode(translation, "UTF-8").trim();
						translation = translation.replaceAll("[\\_]+", " ");
						String language = titleParts[1].trim();
						System.out.println(original + " -" + language + "-> " + translation);
						translations.put(language, translation);
					}
				}
				else if (this.inTitle) {
					String title = IoTools.prepareForPlainText(token).trim();
					title = title.replaceAll("\\s+[\\-\\u2012\\u2013\\u2014\\u2015]\\s+Wikipedia.*", "");
					if (!original.equals(title))
						translations.put("English", title);
				}
			}//<a href="//it.wikipedia.org/wiki/ISO_3166-1" title="ISO 3166-1 – Italian" lang="it" hreflang="it">Italiano</a>
			public void close() throws IOException {}
		});
		wr.close();
		return translations;
	}
	
	private static TreeMap getColonyData() throws IOException {
		final TreeMap colonyData = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		File colonyDataFile = new File(dataPath, "countries.colonial.xml");
		if (colonyDataFile.exists()) {
			BufferedReader cidr = new BufferedReader(new InputStreamReader(new FileInputStream(colonyDataFile), "UTF-8"));
			xmlParser.stream(cidr, new TokenReceiver() {
				public void storeToken(String token, int treeDepth) throws IOException {
					token = token.trim();
					if (xml.isTag(token) && "country".equals(xml.getType(token))) {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xml);
						String countryName = tnas.getAttribute("name");
						String colonyName = tnas.getAttribute("colonial");
						if ((colonyName != null) && (countryName != null))
							colonyData.put(colonyName, countryName);
					}
				}
				public void close() throws IOException {}
			});
			cidr.close();
		}
		else {
			final TreeSet currentCountryNames = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			TreeMap countryIsoData = getCountryIsoData(true);
			currentCountryNames.addAll(countryIsoData.keySet());
			final TreeMap resolvedCountryNames = new TreeMap(String.CASE_INSENSITIVE_ORDER);
			for (Iterator cnit = currentCountryNames.iterator(); cnit.hasNext();) {
				String cn = ((String) cnit.next());
				resolvedCountryNames.put(cn, cn);
			}
			TreeMap formerCountryNames = getFormerCountryNames();
			for (Iterator cnit = formerCountryNames.keySet().iterator(); cnit.hasNext();) {
				String cn = ((String) cnit.next());
				resolvedCountryNames.put(cn, cn);
				if (cn.indexOf(" and ") != -1) {
					String[] cnParts = cn.split("\\s+and\\s+");
					for (int p = 0; p < cnParts.length; p++)
						resolvedCountryNames.put(cnParts[p], cn);
				}
				TreeSet fcns = ((TreeSet) formerCountryNames.get(cn));
				for (Iterator fcnit = fcns.iterator(); fcnit.hasNext();) {
					String fcn = ((String) fcnit.next());
					resolvedCountryNames.put(fcn, cn);
					if (fcn.indexOf(" and ") != -1) {
						String[] fcnParts = fcn.split("\\s+and\\s+");
						for (int p = 0; p < fcnParts.length; p++)
							resolvedCountryNames.put(fcnParts[p], cn);
					}
				}
			}
			for (Iterator cnit = currentCountryNames.iterator(); cnit.hasNext();) {
				String cn = ((String) cnit.next());
				if (cn.indexOf(" and ") != -1) {
					String[] cnParts = cn.split("\\s+and\\s+");
					for (int p = 0; p < cnParts.length; p++)
						resolvedCountryNames.put(cnParts[p], cn);
				}
			}
			colonyDataFile.createNewFile();
			final BufferedWriter cdw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(colonyDataFile), "UTF-8"));
			cdw.write("<countries>");
			cdw.newLine();
			BufferedReader cdr = new BufferedReader(new InputStreamReader((new URL(wpBaseUrl + "List_of_former_European_colonies")).openStream(), "UTF-8"));
			htmlParser.stream(cdr, new TokenReceiver() {
				private final boolean debugParse = false;
				boolean nextIsRegion = false;
				boolean nextIsColonialPower = false;
				boolean nextIsColony = false;
				String colonyName = null;
				String colonialPower = null;
				String region = null;
				String colonyLink = null;
				public void storeToken(String token, int treeDepth) throws IOException {
					if (this.debugParse) {
						String t = token.trim();
						if (t.length() != 0)
							System.out.println(t);
					}
					if (html.isTag(token)) {
						String type = html.getType(token);
						if ("span".equals(type) && !html.isEndTag(token)) {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
							if ("mw-headline".equals(tnas.getAttribute("class"))) {
								this.nextIsRegion = true;
								this.region = null;
								this.colonialPower = null;
								this.colonyName = null;
								this.colonyLink = null;
							}
						}
						else if (this.region == null)
							return;
						else if ("p".equals(type) && !html.isEndTag(token)) {
							this.nextIsColonialPower = true;
							this.colonialPower = null;
							this.colonyName = null;
							this.colonyLink = null;
						}
						else if (this.colonialPower == null)
							return;
						else if ("li".equals(type) && !html.isEndTag(token)) {
							this.nextIsColony = true;
							this.colonyName = null;
							this.colonyLink = null;
						}
						else if (this.nextIsColony && "a".equals(type) && !html.isEndTag(token)) {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
							this.colonyLink = tnas.getAttribute("href");
							if (this.colonyLink != null)
								this.colonyLink = this.colonyLink.substring(this.colonyLink.lastIndexOf('/') + 1);
						}
					}
					else {
						token = IoTools.prepareForPlainText(token).trim();
						if (token.length() == 0)
							return;
						if (this.nextIsRegion) {
							this.region = token;
							this.nextIsRegion = false;
							System.out.println("Continent: " + this.region);
						}
						else if (this.nextIsColonialPower) {
							this.colonialPower = token;
							this.nextIsColonialPower = false;
							System.out.println("  Power: " + this.colonialPower);
						}
						else if (this.nextIsColony) {
							this.colonyName = token;
							this.nextIsColony = false;
							String countryName = null;
							System.out.println("   - " + this.colonyName + " (" + this.colonyLink + ")");
							if (currentCountryNames.contains(this.colonyName)) {
								System.out.println("   --> " + this.colonyName + " is the current name");
								countryName = this.colonyName;
							}
							else if (currentCountryNames.contains(this.colonyName + "e")) {
								System.out.println("   --> " + this.colonyName + "e is the current name");
								countryName = this.colonyName + "e";
							}
							else if (currentCountryNames.contains(this.colonyName.replaceAll("[\\-]", " "))) {
								System.out.println("   --> " + this.colonyName + " is present-day " + this.colonyName.replaceAll("[\\-]", " "));
								countryName = this.colonyName.replaceAll("[\\-]", " ");
							}
							else if (currentCountryNames.contains(this.colonyName.replaceAll("[\\-]", " ") + "e")) {
								System.out.println("   --> " + this.colonyName + " is present-day " + this.colonyName.replaceAll("[\\-]", " ") + "e");
								countryName = this.colonyName.replaceAll("[\\-]", " ") + "e";
							}
							else if ((this.colonyLink != null) && currentCountryNames.contains(this.colonyLink.replaceAll("[\\_]", " "))) {
								System.out.println("   --> " + this.colonyName + " is present-day " + this.colonyLink.replaceAll("[\\_]", " "));
								countryName = this.colonyLink.replaceAll("[\\_]", " ");
							}
							else if ((this.colonyLink != null) && currentCountryNames.contains(this.colonyLink.replaceAll("[\\_]", " ") + "e")) {
								System.out.println("   --> " + this.colonyName + " is present-day " + this.colonyLink.replaceAll("[\\_]", " ") + "e");
								countryName = this.colonyLink.replaceAll("[\\_]", " ") + "e";
							}
							else if (resolvedCountryNames.containsKey(this.colonyName)) {
								System.out.println("   --> " + this.colonyName + " is present-day " + resolvedCountryNames.get(this.colonyName));
								countryName = ((String) resolvedCountryNames.get(this.colonyName));
							}
							else if (resolvedCountryNames.containsKey(this.colonyName + "e")) {
								System.out.println("   --> " + this.colonyName + "e is present-day " + resolvedCountryNames.get(this.colonyName));
								countryName = ((String) resolvedCountryNames.get(this.colonyName + "e"));
							}
							else if (resolvedCountryNames.containsKey(this.colonyName.replaceAll("[\\-]", " ") + "e")) {
								System.out.println("   --> " + this.colonyName + "e is present-day " + resolvedCountryNames.get(this.colonyName.replaceAll("[\\-\\_]", " ")));
								countryName = ((String) resolvedCountryNames.get(this.colonyName.replaceAll("[\\-]", " ") + "e"));
							}
							else if ((this.colonyLink != null) && resolvedCountryNames.containsKey(this.colonyLink.replaceAll("[\\_]", " "))) {
								System.out.println("   --> " + this.colonyName + " is present-day " + resolvedCountryNames.get(this.colonyLink.replaceAll("[\\_]", " ")));
								countryName = ((String) resolvedCountryNames.get(this.colonyLink.replaceAll("[\\_]", " ")));
							}
							else if ((this.colonyLink != null) && resolvedCountryNames.containsKey(this.colonyLink.replaceAll("[\\_]", " ") + "e")) {
								System.out.println("   --> " + this.colonyName + " is present-day " + resolvedCountryNames.get(this.colonyLink.replaceAll("[\\_]", " ") + "e"));
								countryName = ((String) resolvedCountryNames.get(this.colonyLink.replaceAll("[\\_]", " ") + "e"));
							}
							else if (this.colonyLink != null) {
								System.out.println("   --> resolving " + this.colonyName + " via its link");
								countryName = resolveColonyName(this.colonyName, this.colonyLink, this.region, currentCountryNames, resolvedCountryNames);
								System.out.println("   --> resolved to " + countryName);
							}
							else System.out.println("   --> cannot resolve " + this.colonyName + ", no link given");
							cdw.write("\t<country name=\"" + countryName + "\" colonial=\"" + this.colonyName + "\" power=\"" + this.colonialPower + "\" region=\"" + this.region + "\" />");
							cdw.newLine();
						}
					}
				}
				public void close() throws IOException {}
			});
			cdw.write("</countries>");
			cdw.newLine();
			cdw.flush();
			cdw.close();
		}
		return colonyData;
	}
	
	private static String resolveColonyName(String colonyName, String colonyLink, String region, final TreeSet currentCountryNames, final TreeMap resolvedCountryNames) throws IOException {
		final String[] countryName = {null};
		BufferedReader cdr = new BufferedReader(new InputStreamReader((new URL(wpBaseUrl + colonyLink)).openStream(), "UTF-8"));
		htmlParser.stream(cdr, new TokenReceiver() {
			private final boolean debugParse = false;
			StringBuffer infoBoxData = null;
			int openTables = 0;
			boolean inText = false;
			String link = null;
			public void storeToken(String token, int treeDepth) throws IOException {
				if (this.debugParse) {
					String t = token.trim();
					if (t.length() != 0)
						System.out.println(t);
				}
				if (html.isTag(token)) {
					String type = html.getType(token);
					if ("table".equals(type)) {
						if (html.isEndTag(token)) {
							this.openTables--;
							if ((this.openTables == 0) && (this.infoBoxData != null)) {
								this.infoBoxData.append(token);
								QueriableAnnotation infoBox = SgmlDocumentReader.readDocument(new CharSequenceReader(this.infoBoxData));
								QueriableAnnotation[] rows = infoBox.getAnnotations("tr");
								for (int r = 0; r < rows.length; r++) {
									if ((TokenSequenceUtils.indexOf(rows[r], "Country") == -1) && !TokenSequenceUtils.concatTokens(rows[r], true, false).startsWith("Today part of"))
										continue;
									System.out.println("     - checking infobox row " + TokenSequenceUtils.concatTokens(rows[r], true, false));
									QueriableAnnotation[] links = rows[r].getAnnotations("a");
									for (int l = 0; l < links.length; l++) {
										System.out.println("       - checking link " + links[l].toXML());
										String linkText = TokenSequenceUtils.concatTokens(links[l], true, false);
										if (currentCountryNames.contains(linkText)) {
											countryName[0] = linkText;
											System.out.println("       -IB-> " + linkText + " is a current country");
										}
										else if (resolvedCountryNames.containsKey(linkText)) {
											countryName[0] = ((String) resolvedCountryNames.get(linkText));
											System.out.println("       -IB-> " + linkText + " is now " + countryName[0]);
										}
										if (countryName[0] != null)
											break;
										String linkTarget = ((String) links[l].getAttribute("href"));
										if ((linkTarget == null) || (linkTarget.indexOf("index.php") != -1))
											continue;
										linkTarget = linkTarget.substring(linkTarget.lastIndexOf('/') + 1);
										linkTarget = URLDecoder.decode(linkTarget, "UTF-8");
										linkTarget = linkTarget.replaceAll("[\\_]", " ");
										if (currentCountryNames.contains(linkTarget)) {
											countryName[0] = linkTarget;
											System.out.println("       -IB-> " + linkTarget + " is a current country");
										}
										else if (resolvedCountryNames.containsKey(linkTarget)) {
											countryName[0] = ((String) resolvedCountryNames.get(linkTarget));
											System.out.println("       -IB-> " + linkTarget + " is now " + countryName[0]);
										}
										if (countryName[0] != null)
											break;
									}
									if (countryName[0] != null)
										break;
								}
								this.infoBoxData = null;
							}
						}
						else {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
							String cssClass = tnas.getAttribute("class");
							if ((cssClass != null) && (cssClass.indexOf("infobox") != -1) && (cssClass.indexOf("geography") != -1) && (cssClass.indexOf("vcard") != -1))
								this.infoBoxData = new StringBuffer(token);
							this.openTables++;
						}
					}
					else if (this.openTables != 0) {
						if (this.infoBoxData != null)
							this.infoBoxData.append(IoTools.prepareForPlainText(token));
						return;
					}
					else if ("p".equals(type))
						this.inText = !html.isEndTag(token);
					else if (!this.inText)
						return;
					else if ("a".equals(type)) {
						if (html.isEndTag(token))
							this.link = null;
						else {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
							this.link = tnas.getAttribute("href");
							if (this.link != null)
								this.link = this.link.substring(this.link.lastIndexOf('/') + 1);
						}
					}
				}
				else if (this.openTables != 0) {
					if (this.infoBoxData != null)
						this.infoBoxData.append(token);
					return;
				}
				else if (countryName[0] != null)
					return;
//				THIS IS JUST TOO UNRELIABLE AND ERROR PRONE
//				else if (this.link != null) {
//					token = IoTools.prepareForPlainText(token).trim();
//					if (currentCountryNames.contains(token)) {
//						countryName[0] = token;
//						System.out.println("       --> " + token + " is a current country");
//					}
//					else if (resolvedCountryNames.containsKey(token)) {
//						countryName[0] = ((String) resolvedCountryNames.get(token));
//						System.out.println("       --> " + token + " is now " + countryName[0]);
//					}
//					else if (currentCountryNames.contains(this.link.replaceAll("[\\_]", " "))) {
//						countryName[0] = this.link.replaceAll("[\\_]", " ");
//						System.out.println("       --> " + this.link + " is a current country");
//					}
//					else if (resolvedCountryNames.containsKey(this.link.replaceAll("[\\_]", " "))) {
//						countryName[0] = ((String) resolvedCountryNames.get(this.link.replaceAll("[\\_]", " ")));
//						System.out.println("       --> " + this.link + " is now " + countryName[0]);
//					}
//				}
			}
			public void close() throws IOException {}
		});
		return countryName[0];
	}
	
	private static TreeMap getCountryIsoData(final boolean mapAltNames) throws IOException {
		final TreeMap countryIsoData = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		File countryIsoDataFile = new File(dataPath, "countries.iso.xml");
		if (countryIsoDataFile.exists()) {
			BufferedReader cidr = new BufferedReader(new InputStreamReader(new FileInputStream(countryIsoDataFile), "UTF-8"));
			xmlParser.stream(cidr, new TokenReceiver() {
				public void close() throws IOException {}
				public void storeToken(String token, int treeDepth) throws IOException {
					token = token.trim();
					if (xml.isTag(token) && "country".equals(xml.getType(token))) {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xml);
						String cName = tnas.getAttribute("name");
						if (cName == null)
							return;
						TreeMap cIsoData = new TreeMap(String.CASE_INSENSITIVE_ORDER);
						String cAlpha2 = tnas.getAttribute("alpha2");
						if (cAlpha2 != null)
							cIsoData.put("alpha2", cAlpha2);
						String cAlpha3 = tnas.getAttribute("alpha3");
						if (cAlpha3 != null)
							cIsoData.put("alpha3", cAlpha3);
						String cNum = tnas.getAttribute("num");
						if (cNum != null)
							cIsoData.put("num", cNum);
						countryIsoData.put(cName, cIsoData);
						String cAltNames = tnas.getAttribute("altNames");
						if (cAltNames != null) {
							String[] altNames = cAltNames.split("\\;");
							TreeSet cAltNameSet = new TreeSet(String.CASE_INSENSITIVE_ORDER);
							for (int a = 0; a < altNames.length; a++)
								if (!countryIsoData.containsKey(altNames[a])) {
									if (mapAltNames)
										countryIsoData.put(altNames[a], cIsoData);
									cAltNameSet.add(altNames[a]);
								}
							cIsoData.put("altNames", cAltNameSet);
						}
						String cDisplayName = tnas.getAttribute("displayName");
						if (cDisplayName != null)
							cIsoData.put("displayName", cDisplayName);
						String fsp = tnas.getAttribute("formerlySovereignParts");
						if (fsp != null) {
							String[] fsps = fsp.split("\\;");
							TreeSet fspSet = new TreeSet(String.CASE_INSENSITIVE_ORDER);
							for (int a = 0; a < fsps.length; a++)
								if (!countryIsoData.containsKey(fsps[a])) {
									if (mapAltNames)
										countryIsoData.put(fsps[a], cIsoData);
									fspSet.add(fsps[a]);
								}
							cIsoData.put("formerlySovereignParts", fspSet);
						}
					}
				}
			});
			cidr.close();
		}
		else {
			countryIsoDataFile.createNewFile();
			final BufferedWriter cidw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(countryIsoDataFile), "UTF-8"));
			cidw.write("<countries>");
			cidw.newLine();
			BufferedReader cidr = new BufferedReader(new InputStreamReader((new URL(wpBaseUrl + "ISO_3166-1")).openStream(), "UTF-8"));
			htmlParser.stream(cidr, new TokenReceiver() {
				boolean inData = false;
				boolean inCountryData = false;
				boolean inSortKey = false;
				int tdCount = 0;
				String countryName = null;
				String altCountryName = null;
				String countryLink = null;
				String countryAlpha2 = null;
				String countryAlpha3 = null;
				String countryNum = null;
				public void storeToken(String token, int treeDepth) throws IOException {
					if (html.isTag(token)) {
						String type = html.getType(token);
						if ("table".equals(type)) {
							if (html.isEndTag(token))
								this.inData = false;
							else {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
								if ("wikitable sortable".equals(tnas.getAttribute("class")))
									this.inData = true;
							}
						}
						else if (!this.inData)
							return;
						else if ("tr".equals(type)) {
							if (html.isEndTag(token)) {
								if (this.countryName != null) {
									LinkedList altCountryNames = new LinkedList();
									this.addAltCountryNames(this.countryName, altCountryNames);
									if (this.altCountryName != null) {
										altCountryNames.add(this.altCountryName);
										this.addAltCountryNames(this.altCountryName, altCountryNames);
									}
									TreeSet altCountryNameSet = new TreeSet(String.CASE_INSENSITIVE_ORDER);
									altCountryNameSet.addAll(altCountryNames);
									altCountryNameSet.remove(this.countryName);
									altCountryNames.clear();
									altCountryNames.addAll(altCountryNameSet);
									TreeMap countryData = new TreeMap(String.CASE_INSENSITIVE_ORDER);
									cidw.write("\t<country name=\"" + xml.escape(this.countryName) + "\"");
									if (this.countryAlpha2 != null) {
										cidw.write(" alpha2=\"" + xml.escape(this.countryAlpha2) + "\"");
										countryData.put("alpha2", this.countryAlpha2);
									}
									if (this.countryAlpha3 != null) {
										cidw.write(" alpha3=\"" + xml.escape(this.countryAlpha3) + "\"");
										countryData.put("alpha3", this.countryAlpha3);
									}
									if (this.countryNum != null) {
										cidw.write(" num=\"" + xml.escape(this.countryNum) + "\"");
										countryData.put("num", this.countryNum);
									}
									if (altCountryNames.size() != 0) {
										cidw.write(" altNames=\"");
										TreeSet cAltNameSet = new TreeSet(String.CASE_INSENSITIVE_ORDER);
										while (altCountryNames.size() != 0) {
											String altCountryName = ((String) altCountryNames.removeFirst());
											if (altCountryName.startsWith("the "))
												altCountryNames.addFirst(altCountryName.substring("the ".length()));
											if (Character.isLowerCase(altCountryName.charAt(0)))
												altCountryName = (Character.toUpperCase(altCountryName.charAt(0)) + altCountryName.substring(1));
											cidw.write(xml.escape(altCountryName) + (altCountryNames.isEmpty() ? "" : ";"));
											if (!countryIsoData.containsKey(altCountryName)) {
												if (mapAltNames)
													countryIsoData.put(altCountryName, countryData);
												cAltNameSet.add(altCountryName);
											}
										}
										cidw.write("\"");
										countryData.put("altNames", cAltNameSet);
									}
									cidw.write("/>");
									cidw.newLine();
									cidw.flush();
									countryIsoData.put(this.countryName, countryData);
								}
								this.inCountryData = false;
								this.countryName = null;
								this.altCountryName = null;
								this.countryLink = null;
								this.countryAlpha2 = null;
								this.countryAlpha3 = null;
								this.countryNum = null;
							}
							else {
								this.inCountryData = true;
								this.tdCount = 0;
							}
						}
						else if (!this.inCountryData)
							return;
						else if ("td".equals(type)) {
							if (!html.isEndTag(token))
								this.tdCount++;
						}
						else if ((this.tdCount == 1) && "a".equals(type) && !html.isEndTag(token)) {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
							this.countryLink = tnas.getAttribute("href");
							if (this.countryLink != null)
								this.countryLink = this.countryLink.substring(this.countryLink.lastIndexOf('/') + 1);
						}
						else if ("span".equals(type)) {
							if (html.isEndTag(token))
								this.inSortKey = false;
							else {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
								if ("sortkey".equals(tnas.getAttribute("class")))
									this.inSortKey = true;
							}
						}
					}
					else if (!this.inCountryData || this.inSortKey)
						return;
					else if (this.tdCount == 1) {
						token = IoTools.prepareForPlainText(token);
						token = token.trim();
						if (token.length() == 0)
							return;
						if (this.countryName == null)
							this.countryName = token;
						else this.countryName = (this.countryName + ((this.countryName.endsWith("(") || token.startsWith(",") || token.startsWith(")")) ? "" : " ") + token);
						if (this.countryLink != null)
							this.altCountryName = getWikipediaTitle(this.countryLink);
					}
					else if (this.tdCount == 2) {
						token = IoTools.prepareForPlainText(token);
						token = token.trim();
						if (token.length() == 0)
							return;
						if (this.countryAlpha2 == null)
							this.countryAlpha2 = token;
						else this.countryAlpha2 = (this.countryAlpha2 + ((this.countryAlpha2.endsWith("(") || token.startsWith(",") || token.startsWith(")")) ? "" : " ") + token);
					}
					else if (this.tdCount == 3) {
						token = IoTools.prepareForPlainText(token);
						token = token.trim();
						if (token.length() == 0)
							return;
						if (this.countryAlpha3 == null)
							this.countryAlpha3 = token;
						else this.countryAlpha3 = (this.countryAlpha3 + ((this.countryAlpha3.endsWith("(") || token.startsWith(",") || token.startsWith(")")) ? "" : " ") + token);
					}
					else if (this.tdCount == 4) {
						token = IoTools.prepareForPlainText(token);
						token = token.trim();
						if (token.length() == 0)
							return;
						if (this.countryNum == null)
							this.countryNum = token;
						else this.countryNum = (this.countryNum + ((this.countryNum.endsWith("(") || token.startsWith(",") || token.startsWith(")")) ? "" : " ") + token);
					}
				}
				public void close() throws IOException {}
				private void addAltCountryNames(String countryName, LinkedList altCountryNames) {
					if (countryName.indexOf(" and ") == -1) {
						String[] countryNameParts = countryName.split("\\s*\\,\\s*");
						if (countryNameParts.length == 2) {
							altCountryNames.add(countryNameParts[1] + " " + countryNameParts[0]);
							if (countryNameParts[1].matches("U\\.\\s*S\\.")) {
								altCountryNames.add(countryNameParts[0] + ", United States");
								altCountryNames.add("United States " + countryNameParts[0]);
							}
							else if (countryNameParts[1].endsWith(" of") || (countryNameParts[1].indexOf(" of ") != -1))
								altCountryNames.add(countryNameParts[0]);
						}
					}
					else {
						String[] countryNameParts = countryName.split("\\s*(\\,|(\\sand\\s))\\s*");
						for (int p = 0; p < countryNameParts.length; p++)
							altCountryNames.add(countryNameParts[p]);
					}
				}
			});
			cidw.write("</countries>");
			cidw.newLine();
			cidw.flush();
			cidw.close();
		}
		return countryIsoData;
	}
	
	private static String getWikipediaTitle(String countryLink) throws IOException {
		System.out.println("Getting Wikipedia title for " + countryLink);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ie) {}
		final String[] wikipediaTitle = {null};
		try {
			BufferedReader wfr = new BufferedReader(new InputStreamReader((new URL(wpBaseUrl + countryLink)).openStream(), "UTF-8"));
			htmlParser.stream(wfr, new TokenReceiver() {
				private boolean inTitle = false;
				public void storeToken(String token, int treeDepth) throws IOException {
					if (html.isTag(token)) {
						if ("title".equals(html.getType(token)))
							this.inTitle = !html.isEndTag(token);
					}
					else if (this.inTitle) {
						wikipediaTitle[0] = IoTools.prepareForPlainText(token).trim();
						throw new RuntimeException("DONE");
					}
				}
				public void close() throws IOException {}
			});
			wfr.close();
		}
		catch (RuntimeException re) {
			if (!"DONE".equals(re.getMessage()))
				throw re;
		}
		catch (IOException ioe) {
			if (ioe instanceof ConnectException) {
				System.out.println(" ==> timeout, retrying in 5 seconds");
				try {
					Thread.sleep(1000 * 5);
				} catch (InterruptedException ie) {}
				return getWikipediaTitle(countryLink);
			}
			else throw ioe;
		}
		if ((wikipediaTitle[0] != null) && (wikipediaTitle[0].indexOf("Wikipedia") != -1)) {
			wikipediaTitle[0] = wikipediaTitle[0].substring(0, wikipediaTitle[0].indexOf("Wikipedia"));
			while ((wikipediaTitle[0].length() != 0) && !Character.isLetter(wikipediaTitle[0].charAt(wikipediaTitle[0].length() - 1)))
				wikipediaTitle[0] = wikipediaTitle[0].substring(0, (wikipediaTitle[0].length() - 1));
		}
		System.out.println(" ==> " + wikipediaTitle[0]);
		return wikipediaTitle[0];
	}
	
	private static TreeMap getFormerCountryNames() throws IOException {
		final TreeMap formerCountryNameList = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		File formerCountryFile = new File(dataPath, "countries.renamed.xml");
		if (formerCountryFile.exists()) {
			BufferedReader fcnr = new BufferedReader(new InputStreamReader(new FileInputStream(formerCountryFile), "UTF-8"));
			xmlParser.stream(fcnr, new TokenReceiver() {
				private String countryName = null;
				private TreeSet countryNames = null;
				private boolean nextIsCountryName = false;
				public void storeToken(String token, int treeDepth) throws IOException {
					token = token.trim();
					if (xml.isTag(token)) {
						String type = xml.getType(token);
						if ("name".equals(type)) {
							if (xml.isEndTag(token))
								this.nextIsCountryName = false;
							else if (this.countryName != null)
								this.nextIsCountryName = true;
						}
						else if ("country".equals(type)) {
							if (xml.isEndTag(token)) {
								if ((this.countryName != null) && (this.countryNames != null))
									formerCountryNameList.put(this.countryName, this.countryNames);
								this.countryName = null;
								this.countryNames = null;
							}
							else {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xml);
								this.countryName = tnas.getAttribute("name");
								this.countryNames = new TreeSet(String.CASE_INSENSITIVE_ORDER);
							}
						}
					}
					else if (this.nextIsCountryName) {
						this.nextIsCountryName = false;
						if (this.countryName == null)
							return;
						this.countryNames.add(xml.unescape(token).trim());
					}
				}
				public void close() throws IOException {}
			});
			fcnr.close();
		}
		else {
			formerCountryFile.createNewFile();
			final BufferedWriter fcnw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(formerCountryFile), "UTF-8"));
			fcnw.write("<countries>");
			fcnw.newLine();
			BufferedReader fcnr = new BufferedReader(new InputStreamReader((new URL(wpBaseUrl + "Geographical_renaming")).openStream(), "UTF-8"));
			htmlParser.stream(fcnr, new TokenReceiver() {
				private final boolean debugParse = false;
				boolean inData = false;
				String countryNameString = null;
				public void storeToken(String token, int treeDepth) throws IOException {
					if (this.debugParse) {
						String t = token.trim();
						if (t.length() != 0)
							System.out.println(t);
					}
					if (html.isTag(token)) {
						String type = html.getType(token);
						if (this.debugParse) {
							System.out.println(" ==> " + type);
						}
						if ("span".equals(type) && !html.isEndTag(token)) {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
							if ("Countries".equals(tnas.getAttribute("id")))
								this.inData = true;
						}
						else if ("ul".equals(type) && html.isEndTag(token))
							this.inData = false;
						else if (!this.inData)
							return;
						else if ("li".equals(type)) {
							if (html.isEndTag(token)) {
								if (this.countryNameString != null) {
									this.countryNameString = this.countryNameString.trim();
									this.countryNameString = this.countryNameString.replaceAll("\\s*\\([0-9a-z][^\\)]+\\)\\s*", " ");
									String[] countryNameStringParts = this.countryNameString.split("\\s*[\\u2192\\(\\)]\\s*");
									String countryName = null;
									TreeSet formerCountryNames = new TreeSet(String.CASE_INSENSITIVE_ORDER);
									for (int p = 0; p < countryNameStringParts.length; p++) {
										countryNameStringParts[p] = countryNameStringParts[p].trim();
										countryNameStringParts[p] = countryNameStringParts[p].replaceAll("[^A-Za-z]+\\z", "");
										countryNameStringParts[p] = countryNameStringParts[p].replaceAll("\\A[^A-Za-z]+", "");
										countryNameStringParts[p] = countryNameStringParts[p].trim();
										if (countryNameStringParts[p].length() == 0)
											continue;
										countryName = countryNameStringParts[p];
										formerCountryNames.add(countryNameStringParts[p]);
									}
									if (countryName != null) {
										String[] countryNames = null;
										if ((countryName.indexOf(',') == -1) || ((countryName.indexOf(" of ") != -1) && (countryName.indexOf(" of ") < countryName.indexOf(',')))) {
											countryNames = new String[1];
											countryNames[0] = countryName;
										}
										else countryNames = countryName.split("\\s*\\,\\s*");
										for (int c = 0; c < countryNames.length; c++) {
											fcnw.write("\t<country name=\"" + xml.escape(countryNames[c]) + "\">");
											fcnw.newLine();
											for (Iterator cnit = formerCountryNames.iterator(); cnit.hasNext();) {
												String cn = ((String) cnit.next());
												fcnw.write("\t\t<name>" + xml.escape(cn) + "</name>");
												fcnw.newLine();
											}
											fcnw.write("\t</country>");
											fcnw.newLine();
											fcnw.flush();
											formerCountryNameList.put(countryNames[c], formerCountryNames);
										}
									}
								}
								this.countryNameString = null;
							}
							else this.countryNameString = "";
						}
					}
					else if (!this.inData)
						return;
					else if (this.countryNameString != null) {
						token = IoTools.prepareForPlainText(token);
						token = token.trim();
						if (token.length() == 0)
							return;
						this.countryNameString = (this.countryNameString + ((this.countryNameString.endsWith("(") || token.startsWith(",") || token.startsWith(")")) ? "" : " ") + token);
					}
				}
				public void close() throws IOException {}
			});
			fcnw.write("</countries>");
			fcnw.newLine();
			fcnw.flush();
			fcnw.close();
		}
		return formerCountryNameList;
	}
	
	private static TreeMap getLangCountryNames() throws IOException {
		TreeMap langCountryList = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		addLangCountryNames("List_of_country_names_in_various_languages_(A–C)", langCountryList);
		addLangCountryNames("List_of_country_names_in_various_languages_(D-I)", langCountryList);
		addLangCountryNames("List_of_country_names_in_various_languages_(J-P)", langCountryList);
		addLangCountryNames("List_of_country_names_in_various_languages_(Q-Z)", langCountryList);
		addLangCountryNames("List_of_alternative_country_names", langCountryList);
		return langCountryList;
	}
	private static void addLangCountryNames(String wikiPageName, final TreeMap langCountryList) throws IOException {
		File langCountryFile = new File(dataPath, "countries.langs." + wikiPageName.hashCode() + ".xml");
		if (langCountryFile.exists()) {
			BufferedReader lcnr = new BufferedReader(new InputStreamReader(new FileInputStream(langCountryFile), "UTF-8"));
			xmlParser.stream(lcnr, new TokenReceiver() {
				private String countryName = null;
				private String languages = null;
				private TreeMap countryNames = null;
				private boolean nextIsCountryName = false;
				public void storeToken(String token, int treeDepth) throws IOException {
					token = token.trim();
					if (xml.isTag(token)) {
						String type = xml.getType(token);
						if ("name".equals(type)) {
							if (xml.isEndTag(token))
								this.nextIsCountryName = false;
							else if (this.countryName != null) {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xml);
								this.nextIsCountryName = true;
								this.languages = tnas.getAttribute("languages");
							}
						}
						else if ("country".equals(type)) {
							if (xml.isEndTag(token)) {
								if ((this.countryName != null) && (this.countryNames != null)) {
									TreeMap listCountryNames = ((TreeMap) langCountryList.get(this.countryName));
									if (listCountryNames == null)
										langCountryList.put(this.countryName, this.countryNames);
									else for (Iterator lcnit = this.countryNames.keySet().iterator(); lcnit.hasNext();) {
										String lcn = ((String) lcnit.next());
										TreeSet lcnLangs = ((TreeSet) this.countryNames.get(lcn));
										TreeSet listLcnLangs = ((TreeSet) listCountryNames.get(lcn));
										if (listLcnLangs == null)
											listCountryNames.put(lcn, lcnLangs);
										else listLcnLangs.addAll(lcnLangs);
									}
								}
								this.countryName = null;
								this.countryNames = null;
							}
							else {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xml);
								this.countryName = tnas.getAttribute("name");
								this.countryNames = new TreeMap(String.CASE_INSENSITIVE_ORDER);
							}
						}
					}
					else if (this.nextIsCountryName) {
						this.nextIsCountryName = false;
						if ((this.countryName == null) || (this.languages == null))
							return;
						String countryName = xml.unescape(token.trim());
						String[] countryNames = {countryName};
						if (countryName.indexOf(';') != -1)
							countryNames = countryName.split("\\s*\\;\\s*");
						for (int n = 0; n < countryNames.length; n++) {
							TreeSet countryNameLangs = ((TreeSet) this.countryNames.get(countryNames[n]));
							if (countryNameLangs == null) {
								countryNameLangs = new TreeSet(String.CASE_INSENSITIVE_ORDER);
								this.countryNames.put(countryNames[n], countryNameLangs);
							}
							countryNameLangs.addAll(Arrays.asList(this.languages.split("\\s*\\,\\s*")));
						}
					}
				}
				public void close() throws IOException {}
			});
			lcnr.close();
		}
		else {
			langCountryFile.createNewFile();
			final BufferedWriter lcnw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(langCountryFile), "UTF-8"));
			lcnw.write("<countries source=\"" + wikiPageName + "\">");
			lcnw.newLine();
			BufferedReader lcnr = new BufferedReader(new InputStreamReader((new URL(wpBaseUrl + wikiPageName)).openStream(), "UTF-8"));
			htmlParser.stream(lcnr, new TokenReceiver() {
				private final boolean debugParse = false;
				boolean inData = false;
				String countryName = null;
				TreeMap flCountryNames = new TreeMap();
				int tdCount = 0;
				boolean inFlCountryName = false;
				String flCountryName = null;
				String flCountryNameLanguages = null;
				public void storeToken(String token, int treeDepth) throws IOException {
					if (this.debugParse) {
						String t = token.trim();
						if (t.length() != 0)
							System.out.println(t);
					}
					if (html.isTag(token)) {
						String type = html.getType(token);
						if (this.debugParse)
							System.out.println(" ==> " + type + ", td-count is " + this.tdCount);
						if ("table".equals(type)) {
							if (html.isEndTag(token)) {
								this.inData = false;
								this.countryName = null;
								this.flCountryNames.clear();
							}
							else {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
								String cssClass = tnas.getAttribute("class");
								if ((cssClass == null) || ((cssClass.indexOf("navbox") == -1) && (cssClass.indexOf("nowraplinks") == -1)))
									this.inData = true;
							}
						}
						else if (!this.inData)
							return;
						else if ("tr".equals(type)) {
							if (html.isEndTag(token))
								this.storeData();
							this.tdCount = 0;
							this.countryName = null;
							this.flCountryName = null;
							this.flCountryNameLanguages = null;
							this.flCountryNames.clear();
						}
						else if ("td".equals(type)) {
							if (html.isEndTag(token)) {
								this.storeFlData();
								this.flCountryNameLanguages = null;
							}
							else this.tdCount++;
						}
						else if ("small".equals(type) && (this.tdCount == 2)) {
							this.storeFlData();
							this.flCountryName = null;
							this.flCountryNameLanguages = null;
						}
						else if ("p".equals(type) && (this.tdCount == 2)) {
							this.storeFlData();
							this.flCountryName = null;
							this.flCountryNameLanguages = null;
						}
						else if ("b".equals(type) && (this.tdCount == 2)) {
							if (html.isEndTag(token)) {
								this.inFlCountryName = false;
								this.flCountryNameLanguages = null;
							}
							else {
								this.storeFlData();
								this.inFlCountryName = true;
								this.flCountryName = null;
								this.flCountryNameLanguages = null;
							}
						}
					}
					else if (!this.inData)
						return;
					else if (this.tdCount == 1) {
						token = IoTools.prepareForPlainText(token);
						token = token.trim();
						if (token.length() == 0)
							return;
						if (token.length() == 0)
							return;
						if (this.countryName == null)
							this.countryName = html.unescape(token);
						else this.countryName = (this.countryName + ((this.countryName.endsWith("(") || token.startsWith(")")) ? "" : " ") + token);
						System.out.println("ENGLISH COUNTRY NAME: " + this.countryName);
					}
					else if (this.tdCount == 2) {
						token = IoTools.prepareForPlainText(token.trim());
						if (this.inFlCountryName) {
							if (this.flCountryName == null)
								this.flCountryName = token;
							else this.flCountryName = (this.flCountryName + ((this.flCountryName.endsWith("(") || token.startsWith(")")) ? "" : " ") + token);
							System.out.println("  OTHER COUNTRY NAME: " + this.flCountryName);
						}
						else if (this.flCountryName != null) {
							if (this.flCountryNameLanguages == null)
								this.flCountryNameLanguages = token;
							else this.flCountryNameLanguages = (this.flCountryNameLanguages + ((this.flCountryNameLanguages.endsWith("(") || token.startsWith(",") || token.startsWith(")")) ? "" : " ") + token);
						}
					}
				}
				private void storeFlData() {
					if ((this.countryName == null) || (this.flCountryName == null) || (this.flCountryNameLanguages == null))
						return;
					while (this.flCountryNameLanguages.indexOf('(') != -1) {
						this.flCountryNameLanguages = this.flCountryNameLanguages.substring(this.flCountryNameLanguages.indexOf('(') + 1).trim();
						if (this.flCountryNameLanguages.indexOf(')') != -1) {
							String fls = this.flCountryNameLanguages.substring(0, this.flCountryNameLanguages.indexOf(')')).trim();
							System.out.println("'" + this.flCountryName + "' means '" + this.countryName + "' in " + fls);
							if (this.flCountryNames.containsKey(this.flCountryName))
								fls = (((String) this.flCountryNames.get(this.flCountryName)) + ", " + fls);
							this.flCountryNames.put(this.flCountryName, fls);
							this.flCountryNameLanguages = this.flCountryNameLanguages.substring(this.flCountryNameLanguages.indexOf(')') + 1).trim();
						}
					}	
				}
				private void storeData() throws IOException {
					if ((this.countryName == null) || this.flCountryNames.isEmpty())
						return;
					String countryName = this.countryName;
					if (countryName.endsWith(")") && (countryName.indexOf('(') != -1))
						countryName = countryName.substring(0, countryName.indexOf('(')).trim();
					lcnw.write("\t<country name=\"" + xml.escape(countryName) + "\">");
					lcnw.newLine();
					TreeMap countryNames = new TreeMap(String.CASE_INSENSITIVE_ORDER);
					if (!this.countryName.equals(countryName)) {
						if (this.flCountryNames.containsKey(this.countryName)) {
							String langs = ((String) this.flCountryNames.get(this.countryName));
							langs = ("English, " + langs);
							this.flCountryNames.put(this.countryName, langs);
						}
						else this.flCountryNames.put(this.countryName, "English");
					}
					for (Iterator cnit = this.flCountryNames.keySet().iterator(); cnit.hasNext();) {
						String cn = ((String) cnit.next());
						String langs = ((String) this.flCountryNames.get(cn));
						lcnw.write("\t\t<name languages=\"" + xml.escape(langs) + "\">" + this.encode(xml.escape(cn)) + "</name>");
						lcnw.newLine();
						TreeSet countryNameLangs = new TreeSet(String.CASE_INSENSITIVE_ORDER);
						countryNameLangs.addAll(Arrays.asList(langs.split("\\s*\\,\\s*")));
						countryNames.put(cn, countryNameLangs);
					}
					lcnw.write("\t</country>");
					lcnw.newLine();
					lcnw.flush();
					TreeMap listCountryNames = ((TreeMap) langCountryList.get(this.countryName));
					if (listCountryNames == null)
						langCountryList.put(this.countryName, countryNames);
					else for (Iterator lcnit = countryNames.keySet().iterator(); lcnit.hasNext();) {
						String lcn = ((String) lcnit.next());
						TreeSet lcnLangs = ((TreeSet) countryNames.get(lcn));
						TreeSet listLcnLangs = ((TreeSet) listCountryNames.get(lcn));
						if (listLcnLangs == null)
							listCountryNames.put(lcn, lcnLangs);
						else listLcnLangs.addAll(lcnLangs);
					}
				}
				private String encode(String s) {
					StringBuffer sb = new StringBuffer();
					for (int c = 0; c < s.length(); c++) {
						char ch = s.charAt(c);
						if ((ch < 256) || !Character.isLetter(ch))
							sb.append(ch);
						else sb.append("&#x" + Integer.toString(((int) ch), 16).toUpperCase() + ";");
					}
					return sb.toString();
				}
				public void close() throws IOException {}
			});
			lcnw.write("</countries>");
			lcnw.newLine();
			lcnw.flush();
			lcnw.close();
		}
	}
	
	private static String wfbBaseUrl = "https://www.cia.gov/library/publications/the-world-factbook/";
	
	private static TreeMap getCountryList() throws IOException {
		final TreeMap countryList = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		File countryListFile = new File(dataPath, "countries.list.xml");
		if (countryListFile.exists()) {
			BufferedReader clr = new BufferedReader(new InputStreamReader(new FileInputStream(countryListFile), "UTF-8"));
			xmlParser.stream(clr, new TokenReceiver() {
				public void storeToken(String token, int treeDepth) throws IOException {
					if (xml.isTag(token) && "country".equals(xml.getType(token))) {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xml);
						String countryName = tnas.getAttribute("name");
						String countryDataLink = tnas.getAttribute("dataLink");
						if ((countryName != null) && (countryDataLink != null))
							countryList.put(countryName, countryDataLink);
					}
				}
				public void close() throws IOException {}
			});
			clr.close();
		}
		else {
			countryListFile.createNewFile();
			final BufferedWriter clw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(countryListFile), "UTF-8"));
			clw.write("<countries>");
			clw.newLine();
			BufferedReader clr = new BufferedReader(new InputStreamReader((new URL(wfbBaseUrl + "rankorder/2147rank.html")).openStream(), "UTF-8"));
			htmlParser.stream(clr, new TokenReceiver() {
				boolean inCountryData = false;
				boolean inCountryName = false;
				String countryName;
				String countryDataLink;
				public void storeToken(String token, int treeDepth) throws IOException {
					token = token.trim();
					if (html.isTag(token)) {
						String type = html.getType(token);
						if ("td".equals(type)) {
							if (html.isEndTag(token))
								this.inCountryData = false;
							else {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
								if ("region".equals(tnas.getAttribute("class")))
									this.inCountryData = true;
							}
						}
						else if (!this.inCountryData)
							return;
						else if ("a".equals(type)) {
							if (html.isEndTag(token)) {
								if ((this.countryName != null) && (this.countryDataLink != null)) {
									clw.write("\t<country name=\"" + xml.escape(this.countryName) + "\" dataLink=\"" + xml.escape(this.countryDataLink.substring("../".length())) + "\"/>");
									clw.newLine();
									System.out.println(this.countryName + ": " + this.countryDataLink);
									countryList.put(countryName, countryDataLink);
								}
								this.inCountryName = false;
								this.countryName = null;
								this.countryDataLink = null;
							}
							else {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
								this.countryName = null;
								this.countryDataLink = tnas.getAttribute("href");
								this.inCountryName = true;
							}
						}
					}
					else if (this.inCountryName)
						this.countryName = token;
				}
				public void close() throws IOException {}
			});
			clw.write("</countries>");
			clw.newLine();
			clw.flush();
			clw.close();
		}
		return countryList;
	}
	
	private static TreeMap getCountryData(String countryName, final String countryDataLink) throws IOException {
		final TreeMap countryData = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		String countryDataFileSuffix = countryDataLink;
		countryDataFileSuffix = countryDataFileSuffix.substring(countryDataFileSuffix.lastIndexOf('/') + 1);
		countryDataFileSuffix = countryDataFileSuffix.substring(0, countryDataFileSuffix.lastIndexOf('.'));
		File countryDataFile = new File(dataPath, "country.data." + countryDataFileSuffix + ".xml");
		if (countryDataFile.exists()) {
			BufferedReader cdr = new BufferedReader(new InputStreamReader(new FileInputStream(countryDataFile), "UTF-8"));
			xmlParser.stream(cdr, new TokenReceiver() {
				String factName;
				public void storeToken(String token, int treeDepth) throws IOException {
					if (xml.isTag(token) && "fact".equals(xml.getType(token))) {
						if (xml.isEndTag(token))
							this.factName = null;
						else {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xml);
							this.factName = tnas.getAttribute("name");
						}
					}
					else if (this.factName != null) {
						if (countryData.containsKey(this.factName)) {
							String value = ((String) countryData.get(this.factName));
							countryData.put((this.factName + "/0"), value);
							countryData.remove(this.factName);
							int vnr = 0;
							while (countryData.containsKey((this.factName + "/" + vnr)))
								vnr++;
							this.factName = (this.factName + "/" + vnr);
						}
						else if (countryData.containsKey(this.factName + "/0")) {
							int vnr = 0;
							while (countryData.containsKey((this.factName + "/" + vnr)))
								vnr++;
							this.factName = (this.factName + "/" + vnr);
						}
						countryData.put(this.factName, xml.unescape(token));
					}
				}
				public void close() throws IOException {}
			});
			cdr.close();
		}
		else {
			countryDataFile.createNewFile();
			final BufferedWriter cdw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(countryDataFile), "UTF-8"));
			cdw.write("<country name=\"" + xml.escape(countryName) + "\">");
			cdw.newLine();
			BufferedReader cdr = new BufferedReader(new InputStreamReader((new URL(wfbBaseUrl + countryDataLink)).openStream(), "UTF-8"));
			htmlParser.stream(cdr, new TokenReceiver() {
				boolean inData = false;
				boolean inCategory = false;
				boolean inKey = false;
				boolean inValue = false;
				String category;
				String key;
				String value;
				public void storeToken(String token, int treeDepth) throws IOException {
					token = token.trim();
					if (html.isTag(token)) {
						String type = html.getType(token);
						if ("div".equals(type)) {
							if (html.isEndTag(token)) {
								if ((this.key != null) && (this.value != null)) {
									if ("value".equals(this.key)) {
										int vnr = 0;
										while (countryData.containsKey((this.category + "/" + this.key + "/" + vnr)))
											vnr++;
										this.key = (this.key + "/" + vnr);
									}
									cdw.write("\t<fact name=\"" + xml.escape(this.category + "/" + this.key) + "\">");
									cdw.write(xml.escape(this.value));
									cdw.write("</fact>");
									cdw.newLine();
									if (countryData.containsKey(this.category + "/" + this.key)) {
										String value = ((String) countryData.get(this.category + "/" + this.key));
										countryData.put((this.category + "/" + this.key + "/0"), value);
										countryData.remove(this.category + "/" + this.key);
										int vnr = 0;
										while (countryData.containsKey((this.category + "/" + this.key + "/" + vnr)))
											vnr++;
										this.key = (this.key + "/" + vnr);
									}
									else if (countryData.containsKey(this.category + "/" + this.key + "/0")) {
										int vnr = 0;
										while (countryData.containsKey((this.category + "/" + this.key + "/" + vnr)))
											vnr++;
										this.key = (this.key + "/" + vnr);
									}
									System.out.println(countryDataLink + " - " + this.category + "/" + this.key + ": " + this.value);
									countryData.put((this.category + "/" + this.key), this.value);
								}
								this.inData = false;
								this.inCategory = false;
								this.inKey = false;
								this.inValue = false;
								this.key = null;
								this.value = null;
							}
							else {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
								if ("category".equals(tnas.getAttribute("class"))) {
									this.inData = true;
									if ("field".equals(tnas.getAttribute("id")))
										this.inCategory = true;
									else this.inKey = true;
								}
								else if ("category_data".equals(tnas.getAttribute("class"))) {
									this.inData = true;
									this.key = "value";
									this.inValue = true;
									this.value = "";
								}
							}
						}
						else if (!this.inData)
							return;
						else if ("span".equals(type)) {
							if (html.isEndTag(token))
								this.inValue = false;
							else {
								this.inKey = false;
								this.inValue = true;
								this.value = "";
							}
						}
					}
					else if (this.inCategory) {
						 if ((token.length() == 0) || token.startsWith(":"))
							 return;
						 if (token.endsWith(":"))
							 token = token.substring(0, (token.length() - ":".length())).trim();
						 this.category = token.replaceAll("[^A-Za-z0-9\\-]+", "_");
					}
					else if (this.inKey) {
						 if (token.endsWith(":"))
							 token = token.substring(0, (token.length() - ":".length())).trim();
						this.key = token.replaceAll("[^A-Za-z0-9\\-]+", "_");
					}
					else if (this.inValue)
						this.value = html.unescape(token);
				}
				public void close() throws IOException {}
			});
			cdw.write("</country>");
			cdw.newLine();
			cdw.flush();
			cdw.close();
		}
		return countryData;
	}

	private static void loadCountryTranslations(String cName, File translationDataFile, TreeMap countries, TreeMap countryLangs, TreeMap countriesTranslated) throws IOException {
		final TreeMap cNameTranslations = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		BufferedReader tdfr = new BufferedReader(new InputStreamReader(new FileInputStream(translationDataFile), "UTF-8"));
		xmlParser.stream(tdfr, new TokenReceiver() {
			String language = null;
			public void storeToken(String token, int treeDepth) throws IOException {
				if (xml.isTag(token)) {
					if ("translation".equals(xml.getType(token)) && !xml.isEndTag(token)) {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xml);
						this.language = tnas.getAttribute("lang");
					}
					else this.language = null;
				}
				else if (this.language != null)
					cNameTranslations.put(this.language, xml.unescape(token));
			}
			public void close() throws IOException {}
		});
		tdfr.close();
		while (countries.containsKey(cName) && !cName.equals(countries.get(cName)))
			cName = ((String) countries.get(cName));
		for (Iterator lit = cNameTranslations.keySet().iterator(); lit.hasNext();) {
			String lang = ((String) lit.next());
			String trans = ((String) cNameTranslations.get(lang));
			String nTrans = normalizeString(trans);
			if (nTrans.startsWith("Geschichte") || nTrans.startsWith("Histoire") || nTrans.startsWith("History") || nTrans.startsWith("Historia") || nTrans.startsWith("Historio") || nTrans.startsWith("Geschiedenis") || nTrans.startsWith("Storia"))
				continue;
			countriesTranslated.put(trans, cName);
			((TreeSet) countryLangs.get(trans)).add(lang);
			if (cName.indexOf(" and ") == -1)
				continue;
			String[] transParts = trans.split("((\\,\\s+)|(\\s+(y|va|und|un|u|tan|sy|si|og|och|na|me|ma|lae|ken|kap|kaj|ja|ir|in|i|he|ha|et|ed|e|doo|dhe|dan|da|ati|as|and|an|ak|agus|a)\\s+))");
			for (int p = 0; p < transParts.length; p++) {
				countriesTranslated.put(transParts[p], cName);
				((TreeSet) countryLangs.get(transParts[p])).add(lang);
			}
		}
	}
	
	private static void downloadCountryTranslations(String cName, File translationDataFile, TreeMap countries, TreeMap countryLangs, TreeMap countriesTranslated) throws IOException {
		try {
			TreeMap cNameTranslations = getWikipediaTranslations(cName);
			translationDataFile.createNewFile();
			BufferedWriter tdw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(translationDataFile), "UTF-8"));
			tdw.write("<term value=\"" + xml.escape(cName) + "\">");
			tdw.newLine();
			for (Iterator lit = cNameTranslations.keySet().iterator(); lit.hasNext();) {
				String lang = ((String) lit.next());
				String lcName = ((String) cNameTranslations.get(lang));
				tdw.write("\t<translation lang=\"" + xml.escape(lang) + "\">" + xml.escape(lcName) + "</translation>");
				tdw.newLine();
			}
			tdw.write("</term>");
			tdw.newLine();
			tdw.flush();
			tdw.close();
			while (countries.containsKey(cName) && !cName.equals(countries.get(cName)))
				cName = ((String) countries.get(cName));
			for (Iterator lit = cNameTranslations.keySet().iterator(); lit.hasNext();) {
				String lang = ((String) lit.next());
				String trans = ((String) cNameTranslations.get(lang));
				String nTrans = normalizeString(trans);
				if (nTrans.startsWith("Geschichte") || nTrans.startsWith("Histoire") || nTrans.startsWith("History") || nTrans.startsWith("Historia") || nTrans.startsWith("Historio") || nTrans.startsWith("Geschiedenis") || nTrans.startsWith("Storia"))
					continue;
				countriesTranslated.put(trans, cName);
				((TreeSet) countryLangs.get(trans)).add(lang);
				if (cName.indexOf(" and ") == -1)
					continue;
				String[] transParts = trans.split("((\\,\\s+)|(\\s+(y|va|und|un|u|tan|sy|si|og|och|na|me|ma|lae|ken|kap|kaj|ja|ir|in|i|he|ha|et|ed|e|doo|dhe|dan|da|ati|as|and|an|ak|agus|a)\\s+))");
				for (int p = 0; p < transParts.length; p++) {
					countriesTranslated.put(transParts[p], cName);
					((TreeSet) countryLangs.get(transParts[p])).add(lang);
				}
			}
		}
		catch (IOException ioe) {
			System.out.println("Could not get translations for '" + cName + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
	}
	
	public static void mainAddCountry(String[] args) throws Exception {
		
	}
}
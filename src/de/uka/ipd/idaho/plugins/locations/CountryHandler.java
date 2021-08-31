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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * This class represents a dictionary of country names in multiple languages,
 * with the additional capability of translating all the country names into
 * modern-day English for normalization.
 * 
 * @author sautter
 */
public class CountryHandler implements Dictionary {
	private static Grammar grammar = new StandardGrammar();
	private static Parser parser = new Parser(grammar);
	
	private TreeMap countryNames = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private TreeMap countryCodes = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private TreeMap countriesByCode = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	private TreeSet languages = null;
	
	private TreeMap fuzzyCountryNames = null;
	
	private final boolean isDefaultInstance;
	private AnalyzerDataProvider dataProvider = null;
	
	private CountryHandler(boolean idi) {
		this.isDefaultInstance = idi;
	}
	private void readCountryData(Reader countryData, String[] langs) throws IOException {
		if (langs != null) {
			this.languages = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int l = 0; l < langs.length; l++) {
				if (langs[l] != null)
					this.languages.add(langs[l]);
			}
		}
		BufferedReader br = ((countryData instanceof BufferedReader) ? ((BufferedReader) countryData) : new BufferedReader(countryData));
		parser.stream(br, new TokenReceiver() {
			public void close() throws IOException {}
			private String countryName = null;
			private String countryCode2 = null;
			private String countryCode3 = null;
			private boolean nextIsCountryName = false;
			public void storeToken(String token, int treeDepth) throws IOException {
				if (grammar.isTag(token)) {
					String type = grammar.getType(token);
					
					//	country name in some language
					if ("name".equals(type)) {
						if (grammar.isEndTag(token))
							this.nextIsCountryName = false;
						else if (this.countryName != null) {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
							
							//	apply language filter if given
							if (languages != null) {
								String langsString = tnas.getAttribute("languages");
								if (langsString == null)
									return;
								if ((langsString.indexOf("English") == -1) && (langsString.indexOf("initialism") == -1) && (langsString.indexOf("WFB") == -1) && (langsString.indexOf("ISO") == -1)) {
									String[] langs = langsString.split("\\s*\\;\\s*");
									boolean langFilterOut = true;
									for (int l = 0; l < langs.length; l++)
										if (languages.contains(langs[l])) {
											langFilterOut = false;
											break;
										}
									if (langFilterOut)
										return;
								}
							}
							
							//	remember next content token is country name
							this.nextIsCountryName = true;
							
							//	store normalized version(s) of country name to come
							String nCountryNameString = tnas.getAttribute("normalized");
							if (nCountryNameString == null)
								return;
							String[] nCountryNames = nCountryNameString.split("\\s*\\;\\s*");
							for (int n = 0; n < nCountryNames.length; n++) {
								countryNames.put(nCountryNames[n], this.countryName);
								countryNames.put(normalize(nCountryNames[n]), this.countryName);
								if (this.countryCode3.length() == 3) {
									countryCodes.put(nCountryNames[n], this.countryCode3);
									countryCodes.put(normalize(nCountryNames[n]), this.countryCode3);
								}
							}
						}
					}
					
					//	start or end of country data
					else if ("country".equals(type)) {
						if (grammar.isEndTag(token)) {
							this.countryName = null;
							this.countryCode2 = null;
							this.countryCode3 = null;
						}
						else {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
							this.countryName = tnas.getAttribute("name");
							this.countryCode2 = tnas.getAttribute("iso2", "");
							this.countryCode3 = tnas.getAttribute("iso3", "");
							countryNames.put(this.countryName, this.countryName);
							if (this.countryCode2.length() == 2)
								countriesByCode.put(this.countryCode2, this.countryName);
							if (this.countryCode3.length() == 3)
								countriesByCode.put(this.countryCode3, this.countryName);
						}
					}
				}
				
				//	un-normalized country name
				else if (this.nextIsCountryName) {
					this.nextIsCountryName = false;
					if (this.countryName == null)
						return;
					String unCountryNameString = grammar.unescape(token.trim());
					String[] unCountryNames = unCountryNameString.split("\\s*\\;\\s*");
					for (int n = 0; n < unCountryNames.length; n++) {
						countryNames.put(unCountryNames[n], this.countryName);
						countryNames.put(normalize(unCountryNames[n]), this.countryName);
						if (this.countryCode3.length() == 3) {
							countryCodes.put(unCountryNames[n], this.countryCode3);
							countryCodes.put(normalize(unCountryNames[n]), this.countryCode3);
						}
					}
				}
			}
		});
		br.close();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#getEntryIterator()
	 */
	public StringIterator getEntryIterator() {
		final Iterator it = this.countryNames.keySet().iterator();
		return new StringIterator() {
			public boolean hasNext() {
				return it.hasNext();
			}
			public boolean hasMoreStrings() {
				return this.hasNext();
			}
			public Object next() {
				return it.next();
			}
			public String nextString() {
				return ((String) it.next());
			}
			public void remove() {}
		};
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isDefaultCaseSensitive()
	 */
	public boolean isDefaultCaseSensitive() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String, boolean)
	 */
	public boolean lookup(String string, boolean caseSensitive) {
		if ((string == null) || !hasCapitalLetter(string))
			return false;
		if (this.countryNames.containsKey(string))
			return true;
		if (this.fuzzyCountryNames == null)
			return false;
		return this.fuzzyCountryNames.containsKey(getFuzzyString(string));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String)
	 */
	public boolean lookup(String string) {
		return this.lookup(string, this.isDefaultCaseSensitive());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isEmpty()
	 */
	public boolean isEmpty() {
		return (this.size() == 0);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.stringUtils.Dictionary#size()
	 */
	public int size() {
		return this.countryNames.size();
	}
	
	/**
	 * Check if fuzzy lookup is enabled. If this method returns true, several
	 * groups of characters are treated as identical on lookup, namely (B, P),
	 * (C, G, K), (D, T), (I, J, Y) and (S, Z), double consonants are conflated,
	 * and punctuation marks and whitespace are ignored.
	 * @return true if fuzzy lookup is enabled, false otherwise
	 */
	public boolean isFuzzyLookupEnabled() {
		return (this.fuzzyCountryNames != null);
	}
	
	/**
	 * Enable or disable fuzzy lookup. Enabling fuzzy lookup conflates several
	 * groups of characters, namely (B, P), (C, G, K), (D, T), (I, J, Y) and
	 * (S, Z), it conflates double consonants, and ignores punctuation marks
	 * and whitespace. However, this applies only if it does not incur
	 * collisions or ambiguities.
	 * @param fle enable fuzzy lookup?
	 */
	public void setFuzzyLookupEnabled(boolean fle) {
		if (fle == (this.fuzzyCountryNames != null))
			return;
		if (!fle) {
			this.fuzzyCountryNames = null;
			return;
		}
		this.fuzzyCountryNames = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		for (Iterator cnit = this.countryNames.keySet().iterator(); cnit.hasNext();) {
			String cName = ((String) cnit.next()).toLowerCase();
			String fcName = getFuzzyString(cName);
			if (((fcName.length() * 4) >= (cName.replaceAll("[\\-\\,\\&\\(\\)\\s]", "").length() * 3)) && !this.countryNames.containsKey(fcName))
				this.fuzzyCountryNames.put(fcName, this.countryNames.get(cName));
		}
	}
	
	/**
	 * Retrieve the English name of a given country. If there is no English name
	 * for the argument country, this method returns the argument country name
	 * itself.
	 * @param country the country to look up
	 * @return the English name of the country
	 */
	public String getEnglishName(String country) {
		if (country == null)
			return null;
		String eCountry = ((String) this.countryNames.get(country));
		if (eCountry == null)
			eCountry = ((String) this.countryNames.get(normalize(country)));
		if ((eCountry == null) && (this.fuzzyCountryNames != null))
			eCountry = ((String) this.fuzzyCountryNames.get(getFuzzyString(country)));
		return ((eCountry == null) ? country : eCountry);
	}
	
	/**
	 * Retrieve the 3-letter ISO 3166 code of a given country. If there is no
	 * such code for the argument country, this method returns null.
	 * @param country the country to look up
	 * @return the ISO 3166 code of the country
	 */
	public String getIsoCode(String country) {
		if (country == null)
			return null;
		String code = ((String) this.countryCodes.get(country));
		if (code == null)
			code = ((String) this.countryCodes.get(normalize(country)));
		if ((code == null) && (this.fuzzyCountryNames != null)) {
			country = ((String) this.fuzzyCountryNames.get(getFuzzyString(country)));
			code = ((String) this.countryCodes.get(country));
		}
		return code;
	}
	
	/**
	 * Retrieve the country for a given 2- or 3-letter ISO 3166 code. If there
	 * is no country for the argument code, i.e., the argument string is not a
	 * valid ISO 3166 code, this method returns null.
	 * @param code the ISO 3166 code to look up
	 * @return the country identified by the argument code
	 */
	public String getIsoCountry(String code) {
		return ((code == null) ? null : ((String) this.countriesByCode.get(code)));
	}
	
	/**
	 * Obtain a region handler for a specific country. This method only works
	 * if the country handler was created from a file or using an Analyzer Data
	 * Provider, as these two include a path to load region data from. It also
	 * works on default instances, which use the built-in data. Otherwise, this
	 * method throws an UnsupportedOperationException.
	 * @param country the country to obtain the region handler for
	 * @return the region handler
	 * @throws UnsupportedOperationException if the country handler was
	 *          instantiated from a reader or input stream
	 */
	public RegionHandler getRegionHandler(String country) throws UnsupportedOperationException {
		return this.getRegionHandler(country, null);
	}
	
	/**
	 * Obtain a region handler for a specific country. This method only works
	 * if the country handler was created from a file or using an Analyzer Data
	 * Provider, as these two include a path to load region data from. It also
	 * works on default instances, which use the built-in data. Otherwise, this
	 * method throws an UnsupportedOperationException.
	 * @param country the country to obtain the region handler for
	 * @return the region handler
	 * @throws UnsupportedOperationException if the country handler was
	 *          instantiated from a reader or input stream
	 */
	public RegionHandler getRegionHandler(String country, String[] languages) throws UnsupportedOperationException {
		String code = this.getIsoCode(country);
		if (code == null)
			code = ("NI-" + country.replaceAll("[^A-Z]", ""));
		if (this.isDefaultInstance) try {
			RegionHandler rh = new RegionHandler();
			String rhdrn = CountryHandler.class.getName().replaceAll("\\.", "/");
			InputStream rhdis = CountryHandler.class.getClassLoader().getResourceAsStream(rhdrn.substring(0, rhdrn.lastIndexOf('/')) + "/countryData/regions." + code.toLowerCase() + ".xml");
			if (rhdis == null) {
				System.out.println("Region handler data for country " + country + " (code " + code + ") unavailable.");
				return null;
			}
			rh.readRegionData(new InputStreamReader(rhdis, "UTF-8"), languages);
			rhdis.close();
			return rh;
		}
		catch (Exception e) {
			System.out.println("Could not initialize region handler for country " + country + " (code " + code + "): " + e.getMessage());
			e.printStackTrace(System.out);
			return null;
		}
		if (this.dataProvider != null) try {
			RegionHandler rh = new RegionHandler();
			InputStream rhdis = this.dataProvider.getInputStream("regions." + code.toLowerCase() + ".xml");
			rh.readRegionData(new InputStreamReader(rhdis, "UTF-8"), languages);
			rhdis.close();
			return rh;
		}
		catch (IOException ioe) {
			System.out.println("Could not initialize region handler: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
		throw new UnsupportedOperationException("Region handlers are only available if the country handler was generated from a file or Analyzer Data Provider, or from a default instance.");
	}
	
	/**
	 * This class represents a dictionary containing the names of the regions
	 * (states, provinces, districts, etc.) of a country in multiple languages,
	 * with the additional capability of translating all the country names into
	 * modern-day English for normalization.
	 * 
	 * @author sautter
	 */
	public static class RegionHandler implements Dictionary {
		
		private TreeMap regionNames = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		private TreeMap regionTypes = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		private TreeSet languages = null;
		private RegionHandler() {}
		private void readRegionData(Reader regionData, String[] langs) throws IOException {
			if (langs != null) {
				this.languages = new TreeSet(String.CASE_INSENSITIVE_ORDER);
				for (int l = 0; l < langs.length; l++) {
					if (langs[l] != null)
						this.languages.add(langs[l]);
				}
			}
			BufferedReader br = ((regionData instanceof BufferedReader) ? ((BufferedReader) regionData) : new BufferedReader(regionData));
			parser.stream(br, new TokenReceiver() {
				public void close() throws IOException {}
				private String regionName = null;
				private String regionType = null;
				private String regionTypeTranslated = null;
				private boolean nextIsRegionName = false;
				public void storeToken(String token, int treeDepth) throws IOException {
					if (grammar.isTag(token)) {
						String type = grammar.getType(token);
						
						//	region name in some language
						if ("name".equals(type)) {
							if (grammar.isEndTag(token))
								this.nextIsRegionName = false;
							else if (this.regionName != null) {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
								
								//	apply language filter if given
								if (languages != null) {
									String langsString = tnas.getAttribute("languages");
									if (langsString == null)
										return;
									if ((langsString.indexOf("English") == -1) && (langsString.indexOf("initialism") == -1) && (langsString.indexOf("WFB") == -1) && (langsString.indexOf("ISO") == -1)) {
										String[] langs = langsString.split("\\s*\\;\\s*");
										boolean langFilterOut = true;
										for (int l = 0; l < langs.length; l++)
											if (languages.contains(langs[l])) {
												langFilterOut = false;
												break;
											}
										if (langFilterOut)
											return;
									}
								}
								
								//	remember next content token is country name
								this.nextIsRegionName = true;
								
								//	store normalized version(s) of country name to come
								String nRegionNameString = tnas.getAttribute("normalized");
								if (nRegionNameString == null)
									return;
								String[] nRegionNames = nRegionNameString.split("\\s*\\;\\s*");
								for (int n = 0; n < nRegionNames.length; n++) {
									regionNames.put(nRegionNames[n], this.regionName);
									regionNames.put(normalize(nRegionNames[n]), this.regionName);
									regionTypes.put(nRegionNames[n], this.regionType);
									regionTypes.put(normalize(nRegionNames[n]), this.regionType);
									regionTypes.put(("LOCAL-" + nRegionNames[n]), this.regionTypeTranslated);
									regionTypes.put(("LOCAL-" + normalize(nRegionNames[n])), this.regionTypeTranslated);
								}
							}
						}
						
						//	start or end of country data
						else if ("region".equals(type)) {
							if (grammar.isEndTag(token)) {
								this.regionName = null;
								this.regionType = null;
								this.regionTypeTranslated = null;
							}
							else {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
								this.regionName = tnas.getAttribute("name");
								this.regionType = tnas.getAttribute("type", "");
								this.regionTypeTranslated = tnas.getAttribute("typeTranslated", "");
								regionNames.put(this.regionName, this.regionName);
							}
						}
					}
					
					//	un-normalized country name
					else if (this.nextIsRegionName) {
						this.nextIsRegionName = false;
						if (this.regionName == null)
							return;
						String unRegionNameString = grammar.unescape(token.trim());
						String[] unRegionNames = unRegionNameString.split("\\s*\\;\\s*");
						for (int n = 0; n < unRegionNames.length; n++) {
							regionNames.put(unRegionNames[n], this.regionName);
							regionNames.put(normalize(unRegionNames[n]), this.regionName);
							regionTypes.put(unRegionNames[n], this.regionType);
							regionTypes.put(normalize(unRegionNames[n]), this.regionType);
							regionTypes.put(("LOCAL-" + unRegionNames[n]), this.regionTypeTranslated);
							regionTypes.put(("LOCAL-" + normalize(unRegionNames[n])), this.regionTypeTranslated);
						}
					}
				}
			});
			br.close();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#getEntryIterator()
		 */
		public StringIterator getEntryIterator() {
			final Iterator it = this.regionNames.keySet().iterator();
			return new StringIterator() {
				public boolean hasNext() {
					return it.hasNext();
				}
				public boolean hasMoreStrings() {
					return this.hasNext();
				}
				public Object next() {
					return it.next();
				}
				public String nextString() {
					return ((String) it.next());
				}
				public void remove() {}
			};
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isDefaultCaseSensitive()
		 */
		public boolean isDefaultCaseSensitive() {
			return false;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String, boolean)
		 */
		public boolean lookup(String string, boolean caseSensitive) {
			return ((string != null) && this.regionNames.containsKey(string) && hasCapitalLetter(string));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String)
		 */
		public boolean lookup(String string) {
			return this.lookup(string, this.isDefaultCaseSensitive());
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#isEmpty()
		 */
		public boolean isEmpty() {
			return (this.size() == 0);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.stringUtils.Dictionary#size()
		 */
		public int size() {
			return this.regionNames.size();
		}
		
		/**
		 * Retrieve the English name of a given region. If there is no English name
		 * for the argument region, this method returns the argument region name
		 * itself.
		 * @param region the region to look up
		 * @return the English name of the region
		 */
		public String getEnglishName(String region) {
			if (region == null)
				return null;
			String eRegion = ((String) this.regionNames.get(region));
			if (eRegion == null)
				eRegion = ((String) this.regionNames.get(normalize(region)));
			return ((eRegion == null) ? region : eRegion);
		}
		
		/**
		 * Retrieve the type of a given region, e.g. 'state', 'province', or
		 * 'district'. If there is no type for the argument region, this method
		 * returns null.
		 * @param region the region to look up
		 * @return the type of the argument region in English
		 */
		public String getType(String region) {
			if (region == null)
				return null;
			String code = ((String) this.regionTypes.get(region));
			if (code == null)
				code = ((String) this.regionTypes.get(normalize(region)));
			return code;
		}
		
		/**
		 * Retrieve the type of a given region in the country's local language,
		 * e.g. 'oblast' for Russian provinces. If there is no type for the
		 * argument region, this method returns null.
		 * @param region the region to look up
		 * @return the type of the argument region in the local language
		 */
		public String getLocalType(String region) {
			if (region == null)
				return null;
			String code = ((String) this.regionTypes.get(region));
			if (code == null)
				code = ((String) this.regionTypes.get(normalize(region)));
			return code;
		}
	}
	
	private static String normalize(String str) {
		StringBuffer sb = new StringBuffer();
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			if ((ch > 127) && Character.isLetter(ch))
				sb.append(StringUtils.getNormalForm(ch));
			else sb.append(ch);
		}
		return sb.toString();
	}
	
	private static boolean hasCapitalLetter(String str) {
		for (int c = 0; c < str.length(); c++) {
			if (Character.isUpperCase(str.charAt(c)))
				return true;
		}
		return false;
	}
	private static String getFuzzyString(String str) {
		str = normalize(str.toLowerCase());
		str = str.replaceAll("[^a-z]", ""); // remove whitespace and punctuation marks
		str = str.replaceAll("[bp]", "b"); // conflate B and P
		str = str.replaceAll("[cgk]", "c"); // conflate C, G, and K
		str = str.replaceAll("[dt]", "d"); // conflate D and T
		str = str.replaceAll("[ijy]", "i"); // conflate I, J, and Y
		str = str.replaceAll("[sz]", "s"); // conflate S and Z
		StringBuffer sb = new StringBuffer();
		for (int c = 0; c < str.length(); c++) {
			if ((c == 0) || (str.charAt(c-1) != str.charAt(c)))
				sb.append(str.charAt(c));
		}
		return sb.toString();
	}
	
	private static HashMap instances = new HashMap();
	
	/**
	 * Initialize a country handler form a data provider. If there already is an
	 * instance for the argument data provider, this instance is returned
	 * without a new one being created. If the argument data provider is null,
	 * this method returns the default instance of this class, populated with
	 * the built-in data.
	 * @param adp the data provider to build on
	 * @return the country handler
	 */
	public static CountryHandler getCountryHandler(AnalyzerDataProvider adp) {
		return getCountryHandler(adp, "countries.xml", null);
	}
	
	/**
	 * Initialize a country handler form a data provider. If there already is an
	 * instance for the argument data provider, this instance is returned
	 * without a new one being created. If the argument data provider is null,
	 * this method returns the default instance of this class, populated with
	 * the built-in data. If the argument language list is null, data for all
	 * languages is included. Otherwise, only country names associated with the
	 * specified languages is included. English names are always included, no
	 * matter if specified in a non-null language list or not.
	 * @param adp the data provider to build on
	 * @param an array holding the languages to include
	 * @return the country handler
	 */
	public static CountryHandler getCountryHandler(AnalyzerDataProvider adp, String[] languages) {
		return getCountryHandler(adp, "countries.xml", languages);
	}
	
	/**
	 * Initialize a country handler form a data provider using a custom data
	 * item. If there already is an instance for the argument data provider
	 * and item name, this instance is returned without a new one being created.
	 * If the argument data provider or data name is null, this method returns
	 * the default instance of this class, populated with the built-in data.
	 * @param adp the data provider to build on
	 * @param countryDataName the name of the data object to use
	 * @return the country handler
	 */
	public static CountryHandler getCountryHandler(AnalyzerDataProvider adp, String countryDataName) {
		return getCountryHandler(adp, countryDataName, null);
	}
	
	/**
	 * Initialize a country handler form a data provider using a custom data
	 * item. If there already is an instance for the argument data provider
	 * and item name, this instance is returned without a new one being created.
	 * If the argument data provider or data name is null, this method returns
	 * the default instance of this class, populated with the built-in data. If
	 * the argument language list is null, data for all languages is included.
	 * Otherwise, only country names associated with the specified languages is
	 * included. English names are always included, no matter if specified in a
	 * non-null language list or not.
	 * @param adp the data provider to build on
	 * @param countryDataName the name of the data object to use
	 * @param an array holding the languages to include
	 * @return the country handler
	 */
	public static CountryHandler getCountryHandler(AnalyzerDataProvider adp, String countryDataName, String[] languages) {
		if ((adp == null) || (countryDataName == null))
			return getDefaultInstance(languages);
		String chCacheKey = getCacheKey((adp.getAbsolutePath() + "/" + countryDataName), languages);
		CountryHandler ch = ((CountryHandler) instances.get(chCacheKey));
		if (ch == null) try {
			ch = getCountryHandler(adp.getInputStream(countryDataName), languages);
			ch.dataProvider = adp;
			instances.put(chCacheKey, ch);
		}
		catch (IOException ioe) {
			System.out.println("Could not initialize country handler: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		return ch;
	}
	
	/**
	 * Initialize a country handler form a data provider. If there already is an
	 * instance for the argument data file, this instance is returned without a
	 * new one being created. This method expects the file content to be encoded
	 * in UTF-8. If the argument file is null, this method returns the default
	 * instance of this class, populated with the built-in data.
	 * @param countryData the data file to build on
	 * @return the country handler
	 */
	public static CountryHandler getCountryHandler(File countryData) {
		return getCountryHandler(countryData, null);
	}
	
	/**
	 * Initialize a country handler form a data provider. If there already is an
	 * instance for the argument data file, this instance is returned without a
	 * new one being created. This method expects the file content to be encoded
	 * in UTF-8. If the argument file is null, this method returns the default
	 * instance of this class, populated with the built-in data. If the argument
	 * language list is null, data for all languages is included. Otherwise,
	 * only country names associated with the specified languages is included.
	 * English names are always included, no matter if specified in a non-null
	 * language list or not.
	 * @param countryData the data file to build on
	 * @param an array holding the languages to include
	 * @return the country handler
	 */
	public static CountryHandler getCountryHandler(File countryData, String[] languages) {
		if (countryData == null)
			return getDefaultInstance(languages);
		else return getCountryHandler(new AnalyzerDataProviderFileBased(countryData.getParentFile()), countryData.getName(), languages);
	}
	
	/**
	 * Initialize a country handler form a data input stream. This method will
	 * always create a new instance, as input streams cannot be identified by
	 * their underlying source in the general case. This method expects the data
	 * coming from the argument reader to be encoded in UTF-8. If the argument
	 * input stream is null, this method returns the default instance of this
	 * class, populated with the built-in data.
	 * @param countryData the stream providing the data to build on
	 * @return the country handler
	 */
	public static CountryHandler getCountryHandler(InputStream countryData) {
		return getCountryHandler(countryData, null);
	}
	
	/**
	 * Initialize a country handler form a data input stream. This method will
	 * always create a new instance, as input streams cannot be identified by
	 * their underlying source in the general case. This method expects the data
	 * coming from the argument reader to be encoded in UTF-8. If the argument
	 * input stream is null, this method returns the default instance of this
	 * class, populated with the built-in data. If the argument language list
	 * is null, data for all languages is included. Otherwise, only country
	 * names associated with the specified languages is included. English names
	 * are always included, no matter if specified in a non-null language list
	 * or not.
	 * @param countryData the stream providing the data to build on
	 * @param an array holding the languages to include
	 * @return the country handler
	 */
	public static CountryHandler getCountryHandler(InputStream countryData, String[] languages) {
		if (countryData == null)
			return getDefaultInstance(languages);
		else return getCountryHandler(countryData, languages, false);
	}
	private static CountryHandler getCountryHandler(InputStream countryData, String[] languages, boolean idi) {
		if (countryData == null)
			return getDefaultInstance(languages);
		else try {
			return getCountryHandler(new InputStreamReader(countryData, "UTF-8"), languages, idi);
		}
		catch (IOException ioe) {
			System.out.println("Could not initialize country handler: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
	}
	
	/**
	 * Initialize a country handler form a data reader. This method will always
	 * create a new instance, as reader cannot be identified by their underlying
	 * source in the general case. If the argument reader is null, this method
	 * returns the default instance of this class, populated with the built-in
	 * data.
	 * @param countryData the reader providing the data to build on
	 * @return the country handler
	 */
	public static CountryHandler getCountryHandler(Reader countryData) {
		return getCountryHandler(countryData, null);
	}
	
	/**
	 * Initialize a country handler form a data reader. This method will always
	 * create a new instance, as reader cannot be identified by their underlying
	 * source in the general case. If the argument reader is null, this method
	 * returns the default instance of this class, populated with the built-in
	 * data. If the argument language list is null, data for all languages is
	 * included. Otherwise, only country names associated with the specified
	 * languages is included. English names are always included, no matter if
	 * specified in a non-null language list or not.
	 * @param countryData the reader providing the data to build on
	 * @param an array holding the languages to include
	 * @return the country handler
	 */
	public static CountryHandler getCountryHandler(Reader countryData, String[] languages) {
		if (countryData == null)
			return getDefaultInstance(languages);
		else return getCountryHandler(countryData, languages, false);
	}
	private static CountryHandler getCountryHandler(Reader countryData, String[] languages, boolean idi) {
		if (countryData == null)
			return getDefaultInstance(languages);
		else try {
			CountryHandler ch = new CountryHandler(idi);
			ch.readCountryData(countryData, languages);
			return ch;
		}
		catch (IOException ioe) {
			System.out.println("Could not initialize country handler: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
	}
	
	private static CountryHandler getDefaultInstance(String[] languages) {
		String defaultInstanceCacheKey = getCacheKey(null, languages);
		CountryHandler defaultInstance = ((CountryHandler) instances.get(defaultInstanceCacheKey));
		if (defaultInstance == null) {
			String chdrn = CountryHandler.class.getName().replaceAll("\\.", "/");
			InputStream chdis = CountryHandler.class.getClassLoader().getResourceAsStream(chdrn.substring(0, chdrn.lastIndexOf('/')) + "/countryData/countries.xml");
			defaultInstance = getCountryHandler(chdis, languages, true);
			instances.put(defaultInstanceCacheKey, defaultInstance);
		}
		return defaultInstance;
	}
	
	private static String getCacheKey(String dataKey, String[] languages) {
		StringBuffer ck = new StringBuffer((dataKey == null) ? "default" : dataKey);
		if (languages == null)
			return ck.toString();
		TreeSet languagesSorted = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int l = 0; l < languages.length; l++) {
			if (languages[l] != null)
				languagesSorted.add(languages[l]);
		}
		if (languagesSorted.isEmpty())
			return ck.toString();
		for (Iterator lit = languagesSorted.iterator(); lit.hasNext();) {
			ck.append("|");
			ck.append((String) lit.next());
		}
		return ck.toString();
	}
	
	private static String[] testLanguages = {
		"English",
		"French",
		"German",
		"Italian",
		"Portuguese",
		"Russian",
		"Spanish",
	};
	
	//	UN-COMMENT THIS MAIN METHOD FOR TESTING
	public static void main(String[] args) {
//		CountryHandler ch = CountryHandler.getCountryHandler(new AnalyzerDataProviderFileBased(new File("E:/Projektdaten/CountryData/")));
		CountryHandler ch = CountryHandler.getCountryHandler((AnalyzerDataProvider) null, testLanguages);
//		StringIterator cit = ch.getEntryIterator();
//		while (cit.hasMoreStrings())
//			System.out.println(cit.nextString());
		
		RegionHandler rh = ch.getRegionHandler("Germany");
//		StringIterator rit = ch.getEntryIterator();
//		while (rit.hasMoreStrings())
//			System.out.println(rit.nextString());
		System.out.println(rh.getEnglishName("Bayern"));
	}
}
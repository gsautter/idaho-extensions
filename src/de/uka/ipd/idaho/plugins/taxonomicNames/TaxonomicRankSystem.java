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
package de.uka.ipd.idaho.plugins.taxonomicNames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * A system of taxonomic ranks, in correspondence with the various
 * nomenclatorial codes. Secondary ranks are organized in groups around primary
 * ones.
 * 
 * @author sautter
 */
public class TaxonomicRankSystem {
	
	/** the name of the rank system */
	public final String name;
	
	/** the acronym of the nomenclatorial code the rank system corresponds to / the commission reigning the code, e.g. 'ICZN' or 'ICBN' */
	public final String code;
	
	/** the taxonomic domain the rank system belongs to (e.g. 'Zoology' or 'Botany') */
	public final String domain;
	
	ArrayList rankGroups = new ArrayList();
	ArrayList ranks = new ArrayList();
	
	TaxonomicRankSystem(String name, String code, String domain) {
		this.name = name;
		this.code = code;
		this.domain = domain;
	}
	
	/**
	 * Retrieve a rank by its name. If the rank system does not include a rank
	 * with the specified name, this method returns null.
	 * @param name the name of the rank
	 * @return the rank with the specified name
	 */
	public Rank getRank(String name) {
		if (name == null)
			return null;
		for (int r = 0; r < this.ranks.size(); r++) {
			if (name.equalsIgnoreCase(((Rank) this.ranks.get(r)).name))
				return ((Rank) this.ranks.get(r));
		}
		return null;
	}
	
	/**
	 * Retrieve all ranks present in the rank system, ordered by significance,
	 * from least to most. Client code may freely modify the returned array, as
	 * a new one is allocated and filled for each invocation of this method.
	 * @return an array holding the ranks
	 */
	public Rank[] getRanks() {
		return ((Rank[]) this.ranks.toArray(new Rank[this.ranks.size()]));
	}
	
	/**
	 * Retrieve the names of all ranks present in the rank system, ordered by
	 * significance, from least to most. Client code may freely modify the
	 * returned array, as a new one is allocated and filled for each invocation
	 * of this method.
	 * @return an array holding the rank names
	 */
	public String[] getRankNames() {
		String[] rankNames = new String[this.ranks.size()];
		for (int r = 0; r < this.ranks.size(); r++)
			rankNames[r] = ((Rank) this.ranks.get(r)).name;
		return rankNames;
	}
	
	/**
	 * Retrieve a rank group by its name. If the rank system does not include a
	 * rank group with the specified name, this method returns null.
	 * @param name the name of the rank group
	 * @return the rank group with the specified name
	 */
	public RankGroup getRankGroup(String name) {
		if (name == null)
			return null;
		for (int r = 0; r < this.rankGroups.size(); r++) {
			if (name.equalsIgnoreCase(((RankGroup) this.rankGroups.get(r)).name))
				return ((RankGroup) this.rankGroups.get(r));
		}
		return null;
	}
	
	/**
	 * Retrieve all rank groups present in the rank system, ordered by
	 * significance, from least to most. Client code may freely modify the
	 * returned array, as a new one is allocated and filled for each invocation
	 * of this method.
	 * @return an array holding the rank groups
	 */
	public RankGroup[] getRankGroups() {
		return ((RankGroup[]) this.rankGroups.toArray(new RankGroup[this.rankGroups.size()]));
	}
	
	/**
	 * Retrieve the names of all rank groups present in the rank system, ordered
	 * by significance, from least to most. Client code may freely modify the
	 * returned array, as a new one is allocated and filled for each invocation
	 * of this method.
	 * @return an array holding the rank group names
	 */
	public String[] getRankGroupNames() {
		String[] rankGroupNames = new String[this.ranks.size()];
		for (int r = 0; r < this.rankGroups.size(); r++)
			rankGroupNames[r] = ((RankGroup) this.rankGroups.get(r)).name;
		return rankGroupNames;
	}
	
	/**
	 * An individual rank.
	 * 
	 * @author sautter
	 */
	public static class Rank {
		
		/** the name of the rank */
		public final String name;
		
		TaxonomicRankSystem system;
		RankGroup group;
		
		int relativeSignificance = -1;
		
		String suffix = "";
		
		String[] abbreviations;
		
		String epithetTemplate = "@epithet";
		
		Rank(String name) {
			this.name = name;
		}
		
		/**
		 * Retrieve the rank group the rank belongs to.
		 * @return the rank group
		 */
		public RankGroup getRankGroup() {
			return this.group;
		}
		
		/**
		 * Retrieve the rank system the rank belongs to.
		 * @return the rank system
		 */
		public TaxonomicRankSystem getRankSystem() {
			return this.system;
		}
		
		/**
		 * Indicates whether or not the rank is a primary one.
		 * @return true for primary ranks, false for others
		 */
		public boolean isPrimary() {
			return this.name.equals(this.group.name);
		}
		
		/**
		 * Retrieve the relative significance of the rank within a rank system.
		 * @return the relative significance
		 */
		public int getRelativeSignificance() {
			if (this.relativeSignificance == -1) {
				for (int r = 0; r < this.system.ranks.size(); r++)
					if (this.name.equals(((Rank) this.system.ranks.get(r)).name)) {
						this.relativeSignificance = (r+1);
						break;
					}
			}
			return this.relativeSignificance;
		}
		
		/**
		 * Retrieve the suffix epithets of the rank must have according to the
		 * underlying code. If the underlying code does not specify a suffix,
		 * this method returns the empty string, but never null.
		 * @return the suffix
		 */
		public String getSuffix() {
			return this.suffix;
		}
		
		/**
		 * Retrieve the primary abbreviation of the rank.
		 * @return the primary abbreviation
		 */
		public String getAbbreviation() {
			return this.abbreviations[0];
		}
		
		/**
		 * Retrieve all abbreviations of the rank. Client code may freely modify
		 * the returned array, as a new one is allocated and filled for each
		 * invocation of this method.
		 * @return an array holding the abbreviations
		 */
		public String[] getAbbreviations() {
			String[] abbreviations = new String[this.abbreviations.length];
			System.arraycopy(this.abbreviations, 0, abbreviations, 0, this.abbreviations.length);
			return abbreviations;
		}
		
		/**
		 * Format an epithet of the rank, e.g. by adding punctuation marks, etc.
		 * @param epithet the raw epithet to format
		 * @return the formatted epithet
		 */
		public String formatEpithet(String epithet) {
			return (((epithet == null) || (epithet.trim().length() == 0)) ? "" : this.epithetTemplate.replaceAll("\\@epithet", epithet));
		}
	}
	
	/**
	 * A group of ranks, i.e., a primary rank, whose name is also the name of
	 * the group, together with surrounding secondary ranks.
	 * 
	 * @author sautter
	 */
	public static class RankGroup {
		
		/** the name of the rank */
		public final String name;
		
		TaxonomicRankSystem system;
		ArrayList ranks = new ArrayList();
		
		RankGroup(String name) {
			this.name = name;
		}
		
		/**
		 * Retrieve a rank by its name. If the rank group does not include a
		 * rank with the specified name, this method returns null.
		 * @param name the name of the rank
		 * @return the rank with the specified name
		 */
		public Rank getRank(String name) {
			if (name == null)
				return null;
			for (int r = 0; r < this.ranks.size(); r++) {
				if (name.equalsIgnoreCase(((Rank) this.ranks.get(r)).name))
					return ((Rank) this.ranks.get(r));
			}
			return null;
		}
		
		/**
		 * Retrieve all ranks present in the rank group, ordered by
		 * significance, from least to most. Client code may freely modify the
		 * returned array, as a new one is allocated and filled for each
		 * invocation of this method.
		 * @return an array holding the ranks
		 */
		public Rank[] getRanks() {
			return ((Rank[]) this.ranks.toArray(new Rank[this.ranks.size()]));
		}
		
		/**
		 * Retrieve the names of all ranks present in the rank group, ordered by
		 * significance, from least to most. Client code may freely modify the
		 * returned array, as a new one is allocated and filled for each
		 * invokation of this method.
		 * @return an array holding the rank names
		 */
		public String[] getRankNames() {
			String[] rankNames = new String[this.ranks.size()];
			for (int r = 0; r < this.ranks.size(); r++)
				rankNames[r] = ((Rank) this.ranks.get(r)).name;
			return rankNames;
		}
		
		/**
		 * Retrieve the rank system the rank group belongs to.
		 * @return the rank system
		 */
		public TaxonomicRankSystem getRankSystem() {
			return this.system;
		}
	}
	
	private static Properties rankSystemNamesToCodes = new Properties();
	private static Properties rankSystemNamesToDomains = new Properties();
	private static Properties rankSystemNameSynonyms = new Properties();
	static {
		rankSystemNamesToCodes.setProperty("animalia", "ICZN");
		rankSystemNamesToCodes.setProperty("plantae", "ICN");
		rankSystemNamesToCodes.setProperty("bacteriae", "ICNB");
		rankSystemNamesToCodes.setProperty("algae", "ICN");
		rankSystemNamesToCodes.setProperty("fungi", "ICN");
		rankSystemNamesToCodes.setProperty("generic", "NONE");
		for (Iterator rit = rankSystemNamesToCodes.keySet().iterator(); rit.hasNext();) {
			String rsn = ((String) rit.next());
			rankSystemNameSynonyms.setProperty(rankSystemNamesToCodes.getProperty(rsn).toLowerCase(), rsn.toLowerCase());
		}
		rankSystemNamesToDomains.setProperty("animalia", "Zoology");
		rankSystemNamesToDomains.setProperty("plantae", "Botany");
		rankSystemNamesToDomains.setProperty("bacteriae", "Bacteriology");
		rankSystemNamesToDomains.setProperty("algae", "Phycology");
		rankSystemNamesToDomains.setProperty("fungi", "Mycology");
		rankSystemNamesToDomains.setProperty("generic", "Biology");
		for (Iterator rit = rankSystemNamesToDomains.keySet().iterator(); rit.hasNext();) {
			String rsn = ((String) rit.next());
			rankSystemNameSynonyms.setProperty(rankSystemNamesToDomains.getProperty(rsn).toLowerCase(), rsn.toLowerCase());
		}
		rankSystemNameSynonyms.setProperty("ICBN".toLowerCase(), "plantae".toLowerCase());
	}
	
	/**
	 * Retrieve the names of the available rank systems. Client code may freely
	 * modify the returned array, as a new one is allocated and filled for each
	 * invocation of this method.
	 * @return an array holding the rank system names
	 */
	public static String[] getRankSystemNames() {
		return ((String[]) rankSystemNamesToCodes.keySet().toArray(new String[rankSystemNamesToCodes.size()]));
	}
	
	/**
	 * Retrieve the code a rank system corresponds to.
	 * @param the name of the rank system to get the code for
	 * @return the code acronym
	 */
	public static String getRankSystemCode(String name) {
		return rankSystemNamesToCodes.getProperty(name);
	}
	
	/**
	 * Retrieve the domain a rank system belongs to.
	 * @param the name of the rank system to get the domain for
	 * @return the domain name
	 */
	public static String getRankSystemDomain(String name) {
		return rankSystemNamesToDomains.getProperty(name);
	}
	
	/**
	 * Retrieve a rank system by its name, code, or domain. If the argument name
	 * is null, a generic rank system is returned that covers all the ranks
	 * covered by all the named rank systems, but does not include any rank
	 * specific suffixes, etc.
	 * @param name the name, code, or domain of the rank system to load
	 * @return the rank system, or null if there is none
	 */
	public static TaxonomicRankSystem getRankSystem(String name) {
		
		//	cast null to 'generic'
		if (name == null) name = "generic";
		
		//	normalize name
		name = rankSystemNameSynonyms.getProperty(name.toLowerCase(), name.toLowerCase());
		
		//	try cache first
		TaxonomicRankSystem trs = ((TaxonomicRankSystem) rankSystemCache.get(name));
		if (trs != null)
			return trs;
		
		//	load from XML
		try {
			String rsResourceName = TaxonomicRankSystem.class.getName().replaceAll("\\.", "/");
			rsResourceName = rsResourceName.substring(0, rsResourceName.lastIndexOf('/')) + "/rankSystems/" + name + ".xml";
			InputStream rsIn = TaxonomicRankSystem.class.getClassLoader().getResourceAsStream(rsResourceName);
			BufferedReader rsr = new BufferedReader(new InputStreamReader(rsIn, "UTF-8"));
			final TaxonomicRankSystem[] trsl = {null};
			rankSystemParser.stream(rsr, new TokenReceiver() {
				RankGroup rankGroup = null;
				public void storeToken(String token, int treeDepth) throws IOException {
					if (rankSystemGrammar.isTag(token)) {
						String type = rankSystemGrammar.getType(token);
						if ((this.rankGroup != null) && "rank".equals(type) && !rankSystemGrammar.isEndTag(token)) {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, rankSystemGrammar);
							String name = tnas.getAttribute("name");
							String abbreviationString = tnas.getAttribute("abbreviations");
							if ((name == null) || (abbreviationString == null))
								return;
							Rank rank = new Rank(name);
							rank.suffix = tnas.getAttribute("suffix", "");
							rank.abbreviations = abbreviationString.split("\\;");
							rank.epithetTemplate = tnas.getAttribute("epithetTemplate", rank.epithetTemplate);
							rank.group = this.rankGroup;
							rank.system = trsl[0];
							this.rankGroup.ranks.add(rank);
							trsl[0].ranks.add(rank);
						}
						else if ((trsl[0] != null) && "rankGroup".equals(type)) {
							if (rankSystemGrammar.isEndTag(token)) {
								trsl[0].rankGroups.add(this.rankGroup);
								this.rankGroup.system = trsl[0];
							}
							else {
								TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, rankSystemGrammar);
								String name = tnas.getAttribute("name");
								if (name != null)
									this.rankGroup = new RankGroup(name);
							}
						}
						else if ("rankSystem".equals(type) && !rankSystemGrammar.isEndTag(token)) {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, rankSystemGrammar);
							String name = tnas.getAttribute("name");
							String code = tnas.getAttribute("code");
							String domain = tnas.getAttribute("domain");
							if ((name != null) && (code != null) && (domain != null))
								trsl[0] = new TaxonomicRankSystem(name, code, domain);
						}
					}
					else { /* not exactly sure we need this right now ... */ }
				}
				public void close() throws IOException {}
			});
			rsr.close();
			trs = trsl[0];
			
			//	put in cache
			rankSystemCache.put(name, trs);
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
		
		//	finally ...
		return trs;
	}
	
	private static Grammar rankSystemGrammar = new StandardGrammar();
	private static Parser rankSystemParser = new Parser(rankSystemGrammar);
	private static HashMap rankSystemCache = new HashMap();
	
	//	TEST ONLY !!!
	public static void main(String[] args) throws Exception {
		TaxonomicRankSystem trs = getRankSystem("ICZN");
		RankGroup[] rgs = trs.getRankGroups();
		for (int g = 0; g < rgs.length; g++) {
			Rank[] rs = rgs[g].getRanks();
			for (int r = 0; r < rs.length; r++) {
				System.out.println(rgs[g].name + "." + rs[r].name + (rs[r].isPrimary() ? " (PRIMARY)" : "") + ":");
				System.out.println(" - significance: " + rs[r].getRelativeSignificance());
				System.out.println(" - abbreviation: " + rs[r].getAbbreviation());
				System.out.println(" - suffix: " + rs[r].getSuffix());
			}
		}
	}
}
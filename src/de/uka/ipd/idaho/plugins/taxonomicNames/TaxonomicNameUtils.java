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
 *     * Neither the name of the Universitaet Karlsruhe (TH) nor the
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
package de.uka.ipd.idaho.plugins.taxonomicNames;

import java.io.StringReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicRankSystem.Rank;

/**
 * Utility library for handling taxonomic names.
 * 
 * @author sautter
 */
public class TaxonomicNameUtils implements TaxonomicNameConstants {
	
	/**
	 * Representation of a taxonomic name.
	 * 
	 * @author sautter
	 */
	public static class TaxonomicName implements TaxonomicNameConstants {
		private Properties epithets = new Properties();
		private String authorityName = null;
		private int authorityYear = -1;
		private String baseAuthorityName = null;
		private int baseAuthorityYear = -1;
		private String rank;
		private boolean isHybrid = false;
		private String stringWithoutAuthority;
		private String stringWithAuthority;
		private String stringWithFullAuthority;
		private TaxonomicRankSystem rankSystem;
		
		/** Constructor
		 */
		public TaxonomicName() {
			this((String) null);
		}
		
		/**
		 * Constructor
		 * @param code the nomenclatorial code or biological domain the name
		 *            belongs to
		 */
		public TaxonomicName(String codeOrDomain) {
			this(TaxonomicRankSystem.getRankSystem(codeOrDomain));
		}
		
		/** Constructor
		 * @param rankSystem the rank system representing the nomenclatorial
		 *            code the name belongs to
		 */
		public TaxonomicName(TaxonomicRankSystem rankSystem) {
			this.rankSystem = rankSystem;
		}
		
		/** Constructor
		 * @param model the taxonomic name object to copy
		 */
		public TaxonomicName(TaxonomicName model) {
			this.rankSystem = model.rankSystem;
			this.epithets.putAll(model.epithets);
			this.isHybrid = model.isHybrid;
			this.authorityName = model.authorityName;
			this.authorityYear = model.authorityYear;
			this.baseAuthorityName = model.baseAuthorityName;
			this.baseAuthorityYear = model.baseAuthorityYear;
		}
		
		/**
		 * Retrieve the epithet of a given rank.
		 * @param rank the rank to get the epithet for
		 * @return the epithet of the argument rank
		 */
		public String getEpithet(String rank) {
			return this.epithets.getProperty(rank);
		}
		
		/**
		 * Set the epithet of a rank to some string value. If the epithet string
		 * is null, the specified rank is removed.
		 * @param rank the rank to modify
		 * @param epithet the new epithet for the rank
		 */
		public void setEpithet(String rank, String epithet) {
			if (epithet == null)
				this.epithets.remove(rank);
			else this.epithets.setProperty(rank, epithet);
			this.rank = null;
			this.stringWithAuthority = null;
			this.stringWithFullAuthority = null;
			this.stringWithoutAuthority = null;
		}
		
		/**
		 * Is the taxonomic name marked as a hybrid species name?
		 * @return true if the taxonomic name marked as a hybrid name
		 */
		public boolean isHybrid() {
			return this.isHybrid;
		}
		
		/**
		 * Mark the taxonomic name as a hybrid species name. This method only
		 * has an effect if the taxonomic rank system the name belongs to
		 * allows for hybrid species names. Furthermore, this property only has
		 * an effect if the rank of the name is that of a species.
		 * @param isHybrid is the taxonomic name a hybrid name?
		 */
		public void setHybrid(boolean isHybrid) {
			isHybrid = (this.rankSystem.allowsHybrids && isHybrid);
			if (this.isHybrid == isHybrid)
				return;
			this.stringWithAuthority = null;
			this.stringWithFullAuthority = null;
			this.stringWithoutAuthority = null;
			this.isHybrid = isHybrid;
		}
		
		/**
		 * Retrieve the authority name of the taxonomic name, i.e., the name of
		 * the scientist to author the most specific epithet.
		 * @return the authority name
		 */
		public String getAuthorityName() {
			return this.authorityName;
		}
		
		/**
		 * Set the authority name of the taxonomic name, i.e., the name of the
		 * scientist to author the most specific epithet.
		 * @param name the authority name to set
		 */
		public void setAuthorityName(String name) {
			this.authorityName = name;
			this.stringWithAuthority = null;
			this.stringWithFullAuthority = null;
		}
		
		/**
		 * Retrieve the authority year of the taxonomic name, i.e., the year the
		 * most specific epithet was first published.
		 * @return the authority year
		 */
		public int getAuthorityYear() {
			return this.authorityYear;
		}
		
		/**
		 * Set the authority year of the taxonomic name, i.e., the year the most
		 * specific epithet was first published.
		 * @param year the authority year to set
		 */
		public void setAuthorityYear(int year) {
			this.authorityYear = year;
			this.stringWithAuthority = null;
			this.stringWithFullAuthority = null;
		}
		
		/**
		 * Retrieve the authority of the taxonomic name, i.e., the name of the
		 * scientist to author the most specific epithet, and the year the most
		 * specific epithet was first published. If either of both fields is not
		 * set, this method returns the other, if both are not set, this method
		 * returns null.
		 * @return the authority
		 */
		public String getAuthority() {
			if (this.authorityName == null)
				return ((this.authorityYear < 0) ? null : ("" + this.authorityYear));
			else return (this.authorityName + ((this.authorityYear < 0) ? "" : (" " + this.authorityYear)));
		}
		
		/**
		 * Set the authority of the taxonomic name, i.e., the name of the
		 * scientist to author the most specific epithet, and the year the most
		 * specific epithet was first published.
		 * @param name the authority name to set
		 * @param year the authority year to set
		 */
		public void setAuthority(String name, int year) {
			this.authorityName = name;
			this.authorityYear = year;
			this.stringWithAuthority = null;
			this.stringWithFullAuthority = null;
			this.stringWithoutAuthority = null;
		}
		
		/**
		 * Retrieve the authority name of the original taxonomic name, i.e.,
		 * the name of the scientist to author the most specific epithet in its
		 * original combination (the basionym).
		 * @return the basionym authority name
		 */
		public String getBaseAuthorityName() {
			return this.baseAuthorityName;
		}
		
		/**
		 * Set the authority name of the taxonomic name, i.e., the name of the
		 * the name of the scientist to author the most specific epithet in its
		 * original combination (the basionym).
		 * @param name the basionym authority name to set
		 */
		public void setBaseAuthorityName(String name) {
			this.baseAuthorityName = name;
			this.stringWithAuthority = null;
			this.stringWithFullAuthority = null;
		}
		
		/**
		 * Retrieve the authority year of the taxonomic name, i.e., the year the
		 * most specific epithet was first published in its original combination
		 * (the basionym).
		 * @return the basionym authority year
		 */
		public int getBaseAuthorityYear() {
			return this.baseAuthorityYear;
		}
		
		/**
		 * Set the authority year of the taxonomic name, i.e., the year the most
		 * specific epithet was first published in its original combination (the
		 * basionym).
		 * @param year the basionym authority year to set
		 */
		public void setBaseAuthorityYear(int year) {
			this.baseAuthorityYear = year;
			this.stringWithAuthority = null;
			this.stringWithFullAuthority = null;
		}
		
		/**
		 * Retrieve the authority of the basionym, i.e., the name of the
		 * scientist to author the most specific epithet in its original
		 * combination, and the year the most specific epithet was first
		 * published in that original combination. If either of both fields
		 * is not set, this method returns the other, if both are not set,
		 * this method returns null.
		 * @return the basionym authority
		 */
		public String getBaseAuthority() {
			if (this.baseAuthorityName == null)
				return ((this.baseAuthorityYear < 0) ? null : ("" + this.baseAuthorityYear));
			else return (this.baseAuthorityName + ((this.baseAuthorityYear < 0) ? "" : (" " + this.baseAuthorityYear)));
		}
		
		/**
		 * Set the authority of the basionym, i.e., the name of the scientist
		 * to author the most specific epithet in its original combination, and
		 * the year the most specific epithet was first published in that
		 * original combination.
		 * @param name the basionym authority name to set
		 * @param year the basionym authority year to set
		 */
		public void setBaseAuthority(String name, int year) {
			this.baseAuthorityName = name;
			this.baseAuthorityYear = year;
			this.stringWithAuthority = null;
			this.stringWithFullAuthority = null;
			this.stringWithoutAuthority = null;
		}
		
		/**
		 * Retrieve the full authority of the taxonomic name, i.e., the
		 * basionym authority in parentheses followed by the authority proper.
		 * If either of both is not set, this method returns the other, if
		 * both are not set, this method returns null.
		 * @return the full authority
		 */
		public String getFullAuthority() {
			String authority = this.getAuthority();
			String baseAuthority = this.getBaseAuthority();
			if (baseAuthority == null)
				return authority;
			else if (authority == null)
				return ("(" + baseAuthority + ")");
			else return ("(" + baseAuthority + ") " + authority);
		}
		
		/**
		 * Get the rank of the name, i.e., the rank of the most significant
		 * epithet. If the name object does not contain and epithets, this
		 * method returns null.
		 * @return the rank of the most significant epithet
		 */
		public String getRank() {
			if (this.epithets.isEmpty())
				return null;
			if (this.rank != null)
				return this.rank;
			Rank[] ranks = this.rankSystem.getRanks();
			for (int r = ranks.length; r > 0; r--)
				if (this.epithets.containsKey(ranks[r-1].name)) {
					this.rank = ranks[r-1].name;
					break;
				}
			return this.rank;
		}
		
		/**
		 * Convert the taxonomic name into a string, using the formatting
		 * functionality of the embedded rank system. This method is equivalent
		 * to calling <code>toString(false)</code>.
		 * @return a string representation of the taxonomic name
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return this.toString(false, false);
		}
		
		/**
		 * Convert the taxonomic name into a string, using the formatting
		 * functionality of the embedded rank system.
		 * @param includeAuthority add authority information if given?
		 * @return a string representation of the taxonomic name
		 * @see java.lang.Object#toString()
		 */
		public String toString(boolean includeAuthority) {
			return this.toString(includeAuthority, false);
		}
		
		/**
		 * Convert the taxonomic name into a string, using the formatting
		 * functionality of the embedded rank system.
		 * @param includeAuthority add authority information if given?
		 * @param includeBaseAuthority add basionym authority information if
		 *            given?
		 * @return a string representation of the taxonomic name
		 * @see java.lang.Object#toString()
		 */
		public String toString(boolean includeAuthority, boolean includeBaseAuthority) {
			
			//	check cache
			if (includeBaseAuthority && (this.stringWithFullAuthority != null))
				return this.stringWithFullAuthority;
			if (includeAuthority && (this.stringWithAuthority != null))
				return this.stringWithAuthority;
			if (!includeAuthority && (this.stringWithoutAuthority != null))
				return this.stringWithoutAuthority;
			
			//	make sure not to add naked basionym authority
			if (includeBaseAuthority)
				includeAuthority = true;
			
			//	determine rank
			String rank = this.getRank();
			if (rank == null)
				return "";
			Rank ownRank = this.rankSystem.getRank(rank);
			if (ownRank == null)
				return "";
			Rank genusRank = this.rankSystem.getRank(GENUS_ATTRIBUTE);
			StringBuffer string = new StringBuffer();
			
			//	genus or above, only use most significant epithet
			if (ownRank.getRelativeSignificance() <= genusRank.getRelativeSignificance()) {
				String epithet = this.epithets.getProperty(ownRank.name);
				if (epithet != null) {
					if (string.length() != 0)
						string.append(' ');
					string.append(ownRank.formatEpithet(epithet));
				}
			}
			
			//	below genus, use all epithets from genus downward
			else {
				Rank[] ranks = this.rankSystem.getRanks();
				for (int r = 0; r < ranks.length; r++) {
					if (ranks[r].getRelativeSignificance() < genusRank.getRelativeSignificance())
						continue;
					String epithet = this.epithets.getProperty(ranks[r].name);
					if (epithet == null)
						continue;
					if (string.length() != 0)
						string.append(' ');
					if (this.isHybrid && this.rankSystem.allowsHybrids && SPECIES_ATTRIBUTE.equals(ownRank) && SPECIES_ATTRIBUTE.equals(ranks[r].name))
						string.append("\u00D7 ");
					string.append(ranks[r].formatEpithet(epithet));
				}
			}
			
			//	add basionym authority if asked to
			if (includeBaseAuthority && (this.baseAuthorityName != null) && (string.length() != 0)) {
				string.append(' ');
				string.append("(" + this.getBaseAuthority() + ")");
			}
			
			//	add authority if asked to
			if (includeAuthority && (this.authorityName != null) && (string.length() != 0)) {
				string.append(' ');
				string.append(this.getAuthority());
			}
			
			//	cache what we got
			if (includeBaseAuthority)
				this.stringWithFullAuthority = string.toString();
			else if (includeAuthority)
				this.stringWithAuthority = string.toString();
			else this.stringWithoutAuthority = string.toString();
			
			//	finally ...
			return string.toString();
		}
		
		/**
		 * Convert the name into DarwinCore XML, additional ranks supplemented by
		 * DarwinCore-Ranks.
		 * @return the name as DarwinCore XML
		 */
		public String toDwcXml() {
			return TaxonomicNameUtils.toDwcXml(this);
		}
		
		/**
		 * Convert the name into Simple DarwinCore XML, additional ranks
		 * supplemented by DarwinCore-Ranks.
		 * @return the name as Simple DarwinCore XML
		 */
		public String toSimpleDwcXml() {
			return TaxonomicNameUtils.toSimpleDwcXml(this);
		}
	}
	
	/**
	 * Create a name object from its representation as an annotation to a
	 * document, with the details stored in attributes.
	 * @param taxNameAnnot the annotation to convert
	 * @return a name object holding the same data as the argument annotation
	 */
	public static TaxonomicName genericXmlToTaxonomicName(Annotation taxNameAnnot) {
		return genericXmlToTaxonomicName(taxNameAnnot, ((String) null));
	}
	
	/**
	 * Create a name object from its representation as an annotation to a
	 * document, with the details stored in attributes.
	 * @param taxNameAnnot the annotation to convert
	 * @param code the nomenclatorial code or biological domain the name belongs
	 *            to
	 * @return a name object holding the same data as the argument annotation
	 */
	public static TaxonomicName genericXmlToTaxonomicName(Annotation taxNameAnnot, String codeOrDomain) {
		return genericXmlToTaxonomicName(taxNameAnnot, TaxonomicRankSystem.getRankSystem(codeOrDomain));
	}
	
	/**
	 * Create a name object from its representation as an annotation to a
	 * document, with the details stored in attributes.
	 * @param taxNameAnnot the annotation to convert
	 * @param rankSystem the rank system representing the nomenclatorial code
	 *            the name belongs to
	 * @return a name object holding the same data as the argument annotation
	 */
	public static TaxonomicName genericXmlToTaxonomicName(Annotation taxNameAnnot, TaxonomicRankSystem rankSystem) {
		TaxonomicName taxName = null;
		Rank[] ranks = rankSystem.getRanks();
		for (int r = 0; r < ranks.length; r++) {
			String epithet = ((String) taxNameAnnot.getAttribute(ranks[r].name));
			if (epithet == null)
				continue;
			if (taxName == null)
				taxName = new TaxonomicName(rankSystem);
			taxName.setEpithet(ranks[r].name, epithet);
		}
		if (taxName == null)
			return taxName;
		
		//	set hybrid if annotation flagged accordingly
		if (SPECIES_ATTRIBUTE.equals(taxName.getRank()) && rankSystem.allowsHybrids && taxNameAnnot.hasAttribute(HYBRID_MARKER_ATTRIBUTE))
			taxName.setHybrid(true);
		
		//	add authority and basionym authority
		addAuthority(taxName, taxNameAnnot);
		addBaseAuthority(taxName, taxNameAnnot);
		
		//	finally ...
		return taxName;
	}
	
	private static void addAuthority(TaxonomicName taxName, Annotation taxNameAnnot) {
		
		//	get authority details
		String authorityName = ((String) taxNameAnnot.getAttribute(AUTHORITY_NAME_ATTRIBUTE));
		String authorityYear = ((String) taxNameAnnot.getAttribute(AUTHORITY_YEAR_ATTRIBUTE));
		
		//	parse authority from base data if missing
		if (authorityName == null) {
			
			//	get verbatim authority
			String authorityStr = getCombinationAuthority(taxName, taxNameAnnot);
			if (authorityStr == null)
				return;
			
			//	parse authority
			TaxonomicAuthority authority = parseAuthority(authorityStr);
			authorityName = authority.name;
			authorityYear = ((authority.year == -1) ? authorityYear : ("" + authority.year));
		}
		
		//	set authority
		if (authorityName != null)
			taxName.setAuthorityName(authorityName);
		if ((authorityYear != null) && authorityYear.matches("[12][0-9]{3}"))
			taxName.setAuthorityYear(Integer.parseInt(authorityYear));
	}
	
	private static void addBaseAuthority(TaxonomicName taxName, Annotation taxNameAnnot) {
		
		//	get basionym authority details
		String baseAuthorityName = ((String) taxNameAnnot.getAttribute(BASE_AUTHORITY_NAME_ATTRIBUTE));
		String baseAuthorityYear = ((String) taxNameAnnot.getAttribute(BASE_AUTHORITY_YEAR_ATTRIBUTE));
		
		//	parse authority from base data if missing
		if (baseAuthorityName == null) {
			
			//	get verbatim basionym authority
			String baseAuthorityStr = getBasionymAuthority(taxName, taxNameAnnot);
			if (baseAuthorityStr == null)
				return;
			
			//	parse basionym authority
			TaxonomicAuthority baseAuthority = parseAuthority(baseAuthorityStr);
			baseAuthorityName = baseAuthority.name;
			baseAuthorityYear = ((baseAuthority.year == -1) ? baseAuthorityYear : ("" + baseAuthority.year));
		}
		
		//	set authority
		if (baseAuthorityName != null)
			taxName.setBaseAuthorityName(baseAuthorityName);
		if ((baseAuthorityYear != null) && baseAuthorityYear.matches("[12][0-9]{3}"))
			taxName.setBaseAuthorityYear(Integer.parseInt(baseAuthorityYear));
	}
	
	private static final Pattern yearPattern = Pattern.compile("[12][0-9]{3}");
	
	/**
	 * Create a name object from its XML representation in DarwinCore or Simple
	 * DarwinCore.
	 * @param dwcXml the XML representation to parse
	 * @return a name object holding the same data as the argument XML
	 */
	public static TaxonomicName dwcXmlToTaxonomicName(QueriableAnnotation dwcXml) {
		return dwcXmlToTaxonomicName(dwcXml, ((String) null));
	}
	
	/**
	 * Create a name object from its XML representation in DarwinCore or Simple
	 * DarwinCore.
	 * @param dwcXml the XML representation to parse
	 * @param code the nomenclatorial code or biological domain the name belongs
	 *            to
	 * @return a name object holding the same data as the argument XML
	 */
	public static TaxonomicName dwcXmlToTaxonomicName(QueriableAnnotation dwcXml, String codeOrDomain) {
		return dwcXmlToTaxonomicName(dwcXml, TaxonomicRankSystem.getRankSystem(codeOrDomain));
	}
	
	/**
	 * Create a name object from its XML representation in DarwinCore or Simple
	 * DarwinCore.
	 * @param dwcXml the XML representation to parse
	 * @param rankSystem the rank system representing the nomenclatorial code
	 *            the name belongs to
	 * @return a name object holding the same data as the argument XML
	 */
	public static TaxonomicName dwcXmlToTaxonomicName(QueriableAnnotation dwcXml, TaxonomicRankSystem rankSystem) {
		TaxonomicName taxName = null;
		Annotation[] dwcEpithets = dwcXml.getAnnotations();
		for (int e = 0; e < dwcEpithets.length; e++) {
			String type = dwcEpithets[e].getType();
			type = type.substring(type.indexOf(':') + 1);
			String rank = dwcElementsToRanks.getProperty(type);
			if (rank != null) {
				if (taxName == null)
					taxName = new TaxonomicName(rankSystem);
				taxName.setEpithet(rank, dwcEpithets[e].getValue());
			}
			else if ("scientificNameAuthorship".equals(type)) {
				String verbatimAuthority = dwcEpithets[e].getValue();
				String authorityStr = getCombinationAuthority(verbatimAuthority);
				if (authorityStr != null) {
					TaxonomicAuthority authority = parseAuthority(authorityStr);
					taxName.setAuthority(authority.name, authority.year);
				}
				String baseAuthorityStr = getBasionymAuthority(verbatimAuthority);
				if (baseAuthorityStr != null) {
					TaxonomicAuthority baseAuthority = parseAuthority(baseAuthorityStr);
					taxName.setAuthority(baseAuthority.name, baseAuthority.year);
				}
			}
		}
		return taxName;
	}
	
	/**
	 * Convert a name into DarwinCore XML, additional ranks supplemented by
	 * DarwinCore-Ranks.
	 * @param taxName the name to convert
	 * @return the argument name as Simple DarwinCore XML
	 */
	public static String toDwcXml(TaxonomicName taxName) {
		return toXml(taxName, ranksToDwcElementsClassic);
	}
	
	/**
	 * Convert a name into Simple DarwinCore XML, additional ranks supplemented
	 * by DarwinCore-Ranks.
	 * @param taxName the name to convert
	 * @return the argument name as Simple DarwinCore XML
	 */
	public static String toSimpleDwcXml(TaxonomicName taxName) {
		return toXml(taxName, ranksToDwcElementsSimple);
	}
	
	private static String toXml(TaxonomicName taxName, Properties elementMapping) {
		StringBuffer xml = new StringBuffer("<" + TAXONOMIC_NAME_ANNOTATION_TYPE + 
				" xmlns:dwc=\"" + ((elementMapping == ranksToDwcElementsClassic) ? "http://digir.net/schema/conceptual/darwin/2003/1.0" : "http://rs.tdwg.org/dwc/terms/") + "\"" +
				" xmlns:dwcranks=\"http://rs.tdwg.org/UBIF/2006/Schema/1.1\"" +
				">");
		Rank[] ranks = taxName.rankSystem.getRanks();
		String rank = null;
		for (int r = 0; r < ranks.length; r++) {
			String epithet = taxName.getEpithet(ranks[r].name);
			if (epithet != null) {
				rank = ranks[r].name;
				String elementName = elementMapping.getProperty(ranks[r].name);
				xml.append("<" + elementName + ">");
				xml.append(AnnotationUtils.escapeForXml(epithet));
				xml.append("</" + elementName + ">");
			}
		}
		if (rank != null) {
			xml.append("<dwc:taxonRank>");
			xml.append(AnnotationUtils.escapeForXml(rank.toLowerCase()));
			xml.append("</dwc:taxonRank>");
//			String authority = taxName.getAuthority();
			String authority = taxName.getFullAuthority();
			if (authority != null) {
				xml.append("<dwc:scientificNameAuthorship>");
				xml.append(AnnotationUtils.escapeForXml(authority));
				xml.append("</dwc:scientificNameAuthorship>");
			}
		}
		xml.append("</" + TAXONOMIC_NAME_ANNOTATION_TYPE + ">");
		return xml.toString();
	}
	
	private static Properties ranksToDwcElementsAll = new Properties();
	private static Properties ranksToDwcElementsClassic = new Properties(ranksToDwcElementsAll);
	private static Properties ranksToDwcElementsSimple = new Properties(ranksToDwcElementsAll);
	static {
		ranksToDwcElementsAll.setProperty(DOMAIN_ATTRIBUTE, "dwcranks:domain");
		
		ranksToDwcElementsAll.setProperty(SUPERKINGDOM_ATTRIBUTE, "dwcranks:superkingdom");
		ranksToDwcElementsAll.setProperty(SUBKINGDOM_ATTRIBUTE, "dwcranks:subkingdom");
		ranksToDwcElementsAll.setProperty(INFRAKINGDOM_ATTRIBUTE, "dwcranks:infrakingdom");
		
		ranksToDwcElementsAll.setProperty(SUPERPHYLUM_ATTRIBUTE, "dwcranks:superphylum");
		ranksToDwcElementsAll.setProperty(SUBPHYLUM_ATTRIBUTE, "dwcranks:subphylum");
		ranksToDwcElementsAll.setProperty(INFRAPHYLUM_ATTRIBUTE, "dwcranks:infraphylum");
		
		ranksToDwcElementsAll.setProperty(SUPERCLASS_ATTRIBUTE, "dwcranks:superclass");
		ranksToDwcElementsAll.setProperty(SUBCLASS_ATTRIBUTE, "dwcranks:subclass");
		ranksToDwcElementsAll.setProperty(INFRACLASS_ATTRIBUTE, "dwcranks:infraclass");
		
		ranksToDwcElementsAll.setProperty(SUPERORDER_ATTRIBUTE, "dwcranks:superorder");
		ranksToDwcElementsAll.setProperty(SUBORDER_ATTRIBUTE, "dwcranks:suborder");
		ranksToDwcElementsAll.setProperty(INFRAORDER_ATTRIBUTE, "dwcranks:infraorder");
		
		ranksToDwcElementsAll.setProperty(SUPERFAMILY_ATTRIBUTE, "dwcranks:superfamily");
		ranksToDwcElementsAll.setProperty(SUBFAMILY_ATTRIBUTE, "dwcranks:subfamily");
		ranksToDwcElementsAll.setProperty(INFRAFAMILY_ATTRIBUTE, "dwcranks:infrafamily");
		ranksToDwcElementsAll.setProperty(SUPERTRIBE_ATTRIBUTE, "dwcranks:supertribe");
		ranksToDwcElementsAll.setProperty(TRIBE_ATTRIBUTE, "dwcranks:tribe");
		ranksToDwcElementsAll.setProperty(SUBTRIBE_ATTRIBUTE, "dwcranks:subtribe");
		ranksToDwcElementsAll.setProperty(INFRATRIBE_ATTRIBUTE, "dwcranks:infratribe");
		
		ranksToDwcElementsAll.setProperty(SUBGENUS_ATTRIBUTE, "dwcranks:subgenus");
		ranksToDwcElementsAll.setProperty(INFRAGENUS_ATTRIBUTE, "dwcranks:infragenus");
		ranksToDwcElementsAll.setProperty(SECTION_ATTRIBUTE, "dwcranks:section");
		ranksToDwcElementsAll.setProperty(SUBSECTION_ATTRIBUTE, "dwcranks:subsection");
		ranksToDwcElementsAll.setProperty(SERIES_ATTRIBUTE, "dwcranks:series");
		ranksToDwcElementsAll.setProperty(SUBSERIES_ATTRIBUTE, "dwcranks:subseries");
		
		ranksToDwcElementsAll.setProperty(SPECIESAGGREGATE_ATTRIBUTE, "dwcranks:speciesAggregate");
		ranksToDwcElementsAll.setProperty(SUBSPECIES_ATTRIBUTE, "dwcranks:subspeciesEpithet");
		ranksToDwcElementsAll.setProperty(INFRASPECIES_ATTRIBUTE, "dwcranks:infraspeciesEpithet");
		ranksToDwcElementsAll.setProperty(VARIETY_ATTRIBUTE, "dwcranks:varietyEpithet");
		ranksToDwcElementsAll.setProperty(SUBVARIETY_ATTRIBUTE, "dwcranks:subvarietyEpithet");
		ranksToDwcElementsAll.setProperty(FORM_ATTRIBUTE, "dwcranks:formEpithet");
		ranksToDwcElementsAll.setProperty(SUBFORM_ATTRIBUTE, "dwcranks:subformEpithet");
		
		ranksToDwcElementsClassic.setProperty(KINGDOM_ATTRIBUTE, "dwc:Kingdom");
		ranksToDwcElementsClassic.setProperty(PHYLUM_ATTRIBUTE, "dwc:Phylum");
		ranksToDwcElementsClassic.setProperty(CLASS_ATTRIBUTE, "dwc:Class");
		ranksToDwcElementsClassic.setProperty(ORDER_ATTRIBUTE, "dwc:Order");
		ranksToDwcElementsClassic.setProperty(FAMILY_ATTRIBUTE, "dwc:Family");
		ranksToDwcElementsClassic.setProperty(GENUS_ATTRIBUTE, "dwc:Genus");
		ranksToDwcElementsClassic.setProperty(SPECIES_ATTRIBUTE, "dwc:Species");
		ranksToDwcElementsClassic.setProperty(SUBSPECIES_ATTRIBUTE, "dwc:Subspecies");
		
		ranksToDwcElementsSimple.setProperty(KINGDOM_ATTRIBUTE, "dwc:kingdom");
		ranksToDwcElementsSimple.setProperty(PHYLUM_ATTRIBUTE, "dwc:phylum");
		ranksToDwcElementsSimple.setProperty(CLASS_ATTRIBUTE, "dwc:class");
		ranksToDwcElementsSimple.setProperty(ORDER_ATTRIBUTE, "dwc:order");
		ranksToDwcElementsSimple.setProperty(FAMILY_ATTRIBUTE, "dwc:family");
		ranksToDwcElementsSimple.setProperty(GENUS_ATTRIBUTE, "dwc:genus");
		ranksToDwcElementsSimple.setProperty(SUBGENUS_ATTRIBUTE, "dwc:subgenus");
		ranksToDwcElementsSimple.setProperty(SPECIES_ATTRIBUTE, "dwc:speciesEpithet");
	}
	
	private static Properties dwcElementsToRanks = new Properties();
	static {
		for (Iterator ait = ranksToDwcElementsAll.keySet().iterator(); ait.hasNext();) {
			String attribute = ((String) ait.next());
			String element = ranksToDwcElementsAll.getProperty(attribute);
			dwcElementsToRanks.setProperty(element.substring(element.indexOf(':') + 1), attribute);
		}
		for (Iterator ait = ranksToDwcElementsClassic.keySet().iterator(); ait.hasNext();) {
			String attribute = ((String) ait.next());
			String element = ranksToDwcElementsClassic.getProperty(attribute);
			dwcElementsToRanks.setProperty(element.substring(element.indexOf(':') + 1), attribute);
		}
		for (Iterator ait = ranksToDwcElementsSimple.keySet().iterator(); ait.hasNext();) {
			String attribute = ((String) ait.next());
			String element = ranksToDwcElementsSimple.getProperty(attribute);
			dwcElementsToRanks.setProperty(element.substring(element.indexOf(':') + 1), attribute);
		}
	}
	
	/**
	 * Cut the verbatim authority from a given taxonomic name. This is all the
	 * characters from an annotated taxonomic name that follow after the most
	 * significant epithet. If the argument taxonomic name annotation does not
	 * include any tokens beyond the most significant epithet, this method
	 * returns null.
	 * @param taxonName the taxon name object to take the most significant
	 *            epithet from (created from the annotation if null)
	 * @param taxonNameAnnot the taxon name annotation whose authority part to
	 *            extract
	 * @return the verbatim annotated authority
	 */
	public static String getVerbatimAuthority(TaxonomicName taxonName, Annotation taxonNameAnnot) {
		if (taxonName == null)
			taxonName = genericXmlToTaxonomicName(taxonNameAnnot);
		if (taxonName == null)
			return null;
		String rank = taxonName.getRank();
		String mostSigEpithet = taxonName.getEpithet(rank);
		if (mostSigEpithet == null)
			return null; // whatever might be wrong here ...
		int authorityStartIndex = (TokenSequenceUtils.indexOf(taxonNameAnnot, mostSigEpithet, false) + 1);
		if (authorityStartIndex < 1)
			return null; // most significant epithet not found, little we can do ...
		if (authorityStartIndex == taxonNameAnnot.size())
			return null; // most significant epithet at very end, no authority given
		String verbatimAuthority = TokenSequenceUtils.concatTokens(taxonNameAnnot, authorityStartIndex, (taxonNameAnnot.size() - authorityStartIndex), true, true);
		while (verbatimAuthority.startsWith(")") || verbatimAuthority.startsWith("]"))
			verbatimAuthority = verbatimAuthority.substring(")".length()).trim(); // eliminate any leading _closing_ parentheses (can remain from subGenus, section, or subSection epithet ...
		verbatimAuthority = verbatimAuthority.replaceAll("\\(\\s+", "(");
		verbatimAuthority = verbatimAuthority.replaceAll("\\s+\\)", ")");
		return verbatimAuthority;
	}
	
	/**
	 * Cut the authority of the current combination from a given taxonomic
	 * name. This is the verbatim authority less any leading basionym authority
	 * in parentheses. If the verbatim authority is null, this method returns
	 * null as well. If only the basionym authority is given (whole authority
	 * in parentheses), this method also returns null.
	 * @param taxonName the taxon name object to take the most significant
	 *            epithet from (created from the annotation if null)
	 * @param taxonNameAnnot the taxon name annotation whose authority part to
	 *            extract
	 * @return the current combination authority, if any
	 */
	public static String getCombinationAuthority(TaxonomicName taxonName, Annotation taxonNameAnnot) {
		String verbatimAuthority = getVerbatimAuthority(taxonName, taxonNameAnnot);
		return getCombinationAuthority(verbatimAuthority);
	}
	
	/**
	 * Cut the authority of the current combination from a given taxonomic
	 * name. This is the verbatim authority less any leading basionym authority
	 * in parentheses. If the verbatim authority is null, this method returns
	 * null as well. If only the basionym authority is given (whole authority
	 * in parentheses), this method also returns null.
	 * @param verbatimAuthority the verbatim authority string as a whole
	 * @return the current combination authority, if any
	 */
	public static String getCombinationAuthority(String verbatimAuthority) {
		if (verbatimAuthority == null)
			return null;
		if (!verbatimAuthority.startsWith("("))
			return verbatimAuthority; // all we have is the current combination authority
		if (verbatimAuthority.matches(".*\\)\\s*[12][0-9]{3}(\\s?[a-f])?"))
			return null; // treat '(<name>) <year>' just like '(<name>, <year>)'
		int[] parenthesisDepths = getCharacterParenthesisDepths(verbatimAuthority);
		for (int c = 0; c < verbatimAuthority.length(); c++) {
			if (parenthesisDepths[c] == 0)
				return verbatimAuthority.substring(c).trim();
		}
		return null; // whole authority in parentheses
	}
	
	/**
	 * Cut the authority of the basionym (original combination) from a given
	 * taxonomic name. This is any leading part in parentheses contained in the
	 * verbatim authority. If the verbatim authority is null, this method
	 * returns null as well. If only the current combination authority is given
	 * (no part in parentheses), this method also returns null.
	 * @param taxonName the taxon name object to take the most significant
	 *            epithet from (created from the annotation if null)
	 * @param taxonNameAnnot the taxon name annotation whose authority part to
	 *            extract
	 * @return the basionym authority
	 */
	public static String getBasionymAuthority(TaxonomicName taxonName, Annotation taxonNameAnnot) {
		String verbatimAuthority = getVerbatimAuthority(taxonName, taxonNameAnnot);
		return getBasionymAuthority(verbatimAuthority);
	}
	
	/**
	 * Cut the authority of the basionym (original combination) from a given
	 * taxonomic name. This is any leading part in parentheses contained in the
	 * verbatim authority. If the verbatim authority is null, this method
	 * returns null as well. If only the current combination authority is given
	 * (no part in parentheses), this method also returns null.
	 * @param verbatimAuthority the verbatim authority string as a whole
	 * @return the basionym authority
	 */
	public static String getBasionymAuthority(String verbatimAuthority) {
		if (verbatimAuthority == null)
			return null;
		if (!verbatimAuthority.startsWith("("))
			return null; // all we have is the combination authority
		if (verbatimAuthority.matches(".*\\)\\s*[12][0-9]{3}(\\s?[a-f])?")) {
			int parenthesesEnd = verbatimAuthority.lastIndexOf(')'); // convert '(<name>) <year>' into '(<name>, <year>)'
			return (verbatimAuthority.substring(0, parenthesesEnd).trim() + ", " + verbatimAuthority.substring(parenthesesEnd + ")".length()).trim() + ")");
		}
		int[] parenthesisDepths = getCharacterParenthesisDepths(verbatimAuthority);
		for (int c = 0; c < verbatimAuthority.length(); c++) {
			if (parenthesisDepths[c] == 0)
				return verbatimAuthority.substring(0, c).trim();
		}
		return verbatimAuthority; // whole authority in parentheses
	}
	
	/**
	 * Container for a taxonomic name authority, comprising scientists name and
	 * year of publication.
	 * 
	 * @author sautter
	 */
	public static class TaxonomicAuthority {
		
		/** the name part of the authority, null if absent */
		public final String name;
		
		/** the year part of the authority, -1 if absent */
		public final int year;
		
		/**
		 * @param name
		 * @param year
		 */
		public TaxonomicAuthority(String name, int year) {
			this.name = name;
			this.year = year;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			if ((this.name == null) && (this.year < 0))
				return "";
			else if (this.name == null)
				return ("" + this.year);
			else if (this.year < 0)
				return this.name;
			else return (this.name + ", " + this.year);
		}
	}
	
	/**
	 * Parse a taxonomic authority into its name and year parts.
	 * @param authority the authority string to parse
	 * @return the parsed authority
	 */
	public static TaxonomicAuthority parseAuthority(String authority) {
		if (authority == null)
			return null;
		
		//	get punctuation out of the way
		authority = truncatePunctuation(authority);
		
		String authorityName = null;
		String authorityYear = null;
		
		//	find and remove (last) year
		for (Matcher ym = yearPattern.matcher(authority); ym.find();) {
			authorityYear = ym.group();
			authorityName = truncatePunctuation(authority.substring(0, ym.start()).trim());
		}
		
		//	no year give, use all we have
		if (authorityName == null)
			authorityName = authority;
		
		//	finally ...
		return new TaxonomicAuthority(authorityName, ((authorityYear == null) ? -1 : Integer.parseInt(authorityYear)));
	}
	
	private static String truncatePunctuation(String authority) {
		while (authority.startsWith("(")) // starting parenthesis of basionym authority
			authority = authority.substring("(".length()).trim();
		while (authority.endsWith("(")) // end of '<name> (<year>)' authority after cropping year
			authority = authority.substring(0, (authority.length() - "(".length())).trim();
		while (authority.endsWith("[")) // end of '<name> [<year>]' authority after cropping year
			authority = authority.substring(0, (authority.length() - "[".length())).trim();
		while (authority.endsWith(",")) // end of '<name>, <year>' authority after cropping year
			authority = authority.substring(0, (authority.length() - ",".length())).trim();
		while (authority.endsWith(")")) // end of '<name> (<year>)' authority or basionym authority
			authority = authority.substring(0, (authority.length() - ")".length())).trim();
		while (authority.endsWith("]")) // end of '<name> [<year>]' authority
			authority = authority.substring(0, (authority.length() - "]".length())).trim();
		return authority;
	}
	
	private static int[] getCharacterParenthesisDepths(String str) {
		int[] parenthesisDepths = new int[str.length()];
		int parenthesisDepth = 0;
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			if (ch == '(')
				parenthesisDepth++;
			parenthesisDepths[c] = parenthesisDepth;
			if (ch == ')')
				parenthesisDepth--;
		}
		return parenthesisDepths;
	}
	
	//	TEST ONLY !!!
	public static void main(String[] args) throws Exception {
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("<taxonomicName authority=\"(Clement, 1924)\" authorityName=\"Clement\" authorityYear=\"1924\" class=\"Insecta\" family=\"Ichneumonidae\" genus=\"Rhimphoctona\" kingdom=\"Animalia\" lsidName=\"Rhimphoctona (Xylophylax) rufocoxalis\" order=\"Hymenoptera\" pageId=\"0\" pageNumber=\"1047\" phylum=\"Arthropoda\" rank=\"species\" species=\"rufocoxalis\" subGenus=\"Xylophylax\">Rhimphoctona (Xylophylax) rufocoxalis (Clement, 1924)</taxonomicName>"));
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("<taxonomicName authority=\"(Clement, 1924)\" authorityName=\"Clement\" authorityYear=\"1924\" class=\"Insecta\" family=\"Ichneumonidae\" genus=\"Rhimphoctona\" kingdom=\"Animalia\" lsidName=\"Rhimphoctona (Xylophylax) rufocoxalis\" order=\"Hymenoptera\" pageId=\"0\" pageNumber=\"1047\" phylum=\"Arthropoda\" rank=\"species\" species=\"rufocoxalis\" subGenus=\"Xylophylax\">Rhimphoctona (Xylophylax) rufocoxalis (Clement) 1924</taxonomicName>"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("<taxonomicName authority=\"(Clement, 1924)\" authorityName=\"Clement\" authorityYear=\"1924\" class=\"Insecta\" family=\"Ichneumonidae\" genus=\"Rhimphoctona\" kingdom=\"Animalia\" lsidName=\"Rhimphoctona (Xylophylax) rufocoxalis\" order=\"Hymenoptera\" pageId=\"0\" pageNumber=\"1047\" phylum=\"Arthropoda\" rank=\"species\" species=\"rufocoxalis\" subGenus=\"Xylophylax\">Rhimphoctona (Xylophylax) rufocoxalis Clement, 1924</taxonomicName>"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("<taxonomicName authority=\"(Clement, 1924)\" authorityName=\"Clement\" authorityYear=\"1924\" class=\"Insecta\" family=\"Ichneumonidae\" genus=\"Rhimphoctona\" kingdom=\"Animalia\" lsidName=\"Rhimphoctona (Xylophylax) rufocoxalis\" order=\"Hymenoptera\" pageId=\"0\" pageNumber=\"1047\" phylum=\"Arthropoda\" rank=\"species\" species=\"rufocoxalis\" subGenus=\"Xylophylax\">Rhimphoctona (Xylophylax) rufocoxalis Clement (1924)</taxonomicName>"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("<taxonomicName authority=\"(Clement, 1924)\" authorityName=\"Clement\" authorityYear=\"1924\" class=\"Insecta\" family=\"Ichneumonidae\" genus=\"Rhimphoctona\" kingdom=\"Animalia\" lsidName=\"Rhimphoctona (Xylophylax) rufocoxalis\" order=\"Hymenoptera\" pageId=\"0\" pageNumber=\"1047\" phylum=\"Arthropoda\" rank=\"species\" species=\"rufocoxalis\" subGenus=\"Xylophylax\">Rhimphoctona (Xylophylax) rufocoxalis (Clement, 1924) Agosti, 2017</taxonomicName>"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("<taxonomicName authority=\"(Clement, 1924)\" authorityName=\"Clement\" authorityYear=\"1924\" class=\"Insecta\" family=\"Ichneumonidae\" genus=\"Rhimphoctona\" kingdom=\"Animalia\" lsidName=\"Rhimphoctona (Xylophylax) rufocoxalis\" order=\"Hymenoptera\" pageId=\"0\" pageNumber=\"1047\" phylum=\"Arthropoda\" rank=\"species\" species=\"rufocoxalis\" subGenus=\"Xylophylax\">Rhimphoctona (Xylophylax) rufocoxalis (Clement, 1924) Agosti (2017)</taxonomicName>"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("<taxonomicName authority=\"(Clement, 1924)\" authorityName=\"Clement\" authorityYear=\"1924\" class=\"Insecta\" family=\"Ichneumonidae\" genus=\"Rhimphoctona\" kingdom=\"Animalia\" lsidName=\"Rhimphoctona (Xylophylax) rufocoxalis\" order=\"Hymenoptera\" pageId=\"0\" pageNumber=\"1047\" phylum=\"Arthropoda\" rank=\"species\" species=\"rufocoxalis\" subGenus=\"Xylophylax\">Rhimphoctona (Xylophylax) rufocoxalis (Clement (1924)) Agosti (2017)</taxonomicName>"));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("<taxonomicName authority=\"(Clement, 1924)\" authorityName=\"Clement\" authorityYear=\"1924\" class=\"Insecta\" family=\"Ichneumonidae\" genus=\"Rhimphoctona\" kingdom=\"Animalia\" lsidName=\"Rhimphoctona (Xylophylax) rufocoxalis\" order=\"Hymenoptera\" pageId=\"0\" pageNumber=\"1047\" phylum=\"Arthropoda\" rank=\"species\" species=\"rufocoxalis\" subGenus=\"Xylophylax\">Rhimphoctona (Xylophylax) rufocoxalis (Imag &amp; Inary ex Clement (1924)) Agosti (2017)</taxonomicName>"));
		Annotation taxonName = doc.getAnnotations(TAXONOMIC_NAME_ANNOTATION_TYPE)[0];
		System.out.println(getVerbatimAuthority(null, taxonName));
		System.out.println(getCombinationAuthority(null, taxonName));
		System.out.println(" ==> " + parseAuthority(getCombinationAuthority(null, taxonName)));
		System.out.println(getBasionymAuthority(null, taxonName));
		System.out.println(" ==> " + parseAuthority(getBasionymAuthority(null, taxonName)));
		
//		TaxonomicName taxName = new TaxonomicName("animalia");
//		taxName.setEpithet("genus", "Drosiphila");
//		taxName.setEpithet("subGenus", "Morrisia");
//		taxName.setEpithet("species", "melanogaster");
//		taxName.setEpithet("subSpecies", "agostii");
//		taxName.setAuthority("Sautter", 2013);
//		System.out.println(taxName.toString());
//		System.out.println(taxName.toString(true));
//		String dwcXml = toDwcXml(taxName);
//		System.out.println(dwcXml);
//		taxName = dwcXmlToTaxonomicName(SgmlDocumentReader.readDocument(new StringReader(dwcXml)), "animalia"); 
//		System.out.println(taxName.toString());
//		System.out.println(taxName.toString(true));
//		dwcXml = toSimpleDwcXml(taxName);
//		System.out.println(dwcXml);
//		taxName = dwcXmlToTaxonomicName(SgmlDocumentReader.readDocument(new StringReader(dwcXml)), "animalia"); 
//		System.out.println(taxName.toString());
//		System.out.println(taxName.toString(true));
	}
}
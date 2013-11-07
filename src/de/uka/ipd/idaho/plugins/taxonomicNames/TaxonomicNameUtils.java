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

import java.util.Iterator;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
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
		private String rank;
		private String stringWithoutAuthority;
		private String stringWithAuthority;
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
			this.stringWithoutAuthority = null;
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
			this.stringWithoutAuthority = null;
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
			return this.toString(false);
		}
		
		/**
		 * Convert the taxonomic name into a string, using the formatting
		 * functionality of the embedded rank system.
		 * @param includeAuthority add authority information if given?
		 * @return a string representation of the taxonomic name
		 * @see java.lang.Object#toString()
		 */
		public String toString(boolean includeAuthority) {
			
			//	check cache
			if (includeAuthority && (this.stringWithAuthority != null))
				return this.stringWithAuthority;
			if (!includeAuthority && (this.stringWithoutAuthority != null))
				return this.stringWithoutAuthority;
			
			//	determine rank
			String rank = this.getRank();
			if (rank == null)
				return "";
			Rank ownRank = this.rankSystem.getRank(rank);
			if (ownRank == null)
				return "";
			Rank genusRank = this.rankSystem.getRank(GENUS_ATTRIBUTE);
			StringBuffer string = new StringBuffer();
			
			//	above genus or genus, only use most significant epithet
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
					if (epithet != null) {
						if (string.length() != 0)
							string.append(' ');
						string.append(ranks[r].formatEpithet(epithet));
					}
				}
			}
			
			//	addd authority if asked to
			if (includeAuthority && (this.authorityName != null) && (string.length() != 0)) {
				string.append(' ');
				string.append(this.getAuthority());
			}
			
			//	cache what we got
			if (includeAuthority)
				this.stringWithAuthority = string.toString();
			else this.stringWithoutAuthority = string.toString();
			
			//	finally ...
			return string.toString();
		}
		
		/**
		 * Convert the name into DarwinCore XML, additional ranks suplemented by
		 * DarwinCore-Ranks.
		 * @return the name as DarwinCore XML
		 */
		public String toDwcXml() {
			return TaxonomicNameUtils.toDwcXml(this);
		}
		
		/**
		 * Convert the name into Simple DarwinCore XML, additional ranks
		 * suplemented by DarwinCore-Ranks.
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
		
		String authorityName = ((String) taxNameAnnot.getAttribute(AUTHORITY_NAME_ATTRIBUTE));
		String authorityYear = ((String) taxNameAnnot.getAttribute(AUTHORITY_YEAR_ATTRIBUTE));
		
		if (authorityName == null) {
			String authority = ((String) taxNameAnnot.getAttribute(AUTHORITY_ATTRIBUTE));
			if (authority == null)
				return taxName;
			
			TokenSequence authorityTokens = Gamta.newTokenSequence(authority, taxNameAnnot.getTokenizer());
			int authorityNameEndIndex = authorityTokens.size();
			
			for (int t = authorityTokens.size()-1; t >= 0; t--)
				if (authorityTokens.valueAt(t).matches("[12][0-9]{3}")) {
					authorityYear = authorityTokens.valueAt(t);
					authorityNameEndIndex = t;
				}
			if ((0 < authorityNameEndIndex) && "(".equals(authorityTokens.valueAt(authorityNameEndIndex-1)))
				authorityNameEndIndex--;
			if (0 < authorityNameEndIndex)
				authorityName = TokenSequenceUtils.concatTokens(authorityTokens, 0, authorityNameEndIndex, false, true);
		}
		
		if (authorityName == null)
			return taxName;
		
		taxName.setAuthorityName(authorityName);
		if ((authorityYear != null) && authorityYear.matches("[12][0-9]{3}"))
			taxName.setAuthorityYear(Integer.parseInt(authorityYear));
		
		return taxName;
	}
	
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
				String authority = dwcEpithets[e].getValue();
				if (dwcEpithets[e].lastValue().matches("[12][0-9]{3}"))
					taxName.setAuthority(authority.substring(0, (authority.length()-4)).trim(), Integer.parseInt(dwcEpithets[e].lastValue()));
				else taxName.setAuthorityName(authority);
			}
		}
		return taxName;
	}
	
	/**
	 * Convert a name into DarwinCore XML, additional ranks suplemented by
	 * DarwinCore-Ranks.
	 * @param taxName the name to convert
	 * @return the argument name as Simple DarwinCore XML
	 */
	public static String toDwcXml(TaxonomicName taxName) {
		return toXml(taxName, ranksToDwcElementsClassic);
	}
	
	/**
	 * Convert a name into Simple DarwinCore XML, additional ranks suplemented
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
			String authority = taxName.getAuthority();
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
//	//	TEST ONLY !!!
//	public static void main(String[] args) throws Exception {
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
//	}
}
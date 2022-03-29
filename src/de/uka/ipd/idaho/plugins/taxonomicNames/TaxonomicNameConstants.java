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

import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;

/**
 * Constants for the markup of taxonomic names, namely for ranks, corresponding
 * epithets, rank indicators, authorities, etc.
 * 
 * @author sautter
 */
public interface TaxonomicNameConstants extends LiteratureConstants {
	
	/** annotation type for marking taxonomic names, namely 'taxonomicName' */
	public static final String TAXONOMIC_NAME_ANNOTATION_TYPE = "taxonomicName";
	
	/** annotation type for marking taxon status labels like 'spec. nov.' and related things, namely 'taxonomicNameLabel' */
	public static final String TAXONOMIC_NAME_LABEL_ANNOTATION_TYPE = "taxonomicNameLabel";
	
	/** annotation type for marking vernacular names of taxa, namely 'vernacularName' */
	public static final String VERNACULAR_NAME_ANNOTATION_TYPE = "vernacularName";
	
	/** attribute for storing the rank of a taxonomic name or a part thereof, namely 'rank' */
	public static final String RANK_ATTRIBUTE = "rank";
	
	/** attribute for storing the full authority string of a taxonomic name, as given in a document, namely 'authority' */
	public static final String AUTHORITY_ATTRIBUTE = "authority";
	
	/** attribute for storing the name part of the authority of a taxonomic name, namely 'authorityName' */
	public static final String AUTHORITY_NAME_ATTRIBUTE = "authorityName";
	
	/** attribute for storing the year part of the authority of a taxonomic name, namely 'authorityYear' */
	public static final String AUTHORITY_YEAR_ATTRIBUTE = "authorityYear";
	
	/** attribute for storing the name part of the basionym authority of a taxonomic name, namely 'baseAuthorityName' */
	public static final String BASE_AUTHORITY_NAME_ATTRIBUTE = "baseAuthorityName";
	
	/** attribute for storing the year part of the basionym authority of a taxonomic name, namely 'baseAuthorityYear' */
	public static final String BASE_AUTHORITY_YEAR_ATTRIBUTE = "baseAuthorityYear";
	
	/** attribute for storing the (primary) LSID of a taxonomic name, namely 'LSID' */
	public static final String LSID_ATTRIBUTE = "LSID";
	
	/** prefix for attributes storing LSIDs of a taxonomic name, namely 'LSID-'; client code should append an acronym identifying the LSID authority to this prefix to generate unambiguous attribute names */
	public static final String LSID_ATTRIBUTE_PREFIX = "LSID-";
	
	/** attribute for storing the status a taxonomic name has in a specific usage in a document, e.g. 'spec. nov.', namely 'status' */
	public static final String TAXONOMIC_NAME_STATUS_ATTRIBUTE = "status";
	
	/** attribute for marking a taxonomic name as a hybrid species, namely 'isHybrid' */
	public static final String HYBRID_MARKER_ATTRIBUTE = "isHybrid";
	
	public static final String DOMAIN_ATTRIBUTE = "domain";
	
	public static final String SUPERKINGDOM_ATTRIBUTE = "superKingdom";
	public static final String KINGDOM_ATTRIBUTE = "kingdom";
	public static final String SUBKINGDOM_ATTRIBUTE = "subKingdom";
	public static final String INFRAKINGDOM_ATTRIBUTE = "infraKingdom";
	
	public static final String SUPERPHYLUM_ATTRIBUTE = "superPhylum";
	public static final String PHYLUM_ATTRIBUTE = "phylum";
	public static final String SUBPHYLUM_ATTRIBUTE = "subPhylum";
	public static final String INFRAPHYLUM_ATTRIBUTE = "infraPhylum";
	
	public static final String SUPERCLASS_ATTRIBUTE = "superClass";
	public static final String CLASS_ATTRIBUTE = "class";
	public static final String SUBCLASS_ATTRIBUTE = "subClass";
	public static final String INFRACLASS_ATTRIBUTE = "infraClass";
	
	public static final String SUPERORDER_ATTRIBUTE = "superOrder";
	public static final String ORDER_ATTRIBUTE = "order";
	public static final String SUBORDER_ATTRIBUTE = "subOrder";
	public static final String INFRAORDER_ATTRIBUTE = "infraOrder";
	
	public static final String SUPERFAMILY_ATTRIBUTE = "superFamily";
	public static final String FAMILY_ATTRIBUTE = "family";
	public static final String SUBFAMILY_ATTRIBUTE = "subFamily";
	public static final String INFRAFAMILY_ATTRIBUTE = "infraFamily";
	public static final String SUPERTRIBE_ATTRIBUTE = "superTribe";
	public static final String TRIBE_ATTRIBUTE = "tribe";
	public static final String SUBTRIBE_ATTRIBUTE = "subTribe";
	public static final String INFRATRIBE_ATTRIBUTE = "infraTribe";
	
	public static final String GENUS_ATTRIBUTE = "genus";
	public static final String SUBGENUS_ATTRIBUTE = "subGenus";
	public static final String INFRAGENUS_ATTRIBUTE = "infraGenus";
	public static final String SECTION_ATTRIBUTE = "section";
	public static final String SUBSECTION_ATTRIBUTE = "subSection";
	public static final String SERIES_ATTRIBUTE = "series";
	public static final String SUBSERIES_ATTRIBUTE = "subSeries";
	
	public static final String SPECIESAGGREGATE_ATTRIBUTE = "speciesAggregate";
	public static final String SPECIES_ATTRIBUTE = "species";
	public static final String SUBSPECIES_ATTRIBUTE = "subSpecies";
	public static final String INFRASPECIES_ATTRIBUTE = "infraSpecies";
	public static final String VARIETY_ATTRIBUTE = "variety";
	public static final String SUBVARIETY_ATTRIBUTE = "subVariety";
	public static final String FORM_ATTRIBUTE = "form";
	public static final String SUBFORM_ATTRIBUTE = "subForm";
}

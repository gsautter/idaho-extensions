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
package de.uka.ipd.idaho.plugins.materialsCitations;


import de.uka.ipd.idaho.gamta.util.constants.LocationConstants;

/**
 * Constants bearer for collecting event / materials citation markup.
 * 
 * @author sautter
 */
public interface MaterialsCitationConstants extends LocationConstants {
	
	/** the annotation type for marking collecting countries */
	public static final String COLLECTING_COUNTRY_ANNOTATION_TYPE = "collectingCountry";
	
	/** the annotation type for marking groups of collecting regions */
	public static final String COLLECTING_REGION_GROUP_ANNOTATION_TYPE = "collectingRegionGroup";
	
	/** the annotation type for marking collecting regions */
	public static final String COLLECTING_REGION_ANNOTATION_TYPE = "collectingRegion";
	
	/** the annotation type for marking collecting counties (if given) */
	public static final String COLLECTING_COUNTY_ANNOTATION_TYPE = "collectingCounty";
	
	/** the annotation type for marking collecting municipalities (if given) */
	public static final String COLLECTING_MUNICIPALITY_ANNOTATION_TYPE = "collectingMunicipality";
	
	/** the annotation type for marking groups of collecting events */
	public static final String MATERIALS_CITATION_GROUP_ANNOTATION_TYPE = "materialsCitationGroup";
	
	/** the annotation type for marking collecting events */
	public static final String MATERIALS_CITATION_ANNOTATION_TYPE = "materialsCitation";
	
	/** the attribute name for attributing the collecting event with the name of the actual collecting municipality */
	public static final String MUNICIPALITY_ATTRIBUTE = "municipality";
	
	/** the attribute name for attributing the collecting event with the name of the actual collecting location */
	public static final String LOCATION_NAME_ATTRIBUTE = "location";
	
	/** the attribute holding the numerical depth of a collecting event in a respective annotation (e.g. the depth below a water surface) */
	public static final String DEPTH_ATTRIBUTE = "depth";
	
	/** the annotation type for marking a collection code within a collecting event, at the same time the attribute name for attributing the collecting event with the collection code */
	public static final String COLLECTION_CODE = "collectionCode";
	
	/** the annotation type for marking the collector within a collecting event, at the same time the attribute name for attributing the collecting event with the collector's name */
	public static final String COLLECTOR_NAME = "collectorName";
	
	/** the annotation type for marking the determiner of collected specimens within a collecting event, at the same time the attribute name for attributing the collecting event with the determiner's name */
	public static final String DETERMINER_NAME = "determinerName";
	
	/** the annotation type for marking the collecting date within a collecting event, at the same time the attribute name for attributing the collecting event with the collecting date */
	public static final String COLLECTING_DATE = "collectingDate";
	
	/** the attribute name for attributing a collection code or collecting event with the collection name */
	public static final String COLLECTION_NAME_ATTRIBUTE = "collectionName";
	
	/** the annotation type for marking the specimen count within a collecting event, at the same time the attribute name for attributing the collecting event with the specimen code */
	public static final String SPECIMEN_COUNT = "specimenCount";
	
	/** the annotation type for marking the specimen code within a collecting event, at the same time the attribute name for attributing the collecting event with the specimen code */
	public static final String SPECIMEN_CODE = "specimenCode";
	
	/** the annotation type for marking the collecting method within a collecting event, at the same time the attribute name for attributing the collecting event with the collecting method */
	public static final String COLLECTING_METHOD = "collectingMethod";
	
	/** the annotation type for marking the type status within a collecting event, at the same time the attribute name for attributing the collecting event with the type status */
	public static final String TYPE_STATUS = "typeStatus";
}

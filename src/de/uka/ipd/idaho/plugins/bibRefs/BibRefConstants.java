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
package de.uka.ipd.idaho.plugins.bibRefs;

import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;

/**
 * Constants for the markup of bibliographic references, namely for respective
 * details like authors and title.
 * 
 * @author sautter
 */
public interface BibRefConstants extends LiteratureConstants {
	
	/** annotation type for marking the author of a referenced publication */
	public static final String AUTHOR_ANNOTATION_TYPE = "author";
	
	/** attribute to an indicator that the (first) author(s) of a referenced publication ar the same ones as in the previous reference, holding the actual names */
	public static final String REPEATED_AUTHORS_ATTRIBUTE = "repeatedAuthors";	
	
	/** annotation type for marking the year a referenced publication was published */
	public static final String YEAR_ANNOTATION_TYPE = "year";
	
	/** annotation type for marking the exact date a publication was published (if available) */
	public static final String PUBLICATION_DATE_ANNOTATION_TYPE = "pubDate";
	
	/** annotation type for marking the title of a referenced publication */
	public static final String TITLE_ANNOTATION_TYPE = "title";
	
	/** annotation type for marking the name of the journal a referenced article appeared in, or the publisher of a book, used during parsing */
	public static final String JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE = "journalOrPublisher";
	
	/** annotation type for marking the name of the journal an article appeared in */
	public static final String JOURNAL_NAME_ANNOTATION_TYPE = "journal";
	
	/** annotation type for marking the name of the publisher of a book */
	public static final String PUBLISHER_ANNOTATION_TYPE = "publisher";
	
	/** annotation type for marking the location a book was published, or a conference took place */
	public static final String LOCATION_ANNOTATION_TYPE = "location";
	
	/** annotation type for marking the page number or page range of a referenced publication */
	public static final String PAGINATION_ANNOTATION_TYPE = "pagination";
	
	/** annotation type for marking the a part designator of a referenced publication, be it a number, a volume number, an issue number, a fascicle number, or a series designator */
	public static final String PART_DESIGNATOR_ANNOTATION_TYPE = "part";
	
	/** annotation type for marking the volume designator of a referenced publication that appeared in a journal, not to use for the volume number for multi-volume books */
	public static final String VOLUME_DESIGNATOR_ANNOTATION_TYPE = "volume";
	
	/** annotation type for marking the issue designator of a referenced publication that appeared in a journal */
	public static final String ISSUE_DESIGNATOR_ANNOTATION_TYPE = "issue";
	
	/** annotation type for marking the numero designator of a referenced publication that appeared in a journal */
	public static final String NUMERO_DESIGNATOR_ANNOTATION_TYPE = "numero";
	
	/** annotation type for marking the title of a volume a referenced publication is a part of */
	public static final String VOLUME_TITLE_ANNOTATION_TYPE = "volumeTitle";
	
	/** annotation type for marking the editor of a volume a referenced publication is a part of */
	public static final String EDITOR_ANNOTATION_TYPE = "editor";
	
	/** annotation type for marking detail information on a book, like the number of pages, tables, figures, etc. */
	public static final String BOOK_CONTENT_INFO_ANNOTATION_TYPE = "bookContentInfo";
	
	/** attribute for storing a URL a referenced publication can be retrieved from */
	public static final String PUBLICATION_URL_ANNOTATION_TYPE = "publicationUrl";
	
	/** attribute for storing a DOI of a referenced publication */
	public static final String PUBLICATION_DOI_ANNOTATION_TYPE = "DOI";
	
	/** attribute for storing an ISBN of a referenced book or book chapter */
	public static final String PUBLICATION_ISBN_ANNOTATION_TYPE = "ISBN";
	
	/** attribute for storing an identifier of a referenced publication */
	public static final String PUBLICATION_IDENTIFIER_ANNOTATION_TYPE = "ID";
	
	
	/** the type of publication a reference refers to */
	public static final String PUBLICATION_TYPE_ATTRIBUTE = TYPE_ATTRIBUTE;
	
	/** the reference type name for journal articles */
	public static final String JOURNAL_ARTICEL_REFERENCE_TYPE = "journal article";
	
	/** the reference type name for entire journal volumes */
	public static final String JOURNAL_VOLUME_REFERENCE_TYPE = "journal volume";
	
	/** the reference type name for book chapters */
	public static final String BOOK_CHAPTER_REFERENCE_TYPE = "book chapter";
	
	/** the reference type name for entire books */
	public static final String BOOK_REFERENCE_TYPE = "book";
	
	/** the reference type name for publications in conference proceedings */
	public static final String PROCEEDINGS_PAPER_REFERENCE_TYPE = "proceedings paper";
	
	/** the reference type name for entire conference proceedings */
	public static final String PROCEEDINGS_REFERENCE_TYPE = "proceedings";
	
	/** the reference type name for generic online documents that do not match any of the more specific types */
	public static final String URL_REFERENCE_TYPE = "url";
}

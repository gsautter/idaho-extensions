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
package de.uka.ipd.idaho.gamta.util.imaging;

import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;

/**
 * This interface provides constants for handling page images.
 * 
 * @author sautter
 */
public interface ImagingConstants extends LiteratureConstants {
	
	/** the format of the images, namely 'png' */
	public static final String IMAGE_FORMAT = "png";
	
	/**
	 * the attribute holding the name of the image of a page, to retrieve that
	 * image from an ImageProvider.
	 */
	public static final String IMAGE_NAME_ATTRIBUTE = "imageName";
	
	/**
	 * the attribute holding the resolution of the image of a page, to indicate
	 * relations.
	 */
	public static final String IMAGE_DPI_ATTRIBUTE = "imageDpi";
	
	/**
	 * the attribute indicating that the content of a page was created via OCR,
	 * as opposed to being extracted from a textual, born-digital PDF.
	 */
	public static final String IS_OCRED_ATTRIBUTE = "isOcred";	
	
	/**
	 * the annotation type for marking word blocks in page images.
	 */
	public static final String BLOCK_ANNOTATION_TYPE = "block";
	
	/**
	 * the annotation type for marking columns in page images.
	 */
	public static final String COLUMN_ANNOTATION_TYPE = "column";
	
	/**
	 * the annotation type for marking regions in page images.
	 */
	public static final String REGION_ANNOTATION_TYPE = "region";
	
	/**
	 * the annotation type for marking lines of words in page images.
	 */
	public static final String LINE_ANNOTATION_TYPE = "line";
	
	/**
	 * the attribute holding the baseline of a word, or the average baseline of
	 * the words in a line.
	 */
	public static final String BASELINE_ATTRIBUTE = "baseline";
	
	/**
	 * the attribute holding the (average) font size of the words in a line or
	 * paragraph.
	 */
	public static final String FONT_SIZE_ATTRIBUTE = "fontSize";
	
	/**
	 * the attribute indicating that a word is in bold face.
	 */
	public static final String BOLD_ATTRIBUTE = "bold";
	
	/**
	 * the attribute indicating that a word is in italics.
	 */
	public static final String ITALICS_ATTRIBUTE = "italics";
	
	/**
	 * the attribute holding the bounding box for a layout-related annotation
	 * after scanning. This is useful for extracting an image of a specific part
	 * of a page.
	 */
	public static final String BOUNDING_BOX_ATTRIBUTE = "box";
	
	/**
	 * the annotation type for marking word bounding boxes in page images.
	 */
	public static final String WORD_ANNOTATION_TYPE = "word";
	
	/**
	 * the attribute holding the string value of a word annotation.
	 */
	public static final String STRING_ATTRIBUTE = "str";
	
	/**
	 * the attribute holding the average line height in an individual paragraph
	 * or block.
	 */
	public static final String LINE_HEIGHT_ATTRIBUTE = "lineHeight";
	
	/**
	 * the attribute holding the indentation of an individual paragraph or the
	 * paragraphs in a block. For individual paragraphs, this is 'indent',
	 * 'none', or 'exdent', for blocks, it also can be 'mixed'.
	 */
	public static final String INDENTATION_ATTRIBUTE = "indentation";
	
	/**
	 * the value to the indentation attribute indicating that the first line of
	 * a paragraph starts to the right of the other lines.
	 */
	public static final String INDENTATION_INDENT = "indent";
	
	/**
	 * the value to the indentation attribute indicating that the first line of
	 * a paragraph starts where the other lines start.
	 */
	public static final String INDENTATION_NONE = "none";
	
	/**
	 * the value to the indentation attribute indicating that the first line of
	 * a paragraph starts to the left of the other lines.
	 */
	public static final String INDENTATION_EXDENT = "exdent";
	
	/**
	 * the value to the indentation attribute indicating that the paragraphs in
	 * a block do not follow a uniform indentation scheme.
	 */
	public static final String INDENTATION_MIXED = "mixed";
	
	/**
	 * the attribute holding the text orientation of an individual paragraph or
	 * the paragraphs in a block. For individual paragraphs, this is 'center',
	 * 'justified', 'left', or 'right', for blocks, it also can be 'mixed'.
	 */
	public static final String TEXT_ORIENTATION_ATTRIBUTE = "textOrientation";
	
	/**
	 * the value to the text orientation attribute indicating that the lines of
	 * a paragraph are oriented towards the left side.
	 */
	public static final String TEXT_ORIENTATION_LEFT = "left";
	
	/**
	 * the value to the text orientation attribute indicating that the lines of
	 * a paragraph are oriented towards the right side.
	 */
	public static final String TEXT_ORIENTATION_RIGHT = "right";
	
	/**
	 * the value to the text orientation attribute indicating that the lines of
	 * a paragraph are justified to both sides.
	 */
	public static final String TEXT_ORIENTATION_JUSTIFIED = "justified";
	
	/**
	 * the value to the text orientation attribute indicating that the lines of
	 * a paragraph are centered.
	 */
	public static final String TEXT_ORIENTATION_CENTERED = "centered";
	
	/**
	 * the value to the orientation attribute indicating that the paragraphs in
	 * a block do not follow a uniform text orientation scheme.
	 */
	public static final String TEXT_ORIENTATION_MIXED = "mixed";
	
	/**
	 * generic variable to put into URLs in situations where the actual source
	 * authority and path of an image are not clear, namely '@imageBasePath'.
	 * This variable facilitates inserting authority and path dynamically in a
	 * web front-end that knows these data.
	 */
	public static final String IMAGE_BASE_PATH_VARIABLE = "@imageBasePath";
}

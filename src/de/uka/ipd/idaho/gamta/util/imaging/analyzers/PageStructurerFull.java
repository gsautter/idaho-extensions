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
package de.uka.ipd.idaho.gamta.util.imaging.analyzers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.constants.TableConstants;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.imaging.analyzers.PageStructurerFull.PageCleanupFeedbackPanel.RegionBox;
import de.uka.ipd.idaho.gamta.util.imaging.analyzers.PageStructurerFull.PageCleanupFeedbackPanel.WordBox;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * @author sautter
 *
 */
public class PageStructurerFull extends AbstractConfigurableAnalyzer implements ImagingConstants, TableConstants {
	
	private static final String PAGE_RANGE_ANNOTATION_TYPE = "pageRange";
	
	private static final String REGION_ANNOTATION_TYPE = "region";
	
//	private static final String PAGE_START_TOKEN_TYPE = "pageStartToken";
//	
	private static final String CONTINUE_ATTRIBUTE = "continue";
	
	//	subSection types for complex rules
	private static final String MAIN_TEXT_TYPE = "mainText";
	private static final String CONTINUE_MAIN_TEXT_TYPE = "continueMainText";
	private static final String CONTINUE_FOOTNOTE_TYPE = "continueFootnote";
	private static final String HEADING_TYPE = "heading";
	private static final String TABLE_TYPE = "table";
	private static final String OCR_ARTIFACT_TYPE = "ocrArtifact";
	private static final String COLUMN_TYPE = "column";
	
	//	categories for complex rules
	private static final String MAIN_TEXT_LABEL = "Main Text";
	private static final String CONTINUE_MAIN_TEXT_LABEL = "Continue Main Text";
	private static final String CAPTION_LABEL = "Caption";
	private static final String FOOTNOTE_LABEL = "Footnote";
	private static final String CONTINUE_FOOTNOTE_LABEL = "Continue Footnote";
//	private static final String CITATION_LABEL = "Citation / Bibliographic Reference";
	private static final String BIB_REF_LABEL = "Bibliographic Reference";
	private static final String PAGE_TITLE_LABEL = "Page Header / Footer";
	private static final String HEADING_LABEL = "Heading (of Chapter, Section, etc)";
	private static final String TABLE_LABEL = "Table";
	private static final String OCR_ARTIFACT_LABEL = "OCR / Layout Artifact";
	private static final String COLUMN_LABEL = "Text Column (for grouping only)";
	
	private static final String[] BOX_TYPES = {
		MAIN_TEXT_TYPE,
		CONTINUE_MAIN_TEXT_TYPE,
		HEADING_TYPE,
		PAGE_TITLE_TYPE,
		CAPTION_TYPE,
		TABLE_TYPE,
		FOOTNOTE_TYPE,
		CONTINUE_FOOTNOTE_TYPE,
//		CITATION_TYPE,
		BIBLIOGRAPHIC_REFERENCE_TYPE,
		OCR_ARTIFACT_TYPE,
	};
	
	private static HashSet mainTextTypes = new HashSet();
	private static HashSet mainTextBridgeableTypes = new HashSet();
	private static HashSet footnoteTypes = new HashSet();
	private static HashSet footnoteBridgeableTypes = new HashSet();
	static {
		mainTextTypes.add(MAIN_TEXT_TYPE);
//		mainTextTypes.add(CITATION_TYPE);
		mainTextTypes.add(BIBLIOGRAPHIC_REFERENCE_TYPE);
		mainTextTypes.add(HEADING_TYPE);
		
		mainTextBridgeableTypes.add(FOOTNOTE_TYPE);
		mainTextBridgeableTypes.add(CAPTION_TYPE);
		mainTextBridgeableTypes.add(TABLE_TYPE);
		mainTextBridgeableTypes.add(PAGE_TITLE_TYPE);
		mainTextBridgeableTypes.add(OCR_ARTIFACT_TYPE);
		
		footnoteTypes.add(FOOTNOTE_TYPE);
		
		footnoteBridgeableTypes.add(MAIN_TEXT_TYPE);
		footnoteBridgeableTypes.add(CONTINUE_MAIN_TEXT_TYPE);
//		footnoteBridgeableTypes.add(CITATION_TYPE);
		footnoteBridgeableTypes.add(BIBLIOGRAPHIC_REFERENCE_TYPE);
		footnoteBridgeableTypes.add(HEADING_TYPE);
		footnoteBridgeableTypes.add(CAPTION_TYPE);
		footnoteBridgeableTypes.add(TABLE_TYPE);
		footnoteBridgeableTypes.add(PAGE_TITLE_TYPE);
		footnoteBridgeableTypes.add(OCR_ARTIFACT_TYPE);
	}
	
	private Properties boxTypeLabels = new Properties();
	private String getBoxTypeLabel(String type) {
		String label = this.boxTypeLabels.getProperty(type);
		if (label != null) return label;
		
		if (MAIN_TEXT_TYPE.equals(type)) return MAIN_TEXT_LABEL;
		else if (CONTINUE_MAIN_TEXT_TYPE.equals(type)) return CONTINUE_MAIN_TEXT_LABEL;
		else if (CAPTION_TYPE.equals(type)) return CAPTION_LABEL;
		else if (FOOTNOTE_TYPE.equals(type)) return FOOTNOTE_LABEL;
		else if (CONTINUE_FOOTNOTE_TYPE.equals(type)) return CONTINUE_FOOTNOTE_LABEL;
//		else if (CITATION_TYPE.equals(type)) return CITATION_LABEL;
		else if (BIBLIOGRAPHIC_REFERENCE_TYPE.equals(type)) return BIB_REF_LABEL;
		else if (PAGE_TITLE_TYPE.equals(type)) return PAGE_TITLE_LABEL;
		else if (HEADING_TYPE.equals(type)) return HEADING_LABEL;
		else if (OCR_ARTIFACT_TYPE.equals(type)) return OCR_ARTIFACT_LABEL;
		else if (TABLE_TYPE.equals(type)) return TABLE_LABEL;
		else if (COLUMN_TYPE.equals(type)) return COLUMN_LABEL;
		else return MAIN_TEXT_LABEL;
	}
	
	private HashMap boxTypeColors = new HashMap();
	private Color getTypeColor(String type) {
		Color color = ((Color) this.boxTypeColors.get(type));
		if (color == null) {
			if (MAIN_TEXT_TYPE.equals(type))
				color = FeedbackPanel.brighten(Color.LIGHT_GRAY);
			else if (CONTINUE_MAIN_TEXT_TYPE.equals(type))
				color = Color.LIGHT_GRAY;
			else if (CAPTION_TYPE.equals(type))
				color = Color.PINK;
			else if (TABLE_TYPE.equals(type))
				color = FeedbackPanel.brighten(Color.BLUE);
			else if (FOOTNOTE_TYPE.equals(type))
				color = FeedbackPanel.brighten(Color.ORANGE);
			else if (CONTINUE_FOOTNOTE_TYPE.equals(type))
				color = Color.ORANGE;
//			else if (CITATION_TYPE.equals(type))
//				color = Color.RED;
			else if (BIBLIOGRAPHIC_REFERENCE_TYPE.equals(type))
				color = Color.RED;
			else if (PAGE_TITLE_TYPE.equals(type))
				color = Color.GREEN;
			else if (HEADING_TYPE.equals(type))
				color = Color.CYAN;
			else if (OCR_ARTIFACT_TYPE.equals(type))
				color = Color.DARK_GRAY;
			else color = new Color(Color.HSBtoRGB(((float) Math.random()), 0.5f, 1.0f));
			this.boxTypeColors.put(type, color);
		}
		return color;
	}
	
	private String feedbackFontName = "Serif";//"Times New Roman";
	
	private String remoteImageProvider = (IMAGE_BASE_PATH_VARIABLE + "/");
	/* TODOne (that's absolutely future work)
correct absurdly misOCRed words:
- create mapping file of characters to letters, similar to that of new page number extractor
- analyze individual word elements
  - for analysis, ignore (or even cut) leading and tailing punctuation marks that do not require a space after or before them, respectively, e.g. a tailing comma or a leading opening bracket
  - replace embedded punctuation marks with mapped letters
    - produce all possible versions
  - ... and look up resulting word(s) in rest of document (no use using dictionaries, as at this point, we have no clue about document language)
  ==> if lookup successful, correct embedded punctuation marks to most likely letters
    - use trigrams (extract from document) to determine likelyhood of individual replacements
    
- DO NOT correct misOCRings, that's future work (reOCRing the whole thing from the page images and then correcting it using the approach scatched now)
	 */
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		
		//	load type colors
		try {
			InputStream is = this.dataProvider.getInputStream("boxTypes.txt");
			StringVector typeLines = StringVector.loadList(is);
			is.close();
			
			for (int t = 0; t < typeLines.size(); t++) {
				String typeLine = typeLines.get(t).trim();
				if ((typeLine.length() != 0) && !typeLine.startsWith("//")) {
					String[] typeData = typeLine.split("\\s", 3);
					if (typeData.length != 3)
						continue;
					String type = typeData[0];
					Color color = FeedbackPanel.getColor(typeData[1]);
					String label = typeData[2];
					this.boxTypeColors.put(type, color);
					this.boxTypeLabels.setProperty(type, label);
				}
			}
		} catch (IOException ioe) {}
		
		//	load character mappings
		try {
			InputStream is = this.dataProvider.getInputStream("pageNumberCharacters.txt");
			StringVector characterLines = StringVector.loadList(is);
			is.close();
			
			TreeSet pageNumberCharacters = new TreeSet();
			for (int l = 0; l < characterLines.size(); l++) {
				String characterLine = characterLines.get(l).trim();
				if ((characterLine.length() != 0) && !characterLine.startsWith("//")) {
					int split = characterLine.indexOf(' ');
					if (split == -1)
						continue;
					String digit = characterLine.substring(0, split).trim();
					String characters = characterLine.substring(split).trim();
					if (Gamta.isNumber(digit)) {
						for (int c = 0; c < characters.length(); c++) {
							String character = characters.substring(c, (c+1));
							pageNumberCharacters.add(character);
							ArrayList characterTranslations = ((ArrayList) this.pageNumberCharacterTranslations.get(character));
							if (characterTranslations == null) {
								characterTranslations = new ArrayList(2);
								this.pageNumberCharacterTranslations.put(character, characterTranslations);
							}
							characterTranslations.add(digit);
						}
						pageNumberCharacters.add(digit);
						ArrayList characterTranslations = ((ArrayList) this.pageNumberCharacterTranslations.get(digit));
						if (characterTranslations == null) {
							characterTranslations = new ArrayList(2);
							this.pageNumberCharacterTranslations.put(digit, characterTranslations);
						}
						characterTranslations.add(digit);
					}
				}
			}
			for (int d = 0; d < Gamta.DIGITS.length(); d++)
				pageNumberCharacters.add(Gamta.DIGITS.substring(d, (d+1)));
			
			StringBuffer pncPatternBuilder = new StringBuffer();
			for (Iterator cit = pageNumberCharacters.iterator(); cit.hasNext();) {
				String pnc = ((String) cit.next());
				pncPatternBuilder.append(pnc);
				ArrayList characterTranslations = ((ArrayList) this.pageNumberCharacterTranslations.get(pnc));
				if (characterTranslations == null)
					continue;
				int[] cts = new int[characterTranslations.size()];
				for (int t = 0; t < characterTranslations.size(); t++)
					cts[t] = Integer.parseInt((String) characterTranslations.get(t));
				this.pageNumberCharacterTranslations.put(pnc, cts);
			}
			String pncPattern = ("[" + RegExUtils.escapeForRegEx(pncPatternBuilder.toString()) + "]");
			this.pageNumberPatternFuzzy = (pncPattern + "+(\\s?" + pncPattern + "+)*");
		}
		catch (IOException ioe) {
			this.pageNumberPatternFuzzy = "[0-9]++";
		}
		
		this.pageNumberPatternStrict = "[1-9][0-9]++";
		
		this.feedbackFontName = this.getParameter("feedbackFontName", this.feedbackFontName);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	get pages
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		
		//	any pages?
		if (pages.length == 0)
			return;
		
		//	sort out empty pages / group non-empty ones into page ranges
		ArrayList pageRanges = new ArrayList();
		ArrayList pageRange = new ArrayList();
		for (int p = 0; p < pages.length; p++) {
			MutableAnnotation[] words = pages[p].getMutableAnnotations(WORD_ANNOTATION_TYPE);
			if (words.length == 0) {
				if (pageRange.size() != 0) {
					MutableAnnotation[] pageRangePages = ((MutableAnnotation[]) pageRange.toArray(new MutableAnnotation[pageRange.size()]));
					pageRanges.add(data.addAnnotation(PAGE_RANGE_ANNOTATION_TYPE, pageRangePages[0].getStartIndex(), (pageRangePages[pageRangePages.length - 1].getEndIndex() - pageRangePages[0].getStartIndex())));
				}
				pageRange.clear();
			}
			else pageRange.add(pages[p]);
		}
		
		//	we have all the pages in one range
		if (pageRange.size() == pages.length)
			this.processPageRange(data, parameters, null);
		
		//	we have smaller page ranges
		else {
			
			//	mark last page range (if any)
			if (pageRange.size() != 0) {
				MutableAnnotation[] pageRangePages = ((MutableAnnotation[]) pageRange.toArray(new MutableAnnotation[pageRange.size()]));
				pageRanges.add(data.addAnnotation(PAGE_RANGE_ANNOTATION_TYPE, pageRangePages[0].getStartIndex(), (pageRangePages[pageRangePages.length - 1].getEndIndex() - pageRangePages[0].getStartIndex())));
			}
			
			//	do we have anythintg to process?
			if (pageRanges.isEmpty())
				return;
			
			//	process page ranges individually
			for (int r = 0; r < pageRanges.size(); r++) {
				boolean cancelled = this.processPageRange(((MutableAnnotation) pageRanges.get(r)), parameters, ((pageRanges.size() == 1) ? null : ("Page Range " + (r+1) + " of " + pageRanges.size())));
				if (cancelled)
					break;
			}
		}
		
		//	clean up
		AnnotationFilter.removeAnnotations(data, PAGE_RANGE_ANNOTATION_TYPE);
	}
	
	private boolean processPageRange(MutableAnnotation data, Properties parameters, String pageRangeLabel) {
		
		//	get pages
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		
		//	sort out empty pages
		ArrayList pageList = new ArrayList();
		for (int p = 0; p < pages.length; p++) {
			MutableAnnotation[] words = pages[p].getMutableAnnotations(WORD_ANNOTATION_TYPE);
			if (words.length != 0)
				pageList.add(pages[p]);
		}
		if (pageList.size() < pages.length)
			pages = ((MutableAnnotation[]) pageList.toArray(new MutableAnnotation[pageList.size()]));
		
		//	any pages left?
		if (pages.length == 0)
			return false;
		
		//	clean up line starts
		for (int p = 0; p < pages.length; p++) {
			MutableAnnotation[] lines = pages[p].getMutableAnnotations(LINE_ANNOTATION_TYPE);
			for (int l = 1; l < (lines.length - 1); l++) {
				int startAbove = getLineStart(lines[l-1]);
				int startBelow = getLineStart(lines[l+1]);
				int lineStart = getLineStart(lines[l]);
				if ((startAbove == -1) || (startBelow == -1) || (lineStart == -1))
					continue;
				Annotation[] words = lines[l].getAnnotations(WORD_ANNOTATION_TYPE);
				while (((lineStart < startAbove) || (lineStart < startBelow)) && (words.length != 0) && !Gamta.spaceBefore(words[0].firstValue())) {
					System.out.println("Removing line start artifact on page " + p + ": " + words[0].firstValue());
					lines[l].removeTokensAt(0, 1);
					words = lines[l].getAnnotations(WORD_ANNOTATION_TYPE);
					lineStart = getLineStart(lines[l]);
				}
			}
		}
		
		//	TODOne correct absurdly misOCRed words here (that's absolutely future work)
		
		//	re-compute bounding boxes of lines and paragraphs
		aggregateBoxes(data, WORD_ANNOTATION_TYPE, LINE_ANNOTATION_TYPE, false);
		aggregateBoxes(data, LINE_ANNOTATION_TYPE, MutableAnnotation.PARAGRAPH_TYPE, false);
		
		//	run page number detection
		this.detectPageNumbers(pages);
		
		//	we're not allowed to get feedback right now, so there's nothing further we could do
		if (!parameters.containsKey(INTERACTIVE_PARAMETER))
			return false;
		
		//	mark remaining paragraph start lines & build feedback panels
		PageCleanupFeedbackPanel[] scfps = new PageCleanupFeedbackPanel[pages.length];
		HashMap[] wordsById = new HashMap[pages.length];
		for (int p = 0; p < pages.length; p++) {
			wordsById[p] = new HashMap();
			scfps[p] = this.getFeedbackPanel(pages[p], wordsById[p]);
		}
		
		//	get feedback (watch out for cancellations in desktop use)
		int cutoff = pages.length;
		int cutoffPageId = Integer.parseInt((String) pages[pages.length - 1].getAttribute(PAGE_ID_ATTRIBUTE));
		
		//	can we issue all dialogs at once?
		if (FeedbackPanel.isMultiFeedbackEnabled())
			FeedbackPanel.getMultiFeedback(scfps);
		
		//	display dialogs one by one otherwise (allow cancel in the middle)
		else for (int p = 0; p < pages.length; p++) {
			if (p != 0)
				scfps[p].addButton("Previous");
			scfps[p].addButton("Cancel");
			scfps[p].addButton("OK" + (((p+1) == scfps.length) ? "" : " & Next"));
			
			String title = scfps[p].getTitle();
			scfps[p].setTitle(title + " - (" + (p+1) + " of " + scfps.length + ((pageRangeLabel == null) ? "" : pageRangeLabel) + ")");
			
			System.out.println("Getting feedback");
			String f = scfps[p].getFeedback();
			if (f == null) f = "Cancel";
			
			scfps[p].setTitle(title);
			
			//	current dialog submitted, go on
			if (f.startsWith("OK")) {}
			
			//	back to previous dialog
			else if ("Previous".equals(f))
				p-=2;
			
			//	cancel from current dialog on
			else {
				cutoff = p;
				cutoffPageId = ((p == 0) ? -1 : Integer.parseInt((String) pages[p-1].getAttribute(PAGE_ID_ATTRIBUTE))); 
				p = pages.length;
			}
		}
		
		//	process all feedback data together
		System.out.println("Processing feedback on " + pages.length + " pages");
		TreeSet removedPages = new TreeSet();
		for (int p = 0; p < cutoff; p++) {
			System.out.println(" - page " + p + ", ranging " + pages[p].getStartIndex() + " to " + pages[p].getEndIndex() + ": " + AnnotationUtils.produceStartTag(pages[p]));
			boolean remove = this.processFeedback(scfps[p], pages[p], wordsById[p]);
			if (remove) {
				removedPages.add(new Integer(p));
				System.out.println("   --> empty, marked for removal");
			}
			System.out.println("   --> range is " + pages[p].getStartIndex() + " to " + pages[p].getEndIndex() + ": " + AnnotationUtils.produceStartTag(pages[p]));
		}
		
		//	remove pages if necessary
		System.out.println("Got " + pages.length + " pages, removing " + removedPages.size());
		if (removedPages.size() != 0) {
			while (removedPages.size() != 0) {
				Integer p = ((Integer) removedPages.last());
				System.out.println(" - removing page " + p + ", ranging " + pages[p.intValue()].getStartIndex() + " to " + pages[p.intValue()].getEndIndex() + ": " + AnnotationUtils.produceStartTag(pages[p.intValue()]));
				data.removeTokens(pages[p.intValue()]);
				removedPages.remove(p);
			}
			pages = data.getMutableAnnotations(PAGE_TYPE);
			System.out.println(" ==> got " + pages.length + " pages left");
		}
		
		//	add bounding boxes to regions
		aggregateBoxes(data, WORD_ANNOTATION_TYPE, REGION_ANNOTATION_TYPE, false);
		
		//	store page ID and page number for tokens
		for (int p = 0; p < pages.length; p++) {
			System.out.println("Doing page " + p + ", ranging " + pages[p].getStartIndex() + " to " + pages[p].getEndIndex() + ": " + AnnotationUtils.produceStartTag(pages[p]));
			String pageId = ((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE));
			System.out.println(" - page ID is " + pageId);
			String pageNumber = ((String) pages[p].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			System.out.println(" - page number is " + pageNumber);
			for (int t = 0; t < pages[p].size(); t++) {
				pages[p].tokenAt(t).setAttribute(PAGE_ID_ATTRIBUTE, pageId);
				pages[p].tokenAt(t).setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			}
		}
		
		//	restore regions
		this.restoreRegions(data, cutoffPageId);
		
		//	collect region starts
		MutableAnnotation[] regions = data.getMutableAnnotations(REGION_ANNOTATION_TYPE);
		HashMap regionStartsAndTypes = new HashMap();
		for (int r = 0; r < regions.length; r++)
			regionStartsAndTypes.put(new Integer(regions[r].getStartIndex()), regions[r].getAttribute(TYPE_ATTRIBUTE));
		
		//	annotate paragraphs
		int paragraphStart = 0;
		String paragraphType = null;
		for (int t = 0; t < data.size(); t++) {
			Integer index = new Integer(t);
			
			//	this token ends a paragraph
			if (regionStartsAndTypes.containsKey(index)) {
				
				//	annotate paragraph
				if (paragraphType != null) {
					MutableAnnotation paragraph = data.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, paragraphStart, (t - paragraphStart));
					paragraph.setAttribute(TYPE_ATTRIBUTE, paragraphType);
					paragraph.setAttribute(PAGE_ID_ATTRIBUTE, ((String) paragraph.firstToken().getAttribute(PAGE_ID_ATTRIBUTE)));
					paragraph.setAttribute(PAGE_NUMBER_ATTRIBUTE, ((String) paragraph.firstToken().getAttribute(PAGE_NUMBER_ATTRIBUTE)));
					paragraph.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
					System.out.println("Got paragraph: " + paragraph.toXML());
				}
				
				//	remember start of next paragraph
				paragraphStart = t;
				paragraphType = ((String) regionStartsAndTypes.get(index));
			}
		}
		
		//	annotate last paragraph
		if (paragraphType != null) {
			MutableAnnotation paragraph = data.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, paragraphStart, (data.size() - paragraphStart));
			paragraph.setAttribute(TYPE_ATTRIBUTE, paragraphType);
			paragraph.setAttribute(PAGE_ID_ATTRIBUTE, ((String) paragraph.firstToken().getAttribute(PAGE_ID_ATTRIBUTE)));
			paragraph.setAttribute(PAGE_NUMBER_ATTRIBUTE, ((String) paragraph.firstToken().getAttribute(PAGE_NUMBER_ATTRIBUTE)));
			paragraph.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
			System.out.println("Got last paragraph: " + paragraph.toXML());
		}
		
		//	mark page break tokens
		String lastPageId = "";
		String lastPageNumber = "";
		HashSet seenPageIds = new HashSet();
		HashSet seenPageNumbers = new HashSet();
		for (int t = 0; t < data.size(); t++) {
			Token token = data.tokenAt(t);
			String pageId = ((String) token.getAttribute(PAGE_ID_ATTRIBUTE, lastPageId));
			String pageNumber = ((String) token.getAttribute(PAGE_NUMBER_ATTRIBUTE, lastPageNumber));
			if (((lastPageId == null) || lastPageId.equals(pageId)) && ((lastPageNumber == null) || lastPageNumber.equals(pageNumber)))
				continue;
			Annotation pageBreak = data.addAnnotation(PAGE_BREAK_TOKEN_TYPE, t, 1);
			pageBreak.setAttribute(PAGE_ID_ATTRIBUTE, pageId);
			lastPageId = pageId;
			pageBreak.setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumber);
			lastPageNumber = pageNumber;
			if (seenPageIds.add(pageId) | seenPageNumbers.add(pageNumber))
				pageBreak.setAttribute(PAGE_START_ATTRIBUTE, PAGE_START_ATTRIBUTE);
		}
		
		//	clean up
		AnnotationFilter.removeDuplicates(data, PAGE_BREAK_TOKEN_TYPE);
		
		//	remove page ID and page number from tokens
		for (int t = 0; t < data.size(); t++) {
			data.tokenAt(t).removeAttribute(PAGE_ID_ATTRIBUTE);
			data.tokenAt(t).removeAttribute(PAGE_NUMBER_ATTRIBUTE);
		}
		
		//	remove main text regions, and regions that cover exactly one paragraph
		for (int r = 0; r < regions.length; r++) {
			MutableAnnotation[] paragraphs = regions[r].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
			if (paragraphs.length == 1) {
				paragraphs[0].copyAttributes(regions[r]);
				data.removeAnnotation(regions[r]);
			}
			else if (MAIN_TEXT_TYPE.equals(regions[r].getAttribute(TYPE_ATTRIBUTE))) {
				if (paragraphs.length != 0)
					paragraphs[0].copyAttributes(regions[r]);
				data.removeAnnotation(regions[r]);
			}
			else regions[r].changeTypeTo(MutableAnnotation.SUB_SUB_SECTION_TYPE);
		}
		
		//	handle tables
		MutableAnnotation[] ssss = data.getMutableAnnotations(MutableAnnotation.SUB_SUB_SECTION_TYPE);
		for (int s = 0; s < ssss.length; s++) {
			if (TABLE_TYPE.equals(ssss[s].getAttribute(TYPE_ATTRIBUTE))) {
				this.buildTable(ssss[s]);
				ssss[s].changeTypeTo(TABLE_TYPE);
			}
		}
		
		//	TODO figure out if keeping words really neccessary, or if paragraphs are sufficient
		
		//	remove lines, columns, and pages
//		AnnotationFilter.removeAnnotations(data, WORD_ANNOTATION_TYPE); KEEP WORDS TO FACILITATE LATER RE-OCR
		AnnotationFilter.removeAnnotations(data, LINE_ANNOTATION_TYPE);
		AnnotationFilter.removeAnnotations(data, COLUMN_ANNOTATION_TYPE);
		AnnotationFilter.removeAnnotations(data, PAGE_TYPE);
		
		//	 normalize paragraphs
		Gamta.normalizeParagraphStructure(data);
		
		//	strip unnecessary details from words
		AnnotationFilter.removeAnnotationAttribute(data, WORD_ANNOTATION_TYPE, PAGE_NUMBER_ATTRIBUTE);
		AnnotationFilter.removeAnnotationAttribute(data, WORD_ANNOTATION_TYPE, PAGE_ID_ATTRIBUTE);
		
		//	add annotations and attributes where necessary
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			
			//	add special annotations (caption, footnote, citation, etc)
			String type = ((String) paragraphs[p].getAttribute(TYPE_ATTRIBUTE));
//			if (CAPTION_TYPE.equals(type) || FOOTNOTE_TYPE.equals(type) || CITATION_TYPE.equals(type))
//				data.addAnnotation(type, paragraphs[p].getStartIndex(), paragraphs[p].size()).copyAttributes(paragraphs[p]);
			if (CAPTION_TYPE.equals(type) || FOOTNOTE_TYPE.equals(type) || BIBLIOGRAPHIC_REFERENCE_TYPE.equals(type))
				data.addAnnotation(type, paragraphs[p].getStartIndex(), paragraphs[p].size()).copyAttributes(paragraphs[p]);
			
			//	set last page number attribute of paragraphs where needed
			Annotation[] pageBreaks = paragraphs[p].getAnnotations(PAGE_BREAK_TOKEN_TYPE);
			if (pageBreaks.length == 0)
				continue;
			
			String fpid = ((String) paragraphs[p].getAttribute(PAGE_ID_ATTRIBUTE));
			String lpid = ((String) pageBreaks[pageBreaks.length-1].getAttribute(PAGE_ID_ATTRIBUTE));
			if ((fpid != null) && !fpid.equals(lpid))
				paragraphs[p].setAttribute(LAST_PAGE_ID_ATTRIBUTE, lpid);
			String fpn = ((String) paragraphs[p].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			String lpn = ((String) pageBreaks[pageBreaks.length-1].getAttribute(PAGE_NUMBER_ATTRIBUTE));
			if ((fpn != null) && !fpn.equals(lpn))
				paragraphs[p].setAttribute(LAST_PAGE_NUMBER_ATTRIBUTE, lpn);
		}
		
		//	was any of the feedback dialogs cancelled?
		return (cutoff < pages.length);
	}
	
	private PageCleanupFeedbackPanel getFeedbackPanel(MutableAnnotation page, HashMap wordsById) {
		
		//	create and configure feedback panel
		PageCleanupFeedbackPanel pcfp = new PageCleanupFeedbackPanel("Check Page Structure", remoteImageProvider, page.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE), ((String) page.getAttribute(PAGE_ID_ATTRIBUTE)));
		pcfp.setLabel("<HTML>Plase make sure the boxes in this page reflect the logical paragraphs, and also specify their type:" +
				"<BR>- <B>main text</B> paragraph, the <B>continuation</B> of one, or a <B>heading</B>, e.g. of a section or chapter" +
				"<BR>- <B>footnote</B>, the <B>continuation</B> of one from an earlier page or column, or a <B>bibliographic reference</B>" +
				"<BR>- <B>caption</B>, e.g. of a figure or table, an actual <B>table</B> in itself, a <B>page header or footer</B>" +
				"<BR>- <B>OCR artifact</B> that is not actually text at all, but rather a stain on the page, a hand written mark, etc" +
				"<BR>Use boxes of type <B>Column</B> to group paragraphs into page columns." +
				"<BR>Manipulate a box through the buttons in its corners: <B>R</B> for resizing, <B>X</B> for removing, <B>T</B> for changing the type." +
				"<BR>Draw a new box by pressing, dragging, and then releasing the left mouse button.</HTML>");
		for (int t = 0; t < BOX_TYPES.length; t++)
			pcfp.addBoxType(BOX_TYPES[t], this.getBoxTypeLabel(BOX_TYPES[t]), this.getTypeColor(BOX_TYPES[t]));
		pcfp.addBoxType(COLUMN_TYPE, this.getBoxTypeLabel(COLUMN_TYPE), this.getTypeColor(COLUMN_TYPE));
		
		//	add words
		MutableAnnotation[] lines = page.getMutableAnnotations(LINE_ANNOTATION_TYPE);
		for (int l = 0; l < lines.length; l++) {
			int fontSize = Integer.parseInt((String) lines[l].getAttribute(FONT_SIZE_ATTRIBUTE, "-1"));
			int dpi = Integer.parseInt((String) page.getAttribute(IMAGE_DPI_ATTRIBUTE, "72"));
			fontSize = ((fontSize * dpi) / 72);
			Annotation[] words = lines[l].getAnnotations(WORD_ANNOTATION_TYPE);
			for (int w = 0; w < words.length; w++) {
				String wordId = ("W" + wordsById.size());
				wordsById.put(wordId, words[w]);
				BoundingBox bb = BoundingBox.getBoundingBox(words[w]);
				int baseline = Integer.parseInt((String) words[w].getAttribute(BASELINE_ATTRIBUTE, "-1"));
				if (baseline < 0)
					baseline = bb.top + (((bb.bottom - bb.top) * 4) / 5); // looks like a pretty good estimate
				String str = ((String) words[w].getAttribute(STRING_ATTRIBUTE));
				str = (((str == null) || (str.trim().length() == 0)) ? words[w].getValue() : str);
				int fontStyle = Font.PLAIN;
				if (words[w].hasAttribute(BOLD_ATTRIBUTE))
					fontStyle = (fontStyle | Font.BOLD);
				if (words[w].hasAttribute(ITALICS_ATTRIBUTE))
					fontStyle = (fontStyle | Font.ITALIC);
				if (str.length() != 0)
					pcfp.addWordBox(bb.left, bb.top, (bb.right - bb.left), (bb.bottom - bb.top), baseline, str, wordId, false, this.feedbackFontName, fontStyle, fontSize);
			}
		}
//		for (int w = 0; w < words.length; w++) {
//			String wordId = ("W" + w);
//			wordsById.put(wordId, words[w]);
//			BoundingBox bb = BoundingBox.getBoundingBox(words[w]);
//			int baseline = Integer.parseInt((String) words[w].getAttribute(BASELINE_ATTRIBUTE, "0"));
//			String str = ((String) words[w].getAttribute(STRING_ATTRIBUTE));
//			str = ((str == null) ? words[w].getValue() : str);
//			int fontStyle = Font.PLAIN;
//			if (words[w].hasAttribute(BOLD_ATTRIBUTE))
//				fontStyle = (fontStyle | Font.BOLD);
//			if (words[w].hasAttribute(ITALICS_ATTRIBUTE))
//				fontStyle = (fontStyle | Font.ITALIC);
//			if (str.length() != 0)
//				pcfp.addWordBox(bb.left, bb.top, (bb.right - bb.left), (bb.bottom - bb.top), baseline, str, wordId, false, fontName, fontStyle, fontSize);
//		}
		
		//	compute average dots per character to recognize headings
		Annotation[] words = page.getAnnotations(WORD_ANNOTATION_TYPE);
		int averageDotsPerChar = this.computeAverageDotsPerChar(words);
		
		//	work paragraph-wise
		MutableAnnotation[] paragraphs = page.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	compute average font size
		int pageFontSizeSum = 0;
		int pageFontSizeCount = 0;
		for (int p = 0; p < paragraphs.length; p++) {
			String pfsStr = ((String) paragraphs[p].getAttribute(FONT_SIZE_ATTRIBUTE));
			if (pfsStr != null) try {
				int pfs = Integer.parseInt(pfsStr);
				if ((pfs >= this.minTextFontSize) && (pfs <= this.maxTextFontSize)) {
					pageFontSizeSum += (pfs * paragraphs[p].size());
					pageFontSizeCount += paragraphs[p].size();
				}
			} catch (NumberFormatException nfe) {}
		}
		int avgPageFontSize = ((pageFontSizeCount == 0) ? 0 : ((pageFontSizeSum + (pageFontSizeCount / 2)) / pageFontSizeCount));
		
		//	get page resolution
		int dpi = -1;
		try {
			dpi = Integer.parseInt((String) page.getAttribute(IMAGE_DPI_ATTRIBUTE, "-1"));
		} catch (NumberFormatException nfe) {}
		
		//	add region boxes
		String previousType = null;
		for (int p = 0; p < paragraphs.length; p++) {
			BoundingBox bb = BoundingBox.getBoundingBox(paragraphs[p]);
			System.out.println("Adding paragraph " + paragraphs[p].toXML());
			String type = ((String) paragraphs[p].getAttribute(TYPE_ATTRIBUTE));
			if (type == null)
				type = this.getType(paragraphs[p], averageDotsPerChar, previousType, avgPageFontSize, dpi);
			System.out.println(" - type is " + type);
			previousType = type;
			System.out.println(" - adding region box");
			pcfp.addRegionBox(bb.left, bb.top, (bb.right - bb.left), (bb.bottom - bb.top), type);
			System.out.println(" - added");
		}
		
		//	check if we need column boxes
		MutableAnnotation[] columns = page.getMutableAnnotations(COLUMN_ANNOTATION_TYPE);
		boolean addColumns = false;
		for (int c = 0; !addColumns && (c < columns.length); c++) {
			BoundingBox cbb = BoundingBox.getBoundingBox(columns[c]);
			for (int cc = (c+1); !addColumns && (cc < columns.length); cc++) {
				BoundingBox ccbb = BoundingBox.getBoundingBox(columns[cc]);
				if ((cbb.top < ccbb.bottom) && (cbb.bottom > ccbb.top) && ((cbb.right < ccbb.left) || (cbb.left > ccbb.right)))
					addColumns = true;
			}
		}
		
		//	add column boxes if required
		if (addColumns)
			for (int c = 0; c < columns.length; c++) {
				paragraphs = columns[c].getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
				if (paragraphs.length < 2)
					continue;
				BoundingBox bb = BoundingBox.getBoundingBox(columns[c]);
				pcfp.addRegionBox(bb.left-8, bb.top-8, (bb.right - bb.left + 16), (bb.bottom - bb.top + 16), COLUMN_TYPE);
			}
		
		//	add background information
		pcfp.setProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY, page.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE));
		pcfp.setProperty(FeedbackPanel.TARGET_PAGE_NUMBER_PROPERTY, ((String) page.getAttribute(PAGE_NUMBER_ATTRIBUTE)));
		pcfp.setProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY, ((String) page.getAttribute(PAGE_ID_ATTRIBUTE)));
		pcfp.setProperty(FeedbackPanel.TARGET_ANNOTATION_ID_PROPERTY, page.getAnnotationID());
		pcfp.setProperty(FeedbackPanel.TARGET_ANNOTATION_TYPE_PROPERTY, PAGE_TYPE);
		pcfp.setProperty(FeedbackPanel.TARGET_PAGES_PROPERTY, ((String) page.getAttribute(PAGE_NUMBER_ATTRIBUTE)));
		pcfp.setProperty(FeedbackPanel.TARGET_PAGE_IDS_PROPERTY, ((String) page.getAttribute(PAGE_ID_ATTRIBUTE)));
		
		//	return the pane
		return pcfp;
	}
	
	private String getType(MutableAnnotation paragraph, int pageAverageDotsPerChar, String previousType, int avgPageFontSize, int dpi) {
		Annotation[] indicators;
		indicators = paragraph.getAnnotations(FOOTNOTE_TYPE);
		if (indicators.length != 0) {
			System.out.println("Footnote indicator");
			return ((footnoteBridgeableTypes.contains(previousType) && this.isContinued(paragraph)) ? CONTINUE_FOOTNOTE_TYPE : FOOTNOTE_TYPE);
		}
		indicators = paragraph.getAnnotations(CAPTION_TYPE);
		if (indicators.length != 0) {
			System.out.println("Caption indicator");
			return CAPTION_TYPE;
		}
		indicators = paragraph.getAnnotations(PAGE_TITLE_TYPE);
		if (indicators.length != 0) {
			System.out.println("Page header indicator");
			return PAGE_TITLE_TYPE;
		}
		indicators = paragraph.getAnnotations(PAGE_NUMBER_TYPE);
		if (indicators.length != 0) {
			System.out.println("Page number indicator");
			return PAGE_TITLE_TYPE;
		}
		indicators = paragraph.getAnnotations(TABLE_TYPE);
		if (indicators.length != 0) {
			System.out.println("Table indicator");
			return TABLE_TYPE;
		}
//		indicators = paragraph.getAnnotations(CITATION_TYPE);
//		if ((indicators.length != 0) && (indicators[0].size() > 10)) {
//			for (int t = 0; t < indicators[0].size(); t++)
//				if (indicators[0].valueAt(t).matches("[1-2][0-9]{3}")) {
//					System.out.println("Citation indicator");
//					return CITATION_TYPE;
//				}
//		}
		indicators = paragraph.getAnnotations(BIBLIOGRAPHIC_REFERENCE_TYPE);
		if ((indicators.length != 0) && (indicators[0].size() > 10)) {
			for (int t = 0; t < indicators[0].size(); t++)
				if (indicators[0].valueAt(t).matches("[1-2][0-9]{3}")) {
					System.out.println("BibRef indicator");
					return BIBLIOGRAPHIC_REFERENCE_TYPE;
				}
		}
		
		
		int lineCount = paragraph.getAnnotations(LINE_ANNOTATION_TYPE).length;
		
		//	extract word strings
		Annotation[] words = paragraph.getAnnotations(WORD_ANNOTATION_TYPE);
		String[] strs = new String[words.length];
		for (int w = 0; w < words.length; w++) {
			String str = ((String) words[w].getAttribute(STRING_ATTRIBUTE));
			if ((str != null) && (str.length() != 0))
				strs[w] = str;
			else strs[w] = null;
			if (w == 0) System.out.println("First word is " + strs[w]);
		}
		
		//	try and identify page headers based on extremely wide word spacing
		if ((lineCount < 3) && (0 < dpi)) {
			BoundingBox[] wbs = new BoundingBox[words.length];
			for (int w = 0; w < words.length; w++)
				wbs[w] = BoundingBox.getBoundingBox(words[w]);
			int minWordMargin = Integer.MAX_VALUE;
			int maxWordMargin = 0;
			int wordMarginSum = 0;
			int wordMarginCount = 0;
			for (int w = 1; w < wbs.length; w++) {
				if ((wbs[w-1] == null) || (wbs[w] == null))
					continue;
				int wm = (wbs[w].left - wbs[w-1].right);
				if (wm < 1)
					continue;
				minWordMargin = Math.min(minWordMargin, wm);
				maxWordMargin = Math.max(maxWordMargin, wm);
				wordMarginSum += wm;
				wordMarginCount++;
			}
			
			//	check for extremely large whitespace blocks
			if (dpi < maxWordMargin) {
				System.out.println("Page header inch word margin");
				return PAGE_TITLE_TYPE;
			}
			
			//	check for extreme variation in margin width
			if (wordMarginCount != 0) {
				int avgWordMargin = ((wordMarginSum + (wordMarginCount / 2)) / wordMarginCount);
				if ((avgWordMargin * 5) < maxWordMargin) {
					System.out.println("Page header wide word margin");
					return PAGE_TITLE_TYPE;
				}
			}
		}
		
		//	check for caption starts TODO put caption starts in config file
		if ((strs.length != 0) && (strs[0] != null) && (strs[0].length() != 0) && (lineCount <= this.captionMaxLines)) {
			String str = (strs[0].matches("[A-Z].*") ? strs[0].toLowerCase() : strs[0].toUpperCase());
			boolean isCaption = false;
			if (str.matches("(fig(ure|[\\.\\,])).*"))
				isCaption = true;
			else if (str.matches("table.*"))
				isCaption = true;
			else if (str.matches("(diag(ram|[\\.\\,])).*"))
				isCaption = true;
			else if (str.matches("map.*"))
				isCaption = true;
			else if (str.matches("plate.*"))
				isCaption = true;
			if (isCaption) {
				System.out.println("Caption start");
				return CAPTION_TYPE;
			}
		}
		
		//	check for bibliographic references
		String iStr = ((String) paragraph.getAttribute(INDENTATION_ATTRIBUTE));
		if (INDENTATION_EXDENT.equals(iStr) && (lineCount <= this.citationMaxLines)) {
			for (int s = 0; s < strs.length; s++)
				if ((strs[s] != null) && strs[s].matches(".*[12][0-9]{3}.*")) {
					System.out.println("Citation indent and year");
//					return CITATION_TYPE;
					return BIBLIOGRAPHIC_REFERENCE_TYPE;
				}
		}
		
		//	get font size, alignment, and justification
		String pfsStr = ((String) paragraph.getAttribute(FONT_SIZE_ATTRIBUTE));
		if (pfsStr != null) try {
			int pfs = Integer.parseInt(pfsStr);
			System.out.println("Average page font size is " + avgPageFontSize + ", paragraph font size is " + pfs);
			if ((pfs < 1) || (pfs > 200)) {} // ignore absurd font sizes
			else if (pfs > this.maxTextFontSize) {
				System.out.println(" ==> Artifact giant font");
				return OCR_ARTIFACT_TYPE;
			}
			else if (((avgPageFontSize+1) < pfs) && (this.minTextFontSize <= avgPageFontSize)) {
				System.out.println(" ==> Heading large font");
				return HEADING_TYPE;
			}
			else if (pfs < this.minTextFontSize) {
				System.out.println(" ==> Artifact mini font");
				return OCR_ARTIFACT_TYPE;
			}
		} catch (NumberFormatException nfe) {}
		
		String toStr = ((String) paragraph.getAttribute(TEXT_ORIENTATION_ATTRIBUTE));
		if (TEXT_ORIENTATION_CENTERED.equals(toStr)) {
			System.out.println("Heading text orientation");
			return HEADING_TYPE;
		}
//		
//		if (words.length < 10) {
//			int averageDotsPerChar = this.computeAverageDotsPerChar(words);
//			if ((averageDotsPerChar * 8) > (pageAverageDotsPerChar * 10))
//				return HEADING_TYPE;
//		}
		
		return ((((previousType == null) || mainTextBridgeableTypes.contains(previousType)) && this.isContinued(paragraph)) ? CONTINUE_MAIN_TEXT_TYPE : MAIN_TEXT_TYPE);
	}
	private boolean isContinued(MutableAnnotation paragraph) {
		Annotation[] words = paragraph.getAnnotations(WORD_ANNOTATION_TYPE);
		if (words.length == 0)
			return false;
		String firstStr = ((String) words[0].getAttribute(STRING_ATTRIBUTE));
		if ((firstStr == null) || (firstStr.length() == 0))
			return false;
		return Character.isLowerCase(firstStr.charAt(0));
	}
	private int computeAverageDotsPerChar(Annotation[] words) {
		int sumWidth = 0;
		int charCount = 0;
		for (int w = 0; w < words.length; w++) {
			BoundingBox bb = BoundingBox.getBoundingBox(words[w]);
			sumWidth += (bb.right - bb.left);
			charCount += words[w].length();
		}
		return ((charCount == 0) ? 0 : (sumWidth / charCount));
	}
	
	//	TODO make all this configuration parameters
	private int minTextFontSize = 6;
	private int maxTextFontSize = 32;
	private int captionMaxLines = 4;
	private int citationMaxLines = 4;
	
	private boolean processFeedback(PageCleanupFeedbackPanel pcfp, MutableAnnotation page, HashMap wordsById) {
		MutableAnnotation rebuiltPage = Gamta.newDocument(Gamta.newTokenSequence(null, page.getTokenizer()));
		WordBox wb = pcfp.getFirstWordBox();
		HashMap rebuiltWordsById = new HashMap();
		
		//	built word sequence from scratch
		while (wb != null) {
			
			//	add current word
			int wordStart = rebuiltPage.size();
			Annotation word = ((Annotation) wordsById.get(wb.id));
			if ((rebuiltPage.size() != 0) && Gamta.insertSpace(rebuiltPage.lastValue(), word.firstValue()))
				rebuiltPage.addChar(' ');
			String wordStr = ((String) word.getAttribute(STRING_ATTRIBUTE));
			if ((wordStr == null) || (wordStr.trim().length() == 0))
				wordStr = "WW";
			rebuiltPage.addTokens(wordStr);
			Annotation rebuiltWord = rebuiltPage.addAnnotation(WORD_ANNOTATION_TYPE, wordStart, (rebuiltPage.size() - wordStart));
			rebuiltWord.copyAttributes(word);
			rebuiltWordsById.put(wb.id, rebuiltWord);
			
			//	mark line end (last token of page, or next token to the left, or next token starts new region)
			if ((wb.nextWordBox == null) || (wb.nextWordBox.left < wb.left) || (wb.nextWordBox.boxType != null))
				rebuiltPage.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
			
			//	switch to next word
			wb = wb.nextWordBox;
		}
		
		//	annotate regions
		RegionBox[] rbs = pcfp.getRegionBoxes();
		for (int r = 0; r < rbs.length; r++) {
			if (COLUMN_TYPE.equals(rbs[r].boxType))
				continue;
			Annotation firstWord = ((Annotation) rebuiltWordsById.get(rbs[r].firstWordBox.id));
			Annotation lastWord = ((Annotation) rebuiltWordsById.get(rbs[r].lastWordBox.id));
			Annotation region = rebuiltPage.addAnnotation(REGION_ANNOTATION_TYPE, firstWord.getStartIndex(), (lastWord.getEndIndex() - firstWord.getStartIndex()));
			if (CONTINUE_MAIN_TEXT_TYPE.equals(rbs[r].boxType)) {
				region.setAttribute(TYPE_ATTRIBUTE, MAIN_TEXT_TYPE);
				region.setAttribute(CONTINUE_ATTRIBUTE, CONTINUE_ATTRIBUTE);
			}
			else if (CONTINUE_FOOTNOTE_TYPE.equals(rbs[r].boxType)) {
				region.setAttribute(TYPE_ATTRIBUTE, FOOTNOTE_TYPE);
				region.setAttribute(CONTINUE_ATTRIBUTE, CONTINUE_ATTRIBUTE);
			}
			else region.setAttribute(TYPE_ATTRIBUTE, rbs[r].boxType);
			region.setAttribute(PAGE_ID_ATTRIBUTE, ((String) page.getAttribute(PAGE_ID_ATTRIBUTE)));
			region.setAttribute(PAGE_NUMBER_ATTRIBUTE, ((String) page.getAttribute(PAGE_NUMBER_ATTRIBUTE)));
			region.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
		}
		
		//	delete OCR artifacts and and page titles
		MutableAnnotation[] regions = rebuiltPage.getMutableAnnotations(REGION_ANNOTATION_TYPE);
		for (int r = (regions.length - 1); r >= 0; r--) {
			String regionType = ((String) regions[r].getAttribute(TYPE_ATTRIBUTE));
			if (OCR_ARTIFACT_TYPE.equals(regionType) || PAGE_TITLE_TYPE.equals(regionType)) {
				rebuiltPage.removeTokens(regions[r]);
				regions = rebuiltPage.getMutableAnnotations(REGION_ANNOTATION_TYPE);
				r = regions.length;
			}
		}
		
		//	remove duplicates
		regions = rebuiltPage.getMutableAnnotations(REGION_ANNOTATION_TYPE);
		for (int r = 0; r < (regions.length-1); r++) {
			if (AnnotationUtils.equals(regions[r], regions[r+1], false))
				rebuiltPage.removeAnnotation(regions[r]);
		}
		
		//	inherit continue attribute upward from child regions
		regions = rebuiltPage.getMutableAnnotations(REGION_ANNOTATION_TYPE);
		for (int r = 0; r < regions.length; r++) {
			if (this.isContinueRegion(regions[r]))
				regions[r].setAttribute(CONTINUE_ATTRIBUTE, CONTINUE_ATTRIBUTE);
		}
		
		//	empty page, i.e., to remove it
		if (rebuiltPage.size() == 0)
			return true;
		
		//	keep first and last token in order to not destroy page
		int originalPageSize = page.size();
		System.out.println("   - original page size is " + originalPageSize);
		if (originalPageSize > 1)
			page.removeTokensAt(1, (page.size() - 2));
		else page.removeTokensAt(1, (page.size() - 1));
		System.out.println("   - tokens removed, page size is " + page.size());
		
		//	clean annotations
		Annotation[] pageAnnotations = page.getAnnotations();
		for (int a = 0; a < pageAnnotations.length; a++) {
			if (page.getAnnotationID().equals(pageAnnotations[a].getAnnotationID()))
				continue;
			if (PAGE_RANGE_ANNOTATION_TYPE.equals(pageAnnotations[a].getType()))
				continue;
			page.removeAnnotation(pageAnnotations[a]);
		}
		
		//	add/insert new content
		if (originalPageSize > 1)
			page.insertTokensAt(rebuiltPage, 1);
		else page.addTokens(rebuiltPage);
		System.out.println("   - " + rebuiltPage.size() + " tokens inserted, page size is " + page.size());
		
		//	finally, remove first and last token
		page.removeTokensAt(0, 1);
		if (originalPageSize > 1)
			page.removeTokensAt((page.size() - 1), 1);
		System.out.println("   - first and last tokens removed, page size is " + page.size());
		
		//	transfer token attributes
		for (int t = 0; t < rebuiltPage.size(); t++)
			page.tokenAt(t).copyAttributes(rebuiltPage.tokenAt(t));
		
		if (true) {
			System.out.println("   - page after rebuilding is:");
			for (int t = 0; t < page.size(); t++) {
				System.out.print(page.valueAt(t));
				if (page.tokenAt(t).hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))
					System.out.println();
				else System.out.print(page.getWhitespaceAfter(t));
			}
			System.out.println();
		}
		
		//	transfer annotations
		Annotation[] annotations = rebuiltPage.getAnnotations();
		System.out.println("   - transferring annotations:");
		for (int a = 0; a < annotations.length; a++) {
			Annotation added = page.addAnnotation(annotations[a].getType(), annotations[a].getStartIndex(), annotations[a].size());
			added.copyAttributes(annotations[a]);
			System.out.println(added.toXML());
		}
		
		//	indicate page content, i.e., not to remove it
		return false;
//		
//		//	mark page start tokens
//		Annotation pageStart = page.addAnnotation(PAGE_BREAK_TOKEN_TYPE, 0, 1);
//		pageStart.setAttribute(PAGE_ID_ATTRIBUTE, ((String) page.getAttribute(PAGE_ID_ATTRIBUTE)));
//		pageStart.setAttribute(PAGE_NUMBER_ATTRIBUTE, ((String) page.getAttribute(PAGE_NUMBER_ATTRIBUTE)));
//		pageStart.setAttribute(PAGE_START_ATTRIBUTE, PAGE_START_ATTRIBUTE);
	}
	
	private boolean isContinueRegion(MutableAnnotation region) {
		if (region.hasAttribute(CONTINUE_ATTRIBUTE))
			return true;
		MutableAnnotation[] childRegions = region.getMutableAnnotations(REGION_ANNOTATION_TYPE);
		if (childRegions.length == 0)
			return false;
		else if (AnnotationUtils.equals(region, childRegions[0], false))
			return ((childRegions.length != 1) && this.isContinueRegion(childRegions[1]));
		else return this.isContinueRegion(childRegions[0]);
	}
	
	private class Interval implements Comparable {
		final int tl;
		final int br;
		Interval(int tl, int br) {
			this.tl = tl;
			this.br = br;
		}
		public int compareTo(Object obj) {
			Interval i = ((Interval) obj);
			return ((this.tl == i.tl) ? (i.br - this.br) : (this.tl - i.tl));
		}
		public int hashCode() {
			return ((this.tl << 16) + this.br); // should do for all images with width/height not exceeding 2^15=32768 (everything we handle here)
		}
		public boolean equals(Object obj) {
			return (this.compareTo(obj) == 0);
		}
		public String toString() {
			return ("[" + this.tl + "," + this.br + "]");
		}
		boolean overlaps(Interval i) {
			return ((this.tl < i.br) && (i.tl < this.br));
		}
//		boolean includes(Interval i) {
//			return ((this.tl <= i.tl) && (i.br <= this.br));
//		}
		boolean liesIn(Interval i) {
			return ((i.tl <= this.tl) && (this.br <= i.br));
		}
		Interval intersect(Interval i) {
			if (this.liesIn(i))
				return this;
			else if (i.liesIn(this))
				return i;
			else if (this.overlaps(i))
				return new Interval(Math.max(this.tl, i.tl), Math.min(this.br, i.br));
			else return null;
		}
//		Interval union(Interval i) {
//			if (this.includes(i))
//				return this;
//			else if (i.includes(this))
//				return i;
//			else if (this.overlaps(i))
//				return new Interval(Math.min(this.tl, i.tl), Math.max(this.br, i.br));
//			else return null;
//		}
		Interval[] subtract(Interval i) {
			ArrayList remainder = new ArrayList(2);
			if (this.overlaps(i)) {
				if (this.tl < i.tl)
					remainder.add(new Interval(this.tl, i.tl));
				if (i.br < this.br)
					remainder.add(new Interval(i.br, this.br));
			}
			else remainder.add(this);
			return ((Interval[]) remainder.toArray(new Interval[remainder.size()]));
		}
//		boolean contains(int i) {
//			return ((this.tl <= i) && (i < this.br));
//		}
	}
	
	private static class TableCell {
		Annotation annot;
		MutableAnnotation mAnnot;
		BoundingBox bounds;
		int rowSpan = 1;
		int colSpan = 1;
		TableCell(Annotation annot, BoundingBox bounds) {
			this.annot = annot;
			this.mAnnot = null;
			this.bounds = bounds;
		}
		TableCell(MutableAnnotation annot, BoundingBox bounds) {
			this.annot = annot;
			this.mAnnot = annot;
			this.bounds = bounds;
		}
	}
	
	private static class TableRow {
		ArrayList cells = new ArrayList();
		BoundingBox bounds;
	}
	
	private static Comparator lrCellOrder = new Comparator() {
		public int compare(Object o1, Object o2) {
			TableCell c1 = ((TableCell) o1);
			TableCell c2 = ((TableCell) o2);
			return (c1.bounds.left - c2.bounds.left);
		}
	};
	
	private static Comparator tbCellOrder = new Comparator() {
		public int compare(Object o1, Object o2) {
			TableCell c1 = ((TableCell) o1);
			TableCell c2 = ((TableCell) o2);
			return (c1.bounds.top - c2.bounds.top);
		}
	};
	
	private void buildTable(MutableAnnotation table) {
		
		//	add bounding boxes to paragraphs
		aggregateBoxes(table, WORD_ANNOTATION_TYPE, MutableAnnotation.PARAGRAPH_TYPE, false);
		
		//	get cells 
		MutableAnnotation[] cellAnnots = table.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		//	TODO aggregate paragraphs marked as regions (also make sure to adjust bounding boxes)
		
		//	no cells to work with
		if (cellAnnots.length < 2)
			return;
		
		//	wrap cells and compute table dimensions
		int tLeft = Integer.MAX_VALUE;
		int tRight = 0;
		int tTop = Integer.MAX_VALUE;
		int tBottom = 0;
		TableCell[] cells = new TableCell[cellAnnots.length];
		for (int ce = 0; ce < cellAnnots.length; ce++) {
			BoundingBox cellBox = BoundingBox.getBoundingBox(cellAnnots[ce]);
			if (cellBox == null)
				return;
			cells[ce] = new TableCell(cellAnnots[ce], cellBox);
			tLeft = Math.min(tLeft, cellBox.left);
			tRight = Math.max(tRight, cellBox.right);
			tTop = Math.min(tTop, cellBox.top);
			tBottom = Math.max(tBottom, cellBox.bottom);
		}
		
		//	arrange cells in left-right and top-botton order
		TableCell[] cellsLr = new TableCell[cells.length];
		TableCell[] cellsTb = new TableCell[cells.length];
		for (int ce = 0; ce < cellAnnots.length; ce++) {
			cellsLr[ce] = cells[ce];
			cellsTb[ce] = cells[ce];
		}
		Arrays.sort(cellsLr, lrCellOrder);
		Arrays.sort(cellsTb, tbCellOrder);
		
		//	find horizontal passages (vertical gaps) between individual cells
		TreeSet hPassGapCollector = new TreeSet();
		for (int clr = 0; clr < cellsLr.length; clr++) {
			for (int cclr = (clr+1); cclr < cellsLr.length; cclr++) {
				if (cellsLr[clr].bounds.right <= cellsLr[cclr].bounds.left)
					break;
				
				//	span space between cells
				BoundingBox gap = new BoundingBox(
						Math.max(cellsLr[clr].bounds.left, cellsLr[cclr].bounds.left),
						Math.min(cellsLr[clr].bounds.right, cellsLr[cclr].bounds.right), 
						Math.min(cellsLr[clr].bounds.bottom, cellsLr[cclr].bounds.bottom), 
						Math.max(cellsLr[clr].bounds.top, cellsLr[cclr].bounds.top)
					);
				
				//	check if gap overlaps with other cells
				for (int ctb = 0; ctb < cellsTb.length; ctb++) {
					if (cellsTb[ctb].bounds.bottom <= gap.top)
						continue;
					if (gap.bottom <= cellsTb[ctb].bounds.top)
						break;
					if (overlaps(cellsTb[ctb].bounds, gap)) {
						gap = null;
						break;
					}
				}
				
				//	got overlap ==> invalid gap
				if (gap == null)
					continue;
				
				//	store interval
				hPassGapCollector.add(new Interval(Math.min(cellsLr[clr].bounds.bottom, cellsLr[cclr].bounds.bottom), Math.max(cellsLr[clr].bounds.top, cellsLr[cclr].bounds.top)));
			}
		}
		if (DEBUG_TABLES) {
			System.out.println("HPassGaps (" + hPassGapCollector.size() + " initial):");
			for (Iterator iit = hPassGapCollector.iterator(); iit.hasNext();)
				System.out.println(" - " + iit.next());
		}
		
		//	split vertical gaps around cells they span
		Interval[] hPassGaps = ((Interval[]) hPassGapCollector.toArray(new Interval[hPassGapCollector.size()]));
		hPassGapCollector.clear();
		for (int i = 0; i < hPassGaps.length; i++) {
			for (int ctb = 0; ctb < cellsTb.length; ctb++) {
				if (cellsTb[ctb].bounds.top <= hPassGaps[i].tl)
					continue;
				if (hPassGaps[i].br <= cellsTb[ctb].bounds.bottom)
					continue;
				if (hPassGaps[i].br <= cellsTb[ctb].bounds.top)
					break;
				Interval[] parts = hPassGaps[i].subtract(new Interval(cellsTb[ctb].bounds.top, cellsTb[ctb].bounds.bottom));
				if (parts.length == 2) {
					hPassGapCollector.add(parts[0]);
					hPassGaps[i] = parts[1];
				}
			}
			hPassGapCollector.add(hPassGaps[i]);
		}
		if (DEBUG_TABLES) {
			System.out.println("HPassGaps (" + hPassGapCollector.size() + " after splitting):");
			for (Iterator iit = hPassGapCollector.iterator(); iit.hasNext();)
				System.out.println(" - " + iit.next());
		}
		
		//	cut vertical gaps interleaving with cells
		hPassGaps = ((Interval[]) hPassGapCollector.toArray(new Interval[hPassGapCollector.size()]));
		hPassGapCollector.clear();
		for (int i = 0; i < hPassGaps.length; i++) {
			boolean retain = true;
			for (int ctb = 0; ctb < cellsTb.length; ctb++) {
				if (cellsTb[ctb].bounds.bottom <= hPassGaps[i].tl)
					continue;
				if (hPassGaps[i].br <= cellsTb[ctb].bounds.top)
					break;
				if (hPassGaps[i].tl < cellsTb[ctb].bounds.top) {
					hPassGapCollector.add(new Interval(hPassGaps[i].tl, cellsTb[ctb].bounds.top));
					retain = false;
				}
				if (cellsTb[ctb].bounds.bottom < hPassGaps[i].br) {
					hPassGapCollector.add(new Interval(cellsTb[ctb].bounds.bottom, hPassGaps[i].br));
					retain = false;
				}
			}
			if (retain)
				hPassGapCollector.add(hPassGaps[i]);
		}
		if (DEBUG_TABLES) {
			System.out.println("HPassGaps (" + hPassGapCollector.size() + " after cutting):");
			for (Iterator iit = hPassGapCollector.iterator(); iit.hasNext();)
				System.out.println(" - " + iit.next());
		}
		
		//	intersect overlapping vertical intervals
		hPassGaps = ((Interval[]) hPassGapCollector.toArray(new Interval[hPassGapCollector.size()]));
		hPassGapCollector.clear();
		for (int i = 0; i < hPassGaps.length; i++) {
			if ((i+1) == hPassGaps.length)
				hPassGapCollector.add(hPassGaps[i]);
			else if (hPassGaps[i].overlaps(hPassGaps[i+1]))
				hPassGaps[i+1] = hPassGaps[i].intersect(hPassGaps[i+1]);
			else hPassGapCollector.add(hPassGaps[i]);
		}
		if (DEBUG_TABLES) {
			System.out.println("HPassGaps (" + hPassGapCollector.size() + " after intersecting):");
			for (Iterator iit = hPassGapCollector.iterator(); iit.hasNext();)
				System.out.println(" - " + iit.next());
		}
		
		//	find horizontal passages (vertical gaps) between individual cells
		TreeSet vPassGapCollector = new TreeSet();
		for (int ctb = 0; ctb < cellsTb.length; ctb++) {
			for (int cctb = (ctb+1); cctb < cellsTb.length; cctb++) {
				if (cellsTb[ctb].bounds.bottom <= cellsTb[cctb].bounds.top)
					break;
				
				//	span space between cells
				BoundingBox gap = new BoundingBox(
						Math.min(cellsTb[ctb].bounds.right, cellsTb[cctb].bounds.right), 
						Math.max(cellsTb[ctb].bounds.left, cellsTb[cctb].bounds.left),
						Math.max(cellsTb[ctb].bounds.top, cellsTb[cctb].bounds.top),
						Math.min(cellsTb[ctb].bounds.bottom, cellsTb[cctb].bounds.bottom)
					);
				
				//	check if gap overlaps with other cells
				for (int clr = 0; clr < cellsLr.length; clr++) {
					if (cellsLr[clr].bounds.right <= gap.left)
						continue;
					if (gap.right <= cellsLr[clr].bounds.left)
						break;
					if (overlaps(cellsLr[clr].bounds, gap)) {
						gap = null;
						break;
					}
				}
				
				//	got overlap ==> invalid gap
				if (gap == null)
					continue;
				
				//	store interval
				vPassGapCollector.add(new Interval(Math.min(cellsTb[ctb].bounds.right, cellsTb[cctb].bounds.right), Math.max(cellsTb[ctb].bounds.left, cellsTb[cctb].bounds.left)));
			}
		}
		if (DEBUG_TABLES) {
			System.out.println("VPassGaps (" + vPassGapCollector.size() + " initial):");
			for (Iterator iit = vPassGapCollector.iterator(); iit.hasNext();)
				System.out.println(" - " + iit.next());
		}
		
		//	split vertical gaps around cells they span
		Interval[] vPassGaps = ((Interval[]) vPassGapCollector.toArray(new Interval[vPassGapCollector.size()]));
		vPassGapCollector.clear();
		for (int i = 0; i < vPassGaps.length; i++) {
			for (int clr = 0; clr < cellsLr.length; clr++) {
				if (cellsLr[clr].bounds.left <= vPassGaps[i].tl)
					continue;
				if (vPassGaps[i].br <= cellsLr[clr].bounds.right)
					continue;
				if (vPassGaps[i].br <= cellsLr[clr].bounds.left)
					break;
				Interval[] parts = vPassGaps[i].subtract(new Interval(cellsLr[clr].bounds.left, cellsLr[clr].bounds.right));
				if (parts.length == 2) {
					vPassGapCollector.add(parts[0]);
					vPassGaps[i] = parts[1];
				}
			}
			vPassGapCollector.add(vPassGaps[i]);
		}
		if (DEBUG_TABLES) {
			System.out.println("VPassGaps (" + vPassGapCollector.size() + " after splitting):");
			for (Iterator iit = vPassGapCollector.iterator(); iit.hasNext();)
				System.out.println(" - " + iit.next());
		}
		
		//	cut vertical gaps interleaving with cells
		vPassGaps = ((Interval[]) vPassGapCollector.toArray(new Interval[vPassGapCollector.size()]));
		vPassGapCollector.clear();
		for (int i = 0; i < vPassGaps.length; i++) {
			boolean retain = true;
			for (int clr = 0; clr < cellsLr.length; clr++) {
				if (cellsLr[clr].bounds.right <= vPassGaps[i].tl)
					continue;
				if (vPassGaps[i].br <= cellsLr[clr].bounds.left)
					break;
				if (vPassGaps[i].tl < cellsLr[clr].bounds.left) {
					vPassGapCollector.add(new Interval(vPassGaps[i].tl, cellsLr[clr].bounds.left));
					retain = false;
				}
				if (cellsLr[clr].bounds.right < vPassGaps[i].br) {
					vPassGapCollector.add(new Interval(cellsLr[clr].bounds.right, vPassGaps[i].br));
					retain = false;
				}
			}
			if (retain)
				vPassGapCollector.add(vPassGaps[i]);
		}
		if (DEBUG_TABLES) {
			System.out.println("VPassGaps (" + vPassGapCollector.size() + " after cutting):");
			for (Iterator iit = vPassGapCollector.iterator(); iit.hasNext();)
				System.out.println(" - " + iit.next());
		}
		
		//	intersect overlapping vertical intervals
		vPassGaps = ((Interval[]) vPassGapCollector.toArray(new Interval[vPassGapCollector.size()]));
		vPassGapCollector.clear();
		for (int i = 0; i < vPassGaps.length; i++) {
			if ((i+1) == vPassGaps.length)
				vPassGapCollector.add(vPassGaps[i]);
			else if (vPassGaps[i].overlaps(vPassGaps[i+1]))
				vPassGaps[i+1] = vPassGaps[i].intersect(vPassGaps[i+1]);
			else vPassGapCollector.add(vPassGaps[i]);
		}
		if (DEBUG_TABLES) {
			System.out.println("VPassGaps (" + vPassGapCollector.size() + " after intersecting):");
			for (Iterator iit = vPassGapCollector.iterator(); iit.hasNext();)
				System.out.println(" - " + iit.next());
		}
		
		//	line up passing gaps
		hPassGaps = ((Interval[]) hPassGapCollector.toArray(new Interval[hPassGapCollector.size()]));
		vPassGaps = ((Interval[]) vPassGapCollector.toArray(new Interval[vPassGapCollector.size()]));
		
		//	mark full passes
		boolean[] isHorizontalPass = new boolean[tBottom - tTop];
		Arrays.fill(isHorizontalPass, true);
		boolean[] isVerticalPass = new boolean[tRight - tLeft];
		Arrays.fill(isVerticalPass, true);
		for (int ce = 0; ce < cells.length; ce++) {
			for (int c = cells[ce].bounds.left; c < cells[ce].bounds.right; c++)
				isVerticalPass[c-tLeft] = false;
			for (int r = cells[ce].bounds.top; r < cells[ce].bounds.bottom; r++)
				isHorizontalPass[r-tTop] = false;
		}
		
		//	restrict horizontal pass gaps to full passes (if any)
		for (int i = 0; i < hPassGaps.length; i++) {
			int tFullPass = -1;
			int bFullPass = -1;
			for (int r = hPassGaps[i].tl; r < hPassGaps[i].br; r++)
				if (isHorizontalPass[r-tTop]) {
					if (tFullPass == -1)
						tFullPass = r;
					bFullPass = r;
				}
			if (tFullPass != -1)
				hPassGaps[i] = new Interval(tFullPass, (bFullPass+1));
		}
		if (DEBUG_TABLES) {
			System.out.println("HPassGaps (" + hPassGaps.length + " after full pass analysis):");
			for (int i = 0; i < hPassGaps.length; i++)
				System.out.println(" - " + hPassGaps[i]);
		}
		
		//	find horizontal grid lines
		TreeSet hPasses = new TreeSet();
		for (int i = 0; i < hPassGaps.length; i++)
			hPasses.add(new Integer((hPassGaps[i].tl + hPassGaps[i].br) / 2));
		if (DEBUG_TABLES) {
			System.out.println("HPasses (" + hPasses.size() + "):");
			for (Iterator pit = hPasses.iterator(); pit.hasNext();)
				System.out.println(" - " + pit.next());
		}
		
		//	restrict vertical pass gaps to full passes (if any)
		for (int i = 0; i < vPassGaps.length; i++) {
			int lFullPass = -1;
			int rFullPass = -1;
			for (int c = vPassGaps[i].tl; c < vPassGaps[i].br; c++)
				if (isVerticalPass[c-tLeft]) {
					if (lFullPass == -1)
						lFullPass = c;
					rFullPass = c;
				}
			if (lFullPass != -1)
				vPassGaps[i] = new Interval(lFullPass, (rFullPass+1));
		}
		if (DEBUG_TABLES) {
			System.out.println("VPassGaps (" + vPassGaps.length + " after full pass analysis):");
			for (int i = 0; i < vPassGaps.length; i++)
				System.out.println(" - " + vPassGaps[i]);
		}
		
		//	find vertical grid lines
		TreeSet vPasses = new TreeSet();
		for (int i = 0; i < vPassGaps.length; i++)
			vPasses.add(new Integer((vPassGaps[i].tl + vPassGaps[i].br) / 2));
		if (DEBUG_TABLES) {
			System.out.println("VPasses (" + vPasses.size() + "):");
			for (Iterator pit = vPasses.iterator(); pit.hasNext();)
				System.out.println(" - " + pit.next());
		}
		
		//	sort cells left-right for row grouping
		Arrays.sort(cells, lrCellOrder);
		
		//	group cells into rows
		ArrayList rows = new ArrayList();
		for (Iterator pit = hPasses.iterator(); pit.hasNext();) {
			TableRow row = new TableRow();
			Integer hp = ((Integer) pit.next());
			for (int ce = 0; ce < cells.length; ce++) {
				if (cells[ce] == null)
					continue;
				if (cells[ce].bounds.top < hp.intValue()) {
					row.cells.add(cells[ce]);
					cells[ce] = null;
				}
			}
			if (row.cells.size() != 0)
				rows.add(row);
		}
		TableRow lastRow = new TableRow();
		for (int ce = 0; ce < cells.length; ce++) {
			if (cells[ce] == null)
				continue;
			lastRow.cells.add(cells[ce]);
			cells[ce] = null;
		}
		if (lastRow.cells.size() != 0)
			rows.add(lastRow);
		
		//	compute col span of cells
		for (int r = 0; r < rows.size(); r++) {
			TableRow row = ((TableRow) rows.get(r));
			for (int c = 0; c < row.cells.size(); c++) {
				TableCell cell = ((TableCell) row.cells.get(c));
				for (int cr = 0; cr < rows.size(); cr++) {
					if (cr == r)
						continue;
					TableRow cRow = ((TableRow) rows.get(cr));
					int cellsSpanned = 0;
					for (int cc = 0; cc < cRow.cells.size(); cc++) {
						TableCell cCell = ((TableCell) cRow.cells.get(cc));
						if ((cell.bounds.right > cCell.bounds.left) && (cCell.bounds.right > cell.bounds.left))
							cellsSpanned++;
					}
					cell.colSpan = Math.max(cell.colSpan, cellsSpanned);
				}
			}
		}
		
		if (DEBUG_TABLES) {
			System.out.println("TABLE (after colspans):");
			for (int r = 0; r < rows.size(); r++) {
				TableRow row = ((TableRow) rows.get(r));
				System.out.println("<tr>");
				for (int c = 0; c < row.cells.size(); c++) {
					TableCell cell = ((TableCell) row.cells.get(c));
					System.out.print("  <td" + ((cell.colSpan > 1) ? (" colSpan=\"" + cell.colSpan + "\"") : "") + ((cell.rowSpan > 1) ? (" rowSpan=\"" + cell.rowSpan + "\"") : "") + ">");
					System.out.print(cell.annot.getValue());
					System.out.println("</td>");
				}
				System.out.println("</tr>");
			}
		}
		
		//	generate minimum row bounds so empty cells do not grow out of bounds
		for (int r = 0; r < rows.size(); r++) {
			TableRow row = ((TableRow) rows.get(r));
			int rTop = tTop;
			int rBottom = tBottom;
			for (int c = 0; c < row.cells.size(); c++) {
				TableCell cell = ((TableCell) row.cells.get(c));
				rTop = Math.max(rTop, cell.bounds.top);
				rBottom = Math.min(rBottom, cell.bounds.bottom);
			}
			row.bounds = new BoundingBox(tLeft, tRight, rTop, rBottom);
		}
		
		//	compute row span of each cell
		for (int r = 0; r < rows.size(); r++) {
			TableRow row = ((TableRow) rows.get(r));
			for (int c = 0; c < row.cells.size(); c++) {
				TableCell cell = ((TableCell) row.cells.get(c));
				for (int cr = (r+1); cr < rows.size(); cr++) {
					TableRow cRow = ((TableRow) rows.get(cr));
					if (cell.bounds.bottom > cRow.bounds.top)
						cell.rowSpan++;
				}
			}
		}
		
		if (DEBUG_TABLES) {
			System.out.println("TABLE (after rowspans):");
			for (int r = 0; r < rows.size(); r++) {
				TableRow row = ((TableRow) rows.get(r));
				System.out.println("<tr>");
				for (int c = 0; c < row.cells.size(); c++) {
					TableCell cell = ((TableCell) row.cells.get(c));
					System.out.print("  <td" + ((cell.colSpan > 1) ? (" colSpan=\"" + cell.colSpan + "\"") : "") + ((cell.rowSpan > 1) ? (" rowSpan=\"" + cell.rowSpan + "\"") : "") + ">");
					System.out.print(cell.annot.getValue());
					System.out.println("</td>");
				}
				System.out.println("</tr>");
			}
		}
		
		//	build table grid lists
		ArrayList[] rowCells = new ArrayList[rows.size()];
		for (int r = 0; r < rows.size(); r++)
			rowCells[r] = new ArrayList();
		for (int r = 0; r < rows.size(); r++) {
			TableRow row = ((TableRow) rows.get(r));
			for (int c = 0; c < row.cells.size(); c++) {
				TableCell cell = ((TableCell) row.cells.get(c));
				rowCells[r].add(cell);
				for (int s = 1; s < cell.rowSpan; s++)
					rowCells[r+s].add(cell);
			}
		}
		for (int r = 0; r < rowCells.length; r++)
			Collections.sort(rowCells[r], lrCellOrder);
		
		//	generate dummy cell value
		TokenSequence dummyCellContent = Gamta.newTokenSequence(EMPTY_CELL_FILLER, table.getTokenizer());
		
		//	fill in empty cells
		for (int r = 0; r < rows.size(); r++) {
			TableRow row = ((TableRow) rows.get(r));
			for (int c = 0; c <= rowCells[r].size(); c++) {
				int left = ((c == 0) ? tLeft : ((TableCell) rowCells[r].get(c-1)).bounds.right);
				int right = ((c == rowCells[r].size()) ? tRight : ((TableCell) rowCells[r].get(c)).bounds.left);
				for (int cr = 0; cr < rows.size(); cr++) {
					if (cr == r)
						continue;
					TableRow cRow = ((TableRow) rows.get(cr));
					for (int cc = 0; cc < cRow.cells.size(); cc++) {
						TableCell cCell = ((TableCell) cRow.cells.get(cc));
						if ((cCell.bounds.left < left) || (right < cCell.bounds.right))
							continue;
						
						//	generate filler cell
						Annotation fCellAnnot = Gamta.newAnnotation(dummyCellContent, MutableAnnotation.PARAGRAPH_TYPE, 0, 1);
						fCellAnnot.setAttribute(EMPTY_CELL_MARKER_ATTRIBUTE, EMPTY_CELL_MARKER_ATTRIBUTE);
						TableCell fCell = new TableCell(
								fCellAnnot,
								new BoundingBox(
									(cCell.bounds.left),
									(cCell.bounds.right),
									(row.bounds.top),
									(row.bounds.bottom)
								)
							);
						
						//	store cell in grid list
						rowCells[r].add(c, fCell);
						
						//	add cell to row
						row.cells.add(fCell);
						Collections.sort(row.cells, lrCellOrder);
//						System.out.println("Added empty cell: " + fCell.getBoundingBox().toString());
						
						//	start over
						cc = cRow.cells.size();
						cr = rows.size();
					}
				}
			}
		}
		
		if (DEBUG_TABLES) {
			System.out.println("TABLE (after empty cells):");
			for (int r = 0; r < rows.size(); r++) {
				TableRow row = ((TableRow) rows.get(r));
				System.out.println("<tr>");
				for (int c = 0; c < row.cells.size(); c++) {
					TableCell cell = ((TableCell) row.cells.get(c));
					System.out.print("  <td" + (cell.annot.hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE) ? (" isEmpty=\"true") : "") + ((cell.colSpan > 1) ? (" colSpan=\"" + cell.colSpan + "\"") : "") + ((cell.rowSpan > 1) ? (" rowSpan=\"" + cell.rowSpan + "\"") : "") + ">");
					System.out.print(cell.annot.getValue());
					System.out.println("</td>");
				}
				System.out.println("</tr>");
			}
		}
		
		//	insert table in argument annotation
		int tSize = 0;
		for (int r = 0; r < rows.size(); r++) {
			TableRow row = ((TableRow) rows.get(r));
			int rowStart = tSize;
			for (int c = 0; c < row.cells.size(); c++) {
				TableCell cell = ((TableCell) row.cells.get(c));
				table.insertTokensAt(cell.annot, tSize);
				MutableAnnotation cellAnnot = table.addAnnotation("td", tSize, cell.annot.size());
				cellAnnot.copyAttributes(cell.annot);
				if (cell.colSpan > 1)
					cellAnnot.setAttribute(COL_SPAN_ATTRIBUTE, ("" + cell.colSpan));
				if (cell.rowSpan > 1)
					cellAnnot.setAttribute(ROW_SPAN_ATTRIBUTE, ("" + cell.rowSpan));
				if (cell.mAnnot != null) {
					Annotation[] annots = cell.mAnnot.getAnnotations();
					for (int a = 0; a < annots.length; a++) {
						if (!cell.mAnnot.getAnnotationID().equals(annots[a].getAnnotationID()))
							cellAnnot.addAnnotation(annots[a].getType(), annots[a].getStartIndex(), annots[a].size()).copyAttributes(annots[a]);
					}
				}
				tSize += cellAnnot.size();
			}
			table.addAnnotation("tr", rowStart, (tSize - rowStart));
		}
		
		//	delete original table content
		table.removeTokensAt(tSize, (table.size() - tSize));
		
		//	mark table bounding box
		BoundingBox tableBox = new BoundingBox(tLeft, tRight, tTop, tBottom);
		table.setAttribute(BOUNDING_BOX_ATTRIBUTE, tableBox.toString());
		
		//	make whole table one paragraph
		AnnotationFilter.removeAnnotations(table, MutableAnnotation.PARAGRAPH_TYPE);
		MutableAnnotation paragraph = table.addAnnotation(MutableAnnotation.PARAGRAPH_TYPE, 0, table.size());
		paragraph.copyAttributes(table);
	}
	
	private static boolean overlaps(BoundingBox box1, BoundingBox box2) {
		return ((box1.left < box2.right) && (box2.left < box1.right) && (box1.top < box2.bottom) && (box2.top < box1.bottom));
	}
	
	private void restoreRegions(MutableAnnotation data, int maxPageId) {
		
		//	restore main text regions
		this.restoreRegions(data, MAIN_TEXT_TYPE, mainTextTypes, mainTextBridgeableTypes, maxPageId);
		
		//	restore footnotes
		this.restoreRegions(data, FOOTNOTE_TYPE, footnoteTypes, footnoteBridgeableTypes, maxPageId);
		
		//	recurse
		MutableAnnotation[] regions = this.getTopChildRegions(data);
		for (int r = 0; r < regions.length; r++)
			this.restoreRegions(regions[r], maxPageId);
	}
	
	private void restoreRegions(MutableAnnotation data, String restoreType, Set appendTypes, Set bridgeableTypes, int maxPageId) {
		
		//	make sure whitespace is in place
		MutableAnnotation[] regions = this.getTopChildRegions(data);
		for (int r = 0; r < (regions.length - 1); r++) {
			regions[r].setWhitespaceAfter(" ", regions[r].size()-1);
		}
		
		//	restore top level regions
		for (int r = (regions.length - 1); r > 0; r--) {
			if (!regions[r].hasAttribute(CONTINUE_ATTRIBUTE))
				continue;
			if (!restoreType.equals(regions[r].getAttribute(TYPE_ATTRIBUTE)))
				continue;
			if (Integer.parseInt((String) regions[r].getAttribute(PAGE_ID_ATTRIBUTE)) > maxPageId)
				continue;
			
			int ar = r-1;
			while (ar > -1) {
				
				//	we can skip over this one
				if (bridgeableTypes.contains(regions[ar].getAttribute(TYPE_ATTRIBUTE)))
					ar--;
				
				//	we can append to this one
				else if (appendTypes.contains(regions[ar].getAttribute(TYPE_ATTRIBUTE))) {
					
					//	copy to-move paragraph
					MutableAnnotation moveRegion = Gamta.copyDocument(regions[r]);
					
					//	compute aggregate bounding box
					String aggregateBoundingBox = this.getAggregateBoundingBoxesString(regions[ar], regions[r]);
					
					//	remove to-move paragraph
					while (regions[r].size() > 1)
						data.removeTokensAt(regions[r].getStartIndex()+1, 1);
					data.removeTokensAt(regions[r].getStartIndex(), 1);
					
					//	append to-move paragraph to target paragraph
					int movedStart = regions[ar].size();
					regions[ar].addTokens(moveRegion);
					
					//	transfer token attributes
					for (int t = 0; t < moveRegion.size(); t++)
						regions[ar].tokenAt(movedStart + t).copyAttributes(moveRegion.tokenAt(t));
					
					//	transfer annotations
					Annotation[] moveAnnotations = moveRegion.getAnnotations();
					for (int a = 0; a < moveAnnotations.length; a++) {
						if (moveAnnotations[a].size() == moveRegion.size())
							continue;
						regions[ar].addAnnotation(moveAnnotations[a].getType(), (movedStart + moveAnnotations[a].getStartIndex()), moveAnnotations[a].size()).copyAttributes(moveAnnotations[a]);
					}
					
					//	add bounding box
					if (aggregateBoundingBox != null)
						regions[ar].setAttribute(BOUNDING_BOX_ATTRIBUTE, aggregateBoundingBox);
					
					//	resume investigation at restored paragraph (compensate for loop decrement)
					r = ar+1;
					ar = -1;
				}
				
				//	we can neither bridge nor append to this one, so forget about it
				else break;
			}
		}
	}
	
	private MutableAnnotation[] getTopChildRegions(MutableAnnotation data) {
		MutableAnnotation[] regions = data.getMutableAnnotations(REGION_ANNOTATION_TYPE);
		for (int r = 0; r < regions.length; r++) {
			if (regions[r] == null)
				continue;
			if (AnnotationUtils.equals(data, regions[r], false))
				regions[r] = null;
			else for (int lr = (r+1); lr < regions.length; lr++) {
				if (AnnotationUtils.contains(regions[r], regions[lr]))
					regions[lr] = null;
				else if (regions[r].getEndIndex() < regions[lr].getStartIndex())
					lr = regions.length;
			}
		}
		ArrayList topRegions = new ArrayList();
		for (int r = 0; r < regions.length; r++) {
			if (regions[r] != null)
				topRegions.add(regions[r]);
		}
		if (topRegions.size() < regions.length)
			regions = ((MutableAnnotation[]) topRegions.toArray(new MutableAnnotation[topRegions.size()]));
		return regions;
	}
	
	private String getAggregateBoundingBoxesString(Annotation first, Annotation second) {
		
		//	get base boxes
		BoundingBox fpBox = BoundingBox.getBoundingBox(first);
		BoundingBox[] spBoxes = BoundingBox.getBoundingBoxes(second);
		if ((fpBox == null) || (spBoxes == null))
			return null;
		
		//	get page IDs
		int fpId = Integer.parseInt((String) first.getAttribute(PAGE_ID_ATTRIBUTE));
		int spId = Integer.parseInt((String) second.getAttribute(PAGE_ID_ATTRIBUTE));
		String aggregateBoxesString;
		
		//	aggregate first two boxes
		if (fpId == spId) {
			BoundingBox[] boxAggregator = {fpBox, spBoxes[0]};
			spBoxes[0] = BoundingBox.aggregate(boxAggregator);
			aggregateBoxesString = "";
			for (int b = 0; b < spBoxes.length; b++)
				aggregateBoxesString += ((spBoxes[b] == null) ? "[]" : spBoxes[b].toString());
		}
		
		//	concatenate boxes
		else {
			aggregateBoxesString = fpBox.toString();
			for (int pid = (fpId+1); pid < spId; pid++)
				aggregateBoxesString += "[]";
			for (int b = 0; b < spBoxes.length; b++)
				aggregateBoxesString += ((spBoxes[b] == null) ? "[]" : spBoxes[b].toString());
		}
		
		//	return aggregate
		return aggregateBoxesString;
	}
	
	private static final String PAGE_NUMBER_CANDIDATE_TYPE = (PAGE_NUMBER_TYPE + "Candidate");
	private static final boolean DEBUG_PAGE_NUMBERS = true;
	private static final boolean DEBUG_TABLES = true;
	
	private static final void getAllPossibleValues(int[][] baseDigits, int[] digits, int pos, TreeSet values) {
		if (pos == digits.length) {
			StringBuffer value = new StringBuffer();
			for (int d = 0; d < digits.length; d++)
				value.append("" + digits[d]);
			values.add(value.toString());
		}
		else for (int d = 0; d < baseDigits[pos].length; d++) {
			digits[pos] = baseDigits[pos][d];
			getAllPossibleValues(baseDigits, digits, (pos+1), values);
		}
	}
	
	private static class PageNumber {
		final int pageId;
		final int value;
		final int fuzzyness;
		final int ambiguity;
		int score = 0;
		//	!!! this constructor is for set lookups only !!!
		PageNumber(int value) {
			this.value = value;
			this.pageId = -1;
			this.fuzzyness = 1;
			this.ambiguity = 1;
		}
		PageNumber(int pageId, int value, int fuzzyness, int ambiguity) {
			this.value = value;
			this.pageId = pageId;
			this.fuzzyness = fuzzyness;
			this.ambiguity = ambiguity;
		}
		public boolean equals(Object obj) {
			return (((PageNumber) obj).value == this.value);
		}
		public int hashCode() {
			return this.value;
		}
		public String toString() {
			return ("" + this.value);
		}
		boolean isConsistentWith(PageNumber pn) {
			return ((this.value - pn.value) == (this.pageId - pn.pageId));
		}
		int getAdjustedScore() {
			return (((this.fuzzyness == Integer.MAX_VALUE) || (this.ambiguity == Integer.MAX_VALUE)) ? 0 : (this.score / (this.fuzzyness + this.ambiguity)));
		}
	}
	
	private String pageNumberPatternFuzzy;
	private String pageNumberPatternStrict;
	private HashMap pageNumberCharacterTranslations = new HashMap();
	
	private void detectPageNumbers(MutableAnnotation[] pages) {
		
		//	noTODO ==> make page number detection a library with a data provider based instance factory
		//	==> DO NOT DO THIS, at least not candidate generation, as this implementation here works on the STR attributes of words
		
		//	TODO deactivate fuzzy matching for born-digital PDFs
		
		//	process lines in each page & collect page number data
		int[] pageIds = new int[pages.length];
		HashSet[] pageNumberSets = new HashSet[pages.length];
		for (int p = 0; p < pages.length; p++) {
			
			//	store page ID
			pageIds[p] = Integer.parseInt((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE, ("" + p)));
			
			//	are we handling OCR output?
			boolean isOcred = pages[p].hasAttribute(IS_OCRED_ATTRIBUTE);
			
			//	get all candidates
			ArrayList pageNumberList = new ArrayList();
			
			//	extract page numbers from STR attribute of words instead
			Annotation[] words = pages[p].getAnnotations(WORD_ANNOTATION_TYPE);
			for (int w = 0; w < words.length; w++) {
				String str = ((String) words[w].getAttribute(STRING_ATTRIBUTE));
				if ((str == null) || (str.trim().length() == 0))
					continue;
				TokenSequence strTs = pages[p].getTokenizer().tokenize(str);
				if (isOcred) {
					Annotation[] pageNumberCandidates = Gamta.extractAllMatches(strTs, this.pageNumberPatternFuzzy, 3, true, false, true);
					for (int c = 0; c < pageNumberCandidates.length; c++) {
						Annotation wpn = pages[p].addAnnotation(PAGE_NUMBER_CANDIDATE_TYPE, words[w].getStartIndex(), 1);
						wpn.setAttribute(STRING_ATTRIBUTE, pageNumberCandidates[c].getValue().replaceAll("\\s++", ""));
						pageNumberList.add(wpn);
					}
				}
				else {
					Annotation[] pageNumberCandidates = Gamta.extractAllMatches(strTs, this.pageNumberPatternStrict, 3, true, false, true);
					for (int c = 0; c < pageNumberCandidates.length; c++) {
						Annotation wpn = pages[p].addAnnotation(PAGE_NUMBER_CANDIDATE_TYPE, words[w].getStartIndex(), 1);
						wpn.setAttribute(STRING_ATTRIBUTE, pageNumberCandidates[c].getValue().replaceAll("\\s++", ""));
						pageNumberList.add(wpn);
					}
				}
			}
			
			//	process candidates
			Annotation[] pageNumberCandidates = ((Annotation[]) pageNumberList.toArray(new Annotation[pageNumberList.size()]));
			pageNumberList.clear();
			for (int n = 0; n < pageNumberCandidates.length; n++) {
				
				//	use only top and bottom of page
				if ((pageNumberCandidates[n].getStartIndex() > 25) && (pageNumberCandidates[n].getEndIndex() < (pages[p].size() - 25)))
					continue;
				
				if (DEBUG_PAGE_NUMBERS) System.out.println("Possible page number on page " + pageIds[p] + ": " + pageNumberCandidates[n].getAttribute(STRING_ATTRIBUTE, pageNumberCandidates[n].getValue()));
				
				//	get page number string
				String pageNumberString = ((String) pageNumberCandidates[n].getAttribute(STRING_ATTRIBUTE));
				int fuzzyness = 0;
				int ambiguity = 1;
				
				//	if we have a born-digital document, there's no need to care about OCR errors
				if (!isOcred) {
					pageNumberCandidates[n].changeTypeTo(PAGE_NUMBER_CANDIDATE_TYPE);
					pageNumberCandidates[n].setAttribute("fuzzyness", ("" + fuzzyness));
					pageNumberCandidates[n].setAttribute("ambiguity", ("" + ambiguity));
					String pageNumberValue = pageNumberString.replaceAll("\\s+", "");
					pages[p].addAnnotation(pageNumberCandidates[n]).setAttribute("value", pageNumberValue.toString());
					pageNumberList.add(new PageNumber(pageIds[p], Integer.parseInt(pageNumberValue.toString()), fuzzyness, ambiguity));
					if (DEBUG_PAGE_NUMBERS) System.out.println(" ==> value is " + pageNumberValue.toString());
					continue;
				}
				
				//	extract possible interpretations, computing fuzzyness and ambiguity along the way
				int [][] pageNumberDigits = new int[pageNumberString.length()][];
				for (int c = 0; c < pageNumberString.length(); c++) {
					String pnc = pageNumberString.substring(c, (c+1));
					pageNumberDigits[c] = ((int[]) this.pageNumberCharacterTranslations.get(pnc));
					if (pageNumberDigits[c] == null) {
						pageNumberDigits = null;
						break;
					}
					ambiguity *= pageNumberDigits[c].length;
					if (pageNumberDigits[c].length > 1)
						fuzzyness++;
				}
				if (pageNumberDigits == null)
					continue;
				
				//	the first digit is never zero ...
				if ((pageNumberDigits[0].length == 1) && (pageNumberDigits[0][0] == 0)) {
					if (DEBUG_PAGE_NUMBERS) System.out.println(" ==> ignoring zero start");
					continue;
				}
				
				//	page numbers with six or more digits are rather improbable ...
				if (pageNumberDigits.length >= 6) {
					if (DEBUG_PAGE_NUMBERS) System.out.println(" ==> ignoring for over-length");
					continue;
				}
				
				//	set type and base attributes
				pageNumberCandidates[n].changeTypeTo(PAGE_NUMBER_CANDIDATE_TYPE);
				pageNumberCandidates[n].setAttribute("fuzzyness", ("" + fuzzyness));
				if (DEBUG_PAGE_NUMBERS) System.out.println(" - fuzzyness is " + fuzzyness);
				pageNumberCandidates[n].setAttribute("ambiguity", ("" + ambiguity));
				if (DEBUG_PAGE_NUMBERS) System.out.println(" - ambiguity is " + ambiguity);
				
				//	this one is clear, annotate it right away
				if (ambiguity == 1) {
					StringBuffer pageNumberValue = new StringBuffer();
					for (int d = 0; d < pageNumberDigits.length; d++)
						pageNumberValue.append("" + pageNumberDigits[d][0]);
					pages[p].addAnnotation(pageNumberCandidates[n]).setAttribute("value", pageNumberValue.toString());
					pageNumberList.add(new PageNumber(pageIds[p], Integer.parseInt(pageNumberValue.toString()), fuzzyness, ambiguity));
					if (DEBUG_PAGE_NUMBERS) System.out.println(" ==> value is " + pageNumberValue.toString());
				}
				
				//	deal with ambiguity
				else {
					TreeSet values = new TreeSet();
					int[] digits = new int[pageNumberDigits.length];
					getAllPossibleValues(pageNumberDigits, digits, 0, values);
					for (Iterator vit = values.iterator(); vit.hasNext();) {
						String pageNumberValue = ((String) vit.next());
						pages[p].addAnnotation(pageNumberCandidates[n]).setAttribute("value", pageNumberValue);
						pageNumberList.add(new PageNumber(pageIds[p], Integer.parseInt(pageNumberValue.toString()), fuzzyness, ambiguity));
						if (DEBUG_PAGE_NUMBERS) System.out.println(" ==> possible value is " + pageNumberValue);
					}
				}
			}
			
			//	aggregate page numbers
			Collections.sort(pageNumberList, new Comparator() {
				public int compare(Object o1, Object o2) {
					PageNumber pn1 = ((PageNumber) o1);
					PageNumber pn2 = ((PageNumber) o2);
					return ((pn1.value == pn2.value) ? (pn1.fuzzyness - pn2.fuzzyness) : (pn1.value - pn2.value));
				}
			});
			pageNumberSets[p] = new HashSet();
			for (int n = 0; n < pageNumberList.size(); n++)
				pageNumberSets[p].add(pageNumberList.get(n));
		}
		
		//	score page numbers
		for (int p = 0; p < pages.length; p++) {
			for (Iterator pnit = pageNumberSets[p].iterator(); pnit.hasNext();) {
				PageNumber pn = ((PageNumber) pnit.next());
				if (DEBUG_PAGE_NUMBERS) System.out.println("Scoring page number " + pn + " for page " + pn.pageId);
				
				//	look forward
				int fMisses = 0;
				for (int l = 1; (p+l) < pages.length; l++) {
					if (pageNumberSets[p+l].contains(new PageNumber(pn.value + (pageIds[p+l] - pageIds[p]))))
						pn.score += l;
					else {
						fMisses++;
						if (fMisses == 3)
							l = pages.length;
					}
				}
				
				//	look backward
				int bMisses = 0;
				for (int l = 1; l <= p; l++) {
					if (pageNumberSets[p-l].contains(new PageNumber(pn.value + (pageIds[p-l] - pageIds[p]))))
						pn.score += l;
					else {
						bMisses++;
						if (bMisses == 3)
							l = (p+1);
					}
				}
				
				if (DEBUG_PAGE_NUMBERS) System.out.println(" ==> score is " + pn.score);
			}
		}
		
		//	select page number for each page
		PageNumber[] pageNumbers = new PageNumber[pages.length];
		for (int p = 0; p < pages.length; p++) {
			int bestPageNumberScore = 0;
			for (Iterator pnit = pageNumberSets[p].iterator(); pnit.hasNext();) {
				PageNumber pn = ((PageNumber) pnit.next());
				if (pn.score > bestPageNumberScore) {
					pageNumbers[p] = pn;
					bestPageNumberScore = pn.score;
				}
			}
			if (DEBUG_PAGE_NUMBERS) {
				if (pageNumbers[p] == null)
					System.out.println("Could not determine page number of page " + pageIds[p] + ".");
				else System.out.println("Determined page number of page " + pageIds[p] + " as " + pageNumbers[p] + " (score " + pageNumbers[p].score + ")");
			}
		}
		
		//	fill in sequence gaps
		for (int p = 0; p < pages.length; p++) {
			if (pageNumbers[p] == null)
				pageNumbers[p] = new PageNumber(pageIds[p], -1, Integer.MAX_VALUE, Integer.MAX_VALUE);
		}
		
		//	do sequence base correction
		for (int p = 1; p < (pages.length - 1); p++) {
			if (pageNumbers[p+1].isConsistentWith(pageNumbers[p-1]) && !pageNumbers[p+1].isConsistentWith(pageNumbers[p])) {
				int beforeScore = pageNumbers[p-1].score;
				int ownScore = pageNumbers[p].score;
				int afterScore = pageNumbers[p+1].score;
				if ((beforeScore + afterScore) > (ownScore * 3)) {
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, (pageNumbers[p-1].value + (pageNumbers[p].pageId - pageNumbers[p-1].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE);
					pageNumbers[p].score = ((beforeScore + afterScore) / 3);
					if (DEBUG_PAGE_NUMBERS) System.out.println("Corrected page number of page " + pageIds[p] + " to " + pageNumbers[p] + " (score " + ((beforeScore + afterScore) / 2) + " over " + ownScore + ")");
				}
			}
		}
		
		//	do backward sequence extrapolation
		for (int p = (pages.length - 2); p >= 0; p--) {
			if (!pageNumbers[p+1].isConsistentWith(pageNumbers[p])) {
				int ownScore = pageNumbers[p].score;
				int afterScore = pageNumbers[p+1].score;
				if (afterScore > (ownScore * 2)) {
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, (pageNumbers[p+1].value - (pageNumbers[p+1].pageId - pageNumbers[p].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE);
					pageNumbers[p].score = (afterScore / 2);
					if (DEBUG_PAGE_NUMBERS) System.out.println("Extrapolated (backward) page number of page " + pageIds[p] + " to " + pageNumbers[p] + " (score " + afterScore + " over " + ownScore + ")");
				}
			}
		}
		
		//	do forward sequence extrapolation
		for (int p = 1; p < pages.length; p++) {
			if (!pageNumbers[p].isConsistentWith(pageNumbers[p-1])) {
				int beforeScore = pageNumbers[p-1].score;
				int ownScore = pageNumbers[p].score;
				if (beforeScore > (ownScore * 2)) {
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, (pageNumbers[p-1].value + (pageNumbers[p].pageId - pageNumbers[p-1].pageId)), Integer.MAX_VALUE, Integer.MAX_VALUE);
					pageNumbers[p].score = (beforeScore / 2);
					if (DEBUG_PAGE_NUMBERS) System.out.println("Extrapolated (forward) page number of page " + pageIds[p] + " to " + pageNumbers[p] + " (score " + beforeScore + " over " + ownScore + ")");
				}
			}
		}
		
		//	disambiguate page numbers that occur on multiple pages (using fuzzyness and ambiguity)
		HashMap allPageNumbers = new HashMap();
		for (int p = 0; p < pages.length; p++) {
			if (allPageNumbers.containsKey(pageNumbers[p])) {
				PageNumber pn = ((PageNumber) allPageNumbers.get(pageNumbers[p]));
				if (pn.getAdjustedScore() < pageNumbers[p].getAdjustedScore()) {
					if (DEBUG_PAGE_NUMBERS) System.out.println("Ousted page number " + pn.value + " of page " + pn.pageId + " (score " + pn.getAdjustedScore() + " against " + pageNumbers[p].getAdjustedScore() + ")");
					allPageNumbers.put(pageNumbers[p], pageNumbers[p]);
					pn.score = 0;
					pageNumbers[pn.pageId] = new PageNumber(pn.pageId, -1, Integer.MAX_VALUE, Integer.MAX_VALUE);
				}
				else {
					if (DEBUG_PAGE_NUMBERS) System.out.println("Ousted page number " + pageNumbers[p].value + " of page " + pageNumbers[p].pageId + " (score " + pageNumbers[p].getAdjustedScore() + " against " + pn.getAdjustedScore() + ")");
					pageNumbers[p].score = 0;
					pageNumbers[p] = new PageNumber(pageNumbers[p].pageId, -1, Integer.MAX_VALUE, Integer.MAX_VALUE);
				}
			}
			else if (pageNumbers[p].value != -1) allPageNumbers.put(pageNumbers[p], pageNumbers[p]);
		}
		
		if (DEBUG_PAGE_NUMBERS) for (int p = 0; p < pages.length; p++)
			System.out.println("Page number of page " + pageNumbers[p].pageId + " is " + pageNumbers[p] + " (score " + pageNumbers[p].score + ")");
		
		//	annotate page numbers and set attributes
		for (int p = 0; p < pages.length; p++) {
			String pageNumberValue = ("" + pageNumbers[p].value);
			
			//	set page attribute
			pages[p].setAttribute(PAGE_NUMBER_ATTRIBUTE, pageNumberValue);
			
			//	deal with page number candidates
			Annotation[] pageNumberCandidates = pages[p].getAnnotations(PAGE_NUMBER_CANDIDATE_TYPE);
			
			//	if value is -1, we only need to clean up
			if (pageNumbers[p].value == -1) {
				for (int n = 0; n < pageNumberCandidates.length; n++)
					pages[p].removeAnnotation(pageNumberCandidates[n]);
				continue;
			}
			
			//	find (most probable) original annotation otherwise
			int minFuzzyness = (pageNumberValue.length() + 1);
			Annotation leastFuzzyPageNumber = null;
			for (int n = 0; n < pageNumberCandidates.length; n++) {
				
				//	value matches page number value
				if (pageNumberValue.equals(pageNumberCandidates[n].getAttribute("value"))) {
					int fuzzyness = Integer.parseInt((String) pageNumberCandidates[n].getAttribute("fuzzyness"));
					
					//	too risky, could be some single letter in the middle of a line of text
//					if ((fuzzyness == pageNumberValue.length()) && (pageNumberValue.length() == 1))
					if ((fuzzyness == pageNumberValue.length()) && (pageNumberValue.length() == 1) && !Gamta.isNumber(pageNumberValue))
						pages[p].removeAnnotation(pageNumberCandidates[n]);
					
					//	we have a new best match
					else if (fuzzyness < minFuzzyness) {
						if (leastFuzzyPageNumber != null)
							pages[p].removeAnnotation(leastFuzzyPageNumber);
						leastFuzzyPageNumber = pageNumberCandidates[n];
						minFuzzyness = fuzzyness;
					}
					
					//	clean up
					else pages[p].removeAnnotation(pageNumberCandidates[n]);
				}
				
				//	no match, clean up
				else pages[p].removeAnnotation(pageNumberCandidates[n]);
			}
			
			//	we have found a page number, promote annotation
			if (leastFuzzyPageNumber != null) {
				leastFuzzyPageNumber.changeTypeTo(PAGE_NUMBER_TYPE);
				leastFuzzyPageNumber.setAttribute("score", ("" + pageNumbers[p].score));
				if (DEBUG_PAGE_NUMBERS) System.out.println("Annotated page number of page " + pageNumbers[p].pageId + " as " + leastFuzzyPageNumber.toXML() + " at " + leastFuzzyPageNumber.getStartIndex());
			}
		}
	}
	
	private static void aggregateBoxes(MutableAnnotation doc, String baseType, String aggregateType, boolean removeBase) {
		MutableAnnotation[] targetAnnotations = doc.getMutableAnnotations(aggregateType);
		for (int t = 0; t < targetAnnotations.length; t++) {
//			Annotation[] baseAnnotations = targetAnnotations[t].getAnnotations(baseType);
//			if (baseAnnotations.length == 0)
//				continue;
//			int currentPageId = Integer.parseInt((String) baseAnnotations[0].getAttribute(PAGE_ID_ATTRIBUTE, "-1"));
//			StringBuffer bBoxString = new StringBuffer();
//			ArrayList bBoxes = new ArrayList();
//			for (int b = 0; b < baseAnnotations.length; b++) {
//				int pageId = Integer.parseInt((String) baseAnnotations[b].getAttribute(PAGE_ID_ATTRIBUTE, "-1"));
//				if (pageId != currentPageId) {
//					BoundingBox bBox = BoundingBox.aggregate((BoundingBox[]) bBoxes.toArray(new BoundingBox[bBoxes.size()]));
//					if (bBox != null)
//						bBoxString.append(bBox.toString());
//					bBoxes.clear();
//					currentPageId++;
//					while (currentPageId < pageId) {
//						bBoxString.append("[]");
//						currentPageId++;
//					}
//				}
//				BoundingBox bBox = BoundingBox.getBoundingBox(baseAnnotations[b]);
//				if (bBox != null)
//					bBoxes.add(bBox);
//				if (removeBase)
//					targetAnnotations[t].removeAnnotation(baseAnnotations[b]);
//			}
//			BoundingBox bBox = BoundingBox.aggregate((BoundingBox[]) bBoxes.toArray(new BoundingBox[bBoxes.size()]));
//			if (bBox != null)
//				bBoxString.append(bBox.toString());
//			targetAnnotations[t].setAttribute(BOUNDING_BOX_ATTRIBUTE, bBoxString.toString());
			Annotation[] baseAnnotations = targetAnnotations[t].getAnnotations(baseType);
			BoundingBox[] bBoxes = new BoundingBox[baseAnnotations.length];
			for (int b = 0; b < baseAnnotations.length; b++) {
				bBoxes[b] = BoundingBox.getBoundingBox(baseAnnotations[b]);
				if (removeBase)
					targetAnnotations[t].removeAnnotation(baseAnnotations[b]);
			}
			BoundingBox bBox = BoundingBox.aggregate(bBoxes);
			if (bBox != null)
				targetAnnotations[t].setAttribute(BOUNDING_BOX_ATTRIBUTE, bBox.toString());
		}
	}
	
	private static int getLineStart(MutableAnnotation line) {
		Annotation[] words = line.getAnnotations(WORD_ANNOTATION_TYPE);
		if (words.length == 0)
			return -1;
		BoundingBox bBox = BoundingBox.getBoundingBox(words[0]);
		return ((bBox == null) ? -1 : bBox.left);
	}
	
	/**
	 * Feedback panel for page cleanup. This class is public only for remote
	 * class loaders to be able to access it.
	 * 
	 * @author sautter
	 */
	public static class PageCleanupFeedbackPanel extends FeedbackPanel {
		private static final boolean DEBUG = true;
		
		private static class Box {
			int left;
			int top;
			int width;
			int height;
			String boxType;
			int borderWidth;
			RegionBox parentBox;
			Box(int left, int top, int width, int height) {
				this.left = left;
				this.top = top;
				this.width = width;
				this.height = height;
			}
			Box() {this(-1,-1,-1,-1);}
			void mouseOver() {}
			void mouseOut() {}
			void mousePressed() {}
			void mouseReleased() {}
		}
		
		static class WordBox extends Box {
			String word;
			String id;
			int baseline;
			
			String fontName;
			int fontStyle;
			int fontSize;
			boolean fontSizeUnchecked = true;
//			int fontSize = -1;
			int hOffset = 0;
//			int vOffset = 0;
			boolean hasAscent = false;
			boolean hasMedian = true;
			boolean hasDescent = false;
			
			WordBox previousWordBox;
			WordBox nextWordBox;
//			WordBox(int left, int top, int width, int height, int baseline, String word, String id) {
			WordBox(int left, int top, int width, int height, int baseline, String fontName, int fontStyle, int fontSize, String word, String id) {
				super(left, top, width, height);
				this.baseline = baseline;
				this.word = word;
				this.hasAscent = hasAscent(this.word);
				this.hasMedian = hasMedian(this.word);
				this.hasDescent = hasDescent(this.word);
				this.id = id;
				this.borderWidth = 1;
				this.fontName = fontName;
				this.fontStyle = fontStyle;
				this.fontSize = fontSize;
//				this.fontSize = -1;
			}
		}
		
		static class RegionBox extends Box {
			WordBox firstWordBox;
			WordBox lastWordBox;
			ButtonBox resizeLt;
			ButtonBox remove;
			ButtonBox resizeRb;
			ButtonBox retype;
			int hbo;
			int vbo;
			PageCleanupFeedbackPanel pcfp;
			boolean isContainer;
			RegionBox(DrawingBox dBox, PageCleanupFeedbackPanel pcfp) {
				this(dBox.left, dBox.top, dBox.width, dBox.height, pcfp);
				this.parentBox = dBox.parentBox;
			}
			RegionBox(int left, int top, int width, int height, final PageCleanupFeedbackPanel pcfp) {
				super(left, top, width, height);
				this.pcfp = pcfp;
				if (pcfp == null)
					this.borderWidth = 0;
				else {
					this.borderWidth = 2;
					this.resizeLt = new ButtonBox(this.left, this.top, "R") {
						void mousePressed() {
							pcfp.stopRetyping();
							if (pcfp.resizingLt != RegionBox.this)
								pcfp.stopResizingLt();
							pcfp.stopResizingRb();
							pcfp.removing = null;
							pcfp.resizingRb = null;
							pcfp.resizingLt = RegionBox.this;
							pcfp.resizingLtInner = pcfp.buildInnerBox(RegionBox.this);
							pcfp.resizingLtSiblings = pcfp.getChildBoxes(pcfp.resizingLt.parentBox);
						}
						void mouseReleased() {
							pcfp.stopResizingLt();
						}
						void mouseOver() {
							if ((pcfp.activeButton != null) && (pcfp.activeButton != this))
								pcfp.activeButton.mouseOut();
							pcfp.activeButton = this;
							pcfp.stopRetyping();
						}
					};
					this.resizeLt.parentBox = this;
					
					this.remove = new ButtonBox((this.left + this.width - pcfp.buttonSize), this.top, "X") {
						boolean isRemoving = false;
						void mousePressed() {
							pcfp.stopRetyping();
							pcfp.stopResizingLt();
							pcfp.stopResizingRb();
							pcfp.resizingLt = null;
							pcfp.resizingRb = null;
							this.isRemoving = true;
							pcfp.removing = RegionBox.this;
						}
						void mouseReleased() {
							if (this.isRemoving) {
								pcfp.removeBox(RegionBox.this);
								if (pcfp.activeBox == RegionBox.this) {
									pcfp.activeBox = null;
									pcfp.activeButton = null;
								}
							}
							pcfp.removing = null;
							pcfp.updateCanvas();
						}
						void mouseOver() {
							if ((pcfp.activeButton != null) && (pcfp.activeButton != this))
								pcfp.activeButton.mouseOut();
							pcfp.activeButton = this;
							pcfp.stopRetyping();
						}
						void mouseOut() {
							pcfp.removing = null;
							this.isRemoving = false;
						}
					};
					this.remove.parentBox = this;
					
					this.resizeRb = new ButtonBox((this.left + this.width - pcfp.buttonSize), (this.top + this.height - pcfp.buttonSize), "R") {
						void mousePressed() {
							pcfp.stopRetyping();
							pcfp.stopResizingLt();
							if (pcfp.resizingRb != RegionBox.this)
								pcfp.stopResizingRb();
							pcfp.resizingLt = null;
							pcfp.removing = null;
							pcfp.resizingRb = RegionBox.this;
							pcfp.resizingRbInner = pcfp.buildInnerBox(RegionBox.this);
							pcfp.resizingRbSiblings = pcfp.getChildBoxes(pcfp.resizingRb.parentBox);
						}
						void mouseReleased() {
							pcfp.stopResizingRb();
						}
						void mouseOver() {
							if ((pcfp.activeButton != null) && (pcfp.activeButton != this))
								pcfp.activeButton.mouseOut();
							pcfp.activeButton = this;
							pcfp.stopRetyping();
						}
					};
					this.resizeRb.parentBox = this;
					
					this.retype = new ButtonBox(this.left, (this.top + this.height - pcfp.buttonSize), "T") {
						void mousePressed() {
							if (pcfp.retyping == null)
								pcfp.startRetyping(RegionBox.this);
							else pcfp.stopRetyping();
							pcfp.resizingLt = null;
							pcfp.removing = null;
							pcfp.resizingRb = null;
						}
						void mouseOver() {
							if ((pcfp.activeButton != null) && (pcfp.activeButton != this))
								pcfp.activeButton.mouseOut();
							pcfp.activeButton = this;
						}
					};
					this.retype.parentBox = this;
				}
			}
			void mouseOver() {
				if (this.pcfp != null)
					this.pcfp.activeBox = this;
			}
			void mouseOut() {
				if ((this.pcfp != null) && (this.pcfp.activeBox == this))
					pcfp.activeBox = null;
			}
		}
		
		private static class DrawingBox extends RegionBox {
			int iLeft;
			int iTop;
			DrawingBox(int left, int top) {
				super(left, top, 0, 0, null);
				this.iLeft = left;
				this.iTop = top;
				this.borderWidth = 2;
			}
		}
		
		private static class ButtonBox extends Box {
			String label;
			ButtonBox(int left, int top, String label) {
				super(left, top, 0, 0);
				this.label = label;
				this.borderWidth = 0;
			}
		}
		
		private String imageProviderBaseUrl;
		private String docId;
		private String pageId;
		
		private JPopupMenu typeSelector = new JPopupMenu();
		
		private JPanel canvas;
		private RegionBox masterBox;
		
		private ArrayList boxes = new ArrayList();
		private ArrayList wordBoxes = new ArrayList();
		private HashMap wordBoxesByID = new HashMap();
		
		private RegionBox activeBox;
		private ButtonBox activeButton;
		
		private int buttonSize = 16;
		private int borderWidth = 25;
		private double zoomFactor = 1.0f;
		
		/**
		 * Constructor (only public in order to support remote class loading)
		 */
		public PageCleanupFeedbackPanel() {
			super();
			init();
		}
		
		PageCleanupFeedbackPanel(String title, String imageProviderBaseUrl, String docId, String pageId) {
			super(title);
			this.imageProviderBaseUrl = imageProviderBaseUrl;
			while (this.imageProviderBaseUrl.endsWith("/"))
				this.imageProviderBaseUrl = this.imageProviderBaseUrl.substring(0, (this.imageProviderBaseUrl.length()-1));
			this.docId = docId;
			this.pageId = pageId;
			init();
		}
		
		private void init() {
			this.typeSelector.setBackground(Color.WHITE);
			this.typeSelector.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			this.typeSelector.setBorderPainted(false);
			this.typeSelector.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent pme) {
					stopRetyping();
				}
				public void popupMenuWillBecomeInvisible(PopupMenuEvent pme) {}
				public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {}
			});
			
			this.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
					if (masterBox == null)
						return;
					Dimension size = PageCleanupFeedbackPanel.this.getSize();
					zoomFactor = (((double) size.width) / (masterBox.width + (2 * borderWidth)));
					Dimension pSize = new Dimension(size.width, ((int) (zoomFactor * (masterBox.height + (2 * borderWidth)))) + (DEBUG ? 50 : 0));
					setMinimumSize(pSize);
					setMaximumSize(pSize);
					setPreferredSize(pSize);
					updateCanvas();
				}
			});
			
			this.canvas = new JPanel() {
				private float[] hsb = new float[3];
				public void paintComponent(Graphics graphics) {
					super.paintComponent(graphics);
//					System.out.println("Painting canvas");
					
					//	render word boxes
					for (int w = 0; w < wordBoxes.size(); w++)
						this.paintBox(((Box) wordBoxes.get(w)), graphics);
//					System.out.println(" - word boxes done");
					
					//	render region boxes
					for (int b = 0; b < boxes.size(); b++)
						this.paintBox(((Box) boxes.get(b)), graphics);
//					System.out.println(" - boxes done");
					
					if ((activeBox != null) && (activeBox != masterBox))
						this.paintButtons(activeBox, graphics);
//					System.out.println(" - active boxe done");
					
					//	paint drawing box (if any)
					if (drawing != null)
						this.paintBox(drawing, graphics);
//					System.out.println(" - drawing boxe done");
					
//					System.out.println("Canvas painted");
				}
				
				int lastFontSize = 10;
				
				private void paintBox(Box box, Graphics graphics) {
					if (box instanceof ButtonBox)
						return;
					
					if (box instanceof WordBox) {
						Color oldColor = graphics.getColor();
						if (((WordBox) box).fontSize < 2) {
//							System.out.println("Determining font size for " + ((WordBox) box).word);
							int fontSize = this.lastFontSize;
							graphics.setFont(new Font(((WordBox) box).fontName, ((WordBox) box).fontStyle, fontSize));
							TextLayout tl = new TextLayout(((WordBox) box).word, graphics.getFont(), ((Graphics2D) graphics).getFontRenderContext());
							Rectangle2D wordBounds = tl.getBounds();
							if ((wordBounds.getWidth() > box.width) || (wordBounds.getHeight() > box.height)) {
								while ((wordBounds.getWidth() > box.width) || (wordBounds.getHeight() > box.height)) {
									float proportionalFontSize = ((float) (((float) fontSize) * (((float) box.width) / wordBounds.getWidth())));
									proportionalFontSize = ((float) Math.ceil(proportionalFontSize));
									if (((int) proportionalFontSize) < fontSize)
										fontSize = ((int) proportionalFontSize);
									else fontSize--;
//									System.out.println(" - trying font size " + fontSize);
									graphics.setFont(new Font(((WordBox) box).fontName, ((WordBox) box).fontStyle, fontSize));
									tl = new TextLayout(((WordBox) box).word, graphics.getFont(), ((Graphics2D) graphics).getFontRenderContext());
									wordBounds = tl.getBounds();
								}
							}
							while ((wordBounds.getWidth() <= box.width) && (wordBounds.getHeight() <= box.height)) {
//								System.out.println(" - trying font size " + (fontSize + 1));
								graphics.setFont(new Font(((WordBox) box).fontName, ((WordBox) box).fontStyle, (fontSize+1)));
								tl = new TextLayout(((WordBox) box).word, graphics.getFont(), ((Graphics2D) graphics).getFontRenderContext());
								wordBounds = tl.getBounds();
								if ((wordBounds.getWidth() <= box.width) && (wordBounds.getHeight() <= box.height))
									fontSize++;
								else break;
							}
//							System.out.println(" ==> font size is " + fontSize);
							this.lastFontSize = fontSize; // cache font size, gives a good hint where to start trying with next word ==> faster font size computation
							((WordBox) box).fontSize = fontSize;
							graphics.setFont(new Font(((WordBox) box).fontName, ((WordBox) box).fontStyle, ((WordBox) box).fontSize));
							tl = new TextLayout(((WordBox) box).word, graphics.getFont(), ((Graphics2D) graphics).getFontRenderContext());
							wordBounds = tl.getBounds();
							((WordBox) box).hOffset = ((int) ((box.width - wordBounds.getWidth()) / 2));
							((WordBox) box).fontSizeUnchecked = false;
						}
						if (((WordBox) box).fontSizeUnchecked) {
//							System.out.println("Checking font size for " + ((WordBox) box).word);
							int fontSize = ((WordBox) box).fontSize;
							graphics.setFont(new Font(((WordBox) box).fontName, ((WordBox) box).fontStyle, fontSize));
							TextLayout tl = new TextLayout(((WordBox) box).word, graphics.getFont(), ((Graphics2D) graphics).getFontRenderContext());
							Rectangle2D wordBounds = tl.getBounds();
							while ((wordBounds.getWidth() > box.width) || (wordBounds.getHeight() > box.height)) {
								float proportionalFontSize = ((float) (((float) fontSize) * (((float) box.width) / wordBounds.getWidth())));
								proportionalFontSize = ((float) Math.ceil(proportionalFontSize));
								if (((int) proportionalFontSize) < fontSize)
									fontSize = ((int) proportionalFontSize);
								else fontSize--;
//								System.out.println(" - trying font size " + fontSize);
								graphics.setFont(new Font(((WordBox) box).fontName, ((WordBox) box).fontStyle, fontSize));
								tl = new TextLayout(((WordBox) box).word, graphics.getFont(), ((Graphics2D) graphics).getFontRenderContext());
								wordBounds = tl.getBounds();
							}
							while ((wordBounds.getWidth() <= box.width) && (wordBounds.getHeight() <= box.height)) {
//								System.out.println(" - trying font size " + (fontSize + 1));
								graphics.setFont(new Font(((WordBox) box).fontName, ((WordBox) box).fontStyle, (fontSize+1)));
								tl = new TextLayout(((WordBox) box).word, graphics.getFont(), ((Graphics2D) graphics).getFontRenderContext());
								wordBounds = tl.getBounds();
								if ((wordBounds.getWidth() <= box.width) && (wordBounds.getHeight() <= box.height))
									fontSize++;
								else break;
							}
//							System.out.println(" ==> font size is " + fontSize);
							((WordBox) box).fontSize = fontSize;
							((WordBox) box).hOffset = ((int) ((box.width - wordBounds.getWidth()) / 2));
							((WordBox) box).fontSizeUnchecked = false;
						}
						int wordBaseline = zoom(toDisplayCoordinate(((WordBox) box).baseline, false));
						graphics.setFont(new Font(((WordBox) box).fontName, ((WordBox) box).fontStyle, zoom(((WordBox) box).fontSize)));
						graphics.setColor(Color.BLACK);
						graphics.drawString(((WordBox) box).word, zoom(toDisplayCoordinate((box.left + ((WordBox) box).hOffset), true)), wordBaseline);
						graphics.setColor(oldColor);
					}
					
					Color oldColor = graphics.getColor();
					Color boxColor = ((box instanceof DrawingBox) ? Color.RED : ((box instanceof WordBox) ? ((box.parentBox.isContainer || (box.parentBox == masterBox) || COLUMN_TYPE.equals(box.parentBox.boxType)) ? Color.RED : getTypeColor(box.parentBox.boxType)) : getTypeColor(box.boxType)));
					graphics.setColor(boxColor);
					int zdLeft = zoom(toDisplayCoordinate(box.left, true));
					int zdTop = zoom(toDisplayCoordinate(box.top, false));
					int zWidth = zoom(box.width);
					int zHeight = zoom(box.height);
					int borderShift = ((box.borderWidth+1)/2);
					for (int b = 0; b < box.borderWidth; b++)
						graphics.drawRect((zdLeft - borderShift + b), (zdTop - borderShift + b), (zWidth + (2*borderShift) - (2*b)), (zHeight + (2*borderShift) - (2*b)));
					graphics.setColor(oldColor);
				}
				private void paintButtons(RegionBox box, Graphics graphics) {
					Color oldColor = graphics.getColor();
					Color boxColor = getTypeColor(box.boxType);
					graphics.setColor(boxColor);
					int zdLeft = zoom(toDisplayCoordinate(box.left, true));
					int zdTop = zoom(toDisplayCoordinate(box.top, false));
					int zWidth = zoom(box.width);
					int zHeight = zoom(box.height);
					
					box.hbo = (zWidth < (2 * buttonSize)) ? (buttonSize-(zWidth/2)) : 0;
					box.vbo = (zHeight < (2 * buttonSize)) ? (buttonSize-(zHeight/2)) : 0;
					Box innerBox = buildInnerBox(box);
					if (innerBox != null) {
						int lg = (innerBox.left - box.left);
						int rg = ((box.left + box.width) - (innerBox.left + innerBox.width));
						if ((lg < buttonSize) || (rg < buttonSize))
							box.hbo = Math.max(box.hbo, (buttonSize - 3 - Math.min(lg, rg)));
						int tg = (innerBox.top - box.top);
						int bg = ((box.top + box.height) - (innerBox.top + innerBox.height));
						if ((tg < buttonSize) || (bg < buttonSize))
							box.vbo = Math.max(box.vbo, (buttonSize - 3 - Math.min(tg, bg)));
					}
					int uzHbo = unZoom(box.hbo);
					int uzVbo = unZoom(box.vbo);
					int uzButtonSize = unZoom(buttonSize);
					
					box.resizeLt.left = box.left - uzHbo;
					box.resizeLt.top = box.top - uzVbo;
					box.resizeLt.width = uzButtonSize;
					box.resizeLt.height = uzButtonSize;
					box.remove.left = (box.left + box.width - uzButtonSize + uzHbo);
					box.remove.top = box.top - uzVbo;
					box.remove.width = uzButtonSize;
					box.remove.height = uzButtonSize;
					box.resizeRb.left = (box.left + box.width - uzButtonSize + uzHbo);
					box.resizeRb.top = (box.top + box.height - uzButtonSize + uzVbo);
					box.resizeRb.width = uzButtonSize;
					box.resizeRb.height = uzButtonSize;
					box.retype.left = box.left - uzHbo;
					box.retype.top = (box.top + box.height - uzButtonSize + uzVbo);
					box.retype.width = uzButtonSize;
					box.retype.height = uzButtonSize;
					
					graphics.fillRect(zdLeft-box.hbo, zdTop-box.vbo, buttonSize, buttonSize);
					graphics.fillRect(zdLeft+zWidth-buttonSize+box.hbo, zdTop-box.vbo, buttonSize, buttonSize);
					graphics.fillRect(zdLeft+zWidth-buttonSize+box.hbo, zdTop+zHeight-buttonSize+box.vbo, buttonSize, buttonSize);
					graphics.fillRect(zdLeft-box.hbo, zdTop+zHeight-buttonSize+box.vbo, buttonSize, buttonSize);
					
					graphics.setFont(new Font("Verdana", Font.BOLD, (buttonSize-6)));
					graphics.setColor((Color.RGBtoHSB(boxColor.getRed(), boxColor.getGreen(), boxColor.getBlue(), hsb)[2] < 0.4) ? Color.WHITE : Color.BLACK);
					
					graphics.drawString(box.resizeLt.label, zdLeft-box.hbo+3, zdTop-box.vbo+buttonSize-3);
					graphics.drawString(box.remove.label, zdLeft+zWidth-buttonSize+box.hbo+3, zdTop-box.vbo+buttonSize-3);
					graphics.drawString(box.resizeRb.label, zdLeft+zWidth-buttonSize+box.hbo+3, zdTop+zHeight+box.vbo-3);
					graphics.drawString(box.retype.label, zdLeft-box.hbo+3, zdTop+zHeight+box.vbo-3);
					
					graphics.setColor(oldColor);
				}
			};
			this.canvas.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent me) {
					int x = getX(me);
					int y = getY(me);
					if ((activeBox != null) && (activeButton != null) && boxIncludesPoint(activeButton, x, y)) {
						activeButton.mousePressed();
						return;
					}
					startDrawing(x, y);
					updateCanvas();
				}
				public void mouseReleased(MouseEvent me) {
					int x = getX(me);
					int y = getY(me);
					if (activeBox != null) {
						if (activeButton != null) {
							activeButton.mouseReleased();
							updateCanvas();
							return;
						}
					}
					finishDrawing(x, y);
					updateCanvas();
				}
			});
			this.canvas.addMouseMotionListener(new MouseMotionAdapter() {
				public void mouseDragged(MouseEvent me) {
					int x = getX(me);
					int y = getY(me);
					moveDrawing(x, y);
					updateCanvas();
				}
				public void mouseMoved(MouseEvent me) {
					int x = getX(me);
					int y = getY(me);
					if (activeBox != null) {
						if (boxIncludesPoint(activeBox.resizeLt, x, y)) {
							if (activeButton != activeBox.resizeLt)
								activeBox.resizeLt.mouseOver();
							return;
						}
						else if (boxIncludesPoint(activeBox.remove, x, y)) {
							if (activeButton != activeBox.remove)
								activeBox.remove.mouseOver();
							return;
						}
						else if (boxIncludesPoint(activeBox.resizeRb, x, y)) {
							if (activeButton != activeBox.resizeRb)
								activeBox.resizeRb.mouseOver();
							return;
						}
						else if (boxIncludesPoint(activeBox.retype, x, y)) {
							if (activeButton != activeBox.retype)
								activeBox.retype.mouseOver();
							return;
						}
					}
					Box box = boxAt(x, y);
					if (box != activeBox) {
						if (activeBox != null) {
							activeBox.mouseOut();
							if (activeButton != null)
								activeButton.mouseOut();
						}
						if (box != null)
							box.mouseOver();
						updateCanvas();
					}
				}
			});
			this.canvas.setBackground(Color.WHITE);
			this.add(this.canvas, BorderLayout.CENTER);
			
			this.setScrollableTracksViewportWidth(true);
			this.setScrollableTracksViewportHeight(false);
			
			if (DEBUG) {
				JPanel testButtonPanel = new JPanel(new FlowLayout());
				JButton pws = new JButton("Print Word Sequence");
				pws.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						updateWordBoxSequence(masterBox);
						WordBox wb = masterBox.firstWordBox;
						int wbCount = 0;
						while (wb != null) {
							wbCount++;
							if (wb.boxType != null) {
								System.out.println();
								System.out.println();
								System.out.println(wb.boxType + ":");
							}
							else System.out.print(" ");
							System.out.print(wb.word);
							wb = wb.nextWordBox;
						}
						System.out.println();
						System.out.println();
						System.out.println(wbCount + " words in total");
					}
				});
				testButtonPanel.add(pws);
				JButton wd = new JButton("Write Data");
				wd.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						Writer w = new PrintWriter(System.out);
						try {
							PageCleanupFeedbackPanel.this.writeData(w);
						}
						catch (IOException ioe) {
							ioe.printStackTrace(System.out);
						}
					}
				});
				testButtonPanel.add(wd);
				this.add(testButtonPanel, BorderLayout.SOUTH);
			}
		}
		
		private int zoom(int raw) {
			return ((int) Math.round(((double) raw) * this.zoomFactor));
		}
		private int unZoom(int zoomed) {
			return ((int) Math.round(((double) zoomed) / this.zoomFactor));
		}
		
		private int toDisplayCoordinate(int boxCoordinate, boolean horizontal) {
			return (boxCoordinate - ((horizontal ? this.masterBox.left : this.masterBox.top) - this.borderWidth));
		}
		private int toBoxCoordinate(int displayCoordinate, boolean horizontal) {
			return (displayCoordinate + ((horizontal ? this.masterBox.left : this.masterBox.top) - this.borderWidth));
		}
		
		private int getX(MouseEvent me) {
			return toBoxCoordinate(unZoom(me.getX()), true);
		}
		private int getY(MouseEvent me) {
			return toBoxCoordinate(unZoom(me.getY()), false);
		}
		
		private void updateCanvas() {
			this.canvas.validate();
			this.canvas.repaint();
		}
		
		private boolean boxIncludesPoint(Box box, int x, int y) {
			return ((box != null) && (box.left < x) && (box.top < y) && ((box.left + box.width) > x) && ((box.top + box.height) > y));
		}
		
		private void storeBox(RegionBox box) {
			for (int b = 0; b < this.boxes.size(); b++) {
				Box tBox = ((Box) this.boxes.get(b));
				if ((tBox.parentBox == box.parentBox) && boxContains(box, tBox))
					tBox.parentBox = box;
			}
			this.boxes.add(box);
			this.updateWordBoxSequence(this.masterBox);
		}
		
		private void removeBox(Box box) {
			for (int b = 0; b < this.boxes.size(); b++) {
				Box tBox = ((Box) this.boxes.get(b));
				if (tBox.parentBox == box)
					tBox.parentBox = box.parentBox;
				if (tBox == box)
					this.boxes.remove(b--);
			}
			this.updateWordBoxSequence(this.masterBox);
		}
		
		private Box buildInnerBox(Box box) {
			int minx = 10000;
			int miny = 10000;
			int maxx = -1;
			int maxy = -1;
			for (int b = 0; b < this.boxes.size(); b++) {
				Box tBox = ((Box) this.boxes.get(b));
				if (tBox.parentBox == box) {
					minx = Math.min(minx, tBox.left);
					miny = Math.min(miny, tBox.top);
					maxx = Math.max(maxx, (tBox.left + tBox.width));
					maxy = Math.max(maxy, (tBox.top + tBox.height));
				}
			}
			if (minx > maxx)
				return null;
			Box innerBox = new Box();
			innerBox.left = minx;
			innerBox.top = miny;
			innerBox.width = (maxx-minx);
			innerBox.height = (maxy-miny);
			return innerBox;
		}
		
		private ArrayList getChildBoxes(Box box) {
			ArrayList childBoxes = new ArrayList();
			for (int b = 0; b < this.boxes.size(); b++) {
				Box tBox = ((Box) this.boxes.get(b));
				if (tBox.parentBox == box)
					childBoxes.add(tBox);
			}
			return childBoxes;
		}
		
		private RegionBox boxAt(int x, int y) {
			RegionBox box = this.masterBox;
			boolean zoomIn;
			do {
				zoomIn = false;
				for (int b = 0; b < this.boxes.size(); b++) {
					RegionBox tBox = ((RegionBox) this.boxes.get(b));
					if (((box == null) || (tBox.parentBox == box)) && (tBox.left < x) && (tBox.top < y) && ((tBox.left + tBox.width) > x) && ((tBox.top + tBox.height) > y)) {
						box = tBox;
						zoomIn = true;
						b = this.boxes.size();
					}
				}
			} while (zoomIn);
			return box;
		}
		
		private boolean boxContains(Box outerBox, Box innerBox) {
			return ((outerBox.left <= innerBox.left) && (outerBox.top <= innerBox.top) && ((outerBox.left + outerBox.width) >= (innerBox.left + innerBox.width)) && ((outerBox.top + outerBox.height) >= (innerBox.top + innerBox.height)));
		}
		
		private boolean boxOverlaps(Box box1, Box box2) {
			int il = Math.max(box1.left, box2.left);
			int it = Math.max(box1.top, box2.top);
			int ir = Math.min((box1.left + box1.width), (box2.left + box2.width));
			int ib = Math.min((box1.top + box1.height), (box2.top + box2.height));
			return ((il < ir) && (it < ib));
		}
		
		//	TODOne conflate these variables to activeBox DO NOT DO THIS, AS SETTING OF THESE VARIABLES INDICATES CURRENT ACTION
		private DrawingBox drawing;
		private RegionBox drawingParent;
		
		private RegionBox removing;
		
		private RegionBox resizingLt;
		private ArrayList resizingLtSiblings;
		private Box resizingLtInner;
		
		private RegionBox resizingRb;
		private ArrayList resizingRbSiblings;
		private Box resizingRbInner;
		
		private RegionBox retyping;
		
		private void startDrawing(int x, int y) {
			if (drawing != null) {
				System.out.println("Cannot start drawing, already drawing.");
				return;
			}
			if (removing != null) {
				System.out.println("Cannot start drawing, removing.");
				return;
			}
			if (resizingLt != null) {
				System.out.println("Cannot start drawing, resizing LT.");
				return;
			}
			if (resizingRb != null) {
				System.out.println("Cannot start drawing, resizing RB.");
				return;
			}
			if (retyping != null) {
				System.out.println("Cannot start drawing, retyping.");
				return;
			}
			
			this.stopRetyping();
			this.activeBox = null;
			this.drawing = new DrawingBox(x, y);
			this.drawingParent = this.boxAt(x, y);
			this.drawing.parentBox = drawingParent;
		}
		
		private void moveDrawing(int x, int y) {
			if (drawing != null) {
				int boundsExtension = ((drawingParent == masterBox) ? ((borderWidth * 3) / 4) : 0);
				if ((x >= (drawingParent.left - boundsExtension)) && (x <= (drawingParent.left + drawingParent.width + boundsExtension))) {
					drawing.width = Math.abs(drawing.iLeft - x);
					drawing.left = (drawing.iLeft < x) ? drawing.iLeft : x;
				}
				if ((y >= (drawingParent.top - boundsExtension)) && (y <= (drawingParent.top + drawingParent.height + boundsExtension))) {
					drawing.height = Math.abs(drawing.iTop - y);
					drawing.top = (drawing.iTop < y) ? drawing.iTop : y;
				}
			}
			if (resizingLt != null) {
				int nx = (x-(resizingLt.resizeLt.width/2)+((resizingLt.hbo * resizingLt.resizeLt.width) / buttonSize));
				int ny = (y-(resizingLt.resizeLt.height/2)+((resizingLt.vbo * resizingLt.resizeLt.height) / buttonSize));
				int nw = (resizingLt.left + resizingLt.width) - nx;
				int nh = (resizingLt.top + resizingLt.height) - ny;
				if (resizingLtSiblings != null) {
					Box resizedBox = new Box(nx, ny, nw, nh);
					for (int s = 0; s < resizingLtSiblings.size(); s ++) {
						Box sBox = ((Box) resizingLtSiblings.get(s));
						if (sBox == resizingLt)
							continue;
						if (boxOverlaps(resizedBox, sBox))
							return;
					}
				}
				if ((nx >= resizingLt.parentBox.left) && ((nx + nw) <= (resizingLt.parentBox.left + resizingLt.parentBox.width)) && ((resizingLtInner == null) || ((nx <= resizingLtInner.left) && ((nx + nw) >= (resizingLtInner.left + resizingLtInner.width))))) {
					resizingLt.left = nx;
					resizingLt.width = nw;
				}
				if ((ny >= resizingLt.parentBox.top) && ((ny + nh) <= (resizingLt.parentBox.top + resizingLt.parentBox.height)) && ((resizingLtInner == null) || ((ny <= resizingLtInner.top) && ((ny + nh) >= (resizingLtInner.top + resizingLtInner.height))))) {
					resizingLt.top = ny;
					resizingLt.height = nh;
				}
			}
			if (resizingRb != null) {
				int nw = (Math.abs(x-resizingRb.left)+(resizingRb.resizeRb.width/2)-((resizingRb.hbo * resizingRb.resizeRb.width) / buttonSize));
				int nh = (Math.abs(y-resizingRb.top)+(resizingRb.resizeRb.height/2)-((resizingRb.vbo * resizingRb.resizeRb.height) / buttonSize));
				if (resizingRbSiblings != null) {
					Box resizedBox = new Box(resizingRb.left, resizingRb.top, nw, nh);
					for (int s = 0; s < resizingRbSiblings.size(); s ++) {
						Box sBox = ((Box) resizingRbSiblings.get(s));
						if (sBox == resizingRb)
							continue;
						if (boxOverlaps(resizedBox, sBox))
							return;
					}
				}
				if ((resizingRb.left + nw) <= (resizingRb.parentBox.left + resizingRb.parentBox.width) && ((resizingRbInner == null) || ((resizingRb.left + nw) >= (resizingRbInner.left + resizingRbInner.width)))) {
					resizingRb.width = nw;
				}
				if ((resizingRb.top + nh) <= (resizingRb.parentBox.top + resizingRb.parentBox.height) && ((resizingRbInner == null) || ((resizingRb.top + nh) >= (resizingRbInner.top + resizingRbInner.height)))) {
					resizingRb.height = nh;
				}
			}
		}
		
		private RegionBox finishDrawing(int x, int y) {
			System.out.println("   - finishing drawing");
			if (stopResizingLt() || stopResizingRb())
				return null;
			if (drawing == null)
				return null;
			
			boolean noWord = true;
			for (int  w = 0; noWord && (w < this.wordBoxes.size()); w++) {
				Box wBox = ((Box) this.wordBoxes.get(w));
				if (boxContains(drawing, wBox)) {
					noWord = false;
					continue;
				}
				
				int  il = Math.max(drawing.left, wBox.left);
				int  ir = Math.min((drawing.left + drawing.width), (wBox.left + wBox.width));
				int  it = Math.max(drawing.top, wBox.top);
				int  ib = Math.min((drawing.top + drawing.height), (wBox.top + wBox.height));
				if ((il >= ir) || (it >= ib))
					continue;
				
				float wordInBox = (((float) ((ir - il) * (ib - it))) / (wBox.width * wBox.height));
				if (wordInBox > 0.5)
					noWord = false;
			}
			if (noWord) {
				drawing = null;
				return null;
			}
			System.out.println("     - enclosed words collected");
			
			snapToWordBoxes(drawing);
			System.out.println("     - size reduced to enclosed words");
			
			ArrayList drawingSiblings = getChildBoxes(drawing.parentBox);
			boolean resized = false;
			boolean extending = false;
			do {
				resized = false;
				int lx = drawing.left;
				int ly = drawing.top;
				int lw = drawing.width;
				int lh = drawing.height;
				for (int s = 0; s < drawingSiblings.size(); s++) {
					RegionBox sBox = ((RegionBox) drawingSiblings.get(s));
					int il = Math.max(drawing.left, sBox.left);
					int it = Math.max(drawing.top, sBox.top);
					int ir = Math.min((drawing.left + drawing.width), (sBox.left + sBox.width));
					int ib = Math.min((drawing.top + drawing.height), (sBox.top + sBox.height));
					
					// no overlap, or complete inclusion
					if ((il >= ir) || (it >= ib) || boxContains(drawing, sBox) || boxContains(sBox, drawing))
						continue;
					
					//	we're gonna do something, indicate so
					resized = true;
					
					// compute containment ratios
					float drawingInSibling = (((ir - il) * (ib - it)) / (drawing.width * drawing.height));
					float siblingInDrawing = (((ir - il) * (ib - it)) / (sBox.width * sBox.height));
					
					// drawing covers over 50% of sibling ==> enlarge drawing box
					if (extending || (siblingInDrawing > 0.5)) {
						int jl = Math.min(drawing.left, sBox.left);
						int jt = Math.min(drawing.top, sBox.top);
						int jr = Math.max((drawing.left + drawing.width), (sBox.left + sBox.width));
						int jb = Math.max((drawing.top + drawing.height), (sBox.top + sBox.height));
						drawing.left = jl;
						drawing.top = jt;
						drawing.width = (jr-jl);
						drawing.height = (jb-jt);
						sBox.parentBox = drawing;
						drawingSiblings = getChildBoxes(drawing.parentBox);
						s = drawingSiblings.size();
						extending = true;
					}
					
					// over 50% of drawing box inside sibling ==> put it in there
					else if (drawingInSibling > 0.5){
						drawing.left = il;
						drawing.top = it;
						drawing.width = (ir-il);
						drawing.height = (ib-it);
						drawing.parentBox = sBox;
						drawingSiblings = getChildBoxes(drawing.parentBox);
						s = drawingSiblings.size();
					}
					
					// less than 50% of overlap both ways
					else {
						
						// compute possible cuts
						ArrayList cuts = new ArrayList();
						
						// drawing box extends to the left
						if (il > drawing.left) {
							Box cut = new Box();
							cut.left = drawing.left;
							cut.top = drawing.top;
							cut.width = (il - drawing.left);
							cut.height = drawing.height;
							cut.boxType = "O";
							cuts.add(cut);
						}
						
						// drawing box extends to the right
						if (ir < (drawing.left + drawing.width)) {
							Box cut = new Box();
							cut.left = ir;
							cut.top = drawing.top;
							cut.width = (drawing.width - (ir - drawing.left));
							cut.height = drawing.height;
							cut.boxType = "O";
							cuts.add(cut);
						}
						
						// drawing box extends to the top
						if (it > drawing.top) {
							Box cut = new Box();
							cut.left = drawing.left;
							cut.top = drawing.top;
							cut.width = drawing.width;
							cut.height = (it - drawing.top);
							cut.boxType = "O";
							cuts.add(cut);
						}
						
						// drawing box extends to the bottom
						if (ib < (drawing.top + drawing.height)) {
							Box cut = new Box();
							cut.left = drawing.left;
							cut.top = ib;
							cut.width = drawing.width;
							cut.height = (drawing.height - (ib - drawing.top));
							cut.boxType = "O";
							cuts.add(cut);
						}
						
						// add intersection
						Box cut = new Box();
						cut.left = il;
						cut.top = it;
						cut.width = (ir - il);
						cut.height = (ib - it);
						cut.boxType = "I";
						cuts.add(cut);
						
						// select largest cut
						int maxCutSize = 0;
						for (int c = 0; c < cuts.size(); c++) {
							Box tCut = ((Box) cuts.get(c));
							int cutSize = (tCut.width * tCut.height);
							if (cutSize > maxCutSize) {
								cut = tCut;
								maxCutSize = cutSize;
							}
						}
						
						// adjust drawing box
						drawing.left = cut.left;
						drawing.top = cut.top;
						drawing.width = cut.width;
						drawing.height = cut.height;
						if ("I".equals(cut.boxType)) {
							drawing.parentBox = sBox;
							drawingSiblings = getChildBoxes(drawing.parentBox);
							s = drawingSiblings.size();
							extending = false;
						}
					}
				}
				if (resized && (lx == drawing.left) && (ly == drawing.top) && (lw == drawing.width) && (lh == drawing.height))
					resized = false;
			} while(resized);
			System.out.println("     - conflicts resolved");
			
			snapToWordBoxes(this.drawing);
			System.out.println("     - size reduced to enclosed words");
			
			RegionBox box = new RegionBox(this.drawing, this);
			this.drawing = null;
			storeBox(box);
			System.out.println("     - box stored");
			String boxType = MAIN_TEXT_TYPE;
			if ((box.parentBox != masterBox) && MAIN_TEXT_TYPE.equals(box.parentBox.boxType))
				boxType = CAPTION_TYPE;
			else {
				ArrayList children = this.getChildBoxes(box);
				int mainTextChildren = 0;
				String childBoxType = MAIN_TEXT_TYPE;
				for (int c = 0; c < children.size(); c++) {
					Box cBox = ((Box) children.get(c));
					if (cBox.boxType == null)
						continue;
					else if (MAIN_TEXT_TYPE.equals(cBox.boxType))
						mainTextChildren++;
					else childBoxType = cBox.boxType;
				}
				boxType = ((mainTextChildren > 1) ? COLUMN_TYPE : childBoxType);
			}
			System.out.println("     - child box types updated");
			box.boxType = boxType;
			updateTypeLayout(box);
			System.out.println("     - layout updated");
			return box;
		}
		
		private boolean stopResizingLt() {
			if (resizingLt == null)
				return false;
			snapToWordBoxes(resizingLt);
			updateWordBoxSequence(masterBox);
			resizingLt = null;
			resizingLtInner = null;
			resizingLtSiblings = null;
			updateCanvas();
			return true;
		}
		private boolean stopResizingRb() {
			if (resizingRb == null)
				return false;
			snapToWordBoxes(resizingRb);
			updateWordBoxSequence(masterBox);
			resizingRb = null;
			resizingRbInner = null;
			resizingRbSiblings = null;
			updateCanvas();
			return true;
		}
		private void startRetyping(RegionBox box) {
			if (this.retyping != null)
				this.stopRetyping();
			this.retyping = box;
			for (Iterator toit = this.boxTypeOptions.keySet().iterator(); toit.hasNext();) {
				String boxType = ((String) toit.next());
				JMenuItem boxTypeOption = ((JMenuItem) this.boxTypeOptions.get(boxType));
				Font btoFont = boxTypeOption.getFont();
				boolean gotBoxType = boxType.equals(box.boxType);
				if (gotBoxType) {
					if (btoFont.isBold()) // we're done, as the right option already is in bold
						break;
					else boxTypeOption.setFont(btoFont.deriveFont(Font.BOLD));
				}
				else if (btoFont.isBold()) // we need to change the font only if it is bold
					boxTypeOption.setFont(btoFont.deriveFont(Font.PLAIN));
			}
			this.typeSelector.show(this.canvas, zoom(toDisplayCoordinate((this.retyping.retype.left + this.retyping.retype.width - 1), true)), zoom(toDisplayCoordinate(this.retyping.retype.top, false)));
		}
		private void stopRetyping() {
			if (this.retyping != null)
				this.retyping = null;
			this.typeSelector.setVisible(false);
			this.updateCanvas();
		}
		private void updateTypeLayout(RegionBox box) {
			if ((box == null) || (box == masterBox))
				return;
			Color borderColor = this.getTypeColor(box.boxType);
			if (borderColor == null)
				borderColor = Color.GREEN;
			this.updateWordBoxLayout(box, wordBoxes);
			this.updateWordBoxSequence(masterBox);
		}
		
		private void snapToWordBoxes(RegionBox box) {
			if ((box == null) || (box == masterBox))
				return;
			System.out.println("     - reducing size to enclosed word boxes");
			
			int wbl = (box.left + box.width);
			int wbr = box.left;
			int wbt = (box.top + box.height);
			int wbb = box.top;
			for (int  w = 0; w < this.wordBoxes.size(); w++) {
				Box wBox = ((Box) this.wordBoxes.get(w));
				if (boxContains(box, wBox)) {
					wbl = Math.min(wbl, wBox.left);
					wbr = Math.max(wbr, (wBox.left + wBox.width));
					wbt = Math.min(wbt, wBox.top);
					wbb = Math.max(wbb, (wBox.top + wBox.height));
					continue;
				}
				int  il = Math.max(box.left, wBox.left);
				int  ir = Math.min((box.left + box.width), (wBox.left + wBox.width));
				int  it = Math.max(box.top, wBox.top);
				int  ib = Math.min((box.top + box.height), (wBox.top + wBox.height));
				if ((il >= ir) || (it >= ib))
					continue;
				
				float wordInBox = (((float) ((ir - il) * (ib - it))) / (wBox.width * wBox.height));
				if (wordInBox > 0.5) {
					wbl = Math.min(wbl, wBox.left);
					wbr = Math.max(wbr, (wBox.left + wBox.width));
					wbt = Math.min(wbt, wBox.top);
					wbb = Math.max(wbb, (wBox.top + wBox.height));
				}
			}
			System.out.println("       - bounds computed");
			
			ArrayList toBeChildren = new ArrayList();
			for (int b = 0; b < this.boxes.size(); b++) {
				Box cBox = ((Box) this.boxes.get(b));
				if ((cBox != box) && boxContains(box, cBox))
					toBeChildren.add(cBox);
			}
			System.out.println("       - children collected");
			if (toBeChildren.size() != 0) {
				int ibl = Integer.MAX_VALUE;
				int ibt = Integer.MAX_VALUE;
				int ibr = -1;
				int ibb = -1;
				for (int b = 0; b < toBeChildren.size(); b++) {
					Box cBox = ((Box) toBeChildren.get(b));
					ibl = Math.min(ibl, cBox.left);
					ibt = Math.min(ibt, cBox.top);
					ibr = Math.max(ibr, (cBox.left + cBox.width));
					ibb = Math.max(ibb, (cBox.top + cBox.height));
				}
				
				if ((ibl - wbl) < (buttonSize/2))
					wbl = Math.max(box.left, (ibl - (buttonSize/2)));
				if ((wbr - ibr) < (buttonSize/2))
					wbr = Math.min((box.left + box.width), (ibr + (buttonSize/2)));
					
				if ((ibt - wbt) < (buttonSize/2))
					wbt = Math.max(box.top, (ibt - (buttonSize/2)));
				if ((wbb - ibb) < (buttonSize/2))
					wbb = Math.min((box.top + box.height), (ibb + (buttonSize/2)));
			}
			System.out.println("       - adjusted to children");
			
			if ((wbl < wbr) && (wbt < wbb)) {
				box.left = wbl;
				box.top = wbt;
				box.width = (wbr - wbl);
				box.height = (wbb - wbt);
				boolean update;
				HashSet parentBoxes = new HashSet();
				do {
					update = false;
					for (int b = 0; b < this.boxes.size(); b++) {
						RegionBox tBox = ((RegionBox) this.boxes.get(b));
						if (!parentBoxes.contains(tBox) && (tBox != box.parentBox) && (tBox != box) && boxContains(box.parentBox, tBox) && boxContains(tBox, box)) {
							box.parentBox = tBox;
							System.out.println("       - parent box set to box " + b);
							update = true;
							parentBoxes.add(tBox);
							b = boxes.size();
						}
					}
				} while (update);
			}
			System.out.println("       - box hierarchy updated");
		}
		
		private void updateWordBoxSequence(RegionBox box) {
			if (box == null)
				return;
			if ((box != this.masterBox) && (box.parentBox != this.masterBox)) {
				if (box != box.parentBox)
					updateWordBoxSequence(box.parentBox);
			}
			else {
				ArrayList descendingWordBoxes = new ArrayList();
				for (int w = 0; w < this.wordBoxes.size(); w++) {
					Box wBox = ((Box) this.wordBoxes.get(w));
					if (boxContains(box, wBox)) {
						wBox.parentBox = box;
						descendingWordBoxes.add(wBox);
					}
				}
				if (descendingWordBoxes.size() == 0)
					return;
				markNonChildWordBoxes(box, descendingWordBoxes);
				updateWordBoxSequence(box, descendingWordBoxes);
			}
		}
		private void markNonChildWordBoxes(RegionBox box, ArrayList descendingWordBoxes) {
			box.isContainer = ((box == masterBox) || COLUMN_TYPE.equals(box.boxType));
			ArrayList childBoxes = getChildBoxes(box);
			if (childBoxes.size() == 0)
				return;
			
			for (int c = 0; c < childBoxes.size(); c++) {
				RegionBox cBox = ((RegionBox) childBoxes.get(c));
				ArrayList childDescendingWordBoxes = new ArrayList();
				for (int w = 0; w < descendingWordBoxes.size(); w++) {
					Box wBox = ((Box) descendingWordBoxes.get(w));
					if (boxContains(cBox, wBox)) {
						wBox.parentBox = cBox;
						childDescendingWordBoxes.add(wBox);
					}
				}
				box.isContainer = (box.isContainer || MAIN_TEXT_TYPE.equals(cBox.boxType) || CONTINUE_MAIN_TEXT_TYPE.equals(cBox.boxType));
				markNonChildWordBoxes(cBox, childDescendingWordBoxes);
			}
		}
		
		private double minLineWordBoxOverlap = 0.7; // TODO figure out (from deployment experience) if this is sufficiently strict
		private double ascDescRatio = 0.25; // TODOne ==> SEEMS RESONABLE FOR MANY FONTS figure out (from deployment experience) if this is sufficiently accurate an estimate on average
		
		private void updateWordBoxSequence(RegionBox box, ArrayList descendingWordBoxes) {
			
			// do children first so we can return if we have no direct child word boxes
			ArrayList childBoxes = getChildBoxes(box);
			
			// sort child word boxes left to right
			Collections.sort(childBoxes, new Comparator() {
				public int compare(Object cb1, Object cb2) {
					return (((Box) cb1).left - ((Box) cb2).left);
				}
			});
			
			// order child boxes, left to right as long as there is some overlap, then downward
			ArrayList orderedChildBoxes = new ArrayList();
			while (childBoxes.size() > 0) {
				
				// find top-left word box (start of next line)
				int sci = -1;
				for (int c = 0; c < childBoxes.size(); c++) {
					Box cBox = ((Box) childBoxes.get(c));
					if (cBox == null)
						continue;
					if ((sci == -1) || boxAbove(cBox, ((Box) childBoxes.get(sci))))
						sci = c;
				}
				
				// nothing found ==> no boxes left to handle
				if (sci == -1)
					return;
				
				// initialize box line
				Box lineTailBox = ((Box) childBoxes.get(sci));
				orderedChildBoxes.add(lineTailBox);
				childBoxes.remove(sci);
				
				// traverse box line
				Box boxLineBounds = new Box(0, lineTailBox.top, 0, lineTailBox.height);
				for (int c = 0; c < childBoxes.size(); c++) {
					Box cBox = ((Box) childBoxes.get(c));
					if (cBox == null)
						continue;
					
					//	current box is to the right, and not completely out of box line bounds
					if (boxOnRightOf(cBox, lineTailBox) && !boxBelow(cBox, boxLineBounds)) {
						if (boxMutualLineOverlap(cBox, 0, 0, boxLineBounds, 0, 0) < minLineWordBoxOverlap)
							continue;
						lineTailBox = cBox;
						orderedChildBoxes.add(lineTailBox);
						boxLineBounds.top = Math.min(boxLineBounds.top, lineTailBox.top);
						boxLineBounds.height = Math.max((boxLineBounds.top + boxLineBounds.height), (lineTailBox.top + lineTailBox.height))-boxLineBounds.top;
						childBoxes.remove(c--);
					}
				}
			}
			
			// go on with sorted child boxes
			childBoxes = orderedChildBoxes;
			
			// handle content of child boxes first to facilitate later chaining
			for (int c = 0; c < childBoxes.size(); c++)
				updateWordBoxSequence(((RegionBox) childBoxes.get(c)), descendingWordBoxes);
			
			// find direct child word boxes
			ArrayList childWordBoxes = new ArrayList();
			for (int w = 0; w < descendingWordBoxes.size(); w++) {
				Box wBox = ((Box) descendingWordBoxes.get(w));
				if (wBox.parentBox == box)
					childWordBoxes.add(wBox);
			}
			
			// no direct child word boxes, we're done
			if (childWordBoxes.size() == 0) {
				
				// set first and last word box according to children
				if (childBoxes.size() != 0) {
					box.firstWordBox = ((RegionBox) childBoxes.get(0)).firstWordBox;
					box.lastWordBox = ((RegionBox) childBoxes.get(childBoxes.size()-1)).lastWordBox;
					
					// chain first and last word boxes across children
					for (int c = 1; c < childBoxes.size(); c++) {
						((RegionBox) childBoxes.get(c-1)).lastWordBox.nextWordBox = ((RegionBox) childBoxes.get(c)).firstWordBox;
						((RegionBox) childBoxes.get(c)).firstWordBox.previousWordBox = ((RegionBox) childBoxes.get(c-1)).lastWordBox;
					}
				}
				
				//	we're done
				return;
			}
			
			// update colors
			updateWordBoxLayout(box, childWordBoxes);
			
			// sort child word boxes left to right
			Collections.sort(childWordBoxes, new Comparator() {
				public int compare(Object wb1, Object wb2) {
					return (((Box) wb1).left - ((Box) wb2).left);
				}
			});
			
			// traverse lines
			ArrayList lineWordBoxes = null;
			while (childWordBoxes.size() > 0) {
				
				// find top-left word box (start of next line)
				int lswi = -1;
				for (int w = 0; w < childWordBoxes.size(); w++) {
					Box wBox = ((Box) childWordBoxes.get(w));
					if (wBox == null)
						continue;
					if ((lswi == -1) || boxAbove(wBox, ((Box) childWordBoxes.get(lswi))))
						lswi = w;
				}
				
				// nothing found ==> no word boxes left to handle
				if (lswi == -1) {
					return;
				}
				
				// initialize line start
				WordBox lineTailWordBox = ((WordBox) childWordBoxes.get(lswi));
				childWordBoxes.remove(lswi);
				
				// first line
				if (lineWordBoxes == null) {
					lineTailWordBox.boxType = box.boxType;
					lineTailWordBox.previousWordBox = null;
					box.firstWordBox = lineTailWordBox;
				}
				
				// second line onward
				else {
					lineTailWordBox.boxType = null;
					((WordBox) lineWordBoxes.get(lineWordBoxes.size()-1)).nextWordBox = lineTailWordBox;
					lineTailWordBox.previousWordBox = ((WordBox) lineWordBoxes.get(lineWordBoxes.size()-1));
				}
				
				// initialize new line
				lineWordBoxes = new ArrayList();
				lineWordBoxes.add(lineTailWordBox);
				
				// traverse line
				Box lineBounds = new Box(0, lineTailWordBox.top, 0, lineTailWordBox.height);
				boolean lineHasAscent = lineTailWordBox.hasAscent;
				boolean lineHasDescent = lineTailWordBox.hasDescent;
				double lineFillRatio = 1.0 - (lineHasAscent ? 0 : ascDescRatio) - (lineHasDescent ? 0 : ascDescRatio);
				int lineHeigth = ((int) (lineBounds.height * (1.0 / lineFillRatio)));
				int lineAscDesc = ((int) (lineHeigth * ascDescRatio));
				for (int w = 0; w < childWordBoxes.size(); w++) {
					WordBox wBox = ((WordBox) childWordBoxes.get(w));
					if (wBox == null)
						continue;
					
					//	current word box is to the right, and not completely out of line bounds
					if (boxOnRightOf(wBox, lineTailWordBox) && !boxBelow(wBox, lineBounds)) {
						float lineOverlap = boxMutualLineOverlap(wBox, (wBox.hasAscent ? 0 : lineAscDesc), (wBox.hasDescent ? 0 : lineAscDesc), lineBounds, (lineHasAscent ? 0 : lineAscDesc), (lineHasDescent ? 0 : lineAscDesc));
						if (lineOverlap < minLineWordBoxOverlap)
							continue;
						lineTailWordBox.nextWordBox = wBox;
						wBox.previousWordBox = lineTailWordBox;
						lineTailWordBox = wBox;
						lineWordBoxes.add(lineTailWordBox);
						wBox.boxType = null;
						
						int lbt = lineBounds.top;
						lineBounds.top = Math.min(lbt, lineTailWordBox.top);
						lineBounds.height = Math.max((lbt + lineBounds.height), (lineTailWordBox.top + lineTailWordBox.height))-lineBounds.top;
						
						lineHasAscent = (lineHasAscent || lineTailWordBox.hasAscent);
						lineHasDescent = (lineHasDescent || lineTailWordBox.hasDescent);
						lineFillRatio = 1.0 - (lineHasAscent ? 0 : ascDescRatio) - (lineHasDescent ? 0 : ascDescRatio);
						lineHeigth = ((int) (lineBounds.height * (1.0 / lineFillRatio)));
						lineAscDesc = ((int) (lineHeigth * ascDescRatio));
						
						childWordBoxes.remove(w--);
					}
				}
				
				// mark line end
				lineTailWordBox.nextWordBox = null;
			}
			
			// store last word box
			if (lineWordBoxes.size() != 0)
				box.lastWordBox = ((WordBox) lineWordBoxes.get(lineWordBoxes.size()-1));
				
			// update first and last word box according to children
			if (childBoxes.size() != 0) {
				
				//	chain in own child word boxes
				box.lastWordBox.nextWordBox = ((RegionBox) childBoxes.get(0)).firstWordBox;
				((RegionBox) childBoxes.get(0)).firstWordBox.previousWordBox = box.lastWordBox;
				
				//	update own end
				box.lastWordBox = ((RegionBox) childBoxes.get(childBoxes.size()-1)).lastWordBox;
				
				// chain first and last word boxes across children
				for (int c = 1; c < childBoxes.size(); c++) {
					if (((RegionBox) childBoxes.get(c-1)).lastWordBox != null)
						((RegionBox) childBoxes.get(c-1)).lastWordBox.nextWordBox = ((RegionBox) childBoxes.get(c)).firstWordBox;
					if (((RegionBox) childBoxes.get(c)).firstWordBox != null)
						((RegionBox) childBoxes.get(c)).firstWordBox.previousWordBox = ((RegionBox) childBoxes.get(c-1)).lastWordBox;
				}
			}
		}
		private float boxMutualLineOverlap(Box box1, int b1Asc, int b1Desc, Box box2, int b2Asc, int b2Desc) {
			int b1t = (box1.top - b1Asc);
			int b1b = (box1.top + box1.height + b1Desc);
			int b2t = (box2.top - b2Asc);
			int b2b = (box2.top + box2.height + b2Desc);
			int ot = Math.max(b1t, b2t);
			int ob = Math.min(b1b, b2b);
			float overlap = (ob - ot);
			if (overlap <= 0)
				return 0;
			return (overlap / Math.min((b1b-b1t), (b2b-b2t)));
		}
//		private boolean boxOnLeftOf(Box leftBox, Box rightBox) {
//			return ((leftBox.left + leftBox.width) <= rightBox.left);
//		}
		private boolean boxOnRightOf(Box rightBox, Box leftBox) {
			return (rightBox.left >= (leftBox.left + leftBox.width));
		}
		private boolean boxAbove(Box upperBox, Box lowerBox) {
			return ((upperBox.top + upperBox.height) <= lowerBox.top);
		}
		private boolean boxBelow(Box lowerBox, Box upperBox) {
			return (lowerBox.top >= (upperBox.top + upperBox.height));
		}
		private void updateWordBoxLayout(RegionBox box, ArrayList descendingWordBoxes) {
			for (int w = 0; w < descendingWordBoxes.size(); w++) {
				Box wBox = ((Box) descendingWordBoxes.get(w));
				if (wBox.parentBox == box) {
					if ((wBox.parentBox == masterBox) || wBox.parentBox.isContainer || COLUMN_TYPE.equals(wBox.parentBox.boxType))
						wBox.borderWidth = 3;
					else wBox.borderWidth = 1;
				}
			}
		}
		
		WordBox addWordBox(int left, int top, int width, int height, int baseline, String word, String id, boolean updateSequence, String fontName, int fontStyle, int fontSize) {
			WordBox wb = new WordBox(left, top, width, height, baseline, fontName, fontStyle, fontSize, word, id);
			this.wordBoxes.add(wb);
			this.updateMasterBox(updateSequence);
			wb.parentBox = this.masterBox;
			this.wordBoxesByID.put(id, wb);
			return wb;
		}
//		WordBox addWordBox(int left, int top, int width, int height, int baseline, String word, String id, boolean updateSequence) {
//			WordBox wb = new WordBox(left, top, width, height, baseline, word, id);
//			this.wordBoxes.add(wb);
//			this.updateMasterBox(updateSequence);
//			wb.parentBox = this.masterBox;
//			this.wordBoxesByID.put(id, wb);
//			return wb;
//		}
		
		WordBox getFirstWordBox() {
			return this.masterBox.firstWordBox;
		}
		
		void addRegionBox(int left, int top, int width, int height, String boxType) {
			if (boxType == null)
				boxType = MAIN_TEXT_TYPE;
			startDrawing(left, top);
			System.out.println("   - drawing started");
			moveDrawing((left+width), (top+height));
			System.out.println("   - moved");
			RegionBox box = finishDrawing((left+width), (top+height));
			System.out.println("   - drawing finished");
			if (box == null)
				return;
			box.boxType = boxType;
			System.out.println("   - box type set");
			updateTypeLayout(box);
			System.out.println("   - type layout updated");
		}
		
		RegionBox[] getRegionBoxes() {
			ArrayList boxes = new ArrayList();
			for (int b = 0; b < this.boxes.size(); b++) {
				RegionBox box = ((RegionBox) this.boxes.get(b));
				if (box != this.masterBox)
					boxes.add(box);
			}
			return ((RegionBox[]) boxes.toArray(new RegionBox[boxes.size()]));
		}
		
		private void updateMasterBox(boolean updateSequence) {
			if (this.masterBox == null) {
				this.masterBox = new RegionBox(0, 0, 0, 0, null);
				this.masterBox.isContainer = true;
				this.boxes.add(this.masterBox);
			}
			int mbl = Integer.MAX_VALUE;
			int mbr = 0;
			int mbt = Integer.MAX_VALUE;
			int mbb = 0;
			for (int w = 0; w < this.wordBoxes.size(); w++) {
				WordBox wb = ((WordBox) this.wordBoxes.get(w));
				mbl = Math.min(mbl, wb.left);
				mbr = Math.max(mbr, (wb.left+wb.width));
				mbt = Math.min(mbt, wb.top);
				mbb = Math.max(mbb, (wb.top+wb.height));
			}
			this.masterBox.left = mbl;
			this.masterBox.width = (mbr - mbl);
			this.masterBox.top = mbt;
			this.masterBox.height = (mbb - mbt);
			Dimension size = new Dimension((this.masterBox.width + (2 * this.borderWidth)), (this.masterBox.height + (2 * this.borderWidth)));
			this.setMinimumSize(size);
			this.setMaximumSize(size);
			this.setPreferredSize(size);
			if (updateSequence)
				this.updateWordBoxSequence(this.masterBox);
		}
		
		private HashMap boxTypeColors = new LinkedHashMap();
		private HashMap boxTypeOptions = new LinkedHashMap();
		void addBoxType(final String boxType, String typeLabel, Color typeColor) {
			this.boxTypeColors.put(boxType, typeColor);
			JMenuItem typeOption = ((JMenuItem) this.boxTypeOptions.get(boxType));
			if (typeOption == null) {
				typeOption = new JMenuItem("");
				typeOption.setBackground(Color.WHITE);
				typeOption.setBorderPainted(true);
				typeOption.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (retyping != null) {
							retyping.boxType = boxType;
							updateTypeLayout(retyping);
							stopRetyping();
						}
					}
				});
				this.typeSelector.add(typeOption);
				this.boxTypeOptions.put(boxType, typeOption);
			}
			typeOption.setText(typeLabel);
			typeOption.setBorder(BorderFactory.createLineBorder(typeColor, 2));
		}
		
		private Color getTypeColor(String boxType) {
			Color typeColor = ((Color) this.boxTypeColors.get(boxType));
			return ((typeColor == null) ? Color.BLACK : typeColor);
		}
		
		private static String ascending = "bdfhijklt!§$%&/\\^°'\"*#?()[]{}0123456789²³´`";
		private static boolean hasAscent(String str) {
			for (int c = 0; c < str.length(); c++) {
				char ch = str.charAt(c);
				if ((ch >= 'A') && (ch <= 'Z'))
					return true;
				if (ascending.indexOf(ch) != -1)
					return true;
			}
			return false;
		}
		private static String medianLess = "^°'*\"´`²³_";
		private static boolean hasMedian(String str) {
			for (int c = 0; c < str.length(); c++) {
				char ch = str.charAt(c);
				if (medianLess.indexOf(ch) == -1)
					return true;
			}
			return false;
		}
		private static String descending = "gjpqy();,§_";
		private static boolean hasDescent(String str) {
			for (int c = 0; c < str.length(); c++) {
				char ch = str.charAt(c);
				if (descending.indexOf(ch) != -1)
					return true;
			}
			return false;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#checkFeedback(java.lang.String)
		 */
		public String[] checkFeedback(String status) {
			if (status.startsWith("OK")) {
				updateWordBoxSequence(masterBox);
				for (int w = 0; w < this.wordBoxes.size(); w++) {
					Box wBox = ((Box) this.wordBoxes.get(w));
					if ((wBox.parentBox == null) || (wBox.parentBox == masterBox) || wBox.parentBox.isContainer || COLUMN_TYPE.equals(wBox.parentBox.boxType))
						return errorReport;
				}
			}
			return null;
		}
		private static final String[] errorReport = {
			"Some words (the ones that are marked with thick red frames) are not properly nested in paragraphs.",
			"Boxes with nested main text paragraphs are used for grouping only and do not count as proper parents.",
			"If they do not belong to any paragraph, and not to the text at all, please mark them as OCR artifacts."
		};
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getComplexity()
		 */
		public int getComplexity() {
			return this.getDecisionComplexity() * this.getDecisionCount();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
		 */
		public int getDecisionComplexity() {
			return 2;// (anything else would be inflation)
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
		 */
		public int getDecisionCount() {
			return this.wordBoxes.size();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
		 */
		public Properties getFieldStates() {
			this.updateWordBoxSequence(this.masterBox);
			Properties fieldStates = new Properties();
			for (int w = 0; w < this.wordBoxes.size(); w++) {
				WordBox wb = ((WordBox) this.wordBoxes.get(w));
				fieldStates.setProperty((wb.id + "N"), ((wb.nextWordBox == null) ? "" : wb.nextWordBox.id));
			}
			for (int b = 0; b < this.boxes.size(); b++) {
				RegionBox rb = ((RegionBox) this.boxes.get(b));
				if (rb == this.masterBox) {
					fieldStates.setProperty("firstWbId", ((rb.firstWordBox == null) ? "" : rb.firstWordBox.id));
				}
				else {
					fieldStates.setProperty(("B" + b + "F"), ((rb.firstWordBox == null) ? "" : rb.firstWordBox.id));
					fieldStates.setProperty(("B" + b + "L"), ((rb.lastWordBox == null) ? "" : rb.lastWordBox.id));
					fieldStates.setProperty(("B" + b + "T"), ((rb.boxType == null) ? "" : rb.boxType));
				}
			}
			return fieldStates;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
		 */
		public void setFieldStates(Properties states) {
			for (int w = 0; w < this.wordBoxes.size(); w++) {
				WordBox wb = ((WordBox) this.wordBoxes.get(w));
				wb.nextWordBox = ((WordBox) this.wordBoxesByID.get(states.getProperty(wb.id + "N")));
			}
			this.boxes.clear();
			this.boxes.add(this.masterBox);
			this.masterBox.firstWordBox = ((WordBox) this.wordBoxesByID.get(states.getProperty("firstWbId")));
			ArrayList boxes = new ArrayList();
			int boxId = 1; // skipping 0, as this is the master box, which has special treatment
			while (true) {
				WordBox fWb = ((WordBox) this.wordBoxesByID.get(states.getProperty("B" + boxId + "F")));
				WordBox lWb = ((WordBox) this.wordBoxesByID.get(states.getProperty("B" + boxId + "L")));
				if ((fWb == null) || (lWb == null))
					break;
				Box box = this.buildBoundingBox(fWb, lWb);
				box.boxType = states.getProperty(("B" + boxId + "T"), MAIN_TEXT_TYPE);
				boxes.add(box);
				boxId++;
			}
			for (int b = 0; b < boxes.size(); b++) {
				Box box = ((Box) boxes.get(b));
				this.addRegionBox(box.left, box.top, box.width, box.height, box.boxType);
			}
//			
//			THIS IS OBSOLETE, AS ADDING A REGION BOX DOES THE UPDATE AUTOMATICALLY
//			this.updateWordBoxSequence(this.masterBox);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
		 */
		public void writeData(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			
			//	write generic data
			super.writeData(bw);
			
			//	make sure data structures are up to date
			this.updateWordBoxSequence(this.masterBox);
			
			//	write image access data and rendering and line computation parameters (no need to send first word box ID, as this is computed dynamically)
			bw.write("" + this.imageProviderBaseUrl);
			bw.newLine();
			bw.write("" + this.docId);
			bw.newLine();
			bw.write("" + this.pageId);
			bw.newLine();
			bw.write("" + this.borderWidth);
			bw.newLine();
			bw.write("" + this.buttonSize);
			bw.newLine();
			bw.write("" + this.minLineWordBoxOverlap);
			bw.newLine();
			bw.write("" + this.ascDescRatio);
			bw.newLine();
			
			//	write box types and colors
			for (Iterator btit = this.boxTypeColors.keySet().iterator(); btit.hasNext();) {
				String boxType = ((String) btit.next());
				Color boxTypeColor = this.getTypeColor(boxType);
				String boxTypeLabel = ((JMenuItem) this.boxTypeOptions.get(boxType)).getText();
				bw.write(getRGB(boxTypeColor) + " " + URLEncoder.encode(boxType, "UTF-8") + " " + URLEncoder.encode(boxTypeLabel, "UTF-8"));
				bw.newLine();
			}
			bw.newLine();
			
			//	write word boxes
			for (int w = 0; w < this.wordBoxes.size(); w++) {
				WordBox wb = ((WordBox) this.wordBoxes.get(w));
				bw.write(wb.id + " " + 
						wb.left + " " + 
						wb.top + " " + 
						wb.width + " " + 
						wb.height + " " + 
						wb.baseline + " " + 
						((wb.nextWordBox == null) ? "" : wb.nextWordBox.id) + " " + 
						URLEncoder.encode(wb.word, "UTF-8") + " " +
						URLEncoder.encode(wb.fontName, "UTF-8") + " " +
						wb.fontStyle + " " + 
						wb.fontSize + 
					"");
				bw.newLine();
			}
			bw.newLine();
			
			//	write boxes (skip master box, though, as it is computed automatically from word boxes)
			for (int b = 0; b < this.boxes.size(); b++) {
				RegionBox rb = ((RegionBox) this.boxes.get(b));
				if (rb == this.masterBox)
					continue;
				bw.write(rb.firstWordBox.id + " " + rb.lastWordBox.id + " " + URLEncoder.encode(rb.boxType, "UTF-8"));
				bw.newLine();
			}
			
			//	send data
			bw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#initFields(java.io.Reader)
		 */
		public void initFields(Reader in) throws IOException {
			BufferedReader br = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
			super.initFields(br);
			
			//	read image access data and rendering and line computation parameters
			this.imageProviderBaseUrl = br.readLine();
			this.docId = br.readLine();
			this.pageId = br.readLine();
			this.borderWidth = Integer.parseInt(br.readLine());
			this.buttonSize = Integer.parseInt(br.readLine());
			this.minLineWordBoxOverlap = Double.parseDouble(br.readLine());
			this.ascDescRatio = Double.parseDouble(br.readLine());
			
			//	read box types
			this.boxTypeColors.clear();
			this.boxTypeOptions.clear();
			this.typeSelector.removeAll();
			String boxTypeData;
			while (((boxTypeData = br.readLine()) != null) && (boxTypeData.length() != 0)) {
				Color boxTypeColor = getColor(boxTypeData.substring(0, 6));
				boxTypeData = boxTypeData.substring(7);
				String boxType = URLDecoder.decode(boxTypeData.substring(0, boxTypeData.indexOf(" ")), "UTF-8");
				boxTypeData = boxTypeData.substring(boxTypeData.indexOf(" ") + 1);
				String boxTypeLabel = URLDecoder.decode(boxTypeData, "UTF-8");
				this.addBoxType(boxType, boxTypeLabel, boxTypeColor);
			}
			
			//	read word boxes
			this.wordBoxes.clear();
			this.wordBoxesByID.clear();
			String wordBoxData;
			Properties nWbIdCache = new Properties(); // we need to cache successor ID, as successor might not have been added jet, so we need to chain the word boxes later
			while (((wordBoxData = br.readLine()) != null) && (wordBoxData.length() != 0)) {
				String[] wbData = wordBoxData.split("\\s");
				String wbId = wbData[0];
				int wbLeft = Integer.parseInt(wbData[1]);
				int wbTop = Integer.parseInt(wbData[2]);
				int wbWidth = Integer.parseInt(wbData[3]);
				int wbHeight = Integer.parseInt(wbData[4]);
				int wbBaseline = Integer.parseInt(wbData[5]);
				String nWbId = wbData[6];
				String wbWord = URLDecoder.decode(wbData[7], "UTF-8");
				String wbFontName = URLDecoder.decode(wbData[8], "UTF-8");
				int wbFontStyle = Integer.parseInt(wbData[9]);
				int wbFontSize = Integer.parseInt(wbData[10]);
				this.addWordBox(wbLeft, wbTop, wbWidth, wbHeight, wbBaseline, wbWord, wbId, false, wbFontName, wbFontStyle, wbFontSize);
				nWbIdCache.setProperty(wbId, nWbId);
			}
			for (int w = 0; w < this.wordBoxes.size(); w++) {
				WordBox wb = ((WordBox) this.wordBoxes.get(w));
				wb.nextWordBox = ((WordBox) this.wordBoxesByID.get(nWbIdCache.getProperty(wb.id)));
			}
			
			//	read boxes
			this.boxes.clear();
			this.boxes.add(this.masterBox);
			ArrayList boxCache = new ArrayList();
			String boxData;
			while (((boxData = br.readLine()) != null) && (boxData.length() != 0)) {
				String[] rbData = boxData.split("\\s");
				String fWbId = rbData[0];
				String lWbId = rbData[1];
				String boxType = rbData[2];
				WordBox fWb = ((WordBox) this.wordBoxesByID.get(fWbId));
				WordBox lWb = ((WordBox) this.wordBoxesByID.get(lWbId));
				if ((fWb == null) || (lWb == null))
					break;
				Box box = this.buildBoundingBox(fWb, lWb);
				box.boxType = boxType;
				boxCache.add(box);
			}
			for (int b = 0; b < boxCache.size(); b++) {
				Box box = ((Box) boxCache.get(b));
				this.addRegionBox(box.left, box.top, box.width, box.height, box.boxType);
			}
//			
//			THIS IS OBSOLETE, AS ADDING A REGION BOX DOES THE UPDATE AUTOMATICALLY
//			//	recompute internal data
//			this.updateWordBoxSequence(this.masterBox);
		}
		
		private Box buildBoundingBox(WordBox fWb, WordBox lWb) {
			int wbl = fWb.left;
			int wbr = (fWb.left + fWb.width);
			int wbt = fWb.top;
			int wbb = (fWb.top + fWb.height);
			WordBox nWb = fWb;
			while (true) {
				if (nWb == lWb)
					break;
				nWb = nWb.nextWordBox;
				if (nWb == null)
					break;
				wbl = Math.min(wbl, nWb.left);
				wbr = Math.max(wbr, (nWb.left + nWb.width));
				wbt = Math.min(wbt, nWb.top);
				wbb = Math.max(wbb, (nWb.top + nWb.height));
			}
			return new Box(wbl, wbt, (wbr - wbl), (wbb - wbt));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#readResponse(java.util.Properties)
		 */
		public void readResponse(Properties response) {
			Stack boxDataStack = new Stack();
			ArrayList boxDataList = new ArrayList();
			for (int w = 0; w < this.wordBoxes.size(); w++) {
				WordBox wb = ((WordBox) this.wordBoxes.get(w));
				wb.nextWordBox = ((WordBox) this.wordBoxesByID.get(response.getProperty(wb.id + "N")));
				wb.boxType = response.getProperty(wb.id + "T");
			}
			
			WordBox wb = ((WordBox) this.wordBoxesByID.get(response.getProperty("firstWbId")));
			while (wb != null) {
				if ("C".equals(wb.boxType)) {
					wb = wb.nextWordBox;
					continue;
				}
				String[] boxTypes = wb.boxType.split("\\.");
				int boxesContinued = 0;
				for (int t = 0; t < boxTypes.length; t++) {
					if ("C".equals(boxTypes[t]))
						boxesContinued++;
					else t = boxTypes.length;
				}
				while (boxDataStack.size() > boxesContinued) {
					BoxData bd = ((BoxData) boxDataStack.pop());
					bd.lWbNext = wb;
					boxDataList.add(bd);
				}
				for (int t = boxesContinued; t < boxTypes.length; t++) {
					BoxData bd = new BoxData(wb, boxTypes[t]);
					boxDataStack.push(bd);
				}
				wb = wb.nextWordBox;
			}
			while (boxDataStack.size() != 0)
				boxDataList.add(boxDataStack.pop());
			
			this.boxes.clear();
			this.boxes.add(this.masterBox);
			this.masterBox.firstWordBox = ((WordBox) this.wordBoxesByID.get(response.getProperty("firstWbId")));
			ArrayList boxList = new ArrayList();
			for (int b = 0; b < boxDataList.size(); b++) {
				BoxData bd = ((BoxData) boxDataList.get(b));
				WordBox lWb = bd.fWb;
				while ((lWb.nextWordBox != null) && (lWb.nextWordBox != bd.lWbNext))
					lWb = lWb.nextWordBox;
				Box box = this.buildBoundingBox(bd.fWb, lWb);
				box.boxType = bd.type;
				boxList.add(box);
			}
			for (int b = 0; b < boxList.size(); b++) {
				Box box = ((Box) boxList.get(b));
				this.addRegionBox(box.left, box.top, box.width, box.height, box.boxType);
			}
		}
		private static class BoxData {
			WordBox fWb;
			WordBox lWbNext;
			String type;
			BoxData(WordBox fWb, String type) {
				this.fWb = fWb;
				this.type = type;
			}
		}
		
		//	makes HTML width of image + borders 700 px so it fits Facebook iframe, and makes HTML height of image + borders 1000 px so it fits common screens
		private static final int htmlNormWidth = 700;
		private static final int htmlNormHeight = 1000;
		private float getHtmlZoomFactor() {
			float wFactor = (((float) (htmlNormWidth - (2 * this.borderWidth))) / this.masterBox.width);
			float hFactor = (((float) (htmlNormHeight - (2 * this.borderWidth))) / this.masterBox.height);
			return Math.min(wFactor, hFactor);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeCssStyles(java.io.Writer)
		 */
		public void writeCssStyles(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine(".square {");
			blw.writeLine("  border: 2px solid #FF8800;");
			blw.writeLine("  position: absolute;");
			blw.writeLine("  font-size: 1px;");
			blw.writeLine("}");
			blw.writeLine(".squareMaster {");
			blw.writeLine("  border: 0px;");
			blw.writeLine("  position: absolute;");
			blw.writeLine("}");
			blw.writeLine(".squareDrawing {");
			blw.writeLine("  border: 1px solid #FF0000;");
			blw.writeLine("  position: absolute;");
			blw.writeLine("  font-size: 1px;");
			blw.writeLine("}");
			blw.writeLine(".squareFunction {");
			blw.writeLine("  font-family: Verdana;");
			blw.writeLine("  font-weight: bold;");
			blw.writeLine("  border: 0px;");
			blw.writeLine("  margin: 0px;");
			blw.writeLine("  padding: 0px;");
			blw.writeLine("  position: absolute;");
			blw.writeLine("  background-color: #FF8800;");
			blw.writeLine("  z-index: 10;");
			blw.writeLine("}");
			
			blw.writeLine(".wordBox {");
			blw.writeLine("  border: 1px solid #88FF88;");
			blw.writeLine("  position: absolute;");
			blw.writeLine("  z-index: 2;");
			blw.writeLine("  font-size: 1px;");
			blw.writeLine("}");
			
			blw.writeLine(".typeOption {");
			blw.writeLine("  position: absolute;");
			blw.writeLine("  z-index: 30;");
			blw.writeLine("  width: 100%;");
			blw.writeLine("  border: 2px solid #FFFFFF;");
			blw.writeLine("  font-family: Verdana;");
			blw.writeLine("  background-color: #FFFFFF;");
			blw.writeLine("}");
			
			blw.writeLine(".errorMessageBox {");
			blw.writeLine("  background-color: #FFEEEE;");
			blw.writeLine("  opacity: 0.4;");
			blw.writeLine("  filter: alpha(opacity = 40);");
			blw.writeLine("}");
			blw.writeLine(".errorMessage {");
			blw.writeLine("  border: 3px solid #FF0000;");
			blw.writeLine("  background-color: #FFFFFF;");
			blw.writeLine("  padding: 10px;");
			blw.writeLine("  font-family: Verdana;");
			blw.writeLine("  font-size: 10px;");
			blw.writeLine("  font-weight: bold;");
			blw.writeLine("  text-align: center;");
			blw.writeLine("  opacity: 1;");
			blw.writeLine("  filter: alpha(opacity = 100);");
			blw.writeLine("}");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			blw.writeLine("var wordBoxes = new Array();");
			blw.writeLine("var boxes = new Array();");
			
			blw.writeLine("function storeBox(box) {");
			blw.writeLine("  for (var b = 0; b < boxes.length; b++) {");
			blw.writeLine("    if ((boxes[b].parentBox == box.parentBox) && boxContains(box, boxes[b]))");
			blw.writeLine("      boxes[b].parentBox = box;");
			blw.writeLine("  }");
			blw.writeLine("  boxes.push(box);");
			blw.writeLine("  adjustZIndex();");
			blw.writeLine("  updateWordBoxSequence(masterBox);");
			blw.writeLine("}");
			
			blw.writeLine("function removeBox(box) {");
			blw.writeLine("  var removed = false;");
			blw.writeLine("  for (var b = 0; b < boxes.length; b++) {");
			blw.writeLine("    if (boxes[b].parentBox == box)");
			blw.writeLine("      boxes[b].parentBox = box.parentBox;");
			blw.writeLine("    if (boxes[b] == box) {");
			blw.writeLine("      boxes[b] = null;");
			blw.writeLine("      removed = true;");
			blw.writeLine("    }");
			blw.writeLine("    else if (removed) {");
			blw.writeLine("      boxes[b-1] = boxes[b];");
			blw.writeLine("      boxes[b] = null;");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  boxes.pop();");
			blw.writeLine("  adjustZIndex();");
			blw.writeLine("  updateWordBoxSequence(masterBox);");
			blw.writeLine("  updateTypeLayout(box.parentBox);");
			blw.writeLine("}");
			
			blw.writeLine("function buildInnerBox(box) {");
			blw.writeLine("  var minx = 10000;");
			blw.writeLine("  var miny = 10000;");
			blw.writeLine("  var maxx = -1;");
			blw.writeLine("  var maxy = -1;");
			blw.writeLine("  for (var b = 0; b < boxes.length; b++) {");
			blw.writeLine("    if (boxes[b].parentBox == box) {");
			blw.writeLine("      minx = Math.min(minx, boxes[b].x);");
			blw.writeLine("      miny = Math.min(miny, boxes[b].y);");
			blw.writeLine("      maxx = Math.max(maxx, (boxes[b].x + boxes[b].w));");
			blw.writeLine("      maxy = Math.max(maxy, (boxes[b].y + boxes[b].h));");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  if (minx > maxx)");
			blw.writeLine("    return null;");
			blw.writeLine("  var innerBox = new Object();");
			blw.writeLine("  innerBox.x = minx;");
			blw.writeLine("  innerBox.y = miny;");
			blw.writeLine("  innerBox.w = (maxx-minx);");
			blw.writeLine("  innerBox.h = (maxy-miny);");
			blw.writeLine("  return innerBox;");
			blw.writeLine("}");
			
			blw.writeLine("function getChildBoxes(box) {");
			blw.writeLine("  var childBoxes = new Array();");
			blw.writeLine("  for (var b = 0; b < boxes.length; b++) {");
			blw.writeLine("    if (boxes[b].parentBox == box)");
			blw.writeLine("      childBoxes.push(boxes[b]);");
			blw.writeLine("  }");
			blw.writeLine("  return childBoxes;");
			blw.writeLine("}");
			
			blw.writeLine("function getBoxAt(x, y) {");
			blw.writeLine("  var box = ((masterBox == null) ? canvas : masterBox);");
			blw.writeLine("  for (var b = 0; b < boxes.length; b++) {");
			blw.writeLine("    if ((boxes[b].parentBox == box) && (boxes[b].x < x) && (boxes[b].y < y) && ((boxes[b].x + boxes[b].w) > x) && ((boxes[b].y + boxes[b].h) > y))");
			blw.writeLine("      box = boxes[b];");
			blw.writeLine("  }");
			blw.writeLine("  return box;");
			blw.writeLine("}");
			
			blw.writeLine("function boxContains(outerBox, innerBox) {");
			blw.writeLine("  return ((outerBox.x <= innerBox.x) && (outerBox.y <= innerBox.y) && ((outerBox.x + outerBox.w) >= (innerBox.x + innerBox.w)) && ((outerBox.y + outerBox.h) >= (innerBox.y + innerBox.h)));");
			blw.writeLine("}");
			
			blw.writeLine("function boxOverlaps(box1, box2) {");
			blw.writeLine("  var il = Math.max(box1.x, box2.x);");
			blw.writeLine("  var it = Math.max(box1.y, box2.y);");
			blw.writeLine("  var ir = Math.min((box1.x + box1.w), (box2.x + box2.w));");
			blw.writeLine("  var ib = Math.min((box1.y + box1.h), (box2.y + box2.h));");
			blw.writeLine("  return ((il < ir) && (it < ib));");
			blw.writeLine("}");
			
			blw.writeLine("function adjustZIndex() {");
			blw.writeLine("  if (masterBox == null) {");
			blw.writeLine("    canvas.style.zIndex = 1;");
			blw.writeLine("    doAdjustZIndex(canvas, 1);");
			blw.writeLine("  }");
			blw.writeLine("  else {");
			blw.writeLine("    masterBox.style.zIndex = 2;");
			blw.writeLine("    doAdjustZIndex(masterBox, 2);");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("function doAdjustZIndex(box, zi) {");
			blw.writeLine("  for (var b = 0; b < boxes.length; b++) {");
			blw.writeLine("    if (boxes[b].parentBox == box) {");
			blw.writeLine("      boxes[b].style.zIndex = zi+1;");
			blw.writeLine("      doAdjustZIndex(boxes[b], (zi+1));");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("var feedbackForm;");
			blw.writeLine("var canvas;");
			blw.writeLine("var masterBox;");
			
			blw.writeLine("var drawing;");
			blw.writeLine("var drawingParent;");
			
			blw.writeLine("var resizingLt;");
			blw.writeLine("var resizingLtInner;");
			blw.writeLine("var resizingLtSiblings;");
			
			blw.writeLine("var resizingRb;");
			blw.writeLine("var resizingRbInner;");
			blw.writeLine("var resizingRbSiblings;");
			
			blw.writeLine("var removing;");
			
			blw.writeLine("var retyping;");
			
			blw.writeLine("function startPosition(x, y) {");
			blw.writeLine("  if (drawing != null)");
			blw.writeLine("    return;");
			blw.writeLine("  if (removing != null)");
			blw.writeLine("    return;");
			blw.writeLine("  if (resizingLt != null)");
			blw.writeLine("    return;");
			blw.writeLine("  if (resizingRb != null)");
			blw.writeLine("    return;");
			blw.writeLine("  if (retyping != null)");
			blw.writeLine("    return;");
			blw.writeLine("  if (eventConsumed)");
			blw.writeLine("    return;");
			blw.writeLine("  drawingParent = getBoxAt(x, y);");
			blw.writeLine("  var box = document.createElement('div');");
			blw.writeLine("  box.className= 'squareDrawing';");
			blw.writeLine("  box.style.zIndex = 11;");
			blw.writeLine("  box.x = x;");
			blw.writeLine("  box.dx = x;");
			blw.writeLine("  box.y = y;");
			blw.writeLine("  box.dy = y;");
			blw.writeLine("  box.w = 0;");
			blw.writeLine("  box.h = 0;");
			blw.writeLine("  box.style.left = x + 'px';");
			blw.writeLine("  box.style.top = y + 'px';");
			blw.writeLine("  box.onmousedown = function(event) {");
			blw.writeLine("    if (eventConsumed)");
			blw.writeLine("      return;");
			blw.writeLine("    var ex = getX(event);");
			blw.writeLine("    var ey = getY(event);");
			blw.writeLine("    if ((ex == -1) || (ey == -1))");
			blw.writeLine("      return;");
			blw.writeLine("    startPosition(ex, ey);");
			blw.writeLine("  };");
			blw.writeLine("  box.onmousemove = function(event) {");
			blw.writeLine("    var ex = getX(event);");
			blw.writeLine("    var ey = getY(event);");
			blw.writeLine("    if ((ex == -1) || (ey == -1))");
			blw.writeLine("      return;");
			blw.writeLine("    updatePosition(ex, ey);");
			blw.writeLine("  };");
			blw.writeLine("  box.onmouseup = function(event) {");
			blw.writeLine("    var ex = getX(event);");
			blw.writeLine("    var ey = getY(event);");
			blw.writeLine("    if ((ex == -1) || (ey == -1))");
			blw.writeLine("      return;");
			blw.writeLine("    endPosition(ex, ey);");
			blw.writeLine("  };");
			blw.writeLine("  box.onmouseover = function() {");
			blw.writeLine("    if (drawing != null)");
			blw.writeLine("      return;");
			blw.writeLine("    if (retyping != box)");
			blw.writeLine("      stopRetyping();");
			blw.writeLine("    showButtons(box);");
			blw.writeLine("  };");
			blw.writeLine("  box.onmouseout = function() {");
			blw.writeLine("    if (drawing != null)");
			blw.writeLine("      return;");
			blw.writeLine("    if (retyping != box)");
			blw.writeLine("      stopRetyping();");
			blw.writeLine("    hideButtons(box);");
			blw.writeLine("  };");
			blw.writeLine("  canvas.appendChild(box);");
			blw.writeLine("  box.parentBox = drawingParent;");
			blw.writeLine("  hideButtons(drawingParent);");
			blw.writeLine("  drawing = box;");
			blw.writeLine("}");
			
			blw.writeLine("function updatePosition(x, y) {");
			blw.writeLine("  if (drawing != null) {");
			blw.writeLine("    if ((x >= drawingParent.x) && (x <= (drawingParent.x + drawingParent.w))) {");
			blw.writeLine("      drawing.w = Math.abs(drawing.dx-x);");
			blw.writeLine("      drawing.x = (drawing.dx < x) ? drawing.dx : x;");
			blw.writeLine("      drawing.style.left = drawing.x + 'px';");
			blw.writeLine("      drawing.style.width = drawing.w + 'px';");
			blw.writeLine("    }");
			blw.writeLine("    if ((y >= drawingParent.y) && (y <= (drawingParent.y + drawingParent.h))) {");
			blw.writeLine("      drawing.h = Math.abs(drawing.dy-y);");
			blw.writeLine("      drawing.y = (drawing.dy < y) ? drawing.dy : y;");
			blw.writeLine("      drawing.style.top = drawing.y + 'px';");
			blw.writeLine("      drawing.style.height = drawing.h + 'px';");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  if (resizingLt != null) {");
			blw.writeLine("    var nx = (x-(buttonSize/2)+resizingLt.hbo-(borderOffset/2));");
			blw.writeLine("    var ny = (y-(buttonSize/2)+resizingLt.vbo-(borderOffset/2));");
			blw.writeLine("    var nw = (resizingLt.x + resizingLt.w) - nx;");
			blw.writeLine("    var nh = (resizingLt.y + resizingLt.h) - ny;");
			blw.writeLine("    if (resizingLtSiblings != null) {");
			blw.writeLine("      var movedBox = new Object();");
			blw.writeLine("      movedBox.x = nx;");
			blw.writeLine("      movedBox.y = ny;");
			blw.writeLine("      movedBox.w = nw;");
			blw.writeLine("      movedBox.h = nh;");
			blw.writeLine("      for (var s = 0; s < resizingLtSiblings.length; s ++) {");
			blw.writeLine("        if (resizingLtSiblings[s] == resizingLt)");
			blw.writeLine("          continue;");
			blw.writeLine("        if (boxOverlaps(movedBox, resizingLtSiblings[s]))");
			blw.writeLine("          return;");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    if ((nx >= resizingLt.parentBox.x) && ((nx + nw) <= (resizingLt.parentBox.x + resizingLt.parentBox.w)) && ((resizingLtInner == null) || ((nx <= resizingLtInner.x) && ((nx + nw) >= (resizingLtInner.x + resizingLtInner.w))))) {");
			blw.writeLine("      resizingLt.x = nx;");
			blw.writeLine("      resizingLt.w = nw;");
			blw.writeLine("      resizingLt.style.left = resizingLt.x + 'px';");
			blw.writeLine("      resizingLt.style.width = resizingLt.w + 'px';");
			blw.writeLine("      resizingLt.remove.style.left = (resizingLt.w - buttonSize + resizingLt.hbo - borderOffset) + 'px';");
			blw.writeLine("      resizingLt.resizeRb.style.left = (resizingLt.w - buttonSize + resizingLt.hbo - borderOffset) + 'px';");
			blw.writeLine("    }");
			blw.writeLine("    if ((ny >= resizingLt.parentBox.y) && ((ny + nh) <= (resizingLt.parentBox.y + resizingLt.parentBox.h)) && ((resizingLtInner == null) || ((ny <= resizingLtInner.y) && ((ny + nh) >= (resizingLtInner.y + resizingLtInner.h))))) {");
			blw.writeLine("      resizingLt.y = ny;");
			blw.writeLine("      resizingLt.h = nh;");
			blw.writeLine("      resizingLt.style.top = resizingLt.y + 'px';");
			blw.writeLine("      resizingLt.style.height = resizingLt.h + 'px';");
			blw.writeLine("      resizingLt.retype.style.top = (resizingLt.h - buttonSize + resizingLt.vbo - borderOffset) + 'px';");
			blw.writeLine("      resizingLt.resizeRb.style.top = (resizingLt.h - buttonSize + resizingLt.vbo - borderOffset) + 'px';");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  if (resizingRb != null) {");
			blw.writeLine("    var nw = (Math.abs(x-resizingRb.x)+(buttonSize/2)-resizingRb.hbo);");
			blw.writeLine("    var nh = (Math.abs(y-resizingRb.y)+(buttonSize/2)-resizingRb.vbo);");
			blw.writeLine("    if (resizingRbSiblings != null) {");
			blw.writeLine("      var resizedBox = new Object();");
			blw.writeLine("      resizedBox.x = resizingRb.x;");
			blw.writeLine("      resizedBox.y = resizingRb.y;");
			blw.writeLine("      resizedBox.w = nw;");
			blw.writeLine("      resizedBox.h = nh;");
			blw.writeLine("      for (var s = 0; s < resizingRbSiblings.length; s ++) {");
			blw.writeLine("        if (resizingRbSiblings[s] == resizingRb)");
			blw.writeLine("          continue;");
			blw.writeLine("        if (boxOverlaps(resizedBox, resizingRbSiblings[s]))");
			blw.writeLine("          return;");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    if ((resizingRb.x + nw) <= (resizingRb.parentBox.x + resizingRb.parentBox.w) && ((resizingRbInner == null) || ((resizingRb.x + nw) >= (resizingRbInner.x + resizingRbInner.w)))) {");
			blw.writeLine("      resizingRb.w = nw;");
			blw.writeLine("      resizingRb.style.width = resizingRb.w + 'px';");
			blw.writeLine("      resizingRb.remove.style.left = (resizingRb.w - buttonSize + resizingRb.hbo - borderOffset) + 'px';");
			blw.writeLine("      resizingRb.resizeRb.style.left = (resizingRb.w - buttonSize + resizingRb.hbo - borderOffset) + 'px';");
			blw.writeLine("    }");
			blw.writeLine("    if ((resizingRb.y + nh) <= (resizingRb.parentBox.y + resizingRb.parentBox.h) && ((resizingRbInner == null) || ((resizingRb.y + nh) >= (resizingRbInner.y + resizingRbInner.h)))) {");
			blw.writeLine("      resizingRb.h = nh;");
			blw.writeLine("      resizingRb.style.height = resizingRb.h + 'px';");
			blw.writeLine("      resizingRb.retype.style.top = (resizingRb.h - buttonSize + resizingRb.vbo - borderOffset) + 'px';");
			blw.writeLine("      resizingRb.resizeRb.style.top = (resizingRb.h - buttonSize + resizingRb.vbo - borderOffset) + 'px';");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("function endPosition(x, y) {");
			blw.writeLine("  if (stopResizingLt() || stopResizingRb())");
			blw.writeLine("    return;");
			blw.writeLine("  if (drawing == null)");
			blw.writeLine("    return;");
			blw.writeLine("  snapToWordBoxes(drawing);");
			blw.writeLine("  var noWord = (wordBoxes.length > 0);");
			blw.writeLine("  for (var w = 0; noWord && (w < wordBoxes.length); w++) {");
			blw.writeLine("    if (boxContains(drawing, wordBoxes[w])) {");
			blw.writeLine("      noWord = false;");
			blw.writeLine("      continue;");
			blw.writeLine("    }");
			blw.writeLine("    var il = Math.max(drawing.x, wordBoxes[w].x);");
			blw.writeLine("    var ir = Math.min((drawing.x + drawing.w), (wordBoxes[w].x + wordBoxes[w].w));");
			blw.writeLine("    var it = Math.max(drawing.y, wordBoxes[w].y);");
			blw.writeLine("    var ib = Math.min((drawing.y + drawing.h), (wordBoxes[w].y + wordBoxes[w].h));");
			blw.writeLine("    if ((il >= ir) || (it >= ib))");
			blw.writeLine("      continue;");
			blw.writeLine("    var wordInBox = (((ir - il) * (ib - it)) / (wordBoxes[w].w * wordBoxes[w].h));");
			blw.writeLine("    if (wordInBox > 0.5)");
			blw.writeLine("      noWord = false;");
			blw.writeLine("  }");
			blw.writeLine("  if (noWord) {");
			blw.writeLine("    canvas.removeChild(drawing);");
			blw.writeLine("    drawing = null;");
			blw.writeLine("    return;");
			blw.writeLine("  }");
			blw.writeLine("  var drawingSiblings = getChildBoxes(drawing.parentBox);");
			blw.writeLine("  var resized = false;");
			blw.writeLine("  var extending = false;");
			blw.writeLine("  do {");
			blw.writeLine("    resized = false;");
			blw.writeLine("    var lx = drawing.x;");
			blw.writeLine("    var ly = drawing.y;");
			blw.writeLine("    var lw = drawing.w;");
			blw.writeLine("    var lh = drawing.h;");
			blw.writeLine("    for (var s = 0; s < drawingSiblings.length; s++) {");
			blw.writeLine("      var il = Math.max(drawing.x, drawingSiblings[s].x);");
			blw.writeLine("      var it = Math.max(drawing.y, drawingSiblings[s].y);");
			blw.writeLine("      var ir = Math.min((drawing.x + drawing.w), (drawingSiblings[s].x + drawingSiblings[s].w));");
			blw.writeLine("      var ib = Math.min((drawing.y + drawing.h), (drawingSiblings[s].y + drawingSiblings[s].h));");
			blw.writeLine("      ");
			blw.writeLine("      if ((il >= ir) || (it >= ib) || boxContains(drawing, drawingSiblings[s]) || boxContains(drawingSiblings[s], drawing))");
			blw.writeLine("        continue;");
			blw.writeLine("      resized = true;");
			blw.writeLine("      var drawingInSibling = (((ir - il) * (ib - it)) / (drawing.w * drawing.h));");
			blw.writeLine("      var siblingInDrawing = (((ir - il) * (ib - it)) / (drawingSiblings[s].w * drawingSiblings[s].h));");
			blw.writeLine("      if (extending || (siblingInDrawing > 0.5)) {");
			blw.writeLine("        var jl = Math.min(drawing.x, drawingSiblings[s].x);");
			blw.writeLine("        var jt = Math.min(drawing.y, drawingSiblings[s].y);");
			blw.writeLine("        var jr = Math.max((drawing.x + drawing.w), (drawingSiblings[s].x + drawingSiblings[s].w));");
			blw.writeLine("        var jb = Math.max((drawing.y + drawing.h), (drawingSiblings[s].y + drawingSiblings[s].h));");
			blw.writeLine("        drawing.x = jl;");
			blw.writeLine("        drawing.y = jt;");
			blw.writeLine("        drawing.w = (jr-jl);");
			blw.writeLine("        drawing.h = (jb-jt);");
			blw.writeLine("        drawing.style.left = drawing.x + 'px';");
			blw.writeLine("        drawing.style.width = drawing.w + 'px';");
			blw.writeLine("        drawing.style.top = drawing.y + 'px';");
			blw.writeLine("        drawing.style.height = drawing.h + 'px';");
			blw.writeLine("        drawingSiblings[s].parentBox = drawing;");
			blw.writeLine("        drawingSiblings = getChildBoxes(drawing.parentBox);");
			blw.writeLine("        s = drawingSiblings.length;");
			blw.writeLine("        extending = true;");
			blw.writeLine("      }");
			blw.writeLine("      else if (drawingInSibling > 0.5){");
			blw.writeLine("        drawing.x = il;");
			blw.writeLine("        drawing.y = it;");
			blw.writeLine("        drawing.w = (ir-il);");
			blw.writeLine("        drawing.h = (ib-it);");
			blw.writeLine("        drawing.style.left = drawing.x + 'px';");
			blw.writeLine("        drawing.style.width = drawing.w + 'px';");
			blw.writeLine("        drawing.style.top = drawing.y + 'px';");
			blw.writeLine("        drawing.style.height = drawing.h + 'px';");
			blw.writeLine("        drawing.parentBox = drawingSiblings[s];");
			blw.writeLine("        drawingSiblings = getChildBoxes(drawing.parentBox);");
			blw.writeLine("        s = drawingSiblings.length;");
			blw.writeLine("      }");
			blw.writeLine("      else {");
			blw.writeLine("        var cuts = new Array();");
			blw.writeLine("        if (il > drawing.x) {");
			blw.writeLine("          var cut = new Object();");
			blw.writeLine("          cut.x = drawing.x;");
			blw.writeLine("          cut.y = drawing.y;");
			blw.writeLine("          cut.w = (il - drawing.x - (borderOffset/2));");
			blw.writeLine("          cut.h = drawing.h;");
			blw.writeLine("          cut.inside = false;");
			blw.writeLine("          cuts.push(cut);");
			blw.writeLine("        }");
			blw.writeLine("        if (ir < (drawing.x + drawing.w)) {");
			blw.writeLine("          var cut = new Object();");
			blw.writeLine("          cut.x = ir+(borderOffset/2);");
			blw.writeLine("          cut.y = drawing.y;");
			blw.writeLine("          cut.w = (drawing.w - (ir - drawing.x) - (borderOffset/2));");
			blw.writeLine("          cut.h = drawing.h;");
			blw.writeLine("          cut.inside = false;");
			blw.writeLine("          cuts.push(cut);");
			blw.writeLine("        }");
			blw.writeLine("        if (it > drawing.y) {");
			blw.writeLine("          var cut = new Object();");
			blw.writeLine("          cut.x = drawing.x;");
			blw.writeLine("          cut.y = drawing.y;");
			blw.writeLine("          cut.w = drawing.w;");
			blw.writeLine("          cut.h = (it - drawing.y - (borderOffset/2));");
			blw.writeLine("          cut.inside = false;");
			blw.writeLine("          cuts.push(cut);");
			blw.writeLine("        }");
			blw.writeLine("        if (ib < (drawing.y + drawing.h)) {");
			blw.writeLine("          var cut = new Object();");
			blw.writeLine("          cut.x = drawing.x;");
			blw.writeLine("          cut.y = ib + (borderOffset/2);");
			blw.writeLine("          cut.w = drawing.w;");
			blw.writeLine("          cut.h = (drawing.h - (ib - drawing.y) - (borderOffset/2));");
			blw.writeLine("          cut.inside = false;");
			blw.writeLine("          cuts.push(cut);");
			blw.writeLine("        }");
			blw.writeLine("        var cut = new Object();");
			blw.writeLine("        cut.x = il;");
			blw.writeLine("        cut.y = it;");
			blw.writeLine("        cut.w = (ir - il);");
			blw.writeLine("        cut.h = (ib - it);");
			blw.writeLine("        cut.inside = true;");
			blw.writeLine("        cuts.push(cut);");
			blw.writeLine("        var maxCutSize = 0;");
			blw.writeLine("        for (var c = 0; c < cuts.length; c++) {");
			blw.writeLine("          var cutSize = (cuts[c].w * cuts[c].h);");
			blw.writeLine("          if (cutSize > maxCutSize) {");
			blw.writeLine("            cut = cuts[c];");
			blw.writeLine("            maxCutSize = cutSize;");
			blw.writeLine("          }");
			blw.writeLine("        }");
			blw.writeLine("        drawing.x = cut.x;");
			blw.writeLine("        drawing.y = cut.y;");
			blw.writeLine("        drawing.w = cut.w;");
			blw.writeLine("        drawing.h = cut.h;");
			blw.writeLine("        drawing.style.left = drawing.x + 'px';");
			blw.writeLine("        drawing.style.width = drawing.w + 'px';");
			blw.writeLine("        drawing.style.top = drawing.y + 'px';");
			blw.writeLine("        drawing.style.height = drawing.h + 'px';");
			blw.writeLine("        if (cut.inside) {");
			blw.writeLine("          drawing.parentBox = drawingSiblings[s];");
			blw.writeLine("          drawingSiblings = getChildBoxes(drawing.parentBox);");
			blw.writeLine("          s = drawingSiblings.length;");
			blw.writeLine("          extending = false;");
			blw.writeLine("        }");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    if (resized && (lx == drawing.x) && (ly == drawing.y) && (lw == drawing.w) && (lh == drawing.h))");
			blw.writeLine("      resized = false;");
			blw.writeLine("  } while(resized);");
			blw.writeLine("  snapToWordBoxes(drawing);");
			blw.writeLine("  drawing.className = 'square';");
			blw.writeLine("  if (drawing.parentBox.zIndex)");
			blw.writeLine("    drawing.zIndex = drawing.parentBox.zIndex+1;");
			blw.writeLine("  drawing.style.zIndex = drawing.parentBox.style.zIndex+1;");
			blw.writeLine("  storeBox(drawing);");
			blw.writeLine("  addButtons(drawing);");
			blw.writeLine("  var boxType = '" + MAIN_TEXT_TYPE + "';");
			blw.writeLine("  if ((drawing.parentBox != masterBox) && (drawing.parentBox.boxType == '" + MAIN_TEXT_TYPE + "'))");
			blw.writeLine("    boxType = '" + CAPTION_TYPE + "';");
			blw.writeLine("  else {");
			blw.writeLine("    var boxChildren = getChildBoxes(drawing);");
			blw.writeLine("    var mainTextChildren = 0;");
			blw.writeLine("    var childBoxType = '" + MAIN_TEXT_TYPE + "';");
			blw.writeLine("    for (var c = 0; c < boxChildren.length; c++) {");
			blw.writeLine("      var cBox = boxChildren[c];");
			blw.writeLine("      if (cBox.boxType == null)");
			blw.writeLine("        continue;");
			blw.writeLine("      else if (cBox.boxType == '" + MAIN_TEXT_TYPE + "')");
			blw.writeLine("        mainTextChildren++;");
			blw.writeLine("      else childBoxType = cBox.boxType;");
			blw.writeLine("    }");
			blw.writeLine("    boxType = ((mainTextChildren > 1) ? '" + COLUMN_TYPE + "' : childBoxType);");
			blw.writeLine("  }");
			blw.writeLine("  drawing.boxType = boxType;");
			blw.writeLine("  updateTypeLayout(drawing);");
			blw.writeLine("  layoutButtons(drawing.parentBox);");
			blw.writeLine("  if (masterBox != null) {");
			blw.writeLine("    updateWordBoxSequence(masterBox);");
			blw.writeLine("    updateTypeLayout(drawing.parentBox);");
			blw.writeLine("  }");
			blw.writeLine("  drawing = null;");
			blw.writeLine("}");
			
			blw.writeLine("var buttonSize = " + this.buttonSize + ";");
			blw.writeLine("function addButtons(box) {");
			blw.writeLine("  var resizeLt = document.createElement('input');");
			blw.writeLine("  resizeLt.className = 'squareFunction';");
			blw.writeLine("  resizeLt.type = 'button';");
			blw.writeLine("  resizeLt.value = 'R';");
			blw.writeLine("  resizeLt.style.fontSize = ((buttonSize-6) + 'px');");
			blw.writeLine("  resizeLt.onmousedown = function() {");
			blw.writeLine("    stopRetyping();");
			blw.writeLine("    if (resizingLt != box)");
			blw.writeLine("      stopResizingLt();");
			blw.writeLine("    stopResizingRb();");
			blw.writeLine("    resizingLt = box;");
			blw.writeLine("    resizingLtInner = buildInnerBox(box);");
			blw.writeLine("    resizingLtSiblings = getChildBoxes(resizingLt.parentBox);");
			blw.writeLine("    resizeLt.style.zIndex = 20;");
			blw.writeLine("  };");
			blw.writeLine("  resizeLt.onmouseup = function() {");
			blw.writeLine("    stopResizingLt();");
			blw.writeLine("  };");
			blw.writeLine("  resizeLt.onmouseout = function() {};");
			blw.writeLine("  resizeLt.onmouseover = function() {");
			blw.writeLine("    stopRetyping();");
			blw.writeLine("  };");
			blw.writeLine("  box.resizeLt = resizeLt;");
			blw.writeLine("  box.appendChild(resizeLt);");
			blw.writeLine("  var remove = document.createElement('input');");
			blw.writeLine("  remove.className = 'squareFunction';");
			blw.writeLine("  remove.type = 'button';");
			blw.writeLine("  remove.value = 'X';");
			blw.writeLine("  remove.style.fontSize = ((buttonSize-6) + 'px');");
			blw.writeLine("  box.removing = false;");
			blw.writeLine("  remove.onmousedown = function() {");
			blw.writeLine("    stopRetyping();");
			blw.writeLine("    stopResizingLt();");
			blw.writeLine("    stopResizingRb();");
			blw.writeLine("    box.removing = true;");
			blw.writeLine("    removing = box;");
			blw.writeLine("  };");
			blw.writeLine("  remove.onmouseup = function() {");
			blw.writeLine("    if (box.removing) {");
			blw.writeLine("      removeBox(box);");
			blw.writeLine("      layoutButtons(box.parentBox);");
			blw.writeLine("      canvas.removeChild(box);");
			blw.writeLine("    }");
			blw.writeLine("    removing = null;");
			blw.writeLine("  };");
			blw.writeLine("  remove.onmouseout = function() {");
			blw.writeLine("    removing = null;");
			blw.writeLine("    box.removing = false;");
			blw.writeLine("  };");
			blw.writeLine("  remove.onmouseover = function() {");
			blw.writeLine("    stopRetyping();");
			blw.writeLine("  };");
			blw.writeLine("  box.remove = remove;");
			blw.writeLine("  box.appendChild(remove);");
			blw.writeLine("  var resizeRb = document.createElement('input');");
			blw.writeLine("  resizeRb.className = 'squareFunction';");
			blw.writeLine("  resizeRb.type = 'button';");
			blw.writeLine("  resizeRb.value = 'R';");
			blw.writeLine("  resizeRb.style.fontSize = ((buttonSize-6) + 'px');");
			blw.writeLine("  resizeRb.onmousedown = function() {");
			blw.writeLine("    stopRetyping();");
			blw.writeLine("    stopResizingLt();");
			blw.writeLine("    if (resizingRb != box)");
			blw.writeLine("      stopResizingRb();");
			blw.writeLine("    resizingRb = box;");
			blw.writeLine("    resizingRbInner = buildInnerBox(box);");
			blw.writeLine("    resizingRbSiblings = getChildBoxes(resizingRb.parentBox);");
			blw.writeLine("    resizeRb.style.zIndex = 20;");
			blw.writeLine("  };");
			blw.writeLine("  resizeRb.onmouseup = function() {");
			blw.writeLine("    stopResizingRb();");
			blw.writeLine("  };");
			blw.writeLine("  resizeRb.onmouseout = function() {};");
			blw.writeLine("  resizeRb.onmouseover = function() {");
			blw.writeLine("    stopRetyping();");
			blw.writeLine("  };");
			blw.writeLine("  box.resizeRb = resizeRb;");
			blw.writeLine("  box.appendChild(resizeRb);");
			blw.writeLine("  var retype = document.createElement('input');");
			blw.writeLine("  retype.className = 'squareFunction';");
			blw.writeLine("  retype.type = 'button';");
			blw.writeLine("  retype.value = 'T';");
			blw.writeLine("  retype.style.fontSize = ((buttonSize-6) + 'px');");
			blw.writeLine("  retype.onmousedown = function() {");
			blw.writeLine("    if (retyping == null)");
			blw.writeLine("      startRetyping(box);");
			blw.writeLine("    else stopRetyping();");
			blw.writeLine("    stopResizingLt();");
			blw.writeLine("    stopResizingRb();");
			blw.writeLine("    eventConsumed = true;");
			blw.writeLine("  };");
			blw.writeLine("  retype.onmouseup = function() {};");
			blw.writeLine("  retype.onmouseout = function() {};");
			blw.writeLine("  box.retype = retype;");
			blw.writeLine("  box.appendChild(retype);");
			blw.writeLine("  layoutButtons(box);");
			blw.writeLine("}");
			blw.writeLine("function layoutButtons(box) {");
			blw.writeLine("  if ((box == null) || (box == masterBox) || (box == canvas))");
			blw.writeLine("    return;");
			blw.writeLine("  box.hbo = (box.w < (2 * buttonSize)) ? (buttonSize-(box.w/2)) : 0;");
			blw.writeLine("  box.vbo = (box.h < (2 * buttonSize)) ? (buttonSize-(box.h/2)) : 0;");
			blw.writeLine("  var innerBox = buildInnerBox(box);");
			blw.writeLine("  if (innerBox != null) {");
			blw.writeLine("    var lg = (innerBox.x - box.x);");
			blw.writeLine("    var rg = ((box.x + box.w) - (innerBox.x + innerBox.w));");
			blw.writeLine("    if ((lg < buttonSize) || (rg < buttonSize))");
			blw.writeLine("      box.hbo = Math.max(box.hbo, (buttonSize - 3 - Math.min(lg, rg)));");
			blw.writeLine("    var tg = (innerBox.y - box.y);");
			blw.writeLine("    var bg = ((box.y + box.h) - (innerBox.y + innerBox.h));");
			blw.writeLine("    if ((tg < buttonSize) || (bg < buttonSize))");
			blw.writeLine("      box.vbo = Math.max(box.vbo, (buttonSize - 3 - Math.min(tg, bg)));");
			blw.writeLine("  }");
			blw.writeLine("  box.resizeLt.style.width = buttonSize + 'px';");
			blw.writeLine("  box.resizeLt.style.height = buttonSize + 'px';");
			blw.writeLine("  box.resizeLt.style.left = (box.hbo == 0) ? 0 + 'px' : -box.hbo + 'px';");
			blw.writeLine("  box.resizeLt.style.top = (box.vbo == 0) ? 0 + 'px' : -box.vbo + 'px';");
			blw.writeLine("  box.remove.style.width = buttonSize + 'px';");
			blw.writeLine("  box.remove.style.height = buttonSize + 'px';");
			blw.writeLine("  box.remove.style.left = (box.w - buttonSize + box.hbo - borderOffset) + 'px';");
			blw.writeLine("  box.remove.style.top = (box.vbo == 0) ? 0 + 'px' : -box.vbo + 'px';");
			blw.writeLine("  box.resizeRb.style.width = buttonSize + 'px';");
			blw.writeLine("  box.resizeRb.style.height = buttonSize + 'px';");
			blw.writeLine("  box.resizeRb.style.left = (box.w - buttonSize + box.hbo - borderOffset) + 'px';");
			blw.writeLine("  box.resizeRb.style.top = (box.h - buttonSize + box.vbo - borderOffset) + 'px';");
			blw.writeLine("  box.retype.style.width = buttonSize + 'px';");
			blw.writeLine("  box.retype.style.height = buttonSize + 'px';");
			blw.writeLine("  box.retype.style.left = (box.hbo == 0) ? 0 + 'px' : -box.hbo + 'px';");
			blw.writeLine("  box.retype.style.top = (box.h - buttonSize + box.vbo - borderOffset) + 'px';");
			blw.writeLine("}");
			blw.writeLine("function showButtons(box) {");
			blw.writeLine("  if ((box == null) || (box == masterBox) || (box == canvas))");
			blw.writeLine("    return;");
			blw.writeLine("  box.resizeLt.style.display = '';");
			blw.writeLine("  box.remove.style.display = '';");
			blw.writeLine("  box.resizeRb.style.display = '';");
			blw.writeLine("  box.retype.style.display = '';");
			blw.writeLine("  liftZIndex(box, 10);");
			blw.writeLine("}");
			blw.writeLine("function hideButtons(box) {");
			blw.writeLine("  if ((box == null) || (box == masterBox) || (box == canvas))");
			blw.writeLine("    return;");
			blw.writeLine("  if (box == retyping)");
			blw.writeLine("    return;");
			blw.writeLine("  if (box.zIndex)");
			blw.writeLine("    unliftZIndex(box);");
			blw.writeLine("  box.resizeLt.style.display = 'none';");
			blw.writeLine("  box.remove.style.display = 'none';");
			blw.writeLine("  box.resizeRb.style.display = 'none';");
			blw.writeLine("  box.retype.style.display = 'none';");
			blw.writeLine("}");
			
			blw.writeLine("function liftZIndex(box, zi) {");
			blw.writeLine("  box.zIndex = box.style.zIndex;");
			blw.writeLine("  box.style.zIndex = zi;");
			blw.writeLine("  for (var b = 0; b < boxes.length; b++) {");
			blw.writeLine("    if (boxes[b].parentBox == box)");
			blw.writeLine("      liftZIndex(boxes[b], (zi+1));");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("function unliftZIndex(box) {");
			blw.writeLine("  if (box.zIndex)");
			blw.writeLine("    box.style.zIndex = box.zIndex;");
			blw.writeLine("  for (var b = 0; b < boxes.length; b++) {");
			blw.writeLine("    if (boxes[b].parentBox == box)");
			blw.writeLine("      unliftZIndex(boxes[b]);");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("var eventConsumed = false;");
			blw.writeLine("var borderOffset = 0;"); // IE renders borders differently, so we need this variable to make amends
			blw.writeLine("var mysteriousEventOffset = 4;"); // no clue what this is due to, but we seem to need it, across all browsers ...
			
			blw.writeLine("function getX(e) {");
			blw.writeLine("  var ev = (!e) ? window.event : e; // Mozilla : IE");
			blw.writeLine("  if (ev.pageX) { //Mozilla etc.");
			blw.writeLine("    return ev.pageX - canvas.x - mysteriousEventOffset;");
			blw.writeLine("  }");
			blw.writeLine("  else if (ev.clientX) { // IE");
			blw.writeLine("    return ev.clientX + document.body.scrollLeft - canvas.x - mysteriousEventOffset;");
			blw.writeLine("  }");
			blw.writeLine("  else return -1; // old browsers");
			blw.writeLine("}");
			blw.writeLine("function getY(e) {");
			blw.writeLine("  var ev = (!e) ? window.event : e; // Mozilla : IE");
			blw.writeLine("  if (ev.pageY) { // Mozilla etc.");
			blw.writeLine("    return ev.pageY - canvas.y - mysteriousEventOffset;");
			blw.writeLine("  }");
			blw.writeLine("  else if (ev.clientY) { // IE");
			blw.writeLine("    return ev.clientY + document.body.scrollTop - canvas.y - mysteriousEventOffset;");
			blw.writeLine("  }");
			blw.writeLine("  else return -1; // old browsers");
			blw.writeLine("}");
			blw.writeLine("function cutPx(px) {");
			blw.writeLine("  if (px.length < 2)");
			blw.writeLine("    return Math.round(px);");
			blw.writeLine("  if (px.indexOf('px') == -1)");
			blw.writeLine("    return Math.round(px);");
			blw.writeLine("  return Math.round(px.substring(0, (px.length - 2)));");
			blw.writeLine("}");
			
			blw.writeLine("function stopResizingLt() {");
			blw.writeLine("  if (resizingLt == null)");
			blw.writeLine("    return false;");
			blw.writeLine("  snapToWordBoxes(resizingLt);");
			blw.writeLine("  updateWordBoxSequence(masterBox);");
			blw.writeLine("  layoutButtons(resizingLt);");
			blw.writeLine("  resizingLt.resizeLt.style.zIndex = 12;");
			blw.writeLine("  resizingLt = null;");
			blw.writeLine("  resizingLtInner = null;");
			blw.writeLine("  resizingLtSiblings = null;");
			blw.writeLine("  return true;");
			blw.writeLine("}");
			blw.writeLine("function stopResizingRb() {");
			blw.writeLine("  if (resizingRb == null)");
			blw.writeLine("    return false;");
			blw.writeLine("  snapToWordBoxes(resizingRb);");
			blw.writeLine("  updateWordBoxSequence(masterBox);");
			blw.writeLine("  layoutButtons(resizingRb);");
			blw.writeLine("  resizingRb.resizeRb.style.zIndex = 12;");
			blw.writeLine("  resizingRb = null;");
			blw.writeLine("  resizingRbInner = null;");
			blw.writeLine("  resizingRbSiblings = null;");
			blw.writeLine("  return true;");
			blw.writeLine("}");
			
			blw.writeLine("var typeSelector;");
			
			blw.writeLine("function startRetyping(box) {");
			blw.writeLine("  if (retyping != null) {");
			blw.writeLine("    retyping.style.zIndex = retypingZIndex;");
			blw.writeLine("    retyping.removeChild(typeSelector);");
			blw.writeLine("    retyping = null;");
			blw.writeLine("  }");
			blw.writeLine("  retyping = box;");
			blw.writeLine("  canvas.appendChild(typeSelector);");
			blw.writeLine("  typeSelector.style.left = ((retyping.x + buttonSize - retyping.hbo) + 'px');");
			blw.writeLine("  var sto = document.getElementById(retyping.boxType);");
			blw.writeLine("  sto.style.fontWeight = 'bold';");
			blw.writeLine("  var tst = (retyping.y + retyping.h - buttonSize + retyping.vbo - borderOffset - cutPx(sto.style.top));");
			blw.writeLine("  var tsh = cutPx(typeSelector.style.height);");
			blw.writeLine("  if (tst < 0)");
			blw.writeLine("    tst = 0;");
			blw.writeLine("  else if ((tst + tsh) > canvas.h)");
			blw.writeLine("    tst = (canvas.h - tsh);");
			blw.writeLine("  typeSelector.style.top = (tst + 'px');");
			blw.writeLine("  typeSelector.style.zIndex = 30;");
			blw.writeLine("  for (var c = 0; c < typeSelector.childNodes.length; c++) {");
			blw.writeLine("    var typeOption = typeSelector.childNodes[c];");
			blw.writeLine("    if (borderColors[typeOption.id])");
			blw.writeLine("      typeOption.style.zIndex = 30;");
			blw.writeLine("  }");
			blw.writeLine("  typeSelector.style.display = '';");
			blw.writeLine("}");
			
			blw.writeLine("function stopRetyping() {");
			blw.writeLine("  typeSelector.style.display = 'none';");
			blw.writeLine("  if (retyping != null) {");
			blw.writeLine("    var wasRetyping = retyping;");
			blw.writeLine("    canvas.removeChild(typeSelector);");
			blw.writeLine("    updateWordBoxSequence(masterBox);");
			blw.writeLine("    retyping = null;");
			blw.writeLine("    hideButtons(wasRetyping);");
			blw.writeLine("    updateTypeLayout(wasRetyping.parentBox);");
			blw.writeLine("  }");
			blw.writeLine("  for (var c = 0; c < typeSelector.childNodes.length; c++) {");
			blw.writeLine("    var typeOption = typeSelector.childNodes[c];");
			blw.writeLine("    if (!typeOption.id)");
			blw.writeLine("      continue;");
			blw.writeLine("    var typeColor = borderColors[typeOption.id];");
			blw.writeLine("    if (!typeColor)");
			blw.writeLine("      continue;");
			blw.writeLine("    typeOption.style.fontWeight = '';");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("function updateTypeLayout(box) {");
			blw.writeLine("  if ((box == null) || (box == masterBox) || (box == canvas))");
			blw.writeLine("    return;");
			blw.writeLine("  var borderColor = borderColors[box.boxType];");
			blw.writeLine("  if (borderColor == null)");
			blw.writeLine("    borderColor = '00FF00';");
			blw.writeLine("  box.style.borderColor = borderColor;");
			blw.writeLine("  if (box.boxType == '" + COLUMN_TYPE + "' || box.isContainer)");
			blw.writeLine("    box.style.borderStyle = 'dotted';");
			blw.writeLine("  else box.style.borderStyle = 'solid';");
			blw.writeLine("  var buttonLabelColor = '#000000';");
			
			//	make button labels in dark boxes come out white
			float[] hsb = new float[3];
			StringVector darkBoxTypes = new StringVector();
			for (Iterator btit = this.boxTypeColors.keySet().iterator(); btit.hasNext();) {
				String boxType = ((String) btit.next());
				Color boxTypeColor = this.getTypeColor(boxType);
				if (Color.RGBtoHSB(boxTypeColor.getRed(), boxTypeColor.getGreen(), boxTypeColor.getBlue(), hsb)[2] < 0.4)
					darkBoxTypes.addElement(boxType);
			}
			if (darkBoxTypes.size() != 0) {
				blw.writeLine("  if ((box.boxType == '" + darkBoxTypes.concatStrings("') || (box.boxType == '") + "'))");
				blw.writeLine("    buttonLabelColor = '#FFFFFF';");
			}
			blw.writeLine("  box.resizeLt.style.backgroundColor = borderColor;");
			blw.writeLine("  box.resizeLt.style.color = buttonLabelColor;");
			blw.writeLine("  box.remove.style.backgroundColor = borderColor;");
			blw.writeLine("  box.remove.style.color = buttonLabelColor;");
			blw.writeLine("  box.resizeRb.style.backgroundColor = borderColor;");
			blw.writeLine("  box.resizeRb.style.color = buttonLabelColor;");
			blw.writeLine("  box.retype.style.backgroundColor = borderColor;");
			blw.writeLine("  box.retype.style.color = buttonLabelColor;");
			blw.writeLine("  updateWordBoxColor(box, wordBoxes);");
			blw.writeLine("}");
			
			blw.writeLine("function snapToWordBoxes(box) {");
			blw.writeLine("  if ((box == null) || (box == masterBox))");
			blw.writeLine("    return;");
			blw.writeLine("  var wbl = (box.x + box.w);");
			blw.writeLine("  var wbr = box.x;");
			blw.writeLine("  var wbt = (box.y + box.h);");
			blw.writeLine("  var wbb = box.y;");
			blw.writeLine("  for (var w = 0; w < wordBoxes.length; w++) {");
			blw.writeLine("    if (boxContains(box, wordBoxes[w])) {");
			blw.writeLine("      wbl = Math.min(wbl, wordBoxes[w].x);");
			blw.writeLine("      wbr = Math.max(wbr, (wordBoxes[w].x + wordBoxes[w].w));");
			blw.writeLine("      wbt = Math.min(wbt, wordBoxes[w].y);");
			blw.writeLine("      wbb = Math.max(wbb, (wordBoxes[w].y + wordBoxes[w].h));");
			blw.writeLine("      continue;");
			blw.writeLine("    }");
			blw.writeLine("    var il = Math.max(box.x, wordBoxes[w].x);");
			blw.writeLine("    var ir = Math.min((box.x + box.w), (wordBoxes[w].x + wordBoxes[w].w));");
			blw.writeLine("    var it = Math.max(box.y, wordBoxes[w].y);");
			blw.writeLine("    var ib = Math.min((box.y + box.h), (wordBoxes[w].y + wordBoxes[w].h));");
			blw.writeLine("    if ((il >= ir) || (it >= ib))");
			blw.writeLine("      continue;");
			blw.writeLine("    var wordInBox = (((ir - il) * (ib - it)) / (wordBoxes[w].w * wordBoxes[w].h));");
			blw.writeLine("    if (wordInBox > 0.5) {");
			blw.writeLine("      wbl = Math.min(wbl, wordBoxes[w].x);");
			blw.writeLine("      wbr = Math.max(wbr, (wordBoxes[w].x + wordBoxes[w].w));");
			blw.writeLine("      wbt = Math.min(wbt, wordBoxes[w].y);");
			blw.writeLine("      wbb = Math.max(wbb, (wordBoxes[w].y + wordBoxes[w].h));");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  var toBeChildren = new Array();");
			blw.writeLine("  for (var b = 0; b < boxes.length; b++) {");
			blw.writeLine("    if (boxes[b] == box)");
			blw.writeLine("      continue;");
			blw.writeLine("    if (boxContains(box, boxes[b]))");
			blw.writeLine("      toBeChildren.push(boxes[b]);");
			blw.writeLine("  }");
			blw.writeLine("  if (toBeChildren.length != 0) {");
			blw.writeLine("    var ibl = 10000;");
			blw.writeLine("    var ibt = 10000;");
			blw.writeLine("    var ibr = -1;");
			blw.writeLine("    var ibb = -1;");
			blw.writeLine("    for (var b = 0; b < toBeChildren.length; b++) {");
			blw.writeLine("      ibl = Math.min(ibl, toBeChildren[b].x);");
			blw.writeLine("      ibt = Math.min(ibt, toBeChildren[b].y);");
			blw.writeLine("      ibr = Math.max(ibr, (toBeChildren[b].x + toBeChildren[b].w));");
			blw.writeLine("      ibb = Math.max(ibb, (toBeChildren[b].y + toBeChildren[b].h));");
			blw.writeLine("    }");
			blw.writeLine("    if ((ibl - wbl) < (buttonSize/2))");
			blw.writeLine("      wbl = Math.max(box.x, (ibl - (buttonSize/2)));");
			blw.writeLine("    if ((wbr - ibr) < (buttonSize/2))");
			blw.writeLine("      wbr = Math.min((box.x + box.w), (ibr + (buttonSize/2)));");
			blw.writeLine("    if ((ibt - wbt) < (buttonSize/2))");
			blw.writeLine("      wbt = Math.max(box.y, (ibt - (buttonSize/2)));");
			blw.writeLine("    if ((wbb - ibb) < (buttonSize/2))");
			blw.writeLine("      wbb = Math.min((box.y + box.h), (ibb + (buttonSize/2)));");
			blw.writeLine("  }");
			blw.writeLine("  if ((wbl < wbr) && (wbt < wbb)) {");
			blw.writeLine("    box.x = wbl;");
			blw.writeLine("    box.y = wbt;");
			blw.writeLine("    box.w = (wbr - wbl);");
			blw.writeLine("    box.h = (wbb - wbt);");
			blw.writeLine("    box.style.left = box.x + 'px';");
			blw.writeLine("    box.style.width = box.w + 'px';");
			blw.writeLine("    box.style.top = box.y + 'px';");
			blw.writeLine("    box.style.height = box.h + 'px';");
			blw.writeLine("    var update;");
			blw.writeLine("    do {");
			blw.writeLine("      update = false;");
			blw.writeLine("      for (var b = 0; b < boxes.length; b++) {");
			blw.writeLine("        if ((boxes[b] != box.parentBox) && (boxes[b] != box) && boxContains(box.parentBox, boxes[b]) && boxContains(boxes[b], box)) {");
			blw.writeLine("          box.parentBox = boxes[b];");
			blw.writeLine("          update = true;");
			blw.writeLine("          b = boxes.length;");
			blw.writeLine("        }");
			blw.writeLine("      }");
			blw.writeLine("    } while (update);");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("function updateWordBoxSequence(box) {");
			blw.writeLine("  if ((box == null) || (box == canvas))");
			blw.writeLine("    return;");
			blw.writeLine("  if ((box != masterBox) && (box.parentBox != masterBox)) {");
			blw.writeLine("    if (box != box.parentBox)");
			blw.writeLine("      updateWordBoxSequence(box.parentBox);");
			blw.writeLine("  }");
			blw.writeLine("  else {");
			blw.writeLine("    var descendingWordBoxes = new Array();");
			blw.writeLine("    for (var w = 0; w < wordBoxes.length; w++) {");
			blw.writeLine("      if (boxContains(box, wordBoxes[w])) {");
			blw.writeLine("        wordBoxes[w].parentBox = box;");
			blw.writeLine("        descendingWordBoxes.push(wordBoxes[w]);");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    if (descendingWordBoxes.length == 0)");
			blw.writeLine("      return;");
			blw.writeLine("    markNonChildWordBoxes(box, descendingWordBoxes);");
			blw.writeLine("    doUpdateWordBoxSequence(box, descendingWordBoxes);");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("function markNonChildWordBoxes(box, descendingWordBoxes) {");
			blw.writeLine("  box.isContainer = ((box == masterBox) || (box.boxType == '" + COLUMN_TYPE + "'));");
			blw.writeLine("  var childBoxes = getChildBoxes(box);");
			blw.writeLine("  if (childBoxes.length == 0)");
			blw.writeLine("    return;");
			blw.writeLine("  for (var c = 0; c < childBoxes.length; c++) {");
			blw.writeLine("    var childDescendingWordBoxes = new Array();");
			blw.writeLine("    for (var w = 0; w < descendingWordBoxes.length; w++) {");
			blw.writeLine("      if (boxContains(childBoxes[c], descendingWordBoxes[w])) {");
			blw.writeLine("        descendingWordBoxes[w].parentBox = childBoxes[c];");
			blw.writeLine("        childDescendingWordBoxes.push(descendingWordBoxes[w]);");
			blw.writeLine("      }");
			blw.writeLine("      box.isContainer = (box.isContainer || (childBoxes[c].boxType == '" + MAIN_TEXT_TYPE + "') || (childBoxes[c].boxType == '" + CONTINUE_MAIN_TEXT_TYPE + "'));");
			blw.writeLine("    }");
			blw.writeLine("    markNonChildWordBoxes(childBoxes[c], childDescendingWordBoxes);");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("var minLineWordBoxOverlap = " + this.minLineWordBoxOverlap + ";");
			blw.writeLine("var ascDescRatio = " + this.ascDescRatio + ";");
			blw.writeLine("function doUpdateWordBoxSequence(box, descendingWordBoxes) {");
			blw.writeLine("  var childBoxes = getChildBoxes(box);");
			blw.writeLine("  childBoxes.sort(function(wb1, wb2) {");
			blw.writeLine("    return (wb1.x - wb2.x);");
			blw.writeLine("  });");
			blw.writeLine("  var orderedChildBoxes = new Array();");
			blw.writeLine("  var remainingChildBoxes = childBoxes.length;");
			blw.writeLine("  while (remainingChildBoxes > 0) {");
			blw.writeLine("    var startC = -1;");
			blw.writeLine("    for (var c = 0; c < childBoxes.length; c++) {");
			blw.writeLine("      if (childBoxes[c] == null)");
			blw.writeLine("        continue;");
			blw.writeLine("      if ((startC == -1) || boxAbove(childBoxes[c], childBoxes[startC]))");
			blw.writeLine("        startC = c;");
			blw.writeLine("    }");
			blw.writeLine("    if (startC == -1)");
			blw.writeLine("      return;");
			blw.writeLine("    var lineTailBox = childBoxes[startC];");
			blw.writeLine("    orderedChildBoxes.push(lineTailBox)");
			blw.writeLine("    childBoxes[startC] = null;");
			blw.writeLine("    remainingChildBoxes--;");
			blw.writeLine("    var boxLineBounds = new Object();");
			blw.writeLine("    boxLineBounds.y = lineTailBox.y;");
			blw.writeLine("    boxLineBounds.h = lineTailBox.h;");
			blw.writeLine("    for (var c = 0; c < childBoxes.length; c++) {");
			blw.writeLine("      if (childBoxes[c] == null)");
			blw.writeLine("        continue;");
			blw.writeLine("      if (boxOnRightOf(childBoxes[c], lineTailBox) && !boxBelow(childBoxes[c], boxLineBounds)) {");
			blw.writeLine("        if (boxMutualLineOverlap(childBoxes[c], 0, 0, boxLineBounds, 0, 0) < minLineWordBoxOverlap)");
			blw.writeLine("          continue;");
			blw.writeLine("        lineTailBox = childBoxes[c];");
			blw.writeLine("        orderedChildBoxes.push(lineTailBox);");
			blw.writeLine("        boxLineBounds.y = Math.min(boxLineBounds.y, lineTailBox.y);");
			blw.writeLine("        boxLineBounds.h = Math.max((boxLineBounds.y + boxLineBounds.h), (lineTailBox.y + lineTailBox.h))-boxLineBounds.y;");
			blw.writeLine("        childBoxes[c] = null;");
			blw.writeLine("        remainingChildBoxes--;");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  childBoxes = orderedChildBoxes;");
			blw.writeLine("  for (var c = 0; c < childBoxes.length; c++) {");
			blw.writeLine("    doUpdateWordBoxSequence(childBoxes[c], descendingWordBoxes);");
			blw.writeLine("  }");
			blw.writeLine("  var childWordBoxes = new Array();");
			blw.writeLine("  for (var w = 0; w < descendingWordBoxes.length; w++) {");
			blw.writeLine("    if (descendingWordBoxes[w].parentBox == box)");
			blw.writeLine("      childWordBoxes.push(descendingWordBoxes[w]);");
			blw.writeLine("  }");
			blw.writeLine("  if (childWordBoxes.length == 0) {");
			blw.writeLine("    if (childBoxes.length != 0) {");
			blw.writeLine("      box.firstWordBox = childBoxes[0].firstWordBox;");
			blw.writeLine("      box.lastWordBox = childBoxes[childBoxes.length-1].lastWordBox;");
			blw.writeLine("      for (var c = 1; c < childBoxes.length; c++) {");
			blw.writeLine("        childBoxes[c-1].lastWordBox.nextWordBox = childBoxes[c].firstWordBox;");
			blw.writeLine("        childBoxes[c].firstWordBox.previousWordBox = childBoxes[c-1].lastWordBox;");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    return;");
			blw.writeLine("  }");
			blw.writeLine("  updateWordBoxColor(box, childWordBoxes);");
			blw.writeLine("  childWordBoxes.sort(function(wb1, wb2) {");
			blw.writeLine("    return (wb1.x - wb2.x);");
			blw.writeLine("  });");
			blw.writeLine("  var lineWordBoxes;");
			blw.writeLine("  var remainingWordBoxes = childWordBoxes.length;");
			blw.writeLine("  while (remainingWordBoxes > 0) {");
			blw.writeLine("    var lineStartW = -1;");
			blw.writeLine("    for (var w = 0; w < childWordBoxes.length; w++) {");
			blw.writeLine("      if (childWordBoxes[w] == null)");
			blw.writeLine("        continue;");
			blw.writeLine("      if ((lineStartW == -1) || boxAbove(childWordBoxes[w], childWordBoxes[lineStartW]))");
			blw.writeLine("        lineStartW = w;");
			blw.writeLine("    }");
			blw.writeLine("    if (lineStartW == -1) {");
			blw.writeLine("      return;");
			blw.writeLine("    }");
			blw.writeLine("    var lineTailWordBox = childWordBoxes[lineStartW];");
			blw.writeLine("    childWordBoxes[lineStartW] = null;");
			blw.writeLine("    remainingWordBoxes--;");
			blw.writeLine("    if (lineWordBoxes == null) {");
			blw.writeLine("      lineTailWordBox.boxType = box.boxType;");
			blw.writeLine("      lineTailWordBox.previousWordBox = null;");
			blw.writeLine("      box.firstWordBox = lineTailWordBox;");
			blw.writeLine("    }");
			blw.writeLine("    else {");
			blw.writeLine("      lineTailWordBox.boxType = null;");
			blw.writeLine("      lineWordBoxes[lineWordBoxes.length-1].nextWordBox = lineTailWordBox;");
			blw.writeLine("      lineTailWordBox.previousWordBox = lineWordBoxes[lineWordBoxes.length-1];");
			blw.writeLine("    }");
			blw.writeLine("    lineWordBoxes = new Array();");
			blw.writeLine("    lineWordBoxes.push(lineTailWordBox);");
			blw.writeLine("    var lineBounds = new Object();");
			blw.writeLine("    lineBounds.y = lineTailWordBox.y;");
			blw.writeLine("    lineBounds.h = lineTailWordBox.h;");
			blw.writeLine("    var lineHasAscent = lineTailWordBox.hasAsc;");
			blw.writeLine("    var lineHasDescent = lineTailWordBox.hasDesc;");
			blw.writeLine("    var lineFillRatio = 1.0 - (lineHasAscent ? 0 : ascDescRatio) - (lineHasDescent ? 0 : ascDescRatio);");
			blw.writeLine("    var lineHeigth = (lineBounds.h * (1.0 / lineFillRatio));");
			blw.writeLine("    var lineAscDesc = (lineHeigth * ascDescRatio);");
			blw.writeLine("    for (var w = 0; w < childWordBoxes.length; w++) {");
			blw.writeLine("      if (childWordBoxes[w] == null)");
			blw.writeLine("        continue;");
			blw.writeLine("      if (boxOnRightOf(childWordBoxes[w], lineTailWordBox) && !boxBelow(childWordBoxes[w], lineBounds)) {");
			blw.writeLine("        var lineOverlap = boxMutualLineOverlap(childWordBoxes[w], (childWordBoxes[w].hasAsc ? 0 : lineAscDesc), (childWordBoxes[w].hasDesc ? 0 : lineAscDesc), lineBounds, (lineHasAscent ? 0 : lineAscDesc), (lineHasDescent ? 0 : lineAscDesc));");
			blw.writeLine("        if (lineOverlap < minLineWordBoxOverlap)");
			blw.writeLine("          continue;");
			blw.writeLine("        lineTailWordBox.nextWordBox = childWordBoxes[w];");
			blw.writeLine("        childWordBoxes[w].previousWordBox = lineTailWordBox;");
			blw.writeLine("        lineTailWordBox = childWordBoxes[w];");
			blw.writeLine("        lineWordBoxes.push(lineTailWordBox);");
			blw.writeLine("        var lby = lineBounds.y;");
			blw.writeLine("        lineBounds.y = Math.min(lby, lineTailWordBox.y);");
			blw.writeLine("        lineBounds.h = Math.max((lby + lineBounds.h), (lineTailWordBox.y + lineTailWordBox.h))-lineBounds.y;");
			blw.writeLine("        lineHasAscent = (lineHasAscent || lineTailWordBox.hasAsc);");
			blw.writeLine("        lineHasDescent = (lineHasDescent || lineTailWordBox.hasDesc);");
			blw.writeLine("        lineFillRatio = 1.0 - (lineHasAscent ? 0 : ascDescRatio) - (lineHasDescent ? 0 : ascDescRatio);");
			blw.writeLine("        lineHeigth = (lineBounds.height * (1.0 / lineFillRatio));");
			blw.writeLine("        lineAscDesc = (lineHeigth * ascDescRatio);");
			blw.writeLine("        childWordBoxes[w].boxType = null;");
			blw.writeLine("        childWordBoxes[w] = null;");
			blw.writeLine("        remainingWordBoxes--;");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    lineTailWordBox.nextWordBox = null;");
			blw.writeLine("  }");
			blw.writeLine("  if (lineWordBoxes.length != 0)");
			blw.writeLine("    box.lastWordBox = lineWordBoxes[lineWordBoxes.length-1];");
			blw.writeLine("  if (childBoxes.length != 0) {");
			blw.writeLine("    box.lastWordBox.nextWordBox = childBoxes[0].firstWordBox;");
			blw.writeLine("    childBoxes[0].firstWordBox.previousWordBox = box.lastWordBox;");
			blw.writeLine("    box.lastWordBox = childBoxes[childBoxes.length-1].lastWordBox;");
			blw.writeLine("    for (var c = 1; c < childBoxes.length; c++) {");
			blw.writeLine("      childBoxes[c-1].lastWordBox.nextWordBox = childBoxes[c].firstWordBox;");
			blw.writeLine("      childBoxes[c].firstWordBox.previousWordBox = childBoxes[c-1].lastWordBox;");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("function boxMutualLineOverlap(box1, b1Asc, b1Desc, box2, b2Asc, b2Desc) {");
			blw.writeLine("  var b1t = (box1.top - b1Asc);");
			blw.writeLine("  var b1b = (box1.top + box1.height + b1Desc);");
			blw.writeLine("  var b2t = (box2.top - b2Asc);");
			blw.writeLine("  var b2b = (box2.top + box2.height + b2Desc);");
			blw.writeLine("  var ot = Math.max(b1t, b2t);");
			blw.writeLine("  var ob = Math.min(b1b, b2b);");
			blw.writeLine("  var overlap = (ob - ot);");
			blw.writeLine("  if (overlap <= 0) return 0;");
			blw.writeLine("  return (overlap / Math.min((b1b-b1t), (b2b-b2t)));");
			blw.writeLine("}");
			blw.writeLine("function boxOnLeftOf(leftBox, rightBox) {");
			blw.writeLine("  return ((leftBox.x + leftBox.w) <= rightBox.x);");
			blw.writeLine("}");
			blw.writeLine("function boxOnRightOf(rightBox, leftBox) {");
			blw.writeLine("  return (rightBox.x >= (leftBox.x + leftBox.w));");
			blw.writeLine("}");
			blw.writeLine("function boxAbove(upperBox, lowerBox) {");
			blw.writeLine("  return ((upperBox.y + upperBox.h) <= lowerBox.y);");
			blw.writeLine("}");
			blw.writeLine("function boxBelow(lowerBox, upperBox) {");
			blw.writeLine("  return (lowerBox.y >= (upperBox.y + upperBox.h));");
			blw.writeLine("}");
			
			blw.writeLine("function updateWordBoxColor(box, descendingWordBoxes) {");
			blw.writeLine("  for (var w = 0; w < descendingWordBoxes.length; w++) {");
			blw.writeLine("    if (descendingWordBoxes[w].parentBox == box) {");
			blw.writeLine("      if ((descendingWordBoxes[w].parentBox == masterBox) || descendingWordBoxes[w].parentBox.isContainer || (descendingWordBoxes[w].parentBox.boxType == '" + COLUMN_TYPE + "')) {");
			blw.writeLine("        descendingWordBoxes[w].style.borderColor = '#FF0000';");
			blw.writeLine("        descendingWordBoxes[w].style.borderWidth = '3px';");
			blw.writeLine("      }");
			blw.writeLine("      else {");
			blw.writeLine("        descendingWordBoxes[w].style.borderColor = box.style.borderColor;");
			blw.writeLine("        descendingWordBoxes[w].style.borderWidth = '1px';");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("function checkComplete() {");
			blw.writeLine("  updateWordBoxSequence(masterBox);");
			blw.writeLine("  var failWordBoxes = new Array();");
			blw.writeLine("  for (var w = 0; w < wordBoxes.length; w++) {");
			blw.writeLine("    if ((wordBoxes[w].parentBox == null) || wordBoxes[w].parentBox.isContainer || (wordBoxes[w].parentBox == masterBox) || (wordBoxes[w].parentBox.boxType == '" + COLUMN_TYPE + "'))");
			blw.writeLine("      failWordBoxes.push(wordBoxes[w]);");
			blw.writeLine("  }");
			blw.writeLine("  if (failWordBoxes.length == 0)");
			blw.writeLine("    return true;");
			blw.writeLine("  for (var w = 0; w < failWordBoxes.length; w++) {");
			blw.writeLine("    failWordBoxes[w].style.borderWidth = '3px';");
			blw.writeLine("    failWordBoxes[w].style.borderColor = '#FF0000';");
			blw.writeLine("  }");
			blw.writeLine("  showError();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("function writeFormParameters() {");
			blw.writeLine("  if (feedbackForm == null)");
			blw.writeLine("    return;");
			blw.writeLine("  var submit = true;");
			blw.writeLine("  var submitMode = document.getElementById('submitMode'); // TODO think of circumventing this feedback engine specific check, or simply generating parameters even is request was cancelled");
			blw.writeLine("  if ((submitMode != null) && submitMode.value)");
			blw.writeLine("    submit = (submitMode.value.indexOf('OK') != -1);");
			blw.writeLine("  if (!submit) return; // no use generating parameters if request was cancelled ...");
			blw.writeLine("  for (var w = 0; w < wordBoxes.length; w++) {");
			blw.writeLine("    var wbType = document.getElementById(wordBoxes[w].wbId + 'T');");
			blw.writeLine("    if (wbType == null) {");
			blw.writeLine("      wbType = document.createElement('input');");
			blw.writeLine("      wbType.id = wordBoxes[w].wbId + 'T';");
			blw.writeLine("      wbType.type = 'hidden';");
			blw.writeLine("      wbType.name = wordBoxes[w].wbId + 'T';");
			blw.writeLine("      feedbackForm.appendChild(wbType);");
			blw.writeLine("    }");
			blw.writeLine("    var wbt = 'C';");
			blw.writeLine("    if (wordBoxes[w].boxType) {");
			blw.writeLine("      var wbParent = wordBoxes[w].parentBox;");
			blw.writeLine("      wbt = wbParent.boxType;");
			blw.writeLine("      wbParent = wbParent.parentBox;");
			blw.writeLine("      while (wbParent != masterBox) {");
			blw.writeLine("        if (wbParent.firstWordBox == wordBoxes[w])");
			blw.writeLine("          wbt = (wbParent.boxType + '.' + wbt);");
			blw.writeLine("        else wbt = ('C.' + wbt);");
			blw.writeLine("        wbParent = wbParent.parentBox;");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    wbType.value = wbt;");
			blw.writeLine("    var wbNext = document.getElementById(wordBoxes[w].wbId + 'N');");
			blw.writeLine("    if (wbNext == null) {");
			blw.writeLine("      wbNext = document.createElement('input');");
			blw.writeLine("      wbNext.id = wordBoxes[w].wbId + 'N';");
			blw.writeLine("      wbNext.type = 'hidden';");
			blw.writeLine("      wbNext.name = wordBoxes[w].wbId + 'N';");
			blw.writeLine("      feedbackForm.appendChild(wbNext);");
			blw.writeLine("    }");
			blw.writeLine("    wbNext.value = (wordBoxes[w].nextWordBox) ? wordBoxes[w].nextWordBox.wbId : '';");
			blw.writeLine("  }");
			blw.writeLine("  var firstWb = document.getElementById('firstWbId');");
			blw.writeLine("  if (firstWb == null) {");
			blw.writeLine("    firstWb = document.createElement('input');");
			blw.writeLine("    firstWb.id = 'firstWbId';");
			blw.writeLine("    firstWb.type = 'hidden';");
			blw.writeLine("    firstWb.name = 'firstWbId';");
			blw.writeLine("    feedbackForm.appendChild(firstWb);");
			blw.writeLine("  }");
			blw.writeLine("  firstWb.value = (masterBox.firstWordBox) ? masterBox.firstWordBox.wbId : '';");
			blw.writeLine("}");
			
			blw.writeLine("var errorMessage;");
			blw.writeLine("var errorMessageBox;");
			blw.writeLine("function showError() {");
			blw.writeLine("  if (errorMessageBox == null) {");
			blw.writeLine("    errorMessageBox = document.getElementById('errorMessageBox');");
			blw.writeLine("    errorMessageBox.style.width = canvas.w + 'px';");
			blw.writeLine("    errorMessageBox.style.height = canvas.h + 'px';");
			blw.writeLine("    errorMessageBox.onmousedown = function() {");
			blw.writeLine("      eventConsumed = true;");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  if (errorMessage == null) {");
			blw.writeLine("    errorMessage = document.getElementById('errorMessage');");
			blw.writeLine("    errorMessage.style.left = ((canvas.w - (2*imageBorder) - cutPx(errorMessage.style.width)) / 2) + 'px';");
			blw.writeLine("    errorMessage.style.top = ((canvas.h - (2*imageBorder) - cutPx(errorMessage.style.height)) / 2) + 'px';");
			blw.writeLine("    errorMessage.onmousedown = function() {");
			blw.writeLine("      eventConsumed = true;");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  errorMessageBox.style.display = '';");
			blw.writeLine("  errorMessage.style.display = '';");
			blw.writeLine("}");
			blw.writeLine("function hideError() {");
			blw.writeLine("  errorMessageBox.style.display = 'none';");
			blw.writeLine("  errorMessage.style.display = 'none';");
			blw.writeLine("  eventConsumed = true;");
			blw.writeLine("}");
			
			//	store box type colors
			blw.writeLine("var borderColors = new Object();");
			blw.writeLine("function initColors() {");
			for (Iterator btit = this.boxTypeColors.keySet().iterator(); btit.hasNext();) {
				String boxType = ((String) btit.next());
				Color boxTypeColor = this.getTypeColor(boxType);
				blw.writeLine("  borderColors." + boxType + " = '#" + getRGB(boxTypeColor) + "';");
			}
			blw.writeLine("  var toc = 0;");
			blw.writeLine("  for (var c = 0; c < typeSelector.childNodes.length; c++) {");
			blw.writeLine("    var typeOption = typeSelector.childNodes[c];");
			blw.writeLine("    if (!typeOption.id)");
			blw.writeLine("      continue;");
			blw.writeLine("    var typeColor = borderColors[typeOption.id];");
			blw.writeLine("    if (!typeColor)");
			blw.writeLine("      continue;");
			blw.writeLine("    typeOption.style.borderColor = typeColor;");
			blw.writeLine("    typeOption.style.backgroundColor = '#FFFFFF';");
			blw.writeLine("    typeOption.style.fontSize = ((buttonSize-6) + 'px');");
			blw.writeLine("    typeOption.style.top = (((toc * buttonSize)+2) + 'px');");
			blw.writeLine("    typeOption.style.height = ((buttonSize-4) + 'px');");
			blw.writeLine("    toc++;");
			blw.writeLine("    initTypeOption(typeOption);");
			blw.writeLine("  }");
			blw.writeLine("  typeSelector.style.height = ((toc * buttonSize) + 'px');");
			blw.writeLine("}");
			blw.writeLine("function initTypeOption(typeOption) {");
			blw.writeLine("  var boxType = typeOption.id;");
			blw.writeLine("  typeOption.onmousedown = function() {");
			blw.writeLine("    if (retyping != null) {");
			blw.writeLine("      retyping.boxType = boxType;");
			blw.writeLine("      updateTypeLayout(retyping);");
			blw.writeLine("      hideButtons(retyping);");
			blw.writeLine("      eventConsumed = true;");
			blw.writeLine("      stopRetyping();");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			//	add word boxes
			blw.writeLine("function drawWordBoxes() {");
			for (int w = 0; w < this.wordBoxes.size(); w++) {
				WordBox wb = ((WordBox) this.wordBoxes.get(w));
				StringBuffer word = new StringBuffer();
				for (int c = 0; c < wb.word.length(); c++) {
					char ch = wb.word.charAt(c);
					if ((ch == '\n') || (ch == '\r') || (ch == '\f') || (ch == '\t'))
						word.append(' ');
					else {
						if ((ch == '\'') || (ch == '\\'))
							word.append('\\');
						word.append(ch);
					}
				}
				blw.writeLine("  drawWordBox(" + wb.left + "," + wb.top + "," + wb.width + "," + wb.height + ",'" + word + "','" + wb.id + "'," + wb.hasAscent + "," + wb.hasDescent + ");");
			}
			blw.writeLine("}");
			
			//	add boxes
			blw.writeLine("function drawBoxes() {");
			for (int b = 0; b < this.boxes.size(); b++) {
				RegionBox rb = ((RegionBox) this.boxes.get(b));
				if (rb == this.masterBox)
					continue;
				blw.writeLine("  drawBox(" + rb.left + "," + rb.top + "," + rb.width + "," + rb.height + ",'" + rb.boxType + "');");
			}
			blw.writeLine("}");
			
			blw.writeLine("var imageBorder = " + this.borderWidth + ";");
			blw.writeLine("var scaleFactor = " + this.getHtmlZoomFactor() + ";");
			blw.writeLine("var hBoxOffset = -" + this.masterBox.left + ";");
			blw.writeLine("var vBoxOffset = -" + this.masterBox.top + ";");
			
			blw.writeLine("function drawWordBox(l, t, w, h, txt, id, hasAsc, hasDesc) {");
			blw.writeLine("  var wordBox = document.createElement('div');");
			blw.writeLine("  wordBox.className = 'wordBox';");
			blw.writeLine("  wordBox.x = Math.round((l + hBoxOffset) * scaleFactor) + imageBorder;");
			blw.writeLine("  wordBox.y = Math.round((t + vBoxOffset) * scaleFactor) + imageBorder;");
			blw.writeLine("  wordBox.w = Math.round(w * scaleFactor);");
			blw.writeLine("  wordBox.h = Math.round(h * scaleFactor);");
			blw.writeLine("  wordBox.style.left = wordBox.x + 'px';");
			blw.writeLine("  wordBox.style.top = wordBox.y + 'px';");
			blw.writeLine("  wordBox.style.width = wordBox.w + 'px';");
			blw.writeLine("  wordBox.style.height = wordBox.h + 'px';");
			blw.writeLine("  wordBox.word = txt;");
			blw.writeLine("  wordBox.wbId = id;");
			blw.writeLine("  wordBox.hasAsc = hasAsc;");
			blw.writeLine("  wordBox.hasDesc = hasDesc;");
			blw.writeLine("  canvas.appendChild(wordBox);");
			blw.writeLine("  wordBoxes.push(wordBox);");
			blw.writeLine("}");
			
			blw.writeLine("function drawBox(l, t, w, h, boxType) {");
			blw.writeLine("  var bl = Math.round((l + hBoxOffset) * scaleFactor) + imageBorder;");
			blw.writeLine("  var bt = Math.round((t + vBoxOffset) * scaleFactor) + imageBorder;");
			blw.writeLine("  var bw = Math.round(w * scaleFactor);");
			blw.writeLine("  var bh = Math.round(h * scaleFactor);");
			blw.writeLine("  startPosition(bl, bt);");
			blw.writeLine("  var box = drawing;");
			blw.writeLine("  updatePosition((bl+bw), (bt+bh));");
			blw.writeLine("  endPosition((bl+bw), (bt+bh));");
			blw.writeLine("  box.boxType = boxType;");
			blw.writeLine("  updateTypeLayout(box);");
			blw.writeLine("  hideButtons(box);");
			blw.writeLine("}");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScriptInitFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			blw.writeLine("  canvas = document.getElementById('canvas');");
			blw.writeLine("  canvas.onmousedown = function(event) {");
			blw.writeLine("    if (eventConsumed) {");
			blw.writeLine("      eventConsumed = false;");
			blw.writeLine("      return;");
			blw.writeLine("    }");
			blw.writeLine("    var ex = getX(event);");
			blw.writeLine("    var ey = getY(event);");
			blw.writeLine("    if ((ex == -1) || (ey == -1))");
			blw.writeLine("      return;");
			blw.writeLine("    startPosition(ex, ey);");
			blw.writeLine("  };");
			blw.writeLine("  canvas.onmousemove = function(event) {");
			blw.writeLine("    var ex = getX(event);");
			blw.writeLine("    var ey = getY(event);");
			blw.writeLine("    if ((ex == -1) || (ey == -1))");
			blw.writeLine("      return;");
			blw.writeLine("    updatePosition(ex, ey);");
			blw.writeLine("  };");
			blw.writeLine("  canvas.onmouseup = function(event) {");
			blw.writeLine("    var ex = getX(event);");
			blw.writeLine("    var ey = getY(event);");
			blw.writeLine("    if ((ex == -1) || (ey == -1))");
			blw.writeLine("      return;");
			blw.writeLine("    endPosition(ex, ey);");
			blw.writeLine("  };");
			blw.writeLine("  canvas.onmouseover = function() {};");
			blw.writeLine("  if (canvas.offsetParent) {");
			blw.writeLine("    var cx = 0;");
			blw.writeLine("    var cy = 0;");
			blw.writeLine("    var c = canvas;");
			blw.writeLine("    do {");
			blw.writeLine("      cx += c.offsetLeft;");
			blw.writeLine("      cy += c.offsetTop;");
			blw.writeLine("    } while (c = c.offsetParent);");
			blw.writeLine("    canvas.x = cx;");
			blw.writeLine("    canvas.y = cy;");
			blw.writeLine("  }");
			blw.writeLine("  else {");
			blw.writeLine("    canvas.x = cutPx(canvas.style.left);");
			blw.writeLine("    canvas.y = cutPx(canvas.style.top);");
			blw.writeLine("  }");
			blw.writeLine("  canvas.w = cutPx(canvas.style.width);");
			blw.writeLine("  canvas.h = cutPx(canvas.style.height);");
			blw.writeLine("  canvas.style.left = canvas.x + 'px';");
			blw.writeLine("  canvas.style.top = canvas.y + 'px';");
			blw.writeLine("  canvas.style.width = canvas.w + 'px';");
			blw.writeLine("  canvas.style.height = canvas.h + 'px';");
			blw.writeLine("  canvas.style.position = 'absolute';");
			blw.writeLine("  if (navigator.appName.indexOf('Microsoft') != -1) {");
			blw.writeLine("    borderOffset = 4;  ");
			blw.writeLine("    canvas.style.width = (canvas.w + (borderOffset * 2)) + 'px';");
			blw.writeLine("    canvas.style.height = (canvas.h + (borderOffset * 2)) + 'px';");
			blw.writeLine("    var canvasBox = document.getElementById('canvasBox');");
			blw.writeLine("    canvasBox.style.width = (canvas.w + (borderOffset * 2)) + 'px';");
			blw.writeLine("    canvasBox.style.height = (canvas.h + (borderOffset * 2)) + 'px';");
			blw.writeLine("  }");
			
			blw.writeLine("  var image = document.getElementById('image');");
			blw.writeLine("  image.onmousedown = function(event) {");
			blw.writeLine("    if (eventConsumed) {");
			blw.writeLine("      eventConsumed = false;");
			blw.writeLine("      return;");
			blw.writeLine("    }");
			blw.writeLine("    var ex = getX(event);");
			blw.writeLine("    var ey = getY(event);");
			blw.writeLine("    if ((ex == -1) || (ey == -1))");
			blw.writeLine("      return;");
			blw.writeLine("    startPosition(ex, ey);");
			blw.writeLine("  };");
			blw.writeLine("  image.onmousemove = function(event) {");
			blw.writeLine("    var ex = getX(event);");
			blw.writeLine("    var ey = getY(event);");
			blw.writeLine("    if ((ex == -1) || (ey == -1))");
			blw.writeLine("      return;");
			blw.writeLine("    updatePosition(ex, ey);");
			blw.writeLine("  };");
			blw.writeLine("  image.onmouseup = function(event) {");
			blw.writeLine("    var ex = getX(event);");
			blw.writeLine("    var ey = getY(event);");
			blw.writeLine("    if ((ex == -1) || (ey == -1))");
			blw.writeLine("      return;");
			blw.writeLine("    endPosition(ex, ey);");
			blw.writeLine("  };");
			blw.writeLine("  image.onmouseover = function() {};");
			blw.writeLine("  image.onmousedragged = function() {};");
			blw.writeLine("  image.onclick = function() {};");
			blw.writeLine("  image.ondragdrop = function() {};");
			blw.writeLine("  image.style.position = 'absolute';");
			blw.writeLine("  image.style.margin = '" + this.borderWidth + "px';");
			
			blw.writeLine("  typeSelector = document.getElementById('typeSelector');");
			blw.writeLine("  document.getElementById('canvasBox').removeChild(typeSelector);");
			blw.writeLine("  typeSelector.style.display = 'none';");
			
			blw.writeLine("  initColors();");
			
			blw.writeLine("  var bl = 0;");
			blw.writeLine("  var bt = 0;");
			blw.writeLine("  var bw = canvas.w;");
			blw.writeLine("  var bh = canvas.h;");
			blw.writeLine("  startPosition(bl, bt);");
			blw.writeLine("  var mBox = drawing;");
			blw.writeLine("  updatePosition((bl+bw), (bt+bh));");
			blw.writeLine("  endPosition((bl+bw), (bt+bh));");
			blw.writeLine("  hideButtons(mBox);");
			blw.writeLine("  masterBox = mBox;");
			blw.writeLine("  masterBox.isContainer = true;");
			blw.writeLine("  masterBox.className = 'squareMaster';");
			blw.writeLine("  masterBox.x = 0;");
			blw.writeLine("  masterBox.y = 0;");
			blw.writeLine("  masterBox.w = canvas.w;");
			blw.writeLine("  masterBox.h = canvas.h;");
			blw.writeLine("  masterBox.style.left = masterBox.x + 'px';");
			blw.writeLine("  masterBox.style.width = masterBox.w + 'px';");
			blw.writeLine("  masterBox.style.top = masterBox.y + 'px';");
			blw.writeLine("  masterBox.style.height = masterBox.h + 'px';");
			
			blw.writeLine("  drawWordBoxes();");
			blw.writeLine("  drawBoxes();");
			
//			blw.writeLine("  var forms = document.getElementsByTagName('form');");
//			blw.writeLine("  if (forms.length != 0) {");
//			blw.writeLine("    feedbackForm = forms[0];");
//			blw.writeLine("    feedbackForm.onsubmit = function() {");
//			blw.writeLine("      var complete = checkComplete();");
//			blw.writeLine("      if (complete) {");
//			blw.writeLine("        writeFormParameters();");
//			blw.writeLine("        return true;");
//			blw.writeLine("      }");
//			blw.writeLine("      else return false;");
//			blw.writeLine("    };");
//			blw.writeLine("  }");
//			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeJavaScriptCheckFeedbackFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptCheckFeedbackFunctionBody(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine("  var complete = checkComplete();");
			blw.writeLine("  if (complete) {");
			blw.writeLine("    writeFormParameters();");
			blw.writeLine("    return true;");
			blw.writeLine("  }");
			blw.writeLine("  else return false;");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writePanelBody(java.io.Writer)
		 */
		public void writePanelBody(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			float htmlZoomFactor = this.getHtmlZoomFactor();
			int htmlCanvasWidth = ((int) (htmlZoomFactor * this.masterBox.width)) + (2 * this.borderWidth);
			int htmlCanvasHeight = ((int) (htmlZoomFactor * this.masterBox.height)) + (2 * this.borderWidth);
			int htmlImageWidth = htmlCanvasWidth - (2 * this.borderWidth);
			int htmlImageHeight = htmlCanvasHeight - (2 * this.borderWidth);
			BoundingBox imageBox = new BoundingBox(this.masterBox.left, (this.masterBox.left + this.masterBox.width), this.masterBox.top, (this.masterBox.top + this.masterBox.height));
			
			blw.writeLine("<div id=\"canvasBox\" style=\"z-index: 0; width: " + htmlCanvasWidth + "px; height: " + htmlCanvasHeight + "px; margin-bottom: 20px;\">");
				
				blw.writeLine("<div id=\"canvas\" style=\"z-index: 1; top: 0px; left: 0px; width: " + htmlCanvasWidth + "px; height: " + htmlCanvasHeight + "px; border: medium solid black;\">");
					
					blw.writeLine("<img id=\"image\" src=\"" + this.imageProviderBaseUrl + "/" + this.docId + "/" + this.pageId + "." + IMAGE_FORMAT + "?" + BOUNDING_BOX_ATTRIBUTE + "=" + imageBox.toString() + "\" style=\"z-index: 1; left: 0px; top: 0px; width: " + htmlImageWidth + "px; height: " + htmlImageHeight + "px; margin: " + this.borderWidth + "px;\">");
					
					blw.writeLine("<div id=\"errorMessageBox\" class=\"errorMessageBox\" style=\"position: absolute; display: none; top: 0px; left: 0px; width: " + htmlCanvasWidth + "px; height: " + htmlCanvasHeight + "px; z-index: 100;\"></div>");
					
					blw.writeLine("<div id=\"errorMessage\" class=\"errorMessage\" style=\"position: absolute; display: none; top: 240px; left: 110px; width: 240px; height: 170px; z-index: 101; opacity: 0.9; filter: alpha(opacity = 90);\">");
						blw.writeLine("Some words (marked in <b>bold red</b>)<br/>");
						blw.writeLine("are outside paragraphs.<br/>");
						blw.writeLine("<br/>");
						blw.writeLine("Columns do not count as paragraphs,<br/>");
						blw.writeLine("and neither do any boxes with main<br/>");
						blw.writeLine("text paragraphs nested in them,<br/>");
						blw.writeLine("as both are used for grouping only.<br/>");
						blw.writeLine("Affected boxes have <b>dotted lines</b>.<br/>");
						blw.writeLine("<br/>");
						blw.writeLine("Please make sure all words are<br/>");
						blw.writeLine("properly included in paragraphs.<br/>");
						blw.writeLine("<br/>");
						blw.writeLine("<input type=\"button\" value=\"OK\" onmousedown=\"hideError();\">");
					blw.writeLine("</div>");
					
				blw.writeLine("</div>");
				
				//	build type selector
				blw.writeLine("<div id=\"typeSelector\" style=\"position: absolute; width: 200; background-color: white;\">");
				for (Iterator btit = this.boxTypeColors.keySet().iterator(); btit.hasNext();) {
					String boxType = ((String) btit.next());
					String boxTypeLabel = ((JMenuItem) this.boxTypeOptions.get(boxType)).getText();
					blw.writeLine("<div id=\"" + boxType + "\" class=\"typeOption\" style=\"position: absolute; z-index: 30; width: 100%; height: 16; border: 2px solid\">" + IoTools.prepareForHtml(boxTypeLabel) + "</div>");
				}
				blw.writeLine("</div>");
				
			blw.writeLine("</div>");
			
			if (blw != out)
				blw.flush();
		}
	}
}
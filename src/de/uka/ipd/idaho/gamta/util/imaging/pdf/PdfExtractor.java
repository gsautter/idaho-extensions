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
package de.uka.ipd.idaho.gamta.util.imaging.pdf;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.icepdf.core.io.SeekableByteArrayInputStream;
import org.icepdf.core.io.SeekableInputConstrainedWrapper;
import org.icepdf.core.pobjects.Catalog;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.pobjects.PageTree;
import org.icepdf.core.pobjects.Reference;
import org.icepdf.core.pobjects.Resources;
import org.icepdf.core.pobjects.Stream;
import org.icepdf.core.pobjects.filters.FlateDecode;
import org.icepdf.core.pobjects.graphics.text.LineText;
import org.icepdf.core.pobjects.graphics.text.PageText;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.icepdf.core.util.Library;
import org.jpedal.jbig2.JBIG2Decoder;
import org.jpedal.jbig2.JBIG2Exception;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.constants.TableConstants;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging.AnalysisImage;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging.Complex;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging.Peak;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.imaging.PageAnalysis;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageAnalysis;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageAnalysis.Block;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageAnalysis.Line;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageAnalysis.Region;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageAnalysis.TableCell;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageAnalysis.TableRow;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageAnalysis.Word;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageConverter;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageInputStream;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageStore;
import de.uka.ipd.idaho.gamta.util.imaging.ocr.OcrEngine;
import de.uka.ipd.idaho.gamta.util.imaging.pdf.PdfParser.PImage;
import de.uka.ipd.idaho.gamta.util.imaging.pdf.PdfParser.PStream;
import de.uka.ipd.idaho.gamta.util.imaging.pdf.PdfParser.PWord;
import de.uka.ipd.idaho.gamta.util.imaging.test.PdfExtractorTest;
import de.uka.ipd.idaho.stringUtils.StringIndex;

/**
 * Utility for extracting page images from a PDF file.
 * 
 * @author sautter
 */
public class PdfExtractor implements ImagingConstants, TableConstants {
	
	private File basePath;
	private PageImageStore imageStore;
	
	private OcrEngine ocrEngine;
	
	private boolean useMultipleCores = false;
	
	/**
	 * Constructor
	 * @param basePath the folder holding the binaries
	 * @param imageStore the page image store to store extracted page images in
	 */
	public PdfExtractor(File basePath, PageImageStore imageStore) {
		this(basePath, imageStore, false);
	}
	
	/**
	 * Constructor
	 * @param basePath the folder holding the binaries
	 * @param imageStore the page image store to store extracted page images in
	 * @param useMultipleCores use multiple CPU cores for PDF extraction if
	 *            possible (i.e., if multiple cores available)?
	 */
	public PdfExtractor(File basePath, PageImageStore imageStore, boolean useMultipleCores) {
		this.basePath = basePath;
		this.imageStore = imageStore;
		try {
			this.ocrEngine = new OcrEngine(basePath);
		}
		catch (Exception e) {
			System.out.println("PdfExtractor: could not create OCR engine - " + e.getMessage());
			e.printStackTrace(System.out);
		}
		this.useMultipleCores = useMultipleCores;
	}
	
	/**
	 * Test whether or not the PDF extractor has an OCR engine available and
	 * thus provides the ability to load image based PDFs.
	 * @return true if OCR is available, false otherwise
	 */
	public boolean isOcrAvailable() {
		return (this.ocrEngine != null);
	}
	
	/**
	 * Load a document from a PDF. Page images are stored in the page image
	 * store handed to the constructor. The document structure is stored in the
	 * argument GAMTA document, including textual contents if available.
	 * @param doc the document to store the structural information in
	 * @param pdfDoc the PDF document to parse
	 * @param bytes the raw binary data the the PDF document was parsed from
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @return the argument GAMTA document
	 * @throws IOException
	 */
	public DocumentRoot loadGenericPdf(DocumentRoot doc, Document pdfDoc, byte[] bytes, ProgressMonitor psm) throws IOException {
		return this.loadGenericPdf(doc, pdfDoc, bytes, 1, psm);
	}
	
	/**
	 * Load a document from a PDF. Page images are stored in the page image
	 * store handed to the constructor. The document structure is stored in the
	 * argument GAMTA document, including textual contents if available. The
	 * scale factor helps with PDFs whose page boundaries (media boxes) are set
	 * too small. This can be recognized in any PDF viewer by the fact that the
	 * document is tiny and the writing very small at 100% size display. It can
	 * also be detected automatically, e.g. by means of examining the average
	 * height of lines in comparison to the DPI number.
	 * @param doc the document to store the structural information in
	 * @param pdfDoc the PDF document to parse
	 * @param bytes the raw binary data the the PDF document was parsed from
	 * @param scaleFactor the scale factor for image PDFs, to correct PDFs that
	 *            are set too small (DPI number is divided by this factor)
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @return the argument GAMTA document
	 * @throws IOException
	 */
	public DocumentRoot loadGenericPdf(DocumentRoot doc, Document pdfDoc, byte[] bytes, int scaleFactor, ProgressMonitor psm) throws IOException {
		if (psm == null)
			psm = ProgressMonitor.dummy;
		/*
		 * try image loading first, and use text loading only if image loading
		 * fails, as if both images and text are present, the text may well be
		 * some crude OCR we do not want.
		 */
		try {
			return this.loadImagePdf(doc, pdfDoc, bytes, scaleFactor, psm);
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
			psm.setInfo("Could not find images for all pages, loading PDF as text");
			return this.loadTextPdf(doc, pdfDoc, bytes, psm);
		}
	}
	
	private static final int defaultDpi = 72; // default according to PDF specification
	
	//	TODO make these two configurable
	private static final int defaultPageImageDpi = 150; // looks a lot better
//	private static final int maxAnalysisDpi = 300; // higher resolutions are too computationally expensive
	private static final int minAveragePageWords = 25; // this should be exceeded by every digital-born PDF
	
	//	A5 paper format in inchec: 5.83 × 8.27, A6: 4.13 × 5.83
	private static final float a5inchWidth = 5.83f;
	private static final float a5inchHeigth = 8.27f;
//	private static final float a6inchWidth = 4.13f;
//	private static final float a6inchHeigth = 5.83f;
	private static final int minScaleupDpi = 301;
	
	/**
	 * Load a document from a textual PDF, usually a digital-born PDF. Page
	 * images are stored in the page image store handed to the constructor. The
	 * document structure is stored in the argument GAMTA document, including
	 * textual contents with word bounding boxes.
	 * @param doc the document to store the structural information in
	 * @param pdfDoc the PDF document to parse
	 * @param bytes the raw binary data the the PDF document was parsed from
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @return the argument GAMTA document
	 * @throws IOException
	 */
	public DocumentRoot loadTextPdf(DocumentRoot doc, Document pdfDoc, byte[] bytes, ProgressMonitor psm) throws IOException {
		if (psm == null)
			psm = ProgressMonitor.dummy;
		
		//	get document ID
		String docId = ((String) doc.getAttribute(DOCUMENT_ID_ATTRIBUTE));
		if (docId == null) {
			docId = doc.getAnnotationID();
			doc.setAttribute(DOCUMENT_ID_ATTRIBUTE, docId);
		}
		
		//	do it
		return this.doLoadTextPdf(doc, docId, pdfDoc, bytes, psm);
	}
	
	private DocumentRoot doLoadTextPdf(final DocumentRoot doc, final String docId, final Document pdfDoc, byte[] bytes, final ProgressMonitor psm) throws IOException {
		
		//	load document structure (IcePDF is better at that ...)
		final Catalog catalog = pdfDoc.getCatalog();
		final PageTree pageTree = catalog.getPageTree();
		final float magnification = (((float) defaultPageImageDpi) / defaultDpi);
		
		//	parse PDF
		final HashMap objects = PdfParser.getObjects(bytes);
		
		//	create dummy token sequence for storing words
		final TokenSequence dummyTokens = Gamta.newTokenSequence("D", doc.getTokenizer());
		
		//	prepare working in parallel
		final LinkedList pageIDs = new LinkedList();
		final Throwable[] error = {null};
		
		//	extract page objects
		psm.setStep("Importing page words");
		psm.setBaseProgress(0);
		psm.setProgress(0);
		psm.setMaxProgress(40);
		final Rectangle2D.Float[] pageBoxes = new Rectangle2D.Float[pageTree.getNumberOfPages()];
		final PWord[][] pWords = new PWord[pageTree.getNumberOfPages()][];
		final ArrayList[] pWordAnnots = new ArrayList[pageTree.getNumberOfPages()];
		pageIDs.clear();
		if (PdfExtractorTest.aimAtPage != -1) // TODO remove this after test
			pageIDs.addLast(new Integer(PdfExtractorTest.aimAtPage));
		else for (int p = 0; p < pageTree.getNumberOfPages(); p++)
			pageIDs.addLast(new Integer(p));
		error[0] = null;
		this.runJob( new Runnable() {
			public void run() {
				while (true) try {
					
					//	any error or other problems in siblings?
					synchronized (error) {
						if (error[0] != null)
							return;
					}
					
					//	get next page ID to work off
					int p;
					synchronized (pageIDs) {
						if (pageIDs.isEmpty())
							return;
						p = ((Integer) pageIDs.removeFirst()).intValue();
					}
					
					//	update status display (might be inaccurate, but better than lock escalation)
					synchronized (psm) {
						psm.setInfo("Importing page " + (pageTree.getNumberOfPages() - pageIDs.size()) + " of " + pageTree.getNumberOfPages());
						psm.setProgress(( (pageTree.getNumberOfPages() - pageIDs.size() - 1) * 100) / pageTree.getNumberOfPages());
					}
					
					//	get page bounding box
					Page page = pageTree.getPage(p, "");
					pageBoxes[p] = page.getCropBox();
					if (pageBoxes[p] == null)
						pageBoxes[p] = page.getMediaBox();
					
					//	extract page contents to recover layout information
					System.out.println("Page content is " + page.getEntries());
					Object contentsObj = PdfParser.getObject(page.getEntries(), "Contents", objects);
					pWords[p] = null;
					if (contentsObj == null) synchronized (psm) {
						psm.setInfo(" --> content not found");
					}
					else {
						synchronized (psm) {
							psm.setInfo(" --> got content");
						}
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						if (contentsObj instanceof PStream) {
							Object filter = ((PStream) contentsObj).params.get("Filter");
							synchronized (psm) {
								psm.setInfo("   --> stream content, filter is " + filter);
							}
							PdfParser.decode(filter, ((PStream) contentsObj).bytes, ((PStream) contentsObj).params, baos, objects);
						}
						else if (contentsObj instanceof Vector) {
							synchronized (psm) {
								psm.setInfo("   --> array content");
							}
							for (Iterator cit = ((Vector) contentsObj).iterator(); cit.hasNext();) {
								Object contentObj = PdfParser.dereference(cit.next(), objects);
								if (contentObj instanceof PStream) {
									Object filter = ((PStream) contentObj).params.get("Filter");
									if (filter == null)
										continue;
									PdfParser.decode(filter, ((PStream) contentObj).bytes, ((PStream) contentObj).params, baos, objects);
								}
							}
						}
						Object resourcesObj = PdfParser.getObject(page.getEntries(), "Resources", objects);
						if (resourcesObj == null) synchronized (psm) {
							psm.setInfo(" --> resources not found");
						}
						else {
							resourcesObj = PdfParser.dereference(resourcesObj, objects);
							synchronized (psm) {
								psm.setInfo(" --> resources are " + resourcesObj);
							}
							pWords[p] = PdfParser.parsePageContent(baos.toByteArray(), ((Hashtable) resourcesObj), objects, pageBoxes[p].width, pageBoxes[p].height, defaultPageImageDpi);
							System.out.println(" --> extracted " + pWords[p].length + " words");
							for (int w = 0; w < pWords[p].length; w++)
								System.out.println("   - " + pWords[p][w]);
						}
					}
					
					if ((pWords[p] == null) || (pWords[p].length == 0)) {
						synchronized (psm) {
							psm.setInfo(" --> empty page");
						}
						continue;
					}
					
					synchronized (psm) {
						psm.setInfo(" - got page text with " + pWords[p].length + " words");
					}
					
					//	shrink word bounding boxes to actual word size
					BufferedImage mbi = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
					Graphics2D mg = mbi.createGraphics();
					for (int w = 0; w < pWords[p].length; w++) {
						String wordStr = pWords[p][w].str.trim();
						if (wordStr.length() == 0)
							continue;
						Rectangle2D wb = pWords[p][w].bounds;
						
						//	convert bounds, as PDF Y coordinate is bottom-up, whereas Java, JavaScript, etc. Y coordinate is top-down
						//	NO ADJUSTMENT TO JAVA COORDINATES HERE, WE'RE TRANSFORMING ALL WORDS LATER ON
						float left = ((float) wb.getMinX());
						float right = ((float) wb.getMaxX());
						float top = ((float) wb.getMaxY());
						float bottom = ((float) wb.getMinY());
						
						//	shrink bounding boxes to words
						System.out.println("Measuring word " + pWords[p][w].str);
						
						//	prepare font
						int fontStyle = Font.PLAIN;
						if (pWords[p][w].bold)
							fontStyle = (fontStyle | Font.BOLD);
						if (pWords[p][w].italics)
							fontStyle = (fontStyle | Font.ITALIC);
						Font mf = getFont(pWords[p][w].fontName, fontStyle, Math.round(((float) pWords[p][w].fontSize) * 1));
						mg.setFont(mf);
						
						//	adjust word size and vertical position
						TextLayout wtl = new TextLayout((pWords[p][w].str + "IpHq"), mf, mg.getFontRenderContext());
						System.out.println(" - word bounding box is " + wtl.getBounds());
						float boundingBoxY = ((float) wtl.getBounds().getY());
						float boundingBoxHeight = ((float) wtl.getBounds().getHeight());
						float baseline = (top + boundingBoxY);
						System.out.println(" - top is " + top + ", bottom is " + bottom + ", baseline is " + baseline + ", ascent is " + wtl.getAscent() + ", descent is " + wtl.getDescent());
						
						//	if computed bounding box smaller (lower top, higher bottom) than one from PDF, shrink it
						float adjustedTop = (baseline - boundingBoxY);
						float adjustedBottom = (adjustedTop - boundingBoxHeight);
						System.out.println(" - word box y is " + boundingBoxY + ", word box height is " + boundingBoxHeight);
						System.out.println(" - adjusted top is " + adjustedTop + ", adjusted bottom is " + adjustedBottom);
						if (((top - adjustedTop) > 0.25) || ((adjustedBottom - bottom) > 0.25)) {
							PWord pw = new PWord(pWords[p][w].str, new Rectangle2D.Float(left, adjustedBottom, (right - left), (adjustedTop - adjustedBottom)), pWords[p][w].fontSize, pWords[p][w].bold, pWords[p][w].italics, pWords[p][w].fontName, pWords[p][w].fontHasDescent);
							pWords[p][w] = pw;
							System.out.println(" ==> adjusted bounding box to " + pw.bounds);
						}
					}
					
					//	sort words left to right and top to bottom
					Arrays.sort(pWords[p], new Comparator() {
						public int compare(Object o1, Object o2) {
							PWord pw1 = ((PWord) o1);
							PWord pw2 = ((PWord) o2);
							return Double.compare(pw1.bounds.getMinX(), pw2.bounds.getMinX());
						}
					});
					Arrays.sort(pWords[p], new Comparator() {
						public int compare(Object o1, Object o2) {
							PWord pw1 = ((PWord) o1);
							PWord pw2 = ((PWord) o2);
							double m1 = ((pw1.bounds.getMinY() + pw1.bounds.getMaxY()) / 2);
							double m2 = ((pw2.bounds.getMinY() + pw2.bounds.getMaxY()) / 2);
							if ((m1 < pw2.bounds.getMaxY()) && (m1 > pw2.bounds.getMinY()))
								return 0;
							if ((m2 < pw1.bounds.getMaxY()) && (m2 > pw1.bounds.getMinY()))
								return 0;
							if (m1 > pw2.bounds.getMaxY())
								return -1;
							if (m1 < pw2.bounds.getMinY())
								return 1;
							if (m2 > pw1.bounds.getMaxY())
								return 1;
							if (m2 < pw1.bounds.getMinY())
								return -1;
							return 0;
						}
					});
					
					//	TODO figure out / ask if merging super and sub script numbers is OK
					
					//	merge subsequent words less than DPI/60 apart (likely separated due to font change)
					int maxMergeMargin = (defaultPageImageDpi / 60);
					ArrayList pWordList = null;
					PWord lpWord = null;
					BoundingBox lpWordBox = null;
					System.out.println("Checking word mergers ...");
					for (int w = 0; w < pWords[p].length; w++) {
						
						//	generate bounding box
						int bbLeft = Math.round(((float) (pWords[p][w].bounds.getMinX() - pageBoxes[p].getMinX())) * magnification);
						int bbRight = Math.round(((float) (pWords[p][w].bounds.getMaxX() - pageBoxes[p].getMinX()))  * magnification);
						int bbTop = Math.round((pageBoxes[p].height - ((float) (pWords[p][w].bounds.getMaxY() - ((2 * pageBoxes[p].getMinY()) - pageBoxes[p].getMaxY())))) * magnification);
						int bbBottom = Math.round((pageBoxes[p].height - ((float) (pWords[p][w].bounds.getMinY() - ((2 * pageBoxes[p].getMinY()) - pageBoxes[p].getMaxY())))) * magnification);
						BoundingBox pWordBox = new BoundingBox(bbLeft, bbRight, bbTop, bbBottom);
						
						//	no word to compare to
						if ((lpWord == null) || (lpWordBox == null)) {
							lpWord = pWords[p][w];
							lpWordBox = pWordBox;
							continue;
						}
						System.out.println(" - checking words " + lpWordBox + " and " + pWordBox);
						
						//	not in same line
						if ((pWordBox.top >= lpWordBox.bottom) || (lpWordBox.top >= pWordBox.bottom)) {
							lpWord = pWords[p][w];
							lpWordBox = pWordBox;
							System.out.println(" --> different lines");
							continue;
						}
						
						if ((pWordBox.left < lpWordBox.left) || ((pWordBox.left - lpWordBox.right) > maxMergeMargin)) {
							lpWord = pWords[p][w];
							lpWordBox = pWordBox;
							System.out.println(" --> too far apart");
							continue;
						}
						
						//	figure out bold, italic, font size, etc. using weighted majority vote
						float boldWeight = (((float) (((lpWord.bold) ? 1 : 0) * (lpWordBox.right - lpWordBox.left)) + (((pWords[p][w].bold) ? 1 : 0) * (pWordBox.right - pWordBox.left))) / (pWordBox.right - lpWordBox.left));
						float italicsWeight = (((float) (((lpWord.italics) ? 1 : 0) * (lpWordBox.right - lpWordBox.left)) + (((pWords[p][w].italics) ? 1 : 0) * (pWordBox.right - pWordBox.left))) / (pWordBox.right - lpWordBox.left));
						int fontSize = Math.max(lpWord.fontSize, pWords[p][w].fontSize);
						boolean fontHasDescent = (lpWord.fontHasDescent || pWords[p][w].fontHasDescent);
						String fontName = (((lpWordBox.right - lpWordBox.left) < (pWordBox.right - pWordBox.left)) ? pWords[p][w].fontName : lpWord.fontName);
						float top = ((float) Math.max(lpWord.bounds.getMaxY(), pWords[p][w].bounds.getMaxY()));
						float bottom = ((float) Math.min(lpWord.bounds.getMinY(), pWords[p][w].bounds.getMinY()));
						
						//	create merged word
						PWord pw = new PWord((lpWord.str + pWords[p][w].str), new Rectangle2D.Float(((float) lpWord.bounds.getMinX()), bottom, ((float) (pWords[p][w].bounds.getMaxX() - lpWord.bounds.getMinX())), (top - bottom)), fontSize, (boldWeight > 0.5), (italicsWeight > 0.5), fontName, fontHasDescent);
						BoundingBox pwBox = new BoundingBox(lpWordBox.left, pWordBox.right, Math.min(lpWordBox.top, pWordBox.top), Math.max(lpWordBox.bottom, pWordBox.bottom));
						System.out.println(" --> merged words " + lpWord.str + " and " + pWords[p][w].str + " to " + pw.str + " " + pwBox);
						
						//	store merged word
						pWords[p][w] = pw;
						pWords[p][w-1] = null;
						lpWord = pw;
						lpWordBox = pwBox;
						
						//	remember merger
						if (pWordList == null)
							pWordList = new ArrayList();
					}
					
					//	refresh PWord array
					if (pWordList != null) {
						for (int w = 0; w < pWords[p].length; w++) {
							if (pWords[p][w] != null)
								pWordList.add(pWords[p][w]);
						}
						pWords[p] = ((PWord[]) pWordList.toArray(new PWord[pWordList.size()]));
					}
					
					//	convert PDF words into annotations
					pWordAnnots[p] = new ArrayList();
					for (int w = 0; w < pWords[p].length; w++) {
						String wordStr = pWords[p][w].str.trim();
						if (wordStr.length() == 0)
							continue;
						Rectangle2D wb = pWords[p][w].bounds;
						
						//	store PDF word
						Annotation pWordAnnot = Gamta.newAnnotation(dummyTokens, WORD_ANNOTATION_TYPE, 0, 1);
						
						//	set string attribute
						String pWordString = pWords[p][w].str.trim(); // use PWord.str, as IcePDF seems to have more problems with Type3 fonts than we do ...
						pWordAnnot.setAttribute(STRING_ATTRIBUTE, pWordString);
						
						//	convert bounds, as PDF Y coordinate is bottom-up, whereas Java, JavaScript, etc. Y coordinate is top-down
						int left = Math.round(((float) (wb.getMinX() - pageBoxes[p].getMinX())) * magnification);
						int right = Math.round(((float) (wb.getMaxX() - pageBoxes[p].getMinX()))  * magnification);
						int top = Math.round((pageBoxes[p].height - ((float) (wb.getMaxY() - ((2 * pageBoxes[p].getMinY()) - pageBoxes[p].getMaxY())))) * magnification);
						int bottom = Math.round((pageBoxes[p].height - ((float) (wb.getMinY() - ((2 * pageBoxes[p].getMinY()) - pageBoxes[p].getMaxY())))) * magnification);
						
						//	set bounding box
						BoundingBox wordBox = new BoundingBox(left, right, top, bottom);
						pWordAnnot.setAttribute(BOUNDING_BOX_ATTRIBUTE, wordBox.toString());
						
						//	set layout attributes
						if (pWords[p][w].fontSize != -1)
							pWordAnnot.setAttribute(FONT_SIZE_ATTRIBUTE, ("" + pWords[p][w].fontSize));
						if (pWords[p][w].bold)
							pWordAnnot.setAttribute(BOLD_ATTRIBUTE, "true");
						if (pWords[p][w].italics)
							pWordAnnot.setAttribute(ITALICS_ATTRIBUTE, "true");
						
						//	store word
						pWordAnnots[p].add(pWordAnnot);
						System.out.println("PDF word: " + pWordAnnot.toXML());
					}
					synchronized (psm) {
						if (pWordAnnots[p].isEmpty())
							psm.setInfo(" --> empty page");
						else psm.setInfo(" --> got " + pWordAnnots[p].size() + " words in PDF");
					}
				}
				catch (Throwable t) {
					synchronized (error) {
						error[0] = t;
					}
					return;
				}
			}
		}, pageTree.getNumberOfPages());
		psm.setProgress(100);
		
		//	check errors
		if (error[0] != null) {
			if (error[0] instanceof RuntimeException)
				throw ((RuntimeException) error[0]);
			else if (error[0] instanceof Error)
				throw ((Error) error[0]);
			else if (error[0] instanceof IOException)
				throw ((IOException) error[0]);
			else throw new IOException(error[0].getMessage());
		}
		
		//	convert PDF words into annotations
		psm.setStep("Generating page images");
		psm.setBaseProgress(40);
		psm.setProgress(0);
		psm.setMaxProgress(70);
		final BufferedImage[] pageImages = new BufferedImage[pageTree.getNumberOfPages()];
		final String[] imageNames = new String[pageTree.getNumberOfPages()];
		final int[] imageDPIs = new int[pageTree.getNumberOfPages()];
		pageIDs.clear();
		if (PdfExtractorTest.aimAtPage != -1) // TODO remove this after test
			pageIDs.addLast(new Integer(PdfExtractorTest.aimAtPage));
		else for (int p = 0; p < pageTree.getNumberOfPages(); p++)
			pageIDs.addLast(new Integer(p));
		error[0] = null;
		this.runJob(new Runnable() {
			public void run() {
				while (true) try {
					
					//	any error or other problems in siblings?
					synchronized (error) {
						if (error[0] != null)
							return;
					}
					
					//	get next page ID to work off
					int p;
					synchronized (pageIDs) {
						if (pageIDs.isEmpty())
							return;
						p = ((Integer) pageIDs.removeFirst()).intValue();
					}
					
					//	update status display (might be inaccurate, but better than lock escalation)
					synchronized (psm) {
						psm.setInfo("Extracting image of page " + (pageTree.getNumberOfPages() - pageIDs.size()) + " of " + pageTree.getNumberOfPages());
						psm.setProgress(( (pageTree.getNumberOfPages() - pageIDs.size() - 1) * 100) / pageTree.getNumberOfPages());
					}
					
					//	page image generated before
					if (imageStore.isPageImageAvailable(docId, p)) {
						PageImage pi = imageStore.getPageImage(docId, p);
						pageImages[p] = pi.image;
						imageNames[p] = PageImage.getPageImageName(docId, p);
						imageDPIs[p] = pi.currentDpi;
						psm.setInfo(" --> loaded page image generated earlier");
					}
					
					//	generate page image
					else {
						psm.setInfo(" - generating page image");
						pageImages[p] = ((BufferedImage) pdfDoc.getPageImage(p, GraphicsRenderingHints.SCREEN, Page.BOUNDARY_CROPBOX, 0, magnification));
						if (pageImages[p] == null) {
							psm.setInfo(" --> page image generation failed");
							throw new RuntimeException("Could not generate image for page " + p);
						}
						psm.setInfo(" - got page image sized " + pageImages[p].getWidth() + " x " + pageImages[p].getHeight());
						
						//	erase words from IcePDF (can have faulty bounding boxes ...), and paint our own words onto the image
						int cleaningTolerance = (defaultPageImageDpi / 25); // somewhat less than one mm ... 
						Page page = pageTree.getPage(p, "");
						PageText rpt = page.getText();
						ArrayList rpLines = rpt.getPageLines();
						Graphics2D rg = pageImages[p].createGraphics();
						rg.setColor(Color.WHITE);
						for (int l = 0; l < rpLines.size(); l++) {
							LineText lt = ((LineText) rpLines.get(l));
							Rectangle2D.Float wb = lt.getBounds();
							
							//	convert bounds, as PDF Y coordinate is bottom-up, whereas Java, JavaScript, etc. Y coordinate is top-down
							int left = Math.round((wb.x - pageBoxes[p].x) * magnification);
							int right = Math.round(((wb.x - pageBoxes[p].x) + wb.width)  * magnification);
							int top = Math.round((pageBoxes[p].height - ((wb.y - (pageBoxes[p].y - pageBoxes[p].height)) + wb.height)) * magnification);
							int bottom = Math.round((pageBoxes[p].height - (wb.y - (pageBoxes[p].y - pageBoxes[p].height))) * magnification);
							rg.fillRect((left - (cleaningTolerance / 2)), (top - (cleaningTolerance / 2)), (right - left + cleaningTolerance), (bottom - top + cleaningTolerance));
						}
						
						//	clean up black page margins (outmost two pixels)
						rg.fillRect(0, 0, pageImages[p].getWidth(), 2);
						rg.fillRect(0, (pageImages[p].getHeight() - 2), pageImages[p].getWidth(), 2);
						rg.fillRect(0, 0, 2, pageImages[p].getHeight());
						rg.fillRect((pageImages[p].getWidth() - 2), 0, 2, pageImages[p].getHeight());
						
						//	clean whole page
						for (int w = 0; w < pWords[p].length; w++) {
							Rectangle2D wb = pWords[p][w].bounds;
							
							//	convert bounds, as PDF Y coordinate is bottom-up, whereas Java, JavaScript, etc. Y coordinate is top-down
							int left = Math.round(((float) (wb.getMinX() - pageBoxes[p].getMinX())) * magnification);
							int right = Math.round(((float) (wb.getMaxX() - pageBoxes[p].getMinX()))  * magnification);
							int top = Math.round((pageBoxes[p].height - ((float) (wb.getMaxY() - ((2 * pageBoxes[p].getMinY()) - pageBoxes[p].getMaxY())))) * magnification);
							int bottom = Math.round((pageBoxes[p].height - ((float) (wb.getMinY() - ((2 * pageBoxes[p].getMinY()) - pageBoxes[p].getMaxY())))) * magnification);
							
							//	clean background
							rg.setColor(Color.WHITE);
							rg.fillRect(left, top, (right - left + 1), (bottom - top));
						}
						
						//	paint own words
						for (int w = 0; w < pWords[p].length; w++) {
							Rectangle2D wb = pWords[p][w].bounds;
							
							//	convert bounds, as PDF Y coordinate is bottom-up, whereas Java, JavaScript, etc. Y coordinate is top-down
							int left = Math.round(((float) (wb.getMinX() - pageBoxes[p].getMinX())) * magnification);
							int right = Math.round(((float) (wb.getMaxX() - pageBoxes[p].getMinX()))  * magnification);
//							int top = Math.round((pageBox.height - ((float) (wb.getMaxY() - ((2 * pageBox.getMinY()) - pageBox.getMaxY())))) * magnification);
							int bottom = Math.round((pageBoxes[p].height - ((float) (wb.getMinY() - ((2 * pageBoxes[p].getMinY()) - pageBoxes[p].getMaxY())))) * magnification);
							
							//	prepare font
							rg.setColor(Color.BLACK);
							int fontStyle = Font.PLAIN;
							if (pWords[p][w].bold)
								fontStyle = (fontStyle | Font.BOLD);
							if (pWords[p][w].italics)
								fontStyle = (fontStyle | Font.ITALIC);
							Font rf = getFont(pWords[p][w].fontName, fontStyle, Math.round(((float) pWords[p][w].fontSize) * magnification));
							rg.setFont(rf);
							
							//	adjust word size and vertical position
							LineMetrics wlm = rf.getLineMetrics(pWords[p][w].str, rg.getFontRenderContext());
							TextLayout wtl = new TextLayout(pWords[p][w].str, rf, rg.getFontRenderContext());
							double hScale = (((double) (right - left)) / wtl.getBounds().getWidth());
							if (hScale < 1) {
								AffineTransform at = rg.getTransform();
								rg.scale(hScale, 1);
								rg.drawString(pWords[p][w].str, ((int) Math.round(((double) left) / hScale)), (bottom - (pWords[p][w].fontHasDescent ? Math.round(wlm.getDescent()) : 0)));
								rg.setTransform(at);
							}
							else rg.drawString(pWords[p][w].str, left, (bottom - (pWords[p][w].fontHasDescent ? Math.round(wlm.getDescent()) : 0)));
						}
						
						AnalysisImage ai = Imaging.wrapImage(pageImages[p], (PageImage.getPageImageName(docId, p) + defaultPageImageDpi));
						Imaging.whitenWhite(ai);
						pageImages[p] = ai.getImage();
						//	cannot cut margins here because otherwise PDF word assignment gets broken
						imageNames[p] = imageStore.storePageImage(docId, p, pageImages[p], defaultPageImageDpi);
						if (imageNames[p] == null) {
							synchronized (psm) {
								psm.setInfo(" --> page image storage failed");
							}
							throw new RuntimeException("Could not store image of page " + p);
						}
						else {
							synchronized (psm) {
								psm.setInfo(" --> page image stored");
							}
							imageDPIs[p] = defaultPageImageDpi;
						}
					}
				}
				catch (Throwable t) {
					synchronized (error) {
						error[0] = t;
					}
					return;
				}
			}
		}, pageTree.getNumberOfPages());
		psm.setProgress(100);
		
		//	check errors
		if (error[0] != null) {
			if (error[0] instanceof RuntimeException)
				throw ((RuntimeException) error[0]);
			else if (error[0] instanceof Error)
				throw ((Error) error[0]);
			else if (error[0] instanceof IOException)
				throw ((IOException) error[0]);
			else throw new IOException(error[0].getMessage());
		}
		
		//	analyze page structure
		psm.setStep("Analyzing page structure");
		psm.setBaseProgress(70);
		psm.setProgress(0);
		psm.setMaxProgress(90);
		final DocumentRoot[] pages = new DocumentRoot[pageTree.getNumberOfPages()];
		pageIDs.clear();
		if (PdfExtractorTest.aimAtPage != -1) // TODO remove this after test
			pageIDs.addLast(new Integer(PdfExtractorTest.aimAtPage));
		else for (int p = 0; p < pageTree.getNumberOfPages(); p++)
			pageIDs.addLast(new Integer(p));
		error[0] = null;
		this.runJob(new Runnable() {
			public void run() {
				while (true) try {
					
					//	any error or other problems in siblings?
					synchronized (error) {
						if (error[0] != null)
							return;
					}
					
					//	get next page ID to work off
					int p;
					synchronized (pageIDs) {
						if (pageIDs.isEmpty())
							return;
						p = ((Integer) pageIDs.removeFirst()).intValue();
					}
					
					//	generate page document
					pages[p] = Gamta.newDocument(doc.getTokenizer());
					
					//	clean bounding boxes of PDF words - might overlap with several lines in page image ...
					AnalysisImage api = Imaging.wrapImage(pageImages[p], (imageNames[p] + imageDPIs[p]));
					HashMap pWordsByBoxes = new HashMap();
					for (int w = 0; w < pWordAnnots[p].size(); w++) {
						Annotation pWordAnnot = ((Annotation) pWordAnnots[p].get(w));
						BoundingBox wbb = BoundingBox.getBoundingBox(pWordAnnot);
						pWordsByBoxes.put(wbb, pWordAnnot);
					}
					
					//	obtain visual page structure (use smaller-than-usual word margin, as merge will happen automatically further down the road for in-word splits
					Region pageRootRegion = PageImageAnalysis.getPageRegion(api, imageDPIs[p], psm);
					
					//	add page content to document
					ArrayList emptyWords = new ArrayList();
					appendRegionStructure(pages[p], pageRootRegion, imageDPIs[p], pWordsByBoxes, emptyWords, psm);
					
					//	catch empty page
					if (pages[p].size() == 0)
						continue;
					
					//	adjust bounding boxes
					shrinkToChildren(pages[p], LINE_ANNOTATION_TYPE, WORD_ANNOTATION_TYPE);
					shrinkToChildren(pages[p], BLOCK_ANNOTATION_TYPE, LINE_ANNOTATION_TYPE);
					
					//	do structure analysis
					PageAnalysis.splitIntoParagraphs(pages[p].getMutableAnnotations(BLOCK_ANNOTATION_TYPE), imageDPIs[p], null);
					synchronized (psm) {
						psm.setInfo(" - paragraphs done");
					}
					PageAnalysis.computeColumnLayout(pages[p].getMutableAnnotations(COLUMN_ANNOTATION_TYPE), imageDPIs[p]);
					synchronized (psm) {
						psm.setInfo(" - layout analysis done");
					}
					
					//	finally ...
					synchronized (psm) {
						psm.setInfo(" - page done");
					}
				}
				catch (Throwable t) {
					synchronized (error) {
						error[0] = t;
					}
					return;
				}
			}
		}, pageTree.getNumberOfPages());
		psm.setProgress(100);
		
		//	copy pages into main document
		psm.setBaseProgress(95);
		psm.setProgress(0);
		psm.setMaxProgress(100);
		int pageStart;
		for (int p = 0; p < pages.length; p++) {
			psm.setProgress((100 * p) / pages.length);
			pageStart = doc.size();
			
			//	add page tokens
			if ((pages[p] == null) || (pages[p].size() == 0))
				doc.addTokens("P");
			else doc.addTokens(pages[p]);
			
			//	annotate page
			MutableAnnotation pageAnnot = doc.addAnnotation(PAGE_TYPE, pageStart, (doc.size() - pageStart));
			pageAnnot.setAttribute(PAGE_ID_ATTRIBUTE, ("" + p));
			pageAnnot.setAttribute(IMAGE_NAME_ATTRIBUTE, imageNames[p]);
			pageAnnot.setAttribute(IMAGE_DPI_ATTRIBUTE, ("" + imageDPIs[p]));
			
			//	catch empty pages
			if ((pages[p] == null) || (pages[p].size() == 0))
				continue;
			
			//	transfer annotations
			Annotation[] pageAnnots = pages[p].getAnnotations();
			for (int a = 0; a < pageAnnots.length; a++) {
				if (DocumentRoot.DOCUMENT_TYPE.equals(pageAnnots[a].getType()))
					continue;
				if (PAGE_TYPE.equals(pageAnnots[a].getType()))
					continue;
				Annotation docAnnot = pageAnnot.addAnnotation(pageAnnots[a].getType(), pageAnnots[a].getStartIndex(), pageAnnots[a].size());
				docAnnot.copyAttributes(pageAnnots[a]);
			}
		}
		psm.setProgress(100);
		
		//	check plausibility
		if (((doc.size() / pageTree.getNumberOfPages()) < minAveragePageWords) && (PdfExtractorTest.aimAtPage == -1))
			throw new IOException("Too few words per page (less than " + minAveragePageWords + ")");
		
		//	finally ...
		return doc;
	}
	
	private static HashMap fontCache = new HashMap(5);
	private static Font getFont(String name, int style, int size) {
		String fontKey = (name + " " + style + " " + size);
		System.out.println("Getting font " + fontKey);
		Font font = ((Font) fontCache.get(fontKey));
		if (font != null) {
			System.out.println("==> cache hit");
			return font;
		}
		
		if (name != null) {
			if (name.matches("[A-Z]+\\+.*"))
				name = name.substring(name.indexOf('+')+1);
			if (name.startsWith("Symbol"))
				font = new Font("Symbol", style, size);
			else if (name.startsWith("ZapfDingbats"))
				font = new Font("ZapfDingbats", style, size);
			
			if (name.indexOf('-') != -1)
				name = name.substring(0, name.indexOf('-'));
			String ffn = PdfFont.getFallbackFontName(name, false);
			System.out.println("==> falling back to " + ffn);
			if (ffn.startsWith("Helvetica"))
				font = new Font("Helvetica", style, size);
			else if (ffn.startsWith("Times"))
				font = new Font("Times New Roman", style, size);
			else if (ffn.startsWith("Courier"))
				font = new Font("Courier New", style, size);
		}
		
		if (font == null) {
			System.out.println("==> base font not found, using Serif fallback");
			return new Font("Serif", style, size);
		}
		fontCache.put(fontKey, font);
		System.out.println("==> font created");
		return font;
	}
	
	private int appendRegionStructure(MutableAnnotation doc, Region region, int dpi, HashMap pWordsByBoxes, ArrayList emptyWords, ProgressMonitor psm) {
		int wordCount = 0;
		
		//	atomic region ==> block, do further analysis
		if (region.isAtomic()) {
			
			//	analyze block structure
			PageImageAnalysis.getBlockStructure(region, dpi, ((BoundingBox[]) pWordsByBoxes.keySet().toArray(new BoundingBox[pWordsByBoxes.size()])), psm);
			Block block = region.getBlock();
			
			//	append block content
			int blockStart = doc.size();
			wordCount = this.appendTextBlockStructure(doc, block, dpi, pWordsByBoxes, emptyWords, psm);
			
			//	catch empty page
			if (blockStart == doc.size())
				doc.addTokens("B");
			
			//	annotate block
			Annotation blockAnnot = doc.addAnnotation((block.isTable() ? TABLE_ANNOTATION_TYPE : BLOCK_ANNOTATION_TYPE), blockStart, (doc.size() - blockStart));
			BoundingBox blockBox = block.getBoundingBox();
			blockAnnot.setAttribute(BOUNDING_BOX_ATTRIBUTE, blockBox.toString());
			
			//	finally ...
			return wordCount;
		}
		
		//	non-atomic region ==> recurse to child regions
		int blockStart = doc.size();
		for (int s = 0; s < region.getSubRegionCount(); s++)
			wordCount += this.appendRegionStructure(doc, region.getSubRegion(s), dpi, pWordsByBoxes, emptyWords, psm);
		
		//	check if any words inside block left unassigned
		BoundingBox blockBox = region.getBoundingBox();
		ArrayList lostBlockPWordBoxes = new ArrayList();
		for (Iterator bbit = pWordsByBoxes.keySet().iterator(); bbit.hasNext();) {
			BoundingBox wbb = ((BoundingBox) bbit.next());
			if (wbb.bottom <= blockBox.top)
				continue;
			else if (blockBox.bottom <= wbb.top)
				continue;
			else if (wbb.right <= blockBox.left)
				continue;
			else if (blockBox.right <= wbb.left)
				continue;
			
			int wbbMiddle = ((wbb.top + wbb.bottom) / 2);
			if (wbbMiddle <= blockBox.top)
				continue;
			else if (blockBox.bottom <= wbbMiddle)
				continue;
			
			int wbbCenter = ((wbb.left + wbb.right) / 2);
			if (wbbCenter <= blockBox.left)
				continue;
			else if (blockBox.right <= wbbCenter)
				continue;
			
			lostBlockPWordBoxes.add(wbb);
		}
		
		//	assign lost words
		if (lostBlockPWordBoxes.size() != 0) {
			psm.setInfo(" --> got " + pWordsByBoxes.size() + " PDF words left unassigned in block " + blockBox.toString() + ":");
			for (Iterator bbit = lostBlockPWordBoxes.iterator(); bbit.hasNext();) {
				BoundingBox lostWordBox = ((BoundingBox) bbit.next());
				Annotation lostWord = ((Annotation) pWordsByBoxes.remove(lostWordBox));
				
				//	add word
				doc.addTokens("W");
				Annotation lostWordAnnot = doc.addAnnotation(WORD_ANNOTATION_TYPE, (doc.size() - 1), 1);
				lostWordAnnot.copyAttributes(lostWord);
				psm.setInfo("   - assigned " + lostWordAnnot.toXML());
				
				//	annotate line
				Annotation lostLineAnnot = doc.addAnnotation(LINE_ANNOTATION_TYPE, (doc.size() - 1), 1);
				AttributeUtils.copyAttributes(lostWord, lostLineAnnot, lostLineAttributes);
				
				//	annotate block
				Annotation lostBlockAnnot = doc.addAnnotation(BLOCK_ANNOTATION_TYPE, (doc.size() - 1), 1);
				AttributeUtils.copyAttributes(lostWordAnnot, lostBlockAnnot, lostBlockAttributes);
							
				//	remember using word
				wordCount++;
			}
		}
		
		//	annotate block
		Annotation regionAnnot = doc.addAnnotation(((region.isColumn() && region.areChildrenAtomic()) ? COLUMN_ANNOTATION_TYPE : REGION_ANNOTATION_TYPE), blockStart, (doc.size() - blockStart));
		BoundingBox regionBox = region.getBoundingBox();
		regionAnnot.setAttribute(BOUNDING_BOX_ATTRIBUTE, regionBox.toString());
		
		//	finally ...
		return wordCount;
	}
	
	private static final Set lostBlockAttributes = new HashSet() {
		public boolean contains(Object o) {
			return (BOUNDING_BOX_ATTRIBUTE.equals(o) || FONT_SIZE_ATTRIBUTE.equals(o));
		}
	};
	
	private static final Set lostLineAttributes = new HashSet() {
		public boolean contains(Object o) {
			return (BOUNDING_BOX_ATTRIBUTE.equals(o) || FONT_SIZE_ATTRIBUTE.equals(o) || BASELINE_ATTRIBUTE.equals(o));
		}
	};
	
	private int appendTextBlockStructure(MutableAnnotation doc, Block theBlock, int dpi, HashMap pWordsByBoxes, ArrayList emptyWords, ProgressMonitor psm) {
		
		//	catch empty blocks
		if (theBlock.isEmpty())
			return 0;
		
		//	assemble document from structure
		int wordCount = 0;
		
		//	lines wrapped in table cells
		if (theBlock.isTable()) {
			int rowStart;
			int cellStart;
			TableRow[] rows = theBlock.getRows();
			for (int r = 0; r < rows.length; r++) {
				rowStart = doc.size();
				TableCell[] cells = rows[r].getCells();
				for (int c = 0; c < cells.length; c++) {
					cellStart = doc.size();
					Line[] lines = cells[c].getLines();
					this.addLines(doc, cells[c].bounds, lines, pWordsByBoxes, emptyWords, psm);
					
					//	catch empty cells, they are required to keep the table grid in shape
					if (cellStart == doc.size())
						doc.addTokens(" " + EMPTY_CELL_FILLER + " ");
					
					//	annotate cell
					Annotation cellAnnot = doc.addAnnotation(TABLE_CELL_ANNOTATION_TYPE, cellStart, (doc.size() - cellStart));
					cellAnnot.setAttribute(BOUNDING_BOX_ATTRIBUTE, cells[c].getBoundingBox().toString());
					int colSpan = cells[c].getColSpan();
					if (colSpan > 1)
						cellAnnot.setAttribute(COL_SPAN_ATTRIBUTE, ("" + colSpan));
					int rowSpan = cells[c].getRowSpan();
					if (rowSpan > 1)
						cellAnnot.setAttribute(ROW_SPAN_ATTRIBUTE, ("" + rowSpan));
				}
				
				//	we can ignore empty rows, as they do not contain any cells
				if (rowStart == doc.size())
					continue;
				
				//	annotate row
				Annotation rowAnnot = doc.addAnnotation(TABLE_ROW_ANNOTATION_TYPE, rowStart, (doc.size() - rowStart));
				rowAnnot.setAttribute(BOUNDING_BOX_ATTRIBUTE, rows[r].getBoundingBox().toString());
			}
		}
		
		//	regular text lines
		else {
			Line[] lines = theBlock.getLines();
			this.addLines(doc, theBlock.bounds, lines, pWordsByBoxes, emptyWords, psm);
		}
		
		//	finally ...
		return wordCount;
	}
	
	private void addLines(MutableAnnotation doc, ImagePartRectangle blockBounds, Line[] lines, HashMap pWordsByBoxes, ArrayList emptyWords, ProgressMonitor psm) {
		if (lines.length == 0)
			return;
		
		//	compute average line height
		int lineHeightSum = 0;
		for (int l = 0; l < lines.length; l++)
			lineHeightSum += (lines[l].bounds.getBottomRow() - lines[l].bounds.getTopRow());
		int avgLineHeight = (lineHeightSum / lines.length);
		
		//	compute line bounds from PDF words
		int[] lineTop = new int[lines.length];
		Arrays.fill(lineTop, blockBounds.getBottomRow());
		int[] lineBottom = new int[lines.length];
		Arrays.fill(lineBottom, blockBounds.getTopRow());
		for (int l = 0; l < lines.length; l++) {
			BoundingBox lineBox = lines[l].getBoundingBox();
			for (Iterator bbit = pWordsByBoxes.keySet().iterator(); bbit.hasNext();) {
				BoundingBox wbb = ((BoundingBox) bbit.next());
				if (wbb.bottom <= lineBox.top)
					continue;
				else if (lineBox.bottom <= wbb.top)
					continue;
				else if (wbb.right <= lineBox.left)
					continue;
				else if (lineBox.right <= wbb.left)
					continue;
				
				int wbbMiddle = ((wbb.top + wbb.bottom) / 2);
				if (wbbMiddle <= lineBox.top)
					continue;
				else if (lineBox.bottom <= wbbMiddle)
					continue;
				
				int wbbCenter = ((wbb.left + wbb.right) / 2);
				if (wbbCenter <= lineBox.left)
					continue;
				else if (lineBox.right <= wbbCenter)
					continue;
				
				lineTop[l] = Math.min(lineTop[l], wbb.top);
				lineBottom[l] = Math.max(lineBottom[l], wbb.bottom);
			}
		}
		
		//	check all lines for possibly being super/sub-script
		byte[] linePosition = new byte[lines.length];
		Arrays.fill(linePosition, ((byte) 0));
		for (int l = 0; l < lines.length; l++) {
			
			//	above average height lines are very unlikely to be super/sub script
			if (avgLineHeight <= (lineBottom[l] - lineTop[l]))
				continue;
			
//			System.out.println("Checking line for super/sub script: " + lines[l].getBoundingBox());
//			System.out.println(" - height is " + (lineBottom[l] - lineTop[l]));
			
			//	check superscript if line is less than 70% of the one below
//			System.out.println(" - height below is " + (((l+1) < lines.length) ? (lineBottom[l+1] - lineTop[l+1]) : -1));
			int downwardOverlap = 0;
			if (((l+1) < lines.length) && (((lineBottom[l+1] - lineTop[l+1]) * 7) > ((lineBottom[l] - lineTop[l]) * 10)))
				downwardOverlap = Math.max(0, (lineBottom[l] - lineTop[l+1]));
//			System.out.println(" - downward overlap is " + downwardOverlap);
			
			//	check subscript if line is less than 70% of the one above
//			System.out.println(" - height above is " + ((l != 0) ? (lineBottom[l-1] - lineTop[l-1]) : -1));
			int upwardOverlap = 0;
			if ((l != 0) && (((lineBottom[l-1] - lineTop[l-1]) * 7) > ((lineBottom[l] - lineTop[l]) * 10)))
				upwardOverlap = Math.max(0, (lineBottom[l-1] - lineTop[l]));
//			System.out.println(" - upward overlap is " + upwardOverlap);
			
			//	no overlap at all
			if ((upwardOverlap + downwardOverlap) == 0)
				continue;
			
			//	mark where words are
			boolean[] isWord = new boolean[blockBounds.getRightCol() - blockBounds.getLeftCol()];
			Arrays.fill(isWord, false);
			Word[] words = lines[l].getWords();
			for (int w = 0; w < words.length; w++) {
				for (int x = Math.max(words[w].bounds.getLeftCol(), blockBounds.getLeftCol()); x < Math.min(words[w].bounds.getRightCol(), blockBounds.getRightCol()); x++)
					isWord[x - blockBounds.getLeftCol()] = true;
			}
			
			//	check superscript conflicts
			if (downwardOverlap != 0) {
				Word[] wordsBelow = lines[l+1].getWords();
				for (int w = 0; w < wordsBelow.length; w++) {
					int overlappingCols = 0;
					for (int x = Math.max(wordsBelow[w].bounds.getLeftCol(), blockBounds.getLeftCol()); x < Math.min(wordsBelow[w].bounds.getRightCol(), blockBounds.getRightCol()); x++) {
						if (isWord[x - blockBounds.getLeftCol()])
							overlappingCols++;
					}
					if (overlappingCols > 1) {
						downwardOverlap = 0;
//						System.out.println(" - words in conflict with line below");
						break;
					}
				}
			}
			
			//	check superscript conflicts
			if (upwardOverlap != 0) {
				Word[] wordsAbove = lines[l-1].getWords();
				for (int w = 0; w < wordsAbove.length; w++) {
					int overlappingCols = 0;
					for (int x = Math.max(wordsAbove[w].bounds.getLeftCol(), blockBounds.getLeftCol()); x < Math.min(wordsAbove[w].bounds.getRightCol(), blockBounds.getRightCol()); x++) {
						if (isWord[x - blockBounds.getLeftCol()])
							overlappingCols++;
					}
					if (overlappingCols > 1) {
						upwardOverlap = 0;
//						System.out.println(" - words in conflict with line above");
						break;
					}
				}
			}
			
			//	conflicts in all overlapping directions
			if ((upwardOverlap + downwardOverlap) == 0) {
//				System.out.println(" ==> no overlap at all");
				continue;
			}
			
			//	we cannot decide on this one
			if ((upwardOverlap * downwardOverlap) != 0) {
//				System.out.println(" ==> overlap both ways");
				continue;
			}
			
			//	mark super or subscript
			linePosition[l] = ((upwardOverlap < downwardOverlap) ? ((byte) 1) : ((byte) -1));
//			System.out.println(" ==> line in " + ((upwardOverlap < downwardOverlap) ? "super" : "sub") + "script");
		}
		
		//	add lines
		for (int l = 0; l < lines.length; l++) {
			if (linePosition[l] != 0)
				continue;
			
			int lineStart = doc.size();
			BoundingBox lineBox = lines[l].getBoundingBox();
			
			//	collect words in line
			ArrayList lpWordBoxes = new ArrayList();
			for (Iterator bbit = pWordsByBoxes.keySet().iterator(); bbit.hasNext();) {
				BoundingBox wbb = ((BoundingBox) bbit.next());
				if (wbb.bottom <= lineBox.top)
					continue;
				else if (lineBox.bottom <= wbb.top)
					continue;
				else if (wbb.right <= lineBox.left)
					continue;
				else if (lineBox.right <= wbb.left)
					continue;
				
				int wbbMiddle = ((wbb.top + wbb.bottom) / 2);
				if (wbbMiddle <= lineBox.top)
					continue;
				else if (lineBox.bottom <= wbbMiddle)
					continue;
				
				int wbbCenter = ((wbb.left + wbb.right) / 2);
				if (wbbCenter <= lineBox.left)
					continue;
				else if (lineBox.right <= wbbCenter)
					continue;
				
				lpWordBoxes.add(wbb);
			}
			
			//	get superscript lines first and collect words inside them
			Line superScriptLine = (((l != 0) && (linePosition[l-1] > 0)) ? lines[l-1] : null);
			if (superScriptLine != null) {
				BoundingBox sLineBox = superScriptLine.getBoundingBox();
//				System.out.println("Got superscript line with box " + sLineBox);
				for (Iterator bbit = pWordsByBoxes.keySet().iterator(); bbit.hasNext();) {
					BoundingBox wbb = ((BoundingBox) bbit.next());
					if (wbb.bottom <= sLineBox.top)
						continue;
					else if (sLineBox.bottom <= wbb.top)
						continue;
					else if (wbb.right <= sLineBox.left)
						continue;
					else if (sLineBox.right <= wbb.left)
						continue;
					
					int wbbMiddle = ((wbb.top + wbb.bottom) / 2);
					if (wbbMiddle <= sLineBox.top)
						continue;
					else if (sLineBox.bottom <= wbbMiddle)
						continue;
					
					int wbbCenter = ((wbb.left + wbb.right) / 2);
					if (wbbCenter <= sLineBox.left)
						continue;
					else if (sLineBox.right <= wbbCenter)
						continue;
					
					lpWordBoxes.add(wbb);
				}
			}
			
			//	get subscript lines first and collect words inside them
			Line subScriptLine = ((((l+1) < lines.length) && (linePosition[l+1] < 0)) ? lines[l+1] : null);
			if (subScriptLine != null) {
				BoundingBox sLineBox = subScriptLine.getBoundingBox();
//				System.out.println("Got subscript line with box " + sLineBox);
				for (Iterator bbit = pWordsByBoxes.keySet().iterator(); bbit.hasNext();) {
					BoundingBox wbb = ((BoundingBox) bbit.next());
					if (wbb.bottom <= sLineBox.top)
						continue;
					else if (sLineBox.bottom <= wbb.top)
						continue;
					else if (wbb.right <= sLineBox.left)
						continue;
					else if (sLineBox.right <= wbb.left)
						continue;
					
					int wbbMiddle = ((wbb.top + wbb.bottom) / 2);
					if (wbbMiddle <= sLineBox.top)
						continue;
					else if (sLineBox.bottom <= wbbMiddle)
						continue;
					
					int wbbCenter = ((wbb.left + wbb.right) / 2);
					if (wbbCenter <= sLineBox.left)
						continue;
					else if (sLineBox.right <= wbbCenter)
						continue;
					
					lpWordBoxes.add(wbb);
				}
			}
			
			psm.setInfo(" - got " + lpWordBoxes.size() + " PDF words in line " + l);
			if (lpWordBoxes.isEmpty())
				continue;
			
			Collections.sort(lpWordBoxes, new Comparator() {
				public int compare(Object bbo1, Object bbo2) {
					BoundingBox bb1 = ((BoundingBox) bbo1);
					BoundingBox bb2 = ((BoundingBox) bbo2);
					return ((bb1.left == bb2.left) ? (bb1.right - bb2.right) : (bb1.left - bb2.left));
				}
			});
			
			int fontSizeSum = 0;
			int fontSizeCount = 0;
			
			Word[] words = lines[l].getWords();
			psm.setInfo(" - got " + words.length + " original layout words");
			if ((superScriptLine != null) || (subScriptLine != null)) {
				ArrayList wordList = new ArrayList(Arrays.asList(words));
				if (superScriptLine != null)
					wordList.addAll(Arrays.asList(superScriptLine.getWords()));
				if (subScriptLine != null)
					wordList.addAll(Arrays.asList(subScriptLine.getWords()));
				Collections.sort(wordList, new Comparator() {
					public int compare(Object w1, Object w2) {
						ImagePartRectangle bb1 = ((Word) w1).bounds;
						ImagePartRectangle bb2 = ((Word) w2).bounds;
						return ((bb1.getLeftCol() == bb2.getLeftCol()) ? (bb1.getRightCol() - bb2.getRightCol()) : (bb1.getLeftCol() - bb2.getLeftCol()));
					}
				});
				words = ((Word[]) wordList.toArray(new Word[wordList.size()]));
			}
			
			psm.setInfo(" - got " + words.length + " extended layout words");
			for (int w = 0; w < words.length; w++) {
				
				//	gather strings and layout information
				StringBuffer str = new StringBuffer();
				boolean bold = false;
				boolean italics = false;
				for (int lw = 0; lw < lpWordBoxes.size(); lw++) {
					BoundingBox lWordBox = ((BoundingBox) lpWordBoxes.get(lw));
					if (words[w].bounds.getRightCol() <= lWordBox.left)
						break;
					
					//	get PDF word
					lpWordBoxes.remove(lw--);
					Annotation lWord = ((Annotation) pWordsByBoxes.remove(lWordBox));
					if (lWord == null)
						continue;
					
					//	append PDF word
					String pWordString = ((String) lWord.getAttribute(STRING_ATTRIBUTE, ""));
					str.append(pWordString);
					
					//	aggregate layout information
					bold = (bold || lWord.hasAttribute(BOLD_ATTRIBUTE));
					italics = (italics || lWord.hasAttribute(ITALICS_ATTRIBUTE));
					if (lWord.hasAttribute(FONT_SIZE_ATTRIBUTE)) try {
						int pwfs = Integer.parseInt((String) lWord.getAttribute(FONT_SIZE_ATTRIBUTE));
						fontSizeSum += pwfs;
						fontSizeCount++;
					} catch (NumberFormatException nfe) {}
				}
				
				//	add word
				doc.addTokens("W");
				Annotation wordAnnot = doc.addAnnotation(WORD_ANNOTATION_TYPE, (doc.size() - 1), 1);
				BoundingBox wordBox = words[w].getBoundingBox();
				if ((wordBox.top < lineTop[l]) && (wordBox.bottom < lineBottom[l]))
					wordBox = new BoundingBox(wordBox.left, wordBox.right, lineTop[l], Math.min(lineBottom[l], (lineTop[l] + (wordBox.bottom - wordBox.top))));
				if ((lineBottom[l] < wordBox.bottom) && (lineTop[l] < wordBox.top))
					wordBox = new BoundingBox(wordBox.left, wordBox.right, Math.max(lineTop[l], (lineBottom[l] - (wordBox.bottom - wordBox.top))), lineBottom[l]);
				
				wordAnnot.setAttribute(BOUNDING_BOX_ATTRIBUTE, wordBox.toString());
				int baseline = words[w].getBaseline();
				if (0 < baseline)
					wordAnnot.setAttribute(BASELINE_ATTRIBUTE, ("" + baseline));
				
				//	set layout attributes
				if (bold)
					wordAnnot.setAttribute(BOLD_ATTRIBUTE, "true");
				if (italics)
					wordAnnot.setAttribute(ITALICS_ATTRIBUTE, "true");
				
				//	we have a string
				if (str.length() != 0) {
					wordAnnot.setAttribute(STRING_ATTRIBUTE, str.toString());
					psm.setInfo("   - added word: " + wordAnnot.toXML());
				}
				
				//	collect empty words for dealing with later (if desired)
				else {
					emptyWords.add(wordAnnot);
					psm.setInfo("   - got empty word: " + wordAnnot.toXML());
				}
			}
			if (lpWordBoxes.isEmpty())
				psm.setInfo(" - got " + lpWordBoxes.size() + " PDF words remaining after line");
			else {
				psm.setInfo("Not good, got " + lpWordBoxes.size() + " PDF words remaining after line:");
				psm.setInfo(" - line bounds are " + lines[l].getBoundingBox());
				psm.setInfo(" - layout words:");
				for (int w = 0; w < words.length; w++)
					psm.setInfo("   - " + words[w].getBoundingBox());
				psm.setInfo(" - remaining words:");
				for (int lw = 0; lw < lpWordBoxes.size(); lw++) {
					BoundingBox lWordBox = ((BoundingBox) lpWordBoxes.get(lw));
					Annotation lWord = ((Annotation) pWordsByBoxes.remove(lWordBox));
					psm.setInfo("   - " + lWord.toXML());
				}
			}
			
			//	catch empty lines
			if (lineStart == doc.size())
				continue;
			
			//	annotate line
			Annotation lineAnnot = doc.addAnnotation(LINE_ANNOTATION_TYPE, lineStart, (doc.size() - lineStart));
			lineAnnot.setAttribute(BOUNDING_BOX_ATTRIBUTE, lineBox.toString());
			int baseline = lines[l].getBaseline();
			if (0 < baseline)
				lineAnnot.setAttribute(BASELINE_ATTRIBUTE, ("" + baseline));
			if (0 < fontSizeSum) {
				int fontSize = ((fontSizeSum + (fontSizeCount / 2)) / fontSizeCount);
				if (0 < fontSize)
					lineAnnot.setAttribute(FONT_SIZE_ATTRIBUTE, ("" + fontSize));
			}
			else {
				int fontSize = lines[l].getFontSize();
				if (0 < fontSize)
					lineAnnot.setAttribute(FONT_SIZE_ATTRIBUTE, ("" + fontSize));
			}
		}
	}
	
	private static void shrinkToChildren(MutableAnnotation data, String pType, String cType) {
		QueriableAnnotation[] pAnnots = data.getAnnotations(pType);
		for (int p = 0; p < pAnnots.length; p++) {
			Annotation[] cAnnots = pAnnots[p].getAnnotations(cType);
			if (cAnnots.length == 0)
				continue;
			int pLeft = Integer.MAX_VALUE;
			int pRight = 0;
			int pTop = Integer.MAX_VALUE;
			int pBottom = 0;
			for (int c = 0; c < cAnnots.length; c++) {
				BoundingBox cBox = BoundingBox.getBoundingBox(cAnnots[c]);
				if (cBox == null)
					continue;
				pLeft = Math.min(pLeft, cBox.left);
				pRight = Math.max(pRight, cBox.right);
				pTop = Math.min(pTop, cBox.top);
				pBottom = Math.max(pBottom, cBox.bottom);
			}
			if ((pLeft < pRight) && (pTop < pBottom)) {
				BoundingBox pBox = new BoundingBox(pLeft, pRight, pTop, pBottom);
				pAnnots[p].setAttribute(BOUNDING_BOX_ATTRIBUTE, pBox.toString());
			}
		}
	}
	
	/**
	 * Load a document from an OCR PDF. Page images are stored in the page image
	 * store handed to the constructor. The document structure is stored in the
	 * argument GAMTA document, including word bounding boxes.
	 * @param doc the document to store the structural information in
	 * @param pdfDoc the PDF document to parse
	 * @param bytes the raw binary data the the PDF document was parsed from
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @return the argument GAMTA document
	 * @throws IOException
	 */
	public DocumentRoot loadImagePdf(DocumentRoot doc, Document pdfDoc, byte[] bytes, ProgressMonitor psm) throws IOException {
		return this.loadImagePdf(doc, pdfDoc, bytes, 1, psm);
	}
	
	/**
	 * Load a document from an OCR PDF. Page images are stored in the page image
	 * store handed to the constructor. The document structure is stored in the
	 * argument GAMTA document, including word bounding boxes. The scale factor
	 * helps with PDFs whose page boundaries (media boxes) are set too small.
	 * This can be recognized in any PDF viewer by the fact that the document is
	 * tiny and the writing very small at 100% size display. It can also be
	 * detected automatically, e.g. by means of examining the average height of
	 * lines in comparison to the DPI number.
	 * @param doc the document to store the structural information in
	 * @param pdfDoc the PDF document to parse
	 * @param bytes the raw binary data the the PDF document was parsed from
	 * @param scaleFactor the scale factor for image PDFs, to correct PDFs that
	 *            are set too small (DPI number is divided by this factor)
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @return the argument GAMTA document
	 * @throws IOException
	 */
	public DocumentRoot loadImagePdf(DocumentRoot doc, Document pdfDoc, byte[] bytes, int scaleFactor, ProgressMonitor psm) throws IOException {
		if (psm == null)
			psm = ProgressMonitor.dummy;
		
		//	test if we can OCR
		if (this.ocrEngine == null)
			throw new RuntimeException("OCR Engine unavailable, check startup log.");
		
		//	prepare document
		String docId = ((String) doc.getAttribute(DOCUMENT_ID_ATTRIBUTE));
		if (docId == null) {
			docId = doc.getAnnotationID();
			doc.setAttribute(DOCUMENT_ID_ATTRIBUTE, docId);
		}
		
		//	do it
		return this.doLoadImagePdf(doc, docId, pdfDoc, bytes, scaleFactor, psm);
	}
	
	private DocumentRoot doLoadImagePdf(DocumentRoot doc, final String docId, Document pdfDoc, byte[] bytes, int scaleFactor, final ProgressMonitor psm) throws IOException {
		
		//	cache page images (only binary and gray scale, and only up to 300 DPI, as memory consumption gets too high beyond that)
		final HashMap pageImageCache = new HashMap() {
			public Object put(Object cacheKey, Object gapeImage) {
				if (gapeImage instanceof PageImage) {
					PageImage pi = ((PageImage) gapeImage);
					if (pi.currentDpi > 300)
						return null;
					if ((pi.image.getType() != BufferedImage.TYPE_BYTE_BINARY) && (pi.image.getType() != BufferedImage.TYPE_BYTE_GRAY))
						return null;
				}
				return super.put(cacheKey, gapeImage);
			}
		};
		
		//	load basic document structure
		psm.setStep("Loading document page images");
		psm.setBaseProgress(0);
		psm.setMaxProgress(30);
		this.doLoadImagePdfPages(doc, pdfDoc, bytes, scaleFactor, psm, pageImageCache);
		
		//	prepare working in parallel
		final LinkedList pageIDs = new LinkedList();
		final Throwable[] error = {null};
		
		//	make pages individual documents, as GAMTA documents are not thread safe
		MutableAnnotation[] docPages = doc.getMutableAnnotations(PAGE_TYPE);
		final MutableAnnotation[] pages = new MutableAnnotation[docPages.length];
		for (int p = 0; p < pages.length; p++)
			pages[p] = Gamta.copyDocument(docPages[p]);
		
		//	do structure analysis
		psm.setStep("Analyzing page structure");
		psm.setBaseProgress(30);
		psm.setProgress(0);
		psm.setMaxProgress(60);
		pageIDs.clear();
		if (PdfExtractorTest.aimAtPage != -1) // TODO remove this after test
			pageIDs.addLast(new Integer(PdfExtractorTest.aimAtPage));
		else for (int p = 0; p < pages.length; p++)
			pageIDs.addLast(new Integer(p));
		error[0] = null;
		this.runJob( new Runnable() {
			public void run() {
				while (true) try {
					
					//	any error or other problems in siblings?
					synchronized (error) {
						if (error[0] != null)
							return;
					}
					
					//	get next page ID to work off
					int p;
					synchronized (pageIDs) {
						if (pageIDs.isEmpty())
							return;
						p = ((Integer) pageIDs.removeFirst()).intValue();
					}
					
					//	update status display (might be inaccurate, but better than lock escalation)
					synchronized (psm) {
						psm.setInfo("Analyzing structure of page " + (pages.length - pageIDs.size()) + " of " + pages.length);
						psm.setProgress((100 * (pages.length - pageIDs.size() - 1)) / pages.length);
					}
					
					//	get page image
					PageImage pi = ((PageImage) pageImageCache.get(new Integer(p)));
					if (pi == null)
						pi = PageImage.getPageImage(docId, p);
					if (pi == null) {
						psm.setInfo(" - page image not found");
						throw new RuntimeException("Could not find image for page " + p);
					}
					
					//	do structure analysis
					PageImageConverter.fillInTextBlockStructure(pages[p], null, psm, pi);
					synchronized (psm) {
						psm.setInfo(" - page structure done");
					}
					
					//	do layout analysis
					PageImageConverter.analyzeLineWordMetrics(pages[p], psm, pi);
					synchronized (psm) {
						psm.setInfo(" - page layout done");
					}
					
					//	clean up analysis image cache
					Imaging.cleanUpCache(pi.image.hashCode() + "-");
				}
				catch (Throwable t) {
					synchronized (error) {
						error[0] = t;
					}
					return;
				}
			}
		}, pages.length);
		psm.setProgress(100);
		
		//	check errors
		if (error[0] != null) {
			if (error[0] instanceof RuntimeException)
				throw ((RuntimeException) error[0]);
			else if (error[0] instanceof Error)
				throw ((Error) error[0]);
			else if (error[0] instanceof IOException)
				throw ((IOException) error[0]);
			else throw new IOException(error[0].getMessage());
		}
		
		//	do OCR
		psm.setStep("Doing OCR");
		psm.setBaseProgress(60);
		psm.setProgress(0);
		psm.setMaxProgress(95);
		pageIDs.clear();
		if (PdfExtractorTest.aimAtPage != -1) // TODO remove this after test
			pageIDs.addLast(new Integer(PdfExtractorTest.aimAtPage));
		else for (int p = 0; p < pages.length; p++)
			pageIDs.addLast(new Integer(p));
		error[0] = null;
		this.runJob( new Runnable() {
			public void run() {
				while (true) try {
					
					//	any error or other problems in siblings?
					synchronized (error) {
						if (error[0] != null)
							return;
					}
					
					//	get next page ID to work off
					int p;
					synchronized (pageIDs) {
						if (pageIDs.isEmpty())
							return;
						p = ((Integer) pageIDs.removeFirst()).intValue();
					}
					
					//	update status display (might be inaccurate, but better than lock escalation)
					synchronized (psm) {
						psm.setInfo("Doing OCR for page " + (pages.length - pageIDs.size()) + " of " + pages.length);
						psm.setProgress((100 * (pages.length - pageIDs.size() - 1)) / pages.length);
					}
					
					//	get page image
					PageImage pi = ((PageImage) pageImageCache.get(new Integer(p)));
					if (pi == null)
						pi = PageImage.getPageImage(docId, p);
					if (pi == null) {
						psm.setInfo(" - page image not found");
						throw new RuntimeException("Could not find image for page " + p);
					}
					
					//	do OCR
					synchronized (psm) {
						psm.setInfo(" - doing OCR");
					}
					ocrEngine.doWordOcr(pages[p], pi, psm);
					synchronized (psm) {
						psm.setInfo(" - OCR done");
					}
					
					//	do structure analysis
					PageAnalysis.splitIntoParagraphs(pages[p].getMutableAnnotations(BLOCK_ANNOTATION_TYPE), pi.currentDpi, null);
					synchronized (psm) {
						psm.setInfo(" - paragraphs done");
					}
					PageAnalysis.computeColumnLayout(pages[p].getMutableAnnotations(COLUMN_ANNOTATION_TYPE), pi.currentDpi);
					synchronized (psm) {
						psm.setInfo(" - layout analysis done");
					}
					
					//	... finally
					synchronized (psm) {
						psm.setInfo(" - page done");
					}
					
					//	clean up analysis image cache
					Imaging.cleanUpCache(pi.image.hashCode() + "-");
				}
				catch (Throwable t) {
					synchronized (error) {
						error[0] = t;
					}
					return;
				}
			}
		}, pages.length);
		psm.setProgress(100);
		
		//	copy pages into main document
		psm.setBaseProgress(95);
		psm.setProgress(0);
		psm.setMaxProgress(100);
		for (int p = 0; p < pages.length; p++) {
			psm.setProgress((100 * p) / pages.length);
			if ("P".equals(pages[p].firstValue()))
				continue;
			docPages[p].addTokens(pages[p]);
			docPages[p].removeTokensAt(0, 1);
			Annotation[] pageAnnots = pages[p].getAnnotations();
			for (int a = 0; a < pageAnnots.length; a++) {
				if (DocumentRoot.DOCUMENT_TYPE.equals(pageAnnots[a].getType()))
					continue;
				if (PAGE_TYPE.equals(pageAnnots[a].getType())) {
					docPages[p].copyAttributes(pageAnnots[a]);
					continue;
				}
				Annotation docAnnot = docPages[p].addAnnotation(pageAnnots[a].getType(), pageAnnots[a].getStartIndex(), pageAnnots[a].size());
				docAnnot.copyAttributes(pageAnnots[a]);
			}
		}
		psm.setProgress(100);
		
		//	clean up cache
		for (Iterator piit = pageImageCache.keySet().iterator(); piit.hasNext();) {
			Integer pid = ((Integer) piit.next());
			PageImage pi = ((PageImage) pageImageCache.get(pid));
			Imaging.cleanUpCache(pi.image.hashCode() + "-");
		}
		
		//	... finally
		return doc;
	}
	
	/**
	 * Load the pages of a document from an OCR PDF, including their block
	 * structure. Page images are stored in the page image store handed to the
	 * constructor. The document structure is analyzed and represented to the
	 * text block level, but not below. 
	 * @param doc the document to store the structural information in
	 * @param pdfDoc the PDF document to parse
	 * @param bytes the raw binary data the the PDF document was parsed from
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @return the argument GAMTA document
	 * @throws IOException
	 */
	public DocumentRoot loadImagePdfBlocks(DocumentRoot doc, Document pdfDoc, byte[] bytes, ProgressMonitor psm) throws IOException {
		return this.loadImagePdfBlocks(doc, pdfDoc, bytes, 1, psm);
	}
	
	/**
	 * Load the pages of a document from an OCR PDF, including their block
	 * structure. Page images are stored in the page image store handed to the
	 * constructor. The document structure is analyzed and represented to the
	 * text block level, but not below. The scale factor helps with PDFs whose
	 * page boundaries (media boxes) are set too small. This can be recognized
	 * in any PDF viewer by the fact that the document is tiny and the writing
	 * very small at 100% size display. It can also be detected automatically,
	 * e.g. by means of examining the average height of lines in comparison to
	 * the DPI number.
	 * @param doc the document to store the structural information in
	 * @param pdfDoc the PDF document to parse
	 * @param bytes the raw binary data the the PDF document was parsed from
	 * @param scaleFactor the scale factor for image PDFs, to correct PDFs that
	 *            are set too small (DPI number is divided by this factor)
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @return the argument GAMTA document
	 * @throws IOException
	 */
	public DocumentRoot loadImagePdfBlocks(DocumentRoot doc, Document pdfDoc, byte[] bytes, int scaleFactor, ProgressMonitor psm) throws IOException {
		if (psm == null)
			psm = ProgressMonitor.dummy;
		
		//	test if we can OCR
		if (this.ocrEngine == null)
			throw new RuntimeException("OCR Engine unavailable, check startup log.");
		
		//	prepare document
		String docId = ((String) doc.getAttribute(DOCUMENT_ID_ATTRIBUTE));
		if (docId == null) {
			docId = doc.getAnnotationID();
			doc.setAttribute(DOCUMENT_ID_ATTRIBUTE, docId);
		}
		
		//	do it
		return this.doLoadImagePdfBlocks(doc, docId, pdfDoc, bytes, scaleFactor, psm);
	}
	
	private DocumentRoot doLoadImagePdfBlocks(DocumentRoot doc, final String docId, Document pdfDoc, byte[] bytes, int scaleFactor, final ProgressMonitor psm) throws IOException {
		
		//	cache page images (only binary and gray scale, and only up to 300 DPI, as memory consumption gets too high beyond that)
		final HashMap pageImageCache = new HashMap() {
			public Object put(Object cacheKey, Object gapeImage) {
				if (gapeImage instanceof PageImage) {
					PageImage pi = ((PageImage) gapeImage);
					if (pi.currentDpi > 300)
						return null;
					if ((pi.image.getType() != BufferedImage.TYPE_BYTE_BINARY) && (pi.image.getType() != BufferedImage.TYPE_BYTE_GRAY))
						return null;
				}
				return super.put(cacheKey, gapeImage);
			}
		};
		
		//	load basic document structure
		psm.setStep("Loading document page images");
		psm.setBaseProgress(0);
		psm.setProgress(0);
		psm.setMaxProgress(30);
		this.doLoadImagePdfPages(doc, pdfDoc, bytes, scaleFactor, psm, pageImageCache);
		
		//	prepare working in parallel
		final LinkedList pageIDs = new LinkedList();
		final Throwable[] error = {null};
		
		//	make pages individual documents, as GAMTA documents are not thread safe
		MutableAnnotation[] docPages = doc.getMutableAnnotations(PAGE_TYPE);
		final MutableAnnotation[] pages = new MutableAnnotation[docPages.length];
		for (int p = 0; p < pages.length; p++)
			pages[p] = Gamta.copyDocument(docPages[p]);
		
		//	do structure analysis
		psm.setStep("Analyzing page structure");
		psm.setBaseProgress(30);
		psm.setProgress(0);
		psm.setMaxProgress(60);
		pageIDs.clear();
		if (PdfExtractorTest.aimAtPage != -1) // TODO remove this after test
			pageIDs.addLast(new Integer(PdfExtractorTest.aimAtPage));
		else for (int p = 0; p < pages.length; p++)
			pageIDs.addLast(new Integer(p));
		error[0] = null;
		this.runJob( new Runnable() {
			public void run() {
				while (true) try {
					
					//	any error or other problems in siblings?
					synchronized (error) {
						if (error[0] != null)
							return;
					}
					
					//	get next page ID to work off
					int p;
					synchronized (pageIDs) {
						if (pageIDs.isEmpty())
							return;
						p = ((Integer) pageIDs.removeFirst()).intValue();
					}
					
					//	update status display (might be inaccurate, but better than lock escalation)
					synchronized (psm) {
						psm.setInfo("Analyzing structure of page " + (pages.length - pageIDs.size()) + " of " + pages.length);
						psm.setProgress((100 * (pages.length - pageIDs.size() - 1)) / pages.length);
					}
					
					//	get page image
					PageImage pi = ((PageImage) pageImageCache.get(new Integer(p)));
					if (pi == null)
						pi = PageImage.getPageImage(docId, p);
					if (pi == null) {
						psm.setInfo(" - page image not found");
						throw new RuntimeException("Could not find image for page " + p);
					}
					
					//	do block level structure analysis
					PageImageConverter.fillInPageRegions(pages[p], pi, psm);
					synchronized (psm) {
						psm.setInfo(" - page done");
					}
					
					//	clean up analysis image cache
					Imaging.cleanUpCache(pi.image.hashCode() + "-");
				}
				catch (Throwable t) {
					synchronized (error) {
						error[0] = t;
					}
					return;
				}
			}
		}, pages.length);
		psm.setProgress(100);
		
		//	check errors
		if (error[0] != null) {
			if (error[0] instanceof RuntimeException)
				throw ((RuntimeException) error[0]);
			else if (error[0] instanceof Error)
				throw ((Error) error[0]);
			else if (error[0] instanceof IOException)
				throw ((IOException) error[0]);
			else throw new IOException(error[0].getMessage());
		}
		
		//	do OCR
		psm.setStep("Doing OCR");
		psm.setBaseProgress(60);
		psm.setProgress(0);
		psm.setMaxProgress(95);
		pageIDs.clear();
		if (PdfExtractorTest.aimAtPage != -1) // TODO remove this after test
			pageIDs.addLast(new Integer(PdfExtractorTest.aimAtPage));
		else for (int p = 0; p < pages.length; p++)
			pageIDs.addLast(new Integer(p));
		error[0] = null;
		this.runJob( new Runnable() {
			public void run() {
				while (true) try {
					
					//	any error or other problems in siblings?
					synchronized (error) {
						if (error[0] != null)
							return;
					}
					
					//	get next page ID to work off
					int p;
					synchronized (pageIDs) {
						if (pageIDs.isEmpty())
							return;
						p = ((Integer) pageIDs.removeFirst()).intValue();
					}
					
					//	update status display (might be inaccurate, but better than lock escalation)
					synchronized (psm) {
						psm.setInfo("Doing OCR for page " + (pages.length - pageIDs.size()) + " of " + pages.length);
						psm.setProgress((100 * (pages.length - pageIDs.size() - 1)) / pages.length);
					}
					
					//	get page image
					PageImage pi = ((PageImage) pageImageCache.get(new Integer(p)));
					if (pi == null)
						pi = PageImage.getPageImage(docId, p);
					if (pi == null) {
						synchronized (psm) {
							psm.setInfo(" - page image not found");
						}
						throw new RuntimeException("Could not find image for page " + p);
					}
					
					//	do OCR
					synchronized (psm) {
						psm.setInfo(" - doing OCR");
					}
					ocrEngine.doBlockOcr(pages[p], pi, psm);
					synchronized (psm) {
						psm.setInfo(" - OCR done");
					}
					
					//	do structure analysis
					PageAnalysis.splitIntoParagraphs(pages[p].getMutableAnnotations(BLOCK_ANNOTATION_TYPE), pi.currentDpi, null);
					synchronized (psm) {
						psm.setInfo(" - paragraphs done");
					}
					PageAnalysis.computeColumnLayout(pages[p].getMutableAnnotations(COLUMN_ANNOTATION_TYPE), pi.currentDpi);
					synchronized (psm) {
						psm.setInfo(" - layout analysis done");
					}
					
					//	... finally
					synchronized (psm) {
						psm.setInfo(" - page done");
					}
					
					//	clean up analysis image cache
					Imaging.cleanUpCache(pi.image.hashCode() + "-");
				}
				catch (Throwable t) {
					synchronized (error) {
						error[0] = t;
					}
					return;
				}
			}
		}, pages.length);
		psm.setProgress(100);
		
		//	check errors
		if (error[0] != null) {
			if (error[0] instanceof RuntimeException)
				throw ((RuntimeException) error[0]);
			else if (error[0] instanceof Error)
				throw ((Error) error[0]);
			else if (error[0] instanceof IOException)
				throw ((IOException) error[0]);
			else throw new IOException(error[0].getMessage());
		}
		
		//	copy pages into main document
		psm.setBaseProgress(95);
		psm.setProgress(0);
		psm.setMaxProgress(100);
		for (int p = 0; p < pages.length; p++) {
			psm.setProgress((100 * p) / pages.length);
			if ("P".equals(pages[p].firstValue()))
				continue;
			docPages[p].addTokens(pages[p]);
			docPages[p].removeTokensAt(0, 1);
			Annotation[] pageAnnots = pages[p].getAnnotations();
			for (int a = 0; a < pageAnnots.length; a++) {
				if (DocumentRoot.DOCUMENT_TYPE.equals(pageAnnots[a].getType()))
					continue;
				if (PAGE_TYPE.equals(pageAnnots[a].getType())) {
					docPages[p].copyAttributes(pageAnnots[a]);
					continue;
				}
				Annotation docAnnot = docPages[p].addAnnotation(pageAnnots[a].getType(), pageAnnots[a].getStartIndex(), pageAnnots[a].size());
				docAnnot.copyAttributes(pageAnnots[a]);
			}
		}
		psm.setProgress(100);
		
		//	clean up cache
		for (Iterator piit = pageImageCache.keySet().iterator(); piit.hasNext();) {
			Integer pid = ((Integer) piit.next());
			PageImage pi = ((PageImage) pageImageCache.get(pid));
			Imaging.cleanUpCache(pi.image.hashCode() + "-");
		}
		
		//	... finally
		return doc;
	}
	
	/**
	 * Load the pages of a document from an OCR PDF. Page images are stored in
	 * the page image store handed to the constructor. The document structure is
	 * not analyzed or represented beyond the page level. The scale factor helps
	 * with PDFs whose page boundaries (media boxes) are set too small. This can
	 * be recognized in any PDF viewer by the fact that the document is tiny and
	 * the writing very small at 100% size display. It can also be detected
	 * automatically, e.g. by means of examining the average height of lines in
	 * comparison to the DPI number.
	 * @param doc the document to store the structural information in
	 * @param pdfDoc the PDF document to parse
	 * @param bytes the raw binary data the the PDF document was parsed from
	 * @param scaleFactor the scale factor for image PDFs, to correct PDFs that
	 *            are set too small (DPI number is divided by this factor)
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @return the argument GAMTA document
	 * @throws IOException
	 */
	public DocumentRoot loadImagePdfPages(DocumentRoot doc, Document pdfDoc, byte[] bytes, ProgressMonitor psm) throws IOException {
		return this.loadImagePdfPages(doc, pdfDoc, bytes, 1, psm);
	}
	
	/**
	 * Load the pages of a document from an OCR PDF. Page images are stored in
	 * the page image store handed to the constructor. The document structure is
	 * not analyzed or represented beyond the page level. The scale factor helps
	 * with PDFs whose page boundaries (media boxes) are set too small. This can
	 * be recognized in any PDF viewer by the fact that the document is tiny and
	 * the writing very small at 100% size display. It can also be detected
	 * automatically, e.g. by means of examining the average height of lines in
	 * comparison to the DPI number.
	 * @param doc the document to store the structural information in
	 * @param pdfDoc the PDF document to parse
	 * @param bytes the raw binary data the the PDF document was parsed from
	 * @param scaleFactor the scale factor for image PDFs, to correct PDFs that
	 *            are set too small (DPI number is divided by this factor)
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @return the argument GAMTA document
	 * @throws IOException
	 */
	public DocumentRoot loadImagePdfPages(DocumentRoot doc, Document pdfDoc, byte[] bytes, int scaleFactor, ProgressMonitor psm) throws IOException {
		if (psm == null)
			psm = ProgressMonitor.dummy;
		
		psm.setBaseProgress(0);
		psm.setMaxProgress(100);
		
		return this.doLoadImagePdfPages(doc, pdfDoc, bytes, scaleFactor, psm, null);
	}
	
	private DocumentRoot doLoadImagePdfPages(final DocumentRoot doc, final Document pdfDoc, final byte[] bytes, final int scaleFactor, final ProgressMonitor psm, final HashMap pageImageCache) throws IOException {
		
		//	load document structure (IcePDF is better at that ...)
		final Catalog catalog = pdfDoc.getCatalog();
		final PageTree pageTree = catalog.getPageTree();
		
		//	extract page objects
		psm.setInfo("Getting page objects");
		final Hashtable[] pages = new Hashtable[pageTree.getNumberOfPages()];
		for (int p = 0; p < pageTree.getNumberOfPages(); p++) {
			Page page = pageTree.getPage(p, "");
			if (page == null) {
				pages[p] = new Hashtable();
				continue;
			}
			pages[p] = page.getEntries();
		}
		psm.setInfo(" --> got " + pageTree.getNumberOfPages() + " page objects");
		psm.setProgress(2);
		
		//	read objects
		psm.setInfo("Getting remaining objects");
		final HashMap objects = PdfParser.getObjects(bytes);
		psm.setInfo(" --> done");
		psm.setProgress(10);
		
		//	prepare working in parallel
		final LinkedList pageIDs = new LinkedList();
		final Throwable[] error = {null};
		
		//	extract page image objects
		psm.setInfo("Getting page image objects");
		final HashMap[] pageImages = new HashMap[pages.length];
		final int[] minPageImages = {Integer.MAX_VALUE};
		final int[] maxPageImages = {0};
		final int[] sumPageImages = {0};
		pageIDs.clear();
		for (int p = 0; p < pages.length; p++)
			pageIDs.addLast(new Integer(p));
		error[0] = null;
		this.runJob( new Runnable() {
			public void run() {
				while (true) try {
					
					//	any error or other problems in siblings?
					synchronized (error) {
						if (error[0] != null)
							return;
					}
					synchronized (minPageImages) {
						if (minPageImages[0] == 0)
							return;
					}
					
					//	get next page ID to work off
					int p;
					synchronized (pageIDs) {
						if (pageIDs.isEmpty())
							return;
						p = ((Integer) pageIDs.removeFirst()).intValue();
					}
					pageImages[p] = new HashMap(2);
					
					//	get resources
					Object resourcesObj = PdfParser.getObject(pages[p], "Resources", objects);
					if ((resourcesObj == null) || !(resourcesObj instanceof Hashtable)) {
						synchronized (psm) {
							psm.setInfo(" --> resource map not found");
						}
						synchronized (minPageImages) {
							minPageImages[0] = 0;
						}
						return;
					}
					
					//	map IDs to all images in page
					int imgCount = 0;
					for (Iterator rit = getSortedKeyIterator((Hashtable) resourcesObj); rit.hasNext();) {
						Object resKey = rit.next();
						Object resObj = PdfParser.dereference(((Hashtable) resourcesObj).get(resKey), objects);
						if (resObj == null)
							continue;
						if (resObj instanceof PStream) {
							pageImages[p].put(("i" + imgCount), new PImage((PStream) resObj));
							PImage mImg = PdfParser.getMaskImage(((PStream) resObj), objects);
							if (mImg != null)
								pageImages[p].put(("m" + imgCount), mImg);
							imgCount++;
						}
						else if ("XObject".equalsIgnoreCase(resKey.toString()) && (resObj instanceof Hashtable)) {
							for (Iterator xrit = getSortedKeyIterator((Hashtable) resObj); xrit.hasNext();) {
								Object xResKey = xrit.next();
								Object xResObj = PdfParser.dereference(((Hashtable) resObj).get(xResKey), objects);
								if (xResObj == null)
									continue;
								if (xResObj instanceof PStream) {
									pageImages[p].put(("i" + imgCount), new PImage((PStream) xResObj));
									PImage mImg = PdfParser.getMaskImage(((PStream) xResObj), objects);
									if (mImg != null)
										pageImages[p].put(("m" + imgCount), mImg);
									imgCount++;
								}
							}
						}
					}
					
					//	update registers
					synchronized (minPageImages) {
						minPageImages[0] = Math.min(minPageImages[0], pageImages[p].size());
					}
					synchronized (maxPageImages) {
						maxPageImages[0] = Math.max(maxPageImages[0], pageImages[p].size());
					}
					synchronized (sumPageImages) {
						sumPageImages[0] += pageImages[p].size();
					}
				}
				catch (Throwable t) {
					synchronized (error) {
						error[0] = t;
					}
					return;
				}
			}
		}, pages.length);
		psm.setInfo(" --> got at least " + minPageImages + ", at most " + maxPageImages + " images per page, " + sumPageImages + " in total");
		psm.setProgress(20);
		
		//	check errors
		if (error[0] != null) {
			if (error[0] instanceof RuntimeException)
				throw ((RuntimeException) error[0]);
			else if (error[0] instanceof Error)
				throw ((Error) error[0]);
			else if (error[0] instanceof IOException)
				throw ((IOException) error[0]);
			else throw new IOException(error[0].getMessage());
		}
		
		//	check consistency
		if (minPageImages[0] == 0)
			throw new IOException("Unable to find images for all pages");
		
		//	find out which page image holds the text
		psm.setInfo("Determining text image ID");
		final String textImageId = this.findTextImageId(pageImages, pageTree, objects);
		if (textImageId == null)
			throw new IOException("Unable to find images for all pages");
		psm.setInfo(" ==> text page image id is " + textImageId);
		psm.setProgress(30);
		
		//	build document
		String tempDocId = ((String) doc.getAttribute(DOCUMENT_ID_ATTRIBUTE));
		if (tempDocId == null) {
			tempDocId = doc.getAnnotationID();
			doc.setAttribute(DOCUMENT_ID_ATTRIBUTE, tempDocId);
		}
		final String docId = tempDocId;
		
		//	extract & save page images
		final String[] imageNames = new String[pages.length];
		final int[] imageDPIs = new int[pages.length];
		pageIDs.clear();
		if (PdfExtractorTest.aimAtPage != -1) // TODO remove this after test
			pageIDs.addLast(new Integer(PdfExtractorTest.aimAtPage));
		else for (int p = 0; p < pages.length; p++)
			pageIDs.addLast(new Integer(p));
		error[0] = null;
		this.runJob( new Runnable() {
			public void run() {
				int cp = -1;
				while (true) try {
					cp = -1;
					
					//	any error or other problems in siblings?
					synchronized (error) {
						if (error[0] != null)
							return;
					}
					
					//	get next page ID to work off, and update status info
					int p;
					synchronized (pageIDs) {
						if (pageIDs.isEmpty())
							return;
						p = ((Integer) pageIDs.removeFirst()).intValue();
						cp = p;
					}
					
					//	update status display (might be inaccurate, but better than lock escalation)
					synchronized (psm) {
						psm.setStep("Importing page " + (pages.length - pageIDs.size()) + " of " + pages.length);
						psm.setProgress(30 + (((pages.length - pageIDs.size() - 1) * 70) / pages.length));
					}
					
					//	get page image
					synchronized (psm) {
						psm.setInfo(" - getting page image");
					}
					
					//	image already extracted
					if (imageStore.isPageImageAvailable(docId, p)) {
						psm.setInfo(" --> image already rendered");
						psm.setInfo(" - getting image data ...");
						PageImageInputStream piis = imageStore.getPageImageAsStream(docId, p);
						piis.close();
						int dpi = piis.currentDpi;
						imageNames[p] = PageImage.getPageImageName(docId, p);
						imageDPIs[p] = piis.currentDpi;
						psm.setInfo(" - resolution is " + dpi + " DPI");
					}
					
					//	image not extracted as yet, do it now
					else {
						
						//	get compressed image
						PImage imageObject = ((PImage) pageImages[p].get(textImageId));
						if (imageObject == null) {
							psm.setInfo(" --> image not found");
							throw new RuntimeException("Could not find image object for page " + p);
						}
						psm.setInfo(" --> image not rendered yet");
						psm.setInfo(" - rendering image ...");
						
						//	get raw image
						BufferedImage pageImage = getImage(imageObject, objects);
						if (pageImage == null) {
							psm.setInfo(" --> page image generation failed");
							throw new RuntimeException("Could not generate image for page " + p);
						}
						psm.setInfo(" - got page image sized " + pageImage.getWidth() + " x " + pageImage.getHeight());
						
						//	compute DPI
						int pageScaleFactor = scaleFactor;
						Rectangle2D.Float pb = pageTree.getPage(p, "").getMediaBox();
						float dpiRatio = ((((float) pageImage.getWidth()) / pb.width) + (((float) pageImage.getHeight()) / pb.height)) / 2;
						float rawDpi = dpiRatio * defaultDpi;
						int dpi = ((Math.round(rawDpi / 10) * 10) / pageScaleFactor);
						psm.setInfo(" - resolution computed as " + dpi + " DPI");
						
						//	guess DPI based on page size (minimum page size for (very most) publications should be A5 or so)
						float a5distWidth = Math.abs((pb.width / dpi) - a5inchWidth);
						float a5distHeigth = Math.abs((pb.height / dpi) - a5inchHeigth);
						float a7distWidth = Math.abs((pb.width / dpi) - (a5inchWidth / 2));
						float a7distHeigth = Math.abs((pb.height / dpi) - (a5inchHeigth / 2));
						while ((a7distWidth < a5distWidth) && (a7distHeigth < a5distHeigth) && (minScaleupDpi < dpi)) {
							pageScaleFactor++;
							dpi = ((Math.round(rawDpi / 10) * 10) / pageScaleFactor);
							a5distWidth = Math.abs((pb.width / dpi) - a5inchWidth);
							a5distHeigth = Math.abs((pb.height / dpi) - a5inchHeigth);
							a7distWidth = Math.abs((pb.width / dpi) - (a5inchWidth / 2));
							a7distHeigth = Math.abs((pb.height / dpi) - (a5inchHeigth / 2));
						}
						if (pageScaleFactor > 1)
							psm.setInfo(" - resolution scaled to " + dpi + " DPI");
						
						//	enhance image (cannot use cache here, as image might change during correction)
						AnalysisImage ai = Imaging.wrapImage(pageImage, null);
						psm.setInfo(" - enhancing image ...");
						ai = Imaging.correctImage(ai, dpi, psm);
						pageImage = ai.getImage();
						
						//	TODO do further cleanup, e.g. removing pencil strokes, etc.
						//	TODO ==> collect examples and talk to Hilde
						
						//	store image
						imageNames[p] = imageStore.storePageImage(docId, p, pageImage, dpi);
						imageDPIs[p] = dpi;
						if (imageNames[p] == null) {
							psm.setInfo(" --> page image storage failed");
							throw new RuntimeException("Could not store image for page " + p);
						}
						if (pageImageCache != null)
							pageImageCache.put(new Integer(p), new PageImage(pageImage, dpi, imageStore)); // specifying image store from outside indicates who's handling the image now, as we know it was stored successfully
						Imaging.cleanUpCache(pageImage.hashCode() + "-");
						psm.setInfo(" --> page image stored");
					}
				}
				
				//	catch whatever comes our way, so master thread re-throw it later
				catch (Throwable t) {
					synchronized (error) {
						error[0] = t;
					}
					return;
				}
				
				//	clean up after current page
				finally {
					if (cp != -1)
						pageImages[cp].clear();
					System.gc();
				}
			}
		}, pages.length);
		psm.setInfo(" --> got at least " + minPageImages + ", at most " + maxPageImages + " images per page, " + sumPageImages + " in total");
		psm.setProgress(20);
		
		//	assemble document
		int pageStart = 0;
		for (int p = 0; p < pages.length; p++) {
			
			//	add page marker
			doc.addTokens("P");
			
			//	annotate page
			MutableAnnotation pageAnnot = doc.addAnnotation(PAGE_TYPE, pageStart, (doc.size() - pageStart));
			pageAnnot.setAttribute(PAGE_ID_ATTRIBUTE, ("" + p));
			if (imageNames[p] == null)
				pageAnnot.setAttribute(IMAGE_NAME_ATTRIBUTE, "");
			else {
				pageAnnot.setAttribute(IMAGE_NAME_ATTRIBUTE, imageNames[p]);
				pageAnnot.setAttribute(IMAGE_DPI_ATTRIBUTE, ("" + imageDPIs[p]));
			}
			pageAnnot.setAttribute(IS_OCRED_ATTRIBUTE, "true");
		}
		
		//	finally ...
		psm.setProgress(100);
		return doc;
	}
	
	private void runJob(Runnable job, int numPages) {
		int numCores = Runtime.getRuntime().availableProcessors();
		int useCores = (this.useMultipleCores ? Math.max(1, Math.min((numCores - 1), numPages)) : 1);
		if (useCores < 2) {
			job.run();
			return;
		}
		Thread[] threads = new Thread[useCores];
		for (int t = 0; t < threads.length; t++)
			threads[t] = new Thread(job);
		for (int t = 0; t < threads.length; t++)
			threads[t].start();
		for (int t = 0; t < threads.length; t++) try {
			threads[t].join();
		} catch (InterruptedException ie) {}
	}
	
	private static int textImageIdMinSamples = 3;
	private static int textImageIdMaxSamples = 20;
	private String findTextImageId(HashMap[] pageImages, PageTree pageTree, HashMap objects) throws IOException {
		HashSet imageIds = new HashSet();
		StringIndex imageIdScores = new StringIndex(true);
		
		StringIndex iidValidities = new StringIndex(true);
		int sampledPageCount = 0;
		
		if (pageImages.length <= textImageIdMinSamples) {
			for (int p = 0; p < pageImages.length; p++) {
				System.out.println(" Scoring images in page " + p);
				Rectangle2D.Float pb = pageTree.getPage(p, "").getMediaBox();
				BoundingBox pageBounds = new BoundingBox(((int) Math.round(pb.getMinX())), ((int) Math.round(pb.getMaxX())), ((int) Math.round(pb.getMinY())), ((int) Math.round(pb.getMaxY())));
				System.out.println(" - page bounds are " + (pageBounds.right - pageBounds.left) + " x " + (pageBounds.bottom - pageBounds.top));
				this.addImageIdsAndScores(pageImages[p], objects, imageIds, imageIdScores, pageBounds);
				for (Iterator iit = pageImages[p].keySet().iterator(); iit.hasNext();)
					iidValidities.add((String) iit.next());
				sampledPageCount++;
			}
		}
		else if (pageImages.length <= textImageIdMaxSamples) {
			HashSet sampled = new HashSet();
			while (sampled.size() < textImageIdMinSamples) {
				int p = ((int) (pageImages.length * Math.random()));
				if (sampled.add(new Integer(p))) {
					System.out.println(" Sampling images in page " + p);
					Rectangle2D.Float pb = pageTree.getPage(p, "").getMediaBox();
					BoundingBox pageBounds = new BoundingBox(((int) Math.round(pb.getMinX())), ((int) Math.round(pb.getMaxX())), ((int) Math.round(pb.getMinY())), ((int) Math.round(pb.getMaxY())));
					System.out.println(" - page bounds are " + (pageBounds.right - pageBounds.left) + " x " + (pageBounds.bottom - pageBounds.top));
					this.addImageIdsAndScores(pageImages[p], objects, imageIds, imageIdScores, pageBounds);
					for (Iterator iit = pageImages[p].keySet().iterator(); iit.hasNext();)
						iidValidities.add((String) iit.next());
					sampledPageCount++;
				}
			}
			for (int p = 0; p < pageImages.length; p++) {
				if (!sampled.add(new Integer(p)))
					continue;
				else if (!this.scoresAmbiguous(imageIds, imageIdScores))
					break;
				System.out.println(" Scoring images in page " + p);
				Rectangle2D.Float pb = pageTree.getPage(p, "").getMediaBox();
				BoundingBox pageBounds = new BoundingBox(((int) Math.round(pb.getMinX())), ((int) Math.round(pb.getMaxX())), ((int) Math.round(pb.getMinY())), ((int) Math.round(pb.getMaxY())));
				System.out.println(" - page bounds are " + (pageBounds.right - pageBounds.left) + " x " + (pageBounds.bottom - pageBounds.top));
				this.addImageIdsAndScores(pageImages[p], objects, imageIds, imageIdScores, pageBounds);
				for (Iterator iit = pageImages[p].keySet().iterator(); iit.hasNext();)
					iidValidities.add((String) iit.next());
				sampledPageCount++;
			}
		}
		else {
			HashSet sampled = new HashSet();
			while (sampled.size() < textImageIdMinSamples) {
				int p = ((int) (pageImages.length * Math.random()));
				if (sampled.add(new Integer(p))) {
					System.out.println(" Sampling images in page " + p);
					Rectangle2D.Float pb = pageTree.getPage(p, "").getMediaBox();
					BoundingBox pageBounds = new BoundingBox(((int) Math.round(pb.getMinX())), ((int) Math.round(pb.getMaxX())), ((int) Math.round(pb.getMinY())), ((int) Math.round(pb.getMaxY())));
					System.out.println(" - page bounds are " + (pageBounds.right - pageBounds.left) + " x " + (pageBounds.bottom - pageBounds.top));
					this.addImageIdsAndScores(pageImages[p], objects, imageIds, imageIdScores, pageBounds);
					for (Iterator iit = pageImages[p].keySet().iterator(); iit.hasNext();)
						iidValidities.add((String) iit.next());
					sampledPageCount++;
				}
			}
			while ((sampled.size() < textImageIdMaxSamples) && scoresAmbiguous(imageIds, imageIdScores)) {
				int p = ((int) (pageImages.length * Math.random()));
				if (sampled.add(new Integer(p))) {
					System.out.println(" Sampling images in page " + p);
					Rectangle2D.Float pb = pageTree.getPage(p, "").getMediaBox();
					BoundingBox pageBounds = new BoundingBox(((int) Math.round(pb.getMinX())), ((int) Math.round(pb.getMaxX())), ((int) Math.round(pb.getMinY())), ((int) Math.round(pb.getMaxY())));
					System.out.println(" - page bounds are " + (pageBounds.right - pageBounds.left) + " x " + (pageBounds.bottom - pageBounds.top));
					this.addImageIdsAndScores(pageImages[p], objects, imageIds, imageIdScores, pageBounds);
					for (Iterator iit = pageImages[p].keySet().iterator(); iit.hasNext();)
						iidValidities.add((String) iit.next());
					sampledPageCount++;
				}
			}
		}
		
		int maxScore = 0;
		String imageId = null;
		for (Iterator iit = imageIds.iterator(); iit.hasNext();) {
			String iid = ((String) iit.next());
			
			//	check if ID valid in two thirds of all sampled pages
			if ((iidValidities.getCount(iid) * 3) < (sampledPageCount * 2))
				continue;
			
			//	ensure ID works in at least two thirds of all pages
			int iidPages = 0;
			for (int p = 0; p < pageImages.length; p++) {
				if (pageImages[p].containsKey(iid))
					iidPages++;
			}
			if ((iidPages * 3) < (pageImages.length * 2))
				continue;
			
			//	check if we have a new top ID
			int iidScore = imageIdScores.getCount(iid);
			if (iidScore > maxScore) {
				imageId = iid;
				maxScore = iidScore;
			}
		}
		
		if (imageId != null) {
			for (int p = 0; p < pageImages.length; p++)
				for (Iterator iit = pageImages[p].keySet().iterator(); iit.hasNext();) {
					String imgId = ((String) iit.next());
					if (imageId.equals(imgId))
						continue;
					PImage img = ((PImage) pageImages[p].get(imgId));
					img.setImage(null);
				}
		}
		System.gc();
		return imageId;
	}
	
	private boolean scoresAmbiguous(HashSet imageIds, StringIndex imageIdScores) {
		int maxScore = 0;
		int secondMaxScore = 0;
		for (Iterator iit = imageIds.iterator(); iit.hasNext();) {
			String iid = ((String) iit.next());
			int iidScore = imageIdScores.getCount(iid);
			if (iidScore > maxScore) {
				secondMaxScore = maxScore;
				maxScore = iidScore;
			}
			else if (iidScore > secondMaxScore)
				secondMaxScore = iidScore;
		}
		return ((maxScore / 2) < secondMaxScore);
	}
	
	private void addImageIdsAndScores(HashMap pageImages, HashMap objects, HashSet imageIds, StringIndex imageIdScores, BoundingBox pageBounds) throws IOException {
		int pageWidth = (pageBounds.right - pageBounds.left);
		int pageHeight = (pageBounds.bottom - pageBounds.top);
		float pageWidthByHeight = (((float) pageWidth) / pageHeight);
		for (Iterator iit = pageImages.keySet().iterator(); iit.hasNext();) {
			String imgId = ((String) iit.next());
			System.out.println(" - doing image " + imgId);
			PImage img = ((PImage) pageImages.get(imgId));
			
			//	get image
			BufferedImage bi = this.getImage(img, objects);
			if (bi == null) {
				System.out.println(" --> could not load image");
				iit.remove();
				continue;
			}
			System.out.println("   - got image, size is " + bi.getWidth() + " x " + bi.getHeight());
			
			/* use this to analyze cases with page images sliced into stripes,
			 * tiles, etc., to figure out how to re-assemble them */
//			//	deal with cases of page images stored in tiles
//			ImageIO.write(bi, IMAGE_FORMAT, new File(this.basePath, ("Image." + pageTime + "." + imgId + "." + IMAGE_FORMAT)));
			
			//	check image size (this factually checks for at least 72 DPI)
			if ((bi.getWidth() * 100) < (pageWidth * 98)) {
				System.out.println(" --> image too small, width is less than 98% of " + pageWidth);
				iit.remove();
				continue;
			}
			if ((bi.getHeight() * 100) < (pageHeight * 98)) {
				System.out.println(" --> image too small, height is less than 98% of " + pageHeight);
				iit.remove();
				continue;
			}
			System.out.println("   - image size is fine");
			
			//	check image proportions (must be similar to page proportions if we have an image of the page text)
			float imgWidthByHeight = (((float) bi.getWidth()) / bi.getHeight());
			if (((pageWidthByHeight / imgWidthByHeight) < 0.8) || ((imgWidthByHeight / pageWidthByHeight) < 0.8)) {
				System.out.println(" --> image out of proportion, width by hight is " + imgWidthByHeight + ", but page is " + pageWidthByHeight);
				iit.remove();
				continue;
			}
			System.out.println("   - image proportion is fine");
			
			//	check if it is a text image
			int imgScore = this.scoreImage(bi, imgId);
			imageIds.add(imgId);
			imageIdScores.add(imgId, imgScore);
			System.out.println(" --> score is " + imgScore);
		}
	}
	
	private int scoreImage(BufferedImage img, String imgId) throws IOException {
		Complex[][] fft = getFft(img);
		double max = Imaging.getMax(fft, Imaging.ADJUST_MODE_SQUARE_ROOT);
		System.out.println("Got FFT (" + fft.length + "x" + fft[0].length + "), max is " + max);
		
		ArrayList peaks = new ArrayList();
		Imaging.collectPeaks(fft, peaks, max/4, Imaging.ADJUST_MODE_SQUARE_ROOT);
		System.out.println("Got " + peaks.size() + " peaks");
		
		ArrayList sPeaks = new ArrayList();
		for (int p = 0; p < peaks.size(); p++) {
			Peak peak = ((Peak) peaks.get(p));
			if (peak.h >= (max/2))
				sPeaks.add(peak);
		}
		System.out.println("Got " + sPeaks.size() + " scoring peaks");
		
		int score = 0;
		for (int p = 0; p < sPeaks.size(); p++) {
			Peak peak = ((Peak) sPeaks.get(p));
			score += Math.abs(peak.y - fft.length/2);
		}
		System.out.println("Score for " + imgId + " is " + score);
		return score;
	}
	
	private Complex[][] getFft(BufferedImage image) throws IOException {
		boolean isPage = (image.getWidth() < image.getHeight());
		System.out.println("Image loaded (" + image.getWidth() + " x " + image.getHeight() + ") ==> " + (isPage ? "page" : "word"));
		int tdimMax = 256;
		int tdim = 1;
		while ((tdim < tdimMax) && ((tdim < image.getWidth()) || (tdim < image.getHeight())))
			tdim *= 2;
		Complex[][] fft = Imaging.getFft(image, tdim, isPage);
		return fft;
	}
	
	private static Iterator getSortedKeyIterator(Hashtable ht) {
		ArrayList keys = new ArrayList(ht.keySet());
		Collections.sort(keys, new Comparator() {
			public int compare(Object o1, Object o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});
		return keys.iterator();
	}
	
	private BufferedImage getImage(PImage pi, HashMap objects) throws IOException {
		BufferedImage bi = pi.getImage();
		System.out.println("Getting image from " + pi.data.params);
		if (bi == null) {
			bi = this.decodeImage(pi.data.params, pi.data.bytes, objects);
			System.out.println(" - image extracted, type is " + bi.getType());
			if (bi.getType() == BufferedImage.TYPE_CUSTOM) {
				BufferedImage rgbBi = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
				rgbBi.getGraphics().drawImage(bi, 0, 0, null);
				bi = rgbBi;
			}
			else if (bi.getType() == BufferedImage.TYPE_BYTE_BINARY) {}
			else if (bi.getType() == BufferedImage.TYPE_BYTE_GRAY) {}
			else if (bi.getType() != BufferedImage.TYPE_INT_RGB) {
				BufferedImage rgbBi = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
				rgbBi.getGraphics().drawImage(bi, 0, 0, null);
				bi = rgbBi;
			}
			System.out.println(" - type adjusted to " + bi.getType());
			pi.setImage(bi);
			System.out.println(" ==> image extracted");
		}
		else System.out.println(" ==> extracted before");
		return bi;
	}
	
	private BufferedImage decodeImage(Hashtable params, byte[] stream, HashMap objects) throws IOException {
		System.out.println(" ==> read " + stream.length + " bytes");
		System.out.println(" ==> Lenght parameter is " + params.get("Length"));
		System.out.println(" ==> Width parameter is " + params.get("Width"));
		System.out.println(" ==> Height parameter is " + params.get("Height"));
		int length;
		try {
			length = Integer.parseInt(params.get("Length").toString());
			System.out.println(" ==> Lenght is " + length);
		}
		catch (Exception e) {
			length = stream.length;
			System.out.println(" ==> fallback Lenght is " + length);
		}
		
		if (stream.length != length) {
			byte[] padStream = new byte[length];
			System.arraycopy(stream, 0, padStream, 0, Math.min(length, stream.length));
			if (Math.min(length, stream.length) < length)
				Arrays.fill(padStream, stream.length, length, ((byte) 13));
			stream = padStream;
		}
		
		Object filterObj = PdfParser.getObject(params, "Filter", objects);
		if (filterObj instanceof Vector) {
			if (((Vector) filterObj).size() != 0)
				filterObj = ((Vector) filterObj).get(0);
		}
		if (filterObj instanceof Reference)
			filterObj = objects.get(((Reference) filterObj).getObjectNumber() + " " + ((Reference) filterObj).getGenerationNumber());
		
		String filter = ((filterObj == null) ? null : filterObj.toString());
		System.out.println(" --> filter is " + filter);
		if ("JPXDecode".equals(filter))
			return this.decodeJPX(stream);
		else if ("JBIG2Decode".equals(filter)) {
			//	JPedal seems to be the one ...
			return this.decodeJBig2(stream);
		}
		else if ("FlateDecode".equals(filter)) {
			//	TODO use java.util.zip.GZIPInputStream instead of IcePDF
			return this.decodeFlate(stream, params, null);
		}
		else return this.decodeOther(stream, params, filter);
	}
	
	private static String getImageMagickPath() {
		String osName = System.getProperty("os.name");
		if (osName.matches("Win.*2000"))
			return "ImageMagickWin2K";
		else if (osName.matches("Win.*"))
			return "ImageMagickWinXP";
		else if (osName.matches(".*Linux.*"))
			return "ImageMagickLinux";
		else return null;
	}
	
	//	TODO make ImageMagick bridge separate class, to use also for TIFF, etc.
	
	private BufferedImage decodeJPX(byte[] stream) throws IOException {
		try {
			//	this is a breach of the data provider principle,
			//	but that's impossible to avoid if we want to use ImageMagick
			File imPath = new File(this.basePath, getImageMagickPath());
			String[] command = {
					(new File(imPath, "convert.exe").getAbsolutePath()),
					("jp2:-"),
					("png:-"),
			};
			Process im = Runtime.getRuntime().exec(command, null, imPath.getAbsoluteFile());
			OutputStream imIn = im.getOutputStream();
			imIn.write(stream);
			imIn.flush();
			imIn.close();
			BufferedImage bi = ImageIO.read(im.getInputStream());
			System.out.println(" ==> got JPX image, size is " + bi.getWidth() + " x " + bi.getHeight());
			im.waitFor();
			return bi;
		}
		catch (InterruptedException ie) {
			return null;
		}
	}
	
	private BufferedImage decodeJBig2(byte[] stream) throws IOException {
		try {
			JBIG2Decoder jbd = new JBIG2Decoder();
			jbd.decodeJBIG2(stream);
			BufferedImage bi = jbd.getPageAsBufferedImage(0);
			System.out.println(" ==> got JBIG2 image, size is " + bi.getWidth() + " x " + bi.getHeight());
			return bi;
		}
		catch (JBIG2Exception jbe) {
			System.out.println(" ==> Could not decode image: " + jbe.getMessage());
			jbe.printStackTrace(System.out);
			return null;
		}
	}
	
	private BufferedImage decodeFlate(byte[] stream, Hashtable params, ByteArrayOutputStream pageContentAssembler) throws IOException {
		SeekableInputConstrainedWrapper streamInputWrapper = new SeekableInputConstrainedWrapper(new SeekableByteArrayInputStream(stream), 0, stream.length, true);
		Stream str = new Stream(new Library(), params, streamInputWrapper);
		try {
			BufferedImage bi = str.getImage(Color.white, new Resources(new Library(), params), false);
			if (bi != null)
				System.out.println(" ==> got flate image, size is " + bi.getWidth() + " x " + bi.getHeight());
			else System.out.println(" ==> Could not decode image");
			return bi;
		}
		catch (Exception e) {
			System.out.println(" ==> Could not decode image: " + e.getMessage());
			e.printStackTrace(System.out);
			if (true) {
				FlateDecode fd = new FlateDecode(new Library(), params, new ByteArrayInputStream(stream));
				byte[] buffer = new byte[1024];
				int read;
				while ((read = fd.read(buffer)) != -1)
					System.out.print(new String(buffer, 0, read));
				System.out.println();
			}
			return null;
		}
	}
	
	private BufferedImage decodeOther(byte[] stream, Hashtable params, String filter) throws IOException {
		SeekableInputConstrainedWrapper streamInputWrapper = new SeekableInputConstrainedWrapper(new SeekableByteArrayInputStream(stream), 0, stream.length, true);
		Stream str = new Stream(new Library(), params, streamInputWrapper);
		try {
			BufferedImage bi = str.getImage(Color.white, new Resources(new Library(), params), false);
			if (bi != null)
				System.out.println(" ==> got image, size is " + bi.getWidth() + " x " + bi.getHeight());
			else System.out.println(" ==> Could not decode image");
			return bi;
		}
		catch (Exception e) {
			System.out.println(" ==> Could not decode image: " + e.getMessage());
			e.printStackTrace(System.out);
			if (true) {
				System.out.print(new String(stream, 0, stream.length));
				System.out.println();
			}
			return null;
		}
	}
}
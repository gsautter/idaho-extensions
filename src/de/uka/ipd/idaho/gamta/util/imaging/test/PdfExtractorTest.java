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
package de.uka.ipd.idaho.gamta.util.imaging.test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.UIManager;

import org.icepdf.core.pobjects.Document;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.imaging.PageAnalysis;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageInputStream;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageStore;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageStore.AbstractPageImageStore;
import de.uka.ipd.idaho.gamta.util.imaging.analyzers.PageStructurerFull;
import de.uka.ipd.idaho.gamta.util.imaging.ocr.OcrEngine;
import de.uka.ipd.idaho.gamta.util.imaging.pdf.PdfExtractor;
import de.uka.ipd.idaho.gamta.util.imaging.utilities.ImageDisplayDialog;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class PdfExtractorTest implements ImagingConstants {
	public static int aimAtPage = -1;
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		final File pdfDataPath = new File("E:/Testdaten/PdfExtract/");
//		if (true) {
//			BufferedImage bi = ImageIO.read(new File(pdfDataPath, "ants_02732.pdf.0005.png"));
//			PageImage pi = new PageImage(bi, 600);
//			FileOutputStream fos = new FileOutputStream(new File(pdfDataPath, "ants_02732.pdf.0005.png"));
//			pi.write(fos);
//			fos.close();
//			return;
//		}
		final AnalyzerDataProvider dataProvider = new AnalyzerDataProviderFileBased(pdfDataPath);
		
//		//	register image provider
//		Imaging.addImageProvider(new ImageProvider() {
//			public BufferedImage getImage(String name) {
//				if (!dataProvider.isDataAvailable(name))
//					return null;
//				try {
//					InputStream pin = dataProvider.getInputStream(name);
//					PageImage pi = new PageImage(new PageImageInputStream(pin, null));
//					pin.close();
//					return pi.image;
//				}
//				catch (IOException ioe) {
//					ioe.printStackTrace(System.out);
//					return null;
//				}
//			}
//			public BufferedImage getImage(String docId, int pageId) {
//				return this.getImage(getPageImageName(docId, pageId));
//			}
//		});
		
		//	register page image source
//		PageImageStore pis = new PageImageStore() {
//			public boolean isPageImageAvailable(String docId, int pageId) {
//				return dataProvider.isDataAvailable(getPageImageName(docId, pageId));
//			}
//			public String getPageImageName(String docId, int pageId) {
//				return PdfExtractorTest.getPageImageName(docId, pageId);
//			}
//			public PageImage getPageImage(String docId, int pageId) throws IOException {
//				String name = getPageImageName(docId, pageId);
//				if (!dataProvider.isDataAvailable(name))
//					return null;
//				InputStream pin = dataProvider.getInputStream(name);
//				PageImage pi = new PageImage(new PageImageInputStream(pin, this));
//				pin.close();
//				return pi;
//			}
//			public PageImageInputStream getPageImageAsStream(String docId, int pageId) throws IOException {
//				String name = getPageImageName(docId, pageId);
//				if (!dataProvider.isDataAvailable(name))
//					return null;
//				return new PageImageInputStream(dataProvider.getInputStream(name), this);
//			}
//			public String storePageImage(String docId, int pageId, BufferedImage image, int dpi) throws IOException {
//				String imageName = getPageImageName(docId, pageId);
//				if (dpi == 0) {
//					OutputStream imageOut = dataProvider.getOutputStream(imageName);
//					ImageIO.write(image, "png", imageOut);
//					imageOut.close();
//					return imageName;
//				}
//				PageImage pi = new PageImage(image, dpi, this);
//				try {
//					OutputStream imageOut = dataProvider.getOutputStream(imageName);
//					pi.write(imageOut);
//					imageOut.close();
//					return imageName;
//				}
//				catch (IOException ioe) {
//					ioe.printStackTrace(System.out);
//					return null;
//				}
//			}
//			public String storePageImage(String docId, int pageId, PageImage pageImage) throws IOException {
//				String imageName = getPageImageName(docId, pageId);
//				try {
//					OutputStream imageOut = dataProvider.getOutputStream(imageName);
//					pageImage.write(imageOut);
//					imageOut.close();
//					return imageName;
//				}
//				catch (IOException ioe) {
//					ioe.printStackTrace(System.out);
//					return null;
//				}
//			}
//		};
		PageImageStore pis = new AbstractPageImageStore() {
			public boolean isPageImageAvailable(String name) {
				if (!name.endsWith(IMAGE_FORMAT))
					name += ("." + IMAGE_FORMAT);
				return dataProvider.isDataAvailable(name);
			}
			public PageImageInputStream getPageImageAsStream(String name) throws IOException {
				if (!name.endsWith(IMAGE_FORMAT))
					name += ("." + IMAGE_FORMAT);
				if (!dataProvider.isDataAvailable(name))
					return null;
				return new PageImageInputStream(dataProvider.getInputStream(name), this);
			}
			public void storePageImage(String name, PageImage pageImage) throws IOException {
				if (!name.endsWith(IMAGE_FORMAT))
					name += ("." + IMAGE_FORMAT);
				try {
					OutputStream imageOut = dataProvider.getOutputStream(name);
					pageImage.write(imageOut);
					imageOut.close();
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.out);
				}
			}
		};
		PageImage.addPageImageSource(pis);
		final PdfExtractor pdfExtractor = new PdfExtractor(pdfDataPath, pis);
		
		String pdfName;
//		pdfName = "abcofevolution00mcca.pdf"; // clean single column, narrative
//												// rather than treatments
//												// JPX, JBIG2
//		pdfName = "ants_02732.pdf"; // large taxonomic key, some drawings
//									// Flate, white on black
//		pdfName = "SCZ634_Cairns_web_FINAL.pdf"; // digitally born, two column
//													// layout, many figures,
//													// tables without grid,
//													// author omissions in
//													// bibliography
//		pdfName = "ObjectTest.pdf";
//		pdfName = "5834.pdf"; // small fonts, narrow lines, lots of italics
//								// CCITTFaxDecode, multi-line dictionaries
//		pdfName = "21330.pdf"; // pages skewed, otherwise pretty clean
//								// CCITTFaxDecode
//		pdfName = "zt02879p040.pdf"; // digitally born ZooTaxa, Type3 fonts
//		pdfName = "23416.pdf"; // ca 800 page monograph, modern, multiple
//								// languages, with some cyrillic script
//		pdfName = "Test600DPI.pdf";
//		pdfName = "NyBotanicalGarden-plugtmp-17_plugin-view.pdf"; // clean
//																	// botanical
//																	// document
//																	// from NYBG
//		pdfName = "cikm1076-sautter.pdf"; // digital-born multi-column created
//											// with Adobe PDF Printer from MS
//											// Word (computer science paper in
//											// ACM layout)
//		pdfName = "TableTest.pdf"; // textual PDF with table and image
		
//		pdfName = "Menke_Cerat_1.pdf"; // bad image quality, many extended
//										// stains (photocopied staples) that do
//										// fall to white balance, in German,
//										// some pages skewed
//		pdfName = "Forsslund1964.pdf"; // in German, unregular gray background,
//										// some bleed-through, relatively dark
//										// page edges that do not fall to white
//										// balance, some hand markings in text
//		pdfName = "Schmelz+Collado-2005-Achaeta becki.pdf"; // digital-born PDF,
//															// bitmap based
//															// Type3 font,
//															// artifact-only
//															// (blank after
//															// normalization)
//															// last page
//		pdfName = "Schulz_1997.pdf"; // bad image quality, diverse
//									// images, real problems
//		pdfName = "Schulz_1997-2.pdf"; // puncher holes, dark page margins
//		pdfName = "Mellert_et_al_2000.pdf"; // pretty dirty pages,
//											// some fuzzy in
//											// addition, many tables and
//											// diagrams, 600 dpi scans
//		pdfName = "Prasse_1979_1.PDF"; // very light, plus gray
//										// background on some pages, lines
//										// overlapping or blurred together due
//										// to stencil print, fold-outs with
//										// brightness gradient in folds, likely
//										// as bad as it gets regarding
//										// challenges to image enhancement
//		pdfName = "Sansone-2012-44-121.pdf"; // three-column digitally born PDF,
//												// some problem with superscripts
//		pdfName = "Taylor_Wolters_2005.pdf"; // digitally born PDF from
//												// Elsevier, Type1C fonts
//		pdfName = "dikow_2010a.pdf"; // digitally born PDF, Type1 fonts, oversized word boxes
//		pdfName = "dikow_2012.pdf"; // digitally born PDF, Type1 fonts, mdashes
//									// on page 7
//		pdfName = "FOG Main text_ pag 1_8.pdf"; // from pro-iBiosphere, born digital, SIDs in excess of 390
//		pdfName = "Ceratolejeunea_ Lejeuneaceae.pdf"; // from pro-iBiosphere, scanned, with wide black page margins, images stored in stripes, encoded in FlateDecode or DTCDecode, DCT images OK, Flate images strange
//		pdfName = "Ceratolejeunea_ Lejeuneaceae.mono.pdf"; // from pro-iBiosphere, scanned, with wide black page margins, images restored to full using Acrobat 6
//		pdfName = "BEC_1890.pdf"; // from pro-iBiosphere, scanned, monochrome
//		pdfName = "zt03456p035.pdf"; // 2013 Zootaxa born-digital PDF
//		pdfName = "fm__1_4_295_6.pdf"; // camera-scanned file from Donat
//		pdfName = "22817.pdf"; // nicely short 2009 Zootaxa paper, Type3 font with PDF commands containing image data
		
		//	pro-iBiosphere trouble documents
//		pdfName = "Bian et al 2011.pdf"; // born-digital, cell-less table issue resolved
//		pdfName = "Cannonetal-PNAS7July2009.pdf"; // born-digital, problem with Type1C font, and word boxes off up and right (looks like hard stuff) ==> FIEXD (media box not at 0/0, but at 9/9, which seems to be exactly the offset ==> subtract lower right corner of media box from coordinates when computing word boxes in PDF page rendering)
//		pdfName = "FOC-Loranthaceae.pdf"; // born-digital, word bounds come out way too low from PDF rendering ==> FIXED (cap height was 0, substituting with ascent now in that case)
//		pdfName = "MallotusKongkandae.pdf"; // scanned, but image does not render properly in ICEPdf (comes out 0x0 from flate decode)
//		pdfName = "Nephrolepis.pdf"; // sannned, but image does not render properly in ICEPdf (comes out 0x0 from flate decode)
//		pdfName = "page0001.pdf"; // scanned, page image turned by 90 degrees, with image proportion inverse to media box
//		pdfName = "Wallace's line.pdf"; // born-digital, trouble rendering or decoding Type1C font (too many char comparisons), still have to reconstruct ArrayIndexOutOfBounds
		pdfName = "Hovenkamp and Miyamoto 2005 - Nephrolepis.pdf"; // born-digital, Blumea 2005 layout, CID Tyoe 2 fonts, fortunately with ToUnicode mappings
//		pdfName = "Van Tien Tran 2013.pdf"; // born-digital, Blumea 2013 layout, TrueType fonts, but fortunately with ToUnicode mappings
		
		//	Smithsonian sample docs
//		pdfName = "94_Norris_web_FINAL.pdf";
//		pdfName = "94_Norris_web_FINAL_23-27.pdf";
//		pdfName = "SCZ640_FisherLudwig_web_FINAL.pdf"; // born-digital, some text in the background of first page that's invisible in Acrobat
		
		//	example from NYBG
//		pdfName = "NYBGExample-Chapter6.pdf";
		
		//	example from European Journal of Taxonomy
//		pdfName = "131-898-1-PB_pages_1_11.pdf"; // born-digital, for some reasons page headings not showing up, don't seem to be in page content
		
		//	example excerpt from BHL document
//		pdfName = "BHLExcerptExample.pdf"; // mainly scanned pages, with two born-digital meta data pages as a preface, requires rethinking the "find page images for all pages" check
//		pdfName = "BHLExcerptExample.cut.pdf"; // scanned pages, born-digital meta data pages removed, but original resolution not recoverable, as media box equal to image size in pixels (72 DPI ...)
//		pdfName = "BHLExample.cut.pdf"; // scanned pages, extracted from 418 page original document
//		
//		//	example from Greg Riccardi
//		pdfName = "SampleFromGregRiccardi.pdf";
		
//		//	mixed image and OCR text from Donat
//		pdfName = "marashi_najafi_library.pdf";
		
		//	pro-iBiosphere file from Sabrina
//		pdfName = "chenopodiacea_hegi_270_281.pdf"; // scanned to own specs, 300 DPI black and white
//		pdfName = "Guianas_Cheop_61_64_176.pdf"; // scanned to own specs, 300 DPI black and white
		
		//	pro-iBiosphere file from Quentin
//		pdfName = "Chenopodium vulvaria (Chenopodiaceae) - a species extinct.pdf"; // born-digital, in Polish
//		pdfName = "Darwiniana V19 p553.pdf"; // scanned, but text represented only as in born-digital, original page image wiped clean of text and used as page background
		
		//	pro-iBiospher files from Peter (same document, run through 5 different PDF generators)
//		pdfName = "generatorTest/A conspectus of the native and naturalized species of Nephrolepis in the world created as.pdf";
//		pdfName = "generatorTest/A conspectus of the native and naturalized species of Nephrolepis in the world optimized acrobat4.pdf";
//		pdfName = "generatorTest/A conspectus of the native and naturalized species of Nephrolepis in the world optimized.pdf";
//		pdfName = "generatorTest/A conspectus of the native and naturalized species of Nephrolepis in the world printed to.pdf";
//		pdfName = "generatorTest/A conspectus of the native and naturalized species of Nephrolepis in the world saved as.pdf";
		/* they all render fine, only structure recognition makes a block out of
		 * every line due to extremely wide line margins
		 */
		
		//	Swiss spiter documents
		pdfName = "Ono2009c.pdf"; // born-digital, fonts with misleading ToUnicode mappings
//		pdfName = "OttBrescovit2003.pdf"; // born-digital, page 0 with negative line matrix entries, currently renders upside down
		
		long start = System.currentTimeMillis();
		int scaleFactor = 1;
		aimAtPage = -1; // TODO always set this to -1 for JAR export
		//	TODO try pages 12, 13, 16, 17, and 21 of Prasse 1979
		System.out.println("Aiming at page " + aimAtPage);
		
		if (false) {
			File pdfFile = new File(pdfDataPath, pdfName);
			FileInputStream pdfIn = new FileInputStream(pdfFile);
			BufferedInputStream bis = new BufferedInputStream(pdfIn);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int read;
			while ((read = bis.read(buffer, 0, buffer.length)) != -1)
				baos.write(buffer, 0, read);
			bis.close();
			byte[] bytes = baos.toByteArray();
			
			DocumentRoot doc = Gamta.newDocument(Gamta.INNER_PUNCTUATION_TOKENIZER);
			doc.setAnnotationNestingOrder(PAGE_TYPE + " block line " + WORD_ANNOTATION_TYPE);
			doc.setAttribute("docId", pdfName);
			doc.setDocumentProperty("docId", pdfName);
			Document pdfDoc = new Document();
			pdfDoc.setInputStream(new ByteArrayInputStream(bytes), "");
			doc = pdfExtractor.loadGenericPdf(doc, pdfDoc, bytes, 1, null);
			return;
		}
		
		File docFile = new File(pdfDataPath, (pdfName + ".xml"));
		DocumentRoot doc;
		if (docFile.exists() && (aimAtPage == -1)) {
			BufferedReader docReader = new BufferedReader(new InputStreamReader(new FileInputStream(docFile), "UTF-8"));
			doc = SgmlDocumentReader.readDocument(docReader);
			docReader.close();
			doc.setDocumentProperty("docId", pdfName);
		}
		else {
			File pdfFile = new File(pdfDataPath, pdfName);
			FileInputStream pdfIn = new FileInputStream(pdfFile);
			BufferedInputStream bis = new BufferedInputStream(pdfIn);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int read;
			while ((read = bis.read(buffer, 0, buffer.length)) != -1)
				baos.write(buffer, 0, read);
			bis.close();
			byte[] bytes = baos.toByteArray();
			
			doc = Gamta.newDocument(Gamta.INNER_PUNCTUATION_TOKENIZER);
			doc.setAnnotationNestingOrder(PAGE_TYPE + " block line " + WORD_ANNOTATION_TYPE);
			doc.setAttribute("docId", pdfName);
			doc.setDocumentProperty("docId", pdfName);
			Document pdfDoc = new Document();
			pdfDoc.setInputStream(new ByteArrayInputStream(bytes), "");
			
//			doc = pdfExtractor.loadImagePdf(doc, pdfDoc, bytes, scaleFactor, null);
//			doc = pdfExtractor.loadImagePdfBlocks(doc, pdfDoc, bytes, scaleFactor, null);
//			doc = pdfExtractor.loadImagePdfPages(doc, pdfDoc, bytes, scaleFactor, null);
			doc = pdfExtractor.loadTextPdf(doc, pdfDoc, bytes, null);
//			doc = pdfExtractor.loadGenericPdf(doc, pdfDoc, bytes, scaleFactor, null);
			
//			ProcessStatusMonitor psm = new ProcessStatusMonitor() {
//				public void setStep(String step) {
//					System.out.println(step);
//				}
//				public void setLabel(String text) {
//					System.out.println(text);
//					if ((aimAtPage != -1) && (text.indexOf("OCR") != -1))
//						throw new RuntimeException("NO OCR FOR NOW");
//				}
//				public void setBaseProgress(int baseProgress) {}
//				public void setMaxProgress(int maxProgress) {}
//				public void setProgress(int progress) {}
//			};
//			try {
//				doc = pdfExtractor.loadGenericPdf(doc, pdfDoc, bytes, scaleFactor, psm);
//			}
//			catch (Exception e) {
//				System.out.println(e.getMessage());
//			}
			
			if (aimAtPage == -1) {
				BufferedWriter docWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(docFile), "UTF-8"));
				AnnotationUtils.writeXML(doc, docWriter);
				docWriter.flush();
				docWriter.close();
			}
			else {
				System.out.println("PDF loaded:");
				AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
				System.out.println();
			}
		}
		
		if (true) {
			String docId = ((String) doc.getAttribute("docId"));
			MutableAnnotation[] pages = doc.getMutableAnnotations(PAGE_TYPE);
			final ImageDisplayDialog dialog = new ImageDisplayDialog(pdfName);
			for (int p = 0; p < pages.length; p++) {
				if ((aimAtPage != -1) && (p != aimAtPage))
					continue;
				PageImage pi = PageImage.getPageImage(docId, p);
//				if ("P".equals(pages[p].firstValue())) {
//					System.out.println("Page " + p + " is generic, filling in structure");
//					PageImageConverter.fillInPageRegions(pages[p], pi, null);
//				}
				System.gc();
				
//				if (pages[p].getAnnotations(MutableAnnotation.PARAGRAPH_TYPE).length == 0) {
//					System.out.println("Splitting blocks in page " + p);
//					PageAnalysis.splitIntoParagraphs(pages[p].getMutableAnnotations(BLOCK_ANNOTATION_TYPE), pi.currentDpi, null);
//					PageAnalysis.computeColumnLayout(pages[p].getMutableAnnotations(COLUMN_ANNOTATION_TYPE), pi.currentDpi);
//				}
//				if (true)
//					continue;
//				
				int scaleDown = 1;
				int displayDpi = (((aimAtPage == -1) && (pages.length > 5)) ? 150 : 300);
				while ((scaleDown * displayDpi) < pi.currentDpi)
					scaleDown++;
				System.out.println("Scaledown is " + scaleDown);
				int imageMargin = 8;
				final BufferedImage bi = new BufferedImage(((pi.image.getWidth()/scaleDown)+(imageMargin*2)), ((pi.image.getHeight()/scaleDown)+(imageMargin*2)), BufferedImage.TYPE_3BYTE_BGR);
				bi.getGraphics().setColor(Color.WHITE);
				bi.getGraphics().fillRect(0, 0, bi.getWidth(), bi.getHeight());
				bi.getGraphics().drawImage(pi.image, imageMargin, imageMargin, (pi.image.getWidth()/scaleDown), (pi.image.getHeight()/scaleDown), null);
				
				Annotation[] words = pages[p].getAnnotations(WORD_ANNOTATION_TYPE);
				for (int w = 0; w < words.length; w++) {
//					Color col = (!"".equals(words[w].getAttribute(STRING_ATTRIBUTE, "")) ? Color.YELLOW : Color.RED);
//					Color col = ("".equals(words[w].getAttribute(BOLD_ATTRIBUTE, "")) ? Color.YELLOW : Color.RED);
					Color col = ("".equals(words[w].getAttribute(ITALICS_ATTRIBUTE, "")) ? Color.YELLOW : Color.RED);
					paintBox(bi, scaleDown, words[w], col.getRGB(), imageMargin, 0);
					paintBaseline(bi, scaleDown, words[w], Color.PINK.getRGB(), imageMargin);
				}
//				
//				Annotation[] lines = pages[p].getAnnotations(LINE_ANNOTATION_TYPE);
//				for (int l = 0; l < lines.length; l++)
//					paintBox(bi, scaleDown, lines[l], Color.ORANGE.getRGB(), imageMargin, 1);
//				
//				Annotation[] cells = pages[p].getAnnotations("td");
//				for (int c = 0; c < cells.length; c++)
//					paintBox(bi, scaleDown, cells[c], Color.RED.getRGB(), imageMargin, 1);
//				
				Annotation[] blocks = pages[p].getAnnotations(BLOCK_ANNOTATION_TYPE);
				for (int b = 0; b < blocks.length; b++)
					paintBox(bi, scaleDown, blocks[b], Color.BLUE.getRGB(), imageMargin, 3);
//				
//				Annotation[] paragraphs = pages[p].getAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
//				for (int g = 0; g < paragraphs.length; g++)
//					paintBox(bi, scaleDown, paragraphs[g], Color.GREEN.getRGB(), imageMargin, 2);
//				
				Annotation[] columns = pages[p].getAnnotations(COLUMN_ANNOTATION_TYPE);
				for (int c = 0; c < columns.length; c++)
					paintBox(bi, scaleDown, columns[c], Color.DARK_GRAY.getRGB(), imageMargin, 5);
				
				Annotation[] regions = pages[p].getAnnotations(REGION_ANNOTATION_TYPE);
				for (int r = 0; r < regions.length; r++)
					paintBox(bi, scaleDown, regions[r], Color.GRAY.getRGB(), imageMargin, 5);
				
				dialog.addImage(bi, ("Page " + p));
				
				if (aimAtPage != -1) {
					System.out.println("Page " + p + ":");
					AnnotationUtils.writeXML(pages[p], new OutputStreamWriter(System.out));
					System.out.println();
				}
			}
			
			System.out.println("Document done in " + ((System.currentTimeMillis() - start) / 1000) + " seconds");
			
			dialog.setSize(800, 1000);
			dialog.setLocationRelativeTo(null);
//			if (aimAtPage == -1)
//				AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
			Thread dialogThread = new Thread() {
				public void run() {
					dialog.setVisible(true);
				}
			};
			dialogThread.start();
//			if (aimAtPage != -1)
				return;
		}
		
		File ocrDocFile = new File(pdfDataPath, (pdfName + ".ocr.xml"));
		if (ocrDocFile.exists() && (aimAtPage == -1)) {
			doc = SgmlDocumentReader.readDocument(ocrDocFile);
			doc.setDocumentProperty("docId", pdfName);
		}
		else {
			String docId = ((String) doc.getAttribute("docId"));
			MutableAnnotation[] pages = doc.getMutableAnnotations(PAGE_TYPE);
			for (int p = 0; p < pages.length; p++) {
				if ("P".equals(pages[p].firstValue()))
					continue;
				if ((aimAtPage != -1) && (p != aimAtPage))
					continue;
				if (pages[p].getAnnotations(MutableAnnotation.PARAGRAPH_TYPE).length != 0)
					continue;
				System.gc();
				
				PageImage pi = PageImage.getPageImage(docId, p);
				System.out.println("Splitting blocks in page " + p);
				PageAnalysis.splitIntoParagraphs(pages[p].getMutableAnnotations(BLOCK_ANNOTATION_TYPE), pi.currentDpi, null);
				PageAnalysis.computeColumnLayout(pages[p].getMutableAnnotations(COLUMN_ANNOTATION_TYPE), pi.currentDpi);
			}
			
			File ocrDataPath = new File("E:/Testdaten/PdfExtract/");
			OcrEngine ocrEngine = new OcrEngine(ocrDataPath);
			
//			for (int p = 0; p < pages.length; p++) {
//				if ((aimAtPage != -1) && (p != aimAtPage))
//					continue;
//				
//				Annotation[] words = pages[p].getAnnotations(WORD_ANNOTATION_TYPE);
//				int strCount = 0;
//				for (int w = 0; w < words.length; w++) {
//					if (words[w].hasAttribute(STRING_ATTRIBUTE))
//						strCount++;
//				}
//				if (words.length < (strCount * 2))
//					continue;
//				
//				PageImage pi = PageImage.getPageImage(docId, p);
//				ocrEngine.doWordOcr(pages[p], pi, null);
//			}
			
			if (aimAtPage != -1) {
				System.out.println("OCR done:");
				AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
				System.out.println();
			}
			else {
				BufferedWriter docWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ocrDocFile), "UTF-8"));
				AnnotationUtils.writeXML(doc, docWriter);
				docWriter.flush();
				docWriter.close();
			}
		}
		
		Analyzer ps = new PageStructurerFull();
		ps.setDataProvider(new AnalyzerDataProviderFileBased(pdfDataPath));
		Properties params = new Properties();
		params.setProperty(Analyzer.INTERACTIVE_PARAMETER, Analyzer.INTERACTIVE_PARAMETER);
//		doc.addTokenSequenceListener(new TokenSequenceListener() {
//			public void tokenSequenceChanged(TokenSequenceEvent change) {
//				System.out.println("   - TOKEN SEQUENCE CHANGED (at " + change.index + "):");
//				System.out.println("     - inserted: " + change.inserted);
//				System.out.println("     - removed: " + change.removed);
//			}
//		});
//		doc.addCharSequenceListener(new CharSequenceListener() {
//			public void charSequenceChanged(CharSequenceEvent change) {
//				System.out.println("   - CHAR SEQUENCE CHANGED (at " + change.offset + "):");
//				System.out.println("     - inserted: " + change.inserted);
//				System.out.println("     - removed: " + change.removed);
//			}
//		});
		String feedbackType = "";
//		feedbackType = ".singleCell";
//		feedbackType = ".multiCell";
		final String theFeedbackType = feedbackType;
		FeedbackPanel.addFeedbackService(new FeedbackService() {
			private boolean isInInvokation = false;
			public int getPriority() {
				return 11;
			}
			public boolean canGetFeedback(FeedbackPanel fp) {
				return !this.isInInvokation;
			}
			public void getFeedback(FeedbackPanel fp) {
				this.isInInvokation = true;
				try {
					String docId = fp.getProperty(FeedbackPanel.TARGET_DOCUMENT_ID_PROPERTY);
					String pageId = fp.getProperty(FeedbackPanel.TARGET_PAGE_ID_PROPERTY);
					String feedbackId = null;
//					if ((docId != null) && (pageId != null))
//						feedbackId = (docId + "-" + pageId);
					
					//	check for stored status
					if (feedbackId != null) {
						File feedbackFile = new File(pdfDataPath, (feedbackId + theFeedbackType + ".txt"));
						try {
							Properties fss = new Properties();
							StringVector fsl = StringVector.loadList(feedbackFile);
							for (int f = 0; f < fsl.size(); f++) {
								String[] fd = fsl.get(f).split("\\s*\\=\\s*", 2);
								if (fd.length == 2)
									fss.setProperty(fd[0], fd[1]);
							}
							
							//	inject stored status
							fp.setFieldStates(fss);
							fp.setStatusCode("OK");
							return;
						}
						catch (IOException ioe) {}
					}
					
					//	get actual feedback
					FeedbackPanel.getFeedback(fp);
					
					//	store status
					if (feedbackId != null) {
						File feedbackFile = new File(pdfDataPath, (feedbackId + theFeedbackType + ".txt"));
						try {
							Properties fss = fp.getFieldStates();
							StringVector fsl = new StringVector();
							for (Iterator fnit = fss.keySet().iterator(); fnit.hasNext();) {
								String fn = ((String) fnit.next());
								String fs = fss.getProperty(fn);
								fsl.addElement(fn + " = " + fs);
							}
							fsl.storeContent(feedbackFile);
						}
						catch (IOException ioe) {
							if (feedbackFile.exists())
								feedbackFile.delete();
						}
					}
				}
				finally {
					this.isInInvokation = false;
				}
			}
			public boolean isLocal() {
				return true;
			}
			public boolean isMultiFeedbackSupported() {
				return false;
			}
			public void getMultiFeedback(FeedbackPanel[] fps) throws UnsupportedOperationException {
				throw new UnsupportedOperationException("No fulti-feedback from this service.");
			}
			public void shutdown() {}
		});
		ps.process(doc, params);
		
		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
		
//		if (aimAtPage == -1)
//			AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
//		else {
//			MutableAnnotation[] pages = doc.getMutableAnnotations(PAGE_TYPE);
//			AnnotationUtils.writeXML(pages[aimAtPage], new OutputStreamWriter(System.out));
//		}
	}
	private static void paintBox(BufferedImage bi, int scaleDown, Annotation annot, int rgb, int im, int exp) {
		BoundingBox bb = BoundingBox.getBoundingBox(annot);
		if (bb == null) {
			System.out.println("Cannot paint " + annot.getType());
			return;
		}
		try {
			for (int c = ((bb.left/scaleDown) - exp); c < ((bb.right/scaleDown) + exp); c++) {
				bi.setRGB((c+im), ((bb.top/scaleDown)+im-exp), rgb);
				bi.setRGB((c+im), ((bb.bottom/scaleDown)-1+im+exp), rgb);
			}
			for (int r = ((bb.top/scaleDown) - exp); r < ((bb.bottom/scaleDown) + exp); r++) {
				bi.setRGB(((bb.left/scaleDown)+im-exp), (r+im), rgb);
				bi.setRGB(((bb.right/scaleDown)-1+im+exp), (r+im), rgb);
			}
		} catch (Exception e) {}
	}
	
	private static void paintBaseline(BufferedImage bi, int scaleDown, Annotation word, int rgb, int im) {
		BoundingBox bb = BoundingBox.getBoundingBox(word);
		int baseline = Integer.parseInt((String) word.getAttribute(BASELINE_ATTRIBUTE, "-1"));
		if ((bb == null) || (baseline < bb.top)) {
			System.out.println("Cannot paint baseline");
			return;
		}
		for (int c = (bb.left/scaleDown); c < (bb.right/scaleDown); c++)
			bi.setRGB((c+im), ((baseline/scaleDown)+im), rgb);
	}
	
	private static String getPageImageName(String docId, int pageId) {
		return (docId + "." + getPageIdString(pageId, 4) + "." + IMAGE_FORMAT);
	}
	private static String getPageIdString(int pn, int length) {
		String pns = ("" + pn);
		while (pns.length() < length)
			pns = ("0" + pns);
		return pns;
	}
}

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
package de.uka.ipd.idaho.gamta.util.imaging.ocr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging.AnalysisImage;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageConverter;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Wrapper class for Google's Tesseract OCR engine.
 * 
 * @author sautter
 */
public class OcrEngine implements ImagingConstants {
	private static final String OCR_ATTRIBUTE = "ocr";
	
	private File basePath;
	private File cachePath;
	
	/**
	 * Constructor
	 * @param basePath the root path of the individual Tesseract binary folders
	 */
	public OcrEngine(File basePath) {
		this.basePath = new File(basePath, getTesseractPath());
		this.loadCorrections();
		this.cachePath = new File(this.basePath, "cache");
		if (!this.cachePath.exists())
			this.cachePath.mkdirs();
	}
	
	private static String getTesseractPath() {
		String osName = System.getProperty("os.name");
		if (osName.matches("Win.*"))
			return "TesseractWindows";
		else if (osName.matches(".*Linux.*"))
			return "TesseractLinux";
		else {
			System.out.println("OcrEngin: unknown OS name: " + osName);
			return null;
		}
	}
	
	/**
	 * Run Tesseract on the text blocks marked in a page. This method runs OCR
	 * on the image snippes delimited by the <i>box</i> attribute of
	 * <i>block</i> annotations and stores the result in the <i>str</i>
	 * attribute, escaping line breaks as &amp;0x0A;. The page image is
	 * retrieved from Imaging via the getImage() method, invoked with the value
	 * of the argument pages <i>imageName</i> attribute. This method will also
	 * do a basic line splitting based on the OCR result, replacing the content
	 * of the text blocks with its inferred line structure.
	 * @param page the page to run Tesseract on
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @throws IOException
	 */
	public void doBlockOcr(MutableAnnotation page, ProgressMonitor psm) throws IOException {
		String pageImageName = ((String) page.getAttribute(IMAGE_NAME_ATTRIBUTE));
		PageImage pageImage = ((pageImageName == null) ? null : PageImage.getPageImage(pageImageName));
		if (pageImage == null)
			throw new IOException("Could not find page image");
		this.doBlockOcr(page, pageImage, psm);
	}
	
	/**
	 * Run Tesseract on the blocks marked in a page. This method runs OCR on the
	 * image parts delimited by the <i>box</i> attribute of <i>block</i>
	 * annotations and stores the result in the <i>str</i> attribute. This
	 * method also does a basic line splitting based on the OCR result,
	 * replacing the content of the text blocks with its inferred line
	 * structure.
	 * @param page the page to run Tesseract on
	 * @param pageImage the image of the page
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @throws IOException
	 */
	public void doBlockOcr(MutableAnnotation page, PageImage pageImage, ProgressMonitor psm) throws IOException {
		if (psm == null)
			psm = ProgressMonitor.dummy;
		
		//	get blocks
		MutableAnnotation[] blocks = page.getMutableAnnotations(BLOCK_ANNOTATION_TYPE);
		if (blocks.length == 0)
			return;
		
		//	test for previous OCR
		int noOcr = 0;
		int noBox = 0;
		for (int b = 0; b < blocks.length; b++) {
			if (!blocks[b].hasAttribute(STRING_ATTRIBUTE))
				noOcr++;
			else if (!blocks[b].hasAttribute(BOUNDING_BOX_ATTRIBUTE))
				noBox++;
		}
		if (noOcr == 0)
			return;
		else if (noBox != 0)
			throw new IOException("There are blocks without a bounding box");
		
		//	work block by block
		for (int b = 0; b < blocks.length; b++) {
			psm.setInfo("Doing block " + b);
			psm.setProgress((b * 100) / blocks.length);
			
			//	we've seen this one before TODO use ocr attribute only !!!
			if (blocks[b].hasAttribute(STRING_ATTRIBUTE)) {
				psm.setInfo(" ==> OCRed before");
				continue;
			}
			
			//	get bounding box
			BoundingBox blockBox = BoundingBox.getBoundingBox(blocks[b]);
			psm.setInfo(" - bounding box is " + blockBox);
			
			//	build text image
//			System.out.println("Doing block " + blockBox.toString());
			BufferedImage blockTextImage = this.getOcrImage(blockBox, pageImage);
			
			//	do OCR
			TWord[] blockWords = this.doBlockOcr(b, blockTextImage, pageImage.currentDpi, psm, false);
			if (blockWords.length == 0) {
				psm.setInfo(" ==> no text found");
				continue;
			}
			psm.setInfo(" ==> found " + blockWords.length + " words:");
			for (int w = 0; w < blockWords.length; w++)
				psm.setInfo("   - '" + blockWords[w].str + "' at " + blockWords[w].box.toString());
			
			//	index and sort Tesseract OCR result
			ArrayList blockWordBoxList = new ArrayList();
			HashMap blockWordsByBoxes = new HashMap();
			for (int w = 0; w < blockWords.length; w++) {
				if (blockWords[w].str.length() == 0)
					continue;
				BoundingBox blockWordBox = new BoundingBox((blockWords[w].box.left + blockBox.left), (blockWords[w].box.right + blockBox.left), (blockWords[w].box.top + blockBox.top), (blockWords[w].box.bottom + blockBox.top));
				blockWordBoxList.add(blockWordBox);
				blockWordsByBoxes.put(blockWordBox.toString(), blockWords[w]);
			}
			BoundingBox[] blockWordBoxes = ((BoundingBox[]) blockWordBoxList.toArray(new BoundingBox[blockWordBoxList.size()]));
			Arrays.sort(blockWordBoxes, new Comparator() {
				public int compare(Object o1, Object o2) {
					BoundingBox bb1 = ((BoundingBox) o1);
					BoundingBox bb2 = ((BoundingBox) o2);
					if (bb1.bottom <= bb2.top)
						return -1;
					if (bb2.bottom <= bb1.top)
						return 1;
					if (bb1.right <= bb2.left)
						return -1;
					if (bb2.right <= bb1.left)
						return 1;
					return 0;
				}
			});
			
			//	do own content analysis to find words missed by Tessearct (tends to happen with page numbers ...)
			MutableAnnotation cBlockDoc = Gamta.copyDocument(blocks[b]);
			MutableAnnotation cBlock = cBlockDoc.addAnnotation(BLOCK_ANNOTATION_TYPE, 0, cBlockDoc.size());
			cBlock.copyAttributes(blocks[b]);
			psm.setInfo(" - analyzing copy " + cBlock.toXML());
			PageImageConverter.fillInTextBlockStructure(cBlock, pageImage, ((BoundingBox[]) null), psm);
			
			//	get words
			Annotation[] cBlockWords = cBlock.getAnnotations(WORD_ANNOTATION_TYPE);
			psm.setInfo(" ==> found " + cBlockWords.length + " own words:");
			for (int w = 0; w < cBlockWords.length; w++)
				psm.setInfo("   - " + BoundingBox.getBoundingBox(cBlockWords[w]));
			
			//	index and sort result of own layout analysis
			TreeMap cBlockWordsByBoxes = new TreeMap(new Comparator() {
				public int compare(Object o1, Object o2) {
					BoundingBox bb1 = ((BoundingBox) o1);
					BoundingBox bb2 = ((BoundingBox) o2);
					if (bb1.bottom <= bb2.top)
						return -1;
					if (bb2.bottom <= bb1.top)
						return 1;
					if (bb1.right <= bb2.left)
						return -1;
					if (bb2.right <= bb1.left)
						return 1;
					return 0;
				}
			});
			for (int w = 0; w < cBlockWords.length; w++) {
				BoundingBox cBlockWordBox = BoundingBox.getBoundingBox(cBlockWords[w]);
				if (cBlockWordBox != null)
					cBlockWordsByBoxes.put(cBlockWordBox, cBlockWords[w]);
			}
			
			//	subtract Tesseract result TODO retain words whose Tessearcted counterpart has an empty string
			for (int w = 0; w < blockWordBoxes.length; w++)
				for (Iterator cbwbit = cBlockWordsByBoxes.keySet().iterator(); cbwbit.hasNext();) {
					BoundingBox cBlockWordBox = ((BoundingBox) cbwbit.next());
					if (blockWordBoxes[w].bottom <= cBlockWordBox.top)
						break;
					if (cBlockWordBox.bottom <= blockWordBoxes[w].top)
						continue;
					if (blockWordBoxes[w].right <= cBlockWordBox.left)
						break;
					if (cBlockWordBox.right <= blockWordBoxes[w].left)
						continue;
					cbwbit.remove();
				}
			psm.setInfo(" ==> " + cBlockWordsByBoxes.size() + " words missed by Tesseract");
			
			//	individually OCR whatever Tesseract did not find in full block pass
			psm.setInfo(" - recovering possible words missed by Tesseract");
			int cBlockWordNr = 0;
			for (Iterator cbwbit = cBlockWordsByBoxes.keySet().iterator(); cbwbit.hasNext();) {
				BoundingBox cBlockWordBox = ((BoundingBox) cbwbit.next());
				psm.setInfo("   - OCRing " + cBlockWordBox.toString());
				BufferedImage cBlockWordImage = this.getOcrImage(cBlockWordBox, pageImage);
				TWord[] cBlockWordContent = this.doBlockOcr(((b * 100) + cBlockWordNr++), cBlockWordImage, pageImage.currentDpi, psm, false);
				for (int w = 0; w < cBlockWordContent.length; w++) {
					psm.setInfo("     - '" + cBlockWordContent[w].str + "' at " + cBlockWordContent[w].box.toString());
					if (cBlockWordContent[w].str.length() == 0)
						continue;
					BoundingBox cBlockWordContentBox = new BoundingBox((cBlockWordContent[w].box.left + cBlockWordBox.left), (cBlockWordContent[w].box.right + cBlockWordBox.left), (cBlockWordContent[w].box.top + cBlockWordBox.top), (cBlockWordContent[w].box.bottom + cBlockWordBox.top));
					blockWordBoxList.add(cBlockWordContentBox);
					blockWordsByBoxes.put(cBlockWordContentBox.toString(), cBlockWordContent[w]);
				}
			}
			
			//	union with original Tesseract result
			if (blockWordBoxList.size() != blockWordBoxes.length) {
				blockWordBoxes = ((BoundingBox[]) blockWordBoxList.toArray(new BoundingBox[blockWordBoxList.size()]));
				Arrays.sort(blockWordBoxes, new Comparator() {
					public int compare(Object o1, Object o2) {
						BoundingBox bb1 = ((BoundingBox) o1);
						BoundingBox bb2 = ((BoundingBox) o2);
						if (bb1.bottom <= bb2.top)
							return -1;
						if (bb2.bottom <= bb1.top)
							return 1;
						if (bb1.right <= bb2.left)
							return -1;
						if (bb2.right <= bb1.left)
							return 1;
						return 0;
					}
				});
			}
			
			//	fill in text block structure based on combined OCR result
			PageImageConverter.fillInTextBlockStructure(blocks[b], pageImage, blockWordBoxes, psm);
			PageImageConverter.analyzeLineWordMetrics(blocks[b], pageImage, psm);
			
			//	add OCR result to words
			Annotation[] words = blocks[b].getAnnotations(WORD_ANNOTATION_TYPE);
			for (int w = 0; w < words.length; w++) {
				TWord blockWord = ((TWord) blockWordsByBoxes.remove(words[w].getAttribute(BOUNDING_BOX_ATTRIBUTE)));
				if (blockWord != null)
					words[w].setAttribute(STRING_ATTRIBUTE, blockWord.str);
			}
			psm.setInfo(" ==> " + blockWordsByBoxes.size() + " words left unassigned");
			if (blockWordsByBoxes.isEmpty())
				continue;
			
			//	as words might be merged by block structure analysis, check if those left fall within some word
			for (int w = 0; w < words.length; w++) {
				if (words[w].hasAttribute(STRING_ATTRIBUTE))
					continue;
				BoundingBox wordBox = BoundingBox.getBoundingBox(words[w]);
				if (wordBox == null)
					continue;
				ArrayList containedBlockWords = new ArrayList();
				for (Iterator bwit = blockWordsByBoxes.values().iterator(); bwit.hasNext();) {
					TWord cBlockWord = ((TWord) bwit.next());
					if (wordBox.bottom <= (cBlockWord.box.top + blockBox.top))
						continue;
					if ((cBlockWord.box.bottom + blockBox.top) <= wordBox.top)
						continue;
					if (wordBox.right <= (cBlockWord.box.left + blockBox.left))
						continue;
					if ((cBlockWord.box.right + blockBox.left) <= wordBox.left)
						continue;
					containedBlockWords.add(cBlockWord);
					bwit.remove();
				}
				if (containedBlockWords.isEmpty())
					continue;
				Collections.sort(containedBlockWords, new Comparator() {
					public int compare(Object o1, Object o2) {
						BoundingBox bb1 = ((TWord) o1).box;
						BoundingBox bb2 = ((TWord) o2).box;
						return (bb1.left - bb2.left);
					}
				});
				StringBuffer str = new StringBuffer();
				for (int c = 0; c < containedBlockWords.size(); c++)
					str.append(((TWord) containedBlockWords.get(c)).str);
				words[w].setAttribute(STRING_ATTRIBUTE, str.toString());
			}
			psm.setInfo(" ==> " + blockWordsByBoxes.size() + " words left unsalvaged");
		}
		psm.setProgress(100);
	}
	
	private TWord[] doBlockOcr(int blockNr, BufferedImage blockTextImage, int dpi, ProgressMonitor psm, boolean addPrefix) throws IOException {
		
		//	add prefix image
		final int rightShift;
		if (addPrefix) {
			BufferedImage linePrefixImage = this.getSeparatorImage(blockTextImage.getHeight() - (2 * lineTextImageMargin));
			int linePrefixMargin = (dpi / 30);
			
			BufferedImage pBlockTextImage = new BufferedImage(((linePrefixImage.getWidth() + linePrefixMargin + blockTextImage.getWidth()) + (2 * lineTextImageMargin)), blockTextImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			pBlockTextImage.getGraphics().setColor(Color.WHITE);
			pBlockTextImage.getGraphics().fillRect(0, 0, pBlockTextImage.getWidth(), pBlockTextImage.getHeight());
			
			int rgb;
			for (int c = 0; c < linePrefixImage.getWidth(); c++) {
				for (int r = 0; r < linePrefixImage.getHeight(); r++) try {
					rgb = linePrefixImage.getRGB(c, r);
					pBlockTextImage.setRGB((lineTextImageMargin + c), (lineTextImageMargin + r), rgb);
				}
				catch (Exception e) {
//					System.out.println("   - line text image is " + lineTextImage.getWidth() + " x " + lineTextImage.getHeight());
//					System.out.println("   - line separator image is " + lineSeparatorImage.getWidth() + " x " + lineSeparatorImage.getHeight());
//					System.out.println("   - lsi-x is " + (wordLeft - wordGap - lineSeparatorImage.getWidth() + c));
//					System.out.println("   - lsi-y is " + (lineTextImageMargin + baseline + separatorDownPush - lineSeparatorImage.getHeight() + r));
//					System.out.println("   --> ERROR");
////					throw new RuntimeException(e);
					e.printStackTrace(System.out);
					c = linePrefixImage.getWidth();
				}
			}
			
			int blockLeft = (linePrefixImage.getWidth() + linePrefixMargin + lineTextImageMargin);
			rightShift = blockLeft;
			for (int c = 0; c < blockTextImage.getWidth(); c++)
				for (int r = 0; r < blockTextImage.getHeight(); r++) {
					rgb = blockTextImage.getRGB(c, r);
					pBlockTextImage.setRGB((c + blockLeft), r, rgb);
				}
			
			blockTextImage = pBlockTextImage;
		}
		else rightShift = 0;
		
		//	store text image
		String blockName = (((blockNr < 1000) ? "0" : "") + ((blockNr < 100) ? "0" : "") + ((blockNr < 10) ? "0" : "") + blockNr);
		String blockTextImageName = ("cache/block." + blockName + ".b." + IMAGE_FORMAT);
		ImageIO.write(blockTextImage, IMAGE_FORMAT, new File(this.basePath, blockTextImageName));
		if (DEBUG_TESSERACT_PROCESS_CONTROL) System.out.println(" - text block image stored");
		
		//	run Tesseract process
		String blockTextName = ("cache/block." + blockName + ".b");
		String[] blockTextCommand = {
				(new File(this.basePath, "tesseract.exe").getAbsolutePath()),
				(blockTextImageName),
				(blockTextName),
//				("-l fra"), // does not seem to help all too much
				("tesseract_config.cnfg"),
		};
		
		final long tesseractStartTime = System.currentTimeMillis();
		final Process tesseract = Runtime.getRuntime().exec(blockTextCommand, null, this.basePath);
		if (DEBUG_TESSERACT_PROCESS_CONTROL) System.out.println(" - Tesseract started");
		if (false)
			relayOutput(tesseract, psm);
		
		/* We have to use a timeout (20 seconds right now) and a corresponding
		 * guard thread because Tesseract seems to hang for some images after
		 * writing the result file, which results in Process.waitFor() not
		 * returning and hanging the loading thread.
		 */
		final File blockTextFile = new File(this.basePath, (blockTextName + ".html"));
		final boolean[] tesseractFinished = {false};
		final Thread current = Thread.currentThread();
		final Thread guard = new Thread() {
			public void run() {
				if (DEBUG_TESSERACT_PROCESS_CONTROL) System.out.println(" - Guard started");
				int wait = 200;
				while (wait-- > 0) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException ie) {}
					if (tesseractFinished[0]) {
						if (DEBUG_TESSERACT_PROCESS_CONTROL) System.out.println(" - Guard terminated");
						return;
					}
					if (blockTextFile.exists() && (blockTextFile.lastModified() > tesseractStartTime) && (wait > 20)) {
						wait = 20;
						if (DEBUG_TESSERACT_PROCESS_CONTROL) System.out.println(" - Guard waiting rounds reduced to " + wait + " because result file exists");
					}
					if (DEBUG_TESSERACT_PROCESS_CONTROL) System.out.println(" - Guard waiting round finished, will take " + wait + " more");
				}
				if (!tesseractFinished[0])
					current.interrupt();
				if (DEBUG_TESSERACT_PROCESS_CONTROL) System.out.println(" - Guard terminated");
			}
		};
		guard.start();
		try {
			tesseract.waitFor();
			tesseractFinished[0] = true;
			if (DEBUG_TESSERACT_PROCESS_CONTROL) System.out.println(" - Tesseract finished");
		}
		catch (InterruptedException ie) {
			/* Sun specified workaround for interrupted flag not being reset
			 * when interrupting Process.waitFor(), from
			 * http://bugs.sun.com/view_bug.do?bug_id=6420270
			 */
			Thread.interrupted();
			if (DEBUG_TESSERACT_PROCESS_CONTROL) {
				System.out.println(" - Tesseract killed");
				ie.printStackTrace(System.out);
			}
		}
		try {
			/* We have to wait for guard to terminate and then clear the
			 * interrupted flag here in case the Tesseract finished flag was set
			 * after the guard checked it. Synchronized blocks would be the
			 * elegant way of doing this, but Process.waitFor() seems to be
			 * holding on to its monitors, which results in a deadlock.
			 */
			guard.join();
			Thread.interrupted();
			if (DEBUG_TESSERACT_PROCESS_CONTROL) System.out.println(" - Inerrupted flag cleared");
		} catch (InterruptedException ie) {}
		
		//	load text (Tesseract outputs UTF-8 !!!)
		if (!blockTextFile.exists()) {
			if (DEBUG_TESSERACT_PROCESS_CONTROL) System.out.println(" - Tesseract result file does not exist");
			return new TWord[0];
		}
		BufferedReader blockTextReader = new BufferedReader(new InputStreamReader(new FileInputStream(blockTextFile), "UTF-8"));
		final ArrayList blockWords = new ArrayList();
		parser.stream(blockTextReader, new TokenReceiver() {
			String str = null;
			BoundingBox box = null;
			public void storeToken(String token, int treeDepth) throws IOException {
				if (html.isTag(token)) {
					if (!"span".equalsIgnoreCase(html.getType(token)))
						return;
					if (html.isEndTag(token)) {
						if ((this.str != null) && (this.box != null))
							blockWords.add(new TWord(this.str, this.box));
						this.str = null;
						this.box = null;
					}
					else {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
						if (tnas.getAttribute("class", "").endsWith("ocr_word")) {
							String[] bBoxData = tnas.getAttribute("title", "").trim().split("\\s+");
							if (bBoxData.length == 5)
								this.box = new BoundingBox((Integer.parseInt(bBoxData[1]) - rightShift - lineTextImageMargin), (Integer.parseInt(bBoxData[3]) - rightShift - lineTextImageMargin), (Integer.parseInt(bBoxData[2]) - lineTextImageMargin), (Integer.parseInt(bBoxData[4]) - lineTextImageMargin));
						}
					}
				}
				else if (this.box != null)
					this.str = IoTools.prepareForPlainText(token.trim());
			}
			public void close() throws IOException {}
		});
		blockTextReader.close();
		System.out.println(" - Tesseract result loaded");
		
		//	catch empty blocks
		if (blockWords.isEmpty()) {
			
			//	even prefix did not help
			if (addPrefix) {
				psm.setInfo(" --> no text at all in block " + blockNr);
				return new TWord[0];
			}
			
			//	as this tends to happen in number-only blocks, try again with prefix
			else {
				psm.setInfo(" --> no text in block " + blockNr + ", re-trying with letter prefix");
				return this.doBlockOcr(blockNr, blockTextImage, dpi, psm, true);
			}
		}
		
		//	cut prefix
		if (addPrefix) {
			TWord firstWord = ((TWord) blockWords.get(0));
			if (wordSeparatorString.equals(firstWord.str))
				blockWords.remove(0);
			else if (firstWord.str.startsWith(wordSeparatorString))
				blockWords.set(0, new TWord(firstWord.str.substring(wordSeparatorString.length()).trim(), new BoundingBox((firstWord.box.left + rightShift), firstWord.box.right, firstWord.box.top, firstWord.box.bottom)));
		}
		
		//	return words
		return ((TWord[]) blockWords.toArray(new TWord[blockWords.size()]));
	}
	
	private static final boolean DEBUG_TESSERACT_PROCESS_CONTROL = false;
	
	private static Html html = new Html();
	private static Parser parser = new Parser(html);
	
	private class TWord {
		String str;
		BoundingBox box;
		TWord(String str, BoundingBox box) {
			this.str = str;
			this.box = box;
		}
	}
	
	/**
	 * Run Tesseract on the lines marked in a page. This method runs OCR on the
	 * image parts delimited by the <i>box</i> attribute of <i>line</i>
	 * annotations and stores the result in the <i>str</i> attribute. This
	 * method also splits a line into words based on the OCR result, replacing
	 * the content of the line with its inferred line structure. If a line
	 * actually consists of multiple lines clinging together, this method splits
	 * them apart.
	 * @param page the page to run Tesseract on
	 * @param pageImage the image of the page
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @throws IOException
	 */
	public void doLineOcr(MutableAnnotation page, PageImage pageImage, ProgressMonitor psm) throws IOException {
		if (psm == null)
			psm = ProgressMonitor.dummy;
		
		//	get lines
		MutableAnnotation[] lines = page.getMutableAnnotations(LINE_ANNOTATION_TYPE);
		if (lines.length == 0)
			return;
		
		//	test for previous OCR
		int noOcr = 0;
		int noBox = 0;
		for (int l = 0; l < lines.length; l++) {
			if (!lines[l].hasAttribute(STRING_ATTRIBUTE))
				noOcr++;
			else if (!lines[l].hasAttribute(BOUNDING_BOX_ATTRIBUTE))
				noBox++;
		}
		if (noOcr == 0)
			return;
		else if (noBox != 0)
			throw new IOException("There are lines without a bounding box");
		
		//	work line by line
		for (int l = 0; l < lines.length; l++) {
			psm.setInfo("Doing line " + l);
			psm.setProgress((l * 100) / lines.length);
			
			//	we've seen this one before TODO use ocr attribute only !!!
			if (lines[l].hasAttribute(STRING_ATTRIBUTE)) {
				psm.setInfo(" ==> OCRed before");
				continue;
			}
			
			//	get bounding box
			BoundingBox lineBox = BoundingBox.getBoundingBox(lines[l]);
			
			//	build text image
//			System.out.println("Doing block " + blockBox.toString());
			BufferedImage lineTextImage = this.getOcrImage(lineBox, pageImage);
			
			//	do OCR
			String[] textLines = this.doLineOcr(l, lineTextImage, pageImage.currentDpi, lines[l], psm, false);
			if (textLines.length == 0) {
				psm.setInfo(" ==> no text found");
				continue;
			}
			psm.setInfo(" ==> found " + textLines.length + " lines of text:");
			for (int t = 0; t < textLines.length; t++)
				psm.setInfo("   - '" + textLines[t] + "'");
			
			//	fill in block structure based on OCR result
//			PageImageConverter.fillInTextBlockStructure(blocks[b], pageImage, lines, psm);
			/* ==> seems to take 3-5 seconds per block (on main memory swapping
			 * P4 CPU, 1.5-2 on Centrino), consider deactivating it for Plazi
			 * dirty bucket and moving to dedicated analyzer (it's not required
			 * for page number recognition, after all) */
			//	==> problem seems to have been re-wrapping of page image for each block, and thus solved with called method overhaul
//			long start = System.currentTimeMillis();
			PageImageConverter.fillInTextBlockStructure(lines[l], pageImage, textLines, psm);
//			System.out.println("Block structure done in " + (System.currentTimeMillis() - start) + " ms");
			
			//	TODO_not assign whitespace tokenized chops of text lines to words
			
			//	TODO_not do paragraph analysis
		}
		
		//	clean up lines
		AnnotationFilter.removeDuplicates(page, LINE_ANNOTATION_TYPE);
		AnnotationFilter.removeOuter(page, LINE_ANNOTATION_TYPE);
		
		//	finally ...
		psm.setProgress(100);
	}
	
	private String[] doLineOcr(int blockNr, BufferedImage blockTextImage, int dpi, Annotation block, ProgressMonitor psm, boolean addPrefix) throws IOException {
		
		//	add prefix image
		if (addPrefix) {
			BufferedImage linePrefixImage = this.getSeparatorImage(blockTextImage.getHeight() - (2 * lineTextImageMargin));
			int linePrefixMargin = (dpi / 30);
			
			BufferedImage pBlockTextImage = new BufferedImage(((linePrefixImage.getWidth() + linePrefixMargin + blockTextImage.getWidth()) + (2 * lineTextImageMargin)), blockTextImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			pBlockTextImage.getGraphics().setColor(Color.WHITE);
			pBlockTextImage.getGraphics().fillRect(0, 0, pBlockTextImage.getWidth(), pBlockTextImage.getHeight());
			
			int rgb;
			for (int c = 0; c < linePrefixImage.getWidth(); c++) {
				for (int r = 0; r < linePrefixImage.getHeight(); r++) try {
					rgb = linePrefixImage.getRGB(c, r);
					pBlockTextImage.setRGB((lineTextImageMargin + c), (lineTextImageMargin + r), rgb);
				}
				catch (Exception e) {
//					System.out.println("   - line text image is " + lineTextImage.getWidth() + " x " + lineTextImage.getHeight());
//					System.out.println("   - line separator image is " + lineSeparatorImage.getWidth() + " x " + lineSeparatorImage.getHeight());
//					System.out.println("   - lsi-x is " + (wordLeft - wordGap - lineSeparatorImage.getWidth() + c));
//					System.out.println("   - lsi-y is " + (lineTextImageMargin + baseline + separatorDownPush - lineSeparatorImage.getHeight() + r));
//					System.out.println("   --> ERROR");
////					throw new RuntimeException(e);
					e.printStackTrace(System.out);
					c = linePrefixImage.getWidth();
				}
			}
			
			int blockLeft = (linePrefixImage.getWidth() + linePrefixMargin + lineTextImageMargin);
			for (int c = 0; c < blockTextImage.getWidth(); c++)
				for (int r = 0; r < blockTextImage.getHeight(); r++) {
					rgb = blockTextImage.getRGB(c, r);
					pBlockTextImage.setRGB((c + blockLeft), r, rgb);
				}
			
			blockTextImage = pBlockTextImage;
		}
		
		//	store text image
		String blockName = (((blockNr < 1000) ? "0" : "") + ((blockNr < 100) ? "0" : "") + ((blockNr < 10) ? "0" : "") + blockNr);
		String blockTextImageName = ("cache/block." + blockName + ".b." + IMAGE_FORMAT);
		ImageIO.write(blockTextImage, IMAGE_FORMAT, new File(this.basePath, blockTextImageName));
		
		//	run Tesseract process
		String blockTextName = ("cache/block." + blockName + ".b");
		String[] blockTextCommand = {
				(new File(this.basePath, "tesseract.exe").getAbsolutePath()),
				(blockTextImageName),
				(blockTextName),
//				("-l fra"), // does not seem to help all too much
		};
		Process lineTesseract = Runtime.getRuntime().exec(blockTextCommand, null, this.basePath);
		if (false)
			relayOutput(lineTesseract, psm);
		try {
			lineTesseract.waitFor();
		}
		catch (InterruptedException ie) {
			ie.printStackTrace(System.out);
		}
		
		//	load text (Tesseract outputs UTF-8 !!!)
		File blockTextFile = new File(this.basePath, (blockTextName + ".txt"));
		BufferedReader blockTextReader = new BufferedReader(new InputStreamReader(new FileInputStream(blockTextFile), "UTF-8"));
		StringVector blockText = StringVector.loadList(blockTextReader);
		blockTextReader.close();
		
		//	cut leading and tailing blank lines
		while ((blockText.size() > 0) && (blockText.get(0).trim().length() == 0))
			blockText.remove(0);
		while ((blockText.size() > 0) && (blockText.get((blockText.size()-1)).trim().length() == 0))
			blockText.remove(blockText.size()-1);
		
		//	catch empty blocks
		if (blockText.isEmpty()) {
			
			//	even prefix did not help
			if (addPrefix) {
				psm.setInfo(" --> no text at all in block " + blockNr);
				return new String[0];
			}
			
			//	as this tends to happen in number-only blocks, try again with prefix
			else {
				psm.setInfo(" --> no text in block " + blockNr + ", re-trying with letter prefix");
				return this.doLineOcr(blockNr, blockTextImage, dpi, block, psm, true);
			}
		}
		
		//	cut prefix
		if (addPrefix) {
			String firstLineString = blockText.get(0);
			String[] firstLineParts = firstLineString.split(capitalSeparatorSplitPattern, 2);
			
			//	nothing left
			if (firstLineParts.length == 0)
				return new String[0];
			
			//	only one part, let's see what we have
			else if (firstLineParts.length == 1) {
				if ("".equals(firstLineParts[0]))
					return new String[0];
				else blockText.set(0, firstLineParts[0]);
			}
			
			//	two parts, check if any one is non-empty
			else if ("".equals(firstLineParts[0])) {
				if ("".equals(firstLineParts[1]))
					return new String[0];
				else blockText.set(0, firstLineParts[1]);
			}
			else blockText.set(0, firstLineParts[0]);
		}
		
		//	store text
		String blockTextString = blockText.concatStrings("\n");
		block.setAttribute(STRING_ATTRIBUTE, blockTextString);
		block.setAttribute(OCR_ATTRIBUTE, blockTextString);
		
		//	return lines
		return blockText.toStringArray();
	}
	
	/**
	 * Run Tesseract on the words marked in a page. This method runs OCR on the
	 * image snippes delimited by the <i>box</i> attribute of <i>word</i>
	 * annotations and stores the result in the <i>str</i> attribute. The page
	 * image is retrieved from Imaging via the getImage() method, invoked with
	 * the value of the argument pages <i>imageName</i> attribute.
	 * @param page the page to run Tesseract on
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @throws IOException
	 */
	public void doWordOcr(MutableAnnotation page, ProgressMonitor psm) throws IOException {
		String pageImageName = ((String) page.getAttribute(IMAGE_NAME_ATTRIBUTE));
		PageImage pageImage = ((pageImageName == null) ? null : PageImage.getPageImage(pageImageName));
		if (pageImage == null)
			throw new IOException("Could not find page image");
		this.doWordOcr(page, pageImage, psm);
	}
	
	/**
	 * Run Tesseract on the words marked in a page. This method runs OCR on the
	 * image snippes delimited by the <i>box</i> attribute of <i>word</i>
	 * annotations and stores the result in the <i>str</i> attribute.
	 * @param page the page to run Tesseract on
	 * @param pageImage the image of the page
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @throws IOException
	 */
	public void doWordOcr(MutableAnnotation page, PageImage pageImage, ProgressMonitor psm) throws IOException {
		if (psm == null)
			psm = ProgressMonitor.dummy;
		
		//	get words
		Annotation[] words = page.getAnnotations(WORD_ANNOTATION_TYPE);
		if (words.length == 0)
			return;
		
		//	test for previous OCR
		int noOcr = 0;
		int noBox = 0;
		for (int w = 0; w < words.length; w++) {
			if (!words[w].hasAttribute(STRING_ATTRIBUTE))
				noOcr++;
			else if (!words[w].hasAttribute(BOUNDING_BOX_ATTRIBUTE))
				noBox++;
		}
		if (noOcr == 0)
			return;
		else if (noBox != 0)
			throw new IOException("There are words without a bounding box");
		
		//	get minimum word margin
		int minWordMargin = (pageImage.currentDpi / 30); // TODO find out if this makes sense (will turn out in the long haul only, though)
		
		//	work line by line
		MutableAnnotation[] lines = page.getMutableAnnotations(LINE_ANNOTATION_TYPE);
		for (int l = 0; l < lines.length; l++) {
			psm.setInfo("Doing line " + l);
			psm.setProgress((l * 100) / lines.length);
			int rgb;
			
			//	get words
			words = lines[l].getAnnotations(WORD_ANNOTATION_TYPE);
			if (words.length == 0)
				continue;
			
			//	get bounding boxes
			BoundingBox[] wbbs = new BoundingBox[words.length];
			
			//	compute image dimensions
			wbbs[0] = BoundingBox.getBoundingBox(words[0]);
			int lineTop = wbbs[0].top;
			int lineBottom = wbbs[0].bottom;
			int wordWidth = (wbbs[0].right - wbbs[0].left);
			int wordGap = minWordMargin;
			for (int w = 1; w < words.length; w++) {
				wbbs[w] = BoundingBox.getBoundingBox(words[w]);
				lineTop = Math.min(lineTop, wbbs[w].top);
				lineBottom = Math.max(lineBottom, wbbs[w].bottom);
				wordWidth = Math.max(wordWidth, (wbbs[w].right - wbbs[w].left));
				wordGap = Math.max(wordGap, (wbbs[w].left - wbbs[w-1].right));
			}
			psm.setInfo(" - line top is " + lineTop + ", line height is " + (lineBottom - lineTop));
			int wordHeight = (lineBottom - lineTop + (2 * lineTextImageMargin));
			int maxShear = ((int) Math.round(((double) (lineBottom - lineTop)) * Math.tan((Math.PI / 180) * italicShearDeg)));
			int lineIndent = wbbs[0].left;
			
			//	prepare baseline computation
			BufferedImage lineImage = new BufferedImage((wbbs[wbbs.length-1].right - wbbs[0].left), (lineBottom - lineTop), BufferedImage.TYPE_BYTE_GRAY);
			for (int c = 0; c < lineImage.getWidth(); c++) {
				for (int r = 0; r < lineImage.getHeight(); r++) {
					rgb = pageImage.image.getRGB((c + lineIndent - pageImage.leftEdge), (r + lineTop - pageImage.topEdge));
					lineImage.setRGB(c, r, rgb);
				}
			}
			AnalysisImage li = Imaging.wrapImage(lineImage, null);
			ImagePartRectangle lr = Imaging.getContentBox(li);
			ImagePartRectangle[] wrs = new ImagePartRectangle[words.length];
			for (int w = 0; w < words.length; w++)
				wrs[w] = lr.getSubRectangle((wbbs[w].left - lineIndent), (wbbs[w].right - lineIndent), (wbbs[w].top - lineTop), (wbbs[w].bottom - lineTop));
			
			//	compute baseline for each word, and make skewed lines horizontal
			int[] wordBaselines = new int[words.length];
			for (int w = 0; w < words.length; w++) {
				wordBaselines[w] = Integer.parseInt((String) words[w].getAttribute(BASELINE_ATTRIBUTE, "-1"));
				if (wordBaselines[w] == -1) {
					ImagePartRectangle[] nwrs = {wrs[w]};
					wordBaselines[w] = Imaging.findBaseline(li, nwrs);
				}
				else wordBaselines[w] -= lineTop;
			}
			int descCount = 0;
			int evenCount = 0;
			int ascCount = 0;
			for (int w = 1; w < words.length; w++) {
				if ((wordBaselines[w-1] < 1) || (wordBaselines[w] < 1))
					continue;
				if (wordBaselines[w-1] < wordBaselines[w])
					descCount++;
				else if (wordBaselines[w-1] == wordBaselines[w])
					evenCount++;
				else ascCount++;
			}
			int[] wordBaselineShifts = new int[words.length];
			boolean recomputeLineMeasures = false;
			if (((evenCount / 2) < ascCount) && ((words.length * 8) <= ((ascCount + evenCount) * 10))) {
				psm.setInfo(" - got leftward slope");
				recomputeLineMeasures = true;
			}
			else if (((evenCount / 2) < descCount) && ((words.length * 8) <= ((descCount + evenCount) * 10))) {
				psm.setInfo(" - got rightward slope");
				recomputeLineMeasures = true;
			}
			else {
				psm.setInfo(" - got even line");
				Arrays.fill(wordBaselineShifts, 0);
			}
			
			//	TODO compute overall baseline tendency, maybe comparing average of left and right half of line
			
			//	TODO consider linetop-overshooting characters in font size computation
			
			//	compute baseline
//			int baseline = Imaging.findBaseline(li, wrs);
			int baseline = Integer.parseInt((String) lines[l].getAttribute(BASELINE_ATTRIBUTE, "-1"));
			if (baseline == -1)
				baseline = Imaging.findBaseline(li, wrs);
			else baseline -= lineTop;
			
			//	adjust height
			if (recomputeLineMeasures) {
				int lineBaselineSum = 0;
				int lineBaselineWordCount = 0;
				for (int w = 0; w < words.length; w++) {
					if (wordBaselines[w] < 1)
						continue;
					lineBaselineWordCount++;
					lineBaselineSum += wordBaselines[w];
				}
				int lineBaseline = ((lineBaselineWordCount == 0) ? baseline : (lineBaselineSum / lineBaselineWordCount));
				psm.setInfo(" - mean line baseline is " + lineBaseline);
//				for (int w = 0; w < words.length; w++) {
//					if ((w == 0) || ((w+1) == words.length))
//						continue;
//					int lDist = Math.abs(lineBaseline - wordBaselines[w-1]);
//					int oDist = Math.abs(lineBaseline - wordBaselines[w]);
//					int rDist = Math.abs(lineBaseline - wordBaselines[w+1]);
//					if ((oDist > lDist) && (oDist > rDist))
//						wordBaselines[w] = ((wordBaselines[w-1] + wordBaselines[w+1]) / 2);
//				}
				for (int w = 0; w < words.length; w++) {
					if (wordBaselines[w] < 1)
						wordBaselineShifts[w] = 0;
					else wordBaselineShifts[w] = wordBaselines[w] - lineBaseline;
				}
				int minWordBaselineShift = Integer.MAX_VALUE;
				for (int w = 0; w < words.length; w++)
					minWordBaselineShift = Math.min(wordBaselineShifts[w], minWordBaselineShift);
				psm.setInfo(" - min word baseline shift is " + minWordBaselineShift);
				lineBaseline += minWordBaselineShift;
				for (int w = 0; w < words.length; w++)
					wordBaselineShifts[w] -= minWordBaselineShift;
				psm.setInfo(" - line baseline is " + lineBaseline);
				
				int overLineTop = 0;
				for (int w = 0; w < words.length; w++)
					overLineTop = Math.max(overLineTop, (lineTop - (wbbs[w].top - wordBaselineShifts[w])));
				if (overLineTop != 0) {
					psm.setInfo(" - got word over line top by " + overLineTop);
					lineBaseline += overLineTop;
					for (int w = 0; w < words.length; w++)
						wordBaselineShifts[w] -= overLineTop;
					psm.setInfo(" - corrected line baseline is " + lineBaseline);
				}
				
				lineTop = lineBottom;
				lineBottom = 0;
				for (int w = 0; w < words.length; w++) {
					lineTop = Math.min(lineTop, (wbbs[w].top - wordBaselineShifts[w]));
					lineBottom = Math.max(lineBottom, (wbbs[w].bottom - wordBaselineShifts[w]));
				}
				psm.setInfo(" - line top recomputed as " + lineTop + ", adjusted line height is " + (lineBottom - lineTop));
				wordHeight = (lineBottom - lineTop + (2 * lineTextImageMargin));
				maxShear = ((int) Math.round(((double) (lineBottom - lineTop)) * Math.tan((Math.PI / 180) * italicShearDeg)));
				baseline = lineBaseline;
			}
			
			//	build separator image
			BufferedImage lineSeparatorImage = this.getSeparatorImage(baseline + separatorDownPush + Math.min(lineTextImageMargin, separatorUpShot));
			psm.setInfo(" - separator image is " + lineSeparatorImage.getWidth() + " x " + lineSeparatorImage.getHeight());
			wordGap = Math.min(wordGap, lineSeparatorImage.getWidth());
			int lineSeparatorWidth = (wordGap + lineSeparatorImage.getWidth() + wordGap);
			int lineWidth = ((wbbs[wbbs.length-1].right - lineIndent) + ((words.length + 1) * lineSeparatorWidth));
			
			//	build text image
			BufferedImage lineTextImage = new BufferedImage((lineWidth + (2 * lineTextImageMargin) + maxShear), wordHeight, BufferedImage.TYPE_BYTE_GRAY);
			lineTextImage.getGraphics().setColor(Color.WHITE);
			lineTextImage.getGraphics().fillRect(0, 0, lineTextImage.getWidth(), lineTextImage.getHeight());
			for (int w = 0; w < words.length; w++) {
				int wordLeft = ((wbbs[w].left - lineIndent) + ((w + 1) * lineSeparatorWidth) + lineTextImageMargin);
				
				if ((baseline + lineTextImageMargin) < lineSeparatorImage.getHeight())
					baseline = (lineSeparatorImage.getHeight() - lineTextImageMargin);
				
				for (int c = 0; c < lineSeparatorImage.getWidth(); c++) {
					for (int r = 0; r < lineSeparatorImage.getHeight(); r++) try {
						rgb = lineSeparatorImage.getRGB(c, r);
						lineTextImage.setRGB((wordLeft - wordGap - lineSeparatorImage.getWidth() + c), (lineTextImageMargin + baseline + separatorDownPush - lineSeparatorImage.getHeight() + r), rgb);
					}
					catch (Exception e) {
						System.out.println("   - line text image is " + lineTextImage.getWidth() + " x " + lineTextImage.getHeight());
						System.out.println("   - line separator image is " + lineSeparatorImage.getWidth() + " x " + lineSeparatorImage.getHeight());
						System.out.println("   - lsi-x is " + (wordLeft - wordGap - lineSeparatorImage.getWidth() + c));
						System.out.println("   - lsi-y is " + (lineTextImageMargin + baseline + separatorDownPush - lineSeparatorImage.getHeight() + r));
						System.out.println("   --> ERROR");
//						throw new RuntimeException(e);
						e.printStackTrace(System.out);
						c = lineSeparatorImage.getWidth();
					}
				}
				psm.setInfo("Doing word " + w);
//				BufferedImage wordImage = pageImage.image.getSubimage(wbbs[w].left, wbbs[w].top, (wbbs[w].right - wbbs[w].left), (wbbs[w].bottom - wbbs[w].top));
				BufferedImage wordImage = this.getBoxImage(wbbs[w], pageImage);
				if (!"".equals(words[w].getAttribute(ITALICS_ATTRIBUTE, ""))) {
					int wordShear = ((int) Math.round(((double) wordImage.getHeight()) * Math.tan((Math.PI / 180) * italicShearDeg)));
//					BufferedImage sWordImage = new BufferedImage((wordImage.getWidth() + wordShear), wordImage.getHeight(), pageImage.image.getType());
					BufferedImage sWordImage = new BufferedImage((wordImage.getWidth() + wordShear), wordImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
					sWordImage.getGraphics().setColor(Color.WHITE);
					sWordImage.getGraphics().fillRect(0, 0, sWordImage.getWidth(), sWordImage.getHeight());
					for (int r = 0; r < wordImage.getHeight(); r++) {
						int rShear = ((wordShear * r) / wordImage.getHeight());
						for (int c = 0; c < wordImage.getWidth(); c++) {
							rgb = wordImage.getRGB(c, r);
							sWordImage.setRGB((c + rShear), r, rgb);
						}
					}
					wordImage = sWordImage;
				}
				
				for (int c = 0; c < wordImage.getWidth(); c++)
					for (int r = 0; r < wordImage.getHeight(); r++) {
						rgb = wordImage.getRGB(c, r);
						lineTextImage.setRGB((c + wordLeft), (r + lineTextImageMargin + (wbbs[w].top - wordBaselineShifts[w]) - lineTop), rgb);
					}
			}
			
			//	terminate line
			if ((baseline + lineTextImageMargin) < lineSeparatorImage.getHeight())
				baseline = (lineSeparatorImage.getHeight() - lineTextImageMargin);
			for (int c = 0; c < lineSeparatorImage.getWidth(); c++) {
				for (int r = 0; r < lineSeparatorImage.getHeight(); r++) try {
					rgb = lineSeparatorImage.getRGB(c, r);
					lineTextImage.setRGB((lineTextImage.getWidth() - wordGap - lineSeparatorImage.getWidth() + c), (lineTextImageMargin + baseline + separatorDownPush - lineSeparatorImage.getHeight() + r), rgb);
				}
				catch (Exception e) {
					System.out.println("   - line text image is " + lineTextImage.getWidth() + " x " + lineTextImage.getHeight());
					System.out.println("   - line separator image is " + lineSeparatorImage.getWidth() + " x " + lineSeparatorImage.getHeight());
					System.out.println("   --> ERROR");
//					throw new RuntimeException(e);
					e.printStackTrace(System.out);
					c = lineSeparatorImage.getWidth();
				}
			}
			
			lines[l].setAttribute("lineNr", ("" + l));
			this.doWordOcr(l, lineTextImage, words, psm);
			
			for (int w = 1; w < words.length; w++) {
				
				//	words too far apart
				if ((((minWordMargin * 5) + 2) / 4) <= (wbbs[w].left - wbbs[w-1].right))
					continue;
				
				//	some string missing
				if (!words[w-1].hasAttribute(STRING_ATTRIBUTE) || !words[w].hasAttribute(STRING_ATTRIBUTE))
					continue;
				String lWord = ((String) words[w-1].getAttribute(STRING_ATTRIBUTE));
				String rWord = ((String) words[w].getAttribute(STRING_ATTRIBUTE));
				
				//	some string attribute missing
				if ((lWord.length() == 0) || (rWord.length() == 0))
					continue;
				
				//	right word starts with a dash, we might have cut apart a dashed compound word
				if (rWord.startsWith("-")) {
					
					//	words too far apart
					if ((pageImage.originalDpi / 100) <= (wbbs[w].left - wbbs[w-1].right))
						continue;
					
					//	this looks more like an enumeration TODO add enumeration stop words in other languages
					if ("and".equalsIgnoreCase(lWord) || "or".equalsIgnoreCase(lWord))
						continue;
				}
				
				//	non-dash boundary
				else {
					
					//	last and first character do not match
					if (lWord.charAt(lWord.length() - 1) != rWord.charAt(0))
						continue;
					
					//	no to-join character
					if (!this.joinPairCharacters.contains(rWord.substring(0, 1)))
						continue;
				}
				
				words[w].setAttribute(STRING_ATTRIBUTE, (lWord + rWord));
				String lOcr = ((String) words[w-1].getAttribute(OCR_ATTRIBUTE));
				String rOcr = ((String) words[w].getAttribute(OCR_ATTRIBUTE));
				if ((lOcr != null) || (rOcr != null))
					words[w].setAttribute(OCR_ATTRIBUTE, (((lOcr == null) ? lWord : lOcr) + " " + ((rOcr == null) ? rWord : rOcr)));
				
				BoundingBox[] jwbbs = {wbbs[w-1], wbbs[w]};
				BoundingBox jwbb = BoundingBox.aggregate(jwbbs);
				wbbs[w] = jwbb;
				words[w].setAttribute(BOUNDING_BOX_ATTRIBUTE, jwbb.toString());
				
				/*
				 * need to do it this way because word tokens are all a simple
				 * 'W' at this stage, which makes orientaion especially hard
				 */
				lines[l].removeChars(words[w-1].getStartOffset(), words[w-1].length());
				words[w-1] = null;
				wbbs[w-1] = null;
				psm.setInfo(" - joined '" + lWord + "' with '" + rWord + "'");
			}
		}
		psm.setProgress(100);
	}
	
	private void doWordOcr(int lineNr, BufferedImage lineTextImage, Annotation[] words, ProgressMonitor psm) throws IOException {
		
		//	store text image
		String lineName = (((lineNr < 100) ? "0" : "") + ((lineNr < 10) ? "0" : "") + lineNr);
		String lineTextImageName = ("cache/line." + lineName + ".l." + IMAGE_FORMAT);
		ImageIO.write(lineTextImage, IMAGE_FORMAT, new File(this.basePath, lineTextImageName));
		
		//	run Tesseract process
		String lineTextName = ("cache/line." + lineName + ".l");
		String[] lineTextCommand = {
				(new File(this.basePath, "tesseract.exe").getAbsolutePath()),
				(lineTextImageName),
				(lineTextName),
		};
		Process lineTesseract = Runtime.getRuntime().exec(lineTextCommand, null, this.basePath);
		if (false)
			relayOutput(lineTesseract, psm);
		try {
			lineTesseract.waitFor();
		}
		catch (InterruptedException ie) {
			ie.printStackTrace(System.out);
		}
		
		//	load text (Tesseract outputs UTF-8 !!!)
		File wordTextsFile = new File(this.basePath, (lineTextName + ".txt"));
		BufferedReader wordTextsReader = new BufferedReader(new InputStreamReader(new FileInputStream(wordTextsFile), "UTF-8"));
		StringVector wordTexts = StringVector.loadList(wordTextsReader);
		wordTextsReader.close();
		if (wordTexts.isEmpty()) {
			psm.setInfo(" --> no text at all in line " + lineNr);
			return;
		}
		
		String wordTextsString = wordTexts.get(0);
		wordTexts.clear();
		
//		//	split line
//		String[] wordTextStrings = wordTextsString.split("\\s*" + RegExUtils.escapeForRegEx(wordSeparatorString) + "\\s*");
//		
//		//	normal separator did not work, try lower case
//		if ((wordTextStrings.length < 2) && (wordTextsString.indexOf(wordSeparatorString.toLowerCase()) != -1))
//			wordTextStrings = wordTextsString.split("\\s*" + RegExUtils.escapeForRegEx(wordSeparatorString.toLowerCase()) + "\\s*");
//		
//		//	did not work either, try spaced-out version
//		if ((wordTextStrings.length < 2) && (wordTextsString.indexOf(spacedWordSeparatorString) != -1))
//			wordTextStrings = wordTextsString.split("\\s*" + RegExUtils.escapeForRegEx(spacedWordSeparatorString) + "\\s*");
//		
//		//	no separator at all, can happen if font size estimation goes wrong, try whitespace split
//		if ((wordTextStrings.length < 2) && (wordTextsString.indexOf(spaceSeparatorString) != -1))
//			wordTextStrings = wordTextsString.split("\\s*" + RegExUtils.escapeForRegEx(spaceSeparatorString) + "\\s*");
		
		//	split line
		String[] wordTextStrings = wordTextsString.split(capitalSeparatorSplitPattern);
		
		//	normal separator did not work, try lower case
		if (wordTextStrings.length < 2)
			wordTextStrings = wordTextsString.split(mixedSeparatorSplitPattern);
		
		//	store word strings for processing
		wordTexts.addContent(wordTextStrings);
		if (wordTexts.isEmpty()) {
			psm.setInfo(" --> no text at all in line " + lineNr);
			return;
		}
		
		//	take care of standalone text 'X' attached to separator
		for (int w = 0; w < (wordTexts.size() - 1); w++) {
			if (!"".equals(wordTexts.get(w)))
				continue;
			String wordAfterSpace = wordTexts.get(w+1);
			if ("X".equalsIgnoreCase(wordAfterSpace))
				wordTexts.remove(w--);
			else if (wordAfterSpace.toLowerCase().startsWith("x ")) {
				wordTexts.setElementAt(wordAfterSpace.substring(0, 1), w);
				wordTexts.setElementAt(wordAfterSpace.substring(2).trim(), (w+1));
			}
		}
		
		//	check what we got
		if ("".equals(wordTexts.get(0)))
			wordTexts.remove(0);
//		wordTexts.removeAll(wordSeparatorString);
//		wordTexts.removeAll(spacedWordSeparatorString);
		if (wordTexts.size() > words.length)
			throw new IOException("Too many words in line " + lineNr + ": " + wordTexts.concatStrings(" | "));
		
		if (wordTexts.size() != 0) {
			String firstWordText = wordTexts.get(0);
			if (firstWordText.toLowerCase().startsWith(wordSeparatorString.toLowerCase()))
				wordTexts.set(0, firstWordText.substring(wordSeparatorString.length()));
			String lastWordText = wordTexts.get(wordTexts.size() - 1);
			if (lastWordText.toLowerCase().endsWith(wordSeparatorString.toLowerCase()))
				wordTexts.set((wordTexts.size() - 1), lastWordText.substring(0, (lastWordText.length() - wordSeparatorString.length())));
		}
		
		int missing = 0;
		for (int w = 0; w < words.length; w++) {
			if (w < wordTexts.size()) {
				String wordText = wordTexts.get(w);
				String cWordText = this.wholeCorrections.getProperty(wordText);
				for (int c = 0; c < this.patternCorrections.size(); c++) {
					PatternCorrection pc = ((PatternCorrection) this.patternCorrections.get(c));
					if (cWordText == null)
						 cWordText = pc.correct(wordText);
					else cWordText = pc.correct(cWordText);
				}
				if ((cWordText == null) || cWordText.equals(wordText))
					words[w].setAttribute(STRING_ATTRIBUTE, wordText);
				else {
					psm.setInfo(" - corrected '" + wordText + "' to '" + cWordText + "'");
					words[w].setAttribute(STRING_ATTRIBUTE, cWordText);
					words[w].setAttribute(OCR_ATTRIBUTE, wordText);
				}
			}
			else {
				missing++;
				words[w].setAttribute(("_strMissing"), "true");
			}
		}
		if (missing != 0)
			psm.setInfo(" --> missing " + missing + " words in line " + lineNr);
	}
	
	private BufferedImage getOcrImage(BoundingBox box, PageImage pageImage) {
		BufferedImage boxImage = this.getBoxImage(box, pageImage);
		return ((boxImage == null) ? null : this.getOcrImage(boxImage));
	}
	
	private BufferedImage getBoxImage(BoundingBox box, PageImage pageImage) {
		try {
			return pageImage.image.getSubimage((box.left - pageImage.leftEdge), (box.top - pageImage.topEdge), (box.right - box.left), (box.bottom - box.top));
		}
		catch (Exception e) {
			System.out.println("Exception cutting [" + (box.left - pageImage.leftEdge) + "," + (box.right - pageImage.leftEdge) + "] x [" + (box.top - pageImage.topEdge) + "," + (box.bottom - pageImage.topEdge) + "] from " + pageImage.image.getWidth() + " x " + pageImage.image.getHeight() + " image: " + e.getMessage());
			e.printStackTrace(System.out);
		}
		try {
			return pageImage.image.getSubimage((box.left - pageImage.leftEdge), (box.top - pageImage.topEdge), Math.min((box.right - box.left), (pageImage.image.getWidth() - (box.left - pageImage.leftEdge))), Math.min((box.bottom - box.top), (pageImage.image.getHeight() - (box.top - pageImage.topEdge))));
		}
		catch (Exception e) {
			System.out.println("Exception cutting [" + (box.left - pageImage.leftEdge) + "," + (box.right - pageImage.leftEdge) + "] x [" + (box.top - pageImage.topEdge) + "," + (box.bottom - pageImage.topEdge) + "] from " + pageImage.image.getWidth() + " x " + pageImage.image.getHeight() + " image: " + e.getMessage());
			e.printStackTrace(System.out);
		}
		return null;
	}
	
	private BufferedImage getOcrImage(BufferedImage boxImage) {
		BufferedImage ocrImage = new BufferedImage((boxImage.getWidth() + (2 * lineTextImageMargin)), (boxImage.getHeight() + (2 * lineTextImageMargin)), BufferedImage.TYPE_BYTE_GRAY);
		ocrImage.getGraphics().setColor(Color.WHITE);
		ocrImage.getGraphics().fillRect(0, 0, ocrImage.getWidth(), ocrImage.getHeight());
		for (int c = 0; c < boxImage.getWidth(); c++) {
			for (int r = 0; r < boxImage.getHeight(); r++)
				ocrImage.setRGB((c + lineTextImageMargin), (r + lineTextImageMargin), boxImage.getRGB(c, r));
		}
		return ocrImage;
	}
	
	private Set joinPairCharacters = new HashSet();
	private Properties wholeCorrections = new Properties();
	private ArrayList patternCorrections = new ArrayList();
	private static class PatternCorrection {
		private Pattern pattern;
		Properties replacements = new Properties();
		PatternCorrection(String pattern) {
			this.pattern = Pattern.compile(pattern);
		}
		String correct(String str) {
			if (str.length() < 2) // we always need some context
				return str;
//			System.out.println("Matching " + str);
			Matcher m = this.pattern.matcher(str);
			StringBuffer cStr = new StringBuffer();
			int le = 0;
			int lr = 0;
			while (m.find(lr)) {
				int s = m.start();
				int e = m.end();
//				System.out.println(" - found " + s + " - " + e);
				if (le < s)
					cStr.append(str.substring(le, s));
				for (int c = s; c < e; c++) {
					String ch = str.substring(c, (c+1));
					//	TODO enable multi-character replacements
//					cStr.append(this.replacements.getProperty(ch, ch));
					String cs = this.replacements.getProperty(ch, ch);
					cStr.append(cs);
					if (!cs.equals(ch))
						lr = c;
				}
				le = e;
			}
			cStr.append(str.substring(le));
			return cStr.toString();
		}
	}
	
	/* TODO
==> use whitespace-free leading and tailing wildcards for testing in panels ...
==> ... and highlight actually matching substring in bold
  ==> will require custom match result display ...
  ==> ... but then, what the hell, done worse Swing stuff before ...
	 */
	private static Pattern prefixCatcher = Pattern.compile("\\A" +
			"(Mac|Mc|Della|Delle)" + // TODO extend this list
			"[A-Z]");
	private class PrefixCatchingPatternCorrection extends PatternCorrection {
		PrefixCatchingPatternCorrection(String pattern) {
			super(pattern);
		}
		String correct(String str) {
			Matcher pm = prefixCatcher.matcher(str);
			if (pm.find()) {
				int e = pm.end(1);
				return (str.substring(0, e) + super.correct(str.substring(e)));
			}
			else return super.correct(str);
		}
	}

	private void loadCorrections() {
		PatternCorrection pc;
		
		//	TODO correct ripped-apart 'k' and 'K' characters, e.g. from 'I<' and 'l<'
		
		//	TODO correct ',,' into lower double quotes
		
		//	corrects mis-OCRed '0' and '1' in number blocks (may include degree signs and the like) ==> helps with numbers, coordinates, etc.
		pc = new PatternCorrection("" +
				"(" +
					"[0-9]" +
					"[^a-zA-Z\\s\\[]?" +
				")" +
				"[\\]IlOo]" + // extend options for 1
				"(" +
					"[^a-zA-Z\\s\\[]?" +
					"[0-9]" +
				")" +
				"");
		pc.replacements.setProperty("I", "1");
		pc.replacements.setProperty("l", "1");
		pc.replacements.setProperty("]", "1");
		pc.replacements.setProperty("O", "0");
		pc.replacements.setProperty("o", "0");
		this.patternCorrections.add(pc);
		
		//	corrects mis-OCRed 'o' and 'l' in words
		pc = new PrefixCatchingPatternCorrection("" +
				"[a-z]+" +
				"[0I1]+" +
//				"[a-z]+" +
				"(" +
					"[^A-Z]+" +
					"|" +
					"\\Z" +
				")");
		pc.replacements.setProperty("0", "o");
		pc.replacements.setProperty("I", "l");
		pc.replacements.setProperty("1", "l");
		this.patternCorrections.add(pc);
		
		//	corrects mis-OCRed 'O', 'B', and 'I' in words
		pc = new PatternCorrection("" +
				"[A-Z]+" +
				"[081l]+" +
				"[A-Z]+" +
				"");
		pc.replacements.setProperty("0", "O");
		pc.replacements.setProperty("8", "B");
		pc.replacements.setProperty("1", "I");
		pc.replacements.setProperty("l", "I");
		this.patternCorrections.add(pc);
		
		//	corrects lower case letters mistaken for their capital counterparts in lower case blocks
		pc = new PrefixCatchingPatternCorrection("" +
				"[a-z]+" +
				"[CKOSVWXYZ]" +
				"(" +
//					"[a-z]" +
					"[^A-Z]" +
					"|" +
					"\\Z" +
				")");
		pc.replacements.setProperty("C", "c");
		pc.replacements.setProperty("K", "k");
		pc.replacements.setProperty("O", "o");
		pc.replacements.setProperty("S", "s");
		pc.replacements.setProperty("V", "v");
		pc.replacements.setProperty("W", "w");
		pc.replacements.setProperty("X", "x");
		pc.replacements.setProperty("Y", "y");
		pc.replacements.setProperty("Z", "z");
		this.patternCorrections.add(pc);
		
		//	corrects capital case letters mistaken for their lower case counterparts in capital letter blocks
		pc = new PatternCorrection("" +
				"[A-Z]" +
				"[ckosvwxyz]" +
				"[A-Z]" +
				"");
		pc.replacements.setProperty("c", "C");
		pc.replacements.setProperty("k", "K");
		pc.replacements.setProperty("o", "O");
		pc.replacements.setProperty("s", "S");
		pc.replacements.setProperty("v", "V");
		pc.replacements.setProperty("w", "W");
		pc.replacements.setProperty("x", "X");
		pc.replacements.setProperty("y", "Y");
		pc.replacements.setProperty("z", "Z");
		this.patternCorrections.add(pc);
		
		//	corrects mis-OCRed dots in un-spaced dates
		pc = new PatternCorrection("[0-9]" +
				"\\," +
				"[vViIxX]" +
				"");
		pc.replacements.setProperty(",", ".");
		this.patternCorrections.add(pc);
		
		//	corrects mis-OCRed dots of abbreviations
		pc = new PatternCorrection("\\A[B-HJ-Z][a-z]?\\,\\Z");
		pc.replacements.setProperty(",", ".");
		this.patternCorrections.add(pc);
		
		//	corrects mis-OCRed 0's
		pc = new PatternCorrection("" +
				"[0-9]" +
				"\\(\\)" +
				"[0-9]" +
				"");
		pc.replacements.setProperty("(", "0");
		pc.replacements.setProperty(")", "");
		this.patternCorrections.add(pc);
		
		//	corrects mis-OCRed dashes in number blocks
		pc = new PatternCorrection("" +
				"[0-9]+" +
				"\\~" +
				"[0-9]+" +
				"");
		pc.replacements.setProperty("~", "-");
		this.patternCorrections.add(pc);
		
		//	corrects mis-OCRed dashes in words
		pc = new PatternCorrection("" +
				"[A-Za-z]" +
				"\\~" +
				"[A-Za-z]" +
				"");
		pc.replacements.setProperty("~", "-");
		this.patternCorrections.add(pc);
		
		//	corrects mis-OCRed capital I's
		pc = new PatternCorrection("" +
				"(" +
					"[A-Z]{2,}" +
					"[1l]" +
					"(\\Z|[^a-z])" +
				")" +
				"|" +
				"(" +
					"[A-Z]+" +
					"[1l]" +
					"[A-Z]+" +
				")");
		pc.replacements.setProperty("1", "I");
		pc.replacements.setProperty("l", "I");
		this.patternCorrections.add(pc);
		
		//	TODO load pattern corrections from config file - got to find nice file representation first, though
		
		this.wholeCorrections.setProperty("ofthe", "of the");
		this.wholeCorrections.setProperty("ofa", "of a");
		this.wholeCorrections.setProperty("onthe", "on the");
		this.wholeCorrections.setProperty("tothe", "to the");
		
		//	TODO load whole string corrections from config file
		
		this.joinPairCharacters.add("1");
		
		//	TODO load join pair characters from config file
	}
	
	//	TODO make these things configurable
	//	TODO figure out which values make sense
	private static final int lineTextImageMargin = 3;
	private static final int separatorUpShot = 1;
	private static final int separatorDownPush = 1;
	private static final int italicShearDeg = 15;
	
	private static final String capitalSeparatorSplitPattern = "(\\s*([X]\\s*){3})";
	private static final String mixedSeparatorSplitPattern = "(\\s*([Xx]\\s*){3})";
	
	private static final String wordSeparatorString = "XXX";
//	private static final String spacedWordSeparatorString = "X X X";
//	private static final String spaceSeparatorString = "   ";
	
	private BufferedImage getSeparatorImage(int sepHeight) {
		
		//	TODO introduce maximum height for separator image depending on DPI
		
		sepHeight = Math.max(1, sepHeight);
		int sepFontSize = 15;
		
		BufferedImage lineSepImage = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
		lineSepImage.getGraphics().fillRect(0, 0, lineSepImage.getWidth(), lineSepImage.getHeight());
		Graphics2D graphics = lineSepImage.createGraphics();
		graphics.setFont(new Font("Times New Roman", Font.BOLD, sepFontSize));
		TextLayout tl = new TextLayout(wordSeparatorString, graphics.getFont(), graphics.getFontRenderContext());
		Rectangle2D wordBounds = tl.getBounds();
		if (wordBounds.getHeight() > sepHeight) {
			while (wordBounds.getHeight() > sepHeight) {
				sepFontSize--;
				graphics.setFont(new Font("Times New Roman", Font.BOLD, sepFontSize));
				tl = new TextLayout(wordSeparatorString, graphics.getFont(), graphics.getFontRenderContext());
				wordBounds = tl.getBounds();
			}
		}
		else while (wordBounds.getHeight() <= sepHeight) {
			graphics.setFont(new Font("Times New Roman", Font.BOLD, (sepFontSize+1)));
			tl = new TextLayout(wordSeparatorString, graphics.getFont(), graphics.getFontRenderContext());
			wordBounds = tl.getBounds();
			if (wordBounds.getHeight() <= sepHeight)
				sepFontSize++;
		}
		graphics.setFont(new Font("Times New Roman", Font.BOLD, sepFontSize));
		tl = new TextLayout(wordSeparatorString, graphics.getFont(), graphics.getFontRenderContext());
		wordBounds = tl.getBounds();
		
		lineSepImage = new BufferedImage(((int) Math.round(wordBounds.getWidth() + 1)), sepHeight, BufferedImage.TYPE_BYTE_GRAY);
		graphics = lineSepImage.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, lineSepImage.getWidth(), lineSepImage.getHeight());
		
		graphics.setFont(new Font("Times New Roman", Font.BOLD, sepFontSize));
		graphics.setColor(Color.BLACK);
		graphics.drawString(wordSeparatorString, 0, (sepHeight));
		
		return lineSepImage;
	}
	
	private static void relayOutput(final Process process, final ProgressMonitor psm) {
		Thread outRelay = new Thread() {
			public void run() {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
					String s;
					while((s = br.readLine()) != null)
						psm.setInfo(s);
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.out);
				}
			}
		};
		outRelay.start();
		Thread errRelay = new Thread() {
			public void run() {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					String s;
					while((s = br.readLine()) != null)
						psm.setInfo(s);
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.out);
				}
			}
		};
		errRelay.start();
	}
	
	public static void main(String[] args) throws Exception {
		
		OcrEngine oe = new OcrEngine(new File("E:/Testdaten/PdfExtract/"));
		String str;
		str = "20,viii.20()3";
		for (int p = 0; p < oe.patternCorrections.size(); p++) {
			PatternCorrection pc = ((PatternCorrection) oe.patternCorrections.get(p));
			String cStr = pc.correct(str);
			if (!str.equals(cStr)) {
				System.out.println(" ==> " + str + " corrected to " + cStr);
				str = cStr;
			}
		}
		
		str = "AGOST1";
		for (int p = 0; p < oe.patternCorrections.size(); p++) {
			PatternCorrection pc = ((PatternCorrection) oe.patternCorrections.get(p));
			String cStr = pc.correct(str);
			if (!str.equals(cStr)) {
				System.out.println(" ==> " + str + " corrected to " + cStr);
				str = cStr;
			}
		}
		
		str = "Entomol0gY,";
		for (int p = 0; p < oe.patternCorrections.size(); p++) {
			PatternCorrection pc = ((PatternCorrection) oe.patternCorrections.get(p));
			String cStr = pc.correct(str);
			if (!str.equals(cStr)) {
				System.out.println(" ==> " + str + " corrected to " + cStr);
				str = cStr;
			}
		}
		
		str = "M,";
		for (int p = 0; p < oe.patternCorrections.size(); p++) {
			PatternCorrection pc = ((PatternCorrection) oe.patternCorrections.get(p));
			String cStr = pc.correct(str);
			if (!str.equals(cStr)) {
				System.out.println(" ==> " + str + " corrected to " + cStr);
				str = cStr;
			}
		}
		
		str = "3I°48’E).";
		for (int p = 0; p < oe.patternCorrections.size(); p++) {
			PatternCorrection pc = ((PatternCorrection) oe.patternCorrections.get(p));
			String cStr = pc.correct(str);
			if (!str.equals(cStr)) {
				System.out.println(" ==> " + str + " corrected to " + cStr);
				str = cStr;
			}
		}
		
		str= "31°l5’E,";
		for (int p = 0; p < oe.patternCorrections.size(); p++) {
			PatternCorrection pc = ((PatternCorrection) oe.patternCorrections.get(p));
			String cStr = pc.correct(str);
			if (!str.equals(cStr)) {
				System.out.println(" ==> " + str + " corrected to " + cStr);
				str = cStr;
			}
		}
		
		str = "[0.l84];";
		for (int p = 0; p < oe.patternCorrections.size(); p++) {
			PatternCorrection pc = ((PatternCorrection) oe.patternCorrections.get(p));
			String cStr = pc.correct(str);
			if (!str.equals(cStr)) {
				System.out.println(" ==> " + str + " corrected to " + cStr);
				str = cStr;
			}
		}
		
		str = "MacCallum";
		for (int p = 0; p < oe.patternCorrections.size(); p++) {
			PatternCorrection pc = ((PatternCorrection) oe.patternCorrections.get(p));
			String cStr = pc.correct(str);
			if (!str.equals(cStr)) {
				System.out.println(" ==> " + str + " corrected to " + cStr);
				str = cStr;
			}
		}
		
		str = "MocCa";
		for (int p = 0; p < oe.patternCorrections.size(); p++) {
			PatternCorrection pc = ((PatternCorrection) oe.patternCorrections.get(p));
			String cStr = pc.correct(str);
			if (!str.equals(cStr)) {
				System.out.println(" ==> " + str + " corrected to " + cStr);
				str = cStr;
			}
		}
	}
}
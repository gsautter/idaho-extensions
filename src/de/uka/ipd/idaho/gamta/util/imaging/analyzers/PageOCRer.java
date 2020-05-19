///*
// * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *     * Redistributions of source code must retain the above copyright
// *       notice, this list of conditions and the following disclaimer.
// *     * Redistributions in binary form must reproduce the above copyright
// *       notice, this list of conditions and the following disclaimer in the
// *       documentation and/or other materials provided with the distribution.
// *     * Neither the name of the Universitaet Karlsruhe (TH) nor the
// *       names of its contributors may be used to endorse or promote products
// *       derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY UNIVERSITAET KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
// * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
// * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uka.ipd.idaho.gamta.util.imaging.analyzers;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Properties;
//
//import de.uka.ipd.idaho.gamta.MutableAnnotation;
//import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
//import de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer;
//import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
//import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
//import de.uka.ipd.idaho.gamta.util.imaging.PageAnalysis;
//import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
//import de.uka.ipd.idaho.gamta.util.imaging.PageImageConverter;
//
///**
// * @author sautter
// */
//public class PageOCRer extends AbstractConfigurableAnalyzer implements MonitorableAnalyzer, ImagingConstants {
//	
////	private OcrEngine ocrEngine;
////	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
//	 */
//	public void initAnalyzer() {
////		
////		/* this is a breach of the data provider principle, but that's
////		 * impossible to avoid if we want to use ImageMagick and Tesseract
////		 */
////		File basePath = new File(this.dataProvider.getAbsolutePath());
////		try {
////			this.ocrEngine = new OcrEngine(basePath);
////		}
////		catch (Exception e) {
////			System.out.println("PdfExtractor: could not create OCR engine - " + e.getMessage());
////			e.printStackTrace(System.out);
////		}
////		
////		//	test if we can OCR
////		if (this.ocrEngine == null)
////			throw new RuntimeException("OCR Engine unavailable, check log.");
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
//	 */
//	public void process(MutableAnnotation data, Properties parameters) {
//		this.process(data, parameters, ProgressMonitor.dummy);
//	}
//	
//	/* (non-Javadoc)
//	 * @see de.uka.ipd.idaho.gamta.util.MonitorableAnalyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties, de.uka.ipd.idaho.gamta.util.ProgressMonitor)
//	 */
//	public void process(MutableAnnotation data, Properties parameters, ProgressMonitor psm) {
//		
//		//	get document ID
//		String docId = ((String) data.getAttribute(DOCUMENT_ID_ATTRIBUTE));
//		if (docId == null)
//			docId = data.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE);
//		if (docId == null)
//			return;
//		
//		//	do structure analysis
//		psm.setStep("Analyzing page structure");
//		psm.setBaseProgress(30);
//		psm.setMaxProgress(60);
//		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
//		for (int p = 0; p < pages.length; p++) {
//			psm.setInfo("Analyzing structure of page " + (p+1) + " of " + pages.length);
//			psm.setProgress((100 * p) / pages.length);
//			
//			//	test if OCRed before
//			if (!"P".equals(pages[p].firstValue())) {
//				psm.setInfo(" - page OCRed before");
//				continue;
//			}
//			
//			//	get page ID
//			String pidString = ((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE));
//			if ((pidString == null) || !pidString.matches("[0-9]+")) {
//				psm.setInfo(" - page ID missing or invalid");
//				continue;
//			}
//			
//			//	get page image
//			PageImage pi = PageImage.getPageImage(docId, Integer.parseInt(pidString));
//			if (pi == null) {
//				psm.setInfo(" - page image not found");
//				continue;
//			}
//			
//			//	do structure analysis
//			try {
//				PageImageConverter.fillInPageRegions(pages[p], pi, psm);
//				psm.setInfo(" - page done");
//			}
//			catch (IOException ioe) {
//				psm.setInfo(" - error analyzing page structure: " + ioe.getMessage());
//				ioe.printStackTrace(System.out);
//			}
//		}
//		psm.setProgress(100);
//		
//		//	do OCR
//		psm.setStep("Doing OCR");
//		psm.setBaseProgress(60);
//		psm.setMaxProgress(100);
//		for (int p = 0; p < pages.length; p++) {
//			psm.setInfo("Doing OCR for page " + (p+1) + " of " + pages.length);
//			psm.setProgress((100 * p) / pages.length);
//			
//			//	get page ID
//			String pidString = ((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE));
//			if ((pidString == null) || !pidString.matches("[0-9]+")) {
//				psm.setInfo(" - page ID missing or invalid");
//				continue;
//			}
//			
//			//	get page image
//			PageImage pi = PageImage.getPageImage(docId, Integer.parseInt(pidString));
//			if (pi == null) {
//				psm.setInfo(" - page image not found");
//				continue;
//			}
////			
////			//	do OCR
////			try {
////				psm.setInfo(" - doing OCR");
////				this.ocrEngine.doBlockOcr(pages[p], pi, psm);
////				psm.setInfo(" - OCR done");
////			}
////			catch (IOException ioe) {
////				psm.setInfo(" - error doing OCR: " + ioe.getMessage());
////				ioe.printStackTrace(System.out);
////			}
//			
//			//	do structure analysis
//			PageAnalysis.splitIntoParagraphs(pages[p].getMutableAnnotations(BLOCK_ANNOTATION_TYPE), pi.currentDpi, null);
//			psm.setInfo(" - paragraphs done");
//			PageAnalysis.computeColumnLayout(pages[p].getMutableAnnotations(COLUMN_ANNOTATION_TYPE), pi.currentDpi);
//			psm.setInfo(" - layout analysis done");
//			
//			//	... finally
//			psm.setInfo(" - page done");
//		}
//		psm.setProgress(100);
//	}
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		//	TODO test this
//		
//	}
//}
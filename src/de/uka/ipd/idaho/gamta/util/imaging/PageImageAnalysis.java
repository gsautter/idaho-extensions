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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging.AnalysisImage;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging.ImagePartRectangle;

/**
 * Function library for page image analysis.
 * 
 * @author sautter
 */
public class PageImageAnalysis implements ImagingConstants {
	
	/**
	 * A part of a text image, namely a block, a line, or an individual word.
	 * 
	 * @author sautter
	 */
	public static abstract class PagePart {
		public final ImagePartRectangle bounds;
		PagePart(ImagePartRectangle bounds) {
			//	clone bounds, so no two parts shre the same (causes trouble with adjustments)
			this.bounds = new ImagePartRectangle(bounds.analysisImage);
			this.bounds.leftCol = bounds.leftCol;
			this.bounds.rightCol = bounds.rightCol;
			this.bounds.topRow = bounds.topRow;
			this.bounds.bottomRow = bounds.bottomRow;
			this.bounds.splitClean = bounds.splitClean;
		}
		public BoundingBox getBoundingBox() {
			return this.getBoundingBox(1);
		}
		public BoundingBox getBoundingBox(int scaleFactor) {
			int rbAdd = scaleFactor-1;
			return new BoundingBox((this.bounds.leftCol * scaleFactor), ((this.bounds.rightCol * scaleFactor) + rbAdd), (this.bounds.topRow * scaleFactor), ((this.bounds.bottomRow * scaleFactor) + rbAdd));
		}
	}
	
	/**
	 * Wrapper for an image of a generic region of a page.
	 * 
	 * @author sautter
	 */
	public static class Region extends PagePart {
		boolean isAtomic = false;
		Block block;
		final boolean isColumn;
		final Region superRegion;
		ArrayList subRegions = new ArrayList();
		Region(ImagePartRectangle bounds, boolean isColumn, Region superRegion) {
			super(bounds);
			this.isColumn = isColumn;
			this.superRegion = superRegion;
		}
		void addSubRegion(Region subRegion) {
			this.subRegions.add(subRegion);
		}
		public int getSubRegionCount() {
			return this.subRegions.size();
		}
		public Region getSubRegion(int index) {
			return ((Region) this.subRegions.get(index));
		}
		public boolean isAtomic() {
			return this.isAtomic;
		}
		void setAtomic() {
			this.subRegions.clear();
			this.isAtomic = true;
		}
		public boolean areChildrenAtomic() {
			for (int s = 0; s < this.getSubRegionCount(); s++) {
				if (!this.getSubRegion(s).isAtomic)
					return false;
			}
			return true;
		}
		public boolean isColumn() {
			return this.isColumn;
		}
		public Block getBlock() {
			return this.block;
		}
		void setBlock(Block block) {
			this.block = block;
		}
	}
	
	/**
	 * Wrapper for an image of a text block.
	 * 
	 * @author sautter
	 */
	public static class Block extends PagePart {
		ArrayList lines = new ArrayList();
		ArrayList rows = new ArrayList();
		boolean isTable = false;
		Block(ImagePartRectangle bounds) {
			super(bounds);
		}
		void addLine(Line line) {
			this.lines.add(line);
		}
		public Line[] getLines() {
			if (this.isTable) {
				ArrayList lines = new ArrayList();
				TableRow[] rows = this.getRows();
				for (int r = 0; r < rows.length; r++) {
					TableCell[] cells = rows[r].getCells();
					for (int c = 0; c < cells.length; c++)
						lines.addAll(cells[c].lines);
				}
				return ((Line[]) lines.toArray(new Line[lines.size()]));
			}
			return ((Line[]) this.lines.toArray(new Line[this.lines.size()]));
		}
		boolean lineSplitClean() {
			boolean clean = true;
			for (Iterator lit = this.lines.iterator(); clean && lit.hasNext();) {
				Line line = ((Line) lit.next());
				clean = (clean && line.bounds.splitClean);
			}
			return clean;
		}
		public boolean isTable() {
			return this.isTable;
		}
		void addRow(TableRow row) {
			this.rows.add(row);
		}
		public TableRow[] getRows() {
			return ((TableRow[]) this.rows.toArray(new TableRow[this.rows.size()]));
		}
		public boolean isEmpty() {
			return ((this.lines.size() == 0) && (this.rows.size() == 0));
		}
	}
	
	/**
	 * Wrapper for an image of a table row.
	 * 
	 * @author sautter
	 */
	public static class TableRow extends PagePart {
		ArrayList cells = new ArrayList();
		TableRow(ImagePartRectangle bounds) {
			super(bounds);
		}
		void addCell(TableCell cell) {
			this.cells.add(cell);
		}
		public TableCell[] getCells() {
			return ((TableCell[]) this.cells.toArray(new TableCell[this.cells.size()]));
		}
	}
	
	/**
	 * Wrapper for an image of a table cell.
	 * 
	 * @author sautter
	 */
	public static class TableCell extends PagePart {
		ArrayList lines = new ArrayList();
		int colSpan = 1;
		int rowSpan = 1;
		TableCell(ImagePartRectangle bounds) {
			super(bounds);
		}
		void addLine(Line line) {
			this.lines.add(line);
		}
		public Line[] getLines() {
			return ((Line[]) this.lines.toArray(new Line[this.lines.size()]));
		}
		public int getColSpan() {
			return colSpan;
		}
		public int getRowSpan() {
			return rowSpan;
		}
	}
	
	/**
	 * Wrapper for an image of a single line of text.
	 * 
	 * @author sautter
	 */
	public static class Line extends PagePart {
		ArrayList words = new ArrayList();
		int fontSize = -1;
		Line(ImagePartRectangle bounds) {
			super(bounds);
		}
		void addWord(Word word) {
			this.words.add(word);
		}
		public Word[] getWords() {
			return ((Word[]) this.words.toArray(new Word[this.words.size()]));
		}
		public int getBaseline() {
			if (this.words.size() == 0)
				return -1;
			int wordBaselineSum = 0;
			for (int w = 0; w < this.words.size(); w++) {
				Word word = ((Word) this.words.get(w));
				if (word.baseline == -1)
					return -1;
				wordBaselineSum += word.baseline;
			}
			return (wordBaselineSum / this.words.size());
		}
		public int getFontSize() {
			return this.fontSize;
		}
		public boolean isBold() {
			int wws = 0;
			int bwws = 0;
			for (int w = 0; w < this.words.size(); w++) {
				Word word = ((Word) this.words.get(w));
				wws += (word.bounds.rightCol - word.bounds.leftCol);
				if (word.isBold())
					bwws += (word.bounds.rightCol - word.bounds.leftCol);
			}
			return (wws < (2*bwws));
		}
	}
	
	/**
	 * Wrapper for an image of an individual word.
	 * 
	 * @author sautter
	 */
	public static class Word extends PagePart {
		int baseline = -1;
		boolean bold = false;
		boolean italics = false;
		Word(ImagePartRectangle bounds) {
			super(bounds);
		}
		public int getBaseline() {
			return this.baseline;
		}
		public boolean isBold() {
			return this.bold;
		}
		public boolean isItalics() {
			return this.italics;
		}
	}
	
	//	TODO add method signature with arguments for individual margins (to allow for configuring structural analysis for specific layouts)
	
	/* TODO when doing blocks:
	 * - do approximate line chopping (margin 1, but no skew)
	 *   - estimate line height if chopping fails for skew
	 * - measure margin between side-by-side equally wide blocks with many (>5?) lines
	 *   ==> actual column margin
	 *   - no such pairs of blocks exist
	 *     ==> single-column layout
	 *     ==> increase column margin threshold to prevent justified blocks with few lines (e.g. captions) from being split into multiple columns
	 * - merge side-by-side blocks with few lines whose margin is below 70% of measured column margin
	 *   ==> should repair most captions even in multi-column layouts
	 * 
	 * WATCH OUT for measurements from keys, tables, etc., though
	 * ==> test with respective documents
	 */
	
	/**
	 * Analyze the structure of a document page, i.e., chop it into sub regions
	 * and text blocks.
	 * @param ai the page image to analyze
	 * @param dpi the resolution of the underlying page image
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 * @return the root region, representing the whole page
	 * @throws IOException
	 */
	public static Region getPageRegion(AnalysisImage ai, int dpi, ProgressMonitor psm) throws IOException {
		ImagePartRectangle pageBounds = Imaging.getContentBox(ai);
//		int minHorizontalBlockMargin = (dpi / 15); // TODO find out if this makes sense (will turn out in the long haul only, though)
		int minHorizontalBlockMargin = (dpi / 10); // TODO find out if this makes sense (will turn out in the long haul only, though)
//		int minHorizontalBlockMargin = (dpi / 8); // TODO find out if this makes sense (will turn out in the long haul only, though)
//		int minVerticalBlockMargin = (dpi / 15); // TODO find out if this makes sense (will turn out in the long haul only, though)
		int minVerticalBlockMargin = (dpi / 10); // TODO find out if this makes sense (will turn out in the long haul only, though)
		return getPageRegion(pageBounds, minHorizontalBlockMargin, minVerticalBlockMargin, dpi, psm);
	}
	
	private static Region getPageRegion(ImagePartRectangle pageBounds, int minHorizontalBlockMargin, int minVerticalBlockMargin, int dpi, ProgressMonitor psm) {
		
		//	create block comprising whole page
		Region page = new Region(pageBounds, false, null);
		
		//	fill in region tree
		fillInSubRegions(page, minHorizontalBlockMargin, minVerticalBlockMargin, dpi, psm);
		
		//	finally ...
		return page;
	}
	
	private static final float capitalIHeightWidthRatio = (((float) 8) / 2); // the height/width ratio of a capital I is usually lower in serif fonts, but we want to have some safety margin
	private static void fillInSubRegions(Region region, int minHorizontalBlockMargin, int minVerticalBlockMargin, int dpi, ProgressMonitor psm) {
		if (minHorizontalBlockMargin != 1)
			System.out.println("Splitting region " + region.getBoundingBox());
		
		//	do split orthogonally to the one this region originated from
		ImagePartRectangle[] subRegions;
		if (region.isColumn) {
			if ((region.bounds.rightCol - region.bounds.leftCol) < dpi)
				subRegions = Imaging.splitIntoRows(region.bounds, minVerticalBlockMargin, 0.1);
			else subRegions = Imaging.splitIntoRows(region.bounds, minVerticalBlockMargin, 0.3);
		}
		else if ((region.bounds.bottomRow - region.bounds.topRow) < dpi)
			subRegions = Imaging.splitIntoColumns(region.bounds, minHorizontalBlockMargin, 0, true);
		else {
			ImagePartRectangle[][] subRegionsCandidates = new ImagePartRectangle[7][];
			for (int c = 0; c < subRegionsCandidates.length; c++) {
				double shearDegrees = (((double) (c - (subRegionsCandidates.length / 2))) / 10);
				subRegionsCandidates[c] = Imaging.splitIntoColumns(region.bounds, minHorizontalBlockMargin, shearDegrees, true);
				if (minHorizontalBlockMargin != 1)
					System.out.println(" - got " + subRegionsCandidates[c].length + " columns at margin " + minHorizontalBlockMargin + ", split angle " + shearDegrees);
			}
			subRegions = subRegionsCandidates[subRegionsCandidates.length / 2];
			for (int c = 0; c < subRegionsCandidates.length; c++) {
				if (subRegions.length < subRegionsCandidates[c].length)
					subRegions = subRegionsCandidates[c];
			}
		}
		if (minHorizontalBlockMargin != 1)
			System.out.println(" - got " + subRegions.length + " sub regions");
		for (int r = 0; r < subRegions.length; r++) {
			subRegions[r] = Imaging.narrowLeftAndRight(subRegions[r]);
			subRegions[r] = Imaging.narrowTopAndBottom(subRegions[r]);
			if (minHorizontalBlockMargin != 1) {
				byte avgBrightness = Imaging.computeAverageBrightness(subRegions[r]);
				System.out.println("   - " + new BoundingBox(subRegions[r].leftCol, subRegions[r].rightCol, subRegions[r].topRow, subRegions[r].bottomRow) + ", avg brightness is " + avgBrightness);
			}
		}
		
		//	we're not dealing with a whole page, and no further splits found, we're done
		if ((region.superRegion != null) && (subRegions.length == 1)) {
			Imaging.copyBounds(subRegions[0], region.bounds);
			region.setAtomic();
			return;
		}
		
		//	search for further splits
		for (int r = 0; r < subRegions.length; r++) {
			
			//	check empty regions
			if (subRegions[r].isEmpty())
				continue;
			
			//	create sub region
			Region subRegion = new Region(subRegions[r], !region.isColumn, region);
			
			//	analyze sub region recursively
			fillInSubRegions(subRegion, minHorizontalBlockMargin, minVerticalBlockMargin, dpi, psm);
			
			//	this sub region is not atomic, but has no sub regions worth retaining either, so forget about it
			if (!subRegion.isAtomic() && (subRegion.getSubRegionCount() == 0))
				continue;
			
			//	atomic and more than an inch in either direction, check brightness
			if (subRegion.isAtomic() && ((subRegion.bounds.bottomRow - subRegion.bounds.topRow) > dpi) && ((subRegion.bounds.rightCol - subRegion.bounds.leftCol) > dpi)) {
				byte avgBrightness = Imaging.computeAverageBrightness(subRegions[r]);
				if (avgBrightness <= 96)
					continue;
			}
			
			//	what remains of this sub region is less than on fifteenth of an inch (5 pt font size) high, and thus very unlikely to be text
			if ((subRegion.bounds.bottomRow - subRegion.bounds.topRow) < (dpi / 15))
				continue;
			
			//	this sub region is too tall (higher than 72pt font size) to be a single line, and narrower than one inch, and thus very unlikely to be text if no line splits exist
			if (((subRegion.bounds.bottomRow - subRegion.bounds.topRow) > dpi) && ((subRegion.bounds.rightCol - subRegion.bounds.leftCol) < dpi)) {
				ImagePartRectangle[] lines = Imaging.splitIntoRows(subRegions[r]);
				if (lines.length < 2)
					continue;
			}
			
			//	this sub region might be a single character with a large (up to 72 pt) font size, but is too narrow even for a capital I, and thus is very unlikely to be text
			float heightWidthRatio = (((float) (subRegion.bounds.bottomRow - subRegion.bounds.topRow)) / (subRegion.bounds.rightCol - subRegion.bounds.leftCol));
			if (((subRegion.bounds.bottomRow - subRegion.bounds.topRow) <= dpi) && (heightWidthRatio > capitalIHeightWidthRatio)) {
				ImagePartRectangle[] lines = Imaging.splitIntoRows(subRegions[r]);
				if (lines.length < 2)
					continue;
			}
			
			//	slice and dice atomic region with 1 pixel margin and see if anything meaningful remains
			if (subRegion.isAtomic() && (minHorizontalBlockMargin > 1) && (minVerticalBlockMargin > 1)) {
				ImagePartRectangle testRegionBounds = new ImagePartRectangle(subRegion.bounds.analysisImage);
				Imaging.copyBounds(subRegion.bounds, testRegionBounds);
				Region testRegion = new Region(testRegionBounds, true, null);
				fillInSubRegions(testRegion, 1, 1, dpi, psm);
				if (!testRegion.isAtomic() && (testRegion.getSubRegionCount() == 0))
					continue;
			}
			
			//	block with single child-column (column cannot be atomic, as otherwise block would be atomic and have no childern) (scenario can happen if some artifact column is eliminated) ==> add child-blocks of child-column instead of block itself
			if (!subRegion.isColumn && (subRegion.getSubRegionCount() == 1)) {
				Region onlyChildColumn = subRegion.getSubRegion(0);
				for (int s = 0; s < onlyChildColumn.getSubRegionCount(); s++)
					region.addSubRegion(onlyChildColumn.getSubRegion(s));
			}
			
			//	store other sub region
			else region.addSubRegion(subRegion);
		}
		
		//	only found one sub region worth retaining, so we were merely subtracting dirt, and sub region actually is atomic ==> region is atomic
		if ((region.getSubRegionCount() == 1) && region.getSubRegion(0).isAtomic()) {
			Imaging.copyBounds(region.getSubRegion(0).bounds, region.bounds);
			region.setAtomic();
		}
		
		//	multiple sub regions worth retaining, shrink bounds of region to hull of their bounds
		else if (region.getSubRegionCount() != 0) {
			subRegions = new ImagePartRectangle[region.getSubRegionCount()];
			for (int s = 0; s < region.getSubRegionCount(); s++)
				subRegions[s] = region.getSubRegion(s).bounds;
			Imaging.copyBounds(Imaging.getHull(subRegions), region.bounds);
		}
	}
	
	/**
	 * Analyze the inner structure of a single text block to individual lines,
	 * but not words, represented as an atomic region. The block object
	 * representing the inner structure of the argument region can be retrieved
	 * via the getBlock() method.
	 * @param region the text block to analyzs
	 * @param dpi the resolution of the underlying page image
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 */
	public static void getBlockLines(Region region, int dpi, ProgressMonitor psm) {
		Block block = new Block(region.bounds);
		getBlockLines(block, dpi, psm);
		region.setBlock(block);
	}
	
	/**
	 * Analyze the inner structure of a single text block to individual lines,
	 * but not words.
	 * @param block the text block to analyzs
	 * @param dpi the resolution of the underlying page image
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 */
	public static void getBlockLines(Block block, int dpi, ProgressMonitor psm) {
		
		//	compute margin thresholds
		int minVerticalBlockMargin = (dpi / 10); // TODO find out if this makes sense (will turn out in the long haul only, though)
		int minLineMargin = (dpi / 40); // TODO find out if this makes sense (will turn out in the long haul only, though)
		
		//	find text lines
		getBlockLines(block, minVerticalBlockMargin, minLineMargin, dpi, psm);
	}
	
	private static void getBlockLines(Block block, int minVerticalBlockMargin, int minLineMargin, int dpi, ProgressMonitor psm) {
		psm.setInfo("   - doing block " + block.getBoundingBox().toString());
		
		double maxSplitSlopeAngle;
		int minZigzagPartLength;
		double maxZigzagSplitSlopeAngle;
		int maxZigzagSlope;
		
		//	do conservative initial line split, so not to zigzag through page headers
		maxSplitSlopeAngle = 0.1;
		ImagePartRectangle[] lBlocks = Imaging.splitIntoRows(block.bounds, 1, maxSplitSlopeAngle, -1, 0);
		psm.setInfo("     - got " + lBlocks.length + " initial lines");
		
		//	split initial lines into columns
		ArrayList bLines = new ArrayList();
		for (int l = 0; l < lBlocks.length; l++) {
			lBlocks[l] = Imaging.narrowLeftAndRight(lBlocks[l]);
			if (lBlocks[l].isEmpty())
				continue;
			ImagePartRectangle[] cBlocks = Imaging.splitIntoColumns(lBlocks[l], (minVerticalBlockMargin * 3));
			if (cBlocks.length == 1)
				bLines.add(lBlocks[l]);
			else for (int c = 0; c < cBlocks.length; c++) {
				cBlocks[c] = Imaging.narrowTopAndBottom(cBlocks[c]);
				if (cBlocks[c].isEmpty())
					continue;
				bLines.add(cBlocks[c]);
			}
		}
		psm.setInfo("     - got " + bLines.size() + " col block lines at margin " + (minVerticalBlockMargin * 3));
		
		//	do more aggressive line splitting, but only for lines that are higher than a fifth of the dpi, as anything below that will hardly consist of two lines
		int minLineHeight = (dpi / 10); // a single line of text will rarely be lower than one thenth of an inch in total
		int minResplitHeight = (minLineHeight * 2); // the minimum height of two lines, we can securely keep anything below this threshold
		maxSplitSlopeAngle = 0.0;//0.2; zigzagging does it alone
		minZigzagPartLength = (dpi / 2);
		maxZigzagSplitSlopeAngle = 1.0;
		maxZigzagSlope = Math.abs((int) (Math.tan(maxZigzagSplitSlopeAngle * Math.PI / 180) * ((double) (block.bounds.rightCol - block.bounds.leftCol))));
		for (int l = 0; l < bLines.size(); l++) {
			ImagePartRectangle line = ((ImagePartRectangle) bLines.get(l));
			if ((line.bottomRow - line.topRow) < minResplitHeight)
				continue;
			lBlocks = Imaging.splitIntoRows(line, 1, maxSplitSlopeAngle, minZigzagPartLength, maxZigzagSlope);
			if (lBlocks.length > 1) {
				bLines.set(l, lBlocks[0]);
				for (int sl = 1; sl < lBlocks.length; sl++) {
					l++;
					bLines.add(l, lBlocks[sl]);
				}
			}
		}
		
		//	recover line blocks
		lBlocks = ((ImagePartRectangle[]) bLines.toArray(new ImagePartRectangle[bLines.size()]));
		psm.setInfo("     - got " + lBlocks.length + " block lines");
		if (lBlocks.length == 0)
			return;
		
		//	compute average line height
		int lineHeightSum = 0;
		int lineCount = 0;
		for (int l = 0; l < lBlocks.length; l++) {
			
			//	skip over lines higher than one inch
			if (dpi < (lBlocks[l].bottomRow - lBlocks[l].topRow))
				continue;
			
			//	count other lines
			lineHeightSum += (lBlocks[l].bottomRow - lBlocks[l].topRow);
			lineCount++;
		}
		int avgLineHeight = ((lineCount == 0) ? -1 : ((lineHeightSum + (lineCount / 2)) / lineCount));
		
		//	save everything higher than one tenth of an inch, no matter how big surrounding lines are
		int minWordHight = (dpi / 10);
		
		//	filter out lines narrower than one thirtieth of an inch (should still save single 1's)
		int minLineWidth = (dpi / 30);
		
		//	store lines in block
		ImagePartRectangle lostLine = null;
		for (int l = 0; l < lBlocks.length; l++) {
			lBlocks[l] = Imaging.narrowLeftAndRight(lBlocks[l]);
			int lineHeight = (lBlocks[l].bottomRow - lBlocks[l].topRow);
			
			//	keep aside very low lines, likely stains, etc. ... or dots of an i or j, accents of upper case latters, ...
			if (lineHeight < Math.min((avgLineHeight / 2), minWordHight)) {
				lostLine = lBlocks[l];
				continue;
			}
			
			//	filter out lines narrower than minimum width
			int lineWidth = (lBlocks[l].rightCol - lBlocks[l].leftCol);
			if (lineWidth < minLineWidth) {
				lostLine = null;
				continue;
			}
			
			//	join any below-minimum-height line to adjacent smaller-than-average line immediately below it (might be cut-off dot of an i or j)
			//	however, forget about accents of upper case letters, as they blow the line metrics anyways
			if ((lostLine != null)
					&& (lBlocks[l].leftCol <= lostLine.leftCol) && (lostLine.rightCol <= lBlocks[l].rightCol)
					&& (lineHeight < avgLineHeight)
				)
				lBlocks[l].topRow = Math.min(lBlocks[l].topRow, lostLine.topRow);
			block.addLine(new Line(lBlocks[l]));
			lostLine = null;
		}
	}
	
	/**
	 * Analyze the inner structure of a single text block, represented as an
	 * atomic region. The block object representing the inner structure of the
	 * argument region can be retrieved via the getBlock() method.
	 * @param region the text block to analyzs
	 * @param dpi the resolution of the underlying page image
	 * @param existingWords bounding boxes of words already known to be in the
	 *            block
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 */
	public static void getBlockStructure(Region region, int dpi, BoundingBox[] existingWords, ProgressMonitor psm) {
		Block block = new Block(region.bounds);
		getBlockStructure(block, dpi, existingWords, psm);
		region.setBlock(block);
	}
	
	/**
	 * Analyze the inner structure of a single text block.
	 * @param block the text block to analyzs
	 * @param dpi the resolution of the underlying page image
	 * @param existingWords bounding boxes of words already known to be in the
	 *            block
	 * @param psm a monitor object for reporting progress, e.g. to a UI
	 */
	public static void getBlockStructure(Block block, int dpi, BoundingBox[] existingWords, ProgressMonitor psm) {
		
		//	compute margin thresholds
		int minVerticalBlockMargin = (dpi / 10); // TODO find out if this makes sense (will turn out in the long haul only, though)
		int minLineMargin = (dpi / 40); // TODO find out if this makes sense (will turn out in the long haul only, though)
		int minWordMargin = (dpi / 40); // TODO find out if this makes sense (will turn out in the long haul only, though)
		
		//	find text lines and words
		if (existingWords == null)
			getBlockStructure(block, minVerticalBlockMargin, minLineMargin, minWordMargin, dpi, psm);
		else getBlockStructure(block, existingWords, psm);
		
		//	catch empty blocks larger than half an inch in both dimensions, might be (to-parse) table 
		if (block.isEmpty() && (dpi < ((block.bounds.rightCol - block.bounds.leftCol) * 2)) && (dpi < ((block.bounds.bottomRow - block.bounds.topRow) * 2)))
			analyzeTable(block, dpi, existingWords, psm);
	}
	
	private static void analyzeTable(Block block, int dpi, BoundingBox[] existingWords, ProgressMonitor psm) {
//		
//		//	run Sobel gradient analysis
//		float[][] xs = getSobelGrid(block.bounds, xSobel);
//		System.out.println("Sobel-X computed:");
////		for (int r = 0; r < xs[0].length; r++) {
////			System.out.print(" ");
////			for (int c = 0; c < xs.length; c++)
////				System.out.print(" " + Math.round(xs[c][r]));
////			System.out.println();
////		}
//		float[][] ys = getSobelGrid(block.bounds, ySobel);
//		System.out.println("Sobel-Y computed:");
////		for (int r = 0; r < ys[0].length; r++) {
////			System.out.print(" ");
////			for (int c = 0; c < ys.length; c++)
////				System.out.print(" " + Math.round(ys[c][r]));
////			System.out.println();
////		}
//		float[][] sobel = new float[xs.length][xs[0].length];
//		for (int c = 0; c < xs.length; c++) {
//			for (int r = 0; r < xs[c].length; r++)
//				sobel[c][r] = xs[c][r] + ys[c][r];
//		}
//		System.out.println("Sobel computed:");
		
		//	TODOne consider using pattern recognition for finding grid lines instead (might be possible to copy from zigzag splitting, exchanging white and black)
		
		//	copy block image
		BufferedImage blockImage = block.bounds.toImage().getImage();
		
		//	paint copied image white where original has black stripes
		byte[][] brightness = block.bounds.analysisImage.getBrightness();
		int white = Color.WHITE.getRGB();
		
		//	collect grid line bounds for later cell merging
		ArrayList gridLines = new ArrayList();
		
		//	do horizontal analysis
		int minHorizontalPartLength = (dpi / 2); // a table narrower than half an inch if very unlikely
		TreeSet hMatchRows = new TreeSet();
		for (int r = block.bounds.topRow; r < block.bounds.bottomRow; r++) {
			for (int c = block.bounds.leftCol; c < (block.bounds.rightCol - minHorizontalPartLength); c++) {
				if (brightness[c][r] == 127)
					continue;
				
				//	TODO allow for a few white pixels (might exist due to typeset table grid in older documents)
				int ec = c;
				while ((ec < block.bounds.rightCol) && (brightness[ec][r] < 127))
					ec++;
				
				if ((ec - c) >= minHorizontalPartLength) {
					for (int w = c; w < ec; w++)
						blockImage.setRGB((w - block.bounds.leftCol), (r - block.bounds.topRow), white);
					hMatchRows.add(new Integer(r));
					gridLines.add(block.bounds.getSubRectangle(c, ec, r, (r+1)));
				}
				
				c = ec;
			}
		}
		
		//	do vertical analysis
		int minVerticalPartLength = (dpi / 3); // a table lower than a third of an inch if very unlikely
		TreeSet vMatchCols = new TreeSet();
		for (int c = block.bounds.leftCol; c < block.bounds.rightCol; c++) {
			for (int r = block.bounds.topRow; r < (block.bounds.bottomRow - minVerticalPartLength); r++) {
				if (brightness[c][r] == 127)
					continue;
				
				//	TODO allow for a few white pixels (might exist due to typeset table grid in older documents)
				int er = r;
				while ((er < block.bounds.bottomRow) && (brightness[c][er] < 127))
					er++;
				
				if ((er - r) >= minVerticalPartLength) {
					for (int w = r; w < er; w++)
						blockImage.setRGB((c - block.bounds.leftCol), (w - block.bounds.topRow), white);
					vMatchCols.add(new Integer(c));
					gridLines.add(block.bounds.getSubRectangle(c, (c+1), r, er));
				}
				
				r = er;
			}
		}
		
		//	test criterion: replacements at least two columns and rows, one close to each block boundary, and in at most a tenth of the columns and a fifth of the rows
		if ((hMatchRows.size() < 2) || (vMatchCols.size() < 2))
			return;
		if (((hMatchRows.size() * 5) > (block.bounds.bottomRow - block.bounds.topRow)) || ((vMatchCols.size() * 10) > (block.bounds.rightCol - block.bounds.leftCol)))
			return;
		
		//	test if outmost matches are close to block bounds
		int hMatchMin = ((Integer) hMatchRows.first()).intValue();
		if ((hMatchMin - block.bounds.topRow) > (block.bounds.bottomRow - hMatchMin))
			return;
		int hMatchMax = ((Integer) hMatchRows.last()).intValue();
		if ((hMatchMax - block.bounds.topRow) < (block.bounds.bottomRow - hMatchMax))
			return;
		int vMatchMin = ((Integer) vMatchCols.first()).intValue();
		if ((vMatchMin - block.bounds.leftCol) > (block.bounds.rightCol - vMatchMin))
			return;
		int vMatchMax = ((Integer) vMatchCols.last()).intValue();
		if ((vMatchMax - block.bounds.leftCol) < (block.bounds.rightCol - vMatchMax))
			return;
		
		//	TODOne also evaluate minimum and maximum match lengths (find out if even necessary, though)
		
		//	evaluate clustering of matching rows and columns (at least table rows are likely somewhat equidistant)
		int minSignificantDistance = (dpi / 60); // somewhat less than half a millimeter
		ArrayList hMatchRowGroups = new ArrayList();
		TreeSet hMatchRowGroup = new TreeSet();
		int lastHMatchRow = -minSignificantDistance;
		for (Iterator mit = hMatchRows.iterator(); mit.hasNext();) {
			Integer hMatchRow = ((Integer) mit.next());
			
			//	group ends, store it and start new one
			if (((hMatchRow.intValue() - lastHMatchRow) >= minSignificantDistance) && (hMatchRowGroup.size() != 0)) {
				hMatchRowGroups.add(hMatchRowGroup);
				hMatchRowGroup = new TreeSet();
			}
			
			//	add 
			hMatchRowGroup.add(hMatchRow);
			lastHMatchRow = hMatchRow.intValue();
		}
		
		//	store last group
		if (hMatchRowGroup.size() != 0)
			hMatchRowGroups.add(hMatchRowGroup);
		
		//	clean up column separators that might have survived initial grid detenction due to low table row height
		for (int g = 1; g < hMatchRowGroups.size(); g++) {
			int top = ((Integer) ((TreeSet) hMatchRowGroups.get(g-1)).last()).intValue();
			int bottom = ((Integer) ((TreeSet) hMatchRowGroups.get(g)).first()).intValue();
			for (int c = block.bounds.leftCol; c < block.bounds.rightCol; c++) {
				boolean clean = true;
				for (int r = top; clean && (r < bottom); r++)
					clean = (clean && (brightness[c][r] < 127));
				if (clean) {
					for (int r = top; clean && (r < bottom); r++)
						blockImage.setRGB((c - block.bounds.leftCol), (r - block.bounds.topRow), white);
				}
			}
		}
		
		//	compute vertival match groups / grid line positions
		ArrayList vMatchColGroups = new ArrayList();
		TreeSet vMatchColGroup = new TreeSet();
		int lastVMatchCol = -minSignificantDistance;
		for (Iterator mit = vMatchCols.iterator(); mit.hasNext();) {
			Integer vMatchCol = ((Integer) mit.next());
			
			//	group ends, store it and start new one
			if (((vMatchCol.intValue() - lastVMatchCol) >= minSignificantDistance) && (vMatchColGroup.size() != 0)) {
				vMatchColGroups.add(vMatchColGroup);
				vMatchColGroup = new TreeSet();
			}
			
			//	add 
			vMatchColGroup.add(vMatchCol);
			lastVMatchCol = vMatchCol.intValue();
		}
		
		//	store last group
		if (vMatchColGroup.size() != 0)
			vMatchColGroups.add(vMatchColGroup);
		
		//	this one is likely a table
		block.isTable = true;
		
		//	extract table cells
		ImagePartRectangle tBounds = new ImagePartRectangle(Imaging.wrapImage(blockImage, null));
		Block tBlock = new Block(tBounds);
		int minWordMargin = (dpi / 30); // TODOne_above find out if this makes sense (will turn out in the long haul only, though)
		int minVerticalBlockMargin = minWordMargin; // using same as word margin here, as we already know we have a coherent table
		if (existingWords == null)
			getBlockStructure(tBlock, minVerticalBlockMargin, 1, minWordMargin, dpi, psm);
		else getBlockStructure(tBlock, existingWords, psm);
		
		//	adjust table content back to original table block boundaries, and wrap lines in cells
		Line[] tLines = tBlock.getLines();
//		System.out.println("Got table with " + tLines.length + " lines:");
		ArrayList cells = new ArrayList();
		for (int l = 0; l < tLines.length; l++) {
			TableCell cell = new TableCell(block.bounds.getSubRectangle(
					(tLines[l].bounds.leftCol + block.bounds.leftCol),
					(tLines[l].bounds.rightCol + block.bounds.leftCol),
					(tLines[l].bounds.topRow + block.bounds.topRow),
					(tLines[l].bounds.bottomRow + block.bounds.topRow)
				));
			Line line = new Line(block.bounds.getSubRectangle(
					(tLines[l].bounds.leftCol + block.bounds.leftCol),
					(tLines[l].bounds.rightCol + block.bounds.leftCol),
					(tLines[l].bounds.topRow + block.bounds.topRow),
					(tLines[l].bounds.bottomRow + block.bounds.topRow)
				));
			Word[] tWords = tLines[l].getWords();
//			System.out.println(" - got line with " + tWords.length + " words");
			for (int w = 0; w < tWords.length; w++) {
				line.addWord(new Word(block.bounds.getSubRectangle(
						(tWords[w].bounds.leftCol + block.bounds.leftCol),
						(tWords[w].bounds.rightCol + block.bounds.leftCol),
						(tWords[w].bounds.topRow + block.bounds.topRow),
						(tWords[w].bounds.bottomRow + block.bounds.topRow)
					)));
			}
			cell.addLine(line);
			cells.add(cell);
		}
//		
//		//	TODOne remove this (and all rendering) after tests
//		try {
//			ImageIO.write(blockImage, "png", new File("E:/Testdaten/PdfExtract/TableImage." + System.currentTimeMillis() + ".png"));
//		} catch (IOException ioe) {}
		
		//	compare distance between centroids of horizontal MatchRowGroups from grid elimination
		ArrayList hMatchRowGroupCentroids = new ArrayList();
		for (int g = 0; g < hMatchRowGroups.size(); g++) {
			int min = ((Integer) ((TreeSet) hMatchRowGroups.get(g)).first()).intValue();
			int max = ((Integer) ((TreeSet) hMatchRowGroups.get(g)).last()).intValue();
			hMatchRowGroupCentroids.add(new Integer((min + max) / 2));
//			System.out.println("Got horizontal centroid: " + ((min + max) / 2));
		}
		int minRowCentroidDistance = block.bounds.bottomRow;
		int maxRowCentroidDistance = 0;
		for (int c = 1; c < hMatchRowGroupCentroids.size(); c++) {
			int tc = ((Integer) hMatchRowGroupCentroids.get(c-1)).intValue();
			int bc = ((Integer) hMatchRowGroupCentroids.get(c)).intValue();
			int cd = (bc - tc);
			minRowCentroidDistance = Math.min(minRowCentroidDistance, cd);
			maxRowCentroidDistance = Math.max(maxRowCentroidDistance, cd);
		}
		
		//	compare distance between centroids of vertical MatchColGroups from grid elimination
		ArrayList vMatchColGroupCentroids = new ArrayList();
		for (int g = 0; g < vMatchColGroups.size(); g++) {
			int min = ((Integer) ((TreeSet) vMatchColGroups.get(g)).first()).intValue();
			int max = ((Integer) ((TreeSet) vMatchColGroups.get(g)).last()).intValue();
			vMatchColGroupCentroids.add(new Integer((min + max) / 2));
//			System.out.println("Got vertical centroid: " + ((min + max) / 2));
		}
		int minColCentroidDistance = block.bounds.rightCol;
		int maxColCentroidDistance = 0;
		for (int c = 1; c < vMatchColGroupCentroids.size(); c++) {
			int lc = ((Integer) vMatchColGroupCentroids.get(c-1)).intValue();
			int rc = ((Integer) vMatchColGroupCentroids.get(c)).intValue();
			int cd = (rc - lc);
			minColCentroidDistance = Math.min(minColCentroidDistance, cd);
			maxColCentroidDistance = Math.max(maxColCentroidDistance, cd);
		}
		
		//	sort cells top-down for merging
		Collections.sort(cells, new Comparator() {
			public int compare(Object o1, Object o2) {
				TableCell c1 = ((TableCell) o1);
				TableCell c2 = ((TableCell) o2);
				return (c1.bounds.topRow - c2.bounds.topRow);
			}
		});
		
		//	count inner centroids (increment each by 1 and their product is the number of fields, save for multi-row or multi-column cells)
		int hInnerCentroids = 1;
		for (int h = 0; h < hMatchRowGroupCentroids.size(); h++) {
			int hc = ((Integer) hMatchRowGroupCentroids.get(h)).intValue();
			boolean ca = false;
			boolean cb = false;
			for (int c = 0; c < cells.size(); c++) {
				TableCell cell = ((TableCell) cells.get(c));
				if (cell.bounds.bottomRow < hc)
					ca = true;
				if (hc < cell.bounds.topRow)
					cb = true;
				if (ca && cb) {
					hInnerCentroids++;
					break;
				}
			}
		}
		int vInnerCentroids = 1;
		for (int v = 0; v < vMatchColGroupCentroids.size(); v++) {
			int vc = ((Integer) vMatchColGroupCentroids.get(v)).intValue();
			boolean cl = false;
			boolean cr = false;
			for (int c = 0; c < cells.size(); c++) {
				TableCell cell = ((TableCell) cells.get(c));
				if (cell.bounds.rightCol < vc)
					cl = true;
				if (vc < cell.bounds.leftCol)
					cr = true;
				if (cl && cr) {
					vInnerCentroids++;
					break;
				}
			}
		}
//		System.out.println("Got at most " + vInnerCentroids + "x" + hInnerCentroids + " cells");
		boolean gridIncomplete = false;
		//	this definition should catch the desired tables, as an incomplete grid gets more and more unlikely with more inner grid lines painted
		gridIncomplete = (((minRowCentroidDistance * hInnerCentroids) < maxRowCentroidDistance) || ((minColCentroidDistance * vInnerCentroids) < maxColCentroidDistance));
		
		//	TODO figure out how to handle tables that have a partially full grid, e.g. only every third row boundary drawn
		
		//	grid might be incomplete ==> be careful with merging cells
		if (gridIncomplete) {
			
			//	merge cells that are on top of one another if there is another cell overlapping them on the side (probably a two-line entry)
			for (int c = 0; c < cells.size(); c++) {
				TableCell cell = ((TableCell) cells.get(c));
				for (int m = (c+1); m < cells.size(); m++) {
					TableCell mCell = ((TableCell) cells.get(m));
					if ((mCell.bounds.rightCol < cell.bounds.leftCol) || (cell.bounds.rightCol < mCell.bounds.leftCol))
						continue;
					boolean merge = false;
					for (int t = 0; t < cells.size(); t++) {
						if ((t == c) || (t == m))
							continue;
						TableCell tCell = ((TableCell) cells.get(t));
						boolean oc = ((cell.bounds.topRow < tCell.bounds.bottomRow) && (tCell.bounds.topRow < cell.bounds.bottomRow));
						boolean om = ((mCell.bounds.topRow < tCell.bounds.bottomRow) && (tCell.bounds.topRow < mCell.bounds.bottomRow));
						if (oc && om) {
							merge = true;
							break;
						}
					}
					if (merge) {
						ImagePartRectangle[] cbs = {cell.bounds, mCell.bounds};
						ImagePartRectangle cb = Imaging.getHull(cbs);
						Imaging.copyBounds(cb, cell.bounds);
						cell.lines.addAll(mCell.lines);
						cells.remove(m--);
					}
				}
			}
			
			//	sort cells top-down and left-right for row grouping
			Collections.sort(cells, new Comparator() {
				public int compare(Object o1, Object o2) {
					TableCell c1 = ((TableCell) o1);
					TableCell c2 = ((TableCell) o2);
					if ((c1.bounds.topRow < c2.bounds.bottomRow) && (c2.bounds.topRow < c1.bounds.bottomRow))
						return (c1.bounds.leftCol - c2.bounds.leftCol);
					else return (c1.bounds.topRow - c2.bounds.topRow);
				}
			});
			
			//	group cells into rows
			ArrayList rowCells = new ArrayList();
			TableCell lCell = null;
			for (int c = 0; c < cells.size(); c++) {
				TableCell cell = ((TableCell) cells.get(c));
				if ((lCell != null) && ((cell.bounds.topRow >= lCell.bounds.bottomRow) || (lCell.bounds.topRow >= cell.bounds.bottomRow))) {
					ImagePartRectangle[] cbs = new ImagePartRectangle[rowCells.size()];
					for (int r = 0; r < rowCells.size(); r++)
						cbs[r] = ((TableCell) rowCells.get(r)).bounds;
					ImagePartRectangle rb = Imaging.getHull(cbs);
					TableRow row = new TableRow(rb);
					for (int r = 0; r < rowCells.size(); r++)
						row.addCell((TableCell) rowCells.get(r));
					block.addRow(row);
					rowCells.clear();
				}
				rowCells.add(cell);
				lCell = cell;
			}
			
			//	store table row
			if (rowCells.size() != 0) {
				ImagePartRectangle[] cbs = new ImagePartRectangle[rowCells.size()];
				for (int r = 0; r < rowCells.size(); r++)
					cbs[r] = ((TableCell) rowCells.get(r)).bounds;
				ImagePartRectangle rb = Imaging.getHull(cbs);
				TableRow row = new TableRow(rb);
				for (int r = 0; r < rowCells.size(); r++)
					row.addCell((TableCell) rowCells.get(r));
				block.addRow(row);
			}
		}
		
		//	we have a full grid ==> use it
		else {
			
			//	merge cells that are on top of one another with no grid line in between into table cells
			for (int c = 0; c < cells.size(); c++) {
				TableCell cell = ((TableCell) cells.get(c));
				for (int m = (c+1); m < cells.size(); m++) {
					TableCell mCell = ((TableCell) cells.get(m));
					if ((mCell.bounds.rightCol < cell.bounds.leftCol) || (cell.bounds.rightCol < mCell.bounds.leftCol))
						continue;
					ImagePartRectangle[] cbs = {cell.bounds, mCell.bounds};
					ImagePartRectangle mb = Imaging.getHull(cbs);
					boolean merge = true;
					for (int g = 0; g < gridLines.size(); g++) {
						ImagePartRectangle gridLine = ((ImagePartRectangle) gridLines.get(g));
						if ((mb.rightCol <= gridLine.leftCol) || (gridLine.rightCol <= mb.leftCol))
							continue;
						if ((mb.bottomRow <= gridLine.topRow) || (gridLine.bottomRow <= mb.topRow))
							continue;
						merge = false;
						break;
					}
					if (merge) {
						Imaging.copyBounds(mb, cell.bounds);
						cell.lines.addAll(mCell.lines);
						cells.remove(m--);
					}
				}
			}
			
			//	compute row span of each cell
			for (int c = 0; c < cells.size(); c++) {
				TableCell cell = ((TableCell) cells.get(c));
				for (int t = 0; t < hMatchRowGroupCentroids.size(); t++) {
					Integer mgc = ((Integer) hMatchRowGroupCentroids.get(t));
					if ((cell.bounds.topRow < mgc.intValue()) && (mgc.intValue() < cell.bounds.bottomRow))
						cell.rowSpan++;
				}
			}
			
			//	sort cells left-right for row grouping
			Collections.sort(cells, new Comparator() {
				public int compare(Object o1, Object o2) {
					TableCell c1 = ((TableCell) o1);
					TableCell c2 = ((TableCell) o2);
					return (c1.bounds.leftCol - c2.bounds.leftCol);
				}
			});
			
			//	group cells into rows
			for (int r = 0; r < hMatchRowGroupCentroids.size(); r++) {
				ArrayList rowCells = new ArrayList();
				Integer mgc = ((Integer) hMatchRowGroupCentroids.get(r));
				for (int c = 0; c < cells.size(); c++) {
					TableCell cell = ((TableCell) cells.get(c));
					if (cell.bounds.topRow < mgc.intValue()) {
						rowCells.add(cell);
						cells.remove(c--);
					}
				}
				if (rowCells.isEmpty())
					continue;
				
				ImagePartRectangle[] cbs = new ImagePartRectangle[rowCells.size()];
				for (int c = 0; c < rowCells.size(); c++)
					cbs[c] = ((TableCell) rowCells.get(c)).bounds;
				ImagePartRectangle rb = Imaging.getHull(cbs);
				TableRow row = new TableRow(rb);
				for (int c = 0; c < rowCells.size(); c++)
					row.addCell((TableCell) rowCells.get(c));
				block.addRow(row);
			}
			if (cells.size() != 0) {
				ImagePartRectangle[] cbs = new ImagePartRectangle[cells.size()];
				for (int c = 0; c < cells.size(); c++)
					cbs[c] = ((TableCell) cells.get(c)).bounds;
				ImagePartRectangle rb = Imaging.getHull(cbs);
				TableRow row = new TableRow(rb);
				for (int c = 0; c < cells.size(); c++)
					row.addCell((TableCell) cells.get(c));
				block.addRow(row);
			}
		}
		
		//	compute col span of cells
		TableRow[] rows = block.getRows();
		for (int r = 0; r < rows.length; r++) {
			TableCell[] rCells = rows[r].getCells();
			for (int c = 0; c < rCells.length; c++) {
				for (int tr = 0; tr < rows.length; tr++) {
					if (tr == r)
						continue;
					TableCell[] tCells = rows[tr].getCells();
					int tCellsSpanned = 0;
					for (int tc = 0; tc < tCells.length; tc++) {
						if ((rCells[c].bounds.rightCol > tCells[tc].bounds.leftCol) && (tCells[tc].bounds.rightCol > rCells[c].bounds.leftCol))
							tCellsSpanned++;
					}
					rCells[c].colSpan = Math.max(rCells[c].colSpan, tCellsSpanned);
				}
			}
		}
		
		//	adjust row bounds to very small so empty cells do not grow out of bounds
		for (int r = 0; r < rows.length; r++) {
			TableCell[] rCells = rows[r].getCells();
			for (int c = 0; c < rCells.length; c++) {
				rows[r].bounds.topRow = Math.max(rows[r].bounds.topRow, rCells[c].bounds.topRow);
				rows[r].bounds.bottomRow = Math.min(rows[r].bounds.bottomRow, rCells[c].bounds.bottomRow);
			}
		}
		
		//	build table grid lists
		ArrayList[] rowCells = new ArrayList[rows.length];
		for (int r = 0; r < rows.length; r++)
			rowCells[r] = new ArrayList();
		for (int r = 0; r < rows.length; r++) {
			TableCell[] rCells = rows[r].getCells();
			for (int c = 0; c < rCells.length; c++) {
				rowCells[r].add(rCells[c]);
				for (int s = 1; s < rCells[c].rowSpan; s++)
					rowCells[r+s].add(rCells[c]);
			}
		}
		for (int r = 0; r < rows.length; r++)
			Collections.sort(rowCells[r], new Comparator() {
				public int compare(Object o1, Object o2) {
					TableCell c1 = ((TableCell) o1);
					TableCell c2 = ((TableCell) o2);
					return (c1.bounds.leftCol - c2.bounds.leftCol);
				}
			});
		
		//	fill in empty cells
		for (int r = 0; r < rows.length; r++) {
			for (int c = 0; c <= rowCells[r].size(); c++) {
				int left = ((c == 0) ? block.bounds.leftCol : ((TableCell) rowCells[r].get(c-1)).bounds.rightCol);
				int right = ((c == rowCells[r].size()) ? block.bounds.rightCol : ((TableCell) rowCells[r].get(c)).bounds.leftCol);
				for (int tr = 0; tr < rows.length; tr++) {
					if (tr == r)
						continue;
					TableCell[] tCells = rows[tr].getCells();
					for (int tc = 0; tc < tCells.length; tc++) {
						if ((tCells[tc].bounds.leftCol < left) || (right < tCells[tc].bounds.rightCol))
							continue;
						
						//	generate filler cell
						TableCell fCell = new TableCell(block.bounds.getSubRectangle(
								(tCells[tc].bounds.leftCol),
								(tCells[tc].bounds.rightCol),
								(rows[r].bounds.topRow),
								(rows[r].bounds.bottomRow)
							));
						
						//	store cell in grid list
						rowCells[r].add(c, fCell);
						
						//	add cell to row
						rows[r].addCell(fCell);
						Collections.sort(rows[r].cells, new Comparator() {
							public int compare(Object o1, Object o2) {
								TableCell c1 = ((TableCell) o1);
								TableCell c2 = ((TableCell) o2);
								return (c1.bounds.leftCol - c2.bounds.leftCol);
							}
						});
						
						//	start over
						tc = tCells.length;
						tr = rows.length;
					}
				}
			}
		}
		
		//	adjust row bounds back to normal, ignoring rowspan
		for (int r = 0; r < rows.length; r++) {
			TableCell[] rCells = rows[r].getCells();
			for (int c = 0; c < rCells.length; c++) {
				rows[r].bounds.topRow = Math.min(rows[r].bounds.topRow, rCells[c].bounds.topRow);
				if (rCells[c].rowSpan < 2)
					rows[r].bounds.bottomRow = Math.max(rows[r].bounds.bottomRow, rCells[c].bounds.bottomRow);
			}
		}
	}
	
	private static void getBlockStructure(Block block, int minVerticalBlockMargin, int minLineMargin, int minWordMargin, int dpi, ProgressMonitor psm) {
		psm.setInfo("   - doing block " + block.getBoundingBox().toString());
		
		double maxSplitSlopeAngle;
		int minZigzagPartLength;
		double maxZigzagSplitSlopeAngle;
		int maxZigzagSlope;
		
		//	do conservative initial line split, so not to zigzag through page headers
		maxSplitSlopeAngle = 0.2;
		ImagePartRectangle[] lBlocks = Imaging.splitIntoRows(block.bounds, 1, maxSplitSlopeAngle, -1, 0);
		psm.setInfo("     - got " + lBlocks.length + " initial lines");
		
		//	split initial lines into columns
		ArrayList bLines = new ArrayList();
		for (int l = 0; l < lBlocks.length; l++) {
			lBlocks[l] = Imaging.narrowLeftAndRight(lBlocks[l]);
			if (lBlocks[l].isEmpty())
				continue;
			ImagePartRectangle[] cBlocks = Imaging.splitIntoColumns(lBlocks[l], (minVerticalBlockMargin * 3));
			if (cBlocks.length == 1)
				bLines.add(lBlocks[l]);
			else for (int c = 0; c < cBlocks.length; c++) {
				cBlocks[c] = Imaging.narrowTopAndBottom(cBlocks[c]);
				if (cBlocks[c].isEmpty())
					continue;
				bLines.add(cBlocks[c]);
			}
		}
		psm.setInfo("     - got " + bLines.size() + " col block lines at margin " + (minVerticalBlockMargin * 3));
		
		//	do more aggressive line splitting, but only for lines that are higher than a fifth of the dpi, as anything below that will hardly consist of two lines
		int minLineHeight = (dpi / 10); // a single line of text will rarely be lower than one thenth of an inch in total
		int minResplitHeight = (minLineHeight * 2); // the minimum height of two lines, we can securely keep anything below this threshold
		maxSplitSlopeAngle = 0.0;//0.2; zigzagging does it alone
		minZigzagPartLength = (dpi / 2);
		maxZigzagSplitSlopeAngle = 1.0;
		maxZigzagSlope = Math.abs((int) (Math.tan(maxZigzagSplitSlopeAngle * Math.PI / 180) * ((double) (block.bounds.rightCol - block.bounds.leftCol))));
		for (int l = 0; l < bLines.size(); l++) {
			ImagePartRectangle line = ((ImagePartRectangle) bLines.get(l));
			if ((line.bottomRow - line.topRow) < minResplitHeight)
				continue;
			lBlocks = Imaging.splitIntoRows(line, 1, maxSplitSlopeAngle, minZigzagPartLength, maxZigzagSlope);
			if (lBlocks.length > 1) {
				bLines.set(l, lBlocks[0]);
				for (int sl = 1; sl < lBlocks.length; sl++) {
					l++;
					bLines.add(l, lBlocks[sl]);
				}
			}
		}
		
		//	recover line blocks
		lBlocks = ((ImagePartRectangle[]) bLines.toArray(new ImagePartRectangle[bLines.size()]));
		psm.setInfo("     - got " + lBlocks.length + " block lines");
		if (lBlocks.length == 0)
			return;
		
		//	compute average line height
		int lineHeightSum = 0;
		int lineCount = 0;
		for (int l = 0; l < lBlocks.length; l++) {
			
			//	skip over lines higher than one inch
			if (dpi < (lBlocks[l].bottomRow - lBlocks[l].topRow))
				continue;
			
			//	count other lines
			lineHeightSum += (lBlocks[l].bottomRow - lBlocks[l].topRow);
			lineCount++;
		}
		int avgLineHeight = ((lineCount == 0) ? -1 : ((lineHeightSum + (lineCount / 2)) / lineCount));
		
		//	save everything higher than one tenth of an inch, no matter how big surrounding lines are
		int minWordHight = (dpi / 10);
		
		//	filter out lines narrower than one thirtieth of an inch (should still save single 1's)
		int minLineWidth = (dpi / 30);
		
		//	store lines in block
		ImagePartRectangle lostLine = null;
		for (int l = 0; l < lBlocks.length; l++) {
			lBlocks[l] = Imaging.narrowLeftAndRight(lBlocks[l]);
			int lineHeight = (lBlocks[l].bottomRow - lBlocks[l].topRow);
			
			//	keep aside very low lines, likely stains, etc. ... or dots of an i or j, accents of upper case latters, ...
			if (lineHeight < Math.min((avgLineHeight / 2), minWordHight)) {
				lostLine = lBlocks[l];
				continue;
			}
			
			//	filter out lines narrower than minimum width
			int lineWidth = (lBlocks[l].rightCol - lBlocks[l].leftCol);
			if (lineWidth < minLineWidth) {
				lostLine = null;
				continue;
			}
			
			//	join any below-minimum-height line to adjacent smaller-than-average line immediately below it (might be cut-off dot of an i or j)
			//	however, forget about accents of upper case letters, as they blow the line metrics anyways
			if ((lostLine != null)
					&& (lBlocks[l].leftCol <= lostLine.leftCol) && (lostLine.rightCol <= lBlocks[l].rightCol)
					&& (lineHeight < avgLineHeight)
				)
				lBlocks[l].topRow = Math.min(lBlocks[l].topRow, lostLine.topRow);
			block.addLine(new Line(lBlocks[l]));
			lostLine = null;
		}
		
//		//	split lines into words, using adaptive margin
//		for (int l = 0; l < block.lines.size(); l++) {
//			Line line = ((Line) block.lines.get(l));
//			psm.setInfo("     - doing line " + line.getBoundingBox().toString());
//			ImagePartRectangle[] lineChops = Imaging.splitIntoColumns(line.bounds, 1);
//			psm.setInfo("       - got " + lineChops.length + " chops in line " + l);
//			
//			int maxChopDistance = 0;
//			for (int c = 1; c < lineChops.length; c++) {
//				int cd = (lineChops[c].leftCol - lineChops[c-1].rightCol);
//				maxChopDistance = Math.max(maxChopDistance, cd);
//			}
//			System.out.println(" - max chop distance is " + maxChopDistance);
//			int[] chopDistanceCounts = new int[maxChopDistance + 1];
//			Arrays.fill(chopDistanceCounts, 0);
//			for (int c = 1; c < lineChops.length; c++) {
//				int cd = (lineChops[c].leftCol - lineChops[c-1].rightCol);
//				chopDistanceCounts[cd]++;
//			}
//			System.out.println(" - chop distance distribution");
//			for (int d = 0; d < chopDistanceCounts.length; d++)
//				System.out.println("   - " + d + ": " + chopDistanceCounts[d]);
//			
//			//	TODO use chop distance findings
//		}
		
		//	split lines into words
		for (int l = 0; l < block.lines.size(); l++) {
			Line line = ((Line) block.lines.get(l));
			psm.setInfo("     - doing line " + line.getBoundingBox().toString());
			ImagePartRectangle[] lwBlocks = Imaging.splitIntoColumns(line.bounds, minWordMargin);
			psm.setInfo("       - got " + lwBlocks.length + " raw words in line " + l + " at margin " + minWordMargin);
			
			//	split words at gaps at least twice as wide as word-local average, and at least half the global minimum margin
			ArrayList lwBlockList = new ArrayList();
			for (int w = 0; w < lwBlocks.length; w++) {
				
//				//	try to split into individual characters (some might stick together, but thats OK)
//				ImagePartRectangle[] lwbChars = Imaging.splitIntoColumns(lwBlocks[w], 1, 0, true);
//				ImagePartRectangle[] ilwbChars = Imaging.splitIntoColumns(lwBlocks[w], 1, italicShearDeg, false);
//				psm.setInfo("           - got " + lwbChars.length + " chars in word " + w + ", " + ilwbChars.length + " italic chars");
//				
//				//	compute average and maximum distances
//				int lwbCharMarginSum = 0;
//				int maxLwbCharMargin = 0;
//				int lwbCharSpacingSum = 0;
//				int minLwbCharSpacing = Integer.MAX_VALUE;
//				int maxLwbCharSpacing = 0;
//				for (int c = 1; c < lwbChars.length; c++) {
//					int cm = (lwbChars[c].leftCol - lwbChars[c-1].rightCol);
//					lwbCharMarginSum += cm;
//					maxLwbCharMargin = Math.max(maxLwbCharMargin, cm);
//					int cs = (lwbChars[c].leftCol - lwbChars[c-1].leftCol);
//					lwbCharSpacingSum += cs;
//					minLwbCharSpacing = Math.min(minLwbCharSpacing, cs);
//					maxLwbCharSpacing = Math.max(maxLwbCharSpacing, cs);
//				}
//				int avgLwbCharMargin = ((lwbChars.length < 2) ? 0 : ((lwbCharMarginSum + ((lwbChars.length - 1) / 2)) / (lwbChars.length - 1)));
//				int avgLwbCharSpacing = ((lwbChars.length < 2) ? 0 : ((lwbCharSpacingSum + ((lwbChars.length - 1) / 2)) / (lwbChars.length - 1)));
//				psm.setInfo("           - average character margin in raw word " + w + " computed as " + avgLwbCharMargin + ", max is " + maxLwbCharMargin + ", average spacing is " + avgLwbCharSpacing + ", min is " + minLwbCharSpacing + ", max is " + maxLwbCharSpacing);
//				int ilwbCharMarginSum = 0;
//				int maxIlwbCharMargin = 0;
//				int ilwbCharSpacingSum = 0;
//				int minIlwbCharSpacing = Integer.MAX_VALUE;
//				int maxIlwbCharSpacing = 0;
//				for (int c = 1; c < ilwbChars.length; c++) {
//					int cm = (ilwbChars[c].leftCol - ilwbChars[c-1].rightCol);
//					ilwbCharMarginSum += cm;
//					maxIlwbCharMargin = Math.max(maxIlwbCharMargin, cm);
//					int cs = (ilwbChars[c].leftCol - ilwbChars[c-1].leftCol);
//					ilwbCharSpacingSum += cs;
//					minIlwbCharSpacing = Math.min(minIlwbCharSpacing, cs);
//					maxIlwbCharSpacing = Math.max(maxIlwbCharSpacing, cs);
//				}
//				int avgIlwbCharMargin = ((ilwbChars.length < 2) ? 0 : ((ilwbCharMarginSum + ((ilwbChars.length - 1) / 2)) / (ilwbChars.length - 1)));
//				int avgIlwbCharSpacing = ((ilwbChars.length < 2) ? 0 : ((ilwbCharSpacingSum + ((ilwbChars.length - 1) / 2)) / (ilwbChars.length - 1)));
//				psm.setInfo("           - average italics character margin in raw word " + w + " computed as " + avgIlwbCharMargin + ", max is " + maxIlwbCharMargin + ", average spacing is " + avgIlwbCharSpacing + ", min is " + minIlwbCharSpacing + ", max is " + maxIlwbCharSpacing);
//				
//				boolean spacingCatch = true;
//				
//				//	we have outliers, and also a sufficiently large gap that is not due to monospaced typeset ==> do re-split
//				if (((avgLwbCharMargin * 2) <= maxLwbCharMargin) && ((minWordMargin / 2) <= maxLwbCharMargin) && (maxIlwbCharMargin <= maxLwbCharMargin) && (!spacingCatch || (maxLwbCharMargin < (maxLwbCharSpacing - minLwbCharSpacing)) || (((minWordMargin * 4) / 5) < maxLwbCharMargin))) {
//					int resplitMargin = Math.max((minWordMargin / 2), (avgLwbCharMargin * 2));
//					psm.setInfo("           - doing re-split with margin " + resplitMargin);
//					ImagePartRectangle[] lwbWords = Imaging.splitIntoColumns(lwBlocks[w], resplitMargin, 0, true);
//					psm.setInfo("           --> split raw word " + w + " into " + lwbWords.length + " words");
//					lwBlockList.addAll(Arrays.asList(lwbWords));
//				}
//				
//				//	we have outliers, and also a sufficiently large gap that is not due to monospaced typeset ==> do re-split
//				else if (((avgIlwbCharMargin * 2) <= maxIlwbCharMargin) && ((minWordMargin / 2) <= maxIlwbCharMargin) && (maxLwbCharMargin < maxIlwbCharMargin) && (!spacingCatch || (maxIlwbCharMargin < (maxIlwbCharSpacing - minIlwbCharSpacing))  || (((minWordMargin * 4) / 5) < maxIlwbCharMargin))) {
//					int resplitMargin = Math.max((minWordMargin / 2), (avgIlwbCharMargin * 2));
//					psm.setInfo("           - doing italic re-split with margin " + resplitMargin);
//					ImagePartRectangle[] lwbWords = Imaging.splitIntoColumns(lwBlocks[w], resplitMargin, italicShearDeg, true);
//					psm.setInfo("           --> split raw word " + w + " into " + lwbWords.length + " words");
//					lwBlockList.addAll(Arrays.asList(lwbWords));
//				}
//				
//				//	no resplit possible or sensible
//				else 
					lwBlockList.add(lwBlocks[w]);
			}
			
			//	refresh line word blocks
			if (lwBlocks.length < lwBlockList.size())
				lwBlocks = ((ImagePartRectangle[]) lwBlockList.toArray(new ImagePartRectangle[lwBlockList.size()]));
			
			//	filter words
			psm.setInfo("       - got " + lwBlocks.length + " words in line " + l);
			boolean omittedFirst = false;
			boolean omittedLast = false;
			for (int w = 0; w < lwBlocks.length; w++) {
				lwBlocks[w] = Imaging.narrowTopAndBottom(lwBlocks[w]);
//				psm.setInfo("       - assessing " + lwBlocks[w].getId());
				int minWidth = (lwBlocks[w].rightCol - lwBlocks[w].leftCol);
				int maxWidth = 0;
				int minHeight = (lwBlocks[w].bottomRow - lwBlocks[w].topRow);
				int maxHeight = 0;
				
				//	split into columns with margin 1, then determine minimum and maximum width, and apply horizontal filters to those
				ImagePartRectangle[] vBlocks = Imaging.splitIntoColumns(lwBlocks[w], 1);
//				psm.setInfo("         - got " + vBlocks.length + " v-blocks");
				for (int v = 0; v < vBlocks.length; v++) {
					vBlocks[v] = Imaging.narrowTopAndBottom(vBlocks[v]);
//					psm.setInfo("           - " + vBlocks[v].getId());
					minWidth = Math.min(minWidth, (vBlocks[v].rightCol - vBlocks[v].leftCol));
					maxWidth = Math.max(maxWidth, (vBlocks[v].rightCol - vBlocks[v].leftCol));
				}
//				psm.setInfo("         - v-block widths between " + minWidth + " and " + maxWidth);
				
				//	split into rows with margin 1, then determine minimum and maximum heigth, and apply vertical filters to those
				ImagePartRectangle[] hBlocks = Imaging.splitIntoRows(lwBlocks[w], 1);
//				psm.setInfo("         - got " + hBlocks.length + " h-blocks");
				for (int h = 0; h < hBlocks.length; h++) {
					hBlocks[h] = Imaging.narrowLeftAndRight(hBlocks[h]);
//					psm.setInfo("           - " + hBlocks[h].getId());
					minHeight = Math.min(minHeight, (hBlocks[h].bottomRow - hBlocks[h].topRow));
					maxHeight = Math.max(maxHeight, (hBlocks[h].bottomRow - hBlocks[h].topRow));
				}
//				psm.setInfo("         - h-block heights between " + minHeight + " and " + maxHeight);
				
				//	sort out everything sized at most one hundredth of an inch in both dimensions
				if ((maxWidth * maxHeight) <= ((dpi / 100) * (dpi / 100))) {
					psm.setInfo("       --> stain omitted for size below " + (dpi / 100) + " x " + (dpi / 100));
					if (w == 0)
						omittedFirst = true;
					omittedLast = true;
					lwBlocks[w] = null;
					continue;
				}
				
//				//	sort out everything larger than one inch in either dimension TODOne find out if this is safe ==> seems not so, as German compounds can be longer ...
//				else if ((dpi < minWidth) || (dpi < minHeight)) {
//					psm.setInfo("     - word omitted for massive size " + Math.max(minWidth, minHeight) + " exceeding " + dpi + " in either dimension");
//					if (w == 0)
//						omittedFirst = true;
//					omittedLast = true;
//					lwBlocks[w] = null;
//					continue;
//				}
				//	sort out everything larger than two inches in either dimension TODOne SEEMS SO find out if this is safe now
				else if (((dpi * 2) < minWidth) || ((dpi * 2) < minHeight)) {
					psm.setInfo("       --> word omitted for massive size " + Math.max(minWidth, minHeight) + " exceeding " + (2*dpi) + " in either dimension");
					if (w == 0)
						omittedFirst = true;
					omittedLast = true;
					lwBlocks[w] = null;
					continue;
				}
				
				//	store word in line
				omittedLast = false;
				line.addWord(new Word(lwBlocks[w]));
				psm.setInfo("       --> word added");
			}
			
			//	catch empty lines
			if (line.words.isEmpty()) {
//				//	retain large empty lines, though, might be figures or tables
//				//	==> we do that at the block level now
//				if ((((line.bounds.rightCol - line.bounds.leftCol) * 2) < dpi) || (((line.bounds.bottomRow - line.bounds.topRow) * 2) < dpi))
//					block.lines.remove(l--);
				continue;
			}
			psm.setInfo("       - got " + line.words.size() + " words in line " + l + ", ultimately");
			
			//	correct line bounds
			if (omittedFirst || omittedLast) {
				int left = line.bounds.rightCol;
				int right = line.bounds.leftCol;
				int top = line.bounds.bottomRow;
				int bottom = line.bounds.topRow;
				for (int w = 0; w < lwBlocks.length; w++) {
					if (lwBlocks[w] == null)
						continue;
					left = Math.min(left, lwBlocks[w].leftCol);
					right = Math.max(right, lwBlocks[w].rightCol);
					top = Math.min(top, lwBlocks[w].topRow);
					bottom = Math.max(bottom, lwBlocks[w].bottomRow);
				}
				line.bounds.leftCol = left;
				line.bounds.rightCol = right;
				line.bounds.topRow = top;
				line.bounds.bottomRow = bottom;
			}
		}
		
		adjustWordsAndLines(block);
	}
	
	private static int getBlockStructure(Block block, BoundingBox[] existingWords, ProgressMonitor psm) {
		BoundingBox[] existingBlockWords = extractOverlapping(block.bounds, existingWords);
		psm.setInfo("   - doing block " + block.getBoundingBox().toString());
		
		//	sort words left to right
		Arrays.sort(existingBlockWords, new Comparator() {
			public int compare(Object o1, Object o2) {
				return (((BoundingBox) o1).left - ((BoundingBox) o2).left);
			}
		});
		
		//	group words into lines
		while (true) {
			ArrayList lineWords = new ArrayList();
			int lineLeft = -1;
			int lineRight = -1;
			int lineTop = -1;
			int lineBottom = -1;
			BoundingBox lastWord = null;
			for (int w = 0; w < existingBlockWords.length; w++) {
				if (existingBlockWords[w] == null)
					continue;
				
				//	start line
				if (lineWords.isEmpty()) {
					lineWords.add(existingBlockWords[w]);
					lineLeft = existingBlockWords[w].left;
					lineRight = existingBlockWords[w].right;
					lineTop = existingBlockWords[w].top;
					lineBottom = existingBlockWords[w].bottom;
					lastWord = existingBlockWords[w];
					existingBlockWords[w] = null;
					psm.setInfo("     - starting line with " + lastWord);
					continue;
				}
				
				//	this word is outside the line
				if (existingBlockWords[w].bottom < lineTop)
					continue;
				if (lineBottom < existingBlockWords[w].top)
					continue;
				
				//	word completely inside line
				if ((existingBlockWords[w].top >= lineTop) && (existingBlockWords[w].bottom <= lineBottom)) {}
				
				//	line completely inside word
				else if ((existingBlockWords[w].top <= lineTop) && (existingBlockWords[w].bottom >= lineBottom)) {}
				
				//	check for at least 50% overlap
				else {
					int maxTop = Math.max(existingBlockWords[w].top, lineTop);
					int minBottom = Math.min(existingBlockWords[w].bottom, lineBottom);
					int overlap = (minBottom - maxTop);
					if ((overlap * 2) < (lineBottom - lineTop))
						continue;
					if ((overlap * 2) < (existingBlockWords[w].bottom - existingBlockWords[w].top))
						continue;
				}
				
				//	word overlaps with last (can happen due to in-word font change in PDF) ==> merge them
				if (lastWord.right >= existingBlockWords[w].left) {
					lastWord = new BoundingBox(
							lastWord.left, 
							existingBlockWords[w].right, 
							Math.min(existingBlockWords[w].top, lastWord.top), 
							Math.max(existingBlockWords[w].bottom, lastWord.bottom)
						);
					lineWords.set((lineWords.size()-1), lastWord);
					psm.setInfo("       - merged overlapping words " + lastWord);
				}
				
				//	separate word, add it
				else {
					lastWord = existingBlockWords[w];
					lineWords.add(lastWord);
					psm.setInfo("       - added word " + lastWord);
				}
				
				//	adjust line bounds
				lineLeft = Math.min(existingBlockWords[w].left, lineLeft);
				lineRight = Math.max(existingBlockWords[w].right, lineRight);
				lineTop = Math.min(existingBlockWords[w].top, lineTop);
				lineBottom = Math.max(existingBlockWords[w].bottom, lineBottom);
				
				//	mark word as expended
				existingBlockWords[w] = null;
			}
			
			//	no words remaining
			if (lineWords.isEmpty())
				break;
			
			//	create and store line
			Line line = new Line(block.bounds.getSubRectangle(lineLeft, lineRight, lineTop, lineBottom));
			block.addLine(line);
			psm.setInfo("     - created line " + lastWord + " with " + lineWords.size() + " words");
			
			//	add words
			for (int w = 0; w < lineWords.size(); w++) {
				BoundingBox word = ((BoundingBox) lineWords.get(w));
				line.addWord(new Word(line.bounds.getSubRectangle(word.left, word.right, word.top, word.bottom)));
			}
		}
		
		//	sort lines top to bottom
		Collections.sort(block.lines, new Comparator() {
			public int compare(Object o1, Object o2) {
				return (((Line) o1).bounds.topRow - ((Line) o2).bounds.topRow);
			}
		});
		
		//	easy to tell how many words we have ...
		return existingBlockWords.length;
	}
	
	private static BoundingBox[] extractOverlapping(ImagePartRectangle ipr, BoundingBox[] bbs) {
		ArrayList bbl = new ArrayList();
		for (int b = 0; b < bbs.length; b++) {
			if (overlaps(ipr, bbs[b]))
				bbl.add(bbs[b]);
		}
		return ((BoundingBox[]) bbl.toArray(new BoundingBox[bbl.size()]));
	}
	private static boolean overlaps(ImagePartRectangle ipr, BoundingBox bb) {
		if (bb.bottom <= ipr.topRow)
			return false;
		else if (ipr.bottomRow <= bb.top)
			return false;
		else if (bb.right <= ipr.leftCol)
			return false;
		else if (ipr.rightCol <= bb.left)
			return false;
		else return true;
	}
	
	private static final int wordAdjustmentSafetyMargin = 0;
	private static void adjustWordsAndLines(Block block) {
		byte[][] brightness = block.bounds.getImage().getBrightness();
		if (block.lineSplitClean())
			return;
		
		Line[] lines = block.getLines();
		if (lines.length == 0)
			return;
		
		int[] wordTopRows = new int[lines.length];
		int[] wordBottomRows = new int[lines.length];
		for (int l = 0; l < lines.length; l++) {
			wordTopRows[l] = lines[l].bounds.topRow;
			wordBottomRows[l] = lines[l].bounds.bottomRow;
			Word[] words = lines[l].getWords();
			if (words.length == 0)
				continue;
			
			//	above and below lines for quick access
			Word[] wordsAbove = ((l == 0) ? null : lines[l-1].getWords());
			Word[] wordsBelow = (((l+1) == lines.length) ? null : lines[l+1].getWords());
			
			//	expand words in all directions to cover letters fully, as sloped line cuts might have severed letters
			for (int w = 0; w < words.length; w++) {
				boolean expanded;
				
				//	find maximum space around current word
				int leftBound = ((w == 0) ? block.bounds.leftCol : (words[w-1].bounds.rightCol + wordAdjustmentSafetyMargin));
				int rightBound = (((w+1) == words.length) ? block.bounds.rightCol : (words[w+1].bounds.leftCol - wordAdjustmentSafetyMargin));
				
				int topBound = block.bounds.topRow;
				boolean noWordAbove = true;
				for (int wa = 0; (wordsAbove != null) && (wa < wordsAbove.length); wa++)
					if ((leftBound < wordsAbove[wa].bounds.rightCol) && (wordsAbove[wa].bounds.leftCol < rightBound)) {
						topBound = Math.max(topBound, (wordsAbove[wa].bounds.bottomRow + wordAdjustmentSafetyMargin));
						noWordAbove = false;
					}
				int la = l-2;
				while (noWordAbove && (la > -1)) {
					Word[] eWordsAbove = lines[la].getWords();
					for (int wa = 0; (wa < eWordsAbove.length); wa++)
						if ((leftBound < eWordsAbove[wa].bounds.rightCol) && (eWordsAbove[wa].bounds.leftCol < rightBound)) {
							topBound = Math.max(topBound, (eWordsAbove[wa].bounds.bottomRow + wordAdjustmentSafetyMargin));
							noWordAbove = false;
						}
					if (noWordAbove)
						la--;
				}
				
				int bottomBound = block.bounds.bottomRow;
				boolean noWordBelow = true;
				for (int wb = 0; (wordsBelow != null) && (wb < wordsBelow.length); wb++) {
					if ((leftBound < wordsBelow[wb].bounds.rightCol) && (wordsBelow[wb].bounds.leftCol < rightBound)) {
						bottomBound = Math.min(bottomBound, (wordsBelow[wb].bounds.topRow - wordAdjustmentSafetyMargin));
						noWordBelow = false;
					}
				}
				int lb = l+2;
				while (noWordBelow && (lb < lines.length)) {
					Word[] eWordsBelow = lines[lb].getWords();
					for (int wb = 0; (wb < eWordsBelow.length); wb++) {
						if ((leftBound < eWordsBelow[wb].bounds.rightCol) && (eWordsBelow[wb].bounds.leftCol < rightBound)) {
							bottomBound = Math.min(bottomBound, (eWordsBelow[wb].bounds.topRow - wordAdjustmentSafetyMargin));
							noWordBelow = false;
						}
					}
					if (noWordBelow)
						lb++;
				}
				
				//	extend word within bounds
				do {
					expanded = false;
					boolean expand = false;
					
					//	check upward expansion
					for (int c = words[w].bounds.leftCol; (words[w].bounds.topRow > topBound) && (c < words[w].bounds.rightCol); c++) {
						if (brightness[c][words[w].bounds.topRow] == 127)
							continue;
						
						//	collect non-white part
						int nws = c;
						while ((c < words[w].bounds.rightCol) && (brightness[c][words[w].bounds.topRow] < 127))
							c++;
						int nwe = c;
						
						//	allow diagonal connection
						if (words[w].bounds.leftCol < nws)
							nws--;
						if (nwe < words[w].bounds.rightCol)
							nwe++;
						
						//	check if non-white part extends beyond bounds
						boolean gotExtensionPixel = false;
						for (int s = nws; s < nwe; s++)
							if (brightness[s][words[w].bounds.topRow-1] < 127) {
								gotExtensionPixel = true;
								break;
							}
						
						//	got extension, expand
						if (gotExtensionPixel)
							expand = true;
					}
					if (expand) {
						words[w].bounds.topRow--;
						expanded = true;
						continue;
					}
					
					//	check downward expansion
					for (int c = words[w].bounds.leftCol; (words[w].bounds.bottomRow < bottomBound) && (c < words[w].bounds.rightCol); c++) {
						if (brightness[c][words[w].bounds.bottomRow-1] == 127)
							continue;
						
						//	collect non-white part
						int nws = c;
						while ((c < words[w].bounds.rightCol) && (brightness[c][words[w].bounds.bottomRow-1] < 127))
							c++;
						int nwe = c;
						
						//	allow diagonal connection
						if (words[w].bounds.leftCol < nws)
							nws--;
						if (nwe < words[w].bounds.rightCol)
							nwe++;
						
						//	check if non-white part extends beyond bounds
						boolean gotExtendsionPixel = false;
						for (int s = nws; s < nwe; s++)
							if (brightness[s][words[w].bounds.bottomRow] < 127) {
								gotExtendsionPixel = true;
								break;
							}
						
						//	got extension, expand
						if (gotExtendsionPixel)
							expand = true;
					}
					if (expand) {
						words[w].bounds.bottomRow++;
						expanded = true;
						continue;
					}
					
					//	check leftward expansion
					for (int r = words[w].bounds.topRow; (words[w].bounds.leftCol > leftBound) && (r < words[w].bounds.bottomRow); r++)
						if ((brightness[words[w].bounds.leftCol][r] < 127) && (brightness[words[w].bounds.leftCol-1][r] < 127)) {
							expand = true;
							break;
						}
					if (expand) {
						words[w].bounds.leftCol--;
						expanded = true;
						continue;
					}
					
					//	check rightward expansion
					for (int r = words[w].bounds.topRow; (words[w].bounds.rightCol < rightBound) && (r < words[w].bounds.bottomRow); r++)
						if ((brightness[words[w].bounds.rightCol-1][r] < 127) && (brightness[words[w].bounds.rightCol][r] < 127)) {
							expand = true;
							break;
						}
					if (expand) {
						words[w].bounds.rightCol++;
						expanded = true;
						continue;
					}
				} while (expanded);
			}
			
			//	compute upper and lower line bounds
			for (int w = 0; w < words.length; w++)
				wordTopRows[l] = Math.min(wordTopRows[l], words[w].bounds.topRow);
			for (int w = 0; w < words.length; w++)
				wordBottomRows[l] = Math.max(wordBottomRows[l], words[w].bounds.bottomRow);
			
			//	expand words topward up to line height to capture severed dots of i's, etc.
			for (int w = 0; (l != 0) && (w < words.length); w++) {
				int leftBound = ((w == 0) ? block.bounds.leftCol : (words[w-1].bounds.rightCol + wordAdjustmentSafetyMargin));
				int rightBound = (((w+1) == words.length) ? block.bounds.rightCol : (words[w+1].bounds.leftCol - wordAdjustmentSafetyMargin));
				int topBound = wordTopRows[l];
				for (int wa = 0; (wordsAbove != null) && (wa < wordsAbove.length); wa++) {
					if ((leftBound < wordsAbove[wa].bounds.rightCol) && (wordsAbove[wa].bounds.leftCol < rightBound))
						topBound = Math.max(topBound, (wordsAbove[wa].bounds.bottomRow + wordAdjustmentSafetyMargin));
				}
				if (words[w].bounds.topRow < topBound)
					continue;
				ImagePartRectangle wordBox = new ImagePartRectangle(words[w].bounds.analysisImage);
				Imaging.copyBounds(words[w].bounds, wordBox);
				wordBox.topRow = topBound;
				wordBox = Imaging.narrowTopAndBottom(wordBox);
				Imaging.copyBounds(wordBox, words[w].bounds);
			}
		}
		
		//	expand lines upward and downward, but only so far as they do intersect with adjacent line words
		for (int l = 0; l < lines.length; l++) {
			int wordBottomAbove = -1;
			for (int la = (l-1); la > -1; la--)
				if (Imaging.isAboveOneAnother(lines[la].bounds, lines[l].bounds)) {
					wordBottomAbove = wordBottomRows[la];
					break;
				}
			int wordTopBelow = -1;
			for (int lb = (l+1); lb < lines.length; lb++)
				if (Imaging.isAboveOneAnother(lines[l].bounds, lines[lb].bounds)) {
					wordTopBelow = wordTopRows[lb];
					break;
				}
			if (wordBottomAbove == -1)
				lines[l].bounds.topRow = wordTopRows[l];
			else lines[l].bounds.topRow = Math.max(wordTopRows[l], wordBottomAbove);
			if (wordTopBelow == -1)
				lines[l].bounds.bottomRow = wordBottomRows[l];
			else lines[l].bounds.bottomRow = Math.min(wordBottomRows[l], wordTopBelow);
		}
	}
	
/* DERIVATION OF FONT SIZE FORMULAS:

Times New Roman at 96 DPI (100%):
24 pt: line height 37, cap height 21, non-cap height 14, descent 7
18 pt: line height 28, cap height 16, non-cap height 11, descent 5
12 pt: line height 19, cap height 11, non-cap height  7, descent 3
10 pt: line height 16, cap height  9, non-cap height  6, descent 3
 8 pt: line height 13, cap height  8, non-cap height  5, descent 2
 6 pt: line height 10, cap height  6, non-cap height  4, descent 2


Times New Roman at 400 DPI (417%):
24 pt: line height 153, cap height 90, non-cap height 62, descent 26
18 pt: line height 115, cap height 67, non-cap height 46, descent 20
12 pt: line height  77, cap height 45, non-cap height 32, descent 13
10 pt: line height  64, cap height 37, non-cap height 25, descent 11
 8 pt: line height  52, cap height 30, non-cap height 20, descent  8
 6 pt: line height  38, cap height 22, non-cap height 15, descent  7

==> line height: pt = line height / dpi * 63 (unsafe, as line spacing might differ)
==> cap height: pt = cap height / dpi * 107
==> non-cap height: pt = non-cap height / dpi * 155


DERIVATION OF FONT WEIGHT FORMULAS:

Times New Roman at 400 DPI (417%):
24 pt: bold width 18-21, plain width 11-12
18 pt: bold width 14-15, plain width 8
12 pt: bold width 10-11, plain width 6
10 pt: bold width 8-9, plain width 5
 8 pt: bold width 6-7, plain width 4

==> looks like (2 / 3) * (dpi/400) * font size might be a good threshold at 400 DPI, but then, this might depend on the font face
==> (2 * dpi * font size) / (3 * 400) --> seems a bit too small
==> (3 * dpi * font size) / (4 * 400) --> still a bit too small
==> (4 * dpi * font size) / (5 * 400) --> still too small
==> (5 * dpi * font size) / (6 * 400) --> still too unreliable
 	 */
	
	private static final double italicShearDeg = 15;
	
	/**
	 * Compute font metrics in a text block.
	 * @param block the block to analyze
	 * @param dpi the resolution of the backing image
	 */
	public static void computeFontMetrics(Block block, int dpi) {
		computeFontMetrics(block, dpi, true);
	}
	
	/**
	 * Compute font metrics in a text block. If analyzeFontStyle is set to
	 * false, this method does not analyze if a word is in bold face or in
	 * italics.
	 * @param block the block to analyze
	 * @param dpi the resolution of the backing image
	 * @param analyzeFontStyle analyze font style as well?
	 */
	public static void computeFontMetrics(Block block, int dpi, boolean analyzeFontStyle) {
		Line[] lines = block.getLines();
		if (lines.length == 0)
			return;
		ArrayList blockWords = new ArrayList();
		for (int l = 0; l < lines.length; l++) {
			Word[] words = lines[l].getWords();
			if (words.length == 0)
				continue;
			
			blockWords.addAll(Arrays.asList(words));
			
			//	collect word and character bounds
			ArrayList lineChars = new ArrayList();
			CountingSet charHeights = new CountingSet();
			int maxCharHeight = 0;
			CountingSet ascentHeights = new CountingSet();
			int maxAscent = 0;
			CountingSet descentHeights = new CountingSet();
			int maxDescent = 0;
			ImagePartRectangle[][] lineWordChars = new ImagePartRectangle[words.length][];
			for (int w = 0; w < words.length; w++) {
				ImagePartRectangle[] wordChars = Imaging.splitIntoColumns(words[w].bounds, 1);
				for (int c = 0; c < wordChars.length; c++) {
					wordChars[c] = Imaging.narrowTopAndBottom(wordChars[c]);
					if (wordChars[c].isEmpty())
						continue;
					lineChars.add(wordChars[c]);
					charHeights.add(wordChars[c].bottomRow - wordChars[c].topRow);
					maxCharHeight = Math.max(maxCharHeight, (wordChars[c].bottomRow - wordChars[c].topRow));
					ascentHeights.add(words[w].baseline - wordChars[c].topRow);
					maxAscent = Math.max(maxAscent, (words[w].baseline - wordChars[c].topRow));
					descentHeights.add(wordChars[c].bottomRow - words[w].baseline);
					maxDescent = Math.max(maxDescent, (wordChars[c].bottomRow - words[w].baseline));
				}
				lineWordChars[w] = wordChars;
			}
//			System.out.println("Line " + lines[l].bounds.getId() + ": got " + lineChars.size() + " characters from " + words.length + " words, max height is " + maxCharHeight + ", max ascent " + maxAscent + ", max descent " + maxDescent);
//			System.out.println(" - char heights:");
//			for (CountingSet.IntIterator iit = charHeights.iterator(); iit.hasNext();) {
//				int ch = iit.next();
//				System.out.println("   - " + ch + ": " + charHeights.getCount(ch));
//			}
//			System.out.println(" - ascent heights:");
//			for (CountingSet.IntIterator iit = ascentHeights.iterator(); iit.hasNext();) {
//				int ah = iit.next();
//				System.out.println("   - " + ah + ": " + ascentHeights.getCount(ah));
//			}
//			System.out.println(" - descent heights:");
//			for (CountingSet.IntIterator iit = descentHeights.iterator(); iit.hasNext();) {
//				int dh = iit.next();
//				System.out.println("   - " + dh + ": " + descentHeights.getCount(dh));
//			}
			
			//	alleviate baseline underruns
			int descentCut = (dpi / 100); // descents of less than a third of a millimeter are probably not actual descents
			int baselineUnderrunSum = 0;
			int baselineUnderrunCount = 0;
			for (CountingSet.IntIterator iit = descentHeights.iterator(); iit.hasNext();) {
				int dh = iit.next();
				if ((dh < 0) || (descentCut < dh))
					continue;
				int dhc = descentHeights.getCount(dh);
				baselineUnderrunSum += (dh * dhc);
				baselineUnderrunCount += dhc;
			}
			int avgBaselineUnderrun = ((baselineUnderrunCount == 0) ? 0 : (baselineUnderrunSum / baselineUnderrunCount));
//			System.out.println(" --> average baseline underrun is " + avgBaselineUnderrun);
			
			//	compute font metrics
			int capNonCapCut = ((maxAscent * 8) / 10); // in most fonts, lower case letters without ascent will not exceed 80% of cap height
			int nonCapPunctuationCut = ((maxAscent * 4) / 10); // in most fonts, lower case letters will not be lower than 40% of cap height
			int capAscentSum = 0;
			int capAscentCount = 0;
			int nonCapAscentSum = 0;
			int nonCapAscentCount = 0;
			for (CountingSet.IntIterator iit = ascentHeights.iterator(); iit.hasNext();) {
				int ah = iit.next();
				if ((ah + avgBaselineUnderrun) <= nonCapPunctuationCut)
					continue;
				int ahc = ascentHeights.getCount(ah);
				if ((ah + avgBaselineUnderrun) <= capNonCapCut) {
					nonCapAscentSum += ((ah + avgBaselineUnderrun) * ahc);
					nonCapAscentCount += ahc;
				}
				else {
					capAscentSum += ((ah + avgBaselineUnderrun) * ahc);
					capAscentCount += ahc;
				}
			}
			
			int avgCapHeight = ((capAscentCount == 0) ? -1 : (capAscentSum / capAscentCount));
			int avgNonCapHeight = ((nonCapAscentCount == 0) ? -1 : (nonCapAscentSum / nonCapAscentCount));
//			System.out.println(" --> average cap height is " + avgCapHeight + ", average x-height is " + avgNonCapHeight);
			int capFontSize = ((avgCapHeight == -1) ? -1 : (((avgCapHeight * 107) + (dpi / 2)) / dpi));
			int nonCapFontSize = ((avgNonCapHeight == -1) ? -1 : (((avgNonCapHeight * 155) + (dpi / 2)) / dpi));
//			System.out.println(" --> cap font size is " + capFontSize + ", non-cap font size is " + nonCapFontSize);
			if ((capFontSize != -1) && (nonCapFontSize != -1))
				lines[l].fontSize = (((avgCapHeight * 107) + (avgNonCapHeight * 155) + dpi) / (2 * dpi));
			else if (capFontSize != -1)
				lines[l].fontSize = capFontSize;
			else if (nonCapFontSize != -1)
				lines[l].fontSize = nonCapFontSize;
			System.out.println(" --> font size is " + lines[l].fontSize);
			
			//	cannot do font style detection without font size
			if (lines[l].fontSize < 1)
				continue;
			
			//	skip font style detection if ordered so
			if (!analyzeFontStyle)
				continue;
			
			//	detect italics and bold face
			int boldBlockSize = (((dpi * lines[l].fontSize) + (500 / 2)) / 500);
			int analysisHeight = ((avgNonCapHeight < 1) ? avgCapHeight : avgNonCapHeight);
			Word italicCandidate = null;
			for (int w = 0; w < words.length; w++) {
				
				//	detect italics by detecting both upright and forward-leaning lines of lenght avgNonCapHeight and then comparing
				int nonItalicStrokes = countStrokes(words[w].bounds, (analysisHeight / 2), 0, true);
				int italicStrokes = countStrokes(words[w].bounds, (analysisHeight / 2), italicShearDeg, true);
				int nonItalicGaps = countStrokes(words[w].bounds, Math.min(analysisHeight, (words[w].bounds.bottomRow - words[w].bounds.topRow)), 0, false);
				int italicGaps = countStrokes(words[w].bounds, Math.min(analysisHeight, (words[w].bounds.bottomRow - words[w].bounds.topRow)), italicShearDeg, false);
//				System.out.println(" - found " + nonItalicStrokes + " non-italic and " + italicStrokes + " italic strokes" + ", " + nonItalicGaps + " non-italic and " + italicGaps + " italic gaps");
				if ((italicStrokes + (italicGaps / 2)) > (nonItalicStrokes + (nonItalicGaps / 2))) {
					words[w].italics = true;
//					System.out.println(" ==> ITALICS");
					if ((italicCandidate != null) && (((italicCandidate.bounds.rightCol - italicCandidate.bounds.leftCol) * 3) < (words[w].bounds.rightCol - words[w].bounds.leftCol))) {
						italicCandidate.italics = true;
//						System.out.println(" ==> ITALICS CANDIDATE PROMOTED");
					}
					italicCandidate = null;
				}
				else if ((italicStrokes > nonItalicStrokes) || (italicGaps > nonItalicGaps)) {
					italicCandidate = words[w];
//					System.out.println(" ==> ITALICS CANDIDATE");
				}
				else italicCandidate = null;
				
				//	detect bold face
				ImagePartRectangle wordMiddlePart = words[w].bounds.getSubRectangle(words[w].bounds.leftCol, words[w].bounds.rightCol, (words[w].getBaseline() - analysisHeight), words[w].getBaseline());
				wordMiddlePart = Imaging.narrowLeftAndRight(wordMiddlePart);
				wordMiddlePart = Imaging.narrowTopAndBottom(wordMiddlePart);
				if (wordMiddlePart.isEmpty())
					continue;
//				double wordBrightness = Imaging.computeAverageBrightness(wordMiddlePart);
				int boldCharWidthSum = 0;
				int checkCharWidthSum = 0;
				int boldBlockCount = 0;
				for (int c = 0; c < lineWordChars[w].length; c++) {
					if ((words[w].baseline - lineWordChars[w][c].topRow) < nonCapPunctuationCut)
						continue;
					int charBoldBlocks = countBoldBlocks(lineWordChars[w][c], boldBlockSize, (words[w].italics ? italicShearDeg : 0));
					boldBlockCount += charBoldBlocks;
					if (charBoldBlocks != 0)
						boldCharWidthSum += (lineWordChars[w][c].rightCol - lineWordChars[w][c].leftCol);
					checkCharWidthSum += (lineWordChars[w][c].rightCol - lineWordChars[w][c].leftCol);
				}
				//	TODOne make this reliable !!!
				//	==> this is probably as reliable as it gets without becoming a real lot more sophisticated
//				System.out.println(" - brightness of word " + w + " is " + wordBrightness + " from " + lineWordChars[w].length + " chars, found " + boldBlockCount + " bold blocks of size " + boldBlockSize + ", " + boldCharWidthSum + " of " + checkCharWidthSum + " in bold");
				if ((checkCharWidthSum != 0) && ((boldCharWidthSum * 2) > checkCharWidthSum) && ((boldBlockCount * 4) > checkCharWidthSum)) {
					words[w].bold = true;
//					System.out.println(" ==> BOLD");
				}
			}
			
			//	no use doing majority vote with less than two words
			if (words.length < 2)
				continue;
			
			//	check if line is majorly bold
			int wordWidthSum = 0;
			int boldWordWidthSum = 0;
			int boldWordCount = 0;
			for (int w = 0; w < words.length; w++) {
				wordWidthSum += (words[w].bounds.rightCol - words[w].bounds.leftCol);
				if (words[w].bold) {
					boldWordWidthSum += (words[w].bounds.rightCol - words[w].bounds.leftCol);
					boldWordCount++;
				}
			}
			
			//	no (weighted majority of) blod words, no need for further checks
			if ((boldWordCount == 0) || ((boldWordWidthSum * 2) < wordWidthSum))
				continue;
			
			//	make shorter-than-average words bold in lines that are majorly bold (helps with stop words in headings, etc)
			int avgWordWidth = ((wordWidthSum + (words.length / 2)) / words.length);
			for (int w = 0; w < words.length; w++) {
				if (words[w].bold || (avgWordWidth < (words[w].bounds.rightCol - words[w].bounds.leftCol)))
					continue;
				boolean boldLeft = ((w == 0) ? true : words[w-1].bold);
				boolean boldRight = (((w+1) == words.length) ? true : words[w+1].bold);
				words[w].bold = (boldLeft && boldRight);
			}
		}
		
		//	if word is less than twice as wide as high, keep bold and italics only if neighboring word bold or in italics as well (helps with numbers, etc.)
		if (analyzeFontStyle)
			for (int w = 0; w < blockWords.size(); w++) {
				Word word = ((Word) blockWords.get(w));
				if ((word.bounds.rightCol - word.bounds.leftCol) > ((word.bounds.bottomRow - word.bounds.topRow) * 2))
					continue;
				if (word.italics) {
					boolean italicsLeft = ((w == 0) ? false : ((Word) blockWords.get(w-1)).italics);
					boolean italicsRight = (((w+1) == blockWords.size()) ? false : ((Word) blockWords.get(w+1)).italics);
					word.italics = (italicsLeft || italicsRight);
				}
				if (word.bold) {
					boolean boldLeft = ((w == 0) ? false : ((Word) blockWords.get(w-1)).bold);
					boolean boldRight = (((w+1) == blockWords.size()) ? false : ((Word) blockWords.get(w+1)).bold);
					word.bold = (boldLeft || boldRight);
				}
			}
	}
	
	private static class CountingSet {
		private static class Counter {
			int c = 0;
		}
		private static class IntIterator {
			Iterator iter;
			IntIterator(Iterator iter) {
				this.iter = iter;
			}
			boolean hasNext() {
				return this.iter.hasNext();
			}
			int next() {
				return ((Integer) this.iter.next()).intValue();
			}
		}
		private TreeMap content = new TreeMap();
		void add(int i) {
			Integer key = new Integer(i);
			Counter cnt = ((Counter) this.content.get(key));
			if (cnt == null) {
				cnt = new Counter();
				this.content.put(key, cnt);
			}
			cnt.c++;
		}
		int getCount(int i) {
			Integer key = new Integer(i);
			Counter cnt = ((Counter) this.content.get(key));
			return ((cnt == null) ? 0 : cnt.c);
		}
		IntIterator iterator() {
			return new IntIterator(this.content.keySet().iterator());
		}
	}
	
	private static int countBoldBlocks(ImagePartRectangle wordChar, int size, double angle) {
		int hSize = size;
		int vSize = size;
		int blockShear = ((int) Math.round(((double) vSize) * Math.tan((Math.PI / 180) * angle)));
		if (blockShear != 0)
			hSize--;
		int count = 0;
		boolean found;
		byte[][] brightness = wordChar.analysisImage.getBrightness();
		for (int c = (wordChar.leftCol + blockShear); c < (wordChar.rightCol - hSize); c++) {
			for (int r = wordChar.topRow; r < (wordChar.bottomRow - vSize); r++) {
				found = true;
				for (int l = 0; found && (l < size); l++) {
					int s = ((((blockShear * l) + (vSize / 2)) / vSize));
					found = (found
							&&
							((l < hSize) ? (brightness[c+l][r] < 127) : true)
							&&
							(brightness[c+l-blockShear][r+vSize-1] < 127)
							&&
							(brightness[c-s][r+l] < 127)
							&&
							(brightness[c+hSize-1-s][r+l] < 127)
							);
				}
				if (found)
					count++;
			}
		}
		return count;
	}
	
	private static int countStrokes(ImagePartRectangle word, int length, double angle, boolean black) {
		int wordShear = ((int) Math.round(((double) length) * Math.tan((Math.PI / 180) * angle)));
		int count = 0;
		boolean found;
		byte[][] brightness = word.analysisImage.getBrightness();
		for (int c = word.leftCol; c < (word.rightCol - (wordShear / 2)); c++) {
			for (int r = (word.topRow + length - 1); r < word.bottomRow; r++) {
				found = true;
				for (int l = 0; found && (l < length); l++) {
					int lc = (c + (((wordShear * l) + (length / 2)) / length));
					boolean isWhite = ((lc < word.rightCol) ? (brightness[lc][r-l] == 127) : true);
					found = (found
							&&
//							(black ? !isWhite : isWhite)
							(black != isWhite)
							);
				}
				if (found)
					count++;
			}
		}
		return count;
	}
	
	/**
	 * Compute the baselines of the lines in a text block.
	 * @param block the block to analyze
	 * @param dpi the resolution of the backing image
	 */
	public static void computeLineBaselines(Block block, int dpi) {
		int minFullWordHeight = (dpi / 25); // use only words higher than one twentyfifthth of an inch (one millimeter) for baseline computation, then fill in baseline of words lower than that (dashes, etc.)
		Line[] lines = block.getLines();
		if (lines.length == 0)
			return;
		for (int l = 0; l < lines.length; l++) {
			Word[] words = lines[l].getWords();
			if (words.length == 0)
				continue;
			
			//	use only words higher than threshold for baseline computation
			ArrayList baselineComputationWordList = new ArrayList();
			for (int w = 0; w < words.length; w++) {
				if (minFullWordHeight < (words[w].bounds.bottomRow - words[w].bounds.topRow))
					baselineComputationWordList.add(words[w]);
			}
			Word[] baselineComputationWords;
			if ((baselineComputationWordList.size() != 0) && (baselineComputationWordList.size() < words.length))
				baselineComputationWords = ((Word[]) baselineComputationWordList.toArray(new Word[baselineComputationWordList.size()]));
			else baselineComputationWords = words;
			
			//	compute baseline for each word
			ImagePartRectangle[] wordBounds = new ImagePartRectangle[baselineComputationWords.length];
			for (int w = 0; w < baselineComputationWords.length; w++) {
				wordBounds[w] = baselineComputationWords[w].bounds;
				ImagePartRectangle[] nwrs = {baselineComputationWords[w].bounds};
				baselineComputationWords[w].baseline = findBaseline(block.bounds.analysisImage, nwrs);
			}
			
			//	detect line slope
			int descCount = 0;
			int evenCount = 0;
			int ascCount = 0;
			for (int w = 1; w < baselineComputationWords.length; w++) {
				if ((baselineComputationWords[w-1].baseline < 1) || (baselineComputationWords[w].baseline < 1))
					continue;
				if (baselineComputationWords[w-1].baseline < baselineComputationWords[w].baseline)
					descCount++;
				else if (baselineComputationWords[w-1].baseline == baselineComputationWords[w].baseline)
					evenCount++;
				else ascCount++;
			}
			boolean evenLine = true;
			if ((evenCount < ascCount) && ((baselineComputationWords.length * 8) <= ((ascCount + evenCount) * 10))) {
				System.out.println(" - got leftward slope");
				evenLine = (baselineComputationWords.length <= slopedBaselineSlidingWindowSize);
			}
			else if ((evenCount < descCount) && ((baselineComputationWords.length * 8) <= ((descCount + evenCount) * 10))) {
				System.out.println(" - got rightward slope");
				evenLine = (baselineComputationWords.length <= slopedBaselineSlidingWindowSize);
			}
			
			//	even line, smoothen at once
			if (evenLine) {
				
				//	compute average word baseline
				int lineBaselineSum = 0;
				int lineBaselineWordCount = 0;
				for (int w = 0; w < baselineComputationWords.length; w++) {
					if (baselineComputationWords[w].baseline < 1)
						continue;
					lineBaselineWordCount++;
					lineBaselineSum += baselineComputationWords[w].baseline;
				}
				int lineBaseline = ((lineBaselineWordCount == 0) ? findBaseline(block.bounds.analysisImage, wordBounds) : ((lineBaselineSum + (lineBaselineWordCount / 2)) / lineBaselineWordCount));
				System.out.println(" - mean line baseline is " + lineBaseline + " based on " + lineBaselineWordCount + " words");
				
				//	smoothen out word baselines
				for (int w = 1; w < (baselineComputationWords.length - 1); w++) {
					int lDist = Math.abs(lineBaseline - baselineComputationWords[w-1].baseline);
					int oDist = Math.abs(lineBaseline - baselineComputationWords[w].baseline);
					int rDist = Math.abs(lineBaseline - baselineComputationWords[w+1].baseline);
					if ((oDist > lDist) && (oDist > rDist))
						baselineComputationWords[w].baseline = ((baselineComputationWords[w-1].baseline + baselineComputationWords[w+1].baseline) / 2);
				}
			}
			
			//	sloped line, use sliding window for smoothing
			else for (int ws = 0; ws <= (baselineComputationWords.length - slopedBaselineSlidingWindowSize); ws++) {
				
				//	compute average word baseline
				int windowBaselineSum = 0;
				int windowBaselineWordCount = 0;
				for (int w = ws; w < (ws + slopedBaselineSlidingWindowSize); w++) {
					if (baselineComputationWords[w].baseline < 1)
						continue;
					windowBaselineWordCount++;
					windowBaselineSum += baselineComputationWords[w].baseline;
				}
				int windowBaseline = ((windowBaselineWordCount == 0) ? findBaseline(block.bounds.analysisImage, wordBounds) : ((windowBaselineSum + (windowBaselineWordCount / 2)) / windowBaselineWordCount));
				System.out.println(" - mean window baseline is " + windowBaseline + " based on " + windowBaselineWordCount + " words");
				
				//	smoothen out word baselines
				for (int w = (ws+1); w < (ws + slopedBaselineSlidingWindowSize - 1); w++) {
					int lDist = Math.abs(windowBaseline - baselineComputationWords[w-1].baseline);
					int oDist = Math.abs(windowBaseline - baselineComputationWords[w].baseline);
					int rDist = Math.abs(windowBaseline - baselineComputationWords[w+1].baseline);
					if ((oDist > lDist) && (oDist > rDist))
						baselineComputationWords[w].baseline = ((baselineComputationWords[w-1].baseline + baselineComputationWords[w+1].baseline) / 2);
				}
			}
			
			//	end word smoothing is too unreliable if sample is too small
			if (4 < baselineComputationWords.length) {
				
				//	compute average baseline distance
				int wordBaselineDistanceSum = 0;
				for (int w = 1; w < baselineComputationWords.length; w++)
					wordBaselineDistanceSum += (baselineComputationWords[w].baseline - baselineComputationWords[w-1].baseline);
				int avgWordBaselineDistance = ((wordBaselineDistanceSum + ((baselineComputationWords.length - 1) / 2)) / (baselineComputationWords.length - 1));
				
				//	smoothen leftmost and rightmost word based on that
				if (Math.abs(avgWordBaselineDistance) < Math.abs(baselineComputationWords[1].baseline - baselineComputationWords[0].baseline))
					baselineComputationWords[0].baseline = (baselineComputationWords[1].baseline - avgWordBaselineDistance);
				if (Math.abs(avgWordBaselineDistance) < Math.abs(baselineComputationWords[baselineComputationWords.length-1].baseline - baselineComputationWords[baselineComputationWords.length-2].baseline))
					baselineComputationWords[baselineComputationWords.length-1].baseline = (baselineComputationWords[baselineComputationWords.length-2].baseline + avgWordBaselineDistance);
			}
			
			//	fill in baseline of words lower than one twentyfifthth of an inch (dashes, etc.)
			for (int w = 0; w < words.length; w++) {
				if (0 < words[w].baseline)
					continue;
				int blLeft = -1;
				for (int lw = (w-1); lw > -1; lw--)
					if (0 < words[lw].baseline) {
						blLeft = words[lw].baseline;
						break;
					}
				int blRight = -1;
				for (int rw = (w+1); rw < words.length; rw++)
					if (0 < words[rw].baseline) {
						blRight = words[rw].baseline;
						break;
					}
				if ((blLeft != -1) && (blRight != -1))
					words[w].baseline = ((blLeft + blRight + 1) / 2);
				else if (blLeft != -1)
					words[w].baseline = blLeft;
				else if (blRight != -1)
					words[w].baseline = blRight;
			}
		}
	}
	private static final int slopedBaselineSlidingWindowSize = 5;
	
	static void checkWordDescents(Block block, int dpi) {
		int minSignificantDifference = (dpi / 60); // somewhat less than half a millimeter
		Line[] lines = block.getLines();
		if (lines.length == 0)
			return;
		
		int[] wordTopRows = new int[lines.length];
		int[] wordBottomRows = new int[lines.length];
		boolean wordChanged = false;
		for (int l = 0; l < lines.length; l++) {
			wordTopRows[l] = lines[l].bounds.bottomRow;
			wordBottomRows[l] = lines[l].bounds.topRow;
			Word[] words = lines[l].getWords();
			if (words.length == 0)
				continue;
			int lineBaseline = lines[l].getBaseline();
			
			//	expand words in all directions to cover letters fully, as sloped line cuts might have severed letters
			for (int w = 0; w < words.length; w++) {
				if ((words[w].bounds.bottomRow - lineBaseline) < minSignificantDifference)
					continue;
				ImagePartRectangle[] wbps = Imaging.splitIntoRows(words[w].bounds, 1);
				if (wbps.length < 2)
					continue;
				for (int p = 0; p < wbps.length; p++) {
					if (lineBaseline < wbps[p].topRow)
						break;
					words[w].bounds.bottomRow = wbps[p].bottomRow;
					wordChanged = true;
				}
			}
			
			//	compute upper and lower line bounds
			for (int w = 0; w < words.length; w++)
				wordTopRows[l] = Math.min(wordTopRows[l], words[w].bounds.topRow);
			for (int w = 0; w < words.length; w++)
				wordBottomRows[l] = Math.max(wordBottomRows[l], words[w].bounds.bottomRow);
		}
		
		//	nothing changed, we're all set
		if (!wordChanged)
			return;
		
		//	expand lines upward and downward, but only so far as they do intersect with adjacent line words
		for (int l = 0; l < lines.length; l++) {
			int wordBottomAbove = -1;
			for (int la = (l-1); la > -1; la--)
				if (Imaging.isAboveOneAnother(lines[la].bounds, lines[l].bounds)) {
					wordBottomAbove = wordBottomRows[la];
					break;
				}
			int wordTopBelow = -1;
			for (int lb = (l+1); lb < lines.length; lb++)
				if (Imaging.isAboveOneAnother(lines[l].bounds, lines[lb].bounds)) {
					wordTopBelow = wordTopRows[lb];
					break;
				}
			if (wordBottomAbove == -1)
				lines[l].bounds.topRow = wordTopRows[l];
			else lines[l].bounds.topRow = Math.max(wordTopRows[l], wordBottomAbove);
			if (wordTopBelow == -1)
				lines[l].bounds.bottomRow = wordBottomRows[l];
			else lines[l].bounds.bottomRow = Math.min(wordBottomRows[l], wordTopBelow);
		}
	}
	
	private static int findBaseline(AnalysisImage ai, ImagePartRectangle[] words) {
		if (words.length == 0)
			return -1;
		
		int left = words[0].leftCol;
		int right = words[words.length-1].rightCol;
		int top = Integer.MAX_VALUE;
		int bottom = 0;
		for (int w = 0; w < words.length; w++) {
			top = Math.min(top, words[w].topRow);
			bottom = Math.max(bottom, words[w].bottomRow);
		}
		
		int height = (bottom - top);
		byte[][] brightness = ai.getBrightness();
		byte[] rowBrightnesses = new byte[height];
		for (int r = top; (r < bottom) && (r < brightness[0].length); r++) {
			int brightnessSum = 0;
			for (int c = left; (c < right) && (c < brightness.length); c++)
				brightnessSum += brightness[c][r];
			rowBrightnesses[r - top] = ((byte) (brightnessSum / (right - left)));
		}
		
		byte[] rowBrightnessDrops = new byte[height];
		for (int r = 0; r < height; r++)
			rowBrightnessDrops[r] = ((byte) (rowBrightnesses[r] - (((r+1) == height) ? 127 : rowBrightnesses[r+1])));
		byte maxRowBrightnessDrop = 0;
		int maxDropRow = -1;
		for (int r = (height-1); r > (height / 2); r--)
			if (rowBrightnessDrops[r] < maxRowBrightnessDrop) {
				maxRowBrightnessDrop = rowBrightnessDrops[r];
				maxDropRow = r;
			}
		
		return (top + maxDropRow);
	}
}

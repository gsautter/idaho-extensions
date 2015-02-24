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
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * Function library for image processing.
 * 
 * This class uses the FFT code and complex number representation from
 * http://introcs.cs.princeton.edu/java/97data/. These code artifacts are copied
 * over here only for the lack of a respective JAR library.
 * 
 * @author sautter
 */
public class Imaging {
	
	private static final int analysisImageCacheSize = 128;
	private static Map analysisImageCache = Collections.synchronizedMap(new LinkedHashMap(128, 0.9f, true) {
		protected boolean removeEldestEntry(Entry eldest) {
			return (this.size() > analysisImageCacheSize);
		}
	});
	
	/**
	 * Wrap an image for analysis. If the argument cache key is null, caching is
	 * deactivated.
	 * @param image the image to wrap
	 * @param cacheKey a string ID to use for caching
	 * @return the wrapped image
	 */
	public static AnalysisImage wrapImage(BufferedImage image, String cacheKey) {
		synchronized (analysisImageCache) {
			AnalysisImage analysisImage = ((cacheKey == null) ? null : ((AnalysisImage) analysisImageCache.get(cacheKey)));
			if (analysisImage == null) {
				analysisImage = new AnalysisImage(image);
				if (cacheKey != null)
					analysisImageCache.put(cacheKey, analysisImage);
			}
			return analysisImage;
		}
	}
	
	/**
	 * Clean up wrapped image cache, removing all wrapped images whose cache
	 * keys start with the argument prefix.
	 * @param cacheKeyPrefix the prefix of the cache keys to invalidate
	 */
	public static void cleanUpCache(String cacheKeyPrefix) {
		synchronized (analysisImageCache) {
			for (Iterator ckit = analysisImageCache.keySet().iterator(); ckit.hasNext();) {
				String cacheKey = ((String) ckit.next());
				if (cacheKey.startsWith(cacheKeyPrefix))
					ckit.remove();
			}
		}
	}
	
	/**
	 * Wrapper for an image and computed bounds while processing.
	 * 
	 * @author sautter
	 */
	public static class AnalysisImage {
		private BufferedImage image;
		private byte[][] brightness;
		AnalysisImage(BufferedImage image) {
			this.image = image;
		}
		AnalysisImage(BufferedImage image, byte[][] brightnesses) {
			this.image = image;
			this.brightness = brightnesses;
		}
		public BufferedImage getImage() {
			return this.image;
		}
		void setImage(BufferedImage image) {
			this.image = image;
			this.brightness = null;
			this.fftCache.clear();
		}
		
		/**
		 * Retrieve a two-dimensional array holding the brightness values of the
		 * wrapped image, discretized to values between 0-127, inclusive. The
		 * outer dimension is the columns, the inner dimension the rows. The
		 * array must not be modified.
		 * @return an array holding the brightness values of the wrapped image
		 */
		public byte[][] getBrightness() {
			if (this.brightness == null) {
				this.brightness = new byte[this.image.getWidth()][this.image.getHeight()];
				float[] hsb = null;
				int rgb;
				int b;
				for (int c = 0; c < this.image.getWidth(); c++) {
					for (int r = 0; r < this.image.getHeight(); r++) {
						rgb = this.image.getRGB(c, r);
						hsb = new Color(rgb).getColorComponents(hsb);
						b = ((int) (hsb[2] * 128));
						this.brightness[c][r] = ((b == 128) ? 127 : ((byte) b));
					}
				}
			}
			return this.brightness;
		}
		
		/**
		 * Retrieve the FFT of the wrapped image. Having the image repeated
		 * computes the FFT of a plain parquetted with the argument image
		 * instead.
		 * @param tdim the size of the FFT
		 * @param repeatImage repeat the image?
		 * @return the FFT of the wrapped image, sized tdim x tdim
		 */
		public Complex[][] getFft(int tdim, boolean repeatImage) {
			String fftKey = ("" + tdim + "-" + repeatImage);
			Complex[][] fft = ((Complex[][]) this.fftCache.get(fftKey));
			if (fft == null) {
				fft = Imaging.getFft(image, tdim, tdim, repeatImage);
				this.fftCache.put(fftKey, fft);
			}
			return fft;
		}
		/**
		 * Retrieve the FFT of the wrapped image. Having the image repeated
		 * computes the FFT of a plain parquetted with the argument image
		 * instead.
		 * @param repeatImage repeat the image?
		 * @return the FFT of the wrapped image
		 */
		public Complex[][] getFft(boolean repeatImage) {
			int dim = 64;
			int size = Math.min(256, Math.max(this.image.getWidth(), this.image.getHeight()));
			while (dim < size) dim *= 2;
			return this.getFft(dim, repeatImage);
		}
		/**
		 * @return the FFT of the wrapped image
		 */
		public Complex[][] getFft() {
			int dim = 64;
			int size = Math.min(256, Math.max(this.image.getWidth(), this.image.getHeight()));
			while (dim < size) dim *= 2;
			return this.getFft(dim, (this.image.getWidth() < this.image.getHeight()));
		}
		private HashMap fftCache = new HashMap(2);
	}
	
	/**
	 * View-based representation of a rectangular sub image of an AnalysisImage.
	 * 
	 * @author sautter
	 */
	public static class ImagePartRectangle {
		final AnalysisImage analysisImage;
		int topRow; // inclusive
		int bottomRow; // exclusive
		int leftCol; // inclusive
		int rightCol; // exclusive
		boolean splitClean = false;
		ImagePartRectangle(AnalysisImage analysisImage) {
			this.analysisImage = analysisImage;
			this.leftCol = 0;
			this.rightCol = this.analysisImage.image.getWidth();
			this.topRow = 0;
			this.bottomRow = this.analysisImage.image.getHeight();
		}
		/**
		 * @return the analysisImage
		 */
		public AnalysisImage getImage() {
			return this.analysisImage;
		}
		/**
		 * @return the topRow
		 */
		public int getTopRow() {
			return this.topRow;
		}
		/**
		 * @return the bottomRow
		 */
		public int getBottomRow() {
			return this.bottomRow;
		}
		/**
		 * @return the leftCol
		 */
		public int getLeftCol() {
			return this.leftCol;
		}
		/**
		 * @return the rightCol
		 */
		public int getRightCol() {
			return this.rightCol;
		}
		public int getWidth() {
			return (this.rightCol - this.leftCol);
		}
		public int getHeight() {
			return (this.bottomRow - this.topRow);
		}
		public boolean isEmpty() {
			return ((this.getWidth() * this.getHeight()) == 0);
		}
		/**
		 * Create a separate image from the content of this rectangle.
		 * @return the content of the rectangle as a separate image
		 */
		public AnalysisImage toImage() {
			if ((this.leftCol == 0) && (this.topRow == 0) && (this.rightCol == this.analysisImage.image.getWidth()) && (this.bottomRow == this.analysisImage.image.getHeight()))
				return this.analysisImage;
			return new AnalysisImage(this.analysisImage.image.getSubimage(this.leftCol, this.topRow, (this.rightCol - this.leftCol), (this.bottomRow - this.topRow)));
		}
		/**
		 * Create a sub rectangle of this one, referring to the same image.
		 * @param l the left bound of the sub rectangle
		 * @param r the right bound of the sub rectangle
		 * @param t the top bound of the sub rectangle
		 * @param b the bottom bound of the sub rectangle
		 * @return a sub rectangle with the specified boundaries
		 */
		public ImagePartRectangle getSubRectangle(int l, int r, int t, int b) {
			ImagePartRectangle ipr = new ImagePartRectangle(this.analysisImage);
			ipr.leftCol = ((l < 0) ? 0 : l);
			ipr.rightCol = r;
			ipr.topRow = ((t < 0) ? 0 : t);
			ipr.bottomRow = b;
			return ipr;
		}
		public boolean equals(Object obj) {
			if (obj instanceof ImagePartRectangle) {
				ImagePartRectangle ipr = ((ImagePartRectangle) obj);
				if (this.leftCol != ipr.leftCol)
					return false;
				else if (this.rightCol != ipr.rightCol)
					return false;
				else if (this.topRow != ipr.topRow)
					return false;
				else if (this.bottomRow != ipr.bottomRow)
					return false;
				else return true;
			}
			else return false;
		}
		public String getId() {
			return ("[" + this.leftCol + "," + this.rightCol + "," + this.topRow + "," + this.bottomRow + "]");
		}
	}
	
	/**
	 * Correct a page image. This method aggregates several lower level
	 * corrections for convenience, namely inversion check, white balance,
	 * rotation correction, and cutting off white margins.
	 * @param ai the image to correct
	 * @param dpi the resolution of the image
	 * @param psm a monitor object observing progress
	 * @return the corrected image
	 */
	public static AnalysisImage correctImage(AnalysisImage ai, int dpi, ProgressMonitor psm) {
		if (psm == null)
			psm = ProgressMonitor.dummy;
		
		//	prepare image for enhancement
		boolean changed;
		
		//	check for white on black
		changed = false;
		changed = correctWhiteOnBlack(ai, ((byte) 64));
		if (changed)
			psm.setInfo("   - white-on-black inverted");
		
		//	check binary vs. gray scale or color
		boolean isGrayScale = isGrayScale(ai);
		boolean isSharp = true;
		
		//	do the fuzzy stuff only to gray scale images
		if (isGrayScale) {
			
			//	measure blurrieness
			int contrast = measureContrast(ai);
			if (contrast < 16)
				isSharp = false;
			
			//	do background elimination only to blurry images, so not to destroy non-blurry images
			if (!isSharp) {
				
				//	smooth out unevenly printed letters
				changed = false;
				changed = gaussBlur(ai, 1);
				if (changed)
					psm.setInfo("   - letters smoothed");
				
				//	apply low pass filter
				/* TODO figure out if this makes sense here, as images might suffer,
				 * maybe better in OCR engine, applied to individual text images, but
				 * then, this also hampers block identification */
				changed = false;
//				changed = eliminateBackground(ai, (dpi / 4), 3, 12);
				changed = eliminateBackground(ai, dpi);
				if (changed)
					psm.setInfo("   - background elimination done");
			}
			
			//	whiten white
			changed = false;
			changed = whitenWhite(ai);
			if (changed)
				psm.setInfo("   - white balance done");
		}
		
		//	do feather dusting to get rid of spots in the middle of nowhere
		changed = false;
		changed = featherDust(ai, dpi, !isGrayScale, isSharp);
		if (changed)
			psm.setInfo("   - feather dusting done");
		
//		//	do fine grained feather dusting to get rid of smaller spots
//		if (dpi >= 200) {
//			changed = false;
//			changed = featherDust(ai, 1, (dpi/100), (dpi/25), dpi, !isGrayScale);
//			if (changed)
//				psm.setInfo("   - fine grained feather dusting done");
//		}
//		
//		//	do coarse feather dusting to get rid of spots in the middle of nowhere
//		changed = false;
//		changed = featherDust(ai, (dpi/25), (dpi/25), (dpi/25), dpi, !isGrayScale);
//		if (changed)
//			psm.setInfo("   - coarse feather dusting done");
		
		//	correct page rotation
		changed = false;
		changed = correctPageRotation(ai, 0.1, ADJUST_MODE_SQUARE_ROOT);
		if (changed)
			psm.setInfo("   - page rotation corrected");
		
		//	cut white margins
		ImagePartRectangle textBounds = getContentBox(ai);
		ai = textBounds.toImage();
		psm.setInfo("   - white margins removed, size is " + ai.getImage().getWidth() + " x " + ai.getImage().getHeight());
		
		//	we're done here
		return ai;
	}
	
	/**
	 * Determine whether an image is grayscale or plain black and white. More
	 * specifically, this method tests if an image consists of two brightness
	 * values (black and white) or more. Colors are reduced to their brightness
	 * in this analysis, so this method will recognize a color image as
	 * grayscale as well.
	 * @param image the image to check
	 * @return true if the image is grayscale, false otherwise
	 */
	public static boolean isGrayScale(AnalysisImage image) {
		byte[][] brightness = image.getBrightness();
		
		int[] brightnessCounts = new int[16];
		Arrays.fill(brightnessCounts, 0);
		for (int c = 0; c < brightness.length; c++) {
			for (int r = 0; r < brightness[c].length; r++) {
				int bci = ((brightness[c][r] * brightnessCounts.length) / 128);
				while (bci >= brightnessCounts.length)
					bci--;
				brightnessCounts[bci]++;
			}
		}
		
		int nonZeroBrightnessCounts = 0;
		for (int b = 0; b < brightnessCounts.length; b++) {
			if (brightnessCounts[b] == 0)
				continue;
			nonZeroBrightnessCounts++;
			if (nonZeroBrightnessCounts > 2)
				return true;
		}
		return (nonZeroBrightnessCounts > 2);
	}
	
	/**
	 * Measure the contrast of an image. This method measures the brightness
	 * difference between neighboring pixels, ignoring 0 differences, bucketizes
	 * them in 128 buckets, and then returns the 5% quartile of the brightness
	 * differences.
	 * @param image the image to check
	 * @return true if the image is grayscale, false otherwise
	 */
	public static int measureContrast(AnalysisImage image) {
		byte[][] brightness = image.getBrightness();
		
		int[] brightnessDiffCounts = new int[128];
		int brightnessDiffCount = 0;
		Arrays.fill(brightnessDiffCounts, 0);
		for (int c = 1; c < (brightness.length-1); c++) {
			for (int r = 1; r < (brightness[c].length-1); r++) {
				int bd = Math.abs((4 * brightness[c][r]) - brightness[c-1][r] - brightness[c+1][r] - brightness[c][r-1] - brightness[c][r+1]);
				bd /= 4;
				if (bd == 0)
					continue;
				int bdi = ((bd * brightnessDiffCounts.length) / 128);
				while (bdi >= brightnessDiffCounts.length)
					bdi--;
				brightnessDiffCounts[bdi]++;
				brightnessDiffCount++;
			}
		}
		
		System.out.println("Contrast buckets: ");
		for (int d = (brightnessDiffCounts.length - 1); d >= 0; d--)
			System.out.println("  " + d + ": " + brightnessDiffCounts[d]);
		
		int brightnessDiffsCounted = 0;
		for (int d = (brightnessDiffCounts.length - 1); d >= 0; d--) {
			brightnessDiffsCounted += brightnessDiffCounts[d];
			if ((brightnessDiffsCounted * 20) >= brightnessDiffCount)
				return d;
		}
		
		return 0;
	}
	
	private static final int white = Color.WHITE.getRGB();
	
	/**
	 * Check if an analysisImage is inverted, and correct it if so. This method first
	 * computes the average brightness of the analysisImage, and then inverts the analysisImage
	 * if the latter is below the specified threshold.
	 * @param analysisImage the wrapped analysisImage
	 * @param threshold the threshold average brightness
	 * @return true if the analysisImage was changed, false otherwise
	 */
	public static boolean correctWhiteOnBlack(AnalysisImage analysisImage, byte threshold) {
		byte brightness = computeAverageBrightness(analysisImage);
		if (brightness > threshold)
			return false;
		float[] hsb = null;
		int rgb;
		for (int c = 0; c < analysisImage.image.getWidth(); c++)
			for (int r = 0; r < analysisImage.image.getHeight(); r++) {
				rgb = analysisImage.image.getRGB(c, r);
				hsb = new Color(rgb).getColorComponents(hsb);
				analysisImage.brightness[c][r] = ((byte) (127 - analysisImage.brightness[c][r]));
				analysisImage.image.setRGB(c, r, Color.HSBtoRGB(hsb [0], hsb[1], (1 - hsb[2])));
			}
		return true;
	}
	
	/**
	 * Apply a Gaussian blur to an image. This is mainly meant to even out small
	 * gaps or brightness differences in letters in gray scale images. If the
	 * argument radius is less than 1, this method does not change the image and
	 * returns false. The radius of the kernel used for computing the blur is
	 * three times the argument radius, to provide a smooth blurring.
	 * @param analysisImage the wrapped image
	 * @param radius the radius of the blur
	 * @return true
	 */
	public static boolean gaussBlur(AnalysisImage analysisImage, int radius) {
		return gaussBlur(analysisImage,  radius, radius, false);
	}
	
	/**
	 * Apply a Gaussian blur to an image. This is mainly meant to even out small
	 * gaps or brightness differences in letters in gray scale images. If the
	 * argument radius is less than 1, this method does not change the image and
	 * returns false. The radius of the kernel used for computing the blur is
	 * three times the argument radius, to provide a smooth blurring.
	 * @param analysisImage the wrapped image
	 * @param hRadius the horizontal radius of the blur
	 * @param vRadius the vertical radius of the blur
	 * @return true
	 */
	public static boolean gaussBlur(AnalysisImage analysisImage, int hRadius, int vRadius) {
		return gaussBlur(analysisImage, hRadius, vRadius, false);
	}
	
	/**
	 * Apply a Gaussian blur to an image. This is mainly meant to even out small
	 * gaps or brightness differences in letters in gray scale images. If the
	 * argument radius is less than 1, this method does not change the image and
	 * returns false. If the <code>sharpEdge</code> argument is set to true,
	 * the radius of the kernel used to compute the blur is exactly the argument
	 * radius; if it is set to false, radius of the kernel is three times the
	 * argument radius, to provide a smooth blurring.
	 * @param analysisImage the wrapped image
	 * @param radius the radius of the blur
	 * @param sharpEdge use a sharply edged blur instead of a smooth one?
	 * @return true
	 */
	public static boolean gaussBlur(AnalysisImage analysisImage, int radius, boolean sharpEdge) {
		return gaussBlur(analysisImage,  radius, radius);
	}
	
	/**
	 * Apply a Gaussian blur to an image. This is mainly meant to even out small
	 * gaps or brightness differences in letters in gray scale images. If the
	 * argument radius is less than 1, this method does not change the image and
	 * returns false. If the <code>sharpEdge</code> argument is set to true,
	 * the radius of the kernel used to compute the blur is exactly the argument
	 * radius; if it is set to false, radius of the kernel is three times the
	 * argument radius, to provide a smooth blurring.
	 * @param analysisImage the wrapped image
	 * @param hRadius the horizontal radius of the blur
	 * @param vRadius the vertical radius of the blur
	 * @param sharpEdge use a sharply edged blur instead of a smooth one?
	 * @return true
	 */
	public static boolean gaussBlur(AnalysisImage analysisImage, int hRadius, int vRadius, boolean sharpEdge) {
		if ((hRadius < 1) && (vRadius < 1))
			return false;
		
		//	get brightness array
		byte[][] brightness = analysisImage.getBrightness();
		
		//	blur array
		if (hRadius == vRadius)
			gaussBlur2D(brightness, hRadius, sharpEdge);
		else gaussBlur(brightness, hRadius, vRadius, sharpEdge);
		
		//	update image
		for (int c = 0; c < brightness.length; c++) {
			for (int r = 0; r < brightness[c].length; r++)
				analysisImage.image.setRGB(c, r, Color.HSBtoRGB(0, 0, (((float) brightness[c][r]) / 127)));
		}
		
		//	finally ...
		return true;
	}
	
	private static void gaussBlur2D(byte[][] brightness, int radius, boolean sharpEdge) {
		
		//	compute one dimensional kernel
		int kernelRadius = (radius * (sharpEdge ? 1 : 3));
		double[] kernel = new double[kernelRadius + 1 + kernelRadius];
		double kernelSum = 0;
		for (int k = -kernelRadius; k <= kernelRadius; k++) {
			kernel[k + kernelRadius] = (1 / Math.sqrt(2 * Math.PI * radius * radius)) * Math.pow(Math.E, -(((double) (k * k)) / (2 * radius * radius)));
			kernelSum += kernel[k + kernelRadius];
		}
		for (int k = -kernelRadius; k <= kernelRadius; k++)
			kernel[k + kernelRadius] /= kernelSum;
		
		//	build intermediate brightness array
		float[][] iBrightness = new float[brightness.length][brightness[0].length];
		
		//	apply kernel across rows
		for (int c = 0; c < brightness.length; c++) {
			for (int r = 0; r < brightness[c].length; r++) {
				double brightnessSum = 0;
				for (int k = -kernelRadius; k <= kernelRadius; k++) {
					int l = (c + k);
					if (l < 0)
						l = 0;
					else if (l > (brightness.length-1))
						l = (brightness.length-1);
					brightnessSum += (kernel[k + kernelRadius] * brightness[l][r]);
				}
				iBrightness[c][r] = ((float) brightnessSum);
			}
		}
		
		//	apply kernel down columns
		for (int c = 0; c < iBrightness.length; c++) {
			for (int r = 0; r < iBrightness[c].length; r++) {
				double iBrightnessSum = 0;
				for (int k = -kernelRadius; k <= kernelRadius; k++) {
					int l = (r + k);
					if (l < 0)
						l = 0;
					else if (l > (iBrightness[c].length-1))
						l = (iBrightness[c].length-1);
					iBrightnessSum += (kernel[k + kernelRadius] * iBrightness[c][l]);
				}
				int b = ((int) Math.round(iBrightnessSum));
				if (b < 0)
					b = 0;
				else if (b > 127)
					b = 127;
				brightness[c][r] = ((byte) b);
			}
		}
	}
	
	private static void gaussBlur(byte[][] brightness, int hRadius, int vRadius, boolean sharpEdge) {
		if (hRadius >= 1)
			gaussBlur1D(brightness, hRadius, sharpEdge, true);
		if (vRadius >= 1)
			gaussBlur1D(brightness, vRadius, sharpEdge, false);
	}
	
	private static void gaussBlur1D(byte[][] brightness, int radius, boolean sharpEdge, boolean blurRows) {
		
		//	compute one dimensional kernel
		int kernelRadius = (radius * (sharpEdge ? 1 : 3));
		double[] kernel = new double[kernelRadius + 1 + kernelRadius];
		double kernelSum = 0;
		for (int k = -kernelRadius; k <= kernelRadius; k++) {
			kernel[k + kernelRadius] = (1 / Math.sqrt(2 * Math.PI * radius * radius)) * Math.pow(Math.E, -(((double) (k * k)) / (2 * radius * radius)));
			kernelSum += kernel[k + kernelRadius];
		}
		for (int k = -kernelRadius; k <= kernelRadius; k++)
			kernel[k + kernelRadius] /= kernelSum;
		
		//	build intermediate brightness array
		float[][] iBrightness = new float[brightness.length][brightness[0].length];
		
		//	apply kernel across rows
		if (blurRows)
			for (int c = 0; c < brightness.length; c++) {
				for (int r = 0; r < brightness[c].length; r++) {
					double brightnessSum = 0;
					for (int k = -kernelRadius; k <= kernelRadius; k++) {
						int l = (c + k);
						if (l < 0)
							l = 0;
						else if (l > (brightness.length-1))
							l = (brightness.length-1);
						brightnessSum += (kernel[k + kernelRadius] * brightness[l][r]);
					}
					iBrightness[c][r] = ((float) brightnessSum);
				}
			}
		
		//	apply kernel down columns
		else for (int c = 0; c < iBrightness.length; c++) {
			for (int r = 0; r < iBrightness[c].length; r++) {
				double brightnessSum = 0;
				for (int k = -kernelRadius; k <= kernelRadius; k++) {
					int l = (r + k);
					if (l < 0)
						l = 0;
					else if (l > (iBrightness[c].length-1))
						l = (iBrightness[c].length-1);
					brightnessSum += (kernel[k + kernelRadius] * brightness[c][l]);
				}
				iBrightness[c][r] = ((float) brightnessSum);
			}
		}
		
		//	write result back to image
		for (int c = 0; c < iBrightness.length; c++) {
			for (int r = 0; r < iBrightness[c].length; r++) {
				int b = ((int) Math.round(iBrightness[c][r]));
				if (b < 0)
					b = 0;
				else if (b > 127)
					b = 127;
				brightness[c][r] = ((byte) b);
			}
		}
	}
	
	/**
	 * Eliminate the background of an image. This method first applies a low
	 * pass filter (large radius Gauss blur) to identfy the background, then
	 * subtracts it from the foreground. WARNING: This filter may eliminate or
	 * severely damage both color and gray scale images.
	 * @param analysisImage the wrapped image
	 * @param dpi the resolution of the image
	 * @return true
	 */
	public static boolean eliminateBackground(AnalysisImage analysisImage, int dpi) {
		
		//	get brightness array
		byte[][] brightness = analysisImage.getBrightness();
		
		//	copy and blur brightness array
		byte[][] backgroundBrightness = new byte[brightness.length][brightness[0].length];
		for (int c = 0; c < brightness.length; c++) {
			for (int r = 0; r < brightness[c].length; r++)
				backgroundBrightness[c][r] = brightness[c][r];
		}
		gaussBlur2D(backgroundBrightness, (dpi / 20), false);
//		
//		//	subtract background from foreground
//		for (int c = 0; c < brightness.length; c++)
//			for (int r = 0; r < brightness[c].length; r++) {
//				int b = 127 - ((127 - brightness[c][r]) - (127 - backgroundBrightness[c][r]));
//				if (b < 0)
//					b = 0;
//				else if (b > 127)
//					b = 127;
//				analysisImage.brightness[c][r] = ((byte) b);
//			}
		
		//	scale brightness to use background as white
		for (int c = 0; c < brightness.length; c++)
			for (int r = 0; r < brightness[c].length; r++) {
				if (backgroundBrightness[c][r] == 0)
					continue;
				int b = ((brightness[c][r] * 127) / backgroundBrightness[c][r]);
				if (b > 127)
					b = 127;
				analysisImage.brightness[c][r] = ((byte) b);
			}
		
		//	update image
		for (int c = 0; c < brightness.length; c++) {
			for (int r = 0; r < brightness[c].length; r++)
				analysisImage.image.setRGB(c, r, ((brightness[c][r] == 127) ? backgroundEliminated : Color.HSBtoRGB(0, 0, (((float) brightness[c][r]) / 127))));
		}
		
		if (true)
			return true;
		//	TODO make contrast enhancement continuous
		//	TODO use smaller tiles ...
		//	TODO ... and compute average over neighboring tiles as well
		
		//	TODO simply use Gauss blurred image, which IS the average brightness
		//	TODO ==> figure out appropriate radius (has to be larger than individual letters)
		
		//	lay out tiles
		int tileSize = dpi;
		int hSteps = (brightness.length / tileSize);
		while ((hSteps * tileSize) < brightness.length)
			hSteps++;
		int hStart = ((brightness.length - (hSteps * tileSize)) / 2);
		int vSteps = (brightness[0].length / tileSize);
		while ((vSteps * tileSize) < brightness[0].length)
			vSteps++;
		int vStart = ((brightness[0].length - (vSteps * tileSize)) / 2);
		
		//	compute min and max brightness for each tile
		byte[][] tileMinBrightnesses = new byte[hSteps][vSteps];
		byte[][] tileMaxBrightnesses = new byte[hSteps][vSteps];
		for (int h = 0; h < hSteps; h++)
			for (int v = 0; v < vSteps; v++) {
				int tileMinBrightness = 127;
				int tileMaxBrightness = 0;
				for (int c = Math.max((hStart + (h * tileSize)), 0); c < Math.min((hStart + ((h + 1) * tileSize)), brightness.length); c++)
					for (int r = Math.max((vStart + (v * tileSize)), 0); r < Math.min((vStart + ((v + 1) * tileSize)), brightness[c].length); r++) {
						tileMinBrightness = Math.min(tileMinBrightness, brightness[c][r]);
						tileMaxBrightness = Math.max(tileMaxBrightness, brightness[c][r]);
					}
				tileMinBrightnesses[h][v] = ((byte) tileMinBrightness);
				tileMaxBrightnesses[h][v] = ((byte) tileMaxBrightness);
			}
		
		//	enhance contrast of each tile
		for (int h = 0; h < hSteps; h++)
			for (int v = 0; v < vSteps; v++) {
				
				//	compute min and max brightness, figuring in neighboring tiles
				int tileMinBrightness = tileMinBrightnesses[h][v];
				int tileMaxBrightness = tileMaxBrightnesses[h][v];
				if (h != 0) {
					tileMinBrightness = Math.min(tileMinBrightness, tileMinBrightnesses[h-1][v]);
					tileMaxBrightness = Math.max(tileMaxBrightness, tileMaxBrightnesses[h-1][v]);
				}
				if ((h+1) != hSteps) {
					tileMinBrightness = Math.min(tileMinBrightness, tileMinBrightnesses[h+1][v]);
					tileMaxBrightness = Math.max(tileMaxBrightness, tileMaxBrightnesses[h+1][v]);
				}
				if (v != 0) {
					tileMinBrightness = Math.min(tileMinBrightness, tileMinBrightnesses[h][v-1]);
					tileMaxBrightness = Math.max(tileMaxBrightness, tileMaxBrightnesses[h][v-1]);
				}
				if ((v+1) != vSteps) {
					tileMinBrightness = Math.min(tileMinBrightness, tileMinBrightnesses[h][v+1]);
					tileMaxBrightness = Math.max(tileMaxBrightness, tileMaxBrightnesses[h][v+1]);
				}
				
				//	too bright as a whole, nothing darker than 25% gray
				if (tileMinBrightness > 96)
					continue;
				
				//	too dark as a whole, nothing brighter than 75% gray
				if (tileMaxBrightness < 32)
					continue;
				
				//	contrast already high, no need to enhance
				if ((tileMaxBrightness - tileMinBrightness) > 112)
					continue;
				
				//	contrast very low, likely noise
				if ((tileMaxBrightness - tileMinBrightness) < 16)
					continue;
				
				//	correct image
				for (int c = Math.max((hStart + (h * tileSize)), 0); c < Math.min((hStart + ((h + 1) * tileSize)), brightness.length); c++)
					for (int r = Math.max((vStart + (v * tileSize)), 0); r < Math.min((vStart + ((v + 1) * tileSize)), brightness[c].length); r++) {
//						int nb = (((brightness[c][r] - tileMinBrightness) * 127) / (tileMaxBrightness - tileMinBrightness + 1));
						int nb = (((brightness[c][r] - tileMinBrightness) * tileMaxBrightness) / (tileMaxBrightness - tileMinBrightness + 1));
						if (nb < 0)
							nb = 0;
						else if (nb > 127)
							nb = 127;
						analysisImage.brightness[c][r] = ((byte) nb);
					}
			}
		
		//	apply small radius Gauss blur to smooth out unevenly dark letters
		gaussBlur2D(brightness, 1, true);
		
		//	TODO abandon tiles, simply subtract background
		
		//	update image
		for (int c = 0; c < brightness.length; c++) {
			for (int r = 0; r < brightness[c].length; r++)
				analysisImage.image.setRGB(c, r, ((brightness[c][r] == 127) ? backgroundEliminated : Color.HSBtoRGB(0, 0, (((float) brightness[c][r]) / 127))));
		}
		
		//	finally ...
		return true;
	}
	
//	/**
//	 * Eliminate the background of an image. This method first applies a low
//	 * pass filter to identfy the background, then subtracts it from the
//	 * foreground. The smaller the tile size, the more details are removed.
//	 * WARNING: This filter may eliminate or severely demage both color and gray
//	 * scale images.
//	 * @param analysisImage the wrapped image
//	 * @param tileSize the size of the tiles to average for the low pass
//	 * @param tilePartCount the number of parts to analyze each tile in, in each
//	 *            dimension
//	 * @param uniformityThreshold the distance between minimum and maximum
//	 *            brightness below which to regatd a tile as uniformly white
//	 * @return true
//	 */
//	public static boolean eliminateBackground(AnalysisImage analysisImage, int tileSize, int tilePartCount, int uniformityThreshold) {
//		int tilePartSize = (tileSize / tilePartCount);
//		int hSteps = (analysisImage.image.getWidth() / tilePartSize);
//		while ((hSteps * tilePartSize) < analysisImage.image.getWidth())
//			hSteps++;
//		int hStart = ((analysisImage.image.getWidth() - (hSteps * tilePartSize)) / 2);
//		int vSteps = (analysisImage.image.getHeight() / tilePartSize);
//		while ((vSteps * tilePartSize) < analysisImage.image.getHeight())
//			vSteps++;
//		int vStart = ((analysisImage.image.getHeight() - (vSteps * tilePartSize)) / 2);
//		
//		byte[][] brightness = analysisImage.getBrightness();
//		byte[][] tilePartBrightness = new byte[hSteps][vSteps];
//		byte[][] tilePartMinBrightness = new byte[hSteps][vSteps];
//		byte[][] tilePartMaxBrightness = new byte[hSteps][vSteps];
//		for (int h = 0; h < hSteps; h++)
//			for (int v = 0; v < vSteps; v++) {
//				ImagePartRectangle tilePart = new ImagePartRectangle(analysisImage);
//				tilePart.leftCol = Math.max((hStart + (h * tilePartSize)), 0);
//				tilePart.rightCol = Math.min((hStart + ((h + 1) * tilePartSize)), analysisImage.image.getWidth());
//				tilePart.topRow = Math.max((vStart + (v * tilePartSize)), 0);
//				tilePart.bottomRow = Math.min((vStart + ((v + 1) * tilePartSize)), analysisImage.image.getHeight());
//				tilePartBrightness[h][v] = computeAverageBrightness(tilePart);
//				tilePartMinBrightness[h][v] = 127;
//				tilePartMaxBrightness[h][v] = 0;
//				for (int c = tilePart.leftCol; c < tilePart.rightCol; c++)
//					for (int r = tilePart.topRow; r < tilePart.bottomRow; r++) {
//						tilePartMinBrightness[h][v] = ((byte) Math.min(tilePartMinBrightness[h][v], brightness[c][r]));
//						tilePartMaxBrightness[h][v] = ((byte) Math.max(tilePartMaxBrightness[h][v], brightness[c][r]));
//					}
//			}
//		
//		byte[][] tilePartThesholds = new byte[hSteps][vSteps];
//		byte[][] tilePartMins = new byte[hSteps][vSteps];
//		byte[][] tilePartMaxs = new byte[hSteps][vSteps];
//		for (int h = 0; h < hSteps; h++)
//			for (int v = 0; v < vSteps; v++) {
//				int tilePartBrightnessSum = 0;
////				int tilePartMinSum = 0;
////				int tilePartMaxSum = 0;
//				int tilePartMin = 127;
//				int tilePartMax = 0;
//				int tilePartsCounted = 0;
//				for (int ho = -tilePartCount+1; ho <= 0; ho++)
//					for (int vo = -tilePartCount+1; vo <= 0; vo++) {
//						for (int hc = 0; hc < tilePartCount; hc++)
//							for (int vc = 0; vc < tilePartCount; vc++) {
//								int ht = (h + ho + hc);
//								if (ht < 0)
//									continue;
//								if (ht >= hSteps)
//									continue;
//								int vt = (v + vo + vc);
//								if (vt < 0)
//									continue;
//								if (vt >= vSteps)
//									continue;
//								tilePartBrightnessSum += tilePartBrightness[ht][vt];
////								tilePartMinSum += tilePartMinBrightness[ht][vt];
////								tilePartMaxSum += tilePartMaxBrightness[ht][vt];
//								tilePartMin = Math.min(tilePartMin, tilePartMinBrightness[ht][vt]);
//								tilePartMax = Math.max(tilePartMax, tilePartMaxBrightness[ht][vt]);
//								tilePartsCounted++;
//							}
//					}
//				tilePartThesholds[h][v] = ((byte) (tilePartBrightnessSum / tilePartsCounted));
////				tilePartMins[h][v] = ((byte) (tilePartMinSum / tilePartsCounted));
////				tilePartMaxs[h][v] = ((byte) (tilePartMaxSum / tilePartsCounted));
//				tilePartMins[h][v] = ((byte) tilePartMin);
//				tilePartMaxs[h][v] = ((byte) tilePartMax);
//			}
//		
//		for (int h = 0; h < hSteps; h++)
//			for (int v = 0; v < vSteps; v++) {
//				if (tilePartMaxBrightness[h][v] == 127)
//					continue;
////				byte brightnessThreshold = ((byte) (tilePartBrightness[h][v] - ((tilePartMaxBrightness[h][v] - tilePartMinBrightness[h][v]) / 8)));
////				byte brightnessThreshold = tilePartBrightness[h][v];
////				byte brightnessThreshold = ((byte) Math.min(tilePartBrightness[h][v], (tilePartMaxBrightness[h][v] - 16)));
////				byte brightnessThreshold = ((byte) Math.min(tilePartBrightness[h][v], (tilePartMaxBrightness[h][v] - 12)));
//				byte brightnessThreshold = ((byte) Math.min(tilePartBrightness[h][v], (tilePartMaxBrightness[h][v] - uniformityThreshold)));
////				int brightnessThreshold = (((tilePartBrightness[h][v] - tilePartMins[h][v]) * 127) / (tilePartMaxs[h][v] - tilePartMins[h][v] + 1));
//				for (int c = Math.max((hStart + (h * tilePartSize)), 0); c < Math.min((hStart + ((h + 1) * tilePartSize)), analysisImage.image.getWidth()); c++)
//					for (int r = Math.max((vStart + (v * tilePartSize)), 0); r < Math.min((vStart + ((v + 1) * tilePartSize)), analysisImage.image.getHeight()); r++) {
//						if (brightness[c][r] < brightnessThreshold)
//							continue;
//						analysisImage.brightness[c][r] = 127;
//						analysisImage.image.setRGB(c, r, white);
//////						if (brightness[c][r] >= brightnessThreshold) {
//////							analysisImage.brightness[c][r] = 127;
//////							analysisImage.image.setRGB(c, r, white);
//////						}
//////						else {
//////							int nb = (((brightness[c][r] - tilePartMinBrightness[h][v]) * 127) / (tilePartMaxBrightness[h][v] - tilePartMinBrightness[h][v] + 1));
////							int nb = (((brightness[c][r] - tilePartMins[h][v]) * 127) / (tilePartMaxs[h][v] - tilePartMins[h][v] + 1));
////							if (nb < 0)
////								nb = 0;
////							else if (nb > 127)
////								nb = 127;
////							if (nb >= brightnessThreshold) {
////								analysisImage.brightness[c][r] = 127;
////								analysisImage.image.setRGB(c, r, white);
////							}
////							else {
////								analysisImage.brightness[c][r] = ((byte) nb);
////								analysisImage.image.setRGB(c, r, Color.HSBtoRGB(0, 0, (((float) nb) / 127)));
////							}
//////						}
//					}
//			}
//		return true;
//	}
	
	/**
	 * Apply white balance to an image.
	 * @param analysisImage the wrapped image
	 * @return true
	 */
	public static boolean whitenWhite(AnalysisImage analysisImage) {
		byte avgBrightness = computeAverageBrightness(analysisImage);
		for (int c = 0; c < analysisImage.image.getWidth(); c++)
			for (int r = 0; r < analysisImage.image.getHeight(); r++) {
				if (analysisImage.brightness[c][r] == 127)
					continue;
				if (analysisImage.brightness[c][r] >= avgBrightness) {
					analysisImage.brightness[c][r] = 127;
					analysisImage.image.setRGB(c, r, whiteBalanced);
				}
			}
		return true;
	}
	
	/**
	 * Compute the average brightness of an image.
	 * @param analysisImage the wrapped image
	 * @return the avearge brightness
	 */
	public static byte computeAverageBrightness(AnalysisImage analysisImage) {
		byte[][] brightness = analysisImage.getBrightness();
		long brightnessSum = 0; 
		for (int c = 0; c < brightness.length; c++) {
			for (int r = 0; r < brightness[c].length; r++)
				brightnessSum += brightness[c][r];
		}
		return ((byte) (brightnessSum / (brightness.length * brightness[0].length)));
	}
	
	/**
	 * Compute the average brightness of a part of an image.
	 * @param rect the image part to compute the brightness for
	 * @return the avearge brightness
	 */
	public static byte computeAverageBrightness(ImagePartRectangle rect) {
		if ((rect.rightCol <= rect.leftCol) || (rect.bottomRow <= rect.topRow))
			return 0;
		byte[][] brightness = rect.analysisImage.getBrightness();
		long brightnessSum = 0; 
		for (int c = rect.leftCol; c < rect.rightCol; c++) {
			for (int r = rect.topRow; r < rect.bottomRow; r++)
				brightnessSum += brightness[c][r];
		}
		return ((byte) (brightnessSum / ((rect.rightCol - rect.leftCol) * (rect.bottomRow - rect.topRow))));
	}
	
	/**
	 * Compute the brightness distribution of an image.
	 * @param ai the image to compute the brightness for
	 * @param numBuckets the number of buckets to dicretize to
	 * @return the brightness distribution
	 */
	public static int[] getBrightnessDistribution(AnalysisImage ai, int numBuckets) {
		int[] brightnessDist = new int[Math.max(8, Math.min(128, numBuckets))];
		int brightnessBucketWidth = (128 / brightnessDist.length);
		Arrays.fill(brightnessDist, 0);
		byte[][] brightness = ai.getBrightness();
		for (int c = 0; c < brightness.length; c++) {
			for (int r = 0; r < brightness[c].length; r++)
				brightnessDist[brightness[c][r] / brightnessBucketWidth]++;
		}
		return brightnessDist;
	}
	
	/**
	 * Apply feather dusting to an image, i.e., set all non-white blocks below a
	 * given size threshold to white. In particular, this method applies region
	 * identification to the image and then filters non-white regions based on
	 * size, position, and distance to other non-white regions. Depending on
	 * whether or not the image is binary, this method behaves slightly
	 * differently: for binary images, region identification also expands
	 * diagonally, permitted minimum sizes for standalone spots are smaller, and
	 * maximum distances to other non-white sport are larger, this all to
	 * account for gray pixels already set to white during binarization.<br>
	 * This implementation estimates the minSize and minSoloSize parameters of
	 * the six-argument version as (dpi/100) and (dpi/25), respectively.
	 * @param analysisImage the wrapped image
	 * @param dpi the resolution of the image
	 * @param isBinary is the image binary, or gray scale or color?
	 * @param isSharp is the image sharp or blurry?
	 * @return true if the image was modified, false otherwise
	 */
	public static boolean featherDust(AnalysisImage analysisImage, int dpi, boolean isBinary, boolean isSharp) {
		return featherDust(analysisImage, (dpi / 100), (dpi / 25), dpi, isBinary, isSharp);
	}
	
	/**
	 * Apply feather dusting to an image, i.e., set all non-white blocks below a
	 * given size threshold to white. In particular, this method applies region
	 * identification to the image and then filters non-white regions based on
	 * size, position, and distance to other non-white regions. Depending on
	 * whether or not the image is binary, this method behaves slightly
	 * differently: for binary images, region identification also expands
	 * diagonally, permitted minimum sizes for standalone spots are smaller, and
	 * maximum distances to other non-white sport are larger, this all to
	 * account for gray pixels already set to white during binarization.
	 * @param analysisImage the wrapped image
	 * @param minSize the minimum size for non-white spots to be retained
	 * @param minSoloSize the minimum size for non-white spots far apart from
	 *            others to be retained
	 * @param dpi the resolution of the image
	 * @param isBinary is the image binary, or gray scale or color?
	 * @param isSharp is the image sharp or blurry?
	 * @return true if the image was modified, false otherwise
	 */
	public static boolean featherDust(AnalysisImage analysisImage, int minSize, int minSoloSize, int dpi, boolean isBinary, boolean isSharp) {
		return regionColorAndClean(analysisImage, minSize, minSoloSize, dpi, isBinary, isSharp);
		//	TODOne use color coding for coarse cleanup as well
		//	- merge regions closer than chopMargin, measured by bounding box
		//	- re-compute size and bounding box in the process
		//	- set regions to white if they are smaller than a letter ot digit at 6pt and too far away from anything else to be a punctuation mark in a sentence
		//	- make sure not to erase spaced dashes, though
	}
	
	/**
	 * Compute the region coloring of an image, which makes continuous light or
	 * dark regions of an image distinguishable. The result array has the
	 * same dimensions as the argument image. If the brightness threshold value
	 * is positive, this method considers any pixel at least as bright as this
	 * value to be white, i.e., not belonging to any region, but to the area
	 * between regions; if the brightness threshold value is negativ, this
	 * method considers any pixel at most as bright the corresponding absolute
	 * (positive) value as inter-region area. This means this method colors
	 * dark regions for positive thresholds, and light regions for negative
	 * thresholds. In either case, considering diagonally adjacent non-white
	 * (or non-black) pixels as connected is most sensible for binary images.
	 * @param ai the image to analyze
	 * @param brightnessThreshold the white threshold
	 * @param includeDiagonal consider diagonally adjacent pixels connected?
	 * @return the region coloring
	 */
	public static int[][] getRegionColoring(AnalysisImage ai, byte brightnessThreshold, boolean includeDiagonal) {
		byte[][] brightness = ai.getBrightness();
		if (brightness.length == 0)
			return new int[0][0];
		int[][] regionCodes = new int[brightness.length][brightness[0].length];
		for (int c = 0; c < regionCodes.length; c++)
			Arrays.fill(regionCodes[c], 0);
		int currentRegionCode = 1;
		for (int c = 0; c < brightness.length; c++)
			for (int r = 0; r < brightness[c].length; r++) {
				if (brightness[c][r] == 127)
					continue;
				if (regionCodes[c][r] != 0)
					continue;
				int rs = colorRegion(brightness, regionCodes, c, r, currentRegionCode, brightnessThreshold, includeDiagonal);
				if (DEBUG_REGION_COLORING) System.out.println("Region " + currentRegionCode + " is sized " + rs);
				currentRegionCode++;
				//	TODO assemble region size distribution, use it to estimate font size, and use estimate for cleanup thresholds
			}
		return regionCodes;
	}
	private static int colorRegion(byte[][] brightness, int[][] regionCodes, int c, int r, int regionCode, byte brightnessThreshold, boolean isBinaryImage) {
		ArrayList points = new ArrayList() {
			HashSet distinctContent = new HashSet();
			public boolean add(Object obj) {
				return (this.distinctContent.add(obj) ? super.add(obj) : false);
			}
		};
		points.add(new Point(c, r));
		
		int regionSize = 0;
		for (int p = 0; p < points.size(); p++) {
			Point point = ((Point) points.get(p));
			if ((point.c == -1) || (point.r == -1))
				continue;
			if ((point.c == brightness.length) || (point.r == brightness[c].length))
				continue;
			if ((0 < brightnessThreshold) && (brightnessThreshold <= brightness[point.c][point.r]))
				continue;
			if ((brightnessThreshold < 0) && (brightness[point.c][point.r] <= -brightnessThreshold))
				continue;
			if (regionCodes[point.c][point.r] != 0)
				continue;
			regionCodes[point.c][point.r] = regionCode;
			regionSize++;
			
			if (isBinaryImage)
				points.add(new Point((point.c - 1), (point.r - 1)));
			points.add(new Point((point.c - 1), point.r));
			if (isBinaryImage)
				points.add(new Point((point.c - 1), (point.r + 1)));
			points.add(new Point(point.c, (point.r - 1)));
			points.add(new Point(point.c, (point.r + 1)));
			if (isBinaryImage)
				points.add(new Point((point.c + 1), (point.r - 1)));
			points.add(new Point((point.c + 1), point.r));
			if (isBinaryImage)
				points.add(new Point((point.c + 1), (point.r + 1)));
		}
		return regionSize;
	}
	private static class Point {
		final int c;
		final int r;
		Point(int c, int r) {
			this.c = c;
			this.r = r;
		}
		public boolean equals(Object obj) {
			return ((obj instanceof Point) && (((Point) obj).c == this.c) && (((Point) obj).r == this.r));
		}
		public int hashCode() {
			return ((this.c << 16) + this.r);
		}
		public String toString() {
			return ("(" + this.c + "/" + this.r + ")");
		}
	}
	private static final boolean DEBUG_REGION_COLORING = false;
	
//	/**
//	 * Apply feather dusting to an image, i.e., set all non-white blocks below a
//	 * given size threshold to white.
//	 * @param analysisImage the wrapped image
//	 * @param chopMargin the minimum margin for chopping up the image
//	 * @param minSize the minimum size for non-white spots to be retained
//	 * @param minSoloSize the minimum size for non-white spots far apart from
//	 *            others to be retained
//	 * @param dpi the resolution of the image
//	 * @param isBinaryImage is the image binary, or gray scale or color?
//	 * @return true if the image was modified, false otherwise
//	 */
//	public static boolean featherDust(AnalysisImage analysisImage, int chopMargin, int minSize, int minSoloSize, int dpi, boolean isBinaryImage) {
//		//	no need to chop & clean if we do region coloring, as it would do the same, only region coloring is more effective
//		if (chopMargin == 1)
//			return regionColorAndClean(analysisImage, minSize, minSoloSize, dpi, isBinaryImage);
//		else return chopAndClean(getContentBox(analysisImage), chopMargin, true, minSize, false);
//	}
//	private static boolean chopAndClean(ImagePartRectangle rect, int chopMargin, boolean chopVertically, int minSize, boolean returnOnSingleSubRegion) {
//		ImagePartRectangle[] subRects = (chopVertically ? splitIntoColumns(rect, chopMargin) : splitIntoRows(rect, chopMargin));
//		
//		//	only one sub part, we're done
//		if (returnOnSingleSubRegion && (subRects.length == 1))
//			return false;
//		
//		//	investigate sub rectangles
//		boolean changed = false;
//		for (int s = 0; s < subRects.length; s++) {
//			
//			//	make sure it's compact
//			subRects[s] = narrowTopAndBottom(subRects[s]);
//			subRects[s] = narrowLeftAndRight(subRects[s]);
//			
//			//	too small to retain, clean it up
//			if (((subRects[s].rightCol - subRects[s].leftCol) < minSize) || ((subRects[s].bottomRow - subRects[s].topRow) < minSize)) {
//				for (int c = subRects[s].leftCol; c < subRects[s].rightCol; c++)
//					for (int r = subRects[s].topRow; r < subRects[s].bottomRow; r++) {
//						subRects[s].analysisImage.brightness[c][r] = 127;
////						subRects[s].analysisImage.image.setRGB(c, r, white);
//						subRects[s].analysisImage.image.setRGB(c, r, orange);
//					}
//				changed = true;
//			}
//			
//			//	this one's above size threshold
//			else if (chopAndClean(subRects[s], chopMargin, !chopVertically, minSize, true))
//				changed = true;
//		}
//		
//		//	finally ...
//		return changed;
//	}
	
	private static boolean regionColorAndClean(AnalysisImage ai, int minSize, int minSoloSize, int dpi, boolean isBinary, boolean isSharp) {
		boolean changed = false;
		
		byte[][] brightness = ai.getBrightness();
		if (brightness.length == 0)
			return changed;
		
		int[][] regionCodes = getRegionColoring(ai, ((byte) 127), isBinary);
		int regionCodeCount = 0;
		for (int c = 0; c < regionCodes.length; c++) {
			for (int r = 0; r < regionCodes[c].length; r++)
				regionCodeCount = Math.max(regionCodeCount, regionCodes[c][r]);
		}
		regionCodeCount++; // account for 0
//		final int[][] regionCodes = new int[brightness.length][brightness[0].length];
//		for (int c = 0; c < regionCodes.length; c++)
//			Arrays.fill(regionCodes[c], 0);
//		int currentRegionCode = 1;
//		for (int c = 0; c < brightness.length; c++)
//			for (int r = 0; r < brightness[c].length; r++) {
//				if (brightness[c][r] == 127)
//					continue;
//				if (regionCodes[c][r] != 0)
//					continue;
//				int rs = colorRegion(brightness, regionCodes, c, r, currentRegionCode, ((byte) 127), isBinary);
//				if (DEBUG_FEATHERDUST) System.out.println("Region " + currentRegionCode + " is sized " + rs);
//				currentRegionCode++;
//			}
		
		//	measure regions (size, surface, min and max column and row, min brightness)
		int[] regionSizes = new int[regionCodeCount];
		Arrays.fill(regionSizes, 0);
		int[] regionSurfaces = new int[regionCodeCount];
		Arrays.fill(regionSurfaces, 0);
		int[] regionMinCols = new int[regionCodeCount];
		Arrays.fill(regionMinCols, regionCodes.length);
		int[] regionMaxCols = new int[regionCodeCount];
		Arrays.fill(regionMaxCols, 0);
		int[] regionMinRows = new int[regionCodeCount];
		Arrays.fill(regionMinRows, regionCodes[0].length);
		int[] regionMaxRows = new int[regionCodeCount];
		Arrays.fill(regionMaxRows, 0);
		byte[] regionMinBrightness = new byte[regionCodeCount];
		Arrays.fill(regionMinBrightness, ((byte) 127));
		HashMap regionSurfacePointSets = new HashMap();
		HashSet regionCodesInRow = new HashSet();
		for (int c = 0; c < regionCodes.length; c++) {
			regionCodesInRow.clear();
			for (int r = 0; r < regionCodes[c].length; r++) {
				if (regionCodes[c][r] == 0)
					continue;
				regionSizes[regionCodes[c][r]]++;
				Integer regionCode = new Integer(regionCodes[c][r]);
				regionCodesInRow.add(regionCode);
				HashSet regionSurfacePoints = ((HashSet) regionSurfacePointSets.get(regionCode));
				if (regionSurfacePoints == null) {
					regionSurfacePoints = new HashSet();
					regionSurfacePointSets.put(regionCode, regionSurfacePoints);
				}
				if (((c-1) >= 0) && (regionCodes[c-1][r] == 0))
					regionSurfacePoints.add((c-1) + "-" + r);
				if (((c+1) < regionCodes.length) && (regionCodes[c+1][r] == 0))
					regionSurfacePoints.add((c+1) + "-" + r);
				if (((r-1) >= 0) && (regionCodes[c][r-1] == 0))
					regionSurfacePoints.add(c + "-" + (r-1));
				if (((r+1) < regionCodes[c].length) && (regionCodes[c][r+1] == 0))
					regionSurfacePoints.add(c + "-" + (r+1));
				regionMinCols[regionCodes[c][r]] = Math.min(c, regionMinCols[regionCodes[c][r]]);
				regionMaxCols[regionCodes[c][r]] = Math.max(c, regionMaxCols[regionCodes[c][r]]);
				regionMinRows[regionCodes[c][r]] = Math.min(r, regionMinRows[regionCodes[c][r]]);
				regionMaxRows[regionCodes[c][r]] = Math.max(r, regionMaxRows[regionCodes[c][r]]);
				regionMinBrightness[regionCodes[c][r]] = ((byte) Math.min(regionMinBrightness[regionCodes[c][r]], brightness[c][r]));
			}
			for (Iterator rcit = regionSurfacePointSets.keySet().iterator(); rcit.hasNext();) {
				Integer regionCode = ((Integer) rcit.next());
				if (regionCodesInRow.contains(regionCode))
					continue;
				HashSet regionSurfacePoints = ((HashSet) regionSurfacePointSets.get(regionCode));
				regionSurfaces[regionCode.intValue()] = regionSurfacePoints.size();
				regionSurfacePoints.clear();
				rcit.remove();
			}
		}
		for (Iterator rcit = regionSurfacePointSets.keySet().iterator(); rcit.hasNext();) {
			Integer regionCode = ((Integer) rcit.next());
			HashSet regionSurfacePoints = ((HashSet) regionSurfacePointSets.get(regionCode));
			regionSurfaces[regionCode.intValue()] = regionSurfacePoints.size();
			regionSurfacePoints.clear();
			rcit.remove();
		}
		
		//	clean up regions below and above size thresholds
		for (int c = 0; c < regionCodes.length; c++)
			for (int r = 0; r < regionCodes[c].length; r++) {
				if (regionCodes[c][r] <= 0)
					continue;
				
				boolean retain = true;
				int regionCode = regionCodes[c][r];
				if (DEBUG_FEATHERDUST) System.out.println("Assessing region " + regionCode + " (size " + regionSizes[regionCode] + ", surface " + regionSurfaces[regionCode] + ")");
				
				//	too faint to retain
				if (retain && (regionMinBrightness[regionCode] > 96)) // whole region lighter than 25% gray TODO find out if this threshold makes sense
					retain = false;
				if (!retain) {
					for (int cc = regionMinCols[regionCode]; cc <= regionMaxCols[regionCode]; cc++)
						for (int cr = regionMinRows[regionCode]; cr <= regionMaxRows[regionCode]; cr++) {
							if (regionCodes[cc][cr] != regionCode)
								continue;
							ai.brightness[cc][cr] = 127;
							ai.image.setRGB(cc, cr, tooFaint);
							regionCodes[cc][cr] = 0;
							changed = true;
						}
					if (DEBUG_FEATHERDUST) System.out.println(" ==> removed for faintness");
					continue;
				}
				
				//	TODO remove region if min size square is not embeddable
				
				//	compute region size to surface ratio
				int minSurface = (((int) Math.ceil(Math.sqrt(regionSizes[regionCode]))) * 4); // square
				int maxSurface = ((regionSizes[regionCode] * 2) + 2); // straight line
				
				//	TODO refine this estimate, ceil(sqrt) is too coarse for small regions, retains too much
				
				//	too thin in whichever direction
				if ((regionSizes[regionCode] < (minSize * minSoloSize)) && (((minSurface + maxSurface) / 2) <= regionSurfaces[regionCode])) {
					retain = false;
					if (DEBUG_FEATHERDUST) System.out.println(" --> removed as too thin");
				}
				
				//	too narrow, low, or overall small to retain
//				if (retain && ((colorCodeMaxCols[colorCode] - colorCodeMinCols[colorCode] + 1) < minSize))
//					retain = false;
//				if (retain && ((colorCodeMaxRows[colorCode] - colorCodeMinRows[colorCode] + 1) < minSize))
//					retain = false;
//				if (retain && (colorCodeCounts[colorCode] < (isBinaryImage ? ((minSize-1) * (minSize-1)) : (minSize * minSize))))
//					retain = false;
				if (retain && ((regionSizes[regionCode] * (isBinary ? 2 : 1)) < ((minSize * minSize) + (isBinary ? 1 : 0)))) {
					retain = false;
					if (DEBUG_FEATHERDUST) System.out.println(" --> removed for size below " + ((minSize * minSize) / (isBinary ? 2 : 1)));
				}
				
				//	covering at least 70% of page width or height (e.g. A5 scanned A4), likely dark scanning margin (subject to check, though)
				if (retain && ((regionMaxCols[regionCode] - regionMinCols[regionCode] + 1) > ((brightness.length * 7) / 10))) {
					if (DEBUG_FEATHERDUST) System.out.println(" - page wide");
					
					//	test page edges
					int topEdge = 0;
					int bottomEdge = 0;
					for (int lc = regionMinCols[regionCode]; lc <= regionMaxCols[regionCode]; lc++) {
						if (regionCodes[lc][0] == regionCode)
							topEdge++;
						if (regionCodes[lc][brightness[lc].length-1] == regionCode)
							bottomEdge++;
					}
					
					//	at page edge
					if (((topEdge + bottomEdge) * 2) > brightness[c].length)
						retain = false;
					
					//	test if at least (dpi/15) wide in most parts, and at least 90% of page width
					else if (((regionMaxCols[regionCode] - regionMinCols[regionCode] + 1) > ((brightness.length * 9) / 10))) {
						int squareArea = getSquareArea(regionCodes, regionMinCols[regionCode], regionMaxCols[regionCode], regionMinRows[regionCode], regionMaxRows[regionCode], regionCode, (dpi / 15), true);
						if (DEBUG_FEATHERDUST) System.out.println(" - got " + squareArea + " square area");
						if ((squareArea * 2) > regionSizes[regionCode]) {
							retain = false;
							if (DEBUG_FEATHERDUST) System.out.println(" --> removed for page width and thickness beyond " + (dpi / 15));
						}
					}
				}
				if (retain && ((regionMaxRows[regionCode] - regionMinRows[regionCode] + 1) > ((brightness[0].length * 7) / 10))) {
					if (DEBUG_FEATHERDUST) System.out.println(" - page high");
					
					//	test page edges
					int leftEdge = 0;
					int rightEdge = 0;
					for (int lr = regionMinRows[regionCode]; lr <= regionMaxRows[regionCode]; lr++) {
						if (regionCodes[0][lr] == regionCode)
							leftEdge++;
						if (regionCodes[brightness.length-1][lr] == regionCode)
							rightEdge++;
					}
					
					//	at page edge
					if (((leftEdge + rightEdge) * 2) > brightness.length)
						retain = false;
					
					//	test if at least (dpi/15) wide in most parts, and at least 90% of page height
					else if (((regionMaxRows[regionCode] - regionMinRows[regionCode] + 1) > ((brightness[0].length * 9) / 10))) {
						int squareArea = getSquareArea(regionCodes, regionMinCols[regionCode], regionMaxCols[regionCode], regionMinRows[regionCode], regionMaxRows[regionCode], regionCode, (dpi / 15), true);
						if (DEBUG_FEATHERDUST) System.out.println(" - got " + squareArea + " square area");
						if ((squareArea * 2) > regionSizes[regionCode]) {
							retain = false;
							if (DEBUG_FEATHERDUST) System.out.println(" --> removed for page height and thickness beyond " + (dpi / 15));
						}
					}
				}
				
				if (retain) {
					for (int cc = regionMinCols[regionCode]; cc <= regionMaxCols[regionCode]; cc++)
						for (int cr = regionMinRows[regionCode]; cr <= regionMaxRows[regionCode]; cr++) {
							if (regionCodes[cc][cr] == regionCode)
								regionCodes[cc][cr] = -regionCode;
						}
					if (DEBUG_FEATHERDUST) System.out.println(" ==> retained");
				}
				else {
					for (int cc = regionMinCols[regionCode]; cc <= regionMaxCols[regionCode]; cc++)
						for (int cr = regionMinRows[regionCode]; cr <= regionMaxRows[regionCode]; cr++) {
							if (regionCodes[cc][cr] != regionCode)
								continue;
							ai.brightness[cc][cr] = 127;
							ai.image.setRGB(cc, cr, tooSmall);
							regionCodes[cc][cr] = 0;
							changed = true;
						}
					if (DEBUG_FEATHERDUST) System.out.println(" ==> removed");
				}
			}
		
		//	set regions above solo size limit back to positive
		boolean[] assessed = new boolean[regionCodeCount];
		Arrays.fill(assessed, false);
		for (int c = 0; c < regionCodes.length; c++)
			for (int r = 0; r < regionCodes[c].length; r++) {
				if (regionCodes[c][r] >= 0)
					continue;
				if (assessed[-regionCodes[c][r]])
					continue;
				assessed[-regionCodes[c][r]] = true;
				
				boolean standalone = true;
				int regionCode = -regionCodes[c][r];
				if (DEBUG_FEATHERDUST) System.out.println("Assessing region " + regionCode + " (size " + regionSizes[regionCode] + ")");
				
				//	too narrow, low, or overall small to stand alone
				if (isBinary || isSharp) {
					if (standalone && ((regionMaxCols[regionCode] - regionMinCols[regionCode] + 1) < minSoloSize) && ((regionMaxRows[regionCode] - regionMinRows[regionCode] + 1) < minSoloSize)) {
						standalone = false;
						if (DEBUG_FEATHERDUST) System.out.println(" --> too small for standalone");
					}
				}
				else {
					if (standalone && ((regionMaxCols[regionCode] - regionMinCols[regionCode] + 1) < minSoloSize)) {
						standalone = false;
						if (DEBUG_FEATHERDUST) System.out.println(" --> too small for standalone");
					}
					if (standalone && ((regionMaxRows[regionCode] - regionMinRows[regionCode] + 1) < minSoloSize)) {
						standalone = false;
						if (DEBUG_FEATHERDUST) System.out.println(" --> too small for standalone");
					}
				}
				if (standalone && ((regionSizes[regionCode] * (isBinary ? 2 : 1)) < (minSize * minSoloSize))) {
					standalone = false;
					if (DEBUG_FEATHERDUST) System.out.println(" --> too small for standalone, below " + ((minSize * minSoloSize) / (isBinary ? 2 : 1)));
				}
				
				//	count minSize by minSize squares, and deny standalone if not covering at least 33% of region
				if (standalone && !isBinary && !isSharp) {
					int squareArea = getSquareArea(regionCodes, regionMinCols[regionCode], regionMaxCols[regionCode], regionMinRows[regionCode], regionMaxRows[regionCode], -regionCode, minSize, false);
					if ((squareArea * 3) < regionSizes[regionCode]) {
						standalone = false;
						if (DEBUG_FEATHERDUST) System.out.println(" --> too strewn for standalone");
					}
				}
				
				if (standalone) {
					for (int cc = regionMinCols[regionCode]; cc <= regionMaxCols[regionCode]; cc++)
						for (int cr = regionMinRows[regionCode]; cr <= regionMaxRows[regionCode]; cr++) {
							if (regionCodes[cc][cr] == -regionCode)
								regionCodes[cc][cr] = regionCode;
						}
					if (DEBUG_FEATHERDUST) System.out.println(" --> standalone");
				}
			}
		
		//	(iteratively) test regions that are still negative for proximity to positive regions, and set them to positive if any close
		boolean attachedNew;
		int attachRound = 0;
		do {
			attachedNew = false;
			attachRound++;
			Arrays.fill(assessed, false);
			for (int c = 0; c < regionCodes.length; c++)
				for (int r = 0; r < regionCodes[c].length; r++) {
					if (regionCodes[c][r] >= 0)
						continue;
					if (assessed[-regionCodes[c][r]])
						continue;
					assessed[-regionCodes[c][r]] = true;
					
					boolean attach = false;
					int regionCode = -regionCodes[c][r];
					if (DEBUG_FEATHERDUST) System.out.println("Attaching region " + regionCode + " (size " + regionSizes[regionCode] + ")");
					
					//	determine maximum distance, dependent on region size, as dots and hyphens in small fonts are also closer to adjacent letters
					int maxHorizontalMargin = Math.min(minSoloSize, regionSizes[regionCode]);
					int maxVerticalMargin = Math.min(((isBinary || isSharp) ? minSoloSize : (minSoloSize / 2)), regionSizes[regionCode]);
					
					//	search for standalone or attached regions around current one
					for (int cc = Math.max(0, (regionMinCols[regionCode] - maxHorizontalMargin)); cc <= Math.min((brightness.length-1), (regionMaxCols[regionCode] + maxHorizontalMargin)); cc++) {
						for (int cr = Math.max(0, (regionMinRows[regionCode] - maxVerticalMargin)); cr <= Math.min((brightness[cc].length-1), (regionMaxRows[regionCode] + maxVerticalMargin)); cr++)
							if (regionCodes[cc][cr] > 0) {
								attach = true;
								break;
							}
						if (attach)
							break;
					}
					
					//	attach current region
					if (attach) {
						for (int cc = regionMinCols[regionCode]; cc <= regionMaxCols[regionCode]; cc++)
							for (int cr = regionMinRows[regionCode]; cr <= regionMaxRows[regionCode]; cr++) {
								if (regionCodes[cc][cr] == -regionCode)
									regionCodes[cc][cr] = regionCode;
							}
						if (DEBUG_FEATHERDUST) System.out.println(" --> attached");
						attachedNew = true;
					}
				}
		}
		while (attachedNew && (attachRound < maxAttachRounds));
		if (DEBUG_FEATHERDUST) System.out.println("Attachments done in " + attachRound + " rounds");
		
		//	eliminate remaining negative regions
		for (int c = 0; c < regionCodes.length; c++)
			for (int r = 0; r < regionCodes[c].length; r++) {
				if (regionCodes[c][r] >= 0)
					continue;
				
				int regionCode = -regionCodes[c][r];
				if (DEBUG_FEATHERDUST) System.out.println("Removing region " + regionCode + " (size " + regionSizes[regionCode] + ")");
				for (int cc = regionMinCols[regionCode]; cc <= regionMaxCols[regionCode]; cc++)
					for (int cr = regionMinRows[regionCode]; cr <= regionMaxRows[regionCode]; cr++) {
						if (regionCodes[cc][cr] != -regionCode)
							continue;
						ai.brightness[cc][cr] = 127;
						ai.image.setRGB(cc, cr, tooSmallForStandalone);
						regionCodes[cc][cr] = 0;
						changed = true;
					}
			}
		
		return changed;
	}
	private static final boolean DEBUG_FEATHERDUST = true;
	private static final int maxAttachRounds = 3;
	/* working left to right, this should be sufficient for dotted lines, as
	 * well as punctuation marks like colons and semi colons, while preventing
	 * randomly dotted areas from being attached gradually from a few standalone
	 * spots */
	
	//	TODO set debug flat to false for export
	private static final boolean DEBUG_CLEANUP = false;
	private static final int whiteBalanced = (DEBUG_CLEANUP ? Color.CYAN.brighter().getRGB() : white);
	private static final int backgroundEliminated = (DEBUG_CLEANUP ? Color.YELLOW.getRGB() : white);
	private static final int tooFaint = (DEBUG_CLEANUP ? Color.MAGENTA.getRGB() : white);
	private static final int tooSmall = (DEBUG_CLEANUP ? Color.RED.getRGB() : white);
	private static final int tooSmallForStandalone = (DEBUG_CLEANUP ? Color.GREEN.getRGB(): white);
	
	private static int getSquareArea(int[][] colorCodes, int minCol, int maxCol, int minRow, int maxRow, int colorCode, int size, boolean sample) {
		int squareArea = 0;
		int probeStep = (sample ? Math.min((size / 4), 1) : 1);
		int iColorCode = -colorCode;
		for (int sc = minCol; sc <= (maxCol - size + 1); sc += probeStep)
			for (int sr = minRow; sr <= (maxRow - size + 1); sr += probeStep) {
				
				//	check for match
				boolean match = true;
				for (int c = sc; c < (sc + size); c++) {
					if ((colorCodes[c][sr] != colorCode) && (colorCodes[c][sr] != iColorCode)) {
						match = false;
						break;
					}
					if ((colorCodes[c][sr + size -1] != colorCode) && (colorCodes[c][sr + size -1] != iColorCode)) {
						match = false;
						break;
					}
				}
				for (int r = sr; r < (sr + size); r++) {
					if ((colorCodes[sc][r] != colorCode) && (colorCodes[sc][r] != iColorCode)) {
						match = false;
						break;
					}
					if ((colorCodes[sc + size - 1][r] != colorCode) && (colorCodes[sc][r] != iColorCode)) {
						match = false;
						break;
					}
				}
				
				//	count un-marked pixels
				if (match)
					for (int c = sc; c < (sc + size); c++) {
						for (int r = sr; r < (sr + size); r++)
							if (colorCodes[c][r] == colorCode) {
								colorCodes[c][r] = iColorCode;
								squareArea++;
							}
					}
			}
		
		//	undo inversion
		for (int c = minCol; c <= maxCol; c++)
			for (int r = minRow; r <= maxRow; r++) {
				if (colorCodes[c][r] == iColorCode)
					colorCodes[c][r] = colorCode;
			}
		
		//	finally ...
		return squareArea;
	}
	
//	found contrast increase not improving OCR result, rather the contrary !!!
//	
//	int blackBrightness = 0;
//	int counted = colorCodeBrightness[colorCode][blackBrightness];
//	while (counted < (colorCodeCounts[colorCode] / 50)) {
//		blackBrightness++;
//		counted += colorCodeBrightness[colorCode][blackBrightness];
//	}
//	
//	if (blackBrightness == 0) {
//		for (int cc = colorCodeMinCols[colorCode]; cc <= colorCodeMaxCols[colorCode]; cc++)
//			for (int cr = colorCodeMinRows[colorCode]; cr <= colorCodeMaxRows[colorCode]; cr++) {
//				if (colorCodes[cc][cr] == colorCode)
//					colorCodes[cc][cr] = 0;
//			}
//	}
//	
//	else {
//		System.out.println("Found black in region " + colorCode + " at " + blackBrightness);
//		float[] hsb = null;
//		int rgb;
//		for (int cc = colorCodeMinCols[colorCode]; cc <= colorCodeMaxCols[colorCode]; cc++)
//			for (int cr = colorCodeMinRows[colorCode]; cr <= colorCodeMaxRows[colorCode]; cr++) {
//				if (colorCodes[cc][cr] != colorCode)
//					continue;
//				if (ai.brightness[cc][cr] <= blackBrightness)
//					ai.brightness[cc][cr] = 0;
//				else ai.brightness[cc][cr] = ((byte) ((ai.brightness[cc][cr] * (127 - blackBrightness)) / 127));
//				rgb = ai.image.getRGB(cc, cr);
//				hsb = new Color(rgb).getColorComponents(hsb);
//				hsb[2] = (((float) ai.brightness[cc][cr]) / 127);
//				ai.image.setRGB(cc, cr, Color.HSBtoRGB(0, 0, hsb[2]));
//				colorCodes[cc][cr] = 0;
//			}
//		changed = true;
//	}
//	we cannot use recursion, even though it's more elegant,  because of stack overflows in practice (images as large as 3000 by 4000 pixels vs. at most 1024 stack frames) !!!
//	private static void extend(byte[][] brightness, int[][] colorCodes, int c, int r, int colorCode, int[] regionSize) {
//		if ((c == brightness.length) || (r == brightness[c].length))
//			return;
//		if (brightness[c][r] == 127)
//			return;
//		if (colorCodes[c][r] != 0)
//			return;
//		colorCodes[c][r] = colorCode;
//		regionSize[0]++;
//		if (c != 0)
//			extend(brightness, colorCodes, (c-1), r, colorCode, regionSize);
//		extend(brightness, colorCodes, (c+1), r, colorCode, regionSize);
//		if (r != 0)
//			extend(brightness, colorCodes, c, (r-1), colorCode, regionSize);
//		extend(brightness, colorCodes, c, (r+1), colorCode, regionSize);
//	}
	
	/**
	 * Obtain a rectangle encompassing the content of an image, i.e., the whole
	 * image except for any white margins.
	 * @param analysisImage the image to base the rectangle on
	 * @return a rectangle encompassing the content of the image
	 */
	public static ImagePartRectangle getContentBox(AnalysisImage analysisImage) {
		ImagePartRectangle rect = new ImagePartRectangle(analysisImage);
		rect = narrowLeftAndRight(rect);
		rect = narrowTopAndBottom(rect);
		return rect;
	}
	
	/**
	 * Produce a new image part rectangle covering several existing ones.
	 * @param parts the image part rectangles to cover
	 * @returna an image part rectangle covering the argument ones
	 */
	public static ImagePartRectangle getHull(ImagePartRectangle[] parts) {
		if (parts.length == 0)
			return null;
		else if (parts.length == 1)
			return parts[0];
		int left = Integer.MAX_VALUE;
		int right = 0;
		int top = Integer.MAX_VALUE;
		int bottom = 0;
		for (int p = 0; p < parts.length; p++) {
			left = Math.min(left, parts[p].leftCol);
			right = Math.max(right, parts[p].rightCol);
			top = Math.min(top, parts[p].topRow);
			bottom = Math.max(bottom, parts[p].bottomRow);
		}
		ImagePartRectangle hull = new ImagePartRectangle(parts[0].analysisImage);
		hull.leftCol = left;
		hull.rightCol = right;
		hull.topRow = top;
		hull.bottomRow = bottom;
		return hull;
	}
	
	/**
	 * Compute the area of an image part rectangle, i.e., the number of pixels it spans.
	 * @param ipr the image part rectangle whose area to compute
	 * @return the area of the argument image part rectangle
	 */
	public static int getArea(ImagePartRectangle ipr) {
		return ((ipr.rightCol - ipr.leftCol) * (ipr.bottomRow - ipr.topRow));
	}
	
	/**
	 * Copy the boundaries of one image part rectangle into another one.
	 * @param source the image part rectangle to copy the bounds from
	 * @param target the image part rectangle to copy the bounds to
	 */
	public static void copyBounds(ImagePartRectangle source, ImagePartRectangle target) {
		target.leftCol = source.leftCol;
		target.rightCol = source.rightCol;
		target.topRow = source.topRow;
		target.bottomRow = source.bottomRow;
	}
	
	/**
	 * Test if two image part rectangles are side by side, i.e., overlap in their
	 * vertical extent, i.e., there is at least one row of pixels that both
	 * intersect.
	 * @param ipr1 the first image part rectangle to check
	 * @param ipr2 the second image part rectangle to check
	 * @return true if the argument image part rectangle are side by side
	 */
	public static boolean isSideBySide(ImagePartRectangle ipr1, ImagePartRectangle ipr2) {
		return ((ipr1.topRow < ipr2.bottomRow) && (ipr2.topRow < ipr1.bottomRow));
	}
	
	/**
	 * Test if two image part rectangles are above one another, i.e., overlap in their
	 * horizontal extent, i.e., there is at least one column of pixels that both
	 * intersect.
	 * @param ipr1 the first image part rectangle to check
	 * @param ipr2 the second image part rectangle to check
	 * @return true if the argument image part rectangle are above one another
	 */
	public static boolean isAboveOneAnother(ImagePartRectangle ipr1, ImagePartRectangle ipr2) {
		return ((ipr1.leftCol < ipr2.rightCol) && (ipr2.leftCol < ipr1.rightCol));
	}
	
	/**
	 * Test if two image part rectangles overlap, i.e., there is at least one
	 * pixels that both include.
	 * @param ipr1 the first image part rectangle to check
	 * @param ipr2 the second image part rectangle to check
	 * @return true if the argument image part rectangle overlap
	 */
	public static boolean isOverlapping(ImagePartRectangle ipr1, ImagePartRectangle ipr2) {
		return (isSideBySide(ipr1, ipr2) && isAboveOneAnother(ipr1, ipr2));
	}
	
	/**
	 * Compute the fractional horizontal overlap of two image part rectangles. If
	 * all columns of pixels spanned by one of the argument image part rectangles
	 * is also spanned by the other, this method returns 1 even if the other
	 * image part rectangle is wider.
	 * @param ipr1 the first image part rectangle to check
	 * @param ipr2 the second image part rectangle to check
	 * @return the fractional horizontal overlap of the argument image part
	 *         rectangles
	 */
	public static double getHorizontalOverlap(ImagePartRectangle ipr1, ImagePartRectangle ipr2) {
		//	no overlap at all
		if (!isAboveOneAnother(ipr1, ipr2))
			return 0;
		
		//	one completely above or below the other
		if ((ipr1.leftCol <= ipr2.leftCol) && (ipr2.rightCol <= ipr1.rightCol))
			return 1;
		else if ((ipr2.leftCol <= ipr1.leftCol) && (ipr1.rightCol <= ipr2.rightCol))
			return 1;
		
		//	partially overlapping
		double overlap = ((ipr1.leftCol < ipr2.leftCol) ? (ipr1.rightCol - ipr2.leftCol) : (ipr2.rightCol - ipr1.leftCol));
		
		//	compute fraction (overlap divided by average width)
		return ((overlap * 2) / ((ipr1.rightCol - ipr1.leftCol) + (ipr2.rightCol - ipr2.leftCol)));
	}
	
	/**
	 * Compute the fractional vertical overlap of two image part rectangles. If
	 * all rows of pixels spanned by one of the argument image part rectangles
	 * is also spanned by the other, this method returns 1 even if the other
	 * image part rectangle is higher.
	 * @param ipr1 the first image part rectangle to check
	 * @param ipr2 the second image part rectangle to check
	 * @return the fractional vertical overlap of the argument image part
	 *         rectangles
	 */
	public static double getVerticalOverlap(ImagePartRectangle ipr1, ImagePartRectangle ipr2) {
		//	no overlap at all
		if (!isSideBySide(ipr1, ipr2))
			return 0;
		
		//	one completely above or below the other
		if ((ipr1.topRow <= ipr2.topRow) && (ipr2.bottomRow <= ipr1.bottomRow))
			return 1;
		else if ((ipr2.topRow <= ipr1.topRow) && (ipr1.bottomRow <= ipr2.bottomRow))
			return 1;
		
		//	partially overlapping
		double overlap = ((ipr1.topRow < ipr2.topRow) ? (ipr1.bottomRow - ipr2.topRow) : (ipr2.bottomRow - ipr1.topRow));
		
		//	compute fraction (overlap divided by average width)
		return ((overlap * 2) / ((ipr1.bottomRow - ipr1.topRow) + (ipr2.bottomRow - ipr2.topRow)));
	}
	
	/**
	 * Remove any white margines from the left and right edges of a rectangle.
	 * @param rect the rectangle to crop
	 * @return a rectangle with the left and right margins removed
	 */
	public static ImagePartRectangle narrowLeftAndRight(ImagePartRectangle rect) {
		if ((rect.bottomRow <= rect.topRow) || (rect.rightCol <= rect.leftCol))
			return rect;
		
		byte[][] brightness = rect.analysisImage.getBrightness();
		byte[] colBrightnesses = new byte[rect.rightCol - rect.leftCol];
		for (int c = rect.leftCol; c < rect.rightCol; c++) {
			int brightnessSum = 0;
			for (int r = rect.topRow; r < rect.bottomRow; r++)
				brightnessSum += brightness[c][r];
			colBrightnesses[c - rect.leftCol] = ((byte) (brightnessSum / (rect.bottomRow - rect.topRow)));
		}
		
		byte colBrightnessPivot = 127;//getPivot(colBrightnesses, offset);
		int minCol = -1;
		int maxCol = rect.rightCol;
		for (int c = rect.leftCol; c < rect.rightCol; c++) {
			if (colBrightnesses[c - rect.leftCol] < colBrightnessPivot) {
				if (minCol == -1)
					minCol = c;
				maxCol = c;
			}
		}
		
		if ((minCol <= rect.leftCol) && ((maxCol+1) >= rect.rightCol))
			return rect;
		
		ImagePartRectangle res = new ImagePartRectangle(rect.analysisImage);
		res.topRow = rect.topRow;
		res.bottomRow = rect.bottomRow;
		res.leftCol = ((minCol < 0) ? 0 : minCol);
		res.rightCol = maxCol+1;
		return res;
	}
	
	/**
	 * Remove any white margines from the top and bottom edges of a rectangle.
	 * @param rect the rectangle to crop
	 * @return a rectangle with the top and bottom margins removed
	 */
	public static ImagePartRectangle narrowTopAndBottom(ImagePartRectangle rect) {
		if ((rect.bottomRow <= rect.topRow) || (rect.rightCol <= rect.leftCol))
			return rect;
		
		byte[][] brightness = rect.analysisImage.getBrightness();
		byte[] rowBrightnesses = new byte[rect.bottomRow - rect.topRow];
		for (int r = rect.topRow; r < rect.bottomRow; r++) {
			int brightnessSum = 0; 
			for (int c = rect.leftCol; c < rect.rightCol; c++)
				brightnessSum += brightness[c][r];
			rowBrightnesses[r - rect.topRow] = ((byte) (brightnessSum / (rect.rightCol - rect.leftCol)));
		}
		
		byte rowBrightnessPivot = 127;//getPivot(rowBrightnesses, offset);
		
		int minRow = -1;
		int maxRow = rect.bottomRow;
		for (int r = rect.topRow; r < rect.bottomRow; r++) {
			if ((rowBrightnessPivot == 127) ? (rowBrightnesses[r - rect.topRow] < rowBrightnessPivot) : (rowBrightnesses[r - rect.topRow] <= rowBrightnessPivot)) {
				if (minRow == -1)
					minRow = r;
				maxRow = r;
			}
		}
		
		if ((minRow <= rect.topRow) && ((maxRow+1) >= rect.bottomRow))
			return rect;
		
		ImagePartRectangle res = new ImagePartRectangle(rect.analysisImage);
		res.leftCol = rect.leftCol;
		res.rightCol = rect.rightCol;
		res.topRow = ((minRow < 0) ? 0 : minRow);
		res.bottomRow = maxRow+1;
		return res;
	}
	
	/**
	 * Split a rectangular area of an image into columns at embedded vertical
	 * white stripes.
	 * @param rect the rectangle to split
	 * @return an array holding the columns
	 */
	public static ImagePartRectangle[] splitIntoColumns(ImagePartRectangle rect) {
		return splitIntoColumns(rect, 1, 0, true);
	}
	
	/**
	 * Split a rectangular area of an image into columns at embedded vertical
	 * white stripes wider than a given threshold.
	 * @param rect the rectangle to split
	 * @param minSplitMargin the minimum width for splitting
	 * @return an array holding the columns
	 */
	public static ImagePartRectangle[] splitIntoColumns(ImagePartRectangle rect, int minSplitMargin) {
		return splitIntoColumns(rect, minSplitMargin, 0, true);
	}
	
	/**
	 * Split a rectangular area of an image into columns at embedded vertical
	 * white stripes.
	 * @param rect the rectangle to split
	 * @param shearDegrees the deviation of the split lines from the vertical
	 * @param requireVerticalSplit require a white vertical pass in sheared
	 *            splits?
	 * @return an array holding the columns
	 */
	public static ImagePartRectangle[] splitIntoColumns(ImagePartRectangle rect, double shearDegrees, boolean requireVerticalSplit) {
		return splitIntoColumns(rect, 1, shearDegrees, requireVerticalSplit);
	}
	
	/**
	 * Split a rectangular area of an image into columns at embedded vertical
	 * white stripes wider than a given threshold.
	 * @param rect the rectangle to split
	 * @param minSplitMargin the minimum width for splitting
	 * @param shearDegrees the deviation of the split lines from the vertical
	 * @param requireVerticalSplit require a white vertical pass in sheared
	 *            splits?
	 * @return an array holding the columns
	 */
	public static ImagePartRectangle[] splitIntoColumns(ImagePartRectangle rect, int minSplitMargin, double shearDegrees, boolean requireVerticalSplit) {
		if ((rect.bottomRow <= rect.topRow) || (rect.rightCol <= rect.leftCol)) {
			ImagePartRectangle[] iprs = {rect};
			return iprs;
		}
		
		int maxOffset = ((shearDegrees == 0) ? 0 : Math.abs((int) (Math.tan(shearDegrees * Math.PI / 180) * ((double) (rect.bottomRow - rect.topRow)))));
		maxOffset = ((int) (maxOffset * Math.signum(shearDegrees)));
		if (maxOffset == 0)
			requireVerticalSplit = false; // no need for that hassle in vertical split
		int[] offsets = new int[rect.bottomRow - rect.topRow];
		for (int o = 0; o < offsets.length; o++) {
			if (maxOffset > 0)
				offsets[o] = (((o * maxOffset) + (offsets.length / 2)) / offsets.length);
			else offsets[o] = ((((offsets.length - o - 1) * -maxOffset) + (offsets.length / 2)) / offsets.length);
		}
		
		byte[][] brightness = rect.analysisImage.getBrightness();
		byte[] colBrightnesses = new byte[rect.rightCol - rect.leftCol];
		byte[] sColBrightnesses = new byte[rect.rightCol - rect.leftCol];
		for (int c = rect.leftCol; c < rect.rightCol; c++) {
			int brightnessSum = 0;
			byte minBrightness = 127;
			int sBrightnessSum = 0;
			byte sMinBrightness = 127;
			int sc;
			byte sb;
			for (int r = rect.topRow; r < rect.bottomRow; r++) {
				brightnessSum += brightness[c][r];
				minBrightness = ((byte) Math.min(minBrightness, brightness[c][r]));
//				sc = c - offsets[r - rect.topRow]; // WRONG: we have to subtract the offset so positive angles correspond to shearing top of rectangle rightward
				sc = c + offsets[r - rect.topRow]; // RIGHT: we have to add the offset so positive angles correspond to shearing top of rectangle rightward
				sb = (((rect.leftCol <= sc) && (sc < rect.rightCol)) ? brightness[sc][r] : 127);
				sBrightnessSum += sb;
				sMinBrightness = ((byte) Math.min(sMinBrightness, sb));
			}
			colBrightnesses[c - rect.leftCol] = ((byte) (brightnessSum / (rect.bottomRow - rect.topRow)));
			sColBrightnesses[c - rect.leftCol] = ((byte) (sBrightnessSum / (rect.bottomRow - rect.topRow)));
		}
		
		ArrayList rects = new ArrayList();
		boolean[] whiteCols = new boolean[rect.rightCol - rect.leftCol];
		boolean[] sWhiteCols = new boolean[rect.rightCol - rect.leftCol];
		
		byte colBrightnessPivot = 127;
		
		for (int c = rect.leftCol; c < rect.rightCol; c++) {
			whiteCols[c - rect.leftCol] = (colBrightnesses[c - rect.leftCol] >= colBrightnessPivot);
			sWhiteCols[c - rect.leftCol] = (sColBrightnesses[c - rect.leftCol] >= colBrightnessPivot);
		}
		
		int white = 0;
		int left = -1;
//		int lastRight = 0;
		for (int c = rect.leftCol; c < rect.rightCol; c++) {
			if (sWhiteCols[c - rect.leftCol]) {
				white++;
				if ((white >= minSplitMargin) && (left != -1)) {
					int lc = Math.max((left - (maxOffset / 2)), rect.leftCol);
					int rc = Math.min(((c - white + 1) - (maxOffset / 2)), rect.rightCol);
					if (lc < rect.leftCol)
						lc = rect.leftCol;
					if (rc > rect.rightCol)
						rc = rect.rightCol;
					
					//	this actually can happen with tiny chunks (e.g. horizontal lines) and big slopes ...
					if (rc < lc)
						continue;
					
					ImagePartRectangle res = new ImagePartRectangle(rect.analysisImage);
					res.topRow = rect.topRow;
					res.bottomRow = rect.bottomRow;
					res.leftCol = lc;
					res.rightCol = rc;
					if (requireVerticalSplit) {
						while ((rect.leftCol < res.leftCol) && !whiteCols[res.leftCol - rect.leftCol])
							res.leftCol--;
						while (((res.leftCol + 1) < rect.rightCol) && whiteCols[res.leftCol - rect.leftCol])
							res.leftCol++;
						while ((res.rightCol < rect.rightCol) && !whiteCols[res.rightCol - rect.leftCol])
							res.rightCol++;
						while ((rect.leftCol < (res.rightCol - 1)) && whiteCols[res.rightCol-1 - rect.leftCol])
							res.rightCol--;
					}
//					if (lastRight < res.leftCol) {
//						rects.add(res);
//						lastRight = res.rightCol;
//						left = -1;
//					}
					rects.add(res);
					left = -1;
				}
			}
			else {
				if (left == -1)
					left = c;
				white = 0;
			}
		}
		
		if (left == rect.leftCol) {
			ImagePartRectangle[] res = {rect};
			return res;
		}
		
		if (left != -1) {
			ImagePartRectangle res = new ImagePartRectangle(rect.analysisImage);
			res.topRow = rect.topRow;
			res.bottomRow = rect.bottomRow;
			res.leftCol = Math.min(Math.max((left - (maxOffset / 2)), rect.leftCol), (rect.rightCol - 1));
			if (requireVerticalSplit) {
				while ((rect.leftCol < res.leftCol) && !whiteCols[res.leftCol - rect.leftCol])
					res.leftCol--;
				while (((res.leftCol + 1) < rect.rightCol) && whiteCols[res.leftCol - rect.leftCol])
					res.leftCol++;
			}
			res.rightCol = rect.rightCol;
			rects.add(res);
		}
		return ((ImagePartRectangle[]) rects.toArray(new ImagePartRectangle[rects.size()]));
	}
	
	/**
	 * Split a rectangular area of an image into rows at embedded horizontal
	 * white stripes.
	 * @param rect the rectangle to split
	 * @return an array holding the rows
	 */
	public static ImagePartRectangle[] splitIntoRows(ImagePartRectangle rect) {
		return splitIntoRows(rect, 1, 0, -1, 0);
	}
	
	/**
	 * Split a rectangular area of an image into rows at embedded horizontal
	 * white stripes.
	 * @param rect the rectangle to split
	 * @param maxSkewAngle the maximum angle (in degrees) for skewed splits
	 *            (non-positive values deactivate skewed splitting)
	 * @return an array holding the rows
	 */
	public static ImagePartRectangle[] splitIntoRows(ImagePartRectangle rect, double maxSkewAngle) {
		return splitIntoRows(rect, 1, maxSkewAngle, -1, 0);
	}
	
	/**
	 * Split a rectangular area of an image into rows at embedded horizontal
	 * white stripes higher than a given threshold.
	 * @param rect the rectangle to split
	 * @param minSplitMargin the minimum height for splitting
	 * @return an array holding the rows
	 */
	public static ImagePartRectangle[] splitIntoRows(ImagePartRectangle rect, int minSplitMargin) {
		return splitIntoRows(rect, minSplitMargin, 0, -1, 0);
	}
	
	/**
	 * Split a rectangular area of an image into rows at embedded horizontal
	 * white stripes higher than a given threshold.
	 * @param rect the rectangle to split
	 * @param minSplitMargin the minimum height for splitting
	 * @param maxSkewAngle the maximum angle (in degrees) for skewed splits
	 *            (non-positive values deactivate skewed splitting)
	 * @return an array holding the rows
	 */
	public static ImagePartRectangle[] splitIntoRows(ImagePartRectangle rect, int minSplitMargin, double maxSkewAngle) {
		return splitIntoRows(rect, minSplitMargin, maxSkewAngle, -1, 0);
	}
	
	/**
	 * Split a rectangular area of an image into rows at embedded horizontal
	 * white stripes higher than a given threshold.
	 * @param rect the rectangle to split
	 * @param minSplitMargin the minimum height for splitting
	 * @param maxSkewAngle the maximum angle (in degrees) for skewed splits
	 *            (non-positive values deactivate skewed splitting)
	 * @param zigzagPartWidth the minimum width of a straight part in a zigzag
	 *            split (non-positive values deactivate zigzag splitting)
	 * @param maxZigzagSlope the maximum vertical deviation of a zigzag split
	 * @return an array holding the rows
	 */
	public static ImagePartRectangle[] splitIntoRows(ImagePartRectangle rect, int minSplitMargin, double maxSkewAngle, int zigzagPartWidth, int maxZigzagSlope) {
		if ((rect.bottomRow <= rect.topRow) || (rect.rightCol <= rect.leftCol)) {
			ImagePartRectangle[] iprs = {rect};
			return iprs;
		}
		
		ArrayList rects = new ArrayList();
		ImagePartRectangle[] iprs = splitIntoRows(rect, minSplitMargin, 0);
		rects.addAll(Arrays.asList(iprs));
//		System.out.println(" --> got " + iprs.length + " rows after straight split");
		
		//	try skewed cuts up to two tenths of a degree upward and downward, as FFT might fail to discover small rotations
		ImagePartRectangle[] skewCutIprs;
		final int minSkew = 1;
		final int maxSkew = ((maxSkewAngle > 0) ? Math.abs((int) (Math.tan(maxSkewAngle * Math.PI / 180) * ((double) (rect.rightCol - rect.leftCol)))) : 0);
		int lastSkew = -1;
		HashSet completedSplits = new HashSet();
		for (int r = 0; r < rects.size(); r++) {
			ImagePartRectangle ipr = ((ImagePartRectangle) rects.get(r));
			int skew = ((lastSkew == -1) ? minSkew : lastSkew);
			while (maxSkew != 0) {
				if (completedSplits.add(ipr.getId() + skew)) {
					skewCutIprs = splitIntoRows(ipr, minSplitMargin, skew);
					if (skewCutIprs.length > 1) {
						rects.set(r, skewCutIprs[0]);
						for (int s = 1; s < skewCutIprs.length; s++) {
							r++;
							rects.add(r, skewCutIprs[s]);
						}
						r--;
						lastSkew = skew;
						break;
					}
				}
				if (completedSplits.add(ipr.getId() + (-skew))) {
					skewCutIprs = splitIntoRows(ipr, minSplitMargin, -skew);
					if (skewCutIprs.length > 1) {
						rects.set(r, skewCutIprs[0]);
						for (int s = 1; s < skewCutIprs.length; s++) {
							r++;
							rects.add(r, skewCutIprs[s]);
						}
						r--;
						lastSkew = skew;
						break;
					}
				}
				if (skew >= maxSkew) {
					lastSkew = -1;
					break;
				}
				skew++;
			}
		}
		
		//	no zigzagging, we're done
		if (zigzagPartWidth < 1)
			return ((ImagePartRectangle[]) rects.toArray(new ImagePartRectangle[rects.size()]));
		
		//	try skewed cuts up to two tenths of a degree upward and downward, as FFT might fail to discover small rotations
		ImagePartRectangle[] zigzagCutIprs;
		for (int r = 0; r < rects.size(); r++) {
			ImagePartRectangle ipr = ((ImagePartRectangle) rects.get(r));
			zigzagCutIprs = splitIntoRowsZigzag(ipr, minSplitMargin, zigzagPartWidth, maxZigzagSlope);
			if (zigzagCutIprs.length > 1) {
				rects.set(r, zigzagCutIprs[0]);
				for (int s = 1; s < zigzagCutIprs.length; s++) {
					r++;
					rects.add(r, zigzagCutIprs[s]);
				}
			}
		}
		
		return ((ImagePartRectangle[]) rects.toArray(new ImagePartRectangle[rects.size()]));
	}
	
	private static ImagePartRectangle[] splitIntoRowsZigzag(ImagePartRectangle rect, int minSplitMargin, int minPartLength, int maxSlope) {
		
		//	avoid rediculously small parts
		if (minPartLength < 2) {
			ImagePartRectangle[] iprs = {rect};
			return iprs;
		}
		
		//	get brightness grid
		byte[][] brightness = rect.analysisImage.getBrightness();
		
		//	this array stores how far to the right a part extends, so finding a path becomes easier
		int[][] parts = new int[brightness.length][];
		for (int p = 0; p < parts.length; p++) {
			parts[p] = new int[brightness[p].length];
			Arrays.fill(parts[p], 0);
		}
		
		//	find partial passes
		for (int c = 0; c < ((rect.rightCol - rect.leftCol) - minPartLength); c++) {
			boolean foundPassRow = false;
			for (int r = 0; r < (rect.bottomRow - rect.topRow); r++) {
				
				//	we've been here before
				if (parts[c][r] != 0) {
					foundPassRow = true;
					continue;
				}
				
				//	try to find part from current starting point rightward
				int partLength = 0;
				while ((c + rect.leftCol + partLength) < rect.rightCol) {
					if (brightness[c + rect.leftCol + partLength][r + rect.topRow] == 127)
						partLength++;
					else break;
				}
				
				//	this one's long enough, mark it
				if (minPartLength <= partLength) {
					for (int pc = 0; pc < partLength; pc++)
						parts[c + pc][r] = (c + partLength);
					foundPassRow = true;
				}
			}
			
			//	some column we just cannot get through, no use checking any further
			if (!foundPassRow) {
				ImagePartRectangle[] res = {rect};
				return res;
			}
		}
		
		//	assemble partial passes to complete passes
		ArrayList splits = new ArrayList();
		ArrayList splitGroup = new ArrayList();
		for (int r = 0; r < (rect.bottomRow - rect.topRow); r++) {
			
			//	no passage here
			if (parts[0][r] == 0) {
				
				//	close current split group
				if (splitGroup.size() != 0) {
					int minWidth = Integer.MAX_VALUE;
					ZigzagSplit minWidthSplit = null;
					for (int s = 0; s < splitGroup.size(); s++) {
						ZigzagSplit zs = ((ZigzagSplit) splitGroup.get(s));
						if ((zs.maxRow - zs.minRow) < minWidth) {
							minWidth = (zs.maxRow - zs.minRow);
							minWidthSplit = zs;
						}
					}
					if (minWidthSplit != null)
						splits.add(minWidthSplit);
					splitGroup.clear();
				}
				continue;
			}
			
			//	start following partial pass
			int midPassRow = -1;
			int row = r;
			int col = rect.leftCol + 1;
			int minRow = row;
			int maxRow = row;
			int searchOffset = (minPartLength / 10);
			while (true) {
				col = (rect.leftCol + parts[col - rect.leftCol - 1][row]);
				
				//	find continuation above or below
				if (col < rect.rightCol) {
					int maxProgress = col - rect.leftCol;
					int maxProgressRow = -1;
					int pr = row;
					
					//	TODO_ne: minimum lenght of parts prevents wild straying between words, etc.// limit deviation to sensible bounds to prevent trouble cutting wild paths through images
					//	TODO_ne: unnecessary, as later split happens at brightest row between split limits anyways// remember staring row, and keep tendency back there
					
					while ((pr != 0) && (parts[col - searchOffset - rect.leftCol - 1][pr-1] != 0)) {
						pr--;
						if (parts[col - searchOffset - rect.leftCol - 1][pr] > maxProgress) {
							maxProgress = parts[col - searchOffset - rect.leftCol - 1][pr];
							maxProgressRow = pr;
						}
					}
					pr = row;
					while (((pr+1) < (rect.bottomRow - rect.topRow)) && (parts[col - searchOffset - rect.leftCol - 1][pr+1] != 0)) {
						pr++;
						if (parts[col - searchOffset - rect.leftCol - 1][pr] > maxProgress) {
							maxProgress = parts[col - searchOffset - rect.leftCol - 1][pr];
							maxProgressRow = pr;
						}
					}
					
					//	we have a continuation
					if (maxProgressRow != -1) {
						row = maxProgressRow;
						minRow = Math.min(minRow, row);
						maxRow = Math.max(maxRow, row);
					}
					
					//	we've reached a dead end
					else break;
				}
				
				//	we are through, compute horizontal pass
				else {
					midPassRow = ((r + row) / 2) + rect.topRow;
					break;
				}
			}
			
			//	we've found a pass (strict 'less than' is OK, as both maxRow and minRow are inclusive)
			if (midPassRow != -1) {
				if ((maxRow - minRow) < maxSlope)
					splitGroup.add(new ZigzagSplit((minRow + rect.topRow), (maxRow + rect.topRow)));
			}
		}
		
		//	close remaining split group
		if (splitGroup.size() != 0) {
			int minWidth = Integer.MAX_VALUE;
			ZigzagSplit minWidthSplit = null;
			for (int s = 0; s < splitGroup.size(); s++) {
				ZigzagSplit zs = ((ZigzagSplit) splitGroup.get(s));
				if ((zs.maxRow - zs.minRow) < minWidth) {
					minWidth = (zs.maxRow - zs.minRow);
					minWidthSplit = zs;
				}
			}
			if (minWidthSplit != null)
				splits.add(minWidthSplit);
			splitGroup.clear();
		}
		
		//	nothing to do
		if (splits.isEmpty()) {
			ImagePartRectangle[] res = {rect};
			return res;
		}
		
		//	TODOne try out straigh right-through-the-middle approach (with word correction, this should not cause too much demage)
		//	==> still no good
		//	TODOne try inserting margin, size of split
		//	==> seems to work fine, verify with more tests
		
		//	perform splits
		ArrayList rects = new ArrayList();
		int topRow = rect.topRow;
		for (int s = 0; s < splits.size(); s++) {
			ZigzagSplit zs = ((ZigzagSplit) splits.get(s));
			
			//	perform split
			ImagePartRectangle res = new ImagePartRectangle(rect.analysisImage);
			res.leftCol = rect.leftCol;
			res.rightCol = rect.rightCol;
			res.topRow = topRow;
			res.bottomRow = zs.minRow + 1;
			while (res.bottomRow <= res.topRow) {
				res.bottomRow++;
				res.topRow--;
			}
			res = narrowTopAndBottom(res);
			if (res.isEmpty())
				continue;
			rects.add(res);
			topRow = zs.maxRow;
		}
		
		//	mark last rectangle
		if (topRow < rect.bottomRow-1) {
			ImagePartRectangle res = new ImagePartRectangle(rect.analysisImage);
			res.topRow = topRow;
			res.leftCol = rect.leftCol;
			res.bottomRow = rect.bottomRow;
			res.rightCol = rect.rightCol;
			res = narrowTopAndBottom(res);
			if (!res.isEmpty())
				rects.add(res);
		}
		
		//	finally ...
		return ((ImagePartRectangle[]) rects.toArray(new ImagePartRectangle[rects.size()]));
	}
	
	private static class ZigzagSplit {
		final int minRow;
		final int maxRow;
		ZigzagSplit(int minRow, int maxRow) {
			this.minRow = minRow;
			this.maxRow = maxRow;
		}
	}
	
	private static ImagePartRectangle[] splitIntoRows(ImagePartRectangle rect, int minSplitMargin, int maxOffset) {
		int[] offsets = new int[rect.rightCol - rect.leftCol];
		for (int o = 0; o < offsets.length; o++) {
			if (maxOffset > 0)
				offsets[o] = (((o * maxOffset) + (offsets.length / 2)) / offsets.length);
			else offsets[o] = (((o * maxOffset) - (offsets.length / 2)) / offsets.length);
		}
		
		byte[][] brightness = rect.analysisImage.getBrightness();
		byte[] rowBrightnesses = new byte[rect.bottomRow - rect.topRow];
		for (int r = rect.topRow; r < rect.bottomRow; r++) {
			int brightnessSum = 0;
			int or;
			byte b;
			for (int c = rect.leftCol; c < rect.rightCol; c++) {
				or = r + offsets[c - rect.leftCol];
				b = (((rect.topRow <= or) && (or < rect.bottomRow)) ? brightness[c][or] : 127);
				brightnessSum += b;
			}
			rowBrightnesses[r - rect.topRow] = ((byte) (brightnessSum / (rect.rightCol - rect.leftCol)));
		}
		
		ArrayList rects = new ArrayList();
		boolean[] whiteRows = new boolean[rect.bottomRow - rect.topRow];
		
		short rowBrightnessPivot = 127;
		
		//	TODOne try out straigh right-through-the-middle approach (with word correction, this should not cause too much demage)
		//	==> still no good
		//	TODOne try inserting margin, size of split
		//	==> seems to work fine, verify with more tests
		
		for (int r = rect.topRow; r < rect.bottomRow; r++)
			whiteRows[r - rect.topRow] = (rowBrightnesses[r - rect.topRow] >= rowBrightnessPivot);
		int white = 0;
		int top = -1;
		for (int r = rect.topRow; r < rect.bottomRow; r++) {
			if (whiteRows[r - rect.topRow]) {
				white++;
				if ((white >= minSplitMargin) && (top != -1)) {
					int tr = top + ((maxOffset < 0) ? 0 : maxOffset);
					int br = (r - white + 1) + ((maxOffset < 0) ? maxOffset : 0);
					if (tr < rect.topRow)
						tr = rect.topRow;
					if (br > rect.bottomRow)
						br = rect.bottomRow;
					
					//	this actually can happen with tiny chunks (e.g. horizontal lines) and big slopes ...
					if (br < tr)
						continue;
					
					ImagePartRectangle res = new ImagePartRectangle(rect.analysisImage);
					res.topRow = tr;
					res.bottomRow = br;
					res.leftCol = rect.leftCol;
					res.rightCol = rect.rightCol;
					res = narrowTopAndBottom(res);
					if (maxOffset == 0)
						res.splitClean = true;
					if (!res.isEmpty())
						rects.add(res);
					top = -1;
				}
			}
			else {
				if (top == -1)
					top = r;
				white = 0;
			}
		}
		
		if (top == rect.topRow) {
			ImagePartRectangle[] res = {rect};
			return res;
		}
		
		if (top != -1) {
			ImagePartRectangle res = new ImagePartRectangle(rect.analysisImage);
			int tr = top + ((maxOffset < 0) ? 0 : maxOffset);
			if (tr < rect.topRow)
				tr = rect.topRow;
			if (tr < rect.bottomRow) {
				res.topRow = tr;
				res.leftCol = rect.leftCol;
				res.bottomRow = rect.bottomRow;
				res.rightCol = rect.rightCol;
				res = narrowTopAndBottom(res);
				if (maxOffset == 0)
					res.splitClean = true;
				if (!res.isEmpty())
					rects.add(res);
			}
		}
		
		return ((ImagePartRectangle[]) rects.toArray(new ImagePartRectangle[rects.size()]));
	}
	
	/**
	 * Wrapper for an FFT peak.
	 * 
	 * @author sautter
	 */
	public static class Peak {
		public final int x;
		public final int y;
		public final double h;
		Peak(int x, int y, double h) {
			this.x = x;
			this.y = y;
			this.h = h;
		}
	}
	
	public static final int ADJUST_MODE_NONE = 0;
	public static final int ADJUST_MODE_SQUARE_ROOT = 1;
	public static final int ADJUST_MODE_LOG = 2;
	private static double adjust(double d, int mode) {
		if (mode == ADJUST_MODE_NONE)
			return d;
		else if (mode == ADJUST_MODE_SQUARE_ROOT)
			return Math.sqrt(d);
		else if (mode == ADJUST_MODE_LOG) {
			double a = Math.log(d);
			if (a < 0)
				return 0;
			return Math.pow(a, 3);
		}
		else return Math.sqrt(d);
	}
	
	private static final int fftCacheSize = 128;
	private static Map fftCache = Collections.synchronizedMap(new LinkedHashMap(128, 0.9f, true) {
		protected boolean removeEldestEntry(Entry eldest) {
			return (this.size() > fftCacheSize);
		}
	});
	
	/**
	 * Compute the FFT of an analysisImage. Having the analysisImage repeated computes the FFT
	 * of a plain parquetted with the argument analysisImage instead.
	 * @param image the image to use
	 * @param tdim the size of the FFT
	 * @param repeatImage repeat the analysisImage?
	 * @return the FFT of the argument analysisImage, sized tdim x tdim
	 */
	public static Complex[][] getFft(BufferedImage image, int tdim, boolean repeatImage) {
		return getFft(image, tdim, tdim, repeatImage);
	}
	
	/**
	 * Compute the FFT of an analysisImage. Having the analysisImage repeated computes the FFT
	 * of a plain parquetted with the argument analysisImage instead.
	 * @param image the image to use
	 * @param tdimx the width of the FFT
	 * @param tdimy the height of the FFT
	 * @param repeatImage repeat the analysisImage?
	 * @return the FFT of the argument analysisImage, sized tdim x tdim
	 */
	public static Complex[][] getFft(BufferedImage image, int tdimx, int tdimy, boolean repeatImage) {
		String cacheKey = (image.hashCode() + "-" + tdimx + "-" + tdimy + "-" + repeatImage);
		Complex[][] fft = ((Complex[][]) fftCache.get(cacheKey));
		if (fft != null)
			return fft;
		
		int iw = image.getWidth();
		int ih = image.getHeight();
		int agg = 1;
		agg = Math.max(agg, ((iw + tdimx - 1) / tdimx));
		agg = Math.max(agg, ((ih + tdimy - 1) / tdimy));
		
		Complex[][] ytrans = new Complex[tdimy][]; // first dimension is vertical here
		Complex[] row;
		int ix;
		int iy;
		float[] hsb = new float[3];
		float bs;
		int rgb;
		int wrgb = Color.WHITE.getRGB();
		for (int y = 0; y < tdimy; y++) {
			row = new Complex[tdimx];
			for (int x = 0; x < tdimx; x++) {
				ix = (x * agg);
				iy = (y * agg);
				bs = 0;
				for (int ax = 0; ax < agg; ax++) {
					for (int ay = 0; ay < agg; ay++) {
						if (repeatImage)
							rgb = image.getRGB(((ix + ax) % iw), ((iy + ay) % ih));
						else rgb = ((y < ih) ? image.getRGB(((ix + ax) % iw), y) : wrgb);
						hsb = new Color(rgb).getColorComponents(hsb);
						bs += hsb[2];
					}
				}
				row[x] = new Complex((bs / (agg * agg)), 0);
			}
			ytrans[y] = computeFft(row);
		}
		
		Complex[] col;
		fft = new Complex[tdimx][];
		for (int x = 0; x < tdimx; x++) {
			col = new Complex[tdimy];
			for (int y = 0; y < tdimy; y++)
				col[y] = ytrans[y][x];
			fft[x] = computeFft(col);
			if (x == 0)
				fft[0][0] = new Complex(0, 0); // exclude DC
		}
		
		fftCache.put(cacheKey, fft);
		return fft;
	}
	
	/**
	 * Compute the maximum peak of an FFT.
	 * @param fft the FFT
	 * @param adjustMode the peak adjustment mode
	 * @return the adjusted maximum peak height of the argument FFT
	 */
	public static double getMax(Complex[][] fft, int adjustMode) {
		double max = 0;
		for (int x = 0; x < fft.length; x++) {
			for (int y = 0; y < fft[x].length; y++)
				max = Math.max(max, adjust(fft[x][y].abs(), adjustMode));
		}
		return max;
	}
	
	/**
	 * Collect FFT peaks whose adjusted value exceeds a given threshold.
	 * @param fft the FFT result to work with
	 * @param peaks the collection to store the peaks in
	 * @param th the threshold
	 * @param adjustMode the value adjust mode (default is
	 *            ADJUST_MODE_SQUARE_ROOT)
	 */
	public static void collectPeaks(Complex[][] fft, Collection peaks, double th, int adjustMode) {
		double t;
		for (int x = 0; x < fft.length; x++) {
			for (int y = 0; y < fft[x].length; y++) {
				t = adjust(fft[x][y].abs(), adjustMode);
				if (t > th)
					peaks.add(new Peak(((x + fft.length/2) % fft.length), ((y + fft[x].length/2) % fft[x].length), t));
			}
		}
	}
	
	/**
	 * Correct page images who are scanned out of the vertical. This method
	 * first uses FFT peaks to compute the rotation against the vertical, then
	 * rotates the analysisImage back to the vertical if the deviation is more than the
	 * specified granularity.
	 * @param analysisImage the analysisImage to correct
	 * @param granularity the granularity in degrees
	 * @param adjustMode the FFT peak adjust mode
	 * @return the corrected analysisImage
	 */
	public static boolean correctPageRotation(AnalysisImage analysisImage, double granularity, int adjustMode) {
		return correctPageRotation(analysisImage, granularity, null, -1, adjustMode);
	}
	
	/**
	 * Correct page images who are scanned out of the vertical. This method
	 * first uses peaks in the argument FFT to compute the rotation against the
	 * vertical, then rotates the analysisImage back to the vertical if the deviation is
	 * more than the specified granularity. The argument FFT must originate from
	 * the argument analysisImage for the result of this method to be meaningful. If the
	 * argument FFT is null, this method computes it as a 256 by 256 complex
	 * array.
	 * @param analysisImage the analysisImage to correct
	 * @param granularity the granularity in degrees
	 * @param fft the FFT of the analysisImage (set to null to have it computed here)
	 * @param max the adjusted maximum peak height of the argument FFT (set to a
	 *            negative number to have it computed here)
	 * @param adjustMode the FFT peak adjust mode
	 * @return true if the analysisImage was corrected, false otherwise
	 */
	public static boolean correctPageRotation(AnalysisImage analysisImage, double granularity, Complex[][] fft, double max, int adjustMode) {
		if (fft == null) {
			fft = analysisImage.getFft();
			max = getMax(fft, adjustMode);
		}
		else if (max < 0)
			max = getMax(fft, adjustMode);
		ArrayList peaks = new ArrayList();
		collectPeaks(fft, peaks, max/4, adjustMode);
		
//		//	TODO keep this deactivated for export !!!
//		BufferedImage fftImage = getFftImage(fft, adjustMode);
//		try {
//			ImageIO.write(fftImage, "png", new File("E:/Testdaten/PdfExtract/FFT." + System.currentTimeMillis() + ".png"));
//		} catch (IOException ioe) {}
		
		double angle = getPageRotationAngle(peaks, 20, (180 * ((int) (1 / granularity))));
		System.out.println("Page rotation angle is " + (((float) ((int) ((180 / Math.PI) * angle * 100))) / 100) + "°");
		if (angle > maxPageRotationCorrectionAngle)
			return false;
		if (Math.abs(angle) > ((Math.PI / 180) * granularity)) {
			analysisImage.setImage(rotateImage(analysisImage.getImage(), -angle));
			return true;
		}
		else return false;
	}
//	private static final double maxPageRotationAngle = ((Math.PI / 180) * 30); // 30°;
//	/* we have to use this limit, as in quite a few page images background
//	 * stains create patterns that create an angle around 90°, causing the page
//	 * image to be flipped by this angle, which we have to prevent */
	private static final double maxPageRotationCorrectionAngle = ((Math.PI / 180) * 12); // 12°
	/* we have to use a limit below 90° because in quite a few page images
	 * background stains or table columns create patterns that create an angle
	 * around 90°, causing the page image to be flipped by this angle, which we
	 * have to prevent; further, we have to stay below the italics angle of of
	 * common fonts because in sparsely populated pages, italics can result in
	 * peaks at their skew angle */
	private static final double maxPageRotationMeasurementAngle = ((Math.PI / 180) * 30); // 30°
	/* we need this extra threshold so table grids, etc. that might look like a
	 * 90° rotation do not hamper detecting an actual rotation angle that is way
	 * lower */
	
	private static double getPageRotationAngle(List peaks, int maxPeaks, int numBucks) {
		ArrayList aPeaks = new ArrayList();
		Collections.sort(peaks, new Comparator() {
			public int compare(Object p1, Object p2) {
				return -Double.compare(((Peak) p1).h, ((Peak) p2).h);
			}
		});
		if (maxPeaks < 2)
			maxPeaks = peaks.size();
		for (int p = 0; p < (Math.min(peaks.size(), maxPeaks)); p++)
			aPeaks.add(peaks.get(p));
		
		double[] aWeights = new double[numBucks];
		Arrays.fill(aWeights, 0);
		int[] aCounts = new int[aWeights.length];
		Arrays.fill(aCounts, 0);
		for (int p1 = 0; p1 < aPeaks.size(); p1++) {
			Peak peak = ((Peak) aPeaks.get(p1));
			for (int p2 = (p1+1); p2 < aPeaks.size(); p2++) {
				Peak cPeak = ((Peak) aPeaks.get(p2));
				double distx = peak.x - cPeak.x;
				double disty = peak.y - cPeak.y;
				int dist = ((int) Math.sqrt((distx * distx) + (disty * disty)));
				if (dist == 0)
					continue;
				double a = ((disty == 0) ? (Math.PI / 2) : Math.atan(distx / -disty));
				if (Math.abs(a) >= maxPageRotationMeasurementAngle)
					continue;
				int aBuck = ((int) ((a * aWeights.length) / Math.PI)) + (aWeights.length / 2) - 1;
				aWeights[aBuck] += (dist * peak.h * cPeak.h);
				aCounts[aBuck]++;
			}
		}
		
		double aWeightMax = 0;
		double aWeightMax2 = 0;
		double angle = 0;
		for (int a = 0; a < aWeights.length; a++) {
			if (aWeights[a] > aWeightMax) {
				angle = ((Math.PI * (a - aWeights.length/2 + 1)) / aWeights.length);
				aWeightMax2 = aWeightMax;
				aWeightMax = aWeights[a];
			}
			else if (aWeights[a] > aWeightMax2)
				aWeightMax2 = aWeights[a];
		}
		return angle;
	}
	
	private static BufferedImage rotateImage(BufferedImage image, double angle) {
		BufferedImage rImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D rImageGraphics = rImage.createGraphics();
		Color originalColor = rImageGraphics.getColor();
		rImageGraphics.setColor(Color.WHITE);
		rImageGraphics.fillRect(0, 0, rImage.getWidth(), rImage.getHeight());
		rImageGraphics.setColor(originalColor);
		AffineTransform originalTransform = rImageGraphics.getTransform();
		rImageGraphics.rotate((angle / 2), (image.getWidth()/2), (image.getHeight()/2));
//		rImageGraphics.rotate((angle / 1), (image.getWidth()/2), (image.getHeight()/2));
		rImageGraphics.drawRenderedImage(image, null);
		rImageGraphics.setTransform(originalTransform);
		return rImage;
	}
	
	/**
	 * Compute the baseline of a set of words. This method finds the row with
	 * the strongest negative difference in brightness to the row immediately
	 * below it, interpreting it as the lower rim of the letters, save for
	 * descends.
	 * @param words the words to analyze
	 * @param ai the image the words are on
	 * @return the baseline of the argument words
	 */
	public static int findBaseline(AnalysisImage ai, ImagePartRectangle[] words) {
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
			rowBrightnessDrops[r] = ((byte) (rowBrightnesses[r] - (((r+1) == height) ? 1 : rowBrightnesses[r+1])));
		byte maxRowBrightnessDrop = 0;
		int maxDropRow = -1;
		for (int r = (height-1); r > (height / 2); r--) {
			if (rowBrightnessDrops[r] < maxRowBrightnessDrop) {
				maxRowBrightnessDrop = rowBrightnessDrops[r];
				maxDropRow = r;
			}
		}
		
		return (top + maxDropRow);
	}
	
	/**
	 * Compute the FFT of complex vector, assuming its length is a power of 2.
	 * @param x the complex vector to transform
	 * @return the Fourier transform of the argument vector
	 */
	public static Complex[] computeFft(Complex[] x) {
		int N = x.length;

		// base case
		if (N == 1)
			return new Complex[] {x[0]};

		// radix 2 Cooley-Tukey FFT
		if (N % 2 != 0)
			throw new RuntimeException("N is not a power of 2");

		// fft of even terms
		Complex[] even = new Complex[N/2];
		for (int k = 0; k < N/2; k++)
			even[k] = x[2*k];
		Complex[] q = computeFft(even);

		// fft of odd terms
		Complex[] odd  = even;  // reuse the array
		for (int k = 0; k < N/2; k++)
			odd[k] = x[2*k + 1];
		Complex[] r = computeFft(odd);

		// combine
		Complex[] y = new Complex[N];
		for (int k = 0; k < N/2; k++) {
			double kth = -2 * k * Math.PI / N;
			Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
			y[k]	   = q[k].plus(wk.times(r[k]));
			y[k + N/2] = q[k].minus(wk.times(r[k]));
		}
		return y;
	}
	
	/**
	 * Object representing a complex number, including basic functionality
	 * revolving around complex numbers.
	 * 
	 * @author sautter
	 */
	public static class Complex {
		/** the real part */
		public final double re;   // the real part
		/** the imaginary part */
		public final double im;   // the imaginary part

		// create a new object with the given real and imaginary parts
		/**
		 * Constructor
		 * @param real the real part
		 * @param imag the imaginary part
		 */
		public Complex(double real, double imag) {
			re = real;
			im = imag;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			if (im == 0) return re + "";
			if (re == 0) return im + "i";
			if (im <  0) return re + " - " + (-im) + "i";
			return re + " + " + im + "i";
		}

		// return abs/modulus/magnitude and angle/phase/argument
		public double abs() {
			return Math.hypot(re, im);
		}  // Math.sqrt(re*re + im*im)
		
		// return a new Complex object whose value is (this + b)
		public Complex plus(Complex b) {
			Complex a = this;			 // invoking object
			double real = a.re + b.re;
			double imag = a.im + b.im;
			return new Complex(real, imag);
		}

		// return a new Complex object whose value is (this - b)
		public Complex minus(Complex b) {
			Complex a = this;
			double real = a.re - b.re;
			double imag = a.im - b.im;
			return new Complex(real, imag);
		}

		// return a new Complex object whose value is (this * b)
		public Complex times(Complex b) {
			Complex a = this;
			double real = a.re * b.re - a.im * b.im;
			double imag = a.re * b.im + a.im * b.re;
			return new Complex(real, imag);
		}
	}
	
	/**
	 * Render an image for visualizing an FFT result.
	 * @param fft the FFT result
	 * @param adjustMode the peak adjustment mode
	 * @return an image visualizing the FFT
	 */
	public static BufferedImage getFftImage(Complex[][] fft, int adjustMode) {
		double max = getMax(fft, adjustMode);
		float b;
		double t;
		int rgb;
		int rrgb = Color.RED.getRGB();
		double rmin = max/2;
		int yrgb = Color.YELLOW.getRGB();
		double ymin = max/4;
//		int grgb = Color.GREEN.getRGB();
//		double gmin = max/8;
		BufferedImage tImage = new BufferedImage(fft.length, fft[0].length, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < fft.length; x++) {
			for (int y = 0; y < fft[x].length; y++) {
				t = adjust(fft[x][y].abs(), adjustMode);
				b = ((float) (t / max));
				if (t > rmin)
					rgb = rrgb;
				else if (t > ymin)
					rgb = yrgb;
//				else if (t > gmin) {
//					rgb = grgb;
//				}
				else {
					if (b > 1)
						b = 1;
					else if (b < 0)
						b = 0;
					rgb = Color.HSBtoRGB(0f, 0f, b);
				}
				tImage.setRGB(((x + fft.length/2) % fft.length), ((y + fft[x].length/2) % fft[x].length), rgb);
			}
		}
		return tImage;
	}
	
	/**
	 * Enhance the contrast of a gray-sclae image. This method uses a simple
	 * for of contrast limited adaptive histogram equalization.
	 * @param analysisImage the image to treat
	 * @param dpi the resolution of the image
	 * @return true if the image was modified, false otherwise
	 */
	public static boolean enhanceContrast(AnalysisImage analysisImage, int dpi, int ignoreThreshold) {
		//	TODO figure out if threshold makes sense
		
		//	get brightness array
		byte[][] brightness = analysisImage.getBrightness();
		if ((brightness.length == 0) || (brightness[0].length == 0))
			return false;
		
		//	compute number and offset of tiles
		int ts = (dpi / 10); // TODO play with denominator
		int htc = ((brightness.length + ts - 1) / ts);
		int hto = (((htc * ts) - brightness.length) / 2);
		int vtc = ((brightness[0].length + ts - 1) / ts);
		int vto = (((vtc * ts) - brightness[0].length) / 2);
		
		//	compute tiles
		ImageTile[][] tiles = new ImageTile[htc][vtc];
		for (int ht = 0; ht < tiles.length; ht++) {
			int tl = Math.max(((ht * ts) - hto), 0);
			int tr = Math.min((((ht+1) * ts) - hto), brightness.length);
			for (int vt = 0; vt < tiles[ht].length; vt++) {
				int tt = Math.max(((vt * ts) - vto), 0);
				int tb = Math.min((((vt+1) * ts) - vto), brightness[0].length);
				tiles[ht][vt] = new ImageTile(tl, tr, tt, tb);
			}
		}
		
		//	compute min and max brightness for each tile TODO consider using min and max quantile (e.g. 5%) instead of absolute min and max
		for (int ht = 0; ht < tiles.length; ht++)
			for (int vt = 0; vt < tiles[ht].length; vt++) {
				ImageTile tile = tiles[ht][vt];
				for (int c = tile.left; c < tile.right; c++)
					for (int r = tile.top; r < tile.bottom; r++) {
						if (ignoreThreshold <= brightness[c][r])
							continue;
						tile.minBrightness = ((byte) Math.min(tile.minBrightness, brightness[c][r]));
						tile.maxBrightness = ((byte) Math.max(tile.maxBrightness, brightness[c][r]));
					}
			}
		
		//	enhance contrast
		int radius = 2; // TODO play with radius
		for (int ht = 0; ht < tiles.length; ht++)
			for (int vt = 0; vt < tiles[ht].length; vt++) {
				ImageTile tile = tiles[ht][vt];
				
				//	compute min and max brightness for current tile and its neighborhood
				byte minBrightness = tile.minBrightness;
				byte maxBrightness = tile.maxBrightness;
				for (int htl = Math.max(0, (ht-radius)); htl <= Math.min((htc-1), (ht+radius)); htl++)
					for (int vtl = Math.max(0, (vt-radius)); vtl <= Math.min((vtc-1), (vt+radius)); vtl++) {
						minBrightness = ((byte) Math.min(minBrightness, tiles[htl][vtl].minBrightness));
						maxBrightness = ((byte) Math.max(maxBrightness, tiles[htl][vtl].maxBrightness));
					}
				
				//	nothing we can do about this one ...
				if (maxBrightness <= minBrightness)
					continue;
				
				//	avoid over-amplification of noise TODO play with threshold
				int minBrightnessDist = 48;
				if ((maxBrightness - minBrightness) < minBrightnessDist) {
					System.out.println("Limiting weak contrast " + minBrightness + "-" + maxBrightness);
					int remainingBrightnessDist = (minBrightnessDist - (maxBrightness - minBrightness));
					int darkeningMinBrightnessDist = ((remainingBrightnessDist * minBrightness) / (127 - (maxBrightness - minBrightness)));
					int lighteningMaxBrighnessDist = (remainingBrightnessDist - darkeningMinBrightnessDist);
					minBrightness = ((byte) (minBrightness - darkeningMinBrightnessDist));
					maxBrightness = ((byte) (maxBrightness + lighteningMaxBrighnessDist));
					System.out.println(" ==> " + minBrightness + "-" + maxBrightness);
				}
				
				//	adjust image
				for (int c = tile.left; c < tile.right; c++)
					for (int r = tile.top; r < tile.bottom; r++) {
						int b = (((brightness[c][r] - minBrightness) * 127) / (maxBrightness - minBrightness));
						if (127 < b)
							b = 127;
						else if (b < 0)
							b = 0;
						brightness[c][r] = ((byte) b);
						analysisImage.image.setRGB(c, r, Color.HSBtoRGB(0, 0, (((float) brightness[c][r]) / 127)));
					}
			}
		
		//	finally ...
		return true;
	}
	private static class ImageTile {
		final int left;
		final int right;
		final int top;
		final int bottom;
		byte minBrightness = ((byte) 127);
		byte maxBrightness = ((byte) 0);
		ImageTile(int left, int right, int top, int bottom) {
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
		}
	}
	
	private static class ImageData {
		String fileName;
		int dpi;
		ImageData(String fileName, int dpi) {
			this.fileName = fileName;
			this.dpi = dpi;
		}
	}
	/* TODO use region coloring for block detection
	 * - measure distance to next non-white pixel for all white pixels
	 * - use as boundary for region coloring if distance above some (DPI fraction based) threshold
	 * - regions are blocks, no matter of shape
	 * - use size to surface comparison to get rid of lines, etc.
	 * 
	 * PUT THIS IN PageImageAnalysis WHEN DONE
	 */
	
	private static ImageData[] testImages = {
		new ImageData("BEC_1890.pdf.0.png", 200),
		new ImageData("torminosus1.pdf.0.png", 150),
		new ImageData("Mellert_et_al_2000.pdf.2.png", 600),
		new ImageData("Mellert_et_al_2000.pdf.3.png", 600),
		new ImageData("AcaxanthumTullyandRazin1970.pdf.1.png", 600),
		new ImageData("AcaxanthumTullyandRazin1970.pdf.2.png", 600),
		new ImageData("Darwiniana_V19_p553.pdf.0.png", 150),
		new ImageData("Schulz_1997.pdf.5.png", 400),
		new ImageData("23416.pdf.207.png", 300),
		new ImageData("Forsslund1964.pdf.3.png", 300),
		new ImageData("5834.pdf.2.png", 300),
		new ImageData("AcaxanthumTullyandRazin1970.pdf.4.png", 600),
		new ImageData("015798000128826.pdf.2.png", 200),
		new ImageData("Forsslund1964.pdf.1.png", 300),
		new ImageData("Menke_Cerat_1.pdf.12.png", 300),
		new ImageData("fm__1_4_295_6.pdf.0.png", 300),
		new ImageData("fm__1_4_295_6.pdf.1.png", 300),
		new ImageData("fm__1_4_295_6.pdf.2.png", 300),
		new ImageData("Ceratolejeunea_Lejeuneaceae.mono.pdf.0.png", 200),
		new ImageData("chenopodiacea_hegi_270_281.pdf.6.png", 300),
		new ImageData("Prasse_1979_1.pdf.6.png", 300),
		new ImageData("Prasse_1979_1.pdf.9.png", 300),
		new ImageData("Prasse_1979_1.pdf.12.png", 300),
		new ImageData("Prasse_1979_1.pdf.13.png", 300),
	};
	
	/* test PDFs and images: TODO think of further odd cases
	 * - black and white, pages strewn with tiny spots: BEC_1890.pdf.0.png (200 dpi)
	 * - skewed page, somewhat uneven ink distribution: torminosus1.pdf.0.png (150 dpi)
	 * - tables with dotted grid lines, highlighted (darker) cells: Mellert_et_al_2000.pdf.2.png, Mellert_et_al_2000.pdf.3.png (also dirty, as scanned from copy) ==> VERY HARD CASE
	 * - two columns floating around image embedded in mid page: TODO
	 * - change between one and two columns: AcaxanthumTullyandRazin1970.pdf.1.png, AcaxanthumTullyandRazin1970.pdf.2.png
	 * - back frame around page: Darwiniana_V19_p553.pdf.0.png
	 * - black frame around two sides of page, punch holes: Schulz_1997.pdf.5.png
	 * - pencil stroke or fold through mid page: 23416.pdf.207.png
	 * - pencil stroke in text: Forsslund1964.pdf.3.png
	 * - two-columnish drawing with two-column caption: 5834.pdf.2.png
	 * - extremely small font: AcaxanthumTullyandRazin1970.pdf.4.png
	 * - uneven background, bleed-through: 015798000128826.pdf.2.png, Forsslund1964.pdf.1.png
	 * - uneven background with page fold at edge: Menke_Cerat_1.pdf.12.png
	 * - dark, uneven background, disturbed edges: fm__1_4_295_6.pdf.0.png, fm__1_4_295_6.pdf.1.png, fm__1_4_295_6.pdf.2.png
	 * - inverted images: ants_02732.pdf
	 * - wide dark edges, half of neighbor page: Ceratolejeunea_ Lejeuneaceae.mono.pdf.0.png
	 * - light figure, hard to distinguish from text: chenopodiacea_hegi_270_281.pdf.6.png
	 * 
	 * - extremely bad print quality (stenciled): Prasse_1979_1.pdf.6.png
	 * - extremely bad print quality (stenciled), grid-less tables: Prasse_1979_1.pdf.9.png
	 * - extremely bad print quality (stenciled), grid-less tables, folding lines: Prasse_1979_1.pdf.12.png, Prasse_1979_1.pdf.13.png
	 * 
	 * - change from one column to two and back: FOC-Loranthaceae.pdf.7.png (BORN-DIGITAL, MORE FOR STRUCTURING)
	 * - three-column, narrow gaps: Sansone-2012-44-121.pdf (BORN-DIGITAL, MORE FOR STRUCTURING)
	 * - two-column layout, three-column figure, grid-less tables: PlatnickShadab1979b.pdf.13.png (MORE FOR STRUCTURING)
	 * - multi-page grid-less table: SCZ634_Cairns_web_FINAL.pdf (pp 45-47, BORN-DIGITAL, MORE FOR STRUCTURING)
	 * - extremely text-like heading, grid-less table, partially Chinese: ZhangChen1994.pdf.2.png (MORE FOR STRUCTURING)
	 * - mix of one- and two-column layout, embedded footnotes, dividing lines: Wallace's line.pdf (BORN-DIGITAL, MORE FOR STRUCTURING)
	 */
	
	/* TODO try and _analyze_ page images first:
	 * - long horizontal and vertical lines
	 *   ==> TODO implement generalized pattern detection
	 */
	
	//	TODO compute that for all selected images, and put in Excel sheet for overview
	//	TODO plot some clean images against analysis results as basis for classification
	//	TODO store analysis results in AnalysisImage class for reuse
	
	private static final int normDpi = 300;
	private static StringTupel generateStats(String imageFileName, int dpi) {
		System.out.println("Generating stats for " + imageFileName);
		
		//	start stats
		StringTupel st = new StringTupel();
		st.setValue("imageName", imageFileName);
		st.setValue("imageDpi", ("" + dpi));
		
		//	load image
		BufferedImage bi;
		try {
			bi = ImageIO.read(new File("E:/Testdaten/PdfExtract", imageFileName));
		}
		catch (IOException ioe) {
			System.out.println("Error loading image '" + imageFileName + "':" + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return st;
		}
		
		//	scale and adjust image is necessary
		if ((bi.getType() != BufferedImage.TYPE_BYTE_GRAY) || (dpi > normDpi)) {
			int scaleFactor = Math.max(1, ((dpi + normDpi - 1) / normDpi));
			BufferedImage abi = new BufferedImage((bi.getWidth() / scaleFactor), (bi.getHeight() / scaleFactor), BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D ag = abi.createGraphics();
			ag.drawImage(bi, 0, 0, abi.getWidth(), abi.getHeight(), null);
			bi = abi;
			dpi /= scaleFactor;
		}
		st.setValue("analysisDpi", ("" + dpi));
		st.setValue("analysisSize", ("" + (bi.getWidth() * bi.getHeight())));
		
		//	wrap image
		AnalysisImage ai = wrapImage(bi, null);
		
		//	do white balance
		whitenWhite(ai);
		
		//	eliminate background
		//eliminateBackground(ai, dpi);
		//whitenWhite(ai);
		
		//	apply mild overall blur (about one third of a millimeter)
		//gaussBlur(ai, (dpi / 67));
		//whitenWhite(ai);
		
		//	apply mild horizontal blur (about half a millimeter)
		//gaussBlur(ai, (dpi / 50), 0);
		//whitenWhite(ai);
		
		//	apply strong horizontal blur (about five millimeters)
		//gaussBlur(ai, (dpi / 5), 0);
		//whitenWhite(ai);
		
		//	compute brightness distribution
		for (int bb = 8; bb <= 32; bb*=2) {
			int[] brightnessDist = getBrightnessDistribution(ai, bb);
			for (int b = 0; b < brightnessDist.length; b++)
				st.setValue(("brightnessDist" + bb + "-" + b), ("" + brightnessDist[b]));
		}
//		
//		//	compute region size distribution
//		int dpiFactor = dpi;
//		int normDpiFactor = normDpi;
//		for (int f = 2; (f < dpiFactor) && (f < normDpiFactor); f++)
//			while (((dpiFactor % f) == 0) && ((normDpiFactor % f) == 0)) {
//				dpiFactor /= f;
//				normDpiFactor /= f;
//			}
//		System.out.println("- DPI based region size norm factor is (" + dpiFactor + "/" + normDpiFactor + ")²");
//		int maxRegionSizeBucketCount = 16;
//		for (int bt = 1; bt <= 1/*16*/; bt*= 4)
//			for (int id = 0; id < 2; id++) {
//				System.out.println("- doing " + ((id == 1) ? "diagonal" : "straight") + " coloring with threshold " + (128 - bt));
//				int[][] regionCodes = getRegionColoring(ai, ((byte) (128 - bt)), (id == 1));
//				int regionCodeCount = 0;
//				for (int c = 0; c < regionCodes.length; c++) {
//					for (int r = 0; r < regionCodes[c].length; r++)
//						regionCodeCount = Math.max(regionCodeCount, regionCodes[c][r]);
//				}
//				regionCodeCount++; // account for 0
//				int[] regionSizes = new int[regionCodeCount];
//				Arrays.fill(regionSizes, 0);
//				for (int c = 0; c < regionCodes.length; c++) {
//					for (int r = 0; r < regionCodes[c].length; r++)
//						regionSizes[regionCodes[c][r]]++;
//				}
//				int regionSizeBucketCount = 1;
//				for (int rs = 1; rs < (regionCodes.length * regionCodes[0].length); rs*=2)
//					regionSizeBucketCount++;
//				regionSizeBucketCount = Math.min(maxRegionSizeBucketCount, regionSizeBucketCount);
//				int[] regionSizeBuckets = new int[regionSizeBucketCount];
//				int[] regionSizeBucketThresholds = new int[regionSizeBucketCount];
//				regionSizeBuckets[0] = 0;
//				regionSizeBucketThresholds[0] = 1;
//				for (int b = 1; b < regionSizeBuckets.length; b++) {
//					regionSizeBuckets[b] = 0;
//					regionSizeBucketThresholds[b] = (regionSizeBucketThresholds[b-1] * 2);
//				}
//				while ((regionSizeBucketCount == maxRegionSizeBucketCount) && (regionSizeBucketThresholds[maxRegionSizeBucketCount-1] < (regionCodes.length * regionCodes[0].length)))
//					regionSizeBucketThresholds[maxRegionSizeBucketCount-1] *= 2;
//				for (int r = 0; r < regionSizes.length; r++)
//					for (int b = 0; b < regionSizeBuckets.length; b++) {
//						int nRegionSize = ((regionSizes[r] * dpiFactor * dpiFactor) / (normDpiFactor * normDpiFactor));
//						if (nRegionSize <= regionSizeBucketThresholds[b]) {
//							regionSizeBuckets[b]++;
//							break;
//						}
//					}
//				for (int b = 0; b < regionSizeBuckets.length; b++)
//					st.setValue(("regionSizeDist" + ((id == 1) ? "D" : "S") + (128 - bt) + "-" + b), ("" + regionSizeBuckets[b]));
//				for (int b = regionSizeBuckets.length; b < maxRegionSizeBucketCount; b++)
//					st.setValue(("regionSizeDist" + ((id == 1) ? "D" : "S") + (128 - bt) + "-" + b), "0");
//			}
		
		//	finally ...
		return st;
	}
	
	//	FOR TESTS ONLY !!!
	public static void main(String[] args) throws Exception {
//		if (true) {
//			StringRelation stats = new StringRelation();
//			for (int i = 0; i < testImages.length; i++) {
//				StringTupel st = generateStats(testImages[i].fileName, testImages[i].dpi);
//				stats.addElement(st);
//			}
//			File statsFile = new File("E:/Testdaten/PdfExtract", "TestImageStats.csv");
//			BufferedWriter statsBw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(statsFile)));
//			StringRelation.writeCsvData(statsBw, stats, ';', '"', true);
//			statsBw.flush();
//			statsBw.close();
//			return;
//		}
//		
		String imageFileName;
		int dpi;
		int scaleFactor = 1;
		
		//	feather dusting
//		imageFileName = "BEC_1890.pdf.0.png"; dpi = 200;
		
		//	background elimination
//		imageFileName = "Mellert_et_al_2000.pdf.2.png"; dpi = 600;
//		imageFileName = "Mellert_et_al_2000.pdf.3.png"; dpi = 600;
//		imageFileName = "fm__1_4_295_6.pdf.2.png"; dpi = 300;
//		imageFileName = "BEC_1890.pdf.0.png"; dpi = 200;
//		imageFileName = "Prasse_1979_1.pdf.6.png"; dpi = 300;
//		imageFileName = "Prasse_1979_1.pdf.9.png"; dpi = 300;
		imageFileName = "Prasse_1979_1.pdf.12.png"; dpi = 300;
//		imageFileName = "Prasse_1979_1.pdf.13.png"; dpi = 300;
		
		//	page skew correction
//		imageFileName = "torminosus1.pdf.0.png"; dpi = 150;
		
		//	dotted table grid
//		imageFileName = "Mellert_et_al_2000.pdf.2.png"; dpi = 400;
		
		//	distance coloring
//		imageFileName = "Mellert_et_al_2000.pdf.2.png"; dpi = 600;
//		imageFileName = "torminosus1.pdf.0.png"; dpi = 150;
//		imageFileName = "FOC-Loranthaceae.pdf.7.png"; dpi = 200;
//		imageFileName = "fm__1_4_295_6.pdf.0.png"; dpi = 200;
		
		//	load and wrap image
		BufferedImage bi = ImageIO.read(new File("E:/Testdaten/PdfExtract", imageFileName));
		LinkedList beforeImages = new LinkedList();
		if ((bi.getType() != BufferedImage.TYPE_BYTE_GRAY) || (dpi > 300)) {
			scaleFactor = Math.max(1, ((dpi + 299) / 300));
			BufferedImage abi = new BufferedImage((bi.getWidth() / scaleFactor), (bi.getHeight() / scaleFactor), BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D ag = abi.createGraphics();
			ag.drawImage(bi, 0, 0, abi.getWidth(), abi.getHeight(), null);
			bi = abi;
			dpi /= scaleFactor;
		}
		BufferedImage obi = cloneImage(bi);
		AnalysisImage ai = wrapImage(bi, null);
		BufferedImage obirc = getRegionImage(ai, 127);
		
		//	TODO run test
		//featherDust(ai, dpi, true, true);
		//correctPageRotation(ai, 0.02, ADJUST_MODE_LOG); // doesn't do anything for small skews
		//gaussBlur(ai, 2);
		//whitenWhite(ai);
//		gaussBlur(ai, (dpi / 10));
		
//		//	subtract background from foreground
//		BufferedImage bebi = cloneImage(obi);
//		AnalysisImage beai = wrapImage(bebi, null);
//		byte[][] bBrightness = ai.getBrightness();
//		byte[][] beBrightness = beai.getBrightness();
//		for (int c = 0; c < beBrightness.length; c++)
//			for (int r = 0; r < beBrightness[c].length; r++) {
//				int b = 127 - ((127 - beBrightness[c][r]) - (127 - bBrightness[c][r]));
//				if (b < 0)
//					b = 0;
//				else if (b > 127)
//					b = 127;
//				beai.brightness[c][r] = ((byte) b);
//				if (beai.brightness[c][r] == 127)
//					beai.image.setRGB(c, r, white);
//			}
//		whitenWhite(beai);
//		gaussBlur(beai, 2);
//		whitenWhite(beai);
//		
//		eliminateBackground(ai, dpi);
//		whitenWhite(ai);
//		BufferedImage bebi = cloneImage(bi);
//		AnalysisImage beai = wrapImage(bebi, null);
//		//correctImage(ai, dpi, null);
//		for (int i = 0; i < 10; i++) {
//			gaussBlur(ai, (dpi / 67), 0);
//			whitenWhite(ai);
//		}
//		BufferedImage hbi = bi;
//		bi = cloneImage(bebi);
//		ai = wrapImage(bi, null);
//		for (int i = 0; i < 10; i++) {
//			gaussBlur(ai, 0, (dpi / 67));
//			whitenWhite(ai);
//		}
//		BufferedImage vbi = bi;
//		bi = cloneImage(bebi);
//		ai = wrapImage(bi, null);
//		
//		AnalysisImage hai = wrapImage(hbi, null);
//		gaussBlur(hai, (dpi / 50));
//		AnalysisImage vai = wrapImage(vbi, null);
//		gaussBlur(vai, (dpi / 50));
//		
//		byte[][] beBrightness = beai.getBrightness();
//		byte[][] hBrightness = hai.getBrightness();
//		byte[][] vBrightness = vai.getBrightness();
//		
//		BufferedImage rbi = cloneImage(bebi);
//		for (int c = 0; c < beBrightness.length; c++)
//			for (int r = 0; r < beBrightness[c].length; r++) {
//				int rCount = 0;
//				if (beBrightness[c][r] < 127)
//					rCount++;
//				if (hBrightness[c][r] < 127)
//					rCount++;
//				if (vBrightness[c][r] < 127)
//					rCount++;
//				if (rCount < 3 )
//					rbi.setRGB(c, r, white);
//			}
		
		/* THID LOOKS GOOD FOR ISOLATING TABLE GRIDS
		gaussBlur(ai, (dpi / 50), (dpi / 100));
		whitenWhite(ai);
		regionColorAndClean(ai, dpi, (dpi * 2), dpi, true, false);
		*/
//		
//		int[] rgbs = new int[128];
//		for (int i = 0; i < rgbs.length; i++) {
//			Color c = new Color((i*2), (i*2), (i*2));
//			rgbs[i] = c.getRGB();
//		}
//		byte[][] nonWhiteDistances = getBrightnessDistances(ai, ((byte) 127), ((byte) (dpi / 12)));
//		for (int c = 0; c < nonWhiteDistances.length; c++)
//			for (int r = 0; r < nonWhiteDistances[c].length; r++) {
//				if (nonWhiteDistances[c][r] != 0)
//					bi.setRGB(c, r, rgbs[nonWhiteDistances[c][r]]);
//			}
		
		//	eliminate background
		eliminateBackground(ai, dpi);
		BufferedImage bebi = cloneImage(bi);
		BufferedImage bebirc = getRegionImage(ai, 127);
		
		//	whiten remainder
		whitenWhite(ai);
		BufferedImage wbi = cloneImage(bi);
		BufferedImage wbirc = getRegionImage(ai, 127);
		
		//	enhance contrast
		enhanceContrast(ai, dpi, 128);
		BufferedImage cbi = cloneImage(bi);
		BufferedImage cbirc = getRegionImage(ai, 127);
		
		//	smooth image
		gaussBlur(ai, (dpi / 36), (dpi / 144), true);
		BufferedImage bbi = cloneImage(bi);
		BufferedImage bbirc = getRegionImage(ai, 127);
		
		//	make each region as dark as its darkest spot
		int[][] regionCodes = getRegionColoring(ai, ((byte) 127), true);
		int maxRegionCode = 0;
		for (int c = 0; c < regionCodes.length; c++) {
			for (int r = 0; r < regionCodes[c].length; r++)
				maxRegionCode = Math.max(maxRegionCode, regionCodes[c][r]);
		}
		byte[] minRegionBrightness = new byte[maxRegionCode];
		Arrays.fill(minRegionBrightness, ((byte) 127));
		byte[][] brightness = ai.getBrightness();
		for (int c = 0; c < regionCodes.length; c++)
			for (int r = 0; r < regionCodes[c].length; r++) {
				if (regionCodes[c][r] != 0)
					minRegionBrightness[regionCodes[c][r]-1] = ((byte) Math.min(minRegionBrightness[regionCodes[c][r]-1], brightness[c][r]));
			}
		for (int c = 0; c < regionCodes.length; c++) {
			for (int r = 0; r < regionCodes[c].length; r++)
				if (regionCodes[c][r] != 0) {
					brightness[c][r] = minRegionBrightness[regionCodes[c][r]-1];
					ai.image.setRGB(c, r, Color.HSBtoRGB(0, 0, (((float) brightness[c][r]) / 127)));
				}
			}
		
		//	eliminate light regions
		whitenWhite(ai);
		BufferedImage dbi = cloneImage(bi);
		BufferedImage dbirc = getRegionImage(ai, 127);
		
		//	AND-combine result image with original image
		brightness = ai.getBrightness();
		for (int c = 0; c < brightness.length; c++) {
			for (int r = 0; r < brightness[c].length; r++)
//				ai.image.setRGB(c, r, ((brightness[c][r] == 127) ? white : obi.getRGB(c, r)));
				ai.image.setRGB(c, r, ((brightness[c][r] == 127) ? white : bebi.getRGB(c, r)));
		}
		ai.brightness = null;
		gaussBlur(ai, 1, true);
		BufferedImage acbi = cloneImage(bi);
		BufferedImage acbirc = getRegionImage(ai, 127);
		
		//	enhance contrast
		//enhanceContrast(ai, dpi, 120);
		//	contrast enhancement seems to do more harm than good
		//	==> TODO think of something else to make letters darker, but only tellers
		
		BufferedImage birc = getRegionImage(ai, 127);
//		gaussBlur(ai, (dpi / 20), (dpi / 25), true); // for region coloring, we have to blur with 1/4 the designed minimum distance (diameter of blur x2), and 2/3 of that to counter the x3 enlarged kernel radius
//		int[][] regionCodes = getRegionColoring(ai, ((byte) 127), true);
//		int maxRegionCode = 0;
//		for (int c = 0; c < regionCodes.length; c++) {
//			for (int r = 0; r < regionCodes[c].length; r++)
//				maxRegionCode = Math.max(maxRegionCode, regionCodes[c][r]);
//		}
//		System.out.println("Got " + maxRegionCode + " regions");
//		int[] rgbs = new int[maxRegionCode];
//		for (int c = 0; c < rgbs.length; c++)
//			rgbs[c] = Color.HSBtoRGB((((float) c) / rgbs.length), 1.0f, 0.5f);
//		BufferedImage rcbi = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
//		for (int c = 0; c < regionCodes.length; c++)
//			for (int r = 0; r < regionCodes[c].length; r++) {
//				if (regionCodes[c][r] == 0)
//					rcbi.setRGB(c, r, white);
//				else rcbi.setRGB(c, r, rgbs[regionCodes[c][r]-1]);
//			}
//		
//		//	display result
//		ImageDisplayDialog idd = new ImageDisplayDialog("Test result for " + imageFileName);
////		int biCount = beforeImages.size();
////		while (beforeImages.size() != 0)
////			idd.addImage(((BufferedImage) beforeImages.removeFirst()), ("Before " + (biCount - beforeImages.size())));
//		idd.addImage(obi, "Before");
//		idd.addImage(obirc, "Before Regions");
//		idd.addImage(bebi, "Background Gone");
//		idd.addImage(bebirc, "Background Gone Regions");
//		idd.addImage(wbi, "Whitened");
//		idd.addImage(wbirc, "Whitened Regions");
//		idd.addImage(cbi, "Contrast Enhanced");
//		idd.addImage(cbirc, "Contrast Enhanced Regions");
////		idd.addImage(hbi, "Horizontal Blur");
////		idd.addImage(vbi, "Vertical Blur");
////		idd.addImage(rbi, "After");
//		idd.addImage(bbi, "Blurred");
//		idd.addImage(bbirc, "Blurred Regions");
//		idd.addImage(dbi, "Darkened");
//		idd.addImage(dbirc, "Darkened Regions");
////		idd.addImage(rcbi, "Regions");
//		idd.addImage(acbi, "Recombined");
//		idd.addImage(acbirc, "Recombined Regions");
//		idd.addImage(bi, "After");
//		idd.addImage(birc, "After Regions");
//		idd.setSize(Math.min(1600, bi.getWidth()), Math.min(1000, bi.getHeight()));
//		idd.setLocationRelativeTo(null);
//		idd.setVisible(true);
	}
	private static BufferedImage cloneImage(BufferedImage bi) {
		BufferedImage cbi = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D cg = cbi.createGraphics();
		cg.drawImage(bi, 0, 0, cbi.getWidth(), cbi.getHeight(), null);
		return cbi;
	}
	private static BufferedImage getRegionImage(AnalysisImage ai, int brightnessThreshold) {
		int[][] regionCodes = getRegionColoring(ai, ((byte) brightnessThreshold), true);
		int maxRegionCode = 0;
		for (int c = 0; c < regionCodes.length; c++) {
			for (int r = 0; r < regionCodes[c].length; r++)
				maxRegionCode = Math.max(maxRegionCode, regionCodes[c][r]);
		}
		System.out.println("Got " + maxRegionCode + " regions");
		int[] rgbs = new int[maxRegionCode];
		for (int c = 0; c < rgbs.length; c++)
			rgbs[c] = Color.HSBtoRGB((((float) c) / rgbs.length), 1.0f, 0.5f);
		shuffleArray(rgbs);
		BufferedImage rcbi = new BufferedImage(ai.image.getWidth(), ai.image.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int c = 0; c < regionCodes.length; c++)
			for (int r = 0; r < regionCodes[c].length; r++) {
				if (regionCodes[c][r] == 0)
					rcbi.setRGB(c, r, white);
				else rcbi.setRGB(c, r, rgbs[regionCodes[c][r]-1]);
			}
		return rcbi;
	}
	private static void shuffleArray(int[] ints) {
		for (int i = ints.length - 1; i > 0; i--) {
			int index = ((int) (Math.random() * ints.length));
			int a = ints[index];
			ints[index] = ints[i];
			ints[i] = a;
		}
	}
}
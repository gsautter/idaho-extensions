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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.imageio.ImageIO;

/**
 * Wrapper class for page images and meta data.
 * 
 * @author sautter
 */
public class PageImage implements ImagingConstants {
	
	/** the actual page image */
	public final BufferedImage image;
	
	/**
	 * the original width of the image before compression (used for scaling
	 * bounding boxes that refer to the original image)
	 */
	public final int originalWidth;
	
	/**
	 * the original height of the image before compression (used for scaling
	 * bounding boxes that refer to the original image)
	 */
	public final int originalHeight;
	
	/** the resolution of the original image before compression */
	public final int originalDpi;
	
	/** the resolution of the compressed image */
	public final int currentDpi;
	
	/** the width of the whitespace cut on the left edge of the image */
	public final int leftEdge;
	
	/** the width of the whitespace cut on the right edge of the image */
	public final int rightEdge;
	
	/** the height of the whitespace cut on the top edge of the image */
	public final int topEdge;
	
	/** the height of the whitespace cut on the bottom edge of the image */
	public final int bottomEdge;
	
	/** the page image source this image was loaded from (may be null if the page image was constructed by client code) */
	public final PageImageSource source;
	
	/**
	 * Constructor for creating a page image from an input stream after the
	 * meta data has been read
	 * @param piis page image input stream to read from
	 */
	public PageImage(PageImageInputStream piis) throws IOException {
		this.image = readImage(piis);
		this.originalWidth = piis.originalWidth;
		this.originalHeight = piis.originalHeight;
		this.originalDpi = piis.originalDpi;
		this.currentDpi = piis.currentDpi;
		this.leftEdge = piis.leftEdge;
		this.rightEdge = piis.rightEdge;
		this.topEdge = piis.topEdge;
		this.bottomEdge = piis.bottomEdge;
		this.source = piis.source;
	}
	
	/**
	 * Constructor
	 * @param image the actual page image
	 * @param originalWidth the original width of the image before
	 *            compression (used for scaling bounding boxes that refer to
	 *            the original image)
	 * @param originalHeight the original height of the image before
	 *            compression (used for scaling bounding boxes that refer to
	 *            the original image)
	 * @param originalDpi the resolution of the original image before
	 *            compression
	 * @param currentDpi the resolution of the compressed image
	 * @param leftEdge the width of the whitespace cut on the left edge of
	 *            the image
	 * @param rightEdge the width of the whitespace cut on the right edge of
	 *            the image
	 * @param topEdge the width of the whitespace cut on the top edge of the
	 *            image
	 * @param bottomEdge the width of the whitespace cut on the bottom edge
	 *            of the image
	 * @param source the page image source providing the image
	 */
	public PageImage(BufferedImage image, int originalWidth, int originalHeight, int originalDpi, int currentDpi, int leftEdge, int rightEdge, int topEdge, int bottomEdge, PageImageSource source) {
		this.image = image;
		this.originalWidth = originalWidth;
		this.originalHeight = originalHeight;
		this.originalDpi = originalDpi;
		this.currentDpi = currentDpi;
		this.leftEdge = leftEdge;
		this.rightEdge = rightEdge;
		this.topEdge = topEdge;
		this.bottomEdge = bottomEdge;
		this.source = source;
	}
	
	/**
	 * Constructor assuming zero width edges
	 * @param image the actual page image
	 * @param originalWidth the original width of the image before
	 *            compression (used for scaling bounding boxes that refer to
	 *            the original image)
	 * @param originalHeight the original height of the image before
	 *            compression (used for scaling bounding boxes that refer to
	 *            the original image)
	 * @param originalDpi the resolution of the original image before
	 *            compression
	 * @param currentDpi the resolution of the compressed image
	 * @param source the page image source providing the image
	 */
	public PageImage(BufferedImage image, int originalWidth, int originalHeight, int originalDpi, int currentDpi, PageImageSource source) {
		this(image, originalWidth, originalHeight, originalDpi, currentDpi, 0, 0, 0, 0, source);
	}
	
	/**
	 * Constructor assuming zero width edges and the actual image having its
	 * original size
	 * @param image the actual page image
	 * @param originalDpi the resolution of the image
	 * @param source the page image source providing the image
	 */
	public PageImage(BufferedImage image, int originalDpi, PageImageSource source) {
		this(image, image.getWidth(), image.getHeight(), originalDpi, originalDpi, 0, 0, 0, 0, source);
	}
	
	/**
	 * Retrieve a scaled copy of the page image. This method cannot enlarge
	 * a page image. This, if the argument DPI number is greater than or
	 * equal to the DPI number of this page image, this method does not
	 * create a scaled copy, but returns this page image.
	 * @param dpi the resolution of the scaled page image
	 * @return a scaled copy of this page image
	 * @throws IllegalArgumentException if dpi is less than or equal to
	 *             zero
	 */
	public PageImage scaleToDpi(int dpi) {
		
		//	check arguments
		if (dpi < 1)
			throw new IllegalArgumentException("DPI must be greater than zero.");
		
		//	determine scaling factor
		double scaleFactor = (((double) dpi) / this.currentDpi);
		
		//	do the scaling
		return this.scale(scaleFactor, dpi);
	}
	
	/**
	 * Retrieve a scaled copy of the page image. The returned image is
	 * scaled to fit within the specified dimensions. Its actual size may be
	 * smaller than the specified one along one axis, as the scaling avoids
	 * distortion. This method cannot enlarge a page image. Thus, if any one
	 * of the arguments is larger than the size of this image, this method
	 * does not create a scaled copy, but returns this page image. To fix
	 * only one dimension and leave determination of the scaling factor
	 * completely to the other one, specify 0 for the dimension to determin
	 * dynamically.
	 * @param width the maximum width of the returned image
	 * @param height the maximum height of the returned image
	 * @return a scaled copy of this page image
	 * @throws IllegalArgumentException if both arguments are less than or
	 *             equal to zero
	 */
	public PageImage scaleToSize(int width, int height) {
		
		//	check arguments
		if ((width < 1) && (height < 1))
			throw new IllegalArgumentException("at least one dimension must be greater than zero.");
		
		//	determine scaling factor
		double widthScale = ((width < 1) ? 1.0 : (((double) width) / this.image.getWidth()));
		double heightScale = ((height < 1) ? 1.0 : (((double) height) / this.image.getHeight()));
		double scaleFactor = Math.min(widthScale, heightScale);
		
		//	do the scaling
		return this.scale(scaleFactor, ((int) Math.round(this.currentDpi * scaleFactor)));
	}
	
	private PageImage scale(double scaleFactor, int scaledDpi) {
		
		//	check size
		if (scaleFactor >= 1.0)
			return this;
		
		//	scale image
		BufferedImage scaled = new BufferedImage(((int) (this.image.getWidth() * scaleFactor)), ((int) (this.image.getHeight() * scaleFactor)), BufferedImage.TYPE_INT_ARGB);
		Graphics g = scaled.getGraphics();
		g.drawImage(this.image, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
		g.dispose();
		
		//	wrap and return scaled image
		return new PageImage(scaled, this.originalWidth, this.originalHeight, this.originalDpi, scaledDpi, ((int) Math.round(this.leftEdge * scaleFactor)), ((int) Math.round(this.rightEdge * scaleFactor)), ((int) Math.round(this.topEdge * scaleFactor)), ((int) Math.round(this.bottomEdge * scaleFactor)), this.source);
	}
	
	/**
	 * Extract a part from the page image. If the isBbOriginal attribute is set
	 * to true, the argument bounding box is first scaled to this images current
	 * resolution.
	 * @param bb the bounding box describing the part
	 * @param isBbOriginal does the bounding box refer to the original image?
	 * @return the part image encircled by the argument bounding box
	 */
	public PageImage getSubImage(BoundingBox bb, boolean isBbOriginal) {
		
		//	scale bounding box if necessary
		if (isBbOriginal && (this.currentDpi != this.originalDpi))
			bb = new BoundingBox(((this.currentDpi * bb.left) / this.originalDpi), ((this.currentDpi * bb.right) / this.originalDpi), ((this.currentDpi * bb.top) / this.originalDpi), ((this.currentDpi * bb.bottom) / this.originalDpi));
		
		//	compute coordinates
		int x = (bb.left - this.leftEdge);
		int y = (bb.top - this.topEdge);
		int width = (bb.right - bb.left + 1);
		int height = (bb.bottom - bb.top + 1);
		
		//	correct coordinates
		if (x < 0) {
			width += x;
			x = 0;
		}
		if (y < 0) {
			height += y;
			y = 0;
		}
		if ((x + width) > this.image.getWidth())
			width = (this.image.getWidth() - x);
		if ((y + height) > this.image.getHeight())
			height = (this.image.getHeight() - y);
		
		//	extract the part
		return new PageImage(this.image.getSubimage(x, y, width, height),
				this.originalWidth,
				this.originalHeight,
				this.originalDpi,
				this.currentDpi,
				(this.leftEdge + x),
				(this.leftEdge + this.image.getWidth() + this.rightEdge - (x + width)),
				(this.topEdge + y),
				(this.topEdge + this.image.getHeight() + this.bottomEdge - (y + height)),
				this.source
			);
	}
	
	/**
	 * Compile an image of an annotation that runs over multiple pages. the
	 * argument bounding boxes have to refer to pages with subsequent IDs,
	 * starting with firstPageId. If there is nothing to extraxt from some page
	 * in the middle of the sequence, set the respective entry in the bounding
	 * box array to null. This method does not insert space between the part
	 * images.
	 * @param docId the ID of the document the parts belong to
	 * @param firstPageId the ID of the page the first bounding box refers to
	 * @param parts the bounding boxes marking the parts of page images to
	 *            conflate in the returned image
	 * @param areBbsOriginal do the bounding boxes refer to the original images?
	 * @param pis the page image source to retrieve the original page images
	 *            from
	 * @return an image compiled fromt the specified part images
	 */
	public static BufferedImage compileImage(String docId, int firstPageId, BoundingBox[] parts, boolean areBbsOriginal, PageImageSource pis) throws IOException {
		return compileImage(docId, firstPageId, parts, areBbsOriginal, 0, null, pis);
	}
	
	/**
	 * Compile an image of an annotation that runs over multiple pages. The
	 * argument bounding boxes have to refer to pages with subsequent IDs,
	 * starting with firstPageId. If there is nothing to extract from some page
	 * in the middle of the sequence, set the respective entry in the bounding
	 * box array to null.
	 * @param docId the ID of the document the parts belong to
	 * @param firstPageId the ID of the page the first bounding box refers to
	 * @param parts the bounding boxes marking the parts of page images to
	 *            conflate in the returned image
	 * @param areBbsOriginal do the bounding boxes refer to the original images?
	 * @param space the number of pixel rows to insert between the part images
	 * @param spaceColor the color for the pixel rows to insert between the part
	 *            images (if null, white will be used by default)
	 * @param pis the page image source to retrieve the original page images
	 *            from
	 * @return an image compiled from the specified part images
	 */
	public static BufferedImage compileImage(String docId, int firstPageId, BoundingBox[] parts, boolean areBbsOriginal, int space, Color spaceColor, PageImageSource pis) throws IOException {
		
		//	check parameters
		if (space < 0)
			space = 0;
		
		//	get required page images and collect parameters
		PageImage[] partImages = new PageImage[parts.length];
		int width = 0;
		int height = 0;
		for (int p = 0; p < parts.length; p++) {
			if (parts[p] == null)
				partImages[p] = null;
			else {
				PageImage pi = pis.getPageImage(docId, (firstPageId + p));
				partImages[p] = pi.getSubImage(parts[p], areBbsOriginal);
				width = Math.max(width, partImages[p].image.getWidth());
				height += (((p == 0) ? 0 : space) + partImages[p].image.getHeight());
			}
		}
		
		//	paste images together
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();
		int yOffset = 0;
		int spaceRgb = ((spaceColor == null) ? Color.WHITE : spaceColor).getRGB();
		for (int p = 0; p < partImages.length; p++) {
			if (partImages[p] == null)
				continue;
			if ((space != 0) && (p != 0))
				for (int s = 0; s < space; s++) {
					for (int x = 0; x < image.getWidth(); x++)
						image.setRGB(x, yOffset, spaceRgb);
					yOffset++;
				}
			g.drawImage(partImages[p].image, ((width - partImages[p].image.getWidth()) / 2), yOffset, partImages[p].image.getWidth(), partImages[p].image.getHeight(), null);
			yOffset += partImages[p].image.getHeight();
		}
		g.dispose();
		
		//	we're done
		return image;
	}
	
	/**
	 * Write this page image to some output stream. This method writes the
	 * meta data to the argument output stream, and then uses writeImage()
	 * to write the actual image. Thus, any data written by this method
	 * should be read by a PageImageInputStream.
	 * @param out the output stream to write to
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		
		//	send meta data
		writeInt(out, this.originalWidth, 2);
		writeInt(out, this.originalHeight, 2);
		writeInt(out, this.originalDpi, 2);
		writeInt(out, this.currentDpi, 2);
		writeInt(out, this.leftEdge, 2);
		writeInt(out, this.rightEdge, 2);
		writeInt(out, this.topEdge, 2);
		writeInt(out, this.bottomEdge, 2);
		
		//	send image
		this.writeImage(out);
	}
	private static final void writeInt(OutputStream out, int theInt, int numBytes) throws IOException {
		if (numBytes == 4)
			out.write((theInt >>> 24) & 255);
		if (numBytes >= 3)
			out.write((theInt >>> 16) & 255);
		if (numBytes >= 2)
			out.write((theInt >>> 8) & 255);
		out.write(theInt & 255);
	}
	
	/**
	 * Write the wrapped image to some output stream. This method uses
	 * ImageIO.write(), but as opposed to this latter method, does not close
	 * the argument stream before returning.
	 * @param out the output stream to write to
	 * @throws IOException
	 */
	public void writeImage(OutputStream out) throws IOException {
		ImageIO.write(this.image, IMAGE_FORMAT, new FilterOutputStream(out) {
			public void close() throws IOException {
				/*
				 * ImageIO closes the stream, but we cannot rely on
				 * this not to change in the future (it's bad
				 * style). So we ignore the close operation.
				 */
			}
		});
	}
	
	/**
	 * Read an image from an input stream. This method uses ImageIO.read()
	 * to read the actual image, but as opposed to this latter method, this
	 * method does not close the argument stream before returning.
	 * @param in the input stream to read from
	 * @return a page image restored form the data provided by the argument
	 *         input stream
	 * @throws IOException
	 */
	public static BufferedImage readImage(InputStream in) throws IOException {
		return ImageIO.read(new FilterInputStream(in) {
			public void close() throws IOException {
				/*
				 * ImageIO closes the stream, but we cannot rely on
				 * this not to change in the future (it's bad
				 * style). So we ignore the close operation.
				 */
			}
		});
	}
	
	private static ArrayList pageImageSources = new ArrayList(2);
	private static ArrayList pageImageStores = new ArrayList(2);
	
	/**
	 * Retrieve the page image sources currently installed
	 * @return the page image sources currently installed
	 */
	public static PageImageSource[] getPageImageSources() {
		return ((PageImageSource[]) pageImageSources.toArray(new PageImageSource[pageImageSources.size()]));
	}
	
	/**
	 * Add a page image source.
	 * @param pageImageSource the page image source to add
	 */
	public static void addPageImageSource(PageImageSource pageImageSource) {
		if ((pageImageSource != null) && !pageImageSources.contains(pageImageSource))
			pageImageSources.add(pageImageSource);
	}
	
	/**
	 * Remove a page image source.
	 * @param pageImageSource the page image source to remove
	 */
	public static void removePageImageSource(PageImageSource pageImageSource) {
		if (pageImageSource != null)
			pageImageSources.remove(pageImageSource);
	}
	
	/**
	 * Generate the unified single-string name of a page image from document ID
	 * and page ID. In particular, the name consists of the document ID,
	 * followed by a dot, followed by the page ID padded to four digits with
	 * leading zeros. The name does not have a file extension suffix indicating
	 * an image file format.
	 * @param docId the ID of the document the image belongs to
	 * @param pageId the document internal ID of the page the image represents
	 * @return the unified single-string image name
	 */
	public static String getPageImageName(String docId, int pageId) {
		return (docId + "." + getPageIdString(pageId, 4));
	}
	private static String getPageIdString(int pn, int length) {
		String pns = ("" + pn);
		while (pns.length() < length)
			pns = ("0" + pns);
		return pns;
	}
	
	/**
	 * Retrieve an image by its unified single-string image name. This method
	 * searches all registered page image sources. If none has the sought image,
	 * this method returns null.
	 * @param name unified single-string image name
	 * @return the page image
	 */
	public static PageImage getPageImage(String name) {
		PageImage pi = null;
		for (int p = 0; p < pageImageSources.size(); p++) {
			PageImageSource pis = ((PageImageSource) pageImageSources.get(p));
			try {
				pi = pis.getPageImage(name);
			} catch (IOException ioe) {}
			if (pi != null)
				break;
		}
		return pi;
	}
	
	/**
	 * Retrieve an image by its document ID and page ID. This method searches
	 * all registered page image sources. If none has the sought image, this
	 * method returns null. This method is a shorthand for
	 * getPageImage(getPageImageName(docId, pageId)).
	 * @param docId the ID of the document the sought image belongs to
	 * @param pageId the document internal ID of the page the sought image
	 *            represents
	 * @return the page image
	 */
	public static PageImage getPageImage(String docId, int pageId) {
		return getPageImage(getPageImageName(docId, pageId));
	}
	
	/**
	 * Retrieve the page image stores currently installed
	 * @return the page image stores currently installed
	 */
	public static PageImageStore[] getPageImageStores() {
		return ((PageImageStore[]) pageImageStores.toArray(new PageImageSource[pageImageStores.size()]));
	}
	
	/**
	 * Add a page image store. The argument page image store is also added as a
	 * page image source.
	 * @param pageImageStore the page image store to add
	 */
	public static void addPageImageStore(PageImageStore pageImageStore) {
		if ((pageImageStore != null) && !pageImageStores.contains(pageImageStore)) {
			pageImageStores.add(pageImageStore);
			Collections.sort(pageImageStores, new Comparator() {
				public int compare(Object obj1, Object obj2) {
					return (((PageImageStore) obj2).getPriority() - ((PageImageStore) obj1).getPriority());
				}
			});
		}
		addPageImageSource(pageImageStore);
	}
	
	/**
	 * Remove a page image source.
	 * @param pageImageStore the page image store to remove
	 */
	public static void removePageImageStore(PageImageStore pageImageStore) {
		if (pageImageStore != null)
			pageImageStores.remove(pageImageStore);
		removePageImageSource(pageImageStore);
	}
	
	
	/**
	 * Store a page image, implying original size and zero-width margins.
	 * @param name the unified single-string name of the page image
	 * @param image the page image itself
	 * @param dpi the resolution of the image
	 * @return true if the image was stored, false otherwise
	 * @throws IOException
	 */
	public static boolean storePageImage(String name, BufferedImage image, int dpi) throws IOException {
		return storePageImage(name, new PageImage(image, dpi, null));
	}
	
	/**
	 * Store a page image. If the source of the argument page image is not null
	 * and a page image store, the page image is stored to there. Otherwise,
	 * all registered page image stores are asked to store the page image, in
	 * descending priority order, until one of the stores indicates that it has
	 * actually stored the page image.
	 * @param name the unified single-string name of the page image
	 * @param pageImage the page image itself
	 * @return true if the image was stored, false otherwise
	 * @throws IOException
	 */
	public static boolean storePageImage(String name, PageImage pageImage) throws IOException {
		if ((pageImage.source instanceof PageImageStore) && ((PageImageStore) pageImage.source).storePageImage(name, pageImage))
			return true;
		for (int s = 0; s < pageImageStores.size(); s++) {
			if (((PageImageStore) pageImageStores.get(s)).storePageImage(name, pageImage))
				return true;
		}
		return false;
	}
	
	/**
	 * Store a page image, implying original size and zero-width margins. This
	 * method should be implemented as a shorthand for
	 * storePageImage(PageImage.getPageImageName(docId, pageId), image, dpi).
	 * @param docId the ID of the document the page image belongs to
	 * @param pageId the ID of the page depicted in the argument image
	 * @param image the page image itself
	 * @param dpi the resolution of the image
	 * @return the name the image can be recovered with through the
	 *         Imaging.getImage() method, or null, if the image could not be
	 *         stored in any of the registered page image stores
	 * @throws IOException
	 */
	public static String storePageImage(String docId, int pageId, BufferedImage image, int dpi) throws IOException {
		String name = PageImage.getPageImageName(docId, pageId);
		if (storePageImage(name, new PageImage(image, dpi, null)))
			return name;
		else return null;
	}
	
	/**
	 * Store a page image. This method should be implemented as a shorthand for
	 * storePageImage(PageImage.getPageImageName(docId, pageId), pageImage). If
	 * the source of the argument page image is not null and a page image store,
	 * the page image is stored to there. Otherwise, descending priority order,
	 * until one of the stores indicates that it has actually stored the page
	 * image.
	 * @param docId the ID of the document the page image belongs to
	 * @param pageId the ID of the page depicted in the argument image
	 * @param pageImage the page image itself
	 * @return the name the image can be recovered with through the
	 *         Imaging.getImage() method, or null, if the image could not be
	 *         stored in any of the registered page image stores
	 * @throws IOException
	 */
	public static String storePageImage(String docId, int pageId, PageImage pageImage) throws IOException {
		String name = PageImage.getPageImageName(docId, pageId);
		if (storePageImage(name, pageImage))
			return name;
		else return null;
	}
}

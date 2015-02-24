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

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Storage provider for page images. Implementations should also register an
 * ImageProvider that allows for recovering the images by means of the
 * names returned by the storeImage() method.
 * 
 * @author sautter
 */
public interface PageImageStore extends PageImageSource {
	
	/**
	 * Store a page image, implying original size and zero-width margins.
	 * @param name the unified single-string name of the page image
	 * @param image the page image itself
	 * @param dpi the resolution of the image
	 * @return true if the image was stored, false otherwise
	 * @throws IOException
	 */
	public abstract boolean storePageImage(String name, BufferedImage image, int dpi) throws IOException;
	
	/**
	 * Store a page image.
	 * @param name the unified single-string name of the page image
	 * @param pageImage the page image itself
	 * @return true if the image was stored, false otherwise
	 * @throws IOException
	 */
	public abstract boolean storePageImage(String name, PageImage pageImage) throws IOException;
	
	/**
	 * Store a page image, implying original size and zero-width margins. This
	 * method should be implemented as a shorthand for
	 * storePageImage(PageImage.getPageImageName(docId, pageId), image, dpi).
	 * @param docId the ID of the document the page image belongs to
	 * @param pageId the ID of the page depicted in the argument image
	 * @param image the page image itself
	 * @param dpi the resolution of the image
	 * @return the name the image can be recovered with through the
	 *         Imaging.getImage() method
	 * @throws IOException
	 */
	public abstract String storePageImage(String docId, int pageId, BufferedImage image, int dpi) throws IOException;
	
	/**
	 * Store a page image. This method should be implemented as a shorthand for
	 * storePageImage(PageImage.getPageImageName(docId, pageId), pageImage).
	 * @param docId the ID of the document the page image belongs to
	 * @param pageId the ID of the page depicted in the argument image
	 * @param pageImage the page image itself
	 * @return the name the image can be recovered with through the
	 *         Imaging.getImage() method
	 * @throws IOException
	 */
	public abstract String storePageImage(String docId, int pageId, PageImage pageImage) throws IOException;
	
	/**
	 * Retrieve the priority of the page image store, on a 0-10 scale. The
	 * priority determines when a page image store is asked to store a page
	 * image in case multiple page image stores are registered. Page image
	 * stores that are interested only in specific page images, e.g. from a
	 * specific document, should return a high priority to make sure they get
	 * to store the images they want to store. General page image stores, in
	 * turn, should indicate a low priority to yield to specific ones.
	 * @return the priority of the page image store
	 */
	public abstract int getPriority();
	
	/**
	 * Implementation of a page image store that leaves only two methods
	 * abstract in addition to the one from page image source, namely the
	 * one-argument version of isPageImageAvailable() and the two-argument
	 * version of storePageImage().
	 * 
	 * @author sautter
	 */
	public static abstract class AbstractPageImageStore extends AbstractPageImageSource implements PageImageStore {
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.imaging.PageImageStore#storePageImage(java.lang.String, java.awt.image.BufferedImage, int)
		 */
		public boolean storePageImage(String name, BufferedImage image, int dpi) throws IOException {
			return this.storePageImage(name, new PageImage(image, dpi, this));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.imaging.PageImageStore#storePageImage(java.lang.String, int, java.awt.image.BufferedImage, int)
		 */
		public String storePageImage(String docId, int pageId, BufferedImage image, int dpi) throws IOException {
			String name = PageImage.getPageImageName(docId, pageId);
			this.storePageImage(name, new PageImage(image, dpi, this));
			return name;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.imaging.PageImageStore#storePageImage(java.lang.String, int, de.uka.ipd.idaho.gamta.util.imaging.PageImage)
		 */
		public String storePageImage(String docId, int pageId, PageImage pageImage) throws IOException {
			String name = PageImage.getPageImageName(docId, pageId);
			this.storePageImage(name, pageImage);
			return name;
		}
	}
}
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

import java.io.IOException;

/**
 * This interface abstracts from how different image handling components provide
 * page images.
 * 
 * @author sautter
 */
public interface PageImageSource extends ImagingConstants {
	
	/**
	 * Check whether or not an image of a specific page of a specific document
	 * is available from this page image source.
	 * @param name the unified single-string name of the sought page image
	 * @return true if the image is already stored
	 */
	public abstract boolean isPageImageAvailable(String name);
	
	/**
	 * Check whether or not an image of a specific page of a specific document
	 * is available from this page image source. This method should be
	 * implemented as a shorthand for
	 * isPageImageAvailable(PageImage.getPageImageName(docId, pageId)).
	 * @param docId the ID of the document the page image belongs to
	 * @param pageId the ID of the page depicted in the argument image
	 * @return true if the image is available
	 */
	public abstract boolean isPageImageAvailable(String docId, int pageId);
	
	/**
	 * Obtain the image of a specific page in a specific document.
	 * @param name the unified single-string name of the sought page image
	 * @return the image of the page
	 */
	public abstract PageImage getPageImage(String name) throws IOException;
	
	/**
	 * Obtain an input stream for the image of a specific page in a specific
	 * document.
	 * @param name the unified single-string name of the sought page image
	 * @return an input stream for the image of the page
	 */
	public abstract PageImageInputStream getPageImageAsStream(String name) throws IOException;
	
	/**
	 * Obtain the image of a specific page in a specific document. This method
	 * should be implemented as a shorthand for
	 * getPageImage(PageImage.getPageImageName(docId, pageId)).
	 * @param docId the ID of the document the page belongs to
	 * @param pageId the ID of the actual page
	 * @return the image of the page
	 */
	public abstract PageImage getPageImage(String docId, int pageId) throws IOException;
	
	/**
	 * Obtain an input stream for the image of a specific page in a specific
	 * document. This method should be implemented as a shorthand for
	 * getPageImage(PageImage.getPageImageName(docId, pageId)).
	 * @param docId the ID of the document the page belongs to
	 * @param pageId the ID of the actual page
	 * @return an input stream for the image of the page
	 */
	public abstract PageImageInputStream getPageImageAsStream(String docId, int pageId) throws IOException;
	
	/**
	 * Implementation of a page image source that leaves only two abstract
	 * method to implement, namely the one-argument versions of
	 * isPageImageAvailable() and getPageImageAsStream().
	 * 
	 * @author sautter
	 */
	public static abstract class AbstractPageImageSource implements PageImageSource {
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.imaging.PageImageSource#isPageImageAvailable(java.lang.String, int)
		 */
		public boolean isPageImageAvailable(String docId, int pageId) {
			return this.isPageImageAvailable(PageImage.getPageImageName(docId, pageId));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.imaging.PageImageSource#getPageImage(java.lang.String)
		 */
		public PageImage getPageImage(String name) throws IOException {
			PageImageInputStream piis = this.getPageImageAsStream(name);
			if (piis == null)
				return null;
			PageImage pi = new PageImage(piis);
			piis.close();
			return pi;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.imaging.PageImageSource#getPageImage(java.lang.String, int)
		 */
		public PageImage getPageImage(String docId, int pageId) throws IOException {
			return this.getPageImage(PageImage.getPageImageName(docId, pageId));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.imaging.PageImageSource#getPageImageAsStream(java.lang.String, int)
		 */
		public PageImageInputStream getPageImageAsStream(String docId, int pageId) throws IOException {
			return this.getPageImageAsStream(PageImage.getPageImageName(docId, pageId));
		}
	}
}
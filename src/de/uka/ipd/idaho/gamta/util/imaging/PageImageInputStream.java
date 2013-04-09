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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper for an input stream for reading the data written by
 * PageImage.write(). This class reads and interprets the meta data bypes
 * provided by the wrapped stream, and then provides the actual image data
 * as a regular input stream.
 * 
 * @author sautter
 */
public class PageImageInputStream extends FilterInputStream {
	
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
	
	/** the page image source this image was loaded from */
	public final PageImageSource source;
	
	/**
	 * Constructor
	 * @param in the input stream to read from
	 * @param the page image source providing the stream
	 * @throws IOException
	 */
	public PageImageInputStream(InputStream in, PageImageSource source) throws IOException {
		super(in);
		this.originalWidth = readInt(in, 2);
		this.originalHeight = readInt(in, 2);
		this.originalDpi = readInt(in, 2);
		this.currentDpi = readInt(in, 2);
		this.leftEdge = readInt(in, 2);
		this.rightEdge = readInt(in, 2);
		this.topEdge = readInt(in, 2);
		this.bottomEdge = readInt(in, 2);
		this.source = source;
	}
	private static final int readInt(InputStream in, int numBytes) throws IOException {
		int theInt = 0;
		theInt += (in.read() & 255);
		if (numBytes == 1)
			return theInt;
		theInt <<= 8;
		theInt += (in.read() & 255);
		if (numBytes == 2)
			return theInt;
		theInt <<= 8;
		theInt += (in.read() & 255);
		if (numBytes == 3)
			return theInt;
		theInt <<= 8;
		theInt += (in.read() & 255);
		return theInt;
	}
}

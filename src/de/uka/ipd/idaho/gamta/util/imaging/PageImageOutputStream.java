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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapper for an output stream for writing data to be read by
 * PageImage.read(). This class writes the meta data to the wrapped stream,
 * and then awaits the actual image data written to it as to a regular
 * output stream.
 * 
 * @author sautter
 */
public class PageImageOutputStream extends FilterOutputStream {
	
	/**
	 * Constructor
	 * @param out the output stream to write to
	 * @param pageImage the page image whose meta data to send
	 * @throws IOException
	 */
	public PageImageOutputStream(OutputStream out, PageImage pageImage) throws IOException {
		super(out);
		writeInt(out, pageImage.originalWidth, 2);
		writeInt(out, pageImage.originalHeight, 2);
		writeInt(out, pageImage.originalDpi, 2);
		writeInt(out, pageImage.currentDpi, 2);
		writeInt(out, pageImage.leftEdge, 2);
		writeInt(out, pageImage.rightEdge, 2);
		writeInt(out, pageImage.topEdge, 2);
		writeInt(out, pageImage.bottomEdge, 2);
	}
	
	/**
	 * Constructor for looping through the meta data after it has been read
	 * @param out the output stream to write to
	 * @param piis the page image input stream whose meta data to loop
	 *            through
	 * @throws IOException
	 */
	public PageImageOutputStream(OutputStream out, PageImageInputStream piis) throws IOException {
		super(out);
		writeInt(out, piis.originalWidth, 2);
		writeInt(out, piis.originalHeight, 2);
		writeInt(out, piis.originalDpi, 2);
		writeInt(out, piis.currentDpi, 2);
		writeInt(out, piis.leftEdge, 2);
		writeInt(out, piis.rightEdge, 2);
		writeInt(out, piis.topEdge, 2);
		writeInt(out, piis.bottomEdge, 2);
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
}

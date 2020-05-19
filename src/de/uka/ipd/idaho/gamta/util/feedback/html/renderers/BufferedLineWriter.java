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
 *     * Neither the name of the Universitaet Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITAET KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
package de.uka.ipd.idaho.gamta.util.feedback.html.renderers;

import java.io.BufferedWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Utility class for writing line-wise. This class adds a writeLine() method to
 * BufferedWriter, to facilitate more compact code, while still being able to
 * propagate exceptions, or handle them explicitly, which would not be possible
 * using a PrintWriter's println() method. Furthermore, flushing a buffered line
 * writer flushes only the buffer, but not the wrapped writer. This is in order
 * to allow using this convenience class in situations where flushing the
 * wrapped writer prematurely would be fatal, e.g. in a servlet. However,
 * closing the writer does flush the underlying stream before closing it.
 * 
 * @author sautter
 */
public class BufferedLineWriter extends BufferedWriter {
	private Writer out;
	private static class IsolatorWriter extends FilterWriter {
		IsolatorWriter(Writer out) {
			super(out);
		}
		public void flush() throws IOException {}
	}
	
	/**
	 * @param out
	 * @param sz
	 */
	public BufferedLineWriter(Writer out, int sz) {
		super(new IsolatorWriter(out), sz);
		this.out = out;
	}

	/**
	 * @param out
	 */
	public BufferedLineWriter(Writer out) {
		super(new IsolatorWriter(out));
		this.out = out;
	}
	
	/**
	 * Flush the buffer. This implementation <b>does not</b> flush the
	 * underlying stream.
	 * @see java.io.BufferedWriter#flush()
	 */
	public void flush() throws IOException {
		super.flush();
	}
	
	/**
	 * Close the stream. This implementation flushes first the buffer and then
	 * the underlying stream before actually closing it.
	 * @see java.io.BufferedWriter#close()
	 */
	public void close() throws IOException {
		super.flush(); // flush buffer (cascades only to protection wrapper)
		this.out.flush(); // flush underlying stream
		super.close(); // close everything
	}
	
	/**
	 * Combination of write() and newLine(), in this order.
	 * @param line the line to write
	 * @throws IOException
	 */
	public void writeLine(String line) throws IOException {
		this.write(line);
		this.newLine();
	}
}

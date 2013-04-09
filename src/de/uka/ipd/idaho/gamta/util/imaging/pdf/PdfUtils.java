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
package de.uka.ipd.idaho.gamta.util.imaging.pdf;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility methods for PDF handling.
 * 
 * @author sautter
 */
class PdfUtils {
	
	static class LineInputStream extends FilterInputStream {
		int buf = -1;
		LineInputStream(InputStream in) {
			super((in instanceof BufferedInputStream) ? in : new BufferedInputStream(in));
		}
		//	returns a line of bytes, INCLUDING its terminal line break bytes
		byte[] readLine() throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int bl = 0;
			if (this.buf != -1) {
				baos.write(this.buf);
				bl++;
				this.buf = -1;
			}
			int b;
			boolean lastWasCR = false;
			while ((b = this.read()) != -1) {
				if (lastWasCR) {
					if (b == '\n') {
						baos.write(b);
						bl++;
						this.buf = -1;
					}
					else this.buf = b;
					break;
				}
				else if (b == '\n') {
					baos.write(b);
					bl++;
					break;
				}
				lastWasCR = (b == '\r');
				baos.write(b);
				bl++;
			}
			return ((bl == 0) ? null : baos.toByteArray());
		}
	}
	
	static class PeekInputStream extends FilterInputStream {
		private int peek;
		private int mPeek;
		PeekInputStream(ByteArrayInputStream in) throws IOException {
			super(in);
			this.peek = super.read();
		}
		public int peek() {
			return this.peek;
		}
		public boolean skipSpace() throws IOException {
			int p = this.peek();
			while ((p != -1) && (p < 33)) {
				this.read();
				p = this.peek();
			}
			return (p != -1);
		}
		public int read() throws IOException {
			int b = this.peek;
			this.peek = super.read();
			return b;
		}
		public int read(byte[] b, int off, int len) throws IOException {
			if (this.peek() == -1)
				return -1;
			b[off] = ((byte) this.peek());
			int r = super.read(b, (off+1), (len-1));
			this.read();
			return (r+1);
		}
		public long skip(long n) throws IOException {
			long s = super.skip(n-1);
			this.read();
			return (s+1);
		}
		public int available() throws IOException {
			return (super.available() + 1);
		}
		public synchronized void mark(int readlimit) {
			super.mark(readlimit-1);
			this.mPeek = this.peek;
		}
		public synchronized void reset() throws IOException {
			super.reset();
			this.peek = this.mPeek;
		}
	}
	
	static String toString(byte[] bytes, boolean stopAtLineEnd) {
		StringBuffer sb = new StringBuffer();
		for (int c = 0; c < bytes.length; c++) {
			if (stopAtLineEnd && ((bytes[c] == '\n') || (bytes[c] == '\r')))
				break;
			sb.append((char) bytes[c]);
		}
		return sb.toString();
	}
	
	static boolean matches(byte[] bytes, String regEx) {
		return toString(bytes, true).matches(regEx);
	}
	
	static boolean equals(byte[] bytes, String str) {
		if (bytes.length < str.length())
			return false;
		
		int actualByteCount = bytes.length;
		while ((actualByteCount > 0) && ((bytes[actualByteCount-1] == '\n') || (bytes[actualByteCount-1] == '\r')))
			actualByteCount--;
		if (actualByteCount != str.length())
			return false;
		
		for (int c = 0; c < str.length(); c++) {
			if (bytes[c] != str.charAt(c))
				return false;
		}
		
		return true;
	}
	
	static boolean startsWith(byte[] bytes, String str, int offset) {
		if (bytes.length < (offset + str.length()))
			return false;
		
		for (int c = 0; c < str.length(); c++) {
			if (bytes[offset + c] != str.charAt(c))
				return false;
		}
		
		return true;
	}
	
	static boolean endsWith(byte[] bytes, String str) {
		if (bytes.length < str.length())
			return false;
		
		int actualByteCount = bytes.length;
		while ((actualByteCount > 0) && ((bytes[actualByteCount-1] == '\n') || (bytes[actualByteCount-1] == '\r')))
			actualByteCount--;
		if (actualByteCount < str.length())
			return false;
		
		for (int c = 0; c < str.length(); c++) {
			if (bytes[c + (actualByteCount - str.length())] != str.charAt(c))
				return false;
		}
		
		return true;
	}
}

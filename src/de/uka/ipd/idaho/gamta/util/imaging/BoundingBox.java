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
import java.io.InputStream;
import java.io.OutputStream;

import de.uka.ipd.idaho.gamta.Annotation;

/**
 * A bounding box marks an area of a page image.
 * 
 * @author sautter
 */
public class BoundingBox {
	
	/** the left boundary of the box */
	public final int left;
	
	/** the right boundary of the box */
	public final int right;
	
	/** the top boundary of the box */
	public final int top;
	
	/** the bottom boundary of the box */
	public final int bottom;
	
	/**
	 * Constructor
	 * @param left the left boundary of the box
	 * @param right the right boundary of the box
	 * @param top the top boundary of the box
	 * @param bottom the bottom boundary of the box
	 */
	public BoundingBox(int left, int right, int top, int bottom) {
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
	}
	
	/**
	 * Translate this bounding box relative to another one
	 * @param bb the bounding box to translate this one to
	 * @return a bounding box marking the same area as this one, but inside the
	 *         argument bounding box
	 */
	public BoundingBox relativeTo(BoundingBox bb) {
		return new BoundingBox((this.left - bb.left), (this.right - bb.left), (this.top - bb.top), (this.bottom - bb.top));
	}
	
	/**
	 * @return a string representation of the bounding box, namely the left,
	 *         right, top, and bottom boundaries, comma separated in this order.
	 */
	public String toString() {
		return ("[" + this.left + "," + this.right + "," + this.top + "," + this.bottom + "]");
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (this.hashCode == 0) {
			this.hashCode <<= 8;
			this.hashCode &= (this.left & 255);
			this.hashCode <<= 8;
			this.hashCode &= (this.right & 255);
			this.hashCode <<= 8;
			this.hashCode &= (this.top & 255);
			this.hashCode <<= 8;
			this.hashCode &= (this.bottom & 255);
		}
		return this.hashCode;
	}
	private int hashCode = 0;
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof BoundingBox) {
			BoundingBox bb = ((BoundingBox) obj);
			return ((bb.left == this.left) && (bb.right == this.right) && (bb.top == this.top) && (bb.bottom == this.bottom));
		}
		else return false;
	}
	
	/**
	 * Test whether or not this bounding box includes another bounding box. If
	 * <code>fuzzy</code> is set to <code>true</code>, the this method tests if
	 * this box includes the center of the argument box rather than the argument
	 * box as a whole.
	 * @param box the bounding box to test
	 * @param fuzzy do a fuzzy test?
	 * @return true if this bounding box includes the argument one
	 */
	public boolean includes(BoundingBox box, boolean fuzzy) {
		if ((this.left <= box.left) && (box.right <= this.right) && (this.top <= box.top) && (box.bottom <= this.bottom))
			return true;
		else if (fuzzy) {
			int bcx = ((box.left + box.right) / 2);
			int bcy = ((box.top + box.bottom) / 2);
			return ((this.left <= bcx) && (bcx < this.right) && (this.top <= bcy) && (bcy < this.bottom));
		}
		else return false;
	}
	
	/**
	 * Test whether or not this bounding box lies in another bounding box. If
	 * <code>fuzzy</code> is set to <code>true</code>, the this method tests if
	 * the argument box includes the center of this box rather than this box as
	 * a whole.
	 * @param box the bounding box to test
	 * @param fuzzy do a fuzzy test?
	 * @return true if the argument bounding box includes this one
	 */
	public boolean liesIn(BoundingBox box, boolean fuzzy) {
		return ((box != null) && box.includes(this, fuzzy));
	}
	
	/**
	 * Test whether or not this bounding box overlaps another bounding box.
	 * @param box the bounding box to test
	 * @return true if this bounding box overlaps the argument one
	 */
	public boolean overlaps(BoundingBox box) {
		return ((this.left < box.right) && (box.left < this.right) && (this.top < box.bottom) && (box.top < this.bottom));
	}
	
	/**
	 * Get the width of the bounding box.
	 * @return the width of the bounding box
	 */
	public int getWidth() {
		return (this.right - this.left);
	}
	
	/**
	 * Get the height of the bounding box.
	 * @return the height of the bounding box
	 */
	public int getHeight() {
		return (this.bottom - this.top);
	}
	
	/**
	 * Get the area of the bounding box.
	 * @return the area of the bounding box
	 */
	public int getArea() {
		return ((this.right - this.left) * (this.bottom - this.top));
	}
	
	/**
	 * Translate the bounding box.
	 * @param tx the distance to translate by in horizontal direction
	 * @param ty the distance to translate by in vertical direction
	 * @return the translated bounding box
	 */
	public BoundingBox translate(int tx, int ty) {
		return new BoundingBox(
				(this.left + tx),
				(this.right + tx),
				(this.top + ty),
				(this.bottom + ty)
			);
	}
	
	/**
	 * Scale the bounding box. The edges of the returned bounding box are
	 * rounded to the nearest integer.
	 * @param s the factor to scale by
	 * @return the scaled bounding box
	 */
	public BoundingBox scale(float s) {
		return this.scale(s, s);
	}
	
	/**
	 * Scale the bounding box. The edges of the returned bounding box are
	 * rounded to the nearest integer.
	 * @param sx the factor to scale by in horizontal direction
	 * @param sy the factor to scale by in vertical direction
	 * @return the scaled bounding box
	 */
	public BoundingBox scale(float sx, float sy) {
		return new BoundingBox(
				Math.round(this.left * sx),
				Math.round(this.right * sx),
				Math.round(this.top * sy),
				Math.round(this.bottom * sy)
			);
	}
	
	/**
	 * Parse a bounding box from its string representation. This method expects
	 * the string to consist of four comma separated positive integer numbers,
	 * optionally delimited with square brackets. If the argument string is null
	 * or the empty string, this method returns null.
	 * @param bbDataString the string to parse.
	 * @return a bounding box decoded from the argument string
	 * @throws IllegalArgumentException
	 */
	public static BoundingBox parse(String bbDataString) throws IllegalArgumentException {
		if (bbDataString == null)
			return null;
		bbDataString = bbDataString.trim();
		if (bbDataString.startsWith("["))
			bbDataString = bbDataString.substring(1);
		if (bbDataString.endsWith("]"))
			bbDataString = bbDataString.substring(0, (bbDataString.length() - 1));
		bbDataString = bbDataString.trim();
		if (bbDataString.length() == 0)
			return null;
		
		String[] bbData = bbDataString.split("\\s*\\,\\s*");
		if (bbData.length == 4) {
			int left = Integer.parseInt(bbData[0]);
			int right = Integer.parseInt(bbData[1]);
			int top = Integer.parseInt(bbData[2]);
			int bottom = Integer.parseInt(bbData[3]);
			return new BoundingBox(left, right, top, bottom);
		}
		else throw new IllegalArgumentException("Invalid bounding box data: " + bbDataString);
	}
	
	/**
	 * Parse a group of bounding boxes from the concatenation of their string
	 * representations. This method expects a concatenation of strings compliant
	 * with the contract of the parse() method, each one enclosed in square
	 * brackets. If the argument data string is null, this method returns null.
	 * Consequently, the returned array always contains at least one element,
	 * which may be null, though.
	 * @param bbDataString the string to parse.
	 * @return a bounding box decoded from the argument string
	 * @throws IllegalArgumentException
	 */
	public static BoundingBox[] parseBoundingBoxes(String bbDataString) throws IllegalArgumentException {
		if (bbDataString == null)
			return null;
		String[] bbDataStrings = bbDataString.trim().split("\\]\\s*\\[");
		BoundingBox[] boxes = new BoundingBox[bbDataStrings.length];
		for (int b = 0; b < bbDataStrings.length; b++)
			boxes[b] = parse(bbDataStrings[b].trim());
		return boxes;
	}

	/**
	 * Create a bounding box from the 'box' attribute of an annotation. If the
	 * argument annotation lacks the 'box' attribute, this method returns null.
	 * If the annotation spans more than one page, use the getBoundingBoxes()
	 * method instead.
	 * @param annotation the annotation to parse the box from
	 * @return a bounding box created from the 'box' attribute of the argument
	 *         annotation
	 */
	public static BoundingBox getBoundingBox(Annotation annotation) {
		return parse(((String) annotation.getAttribute(ImagingConstants.BOUNDING_BOX_ATTRIBUTE)));
	}
	
	/**
	 * Create bounding boxes from the 'box' attribute of an annotation. If the
	 * argument annotation lacks the 'box' attribute, this method returns null.
	 * Consequently, the returned array always contains at least one element,
	 * which may be null, though.
	 * @param annotation the annotation to parse the box from
	 * @return a bounding box created from the 'box' attribute of the argument
	 *         annotation
	 */
	public static BoundingBox[] getBoundingBoxes(Annotation annotation) {
		return parseBoundingBoxes(((String) annotation.getAttribute(ImagingConstants.BOUNDING_BOX_ATTRIBUTE)));
	}
	
	/**
	 * Create a bounding box that covers a series of smaller bounding boxes. The
	 * resulting bounding box is only meaningful if the argument bounding boxes
	 * refer to the same image.
	 * @param boxes the bounding boxes to aggregate
	 * @return a bounding box that covers the argument bounding boxes
	 */
	public static BoundingBox aggregate(BoundingBox[] boxes) {
		if (boxes.length == 0)
			return null;
		if (boxes.length == 1)
			return boxes[0];
		int left = Integer.MAX_VALUE;
		int right = 0;
		int top = Integer.MAX_VALUE;
		int bottom = 0;
		for (int b = 0; b < boxes.length; b++) {
			if (boxes[b] == null)
				continue;
			left = Math.min(left, boxes[b].left);
			right = Math.max(right, boxes[b].right);
			top = Math.min(top, boxes[b].top);
			bottom = Math.max(bottom, boxes[b].bottom);
		}
		return (((left < right) && (top < bottom)) ? new BoundingBox(left, right, top, bottom) : null);
	}
	
	/**
	 * Write the bounding box to an output stream, using two bytes for each
	 * boundary. Data written trough this method is supposed to be re-read and
	 * de-serialized through the read() method. This method writes 8 bytes to
	 * the argument stream.
	 * @param out the writer to write to
	 * @throws IOException
	 */
	public void write(OutputStream out) throws IOException {
		out.write((this.left >>> 8) & 255);
		out.write(this.left & 255);
		out.write((this.right >>> 8) & 255);
		out.write(this.right & 255);
		out.write((this.top >>> 8) & 255);
		out.write(this.top & 255);
		out.write((this.bottom >>> 8) & 255);
		out.write(this.bottom & 255);
	}
	
	/**
	 * Read a bounding box from an input stream. This method is intended to
	 * recover a bounding box from the serial representation it has written to
	 * a writer via its write() method. This method reads 8 bytes from the
	 * argument stream.
	 * @param in the reader to read from
	 * @return a bounding box de-serialized from the argument stream
	 * @throws IOException
	 */
	public static BoundingBox read(InputStream in) throws IOException {
		int left = 0;
		left += (in.read() & 255);
		left <<= 8;
		left += (in.read() & 255);
		int right = 0;
		right += (in.read() & 255);
		right <<= 8;
		right += (in.read() & 255);
		int top = 0;
		top += (in.read() & 255);
		top <<= 8;
		top += (in.read() & 255);
		int bottom = 0;
		bottom += (in.read() & 255);
		bottom <<= 8;
		bottom += (in.read() & 255);
		return new BoundingBox(left, right, top, bottom);
	}
}

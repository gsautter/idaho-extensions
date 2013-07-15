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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextLayout;
import java.awt.geom.CubicCurve2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.icepdf.core.pobjects.Name;

import de.uka.ipd.idaho.gamta.util.imaging.Imaging;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging.AnalysisImage;
import de.uka.ipd.idaho.gamta.util.imaging.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.gamta.util.imaging.pdf.PdfParser.PStream;
import de.uka.ipd.idaho.gamta.util.imaging.pdf.PdfParser.PString;
import de.uka.ipd.idaho.gamta.util.imaging.pdf.PdfParser.PtTag;
import de.uka.ipd.idaho.gamta.util.imaging.pdf.PdfUtils.LineInputStream;
import de.uka.ipd.idaho.gamta.util.imaging.pdf.PdfUtils.PeekInputStream;
import de.uka.ipd.idaho.gamta.util.imaging.utilities.ImageDisplayDialog;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * A single font extracted from a PDF. Class methods for font parsing.
 * 
 * @author sautter
 */
class PdfFont {
	
	private static final boolean DEBUG_CHAR_HANDLING = false;
	
	static class BaseFont {
//		Hashtable data;
		Hashtable descriptor;
		int firstChar;
		int lastChar;
		float[] charWidths;
		float mCharWidth;
//		String name;
		boolean bold;
		boolean italic;
		String encoding;
//		float ascent = 0;
//		float descent = 0;
//		float capHeigth = 0;
//		int type = 1;
		HashMap charMappings = new HashMap(1);
		HashMap nCharWidths = new HashMap(1);
//		Font font;
		BaseFont(Hashtable data, Hashtable descriptor, float mCharWidth, String name, String encoding) {
//			this.data = data;
			this.descriptor = descriptor;
			this.mCharWidth = mCharWidth;
//			this.name = name;
			this.bold = (name.indexOf("Bold") != -1);
			this.italic = ((name.indexOf("Italic") != -1) || (name.indexOf("Oblique") != -1));
			this.encoding = encoding;
		}
		char resolveByte(int chb) {
			if (DEBUG_CHAR_HANDLING || (127 < chb))
				System.out.println("   - BF resolving '" + ((char) chb) + "' (" + chb + ")");				
			Integer chi = new Integer(chb);
			Character mCh = ((Character) this.charMappings.get(chi));
			if (mCh != null) {
				if (DEBUG_CHAR_HANDLING || (127 < chb))
					System.out.println("   --> mapping resolved to '" + mCh.charValue() + "' (" + ((int) mCh.charValue()) + ")");				
				return mCh.charValue();
			}
			if ("WinAnsiEncoding".equals(this.encoding))
				mCh = ((Character) PdfFont.winAnsiMappings.get(chi));
			else if ("MacRomanEncoding".equals(this.encoding))
				mCh = ((Character) PdfFont.macRomanMappings.get(chi));
			else if ("MacExpertEncoding".equals(this.encoding))
				mCh = ((Character) PdfFont.macExpertMappings.get(chi));
			else if ("AdobeStandardEncoding".equals(this.encoding) || "StandardEncoding".equals(this.encoding))
				mCh = ((Character) PdfFont.standardAdobeMappings.get(chi));
			if ((DEBUG_CHAR_HANDLING || (127 < chb)) && (mCh != null))
				System.out.println("   --> " + this.encoding + " resolved to '" + mCh.charValue() + "' (" + ((int) mCh.charValue()) + ")");				
			return ((mCh == null) ? 0 : mCh.charValue());
		}
		float getCharWidth(char ch) {
			if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - getting width for '" + ch + "' (" + ((int) ch) + ")");
			float cw = 0;
			if (cw == 0) {
				Float cwObj = ((Float) this.nCharWidths.get(new Character(ch)));
				if (cwObj != null) {
					cw = cwObj.floatValue();
					if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println("   - got char width from map: " + cw);
				}
			}
			if ((cw == 0) && (this.firstChar <= ch) && (ch <= this.lastChar)) {
				cw = this.charWidths[((int) ch) - this.firstChar];
				if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println("   - got char width from array: " + cw);
			}
			if (cw == 0) {
				cw = this.mCharWidth;
				if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println("   - falling back to missing width: " + cw);
			}
			return cw;
		}
		void setCharWidths(int fc, int lc, float[] cws) {
			this.firstChar = fc;
			this.lastChar = lc;
			this.charWidths = cws;
		}
		void setNamedCharWidth(char ch, float cw) {
			this.nCharWidths.put(new Character(ch), new Float(cw));
		}
	}
	
	Hashtable data;
	Hashtable descriptor;
	int firstChar;
	int lastChar;
	float[] charWidths;
	float mCharWidth;
	String name;
	boolean bold;
	boolean italic;
	String encoding;
	float ascent = 0;
	float descent = 0;
	float capHeigth = 0;
	boolean hasDescent = true;
	int type = 1;
//	HashMap charMappings = new HashMap(1);
//	HashMap rCharMappings = new HashMap(1);
	HashMap ucMappings = new HashMap(1);
	HashMap diffMappings = new HashMap(1);
	HashSet subSetFilter = null;
	BaseFont baseFont;
	HashMap ccWidths = new HashMap(1);
	HashMap cWidths = new HashMap(1);
	HashMap charBaselineShifts = new HashMap(2);
	PdfFont(Hashtable data, Hashtable descriptor, int firstChar, int lastChar, float[] charWidths, float mCharWidth, String name, String encoding) {
		this.data = data;
		this.descriptor = descriptor;
		this.firstChar = firstChar;
		this.lastChar = lastChar;
		this.charWidths = charWidths;
		this.mCharWidth = mCharWidth;
		this.name = name;
		this.bold = (name.indexOf("Bold") != -1);
		this.italic = ((name.indexOf("Italic") != -1) || (name.indexOf("Oblique") != -1));
		this.encoding = encoding;
	}
	void setBaseFont(BaseFont baseFont) {
		this.baseFont = baseFont;
		if ((this.encoding != null) && !this.encoding.equals(this.baseFont.encoding)) {
			if (DEBUG_LOAD_FONTS)
				System.out.println(" - base font encoding corrected to " + this.encoding);
			this.baseFont.encoding = this.encoding;
		}
		//	TODO figure out if encoding correction makes sense or causes errors
	}
//	String resolveByte(byte b) {
//		return this.resolveByte((int) b);
//	}
	String getUnicode(int chb) {
		if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println("Resolving " + chb + " ...");
		if (chb < 0) {
			chb += 256;
			if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" - incremented to " + ((int) chb));
		}
		Integer chi = new Integer(chb);
		
		//	try direct unicode mapping
		String mStr = ((String) this.ucMappings.get(chi));
		if (mStr != null) {
			if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> UC resolved to " + mStr);
			return mStr;
		}
		
		//	try differences mapping
		Character mCh = ((Character) this.diffMappings.get(chi));
		if (mCh != null) {
			if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> Diff resolved to " + mCh);
			return ("" + mCh.charValue());
		}
		
		//	check base font if char in range
		if ((this.baseFont != null) && ((chb < this.firstChar) || (this.lastChar < chb))) {
			char ch = this.baseFont.resolveByte(chb);
			if (ch != 0) {
				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> base font out of own range resolved to " + ch);
				return ("" + ch);
			}
		}
		
		//	try default encodings
		if ("AdobeStandardEncoding".equals(this.encoding)) {
			mCh = ((Character) standardAdobeMappings.get(chi));
			if (mCh != null) {
				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> Adobe resolved to " + mCh.charValue());
				return ("" + mCh.charValue());
			}
			else if ((this.firstChar <= chb) && (chb <= this.lastChar)) {
				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> Adobe default resolved to " + ((char) chb));
				return ("" + ((char) chb));
			}
		}
		else if ("MacRomanEncoding".equals(this.encoding)) {
			mCh = ((Character) macRomanMappings.get(chi));
			if (mCh != null) {
				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> MacRoman resolved to " + mCh.charValue());
				return ("" + mCh.charValue());
			}
			else if ((this.firstChar <= chb) && (chb <= this.lastChar)) {
				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> MacRoman default resolved to " + ((char) chb));
				return ("" + ((char) chb));
			}
		}
		else if ("MacExpertEncoding".equals(this.encoding)) {
			mCh = ((Character) macExpertMappings.get(chi));
			if (mCh != null) {
				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> MacExpert resolved to " + mCh.charValue());
				return ("" + mCh.charValue());
			}
			else if ((this.firstChar <= chb) && (chb <= this.lastChar)) {
				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> MacExpert default resolved to " + ((char) chb));
				return ("" + ((char) chb));
			}
		}
		else if ("WinAnsiEncoding".equals(this.encoding) || (this.encoding == null)) {
			mCh = ((Character) winAnsiMappings.get(chi));
			if (mCh != null) {
				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> ANSI resolved to " + mCh.charValue());
				return ("" + mCh.charValue());
			}
			else if ((this.firstChar <= chb) && (chb <= this.lastChar)) {
				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> ANSI default resolved to " + ((char) chb));
				return ("" + ((char) chb));
			}
		}
		
		//	try base font regardless of range
		if (this.baseFont != null) {
			char ch = this.baseFont.resolveByte(chb);
			if (ch != 0) {
				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> base font resolved to " + ch);
				return ("" + ch);
			}
		}
		
		//	resort to ASCII
		if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> ASCII resolved to " + ((char) chb));
		return ("" + ((char) chb));
	}
	void mapDifference(Integer ch, Character mCh) {
		this.diffMappings.put(ch, mCh);
		if (DEBUG_CHAR_HANDLING)
			System.out.println("Diff-Mapped " + ch + " to '" + mCh + "'");
	}
	void mapUnicode(Integer ch, String str) {
		this.ucMappings.put(ch, str);
		if (DEBUG_CHAR_HANDLING)
			System.out.println("UC-Mapped " + ch + " to '" + str + "'");
	}
	float getCharWidth(char ch) {
		if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println("Getting width for '" + ch + "' (" + ((int) ch) + ")");
		float cw = 0;
		if (cw == 0) {
			Float cwObj = ((Float) this.ccWidths.get(new Integer((int) ch)));
			if (cwObj != null) {
				cw = cwObj.floatValue();
				if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - width from own ur-mapping: " + cw);
			}
		}
		if (cw == 0) {
			Float cwObj = ((Float) this.cWidths.get(new Character(ch)));
			if (cwObj != null) {
				cw = cwObj.floatValue();
				if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - width from own r-mapping: " + cw);
			}
		}
		if ((cw == 0) && (this.firstChar <= ch) && (ch <= this.lastChar)) {
			cw = this.charWidths[((int) ch) - this.firstChar];
			if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - width from base array: " + cw);
		}
		if ((cw == 0) && (this.baseFont != null)) {
			cw = this.baseFont.getCharWidth(ch);
			if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - width from base font: " + cw);
		}
//			if ((cw == 0) && (this.baseFont != null)) {
//				cw = this.baseFont.getCharWidth(ch);
//				if (DEBUG_CHAR_HANDLING) System.out.println(" - width from base font: " + cw);
//			}
//		if ((cw == 0) && this.rCharMappings.containsKey(new Character((char) ch))) {
//			Integer rmCh = ((Integer) this.rCharMappings.get(new Character((char) ch)));
//			if ((rmCh != null) && (rmCh.intValue() != ch)) {
//				if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - recursing with reverse mapped CID");
//				return this.getCharWidth((char) rmCh.intValue());
//			}
//		}
		if (cw == 0) {
			cw = this.mCharWidth;
			if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - fallback missing width: " + cw);
		}
		if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" --> width is " + cw);
		return cw;
	}
	
	/** set width for unresolved character code */
	void setCharWidth(Integer ch, float cw) {
		this.ccWidths.put(ch, new Float(cw));
		if (DEBUG_CHAR_HANDLING)
			System.out.println("Unresolved width for " + ch + " set to " + cw);
	}
	
	/** set width for actual character */
	void setCharWidth(Character ch, float cw) {
		this.cWidths.put(ch, new Float(cw));
		if (DEBUG_CHAR_HANDLING)
			System.out.println("Resolved width for '" + ch + "' (" + ((int) ch.charValue()) + ") set to " + cw);
	}
//	char resolveByte(byte b) {
//		return this.resolveByte((int) b);
//	}
//	char resolveByte(int chb) {
//		if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println("Resolving " + chb + " ...");
//		if (chb < 0) {
//			chb += 256;
//			if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" - incremented to " + ((int) chb));
//		}
//		Integer chi = new Integer(chb);
//		Character mCh = ((Character) this.charMappings.get(chi));
//		if (mCh != null) {
//			if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> resolved to " + mCh.charValue());
//			return mCh.charValue();
//		}
//		if ((this.baseFont != null) && ((chb < this.firstChar) || (this.lastChar < chb))) {
//			char ch = this.baseFont.resolveByte(chb);
//			if (ch != 0) {
//				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> base font out of own range resolved to " + ch);
//				return ch;
//			}
//		}
//		if ("AdobeStandardEncoding".equals(this.encoding)) {
//			mCh = ((Character) standardAdobeMappings.get(chi));
//			if (mCh != null) {
//				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> Adobe resolved to " + mCh.charValue());
//				return mCh.charValue();
//			}
//			else if ((this.firstChar <= chb) && (chb <= this.lastChar)) {
//				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> Adobe default resolved to " + ((char) chb));
//				return ((char) chb);
//			}
//		}
//		else if ("MacRomanEncoding".equals(this.encoding)) {
//			mCh = ((Character) macRomanMappings.get(chi));
//			if (mCh != null) {
//				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> MacRoman resolved to " + mCh.charValue());
//				return mCh.charValue();
//			}
//			else if ((this.firstChar <= chb) && (chb <= this.lastChar)) {
//				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> MacRoman default resolved to " + ((char) chb));
//				return ((char) chb);
//			}
//		}
//		else if ("MacExpertEncoding".equals(this.encoding)) {
//			mCh = ((Character) macExpertMappings.get(chi));
//			if (mCh != null) {
//				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> MacExpert resolved to " + mCh.charValue());
//				return mCh.charValue();
//			}
//			else if ((this.firstChar <= chb) && (chb <= this.lastChar)) {
//				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> MacExpert default resolved to " + ((char) chb));
//				return ((char) chb);
//			}
//		}
//		else if ("WinAnsiEncoding".equals(this.encoding) || (this.encoding == null)) {
//			mCh = ((Character) winAnsiMappings.get(chi));
//			if (mCh != null) {
//				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> ANSI resolved to " + mCh.charValue());
//				return mCh.charValue();
//			}
//			else if ((this.firstChar <= chb) && (chb <= this.lastChar)) {
//				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> ANSI default resolved to " + ((char) chb));
//				return ((char) chb);
//			}
//		}
//		if (this.baseFont != null) {
//			char ch = this.baseFont.resolveByte(chb);
//			if (ch != 0) {
//				if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> base font resolved to " + ch);
//				return ch;
//			}
//		}
//		if (DEBUG_CHAR_HANDLING || (127 < chb)) System.out.println(" --> ASCII resolved to " + ((char) chb));
//		return ((char) chb);
//	}
//	void mapChar(Integer ch, Character mCh) {
//		this.charMappings.put(ch, mCh);
//		if (DEBUG_CHAR_HANDLING)
//			System.out.println("Mapped " + ch + " to '" + mCh + "'");
//		this.rCharMappings.put(mCh, ch);
//	}
//	float getCharWidth(char ch) {
//		if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println("Getting width for '" + ch + "' (" + ((int) ch) + ")");
//		float cw = 0;
//		if (cw == 0) {
//			Float cwObj = ((Float) this.nCharWidths.get(new Character(ch)));
//			if (cwObj != null) {
//				cw = cwObj.floatValue();
//				if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - width from own mapping: " + cw);
//			}
//		}
//		if ((cw == 0) && (this.baseFont != null)) {
//			cw = this.baseFont.getCharWidth(ch);
//			if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - width from base font: " + cw);
//		}
//		if ((cw == 0) && (this.firstChar <= ch) && (ch <= this.lastChar)) {
//			cw = this.charWidths[((int) ch) - this.firstChar];
//			if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - width from base array: " + cw);
//		}
////			if ((cw == 0) && (this.baseFont != null)) {
////				cw = this.baseFont.getCharWidth(ch);
////				if (DEBUG_CHAR_HANDLING) System.out.println(" - width from base font: " + cw);
////			}
//		if ((cw == 0) && this.rCharMappings.containsKey(new Character((char) ch))) {
//			Integer rmCh = ((Integer) this.rCharMappings.get(new Character((char) ch)));
//			if ((rmCh != null) && (rmCh.intValue() != ch)) {
//				if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - recursing with reverse mapped CID");
//				return this.getCharWidth((char) rmCh.intValue());
//			}
//		}
//		if (cw == 0) {
//			cw = this.mCharWidth;
//			if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" - fallback missing width: " + cw);
//		}
//		if (DEBUG_CHAR_HANDLING || (127 < ch)) System.out.println(" --> width is " + cw);
//		return cw;
//	}
//	void setNamedCharWidth(Character ch, float cw) {
//		this.nCharWidths.put(ch, new Float(cw));
//	}
	float getRelativeBaselineShift(char ch) {
		Float bls = ((Float) this.charBaselineShifts.get(new Character(ch)));
		return ((bls == null) ? 0 : bls.floatValue());
	}
	float getRelativeBaselineShift(String str) {
		float blsUp = 0;
		float blsDown = 0;
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			if (Character.isWhitespace(ch))
				continue;
			float chBls = this.getRelativeBaselineShift(ch);
			blsUp = Math.max(blsUp, chBls);
			blsDown = Math.min(blsDown, chBls);
		}
		return ((blsDown == 0) ? blsUp : blsDown);
	}
	void setRelativeBaselineShift(Character ch, float bls) {
		this.charBaselineShifts.put(ch, new Float(bls));
	}
	private static HashMap winAnsiMappings = new HashMap();
	static {
		winAnsiMappings.put(new Integer(128), new Character('\u20AC'));
		
		winAnsiMappings.put(new Integer(130), new Character('\u201A'));
		winAnsiMappings.put(new Integer(131), new Character('\u0192'));
		winAnsiMappings.put(new Integer(132), new Character('\u201E'));
		winAnsiMappings.put(new Integer(133), new Character('\u2026'));
		winAnsiMappings.put(new Integer(134), new Character('\u2020'));
		winAnsiMappings.put(new Integer(135), new Character('\u2021'));
		winAnsiMappings.put(new Integer(136), new Character('\u02C6'));
		winAnsiMappings.put(new Integer(137), new Character('\u2030'));
		winAnsiMappings.put(new Integer(138), new Character('\u0160'));
		winAnsiMappings.put(new Integer(139), new Character('\u2039'));
		winAnsiMappings.put(new Integer(140), new Character('\u0152'));
		
		winAnsiMappings.put(new Integer(142), new Character('\u017D'));
		
		winAnsiMappings.put(new Integer(145), new Character('\u2018'));
		winAnsiMappings.put(new Integer(146), new Character('\u2019'));
		winAnsiMappings.put(new Integer(147), new Character('\u201C'));
		winAnsiMappings.put(new Integer(148), new Character('\u201D'));
		winAnsiMappings.put(new Integer(149), new Character('\u2022'));
		winAnsiMappings.put(new Integer(150), new Character('\u2013'));
		winAnsiMappings.put(new Integer(151), new Character('\u2014'));
		winAnsiMappings.put(new Integer(152), new Character('\u02DC'));
		winAnsiMappings.put(new Integer(153), new Character('\u2122'));
		winAnsiMappings.put(new Integer(154), new Character('\u0161'));
		winAnsiMappings.put(new Integer(155), new Character('\u203A'));
		winAnsiMappings.put(new Integer(156), new Character('\u0153'));
		
		winAnsiMappings.put(new Integer(158), new Character('\u017E'));
		winAnsiMappings.put(new Integer(159), new Character('\u0178'));
	}
	
	private static HashMap macRomanMappings = new HashMap();
	static {
		macRomanMappings.put(new Integer(128), new Character('\u00C4'));
		macRomanMappings.put(new Integer(129), new Character('\u00C5'));
		macRomanMappings.put(new Integer(130), new Character('\u00C7'));
		macRomanMappings.put(new Integer(131), new Character('\u00C9'));
		macRomanMappings.put(new Integer(132), new Character('\u00D1'));
		macRomanMappings.put(new Integer(133), new Character('\u00D6'));
		macRomanMappings.put(new Integer(134), new Character('\u00DC'));
		macRomanMappings.put(new Integer(135), new Character('\u00E1'));
		macRomanMappings.put(new Integer(136), new Character('\u00E0'));
		macRomanMappings.put(new Integer(137), new Character('\u00E2'));
		macRomanMappings.put(new Integer(138), new Character('\u00E4'));
		macRomanMappings.put(new Integer(139), new Character('\u00E3'));
		macRomanMappings.put(new Integer(140), new Character('\u00E5'));
		macRomanMappings.put(new Integer(141), new Character('\u00E7'));
		macRomanMappings.put(new Integer(142), new Character('\u00E9'));
		macRomanMappings.put(new Integer(143), new Character('\u00E8'));
		
		macRomanMappings.put(new Integer(144), new Character('\u00EA'));
		macRomanMappings.put(new Integer(145), new Character('\u00EB'));
		macRomanMappings.put(new Integer(146), new Character('\u00ED'));
		macRomanMappings.put(new Integer(147), new Character('\u00EC'));
		macRomanMappings.put(new Integer(148), new Character('\u00EE'));
		macRomanMappings.put(new Integer(149), new Character('\u00EF'));
		macRomanMappings.put(new Integer(150), new Character('\u00F1'));
		macRomanMappings.put(new Integer(151), new Character('\u00F3'));
		macRomanMappings.put(new Integer(152), new Character('\u00F2'));
		macRomanMappings.put(new Integer(153), new Character('\u00F4'));
		macRomanMappings.put(new Integer(154), new Character('\u00F6'));
		macRomanMappings.put(new Integer(155), new Character('\u00F5'));
		macRomanMappings.put(new Integer(156), new Character('\u00FA'));
		macRomanMappings.put(new Integer(157), new Character('\u00F9'));
		macRomanMappings.put(new Integer(158), new Character('\u00FB'));
		macRomanMappings.put(new Integer(159), new Character('\u00FC'));
		
		macRomanMappings.put(new Integer(160), new Character('\u2020'));
		macRomanMappings.put(new Integer(161), new Character('\u00B0'));
		macRomanMappings.put(new Integer(162), new Character('\u00A2'));
		macRomanMappings.put(new Integer(163), new Character('\u00A3'));
		macRomanMappings.put(new Integer(164), new Character('\u00A7'));
		macRomanMappings.put(new Integer(165), new Character('\u2022'));
		macRomanMappings.put(new Integer(166), new Character('\u00B6'));
		macRomanMappings.put(new Integer(167), new Character('\u00DF'));
		macRomanMappings.put(new Integer(168), new Character('\u00AE'));
		macRomanMappings.put(new Integer(169), new Character('\u00A9'));
		macRomanMappings.put(new Integer(170), new Character('\u2122'));
		macRomanMappings.put(new Integer(171), new Character('\u00B4'));
		macRomanMappings.put(new Integer(172), new Character('\u00A8'));
		macRomanMappings.put(new Integer(173), new Character('\u2260'));
		macRomanMappings.put(new Integer(174), new Character('\u00C6'));
		macRomanMappings.put(new Integer(175), new Character('\u00D8'));
		
		macRomanMappings.put(new Integer(176), new Character('\u221E'));
		macRomanMappings.put(new Integer(177), new Character('\u00B1'));
		macRomanMappings.put(new Integer(178), new Character('\u2264'));
		macRomanMappings.put(new Integer(179), new Character('\u2265'));
		macRomanMappings.put(new Integer(180), new Character('\u00A5'));
		macRomanMappings.put(new Integer(181), new Character('\u00B5'));
		macRomanMappings.put(new Integer(182), new Character('\u2202'));
		macRomanMappings.put(new Integer(183), new Character('\u2211'));
		macRomanMappings.put(new Integer(184), new Character('\u220F'));
		macRomanMappings.put(new Integer(185), new Character('\u03C0'));
		macRomanMappings.put(new Integer(186), new Character('\u222B'));
		macRomanMappings.put(new Integer(187), new Character('\u00AA'));
		macRomanMappings.put(new Integer(188), new Character('\u00BA'));
		macRomanMappings.put(new Integer(189), new Character('\u03A9'));
		macRomanMappings.put(new Integer(190), new Character('\u00E6'));
		macRomanMappings.put(new Integer(191), new Character('\u00F8'));
		
		macRomanMappings.put(new Integer(192), new Character('\u00BF'));
		macRomanMappings.put(new Integer(193), new Character('\u00A1'));
		macRomanMappings.put(new Integer(194), new Character('\u00AC'));
		macRomanMappings.put(new Integer(195), new Character('\u221A'));
		macRomanMappings.put(new Integer(196), new Character('\u0192'));
		macRomanMappings.put(new Integer(197), new Character('\u2248'));
		macRomanMappings.put(new Integer(198), new Character('\u2206'));
		macRomanMappings.put(new Integer(199), new Character('\u00AB'));
		macRomanMappings.put(new Integer(200), new Character('\u00BB'));
		macRomanMappings.put(new Integer(201), new Character('\u2026'));
		macRomanMappings.put(new Integer(202), new Character('\u00A0'));
		macRomanMappings.put(new Integer(203), new Character('\u00C0'));
		macRomanMappings.put(new Integer(204), new Character('\u00C3'));
		macRomanMappings.put(new Integer(205), new Character('\u00D5'));
		macRomanMappings.put(new Integer(206), new Character('\u0152'));
		macRomanMappings.put(new Integer(207), new Character('\u0153'));
		
		macRomanMappings.put(new Integer(208), new Character('\u2013'));
		macRomanMappings.put(new Integer(209), new Character('\u2014'));
		macRomanMappings.put(new Integer(210), new Character('\u201C'));
		macRomanMappings.put(new Integer(211), new Character('\u201D'));
		macRomanMappings.put(new Integer(212), new Character('\u2018'));
		macRomanMappings.put(new Integer(213), new Character('\u2019'));
		macRomanMappings.put(new Integer(214), new Character('\u00F7'));
		macRomanMappings.put(new Integer(215), new Character('\u25CA'));
		macRomanMappings.put(new Integer(216), new Character('\u00FF'));
		macRomanMappings.put(new Integer(217), new Character('\u0178'));
		macRomanMappings.put(new Integer(218), new Character('\u2044'));
		macRomanMappings.put(new Integer(219), new Character('\u20AC'));
		macRomanMappings.put(new Integer(220), new Character('\u2039'));
		macRomanMappings.put(new Integer(221), new Character('\u203A'));
		macRomanMappings.put(new Integer(222), new Character('\uFB01'));
		macRomanMappings.put(new Integer(223), new Character('\uFB02'));
		
		macRomanMappings.put(new Integer(224), new Character('\u2021'));
		macRomanMappings.put(new Integer(225), new Character('\u00B7'));
		macRomanMappings.put(new Integer(226), new Character('\u201A'));
		macRomanMappings.put(new Integer(227), new Character('\u201E'));
		macRomanMappings.put(new Integer(228), new Character('\u2030'));
		macRomanMappings.put(new Integer(229), new Character('\u00C2'));
		macRomanMappings.put(new Integer(230), new Character('\u00CA'));
		macRomanMappings.put(new Integer(231), new Character('\u00C1'));
		macRomanMappings.put(new Integer(232), new Character('\u00CB'));
		macRomanMappings.put(new Integer(233), new Character('\u00C8'));
		macRomanMappings.put(new Integer(234), new Character('\u00CD'));
		macRomanMappings.put(new Integer(235), new Character('\u00CE'));
		macRomanMappings.put(new Integer(236), new Character('\u00CF'));
		macRomanMappings.put(new Integer(237), new Character('\u00CC'));
		macRomanMappings.put(new Integer(238), new Character('\u00D3'));
		macRomanMappings.put(new Integer(239), new Character('\u00D4'));
		
		macRomanMappings.put(new Integer(240), new Character('\uF8FF'));
		macRomanMappings.put(new Integer(241), new Character('\u00D2'));
		macRomanMappings.put(new Integer(242), new Character('\u00DA'));
		macRomanMappings.put(new Integer(243), new Character('\u00DB'));
		macRomanMappings.put(new Integer(244), new Character('\u00D9'));
		macRomanMappings.put(new Integer(245), new Character('\u0131'));
		macRomanMappings.put(new Integer(246), new Character('\u02C6'));
		macRomanMappings.put(new Integer(247), new Character('\u02DC'));
		macRomanMappings.put(new Integer(248), new Character('\u00AF'));
		macRomanMappings.put(new Integer(249), new Character('\u02D8'));
		macRomanMappings.put(new Integer(250), new Character('\u02D9'));
		macRomanMappings.put(new Integer(251), new Character('\u02DA'));
		macRomanMappings.put(new Integer(252), new Character('\u00B8'));
		macRomanMappings.put(new Integer(253), new Character('\u02DD'));
		macRomanMappings.put(new Integer(254), new Character('\u02DB'));
		macRomanMappings.put(new Integer(255), new Character('\u02C7'));
	}
	
	private static HashMap macExpertMappings = new HashMap();
	static {
		macExpertMappings.put(new Integer(33), new Character('\uf721'));
		macExpertMappings.put(new Integer(34), new Character('\uf6f8'));
		macExpertMappings.put(new Integer(35), new Character('\uf7a2'));
		macExpertMappings.put(new Integer(36), new Character('\uf724'));
		macExpertMappings.put(new Integer(37), new Character('\uf6e4'));
		macExpertMappings.put(new Integer(38), new Character('\uf726'));
		macExpertMappings.put(new Integer(39), new Character('\uf7b4'));
		macExpertMappings.put(new Integer(40), new Character('\u207d'));
		macExpertMappings.put(new Integer(41), new Character('\u207e'));
		macExpertMappings.put(new Integer(43), new Character('\u2024'));
		macExpertMappings.put(new Integer(47), new Character('\u2044'));
		macExpertMappings.put(new Integer(49), new Character('\uf731'));
		macExpertMappings.put(new Integer(51), new Character('\uf733'));
		macExpertMappings.put(new Integer(52), new Character('\uf734'));
		macExpertMappings.put(new Integer(53), new Character('\uf735'));
		macExpertMappings.put(new Integer(54), new Character('\uf736'));
		macExpertMappings.put(new Integer(55), new Character('\uf737'));
		macExpertMappings.put(new Integer(56), new Character('\uf738'));
		macExpertMappings.put(new Integer(57), new Character('\uf739'));
		macExpertMappings.put(new Integer(61), new Character('\uf6de'));
		macExpertMappings.put(new Integer(63), new Character('\uf73f'));
		macExpertMappings.put(new Integer(68), new Character('\uf7f0'));
		macExpertMappings.put(new Integer(71), new Character('\u00bc'));
		macExpertMappings.put(new Integer(72), new Character('\u00bd'));
		macExpertMappings.put(new Integer(73), new Character('\u00be'));
		macExpertMappings.put(new Integer(74), new Character('\u215b'));
		macExpertMappings.put(new Integer(75), new Character('\u215c'));
		macExpertMappings.put(new Integer(76), new Character('\u215d'));
		macExpertMappings.put(new Integer(77), new Character('\u215e'));
		macExpertMappings.put(new Integer(78), new Character('\u2153'));
		macExpertMappings.put(new Integer(86), new Character('\ufb00'));
		macExpertMappings.put(new Integer(87), new Character('\ufb01'));
		macExpertMappings.put(new Integer(88), new Character('\ufb02'));
		macExpertMappings.put(new Integer(89), new Character('\ufb03'));
		macExpertMappings.put(new Integer(90), new Character('\ufb04'));
		macExpertMappings.put(new Integer(91), new Character('\u208d'));
		macExpertMappings.put(new Integer(93), new Character('\u208e'));
		macExpertMappings.put(new Integer(94), new Character('\uf6f6'));
		macExpertMappings.put(new Integer(95), new Character('\uf6e5'));
		macExpertMappings.put(new Integer(96), new Character('\uf760'));
		macExpertMappings.put(new Integer(97), new Character('\uf761'));
		macExpertMappings.put(new Integer(98), new Character('\uf762'));
		macExpertMappings.put(new Integer(99), new Character('\uf763'));
		macExpertMappings.put(new Integer(100), new Character('\uf764'));
		macExpertMappings.put(new Integer(101), new Character('\uf765'));
		macExpertMappings.put(new Integer(102), new Character('\uf766'));
		macExpertMappings.put(new Integer(103), new Character('\uf767'));
		macExpertMappings.put(new Integer(104), new Character('\uf768'));
		macExpertMappings.put(new Integer(105), new Character('\uf769'));
		macExpertMappings.put(new Integer(106), new Character('\uf76a'));
		macExpertMappings.put(new Integer(107), new Character('\uf76b'));
		macExpertMappings.put(new Integer(108), new Character('\uf76c'));
		macExpertMappings.put(new Integer(109), new Character('\uf76d'));
		macExpertMappings.put(new Integer(110), new Character('\uf76e'));
		macExpertMappings.put(new Integer(111), new Character('\uf76f'));
		macExpertMappings.put(new Integer(112), new Character('\uf770'));
		macExpertMappings.put(new Integer(113), new Character('\uf771'));
		macExpertMappings.put(new Integer(114), new Character('\uf772'));
		macExpertMappings.put(new Integer(115), new Character('\uf773'));
		macExpertMappings.put(new Integer(116), new Character('\uf774'));
		macExpertMappings.put(new Integer(117), new Character('\uf775'));
		macExpertMappings.put(new Integer(118), new Character('\uf776'));
		macExpertMappings.put(new Integer(119), new Character('\uf777'));
		macExpertMappings.put(new Integer(120), new Character('\uf778'));
		macExpertMappings.put(new Integer(121), new Character('\uf779'));
		macExpertMappings.put(new Integer(122), new Character('\uf77a'));
		macExpertMappings.put(new Integer(123), new Character('\u20a1'));
		macExpertMappings.put(new Integer(124), new Character('\uf6dc'));
		macExpertMappings.put(new Integer(125), new Character('\uf6dd'));
		macExpertMappings.put(new Integer(126), new Character('\uf6fe'));
		macExpertMappings.put(new Integer(129), new Character('\uf6e9'));
		macExpertMappings.put(new Integer(130), new Character('\uf6e0'));
		macExpertMappings.put(new Integer(135), new Character('\uf7e1'));
		macExpertMappings.put(new Integer(136), new Character('\uf7e0'));
		macExpertMappings.put(new Integer(137), new Character('\uf7e2'));
		macExpertMappings.put(new Integer(138), new Character('\uf7e4'));
		macExpertMappings.put(new Integer(139), new Character('\uf7e3'));
		macExpertMappings.put(new Integer(140), new Character('\uf7e5'));
		macExpertMappings.put(new Integer(141), new Character('\uf7e7'));
		macExpertMappings.put(new Integer(142), new Character('\uf7e9'));
		macExpertMappings.put(new Integer(143), new Character('\uf7e8'));
		macExpertMappings.put(new Integer(144), new Character('\uf7ea'));
		macExpertMappings.put(new Integer(145), new Character('\uf7eb'));
		macExpertMappings.put(new Integer(146), new Character('\uf7ed'));
		macExpertMappings.put(new Integer(147), new Character('\uf7ec'));
		macExpertMappings.put(new Integer(148), new Character('\uf7ee'));
		macExpertMappings.put(new Integer(149), new Character('\uf7ef'));
		macExpertMappings.put(new Integer(150), new Character('\uf7f1'));
		macExpertMappings.put(new Integer(151), new Character('\uf7f3'));
		macExpertMappings.put(new Integer(152), new Character('\uf7f2'));
		macExpertMappings.put(new Integer(153), new Character('\uf7f4'));
		macExpertMappings.put(new Integer(154), new Character('\uf7f6'));
		macExpertMappings.put(new Integer(155), new Character('\uf7f5'));
		macExpertMappings.put(new Integer(156), new Character('\uf7fa'));
		macExpertMappings.put(new Integer(157), new Character('\uf7f9'));
		macExpertMappings.put(new Integer(158), new Character('\uf7fb'));
		macExpertMappings.put(new Integer(159), new Character('\uf7fc'));
		macExpertMappings.put(new Integer(161), new Character('\u2078'));
		macExpertMappings.put(new Integer(162), new Character('\u2084'));
		macExpertMappings.put(new Integer(163), new Character('\u2083'));
		macExpertMappings.put(new Integer(164), new Character('\u2086'));
		macExpertMappings.put(new Integer(165), new Character('\u2088'));
		macExpertMappings.put(new Integer(166), new Character('\u2087'));
		macExpertMappings.put(new Integer(167), new Character('\uf6fd'));
		macExpertMappings.put(new Integer(169), new Character('\uf6df'));
		macExpertMappings.put(new Integer(172), new Character('\uf7a8'));
		macExpertMappings.put(new Integer(174), new Character('\uf6f5'));
		macExpertMappings.put(new Integer(175), new Character('\uf6f0'));
		macExpertMappings.put(new Integer(176), new Character('\u2085'));
		macExpertMappings.put(new Integer(178), new Character('\uf6e1'));
		macExpertMappings.put(new Integer(179), new Character('\uf6e7'));
		macExpertMappings.put(new Integer(180), new Character('\uf7fd'));
		macExpertMappings.put(new Integer(182), new Character('\uf6e3'));
		macExpertMappings.put(new Integer(185), new Character('\uf7fe'));
		macExpertMappings.put(new Integer(187), new Character('\u2089'));
		macExpertMappings.put(new Integer(189), new Character('\uf6ff'));
		macExpertMappings.put(new Integer(190), new Character('\uf7e6'));
		macExpertMappings.put(new Integer(191), new Character('\uf7f8'));
		macExpertMappings.put(new Integer(192), new Character('\uf7bf'));
		macExpertMappings.put(new Integer(193), new Character('\u2081'));
		macExpertMappings.put(new Integer(194), new Character('\uf6f9'));
		macExpertMappings.put(new Integer(201), new Character('\uf7b8'));
		macExpertMappings.put(new Integer(207), new Character('\uf6fa'));
		macExpertMappings.put(new Integer(208), new Character('\u2012'));
		macExpertMappings.put(new Integer(209), new Character('\uf6e6'));
		macExpertMappings.put(new Integer(214), new Character('\uf7a1'));
		macExpertMappings.put(new Integer(216), new Character('\uf7ff'));
		macExpertMappings.put(new Integer(218), new Character('\u00b9'));
		macExpertMappings.put(new Integer(221), new Character('\u2074'));
		macExpertMappings.put(new Integer(222), new Character('\u2075'));
		macExpertMappings.put(new Integer(223), new Character('\u2076'));
		macExpertMappings.put(new Integer(224), new Character('\u2077'));
		macExpertMappings.put(new Integer(225), new Character('\u2079'));
		macExpertMappings.put(new Integer(228), new Character('\uf6ec'));
		macExpertMappings.put(new Integer(229), new Character('\uf6f1'));
		macExpertMappings.put(new Integer(233), new Character('\uf6ed'));
		macExpertMappings.put(new Integer(234), new Character('\uf6f2'));
		macExpertMappings.put(new Integer(235), new Character('\uf6eb'));
		macExpertMappings.put(new Integer(241), new Character('\uf6ee'));
		macExpertMappings.put(new Integer(242), new Character('\uf6fb'));
		macExpertMappings.put(new Integer(243), new Character('\uf6f4'));
		macExpertMappings.put(new Integer(244), new Character('\uf7af'));
		macExpertMappings.put(new Integer(245), new Character('\uf6ea'));
		macExpertMappings.put(new Integer(246), new Character('\u207f'));
		macExpertMappings.put(new Integer(247), new Character('\uf6ef'));
		macExpertMappings.put(new Integer(248), new Character('\uf6e2'));
		macExpertMappings.put(new Integer(249), new Character('\uf6e8'));
		macExpertMappings.put(new Integer(250), new Character('\uf6f7'));
		macExpertMappings.put(new Integer(251), new Character('\uf6fc'));
	}
	
	private static HashMap standardAdobeMappings = new HashMap();
	static {
		standardAdobeMappings.put(new Integer(39), new Character('\u2019'));
		
		standardAdobeMappings.put(new Integer(45), new Character('\u00AD'));
		
		standardAdobeMappings.put(new Integer(96), new Character('\u2018'));
		
		standardAdobeMappings.put(new Integer(164), new Character('\u2044'));
		standardAdobeMappings.put(new Integer(164), new Character('\u2215'));
		standardAdobeMappings.put(new Integer(166), new Character('\u0192'));
		standardAdobeMappings.put(new Integer(168), new Character('\u00A4'));
		standardAdobeMappings.put(new Integer(169), new Character('\''));
		standardAdobeMappings.put(new Integer(170), new Character('\u201C'));
		standardAdobeMappings.put(new Integer(172), new Character('\u2039'));
		standardAdobeMappings.put(new Integer(173), new Character('\u203A'));
		standardAdobeMappings.put(new Integer(174), new Character('\uFB01'));
		standardAdobeMappings.put(new Integer(175), new Character('\uFB02'));
		
		standardAdobeMappings.put(new Integer(177), new Character('\u2013'));
		standardAdobeMappings.put(new Integer(178), new Character('\u2020'));
		standardAdobeMappings.put(new Integer(179), new Character('\u2021'));
		standardAdobeMappings.put(new Integer(180), new Character('\u00B7'));
		standardAdobeMappings.put(new Integer(180), new Character('\u2219'));
		standardAdobeMappings.put(new Integer(183), new Character('\u2022'));
		standardAdobeMappings.put(new Integer(184), new Character('\u201A'));
		standardAdobeMappings.put(new Integer(185), new Character('\u201E'));
		standardAdobeMappings.put(new Integer(186), new Character('\u201D'));
		standardAdobeMappings.put(new Integer(188), new Character('\u2026'));
		standardAdobeMappings.put(new Integer(189), new Character('\u2030'));
		
		standardAdobeMappings.put(new Integer(193), new Character('\u0060'));
		standardAdobeMappings.put(new Integer(194), new Character('\u00B4'));
		standardAdobeMappings.put(new Integer(195), new Character('\u02C6'));
		standardAdobeMappings.put(new Integer(196), new Character('\u02DC'));
		standardAdobeMappings.put(new Integer(197), new Character('\u00AF'));
		standardAdobeMappings.put(new Integer(197), new Character('\u02C9'));
		standardAdobeMappings.put(new Integer(198), new Character('\u02D8'));
		standardAdobeMappings.put(new Integer(199), new Character('\u02D9'));
		standardAdobeMappings.put(new Integer(200), new Character('\u00A8'));
		standardAdobeMappings.put(new Integer(202), new Character('\u02DA'));
		standardAdobeMappings.put(new Integer(203), new Character('\u00B8'));
		standardAdobeMappings.put(new Integer(205), new Character('\u02DD'));
		standardAdobeMappings.put(new Integer(206), new Character('\u02DB'));
		standardAdobeMappings.put(new Integer(207), new Character('\u02C7'));
		
		standardAdobeMappings.put(new Integer(208), new Character('\u2014'));
		
		standardAdobeMappings.put(new Integer(225), new Character('\u00C6'));
		standardAdobeMappings.put(new Integer(227), new Character('\u00AA'));
		standardAdobeMappings.put(new Integer(232), new Character('\u0141'));
		standardAdobeMappings.put(new Integer(233), new Character('\u00D8'));
		standardAdobeMappings.put(new Integer(234), new Character('\u0152'));
		standardAdobeMappings.put(new Integer(235), new Character('\u00BA'));
		
		standardAdobeMappings.put(new Integer(241), new Character('\u00E6'));
		standardAdobeMappings.put(new Integer(245), new Character('\u0131'));
		standardAdobeMappings.put(new Integer(248), new Character('\u0142'));
		standardAdobeMappings.put(new Integer(249), new Character('\u00F8'));
		standardAdobeMappings.put(new Integer(250), new Character('\u0153'));
		standardAdobeMappings.put(new Integer(251), new Character('\u00DF'));
	}
	
	static final boolean DEBUG_LOAD_FONTS = true;
	
	static PdfFont readFont(Object fnObj, Hashtable fontData, HashMap objects, boolean needChars) throws IOException {
		Object ftObj = fontData.get("Subtype");
		if (ftObj == null) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> missing font type");
			}
			return null;
		}
		
		if ("Type0".equals(ftObj.toString()))
			return readFontType0(fontData, objects);
		
		if ("Type1".equals(ftObj.toString()))
			return readFontType1(fontData, objects);
		
		if ("Type3".equals(ftObj.toString()))
			return readFontType3(fnObj, fontData, objects);
		
		if ("TrueType".equals(ftObj.toString()))
			return readFontTrueType(fontData, objects);
		
		if ("CIDFontType2".equals(ftObj.toString()))
			return readFontCidType2(fontData, objects, needChars);
		
		if (DEBUG_LOAD_FONTS) {
			System.out.println("Problem in font " + fontData);
			System.out.println(" --> unknown font type");
		}
		return null;
	}
	
	private static PdfFont readFontType0(Hashtable fontData, HashMap objects) throws IOException {
		Object dfaObj = PdfParser.dereference(fontData.get("DescendantFonts"), objects);
		if (!(dfaObj instanceof Vector) || (((Vector) dfaObj).size() != 1)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange font descriptor: " + dfaObj);
			}
			return null;
		}
		
		Object tuObj = PdfParser.dereference(fontData.get("ToUnicode"), objects);
		HashMap toUnicodeMappings = null;
		if (tuObj instanceof PStream) {
			Object filter = ((PStream) tuObj).params.get("Filter");
			if (filter == null)
				filter = "FlateDecode";
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PdfParser.decode(filter, ((PStream) tuObj).bytes, ((PStream) tuObj).params, baos, objects);
			byte[] tuMapData = baos.toByteArray();
			if (DEBUG_LOAD_FONTS)
				System.out.println(" --> to unicode: " + new String(tuMapData));
			toUnicodeMappings = readToUnicodeCMap(tuMapData, true);
			//	TODO if we have this mapping, we can use it right away, no need to bother with rendering and recognizing the characters
			//	- char widths are in descendant font dictionary, entry W
			//	- missing width is in descendant font dictionary, entry DW
			//PdfFont pFont = new PdfFont(data, descriptor, firstChar, lastChar, charWidths, mCharWidth, name, encoding);
		}
		else System.out.println(" --> to unicode: " + tuObj);
		
		Object dfObj = PdfParser.dereference(((Vector) dfaObj).get(0), objects);
		if (!(dfObj instanceof Hashtable)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange descendant font: " + dfObj);
			}
			return null;
		}
		System.out.println(" --> descendant font: " + dfObj);
		
		PdfFont dFont = readFont(null, ((Hashtable) dfObj), objects, (toUnicodeMappings == null));
		if (dFont == null) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + dfObj);
				System.out.println(" --> could not load descendant font");
			}
			return null;
		}
		
		Object fnObj = PdfParser.dereference(fontData.get("BaseFont"), objects);
		if (fnObj == null) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> missing font name");
			}
			return null;
		}
		
		Object feObj = PdfParser.dereference(fontData.get("Encoding"), objects);
		if (feObj instanceof Hashtable)
			feObj = PdfParser.dereference(((Hashtable) feObj).get("BaseEncoding"), objects);
		
		PdfFont pFont = new PdfFont(fontData, dFont.descriptor, dFont.firstChar, dFont.lastChar, dFont.charWidths, dFont.mCharWidth, fnObj.toString(), ((feObj == null) ? null : feObj.toString()));
		pFont.ascent = dFont.ascent;
		pFont.descent = dFont.descent;
		pFont.capHeigth = dFont.capHeigth;
		
		pFont.bold = dFont.bold;
		pFont.italic = dFont.italic;
		
		pFont.setBaseFont(dFont.baseFont);
		
		pFont.ccWidths.putAll(dFont.ccWidths);
		pFont.cWidths.putAll(dFont.cWidths);
		
		if (toUnicodeMappings == null) {
			//	TODO transfer special mappings from dFont
		}
		else for (Iterator cmit = toUnicodeMappings.keySet().iterator(); cmit.hasNext();) {
			Integer chc = ((Integer) cmit.next());
			pFont.mapUnicode(chc, ((String) toUnicodeMappings.get(chc)));
		}
		
		return pFont;
	}
	
	private static HashMap readToUnicodeCMap(byte[] cMap, boolean isHex) throws IOException {
		HashMap tuc = new HashMap();
		PeekInputStream bytes = new PeekInputStream(new ByteArrayInputStream(cMap));
		while (bytes.peek() != -1) {
			bytes.skipSpace();
			byte[] lookahead = getLookahead(bytes, 12);
			if (lookahead == null)
				break;
			if (PdfUtils.startsWith(lookahead, "beginbfchar", 0)) {
				bytes.skip("beginbfchar".length());
				cropBfCharMapping(bytes, isHex, tuc);
			}
			else if (PdfUtils.startsWith(lookahead, "beginbfrange", 0)) {
				bytes.skip("beginbfrange".length());
				cropBfCharMappingRange(bytes, isHex, tuc);
			}
			else PdfParser.cropNext(bytes, true, false);
		}
		return tuc;
		//	TODO also read PostScript char names
	}
	
	private static byte[] getLookahead(PeekInputStream bytes, int length) throws IOException {
		byte[] lookahead = new byte[length];
		bytes.mark(lookahead.length);
		int l = bytes.read(lookahead);
		bytes.reset();
		return ((l < lookahead.length) ? null : lookahead);
	}
	
	/*
	n beginbfchar
	srcCode dstString
	endbfchar
	 */
	private static void cropBfCharMapping(PeekInputStream bytes, boolean isHex, HashMap tuc) throws IOException {
		while (bytes.peek() != -1) {
			Integer ch = null;
			Object srcObj = PdfParser.cropNext(bytes, true, true);
			if ((srcObj instanceof PtTag) && "endbfchar".equals(((PtTag) srcObj).tag))
				break;
			else if (srcObj instanceof PString) {
				PString src = ((PString) srcObj);
				if (src.length() == 1) // given as Hex2
					ch = new Integer(src.charAt(0));
				else if (src.length() == 2)// given as Hex4
					ch = new Integer((256 * src.charAt(0)) + src.charAt(1));
			}
			else if (srcObj instanceof Number)
				ch = new Integer(((Number) srcObj).intValue());
			
			String ucStr = null;
			Object destObj = PdfParser.cropNext(bytes, false, false);
			if (destObj instanceof PString) {
				PString dest = ((PString) destObj);
				if (dest.length() != 0) {
					if (dest.isHexWithSpace)
						ucStr = ("" + dest.charAt(0));
					else ucStr = dest.toString();
				}
			}
			else if (destObj instanceof Number)
				ucStr = ("" + ((char) ((Number) destObj).intValue()));
			else if (destObj instanceof Name) {
				char uch = StringUtils.getCharForName(((Name) destObj).toString());
				if (uch != 0)
					ucStr = ("" + StringUtils.getNormalForm(uch));
			}
			
			if ((ch != null) && (ucStr != null)) {
				if (DEBUG_LOAD_FONTS) 
					System.out.println(" - mapped (s) '" + ch.intValue() + "' to '" + ucStr + "'" + ((ucStr.length() == 1) ? (" (" + ((int) ucStr.charAt(0)) + ")") : ""));
				tuc.put(ch, ucStr);
			}
		}
	}
	
	/*
	n beginbfrange
	srcCode1 srcCode2 dstString
	endbfrange
	
	n beginbfrange
	srcCode1 srcCoden [dstString1 dstString2 … dstStringn]
	endbfrange
	 */	
	private static void cropBfCharMappingRange(PeekInputStream bytes, boolean isHex, HashMap tuc) throws IOException {
		while (bytes.peek() != -1) {
			int fch = -1;
			Object fSrcObj = PdfParser.cropNext(bytes, true, true);
			if ((fSrcObj instanceof PtTag) && "endbfrange".equals(((PtTag) fSrcObj).tag))
				break;
			else if (fSrcObj instanceof PString) {
				PString src = ((PString) fSrcObj);
				if (src.length() == 1) // given as Hex2
					fch = ((int) src.charAt(0));
				else if (src.length() == 2) // given as Hex4
					fch = ((int) ((256 * src.charAt(0)) + src.charAt(1)));
			}
			else if (fSrcObj instanceof Number)
				fch = ((Number) fSrcObj).intValue();
			
			int lch = -1;
			Object lSrcObj = PdfParser.cropNext(bytes, false, true);
			if (lSrcObj instanceof PString) {
				PString src = ((PString) lSrcObj);
				if (src.length() == 1) // given as Hex2
					lch =((int) src.charAt(0));
				else if (src.length() == 2) // given as Hex4
					lch = ((int) ((256 * src.charAt(0)) + src.charAt(1)));
			}
			else if (lSrcObj instanceof Number)
				lch = ((Number) lSrcObj).intValue();
			
			int mch = -1;
			Object destObj = PdfParser.cropNext(bytes, false, false);
			
			if (destObj instanceof PString) {
				PString dest = ((PString) destObj);
				if (dest.length() != 0)
					mch = ((int) dest.charAt(0));
			}
			else if (destObj instanceof Number)
				mch = ((Number) destObj).intValue();
			
			if ((fch == -1) || (lch == -1))
				continue;
			
			if (mch != -1) {
				for (int c = fch; c <= lch; c++) {
					if (DEBUG_LOAD_FONTS) 
						System.out.println(" - mapped (r) '" + c + "' to '" + ((char) mch) + "'");
					tuc.put(new Integer(c), ("" + ((char) mch++)));
				}
				continue;
			}
			
			if (destObj instanceof Vector) {
				Vector dest = ((Vector) destObj);
				for (int c = fch; (c <= lch) && ((c-fch) < dest.size()); c++) {
					Object dObj = dest.get(c-fch);
					if (dObj instanceof PString) {
						PString d = ((PString) dObj);
						if (d.length() != 0) {
							if (DEBUG_LOAD_FONTS) 
								System.out.println(" - mapped (a) '" + c + "' to '" + d.toString() + "'");
							tuc.put(new Integer(c), d.toString());
						}
					}
					else if (dObj instanceof Number) {
						if (DEBUG_LOAD_FONTS) 
							System.out.println(" - mapped (a) '" + c + "' to '" + ((char) ((Number) dObj).intValue()) + "'");
						tuc.put(new Integer(c), ("" + ((char) ((Number) dObj).intValue())));
					}
				}
			}
		}
	}
	
	private static PdfFont readFontCidType2(Hashtable fontData, HashMap objects, boolean needChars) throws IOException {
		Object fdObj = PdfParser.dereference(fontData.get("FontDescriptor"), objects);
		if (!(fdObj instanceof Hashtable)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange font descriptor: " + fdObj);
			}
			return null;
		}
		if (DEBUG_LOAD_FONTS) {
			System.out.println("Got CID Type 2 font descriptor:" + fdObj);
		}
		
		Object fnObj = PdfParser.dereference(fontData.get("BaseFont"), objects);
		if (fnObj == null) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> missing font name");
			}
			return null;
		}
		
		BaseFont baseFont = getBuiltInFont(fnObj.toString().substring(7), false);
		if (baseFont == null) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> could not load base font " + fnObj.toString().substring(7));
			}
			return null;
		}
		
		Hashtable fd = ((Hashtable) fdObj);
		
		int fc = -1;
		int lc = -1;
		float[] cws = new float[0];
		Object dwObj = fd.get("DW");
		float mcw = ((dwObj instanceof Number) ? ((Number) dwObj).floatValue() : 1000);
		
		Object wObj = PdfParser.dereference(fontData.get("W"), objects);
		if (DEBUG_LOAD_FONTS)
			System.out.println(" --> width object: " + wObj);
		HashMap widths = new HashMap();
		if ((wObj instanceof Vector) && (((Vector) wObj).size() >= 2)) {
			Vector w = ((Vector) wObj);
			ArrayList cwList = new ArrayList();
			for (int i = 0; i < (w.size()-1);) {
				Object fcObj = w.get(i++);
				if (!(fcObj instanceof Number))
					continue;
				
				int rfc = ((Number) fcObj).intValue();
				if (fc == -1)
					fc = rfc;
				else while (lc < rfc) {
					cwList.add(new Float(0));
					lc++;
				}
				if (DEBUG_LOAD_FONTS)
					System.out.println("   - range first char is " + rfc);
				
				Object lcObj = w.get(i++);
				if (DEBUG_LOAD_FONTS)
					System.out.println("   - range last char object is " + lcObj);
				if ((lcObj instanceof Number) && (i < w.size())) {
					lc = ((Number) lcObj).intValue();
					if (DEBUG_LOAD_FONTS)
						System.out.println("   - (new) last char is " + lc);
					
					Object rwObj = w.get(i++);
					if (DEBUG_LOAD_FONTS)
						System.out.println("   - range width object is " + rwObj);
					if (rwObj instanceof Number) {
						Float rw = new Float(((Number) rwObj).floatValue());
						for (int c = rfc; c <= lc; c++) {
							cwList.add(rw);
							widths.put(new Integer(c), rw);
						}
					}
				}
				else if (lcObj instanceof Vector) {
					Vector ws = ((Vector) lcObj);
					for (int c = 0; c < ws.size(); c++) {
						Object cwObj = ws.get(c);
						if (cwObj instanceof Number) {
							Float cw = new Float(((Number) cwObj).floatValue());
							cwList.add(cw);
							widths.put(new Integer(rfc + c), cw);
						}
						else cwList.add(new Float(0));
						if (c != 0)
							lc++;
					}
				}
			}
			cws = new float[cwList.size()];
			for (int c = 0; c < cwList.size(); c++)
				cws[c] = ((Float) cwList.get(c)).floatValue();
		}
		
		/*
CIDSet=66 0R
FontFile2=65 0R
//FontFamily=Times New Roman
//FontStretch=Normal
//StemV=82
//FontName=OGCEJG+TimesNewRomanPSMT
		 */
		
		Object fbbObj = fd.get("FontBBox");
		if (!(fbbObj instanceof Vector) || (((Vector) fbbObj).size() != 4)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange bounding box: " + fbbObj);
			}
			fbbObj = null;
		}
		
		Object aObj = fd.get("Ascent");
		if (!(aObj instanceof Number) || ((((Number) aObj).floatValue() == 0) && fd.containsKey("Ascender")))
			aObj = fd.get("Ascender");
		if (!(aObj instanceof Number) || (((Number) aObj).floatValue() == 0)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange ascent: " + aObj);
			}
			if ((fbbObj != null) && (((Vector) fbbObj).get(2) instanceof Number)) {
				aObj = ((Vector) fbbObj).get(2);
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> fallback from bounding box: " + aObj);
			}
			else {
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> could not determine ascent");
				return null;
			}
		}
		
		Object dObj = fd.get("Descent");
		if (!(dObj instanceof Number) || ((((Number) dObj).floatValue() == 0)) && fd.containsKey("Descender"))
			dObj = fd.get("Descender");
		if (!(dObj instanceof Number)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange descent: " + dObj);
			}
			if ((fbbObj != null) && (((Vector) fbbObj).get(0) instanceof Number)) {
				dObj = ((Vector) fbbObj).get(2);
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> fallback from bounding box: " + dObj);
			}
			else {
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> could not determine descent");
				return null;
			}
		}
		Object chObj = fd.get("CapHeight");
		if (!(chObj instanceof Number) || (((Number) chObj).floatValue() == 0)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange cap height: " + chObj);
				System.out.println(" --> fallback to ascent: " + aObj);
			}
			chObj = aObj;
		}
		
		Object cidsObj = PdfParser.dereference(fd.get("CIDSet"), objects);
		ArrayList cids = new ArrayList();
		if (cidsObj instanceof PStream) {
			Object filter = ((PStream) cidsObj).params.get("Filter");
			if (filter == null)
				filter = "FlateDecode";
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PdfParser.decode(filter, ((PStream) cidsObj).bytes, ((PStream) cidsObj).params, baos, objects);
			byte[] csBytes = baos.toByteArray();
			int cid = 0;
			for (int b = 0; b < csBytes.length; b++) {
				int bt = convertUnsigned(csBytes[b]);
				for (int i = 0; i < 8; i++) {
					if ((bt & 128) != 0) {
						if (DEBUG_LOAD_FONTS) 
							System.out.println(" - CID " + cid + " is set");
						cids.add(new Integer(cid));
					}
					bt <<= 1;
					cid++;
				}
			}
		}
		else System.out.println("Got CID set: " + cidsObj);
		
		//	add special mappings from font file
//		if (needChars) 
		{
			Object ff2Obj = PdfParser.dereference(fd.get("FontFile2"), objects);
			if (ff2Obj instanceof PStream) {
				System.out.println("Got font file 2");
				System.out.println("  --> params are " + ((PStream) ff2Obj).params);
				Object filter = ((PStream) ff2Obj).params.get("Filter");
				System.out.println("  --> filter is " + filter);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PdfParser.decode(filter, ((PStream) ff2Obj).bytes, ((PStream) ff2Obj).params, baos, objects);
				byte[] ffBytes = baos.toByteArray();
				Object l1Obj = ((PStream) ff2Obj).params.get("Length1");
				if (DEBUG_LOAD_FONTS) {
					System.out.println("Got font program type 2:");
					System.out.println(" - length is " + ffBytes.length + ", length1 is " + l1Obj);
					int ffByteOffset = 0;
					int scalerType = readUnsigned(ffBytes, ffByteOffset, (ffByteOffset+4));
					ffByteOffset += 4;
					System.out.println(" - scaler type is " + scalerType);
					int numTables = readUnsigned(ffBytes, ffByteOffset, (ffByteOffset+2));
					ffByteOffset += 2;
					System.out.println(" - num tables is " + numTables);
					int searchRange = readUnsigned(ffBytes, ffByteOffset, (ffByteOffset+2));
					ffByteOffset += 2;
					System.out.println(" - search range is " + searchRange);
					int entrySelector = readUnsigned(ffBytes, ffByteOffset, (ffByteOffset+2));
					ffByteOffset += 2;
					System.out.println(" - entry selector is " + entrySelector);
					int rangeShift = readUnsigned(ffBytes, ffByteOffset, (ffByteOffset+2));
					ffByteOffset += 2;
					System.out.println(" - range shift is " + rangeShift);
					
					int tableEnd = -1;
					for (int t = 0; t < numTables; t++) {
						String tag = "";
						for (int i = 0; i < 4; i++) {
							tag += ((char) readUnsigned(ffBytes, ffByteOffset, (ffByteOffset+1)));
							ffByteOffset++;
						}
						System.out.println(" - table " + tag + ":");
						int checkSum = readUnsigned(ffBytes, ffByteOffset, (ffByteOffset+4));
						ffByteOffset += 4;
						System.out.println("   - checksum is " + checkSum);
						int offset = readUnsigned(ffBytes, ffByteOffset, (ffByteOffset+4));
						ffByteOffset += 4;
						System.out.println("   - offset is " + offset);
						int length = readUnsigned(ffBytes, ffByteOffset, (ffByteOffset+4));
						ffByteOffset += 4;
						System.out.println("   - length is " + length);
						tableEnd = offset + length;
//						if ("post".equals(tag)) {
//							for (int b = offset; b < tableEnd; b++)
//								System.out.println("   " + convertUnsigned(ffBytes[b]));
//						}
						while ((tableEnd % 4) != 0)
							tableEnd++;
						
					}
					System.out.println(" - table end is " + tableEnd);
					if (tableEnd != -1)
						ffByteOffset = tableEnd;
					
					if (ffByteOffset < ffBytes.length) {
//						System.out.println(" - bytes are " + new String(ffBytes, ffByteOffset, (ffBytes.length-ffByteOffset)));
						for (int b = ffByteOffset; b < Math.min(ffBytes.length, (ffByteOffset + 1024)); b++) {
							int bt = convertUnsigned(ffBytes[b]);
							System.out.println(" - " + bt + " (" + ((char) bt) + ")");
						}
					}
					else System.out.println(" - no bytes remaining after tables");
					
					//	TODO interpret font file
				}
			}
		}
		
		Object feObj = PdfParser.dereference(fontData.get("Encoding"), objects);
		if (feObj instanceof Hashtable)
			feObj = PdfParser.dereference(((Hashtable) feObj).get("BaseEncoding"), objects);
		else if ((feObj == null) && (fdObj instanceof Hashtable))
			feObj = PdfParser.dereference(((Hashtable) fdObj).get("Encoding"), objects);
		
		PdfFont pFont = new PdfFont(fontData, fd, fc, lc, cws, mcw, fnObj.toString(), ((feObj == null) ? null : feObj.toString()));
		if (DEBUG_LOAD_FONTS) System.out.println("CIDType2 font created");
		pFont.ascent = (((Number) aObj).floatValue() / 1000);
		if (DEBUG_LOAD_FONTS) System.out.println(" - ascent is " + pFont.ascent);
		pFont.descent = (((Number) dObj).floatValue() / 1000);
		if (DEBUG_LOAD_FONTS) System.out.println(" - descent is " + pFont.descent);
		pFont.capHeigth = (((Number) chObj).floatValue() / 1000);
		if (DEBUG_LOAD_FONTS) System.out.println(" - cap height is " + pFont.capHeigth);
		if (pFont.capHeigth == 0) {
			pFont.capHeigth = pFont.ascent;
			if (DEBUG_LOAD_FONTS) System.out.println(" - cap height corrected to " + pFont.capHeigth);
		}
		
		pFont.setBaseFont(baseFont);
		
		for (Iterator ccit = widths.keySet().iterator(); ccit.hasNext();) {
			Integer cc = ((Integer) ccit.next());
			pFont.setCharWidth(cc, ((Float) widths.get(cc)).floatValue());
		}
		
		Object fObj = fd.get("Flags");
		int flags = ((fObj instanceof Number) ? ((Number) fObj).intValue() : -1);
		if (flags != -1) {
			//	TODO interpret flags (should the need arise)
		}
		
		Object fwObj = fd.get("FontWeight");
		float fw = ((fwObj instanceof Number) ? ((Number) fwObj).floatValue() : 400);
		pFont.bold = (fw > 650);
		
		Object iaObj = fd.get("ItalicAngle");
		float ia = ((iaObj instanceof Number) ? ((Number) iaObj).floatValue() : 0);
		pFont.italic = (ia < -5);
		
		
////		float llx = ((Number) bb.get(0)).floatValue();
//		float lly = ((Number) bb.get(1)).floatValue();
////		float urx = ((Number) bb.get(2)).floatValue();
//		float ury = ((Number) bb.get(3)).floatValue();
//		if (fmd < 0) {
//			pFont.ascent = ((-lly * normFactor) / 1000);
//			if (DEBUG_LOAD_FONTS) System.out.println(" - ascent is " + pFont.ascent);
//			pFont.descent = ((-ury * normFactor) / 1000);
//			if (DEBUG_LOAD_FONTS) System.out.println(" - descent is " + pFont.descent);
//			pFont.capHeigth = ((-lly * normFactor) / 1000);
//			if (DEBUG_LOAD_FONTS) System.out.println(" - cap height is " + pFont.capHeigth);
//		}
//		else {
//			pFont.ascent = ((ury * normFactor) / 1000);
//			if (DEBUG_LOAD_FONTS) System.out.println(" - ascent is " + pFont.ascent);
//			pFont.descent = ((lly * normFactor) / 1000);
//			if (DEBUG_LOAD_FONTS) System.out.println(" - descent is " + pFont.descent);
//			pFont.capHeigth = ((ury * normFactor) / 1000);
//			if (DEBUG_LOAD_FONTS) System.out.println(" - cap height is " + pFont.capHeigth);
//		}
		
		return pFont;
	}
	
	private static PdfFont readFontType3(Object fnObj, Hashtable fontData, HashMap objects) throws IOException {
		Object fdObj = PdfParser.dereference(fontData.get("FontDescriptor"), objects);
		if (!(fdObj instanceof Hashtable)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange font descriptor: " + fdObj);
			}
//			Object dfaObj = fontData.get("DescendantFonts");
//			if (dfaObj instanceof Vector) {
//				Object dfObj = PdfParser.dereference(((Vector) dfaObj).get(0), objects);
//				if (dfObj != null)
//					System.out.println(" --> descendant fonts: " + dfObj);
//			}
//			Object tuObj = PdfParser.dereference(fontData.get("ToUnicode"), objects);
//			if (tuObj instanceof PStream)
//				decodeObjectStream(((PStream) tuObj), objects, true);
//			else System.out.println(" --> to unicode: " + tuObj);
//			return null;
		}
		
		Object tuObj = PdfParser.dereference(fontData.get("ToUnicode"), objects);
		HashMap toUnicodeMappings = null;
		if (tuObj instanceof PStream) {
			Object filter = ((PStream) tuObj).params.get("Filter");
			if (filter == null)
				filter = "FlateDecode";
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PdfParser.decode(filter, ((PStream) tuObj).bytes, ((PStream) tuObj).params, baos, objects);
			byte[] tuMapData = baos.toByteArray();
			if (DEBUG_LOAD_FONTS)
				System.out.println(" --> to unicode: " + new String(tuMapData));
			toUnicodeMappings = readToUnicodeCMap(tuMapData, true);
			//	TODO if we have this mapping, we can use it right away, no need to bother with rendering and recognizing the characters
			//	- char widths are in descendant font dictionary, entry W
			//	- missing width is in descendant font dictionary, entry DW
			//PdfFont pFont = new PdfFont(data, descriptor, firstChar, lastChar, charWidths, mCharWidth, name, encoding);
		}
		else System.out.println(" --> to unicode: " + tuObj);
		
		Object rObj = PdfParser.dereference(fontData.get("Resources"), objects);
		if (rObj instanceof Hashtable) {
			if (DEBUG_LOAD_FONTS) System.out.println("Got font resources: " + rObj);
		}
		else if (DEBUG_LOAD_FONTS) {
			System.out.println("Problem in font " + fontData);
			System.out.println(" --> strange resources: " + rObj);
		}
		
		Object eObj = PdfParser.dereference(fontData.get("Encoding"), objects);
		HashMap diffEncodings = new HashMap();
		HashMap diffEncodingNames = new HashMap();
		HashMap resolvedCodes = new HashMap();
		HashMap unresolvedDiffEncodings = new HashMap();
		HashMap unresolvedCodes = new HashMap();
		if (eObj instanceof Hashtable) {
			if (DEBUG_LOAD_FONTS) System.out.println("Got encoding: " + eObj);
			Object dObj = PdfParser.dereference(((Hashtable) eObj).get("Differences"), objects);
			if (DEBUG_LOAD_FONTS) System.out.println("Got differences: " + dObj);
			if (dObj instanceof Vector) {
				Vector diffs = ((Vector) dObj);
				int nextCode = -1;
				for (int d = 0; d < diffs.size(); d++) {
					Object diff = diffs.get(d);
					if (diff instanceof Number)
						nextCode = ((Number) diff).intValue();
					else if (nextCode != -1) {
						Integer code = new Integer(nextCode++);
						String diffCharName = diff.toString();
						diffEncodingNames.put(code, diffCharName);
						char diffChar = StringUtils.getCharForName(diffCharName);
						if (diffChar != 0) {
							diffEncodings.put(code, new Character(diffChar));
							resolvedCodes.put(diffCharName, code);
						}
						else if ((toUnicodeMappings == null) || !toUnicodeMappings.containsKey(code)) {
							unresolvedDiffEncodings.put(code, diffCharName);
							unresolvedCodes.put(diffCharName, code);
						}
					}
				}
			}
			else {
				if (DEBUG_LOAD_FONTS) {
					System.out.println("Problem in font " + fontData);
					System.out.println(" --> strange differences definition: " + dObj);
				}
				return null;
			}
			if (DEBUG_LOAD_FONTS) {
				System.out.print("Got mappings: {");
				for (Iterator dit = diffEncodings.keySet().iterator(); dit.hasNext();) {
					Integer d = ((Integer) dit.next());
					Character ch = ((Character) diffEncodings.get(d));
					System.out.print(d + "=" + ch + "(" + StringUtils.getNormalForm(ch.charValue()) + ")");
					if (dit.hasNext())
						System.out.print(", ");
				}
				System.out.println("}");
			}
		}
		else {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange encoding: " + eObj);
			}
			return null;
		}
		
		Object cpsObj = PdfParser.dereference(fontData.get("CharProcs"), objects);
		HashMap cpOutputs = new HashMap();
		if (cpsObj instanceof Hashtable) {
			if (DEBUG_LOAD_FONTS) System.out.println("Got char procs: " + cpsObj);
			Hashtable cps = ((Hashtable) cpsObj);
			HashMap cache = ((diffEncodings.size() < 10) ? null : new HashMap());
			for (Iterator eit = diffEncodings.keySet().iterator(); eit.hasNext();) {
				Integer charCode = ((Integer) eit.next());
				Object charName = diffEncodingNames.get(charCode);
				Object cpObj = PdfParser.dereference(cps.get(charName), objects);
				if (cpObj instanceof PStream) {
					if (DEBUG_LOAD_FONTS) System.out.println("  Got char proc for '" + charName + "': " + ((PStream) cpObj).params);
					char cpChar = getChar(((PStream) cpObj), charName.toString(), objects, cache);
					if (cpChar != 0)
						cpOutputs.put(charCode, ("" + cpChar));
//					
//					//	TODO obtain and store font size relative baseline shift
//					pFont.setRelativeBaselineShift(bsCh, (((float) bestSim.match.shiftBelowBaseline) / bestSim.match.fontSize));
				}
				else if (DEBUG_LOAD_FONTS) System.out.println(" --> strange char proc for '" + charName + "': " + cpObj);
				
				//	TODO also observe FontFile2 field
			}
		}
		else {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange char procs: " + cpsObj);
			}
			return null;
		}
		
		float normFactor = 1; // factor for normalizing to 1/1000 scale used in Type1 fonts
		Object fmObj = PdfParser.dereference(fontData.get("FontMatrix"), objects);
		float fma = 0.001f;
		float fmd = 0.001f;
		if (fmObj instanceof Vector) {
			if (DEBUG_LOAD_FONTS) System.out.println("Got font matrix: " + fmObj);
			Vector fm = ((Vector) fmObj);
			if (fm.size() >= 4) {
//				float a = 1;
				Object aObj = fm.get(0);
				if (aObj instanceof Number)
					fma = ((Number) aObj).floatValue();
//				float d = 1;
				Object dObj = fm.get(3);
				if (dObj instanceof Number)
					fmd = ((Number) dObj).floatValue();
				normFactor = (((Math.abs(fma) + Math.abs(fmd)) / 2) / 0.001f);
				if (DEBUG_LOAD_FONTS) System.out.println("Norm factor is: " + normFactor);
			}
		}
		else {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange font matrix: " + fmObj);
			}
			return null;
		}
		
		Object bbObj = fontData.get("FontBBox");
		if (!(bbObj instanceof Vector)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange bounding box: " + bbObj);
			}
			return null;
		}
		Vector bb = ((Vector) bbObj);
		if (bb.size() != 4) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange bounding box: " + bb);
			}
			return null;
		}
		
		Object fcObj = fontData.get("FirstChar");
		if (!(fcObj instanceof Number)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange first char: " + fcObj);
			}
			return null;
		}
		Object lcObj = fontData.get("LastChar");
		if (!(lcObj instanceof Number)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange last char: " + lcObj);
			}
			return null;
		}
		Object cwObj = PdfParser.dereference(fontData.get("Widths"), objects);
		if (!(cwObj instanceof Vector)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> strange char widths: " + cwObj);
			}
			return null;
		}
		
		Object feObj = PdfParser.dereference(fontData.get("Encoding"), objects);
		if (feObj instanceof Hashtable)
			feObj = PdfParser.dereference(((Hashtable) feObj).get("BaseEncoding"), objects);
		else if ((feObj == null) && (fdObj instanceof Hashtable))
			feObj = PdfParser.dereference(((Hashtable) fdObj).get("Encoding"), objects);
		
		Hashtable fd = ((Hashtable) fdObj);
		int fc = ((Number) fcObj).intValue();
		int lc = ((Number) lcObj).intValue();
		Vector cwv = ((Vector) cwObj);
		if (cwv.size() != (lc - fc + 1)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> invalid char widths");
			}
			return null;
		}
		float[] cws = new float[lc - fc + 1];
		for (int c = 0; c < cws.length; c++) {
			Object cwo = cwv.get(c);
			cws[c] = ((cwo instanceof Number) ? (((Number) cwo).floatValue() * normFactor) : 0);
		}
		Object mcwObj = ((fd == null) ? null : fd.get("MissingWidth"));
		float mcw = ((mcwObj instanceof Number) ? ((Number) mcwObj).floatValue() : 0);
		
		PdfFont pFont = new PdfFont(fontData, fd, fc, lc, cws, mcw, fnObj.toString(), ((feObj == null) ? null : feObj.toString()));
		if (DEBUG_LOAD_FONTS) System.out.println("Type3 font created");
		boolean hasDescent = false;
		for (Iterator cmit = cpOutputs.keySet().iterator(); cmit.hasNext();) {
			Integer chc = ((Integer) cmit.next());
			String str = ((String) cpOutputs.get(chc));
			pFont.mapUnicode(chc, str);
			if (((chc.intValue() - fc) >= 0) && ((chc.intValue() - fc) < cws.length))
				pFont.setCharWidth(chc, cws[chc.intValue() - fc]);
			for (int c = 0; c < str.length(); c++)
				hasDescent = (hasDescent || ("Qgjpqy".indexOf(StringUtils.getBaseChar(str.charAt(c))) != -1));
		}
//		float llx = ((Number) bb.get(0)).floatValue();
		float lly = ((Number) bb.get(1)).floatValue();
//		float urx = ((Number) bb.get(2)).floatValue();
		float ury = ((Number) bb.get(3)).floatValue();
		if (fmd < 0) {
			pFont.ascent = ((-lly * normFactor) / 1000);
			if (DEBUG_LOAD_FONTS) System.out.println(" - ascent is " + pFont.ascent);
			pFont.descent = ((-ury * normFactor) / 1000);
			if (DEBUG_LOAD_FONTS) System.out.println(" - descent is " + pFont.descent);
			pFont.capHeigth = ((-lly * normFactor) / 1000);
			if (DEBUG_LOAD_FONTS) System.out.println(" - cap height is " + pFont.capHeigth);
		}
		else {
			pFont.ascent = ((ury * normFactor) / 1000);
			if (DEBUG_LOAD_FONTS) System.out.println(" - ascent is " + pFont.ascent);
			pFont.descent = ((lly * normFactor) / 1000);
			if (DEBUG_LOAD_FONTS) System.out.println(" - descent is " + pFont.descent);
			pFont.capHeigth = ((ury * normFactor) / 1000);
			if (DEBUG_LOAD_FONTS) System.out.println(" - cap height is " + pFont.capHeigth);
		}
		pFont.type = 3;
		pFont.hasDescent = hasDescent;
		
		for (Iterator cmit = diffEncodings.keySet().iterator(); cmit.hasNext();) {
			Integer chc = ((Integer) cmit.next());
			pFont.mapDifference(chc, ((Character) diffEncodings.get(chc)));
		}
		
		if (toUnicodeMappings != null)
			for (Iterator cmit = toUnicodeMappings.keySet().iterator(); cmit.hasNext();) {
				Integer chc = ((Integer) cmit.next());
				pFont.mapUnicode(chc, ((String) toUnicodeMappings.get(chc)));
			}
		
		return pFont;
	}
	
	private static char getChar(PStream charProg, String charName, HashMap objects, HashMap cache) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfParser.decode(charProg.params.get("Filter"), charProg.bytes, charProg.params, baos, objects);
		byte[] cpBytes = baos.toByteArray();
		if (DEBUG_LOAD_FONTS) {
			System.out.write(cpBytes);
			System.out.println();
		}
		int imgWidth = -1;
		int imgHeight = -1;
		int bpc = -1;
		boolean isMaskImage = false;
		String imgFilter = null;
		ByteArrayOutputStream imgBuffer = null;
		byte[] imgData = null;
		LineInputStream lis = new LineInputStream(new ByteArrayInputStream(cpBytes));
		byte[] line;
		while ((line = lis.readLine()) != null) {
			if (PdfUtils.startsWith(line, "BI", 0))
				break;
			if (DEBUG_LOAD_FONTS) System.out.println("IGNORING: " + new String(line));
		}
		Hashtable imgParams = new Hashtable();
		while ((line = lis.readLine()) != null) {
			if (PdfUtils.startsWith(line, "EI", 0)) {
				if (imgBuffer != null)
					imgData = imgBuffer.toByteArray();
				break;
			}
			if (PdfUtils.startsWith(line, "ID", 0)) {
				imgBuffer = new ByteArrayOutputStream();
				if (line.length > 3)
					imgBuffer.write(line, 3, (line.length-3));
			}
			else if (imgBuffer != null)
				imgBuffer.write(line);
			else if (PdfUtils.startsWith(line, "/", 0)) {
				String keyValuePair = new String(line, 1, (line.length-1));
				String[] keyValue = keyValuePair.split("\\s+");
				if (keyValue.length != 2) {
					if (DEBUG_LOAD_FONTS) System.out.println("BROKEN PARAMETER LINE: " + new String(line));
					continue;
				}
				imgParams.put(keyValue[0], keyValue[1]);
				if ("W".equals(keyValue[0]) || "Width".equals(keyValue[0]))
					imgWidth = Integer.parseInt(keyValue[1]);
				else if ("H".equals(keyValue[0]) || "Height".equals(keyValue[0]))
					imgHeight = Integer.parseInt(keyValue[1]);
				else if ("BPC".equals(keyValue[0]) || "BitsPerComponent".equals(keyValue[0]))
					bpc = Integer.parseInt(keyValue[1]);
				else if ("IM".equals(keyValue[0]) || "ImageMask".equals(keyValue[0]))
					isMaskImage = "true".equals(keyValue[1]);
				else if ("F".equals(keyValue[0]) || "Filter".equals(keyValue[0])) {
					imgFilter = keyValue[1];
					if (imgFilter.startsWith("/"))
						imgFilter = imgFilter.substring(1);
				}
			}
		}
		
		//	TODO implement other types of true type fonts
		
		if ((imgWidth == -1) || (imgHeight == -1) || (imgData == null)) {
			if (DEBUG_LOAD_FONTS) System.out.println("Invalid char prog");
			return 0;
		}
		if (imgFilter != null) {
			imgBuffer = new ByteArrayOutputStream();
			PdfParser.decode(imgFilter, imgData, imgParams, imgBuffer, objects);
			imgData = imgBuffer.toByteArray();
		}
		if (DEBUG_LOAD_FONTS) System.out.println("GOT DATA FOR CHAR IMAGE (" + imgWidth + " x " + imgHeight + "): " + imgData.length);
		BufferedImage cpImg = null;
		if (isMaskImage)
			cpImg = createImageMask(imgWidth, imgHeight, imgData);
		else {
			//	TODO implement color spaces, and read them as bitmaps nontheless
			//	TODO to achive this, create a BufferedImage in respective color space and read back brightness
		}
		return ((cpImg == null) ? 0 : getChar(cpImg, 0, charName, cpImg.getHeight(), cache));
	}
	
	private static PdfFont readFontType1(Hashtable fontData, HashMap objects) throws IOException {
		Object fnObj = PdfParser.dereference(fontData.get("BaseFont"), objects);
		if (fnObj == null) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> missing font name");
			}
			return null;
		}
		
		boolean isSubSet = fnObj.toString().matches("[A-Z]{6}\\+.+");
		boolean isSymbolic = false;
		Object fdObj = PdfParser.dereference(fontData.get("FontDescriptor"), objects);
		if (fdObj instanceof Hashtable) {
			Object fObj = ((Hashtable) fdObj).get("Flags");
			if (fObj != null)
				isSymbolic = (fObj.toString().indexOf("3") != -1);
		}
		
		BaseFont baseFont = getBuiltInFont((isSubSet ? fnObj.toString().substring(7) : fnObj), isSymbolic);
		
		HashMap toUnicodeMappings = null;
		if (fdObj instanceof Hashtable) {
			if (DEBUG_LOAD_FONTS) System.out.println("Got font descriptor: " + fdObj);
			if (baseFont != null)
				for (Iterator kit = baseFont.descriptor.keySet().iterator(); kit.hasNext();) {
					Object key = kit.next();
					if (!((Hashtable) fdObj).containsKey(key)) {
						((Hashtable) fdObj).put(key, baseFont.descriptor.get(key));
					}
				}
			Object tuObj = PdfParser.dereference(fontData.get("ToUnicode"), objects);
			if (tuObj instanceof PStream) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				Object filter = ((PStream) tuObj).params.get("Filter");
				if (filter == null)
					filter = "FlateDecode";
				PdfParser.decode(filter, ((PStream) tuObj).bytes, ((PStream) tuObj).params, baos, objects);
				byte[] tuMapData = baos.toByteArray();
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> to unicode: " + new String(tuMapData));
				toUnicodeMappings = readToUnicodeCMap(tuMapData, true);
			}
			else if (DEBUG_LOAD_FONTS) System.out.println(" --> to unicode: " + tuObj);
		}
		else {
			if (baseFont == null) {
				if (DEBUG_LOAD_FONTS) {
					System.out.println("Problem in font " + fontData);
					System.out.println(" --> strange font descriptor: " + fdObj);
				}
				Object dfaObj = fontData.get("DescendantFonts");
				if (dfaObj instanceof Vector) {
					Object dfObj = PdfParser.dereference(((Vector) dfaObj).get(0), objects);
					if (dfObj != null) {
						if (DEBUG_LOAD_FONTS) System.out.println(" --> descendant fonts: " + dfObj);
					}
				}
				Object tuObj = PdfParser.dereference(fontData.get("ToUnicode"), objects);
				if (tuObj instanceof PStream) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					Object filter = ((PStream) tuObj).params.get("Filter");
					if (filter == null)
						filter = "FlateDecode";
					PdfParser.decode(filter, ((PStream) tuObj).bytes, ((PStream) tuObj).params, baos, objects);
					byte[] tuMapData = baos.toByteArray();
					if (DEBUG_LOAD_FONTS)
						System.out.println(" --> to unicode: " + new String(tuMapData));
					toUnicodeMappings = readToUnicodeCMap(tuMapData, true);
				}
				else if (DEBUG_LOAD_FONTS) System.out.println(" --> to unicode: " + tuObj);
				return null;
			}
			else fdObj = baseFont.descriptor;
			if (DEBUG_LOAD_FONTS) System.out.println("Got font descriptor: " + fdObj);
		}
		
		Object fcObj = fontData.get("FirstChar");
		if (!(fcObj instanceof Number)) {
			if (baseFont == null) {
				if (DEBUG_LOAD_FONTS) {
					System.out.println("Problem in font " + fontData);
					System.out.println(" --> strange first char: " + fcObj);
				}
				return null;
			}
			else fcObj = new Integer(baseFont.firstChar);
		}
		Object lcObj = fontData.get("LastChar");
		if (!(lcObj instanceof Number)) {
			if (baseFont == null) {
				if (DEBUG_LOAD_FONTS) {
					System.out.println("Problem in font " + fontData);
					System.out.println(" --> strange last char: " + lcObj);
				}
				return null;
			}
			else lcObj = new Integer(baseFont.lastChar);
		}
		Object cwObj = PdfParser.dereference(fontData.get("Widths"), objects);
		if (!(cwObj instanceof Vector)) {
			if (baseFont == null) {
				if (DEBUG_LOAD_FONTS) {
					System.out.println("Problem in font " + fontData);
					System.out.println(" --> strange char widths: " + cwObj);
				}
				return null;
			}
			else {
				cwObj = new Vector();
				for (int c = 0; c < baseFont.charWidths.length; c++)
					((Vector) cwObj).add(new Float(baseFont.charWidths[c]));
			}
		}
		
		Object feObj = PdfParser.dereference(fontData.get("Encoding"), objects);
		HashMap diffEncodings = new HashMap();
		HashMap resolvedCodes = new HashMap();
		HashMap unresolvedDiffEncodings = new HashMap();
		HashMap unresolvedCodes = new HashMap();
		if (feObj instanceof Hashtable) {
			if (DEBUG_LOAD_FONTS) System.out.println("Got encoding: " + feObj);
			Object dObj = PdfParser.dereference(((Hashtable) feObj).get("Differences"), objects);
			if (dObj != null) {
				if (DEBUG_LOAD_FONTS) System.out.println("Got differences: " + dObj);
				if (dObj instanceof Vector) {
					Vector diffs = ((Vector) dObj);
					int nextCode = -1;
					for (int d = 0; d < diffs.size(); d++) {
						Object diff = diffs.get(d);
						if (diff instanceof Number)
							nextCode = ((Number) diff).intValue();
						else if (nextCode != -1) {
							Integer code = new Integer(nextCode++);
							String diffCharName = diff.toString();
							char diffChar = StringUtils.getCharForName(diffCharName);
							if (diffChar != 0) {
								diffEncodings.put(code, new Character(diffChar));
								resolvedCodes.put(diffCharName, code);
							}
							else if ((toUnicodeMappings == null) || !toUnicodeMappings.containsKey(code)) {
								unresolvedDiffEncodings.put(code, diffCharName);
								unresolvedCodes.put(diffCharName, code);
							}
						}
					}
				}
				else {
					if (DEBUG_LOAD_FONTS) {
						System.out.println("Problem in font " + fontData);
						System.out.println(" --> strange differences definition: " + dObj);
					}
					return null;
				}
				if (DEBUG_LOAD_FONTS) {
					System.out.print("Got mappings: {");
					for (Iterator dit = diffEncodings.keySet().iterator(); dit.hasNext();) {
						Integer d = ((Integer) dit.next());
						Character c = ((Character) diffEncodings.get(d));
						System.out.print(d + "=" + c.charValue() + "(" + StringUtils.getNormalForm(c.charValue()) + ")");
						if (dit.hasNext())
							System.out.print(", ");
					}
					System.out.println("}");
					if (unresolvedDiffEncodings.size() != 0) {
						System.out.print("Still to map: {");
						for (Iterator dit = unresolvedDiffEncodings.keySet().iterator(); dit.hasNext();) {
							Integer d = ((Integer) dit.next());
							String cn = ((String) unresolvedDiffEncodings.get(d));
							System.out.print(d + "=" + cn);
							if (dit.hasNext())
								System.out.print(", ");
						}
						System.out.println("}");
					}
				}
			}
			
			//	get encoding name
			feObj = PdfParser.dereference(((Hashtable) feObj).get("BaseEncoding"), objects);
		}
		if ((feObj == null) && (fdObj instanceof Hashtable))
			feObj = PdfParser.dereference(((Hashtable) fdObj).get("Encoding"), objects);
		
		Hashtable fd = ((Hashtable) fdObj);
		int fc = ((Number) fcObj).intValue();
		int lc = ((Number) lcObj).intValue();
		Vector cwv = ((Vector) cwObj);
		if (cwv.size() != (lc - fc + 1)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> invalid char widths");
			}
			return null;
		}
		float[] cws = new float[lc - fc + 1];
		for (int c = 0; c < cws.length; c++) {
			Object cwo = cwv.get(c);
			cws[c] = ((cwo instanceof Number) ? ((Number) cwo).floatValue() : 0);
		}
		Object mcwObj = fd.get("MissingWidth");
		float mcw = ((mcwObj instanceof Number) ? ((Number) mcwObj).floatValue() : 0);
		if ((mcw == 0) && (DEBUG_LOAD_FONTS)) {
			System.out.println("Problem in font " + fontData);
			System.out.println(" --> invalid missing char width");
		}
		float scw = 0;
		
		if ((fc > 32) && (mcw == 0)) {
			Object csObj = fd.get("CharSet");
			if (DEBUG_LOAD_FONTS)
				System.out.println(" --> char set is " + csObj);
			if ((csObj instanceof PString) && (((PString) csObj).toString().indexOf("/space") == -1)) {
				float wSum = 0;
				int wCount = 0;
				for (int c = 0; c < cws.length; c++)
					if (cws[c] != 0) {
						wSum += cws[c];
						wCount++;
					}
				if (wCount != 0)
					scw = ((wSum * 2) / (wCount * 3));
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> space width interpolated as " + scw);
			}
		}
		
		Object fbbObj = fd.get("FontBBox");
		if (!(fbbObj instanceof Vector) || (((Vector) fbbObj).size() != 4)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange bounding box: " + fbbObj);
			}
			fbbObj = null;
		}
		
		Object aObj = fd.get("Ascent");
		if (!(aObj instanceof Number) || ((((Number) aObj).floatValue() == 0) && fd.containsKey("Ascender")))
			aObj = fd.get("Ascender");
		if (!(aObj instanceof Number) || (((Number) aObj).floatValue() == 0)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange ascent: " + aObj);
			}
			if ((fbbObj != null) && (((Vector) fbbObj).get(2) instanceof Number)) {
				aObj = ((Vector) fbbObj).get(2);
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> fallback from bounding box: " + aObj);
			}
			else {
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> could not determine ascent");
				return null;
			}
		}
		
		Object dObj = fd.get("Descent");
		if (!(dObj instanceof Number) || ((((Number) dObj).floatValue() == 0)) && fd.containsKey("Descender"))
			dObj = fd.get("Descender");
		if (!(dObj instanceof Number)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange descent: " + dObj);
			}
			if ((fbbObj != null) && (((Vector) fbbObj).get(0) instanceof Number)) {
				dObj = ((Vector) fbbObj).get(2);
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> fallback from bounding box: " + dObj);
			}
			else {
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> could not determine descent");
				return null;
			}
		}
		Object chObj = fd.get("CapHeight");
		if (!(chObj instanceof Number) || (((Number) chObj).floatValue() == 0)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange cap height: " + chObj);
				System.out.println(" --> fallback to ascent: " + aObj);
			}
			chObj = aObj;
		}
		
		PdfFont pFont = new PdfFont(fontData, fd, fc, lc, cws, mcw, fnObj.toString(), ((feObj == null) ? null : feObj.toString()));
		if (DEBUG_LOAD_FONTS) System.out.println("Type1 font created");
		pFont.ascent = (((Number) aObj).floatValue() / 1000);
		if (DEBUG_LOAD_FONTS) System.out.println(" - ascent is " + pFont.ascent);
		pFont.descent = (((Number) dObj).floatValue() / 1000);
		if (DEBUG_LOAD_FONTS) System.out.println(" - descent is " + pFont.descent);
		pFont.capHeigth = (((Number) chObj).floatValue() / 1000);
		if (DEBUG_LOAD_FONTS) System.out.println(" - cap height is " + pFont.capHeigth);
//		if (pFont.capHeigth == 0) {
//			pFont.capHeigth = pFont.ascent;
//			if (DEBUG_LOAD_FONTS) System.out.println(" - cap height corrected to " + pFont.capHeigth);
//		}
		
		for (Iterator cmit = diffEncodings.keySet().iterator(); cmit.hasNext();) {
			Integer chc = ((Integer) cmit.next());
			pFont.mapDifference(chc, ((Character) diffEncodings.get(chc)));
		}
		pFont.setBaseFont(baseFont);
		
		if (toUnicodeMappings != null)
			for (Iterator cmit = toUnicodeMappings.keySet().iterator(); cmit.hasNext();) {
				Integer chc = ((Integer) cmit.next());
				pFont.mapUnicode(chc, ((String) toUnicodeMappings.get(chc)));
			}
		
		if (scw != 0) {
			pFont.setCharWidth(new Character(' '), scw);
			pFont.setCharWidth(new Integer((int) ' '), scw);
		}
		
//		TODO implement this case as well (soon as a PDF for testing becomes available)
		Object ffObj = PdfParser.dereference(fd.get("FontFile"), objects);
		if (ffObj instanceof PStream) {
			System.out.println("Got font file");
			System.out.println("  --> params are " + ((PStream) ffObj).params);
			Object filter = ((PStream) ffObj).params.get("Filter");
			System.out.println("  --> filter is " + filter);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PdfParser.decode(filter, ((PStream) ffObj).bytes, ((PStream) ffObj).params, baos, objects);
//			System.out.println(new String(baos.toByteArray()));
			readFontType1(baos.toByteArray(), ((PStream) ffObj).params, fd, pFont);
		}
		
		//	add special mappings from font file
		Object ff3Obj = PdfParser.dereference(fd.get("FontFile3"), objects);
		if (ff3Obj instanceof PStream) {
			System.out.println("Got font file 3");
			System.out.println("  --> params are " + ((PStream) ff3Obj).params);
			Object filter = ((PStream) ff3Obj).params.get("Filter");
			System.out.println("  --> filter is " + filter);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PdfParser.decode(filter, ((PStream) ff3Obj).bytes, ((PStream) ff3Obj).params, baos, objects);
			Object stObj = ((PStream) ff3Obj).params.get("Subtype");
			if ((stObj != null) && "Type1C".equals(stObj.toString()))
				readFontType1C(baos.toByteArray(), fd, pFont, resolvedCodes, unresolvedCodes);
		}
		
		return pFont;
	}
	
	private static void readFontType1(byte[] data, Hashtable dataParams, Hashtable fd, PdfFont pFont) {
		//	TODO read meta data up to 'eexec' plus subsequent single space char (Length1 parameter in stream dictionary)
		
		//	TODO get encrypted portion of data (Length2 parameter in stream dictionary)
		
		//	TODO decrypt data
		
		//	TODO deletgate to code already handling Type1C
	}
	
	private static final boolean DEBUG_TYPE1C_LOADING = true;
	
	private static void readFontType1C(byte[] data, Hashtable fd, PdfFont pFont, HashMap resolvedCodes, HashMap unresolvedCodes) {
		int i = 0;
		
		//	read header
//		if (DEBUG_TYPE1C_LOADING) System.out.println(new String(data));
		int major = convertUnsigned(data[i++]);
		int minor = convertUnsigned(data[i++]);
		if (DEBUG_TYPE1C_LOADING) System.out.println("Version is " + major + "." + minor);
		int hdrSize = convertUnsigned(data[i++]);
		if (DEBUG_TYPE1C_LOADING) System.out.println("Header size is " + hdrSize);
		int offSize = convertUnsigned(data[i++]);
		if (DEBUG_TYPE1C_LOADING) System.out.println("Offset size is " + offSize);
		
		//	read base data
		i = readFontType1cIndex(data, i, "name", null, null, false, null);
		if (DEBUG_TYPE1C_LOADING) System.out.println("GOT TO " + i + " of " + data.length + " bytes");
		HashMap topDict = new HashMap() {
			public Object put(Object key, Object value) {
				if (DEBUG_TYPE1C_LOADING) {
					System.out.print("TopDict: " + key + " set to " + value);
					if (value instanceof Number[]) {
						Number[] nums = ((Number[]) value);
						for (int n = 0; n < nums.length; n++)
							System.out.print(" " + nums[n]);
					}
					System.out.println();
				}
				return super.put(key, value);
			}
		};
		i = readFontType1cIndex(data, i, "TopDICT", topDict, type1cTopDictOpResolver, true, null);
		if (DEBUG_TYPE1C_LOADING) System.out.println("GOT TO " + i + " of " + data.length + " bytes");
//		THIS IS NOT A SEPARATE DICTIONARY
//		i = readFontType1cIndex(data, i, "TopDICTData", null, null, false, null);
//		if (DEBUG_TYPE1C_LOADING) System.out.println("GOT TO " + i + " of " + data.length + " bytes");
		ArrayList sidIndex = new ArrayList() {
			public boolean add(Object o) {
				System.out.println("StringIndex: " + this.size() + " (SID " + (this.size() + sidResolver.size()) + ") set to " + o);
				return super.add(o);
			}
		};
		i = readFontType1cIndex(data, i, "String", null, null, false, sidIndex);
		if (DEBUG_TYPE1C_LOADING) System.out.println("GOT TO " + i + " of " + data.length + " bytes");
		
		//	TODO from here onward, Type 1 and Type 1C should be the same
		
		//	read encoding
		HashMap eDict = new HashMap();
		int eEnd = 0;
		if (topDict.containsKey("Encoding")) {
			Number[] eos = ((Number[]) topDict.get("Encoding"));
			if (eos.length != 0) {
				int eso = eos[0].intValue();
				eEnd = readFontType1cEncoding(data, eso, eDict);
			}
		}
		
		//	read char rendering data
		HashMap csDict = new HashMap();
		ArrayList csIndexContent = new ArrayList();
		int csEnd = 0;
		if (topDict.containsKey("CharStrings")) {
			Number[] csos = ((Number[]) topDict.get("CharStrings"));
			if (csos.length != 0) {
				int cso = csos[0].intValue();
				csEnd = readFontType1cIndex(data, cso, "CharStrings", csDict, glyphProgOpResolver, false, csIndexContent);
			}
		}
		ArrayList csContent = new ArrayList();
		int cEnd = 0;
		if (topDict.containsKey("Charset") && csDict.containsKey("Count")) {
			Number[] csos = ((Number[]) topDict.get("Charset"));
			Number[] cnts = ((Number[]) csDict.get("Count"));
			if ((csos.length * cnts.length) != 0) {
				int cso = csos[0].intValue();
				int cnt = cnts[0].intValue();
				cEnd = readFontType1cCharset(data, cso, cnt, csContent);
			}
		}
		HashMap pDict = new HashMap();
		int pEnd = 0;
		if (topDict.containsKey("Private")) {
			Number[] pos = ((Number[]) topDict.get("Private"));
			if (pos.length != 0) try {
				int po = pos[0].intValue();
				ArrayList pDictContent = new ArrayList();
				pEnd = readFontType1cDict(data, po, "Private", pDict, false, privateOpResolver, pDictContent);
			}
			catch (RuntimeException re) {
				System.out.println("Error reading private dictionary: " + re.getMessage());
			}
		}
		i = Math.max(Math.max(i, pEnd), Math.max(csEnd, cEnd));
		if (DEBUG_TYPE1C_LOADING) {
			System.out.println("GOT TO " + i + " of " + data.length + " bytes");
			System.out.println("Got " + csContent.size() + " char IDs, " + csIndexContent.size() + " char progs");
		}
		if (csContent.isEmpty() || (((Integer) csContent.get(0)).intValue() != 0))
			csContent.add(0, new Integer(0));
		
		//	get default width
		int dWidth = -1;
		if (pDict.containsKey("defaultWidthX")) {
			Number[] dws = ((Number[]) pDict.get("defaultWidthX"));
			if (dws.length != 0)
				dWidth = dws[0].intValue();
		}
		int nWidth = -1;
		if (pDict.containsKey("nominalWidthX")) {
			Number[] nws = ((Number[]) pDict.get("nominalWidthX"));
			if (nws.length != 0)
				nWidth = nws[0].intValue();
		}
		if (nWidth == -1)
			nWidth = dWidth;
		else if (dWidth == -1)
			dWidth = nWidth;
		if (DEBUG_TYPE1C_LOADING) System.out.println("Default char width is " + dWidth + ", nominal width is " + nWidth);
		
		//	measure characters
		int maxDescent = 0;
		int maxCapHeight = 0;
		OpTracker[] otrs = new OpTracker[Math.min(csIndexContent.size(), csContent.size())];
		for (int c = 0; c < Math.min(csIndexContent.size(), csContent.size()); c++) {
			Op[] cs = ((Op[]) csIndexContent.get(c));
			otrs[c] = new OpTracker();
			Integer sid = ((Integer) csContent.get(c));
			if ((0 < DEBUG_TYPE1C_TARGET_SID) && (sid.intValue() != DEBUG_TYPE1C_TARGET_SID))
				continue;
			String chn = ((String) sidResolver.get(sid));
			if ((chn == null) && (sid.intValue() >= sidResolver.size()) && ((sid.intValue() - sidResolver.size()) < sidIndex.size()))
				chn = ((String) sidIndex.get(sid.intValue() - sidResolver.size()));
			char ch = StringUtils.getCharForName(chn);
			if (DEBUG_TYPE1C_LOADING) System.out.println("Measuring char " + (c+1) + ", SID is " + sid + " (" + chn + "/'" + ch + "'/'" + StringUtils.getNormalForm(ch) + "'/" + ((int) ch) + ")");
			runFontType1Ops(cs, otrs[c], false, false, null, -1); // we don't know if multi-path or not so far ... 
//			if (DEBUG_TYPE1C_LOADING) System.out.println(" - " + otrs[c].id + ": " + otrs[c].minX + " < X < " + otrs[c].maxX);
			maxDescent = Math.min(maxDescent, otrs[c].minY);
			maxCapHeight = Math.max(maxCapHeight, otrs[c].maxY);
		}
		if (DEBUG_TYPE1C_LOADING) System.out.println("Max descent is " + maxDescent + ", max cap height is " + maxCapHeight);
		
		//	set up rendering
		int maxRenderSize = 300;
		float scale = 1.0f;
		if ((maxCapHeight - maxDescent) > maxRenderSize)
			scale = (((float) maxRenderSize) / (maxCapHeight - maxDescent));
		
 		//	set up style-aware name based checks
		Font serifFont = getSerifFont();
		Font[] serifFonts = new Font[4];
		Font sansFont = getSansSerifFont();
		Font[] sansFonts = new Font[4];
		for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
			serifFonts[s] = serifFont.deriveFont(s);
			sansFonts[s] = sansFont.deriveFont(s);
		}
		
		ImageSimilarity[][] serifStyleSims = new ImageSimilarity[Math.min(csContent.size(), csIndexContent.size())][4];
		int[] serifCharCounts = {0, 0, 0, 0};
		int[] serifCharCountsNs = {0, 0, 0, 0};
		double[] serifStyleSimMins = {1, 1, 1, 1};
		double[] serifStyleSimMinsNs = {1, 1, 1, 1};
		double[] serifStyleSimSums = {0, 0, 0, 0};
		double[] serifStyleSimSumsNs = {0, 0, 0, 0};
		
		ImageSimilarity[][] sansStyleSims = new ImageSimilarity[Math.min(csContent.size(), csIndexContent.size())][4];
		int[] sansCharCounts = {0, 0, 0, 0};
		int[] sansCharCountsNs = {0, 0, 0, 0};
		double[] sansStyleSimMins = {1, 1, 1, 1};
		double[] sansStyleSimMinsNs = {1, 1, 1, 1};
		double[] sansStyleSimSums = {0, 0, 0, 0};
		double[] sansStyleSimSumsNs = {0, 0, 0, 0};
		float simFontSize = -1;
		
		double simMin = 1;
		double simMinNs = 1;
		double simSum = 0;
		double simSumNs = 0;
		int charCount = 0;
		int charCountNs = 0;
		
		BufferedImage[] imgs = new BufferedImage[Math.min(csContent.size(), csIndexContent.size())];
		char[] chars = new char[Math.min(csContent.size(), csIndexContent.size())];
		Arrays.fill(chars, ((char) 0));
		
		//	generate images and match against named char
		ImageDisplayDialog fidd = (DEBUG_TYPE1C_RENDRING ? new ImageDisplayDialog("Whole Font") : null);
		for (int c = 0; c < Math.min(csContent.size(), csIndexContent.size()); c++) {
			Integer sid = ((Integer) csContent.get(c));
			if (sid.intValue() == 0)
				continue;
			if ((0 < DEBUG_TYPE1C_TARGET_SID) && (sid.intValue() != DEBUG_TYPE1C_TARGET_SID))
				continue;
			String chn = ((String) sidResolver.get(sid));
			if ((chn == null) && (sid.intValue() >= sidResolver.size()) && ((sid.intValue() - sidResolver.size()) < sidIndex.size()))
				chn = ((String) sidIndex.get(sid.intValue() - sidResolver.size()));
			chars[c] = StringUtils.getCharForName(chn);
			if (DEBUG_TYPE1C_LOADING) System.out.println("Decoding char " + c + ", SID is " + sid + " (" + chn + "/'" + chars[c] + "'/'" + StringUtils.getNormalForm(chars[c]) + "'/" + ((int) chars[c]) + ")");
//			String chs = ("" + chars[c]);
//			int chw = ((otrs[c].rWidth == Integer.MIN_VALUE) ? dWidth : (nWidth + otrs[c].rWidth));
			int chw = ((otrs[c].rWidth == 0) ? dWidth : (nWidth + otrs[c].rWidth));
			if (DEBUG_TYPE1C_LOADING) System.out.println(" - char width is " + chw);
//			if (DEBUG_TYPE1C_LOADING) System.out.println(" - " + otrs[c].id + ": " + otrs[c].minX + " < X < " + otrs[c].maxX);
			
			boolean ignoreForMin = ((minIgnoreChars.indexOf(chars[c]) != -1) || (chars[c] != StringUtils.getBaseChar(chars[c])));
			
			//	check if char rendering possible
			if ((otrs[c].maxX <= otrs[c].minX) || (otrs[c].maxY <= otrs[c].minY))
				continue;
			
			//	render char
			Op[] cs = ((Op[]) csIndexContent.get(c));
			int mx = 8;
			int my = ((mx * (maxCapHeight - maxDescent)) / (otrs[c].maxX - otrs[c].minX));
			OpGraphics ogr = new OpGraphics(
					otrs[c].minX,
					maxDescent,
					(maxCapHeight - maxDescent + (my / 2)),
					scale,
					new BufferedImage(
							Math.round(scale * (otrs[c].maxX - otrs[c].minX + mx)),
							Math.round(scale * (maxCapHeight - maxDescent + my)),
							BufferedImage.TYPE_INT_RGB)
					);
//			runFontType1Ops(cs, ogr, otrs[c].isMultiPath, false, fidd, sid.intValue());
//			runFontType1Ops(cs, ogr, otrs[c].isMultiPath, (sid.intValue() > 390), fidd, sid.intValue());
			runFontType1Ops(cs, ogr, otrs[c].isMultiPath, (0 < DEBUG_TYPE1C_TARGET_SID), fidd, sid.intValue());
			imgs[c] = ogr.img;
			if (DEBUG_TYPE1C_LOADING) System.out.println(" - image rendered");
			
			//	cut image margins from letters and digits
			BufferedImage charImg = imgs[c];
			AnalysisImage ai = Imaging.wrapImage(charImg, null);
			ImagePartRectangle ipr = Imaging.getContentBox(ai);
			charImg = ipr.toImage().getImage();
			
			//	compute scaled layout
			float cScale = (((float) charImg.getHeight()) / (maxCapHeight - maxDescent));
			int cHeight = ((int) Math.floor(cScale * (otrs[c].maxY - otrs[c].minY)));
			
			//	measure best match
			float bestSim = -1;
			float bestSimNs = -1;
			
			//	try named char match first (render known chars to fill whole image)
			CharMatchResult matchResult = matchChar(charImg, chars[c], true, cScale, cHeight, simFontSize, serifFonts, sansFonts);
			float oFontSizeSum = 0;
			float oSimSum = 0;
			int oSimCount = 0;
			for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
				if (matchResult.serifStyleSims[s] != null) {
					oFontSizeSum += matchResult.serifStyleSims[s].match.fontSize;
					oSimSum += matchResult.serifStyleSims[s].sim;
					oSimCount++;
				}
				if (matchResult.sansStyleSims[s] != null) {
					oFontSizeSum += matchResult.sansStyleSims[s].match.fontSize;
					oSimSum += matchResult.sansStyleSims[s].sim;
					oSimCount++;
				}
			}
			if (DEBUG_TYPE1C_LOADING) {
				System.out.println(" - average match font size is " + ((oSimCount == 0) ? 0 : (oFontSizeSum / oSimCount)));
				System.out.println(" - average similarity is " + ((oSimCount == 0) ? 0 : (oSimSum / oSimCount)));
			}
			
			//	if ToUnicode mapping exists, verify it, and use whatever fits better
			if (pFont.ucMappings.containsKey(new Integer((int) chars[c]))) {
				String ucStr = ((String) pFont.ucMappings.get(new Integer((int) chars[c])));
				if ((ucStr != null) && (ucStr.length() == 1) && (ucStr.charAt(0) != chars[c])) {
					char ucChar = ucStr.charAt(0);
					if (DEBUG_TYPE1C_LOADING) System.out.println(" - testing Unicode mapping '" + ucChar + "'");
					CharMatchResult ucMatchResult = matchChar(charImg, ucChar, (chars[c] != Character.toUpperCase(ucChar)), cScale, cHeight, simFontSize, serifFonts, sansFonts);
					
					float ucFontSizeSum = 0;
					float ucSimSum = 0;
					int ucSimCount = 0;
					for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
						if (ucMatchResult.serifStyleSims[s] != null) {
							ucFontSizeSum += ucMatchResult.serifStyleSims[s].match.fontSize;
							ucSimSum += ucMatchResult.serifStyleSims[s].sim;
							ucSimCount++;
						}
						if (ucMatchResult.sansStyleSims[s] != null) {
							ucFontSizeSum += ucMatchResult.sansStyleSims[s].match.fontSize;
							ucSimSum += ucMatchResult.sansStyleSims[s].sim;
							ucSimCount++;
						}
					}
					if (DEBUG_TYPE1C_LOADING) {
						System.out.println(" - average match font size is " + ((ucSimCount == 0) ? 0 : (ucFontSizeSum / ucSimCount)));
						System.out.println(" - average similarity is " + ((ucSimCount == 0) ? 0 : (ucSimSum / ucSimCount)));
					}
					
					int originalBetter = 0;
					int ucMappingBetter = 0;
					for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
						if ((matchResult.serifStyleSims[s] != null) && (ucMatchResult.serifStyleSims[s] != null)) {
							if (matchResult.serifStyleSims[s].sim < ucMatchResult.serifStyleSims[s].sim) {
								ucMappingBetter++;
								matchResult.serifStyleSims[s] = ucMatchResult.serifStyleSims[s];
							}
							else originalBetter++;
						}
						else if (ucMatchResult.serifStyleSims[s] != null) {
							ucMappingBetter++;
							matchResult.serifStyleSims[s] = ucMatchResult.serifStyleSims[s];
						}
						else if (matchResult.serifStyleSims[s] != null)
							originalBetter++;
						
						if ((matchResult.sansStyleSims[s] != null) && (ucMatchResult.sansStyleSims[s] != null)) {
							if (matchResult.sansStyleSims[s].sim < ucMatchResult.sansStyleSims[s].sim) {
								ucMappingBetter++;
								matchResult.sansStyleSims[s] = ucMatchResult.sansStyleSims[s];
							}
							else originalBetter++;
						}
						else if (ucMatchResult.sansStyleSims[s] != null) {
							ucMappingBetter++;
							matchResult.sansStyleSims[s] = ucMatchResult.sansStyleSims[s];
						}
						else if (matchResult.sansStyleSims[s] != null)
							originalBetter++;
					}
					
					if (originalBetter > ucMappingBetter) {
						pFont.ucMappings.remove(new Integer((int) chars[c]));
						if (DEBUG_TYPE1C_LOADING) System.out.println(" --> found original char to be better match (" + originalBetter + " vs. " + ucMappingBetter + "), removing mapping");
					}
					else if (DEBUG_TYPE1C_LOADING) System.out.println(" --> found mapped char to be better match (" + ucMappingBetter + " vs. " + originalBetter + ")");
				}
			}
			
			//	evaluate match result
			if (matchResult.rendered) {
				simFontSize = matchResult.simFontSize;
				charCount++;
				if ((matchResult.serifStyleSims[Font.PLAIN] != null) || (matchResult.serifStyleSims[Font.BOLD] != null) || (matchResult.sansStyleSims[Font.PLAIN] != null) || (matchResult.sansStyleSims[Font.BOLD] != null) || (skewChars.indexOf(chars[c]) == -1))
					charCountNs++;
				for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
					serifStyleSims[c][s] = matchResult.serifStyleSims[s];
					if (serifStyleSims[c][s] == null) {
						serifStyleSimMins[s] = 0;
						continue;
					}
					serifCharCounts[s]++;
					serifStyleSimSums[s] += serifStyleSims[c][s].sim;
					bestSim = Math.max(bestSim, serifStyleSims[c][s].sim);
					if (!ignoreForMin)
						serifStyleSimMins[s] = Math.min(serifStyleSims[c][s].sim, serifStyleSimMins[s]);
					if (((s & Font.ITALIC) == 0) || (skewChars.indexOf(chars[c]) == -1)) {
						serifCharCountsNs[s]++;
						serifStyleSimSumsNs[s] += serifStyleSims[c][s].sim;
						bestSimNs = Math.max(bestSimNs, serifStyleSims[c][s].sim);
						if (!ignoreForMin)
							serifStyleSimMinsNs[s] = Math.min(serifStyleSims[c][s].sim, serifStyleSimMinsNs[s]);
					}
				}
				for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
					sansStyleSims[c][s] = matchResult.sansStyleSims[s];
					if (sansStyleSims[c][s] == null) {
						sansStyleSimMins[s] = 0;
						continue;
					}
					sansCharCounts[s]++;
					sansStyleSimSums[s] += sansStyleSims[c][s].sim;
					bestSim = Math.max(bestSim, sansStyleSims[c][s].sim);
					if (!ignoreForMin)
						sansStyleSimMins[s] = Math.min(sansStyleSims[c][s].sim, sansStyleSimMins[s]);
					if (((s & Font.ITALIC) == 0) || (skewChars.indexOf(chars[c]) == -1)) {
						sansCharCountsNs[s]++;
						sansStyleSimSumsNs[s] += sansStyleSims[c][s].sim;
						bestSimNs = Math.max(bestSimNs, sansStyleSims[c][s].sim);
						if (!ignoreForMin)
							sansStyleSimMinsNs[s] = Math.min(sansStyleSims[c][s].sim, sansStyleSimMinsNs[s]);
					}
				}
			}
			
			//	update overall measures
			if (DEBUG_TYPE1C_LOADING) System.out.println(" --> best similarity is " + bestSim + " / " + bestSimNs);
			if (!ignoreForMin) {
				if (bestSim >= 0)
					simMin = Math.min(simMin, bestSim);
				if (bestSimNs >= 0)
					simMinNs = Math.min(simMinNs, bestSimNs);
			}
			if (bestSim >= 0)
				simSum += bestSim;
			if (bestSimNs >= 0)
				simSumNs += bestSimNs;
		}
		
		//	use maximum of all fonts and styles when computing min and average similarity
		if (DEBUG_TYPE1C_LOADING) System.out.println(" - min similarity is " + simMin + " all / " + simMinNs + " non-skewed");
		double sim = ((charCount == 0) ? 0 : (simSum / charCount));
		double simNs = ((charCountNs == 0) ? 0 : (simSumNs / charCountNs));
		if (DEBUG_TYPE1C_LOADING) System.out.println(" - average similarity is " + sim + " (" + charCount + ") all / " + simNs + " (" + charCountNs + ") non-skewed");
		
		//	do we have a match? (be more strict with fewer chars, as fonts with few chars tend to be the oddjobs)
//		if ((simMin > 0.5) && (sim > ((charCount < 26) ? 0.7 : 0.6))) {
		if ((simMin > 0.5) && (sim > 0.6)) {
			if (DEBUG_TYPE1C_LOADING) System.out.println(" ==> match");
			
			//	try to select font style if pool sufficiently large
			int bestStyle = -1;
			if (Math.max(serifStyleSimSums.length, sansStyleSimSums.length) >= 2) {
				double bestStyleSim = 0;
				for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
					if (DEBUG_TYPE1C_LOADING) {
						System.out.println(" - checking style " + s);
						System.out.println("   - min similarity is " + serifStyleSimMins[s] + "/" + sansStyleSimMins[s] + " all / " + serifStyleSimMinsNs[s] + "/" + sansStyleSimMinsNs[s] + " non-skewed");
					}
					double serifStyleSim = ((serifCharCounts[s] == 0) ? 0 : (serifStyleSimSums[s] / serifCharCounts[s]));
					double serifStyleSimNs = ((serifCharCountsNs[s] == 0) ? 0 : (serifStyleSimSumsNs[s] / serifCharCountsNs[s]));
					double sansStyleSim = ((sansCharCounts[s] == 0) ? 0 : (sansStyleSimSums[s] / sansCharCounts[s]));
					double sansStyleSimNs = ((sansCharCountsNs[s] == 0) ? 0 : (sansStyleSimSumsNs[s] / sansCharCountsNs[s]));
					if (DEBUG_TYPE1C_LOADING) System.out.println("   - average similarity is " + serifStyleSim + "/" + sansStyleSim + " all / " + serifStyleSimNs + "/" + sansStyleSimNs + " non-skewed");
					if ((((s & Font.ITALIC) == 0) ? Math.max(serifStyleSim, sansStyleSim) : Math.max(serifStyleSimNs, sansStyleSimNs)) > bestStyleSim) {
						if (DEBUG_TYPE1C_LOADING) System.out.println(" ==> new best match style");
						bestStyleSim = (((s & Font.ITALIC) == 0) ? Math.max(serifStyleSim, sansStyleSim) : Math.max(serifStyleSimNs, sansStyleSimNs));
						bestStyle = s;
					}
				}
			}
			
			//	style not found, use plain
			if (bestStyle == -1)
				bestStyle = 0;
			
			//	set base font according to style
			pFont.bold = ((Font.BOLD & bestStyle) != 0);
			pFont.italic = ((Font.ITALIC & bestStyle) != 0);
			System.out.println(" ==> font decoded");
			if ((pFont.baseFont == null) || (pFont.bold != pFont.baseFont.bold) || (pFont.italic != pFont.baseFont.italic)) {
				String bfn = "Times-";
				if (pFont.bold && pFont.italic)
					bfn += "BoldItalic";
				else if (pFont.bold)
					bfn += "Bold";
				else if (pFont.italic)
					bfn += "Italic";
				else bfn += "Roman";
				pFont.setBaseFont(getBuiltInFont(bfn, false));
			}
			
			//	store character widths
			if (dWidth != -1)
				pFont.mCharWidth = dWidth;
			for (int c = 0; c < Math.min(csContent.size(), csIndexContent.size()); c++) {
				Integer sid = ((Integer) csContent.get(c));
				if (sid.intValue() == 0)
					continue;
			}
			
			//	check for descent
			pFont.hasDescent = (maxDescent < -150);
			
			//	we're done here
			if (unresolvedCodes.isEmpty())
				return;
		}
		
		//	reset chars with known names to mark them for re-evaluation (required with fonts that use arbitrary char names)
		else Arrays.fill(chars, ((char) 0));
		
		//	cache character images to speed up matters
		final float[] cacheHitRate = {0};
		HashMap cache = new HashMap() {
			int lookups = 0;
			int hits = 0;
			public Object get(Object key) {
				this.lookups ++;
				Object value = super.get(key);
				if (value != null)
					this.hits++;
				cacheHitRate[0] = (((float) this.hits) / this.lookups);
				return value;
			}
		};
		
		//	decode remaining characters
		for (int c = 0; c < Math.min(csContent.size(), csIndexContent.size()); c++) {
			if (chars[c] != 0)
				continue;
			Integer sid = ((Integer) csContent.get(c));
			if (sid.intValue() == 0)
				continue;
			if ((0 < DEBUG_TYPE1C_TARGET_SID) && (sid.intValue() != DEBUG_TYPE1C_TARGET_SID))
				continue;
			String chn = ((String) sidResolver.get(sid));
			if ((chn == null) && (sid.intValue() >= sidResolver.size()) && ((sid.intValue() - sidResolver.size()) < sidIndex.size()))
				chn = ((String) sidIndex.get(sid.intValue() - sidResolver.size()));
			Integer chc = ((Integer) unresolvedCodes.get(chn));
			if (chc == null)
				chc = ((Integer) resolvedCodes.get(chn));
			if (chc == null)
				continue;
			if (DEBUG_TYPE1C_LOADING) System.out.println("Decoding char " + c + ", code is " + chc + ", SID is " + sid + " (" + chn + "/'" + chars[c] + "'/'" + StringUtils.getNormalForm(chars[c]) + "')");
			
			//	don't try to match space
			if (imgs[c] == null)
				continue;
			
			//	compute char layout scale
			float cScale = (((float) imgs[c].getHeight()) / (maxCapHeight - maxDescent));
			
			//	try serif match first
			ImageSimilarity bestSim = getPunctuationChar(imgs[c], cScale, maxDescent, otrs[c], serifFont, ((simFontSize < 0) ? imgs[c].getHeight() : simFontSize), cache);
			if (bestSim != null)
				simFontSize = bestSim.match.fontSize;
			
//			//	no sufficient match, try sans serif ==> seems to do more harm than good ...
//			if ((bestSim == null) || (bestSim.sim < 0.8)) {
//				ImageSimilarity bestSimSans = getPunctuationChar(imgs[c], cScale, maxDescent, otrs[c], fontSans);
//				if ((bestSimSans != null) && ((bestSim == null) || (bestSim.sim < bestSimSans.sim)))
//					bestSim = bestSimSans;
//			}
//			
			//	do we have a reliable match?
			if ((bestSim != null) && (bestSim.sim > 0.8)) {
				if (DEBUG_TYPE1C_LOADING) System.out.println(" ==> char decoded (1) to '" + bestSim.match.ch + "' (" + StringUtils.getCharName(bestSim.match.ch) + ", '" + StringUtils.getNormalForm(bestSim.match.ch) + "') at " + bestSim.sim + ", cache hit rate at " + cacheHitRate[0]);
				Character bsCh = new Character(bestSim.match.ch);
				
				//	correct char mapping specified in differences array
				pFont.mapDifference(chc, bsCh);
				pFont.mapUnicode(chc, StringUtils.getNormalForm(bsCh.charValue()));
				
				//	map char named by SID to what image actually displays
				if (DEBUG_TYPE1C_LOADING) System.out.println(" ==> mapped (1) " + chc + " to " + bsCh);
				
				//	store font size relative baseline shift
				pFont.setRelativeBaselineShift(bsCh, (((float) bestSim.match.shiftBelowBaseline) / bestSim.match.fontSize));
				
				//	no need to hassle about this char any more
				continue;
			}
			
			//	try basic ASCII letters and digits if punctuation match not sufficiently good
			for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
				TreeSet baseChars = getBaseChars(imgs[c], cScale, maxDescent, otrs[c], serifFonts[s], cache);
				ImageSimilarity bestBcSim = null;
				for (Iterator isit = baseChars.iterator(); isit.hasNext();) {
					ImageSimilarity is = ((ImageSimilarity) isit.next());
					if ((bestBcSim == null) || (bestBcSim.sim == 0))
						bestBcSim = is;
					else if ((is.sim + 0.05f) < bestBcSim.sim)
						break;
					if (0 < DEBUG_TYPE1C_TARGET_SID)
						displayCharMatch(imgs[c], is, "Good Match");
					//	TODO put actual base chars in set
					
					//	TODO try to match chars derived from base chars
				}
				
				//	store best match found
				if ((bestBcSim != null) && ((bestSim == null) || (bestSim.sim < bestBcSim.sim))) {
					bestSim = bestBcSim;
//					if (DEBUG_TYPE1C_LOADING)
//						displayCharMatch(imgs[c], bestSim, "New best base char match");
				}
			}
			
			//	use whatever we got
			if (bestSim != null) {
				if (DEBUG_TYPE1C_LOADING) System.out.println(" ==> char decoded (2) to '" + bestSim.match.ch + "' (" + StringUtils.getCharName(bestSim.match.ch) + ", '" + StringUtils.getNormalForm(bestSim.match.ch) + "') at " + bestSim.sim + ", cache hit rate at " + cacheHitRate[0]);
//				if (DEBUG_TYPE1C_LOADING && (bestSim.sim < 0.7))
//					displayCharMatch(imgs[c], bestSim, "Best Match");
				Character bsCh = new Character(bestSim.match.ch);
				
				//	correct char mapping specified in differences array
				pFont.mapDifference(chc, bsCh);
				pFont.mapUnicode(chc, StringUtils.getNormalForm(bsCh.charValue()));
				if (DEBUG_TYPE1C_LOADING) System.out.println(" ==> mapped (2) " + chc + " to " + bsCh);
				
				//	check for descent
				pFont.hasDescent = (maxDescent < -150);
				
				//	store font size relative baseline shift
				pFont.setRelativeBaselineShift(bsCh, (((float) bestSim.match.shiftBelowBaseline) / bestSim.match.fontSize));
			}
		}
		
		//	TODO exploit mid-char move-to's to narrow down search
		//	TODO exploit relative position of char in line
		//	TODO cache char images by font image size
	}
	
	private static class CharMatchResult {
		boolean rendered = false;
		float simFontSize = -1;
		ImageSimilarity[] serifStyleSims = new ImageSimilarity[4];
		ImageSimilarity[] sansStyleSims = new ImageSimilarity[4];
		CharMatchResult(float simFontSize) {
			this.simFontSize = simFontSize;
		}
	}
	
	private static CharMatchResult matchChar(BufferedImage charImg, char ch, boolean allowCaps, float cScale, int cHeight, float simFontSize, Font[] serifFonts, Font[] sansFonts) {
		CharMatchResult matchResult = new CharMatchResult(simFontSize);
		
		//	try named char match first (render known chars to fill whole image)
		for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
			CharImage simImg = createImageChar(charImg.getWidth(), charImg.getHeight(), ch, serifFonts[s], ((matchResult.simFontSize < 0) ? cHeight : matchResult.simFontSize), null);
			matchResult.serifStyleSims[s] = matchChar(charImg, simImg, (ch == DEBUG_MATCH_TARGET_CHAR));
			if (allowCaps && (ch != Character.toUpperCase(ch))) {
				CharImage capSimImg = createImageChar(charImg.getWidth(), charImg.getHeight(), Character.toUpperCase(ch), serifFonts[s], ((matchResult.simFontSize < 0) ? cHeight : matchResult.simFontSize), null);
				ImageSimilarity capSim = matchChar(charImg, capSimImg, (ch == DEBUG_MATCH_TARGET_CHAR));
				if ((capSim != null) && ((matchResult.serifStyleSims[s] == null) || (matchResult.serifStyleSims[s].sim < capSim.sim)))
					matchResult.serifStyleSims[s] = capSim;
			}
			if (matchResult.serifStyleSims[s] == null)
				continue;
			matchResult.simFontSize = matchResult.serifStyleSims[s].match.fontSize;
			matchResult.rendered = true;
		}
		for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
			CharImage simImg = createImageChar(charImg.getWidth(), charImg.getHeight(), ch, sansFonts[s], ((matchResult.simFontSize < 0) ? cHeight : matchResult.simFontSize), null);
			matchResult.sansStyleSims[s] = matchChar(charImg, simImg, (ch == DEBUG_MATCH_TARGET_CHAR));
			if (allowCaps && (ch != Character.toUpperCase(ch))) {
				CharImage capSimImg = createImageChar(charImg.getWidth(), charImg.getHeight(), Character.toUpperCase(ch), sansFonts[s], ((matchResult.simFontSize < 0) ? cHeight : matchResult.simFontSize), null);
				ImageSimilarity capSim = matchChar(charImg, capSimImg, (ch == DEBUG_MATCH_TARGET_CHAR));
				if ((capSim != null) && ((matchResult.sansStyleSims[s] == null) || (matchResult.sansStyleSims[s].sim < capSim.sim)))
					matchResult.sansStyleSims[s] = capSim;
			}
			if (matchResult.sansStyleSims[s] == null)
				continue;
			matchResult.simFontSize = matchResult.sansStyleSims[s].match.fontSize;
			matchResult.rendered = true;
		}
//		
//		//	TODO if ToUnicode mapping exists, verify it
//		if (pFont.ucMappings.containsKey(new Integer((int) chars[c]))) {
//			String ucStr = ((String) pFont.ucMappings.get(new Integer((int) chars[c])));
//			if ((ucStr != null) && (ucStr.length() == 1) && (ucStr.charAt(0) != chars[c])) {
//				char ucChar = ucStr.charAt(0);
//				for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
////					System.out.println("   - trying serif style " + s);
//					CharImage simImg = createImageChar(charImg.getWidth(), charImg.getHeight(), ucChar, serifFonts[s], ((simFontSize < 0) ? cHeight : simFontSize), null);
//					serifStyleSims[c][s] = matchChar(charImg, simImg, (ucChar == DEBUG_MATCH_TARGET_CHAR));
//					if (ucChar != Character.toUpperCase(ucChar)) {
//						CharImage capSimImg = createImageChar(charImg.getWidth(), charImg.getHeight(), Character.toUpperCase(ucChar), serifFonts[s], ((simFontSize < 0) ? cHeight : simFontSize), null);
//						ImageSimilarity capSim = matchChar(charImg, capSimImg, (ucChar == DEBUG_MATCH_TARGET_CHAR));
//						if ((capSim != null) && ((serifStyleSims[c][s] == null) || (serifStyleSims[c][s].sim < capSim.sim)))
//							serifStyleSims[c][s] = capSim;
//					}
//					if (serifStyleSims[c][s] == null) {
////						System.out.println("   --> could not render image");
//						serifStyleSimMins[s] = 0;
//						continue;
//					}
//					simFontSize = serifStyleSims[c][s].match.fontSize;
////					System.out.println("   --> similarity is " + serifStyleSims[c][s].sim);
//					serifCharCounts[s]++;
//					serifStyleSimSums[s] += serifStyleSims[c][s].sim;
//					bestSim = Math.max(bestSim, serifStyleSims[c][s].sim);
//					if (charToCount) {
//						charCount++;
//						charToCount = false;
//					}
//					if (!ignoreForMin)
//						serifStyleSimMins[s] = Math.min(serifStyleSims[c][s].sim, serifStyleSimMins[s]);
//					if (((s & Font.ITALIC) == 0) || (skewChars.indexOf(ucChar) == -1)) {
//						serifCharCountsNs[s]++;
//						serifStyleSimSumsNs[s] += serifStyleSims[c][s].sim;
//						bestSimNs = Math.max(bestSimNs, serifStyleSims[c][s].sim);
//						if (charToCountNs) {
//							charCountNs++;
//							charToCountNs = false;
//						}
//						if (!ignoreForMin)
//							serifStyleSimMinsNs[s] = Math.min(serifStyleSims[c][s].sim, serifStyleSimMinsNs[s]);
//					}
//				}
//				for (int s = Font.PLAIN; s <= (Font.BOLD | Font.ITALIC); s++) {
////					System.out.println("   - trying sans style " + s);
//					CharImage simImg = createImageChar(charImg.getWidth(), charImg.getHeight(), ucChar, sansFonts[s], ((simFontSize < 0) ? cHeight : simFontSize), null);
//					sansStyleSims[c][s] = matchChar(charImg, simImg, (ucChar == DEBUG_MATCH_TARGET_CHAR));
//					if (ucChar != Character.toUpperCase(ucChar)) {
//						CharImage capSimImg = createImageChar(charImg.getWidth(), charImg.getHeight(), Character.toUpperCase(ucChar), sansFonts[s], ((simFontSize < 0) ? cHeight : simFontSize), null);
//						ImageSimilarity capSim = matchChar(charImg, capSimImg, (ucChar == DEBUG_MATCH_TARGET_CHAR));
//						if ((capSim != null) && ((sansStyleSims[c][s] == null) || (sansStyleSims[c][s].sim < capSim.sim)))
//							sansStyleSims[c][s] = capSim;
//					}
//					if (sansStyleSims[c][s] == null) {
////						System.out.println("   --> could not render image");
//						sansStyleSimMins[s] = 0;
//						continue;
//					}
//					simFontSize = sansStyleSims[c][s].match.fontSize;
////					System.out.println("   --> similarity is " + sansStyleSims[c][s].sim);
//					sansCharCounts[s]++;
//					sansStyleSimSums[s] += sansStyleSims[c][s].sim;
//					bestSim = Math.max(bestSim, sansStyleSims[c][s].sim);
//					if (charToCount) {
//						charCount++;
//						charToCount = false;
//					}
//					if (!ignoreForMin)
//						sansStyleSimMins[s] = Math.min(sansStyleSims[c][s].sim, sansStyleSimMins[s]);
//					if (((s & Font.ITALIC) == 0) || (skewChars.indexOf(ucChar) == -1)) {
//						sansCharCountsNs[s]++;
//						sansStyleSimSumsNs[s] += sansStyleSims[c][s].sim;
//						bestSimNs = Math.max(bestSimNs, sansStyleSims[c][s].sim);
//						if (charToCountNs) {
//							charCountNs++;
//							charToCountNs = false;
//						}
//						if (!ignoreForMin)
//							sansStyleSimMinsNs[s] = Math.min(sansStyleSims[c][s].sim, sansStyleSimMinsNs[s]);
//					}
//				}
//			}
//		}
		
		return matchResult;
	}
	
	private static ImageSimilarity getPunctuationChar(BufferedImage img, float cScale, int maxDescent, OpTracker otr, Font font, float fontSize, HashMap cache) {
		
		//	compute char image metrics
		int cWidth = ((int) Math.floor((otr.maxX - otr.minX) * cScale));
		int cHeight = ((int) Math.floor(cScale * (otr.maxY - otr.minY)));
		int cBase = ((int) Math.floor(cScale * -maxDescent));
		int cDesc = ((int) Math.floor(cScale * -otr.minPaintY));
		int lSpace = Math.max(0, ((int) Math.floor(cScale * otr.minPaintX)));
		
		//	set up statistics
		ImageSimilarity bestSim = null;
		
		//	try Unicode punctuation characters first
		for (int pch = 33; pch < highestPossiblePunctuationMark; pch++) {
			
			//	stop after basic ISO-8859 punctuation marks if satisfacory match found
			if ((pch == 256) && (bestSim != null) && (bestSim.sim > 0.8))
				return bestSim;
			
			//	jump reserved ASCII range
			if ((127 <= pch) && (pch <= 159))
				continue;
			
			//	jump letters and digits, we're only out for punctuation here
			if (Character.isLetterOrDigit((char) pch))
				continue;
			
			//	jump whitespace
			if (Character.isWhitespace((char) pch))
				continue;
			if ((688 <= pch) && (pch <= 767)) {
				pch = 767;
				continue;
			}
			
			//	jump diacritic modifiers
			if ((768 <= pch) && (pch <= 879)) {
				pch = 879;
				continue;
			}
			
			//	jump Greek, Cyrillic, ..., Mongolian, Latin Extended Additional, Greek Extended Additional
			if ((880 <= pch) && (pch <= 6319)) {
				pch = 6319;
				continue; 
			}
			
			//	jump Box Drawing, Block Elements, Geometric Shapes 
			if ((9472 <= pch) && (pch <= 9727)) {
				pch = 9727;
				continue;
			}
			
			//	jump unprintable characters
			if (!font.canDisplay((char) pch))
				continue;
			
			//	jump plain diacritic markers
			if (diacriticMarkerChars.indexOf((char) pch) != -1)
				continue;
			
//			System.out.println(" - trying char " + pch + " ('" + ((char) pch) + "', " + StringUtils.getCharName((char) pch) + ")");
			ImageSimilarity sim = null;
			
			//	increase match offset step by step as long as best similarity below 50%
			for (int o = 2; o <= 4; o *= 2) {
				
				//	if char high above baseline (negative descent), use baseline-aware rendering in such cases
				if ((-cDesc * 3) > cHeight)
					sim = matchChar(img, cWidth, cHeight, lSpace, cBase, cDesc, ((char) pch), font, ((bestSim == null) ? fontSize : bestSim.match.fontSize), cache, 2, false);
				
				//	use baseline-agnostic rendering in the other cases
				else {
					AnalysisImage ai = Imaging.wrapImage(img, null);
					ImagePartRectangle ipr = Imaging.getContentBox(ai);
					sim = matchChar(ipr.toImage().getImage(), (img.getHeight() - ipr.getBottomRow()), ((char) pch), font, ((bestSim == null) ? fontSize : bestSim.match.fontSize), cache, 2, false);
				}
				
				//	this one's good enough
				if ((sim != null) && (sim.sim > 0.5))
					break;
			}
			
			if (sim == null) {
//				System.out.println("   --> could not render image");
				continue;
			}
//			System.out.println("   --> similarity is " + sim.sim);
			if ((bestSim == null) || (sim.sim > bestSim.sim)) {
				bestSim = sim;
				System.out.println("   ==> new best match " + pch + " ('" + ((char) pch) + "', " + StringUtils.getCharName((char) pch) + "), similarity is " + sim.sim);
//				//	TODO remove this after tests
//				displayCharMatch(bestSim.base, bestSim, "New best punctuation match");
			}
		}
		
		//	finally ...
		return bestSim;
	}
	
	private static HashSet unrenderableChars = new HashSet();
	
	private static TreeSet getBaseChars(BufferedImage img, float cScale, int maxDescent, OpTracker otr, Font font, HashMap cache) {
		
		//	compute char image metrics
		int cWidth = ((int) Math.floor((otr.maxX - otr.minX) * cScale));
		int cHeight = ((int) Math.floor(cScale * (otr.maxY - otr.minY)));
		int cBase = ((int) Math.floor(cScale * -maxDescent));
		int cDesc = ((int) Math.floor(cScale * -otr.minPaintY));
		int lSpace = Math.max(0, ((int) Math.floor(cScale * otr.minPaintX)));
		
		//	set up statistics
		TreeSet baseChars = new TreeSet(new Comparator() {
			public int compare(Object is1, Object is2) {
				return Double.compare(((ImageSimilarity) is2).sim, ((ImageSimilarity) is1).sim);
			}
		});
		float simFontSize = -1;
		
		//	try basic latin characters
		for (int bch = 33; bch < 127; bch++) {
			
			//	jump non-letters
			if (!Character.isLetter((char) bch))
				continue;
			
//			System.out.println(" - trying char " + bch + " ('" + ((char) bch) + "', " + StringUtils.getCharName((char) bch) + ")");
			ImageSimilarity sim = null;
			
			//	increase match offset step by step as long as best similarity below 50%
			for (int o = 2; o <= 4; o *= 2) {
				
				//	if char high above baseline (negative descent), use baseline-aware rendering in such cases
				if ((-cDesc * 3) > cHeight)
					sim = matchChar(img, cWidth, cHeight, lSpace, cBase, cDesc, ((char) bch), font, ((simFontSize < 0) ? img.getHeight() : simFontSize), cache, 2, false);
				
				//	use baseline-agnostic rendering in the other cases
				else {
					AnalysisImage ai = Imaging.wrapImage(img, null);
					ImagePartRectangle ipr = Imaging.getContentBox(ai);
					sim = matchChar(ipr.toImage().getImage(), (img.getHeight() - ipr.getBottomRow()), ((char) bch), font, ((simFontSize < 0) ? img.getHeight() : simFontSize), cache, 2, false);
				}
				
				//	this one's good enough
				if ((sim != null) && (sim.sim > 0.5))
					break;
			}
			
			if (sim == null) {
				System.out.println("   --> could not render image");
				continue;
			}
			
			simFontSize = sim.match.fontSize;
			
//			System.out.println("   --> similarity is " + sim.sim);
			if (sim.sim > 0.5)
				baseChars.add(sim);
		}
		
		//	finally ...
		return baseChars;
	}
	
	private static final String skewChars = "AIJSVWXYfgsvwxy7()[]{}/\\!%&"; // chars that can cause trouble in italic fonts due to varying angle
	private static final String diacriticMarkerChars = (""
			+ StringUtils.getCharForName("acute")
			+ StringUtils.getCharForName("grave")
			+ StringUtils.getCharForName("breve")
			+ StringUtils.getCharForName("circumflex")
			+ StringUtils.getCharForName("cedilla")
			+ StringUtils.getCharForName("dieresis")
			+ StringUtils.getCharForName("macron")
			+ StringUtils.getCharForName("caron")
			+ StringUtils.getCharForName("ogonek")
			+ StringUtils.getCharForName("ring")
		);
	
	/*
	 * chars (mostly actually char modifiers, safe for a few punctuation marks,
	 * and ligatures) that vary too widely across fonts to prevent a font match;
	 * we have to exclude both upper and lower case V and W as well, as they
	 * vary a lot in their angle (both upper and lower case) or are round (lower
	 * case) in some italics font faces and thus won't match comparison; in
	 * addition, capital A also varies too much in italic angle, and the
	 * descending tails of capital J and Q and lower case Y and Z exhibit
	 * extreme variability as well
	 */
	private static final String minIgnoreChars = ("%@?*&" + diacriticMarkerChars + '\uFB00' + '\uFB01' + '\uFB02' + '\uFB03' + '\uFB04' + '0' + 'A' + 'J' + 'O' + 'Q' + 'V' + 'W' + 'k' + 'v' + 'w' + 'y' + 'z' + '\u00F8' + '\u00BC' + '\u00BD' + '\u2153'); 
	private static final int highestPossiblePunctuationMark = 9842; // corresponds to 2672 in Unicode, the end of the Misc Symbols range (we need this for 'male' and 'female' ...)
	
	private static void displayCharMatch(BufferedImage img, ImageSimilarity maxSim, String message) {
		BufferedImage bi = new BufferedImage((img.getWidth() + 1 + maxSim.match.img.getWidth() + 1 + Math.max(img.getWidth(), maxSim.match.img.getWidth())), Math.max(img.getHeight(), maxSim.match.img.getHeight()), ((img.getType() == BufferedImage.TYPE_CUSTOM) ? BufferedImage.TYPE_BYTE_GRAY : img.getType()));
		Graphics g = bi.getGraphics();
		g.setColor(Color.WHITE);
		g.drawImage(img, 0, 0, null);
		if (img.getHeight() < bi.getHeight())
			g.fillRect(0, img.getHeight(), img.getWidth(), (bi.getHeight() - img.getHeight()));
		g.drawImage(maxSim.match.img, (img.getWidth() + 1), 0, null);
		for (int x = 0; x < Math.max(img.getWidth(), (maxSim.match.img.getWidth()-maxSim.xOff)); x++) {
			for (int y = 0; y < Math.max(img.getHeight(), (maxSim.match.img.getHeight()-maxSim.yOff)); y++) {
				int rgb1 = (((x >= 0) && (x < img.getWidth()) && (y >= 0) && (y < img.getHeight())) ? img.getRGB(x, y) : white);
				int rgb2 = ((((x+maxSim.xOff) >= 0) && ((x+maxSim.xOff) < maxSim.match.img.getWidth()) && ((y+maxSim.yOff) >= 0) && ((y+maxSim.yOff) < maxSim.match.img.getHeight())) ? maxSim.match.img.getRGB((x+maxSim.xOff), (y+maxSim.yOff)) : white);
				if (rgb1 != rgb2)
					g.drawLine(((img.getWidth() + 1 + maxSim.match.img.getWidth() + 1) + x), y, ((img.getWidth() + 1 + maxSim.match.img.getWidth() + 1 + x)), y);
			}
		}
		JOptionPane.showMessageDialog(null, (message + ": '" + maxSim.match.ch + "', similarity is " + maxSim.sim), "Comparison Image", JOptionPane.PLAIN_MESSAGE, new ImageIcon(bi));
	}
	
	private static abstract class OpReceiver {
//		int rWidth = Integer.MIN_VALUE;
		int rWidth = 0;
		int x = 0;
		int y = 0;
		abstract void moveTo(int dx, int dy);
		abstract void lineTo(int dx, int dy);
		abstract void curveTo(int dx1, int dy1, int dx2, int dy2, int dx3, int dy3);
		abstract void closePath();
	}
	
	private static class OpTracker extends OpReceiver {
//		String id = ("" + Math.random());
		int minX = 0;
		int minY = 0;
		int minPaintX = Integer.MAX_VALUE;
		int minPaintY = Integer.MAX_VALUE;
		int maxX = 0;
		int maxY = 0;
		int mCount = 0;
		boolean isMultiPath = false;
		void moveTo(int dx, int dy) {
			if (this.mCount != 0)
				this.isMultiPath = true;
			this.x += dx;
			this.minX = Math.min(this.minX, this.x);
			this.maxX = Math.max(this.maxX, this.x);
			this.y += dy;
			this.minY = Math.min(this.minY, this.y);
			this.maxY = Math.max(this.maxY, this.y);
//			System.out.println("Move " + this.id + " to " + dx + "/" + dy + ":");
//			System.out.println(" " + this.minX + " < X < " + this.maxX);
//			System.out.println(" " + this.minY + " < Y < " + this.maxY);
		}
		void lineTo(int dx, int dy) {
			this.minPaintX = Math.min(this.minPaintX, this.x);
			this.minPaintY = Math.min(this.minPaintY, this.y);
			this.x += dx;
			this.minX = Math.min(this.minX, this.x);
			this.maxX = Math.max(this.maxX, this.x);
			this.y += dy;
			this.minY = Math.min(this.minY, this.y);
			this.maxY = Math.max(this.maxY, this.y);
			this.mCount++;
			this.minPaintX = Math.min(this.minPaintX, this.x);
			this.minPaintY = Math.min(this.minPaintY, this.y);
//			System.out.println("Line " + this.id + " to " + dx + "/" + dy + ":");
//			System.out.println(" " + this.minX + " < X < " + this.maxX);
//			System.out.println(" " + this.minY + " < Y < " + this.maxY);
		}
		void curveTo(int dx1, int dy1, int dx2, int dy2, int dx3, int dy3) {
			this.lineTo(dx1, dy1);
			this.lineTo(dx2, dy2);
			this.lineTo(dx3, dy3);
		}
		void closePath() {}
	}
	
	private static class OpGraphics extends OpReceiver {
		int minX;
		int minY;
		int height;
		int sx = 0;
		int sy = 0;
		int lCount = 0;
		BufferedImage img;
		Graphics2D gr;
		private float scale = 1.0f;
		OpGraphics(int minX, int minY, int height, float scale, BufferedImage img) {
			this.minX = minX;
			this.minY = minY;
			this.height = height;
			this.scale = scale;
			this.setImage(img);
			this.gr.setColor(Color.WHITE);
			this.gr.fillRect(0, 0, img.getWidth(), img.getHeight());
			this.gr.setColor(Color.BLACK);
		}
		void setImage(BufferedImage img) {
			this.img = img;
			this.gr = this.img.createGraphics();
		}
		void lineTo(int dx, int dy) {
			if (this.lCount == 0) {
				this.sx = this.x;
				this.sy = this.y;
			}
			this.gr.drawLine(
					Math.round(this.scale * (this.x - this.minX)),
					Math.round(this.scale * (this.height - (this.y - this.minY))),
					Math.round(this.scale * (this.x - this.minX + dx)),
					Math.round(this.scale * (this.height - (this.y - this.minY + dy)))
				);
			this.x += dx;
			this.y += dy;
			this.lCount++;
		}
		void curveTo(int dx1, int dy1, int dx2, int dy2, int dx3, int dy3) {
			if (this.lCount == 0) {
				this.sx = this.x;
				this.sy = this.y;
			}
			CubicCurve2D.Float cc = new CubicCurve2D.Float();
			cc.setCurve(
					Math.round(this.scale * (this.x - this.minX)),
					Math.round(this.scale * (this.height - (this.y - this.minY))),
					Math.round(this.scale * (this.x - this.minX + dx1)),
					Math.round(this.scale * (this.height - (this.y - this.minY + dy1))),
					Math.round(this.scale * (this.x - this.minX + dx1 + dx2)),
					Math.round(this.scale * (this.height - (this.y - this.minY + dy1 + dy2))),
					Math.round(this.scale * (this.x - this.minX + dx1 + dx2 + dx3)),
					Math.round(this.scale * (this.height - (this.y - this.minY + dy1 + dy2 + dy3)))
				);
			this.gr.draw(cc);
			this.x += (dx1 + dx2 + dx3);
			this.y += (dy1 + dy2 + dy3);
			this.lCount++;
		}
		void moveTo(int dx, int dy) {
			if (this.lCount != 0)
				this.closePath();
			this.lCount = 0;
			this.x += dx;
			this.y += dy;
		}
		void closePath() {
			this.gr.drawLine(
					Math.round(this.scale * (this.x - this.minX)),
					Math.round(this.scale * (this.height - (this.y - this.minY))),
					Math.round(this.scale * (this.sx - this.minX)),
					Math.round(this.scale * (this.height - (this.sy - this.minY)))
				);
		}
	}
	
	private static final boolean DEBUG_TYPE1C_RENDRING = false;
	private static final int DEBUG_TYPE1C_TARGET_SID = -1;
	
	private static void runFontType1Ops(Op[] ops, OpReceiver opr, boolean isMultiPath, boolean show, ImageDisplayDialog fidd, int sid) {
		ImageDisplayDialog idd = null;
//		boolean skipFirst = false;
		boolean emptyOp = false;
		
		for (int o = 0; o < ops.length; o++) {
			int op = ops[o].op;
//			System.out.print("Executing " + ops[o].name);
//			for (int a = 0; a < ops[o].args.length; a++)
//				System.out.print(" " + ops[o].args[a].intValue());
//			System.out.println();
			
//			int a = 0;
			int skipped = 0;// (skipFirst ? 1 : 0);
			int a = skipped;
//			skipFirst = false;
			
			while (op != -1) {
				
				//	hstem, vstem, hstemhm, or vstemhm
				if ((op == 1) || (op == 3) || (op == 18) || (op == 23)) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "<hints>", skipped, a, ops[o].args);
					if ((o == 0) && ((ops[o].args.length % 2) == 1))
						opr.rWidth += ops[o].args[a++].intValue();
					op = -1;
				}
				
				//	rmoveto |- dx1 dy1 rmoveto (21) |-
				//	moves the current point to a position at the relative coordinates (dx1, dy1).
				else if (op == 21) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "rmoveto", skipped, a, ops[o].args);
//					if ((o == 0) && (ops[o].args.length > 2))
//						opr.rWidth = ops[o].args[a++].intValue();
//					if ((ops[o].args.length > 2))
//						opr.rWidth = ops[o].args[a++].intValue();
					if (ops[o].args.length < 2)
						emptyOp = true;
					if ((o == 0) && ((a+2) < ops[o].args.length))
						opr.rWidth += ops[o].args[a++].intValue();
					opr.moveTo(ops[o].args[a++].intValue(), ops[o].args[a++].intValue());
					op = -1;
				}
				
				//Note 4 The first stack-clearing operator, which must be one of 
				// hstem,
				// hstemhm,
				// vstem,
				// vstemhm,
				// cntrmask,
				// hintmask,
				// - hmoveto,
				// - vmoveto,
				// - rmoveto, or
				// - endchar,
				//takes an additional argument — the width (as described earlier), which may be expressed as zero or one numeric argument.
				
				//	hmoveto |- dx1 hmoveto (22) |-
				//	moves the current point dx1 units in the horizontal direction. See Note 4.
				else if (op == 22) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "hmoveto", skipped, a, ops[o].args);
//					if ((o == 0) && (ops[o].args.length > 1))
//						opr.rWidth = ops[o].args[a++].intValue();
					if (ops[o].args.length < 1)
						emptyOp = true;
					if ((o == 0) && ((a+1) < ops[o].args.length))
						opr.rWidth += ops[o].args[a++].intValue(); 
					opr.moveTo(ops[o].args[a++].intValue(), 0);
					op = -1;
				}
				
				//	vmoveto |- dy1 vmoveto (4) |-
				//	moves the current point dy1 units in the vertical direction. See Note 4.
				else if (op == 4) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "vmoveto", skipped, a, ops[o].args);
//					if ((o == 0) && (ops[o].args.length > 1))
//						opr.rWidth = ops[o].args[a++].intValue();
					if (ops[o].args.length < 1)
						emptyOp = true;
					if ((o == 0) && ((a+1) < ops[o].args.length))
						opr.rWidth += ops[o].args[a++].intValue(); 
					opr.moveTo(0, ops[o].args[a++].intValue());
					op = -1;
				}
				
				//	rlineto |- {dxa dya}+ rlineto (5) |-
				//	appends a line from the current point to a position at the relative coordinates dxa, dya. Additional rlineto operations are performed for all subsequent argument pairs. The number of lines is determined from the number of arguments on the stack.
				else if (op == 5) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "rlineto", skipped, a, ops[o].args);
					if (((ops[o].args.length - a) % 2) != 0)
						a++;
					if (ops[o].args.length < 2)
						emptyOp = true;
					if ((a+2) <= ops[o].args.length)
						opr.lineTo(ops[o].args[a++].intValue(), ops[o].args[a++].intValue());
					else op = -1;
				}
				
				//	hlineto |- dx1 {dya dxb}* hlineto (6) |- OR |- {dxa dyb}+ hlineto (6) |-
				//	appends a horizontal line of length dx1 to the current point.
				//	With an odd number of arguments, subsequent argument pairs are interpreted as alternating values of dy and dx, for which additional lineto operators draw alternating vertical and horizontal lines.
				//	With an even number of arguments, the arguments are interpreted as alternating horizontal and vertical lines. The number of lines is determined from the number of arguments on the stack.
				else if (op == 6) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "hlineto", skipped, a, ops[o].args);
					if (ops[o].args.length < 1)
						emptyOp = true;
					if ((a+1) <= ops[o].args.length) {
						opr.lineTo(ops[o].args[a++].intValue(), 0);
						op = 7;
					}
					else op = -1;
				}
				
				//	vlineto |- dy1 {dxa dyb}* vlineto (7) |- OR |- {dya dxb}+ vlineto (7) |-
				//	appends a vertical line of length dy1 to the current point.
				//	With an odd number of arguments, subsequent argument pairs are interpreted as alternating values of dx and dy, for which additional lineto operators draw alternating horizontal and vertical lines.
				//	With an even number of arguments, the arguments are interpreted as alternating vertical and horizontal lines. The number of lines is determined from the number of arguments on the stack.
				else if (op == 7) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "vlineto", skipped, a, ops[o].args);
					if (ops[o].args.length < 1)
						emptyOp = true;
					if ((a+1) <= ops[o].args.length) {
						opr.lineTo(0, ops[o].args[a++].intValue());
						op = 6;
					}
					else op = -1;
				}
				
				//	rrcurveto |- {dxa dya dxb dyb dxc dyc}+ rrcurveto (8) |-
				//	appends a Bézier curve, defined by dxa...dyc, to the current point. For each subsequent set of six arguments, an additional curve is appended to the current point. The number of curve segments is determined from the number of arguments on the number stack and is limited only by the size of the number stack.
				else if (op == 8) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "rrcurveto", skipped, a, ops[o].args);
					
					//	skip superfluous arguments
					if (a == skipped) {
						while ((a < ops[o].args.length) && (((ops[o].args.length - a) % 6) != 0))
							a++;
						if (DEBUG_TYPE1C_RENDRING)
							System.out.println(" - skipped to parameter " + a);
					}
					if (ops[o].args.length < 6)
						emptyOp = true;
					
					//	execute op
					if ((a+6) <= ops[o].args.length)
						opr.curveTo(ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue());
					else op = -1;
				}
				
				//	hhcurveto |- dy1? {dxa dxb dyb dxc}+ hhcurveto (27) |-
				//	appends one or more Bézier curves, as described by the dxa...dxc set of arguments, to the current point. For each curve, if there are 4 arguments, the curve starts and ends horizontal. The first curve need not start horizontal (the odd argument case). Note the argument order for the odd argument case.
				else if (op == 27) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "hhcurveto", skipped, a, ops[o].args);
					
					//	skip superfluous arguments
					if (a == skipped) {
						while ((a < ops[o].args.length) && (((ops[o].args.length - a) % 4) > 1)) {
							a++;
							skipped++;
						}
						if (DEBUG_TYPE1C_RENDRING)
							System.out.println(" - skipped to parameter " + a);
					}
					if (ops[o].args.length < 4)
						emptyOp = true;
					
					//	execute op
					if ((a+4) <= ops[o].args.length) {
						int dx1;
						int dy1;
						int dx2;
						int dy2;
						int dx3;
						int dy3;
//						if ((a == 0) && ((ops[o].args.length % 4) == 1))
//							dy1 = ops[o].args[a++].intValue();
						if ((a == skipped) && (((ops[o].args.length - skipped) % 4) == 1))
							dy1 = ops[o].args[a++].intValue();
						else dy1 = 0;
						dx1 = ops[o].args[a++].intValue();
						dx2 = ops[o].args[a++].intValue();
						dy2 = ops[o].args[a++].intValue();
						dx3 = ops[o].args[a++].intValue();
						dy3 = 0;
						opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
					}
					else op = -1;
				}
				
				//	hvcurveto |- dx1 dx2 dy2 dy3 {dya dxb dyb dxc dxd dxe dye dyf}* dxf? hvcurveto (31) |- OR |- {dxa dxb dyb dyc dyd dxe dye dxf}+ dyf? hvcurveto (31) |-
				//	appends one or more Bézier curves to the current point. The tangent for the first Bézier must be horizontal, and the second must be vertical (except as noted below). If there is a multiple of four arguments, the curve starts horizontal and ends vertical. Note that the curves alternate between start horizontal, end vertical, and start vertical, and end horizontal. The last curve (the odd argument case) need not end horizontal/vertical.
				else if (op == 31) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "hvcurveto", skipped, a, ops[o].args);
					
					//	skip superfluous arguments
					if (a == skipped) {
						while ((a < ops[o].args.length) && (((ops[o].args.length - a) % 4) > 1)) {
							a++;
							skipped++;
						}
						if (DEBUG_TYPE1C_RENDRING)
							System.out.println(" - skipped to parameter " + a);
					}
					if (ops[o].args.length < 4)
						emptyOp = true;
					
					//	hvcurveto |- dx1 dx2 dy2 dy3 {dya dxb dyb dxc dxd dxe dye dyf}* dxf? hvcurveto (31) |-
					if (((ops[o].args.length - skipped) % 8) >= 4) {
						if (a == skipped) {
							int dx1;
							int dy1;
							int dx2;
							int dy2;
							int dx3;
							int dy3;
							dx1 = ops[o].args[a++].intValue();
							dy1 = 0;
							dx2 = ops[o].args[a++].intValue();
							dy2 = ops[o].args[a++].intValue();
							if ((a+2) == ops[o].args.length) {
								dy3 = ops[o].args[a++].intValue();
								dx3 = ops[o].args[a++].intValue();
							}
							else {
								dy3 = ops[o].args[a++].intValue();
								dx3 = 0;
							}
							opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
						}
						else if ((a+8) <= ops[o].args.length) {
							int dx1;
							int dy1;
							int dx2;
							int dy2;
							int dx3;
							int dy3;
							dx1 = 0;
							dy1 = ops[o].args[a++].intValue();
							dx2 = ops[o].args[a++].intValue();
							dy2 = ops[o].args[a++].intValue();
							dx3 = ops[o].args[a++].intValue();
							dy3 = 0;
							opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
							dx1 = ops[o].args[a++].intValue();
							dy1 = 0;
							dx2 = ops[o].args[a++].intValue();
							dy2 = ops[o].args[a++].intValue();
							if ((a+2) == ops[o].args.length) {
								dy3 = ops[o].args[a++].intValue();
								dx3 = ops[o].args[a++].intValue();
							}
							else {
								dy3 = ops[o].args[a++].intValue();
								dx3 = 0;
							}
							opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
						}
						else op = -1;
					}
					
					//	hvcurveto |- {dxa dxb dyb dyc dyd dxe dye dxf}+ dyf? hvcurveto (31) |-
					else {
						if ((a+8) <= ops[o].args.length) {
							int dx1;
							int dy1;
							int dx2;
							int dy2;
							int dx3;
							int dy3;
							dx1 = ops[o].args[a++].intValue();
							dy1 = 0;
							dx2 = ops[o].args[a++].intValue();
							dy2 = ops[o].args[a++].intValue();
							dx3 = 0;
							dy3 = ops[o].args[a++].intValue();
							opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
							dx1 = 0;
							dy1 = ops[o].args[a++].intValue();
							dx2 = ops[o].args[a++].intValue();
							dy2 = ops[o].args[a++].intValue();
							dx3 = ops[o].args[a++].intValue();
							if ((a+1) == ops[o].args.length)
								dy3 = ops[o].args[a++].intValue();
							else dy3 = 0;
							opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
						}
						else op = -1;
					}
				}
				
				//	rcurveline |- {dxa dya dxb dyb dxc dyc}+ dxd dyd rcurveline (24) |-
				//	is equivalent to one rrcurveto for each set of six arguments dxa...dyc, followed by exactly one rlineto using the dxd, dyd arguments. The number of curves is determined from the count on the argument stack.
				else if (op == 24) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "rcurveline", skipped, a, ops[o].args);
					
					//	skip superfluous arguments
					if (a == skipped) {
						while ((a < ops[o].args.length) && (((ops[o].args.length - a) % 6) != 2))
							a++;
						if (DEBUG_TYPE1C_RENDRING)
							System.out.println(" - skipped to parameter " + a);
					}
					if (ops[o].args.length < 8)
						emptyOp = true;
					
					//	execute op
					while ((a+8) <= ops[o].args.length)
						opr.curveTo(ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue());
					if ((a+2) <= ops[o].args.length)
						opr.lineTo(ops[o].args[a++].intValue(), ops[o].args[a++].intValue());
					op = -1;
//					if ((a+6) <= ops[o].args.length)
//						opr.curveTo(ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue());
//					else {
//						opr.lineTo(ops[o].args[a++].intValue(), ops[o].args[a++].intValue());
//						op = -1;
//					}
				}
				
				//	rlinecurve |- {dxa dya}+ dxb dyb dxc dyc dxd dyd rlinecurve (25) |-
				//	is equivalent to one rlineto for each pair of arguments beyond the six arguments dxb...dyd needed for the one rrcurveto command. The number of lines is determined from the count of items on the argument stack.
				else if (op == 25) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "rlinecurve", skipped, a, ops[o].args);
					
					//	skip superfluous arguments
					if (a == skipped) {
						while ((a < ops[o].args.length) && (((ops[o].args.length - a) % 2) != 0))
							a++;
						if (DEBUG_TYPE1C_RENDRING)
							System.out.println(" - skipped to parameter " + a);
					}
					if (ops[o].args.length < 8)
						emptyOp = true;
					
					//	execute op
					while ((a+8) <= ops[o].args.length)
						opr.lineTo(ops[o].args[a++].intValue(), ops[o].args[a++].intValue());
					if ((a+6) <= ops[o].args.length)
						opr.curveTo(ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue());
					op = -1;
//					if ((a+8) <= ops[o].args.length)
//						opr.lineTo(ops[o].args[a++].intValue(), ops[o].args[a++].intValue());
//					else {
//						opr.curveTo(ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue(), ops[o].args[a++].intValue());
//						op = -1;
//					}
				}
				
				//	vhcurveto |- dy1 dx2 dy2 dx3 {dxa dxb dyb dyc dyd dxe dye dxf}* dyf? vhcurveto (30) |- OR |- {dya dxb dyb dxc dxd dxe dye dyf}+ dxf? vhcurveto (30) |-
				//	appends one or more Bézier curves to the current point, where the first tangent is vertical and the second tangent is horizontal. This command is the complement of hvcurveto; see the description of hvcurveto for more information.
				else if (op == 30) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "vhcurveto", skipped, a, ops[o].args);
					
					//	skip superfluous arguments
					if (a == skipped) {
						while ((a < ops[o].args.length) && (((ops[o].args.length - a) % 4) > 1)) {
							a++;
							skipped++;
						}
//						if ((((ops[o].args.length) % 4) == 1) && (ops[o].args[0].intValue() == 53)) {
//							a++;
//							skipped++;
//						}
						if (DEBUG_TYPE1C_RENDRING)
							System.out.println(" - skipped to parameter " + a);
					}
					if (ops[o].args.length < 4)
						emptyOp = true;
					
					//	vhcurveto |- dy1 dx2 dy2 dx3 {dxa dxb dyb dyc dyd dxe dye dxf}* dyf? vhcurveto (30) |-
					if (((ops[o].args.length - skipped) % 8) >= 4) {
						if (a == skipped) {
							int dx1;
							int dy1;
							int dx2;
							int dy2;
							int dx3;
							int dy3;
							dx1 = 0;
							dy1 = ops[o].args[a++].intValue();
							dx2 = ops[o].args[a++].intValue();
							dy2 = ops[o].args[a++].intValue();
							dx3 = ops[o].args[a++].intValue();
							if ((a+1) == ops[o].args.length)
								dy3 = ops[o].args[a++].intValue();
							else dy3 = 0;
							opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
						}
						else if ((a+8) <= ops[o].args.length) {
							int dx1;
							int dy1;
							int dx2;
							int dy2;
							int dx3;
							int dy3;
							dx1 = ops[o].args[a++].intValue();
							dy1 = 0;
							dx2 = ops[o].args[a++].intValue();
							dy2 = ops[o].args[a++].intValue();
							dx3 = 0;
							dy3 = ops[o].args[a++].intValue();
							opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
							dx1 = 0;
							dy1 = ops[o].args[a++].intValue();
							dx2 = ops[o].args[a++].intValue();
							dy2 = ops[o].args[a++].intValue();
							dx3 = ops[o].args[a++].intValue();
							if ((a+1) == ops[o].args.length)
								dy3 = ops[o].args[a++].intValue();
							else dy3 = 0;
							opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
						}
						else op = -1;
					}
					
					//	vhcurveto |- {dya dxb dyb dxc dxd dxe dye dyf}+ dxf? vhcurveto (30) |-
					else {
						if ((a+8) <= ops[o].args.length) {
							int dx1;
							int dy1;
							int dx2;
							int dy2;
							int dx3;
							int dy3;
							dx1 = 0;
							dy1 = ops[o].args[a++].intValue();
							dx2 = ops[o].args[a++].intValue();
							dy2 = ops[o].args[a++].intValue();
							dx3 = ops[o].args[a++].intValue();
							dy3 = 0;
							opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
							dx1 = ops[o].args[a++].intValue();
							dy1 = 0;
							dx2 = ops[o].args[a++].intValue();
							dy2 = ops[o].args[a++].intValue();
							if ((a+2) == ops[o].args.length) {
								dy3 = ops[o].args[a++].intValue();
								dx3 = ops[o].args[a++].intValue();
							}
							else {
								dy3 = ops[o].args[a++].intValue();
								dx3 = 0;
							}
							opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
						}
						else op = -1;
					}
				}
				
				//	vvcurveto |- dx1? {dya dxb dyb dyc}+ vvcurveto (26) |-
				//	appends one or more curves to the current point. If the argument count is a multiple of four, the curve starts and ends vertical. If the argument count is odd, the first curve does not begin with a vertical tangent.
				else if (op == 26) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "vvcurveto", skipped, a, ops[o].args);
					
					//	skip superfluous arguments
					if (a == skipped) {
						while ((a < ops[o].args.length) && (((ops[o].args.length - a) % 4) > 1)) {
							a++;
							skipped++;
						}
						if (DEBUG_TYPE1C_RENDRING)
							System.out.println(" - skipped to parameter " + a);
					}
					if (ops[o].args.length < 4)
						emptyOp = true;
					
					//	execute op
					if ((a+4) <= ops[o].args.length) {
						int dx1;
						int dy1;
						int dx2;
						int dy2;
						int dx3;
						int dy3;
//						if ((a == 0) && ((ops[o].args.length % 4) == 1))
//							dx1 = ops[o].args[a++].intValue();
						if ((a == skipped) && (((ops[o].args.length - skipped) % 4) == 1))
							dx1 = ops[o].args[a++].intValue();
						else dx1 = 0;
						dy1 = ops[o].args[a++].intValue();
						dx2 = ops[o].args[a++].intValue();
						dy2 = ops[o].args[a++].intValue();
						dx3 = 0;
						dy3 = ops[o].args[a++].intValue();
						opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
					}
					else op = -1;
				}
				
				//flex |- dx1 dy1 dx2 dy2 dx3 dy3 dx4 dy4 dx5 dy5 dx6 dy6 fd flex (12 35) |-
				//causes two Bézier curves, as described by the arguments (as shown in Figure 2 below), to be rendered as a straight line when the flex depth is less than fd /100 device pixels, and as curved lines when the flex depth is greater than or equal to fd/100 device pixels. The flex depth for a horizontal curve, as shown in Figure 2, is the distance from the join point to the line connecting the start and end points on the curve. If the curve is not exactly horizontal or vertical, it must be determined whether the curve is more horizontal or vertical by the method described in the flex1 description, below, and as illustrated in Figure 3.
				//Note 5 In cases where some of the points have the same x or y coordinate as other points in the curves, arguments may be omitted by using one of the following forms of the flex operator, hflex, hflex1, or flex1.
				else if (op == 1035) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "flex", skipped, a, ops[o].args);
					
					if (ops[o].args.length < 13)
						emptyOp = true;
					
					//	execute op
					if ((a+12) <= ops[o].args.length) {
						int dx1;
						int dy1;
						int dx2;
						int dy2;
						int dx3;
						int dy3;
						dx1 = ops[o].args[a++].intValue();
						dy1 = ops[o].args[a++].intValue();
						dx2 = ops[o].args[a++].intValue();
						dy2 = ops[o].args[a++].intValue();
						dx3 = ops[o].args[a++].intValue();
						dy3 = ops[o].args[a++].intValue();
						opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
						dx1 = ops[o].args[a++].intValue();
						dy1 = ops[o].args[a++].intValue();
						dx2 = ops[o].args[a++].intValue();
						dy2 = ops[o].args[a++].intValue();
						dx3 = ops[o].args[a++].intValue();
						dy3 = ops[o].args[a++].intValue();
						opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
					}
					else op = -1;
				}
				
				//hflex |- dx1 dx2 dy2 dx3 dx4 dx5 dx6 hflex (12 34) |-
				//causes the two curves described by the arguments dx1...dx6 to be rendered as a straight line when the flex depth is less than 0.5 (that is, fd is 50) device pixels, and as curved lines when the flex depth is greater than or equal to 0.5 device pixels. hflex is used when the following are all true:
				//a) the starting and ending points, first and last control points have the same y value.
				//b) the joining point and the neighbor control points have the same y value.
				//c) the flex depth is 50.
				else if (op == 1034) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "hflex", skipped, a, ops[o].args);
					
					if (ops[o].args.length < 7)
						emptyOp = true;
					
					//	execute op
					if ((a+7) <= ops[o].args.length) {
						int dx1;
						int dy1;
						int dx2;
						int dy2;
						int dx3;
						int dy3;
						dx1 = ops[o].args[a++].intValue();
						dy1 = 0;
						dx2 = ops[o].args[a++].intValue();
						dy2 = ops[o].args[a++].intValue();
						dx3 = ops[o].args[a++].intValue();
						dy3 = 0;
						opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
						dx1 = ops[o].args[a++].intValue();
						dy1 = 0;
						dx2 = ops[o].args[a++].intValue();
						dy2 = 0;
						dx3 = ops[o].args[a++].intValue();
						dy3 = 0;
						opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
					}
					else op = -1;
				}
				
				//hflex1 |- dx1 dy1 dx2 dy2 dx3 dx4 dx5 dy5 dx6 hflex1 (12 36) |-
				//causes the two curves described by the arguments to be rendered as a straight line when the flex depth is less than 0.5 device pixels, and as curved lines when the flex depth is greater than or equal to 0.5 device pixels. hflex1 is used if the conditions for hflex are not met but all of the following are true:
				//a) the starting and ending points have the same y value,
				//b) the joining point and the neighbor control points have the same y value.
				else if (op == 1036) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "hflex1", skipped, a, ops[o].args);
					
					if (ops[o].args.length < 9)
						emptyOp = true;
					
					//	execute op
					if ((a+9) <= ops[o].args.length) {
						int dx1;
						int dy1;
						int dx2;
						int dy2;
						int dx3;
						int dy3;
						dx1 = ops[o].args[a++].intValue();
						dy1 = ops[o].args[a++].intValue();
						dx2 = ops[o].args[a++].intValue();
						dy2 = ops[o].args[a++].intValue();
						dx3 = ops[o].args[a++].intValue();
						dy3 = 0;
						opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
						dx1 = ops[o].args[a++].intValue();
						dy1 = 0;
						dx2 = ops[o].args[a++].intValue();
						dy2 = ops[o].args[a++].intValue();
						dx3 = ops[o].args[a++].intValue();
						dy3 = 0;
						opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
					}
					else op = -1;
				}
				
				//flex1 |- dx1 dy1 dx2 dy2 dx3 dy3 dx4 dy4 dx5 dy5 d6 flex1 (12 37) |-
				//causes the two curves described by the arguments to be rendered as a straight line when the flex depth is less than 0.5 device pixels, and as curved lines when the flex depth is greater than or equal to 0.5 device pixels.
				//The d6 argument will be either a dx or dy value, depending on the curve (see Figure 3). To determine the correct value, compute the distance from the starting point (x, y), the first point of the first curve, to the last flex control point (dx5, dy5) by summing all the arguments except d6; call this (dx, dy). If abs(dx) > abs(dy), then the last point’s x-value is given by d6, and its y-value is equal to y. Otherwise, the last point’s x-value is equal to x and its y-value is given by d6.
				//flex1 is used if the conditions for hflex and hflex1 are notmet but all of the following are true:
				//a) the starting and ending points have the same x or y value,
				//b) the flex depth is 50.
				else if (op == 1037) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "flex1", skipped, a, ops[o].args);
					
					if (ops[o].args.length < 11)
						emptyOp = true;
					
					//	execute op
					if ((a+11) <= ops[o].args.length) {
						int sx = opr.x;
						int sy = opr.y;
						int dx1;
						int dy1;
						int dx2;
						int dy2;
						int dx3;
						int dy3;
						dx1 = ops[o].args[a++].intValue();
						dy1 = ops[o].args[a++].intValue();
						dx2 = ops[o].args[a++].intValue();
						dy2 = ops[o].args[a++].intValue();
						dx3 = ops[o].args[a++].intValue();
						dy3 = ops[o].args[a++].intValue();
						opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
						dx1 = ops[o].args[a++].intValue();
						dy1 = ops[o].args[a++].intValue();
						dx2 = ops[o].args[a++].intValue();
						dy2 = ops[o].args[a++].intValue();
						int dx = (opr.x + dx1 + dx2 - sx);
						int dy = (opr.y + dy1 + dy2 - sy);
						if (Math.abs(dx) > Math.abs(dy)) {
							dx3 = ops[o].args[a++].intValue();
							dy3 = 0;
						}
						else {
							dx3 = 0;
							dy3 = ops[o].args[a++].intValue();
						}
						opr.curveTo(dx1, dy1, dx2, dy2, dx3, dy3);
					}
					else op = -1;
				}
				
				//	endchar – endchar (14) |–
				//	finishes a charstring outline definition, and must be the last operator in a character’s outline.
				else if (op == 14) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "endchar", skipped, a, ops[o].args);
//					if ((o == 0) && (ops[o].args.length > 0))
//						opr.rWidth = ops[o].args[a++].intValue();
					if ((o == 0) && (ops[o].args.length > 0))
						opr.rWidth += ops[o].args[a++].intValue(); 
					opr.closePath();
					op = -1;
				}
				
				//	hintmask
				else if (op == 19) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "hintmask", skipped, a, ops[o].args);
					if ((o == 0) && (ops[o].args.length > 1))
						opr.rWidth += ops[o].args[a++].intValue();
//					skipFirst = true;
					op = -1;
				}
				
				//	cntrmask
				else if (op == 20) {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, "cntrmask", skipped, a, ops[o].args);
					if ((o == 0) && (ops[o].args.length > 1))
						opr.rWidth += ops[o].args[a++].intValue();
//					skipFirst = true;
					op = -1;
				}
				
				//Note 6 The charstring itself may end with a call(g)subr; the subroutine must then end with an endchar operator.
				//Note 7 A character that does not have a path (e.g. a space character) may consist of an endchar operator preceded only by a width value. Although the width must be specified in the font, it may be specified as the defaultWidthX in the CFF data, in which case it should not be specified in the charstring. Also, it may appear in the charstring as the difference from nominalWidthX. Thus the smallest legal charstring consists of a single endchar operator.
				//Note 8 endchar also has a deprecated function; see Appendix C, “Comaptibility and Deprecated Operators.”
				else {
					if (DEBUG_TYPE1C_RENDRING)
						printOp(op, null, skipped, a, ops[o].args);
					op = -1;
				}
			}
			
			if (DEBUG_TYPE1C_RENDRING && (opr instanceof OpGraphics))
				System.out.println(" ==> dot at (" + ((OpGraphics) opr).x + "/" + ((OpGraphics) opr).y + ")");
			
			if (DEBUG_TYPE1C_RENDRING && (show || emptyOp) && (opr instanceof OpGraphics)) {
				BufferedImage dImg = new BufferedImage(((OpGraphics) opr).img.getWidth(), ((OpGraphics) opr).img.getHeight(), BufferedImage.TYPE_INT_RGB);
				Graphics g = dImg.getGraphics();
				g.drawImage(((OpGraphics) opr).img, 0, 0, dImg.getWidth(), dImg.getHeight(), null);
				g.setColor(Color.RED);
				g.fillRect(((OpGraphics) opr).x-2, ((OpGraphics) opr).y-2, 5, 5);
				if (idd == null) {
					idd = new ImageDisplayDialog("Rendering Progress");
					idd.setSize((dImg.getWidth() + 200), (dImg.getHeight() + 100));
				}
				idd.addImage(dImg, ("After " + ops[o].op));
				idd.setLocationRelativeTo(null);
				idd.setVisible(true);
			}
		}
		
		//	only tracking, we're done here
		if (opr instanceof OpTracker)
			return;
		
		//	fill outline
		int blackRgb = Color.BLACK.getRGB();
		int whiteRgb = Color.WHITE.getRGB();
		int outsideRgb = Color.GRAY.getRGB();
		int insideRgb = Color.GRAY.darker().getRGB();
		HashSet insideRgbs = new HashSet();
		HashSet outsideRgbs = new HashSet();
		
		//	fill outside
		fill(((OpGraphics) opr).img, 1, 1, whiteRgb, outsideRgb);
		insideRgbs.add(new Integer(insideRgb));
		insideRgbs.add(new Integer(blackRgb));
		outsideRgbs.add(new Integer(outsideRgb));
		
		//	fill multi-path characters outside-in
		if (isMultiPath) {
			outsideRgb = (new Color(outsideRgb)).brighter().getRGB();
			int seekWidth = Math.max(1, (Math.round(((OpGraphics) opr).scale * 10)));
			boolean gotWhite = true;
			boolean outmostWhiteIsInside = true;
			while (gotWhite) {
				
				if (DEBUG_TYPE1C_RENDRING && show && (opr instanceof OpGraphics)) {
					BufferedImage dImg = new BufferedImage(((OpGraphics) opr).img.getWidth(), ((OpGraphics) opr).img.getHeight(), BufferedImage.TYPE_INT_RGB);
					Graphics g = dImg.getGraphics();
					g.drawImage(((OpGraphics) opr).img, 0, 0, dImg.getWidth(), dImg.getHeight(), null);
					g.setColor(Color.RED);
					g.fillRect(((OpGraphics) opr).x-2, ((OpGraphics) opr).y-2, 5, 5);
					if (idd == null) {
						idd = new ImageDisplayDialog("Rendering Progress");
						idd.setSize((dImg.getWidth() + 200), (dImg.getHeight() + 100));
					}
					idd.addImage(dImg, ("Filling"));
					idd.setLocationRelativeTo(null);
					idd.setVisible(true);
				}
				
				gotWhite = false;
				int fillRgb;
				if (outmostWhiteIsInside) {
					fillRgb = insideRgb;
					insideRgbs.add(new Integer(fillRgb));
				}
				else {
					fillRgb = outsideRgb;
					outsideRgbs.add(new Integer(fillRgb));
				}
//				System.out.println("Fill RGB is " + fillRgb);
				for (int c = seekWidth; c < ((OpGraphics) opr).img.getWidth(); c += seekWidth) {
//					System.out.println("Investigating column " + c);
					int r = 0;
					while ((r < ((OpGraphics) opr).img.getHeight()) && (((OpGraphics) opr).img.getRGB(c, r) != whiteRgb) && (((OpGraphics) opr).img.getRGB(c, r) != fillRgb)) {
//						if ((r % 10) == 0) System.out.println(" - ignoring pixel with RGB " + img.getRGB(c, r));
						r++;
					}
//					System.out.println(" - found interesting pixel at row " + r);
					if (r >= ((OpGraphics) opr).img.getHeight()) {
//						System.out.println(" --> bottom of column");
						continue;
					}
//					System.out.println(" - RGB is " + img.getRGB(c, r));
					if (((OpGraphics) opr).img.getRGB(c, r) == fillRgb) {
//						System.out.println(" --> filled before");
						continue;
					}
					
//					System.out.println(" --> filling at " + c + "/" + r + " with " + fillRgb);
					fill(((OpGraphics) opr).img, c, r, whiteRgb, fillRgb);
					gotWhite = true;
				}
				
				for (int c = (((OpGraphics) opr).img.getWidth() - seekWidth); c > 0; c -= seekWidth) {
//					System.out.println("Investigating column " + c);
					int r = (((OpGraphics) opr).img.getHeight() - 1);
					while ((r >= 0) && (((OpGraphics) opr).img.getRGB(c, r) != whiteRgb) && (((OpGraphics) opr).img.getRGB(c, r) != fillRgb)) {
//						if ((r % 10) == 0) System.out.println(" - ignoring pixel with RGB " + img.getRGB(c, r));
						r--;
					}
//					System.out.println(" - found interesting pixel at row " + r);
					if (r < 0) {
//						System.out.println(" --> bottom of column");
						continue;
					}
//					System.out.println(" - RGB is " + img.getRGB(c, r));
					if (((OpGraphics) opr).img.getRGB(c, r) == fillRgb) {
//						System.out.println(" --> filled before");
						continue;
					}
					
//					System.out.println(" --> filling at " + c + "/" + r + " with " + fillRgb);
					fill(((OpGraphics) opr).img, c, r, whiteRgb, fillRgb);
					gotWhite = true;
				}
				
				for (int r = seekWidth; r < ((OpGraphics) opr).img.getHeight(); r += seekWidth) {
//					System.out.println("Investigating row " + r);
					int c = 0;
					while ((c < ((OpGraphics) opr).img.getWidth()) && (((OpGraphics) opr).img.getRGB(c, r) != whiteRgb) && (((OpGraphics) opr).img.getRGB(c, r) != fillRgb)) {
//						if ((c % 10) == 0) System.out.println(" - ignoring pixel with RGB " + img.getRGB(c, r));
						c++;
					}
//					System.out.println(" - found interesting pixel at column " + r);
					if (c >= ((OpGraphics) opr).img.getWidth()) {
//						System.out.println(" --> right end of row");
						continue;
					}
//					System.out.println(" - RGB is " + img.getRGB(c, r));
					if (((OpGraphics) opr).img.getRGB(c, r) == fillRgb) {
//						System.out.println(" --> filled before");
						continue;
					}
					
//					System.out.println(" --> filling at " + c + "/" + r + " with " + fillRgb);
					fill(((OpGraphics) opr).img, c, r, whiteRgb, fillRgb);
					gotWhite = true;
				}
				
				for (int r = (((OpGraphics) opr).img.getHeight() - seekWidth); r >= 0; r -= seekWidth) {
//					System.out.println("Investigating row " + r);
					int c = (((OpGraphics) opr).img.getWidth() - 1);
					while ((c >= 0) && (((OpGraphics) opr).img.getRGB(c, r) != whiteRgb) && (((OpGraphics) opr).img.getRGB(c, r) != fillRgb)) {
//						if ((c % 10) == 0) System.out.println(" - ignoring pixel with RGB " + img.getRGB(c, r));
						c--;
					}
//					System.out.println(" - found interesting pixel at column " + r);
					if (c < 0) {
//						System.out.println(" --> right end of row");
						continue;
					}
//					System.out.println(" - RGB is " + img.getRGB(c, r));
					if (((OpGraphics) opr).img.getRGB(c, r) == fillRgb) {
//						System.out.println(" --> filled before");
						continue;
					}
					
//					System.out.println(" --> filling at " + c + "/" + r + " with " + fillRgb);
					fill(((OpGraphics) opr).img, c, r, whiteRgb, fillRgb);
					gotWhite = true;
				}
				
				if (outmostWhiteIsInside) {
					outmostWhiteIsInside = false;
					insideRgb = (new Color(insideRgb)).darker().getRGB();
//					System.out.println("Inside RGB set to " + insideRgb);
				}
				else {
					outmostWhiteIsInside = true;
					outsideRgb = (new Color(outsideRgb)).brighter().getRGB();
//					System.out.println("Outside RGB set to " + outsideRgb);
				}
			}
		}
		
		//	fill single-path character
		else insideRgbs.add(new Integer(whiteRgb));
		
		
		//	make it black and white, finally
		for (int c = 0; c < ((OpGraphics) opr).img.getWidth(); c++) {
			for (int r = 0; r < ((OpGraphics) opr).img.getHeight(); r++) {
				int rgb = ((OpGraphics) opr).img.getRGB(c, r);
				if (insideRgbs.contains(new Integer(rgb)))
					((OpGraphics) opr).img.setRGB(c, r, blackRgb);
				else ((OpGraphics) opr).img.setRGB(c, r, whiteRgb);
			}
		}
		
		//	scale down image
		int maxSize = 100;
		if (((OpGraphics) opr).img.getHeight() > maxSize) {
			BufferedImage sImg = new BufferedImage(((maxSize * ((OpGraphics) opr).img.getWidth()) / ((OpGraphics) opr).img.getHeight()), maxSize, BufferedImage.TYPE_BYTE_GRAY);
			sImg.getGraphics().drawImage(((OpGraphics) opr).img, 0, 0, sImg.getWidth(), sImg.getHeight(), null);
//			System.out.println("Scaled-down image is " + sImg.getWidth() + " x " + sImg.getHeight() + " (" + (((float) sImg.getWidth()) / sImg.getHeight()) + ")");
			((OpGraphics) opr).setImage(sImg);
		}
		
		//	display result for rendering tests
		if (DEBUG_TYPE1C_RENDRING && (show || emptyOp)) {
			if (idd != null) {
				idd.addImage(((OpGraphics) opr).img, "Result");
				idd.setLocationRelativeTo(null);
				idd.setVisible(true);
			}
			if (fidd != null) {
				fidd.addImage(((OpGraphics) opr).img, ("" + sid));
				if (idd == null) {
					fidd.setLocationRelativeTo(null);
					fidd.setVisible(true);
				}
			}
		}
	}
	
	private static final void printOp(int op, String opName, int skipped, int a, Number[] args) {
		System.out.print("Executing " + ((opName == null) ? ("" + op + ":") : (opName + " (" + op + "):")));
		for (int i = 0; i < Math.min(skipped, args.length); i++)
			System.out.print(" [" + args[i].intValue() + "]");
		for (int i = skipped; i < Math.min(a, args.length); i++)
			System.out.print(" (" + args[i].intValue() + ")");
		for (int i = a; i < args.length; i++)
			System.out.print(" " + args[i].intValue());
		System.out.println();
	}
	
	private static void fill(BufferedImage img, int x, int y, int toFillRgb, int fillRgb) {
		if ((x < 0) || (x >= img.getWidth()))
			return;
		if ((y < 0) || (y >= img.getHeight()))
			return;
		int rgb = img.getRGB(x, y);
		if (rgb != toFillRgb)
			return;
		img.setRGB(x, y, fillRgb);
//		System.out.println("Filling from " + x + "/" + y + ", boundary is " + boundaryRgb + ", fill is " + fillRgb + ", set is " + img.getRGB(x, y));
		int xlm = x;
		for (int xl = (x-1); xl >= 0; xl--) {
			rgb = img.getRGB(xl, y);
			if (rgb != toFillRgb)
				break;
			img.setRGB(xl, y, fillRgb);
			xlm = xl;
		}
		int xrm = x;
		for (int xr = (x+1); xr < img.getWidth(); xr++) {
			rgb = img.getRGB(xr, y);
			if (rgb != toFillRgb)
				break;
			img.setRGB(xr, y, fillRgb);
			xrm = xr;
		}
		for (int xe = xlm; xe <= xrm; xe++) {
			fill(img, xe, (y - 1), toFillRgb, fillRgb);
			fill(img, xe, (y + 1), toFillRgb, fillRgb);
		}
	}
	
	private static int readFontType1cEncoding(byte[] data, int start, HashMap eDict) {
		if (DEBUG_TYPE1C_LOADING) System.out.println("Reading encoding:");
		int i = start;
		int fmt = convertUnsigned(data[i++]);
		if (DEBUG_TYPE1C_LOADING) System.out.println(" - format is " + fmt);
		if (fmt == 0) {
			int nCodes = convertUnsigned(data[i++]);
			if (DEBUG_TYPE1C_LOADING) System.out.println(" - expecting " + nCodes + " codes");
			for (int c = 1; c <= nCodes; c++) {
				int code = convertUnsigned(data[i++]);
				if (DEBUG_TYPE1C_LOADING) System.out.println(" - 0 got char " + c + ": " + code);
				eDict.put(new Integer(c), new Integer(code));
			}
		}
		else if (fmt == 1) {
			int nRanges = convertUnsigned(data[i++]);
			if (DEBUG_TYPE1C_LOADING) System.out.println(" - expecting " + nRanges + " ranges");
			for (int r = 0; r < nRanges; r++) {
				int rStart = convertUnsigned(data[i++]);
				int rSize = convertUnsigned(data[i++]);
				for (int ro = 0; ro < rSize; ro++) {
					int code = (rStart + ro);
					if (DEBUG_TYPE1C_LOADING) System.out.println(" - 1 got" + ((ro == 0) ? "" : " next") + " char " + (eDict.size()+1) + ": " + code);
					eDict.put(new Integer(eDict.size()+1), new Integer(code));
				}
			}
		}
		return i;
	}
	
	private static int readFontType1cCharset(byte[] data, int start, int charCount, ArrayList content) {
		if (DEBUG_TYPE1C_LOADING) System.out.println("Reading char set:");
		int i = start;
		int fmt = convertUnsigned(data[i++]);
		if (DEBUG_TYPE1C_LOADING) System.out.println(" - format is " + fmt);
		if (fmt == 0) {
			if (content != null)
				content.add(new Integer(0));
			for (int c = 1; c < charCount; c++) {
				int sid = ((convertUnsigned(data[i++]) * 256) + convertUnsigned(data[i++]));
				if (content != null)
					content.add(new Integer(sid));
				if (DEBUG_TYPE1C_LOADING) System.out.println(" - 0 got char " + c + ": " + sid);
			}
		}
		else if (fmt == 1) {
			int toCome = charCount;
			while (toCome > 0) {
				int sid = ((convertUnsigned(data[i++]) * 256) + convertUnsigned(data[i++]));
				toCome--;
				if (content != null)
					content.add(new Integer(sid));
				if (DEBUG_TYPE1C_LOADING) System.out.println(" - 1 got char " + content.size() + ": " + sid);
				int toComeInRange = convertUnsigned(data[i++]);
				if (DEBUG_TYPE1C_LOADING) System.out.println("   - " + toComeInRange + " more in range, " + toCome + " in total");
				for (int c = 0; c < toComeInRange; c++) {
					sid++;
					toCome--;
					if (content != null)
						content.add(new Integer(sid));
					if (DEBUG_TYPE1C_LOADING) System.out.println(" - 1 got next char " + content.size() + ": " + sid);
				}
			}
		}
		else if (fmt == 2) {
			int toCome = charCount;
			while (toCome > 0) {
				int sid = ((convertUnsigned(data[i++]) * 256) + convertUnsigned(data[i++]));
				toCome--;
				if (content != null)
					content.add(new Integer(sid));
				if (DEBUG_TYPE1C_LOADING) System.out.println(" - 2 got char " + content.size() + ": " + sid);
				int toComeInRange = ((convertUnsigned(data[i++]) * 256) + convertUnsigned(data[i++]));
				for (int c = 0; c < toComeInRange; c++) {
					sid++;
					toCome--;
					if (content != null)
						content.add(new Integer(sid));
					if (DEBUG_TYPE1C_LOADING) System.out.println(" - 2 got char " + content.size() + ": " + sid);
				}
			}
		}
		return i;
	}
	
	private static int readFontType1cIndex(byte[] data, int start, String name, HashMap dictEntries, HashMap dictOpResolver, boolean isTopDict, ArrayList content) {
		int i = start;
		System.out.println("Doing " + name + " index:");
		int count = (256 * convertUnsigned(data[i++]) + convertUnsigned(data[i++]));
		System.out.println(" - count is " + count);
		if (dictEntries != null) {
			Number[] cnt = {new Integer(count)};
			dictEntries.put("Count", cnt);
		}
		if (count == 0)
			return i;
		int offSize = convertUnsigned(data[i++]);
		System.out.println(" - offset size is " + offSize);
		int[] offsets = new int[count+1];
		for (int c = 0; c <= count; c++) {
			offsets[c] = 0;
			for (int b = 0; b < offSize; b++)
				offsets[c] = ((offsets[c] * 256) + convertUnsigned(data[i++]));
			System.out.println(" - offset[" + c + "] is " + offsets[c]);
		}
		for (int c = 0; c < count; c++) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for (int b = offsets[c]; b < offsets[c+1]; b++)
				baos.write(convertUnsigned(data[i++]));
			if ((dictOpResolver != null) && (dictEntries != null)) {
				ArrayList dictContent = ((content == null) ? null : new ArrayList());
				readFontType1cDict(baos.toByteArray(), 0, (name + "-entry[" + c + "]"), dictEntries, isTopDict, dictOpResolver, dictContent);
				if (content != null)
					content.add((Op[]) dictContent.toArray(new Op[dictContent.size()]));
			}
			else if (content != null)
				content.add(new String(baos.toByteArray()));
			else System.out.println(" - entry[" + c + "]: " + new String(baos.toByteArray()));
		}
		return i;
	}
	
	private static int readFontType1cDict(byte[] data, int start, String name, HashMap dictEntries, boolean isTopDict, HashMap opResolver, ArrayList content) {
//		System.out.println("Doing " + name + " dict:");
		int i = start;
		LinkedList stack = new LinkedList();
		int hintCount = 0;
		while (i < data.length)  {
			
			//	read value
			int bs = convertUnsigned(data[i++]);
//			System.out.println(" - first byte is " + bs + " (" + data[i-1] + ")");
			int op = Integer.MIN_VALUE;
			int iVal = Integer.MIN_VALUE;
			double dVal = Double.NEGATIVE_INFINITY;
			
			if ((0 <= bs) && (bs <= 11)) {
				op = bs;
			}
			else if (bs == 12) {
				op = (1000 + convertUnsigned(data[i++]));
			}
			else if ((13 <= bs) && (bs <= 18)) {
				op = bs;
			}
			else if (bs == 19) {
				op = bs;
			}
			else if (bs == 20) {
				op = bs;
			}
			else if ((21 <= bs) && (bs <= 27)) {
				op = bs;
			}
			else if (bs == 28) {
				iVal = 0;
				for (int b = 0; b < 2; b++) {
					iVal <<= 8;
					iVal += convertUnsigned(data[i++]);
				}
				if (iVal > 32768)
					iVal -= 65536;
			}
			else if (bs == 29) {
				iVal = 0;
				for (int b = 0; b < 4; b++) {
					iVal <<= 8;
					iVal += convertUnsigned(data[i++]);
				}
			}
			else if (isTopDict && (bs == 30)) {
				StringBuffer val = new StringBuffer();
				while (true) {
					int b = convertUnsigned(data[i++]);
					int hq = (b & 240) >>> 4;
					int lq = (b & 15);
					
					if (hq < 10)
						val.append("" + hq);
					else if (hq == 10)
						val.append(".");
					else if (hq == 11)
						val.append("E");
					else if (hq == 12)
						val.append("E-");
					else if (hq == 13)
						val.append("");
					else if (hq == 14)
						val.append("-");
					else if (hq == 15)
						break;
					
					if (lq < 10)
						val.append("" + lq);
					else if (lq == 10)
						val.append(".");
					else if (lq == 11)
						val.append("E");
					else if (lq == 12)
						val.append("E-");
					else if (lq == 13)
						val.append("");
					else if (lq == 14)
						val.append("-");
					else if (lq == 15)
						break;
				}
				dVal = Double.parseDouble(val.toString());
			}
			else if (bs == 30) {
				op = bs;
			}
			else if (bs == 31) {
				op = bs;
			}
			else if ((32 <= bs) && (bs <= 246)) {
				iVal = (bs - 139);
			}
			else if ((247 <= bs) && (bs <= 250)) {
				int b1 = bs;
				int b2 = convertUnsigned(data[i++]);
				iVal = ((b1 - 247) * 256) + b2 + 108;
			}
			else if ((251 <= bs) && (bs <= 254)) {
				int b1 = bs;
				int b2 = convertUnsigned(data[i++]);
				iVal = -((b1 - 251) * 256) - b2 - 108;
			}
			else if (bs == 255) {
				int ib1 = convertUnsigned(data[i++]);
				int ib2 = convertUnsigned(data[i++]);
				int iv = ((ib1 * 256) + ib2);
				int fb1 = convertUnsigned(data[i++]);
				int fb2 = convertUnsigned(data[i++]);
				int fv = ((fb1 * 256) + fb2);
				dVal = ((iv << 16) + fv);
				dVal /= 65536;
			}
			
			if (op != Integer.MIN_VALUE) {
				
				//	catch hint and mask operators, as the latter take a few _subsequent_ bytes depending on the number of arguments existing for the former
				if ((content != null) && (opResolver == glyphProgOpResolver)) {
					
					//	hintmask & cntrmask
					if ((op == 19) || (op == 20)) {
						
						//	if last op is hstemhm and we have something on the stack, it's an implicit vstemhm
						if ((content.size() != 0) && ((((Op) content.get(content.size()-1)).op == 1) || (((Op) content.get(content.size()-1)).op == 18))) {
							content.add(new Op(23, null, ((Number[]) stack.toArray(new Number[stack.size()]))));
//							System.out.println(" --> got " + (stack.size() / 2) + " new hints from " + stack.size() + " args, op is " + op);
							hintCount += (stack.size() / 2);
							stack.clear();
						}
						
						//	read mask bytes
						int h = 0;
						int hintmask = 0;
						while (h < ((hintCount + 7) / 8)) {
							hintmask <<= 8;
							hintmask += convertUnsigned(data[i++]);
							h++;
//							break;
						}
						stack.addLast(new Integer(hintmask));
//						System.out.println("Skipped " + h + " hint mask bytes: " + hintmask + " (" + hintCount + " hints)");
					}
					
					//	hstem, vstem, hstemhm & vstemhm (hints are number pairs !!)
					else if ((op == 1) || (op == 3) || (op == 18) || (op == 23)) {
//						System.out.println(" --> got " + (stack.size() / 2) + " new hints from " + stack.size() + " args, op is " + op);
						hintCount += (stack.size() / 2);
					}
				}
				
				String opStr = ((String) opResolver.get(new Integer(op)));
//				System.out.print(" --> read operator " + op + " (" + opStr + ")");
				if (opStr != null)
					dictEntries.put(opStr, ((Number[]) stack.toArray(new Number[stack.size()])));
				if (content != null)
					content.add(new Op(op, opStr, ((Number[]) stack.toArray(new Number[stack.size()]))));
//				while (stack.size() != 0)
//					System.out.print(" " + ((Number) stack.removeFirst()));
				stack.clear();
//				System.out.println();
			}
			else if (iVal != Integer.MIN_VALUE) {
//				System.out.println(" --> read int " + iVal);
				stack.addLast(new Integer(iVal));
			}
			else if (dVal != Double.NEGATIVE_INFINITY) {
//				System.out.println(" --> read double " + dVal);
				stack.addLast(new Double(dVal));
			}
		}
		
		return i;
	}
	
	private static class Op {
		int op;
//		String name;
		Number[] args;
		Op(int op, String name, Number[] args) {
			this.op = op;
//			this.name = name;
			this.args = args;
		}
	}
	
	private static HashMap type1cTopDictOpResolver = new HashMap();
	static {
		type1cTopDictOpResolver.put(new Integer(0), "Version");
		type1cTopDictOpResolver.put(new Integer(1), "Notice");
		type1cTopDictOpResolver.put(new Integer(2), "FullName");
		type1cTopDictOpResolver.put(new Integer(3), "FamilyName");
		type1cTopDictOpResolver.put(new Integer(4), "Weight");
		type1cTopDictOpResolver.put(new Integer(5), "FontBBox");
		type1cTopDictOpResolver.put(new Integer(13), "UniqueID");
		type1cTopDictOpResolver.put(new Integer(14), "XUID");
		type1cTopDictOpResolver.put(new Integer(15), "Charset");
		type1cTopDictOpResolver.put(new Integer(16), "Encoding");
		type1cTopDictOpResolver.put(new Integer(17), "CharStrings");
		type1cTopDictOpResolver.put(new Integer(18), "Private");
		
		type1cTopDictOpResolver.put(new Integer(1000), "Copyright");
		type1cTopDictOpResolver.put(new Integer(1001), "IsFixedPitch");
		type1cTopDictOpResolver.put(new Integer(1002), "ItalicAngle");
		type1cTopDictOpResolver.put(new Integer(1003), "UnderlinePosition");
		type1cTopDictOpResolver.put(new Integer(1004), "UnderlineThickness");
		type1cTopDictOpResolver.put(new Integer(1005), "PaintType");
		type1cTopDictOpResolver.put(new Integer(1006), "CharstringType");
		type1cTopDictOpResolver.put(new Integer(1007), "FontMatrix");
		type1cTopDictOpResolver.put(new Integer(1008), "StrokeWidth");
		type1cTopDictOpResolver.put(new Integer(1020), "SyntheticBase");
		type1cTopDictOpResolver.put(new Integer(1021), "PostScript");
		type1cTopDictOpResolver.put(new Integer(1022), "BaseFontName");
		type1cTopDictOpResolver.put(new Integer(1023), "BaseFondBlend");
		
		type1cTopDictOpResolver.put(new Integer(1030), "ROS");
		type1cTopDictOpResolver.put(new Integer(1031), "CIDFontVersion");
		type1cTopDictOpResolver.put(new Integer(1032), "CIDFontRevision");
		type1cTopDictOpResolver.put(new Integer(1033), "CIDFontType");
		type1cTopDictOpResolver.put(new Integer(1034), "CIDCount");
		type1cTopDictOpResolver.put(new Integer(1035), "UIDBase");
		type1cTopDictOpResolver.put(new Integer(1036), "FDArray");
		type1cTopDictOpResolver.put(new Integer(1037), "FDSelect");
		type1cTopDictOpResolver.put(new Integer(1038), "FontName");
	}
	
	/*
ROS12 30//SID SID number–, Registry Ordering Supplement
CIDFontVersion12 31//number0
CIDFontRevision12 32//number0
CIDFontType12 33//number0
CIDCount12 34//number8720
UIDBase12 35//number–
FDArray12 36//number–, Font DICT (FD) INDEX offset (0)
FDSelect12 37//number–, FDSelect offset (0)
FontName12 38//SID–, FD FontName
	 */
	
	private static HashMap glyphProgOpResolver = new HashMap();
	static {
		glyphProgOpResolver.put(new Integer(1), "hstem");
		
		glyphProgOpResolver.put(new Integer(3), "vstem");
		glyphProgOpResolver.put(new Integer(4), "vmoveto");
		glyphProgOpResolver.put(new Integer(5), "rlineto");
		glyphProgOpResolver.put(new Integer(6), "hlineto");
		glyphProgOpResolver.put(new Integer(7), "vlineto");
		glyphProgOpResolver.put(new Integer(8), "rrcurveto");
		
		glyphProgOpResolver.put(new Integer(10), "callsubr");
		glyphProgOpResolver.put(new Integer(11), "return");
		
		glyphProgOpResolver.put(new Integer(14), "endchar");
		
		glyphProgOpResolver.put(new Integer(18), "hstemhm");
		glyphProgOpResolver.put(new Integer(19), "hintmask");
		glyphProgOpResolver.put(new Integer(20), "cntrmask");
		glyphProgOpResolver.put(new Integer(21), "rmoveto");
		glyphProgOpResolver.put(new Integer(22), "hmoveto");
		glyphProgOpResolver.put(new Integer(23), "vstemhm");
		glyphProgOpResolver.put(new Integer(24), "rcurveline");
		glyphProgOpResolver.put(new Integer(25), "rlinecurve");
		glyphProgOpResolver.put(new Integer(26), "vvcurveto");
		glyphProgOpResolver.put(new Integer(27), "hhcurveto");
		
		glyphProgOpResolver.put(new Integer(29), "callsubr");
		glyphProgOpResolver.put(new Integer(30), "vhcurveto");
		glyphProgOpResolver.put(new Integer(31), "hvcurveto");
	}
	
	private static HashMap privateOpResolver = new HashMap();
	static {
		privateOpResolver.put(new Integer(6), "BlueValues");
		privateOpResolver.put(new Integer(7), "OtherBlues");
		privateOpResolver.put(new Integer(8), "FamilyBlues");
		privateOpResolver.put(new Integer(9), "FamilyOtherBlues");
		privateOpResolver.put(new Integer(1009), "BlueScale");
		privateOpResolver.put(new Integer(1010), "BlueSchift");
		privateOpResolver.put(new Integer(1011), "BlueFuzz");
		privateOpResolver.put(new Integer(10), "StdHW");
		privateOpResolver.put(new Integer(11), "StdVW");
		privateOpResolver.put(new Integer(1012), "StemSnapH");
		privateOpResolver.put(new Integer(1013), "StemSnapV");
		privateOpResolver.put(new Integer(1014), "ForceBold");
		privateOpResolver.put(new Integer(1017), "LanguageGroup");
		privateOpResolver.put(new Integer(1018), "ExpansionFactor");
		privateOpResolver.put(new Integer(1019), "InitialRandomSeed");
		privateOpResolver.put(new Integer(19), "Subrs");
		privateOpResolver.put(new Integer(20), "defaultWidthX");
		privateOpResolver.put(new Integer(21), "nominalWidthX");
	}
	
	private static HashMap sidResolver = new HashMap();
	static {
		sidResolver.put(new Integer(0), ".notdef");
		sidResolver.put(new Integer(1), "space");
		sidResolver.put(new Integer(2), "exclam");
		sidResolver.put(new Integer(3), "quotedbl");
		sidResolver.put(new Integer(4), "numbersign");
		sidResolver.put(new Integer(5), "dollar");
		sidResolver.put(new Integer(6), "percent");
		sidResolver.put(new Integer(7), "ampersand");
		sidResolver.put(new Integer(8), "quoteright");
		sidResolver.put(new Integer(9), "parenleft");
		sidResolver.put(new Integer(10), "parenright");
		sidResolver.put(new Integer(11), "asterisk");
		sidResolver.put(new Integer(12), "plus");
		sidResolver.put(new Integer(13), "comma");
		sidResolver.put(new Integer(14), "hyphen");
		sidResolver.put(new Integer(15), "period");
		sidResolver.put(new Integer(16), "slash");
		sidResolver.put(new Integer(17), "zero");
		sidResolver.put(new Integer(18), "one");
		sidResolver.put(new Integer(19), "two");
		sidResolver.put(new Integer(20), "three");
		sidResolver.put(new Integer(21), "four");
		sidResolver.put(new Integer(22), "five");
		sidResolver.put(new Integer(23), "six");
		sidResolver.put(new Integer(24), "seven");
		sidResolver.put(new Integer(25), "eight");
		sidResolver.put(new Integer(26), "nine");
		sidResolver.put(new Integer(27), "colon");
		sidResolver.put(new Integer(28), "semicolon");
		sidResolver.put(new Integer(29), "less");
		sidResolver.put(new Integer(30), "equal");
		sidResolver.put(new Integer(31), "greater");
		sidResolver.put(new Integer(32), "question");
		sidResolver.put(new Integer(33), "at");
		sidResolver.put(new Integer(34), "A");
		sidResolver.put(new Integer(35), "B");
		sidResolver.put(new Integer(36), "C");
		sidResolver.put(new Integer(37), "D");
		sidResolver.put(new Integer(38), "E");
		sidResolver.put(new Integer(39), "F");
		sidResolver.put(new Integer(40), "G");
		sidResolver.put(new Integer(41), "H");
		sidResolver.put(new Integer(42), "I");
		sidResolver.put(new Integer(43), "J");
		sidResolver.put(new Integer(44), "K");
		sidResolver.put(new Integer(45), "L");
		sidResolver.put(new Integer(46), "M");
		sidResolver.put(new Integer(47), "N");
		sidResolver.put(new Integer(48), "O");
		sidResolver.put(new Integer(49), "P");
		sidResolver.put(new Integer(50), "Q");
		sidResolver.put(new Integer(51), "R");
		sidResolver.put(new Integer(52), "S");
		sidResolver.put(new Integer(53), "T");
		sidResolver.put(new Integer(54), "U");
		sidResolver.put(new Integer(55), "V");
		sidResolver.put(new Integer(56), "W");
		sidResolver.put(new Integer(57), "X");
		sidResolver.put(new Integer(58), "Y");
		sidResolver.put(new Integer(59), "Z");
		sidResolver.put(new Integer(60), "bracketleft");
		sidResolver.put(new Integer(61), "backslash");
		sidResolver.put(new Integer(62), "bracketright");
		sidResolver.put(new Integer(63), "asciicircum");
		sidResolver.put(new Integer(64), "underscore");
		sidResolver.put(new Integer(65), "quoteleft");
		sidResolver.put(new Integer(66), "a");
		sidResolver.put(new Integer(67), "b");
		sidResolver.put(new Integer(68), "c");
		sidResolver.put(new Integer(69), "d");
		sidResolver.put(new Integer(70), "e");
		sidResolver.put(new Integer(71), "f");
		sidResolver.put(new Integer(72), "g");
		sidResolver.put(new Integer(73), "h");
		sidResolver.put(new Integer(74), "i");
		sidResolver.put(new Integer(75), "j");
		sidResolver.put(new Integer(76), "k");
		sidResolver.put(new Integer(77), "l");
		sidResolver.put(new Integer(78), "m");
		sidResolver.put(new Integer(79), "n");
		sidResolver.put(new Integer(80), "o");
		sidResolver.put(new Integer(81), "p");
		sidResolver.put(new Integer(82), "q");
		sidResolver.put(new Integer(83), "r");
		sidResolver.put(new Integer(84), "s");
		sidResolver.put(new Integer(85), "t");
		sidResolver.put(new Integer(86), "u");
		sidResolver.put(new Integer(87), "v");
		sidResolver.put(new Integer(88), "w");
		sidResolver.put(new Integer(89), "x");
		sidResolver.put(new Integer(90), "y");
		sidResolver.put(new Integer(91), "z");
		sidResolver.put(new Integer(92), "braceleft");
		sidResolver.put(new Integer(93), "bar");
		sidResolver.put(new Integer(94), "braceright");
		sidResolver.put(new Integer(95), "asciitilde");
		sidResolver.put(new Integer(96), "exclamdown");
		sidResolver.put(new Integer(97), "cent");
		sidResolver.put(new Integer(98), "sterling");
		sidResolver.put(new Integer(99), "fraction");
		sidResolver.put(new Integer(100), "yen");
		sidResolver.put(new Integer(101), "florin");
		sidResolver.put(new Integer(102), "section");
		sidResolver.put(new Integer(103), "currency");
		sidResolver.put(new Integer(104), "quotesingle");
		sidResolver.put(new Integer(105), "quotedblleft");
		sidResolver.put(new Integer(106), "guillemotleft");
		sidResolver.put(new Integer(107), "guilsinglleft");
		sidResolver.put(new Integer(108), "guilsinglright");
		sidResolver.put(new Integer(109), "fi");
		sidResolver.put(new Integer(110), "fl");
		sidResolver.put(new Integer(111), "endash");
		sidResolver.put(new Integer(112), "dagger");
		sidResolver.put(new Integer(113), "daggerdbl");
		sidResolver.put(new Integer(114), "periodcentered");
		sidResolver.put(new Integer(115), "paragraph");
		sidResolver.put(new Integer(116), "bullet");
		sidResolver.put(new Integer(117), "quotesinglbase");
		sidResolver.put(new Integer(118), "quotedblbase");
		sidResolver.put(new Integer(119), "quotedblright");
		sidResolver.put(new Integer(120), "guillemotright");
		sidResolver.put(new Integer(121), "ellipsis");
		sidResolver.put(new Integer(122), "perthousand");
		sidResolver.put(new Integer(123), "questiondown");
		sidResolver.put(new Integer(124), "grave");
		sidResolver.put(new Integer(125), "acute");
		sidResolver.put(new Integer(126), "circumflex");
		sidResolver.put(new Integer(127), "tilde");
		sidResolver.put(new Integer(128), "macron");
		sidResolver.put(new Integer(129), "breve");
		sidResolver.put(new Integer(130), "dotaccent");
		sidResolver.put(new Integer(131), "dieresis");
		sidResolver.put(new Integer(132), "ring");
		sidResolver.put(new Integer(133), "cedilla");
		sidResolver.put(new Integer(134), "hungarumlaut");
		sidResolver.put(new Integer(135), "ogonek");
		sidResolver.put(new Integer(136), "caron");
		sidResolver.put(new Integer(137), "emdash");
		sidResolver.put(new Integer(138), "AE");
		sidResolver.put(new Integer(139), "ordfeminine");
		sidResolver.put(new Integer(140), "Lslash");
		sidResolver.put(new Integer(141), "Oslash");
		sidResolver.put(new Integer(142), "OE");
		sidResolver.put(new Integer(143), "ordmasculine");
		sidResolver.put(new Integer(144), "ae");
		sidResolver.put(new Integer(145), "dotlessi");
		sidResolver.put(new Integer(146), "lslash");
		sidResolver.put(new Integer(147), "oslash");
		sidResolver.put(new Integer(148), "oe");
		sidResolver.put(new Integer(149), "germandbls");
		sidResolver.put(new Integer(150), "onesuperior");
		sidResolver.put(new Integer(151), "logicalnot");
		sidResolver.put(new Integer(152), "mu");
		sidResolver.put(new Integer(153), "trademark");
		sidResolver.put(new Integer(154), "Eth");
		sidResolver.put(new Integer(155), "onehalf");
		sidResolver.put(new Integer(156), "plusminus");
		sidResolver.put(new Integer(157), "Thorn");
		sidResolver.put(new Integer(158), "onequarter");
		sidResolver.put(new Integer(159), "divide");
		sidResolver.put(new Integer(160), "brokenbar");
		sidResolver.put(new Integer(161), "degree");
		sidResolver.put(new Integer(162), "thorn");
		sidResolver.put(new Integer(163), "threequarters");
		sidResolver.put(new Integer(164), "twosuperior");
		sidResolver.put(new Integer(165), "registered");
		sidResolver.put(new Integer(166), "minus");
		sidResolver.put(new Integer(167), "eth");
		sidResolver.put(new Integer(168), "multiply");
		sidResolver.put(new Integer(169), "threesuperior");
		sidResolver.put(new Integer(170), "copyright");
		sidResolver.put(new Integer(171), "Aacute");
		sidResolver.put(new Integer(172), "Acircumflex");
		sidResolver.put(new Integer(173), "Adieresis");
		sidResolver.put(new Integer(174), "Agrave");
		sidResolver.put(new Integer(175), "Aring");
		sidResolver.put(new Integer(176), "Atilde");
		sidResolver.put(new Integer(177), "Ccedilla");
		sidResolver.put(new Integer(178), "Eacute");
		sidResolver.put(new Integer(179), "Ecircumflex");
		sidResolver.put(new Integer(180), "Edieresis");
		sidResolver.put(new Integer(181), "Egrave");
		sidResolver.put(new Integer(182), "Iacute");
		sidResolver.put(new Integer(183), "Icircumflex");
		sidResolver.put(new Integer(184), "Idieresis");
		sidResolver.put(new Integer(185), "Igrave");
		sidResolver.put(new Integer(186), "Ntilde");
		sidResolver.put(new Integer(187), "Oacute");
		sidResolver.put(new Integer(188), "Ocircumflex");
		sidResolver.put(new Integer(189), "Odieresis");
		sidResolver.put(new Integer(190), "Ograve");
		sidResolver.put(new Integer(191), "Otilde");
		sidResolver.put(new Integer(192), "Scaron");
		sidResolver.put(new Integer(193), "Uacute");
		sidResolver.put(new Integer(194), "Ucircumflex");
		sidResolver.put(new Integer(195), "Udieresis");
		sidResolver.put(new Integer(196), "Ugrave");
		sidResolver.put(new Integer(197), "Yacute");
		sidResolver.put(new Integer(198), "Ydieresis");
		sidResolver.put(new Integer(199), "Zcaron");
		sidResolver.put(new Integer(200), "aacute");
		sidResolver.put(new Integer(201), "acircumflex");
		sidResolver.put(new Integer(202), "adieresis");
		sidResolver.put(new Integer(203), "agrave");
		sidResolver.put(new Integer(204), "aring");
		sidResolver.put(new Integer(205), "atilde");
		sidResolver.put(new Integer(206), "ccedilla");
		sidResolver.put(new Integer(207), "eacute");
		sidResolver.put(new Integer(208), "ecircumflex");
		sidResolver.put(new Integer(209), "edieresis");
		sidResolver.put(new Integer(210), "egrave");
		sidResolver.put(new Integer(211), "iacute");
		sidResolver.put(new Integer(212), "icircumflex");
		sidResolver.put(new Integer(213), "idieresis");
		sidResolver.put(new Integer(214), "igrave");
		sidResolver.put(new Integer(215), "ntilde");
		sidResolver.put(new Integer(216), "oacute");
		sidResolver.put(new Integer(217), "ocircumflex");
		sidResolver.put(new Integer(218), "odieresis");
		sidResolver.put(new Integer(219), "ograve");
		sidResolver.put(new Integer(220), "otilde");
		sidResolver.put(new Integer(221), "scaron");
		sidResolver.put(new Integer(222), "uacute");
		sidResolver.put(new Integer(223), "ucircumflex");
		sidResolver.put(new Integer(224), "udieresis");
		sidResolver.put(new Integer(225), "ugrave");
		sidResolver.put(new Integer(226), "yacute");
		sidResolver.put(new Integer(227), "ydieresis");
		sidResolver.put(new Integer(228), "zcaron");
		sidResolver.put(new Integer(229), "exclamsmall");
		sidResolver.put(new Integer(230), "Hungarumlautsmall");
		sidResolver.put(new Integer(231), "dollaroldstyle");
		sidResolver.put(new Integer(232), "dollarsuperior");
		sidResolver.put(new Integer(233), "ampersandsmall");
		sidResolver.put(new Integer(234), "Acutesmall");
		sidResolver.put(new Integer(235), "parenleftsuperior");
		sidResolver.put(new Integer(236), "parenrightsuperior");
		sidResolver.put(new Integer(237), "twodotenleader");
		sidResolver.put(new Integer(238), "onedotenleader");
		sidResolver.put(new Integer(239), "zerooldstyle");
		sidResolver.put(new Integer(240), "oneoldstyle");
		sidResolver.put(new Integer(241), "twooldstyle");
		sidResolver.put(new Integer(242), "threeoldstyle");
		sidResolver.put(new Integer(243), "fouroldstyle");
		sidResolver.put(new Integer(244), "fiveoldstyle");
		sidResolver.put(new Integer(245), "sixoldstyle");
		sidResolver.put(new Integer(246), "sevenoldstyle");
		sidResolver.put(new Integer(247), "eightoldstyle");
		sidResolver.put(new Integer(248), "nineoldstyle");
		sidResolver.put(new Integer(249), "commasuperior");
		sidResolver.put(new Integer(250), "threequartersemdash");
		sidResolver.put(new Integer(251), "periodsuperior");
		sidResolver.put(new Integer(252), "questionsmall");
		sidResolver.put(new Integer(253), "asuperior");
		sidResolver.put(new Integer(254), "bsuperior");
		sidResolver.put(new Integer(255), "centsuperior");
		sidResolver.put(new Integer(256), "dsuperior");
		sidResolver.put(new Integer(257), "esuperior");
		sidResolver.put(new Integer(258), "isuperior");
		sidResolver.put(new Integer(259), "lsuperior");
		sidResolver.put(new Integer(260), "msuperior");
		sidResolver.put(new Integer(261), "nsuperior");
		sidResolver.put(new Integer(262), "osuperior");
		sidResolver.put(new Integer(263), "rsuperior");
		sidResolver.put(new Integer(264), "ssuperior");
		sidResolver.put(new Integer(265), "tsuperior");
		sidResolver.put(new Integer(266), "ff");
		sidResolver.put(new Integer(267), "ffi");
		sidResolver.put(new Integer(268), "ffl");
		sidResolver.put(new Integer(269), "parenleftinferior");
		sidResolver.put(new Integer(270), "parenrightinferior");
		sidResolver.put(new Integer(271), "Circumflexsmall");
		sidResolver.put(new Integer(272), "hyphensuperior");
		sidResolver.put(new Integer(273), "Gravesmall");
		sidResolver.put(new Integer(274), "Asmall");
		sidResolver.put(new Integer(275), "Bsmall");
		sidResolver.put(new Integer(276), "Csmall");
		sidResolver.put(new Integer(277), "Dsmall");
		sidResolver.put(new Integer(278), "Esmall");
		sidResolver.put(new Integer(279), "Fsmall");
		sidResolver.put(new Integer(280), "Gsmall");
		sidResolver.put(new Integer(281), "Hsmall");
		sidResolver.put(new Integer(282), "Ismall");
		sidResolver.put(new Integer(283), "Jsmall");
		sidResolver.put(new Integer(284), "Ksmall");
		sidResolver.put(new Integer(285), "Lsmall");
		sidResolver.put(new Integer(286), "Msmall");
		sidResolver.put(new Integer(287), "Nsmall");
		sidResolver.put(new Integer(288), "Osmall");
		sidResolver.put(new Integer(289), "Psmall");
		sidResolver.put(new Integer(290), "Qsmall");
		sidResolver.put(new Integer(291), "Rsmall");
		sidResolver.put(new Integer(292), "Ssmall");
		sidResolver.put(new Integer(293), "Tsmall");
		sidResolver.put(new Integer(294), "Usmall");
		sidResolver.put(new Integer(295), "Vsmall");
		sidResolver.put(new Integer(296), "Wsmall");
		sidResolver.put(new Integer(297), "Xsmall");
		sidResolver.put(new Integer(298), "Ysmall");
		sidResolver.put(new Integer(299), "Zsmall");
		sidResolver.put(new Integer(300), "colonmonetary");
		sidResolver.put(new Integer(301), "onefitted");
		sidResolver.put(new Integer(302), "rupiah");
		sidResolver.put(new Integer(303), "Tildesmall");
		sidResolver.put(new Integer(304), "exclamdownsmall");
		sidResolver.put(new Integer(305), "centoldstyle");
		sidResolver.put(new Integer(306), "Lslashsmall");
		sidResolver.put(new Integer(307), "Scaronsmall");
		sidResolver.put(new Integer(308), "Zcaronsmall");
		sidResolver.put(new Integer(309), "Dieresissmall");
		sidResolver.put(new Integer(310), "Brevesmall");
		sidResolver.put(new Integer(311), "Caronsmall");
		sidResolver.put(new Integer(312), "Dotaccentsmall");
		sidResolver.put(new Integer(313), "Macronsmall");
		sidResolver.put(new Integer(314), "figuredash");
		sidResolver.put(new Integer(315), "hypheninferior");
		sidResolver.put(new Integer(316), "Ogoneksmall");
		sidResolver.put(new Integer(317), "Ringsmall");
		sidResolver.put(new Integer(318), "Cedillasmall");
		sidResolver.put(new Integer(319), "questiondownsmall");
		sidResolver.put(new Integer(320), "oneeighth");
		sidResolver.put(new Integer(321), "threeeighths");
		sidResolver.put(new Integer(322), "fiveeighths");
		sidResolver.put(new Integer(323), "seveneighths");
		sidResolver.put(new Integer(324), "onethird");
		sidResolver.put(new Integer(325), "twothirds");
		sidResolver.put(new Integer(326), "zerosuperior");
		sidResolver.put(new Integer(327), "foursuperior");
		sidResolver.put(new Integer(328), "fivesuperior");
		sidResolver.put(new Integer(329), "sixsuperior");
		sidResolver.put(new Integer(330), "sevensuperior");
		sidResolver.put(new Integer(331), "eightsuperior");
		sidResolver.put(new Integer(332), "ninesuperior");
		sidResolver.put(new Integer(333), "zeroinferior");
		sidResolver.put(new Integer(334), "oneinferior");
		sidResolver.put(new Integer(335), "twoinferior");
		sidResolver.put(new Integer(336), "threeinferior");
		sidResolver.put(new Integer(337), "fourinferior");
		sidResolver.put(new Integer(338), "fiveinferior");
		sidResolver.put(new Integer(339), "sixinferior");
		sidResolver.put(new Integer(340), "seveninferior");
		sidResolver.put(new Integer(341), "eightinferior");
		sidResolver.put(new Integer(342), "nineinferior");
		sidResolver.put(new Integer(343), "centinferior");
		sidResolver.put(new Integer(344), "dollarinferior");
		sidResolver.put(new Integer(345), "periodinferior");
		sidResolver.put(new Integer(346), "commainferior");
		sidResolver.put(new Integer(347), "Agravesmall");
		sidResolver.put(new Integer(348), "Aacutesmall");
		sidResolver.put(new Integer(349), "Acircumflexsmall");
		sidResolver.put(new Integer(350), "Atildesmall");
		sidResolver.put(new Integer(351), "Adieresissmall");
		sidResolver.put(new Integer(352), "Aringsmall");
		sidResolver.put(new Integer(353), "AEsmall");
		sidResolver.put(new Integer(354), "Ccedillasmall");
		sidResolver.put(new Integer(355), "Egravesmall");
		sidResolver.put(new Integer(356), "Eacutesmall");
		sidResolver.put(new Integer(357), "Ecircumflexsmall");
		sidResolver.put(new Integer(358), "Edieresissmall");
		sidResolver.put(new Integer(359), "Igravesmall");
		sidResolver.put(new Integer(360), "Iacutesmall");
		sidResolver.put(new Integer(361), "Icircumflexsmall");
		sidResolver.put(new Integer(362), "Idieresissmall");
		sidResolver.put(new Integer(363), "Ethsmall");
		sidResolver.put(new Integer(364), "Ntildesmall");
		sidResolver.put(new Integer(365), "Ogravesmall");
		sidResolver.put(new Integer(366), "Oacutesmall");
		sidResolver.put(new Integer(367), "Ocircumflexsmall");
		sidResolver.put(new Integer(368), "Otildesmall");
		sidResolver.put(new Integer(369), "Odieresissmall");
		sidResolver.put(new Integer(370), "OEsmall");
		sidResolver.put(new Integer(371), "Oslashsmall");
		sidResolver.put(new Integer(372), "Ugravesmall");
		sidResolver.put(new Integer(373), "Uacutesmall");
		sidResolver.put(new Integer(374), "Ucircumflexsmall");
		sidResolver.put(new Integer(375), "Udieresissmall");
		sidResolver.put(new Integer(376), "Yacutesmall");
		sidResolver.put(new Integer(377), "Thornsmall");
		sidResolver.put(new Integer(378), "Ydieresissmall");
		sidResolver.put(new Integer(379), "001.000");
		sidResolver.put(new Integer(380), "001.001");
		sidResolver.put(new Integer(381), "001.002");
		sidResolver.put(new Integer(382), "001.003");
		sidResolver.put(new Integer(383), "Black");
		sidResolver.put(new Integer(384), "Bold");
		sidResolver.put(new Integer(385), "Book");
		sidResolver.put(new Integer(386), "Light");
		sidResolver.put(new Integer(387), "Medium");
		sidResolver.put(new Integer(388), "Regular");
		sidResolver.put(new Integer(389), "Roman");
		sidResolver.put(new Integer(390), "Semibold");
	}
	
	private static final int readUnsigned(byte[] bytes, int s, int e) {
		int ui = convertUnsigned(bytes[s++]);
		for (; s < e;) {
			ui <<= 8;
			ui += convertUnsigned(bytes[s++]);
		}
		return ui;
	}
	
	private static final int convertUnsigned(byte b) {
		return ((b < 0) ? (((int) b) + 256) : b);
	}
	
	private static PdfFont readFontTrueType(Hashtable fontData, HashMap objects) throws IOException {
		Object fnObj = PdfParser.dereference(fontData.get("BaseFont"), objects);
		if (fnObj == null) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> missing font name");
			}
			return null;
		}
		
		boolean isSymbolic = false;
		Object fdObj = PdfParser.dereference(fontData.get("FontDescriptor"), objects);
		if (fdObj instanceof Hashtable) {
			Object fObj = ((Hashtable) fdObj).get("Flags");
			if (fObj != null)
				isSymbolic = (fObj.toString().indexOf("3") != -1);
		}
		
		BaseFont baseFont = getBuiltInFont(fnObj, isSymbolic);
		
		if (fdObj instanceof Hashtable) {
			if (DEBUG_LOAD_FONTS) System.out.println("Got font descriptor: " + fdObj);
			if (baseFont != null)
				for (Iterator kit = baseFont.descriptor.keySet().iterator(); kit.hasNext();) {
					Object key = kit.next();
					if (!((Hashtable) fdObj).containsKey(key)) {
						((Hashtable) fdObj).put(key, baseFont.descriptor.get(key));
					}
				}
		}
		else {
			if (baseFont == null) {
				if (DEBUG_LOAD_FONTS) {
					System.out.println("Problem in font " + fontData);
					System.out.println(" --> strange font descriptor: " + fdObj);
				}
				Object dfaObj = fontData.get("DescendantFonts");
				if (dfaObj instanceof Vector) {
					Object dfObj = PdfParser.dereference(((Vector) dfaObj).get(0), objects);
					if (dfObj != null) {
						if (DEBUG_LOAD_FONTS) System.out.println(" --> descendant fonts: " + dfObj);
					}
				}
				Object tuObj = PdfParser.dereference(fontData.get("ToUnicode"), objects);
				if (tuObj instanceof PStream)
					PdfParser.decodeObjectStream(((PStream) tuObj), objects, true);
				else if (DEBUG_LOAD_FONTS) System.out.println(" --> to unicode: " + tuObj);
				return null;
			}
			else fdObj = baseFont.descriptor;
		}
		
		HashMap toUnicodeMappings = null;
		Object tuObj = PdfParser.dereference(fontData.get("ToUnicode"), objects);
		if (tuObj instanceof PStream) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Object filter = ((PStream) tuObj).params.get("Filter");
			if (filter == null)
				filter = "FlateDecode";
			PdfParser.decode(filter, ((PStream) tuObj).bytes, ((PStream) tuObj).params, baos, objects);
			byte[] tuMapData = baos.toByteArray();
			if (DEBUG_LOAD_FONTS)
				System.out.println(" --> to unicode: " + new String(tuMapData));
			toUnicodeMappings = readToUnicodeCMap(tuMapData, true);
		}
		else if (DEBUG_LOAD_FONTS) System.out.println(" --> to unicode: " + tuObj);
		
		Object fcObj = fontData.get("FirstChar");
		if (!(fcObj instanceof Number)) {
			if (baseFont == null) {
				if (DEBUG_LOAD_FONTS) {
					System.out.println("Problem in font " + fontData);
					System.out.println(" --> strange first char: " + fcObj);
				}
				return null;
			}
			else fcObj = new Integer(baseFont.firstChar);
		}
		Object lcObj = fontData.get("LastChar");
		if (!(lcObj instanceof Number)) {
			if (baseFont == null) {
				if (DEBUG_LOAD_FONTS) {
					System.out.println("Problem in font " + fontData);
					System.out.println(" --> strange last char: " + lcObj);
				}
				return null;
			}
			else lcObj = new Integer(baseFont.lastChar);
		}
		Object cwObj = PdfParser.dereference(fontData.get("Widths"), objects);
		if (!(cwObj instanceof Vector)) {
			if (baseFont == null) {
				if (DEBUG_LOAD_FONTS) {
					System.out.println("Problem in font " + fontData);
					System.out.println(" --> strange char widths: " + cwObj);
				}
				return null;
			}
			else {
				cwObj = new Vector();
				for (int c = 0; c < baseFont.charWidths.length; c++)
					((Vector) cwObj).add(new Float(baseFont.charWidths[c]));
			}
		}
		
		Object feObj = PdfParser.dereference(fontData.get("Encoding"), objects);
		HashMap diffEncodings = new HashMap();
		if (feObj instanceof Hashtable) {
			if (DEBUG_LOAD_FONTS) System.out.println("Got encoding: " + feObj);
			Object dObj = PdfParser.dereference(((Hashtable) feObj).get("Differences"), objects);
			if (dObj != null) {
				if (DEBUG_LOAD_FONTS) System.out.println("Got differences: " + dObj);
				if (dObj instanceof Vector) {
					Vector diffs = ((Vector) dObj);
					int code = -1;
					for (int d = 0; d < diffs.size(); d++) {
						Object diff = diffs.get(d);
						if (diff instanceof Number)
							code = ((Number) diff).intValue();
						else if (code != -1) {
							char diffCh = StringUtils.getCharForName(diff.toString());
							if (diffCh != 0)
								diffEncodings.put(new Integer(code), new Character(diffCh));
							code++;
						}
					}
				}
				else {
					if (DEBUG_LOAD_FONTS) {
						System.out.println("Problem in font " + fontData);
						System.out.println(" --> strange differences definition: " + dObj);
					}
					return null;
				}
				if (DEBUG_LOAD_FONTS) {
					System.out.print("Got mappings: {");
					for (Iterator dit = diffEncodings.keySet().iterator(); dit.hasNext();) {
						Integer d = ((Integer) dit.next());
						Character c = ((Character) diffEncodings.get(d));
						System.out.print(d + "=" + c.charValue() + "(" + StringUtils.getNormalForm(c.charValue()) + ")");
						if (dit.hasNext())
							System.out.print(", ");
					}
					System.out.println("}");
				}
			}
			
			//	get encoding name
			feObj = PdfParser.dereference(((Hashtable) feObj).get("BaseEncoding"), objects);
		}
		if ((feObj == null) && (fdObj instanceof Hashtable))
			feObj = PdfParser.dereference(((Hashtable) fdObj).get("Encoding"), objects);
		
		Hashtable fd = ((Hashtable) fdObj);
		int fc = ((Number) fcObj).intValue();
		int lc = ((Number) lcObj).intValue();
		Vector cwv = ((Vector) cwObj);
		if (cwv.size() != (lc - fc + 1)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" --> invalid char widths");
			}
			return null;
		}
		float[] cws = new float[lc - fc + 1];
		for (int c = 0; c < cws.length; c++) {
			Object cwo = cwv.get(c);
			cws[c] = ((cwo instanceof Number) ? ((Number) cwo).floatValue() : 0);
		}
		Object mcwObj = fd.get("MissingWidth");
		float mcw = ((mcwObj instanceof Number) ? ((Number) mcwObj).floatValue() : 0);
		
		Object fbbObj = fd.get("FontBBox");
		if (!(fbbObj instanceof Vector) || (((Vector) fbbObj).size() != 4)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange bounding box: " + fbbObj);
			}
			fbbObj = null;
		}
		
		Object aObj = fd.get("Ascent");
		if (!(aObj instanceof Number) || ((((Number) aObj).floatValue() == 0) && fd.containsKey("Ascender")))
			aObj = fd.get("Ascender");
		if (!(aObj instanceof Number) || (((Number) aObj).floatValue() == 0)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange ascent: " + aObj);
			}
			if ((fbbObj != null) && (((Vector) fbbObj).get(2) instanceof Number)) {
				aObj = ((Vector) fbbObj).get(2);
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> fallback from bounding box: " + aObj);
			}
			else {
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> could not determine ascent");
				return null;
			}
		}
		
		Object dObj = fd.get("Descent");
		if (!(dObj instanceof Number) || ((((Number) dObj).floatValue() == 0)) && fd.containsKey("Descender"))
			dObj = fd.get("Descender");
		if (!(dObj instanceof Number)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange descent: " + dObj);
			}
			if ((fbbObj != null) && (((Vector) fbbObj).get(0) instanceof Number)) {
				dObj = ((Vector) fbbObj).get(2);
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> fallback from bounding box: " + dObj);
			}
			else {
				if (DEBUG_LOAD_FONTS)
					System.out.println(" --> could not determine descent");
				return null;
			}
		}
		Object chObj = fd.get("CapHeight");
		if (!(chObj instanceof Number) || (((Number) chObj).floatValue() == 0)) {
			if (DEBUG_LOAD_FONTS) {
				System.out.println("Problem in font " + fontData);
				System.out.println(" - descriptor: " + fd);
				System.out.println(" --> strange cap height: " + chObj);
				System.out.println(" --> fallback to ascent: " + aObj);
			}
			chObj = aObj;
		}
		
		PdfFont pFont = new PdfFont(fontData, fd, fc, lc, cws, mcw, fnObj.toString(), ((feObj == null) ? null : feObj.toString()));
		if (DEBUG_LOAD_FONTS) System.out.println("TrueType font created");
		pFont.ascent = (((Number) aObj).floatValue() / 1000);
		if (DEBUG_LOAD_FONTS) System.out.println(" - ascent is " + pFont.ascent);
		pFont.descent = (((Number) dObj).floatValue() / 1000);
		if (DEBUG_LOAD_FONTS) System.out.println(" - descent is " + pFont.descent);
		pFont.capHeigth = (((Number) chObj).floatValue() / 1000);
		if (DEBUG_LOAD_FONTS) System.out.println(" - cap height is " + pFont.capHeigth);
//		if (pFont.capHeigth == 0) {
//			pFont.capHeigth = pFont.ascent;
//			if (DEBUG_LOAD_FONTS) System.out.println(" - cap height corrected to " + pFont.capHeigth);
//		}
		
		for (Iterator cmit = diffEncodings.keySet().iterator(); cmit.hasNext();) {
			Integer chc = ((Integer) cmit.next());
			pFont.mapDifference(chc, ((Character) diffEncodings.get(chc)));
		}
		
		pFont.setBaseFont(baseFont);
		
		if (toUnicodeMappings != null)
			for (Iterator cmit = toUnicodeMappings.keySet().iterator(); cmit.hasNext();) {
				Integer chc = ((Integer) cmit.next());
				pFont.mapUnicode(chc, ((String) toUnicodeMappings.get(chc)));
			}
		
		return pFont;
	}
	
	private static char getChar(BufferedImage img, int tBelowCh, String charName, float fontSize, HashMap cache) {
		
		//	get char for name from StringUtils and try that character first
		char nch = StringUtils.getCharForName(charName);
		if (DEBUG_LOAD_FONTS) System.out.println("Resolved " + charName + " to '" + nch + "' (" + ((int) nch) + ")");
		
		//	immediately return for whitespaces
		if (Character.isWhitespace(nch))
			return nch;
		if ((8192 <= nch) && (nch <= 8207))
			return nch;
		if ((8232 <= nch) && (nch <= 8239))
			return nch;
		
		//	get fonts
		Font fontSerif = getSerifFont();
		Font fontSans = getSansSerifFont();
		Font font = fontSerif;
		
		//	set up matching and statistics
		ImageSimilarity sim = null;
		int chImgCount = 0;
		long start = System.currentTimeMillis();
		
		
		//	increase match offset step by step as long as best similarity below 50%
		for (int o = 2; o <= 16; o *= 2) {
			
			//	try named char match first
			for (int s = Font.PLAIN; s < (Font.BOLD | Font.ITALIC); s++) {
				Font sFont = font.deriveFont(s);
				ImageSimilarity sSim = matchChar(img, tBelowCh, nch, sFont, fontSize, cache, o, false);
				chImgCount++;
				if ((sSim != null) && ((sim == null) || (sSim.sim > sim.sim)))
					sim = sSim;
			}
			
			//	this one's good enough
			if ((sim != null) && (sim.sim > 0.5))
				break;
		}
		if ((sim != null) && (sim.sim > 0.8)) {
			if (DEBUG_LOAD_FONTS) System.out.println("Got name char match for '" + charName + "': '" + nch + "' (" + ((int) nch) + ", " + StringUtils.getCharName(nch) + ") at font size " + sim.match.fontSize + " with similarity " + sim.sim + " using " + chImgCount + " checks in " + (System.currentTimeMillis() - start) + " ms");
			return nch;
		}
		
		//	use base chars to find actual char
		return getChar(img, tBelowCh, charName, true, font, fontSize, cache);
	}
	
	private static char getChar(BufferedImage img, int tBelowCh, String charName, boolean lettersAndDigits, Font font, float fontSize, HashMap cache) {
		int chImgCount = 0;
		long start = System.currentTimeMillis();
		
		//	find base char (good match with low unmatched pixel count), and then try only derivatives of the latter
		HashSet baseChars = null;
		if (lettersAndDigits) {
			ImageSimilarity maxBcSim = null;
			ArrayList nearMaxBcSims = new ArrayList();
			for (int c = 33; c < 128; c++) {
				
				//	get character
				char ch = ((char) c);
				
				//	jump ignorable character types
				if (!Character.isLetterOrDigit(ch))
					continue;
				if (Character.isWhitespace(ch))
					continue;
				
				//	render character
				CharImage chImg = createImageChar(img.getWidth(), img.getHeight(), tBelowCh, ch, font, ((maxBcSim == null) ? fontSize : maxBcSim.match.fontSize), cache);
				if (chImg == null)
					continue;
				chImgCount++;
				
				//	compare to target image
				ImageSimilarity sim = getSimilarity(img, chImg, 2);
				
				//	this one is too far away
				if (sim.matched < (50 * sim.unmatched))
					continue;
				if (sim.sim < 0.5)
					continue;
				
				//	this one might be OK
				if ((maxBcSim == null) || (sim.sim > maxBcSim.sim)) {
					if ((maxBcSim != null) && ((sim.sim - maxBcSim.sim) < 0.05))
						nearMaxBcSims.add(maxBcSim);
					maxBcSim = sim;
				}
				else if ((maxBcSim.sim - sim.sim) < 0.05)
					nearMaxBcSims.add(sim);
			}
			
			//	no base char found, try symbols
			if (maxBcSim == null) {
				if (DEBUG_LOAD_FONTS) System.out.println("Base char not found, recursing for punctuation and symbols");
				return getChar(img, tBelowCh, charName, false, font, fontSize, cache);
			}
			
			//	collect base chars for lookup
			nearMaxBcSims.add(maxBcSim);
			baseChars = new HashSet();
			for (int s = 0; s < nearMaxBcSims.size(); s++) {
				ImageSimilarity sim = ((ImageSimilarity) nearMaxBcSims.get(s));
				if ((maxBcSim.sim - sim.sim) >= 0.05) {
					nearMaxBcSims.remove(s--);
					continue;
				}
				baseChars.add(new Character(sim.match.ch));
				if (DEBUG_LOAD_FONTS) System.out.println("Base char match for '" + charName + "': '" + sim.match.ch + "' (" + ((int) sim.match.ch) + ", " + StringUtils.getCharName(sim.match.ch) + ") at font size " + sim.match.fontSize + " with similarity is " + sim.sim + " (stat: m" + sim.matched + "/r" + sim.remainder + "/u" + sim.unmatched + ") using " + chImgCount + " checks in " + (System.currentTimeMillis() - start) + " ms");
			}
		}
		
		//	try to find char by trial and error (high effort, but only way ...)
		ImageSimilarity maxSim = null;
		ArrayList nearMaxSims = new ArrayList();
		for (int c = 33; c < Character.MAX_VALUE; c++) {
			
			//	jump whitespace and private areas
			if ((8192 <= c) && (c <= 8207))
				continue;
			if ((8232 <= c) && (c <= 8239))
				continue;
			if ((57344 <= c) && (c <= 63743))
				continue;
			
			//	don't have user wait too long without feedback
			if ((c % 1000) == 0) {
				if (DEBUG_LOAD_FONTS) System.out.println("Done with " + c + " chars after " + chImgCount + " images in " + (System.currentTimeMillis() - start) + " ms");
			}
			
			//	get character
			char ch = ((char) c);
			
			//	jump ignorable character types
			if (Character.isLetterOrDigit(ch) != lettersAndDigits)
				continue;
			if (Character.isWhitespace(ch))
				continue;
			
			//	jump characters with the wrong base
			if (baseChars != null) {
				char bch = StringUtils.getBaseChar(ch);
				if (!baseChars.contains(new Character(bch)))
					continue;
			}
			
			//	jump combination characters
			String chn = StringUtils.getCharName(ch);
			if ((chn != null) && chn.endsWith("cmb"))
				continue;
			
			//	render character
			CharImage chImg = createImageChar(img.getWidth(), img.getHeight(), tBelowCh, ch, font, ((maxSim == null) ? fontSize : maxSim.match.fontSize), cache);
			if (chImg == null)
				continue;
			chImgCount++;
			
			//	compare to target image
			ImageSimilarity sim = getSimilarity(img, chImg, 2);
			if ((maxSim == null) || (sim.sim > maxSim.sim)) {
				if ((maxSim != null) && ((sim.sim - maxSim.sim) < 0.05))
					nearMaxSims.add(maxSim);
				maxSim = sim;
				if (DEBUG_LOAD_FONTS) System.out.println("Found new best match for '" + charName + "': '" + ch + "' (" + ((int) ch) + ", " + StringUtils.getCharName(ch) + ") at font size " + chImg.fontSize + " with similarity is " + sim.sim + " (stat: m" + sim.matched + "/r" + sim.remainder + "/u" + sim.unmatched + ") using " + chImgCount + " checks in " + (System.currentTimeMillis() - start) + " ms");
			}
			else if ((maxSim.sim - sim.sim) < 0.05) {
				nearMaxSims.add(sim);
				if (DEBUG_LOAD_FONTS) System.out.println("Keeping close-to-best match for '" + charName + "': '" + ch + "' (" + ((int) ch) + ", " + StringUtils.getCharName(ch) + ") at font size " + chImg.fontSize + " with similarity is " + sim.sim + " (stat: m" + sim.matched + "/r" + sim.remainder + "/u" + sim.unmatched + ") using " + chImgCount + " checks in " + (System.currentTimeMillis() - start) + " ms");
			}
		}
		
		if (maxSim != null)
			nearMaxSims.add(maxSim);
		
		if (nearMaxSims.size() != 0) {
			for (int s = 0; s < nearMaxSims.size(); s++) {
				ImageSimilarity sim = ((ImageSimilarity) nearMaxSims.get(s));
				if ((maxSim.sim - sim.sim) >= 0.05) {
					nearMaxSims.remove(s--);
					continue;
				}
				if (DEBUG_LOAD_FONTS) System.out.println("Good match for '" + charName + "': '" + sim.match.ch + "' (" + ((int) sim.match.ch) + ", " + StringUtils.getCharName(sim.match.ch) + ") at font size " + sim.match.fontSize + " with similarity is " + sim.sim + " (stat: m" + sim.matched + "/r" + sim.remainder + "/u" + sim.unmatched + ") using " + chImgCount + " checks in " + (System.currentTimeMillis() - start) + " ms");
			}
		}
		
		//	if best match similarity for characters too low, try all unicode symbols (high effort, but probably inevitable ...)
		if ((maxSim == null) || (maxSim.sim < 0.5))
			return (lettersAndDigits ? getChar(img, tBelowCh, charName, false, font, fontSize, cache) : 0);
		
		//	found a sufficiently similar match
		if (DEBUG_LOAD_FONTS) System.out.println("Decoded image named '" + charName + "' to '" + maxSim.match.ch + "' (" + ((int) maxSim.match.ch) + ", " + StringUtils.getCharName(maxSim.match.ch) + ") at font size " + maxSim.match.fontSize + " with similarity " + maxSim.sim + " using " + chImgCount + " checks in " + (System.currentTimeMillis() - start) + " ms");
		return maxSim.match.ch;
	}
	
	private static final boolean DEBUG_CHAR_MATCHING = false;
	private static final char DEBUG_MATCH_TARGET_CHAR = ((char) 0);
	
	private static ImageSimilarity matchChar(BufferedImage img, int tBelowCh, char nch, Font font, float fontSize, HashMap cache, int maxOffset, boolean show) {
		return matchChar(img, createImageChar(img.getWidth(), img.getHeight(), tBelowCh, nch, font, fontSize, cache), maxOffset, show);
	}
	
	private static ImageSimilarity matchChar(BufferedImage img, int cWidth, int cHeight, int lSpace, int cBase, int cDesc, char nch, Font font, float fontSize, HashMap cache, int maxOffset, boolean show) {
		return matchChar(img, createImageChar(img.getWidth(), img.getHeight(), cWidth, cHeight, lSpace, cBase, cDesc, nch, font, fontSize, cache), maxOffset, show);
	}
	
	//	TODO when matching chars by name, simply cut white margins from unknown image, then render known character to same height rather than same width
	//	==> should be a lot faster
	
	private static ImageSimilarity matchChar(BufferedImage img, CharImage nchImg, int maxOffset, boolean show) {
		if (nchImg == null)
			return null;
		ImageSimilarity sim = getSimilarity(img, nchImg, maxOffset);
		if (show)
			displayCharMatch(img, sim, "Match result");
		return sim;
	}
	
	private static ImageSimilarity matchChar(BufferedImage img, CharImage nchImg, boolean show) {
		if (nchImg == null)
			return null;
		if (nchImg.ch == DEBUG_MATCH_TARGET_CHAR) System.out.println(" - getting similarity");
		ImageSimilarity sim = getSimilarity(img, nchImg);
		if (nchImg.ch == DEBUG_MATCH_TARGET_CHAR) System.out.println(" - similarity computed");
		if (show)
			displayCharMatch(img, sim, "Match result");
		return sim;
	}
	
	
	private static Font getSerifFont() {
		String osName = System.getProperty("os.name");
		String fontName = "Serif";
		if (osName.matches("Win.*"))
			fontName = "Times New Roman";
		else if (osName.matches(".*Linux.*")) {
			String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
			for (int f = 0; f < fontNames.length; f++)
				if (fontNames[f].toLowerCase().startsWith("times")) {
					fontName = fontNames[f];
					break;
				}
		}
		return Font.decode(fontName);
	}
	
	private static Font getSansSerifFont() {
		String osName = System.getProperty("os.name");
		String fontName = "SansSerif";
		if (osName.matches("Win.*"))
			fontName = "Arial Unicode MS";
		else if (osName.matches(".*Linux.*")) {
			String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
			for (int f = 0; f < fontNames.length; f++)
				if (fontNames[f].toLowerCase().startsWith("Arial")) {
					fontName = fontNames[f];
					break;
				}
		}
		return Font.decode(fontName);
	}
	
	private static BufferedImage createImageMask(int width, int height, byte[] bits) {
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = bi.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g.setColor(Color.BLACK);
		int rw = (((width % 8) == 0) ? width : (((width / 8) + 1) * 8));
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				int bitOffset = (rw * r) + c;
				int byteIndex = (bitOffset / 8);
				if (byteIndex < bits.length) {
					int byteOffset = (bitOffset % 8);
					int bt = bits[byteIndex];
					int btMask = 1 << (7 - byteOffset);
					boolean bit = ((bt & btMask) == 0);
					if (bit)
						g.drawLine(c, r, c, r);
				}
			}
		}
		return bi;
	}
	
	
	
	private static class CharImage {
		final BufferedImage img;
		final char ch;
		final float fontSize;
		final int shiftBelowBaseline;
		CharImage(BufferedImage img, char ch, float fontSize, int shiftBelowBaseline) {
			this.img = img;
			this.ch = ch;
			this.fontSize = fontSize;
			this.shiftBelowBaseline = shiftBelowBaseline;
		}
	}
	
	private static CharImage createImageChar(int width, int height, int trimBelowCh, char ch, Font font, float fontSize, HashMap cache) {
		if (!font.canDisplay(ch))
			return null;
		if (ch == 160)
			return null;
		String charKey = (ch + "-in-" + font.getName() + "-" + font.getStyle());
		if (unrenderableChars.contains(charKey))
			return null;
		String str = ("" + ch);
		BufferedImage bi = new BufferedImage((width+1), (height+1), BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D gr = bi.createGraphics();
		
		//	find font size
		TextLayout tl = null;
		do {
			if (tl == null)
				fontSize += 5; // go up more quickly, we run until overshooting anyway, and this covers big differences a lot more quickly
			else {
				double wFact = ((tl.getBounds().getWidth() > 0) ? Math.floor(((float) width) / tl.getBounds().getWidth()) : 1);
				double hFact = ((tl.getBounds().getHeight() > 0) ? Math.floor(((float) height) / tl.getBounds().getHeight()) : 1);
				double fact = Math.min(wFact, hFact);
				if (fact > 1)
					fontSize = ((float) Math.floor(fontSize * fact));
				else fontSize += 5;
			}
			if (fontSize > 1000) {
				if (DEBUG_LOAD_FONTS) System.out.println("Font size for '" + ch + "' (" + ((int) ch) + ") grew to " + fontSize + ", giving up");
				unrenderableChars.add(charKey);
				return null;
			}
			gr.setFont(font.deriveFont(fontSize));
			tl = new TextLayout(str, gr.getFont(), gr.getFontRenderContext());
		} while ((tl.getBounds().getWidth() <= width) && (tl.getBounds().getHeight() <= height));
		while ((tl.getBounds().getWidth() > width) || (tl.getBounds().getHeight() > height)) {
			fontSize--;
			gr.setFont(font.deriveFont(fontSize));
			tl = new TextLayout(str, gr.getFont(), gr.getFontRenderContext());
		}
		
		//	try cache lookup
		String cacheKey = null;
		if (cache != null) {
			cacheKey = (ch + "-in-" + font.getName() + "-" + font.getStyle() + "-at-" + fontSize + "-by-" + (((int) Math.floor(tl.getBounds().getMaxY())) - trimBelowCh));
			CharImage ci = ((CharImage) cache.get(cacheKey));
			if (ci != null) {
//				if (DEBUG_LOAD_FONTS) System.out.println(" - cache hit for " + cacheKey);
				return ci;
			}
		}
//		if (!Character.isLetterOrDigit(ch)) {
//			System.out.println("MinY is " + Math.floor(tl.getBounds().getMinY()) + ", MaxY is " + Math.floor(tl.getBounds().getMaxY()) + ", height is " + height + ", tBelowCh is " + trimBelowCh + ", tlDesc is " + tl.getDescent());
//			System.out.println("Baseline shift is " + (((int) Math.floor(tl.getBounds().getMaxY())) - trimBelowCh) + " at font size " + fontSize);
//		}
		
		//	render image
		gr.setFont(font.deriveFont(fontSize));
		gr.setColor(Color.WHITE);
		gr.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		gr.setColor(Color.BLACK);
		gr.drawString(str, (0-((int) Math.floor(tl.getBounds().getMinX()))), (height - ((int) Math.floor(tl.getBounds().getMaxY()))));
		gr.dispose();
		CharImage ci = new CharImage(bi, ch, fontSize, (((int) Math.floor(tl.getBounds().getMaxY())) - trimBelowCh));
		
		//	cache image if possible and return it
		if (cache != null)
			cache.put(cacheKey, ci);
		return ci;
	}
	
	private static CharImage createImageChar(int iWidth, int iHeight, int cWidth, int cHeight, int lSpace, int cBase, int cDesc, char ch, Font font, float fontSize, HashMap cache) {
		if (!font.canDisplay(ch))
			return null;
		if (ch == 160)
			return null;
		String charKey = (ch + "-in-" + font.getName() + "-" + font.getStyle());
		if (unrenderableChars.contains(charKey))
			return null;
		String str = ("" + ch);
		BufferedImage bi = new BufferedImage((iWidth+1), (iHeight+1), BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D gr = bi.createGraphics();
		
		//	find font size
		TextLayout tl = null;
		do {
			if (tl == null)
				fontSize += 5; // go up more quickly, we run until overshooting anyway, and this covers big differences a lot more quickly
			else {
				double wFact = ((tl.getBounds().getWidth() > 0) ? Math.floor(((float) (cWidth - lSpace)) / tl.getBounds().getWidth()) : 1);
				double hFact = ((tl.getBounds().getHeight() > 0) ? Math.floor(((float) cHeight) / tl.getBounds().getHeight()) : 1);
				double fact = Math.min(wFact, hFact);
				if (fact > 1)
					fontSize = ((float) Math.floor(fontSize * fact));
				else fontSize += 5;
			}
			if (fontSize > 1000) {
//				if (DEBUG_LOAD_FONTS) System.out.println("Font size for '" + ch + "' (" + ((int) ch) + ") grew to " + fontSize + ", giving up");
				unrenderableChars.add(charKey);
				return null;
			}
			gr.setFont(font.deriveFont(fontSize));
			tl = new TextLayout(str, gr.getFont(), gr.getFontRenderContext());
		} while ((tl.getBounds().getWidth() <= (cWidth - lSpace)) && (tl.getBounds().getHeight() <= cHeight));
		while ((tl.getBounds().getWidth() > (cWidth - lSpace)) || (tl.getBounds().getHeight() > cHeight)) {
			fontSize--;
			gr.setFont(font.deriveFont(fontSize));
			tl = new TextLayout(str, gr.getFont(), gr.getFontRenderContext());
		}
		
		//	for punctuation marks, whatever the settings, adjust vertical alignment, as this may differ even between individual styles in one font, and the more so between fonts
		int baselineShift = 0;
		if (!Character.isLetterOrDigit(ch)) {
//			System.out.println("MinY is " + Math.floor(tl.getBounds().getMinY()) + ", MaxY is " + Math.floor(tl.getBounds().getMaxY()) + ", iHeight is " + iHeight + ", cHeight is " + cHeight + ", cBase is " + cBase + ", cDesc is " + cDesc);
			if (tl.getBounds().getMinY() < -cHeight)
				baselineShift = (cHeight + ((int) Math.floor(tl.getBounds().getMinY())));
			else if ((tl.getBounds().getMaxY() < 0) && (tl.getBounds().getMaxY() > cDesc))
				baselineShift = -(cDesc - ((int) Math.floor(tl.getBounds().getMaxY())));
//			System.out.println("Baseline shift is " + baselineShift + " at font size " + fontSize);
		}
		
		//	try cache lookup
		String cacheKey = null;
		if (cache != null) {
			cacheKey = (ch + "-in-" + font.getName() + "-" + font.getStyle() + "-at-" + fontSize + "-by-" + baselineShift);
			CharImage ci = ((CharImage) cache.get(cacheKey));
			if (ci != null) {
//				if (DEBUG_LOAD_FONTS) System.out.println(" - cache hit for " + cacheKey);
				return ci;
			}
		}
		
		//	render image
		gr.setFont(font.deriveFont(fontSize));
		gr.setColor(Color.WHITE);
		gr.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		gr.setColor(Color.GRAY);
		gr.drawLine(0, (iHeight - cBase), iWidth, (iHeight - cBase));
		gr.setColor(Color.BLACK);
		gr.drawString(str, (0-((int) Math.floor(tl.getBounds().getMinX())) + lSpace), (iHeight - cBase - baselineShift));
		gr.dispose();
		CharImage ci = new CharImage(bi, ch, fontSize, baselineShift);
		
		//	cache image if possible and return it
		if (cache != null)
			cache.put(cacheKey, ci);
		return ci;
	}
	
	private static CharImage createImageChar(int width, int height, char ch, Font font, float fontSize, HashMap cache) {
		if (!font.canDisplay(ch))
			return null;
		if (ch == 160)
			return null;
		String charKey = (ch + "-in-" + font.getName() + "-" + font.getStyle());
		if (unrenderableChars.contains(charKey))
			return null;
		String str = ("" + ch);
		BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D gr = bi.createGraphics();
		
		//	find font size
		TextLayout tl = null;
		do {
			if (tl == null)
				fontSize += 5; // go up more quickly, we run until overshooting anyway, and this covers big differences a lot more quickly
			else {
				double fact = ((tl.getBounds().getHeight() > 0) ? Math.floor(((float) height) / tl.getBounds().getHeight()) : 1);
				if (fact > 1)
					fontSize = ((float) Math.floor(fontSize * fact));
				else fontSize += 5;
			}
			if (fontSize > 1000) {
				if (DEBUG_LOAD_FONTS) System.out.println("Font size for '" + ch + "' (" + ((int) ch) + ") grew to " + fontSize + ", giving up");
				unrenderableChars.add(charKey);
				return null;
			}
			gr.setFont(font.deriveFont(fontSize));
			tl = new TextLayout(str, gr.getFont(), gr.getFontRenderContext());
		} while ((tl.getBounds().getHeight() <= height));
		while ((tl.getBounds().getHeight() > height)) {
			fontSize--;
			gr.setFont(font.deriveFont(fontSize));
			tl = new TextLayout(str, gr.getFont(), gr.getFontRenderContext());
		}
		String cacheKey = null;
		if (cache != null) {
			cacheKey = (ch + "-in-" + font.getName() + "-" + font.getStyle() + "-at-" + fontSize + "-by-" + 0);
			CharImage ci = ((CharImage) cache.get(cacheKey));
			if (ci != null) {
//				if (DEBUG_LOAD_FONTS) System.out.println(" - cache hit for " + cacheKey);
				return ci;
			}
		}
		int iWidth = Math.max(width, ((int) Math.ceil(tl.getBounds().getWidth())));
		int iHeight = ((int) Math.ceil(tl.getBounds().getHeight() + tl.getAscent()));
		bi = new BufferedImage((iWidth + iWidth), ((int) Math.ceil(iHeight + tl.getDescent())), BufferedImage.TYPE_BYTE_GRAY);
		gr = bi.createGraphics();
		
		//	render image
		if (ch == DEBUG_MATCH_TARGET_CHAR) System.out.println(" - font size for '" + ch + "' (" + ((int) ch) + ") is " + fontSize);
		gr.setFont(font.deriveFont(fontSize));
		gr.setColor(Color.WHITE);
		gr.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		gr.setColor(Color.BLACK);
		if (ch == DEBUG_MATCH_TARGET_CHAR) System.out.println(" - scaling is " + (width / tl.getBounds().getWidth()));
		gr.scale((width / tl.getBounds().getWidth()), 1);
		gr.drawString(str, 0, iHeight);
		gr.dispose();
		if (ch == DEBUG_MATCH_TARGET_CHAR) {
			System.out.println(" - image rendered");
			JOptionPane.showMessageDialog(null, ("Rendering of '" + ch + "'"), "Char Image", JOptionPane.PLAIN_MESSAGE, new ImageIcon(bi));
		}
		
		//	cut image
		AnalysisImage ai = Imaging.wrapImage(bi, null);
		ImagePartRectangle ipr = Imaging.getContentBox(ai);
		CharImage ci = new CharImage(ipr.toImage().getImage(), ch, fontSize, 0);
		if (ch == DEBUG_MATCH_TARGET_CHAR) System.out.println(" - image margins cut");
		
		//	cache image if possible and return it
		if (cache != null)
			cache.put(cacheKey, ci);
		return ci;
	}
	
	
	private static class ImageSimilarity {
		CharImage match;
		int matched;
		int remainder;
		int unmatched;
		float sim;
		int xOff;
		int yOff;
		ImageSimilarity(CharImage match, int matched, int remainder, int unmatched, float sim, int xOff, int yOff) {
			this.match = match;
			this.matched = matched;
			this.remainder = remainder;
			this.unmatched = unmatched;
			this.sim = sim;
			this.xOff = xOff;
			this.yOff = yOff;
		}
	}
	
	private static final int white = Color.WHITE.getRGB();
	private static ImageSimilarity getSimilarity(BufferedImage base, CharImage match, int maxOffset) {
		int size = (((base.getWidth() * base.getHeight()) + (match.img.getWidth() * match.img.getHeight()) + 1) / 2);
		int bc = 0;
		int bc1 = 0;
		int bc2 = 0;
		for (int c = 0; c < Math.max(base.getWidth(), match.img.getWidth()); c++) {
			for (int r = 0; r < Math.max(base.getHeight(), match.img.getHeight()); r++) {
				int rgb1 = (((c >= 0) && (c < base.getWidth()) && (r >= 0) && (r < base.getHeight())) ? base.getRGB(c, r) : white);
				int rgb2 = (((c >= 0) && (c < match.img.getWidth()) && (r >= 0) && (r < match.img.getHeight())) ? match.img.getRGB(c, r) : white);
				if (rgb1 != white)
					bc1++;
				if (rgb2 != white)
					bc2++;
				if ((rgb2 != white) && (rgb1 != white))
					bc++;
			}
		}
		int[][] diffSums = new int[(2*maxOffset) + 1][(2*maxOffset) + 1];
		int[][] matched = new int[(2*maxOffset) + 1][(2*maxOffset) + 1];
		int[][] remainder = new int[(2*maxOffset) + 1][(2*maxOffset) + 1];
		int[][] unmatched = new int[(2*maxOffset) + 1][(2*maxOffset) + 1];
		for (int ho = -maxOffset; ho <= maxOffset; ho++) {
			for (int vo = -maxOffset; vo <= maxOffset; vo++) {
				diffSums[ho+maxOffset][vo+maxOffset] = 0;
				matched[ho+maxOffset][vo+maxOffset] = 0;
				remainder[ho+maxOffset][vo+maxOffset] = 0;
				unmatched[ho+maxOffset][vo+maxOffset] = 0;
				for (int c = 0; c < Math.max(base.getWidth(), (match.img.getWidth()-ho)); c++) {
					for (int r = 0; r < Math.max(base.getHeight(), (match.img.getHeight()-vo)); r++) {
						int rgb1 = (((c >= 0) && (c < base.getWidth()) && (r >= 0) && (r < base.getHeight())) ? base.getRGB(c, r) : white);
						int rgb2 = ((((c+ho) >= 0) && ((c+ho) < match.img.getWidth()) && ((r+vo) >= 0) && ((r+vo) < match.img.getHeight())) ? match.img.getRGB((c+ho), (r+vo)) : white);
						if (rgb1 == rgb2)
							matched[ho+maxOffset][vo+maxOffset]++;
						else {
							diffSums[ho+maxOffset][vo+maxOffset]++;
							if (rgb1 != white)
								remainder[ho+maxOffset][vo+maxOffset]++;
							else if (rgb2 != white)
								unmatched[ho+maxOffset][vo+maxOffset]++;
						}
					}
				}
			}
		}
		int minDiffSum = size;
		int minDiffXO = 0;
		int minDiffYO = 0;
		int minDiffMatched = 0;
		int minDiffRemainder = 0;
		int minDiffUnmatched = 0;
		for (int ho = -maxOffset; ho <= maxOffset; ho++) {
			for (int vo = -maxOffset; vo <= maxOffset; vo++)
				if (diffSums[ho+maxOffset][vo+maxOffset] < minDiffSum) {
					minDiffSum = diffSums[ho+maxOffset][vo+maxOffset];
					minDiffXO = ho;
					minDiffYO = vo;
					minDiffMatched = matched[ho+maxOffset][vo+maxOffset];
					minDiffRemainder = remainder[ho+maxOffset][vo+maxOffset];
					minDiffUnmatched = unmatched[ho+maxOffset][vo+maxOffset];
				}
		}
		return new ImageSimilarity(match, minDiffMatched, minDiffRemainder, minDiffUnmatched, (1f - (((float) minDiffSum) / (bc1 + bc2 - bc))), minDiffXO, minDiffYO);
	}
	
	private static ImageSimilarity getSimilarity(BufferedImage base, CharImage match) {
		int size = (((base.getWidth() * base.getHeight()) + (match.img.getWidth() * match.img.getHeight()) + 1) / 2);
		
		int hDiff = Math.abs(base.getWidth() - match.img.getWidth());
		int vDiff = Math.abs(base.getHeight() - match.img.getHeight());
		if (match.ch == DEBUG_MATCH_TARGET_CHAR) System.out.println("   - hDiff is " + hDiff + ", vDiff is " + vDiff);
		int[][] diffSums = new int[hDiff + 1][vDiff + 1];
		int[][] matched = new int[hDiff + 1][vDiff + 1];
		int[][] background = new int[hDiff + 1][vDiff + 1];
		int[][] remainder = new int[hDiff + 1][vDiff + 1];
		int[][] unmatched = new int[hDiff + 1][vDiff + 1];
		for (int hs = 0; hs <= hDiff; hs++) {
			for (int vs = 0; vs <= vDiff; vs++) {
				diffSums[hs][vs] = 0;
				matched[hs][vs] = 0;
				background[hs][vs] = 0;
				remainder[hs][vs] = 0;
				unmatched[hs][vs] = 0;
				for (int c = 0; c < Math.max(base.getWidth(), match.img.getWidth()); c++) {
					for (int r = 0; r < Math.max(base.getHeight(), match.img.getHeight()); r++) {
						int c1 = (c - ((base.getWidth() < match.img.getWidth()) ? hs : 0));
						int r1 = (r - ((base.getHeight() < match.img.getHeight()) ? vs : 0));
						int rgb1 = (((-1 < c1) && (c1 < base.getWidth()) && (-1 < r1) && (r1 < base.getHeight())) ? base.getRGB(c1, r1) : white);
						int c2 = (c - ((match.img.getWidth() < base.getWidth()) ? hs : 0));
						int r2 = (r - ((match.img.getHeight() < base.getHeight()) ? vs : 0));
						int rgb2 = (((-1 < c2) && (c2 < match.img.getWidth()) && (-1 < r2) && (r2 < match.img.getHeight())) ? match.img.getRGB(c2, r2) : white);
						if (rgb1 == rgb2) {
							if (rgb1 == white)
								background[hs][vs]++;
							else matched[hs][vs]++;
						}
						else {
							diffSums[hs][vs]++;
							if (rgb1 != white)
								remainder[hs][vs]++;
							else if (rgb2 != white)
								unmatched[hs][vs]++;
						}
					}
				}
			}
		}
		
		int minDiffSum = size;
		int minDiffXO = 0;
		int minDiffYO = 0;
		int minDiffMatched = 0;
		int minDiffBackground = 0;
		int minDiffRemainder = 0;
		int minDiffUnmatched = 0;
		for (int hs = 0; hs <= hDiff; hs++) {
			for (int vs = 0; vs <= vDiff; vs++)
				if (diffSums[hs][vs] < minDiffSum) {
					minDiffSum = diffSums[hs][vs];
					minDiffXO = ((base.getWidth() < match.img.getWidth()) ? hs : -hs);
					minDiffYO = ((base.getHeight() < match.img.getHeight()) ? vs : -vs);
					minDiffMatched = matched[hs][vs];
					minDiffBackground = background[hs][vs];
					minDiffRemainder = remainder[hs][vs];
					minDiffUnmatched = unmatched[hs][vs];
				}
		}
//		return new ImageSimilarity(match, (minDiffMatched + minDiffBackground), minDiffRemainder, minDiffUnmatched, (1f - (((float) minDiffSum) / (minDiffSum + minDiffMatched))), minDiffXO, minDiffYO);
		return new ImageSimilarity(match, (minDiffMatched + minDiffBackground), minDiffRemainder, minDiffUnmatched, (((float) minDiffMatched) / (minDiffMatched + minDiffRemainder + minDiffUnmatched)), minDiffXO, minDiffYO);
	}
	
	
	private static BaseFont getBuiltInFont(Object fnObj, boolean isSymbolic) {
		String fn = fnObj.toString();
		BaseFont font = ((BaseFont) builtInFontData.get(fn));
		if (font == null) {
			String ppcrn = PdfParser.class.getName().replaceAll("\\.", "/");
			String afmrn = (ppcrn.substring(0, ppcrn.lastIndexOf('/')) + "/afmRes/" + fn + ".afm");
			try {
				InputStream fis = PdfParser.class.getClassLoader().getResourceAsStream(afmrn);
				BufferedReader fr = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
				font = readBaseFont(fr);
				fr.close();
				builtInFontData.put(fn, font);
			}
			catch (Exception e) {
				System.out.println("PdfParser: could not load built-in font '" + fn + "' from resource '" + afmrn + "'");
				e.printStackTrace(System.out);
				
//				String baseName = null;
//				if (fn.startsWith("Times"))
//					baseName = "Times";
//				else if (fn.startsWith("Courier"))
//					baseName = "Courier";
//				else if (fn.startsWith("Helvetica"))
//					baseName = "Helvetica";
//				else if (fn.startsWith("Arial"))
//					baseName = "Helvetica";
				
				String baseName = getFallbackFontName(fn, isSymbolic);
				if (baseName != null) {
					String modifier = "-";
//					if (!isSymbolic) {
						if (fn.indexOf("Bold") != -1)
							modifier += "Bold";
						if (fn.indexOf("Italic") != -1)
							modifier += ("Times".equals(baseName) ? "Italic" : "Oblique");
//					}
					
					String substituteFontName = baseName;
					if (modifier.length() > 1)
						substituteFontName += modifier;
					else if ("Times".equals(baseName))
						substituteFontName += "-Roman";
					
					System.out.println("PdfParser: falling back to '" + substituteFontName + "'");
					return getBuiltInFont(substituteFontName, isSymbolic);
				}
			}
		}
		return font;
	}
	
	/**
	 * Retrieve the name of the base font to use in case some font is not
	 * available. This method maps serif fonts to 'Times', sans-serif fonts to
	 * 'Helvetica', and monospaced fonts to 'Courier'. These three fonts are
	 * manadtory to support for all PDF applications.
	 * @param fontName the name of the font to substitute
	 * @param isSymbolic do we need a fallback for a symbolic font?
	 * @return the substitute font name
	 */
	public static String getFallbackFontName(String fontName, boolean isSymbolic) {
		if (fallbackFontNameMappings == null) {
			String ppcrn = PdfParser.class.getName().replaceAll("\\.", "/");
			String fmrn = (ppcrn.substring(0, ppcrn.lastIndexOf('/')) + "/afmRes/FallbackMappings.txt");
			try {
				TreeMap fms = new TreeMap(new Comparator() {
					public int compare(Object o1, Object o2) {
						String s1 = ((String) o1);
						String s2 = ((String) o2);
						return ((s1.length() == s2.length()) ? s1.compareTo(s2) : (s2.length() - s1.length()));
					}
				});
				InputStream fmis = PdfParser.class.getClassLoader().getResourceAsStream(fmrn);
				BufferedReader fmr = new BufferedReader(new InputStreamReader(fmis, "UTF-8"));
				String fm;
				String f = null;
				while ((fm = fmr.readLine()) != null) {
					fm = fm.trim();
					if (fm.length() == 0)
						continue;
					if (fm.startsWith("//"))
						continue;
					
					if (fm.startsWith("#"))
						f = fm.substring(1).trim();
					else if (f != null) {
						fm = fm.replaceAll("[^A-Za-z0-9]", "");
						fms.put(fm, f);
					}
				}
				fmr.close();
				fallbackFontNameMappings = fms;
			}
			catch (Exception e) {
				System.out.println("PdfParser: could not load fallback font mappings.");
				e.printStackTrace(System.out);
//				return (isSymbolic ? "ZapfDingbats" : "Times"); // last resort
				return "Times"; // last resort
			}
		}
		
		//	normalize font name
		fontName = fontName.replaceAll("[^A-Za-z0-9]", "");
		
		//	do lookup
		for (Iterator fmit = fallbackFontNameMappings.keySet().iterator(); fmit.hasNext();) {
			String fm = ((String) fmit.next());
			if (fm.startsWith(fontName))
				return ((String) fallbackFontNameMappings.get(fm));
			if (fontName.startsWith(fm))
				return ((String) fallbackFontNameMappings.get(fm));
		}
		
		//	chunk font name
		String[] fontNameParts = fontName.split("\\+");
		if (fontNameParts.length == 1)
//			return (isSymbolic ? "ZapfDingbats" : "Times"); // last resort
			return "Times"; // last resort
		
		//	look up individual chunks
		for (int p = 0; p < fontNameParts.length; p++) {
			String fm = getFallbackFontName(fontNameParts[p], isSymbolic);
			if (fm != null)
				return fm;
		}
		
		//	last resort
		return "Times";
	}
	private static TreeMap fallbackFontNameMappings = null;
	
	private static HashMap builtInFontData = new HashMap();
	private static BaseFont readBaseFont(BufferedReader br) throws IOException {
		String line;
		Hashtable fd = new Hashtable();
		BaseFont font = null;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.startsWith("Comment"))
				continue;
			if (line.startsWith("Notice"))
				continue;
			if (line.startsWith("Version"))
				continue;
			
			for (int p = 0; p < afmFontParams.length; p++) {
				String value = readAfmFontParam(afmFontParams[p], line);
				if (value != null) {
					Object pValue = parseAfmFontParameter(afmFontParams[p], value);
					if (pValue != null) {
						fd.put(afmFontParams[p], pValue);
						fd.put(afmFontParamMappings.getProperty(afmFontParams[p], afmFontParams[p]), pValue);
					}
					break;
				}
			}
			
			if (line.startsWith("StartCharMetrics")) {
				Object fnObj = fd.get("FontName");
				if (fnObj == null)
					return null;
				Object eObj = fd.get("EncodingScheme");
				Object mcwObj = ((fd == null) ? null : fd.get("CharWidth"));
				font = new BaseFont(new Hashtable(), fd, ((mcwObj instanceof Number) ? ((Number) mcwObj).floatValue() : 0), fnObj.toString(), ((eObj == null) ? null : eObj.toString()));
				readCharMetrics(br, font);
			}
			
			if (line.startsWith("StartKernData")) {
				readKernData(br, font);
			}
		}
		return font;
	}
	
	private static String[] afmFontParams = {
		
		//	string valued
		"FontName",
		"EncodingScheme",
		"CharacterSet",
		
		//	boolean valued
		"IsFixedPitch",
		
		//	number valued
		"ItalicAngle",
		"UnderlinePosition",
		"UnderlineThickness",
		"CapHeight",
		"XHeight",
		"Ascender",
		"Descender",
		
		//	two numbers
		"CharWidth",
		
		//	four numbers
		"FontBBox",
	};
	
	private static Properties afmFontParamMappings = new Properties();
	static {
		afmFontParamMappings.setProperty("EncodingScheme", "Encoding");
		afmFontParamMappings.setProperty("CharWidth", "MissingWidth");
		afmFontParamMappings.setProperty("Ascender", "Ascent");
		afmFontParamMappings.setProperty("Descender", "Descent");
	}
	
	private static String readAfmFontParam(String param, String line) {
		return (line.startsWith(param) ? line.substring(param.length()).trim() : null);
	}
	
	private static Object parseAfmFontParameter(String param, String value) {
		
		//	string valued
		if ("FontName".equals(param))
			return value;
		if ("EncodingScheme".equals(param))
			return value;
		if ("CharacterSet".equals(param))
			return value;
		
		//	boolean valued
		if ("IsFixedPitch".equals(param))
			return new Boolean(value);
		
		//	number valued
		if ("ItalicAngle".equals(param))
			return new Float(value);
		if ("UnderlinePosition".equals(param))
			return new Float(value);
		if ("UnderlineThickness".equals(param))
			return new Float(value);
		if ("CapHeight".equals(param))
			return new Float(value);
		if ("XHeight".equals(param))
			return new Float(value);
		if ("Ascender".equals(param))
			return new Float(value);
		if ("Descender".equals(param))
			return new Float(value);
		
		//	two numbers
		if ("CharWidth".equals(param)) {
			int split = value.indexOf(' ');
			if (split == -1)
				return new Float(value);
			else return new Float(value.substring(0, split));
			//	this only reads the X direction, but that should do for the Latin fonts we handle for now
		}
//		
//		//	four numbers
//		"FontBBox",
		
		//	unknown
		return null;
	}
	
	private static void readCharMetrics(BufferedReader br, BaseFont font) throws IOException {
		String line;
		int fc = -1;
		int lc = -1;
		ArrayList encodedCharWidths = new ArrayList();
		while ((line = br.readLine()) != null) {
			line = line.trim();
			
			if ("EndCharMetrics".equals(line))
				break;
			
			String[] fd = line.split("\\s*\\;\\s*");
			int cc = -1;
			int cw = -1;
			String cn = null;
			for (int d = 0; d < fd.length; d++) {
				if (fd[d].startsWith("C "))
					cc = Integer.parseInt(fd[d].substring("C ".length()).trim());
				else if (fd[d].startsWith("WX "))
					cw = Integer.parseInt(fd[d].substring("WX ".length()).trim());
				else if (fd[d].startsWith("N "))
					cn = fd[d].substring("N ".length()).trim();
			}
			
			if (cw == -1)
				continue;
			
			if (cn != null) {
				char ch = StringUtils.getCharForName(cn);
				if (ch != 0)
					font.setNamedCharWidth(ch, cw);
			}
			
			if (cc == -1)
				continue;
			
			while ((fc != -1) && (lc < (cc-1))) {
				encodedCharWidths.add(new Float(0));
				lc++;
			}
			if (fc == -1)
				fc = cc;
			lc = cc;
			encodedCharWidths.add(new Float(cw));
		}
		
		if (fc != -1) {
			float[] cws = new float[encodedCharWidths.size()];
			for (int c = 0; c < encodedCharWidths.size(); c++)
				cws[c] = ((Float) encodedCharWidths.get(c)).floatValue();
			font.setCharWidths(fc, lc, cws);
		}
	}
	
	private static void readKernData(BufferedReader br, BaseFont font) throws IOException {
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if ("EndKernData".equals(line))
				break;
			//	TODO actually read kern data
		}
	}
	
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		Font fontSerif = new Font("Serif", Font.PLAIN, 1);
		System.out.println("" + fontSerif.getFontName());
		
		String[] fns = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (int f = 0; f < fns.length; f++)
			System.out.println("" + fns[f]);
		if (true)
			return;
		
		int width = 36;
		int height = 54;
		Font font = new Font("Times New Roman", Font.PLAIN, 1);
//		Font font = new Font("Arial Unicode MS", Font.PLAIN, 1);
		for (int c = 400; c < Character.MAX_VALUE; c++) {
			char ch = ((char) c);
			if (!Character.isLetterOrDigit(ch))
				continue;
			CharImage chImg = createImageChar(width, height, 0, ch, font, height, null);
			if (chImg == null)
				continue;
		}
	}
}
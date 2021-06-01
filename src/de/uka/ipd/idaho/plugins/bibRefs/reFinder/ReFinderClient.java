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
 *     * Neither the name of the Universitaet Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.plugins.bibRefs.reFinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.easyIO.streams.CharSequenceReader;
import de.uka.ipd.idaho.easyIO.util.JsonParser;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefConstants;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.stringUtils.StringUtils;


/**
 * Client object for querying the ReFinder bibliographic data search portal.
 * 
 * @author sautter
 */
public class ReFinderClient implements BibRefConstants {
	private String rfrBaseUrl;
	
	/** Constructor using default URL 'http://refinder.org'
	 */
	public ReFinderClient() {
		this("http://refinder.org");
	}
	
	/** Constructor using custom URL
	 * @param rfrBaseUrl the ReFinder API base URL
	 */
	public ReFinderClient(String rfrBaseUrl) {
		this.rfrBaseUrl = (rfrBaseUrl + (rfrBaseUrl.endsWith("/") ? "" : "/"));
	}
	
	/**
	 * Run a simple full text search on ReFinder. This search might not query
	 * all sources underlying sources, depending on their API restrictions. The
	 * returned list of parsed bibliographic references may contain duplicates,
	 * as ReFinder does not reconcile results coming in from more than one of
	 * its underlying sources. The name of the underlying data source come in
	 * the <code>refSource</code> attribute of the result reference data sets.
	 * @param query the query
	 * @return the list of matching references
	 * @throws IOException
	 */
	public RefData[] find(String query) throws IOException {
		return this.find(null, query);
	}
	
	/**
	 * Run a simple full text search on ReFinder. This search might not query
	 * all sources underlying sources, depending on their API restrictions. The
	 * returned list of parsed bibliographic references may contain duplicates,
	 * as ReFinder does not reconcile results coming in from more than one of
	 * its underlying sources. The name of the underlying data source come in
	 * the <code>refSource</code> attribute of the result reference data sets.
	 * @param sourceName the name of the data source to search
	 * @param query the query
	 * @return the list of matching references
	 * @throws IOException
	 */
	public RefData[] find(String sourceName, String query) throws IOException {
		if (query.length() == 0)
			return null;
		return this.parseResults(new URL(this.rfrBaseUrl + "find?search=simple" + ((sourceName == null) ? "" : ("&db=" + URLEncoder.encode(sourceName.trim(), "UTF-8"))) + "&text=" + URLEncoder.encode(query, "UTF-8")));
	}
	
	/**
	 * Run an advanced search on ReFinder. If neither of the arguments is set
	 * to a meaningful value, this method returns null. The returned list of
	 * parsed bibliographic references may contain duplicates, as ReFinder does
	 * not reconcile results coming in from more than one of its underlying
	 * sources. The name of the underlying data source come in the
	 * <code>refSource</code> attribute of the result reference data sets.
	 * @param title the title to search
	 * @param author the author to search
	 * @param year the year to search (use -1 to leave empty)
	 * @param origin the journal name or publisher/location to search
	 * @return the list of matching references
	 * @throws IOException
	 */
	public RefData[] find(String title, String author, int year, String origin) throws IOException {
		return this.find(null, title, author, year, origin);
	}
	
	/**
	 * Run an advanced search on ReFinder. If neither of the arguments is set
	 * to a meaningful value, this method returns null. The returned list of
	 * parsed bibliographic references may contain duplicates, as ReFinder does
	 * not reconcile results coming in from more than one of its underlying
	 * sources. The name of the underlying data source come in the
	 * <code>refSource</code> attribute of the result reference data sets.
	 * @param sourceName the name of the data source to search (use space to
	 *            separate multiple sources)
	 * @param title the title to search
	 * @param author the author to search
	 * @param year the year to search (use -1 to leave empty)
	 * @param origin the journal name or publisher/location to search
	 * @return the list of matching references
	 * @throws IOException
	 */
	public RefData[] find(String sourceName, String title, String author, int year, String origin) throws IOException {
		StringBuffer query = new StringBuffer();
		if ((title != null) && (title.trim().length() != 0))
			query.append("&title=" + URLEncoder.encode(title.trim(), "UTF-8"));
		if ((author != null) && (author.trim().length() != 0))
			query.append("&author=" + URLEncoder.encode(author.trim(), "UTF-8"));
		if (year >= 0)
			query.append("&year=" + year);
		if ((origin != null) && (origin.trim().length() != 0))
			query.append("&origin=" + URLEncoder.encode(origin.trim(), "UTF-8"));
		if (query.length() == 0)
			return null;
		System.out.println("URL is " + this.rfrBaseUrl + "find?search=advanced" + getSourceParameter(sourceName) + query.toString());
		return this.parseResults(new URL(this.rfrBaseUrl + "find?search=advanced" + getSourceParameter(sourceName) + query.toString()));
	}
	
	private static String getSourceParameter(String sourceName) throws IOException {
		if (sourceName == null)
			return "";
		sourceName = sourceName.trim();
		if (sourceName.length() == 0)
			return "";
		String[] sourceNames = sourceName.split("\\s+");
		StringBuffer sourceParameters = new StringBuffer();
		for (int s = 0; s < sourceNames.length; s++)
			sourceParameters.append("&db=" + URLEncoder.encode(sourceNames[s].trim(), "UTF-8"));
		return sourceParameters.toString();
	}
	
	private RefData[] parseResults(URL url) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
		StringBuffer sb = new StringBuffer();
		for (int ch, lch = 0; (ch = br.read()) != -1;) {
			if (lch == ']') {
				if (ch < 33)
					continue;
				if (ch == '[') {
					sb.replace((sb.length()-1), sb.length(), ",");
					lch = ',';
					continue;
				}
			}
			sb.append((char) ch);
			lch = ch;
		}
		System.out.println(sb);
		List resList = ((List) JsonParser.parseJson(new CharSequenceReader(sb)));
		List resRefDats = new ArrayList();
		for (int r = 0; r < resList.size(); r++) {
			Map res = JsonParser.getObject(resList, r);
			if (res == null)
				continue;
			
			Boolean isParsed = JsonParser.getBoolean(res, "isParsed");
			if ((isParsed == null) || !isParsed.booleanValue())
				continue;
			String type = JsonParser.getString(res, "type");
			
			String title = findString(res.get("title"));
			if (title == null)
				continue;
			List authors = findPersonNames(res.get("authors"));
			if ((authors == null) || authors.isEmpty())
				continue;
			String year = findNumber(res.get("year"));
			if ((year == null) || !year.matches("[12][0-9]{3}"))
				continue;
			
			RefData rd = new RefData();
			for (int a = 0; a < authors.size(); a++) {
				List author = JsonParser.getArray(authors, a);
				if ((author == null) || author.isEmpty())
					continue;
				String aln = JsonParser.getString(author, ((author.size() == 1) ? 0 : 1));
				if (aln == null)
					continue;
				String afn = ((author.size() < 2) ? null : JsonParser.getString(author, 0));
				rd.addAttribute(AUTHOR_ANNOTATION_TYPE, (aln + ((afn == null) ? "" : (", " + afn))));
			}
			rd.setAttribute(YEAR_ANNOTATION_TYPE, year);
			rd.setAttribute(TITLE_ANNOTATION_TYPE, title);
			resRefDats.add(rd);
			
			this.setAttribute(res, "volume", VOLUME_DESIGNATOR_ANNOTATION_TYPE, true, rd);
			this.setAttribute(res, "issue", ISSUE_DESIGNATOR_ANNOTATION_TYPE, true, rd);
			if ((type != null) && type.startsWith("journal"))
				this.setAttribute(res, "publishedIn", JOURNAL_NAME_ANNOTATION_TYPE, false, rd);
			else if ((type == null) && (rd.hasAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE) || rd.hasAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE)))
				this.setAttribute(res, "publishedIn", JOURNAL_NAME_ANNOTATION_TYPE, false, rd);
			else this.setAttribute(res, "publishedIn", JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE, false, rd);
			this.setAttribute(res, "publicationDate", PUBLICATION_DATE_ANNOTATION_TYPE, false, rd);
			String fpg = trimLeadingZeros(findNumber(res.get("spage")));
			if (fpg != null) {
				String lpg = trimLeadingZeros(findNumber(res.get("epage")));
				rd.setAttribute(PAGINATION_ANNOTATION_TYPE, (fpg + ((lpg == null) ? "" : ("-" + lpg))));
			}
			
			this.setAttribute(res, "href", PUBLICATION_URL_ANNOTATION_TYPE, false, rd);
			this.setAttribute(res, "doi", "ID-DOI", false, rd);
			
			String source = JsonParser.getString(res, "source");
			if (source == null)
				source = "ReFinder";
			else source = (source + " via ReFinder");
			rd.setAttribute(BibRefDataSource.REFERENCE_DATA_SOURCE_ATTRIBUTE, source);
			this.setAttribute(res, "score", BibRefDataSource.REFERENCE_MATCH_SCORE_ATTRIBUTE, false, rd);
			
			BibRefUtils.classify(rd);
		}
		return ((RefData[]) resRefDats.toArray(new RefData[resRefDats.size()]));
	}
	
	private void setAttribute(Map res, String rAttribute, String rdAttribute, boolean isNumber, RefData rd) {
		String value = (isNumber ? findNumber(res.get(rAttribute)) : findString(res.get(rAttribute)));
		if (value == null)
			return;
		value = value.trim();
		if (value.length() == 0)
			return;
		if (VOLUME_DESIGNATOR_ANNOTATION_TYPE.equals(rdAttribute) || ISSUE_DESIGNATOR_ANNOTATION_TYPE.equals(rdAttribute) || NUMERO_DESIGNATOR_ANNOTATION_TYPE.equals(rdAttribute)) {
			value = BibRefUtils.sanitizeAttributeValue(rdAttribute, value);
			if (value == null)
				return;
		}
		else if (PUBLICATION_DATE_ANNOTATION_TYPE.equals(rdAttribute)) {
			value = StringUtils.normalizeString(value);
			String[] valueParts = value.split("\\s*\\-+\\s*");
			String valueRaw = null;
			if (valueParts.length == 3) {}
			else if (valueParts.length == 2) {
				String[] cValueParts = {
					valueParts[0],
					trimLeadingZeros(valueParts[1]),
					null
				};
				if ("1".equals(cValueParts[1]) || "3".equals(cValueParts[1]) || "5".equals(cValueParts[1]) || "7".equals(cValueParts[1]) || "8".equals(cValueParts[1]) || "10".equals(cValueParts[1]) || "12".equals(cValueParts[1]))
					cValueParts[2] = "31";
				else if ("4".equals(cValueParts[1]) || "6".equals(cValueParts[1]) || "9".equals(cValueParts[1]) || "11".equals(cValueParts[1]))
					cValueParts[2] = "30";
				else try {
					int year = Integer.parseInt(cValueParts[0]);
					if ((year % 4) != 0) // leap years are multiples of four
						cValueParts[2] = "28";
					else if ((year % 100) != 0) // leap years are only skipped at turns of centuries ...
						cValueParts[2] = "29";
					else if ((year % 400) != 0) // ... that aren't a multiple of 400
						cValueParts[2] = "28";
					else cValueParts[2] = "29";
				}
				catch (NumberFormatException nfe) {
					return;
				}
				valueParts = cValueParts;
				valueRaw = value;
			}
			else if (valueParts.length == 1) {
				String[] cValueParts = {
					valueParts[0],
					"12",
					"31"
				};
				valueParts = cValueParts;
				valueRaw = value;
			}
			else return; // whatever this could possibly be ...
			String year = valueParts[0];
			if (!year.matches("[12][0-9]{3}"))
				return;
			String month = trimLeadingZeros(valueParts[1]);
			if (!month.matches("((1[0-2])|[1-9])"))
				return;
			String day = trimLeadingZeros(valueParts[2]);
			if (!day.matches("(([12][0-9])|(3[0-1])|[1-9])"))
				return;
			value = (year + "-" + ((month.length() < 2) ? "0" : "") + month + "-" + ((day.length() < 2) ? "0" : "") + day);
			if (valueRaw != null)
				rd.setAttribute((PUBLICATION_DATE_ANNOTATION_TYPE + "Raw"), valueRaw);
		}
		if (value.length() == 0)
			return;
		if (JOURNAL_NAME_ANNOTATION_TYPE.equals(rdAttribute) && value.endsWith(")") && (value.lastIndexOf("(") != -1)) {
			String sj = value.substring(value.lastIndexOf("(") + "(".length()).trim();
			sj = sj.substring(0, (sj.length() - ")".length())).trim();
			rd.setAttribute(SERIES_IN_JOURNAL_ANNOTATION_TYPE, sj);
			rd.setAttribute(rdAttribute, value.substring(0, value.lastIndexOf("(")).trim());
		}
		else rd.setAttribute(rdAttribute, value);
	}
	
	private static String trimLeadingZeros(String str) {
		if (str == null)
			return str;
		while (str.startsWith("0"))
			str = str.substring("0".length());
		return str;
	}
	
	public static void main(String[] args) throws Exception {
//		System.out.println(Long.toHexString(-1L));
//		System.out.println(Long.toHexString((-1L & 0xFFFFFFFF)));
//		System.out.println(Long.toHexString(((-1L >>> 32) & 0xFFFFFFFF)));
//		System.out.println(Integer.toString((((int) (-1L >>> 48)) & 0xFFFF), 16));
//		System.out.println(Integer.toString((((int) (-1L >>> 32)) & 0xFFFF), 16));
//		System.out.println(Integer.toString((((int) (-1L >>> 16)) & 0xFFFF), 16));
//		System.out.println(Integer.toString((((int) (-1L >>> 0)) & 0xFFFF), 16));
//		System.out.println(Long.toString((((int) (-1L >>> 32)) & 0xFFFFFFFFL), 16));
//		System.out.println(Long.toString((((int) (-1L >>> 0)) & 0xFFFFFFFFL), 16));
//		char[] hex = new char[16];
//		for (int h = 0; h < hex.length; h++) {
//			int ch = ((int) ((-1L >>> ((16 * 4) - ((h + 1) * 4))) & 0x000000000000000FL));
//			hex[h] = ((char) ((ch < 10) ? ('0' + ch) : ('A' + (ch - 10))));
//		}
//		System.out.println(Arrays.toString(hex));
		ReFinderClient rfrc = new ReFinderClient("http://refinder.org");
//		RefData[] rds = rfrc.find("10.3897/zookeys.491.8553");
		//db=crossref&search=advanced&author=Vozenin&year=2011&origin=Geodiversitas
//		RefData[] rds = rfrc.find("10.5252/g2011n1a1");
		RefData[] rds = rfrc.find(
				"crossref datacite",
//				"Description of Two Species of Hoploscaphites (Ammonoidea: Ancyloceratina) from the Upper Cretaceous (Lower Maastrichtian) of the U.S. Western Interior",
//				"Landman",
//				2019,
//				"Bulletin of the American Museum of Natural History"
				null,
				"Vozenin",
				2011,
				"Geodiversitas"
			);
		for (int r = 0; r < rds.length; r++) {
			BibRefUtils.classify(rds[r]);
			System.out.println(rds[r].getAttribute(PUBLICATION_TYPE_ATTRIBUTE) + " from " + rds[r].getAttribute("refSource"));
			System.out.println(rds[r].toXML());
			System.out.println(BibRefUtils.toRefString(rds[r]).trim());
		}
	}
	
	private static abstract class RefAttributeCollector {
		abstract boolean accept(Object data);
		abstract boolean attributeComplete();
	}
	private static class StringAttributeCollector extends RefAttributeCollector {
		private Pattern pattern;
		String value = null;
		StringAttributeCollector(String pattern) {
			this.pattern = ((pattern == null) ? null : Pattern.compile(pattern));
		}
		boolean accept(Object data) {
			if (!(data instanceof CharSequence))
				return false;
			if ((this.pattern == null) || this.pattern.matcher((CharSequence) data).matches()) {
				this.value = ((CharSequence) data).toString();
				return true;
			}
			else return false;
		}
		boolean attributeComplete() {
			return (this.value != null);
		}
	}
	private static class NumberAttributeCollector extends StringAttributeCollector {
		NumberAttributeCollector() {
			super("[0-9]+");
		}
		boolean accept(Object data) {
			if (super.accept(data)) {
				this.value = trimLeadingZeros(this.value);
				return true;
			}
			else if (data instanceof Number) {
				this.value = ("" + ((Number) data).intValue());
				return true;
			}
			else return false;
		}
	}
	private static class PersonNameCollector extends RefAttributeCollector {
		ArrayList value = null;
		boolean accept(Object data) {
			if (!(data instanceof List))
				return false;
			List array = ((List) data);
			ArrayList value = new ArrayList(array.size());
			for (int e = 0; e < array.size(); e++) {
				Object entry = array.get(e);
				StringAttributeCollector sac = new StringAttributeCollector(null);
				collectRefAttribute(entry, sac);
				if (sac.attributeComplete())
					value.add(sac.value);
				else return false;
			}
			if (value.isEmpty())
				return false;
			this.value = value;
			return true;
		}
		boolean attributeComplete() {
			return (this.value != null);
		}
	}
	private static class PersonListCollector extends RefAttributeCollector {
		ArrayList personNames;
		boolean accept(Object data) {
			if (!(data instanceof List))
				return false;
			List array = ((List) data);
			for (int e = 0; e < array.size(); e++) {
				Object entry = array.get(e);
				PersonNameCollector pnc = new PersonNameCollector();
				collectRefAttribute(entry, pnc);
				if (pnc.attributeComplete()) {
					if (this.personNames == null)
						this.personNames = new ArrayList(4);
					this.personNames.add(pnc.value);
				}
			}
			return true;
		}
		boolean attributeComplete() {
			return false; // we want to keep looking through the end
		}
	}
	private static void collectRefAttribute(Object data, RefAttributeCollector rac) {
		if (rac.accept(data))
			return;
		if (data instanceof List) {
			List array = ((List) data);
			for (int e = 0; e < array.size(); e++) {
				Object entry = array.get(e);
				collectRefAttribute(entry, rac);
				if (rac.attributeComplete())
					return;
			}
		}
		if (data instanceof Map) {
			Map object = ((Map) data);
			for (Iterator pnit = object.keySet().iterator(); pnit.hasNext();) {
				Object pv = object.get(pnit.next());
				collectRefAttribute(pv, rac);
				if (rac.attributeComplete())
					return;
			}
		}
	}
	private static String findString(Object data) {
		StringAttributeCollector sac = new StringAttributeCollector(null);
		collectRefAttribute(data, sac);
		return ((sac.value == null) ? null : sac.value.replaceAll("\\s+", " ").trim());
	}
	private static String findNumber(Object data) {
		NumberAttributeCollector nac = new NumberAttributeCollector();
		collectRefAttribute(data, nac);
		return nac.value;
	}
	private static List findPersonNames(Object data) {
		PersonListCollector plc = new PersonListCollector();
		collectRefAttribute(data, plc);
		return plc.personNames;
	}
}
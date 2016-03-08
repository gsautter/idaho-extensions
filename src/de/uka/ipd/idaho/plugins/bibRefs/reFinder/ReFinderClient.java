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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.plugins.bibRefs.reFinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.uka.ipd.idaho.easyIO.streams.CharSequenceReader;
import de.uka.ipd.idaho.easyIO.util.JsonParser;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefConstants;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;


/**
 * Client object for querying the ReFinder bibliographic data search portal.
 * 
 * @author sautter
 */
public class ReFinderClient implements BibRefConstants {
	public static final String REFERENCE_DATA_SOURCE_ATTRIBUTE = "refSource";
	public static final String REFERENCE_MATCH_SCORE_ATTRIBUTE = "refMatchScore";
	
	private String rfrBaseUrl;
	
	/** Constructor using default URL 'http://refinder.org/find'
	 */
	public ReFinderClient() {
		this("http://refinder.org/find");
	}
	
	/** Constructor using custom URL
	 * @param rfrBaseUrl the ReFinder API base URL
	 */
	public ReFinderClient(String rfrBaseUrl) {
		this.rfrBaseUrl = rfrBaseUrl;
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
		if (query.length() == 0)
			return null;
		else return this.parseResults(new URL(this.rfrBaseUrl + "?search=simple&text=" + URLEncoder.encode(query, "UTF-8")));
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
		else return this.parseResults(new URL(this.rfrBaseUrl + "?search=advanced" + query.toString()));
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
			
			List authors = JsonParser.getArray(res, "authors");
			if ((authors == null) || authors.isEmpty())
				continue;
			String year = JsonParser.getString(res, "year");
			if ((year == null) || !year.matches("[12][0-9]{3}"))
				continue;
			String title = JsonParser.getString(res, "title");
			if (title == null)
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
			
			if ((type != null) && "journal article".equalsIgnoreCase(type))
				this.setAttribute(res, "publishedIn", JOURNAL_NAME_ANNOTATION_TYPE, rd);
			else this.setAttribute(res, "publishedIn", JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE, rd);
			this.setAttribute(res, "volume", VOLUME_DESIGNATOR_ANNOTATION_TYPE, rd);
			this.setAttribute(res, "issue", ISSUE_DESIGNATOR_ANNOTATION_TYPE, rd);
			String fpg = JsonParser.getString(res, "spage");
			if (fpg != null) {
				String lpg = JsonParser.getString(res, "epage");
				rd.setAttribute(PAGINATION_ANNOTATION_TYPE, (fpg + ((lpg == null) ? "" : ("-" + lpg))));
			}
			
			this.setAttribute(res, "href", PUBLICATION_URL_ANNOTATION_TYPE, rd);
			this.setAttribute(res, "doi", "ID-DOI", rd);
			
			this.setAttribute(res, "source", REFERENCE_DATA_SOURCE_ATTRIBUTE, rd);
			this.setAttribute(res, "score", REFERENCE_MATCH_SCORE_ATTRIBUTE, rd);
		}
		return ((RefData[]) resRefDats.toArray(new RefData[resRefDats.size()]));
	}
	
	private void setAttribute(Map res, String rAttribute, String rdAttribute, RefData rd) {
		String value = JsonParser.getString(res, rAttribute);
		if ((value != null) && (value.length() != 0))
			rd.setAttribute(rdAttribute, value);
	}
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws Exception {
//		ReFinderClient rfrc = new ReFinderClient("http://refinder.org/find");
//		RefData[] rds = rfrc.find("10.3897/zookeys.491.8553");
//		for (int r = 0; r < rds.length; r++) {
//			BibRefUtils.classify(rds[r]);
//			System.out.println(rds[r].getAttribute(PUBLICATION_TYPE_ATTRIBUTE) + " from " + rds[r].getAttribute("refSource"));
//			System.out.println(rds[r].toXML());
//			System.out.println(BibRefUtils.toRefString(rds[r]).trim());
//		}
//	}
}
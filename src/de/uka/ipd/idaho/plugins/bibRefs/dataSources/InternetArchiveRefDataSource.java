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
package de.uka.ipd.idaho.plugins.bibRefs.dataSources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;

/**
 * Data source fetching document meta data from Internet Archive. This class
 * expects its data provider to have a file named <code>config.IA.cnfg</code>
 * available, with up to two setting: <code>searchBaseUrl</code> containing the
 * URL to send search queries to, including the search field, and
 * <code>archiveBaseUrl</code> containing the base URL where the actual data is
 * stored. <code>searchBaseUrl</code> defaults to
 * <code>http://www.archive.org/search.php?query=</code>,
 * <code>archiveBaseUrl</code> to <code>http://archive.org/download/</code>
 * 
 * @author sautter
 */
public abstract class InternetArchiveRefDataSource extends BibRefDataSource {
	
	private String searchBaseUrl = "http://www.archive.org/search.php?query=";
	
	private String archiveBaseUrl = "http://archive.org/download/";
	
	private static String pdfFileSuffix = "_bw.pdf";
	private static String metadataFileSuffix = "_meta.xml";
	
	private String iaCollectionName;
	
	/**
	 * Constructor taking the name for the data source and the one of the
	 * collection in Internet Archive. Both names must be specified as constant
	 * strings in sub class constructors, which have to be public and take no
	 * arguments to facilitate class loading. If the argument collection name is
	 * null, search will not work. However, retrieving reference data for a
	 * given ID still will.
	 * @param name the name of the data source
	 * @param iaCollectionName the name of the backing collection in Internet
	 *            Archive
	 */
	public InternetArchiveRefDataSource(String name, String iaCollectionName) {
		super(name);
		this.iaCollectionName = iaCollectionName;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource#init()
	 */
	public void init() {
		try {
			Reader setReader = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("config.IA.cnfg"), "UTF-8"));
			Settings set = Settings.loadSettings(setReader);
			this.searchBaseUrl = set.getSetting("searchBaseUrl", this.searchBaseUrl);
			this.archiveBaseUrl = set.getSetting("archiveBaseUrl", this.archiveBaseUrl);
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource#getRefData(java.lang.String)
	 */
	public RefData getRefData(String sourceDocId) throws IOException {
		
		//	check if ID might come from backing source
		if (!this.isSuitableID(sourceDocId))
			return null;
		
		//	load raw data
		URL rawDataUrl = new URL(this.archiveBaseUrl + sourceDocId + "/" + sourceDocId + metadataFileSuffix);
		InputStreamReader rawDataReader = new InputStreamReader(((HttpURLConnection) rawDataUrl.openConnection()).getInputStream(), "UTF-8");
		MutableAnnotation rawData = SgmlDocumentReader.readDocument(rawDataReader);
		
		RefData refData = new RefData();
		for (Iterator anit = resultAttributeMapping.keySet().iterator(); anit.hasNext();) {
			String attributeValueAnnotationType = ((String) anit.next());
			String attributeName = resultAttributeMapping.getProperty(attributeValueAnnotationType);
			Annotation[] attributeValues = rawData.getAnnotations(attributeValueAnnotationType);
			for (int v = 0; v < attributeValues.length; v++) {
				String attributeValue = TokenSequenceUtils.concatTokens(attributeValues[v], true, true);
				if (AUTHOR_ANNOTATION_TYPE.equals(attributeName) || EDITOR_ANNOTATION_TYPE.equals(attributeName)) {
					attributeValue = attributeValue.replaceAll("\\([^\\)]*\\)", "").trim();
					attributeValue = attributeValue.replaceAll("\\[[^\\]]*\\]", "").trim();
					attributeValue = attributeValue.replaceAll("[1-2][0-9]{3}.*", "").trim();
					while (!attributeValue.endsWith(".") && Gamta.isPunctuation(attributeValue.substring(attributeValue.length()-1)))
						attributeValue = attributeValue.substring(0, (attributeValue.length()-1)).trim();
				}
				else if (attributeName.startsWith("ID-"))
					attributeValue = attributeValue.replaceAll("\\s", "").trim();
				if (multiValueAttributes.contains(attributeName))
					refData.addAttribute(attributeName, attributeValue);
				else refData.setAttribute(attributeName, attributeValue);
			}
		}
		refData.addAttribute(PUBLICATION_URL_ANNOTATION_TYPE, (this.archiveBaseUrl + sourceDocId + "/" + sourceDocId + pdfFileSuffix));
		refData.setIdentifier(this.getName(), sourceDocId);
		this.addNonStandardAttributes(rawData, refData);
		String type = refData.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		if (type == null)
			BibRefUtils.classify(refData);
		return refData;
	}
	
	private static Properties resultAttributeMapping = new Properties();
	static {
		resultAttributeMapping.setProperty("creator", AUTHOR_ANNOTATION_TYPE);
		resultAttributeMapping.setProperty("year", YEAR_ANNOTATION_TYPE);
		resultAttributeMapping.setProperty("date", YEAR_ANNOTATION_TYPE);
		resultAttributeMapping.setProperty("title", TITLE_ANNOTATION_TYPE);
		resultAttributeMapping.setProperty("publisher", PUBLISHER_ANNOTATION_TYPE);
		resultAttributeMapping.setProperty("identifier-ark", "ID-ARK");
		//	TODO complete and adjust this as the need arises
	}
	private static HashSet multiValueAttributes = new HashSet();
	static {
		multiValueAttributes.add(AUTHOR_ANNOTATION_TYPE);
		multiValueAttributes.add(EDITOR_ANNOTATION_TYPE);
		multiValueAttributes.add(TITLE_ANNOTATION_TYPE);
		//	TODO complete this
	}
	
	/**
	 * Add attributes to a reference data set that are represented in a
	 * collection specific way. This default implementation does nothing, sub
	 * classes are welcome to overwrite it as needed.
	 * @param rawData the raw reference meta data
	 * @param refData the reference data set to extend
	 */
	protected void addNonStandardAttributes(MutableAnnotation rawData, RefData refData) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource#findRefData(java.util.Properties)
	 */
	public RefData[] findRefData(Properties searchData) throws IOException {
		
		//	check collection name
		if (this.iaCollectionName == null)
			return null;
		
		//	build query (restrict to configured collection)
		String query = (this.iaCollectionName.equals("*") ? "" : ("collection:(" + this.iaCollectionName + ")"));
		for (Iterator pit = searchData.keySet().iterator(); pit.hasNext();) {
			String searchAttribute = ((String) pit.next());
			String iaSearchAttribute = searchAttributeMapping.getProperty(searchAttribute);
			if (iaSearchAttribute == null)
				continue;
			String searchValue = searchData.getProperty(searchAttribute);
			if (query.length() != 0)
				query += " AND ";
			query += (iaSearchAttribute + ":(" + searchValue + ")");
		}
		
		//	check attributes
		if (query.indexOf(" AND ") == -1)
			return null;
		System.out.println("Query is " + query);
		
		//	search document IDs
		ArrayList iaDocIds = new ArrayList();
		String iaDocIdListUrl = this.searchBaseUrl + URLEncoder.encode(query, "UTF-8");
		do {
			iaDocIdListUrl = this.extractAiDocIds(iaDocIdListUrl, iaDocIds);
			System.out.println(" - proceeding to " + iaDocIdListUrl);
		} while (iaDocIdListUrl != null);
		
		//	fetch data
		ArrayList refDataList = new ArrayList();
		System.out.println("Found " + iaDocIds.size() + " IDs, fetching data");
		for (int d = 0; d < iaDocIds.size(); d++) {
			String iaDocId = ((String) iaDocIds.get(d));
			try {
				RefData refData = this.getRefData(iaDocId);
				if (refData != null) {
					System.out.println(" - got data for " + iaDocId);
					refDataList.add(refData);
				}
			}
			catch (IOException ioe) {
				System.out.println("Error getting MODS data for search result " + iaDocId);
				ioe.printStackTrace(System.out);
			}
		}
		return ((RefData[]) refDataList.toArray(new RefData[refDataList.size()]));
	}
	
	private static Properties searchAttributeMapping = new Properties();
	static {
		searchAttributeMapping.setProperty(AUTHOR_ANNOTATION_TYPE, "creator");
		searchAttributeMapping.setProperty(YEAR_ANNOTATION_TYPE, "year");
		searchAttributeMapping.setProperty(TITLE_ANNOTATION_TYPE, "title");
		searchAttributeMapping.setProperty(PUBLISHER_ANNOTATION_TYPE, "publisher");
		searchAttributeMapping.setProperty(JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE, "publisher");
	}
	
	private static Grammar grammar = new StandardGrammar();
	private static Parser parser = new Parser(grammar);
	
	private String extractAiDocIds(String listUrl, final List modsIdList) throws IOException {
		final URL url = new URL(listUrl);
		final String[] nextUrl = {null};
		parser.stream(url.openStream(), new TokenReceiver() {
			private String lastHref = null;
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (grammar.isTag(token) && "a".equals(grammar.getType(token))) {
					TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
					String href = tnas.getAttribute("href");
					if (href == null)
						return;
					if ("titleLink".equals(tnas.getAttribute("class"))) {
						href = href.substring(href.lastIndexOf('/') + 1);
						if (href.length() > 0) {
							System.out.println(" - found ID: " + href);
							modsIdList.add(href);
						}
					}
					else this.lastHref = href;
				}
				else if ("Next".equals(token))
					nextUrl[0] = ((this.lastHref.startsWith("/") ? (url.getProtocol() + "://" + url.getAuthority()) : "") + this.lastHref);
			}
		});
		return nextUrl[0];
	}
	
//	http://www.archive.org/search.php?query=title%3A%28Ameisen%20aus%20Rhodesia%29%20AND%20creator%3A%28A%20Forel%29%20AND%20year%3A%281913%29
//	creator:(Forel) AND collection:(biodiversity)
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		System.getProperties().put("proxySet", "true");
		System.getProperties().put("proxyHost", "proxy.rz.uni-karlsruhe.de");
		System.getProperties().put("proxyPort", "3128");
		
		BibRefDataSource bmds = new InternetArchiveRefDataSource("HNS", "ant_texts") {
			public boolean isSuitableID(String docId) {
				return docId.matches("ants\\_[0-9]++");
			}
			//	salvage journal name, volume, and pagination from 'notes' element
			protected void addNonStandardAttributes(MutableAnnotation rawData, RefData refData) {
				Annotation[] notes = rawData.getAnnotations("notes");
				if (notes.length == 0)
					return;
				int journalEnd = notes[0].size();
				Annotation[] pagination = Gamta.extractAllMatches(notes[0], "Page(s)?\\:\\s*[0-9]+(\\s*\\-+\\s*[0-9]+)?");
				if (pagination.length != 0) {
					refData.setAttribute(PAGINATION_ANNOTATION_TYPE, TokenSequenceUtils.concatTokens(pagination[0], 2, (pagination[0].size()-2), true, true));
					journalEnd = Math.min(journalEnd, pagination[0].getStartIndex());
				}
				Annotation[] volume = Gamta.extractAllMatches(notes[0], "Volume\\:\\s*[0-9]+");
				if (volume.length != 0) {
					refData.setAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE, volume[0].lastValue());
					journalEnd = Math.min(journalEnd, volume[0].getStartIndex());
				}
				Annotation[] issue = Gamta.extractAllMatches(notes[0], "Issue\\:\\s*[0-9]+");
				if (issue.length != 0) {
					refData.setAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE, issue[0].lastValue());
					journalEnd = Math.min(journalEnd, issue[0].getStartIndex());
				}
				Annotation[] numero = Gamta.extractAllMatches(notes[0], "(Numero|Number|Nr\\.)\\:\\s*[0-9]+");
				if (numero.length != 0) {
					refData.setAttribute(NUMERO_DESIGNATOR_ANNOTATION_TYPE, numero[0].lastValue());
					journalEnd = Math.min(journalEnd, numero[0].getStartIndex());
				}
				if (journalEnd != 0) {
					String journal = TokenSequenceUtils.concatTokens(notes[0], 0, journalEnd, true, true);
					while ((journal.length() != 0) && Gamta.isPunctuation(journal.substring(journal.length()-1)))
						journal = journal.substring(0, (journal.length()-1)).trim();
					if (journal.length() != 0)
						refData.setAttribute(JOURNAL_NAME_ANNOTATION_TYPE, journal);
				}
			}
		};
		
//		BibRefDataSource bmds = new InternetArchiveRefDataSource("BHL", "biodiversity") {
//			public boolean isSuitableID(String docId) {
//				return docId.matches("[a-z0-9]++");
//			}
//		};
		
		Properties searchData = new Properties();
		searchData.setProperty(AUTHOR_ANNOTATION_TYPE, "Wheeler");
		searchData.setProperty(YEAR_ANNOTATION_TYPE, "1915");
		
		RefData[] rds = bmds.findRefData(searchData);
		for (int d = 0; d < rds.length; d++) {
			System.out.println();
			System.out.println(rds[d].getAttribute(TITLE_ANNOTATION_TYPE));
			System.out.println(BibRefUtils.toModsXML(rds[d]));
		}
	}
}

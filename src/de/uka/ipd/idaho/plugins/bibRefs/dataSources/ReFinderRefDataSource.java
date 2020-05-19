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
package de.uka.ipd.idaho.plugins.bibRefs.dataSources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.plugins.bibRefs.reFinder.ReFinderClient;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * Data source fetching document meta data from a ReFinder node. If the URL of
 * the ReFinder node to use is not specified via the constructor, this class
 * expects its data provider to have a file named <code>config.ReFinder.cnfg</code>
 * available, with a setting named <code>reFinderNodeUrl</code> containing the
 * URL of the ReFinder node to connect to.
 * 
 * @author sautter
 */
public class ReFinderRefDataSource extends BibRefDataSource {
	private ReFinderClient rfr;
	
	/** Constructor
	 */
	public ReFinderRefDataSource() {
		this(null);
	}
	
	/** Constructor
	 * @param reFinderUrl the URL of the ReFinder node to use
	 */
	public ReFinderRefDataSource(String reFinderUrl) {
		super("ReFinder");
		if (reFinderUrl != null)
			this.rfr = new ReFinderClient(reFinderUrl);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource#init()
	 */
	public void init() {
		if (this.rfr == null) try {
			Reader setReader = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("config.ReFinder.cnfg"), "UTF-8"));
			Settings set = Settings.loadSettings(setReader);
			String reFinderUrl = set.getSetting("reFinderUrl");
			if (reFinderUrl == null)
				throw new RuntimeException("URL of ReFinder missing.");
			this.rfr = new ReFinderClient(reFinderUrl);
		}
		catch (IOException ioe) {
			throw new RuntimeException("Could not load URL of ReFinder.");
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource#isSuitableID(java.lang.String)
	 */
	public boolean isSuitableID(String docId) {
		return (this.normalizeDoi(docId) != null);
	}
	
	private String normalizeDoi(String doi) {
		if (doi == null)
			return null;
		if (doi.indexOf(" ") != -1)
			doi = doi.replaceAll("\\s", "");
		if (doi.indexOf("doi.org/10.") != -1)
			doi = doi.substring(doi.indexOf("doi.org/10.") + "doi.org/".length());
		return (doi.matches("10\\.[1-9][0-9]+\\/.*") ? doi : null);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource#getRefData(java.lang.String)
	 */
	public RefData getRefData(String docDoi) throws IOException {
		RefData[] rds = this.rfr.find(this.normalizeDoi(docDoi));
		if (rds.length == 0)
			return null;
		return this.aggregateDuplicates(new ArrayList(Arrays.asList(rds)));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource#findRefData(java.util.Properties)
	 */
	public RefData[] findRefData(Properties searchData) throws IOException {
		
		//	get query data requiring special treatment
		String extIdType = null;
		String extId = null;
		for (Iterator fit = searchData.keySet().iterator(); fit.hasNext();) {
			String field = ((String) fit.next());
			if (!field.startsWith("ID-"))
				continue;
			extIdType = field.substring("ID-".length());
			extId = searchData.getProperty(field);
		}
		if ((extId != null) && "DOI".equals(extIdType)) {
			RefData[] rds = {this.getRefData(extId)};
			return rds;
		}
		
		//	get year
		int year = -1;
		try {
			year = Integer.parseInt(searchData.getProperty(YEAR_ANNOTATION_TYPE));
		} catch (Exception e) {}
		
		//	do search
		RefData[] rds = this.rfr.find(searchData.getProperty(TITLE_ANNOTATION_TYPE), searchData.getProperty(AUTHOR_ANNOTATION_TYPE), year, this.getOrigin(searchData));
		
		//	eliminate duplicates and return result
		return this.aggregateDuplicates(rds);
	}
	
	private String getOrigin(Properties searchData) {
		String origin = searchData.getProperty(JOURNAL_NAME_ANNOTATION_TYPE);
		if (origin != null) {
			String vd = searchData.getProperty(VOLUME_DESIGNATOR_ANNOTATION_TYPE);
			if (vd != null) {
				origin += (" " + vd);
				return origin;
			}
			String id = searchData.getProperty(ISSUE_DESIGNATOR_ANNOTATION_TYPE);
			if (id != null) {
				origin += (" " + id);
				return origin;
			}
			String nd = searchData.getProperty(NUMERO_DESIGNATOR_ANNOTATION_TYPE);
			if (nd != null) {
				origin += (" " + nd);
				return origin;
			}
		}
		
		origin = searchData.getProperty(VOLUME_TITLE_ANNOTATION_TYPE);
		if (origin != null)
			return origin;
		
		origin = searchData.getProperty(PUBLISHER_ANNOTATION_TYPE);
		if (origin != null) {
			String location = searchData.getProperty(LOCATION_ANNOTATION_TYPE);
			if (location != null)
				origin = (location + ": " + origin);
			return origin;
		}
		
		origin = searchData.getProperty(LOCATION_ANNOTATION_TYPE);
		if (origin != null)
			return origin;
		
		origin = searchData.getProperty(JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE);
		if (origin == null)
			return null;
		String vd = searchData.getProperty(VOLUME_DESIGNATOR_ANNOTATION_TYPE);
		if (vd != null) {
			origin += (" " + vd);
			return origin;
		}
		String id = searchData.getProperty(ISSUE_DESIGNATOR_ANNOTATION_TYPE);
		if (id != null) {
			origin += (" " + id);
			return origin;
		}
		String nd = searchData.getProperty(NUMERO_DESIGNATOR_ANNOTATION_TYPE);
		if (nd != null) {
			origin += (" " + nd);
			return origin;
		}
		
		return origin; // lone publisher
	}
	
	/* TODO
Aggregate external identifiers in ReFinder lookup:
- switch on "id" field, iterating through object properties as required ...
- ... setting identifiers based upon property keys
- add (configure ???) types and patterns for identifiers to be preserved in wrapping ReFinder data source ...
- ... aggregate identifiers during aggregation ...
- ... and filter before returning lookup results
	 */
	
	private RefData[] aggregateDuplicates(RefData[] rds) {
		if (rds.length < 2)
			return rds;
		ArrayList rdList = new ArrayList(Arrays.asList(rds));
		
		//	index by DOI where possible
		LinkedHashMap rdsByDois = new LinkedHashMap();
		for (int r = 0; r < rdList.size(); r++) {
			RefData rd = ((RefData) rdList.get(r));
			String doi = rd.getIdentifier("DOI");
			if (doi == null)
				continue;
			if (doi.indexOf("doi.org/10.") != -1) {
				doi = doi.substring(doi.indexOf("doi.org/10.") + "doi.org/".length());
				rd.setIdentifier("DOI", doi);
			}
			ArrayList doiRds = ((ArrayList) rdsByDois.get(doi));
			if (doiRds == null) {
				doiRds = new ArrayList(3);
				rdsByDois.put(doi, doiRds);
			}
			doiRds.add(rd);
			rdList.remove(r--); // this one is assigned
		}
		
		//	aggregate DOI identified duplicates, and index by title
		LinkedHashMap rdsByDataHashes = new LinkedHashMap();
		for (Iterator doiIt = rdsByDois.keySet().iterator(); doiIt.hasNext();) {
			String doi = ((String) doiIt.next());
			ArrayList doiRds = ((ArrayList) rdsByDois.get(doi));
			RefData doiRd = this.aggregateDuplicates(doiRds);
			String title = this.hashLight(doiRd.getAttribute(TITLE_ANNOTATION_TYPE));
			//	TODO refine this (check year and authors as well, for instance) !!!
			ArrayList dataHashRds = ((ArrayList) rdsByDataHashes.get(title));
			if (dataHashRds == null) {
				dataHashRds = new ArrayList(3);
				rdsByDataHashes.put(title, dataHashRds);
			}
			dataHashRds.add(doiRd);
		}
		
		//	add DOI-less references to by-title index
		for (int r = 0; r < rdList.size(); r++) {
			RefData rd = ((RefData) rdList.get(r));
			String title = this.hashLight(rd.getAttribute(TITLE_ANNOTATION_TYPE));
			//	TODO refine this (check year and authors as well, for instance) !!!
			ArrayList dataHashRds = ((ArrayList) rdsByDataHashes.get(title));
			if (dataHashRds == null) {
				dataHashRds = new ArrayList(3);
				rdsByDataHashes.put(title, dataHashRds);
			}
			dataHashRds.add(rd);
			rdList.remove(r--); // this one is assigned
		}
		
		//	perform final round of aggregation
		for (Iterator tit = rdsByDataHashes.keySet().iterator(); tit.hasNext();) {
			String title = ((String) tit.next());
			ArrayList titleRds = ((ArrayList) rdsByDataHashes.get(title));
			RefData titleRd = this.aggregateDuplicates(titleRds);
			rdList.add(titleRd);
		}
		
		//	finally ...
		return ((RefData[]) rdList.toArray(new RefData[rdList.size()]));
	}
	
	private String hashLight(String str) {
		if (str == null)
			return str;
		str = str.replaceAll("\\<[^\\>]\\>", ""); // remove any XML tags (sometimes come in as formatting)
		str = StringUtils.normalizeString(str); // strip accents
		str = str.replaceAll("[A-Za-z0-9]", ""); // strip punctuation marks and spaces
		str = str.toLowerCase(); // abstract from case
		return str;
	}
	
	private RefData aggregateDuplicates(ArrayList rds) {
		if (rds.isEmpty())
			return null;
		if (rds.size() == 1)
			return ((RefData) rds.get(0));
		
		//	always use volume if only one of volume and issue given
		for (int r = 0; r < rds.size(); r++) {
			RefData rd = ((RefData) rds.get(r));
			if (rd.hasAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE) && !rd.hasAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE)) {
				rd.setAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE, rd.getAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE));
				rd.removeAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE);
			}
		}
		
		//	aggregate other attributes
		RefData aRd = ((RefData) rds.get(0));
		for (int r = 1; r < rds.size(); r++) {
			RefData rd = ((RefData) rds.get(r));
			String[] ans = rd.getAttributeNames();
			for (int a = 0; a < ans.length; a++) {
				//	TODO maybe use some kind of majority vote on attribute values
				if (!aRd.hasAttribute(ans[a]))
					aRd.setAttribute(ans[a], rd.getAttribute(ans[a]));
			}
		}
		return aRd;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ReFinderRefDataSource rfrRds = new ReFinderRefDataSource("http://refinder.org/find");
//		RefData[] rds = {rfrRds.getRefData("10.3897/zookeys.491.8553")};
		Properties query = new Properties();
//		query.setProperty("ID-DOI", "http://dx.doi.org/10.3897/zookeys.491.8553");
		query.setProperty(TITLE_ANNOTATION_TYPE, "New species and records of Chimarra (Trichoptera, Philopotamidae) from Northeastern Brazil, and an updated key to subgenus Chimarra (Chimarrita)");
		query.setProperty(YEAR_ANNOTATION_TYPE, "2015");
		query.setProperty(AUTHOR_ANNOTATION_TYPE, "Calor");
		query.setProperty(JOURNAL_NAME_OR_PUBLISHER_ANNOTATION_TYPE, "ZooKeys");
		RefData[] rds = rfrRds.findRefData(query);
		for (int r = 0; r < rds.length; r++) {
			BibRefUtils.classify(rds[r]);
			System.out.println(rds[r].getAttribute(PUBLICATION_TYPE_ATTRIBUTE) + " from " + rds[r].getAttribute("refSource"));
			System.out.println(rds[r].toXML());
			System.out.println(BibRefUtils.toRefString(rds[r]).trim());
		}
	}
}
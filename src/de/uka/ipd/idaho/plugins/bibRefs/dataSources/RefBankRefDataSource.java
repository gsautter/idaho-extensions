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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.plugins.bibRefs.refBank.RefBankClient;
import de.uka.ipd.idaho.plugins.bibRefs.refBank.RefBankClient.BibRef;
import de.uka.ipd.idaho.plugins.bibRefs.refBank.RefBankClient.BibRefIterator;

/**
 * Data source fetching document meta data from a RefBank node. If the URL of
 * the RefBank node to use is not specified via the constructor, this class
 * expects its data provider to have a file named <code>config.RefBank.cnfg</code>
 * available, with a setting named <code>refBankNodeUrl</code> containing the
 * URL of the RefBank node to connect to.
 * 
 * @author sautter
 */
public class RefBankRefDataSource extends BibRefDataSource {
	private RefBankClient rbk;
	
	/** Constructor
	 */
	public RefBankRefDataSource() {
		this(null);
	}
	
	/** Constructor
	 * @param refBankUrl the URL of the RefBank node to use
	 */
	public RefBankRefDataSource(String refBankUrl) {
		super("RefBank");
		if (refBankUrl != null)
			this.rbk = new RefBankClient(refBankUrl);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource#init()
	 */
	public void init() {
		if (this.rbk == null) try {
			Reader setReader = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("config.RefBank.cnfg"), "UTF-8"));
			Settings set = Settings.loadSettings(setReader);
			String refBankUrl = set.getSetting("refBankNodeUrl");
			if (refBankUrl == null)
				throw new RuntimeException("URL of RefBank node missing.");
			this.rbk = new RefBankClient(refBankUrl);
		}
		catch (IOException ioe) {
			throw new RuntimeException("Could not load URL of RefBank node.");
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource#isSuitableID(java.lang.String)
	 */
	public boolean isSuitableID(String docId) {
		return ((docId != null) && docId.matches("[0-9A-F]{32}"));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.bibRefs.BibRefDataSource#getRefData(java.lang.String)
	 */
	public RefData getRefData(String sourceDocId) throws IOException {
		BibRef br = this.rbk.getRef(sourceDocId);
		return ((br == null) ? null : this.getRefData(br));
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
		int year = -1;
		try {
			year = Integer.parseInt(searchData.getProperty(YEAR_ANNOTATION_TYPE));
		} catch (Exception e) {}
		
		//	do search
		BibRefIterator brit = this.rbk.findRefs(null, searchData.getProperty(AUTHOR_ANNOTATION_TYPE), searchData.getProperty(TITLE_ANNOTATION_TYPE), year, this.getOrigin(searchData), extId, extIdType, 0, false);
		ArrayList rdList = new ArrayList();
		while (brit.hasNextRef()) {
			BibRef br = brit.getNextRef();
			RefData rd = this.getRefData(br);
			if (rd == null)
				continue;
			rd.setAttribute(REFERENCE_DATA_SOURCE_ATTRIBUTE, "RefBank");
			BibRefUtils.classify(rd);
			rdList.add(rd);
		}
		return ((RefData[]) rdList.toArray(new RefData[rdList.size()]));
	}
	
	private RefData getRefData(BibRef br) throws IOException {
		if (br.getRefParsed() == null)
			return null;
		return BibRefUtils.modsXmlToRefData(SgmlDocumentReader.readDocument(new StringReader(br.getRefParsed())));
	}
	
	private String getOrigin(Properties searchData) {
		String origin = searchData.getProperty(JOURNAL_NAME_ANNOTATION_TYPE);
		if (origin != null) {
			String sj = searchData.getProperty(SERIES_IN_JOURNAL_ANNOTATION_TYPE);
			if (sj != null)
				origin += (" (" + sj + ")");
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
		
		return null;
	}
}
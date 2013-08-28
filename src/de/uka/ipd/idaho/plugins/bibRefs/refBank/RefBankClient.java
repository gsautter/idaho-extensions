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
package de.uka.ipd.idaho.plugins.bibRefs.refBank;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * Basic REST client for the RefBank bibliographic reference repository.
 * 
 * @author sautter
 */
public class RefBankClient {
	
	private static Grammar xmlGrammar = new StandardGrammar();
	private static Parser xmlParser = new Parser(xmlGrammar);
	
	private static DateFormat timestampHandler = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
	static {
		timestampHandler.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	/**
	 * Container for bibliographic reference data according to the RefBank API.
	 * 
	 * @author sautter
	 */
	public static class BibRef {
		public final String id;
		private String canonicalId = null;
		private String refString = null;
		private String refParsed = null;
		private String parseChecksum = null;
		private String parseError = null;
		private long createTime = -1;
		private String createDomain = null;
		private String createUser = null;
		private long updateTime = -1;
		private String updateDomain = null;
		private String updateUser = null;
		private long nodeUpdateTime = -1;
		private boolean created = false;
		private boolean updated = false;
		private boolean deleted = false;
		private BibRef(String id) {
			this.id = id;
		}
		public String getRefString() {
			return this.refString;
		}
		public String getRefParsed() {
			return this.refParsed;
		}
		public String getCanonicalID() {
			return ((this.canonicalId == null) ? this.id : this.canonicalId);
		}
		public String getParseChecksum() {
			return this.parseChecksum;
		}
		public String getParseError() {
			return this.parseError;
		}
		public long getCreateTime() {
			return this.createTime;
		}
		public String getCreateDomain() {
			return this.createDomain;
		}
		public String getCreateUser() {
			return this.createUser;
		}
		public long getUpdateTime() {
			return this.updateTime;
		}
		public String getUpdateDomain() {
			return this.updateDomain;
		}
		public String getUpdateUser() {
			return this.updateUser;
		}
		public long getNodeUpdateTime() {
			return this.nodeUpdateTime;
		}
		public boolean wasCreated() {
			return this.created;
		}
		public boolean wasUpdated() {
			return this.updated;
		}
		public boolean isDeleted() {
			return this.deleted;
		}
	}
	
	/**
	 * Iterator over individual bibliographic references.
	 * 
	 * @author sautter
	 */
	public static class BibRefIterator {
		private Iterator brit;
		private BibRefIterator(ArrayList refs) {
			this.brit = refs.iterator();
		}
		public boolean hasNextRef() {
			return this.brit.hasNext();
		}
		public BibRef getNextRef() {
			return (this.brit.hasNext() ? ((BibRef) this.brit.next()) : null);
		}
	}
	
	private String refBankUrl;
	
	/**
	 * Constructor
	 * @param refBankUrl the URL of the RefBank node to connect to
	 */
	public RefBankClient(String refBankUrl) {
		this.refBankUrl = refBankUrl;
	}
	
	/**
	 * Retrive a bibliograthic reference by its RefBank ID.
	 * @param refId the reference ID
	 * @return the reference with the argument ID
	 * @throws IOException
	 */
	public BibRef getRef(String refId) throws IOException {
		String[] refIds = {refId};
		BibRefIterator brit = this.getRefs(refIds);
		return (brit.hasNextRef() ? brit.getNextRef() : null);
	}
	
	/**
	 * Retrive bibliograthic references by their RefBank IDs.
	 * @param refIds an array holding the reference IDs
	 * @return an iterator over the references with the argument IDs
	 * @throws IOException
	 */
	public BibRefIterator getRefs(String[] refIds) throws IOException {
		StringBuffer refIdString = new StringBuffer();
		for (int i = 0; i < refIds.length; i++)
			refIdString.append("&id=" + URLEncoder.encode(refIds[i], "UTF-8"));
		URL getUrl = new URL(this.refBankUrl + "?action=get" + refIdString.toString());
		return this.readRefs(new BufferedReader(new InputStreamReader(getUrl.openStream(), "UTF-8")));
	}
	
	/**
	 * Retrive bibliograthic references by their canonical ID.
	 * @param canRefId the canonical ID to get the references for
	 * @return an iterator over the references with the argument canonical ID
	 * @throws IOException
	 */
	public BibRefIterator getLinkedRefs(String canRefId) throws IOException {
		URL getUrl = new URL(this.refBankUrl + "?action=get&canonicalId=" + URLEncoder.encode(canRefId, "UTF-8"));
		return this.readRefs(new BufferedReader(new InputStreamReader(getUrl.openStream(), "UTF-8")));
	}
	
	/**
	 * Search references.
	 * @param freeText a free text search string, to match some pert of the reference string
	 * @param author an author name to search for
	 * @param title a text snippet to find in titles
	 * @param year the year of publication to search for
	 * @param origin the journal, publisher, or conference to search for
	 * @param extId an external identifier, e.g. a DOI
	 * @param extIdType the type of the external identifier
	 * @return an iterator over the references matching the query
	 * @throws IOException
	 */
	public BibRefIterator findRefs(String freeText, String author, String title, int year, String origin, String extId, String extIdType) throws IOException {
		StringBuffer queryString = new StringBuffer();
		if (author != null)
			queryString.append("&author=" + URLEncoder.encode(author, "UTF-8"));
		if (title != null)
			queryString.append("&title=" + URLEncoder.encode(title, "UTF-8"));
		if (year > 0)
			queryString.append("&date=" + year);
		if (origin != null)
			queryString.append("&origin=" + URLEncoder.encode(origin, "UTF-8"));
		if ((extId != null) && (extIdType != null))
			queryString.append("&ID-" + extIdType + "=" + URLEncoder.encode(extId, "UTF-8"));
		if (freeText != null)
			queryString.append("&query=" + URLEncoder.encode(freeText, "UTF-8"));
		URL findUrl = new URL(this.refBankUrl + "?action=find" + queryString.toString());
		return this.readRefs(new BufferedReader(new InputStreamReader(findUrl.openStream(), "UTF-8")));
	}
	
	private BibRefIterator readRefs(Reader r) throws IOException {
		RefReader rr = new RefReader();
		xmlParser.stream(r, rr);
		return new BibRefIterator(rr.refs);
	}
	private static class RefReader extends TokenReceiver {
		private StringBuffer refStringBuffer = null;
		private String refString = null;
		private StringBuffer refParsedBuffer = null;
		private String refParsed = null;
		private BibRef ps = null;
		ArrayList refs = new ArrayList();
		RefReader() throws IOException {
			super();
		}
		public void close() throws IOException {}
		public void storeToken(String token, int treeDepth) throws IOException {
			if (xmlGrammar.isTag(token)) {
				String type = xmlGrammar.getType(token);
				type = type.substring(type.indexOf(':') + 1);
				boolean isEndTag = xmlGrammar.isEndTag(token);
				if ("ref".equals(type)) {
					if (isEndTag) {
						if (this.ps != null) {
							this.ps.refString = this.refString;
							this.ps.refParsed = this.refParsed;
							this.refs.add(this.ps);
						}
						this.ps = null;
					}
					else {
						TreeNodeAttributeSet stringAttributes = TreeNodeAttributeSet.getTagAttributes(token, xmlGrammar);
						String refId = stringAttributes.getAttribute("id");
						if (refId == null)
							return;
						this.ps = new BibRef(refId);
						try {
							this.ps.createTime = parseTime(stringAttributes.getAttribute("createTime", "-1"));
							this.ps.createDomain = stringAttributes.getAttribute("createDomain");
							this.ps.createUser = stringAttributes.getAttribute("createUser");
							this.ps.updateTime = parseTime(stringAttributes.getAttribute("updateTime", "-1"));
							this.ps.updateDomain = stringAttributes.getAttribute("updateDomain");
							this.ps.updateUser = stringAttributes.getAttribute("updateUser");
							this.ps.nodeUpdateTime = parseTime(stringAttributes.getAttribute("loaclUpdateTime", "-1"));
							this.ps.deleted = "true".equalsIgnoreCase(stringAttributes.getAttribute("deleted"));
						}
						catch (NumberFormatException nfe) {
							System.out.println("Error in timestamps, token is " + token);
							nfe.printStackTrace(System.out);
							this.ps = null;
							return;
						}
						this.ps.canonicalId = stringAttributes.getAttribute("canonicalId");
						this.ps.parseChecksum = stringAttributes.getAttribute("parseChecksum");
						this.ps.parseError = stringAttributes.getAttribute("parseError");
						this.ps.created = "true".equalsIgnoreCase(stringAttributes.getAttribute("created"));
						this.ps.updated = "true".equalsIgnoreCase(stringAttributes.getAttribute("updated"));
						if (xmlGrammar.isSingularTag(token)) {
							this.refs.add(this.ps);
							this.ps = null;
						}
					}
					this.refStringBuffer = null;
					this.refString = null;
					this.refParsedBuffer = null;
					this.refParsed = null;
				}
				else if ("refString".equals(type)) {
					if (isEndTag) {
						if (this.refStringBuffer.length() != 0)
							this.refString = this.refStringBuffer.toString();
						this.refStringBuffer = null;
					}
					else this.refStringBuffer = new StringBuffer();
				}
				else if ("refParsed".equals(type)) {
					if (isEndTag) {
						if (this.refParsedBuffer.length() != 0)
							this.refParsed = this.refParsedBuffer.toString();
						this.refParsedBuffer = null;
					}
					else this.refParsedBuffer = new StringBuffer();
				}
				else if (this.refParsedBuffer != null)
					this.refParsedBuffer.append(token);
			}
			else if (this.refStringBuffer != null)
				this.refStringBuffer.append(xmlGrammar.unescape(token));
			else if (this.refParsedBuffer != null)
				this.refParsedBuffer.append(token.trim());
		}
		private long parseTime(String timeString) throws NumberFormatException {
			try {
				return timestampHandler.parse(timeString).getTime();
			}
			catch (ParseException pe) {
				return Long.parseLong(timeString);
			}
			catch (Exception e) {
				System.out.println("Could not parse time string |" + timeString + "|");
				throw new NumberFormatException(timeString);
			}
		}
	}
	
	/**
	 * Retrieve the update feed summarizing all references updated since a given time.
	 * @param since the UTC time since when to retrieve the update summary
	 * @return an interator over the strings updated since the argument time
	 * @throws IOException
	 */
	public BibRefIterator getUpdates(long since) throws IOException {
		URL feedUrl = new URL(this.refBankUrl + "?action=feed&updatedSince=" + URLEncoder.encode(timestampHandler.format(new Date(since)), "UTF-8"));
		return this.readRefs(new BufferedReader(new InputStreamReader(feedUrl.openStream(), "UTF-8")));
	}
	
	/**
	 * Upload a bibliographic reference to RefBank
	 * @param refString the plain reference string
	 * @param refParsed a parsed representation of the reference (MODS XML)
	 * @param user the user to credit for the upload
	 * @return the upload result
	 * @throws IOException
	 */
	public BibRef uploadRef(String refString, String refParsed, String user) throws IOException {
		URL putUrl = new URL(this.refBankUrl);
		HttpURLConnection putCon = ((HttpURLConnection) putUrl.openConnection());
		putCon.setDoInput(true);
		putCon.setDoOutput(true);
		putCon.setRequestMethod("PUT");
		putCon.setRequestProperty("Data-Format", "xml");
		if (user != null)
			putCon.setRequestProperty("User-Name", user);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(putCon.getOutputStream(), "UTF-8"));
		bw.write("<refSet><ref>");
		bw.write("<refString>" + AnnotationUtils.escapeForXml(refString) + "</refString>");
		if (refParsed != null) {
			bw.write("<refParsed>");
			bw.write(refParsed);
			bw.write("</refParsed>");
		}
		bw.write("</ref></refSet>");
		bw.flush();
		bw.close();
		BibRefIterator brit =  this.readRefs(new BufferedReader(new InputStreamReader(putCon.getInputStream(), "UTF-8")));
		return (brit.hasNextRef() ? brit.getNextRef() : null);
	}
	
	/**
	 * Set the canonical ID of a reference.
	 * @param refId the ID of the reference to update
	 * @param canRefId the canonical ID to set
	 * @param user the user to credit for the update
	 * @return the update result
	 * @throws IOException
	 */
	public BibRef setCanRefId(String refId, String canRefId, String user) throws IOException {
		URL url = new URL(this.refBankUrl + "/update");
		HttpURLConnection con = ((HttpURLConnection) url.openConnection());
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Data-Format", "xml");
		if (user != null)
			con.setRequestProperty("user", user);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
		bw.write("<refSet>");
		bw.newLine();
		bw.write("<ref");
		bw.write(" id=\"" + AnnotationUtils.escapeForXml(refId, true) + "\"");
		if (canRefId != null)
			bw.write(" canonicalId=\"" + AnnotationUtils.escapeForXml(canRefId, true) + "\"");
		bw.write(" deleted=\"false\"");
		bw.write("/>");
		bw.newLine();
		bw.write("</refSet>");
		bw.newLine();
		bw.flush();
		bw.close();
		BibRefIterator psi = this.readRefs(new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8")));
		return (psi.hasNextRef() ? psi.getNextRef() : null);
	}
	
	/**
	 * Mark a reference as deleted or not.
	 * @param refId the ID of the reference to update
	 * @param deleted the deletion status to set
	 * @param user the user to credit for the update
	 * @return the update result
	 * @throws IOException
	 */
	public BibRef setDeleted(String refId, boolean deleted, String user) throws IOException {
		URL url = new URL(this.refBankUrl + "/update");
		HttpURLConnection con = ((HttpURLConnection) url.openConnection());
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Data-Format", "xml");
		if (user != null)
			con.setRequestProperty("user", user);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
		bw.write("<refSet>");
		bw.write("<ref id=\"" + AnnotationUtils.escapeForXml(refId, true) + "\" deleted=\"" + (deleted ? "true" : "false") + "\"/>");
		bw.write("</refSet>");
		bw.flush();
		bw.close();
		BibRefIterator psi = this.readRefs(new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8")));
		return (psi.hasNextRef() ? psi.getNextRef() : null);
	}
}
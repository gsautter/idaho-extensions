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
package de.uka.ipd.idaho.gamta.util.feedback.tools;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Properties;

import javax.swing.JOptionPane;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.AnnotationEditorFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.AssignmentDisambiguationFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.CategorizationFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.CheckBoxFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.StartContinueFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.StartContinueOtherFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AnnotationEditorFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AssignmentDisambiguationFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.CategorizationFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.CheckBoxFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.StartContinueFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.StartContinueOtherFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageSource;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * The purpose of this class is to serve as a test environment for feedback
 * panel HTML renderers. In particular, the methods of this class allow for
 * writing the JavaScript, CSS, and HTML code representing a feedback panel to
 * some arbitrary writer. Furthermore, it can open an HTTP server and receive
 * and process the submitted feedback form. With this class, test code in a
 * method using a feedback panel reduces to<br>
 * <code><pre>
 * FeedbackPanel myFeedbackPanel = new MySpecialFeedbackPanel(); // some sub class of FeedbackPanel
 * 
 * // ... fill the feedback panel with your data
 * 
 * // prepare storing feedback value
 * String feedback = null;
 * 
 * // normally, you would get feedback like this
 * feedback = myFeedbackPanel.getFeedback();
 * 
 * // test the feedback panel's HTML appearance and functionality
 * try {
 * 	feedback = FeedbackPanelHtmlTester.testFeedbackPanel(myFeedbackPanel, 0);
 * } catch (IOException ioe) {
 * 	ioe.printStackTrace();
 * }
 * // test done, FeedbackPanelHtmlTester does the rest
 * 
 * // ... proceed with regular application code
 * </pre></code><br>
 * Instances of this class also implement a standard feedback service, so test
 * classes can use it without even changing the code of the method they test. In
 * this latter case, it uses a primitive, yet multi-threaded HTTP server that
 * supports only GET in general and wants the response via POST.
 * 
 * @author sautter
 */
public class FeedbackPanelHtmlTester implements FeedbackService, ImagingConstants {
	
	private static AnnotationEditorFeedbackPanelRenderer aefpr = new AnnotationEditorFeedbackPanelRenderer();
	private static AssignmentDisambiguationFeedbackPanelRenderer adfpr = new AssignmentDisambiguationFeedbackPanelRenderer();
	private static CategorizationFeedbackPanelRenderer cfpr = new CategorizationFeedbackPanelRenderer();
	private static CheckBoxFeedbackPanelRenderer cbfpr = new CheckBoxFeedbackPanelRenderer();
	private static StartContinueFeedbackPanelRenderer scfpr = new StartContinueFeedbackPanelRenderer();
	private static StartContinueOtherFeedbackPanelRenderer scofpr = new StartContinueOtherFeedbackPanelRenderer();
	
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM, yyyy hh:mm:ss z");
	
	private int port;
	private File basePath;
	private AnalyzerDataProvider dataProvider;
	
	private String basePage;
	private String[] cssFiles;
	private String[] javaScripts;
	
	private PageImageSource pis;
	private String url;
	private ServerSocket ss;
	private boolean active = false;
	
	/**
	 * Constructor (registers feedback service automatically)
	 * @param port the port to use
	 * @param basePath the folder to look for data
	 * @param basePage the file name of the feedback base page
	 * @param cssFiles the names of CSS files to include in feedback request
	 *            pages
	 * @param javaScripts the names of JavaScript files to include in feedback
	 *            request pages
	 * @throws IOException
	 */
	public FeedbackPanelHtmlTester(int port, File basePath, String basePage, String[] cssFiles, String[] javaScripts) throws IOException {
		this(port, basePath, basePage, cssFiles, javaScripts, null);
	}
	
	/**
	 * Constructor (registers feedback service automatically)
	 * @param port the port to use
	 * @param basePath the folder to look for data
	 * @param basePage the file name of the feedback base page
	 * @param cssFiles the names of CSS files to include in feedback request
	 *            pages
	 * @param javaScripts the names of JavaScript files to include in feedback
	 *            request pages
	 * @param pis a sorce for images that occur in the feedback requests
	 * @throws IOException
	 */
	public FeedbackPanelHtmlTester(int port, File basePath, String basePage, String[] cssFiles, String[] javaScripts, PageImageSource pis) throws IOException {
		this.port = port;
		this.basePath = basePath;
		this.dataProvider = new AnalyzerDataProviderFileBased(this.basePath);
		this.basePage = basePage;
		this.cssFiles = ((cssFiles == null) ? new String[0] : cssFiles);
		this.javaScripts = ((javaScripts == null) ? new String[0] : javaScripts);
		this.pis = pis;
		this.url = ("http://localhost:" + port);
		this.startServer();
		FeedbackPanel.addFeedbackService(this);
	}
	
	/**
	 * Constructor (registers feedback service automatically)
	 * @param port the port to use
	 * @param dataProvider the data provider for the feedback service
	 * @param basePage the file name of the feedback base page
	 * @param cssFiles the names of CSS files to include in feedback request
	 *            pages
	 * @param javaScripts the names of JavaScript files to include in feedback
	 *            request pages
	 * @throws IOException
	 */
	public FeedbackPanelHtmlTester(int port, AnalyzerDataProvider dataProvider, String basePage, String[] cssFiles, String[] javaScripts) throws IOException {
		this(port, dataProvider, basePage, cssFiles, javaScripts, null);
	}
	
	/**
	 * Constructor (registers feedback service automatically)
	 * @param port the port to use
	 * @param dataProvider the data provider for the feedback service
	 * @param basePage the file name of the feedback base page
	 * @param cssFiles the names of CSS files to include in feedback request
	 *            pages
	 * @param javaScripts the names of JavaScript files to include in feedback
	 *            request pages
	 * @param pis a sorce for images that occur in the feedback requests
	 * @throws IOException
	 */
	public FeedbackPanelHtmlTester(int port, AnalyzerDataProvider dataProvider, String basePage, String[] cssFiles, String[] javaScripts, PageImageSource pis) throws IOException {
		this.port = port;
		this.basePath = null;
		this.dataProvider = dataProvider;
		this.basePage = basePage;
		this.cssFiles = ((cssFiles == null) ? new String[0] : cssFiles);
		this.javaScripts = ((javaScripts == null) ? new String[0] : javaScripts);
		this.pis = pis;
		this.url = ("http://localhost:" + port);
		this.startServer();
		FeedbackPanel.addFeedbackService(this);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getPriority()
	 */
	public int getPriority() {
		return 11; // if we are active, we want them requests
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#canGetFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public boolean canGetFeedback(FeedbackPanel fp) {
		return this.active;
	}
	
	/**
	 * @return true if the feedback service is activated, false otherwise
	 */
	public boolean isActive() {
		return this.active;
	}
	
	/**
	 * Activate or deactivate the feedback service.
	 * @param active the new activation status
	 */
	public void setActive(boolean active) {
		this.active = active;
	}
	
	/**
	 * @return the to reach send HTTP requests to
	 */
	public String getUrl() {
		return url;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public void getFeedback(FeedbackPanel fp) {
		testFeedbackPanel(fp);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getMultiFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel[])
	 */
	public void getMultiFeedback(FeedbackPanel[] fps) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("MultiFeedback is not supported for now.");
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#isLocal()
	 */
	public boolean isLocal() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#isMultiFeedbackSupported()
	 */
	public boolean isMultiFeedbackSupported() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#shutdown()
	 */
	public void shutdown() {
		if (this.ss != null) try {
			this.ss.close();
			this.ss = null;
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	private FeedbackPanel cFp = null;
	private FeedbackPanelHtmlRendererInstance cFpr;
	private Object fpLock = new Object();
	
	private void testFeedbackPanel(FeedbackPanel fp) {
		synchronized (this.fpLock) {
			FeedbackPanelHtmlRenderer fpr;
			if (fp instanceof AssignmentDisambiguationFeedbackPanel)
				fpr = adfpr;
			else if (fp instanceof AnnotationEditorFeedbackPanel)
				fpr = aefpr;
			else if (fp instanceof CategorizationFeedbackPanel)
				fpr = cfpr;
			else if (fp instanceof CheckBoxFeedbackPanel)
				fpr = cbfpr;
			else if (fp instanceof StartContinueFeedbackPanel)
				fpr = scfpr;
			else if (fp instanceof StartContinueOtherFeedbackPanel)
				fpr = scofpr;
			else fpr = new FeedbackPanelHtmlRenderer() {
				public int canRender(FeedbackPanel fp) {
					return 0;
				}
				public FeedbackPanelHtmlRendererInstance getRendererInstance(FeedbackPanel fp) {
					return new FeedbackPanelHtmlRendererInstance(fp) {};
				}
			};
			this.cFpr = fpr.getRendererInstance(fp);
			this.cFp = fp;
			try {
				this.fpLock.wait();
			} catch (InterruptedException ie) {}
			this.cFp = null;
			this.cFpr = null;
		}
	}
	
	private static final boolean DEBUG = false;
	private void startServer() throws IOException {
		this.ss = new ServerSocket(this.port);
		Thread st = new Thread() {
			public void run() {
				while (!ss.isClosed()) try {
					final Socket rs = ss.accept();
					Thread st = new Thread() {
						public void run() {
							try {
								if (DEBUG) System.out.println("===== Request Start =====");
								BufferedReader rr = new BufferedReader(new InputStreamReader(rs.getInputStream(), "UTF-8"));
								String rl;
								String method = null;
								String path = null;
								String queryString = null;
								int rLength = Integer.MAX_VALUE;
								while (((rl = rr.readLine()) != null) && (rl.length() != 0)) {
									if (DEBUG) System.out.println(rl);
									if (method == null) {
										String[] rlps = rl.split("\\s++");
										method = rlps[0];
										path = rlps[1];
										if (path.startsWith("/"))
											path = path.substring(1);
										if ("GET".equals(method) && (path.indexOf('?') != -1)) {
											queryString = path.substring(path.indexOf('?') + 1);
											path = path.substring(0, path.indexOf('?'));
											for (int p = 2; p < (rlps.length - 1); p++)
												queryString = (queryString + rlps[p]);
										}
									}
									if (rl.startsWith("Content-Length: "))
										rLength = Integer.parseInt(rl.substring("Content-Length: ".length()));
								}
								if (DEBUG) {
									System.out.println("===== Request Header End =====");
									System.out.println("Method: " + method);
									System.out.println("Path: " + path);
									System.out.println("Query: " + queryString);
								}
								
								Properties query = parseQueryString(queryString);
								
								OutputStream ros = rs.getOutputStream();
								BufferedWriter rw = new BufferedWriter(new OutputStreamWriter(ros, "UTF-8"));
								
								if ("GET".equals(method)) {
									
									//	initial request, send feedback base page
									if (path.length() == 0) {
										if (cFp == null) {
											writeResponseHeader(rw, 200, "OK", "text/html; charset=UTF-8", -1);
											rw.write("<html><body>");
											rw.newLine();
											rw.write("<a href=\"" + url + "\">No Request, Try Again</a>");
											rw.newLine();
											rw.write("<body></html>");
											rw.newLine();
										}

										//	send feedback request
										else {
											writeResponseHeader(rw, 200, "OK", null, -1);
											sendFeedbackPanel(cFp, cFpr, rw);
											rw.flush();
										}
									}
									
									//	request for page image
									else if (path.startsWith("images/") && path.endsWith("." + IMAGE_FORMAT)) {
										String[] ipp = path.split("\\/");
										PageImage pi = null;
										if ((pis != null) && (ipp.length == 3)) try {
											pi = getPageImage(pis, ipp[1], Integer.parseInt(ipp[2].substring(0, ipp[2].indexOf('.'))), query);
										} catch (IOException ioe) {}
										
										//	send 404
										if (pi == null)
											writeResponseHeader(rw, 404, "NOT_FOUND", null, -1);
										
										//	send image
										else {
											writeResponseHeader(rw, 200, "OK", null, (pi.image.getWidth() * pi.image.getHeight() * 4));
											rw.flush();
											pi.writeImage(ros);
										}
									}
									
									//	request for some other file
									else {
										int length = -1;
										if (basePath != null) {
											File file = new File(basePath, path);
											if (file.exists() && file.isFile())
												length = ((int) file.length());
										}
										
										//	check if file exists
										if (dataProvider.isDataAvailable(path)) {
											
											//	catch images
											if (path.endsWith(".gif") || path.endsWith(".jgp") || path.endsWith(".jpeg") || path.endsWith(".png")) {
												writeResponseHeader(rw, 200, "OK", ("image/" + path.substring(path.lastIndexOf('.') + 1)), length);
												rw.flush();
												InputStream is = dataProvider.getInputStream(path);
												byte[] buffer = new byte[1024];
												int read;
												while ((read = is.read(buffer, 0, buffer.length)) != -1)
													ros.write(buffer, 0, read);
												is.close();
											}
											
											//	some text base data
											else {
												String ct;
												if (path.endsWith(".html") || path.endsWith(".htm"))
													ct = ("text/html; charset=UTF-8");
												else if (path.endsWith(".xml"))
													ct = ("text/xml; charset=UTF-8");
												else ct = ("text/plain; charset=UTF-8");
												
												writeResponseHeader(rw, 200, "OK", ct, length);
												rw.flush();
												InputStream is = dataProvider.getInputStream(path);
												byte[] buffer = new byte[1024];
												int read;
												while ((read = is.read(buffer, 0, buffer.length)) != -1)
													ros.write(buffer, 0, read);
												is.close();
											}
										}
										
										//	send 404
										else writeResponseHeader(rw, 404, "NOT_FOUND", null, -1);
									}
								}
								
								//	response to feedback request
								else if ("POST".equals(method)) {
									StringBuffer rQuery = new StringBuffer();
									int rChar;
									int rRead = 0;
									while ((rRead++ < rLength) && ((rChar = rr.read()) != -1)) {
										rQuery.append((char) rChar);
									}
									if (DEBUG) {
										System.out.println(rQuery.toString());
										System.out.println("===== Request Body End =====");
									}
									
									//	parse response
									query = parseQueryString(rQuery.toString());
									
									writeResponseHeader(rw, 200, "OK", "text/html; charset=UTF-8", -1);
									rw.write("<html><body>");
									rw.newLine();
									rw.write("<ul>");
									rw.newLine();
									
									ArrayList keys = new ArrayList(query.keySet());
									Collections.sort(keys);
									for (int k = 0; k < keys.size(); k++) {
										String key = keys.get(k).toString();
										String value = query.getProperty(key);
										rw.write("<li>" + key + " = " + ((value == null) ? "null" : ("'" + value + "'")) + "</li>");
										rw.newLine();
									}
									
									rw.write("</ul>");
									rw.newLine();
									rw.write("<a href=\"" + url + "\">Next Request</a>");
									rw.newLine();
									rw.write("<body></html>");
									rw.newLine();
									
									//	feed response to feedback panel
									cFpr.readResponse(query);
									cFp.setStatusCode("OK");
									
									//	wake up waiting thread
									synchronized (fpLock) {
										fpLock.notify();
									}
								}
								
								rw.flush();
								ros.flush();
								rs.close();
							}
							catch (IOException ioe) {
								ioe.printStackTrace(System.out);
							}
						}
					};
					st.start();
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.out);
				}
			}
		};
		st.start();
	}
	
	private static final Properties parseQueryString(String queryString) throws IOException {
		if (queryString == null)
			return null;
		Properties query = new Properties();
		String[] kvPairs = queryString.split("\\&");
		for (int p = 0; p < kvPairs.length; p++) {
			int kvSplit = kvPairs[p].indexOf('=');
			if (kvSplit != -1) {
				String key = kvPairs[p].substring(0, kvSplit);
				String value = URLDecoder.decode(kvPairs[p].substring(kvSplit + 1), "UTF-8");
				if (DEBUG) System.out.println("  - " + key + " = '" + value + "'");
				if (value.length() != 0)
					query.setProperty(key, value);
			}
		}
		return query;
	}
	
	private static final void writeResponseHeader(BufferedWriter rw, int sc, String ss, String ct, int cl) throws IOException {
		String rt = dateFormat.format(new Date());
		if (sc == 200) {
			rw.write("HTTP/1.1 200 OK");
			rw.newLine();
			rw.write("Date: " + rt);
			rw.newLine();
			rw.write("Server: Feedback Panel Test Server");
			rw.newLine();
			rw.write("Last-Modified: " + rt);
			rw.newLine();
			rw.write("Accept-Ranges: bytes");
			rw.newLine();
			rw.write("Connection: close");
			rw.newLine();
			rw.write("Content-Type: " + ct);
			rw.newLine();
			if (cl != -1) {
				rw.write("Content-Type: " + ct);
				rw.newLine();
				rw.write("");
			}
			rw.newLine();
		}
		else {
			rw.write("HTTP/1.1 " + sc + " " + ss);
			rw.newLine();
			rw.write("Date: " + rt);
			rw.newLine();
			rw.write("Server: Feedback Panel Test Server");
			rw.newLine();
			rw.write("Connection: close");
			rw.newLine();
			rw.write("");
			rw.newLine();
		}
	}
	
	private PageImage getPageImage(PageImageSource pis, String docId, int pageId, Properties query) throws IOException {
		PageImage pi = pis.getPageImage(docId, pageId);
		
		//	read parameters
		String bbString = ((query == null) ? null : query.getProperty(BOUNDING_BOX_ATTRIBUTE));
		BoundingBox[] bbs = BoundingBox.parseBoundingBoxes(bbString);
		String dpiString = ((query == null) ? null : query.getProperty("dpi"));
		int dpi = pi.currentDpi;
		if (dpiString != null) try {
			dpi = Integer.parseInt(dpiString);
		} catch (NumberFormatException nfe) {}
		
		//	request for plain image
		if ((pi.currentDpi <= dpi) && (bbs == null))
			return pi;
		
		//	request for scaled image
		else if (bbs == null)
			return pi.scaleToDpi(dpi);
		
		//	request for image part, but bad boounding box string
		else if (bbs[0] == null)
			return null;
		
		//	request for part of single image
		else if (bbs.length == 1) {
			pi = pi.getSubImage(bbs[0], true);
			if (dpi < pi.currentDpi)
				pi = pi.scaleToDpi(dpi);
			return pi;
		}
		
		//	request for compiled image
		else {
			
			//	compile and scale image
			pi = new PageImage(PageImage.compileImage(docId, pageId, bbs, true, 3, null, pis), pi.currentDpi, null);
			if (dpi < pi.currentDpi)
				pi = pi.scaleToDpi(dpi);
			return pi;
		}
	}
	
	//	TODO use buffered line writer (put in separate package)
	
	//	TODO use feedback form page builder ==> not possible, we're not in a servlet
	//	TODO well, then mock-up HTTP servlet request and response ...
	
	//	TODO use basic HTML page embedded in JAR
	//	TODO unless data path set and base page available from there
	
	//	TODO facilitate retrieving a feedback form page multiple times
	
	//	TODO use fully blown multi threaded HTTP server, if without thread pooling, and only handling GET and POST
	
	private static Html html = new Html();
	private static Parser htmlParser = new Parser(html);
	
	private void sendFeedbackPanel(final FeedbackPanel fp, final FeedbackPanelHtmlRendererInstance fpr, final BufferedWriter bw) throws IOException {
		
		//	prepare page builder
		TokenReceiver tr = new TokenReceiver() {
			
			//	local status information
			private boolean inHyperLink = false;
			private boolean inTitle = false;
			private String title;
			
			//	output writer
			private BufferedLineWriter out = new BufferedLineWriter(bw) {
				public void write(String str) throws IOException {
					super.write(str.replaceAll(("\\/*" + RegExUtils.escapeForRegEx(IMAGE_BASE_PATH_VARIABLE) + "\\/*"), "/images/"));
				}
			};
			public void close() throws IOException {
				this.out.flush();
			}
			private void write(String s) throws IOException {
				this.out.write(s);
			}
			private void newLine() throws IOException {
				this.out.newLine();
			}
			private void writeLine(String s) throws IOException {
				this.out.write(s);
				this.out.newLine();
			}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (html.isTag(token)) {
					String type = html.getType(token);
					
					if ("includeFile".equals(type)) {
						TreeNodeAttributeSet as = TreeNodeAttributeSet.getTagAttributes(token, html);
						String includeFile = as.getAttribute("file");
						if (includeFile != null) 
							this.includeFile(includeFile);
					}
					
					else if (type.startsWith("include")) {
						if (!html.isEndTag(token))
							this.include(type, token);
					}
					
					//	page title
					else if ("title".equalsIgnoreCase(html.getType(token))) {
						if (html.isEndTag(token)) {
							this.write("<title>");
							this.write(this.getPageTitle(this.title));
							this.write("</title>");
							this.newLine();
							this.inTitle = false;
						}
						else this.inTitle = true;
					}
					
					//	page head
					else if ("head".equalsIgnoreCase(type) && html.isEndTag(token)) {
						
						//	write extensions to page head
						this.writePageHeadExtensions();
						
						//	close page head
						this.writeLine(token);
					}
					
					//	start of page body
					else if ("body".equalsIgnoreCase(type) && !html.isEndTag(token)) {
						
						//	include calls to doOnloadCalls() and doOnunloadCalls() functions
						this.writeLine("<body onload=\"doOnloadCalls();\" onunload=\"doOnunloadCalls();\">");
					}
					
					//	image, make link absolute
					else if ("img".equalsIgnoreCase(type)) {
						this.write(token);
					}
					
					//	other token
					else {
						
						//	write token
						this.write(token);
						
						// do not insert line break after hyperlink tags, bold tags, and span tags
						if (!"a".equalsIgnoreCase(type) && !"b".equalsIgnoreCase(type) && !"span".equalsIgnoreCase(type))
							this.newLine();
						
						//	remember being in hyperlink (for auto-activation)
						if ("a".equals(type))
							this.inHyperLink = !html.isEndTag(token);
					}
				}
				
				//	textual content
				else {
					
					//	remove spaces from links, and activate them
					if ((token.startsWith("http:") || token.startsWith("ftp:")) && (token.indexOf("tp: //") != -1)) {
						String link = token.replaceAll("\\s", "");
						if (!this.inHyperLink) this.write("<a mark=\"autoGenerated\" href=\"" + link + "\">");
						this.write(link);
						if (!this.inHyperLink) this.write("</a>");
					}
					
					//	store title to facilitate modification by sub classes
					else if (this.inTitle)
						this.title = token;
					
					//	other token, just write it
					else this.write(token);
				}
			}
			private String getPageTitle(String title) {
				return title;
			}
			private void writePageHeadExtensions() throws IOException {
				
				//	include CSS
				for (int c = 0; c < cssFiles.length; c++)
					this.writeLine("<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"" + cssFiles[c] + "\"></link>");
				
				//	include JavaScript
				for (int j = 0; j < javaScripts.length; j++)
					this.writeLine("<script type=\"text/javascript\" src=\"" + javaScripts[j] + "\"></script>");
				
				//	get JavaScript calls for page loading
				this.writeLine("<script type=\"text/javascript\">");
				this.writeLine("function doOnloadCalls() {");
				this.writeLine("  init();");
				this.writeLine("}");
				
				//	get JavaScript calls for page un-loading
				this.writeLine("function doOnunloadCalls() {");
				this.writeLine("}");
				this.writeLine("</script>");
				
				//	write servlet specific header
				FeedbackPanelHtmlTester.this.writePageHeadExtensions(this.out, fpr);
			}
			private void includeFile(String fileName) throws IOException {
				this.newLine();
				InputStream is = null;
				try {
					final TokenReceiver fTr = this;
					TokenReceiver fr = new TokenReceiver() {
						private boolean inBody = false;
						public void close() throws IOException {}
						public void storeToken(String token, int treeDepth) throws IOException {
							if (html.isTag(token) && "body".equalsIgnoreCase(html.getType(token))) {
								if (html.isEndTag(token))
									this.inBody = false;
								else
									this.inBody = true;
							}
							else if (this.inBody) fTr.storeToken(token, 0);
						}
					};
					if (dataProvider.isDataAvailable(fileName)) {
						is = new ByteOrderMarkFilterInputStream(dataProvider.getInputStream(fileName));
						htmlParser.stream(is, fr);
					}
					else this.storeToken("<!-- file '" + fileName + "' not found -->", 0);
				}
				catch (Exception e) {
					this.writeExceptionAsXmlComment(("exception including file '" + fileName + "'"), e);
				}
				finally {
					if (is != null)
						is.close();
				}
				this.newLine();
			}
			private void writeExceptionAsXmlComment(String label, Exception e) throws IOException {
				this.writeLine("<!-- " + label + ": " + e.getMessage());
				StackTraceElement[] ste = e.getStackTrace();
				for (int s = 0; s < ste.length; s++)
					this.writeLine("  " + ste[s].toString());
				this.writeLine("  " + label + " -->");
			}
			private void include(String type, String tag) throws IOException {
				if ("includeBody".equals(type))
					this.includeBody();
				else if ("includeLink".equals(type))
					this.includeLink(tag);
				else this.writeLine("<!-- include tag '" + type + "' not understood -->");
			}
			private void includeBody() throws IOException {
				this.writeLine("<form method=\"POST\" id=\"feedbackForm\" action=\"" + url + "\">");
				this.writeLine("<table class=\"feedbackTable\">");
				
				String label = cFp.getLabel();
				if (label != null) {
					this.writeLine("<tr>");
					this.writeLine("<td class=\"feedbackTableHeader\">");
					this.writeLine("<div style=\"border-width: 1; border-style: solid; border-color: #FF0000; padding: 5px; margin: 2px;\">");
					this.writeLine("<b>What to do in this dialog?</b>");
					this.writeLine("<br>");
					this.writeLine(FeedbackPanelHtmlRenderer.prepareForHtml(label));
					this.writeLine("</div>");
					this.writeLine("</td>");
					this.writeLine("</tr>");
				}
				
				this.writeLine("<tr>");
				this.writeLine("<td class=\"feedbackTableBody\">");
				fpr.writePanelBody(this.out);
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				this.writeLine("</tr>");
				this.writeLine("<td class=\"feedbackTableBody\">");
				this.writeLine("<input type=\"submit\" value=\"OK\" title=\"Submit feedback\" class=\"submitButton\">");
				this.writeLine("&nbsp;");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				this.writeLine("</table>");
				this.writeLine("</form>");
			}
			private void includeLink(String tag) throws IOException {
				TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(tag, html);
				String href = tnas.getAttribute("href");
				String onclick = tnas.getAttribute("onclick");
				if ((href == null) && (onclick == null)) return;
				else if (href == null) href = "#";
				
				String label = tnas.getAttribute("label");
				if (label == null) return;
				
				if ((href.indexOf('@') != -1) || ((onclick != null) && (onclick.indexOf('@') != -1))) {
					String[] pns = cFp.getPropertyNames();
					Arrays.sort(pns, new Comparator() {
						public int compare(Object o1, Object o2) {
							String s1 = ((String) o1);
							String s2 = ((String) o2);
							int c = (s2.length() - s1.length());
							return ((c == 0) ? s1.compareTo(s2) : c);
						}
					});
					for (int p = 0; p < pns.length; p++) {
						href = href.replaceAll(("\\@" + pns[p]), cFp.getProperty(pns[p]));
						if (onclick != null)
							onclick = onclick.replaceAll(("\\@" + pns[p]), cFp.getProperty(pns[p]));
					}
				}
				
				if ((href.indexOf('@') == -1) && ((onclick == null) || (onclick.indexOf('@') == -1))) {
					String target = null;
					if (onclick == null)
						target = "_blank";
					else {
						if (!onclick.endsWith(";"))
							onclick += ";";
						if (!onclick.endsWith("return false;"))
							onclick += " return false;";
					}
					String title = tnas.getAttribute("title");
					this.writeLine("<span class=\"functionLink\">" +
							"<a" +
								" href=\"" + href + "\"" + 
								((onclick == null) ? "" : (" onclick=\"" + onclick + "\"")) + 
								((title == null) ? "" : (" title=\"" + title + "\"")) + 
								((target == null) ? "" : (" target=\"" + target + "\"")) + 
							">" + label + "</a>" +
							"</span>");
				}
			}
		};
		InputStream is = new ByteOrderMarkFilterInputStream(this.dataProvider.getInputStream(this.basePage));
		try {
			htmlParser.stream(is, tr);
			tr.close();
		}
		finally {
			is.close();
		}
	}
	
	private void writePageHeadExtensions(BufferedLineWriter out, FeedbackPanelHtmlRendererInstance fpri) throws IOException {
		
		//	add JavaScript functions
		out.writeLine("<script type=\"text/javascript\">");
		
		//	add init() function
		out.writeLine("function init() {");
		fpri.writeJavaScriptInitFunctionBody(out);
		out.writeLine("}");
		
		//	add additional functions and variables
		fpri.writeJavaScript(out);
		
		//	close script
		out.writeLine("</script>");
		
		//	add CSS styles
		out.writeLine("<style type=\"text/css\">");
		fpri.writeCssStyles(out);
		out.writeLine("</style>");
	}
	
	private static class ByteOrderMarkFilterInputStream extends FilterInputStream {
		private boolean inContent = false;
		ByteOrderMarkFilterInputStream(InputStream in) {
			super(in);
		}
		
		/* (non-Javadoc)
		 * @see java.io.FilterInputStream#read()
		 */
		public int read() throws IOException {
			int i = super.read();
			while (!this.inContent) {
				if (i == '<') this.inContent = true;
				else i = super.read();
			}
			return i;
		}

		/* (non-Javadoc)
		 * @see java.io.FilterInputStream#read(byte[], int, int)
		 */
		public int read(byte[] b, int off, int len) throws IOException {
			if (this.inContent)	return super.read(b, off, len);
			else {
				int i = super.read();
				while (!this.inContent) {
					if (i == '<') this.inContent = true;
					else i = super.read();
				}
				b[off] = ((byte) i);
				return (1 + super.read(b, (off + 1), (len - 1)));
			}
		}
	}
	
	/**
	 * Render a feedback panel. This method is intended for two types of feedback
	 * panels: (a) ones that have their rendering methods on-board and thus need
	 * not rely on a specific renderer, and (b) ones that belong to the feedback
	 * API and use a default renderer, namely AnnotationEditorFeedbackPanel,
	 * AssignmentDisambiguationFeedbackPanel, CategorizationFeedbackPanel,
	 * CheckBoxFeedbackPanel, StartContinueFeedbackPanel, and
	 * StartContinueOtherFeedbackPanel. This method uses URF-8 encoding for
	 * writing to the target output stream. If a different encoding is required,
	 * create a respective writer externally and use the version of this method
	 * that takes a writer as an argument.
	 * @param fp the feedback panel to test
	 * @param out the output stream to write the rendering result to
	 */
	public static void renderFeedbackPanel(FeedbackPanel fp, OutputStream out) throws IOException {
		renderFeedbackPanel(fp, new OutputStreamWriter(out, "UTF-8"));
	}
	
	/**
	 * Render a feedback panel. This method is intended for two types of feedback
	 * panels: (a) ones that have their rendering methods on-board and thus need
	 * not rely on a specific renderer, and (b) ones that belong to the feedback
	 * API and use a default renderer, namely AnnotationEditorFeedbackPanel,
	 * AssignmentDisambiguationFeedbackPanel, CategorizationFeedbackPanel,
	 * CheckBoxFeedbackPanel, StartContinueFeedbackPanel, and
	 * StartContinueOtherFeedbackPanel.
	 * @param fp the feedback panel to test
	 * @param out the writer to write the rendering result to
	 */
	public static void renderFeedbackPanel(FeedbackPanel fp, Writer out) throws IOException {
		FeedbackPanelHtmlRenderer fpr;
		if (fp instanceof AssignmentDisambiguationFeedbackPanel)
			fpr = adfpr;
		else if (fp instanceof AnnotationEditorFeedbackPanel)
			fpr = aefpr;
		else if (fp instanceof CategorizationFeedbackPanel)
			fpr = cfpr;
		else if (fp instanceof CheckBoxFeedbackPanel)
			fpr = cbfpr;
		else if (fp instanceof StartContinueFeedbackPanel)
			fpr = scfpr;
		else if (fp instanceof StartContinueOtherFeedbackPanel)
			fpr = scofpr;
		else fpr = new FeedbackPanelHtmlRenderer() {
			public int canRender(FeedbackPanel fp) {
				return 0;
			}
			public FeedbackPanelHtmlRendererInstance getRendererInstance(FeedbackPanel fp) {
				return new FeedbackPanelHtmlRendererInstance(fp) {};
			}
		};
		renderFeedbackPanel(fp, out, fpr);
	}
	
	/**
	 * Render a feedback panel. This method is intended for feedback panels that
	 * have a custom renderer. This method can also be used to test alternative
	 * renderers for the standard feedback panels of the feedback API,
	 * AnnotationEditorFeedbackPanel, AssignmentDisambiguationFeedbackPanel,
	 * CategorizationFeedbackPanel, CheckBoxFeedbackPanel,
	 * StartContinueFeedbackPanel, and StartContinueOtherFeedbackPanel. This
	 * method uses URF-8 encoding for writing to the target output stream. If a
	 * different encoding is required, create a respective writer externally and
	 * use the version of this method that takes a writer as an argument.
	 * @param fp the feedback panel to test
	 * @param out the output stream to write the rendering result to
	 * @param fpr the HTML renderer to use
	 */
	public static void renderFeedbackPanel(FeedbackPanel fp, OutputStream out, FeedbackPanelHtmlRenderer fpr) throws IOException {
		renderFeedbackPanel(fp, new OutputStreamWriter(out, "UTF-8"), fpr);
	}
	
	/**
	 * Render a feedback panel. This method is intended for feedback panels that
	 * have a custom renderer. This method can also be used to test alternative
	 * renderers for the standard feedback panels of the feedback API,
	 * AnnotationEditorFeedbackPanel, AssignmentDisambiguationFeedbackPanel,
	 * CategorizationFeedbackPanel, CheckBoxFeedbackPanel,
	 * StartContinueFeedbackPanel, and StartContinueOtherFeedbackPanel.
	 * @param fp the feedback panel to test
	 * @param out the writer to write the rendering result to
	 * @param fpr the HTML renderer to use
	 */
	public static void renderFeedbackPanel(FeedbackPanel fp, Writer out, FeedbackPanelHtmlRenderer fpr) throws IOException {
		FeedbackPanelHtmlRendererInstance fpri = fpr.getRendererInstance(fp);
		renderFeedbackPanel(fp, out, fpri, "");
	}
	
	/**
	 * Test a feedback panel. This method is intended for two types of feedback
	 * panels: (a) ones that have their rendering methods on-board and thus need
	 * not rely on a specific renderer, and (b) ones that belong to the feedback
	 * API and use a default renderer, namely AnnotationEditorFeedbackPanel,
	 * AssignmentDisambiguationFeedbackPanel, CategorizationFeedbackPanel,
	 * CheckBoxFeedbackPanel, StartContinueFeedbackPanel, and
	 * StartContinueOtherFeedbackPanel.
	 * @param fp the feedback panel to test
	 * @param port the port to use for the test HTTP server (a non-positive
	 *            value will result in a random high-port (between 8192 and
	 *            65535) being used)
	 * @return the status code of the feedback panel on closing, usually 'OK'
	 */
	public static String testFeedbackPanel(FeedbackPanel fp, int port) throws IOException {
		FeedbackPanelHtmlRenderer fpr;
		if (fp instanceof AssignmentDisambiguationFeedbackPanel)
			fpr = adfpr;
		else if (fp instanceof AnnotationEditorFeedbackPanel)
			fpr = aefpr;
		else if (fp instanceof CategorizationFeedbackPanel)
			fpr = cfpr;
		else if (fp instanceof CheckBoxFeedbackPanel)
			fpr = cbfpr;
		else if (fp instanceof StartContinueFeedbackPanel)
			fpr = scfpr;
		else if (fp instanceof StartContinueOtherFeedbackPanel)
			fpr = scofpr;
		else fpr = new FeedbackPanelHtmlRenderer() {
			public int canRender(FeedbackPanel fp) {
				return 0;
			}
			public FeedbackPanelHtmlRendererInstance getRendererInstance(FeedbackPanel fp) {
				return new FeedbackPanelHtmlRendererInstance(fp) {};
			}
		};
		return testFeedbackPanel(fp, port, fpr);
	}
	
	/**
	 * Test a feedback panel. This method is intended for feedback panels that
	 * have a custom renderer. This method can also be used to test alternative
	 * renderers for the standard feedback panels of the feedback API,
	 * AnnotationEditorFeedbackPanel, AssignmentDisambiguationFeedbackPanel,
	 * CategorizationFeedbackPanel, CheckBoxFeedbackPanel,
	 * StartContinueFeedbackPanel, and StartContinueOtherFeedbackPanel. This
	 * method uses URF-8 encoding for writing to the target output stream. If a
	 * different encoding is required, create a respective writer externally and
	 * use the version of this method that takes a writer as an argument.
	 * @param fp the feedback panel to test
	 * @param port the port to use for the test HTTP server (a non-positive
	 *            value will result in a random high-port (between 8192 and
	 *            65535) being used)
	 * @param fpr the HTML renderer to use
	 * @return the status code of the feedback panel on closing, usually 'OK'
	 */
	public static String testFeedbackPanel(FeedbackPanel fp, int port, FeedbackPanelHtmlRenderer fpr) throws IOException {
		
		//	correct port
		if (port < 1) port = 8192 + ((int) (Math.random() * (65536 - 8192)));
		
		//	open HTTP server
		ServerSocket ss = new ServerSocket(port);
		
		//	generate URL
		String actionUrl = ("http://localhost:" + port);
		
		//	prompt URL to user
		JOptionPane.showInputDialog(null, "Please copy and paste the URL to your browser to view the HTML version of your feedback panel", "Test URL", JOptionPane.INFORMATION_MESSAGE, null, null, actionUrl);
		
		//	await request
		Socket fpRequest = ss.accept();
		
		//	render feedback panel as response to request
		BufferedReader fpRequestReader = new BufferedReader(new InputStreamReader(fpRequest.getInputStream()));
		String fpRequestLine;
		while (((fpRequestLine = fpRequestReader.readLine()) != null) && (fpRequestLine.length() != 0)) {
			System.out.println(fpRequestLine);
		}
		System.out.println("===== Request End =====");
		
		BufferedWriter fpWriter = new BufferedWriter(new OutputStreamWriter(fpRequest.getOutputStream()));
		String fpDateTime = dateFormat.format(new Date());
		fpWriter.write("HTTP/1.1 200 OK");
		fpWriter.newLine();
		fpWriter.write("Date: " + fpDateTime);
		fpWriter.newLine();
		fpWriter.write("Server: Feedback Panel Test Server");
		fpWriter.newLine();
		fpWriter.write("Last-Modified: " + fpDateTime);
		fpWriter.newLine();
		fpWriter.write("Etag: \"" + fp.getClass().getName() + "-" + fp.hashCode() + "\"");
		fpWriter.newLine();
		fpWriter.write("Accept-Ranges: bytes");
		fpWriter.newLine();
		fpWriter.write("Connection: close");
		fpWriter.newLine();
		fpWriter.write("Content-Type: text/html; charset=UTF-8");
		fpWriter.newLine();
		fpWriter.write("");
		fpWriter.newLine();
/*	
 HTTP/1.1 200 OK
 Date: Mon, 23 May 2005 22:38:34 GMT
 Server: Apache/1.3.3.7 (Unix)  (Red-Hat/Linux)
 Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT
 Etag: "3f80f-1b6-3e1cb03b"
 Accept-Ranges: bytes
 Content-Length: 438
 Connection: close
 Content-Type: text/html; charset=UTF-8
 */		
		FeedbackPanelHtmlRendererInstance fpri = fpr.getRendererInstance(fp);
		renderFeedbackPanel(fp, fpWriter, fpri, actionUrl);
		fpWriter.flush();
		fpWriter.close();
		fpRequestReader.close();
		fpRequest.close();
		
		//	await response
		Socket fpResponse;
		BufferedReader fpResponseReader;
		String fpResponseLine;
		String fpResponseLookahead;
		do {
			fpResponse = ss.accept();
			fpResponseReader = new BufferedReader(new InputStreamReader(fpResponse.getInputStream()));
			fpResponseReader.mark(1024);
			fpResponseLookahead = fpResponseReader.readLine();
			fpResponseReader.reset();
			
			//	some subsequent load request ...
			if (fpResponseLookahead.startsWith("GET")) {
				while (((fpResponseLine = fpResponseReader.readLine()) != null) && (fpResponseLine.length() != 0)) {
					System.out.println(fpResponseLine);
				}
				System.out.println("===== SubRequest End =====");
				fpResponse.close();
			}
			
			//	finally the response
			else {}
		} while (fpResponseLookahead.startsWith("GET"));
		
		//	read response header
		int fpResponseLength = Integer.MAX_VALUE;
		while (((fpResponseLine = fpResponseReader.readLine()) != null) && (fpResponseLine.length() != 0)) {
			System.out.println(fpResponseLine);
			if (fpResponseLine.startsWith("Content-Length: "))
				fpResponseLength = Integer.parseInt(fpResponseLine.substring("Content-Length: ".length()));
		}
		System.out.println("===== Response Header End =====");
		
		StringBuffer fpResonseBody = new StringBuffer();
		int fpResonseChar;
		int fpResponseRead = 0;
		while ((fpResponseRead++ < fpResponseLength) && ((fpResonseChar = fpResponseReader.read()) != -1)) {
			fpResonseBody.append((char) fpResonseChar);
		}
		System.out.println(fpResonseBody.toString());
		
		//	parse response
		Properties response = new Properties();
		String[] kvPairs = fpResonseBody.toString().split("\\&");
		for (int p = 0; p < kvPairs.length; p++) {
			int kvSplit = kvPairs[p].indexOf('=');
			if (kvSplit != -1) {
				String key = kvPairs[p].substring(0, kvSplit);
				String value = URLDecoder.decode(kvPairs[p].substring(kvSplit + 1), "UTF-8");
				System.out.println("  - " + key + " = '" + value + "'");
				if (value.length() != 0)
					response.setProperty(key, value);
			}
		}
		System.out.println("===== Response End =====");
		
		//	show result in browser
		BufferedWriter frWriter = new BufferedWriter(new OutputStreamWriter(fpResponse.getOutputStream()));
		String frDateTime = dateFormat.format(new Date());
		frWriter.write("HTTP/1.1 200 OK");
		frWriter.newLine();
		frWriter.write("Date: " + frDateTime);
		frWriter.newLine();
		frWriter.write("Server: Feedback Panel Test Server");
		frWriter.newLine();
		frWriter.write("Last-Modified: " + frDateTime);
		frWriter.newLine();
		frWriter.write("Etag: \"" + fp.getClass().getName() + "-" + fp.hashCode() + "\"");
		frWriter.newLine();
		frWriter.write("Accept-Ranges: bytes");
		frWriter.newLine();
		frWriter.write("Connection: close");
		frWriter.newLine();
		frWriter.write("Content-Type: text/html; charset=UTF-8");
		frWriter.newLine();
		frWriter.write("");
		frWriter.newLine();
		
		frWriter.write("<html><body>");
		frWriter.newLine();
		frWriter.write("<ul>");
		frWriter.newLine();
		
		ArrayList keys = new ArrayList(response.keySet());
		Collections.sort(keys);
		for (int k = 0; k < keys.size(); k++) {
			String key = keys.get(k).toString();
			String value = response.getProperty(key);
			frWriter.write("<li>" + key + " = " + ((value == null) ? "null" : ("'" + value + "'")) + "</li>");
			frWriter.newLine();
		}
		
		frWriter.write("</ul>");
		frWriter.newLine();
		frWriter.write("<body></html>");
		frWriter.newLine();
		
		frWriter.flush();
		frWriter.close();
		fpResponseReader.close();
		fpResponse.close();
		
		//	close HTTP server
		ss.close();
		
		//	feed response to feedback panel
		fpri.readResponse(response);
		fp.setStatusCode("OK");
		
		//	return status code
		return fp.getStatusCode();
	}
	
	private static void renderFeedbackPanel(FeedbackPanel fp, Writer out, FeedbackPanelHtmlRendererInstance fpri, String actionUrl) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		
		bw.write("<html><head>");
		bw.newLine();
		bw.write("<title>" + fp.getTitle() + "</title>");
		bw.newLine();
		
		//	add JavaScript functions
		bw.write("<script type=\"text/javascript\">");
		bw.newLine();
		
		Dimension lPs = ((fp.getLabel() == null) ? new Dimension(0, 0) : new Dimension(0, 50));
		Dimension fpPs = fp.getPreferredSize();
		Dimension bpPs = new Dimension(0, 50);
		
		//	add init() function
		bw.write("function init() {");
		bw.newLine();
		
		if ((fpPs != null) && (bpPs != null) && (lPs != null)) {
			bw.write("  var maxWidth = ((screen.width * 2) / 3);");
			bw.newLine();
			bw.write("  var maxHeight = ((screen.height * 2) / 3);");
			bw.newLine();
			bw.write("  var panelWidth = " + (Math.max(lPs.width, Math.max(fpPs.width, bpPs.width)) + 10) + ";");
			bw.newLine();
			bw.write("  var panelHeight = " + (lPs.height + fpPs.height + bpPs.height + 50) + ";");
			bw.newLine();
			bw.write("  if ((panelWidth > maxWidth) || (panelHeight > maxHeight)) {");
			bw.newLine();
			bw.write("    panelWidth = Math.min((panelWidth + 50), maxWidth);");
			bw.newLine();
			bw.write("    panelHeight = Math.min(panelHeight, maxHeight);");
			bw.newLine();
			bw.write("  }");
			bw.newLine();
		}
		else {
			bw.write("  var panelWidth = ((screen.width * 2) / 3);");
			bw.newLine();
			bw.write("  var panelHeight = ((screen.height * 2) / 3);");
			bw.newLine();
		}
		
		bw.write("  panelWidth = Math.max(panelWidth, window.outerWidth);");
		bw.newLine();
		bw.write("  panelHeight = Math.max(panelHeight, window.outerHeight);");
		bw.newLine();
		bw.write("  if (navigator.appName == 'Netscape')"); // next command causes trouble in IE, and chaotic behavior in Chrome
		bw.newLine();
		bw.write("    window.resizeTo(panelWidth, panelHeight);");
		bw.newLine();
		bw.write("  else {"); // we're in IE, Chrome, or something else ... just try, might not work 
		bw.newLine();
		bw.write("    window.outerWidth = panelWidth;");
		bw.newLine();
		bw.write("    window.outerHeight = panelHeight;");
		bw.newLine();
		bw.write("  }");
		bw.newLine();
		bw.newLine();
		fpri.writeJavaScriptInitFunctionBody(bw);
		bw.write("}");
		bw.newLine();
		bw.newLine();
		
		//	add checkFeedback() function
		bw.write("function checkFeedback() {"); bw.newLine();
		fpri.writeJavaScriptCheckFeedbackFunctionBody(bw);
		bw.newLine();
		bw.write("  return true;"); bw.newLine();
		bw.write("}"); bw.newLine();
		
		//	add checkSubmit() function
		bw.write("function checkSubmit() {"); bw.newLine();
		bw.write("  var ok = checkFeedback();"); bw.newLine();
//		bw.write("  if (ok)"); bw.newLine();
//		bw.write("    doOnsubmitCalls();"); bw.newLine();
		bw.write("  if (ok)"); bw.newLine();
		bw.write("    prepareSubmit();"); bw.newLine();
		bw.write("  return ok;"); bw.newLine();
		bw.write("}"); bw.newLine();
		
		//	add prepareSubmit() function
		bw.write("function prepareSubmit() {"); bw.newLine();
		fpri.writeJavaScriptPrepareSubmitFunctionBody(bw);
		bw.write("}"); bw.newLine();
		
		//	add additional functions and variables
		fpri.writeJavaScript(bw);
		bw.newLine();
		
//		//	add submit mode function
//		bw.write("function setSubmitMode(mode) {");
//		bw.newLine();
//		bw.write("  document.getElementById('feedbackForm')['" + SUBMIT_MODE_PARAMETER + "'].value = mode;");
//		bw.newLine();
//		bw.write("}");
//		bw.newLine();
		
		bw.write("</script>");
		bw.newLine();
		
		
		//	add CSS styles
		bw.write("<style type=\"text/css\">");
		bw.newLine();
		fpri.writeCssStyles(bw);
		bw.write("</style>");
		bw.newLine();
		
		
		bw.write("</head>");
		bw.newLine();
		
		bw.write("<body onload=\"init();\">");
		bw.newLine();
		
		
		bw.write("<form method=\"POST\" id=\"feedbackForm\" action=\"" + (actionUrl.startsWith("http://") ? "" : "http://") + actionUrl + "\" onsubmit=\"return checkSubmit();\">");
		bw.newLine();
//		bw.write("<form method=\"POST\" id=\"feedbackForm\" action=\"" + "http://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + request.getServletPath() + "\">");
//		bw.newLine();
//		bw.write("<input type=\"hidden\" name=\"" + REQUEST_ID_PARAMETER + "\" value=\"" + fr.id + "\">");
//		bw.newLine();
//		bw.write("<input type=\"hidden\" name=\"" + SUBMIT_MODE_PARAMETER + "\" value=\"" + CANCEL_SUBMIT_MODE + "\">");
//		bw.newLine();
		bw.write("<table class=\"feedbackTable\">");
		bw.newLine();
		
		String label = fp.getLabel();
		if (label != null) {
			bw.write("<tr>");
			bw.newLine();
			bw.write("<td class=\"feedbackTableHeader\">");
			bw.newLine();
			bw.write("<div style=\"border-width: 1; border-style: solid; border-color: #FF0000; padding: 5px; margin: 2px;\">");
			bw.newLine();
			bw.write("<b>What to do in this dialog?</b>");
			bw.newLine();
			bw.write("<br>");
			bw.newLine();
			bw.write(FeedbackPanelHtmlRenderer.prepareForHtml(label));
			bw.newLine();
			bw.write("</div>");
			bw.newLine();
			bw.write("</td>");
			bw.newLine();
			bw.write("</tr>");
			bw.newLine();
		}
		
		bw.write("<tr>");
		bw.newLine();
		bw.write("<td class=\"feedbackTableBody\">");
		bw.newLine();
		fpri.writePanelBody(bw);
		bw.write("</td>");
		bw.newLine();
		bw.write("</tr>");
		bw.newLine();
		
		bw.write("<tr>");
		bw.newLine();
		bw.write("<td class=\"feedbackTableBody\">");
		bw.newLine();
		bw.write("<input type=\"submit\" value=\"OK\" title=\"Submit feedback\" class=\"submitButton\">");
		bw.newLine();
		bw.write("</td>");
		bw.newLine();
		bw.write("</tr>");
		bw.newLine();
		
		bw.write("</table>");
		bw.newLine();
		bw.write("</form>");
		bw.newLine();
		
		bw.write("</body></html>");
		bw.newLine();
		
		if (bw != out) bw.flush();
	}
	
	public static void main(String args[]) throws Exception {
		CheckBoxFeedbackPanel cbfp = new CheckBoxFeedbackPanel();
		String[] lines = {"Test 1", "Test 2 with a somewhat longer text", "Test 3", "Test 4"};
		boolean[] selected = {false, true, false, false};
		for (int t = 0; t < lines.length; t++)
			cbfp.addLine(lines[t], selected[t]);
		
//		testFeedbackPanel(cbfp, 0);
		String[] csss = {"feedbackClientLayout.css"};
		FeedbackPanelHtmlTester fpht = new FeedbackPanelHtmlTester(8888, new File("E:/GoldenGATEv3.WebApp/feedbackData/"), "feedback.html", csss, null, null);
		fpht.setActive(true);
		fpht.getFeedback(cbfp);
		//renderFeedbackPanel(cbfp, System.out);
	}
}

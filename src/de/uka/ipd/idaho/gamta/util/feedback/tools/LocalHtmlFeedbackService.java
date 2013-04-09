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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import javax.swing.JOptionPane;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.AnnotationEditorFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.AssignmentDisambiguationFeedbackPanelRenderer;
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
 * // activate local HTML feedback service
 * LocalHtmlFeedbackService.activate();
 * 
 * // normally, you would get feedback like this
 * feedback = myFeedbackPanel.getFeedback();
 * 
 * // ... proceede with regular application code
 * </pre></code>
 * 
 * @author sautter
 */
public class LocalHtmlFeedbackService implements FeedbackService {
	
	private int port = -1;
	private ServerSocket ss;
	private String actionUrl;
	private boolean active = false;
	
	private LocalHtmlFeedbackService() {
		FeedbackPanel.addFeedbackService(this);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getPriority()
	 */
	public int getPriority() {
		return 11;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#canGetFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public boolean canGetFeedback(FeedbackPanel fp) {
		return this.active;
	}
	
	private void setActive(boolean active) {
		if (active == this.active)
			return;
		this.active = active;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public void getFeedback(FeedbackPanel fp) {
//		
//		FeedbackPanelHtmlRenderer fpr;// = FeedbackPanelHtmlRenderer.getRenderer(fp);
//		if (fp instanceof AssignmentDisambiguationFeedbackPanel)
//			fpr = adfpr;
//		else if (fp instanceof AnnotationEditorFeedbackPanel)
//			fpr = aefpr;
//		else if (fp instanceof CategorizationFeedbackPanel)
//			fpr = cfpr;
//		else if (fp instanceof CheckBoxFeedbackPanel)
//			fpr = cbfpr;
//		else if (fp instanceof StartContinueFeedbackPanel)
//			fpr = scfpr;
//		else if (fp instanceof StartContinueOtherFeedbackPanel)
//			fpr = scofpr;
//		else fpr = new FeedbackPanelHtmlRenderer() {
//			public int canRender(FeedbackPanel fp) {
//				return 0;
//			}
//			public FeedbackPanelHtmlRendererInstance getRendererInstance(FeedbackPanel fp) {
//				return new FeedbackPanelHtmlRendererInstance(fp) {};
//			}
//		};
		
		if (this.port == -1) {
			this.port = 8192 + ((int) (Math.random() * (65536 - 8192)));
//			
//			//	open HTTP server
//			try {
//				this.ss = new ServerSocket(port);
//			}
//			catch (IOException ioe) {
//				ioe.printStackTrace(System.out);
//				return;
//			}
			
			//	generate URL
			this.actionUrl = ("http://localhost:" + port);
			
			//	prompt URL to user
			JOptionPane.showInputDialog(null, "Plesae copy and paste the URL to your browser to view the HTML version of your feedback panel", "Test URL", JOptionPane.INFORMATION_MESSAGE, null, null, actionUrl);
		}
		
		//	open HTTP server
		if (this.ss == null) try {
			this.ss = new ServerSocket(port);
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
			return;
		}
		
		try {
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
//			FeedbackPanelHtmlRendererInstance fpri = fpr.getRendererInstance(fp);
			FeedbackPanelHtmlRendererInstance fpri = FeedbackPanelHtmlRenderer.getRenderer(fp);
			renderFeedbackPanel(fp, fpWriter, fpri, this.actionUrl);
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
					System.out.println("===== SubRequest Start =====");
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
			frWriter.write("<a href=\"" + this.actionUrl + "\">Continue</a>");
			frWriter.newLine();
			frWriter.write("<body></html>");
			frWriter.newLine();
			
			frWriter.flush();
			frWriter.close();
			fpResponseReader.close();
			fpResponse.close();
			
			//	feed response to feedback panel
			fpri.readResponse(response);
			fp.setStatusCode("OK");
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getMultiFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel[])
	 */
	public void getMultiFeedback(FeedbackPanel[] fps) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("MultiFeedback is not available.");
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
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	private static AnnotationEditorFeedbackPanelRenderer aefpr = new AnnotationEditorFeedbackPanelRenderer();
	private static AssignmentDisambiguationFeedbackPanelRenderer adfpr = new AssignmentDisambiguationFeedbackPanelRenderer();
	private static CategorizationFeedbackPanelRenderer cfpr = new CategorizationFeedbackPanelRenderer();
	private static CheckBoxFeedbackPanelRenderer cbfpr = new CheckBoxFeedbackPanelRenderer();
	private static StartContinueFeedbackPanelRenderer scfpr = new StartContinueFeedbackPanelRenderer();
	private static StartContinueOtherFeedbackPanelRenderer scofpr = new StartContinueOtherFeedbackPanelRenderer();
	
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM, yyyy hh:mm:ss z");
	
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
		
		//	add additional functions and variables
		fpri.writeJavaScript(bw);
		bw.newLine();
		
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
		
		
		bw.write("<form method=\"POST\" id=\"feedbackForm\" action=\"" + (actionUrl.startsWith("http://") ? "" : "http://") + actionUrl + "\">");
		bw.newLine();
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
//		//renderFeedbackPanel(cbfp, System.out);
	}
	
	private static LocalHtmlFeedbackService activeInstance = null;
	private static Thread shutdownHook = null;
	
	public static void activate() {
		if (activeInstance == null) {
			activeInstance = new LocalHtmlFeedbackService();
			shutdownHook = new Thread() {
				public void run() {
					activeInstance.shutdown();
				}
			};
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
		activeInstance.setActive(true);
//		FeedbackPanel.setFeedbackService(activeInstance);
	}
	
	public static void deactivate() {
		if (activeInstance != null) {
			activeInstance.setActive(false);
//			Runtime.getRuntime().removeShutdownHook(shutdownHook);
//			activeInstance.shutdown();
//			activeInstance = null;
//			shutdownHook = null;
		}
//		FeedbackPanel.setFeedbackService(null);
	}
}

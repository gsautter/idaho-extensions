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
package de.uka.ipd.idaho.gamta.util.feedback.html;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance;
import de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;

/**
 * HTML page builder specialized in forms displaying feedback panels in a
 * browser.
 * 
 * @author sautter
 */
public abstract class FeedbackFormBuilder extends HtmlPageBuilder {
	
	/**
	 * Data container for the submit buttons to embed in the feedback form.
	 * 
	 * @author sautter
	 */
	public static class SubmitMode {
		public final String name;
		public final String label;
		public final String tooltip;
		/**
		 * @param name
		 * @param label
		 * @param tooltip
		 */
		public SubmitMode(String name, String label, String tooltip) {
			this.name = name;
			this.label = label;
			this.tooltip = tooltip;
		}
	}
	
	/**
	 * the name of the HTML form parameter holding the selected submit mode,
	 * i.e., the button a user clicked to submit the feedback form.
	 */
	public static final String SUBMIT_MODE_PARAMETER = "submitMode";
	
	/**
	 * the name of the HTML form parameter holding the feedback request ID, so
	 * to facilitate associating the form data with a feedback panel.
	 */
	public static final String REQUEST_ID_PARAMETER = "requestId";
	
	final FeedbackPanel fp;
	final FeedbackPanelHtmlRendererInstance fpRenderer;
	
	final SubmitMode[] submitModes;
	final String requestId;
	
	/**
	 * Constructor
	 * @param host the page builder host to work with
	 * @param request the HTTP request retrieving the feedback form, required
	 *            for the form submission address
	 * @param response the HTTP response to send the form page to
	 * @param fp the feedback panel to display in the form
	 * @param submitModes the form submit modes, i.e., the submit buttons to
	 *            show
	 * @param requestId the feedback request ID
	 * @throws IOException
	 */
	public FeedbackFormBuilder(HtmlPageBuilderHost host, HttpServletRequest request, HttpServletResponse response, FeedbackPanel fp, SubmitMode[] submitModes, String requestId) throws IOException {
		this(host, request, response, fp, FeedbackPanelHtmlRenderer.getRenderer(fp), submitModes, requestId);
	}
	
	/**
	 * Constructor
	 * @param host the page builder host to work with
	 * @param request the HTTP request retrieving the feedback form, required
	 *            for the form submission address
	 * @param response the HTTP response to send the form page to
	 * @param fp the feedback panel to display in the form
	 * @param fpRenderer the renderer for the feedback panel
	 * @param submitModes the form submit modes, i.e., the submit buttons to
	 *            show
	 * @param requestId the feedback request ID
	 * @throws IOException
	 */
	public FeedbackFormBuilder(HtmlPageBuilderHost host, HttpServletRequest request, HttpServletResponse response, FeedbackPanel fp, FeedbackPanelHtmlRendererInstance fpRenderer, SubmitMode[] submitModes, String requestId) throws IOException {
		super(host, request, response);
		this.fp = fp;
		this.fpRenderer = fpRenderer;
		this.submitModes = submitModes;
		this.requestId = requestId;
	}
	
	/**
	 * Constructor
	 * @param host the page builder host to work with
	 * @param request the HTTP request retrieving the feedback form, required
	 *            for the form submission address
	 * @param response the HTTP response to send the form page to
	 * @param fr the feedback request to display in the form
	 * @param submitModes the form submit modes, i.e., the submit buttons to
	 *            show
	 * @throws IOException
	 */
	public FeedbackFormBuilder(HtmlPageBuilderHost host, HttpServletRequest request, HttpServletResponse response, FeedbackRequest fr, SubmitMode[] submitModes) throws IOException {
		this(host, request, response, fr.fp, fr.fpRenderer, submitModes, fr.id);
	}
	
	/**
	 * This implementation handles the &lt;includeBody/&gt; and
	 * &lt;includeLink/&gt; marker tags. Thus, sub classes overwriting this
	 * method have to make the super call.
	 * @param type the HTML element name of the marker tag
	 * @param tag the HTML tag as a whole, to enable attribute access
	 */
	protected void include(String type, String tag) throws IOException {
		if ("includeBody".equals(type))
			this.includeBody();
		else if ("includeLink".equals(type)) {
			TreeNodeAttributeSet as = TreeNodeAttributeSet.getTagAttributes(tag, html);
			this.includeLink(as);
		}
		else super.include(type, tag);
	}
	
	void writeStylesAndScripts() throws IOException {
		
		//	add JavaScript functions
		this.writeLine("<script type=\"text/javascript\">");
		
		//	add init() function
		this.writeLine("function init() {");
		this.fpRenderer.writeJavaScriptInitFunctionBody(this.asWriter());
		this.writeLine("}");
		
		//	add checkFeedback() function
		this.writeLine("function checkFeedback() {");
		this.fpRenderer.writeJavaScriptCheckFeedbackFunctionBody(this.asWriter());
		this.newLine();
		this.writeLine("  return true;");
		this.writeLine("}");
		
		//	add prepareSubmit() function
		this.writeLine("function prepareSubmit() {");
		this.fpRenderer.writeJavaScriptPrepareSubmitFunctionBody(this.asWriter());
		this.writeLine("}");
		
		//	add doOnsubmitCalls() function
		String[] oscs = this.getOnsubmitCalls();
		this.writeLine("function doOnsubmitCalls() {");
		this.writeLine("  prepareSubmit();");
		if (oscs != null)
			for (int f = 0; f < oscs.length; f++) {
				String call = oscs[f];
				if (call != null)
					this.writeLine(call + (call.endsWith(";") ? "" : ";"));
			}
		this.writeLine("}");
		
		//	add checkSubmit() function
		this.writeLine("function checkSubmit() {");
		this.writeLine("  var ok = checkFeedback();");
		this.writeLine("  if (ok)");
		this.writeLine("    doOnsubmitCalls();");
		this.writeLine("  return ok;");
		this.writeLine("}");
		
		//	add additional functions and variables
		this.fpRenderer.writeJavaScript(this.asWriter());
		
		//	add submit mode handling functions
		this.writeLine("var submitMode;");
		this.writeLine("function getSubmitMode() {");
		this.writeLine("  return submitMode;");
		this.writeLine("}");
		this.writeLine("function setSubmitMode(mode) {");
		this.writeLine("  submitMode = mode;");
//		this.writeLine("  document.getElementById('feedbackForm')['" + SUBMIT_MODE_PARAMETER + "'].value = mode;");
		this.writeLine("  document.getElementById('" + SUBMIT_MODE_PARAMETER + "_field').value = mode;");
		this.writeLine("}");
		
		this.writeLine("</script>");
		
		//	add CSS styles
		this.writeLine("<style type=\"text/css\">");
		this.fpRenderer.writeCssStyles(this.asWriter());
		this.writeLine("</style>");
	}
	
	void writeFormContentHtml() throws IOException {
//		this.writeLine("<input type=\"hidden\" name=\"" + SUBMIT_MODE_PARAMETER + "\" value=\"\">");
		this.writeLine("<input type=\"hidden\" id=\"" + SUBMIT_MODE_PARAMETER + "_field\" name=\"" + SUBMIT_MODE_PARAMETER + "\" value=\"\">");
		this.writeLine("<input type=\"hidden\" name=\"" + REQUEST_ID_PARAMETER + "\" value=\"" + this.requestId + "\">");
		this.writeHiddenFields();
		this.writeLine("<table class=\"feedbackTable\">");
		
		String label = this.fp.getLabel();
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
		this.fpRenderer.writePanelBody(this.asWriter());
		this.writeLine("</td>");
		this.writeLine("</tr>");
		
		this.writeLine("<tr>");
		this.writeLine("<td class=\"feedbackTableBody\">");
		for (int s = 0; s < this.submitModes.length; s++) {
			if (s != 0)
				this.writeLine("&nbsp;");
			this.writeLine("<input type=\"submit\" value=\"" + this.submitModes[s].label + "\" title=\"" + this.submitModes[s].tooltip + "\" onclick=\"setSubmitMode('" + this.submitModes[s].name + "');\" class=\"submitButton\">");
		}
		this.writeLine("</td>");
		this.writeLine("</tr>");
		
		this.writeLine("</table>");
	}
	
	/**
	 * Retrieve an array holding the JavaScript commands to execute when the
	 * feedback form is submitted in a browser. The calls are executed only if
	 * the checkFeedback() function returns true. This default implementation
	 * returns an empty array, sub classes are welcome to overwrite it as
	 * needed.
	 * @return an array holding the JavaScript commands to execute when the
	 *         feedback form is submitted
	 */
	protected String[] getOnsubmitCalls() {
		return new String[0];
	}
	
	/**
	 * Retrieve a path info part for the action URL of the feedback form, to be
	 * appended to context path and servlet path. If this method returns a
	 * non-null result, the returned string has to be a valid part for a URL.
	 * This default implementation returns null, sub classes are welcome to
	 * overwrite it as needed.
	 * @return the form action path info
	 */
	protected String getFormActionPathInfo() {
		return null;
	}
	
	/**
	 * Write the HTML form body. This method is called when the storeToken()
	 * method encounters the &lt;includeBody/&gt; marker tag.
	 * @throws IOException
	 */
	protected final void includeBody() throws IOException {
		String actionPathInfo = this.getFormActionPathInfo();
		if (actionPathInfo == null)
			actionPathInfo = "";
		else if ((actionPathInfo.length() != 0) && !actionPathInfo.startsWith("/"))
			actionPathInfo = ("/" + actionPathInfo);
		
		this.writeLine("<form method=\"POST\" id=\"feedbackForm\" action=\"" + this.request.getContextPath() + this.request.getServletPath() + actionPathInfo + "\" onsubmit=\"return checkSubmit();\">");
		this.writeFormContent();
		this.writeLine("</form>");
	}
	
	abstract void writeFormContent() throws IOException;
	
	/**
	 * Write implementation specific hidden parameters to the HTML form body.
	 * This default implementation does nothing, sub classes are welcome to
	 * overwrite it as needed.
	 * @throws IOException
	 */
	protected void writeHiddenFields() throws IOException {}
	
	/**
	 * Include a hyperlink in the form, replacing variables with properties of
	 * the wrapped feedback panel.
	 * @param tnas the attributes of HTML element representing the generic link
	 * @throws IOException
	 */
	protected void includeLink(TreeNodeAttributeSet tnas) throws IOException {
		String href = tnas.getAttribute("href");
		String onclick = tnas.getAttribute("onclick");
		if ((href == null) && (onclick == null)) return;
		else if (href == null) href = "#";
		
		String label = tnas.getAttribute("label");
		if (label == null) return;
		
		if ((href.indexOf('@') != -1) || ((onclick != null) && (onclick.indexOf('@') != -1))) {
			String[] pns = this.fp.getPropertyNames();
			Arrays.sort(pns, new Comparator() {
				public int compare(Object o1, Object o2) {
					String s1 = ((String) o1);
					String s2 = ((String) o2);
					int c = (s2.length() - s1.length());
					return ((c == 0) ? s1.compareTo(s2) : c);
				}
			});
			for (int p = 0; p < pns.length; p++) {
				href = href.replaceAll(("\\@" + pns[p]), this.fp.getProperty(pns[p]));
				if (onclick != null)
					onclick = onclick.replaceAll(("\\@" + pns[p]), this.fp.getProperty(pns[p]));
			}
			href = href.replaceAll("\\@ContextAddress", this.request.getContextPath());
			if (onclick != null)
				onclick = onclick.replaceAll("\\@ContextAddress", this.request.getContextPath());
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
}

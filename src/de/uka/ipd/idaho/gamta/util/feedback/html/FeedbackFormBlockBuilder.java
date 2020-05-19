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
package de.uka.ipd.idaho.gamta.util.feedback.html;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance;
import de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest;

/**
 * HTML page builder specialized in forms displaying feedback panels in a
 * browser. As opposed to FeedbackFormPageBuilder, instances of this class
 * output all code relevant for the feedback form inside the form element,
 * including CSS styles and JavaScript code. This is to enable client code to
 * place feedback forms more flexibly, in particular without having to capture
 * parts of the page head.
 * 
 * @author sautter
 */
public class FeedbackFormBlockBuilder extends FeedbackFormBuilder {
	
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
	public FeedbackFormBlockBuilder(HtmlPageBuilderHost host, HttpServletRequest request, HttpServletResponse response, FeedbackPanel fp, SubmitMode[] submitModes, String requestId) throws IOException {
		super(host, request, response, fp, FeedbackPanelHtmlRenderer.getRenderer(fp), submitModes, requestId);
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
	public FeedbackFormBlockBuilder(HtmlPageBuilderHost host, HttpServletRequest request, HttpServletResponse response, FeedbackPanel fp, FeedbackPanelHtmlRendererInstance fpRenderer, SubmitMode[] submitModes, String requestId) throws IOException {
		super(host, request, response, fp, fpRenderer, submitModes, requestId);
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
	public FeedbackFormBlockBuilder(HtmlPageBuilderHost host, HttpServletRequest request, HttpServletResponse response, FeedbackRequest fr, SubmitMode[] submitModes) throws IOException {
		super(host, request, response, fr.fp, fr.fpRenderer, submitModes, fr.id);
	}
	
	/**
	 * Write the content of the feedback form, excluding the actual FORM tags.
	 * This default implementation write the JavaScripts, CSS styles, and HTML
	 * code representing the feedback form. Sub classes overwriting this method
	 * thus have to make the super call.
	 */
	protected void writeFormContent() throws IOException {
		this.writeStylesAndScripts();
		this.writeFormContentHtml();
		this.writeLine("<script type=\"text/javascript\">");
		this.writeLine("init();");
		this.writeLine("</script>");
	}
}

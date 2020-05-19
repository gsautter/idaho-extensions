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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.uka.ipd.idaho.easyIO.web.HtmlServlet;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackFormBuilder.SubmitMode;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance;
import de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.ByteOrderMarkFilterInputStream;

/**
 * Default implementation of showing feedback requests to users in HTML pages.
 * This implementation still leaves the source of the actual feedback panels
 * abstract. Its purpose is to provide a default implementation for the HTML
 * front-end.<br>
 * The servlet can have its data stored in a separate folder below the
 * surrounding web-app's context path, its so-called data path. The default data
 * path is the web-app's context path itself, but a specific data path can be
 * specified as the <b>dataPath</b> parameter in the web.xml.<br>
 * For sub class specific settings and parameters, each servlet in addition has
 * an instance specific configuration file, loaded from its data path. By
 * default, this file is named <b>config.cnfg</b>, but an alternative name can
 * be specified in an the <b>configFile</b> parameter in the web.xml.<br>
 * Sub classes willing to change the layout (beyond the power of CSS) and
 * behavior of the web pages should overwrite to doPost() method, which does the
 * request handling. The doGet() method simply loops to the doPost() method in
 * this class. Sub classes may change this, however.
 * 
 * @author sautter
 */
public abstract class HtmlFeedbackServlet extends HtmlServlet {
	
	private static final String feedbackBasePage = "feedback.html";
	
	private static final String ANONYMOUS_LOGIN_PARAMETER = "anonymousLogin";
	private static final String USER_NAME_PARAMETER = "userName";
	private static final String PASSWORD_PARAMETER = "pswd";
	private static final String REQUEST_ID_PARAMETER = "requestId";
	private static final String SUBMIT_MODE_PARAMETER = "submitMode";
	
	private static final String OK_SUBMIT_MODE = "OK";
	private static final String OK_NEW_SUBMIT_MODE = "OKN";
	private static final String CANCEL_SUBMIT_MODE = "C";
	private static final String CANCEL_NEW_SUBMIT_MODE = "CN";
	
	private HashMap sessionsToUserNames = new HashMap();
	private HashMap userNamesToSessions = new HashMap();
	private boolean allowAnonymousLogin = false;
	
	private HtmlFeedbackEngine feedbackEngine;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.HtmlServlet#doInit()
	 */
	protected void doInit() throws ServletException {
		super.doInit();
		
		//	get login mode
		this.allowAnonymousLogin = (this.getSetting(ANONYMOUS_LOGIN_PARAMETER) != null);
		
		//	create feedback engine
		this.feedbackEngine = this.produceFeedbackEngine();
	}
	
	/**
	 * Produce a feedback engine for the servlet. This factory method exists so
	 * sub classes can overwrite it in order to provide an implementation
	 * specific sub class of HtmlFeedbackEngine. This default implementation
	 * produces a feedback engine that simply loops through to the
	 * HtmlFeedbackServlet.getFeedbackPanel(),
	 * HtmlFeedbackServlet.answerFeedbackPanel(), and
	 * HtmlFeedbackServlet.cancelFeedbackPanel() methods.
	 * @return the HtmlFeedbackEngine to use in the servlet
	 */
	protected HtmlFeedbackEngine produceFeedbackEngine() {
		return new HtmlFeedbackEngine() {
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#filterResponseData(java.util.Properties)
			 */
			protected void filterResponseData(Properties responseData) {
				HtmlFeedbackServlet.this.filterResponseData(responseData);
			}

			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#findFeedbackRequest(java.lang.String)
			 */
			protected FeedbackRequest findFeedbackRequest(String userName) {
				
				//	try 10 times to find a renderable request
				for (int attempts = 0; attempts < 10; attempts++) {
					
					//	get feedback panel
					FeedbackPanel fp = HtmlFeedbackServlet.this.getFeedbackPanel();
					
					//	no request pending (won't change with further attempts)
					if (fp == null) return null;
					
					//	obtain renderer
					FeedbackPanelHtmlRendererInstance fpRenderer = FeedbackPanelHtmlRenderer.getRenderer(fp);
					
					//	renderer not found, handle back to backing source
					if (fpRenderer == null)
						HtmlFeedbackServlet.this.cancelFeedbackPanel(fp);
					
					//	got request
					else return new HtmlFeedbackRequest(fp, fpRenderer, this);
				}
				
				//	did not find renderable request
				return null;
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#returnFeedbackRequest(de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest, boolean)
			 */
			protected void returnFeedbackRequest(FeedbackRequest fr, boolean wasAnswered) {
				if (wasAnswered) HtmlFeedbackServlet.this.answerFeedbackPanel(fr.fp);
				else HtmlFeedbackServlet.this.cancelFeedbackPanel(fr.fp);
			}
			
			class HtmlFeedbackRequest extends FeedbackRequest {
				Properties answerData = null;
				
				HtmlFeedbackRequest(FeedbackPanel fp, FeedbackPanelHtmlRendererInstance fpRenderer, HtmlFeedbackEngine hostEngine) {
					super(fp, fpRenderer, hostEngine);
				}
				
				/* (non-Javadoc)
				 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#addAnswerData(java.util.Properties, java.lang.String)
				 */
				protected void addAnswerData(Properties answerData, String userName) {
					this.answerData = new Properties();
					this.answerData.putAll(answerData);
				}
				
				/* (non-Javadoc)
				 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#isAnswered()
				 */
				public boolean isAnswered() {
					return (this.answerData != null);
				}
				
				/* (non-Javadoc)
				 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#getAnswerData()
				 */
				protected Properties getAnswerData() {
					return this.answerData;
				}
				
				/* (non-Javadoc)
				 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#getAnswerCount()
				 */
				public int getAnswerCount() {
					return ((this.answerData == null) ? 0 : this.answerData.size());
				}
			}
		};
	}
	
	/**
	 * Filter out servlet specific parameters from a response, i.e., remove the
	 * respective name/value pairs from the argument Properties object. This
	 * servlet's default feedback engine loops here so sub classes of the
	 * servlet can remove further parameters by overwriting this method while
	 * not having to implement a complete feedback engine on their own.
	 * Furthermore, sub classes that use a different feedback engine can use
	 * this method to filter out parameters specific to this servlet. Since this
	 * implementation actually removes some parameters, sub classes overwriting
	 * it should make the super invokation.
	 * @param responseData the data to filter
	 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#filterResponseData(java.util.Properties)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackServlet#produceFeedbackEngine()
	 */
	protected void filterResponseData(Properties responseData) {
		responseData.remove(ANONYMOUS_LOGIN_PARAMETER);
		responseData.remove(USER_NAME_PARAMETER);
		responseData.remove(PASSWORD_PARAMETER);
		responseData.remove(REQUEST_ID_PARAMETER);
		responseData.remove(SUBMIT_MODE_PARAMETER);
	}
	
	/**
	 * This implementation shuts down the feedback engine. Thus, sub classes
	 * overwriting this method have to make the super call.
	 * @see de.uka.ipd.idaho.easyIO.web.WebServlet#exit()
	 */
	protected void exit() {
		this.feedbackEngine.shutdown();
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		HttpSession session = request.getSession(true);
		String sessionId = session.getId();
		String userName = null;
		
		System.out.println("HtmlFeedbackEngine: handling request from " + request.getRemoteHost());
		
		//	search user name cookie (instant login)
		Cookie[] cookies = request.getCookies();
		for (int c = 0; (cookies != null) && (c < cookies.length); c++)
			if (USER_NAME_PARAMETER.equals(cookies[c].getName())) {
				userName = cookies[c].getValue();
				this.sessionsToUserNames.remove(this.userNamesToSessions.remove(userName));
				this.sessionsToUserNames.put(sessionId, userName);
				this.userNamesToSessions.put(userName, sessionId);
			}
		
		//	cookie not set ==> look up session (cookies might be disabled on client side)
		if (userName == null)
			userName = ((String) this.sessionsToUserNames.get(sessionId));
		
		//	anonymous login (use & remember remote IP address as user name, but don't set cookie)
		if ((userName == null) && this.allowAnonymousLogin && ANONYMOUS_LOGIN_PARAMETER.equals(request.getParameter(ANONYMOUS_LOGIN_PARAMETER))) {
			userName = request.getRemoteAddr();
			session.setMaxInactiveInterval(120);
			this.sessionsToUserNames.remove(this.userNamesToSessions.remove(userName));
			this.sessionsToUserNames.put(sessionId, userName);
			this.userNamesToSessions.put(userName, sessionId);
		}
		
		//	not logged in (cookie not set, and session ID not mapped to a user)
		if (userName == null) {
			
			//	gather login data
			userName = request.getParameter(USER_NAME_PARAMETER);
			String password = request.getParameter(PASSWORD_PARAMETER);
			
			//	login data incomplete ==> display login form
			if ((userName == null) || (password == null)) {
				this.displayLoginPage(request, response);
				return;
			}
			
			//	got login data, try authentication
			else if (this.authenticate(userName, password)) {
				
				//	authentication successful ==> remember session & set cookie
				this.sessionsToUserNames.remove(this.userNamesToSessions.remove(userName));
				this.sessionsToUserNames.put(sessionId, userName);
				this.userNamesToSessions.put(userName, sessionId);
				Cookie cookie = new Cookie(USER_NAME_PARAMETER, userName);
				cookie.setDomain(request.getServerName());
				cookie.setPath(request.getContextPath() + request.getServletPath());
				cookie.setComment("Remember Feedback Server user name on this computer for future feedback contributions?");
				cookie.setMaxAge(Integer.MAX_VALUE);
				response.addCookie(cookie);
			}
			
			//	report login failure
			else {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
		}
		
		//	!!! from here, userName is definitely not null, and the session is valid !!!
		System.out.println("  - user name is " + userName);
		
		//	get request ID and submit mode
		String requestId = request.getParameter(REQUEST_ID_PARAMETER);
		System.out.println("  - request ID is " + requestId);
		String submitMode = request.getParameter(SUBMIT_MODE_PARAMETER);
		System.out.println("  - submit mode is " + submitMode);
		
		//	request contains answer to feedback request
		if (requestId != null) {
			
			//	answer or cancel request
			if (OK_SUBMIT_MODE.equals(submitMode) || OK_NEW_SUBMIT_MODE.equals(submitMode))
				this.feedbackEngine.answerFeedbackRequest(requestId, request, userName);
			else this.feedbackEngine.cancelFeedbackRequest(requestId, userName);
			
			//	select response
			if (OK_NEW_SUBMIT_MODE.equals(submitMode) || CANCEL_NEW_SUBMIT_MODE.equals(submitMode))
				this.displayFeedbackRequest(request, response, userName);
			else this.displayThankYouPage(request, response);
		}
		
		//	display feedback request
		else this.displayFeedbackRequest(request, response, userName);
	}
	
	private void displayLoginPage(final HttpServletRequest request, HttpServletResponse response) throws IOException {
		InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackBasePage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		FeedbackPageBuilder fpb = new FeedbackPageBuilder(this, request, response) {
			protected void writePageHeadExtensions() throws IOException {
				
				//	include alert function if anonymous login enabled
				if (allowAnonymousLogin) {
					this.writeLine("<script type=\"text/javascript\">");
					this.writeLine("function alertAnonymousLogin() {");
					this.writeLine("  if (document.getElementById('" + ANONYMOUS_LOGIN_PARAMETER + "').checked)");
					this.writeLine("    return confirm('WARNING: Your IP address will be logged! Proceed?');");
					this.writeLine("}");
					this.writeLine("</script>");
				}
			}
			
			protected void includeBody() throws IOException {
				
				//	open form (with submit listener if anonymous login allowed)
				this.writeLine("<form" + 
						" method=\"POST\"" +
						" action=\"" + request.getContextPath() + request.getServletPath() + "\"" +
						(allowAnonymousLogin ? " onsubmit=\"return alertAnonymousLogin();\"" : "") + 
						">");
				
				//	open master table
				this.writeLine("<table class=\"loginTable\">");
				
				//	add head row
				this.writeLine("<tr>");
				this.writeLine("<td colspan=\"2\" class=\"loginTableHeader\">");
				this.writeLine("Please log in to Feedback Server.");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				//	add login fields
				this.writeLine("<tr>");
				
				this.writeLine("<td class=\"loginTableBody\">");
				this.writeLine("User Name&nbsp;");
				this.writeLine("<input type=\"text\" name=\"" + USER_NAME_PARAMETER + "\">");
				this.writeLine("</td>");
				
				this.writeLine("<td class=\"loginTableBody\">");
				this.writeLine("Password&nbsp;");
				this.writeLine("<input type=\"password\" name=\"" + PASSWORD_PARAMETER + "\">");
				this.writeLine("</td>");
				
				this.writeLine("</tr>");
				
				//	add anonymous login if enabled
				if (allowAnonymousLogin) {
					this.writeLine("<tr>");
					
					this.writeLine("<td colspan=\"2\" class=\"loginTableBody\">");
					this.writeLine("<input id=\"" + ANONYMOUS_LOGIN_PARAMETER + "\" type=\"checkbox\" name=\"" + ANONYMOUS_LOGIN_PARAMETER + "\" value=\"" + ANONYMOUS_LOGIN_PARAMETER + "\">");
					this.writeLine("&nbsp;Log in anonymously (the feedback server will log your IP address, however)");
					this.writeLine("</td>");
					
					this.writeLine("</tr>");
				}
				
				//	add login button
				this.writeLine("<tr>");
				this.writeLine("<td colspan=\"2\" class=\"loginTableBody\">");
				this.writeLine("<input type=\"submit\" value=\"Log In\" class=\"submitButton\">");
				this.writeLine("&nbsp;");
				this.writeLine("<input type=\"reset\" value=\"Cancel\" class=\"submitButton\" onclick=\"window.close();\">");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				//	close table
				this.writeLine("</table>");
				
				this.writeLine("</form>");
			}
		};
		
		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		this.sendHtmlPage(fbpReader, fpb);
		fbpReader.close();
	}
	
	private void displayFeedbackRequest(HttpServletRequest request, HttpServletResponse response, String userName) throws IOException {
		System.out.println("HtmlFeedbackServlet: getting feedback request for " + userName);
		
		//	retrieve feedback request
		FeedbackRequest fr = this.feedbackEngine.getFeedbackRequest(userName);
		
		//	no request for user to answer
		if (fr == null) {
			System.out.println("HtmlFeedbackServlet: feedback request for " + userName + " not found");
			this.displayNoRequestsPage(request, response);
		}
		
		//	request given the user can answer
		else {
			System.out.println("HtmlFeedbackServlet: got feedback request for " + userName + ": " + fr.fp.getClass().getName());
			this.displayFeedbackRequest(fr, request, response);
		}
	}
	
	private void displayNoRequestsPage(final HttpServletRequest request, HttpServletResponse response) throws IOException {
		InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackBasePage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		FeedbackPageBuilder fpb = new FeedbackPageBuilder(this, request, response) {
			protected void includeBody() throws IOException {
				//	TODO: make "thank you, no request pending" text a parameter
				this.writeLine("<p class=\"label\">" + FeedbackPanelHtmlRenderer.prepareForHtml("Thank you for your will to contribute. There are no feedback requests pending at the moment.") + "</p>");
				this.writeLine("<p class=\"functionLink\"><a href=\".\" onclick=\"window.close();\">close feedback window</a></p>");
			}
		};
		
		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		this.sendHtmlPage(fbpReader, fpb);
		fbpReader.close();
	}
	
	private static SubmitMode[] submitModes = {
		new SubmitMode(OK_SUBMIT_MODE, "OK", "Submit feedback"),
		new SubmitMode(OK_NEW_SUBMIT_MODE, "OK & New", "Submit feedback and open new request"),
		new SubmitMode(CANCEL_NEW_SUBMIT_MODE, "Skip", "Cancel current feedback and open new one"),
		new SubmitMode(CANCEL_SUBMIT_MODE, "Cancel", "Cancel feedback"),
	};
	
	private void displayFeedbackRequest(FeedbackRequest fr, HttpServletRequest request, HttpServletResponse response) throws IOException {
		InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackBasePage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		
		FeedbackFormPageBuilder ffpb = new FeedbackFormPageBuilder(this, request, response, fr, submitModes);
		
		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		this.sendHtmlPage(fbpReader, ffpb);
		fbpReader.close();
	}
	
	private void displayThankYouPage(final HttpServletRequest request, HttpServletResponse response) throws IOException {
		InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackBasePage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		FeedbackPageBuilder fpb = new FeedbackPageBuilder(this, request, response) {
			protected void includeBody() throws IOException {
				//	TODO: make "thank you" text a parameter
				this.writeLine("<p class=\"label\">" + FeedbackPanelHtmlRenderer.prepareForHtml("Thank you for your contribution.") + "</p>");
				this.writeLine("<p class=\"functionLink\"><a href=\".\" onclick=\"window.close();\">close feedback window</a>&nbsp;<a href=\"" + this.request.getContextPath() + this.request.getServletPath() + "\">new feedback request</a></p>");
			}
		};
		
		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		this.sendHtmlPage(fbpReader, fpb);
		fbpReader.close();
	}
	
	private abstract class FeedbackPageBuilder extends HtmlPageBuilder {
		FeedbackPageBuilder(HtmlPageBuilderHost host, HttpServletRequest request, HttpServletResponse response) throws IOException {
			super(host, request, response);
		}
		protected void include(String type, String tag) throws IOException {
			if ("includeBody".equals(type))
				this.includeBody();
			else if ("includeLink".equals(type)) {}
			else super.include(type, tag);
		}
		
		protected abstract void includeBody() throws IOException;
	}
	
	/**
	 * Authenticate a combination of a user name and a password. This default
	 * implementation simply returns true. Sub classes wishing to restrict
	 * access are welcome to overwrite it as needed.
	 * @param userName the name of the user to authenticate
	 * @param password the password to use for authentication
	 * @return true if the authentication was successful, false otherwise
	 */
	protected boolean authenticate(String userName, String password) {
		return true;
	}
	
	/**
	 * @return a feedback panel representing a new request to answer, or null,
	 *         if there are currently no pending requests
	 */
	protected abstract FeedbackPanel getFeedbackPanel();
	
	/**
	 * Cancel a feedback request, i.e., make the backing source of feedback requests know
	 * that a specific feedback panel is handed back unanswered.
	 * @param fp the feedback panel representing the request to cancel
	 */
	protected abstract void cancelFeedbackPanel(FeedbackPanel fp);
	
	/**
	 * Answer a feedback request, i.e., hand a filled in feedback panel back to
	 * the backing source of feedback requests.
	 * @param fp the feedback panel representing the answered request
	 */
	protected abstract void answerFeedbackPanel(FeedbackPanel fp);
}

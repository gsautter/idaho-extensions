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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.uka.ipd.idaho.easyIO.web.HtmlServlet;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackFormBuilder.SubmitMode;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance;
import de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.ByteOrderMarkFilterInputStream;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;

/**
 * Implementation a feedback engine showing feedback requests to users in HTML
 * pages, while letting them compete on the answers in sort of a game show. This
 * implementation leaves the source of the actual feedback panels abstract. Its
 * purpose is to provide the HTML front-end for the gameshow style feedback
 * competitions.<br>
 * The servlet can have its data stored in a separate folder below the
 * surrounding web-app's context path, its so-called data path. The default data
 * path is the web-app's context path itself, but a specific data path can be
 * specified as the <b>dataPath</b> parameter in the web.xml.<br>
 * For sub class specific settings and parameters, each servlet in addition has
 * an instance specific configuration file, loaded from its data path. By
 * default, this file is named <b>config.cnfg</b>, but an alternative name can
 * be specified in an the <b>configFile</b> parameter in the web.xml. <br>
 * 
 * @author sautter
 */
public abstract class QuizHtmlFeedbackServlet extends HtmlServlet {
	
	private static final String feedbackBasePage = "feedbackQuiz.html";
	private static final String feedbackPartPage = "feedbackPart.html";
	
	private static final String USER_NAME_PARAMETER = "userName";
	private static final String PASSWORD_PARAMETER = "pswd";
	
	private static final String QUIZ_ID_PARAMETER = "quizId";
	private static final String LOGOUT_QUIZ_ID = "logout";
	
	private static final String QUIZ_NAME_PARAMETER = "quizName";
	private static final String QUIZ_PASSCODE_PARAMETER = "quizPassCode";
	private static final String QUIZ_HOST_PARAMETER = "quizHost";
	
	private static final String QUIZ_TYPE_PARAMETER = "quizType";
	private static final String OPEN_QUIZ_TYPE = "openQuiz";
	private static final String HOSTED_QUIZ_TYPE = "hostedQuiz";
	
	private static final String QUIZ_ADMIN_COMMAND_PARAMETER = "quizAdminCommand";
	private static final String CONFIGURE_QUIZ_ADMIN_COMMAND = "configureQuiz";
	private static final String START_QUIZ_ADMIN_COMMAND = "startQuiz";
	private static final String PAUSE_QUIZ_ADMIN_COMMAND = "pauseQuiz";
	private static final String FINISH_QUIZ_ADMIN_COMMAND = "finishQuiz";
	private static final String CLOSE_QUIZ_ADMIN_COMMAND = "closeQuiz";
	
	private static final String CONFIGURE_QUIZ_ANSWER_REDUNDANCY_PARAMETER = "answerRedundancy";
	private static final String CONFIGURE_QUIZ_CORRECT_ANSWER_SCORE_PARAMETER = "answerScore";
	private static final String CONFIGURE_QUIZ_WRONG_ANSWER_PENALTY_PARAMETER = "answerPenalty";
	private static final String CONFIGURE_QUIZ_REQUEST_TIMEOUT_PENALTY_PARAMETER = "timeoutPenalty";
	private static final String CONFIGURE_QUIZ_REQUEST_CANCEL_PENALTY_PARAMETER = "cancelPenalty";
	
	private static final String REQUEST_ID_PARAMETER = "requestId";
	private static final String RANKING_REQUEST_ID = "ranking";
	
	private static final String SUBMIT_MODE_PARAMETER = "submitMode";
	private static final String OK_SUBMIT_MODE = "OK";
	private static final String CANCEL_SUBMIT_MODE = "C";
	
	private HashMap sessionsToUserNames = new HashMap();
	private HashMap sessionsToQuizIDs = new HashMap();
	
	private int quizTimeout = 3600; // max idle time (in seconds) for a quiz before auto-shutdown
	private QuizTimeoutWatchdog quizTimeoutWatchdog = null;
	
	/**
	 * Do implementation specific initialization. This implementation loads
	 * various parameters, so sub classes overwriting it have to make the super
	 * call.
	 * @see de.uka.ipd.idaho.easyIO.web.HtmlServlet#doInit()
	 */
	protected void doInit() throws ServletException {
		super.doInit();
		
		//	start quiz timeout watchdog
		this.quizTimeoutWatchdog = new QuizTimeoutWatchdog();
		this.quizTimeoutWatchdog.start();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.WebServlet#exit()
	 */
	protected void exit() {
		
		//	shut down quiz timeout qatchdog
		if (this.quizTimeoutWatchdog != null) {
			this.quizTimeoutWatchdog.shutdown();
			this.quizTimeoutWatchdog = null;
		}
		
		//	shut down remaining quizzes
		Quiz[] quizzes = ((Quiz[]) quizzesById.values().toArray(new Quiz[quizzesById.size()]));
		this.quizzesById.clear();
		this.quizzesByName.clear();
		for (int q = 0; q < quizzes.length; q++)
			quizzes[q].shutdown();
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doPost(request, response);
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		HttpSession session = request.getSession(true);
		String sessionId = session.getId();
		String userName = null;
		String quizId = null;
		boolean quizHost = false;
		
		System.out.println("HtmlFeedbackEngine: handling request from " + request.getRemoteHost());
		
		//	search user name cookie (instant login)
		Cookie[] cookies = request.getCookies();
		for (int c = 0; (cookies != null) && (c < cookies.length); c++)
			if (USER_NAME_PARAMETER.equals(cookies[c].getName())) {
				userName = cookies[c].getValue();
				this.sessionsToUserNames.put(sessionId, userName);
			}
			else if (QUIZ_ID_PARAMETER.endsWith(cookies[c].getName())) {
				quizId = cookies[c].getValue();
				this.sessionsToQuizIDs.put(sessionId, quizId);
			}
		
		//	cookie not set ==> look up session (cookies might be disabled on client side)
		if (userName == null)
			userName = ((String) this.sessionsToUserNames.get(sessionId));
		
		//	cookie not set ==> look up session (cookies might be disabled on client side)
		if (quizId == null)
			quizId = ((String) this.sessionsToQuizIDs.get(sessionId));
		
		//	not logged in (cookie not set, and session ID not mapped to a user)
		if (userName == null) {
			
			//	gather login data
			userName = request.getParameter(USER_NAME_PARAMETER);
			quizId = request.getParameter(QUIZ_ID_PARAMETER);
			String password = request.getParameter(PASSWORD_PARAMETER);
			
			//	login data incomplete ==> display login form
			if ((userName == null) || (password == null)) {
				this.displayLoginPage(request, response, "Please log in.");
				return;
			}
			
			//	global login, eg for creating a new quiz
			else if (quizId == null) {
				
				//	authenticate globally
				if (this.authenticate(userName, password)) {
					
					//	collect data for new quiz
					String quizType = request.getParameter(QUIZ_TYPE_PARAMETER);
					String quizName = request.getParameter(QUIZ_NAME_PARAMETER);
					String quizPassCode = request.getParameter(QUIZ_PASSCODE_PARAMETER);
					
					//	check data
					if ((quizName == null) || (quizName.trim().length() == 0)) {
						this.displayLoginPage(request, response, "Invalid quiz name. Please use a non-empty name for a quiz.");
						return;
					}
					else if (this.quizzesByName.containsKey(quizName)) {
						this.displayLoginPage(request, response, "A quiz named '" + quizName + "' is already running, please use a different name.");
						return;
					}
					else if (HOSTED_QUIZ_TYPE.equals(quizType) && ((quizPassCode == null) || (quizPassCode.trim().length() == 0))) {
						this.displayLoginPage(request, response, "Invalid pass code. Please use an non-empty pass code for a hosted quiz.");
						return;
					}
					
					//	create new quiz
					Quiz quiz = new Quiz(
							quizName,
							userName,
							(HOSTED_QUIZ_TYPE.equals(quizType) ? quizPassCode : null)
							);
					this.quizzesById.put(quiz.id, quiz);
					this.quizzesByName.put(quiz.name, quiz);
					quizId = quiz.id;
					quiz.addUser(userName);
					quizHost = true;
					
					//	authentication successful ==> remember session & set cookie
					this.sessionsToUserNames.put(sessionId, userName);
					Cookie userNameCookie = new Cookie(USER_NAME_PARAMETER, userName);
					userNameCookie.setDomain(request.getServerName());
					userNameCookie.setPath(request.getContextPath() + request.getServletPath());
					userNameCookie.setComment("Remember Feedback Server user name on this computer for future feedback contributions?");
					userNameCookie.setMaxAge(-1);// make cookie last until client browser is closed
					response.addCookie(userNameCookie);
					
					this.sessionsToQuizIDs.put(sessionId, quizId);
					Cookie quizIdCookie = new Cookie(QUIZ_ID_PARAMETER, quizId);
					quizIdCookie.setDomain(request.getServerName());
					quizIdCookie.setPath(request.getContextPath() + request.getServletPath());
					quizIdCookie.setComment("Remember Feedback Server user name on this computer for future feedback contributions?");
					quizIdCookie.setMaxAge(-1);// make cookie last until client browser is closed
					response.addCookie(quizIdCookie);
					
					this.displayBasePage(request, response, quiz, true);
					return;
				}
				
				//	report login failure
				else {
					this.displayLoginPage(request, response, "Login failed.");
					return;
				}
			}
			
			//	login for specific quiz
			else {
				
				//	get referenced quiz
				Quiz quiz = ((Quiz) this.quizzesById.get(quizId));
				
				//	quiz not found
				if (quiz == null) {
					this.displayLoginPage(request, response, "The quiz you tried to join does not exist.");
					return;
				}
				
				//	authenticate with quiz
				else {
					
					//	authenticate with quiz
					if (quiz.authenticate(userName, password)) {
						
						//	user added successfully
						if (quiz.addUser(userName)) {
							
							//	authentication successful ==> remember session & set cookies
							this.sessionsToUserNames.put(sessionId, userName);
							Cookie userNameCookie = new Cookie(USER_NAME_PARAMETER, userName);
							userNameCookie.setDomain(request.getServerName());
							userNameCookie.setPath(request.getContextPath() + request.getServletPath());
							userNameCookie.setComment("Remember Feedback Server user name on this computer for future feedback contributions?");
							userNameCookie.setMaxAge(-1);// make cookie last until client browser is closed
							response.addCookie(userNameCookie);
							
							this.sessionsToQuizIDs.put(sessionId, quizId);
							Cookie quizIdCookie = new Cookie(QUIZ_ID_PARAMETER, quizId);
							quizIdCookie.setDomain(request.getServerName());
							quizIdCookie.setPath(request.getContextPath() + request.getServletPath());
							quizIdCookie.setComment("Remember Feedback Server user name on this computer for future feedback contributions?");
							quizIdCookie.setMaxAge(-1);// make cookie last until client browser is closed
							response.addCookie(quizIdCookie);
							
							this.displayBasePage(request, response, quiz, false);
							return;
						}
						
						//	error addding user (name already taken)
						else {
							this.displayLoginPage(request, response, "Unable to join quiz '" + quiz.name + "' as '" + userName + "', please use a different user name.");
							return;
						}
					}
					
					//	authentication with quiz failed
					else {
						this.displayLoginPage(request, response, "Unable to join quiz '" + quiz.name + "', please check your login data & quiz status.");
						return;
					}
				}
			}
		}
		
		//	!!! from here, userName and quizId are definitely not null, and the session is valid !!!
		System.out.println("  - user name is " + userName);
		System.out.println("  - quiz ID is " + quizId);
		
		//	check for logout, bypassing cookies and HTTP sessions
		if (LOGOUT_QUIZ_ID.equals(request.getParameter(QUIZ_ID_PARAMETER))) {
			
			this.sessionsToUserNames.remove(sessionId);
			Cookie userNameCookie = new Cookie(USER_NAME_PARAMETER, userName);
			userNameCookie.setDomain(request.getServerName());
			userNameCookie.setPath(request.getContextPath() + request.getServletPath());
			userNameCookie.setMaxAge(0);// delete cookie
			response.addCookie(userNameCookie);
			
			this.sessionsToQuizIDs.remove(sessionId);
			Cookie quizIdCookie = new Cookie(QUIZ_ID_PARAMETER, quizId);
			quizIdCookie.setDomain(request.getServerName());
			quizIdCookie.setPath(request.getContextPath() + request.getServletPath());
			quizIdCookie.setMaxAge(0);// delete cookie
			response.addCookie(quizIdCookie);
			
			session.invalidate();
			
			this.displayThankYouPage(request, response);
			return;
		}
		
		//	get referenced quiz
		Quiz quiz = ((Quiz) this.quizzesById.get(quizId));
		
		//	quiz not found
		if (quiz == null) {
			this.displayLoginPage(request, response, "The quiz you are trying to participate on does not exist or has already been closed.");
			return;
		}
		
		//	let adddressed quiz handle request
		else {
			System.out.println("  - quiz is " + quiz.name);
			
			//	display quiz admin page
			if (quizHost || (quiz.hostUserName.equals(userName) && QUIZ_HOST_PARAMETER.equals(request.getParameter(QUIZ_HOST_PARAMETER))))
				this.doQuizAdminRequest(request, response, quiz);
			
			//	process request
			else {
				
				//	get request ID and submit mode
				String requestId = request.getParameter(REQUEST_ID_PARAMETER);
				System.out.println("  - request ID is " + requestId);
				String submitMode = request.getParameter(SUBMIT_MODE_PARAMETER);
				System.out.println("  - submit mode is " + submitMode);
				
				//	display current ranking
				if (RANKING_REQUEST_ID.equals(requestId)) {
					this.displayQuizRanking(request, response, quiz);
					return;
				}
				
				//	request contains answer to feedback request, answer or cancel it
				else if (requestId != null) {
					if (OK_SUBMIT_MODE.equals(submitMode))
						quiz.answerFeedbackRequest(requestId, request, userName);
					else quiz.cancelFeedbackRequest(requestId, userName);
				}
				
				//	display next feedback request
				this.displayFeedbackRequest(request, response, userName, quiz);
			}
		}
	}
	
	private void displayFeedbackRequest(HttpServletRequest request, HttpServletResponse response, String userName, final Quiz quiz) throws IOException {
		
		//	quiz not yet started, display waiting page
		if (quiz.status == Quiz.INITIALIZING)
			this.displayWaitingPage(request, response, quiz, ("Quiz '" + quiz.name + "' has not yet started, please wait."));
		
		//	quiz running, display request
		else if (quiz.status == Quiz.RUNNING) {
			
			//	retrieve feedback request
			FeedbackRequest fr = quiz.getFeedbackRequest(userName);
			
			//	no request for user to answer
			if (fr == null)
				this.displayWaitingPage(request, response, quiz, ("There are currently no feedback requests for quiz '" + quiz.name + "', sorry."));
			
			//	request given the user can answer
			else this.displayFeedbackRequest(fr, request, response, quiz);
		}
		
		//	quiz paused, display waiting page
		else if (quiz.status == Quiz.PAUSED)
			this.displayWaitingPage(request, response, quiz, ("Quiz '" + quiz.name + "' has been paused, please wait."));
		
		//	quiz finished, display final ranking
		else if (quiz.status == Quiz.FINISHED)
			this.displayQuizResult(request, response, quiz);
		
		//	quiz closed, display goodbye page
		else this.displayThankYouPage(request, response);
	}
	
	private void displayLoginPage(final HttpServletRequest request, HttpServletResponse response, final String message) throws IOException {
		InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackBasePage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		QuizFeedbackPageBuilder fpb = new QuizFeedbackPageBuilder(this, request, response) {
			protected void includeBody() throws IOException {
				
				//	display message, if any
				if (message != null) {
					
					//	open table
					this.writeLine("<table class=\"loginTable\">");
					
					//	add head row
					this.writeLine("<tr>");
					this.writeLine("<td class=\"loginTableHeader\">");
					this.writeLine(IoTools.prepareForHtml(message));
					this.writeLine("</td>");
					this.writeLine("</tr>");
					
					//	close table
					this.writeLine("</table>");
					
					//	add spacer
					this.writeLine("<br>");
				}
				
				//	get joinable quizzes
				Quiz[] quizzes = ((Quiz[]) quizzesById.values().toArray(new Quiz[quizzesById.size()]));
				ArrayList joinableQuizzes = new ArrayList();
				for (int q = 0; q < quizzes.length; q++)
					if (quizzes[q].status == Quiz.INITIALIZING)
						joinableQuizzes.add(quizzes[q]);
				quizzes = ((Quiz[]) joinableQuizzes.toArray(new Quiz[joinableQuizzes.size()]));
				
				//	no quizzes to join
				if (quizzes.length == 0) {
					
					//	open table
					this.writeLine("<table class=\"loginTable\">");
					
					//	add head row
					this.writeLine("<tr>");
					this.writeLine("<td class=\"loginTableHeader\">");
					this.writeLine("Join an existing quiz.");
					this.writeLine("</td>");
					this.writeLine("</tr>");
					
					//	add message row
					this.writeLine("<tr>");
					this.writeLine("<td class=\"loginTableBody\">");
					this.writeLine("Currently, there are no quizzes to join, sorry.<br>If you have an account, you can use the form below to start a quiz yourself and invite your friends to join.");
					this.writeLine("</td>");
					this.writeLine("</tr>");
					
					//	close table
					this.writeLine("</table>");
				}
				
				//	quizzes to join, add form
				else {
					this.writeLine("<form method=\"POST\" action=\"" + this.request.getContextPath() + this.request.getServletPath() + "\">");
					
					//	open master table
					this.writeLine("<table class=\"loginTable\">");
					
					//	add head row
					this.writeLine("<tr>");
					this.writeLine("<td colspan=\"3\" class=\"loginTableHeader\">");
					this.writeLine("Join an existing quiz.");
					this.writeLine("</td>");
					this.writeLine("</tr>");
					
					//	add joining fields
					this.writeLine("<tr>");
					
					this.writeLine("<td class=\"loginTableCell\">");
					this.writeLine("User Name&nbsp;");
					this.writeLine("<input type=\"text\" name=\"" + USER_NAME_PARAMETER + "\">");
					this.writeLine("</td>");
					
					this.writeLine("<td class=\"loginTableCell\">");
					this.writeLine("Password/Passcode&nbsp;");
					this.writeLine("<input type=\"password\" name=\"" + PASSWORD_PARAMETER + "\">");
					this.writeLine("</td>");
					
					this.writeLine("<td class=\"loginTableCell\">");
					this.writeLine("Quiz&nbsp;to&nbsp;Join&nbsp;");
					this.writeLine("<select name=\"" + QUIZ_ID_PARAMETER + "\">");
					for (int q = 0; q < quizzes.length; q++)
						this.writeLine("<option value=\"" + quizzes[q].id + "\">" + IoTools.prepareForHtml(quizzes[q].name) + ", Host: " + quizzes[q].hostUserName + " (" + (quizzes[q].isHosted() ? "hosted" : "open")  + ")</option>");
					this.writeLine("</select>");
					this.writeLine("</td>");
					
					this.writeLine("</tr>");
					
					//	add login button
					this.writeLine("<tr>");
					this.writeLine("<td colspan=\"3\" class=\"loginTableBody\">");
					this.writeLine("<input type=\"submit\" value=\"Join\" class=\"submitButton\">");
					this.writeLine("</td>");
					this.writeLine("</tr>");
					
					//	close table
					this.writeLine("</table>");
					
					this.writeLine("</form>");
				}
				
				//	add spacer
				this.writeLine("<br>");
				
				//	add form for starting a new quiz
				this.writeLine("<form method=\"POST\" action=\"" + request.getContextPath() + request.getServletPath() + "\">");
				
				//	open master table
				this.writeLine("<table class=\"loginTable\">");
				
				//	add head row
				this.writeLine("<tr>");
				this.writeLine("<td colspan=\"3\" class=\"loginTableHeader\">");
				this.writeLine("Start a new quiz.");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				//	add explanation row
				this.writeLine("<tr>");
				this.writeLine("<td colspan=\"3\" class=\"loginTableBody\">");
				this.writeLine("A <b>pass code</b> is required only for a <b>hosted quiz</b>.<br>In an <b>open quiz</b>, participating users log in with their server accounts.");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				//	add joining fields
				this.writeLine("<tr>");
				
				this.writeLine("<td class=\"loginTableCell\">");
				this.writeLine("User&nbsp;Name&nbsp;");
				this.writeLine("<input type=\"text\" name=\"" + USER_NAME_PARAMETER + "\">");
				this.writeLine("</td>");
				
				this.writeLine("<td class=\"loginTableCell\">");
//				this.writeLine("Hosted&nbsp;Quiz&nbsp;");
//				this.writeLine("<input type=\"radio\" name=\"" + QUIZ_TYPE_PARAMETER + "\" value=\"" + HOSTED_QUIZ_TYPE + "\" checked>");
				this.writeLine("Hosted&nbsp;Quiz&nbsp;<input type=\"radio\" name=\"" + QUIZ_TYPE_PARAMETER + "\" value=\"" + HOSTED_QUIZ_TYPE + "\" checked>");
				this.writeLine("</td>");
				
				this.writeLine("<td class=\"loginTableCell\">");
				this.writeLine("Quiz&nbsp;Name&nbsp;");
				this.writeLine("<input type=\"text\" name=\"" + QUIZ_NAME_PARAMETER + "\">");
				this.writeLine("</td>");
				
				this.writeLine("</tr>");
				
				//	add quiz data fields
				this.writeLine("<tr>");
				
				this.writeLine("<td class=\"loginTableCell\">");
				this.writeLine("Password&nbsp;");
				this.writeLine("<input type=\"password\" name=\"" + PASSWORD_PARAMETER + "\">");
				this.writeLine("</td>");
				
				this.writeLine("<td class=\"loginTableCell\">");
//				this.writeLine("Open&nbsp;Quiz&nbsp;");
//				this.writeLine("<input type=\"radio\" name=\"" + QUIZ_TYPE_PARAMETER + "\" value=\"" + OPEN_QUIZ_TYPE + "\">");
				this.writeLine("Open&nbsp;Quiz&nbsp;<input type=\"radio\" name=\"" + QUIZ_TYPE_PARAMETER + "\" value=\"" + OPEN_QUIZ_TYPE + "\">");
				this.writeLine("</td>");
				
				this.writeLine("<td class=\"loginTableCell\">");
				this.writeLine("Passcode&nbsp;");
				this.writeLine("<input type=\"text\" name=\"" + QUIZ_PASSCODE_PARAMETER + "\">");
				this.writeLine("</td>");
				
				this.writeLine("</tr>");
				
				//	add login button
				this.writeLine("<tr>");
				this.writeLine("<td colspan=\"3\" class=\"loginTableBody\">");
				this.writeLine("<input type=\"submit\" value=\"Start Quiz\" class=\"submitButton\">");
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
	
	private void displayBasePage(final HttpServletRequest request, HttpServletResponse response, final Quiz quiz, final boolean quizHost) throws IOException {
		InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackBasePage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		QuizFeedbackPageBuilder fpb = new QuizFeedbackPageBuilder(this, request, response) {
			protected void writePageHeadExtensions() throws IOException {
				
				//	add JavaScript functions
				this.writeLine("<script type=\"text/javascript\">");
				
				//	add ranking display function
				this.writeLine("var quizFinished = false;");
				this.writeLine("function displayRanking() {");
				this.writeLine("  if (quizFinished) {");
				this.writeLine("    alert('The quiz has been finished, cannot access ranking window any more.');");
				this.writeLine("    return;");
				this.writeLine("  }");
				this.writeLine("  if (ranking && ranking.closed)");
				this.writeLine("    ranking = null;");
				this.writeLine("  if (ranking) {");
				this.writeLine("    ranking.focus();");
				this.writeLine("    ranking.location.reload();");
				this.writeLine("  }");
				this.writeLine("  else ranking = window.open(" + 
						"'" + request.getContextPath() + request.getServletPath() + 
							"?" + QUIZ_ID_PARAMETER + "=" + quiz.id + 
							"&" + REQUEST_ID_PARAMETER + "=" + RANKING_REQUEST_ID + "', " +
						"'QuizRanking', " +
						"'width=600,height=400,location=no'" +
						");");
				this.writeLine("}");
				this.writeLine("var ranking;");
				
				//	add control function if host
				if (quizHost) {
					this.writeLine("var quizClosed = false;");
					this.writeLine("function displayQuizControl() {");
					this.writeLine("  if (quizClosed) {");
					this.writeLine("    alert('The quiz has been closed, cannot access control window any more.');");
					this.writeLine("    return;");
					this.writeLine("  }");
					this.writeLine("  if (quizControl && quizControl.closed)");
					this.writeLine("    quizControl = null;");
					this.writeLine("  if (quizControl)");
					this.writeLine("    quizControl.focus();");
					this.writeLine("  else quizControl = window.open(" + 
							"'" + request.getContextPath() + request.getServletPath() + 
								"?" + QUIZ_ID_PARAMETER + "=" + quiz.id + 
								"&" + QUIZ_HOST_PARAMETER + "=" + QUIZ_HOST_PARAMETER + "', " +
							"'QuizControl', " +
							"'width=600,height=400,location=no'" +
							");");
					this.writeLine("}");
					this.writeLine("var quizControl;");
				}
				
				this.writeLine("</script>");
			}
			
			protected void includeBody() throws IOException {
				
				//	include function links
				this.writeLine("<p class=\"functionLink\">");
				this.writeLine("<a href=\"#\" onclick=\"displayRanking();\">current ranking</a>");
				if (quizHost) {
					this.writeLine("&nbsp;");
					this.writeLine("<a href=\"#\" onclick=\"displayQuizControl();\">open quiz control</a>");
				}
				this.writeLine("</p>");
				
				this.writeLine("<center>");
				this.writeLine("<iframe id=\"quizFrame\" class=\"quizFrame\" src=\"" + request.getContextPath() + request.getServletPath() + "?" + QUIZ_ID_PARAMETER + "=" + quiz.id + "\">");
				this.writeLine("</iframe>");
				this.writeLine("<br>");
				this.writeLine("</center>");
			}
		};
		
		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		this.sendHtmlPage(fbpReader, fpb);
		fbpReader.close();
	}
	
	private void doQuizAdminRequest(final HttpServletRequest request, HttpServletResponse response, final Quiz quiz) throws IOException {
		
		//	get command
		String command = request.getParameter(QUIZ_ADMIN_COMMAND_PARAMETER);
		System.out.println("  - quiz status is " + quiz.status + ", command is " + command);
		String error = null;
		
		//	quiz not yet started, process 'configure' and 'start' commands
		if (quiz.status == Quiz.INITIALIZING) {
			
			//	configuration only
			if (CONFIGURE_QUIZ_ADMIN_COMMAND.equals(command))
				this.setQuizParameters(request, quiz);
			
			//	start command
			else if (START_QUIZ_ADMIN_COMMAND.equals(command)) {
				this.setQuizParameters(request, quiz);
				error = quiz.start();
			}
		}
		
		//	quiz running, process 'pause' command
		else if (quiz.status == Quiz.RUNNING) {
			
			//	pause command
			if (PAUSE_QUIZ_ADMIN_COMMAND.equals(command))
				error = quiz.pause();
		}
		
		//	quiz paused, process 'start' and 'finish' commands
		else if (quiz.status == Quiz.PAUSED) {
			
			//	start command
			if (START_QUIZ_ADMIN_COMMAND.equals(command))
				error = quiz.start();
			
			//	finish command
			else if (FINISH_QUIZ_ADMIN_COMMAND.equals(command))
				error = quiz.finish();
		}
		
		//	quiz finished, process 'close' command
		else if (quiz.status == Quiz.FINISHED) {
			
			//	close command
			if (CLOSE_QUIZ_ADMIN_COMMAND.equals(command)) {
				error = quiz.close();
				if (error == null) {
					this.quizzesById.remove(quiz.id);
					this.quizzesByName.remove(quiz.name);
				}
			}
		}
		
		final String message = error;
		
		InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackPartPage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		QuizFeedbackPageBuilder fpb = new QuizFeedbackPageBuilder(this, request, response) {
			protected void writePageHeadExtensions() throws IOException {
				
				//	add JavaScript functions
				this.writeLine("<script type=\"text/javascript\">");
				this.writeLine("function setCommand(command) {");
				this.writeLine("  document.getElementById('quizAdminForm')['" + QUIZ_ADMIN_COMMAND_PARAMETER + "'].value = command;");
				this.writeLine("  if (command == '" + CLOSE_QUIZ_ADMIN_COMMAND + "') {");
				this.writeLine("    opener.quizClosed = true;");
				this.writeLine("    window.close();");
				this.writeLine("  }");
				this.writeLine("}");
				this.writeLine("</script>");
			}
			
			protected void includeBody() throws IOException {
				
				//	display message (if any)
				if (message != null)
					this.writeLine("<p class=\"label\">" + FeedbackPanelHtmlRenderer.prepareForHtml(message) + "</p>");
				
				//	open form and add function parameters
				this.writeLine("<form method=\"POST\" id=\"quizAdminForm\" action=\"" + request.getContextPath() + request.getServletPath() + "\">");
				this.writeLine("<input type=\"hidden\" name=\"" + QUIZ_ID_PARAMETER + "\" value=\"" + quiz.id + "\">");
				this.writeLine("<input type=\"hidden\" name=\"" + QUIZ_HOST_PARAMETER + "\" value=\"" + QUIZ_HOST_PARAMETER + "\">");
				this.writeLine("<input type=\"hidden\" name=\"" + QUIZ_ADMIN_COMMAND_PARAMETER + "\" value=\"" + CONFIGURE_QUIZ_ADMIN_COMMAND + "\">");
				this.writeLine("<table class=\"configTable\">");
				
				//	quiz not yet started, display configuration fields and 'configure' & 'start' buttons
				if (quiz.status == Quiz.INITIALIZING) {
					
					this.writeLine("<tr>");
					
					this.writeLine("<td class=\"configTableCell\">");
					this.writeLine("Answer&nbsp;Redundancy&nbsp;<input size=\"3\" type=\"text\" name=\"" + CONFIGURE_QUIZ_ANSWER_REDUNDANCY_PARAMETER + "\" value=\"" + quiz.answerRedundancy + "\">");
					this.writeLine("</td>");
					
					this.writeLine("<td class=\"configTableCell\">");
					this.writeLine("Correct&nbsp;Answer&nbsp;Score&nbsp;<input size=\"3\" type=\"text\" name=\"" + CONFIGURE_QUIZ_CORRECT_ANSWER_SCORE_PARAMETER + "\" value=\"" + quiz.correctAnswerScore + "\">");
					this.writeLine("</td>");
					
					this.writeLine("<td class=\"configTableCell\">");
					this.writeLine("Wrong&nbsp;Answer&nbsp;Penalty&nbsp;<input size=\"3\" type=\"text\" name=\"" + CONFIGURE_QUIZ_WRONG_ANSWER_PENALTY_PARAMETER + "\" value=\"" + quiz.wrongAnswerPenalty + "\">");
					this.writeLine("</td>");
					
					this.writeLine("</tr>");
					
					this.writeLine("<tr>");
					
					this.writeLine("<td class=\"configTableCell\">");
					this.writeLine("&nbsp;");
					this.writeLine("</td>");
					
					this.writeLine("<td class=\"configTableCell\">");
					this.writeLine("Request&nbsp;Timeout&nbsp;Penalty&nbsp;<input size=\"3\" type=\"text\" name=\"" + CONFIGURE_QUIZ_REQUEST_TIMEOUT_PENALTY_PARAMETER + "\" value=\"" + quiz.timeoutPenalty + "\">");
					this.writeLine("</td>");
					
					this.writeLine("<td class=\"configTableCell\">");
					this.writeLine("Request&nbsp;Cancel&nbsp;Penalty&nbsp;<input size=\"3\" type=\"text\" name=\"" + CONFIGURE_QUIZ_REQUEST_CANCEL_PENALTY_PARAMETER + "\" value=\"" + quiz.cancelPenalty + "\">");
					this.writeLine("</td>");
					
					this.writeLine("</tr>");
					
					this.writeLine("<tr>");
					this.writeLine("<td colspan=\"3\" class=\"configTableBody\">");
					quiz.writeDetailRanking(this, "Participating Users");
					this.writeLine("</td>");
					this.writeLine("</tr>");
					
					this.writeLine("<td colspan=\"3\" class=\"configTableBody\">");
					this.writeLine("<input type=\"submit\" value=\"Configure\" title=\"Change the quiz's configuration\" onclick=\"setCommand('" + CONFIGURE_QUIZ_ADMIN_COMMAND + "');\" class=\"submitButton\">");
					this.writeLine("&nbsp;");
					this.writeLine("<input type=\"submit\" value=\"Start Quiz\" title=\"Start the quiz\" onclick=\"setCommand('" + START_QUIZ_ADMIN_COMMAND + "');\" class=\"submitButton\">");
					this.writeLine("</td>");
					this.writeLine("</tr>");
				}
				
				//	quiz running, display and 'pause' button
				else if (quiz.status == Quiz.RUNNING) {
					
					this.writeLine("<tr>");
					this.writeLine("<td class=\"configTableBody\">");
					this.writeLine("<input type=\"submit\" value=\"Pause Quiz\" title=\"Pause the quiz\" onclick=\"setCommand('" + PAUSE_QUIZ_ADMIN_COMMAND + "');\" class=\"submitButton\">");
					this.writeLine("</td>");
					this.writeLine("</tr>");
				}
				
				//	quiz paused, display intermediate result and 'start' & 'finish' buttons
				else if (quiz.status == Quiz.PAUSED) {
					
					this.writeLine("<tr>");
					this.writeLine("<td class=\"configTableBody\">");
					quiz.writeDetailRanking(this, "Intermediate Ranking");
					this.writeLine("</td>");
					this.writeLine("</tr>");
					
					this.writeLine("<td class=\"configTableBody\">");
					this.writeLine("<input type=\"submit\" value=\"Restart Quiz\" title=\"Restart the quiz\" onclick=\"setCommand('" + START_QUIZ_ADMIN_COMMAND + "');\" class=\"submitButton\">");
					this.writeLine("&nbsp;");
					this.writeLine("<input type=\"submit\" value=\"Finish Quiz\" title=\"Finish the quiz\" onclick=\"setCommand('" + FINISH_QUIZ_ADMIN_COMMAND + "');\" class=\"submitButton\">");
					this.writeLine("</td>");
					this.writeLine("</tr>");
				}
				
				//	quiz finished, display final result and 'close' button
				else if (quiz.status == Quiz.FINISHED) {
					
					this.writeLine("<tr>");
					this.writeLine("<td class=\"configTableBody\">");
					quiz.writeDetailRanking(this, "Final Ranking");
					this.writeLine("</td>");
					this.writeLine("</tr>");
					
					this.writeLine("<td class=\"configTableBody\">");
					this.writeLine("<input type=\"submit\" value=\"Close Quiz\" title=\"Close the quiz\" onclick=\"setCommand('" + CLOSE_QUIZ_ADMIN_COMMAND + "');\" class=\"submitButton\">");
					this.writeLine("</td>");
					this.writeLine("</tr>");
				}
				
				//	quiz closed, display goodbye message
				else {
					
					this.writeLine("<tr>");
					this.writeLine("<td class=\"configTableBody\">");
					this.writeLine("Thanks for playing markup quiz, please come back soon.");
					this.writeLine("</td>");
					this.writeLine("</tr>");
				}
				
				this.writeLine("</table>");
				this.writeLine("</form>");
			}
		};
		
		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		this.sendHtmlPage(fbpReader, fpb);
		fbpReader.close();
	}
	
	private void setQuizParameters(HttpServletRequest request, Quiz quiz) {
		try {
			int answerRedundancy = Integer.parseInt(request.getParameter(CONFIGURE_QUIZ_ANSWER_REDUNDANCY_PARAMETER));
			if (answerRedundancy > 0)
				quiz.answerRedundancy = answerRedundancy;
		} catch (Exception e) {}
		
		try {
			int correctAnswerScore = Integer.parseInt(request.getParameter(CONFIGURE_QUIZ_CORRECT_ANSWER_SCORE_PARAMETER));
			if (correctAnswerScore > 0)
				quiz.correctAnswerScore = correctAnswerScore;
		} catch (Exception e) {}
		
		try {
			int wrongAnswerPenalty = Integer.parseInt(request.getParameter(CONFIGURE_QUIZ_WRONG_ANSWER_PENALTY_PARAMETER));
			if (wrongAnswerPenalty > 0)
				quiz.wrongAnswerPenalty = wrongAnswerPenalty;
		} catch (Exception e) {}
		
		try {
			int timeoutPenalty = Integer.parseInt(request.getParameter(CONFIGURE_QUIZ_REQUEST_TIMEOUT_PENALTY_PARAMETER));
			if (timeoutPenalty > 0)
				quiz.timeoutPenalty = timeoutPenalty;
		} catch (Exception e) {}
		
		try {
			int cancelPenalty = Integer.parseInt(request.getParameter(CONFIGURE_QUIZ_REQUEST_CANCEL_PENALTY_PARAMETER));
			if (cancelPenalty > 0)
				quiz.cancelPenalty = cancelPenalty;
		} catch (Exception e) {}
	}
	
	private void displayWaitingPage(final HttpServletRequest request, HttpServletResponse response, final Quiz quiz, final String message) throws IOException {
		InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackPartPage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		QuizFeedbackPageBuilder fpb = new QuizFeedbackPageBuilder(this, request, response) {
			protected void writePageHeadExtensions() throws IOException {
				//	trigger automated reload after 5 seconds
				this.writeLine("<meta http-equiv=\"refresh\" content=\"5; URL=" + this.request.getContextPath() + this.request.getServletPath() + "?" + QUIZ_ID_PARAMETER + "=" + quiz.id + "\">");
			}
			
			protected void includeBody() throws IOException {
				if (message != null) {
					this.writeLine("<p class=\"label\">" + FeedbackPanelHtmlRenderer.prepareForHtml(message) + "</p>");
				}
				
				quiz.writeRanking(this, IoTools.prepareForHtml("Current Ranking for Quiz '" + quiz.name + "'"));
				
				this.writeLine("<p class=\"functionLink\">");
				this.writeLine("<a href=\"" + request.getContextPath() + request.getServletPath() + "?" + QUIZ_ID_PARAMETER + "=" + quiz.id + "\">try again</a> (will re-try automatically after 5 seconds)");
				this.writeLine("</p>");
			}
		};
		
		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		this.sendHtmlPage(fbpReader, fpb);
		fbpReader.close();
	}
	
	private static SubmitMode[] submitModes = {
		new SubmitMode(OK_SUBMIT_MODE, "OK", "Submit feedback"),
		new SubmitMode(CANCEL_SUBMIT_MODE, "Cancel", "Cancel feedback"),
	};
	
	private void displayFeedbackRequest(FeedbackRequest fr, HttpServletRequest request, HttpServletResponse response, final Quiz quiz) throws IOException {
		InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackPartPage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		
		FeedbackFormPageBuilder ffpb = new FeedbackFormPageBuilder(this, request, response, fr.fp, fr.fpRenderer, submitModes, fr.id) {
			protected void writeHiddenFields() throws IOException {
				this.writeLine("<input type=\"hidden\" name=\"" + QUIZ_ID_PARAMETER + "\" value=\"" + quiz.id + "\">");
			}
		};
		
		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		this.sendHtmlPage(fbpReader, ffpb);
		fbpReader.close();
		
//		QuizFeedbackPageBuilder fpb = new QuizFeedbackPageBuilder(this, request, response) {
//			private Writer loopThroughWriter = new Writer() {
//				public void write(char[] cbuf, int off, int len) throws IOException {
//					loopThroughWrite(new String(cbuf, off, len));
//				}
//				public void flush() throws IOException {}
//				public void close() throws IOException {}
//			};
//			private void loopThroughWrite(String s) throws IOException {
//				this.write(s);
//			}
//			protected void writePageHeadExtensions() throws IOException {
//				
//				//	add JavaScript functions
//				this.writeLine("<script type=\"text/javascript\">");
//				
//				//	add init() function
//				this.writeLine("function init() {");
//				fr.writeJavaScriptInitFunctionBody(this.loopThroughWriter);
//				this.writeLine("}");
//				
//				//	add additional functions and variables
//				fr.writeJavaScript(this.loopThroughWriter);
//				
//				//	add submit mode function
//				this.writeLine("function setSubmitMode(mode) {");
//				this.writeLine("  document.getElementById('feedbackForm')['" + SUBMIT_MODE_PARAMETER + "'].value = mode;");
//				this.writeLine("}");
//				
//				this.writeLine("</script>");
//				
//				//	add CSS styles
//				this.writeLine("<style type=\"text/css\">");
//				fr.writeCssStyles(this.loopThroughWriter);
//				this.writeLine("</style>");
//			}
//			
//			protected String[] getOnloadCalls() {
//				String[] olc = {"init();"};
//				return olc;
//			}
//			
//			protected void includeBody() throws IOException {
//				this.writeLine("<form method=\"POST\" id=\"feedbackForm\" action=\"" + request.getContextPath() + request.getServletPath() + "\">");
//				this.writeLine("<input type=\"hidden\" name=\"" + REQUEST_ID_PARAMETER + "\" value=\"" + fr.id + "\">");
//				this.writeLine("<input type=\"hidden\" name=\"" + QUIZ_ID_PARAMETER + "\" value=\"" + quiz.id + "\">");
//				this.writeLine("<input type=\"hidden\" name=\"" + SUBMIT_MODE_PARAMETER + "\" value=\"" + CANCEL_SUBMIT_MODE + "\">");
//				this.writeLine("<table class=\"feedbackTable\">");
//				
//				String label = fr.getLabel();
//				if (label != null) {
//					this.writeLine("<tr>");
//					this.writeLine("<td class=\"feedbackTableHeader\">");
//					this.writeLine("<div style=\"border-width: 1; border-style: solid; border-color: #FF0000; padding: 5px; margin: 2px;\">");
//					this.writeLine("<b>What to do in this dialog?</b>");
//					this.writeLine("<br>");
//					this.writeLine(FeedbackPanelHtmlRenderer.prepareForHtml(label));
//					this.writeLine("</div>");
//					this.writeLine("</td>");
//					this.writeLine("</tr>");
//				}
//				
//				this.writeLine("<tr>");
//				this.writeLine("<td class=\"feedbackTableBody\">");
//				fr.writePanelBody(this.loopThroughWriter);
//				this.writeLine("</td>");
//				this.writeLine("</tr>");
//				
//				this.writeLine("<td class=\"feedbackTableBody\">");
//				this.writeLine("<input type=\"submit\" value=\"OK\" title=\"Submit feedback\" onclick=\"setSubmitMode('" + OK_SUBMIT_MODE + "');\" class=\"submitButton\">");
//				this.writeLine("&nbsp;");
//				this.writeLine("<input type=\"submit\" value=\"Cancel\" title=\"Cancel feedback\" onclick=\"window.close();\" class=\"submitButton\">");
//				this.writeLine("</td>");
//				this.writeLine("</tr>");
//				
//				this.writeLine("</table>");
//				this.writeLine("</form>");
//			}
//			
//			protected void includeLink(TreeNodeAttributeSet tnas) throws IOException {
//				String href = tnas.getAttribute("href");
//				String onclick = tnas.getAttribute("onclick");
//				if ((href == null) && (onclick == null)) return;
//				else if (href == null) href = "#";
//				
//				String label = tnas.getAttribute("label");
//				if (label == null) return;
//				
//				if ((href.indexOf('@') != -1) || ((onclick != null) && (onclick.indexOf('@') != -1))) {
//					String[] pns = fr.fp.getPropertyNames();
//					Arrays.sort(pns, new Comparator() {
//						public int compare(Object o1, Object o2) {
//							String s1 = ((String) o1);
//							String s2 = ((String) o2);
//							int c = (s2.length() - s1.length());
//							return ((c == 0) ? s1.compareTo(s2) : c);
//						}
//					});
//					for (int p = 0; p < pns.length; p++) {
//						href = href.replaceAll(("\\@" + pns[p]), fr.fp.getProperty(pns[p]));
//						if (onclick != null)
//							onclick = onclick.replaceAll(("\\@" + pns[p]), fr.fp.getProperty(pns[p]));
//					}
//					href = href.replaceAll("\\@ContextAddress", request.getContextPath());
//					if (onclick != null)
//						onclick = onclick.replaceAll("\\@ContextAddress", request.getContextPath());
//				}
//				
//				if ((href.indexOf('@') == -1) && ((onclick == null) || (onclick.indexOf('@') == -1))) {
//					String target = null;
//					if (onclick == null)
//						target = "_blank";
//					else {
//						if (!onclick.endsWith(";"))
//							onclick += ";";
//						if (!onclick.endsWith("return false;"))
//							onclick += " return false;";
//					}
//					String title = tnas.getAttribute("title");
//					this.writeLine("<span class=\"functionLink\">" +
//							"<a" +
//								" href=\"" + href + "\"" + 
//								((onclick == null) ? "" : (" onclick=\"" + onclick + "\"")) + 
//								((title == null) ? "" : (" title=\"" + title + "\"")) + 
//								((target == null) ? "" : (" target=\"" + target + "\"")) + 
//							">" + label + "</a>" +
//							"</span>");
//				}
//			}
//		};
//		
//		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
//		this.sendHtmlPage(fbpReader, fpb);
//		fbpReader.close();
	}
	
	private void displayQuizRanking(final HttpServletRequest request, HttpServletResponse response, final Quiz quiz) throws IOException {
		final InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackPartPage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		QuizFeedbackPageBuilder fpb = new QuizFeedbackPageBuilder(this, request, response) {
			protected void includeBody() throws IOException {
				quiz.writeRanking(this, ("Current Standing of Quiz '" + quiz.name + "'"));
				this.writeLine("<p class=\"functionLink\"><a href=\"#\" onclick=\"window.close();\">close ranking window</a></p>");
			}
		};
		
		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		this.sendHtmlPage(fbpReader, fpb);
		fbpReader.close();
	}
	
	private void displayQuizResult(final HttpServletRequest request, HttpServletResponse response, final Quiz quiz) throws IOException {
		InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackPartPage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		QuizFeedbackPageBuilder fpb = new QuizFeedbackPageBuilder(this, request, response) {
			protected String[] getOnloadCalls() {
				String[] olc = {"parent.quizFinished = true;"};
				return olc;
			}
			protected void includeBody() throws IOException {
				quiz.writeDetailRanking(this, ("Final Ranking of Quiz '" + quiz.name + "':"));
				this.writeLine("<p class=\"functionLink\"><a target=\"_parent\" href=\"" + request.getContextPath() + request.getServletPath() + "?" + QUIZ_ID_PARAMETER + "=" + LOGOUT_QUIZ_ID + "\">log out</a></p>");
			}
		};
		
		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		this.sendHtmlPage(fbpReader, fpb);
		fbpReader.close();
	}
	
	private void displayThankYouPage(final HttpServletRequest request, HttpServletResponse response) throws IOException {
		InputStream is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(feedbackBasePage)));
		
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		QuizFeedbackPageBuilder fpb = new QuizFeedbackPageBuilder(this, request, response) {
			protected void includeBody() throws IOException {
				this.writeLine("<p class=\"functionLink\">Thank you for playing feedback quiz. Hope you enjoyed it, and hope to see you again soon.</p>");
				this.writeLine("<p class=\"functionLink\"><a href=\"" + request.getContextPath() + request.getServletPath() + "\">back to start page</a></p>");
			}
		};
		
		BufferedReader fbpReader = new BufferedReader(new InputStreamReader(is, "utf-8"));
		this.sendHtmlPage(fbpReader, fpb);
		fbpReader.close();
	}
	
	
	private abstract class QuizFeedbackPageBuilder extends HtmlPageBuilder {
		QuizFeedbackPageBuilder(HtmlPageBuilderHost host, HttpServletRequest request, HttpServletResponse response) throws IOException {
			super(host, request, response);
		}
		protected void include(String type, String tag) throws IOException {
			if ("includeBody".equals(type))
				this.includeBody();
			else if ("includeLink".equals(type)) {
//				TreeNodeAttributeSet as = TreeNodeAttributeSet.getTagAttributes(tag, html);
//				this.includeLink(as);
			}
			else super.include(type, tag);
		}
		
		protected abstract void includeBody() throws IOException;
//		
//		protected void includeLink(TreeNodeAttributeSet tnas) {}
	}
//	
//	private abstract class QuizFeedbackPageBuilderOld extends TokenReceiver {
//		private boolean inHyperLink = false;
//		protected BufferedWriter out;
//		protected String contextAddress;
//		QuizFeedbackPageBuilderOld(BufferedWriter out, String contextAddress) throws IOException {
//			this.out = out;
//			this.contextAddress = contextAddress;
//		}
//		
//		public void close() throws IOException {
//			this.out.flush();
//			this.out.close();
//		}
//		
//		protected void includeHead() throws IOException {}
//		
//		protected void writeBodyStartTag() throws IOException {
//			this.out.write("<body>");
//			this.out.newLine();
//		}
//		
//		protected abstract void includeBody() throws IOException;
//		
//		protected void includeLink(TreeNodeAttributeSet tnas) throws IOException {}
//		
//		public void storeToken(String token, int treeDepth) throws IOException {
//			if (HTML.isTag(token)) {
//				if ("includeFile".equals(HTML.getType(token))) {
//					TreeNodeAttributeSet as = TreeNodeAttributeSet.getTagAttributes(token, HTML);
//					String includeFile = as.getAttribute("file");
//					if (includeFile != null) {
//						this.out.newLine();
//						this.out.flush();
//						this.includeFile(includeFile);
//						this.out.newLine();
//					}
//				}
//				else if ("includeBody".equals(HTML.getType(token))) {
//					if ((HTML.isEndTag(token) || HTML.isSingularTag(token)))
//						this.includeBody();
//				}
//				else if ("includeLink".equals(HTML.getType(token))) {
//					if ((!HTML.isEndTag(token) || HTML.isSingularTag(token))) {
//						TreeNodeAttributeSet as = TreeNodeAttributeSet.getTagAttributes(token, HTML);
//						this.includeLink(as);
//					}
//				}
//				else if ("head".equalsIgnoreCase(HTML.getType(token)) && HTML.isEndTag(token)) {
//					if (cssName != null) {
//						String cssUrl = this.contextAddress + dataPath + "/" + cssName;
//						this.out.write("<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"" + cssUrl + "\"></link>");
//						this.out.newLine();
//					}
//					this.includeHead();
//				}
//				else if ("body".equalsIgnoreCase(HTML.getType(token)) && !HTML.isEndTag(token)) {
//					this.writeBodyStartTag();
//				}
//				else {
//					String type = HTML.getType(token);
//					
//					//	make image adddresses absolute
//					if ("img".equalsIgnoreCase(type))
//						token = token.replaceAll("\\s(S|s)(R|r)(C|c)\\=\\\"", (" src=\"" + this.contextAddress + "/"));
//					
//					this.out.write(token);
//					
//					// do not insert line break after hyperlink tags, bold tags, and span tags
//					if (!"a".equalsIgnoreCase(type) && !"b".equalsIgnoreCase(type) && !"span".equalsIgnoreCase(type)) this.out.newLine();
//					
//					//	remember being in hyperlink
//					if ("a".equals(type)) this.inHyperLink = !HTML.isEndTag(token);
//				}
//			}
//			else {
//				//	remove spaces from links, and activate them
//				if ((token.startsWith("http:") || token.startsWith("ftp:")) && (token.indexOf("tp: //") != -1)) {
//					String link = token.replaceAll("\\s", "");
//					if (!this.inHyperLink) this.out.write("<a href=\"" + link + "\">");
//					this.out.write(link);
//					if (!this.inHyperLink) this.out.write("</a>");
//				} else this.out.write(token);
//			}
//		}
//		
//		private void includeFile(String fileName) throws IOException {
//			InputStream is = null;
//			try {
//				TokenReceiver fr = new TokenReceiver() {
//					private boolean inBody = false;
//					public void close() throws IOException {}
//					public void storeToken(String token, int treeDepth) throws IOException {
//						if (HTML.isTag(token) && "body".equalsIgnoreCase(HTML.getType(token))) {
//							if (HTML.isEndTag(token)) this.inBody = false;
//							else this.inBody = true;
//						} else if (this.inBody) QuizFeedbackPageBuilder.this.storeToken(token, treeDepth);
//					}
//				};
//				is = new ByteOrderMarkFilterInputStream(new FileInputStream(findFile(fileName)));
//				HTML_PARSER.stream(is, fr);
//			}
//			catch (FileNotFoundException fnfe) {
//				//	ignore inclusions that don't exist
//				System.out.println("Include file not found: " + fileName);
//			}
//			catch (Exception e) {
//				throw new IOException(e.getMessage());
//			}
//			finally {
//				if (is != null)
//					is.close();
//			}
//		}
//	}
//	
//	private File findFile(String fileName) throws FileNotFoundException {
//		File file;
//		
//		file = new File(this.dataFolder, fileName);
//		if (file.exists()) return file;
//		
//		file = new File(this.rootFolder, fileName);
//		if (file.exists()) return file;
//		
//		throw new FileNotFoundException(fileName);
//	}
//	
//	private class ByteOrderMarkFilterInputStream extends FilterInputStream {
//		private boolean inContent = false;
//		
//		/**
//		 * Constructor
//		 * @param in the input stream to wrap
//		 */
//		public ByteOrderMarkFilterInputStream(InputStream in) {
//			super(in);
//		}
//		
//		/* (non-Javadoc)
//		 * @see java.io.FilterInputStream#read()
//		 */
//		public int read() throws IOException {
//			int i = super.read();
//			while (!this.inContent) {
//				if (i == '<') this.inContent = true;
//				else i = super.read();
//			}
//			return i;
//		}
//
//		/* (non-Javadoc)
//		 * @see java.io.FilterInputStream#read(byte[], int, int)
//		 */
//		public int read(byte[] b, int off, int len) throws IOException {
//			if (this.inContent)	return super.read(b, off, len);
//			else {
//				int i = super.read();
//				while (!this.inContent) {
//					if (i == '<') this.inContent = true;
//					else i = super.read();
//				}
//				b[off] = ((byte) i);
//				return (1 + super.read(b, (off + 1), (len - 1)));
//			}
//		}
//	}
	
	private HashMap quizzesById = new HashMap();
	private HashMap quizzesByName = new HashMap();
	
	/**
	 * A quiz instance, representing a single quiz in progress. This class is a
	 * feedback engine in its own right.
	 * 
	 * @author sautter
	 */
	protected class Quiz extends HtmlFeedbackEngine {
		
		long lastAccessed = System.currentTimeMillis();
		
		final String id = Gamta.getAnnotationID();
		final String name;
		
		final String passCode;
		final String hostUserName;
		
		int answerRedundancy = 2;
		
		int correctAnswerScore = 1;
		int wrongAnswerPenalty = 1;
		
		int timeoutPenalty = 1;
		int cancelPenalty = 1;
		
		int status = INITIALIZING;
		
		static final int INITIALIZING = 0;
		static final int RUNNING = 1;
		static final int PAUSED = 2;
		static final int FINISHED = 4;
		static final int CLOSED = 8;
		
		TreeMap users = new TreeMap();
		
		private class User {
			final String name;
			int requestsShown = 0;
			int requestsAnswered = 0;
			int requestAnswerTimeTotal = 0;
			int requestsCancelled = 0;
			int requestsTimedOut = 0;
			int score = 0;
			int penalty = 0;
			User(String name) {
				this.name = name;
			}
			int getScore() {
				return (this.score - this.penalty);
			}
		}
		
		protected Quiz(String name, String hostUserName, String passCode) {
			this.name = name;
			this.hostUserName = hostUserName;
			this.passCode = passCode;
		}
		
		boolean isHosted() {
			return (this.passCode != null);
		}
		
		synchronized String start() {
			this.lastAccessed = System.currentTimeMillis();
			
			if (this.status == INITIALIZING) {
				if (this.users.size() < ((this.answerRedundancy * 2) - 1))
					return "Cannot start, not enough users participating for desired answer redundancy.";
				else {
					this.status = RUNNING;
					return null;
				}
			}
			else if (this.status == RUNNING) return "Quiz already running.";
			else if (this.status == PAUSED) {
				this.status = RUNNING;
				return null;
			}
			else if (this.status == FINISHED) return "Quiz finished, cannot restart.";
			else if (this.status == CLOSED) return "Quiz closed, cannot restart.";
			else return "Quiz in unknown state.";
		}
		
		synchronized String pause() {
			this.lastAccessed = System.currentTimeMillis();
			
			if (this.status == INITIALIZING) return "Quiz not running yet.";
			else if (this.status == RUNNING) {
				this.status = PAUSED;
				return null;
			}
			else if (this.status == PAUSED) return "Quiz already paused.";
			else if (this.status == FINISHED) return "Quiz finished, cannot pause any more.";
			else if (this.status == CLOSED) return "Quiz closed, cannot pause any more.";
			else return "Quiz in unknown state.";
		}
		
		synchronized String finish() {
			this.lastAccessed = System.currentTimeMillis();
			
			if (this.status == INITIALIZING) return "Quiz not running yet, need to run before finishing.";
			else if (this.status == RUNNING) return "Quiz running, need to pause first in order to finish.";
			else if (this.status == PAUSED) {
				this.status = FINISHED;
				return null;
			}
			else if (this.status == FINISHED) return "Quiz already finished.";
			else if (this.status == CLOSED) return "Quiz already closed.";
			else return "Quiz in unknown state.";
		}
		
		synchronized String close() {
			this.lastAccessed = System.currentTimeMillis();
			
			if (this.status == INITIALIZING) return "Quiz not running yet, need to run before closing.";
			else if (this.status == RUNNING) return "Quiz not finished yet, need to finish first in order to close.";
			else if (this.status == PAUSED) return "Quiz not finished yet, need to finish first in order to close.";
			else if (this.status == FINISHED) {
				this.status = CLOSED;
				this.shutdown();
				return null;
			}
			else if (this.status == CLOSED) return "Quiz already closed.";
			else return "Quiz in unknown state.";
		}
		
		protected synchronized boolean authenticate(String userName, String password) {
			this.lastAccessed = System.currentTimeMillis();
			
			//	quiz already running, too late to join
			if (this.status != INITIALIZING)
				return false;
			
			//	open quiz, authenticate with global account
			if (this.passCode == null)
				return QuizHtmlFeedbackServlet.this.authenticate(userName, password);
			
			//	hosted quiz, authenticate with local pass code
			else return this.passCode.equals(password);
		}
		
		synchronized boolean addUser(String userName) {
			this.lastAccessed = System.currentTimeMillis();
			
			if (this.users.containsKey(userName))
				return false;
			else {
				this.users.put(userName, new User(userName));
				return true;
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#getFeedbackRequest(java.lang.String)
		 */
		public synchronized FeedbackRequest getFeedbackRequest(String userName) {
			this.lastAccessed = System.currentTimeMillis();
			
			return ((this.status == RUNNING) ? super.getFeedbackRequest(userName) : null);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#getTimeout(de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest, java.lang.String)
		 */
		protected int getTimeout(FeedbackRequest fr, String userName) {
			return Math.max((
					Math.max(1, fr.getDecisionComplexity()) // time per decision
					* 
					Math.max(1, fr.getDecisionCount()) // number of decisions
					)
					,
					60 // minimum timeout
					);
		}
		
		private LinkedList requestQueue = new LinkedList();
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#findFeedbackRequest(java.lang.String)
		 */
		protected FeedbackRequest findFeedbackRequest(String userName) {
			if (userName == null) return null;
			
			FeedbackRequest fr = null;
			LinkedList cancelledFrs = new LinkedList(); // used for tracking request already cancelled by user
			synchronized (this.requestQueue) {
				for (Iterator rit = this.requestQueue.iterator(); (fr == null) && rit.hasNext();) {
					
					//	retrieve first request
					fr = ((FeedbackRequest) rit.next());
					
					//	forget about requests already answered
					if (fr.wasAnsweredBy(userName))
						fr = null;
					
					else {
						
						//	extract request from list
						rit.remove();
						
						//	if requset has been cancelled, put it in fallback list
						if (fr.wasCancelledBy(userName)) {
							cancelledFrs.addLast(fr);
							fr = null;
						}
					}
				}
				
				//	fall back to requests the user has cancelled
				if ((fr == null) && !cancelledFrs.isEmpty())
					fr = ((FeedbackRequest) cancelledFrs.removeFirst());
				
				//	put back remainder of fallback list
				this.requestQueue.addAll(cancelledFrs);
			}
			
			//	get (at most 10) new requests until one fits requesting user TODO: make limit a config parameter
			for (int attempt = 0; attempt < 10; attempt++) {
				
				//	get feedback panel
				FeedbackPanel fp = QuizHtmlFeedbackServlet.this.getFeedbackPanel();
				
				//	no request pending (won't change with further attempts)
				if (fp == null) {
					System.out.println("QuizHtmlFeedbackServlet.Quiz: feedback request for " + userName + " not found in backing source");
					return null;
				}
				else System.out.println("QuizHtmlFeedbackServlet.Quiz: got feedback request for " + userName + " from backing source: " + fp.getClass().getName());
				
				//	obtain renderer
				FeedbackPanelHtmlRendererInstance fpRenderer = FeedbackPanelHtmlRenderer.getRenderer(fp);
				
				//	renderer not found, handle back to backing source
				if (fpRenderer == null) {
					System.out.println(" - renderer not found ==> unusable");
					QuizHtmlFeedbackServlet.this.cancelFeedbackPanel(fp);
				}
				
				//	got request
				else {
					System.out.println(" - renderer found, returning feedback request");
					return new QuizFeedbackRequest(fp, fpRenderer, this, this.answerRedundancy);
				}
			}
			
			//	no request found for user, despite all efforts
			return null;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#returnFeedbackRequest(de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest, boolean)
		 */
		protected void returnFeedbackRequest(FeedbackRequest fr, boolean wasAnswered) {
			
			//	is this request finished?
			if (wasAnswered && fr.isAnswered()) {
				
				//	compute scores
				this.feedbackRequestAnswered(fr);
				
				//	hand back to backing source
				QuizHtmlFeedbackServlet.this.answerFeedbackPanel(fr.fp);
			}
			
			//	enqueue for other users to answer
			else synchronized (this.requestQueue) {
				this.requestQueue.addLast(fr);
			}
		}
		
		private class QuizFeedbackRequest extends FeedbackRequest {
			private ParameterValueAggregator answerData;
			private Map correctAnswerCounts = null;
			
			QuizFeedbackRequest(FeedbackPanel fp, FeedbackPanelHtmlRendererInstance fpRenderer, Quiz quiz, int answerRedundancy) {
				super(fp, fpRenderer, quiz);
				
				//	get request complexity
				int fpComplexity = this.getDecisionComplexity();
				if (fpComplexity < 1) fpComplexity = 1;
				
				//	how many agreeing users can we reach in the worst case?
				int maxRedundancy = ((users.size() + (fpComplexity - 1)) / fpComplexity);
				answerRedundancy = Math.min(answerRedundancy, maxRedundancy);
				
				//	make sure the available number of users can reach a decision
				this.answerData  = new ParameterValueAggregator(answerRedundancy);
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#addAnswerData(java.util.Properties, java.lang.String)
			 */
			protected void addAnswerData(Properties answerData, String userName) {
				this.answerData.addParameterValues(answerData, userName);
				
				//	if answer is complete, count correct decisions for each user
				if (this.answerData.isComplete()) {
					
					//	line up users
					String[] answerUserNames = this.getAnsweringUsers();
					ArrayList answerUserList = new ArrayList();
					for (int u = 0; u < answerUserNames.length; u++) {
						User answerUser = ((User) Quiz.this.users.get(answerUserNames[u]));
						if (answerUser != null)
							answerUserList.add(answerUser);
					}
					User[] answerUsers = ((User[]) answerUserList.toArray(new User[answerUserList.size()]));
					
					//	prepare counting array
					int[] correctAnswerCounts = new int[answerUsers.length];
					Arrays.fill(correctAnswerCounts, 0);
					
					//	count correct answers per user
					String[] answerParamNames = this.answerData.getParameterNames();
					for (int p = 0; p < answerParamNames.length; p++) {
						String answerParamValue = this.answerData.getParameterValue(answerParamNames[p]);
						Set correctAnswerUserSet = new HashSet(Arrays.asList(this.answerData.getVoteUsers(answerParamNames[p], answerParamValue)));
						for (int u = 0; u < answerUsers.length; u++) {
							if (correctAnswerUserSet.contains(answerUsers[u].name))
								correctAnswerCounts[u]++;
						}
					}
					
					//	store correct answer counts
					this.correctAnswerCounts = new HashMap();
					for (int u = 0; u < answerUsers.length; u++)
						this.correctAnswerCounts.put(answerUsers[u].name, new Integer(correctAnswerCounts[u]));
				}
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#isAnswered()
			 */
			public boolean isAnswered() {
				return this.answerData.isComplete();
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#getAnswerData()
			 */
			protected Properties getAnswerData() {
				return this.answerData.toProperties();
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#getAnswerCount()
			 */
			public int getAnswerCount() {
				return this.answerData.size();
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#getCorrectAnswerCount(java.lang.String)
			 */
			public int getCorrectAnswerCount(String userName) {
				if (this.correctAnswerCounts == null)
					return -1;
				Integer cac = ((Integer) this.correctAnswerCounts.get(userName));
				return ((cac == null) ? 0 : cac.intValue());
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#shutdown()
		 */
		protected void shutdown() {
			super.shutdown();
			
			//	cancel all request in queue
			synchronized(this.requestQueue) {
				while (!this.requestQueue.isEmpty())
					QuizHtmlFeedbackServlet.this.cancelFeedbackPanel(((FeedbackRequest) this.requestQueue.removeFirst()).fp);
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#filterResponseData(java.util.Properties)
		 */
		protected void filterResponseData(Properties responseData) {
			responseData.remove(USER_NAME_PARAMETER);
			responseData.remove(PASSWORD_PARAMETER);
			responseData.remove(QUIZ_ID_PARAMETER);
			
			responseData.remove(QUIZ_PASSCODE_PARAMETER);
			responseData.remove(QUIZ_HOST_PARAMETER);
			responseData.remove(QUIZ_TYPE_PARAMETER);
			responseData.remove(QUIZ_ADMIN_COMMAND_PARAMETER);
			
			responseData.remove(CONFIGURE_QUIZ_ANSWER_REDUNDANCY_PARAMETER);
			responseData.remove(CONFIGURE_QUIZ_CORRECT_ANSWER_SCORE_PARAMETER);
			responseData.remove(CONFIGURE_QUIZ_WRONG_ANSWER_PENALTY_PARAMETER);
			responseData.remove(CONFIGURE_QUIZ_REQUEST_TIMEOUT_PENALTY_PARAMETER);
			responseData.remove(CONFIGURE_QUIZ_REQUEST_CANCEL_PENALTY_PARAMETER);
			
			responseData.remove(REQUEST_ID_PARAMETER);
			responseData.remove(SUBMIT_MODE_PARAMETER);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#feedbackRequestAnswered(de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest, java.lang.String)
		 */
		protected synchronized void feedbackRequestAnswered(FeedbackRequest fr, String userName) {
			this.lastAccessed = System.currentTimeMillis();
			
			User user = ((User) this.users.get(userName));
			if (user != null) {
				user.requestsShown++;
				user.requestsAnswered++;
				user.requestAnswerTimeTotal += fr.getAnswerTime(userName);
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#feedbackRequestCancelled(de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest, java.lang.String)
		 */
		protected synchronized void feedbackRequestCancelled(FeedbackRequest fr, String userName) {
			this.lastAccessed = System.currentTimeMillis();
			
			User user = ((User) this.users.get(userName));
			if (user != null) {
				user.requestsShown++;
				user.requestsCancelled++;
				user.penalty += (fr.getDecisionCount() * this.cancelPenalty);
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#feedbackRequestTimedOut(de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest, java.lang.String)
		 */
		protected synchronized void feedbackRequestTimedOut(FeedbackRequest fr, String userName) {
			User user = ((User) this.users.get(userName));
			if (user != null) {
				user.requestsShown++;
				user.requestsTimedOut++;
				user.penalty += (fr.getDecisionCount() * this.timeoutPenalty);
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#feedbackRequestAnswered(de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest)
		 */
		protected synchronized void feedbackRequestAnswered(FeedbackRequest fr) {
			this.lastAccessed = System.currentTimeMillis();
			
			//	get answering users
			String[] answerUserNames = fr.getAnsweringUsers();
			ArrayList answerUserList = new ArrayList();
			for (int u = 0; u < answerUserNames.length; u++) {
				User answerUser = ((User) this.users.get(answerUserNames[u]));
				if (answerUser != null)
					answerUserList.add(answerUser);
			}
			User[] answerUsers = ((User[]) answerUserList.toArray(new User[answerUserList.size()]));
			
			//	prepare computing user scores
			int answerCount = fr.getDecisionCount();
			
			//	compute score for correct answer based on complexity of decision
			int correctAnswerScore = (Math.max(1, fr.getDecisionComplexity()) * this.correctAnswerScore);
			
			//	compute penalty for wrong answer based on complexity of decision
			int wrongAnswerPenalty = (Math.min(1, (fr.getDecisionComplexity() - 1)) * this.wrongAnswerPenalty);
			
			//	do scoring for individual parameters
			for (int u = 0; u < answerUsers.length; u++) {
				int correctAnswerCount = fr.getCorrectAnswerCount(answerUsers[u].name);
				answerUsers[u].score += (correctAnswerScore * correctAnswerCount);
				answerUsers[u].penalty += (wrongAnswerPenalty * (answerCount - correctAnswerCount));
			}
			
			//	adjust statistical properties of feedback panel to quiz host
			if (this.isHosted()) {
				String[] pns = fr.fp.getPropertyNames();
				int answerTime = 0;
				for (int p = 0; p < pns.length; p++) {
					if (pns[p].startsWith(FeedbackPanel.CORRECT_ANSWER_COUNT_PROPERTY_PREFIX))
						fr.fp.removeProperty(pns[p]);
					else if (pns[p].startsWith(FeedbackPanel.ANSWER_TIME_PROPERTY_PREFIX)) {
						try {
							answerTime = Math.max(answerTime, Integer.parseInt(fr.fp.getProperty(pns[p])));
						} catch (NumberFormatException nfe) {}
						fr.fp.removeProperty(pns[p]);
					}
				}
				fr.fp.setProperty((FeedbackPanel.CORRECT_ANSWER_COUNT_PROPERTY_PREFIX + this.hostUserName), ("" + fr.getDecisionCount()));
				fr.fp.setProperty((FeedbackPanel.ANSWER_TIME_PROPERTY_PREFIX + this.hostUserName), ("" + answerTime));
			}
		}
		
		/**
		 * Write the current ranking of the quiz' participants to some writer.
		 * @param out the writer to write to
		 * @param title the title to display above the user ranking
		 * @throws IOException
		 */
		protected synchronized void writeRanking(QuizFeedbackPageBuilder out, String title) throws IOException {
			
			//	open table
			out.writeLine("<table class=\"rankingTable\">");
			
			//	write title if given
			if (title != null) {
				out.writeLine("<tr>");
				out.write("<td colspan=\"4\" class=\"rankingTableHeader\">");
				out.write(IoTools.prepareForHtml(title));
				out.writeLine("</td>");
				out.writeLine("</tr>");
			}
			
			//	add head row
			out.writeLine("<tr>");
			out.writeLine("<td width=\"10%\" class=\"rankingTableHeader\">Rank</td>");
			out.writeLine("<td width=\"40%\" class=\"rankingTableHeader\">User Name</td>");
			out.writeLine("<td width=\"30%\" class=\"rankingTableHeader\">Requests Answered</td>");
			out.writeLine("<td width=\"20%\" class=\"rankingTableHeader\">Score</td>");
			out.writeLine("</tr>");
			
			//	get & sort user data
			User[] users = ((User[]) this.users.values().toArray(new User[this.users.size()]));
			Arrays.sort(users, new Comparator() {
				public int compare(Object o1, Object o2) {
					User u1 = ((User) o1);
					User u2 = ((User) o2);
					int c = u2.getScore() - u1.getScore();
					return ((c == 0) ? u1.name.compareTo(u2.name) : c);
				}
			});
			int rank = 1;
			for (int u = 0; u < users.length; u++) {
				
				//	adjust rank
				if ((u != 0) && (users[u-1].getScore() > users[u].getScore()))
					rank = (u+1);
				
				out.writeLine("<tr>");
				out.writeLine("<td width=\"10%\" class=\"rankingTableBody\">" + rank + "</td>");
				out.writeLine("<td width=\"40%\" class=\"rankingTableBody\">" + IoTools.prepareForHtml(users[u].name) + (this.hostUserName.equals(users[u].name) ? " (Host)" : "") + "</td>");
				out.writeLine("<td width=\"30%\" class=\"rankingTableBody\">" + users[u].requestsAnswered + "</td>");
				out.writeLine("<td width=\"20%\" class=\"rankingTableBody\">" + users[u].getScore() + "</td>");
				out.writeLine("</tr>");
			}
			
			//	close table
			out.writeLine("</table>");
		}
		
		/**
		 * Write the final ranking of the quiz' participants to some writer.
		 * @param out the writer to write to
		 * @param title the title to display above the user ranking
		 * @throws IOException
		 */
		protected synchronized void writeDetailRanking(QuizFeedbackPageBuilder out, String title) throws IOException {
			
			//	open table
			out.writeLine("<table class=\"rankingTable\">");
			
			//	write title if given
			if (title != null) {
				out.writeLine("<tr>");
				out.write("<td colspan=\"5\" class=\"rankingTableHeader\">");
				out.write(IoTools.prepareForHtml(title));
				out.writeLine("</td>");
				out.writeLine("</tr>");
			}
			
			//	add head row
			out.writeLine("<tr>");
			out.writeLine("<td width=\"10%\" class=\"rankingTableHeader\">Rank</td>");
			out.writeLine("<td width=\"30%\" class=\"rankingTableHeader\">User Name</td>");
			out.writeLine("<td width=\"30%\" class=\"rankingTableHeader\">Requests (Answ./Canc./TO.)</td>");
			out.writeLine("<td width=\"10%\" class=\"rankingTableHeader\">Seconds per Answer</td>");
			out.writeLine("<td width=\"20%\" class=\"rankingTableHeader\">Score (Sco./Pen.)</td>");
			out.writeLine("</tr>");
			
			//	get & sort user data
			User[] users = ((User[]) this.users.values().toArray(new User[this.users.size()]));
			Arrays.sort(users, new Comparator() {
				public int compare(Object o1, Object o2) {
					User u1 = ((User) o1);
					User u2 = ((User) o2);
					int c = u2.getScore() - u1.getScore();
					return ((c == 0) ? u1.name.compareTo(u2.name) : c);
				}
			});
			int rank = 1;
			for (int u = 0; u < users.length; u++) {
				
				//	adjust rank
				if ((u != 0) && (users[u-1].getScore() > users[u].getScore()))
					rank = (u+1);
				
				out.writeLine("<tr>");
				out.writeLine("<td width=\"10%\" class=\"rankingTableBody\">" + rank + "</td>");
				out.writeLine("<td width=\"30%\" class=\"rankingTableBody\">" + IoTools.prepareForHtml(users[u].name) + (this.hostUserName.equals(users[u].name) ? " (Host)" : "") + "</td>");
				out.writeLine("<td width=\"30%\" class=\"rankingTableBody\">" + users[u].requestsShown + "(" + users[u].requestsAnswered + "/" + users[u].requestsCancelled + "/" + users[u].requestsTimedOut + ")</td>");
				out.writeLine("<td width=\"10%\" class=\"rankingTableBody\">" + ((users[u].requestsAnswered == 0) ? "N/A" : ("" + (users[u].requestAnswerTimeTotal / users[u].requestsAnswered))) + "</td>");
				out.writeLine("<td width=\"20%\" class=\"rankingTableBody\">" + users[u].getScore() + "(" + users[u].score + "/" + users[u].penalty + ")</td>");
				out.writeLine("</tr>");
			}
			
			//	close table
			out.writeLine("</table>");
		}
	}
	
	private class QuizTimeoutWatchdog extends Thread {
		private final Object sleepLock = new Object();
		private boolean doWork = true;
		
		public void run() {
			while (this.doWork) {
				
				synchronized(this.sleepLock) {
					this.sleepLock.notify();
					try {
						this.sleepLock.wait((quizTimeout / 60) * 1000); // check every 60-th part of timeout
					} catch (InterruptedException ie) {}
				}
				
				if (this.doWork) {
					long currentTime = System.currentTimeMillis();
					Quiz[] quizzes = ((Quiz[]) quizzesById.values().toArray(new Quiz[quizzesById.size()]));
					for (int q = 0; q < quizzes.length; q++)
						/*
						 * using days instead of hours for timing out
						 * initializing quizzes
						 * ==> no problem, because blocking no requests
						 * ==> early initialization facilitates sending invitations
						 */
						if ((currentTime - quizzes[q].lastAccessed) > (quizTimeout * ((quizzes[q].status == Quiz.INITIALIZING) ? 24000 : 1000))) {
							quizzes[q].status = Quiz.CLOSED;
							quizzes[q].shutdown();
							quizzesById.remove(quizzes[q].id);
							quizzesByName.remove(quizzes[q].name);
						}
				}
			}
		}
		
		void shutdown() {
			synchronized(this.sleepLock) {
				this.doWork = false;
				this.sleepLock.notify();
			}
		}
		
		public void start() {
			synchronized(this.sleepLock) {
				super.start();
				try {
					this.sleepLock.wait();
				} catch (InterruptedException ie) {}
			}
		}
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

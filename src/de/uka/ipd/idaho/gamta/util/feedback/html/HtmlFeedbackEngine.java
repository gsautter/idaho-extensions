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

import java.awt.Dimension;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance;

/**
 * An HtmlFeedbackEngine acts as the adapter between a web front-end (e.g. a
 * servlet) and a backing feedback service. The engine handles retrieving
 * feedback panels from a backing feedback service (to be implemented by sub
 * classes), user authentication, finding renderers for feedback panels,
 * coordination of showing feedback dialogs to users, feedback dialog timeouts,
 * aggregation of user's answers, and putting the answers back into the original
 * feedback panels.<br>
 * <br>
 * Surrounding servlets are responsible for the overall layout of the HTML
 * representation of the feedback panels. It is sensible, though, to rely on
 * feedback panel renderers contained in the FeedbackRequest objects for the
 * detail layout.
 * 
 * @author sautter
 */
public abstract class HtmlFeedbackEngine {
	
	protected HtmlFeedbackEngine() {
		
		//	start request timeout thread
		this.showingRequestTimeoutThread = new ShowingRequestTimeoutThread();
		this.showingRequestTimeoutThread.start();
	}
	
	/**
	 * Shut down the feedback engine. This will cancel all pending requests and
	 * shut down the timeout handler.
	 */
	protected void shutdown() {
		
		//	shut down timout thread
		synchronized (this.showingRequestTimeoutQueue) {
			this.showingRequestTimeoutQueue.add(SHUTDOWN);
			this.showingRequestTimeoutQueue.notify();
		}
		
		//	time out all remaining requests
		while (!this.showingRequestTimeoutQueue.isEmpty()) {
			ShowingRequestTimeout srt = ((ShowingRequestTimeout) showingRequestTimeoutQueue.first());
			showingRequestTimeoutQueue.remove(srt);
			FeedbackRequest fr = ((FeedbackRequest) this.showingRequests.remove(srt.frId + "." + srt.userName));
			if (fr != null)
				this.returnFeedbackRequest(fr, false);
		}
	}
	
	private Map showingRequests = Collections.synchronizedMap(new HashMap());
	
	/**
	 * Retrieve a feedback request to be answered by a given user. Feedback
	 * requests obtained from this method are marked as checkd out by the
	 * specified user, and are scheduled for timeout after the number of seconds
	 * returned by the getTimeout(FeedbackRequest, String) method.
	 * @param userName the name of the user to show the feedback request to
	 * @return a feedback request to answer by the specified user, or null, if
	 *         there are currently no feedback requests for the user to answer
	 */
	public FeedbackRequest getFeedbackRequest(String userName) {
		System.out.println("HtmlFeedbackEngine: getting feedback request for " + userName);
		FeedbackRequest fr = this.findFeedbackRequest(userName);
		if (fr == null) {
			System.out.println("HtmlFeedbackEngine: feedback request for " + userName + " not found");
			return null;
		}
		else {
			System.out.println("HtmlFeedbackEngine: got feedback request for " + userName + ": " + fr.fp.getClass().getName());
			this.showingRequests.put((fr.id + "." + userName), fr);
			synchronized (this.showingRequestTimeoutQueue) {
				this.showingRequestTimeoutQueue.add(new ShowingRequestTimeout(this.getTimeout(fr, userName), fr.id, userName));
				this.showingRequestTimeoutQueue.notify();
			}
			fr.checkout(userName);
			return fr;
		}
	}
	
	private TreeSet showingRequestTimeoutQueue = new TreeSet();
	private ShowingRequestTimeout SHUTDOWN = new ShowingRequestTimeout(-1, "", "");
	private ShowingRequestTimeoutThread showingRequestTimeoutThread;
	
	private class ShowingRequestTimeout implements Comparable {
		final long timeout;
		final String frId;
		final String userName;
		ShowingRequestTimeout(int timeout, String frId, String userName) {
			this.timeout = (System.currentTimeMillis() + (timeout * 1000));
			this.frId = frId;
			this.userName = userName;
		}
		public int compareTo(Object obj) {
			int c = ((int) (this.timeout - ((ShowingRequestTimeout) obj).timeout));
			return ((c == 0) ? this.frId.compareTo(((ShowingRequestTimeout) obj).frId) : c);
		}
	}
	
	private class ShowingRequestTimeoutThread extends Thread {
		public void run() {
			while (true) {
				synchronized (showingRequestTimeoutQueue) {
					
					//	no timeouts pending, wait until timeout enqueued
					if (showingRequestTimeoutQueue.isEmpty()) {
						try {
							showingRequestTimeoutQueue.wait();
						} catch (InterruptedException ie) {}
					}
					
					//	timeouts pending, wait until first timeout due
					else {
						
						//	pull first timeout
						ShowingRequestTimeout srt = ((ShowingRequestTimeout) showingRequestTimeoutQueue.first());
						showingRequestTimeoutQueue.remove(srt);
						
						//	shutdown command
						if (srt == SHUTDOWN) return;
						
						//	get time
						long currentTime = System.currentTimeMillis();
						
						//	first timeout not yet over, put timeout back and wait until it's over
						if (currentTime < srt.timeout) {
							showingRequestTimeoutQueue.add(srt);
							try {
								showingRequestTimeoutQueue.wait(srt.timeout - currentTime);
							} catch (InterruptedException ie) {}
						}
						
						//	first timeout over
						else {
							
							//	check if request still showing with user
							FeedbackRequest fr = ((FeedbackRequest) showingRequests.remove(srt.frId + "." + srt.userName));
							if (fr != null) {
								fr.timeout();
								returnFeedbackRequest(fr, false);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Cancel a feedback request. This method is intended for invocation by a
	 * front-end servlet that displays feedback requests to users.
	 * @param requestId the ID of the feedback request to cancel
	 * @param userName the name of the user who cancelled the feedback request
	 */
	public void cancelFeedbackRequest(String requestId, String userName) {
		FeedbackRequest fr = ((FeedbackRequest) this.showingRequests.remove(requestId + "." + userName));
		if (fr != null) {
			System.out.println("Request " + requestId + " cancelled by user " + userName);
			fr.cancel();
			this.returnFeedbackRequest(fr, false);
		}
	}
	
	/**
	 * Answer a feedback request, i.e. add the asnwer data submitted by a
	 * specific user to the answer of the request. This method is intended for
	 * invocation by a front-end servlet that displays feedback requests to
	 * users.
	 * @param requestId the ID if the feedback request to answer
	 * @param response the HttpServletResponse containing the answer data
	 * @param userName the name of the user answering the request
	 */
	public void answerFeedbackRequest(String requestId, HttpServletRequest response, String userName) {
		FeedbackRequest fr = ((FeedbackRequest) this.showingRequests.remove(requestId + "." + userName));
		
		//	request has timed out before answer came, or flawed answer
		if (fr == null)
			System.out.println(userName + " answered request " + requestId + ", but after timeout");
		
		//	found request for answer
		else {
			Properties responseData = new Properties();
			
			Enumeration paramNames = response.getParameterNames();
			while (paramNames.hasMoreElements()) {
				String paramName = paramNames.nextElement().toString();
				String paramValue = response.getParameter(paramName);
				if (paramValue != null)
					responseData.setProperty(paramName, paramValue);
			}
			
			this.filterResponseData(responseData);
			
			fr.answer(responseData);
			this.returnFeedbackRequest(fr, true);
			System.out.println("Request " + requestId + " answered by user " + userName + " in " + fr.getAnswerTime(userName) + " seconds");
		}
	}
	
	/**
	 * Find a feedback request to answer for a given user. This method is
	 * intended to retrieve a feedback request from some sort of implementation
	 * specific source. Handling of timeouts, etc. is done in the
	 * getFeedbackRequest() method, which invokes this one.
	 * @param userName the name of the user
	 * @return a feedback request suited for the specified user, or null, if
	 *         there is none.
	 */
	protected abstract FeedbackRequest findFeedbackRequest(String userName);
	
	/**
	 * Take back a feedback request returning from the front end. This method
	 * hands a feedback request back to implementation specific code. The
	 * boolean argument indicates whether or not the feedback request has been
	 * answered in the front-end. This argument is false for both timeouts and
	 * actual front-end cancellations. The runtime type of the argument feedback
	 * request is equal to that of the feedback requests retuned by the
	 * findFeedbackRequest() method, since the front-end never produces feedback
	 * requests, but only handles the ones it gets from the feedback engine.
	 * @param fr the feedback request that was answered
	 * @param wasAnswered was the feedback request answered in the front-end?
	 */
	protected abstract void returnFeedbackRequest(FeedbackRequest fr, boolean wasAnswered);
	
	/**
	 * Filter out feedback engine specific parameters from a response, i.e.,
	 * remove the respective name/value pairs from the argument Properties
	 * object. This default implementation does nothing, sub classes are welcome
	 * to overwrite it as needed.
	 * @param responseData the data to filter
	 */
	protected void filterResponseData(Properties responseData) {}
	
	/**
	 * Obtain the maximum time (in seconds) a user has to submit an answer for a
	 * given feedback request before the respective dialog times out. This
	 * default implementation simply returns 120 as the number of seconds, i.e.
	 * two minutes. Sub classes wanting to provide a different timeout, maybe
	 * one specific to the given feedback request or the user, are wlecome to
	 * overwrite it as needed.
	 * @param fr the feedback request to check
	 * @param userName the user the feedback request is to be shown to
	 * @return the timeout for the specified feedback request
	 */
	protected int getTimeout(FeedbackRequest fr, String userName) {
		return 120;
	}
	
	/**
	 * React to a feedback request dialog shown to a given user timed out. This
	 * method is intended for actions specific to the feedback engine and
	 * directed at the user, eg scoring (or penalizing), all further measures
	 * are taken inside this abstract class. This default implementation does
	 * nothing, sub classes are welcome to overwrite it as needed.
	 * @param fr the feedback request that timed out
	 * @param userName the user the request timed out for
	 */
	protected void feedbackRequestTimedOut(FeedbackRequest fr, String userName) {}
	
	/**
	 * React to a feedback request dialog having been cancelled by a given user.
	 * This method is intended for actions specific to the feedback engine and
	 * directed at the user, eg scoring (or penalizing), all further measures
	 * are taken inside this abstract class. This default implementation does
	 * nothing, sub classes are welcome to overwrite it as needed.
	 * @param fr the feedback request that was cancelled
	 * @param userName the user who cancelled the request
	 */
	protected void feedbackRequestCancelled(FeedbackRequest fr, String userName) {}
	
	/**
	 * React to a feedback request dialog having been answered by a given user.
	 * This method is intended for actions specific to the feedback engine and
	 * directed at the user, eg scoring or verification, all further measures
	 * are taken inside this abstract class. This default implementation does
	 * nothing, sub classes are welcome to overwrite it as needed.
	 * @param fr the feedback request that was answered
	 * @param userName the user who answered the request
	 */
	protected void feedbackRequestAnswered(FeedbackRequest fr, String userName) {}
	
	/**
	 * Container for processing the request represented by a specific feedback
	 * panel.
	 * 
	 * @author sautter
	 */
	public static abstract class FeedbackRequest {
		
		/**
		 * the ID identifying the request inside the feedback engine
		 */
		public final String id = Gamta.getAnnotationID();
		
		/**
		 * the feedback panel representing the actual request (this field is
		 * public so sub classes of HtmlFeedbackEngine cann access it, not for
		 * public use)
		 */
		public final FeedbackPanel fp;
		
		/** the HTML renderer for feedback panel */
		public final FeedbackPanelHtmlRendererInstance fpRenderer;
		
		/**
		 * the feedback engine that created the feedback request
		 */
		protected final HtmlFeedbackEngine hostEngine;
		
		/**
		 * the set containing the names of the users who cancelled the request
		 * (not including timeouts)
		 */
		private Set cancelledBy = new TreeSet();
		
		/** the set containing the names of the users who answered the request */
		private Set answeredBy = new TreeSet();
		
		/**
		 * mapping of the names of the users who answered the request to Integer
		 * objects holding the number of seconds it took them to answer
		 */
		private Map answerTimes = new HashMap();
		
		/**
		 * the name of the user who has checked out the request (null if
		 * currently not checked out)
		 */
		private String checkoutUser = null;
		
		/**
		 * the time when the request was checked out for the last time (-1 if
		 * currently not checked out)
		 */
		private long checkoutTime = -1;
		
		/**
		 * Construtor
		 * @param fp the feedback panel representing the actual request from the
		 *            backing server
		 * @param fpRenderer the renderer instance to represent the feedback
		 *            panel in HTML
		 * @param hostEngine the feedback engine that issued the request
		 */
		protected FeedbackRequest(FeedbackPanel fp, FeedbackPanelHtmlRendererInstance fpRenderer, HtmlFeedbackEngine hostEngine) {
			this.fp = fp;
			this.fpRenderer = fpRenderer;
			this.hostEngine = hostEngine;
		}
		
		/**
		 * Retrieve the label of the enclosed feedback panel.
		 * @return the label of the enclosed feedback panel
		 */
		public String getLabel() {
			return this.fp.getLabel();
		}
		
		/**
		 * Retrieve the (preferred) size of the enclosed feedback panel.
		 * @return the (preferred) size of the enclosed feedback panel
		 */
		public Dimension getPreferredPanelSize() {
			return this.fp.getPreferredSize();
		}
		
//		/**
//		 * Write the body of the JavaScript page initializer function. This
//		 * method is needed for displaying feedback panels as HTML pages. This
//		 * method loops through to the enclosed feedback panel renderer.
//		 * @param out the Writer to write to
//		 * @throws IOException
//		 */
//		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
//			this.fpRenderer.writeJavaScriptInitFunctionBody(out);
//		}
//		
//		/**
//		 * Write further JavaScript code (variables and functions). This method
//		 * is needed for displaying feedback panels as HTML pages. This method
//		 * loops through to the enclosed feedback panel renderer.
//		 * @param out the Writer to write to
//		 * @throws IOException
//		 */
//		public void writeJavaScript(Writer out) throws IOException {
//			this.fpRenderer.writeJavaScript(out);
//		}
//		
//		/**
//		 * Write the CSS styles needed for the HTML representation of this
//		 * feedback panel. This method loops through to the enclosed feedback
//		 * panel renderer.
//		 * @param out the Writer to write to
//		 * @throws IOException
//		 */
//		public void writeCssStyles(Writer out) throws IOException {
//			this.fpRenderer.writeCssStyles(out);
//		}
//		
//		/**
//		 * Write the HTML representation of the feedback panel's content. This
//		 * method loops through to the enclosed feedback panel renderer.
//		 * @param out the Writer to write to
//		 * @throws IOException
//		 */
//		public void writePanelBody(Writer out) throws IOException {
//			this.fpRenderer.writePanelBody(out);
//		}
//		
		/**
		 * Retrieve the size of the wrapped feedback panel. This method loops
		 * through to the enclosed feedback panel renderer.
		 * @return the number of feedback decisions a user has to make for
		 *         answering the feedback panel.
		 */
		public int getDecisionCount() {
			return this.fpRenderer.getDecisionCount();
		}
		
		/**
		 * Retrieve the complexity of the wrapped feedback panel. This method
		 * loops through to the enclosed feedback panel renderer.
		 * @return the complexity of the individual feedback decisions a user
		 *         has to make for answering the feedback panel.
		 */
		public int getDecisionComplexity() {
			return this.fpRenderer.getDecisionComplexity();
		}
		
		/**
		 * Check out the feedback request for a given user. This method has to
		 * be invoked by a feedback engine prior to providing any answer data
		 * via the answer() method. If not, answer data will be ignored. It is
		 * recommended for feedback engines to do the checkout before handing
		 * out the request to a front-end servlet.
		 * @param userName the user to check the request out for
		 */
		public void checkout(String userName) {
			this.checkoutUser = userName;
			this.checkoutTime = System.currentTimeMillis();
		}
		
		private void checkin() {
			this.checkoutUser = null;
			this.checkoutTime = -1;
		}
		
		/**
		 * Answer the feedback request. This method attributes the specified
		 * answer data to the user specified in the last invocation of the
		 * checkout() method. If the latter method has not been invoked, this
		 * method returns without taking any action, resulting in the answer
		 * data going lost. If it has been invoked, this method also measures
		 * the time it took the checkout user to answer the request, or - more
		 * specifically - the time from the checkout up to the invokation of
		 * this method. Further, this method loops the answer data through to
		 * the addAnswerData() method to do the actual data handling, which may
		 * vary from implementation to implementation. Finally, it checks if
		 * isAnswered() returns true after the given round of answer data has
		 * been added to the overall answer, and if so, stores each contributing
		 * user's answering time and number of correct answers as properties of
		 * the enclosed feedback panel, and also writes the response data to the
		 * enclosed feedback panel.
		 * @param answerData the answer data
		 */
		public void answer(Properties answerData) {
			if (this.checkoutUser == null) return;
			
			String userName = this.checkoutUser;
			long checkoutTime = this.checkoutTime;
			this.checkin();
			
			this.answeredBy.add(userName);
			this.answerTimes.put(userName, new Integer(((int) ((System.currentTimeMillis() - checkoutTime + 500) / 1000))));
			this.addAnswerData(answerData, userName);
			this.hostEngine.feedbackRequestAnswered(this, userName);
			
			if (this.isAnswered()) {
				
				this.fpRenderer.readResponse(this.getAnswerData());
				
				this.fp.setProperty(FeedbackPanel.ANSWER_COUNT_PROPERTY, ("" + this.getAnswerCount()));
				
				String[] answerUsers = this.getAnsweringUsers();
				for (int u = 0; u < answerUsers.length; u++) {
					this.fp.setProperty((FeedbackPanel.CORRECT_ANSWER_COUNT_PROPERTY_PREFIX + answerUsers[u]), ("" + this.getCorrectAnswerCount(answerUsers[u])));
					this.fp.setProperty((FeedbackPanel.ANSWER_TIME_PROPERTY_PREFIX + answerUsers[u]), ("" + this.getAnswerTime(answerUsers[u])));
				}
			}
		}
		
		/**
		 * Handle the answer of a user. This method is abstract in order to
		 * allow different implementations to handle answers in their specific
		 * way, e.g. using redundancy.
		 * @param answerData the data representing the user's answer
		 * @param userName the user providing the data
		 */
		protected abstract void addAnswerData(Properties answerData, String userName);
		
		/**
		 * Test if the request is completely answered. This method is used to
		 * check whether a feedback request is completely answered after the
		 * answers of a user have been added. If so, (aggregated) answer is
		 * handed to the enclosed feedback panel.
		 * @return true if the answer is complete, false if the request (still)
		 *         has to be answered by at least one user.
		 */
		public abstract boolean isAnswered();
		
		/**
		 * Retrieve the complete, over-all answer to the feedback request. This
		 * method may (should) return null while isAnswered() returns false, but
		 * must not return null if isAnswered() returns true.
		 * @return the answer to the feedback request
		 */
		protected abstract Properties getAnswerData();
		
		/**
		 * Cancel the request for the user who has currently checked it out.
		 */
		public void cancel() {
			if (this.checkoutUser == null) return;
			
			String userName = this.checkoutUser;
			this.checkin();
			this.cancelledBy.add(userName);
			this.hostEngine.feedbackRequestCancelled(this, userName);
		}
		
		/**
		 * Time out the request for the user who has currently checked it out.
		 */
		public void timeout() {
			if (this.checkoutUser == null) return;
			
			String userName = this.checkoutUser;
			this.checkin();
			this.hostEngine.feedbackRequestTimedOut(this, userName);
		}
		
		/**
		 * Retrieve the number of individual answer parameters that make up the
		 * answer to the feedback request. This figure may be larger than the
		 * value returned by the getDecisionCount() method, in particular in
		 * cases where multiple parameters are used for the individual
		 * decisions. However, this basic implementation returns a value equal
		 * to the one returned by the getDecisionCount() method. Sub classes are
		 * welcome to overwrite it as needed.
		 * @return the number of individual answer parameters that make up the
		 *         answer of the feedback request
		 */
		public int getAnswerCount() {
			return this.getDecisionCount();
		}
		
		/**
		 * Test whether a given user has cancelled this feedback request. This
		 * is true if cancel() was invoked after the specified user was the last
		 * one to check out this feedback request.
		 * @param userName the name of the user
		 * @return true if the specified user has cancelled this feedback
		 *         request
		 */
		public boolean wasCancelledBy(String userName) {
			return ((userName != null) && this.cancelledBy.contains(userName));
		}
		
		/**
		 * Test whether a given user has answered (or contributed to answering)
		 * this feedback request. This is true if answer() was invoked after the
		 * specified user was the last one to check out this feedback request.
		 * @param userName the name of the user
		 * @return true if the specified user has answered this feedback request
		 */
		public boolean wasAnsweredBy(String userName) {
			return ((userName != null) && this.answeredBy.contains(userName));
		}
		
		/**
		 * Retrieve the number of correct answers a given user contributed to
		 * the feedback request. This basic implementation loops through to the
		 * getAnswerCount() method, thus indicating all answers given by the
		 * specified user were correct. In scenarios where multiple users
		 * contribute to answering a feedback request (eg by means of voting),
		 * however, this method might have to be overwritten in order to provide
		 * a scenario specific implementation.
		 * @param userName the user whose correct answers to retrieve
		 * @return the number of correct answers the specified user contributed
		 *         to answering the feedback request
		 */
		public int getCorrectAnswerCount(String userName) {
			return this.getAnswerCount();
		}
		
		/**
		 * Retrieve the time it took a given user to answer the feedback
		 * request. If the specified user did not answer the feedback request at
		 * all, this method returns -1.
		 * @param userName the user whose answer time to retrieve
		 * @return the time it to the specified user to answer the feedback
		 *         request
		 */
		public int getAnswerTime(String userName) {
			Integer answerTime = ((Integer) this.answerTimes.get(userName));
			return ((answerTime == null) ? -1 : answerTime.intValue());
		}
		
		/**
		 * Retrieve the names of the users who have answered the feedback
		 * request so far. This is basically the user names for which the
		 * wasAnsweredBy() method returns true.
		 * @return an array holding the names of the users who have answered the
		 *         feedback request so far
		 */
		public String[] getAnsweringUsers() {
			return ((String[]) this.answeredBy.toArray(new String[this.answeredBy.size()]));
		}
	}
}

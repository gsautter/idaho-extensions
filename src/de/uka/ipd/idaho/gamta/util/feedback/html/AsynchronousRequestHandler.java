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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackFormBuilder.SubmitMode;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;

/**
 * Utility for asynchronous handling of requests to a servlet. This class takes
 * in data from HTTP requests, handles it in background threads, provides status
 * information while the data is being processed, and also handles feedback
 * requests that might emerge while processing the data.<br>
 * Client code, first and foremost servlets, should use the path info part of
 * the URL, i.e., the part after the servlet path, to distinguish which of the
 * methods of this class to use to handle a given HTTP request, and whether or
 * not to delegate the request to this class at all. In HTTP requests looping
 * back to this class, the servlet path starts with the request ID and can
 * contain further parts. Therefore, client code should alsways chunk the path
 * info at the first forward slash (safe for the initial one), if any, and test
 * the result against the isRequestID() method. If this method returns true, the
 * HTTP request should generally be looped through to this class; there can be
 * implementation specific exceptions, however. The handleRequest() method of
 * this class implements exactly this mechanism for convenience, but client code
 * my also choose to dispatch incoming HTTP requests itself and directly
 * delegate them to the appropriate methods of this class.<br>
 * If an asynchronous request creates a feedback panel and displays it via its
 * getFeedback() method, the thread calling this latter method has to be the
 * asynchronous request thread itself.
 * 
 * @author sautter
 */
public abstract class AsynchronousRequestHandler {
	
	/**
	 * A single asynchronous request. Asynchronous requests must not be started
	 * by client code (attempting to do so results in a RuntimeException), but
	 * handed to the enqueueAsynchronousRequest() method of their parent
	 * handler.
	 * 
	 * @author sautter
	 */
	public static abstract class AsynchronousRequest extends Thread {
		
		/** The unique ID of the request */
		public final String id;
		
		/** The name of the request (for display purposes, may be null) */
		public final String name;
		
		//	connection to parent handler
		AsynchronousRequestHandler parent = null;
		String clientId = null;
		
		//	status flags keeping track of external termination
		private boolean terminated = false;
		private boolean cancelled = false;
		
		//	generic status data
		private String gStatus = "Started";
		private int gPercentFinished = 0;
		
		//	custom status data
		private String status = null;
		private int percentFinished = -1;
		
		//	error data
		private String errorMessage = null;
		private Throwable error;
		
		//	status data after finishing
		private long lastAccessTime = -1;
		private long finishTime = -1;
		private boolean finishedStatusSent = false;
		private boolean resultSent = false;
		private boolean errorLogSent = false;
		
		/**
		 * Constructor assigning a random ID to the asynchronous request,
		 * sufficient for strict in-memory handling.
		 * @param name the name of the request
		 */
		protected AsynchronousRequest(String name) {
			super(Gamta.getAnnotationID());
			this.id = this.getName();
			this.name = name;
		}
		
		/**
		 * Constructor assigning a custom ID to the asynchronous request, for
		 * cases in which the identifier is assigned extrenally (e.g. for
		 * caching request data) before the actual asynchronous request is
		 * created. The argument ID must be unique and intended for one-off use;
		 * in particular, it should not be the identifier of a persistent data
		 * object for which several asynchronous request might be generated over
		 * time or even concurrently.
		 * @param id the identifier of the request
		 * @param name the name of the request
		 */
		protected AsynchronousRequest(String id, String name) {
			super(id);
			this.id = id;
			this.name = name;
		}

		/**
		 * This implementation does the initialization, data processing,
		 * cleanup, and status updates.
		 */
		public final void run() {
			
			//	complete startup handshake
			synchronized(this) {
				this.notify();
			}
			
			//	do initialization
			try {
				this.gStatus = "Initializing";
				this.init();
			}
			catch (Throwable t) {
				t.printStackTrace(System.out);
				this.errorMessage = ("Error during initialization: " + t.getMessage());
				this.error = t;
			}
			this.gPercentFinished = 5;
			
			//	process data if initialization successful
			if (this.error == null) try {
				this.gStatus = "Running";
				this.process();
			}
			catch (Throwable t) {
				t.printStackTrace(System.out);
				this.errorMessage = ("Error during data processing: " + t.getMessage());
				this.error = t;
			}
			this.gPercentFinished = 95;
			
			//	clean up, even if some error occurred
			try {
				this.gStatus = "Finishing";
				this.cleanup();
			}
			catch (Throwable t) {
				t.printStackTrace(System.out);
				this.errorMessage = ("Error during cleanup: " + t.getMessage());
				this.error = t;
			}
			
			//	set end status
			this.gStatus = "Finished";
			this.gPercentFinished = 100;
			this.finishTime = System.currentTimeMillis();
			
			//	notify parent handler
			this.parent.notifyAsynchronousRequestFinished(this);
		}
		
		synchronized void startAr(AsynchronousRequestHandler parent) {
			this.parent = parent;
			super.start();
			try {
				this.wait();
			} catch (InterruptedException ie) {}
		}
		
		/**
		 * This implementation overwrites the original start method to lock it
		 * from client access. In particular, this implementation throws a
		 * RuntimeException. To start an asynchronous request, client code has
		 * to hand it to the enqueueAsynchronousRequest() method of its parent
		 * handler.
		 * @see java.lang.Thread#start()
		 */
		public synchronized void start() {
			throw new RuntimeException("Asynchronous requests are to be started by their handler.");
		}
		
		/**
		 * Initialize before the request is processed. This method is called
		 * from the run() method before the process() method. This default
		 * implementation does nothing, sub classes are welcome to overwrite it
		 * as needed.
		 */
		protected void init() throws Exception  {}
		
		/**
		 * Do actual data processing.
		 */
		protected abstract void process() throws Exception;
		
		/**
		 * Clean up after the request is processed. This method is called from
		 * the run() method after the process() method returns. This default
		 * implementation does nothing, sub classes are welcome to overwrite it
		 * as needed.
		 */
		protected void cleanup() throws Exception {}
		
		/**
		 * Check if the asynchronous request was terminated externally, be it at
		 * a user's request or due to system shutdown. Sub classes whose
		 * process() method runs for a long time (not counting time waiting for
		 * user feedback) should periodically check this method and return if it
		 * returns true. The cleanup() method will be called afterward anyway.
		 * @return true if the asynchronous request was terminated externally
		 */
		protected final boolean isTerminated() {
			return this.terminated;
		}
		void terminate(boolean isUserCancellation) {
			this.parent.feedbackService.answerFeedbackRequest(this.id, null);
			this.terminated = true;
			if (isUserCancellation)
				this.cancelled = true;
		}
		
		/**
		 * Retrieve the system time of the last access to this asynchronous
		 * request.
		 * @return the system time of the last access to this asynchronous
		 *         request
		 */
		public long getLastAccessTime() {
			return this.lastAccessTime;
		}
		void accessed() {
			this.lastAccessTime = System.currentTimeMillis();
		}
		
		/**
		 * Check if the asynchrounous request is finished.
		 * @return true if processing is finished
		 */
		public boolean isFinished() {
			return (this.finishTime != -1);
		}
		
		/**
		 * Check if the asynchronous request has been cancelled by a user.
		 * @return true if the asynchronous request was cancelled at a user's
		 *         request
		 */
		protected final boolean isCancelled() {
			return this.cancelled;
		}
		
		/**
		 * Retrieve textual information on the processing status of the request.
		 * Sub classes can set the status via the setStatus() method. If they do
		 * not do so, this method simply returns 'Started', 'Initializing',
		 * 'Running', 'Finishing', or 'Finished', depending on the processing
		 * status.
		 * @return the status
		 */
		public String getStatus() {
			return ((this.status == null) ? this.gStatus : this.status);
		}
		
		/**
		 * Set an implementation specific status information text. The status
		 * text may include HTML markup; if it does, it has to be enclosed in an
		 * 'HTML' tag, and special characters must be escaped appropriately.
		 * @param status the status to set
		 */
		protected void setStatus(String status) {
			this.status = status;
		}
		
		/**
		 * Retrieve an estimate of the processing percentage of the request. Sub
		 * classes can set the percentage via the setPercentFinished() method.
		 * If they do not do so, this method simply returns 0 or 100, depending
		 * on the processing status.
		 * @return the percent finished
		 */
		public int getPercentFinished() {
			return ((this.percentFinished == -1) ? this.gPercentFinished : this.percentFinished);
		}
		
		/**
		 * Set an implementation specific estimate of the percentage. If the
		 * percentage is less than 1 or larger than 100, it is ignored.
		 * @param pf the percentage to set
		 */
		protected void setPercentFinished(int pf) {
			if ((0 < pf) && (pf <= 100))
				this.percentFinished = pf;
		}
		
		/**
		 * Check whether or not the status of the asynchronous request has been
		 * sent to a client since it has finished. This is to check whether or
		 * not the client has received notification that the request is
		 * finished.
		 * @return true if the sendStatusUpdate() method has been called since
		 *         the request is finished
		 */
		public boolean isFinishedStatusSent() {
			return this.finishedStatusSent;
		}
		
		/**
		 * Update the internal status of the asynchronous request after a status
		 * update has been sent.
		 */
		protected void statusSent() {
			if (this.isFinished()) {
				this.finishedStatusSent = true;
				this.parent.notifyAsynchronousRequestStatusChanged(this);
			}
		}
		
		/**
		 * Check whether or not the result of the asynchronous request has been
		 * sent to a client since it has finished. This is to check whether or
		 * not the client has retrieved the result.
		 * @return true if the sendResult() method has been called since the
		 *         request is finished, and actually output a result.
		 */
		public boolean isResultSent() {
			return this.resultSent;
		}
		
		/**
		 * Update the internal status of the asynchronous request after the
		 * result has been sent.
		 */
		protected void resultSent() {
			if (this.isFinished()) {
				this.resultSent = true;
				this.parent.notifyAsynchronousRequestStatusChanged(this);
			}
		}
		
		/**
		 * Check whether or not the error log of the asynchronous request has
		 * been sent to a client since it has finished. This is to check whether
		 * or not the client has retrieved the error log.
		 * @return true if the sendErrorReport() method has been called since
		 *         the request is finished, and actually output an error log.
		 */
		public boolean isErrorLogSent() {
			return this.errorLogSent;
		}

		/**
		 * Update the internal status of the asynchronous request after the
		 * error log has been sent.
		 */
		protected void errorReportSent() {
			if (this.isFinished()) {
				this.errorLogSent = true;
				this.parent.notifyAsynchronousRequestStatusChanged(this);
			}
		}
		
		/**
		 * Retrieve the system time when the asynchronous request has finished.
		 * If the asynchronous request is still running, this method returns -1.
		 * @return the system time when the asynchronous request finished
		 */
		public long getFinishTime() {
			return this.finishTime;
		}

		/**
		 * Check if there was an error, in the Java sense. This default
		 * implementation checks only if some Throwable originating from either
		 * of the init(), process(), or cleanup() methods has been cought. Sub
		 * classes may overwrite this method to report on data errors as well.
		 * However, if they do, they (a) should check this implementation first
		 * and (b) overwrite the getErrorMessage(), getError(), and
		 * sendErrorReport() methods accordingly.
		 * @return true if an error occurred
		 */
		public boolean hasError() {
			return (this.error != null);
		}
		
		/**
		 * Retrieve a message string describing the error that occurred. This
		 * method returns a non-null result only if hasError() returns true.
		 * This default implementation only observes a Throwable that has been
		 * cought from either of the init(), process(), or cleanup() methods.
		 * Sub classes may overwrite this method to report on data errors as
		 * well. However, if they do, they (a) should check this implementation
		 * first and (b) overwrite the hasError(), getError(), and
		 * sendErrorReport() methods accordingly.
		 * @return the error message
		 */
		public String getErrorMessage() {
			return this.errorMessage;
		}
		
		/**
		 * Retrieve the error that occurred. This method returns a non-null
		 * result only if hasError() returns true. This default implementation
		 * covers only a Throwable cought from either of the init(), process(),
		 * or cleanup() methods. Sub classes may overwrite this method to report
		 * on data errors as well. However, if they do, they (a) should check
		 * this implementation first and (b) overwrite the hasError(),
		 * getErrorMessage(), and sendErrorReport() methods accordingly. For
		 * data errors that are only reported as textual messages, this method
		 * may return null even if hasError() returns true.
		 * @return the error
		 */
		public Throwable getError() {
			return this.error;
		}
		
		/**
		 * Retrieve the status label to show in the status display as long as
		 * the asynchronous request is running. This default implementation
		 * returns 'Your request is being processed, please wait', sub classes
		 * are welcome to overwrite it as needed.
		 * @return the status label for the running request
		 */
		public String getRunningStatusLabel() {
			return "Your request is being processed, please wait";
		}
		
		/**
		 * Retrieve the status label to show in the status display after
		 * the asynchronous request is finished. This default implementation
		 * returns 'Your request has been finished', sub classes
		 * are welcome to overwrite it as needed.
		 * @return the status label for the finished request
		 */
		public String getFinishedStatusLabel() {
			return "Your request has been finished";
		}
		
		/**
		 * Retrieve the status label to show in the status display after the
		 * asynchronous request has been cancelled at a user's request. The
		 * status label returned by this method is only ever displayed if the
		 * getCancelLinkLabel() method returns a non-null result. This default
		 * implementation returns 'Your request has been cancelled', sub classes
		 * are welcome to overwrite it as needed.
		 * @return the status label for the cancelled request
		 */
		public String getCancelledStatusLabel() {
			return "Your request has been cancelled";
		}
		
		/**
		 * Retrieve the label for the link allowing a user to cancel an
		 * asynchronous request. If this method returns null, users cannot
		 * cancel asynchronous requests. This default implementation does return
		 * null, sub classes are welcome to overwrite it as needed.
		 * @return the cancel link label
		 */
		public String getCancelLinkLabel() {
			return null;
		}
		
		/**
		 * Retrieve the confirmation dialog text for the user side cancellation
		 * of an asynchronous request. If this method returns null, the
		 * asynchronous request is cancelled without prompting the user for
		 * confirmation. This default implementation does return null. Sub
		 * classes that overwrite the getCancelLinkLabel() method to return a
		 * non-null result and thus enable user side cancellation of
		 * asynchronous requests are recommended to overwrite this method to
		 * return a non-null result as well in order to avoid accidental
		 * cancellation of asynchronous requests.
		 * @return the text for the termination confirm dialog
		 */
		public String getCancellationConfirmDialogText() {
			return null;
		}
		
		/**
		 * Retrieve the label for the link taking a user away from the status
		 * display after cancelling an asynchronous request. If this method
		 * returns null, users are immediately forwarded to the URL returned by
		 * the getCancelledLink() method after cancelling an asynchronous
		 * request. This default implementation does return null, sub classes
		 * are welcome to overwrite it as needed.
		 * @return the cancelled link label
		 */
		public String getCancelledLinkLabel() {
			return null;
		}
		
		/**
		 * Retrieve the URL for the link leading away from the status display
		 * after a user side cancellation of an asynchronous request. If this
		 * method returns null, users cannot cancel asynchronous requests. This
		 * default implementation does return null, sub classes are welcome to
		 * overwrite it as needed.
		 * @param request an HTTP request to retrieve parts of the link path
		 *            from
		 * @return the cancelled link
		 */
		public String getCancelledLink(HttpServletRequest request) {
			return null;
		}
		
		/**
		 * Retrieve the label for the link opening a feedback request. This
		 * default implementation returns 'Open Feedback Request', sub classes
		 * are welcome to overwrite it as needed.
		 * @return the feedback link label
		 */
		public String getFeedbackLinkLabel() {
			return "Open Feedback Request";
		}
		
		/**
		 * Retrieve the label for the link opening the error log of an
		 * asynchronous request. This default implementation returns 'View Error
		 * Log', sub classes are welcome to overwrite it as needed.
		 * @return the error log link label
		 */
		public String getErrorLogLinkLabel() {
			return "View Error Log";
		}
		
		/**
		 * Retrieve the label for the link opening the result of an asynchronous
		 * request. This default implementation returns 'View Result', sub
		 * classes are welcome to overwrite it as needed.
		 * @return the result link label
		 */
		public String getResultLinkLabel() {
			return "View Result";
		}
		
		/**
		 * Retrieve the URL for the link to the result of an asynchronous
		 * request. If this method returns null, the default callback link is
		 * used. This default implementation does return null, sub classes are
		 * welcome to overwrite it as needed.
		 * @param request an HTTP request to retrieve parts of the link path
		 *            from
		 * @return the result link
		 */
		public String getResultLink(HttpServletRequest request) {
			return null;
		}
		
		/**
		 * Check whether or not to immediately forward the status display page
		 * of an asynchronous request to the result page after processing is
		 * finished. If this method returns true, the link to the error log is
		 * never displayed. However, the immediate forwarding only works if the
		 * getResultLink() method does not return null. This default
		 * implementation returns false, sub classes are welcome to overwrite it
		 * as needed.
		 * @return true for immediate result forwarding
		 */
		public boolean doImmediateResultForward() {
			return false;
		}
		
		/**
		 * Write a status update. In HTML mode, this method outputs HTML code
		 * that updates the displayed request processing status. This page is
		 * for display in the invisible IFRAME created by the
		 * writeStatusDisplay() and writeStatusDisplayPage() methods, not for
		 * actual display. It contains nothing but a form containing a few
		 * hidden elements for the status display code to read and update itself
		 * from. In XML mode, it sends status information encoded in XML.
		 * @param request the HTTP request in whose response to include the
		 *            status display
		 * @param response the HTTP resonse to write to
		 * @throws IOException
		 */
		public void sendStatusUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
			
			//	remember sending status (before actually sending it, so it can be retrieved again if the request finishes while sending the status)
			this.statusSent();
			
			//	write XML status
			if (this.parent.inXmlMode) {
				
				//	prepare output
				response.setContentType("text/xml");
				response.setCharacterEncoding("UTF-8");
				Writer out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
				
				//	send data
				this.writeStatusXML(request, out);
				out.flush();
			}
			
			//	HTML update
			else {
				
				//	prepare output
				response.setContentType("text/html");
				response.setCharacterEncoding("UTF-8");
				Writer out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
				BufferedLineWriter bw = new BufferedLineWriter(out);
				
				//	write HTML status form
				bw.writeLine("<html><body>");
				bw.writeLine("<form action=\"none\" method=\"GET\">");
				bw.writeLine("<input name=\"time\" type=\"hidden\" id=\"time\" value=\"" + System.currentTimeMillis() + "\">");
				bw.writeLine("<input name=\"status\" type=\"hidden\" id=\"status\" value=\"" + AnnotationUtils.escapeForXml(this.getStatus(), true) + "\">");
				bw.writeLine("<input name=\"percent\" type=\"hidden\" id=\"percent\" value=\"" + this.getPercentFinished() + "\">");
				ArFeedbackRequest fr = this.parent.feedbackService.getFeedbackRequest(this.id);
				if (fr != null)
					bw.writeLine("<input name=\"feedback\" type=\"hidden\" id=\"feedback\" value=\"" + fr.hashCode() + "\">");
				if (this.isFinished()) {
					bw.writeLine("<input name=\"finished\" type=\"hidden\" id=\"finished\" value=\"true\">");
					String cancelledLink = this.getCancelledLink(request);
					String resultLink = this.getResultLink(request);
					if (this.isCancelled() && (this.getCancelledLinkLabel() == null))
						bw.writeLine("<input name=\"forward\" type=\"hidden\" id=\"forward\" value=\"" + cancelledLink + "\">");
					else if ((resultLink != null) && this.doImmediateResultForward())
						bw.writeLine("<input name=\"forward\" type=\"hidden\" id=\"forward\" value=\"" + resultLink + "\">");
					else if (this.hasError())
						bw.writeLine("<input name=\"errors\" type=\"hidden\" id=\"errors\" value=\"true\">");
				}
				bw.writeLine("</form>");
				bw.writeLine("</body></html>");
				
				bw.flush();
				out.flush();
			}
		}
		
		/**
		 * Write the status information encoded in XML.
		 * @param request the HTTP request in whose response to include the
		 *            status display
		 * @param out the writer to write to
		 * @throws IOException
		 */
		public void writeStatusXML(HttpServletRequest request, Writer out) throws IOException {
			BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			//	write XML status message
			bw.writeLine("<status id=\"" + this.id + "\"" + ((this.name == null) ? "" : (" name=\"" + AnnotationUtils.escapeForXml(this.name, true) + "\"")) + " state=\"" + AnnotationUtils.escapeForXml(this.gStatus, true) + "\" stateDetail=\"" + AnnotationUtils.escapeForXml(this.getStatus(), true) + "\" percentFinished=\"" + this.getPercentFinished() + "\">");
			bw.writeLine("<callback type=\"status\">" + request.getContextPath() + request.getServletPath() + "/" + this.id + "/" + STATUS_UPDATE_ACTION + "</callback>");
			if (this.parent.feedbackService.hasFeedbackRequest(this.id)) {
				bw.writeLine("<callback type=\"showFeedbackPage\">" + request.getContextPath() + request.getServletPath() + "/" + this.id + "/" + GET_FEEDBACK_REQUEST_PAGE_ACTION + "</callback>");
				bw.writeLine("<callback type=\"getFeedbackBlock\">" + request.getContextPath() + request.getServletPath() + "/" + this.id + "/" + GET_FEEDBACK_REQUEST_BLOCK_ACTION + "</callback>");
				bw.writeLine("<callback type=\"cancelFeedback\">" + request.getContextPath() + request.getServletPath() + "/" + this.id + "/" + CANCEL_FEEDBACK_REQUEST_ACTION + "</callback>");
			}
			if (this.isFinished()) {
				bw.writeLine("<callback type=\"result\">" + request.getContextPath() + request.getServletPath() + "/" + this.id + "/" + RESULT_ACTION + "</callback>");
				if (this.hasError())
					bw.writeLine("<callback type=\"error\">" + request.getContextPath() + request.getServletPath() + "/" + this.id + "/" + ERRORS_ACTION + "</callback>");
			}
			bw.writeLine("</status>");
			
			//	send data
			bw.flush();
		}
		
		/**
		 * Send the result of the asynchronous feedback request. This method
		 * takes the HTTP request and response as arguments so sub classes can
		 * read request parameters and set the response content type. The
		 * returned boolean indicates to a surrounding servlet whether or not
		 * the argument HTTP request has been answered. This default
		 * implementation simply returns false, sub classes are welcome to
		 * overwrite it as needed. Sub classes that do overwrite this method
		 * should remember sending the result to support cleanup decisions. To
		 * achieve this, they should call the resultSent() method right before
		 * their implementation of this method returns true.
		 * @param request the HTTP request
		 * @param response the HTTP resonse to write to
		 * @return true if the result was written, false otherwise
		 * @throws IOException
		 */
		public boolean sendResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
			return false;
		}
		
		/**
		 * Send an error report. This method takes the HTTP request and response
		 * as arguments so sub classes can read request parameters and set the
		 * response content type. The returned boolean indicates to a
		 * surrounding servlet whether or not the HTTP request has been
		 * answered. This default implementation only writes an error message
		 * and a stack trace from a Throwable caught from either of the init(),
		 * process(), or cleanup() methods. Sub classes may overwrite this
		 * method to report on data errors as well. However, if they do, they
		 * (a) should check this implementation first and (b) overwrite the
		 * hasError(), getErrorMessage(), and getError() methods accordingly.
		 * Sub classes that do overwrite this method should remember sending the
		 * error log to support cleanup decisions. To achieve this, they should
		 * call the errorReportSent() method right before their implementation
		 * of this method returns true.
		 * @param request the HTTP request
		 * @param response the HTTP resonse to write to
		 * @return true if an error report was written, false otherwise
		 * @throws IOException
		 */
		public boolean sendErrorReport(HttpServletRequest request, HttpServletResponse response) throws IOException {
			if (this.error != null) {
				response.setContentType("text/plain");
				response.setCharacterEncoding("UTF-8");
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
				bw.write(this.errorMessage);
				bw.newLine();
				bw.newLine();
				this.error.printStackTrace(new PrintWriter(bw));
				bw.flush();
				this.errorReportSent();
				return true;
			}
			else return false;
		}
	}
	
	/**
	 * Container class representing a feedback request pending for an
	 * asynchronous request, together with an appropriate HTML renderer.
	 * 
	 * @author sautter
	 */
	protected static class ArFeedbackRequest {
		
		/** the ID of the asynchronous request the feedback request belongs to */
		public final String arId;
		
		/** the feedback panel representing the feedback request */
		public final FeedbackPanel fp;
		
		/** the HTML renderer to use for rendering the feedback panel as an HTML form */
		public final FeedbackPanelHtmlRendererInstance fpRenderer;
		
		boolean answered = false;
		ArFeedbackRequest(String arId, FeedbackPanel fp, FeedbackPanelHtmlRendererInstance fpRenderer) {
			this.arId = arId;
			this.fp = fp;
			this.fpRenderer = fpRenderer;
		}
		
		/**
		 * Answer the feedback request with the parameters provided by the
		 * argument HTTP request. Calling this method with a null argument
		 * cancels the feedback request.
		 * @param request the HTTP request containing the answer parameters
		 */
		public synchronized void answer(HttpServletRequest request) {
			if (this.answered)
				return;
			this.answered = true;
			
			//	cancelled
			if (request == null) {
				this.fp.setStatusCode("Cancel");
				this.notify();
				return;
			}
			
			//	answered, extract data from HTTP request ...
			Properties responseData = new Properties();
			Enumeration paramNames = request.getParameterNames();
			while (paramNames.hasMoreElements()) {
				String paramName = paramNames.nextElement().toString();
				String paramValue = request.getParameter(paramName);
				if (paramValue != null)
					responseData.setProperty(paramName, paramValue);
			}
			
			//	... and inject it into feedback request
			this.fp.setStatusCode(responseData.getProperty(FeedbackFormBuilder.SUBMIT_MODE_PARAMETER, "OK"));
			this.fpRenderer.readResponse(responseData);
			this.notify();
		}
		
		/**
		 * Check if the feedback request has been answered (or cancelled), i.e.,
		 * if the answer() method has beed called at least once.
		 * @return true if the feedback request has been answered
		 */
		public boolean isAnswered() {
			return this.answered;
		}
	}
	
	private class ArhFeedbackService implements FeedbackService {
		private HashMap feedbackRequestsByID = new HashMap();
		public int getPriority() {
			return 10; // we whan these requests ...
		}
		public boolean canGetFeedback(FeedbackPanel fp) {
			System.out.println("ARH Feedback Service (" + AsynchronousRequestHandler.this.getClass().getName() + "): Asked for feedback by thread " + Thread.currentThread().getName());
			if (runningRequestsById.containsKey(Thread.currentThread().getName())) {
				System.out.println(" ==> got it");
				return true;
			}
			else {
				System.out.println(" ==> none of our business");
				return false;
			}
		}
		public void getFeedback(FeedbackPanel fp) {
			AsynchronousRequest ar = getAsynchronousRequest(Thread.currentThread().getName());
			if (!runningRequestsById.containsKey(ar.id))
				throw new RuntimeException("Cannot process foreign feedback request.");
			
			FeedbackPanelHtmlRendererInstance fpRenderer = FeedbackPanelHtmlRenderer.getRenderer(fp);
			if (fpRenderer == null)
				throw new RuntimeException("Cannot render feedback request as HTML.");
			
			ArFeedbackRequest fr = new ArFeedbackRequest(ar.id, fp, fpRenderer);
			this.feedbackRequestsByID.put(fr.arId, fr);
			
			feedbackAwaitingRequestIDs.add(ar.id);
			asynchronousRequestRequestedFeedback(ar);
			
			synchronized (fr) {
				try {
					fr.wait(feedbackTimeout * 1000);
					if (!fr.isAnswered())
						throw new RuntimeException("Feedback request timed out.");
				}
				catch (InterruptedException ie) {
					throw new RuntimeException("Feedback request cancelled due to shutdown.");
				}
				finally {
					feedbackAwaitingRequestIDs.remove(ar.id);
				}
			}
			
			asynchronousRequestGotFeedback(ar);
		}
		public boolean isLocal() {
			return false;
		}
		public boolean isMultiFeedbackSupported() {
			return false;
		}
		public void getMultiFeedback(FeedbackPanel[] fps) throws UnsupportedOperationException {
			throw new UnsupportedOperationException("Multi-feedback not supported.");
		}
		public void shutdown() {
			ArrayList frIds = new ArrayList(this.feedbackRequestsByID.keySet());
			for (int r = 0; r < frIds.size(); r++) {
				ArFeedbackRequest fr = ((ArFeedbackRequest) this.feedbackRequestsByID.remove(frIds.get(r)));
				if (fr != null)
					fr.answer(null);
			}
		}
		boolean hasFeedbackRequest(String arId) {
			return this.feedbackRequestsByID.containsKey(arId);
		}
		ArFeedbackRequest getFeedbackRequest(String arId) {
			return ((ArFeedbackRequest) this.feedbackRequestsByID.get(arId));
		}
		void answerFeedbackRequest(String arId, HttpServletRequest request) {
			ArFeedbackRequest fr = ((ArFeedbackRequest) this.feedbackRequestsByID.remove(arId));
			if (fr != null) {
				this.feedbackRequestsByID.remove(fr.arId);
				fr.answer(request);
			}
		}
	}
	
	private Map runningRequestsById = Collections.synchronizedMap(new HashMap());
	private Set feedbackAwaitingRequestIDs = Collections.synchronizedSet(new HashSet());
	private Map finishedRequestsById = Collections.synchronizedMap(new LinkedHashMap(16, 0.8f, true) {
		protected boolean removeEldestEntry(Entry e) {
			final AsynchronousRequest ar = ((AsynchronousRequest) e.getValue());
			if (retainAsynchronousRequest(ar, this.size()))
				return false;
			else {
				asynchronousRequestDiscarded(ar);
				return true;
			}
		}
	});
	private Map requestsIDsByClientId = Collections.synchronizedMap(new HashMap());
	
	ArhFeedbackService feedbackService;
	
	private int feedbackTimeout = 0;
	
	final boolean inXmlMode;
	
	/**
	 * Constructor
	 * @param inXmlMode use simple XML for status output, so client code can
	 *            take care of displaying data?
	 */
	protected AsynchronousRequestHandler(boolean inXmlMode) {
		this.inXmlMode = inXmlMode;
		this.feedbackService = new ArhFeedbackService();
		FeedbackPanel.addFeedbackService(this.feedbackService);
	}
	
	/**
	 * Retrieve the timeout (in seconds) for feedback requests issued by the
	 * process() method of asynchronous requests.
	 * @return the feedback timeout
	 */
	public int getFeedbackTimeout() {
		return this.feedbackTimeout;
	}
	
	/**
	 * Set the timeout (in seconds) for feedback requests issued by the
	 * process() method of asynchronous requests. If the feedback request has
	 * not been answered before the timeout expires, a RuntimeException is
	 * thrown into the process() method. Setting the timeout to 0 (or any
	 * negative number) deactivates the timeout mechanism, causing asynchronous
	 * requests to wait for an answer until it arrives. 0 is the default
	 * setting, so the timeout is deactivated by default.
	 * @param ft the feedback timeout to set
	 */
	public void setFeedbackTimeout(int ft) {
		this.feedbackTimeout = ((ft < 0) ? 0 : ft);
	}
	
	/**
	 * Retrieve the number of asynchronous requests currently running, including
	 * the ones awaiting external feedback.
	 * @return the number of running asynchronous requests
	 */
	public int getRunningRequestCount() {
		return this.runningRequestsById.size();
	}
	
	/**
	 * Retrieve the number of asynchronous requests currently awaiting external
	 * feedback.
	 * @return the number of asynchronous requests awaiting external feedback
	 */
	public int getFeedbackAwaitingRequestCount() {
		return this.feedbackAwaitingRequestIDs.size();
	}
	
	/**
	 * Retrieve the total number of asynchronous requests currently held in the
	 * internal registers of the asynchronous request handler, including
	 * finished ones.
	 * @return the number of asynchronous requests
	 */
	public int getRequestsCount() {
		return (this.runningRequestsById.size() + this.finishedRequestsById.size());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		this.shutdown();
	}
	
	/**
	 * Shut down the asynchronous request handler, terminating all pending
	 * asynchronous requests. This method is to be called by a surrounding
	 * servlet on shutdown, best from its destroy() method.
	 */
	public synchronized void shutdown() {
		if (this.feedbackService == null)
			return;
		
		//	terminate all running requests
		for (Iterator arIt = this.runningRequestsById.keySet().iterator(); arIt.hasNext();) {
			String arId = ((String) arIt.next());
			AsynchronousRequest ar = this.getAsynchronousRequest(arId);
			if (ar != null)
				ar.terminate(false);
		}
		this.finishedRequestsById.clear();
		this.requestsIDsByClientId.clear();
		
		//	shut down feedback service
		this.feedbackService.shutdown();
		FeedbackPanel.removeFeedbackService(this.feedbackService);
		this.feedbackService = null;
	}
	
	/**
	 * Test whether ot not a given string is the ID of a running asynchronous
	 * request, or of a finished one still in cache.
	 * @param str the string to test
	 * @return true if the argument string is a request ID, false otherwise
	 */
	public boolean isRequestID(String str) {
		return (this.runningRequestsById.containsKey(str) || this.finishedRequestsById.containsKey(str));
	}
	
	/**
	 * Test whether ot not a given string is the ID of a running asynchronous
	 * request.
	 * @param str the string to test
	 * @return true if the argument string is a request ID, false otherwise
	 */
	public boolean isRunningRequestID(String str) {
		return this.runningRequestsById.containsKey(str);
	}
	
	/**
	 * Set up an HTTP request for asynchronous handling. If the implementation
	 * cannot handle the request asynchronously, this method returns null.
	 * @param request the HTTP request
	 * @param clientId generic identifier of the request owner, e.g. a user name
	 *            or an HTTP session ID
	 * @return the ID of the asynchronous request
	 * @throws IOException
	 */
	public String createRequest(HttpServletRequest request, String clientId) throws IOException {
		
		//	let sub class build asynchronous request
		AsynchronousRequest ar = this.buildAsynchronousRequest(request);
		
		//	could not do it
		if (ar == null)
			return null;
		
		//	enqueue asynchronous request
		this.enqueueRequest(ar, clientId);
		
		//	finally ...
		return ar.id;
	}
	
	/**
	 * Enqueue and start an asynchronous request. This method allows to
	 * construct asynchronous requests in client code, facilitating more fine
	 * grained data handling than the generic handleRequest() and
	 * buildAsynchronousRequest() methods. The argument asynchronous request
	 * must not be started by client code.
	 * @param ar the asynchronous request
	 * @param clientId generic identifier of the request owner, e.g. a user name
	 *            or an HTTP session ID
	 */
	public synchronized void enqueueRequest(AsynchronousRequest ar, String clientId) {
		
		//	index for later access
		this.runningRequestsById.put(ar.id, ar);
		if (clientId != null) {
			HashSet cidRequestIDs = ((HashSet) this.requestsIDsByClientId.get(clientId));
			if (cidRequestIDs == null) {
				cidRequestIDs = new LinkedHashSet(2);
				this.requestsIDsByClientId.put(clientId, cidRequestIDs);
			}
			cidRequestIDs.add(ar.id);
			ar.clientId = clientId;
		}
		
		//	start asynchronous request
		ar.startAr(this);
	}
	
	/**
	 * Create an asynchronous request from an HTTP request. If this method
	 * cannot create an asynchronous request, it must not consume the request in
	 * any way. For any HTTP method but GET, this means implementations should
	 * decide exclusively based on the request path and headers. Should this
	 * method start reading the request body, yet fail to create an asynchronous
	 * request, it should throw an exception rather than returning null. At
	 * least, surrounding servlets must be aware what their implementation of
	 * this method does.
	 * @param request the HTTP request
	 * @return the asynchronous request
	 * @throws IOException
	 */
	public abstract AsynchronousRequest buildAsynchronousRequest(HttpServletRequest request) throws IOException;
	
	synchronized void notifyAsynchronousRequestFinished(AsynchronousRequest ar) {
		this.runningRequestsById.remove(ar.id);
		boolean discard = ((ar.isTerminated() && !ar.isCancelled()) || !this.retainAsynchronousRequest(ar, this.finishedRequestsById.size()));
		if (discard) {
			HashSet cidRequestIDs = ((HashSet) this.requestsIDsByClientId.get(ar.clientId));
			if (cidRequestIDs != null)
				cidRequestIDs.remove(ar.id);
		}
		else this.finishedRequestsById.put(ar.id, ar);
		
		this.asynchronousRequestFinished(ar, !discard);
		if (discard)
			this.asynchronousRequestDiscarded(ar);
	}
	
	synchronized void notifyAsynchronousRequestStatusChanged(AsynchronousRequest ar) {
		if (this.retainAsynchronousRequest(ar, this.finishedRequestsById.size()))
			return;
		this.finishedRequestsById.remove(ar.id);
		HashSet cidRequestIDs = ((HashSet) this.requestsIDsByClientId.get(ar.clientId));
		if (cidRequestIDs != null)
			cidRequestIDs.remove(ar.id);
		this.asynchronousRequestDiscarded(ar);
	}
	
	/**
	 * React on an asynchronous request issuing a request for external feedback.
	 * The runtime type of the argument asynchronous request ist the one of the
	 * asynchronous requests created by the buildAsynchronousRequest() method,
	 * or created by client code and handed to the enqueueAsynchrounousRequest()
	 * method, respectively. This method allows sub classes to take further
	 * action. This default implementation does nothing, sub classes are welcome
	 * to overwrite it as needed. As calls to this method might come from code
	 * synchronized on the handler itself, sub classes engaging in
	 * time-consuming actions should delegate theses actions to some separate
	 * thread in order not to block the asynchronous request handler.
	 * @param ar the asynchronous request issuing a feedback request
	 */
	public void asynchronousRequestRequestedFeedback(AsynchronousRequest ar) {}
	
	/**
	 * React on an asynchronous request receiving external feedback. The runtime
	 * type of the argument asynchronous request ist the one of the asynchronous
	 * requests created by the buildAsynchronousRequest() method, or created by
	 * client code and handed to the enqueueAsynchrounousRequest() method,
	 * respectively. This method allows sub classes to take further action. This
	 * default implementation does nothing, sub classes are welcome to overwrite
	 * it as needed. As calls to this method might come from code synchronized
	 * on the handler itself, sub classes engaging in time-consuming actions
	 * should delegate theses actions to some separate thread in order not to
	 * block the asynchronous request handler.
	 * @param ar the asynchronous request that got feedback
	 */
	public void asynchronousRequestGotFeedback(AsynchronousRequest ar) {}
	
	/**
	 * React on an asynchronous request finishing. This method is called after
	 * an asynchronous request finishes and the internal registers are updated,
	 * possibly calling the retainAsynchronousRequest() method. The runtime type
	 * of the argument asynchronous request ist the one of the asynchronous
	 * requests created by the buildAsynchronousRequest() method, or created by
	 * client code and handed to the enqueueAsynchrounousRequest() method,
	 * respectively. This method allows sub classes to take further action. This
	 * default implementation does nothing, sub classes are welcome to overwrite
	 * it as needed. If the willBeRetained flag is false, the
	 * asynchronousRequestDiscarded() method will be called right afterward. As
	 * calls to this method come from code synchronized on the handler itself,
	 * sub classes engaging in time-consuming actions should delegate theses
	 * actions to some separate thread in order not to block the asynchronous
	 * request handler.
	 * @param ar the asynchronous request that just finished
	 * @param willBeRetained will the argument asynchronous request be kept in
	 *            the internal registers any further?
	 */
	public void asynchronousRequestFinished(AsynchronousRequest ar, boolean willBeRetained) {}
	
	/**
	 * React on an asynchronous request being removed from the internal
	 * registers. This method is called after an asynchronous request finishes
	 * and the internal registers are updated, possibly calling the
	 * asynchronousRequestFinished() method right beforehand. The runtime type
	 * of the argument asynchronous request ist the one of the asynchronous
	 * requests created by the buildAsynchronousRequest() method, or created by
	 * client code and handed to the enqueueAsynchrounousRequest() method,
	 * respectively. This method allows sub classes to take further action. This
	 * default implementation does nothing, sub classes are welcome to overwrite
	 * it as needed. As calls to this method come from code synchronized on the
	 * handler itself, sub classes engaging in time-consuming actions should
	 * delegate theses actions to some separate thread in order not to block the
	 * asynchronous request handler.
	 * @param ar the asynchronous request being removed from the internal
	 *            registers
	 */
	public void asynchronousRequestDiscarded(AsynchronousRequest ar) {}
	
	/**
	 * Decide whether or not to further retain an asynchronous request that has
	 * finished. If this method returns true, the argument asynchronous request
	 * will be retained further, if this method returns false, it is disposed of
	 * to free up resources. Implementations of this method are responsible to
	 * decide on how long finished asnychronous requests are retained before the
	 * resources they occupy are released. The runtime class of the argument
	 * asynchronous request is the same as that of the ones handed to the
	 * enqueueRequest() method.
	 * @param ar the asynchronous request to decide on
	 * @param finishedArCount the number of finished asynchronous requests
	 *            currently being held
	 * @return true if the asynchronous request should be retained further
	 */
	protected abstract boolean retainAsynchronousRequest(AsynchronousRequest ar, int finishedArCount);
	
	/**
	 * Retrieve an asynchronous request. This method does not exist to make
	 * asynchronous requests available for external manipulation. It is rather
	 * intended to grant sub classes access to asynchronous requests so they can
	 * customize the HTML generation methods.
	 * @param arId the ID of the asynchronous request
	 * @return the asynchronous request with the specified ID
	 */
	protected synchronized AsynchronousRequest getAsynchronousRequest(String arId) {
		AsynchronousRequest ar = ((AsynchronousRequest) this.runningRequestsById.get(arId));
		if (ar == null)
			ar = ((AsynchronousRequest) this.finishedRequestsById.get(arId));
		if (ar != null)
			ar.accessed();
		return ar;
	}
	
	/**
	 * Retrieve the feedback request currently pendings for an asynchronous
	 * request, if any. This method exists to grant sub classes access to
	 * feedback requests so they can customize the way they are handled.
	 * @param arId the ID of the asynchronous request
	 * @return the feedback request currently pending for the asynchronous
	 *         request with the specified ID
	 */
	protected ArFeedbackRequest getFeedbackRequest(String arId) {
		return this.feedbackService.getFeedbackRequest(arId);
	}
	
	/**
	 * Retrieve a list of the IDs of all asynchronous requests that belong to a
	 * given client. If there are no asynchronous requests for the argument
	 * client ID, this method returns an empty array, but never null.
	 * @param clientId the ID of the client.
	 * @return the IDs of the asynchronous requests belonging to the specified
	 *         client ID.
	 */
	public String[] getRequestIDs(String clientId) {
		if (clientId == null)
			return new String[0];
		HashSet cidRequestIDs = ((HashSet) this.requestsIDsByClientId.get(clientId));
		return ((cidRequestIDs == null) ? new String[0] : ((String[]) cidRequestIDs.toArray(new String[cidRequestIDs.size()])));
	}
	
	/**
	 * Write HTML code that displays the request status, wrapped by clinet code
	 * in some DIV or TD element, for instance. This default implementation uses
	 * an invisible IFRAME for updates.
	 * @param out the writer to write to
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @throws IOException
	 */
	public void writeStatusDisplay(Writer out, HttpServletRequest request, String requestId) throws IOException {
		if (requestId == null)
			requestId = this.getRequestID(request);
		
		//	wrap writer if necessary
		BufferedLineWriter bw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
		
		//	write scripts and styles first
		this.writeStatusDisplayStylesAndScripts(bw, request, requestId);
		
		//	write HTML code
		this.writeStatusDisplayHtml(bw, request, requestId);
		
		//	write starter scrip
		bw.writeLine("<script type=\"text/javascript\">");
		bw.writeLine("updateStatus();");
		bw.writeLine("</script>");
		
		//	flush buffer
		bw.flush();
	}
	
	/**
	 * Write an HTML page that displays the request status, for display inside
	 * an IFRAME element. This default implementation uses an invisible IFRAME
	 * for updates.
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @param response the HTTP resonse to write to
	 * @throws IOException
	 */
	public void sendStatusDisplayFrame(HttpServletRequest request, String requestId, HttpServletResponse response) throws IOException {
		if (requestId == null)
			requestId = this.getRequestID(request);
		final String arId = requestId;
		
		//	send IFRAME HTML page including status display
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		HtmlPageBuilder hpb = new HtmlPageBuilder(this.getPageBuilderHost(), request, response) {
			protected String[] getOnloadCalls() {
				String[] olcs = {"updateStatus();"};
				return olcs;
			}
			protected void include(String type, String tag) throws IOException {
				if ("includeBody".equals(type)) {
					BufferedLineWriter bw = new BufferedLineWriter(this.asWriter());
					AsynchronousRequestHandler.this.writeStatusDisplayHtml(bw, this.request, arId);
				}
				else super.include(type, tag);
			}
			protected void writePageHeadExtensions() throws IOException {
				BufferedLineWriter bw = new BufferedLineWriter(this.asWriter());
				AsynchronousRequestHandler.this.writeStatusDisplayStylesAndScripts(bw, this.request, arId);
			}
		};
		this.sendStatusDisplayIFramePage(hpb);
	}
	
	/**
	 * Write an HTML page that displays the request status, for display inside a
	 * dedicated status window. This default implementation uses an invisible
	 * IFRAME for updates.
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @param response the HTTP resonse to write to
	 * @throws IOException
	 */
	public void sendStatusDisplayPage(HttpServletRequest request, String requestId, HttpServletResponse response) throws IOException {
		if (requestId == null)
			requestId = this.getRequestID(request);
		final String arId = requestId;
		final AsynchronousRequest ar = this.getAsynchronousRequest(arId);
		
		//	send popup HTML page including status display
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		HtmlPageBuilder hpb = new HtmlPageBuilder(this.getPageBuilderHost(), request, response) {
			protected String[] getOnloadCalls() {
				String[] olcs = {"updateStatus();"};
				return olcs;
			}
			protected void include(String type, String tag) throws IOException {
				if ("includeBody".equals(type)) {
					BufferedLineWriter bw = new BufferedLineWriter(this.asWriter());
					AsynchronousRequestHandler.this.writeStatusDisplayHtml(bw, this.request, arId);
				}
				else super.include(type, tag);
			}
			protected void writePageHeadExtensions() throws IOException {
				BufferedLineWriter bw = new BufferedLineWriter(this.asWriter());
				AsynchronousRequestHandler.this.writeStatusDisplayStylesAndScripts(bw, this.request, arId);
			}
			protected String getPageTitle(String title) {
				return (((ar == null) || (ar.name == null)) ? super.getPageTitle(title) : ar.name);
			}
		};
		this.sendStatusDisplayPopupPage(hpb);
	}
	
	/**
	 * Write the actual HTML code that displays the request status, wrapped by
	 * clinet code in some DIV or TD element, for instance. This default
	 * implementation uses an invisible IFRAME for updates. Sub classes may
	 * overwrite this method to display the request status differently. However,
	 * they than have to make sure that the HTML code written by their version
	 * of this method works with the JavaScript code written by the
	 * writeStatusDisplayStylesAndScripts() method. It is best to always
	 * overwrite both.
	 * @param out the writer to write to
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @throws IOException
	 */
	protected void writeStatusDisplayHtml(BufferedLineWriter out, HttpServletRequest request, String requestId) throws IOException {
		if (requestId == null)
			requestId = this.getRequestID(request);
		
		//	get asynchronous request
		AsynchronousRequest ar = this.getAsynchronousRequest(requestId);
		if (ar == null) {
			out.writeLine("<p id=\"statusMessage\">No request whose status to display.</p>");
			out.flush();
			return;
		}
		
		/*
		 * write update fetcher IFRAME (have to set actual status URL only in
		 * JavaScript in case request is already finished when page is loaded,
		 * so JavaScript gets 'finished' message even if request is discarded
		 * immediately after first notifying client of completion)
		 */
		out.write("<iframe id=\"statusUpdateFrame\" height=\"0px\" style=\"border-width: 0px;\" src=\"about:blank\">");
		out.writeLine("</iframe>");
		
		//	gather data
		String resultLink = ar.getResultLink(request);
		if (resultLink == null)
			resultLink = (request.getContextPath() + request.getServletPath() + "/" + requestId + "/" + RESULT_ACTION);
		String cancelLinkLabel = ar.getCancelLinkLabel();
		String cancelledLink = ar.getCancelledLink(request);
		String cancelledLinkLabel = ar.getCancelledLinkLabel();
		if ((cancelLinkLabel == null) || (cancelledLink == null)) {
			cancelLinkLabel = null;
			cancelledLink = null;
			cancelledLinkLabel = null;
		}
		
		//	write progress bar
		out.write("<p>");
		out.write("<span id=\"statusLabelRunning\" class=\"statusLabel\">" + ar.getRunningStatusLabel() + "</span>");
		out.write("<span id=\"statusLabelFinished\" class=\"statusLabel\" style=\"display: none;\">" + ar.getFinishedStatusLabel() + "</span>");
		if (cancelLinkLabel != null)
			out.write("<span id=\"statusLabelCancelled\" class=\"statusLabel\" style=\"display: none;\">" + ar.getCancelledStatusLabel() + "</span>");
		out.write("&nbsp;<span id=\"statusDisplayDot0\" class=\"statusDisplayDot\">*</span>");
		out.write("&nbsp;<span id=\"statusDisplayDot1\" class=\"statusDisplayDot\">*</span>");
		out.write("&nbsp;<span id=\"statusDisplayDot2\" class=\"statusDisplayDot\">*</span>");
		out.write("&nbsp;<span id=\"statusDisplayDot3\" class=\"statusDisplayDot\">*</span>");
		out.write("&nbsp;<span id=\"statusDisplayDot4\" class=\"statusDisplayDot\">*</span>");
		out.write("&nbsp;<span id=\"statusDisplayDot5\" class=\"statusDisplayDot\">*</span>");
		out.write("&nbsp;<span id=\"statusDisplayDot6\" class=\"statusDisplayDot\">*</span>");
		out.write("&nbsp;<span id=\"statusDisplayDot7\" class=\"statusDisplayDot\">*</span>");
		out.writeLine("</p>");
		
		//	add progress bar
		out.writeLine("<p><div id=\"statusDisplayProgressBar\" class=\"statusDisplayProgressBar\"><div id=\"statusDisplayProgressIndicator\" class=\"statusDisplayProgressIndicator\"></div></div></p>");
		
		//	write status message display
		out.writeLine("<p id=\"statusMessage\">Your request is being processed.</p>");
		
		//	write link section
		out.write("<div id=\"links\">");
		if (cancelLinkLabel != null)
			out.write("<span id=\"cancelLink\"><a class=\"navigationLink\" href=\"#\" onclick=\"cancelRequest()\">" + IoTools.prepareForHtml(cancelLinkLabel) + "</a></span>");
		out.write("<span id=\"feedbackLink\" style=\"display: none;\"><a class=\"navigationLink\" href=\"#\" onclick=\"openFeedbackRequest(null)\">" + IoTools.prepareForHtml(ar.getFeedbackLinkLabel()) + "</a></span>");
		out.write("<span id=\"resultLink\" style=\"display: none;\"><a class=\"navigationLink\" href=\"" + resultLink + "\" onclick=\"resultRetrieved();\">" + IoTools.prepareForHtml(ar.getResultLinkLabel()) + "</a></span>");
		out.write("<span id=\"errorLogLink\" style=\"display: none;\"><a class=\"navigationLink\" target=\"_blank\" href=\"" + request.getContextPath() + request.getServletPath() + "/" + requestId + "/" + ERRORS_ACTION + "\" onclick=\"errorLogRetrieved();\">" + IoTools.prepareForHtml(ar.getErrorLogLinkLabel()) + "</a></span>");
		if ((cancelledLink != null) && (cancelledLinkLabel != null))
			out.write("<span id=\"cancelledLink\" style=\"display: none;\"><a class=\"navigationLink\" href=\"" + cancelledLink + "\">" + IoTools.prepareForHtml(cancelledLinkLabel) + "</a></span>");
		out.writeLine("</div>");
		
		//	flush buffer
		out.flush();
	}
	
	/**
	 * Write JavaScript code that animates and updates the request status. This
	 * default implementation uses an invisible IFRAME for updates. Sub classes
	 * may overwrite this method to animate and update the request status
	 * differently. However, they than have to make sure that the JavaScript
	 * code written by their version of this method works with the HTML code
	 * written by the writeStatusDisplayHtml() method. It is best to always
	 * overwrite both.<br>
	 * <br>
	 * The JavaScript generated by this implementation includes seven mounting
	 * points notifying client code of actions in the status display:
	 * <ul>
	 * <li>statusUpdated(): called whenever the status data in the invisible
	 * IFRAME has been updated.</li>
	 * <li>feedbackWindowOpened(): called when a feedback request dialog window
	 * has been opened.</li>
	 * <li>feedbackWindowSubmitted(mode): called when a feedback request dialog
	 * window is submitted; the mode parameter identifies the submit button the
	 * user clicked on.</li>
	 * <li>requestCancelled(): called when the request the status display
	 * belongs to has been cancelled, after cancellation is confirmed via status
	 * information.</li>
	 * <li>requestFinished(): called when the request the status display belongs
	 * to is finished, after this is confirmed via status information.</li>
	 * <li>resultRetrieved(): called when the link to the result is clicked.</li>
	 * <li>errorLogRetrieved(): called when the link to the error log is clicked
	 * </li>
	 * </ul>
	 * The default implementations of the mounting point methods are empty,
	 * client code wanting to use the mounting points is free to replace them
	 * with some application specific implementation.
	 * @param out the writer to write to
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @throws IOException
	 */
	protected void writeStatusDisplayStylesAndScripts(BufferedLineWriter out, HttpServletRequest request, String requestId) throws IOException {
		AsynchronousRequest ar = this.getAsynchronousRequest(requestId);
		if (ar == null)
			return;
		
		out.writeLine("<script type=\"text/javascript\">");
		
		out.writeLine("var statusUpdater;");
		out.writeLine("var statusDisplay = 0;");
		out.writeLine("var statusTime = 0;");
		out.writeLine("var statusUpdateFrame;");
		out.writeLine("var statusMessage;");
		out.writeLine("var statusDisplayProgressIndicator;");
		out.writeLine("var feedbackLink;");
		
		out.writeLine("function updateStatus() {");
		out.writeLine("  if (statusUpdater == null)");
		out.writeLine("    statusUpdater = window.setInterval('updateStatus()', 500);");
		out.writeLine("  if (statusUpdateFrame == null)");
		out.writeLine("    statusUpdateFrame = $('statusUpdateFrame');");
		out.writeLine("  if (statusMessage == null)");
		out.writeLine("    statusMessage = $('statusMessage');");
		out.writeLine("  if (statusDisplayProgressIndicator == null)");
		out.writeLine("    statusDisplayProgressIndicator = $('statusDisplayProgressIndicator');");
		out.writeLine("  if (feedbackLink == null)");
		out.writeLine("    feedbackLink = $('feedbackLink');");
		out.writeLine("  $('statusDisplayDot' + statusDisplay).style.color = 'white';");
		out.writeLine("  statusDisplay++;");
		out.writeLine("  if (statusDisplay > 7)");
		out.writeLine("    statusDisplay -= 8;");
		out.writeLine("  $('statusDisplayDot' + statusDisplay).style.color = 'black';");
		out.writeLine("  if ((statusDisplay % 4) == 0) {");
		out.writeLine("    statusUpdateFrame.src = '" + request.getContextPath() + request.getServletPath() + "/" + requestId + "/" + STATUS_UPDATE_ACTION + "';");
		out.writeLine("    statusUpdated();");
		out.writeLine("  }");
		out.writeLine("  else {");
		out.writeLine("    if (statusUpdateFrame.contentWindow.document.getElementById('status'))");
		out.writeLine("      statusMessage.innerHTML = statusUpdateFrame.contentWindow.document.getElementById('status').value;");
		out.writeLine("    if (statusUpdateFrame.contentWindow.document.getElementById('percent'))");
		out.writeLine("      statusDisplayProgressIndicator.style.width = (statusUpdateFrame.contentWindow.document.getElementById('percent').value + '%');");
		out.writeLine("    if (statusUpdateFrame.contentWindow.document.getElementById('feedback')) {");
		out.writeLine("      var st = (statusUpdateFrame.contentWindow.document.getElementById('time') ? statusUpdateFrame.contentWindow.document.getElementById('time').value : 0);");
		out.writeLine("      if (st != statusTime) {");
		out.writeLine("        openFeedbackRequest(statusUpdateFrame.contentWindow.document.getElementById('feedback').value);");
		out.writeLine("        feedbackLink.style.display = '';");
		out.writeLine("        statusTime = st;");
		out.writeLine("      }");
		out.writeLine("    }");
		out.writeLine("    else feedbackLink.style.display = 'none';"); // callback should do this, but let's keep this in case something goes wrong
		out.writeLine("  }");
		out.writeLine("  if (statusUpdateFrame.contentWindow.document.getElementById('finished')) {");
		out.writeLine("    $('statusDisplayDot' + statusDisplay).style.color = 'white';");
		out.writeLine("    window.clearInterval(statusUpdater);");
		out.writeLine("    requestFinished();");
		out.writeLine("    if (statusUpdateFrame.contentWindow.document.getElementById('forward'))");
		out.writeLine("      window.location.href = statusUpdateFrame.contentWindow.document.getElementById('forward').value;");
		out.writeLine("    else {");
		out.writeLine("      $('statusLabelRunning').style.display = 'none';");
		out.writeLine("      for (var sdd = 0; sdd < 8; sdd++)");
		out.writeLine("        $('statusDisplayDot' + sdd).style.display = 'none';");
		out.writeLine("      $('statusLabelFinished').style.display = '';");
		out.writeLine("      $('resultLink').style.display = '';");
		out.writeLine("      if ($('cancelLink'))");
		out.writeLine("        $('cancelLink').style.display = 'none';");
		out.writeLine("      if (statusUpdateFrame.contentWindow.document.getElementById('errors'))");
		out.writeLine("        $('errorLogLink').style.display = '';");
		out.writeLine("    }");
		out.writeLine("  }");
		out.writeLine("}");
		
		String cancellationConfirmDialogText = ar.getCancellationConfirmDialogText();
		out.writeLine("var preCancelStatusTime = 0;");
		out.writeLine("function cancelRequest() {");
		if (cancellationConfirmDialogText != null) {
			out.writeLine("  var cc = window.confirm('" + cancellationConfirmDialogText + "');");
			out.writeLine("  if (cc == false)");
			out.writeLine("    return;");
		}
		out.writeLine("  window.clearInterval(statusUpdater);");
		out.writeLine("  if (statusUpdateFrame.contentWindow.document.getElementById('time'))");
		out.writeLine("    preCancelStatusTime = statusUpdateFrame.contentWindow.document.getElementById('time').value;");
		out.writeLine("  statusUpdateFrame.src = '" + request.getContextPath() + request.getServletPath() + "/" + requestId + "/" + CANCEL_ACTION + "';");
		out.writeLine("  checkRequestCancelled();");
		out.writeLine("}");
		
		out.writeLine("function checkRequestCancelled() {");
		out.writeLine("  var st = 1;");
		out.writeLine("  if (statusUpdateFrame.contentWindow.document.getElementById('time'))");
		out.writeLine("    st = statusUpdateFrame.contentWindow.document.getElementById('time').value;");
		out.writeLine("  if (preCancelStatusTime == st) {");
		out.writeLine("    window.setTimeout('checkRequestCancelled()', 250);");
		out.writeLine("    return;");
		out.writeLine("  }");
		out.writeLine("  requestCancelled();");
		out.writeLine("  if (statusUpdateFrame.contentWindow.document.getElementById('forward'))");
		out.writeLine("    window.location.href = statusUpdateFrame.contentWindow.document.getElementById('forward').value;");
		out.writeLine("  else {");
		out.writeLine("    $('statusLabelRunning').style.display = 'none';");
		out.writeLine("    for (var sdd = 0; sdd < 8; sdd++)");
		out.writeLine("      $('statusDisplayDot' + sdd).style.display = 'none';");
		out.writeLine("    $('statusLabelCancelled').style.display = '';");
		out.writeLine("    $('cancelLink').style.display = 'none';");
		out.writeLine("    $('cancelledLink').style.display = '';");
		out.writeLine("  }");
		out.writeLine("}");

		out.writeLine("var feedbackWindow;");
		out.writeLine("var feedbackWindowId;");
		out.writeLine("var feedbackSubmitted = new Object();");
		
		out.writeLine("function openFeedbackRequest(feedbackId) {");
		out.writeLine("  if (feedbackSubmitted[feedbackId])");
		out.writeLine("    return;");
		out.writeLine("  if (feedbackId)");
		out.writeLine("    feedbackWindowId = feedbackId;");
		out.writeLine("  else feedbackId = feedbackWindowId;");
		out.writeLine("  if (feedbackWindow && !feedbackWindow.closed) {");
		out.writeLine("    feedbackWindow.focus();");
		out.writeLine("    return;");
		out.writeLine("  }");
		out.writeLine("  feedbackWindow = window.open('" + request.getContextPath() + request.getServletPath() + "/" + requestId + "/" + GET_FEEDBACK_REQUEST_PAGE_ACTION + "', 'Your Input is Required', 'width=500,height=400,top=100,left=100,resizable=yes,scrollbar=yes,scrollbars=yes');");
		out.writeLine("  if (feedbackWindow)");
		out.writeLine("    setNotifyParentScript();");
		out.writeLine("  else {");
		out.writeLine("    alert('Please deactivate your popup blocker for this page\\nso JavaScript can open feedback requests.');");
		out.writeLine("    openFeedbackRequest(null);");
		out.writeLine("  }");
		out.writeLine("}");
		
		out.writeLine("function setNotifyParentScript() {");
		out.writeLine("  if (feedbackWindow == null)");
		out.writeLine("    return;");
		out.writeLine("  if (feedbackWindow.notifyParentScript) {");
		out.writeLine("    feedbackWindow.notifyParentScript = function() {");
		out.writeLine("        feedbackSubmitted[feedbackWindowId] = true;");
		out.writeLine("        feedbackLink.style.display = 'none';");
		out.writeLine("        feedbackWindowSubmitted(feedbackWindow.getSubmitMode());");
		out.writeLine("        feedbackWindow = null;");
		out.writeLine("        feedbackWindowId = null;");
		out.writeLine("      };");
		out.writeLine("    feedbackWindowOpened();");
		out.writeLine("  }");
		out.writeLine("  else window.setTimeout('setNotifyParentScript()', 250);");
		out.writeLine("}");
		
		out.writeLine("function $(id) {");
		out.writeLine("  return document.getElementById(id);");
		out.writeLine("}");
		
		out.writeLine("function statusUpdated() {}");
		out.writeLine("function feedbackWindowOpened() {}");
		out.writeLine("function feedbackWindowSubmitted(mode) {}");
		out.writeLine("function requestCancelled() {}");
		out.writeLine("function requestFinished() {}");
		out.writeLine("function resultRetrieved() {}");
		out.writeLine("function errorLogRetrieved() {}");
		
		out.writeLine("</script>");
		
		out.writeLine("<style>");
		out.writeLine(".statusDisplayDot {");
		out.writeLine("  font-weight: bold;");
		out.writeLine("  color: white;");
		out.writeLine("}");
		out.writeLine(".statusDisplayProgressBar {");
		out.writeLine("  border-width: 1px;");
		out.writeLine("  border-style: solid;");
		out.writeLine("  border-color: black;");
		out.writeLine("}");
		out.writeLine(".statusDisplayProgressIndicator {");
		out.writeLine("  width: 0%;");
		out.writeLine("  height: 20px;");
		out.writeLine("  background-color: blue;");
		out.writeLine("}");
		out.writeLine("</style>");
		
		out.flush();
	}
	
	/**
	 * Write HTML code that updates the displayed request processing status.
	 * This page is for display in the invisible IFRAME created by the
	 * writeStatusDisplay() and writeStatusDisplayPage() methods, not for actual
	 * display. It contains nothing but a form containing a few hidden elements
	 * for the status display code to read and update itself from.
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @param response the HTTP resonse to write to
	 * @throws IOException
	 */
	public void sendStatusUpdate(HttpServletRequest request, String requestId, HttpServletResponse response) throws IOException {
		if (requestId == null)
			requestId = this.getRequestID(request);
		
		//	get asynchronous request
		AsynchronousRequest ar = this.getAsynchronousRequest(requestId);
		
		//	found it, delegate
		if (ar != null)
			ar.sendStatusUpdate(request, response);
		
		//	request not found, send error if in XML mode
		else if (this.inXmlMode)
			response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Invalid Request ID " + requestId));
		
		//	request not found, send finalization status page if in HTML mode
		else {
			
			//	prepare output
			response.setContentType("text/html");
			response.setCharacterEncoding("UTF-8");
			Writer out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
			BufferedLineWriter bw = new BufferedLineWriter(out);
			
			//	write HTML status form
			bw.writeLine("<html><body>");
			bw.writeLine("<form action=\"none\" method=\"GET\">");
			bw.writeLine("<input name=\"time\" type=\"hidden\" id=\"time\" value=\"" + System.currentTimeMillis() + "\">");
			bw.writeLine("<input name=\"status\" type=\"hidden\" id=\"status\" value=\"Invalid Request ID\">");
			bw.writeLine("<input name=\"percent\" type=\"hidden\" id=\"percent\" value=\"0\">");
			bw.writeLine("<input name=\"finished\" type=\"hidden\" id=\"finished\" value=\"true\">");
			bw.writeLine("</form>");
			bw.writeLine("</body></html>");
			
			bw.flush();
			out.flush();
		}
	}
	
	/**
	 * Write the status information encoded in XML.
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @param out the writer to write to
	 * @throws IOException
	 */
	public void writeStatusXML(HttpServletRequest request, String requestId, Writer out) throws IOException {
		if (requestId == null)
			requestId = this.getRequestID(request);
		
		//	get asynchronous request
		AsynchronousRequest ar = this.getAsynchronousRequest(requestId);
		
		//	found it, send status
		if (ar != null)
			ar.writeStatusXML(request, out);
	}
	
	/**
	 * Write an HTML page that displays a pending feedback request. The
	 * JavaScripts output by the default implementation of the status display
	 * use a new browser window for each feedback request. Thus, this default
	 * implementation closes this very window when the feedback form is
	 * submitted. Sub classes that choose to display feedback requst froms in a
	 * different way should also overwrite the sendFeedbackResultPage() method
	 * to provide a meaningful result page.
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @param response the HTTP resonse to write to
	 * @throws IOException
	 */
	public void sendFeedbackRequestPage(HttpServletRequest request, String requestId, HttpServletResponse response) throws IOException {
		if (requestId == null)
			requestId = this.getRequestID(request);
		final String arId = requestId;
		
		//	get feedback request
		ArFeedbackRequest fr = this.feedbackService.getFeedbackRequest(arId);
		
		//	no feedback request pending
		if (fr == null) {
			this.sendFeedbackResultPage(request, requestId, response);
			return;
		}
		
		//	create form page builder
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		FeedbackFormPageBuilder ffpb = new FeedbackFormPageBuilder(this.getPageBuilderHost(), request, response, fr.fp, fr.fpRenderer, this.getSubmitModes(fr), requestId) {
			protected String getFormActionPathInfo() {
				return ("/" + arId + "/" + ANSWER_FEEDBACK_REQUEST_ACTION);
			}
			protected String[] getOnsubmitCalls() {
				String[] oscs = {
						"notifyParentScript();",
					};
				return oscs;
			}
			protected void writePageHeadExtensions() throws IOException {
				super.writePageHeadExtensions();
				this.writeLine("<script type=\"text/javascript\">");
				this.writeLine("function notifyParentScript() {}");
				this.writeLine("</script>");
			}
		};
		
		//	send feedback form
		this.sendFeedbackFormPage(ffpb);
	}
	
	/**
	 * Write a block for an HTML page that displays a pending feedback request,
	 * in particuar a FORM element that also includes the JavaScripts and CSS
	 * styles required for the form content to work.
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @param response the HTTP resonse to write to
	 * @throws IOException
	 */
	public void sendFeedbackRequestBlock(HttpServletRequest request, String requestId, HttpServletResponse response) throws IOException {
		if (requestId == null)
			requestId = this.getRequestID(request);
		final String arId = requestId;
		
		//	get feedback request
		ArFeedbackRequest fr = this.feedbackService.getFeedbackRequest(arId);
		
		//	no feedback request pending
		if (fr == null) {
			this.sendFeedbackResultPage(request, requestId, response);
			return;
		}
		
		//	create form page builder
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		FeedbackFormBlockBuilder ffpb = new FeedbackFormBlockBuilder(this.getPageBuilderHost(), request, response, fr.fp, fr.fpRenderer, this.getSubmitModes(fr), requestId) {
			protected String getFormActionPathInfo() {
				return ("/" + arId + "/" + ANSWER_FEEDBACK_REQUEST_ACTION);
			}
			protected String[] getOnsubmitCalls() {
				String[] oscs = {
						"notifyParentScript();",
					};
				return oscs;
			}
			protected void writeFormContent() throws IOException {
				this.writeLine("<script type=\"text/javascript\">");
				this.writeLine("function notifyParentScript() {}");
				this.writeLine("</script>");
				super.writeFormContent();
			}
		};
		
		//	send feedback form
		this.sendFeedbackFormPage(ffpb);
	}
	
	/**
	 * Retrieve the submit modes for a feedback request, i.e., the submit
	 * buttons to display in the feedback form. This default implementation
	 * returns a single 'OK' submit mode, sub classes wanting to allow more
	 * options can overwrite it as needed.
	 * @param arfr the feedback request to get the submit modes for
	 * @return the submit modes for the argument feedback request
	 */
	protected SubmitMode[] getSubmitModes(ArFeedbackRequest arfr) {
		return defaultSubmitModes;
	}
	
	private static SubmitMode[] defaultSubmitModes = {
		new SubmitMode("OK", "OK", "Submit feedback"),
	};
	
	/**
	 * Write the HTML page that displays the result of the submission of a
	 * feedback request. As submitting a feedback request form generated by the
	 * default implementation of sendFeedbackRequestPage() closes the feedback
	 * window, this default implementation simply sends an empty HTML page
	 * closing the window in case hte latter did not work with the form
	 * submission. Sub classes that choose to display feedback requst froms in a
	 * different way should also overwrite this method to provide a meaningful
	 * result page.
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @param response the HTTP resonse to write to
	 * @throws IOException
	 */
	public void sendFeedbackResultPage(HttpServletRequest request, String requestId, HttpServletResponse response) throws IOException {
		
		//	set up output
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
		
		//	write window closer (we have to wait a little to make sure all other onload handlers have run, as one might modify the close() function, etc.)
		bw.write("<html><body onload=\"window.setTimeout('window.close()', 25);\"></body></html>");
		
		//	send window closer
		bw.flush();
	}
	
	/**
	 * Process data submitted from a feedback request form.
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @param isAnswer does the request contain an answer to a feedback request,
	 *            or a cancellation?
	 * @param response the HTTP resonse to write to
	 * @throws IOException
	 */
	public void processFeedback(HttpServletRequest request, String requestId, boolean isAnswer, HttpServletResponse response) throws IOException {
		if (requestId == null)
			requestId = this.getRequestID(request);
		
		//	answer/cancel feedback request
//		this.feedbackService.answerFeedbackRequest(requestId, ((isAnswer && "OK".equals(request.getParameter(FeedbackFormPageBuilder.SUBMIT_MODE_PARAMETER))) ? request : null));
		this.feedbackService.answerFeedbackRequest(requestId, (isAnswer ? request : null));
		
		//	send result page
		this.sendFeedbackResultPage(request, requestId, response);
	}
	
	/**
	 * Forcefully cancel a running asynchronous request externally.
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @param response the HTTP resonse to write to
	 * @throws IOException
	 */
	public void cancelRequest(HttpServletRequest request, String requestId, HttpServletResponse response) throws IOException {
		if (requestId == null)
			requestId = this.getRequestID(request);
		
		//	get asynchronous request ...
		AsynchronousRequest ar = this.getAsynchronousRequest(requestId);
		
		//	... and terminate it
		if (ar != null) {
			ar.terminate(true);
			ar.sendStatusUpdate(request, response);
		}
		
		//	no such request
		else response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Invalid Request ID " + requestId));
	}
	
	/**
	 * Write the error report of an asynchronous request. If no asynchronous
	 * request exists for the argument ID, this method sends HTTP error 404
	 * ('Not Found'). If an asynchronous request exists, but is not yet
	 * finished, this method sends a text string indicating this (HTML mode) or
	 * sends HTTP error 404 (XML mode)
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @param response the HTTP resonse to write to
	 * @throws IOException
	 */
	public void sendResult(HttpServletRequest request, String requestId, HttpServletResponse response) throws IOException {
		
		//	get asynchronous request
		AsynchronousRequest ar = this.getAsynchronousRequest(requestId);
		
		//	write XML result
		if (this.inXmlMode) {
			
			//	request not found, send error
			if (ar == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Invalid Request ID " + requestId));
				return;
			}
			
			//	request not finished yet
			if (!ar.isFinished()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Request " + requestId + " Still Processing"));
				return;
			}
			
			//	try implementation specific output
			if (ar.sendResult(request, response))
				return;
			
			//	result not available
			response.sendError(HttpServletResponse.SC_NOT_FOUND, ("No Result Available for Request " + requestId));
		}
		
		//	write HTML result
		else {
			
			//	request not found, send error
			if (ar == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Invalid Request ID " + requestId));
				return;
			}
			
			//	try implementation specific output
			if (ar.sendResult(request, response))
				return;
			
			//	prepare output
			response.setContentType("text/plain");
			response.setCharacterEncoding("UTF-8");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
			
			//	request not finished yet
			if (!ar.isFinished())
				bw.write("Request " + requestId + " is still processing, a result is only available after the request is complete.");
			
			//	no result available
			else bw.write("Final Status of Request " + requestId + ": " + ar.getStatus());
			
			//	send data
			bw.flush();
		}
	}
	
	/**
	 * Write the error report of an asynchronous request. If no asynchronous
	 * request exists for the argument ID, this method sends HTTP error 404
	 * ('Not Found'). If an asynchronous request exists, but is not yet
	 * finished, this method sends a text string indicating this (HTML mode) or
	 * sends HTTP error 404 (XML mode)
	 * @param request the HTTP request in whose response to include the status
	 *            display
	 * @param requestId the ID of the asynchronous request whose status to
	 *            display
	 * @param response the HTTP resonse to write to
	 * @throws IOException
	 */
	public void sendErrorReport(HttpServletRequest request, String requestId, HttpServletResponse response) throws IOException {
		
		//	get asynchronous request
		AsynchronousRequest ar = this.getAsynchronousRequest(requestId);
		
		//	write XML error report
		if (this.inXmlMode) {
			
			//	request not found, send error
			if (ar == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Invalid Request ID " + requestId));
				return;
			}
			
			//	request not finished yet
			if (!ar.isFinished()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Request " + requestId + " Still Processing"));
				return;
			}
			
			//	try implementation specific output
			if (ar.sendErrorReport(request, response))
				return;
			
			//	prepare output
			response.setContentType("text/plain");
			response.setCharacterEncoding("UTF-8");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
			
			//	errors to report
			if (ar.hasError()) {
				bw.write(ar.getErrorMessage());
				if (ar.getError() != null) {
					bw.newLine();
					bw.newLine();
					ar.getError().printStackTrace(new PrintWriter(bw));
				}
			}
			
			//	no errors
			else bw.write("No Errors");
			
			//	send data
			bw.flush();
		}
		
		//	write HTML error report
		else {
			
			//	request not found, send error
			if (ar == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, ("Invalid Request ID " + requestId));
				return;
			}
			
			//	try implementation specific output
			if (ar.sendErrorReport(request, response))
				return;
			
			//	prepare output
			response.setContentType("text/plain");
			response.setCharacterEncoding("UTF-8");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
			
			//	request not finished yet
			if (!ar.isFinished())
				bw.write("Request " + requestId + " is still processing, an error report is only available after the request is complete.");
			
			//	errors to report
			else if (ar.hasError()) {
				bw.write(ar.getErrorMessage());
				if (ar.getError() != null) {
					bw.newLine();
					bw.newLine();
					ar.getError().printStackTrace(new PrintWriter(bw));
				}
			}
			
			//	no errors
			else bw.write("No Errors");
			
			//	send data
			bw.flush();
		}
	}
	
	/**
	 * Send a generic HTML page, for instance displaying the result of an
	 * asynchronous request or an error log. This method exists so concrete sub
	 * classes of this class can use the facilities of their surrounding
	 * servlets to output web pages.
	 * @param hpb the page builder to use
	 * @throws IOException
	 */
	protected abstract void sendHtmlPage(HtmlPageBuilder hpb) throws IOException;
	
	/**
	 * Send a generic HTML page for display in a popup window, for instance
	 * displaying the result of an asynchronous request or an error log. This
	 * method exists so concrete sub classes of this class can use the
	 * facilities of their surrounding servlets to output web pages.
	 * @param hpb the page builder to use
	 * @throws IOException
	 */
	protected abstract void sendPopupHtmlPage(HtmlPageBuilder hpb) throws IOException;
	
	/**
	 * Send an HTML page showing the status of an asynchronous request in an
	 * IFRAME. This method exists so concrete sub classes of this class can use
	 * the facilities of their surrounding servlets to output web pages. This
	 * default implementation loops through to the sendHtmlPage() method, sub
	 * classes are welcome to overwrite it as needed.
	 * @param hpb the page builder to use
	 * @throws IOException
	 */
	protected void sendStatusDisplayIFramePage(HtmlPageBuilder hpb) throws IOException {
		this.sendHtmlPage(hpb);
	}
	
	/**
	 * Send an HTML page showing the status of an asynchronous request in a
	 * popup window. This method exists so concrete sub classes of this class
	 * can use the facilities of their surrounding servlets to output web pages.
	 * This default implementation loops through to the sendPopupHtmlPage()
	 * method, sub classes are welcome to overwrite it as needed.
	 * @param hpb the page builder to use
	 * @throws IOException
	 */
	protected void sendStatusDisplayPopupPage(HtmlPageBuilder hpb) throws IOException {
		this.sendPopupHtmlPage(hpb);
	}
	
	/**
	 * Send an HTML page containing a feedback form. This method exists so
	 * concrete sub classes of this class can use the facilities of their
	 * surrounding servlets to output web pages. This default implementation
	 * loops through to the sendPopupHtmlPage() method, sub classes are welcome
	 * to overwrite it as needed.
	 * @param hpb the page builder to use
	 * @throws IOException
	 */
	protected void sendFeedbackFormPage(HtmlPageBuilder hpb) throws IOException {
		this.sendPopupHtmlPage(hpb);
	}
	
	/**
	 * Retrieve an HTML page builder host. This method exists so concrete sub
	 * classes of this calss can use the facilities of their surrounding
	 * servlets to output web pages.
	 */
	protected abstract HtmlPageBuilderHost getPageBuilderHost();
	
	private String getRequestID(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		if (pathInfo == null)
			return null;
		if (pathInfo.startsWith("/"))
			pathInfo = pathInfo.substring(1);
		return ((pathInfo.indexOf('/') == -1) ? pathInfo : pathInfo.substring(0, pathInfo.indexOf('/')));
	}
	
	/**
	 * Handle an HTTP request received by a surrounding servlet. This method
	 * checks the path info of the HTTP request and chunks the ID of the
	 * asynchronous request and the action to take from it, nothing else. Note
	 * that this method only handles HTTP requests referring to asynchronous
	 * requests that already exist; recognizing HTTP requests that start an
	 * asynchronous request is up to the surrounding servlet. In particular,
	 * this method handles HTTP requests whose path info starts with the ID of
	 * an existing asynchonous request, followed by a slash and one of the
	 * action constants defined by this class. Surrounding servlets need not use
	 * this method; they may parse the path info themselves and then delegate to
	 * the appropriate method of this class. They may also implement additional
	 * actions and use them as needed, or handle the existing actions
	 * differently.
	 * @param request the HTTP request to handle
	 * @param response the HTTP resonse to write to
	 * @return true if the request has been handled, false otherwise
	 * @throws IOException
	 */
	public boolean handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		//	parse path info
		String pathInfo = request.getPathInfo();
		if (pathInfo == null)
			return false;
		if (pathInfo.startsWith("/"))
			pathInfo = pathInfo.substring(1);
		String arId;
		String action = STATUS_ACTION;
		if (pathInfo.indexOf('/') == -1)
			arId = pathInfo;
		else {
			arId = pathInfo.substring(0, pathInfo.indexOf('/'));
			action = pathInfo.substring(pathInfo.indexOf('/') + 1);
		}
		
		//	check if request exists
		if (!this.isRequestID(arId))
			return false;
		
		//	status update page request
		if (STATUS_UPDATE_ACTION.equals(action)) {
			this.sendStatusUpdate(request, arId, response);
			return true;
		}
		
		//	status page request
		if (STATUS_ACTION.equals(action)) {
			this.sendStatusDisplayPage(request, arId, response);
			return true;
		}
		
		//	result page request
		if (RESULT_ACTION.equals(action)) {
			this.sendResult(request, arId, response);
			return true;
		}
		
		//	error report request
		if (ERRORS_ACTION.equals(action)) {
			this.sendErrorReport(request, arId, response);
			return true;
		}
		
		//	cancellation request
		if (CANCEL_ACTION.equals(action)) {
			this.cancelRequest(request, arId, response);
			return true;
		}
		
		//	feedback form request
		if (GET_FEEDBACK_REQUEST_PAGE_ACTION.equals(action)) {
			this.sendFeedbackRequestPage(request, arId, response);
			return true;
		}
		
		//	feedback form request
		if (GET_FEEDBACK_REQUEST_BLOCK_ACTION.equals(action)) {
			this.sendFeedbackRequestBlock(request, arId, response);
			return true;
		}
		
		//	feedback form submission request
		if (ANSWER_FEEDBACK_REQUEST_ACTION.equals(action)) {
			this.processFeedback(request, arId, true, response);
			return true;
		}
		
		//	feedback form request
		if (CANCEL_FEEDBACK_REQUEST_ACTION.equals(action)) {
			this.processFeedback(request, arId, false, response);
			return true;
		}
		
		//	we cannot handle this one
		return false;
	}
	
	/**
	 * the invocation suffix for obtaining the status page of an asynchronous
	 * request, if showing in a separate IFRAME or window
	 */
	public static final String STATUS_ACTION = "status";
	
	/**
	 * the invocation suffix for obtaining the status of an asynchronous request
	 */
	public static final String STATUS_UPDATE_ACTION = "su";
	
	/**
	 * the invocation suffix for obtaining the result of an asynchronous
	 * request, soon as it becomes available
	 */
	public static final String RESULT_ACTION = "result";
	
	/**
	 * the invocation suffix for obtaining the error report of an asynchronous
	 * request, soon as it becomes available, which is at the same time as the
	 * result
	 */
	public static final String ERRORS_ACTION = "errors";
	
	/**
	 * The invocation suffix for externally cancelling a running asynchronous
	 * request.
	 */
	public static final String CANCEL_ACTION = "cancel";
	
	/**
	 * The invocation suffix for obtaining the currently pending feedback
	 * request (if any) of an asynchronous request as a full, standalone HTML
	 * page, if any. This invokation suffix is the one used by the default
	 * status display code. Sub classes may change this behavior, however. For
	 * each pending feedback request, client code shoud use either this
	 * invokation suffix or its block counterpart, but not both.
	 */
	public static final String GET_FEEDBACK_REQUEST_PAGE_ACTION = "frGetP";
	
	/**
	 * The invocation suffix for obtaining the currently pending feedback
	 * request (if any) of an asynchronous request as a block element that can
	 * be embedded in an HTML page, in particular a FORM element that also
	 * includes the JavaScripts and CSS styles required for the form content to
	 * work. The JavaScripts of the default status display do not use this
	 * invokation suffix, it is mostly intended for XML based interaction with
	 * AJAX based fron ends. Sub classes may, however, choose to also use it in
	 * the default status display. For each pending feedback request, client
	 * code shoud use either this invokation suffix or its page counterpart, but
	 * not both.
	 */
	public static final String GET_FEEDBACK_REQUEST_BLOCK_ACTION = "frGetB";
	
	/**
	 * The invocation suffix for aswering the currently pending feedback request
	 * of an asynchronous request, if any. This invokation suffix is exclusively
	 * used in the action URL of feedback forms.
	 */
	public static final String ANSWER_FEEDBACK_REQUEST_ACTION = "frAnswer";
	
	/**
	 * The invocation suffix for cancelling the currently pending feedback
	 * request of an asynchronous request, if any. This invokation suffix is
	 * only used in XML mode by default, not in the JavaScripts of the default
	 * status display. Sub classes may change this, however.
	 */
	public static final String CANCEL_FEEDBACK_REQUEST_ACTION = "frCancel";
}

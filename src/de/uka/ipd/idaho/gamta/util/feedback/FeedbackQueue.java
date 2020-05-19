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
package de.uka.ipd.idaho.gamta.util.feedback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Waiting queue for feedback requests to be answered asynchronously. In order
 * for this to work, some component has to retrieve pending requests via the
 * getFeedbackRequest() method, display them in some fashion, and then indicate
 * having received an answer through the answerFeedbackRequest() method.
 * 
 * @author sautter
 */
public class FeedbackQueue implements FeedbackPanel.FeedbackService {
	
	private static final boolean DEBUG = true;
	
	private LinkedList waitingRequests = new LinkedList();
	private HashMap waitingRequestGroups = new HashMap();
	private HashSet pendingRequests = new HashSet();
	
	/**
	 * Retrieve an enqueued feedback request for displaying. This method will
	 * block until a feedback request is available
	 * @return a pending feedback request so it can be answered
	 * @see java.lang.Object#wait()
	 */
	public synchronized FeedbackPanel getFeedbackRequest() {
		return this.getFeedbackRequest(0);
	}
	
	/**
	 * Retrieve an enqueued feedback request for displaying. A timeout of 0 will
	 * wait until there is a feedback request, or the queue is flushed or
	 * cleared. A timeout of -1 (or any negative value) does not wait at all,
	 * thus returns null immediately if there are no feedback request in the
	 * queue. The latter is for threads that check for feedback requests
	 * periodically and do other stuff in the meantime, or do their waiting on
	 * some other object.
	 * @param timeout the maximum number of milliseconds to wait for a feedback
	 *            request to be available before returning null
	 * @return a pending feedback request so it can be answered, or null, if
	 *         there are no requests pending
	 * @see java.lang.Object#wait(long)
	 */
	public synchronized FeedbackPanel getFeedbackRequest(long timeout) {
		if (this.waitingRequests.isEmpty() && (timeout > -1)) {
			try {
				if (DEBUG) System.out.println("- waiting on FP queue (" + Thread.currentThread().getId() + ")");
				this.wait(timeout);
			} catch (InterruptedException ie) {}
			if (DEBUG) System.out.println("- woken up from FP queue (" + Thread.currentThread().getId() + ")");
		}
		if (this.waitingRequests.isEmpty()) return null;
		else {
			FeedbackPanel fp = ((FeedbackPanel) this.waitingRequests.removeFirst());
			this.pendingRequests.add(fp);
			return fp;
		}
	}
	
	/**
	 * Retrieve the size of the feedback queue, i.e., the number of feedback
	 * requests awaiting an answer or are in the process of being answered.
	 * @return the current size of the feedback queue
	 */
	public synchronized int size() {
		return (this.waitingRequests.size() + this.pendingRequests.size());
	}
	
	/**
	 * Retrieve the number of feedback requests awaiting an answer. This
	 * excludes the feedback requests currently checked out through the
	 * getFeedbackRequest() methods.
	 * @return the number of waiting feedback requests
	 */
	public synchronized int waiting() {
		return this.waitingRequests.size();
	}
	
	/**
	 * Retrieve the number of feedback requests in the process of being
	 * answered, i.e., the feedback requests currently checked out through the
	 * getFeedbackRequest() methods.
	 * @return the number of feedback requests currently being answered
	 */
	public synchronized int pending() {
		return this.pendingRequests.size();
	}
	
	/**
	 * Flush the feedback queue, cancelling all waiting requests and making any
	 * thread waiting on one of the getFeedbackRequest() methods return (return
	 * value will be null). Pending requests remain untouched by this method.
	 * All waiting invokations of the getFeedback() or getMultiFeedback()
	 * methods will throw a RuntimeException, unless their requesr is already
	 * pending, i.e., checked out by some other party for answering.
	 */
	public synchronized void flush() {
		Set fpGroupSet = new HashSet();
		for (Iterator fpgit = this.waitingRequests.iterator(); fpgit.hasNext();) {
			FeedbackPanel fp = ((FeedbackPanel) fpgit.next());
			FpGroup fpGroup = ((FpGroup) this.waitingRequestGroups.remove(fp));
			fpGroupSet.add(fpGroup);
		}
		this.waitingRequests.clear();
		for (Iterator fpgit = fpGroupSet.iterator(); fpgit.hasNext();) {
			FpGroup fpGroup = ((FpGroup) fpgit.next());
			synchronized(fpGroup) {
				if (DEBUG) System.out.println("- notifying FpGroup lock (" + Thread.currentThread().getId() + ")");
				fpGroup.exception = new RuntimeException("Feedback request cancelled because queue flushed.");
//				for (int f = 0; f < fpGroup.fps.length; f++)
//					this.feedbackRequestCancelled(fpGroup.fps[f]);
				fpGroup.notify();
			}
		}
		fpGroupSet.clear();
		this.notify();
	}
	
	/**
	 * Clear the feedback queue, cancelling all waiting and pending requests and
	 * making any thread waiting on one of the getFeedbackRequest() methods
	 * return (return value will be null). Pending requests will be cancelled.
	 * All waiting invokations of the getFeedback() or getMultiFeedback()
	 * methods will throw a RuntimeException.
	 */
	public synchronized void clear() {
		Set fpGroupSet = new HashSet();
		for (Iterator fpgit = this.waitingRequests.iterator(); fpgit.hasNext();) {
			FeedbackPanel fp = ((FeedbackPanel) fpgit.next());
			FpGroup fpGroup = ((FpGroup) this.waitingRequestGroups.remove(fp));
			fpGroupSet.add(fpGroup);
		}
		this.waitingRequests.clear();
		for (Iterator fpgit = this.pendingRequests.iterator(); fpgit.hasNext();) {
			FeedbackPanel fp = ((FeedbackPanel) fpgit.next());
			FpGroup fpGroup = ((FpGroup) this.waitingRequestGroups.remove(fp));
			fpGroupSet.add(fpGroup);
		}
		this.pendingRequests.clear();
		for (Iterator fpgit = fpGroupSet.iterator(); fpgit.hasNext();) {
			FpGroup fpGroup = ((FpGroup) fpgit.next());
			synchronized(fpGroup) {
				if (DEBUG) System.out.println("- notifying FpGroup lock (" + Thread.currentThread().getId() + ")");
				fpGroup.exception = new RuntimeException("Feedback request cancelled because queue cleared.");
//				for (int f = 0; f < fpGroup.fps.length; f++)
//					this.feedbackRequestCancelled(fpGroup.fps[f]);
				fpGroup.notify();
			}
		}
		fpGroupSet.clear();
		this.notify();
	}
	
	/**
	 * Shutting down the feedback queue is equal to clearing it, using the
	 * clear() method. This implementation actually loops there.
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#shutdown()
	 */
	public void shutdown() {
		this.clear();
	}
	
	/**
	 * Enqueue a feedback request to be displayed to some client. This method
	 * loops through to getMultiFeedback() in order to provide a single entry
	 * point for requests.
	 * @param fp the panel representing the feedback request to answer
	 */
	public void getFeedback(FeedbackPanel fp) {
		FeedbackPanel[] fps = {fp};
		this.getMultiFeedback(fps);
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
		return true;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getPriority()
	 */
	public int getPriority() {
		return 0; // we're a general-purpose feedback service, yield to more specialized ones that might come
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#canGetFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public boolean canGetFeedback(FeedbackPanel fp) {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getMultiFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel[])
	 */
	public void getMultiFeedback(FeedbackPanel[] fps) throws UnsupportedOperationException {
		FpGroup fpGroup = new FpGroup(fps);
		synchronized(this) {
			for (int f = 0; f < fpGroup.fps.length; f++) {
				this.waitingRequests.addLast(fpGroup.fps[f]);
				this.waitingRequestGroups.put(fpGroup.fps[f], fpGroup);
			}
			if (DEBUG) System.out.println("- notifying FP queue (" + Thread.currentThread().getId() + ")");
			this.notify();
		}
		synchronized(fpGroup) {
			try {
				if (DEBUG) System.out.println("- waiting on FpGroup lock (" + Thread.currentThread().getId() + ")");
				long timeout = ((fpGroup.timeout == 0) ? Long.MAX_VALUE : (System.currentTimeMillis() + fpGroup.timeout));
				fpGroup.wait(fpGroup.timeout);
				if (timeout < System.currentTimeMillis()) {
					fpGroup.exception = new RuntimeException("Feedback request timed out after " + fpGroup.timeout + "ms.");
//					for (int f = 0; f < fpGroup.fps.length; f++)
//						this.feedbackRequestTimedOut(fpGroup.fps[f]);
				}
			} catch (InterruptedException ie) {}
			
			if (fpGroup.exception != null)
				throw fpGroup.exception;
		}
	}
	
	private class FpGroup {
		final FeedbackPanel[] fps;
		final boolean[] fpAnswered;
		final long timeout;
		RuntimeException exception = null;
		FpGroup(FeedbackPanel[] fps) {
			this.fps = fps;
			this.fpAnswered = new boolean[this.fps.length];
			Arrays.fill(this.fpAnswered, false);
			long maxTimeout = 0;
			long minTimeout = Long.MAX_VALUE;
			for (int f = 0; f < fps.length; f++) {
				maxTimeout = Math.max(maxTimeout, this.fps[f].getTimeout());
				minTimeout = Math.min(minTimeout, this.fps[f].getTimeout());
			}
			this.timeout = ((minTimeout == 0) ? 0 : maxTimeout);
		}
		void setFpAnswered(FeedbackPanel fp) {
			for (int f = 0; f < this.fps.length; f++)
				if (this.fps[f] == fp)
					this.fpAnswered[f] = true;
		}
		boolean isAnswered() {
			for (int f = 0; f < this.fpAnswered.length; f++)
				if (!this.fpAnswered[f]) return false;
			return true;
		}
	}
	
	/**
	 * Notify the service that some attempt of getting feedback has failed. The
	 * specified feedback panel will be added to the end of the feedback queue
	 * in order to allow for a new answering attempt.
	 * @param fp the panel representing the request that could not be answered
	 */
	public synchronized void cancelFeedbackRequest(FeedbackPanel fp) {
		if (this.pendingRequests.contains(fp)) {
			this.pendingRequests.remove(fp);
			this.waitingRequests.addLast(fp);
			if (DEBUG) System.out.println("- notifying FP queue (" + Thread.currentThread().getId() + ")");
			this.feedbackRequestCancelled(fp);
			this.notify();
		}
	}
	
	/**
	 * Notify the service that a feedback request has been answered. This
	 * notification assumes that the feedback has already been written into the
	 * request panel.
	 * @param fp the panel representing the request that has been answered
	 */
	public synchronized void answerFeedbackRequest(FeedbackPanel fp) {
		if (this.pendingRequests.contains(fp)) {
			this.pendingRequests.remove(fp);
			FpGroup fpGroup = ((FpGroup) this.waitingRequestGroups.remove(fp));
			synchronized(fpGroup) {
				fpGroup.setFpAnswered(fp);
				this.feedbackRequestAnswered(fp);
				if (fpGroup.isAnswered()) {
					if (DEBUG) System.out.println("- notifying FpGroup lock (" + Thread.currentThread().getId() + ")");
					fpGroup.notify();
				}
			}
		}
	}
	
	/**
	 * React to a feedback panel timed out. This method is intended for sub
	 * classes to take respective measures, such as logging or notifying
	 * listeners. This default implementation does nothing, sub classes are
	 * welcome to overwrite it as needed.
	 * @param fp the feedback panel that timed out
	 */
	protected void feedbackRequestTimedOut(FeedbackPanel fp) {}

	/**
	 * React to a feedback panel having been. This method is intended for sub
	 * classes to take respective measures, such as logging or notifying
	 * listeners. This default implementation does nothing, sub classes are
	 * welcome to overwrite it as needed.
	 * @param fp the feedback panel that was cancelled
	 */
	protected void feedbackRequestCancelled(FeedbackPanel fp) {}

	/**
	 * React to a feedback panel having been answered. This method is intended
	 * for sub classes to take respective measures, such as logging or notifying
	 * listeners. This default implementation does nothing, sub classes are
	 * welcome to overwrite it as needed.
	 * @param fp the feedback panel that was answered
	 */
	protected void feedbackRequestAnswered(FeedbackPanel fp) {}
}

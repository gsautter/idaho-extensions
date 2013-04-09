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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance;

/**
 * Voting based feedback engine implementation. This engine uses a queue to
 * store requests that have been answered by one or more users, but do not
 * have a complete answer yet. This class uses a basic implementation of
 * that queue, which makes sure that no user is shown a feedback request ha
 * has already answered. Sub classes are wlecome to overwrite the
 * produceFeedbackQueue() method in order to add implementation specific
 * functionality for assigning feedback requests to users.
 * 
 * @author sautter
 */
public abstract class VotingHtmlFeedbackEngine extends HtmlFeedbackEngine {
	
	/**
	 * Constructor (protected to be accessible for sub classes)
	 */
	protected VotingHtmlFeedbackEngine() {
		this.requestQueue = this.produceFeedbackRequestQueue();
	}
	
	/**
	 * Extends the super class implementation to flush the request queue on
	 * system shutdown. Sub classes overwriting this method should make the
	 * super invocation.
	 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#shutdown()
	 */
	protected void shutdown() {
		super.shutdown();
		
		//	cancel all request in queue
		this.requestQueue.flushFeedbackRequestQueue();
	}
	
	private FeedbackRequestQueue requestQueue;
	
	/**
	 * This implementation loops through to the feedback request queue.
	 * @see de.uka.ipd.idaho.gamta.util.feedback.html.VotingHtmlFeedbackEngine.FeedbackRequestQueue#findFeedbackRequests(java.lang.String)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#findFeedbackRequest(java.lang.String)
	 */
	protected FeedbackRequest findFeedbackRequest(String userName) {
		System.out.println("VotingHtmlFeedbackEngine: getting feedback request for " + userName);
		if (userName == null) return null;
		
		//	try to find a request for the specified user
		for (int attempts = 0; attempts < 10; attempts++) {
			
			//	get a request from the queue
			VotingFeedbackRequest fr = this.requestQueue.findFeedbackRequests(userName);
			
			//	nothing found in queue, retrying is no use
			if (fr == null) {
				System.out.println("VotingHtmlFeedbackEngine: feedback request for " + userName + " not found");
				return null;
			}
			
			//	check if answered (contract of the interface prevents it, but you never know for sure ...)
			else {
				System.out.println("VotingHtmlFeedbackEngine: got feedback request for " + userName + ": " + fr.fp.getClass().getName());
				if (!fr.wasAnsweredBy(userName)) {
					System.out.println(" - unaswered ==> usable");
					return fr;
				}
				else {
					System.out.println(" - already answered, though");
					this.requestQueue.returnFeedbackRequest(fr, false);
				}
			}
		}
		
		//	nothing found, despite all efforts
		return null;
	}
	
	/**
	 * This implementation checks if the argument feedback request is
	 * completely answered, i.e., if there is a sufficient majority for all
	 * answer parameters. If so, it hands the request back to the backing
	 * source as answered, via the HtmlFeedbackServlet.answerFeedbackPanel()
	 * method. Otherwise, the feedback request goes back into the request
	 * queue to await an additional round of votes from another user.
	 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#returnFeedbackRequest(de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest, boolean)
	 */
	protected void returnFeedbackRequest(FeedbackRequest fr, boolean wasAnswered) {
		this.requestQueue.returnFeedbackRequest(((VotingFeedbackRequest) fr), wasAnswered);
	}
	
	/**
	 * Queue for feedback requests. Implementations should be synchronized
	 * in some way, since concurrent access is likely in a servlet.
	 * 
	 * @author sautter
	 */
	protected static abstract class FeedbackRequestQueue {
		
		/**
		 * the voting based feedback engine the feedback request queue belongs
		 * to
		 */
		protected VotingHtmlFeedbackEngine hostEngine;
		
		/**
		 * Constructor
		 * @param hostEngine the request queue's host feedback engine
		 */
		protected FeedbackRequestQueue(VotingHtmlFeedbackEngine hostEngine) {
			this.hostEngine = hostEngine;
		}
		
		/**
		 * Take back a feedback request returning from the front end. This
		 * method hands a feedback request back to implementation specific code.
		 * The boolean argument indicates whether or not the feedback request
		 * has been answered in the front-end. This argument is false for both
		 * timeouts and actual front-end cancellations. The runtime type of the
		 * argument feedback request is equal to that of the feedback requests
		 * retuned by the findFeedbackRequest() method, since the front-end
		 * never produces feedback requests, but only handles the ones it gets
		 * from the feedback engine.
		 * @param fr the feedback request that was answered
		 * @param wasAnswered was the feedback request answered in the
		 *            surrounding feedback engine?
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine#returnFeedbackRequest(de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest, boolean)
		 */
		protected void returnFeedbackRequest(VotingFeedbackRequest fr, boolean wasAnswered) {
			
			//	is this request finished?
			if (wasAnswered && this.isAnswered(fr) && fr.isAnswered()) {
				
				//	hand back to backing source
				this.returnFeedbackRequest(fr);
			}
			
			//	enqueue for other users to answer
			else this.enqueueFeedbackRequest(fr);
		}
		
		/**
		 * Determine if a feedback request is completely answered. This basic
		 * implementation simply loops through to the argument request's
		 * isAnswered() method. Sub classes are welcome to overwrite it as
		 * needed. The returnFeedbackRequest() method consults this method
		 * before checking the feedback request's own isAnswered() method. Thus,
		 * alterantive implementations of this method have the chance to
		 * manipulate the argument feedback request in am implementation
		 * specific way so its own isAnswered() method returns true after the
		 * manipulation. This is intended for applications like, for instance,
		 * giving higher weight to answers from expert users.
		 * @param fr the feedback request to check
		 * @return true if the specified feedback request is completely
		 *         answered, false otherwise
		 */
		protected boolean isAnswered(VotingFeedbackRequest fr) {
			return fr.isAnswered();
		}
		
		/**
		 * Store a feedback request in the queue to be retrievable through the
		 * findFeedbackRequest() method. The surrounding feedback engine uses
		 * this method to put a feedback request returning from the front-end
		 * back in the queue. This happens (a) after it was answered by a user,
		 * but has no complete answer yet, (b) after a user cancelled the it, or
		 * (c) after it timed out.
		 * @param fr the feedback request to enqueue
		 */
		public abstract void enqueueFeedbackRequest(VotingFeedbackRequest fr);
		
		/**
		 * Return a feedback request to the backing source. The surrounding
		 * feedback engine uses this method to return a feedback request to the
		 * backing source after its answer is complete.
		 * @param fr the feedback request to return
		 */
		public abstract void returnFeedbackRequest(VotingFeedbackRequest fr);
		
		/**
		 * Find a feedback request answerable by a specific user. If this method
		 * returns a feedback request already answered by the specified user, it
		 * will be returned to the end of the queue. To avoid shifting around
		 * too much data, implementations should make sure the specified user
		 * has not answered the returned request so far (though the invocing
		 * code will enforce this anyway). If there is no feedback request the
		 * specified user can answer found in the queue, implementations should
		 * fetch new requests from the backing source.
		 * @param userName the user to find a feedback request for
		 * @return a feedback request for the specified user to answer
		 */
		public abstract VotingFeedbackRequest findFeedbackRequests(String userName);
		
		/**
		 * Flush the feedback queue, i.e., return all feedback requests in
		 * the queue to the backing source. This method is invoced when the
		 * surrounding feedback engine shuts down.
		 */
		public abstract void flushFeedbackRequestQueue();
	}
	
	/**
	 * Produce a request queue for the feedback engine. This factory method
	 * exists so sub classes can provide their specific implementation of
	 * feedback request queue.
	 * @return the HtmlFeedbackEngine to use in the servlet
	 */
	protected abstract FeedbackRequestQueue produceFeedbackRequestQueue();
	
	/**
	 * Vote based feedback request implementation. This class works with the
	 * MINIMUM_REDUNDANCY_PROPERTY_NAME of the FeedbackPanel class to
	 * determine the minimum majority for answer parameter values. It also
	 * computes the actual number of correct answers for each user soon as
	 * there is a majority-backed value for each answer parameter.
	 * 
	 * @author sautter
	 */
	public static class VotingFeedbackRequest extends FeedbackRequest {
		
		private ParameterValueAggregator answerData;
		private Map correctAnswerCounts = null;
		
		/**
		 * the vote based feedback engine that issued the feedback request
		 */
		protected VotingHtmlFeedbackEngine votingHostEngine;
		
		/**
		 * Constructor
		 * @param fp the feedback panel representing the actual request from the
		 *            backing source
		 * @param fpRenderer the HTML renderer for the feedback panel
		 * @param hostEngine the feedback engine issuing the feedback request
		 */
		public VotingFeedbackRequest(FeedbackPanel fp, FeedbackPanelHtmlRendererInstance fpRenderer, VotingHtmlFeedbackEngine hostEngine) {
			super(fp, fpRenderer, hostEngine);
			this.votingHostEngine = hostEngine;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#addAnswerData(java.util.Properties, java.lang.String)
		 */
		protected void addAnswerData(Properties answerData, String userName) {
			
			//	create answer data collector on demand
			if (this.answerData == null) {
				int redundancy = this.computeAnswerRedundancy();
				System.out.println("VotingFeedbackRequest: computed answer redundancy as " + redundancy);
				this.answerData = new ParameterValueAggregator(redundancy);
			}
			
			//	store answer data
			this.answerData.addParameterValues(answerData, userName);
			System.out.println("VotingFeedbackRequest: answer data added, now answered by " + this.getAnsweringUsers().length + " users");
			
			//	if answer is complete, count correct decisions for each user
			if (this.answerData.isComplete()) {
				System.out.println("VotingFeedbackRequest: answer complete");
				
				//	line up users
				String[] answerUsers = this.getAnsweringUsers();
				
				//	prepare counting array
				int[] correctAnswerCounts = new int[answerUsers.length];
				Arrays.fill(correctAnswerCounts, 0);
				
				//	count correct answers per user
				String[] answerParamNames = this.answerData.getParameterNames();
				for (int p = 0; p < answerParamNames.length; p++) {
					String answerParamValue = this.answerData.getParameterValue(answerParamNames[p]);
					Set correctAnswerUserSet = new HashSet(Arrays.asList(this.answerData.getVoteUsers(answerParamNames[p], answerParamValue)));
					for (int u = 0; u < answerUsers.length; u++) {
						if (correctAnswerUserSet.contains(answerUsers[u]))
							correctAnswerCounts[u]++;
					}
				}
				
				//	store correct answer counts
				this.correctAnswerCounts = new HashMap();
				for (int u = 0; u < answerUsers.length; u++)
					this.correctAnswerCounts.put(answerUsers[u], new Integer(correctAnswerCounts[u]));
			}
		}
		
		/**
		 * Compute the redundancy level required for the feedback request, i.e.,
		 * the minimum number of users who have to agree on a value for an
		 * answer parameter before that value is assumed to be correct. This
		 * basic implementation uses the value of
		 * MINIMUM_REDUNDANCY_PROPERTY_NAME of the enclosed feedback panel. Sub
		 * classes are welcome to overwrite this method and use a different
		 * approach.
		 * @return the answer redundancy for the feedback request
		 */
		protected int computeAnswerRedundancy() {
			
			//	get request complexity
			int fpComplexity = this.getDecisionComplexity();
			if (fpComplexity < 1) fpComplexity = 1;
			
			//	how many agreeing users do we require?
			int minVotes = this.votingHostEngine.getMinVoteCount();
			try {
				minVotes = Integer.parseInt(fp.getProperty(FeedbackPanel.MINIMUM_REDUNDANCY_PROPERTY_NAME, ("" + minVotes)));
			}
			catch (NumberFormatException nfe) {}
			if (minVotes < 1) minVotes = 1;
			
			//	how many agreeing users can we reach in the worst case?
			int maxMinVotes = ((this.votingHostEngine.getMaxVoteCount() + (fpComplexity - 1)) / fpComplexity);
			return Math.min(minVotes, maxMinVotes);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#isAnswered()
		 */
		public boolean isAnswered() {
			return ((this.answerData != null) && this.answerData.isComplete());
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#getAnswerData()
		 */
		protected Properties getAnswerData() {
			return ((this.answerData == null) ? null : this.answerData.toProperties());
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackEngine.FeedbackRequest#getAnswerCount()
		 */
		public int getAnswerCount() {
			return ((this.answerData == null) ? 0 : this.answerData.size());
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
	
	/**
	 * Obtain the minimum number of users who have to agree on a specific value
	 * for a parameter to actually assign that value to the parameter. This
	 * implementation returns 1, so every feedback request is answered with the
	 * first submitted answer. Sub classes are welcome to overwrite this method
	 * to return their own minimum of votes.
	 * @return the minimum number of users who have to agree on a specific value
	 *         for a parameter to actually assign that value to the parameter
	 */
	protected int getMinVoteCount() {
		return 1;
	}
	
	/**
	 * Obtain the maximum number of users who can vote on the value of a
	 * parameter, i.e., the number of users in the system. This implementation
	 * returns (Integer.MAX_VALUE / 2) (half the max in order to prevent
	 * computation overflows), so practically does not impose any limit. Sub
	 * classes are welcome to overwrite this method to return their own maximum
	 * possible number of votes.
	 * @return the maximum number of users who can vote on the value of a
	 *         parameter
	 */
	protected int getMaxVoteCount() {
		return (Integer.MAX_VALUE / 2);
	}
}

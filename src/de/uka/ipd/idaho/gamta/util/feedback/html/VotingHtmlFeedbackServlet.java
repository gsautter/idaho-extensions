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

import java.util.Iterator;
import java.util.LinkedList;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance;



/**
 * Voting based implementation of showing feedback requests to users in HTML
 * pages. This implementation changes the feedback engine and feedback request
 * implementations in a way that shows any given feedback request to multiple
 * users, until a majority for a response is found. It implementation still
 * leaves the source of the actual feedback panels abstract. Its purpose is to
 * provide a voting-based implementation for the HTML front-end.
 * 
 * @author sautter
 */
public abstract class VotingHtmlFeedbackServlet extends HtmlFeedbackServlet {
	
	/**
	 * Overwrites the default implementation in order to return a
	 * VotingHtmlFeedbackEngine. The returned engine's request queue directly
	 * uses the HtmlFeedbackServlet.getFeedbackPanel() for retrieving feedback
	 * panels from the backing source, the
	 * HtmlFeedbackServlet.cancelFeedbackPanel() to hand them back in case no
	 * renderer can be found for them or the queue is flushed, and the
	 * HtmlFeedbackServlet.answerFeedbackPanel() method to return requests that
	 * have been answered. The getMinVoteCount() and getMaxVoteCount() default
	 * to the respective methods of the surrounding servlet. This is in order to
	 * allow for changing the vote limits a stimm use the default feedback
	 * engine.
	 * @see de.uka.ipd.idaho.gamta.util.feedback.html.HtmlFeedbackServlet#produceFeedbackEngine()
	 */
	protected HtmlFeedbackEngine produceFeedbackEngine() {
		return new VotingHtmlFeedbackEngine() {
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.gamta.util.feedback.html.VotingHtmlFeedbackEngine#produceFeedbackRequestQueue()
			 */
			protected FeedbackRequestQueue produceFeedbackRequestQueue() {
				return new FeedbackRequestQueue(this) {
					private LinkedList requestQueue = new LinkedList();
					public void enqueueFeedbackRequest(VotingFeedbackRequest fr) {
						synchronized (this.requestQueue) {
							this.requestQueue.addLast(fr);
						}
					}
					public VotingFeedbackRequest findFeedbackRequests(String userName) {
						System.out.println("VotingHtmlFeedbackEngine.FeedbackRequestQueue: getting feedback request for " + userName);
						if (userName == null) return null;
						
						//	first, check if there is a request in the queue for the given user
						VotingFeedbackRequest fr = this.searchFeedbackRequestQueue(userName);
						
						//	if there is none, get one from the backing source
						if (fr == null) {
							System.out.println("VotingHtmlFeedbackEngine.FeedbackRequestQueue: feedback request for " + userName + " not found in queue, getting one from backing source");
							fr = this.produceFeedbackRequest(userName);
						}
						if (fr == null)
							System.out.println("VotingHtmlFeedbackEngine.FeedbackRequestQueue: feedback request for " + userName + " not found");
						else System.out.println("VotingHtmlFeedbackEngine.FeedbackRequestQueue: got feedback request for " + userName + ": " + fr.fp.getClass().getName());
						
						//	return whatever was found
						return fr;
					}
					private VotingFeedbackRequest searchFeedbackRequestQueue(String userName) {
						if (this.requestQueue.isEmpty()) return null;
						
						VotingFeedbackRequest fr = null;
						LinkedList cancelledFrs = new LinkedList(); // used for tracking request already cancelled by user
						synchronized (this.requestQueue) {
							for (Iterator rit = this.requestQueue.iterator(); (fr == null) && rit.hasNext();) {
								
								//	retrieve first request
								fr = ((VotingFeedbackRequest) rit.next());
								
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
								fr = ((VotingFeedbackRequest) cancelledFrs.removeFirst());
							
							//	put back remainder of fallback list
							this.requestQueue.addAll(cancelledFrs);
						}
						
						//	return wharever was found
						return fr;
					}
					private VotingFeedbackRequest produceFeedbackRequest(String userName) {
						System.out.println("VotingHtmlFeedbackEngine.FeedbackRequestQueue: producing feedback request for " + userName + " from backing source");
						
						//	get (at most 10) new requests until one fits requesting user TODO: make limit a config parameter
						for (int attempt = 0; attempt < 10; attempt++) {
							
							//	get feedback panel
							FeedbackPanel fp = VotingHtmlFeedbackServlet.this.getFeedbackPanel();
							
							//	no request pending (won't change with further attempts)
							if (fp == null) {
								System.out.println("VotingHtmlFeedbackEngine.FeedbackRequestQueue: feedback request for " + userName + " not found in backing source");
								return null;
							}
							else System.out.println("VotingHtmlFeedbackEngine.FeedbackRequestQueue: got feedback request for " + userName + " from backing source: " + fp.getClass().getName());
							
							//	obtain renderer
							FeedbackPanelHtmlRendererInstance fpRenderer = FeedbackPanelHtmlRenderer.getRenderer(fp);
							
							//	renderer not found, handle back to backing source
							if (fpRenderer == null) {
								System.out.println(" - renderer not found ==> unusable");
								VotingHtmlFeedbackServlet.this.cancelFeedbackPanel(fp);
							}
							
							//	got request
							else {
								System.out.println(" - renderer found, returning feedback request");
								return new VotingFeedbackRequest(fp, fpRenderer, this.hostEngine);
							}
						}
						
						//	no request found for user, despite all efforts
						return null;
					}
					public void flushFeedbackRequestQueue() {
						synchronized (this.requestQueue) {
							while (!this.requestQueue.isEmpty())
								VotingHtmlFeedbackServlet.this.cancelFeedbackPanel(((FeedbackRequest) this.requestQueue.removeFirst()).fp);
						}
					}
					public void returnFeedbackRequest(VotingFeedbackRequest fr) {
						VotingHtmlFeedbackServlet.this.answerFeedbackPanel(fr.fp);
					}
				};
			}
			protected int getMinVoteCount() {
				return VotingHtmlFeedbackServlet.this.getMinVoteCount();
			}
			protected int getMaxVoteCount() {
				return VotingHtmlFeedbackServlet.this.getMaxVoteCount();
			}
		};
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

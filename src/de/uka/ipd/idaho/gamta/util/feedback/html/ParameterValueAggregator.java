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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The ParameterValueAggregator is derived from a HashMap and supports users
 * voting on the value a given key is mapped to. Simple majority is decisive,
 * null values are possible.
 * 
 * @author sautter
 */
public class ParameterValueAggregator {
	
	/**
	 * the constant return value for the getParameterValue() methods indication
	 * that the requested parameter's value has not been decided on so far,
	 * i.e., no specific value received the required number of votes
	 */
	public static final String AMBIGUOUS = "A_M_B_I_G_U_O_U_S";
	
	private int voteThreshold;
	
	private Map parameterNamesToValueSets = new TreeMap();
	private Set voteUserNames = new TreeSet();
	
	/**
	 * Constructor
	 * @param voteThreshold the minimum number of votes a value has to have to
	 *            be considered the actual value of a parameter
	 */
	public ParameterValueAggregator(int voteThreshold) {
		this.voteThreshold = voteThreshold;
	}
	
	private class ParameterValues {
	    Map valuesToSupportUserSets = new HashMap();
	    Set nonNullVoteUserNames = new HashSet();
		int maxSupport = 0;
		String maxSupportValue = null;
		
		void addValue(String value, String userName) {
			if (value == null) return;
			
			Set valueSupportUserSet = ((Set) this.valuesToSupportUserSets.get(value));
			if (valueSupportUserSet == null) {
				valueSupportUserSet = new HashSet();
				this.valuesToSupportUserSets.put(value, valueSupportUserSet);
			}
			
			this.nonNullVoteUserNames.add(userName);
			valueSupportUserSet.add(userName);
			
		    if (valueSupportUserSet.size() > this.maxSupport) {
		    	this.maxSupport = valueSupportUserSet.size();
		    	this.maxSupportValue = value;
		    }
		}
		
		boolean isComplete() {
			if ((voteUserNames.size() - this.nonNullVoteUserNames.size()) >= voteThreshold)
				return true;
			else if (this.maxSupport >= voteThreshold)
				return true;
			else return false;
		}
		
		String getValue() {
			if ((voteUserNames.size() - this.nonNullVoteUserNames.size()) >= voteThreshold)
				return null;
			else if (this.maxSupport >= voteThreshold)
				return this.maxSupportValue;
			else return AMBIGUOUS;
		}
	}
	
	/**
	 * Test whether the parameter Map is complete, i.e. if all parameters
	 * present in the Map have a value assigned to them that the required number
	 * of users have voted for.
	 * @return true if all parameters in the Map have a value a majority of
	 *         users agrees on
	 */
	public boolean isComplete() {
		for (Iterator pit = this.parameterNamesToValueSets.values().iterator(); pit.hasNext();)
			if (!((ParameterValues) pit.next()).isComplete()) return false;
		return true;
	}

	/**
	 * Test whether a given parameter is complete, i.e. if it has a value
	 * assigned to it that the required number of users has voted for.
	 * @param name the name of the parameter to check
	 * @return true if the parameter with the specified name has a value a
	 *         majority of users agrees on
	 */
	public boolean isComplete(String name) {
		ParameterValues pv = ((ParameterValues) this.parameterNamesToValueSets.get(name));
		if (pv == null) return (this.voteUserNames.size() >= this.voteThreshold);
		else return pv.isComplete();
	}
	
	/**
	 * Retrieve the names of the parameters that have a non-null value assigned
	 * to them a sufficient majority of users has voted for
	 * @return the names of the parameters with a definite non-null value
	 */
	public String[] getParameterNames() {
		TreeSet names = new TreeSet();
		for (Iterator pit = this.parameterNamesToValueSets.keySet().iterator(); pit.hasNext();) {
			String name = ((String) pit.next());
			ParameterValues pv = ((ParameterValues) this.parameterNamesToValueSets.get(name));
			if ((pv != null) && pv.isComplete() && (pv.getValue() != null))
				names.add(name);
		}
		return ((String[]) names.toArray(new String[names.size()]));
	}
	
//	public void addParameterValue(String name, String value, String userName) {
//		ParameterValues pv = ((ParameterValues) this.parameterNamesToValueSets.get(name));
//		if (pv == null) {
//			pv = new ParameterValues();
//			this.parameterNamesToValueSets.put(name, pv);
//		}
//		pv.addValue(value, userName);
//		this.voteUserNames.add(userName);
//	} // !!! we need the answers en block, otherwise ther might be problems with the 'null' votes ==> This method's private and only for testing !!!
	
	private void addParameterValue(String name, String value, String userName) {
		ParameterValues pv = ((ParameterValues) this.parameterNamesToValueSets.get(name));
		if (pv == null) {
			pv = new ParameterValues();
			this.parameterNamesToValueSets.put(name, pv);
		}
		pv.addValue(value, userName);
		this.voteUserNames.add(userName);
	}
	
	/**
	 * Add the votes of a single user. The parameter/value pairs the user votes
	 * for are represented as a Properties object. Parameters not mapped to any
	 * value are considered as votes for those parameters to be null.
	 * @param data the parameter/value pairs the user votes for
	 * @param userName the name of the voting user
	 */
	public void addParameterValues(Properties data, String userName) {
		for (Iterator pit = data.keySet().iterator(); pit.hasNext();) {
			String name = ((String) pit.next());
			ParameterValues pv = ((ParameterValues) this.parameterNamesToValueSets.get(name));
			if (pv == null) {
				pv = new ParameterValues();
				this.parameterNamesToValueSets.put(name, pv);
			}
			pv.addValue(data.getProperty(name), userName);
		}
		this.voteUserNames.add(userName);
	}
	
	/**
	 * Retrieve the value of a given parameter. If there is no value for the
	 * specified parameter so far, this method returns the AMBIGUOUS constant,
	 * which can be checked using '==' against the constant of this class. This
	 * is in order to facilitate telling a parameter with a 'null' value agreed
	 * to by a sufficient majority from a parameter that does not have a
	 * definite value so far.
	 * @param name the name of the parameter
	 * @return the value of the parameter with the specified name (which can be
	 *         null), or the AMBIGUOUS constant, if a value for the specified
	 *         parameter has not been agreed on so far
	 */
	public String getParameterValue(String name) {
		ParameterValues pv = ((ParameterValues) this.parameterNamesToValueSets.get(name));
		return ((pv == null) ? null : pv.getValue());
	}
	
	/**
	 * Retrieve the names of the users who voted for a given parameter to have a
	 * given value
	 * @param name the name of the parameter
	 * @param value the value in question
	 * @return an array holding the names of the users who voted for the
	 *         specified parameter to have the specified value
	 */
	public String[] getVoteUsers(String name, String value) {
		ParameterValues pv = ((ParameterValues) this.parameterNamesToValueSets.get(name));
		if (pv == null)
			return ((value == null) ? ((String[]) this.voteUserNames.toArray(new String[this.voteUserNames.size()])) : new String[0]);
		else if (value == null) {
			Set nullVoteUserNames = new TreeSet(this.voteUserNames);
			nullVoteUserNames.removeAll(pv.nonNullVoteUserNames);
			return ((String[]) nullVoteUserNames.toArray(new String[nullVoteUserNames.size()]));
		}
		else {
			Set valueVoteUserNames = ((Set) pv.valuesToSupportUserSets.get(value));
			return ((valueVoteUserNames == null) ? new String[0] : ((String[]) valueVoteUserNames.toArray(new String[valueVoteUserNames.size()])));
		}
	}
	
	/**
	 * Obtain a Properties object representing the current state of this Map.
	 * Parameters with no value assigned to them by a sufficient majority of
	 * users are ignored, as are parameters with a null value. The returned
	 * Properties contains a snapshot of the current state of this Map. Future
	 * changes to the Map will not reflect in the returned Properties object.
	 * @return a Properties object containing the parameter/value pairs
	 *         currently contained in this Map
	 */
	public Properties toProperties() {
		Properties properties = new Properties();
		for (Iterator pit = this.parameterNamesToValueSets.keySet().iterator(); pit.hasNext();) {
			String name = ((String) pit.next());
			ParameterValues pv = ((ParameterValues) this.parameterNamesToValueSets.get(name));
			if ((pv != null) && pv.isComplete()) {
				String value = pv.getValue();
				if (value != null)
					properties.setProperty(name, value);
			}
		}
		return properties;
	}
	
	/**
	 * Retrieve the size of the parameter value aggregator, i.e. the number of
	 * parameters contained, regardless of the number of actual values.
	 * @return the size of the parameter value aggregator
	 */
	public int size() {
		return this.parameterNamesToValueSets.size();
	}
	
//	/**
//	 * Obtain a Properties object as a wrapper for this Map. The Properties acts
//	 * as if parameters with no value assigned to them by a sufficient majority
//	 * of users are not present, as it does for parameters with a null value.
//	 * The returned Properties is a wrapper for this Map. Future changes to the
//	 * Map will reflect in the returned Properties object, but changes to the
//	 * Properties object result in exceptions being thrown.
//	 * @return a Properties object acting as a wrapper for this Map
//	 * @see de.uka.ipd.idaho.gamta.util.feedback.html.ParameterValueAggregator#toProperties()
//	 */
//	public Properties asProperties() {
//		return new Properties() {
//			//	TO DO: implement this
//		};
//	}
	
	//	main method for test purposes
	public static void main(String[] args) {
		ParameterValueAggregator pva = new ParameterValueAggregator(2);
		pva.addParameterValue("p1", "v1.1", "u1");
		System.out.println(pva.isComplete());
		pva.addParameterValue("p1", "v1.2", "u2");
		System.out.println(pva.isComplete());
		pva.addParameterValue("p1", "v1.2", "u3");
		System.out.println(pva.isComplete());
		pva.addParameterValue("p2", "v2.0", "u3");
		System.out.println(pva.isComplete());
//		pva.addParameterValue("p2", "v2.1", "u2");
//		System.out.println(pva.isComplete());
		pva.addParameterValue("p2", "v2.0", "u2");
		System.out.println(pva.isComplete());
		
		String[] pns = pva.getParameterNames();
		for (int p = 0; p < pns.length; p++)
			System.out.println(pns[p] + ": " + pva.getParameterValue(pns[p]));
	}
}

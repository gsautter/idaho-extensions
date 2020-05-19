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
package de.uka.ipd.idaho.plugins.bibRefs;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;

/**
 * This class is an abstract adapter for reading reference data sets from
 * external representations, like BibTeX or RIS. This class does not implement
 * transformation of reference data sets into external formats, as the
 * formatting functionality of BibRefUtils can handle this more flexibly.
 * 
 * @author sautter
 */
public abstract class BibRefDataParser implements BibRefConstants {
	
	/**
	 * A reference data set parsed from an external representation, e.g. BibTeX.
	 * This class adds support for (a) keeping the source text the reference
	 * data set was parsed from and (b) for error messages, e.g. regarding
	 * missing elements.
	 * 
	 * @author sautter
	 */
	public static class ParsedRefData extends RefData {
		private String error;
		private String source;
		
		/**
		 * Constructor
		 */
		public ParsedRefData() {}
		
		/**
		 * Add the original input data to the reference data set. This
		 * facilitates, for instance, prompting users for corrections on
		 * erroneous or incomplete records.
		 * @param source the source the reference was parsed from
		 */
		public void setSource(String source) {
			this.source = source;
		}
		
		/**
		 * Retrieve the original source data of the reference data set. This
		 * method only returns a non-null value after the setSource() method has
		 * been invoked.
		 * @return the original source data of the reference
		 */
		public String getSource() {
			return this.source;
		}
		
		/**
		 * Add an error message and the original input data to the reference
		 * data set. This facilitates, for instance, prompting users for
		 * corrections of erroneous or incomplete records. If this method is
		 * used, the attributes are cleared.
		 * @param error the error message informing the user of what is wrong
		 */
		public void setError(String error) {
			this.error = error;
			this.clearAttributes();
		}
		
		/**
		 * Check if this reference data set has an error message attached to it.
		 * This method only returns true value after the setError() method has
		 * been invoked.
		 * @return true if there is an error message, false otherwise
		 */
		public boolean hasError() {
			return (this.error != null);
		}
		
		/**
		 * Retrieve the error message indicating the problem in this reference
		 * data set. This method only returns a non-null value after the
		 * setError() method has been invoked.
		 * @return the error message
		 */
		public String getError() {
			return this.error;
		}
	}
	
	/**
	 * Iterator over reference data sets extracted from a source during parsing.
	 * 
	 * @author sautter
	 */
	public static interface ParsedRefDataIterator {
		
		/**
		 * Retrieve an estimate of the number of reference data sets yet to
		 * come. This method is to at least vaguely gauge progress with data
		 * formats that read the entire input data before returning any
		 * reference data sets. If an estimate is not available, e.g. in data
		 * formats that really stream their input, this method should return -1.
		 * @return an estimate of the number of reference data sets yet to come.
		 */
		public abstract int estimateRemaining();
		
		/**
		 * @return the next reference data set
		 */
		public abstract ParsedRefData nextRefData() throws IOException;
		
		/**
		 * @return true if there are more reference data sets to retrieve, false
		 *         otherwise
		 */
		public abstract boolean hasNextRefData() throws IOException;
	}
	
	
	/**
	 * The data format name. Should be brief, for longer format names the common
	 * acronym rather than the full name, e.g. 'DC' instead of 'DublinCore'.
	 */
	public final String name;
	
	/**
	 * The data provider to load data from.
	 */
	protected AnalyzerDataProvider dataProvider;
	
	/**
	 * Constructor. Concrete sub classes should provide a public no-argument
	 * constructor to facilitate class loading. They should invoke this
	 * constructor with a hard-coded argument that is strictly alphanumeric and
	 * does not include whitespace.
	 * @param name the data format name
	 */
	protected BibRefDataParser(String name) {
		this.name = name;
	}
	
	/**
	 * Constructor. Concrete sub classes can provide a constructor looping
	 * through to this one with a hard-coded value for the name argument to
	 * facilitate explicit instantiation of the individual sub class, i.e., not
	 * by means of class loading. This constructor sets the data provider.
	 * @param name the data format name
	 * @param adp the data provider
	 */
	protected BibRefDataParser(String name, AnalyzerDataProvider adp) {
		this.name = name;
		this.setDataProvider(adp);
	}
	
	/**
	 * Constructor. Concrete sub classes can provide a constructor looping
	 * through to this one with a hard-coded value for the name argument to
	 * facilitate explicit instantiation of the individual sub class, i.e., not
	 * by means of class loading. This constructor sets the data provider to one
	 * referring to the argument data folder.
	 * @param name the data format name
	 * @param dataFolder the data folder
	 */
	protected BibRefDataParser(String name, File dataFolder) {
		this.name = name;
		this.setDataProvider(new AnalyzerDataProviderFileBased(dataFolder));
	}
	
	/**
	 * Make the reference data format know where its data is stored. This method
	 * should only be called by code instantiating reference data formats.
	 * @param adp the data provider
	 */
	public void setDataProvider(AnalyzerDataProvider adp) {
		this.dataProvider = adp;
		
		//	initialize sub class
		this.init();
	}
	
	/**
	 * Initialize the data format. This method is invoked immediately after the
	 * data provider is set. That means sub classes can assume the data provider
	 * not to be null. This convenience implementation does nothing, sub classes
	 * are welcome to overwrite it as needed.
	 */
	protected void init() {}
	
	/**
	 * Shut down the data format. This method should be invoked by client code
	 * immediately before discarding the data format to facilitate
	 * implementation specific cleanup. Invoking this method might render
	 * individual data formats useless, depending on the actual implementation.
	 * Thus client code should not use a data format after invoking this method.
	 * This convenience implementation does nothing, sub classes are welcome to
	 * overwrite it as needed.
	 */
	public void exit() {}
	
	/**
	 * Retrieve a label for the data format, e.g. for use in drop-down selector
	 * fields. The returned string should not contain any line breaks or HTML
	 * formatting.
	 * @return a label for the data format
	 */
	public abstract String getLabel();
	
	/**
	 * Retrieve a brief description of the data format, e.g. for explanation in
	 * a user interface. The returned string should not contain any line breaks,
	 * but may include HTML formatting. If HTML formatting is used, the returned
	 * string has to start with '&lt;html&gt;' to indicate so. This default
	 * implementation simply returns the label provided by the getLabel()
	 * method. Sub classes are recommended to overwrite this method to provide a
	 * more comprehensive description.
	 * @return a description of the data format
	 */
	public String getDescription() {
		return this.getLabel();
	}
	
	/**
	 * Parse reference data sets from the character stream provided by an input
	 * reader.
	 * @param in the reader to read from
	 * @return an array of reference data objects
	 * @throws IOException
	 */
	public ParsedRefData[] parse(Reader in) throws IOException {
		ArrayList refs = new ArrayList();
		for (ParsedRefDataIterator rit = this.streamParse(in); rit.hasNextRefData();)
			refs.add(rit.nextRefData());
		return ((ParsedRefData[]) refs.toArray(new ParsedRefData[refs.size()]));
	}
	
	/**
	 * Parse reference data sets one by one from the character stream provided by
	 * an input reader.
	 * @param in the reader to read from
	 * @return an iterator over reference data objects
	 * @throws IOException
	 */
	public abstract ParsedRefDataIterator streamParse(Reader in) throws IOException;	
}
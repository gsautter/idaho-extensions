///*
// * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *     * Redistributions of source code must retain the above copyright
// *       notice, this list of conditions and the following disclaimer.
// *     * Redistributions in binary form must reproduce the above copyright
// *       notice, this list of conditions and the following disclaimer in the
// *       documentation and/or other materials provided with the distribution.
// *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
// *       names of its contributors may be used to endorse or promote products
// *       derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
// * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
// * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uka.ipd.idaho.gamta.util.errorLogging;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.OutputStreamWriter;
//import java.io.Reader;
//import java.io.Writer;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.Iterator;
//import java.util.TreeMap;
//import java.util.TreeSet;
//
//import de.uka.ipd.idaho.gamta.Attributed;
//
///**
// * A container for collecting errors found by automated means in some document.
// * 
// * @author sautter
// */
//public abstract class DocumentErrorProtocol {
//	
//	/**
//	 * Abstract recorder of document error protocols.
//	 * 
//	 * @author sautter
//	 */
//	public static interface Recorder {
//		
//		/**
//		 * Obtain an error protocol for a document. If the recorder cannot
//		 * provide an error protocol for the argument document, it has to
//		 * return <code>null</code>.
//		 * @param doc the document to obtain the error protocol for
//		 * @return the error protocol for the argument document
//		 */
//		public abstract DocumentErrorProtocol getErrorProtocolFor(Attributed doc);
//		
//		/**
//		 * Check the priority of the recorder for a given document. This helps
//		 * picking the appropriate one in scenarios with multiple recorders
//		 * aimed at different levels of generality in a data model type
//		 * hierarchy. If the recorder cannot provide an error protocol for the
//		 * argument document, it has to return <code>-1</code>.
//		 * @param doc the document to check the priority for
//		 * @return the priority
//		 */
//		public abstract int getPriority(Attributed doc);
//	}
//	
//	private static ArrayList recorders = new ArrayList(2);
//	
//	/**
//	 * Add a document error recorder.
//	 * @param depr the recorder to add
//	 */
//	public static void addRecorder(Recorder depr) {
//		if ((depr != null) && !recorders.contains(depr))
//			recorders.add(depr);
//	}
//	
//	/**
//	 * Remove a document error recorder.
//	 * @param depr the recorder to remove
//	 */
//	public static void removeRecorder(Recorder depr) {
//		recorders.remove(depr);
//	}
//	
//	/**
//	 * Obtain an error protocol for a document. If none of the registered
//	 * recorder can provide an error protocol for the argument document, this
//	 * method returns <code>null</code>.
//	 * @param doc the document to obtain the error protocol for
//	 * @return the error protocol for the argument document
//	 */
//	public static DocumentErrorProtocol getErrorProtocol(Attributed doc) {
//		int depPriority = 0;
//		DocumentErrorProtocol dep = null;
//		for (int r = 0; r < recorders.size(); r++) {
//			int priority = ((Recorder) recorders.get(r)).getPriority(doc);
//			if (priority < depPriority)
//				continue;
//			dep = ((Recorder) recorders.get(r)).getErrorProtocolFor(doc);
//			if (dep != null)
//				depPriority = priority;
//		}
//		return dep;
//	}
//	
//	private TreeMap categoryLabels = null;
//	private TreeMap categoryDescriptions = null;
//	private TreeMap typeLabels = null;
//	private TreeMap typeDescriptions = null;
//	
//	/** default constructor
//	 */
//	public DocumentErrorProtocol() {}
//	
//	/**
//	 * Retrieve all error categories present in the protocol.
//	 * @return an array holding the error categories
//	 */
//	public String[] getErrorCategories() {
//		return ((this.categoryLabels == null) ? new String[0] : ((String[]) this.categoryLabels.keySet().toArray(new String[this.categoryLabels.size()])));
//	}
//	
//	/**
//	 * Add an error category with a human readable label and description
//	 * (mostly for use in a UI).
//	 * @param category the error category
//	 * @param label the label for the argument category
//	 * @param description the description for the argument category
//	 */
//	public void addErrorCategory(String category, String label, String description) {
//		this.setErrorCategoryLabel(category, label);
//		this.setErrorCategoryDescription(category, description);
//	}
//	
//	/**
//	 * Retrieve the human readable label for an error category (mostly for
//	 * use in a UI).
//	 * @param category the error category
//	 * @return the label for the argument category
//	 */
//	public String getErrorCategoryLabel(String category) {
//		return ((this.categoryLabels == null) ? null : ((String) this.categoryLabels.get(category)));
//	}
//	
//	/**
//	 * Provide a human readable label for an error category (mostly for use
//	 * in a UI).
//	 * @param category the error category
//	 * @param label the label for the argument category
//	 */
//	public void setErrorCategoryLabel(String category, String label) {
//		if (label == null) {
//			if (this.categoryLabels != null)
//				this.categoryLabels.remove(category);
//		}
//		else {
//			if (this.categoryLabels == null)
//				this.categoryLabels = new TreeMap(String.CASE_INSENSITIVE_ORDER);
//			this.categoryLabels.put(category, label);
//		}
//	}
//	
//	/**
//	 * Retrieve a human readable description for an error category (mostly
//	 * for use in a UI).
//	 * @param category the error category
//	 * @return description the description for the argument category
//	 */
//	public String getErrorCategoryDescription(String category) {
//		return ((this.categoryDescriptions == null) ? null : ((String) this.categoryDescriptions.get(category)));
//	}
//	
//	/**
//	 * Provide a human readable description for an error category (mostly
//	 * for use in a UI).
//	 * @param category the error category
//	 * @param description the description for the argument category
//	 */
//	public void setErrorCategoryDescription(String category, String description) {
//		if (description == null) {
//			if (this.categoryDescriptions != null)
//				this.categoryDescriptions.remove(category);
//		}
//		else {
//			if (this.categoryDescriptions == null)
//				this.categoryDescriptions = new TreeMap(String.CASE_INSENSITIVE_ORDER);
//			this.categoryDescriptions.put(category, description);
//		}
//	}
//	
//	/**
//	 * Retrieve all error types present in the protocol for a given category.
//	 * @param category the error category
//	 * @return an array holding the error types
//	 */
//	public String[] getErrorTypes(String category) {
//		if (this.typeLabels == null)
//			return new String[0];
//		TreeSet errorTypes = new TreeSet();
//		String categoryPrefix = (category + ".");
//		for (Iterator tit = this.typeLabels.keySet().iterator(); tit.hasNext();) {
//			String ct = ((String) tit.next());
//			if (ct.startsWith(categoryPrefix))
//				errorTypes.add(ct.substring(categoryPrefix.length()));
//		}
//		return ((String[]) errorTypes.toArray(new String[errorTypes.size()]));
//	}
//	
//	/**
//	 * Add an error type with a human readable label and description (mostly
//	 * for use in a UI).
//	 * @param category the error category the argument type belongs to
//	 * @param type the error type
//	 * @param label the label for the argument type
//	 * @param description the description for the argument type
//	 */
//	public void addErrorType(String category, String type, String label, String description) {
//		this.setErrorTypeLabel(category, type, label);
//		this.setErrorTypeDescription(category, type, description);
//	}
//	
//	/**
//	 * Retrieve a human readable label for an error type (mostly for use in
//	 * a UI).
//	 * @param category the error category the argument type belongs to
//	 * @param type the error type
//	 * @return the label for the argument type
//	 */
//	public String getErrorTypeLabel(String category, String type) {
//		return ((this.typeLabels == null) ? null : ((String) this.typeLabels.get(category + "." + type)));
//	}
//	
//	/**
//	 * Provide a human readable label for an error type (mostly for use in a
//	 * UI).
//	 * @param category the error category the argument type belongs to
//	 * @param type the error type
//	 * @param label the label for the argument type
//	 */
//	public void setErrorTypeLabel(String category, String type, String label) {
//		if (label == null) {
//			if (this.typeLabels != null)
//				this.typeLabels.remove(category + "." + type);
//		}
//		else {
//			if ((this.categoryLabels == null) || !this.categoryLabels.containsKey(category))
//				this.addErrorCategory(category, category, category);
//			if (this.typeLabels == null)
//				this.typeLabels = new TreeMap(String.CASE_INSENSITIVE_ORDER);
//			this.typeLabels.put((category + "." + type), label);
//		}
//	}
//	
//	/**
//	 * Retrieve a human readable description for an error type (mostly for
//	 * use in a UI).
//	 * @param category the error category the argument type belongs to
//	 * @param type the error type
//	 * @return the description for the argument type
//	 */
//	public String getErrorTypeDescription(String category, String type) {
//		return ((this.typeDescriptions == null) ? null : ((String) this.typeDescriptions.get(category + "." + type)));
//	}
//	
//	/**
//	 * Provide a human readable description for an error type (mostly for
//	 * use in a UI).
//	 * @param category the error category the argument type belongs to
//	 * @param type the error type
//	 * @param description the description for the argument type
//	 */
//	public void setErrorTypeDescription(String category, String type, String description) {
//		if (description == null) {
//			if (this.typeDescriptions != null)
//				this.typeDescriptions.remove(category + "." + type);
//		}
//		else {
//			if ((this.categoryLabels == null) || !this.categoryLabels.containsKey(category))
//				this.addErrorCategory(category, category, category);
//			if (this.typeDescriptions == null)
//				this.typeDescriptions = new TreeMap(String.CASE_INSENSITIVE_ORDER);
//			this.typeDescriptions.put((category + "." + type), description);
//		}
//	}
//	
//	/**
//	 * Copy all error metadata (categories and types with their respective
//	 * labels and descriptions) from another error protocol, e.g. for
//	 * initialization from a prototype error protocol.
//	 * @param dep the error protocol to copy the metadata from
//	 * @param overwrite overwrite existing entries?
//	 */
//	public void copyErrorMetadata(DocumentErrorProtocol dep, boolean overwrite) {
//		if (dep == this)
//			return;
//		String[] categories = dep.getErrorCategories();
//		for (int c = 0; c < categories.length; c++)
//			this.copyErrorCategoryMetadata(dep, categories[c], overwrite);
//	}
//	
//	/**
//	 * Copy the metadata of a given error category (label, description, and
//	 * types with their respective labels and descriptions) from another
//	 * error protocol, e.g. for initialization from a prototype error protocol.
//	 * @param dep the error protocol to copy the metadata from
//	 * @param category the error category whose metadata to copy
//	 * @param overwrite overwrite existing entries?
//	 */
//	public void copyErrorCategoryMetadata(DocumentErrorProtocol dep, String category, boolean overwrite) {
//		if (dep == this)
//			return;
//		String cLabel = reconcileString(this.getErrorCategoryLabel(category), dep.getErrorCategoryLabel(category), overwrite);
//		String cDescription = reconcileString(this.getErrorCategoryDescription(category), dep.getErrorCategoryDescription(category), overwrite);
//		this.addErrorCategory(category, cLabel, cDescription);
//		String[] types = dep.getErrorTypes(category);
//		for (int t = 0; t < types.length; t++) {
//			String tLabel = reconcileString(this.getErrorTypeLabel(category, types[t]), dep.getErrorTypeLabel(category, types[t]), overwrite);
//			String tDescription = reconcileString(this.getErrorTypeDescription(category, types[t]), dep.getErrorTypeDescription(category, types[t]), overwrite);
//			this.addErrorType(category, types[t], tLabel, tDescription);
//		}
//	}
//	
//	/**
//	 * Copy the metadata of a given error category and type (respective labels
//	 * and descriptions) from another error protocol, e.g. for initialization
//	 * from a prototype error protocol.
//	 * @param dep the error protocol to copy the metadata from
//	 * @param category the error category whose metadata to copy
//	 * @param type the error type whose metadata to copy
//	 * @param overwrite overwrite existing entries?
//	 */
//	public void copyErrorTypeMetadata(DocumentErrorProtocol dep, String category, String type, boolean overwrite) {
//		if (dep == this)
//			return;
//		String cLabel = reconcileString(this.getErrorCategoryLabel(category), dep.getErrorCategoryLabel(category), false);
//		String cDescription = reconcileString(this.getErrorCategoryDescription(category), dep.getErrorCategoryDescription(category), false);
//		this.addErrorCategory(category, cLabel, cDescription);
//		String tLabel = reconcileString(this.getErrorTypeLabel(category, type), dep.getErrorTypeLabel(category, type), overwrite);
//		String tDescription = reconcileString(this.getErrorTypeDescription(category, type), dep.getErrorTypeDescription(category, type), overwrite);
//		this.addErrorType(category, type, tLabel, tDescription);
//	}
//	private static String reconcileString(String own, String other, boolean overwrite) {
//		if (own == null)
//			return other;
//		if (overwrite && (other != null))
//			return other;
//		return own;
//	}
//	
//	/**
//	 * Obtain the subject of an error, mainly on filling the protocol from the
//	 * serialized form of its content. The argument data is the same returned
//	 * by <code>getSubjectData()</code> of an error on serialization.
//	 * @param doc the document the error subject lies in
//	 * @param data the data identifying the error subject
//	 * @return the error subject, or null if it cannot be found
//	 */
//	public abstract Attributed findErrorSubject(Attributed doc, String[] data);
//	
//	/**
//	 * Add an error to the protocol. Any de-duplication efforts are up to
//	 * implementations, if any de-duplication is to happen at all. The source
//	 * argument is intended as a link back to the source of the error, e.g. for
//	 * re-checking purposes; it may be null.
//	 * @param source the name of the error source
//	 * @param subject the object the error pertains to
//	 * @param parent the object the error subject belongs to
//	 * @param category the error category (for grouping)
//	 * @param type the error type (for grouping)
//	 * @param description the detailed error description
//	 * @param severity the severity of the error (one of 'blocker', 'critical', 'major', and 'minor')
//	 */
//	public abstract void addError(String source, Attributed subject, Attributed parent, String category, String type, String description, String severity);
//	
//	/**
//	 * Retrieve the total number of errors in the protocol.
//	 * @return the total number of errors
//	 */
//	public abstract int getErrorCount();
//	
//	/**
//	 * Retrieve the total number of errors of a given severity present in the
//	 * protocol.
//	 * @param severity the error severity
//	 * @return the number of errors with the argument severity
//	 */
//	public abstract int getErrorSeverityCount(String severity);
//	
//	/**
//	 * Retrieve all errors in the protocol.
//	 * @return an array holding the errors
//	 */
//	public abstract Error[] getErrors();
//	
//	/**
//	 * Retrieve the total number of errors of a given category present in the
//	 * protocol.
//	 * @param category the error category
//	 * @return the number of errors
//	 */
//	public abstract int getErrorCount(String category);
//	
//	/**
//	 * Retrieve the total number of errors of a given category and severity
//	 * present in the protocol.
//	 * @param category the error category
//	 * @param severity the error severity
//	 * @return the number of errors with the argument severity
//	 */
//	public abstract int getErrorSeverityCount(String category, String severity);
//	
//	/**
//	 * Retrieve all errors of a given category present in the protocol.
//	 * @param category the error category
//	 * @return the total number of errors
//	 */
//	public abstract Error[] getErrors(String category);
//	
//	/**
//	 * Retrieve the total number of errors of a given category and type present
//	 * in the protocol.
//	 * @param category the error category
//	 * @param type the error type
//	 * @return the number of errors
//	 */
//	public abstract int getErrorCount(String category, String type);
//	
//	/**
//	 * Retrieve the total number of errors of a given category, type, and
//	 * severity present in the protocol.
//	 * @param category the error category
//	 * @param type the error type
//	 * @param severity the error severity
//	 * @return the number of errors with the argument severity
//	 */
//	public abstract int getErrorSeverityCount(String category, String type, String severity);
//	
//	/**
//	 * Retrieve all errors of a given category and type present in the protocol.
//	 * @param category the error category
//	 * @param type the error type
//	 * @return the total number of errors
//	 */
//	public abstract Error[] getErrors(String category, String type);
//	
//	/**
//	 * Remove an error from the protocol. The runtime class of the argument
//	 * error is the one of the errors obtained from the getError() methods.
//	 * @param error the error to remove
//	 */
//	public abstract void removeError(DocumentErrorProtocol.Error error);
//	
//	/**
//	 * Obtain a comparator for sorting the errors in the protocol. The runtime
//	 * class of the errors handed to the compare() method is the one of the
//	 * errors obtained from the getError() methods. If the errors do not have
//	 * a defined sort order, this method must return null.
//	 * @return an error comparator
//	 */
//	public abstract Comparator getErrorComparator();
//	
//	/**
//	 * Object holding the complete information for an individual error in the
//	 * protocol.
//	 * 
//	 * @author sautter
//	 */
//	public static abstract class Error {
//		public static final String SEVERITY_BLOCKER = "blocker";
//		public static final String SEVERITY_CRITICAL = "critical";
//		public static final String SEVERITY_MAJOR = "major";
//		public static final String SEVERITY_MINOR = "minor";
//		
//		/** the name of the error source */
//		public final String source;
//		
//		/** the object the error pertains to */
//		public final Attributed subject;
//		
//		/** the error category (for grouping) */
//		public final String category;
//		
//		/** the error type (for grouping) */
//		public final String type;
//		
//		/** the detailed error description */
//		public final String description;
//		
//		/** the severity of the error (one of 'blocker', 'critical', 'major', and 'minor') */
//		public final String severity;
//		
//		/**
//		 * Constructor
//		 * @param source the name of the error source
//		 * @param subject the object the error pertains to
//		 * @param category the error category (for grouping)
//		 * @param type the error type (for grouping)
//		 * @param description the detailed error description
//		 * @param severity the severity of the error (one of 'blocker', 'critical', 'major', and 'minor')
//		 */
//		protected Error(String source, Attributed subject, String category, String type, String description, String severity) {
//			this.source = source;
//			this.subject = subject;
//			this.category = category;
//			this.type = type;
//			this.description = description;
//			this.severity = checkSeverity(severity);
//		}
//		private static String checkSeverity(String severity) {
//			if (SEVERITY_BLOCKER.equalsIgnoreCase(severity))
//				return SEVERITY_BLOCKER;
//			else if (SEVERITY_CRITICAL.equalsIgnoreCase(severity))
//				return SEVERITY_CRITICAL;
//			else if (SEVERITY_MAJOR.equalsIgnoreCase(severity))
//				return SEVERITY_MAJOR;
//			else if (SEVERITY_MINOR.equalsIgnoreCase(severity))
//				return SEVERITY_MINOR;
//			else return SEVERITY_CRITICAL;
//		}
//		
//		/**
//		 * Return more detailed data for persisting, e.g. data that enables a
//		 * protocol implementation to obtain the error subject on loading. This
//		 * default implementation returns an empty array. Sub classes are
//		 * welcome to overwrite it as needed. This method must not return null,
//		 * nor may any of the elements be null.
//		 * @return an array holding the custom data
//		 */
//		public String[] getSubjectData() {
//			return new String[0];
//		}
//	}
//	
//	/**
//	 * Store an error protocol to a given output stream.
//	 * @param dep the error protocol to store
//	 * @param out the output stream to store the error protocol to
//	 * @throws IOException
//	 */
//	public static void storeErrorProtocol(DocumentErrorProtocol dep, OutputStream out) throws IOException {
//		storeErrorProtocol(dep, new OutputStreamWriter(out, "UTF-8"));
//	}
//	
//	/**
//	 * Store an error protocol to a given output stream.
//	 * @param dep the error protocol to store
//	 * @param out the output stream to store the error protocol to
//	 * @throws IOException
//	 */
//	public static void storeErrorProtocol(DocumentErrorProtocol dep, Writer out) throws IOException {
//		//	TODOnot consider zipping ==> IMF is zipped anyway, and IMD is huge, so ease of access more important
//		
//		//	persist error protocol
//		BufferedWriter epBw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
//		String[] categories = dep.getErrorCategories();
//		for (int c = 0; c < categories.length; c++) {
//			
//			//	store category proper
//			epBw.write("CATEGORY");
//			epBw.write("\t" + categories[c]);
//			epBw.write("\t" + dep.getErrorCategoryLabel(categories[c]));
//			epBw.write("\t" + dep.getErrorCategoryDescription(categories[c]));
//			epBw.newLine();
//			
//			//	store error types in current category
//			String[] types = dep.getErrorTypes(categories[c]);
//			for (int t = 0; t < types.length; t++) {
//				
//				//	store type proper
//				epBw.write("TYPE");
//				epBw.write("\t" + types[t]);
//				epBw.write("\t" + dep.getErrorTypeLabel(categories[c], types[t]));
//				epBw.write("\t" + dep.getErrorTypeDescription(categories[c], types[t]));
//				epBw.newLine();
//				
//				//	store actual errors
//				Error[] errors = dep.getErrors(categories[c], types[t]);
//				for (int e = 0; e < errors.length; e++) {
//					epBw.write("ERROR");
//					epBw.write("\t" + errors[e].severity);
//					epBw.write("\t" + errors[e].description);
//					epBw.write("\t" + errors[e].source);
//					String[] sData = errors[e].getSubjectData();
//					for (int d = 0; d < sData.length; d++)
//						epBw.write("\t" + sData[d]);
//					epBw.newLine();
//				}
//			}
//		}
//		epBw.flush();
//	}
//	
//	/**
//	 * Fill an error protocol with the data provided by a given input stream.
//	 * @param doc the document the error protocol pertains to
//	 * @param dep the error protocol to populate
//	 * @param in the input stream to populate the error protocol from
//	 * @throws IOException
//	 */
//	public static void fillErrorProtocol(DocumentErrorProtocol dep, Attributed doc, InputStream in) throws IOException {
//		fillErrorProtocol(dep, doc, new InputStreamReader(in, "UTF-8"));
//	}
//	
//	/**
//	 * Fill an error protocol with the data provided by a given input stream.
//	 * @param dep the error protocol to populate
//	 * @param doc the document the error protocol refer to
//	 * @param in the input stream to populate the error protocol from
//	 * @throws IOException
//	 */
//	public static void fillErrorProtocol(DocumentErrorProtocol dep, Attributed doc, Reader in) throws IOException {
//		//	TODOnot consider zipping ==> IMF is zipped anyway, and IMD is huge, so ease of access more important
//		
//		//	load error protocol, scoping error categories and types
//		BufferedReader epBr = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
//		String category = "null";
//		String type = "null";
//		for (String line; (line = epBr.readLine()) != null;) {
//			line = line.trim();
//			if (line.length() == 0)
//				continue;
//			
//			//	parse data
//			String[] data = line.split("\\t");
//			if (data.length < 2)
//				continue;
//			
//			//	read error category
//			if ("CATEGORY".equals(data[0])) {
//				category = data[1];
//				String label = getElement(data, 2, category);
//				String description = getElement(data, 3, category);
//				dep.addErrorCategory(category, label, description);
//				continue;
//			}
//			
//			//	read error type
//			if ("TYPE".equals(data[0])) {
//				type = data[1];
//				String label = getElement(data, 2, type);
//				String description = getElement(data, 3, type);
//				dep.addErrorType(category, type, label, description);
//				continue;
//			}
//			
//			//	read error (handle absence of severity for now, we do have a few existing protocols without it out there)
//			//	TODO remove severity absence hack
//			if ("ERROR".equals(data[0])) {
//				int i = 1;
//				String severity = (isSeverity(data[i]) ? data[i++] : Error.SEVERITY_CRITICAL);
//				String description = data[i++];
//				String source = data[i++];
//				Attributed subject = null;
//				if (data.length > i) {
//					String[] sData = new String[data.length - i];
//					System.arraycopy(data, i, sData, 0, sData.length);
//					subject = dep.findErrorSubject(doc, sData);
//				}
//				dep.addError(source, subject, doc, category, type, description, severity);
//			}
//		}
//		epBr.close();
//	}
//	private static String getElement(String[] data, int index, String def) {
//		return ((index < data.length) ? data[index] : def);
//	}
//	private static boolean isSeverity(String data) {
//		if (Error.SEVERITY_BLOCKER.equals(data))
//			return true;
//		if (Error.SEVERITY_CRITICAL.equals(data))
//			return true;
//		if (Error.SEVERITY_MAJOR.equals(data))
//			return true;
//		if (Error.SEVERITY_MINOR.equals(data))
//			return true;
//		return false;
//	}
//}

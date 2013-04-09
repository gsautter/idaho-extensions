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
package de.uka.ipd.idaho.plugins.bibRefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.TreeMap;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import de.uka.ipd.idaho.easyIO.streams.CharSequenceReader;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.GenericQueriableAnnotationWrapper;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathExpression;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathSyntaxException;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.XsltUtils;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * A type system of bibliographic references, comprising several reference
 * types. This class exists so multiple reference type systems can exist within
 * one Java Virtual Machine, if client code chooses to do so.<br>
 * The built-in default type system comprises six reference types: 'journal
 * article', 'journal volume', 'book chapter', 'book', 'proceedings', and
 * 'proceedings paper'. Client code can create and add custom reference types as
 * needed.
 * 
 * @author sautter
 */
public class BibRefTypeSystem implements BibRefConstants {
	
	/**
	 * A type of bibliographic reference, i.e., a type of referenced work, like
	 * 'journal article' or 'book', allowing to specify which data elements a
	 * reference data set should contain if it referes to a specific type of work.
	 * Instances of this class can check reference data sets for consistency and
	 * produce error messages.
	 * 
	 * @author sautter
	 */
	public static class BibRefType {
		private static abstract class Constraint {
			private String id;
			private String error;
			private Properties altErrors;
			Constraint(String error, Properties altErrors) {
				this.error = error;
				this.altErrors = altErrors;
			}
			String getId() {
				if (this.id == null)
					this.id = this.produceId();
				return this.id;
			}
			abstract String produceId();
			public boolean equals(Object obj) {
				return ((obj instanceof Constraint) && (((Constraint) obj).getId().equals(this.getId())));
			}
			public int hashCode() {
				return this.getId().hashCode();
			}
			String getError(String lang) {
				return (((lang == null) || (this.altErrors == null)) ? this.error : this.altErrors.getProperty(lang, this.error));
			}
			abstract String getError(Attributed refData, String lang);
		}
		private static class Requirement extends Constraint {
			private String[] attributes;
			Requirement(String error, Properties altErrors, String[] attributes) {
				super(error, altErrors);
				this.attributes = new String[attributes.length];
				System.arraycopy(attributes, 0, this.attributes, 0, attributes.length);
				Arrays.sort(this.attributes);
			}
			String produceId() {
				StringBuffer id = new StringBuffer("R: " + this.attributes[0]);
				for (int a = 1; a < this.attributes.length; a++) {
					id.append(' ');
					id.append(this.attributes[a]);
				}
				return id.toString();
			}
			String getError(Attributed refData, String lang) {
				for (int a = 0; a < this.attributes.length; a++) {
					if (refData.hasAttribute(this.attributes[a]))
						return null;
				}
				return this.getError(lang);
			}
		}
		private static class Pattern extends Constraint {
			private String attribute;
			private String pattern;
			Pattern(String error, Properties altErrors, String attribute, String pattern) {
				super(error, altErrors);
				this.attribute = attribute;
				this.pattern = pattern;
			}
			String produceId() {
				return ("P: " + this.attribute + " " + this.pattern);
			}
			String getError(Attributed refData, String lang) {
				Object value = refData.getAttribute(this.attribute);
				return ((value == null) ? null : this.getError(value.toString(), lang));
			}
			String getError(String value, String lang) {
				if (value == null)
					return null;
				return (value.trim().matches(this.pattern) ? null : this.getError(lang));
			}
		}
		private static class Exclusion extends Constraint {
			private String[] attributes;
			Exclusion(String error, Properties altErrors, String[] attributes) {
				super(error, altErrors);
				this.attributes = new String[attributes.length];
				System.arraycopy(attributes, 0, this.attributes, 0, attributes.length);
				Arrays.sort(this.attributes);
			}
			String produceId() {
				StringBuffer id = new StringBuffer("E: " + this.attributes[0]);
				for (int a = 1; a < this.attributes.length; a++) {
					id.append(' ');
					id.append(this.attributes[a]);
				}
				return id.toString();
			}
			String getError(Attributed refData, String lang) {
				for (int a = 0; a < this.attributes.length; a++) {
					if (refData.hasAttribute(this.attributes[a]))
						return this.getError(lang);
				}
				return null;
			}
		}
		private static class Condition extends Constraint {
			GPathExpression test;
			Condition(String error, Properties altErrors, GPathExpression test) {
				super(error, altErrors);
				this.test = test;
			}
			String produceId() {
				return this.test.toString();
			}
			String getError(final Attributed refData, String lang) {
				QueriableAnnotation refDoc = (
						(refData instanceof QueriableAnnotation)
						?
						((QueriableAnnotation) refData)
						:
						new AttributedWrapper(refData)
					);
				try {
					GPathObject checkRes = GPath.evaluateExpression(this.test, refDoc, null);
					return (((checkRes != null) && checkRes.asBoolean().value) ? this.getError(lang) : null);
				}
				catch (GPathException gpe) {
					return gpe.getMessage();
				}
			}
		}
		
		private HashSet required = new HashSet();
		private HashMap patterns = new HashMap();
		private HashMap alternativeSets = new HashMap(2);
		private HashSet excluded = new HashSet();
		private LinkedHashSet tests = new LinkedHashSet();
		private int checkStrictness = 5;
		
		private Transformer originXslt;
		private Transformer modsXslt;
		
		/** the name of the reference type */
		public final String name;
		
		private String label;
		private Properties altLabels = null;
		
		BibRefType(String name, int checkStrictness, String label, Properties altLabels) {
			this.checkStrictness = checkStrictness;
			this.name = name;
			if (label == null) {
				if (this.name.indexOf(' ') == -1)
					this.label = StringUtils.capitalize(this.name);
				else {
					String[] nameParts = this.name.split("\\s+");
					StringBuffer sb = new StringBuffer(StringUtils.capitalize(nameParts[0]));
					for (int p = 1; p < nameParts.length; p++) {
						sb.append(' ');
						sb.append(StringUtils.capitalize(nameParts[p]));
					}
					this.label = sb.toString();
				}
			}
			else this.label = label;
			this.altLabels = altLabels;
		}
		
		void setOriginXslt(Transformer originXslt) {
			this.originXslt = originXslt;
		}
		void setModsXslt(Transformer modsXslt) {
			this.modsXslt = modsXslt;
		}
		
		/**
		 * @return the label
		 */
		public String getLabel() {
			return this.label;
		}
		
		/**
		 * 
		 * @return the label
		 */
		public String getLabel(String lang) {
			return (((lang == null) || (this.altLabels == null)) ? this.label : this.altLabels.getProperty(lang, this.label));
		}
		
		/**
		 * Add a required attribute to the reference type. This requirement will
		 * be used to validate reference data sets in the getErrors() method.
		 * Note that the test is disjunctive, i.e., a reference data set
		 * fulfills the requirement if it has at least one of the attributes in
		 * the array. The argument array must have at least one element; if it
		 * has zero elements, the addition of the requirement is ignored.
		 * @param attributes the required attributes
		 * @param error the error message to output if a reference data set does
		 *            not have any of the attributes in the array
		 */
		public void addRequirement(String[] attributes, String error) {
			this.addRequirement(attributes, error, null);
		}
		
		/**
		 * Add a required attribute to the reference type. This requirement will
		 * be used to validate reference data sets in the getErrors() method.
		 * Note that the test is disjunctive, i.e., a reference data set
		 * fulfills the requirement if it has at least one of the attributes in
		 * the array. The argument array must have at least one element; if it
		 * has zero elements, the addition of the requirement is ignored.
		 * @param attributes the required attributes
		 * @param error the error message to output if a reference data set does
		 *            not have any of the attributes in the array
		 * @param altErrors error messages in alernative languages
		 */
		public void addRequirement(String[] attributes, String error, Properties altErrors) {
			if (attributes.length == 0)
				return;
			this.tests.add(new Requirement(error, altErrors, attributes));
			this.required.addAll(Arrays.asList(attributes));
			if (attributes.length == 1)
				return;
			HashSet as = new LinkedHashSet(Arrays.asList(attributes));
			for (int a = 0; a < attributes.length; a++)
				this.alternativeSets.put(attributes[a], as);
		}
		
		/**
		 * Exclude an attribute from the reference type. This exclusion will be
		 * used to validate reference data sets in the getErrors() method. Note
		 * that the test is disjunctive, i.e., a reference data set violates the
		 * exclusion if it has at least one of the attributes in the array. The
		 * argument array must have at least one element; if it has zero
		 * elements, the addition of the exclusion is ignored.
		 * @param attributes the excluded attributes
		 * @param error the error message to output if a reference data set has
		 *            any of the attributes in the array
		 */
		public void addExclusion(String[] attributes, String error) {
			this.addExclusion(attributes, error, null);
		}
		
		/**
		 * Exclude an attribute from the reference type. This exclusion will be
		 * used to validate reference data sets in the getErrors() method. Note
		 * that the test is disjunctive, i.e., a reference data set violates the
		 * exclusion if it has at least one of the attributes in the array. The
		 * argument array must have at least one element; if it has zero
		 * elements, the addition of the exclusion is ignored.
		 * @param attributes the excluded attributes
		 * @param error the error message to output if a reference data set has
		 *            any of the attributes in the array
		 * @param altErrors error messages in alernative languages
		 */
		public void addExclusion(String[] attributes, String error, Properties altErrors) {
			if (attributes.length == 0)
				return;
			this.tests.add(new Exclusion(error, altErrors, attributes));
			this.excluded.addAll(Arrays.asList(attributes));
		}
		
		/**
		 * Add a pattern based test for a required or optional attribute of the
		 * reference type. This check will be used to validate reference data
		 * sets in the getErrors() method.
		 * @param attributes the name of the attribute to check
		 * @param pattern the pattern to test the attribute against
		 * @param error the error message to output if the value the attribute
		 *            has in a reference data set does not match the pattern
		 */
		public void addPattern(String attribute, String pattern, String error) {
			this.addPattern(attribute, pattern, error, null);
		}
		
		/**
		 * Add a pattern based test for a required or optional attribute of the
		 * reference type. This check will be used to validate reference data
		 * sets in the getErrors() method.
		 * @param attributes the name of the attribute to check
		 * @param pattern the pattern to test the attribute against
		 * @param error the error message to output if the value the attribute
		 *            has in a reference data set does not match the pattern
		 * @param altErrors error messages in alernative languages
		 */
		public void addPattern(String attribute, String pattern, String error, Properties altErrors) {
			Pattern p = new Pattern(error, altErrors, attribute, pattern);
			this.tests.add(p);
			this.patterns.put(attribute, p);
		}
		
		/**
		 * Add a consistency condition to the reference type. This condition
		 * will be used to validate reference data sets in the getErrors()
		 * method. Note that the test is negative, i.e., it fails if the XML
		 * representation of a given reference data set matches the argument
		 * test, akin to SchemaTron's 'report' element. Checks are not intended
		 * to test the mere presence of individual attributes or specific
		 * constraints for their values, but rather attribute combinations; for
		 * presence tests, use on of the addRequirement() methods instead, for
		 * constraints on attribute values one of the the addPattern() methods.
		 * @param test the test GPath expression
		 * @param error the error message to output if a reference data set
		 *            matches the test
		 */
		public void addCondition(GPathExpression test, String error) {
			this.addCondition(test, error, null);
		}
		
		/**
		 * Add a consistency condition to the reference type. This condition
		 * will be used to validate reference data sets in the getErrors()
		 * method. Note that the test is negative, i.e., it fails if the XML
		 * representation of a given reference data set matches the argument
		 * test, akin to SchemaTron's 'report' element. Checks are not intended
		 * to test the mere presence of individual attributes or specific
		 * constraints for their values, but rather attribute combinations; for
		 * presence tests, use on of the addRequirement() methods instead, for
		 * constraints on attribute values on of the the addPattern() methods.
		 * @param test the test GPath expression
		 * @param error the error message to output if a reference data set
		 *            matches the test
		 * @param altErrors error messages in alernative languages
		 */
		public void addCondition(GPathExpression test, String error, Properties altErrors) {
			this.tests.add(new Condition(error, altErrors, test));
		}
		
		/**
		 * Test if a reference of this type must have a given attribute. This
		 * method returns true even if the argument attribute is part of a set
		 * of attributes only one of which needs to be present. Use the
		 * hasAlternatives() and getAlternatives() methods to find out if an
		 * attribute actually is a member of such a set, and what its
		 * alternatives are.
		 * @param name the name of the attribute
		 * @return true if the argument attribute is required in references of
		 *         this type
		 */
		public boolean requiresAttribute(String name) {
			return this.required.contains(name);
		}
		
		/**
		 * Test if an attribute is a member of an attribute set only one of
		 * which is required to be present.
		 * @param attribute the attribute to test
		 * @return true if the argument attribute is a member of a set
		 */
		public boolean hasAlternatives(String attribute) {
			return this.alternativeSets.containsKey(attribute);
		}
		
		/**
		 * Retrieve the alternatives of an attribute that is a member of an
		 * attribute set only one of which is required to be present. If the
		 * argument attribute is not member of a set of alternatives, this
		 * method returns null. Thus, the returned array has at least two
		 * elements, namely the argument attribute and at least one alternative.
		 * @param attribute the attribute to obtain the alternatives for
		 * @return an array holding the alternatives of the argument attribute,
		 *         including the argument attribute itself
		 */
		public String[] getAlternatives(String attribute) {
			HashSet as = ((HashSet) this.alternativeSets.get(attribute));
			return ((as == null) ? null : ((String[]) as.toArray(new String[as.size()])));
		}
		
		/**
		 * Test if a reference of this type may have a given attribute.
		 * @param name the name of the attribute
		 * @return true if the argument attribute is allowed in references of
		 *         this type
		 */
		public boolean canHaveAttribute(String name) {
			return !this.excluded.contains(name);
		}
		
		/**
		 * Check a single attribute value for validity. If the argument
		 * attribute is excluded, this method returns true only if the argument
		 * value is null, the empty string, or consists solely of whitespace.
		 * Likewise, if the argument attribute is required, this method can only
		 * return true if the argument value is not null, nor the empty string,
		 * nor consists solely of whitespace. For required and optional
		 * attributes, additional pattern based ckacks may apply.
		 * @param name the attribute name
		 * @param value the attribute value to test
		 * @return true if the specified value is valid for the specified
		 *         attribute, false otherwise
		 */
		public boolean checkValue(String name, String value) {
			value = ((value == null) ? "" : value.trim());
			if (this.excluded.contains(name))
				return (value.length() == 0);
			else if (value.length() == 0)
				return (this.requiresAttribute(name) ? (this.getAlternatives(name) != null) : true);
			Pattern p = ((Pattern) this.patterns.get(name));
			return ((p == null) ? true : (p.getError(value, null) == null));
		}
		
		/**
		 * Check a reference data set for errors. If this method does not find any
		 * errors, it returns null. Thus, an array returned by this method always
		 * has at least one element.
		 * @param refData the reference data set to check
		 * @return the error messages in an array, or null, if there are no errors
		 */
		public String[] getErrors(RefData refData) {
			return this.getErrors(refData, null);
		}
		
		/**
		 * Check a reference data set for errors. If this method does not find any
		 * errors, it returns null. Thus, an array returned by this method always
		 * has at least one element.
		 * @param refData the reference data set to check
		 * @return the error messages in an array, or null, if there are no errors
		 */
		public String[] getErrors(Attributed refData) {
			return this.getErrors(refData, null);
		}
		
		/**
		 * Check a reference data set for errors. If this method does not find
		 * any errors, it returns null. Thus, an array returned by this method
		 * always has at least one element. Languages should be indicated by
		 * two-letter ISO codes. If the argument language is null, the default
		 * (usually English) error messages are returned. The same applies if a
		 * check does not provide an error message in the specified language.
		 * @param refData the reference data set to check
		 * @param lang the language for the error messages
		 * @return the error messages in an array, or null, if there are no
		 *         errors
		 */
		public String[] getErrors(RefData refData, String lang) {
			return this.getErrors(new RefDataWrapper(refData), lang);
		}
		
		/**
		 * Check a reference data set for errors. If this method does not find
		 * any errors, it returns null. Thus, an array returned by this method
		 * always has at least one element. Languages should be indicated by
		 * two-letter ISO codes. If the argument language is null, the default
		 * (usually English) error messages are returned. The same applies if a
		 * check does not provide an error message in the specified language.
		 * @param refData the reference data set to check
		 * @param lang the language for the error messages
		 * @return the error messages in an array, or null, if there are no
		 *         errors
		 */
		public String[] getErrors(Attributed refData, String lang) {
			LinkedHashSet errors = new LinkedHashSet(2);
			for (Iterator tit = this.tests.iterator(); tit.hasNext();) {
				Constraint test = ((Constraint) tit.next());
				if ((test instanceof Condition) && !(refData instanceof QueriableAnnotation))
					refData = new AttributedWrapper(refData);
				String error = test.getError(refData, lang);
				if (error != null)
					errors.add(error);
			}
			return (errors.isEmpty() ? null : ((String[]) errors.toArray(new String[errors.size()])));
		}
		
		/**
		 * Test whether or not a reference data set matches this reference type.
		 * This method is used primarily for classification.
		 * @param refData the reference data set to test.
		 * @return true if the argument reference data set matches this reference
		 *         type
		 */
		public boolean matches(RefData refData) {
			return (this.getErrors(refData) == null);
		}
		
		/**
		 * Retrieve an indicator for how strict the matches() method is, on a 0
		 * to 10 scale, i.e., how liberal (0) or strict (10) a match is, or how
		 * many data elements it requires or excludes. This is, for instance, to
		 * help prioritizing individual reference types during classification.
		 * The default is 5.
		 * @return the match strictness
		 */
		public int getMatchStrictness() {
			return this.checkStrictness;
		}
		
		/**
		 * Obtain the origin of a reference data set. This can be the journal
		 * name and volume number for journal publications, the publisher and
		 * location for books, the conference name for proceedings papers, etc.
		 * @param refData the reference data set to get the origin from
		 * @return the origin
		 */
		public String getOrigin(RefData refData) {
			StringWriter origin = new StringWriter();
			try {
				this.originXslt.transform(new StreamSource(new StringReader(refData.toXML())), new StreamResult(origin));
				return grammar.unescape(origin.toString().trim());
			}
			catch (TransformerException te) {
				return null;
			}
		}
		
		/**
		 * Convert a reference data set into its MODS XML representation. This
		 * method loops through to the writeModsXML method. Should the latter fail
		 * for any reason, this method returns null. One reason for the
		 * writeModsXML() to fail is an erroneous or incomplete reference data set.
		 * Thus, client code should check a reference data set using the getErrors()
		 * method before handing it to this method.
		 * @param refData the reference data set to convert
		 * @return the MODS XML representation as a string
		 */
		public String toModsXML(RefData refData) {
			StringWriter mods = new StringWriter();
			try {
				this.writeModsXML(refData, mods);
				return mods.toString();
			}
			catch (IOException ioe) {
				return null;
			}
		}
		
		/**
		 * Write the MODS XML representation of a reference data set to some writer.
		 * @param refData the reference data set to convert
		 */
		public void writeModsXML(RefData refData, Writer out) throws IOException {
			try {
				this.modsXslt.transform(new StreamSource(new StringReader(refData.toXML())), new StreamResult(out));
			}
			catch (TransformerException te) {
				throw new IOException(te.getMessage());
			}
		}
	}
	
	
	private static class RefDataWrapper extends AbstractAttributed {
		private RefData refData;
		RefDataWrapper(RefData refData) {
			this.refData = refData;
		}
		public Object getAttribute(String name, Object def) {
			String value = this.refData.getAttribute(name);
			return ((value == null) ? def : value);
		}
		public Object getAttribute(String name) {
			return this.refData.getAttribute(name);
		}
		public boolean hasAttribute(String name) {
			return this.refData.hasAttribute(name);
		}
		public String[] getAttributeNames() {
			return this.refData.getAttributeNames();
		}
	}
	private static class AttributedWrapper extends GenericQueriableAnnotationWrapper {
		private static final QueriableAnnotation dummy = Gamta.newDocument(Gamta.newTokenSequence("DUMMY", Gamta.INNER_PUNCTUATION_TOKENIZER));
		private Attributed refData;
		AttributedWrapper(Attributed refData) {
			super(dummy);
			this.refData = refData;
		}
		public Object getAttribute(String name, Object def) {
			return refData.getAttribute(name, def);
		}
		public Object getAttribute(String name) {
			return refData.getAttribute(name);
		}
		public String[] getAttributeNames() {
			return refData.getAttributeNames();
		}
		public boolean hasAttribute(String name) {
			return refData.hasAttribute(name);
		}
	}
	
	private String key = "DefaultBibRefTypeSystem";
	private TreeMap bibRefTypes = new TreeMap();
	
	private BibRefTypeSystem() {
		this(null, null, false);
	}
	private BibRefTypeSystem(String typeSystemName, Reader typeSystemData, boolean inheritChecks) {
		
		//	load default type system
		try {
			String brtscn = BibRefTypeSystem.class.getName().replaceAll("\\.", "/");
			InputStream tis = BibRefTypeSystem.class.getClassLoader().getResourceAsStream(brtscn.substring(0, brtscn.lastIndexOf('/')) + "/BibRefTypeSystem.xml");
			BufferedReader tr = new BufferedReader(new InputStreamReader(tis, "UTF-8"));
			this.addBibRefTypes(tr, true, false);
			tr.close();
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
		}
		
		//	load custom type system
		if (typeSystemData != null) try {
			this.key = ("BibRefTypeSystem" + typeSystemName.hashCode());
			this.addBibRefTypes(typeSystemData, true,inheritChecks);
			typeSystemData.close();
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}
	
	/**
	 * Read reference types from an XML data stream. The argument boolean
	 * controls whether or not existing reference types are replaced by the ones
	 * read from the argument data stream.
	 * @param in the reader to read from
	 * @param replaceExisting replace existing reference types with ones from
	 *            the stream?
	 * @param mergeChecks merge checks from existing and newly loaded reference
	 *            types?
	 */
	public void addBibRefTypes(Reader in, boolean replaceExisting, boolean mergeChecks) throws IOException {
		final ArrayList refTypes = new ArrayList();
		final HashMap originXsltTokensByType = new HashMap();
		final HashMap modsXsltTokensByType = new HashMap();
		final StringVector globalXsltTokens = new StringVector();
		parser.stream(in, new TokenReceiver() {
			private BibRefType refType = null;
			private StringVector originTemplateTokens = null;
			private StringVector modsTemplateTokens = null;
			private boolean inGlobalTemplate = false;
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (grammar.isTag(token)) {
					String type = grammar.getType(token);
					boolean isEndTag = grammar.isEndTag(token);
					if ("bibRefType".equals(type)) {
						if (isEndTag) {
							if (this.refType != null)
								refTypes.add(this.refType);
							this.refType = null;
						}
						else {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
							String name = tnas.getAttribute("name");
							String label = tnas.getAttribute("label");
							int ms = Integer.parseInt(tnas.getAttribute("matchStrictness", "5"));
							if (name != null)
								this.refType = new BibRefType(name, ms, label, this.getAltStrings(tnas, "label-"));
						}
					}
					else if ("constraint".equals(type) || "test".equals(type)) { // TODO remove "test" element name after some grace period
						if (this.refType != null) {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
							String error = tnas.getAttribute("message");
							String pattern = tnas.getAttribute("pattern");
							String require = tnas.getAttribute("require");
							String attributes = tnas.getAttribute("attributes");
							String exclude = tnas.getAttribute("exclude");
							String condition = tnas.getAttribute("condition", tnas.getAttribute("check")); // TODO remove "check" attribute name after some grace period
							if (require != null) {
								String[] attributeNames = require.trim().split("\\s+");
								if (error == null)
									error = (this.getEnumeration(attributeNames, true, "and") + " " + ((attributeNames.length == 1) ? "is" : "are") + " missing" + ((pattern == null) ? "" : " or invalid") + ".");
								Properties altErrors = this.getAltStrings(tnas, "message-");
								this.refType.addRequirement(attributeNames, error, altErrors);
								if (pattern != null) {
									for (int a = 0; a < attributeNames.length; a++)
										this.refType.addPattern(attributeNames[a], pattern, error, altErrors);
								}
							}
							else if (exclude != null) {
								String[] attributeNames = exclude.trim().split("\\s+");
								if (error == null)
									error = (StringUtils.capitalize(this.refType.name) + "s don't have a " + this.getEnumeration(attributeNames, false, "or") + ".");
								this.refType.addExclusion(attributeNames, error, this.getAltStrings(tnas, "message-"));
							}
							else if (error != null) {
								if ((attributes != null) && (pattern != null)) {
									String patternError = RegExUtils.validateRegEx(pattern);
									if (patternError == null) {
										String[] attributeNames = attributes.trim().split("\\s+");
										Properties altErrors = this.getAltStrings(tnas, "message-");
										for (int a = 0; a < attributeNames.length; a++)
											this.refType.addPattern(attributeNames[a], pattern, error, altErrors);
									}
									else System.out.println("Invalid regular expression pattern '" + pattern + "'.");
								}
								else if (condition != null) try {
									this.refType.addCondition(GPathParser.parseExpression(condition), error, this.getAltStrings(tnas, "message-"));
								}
								catch (GPathSyntaxException gpse) {
									System.out.println("Invalid GPath expression '" + condition + "'.");
									gpse.printStackTrace(System.out);
								}
							}
						}
					}
					else if ("originTemplate".equals(type)) {
						if (isEndTag) {
							if (this.refType != null)
								originXsltTokensByType.put(this.refType.name, this.originTemplateTokens);
							this.originTemplateTokens = null;
						}
						else this.originTemplateTokens = new StringVector();
					}
					else if ("modsTemplate".equals(type)) {
						if (isEndTag) {
							if (this.refType != null)
								modsXsltTokensByType.put(this.refType.name, this.modsTemplateTokens);
							this.modsTemplateTokens = null;
						}
						else this.modsTemplateTokens = new StringVector();
					}
					else if ("xsl:template".equals(type)) {
						if (isEndTag) {
							if (this.inGlobalTemplate)
								globalXsltTokens.addElement(token);
							this.inGlobalTemplate = false;
						}
						else {
							this.inGlobalTemplate = true;
							globalXsltTokens.addElement(token);
						}
					}
					else if (this.originTemplateTokens != null)
						this.originTemplateTokens.addElement(token);
					else if (this.modsTemplateTokens != null)
						this.modsTemplateTokens.addElement(token);
					else if (this.inGlobalTemplate)
						globalXsltTokens.addElement(token);
				}
				else if (this.inGlobalTemplate)
					globalXsltTokens.addElement(token);
				else if (this.modsTemplateTokens != null)
					this.modsTemplateTokens.addElement(token);
				else if (this.originTemplateTokens != null)
					this.originTemplateTokens.addElement(token);
			}
			private String getEnumeration(String[] attributes, boolean capitalizeFirst, String conjunction) {
				StringBuffer enumeration = new StringBuffer(capitalizeFirst ? StringUtils.capitalize(attributes[0]) : attributes[0]);
				for (int a = 1; a < attributes.length; a++) {
					if (attributes.length > 2)
						enumeration.append(',');
					if ((a+1) == attributes.length)
						enumeration.append(" " + conjunction);
					enumeration.append(" " + attributes[a]);
				}
				return enumeration.toString();
			}
			private Properties getAltStrings(TreeNodeAttributeSet tnas, String prefix) {
				Properties altStrings = null;
				String[] ans = tnas.getAttributeNames();
				for (int a = 0; a < ans.length; a++) {
					if (ans[a].startsWith(prefix)) {
						String lang = ans[a].substring(prefix.length());
						if (altStrings == null)
							altStrings = new Properties();
						altStrings.setProperty(lang, tnas.getAttribute(ans[a]));
					}
				}
				return altStrings;
			}
		});
		
		//	insert origin and MODS XML templates into XSLT stylesheet stubs, create transformers, and store reference type
		for (int r = 0; r < refTypes.size(); r++) {
			BibRefType brt = ((BibRefType) refTypes.get(r));
			
			//	get type specific XSLT tokens
			StringVector originXsltTokens = ((StringVector) originXsltTokensByType.get(brt.name));
			if (originXsltTokens == null)
				continue;
			
			//	get type specific XSLT tokens
			StringVector modsXsltTokens = ((StringVector) modsXsltTokensByType.get(brt.name));
			if (modsXsltTokens == null)
				continue;
			
			//	generate XSLT stylesheet
			StringBuffer originXstlBuilder = new StringBuffer();
			for (int p = 0; p < originXsltPrefix.length; p++)
				originXstlBuilder.append(modsXsltPrefix[p]);
			for (int t = 0; t < originXsltTokens.size(); t++)
				originXstlBuilder.append(originXsltTokens.get(t));
			for (int i = 0; i < originXsltInfix.length; i++)
				originXstlBuilder.append(originXsltInfix[i]);
			for (int g = 0; g < globalXsltTokens.size(); g++)
				originXstlBuilder.append(globalXsltTokens.get(g));
			for (int s = 0; s < originXsltSuffix.length; s++)
				originXstlBuilder.append(originXsltSuffix[s]);
			
			//	generate XSLT stylesheet
			StringBuffer modsXstlBuilder = new StringBuffer();
			for (int p = 0; p < modsXsltPrefix.length; p++)
				modsXstlBuilder.append(modsXsltPrefix[p]);
			for (int t = 0; t < modsXsltTokens.size(); t++)
				modsXstlBuilder.append(modsXsltTokens.get(t));
			for (int i = 0; i < modsXsltInfixBeforeStaticClassification.length; i++)
				modsXstlBuilder.append(modsXsltInfixBeforeStaticClassification[i]);
			modsXstlBuilder.append("<mods:classification>" + grammar.escape(brt.name) + "</mods:classification>");
			for (int i = 0; i < modsXsltInfixAfterStaticClassification.length; i++)
				modsXstlBuilder.append(modsXsltInfixAfterStaticClassification[i]);
			for (int g = 0; g < globalXsltTokens.size(); g++)
				modsXstlBuilder.append(globalXsltTokens.get(g));
			for (int s = 0; s < modsXsltSuffix.length; s++)
				modsXstlBuilder.append(modsXsltSuffix[s]);
			
			try {
				
				//	compile XSL transformer
				try {
					brt.setOriginXslt(XsltUtils.getTransformer((this.key + ":" + brt.name + ".origin"), new CharSequenceReader(originXstlBuilder)));
				}
				catch (IOException ioe) {
					System.out.println(" === " + brt.name + " origin === ");
					System.out.println(originXstlBuilder);
					throw ioe;
				}
				
				//	compile XSL transformer
				try {
					brt.setModsXslt(XsltUtils.getTransformer((this.key + ":" + brt.name + ".mods"), new CharSequenceReader(modsXstlBuilder)));
				}
				catch (IOException ioe) {
					System.out.println(" === " + brt.name + " MODS === ");
					System.out.println(modsXstlBuilder);
					throw ioe;
				}
				
				//	retrieve existing reference type
				BibRefType eBrt = (mergeChecks ? this.getBibRefType(brt.name) : null);
				
				//	store newly added reference type, or replace existing one
				if ((eBrt == null) || replaceExisting) {
					
					//	inherit checks from existing reference type
					if (eBrt != null)
						brt.tests.addAll(eBrt.tests);
					
					//	store reference type
					this.bibRefTypes.put(brt.name, brt);
				}
				
				//	merge checks of newly loaded reference type into existing one
				else if (mergeChecks)
					eBrt.tests.addAll(brt.tests);
			}
			catch (IOException ioe) {
				ioe.printStackTrace(System.out);
			}
		}
	}
	
	private static final Grammar grammar = new StandardGrammar();
	private static final Parser parser = new Parser(grammar);
	
	private static String[] originXsltPrefix = {
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
		"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">",
		"<xsl:output method=\"xml\" version=\"1.0\" encoding=\"UTF-8\" indent=\"yes\"/>",
		"<xsl:output omit-xml-declaration=\"yes\"/>",
		"<xsl:template match=\"//bibRef\">",
	};
	private static String[] originXsltInfix = {
		"</xsl:template>",
	};
	private static String[] originXsltSuffix = {
		"</xsl:stylesheet>"
	};
	
	private static String[] modsXsltPrefix = {
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
		"<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:mods=\"http://www.loc.gov/mods/v3\">",
		"<xsl:output method=\"xml\" version=\"1.0\" encoding=\"UTF-8\" indent=\"yes\"/>",
		"<xsl:output omit-xml-declaration=\"yes\"/>",
		"<xsl:template match=\"//bibRef\">",
		"<mods:mods>",
	};
	private static String[] modsXsltInfixBeforeStaticClassification = {
		"<xsl:choose>",
		"<xsl:when test=\"./@type\">",
		"<mods:classification>",
		"<xsl:value-of select=\"./@type\"/>",
		"</mods:classification>",
		"</xsl:when>",
		"<xsl:otherwise>",
	};
	private static String[] modsXsltInfixAfterStaticClassification = {
		"</xsl:otherwise>",
		"</xsl:choose>",
		"<xsl:call-template name=\"identifiers\"/>",
		"</mods:mods>",
		"</xsl:template>"
	};
	private static String[] modsXsltSuffix = {
		"<xsl:template name=\"identifiers\">",
		"<xsl:for-each select=\"./" + PUBLICATION_IDENTIFIER_ANNOTATION_TYPE + "\">",
		"<xsl:element name=\"mods:identifier\">",
		"<xsl:attribute name=\"type\"><xsl:value-of select=\"./@" + TYPE_ATTRIBUTE + "\"/></xsl:attribute>",
		"<xsl:value-of select=\".\"/>",
		"</xsl:element>",
		"</xsl:for-each>",
		"</xsl:template>",
		"</xsl:stylesheet>"
	};
	
	/**
	 * Retrieve a reference type by name. If no reference type with the argument
	 * name exists in this reference type system, this method returns null.
	 * @param name
	 * @return
	 */
	public BibRefType getBibRefType(String name) {
		return ((name == null) ? null : ((BibRefType) this.bibRefTypes.get(name)));
	}
	
	/**
	 * Retrieve the reference types currently existing in this reference type
	 * system. Client code may modify the returned array as desired.
	 * @return an array holding the reference types
	 */
	public BibRefType[] getBibRefTypes() {
		return ((BibRefType[]) this.bibRefTypes.values().toArray(new BibRefType[this.bibRefTypes.size()]));
	}
	
	/**
	 * Retrieve the names of the reference types currently existing in this
	 * reference type system. Client code may modify the returned array as
	 * desired.
	 * @return an array holding the reference type names
	 */
	public String[] getBibRefTypeNames() {
		return ((String[]) this.bibRefTypes.keySet().toArray(new String[this.bibRefTypes.size()]));
	}
	
	/**
	 * Check a reference data set for errors, based on its type attribute. If
	 * the reference data set does not have a type attribute, or if this
	 * reference type system does not know the type attribute, this method
	 * returns a one-element array with the only entry saying so. If this method
	 * does not find any errors, it returns null. Thus, an array returned by
	 * this method always has at least one element.
	 * @param refData the reference data set to check
	 * @return the error messages in an array, or null, if there are no errors
	 */
	public String[] checkType(Attributed ref) {
		Object type = ref.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		if (type == null) {
			String[] errors = {"Unknown reference type 'null'."};
			return errors;
		}
		BibRefType brt = this.getBibRefType(type.toString());
		if (brt == null) {
			String[] errors = {"Unknown reference type '" + type.toString() + "'."};
			return errors;
		}
		return brt.getErrors(ref);
	}
	
	/**
	 * Check a reference data set for errors, based on its type attribute. If
	 * the reference data set does not have a type attribute, or if this
	 * reference type system does not know the type attribute, this method
	 * returns a one-element array with the only entry saying so. If this method
	 * does not find any errors, it returns null. Thus, an array returned by
	 * this method always has at least one element.
	 * @param ref the reference data set to classify
	 * @return the name of the best fitting reference type
	 */
	public String[] checkType(RefData ref) {
		String type = ref.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		if (type == null) {
			String[] errors = {"Unknown reference type 'null'."};
			return errors;
		}
		BibRefType brt = this.getBibRefType(type);
		if (brt == null) {
			String[] errors = {"Unknown reference type '" + type + "'."};
			return errors;
		}
		return brt.getErrors(ref);
	}
	
	/**
	 * Classify a reference data set. This method checks which of the types in
	 * this reference type system the argument reference data set fits. If the
	 * type system does not include a reference type that matches the argument
	 * reference data set, this method returns null.
	 * @param ref the reference data set to classify
	 * @return the name of the best fitting reference type
	 */
	public String classify(Attributed ref) {
		String type = null;
		int typeStrictness = 0;
		for (Iterator tit = this.bibRefTypes.values().iterator(); tit.hasNext();) {
			BibRefType brt = ((BibRefType) tit.next());
			String[] errors = brt.getErrors(ref);
			if (errors != null)
				continue;
			if (brt.getMatchStrictness() > typeStrictness) {
				type = brt.name;
				typeStrictness = brt.getMatchStrictness();
			}
		}
		return type;
	}
	
	/**
	 * Classify a reference data set. This method checks which of the types in
	 * this reference type system the argument reference data set fits. If the
	 * type system does not include a reference type that matches the argument
	 * reference data set, this method returns null.
	 * @param ref the reference data set to classify
	 * @return the name of the best fitting reference type
	 */
	public String classify(RefData ref) {
		return this.classify(new RefDataWrapper(ref));
	}
	
	/**
	 * Transform a reference data set into MODS XML. If the argument reference
	 * does not have a 'type' attribute, this method calls the classify() method
	 * before doing the actual transformation. If none of the reference types in
	 * this reference type system matches the argument reference data set, this
	 * method returns null.
	 * @param ref the reference data set to transform
	 * @return the MODS XML representation of the argument reference data set
	 */
	public String toModsXML(RefData ref) {
		String type = ref.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		if ((type == null) || (type.length() == 0))
			type = this.classify(ref);
		if (type == null)
			return null;
		BibRefType brt = this.getBibRefType(type);
		return ((brt == null) ? null : brt.toModsXML(ref));
	}
	
	/**
	 * Obtain the origin string of a reference data set. If the argument
	 * reference does not have a 'type' attribute, this method calls the
	 * classify() method before doing the actual extraction. If none of the
	 * reference types in this reference type system matches the argument
	 * reference data set, this method returns null.
	 * @param ref the reference data set to obtain the origin for
	 * @return the origin string for the argument reference data set
	 */
	public String getOrigin(RefData ref) {
		String type = ref.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		if ((type == null) || (type.length() == 0))
			type = this.classify(ref);
		if (type == null)
			return null;
		BibRefType brt = this.getBibRefType(type);
		return ((brt == null) ? null : brt.getOrigin(ref));
	}
	
	/**
	 * Retrieve the built-in default reference type system.
	 * @return the default reference type system
	 */
	public static BibRefTypeSystem getDefaultInstance() {
		if (defaultInstance == null)
			defaultInstance = new BibRefTypeSystem();
		return defaultInstance;
	}
	
	/**
	 * Retrieve a reference type system rooted in a specific storage location.
	 * This factory method ensures that there is only a single instance of this
	 * class per storage location. If the argument file does not exist, the
	 * returned type system is empty, safe for the built-in default types, but
	 * no exception is thrown. If the argument file is null, this method returns
	 * the built-in default instance. If the argument file is a folder, this
	 * method attempts to load a file 'BibRefTypeSystem.xml' from it. Custom
	 * instances always contain the built-in default reference types from the
	 * default instance. If the type system file contains a custom definition of
	 * any of the default types, this definition supersenses the built-in
	 * default definition.
	 * @param dataPath the data path to use
	 * @param inheritChecks inherit consistency checks from default type system?
	 * @return the reference type system rooted in the argument storage location
	 */
	public static BibRefTypeSystem getInstance(File dataPath, boolean inheritChecks) {
		if (dataPath == null)
			return getDefaultInstance();
		if (dataPath.isDirectory())
			return getInstance(new AnalyzerDataProviderFileBased(dataPath), inheritChecks);
		else return getInstance(new AnalyzerDataProviderFileBased(dataPath.getParentFile()), dataPath.getName(), inheritChecks);
	}
	
	/**
	 * Retrieve a reference type system rooted in a specific storage location,
	 * the latter being represented by the argument data provider. This factory
	 * method ensures that there is only a single instance of this class per
	 * storage location. This method expects to find a file named
	 * 'BibRefTypeSystem.xml' in the argument storage location. If this file
	 * does not exist, the returned type system is empty, safe for the built-in
	 * default types, but no exception is thrown. If the argument data provider
	 * is null, this method returns the built-in default instance. Custom
	 * instances always contain the built-in default reference types from the
	 * default instance. If the type system file contains a custom definition of
	 * any of the default types, this definition supersenses the built-in
	 * default definition.
	 * @param adp the data provider to use
	 * @param inheritChecks inherit consistency checks from default type system?
	 * @return the reference type system rooted in the argument storage location
	 */
	public static BibRefTypeSystem getInstance(AnalyzerDataProvider adp, boolean inheritChecks) {
		return getInstance(adp, "BibRefTypeSystem.xml", inheritChecks);
	}
	
	/**
	 * Retrieve a reference type system rooted in a specific storage location,
	 * the latter being represented by the argument data provider and the
	 * argument data file name. This factory method ensures that there is only a
	 * single instance of this class per storage location. If this file does not
	 * exist, the returned type system is empty, safe for the built-in default
	 * types, but no exception is thrown. If the argument data provider is null,
	 * this method returns the built-in default instance. Custom instances
	 * always contain the built-in default reference types from the default
	 * instance. If the type system file contains a custom definition of any of
	 * the default types, this definition supersenses the built-in default
	 * definition.
	 * @param adp the data provider to use
	 * @param typeSystemFileName the name of the file to load the reference type
	 *            system from
	 * @param inheritChecks inherit consistency checks from default type system?
	 * @return the reference type system rooted in the argument storage location
	 */
	public static BibRefTypeSystem getInstance(AnalyzerDataProvider adp, String typeSystemFileName, boolean inheritChecks) {
		if (adp == null)
			return getDefaultInstance();
		try {
			InputStream tis = adp.getInputStream(typeSystemFileName);
			BufferedReader tr = new BufferedReader(new InputStreamReader(tis, "UTF-8"));
			return getInstance((adp.getAbsolutePath() + "/" + typeSystemFileName), tr, inheritChecks);
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
			return getDefaultInstance();
		}
	}
	
	/**
	 * Retrieve a reference type system by a dedicated name uniquely identifying
	 * it within the JVM. This factory method ensures that there is only a
	 * single instance of this class per name. If the argument reader is null,
	 * this method returns the built-in default instance. Custom instances
	 * always contain the built-in default reference types from the default
	 * instance. If the type system file contains a custom definition of any of
	 * the default types, this definition supersenses the built-in default
	 * definition. The argument reader is closed after reading the data.
	 * @param typeSystemName the name of the type system
	 * @param typeSystemData the reader to load the type system data from
	 * @param inheritChecks inherit consistency checks from default type system?
	 * @return the reference type system with the argument name
	 */
	public static BibRefTypeSystem getInstance(String typeSystemName, Reader typeSystemData, boolean inheritChecks) {
		if (typeSystemData == null)
			return getDefaultInstance();
		BibRefTypeSystem brts = ((BibRefTypeSystem) instances.get(typeSystemName));
		if (brts == null) {
			brts = new BibRefTypeSystem(typeSystemName, typeSystemData, inheritChecks);
			instances.put(typeSystemName, brts);
		}
		return brts;
	}
	
	/**
	 * Retrieve a reference type system by a dedicated name uniquely identifying
	 * it within the JVM. This factory method ensures that there is only a
	 * single instance of this class per name. If the argument name is null,
	 * this method returns the built-in default instance. Otherwise, this method
	 * returns (and possibly generates before that) a clone of the default type
	 * system and makes it available under the argument name. This method is
	 * mainly provided for client code that builds upon the default type system,
	 * but wants to modify it.
	 * @param typeSystemName the name of the type system
	 * @return the reference type system with the argument name
	 */
	public static BibRefTypeSystem getInstance(String typeSystemName) {
		if (typeSystemName == null)
			return getDefaultInstance();
		BibRefTypeSystem brts = ((BibRefTypeSystem) instances.get(typeSystemName));
		if (brts == null) {
			brts = new BibRefTypeSystem(typeSystemName, null, true);
			instances.put(typeSystemName, brts);
		}
		return brts;
	}
	
	private static BibRefTypeSystem defaultInstance = null;
	private static final HashMap instances = new HashMap();
}
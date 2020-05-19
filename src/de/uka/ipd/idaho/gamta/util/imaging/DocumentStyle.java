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
 *     * Neither the name of the Universitaet Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.gamta.util.imaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * Instances of this class describe the style of a particular type or class of
 * documents, e.g. articles from a specific journal, by means of any kind of
 * parameters. This can range from the font styles used for headings of
 * individual levels to the format of named entities like dates to font styles
 * and position information of bibliographic attributes in the article header
 * to the styles of bibliographic references, and so forth.<br>
 * All parameter values are stored as strings internally. Their external values
 * can be of other types as well, however. Values of primitive types, lists,
 * and bounding boxes are recovered via the <code>parseXyz(String)</code>
 * methods of the respective (wrapper) classes.<br>
 * Parameters that represent positional information or measurements (e.g. the
 * location of page numbers, or the width of a column margin) naturally depend
 * on page image resolution. It is recommended to provide such parameter values
 * for a resolution of 72 DPI, the default typographical Point unit.<br>
 * It is recommended to group parameters by means of prefixing their names,
 * e.g. <code>docMeta.title.fontStyle</code> for the font style used in the
 * document title. This helps group parameters that refer to individual aspects
 * of document analysis. In addition, prefixed views can considerably shorten
 * parameter names in client code.<br>
 * Because it is impossible to a priory know all the style parameters used by
 * individual document markup and data extraction tools, this class learns all
 * parameter names it is asked for, together with their types. This allows
 * listing the names of all parameters ever asked for, and simplifies filling
 * any gaps, even if they emerge only after updates.<br>
 * To abstract the location where actual style parameter lists are stored, this
 * class statically provides a central hub for sources of parameter lists. The
 * <code>getStyleFor()</code> methods delegate to the registered providers, so
 * the latter can vary freely depending on each individual deployment scenario.
 * 
 * @author sautter
 */
public class DocumentStyle extends Properties {
	
	/** the attribute storing the style of a document */
	public static final String DOCUMENT_STYLE_ATTRIBUTE = "docStyle";
	
	/** the attribute storing the name of the style of a document, as a hint for providers */
	public static final String DOCUMENT_STYLE_NAME_ATTRIBUTE = "docStyleName";
	
	/**
	 * Abstract provider of document style parameter lists.
	 * 
	 * @author sautter
	 */
	public static interface Provider {
		
		/**
		 * Obtain the style parameter list for a given document, represented as
		 * a generic <code>Attributed</code> object. If a provider does not
		 * have a parameter list for the argument document, or the argument
		 * document cannot be matched to any parameter list, it has to return
		 * <code>null</code>. A non-<code>null</code> return value is wrapped
		 * in a <code>DocumentStyle</code> object to prevent accidental
		 * modification by client code.
		 * @param doc the document to obtain the style for
		 * @return the style parameter list for the argument document
		 */
		public abstract Properties getStyleFor(Attributed doc);
	}
	
	private static ArrayList providers = new ArrayList(2);
	private static Map parameterNamesToClasses = Collections.synchronizedMap(new TreeMap());
	
	/**
	 * Add a document style provider.
	 * @param dsp the provider to add
	 */
	public static void addProvider(Provider dsp) {
		if ((dsp != null) && !providers.contains(dsp))
			providers.add(dsp);
	}
	
	/**
	 * Remove a document style provider.
	 * @param dsp the provider to remove
	 */
	public static void removeProvider(Provider dsp) {
		providers.remove(dsp);
	}
	
	/**
	 * Obtain the style parameter list for a given document, represented as a
	 * generic <code>Attributed</code> object. This method first checks if the 
	 * argument document already has a <code>DocumentStyle</code> attached to
	 * if in its 'docStyle' attribute, and if so, returns it. Otherwise, this 
	 * method delegates to the registered providers. If none are registered, or
	 * none has a style parameter list for the argument document, this method
	 * returns an empty <code>DocumentStyle</code> object, but never
	 * <code>null</code>. In any case, this method attempts to store the
	 * returned <code>DocumentStyle</code> in the 'docStyle' attribute for
	 * easier access later on.
	 * @param doc the document to obtain the style for
	 * @return the style parameter list for the argument document
	 */
	public static DocumentStyle getStyleFor(Attributed doc) {
		Object dso = doc.getAttribute(DOCUMENT_STYLE_ATTRIBUTE);
		if (dso instanceof DocumentStyle)
			return ((DocumentStyle) dso);
		Properties dspl = null;
		for (int p = 0; p < providers.size(); p++) {
			dspl = ((Provider) providers.get(p)).getStyleFor(doc);
			if (dspl != null)
				break;
		}
		DocumentStyle ds = new DocumentStyle(dspl);
		try {
			doc.setAttribute(DOCUMENT_STYLE_ATTRIBUTE, ds);
			if (!doc.hasAttribute(DOCUMENT_STYLE_NAME_ATTRIBUTE) && (dspl != null)) {
				String dsn = dspl.getProperty(DOCUMENT_STYLE_NAME_ATTRIBUTE);
				if (dsn != null)
					doc.setAttribute(DOCUMENT_STYLE_NAME_ATTRIBUTE, dsn);
			}
		} catch (Throwable t) { /* catch any exception thrown from immutable documents, etc. */ }
		return ds;
	}
	
	/**
	 * Retrieve a list of all style parameter names requested so far. This
	 * enables learning new parameters manes and filling any gaps that might
	 * emerge.
	 * @return a list of all style parameter names
	 */
	public static String[] getParameterNames() {
		return ((String[]) parameterNamesToClasses.keySet().toArray(new String[parameterNamesToClasses.size()]));
	}
	
	/**
	 * Retrieve the class of the values of a given parameter. If the argument
	 * parameter has not been requested yet, this method returns the default
	 * class <code>String</code>.
	 * @param pn the name of the parameter
	 * @return the class of the parameter values
	 */
	public static Class getParameterValueClass(String pn) {
		Class cls = ((Class) parameterNamesToClasses.get(pn));
		return ((cls == null) ? String.class : cls);
	}
	
	/**
	 * Retrieve the class of the entries of a list value class.
	 * @param listClass the list value class
	 * @return the list entry class
	 */
	public static Class getListElementClass(Class listClass) {
		if (listClass.getName().equals(stringListClass.getName()))
			return String.class;
		else if (listClass.getName().equals(intListClass.getName()))
			return Integer.class;
		else if (listClass.getName().equals(floatListClass.getName()))
			return Float.class;
		else if (listClass.getName().equals(doubleListClass.getName()))
			return Double.class;
		else if (listClass.getName().equals(booleanListClass.getName()))
			return Boolean.class;
		else if (listClass.getName().equals(boxListClass.getName()))
			return BoundingBox.class;
		else return listClass;
	}
	
	/**
	 * Descriptor object for a group of document style parameters, i.e., all
	 * parameters that share the same name right up to and including the last
	 * period. This class enables consumers of such parameter groups to provide
	 * labels, explanations, and custom testing facilities for the parameters
	 * they use, for instance in order to augment editing facilities with more
	 * user friendly information about the semantics of the parameters and
	 * parameter groups they use.<br/>
	 * Attention: some parameter groups might be used by multiple consumers. In
	 * such cases, the code providing a parameter group description should check
	 * for any existing descriptions to augment first.
	 * 
	 * @author sautter
	 */
	public static class ParameterGroupDescription {
		
		/** the prefix shared between all parameter names in the group, _excluding_ the terminal period */
		public final String parameterNamePrefix;
		
		private String label;
		private String description;
		
		private HashMap parameterNamesToDescriptions = new HashMap();
		
		/** Constructor
		 * @param pnp the prefix shared between all parameter names in the group
		 */
		public ParameterGroupDescription(String pnp) {
			this.parameterNamePrefix = pnp;
		}
		
		/**
		 * Retrieve the label of the parameter group.
		 * @return the label
		 */
		public String getLabel() {
			return this.label;
		}
		
		/**
		 * Provide a label for the parameter group, e.g. for displaying in a UI.
		 * @param label the label to set
		 */
		public void setLabel(String label) {
			this.label = label;
		}
		
		/**
		 * Retrieve the description of the parameter group.
		 * @return the description
		 */
		public String getDescription() {
			return this.description;
		}
		
		/**
		 * Provide a description for the parameter group, e.g. for displaying
		 * in a UI.
		 * @param description the description to set
		 */
		public void setDescription(String description) {
			this.description = description;
		}
		
		/**
		 * Retrieve the names of the parameters currently described in this
		 * group description.
		 * @return an array holding the parameter names
		 */
		public String[] getParameterNames() {
			return ((String[]) this.parameterNamesToDescriptions.keySet().toArray(new String[this.parameterNamesToDescriptions.size()]));
		}
		
		/**
		 * Retrieve the description for a parameter in the group. The argument
		 * name has to be without the group prefix.
		 * @param pn the name of the parameter to obtain the label for
		 * @return the description for the argument parameter
		 */
		public ParameterDescription getParameterDescription(String pn) {
			pn = pn.substring(pn.lastIndexOf(".") + ".".length());
			return this.getParameterDescription(pn, false);
		}
		
		/**
		 * Provide a description for a parameter in the group, e.g. for displaying
		 * in a UI. The argument name has to be without the group prefix.
		 * @param pn the name of the parameter to set the label for
		 * @param pd the description for the argument parameter
		 */
		public void setParameterDescription(String pn, ParameterDescription pd) {
			String lpn = pn.substring(pn.lastIndexOf(".") + ".".length());
			if (pd == null) {
				this.parameterNamesToDescriptions.remove(lpn);
				return;
			}
			ParameterDescription epd = this.getParameterDescription(pn, false);
			this.parameterNamesToDescriptions.put(pn, pd);
			if (epd == null)
				return;
			if (pd.getLabel() == null)
				pd.setLabel(epd.getLabel());
			if (pd.getDescription() == null)
				pd.setDescription(epd.getDescription());
			if (pd.getValues() == null)
				pd.setValues(epd.getValues());
			String[] pdValues = pd.getValues();
			if (pdValues == null)
				return;
			for (int v = 0; v < pdValues.length; v++) {
				if (pd.getValueLabel(pdValues[v]) == null)
					pd.setValueLabel(pdValues[v], epd.getValueLabel(pdValues[v]));
			}
		}
		
		private ParameterDescription getParameterDescription(String lpn, boolean create) {
			ParameterDescription pd = ((ParameterDescription) this.parameterNamesToDescriptions.get(lpn));
			if ((pd == null) && create) {
				pd = new ParameterDescription(this.parameterNamePrefix + "." + lpn);
				this.parameterNamesToDescriptions.put(lpn, pd);
			}
			return pd;
		}
		
		/**
		 * Retrieve the label for a parameter in the group. The argument name
		 * has to be without the group prefix.
		 * @param pn the name of the parameter to obtain the label for
		 * @return the label for the argument parameter
		 */
		public String getParamLabel(String pn) {
			ParameterDescription pd = this.getParameterDescription(pn, false);
			return ((pd == null) ? null : pd.getLabel());
		}
		
		/**
		 * Provide a label for a parameter in the group, e.g. for displaying
		 * in a UI. The argument name has to be without the group prefix.
		 * @param pn the name of the parameter to set the label for
		 * @param label the label for the argument parameter
		 */
		public void setParamLabel(String pn, String label) {
			ParameterDescription pd = this.getParameterDescription(pn, (label != null));
			if (pd != null)
				pd.setLabel(label);
		}
		
		/**
		 * Retrieve the description for a parameter in the group. The argument
		 * name has to be without the group prefix.
		 * @param pn the name of the parameter to obtain the label for
		 * @return the description for the argument parameter
		 */
		public String getParamDescription(String pn) {
			ParameterDescription pd = this.getParameterDescription(pn, false);
			return ((pd == null) ? null : pd.getDescription());
		}
		
		/**
		 * Provide a description for a parameter in the group, e.g. for displaying
		 * in a UI. The argument name has to be without the group prefix.
		 * @param pn the name of the parameter to set the label for
		 * @param description the description for the argument parameter
		 */
		public void setParamDescription(String pn, String description) {
			ParameterDescription pd = this.getParameterDescription(pn, (description != null));
			if (pd != null)
				pd.setDescription(description);
		}
		
		/**
		 * Retrieve the default value of a parameter in the group. If there is
		 * a list of permitted values, it must contain any default value.
		 * @param pn the name of the parameter to obtain the default value for
		 * @return the default value of the parameter
		 */
		public String getParamDefaultValue(String pn) {
			ParameterDescription pd = this.getParameterDescription(pn, false);
			return ((pd == null) ? null : pd.getDefaultValue());
		}
		
		/**
		 * Provide the default value for a parameter in the group. If there is
		 * a list of permitted values for the same parameter, the default value
		 * must be contained in it. If it is not, the list of permitted values
		 * is erased.
		 * @param pn the name of the parameter to set the default value for
		 * @param defaultValue the default value to set
		 */
		public void setParamDefaultValue(String pn, String defaultValue) {
			ParameterDescription pd = this.getParameterDescription(pn, (defaultValue != null));
			if (pd != null)
				pd.setDefaultValue(defaultValue);
		}
		
		/**
		 * Retrieve a list of permitted values for a parameter in the group.
		 * This is particularly useful if there are only few meaningful values.
		 * The argument name has to be without the group prefix.
		 * @param pn the name of the parameter to obtain the values for
		 * @return the values for the argument parameter
		 */
		public String[] getParamValues(String pn) {
			ParameterDescription pd = this.getParameterDescription(pn, false);
			return ((pd == null) ? null : pd.getValues());
		}
		
		/**
		 * Provide a list of permitted values for a parameter in the group,
		 * e.g. for displaying a selection in a UI. This is particularly useful
		 * if there are only few meaningful values. The argument name has to be
		 * without the group prefix. If there is a default value for the same
		 * parameter, it must be contained in the argument list, or it is
		 * erased.
		 * @param pn the name of the parameter to set the values for
		 * @param values the values for the argument parameter
		 */
		public void setParamValues(String pn, String[] values) {
			ParameterDescription pd = this.getParameterDescription(pn, (values != null));
			if (pd != null)
				pd.setValues(values);
		}
		
		/**
		 * Retrieve the label for a specific value of a parameter in the group.
		 * The argument parameter name has to be without the group prefix.
		 * @param pn the name of the parameter to obtain the label for
		 * @param pv the parameter value to obtain the label for
		 * @return the label for the argument parameter
		 */
		public String getParamValueLabel(String pn, String pv) {
			ParameterDescription pd = this.getParameterDescription(pn, false);
			return ((pd == null) ? null : pd.getValueLabel(pv));
		}
		
		/**
		 * Provide a label for a specific value of a parameter in the group,
		 * e.g. for displaying in a UI. The argument name has to be without the
		 * group prefix.
		 * @param pn the name of the parameter to set the label for
		 * @param pv the parameter value to obtain the label for
		 * @param label the label for the argument parameter
		 */
		public void setParamValueLabel(String pn, String pv, String label) {
			ParameterDescription pd = this.getParameterDescription(pn, (label != null));
			if (pd != null)
				pd.setValueLabel(pv, label);
		}
		
		/**
		 * Mark a parameter as required for the whole parameter group to be
		 * useful. This is intended to help semantics in a configuration UI,
		 * e.g. by highlighting respective input fields. The argument name has
		 * to be without the group prefix.
		 * @param pn the name of the parameter to mark as required
		 */
		public void setParamRequired(String pn) {
			ParameterDescription pd = this.getParameterDescription(pn, false);
			if (pd != null)
				pd.setRequired();
		}
		
		/**
		 * Check if a parameter is required for the whole parameter group to be
		 * useful. This is intended to help semantics in a configuration UI,
		 * e.g. by highlighting respective input fields. The argument name has
		 * to be without the group prefix.
		 * @param pn the name of the parameter to check
		 * @return true if the parameter is required
		 */
		public boolean isParamRequired(String pn) {
			ParameterDescription pd = this.getParameterDescription(pn, false);
			return ((pd != null) && pd.isRequired());
		}
	}
	
	/**
	 * Descriptor object for an individual document style parameters. This
	 * class enables consumers of these parameters to provide labels,
	 * explanations, and custom testing facilities for the parameters they use,
	 * for instance in order to augment editing facilities with more user
	 * friendly information about the semantics of the parameters they use.<br/>
	 * Attention: some parameters might be used by multiple consumers. In such
	 * cases, the code providing a parameter description should check for any
	 * existing descriptions to augment first.
	 * 
	 * @author sautter
	 */
	/**
	 * @author sautter
	 *
	 */
	public static class ParameterDescription {
		
		/** the full name of the parameter, including the prefix */
		public final String fullName;
		
		/** the group-local name of the parameter, excluding the prefix */
		public final String localName;
		
		private String label;
		private String description;
		
		private String defaultValue = null;
		private String[] values = null;
		private Properties valuesToLabels = new Properties();
		
		private boolean required = false;
		private HashMap valuesToRequiredParameters = null;
		private HashMap valuesToExcludedParameters = null;
		
		/** Constructor
		 * @param fpn the full name of the parameter, including the prefix
		 */
		public ParameterDescription(String fpn) {
			this.fullName = fpn;
			this.localName = this.fullName.substring(this.fullName.lastIndexOf(".") + ".".length());
		}
		
		/**
		 * Retrieve the label of the parameter.
		 * @return the label
		 */
		public String getLabel() {
			return this.label;
		}
		
		/**
		 * Provide a label for the parameter, e.g. for displaying in a UI.
		 * @param label the label to set
		 */
		public void setLabel(String label) {
			this.label = label;
		}
		
		/**
		 * Retrieve the description of the parameter.
		 * @return the description
		 */
		public String getDescription() {
			return this.description;
		}
		
		/**
		 * Provide a description for the parameter, e.g. for displaying in a UI.
		 * @param description the description to set
		 */
		public void setDescription(String description) {
			this.description = description;
		}
		
		/**
		 * Retrieve the default value of the parameter. If there is a list of
		 * permitted values, it must contain any default value.
		 * @return the default value of the parameter
		 */
		public String getDefaultValue() {
			return this.defaultValue;
		}
		
		/**
		 * Provide the default value of the parameter. If there is a list of
		 * permitted values, the default value must be contained in it. If it
		 * is not, the list of permitted values is erased.
		 * @param defaultValue the default value to set
		 */
		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
			if ((this.values == null) || (this.defaultValue == null))
				return;
			for (int v = 0; v < this.values.length; v++) {
				if (this.defaultValue.equals(this.values[v]))
					return;
			}
			this.values = null;
		}
		
		/**
		 * Retrieve a list of permitted values for the parameter. This is
		 * particularly useful if there are only few meaningful values.
		 * @return the values for the parameter
		 */
		public String[] getValues() {
			return this.values;
		}
		
		/**
		 * Provide a list of permitted values for the parameter, e.g. for
		 * displaying a selection in a UI. This is particularly useful if there
		 * are only few meaningful values. If there is a non-null default value
		 * for the parameter, it must be contained in the argument list, or it
		 * is erased
		 * @param values the values for the parameter
		 */
		public void setValues(String[] values) {
			this.values = values;
			if ((this.values == null) || (this.defaultValue == null))
				return;
			for (int v = 0; v < this.values.length; v++) {
				if (this.defaultValue.equals(this.values[v]))
					return;
			}
			this.defaultValue = null;
		}
		
		/**
		 * Retrieve the label for a specific value of the parameter.
		 * @param pv the parameter value to obtain the label for
		 * @return the label for the argument parameter name
		 */
		public String getValueLabel(String pv) {
			return this.valuesToLabels.getProperty(pv);
		}
		
		/**
		 * Provide a label for a specific value of the parameter, e.g. for
		 * displaying in a UI.
		 * @param pv the parameter value to set the label for
		 * @param label the label for the argument parameter value
		 */
		public void setValueLabel(String pv, String label) {
			if (label == null)
				this.valuesToLabels.remove(pv);
			else this.valuesToLabels.setProperty(pv, label);
		}
		
		/**
		 * Mark the parameter as required for the whole parameter group to be
		 * useful. This is intended to help semantics in a configuration UI,
		 * e.g. by highlighting respective input fields.
		 */
		public void setRequired() {
			this.required = true;
		}
		
		/**
		 * Check if the parameter is required for the whole parameter group to
		 * be useful. This is intended to help semantics in a configuration UI,
		 * e.g. by highlighting respective input fields.
		 * @return true if the parameter is required
		 */
		public boolean isRequired() {
			return this.required;
		}
		
		/**
		 * Indicate that a value of this parameter requires another parameter.
		 * Setting the argument value to null indicates using this parameter
		 * generally requires the argument parameter. This is intended to help
		 * reflecting dependencies in a configuration UI, e.g. by enabling and
		 * disabling input fields.
		 * @param pv the value of this parameter requiring the other parameter
		 * @param rpn the name of the required parameter
		 */
		public void addRequiredParameter(String pv, String rpn) {
			if (rpn == null)
				return;
			if (this.valuesToRequiredParameters == null)
				this.valuesToRequiredParameters = new HashMap();
			HashSet rpns = ((HashSet) this.valuesToRequiredParameters.get(pv));
			if (rpns == null) {
				rpns = new HashSet();
				this.valuesToRequiredParameters.put(pv, rpns);
			}
			rpns.add(rpn);
		}
		
		/**
		 * Check if a specific value of this parameter requires another
		 * parameter. This is intended to help reflecting dependencies in a
		 * configuration UI, e.g. by enabling and disabling input fields.
		 * @param pv the value of this parameter
		 * @param cpn the parameter name to check
		 * @return true if the argument value of this parameter requires the
		 *        argument parameter
		 */
		public boolean requiresParameter(String pv, String cpn) {
			if (cpn == null)
				return false;
			if (this.valuesToRequiredParameters == null)
				return false;
			HashSet rpns = ((HashSet) this.valuesToRequiredParameters.get(pv));
			if ((rpns != null) && (rpns.contains(cpn)))
				return true;
			return ((pv == null) ? false : this.requiresParameter(null, cpn));
		}
		
		/**
		 * Get the names of the other parameters required for this parameter to
		 * be useful. This is intended to help reflecting dependencies in a
		 * configuration UI, e.g. by enabling and disabling input fields.
		 * @return an array holding the names of the required parameters
		 */
		public String[] getRequiredParameters() {
			return this.getRequiredParameters(null);
		}
		
		/**
		 * Get the names of the other parameters required for a specific value
		 * of this parameter. This is intended to help reflecting dependencies
		 * in a configuration UI, e.g. by enabling and disabling input fields.
		 * @param pv the value of this parameter
		 * @return an array holding the names of the required parameters
		 */
		public String[] getRequiredParameters(String pv) {
			if (this.valuesToRequiredParameters == null)
				return null;
			HashSet rpns = ((HashSet) this.valuesToRequiredParameters.get(pv));
			return ((rpns == null) ? null : ((String[]) rpns.toArray(new String[rpns.size()])));
		}
		
		/**
		 * Indicate that a value of this parameter excludes using another
		 * parameter. Setting the argument value to null indicates using this
		 * parameter generally excludes the argument parameter. This is
		 * intended to help reflecting dependencies in a configuration UI,
		 * e.g. by enabling and disabling input fields.
		 * @param pv the value of this parameter excluding the other parameter
		 * @param epn the name of the excluded parameter
		 */
		public void addExcludedParameter(String pv, String epn) {
			if (this.valuesToExcludedParameters == null)
				this.valuesToExcludedParameters = new HashMap();
			HashSet epns = ((HashSet) this.valuesToExcludedParameters.get(pv));
			if (epns == null) {
				epns = new HashSet();
				this.valuesToExcludedParameters.put(pv, epns);
			}
			epns.add(epn);
		}
		
		/**
		 * Check if a specific value of this parameter excludes using another
		 * parameter. This is intended to help reflecting dependencies in a
		 * configuration UI, e.g. by enabling and disabling input fields.
		 * @param pv the value of this parameter
		 * @param cpn the parameter name to check
		 * @return true if the argument value of this parameter excludes using
		 *        the argument parameter
		 */
		public boolean excludesParameter(String pv, String cpn) {
			if (cpn == null)
				return false;
			if (this.valuesToExcludedParameters == null)
				return false;
			HashSet epns = ((HashSet) this.valuesToExcludedParameters.get(pv));
			if ((epns != null) && (epns.contains(cpn)))
				return true;
			return ((pv == null) ? false : this.excludesParameter(null, cpn));
		}
		
		/**
		 * Get the names of the other parameters excluded by this parameter.
		 * This is intended to help reflecting dependencies in a configuration
		 * UI, e.g. by enabling and disabling input fields.
		 * @return an array holding the names of the excluded parameters
		 */
		public String[] getExcludedParameters() {
			return this.getExcludedParameters(null);
		}
		
		/**
		 * Get the names of the other parameters excluded by a specific value
		 * of this parameter. This is intended to help reflecting dependencies
		 * in a configuration UI, e.g. by enabling and disabling input fields.
		 * @param pv the value of this parameter
		 * @return an array holding the names of the excluded parameters
		 */
		public String[] getExcludedParameters(String pv) {
			if (this.valuesToExcludedParameters == null)
				return null;
			HashSet epns = ((HashSet) this.valuesToExcludedParameters.get(pv));
			return ((epns == null) ? null : ((String[]) epns.toArray(new String[epns.size()])));
		}
	}
	
	/**
	 * Interface to be implemented by descriptions of parameters or parameter
	 * groups that can provide some for of testing functionality, e.g. some
	 * output that informs a user about how the current value of a parameter
	 * or value combination in a parameter group behaves.
	 * 
	 * @author sautter
	 */
	public static interface TestableElement {
		
		/**
		 * Test the parameter or parameter group described by the implementor.
		 * @param paramGroup the parameter group containing the current values
		 */
		public abstract void test(Properties paramGroup);
	}
	
	private static Map descriptionsByParameterGroupPrefix = Collections.synchronizedMap(new TreeMap());
	
	/**
	 * Retrieve a description for a parameter group with a given prefix.
	 * @param pnp the parameter group prefix to obtain a description for
	 */
	public static ParameterGroupDescription getParameterGroupDescription(String pnp) {
		return ((ParameterGroupDescription) descriptionsByParameterGroupPrefix.get(pnp));
	}
	
	/**
	 * Add a parameter group description.
	 * @param pgd the parameter group description to add
	 */
	public static void addParameterGroupDescription(ParameterGroupDescription pgd) {
		if ((pgd != null) && (pgd.parameterNamePrefix != null))
			descriptionsByParameterGroupPrefix.put(pgd.parameterNamePrefix, pgd);
	}
	
	/**
	 * Remove a parameter group description.
	 * @param pgd the parameter group description to remove
	 */
	public static void removeParameterGroupDescription(ParameterGroupDescription pgd) {
		if ((pgd != null) && (pgd.parameterNamePrefix != null))
			descriptionsByParameterGroupPrefix.remove(pgd.parameterNamePrefix);
	}
	
	/** the default resolution of 72 DPI, i.e., the default typographical Point */
	public static int DEFAULT_DPI = 72;
	
	private final String prefix;
	
	/* keep the constructors private for now */
	private DocumentStyle(Properties defaults) {
		super((defaults == null) ? new Properties() : defaults);
		this.prefix = null;
	}
	
	/* keep the constructors private for now */
	private DocumentStyle(DocumentStyle parent, String prefix) {
		super(parent.defaults);
		if ((prefix.length() != 0) && !prefix.endsWith("."))
			prefix += ".";
		this.prefix = (((parent.prefix == null) ? "" : parent.prefix) + prefix);
	}
	
	/**
	 * Retrieve the wrapped parameter list for modification.
	 * @return the wrapped parameter list
	 */
	public Properties getParameters() {
		return this.defaults;
	}
	
	/**
	 * Obtain a sublist of this document style parameter list, comprising all
	 * parameters whose name starts with the argument prefix plus a dot. Only
	 * the name part after the prefix needs to be specified to retrieve the
	 * parameters from the sublist. This is useful for eliminating the need to
	 * specify full parameter names in client code.
	 * @param prefix the prefix for the sublist
	 * @return a sublist with the argument prefix
	 */
	public DocumentStyle getSubset(String prefix) {
		return (((prefix == null) || (prefix.trim().length() == 0)) ? this : new DocumentStyle(this, prefix.trim()));
	}
	
	/* (non-Javadoc)
	 * @see java.util.Properties#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, String.class);
		return this.defaults.getProperty(key);
	}
	
	/* (non-Javadoc)
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public String getProperty(String key, String defVal) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, String.class);
		return this.defaults.getProperty(key, defVal);
	}
	
	/**
	 * Retrieve an integer property. If the argument key is not mapped to any
	 * value, or if its mapped value fails to parse into an integer, this
	 * method returns the argument default.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public int getIntProperty(String key, int defVal) {
		return this.getIntProperty(key, defVal, DEFAULT_DPI);
	}
	
	/**
	 * Retrieve an integer property. If the argument key is not mapped to any
	 * value, or if its mapped value fails to parse into an integer, this
	 * method returns the argument default. If the argument key does map to a
	 * valid integer value, the latter is scaled to the argument DPI number,
	 * assuming a base resolution of 72 DPI. The default value argument is
	 * never scaled.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @param dpi the DPI to scale to, assuming the value to be in 72 DPI
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public int getIntProperty(String key, int defVal, int dpi) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, Integer.class);
		String valStr = this.defaults.getProperty(key);
		if ((valStr != null) && (valStr.trim().length() != 0)) try {
			int val = Integer.parseInt(valStr);
			if ((dpi < 1) || (dpi == DEFAULT_DPI))
				return val;
			else return scaleInt(val, DEFAULT_DPI, dpi);
		} catch (NumberFormatException nfe) {}
		return defVal;
	}
	
	/**
	 * Retrieve a float (floating point) property. If the argument key is not
	 * mapped to any value, or if its mapped value fails to parse into a float,
	 * this method returns the argument default.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public float getFloatProperty(String key, float defVal) {
		return this.getFloatProperty(key, defVal, DEFAULT_DPI);
	}
	
	/**
	 * Retrieve a float (floating point) property. If the argument key is not
	 * mapped to any value, or if its mapped value fails to parse into a float,
	 * this method returns the argument default. If the argument key does map
	 * to a valid float value, the latter is scaled to the argument DPI number,
	 * assuming a base resolution of 72 DPI. The default value argument is
	 * never scaled.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @param dpi the DPI to scale to, assuming the value to be in 72 DPI
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public float getFloatProperty(String key, float defVal, int dpi) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, Float.class);
		String valStr = this.defaults.getProperty(key);
		if ((valStr != null) && (valStr.trim().length() != 0)) try {
			float val = Float.parseFloat(valStr);
			if ((dpi < 1) || (dpi == DEFAULT_DPI))
				return val;
			else return scaleFloat(val, DEFAULT_DPI, dpi);
		} catch (NumberFormatException nfe) {}
		return defVal;
	}
	
	/**
	 * Retrieve a double (floating point) property. If the argument key is not
	 * mapped to any value, or if its mapped value fails to parse into a double,
	 * this method returns the argument default.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public double getDoubleProperty(String key, double defVal) {
		return this.getDoubleProperty(key, defVal, DEFAULT_DPI);
	}
	
	/**
	 * Retrieve a double (floating point) property. If the argument key is not
	 * mapped to any value, or if its mapped value fails to parse into a double,
	 * this method returns the argument default. If the argument key does map
	 * to a valid double value, the latter is scaled to the argument DPI number,
	 * assuming a base resolution of 72 DPI. The default value argument is
	 * never scaled.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @param dpi the DPI to scale to, assuming the value to be in 72 DPI
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public double getDoubleProperty(String key, double defVal, int dpi) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, Double.class);
		String valStr = this.defaults.getProperty(key);
		if ((valStr != null) && (valStr.trim().length() != 0)) try {
			double val = Double.parseDouble(valStr);
			if ((dpi < 1) || (dpi == DEFAULT_DPI))
				return val;
			else return scaleDouble(val, DEFAULT_DPI, dpi);
		} catch (NumberFormatException nfe) {}
		return defVal;
	}
	
	/**
	 * Retrieve an boolean property. If the argument key is not mapped to any
	 * value, or if its mapped value fails to parse into a boolean, this
	 * method returns the argument default.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public boolean getBooleanProperty(String key, boolean defVal) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, Boolean.class);
		String valStr = this.defaults.getProperty(key);
		return (((valStr == null) || (valStr.trim().length() == 0)) ? defVal : Boolean.parseBoolean(valStr));
	}
	
	/**
	 * Retrieve a bounding box property. If the argument key is not mapped to
	 * any value, or if its mapped value fails to parse into a bounding box,
	 * this method returns the argument default.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public BoundingBox getBoxProperty(String key, BoundingBox defVal) {
		return this.getBoxProperty(key, defVal, DEFAULT_DPI);
	}
	
	/**
	 * Retrieve a bounding box property. If the argument key is not mapped to
	 * any value, or if its mapped value fails to parse into a bounding box,
	 * this method returns the argument default. If the argument key does map
	 * to a valid bounding box value, the latter is scaled to the argument DPI
	 * number, assuming a base resolution of 72 DPI. The default value argument
	 * is never scaled.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @param dpi the DPI to scale to, assuming the value to be in 72 DPI
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public BoundingBox getBoxProperty(String key, BoundingBox defVal, int dpi) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, BoundingBox.class);
		String valStr = this.defaults.getProperty(key);
		if ((valStr != null) && (valStr.trim().length() != 0)) try {
			BoundingBox val = BoundingBox.parse(valStr);
			if ((dpi < 1) || (dpi == DEFAULT_DPI))
				return val;
			else return scaleBox(val, DEFAULT_DPI, dpi);
		} catch (IllegalArgumentException iae) {}
		return defVal;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public String[] getListProperty(String key, String[] defVal, String sep) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, stringListClass);
		String valStr = this.defaults.getProperty(key);
		if ((valStr == null) || (valStr.trim().length() == 0))
			return defVal;
		else return valStr.split("\\s*" + RegExUtils.escapeForRegEx(sep) + "\\s*");
	}
	
	/**
	 * Retrieve an integer list property. If the argument key is not mapped to
	 * any value, or if its mapped value fails to parse into an integer list,
	 * this method returns the argument default.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public int[] getIntListProperty(String key, int[] defVal) {
		return this.getIntListProperty(key, defVal, DEFAULT_DPI);
	}
	
	/**
	 * Retrieve an integer list property. If the argument key is not mapped to
	 * any value, or if its mapped value fails to parse into an integer list,
	 * this method returns the argument default. If the argument key does map
	 * to a valid integer list value, the latter is scaled to the argument DPI
	 * number, assuming a base resolution of 72 DPI. The default value argument
	 * is never scaled.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @param dpi the DPI to scale to, assuming the value to be in 72 DPI
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public int[] getIntListProperty(String key, int[] defVal, int dpi) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, intListClass);
		String valStr = this.defaults.getProperty(key);
		if ((valStr == null) || (valStr.trim().length() == 0))
			return defVal;
		String[] valStrs = valStr.split("[^0-9]+");
		int[] vals = new int[valStrs.length];
		try {
			for (int v = 0; v < valStrs.length; v++) {
				vals[v] = Integer.parseInt(valStrs[v]);
				if ((0 < dpi) && (dpi != DEFAULT_DPI))
					vals[v] = scaleInt(vals[v], DEFAULT_DPI, dpi);
			}
			return vals;
		} catch (NumberFormatException nfe) {}
		return defVal;
	}
	
	/**
	 * Retrieve a float (floating point) list property. If the argument key is
	 * not mapped to any value, or if its mapped value fails to parse into a
	 * float list, this method returns the argument default.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public float[] getFloatListProperty(String key, float[] defVal) {
		return this.getFloatListProperty(key, defVal, DEFAULT_DPI);
	}
	
	/**
	 * Retrieve a float (floating point) list property. If the argument key is
	 * not mapped to any value, or if its mapped value fails to parse into a
	 * float list, this method returns the argument default. If the argument
	 * key does map to a valid float list value, the latter is scaled to the
	 * argument DPI number, assuming a base resolution of 72 DPI. The default
	 * value argument is never scaled.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @param dpi the DPI to scale to, assuming the value to be in 72 DPI
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public float[] getFloatListProperty(String key, float[] defVal, int dpi) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, floatListClass);
		String valStr = this.defaults.getProperty(key);
		if ((valStr == null) || (valStr.trim().length() == 0))
			return defVal;
		String[] valStrs = valStr.split("[^0-9\\,\\.]+");
		float[] vals = new float[valStrs.length];
		try {
			for (int v = 0; v < valStrs.length; v++) {
				vals[v] = Float.parseFloat(valStrs[v]);
				if ((0 < dpi) && (dpi != DEFAULT_DPI))
					vals[v] = scaleFloat(vals[v], DEFAULT_DPI, dpi);
			}
			return vals;
		} catch (NumberFormatException nfe) {}
		return defVal;
	}
	
	/**
	 * Retrieve a double (floating point) list property. If the argument key is
	 * not mapped to any value, or if its mapped value fails to parse into a
	 * double list, this method returns the argument default.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public double[] getDoubleListProperty(String key, double[] defVal) {
		return this.getDoubleListProperty(key, defVal, DEFAULT_DPI);
	}
	
	/**
	 * Retrieve a double (floating point) list property. If the argument key is
	 * not mapped to any value, or if its mapped value fails to parse into a
	 * double list, this method returns the argument default. If the argument
	 * key does map to a valid double list value, the latter is scaled to the
	 * argument DPI number, assuming a base resolution of 72 DPI. The default
	 * value argument is never scaled.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @param dpi the DPI to scale to, assuming the value to be in 72 DPI
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public double[] getDoubleListProperty(String key, double[] defVal, int dpi) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, doubleListClass);
		String valStr = this.defaults.getProperty(key);
		if ((valStr == null) || (valStr.trim().length() == 0))
			return defVal;
		String[] valStrs = valStr.split("[^0-9\\,\\.]+");
		double[] vals = new double[valStrs.length];
		try {
			for (int v = 0; v < valStrs.length; v++) {
				vals[v] = Double.parseDouble(valStrs[v]);
				if ((0 < dpi) && (dpi != DEFAULT_DPI))
					vals[v] = scaleDouble(vals[v], DEFAULT_DPI, dpi);
			}
			return vals;
		} catch (NumberFormatException nfe) {}
		return defVal;
	}
	
	/**
	 * Retrieve an boolean list property. If the argument key is not mapped to
	 * any value, or if its mapped value fails to parse into a boolean list,
	 * this method returns the argument default.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public boolean[] getBooleanListProperty(String key, boolean[] defVal) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, booleanListClass);
		String valStr = this.defaults.getProperty(key);
		if ((valStr == null) || (valStr.trim().length() == 0))
			return defVal;
		String[] valStrs = valStr.split("[^a-zA-Z]+");
		boolean[] vals = new boolean[valStrs.length];
		try {
			for (int v = 0; v < valStrs.length; v++)
				vals[v] = Boolean.parseBoolean(valStrs[v]);
			return vals;
		} catch (NumberFormatException nfe) {}
		return defVal;
	}
	
	/**
	 * Retrieve a bounding box list property. If the argument key is not mapped
	 * to any value, or if its mapped value fails to parse into a bounding box
	 * list, this method returns the argument default.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public BoundingBox[] getBoxListProperty(String key, BoundingBox[] defVal) {
		return this.getBoxListProperty(key, defVal, DEFAULT_DPI);
	}
	
	/**
	 * Retrieve a bounding box list property. If the argument key is not mapped
	 * to any value, or if its mapped value fails to parse into a bounding box
	 * list, this method returns the argument default. If the argument key does
	 * map to a valid bounding box list value, the latter is scaled to the
	 * argument DPI number, assuming a base resolution of 72 DPI. The default
	 * value argument is never scaled.
	 * @param key the hashtable key
	 * @param defVal a default value
	 * @param dpi the DPI to scale to, assuming the value to be in 72 DPI
	 * @return the value in this property list with the specified key value.
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public BoundingBox[] getBoxListProperty(String key, BoundingBox[] defVal, int dpi) {
		if (this.prefix != null)
			key = (this.prefix + key);
		parameterNamesToClasses.put(key, boxListClass);
		String valStr = this.defaults.getProperty(key);
		if ((valStr == null) || (valStr.trim().length() == 0))
			return defVal;
		String[] valStrs = valStr.split("[^0-9\\,\\[\\]]+");
		BoundingBox[] vals = new BoundingBox[valStrs.length];
		try {
			for (int v = 0; v < valStrs.length; v++) {
				vals[v] = BoundingBox.parse(valStrs[v]);
				if ((0 < dpi) && (dpi != DEFAULT_DPI))
					vals[v] = scaleBox(vals[v], DEFAULT_DPI, dpi);
			}
			return vals;
		} catch (IllegalArgumentException iae) {}
		return defVal;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Properties#propertyNames()
	 */
	public Enumeration propertyNames() {
		if (this.prefix == null)
			return this.defaults.propertyNames();
		final Enumeration allPropertyNames = this.defaults.propertyNames();
		return new Enumeration() {
			private Object next = null;
			public boolean hasMoreElements() {
				if (this.next != null)
					return true;
				while (allPropertyNames.hasMoreElements()) {
					Object next = allPropertyNames.nextElement();
					if (next.toString().startsWith(prefix)) {
						this.next = next;
						return true;
					}
				}
				return false;
			}
			public Object nextElement() {
				Object next = this.next;
				this.next = null;
				return next;
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see java.util.Hashtable#keys()
	 */
	public synchronized Enumeration keys() {
		return this.propertyNames();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Hashtable#toString()
	 */
	public synchronized String toString() {
		return ("DocumentStyle" + this.defaults.toString());
	}
	
	/* (non-Javadoc)
	 * @see java.util.Hashtable#keySet()
	 */
	public Set keySet() {
		return this.defaults.keySet();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Hashtable#size()
	 */
	public synchronized int size() {
		if (this.prefix == null)
			return this.defaults.size();
		int size = 0;
		for (Enumeration pns = this.propertyNames(); pns.hasMoreElements(); pns.nextElement())
			size++;
		return size;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Hashtable#isEmpty()
	 */
	public synchronized boolean isEmpty() {
		return (this.size() == 0);
	}
	
	/* (non-Javadoc)
	 * @see java.util.Hashtable#containsKey(java.lang.Object)
	 */
	public synchronized boolean containsKey(Object key) {
		if (this.prefix == null)
			return this.defaults.containsKey(key);
		else return this.defaults.containsKey(this.prefix + key);
	}
	
	/**
	 * Scale an integer value.
	 * @param val the integer to scale 
	 * @param cDpi the current DPI to scale from
	 * @param tDpi the target DPI to scale to
	 * @return the scaled integer value
	 */
	public static final int scaleInt(int val, int cDpi, int tDpi) {
		return (((val * tDpi) + (cDpi / 2)) / cDpi);
	}
	
	/**
	 * Scale an integer value.
	 * @param val the integer to scale 
	 * @param cDpi the current DPI to scale from
	 * @param tDpi the target DPI to scale to
	 * @param mode either of R(ound), F(loor), and C(eiling)
	 * @return the scaled integer value
	 */
	public static final int scaleInt(int val, int cDpi, int tDpi, char mode) {
		int moduloAmortizer;
		if (mode == 'F')
			moduloAmortizer = 0;
		else if (mode == 'C')
			moduloAmortizer = (cDpi - 1);
		else moduloAmortizer = (cDpi / 2);
		return (((val * tDpi) + moduloAmortizer) / cDpi);
	}
	
	/**
	 * Scale a float value.
	 * @param val the float to scale 
	 * @param cDpi the current DPI to scale from
	 * @param tDpi the target DPI to scale to
	 * @return the scaled float value
	 */
	public static final float scaleFloat(float val, int cDpi, int tDpi) {
		return ((val * tDpi) / cDpi);
	}
	
	/**
	 * Scale a double value.
	 * @param val the integer to scale 
	 * @param cDpi the current DPI to scale from
	 * @param tDpi the target DPI to scale to
	 * @return the scaled double value
	 */
	public static final double scaleDouble(double val, int cDpi, int tDpi) {
		return ((val * tDpi) / cDpi);
	}
	
	/**
	 * Scale an integer value.
	 * @param val the integer to scale 
	 * @param cDpi the current DPI to scale from
	 * @param tDpi the target DPI to scale to
	 * @return the scaled integer
	 */
	public static final BoundingBox scaleBox(BoundingBox val, int cDpi, int tDpi) {
		return new BoundingBox(
			scaleInt(val.left, cDpi, tDpi),
			scaleInt(val.right, cDpi, tDpi),
			scaleInt(val.top, cDpi, tDpi),
			scaleInt(val.bottom, cDpi, tDpi)
		);
	}
	
	/**
	 * Scale an integer value.
	 * @param val the integer to scale 
	 * @param cDpi the current DPI to scale from
	 * @param tDpi the target DPI to scale to
	 * @param mode either of R(ound), I(nward), and O(utward)
	 * @return the scaled integer
	 */
	public static final BoundingBox scaleBox(BoundingBox val, int cDpi, int tDpi, char mode) {
		if (mode == 'I')
			return new BoundingBox(
					scaleInt(val.left, cDpi, tDpi, 'C'),
					scaleInt(val.right, cDpi, tDpi, 'F'),
					scaleInt(val.top, cDpi, tDpi, 'C'),
					scaleInt(val.bottom, cDpi, tDpi, 'F')
				);
		else if (mode == 'O')
			return new BoundingBox(
					scaleInt(val.left, cDpi, tDpi, 'F'),
					scaleInt(val.right, cDpi, tDpi, 'C'),
					scaleInt(val.top, cDpi, tDpi, 'F'),
					scaleInt(val.bottom, cDpi, tDpi, 'C')
				);
		else return new BoundingBox(
			scaleInt(val.left, cDpi, tDpi),
			scaleInt(val.right, cDpi, tDpi),
			scaleInt(val.top, cDpi, tDpi),
			scaleInt(val.bottom, cDpi, tDpi)
		);
	}
	
	private static final Class stringListClass;
	private static final Class intListClass;
	private static final Class floatListClass;
	private static final Class doubleListClass;
	private static final Class booleanListClass;
	private static final Class boxListClass;
	static {
		try {
			stringListClass = Class.forName("[L" + String.class.getName() + ";");
			intListClass = Class.forName("[I");
			floatListClass = Class.forName("[F");
			doubleListClass = Class.forName("[D");
			booleanListClass = Class.forName("[Z");
			boxListClass = Class.forName("[L" + BoundingBox.class.getName() + ";");
		}
		catch (ClassNotFoundException cnfe) {
			throw new RuntimeException(cnfe);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String[] strs = {"", ""};
		System.out.println(strs.getClass().getName());
		int[] ints = {1, 2};
		System.out.println(ints.getClass().getName());
		float[] floats = {0.1f, 0.2f};
		System.out.println(floats.getClass().getName());
		double[] doubles = {0.1, 0.2};
		System.out.println(doubles.getClass().getName());
		boolean[] booleans = {true, false};
		System.out.println(booleans.getClass().getName());
		BoundingBox[] boxes = {};
		System.out.println(boxes.getClass().getName());
		
		Class.forName("[Ljava.lang.String;");
		Class.forName("[I");
		Class.forName("[F");
		Class.forName("[D");
		Class.forName("[Z");
		Class.forName("[Lde.uka.ipd.idaho.gamta.util.imaging.BoundingBox;");
	}
}
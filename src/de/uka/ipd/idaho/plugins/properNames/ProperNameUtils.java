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
package de.uka.ipd.idaho.plugins.properNames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.AnnotationIndex;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTree;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTreeNode;
import de.uka.ipd.idaho.gamta.util.constants.NamedEntityConstants;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle.ParameterGroupDescription;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Utility class for handling proper names (both person names and institution
 * and location names) as well as parts thereof.
 * 
 * @person sautter
 */
public class ProperNameUtils implements NamedEntityConstants {

	/**
	 * Container for name style parameters.
	 *
	 * @person sautter
	 */
	public static class NameStyle {
		private String namePartOrder = null;
		
		private String nameCase = null;
		private String lastNameCase = null;
		private String firstNameCase = null;
		
		private String firstNameStyle = null;
		private String initialsStyle = null;
		
		private String affixCommaSeparated = null;
		private String affixPosition = null;
		
		private String infixPosition = null;
		
		private String aliasPosition = null;
		
		private String lastNameMixedCaseRegEx = null;
		private String lastNameAllCapsRegEx = null;
		
		private String firstNameMixedCaseRegEx = null;
		private String firstNameAllCapsRegEx = null;
		
		private String dottedInitialsRegEx = null;
		private String undottedInitialsRegEx = null;
		private String blockedInitialsRegEx = null;
		
		private String personNameAffixRegEx = null;
		
		private Dictionary nameStopWords = null;
		
		private String acronymRegEx = null;
		
		private String properNamePartTitleCaseRegEx = null;
		private String properNamePartAllCapsRegEx = null;
		
		private String aliasQuoter = null;
		
		/**
		 * Retrieve the name part order, one of 'lastName, firstName' ('LnFn'),
		 * 'lastName initials' ('LnIn'), 'firstName lastName' ('FnLn'), and
		 * 'initials lastName' ('InLn'). Combinations are also possible, the
		 * respective abbreviations are to be separated with a '+'.
		 * This property defaults to null, allowing all name part orders in
		 * person name tagging. It has no effect on institution and location
		 * names whose parts are always in the same order.
		 * @return the name part order
		 */
		public String getNamePartOrder() {
			return this.namePartOrder;
		}
		
		/**
		 * Set the name part order, one of 'lastName, firstName' ('LnFn'),
		 * 'lastName initials' ('LnIn'), 'firstName lastName' ('FnLn'), and
		 * 'initials lastName' ('InLn'). Combinations are also possible, the
		 * respective abbreviations are to be separated with a '+'.
		 * If this property is set to null, all name part orders are allowed in
		 * person name tagging. It has no effect on institution and location
		 * names whose parts are always in the same order.
		 * @param namePartOrder the name part order to set
		 */
		public void setNamePartOrder(String namePartOrder) {
			this.namePartOrder = namePartOrder;
		}
		
		/**
		 * Retrieve the case for names as a whole, either title case ('TC') or
		 * all-caps ('AC').
		 * This property defaults to null, allowing both cases. It doubles as a
		 * default for the case of all individual name parts, affecting person
		 * names as well as institution and location names.
		 * @return the name case
		 */
		public String getNameCase() {
			return this.nameCase;
		}
		
		/**
		 * Set the case for names as a whole, either title case ('TC') or
		 * all-caps ('AC').
		 * If this property is set to null, both cases are allowed. It doubles
		 * as a default for the case of all individual name parts, affecting
		 * person names as well as institution and location names.
		 * @param nameCase the name case to set
		 */
		public void setNameCase(String nameCase) {
			this.nameCase = nameCase;
		}
		
		/**
		 * Retrieve the case for person last names, either title case ('TC') or
		 * all-caps ('AC').
		 * This property defaults to null, allowing both cases. It has no
		 * effect on institution and location names as they do not have a
		 * dedicated last name.
		 * @return the last name case
		 */
		public String getLastNameCase() {
			return ((this.lastNameCase == null) ? this.nameCase : this.lastNameCase);
		}
		
		/**
		 * Set the case for person last names, either title case ('TC') or
		 * all-caps ('AC').
		 * If this property is set to null, both cases are allowed. It has no
		 * effect on institution and location names as they do not have a
		 * dedicated last name.
		 * @param lastNameCase the last name case to set
		 */
		public void setLastNameCase(String lastNameCase) {
			this.lastNameCase = lastNameCase;
		}
		
		/**
		 * Retrieve the style for person first names, either initials only
		 * ('I') or full name ('N'), potentially combined with initials.
		 * This property defaults to null, allowing both cases. It has no
		 * effect on institution and location names as they do not have any
		 * first names to be abbreviated to initials.
		 * @return the first name style
		 */
		public String getFirstNameStyle() {
			return this.firstNameStyle;
		}
		
		/**
		 * Set the style for person first names, either initials only ('I') or
		 * full name ('N'), potentially combined with initials.
		 * If this property is set to null, both cases are allowed. It has no
		 * effect on institution and location names as they do not have any
		 * first names to be abbreviated to initials.
		 * @param firstNameStye the first name style to set
		 */
		public void setFirstNameStyle(String firstNameStye) {
			this.firstNameStyle = firstNameStye;
		}
		
		/**
		 * Retrieve the case for person first names, either title case ('TC')
		 * or all-caps ('AC').
		 * This property defaults to null, allowing both cases. It has no
		 * effect on institution and location names as they do not have a
		 * dedicated first name.
		 * @return the first name case
		 */
		public String getFirstNameCase() {
			return ((this.firstNameCase == null) ? this.nameCase : this.firstNameCase);
		}
		
		/**
		 * Set the case for person first names, either title case ('TC') or
		 * all-caps ('AC').
		 * If this property is set to null, both cases are allowed. It has no
		 * effect on institution and location names as they do not have a
		 * dedicated first name.
		 * @param firstNameCase the first name case to set
		 */
		public void setFirstNameCase(String firstNameCase) {
			this.firstNameCase = firstNameCase;
		}
		
		/**
		 * Retrieve the style initials are given on person names, either of
		 * with dots after each one ('D'), without dots ('U'), and as a block
		 * ('B').
		 * This property defaults to null, allowing all three styles. It has no
		 * effect on institution and location names as they do not have any
		 * first names to be abbreviated to initials.
		 * @return the initials style
		 */
		public String getInitialsStyle() {
			return this.initialsStyle;
		}
		
		/**
		 * Set the style initials are given on person names, either of with
		 * dots after each one ('D'), without dots ('U'), and as a block ('B').
		 * If this property is set to null, all three styles are allowed. It
		 * has no effect on institution and location names as they do not have
		 * any first names to be abbreviated to initials.
		 * @param initialsStyle the initials style to set
		 */
		public void setInitialsStyle(String initialsStyle) {
			this.initialsStyle = initialsStyle;
		}
		
		/**
		 * Retrieve whether or not person name affixes are comma separated from
		 * the main name, i.e. 'John Doe, Jr.' vs. 'John Doe Jr.'.
		 * This property defaults to null, allowing both cases. It has no
		 * effect on institution and location names as they do not have any
		 * such affixes.
		 * @return 'Y' if person name affixes are comma separated, 'N' otherwise
		 */
		public String getAffixCommaSeparated() {
			return this.affixCommaSeparated;
		}
		
		/**
		 * Specify whether or not person name affixes are comma separated from
		 * the main name, i.e. 'John Doe, Jr.' vs. 'John Doe Jr.'.
		 * If this property is set to null, both cases are allowed. It has no
		 * effect on institution and location names as they do not have any
		 * such affixes.
		 * @param affixCommaSeparated the person name affix style to set
		 */
		public void setAffixCommaSeparated(String affixCommaSeparated) {
			this.affixCommaSeparated = affixCommaSeparated;
		}
		
		/**
		 * Retrieve the position of person name affixes, either strictly
		 * attached to the last name ('A', e.g. 'Doe, Jr., John') or tailing
		 * ('T', e.g. 'Doe, John, Jr.').
		 * This property defaults to null, allowing both cases. It has no
		 * effect on institution and location names as they do not have any
		 * such affixes.
		 * @return the affix position to set
		 */
		public String getAffixPosition() {
			return this.affixPosition;
		}
		
		/**
		 * Specify the position of person name affixes, either strictly
		 * attached to the last name ('A', e.g. 'Doe, Jr., John') or tailing
		 * ('T', e.g. 'Doe, John, Jr.').
		 * If this property is set to null, both cases are allowed. It has no
		 * effect on institution and location names as they do not have any
		 * such affixes.
		 * @param affixPosition the affix position to set
		 */
		public void setAffixPosition(String affixPosition) {
			this.affixPosition = affixPosition;
		}
		
		/**
		 * Retrieve the position of person's alias, either strictly attached to
		 * the last name ('A', e.g. '"The Man" Doe, John') or tailing ('T',
		 * e.g. 'Doe, John "The Man"').
		 * This property defaults to null, allowing both cases. It has no
		 * effect on institution and location names as they do not have any
		 * such affixes.
		 * @return the alias position to set
		 */
		public String getAliasPosition() {
			return this.aliasPosition;
		}
		
		/**
		 * Specify the position of person'a alias, either strictly attached to
		 * the last name ('A', e.g. '"The Man" Doe, John') or tailing ('T',
		 * e.g. 'Doe, John "The Man"').
		 * If this property is set to null, both cases are allowed. It has no
		 * effect on institution and location names as they do not have any
		 * such alias interspersements.
		 * @param aliasPosition the alias position to set
		 */
		public void setAliasPosition(String aliasPosition) {
			this.aliasPosition = aliasPosition;
		}
		
		/**
		 * Retrieve the position of person name infixes, either strictly
		 * attached to the last name ('A', e.g. 'van Doe, John') or tailing
		 * ('T', e.g. 'Doe, John van').
		 * This property defaults to null, allowing both cases. It has no
		 * effect on institution and location names as they do not have any
		 * such infixes.
		 * @return the infix position
		 */
		public String getInfixPosition() {
			return this.infixPosition;
		}
		
		/**
		 * Specify the position of person name infixes, either strictly
		 * attached to the last name ('A', e.g. 'van Doe, John') or tailing
		 * ('T', e.g. 'Doe, John van').
		 * If this property is set to null, both cases are allowed. It has no
		 * effect on institution and location names as they do not have any
		 * such infixes.
		 * @param infixPosition the infix position to set
		 */
		public void setInfixPosition(String infixPosition) {
			this.infixPosition = infixPosition;
		}
		
		/**
		 * Retrieve the pattern for person last names in title case.
		 * @return the regular expression pattern for person last names
		 */
		public String getLastNameMixedCaseRegEx() {
			return ((this.lastNameMixedCaseRegEx == null) ? defaultLastNameMixedCaseRegEx : this.lastNameMixedCaseRegEx);
		}
		
		/**
		 * Set the pattern for person last names in title case. If this property
		 * is set to null, the built-in default will be used. Use of this pattern
		 * depends on the return value of <code>getLastNameCase()</code>.
		 * @param lastNameMixedCaseRegEx the pattern for person last names
		 */
		public void setLastNameMixedCaseRegEx(String lastNameMixedCaseRegEx) {
			this.lastNameMixedCaseRegEx = lastNameMixedCaseRegEx;
		}
		
		/**
		 * Retrieve the pattern for person last names in all-caps.
		 * @return the regular expression pattern for person last names in
		 * 	      all-caps
		 */
		public String getLastNameAllCapsRegEx() {
			return ((this.lastNameAllCapsRegEx == null) ? defaultLastNameAllCapsRegEx : this.lastNameAllCapsRegEx);
		}
		
		/**
		 * Set the pattern for person last names in all-caps. If this property
		 * is set to null, the built-in default will be used. Use of this
		 * pattern depends on the return value of <code>getLastNameCase()</code>.
		 * @param lastNameAllCapsRegEx the pattern for person last names in
		 *        all-caps
		 */
		public void setLastNameAllCapsRegEx(String lastNameAllCapsRegEx) {
			this.lastNameAllCapsRegEx = lastNameAllCapsRegEx;
		}
		
		/**
		 * Retrieve the pattern for person first names in title case.
		 * @return the regular expression pattern for person first names
		 */
		public String getFirstNameMixedCaseRegEx() {
			return ((this.firstNameMixedCaseRegEx == null) ? defaultFirstNameMixedCaseRegEx : this.firstNameMixedCaseRegEx);
		}
		
		/**
		 * Set the pattern for person first names in title case. If this property
		 * is set to null, the built-in default will be used. Use of this pattern
		 * depends on the return value of <code>getFirstNameCase()</code>.
		 * @param firstNameMixedCaseRegEx the pattern for person first names
		 */
		public void setFirstNameMixedCaseRegEx(String firstNameMixedCaseRegEx) {
			this.firstNameMixedCaseRegEx = firstNameMixedCaseRegEx;
		}
		
		/**
		 * Retrieve the pattern for person first names in all-caps.
		 * @return the regular expression pattern for person fist names in
		 * 	      all-caps
		 */
		public String getFirstNameAllCapsRegEx() {
			return ((this.firstNameAllCapsRegEx == null) ? defaultFirstNameAllCapsRegEx : this.firstNameAllCapsRegEx);
		}
		
		/**
		 * Set the pattern for person first names in all-caps. If this property
		 * is set to null, the built-in default will be used. Use of this
		 * pattern depends on the return value of <code>getFirstNameCase()</code>.
		 * @param firstNameAllCapsRegEx the pattern for person first names in
		 *        all-caps
		 */
		public void setFirstNameAllCapsRegEx(String firstNameAllCapsRegEx) {
			this.firstNameAllCapsRegEx = firstNameAllCapsRegEx;
		}
		
		/**
		 * Retrieve the pattern for dotted person name initials.
		 * @return the regular expression pattern for dotted person name initials
		 */
		public String getDottedInitialsRegEx() {
			return ((this.dottedInitialsRegEx == null) ? defaultDottedInitialsRegEx : this.dottedInitialsRegEx);
		}
		
		/**
		 * Set the pattern for dotted person name initials. If this property is
		 * set to null, the built-in default will be used. Use of this
		 * pattern depends on the return value of <code>getInitialsStyle()</code>.
		 * @param dottedInitialsRegEx the regular expression pattern for dotted
		 *        person name initials
		 */
		public void setDottedInitialsRegEx(String dottedInitialsRegEx) {
			this.dottedInitialsRegEx = dottedInitialsRegEx;
		}
		
		/**
		 * Retrieve the pattern for non-dotted person name initials.
		 * @return the regular expression pattern for non-dotted person name
		 *        initials
		 */
		public String getUndottedInitialsRegEx() {
			return ((this.undottedInitialsRegEx == null) ? defaultUndottedInitialsRegEx : this.undottedInitialsRegEx);
		}
		
		/**
		 * Set the pattern for un-dotted person name initials. If this property
		 * is set to null, the built-in default will be used. Use of this
		 * pattern depends on the return value of <code>getInitialsStyle()</code>.
		 * @param undottedInitialsRegEx the regular expression pattern for
		 *        non-dotted person name initials
		 */
		public void setUndottedInitialsRegEx(String undottedInitialsRegEx) {
			this.undottedInitialsRegEx = undottedInitialsRegEx;
		}
		
		/**
		 * Retrieve the pattern for blocked person name initials.
		 * @return the regular expression pattern for blocked person name
		 *        initials
		 */
		public String getBlockedInitialsRegEx() {
			return ((this.blockedInitialsRegEx == null) ? defaultBlockedInitialsRegEx : this.blockedInitialsRegEx);
		}
		
		/**
		 * Set the pattern for blocked person name initials. If this property
		 * is set to null, the built-in default will be used. Use of this
		 * pattern depends on the return value of <code>getInitialsStyle()</code>.
		 * @param blockedInitialsRegEx the regular expression pattern for
		 *        blocked person name initials
		 */
		public void setBlockedInitialsRegEx(String blockedInitialsRegEx) {
			this.blockedInitialsRegEx = blockedInitialsRegEx;
		}
		
		/**
		 * Retrieve the pattern for blocked person name affixes.
		 * @return the regular expression pattern for person name affixes
		 */
		public String getPersonNameAffixRegEx() {
			return ((this.personNameAffixRegEx == null) ? defaultPersonNameAffixRegEx : this.personNameAffixRegEx);
		}
		
		/**
		 * Set the pattern for blocked person name initials. If this property
		 * is set to null, the built-in default will be used.
		 * @param personNameAffixRegEx the regular expression pattern for
		 *        person name affixes
		 */
		public void setPersonNameAffixRegEx(String personNameAffixRegEx) {
			this.personNameAffixRegEx = personNameAffixRegEx;
		}
		
		/**
		 * Retrieve the pattern for acronyms.
		 * @return the regular expression pattern acronyms
		 */
		public String getAcronymRegEx() {
			return ((this.acronymRegEx == null) ? defaultAcronymRegEx : this.acronymRegEx);
		}
		
		/**
		 * Set the pattern for acronyms. If this property is set to null, the
		 * built-in default will be used.
		 * @param acronymRegEx the pattern for acronyms
		 */
		public void setAcronymRegEx(String acronymRegEx) {
			this.acronymRegEx = acronymRegEx;
		}
		
		/**
		 * Retrieve the quotation mark enclosing aliases embedded in person
		 * names.
		 * @return the quotation mark enclosing aliases
		 */
		public String getAliasQuoter() {
			return this.aliasQuoter;
		}
		
		/**
		 * Set the quotation mark enclosing aliases embedded in person names.
		 * If this property is set to null, both single and double quotes will
		 * be considered.
		 * @param aliasQuoter the alias quotation mark to set
		 */
		public void setAliasQuoter(String aliasQuoter) {
			this.aliasQuoter = aliasQuoter;
		}
		
		/**
		 * Retrieve the dictionary of name infixes to use. First and foremost,
		 * this concerns person name infixes like 'van', but it also covers the
		 * stop words found in institution and location names.
		 * @return the dictionary of name stop words
		 */
		public Dictionary getNameStopWords() {
			if (this.nameStopWords == null) {
				initStopWords();
				return defaultNameStopWords;
			}
			else return this.nameStopWords;
		}
		
		/**
		 * Retrieve the dictionary of name infixes to use. First and foremost,
		 * this concerns person name infixes like 'van', but it also covers the
		 * stop words found in institution and location names. If this property
		 * is set to null, the built-in default will be used, mostly covering
		 * the stop words found in northern and western European person names.
		 * @param nameStopWords the dictionary of name stop words to use
		 */
		public void setNameStopWords(Dictionary nameStopWords) {
			this.nameStopWords = nameStopWords;
		}

		/**
		 * Retrieve the pattern for (parts of) proper names in title case.
		 * @return the regular expression pattern for (parts of) proper names
		 *        in title case
		 */
		public String getProperNamePartTitleCaseRegEx() {
			return ((this.properNamePartTitleCaseRegEx == null) ? defaultProperNamePartTitleCaseRegEx : this.properNamePartTitleCaseRegEx);
		}

		/**
		 * Set the pattern for (parts of) proper names in title case. If this
		 * property is set to null, the built-in default will be used.
		 * @param properNamePartTitleCaseRegEx the pattern for (parts of) proper
		 *        names in title case to set
		 */
		public void setProperNamePartTitleCaseRegEx(String properNamePartTitleCaseRegEx) {
			this.properNamePartTitleCaseRegEx = properNamePartTitleCaseRegEx;
		}

		/**
		 * Retrieve the pattern for (parts of) proper names in all-caps.
		 * @return the regular expression pattern for (parts of) proper names
		 *        in all-caps
		 */
		public String getProperNamePartAllCapsRegEx() {
			return ((this.properNamePartAllCapsRegEx == null) ? defaultProperNamePartAllCapsRegEx : this.properNamePartAllCapsRegEx);
		}

		/**
		 * Set the pattern for (parts of) proper names in all-caps. If this
		 * property is set to null, the built-in default will be used.
		 * @param properNamePartAllCapsRegEx the pattern for (parts of) proper
		 *        names in all-caps to set
		 */
		public void setProperNamePartAllCapsRegEx(String properNamePartAllCapsRegEx) {
			this.properNamePartAllCapsRegEx = properNamePartAllCapsRegEx;
		}
		
		/**
		 * Create a name style parameter set from a document style template.
		 * @param nameStyle the document style template to read
		 * @return a name style reflecting the argument style template
		 */
		public static NameStyle createFromTemplate(DocumentStyle nameStyle) {
			NameStyle ns = new NameStyle();
			String nameCase = nameStyle.getProperty("nameCase");
			if (titleCase.equals(nameCase) || allCaps.equals(nameCase))
				ns.setNameCase(nameCase);
			String lastNameCase = nameStyle.getProperty("lastNameCase", nameCase);
			if (titleCase.equals(lastNameCase) || allCaps.equals(lastNameCase))
				ns.setLastNameCase(nameCase);
			String firstNameStyle = nameStyle.getProperty("firstNameStyle", null);
			if (fullFirstName.equals(firstNameStyle) || initialsFirstName.equals(firstNameStyle))
				ns.setFirstNameStyle(firstNameStyle);
			String firstNameCase = nameStyle.getProperty("firstNameCase", nameCase);
			if (titleCase.equals(firstNameCase) || allCaps.equals(firstNameCase))
				ns.setFirstNameCase(firstNameCase);
			String initialsStyle = nameStyle.getProperty("initialsStyle"); // dotted, undotted, or block
			if (dottedInitials.equals(initialsStyle) || undottedInitials.equals(initialsStyle) || blockInitials.equals(initialsStyle))
				ns.setInitialsStyle(initialsStyle);
			String affixCommaSeparated = nameStyle.getProperty("affixCommaSeparated");
			if ("Y".equals(affixCommaSeparated) || "N".equals(affixCommaSeparated))
				ns.setAffixCommaSeparated(affixCommaSeparated);
			String affixPosition = nameStyle.getProperty("affixPosition");
			if (attachedPosition.equals(affixPosition) || tailingPosition.equals(affixPosition))
				ns.setAffixPosition(affixPosition);
			String infixPosition = nameStyle.getProperty("infixPosition");
			if (attachedPosition.equals(infixPosition) || tailingPosition.equals(infixPosition))
				ns.setInfixPosition(infixPosition);
			String namePartOrder = nameStyle.getProperty("namePartOrder");
			if (lastNameFirstName.equals(namePartOrder) || lastNameInitials.equals(namePartOrder) || firstNameLastName.equals(namePartOrder) || initialsLastName.equals(namePartOrder))
				ns.setNamePartOrder(namePartOrder);
			return ns;
		}
	}
	
	/**
	 * Add descriptions of the parameters used in name styles to a given
	 * parameter group description. It is up to client code to provide label
	 * and description of the parameter group proper.
	 * @param pgd the parameter group description to amend
	 */
	public static void addParameterDescriptions(ParameterGroupDescription pgd) {
		String[] nameCases = {"", titleCase, allCaps};
		pgd.setParamLabel("nameCase", "Case of Names as a Whole");
		pgd.setParamDescription("nameCase", "The case of names as a whole; if only last names are in all-caps, specify so below.");
		pgd.setParamValues("nameCase", nameCases);
		pgd.setParamValueLabel("nameCase", titleCase, "Title Case");
		pgd.setParamValueLabel("nameCase", allCaps, "All-Caps");
		
		pgd.setParamLabel("lastNameCase", "Case of Last Names");
		pgd.setParamDescription("lastNameCase", "The case of last names; defaults to case of whole name if absent or empty.");
		pgd.setParamValues("lastNameCase", nameCases);
		pgd.setParamValueLabel("lastNameCase", titleCase, "Title Case");
		pgd.setParamValueLabel("lastNameCase", allCaps, "All-Caps");
		
		pgd.setParamLabel("firstNameCase", "Case of First Names");
		pgd.setParamDescription("firstNameCase", "The case of first names; defaults to case of whole name if absent or empty.");
		pgd.setParamValues("firstNameCase", nameCases);
		pgd.setParamValueLabel("firstNameCase", titleCase, "Title Case");
		pgd.setParamValueLabel("firstNameCase", allCaps, "All-Caps");
		
		String[] firstNameStyles = {"", fullFirstName, initialsFirstName};
		pgd.setParamLabel("firstNameStyle", "Style of First Names");
		pgd.setParamDescription("firstNameStyle", "The style of first names, i.e., full names or strictly initials. ('John F. Kennedy' vs. 'J.F. Kennedy')");
		pgd.setParamValues("firstNameStyle", firstNameStyles);
		pgd.setParamValueLabel("firstNameStyle", fullFirstName, "Full First Name");
		pgd.setParamValueLabel("firstNameStyle", initialsFirstName, "Initials Only");
		
		String[] initialsStyles = {"", dottedInitials, undottedInitials, blockInitials};
		pgd.setParamLabel("initialsStyle", "Style of Initials");
		pgd.setParamDescription("initialsStyle", "The style of Initials, i.e., with ('Kennedy, J.F.') or without ('Kennedy, J F') period, or en block ('Kennedy, JF' or even 'Kennedy JF').");
		pgd.setParamValues("initialsStyle", initialsStyles);
		pgd.setParamValueLabel("initialsStyle", dottedInitials, "Individual Periods");
		pgd.setParamValueLabel("initialsStyle", undottedInitials, "No Periods");
		pgd.setParamValueLabel("initialsStyle", blockInitials, "Single Block");
		
		String[] affixCommaSeparateds = {"", "Y", "N"};
		pgd.setParamLabel("affixCommaSeparated", "Are Name Affixes Comma Separated?");
		pgd.setParamDescription("affixCommaSeparated", "Do name affixes like 'Jr.' have a preceding comma separating them from the rest of the name?");
		pgd.setParamValues("affixCommaSeparated", affixCommaSeparateds);
		pgd.setParamValueLabel("affixCommaSeparated", "Y", "Yes");
		pgd.setParamValueLabel("affixCommaSeparated", "N", "No");
		
		String[] affixPositions = {"", attachedPosition, tailingPosition};
		pgd.setParamLabel("affixPosition", "Location of Name Affixes");
		pgd.setParamDescription("affixPosition", "Are name affixes like 'Jr.' strictly attached to the last name, or always at the very end? (only relevant if last name before first name, like in 'Buren, Jr., M.' vs. 'Buren, M., Jr.')");
		pgd.setParamValues("affixPosition", affixPositions);
		pgd.setParamValueLabel("affixPosition", attachedPosition, "Right after Last Name");
		pgd.setParamValueLabel("affixPosition", tailingPosition, "At very End of Name");
		
		String[] infixPositions = {"", attachedPosition, tailingPosition};
		pgd.setParamLabel("infixPosition", "Location of Name Infixes");
		pgd.setParamDescription("infixPosition", "Are name infixes like 'van den' strictly in front of the last name, or strictly after the first name or middel initials? (only relevant if last name before first name, like in 'van Buren, M.' vs. 'Buren, M. van')");
		pgd.setParamValues("infixPosition", infixPositions);
		pgd.setParamValueLabel("infixPosition", attachedPosition, "Always before Last Name");
		pgd.setParamValueLabel("infixPosition", tailingPosition, "Always after First Name");
		
		String[] namePartOrders = {"", lastNameFirstName, lastNameInitials, firstNameLastName, initialsLastName};
		pgd.setParamLabel("namePartOrder", "Overall Order of Name Parts");
		pgd.setParamDescription("namePartOrder", "The overall order of name parts, i.e., the ordering of last name relative to first name and/or initials.");
		pgd.setParamValues("namePartOrder", namePartOrders);
		pgd.setParamValueLabel("namePartOrder", lastNameFirstName, "LastName, FirstName (e.g. 'Kennedy, John F.')");
		pgd.setParamValueLabel("namePartOrder", lastNameInitials, "LastName, Initials (e.g. 'Kennedy, J.F.')");
		pgd.setParamValueLabel("namePartOrder", firstNameLastName, "FirstName LastName (e.g. 'John F. Kennedy')");
		pgd.setParamValueLabel("namePartOrder", initialsLastName, "Initials LastName (e.g. 'J.F. Kennedy')");
	}
	
	private static String defaultFirstNameMixedCaseBaseRegEx = 
			"(" +
				"[A-Z]" +
				"([a-z\\-]*[aeiouy][a-z]*)" +
			")" +
			"|" +
			"(" +
				"[AEIOUY]" +
				"[a-z\\-]+" +
			")";
	private static String defaultFirstNameMixedCaseRegEx = "(" + defaultFirstNameMixedCaseBaseRegEx + ")((\\-|\\s)?" + defaultFirstNameMixedCaseBaseRegEx + ")*";
	
	private static String defaultFirstNameAllCapsBaseRegEx = 
			"(" +
				"[A-Z]" +
				"([A-Z]*[AEIOUY][A-Z]*)" +
			")" +
			"|" +
			"(" +
				"[AEIOUY]" +
				"[A-Z]+" +
			")";
	private static String defaultFirstNameAllCapsRegEx = "(" + defaultFirstNameAllCapsBaseRegEx + ")((\\-|\\s)?" + defaultFirstNameAllCapsBaseRegEx + ")*";
	
	//	TODO overhaul this sucker (maybe put optional part on left in overall initials patterns)
	private static String defaultDottedInitialsBaseRegEx = 
		"(" +
			"[A-Z]" +
			"[a-z]?" +
			"\\-" +
		")?" + // also need to capture double name initials with dot only at end
		"[A-Z]" +
		"[a-z]?" +
		"\\.";
	
	private static String defaultUndottedInitialsBaseRegEx = 
			"[A-Z]" +
			"[a-z]?";
	
	private static String defaultDottedInitialsRegEx = "(" + defaultDottedInitialsBaseRegEx + ")(((\\s?\\-\\s?)|\\s)?" + defaultDottedInitialsBaseRegEx + ")*";
	
	private static String defaultUndottedInitialsRegEx = "(" + defaultUndottedInitialsBaseRegEx + ")((\\-|\\s)" + defaultUndottedInitialsBaseRegEx + ")*";
	
	private static String defaultBlockedInitialsRegEx = "(" + defaultUndottedInitialsBaseRegEx + ")(\\-?" + defaultUndottedInitialsBaseRegEx + ")*";
	
	private static String defaultLastNameMixedCaseBaseRegEx = 
			"([A-Za-z]+\\'?)?" +
			"(" +
				"(" +
					"[A-Z]" +
					"(" +
//						"([a-z\\-]*[aeiouy][a-z]*)" +
						"([a-z]*[aeiouy][a-z]*)" +
						"|" +
//						"([A-Z]*[AEIOUY][A-Z]*)" +
//						"|" +
						"([a-z]{1,2})" +
					")" +
				")" +
				"|" +
				"(" +
					"[AEIOUY]" +
//					"(" +
//						"([a-z\\-]*[a-z]+)" +
						"([a-z]*[a-z]+)" +
//						"|" +
//						"[A-Z]+" +
//					")" +
				")" +
			")" +
			"(\\'[a-z]+)?" +
			"";
//	private static String defaultLastNameMixedCaseRegEx = "(" + defaultLastNameMixedCaseBaseRegEx + ")((\\-|\\s)?" + defaultLastNameMixedCaseBaseRegEx + ")*";
//	private static String defaultLastNameMixedCaseRegEx = "(" + defaultLastNameMixedCaseBaseRegEx + ")((\\-" + defaultDottedInitialsBaseRegEx + ")|((\\-|\\s)?" + defaultLastNameMixedCaseBaseRegEx + ")*)?";
	private static String defaultLastNameMixedCaseRegEx = 
			"(" + 
				defaultLastNameMixedCaseBaseRegEx + 
			")" +
			"(" +
				"(\\-" + defaultDottedInitialsBaseRegEx + ")" +
				"|" +
				"(" +
					"(" +
						"(\\-([a-z]{1,5}\\-){0,2})" +
						"|" +
						"\\s" +
					")?" + 
					defaultLastNameMixedCaseBaseRegEx + 
				")*" +
			")?" +
			"";
	
	private static String defaultLastNameAllCapsBaseRegEx = 
			"([A-Za-z]+\\'?)?" +
			"(" +
//				"(" +
//					"[A-Z]" +
//					"(" +
//						"([a-z\\-]*[aeiouy][a-z]*)" +
//						"|" +
						"([A-Z]*[AEIOUY][A-Z]*)" +
//						"|" +
//						"([a-z]{1,2})" +
//					")" +
//				")" +
				"|" +
				"(" +
					"[AEIOUY]" +
//					"(" +
//						"([a-z\\-]*[a-z]+)" +
//						"|" +
						"[A-Z]+" +
//					")" +
				")" +
			")" +
//			"(\\'[a-z]+)?" +
			"";
//	private static String defaultLastNameAllCapsRegEx = "(" + defaultLastNameAllCapsBaseRegEx + ")((\\-|\\s)?" + defaultLastNameAllCapsBaseRegEx + ")*";
	private static String defaultLastNameAllCapsRegEx = 
			"(" + 
					defaultLastNameAllCapsBaseRegEx + 
				")" +
				"(" +
					"(\\-" + defaultDottedInitialsBaseRegEx + ")" +
					"|" +
					"(" +
						"(\\-|\\s)?" + 
						defaultLastNameAllCapsBaseRegEx + 
					")*" +
				")?" +
				"";
	
	private static String defaultPersonNameAffixRegEx = 
		"(" +
			"(" +
				"(" +
					"(" +
						"Jr" +
						"|" +
						"JR" +
						"|" +
						"Sr" +
						"|" +
						"SR" +
					")" +
					"\\.?" +
				")" +
				"|" +
				"(" +
					"X{0,3}" +
					"(" +
						"I{1,4}" +
						"|" +
						"IV" +
						"|" +
						"(VI{0,4})" +
						"|" +
						"IX" +
					")" +
					"\\.?" +
				")" +
				"|" +
				"(1\\s?(st|ST))" +
				"|" +
				"(2\\s?(nd|ND))" +
				"|" +
				"(3\\s?(rd|RD))" +
				"|" +
				"([4-9]\\s?(th|TH))" +
			")" +
		")";
	
	private static String defaultProperNamePartTitleCaseRegEx = 
			"(" +
				"(" +
					"[A-Z]" +
					"[a-z\\-\\']*[aeiouy][a-z]*" +
					"([\\-\\'][a-z]+)*" +
				")" +
				"|" +
				"(" +
					"[AEIOUY]" +
					"[a-z]+" +
					"([\\-\\'][a-z]+)*" +
				")" +
				"|" +
				"(" +
					"[A-Z]{2,}" +
				")" +
			")" +
			"";
	
	private static String defaultProperNamePartAllCapsRegEx = 
			"(" +
				"(" +
					"[A-Z]+[AEIOUY][A-Z]*" +
				")" +
				"|" +
				"(" +
					"[AEIOUY][A-Z]+" +
				")" +
				"|" +
				"(" +
					"[A-Z]{2,}" +
				")" +
			")" +
			"";
	
	private static String defaultAcronymRegEx = 
			"[A-Z]{3,}";
	
	private static StringVector defaultNameStopWords = new StringVector(false);
	
	private static boolean doneInitStopWords = false;
	private static synchronized boolean initStopWords() {
		if (!doneInitStopWords) try {
			doInitStopWords();
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
		}
		return doneInitStopWords;
	}
	
	private static synchronized void doInitStopWords() throws IOException {
		System.out.println("ProperNameUtils: initializing stop words ...");
		
		//	index characters by character names and vice versa
		String pnucrn = ProperNameUtils.class.getName().replaceAll("\\.", "/");
		InputStream swis = ProperNameUtils.class.getClassLoader().getResourceAsStream(pnucrn.substring(0, pnucrn.lastIndexOf('/')) + "/ProperNameStopWords.txt");
		BufferedReader swr = new BufferedReader(new InputStreamReader(swis, "UTF-8"));
		for (String swl; (swl = swr.readLine()) != null;) {
			swl = swl.trim();
			if (swl.length() == 0)
				continue;
			if (swl.startsWith("//"))
				continue;
			defaultNameStopWords.addElementIgnoreDuplicates(swl);
		}
		
		System.out.println("ProperNameUtils: stop words initialized.");
		doneInitStopWords = true;
	}
	
	/**
	 * Retrieve the default dictionary of name infixes to use. First and
	 * foremost, this concerns person name infixes like 'van', but it also
	 * covers the stop words found in institution and location names.
	 * You can amend (or generally modify) this dictionary. But be careful to
	 * do so, as any modification will affect all client code of this class
	 * throughout the whole JVM.
	 * @return the default dictionary of name stop words
	 */
	public static StringVector getDefaultNameStopWords() {
		initStopWords();
		return defaultNameStopWords;
	}
	
	private static final boolean DEBUG = false;
	
	private static final boolean DEBUG_PERSON_NAME_EXTRACTION = (DEBUG && true);
	
	//	TODO make these guys public static
	private static final String namePartOrderAttribute = "npo";
	private static final String lastNameFirstName = "LnFn";
	private static final String lastNameInitials = "LnIn";
	private static final String firstNameLastName = "FnLn";
	private static final String initialsLastName = "InLn";
	
	private static final String lastNameAttribute = "ln";
	private static final String firstNameAttribute = "fn";
	private static final String initialsAttribute = "in";
	private static final String tailingStopWordsAttribute = "tsw";
	
	private static final String firstNameStyleAttribute = "fns";
	private static final String fullFirstName = "N";
	private static final String initialsFirstName = "I";
	
	private static final String initialsStyleAttribute = "ins";
	private static final String dottedInitials = "D";
	private static final String undottedInitials = "U";
	private static final String blockInitials = "B";
	
	private static final String caseAttribute = "case";
	private static final String firstNameCaseAttribute = "fnc";
	private static final String lastNameCaseAttribute = "lnc";
	private static final String allCaps = "AC";
	private static final String titleCase = "TC";
	
	private static final String attachedPosition = "A";
	private static final String tailingPosition = "T";
	
	/**
	 * Tag person last names. The behavior of the tagger is controlled by the
	 * argument name style. See the property accessors of the <code>NameStyle</code>
	 * class above for details and permitted values.
	 * @param tokens the token sequence to investigate
	 * @param style the name style to use
	 * @return an array of annotations marking the last names found
	 */
	public static Annotation[] getPersonLastNames(TokenSequence tokens, NameStyle style) {
		
		//	get stop word blocks
		Annotation[] stopWordBlocks = getStopWordBlocks(tokens, style);
		
		//	get last name style settings (allow both is setting missing)
		String lastNameCase = style.getLastNameCase();
		boolean lastNamesMixedCase = !allCaps.equals(lastNameCase); // not explicitly all-caps
		boolean lastNamesAllCaps = !titleCase.equals(lastNameCase); // not explicitly title case
		
		//	get person name parts
		ArrayList personLastNameList = new ArrayList();
		
		//	last names (mixed-case and all-caps separately to allow template based switching)
		if (lastNamesMixedCase) {
			Annotation[] lastNames = Gamta.extractAllMatches(tokens, style.getLastNameMixedCaseRegEx(), true);
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - person last names (mixed case):");
			for (int l = 0; l < lastNames.length; l++) {
				if (!StringUtils.containsVowel(lastNames[l]))
					continue;
				lastNames[l].setAttribute(caseAttribute, titleCase);
				personLastNameList.add(lastNames[l]);
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + lastNames[l]);
			}
			
			//	add double last names with embedded stop words (mainly to cover Spanish last names and related cultures, e.g. 'Desbrochers des Loges')
			AnnotationIndex lastNamePartIndex = new AnnotationIndex();
			lastNamePartIndex.addAnnotations(lastNames, "lastName");
			lastNamePartIndex.addAnnotations(stopWordBlocks, "stopWords");
			Annotation[] doubleLastNames = AnnotationPatternMatcher.getMatches(tokens, lastNamePartIndex, "<lastName> <stopWords> <lastName>");
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - person double last names (mixed case):");
			for (int l = 0; l < doubleLastNames.length; l++) {
				doubleLastNames[l].setAttribute(caseAttribute, titleCase);
				personLastNameList.add(doubleLastNames[l]);
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + doubleLastNames[l]);
			}
		}
		if (lastNamesAllCaps) {
			Annotation[] lastNames = Gamta.extractAllMatches(tokens, style.getLastNameAllCapsRegEx(), true);
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - person last names (all caps):");
			for (int l = 0; l < lastNames.length; l++) {
				if (!StringUtils.containsVowel(lastNames[l]))
					continue;
				lastNames[l].setAttribute(caseAttribute, allCaps);
				personLastNameList.add(lastNames[l]);
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + lastNames[l]);
			}
			
			//	add double last names with embedded stop words (mainly to cover Spanish last names and related cultures, e.g. 'Desbrochers des Loges')
			AnnotationIndex lastNamePartIndex = new AnnotationIndex();
			lastNamePartIndex.addAnnotations(lastNames, "lastName");
			lastNamePartIndex.addAnnotations(stopWordBlocks, "stopWords");
			Annotation[] doubleLastNames = AnnotationPatternMatcher.getMatches(tokens, lastNamePartIndex, "<lastName> <stopWords> <lastName>");
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - person double last names (all caps):");
			for (int l = 0; l < doubleLastNames.length; l++) {
				doubleLastNames[l].setAttribute(caseAttribute, titleCase);
				personLastNameList.add(doubleLastNames[l]);
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + doubleLastNames[l]);
			}
		}
		
		//	finally ...
		return ((Annotation[]) personLastNameList.toArray(new Annotation[personLastNameList.size()]));
	}
	
	/**
	 * Tag whole person names. The behavior of the tagger is controlled by the
	 * argument name style. See the property accessors of the <code>NameStyle</code>
	 * class above for details and permitted values. Last names are tagged by
	 * the <code>getPersonLastNames()</code> method, also using the argument
	 * name style.
	 * @param tokens the token sequence to investigate
	 * @param style the name style to use
	 * @return an array of annotations marking the person names found
	 */
	public static Annotation[] getPersonNames(TokenSequence tokens, NameStyle style) {
		return getPersonNames(tokens, getPersonLastNames(tokens, style), style, null);
	}
	
	/**
	 * Tag whole person names, optionally around previously identified aliases
	 * (can be null). The behavior of the tagger is controlled by the argument
	 * name style. See the property accessors of the <code>NameStyle</code>
	 * class above for details and permitted values. Last names are tagged by
	 * the <code>getPersonLastNames()</code> method, also using the argument
	 * name style.
	 * @param tokens the token sequence to investigate
	 * @param style the name style to use
	 * @param aliases an array holding any aliases to include in the names
	 * @return an array of annotations marking the person names found
	 */
	public static Annotation[] getPersonNames(TokenSequence tokens, NameStyle style, Annotation[] aliases) {
		return getPersonNames(tokens, getPersonLastNames(tokens, style), style, aliases);
	}
	
	/**
	 * Tag whole person names around previously identified last names. The
	 * behavior of the tagger is controlled by the argument name style. See the
	 * property accessors of the <code>NameStyle</code> class above for details
	 * and permitted values.
	 * @param tokens the token sequence to investigate
	 * @param lastNames the last names to use
	 * @param style the name style to use
	 * @return an array of annotations marking the person names found
	 */
	public static Annotation[] getPersonNames(TokenSequence tokens, Annotation[] lastNames, NameStyle style) {
		return getPersonNames(tokens, lastNames, style, null);
	}
	
	/**
	 * Tag whole person names around previously identified last names and,
	 * optionally, aliases (the latter can be null). The behavior of the tagger
	 * is controlled by the argument name style. See the property accessors of
	 * the <code>NameStyle</code> class above for details and permitted values.
	 * @param tokens the token sequence to investigate
	 * @param lastNames the last names to use
	 * @param style the name style to use
	 * @param aliases an array holding any aliases to include in the names
	 * @return an array of annotations marking the person names found
	 */
	public static Annotation[] getPersonNames(TokenSequence tokens, Annotation[] lastNames, NameStyle style, Annotation[] aliases) {
		
		//	get full name style settings as default for first and last names (allow both is setting missing)
		String nameCase = style.getNameCase();
		boolean namesMixedCase = !allCaps.equals(nameCase); // not explicitly all-caps
		boolean namesAllCaps = !titleCase.equals(nameCase); // not explicitly title case
		
		//	get stop word blocks
		Annotation[] stopWordBlocks = getStopWordBlocks(tokens, style);
		
		//	get person name parts
		ArrayList namePartList = new ArrayList();
		
		//	get first name representation
		String firstNameStyle = style.getFirstNameStyle();
		boolean firstNamesFull = !initialsFirstName.equals(firstNameStyle); // not explicitly restricted to initials
		boolean firstNamesInitialsOnly = !fullFirstName.equals(firstNameStyle); // not explicitly requiring full names
		
		//	first names (unless we're restricted to initials-only)
		Annotation[] firstNames = new Annotation[0];
		if (firstNamesFull) {
			
			//	get first name style settings (allow both is setting missing)
			String firstNameCase = style.getFirstNameCase();
			boolean firstNamesMixedCase = !allCaps.equals(firstNameCase); // not explicitly all-caps
			boolean firstNamesAllCaps = !titleCase.equals(firstNameCase); // not explicitly title case
			
			//	first names (mixed-case and all-caps separately to allow template based switching)
			if (firstNamesMixedCase) {
				firstNames = Gamta.extractAllMatches(tokens, style.getFirstNameMixedCaseRegEx(), true);
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - first names (mixed case):");
				for (int f = 0; f < firstNames.length; f++) {
					if (!StringUtils.containsVowel(firstNames[f]))
						continue;
					firstNames[f].setAttribute(caseAttribute, titleCase);
					namePartList.add(firstNames[f]);
					if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + firstNames[f]);
				}
			}
			if (firstNamesAllCaps) {
				firstNames = Gamta.extractAllMatches(tokens, style.getFirstNameAllCapsRegEx(), true);
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - first names (all caps):");
				for (int f = 0; f < firstNames.length; f++) {
					if (!StringUtils.containsVowel(firstNames[f]))
						continue;
					firstNames[f].setAttribute(caseAttribute, allCaps);
					namePartList.add(firstNames[f]);
					if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + firstNames[f]);
				}
			}
			firstNames = ((Annotation[]) namePartList.toArray(new Annotation[namePartList.size()]));
			namePartList.clear();
		}
		
		//	get style of initials (if any)
		String initialsStyle = style.getInitialsStyle(); // dotted, undotted, or block
		
		//	blocks of initials (need to allow sub-matches here, as otherwise all-caps last names end up conflated with initials)
		if ((initialsStyle == null) || "D".equals(initialsStyle)) {
			Annotation[] initials = Gamta.extractAllMatches(tokens, style.getDottedInitialsRegEx(), true);
			for (int i = 0; i < initials.length; i++)
				initials[i].setAttribute(initialsStyleAttribute, dottedInitials);
			namePartList.addAll(Arrays.asList(initials));
			if (DEBUG_PERSON_NAME_EXTRACTION) {
				System.out.println("  - dotted initial sequences:");
				for (int i = 0; i < initials.length; i++)
					System.out.println("    - " + initials[i]);
			}
		}
		if ((initialsStyle == null) || "U".equals(initialsStyle)) {
			Annotation[] initials = Gamta.extractAllMatches(tokens, style.getUndottedInitialsRegEx(), true);
			for (int i = 0; i < initials.length; i++)
				initials[i].setAttribute(initialsStyleAttribute, undottedInitials);
			namePartList.addAll(Arrays.asList(initials));
			if (DEBUG_PERSON_NAME_EXTRACTION) {
				System.out.println("  - undotted initial sequences:");
				for (int i = 0; i < initials.length; i++)
					System.out.println("    - " + initials[i]);
			}
		}
		if ((initialsStyle == null) || "B".equals(initialsStyle)) {
			Annotation[] initials = Gamta.extractAllMatches(tokens, style.getBlockedInitialsRegEx(), true);
			for (int i = 0; i < initials.length; i++)
				initials[i].setAttribute(initialsStyleAttribute, blockInitials);
			namePartList.addAll(Arrays.asList(initials));
			if (DEBUG_PERSON_NAME_EXTRACTION) {
				System.out.println("  - initial blocks:");
				for (int i = 0; i < initials.length; i++)
					System.out.println("    - " + initials[i]);
			}
		}
		Annotation[] initials = ((Annotation[]) namePartList.toArray(new Annotation[namePartList.size()]));
		namePartList.clear();
		
		//	tag and index affixes (parameter dependent case distinction inside loop)
		Annotation[] affixes = Gamta.extractAllMatches(tokens, style.getPersonNameAffixRegEx(), false);
		if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - affixes:");
		for (int a = 0; a < affixes.length; a++) {
			if (affixes[a].getValue().length() == 1)
				affixes[a].setAttribute(caseAttribute, "*");
			else if (StringUtils.isRomanNumber(affixes[a].getValue()))
				affixes[a].setAttribute(caseAttribute, "*");
			else if (affixes[a].getValue().equals(affixes[a].getValue().toUpperCase()))
				affixes[a].setAttribute(caseAttribute, allCaps);
			else affixes[a].setAttribute(caseAttribute, titleCase);
			if (namesMixedCase)
				namePartList.add(affixes[a]);
			else if (namesAllCaps && affixes[a].getValue().equals(affixes[a].getValue().toUpperCase()))
				namePartList.add(affixes[a]);
			else continue;
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + affixes[a]);
		}
		affixes = ((Annotation[]) namePartList.toArray(new Annotation[namePartList.size()]));
		namePartList.clear();
		
		//	index person name parts
		AnnotationIndex personNameParts = new AnnotationIndex();
		personNameParts.addAnnotations(lastNames, "lastName");
		personNameParts.addAnnotations(stopWordBlocks, "stopWords");
		personNameParts.addAnnotations(AnnotationPatternMatcher.getMatches(tokens, personNameParts, "<lastName> <stopWords> <lastName>"), "lastName");
		personNameParts.addAnnotations(initials, "initials");
		personNameParts.addAnnotations(affixes, "affix");
		
		//	combine full first names with initials if full first names in use
		if (firstNamesFull) {
			personNameParts.addAnnotations(firstNames, "firstName");
			personNameParts.addAnnotations(AnnotationPatternMatcher.getMatches(tokens, personNameParts, "<firstName> <initials>"), "firstName");
			personNameParts.addAnnotations(AnnotationPatternMatcher.getMatches(tokens, personNameParts, "<initials> <firstName>"), "firstName");
			
			//	add aliases if present
			if (aliases != null) {
				personNameParts.addAnnotations(aliases, "alias");
				personNameParts.addAnnotations(AnnotationPatternMatcher.getMatches(tokens, personNameParts, "<firstName> <alias>"), "firstName");
			}
		}
		
		//	get affix style (allowing all the options by default)
		String affixCommaSeparated = style.getAffixCommaSeparated();
		boolean affixWithComma = !"N".equals(affixCommaSeparated); // not explicitly no
		boolean affixWithoutComma = !"Y".equals(affixCommaSeparated); // not explicitly yes
		String affixPosition = style.getAffixPosition();
		boolean affixOnLastName = !tailingPosition.equals(affixPosition); // not explicitly tailing
		boolean affixTailing = !attachedPosition.equals(affixPosition); // not explicitly attached to last name
		
		//	create affix matcher to construct full name patterns with
		String affixMatcher;
		if (affixWithComma && affixWithoutComma)
			affixMatcher = "','? <affix>";
		else if (affixWithComma)
			affixMatcher = "',' <affix>";
		else if (affixWithoutComma)
			affixMatcher = "<affix>";
		else affixMatcher = "','? <affix>";
		
		//	get infix position (allowing both way by default)
		String infixPosition = style.getInfixPosition();
		boolean infixOnLastName = !tailingPosition.equals(infixPosition); // not explicitly tailing
		boolean infixTailing = !attachedPosition.equals(infixPosition); // not explicitly attached to last name
		
		//	get name part order (if any)
		String namePartOrder = style.getNamePartOrder();
		
		//	build person names
		ArrayList personNameList = new ArrayList();
		HashSet personNameStrings = new HashSet();
		
		//	last name first, initials
		if ((namePartOrder == null) || (namePartOrder.contains(lastNameFirstName) && firstNamesInitialsOnly)) {
			addPersonNames(tokens, personNameParts, "<lastName> ',' <initials>", false, personNameList, personNameStrings, lastNameFirstName, initialsFirstName);
			if (affixOnLastName)
				addPersonNames(tokens, personNameParts, ("<lastName> " + affixMatcher + " ',' <initials>"), false, personNameList, personNameStrings, lastNameFirstName, initialsFirstName);
			if (affixTailing)
				addPersonNames(tokens, personNameParts, ("<lastName> ',' <initials> " + affixMatcher + ""), false, personNameList, personNameStrings, lastNameFirstName, initialsFirstName);
			if (infixOnLastName) {
				addPersonNames(tokens, personNameParts, "<stopWords> <lastName> ',' <initials>", false, personNameList, personNameStrings, lastNameFirstName, initialsFirstName);
				if (affixOnLastName)
					addPersonNames(tokens, personNameParts, ("<stopWords> <lastName> " + affixMatcher + " ',' <initials>"), false, personNameList, personNameStrings, lastNameFirstName, initialsFirstName);
				if (affixTailing)
					addPersonNames(tokens, personNameParts, ("<stopWords> <lastName> ',' <initials> " + affixMatcher + ""), false, personNameList, personNameStrings, lastNameFirstName, initialsFirstName);
			}
			if (infixTailing) {
				addPersonNames(tokens, personNameParts, "<lastName> ',' <initials> <stopWords>", true, personNameList, personNameStrings, lastNameFirstName, initialsFirstName);
				if (affixOnLastName)
					addPersonNames(tokens, personNameParts, ("<lastName> " + affixMatcher + " ',' <initials> <stopWords>"), true, personNameList, personNameStrings, lastNameFirstName, initialsFirstName);
				if (affixTailing)
					addPersonNames(tokens, personNameParts, ("<lastName> ',' <initials> <stopWords> " + affixMatcher + ""), false, personNameList, personNameStrings, lastNameFirstName, initialsFirstName);
			}
			personNameStrings.clear();
		}
		if ((namePartOrder == null) || (namePartOrder.contains(lastNameInitials) && firstNamesInitialsOnly)) {
			addPersonNames(tokens, personNameParts, "<lastName> <initials>", false, personNameList, personNameStrings, lastNameInitials, initialsFirstName);
			if (affixOnLastName)
				addPersonNames(tokens, personNameParts, ("<lastName> " + affixMatcher + " <initials>"), false, personNameList, personNameStrings, lastNameInitials, initialsFirstName);
			if (affixTailing)
				addPersonNames(tokens, personNameParts, ("<lastName> <initials> " + affixMatcher + ""), false, personNameList, personNameStrings, lastNameInitials, initialsFirstName);
			if (infixOnLastName) {
				addPersonNames(tokens, personNameParts, "<stopWords> <lastName> <initials>", false, personNameList, personNameStrings, lastNameInitials, initialsFirstName);
				if (affixOnLastName)
					addPersonNames(tokens, personNameParts, ("<stopWords> <lastName> " + affixMatcher + " <initials>"), false, personNameList, personNameStrings, lastNameInitials, initialsFirstName);
				if (affixTailing)
					addPersonNames(tokens, personNameParts, ("<stopWords> <lastName> <initials> " + affixMatcher + ""), false, personNameList, personNameStrings, lastNameInitials, initialsFirstName);
			}
			if (infixTailing) {
				addPersonNames(tokens, personNameParts, "<lastName> <initials> <stopWords>", true, personNameList, personNameStrings, lastNameInitials, initialsFirstName);
				if (affixOnLastName)
					addPersonNames(tokens, personNameParts, ("<lastName> " + affixMatcher + " <initials> <stopWords>"), true, personNameList, personNameStrings, lastNameInitials, initialsFirstName);
				if (affixTailing)
					addPersonNames(tokens, personNameParts, ("<lastName> <initials> <stopWords> " + affixMatcher + ""), false, personNameList, personNameStrings, lastNameInitials, initialsFirstName);
			}
			personNameStrings.clear();
		}
		
		//	last name first, full first name
		if ((namePartOrder == null) || (namePartOrder.contains(lastNameFirstName) && firstNamesFull)) {
			addPersonNames(tokens, personNameParts, "<lastName> ',' <firstName>", false, personNameList, personNameStrings, lastNameFirstName, fullFirstName);
			if (affixOnLastName)
				addPersonNames(tokens, personNameParts, ("<lastName> " + affixMatcher + " ',' <firstName>"), false, personNameList, personNameStrings, lastNameFirstName, fullFirstName);
			if (affixTailing)
				addPersonNames(tokens, personNameParts, ("<lastName> ',' <firstName> " + affixMatcher + ""), false, personNameList, personNameStrings, lastNameFirstName, fullFirstName);
			if (infixOnLastName) {
				addPersonNames(tokens, personNameParts, "<stopWords> <lastName> ',' <firstName>", false, personNameList, personNameStrings, lastNameFirstName, fullFirstName);
				if (affixOnLastName)
					addPersonNames(tokens, personNameParts, ("<stopWords> <lastName> " + affixMatcher + " ',' <firstName>"), false, personNameList, personNameStrings, lastNameFirstName, fullFirstName);
				if (affixTailing)
					addPersonNames(tokens, personNameParts, ("<stopWords> <lastName> ',' <firstName> " + affixMatcher + ""), false, personNameList, personNameStrings, lastNameFirstName, fullFirstName);
			}
			if (infixTailing) {
				addPersonNames(tokens, personNameParts, "<lastName> ',' <firstName> <stopWords>", true, personNameList, personNameStrings, lastNameFirstName, fullFirstName);
				if (affixOnLastName)
					addPersonNames(tokens, personNameParts, ("<lastName> " + affixMatcher + " ',' <firstName> <stopWords>"), true, personNameList, personNameStrings, lastNameFirstName, fullFirstName);
				if (affixTailing)
					addPersonNames(tokens, personNameParts, ("<lastName> ',' <firstName> <stopWords> " + affixMatcher + ""), false, personNameList, personNameStrings, lastNameFirstName, fullFirstName);
			}
			personNameStrings.clear();
		}
		
		//	last name last, initials
		if ((namePartOrder == null) || (namePartOrder.contains(firstNameLastName) && firstNamesInitialsOnly) || namePartOrder.contains(initialsLastName)) {
			addPersonNames(tokens, personNameParts, "<initials> <stopWords>? <lastName>", false, personNameList, personNameStrings, initialsLastName, initialsFirstName);
			addPersonNames(tokens, personNameParts, ("<initials> <stopWords>? <lastName> " + affixMatcher + ""), false, personNameList, personNameStrings, initialsLastName, initialsFirstName);
			personNameStrings.clear();
		}
		
		//	last name last, full first name
		if ((namePartOrder == null) || (namePartOrder.contains(firstNameLastName) && firstNamesFull)) {
			addPersonNames(tokens, personNameParts, "<firstName> <stopWords>? <lastName>", false, personNameList, personNameStrings, firstNameLastName, fullFirstName);
			addPersonNames(tokens, personNameParts, ("<firstName> <stopWords>? <lastName> " + affixMatcher + ""), false, personNameList, personNameStrings, firstNameLastName, fullFirstName);
			personNameStrings.clear();
		}
		
		//	set type for all person names
		for (int a = 0; a < personNameList.size(); a++)
			((Annotation) personNameList.get(a)).changeTypeTo(PERSON_TYPE);
		
		/* If person names form non-overlapping clusters, we can remove the
		 * inner ones without the risk of breaking any name lists, so we retain
		 * only the names covering a whole cluster.
		 * Removing a name only for nesting in a longer one of the same style
		 * also retains positional duplicates with different styles, facilitating
		 * further filtering based upon a style vote in client code */
		Collections.sort(personNameList, AnnotationUtils.ANNOTATION_NESTING_ORDER);
		for (int p = 0; p < personNameList.size(); p++) {
			Annotation personName = ((Annotation) personNameList.get(p));
			String personNamePartOrder = ((String) personName.getAttribute(namePartOrderAttribute, ""));
			String personNameFns = ((String) personName.getAttribute(firstNameStyleAttribute, ""));
			boolean tailingStopWords = personName.hasAttribute(tailingStopWordsAttribute);
			
			for (int cp = (p+1); cp < personNameList.size(); cp++) {
				Annotation cPersonName = ((Annotation) personNameList.get(cp));
				
				//	we got after covering annotation without finding any of the other conditions, remove contained
				if (personName.getEndIndex() <= cPersonName.getStartIndex()) {
					cp--; // keep current person name that got us after open cluster
					if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - removing " + personNamePartOrder + "-" + personNameFns + (tailingStopWords ? "-tsw" : "") + " person names nested in " + personName.getValue());
					while (cp > p) {
						cPersonName = ((Annotation) personNameList.get(cp));
						String cPersonNamePartOrder = ((String) cPersonName.getAttribute(namePartOrderAttribute, ""));
						String cPersonNameFns = ((String) cPersonName.getAttribute(firstNameStyleAttribute));
						boolean cTailingStopWords = cPersonName.hasAttribute(tailingStopWordsAttribute);
						if (personNamePartOrder.equals(cPersonNamePartOrder) && personNameFns.equals(cPersonNameFns) && (tailingStopWords == cTailingStopWords)) {
							if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("      - style " + cPersonNamePartOrder + "-" + cPersonNameFns + (cTailingStopWords ? "-tsw" : "") + " " + cPersonName.getValue());
							personNameList.remove(cp--);
						}
						else {
							if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("      + style " + cPersonNamePartOrder + "-" + cPersonNameFns + (cTailingStopWords ? "-tsw" : "") + " " + cPersonName.getValue());
							cp--;
						}
					}
					break;
				}
				
				//	non-including overlap, no usable cluster
				else if (personName.getEndIndex() < cPersonName.getEndIndex())
					break;
				
				//	included in potential cluster, simply keep looking
				else {}
			}
		}
		
		
		//	finally ...
		Annotation[] personNames = ((Annotation[]) personNameList.toArray(new Annotation[personNameList.size()]));
		Arrays.sort(personNames);
		return personNames;
	}
	
	private static void addPersonNames(TokenSequence tokens, AnnotationIndex personNameParts, String personNamePattern, boolean tailingStopWords, ArrayList personNameList, HashSet personNameStrings, String namePartOrderKey, String firstNameStyleKey) {
		MatchTree[] personNameMatches = AnnotationPatternMatcher.getMatchTrees(tokens, personNameParts, personNamePattern);
		if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - " + personNameMatches.length + " persons " + personNamePattern + ":");
		for (int a = 0; a < personNameMatches.length; a++) {
			Annotation personName = personNameMatches[a].getMatch();
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + personName);
			addPersonNameAttributes(personNameMatches[a], personName);
			personName.setAttribute(namePartOrderAttribute, namePartOrderKey);
			personName.setAttribute(firstNameStyleAttribute, firstNameStyleKey);
			if (tailingStopWords)
				personName.setAttribute(tailingStopWordsAttribute);
			if (personNameStrings.add(personName.getStartIndex() + "-" + personName.getValue()))
				personNameList.add(personName); // de-duplicate within individual name part orders
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("      ==> " + personName.toXML());
		}
	}
	
	private static void addPersonNameAttributes(MatchTree personNameMatch, Annotation personName) {
		HashSet cases = new HashSet();
		addPersonNameAttributes(personNameMatch.getChildren(), personName, cases);
		if (cases.size() == 1)
			personName.setAttribute(caseAttribute, cases.iterator().next());
	}
	
	private static void addPersonNameAttributes(MatchTreeNode[] mtns, Annotation personName, HashSet cases) {
		for (int n = 0; n < mtns.length; n++) {
			if (mtns[n].getPattern().startsWith("<")) {
				String partType = mtns[n].getPattern().substring(1).replaceAll("[^a-zA-Z].*", "");
				Annotation personNamePart = mtns[n].getMatch();
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("      - " + partType + ": " + personNamePart.getValue());
				
				//	handle case
				if (personNamePart.hasAttribute(caseAttribute)) {
					cases.add(personNamePart.getAttribute(caseAttribute));
					if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("        case: " + personNamePart.getAttribute(caseAttribute));
					if ("lastName".equals(partType) && personNamePart.hasAttribute(caseAttribute))
						personName.setAttribute(lastNameCaseAttribute, personNamePart.getAttribute(caseAttribute));
					if ("firstName".equals(partType) && personNamePart.hasAttribute(caseAttribute))
						personName.setAttribute(firstNameCaseAttribute, personNamePart.getAttribute(caseAttribute));
				}
				
				//	handle specific name parts
				if ("lastName".equals(partType))
					personName.setAttribute(lastNameAttribute, personNamePart.getValue());
				else if ("firstName".equals(partType))
					personName.setAttribute(firstNameAttribute, personNamePart.getValue());
				else if ("initials".equals(partType)) {
					personName.setAttribute(initialsAttribute, personNamePart.getValue());
					if (personNamePart.hasAttribute(initialsStyleAttribute)) {
						personName.setAttribute(initialsStyleAttribute, personNamePart.getAttribute(initialsStyleAttribute));
						if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("        style: " + personNamePart.getAttribute(initialsStyleAttribute));
					}
				}
			}
			else {
				MatchTreeNode[] cMtns = mtns[n].getChildren();
				if (cMtns != null)
					addPersonNameAttributes(cMtns, personName, cases);
			}
		}
	}
	
	/**
	 * Tag straight proper names, i.e., names whose individual tokens (a) do
	 * not involve any punctuation marks and (b) are always in the same order,
	 * independent of any specific style. The argument style can specify the
	 * case of proper names to seek (title case or all-caps), as well as the
	 * stop words allowed to be included in the names to tag (e.g. 'Institute
	 * of the Study of YouKnowWhat').
	 * @param tokens the token sequence to investigate
	 * @param style the name style to use
	 * @return an array of annotations marking the proper names found
	 */
	public static Annotation[] getStraightProperNames(TokenSequence tokens, NameStyle style) {
		ArrayList nameList = new ArrayList();
		HashSet nameStrings = new HashSet();
		
		//	get full name style settings as default for first and last names (allow both is setting missing)
		String nameCase = style.getNameCase();
		boolean namesMixedCase = !allCaps.equals(nameCase); // not explicitly all-caps
		boolean namesAllCaps = !titleCase.equals(nameCase); // not explicitly title case
		
		//	get stop word blocks
		Annotation[] stopWordBlocks = getStopWordBlocks(tokens, style);
		
		//	tag institutional person names (wildcard on style, apart from case)
		Annotation[] nameParts;
		if (namesMixedCase) {
			
			//	tag and index name parts
			nameParts = Gamta.extractAllMatches(tokens, style.getProperNamePartTitleCaseRegEx());
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - institution name parts (title case):");
			for (int p = 0; p < nameParts.length; p++) {
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + nameParts[p]);
				nameList.add(nameParts[p]);
			}
			
			//	tag full proper names
			AnnotationIndex namePartIndex = new AnnotationIndex();
			namePartIndex.addAnnotations(nameParts, "namePart");
			namePartIndex.addAnnotations(stopWordBlocks, "stopWords");
			addProperNames(tokens, namePartIndex, nameList, nameStrings, titleCase);
		}
		if (namesAllCaps) {
			
			//	tag and index name parts
			nameParts = Gamta.extractAllMatches(tokens, style.getProperNamePartAllCapsRegEx());
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - institution name parts (all-caps):");
			for (int p = 0; p < nameParts.length; p++) {
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + nameParts[p]);
				nameList.add(nameParts[p]);
			}
			
			//	tag full proper names
			AnnotationIndex namePartIndex = new AnnotationIndex();
			namePartIndex.addAnnotations(nameParts, "namePart");
			namePartIndex.addAnnotations(stopWordBlocks, "stopWords");
			addProperNames(tokens, namePartIndex, nameList, nameStrings, allCaps);
		}
		
		//	finally ...
		Collections.sort(nameList, AnnotationUtils.ANNOTATION_NESTING_ORDER);
		return ((Annotation[]) nameList.toArray(new Annotation[nameList.size()]));
	}
	
	private static void addProperNames(TokenSequence tokens, AnnotationIndex namePartIndex, ArrayList nameList, HashSet nameStrings, String caseAttributeValue) {
		ArrayList newNameList = new ArrayList();
		do {
			if (DEBUG) System.out.println("Attempting name expansion");
			newNameList.clear();
			Annotation[] nameMatches = AnnotationPatternMatcher.getMatches(tokens, namePartIndex, "<namePart> <stopWords>? <namePart>");
			if (DEBUG) System.out.println(" - got " + nameMatches.length + " expanded names");
			for (int l = 0; l < nameMatches.length; l++)
				if (nameStrings.add(nameMatches[l].getValue())) {
					nameMatches[l].setAttribute(caseAttribute, caseAttributeValue);
					newNameList.add(nameMatches[l]);
					if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("Added name " + nameMatches[l].toXML());
				}
			nameList.addAll(newNameList);
			namePartIndex.addAnnotations(((Annotation[]) newNameList.toArray(new Annotation[newNameList.size()])), "namePart");
		} while (newNameList.size() != 0);
	}
	
	private static Annotation[] getStopWordBlocks(TokenSequence tokens, NameStyle style) {
		ArrayList stopWordBlockList = new ArrayList();
		
		//	get full name style settings as default for first and last names (allow both is setting missing)
		String nameCase = style.getNameCase();
		boolean namesMixedCase = !allCaps.equals(nameCase); // not explicitly all-caps
		boolean namesAllCaps = !titleCase.equals(nameCase); // not explicitly title case
		
		//	stop word blocks (pick out lower case and all-caps separately to allow template based switching)
		//	TODOne check insistence on vowels (there might be short stop words that have none in some language ...)
		Annotation[] stopWordBlocks = Gamta.extractAllContained(tokens, style.getNameStopWords(), false, true);
		if (namesMixedCase) {
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - lower case stop word blocks:");
			for (int s = 0; s < stopWordBlocks.length; s++) {
				if (!StringUtils.containsVowel(stopWordBlocks[s]) && ((stopWordBlocks[s].getValue().length() > 2) || (stopWordBlocks[s].getValue().indexOf("'") == -1)))
					continue;
				if (stopWordBlocks[s].getValue().equals(stopWordBlocks[s].getValue().toUpperCase()))
					continue;
				stopWordBlocks[s].setAttribute(caseAttribute, titleCase);
				stopWordBlockList.add(stopWordBlocks[s]);
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + stopWordBlocks[s]);
			}
		}
		if (namesAllCaps) {
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - all-caps stop word blocks:");
			for (int s = 0; s < stopWordBlocks.length; s++) {
				if (!StringUtils.containsVowel(stopWordBlocks[s]) && ((stopWordBlocks[s].getValue().length() > 2) || (stopWordBlocks[s].getValue().indexOf("'") == -1)))
					continue;
				if (!stopWordBlocks[s].getValue().equals(stopWordBlocks[s].getValue().toUpperCase()))
					continue;
				stopWordBlocks[s].setAttribute(caseAttribute, allCaps);
				stopWordBlockList.add(stopWordBlocks[s]);
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + stopWordBlocks[s]);
			}
		}
		return ((Annotation[]) stopWordBlockList.toArray(new Annotation[stopWordBlockList.size()]));
	}
	
	/**
	 * Tag acronyms. The pattern to use can be specified via the argument name
	 * style. Leaving it void results in sequences of three or more capital
	 * letters (basic Latin only) to be tagged.
	 * @param tokens the token sequence to investigate
	 * @param style the name style to use
	 * @return an array of annotations marking the acronyms found
	 */
	public static Annotation[] getAcronyms(TokenSequence tokens, NameStyle style) {
		Annotation[] acronyms = Gamta.extractAllMatches(tokens, style.getAcronymRegEx());
		if (DEBUG_PERSON_NAME_EXTRACTION) {
			System.out.println("  - acronyms:");
			for (int a = 0; a < acronyms.length; a++)
				System.out.println("    - " + acronyms[a]);
		}
		return acronyms;
	}
	
	/**
	 * Tag quoted aliases (like in 'Pete "Maverick" Mitchel' or 'Nick "Goose"
	 * Bradshaw'). The quoter character to use can be specified via the argument
	 * name style. Leaving it void results in both double quotes and single and
	 * double high commas to be tagged. The aliases proper are tagged by the
	 * <code>getStraightProperNames()</code> method, also using the argument
	 * name style.
	 * @param tokens the token sequence to investigate
	 * @param style the name style to use
	 * @return an array of annotations marking the quoted aliases found
	 */
	public static Annotation[] getQuotedAliases(TokenSequence tokens, NameStyle style) {
		
		//	tag and index proper names
		Annotation[] properNames = getStraightProperNames(tokens, style);
		AnnotationIndex aliasPartIndex = new AnnotationIndex();
		aliasPartIndex.addAnnotations(properNames, "alias");
		
		//	get surrounding quotation mark
		String aliasQuoter = style.getAliasQuoter();
		
		//	tag quoted aliases
		ArrayList quotedAliasList = new ArrayList();
		Annotation[] quotedAliases;
		if ((aliasQuoter == null) || "'".equals(aliasQuoter)) {
			quotedAliases = AnnotationPatternMatcher.getMatches(tokens, aliasPartIndex, "'\\'' <alias> '\\''");
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - quoted aliases:");
			for (int a = 0; a < quotedAliases.length; a++) {
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + quotedAliases[a]);
				quotedAliasList.add(quotedAliases[a]);
			}
		}
		if ((aliasQuoter == null) || "''".equals(aliasQuoter)) {
			quotedAliases = AnnotationPatternMatcher.getMatches(tokens, aliasPartIndex, "'\\'\\'' <alias> '\\'\\''");
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - quoted aliases:");
			for (int a = 0; a < quotedAliases.length; a++) {
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + quotedAliases[a]);
				quotedAliasList.add(quotedAliases[a]);
			}
		}
		if ((aliasQuoter == null) || "\"".equals(aliasQuoter)) {
			quotedAliases = AnnotationPatternMatcher.getMatches(tokens, aliasPartIndex, "'\\\"' <alias> '\\\"'");
			if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("  - quoted aliases:");
			for (int a = 0; a < quotedAliases.length; a++) {
				if (DEBUG_PERSON_NAME_EXTRACTION) System.out.println("    - " + quotedAliases[a]);
				quotedAliasList.add(quotedAliases[a]);
			}
		}
		
		//	sort and return quoted initials
		quotedAliases = ((Annotation[]) quotedAliasList.toArray(new Annotation[quotedAliasList.size()]));
		Arrays.sort(quotedAliases, AnnotationUtils.ANNOTATION_NESTING_ORDER);
		return quotedAliases;
	}
//	
//	public static void main(String[] args) throws Exception {
//		//	TODO use for testing
//		TokenSequence ts;
////		ts = Gamta.newTokenSequence("9. Cloutier, R. The primitive actinistian Miguashaia bureaui Schultze (Sarcopterygii). (Verlag Dr. Friedrich Pfeil,Munchen, 1996).", null);
//		ts = Gamta.newTokenSequence("Ruiz-C.R.I., Roman-Valencia C., Herrera B. Pelaez O. &amp; Ermakova-A.A. 2011. Variacion morfologica de las especies de Astyanax subgenero Zygogaster (Pisces, Characiformes, Characidae). Animal Biodiversity and Conservation 34: 47-66. Available from http://abc.museucienciesjournals.cat/wp-content/blogs.dir/2/files/ABC-34-1-pp-47-66.pdf [accessed 20 Oct. 2017].", null);
//		Annotation[] pns = getPersonNames(ts, new NameStyle());
//		for (int n = 0; n < pns.length; n++)
//			System.out.println(pns[n].toXML());
//	}
}
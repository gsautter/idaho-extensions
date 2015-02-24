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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.plugins.abbreviations.analyzers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.plugins.abbreviations.AbbreviationConstants;

/**
 * @author sautter
 *
 */
public class AbbreviationAugmenter extends AbstractConfigurableAnalyzer implements AbbreviationConstants {

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	index existing abbreviation to resolve parent references
		Annotation[] abbreviations = data.getAnnotations(ABBREVIATION_ANNOTATION_TYPE);
		HashMap abbreviationsByString = new HashMap();
		for (int a = 0; a < abbreviations.length; a++) {
			String abbreviationString = TokenSequenceUtils.concatTokens(abbreviations[a], true, true);
			abbreviationsByString.put(abbreviationString, abbreviations[a]);
		}
		
		//	work paragraph wise
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		for (int p = 0; p < paragraphs.length; p++) {
			MutableAnnotation[] abbreviationDatas = paragraphs[p].getMutableAnnotations(ABBREVIATION_DATA_ANNOTATION_TYPE);
			if (abbreviationDatas.length == 0)
				continue;
			
			//	transfer all detail data from abbreviationData sections to all abbreviations nested in them
			for (int a = 0; a < abbreviationDatas.length; a++)
				this.augmentAbbreviation(abbreviationDatas[a], abbreviationsByString);
		}
	}
	
	private void augmentAbbreviation(MutableAnnotation abbreviationData, HashMap abbreviationsByString) {
		
		//	get nested abbreviation data sections
		Annotation[] nestedAbbreviationDatas = abbreviationData.getMutableAnnotations(ABBREVIATION_DATA_ANNOTATION_TYPE);
		
		//	get targeted abbreviation (range)
		Annotation[] abbreviations = abbreviationData.getMutableAnnotations(ABBREVIATION_ANNOTATION_TYPE);
		if (abbreviations.length == 0)
			abbreviations = abbreviationData.getMutableAnnotations(ABBREVIATION_RANGE_ANNOTATION_TYPE);
		if (abbreviations.length == 0)
			return;
		Annotation abbreviation = abbreviations[0];
		
		//	check nesting
		for (int n = 0; n < nestedAbbreviationDatas.length; n++) {
			if (nestedAbbreviationDatas[n].size() == abbreviationData.size())
				continue;
			else if (AnnotationUtils.overlaps(abbreviation, nestedAbbreviationDatas[n]))
				return;
		}
		
		//	transfer attributes from parent abbreviation (if any), do this now so own details overrule
		if (abbreviation.hasAttribute(PARENT_ABBREVIATION_ATTTRIBUTE)) {
			Annotation parentAbbreviation = ((Annotation) abbreviationsByString.get(abbreviation.getAttribute(PARENT_ABBREVIATION_ATTTRIBUTE)));
			if (parentAbbreviation != null)
				AttributeUtils.copyAttributes(parentAbbreviation, abbreviation, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
		}
		
		//	get detail annotations
		Annotation[] details = abbreviationData.getAnnotations();
		HashMap detailsByType = new HashMap();
		for (int d = 0; d < details.length; d++) {
			if (ABBREVIATION_ANNOTATION_TYPE.equals(details[d].getType()) || ABBREVIATION_DATA_ANNOTATION_TYPE.equals(details[d].getType()) || AnnotationUtils.overlaps(details[d], abbreviation))
				continue;
			else for (int n = 0; (n < nestedAbbreviationDatas.length) && (details[d] != null); n++) {
				if (nestedAbbreviationDatas[n].size() == abbreviationData.size())
					continue;
				else if (AnnotationUtils.overlaps(details[d], nestedAbbreviationDatas[n]))
					details[d] = null;
			}
			if (details[d] != null) {
				ArrayList detailList = ((ArrayList) detailsByType.get(details[d].getType()));
				if (detailList == null) {
					detailList = new ArrayList(2);
					detailsByType.put(details[d].getType(), detailList);
				}
				detailList.add(details[d]);
			}
		}
		
		//	use detail annotations types, one by one
		for (Iterator tit = detailsByType.keySet().iterator(); tit.hasNext();) {
			String detailType = ((String) tit.next());
			ArrayList detailList = ((ArrayList) detailsByType.get(detailType));
			
			//	only one annotation of type, no need for numbering
			if (detailList.size() == 1)
				setDetailAttributes(abbreviation, ((Annotation) detailList.get(0)), detailType);
			
			//	multiple annotations of type, number derived attributes
			else for (int d = 0; d < detailList.size(); d++)
				setDetailAttributes(abbreviation, ((Annotation) detailList.get(d)), (detailType + "--" + (d+1)));
		}
	}
	
	private void setDetailAttributes(Annotation abbreviation, Annotation detail, String namePrefix) {
		if (ABBREVIATION_REFERENCE_ANNOTATION_TYPE.equals(detail.getType()))
			AttributeUtils.copyAttributes(detail, abbreviation, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
		else {
			abbreviation.setAttribute((namePrefix + "--" + ANNOTATED_VALUE_ATTRIBUTE), detail.getValue());
			String[] detailAttributeNames = detail.getAttributeNames();
			for (int a = 0; a < detailAttributeNames.length; a++)
				abbreviation.setAttribute((namePrefix + "--" + detailAttributeNames[a]), detail.getAttribute(detailAttributeNames[a]));
		}
	}
}
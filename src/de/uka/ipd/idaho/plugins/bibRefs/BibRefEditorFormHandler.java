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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.BufferedLineWriter;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefTypeSystem.BibRefType;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * @author sautter
 */
public class BibRefEditorFormHandler implements BibRefConstants {
	
	/**
	 * Write the JavaScript code used by the reference editor form fields to
	 * some writer.
	 * @param out the writer to write to
	 * @param typeSystem the reference type system to use for data validation
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 * @throws IOException
	 */
	public static void writeJavaScripts(Writer out, BibRefTypeSystem typeSystem, String[] idTypes) throws IOException {
		if (typeSystem == null)
			typeSystem = BibRefTypeSystem.getDefaultInstance();
		
		BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
		blw.writeLine("<script type=\"text/javascript\">");
		blw.writeLine("function bibRefEditor_getFieldState(fieldName, refType) {");
		String[] brtns = typeSystem.getBibRefTypeNames();
		for (int t = 0; t < brtns.length; t++) {
			blw.writeLine("  " + ((t == 0) ? "" : "else ") + "if (refType == '" + brtns[t] + "') {");
			BibRefType brt = typeSystem.getBibRefType(brtns[t]);
			for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
				String fs = (brt.requiresAttribute(BibRefEditor.fieldNames[f]) ? "r" : (brt.canHaveAttribute(BibRefEditor.fieldNames[f]) ? "o" : "f"));
				if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
					if (brt.requiresAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE) || brt.requiresAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE) || brt.requiresAttribute(NUMERO_DESIGNATOR_ANNOTATION_TYPE))
						fs = "r";
					else if (brt.canHaveAttribute(VOLUME_DESIGNATOR_ANNOTATION_TYPE) || brt.canHaveAttribute(ISSUE_DESIGNATOR_ANNOTATION_TYPE) || brt.canHaveAttribute(NUMERO_DESIGNATOR_ANNOTATION_TYPE))
						fs = "o";
					else fs = "f";
				}
				blw.writeLine("    if (fieldName == '" + BibRefEditor.fieldNames[f] + "')");
				blw.writeLine("      return '" + fs + "';");
			}
			if (idTypes != null)
				for (int i = 0; i < idTypes.length; i++) {
					if (idTypes[i] == null)
						continue;
					String idType = idTypes[i].trim();
					if (idType.length() == 0)
						continue;
					blw.writeLine("    if (fieldName == '" + ("ID-" + idType) + "')");
					blw.writeLine("      return '" + ((i == 0) ? "r" : "o") + "';");
				}
			blw.writeLine("  }");
		}
		blw.writeLine("  return 'o';");
		blw.writeLine("}");
		
		blw.writeLine("function bibRefEditor_getRefType() {");
		blw.writeLine("  var field = $('" + PUBLICATION_TYPE_ATTRIBUTE + "_field');");
		blw.writeLine("  return ((field == null) ? '' : field.value);");
		blw.writeLine("}");
		blw.writeLine("function bibRefEditor_getFieldValue(fieldName) {");
		blw.writeLine("  if (fieldName == '" + PART_DESIGNATOR_ANNOTATION_TYPE + "')");
		blw.writeLine("    return (bibRefEditor_getFieldValue('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "') + bibRefEditor_getFieldValue('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "') + bibRefEditor_getFieldValue('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "'));");
		blw.writeLine("  else {");
		blw.writeLine("    var field = $(fieldName + '_field');");
		blw.writeLine("    return ((field == null) ? '' : field.value);");
		blw.writeLine("  }");
		blw.writeLine("}");
		
		blw.writeLine("function bibRefEditor_refTypeChanged() {");
		blw.writeLine("  var refType = bibRefEditor_getRefType();");
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				blw.writeLine("  bibRefEditor_displayFieldState('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType));");
				blw.writeLine("  bibRefEditor_displayFieldState('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType));");
				blw.writeLine("  bibRefEditor_displayFieldState('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType));");
			}
			else {
				blw.writeLine("  bibRefEditor_displayFieldState('" + BibRefEditor.fieldNames[f] + "', bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType));");
			}
			blw.writeLine("  bibRefEditor_displayFieldError('" + BibRefEditor.fieldNames[f] + "', bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType));");
		}
		if (idTypes != null)
			for (int i = 0; i < idTypes.length; i++) {
				if (idTypes[i] == null)
					continue;
				String idType = idTypes[i].trim();
				if (idType.length() == 0)
					continue;
				blw.writeLine("  bibRefEditor_displayFieldState('" + ("ID-" + idType) + "', bibRefEditor_getFieldState('" + ("ID-" + idType) + "', refType));");
				blw.writeLine("  bibRefEditor_displayFieldError('" + ("ID-" + idType) + "', bibRefEditor_getFieldState('" + ("ID-" + idType) + "', refType));");
			}
		blw.writeLine("}");
		blw.writeLine("function bibRefEditor_fieldValueChanged(fieldName) {");
		blw.writeLine("  if (fieldName == '" + PART_DESIGNATOR_ANNOTATION_TYPE + "') {");
		blw.writeLine("    bibRefEditor_displayFieldState('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState(fieldName, bibRefEditor_getRefType()));");
		blw.writeLine("    bibRefEditor_displayFieldState('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState(fieldName, bibRefEditor_getRefType()));");
		blw.writeLine("    bibRefEditor_displayFieldState('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState(fieldName, bibRefEditor_getRefType()));");
		blw.writeLine("  }");
		blw.writeLine("  else bibRefEditor_displayFieldState(fieldName, bibRefEditor_getFieldState(fieldName, bibRefEditor_getRefType()));");
		blw.writeLine("  bibRefEditor_displayFieldError(fieldName, bibRefEditor_getFieldState(fieldName, bibRefEditor_getRefType()));");
		blw.writeLine("}");
		
		blw.writeLine("function bibRefEditor_displayFieldState(fieldName, fieldState) {");
		blw.writeLine("  var field = $(fieldName + '_field');");
		blw.writeLine("  if (field == null)");
		blw.writeLine("    return;");
		blw.writeLine("  if (fieldState == 'r') {");
		blw.writeLine("    field.style.backgroundColor = 'FFFFFF';");
		blw.writeLine("    field.style.color = '000000';");
		blw.writeLine("    field.disabled = false;");
		blw.writeLine("  }");
		blw.writeLine("  else if (fieldState == 'o') {");
		blw.writeLine("    field.style.backgroundColor = 'CCCCCC';");
		blw.writeLine("    field.style.color = '000000';");
		blw.writeLine("    field.disabled = false;");
		blw.writeLine("  }");
		blw.writeLine("  else if (fieldState == 'f') {");
		blw.writeLine("    field.style.backgroundColor = '888888';");
		blw.writeLine("    field.style.color = '444444';");
		blw.writeLine("    field.disabled = true;");
		blw.writeLine("  }");
		blw.writeLine("}");
		blw.writeLine("function bibRefEditor_displayFieldError(fieldName, fieldState) {");
		blw.writeLine("  var border = $(fieldName + '_border');");
		blw.writeLine("  if (border == null)");
		blw.writeLine("    return;");
		blw.writeLine("  var fieldValue = bibRefEditor_getFieldValue(fieldName);");
		blw.writeLine("  if (fieldState == 'r')");
		blw.writeLine("    border.style.borderColor = ((fieldValue == '') ? 'FF0000' : 'FFFFFF');");
		blw.writeLine("  else if (fieldState == 'o')");
		blw.writeLine("    border.style.borderColor = 'FFFFFF';");
		blw.writeLine("  else if (fieldState == 'f')");
		blw.writeLine("    border.style.borderColor = 'FFFFFF';");
		blw.writeLine("}");
		
		blw.writeLine("function bibRefEditor_setRef(ref) {");
		blw.writeLine("  if (ref == null)");
		blw.writeLine("    ref = new Object();");
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				blw.writeLine("  bibRefEditor_setFieldValue('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "', ref['" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "']);");
				blw.writeLine("  bibRefEditor_setFieldValue('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "', ref['" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "']);");
				blw.writeLine("  bibRefEditor_setFieldValue('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "', ref['" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "']);");
			}
			else blw.writeLine("  bibRefEditor_setFieldValue('" + BibRefEditor.fieldNames[f] + "', ref['" + BibRefEditor.fieldNames[f] + "']);");
		}
		if (idTypes != null)
			for (int i = 0; i < idTypes.length; i++) {
				if (idTypes[i] == null)
					continue;
				String idType = idTypes[i].trim();
				if (idType.length() == 0)
					continue;
				blw.writeLine("  bibRefEditor_setFieldValue('" + ("ID-" + idType) + "', ref['" + ("ID-" + idType) + "']);");
			}
		blw.writeLine("  var refTypeField = $('" + PUBLICATION_TYPE_ATTRIBUTE + "_field');");
		blw.writeLine("  if (refTypeField != null)");
		blw.writeLine("    refTypeField.value = ref['" + PUBLICATION_TYPE_ATTRIBUTE + "'];");
		blw.writeLine("  bibRefEditor_refTypeChanged();");
		blw.writeLine("}");
		blw.writeLine("function bibRefEditor_setFieldValue(fieldName, fieldValue) {");
		blw.writeLine("  var field = $(fieldName + '_field');");
		blw.writeLine("  if (field == null)");
		blw.writeLine("    return;");
		blw.writeLine("  field.value = ((fieldValue == null) ? '' : fieldValue);");
		blw.writeLine("  if ((fieldName == '" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "') || (fieldName == '" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "') || (fieldName == '" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "'))");
		blw.writeLine("    bibRefEditor_fieldValueChanged('" + PART_DESIGNATOR_ANNOTATION_TYPE + "');");
		blw.writeLine("  else bibRefEditor_fieldValueChanged(fieldName);");
		blw.writeLine("}");
		
		blw.writeLine("function bibRefEditor_getRefErrors() {");
		blw.writeLine("  var errors = new Object();");
		blw.writeLine("  var errorCount = 0;");
		blw.writeLine("  var refType = bibRefEditor_getRefType();");
		blw.writeLine("  if (refType == '')");
		blw.writeLine("    return null;");
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			blw.writeLine("  if ((bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType) == 'r') && (bibRefEditor_getFieldValue('" + BibRefEditor.fieldNames[f] + "') == '')) {");
			blw.writeLine("    errors['' + errorCount] = 'The " + BibRefEditor.getFieldLabel(BibRefEditor.fieldNames[f]) + " field is empty.';");
			blw.writeLine("    errorCount++;");
			blw.writeLine("  }");
		}
		if ((idTypes != null) && (idTypes.length != 0) && (idTypes[0] != null) && (idTypes[0].trim().length() != 0)) {
			blw.writeLine("  if (bibRefEditor_getFieldValue('" + ("ID-" + idTypes[0]) + "') == '') {");
			blw.writeLine("    errors['' + errorCount] = 'The Primary Identifier field is empty.';");
			blw.writeLine("    errorCount++;");
			blw.writeLine("  }");
		}
		blw.writeLine("  return ((errorCount == 0) ? null : errors);");
		blw.writeLine("}");
		
		blw.writeLine("function bibRefEditor_getRef() {");
		blw.writeLine("  var ref = new Object();");
		blw.writeLine("  var refType = bibRefEditor_getRefType();");
		blw.writeLine("  if (refType != '')");
		blw.writeLine("    ref['" + PUBLICATION_TYPE_ATTRIBUTE + "'] = refType;");
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				blw.writeLine("  bibRefEditor_setRefAttribute('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "', ref, refType);");
				blw.writeLine("  bibRefEditor_setRefAttribute('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "', ref, refType);");
				blw.writeLine("  bibRefEditor_setRefAttribute('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "', ref, refType);");
			}
			else blw.writeLine("  bibRefEditor_setRefAttribute('" + BibRefEditor.fieldNames[f] + "', ref, refType);");
		}
		if (idTypes != null)
			for (int i = 0; i < idTypes.length; i++) {
				if (idTypes[i] == null)
					continue;
				String idType = idTypes[i].trim();
				if (idType.length() == 0)
					continue;
				blw.writeLine("  bibRefEditor_setRefAttribute('" + ("ID-" + idType) + "', ref, refType);");
			}
		blw.writeLine("  return ref;");
		blw.writeLine("}");
		blw.writeLine("function bibRefEditor_setRefAttribute(fieldName, ref, refType) {");
		blw.writeLine("  if (bibRefEditor_getFieldState(fieldName, refType) == 'f')");
		blw.writeLine("    return;");
		blw.writeLine("  var fieldValue = bibRefEditor_getFieldValue(fieldName);");
		blw.writeLine("  if (fieldValue != '')");
		blw.writeLine("    ref[fieldName] = fieldValue;");
		blw.writeLine("}");
		blw.writeLine("function $(id) {");
		blw.writeLine("  return document.getElementById(id);");
		blw.writeLine("}");
		
		blw.writeLine("function bibRefEditor_addRefAttributeInputs(form) {");
		blw.writeLine("  var refType = bibRefEditor_getRefType();");
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				blw.writeLine("  bibRefEditor_addRefAttributeInput('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "', form, refType);");
				blw.writeLine("  bibRefEditor_addRefAttributeInput('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "', form, refType);");
				blw.writeLine("  bibRefEditor_addRefAttributeInput('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "', form, refType);");
			}
			else blw.writeLine("  bibRefEditor_addRefAttributeInput('" + BibRefEditor.fieldNames[f] + "', form, refType);");
		}
		if (idTypes != null)
			for (int i = 0; i < idTypes.length; i++) {
				if (idTypes[i] == null)
					continue;
				String idType = idTypes[i].trim();
				if (idType.length() == 0)
					continue;
				blw.writeLine("  bibRefEditor_addRefAttributeInput('" + ("ID-" + idType) + "', form, refType);");
			}
		blw.writeLine("}");
		blw.writeLine("function bibRefEditor_addRefAttributeInput(fieldName, form, refType) {");
		blw.writeLine("  if (bibRefEditor_getFieldState(fieldName, refType) == 'f')");
		blw.writeLine("    return;");
		blw.writeLine("  var fieldValue = (('" + PUBLICATION_TYPE_ATTRIBUTE + "' == fieldName) ? refType : bibRefEditor_getFieldValue(fieldName));");
		blw.writeLine("  if (fieldValue == '')");
		blw.writeLine("    return;");
		blw.writeLine("  var input = document.createElement('input');");
		blw.writeLine("  bibRefEditor_setInputAttribute(input, 'type', 'hidden');");
		blw.writeLine("  bibRefEditor_setInputAttribute(input, 'name', fieldName);");
		blw.writeLine("  bibRefEditor_setInputAttribute(input, 'value', fieldValue);");
		blw.writeLine("  form.appendChild(input);");
		blw.writeLine("}");
		blw.writeLine("function bibRefEditor_setInputAttribute(input, name, value) {");
		blw.writeLine("  if (!input.setAttributeNode)");
		blw.writeLine("    return;");
		blw.writeLine("  var attribute = document.createAttribute(name);");
		blw.writeLine("  attribute.nodeValue = value;");
		blw.writeLine("  input.setAttributeNode(attribute);");
		blw.writeLine("}");
		
		blw.writeLine("</script>");
		
		if (blw != out)
			blw.flush();
	}
	
	/**
	 * Write the HTML code for the fields in a reference editor form to some
	 * writer. If the includeJavaScripts parameter is set to false, the
	 * writeJavaScripts() method has to be called by client code in oder to make
	 * sure the JavaScript functions required for the form to work are
	 * available. In addition, client code has to make sure that the
	 * bibRefEditor_refTypeChanged() function is called after the form fields
	 * are loaded completely, so to finalizy initialization.
	 * @param out the writer to write to
	 * @param includeJavaScripts include JavaScript functions right above the
	 *            HTML code for the fields?
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 * @throws IOException
	 */
	public static void createHtmlForm(Writer out, boolean includeJavaScripts, String[] idTypes) throws IOException {
		createHtmlForm(out, includeJavaScripts, null, idTypes, null);
	}
	
	/**
	 * Write the HTML code for the fields in a reference editor form to some
	 * writer. If the includeJavaScripts parameter is set to false, the
	 * writeJavaScripts() method has to be called by client code in oder to make
	 * sure the JavaScript functions required for the form to work are
	 * available. In addition, client code has to make sure that the
	 * bibRefEditor_refTypeChanged() function is called after the form fields
	 * are loaded completely, so to finalizy initialization.
	 * @param out the writer to write to
	 * @param includeJavaScripts include JavaScript functions right above the
	 *            HTML code for the fields?
	 * @param typeSystem the reference type system to use for data validation
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 * @throws IOException
	 */
	public static void createHtmlForm(Writer out, boolean includeJavaScripts, BibRefTypeSystem typeSystem, String[] idTypes) throws IOException {
		createHtmlForm(out, includeJavaScripts, typeSystem, idTypes, null);
	}
	
	/**
	 * Write the HTML code for the fields in a reference editor form to some
	 * writer. If the includeJavaScripts parameter is set to false, the
	 * writeJavaScripts() method has to be called by client code in oder to make
	 * sure the JavaScript functions required for the form to work are
	 * available. In addition, client code has to make sure that the
	 * bibRefEditor_refTypeChanged() function is called after the form fields
	 * are loaded completely, so to finalizy initialization.
	 * @param out the writer to write to
	 * @param includeJavaScripts include JavaScript functions right above the
	 *            HTML code for the fields?
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 * @param ref the reference data set to display initially
	 * @throws IOException
	 */
	public static void createHtmlForm(Writer out, boolean includeJavaScripts, RefData ref, String[] idTypes) throws IOException {
		createHtmlForm(out, includeJavaScripts, null, idTypes, ref);
	}
	
	/**
	 * Write the HTML code for the fields in a reference editor form to some
	 * writer. If the includeJavaScripts parameter is set to false, the
	 * writeJavaScripts() method has to be called by client code in oder to make
	 * sure the JavaScript functions required for the form to work are
	 * available. In addition, client code has to make sure that the
	 * bibRefEditor_refTypeChanged() function is called after the form fields
	 * are loaded completely, so to finalizy initialization.
	 * @param out the writer to write to
	 * @param includeJavaScripts include JavaScript functions right above the
	 *            HTML code for the fields?
	 * @param typeSystem the reference type system to use for data validation
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 * @param ref the reference data set to display initially
	 * @throws IOException
	 */
	public static void createHtmlForm(Writer out, boolean includeJavaScripts, BibRefTypeSystem typeSystem, String[] idTypes, RefData ref) throws IOException {
		if (typeSystem == null)
			typeSystem = BibRefTypeSystem.getDefaultInstance();
		
		BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
		
		if (includeJavaScripts)
			writeJavaScripts(blw, typeSystem, idTypes);
		
		blw.writeLine("<table class=\"bibRefEditorTable\">");
		blw.writeLine("<tr>");
		blw.writeLine("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">Publication Type:</td>");
		if ((idTypes != null) && (idTypes.length != 0) && (idTypes[0] != null) && (idTypes[0].trim().length() != 0)) {
			blw.writeLine("<td class=\"bibRefEditorFieldCell\"><select class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + PUBLICATION_TYPE_ATTRIBUTE + "_field\" name=\"" + PUBLICATION_TYPE_ATTRIBUTE + "\" onchange=\"bibRefEditor_refTypeChanged();\">");
			BibRefType[] brts = typeSystem.getBibRefTypes();
			for (int t = 0; t < brts.length; t++)
				blw.writeLine("<option value=\"" + brts[t].name + "\">" + brts[t].getLabel() + "</option>");
			blw.writeLine("</select></td>");
			blw.writeLine("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + idTypes[0] + " Identifier" + ":</td>");
			blw.writeLine("<td class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style:solid;\" id=\"" + ("ID-" + idTypes[0]) + "_border\"><input class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + ("ID-" + idTypes[0]) + "_field\" name=\"" + ("ID-" + idTypes[0]) + "\" onchange=\"bibRefEditor_fieldValueChanged('" + ("ID-" + idTypes[0]) + "');\" /></div></td>");
		}
		else {
			blw.writeLine("<td colspan=\"3\" class=\"bibRefEditorFieldCell\"><select class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + PUBLICATION_TYPE_ATTRIBUTE + "_field\" name=\"" + PUBLICATION_TYPE_ATTRIBUTE + "\" onchange=\"bibRefEditor_refTypeChanged();\">");
			BibRefType[] brts = typeSystem.getBibRefTypes();
			for (int t = 0; t < brts.length; t++)
				blw.writeLine("<option value=\"" + brts[t].name + "\">" + brts[t].getLabel() + "</option>");
			blw.writeLine("</select></td>");
		}
		blw.writeLine("</tr>");
		
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			blw.writeLine("<tr>");
			if (((f+1) < BibRefEditor.fieldNames.length) && YEAR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f]) && PAGINATION_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f+1])) {
				blw.writeLine("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + BibRefEditor.getFieldLabel(BibRefEditor.fieldNames[f]) + ":</td>");
				blw.writeLine("<td class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style:solid;\" id=\"" + BibRefEditor.fieldNames[f] + "_border\"><input class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + BibRefEditor.fieldNames[f] + "_field\" name=\"" + BibRefEditor.fieldNames[f] + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" /></div></td>");
				f++;
				blw.writeLine("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + BibRefEditor.getFieldLabel(BibRefEditor.fieldNames[f]) + ":</td>");
				blw.writeLine("<td class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style:solid;\" id=\"" + BibRefEditor.fieldNames[f] + "_border\"><input class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + BibRefEditor.fieldNames[f] + "_field\" name=\"" + BibRefEditor.fieldNames[f] + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" /></div></td>");
			}
			else if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				blw.writeLine("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">Part Designators:</td>");
				blw.write("<td colspan=\"3\" class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style:solid;\" id=\"" + BibRefEditor.fieldNames[f] + "_border\">");
				blw.write(" " + VOLUME_DESIGNATOR_ANNOTATION_TYPE + ": <input class=\"bibRefEditorPartField\" style=\"width: 40px;\" id=\"" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "_field\" name=\"" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" />");
				blw.write(" " + ISSUE_DESIGNATOR_ANNOTATION_TYPE + ": <input class=\"bibRefEditorPartField\" style=\"width: 40px;\" id=\"" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "_field\" name=\"" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" />");
				blw.write(" " + NUMERO_DESIGNATOR_ANNOTATION_TYPE + ": <input class=\"bibRefEditorPartField\" style=\"width: 40px;\" id=\"" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "_field\" name=\"" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" />");
				blw.writeLine("</div></td>");
			}
			else {
				if (AUTHOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f]) || EDITOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f]))
					blw.writeLine("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + (StringUtils.capitalize(BibRefEditor.fieldNames[f]) + "s (use '&amp;' to separate)") + ":</td>");
				else blw.writeLine("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + BibRefEditor.getFieldLabel(BibRefEditor.fieldNames[f]) + ":</td>");
				blw.writeLine("<td colspan=\"3\" class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style:solid;\" id=\"" + BibRefEditor.fieldNames[f] + "_border\"><input class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + BibRefEditor.fieldNames[f] + "_field\" name=\"" + BibRefEditor.fieldNames[f] + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" /></div></td>");
			}
			blw.writeLine("</tr>");
		}
		
		if (idTypes != null)
			for (int t = 1; t < idTypes.length; t++) {
				if (idTypes[t] == null)
					continue;
				String idType = idTypes[t].trim();
				if (idType.length() == 0)
					continue;
				blw.writeLine("<tr>");
				blw.writeLine("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + idType + " Identifier" + ":</td>");
				blw.writeLine("<td colspan=\"3\" class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style:solid;\" id=\"" + ("ID-" + idType) + "_border\"><input class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + ("ID-" + idType) + "_field\" name=\"" + ("ID-" + idType) + "\" onchange=\"bibRefEditor_fieldValueChanged('" + ("ID-" + idType) + "');\" /></div></td>");
				blw.writeLine("</tr>");
			}
		
		blw.writeLine("</table>");
		
		if (includeJavaScripts) {
			blw.writeLine("<script type=\"text/javascript\">");
			blw.writeLine("  bibRefEditor_refTypeChanged();");
			blw.writeLine("</script>");
		}
		
		if (blw != out)
			blw.flush();
	}
	
	/**
	 * Generate JavaScript code creating an object that has the same attributes
	 * as the argument reference data set. Multiple author or editor names are
	 * concatenated, separated by '&amp;'. Client code has to generate the
	 * JavaScript code declaring the object the code generated by this method
	 * adds attributes to.
	 * @param out the writer to write to
	 * @param rd the reference data set to output
	 * @param jsObjectName the name of the JavaScript object representing the
	 *            reference
	 * @throws IOException
	 */
	public static void writeRefDataAsJavaScriptObject(Writer out, RefData rd, String jsObjectName) throws IOException {
		BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
		
		String refType = rd.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		if (refType != null)
			blw.writeLine("  " + jsObjectName + "['" + PUBLICATION_TYPE_ATTRIBUTE + "'] = '" + escapeForJavaScript(refType) + "';");
		
		String[] attributeNames = rd.getAttributeNames();
		for (int a = 0; a < attributeNames.length; a++) {
			if (PUBLICATION_TYPE_ATTRIBUTE.equals(attributeNames[a]) || PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(attributeNames[a]))
				continue;
			if (AUTHOR_ANNOTATION_TYPE.equals(attributeNames[a]) || EDITOR_ANNOTATION_TYPE.equals(attributeNames[a])) {
				String[] values = rd.getAttributeValues(attributeNames[a]);
				if (values == null)
					continue;
				blw.write("  " + jsObjectName + "['" + attributeNames[a] + "'] = '" + escapeForJavaScript(values[0]));
				for (int v = 1; v < values.length; v++)
					blw.write(" & " + escapeForJavaScript(values[v]));
				blw.writeLine("';");
			}
			else {
				String value = rd.getAttribute(attributeNames[a]);
				if (value == null)
					continue;
				blw.writeLine("  " + jsObjectName + "['" + attributeNames[a] + "'] = '" + escapeForJavaScript(value) + "';");
			}
		}
		
		String[] idTypes = rd.getIdentifierTypes();
		for (int i = 0; i < idTypes.length; i++) {
			String id = rd.getIdentifier(idTypes[i]);
			if (id == null)
				continue;
			blw.writeLine("  " + jsObjectName + "['ID-" + idTypes[i] + "'] = '" + escapeForJavaScript(id) + "';");
		}
		
		if (blw != out)
			blw.flush();
	}
	
	/**
	 * Escape a string for JavaScript, i.e., escape ''' and '\' with '\', and
	 * replace any line breaks with spaces.
	 * @param str the string to escape
	 * @return the escaped string
	 */
	public static String escapeForJavaScript(String str) {
		StringBuffer escaped = new StringBuffer();
		char ch;
		for (int c = 0; c < str.length(); c++) {
			ch = str.charAt(c);
			if ((ch == '\\') || (ch == '\''))
				escaped.append('\\');
			if (ch < 32)
				escaped.append(' ');
			else escaped.append(ch);
		}
		return escaped.toString();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String[] idTypes = {"SMNK-Pub", "HNS-PUB", "TEST"};
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
		bw.write("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"></head><body><!--");
		bw.flush();
		BibRefTypeSystem.getDefaultInstance();
		bw.write("-->");
		createHtmlForm(bw, true, idTypes);
		bw.write("</body></html>");
		bw.flush();
	}
}
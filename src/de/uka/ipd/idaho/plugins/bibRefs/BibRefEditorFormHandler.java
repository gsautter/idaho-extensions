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

import de.uka.ipd.idaho.plugins.bibRefs.BibRefTypeSystem.BibRefType;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * HTML and JavaScript code generator creating form fields for entering and
 * editing bibliographic references in a web browser.
 * 
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
		
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		
		bw.write("<script type=\"text/javascript\">");bw.newLine();
		bw.write("function bibRefEditor_getFieldState(fieldName, refType) {");bw.newLine();
		String[] brtns = typeSystem.getBibRefTypeNames();
		for (int t = 0; t < brtns.length; t++) {
			bw.write("  " + ((t == 0) ? "" : "else ") + "if (refType == '" + brtns[t] + "') {");bw.newLine();
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
				bw.write("    if (fieldName == '" + BibRefEditor.fieldNames[f] + "')");bw.newLine();
				bw.write("      return '" + fs + "';");bw.newLine();
			}
			if (idTypes != null)
				for (int i = 0; i < idTypes.length; i++) {
					if (idTypes[i] == null)
						continue;
					String idType = idTypes[i].trim();
					if (idType.length() == 0)
						continue;
					bw.write("    if (fieldName == '" + ("ID-" + idType) + "')");bw.newLine();
					bw.write("      return '" + ((i == 0) ? "r" : "o") + "';");bw.newLine();
				}
			bw.write("  }");bw.newLine();
		}
		bw.write("  return 'o';");bw.newLine();
		bw.write("}");bw.newLine();
		
		bw.write("function bibRefEditor_getRefType() {");bw.newLine();
		bw.write("  var field = $('" + PUBLICATION_TYPE_ATTRIBUTE + "_field');");bw.newLine();
		bw.write("  return ((field == null) ? '' : field.value);");bw.newLine();
		bw.write("}");bw.newLine();
		bw.write("function bibRefEditor_getFieldValue(fieldName) {");bw.newLine();
		bw.write("  if (fieldName == '" + PART_DESIGNATOR_ANNOTATION_TYPE + "')");bw.newLine();
		bw.write("    return (bibRefEditor_getFieldValue('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "') + bibRefEditor_getFieldValue('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "') + bibRefEditor_getFieldValue('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "'));");bw.newLine();
		bw.write("  else {");bw.newLine();
		bw.write("    var field = $(fieldName + '_field');");bw.newLine();
		bw.write("    return ((field == null) ? '' : field.value);");bw.newLine();
		bw.write("  }");bw.newLine();
		bw.write("}");bw.newLine();
		
		bw.write("function bibRefEditor_refTypeChanged() {");bw.newLine();
		bw.write("  var refType = bibRefEditor_getRefType();");bw.newLine();
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				bw.write("  bibRefEditor_displayFieldState('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType));");bw.newLine();
				bw.write("  bibRefEditor_displayFieldState('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType));");bw.newLine();
				bw.write("  bibRefEditor_displayFieldState('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType));");bw.newLine();
			}
			else {bw.write("  bibRefEditor_displayFieldState('" + BibRefEditor.fieldNames[f] + "', bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType));");bw.newLine();}
			bw.write("  bibRefEditor_displayFieldError('" + BibRefEditor.fieldNames[f] + "', bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType));");bw.newLine();
		}
		if (idTypes != null)
			for (int i = 0; i < idTypes.length; i++) {
				if (idTypes[i] == null)
					continue;
				String idType = idTypes[i].trim();
				if (idType.length() == 0)
					continue;
				bw.write("  bibRefEditor_displayFieldState('" + ("ID-" + idType) + "', bibRefEditor_getFieldState('" + ("ID-" + idType) + "', refType));");bw.newLine();
				bw.write("  bibRefEditor_displayFieldError('" + ("ID-" + idType) + "', bibRefEditor_getFieldState('" + ("ID-" + idType) + "', refType));");bw.newLine();
			}
		bw.write("}");bw.newLine();
		bw.write("function bibRefEditor_fieldValueChanged(fieldName) {");bw.newLine();
		bw.write("  if (fieldName == '" + PART_DESIGNATOR_ANNOTATION_TYPE + "') {");bw.newLine();
		bw.write("    bibRefEditor_displayFieldState('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState(fieldName, bibRefEditor_getRefType()));");bw.newLine();
		bw.write("    bibRefEditor_displayFieldState('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState(fieldName, bibRefEditor_getRefType()));");bw.newLine();
		bw.write("    bibRefEditor_displayFieldState('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "', bibRefEditor_getFieldState(fieldName, bibRefEditor_getRefType()));");bw.newLine();
		bw.write("  }");bw.newLine();
		bw.write("  else bibRefEditor_displayFieldState(fieldName, bibRefEditor_getFieldState(fieldName, bibRefEditor_getRefType()));");bw.newLine();
		bw.write("  bibRefEditor_displayFieldError(fieldName, bibRefEditor_getFieldState(fieldName, bibRefEditor_getRefType()));");bw.newLine();
		bw.write("}");bw.newLine();
		
		bw.write("function bibRefEditor_displayFieldState(fieldName, fieldState) {");bw.newLine();
		bw.write("  var field = $(fieldName + '_field');");bw.newLine();
		bw.write("  if (field == null)");bw.newLine();
		bw.write("    return;");bw.newLine();
		bw.write("  if (fieldState == 'r') {");bw.newLine();
		bw.write("    field.style.backgroundColor = '#FFFFFF';");bw.newLine();
		bw.write("    field.style.color = '#000000';");bw.newLine();
		bw.write("    field.disabled = false;");bw.newLine();
		bw.write("  }");bw.newLine();
		bw.write("  else if (fieldState == 'o') {");bw.newLine();
		bw.write("    field.style.backgroundColor = '#EEEEEE';");bw.newLine();
		bw.write("    field.style.color = '#000000';");bw.newLine();
		bw.write("    field.disabled = false;");bw.newLine();
		bw.write("  }");bw.newLine();
		bw.write("  else if (fieldState == 'f') {");bw.newLine();
		bw.write("    field.style.backgroundColor = '#BBBBBB';");bw.newLine();
		bw.write("    field.style.color = '#888888';");bw.newLine();
		bw.write("    field.disabled = true;");bw.newLine();
		bw.write("  }");bw.newLine();
		bw.write("}");bw.newLine();
		bw.write("function bibRefEditor_displayFieldError(fieldName, fieldState) {");bw.newLine();
		bw.write("  var border = $(fieldName + '_border');");bw.newLine();
		bw.write("  if (border == null)");bw.newLine();
		bw.write("    return;");bw.newLine();
		bw.write("  var fieldValue = bibRefEditor_getFieldValue(fieldName);");bw.newLine();
		bw.write("  if (fieldState == 'r')");bw.newLine();
		bw.write("    border.style.borderColor = ((fieldValue == '') ? '#FF0000' : '#FFFFFF');");bw.newLine();
		bw.write("  else if (fieldState == 'o')");bw.newLine();
		bw.write("    border.style.borderColor = '#FFFFFF';");bw.newLine();
		bw.write("  else if (fieldState == 'f')");bw.newLine();
		bw.write("    border.style.borderColor = '#FFFFFF';");bw.newLine();
		bw.write("}");bw.newLine();
		
		bw.write("function bibRefEditor_setRef(ref) {");bw.newLine();
		bw.write("  if (ref == null)");bw.newLine();
		bw.write("    ref = new Object();");bw.newLine();
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				bw.write("  bibRefEditor_setFieldValue('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "', ref['" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "']);");bw.newLine();
				bw.write("  bibRefEditor_setFieldValue('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "', ref['" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "']);");bw.newLine();
				bw.write("  bibRefEditor_setFieldValue('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "', ref['" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "']);");bw.newLine();
			}
			else {bw.write("  bibRefEditor_setFieldValue('" + BibRefEditor.fieldNames[f] + "', ref['" + BibRefEditor.fieldNames[f] + "']);");bw.newLine();}
		}
		if (idTypes != null)
			for (int i = 0; i < idTypes.length; i++) {
				if (idTypes[i] == null)
					continue;
				String idType = idTypes[i].trim();
				if (idType.length() == 0)
					continue;
				bw.write("  bibRefEditor_setFieldValue('" + ("ID-" + idType) + "', ref['" + ("ID-" + idType) + "']);");bw.newLine();
			}
		bw.write("  var refTypeField = $('" + PUBLICATION_TYPE_ATTRIBUTE + "_field');");bw.newLine();
		bw.write("  if (refTypeField != null)");bw.newLine();
		bw.write("    refTypeField.value = ref['" + PUBLICATION_TYPE_ATTRIBUTE + "'];");bw.newLine();
		bw.write("  bibRefEditor_refTypeChanged();");bw.newLine();
		bw.write("}");bw.newLine();
		bw.write("function bibRefEditor_setFieldValue(fieldName, fieldValue) {");bw.newLine();
		bw.write("  var field = $(fieldName + '_field');");bw.newLine();
		bw.write("  if (field == null)");bw.newLine();
		bw.write("    return;");bw.newLine();
		bw.write("  field.value = ((fieldValue == null) ? '' : fieldValue);");bw.newLine();
		bw.write("  if ((fieldName == '" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "') || (fieldName == '" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "') || (fieldName == '" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "'))");bw.newLine();
		bw.write("    bibRefEditor_fieldValueChanged('" + PART_DESIGNATOR_ANNOTATION_TYPE + "');");bw.newLine();
		bw.write("  else bibRefEditor_fieldValueChanged(fieldName);");bw.newLine();
		bw.write("}");bw.newLine();
		
		bw.write("function bibRefEditor_getRefErrors() {");bw.newLine();
		bw.write("  var errors = new Object();");bw.newLine();
		bw.write("  var errorCount = 0;");bw.newLine();
		bw.write("  var refType = bibRefEditor_getRefType();");bw.newLine();
		bw.write("  if (refType == '')");bw.newLine();
		bw.write("    return null;");bw.newLine();
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			bw.write("  if ((bibRefEditor_getFieldState('" + BibRefEditor.fieldNames[f] + "', refType) == 'r') && (bibRefEditor_getFieldValue('" + BibRefEditor.fieldNames[f] + "') == '')) {");bw.newLine();
			bw.write("    errors['' + errorCount] = 'The " + BibRefEditor.getFieldLabel(BibRefEditor.fieldNames[f]) + " field is empty.';");bw.newLine();
			bw.write("    errorCount++;");bw.newLine();
			bw.write("  }");bw.newLine();
		}
		if ((idTypes != null) && (idTypes.length != 0) && (idTypes[0] != null) && (idTypes[0].trim().length() != 0)) {
			bw.write("  if (bibRefEditor_getFieldValue('" + ("ID-" + idTypes[0]) + "') == '') {");bw.newLine();
			bw.write("    errors['' + errorCount] = 'The Primary Identifier field is empty.';");bw.newLine();
			bw.write("    errorCount++;");bw.newLine();
			bw.write("  }");bw.newLine();
		}
		bw.write("  return ((errorCount == 0) ? null : errors);");bw.newLine();
		bw.write("}");bw.newLine();
		
		bw.write("function bibRefEditor_getRef() {");bw.newLine();
		bw.write("  var ref = new Object();");bw.newLine();
		bw.write("  var refType = bibRefEditor_getRefType();");bw.newLine();
		bw.write("  if (refType != '')");bw.newLine();
		bw.write("    ref['" + PUBLICATION_TYPE_ATTRIBUTE + "'] = refType;");bw.newLine();
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				bw.write("  bibRefEditor_setRefAttribute('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "', ref, refType);");bw.newLine();
				bw.write("  bibRefEditor_setRefAttribute('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "', ref, refType);");bw.newLine();
				bw.write("  bibRefEditor_setRefAttribute('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "', ref, refType);");bw.newLine();
			}
			else {bw.write("  bibRefEditor_setRefAttribute('" + BibRefEditor.fieldNames[f] + "', ref, refType);");bw.newLine();}
		}
		if (idTypes != null)
			for (int i = 0; i < idTypes.length; i++) {
				if (idTypes[i] == null)
					continue;
				String idType = idTypes[i].trim();
				if (idType.length() == 0)
					continue;
				bw.write("  bibRefEditor_setRefAttribute('" + ("ID-" + idType) + "', ref, refType);");bw.newLine();
			}
		bw.write("  return ref;");bw.newLine();
		bw.write("}");bw.newLine();
		bw.write("function bibRefEditor_setRefAttribute(fieldName, ref, refType) {");bw.newLine();
		bw.write("  if (bibRefEditor_getFieldState(fieldName, refType) == 'f')");bw.newLine();
		bw.write("    return;");bw.newLine();
		bw.write("  var fieldValue = bibRefEditor_getFieldValue(fieldName);");bw.newLine();
		bw.write("  if (fieldValue != '')");bw.newLine();
		bw.write("    ref[fieldName] = fieldValue;");bw.newLine();
		bw.write("}");bw.newLine();
		bw.write("function $(id) {");bw.newLine();
		bw.write("  return document.getElementById(id);");bw.newLine();
		bw.write("}");bw.newLine();
		
		bw.write("function bibRefEditor_addRefAttributeInputs(form) {");bw.newLine();
		bw.write("  var refType = bibRefEditor_getRefType();");bw.newLine();
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				bw.write("  bibRefEditor_addRefAttributeInput('" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "', form, refType);");bw.newLine();
				bw.write("  bibRefEditor_addRefAttributeInput('" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "', form, refType);");bw.newLine();
				bw.write("  bibRefEditor_addRefAttributeInput('" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "', form, refType);");bw.newLine();
			}
			else {bw.write("  bibRefEditor_addRefAttributeInput('" + BibRefEditor.fieldNames[f] + "', form, refType);");bw.newLine();}
		}
		if (idTypes != null)
			for (int i = 0; i < idTypes.length; i++) {
				if (idTypes[i] == null)
					continue;
				String idType = idTypes[i].trim();
				if (idType.length() == 0)
					continue;
				bw.write("  bibRefEditor_addRefAttributeInput('" + ("ID-" + idType) + "', form, refType);");bw.newLine();
			}
		bw.write("}");bw.newLine();
		bw.write("function bibRefEditor_addRefAttributeInput(fieldName, form, refType) {");bw.newLine();
		bw.write("  if (bibRefEditor_getFieldState(fieldName, refType) == 'f')");bw.newLine();
		bw.write("    return;");bw.newLine();
		bw.write("  var fieldValue = (('" + PUBLICATION_TYPE_ATTRIBUTE + "' == fieldName) ? refType : bibRefEditor_getFieldValue(fieldName));");bw.newLine();
		bw.write("  if (fieldValue == '')");bw.newLine();
		bw.write("    return;");bw.newLine();
		bw.write("  var input = document.createElement('input');");bw.newLine();
		bw.write("  bibRefEditor_setInputAttribute(input, 'type', 'hidden');");bw.newLine();
		bw.write("  bibRefEditor_setInputAttribute(input, 'name', fieldName);");bw.newLine();
		bw.write("  bibRefEditor_setInputAttribute(input, 'value', fieldValue);");bw.newLine();
		bw.write("  form.appendChild(input);");bw.newLine();
		bw.write("}");bw.newLine();
		bw.write("function bibRefEditor_setInputAttribute(input, name, value) {");bw.newLine();
		bw.write("  if (!input.setAttributeNode)");bw.newLine();
		bw.write("    return;");bw.newLine();
		bw.write("  var attribute = document.createAttribute(name);");bw.newLine();
		bw.write("  attribute.nodeValue = value;");bw.newLine();
		bw.write("  input.setAttributeNode(attribute);");bw.newLine();
		bw.write("}");
		
		bw.write("</script>");bw.newLine();
		
		if (bw != out)
			bw.flush();
	}
	
	/**
	 * Write the HTML code for the fields in a reference editor form to some
	 * writer. If the includeJavaScripts parameter is set to false, the
	 * writeJavaScripts() method has to be called by client code in order to
	 * make sure the JavaScript functions required for the form to work are
	 * available. In addition, client code has to make sure that the
	 * bibRefEditor_refTypeChanged() function is called after the form fields
	 * are loaded completely, so to finalize initialization.
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
	 * writeJavaScripts() method has to be called by client code in order to
	 * make sure the JavaScript functions required for the form to work are
	 * available. In addition, client code has to make sure that the
	 * bibRefEditor_refTypeChanged() function is called after the form fields
	 * are loaded completely, so to finalize initialization.
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
	 * writeJavaScripts() method has to be called by client code in order to
	 * make sure the JavaScript functions required for the form to work are
	 * available. In addition, client code has to make sure that the
	 * bibRefEditor_refTypeChanged() function is called after the form fields
	 * are loaded completely, so to finalize initialization.
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
	 * writeJavaScripts() method has to be called by client code in order to
	 * make sure the JavaScript functions required for the form to work are
	 * available. In addition, client code has to make sure that the
	 * bibRefEditor_refTypeChanged() function is called after the form fields
	 * are loaded completely, so to finalize initialization.
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
		
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		
		if (includeJavaScripts)
			writeJavaScripts(bw, typeSystem, idTypes);
		
		bw.write("<table class=\"bibRefEditorTable\">");bw.newLine();
		bw.write("<tr>");bw.newLine();
		bw.write("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">Publication Type:</td>");bw.newLine();
		if ((idTypes != null) && (idTypes.length != 0) && (idTypes[0] != null) && (idTypes[0].trim().length() != 0)) {
			bw.write("<td class=\"bibRefEditorFieldCell\"><select class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + PUBLICATION_TYPE_ATTRIBUTE + "_field\" name=\"" + PUBLICATION_TYPE_ATTRIBUTE + "\" onchange=\"bibRefEditor_refTypeChanged();\">");bw.newLine();
			BibRefType[] brts = typeSystem.getBibRefTypes();
			for (int t = 0; t < brts.length; t++)
				bw.write("<option value=\"" + brts[t].name + "\">" + brts[t].getLabel() + "</option>");bw.newLine();
			bw.write("</select></td>");bw.newLine();
			bw.write("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + idTypes[0] + " Identifier" + ":</td>");bw.newLine();
			bw.write("<td class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style: solid;\" id=\"" + ("ID-" + idTypes[0]) + "_border\"><input class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + ("ID-" + idTypes[0]) + "_field\" name=\"" + ("ID-" + idTypes[0]) + "\" onchange=\"bibRefEditor_fieldValueChanged('" + ("ID-" + idTypes[0]) + "');\" /></div></td>");bw.newLine();
		}
		else {
			bw.write("<td colspan=\"3\" class=\"bibRefEditorFieldCell\"><select class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + PUBLICATION_TYPE_ATTRIBUTE + "_field\" name=\"" + PUBLICATION_TYPE_ATTRIBUTE + "\" onchange=\"bibRefEditor_refTypeChanged();\">");bw.newLine();
			BibRefType[] brts = typeSystem.getBibRefTypes();
			for (int t = 0; t < brts.length; t++)
				bw.write("<option value=\"" + brts[t].name + "\">" + brts[t].getLabel() + "</option>");bw.newLine();
			bw.write("</select></td>");bw.newLine();
		}
		bw.write("</tr>");bw.newLine();
		
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
			bw.write("<tr>");bw.newLine();
			if (((f+1) < BibRefEditor.fieldNames.length) && YEAR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f]) && PAGINATION_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f+1])) {
				bw.write("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + BibRefEditor.getFieldLabel(BibRefEditor.fieldNames[f]) + ":</td>");bw.newLine();
				bw.write("<td class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style:solid;\" id=\"" + BibRefEditor.fieldNames[f] + "_border\"><input class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + BibRefEditor.fieldNames[f] + "_field\" name=\"" + BibRefEditor.fieldNames[f] + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" /></div></td>");bw.newLine();
				f++;
				bw.write("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + BibRefEditor.getFieldLabel(BibRefEditor.fieldNames[f]) + ":</td>");bw.newLine();
				bw.write("<td class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style:solid;\" id=\"" + BibRefEditor.fieldNames[f] + "_border\"><input class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + BibRefEditor.fieldNames[f] + "_field\" name=\"" + BibRefEditor.fieldNames[f] + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" /></div></td>");bw.newLine();
			}
			else if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				bw.write("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">Part Designators:</td>");bw.newLine();
				bw.write("<td colspan=\"3\" class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style:solid;\" id=\"" + BibRefEditor.fieldNames[f] + "_border\">");
				bw.write(" " + VOLUME_DESIGNATOR_ANNOTATION_TYPE + ": <input class=\"bibRefEditorPartField\" style=\"width: 40px;\" id=\"" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "_field\" name=\"" + VOLUME_DESIGNATOR_ANNOTATION_TYPE + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" />");
				bw.write(" " + ISSUE_DESIGNATOR_ANNOTATION_TYPE + ": <input class=\"bibRefEditorPartField\" style=\"width: 40px;\" id=\"" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "_field\" name=\"" + ISSUE_DESIGNATOR_ANNOTATION_TYPE + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" />");
				bw.write(" " + NUMERO_DESIGNATOR_ANNOTATION_TYPE + ": <input class=\"bibRefEditorPartField\" style=\"width: 40px;\" id=\"" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "_field\" name=\"" + NUMERO_DESIGNATOR_ANNOTATION_TYPE + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" />");
				bw.write("</div></td>");bw.newLine();
			}
			else {
				if (AUTHOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f]) || EDITOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f]))
					{bw.write("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + (StringUtils.capitalize(BibRefEditor.fieldNames[f]) + "s (use '&amp;' to separate)") + ":</td>");bw.newLine();}
				else {bw.write("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + BibRefEditor.getFieldLabel(BibRefEditor.fieldNames[f]) + ":</td>");bw.newLine();}
				bw.write("<td colspan=\"3\" class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style:solid;\" id=\"" + BibRefEditor.fieldNames[f] + "_border\"><input class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + BibRefEditor.fieldNames[f] + "_field\" name=\"" + BibRefEditor.fieldNames[f] + "\" onchange=\"bibRefEditor_fieldValueChanged('" + BibRefEditor.fieldNames[f] + "');\" /></div></td>");bw.newLine();
			}
			bw.write("</tr>");bw.newLine();
		}
		
		if (idTypes != null)
			for (int t = 1; t < idTypes.length; t++) {
				if (idTypes[t] == null)
					continue;
				String idType = idTypes[t].trim();
				if (idType.length() == 0)
					continue;
				bw.write("<tr>");bw.newLine();
				bw.write("<td class=\"bibRefEditorFieldLabel\" style=\"text-align: right;\">" + idType + " Identifier" + ":</td>");bw.newLine();
				bw.write("<td colspan=\"3\" class=\"bibRefEditorFieldCell\"><div style=\"border-width: 2px; border-style:solid;\" id=\"" + ("ID-" + idType) + "_border\"><input class=\"bibRefEditorField\" style=\"width: 100%;\" id=\"" + ("ID-" + idType) + "_field\" name=\"" + ("ID-" + idType) + "\" onchange=\"bibRefEditor_fieldValueChanged('" + ("ID-" + idType) + "');\" /></div></td>");bw.newLine();
				bw.write("</tr>");bw.newLine();
			}
		
		bw.write("</table>");bw.newLine();
		
		if (includeJavaScripts) {
			bw.write("<script type=\"text/javascript\">");bw.newLine();
			bw.write("  bibRefEditor_refTypeChanged();");bw.newLine();
			bw.write("</script>");bw.newLine();
		}
		
		if (bw != out)
			bw.flush();
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
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		
		String refType = rd.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		if (refType != null)
			bw.write("  " + jsObjectName + "['" + PUBLICATION_TYPE_ATTRIBUTE + "'] = '" + escapeForJavaScript(refType) + "';");bw.newLine();
		
		String[] attributeNames = rd.getAttributeNames();
		for (int a = 0; a < attributeNames.length; a++) {
			if (PUBLICATION_TYPE_ATTRIBUTE.equals(attributeNames[a]) || PUBLICATION_IDENTIFIER_ANNOTATION_TYPE.equals(attributeNames[a]))
				continue;
			if (AUTHOR_ANNOTATION_TYPE.equals(attributeNames[a]) || EDITOR_ANNOTATION_TYPE.equals(attributeNames[a])) {
				String[] values = rd.getAttributeValues(attributeNames[a]);
				if (values == null)
					continue;
				bw.write("  " + jsObjectName + "['" + attributeNames[a] + "'] = '" + escapeForJavaScript(values[0]));
				for (int v = 1; v < values.length; v++)
					bw.write(" & " + escapeForJavaScript(values[v]));
				bw.write("';");
				bw.newLine();
			}
			else {
				String value = rd.getAttribute(attributeNames[a]);
				if (value == null)
					continue;
				bw.write("  " + jsObjectName + "['" + attributeNames[a] + "'] = '" + escapeForJavaScript(value) + "';");bw.newLine();
			}
		}
		
		String[] idTypes = rd.getIdentifierTypes();
		for (int i = 0; i < idTypes.length; i++) {
			String id = rd.getIdentifier(idTypes[i]);
			if (id == null)
				continue;
			bw.write("  " + jsObjectName + "['ID-" + idTypes[i] + "'] = '" + escapeForJavaScript(id) + "';");bw.newLine();
		}
		
		if (bw != out)
			bw.flush();
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
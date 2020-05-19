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
package de.uka.ipd.idaho.gamta.util.feedback.html.renderers;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AnnotationEditorFeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AnnotationEditorFeedbackPanel.TokenStates;

/**
 * Renderer for annotation editor feedback panels
 * 
 * @author sautter
 */
public class AnnotationEditorFeedbackPanelRenderer extends FeedbackPanelHtmlRenderer {
	
	/**
	 * Constructor
	 */
	public AnnotationEditorFeedbackPanelRenderer() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer#canRender(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public int canRender(FeedbackPanel fp) {
		return getInheritanceDepth(AnnotationEditorFeedbackPanel.class, fp.getClass());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer#getRendererInstance(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public FeedbackPanelHtmlRendererInstance getRendererInstance(FeedbackPanel fp) {
		return ((fp instanceof AnnotationEditorFeedbackPanel) ? new AnnotationEditorFeedbackPanelRendererInstance((AnnotationEditorFeedbackPanel) fp) : null);
	}
	
	private static class AnnotationEditorFeedbackPanelRendererInstance extends FeedbackPanelHtmlRendererInstance {
		private AnnotationEditorFeedbackPanel aefp;
		private TokenStates[] aeTokenStates;
		AnnotationEditorFeedbackPanelRendererInstance(AnnotationEditorFeedbackPanel aefp) {
			super(aefp);
			this.aefp = aefp;
			this.aeTokenStates = new TokenStates[this.aefp.annotationCount()];
			for (int a = 0; a < this.aefp.annotationCount(); a++)
				this.aeTokenStates[a] = this.aefp.getTokenStatesAt(a);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writeJavaScriptInitFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
//			blw.writeLine("  initContext();");
			blw.writeLine("  initAnnotEditor();");
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine("function getById(id) {");
			blw.writeLine("  return document.getElementById(id);");
			blw.writeLine("}");
			blw.writeLine("function getByName(name) {");
			blw.writeLine("  var elements = document.getElementsByTagName(name);");
			blw.writeLine("  return (((elements != null) && (elements.length != 0)) ? elements[0] : null);");
			blw.writeLine("}");
			blw.writeLine("function newElement(type, id, cssClass, text) {");
			blw.writeLine("  var element = document.createElement(type);");
			blw.writeLine("  if (id != null)");
			blw.writeLine("    setAttribute(element, 'id', id);");
			blw.writeLine("  if (cssClass != null)");
			blw.writeLine("    setAttribute(element, 'class', cssClass);");
			blw.writeLine("  if (text != null)");
			blw.writeLine("    element.appendChild(document.createTextNode(text));");
			blw.writeLine("  return element;");
			blw.writeLine("}");
			blw.writeLine("function removeElement(node) {");
			blw.writeLine("  if (node.parentNode != null)");
			blw.writeLine("    node.parentNode.removeChild(node);");
			blw.writeLine("}");
			blw.writeLine("function setAttribute(node, name, value) {");
			blw.writeLine("  if (!node.setAttributeNode)");
			blw.writeLine("    return;");
			blw.writeLine("  var attribute = document.createAttribute(name);");
			blw.writeLine("  attribute.nodeValue = value;");
			blw.writeLine("  node.setAttributeNode(attribute);");
			blw.writeLine("}");
			blw.writeLine("function getOverlay(id, cssClass, add) {");
			blw.writeLine("  var overlay = newElement('div', id, cssClass, null);");
			blw.writeLine("  setAttribute(overlay, 'style', 'position: fixed; left: 0; top: 0; width: 100%; height: 100%;');");
			blw.writeLine("  if (add) {");
			blw.writeLine("    var body = getByName('body');");
			blw.writeLine("    if (body != null)");
			blw.writeLine("      body.appendChild(overlay);");
			blw.writeLine("  }");
			blw.writeLine("  return overlay;");
			blw.writeLine("}");


			
			blw.writeLine("var contextMenu;");
			blw.writeLine("var feedbackForm;");
			blw.writeLine("var annotationTypeColors = new Object();");
			blw.writeLine("function initAnnotEditor() {");

			// get main page parts
			blw.writeLine("  contextMenu = getById('contextMenuDiv');");
			blw.writeLine("  feedbackForm = getById('feedbackForm');");

			// get menu items for quicker access below
			blw.writeLine("  annotateMenuItems = new Array();");
			blw.writeLine("  for (var a = 0; getById('annotate' + a); a++)");
			blw.writeLine("    annotateMenuItems[annotateMenuItems.length] = getById('annotate' + a);");
			blw.writeLine("  annotateAllMenuItems = new Array();");
			blw.writeLine("  for (var a = 0; getById('annotateAll' + a); a++)");
			blw.writeLine("    annotateAllMenuItems[annotateAllMenuItems.length] = getById('annotateAll' + a);");
			
			blw.writeLine("  removeMenuItem = getById('remove');");
			blw.writeLine("  removeMenuItemText = getById('removeText');");
			blw.writeLine("  removeByValueMenuItem = getById('removeAllValue');");
			blw.writeLine("  removeByValueMenuItemText = getById('removeAllValueText');");
			blw.writeLine("  removeByTypeMenuItem = getById('removeAllType');");
			blw.writeLine("  removeByTypeMenuItemText = getById('removeAllTypeText');");
			blw.writeLine("  removeBySelectionMenuItem = getById('removeAllSelection');");
			
			blw.writeLine("  extendMenuItem = getById('extend');");
			blw.writeLine("  extendMenuItemText = getById('extendText');");
			blw.writeLine("  mergeMenuItem = getById('merge');");
			blw.writeLine("  mergeMenuItemText = getById('mergeText');");
			
			blw.writeLine("  splitBeforeMenuItem = getById('splitBefore');");
			blw.writeLine("  splitBeforeMenuItemText = getById('splitBeforeText');");
			blw.writeLine("  splitAroundMenuItem = getById('splitAround');");
			blw.writeLine("  splitAroundMenuItemText = getById('splitAroundText');");
			blw.writeLine("  splitAfterMenuItem = getById('splitAfter');");
			blw.writeLine("  splitAfterMenuItemText = getById('splitAfterText');");
			
			blw.writeLine("  cutUpToMenuItem = getById('cutUpTo');");
			blw.writeLine("  cutUpToMenuItemText = getById('cutUpToText');");
			blw.writeLine("  cutFromMenuItem = getById('cutFrom');");
			blw.writeLine("  cutFromMenuItemText = getById('cutFromText');");
			blw.writeLine("  cutBeforeMenuItem = getById('cutBefore');");
			blw.writeLine("  cutBeforeMenuItemText = getById('cutBeforeText');");
			blw.writeLine("  cutAfterMenuItem = getById('cutAfter');");
			blw.writeLine("  cutAfterMenuItemText = getById('cutAfterText');");
			
			// remove context menu altogether, will add it to glass pane later
			blw.writeLine("  removeElement(contextMenu);");
			
			// initialize annotation type colors
			String[] types = this.aefp.getDetailTypes();
			for (int t = 0; t < types.length; t++)
				blw.writeLine("  annotationTypeColors." + types[t] + " = '" + FeedbackPanel.getRGB(this.aefp.getDetailColor(types[t])) + "';");
			
			blw.writeLine("  for (var p = 0;; p++) {");
			blw.writeLine("    var t = 0;");
			blw.writeLine("    for (; getById('part' + p + '_token' + t); t++) {");
			blw.writeLine("      var token = getById('part' + p + '_token' + t);");
			blw.writeLine("      if (token)");
			blw.writeLine("        initTokenListeners(token, p, t);");
			blw.writeLine("    }");
			blw.writeLine("    if (t == 0)");
			blw.writeLine("      break;");
			blw.writeLine("    adjustColors(p);");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("function initTokenListeners(token, part, index) {");
			blw.writeLine("  token.onmousedown = function(event) {");
			blw.writeLine("    extendSelection(part, index);");
			blw.writeLine("  }");
			blw.writeLine("  token.onmouseover = function(event) {");
			blw.writeLine("    if (event.which != 0)");
			blw.writeLine("      extendSelection(part, index);");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("function adjustColors(part) {");
			blw.writeLine("  for (var t = 0; getById('part' + part + '_token' + t); t++) {");
			blw.writeLine("    var token = getById('part' + part + '_token' + t);");
			blw.writeLine("    var space = getById('part' + part + '_space' + t);");
			blw.writeLine("    if (token) {");
			blw.writeLine("      var state = getState(part, t);");
			blw.writeLine("      var type = getType(part, t);");
			blw.writeLine("      if (state && (state == 'S')) {");
			blw.writeLine("        if (type && annotationTypeColors[type]) {");
			blw.writeLine("          token.style.backgroundColor = annotationTypeColors[type];");
			blw.writeLine("          if (space)");
			blw.writeLine("            space.style.backgroundColor = 'FFFFFF';");
			blw.writeLine("        }");
			blw.writeLine("      }");
			blw.writeLine("      else if (state && (state == 'C')) {");
			blw.writeLine("        if (type && annotationTypeColors[type]) {");
			blw.writeLine("          token.style.backgroundColor = annotationTypeColors[type];");
			blw.writeLine("          if (space)");
			blw.writeLine("            space.style.backgroundColor = annotationTypeColors[type];");
			blw.writeLine("        }");
			blw.writeLine("      }");
			blw.writeLine("      else {");
			blw.writeLine("        token.style.backgroundColor = 'FFFFFF';");
			blw.writeLine("        if (space)");
			blw.writeLine("          space.style.backgroundColor = 'FFFFFF';");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("}");

			blw.writeLine("function annotate(type) {");
			blw.writeLine("  for (var t = contextMenu.start; t <= contextMenu.end; t++) {");
			blw.writeLine("    setState(contextMenu.part, t, ((t == contextMenu.start) ? 'S' : 'C'));");
			blw.writeLine("    setType(contextMenu.part, t, type);");
			blw.writeLine("  }");
			blw.writeLine("  var tokenAfter = getById('part' + contextMenu.part + '_token' + (contextMenu.end + 1));");
			blw.writeLine("  if (tokenAfter && (getState(contextMenu.part, (contextMenu.end + 1)) == 'C'))");
			blw.writeLine("    setState(contextMenu.part, (contextMenu.end + 1), 'S');");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("function annotateAll(type) {");
			blw.writeLine("  ");
			//  extract match token sequence
			blw.writeLine("  var values = new Array(contextMenu.end - contextMenu.start + 1);");
			blw.writeLine("  for (var v = 0; v < values.length; v++)");
			blw.writeLine("    values[v] = getTokenValue(contextMenu.part, (contextMenu.start + v));");
			blw.writeLine("  ");
			//  annotate all matches
			blw.writeLine("  for (var s = 0; getById('part' + contextMenu.part + '_token' + (s + values.length - 1)); s++) {");
			blw.writeLine("    if (!tokenSequenceMatch(s, values, true, false))");
			blw.writeLine("      continue;");
			blw.writeLine("    ");
			//  annotate
			blw.writeLine("    for (var t = 0; t < values.length; t++) {");
			blw.writeLine("      setState(contextMenu.part, (s + t), ((t == 0) ? 'S' : 'C'));");
			blw.writeLine("      setType(contextMenu.part, (s + t), type);");
			blw.writeLine("    }");
			blw.writeLine("    ");
			//  fix overwritten start of annotation (if any)
			blw.writeLine("    if (getById('part' + contextMenu.part + '_token' + (s + values.length)) && (getState(contextMenu.part, (s + values.length)) == 'C'))");
			blw.writeLine("      setState(contextMenu.part, (s + values.length), 'S');");
			blw.writeLine("    ");
			//  jump to last token of added annotation (loop increment will move forward an additional token)
			blw.writeLine("    s += (values.length - 1);");
			blw.writeLine("  }");
			blw.writeLine("  ");
			//  make changes visible
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;");
			blw.writeLine("}");

			blw.writeLine("function removeSelection() {");
			blw.writeLine("  var start = contextMenu.start;");
			blw.writeLine("  while (getById('part' + contextMenu.part + '_token' + start) && (getState(contextMenu.part, start) == 'C'))");
			blw.writeLine("    start--;");
			blw.writeLine("  var end = contextMenu.end;");
			blw.writeLine("  while (getById('part' + contextMenu.part + '_token' + (end + 1)) && (getState(contextMenu.part, (end + 1)) == 'C'))");
			blw.writeLine("    end++;");
			blw.writeLine("  for (var t = start; t <= end; t++) {");
			blw.writeLine("    setState(contextMenu.part, t, 'O');");
			blw.writeLine("    setType(contextMenu.part, t, null);");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("function removeAllValue() {");
			blw.writeLine("  ");
			//  extract match token sequence
			blw.writeLine("  var values = new Array(contextMenu.end - contextMenu.start + 1);");
			blw.writeLine("  for (var v = 0; v < values.length; v++)");
			blw.writeLine("    values[v] = getTokenValue(contextMenu.part, (contextMenu.start + v));");
			blw.writeLine("  ");
			//  de-annotate all matches
			blw.writeLine("  for (var s = 0; getById('part' + contextMenu.part + '_token' + (s + values.length - 1)); s++) {");
			blw.writeLine("    if (!tokenSequenceMatch(s, values, true, false))");
			blw.writeLine("      continue;");
			blw.writeLine("    if (contextMenu.annotType == getType(contextMenu.part, s))");
			blw.writeLine("      continue;");
			blw.writeLine("    if (getById('part' + contextMenu.part + '_token' + (s + values.length)) && (getState(contextMenu.part, (s + values.length)) == 'C'))");
			blw.writeLine("      continue;");
			blw.writeLine("    ");
			//  de-annotate
			blw.writeLine("    for (var t = 0; t < values.length; t++) {");
			blw.writeLine("      setState(contextMenu.part, (s + t), 'O');");
			blw.writeLine("      setType(contextMenu.part, (s + t), null);");
			blw.writeLine("    }");
			blw.writeLine("    ");
			//  fix overwritten start of annotation (if any)
			blw.writeLine("    if (getById('part' + contextMenu.part + '_token' + (s + values.length)) && (getState(contextMenu.part, (s + values.length)) == 'C'))");
			blw.writeLine("      setState(contextMenu.part, (s + values.length),  'S');");
			blw.writeLine("    ");
			//  jump to last token of added annotation (loop increment will move forward an additional token)
			blw.writeLine("    s += (values.length - 1);");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("function removeAllType() {");
			blw.writeLine("  for (var t = 0; getById('part' + contextMenu.part + '_token' + t); t++) {");
			blw.writeLine("    if (getType(contextMenu.part, t) != contextMenu.annotType)");
			blw.writeLine("      continue;");
			blw.writeLine("    if (getState(contextMenu.part, t) == 'O')");
			blw.writeLine("      continue;");
			blw.writeLine("    setState(contextMenu.part, t, 'O');");
			blw.writeLine("    setType(contextMenu.part, t, null);");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("function removeAllSelection() {");
			blw.writeLine("  var start = findStart(contextMenu.part, contextMenu.start);");
			blw.writeLine("  var end = findEnd(contextMenu.part, contextMenu.end);");
			blw.writeLine("  for (var t = start; t <= end; t++) {");
			blw.writeLine("    setState(contextMenu.part, t, 'O');");
			blw.writeLine("    setType(contextMenu.part, t, null);");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("function findStart(part, start) {");
			blw.writeLine("  while (getState(part, start) == 'O')");
			blw.writeLine("    start++;");
			blw.writeLine("  while (getState(part, start) != 'S')");
			blw.writeLine("    start--;");
			blw.writeLine("  return start;");
			blw.writeLine("}");
			blw.writeLine("function findEnd(part, end) {");
			blw.writeLine("  while (getState(part, end) == 'O')");
			blw.writeLine("    end--;");
			blw.writeLine("  while (getById('part' + part + '_token' + (end + 1)) && ('C' == getState(part, (end + 1))))");
			blw.writeLine("    end++;");
			blw.writeLine("  return end;");
			blw.writeLine("}");

			blw.writeLine("function merge() {");
			blw.writeLine("  var start = contextMenu.start;");
			blw.writeLine("  while (getById('part' + contextMenu.part + '_token' + start) && (getState(contextMenu.part, start) == 'O'))");
			blw.writeLine("    start++;");
			blw.writeLine("  while (getById('part' + contextMenu.part + '_token' + start) && (getState(contextMenu.part, start) == 'C'))");
			blw.writeLine("    start--;");
			blw.writeLine("  var end = contextMenu.end;");
			blw.writeLine("  while (getById('part' + contextMenu.part + '_token' + (end + 1)) && (getState(contextMenu.part, (end + 1)) == 'O'))");
			blw.writeLine("    end--;");
			blw.writeLine("  while (getById('part' + contextMenu.part + '_token' + (end + 1)) && (getState(contextMenu.part, (end + 1)) == 'C'))");
			blw.writeLine("    end++;");
			blw.writeLine("  for (var t = start; t <= end; t++) {");
			blw.writeLine("    setState(contextMenu.part, t, ((t == start) ? 'S' : 'C'));");
			blw.writeLine("    setType(contextMenu.part, t, contextMenu.annotType);");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("function extend() {");
			blw.writeLine("  for (var t = contextMenu.start; t <= contextMenu.end; t++) {");
			blw.writeLine("    setState(contextMenu.part, t, (((t == contextMenu.start) && (getState(contextMenu.part, t) != 'C')) ? 'S' : 'C'));");
			blw.writeLine("    setType(contextMenu.part, t, contextMenu.annotType);");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;");
			blw.writeLine("}");

			blw.writeLine("function splitAround() {");
			blw.writeLine("  setState(contextMenu.part, contextMenu.start, 'O');");
			blw.writeLine("  setType(contextMenu.part, contextMenu.start, null);");
			blw.writeLine("  var nextToken = getById('part' + contextMenu.part + '_token' + (contextMenu.start + 1));");
			blw.writeLine("  if (nextToken && (getState(contextMenu.part, (contextMenu.start + 1)) == 'C'))");
			blw.writeLine("    setState(contextMenu.part, (contextMenu.start + 1), 'S');");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;");
			blw.writeLine("}");

			blw.writeLine("function splitBefore() {");
			blw.writeLine("  setState(contextMenu.part, contextMenu.start, 'S');");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;");
			blw.writeLine("}");

			blw.writeLine("function splitAfter() {");
			blw.writeLine("  var nextToken = getById('part' + contextMenu.part + '_token' + (contextMenu.start + 1));");
			blw.writeLine("  if (nextToken && (getState(contextMenu.part, (contextMenu.start + 1)) == 'C'))");
			blw.writeLine("    setState(contextMenu.part, (contextMenu.start + 1), 'S');");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;  ");
			blw.writeLine("}");

			blw.writeLine("function cutUpTo() {");
			blw.writeLine("  var nextToken = getById('part' + contextMenu.part + '_token' + (contextMenu.start + 1));");
			blw.writeLine("  if (nextToken)");
			blw.writeLine("    setState(contextMenu.part, (contextMenu.start + 1), 'S');");
			blw.writeLine("  for (var t = contextMenu.start; getById('part' + contextMenu.part + '_token' + t); t--) {");
			blw.writeLine("    var oldState = getState(contextMenu.part, t);");
			blw.writeLine("    setState(contextMenu.part, t, 'O');");
			blw.writeLine("    setType(contextMenu.part, t, null);");
			blw.writeLine("    if (oldState == 'S')");
			blw.writeLine("      break;");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;  ");
			blw.writeLine("}");

			blw.writeLine("function cutFrom() {");
			blw.writeLine("  for (var t = contextMenu.start; getById('part' + contextMenu.part + '_token' + t); t++) {");
			blw.writeLine("    var oldState = getState(contextMenu.part, t);");
			blw.writeLine("    setState(contextMenu.part, t, 'O');");
			blw.writeLine("    setType(contextMenu.part, t, null);");
			blw.writeLine("    if (oldState != 'C')");
			blw.writeLine("      break;");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;  ");
			blw.writeLine("}");

			blw.writeLine("function cutBefore() {");
			blw.writeLine("  setState(contextMenu.part, contextMenu.start, 'S');");
			blw.writeLine("  for (var t = (contextMenu.start - 1); getById('part' + contextMenu.part + '_token' + t); t--) {");
			blw.writeLine("    var oldState = getState(contextMenu.part, t);");
			blw.writeLine("    setState(contextMenu.part, t, 'O');");
			blw.writeLine("    setType(contextMenu.part, t, null);");
			blw.writeLine("    if (oldState == 'S')");
			blw.writeLine("      break;");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;  ");
			blw.writeLine("}");
			blw.writeLine("function cutAfter() {");
			blw.writeLine("  for (var t = (contextMenu.start + 1); getById('part' + contextMenu.part + '_token' + t); t++) {");
			blw.writeLine("    var oldState = getState(contextMenu.part, t);");
			blw.writeLine("    setState(contextMenu.part, t, 'O');");
			blw.writeLine("    setType(contextMenu.part, t, null);");
			blw.writeLine("    if (oldState != 'C')");
			blw.writeLine("      break;");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors(contextMenu.part);");
			blw.writeLine("  return false;  ");
			blw.writeLine("}");

			blw.writeLine("function getTokenValue(part, index) {");
			blw.writeLine("  return getById('part' + part + '_token' + index).innerHTML;");
			blw.writeLine("}");

			blw.writeLine("function getState(part, index) {");
			blw.writeLine("  return feedbackForm[('part' + part + '_token' + index + '_state')].value;");
			blw.writeLine("}");

			blw.writeLine("function setState(part, index, state) {");
			blw.writeLine("  feedbackForm[('part' + part + '_token' + index + '_state')].value = state;");
			blw.writeLine("}");

			blw.writeLine("function getType(part, index) {");
			blw.writeLine("  return feedbackForm[('part' + part + '_token' + index + '_type')].value;");
			blw.writeLine("}");

			blw.writeLine("function setType(part, index, type) {");
			blw.writeLine("  feedbackForm[('part' + part + '_token' + index + '_type')].value = ((type == null) ? '' : type);");
			blw.writeLine("}");
			blw.writeLine("var submitMode;");
			blw.writeLine("function getSubmitMode() {");
			blw.writeLine("  return submitMode;");
			blw.writeLine("}");
			blw.writeLine("function setSubmitMode(mode) {");
			blw.writeLine("  submitMode = mode;");
			blw.writeLine("  document.getElementById('submitMode_field').value = mode;");
			blw.writeLine("}");

			blw.writeLine("var selectionStartPart = -1;");
			blw.writeLine("var selectionStartIndex = -1;");
			blw.writeLine("var selectionEndPart = -1;");
			blw.writeLine("var selectionEndIndex = -1;");
			blw.writeLine("function extendSelection(part, index) {");
			blw.writeLine("  if (selectionStartPart == -1)");
			blw.writeLine("    selectionStartPart = part;");
			blw.writeLine("  if (selectionStartIndex == -1)");
			blw.writeLine("    selectionStartIndex = index;");
			blw.writeLine("  selectionEndPart = part;");
			blw.writeLine("  selectionEndIndex = index;");
			blw.writeLine("}");
			blw.writeLine("function clearSelection() {");
			blw.writeLine("  selectionStartPart = -1;");
			blw.writeLine("  selectionStartIndex = -1;");
			blw.writeLine("  selectionEndPart = -1;");
			blw.writeLine("  selectionEndIndex = -1;");
			blw.writeLine("  var selection = null;");
			blw.writeLine("  if (window.getSelection)");
			blw.writeLine("    selection = window.getSelection();");
			blw.writeLine("  else if (document.selection)");
			blw.writeLine("    selection = document.selection;");
			blw.writeLine("  if (selection == null) {}");
			blw.writeLine("  else if (selection.removeAllRanges)");
			blw.writeLine("    selection.removeAllRanges();");
			blw.writeLine("  else if (selection.empty)");
			blw.writeLine("    selection.empty();");
			blw.writeLine("}");

			blw.writeLine("var annotateMenuItems = new Array();");
			blw.writeLine("var annotateAllMenuItems = new Array();");

			blw.writeLine("var removeMenuItem = null;");
			blw.writeLine("var removeMenuItemText = null;");
			blw.writeLine("var removeByValueMenuItem = null;");
			blw.writeLine("var removeByValueMenuItemText = null;");
			blw.writeLine("var removeByTypeMenuItem = null;");
			blw.writeLine("var removeByTypeMenuItemText = null;");
			blw.writeLine("var removeBySelectionMenuItem = null;");

			blw.writeLine("var extendMenuItem = null;");
			blw.writeLine("var extendMenuItemText = null;");
			blw.writeLine("var mergeMenuItem = null;");
			blw.writeLine("var mergeMenuItemText = null;");

			blw.writeLine("var splitBeforeMenuItem = null;");
			blw.writeLine("var splitBeforeMenuItemText = null;");
			blw.writeLine("var splitAroundMenuItem = null;");
			blw.writeLine("var splitAroundMenuItemText = null;");
			blw.writeLine("var splitAfterMenuItem = null;");
			blw.writeLine("var splitAfterMenuItemText = null;");

			blw.writeLine("var cutUpToMenuItem = null;");
			blw.writeLine("var cutUpToMenuItemText = null;");
			blw.writeLine("var cutFromMenuItem = null;");
			blw.writeLine("var cutFromMenuItemText = null;");
			blw.writeLine("var cutBeforeMenuItem = null;");
			blw.writeLine("var cutBeforeMenuItemText = null;");
			blw.writeLine("var cutAfterMenuItem = null;");
			blw.writeLine("var cutAfterMenuItemText = null;");

			blw.writeLine("function getSelectedText(selPart, selStart, selEnd) {");
			blw.writeLine("  var selText = '';");
			blw.writeLine("  for (var t = selStart; t <= selEnd; t++) {");
			blw.writeLine("    if (selStart < t)");
			blw.writeLine("      selText = (selText + getById('part' + selPart + '_space' + t).innerHTML);");
			blw.writeLine("    var token = getById('part' + selPart + '_token' + t);");
			blw.writeLine("    if (token)");
			blw.writeLine("      selText = (selText + token.innerHTML);");
			blw.writeLine("  }");
			blw.writeLine("  return selText;");
			blw.writeLine("}");
			blw.writeLine("function getSelectedAnnotType(selPart, selStart, selEnd) {");
			blw.writeLine("  var selType = null;");
			blw.writeLine("  for (var t = selStart; t <= selEnd; t++) {");
			blw.writeLine("    var type = getType(selPart, t);");
			blw.writeLine("    if (type == null) {}");
			blw.writeLine("    else if (type == '') {}");
			blw.writeLine("    else if (selType == null)");
			blw.writeLine("      selType = type;");
			blw.writeLine("    else if (selType != type)");
			blw.writeLine("      return null;");
			blw.writeLine("  }");
			blw.writeLine("  return selType;");
			blw.writeLine("}");
			blw.writeLine("function adjustContextMenu(selPart, selStart, selEnd) {");
			blw.writeLine("  ");
			//  get selection and annto type
			blw.writeLine("  var selText = getSelectedText(selectionStartPart, selStart, selEnd);");
			blw.writeLine("  var selAnnotType = getSelectedAnnotType(selectionStartPart, selStart, selEnd);");
			blw.writeLine("  ");
			//  tray up selected tokens
			blw.writeLine("  var values = new Array(selEnd - selStart + 1);");
			blw.writeLine("  for (var v = 0; v < values.length; v++)");
			blw.writeLine("    values[v] = getTokenValue(selPart, (selStart + v));");
			blw.writeLine("  ");
			//  analyze annotation status of selection
			blw.writeLine("  var firstS = -1;");
			blw.writeLine("  var firstC = -1;");
			blw.writeLine("  var firstO = -1;");

			blw.writeLine("  var selAnnotCount = 0;");
			blw.writeLine("  for (var t = selStart; t <= selEnd; t++) {");
			blw.writeLine("    var state = getState(selPart, t);");
			blw.writeLine("    if (state == 'S') {");
			blw.writeLine("      selAnnotCount++;");
			blw.writeLine("      if (firstS == -1)");
			blw.writeLine("        firstS = t;");
			blw.writeLine("    }");
			blw.writeLine("    else if (state == 'C') {");
			blw.writeLine("      if (firstC == -1) {");
			blw.writeLine("        firstC = t;");
			blw.writeLine("        if (firstS == -1)");
			blw.writeLine("          selAnnotCount++;");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    else if (state == 'O') {");
			blw.writeLine("      if (firstO == -1)");
			blw.writeLine("        firstO = t;");
			blw.writeLine("    }");
			blw.writeLine("  }");

			blw.writeLine("  var sameValueCount = 0;");
			blw.writeLine("  if (selAnnotCount == 0) {");
			blw.writeLine("    for (var s = 0; getById('part' + selPart + '_token' + s); s++) {");
			blw.writeLine("      if (tokenSequenceMatch(selPart, s, values, true, false))");
			blw.writeLine("        sameValueCount++;");
			blw.writeLine("    }");
			blw.writeLine("  }");

			blw.writeLine("  var sameTypeCount = 0;");
			blw.writeLine("  if (selAnnotType != null) {");
			blw.writeLine("    for (var s = 0; getById('part' + selPart + '_token' + s); s++) {");
			blw.writeLine("      if (('S' == getState(selPart, s)) && (selAnnotType == getType(selPart, s)))");
			blw.writeLine("        sameTypeCount++;");
			blw.writeLine("    }");
			blw.writeLine("    for (var s = 0; getById('part' + selPart + '_token' + s); s++) {");
			blw.writeLine("      if (tokenSequenceMatch(selPart, s, values, true, true) && (selAnnotType == getType(selPart, s)) && (!getById('part' + selPart + '_token' + (s + values.length)) || ('C' != getState(selPart, (s + values.length)))))");
			blw.writeLine("        sameValueCount++;");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  console.log('firstS = ' + firstS + ', firstC = ' + firstC + ', firstO = ' + firstO + ', selAnnotCount = ' + selAnnotCount + ', selAnnotType = ' + selAnnotType);");

			//  annotate functions
			blw.writeLine("  for (var a = 0; a < annotateMenuItems.length; a++)");
			blw.writeLine("    annotateMenuItems[a].style.display = ((selAnnotCount == 0) ? null : 'none');");
			blw.writeLine("  for (var a = 0; a < annotateAllMenuItems.length; a++)");
			blw.writeLine("    annotateAllMenuItems[a].style.display = (((selAnnotCount == 0) && (sameValueCount > 1)) ? null : 'none');");

			//  remove functions
			blw.writeLine("  removeMenuItemText.innerHTML = ('Remove \\'' + selAnnotType + '\\' Annotation');");
			blw.writeLine("  removeMenuItem.style.display = ((selAnnotCount == 1) ? '' : 'none');");
			blw.writeLine("  if (sameValueCount > 1) {");
			blw.writeLine("    removeByValueMenuItemText.innerHTML = ('Remove all Annotations of \\'' + selText + '\\'');");
			blw.writeLine("    removeByValueMenuItem.style.display = null;");
			blw.writeLine("  }");
			blw.writeLine("  else removeByValueMenuItem.style.display = 'none';");
			blw.writeLine("  if (sameTypeCount > 1) {");
			blw.writeLine("    removeByTypeMenuItemText.innerHTML = ('Remove all \\'' + selAnnotType + '\\' Annotations');");
			blw.writeLine("    removeByTypeMenuItem.style.display = null;");
			blw.writeLine("  }");
			blw.writeLine("  else removeByTypeMenuItem.style.display = 'none';");
			blw.writeLine("  removeBySelectionMenuItem.style.display = ((selAnnotCount > 1) ? '' : 'none');");

			//  extend function
			blw.writeLine("  if ((selAnnotCount == 1) && (firstO != -1)) {");
			blw.writeLine("    extendMenuItemText.innerHTML = ('Extend \\'' + selAnnotType + '\\' Annotation');");
			blw.writeLine("    extendMenuItem.style.display = null;");
			blw.writeLine("  }");
			blw.writeLine("  else extendMenuItem.style.display = 'none';");

			//  merge function
			blw.writeLine("  if ((selAnnotType != null) && (selAnnotCount > 1)) {");
			blw.writeLine("    mergeMenuItemText.innerHTML = ('Merge \\'' + selAnnotType + '\\' Annotations');");
			blw.writeLine("    mergeMenuItem.style.display = null;");
			blw.writeLine("  }");
			blw.writeLine("  else mergeMenuItem.style.display = 'none';");

			//  splitting and cutting functions
			blw.writeLine("  if ((selStart == selEnd) && (selAnnotType != null)) {");
			blw.writeLine("    var token = getById('part' + selPart + '_token' + selStart);");
			blw.writeLine("    var state = getState(selPart, selStart);");
			blw.writeLine("    var nextToken = getById('part' + selPart + '_token' + (selStart + 1));");
			blw.writeLine("    var nextState = (nextToken ? getState(selPart, (selStart + 1)) : null);");

			blw.writeLine("    splitBeforeMenuItemText.innerHTML = ('Split \\'' + selAnnotType + '\\' Annotation before \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    splitAroundMenuItemText.innerHTML = ('Split \\'' + selAnnotType + '\\' Annotation around \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    splitAfterMenuItemText.innerHTML = ('Split \\'' + selAnnotType + '\\' Annotation after \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    splitBeforeMenuItem.style.display = ((state == 'C') ? '' : 'none');");
			blw.writeLine("    splitAroundMenuItem.style.display = (((state == 'C') && nextToken && (nextState == 'C')) ? null : 'none');");
			blw.writeLine("    splitAfterMenuItem.style.display = ((nextToken && (nextState == 'C')) ? null : 'none');");

			blw.writeLine("    cutUpToMenuItemText.innerHTML = ('Cut \\'' + selAnnotType + '\\' Annotation up to \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    cutFromMenuItemText.innerHTML = ('Cut \\'' + selAnnotType + '\\' Annotation from \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    cutUpToMenuItem.style.display = ((nextToken && (nextState == 'C')) ? null : 'none');");
			blw.writeLine("    cutFromMenuItem.style.display = ((state == 'C') ? null : 'none');");

			blw.writeLine("    cutBeforeMenuItemText.innerHTML = ('Cut \\'' + selAnnotType + '\\' Annotation before \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    cutAfterMenuItemText.innerHTML = ('Cut \\'' + selAnnotType + '\\' Annotation after \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    cutBeforeMenuItem.style.display = ((state == 'C') ? null : 'none');");
			blw.writeLine("    cutAfterMenuItem.style.display = ((nextToken && (nextState == 'C')) ? null : 'none');");
			blw.writeLine("  }");
			blw.writeLine("  else {");
			blw.writeLine("    splitBeforeMenuItem.style.display = 'none';");
			blw.writeLine("    splitAroundMenuItem.style.display = 'none';");
			blw.writeLine("    splitAfterMenuItem.style.display = 'none';");
			blw.writeLine("    cutUpToMenuItem.style.display = 'none';");
			blw.writeLine("    cutFromMenuItem.style.display = 'none';");
			blw.writeLine("    cutBeforeMenuItem.style.display = 'none';");
			blw.writeLine("    cutAfterMenuItem.style.display = 'none';");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("function tokenSequenceMatch(part, start, values, checkStates, mustBeAnnotated) {");
			blw.writeLine("  for (var v = 0; v < values.length; v++) {");
			blw.writeLine("    if (!getById('part' + part + '_token' + (start + v)))");
			blw.writeLine("      return false;");
			blw.writeLine("    if (getTokenValue(part, (start + v)) != values[v])");
			blw.writeLine("      return false;");
			blw.writeLine("    if (checkStates && (mustBeAnnotated ? (getState(part, (start + v)) != ((v == 0) ? 'S' : 'C')) : getState(part, (start + v)) != 'O'))");
			blw.writeLine("      return false;");
			blw.writeLine("  }");
			blw.writeLine("  return true;");
			blw.writeLine("}");

			blw.writeLine("function showContextMenu(event, part) {");
			blw.writeLine("  ");
			//  test if we have a selection
			blw.writeLine("  if (selectionStartPart == -1)");
			blw.writeLine("    return;");
			blw.writeLine("  if (selectionStartPart != selectionEndPart)");
			blw.writeLine("    return;");
			blw.writeLine("  if (selectionStartIndex == -1)");
			blw.writeLine("    return;");
			blw.writeLine("  ");
			//  order selection end points
			blw.writeLine("  var selStart = selectionStartIndex;");
			blw.writeLine("  var selEnd = selectionEndIndex;");
			blw.writeLine("  if (selStart > selEnd) {");
			blw.writeLine("    var selTemp = selStart;");
			blw.writeLine("    selStart = selEnd;");
			blw.writeLine("    selEnd = selTemp;");
			blw.writeLine("  }");
			blw.writeLine("  console.log('showing context menu, new way !!!');");
			blw.writeLine("  console.log('part is ' + selectionStartPart + ', token(s) ' + selStart + ' to ' + selEnd);");
			blw.writeLine("  ");
			//  get selection and annto type
			blw.writeLine("  var selText = getSelectedText(selectionStartPart, selStart, selEnd);");
			blw.writeLine("  var selAnnotType = getSelectedAnnotType(selectionStartPart, selStart, selEnd);");
			blw.writeLine("  console.log('selected text is ' + selText + ', annot type is ' + selAnnotType);");
			blw.writeLine("  ");
			//  adjust or assemble context menu depending on selection (offer functions as above)
			blw.writeLine("  adjustContextMenu(selectionStartPart, selStart, selEnd);");
			blw.writeLine("  ");
			//  add glass pane !!!
			blw.writeLine("  var glassPane = getOverlay('contextMenuGlassPane', 'menuGlassPane', true);");
			blw.writeLine("  glassPane.onclick = function(event) {");
			blw.writeLine("    event.stopPropagation();");
			blw.writeLine("    clearSelection();");
			blw.writeLine("    removeElement(glassPane);");
			blw.writeLine("  }");
			blw.writeLine("  glassPane.oncontextmenu = function() {");
			blw.writeLine("    return false;");
			blw.writeLine("  };");
			blw.writeLine("  ");
			//  store variables
			blw.writeLine("  contextMenu.part = part;");
			blw.writeLine("  contextMenu.start = selStart;");
			blw.writeLine("  contextMenu.end = selEnd;");
			blw.writeLine("  contextMenu.annotType = selAnnotType;");
			blw.writeLine("  ");
			//  add context menu to glass pane, outside window initially
			blw.writeLine("  contextMenu.style.display = null;");
			blw.writeLine("  contextMenu.style.left = (-10000 + 'px');");
			blw.writeLine("  contextMenu.style.top = (-10000 + 'px');");
			blw.writeLine("  glassPane.appendChild(contextMenu);");
			blw.writeLine("  ");
			//  we can measure only now
			blw.writeLine("  var contextMenuLeft = event.clientX;");
			blw.writeLine("  if ((contextMenuLeft + contextMenu.offsetWidth) > window.innerWidth)");
			blw.writeLine("    contextMenuLeft = Math.max(0, (window.innerWidth - contextMenu.offsetWidth));");
			blw.writeLine("  var contextMenuTop = event.clientY;");
			blw.writeLine("  if ((contextMenuTop + contextMenu.offsetHeight) > window.innerHeight)");
			blw.writeLine("    contextMenuTop = Math.max(0, (window.innerHeight - contextMenu.offsetHeight));");
			blw.writeLine("  console.log('click Y is ' + event.clientY + ', menu height is ' + contextMenu.offsetHeight + ', window height is ' + window.innerHeight);");
			blw.writeLine("  contextMenu.style.left = (contextMenuLeft + 'px');");
			blw.writeLine("  contextMenu.style.top = (contextMenuTop + 'px');");
			blw.writeLine("}");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writeCssStyles(java.io.Writer)
		 */
		public void writeCssStyles(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine(".menu {");
			blw.writeLine("  margin: 0;");
			blw.writeLine("  padding: 0.3em;");
			blw.writeLine("  list-style-type: none;");
			blw.writeLine("  background-color: ddd;");
			blw.writeLine("}");
			
			blw.writeLine(".menu li:hover {}");
			
			blw.writeLine(".menu hr {");
			blw.writeLine("  border: 0;");
			blw.writeLine("  border-bottom: 1px solid grey;");
			blw.writeLine("  margin: 3px 0px 3px 0px;");
			blw.writeLine("  width: 10em;");
			blw.writeLine("}");
			
			blw.writeLine(".menu a {");
			blw.writeLine("  border: 0 !important;");
			blw.writeLine("  text-decoration: none;");
			blw.writeLine("}");
			
			blw.writeLine(".menu a:hover {");
			blw.writeLine("  text-decoration: underline !important;");
			blw.writeLine("}");
			
			blw.writeLine(".part {");
			blw.writeLine("  width: 70%;");
			blw.writeLine("  margin-top: 10px;");
			blw.writeLine("  border-width: 2px;");
			blw.writeLine("  border-color: 666666;");
			blw.writeLine("  border-style: solid;");
			blw.writeLine("  border-collapse: collapse;");
			blw.writeLine("}");
			
			blw.writeLine(".part td {");
			blw.writeLine("  padding: 10px;");
			blw.writeLine("  margin: 0px;");
			blw.writeLine("}");
			
			blw.writeLine(".menuItem {"); // TODO make this adjustable
			blw.writeLine("  font-family: Arial,Helvetica,Verdana;");
			blw.writeLine("  font-size: 10pt;");
			blw.writeLine("  padding: 2pt 5pt 2pt;");
			blw.writeLine("  text-align: left;");
			blw.writeLine("}");
			
			blw.writeLine(".menuItemText {");
			blw.writeLine("  display: inline-block;");
			blw.writeLine("  width: 100%;");
			blw.writeLine("  cursor: default;");
			blw.writeLine("}");
			
			blw.writeLine(".token {");
			blw.writeLine("  font-family: " + this.aefp.getFontName() + ";");
			blw.writeLine("  font-size: " + this.aefp.getFontSize() + "pt;");
			blw.writeLine("}");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writePanelBody(java.io.Writer)
		 */
		public void writePanelBody(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			String[] types = this.aefp.getDetailTypes();
			
			if (this.aefp.isShowingDetailTypeLegend()) {
				blw.writeLine("<table class=\"part\">");
				for (int r = 0; r < types.length; r+=4) {
					blw.writeLine("<tr>");
					for (int c = 0; c < 4; c++) {
						blw.write("<td style=\"cursor: default;\">");
						if ((r + c) < types.length) {
							blw.write("<span style=\"width: 12px; height: 12 px; background-color: " + FeedbackPanel.getRGB(this.aefp.getDetailColor(types[r+c])) + ";\">&nbsp;&nbsp;&nbsp;&nbsp;</span>&nbsp;");
							blw.write(types[r+c]);
							//	TODO add click listener
						}
						blw.writeLine("</td>");
					}
					blw.writeLine("</tr>");
				}
				blw.writeLine("</table>");
			}
			
			blw.writeLine("<div id=\"contextMenuDiv\" style=\"border: 1px solid blue; display: none; position: absolute\">");
			blw.writeLine("<ul class=\"menu\">");
			
			for (int t = 0; t < types.length; t++) {
				blw.writeLine("<li id=\"annotate" + t + "\" class=\"menuItem\"><span class=\"menuItemText\" onclick=\"return annotate('" + types[t] + "');\">Annotate as '" + types[t] + "'</span></li>");
				blw.writeLine("<li id=\"annotateAll" + t + "\" class=\"menuItem\"><span class=\"menuItemText\" onclick=\"return annotateAll('" + types[t] + "');\">Annotate all as '" + types[t] + "'</span></li>");
			}
			
			blw.writeLine("<li id=\"remove\" class=\"menuItem\"><span class=\"menuItemText\" id=\"removeText\" onclick=\"return removeSelection();\">Remove Annotation</span></li>");
			blw.writeLine("<li id=\"removeAllValue\" class=\"menuItem\"><span class=\"menuItemText\" id=\"removeAllValueText\" onclick=\"return removeAllValue();\">Remove all Annotations by Value</span></li>");
			blw.writeLine("<li id=\"removeAllType\" class=\"menuItem\"><span class=\"menuItemText\" id=\"removeAllTypeText\" onclick=\"return removeAllType();\">Remove all Annotations by Type</span></li>");
			blw.writeLine("<li id=\"removeAllSelection\" class=\"menuItem\"><span class=\"menuItemText\" onclick=\"return removeAllSelection();\">Remove all Selected Annotations</span></li>");
			
			blw.writeLine("<li id=\"extend\" class=\"menuItem\"><span class=\"menuItemText\" id=\"extendText\" onclick=\"return extend();\">Extend Annotation</span></li>");
			blw.writeLine("<li id=\"merge\" class=\"menuItem\"><span class=\"menuItemText\" id=\"mergeText\" onclick=\"return merge();\">Merge Annotations</span></li>");
			
			blw.writeLine("<li id=\"splitBefore\" class=\"menuItem\"><span class=\"menuItemText\" id=\"splitBeforeText\" onclick=\"return splitBefore();\">Split Annotation Before</span></li>");
			blw.writeLine("<li id=\"splitAround\" class=\"menuItem\"><span class=\"menuItemText\" id=\"splitAroundText\" onclick=\"return splitAround();\">Split Annotation Around</span></li>");
			blw.writeLine("<li id=\"splitAfter\" class=\"menuItem\"><span class=\"menuItemText\" id=\"splitAfterText\" onclick=\"return splitAfter();\">Split Annotation After</span></li>");
			
			blw.writeLine("<li id=\"cutUpTo\" class=\"menuItem\"><span class=\"menuItemText\" id=\"cutUpToText\" onclick=\"return cutUpTo();\">Cut Annotation Up To</span></li>");
			blw.writeLine("<li id=\"cutFrom\" class=\"menuItem\"><span class=\"menuItemText\" id=\"cutFromText\" onclick=\"return cutFrom();\">Cut Annotation From</span></li>");
			blw.writeLine("<li id=\"cutBefore\" class=\"menuItem\"><span class=\"menuItemText\" id=\"cutBeforeText\" onclick=\"return cutBefore();\">Cut Annotation Before</span></li>");
			blw.writeLine("<li id=\"cutAfter\" class=\"menuItem\"><span class=\"menuItemText\" id=\"cutAfterText\" onclick=\"return cutAfter();\">Cut Annotation After</span></li>");
			
			blw.writeLine("</ul>");
			blw.writeLine("</div>");
			blw.writeLine("");
			
			for (int a = 0; a < this.aeTokenStates.length; a++) {
				blw.writeLine("<table class=\"part\" id=\"part" + a + "\">");
				blw.writeLine("<tr>");
				blw.writeLine("<td onmouseup=\"showContextMenu(event, " + a + ");\" oncontextmenu=\"return false;\">");
				
				for (int t = 0; t < this.aeTokenStates[a].getTokenCount(); t++) {
					if (t != 0) {
						blw.write("<span" +
								" class=\"token\"" +
								" id=\"part" + a + "_space" + t + "\"" +
								">" +
								this.aeTokenStates[a].getWhitespaceBefore(t) +
								"</span>");
					}
					blw.write("<span" +
							" class=\"token\"" +
							" id=\"part" + a + "_token" + t + "\"" +
							">" +
							prepareForHtml(this.aeTokenStates[a].getValueAt(t)) +
							"</span>");
				}
				blw.newLine();
				
				blw.writeLine("</td>");
				blw.writeLine("</tr>");
				blw.writeLine("</table>");
				blw.writeLine("");
				
				for (int t = 0; t < this.aeTokenStates[a].getTokenCount(); t++) {
					String state = this.aeTokenStates[a].getStateAt(t);
					String type = this.aeTokenStates[a].getTypeAt(t);
					if (AnnotationEditorFeedbackPanel.START.equals(state))
						state = "S";
					else if (AnnotationEditorFeedbackPanel.CONTINUE.equals(state))
						state = "C";
					else if (AnnotationEditorFeedbackPanel.OTHER.equals(state)) {
						state = "O";
						type = "";
					}
					blw.writeLine("<input type=\"hidden\" name=\"part" + a + "_token" + t + "_state\" value=\"" + state + "\" />");
					blw.writeLine("<input type=\"hidden\" name=\"part" + a + "_token" + t + "_type\" value=\"" + type + "\" />");
				}
				blw.writeLine("");
			}
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#readResponse(java.util.Properties)
		 */
		public void readResponse(Properties response) {
			for (int a = 0; a < this.aeTokenStates.length; a++) {
				this.aeTokenStates[a].clearStates();
				
				for (int t = 0; t < this.aeTokenStates[a].getTokenCount(); t++) {
					
					String state = response.getProperty(("part" + a + "_token" + t + "_state"), "O");
					String type = ("S".equals(state) ? response.getProperty("part" + a + "_token" + t + "_type") : "");
					
					if ("S".equals(state))
						state = AnnotationEditorFeedbackPanel.START;
					else if ("C".equals(state))
						state = AnnotationEditorFeedbackPanel.CONTINUE;
					else if ("O".equals(state))
						state = AnnotationEditorFeedbackPanel.OTHER;
					
					this.aeTokenStates[a].setStateAt(t, state, type);
				}
			}
		}
	}
}

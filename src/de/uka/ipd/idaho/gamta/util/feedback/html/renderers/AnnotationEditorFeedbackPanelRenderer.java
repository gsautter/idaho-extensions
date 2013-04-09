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
			blw.writeLine("  initContext();");
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine("var _DEBUG = false;");
			blw.writeLine("");
			blw.writeLine("var _replaceContext = false;		// replace the system context menu?");
			blw.writeLine("var _mouseOverContext = false;		// is the mouse over the context menu?");
			blw.writeLine("var _contextHasContent = false;");
			blw.writeLine("");
			blw.writeLine("var _divContext;	// makes my life easier");
			blw.writeLine("var _feedbackForm;");
			blw.writeLine("");
			blw.writeLine("var _colors = new Object();");
			blw.writeLine("");
			
			
			blw.writeLine("function initContext() {");
			blw.writeLine("  _divContext = $$('divContext');");
			blw.writeLine("  _feedbackForm = $$('feedbackForm');");
			blw.writeLine("  ");
			blw.writeLine("  _divContext.onmouseover = function() { _mouseOverContext = true; };");
			blw.writeLine("  _divContext.onmouseout = function() { _mouseOverContext = false; };");
			blw.writeLine("  ");
			blw.writeLine("  document.body.onmousedown = contextMouseDown;");
			blw.writeLine("  document.body.oncontextmenu = contextShow;");
			blw.writeLine("  ");
			
			String[] types = this.aefp.getDetailTypes();
			for (int t = 0; t < types.length; t++) {
				blw.writeLine("  _colors." + types[t] + " = '" + FeedbackPanel.getRGB(this.aefp.getDetailColor(types[t])) + "';");
			}
			
			blw.writeLine("  ");
			blw.writeLine("  ");
			blw.writeLine("  var p = 0;");
			blw.writeLine("  var t = 1;");
			blw.writeLine("  while (t != 0) {");
			blw.writeLine("    _part = p;");
			blw.writeLine("    t = 0;");
			blw.writeLine("    while ($('token' + t)) {");
			blw.writeLine("      var token = $('token' + t);");
			blw.writeLine("      if (token) {");
			blw.writeLine("        token.onmousedown = startSelection;");
			blw.writeLine("        token.onmouseup = endSelection;");
			blw.writeLine("      }");
			blw.writeLine("      var space = $('space' + t);");
			blw.writeLine("      if (space) {");
			blw.writeLine("        space.onmousedown = startSelection;");
			blw.writeLine("        space.onmouseup = endSelection;");
			blw.writeLine("      }");
			blw.writeLine("      if (t == 0) p++;");
			blw.writeLine("      t++;");
			blw.writeLine("    }");
			blw.writeLine("    ");
			blw.writeLine("    if (t != 0)");
			blw.writeLine("      adjustColors();");
			blw.writeLine("    ");
			blw.writeLine("    _part = null;");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function adjustColors() {");
			blw.writeLine("  var t = 0;");
			blw.writeLine("  while ($('token' + t)) {");
			blw.writeLine("    var token = $('token' + t);");
			blw.writeLine("    var space = $('space' + t);");
			blw.writeLine("    if (token) {");
			blw.writeLine("      var state = getState(t);");
			blw.writeLine("      var type = getType(t);");
			blw.writeLine("      if (state && (state == 'S')) {");
			blw.writeLine("        if (type && _colors[type]) {");
			blw.writeLine("          token.style.backgroundColor = _colors[type];");
			blw.writeLine("          if (space)");
			blw.writeLine("            space.style.backgroundColor = 'FFFFFF';");
			blw.writeLine("        }");
			blw.writeLine("      }");
			blw.writeLine("      else if (state && (state == 'C')) {");
			blw.writeLine("        if (type && _colors[type]) {");
			blw.writeLine("          token.style.backgroundColor = _colors[type];");
			blw.writeLine("          if (space)");
			blw.writeLine("            space.style.backgroundColor = _colors[type];");
			blw.writeLine("        }");
			blw.writeLine("      }");
			blw.writeLine("      else {");
			blw.writeLine("        token.style.backgroundColor = 'FFFFFF';");
			blw.writeLine("        if (space)");
			blw.writeLine("          space.style.backgroundColor = 'FFFFFF';");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    t++;");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("");

			
			blw.writeLine("function adjustContext() {");
			blw.writeLine("  _contextHasContent = false;");
			blw.writeLine("  if (_part == null) return;");
			blw.writeLine("  ");
			blw.writeLine("  var firstS = -1;");
			blw.writeLine("  var firstC = -1;");
			blw.writeLine("  var firstO = -1;");
			blw.writeLine("  ");
			blw.writeLine("  var aCount = 0;");
			blw.writeLine("  var sameValueCount = 0;");
			blw.writeLine("  var sameTypeCount = 0;");
			blw.writeLine("  ");
			blw.writeLine("  if ((_start != null) && (_end != null)) {");
			blw.writeLine("    ");
			blw.writeLine("    var values = new Array(_end - _start + 1);");
			blw.writeLine("    for (var v = 0; v < values.length; v++)");
			blw.writeLine("      values[v] = getTokenValue(_start + v);");
			blw.writeLine("    ");
			blw.writeLine("    for (var t = _start; t <= _end; t++) {");
			blw.writeLine("      var token = $('token' + t);");
			blw.writeLine("      if (token) {");
			blw.writeLine("        var state = getState(t);");
			blw.writeLine("        ");
			blw.writeLine("        if (state == 'S') {");
			blw.writeLine("          aCount++;");
			blw.writeLine("          if (firstS == -1)");
			blw.writeLine("            firstS = t;");
			blw.writeLine("        }");
			blw.writeLine("        if (state == 'C') {");
			blw.writeLine("          if (firstC == -1) {");
			blw.writeLine("            firstC = t;");
			blw.writeLine("            if (firstS == -1)");
			blw.writeLine("              aCount++;");
			blw.writeLine("          }");
			blw.writeLine("        }");
			blw.writeLine("        else if (state == 'O') {");
			blw.writeLine("          if (firstO == -1)");
			blw.writeLine("            firstO = t;");
			blw.writeLine("        }");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    ");
			blw.writeLine("    if (aCount == 0) {");
			blw.writeLine("      for (var s = 0; $('token' + s); s++) {");
			blw.writeLine("        if (tokenSequenceMatch(s, values, true, false))");
			blw.writeLine("          sameValueCount++;");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    ");
			blw.writeLine("    if (_type) {");
			blw.writeLine("      for (var s = 0; $('token' + s); s++) {");
			blw.writeLine("        if (('S' == getState(s)) && (_type == getType(s)))");
			blw.writeLine("          sameTypeCount++;");
			blw.writeLine("      }");
			blw.writeLine("      for (var s = 0; $('token' + s); s++) {");
			blw.writeLine("        if (tokenSequenceMatch(s, values, true, true) && (_type == getType(s)) && (!$('token' + (s + values.length)) || ('C' != getState(s + values.length))))");
			blw.writeLine("          sameValueCount++;");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  ");
			blw.writeLine("  if (_DEBUG) alert('part = ' + _part + ', firstS = ' + firstS + ', firstC = ' + firstC + ', firstO = ' + firstO + ', aCount = ' + aCount + ', _type = ' + _type);");
			blw.writeLine("  ");
			blw.writeLine("  $$('showSelectedTokens').style.display = (_DEBUG ? '' : 'none');");
			blw.writeLine("  $$('showValue').style.display = (_DEBUG ? '' : 'none');");
			blw.writeLine("  _contextHasContent = (_DEBUG || _selection || _type || (aCount == 1));");
			blw.writeLine("  ");
			blw.writeLine("  //  annotate functions");
			blw.writeLine("  var a = 0;");
			blw.writeLine("  while ($$('annotate' + a) && $$('annotateAll' + a)) {");
			blw.writeLine("    $$('annotate' + a).style.display = (((aCount == 0) && _selection) ? '' : 'none');");
			blw.writeLine("    $$('annotateAll' + a).style.display = (((aCount == 0) && (sameValueCount > 1) && _selection) ? '' : 'none');");
			blw.writeLine("    a++;");
			blw.writeLine("  }");
			blw.writeLine("  ");
			blw.writeLine("  //  remove");
			blw.writeLine("  $$('remove').style.display = ((aCount == 1) ? '' : 'none');");
			blw.writeLine("  if (sameValueCount > 1) {");
			blw.writeLine("    $$('removeAllValueItem').innerHTML = ('Remove all Annotations of \\'' + getValue(_start, _end) + '\\'');");
			blw.writeLine("    $$('removeAllValue').style.display = '';");
			blw.writeLine("  }");
			blw.writeLine("  else $$('removeAllValue').style.display = 'none';");
			blw.writeLine("  if (sameTypeCount > 1) {");
			blw.writeLine("    $$('removeAllTypeItem').innerHTML = ('Remove all \\'' + _type + '\\' Annotations');");
			blw.writeLine("    $$('removeAllType').style.display = '';");
			blw.writeLine("  }");
			blw.writeLine("  else $$('removeAllType').style.display = 'none';");
			blw.writeLine("  $$('removeAllSelection').style.display = ((aCount > 1) ? '' : 'none');");
			blw.writeLine("  ");
			blw.writeLine("  //  extend");
			blw.writeLine("  $$('extend').style.display = (((aCount == 1) && (firstO != -1)) ? '' : 'none');");
			blw.writeLine("  ");
			blw.writeLine("  //  merge");
			blw.writeLine("  $$('merge').style.display = ((_type && (aCount > 1)) ? '' : 'none');");
			blw.writeLine("  ");
			blw.writeLine("  //  adjust splitting functions");
			blw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			blw.writeLine("    var token = $('token' + _start);");
			blw.writeLine("    var nextToken = $('token' + (_start + 1));");
			blw.writeLine("    ");
			blw.writeLine("    $$('splitAroundItem').innerHTML = ('Split Annotation around \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    $$('splitBeforeItem').innerHTML = ('Split Annotation before \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    $$('splitAfterItem').innerHTML = ('Split Annotation after \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    ");
			blw.writeLine("    $$('splitAround').style.display = (((getState(_start) == 'C') && nextToken && (getState(_start + 1) == 'C')) ? '' : 'none');");
			blw.writeLine("    $$('splitBefore').style.display = ((getState(_start) == 'C') ? '' : 'none');");
			blw.writeLine("    $$('splitAfter').style.display = ((nextToken && (getState(_start + 1) == 'C')) ? '' : 'none');");
			blw.writeLine("    ");
			blw.writeLine("    $$('cutUpToItem').innerHTML = ('Cut Annotation up to \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    $$('cutFromItem').innerHTML = ('Cut Annotation from \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    $$('cutBeforeItem').innerHTML = ('Cut Annotation before \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    $$('cutAfterItem').innerHTML = ('Cut Annotation after \\'' + token.innerHTML + '\\'');");
			blw.writeLine("    ");
			blw.writeLine("    $$('cutUpTo').style.display = ((nextToken && (getState(_start + 1) == 'C')) ? '' : 'none');");
			blw.writeLine("    $$('cutFrom').style.display = ((getState(_start) == 'C') ? '' : 'none');");
			blw.writeLine("    $$('cutBefore').style.display = ((getState(_start) == 'C') ? '' : 'none');");
			blw.writeLine("    $$('cutAfter').style.display = ((nextToken && (getState(_start + 1) == 'C')) ? '' : 'none');");
			blw.writeLine("  }");
			blw.writeLine("  else {");
			blw.writeLine("    $$('splitAround').style.display = 'none';");
			blw.writeLine("    $$('splitBefore').style.display = 'none';");
			blw.writeLine("    $$('splitAfter').style.display = 'none';");
			blw.writeLine("    $$('cutUpTo').style.display = 'none';");
			blw.writeLine("    $$('cutFrom').style.display = 'none';");
			blw.writeLine("    $$('cutBefore').style.display = 'none';");
			blw.writeLine("    $$('cutAfter').style.display = 'none';");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("// call from the onMouseDown event, passing the event if standards compliant");
			blw.writeLine("function contextMouseDown(event) {");
			blw.writeLine("  if (_mouseOverContext)");
			blw.writeLine("    return;");
			blw.writeLine("");
			blw.writeLine("  // IE is evil and doesn't pass the event object");
			blw.writeLine("  if (event == null)");
			blw.writeLine("    event = window.event;");
			blw.writeLine("  ");
			blw.writeLine("  // we assume we have a standards compliant browser, but check if we have IE");
			blw.writeLine("  var target = event.target != null ? event.target : event.srcElement;");
			blw.writeLine("");
			blw.writeLine("  // only show the context menu if the right mouse button is pressed");
			blw.writeLine("  //   and a hyperlink has been clicked (the code can be made more selective)");
			blw.writeLine("  if (event.button == 2)");
			blw.writeLine("    _replaceContext = true;");
			blw.writeLine("  else if (!_mouseOverContext)");
			blw.writeLine("    _divContext.style.display = 'none';");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("function closeContext() {");
			blw.writeLine("  _mouseOverContext = false;");
			blw.writeLine("  _divContext.style.display = 'none';");
			blw.writeLine("  ");
			blw.writeLine("  // clean up selection state");
			blw.writeLine("  _part = null;");
			blw.writeLine("  _start = null;");
			blw.writeLine("  _end = null;");
			blw.writeLine("  _selection = null;");
			blw.writeLine("  _type = null;");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("// call from the onContextMenu event, passing the event");
			blw.writeLine("// if this function returns false, the browser's context menu will not show up");
			blw.writeLine("function contextShow(event) {");
			blw.writeLine("  if (_mouseOverContext)");
			blw.writeLine("    return;");
			blw.writeLine("  ");
			blw.writeLine("  // IE is evil and doesn't pass the event object");
			blw.writeLine("  if (event == null)");
			blw.writeLine("    event = window.event;");
			blw.writeLine("");
			blw.writeLine("  // we assume we have a standards compliant browser, but check if we have IE");
			blw.writeLine("  var target = event.target != null ? event.target : event.srcElement;");
			blw.writeLine("");
			blw.writeLine("  if (_replaceContext) {");
			blw.writeLine("    getSelectionState();");
			blw.writeLine("    adjustContext();");
			blw.writeLine("    ");
			blw.writeLine("    if (_contextHasContent) {");
			blw.writeLine("      ");
			blw.writeLine("      // document.body.scrollTop does not work in IE");
			blw.writeLine("      var scrollTop = document.body.scrollTop ? document.body.scrollTop :");
			blw.writeLine("        document.documentElement.scrollTop;");
			blw.writeLine("      var scrollLeft = document.body.scrollLeft ? document.body.scrollLeft :");
			blw.writeLine("        document.documentElement.scrollLeft;");
			blw.writeLine("      ");
			blw.writeLine("      // hide the menu first to avoid an 'up-then-over' visual effect");
			blw.writeLine("      _divContext.style.display = 'none';");
			blw.writeLine("      _divContext.style.left = event.clientX + scrollLeft + 'px';");
			blw.writeLine("      _divContext.style.top = event.clientY + scrollTop + 'px';");
			blw.writeLine("      _divContext.style.display = 'block';");
			blw.writeLine("      ");
			blw.writeLine("    }");
			blw.writeLine("    ");
			blw.writeLine("    _contextHasContent = false;");
			blw.writeLine("    _replaceContext = false;");
			blw.writeLine("");
			blw.writeLine("    return false;");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("// comes from prototype.js; this is simply easier on the eyes and fingers");
			blw.writeLine("function $(id) {");
			blw.writeLine("  return ((_part == null) ? $$(id) : $$('part' + _part + '_' + id));");
			blw.writeLine("}");
			blw.writeLine("function $$(id) {");
			blw.writeLine("  return document.getElementById(id);");
			blw.writeLine("}");
			blw.writeLine("");
			blw.writeLine("");
			
		
			blw.writeLine("// working variables for capturing active tokens in Netscape");
			blw.writeLine("var _activePart = null;");
			blw.writeLine("var _activeToken = null;");
			blw.writeLine("");
			
			blw.writeLine("function activateToken(part, index) {");
			blw.writeLine("  _activePart = part;");
			blw.writeLine("  _activeToken = index;");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function inactivateToken(part, index) {");
			blw.writeLine("  _activePart = null;");
			blw.writeLine("  _activeToken = null;");
			blw.writeLine("}");
			blw.writeLine("");
			blw.writeLine("");
			
			
			blw.writeLine("// working variables for capturing clicks on tokens");
			blw.writeLine("var _clickedPart = null;");
			blw.writeLine("var _clickedToken = null;");
			blw.writeLine("");
			
			blw.writeLine("function clickToken(part, index) {");
			blw.writeLine("  _clickedPart = part;");
			blw.writeLine("  _clickedToken = index;");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("// working variables for capturing selections");
			blw.writeLine("var _selectionStartPart = null;");
			blw.writeLine("var _selectionStart = null;");
			blw.writeLine("var _selectionEndPart = null;");
			blw.writeLine("var _selectionEnd = null;");
			blw.writeLine("");
			
			blw.writeLine("function startSelection(event) {");
			blw.writeLine("  if (!event || (event == null)) event = window.event;");
			blw.writeLine("  if ((_activeToken != null) && event && isLeftClick(event.button)) {");
			blw.writeLine("    _selectionStartPart = _activePart;");
			blw.writeLine("    _selectionStart = _activeToken;");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function endSelection(event) {");
			blw.writeLine("  if (!event || (event == null)) event = window.event;");
			blw.writeLine("  if ((_activeToken != null) && event && isLeftClick(event.button)) {");
			blw.writeLine("    _selectionEndPart = _activePart;");
			blw.writeLine("    _selectionEnd = _activeToken;");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("function isLeftClick(button) {");
			blw.writeLine("  return (((navigator.appName == 'Netscape') && (button == 0)) || (button == 1));");
			blw.writeLine("}");
			blw.writeLine("");
			
		
			blw.writeLine("// state variables to use for context menu");
			blw.writeLine("var _part = null;");
			blw.writeLine("");
			blw.writeLine("var _start = null;");
			blw.writeLine("var _end = null;");
			blw.writeLine("var _selection = null;");
			blw.writeLine("var _type = null;");
			blw.writeLine("");
			
			blw.writeLine("function getSelectionState() {");
			blw.writeLine("  if (_selectionStartPart != _selectionEndPart) {");
			blw.writeLine("    alert('Please work in one part at a time.');");
			blw.writeLine("    ");
			blw.writeLine("    _selectionStartPart = null;");
			blw.writeLine("    _selectionStart = null;");
			blw.writeLine("    _selectionEndPart = null;");
			blw.writeLine("    _selectionEnd = null;");
			blw.writeLine("    ");
			blw.writeLine("    _clickedPart = null;");
			blw.writeLine("    _clickedToken = null;");
			blw.writeLine("    ");
			blw.writeLine("    _part = null;");
			blw.writeLine("     _start = null;");
			blw.writeLine("     _end = null;");
			blw.writeLine("     _selection = null;");
			blw.writeLine("     _type = null;");
			blw.writeLine("     ");
			blw.writeLine("     return;");
			blw.writeLine("  }");
			blw.writeLine("  else {");
			blw.writeLine("    _part = _selectionStartPart;");
			blw.writeLine("  }");
			blw.writeLine("  ");
			blw.writeLine("  ");
			blw.writeLine("  var selection = '';");
			blw.writeLine("  if (window.getSelection)");
			blw.writeLine("    selection = window.getSelection();");
			blw.writeLine("  else if (document.getSelection)");
			blw.writeLine("    selection = document.getSelection();");
			blw.writeLine("  else if (document.selection)");
			blw.writeLine("    selection = document.selection.createRange().text;");
			blw.writeLine("  ");
			blw.writeLine("  var type = null;");
			blw.writeLine("  var sameType = true;");
			blw.writeLine("  ");
			blw.writeLine("  if ((selection != '') && (_selectionStart != null) && (_selectionEnd != null)) {");
			blw.writeLine("     _start = Math.ceil(Math.min(_selectionStart, _selectionEnd));");
			blw.writeLine("     _end = Math.floor(Math.max(_selectionStart, _selectionEnd));");
			blw.writeLine("    selection = '';");
			blw.writeLine("    for (var t = _start; t <= _end; t++) {");
			blw.writeLine("      if (_start < t)");
			blw.writeLine("        selection = (selection + $('space' + t).innerHTML);");
			blw.writeLine("      ");
			blw.writeLine("      var token = $('token' + t);");
			blw.writeLine("      if (token) {");
			blw.writeLine("        selection = (selection + token.innerHTML);");
			blw.writeLine("        ");
			blw.writeLine("        if (getState(t) == 'S') {");
			blw.writeLine("          if (type == null)");
			blw.writeLine("            type = getType(t);");
			blw.writeLine("          else");
			blw.writeLine("            sameType = (sameType && (getType(t) == type));");
			blw.writeLine("        }");
			blw.writeLine("        else if (getState(t) == 'C') {");
			blw.writeLine("          if (type == null)");
			blw.writeLine("            type = getType(t);");
			blw.writeLine("        }");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    ");
			blw.writeLine("    _selection = selection;");
			blw.writeLine("    _type = ((type && sameType) ? type : null);");
			blw.writeLine("  }");
			blw.writeLine("  else if ((_clickedToken != null) && (_clickedPart != null)) {");
			blw.writeLine("    _part = _clickedPart;");
			blw.writeLine("     _start = _clickedToken;");
			blw.writeLine("     _end = _clickedToken;");
			blw.writeLine("     _selection = null;");
			blw.writeLine("     _type = null;");
			blw.writeLine("  }");
			blw.writeLine("  else {");
			blw.writeLine("    _part = null;");
			blw.writeLine("     _start = null;");
			blw.writeLine("     _end = null;");
			blw.writeLine("     _selection = null;");
			blw.writeLine("     _type = null;");
			blw.writeLine("  }");
			blw.writeLine("  ");
			blw.writeLine("  _selectionStartPart = null;");
			blw.writeLine("  _selectionStart = null;");
			blw.writeLine("  _selectionEndPart = null;");
			blw.writeLine("  _selectionEnd = null;");
			blw.writeLine("  ");
			blw.writeLine("  _clickedPart = null;");
			blw.writeLine("  _clickedToken = null;");
			blw.writeLine("}");
			blw.writeLine("");
			blw.writeLine("");
			
			
			blw.writeLine("// context menu functions");
			blw.writeLine("function showSelectedTokens() {");
			blw.writeLine("  var selection = (_selection ? _selection : '<Nothing Selected>');");
			blw.writeLine("  closeContext();");
			blw.writeLine("  alert(selection);");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("function annotate(type) {");
			blw.writeLine("  if (_selection && (_start != null) && (_end != null)) {");
			blw.writeLine("    for (var t = _start; t <= _end; t++) {");
			blw.writeLine("      var token = $('token' + t)");
			blw.writeLine("      if (token) {");
			blw.writeLine("        setState(t, ((t == _start) ? 'S' : 'C'));");
			blw.writeLine("        setType(t, type);");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    ");
			blw.writeLine("    var tokenAfter = $('token' + (_end+1));");
			blw.writeLine("    if (tokenAfter && (getState(_end + 1) == 'C'))");
			blw.writeLine("      setState((_end + 1), 'S');");
			blw.writeLine("    adjustColors();");
			blw.writeLine("  }");
			blw.writeLine("  else alert('Cannot annotate nothing !!!');");
			blw.writeLine("  ");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function annotateAll(type) {");
			blw.writeLine("  ");
			blw.writeLine("  //  extract match token sequence");
			blw.writeLine("  var values = new Array(_end - _start + 1);");
			blw.writeLine("  for (var v = 0; v < values.length; v++)");
			blw.writeLine("    values[v] = getTokenValue(_start + v);");
			blw.writeLine("  ");
			blw.writeLine("  //  annotate all matches");
			blw.writeLine("  for (var s = 0; $('token' + s); s++) {");
			blw.writeLine("    if (tokenSequenceMatch(s, values, true, false)) {");
			blw.writeLine("      ");
			blw.writeLine("      //  annotate");
			blw.writeLine("      for (var t = 0; t < values.length; t++) {");
			blw.writeLine("        setState((s + t), ((t == 0) ? 'S' : 'C'));");
			blw.writeLine("        setType((s + t), type);");
			blw.writeLine("      }");
			blw.writeLine("      ");
			blw.writeLine("      //  fix overwritten start of annotation (if any)");
			blw.writeLine("      if ($('token' + (s + values.length)) && (getState(s + values.length) == 'C'))");
			blw.writeLine("        setState((s + values.length), 'S');");
			blw.writeLine("      ");
			blw.writeLine("      //  jump to last token of added annotation (loop increment will move forward an additional token)");
			blw.writeLine("      s += (values.length - 1);");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors();");
			blw.writeLine("  ");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			
			blw.writeLine("function remove() {");
			blw.writeLine("  if ((_start != null) && (_end != null)) {");
			blw.writeLine("    while ($('token' + _start) && (getState(_start) == 'C'))");
			blw.writeLine("      _start--;");
			blw.writeLine("    while ($('token' + (_end+1)) && (getState(_end+1) == 'C'))");
			blw.writeLine("      _end++;");
			blw.writeLine("    for (var t = _start; t <= _end; t++) {");
			blw.writeLine("      var token = $('token' + t)");
			blw.writeLine("      if (token) {");
			blw.writeLine("        setState(t, 'O');");
			blw.writeLine("        setType(t, null);");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    adjustColors();");
			blw.writeLine("  }");
			blw.writeLine("  else alert('Nothing to remove!!!');");
			blw.writeLine("  ");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("");

			blw.writeLine("function removeAllValue() {");
			blw.writeLine("  ");
			blw.writeLine("  //  extract match token sequence");
			blw.writeLine("  var values = new Array(_end + 1 - _start);");
			blw.writeLine("  for (var v = 0; v < values.length; v++)");
			blw.writeLine("    values[v] = getTokenValue(_start + v);");
			blw.writeLine("  ");
			blw.writeLine("  //  de-annotate all matches");
			blw.writeLine("  for (var s = 0; $('token' + (s + values.length - 1)); s++) {");
			blw.writeLine("    if (tokenSequenceMatch(s, values, true, true) && (_type = getType(s)) && (!$('token' + (s + values.length)) || ('C' != getState(s + values.length)))) {");
			blw.writeLine("      ");
			blw.writeLine("      //  de-annotate");
			blw.writeLine("      for (var t = 0; t < values.length; t++) {");
			blw.writeLine("        setState((s + t), 'O');");
			blw.writeLine("        setType((s + t), null);");
			blw.writeLine("      }");
			blw.writeLine("      ");
			blw.writeLine("      //  fix overwritten start of annotation (if any)");
			blw.writeLine("      if ($('token' + (s + values.length)) && ('C' == getState(s + values.length)))");
			blw.writeLine("        setState((s + values.length),  'S');");
			blw.writeLine("      ");
			blw.writeLine("      //  jump to last token of added annotation (loop increment will move forward an additional token)");
			blw.writeLine("      s += (values.length - 1);");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors();");
			blw.writeLine("");  
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function removeAllType() {");
			blw.writeLine("");  
			blw.writeLine("  //  de-annotate all matches");
			blw.writeLine("  for (var s = 0; $('token' + s); s++) {");
			blw.writeLine("    if ((getType(s) == _type) && ('S' == getState(s))) {");
			blw.writeLine("      ");
			blw.writeLine("      //  de-annotate");
			blw.writeLine("      setState(s, 'O');");
			blw.writeLine("      setType(s, null);");
			blw.writeLine("      for (var t = 1; $('token' + (s + t)) && ('C' == getState(s + t)); t++) {");
			blw.writeLine("        setState((s + t), 'O');");
			blw.writeLine("        setType((s + t), null);");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors();");
			blw.writeLine("  ");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("//  de-annotate all selected annotations");
			blw.writeLine("function removeAllSelection() {");
			blw.writeLine("  ");
			blw.writeLine("  var start = findStart(_start);");
			blw.writeLine("  var end = findEnd(_end);");
			blw.writeLine("  ");
			blw.writeLine("  for (var t = start; t <= end; t++) {");
			blw.writeLine("    setState(t, 'O');");
			blw.writeLine("    setType(t, null);");
			blw.writeLine("  }");
			blw.writeLine("  adjustColors();");
			blw.writeLine("  ");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			
			
			blw.writeLine("function merge() {");
			blw.writeLine("  if (_selection && _type && (_start != null) && (_end != null)) {");
			blw.writeLine("    while ($('token' + _start) && (getState(_start) == 'O'))");
			blw.writeLine("      _start++;");
			blw.writeLine("    while ($('token' + _start) && (getState(_start) == 'C'))");
			blw.writeLine("      _start--;");
			blw.writeLine("    while ($('token' + _end) && (getState(_end) == 'O'))");
			blw.writeLine("      _end--;");
			blw.writeLine("    while ($('token' + (_end+1)) && (getState(_end+1) == 'C'))");
			blw.writeLine("      _end++;");
			blw.writeLine("    ");
			blw.writeLine("    for (var t = _start; t <= _end; t++) {");
			blw.writeLine("      var token = $('token' + t)");
			blw.writeLine("      if (token) {");
			blw.writeLine("        setState(t, ((t == _start) ? 'S' : 'C'));");
			blw.writeLine("        setType(t, _type);");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    adjustColors();");
			blw.writeLine("  }");
			blw.writeLine("  ");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("function extend() {");
			blw.writeLine("  if (_selection && _type && (_start != null) && (_end != null)) {");
			blw.writeLine("    for (var t = _start; t <= _end; t++) {");
			blw.writeLine("      var token = $('token' + t);");
			blw.writeLine("      if (token) {");
			blw.writeLine("        setState(t, (((t > _start) || (getState(t) == 'C')) ? 'C' : 'S'));");
			blw.writeLine("        setType(t, _type);");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    adjustColors();");
			blw.writeLine("  }");
			blw.writeLine("  ");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("function splitAround() {");
			blw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			blw.writeLine("    var token = $('token' + _start);");
			blw.writeLine("    var nextToken = $('token' + (_start+1));");
			blw.writeLine("    if (token && nextToken) {");
			blw.writeLine("      setState(_start, 'O');");
			blw.writeLine("      setType(_start, null);");
			blw.writeLine("      setState((_start + 1), 'S');");
			blw.writeLine("      adjustColors();");
			blw.writeLine("     }");
			blw.writeLine("  }");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function splitBefore() {");
			blw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			blw.writeLine("    var token = $('token' + _start);");
			blw.writeLine("    if (token) {");
			blw.writeLine("      setState(_start, 'S');");
			blw.writeLine("      adjustColors();");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function splitAfter() {");
			blw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			blw.writeLine("    var nextToken = $('token' + (_start+1));");
			blw.writeLine("    if (nextToken) {");
			blw.writeLine("      setState((_start + 1), 'S');");
			blw.writeLine("      adjustColors();");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;  ");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("function cutUpTo() {");
			blw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			blw.writeLine("    var nextToken = $('token' + (_start+1));");
			blw.writeLine("    if (nextToken)");
			blw.writeLine("      setState((_start + 1), 'S');");
			blw.writeLine("    var s = _start;");
			blw.writeLine("    var token = $('token' + s);");
			blw.writeLine("    while (token) {");
			blw.writeLine("      var ts = getState(s);");
			blw.writeLine("      setState(s, 'O');");
			blw.writeLine("      if (ts == 'S')");
			blw.writeLine("        token = null;");
			blw.writeLine("      else {");
			blw.writeLine("        s = (s-1);");
			blw.writeLine("        token = $('token' + s);");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    adjustColors();");
			blw.writeLine("  }");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function cutFrom() {");
			blw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			blw.writeLine("    var s = _start;");
			blw.writeLine("    token = $('token' + s);");
			blw.writeLine("    while (token) {");
			blw.writeLine("      if (getState(s) == 'C') {");
			blw.writeLine("        setState(s, 'O');");
			blw.writeLine("        s = (s+1);");
			blw.writeLine("        token = $('token' + s);");
			blw.writeLine("      }");
			blw.writeLine("      else token = null;");
			blw.writeLine("    }");
			blw.writeLine("    adjustColors();");
			blw.writeLine("  }");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;  ");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function cutBefore() {");
			blw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			blw.writeLine("    var token = $('token' + (_start));");
			blw.writeLine("    if (token)");
			blw.writeLine("      setState(_start, 'S');");
			blw.writeLine("    var s = _start-1;");
			blw.writeLine("    token = $('token' + s);");
			blw.writeLine("    while (token) {");
			blw.writeLine("      var ts = getState(s);");
			blw.writeLine("      setState(s, 'O');");
			blw.writeLine("      if (ts == 'S')");
			blw.writeLine("        token = null;");
			blw.writeLine("      else {");
			blw.writeLine("        s = (s-1);");
			blw.writeLine("        token = $('token' + s);");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    adjustColors();");
			blw.writeLine("  }");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;  ");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function cutAfter() {");
			blw.writeLine("  if (_selection && (_start != null) && (_end != null) && (_start == _end)) {");
			blw.writeLine("    var s = _start+1;");
			blw.writeLine("    token = $('token' + s);");
			blw.writeLine("    while (token) {");
			blw.writeLine("      if (getState(s) == 'C') {");
			blw.writeLine("        setState(s, 'O');");
			blw.writeLine("        s = (s+1);");
			blw.writeLine("        token = $('token' + s);");
			blw.writeLine("      }");
			blw.writeLine("      else token = null;");
			blw.writeLine("    }");
			blw.writeLine("    adjustColors();");
			blw.writeLine("  }");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;  ");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("function findStart(start) {");
			blw.writeLine("  while (getState(start) == 'O')");
			blw.writeLine("    start++;");
			blw.writeLine("  while (getState(start) != 'S')");
			blw.writeLine("    start--;");
			blw.writeLine("  return start;");
			blw.writeLine("}");
			blw.writeLine("");
			blw.writeLine("function findEnd(end) {");
			blw.writeLine("  while (getState(end) == 'O')");
			blw.writeLine("    end--;");
			blw.writeLine("  while ($('token' + (end + 1)) && ('C' == getState(end + 1)))");
			blw.writeLine("    end++;");
			blw.writeLine("  return end;");
			blw.writeLine("}");
			blw.writeLine("");
			blw.writeLine("function tokenSequenceMatch(start, values, checkStates, mustBeAnnotated) {");
			blw.writeLine("  for (var v = 0; v < values.length; v++) {");
			blw.writeLine("    if (!$('token' + (start + v)))");
			blw.writeLine("      return false;");
			blw.writeLine("    ");
			blw.writeLine("    if (getTokenValue(start + v) != values[v])");
			blw.writeLine("      return false;");
			blw.writeLine("    ");
			blw.writeLine("    if (checkStates && (mustBeAnnotated ? (getState(start + v) != ((v == 0) ? 'S' : 'C')) : getState(start + v) != 'O'))");
			blw.writeLine("      return false;");
			blw.writeLine("  }");
			blw.writeLine("  return true;");
			blw.writeLine("}");
			blw.writeLine("");
			blw.writeLine("function showValue() {");
			blw.writeLine("  if (_selection && (_start != null) && (_end != null)) {");
			blw.writeLine("    var value = getValue(_start, _end);");
			blw.writeLine("    alert(value);");
			blw.writeLine("  }");
			blw.writeLine("  ");
			blw.writeLine("  closeContext();");
			blw.writeLine("  return false;");
			blw.writeLine("}");
			blw.writeLine("");
			blw.writeLine("function getValue(start, end) {");
			blw.writeLine("  var value = '';");
			blw.writeLine("  for (var t = start; t <= end; t++) {");
			blw.writeLine("    if (t != start)");
			blw.writeLine("      value += ' ';");
			blw.writeLine("    value += getTokenValue(t);");
			blw.writeLine("  }");
			blw.writeLine("  return value;");
			blw.writeLine("}");
			blw.writeLine("");
			blw.writeLine("function getTokenValue(index) {");
			blw.writeLine("  return $('token' + index).innerHTML;");
			blw.writeLine("}");
		
			
			blw.writeLine("function getState(index) {");
			blw.writeLine("  return _feedbackForm[('part' + _part + '_token' + index + '_state')].value;");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function setState(index, state) {");
			blw.writeLine("  _feedbackForm[('part' + _part + '_token' + index + '_state')].value = state;");
			blw.writeLine("}");
			blw.writeLine("");
			
			
			blw.writeLine("function getType(index) {");
			blw.writeLine("  return _feedbackForm[('part' + _part + '_token' + index + '_type')].value;");
			blw.writeLine("}");
			blw.writeLine("");
			
			blw.writeLine("function setType(index, type) {");
			blw.writeLine("  _feedbackForm[('part' + _part + '_token' + index + '_type')].value = ((type == null) ? '' : type);");
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
						blw.writeLine("<td>");
						if ((r + c) < types.length) {
							blw.write("<span style=\"background-color: " + FeedbackPanel.getRGB(this.aefp.getDetailColor(types[r+c])) + ";\">&nbsp;&nbsp;&nbsp;&nbsp;</span>&nbsp;");
							blw.writeLine(types[r+c]);
						}
						blw.writeLine("</td>");
					}
					blw.writeLine("</tr>");
				}
				blw.writeLine("</table>");
			}
			
			blw.writeLine("<div id=\"divContext\" style=\"border: 1px solid blue; display: none; position: absolute\">");
			blw.writeLine("<ul class=\"menu\">");
			blw.writeLine("");
			
			blw.writeLine("<li id=\"showSelectedTokens\" class=\"menuItem\"><a href=\"#\" onclick=\"return showSelectedTokens();\">Show Selected Tokens</a></li>");
			blw.writeLine("<li id=\"showValue\" class=\"menuItem\"><a href=\"#\" onclick=\"return showValue();\">Show Value</a></li>");
			blw.writeLine("");
			
			for (int t = 0; t < types.length; t++) {
				blw.writeLine("<li id=\"annotate" + t + "\" class=\"menuItem\"><a href=\"#\" onclick=\"return annotate('" + types[t] + "');\">Annotate as '" + types[t] + "'</a></li>");
				blw.writeLine("<li id=\"annotateAll" + t + "\" class=\"menuItem\"><a href=\"#\" onclick=\"return annotateAll('" + types[t] + "');\">Annotate all as '" + types[t] + "'</a></li>");
			}
			blw.writeLine("");
			
			blw.writeLine("<li id=\"remove\" class=\"menuItem\"><a href=\"#\" onclick=\"return remove();\">Remove Annotation</a></li>");
			blw.writeLine("<li id=\"removeAllValue\" class=\"menuItem\"><a id=\"removeAllValueItem\" href=\"#\" onclick=\"return removeAllValue();\">Remove all Annotations by Value</a></li>");
			blw.writeLine("<li id=\"removeAllType\" class=\"menuItem\"><a id=\"removeAllTypeItem\" href=\"#\" onclick=\"return removeAllType();\">Remove all Annotations by Type</a></li>");
			blw.writeLine("<li id=\"removeAllSelection\" class=\"menuItem\"><a href=\"#\" onclick=\"return removeAllSelection();\">Remove all Selected Annotations</a></li>");
			
			blw.writeLine("<li id=\"extend\" class=\"menuItem\"><a href=\"#\" onclick=\"return extend();\">Extend Annotation</a></li>");
			blw.writeLine("<li id=\"merge\" class=\"menuItem\"><a href=\"#\" onclick=\"return merge();\">Merge Annotations</a></li>");
			blw.writeLine("");
			
			blw.writeLine("<li id=\"splitAround\" class=\"menuItem\"><a id=\"splitAroundItem\" href=\"#\" onclick=\"return splitAround();\">Split Annotation Around</a></li>");
			blw.writeLine("<li id=\"splitBefore\" class=\"menuItem\"><a id=\"splitBeforeItem\" href=\"#\" onclick=\"return splitBefore();\">Split Annotation Before</a></li>");
			blw.writeLine("<li id=\"splitAfter\" class=\"menuItem\"><a id=\"splitAfterItem\" href=\"#\" onclick=\"return splitAfter();\">Split Annotation After</a></li>");
			blw.writeLine("");
			
			blw.writeLine("<li id=\"cutUpTo\" class=\"menuItem\"><a id=\"cutUpToItem\" href=\"#\" onclick=\"return cutUpTo();\">Cut Annotation Up To</a></li>");
			blw.writeLine("<li id=\"cutFrom\" class=\"menuItem\"><a id=\"cutFromItem\" href=\"#\" onclick=\"return cutFrom();\">Cut Annotation From</a></li>");
			blw.writeLine("<li id=\"cutBefore\" class=\"menuItem\"><a id=\"cutBeforeItem\" href=\"#\" onclick=\"return cutBefore();\">Cut Annotation Before</a></li>");
			blw.writeLine("<li id=\"cutAfter\" class=\"menuItem\"><a id=\"cutAfterItem\" href=\"#\" onclick=\"return cutAfter();\">Cut Annotation After</a></li>");
			blw.writeLine("");
			
			blw.writeLine("</ul>");
			blw.writeLine("</div>");
			blw.writeLine("");
			
			for (int a = 0; a < this.aeTokenStates.length; a++) {
				blw.writeLine("<table class=\"part\" id=\"part" + a + "\">");
				blw.writeLine("<tr>");
				blw.writeLine("<td>");
				
				for (int t = 0; t < this.aeTokenStates[a].getTokenCount(); t++) {
					if (t != 0) {
						blw.write("<span" +
								" class=\"token\"" +
								" id=\"part" + a + "_space" + t + "\"" +
								" onmouseover=\"activateToken(" + a + ", " + (t-1) + ".5);\"" +
								" onmouseout=\"inactivateToken(" + a + ", " + (t-1) + ".5);\"" +
								">" +
								this.aeTokenStates[a].getWhitespaceBefore(t) +
								"</span>");
					}
					blw.write("<span" +
							" class=\"token\"" +
							" id=\"part" + a + "_token" + t + "\"" +
							" onmouseover=\"activateToken(" + a + ", " + t + ");\"" +
							" onmouseout=\"inactivateToken(" + a + ", " + t + ");\"" +
							" onclick=\"clickToken(" + a + ", " + t + ");\"" +
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
					blw.writeLine("<input type=\"hidden\" name=\"part" + a + "_token" + t + "_state\" value=\"" + state + "\">");
					blw.writeLine("<input type=\"hidden\" name=\"part" + a + "_token" + t + "_type\" value=\"" + type + "\">");
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

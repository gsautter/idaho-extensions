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
import de.uka.ipd.idaho.gamta.util.feedback.panels.StartContinueOtherFeedbackPanel;

/**
 * @author sautter
 *
 */
public class StartContinueOtherFeedbackPanelRenderer extends FeedbackPanelHtmlRenderer {
	
	/**
	 * Constructor
	 */
	public StartContinueOtherFeedbackPanelRenderer() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer#canRender(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public int canRender(FeedbackPanel fp) {
		return getInheritanceDepth(StartContinueOtherFeedbackPanel.class, fp.getClass());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer#getRendererInstance(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public FeedbackPanelHtmlRendererInstance getRendererInstance(FeedbackPanel fp) {
		return ((fp instanceof StartContinueOtherFeedbackPanel) ? new StartContinueOtherFeedbackPanelRendererInstance((StartContinueOtherFeedbackPanel) fp) : null) ;
	}
	
	private static class StartContinueOtherFeedbackPanelRendererInstance extends FeedbackPanelHtmlRendererInstance {
		
		private StartContinueOtherFeedbackPanel scofp;
		
		StartContinueOtherFeedbackPanelRendererInstance(StartContinueOtherFeedbackPanel scofp) {
			super(scofp);
			this.scofp = scofp;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writeJavaScriptInitFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine("  colors." + StartContinueOtherFeedbackPanel.START + " = '#" + FeedbackPanel.getRGB(this.scofp.getStartColor()) + "';");
			blw.writeLine("  colors." + StartContinueOtherFeedbackPanel.CONTINUE + " = '#" + FeedbackPanel.getRGB(this.scofp.getContinueColor()) + "';");
			blw.writeLine("  colors." + StartContinueOtherFeedbackPanel.OTHER + " = '#" + FeedbackPanel.getRGB(this.scofp.getOtherColor()) + "';");
			blw.writeLine("  ");
			blw.writeLine("  var selects = document.getElementsByTagName('select');");
			blw.writeLine("  var selectNumber = 0;");
			blw.writeLine("  var lastSelect;");
			blw.writeLine("  for (s = 0; s < selects.length; s++) {");
			blw.writeLine("    selects[s].id = ('option' + selectNumber);");
			blw.writeLine("    selects[s].number = selectNumber;");
			blw.writeLine("    selects[s].oldValue = selects[s].value;");
			blw.writeLine("    if (lastSelect) {");
			blw.writeLine("      lastSelect.next = selects[s];");
			blw.writeLine("      selects[s].previous = lastSelect;");
			blw.writeLine("    }");
			blw.writeLine("    lastSelect = selects[s];");
			blw.writeLine("    adjustLayout(selectNumber);");
			blw.writeLine("    selectNumber++;");
			blw.writeLine("  }");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));

			blw.writeLine("function change(id) {");
			blw.writeLine("  var option = document.getElementById('option' + id);");
			blw.writeLine("  if ((option.value == '" + StartContinueOtherFeedbackPanel.CONTINUE + "') && (!option.previous || (option.previous.value == '" + StartContinueOtherFeedbackPanel.OTHER + "'))) {");
			blw.writeLine("    alert('Cannot continue without start');");
			blw.writeLine("    option.value = option.oldValue;");
			blw.writeLine("    return;");
			blw.writeLine("  }");
			blw.writeLine("  else if (option.next) {");
			blw.writeLine("    var nextOption = option.next;");
			blw.writeLine("    if (((option.value == '" + StartContinueOtherFeedbackPanel.START + "') || (option.value == '" + StartContinueOtherFeedbackPanel.CONTINUE + "')) && (nextOption.value == '" + StartContinueOtherFeedbackPanel.OTHER + "')) {");
			blw.writeLine("      nextOption.value = '" + StartContinueOtherFeedbackPanel.CONTINUE + "';");
			blw.writeLine("      change(nextOption.number);");
			blw.writeLine("    }");
			blw.writeLine("    else if ((option.value == '" + StartContinueOtherFeedbackPanel.OTHER + "') && (nextOption.value == '" + StartContinueOtherFeedbackPanel.CONTINUE + "')) {");
			blw.writeLine("      nextOption.value = '" + StartContinueOtherFeedbackPanel.OTHER + "';");
			blw.writeLine("      change(nextOption.number);");
			blw.writeLine("    }");
			blw.writeLine("    else adjustLayout(id + 1);");
			blw.writeLine("  }");
			blw.writeLine("  option.oldValue = option.value;");
			blw.writeLine("  ");
			blw.writeLine("  adjustLayout(id);");
			blw.writeLine("}");
			blw.writeLine("");
			blw.writeLine("function adjustLayout(id) {");
			blw.writeLine("  var option = document.getElementById('option' + id);");
			blw.writeLine("  var input = document.getElementById('input' + id);");
			blw.writeLine("  var line = document.getElementById('line' + id);");
			blw.writeLine("  if (option && input && line) {");
			blw.writeLine("    if (!option.previous) {");
			blw.writeLine("      line.style.marginTop = 0;");
			blw.writeLine("      input.style.marginTop = 0;");
			blw.writeLine("    }");
			blw.writeLine("    else if (option.value == '" + StartContinueOtherFeedbackPanel.START + "') {");
			blw.writeLine("      line.style.marginTop = startSpacing;");
			blw.writeLine("      input.style.marginTop = startSpacing;");
			blw.writeLine("    }");
			blw.writeLine("    else if ((option.value == '" + StartContinueOtherFeedbackPanel.OTHER + "') && (option.previous.value != '" + StartContinueOtherFeedbackPanel.OTHER + "')) {");
			blw.writeLine("      line.style.marginTop = otherSpacing;");
			blw.writeLine("      input.style.marginTop = otherSpacing;");
			blw.writeLine("    }");
			blw.writeLine("    else {");
			blw.writeLine("      line.style.marginTop = continueSpacing;");
			blw.writeLine("      input.style.marginTop = continueSpacing;");
			blw.writeLine("    }");
			blw.writeLine("    line.style.backgroundColor = colors[option.value];");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("");
			blw.writeLine("var colors = new Object();");
			blw.writeLine("");
			blw.writeLine("var startSpacing = " + this.scofp.getStartSpacing() + ";");
			blw.writeLine("var continueSpacing = " + this.scofp.getContinueSpacing() + ";");
			blw.writeLine("var otherSpacing = " + this.scofp.getOtherSpacing() + ";");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writeCssStyles(java.io.Writer)
		 */
		public void writeCssStyles(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine("table {");
			blw.writeLine("  border-width: 0;");
			blw.writeLine("  border-spacing: 0;");
			blw.writeLine("  border-collapse: collapse;");
			blw.writeLine("}");
			blw.writeLine("td {");
			blw.writeLine("  padding: 0;");
			blw.writeLine("  margin: 0;");
			blw.writeLine("}");
			blw.writeLine(".labelCell {");
			blw.writeLine("  width: 100%;");
			blw.writeLine("}");
			
			if (blw != out)
				blw.flush();
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writePanelBody(java.io.Writer)
		 */
		public void writePanelBody(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine("<table class=\"inputTable\">");
			
			for (int l = 0; l < this.scofp.lineCount(); l++) {
				blw.writeLine("<tr>");
				
				/*
				<p id="input0"><select name="category0" onchange="change(0);">
				  <option value="S">Start</option>
				  <option value="C">Continue</option>
				  <option value="O" selected>Other</option>
				</select></p>
        		 */
				blw.writeLine("<td class=\"inputCell\">");
				
				blw.writeLine("<p id=\"input" + l + "\">");
				blw.writeLine("<select type=\"checkbox\" name=\"option" + l + "\" onchange=\"change(" + l + ");\">");
				
				String option = this.scofp.getOptionAt(l);
				blw.write("<option value=\"" + StartContinueOtherFeedbackPanel.START + "\"" + (StartContinueOtherFeedbackPanel.START.equals(option) ? " selected" : "") + ">");
				blw.write(prepareForHtml(this.scofp.getStartLabel()));
				blw.writeLine("</option>");
				blw.write("<option value=\"" + StartContinueOtherFeedbackPanel.CONTINUE + "\"" + (StartContinueOtherFeedbackPanel.CONTINUE.equals(option) ? " selected" : "") + ">");
				blw.write(prepareForHtml(this.scofp.getContinueLabel()));
				blw.writeLine("</option>");
				blw.write("<option value=\"" + StartContinueOtherFeedbackPanel.OTHER + "\"" + (StartContinueOtherFeedbackPanel.OTHER.equals(option) ? " selected" : "") + ">");
				blw.write(prepareForHtml(this.scofp.getOtherLabel()));
				blw.writeLine("</option>");
				
				blw.writeLine("</select>");
				blw.writeLine("</p>");
				
				blw.writeLine("</td>");
				
				//<p id="line0">Test1</p>
				blw.writeLine("<td class=\"labelCell\">");
				
				blw.write("<p id=\"line" + l + "\">");
				blw.write(prepareForHtml(this.scofp.getLineAt(l)));
				blw.writeLine("</p>");
				
				blw.writeLine("</td>");
				
				blw.writeLine("</tr>");
			}
			
			blw.writeLine("</table>");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#readResponse(java.util.Properties)
		 */
		public void readResponse(Properties response) {
			for (int l = 0; l < this.scofp.lineCount(); l++)
				this.scofp.setOptionAt(l, response.getProperty(("option" + l)));
		}
	}
}

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
import de.uka.ipd.idaho.gamta.util.feedback.panels.CheckBoxFeedbackPanel;

/**
 * HTML renderer for CheckBoxFeedbackPanel.
 * 
 * @author sautter
 */
public class CheckBoxFeedbackPanelRenderer extends FeedbackPanelHtmlRenderer {
	
	/**
	 * Constructor
	 */
	public CheckBoxFeedbackPanelRenderer() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer#canRender(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public int canRender(FeedbackPanel fp) {
		return getInheritanceDepth(CheckBoxFeedbackPanel.class, fp.getClass());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer#getRendererInstance(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public FeedbackPanelHtmlRendererInstance getRendererInstance(FeedbackPanel fp) {
		return ((fp instanceof CheckBoxFeedbackPanel) ? new CheckBoxFeedbackPanelRendererInstance((CheckBoxFeedbackPanel) fp) : null) ;
	}
	
	private static class CheckBoxFeedbackPanelRendererInstance extends FeedbackPanelHtmlRendererInstance {
		
		private CheckBoxFeedbackPanel cbfp;
		
		CheckBoxFeedbackPanelRendererInstance(CheckBoxFeedbackPanel cbfp) {
			super(cbfp);
			this.cbfp = cbfp;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writeJavaScriptInitFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine("  var checkboxes = document.getElementsByTagName('input');");
			blw.writeLine("  var checkBoxNumber = 0;");
			blw.writeLine("  for (s = 0; s < checkboxes.length; s++) {");
			blw.writeLine("    if (checkboxes[s].type == 'checkbox') {");
			blw.writeLine("      checkboxes[s].id = ('checkbox' + checkBoxNumber);");
			blw.writeLine("      adjustLayout(checkBoxNumber);");
			blw.writeLine("      checkBoxNumber++;");
			blw.writeLine("    }");
			blw.writeLine("  }");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));

			blw.writeLine("function invert(id) {");
			blw.writeLine("  var checkbox = document.getElementById('checkbox' + id);");
			blw.writeLine("  if (checkbox) {");
			blw.writeLine("    checkbox.checked = !checkbox.checked;");
			blw.writeLine("    change(id);");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("");
			
			blw.writeLine("function change(id) {");
			blw.writeLine("  adjustLayout(id);");
			blw.writeLine("  var checkbox = document.getElementById('checkbox' + id);");
			blw.writeLine("  var state = document.getElementById('state' + id);");
			blw.writeLine("  if (checkbox && state) {");
			blw.writeLine("    if (checkbox.checked)");
			blw.writeLine("      state.value = 'T';");
			blw.writeLine("    else state.value = 'F';");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("");
			
			blw.writeLine("function adjustLayout(id) {");
			blw.writeLine("  var checkbox = document.getElementById('checkbox' + id);");
			blw.writeLine("  var input = document.getElementById('input' + id);");
			blw.writeLine("  var line = document.getElementById('line' + id);");
			blw.writeLine("  if (checkbox && input && line) {");
			blw.writeLine("    if (checkbox.checked) {");
			blw.writeLine("      line.style.backgroundColor = trueColor;");
			blw.writeLine("      if (id != 0) {");
			blw.writeLine("        line.style.marginTop = trueSpacing;");
			blw.writeLine("        input.style.marginTop = trueSpacing;");
			blw.writeLine("      }");
			blw.writeLine("    }");
			blw.writeLine("    else {");
			blw.writeLine("      line.style.backgroundColor = falseColor;");
			blw.writeLine("      line.style.marginTop = falseSpacing;");
			blw.writeLine("      input.style.marginTop = falseSpacing;");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("}");
			
			blw.writeLine("");
			
			blw.writeLine("var trueSpacing = " + this.cbfp.getTrueSpacing() + ";");
			blw.writeLine("var falseSpacing = " + this.cbfp.getFalseSpacing() + ";");
			blw.writeLine("var trueColor = '#" + FeedbackPanel.getRGB(this.cbfp.getTrueColor()) + "';");
			blw.writeLine("var falseColor = '#" + FeedbackPanel.getRGB(this.cbfp.getFalseColor()) + "';");
			
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
			blw.writeLine("  font-family: Verdana, Arial, Helvetica;");
			blw.writeLine("  font-size: 10pt;");
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
			
			for (int l = 0; l < this.cbfp.lineCount(); l++) {
				blw.writeLine("<tr>");
				
				//<p id="input0"><input type="checkbox" name="checkbox0" value="T" onclick="change(0);"></p>
				blw.writeLine("<td class=\"inputCell\">");
				
				blw.write("<p id=\"input" + l + "\">");
				blw.write("<input type=\"checkbox\" id=\"checkbox" + l + "\" value=\"T\"" + (this.cbfp.getStateAt(l) ? " checked" : "") + " onclick=\"change(" + l + ");\" />");
				blw.write("<input type=\"hidden\" name=\"state" + l + "\" id=\"state" + l + "\" value=\"" + (this.cbfp.getStateAt(l) ? "T" : "F") + "\" />");
				blw.writeLine("</p>");
				
				blw.writeLine("</td>");
				
				//<p id="line0">Test1</p>
				blw.writeLine("<td class=\"labelCell\">");
				
				blw.write("<p id=\"line" + l + "\" onclick=\"invert(" + l + ");\">");
				blw.write(prepareForHtml(this.cbfp.getLineAt(l)));
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
			for (int l = 0; l < this.cbfp.lineCount(); l++)
				this.cbfp.setStateAt(l, "T".equals(response.getProperty(("state" + l), "F")));
		}
	}
}

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

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.panels.AssignmentDisambiguationFeedbackPanel;

/**
 * @author sautter
 *
 */
public class AssignmentDisambiguationFeedbackPanelRenderer extends FeedbackPanelHtmlRenderer {
	
	/**
	 * Constructor
	 */
	public AssignmentDisambiguationFeedbackPanelRenderer() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer#canRender(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public int canRender(FeedbackPanel fp) {
		return getInheritanceDepth(AssignmentDisambiguationFeedbackPanel.class, fp.getClass());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer#getRendererInstance(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
	 */
	public FeedbackPanelHtmlRendererInstance getRendererInstance(FeedbackPanel fp) {
		return ((fp instanceof AssignmentDisambiguationFeedbackPanel) ? new AssignmentDisambiguationFeedbackPanelRendererInstance((AssignmentDisambiguationFeedbackPanel) fp) : null);
	}
	
	private static class AssignmentDisambiguationFeedbackPanelRendererInstance extends FeedbackPanelHtmlRendererInstance {
		
		private AssignmentDisambiguationFeedbackPanel adfp;
		private Properties[] optionMappings;
		
		AssignmentDisambiguationFeedbackPanelRendererInstance(AssignmentDisambiguationFeedbackPanel cfp) {
			super(cfp);
			this.adfp = cfp;
			this.optionMappings = new Properties[this.adfp.lineCount()];
			for (int l = 0; l < this.adfp.lineCount(); l++) {
				this.optionMappings[l] = new Properties();
				String[] options = this.adfp.getOptionsAt(l);
				for (int o = 0; o < options.length; o++) {
					String encodedOption = ("l" + l + "o" + o);
					this.optionMappings[l].setProperty(encodedOption, options[o]);
					this.optionMappings[l].setProperty(options[o], encodedOption);
				}
			}
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
			blw.writeLine("  text-align: " + (this.adfp.areOptionsLeading() ? "justify" : "right") + ";");
			blw.writeLine("  width: 100%;");
			blw.writeLine("}");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writeJavaScript(java.io.Writer)
		 */
		public void writeJavaScript(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			blw.writeLine("function maskMultiSelect(name) {");
			blw.writeLine("  var select = document.getElementsByName(name)[0];");
			blw.writeLine("  var optCount = 0;");
			blw.writeLine("  for (var o = 0; o < select.options.length; o++) {");
			blw.writeLine("    if (select.options[o].selected == true) {");
			blw.writeLine("      var of = getHiddenField(select.options[o].value, 'S');");
			blw.writeLine("      select.parentNode.appendChild(of);");
			blw.writeLine("    }");
			blw.writeLine("  }");
			blw.writeLine("}");
			blw.writeLine("function getHiddenField(name, value) {");
			blw.writeLine("  var i = document.createElement('input');");
			blw.writeLine("  var ta = document.createAttribute('type');");
			blw.writeLine("  ta.nodeValue = 'hidden';");
			blw.writeLine("  i.setAttributeNode(ta);");
			blw.writeLine("  var na = document.createAttribute('name');");
			blw.writeLine("  na.nodeValue = name;");
			blw.writeLine("  i.setAttributeNode(na);");
			blw.writeLine("  var va = document.createAttribute('value');");
			blw.writeLine("  va.nodeValue = value;");
			blw.writeLine("  i.setAttributeNode(va);");
			blw.writeLine("  return i;");
			blw.writeLine("}");
			for (int l = 0; l < this.adfp.lineCount(); l++)
				if (this.adfp.isMultiSelectEnabledAt(l)) {
					blw.writeLine("function multiSelectChanged" + l + "() {");
					blw.writeLine("  var select = document.getElementsByName('option" + l + "')[0];");
					blw.writeLine("  if (select.selectedIndex == -1)");
					blw.writeLine("    select.selectedIndex = selectedIndex" + l + ";");
					blw.writeLine("  else selectedIndex" + l + " = select.selectedIndex;");
					blw.writeLine("}");
				}
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writeJavaScriptPrepareSubmitFunctionBody(java.io.Writer)
		 */
		public void writeJavaScriptPrepareSubmitFunctionBody(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			for (int l = 0; l < this.adfp.lineCount(); l++) {
				if (this.adfp.isMultiSelectEnabledAt(l))
					blw.writeLine("  maskMultiSelect('option" + l + "');");
			}
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.htmlTest.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#writePanelBody(java.io.Writer)
		 */
		public void writePanelBody(Writer out) throws IOException {
			BufferedLineWriter blw = ((out instanceof BufferedLineWriter) ? ((BufferedLineWriter) out) : new BufferedLineWriter(out));
			
			blw.writeLine("<table class=\"inputTable\">");
			
			for (int l = 0; l < this.adfp.lineCount(); l++) {
				blw.writeLine("<tr>");
				
				HashSet opts = new HashSet();
				opts.addAll(Arrays.asList(this.adfp.getSelectedOptionsAt(l)));
				int fOpt = -1;
				String[] options = this.adfp.getOptionsAt(l);
				int spacing = this.adfp.getLineSpacingAt(l);
				if (spacing < 0)
					spacing = this.adfp.getLineSpacing();
				if (l == 0)
					spacing = 0;
				Color color = this.adfp.getLineColorAt(l);
				if (color == null)
					color = this.adfp.getDefaultLineColor();
				
				if (!this.adfp.areOptionsLeading()) {
					blw.writeLine("<td class=\"labelCell\" style=\"padding-top: " + spacing + "px;\">");
					blw.write("<p id=\"line" + l + "\" style=\"background-color: " + FeedbackPanel.getRGB(color) + ";\">");
					blw.write(prepareForHtml(this.adfp.getLineAt(l)));
					blw.writeLine("</p>");
					blw.writeLine("</td>");
				}
				
				blw.writeLine("<td class=\"inputCell\" style=\"padding-top: " + spacing + "px; white-space: nowrap;\">");
				blw.writeLine("<select name=\"option" + l + "\"" + (this.adfp.isMultiSelectEnabledAt(l) ? (" multiple=\"true\" size=\"" + Math.min(options.length, 1) + "\" onchange=\"multiSelectChanged" + l + "();\" onfocus=\"this.size = " + Math.min(options.length, 10) + ";\" onblur=\"this.size = " + Math.min(options.length, 1) + ";\"") : "") + ">");
				
				for (int o = 0; o < options.length; o++) {
					blw.write("<option value=\"" + this.optionMappings[l].getProperty(options[o]) + "\"" + (opts.contains(options[o].trim()) ? " selected=\"true\"" : "") + ">");
					String optRaw = options[o];
					StringBuffer opt = new StringBuffer();
					while ((optRaw.length() != 0) && (optRaw.charAt(0) < 33)) {
						opt.append("&nbsp;");
						optRaw = optRaw.substring(1);
					}
					opt.append(prepareForHtml(optRaw));
					blw.write(opt.toString());
					blw.writeLine("</option>");
					if ((fOpt == -1) && opts.contains(options[o].trim()))
						fOpt = o;
				}
				
				if (this.adfp.isMultiSelectEnabledAt(l)) {
					blw.write("</select>");
					blw.writeLine("<input type=\"button\" value=\"&#x25BC;\" style=\"width: 16px; height: 16px; padding: 0px; vertical-align: top; text-align: center;\" onclick=\"document.getElementsByName('option" + l + "')[0].focus();\">");
					blw.writeLine("<script type=\"text/javascript\">");
					blw.writeLine("var selectedIndex" + l + " = " + fOpt + ";");
					blw.writeLine("</script>");
				}
				else blw.writeLine("</select>");
				
				blw.writeLine("</td>");
	
				if (this.adfp.areOptionsLeading()) {
					blw.writeLine("<td class=\"labelCell\" style=\"padding-top: " + spacing + "px;\">");
					blw.write("<p id=\"line" + l + "\" style=\"background-color: " + FeedbackPanel.getRGB(color) + ";\">");
					blw.write(prepareForHtml(this.adfp.getLineAt(l)));
					blw.writeLine("</p>");
					blw.writeLine("</td>");
				}
				
				blw.writeLine("</tr>");
			}
			
			blw.writeLine("</table>");
			
			if (blw != out)
				blw.flush();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.html.FeedbackPanelHtmlRenderer.FeedbackPanelHtmlRendererInstance#readResponse(java.util.Properties)
		 */
		public void readResponse(Properties response) {
			for (int l = 0; l < this.adfp.lineCount(); l++) {
				if (this.adfp.isMultiSelectEnabledAt(l)) {
					ArrayList opts = new ArrayList(3);
					for (int o = 0; o < this.optionMappings[l].size(); o++) {
						String opt = ("l" + l + "o" + o);
						if ("S".equals(response.getProperty(opt)))
							opts.add(this.optionMappings[l].getProperty(opt));
					}
					this.adfp.setSelectedOptionsAt(l, ((String[]) opts.toArray(new String[opts.size()])));
				}
				else this.adfp.setSelectedOptionAt(l, this.optionMappings[l].getProperty(response.getProperty("option" + l)));
			}
		}
	}
}

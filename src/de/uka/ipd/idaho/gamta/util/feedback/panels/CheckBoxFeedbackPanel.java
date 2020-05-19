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
package de.uka.ipd.idaho.gamta.util.feedback.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;

/**
 * Feedback panel for True-False classification. This style of classification is
 * useful for tasks like sorting out false negatives from some NLP-based entity
 * recognition component, and generally for modifying binary attributes of any
 * sort of annotations.
 * 
 * @author sautter
 */
public class CheckBoxFeedbackPanel extends FeedbackPanel {
	
	/** Default spacing above an annotation with its checkbox checked, 10 pixels */
	public static final int DEFAULT_TRUE_SPACING = 10;
	int trueSpacing = DEFAULT_TRUE_SPACING;
	
	/** Default spacing above an annotation with its checkbox unchecked, 0 pixels */
	public static final int DEFAULT_FALSE_SPACING = 0;
	int falseSpacing = DEFAULT_FALSE_SPACING;
	
	/** Default highlight color for an annotation with its checkbox checked, COLOR.YELLOW */
	public static final Color DEFAULT_TRUE_COLOR = Color.YELLOW;
	Color trueColor = DEFAULT_TRUE_COLOR;
	
	/** Default highlight color for an annotation with its checkbox unchecked, COLOR.WHITE */
	public static final Color DEFAULT_FALSE_COLOR = Color.WHITE;
	Color falseColor = DEFAULT_FALSE_COLOR;
	
	/**
	 * @return the highlight color for an annotation with its checkbox checked
	 */
	public Color getTrueColor() {
		return this.trueColor;
	}

	/**
	 * @param trueColor the new highlight color for an annotation with its
	 *            checkbox checked
	 */
	public void setTrueColor(Color trueColor) {
		this.trueColor = trueColor;
		this.updateStateColor(true, this.trueColor);
	}

	/**
	 * @return the highlight color for an annotation with its checkbox unchecked
	 */
	public Color getFalseColor() {
		return this.falseColor;
	}

	/**
	 * @param falseColor the new highlight color for an annotation with its
	 *            checkbox unchecked
	 */
	public void setFalseColor(Color falseColor) {
		this.falseColor = falseColor;
		this.updateStateColor(false, this.falseColor);
	}

	/**
	 * @return the spacing above an annotation with its checkbox checked
	 */
	public int getTrueSpacing() {
		return this.trueSpacing;
	}

	/**
	 * @param trueSpacing the new spacing above an annotation with its checkbox
	 *            checked
	 */
	public void setTrueSpacing(int trueSpacing) {
		this.trueSpacing = trueSpacing;
		this.updateSpacing();
	}

	/**
	 * @return the spacing above an annotation with its checkbox unchecked
	 */
	public int getFalseSpacing() {
		return this.falseSpacing;
	}

	/**
	 * @param falseSpacing the new spacing above an annotation with its checkbox
	 *            checked
	 */
	public void setFalseSpacing(int falseSpacing) {
		this.falseSpacing = falseSpacing;
		this.updateSpacing();
	}
	
	private void updateSpacing() {
		boolean doLayout = false;
		int l = 0;
		for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
			LinePanel lp = ((LinePanel) lit.next());
			int dist = ((l++ == 0) ? 0 : (lp.checkBox.isSelected() ? this.trueSpacing : this.falseSpacing));
			if (lp.gbc.insets.top != dist) {
				lp.gbc.insets.top = dist;
				this.gbl.setConstraints(lp, lp.gbc);
				doLayout = true;
			}
		}
		
		if (doLayout)
			this.gbl.layoutContainer(this);
	}
	
	private void updateStateColor(boolean state, Color color) {
		boolean doLayout = false;
		for (int l = 0; l < this.lines.size(); l++) {
			LinePanel lp = ((LinePanel) this.lines.get(l));
			if (lp.checkBox.isSelected() == state) {
				lp.lineLabel.setBackground(color);
				doLayout = true;
			}
		}
		if (doLayout) this.repaint();
	}
	
	private LinkedList lines = new LinkedList();
	
	private GridBagLayout gbl = new GridBagLayout();
	private GridBagConstraints gbc = new GridBagConstraints();
	
	/**
	 * Constructor
	 */
	public CheckBoxFeedbackPanel() {
		this(null);
	}
	
	/**
	 * Constructor
	 * @param title
	 */
	public CheckBoxFeedbackPanel(String title) {
		super(title);
		this.setLayout(this.gbl);
		this.gbc.insets.top = 0;
		this.gbc.insets.bottom = 0;
		this.gbc.insets.left = 0;
		this.gbc.insets.right = 0;
		this.gbc.fill = GridBagConstraints.HORIZONTAL;
		this.gbc.weightx = 1;
		this.gbc.weighty = 0;
		this.gbc.gridwidth = 1;
		this.gbc.gridheight = 1;
		this.gbc.gridx = 0;
		this.gbc.gridy = 0;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getComplexity()
	 */
	public int getComplexity() {
		return (this.lines.size() * 2);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
	 */
	public int getDecisionComplexity() {
		return 2;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
	 */
	public int getDecisionCount() {
		return this.lines.size();
	}
	
	/**
	 * Append a line to classify to the feedback panel, with its checkbox
	 * initially unchecked.
	 * @param line the line of line to classify (may contain HTML markup)
	 */
	public void addLine(String line) {
		this.addLine(line, false);
	}
	
	/**
	 * Append a line of line to classify to the feedback panel.
	 * @param line the line of line to classify (may contain HTML markup)
	 * @param state the initial classification of the line to classify
	 * @throws IllegalArgumentException
	 */
	public void addLine(String line, boolean state) {
		this.gbc.gridy = this.lines.size();
		LinePanel lp = new LinePanel(line, state, this.lines.size(), this.gbl, ((GridBagConstraints) this.gbc.clone()));
		if (this.lines.size() != 0) {
			lp.gbc.insets.top = (state ? this.trueSpacing : this.falseSpacing);
		}
		this.lines.add(lp);
		this.add(lp, lp.gbc);
	}
	
	/**
	 * Retrieve the lines displayed in this feedback panel for classification.
	 * @return an array holding the contained lines
	 */
	public String[] getLines() {
		String[] lines = new String[this.lines.size()];
		int l = 0;
		for (Iterator lit = this.lines.iterator(); lit.hasNext();)
			lines[l++] = ((LinePanel) lit.next()).lineLabel.getText();
		return lines;
	}
	
	/**
	 * Retrieve the current selection status of each of the lines displayed
	 * in this feedback panel for classification.
	 * @return an array holding the classification status of each of the
	 *         contained lines
	 */
	public boolean[] getStates() {
		boolean[] states = new boolean[this.lines.size()];
		int l = 0;
		for (Iterator lit = this.lines.iterator(); lit.hasNext();)
			states[l++] = ((LinePanel) lit.next()).checkBox.isSelected();
		return states;
	}
	
	/**
	 * Retrieve the text of the index-th line in this feedback panel.
	 * @param index the index of the line to look at
	 * @return the text of the index-th line
	 */
	public String getLineAt(int index) {
		return ((LinePanel) this.lines.get(index)).lineLabel.getText();
	}
	
	/**
	 * Retrieve the current select state for the index-th line in this feedback
	 * panel.
	 * @param index the index of the line to look at
	 * @return the option selected for the index-th line
	 */
	public boolean getStateAt(int index) {
		return ((LinePanel) this.lines.get(index)).checkBox.isSelected();
	}
	
	/**
	 * Set the select state for the index-th line in this feedback panel.
	 * @param index the index of the line to set the state for
	 * @param state the new state for the index-th line
	 */
	public void setStateAt(int index, boolean state) {
		((LinePanel) this.lines.get(index)).checkBox.setSelected(state);
	}
	
	/**
	 * @return the number of lines in this feedback panel
	 */
	public int lineCount() {
		return this.lines.size();
	}
	
	private class LinePanel extends JPanel {
		String line;
		JLabel lineLabel;
		JCheckBox checkBox = new JCheckBox();
		GridBagConstraints gbc;
		LinePanel(String line, boolean selected, final int order, final GridBagLayout gbl, GridBagConstraints gbc) {
			super(new BorderLayout(), true);
			this.gbc = gbc;
			
			this.line = line;
			if ((this.line.length() < 6) || !"<html>".equals(this.line.substring(0, 6).toLowerCase()))
				line = ("<HTML>" + line + "</HTML>");
			this.lineLabel = new JLabel(line, JLabel.LEFT);
			this.lineLabel.setOpaque(true);
			this.lineLabel.setBackground(selected ? getTrueColor() : getFalseColor());
			this.lineLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					checkBox.setSelected(!checkBox.isSelected());
				}
			});
			
			this.checkBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (LinePanel.this.checkBox.isSelected()) {
						LinePanel.this.gbc.insets.top = ((order == 0) ? 0 : getTrueSpacing());
						LinePanel.this.lineLabel.setBackground(getTrueColor());
					}
					else {
						LinePanel.this.gbc.insets.top = ((order == 0) ? 0 : getFalseSpacing());
						LinePanel.this.lineLabel.setBackground(getFalseColor());
					}
					gbl.setConstraints(LinePanel.this, LinePanel.this.gbc);
					gbl.layoutContainer(CheckBoxFeedbackPanel.this);
				}
			});
			this.checkBox.setSelected(selected);
			this.add(this.checkBox, BorderLayout.WEST);
			this.add(this.lineLabel, BorderLayout.CENTER);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
	 */
	public void writeData(Writer out) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		super.writeData(bw);
		
		//	write colors
		bw.write(getRGB(this.trueColor));
		bw.newLine();
		bw.write(getRGB(this.falseColor));
		bw.newLine();
		
		//	write spacing
		bw.write("" + this.trueSpacing);
		bw.newLine();
		bw.write("" + this.falseSpacing);
		bw.newLine();
		
		//	write data
		for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
			LinePanel lp = ((LinePanel) lit.next());
			bw.write((lp.checkBox.isSelected() ? "T" : "F") + " " + URLEncoder.encode(lp.line, "UTF-8"));
			bw.newLine();
		}
		bw.newLine();
		
		//	send data
		bw.flush();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#initFields(java.io.Reader)
	 */
	public void initFields(Reader in) throws IOException {
		BufferedReader br = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
		super.initFields(br);
		
		//	read colors
		this.setTrueColor(getColor(br.readLine()));
		this.setFalseColor(getColor(br.readLine()));
		
		//	read spacing
		this.setTrueSpacing(Integer.parseInt(br.readLine()));
		this.setFalseSpacing(Integer.parseInt(br.readLine()));
		
		//	read data
		Iterator lit = this.lines.iterator();
		String line;
		while (((line = br.readLine()) != null) && (line.length() != 0)) {
			String option = line.substring(0, 1);
			line = URLDecoder.decode(line.substring(2), "UTF-8");
			
			//	transferring option status only
			if ((lit != null) && lit.hasNext())
				((LinePanel) lit.next()).checkBox.setSelected("T".equals(option));
			
			//	transferring whole content
			else {
				lit = null;
				this.addLine(line, "T".equals(option));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
	 */
	public Properties getFieldStates() {
		Properties fs = new Properties();
		for (int l = 0; l < this.lineCount(); l++)
			fs.setProperty(("state" + l), (this.getStateAt(l) ? "T" : "F"));
		return fs;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
	 */
	public void setFieldStates(Properties states) {
		for (int l = 0; l < this.lineCount(); l++)
			this.setStateAt(l, "T".equals(states.getProperty(("state" + l), "F")));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		CheckBoxFeedbackPanel cbfp = new CheckBoxFeedbackPanel();
		String[] lines = {"Test 1", "Test 2 with a somewhat longer text", "Test 3", "Test 4"};
		boolean[] selected = {false, true, false, false};
		for (int t = 0; t < lines.length; t++)
			cbfp.addLine(lines[t], selected[t]);
		
		cbfp.addButton("OK");
		cbfp.addButton("Cancel");
		
		cbfp.getFeedback();
		
		StringWriter sw = new StringWriter();
		cbfp.writeData(sw);
		System.out.println(sw.toString());
		
		CheckBoxFeedbackPanel cbfp2 = new CheckBoxFeedbackPanel();
		cbfp2.initFields(new StringReader(sw.toString()));
		cbfp2.getFeedback();
	}
}

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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;

/**
 * Feedback panel for Start-Continue-Other classification. This style of
 * classification is useful for tasks like producing a (maybe not completly
 * covering) overlay of annotations over existing ones, e.g. an overlay of
 * sections over the paragraphs of a document. In this example task, the
 * paragraphs starting a section would be classified as 'Start', those belonging
 * to the same section as the one last started as 'Continue', and those not
 * belonging to any section as 'Other'. Given these classifications, one can
 * annotate the sections from the start of every 'Start' paragraph to the end of
 * the last of the sequence of 'Continue' paragraphs following the 'Start' one.
 * 
 * @author sautter
 */
public class StartContinueOtherFeedbackPanel extends FeedbackPanel {
	
	public static final String START = "S";
	public static final String CONTINUE = "C";
	public static final String OTHER = "O";
	
	String startLabel = "Start";
	String continueLabel = "Continue";
	String otherLabel = "Other";
	
	/** Default spacing above an annotation classified as 'Start', 10 pixels */
	public static final int DEFAULT_START_SPACING = 10;
	int startSpacing = DEFAULT_START_SPACING;
	
	/** Default spacing above an annotation classified as 'Continue', and above one classified as 'Other' if the preceeding one is also classified as 'Other', 0 pixels */
	public static final int DEFAULT_CONTINUE_SPACING = 0;
	int continueSpacing = DEFAULT_CONTINUE_SPACING;
	
	/** Default spacing above an annotation classified as 'Other' if the preceeding one is not classified as 'Other', 10 pixels */
	public static final int DEFAULT_OTHER_SPACING = 10;
	int otherSpacing = DEFAULT_OTHER_SPACING;
	
	/** Default highlight color for an annotation classified as 'Start', Color.ORANGE */
	public static final Color DEFAULT_START_COLOR = Color.ORANGE;
	Color startColor = DEFAULT_START_COLOR;
	
	/** Default highlight color for an annotation classified as 'Continue', Color.YELLOW */
	public static final Color DEFAULT_CONTINUE_COLOR = Color.YELLOW;
	Color continueColor = DEFAULT_CONTINUE_COLOR;
	
	/** Default highlight color for an annotation classified as 'Other', Color.WHITE */
	public static final Color DEFAULT_OTHER_COLOR = Color.WHITE;
	Color otherColor = DEFAULT_OTHER_COLOR;
	
	/**
	 * @return Returns the startLabel.
	 */
	public String getStartLabel() {
		return this.startLabel;
	}

	/**
	 * @param startLabel The startLabel to set.
	 */
	public void setStartLabel(String startLabel) {
		this.startLabel = startLabel;
		this.updateOptionLabel(START, this.startLabel);
	}

	/**
	 * @return Returns the continueLabel.
	 */
	public String getContinueLabel() {
		return this.continueLabel;
	}

	/**
	 * @param continueLabel The continueLabel to set.
	 */
	public void setContinueLabel(String continueLabel) {
		this.continueLabel = continueLabel;
		this.updateOptionLabel(CONTINUE, this.continueLabel);
	}

	/**
	 * @return Returns the otherLabel.
	 */
	public String getOtherLabel() {
		return this.otherLabel;
	}

	/**
	 * @param otherLabel The otherLabel to set.
	 */
	public void setOtherLabel(String otherLabel) {
		this.otherLabel = otherLabel;
		this.updateOptionLabel(OTHER, this.otherLabel);
	}
	
	/**
	 * @return Returns the startSpacing.
	 */
	public int getStartSpacing() {
		return this.startSpacing;
	}

	/**
	 * @param startSpacing The startSpacing to set.
	 */
	public void setStartSpacing(int startSpacing) {
		this.startSpacing = startSpacing;
		this.updateSpacing();
	}

	/**
	 * @return Returns the continueSpacing.
	 */
	public int getContinueSpacing() {
		return this.continueSpacing;
	}

	/**
	 * @param continueSpacing The continueSpacing to set.
	 */
	public void setContinueSpacing(int continueSpacing) {
		this.continueSpacing = continueSpacing;
		this.updateSpacing();
	}

	/**
	 * @return Returns the otherSpacing.
	 */
	public int getOtherSpacing() {
		return this.otherSpacing;
	}

	/**
	 * @param otherSpacing The otherSpacing to set.
	 */
	public void setOtherSpacing(int otherSpacing) {
		this.otherSpacing = otherSpacing;
		this.updateSpacing();
	}

	/**
	 * @return Returns the startColor.
	 */
	public Color getStartColor() {
		return this.startColor;
	}

	/**
	 * @param startColor The startColor to set.
	 */
	public void setStartColor(Color startColor) {
		this.startColor = startColor;
		this.updateOptionColor(this.startLabel, this.startColor);
	}
	
	/**
	 * @return Returns the continueColor.
	 */
	public Color getContinueColor() {
		return this.continueColor;
	}

	/**
	 * @param continueColor The continueColor to set.
	 */
	public void setContinueColor(Color continueColor) {
		this.continueColor = continueColor;
		this.updateOptionColor(this.continueLabel, this.continueColor);
	}

	/**
	 * @return Returns the otherColor.
	 */
	public Color getOtherColor() {
		return this.otherColor;
	}
	
	/**
	 * @param otherColor The otherColor to set.
	 */
	public void setOtherColor(Color otherColor) {
		this.otherColor = otherColor;
		this.updateOptionColor(this.otherLabel, this.otherColor);
	}

	private String getOptionLabel(String option) {
		if (START.equals(option))
			return this.startLabel;
		else if (CONTINUE.equals(option))
			return this.continueLabel;
		else return this.otherLabel;
	}
	
	private void updateOptionLabel(String option, String optionLabel) {
		for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
			LinePanel lp = ((LinePanel) lit.next());
			((ScoComboBoxModel) lp.optionBox.getModel()).fireContentsChanged();
			if (option.equals(lp.option)) {
				lp.optionBox.setSelectedItem(optionLabel);
				lp.optionBox.repaint();
			}
		}
	}
	
	private int getSpacing(String previousOption, String option) {
		if (previousOption == null) return 0;
		else if (START.equals(option))
			return this.startSpacing;
		else if (previousOption.equals(option))
			return this.continueSpacing;
		else if (OTHER.equals(option))
			return this.otherSpacing;
		else return this.continueSpacing;
	}
	
	private void updateSpacing() {
		boolean doLayout = false;
		for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
			LinePanel lp = ((LinePanel) lit.next());
			int previousDist = this.getSpacing(((lp.previous == null) ? null : lp.previous.option), lp.option);
			if (lp.gbc.insets.top != previousDist) {
				lp.gbc.insets.top = previousDist;
				this.gbl.setConstraints(lp, lp.gbc);
				doLayout = true;
			}
		}
		
		if (doLayout)
			this.gbl.layoutContainer(this);
	}
	
	private Color getOptionColor(String option) {
		if (START.equals(option))
			return this.startColor;
		else if (CONTINUE.equals(option))
			return this.continueColor;
		else return this.otherColor;
	}
	
	private void updateOptionColor(String option, Color color) {
		boolean doLayout = false;
		for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
			LinePanel lp = ((LinePanel) lit.next());
			if (option.equals(lp.option)) {
				lp.lineLabel.setBackground(color);
				doLayout = true;
			}
		}
		if (doLayout) this.repaint();
	}
	
	private ArrayList lines = new ArrayList();
	
	private GridBagLayout gbl = new GridBagLayout();
	private GridBagConstraints gbc = new GridBagConstraints();
	
	/**
	 * Constructor
	 */
	public StartContinueOtherFeedbackPanel() {
		this(null);
	}
	
	/**
	 * Constructor
	 * @param title
	 */
	public StartContinueOtherFeedbackPanel(String title) {
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
		return (this.lines.size() * 3);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
	 */
	public int getDecisionComplexity() {
		return 3;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
	 */
	public int getDecisionCount() {
		return this.lines.size();
	}
	
	/**
	 * Append a line to classify to the feedback panel. The option will be set
	 * to 'Other' the status of the preceeding line is 'Other' or the line is
	 * the first to be added, and to 'Continue' if the status of the preceeding
	 * line is 'Start' or 'Continue'.
	 * @param line the line of text to classify (may contain HTML markup)
	 */
	public void addLine(String line) {
		this.addLine(line, null);
	}
	
	/**
	 * Append a line to classify to the feedback panel. Specifying null as the
	 * option will result in 'Other' being used if the status of the preceeding
	 * line is 'Other' or the line is the first to be added, and 'Continue' if
	 * the status of the preceeding line is 'Start' or 'Continue'. Specifying
	 * 'Continue' if the line is the first to be added or the status of the
	 * preceeding line is 'Other' will result in an IllegalArgumentException
	 * being thrown. Likewise, an option other than the START, CONTINUE, or
	 * OTHER constants will also trigger an IllegalArgumentException.
	 * @param line the line of text to classify (may contain HTML markup)
	 * @param option the initial classification of the line to classify (one of
	 *            the START, CONTINUE, or OTHER constants)
	 * @throws IllegalArgumentException
	 */
	public void addLine(String line, String option) {
		if (option == null) {
			if (this.lines.isEmpty() || OTHER.equals(((LinePanel) this.lines.get(this.lines.size() - 1)).option))
				option = OTHER;
			else option = CONTINUE;
		}
		else if (((this.lines.isEmpty() || OTHER.equals(((LinePanel) this.lines.get(this.lines.size() - 1)).option))) && CONTINUE.equals(option))
			throw new IllegalArgumentException("Cannot continue without start");
		else if (!START.equals(option) && !CONTINUE.equals(option) && !OTHER.equals(option))
			throw new IllegalArgumentException("Illegal option '" + option + "', use one of the START, CONTINUE, and OTHER constants.");
		
		this.gbc.gridy = this.lines.size();
		LinePanel lp = new LinePanel(line, option, ((GridBagConstraints) this.gbc.clone()));
		if (this.lines.size() != 0) {
			LinePanel previousLp = ((LinePanel) this.lines.get(this.lines.size() - 1));
			previousLp.next = lp;
			lp.previous = previousLp;
			lp.gbc.insets.top = this.getSpacing(previousLp.option, lp.option);
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
	 * Retrieve the current classification status of each of the lines displayed
	 * in this feedback panel for classification.
	 * @return an array holding the classification status of each of the
	 *         contained lines
	 */
	public String[] getSelectedOptions() {
		String[] options = new String[this.lines.size()];
		int l = 0;
		for (Iterator lit = this.lines.iterator(); lit.hasNext();)
			options[l++] = ((LinePanel) lit.next()).option;
		return options;
	}
	
	/**
	 * Retrieve the text of the index-th line in this feedback panel
	 * @param index the index of the line to look at
	 * @return the text of the index-th line
	 */
	public String getLineAt(int index) {
		return ((LinePanel) this.lines.get(index)).lineLabel.getText();
	}
	
	/**
	 * Retrieve the option currently selected for the index-th line in this
	 * feedback panel (one of START, CONTINUE, and OTHER)
	 * @param index the index of the line to look at
	 * @return the option selected for the index-th line
	 */
	public String getOptionAt(int index) {
		return ((LinePanel) this.lines.get(index)).option;
	}
	
	/**
	 * Set the option for the index-th line in this feedback panel. If the
	 * specified option is none of START, CONTINUE, and OTHER, this method
	 * throws an IllegalArgumentException. Furthermore, CONTINUE is only
	 * admissible if (a) index != 0, (b) the option at line index-1 if not
	 * OTHER. If any of a and b applies and CONTINUE is specified, this method
	 * also throws an IllegalArgumentException
	 * @param index the index of the line to assignd the category to
	 * @param option the category to assign to the index-th line
	 */
	public void setOptionAt(int index, String option) {
		if (CONTINUE.equals(option)) {
			if ((index == 0) || OTHER.equals(((LinePanel) this.lines.get(index - 1)).option))
				throw new IllegalArgumentException("Cannot continue without start");
		}
		else if (!START.equals(option) && !CONTINUE.equals(option) && !OTHER.equals(option))
			throw new IllegalArgumentException("Illegal option '" + option + "', use one of the START, CONTINUE, and OTHER constants.");
		
		else ((LinePanel) this.lines.get(index)).optionBox.setSelectedItem(this.getOptionLabel(option));
	}
	
	/**
	 * @return the number of lines in this feedback panel
	 */
	public int lineCount() {
		return this.lines.size();
	}
	
	private class LinePanel extends JPanel {
		String option;
		String optionLabel;
		JComboBox optionBox;
		
		String line;
		JLabel lineLabel;
		
		LinePanel previous;
		LinePanel next;
		
		GridBagConstraints gbc;
		
		LinePanel(String line, String option, GridBagConstraints gbc) {
			super(new BorderLayout(), true);
			
			this.gbc = gbc;
			
			this.line = line;
			if ((this.line.length() < 6) || !"<html>".equals(this.line.substring(0, 6).toLowerCase()))
				line = ("<HTML>" + line + "</HTML>");
			this.lineLabel = new JLabel(line, JLabel.LEFT);
			this.lineLabel.setOpaque(true);
			this.lineLabel.setBackground(getOptionColor(option));
			
			this.option = option;
			this.optionLabel = getOptionLabel(option);
			this.optionBox = new JComboBox(new ScoComboBoxModel());
			this.optionBox.setMaximumRowCount(Math.min(20, this.optionBox.getItemCount()));
			this.optionBox.setSelectedItem(this.optionLabel);
			
			this.optionBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (ie.getStateChange() == ItemEvent.SELECTED) {
						String newOptionLabel = optionBox.getSelectedItem().toString();
						if (continueLabel.equals(newOptionLabel) && ((previous == null) || OTHER.equals(previous.option))) {
							JOptionPane.showMessageDialog(LinePanel.this, "Cannot continue without start.", "Invalid Selection", JOptionPane.ERROR_MESSAGE);
							optionBox.removeItemListener(this);
							optionBox.setSelectedItem(optionLabel);
							optionBox.addItemListener(this);
						}
						else {
							optionLabel = newOptionLabel;
							doOptionChange(LinePanel.this);
							repaint();
						}
					}
				}
			});
			
			//	make sure drop-down is not expanded to text label height 
			JPanel optionBoxPanel = new JPanel(new BorderLayout(), true);
			optionBoxPanel.add(this.optionBox, BorderLayout.NORTH);
			this.add(optionBoxPanel, BorderLayout.WEST);
			
			this.add(this.lineLabel, BorderLayout.CENTER);
		}
	}
	
	private class ScoComboBoxModel extends AbstractListModel implements ComboBoxModel {
		private String selectedOption = null;
		public Object getSelectedItem() {
			return this.selectedOption;
		}
		public void setSelectedItem(Object anItem) {
			this.selectedOption = ((anItem == null) ? null : anItem.toString());
		}
		public Object getElementAt(int index) {
			switch (index) {
				case 0: return startLabel;
				case 1: return continueLabel;
				case 2: return otherLabel;
				default: return null;
			}
		}
		public int getSize() {
			return 3;
		}
		public void fireContentsChanged() {
			this.fireContentsChanged(this, 0, 2);
		}
	}
	
	private boolean inProgrammaticOptionChange = false;
	private void doOptionChange(LinePanel lp) {
		
		String oldOption = lp.option;
		String newOption;
		if (this.startLabel.equals(lp.optionLabel))
			newOption = START;
		else if (this.continueLabel.equals(lp.optionLabel))
			newOption = CONTINUE;
		else newOption = OTHER;
		
		if (oldOption.equals(newOption)) return;
		else lp.option = newOption;
		
		lp.gbc.insets.top = this.getSpacing(((lp.previous == null) ? null : lp.previous.option), lp.option);
		this.gbl.setConstraints(lp, lp.gbc);
		
		lp.lineLabel.setBackground(this.getOptionColor(lp.option));
		
		if (this.inProgrammaticOptionChange) return;
		this.inProgrammaticOptionChange = true;
		
		System.out.println("Propagating ...");
		
		while ((lp != null) && ((lp = lp.next) != null)) {
			if (START.equals(lp.option))
				lp = null;
			
			else if (OTHER.equals(oldOption) && OTHER.equals(lp.option)) {
				lp.optionBox.setSelectedItem(this.continueLabel);
				oldOption = OTHER;
				newOption = CONTINUE;
			}
			
			else if (OTHER.equals(newOption) && CONTINUE.equals(lp.option)) {
				lp.optionBox.setSelectedItem(this.otherLabel);
				oldOption = CONTINUE;
				newOption = OTHER;
			}
			
			else {
				lp.gbc.insets.top = this.getSpacing(((lp.previous == null) ? null : lp.previous.option), lp.option);
				this.gbl.setConstraints(lp, lp.gbc);
				lp = null;
			}
		}
		
		this.gbl.layoutContainer(this);
		this.inProgrammaticOptionChange = false;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
	 */
	public void writeData(Writer out) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		super.writeData(bw);
		
		//	write labels
		bw.write(URLEncoder.encode(this.startLabel, "UTF-8"));
		bw.newLine();
		bw.write(URLEncoder.encode(this.continueLabel, "UTF-8"));
		bw.newLine();
		bw.write(URLEncoder.encode(this.otherLabel, "UTF-8"));
		bw.newLine();
		
		//	write colors
		bw.write(getRGB(this.startColor));
		bw.newLine();
		bw.write(getRGB(this.continueColor));
		bw.newLine();
		bw.write(getRGB(this.otherColor));
		bw.newLine();
		
		//	write spacing
		bw.write("" + this.startSpacing);
		bw.newLine();
		bw.write("" + this.continueSpacing);
		bw.newLine();
		bw.write("" + this.otherSpacing);
		bw.newLine();
		
		//	write data
		for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
			LinePanel lp = ((LinePanel) lit.next());
			bw.write(lp.option + " " + URLEncoder.encode(lp.line, "UTF-8"));
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
		
		//	read labels
		this.setStartLabel(URLDecoder.decode(br.readLine(), "UTF-8"));
		this.setContinueLabel(URLDecoder.decode(br.readLine(), "UTF-8"));
		this.setOtherLabel(URLDecoder.decode(br.readLine(), "UTF-8"));
		
		//	read colors
		this.setStartColor(getColor(br.readLine()));
		this.setContinueColor(getColor(br.readLine()));
		this.setOtherColor(getColor(br.readLine()));
		
		//	read spacing
		this.setStartSpacing(Integer.parseInt(br.readLine()));
		this.setContinueSpacing(Integer.parseInt(br.readLine()));
		this.setOtherSpacing(Integer.parseInt(br.readLine()));
		
		//	read data
		Iterator lit = this.lines.iterator();
		String line;
		while (((line = br.readLine()) != null) && (line.length() != 0)) {
			String option = line.substring(0, 1);
			line = URLDecoder.decode(line.substring(2), "UTF-8");
			
			//	transferring option status only
			if ((lit != null) && lit.hasNext()) {
				this.inProgrammaticOptionChange = true;
				((LinePanel) lit.next()).optionBox.setSelectedItem(this.getOptionLabel(option));
				this.inProgrammaticOptionChange = false;
			}
			
			//	transferring whole content
			else {
				lit = null;
				this.addLine(line, option);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
	 */
	public Properties getFieldStates() {
		Properties fs = new Properties();
		for (int l = 0; l < this.lineCount(); l++)
			fs.setProperty(("option" + l), this.getOptionAt(l));
		return fs;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
	 */
	public void setFieldStates(Properties states) {
		for (int l = 0; l < this.lineCount(); l++)
			this.setOptionAt(l, states.getProperty(("option" + l)));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		String[] lines = {"Test 1 is becoming very long now", "Test 2", "Test 3", "Test 4"};
		String[] selectedOptions = {OTHER, START, CONTINUE, OTHER};
		StartContinueOtherFeedbackPanel scoFp = new StartContinueOtherFeedbackPanel("Original");
		for (int t = 0; t < lines.length; t++)
			scoFp.addLine(lines[t], selectedOptions[t]);
		scoFp.addButton("OK");
		scoFp.addButton("Cancel");
		
		scoFp.getFeedback();
		
		StringWriter sw = new StringWriter();
		scoFp.writeData(sw);
		System.out.println(sw.toString());
		
		StartContinueOtherFeedbackPanel scoFp2 = new StartContinueOtherFeedbackPanel();
		scoFp2.initFields(new StringReader(sw.toString()));
		scoFp2.setOptionAt(1, OTHER);
		scoFp2.getFeedback();
	}
}

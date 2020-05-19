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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.tools.FeedbackPanelHtmlTester;

/**
 * Feedback panel for selecting which of a list of data items matches another
 * given data item.
 * 
 * @author sautter
 */
public class AssignmentDisambiguationFeedbackPanel extends FeedbackPanel {
	
	/** Default spacing between two annotations, 10 pixels */
	public static final int DEFAULT_LINE_SPACING = 10;
	int lineSpacing = DEFAULT_LINE_SPACING;
	
	/** Default highlight color for annotations, Color.WHITE */
	public static final Color DEFAULT_LINE_COLOR = Color.WHITE;
	Color defaultLineColor = DEFAULT_LINE_COLOR;
	
	boolean optionsLeading = true;
	
	/**
	 * @return the options leading property
	 */
	public boolean areOptionsLeading() {
		return this.optionsLeading;
	}
	
	/**
	 * Set the options leading property. If set to true, selector drop-downs
	 * apper on the left, labels on the right. If set to false, the positions
	 * are reversed.
	 * @param optionsLeading the optionsLeading to set
	 */
	public void setOptionsLeading(boolean optionsLeading) {
		if (this.optionsLeading == optionsLeading)
			return;
		this.optionsLeading = optionsLeading;
		for (int l = 0; l < this.lines.size(); l++) {
			LinePanel lp = ((LinePanel) this.lines.get(l));
			lp.lineLabel.setHorizontalAlignment(this.optionsLeading ? JLabel.LEFT : JLabel.RIGHT);
			this.add(lp);
		}
		this.gbl.layoutContainer(this);
	}
	
	/**
	 * @return the default line background color
	 */
	public Color getDefaultLineColor() {
		return this.defaultLineColor;
	}

	/**
	 * @param dlc the new line background color
	 */
	public void setDefaultLineColor(Color dlc) {
		this.defaultLineColor = dlc;
		for (int l = 0; l < this.lines.size(); l++) {
			LinePanel lp = ((LinePanel) this.lines.get(l));
			lp.setColor(lp.color);
		}
		this.repaint();
	}
	
	/**
	 * @return the spacing between two lines
	 */
	public int getLineSpacing() {
		return this.lineSpacing;
	}

	/**
	 * Set the spacing between two lines.
	 * @param lineSpacing the new spacing between two lines
	 */
	public void setLineSpacing(int lineSpacing) {
		this.lineSpacing = lineSpacing;
		for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
			LinePanel lp = ((LinePanel) lit.next());
			int previousDist = ((lp.previous == null) ? 0 : ((lp.spacing < 0) ? this.lineSpacing : lp.spacing));
			if (lp.gbc.insets.top != previousDist) {
				lp.gbc.insets.top = previousDist;
				this.add(lp);
			}
		}
		this.gbl.layoutContainer(this);
	}
	
	private ArrayList lines = new ArrayList();
	
	private GridBagLayout gbl = new GridBagLayout();
	private GridBagConstraints gbc = new GridBagConstraints();
	
	/**
	 * Constructor
	 */
	public AssignmentDisambiguationFeedbackPanel() {
		this(null);
	}
	
	/**
	 * Constructor
	 * @param title
	 */
	public AssignmentDisambiguationFeedbackPanel(String title) {
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
		int complexity = 0;
		for (int l = 0; l < this.lines.size(); l++)
			complexity += ((LinePanel) this.lines.get(l)).optionSelector.getItemCount();
		return complexity;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
	 */
	public int getDecisionComplexity() {
		int decisionComplexity = 0;
		for (int l = 0; l < this.lines.size(); l++)
			decisionComplexity = Math.max(decisionComplexity, ((LinePanel) this.lines.get(l)).optionSelector.getItemCount());
		return decisionComplexity;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
	 */
	public int getDecisionCount() {
		return this.lines.size();
	}
	
	/**
	 * Append a line to the feedback panel. The specified array of options has
	 * to contain at least one string. Otherwise, an IllegalArgumentException
	 * will be thrown. The entries of the option array may have leading spaces,
	 * so to allow grouping it by means of indentation. These spaces are ignored
	 * when getting or setting individual options.
	 * @param line the line of text to assign one of the options to (may contain
	 *            HTML markup)
	 * @param options an array holding the available options
	 * @throws IllegalArgumentException
	 */
	public void addLine(String line, String[] options) {
		this.addLine(line, options, ((String) null), -1, null);
	}
	
	/**
	 * Append a line to the feedback panel. The specified array of options has
	 * to contain at least one string. Otherwise, an IllegalArgumentException
	 * will be thrown. The entries of the option array may have leading spaces,
	 * so to allow grouping it by means of indentation. These spaces are ignored
	 * when getting or setting individual options.
	 * @param line the line of text to assign one of the options to (may contain
	 *            HTML markup)
	 * @param options an array holding the available options
	 * @param spacing the spacing above the line
	 * @param color the background color for the line
	 * @throws IllegalArgumentException
	 */
	public void addLine(String line, String[] options, int spacing, Color color) {
		this.addLine(line, options, ((String) null), spacing, color);
	}
	
	/**
	 * Append a line to the feedback panel. The specified array of options has
	 * to contain at least one string. Otherwise, an IllegalArgumentException
	 * will be thrown. The entries of the option array may have leading spaces,
	 * so to allow grouping it by means of indentation. These spaces are ignored
	 * when getting or setting individual options.
	 * @param line the line of text to assign one of the options to (may contain
	 *            HTML markup)
	 * @param options an array holding the available options
	 * @param selectedOption the option initially selected (if this argument is null,
	 *            the first of the specified options will be used as the initial
	 *            selection)
	 * @throws IllegalArgumentException
	 */
	public void addLine(String line, String[] options, String selectedOption) {
		this.addLine(line, options, selectedOption, -1, null);
	}
	
	/**
	 * Append a line to the feedback panel. The specified array of options has
	 * to contain at least one string. Otherwise, an IllegalArgumentException
	 * will be thrown. The entries of the option array may have leading spaces,
	 * so to allow grouping it by means of indentation. These spaces are ignored
	 * when getting or setting individual options.
	 * @param line the line of text to assign one of the options to (may contain
	 *            HTML markup)
	 * @param options an array holding the available options
	 * @param selectedOptions the options initially selected (if this argument
	 *            is null, the first of the specified options will be used as
	 *            the initial selection)
	 * @throws IllegalArgumentException
	 */
	public void addLine(String line, String[] options, String[] selectedOptions) {
		this.addLine(line, options, selectedOptions, -1, null);
	}
	
	/**
	 * Append a line to the feedback panel. The specified array of options has
	 * to contain at least one string. Otherwise, an IllegalArgumentException
	 * will be thrown. The entries of the option array may have leading spaces,
	 * so to allow grouping it by means of indentation. These spaces are ignored
	 * when getting or setting individual options.
	 * @param line the line of text to assign one of the options to (may contain
	 *            HTML markup)
	 * @param options an array holding the available options
	 * @param selectedOption the option initially selected (if this argument is null,
	 *            the first of the specified options will be used as the initial
	 *            selection)
	 * @param spacing the spacing above the line
	 * @param color the background color for the line
	 * @throws IllegalArgumentException
	 */
	public void addLine(String line, String[] options, String selectedOption, int spacing, Color color) {
		String[] sOpts;
		if (selectedOption == null)
			sOpts = null;
		else {
			sOpts = new String[1];
			sOpts[0] = selectedOption;
		}
		this.doAddLine(line, options, sOpts, false, spacing, color);
	}
	
	/**
	 * Append a line to the feedback panel. The specified array of options has
	 * to contain at least one string. Otherwise, an IllegalArgumentException
	 * will be thrown. The entries of the option array may have leading spaces,
	 * so to allow grouping it by means of indentation. These spaces are ignored
	 * when getting or setting individual options.
	 * @param line the line of text to assign one of the options to (may contain
	 *            HTML markup)
	 * @param options an array holding the available options
	 * @param selectedOptions the options initially selected (if this argument is null,
	 *            the first of the specified options will be used as the initial
	 *            selection)
	 * @param spacing the spacing above the line
	 * @param color the background color for the line
	 * @throws IllegalArgumentException
	 */
	public void addLine(String line, String[] options, String[] selectedOptions, int spacing, Color color) {
		this.doAddLine(line, options, selectedOptions, true, spacing, color);
	}
	
	private void doAddLine(String line, String[] options, String[] selectedOptions, boolean mse, int spacing, Color color) {
		if (options.length < 1)
			throw new IllegalArgumentException("Please specify at least one option.");
		this.gbc.gridy = this.lines.size();
		LinePanel lp = new LinePanel(line, options, selectedOptions, mse, spacing, color, ((GridBagConstraints) this.gbc.clone()));
		if (this.lines.size() != 0) {
			LinePanel previousLp = ((LinePanel) this.lines.get(this.lines.size() - 1));
			lp.previous = previousLp;
			lp.gbc.insets.top = ((lp.spacing < 0) ? this.lineSpacing : lp.spacing);
		}
		this.lines.add(lp);
		this.add(lp);
		this.gbl.layoutContainer(this);
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
			options[l++] = ((LinePanel) lit.next()).getSelectedOption();
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
	 * @return the background color of the index-th line
	 */
	public Color getLineColorAt(int index) {
		return ((LinePanel) this.lines.get(index)).color;
	}
	
	/**
	 * Set the background color for the index-th line. Setting the color to null
	 * resets it to the panel default.
	 * @param lineColor the new background color for the index-th line
	 */
	public void setLineColorAt(int index, Color lineColor) {
		LinePanel lp = ((LinePanel) this.lines.get(index));
		lp.setColor(lineColor);
		this.repaint();
	}
	
	/**
	 * @return the spacing above the index-th line
	 */
	public int getLineSpacingAt(int index) {
		return ((LinePanel) this.lines.get(index)).spacing;
	}
	
	/**
	 * Set the spacing above the index-th line. Setting the spacing to -1 resets
	 * it to the panel default.
	 * @param lineSpacing the new spacing above the index-th line
	 */
	public void setLineSpacingAt(int index, int lineSpacing) {
		LinePanel lp = ((LinePanel) this.lines.get(index));
		lp.spacing = lineSpacing;
		int previousDist = ((lp.previous == null) ? 0 : ((lp.spacing < 0) ? this.lineSpacing : lp.spacing));
		if (lp.gbc.insets.top != previousDist) {
			lp.gbc.insets.top = previousDist;
			this.add(lp);
		}
		this.gbl.layoutContainer(this);
	}
	
	/**
	 * Retrieve the options selectable in the index-th line in this feedback
	 * panel. The array contains the original options, i.e., including leading
	 * spaces.
	 * @param index the index of the line to look at
	 * @return the options selectable in the index-th line
	 */
	public String[] getOptionsAt(int index) {
		LinePanel lp = ((LinePanel) this.lines.get(index));
		ComboBoxModel cbm = lp.optionSelector.getModel();
		String[] options = new String[cbm.getSize()];
		for (int o = 0; o < cbm.getSize(); o++)
			options[o] = cbm.getElementAt(o).toString();
		return options;
	}
	
	/**
	 * Test if multi-selection is enabled in the index-th line.
	 * @param index the index of the line to look at
	 * @return true if multi-selection is enabled in the index-th line, false
	 *         otherwise
	 */
	public boolean isMultiSelectEnabledAt(int index) {
		return ((LinePanel) this.lines.get(index)).isMultiSelectEnabled();
	}
	
	/**
	 * Enable or disable multi-selection in the index-th line.
	 * @param index the index of the line to change
	 * @param mse enable multi-selection in the index-th line?
	 */
	public void setMultiSelectEnabledAt(int index, boolean mse) {
		((LinePanel) this.lines.get(index)).setMultiSelectEnabled(mse);
	}
	
	/**
	 * Retrieve the option currently selected in the index-th line in this
	 * feedback panel. This method removes leading and tailing spaces, so to
	 * allow grouping the option array by means of indentation.
	 * @param index the index of the line to look at
	 * @return the option selected in the index-th line
	 */
	public String getSelectedOptionAt(int index) {
		return ((LinePanel) this.lines.get(index)).getSelectedOption();
	}
	
	/**
	 * Retrieve the options currently selected in the index-th line in this
	 * feedback panel. This method removes leading and tailing spaces, so to
	 * allow grouping the option array by means of indentation. If
	 * multi-selection is disable for the index-th line, the returned array
	 * always has a single element. Otherwise, it can have multiple elements,
	 * but always has at least one.
	 * @param index the index of the line to look at
	 * @return the option selected in the index-th line
	 */
	public String[] getSelectedOptionsAt(int index) {
		return ((LinePanel) this.lines.get(index)).getSelectedOptions();
	}
	
	/**
	 * Set the option assigned to the index-th line in this feedback panel. If
	 * the specified option is not present in the index-th line, nothing
	 * changes. This method ignores leading and tailing spaces, so to allow
	 * grouping the option array by means of indentation.
	 * @param index the index of the line to assignd the option to
	 * @param selectedOption the option to assign to the index-th line
	 */
	public void setSelectedOptionAt(int index, String selectedOption) {
		((LinePanel) this.lines.get(index)).setSelectedOption(selectedOption);
	}
	
	/**
	 * Set the options assigned to the index-th line in this feedback panel. If
	 * the specified option is not present in the index-th line, nothing
	 * changes. This method ignores leading and tailing spaces, so to allow
	 * grouping the option array by means of indentation.
	 * @param index the index of the line to assignd the option to
	 * @param selectedOptions the options to assign to the index-th line
	 */
	public void setSelectedOptionsAt(int index, String[] selectedOptions) {
		((LinePanel) this.lines.get(index)).setSelectedOptions(selectedOptions);
	}
	
	/**
	 * @return the number of lines in this feedback panel
	 */
	public int lineCount() {
		return this.lines.size();
	}
	
	private void add(LinePanel lp) {
		lp.gbc.gridx = (this.optionsLeading ? 0 : 1);
		lp.gbc.weightx = 0;
		this.add(lp.optionPanel, lp.gbc);
		lp.gbc.gridx = (this.optionsLeading ? 1 : 0);
		lp.gbc.weightx = 1;
		this.add(lp.lineLabel, lp.gbc);
	}
	
	private class LinePanel {
		Color color = null;
		int spacing = -1;
		
		Properties optionMappings = new Properties();
		HashMap optionIndices = new HashMap();
		JComboBox optionSelector;
		JList optionList;
		JScrollPane optionListBox;
		JPopupMenu optionListTray;
		JPanel optionListPanel;
		JLabel optionListLabel;
		JLabel optionListButton;
		JPanel optionPanel;
		boolean multiSelectEnabled = false;
		
		String line;
		JLabel lineLabel;
		
		LinePanel previous;
		
		GridBagConstraints gbc;
		
		LinePanel(String line, String[] options, String[] selectedOptions, boolean mse, int spacing, Color color, GridBagConstraints gbc) {
			
			this.gbc = gbc;
			this.spacing = spacing;
			
			this.line = line;
			if ((this.line.length() < 6) || !"<html>".equals(this.line.substring(0, 6).toLowerCase()))
				line = ("<HTML>" + line + "</HTML>");
			this.lineLabel = new JLabel(line, (optionsLeading ? JLabel.LEFT : JLabel.RIGHT));
			this.lineLabel.setOpaque(true);
			this.setColor(color);
			
			for (int o = 0; o < options.length; o++) {
				String opt = options[o].trim();
				if (opt.length() < options[o].length())
					this.optionMappings.setProperty(opt, options[o]);
				this.optionIndices.put(options[o], new Integer(o));
			}
			this.optionSelector = new JComboBox(options);
			this.optionSelector.setMaximumRowCount(Math.min(20, this.optionSelector.getItemCount()));
			
			this.optionList = new JList(options);
			this.optionList.setBorder(BorderFactory.createEmptyBorder());
			this.optionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			this.optionList.addListSelectionListener(new ListSelectionListener() {
				int si = 0;
				public void valueChanged(ListSelectionEvent lse) {
					int si = optionList.getSelectedIndex();
					if (si == -1)
						optionList.setSelectedIndex(this.si);
					else this.si = si;
				}
			});
			this.optionListBox = new JScrollPane(this.optionList);
			this.optionListBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			this.optionListBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			this.optionListBox.setBorder(BorderFactory.createEmptyBorder());
			this.optionListTray = new JPopupMenu();
			this.optionListTray.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
			this.optionListTray.setPreferredSize(new Dimension(this.optionSelector.getPreferredSize().width, Math.min((this.optionList.getPreferredSize().height + 10), 300)));
			this.optionListTray.setLayout(new BorderLayout());
			this.optionListTray.add(this.optionListBox, BorderLayout.CENTER);
			
			this.optionListLabel = new JLabel("CLICK TO CHANGE");
			this.optionListLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
			this.optionListLabel.setOpaque(true);
			this.optionListLabel.setBackground(Color.WHITE);
			this.optionListLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					optionListTray.show(optionListPanel, 0, optionListPanel.getPreferredSize().height);
				}
			});
			
			this.optionListButton = new JLabel("<html>&#x25BC;</html>", JLabel.CENTER);
			this.optionListButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.optionListButton.setPreferredSize(new Dimension(this.optionListButton.getPreferredSize().height,this.optionListButton.getPreferredSize().height));
			this.optionListButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					optionListTray.show(optionListPanel, 0, optionListPanel.getPreferredSize().height);
				}
			});
			
			this.optionListPanel = new JPanel(new BorderLayout());
			this.optionListPanel.setBorder(BorderFactory.createLoweredBevelBorder());
			this.optionListPanel.setPreferredSize(new Dimension((this.optionSelector.getPreferredSize().width + this.optionSelector.getPreferredSize().height), this.optionSelector.getPreferredSize().height));
			this.optionListPanel.add(this.optionListLabel, BorderLayout.CENTER);
			this.optionListPanel.add(this.optionListButton, BorderLayout.EAST);
			this.optionList.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent fe) {
					Object[] opts = optionList.getSelectedValues();
					optionListLabel.setText(opts[0].toString().trim() + ((opts.length == 1) ? "" : (" (" + opts.length + ")")));
					StringBuffer oltt = new StringBuffer("<html> - " + opts[0].toString().trim());
					for (int o = 1; o < opts.length; o++)
						oltt.append("<br> - " + opts[o].toString().trim());
					oltt.append("</html>");
					optionListLabel.setToolTipText(oltt.toString());
				}
			});
			
			this.optionPanel = new JPanel(new BorderLayout(), true);
			
			this.setMultiSelectEnabled(mse);
			if ((selectedOptions == null) || (selectedOptions.length == 0)) {
				selectedOptions = new String[1];
				selectedOptions[0] = options[0];
			}
			this.setSelectedOptions(selectedOptions);
		}
		void setColor(Color color) {
			this.color = color;
			this.lineLabel.setBackground((this.color == null) ? defaultLineColor : this.color);
			this.lineLabel.setBorder(BorderFactory.createMatteBorder(1, 3, 1, 3, ((this.color == null) ? defaultLineColor : this.color)));
		}
		String getSelectedOption() {
			if (this.multiSelectEnabled) {
				Object[] oOpts = this.optionList.getSelectedValues();
				return (((oOpts == null) || (oOpts.length == 0)) ? null : oOpts[0].toString().trim());
			}
			else return this.optionSelector.getSelectedItem().toString().trim();
		}
		String[] getSelectedOptions() {
			if (this.multiSelectEnabled) {
				Object[] oOpts = this.optionList.getSelectedValues();
				if (oOpts == null)
					return new String[0];
				String[] opts = new String[oOpts.length];
				for (int o = 0; o < oOpts.length; o++)
					opts[o] = oOpts[o].toString().trim();
				return opts;
			}
			else {
				String[] opts = {this.optionSelector.getSelectedItem().toString().trim()};
				return opts;
			}
		}
		void setSelectedOption(String option) {
			option = option.trim();
			String opt = this.optionMappings.getProperty(option, option);
			this.optionList.setSelectedValue(opt, true); // do not use option index (setting selected index does not scroll)
			this.optionListLabel.setText(opt.trim());
			this.optionSelector.setSelectedItem(opt);
		}
		void setSelectedOptions(String[] options) {
			if ((options == null) || (options.length == 0))
				return;
			String[] opts = new String[options.length];
			for (int o = 0; o < options.length; o++) {
				opts[o] = options[o].trim();
				opts[o] = this.optionMappings.getProperty(opts[o], opts[o]);
			}
			this.optionList.setSelectedValue(opts[0], true); // always do this to scroll to first option
			this.optionListLabel.setText(opts[0].toString().trim() + ((opts.length == 1) ? "" : (" (" + opts.length + ")")));
			if (this.multiSelectEnabled && (opts.length > 1)) {
				StringBuffer oltt = new StringBuffer("<html> - " + opts[0].toString().trim());
				
				int[] indices = new int[opts.length];
				int indexCount = 0;
				for (int o = 0; o < opts.length; o++) {
					Integer index = ((Integer) this.optionIndices.get(opts[o]));
					if (index == null)
						indices[o] = -1;
					else {
						indices[o] = index.intValue();
						indexCount++;
					}
				}
				if (indexCount < indices.length) {
					int[] cIndices = new int[indexCount];
					int ci = 0;
					for (int i = 0; i < indices.length; i++)
						if (indices[i] != -1) {
							cIndices[ci++] = indices[i];
							oltt.append("<br> - " + opts[indices[i]].toString().trim());
						}
					indices = cIndices;
				}
				if (indices.length > 1) {
					Arrays.sort(indices);
					this.optionList.setSelectedIndices(indices);
					oltt.append("</html>");
					this.optionListLabel.setToolTipText(oltt.toString());
				}
			}
			this.optionSelector.setSelectedItem(opts[0]);
		}
		boolean isMultiSelectEnabled() {
			return this.multiSelectEnabled;
		}
		void setMultiSelectEnabled(boolean mse) {
			this.multiSelectEnabled = mse;
			this.optionPanel.removeAll();
			if (this.multiSelectEnabled)
				this.optionPanel.add(this.optionListPanel, BorderLayout.NORTH);
			else this.optionPanel.add(this.optionSelector, BorderLayout.NORTH);
			validate();
			repaint();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
	 */
	public void writeData(Writer out) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		super.writeData(bw);
		
		//	write spacing and propagation
		bw.write("" + this.lineSpacing);
		bw.newLine();
		bw.write(getRGB(this.defaultLineColor));
		bw.newLine();
		bw.write(this.optionsLeading ? "L" : "T");
		bw.newLine();
		
		//	write data
		for (int l = 0; l < this.lines.size(); l++) {
			LinePanel lp = ((LinePanel) this.lines.get(l));
			bw.write(URLEncoder.encode(lp.line, "UTF-8"));
			bw.write(" " + lp.spacing);
			bw.write(" " + ((lp.color == null) ? "N" : getRGB(lp.color)));
			if (lp.isMultiSelectEnabled()) {
				bw.write(" M");
				String[] opts = lp.getSelectedOptions();
				bw.write(" " + URLEncoder.encode(opts[0], "UTF-8"));
				for (int o = 1; o < opts.length; o++)
					bw.write("&" + URLEncoder.encode(opts[o], "UTF-8"));
			}
			else {
				bw.write(" S");
				bw.write(" " + URLEncoder.encode(lp.getSelectedOption(), "UTF-8"));
			}
			String[] options = this.getOptionsAt(l);
			for (int o = 0; o < options.length; o++)
				bw.write(" " + URLEncoder.encode(options[o], "UTF-8"));
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
		
		//	read spacing and propagation
		this.setLineSpacing(Integer.parseInt(br.readLine()));
		this.setDefaultLineColor(getColor(br.readLine()));
		this.setOptionsLeading("L".equals(br.readLine()));
		
		//	read data
		Iterator lit = this.lines.iterator();
		String line;
		while (((line = br.readLine()) != null) && (line.length() != 0)) {
			String[] parts = line.split("\\s");
			line = URLDecoder.decode(parts[0], "UTF-8");
			int spacing = Integer.parseInt(parts[1]);
			String color = parts[2];
			boolean mse = "M".equals(parts[3]);
			String[] opts = parts[4].split("\\&");
			for (int o = 0; o < opts.length; o++)
				opts[o] = URLDecoder.decode(opts[o], "UTF-8");
			String[] options = new String[parts.length - 5];
			for (int o = 0; o < options.length; o++)
				options[o] = URLDecoder.decode(parts[o + 5], "UTF-8");
			
			//	transferring option status only
			if ((lit != null) && lit.hasNext()) {
				if (mse)
					((LinePanel) lit.next()).setSelectedOptions(opts);
				else ((LinePanel) lit.next()).setSelectedOption(opts[0]);
			}
			
			//	transferring whole content
			else {
				lit = null;
				if (mse)
					this.addLine(line, options, opts, spacing, ("N".equals(color) ? null : getColor(color)));
				else this.addLine(line, options, opts[0], spacing, ("N".equals(color) ? null : getColor(color)));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
	 */
	public Properties getFieldStates() {
		Properties fs = new Properties();
		for (int l = 0; l < this.lineCount(); l++) {
			if (this.isMultiSelectEnabledAt(l)) {
				String[] opts = this.getSelectedOptionsAt(l);
				for (int o = 0; o < opts.length; o++)
					fs.setProperty(("option" + l + "." + o), opts[o]);
			}
			else fs.setProperty(("option" + l), this.getSelectedOptionAt(l));
		}
		return fs;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
	 */
	public void setFieldStates(Properties states) {
		for (int l = 0; l < this.lineCount(); l++) {
			if (this.isMultiSelectEnabledAt(l)) {
				ArrayList opts = new ArrayList();
				for (int o = 0;; o++) {
					String opt = states.getProperty("option" + l + "." + o);
					if (opt == null)
						break;
					else opts.add(opt);
				}
				this.setSelectedOptionsAt(l, ((String[]) opts.toArray(new String[opts.size()])));
			}
			else this.setSelectedOptionAt(l, states.getProperty("option" + l));
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		String line = "Washington";
		String[] options = {
				"Persons:",
				"  Denzel Washington",
				"  George Washington",
				"Locations",
				"  Washington, D.C.",
				"  Washington state"
			};
		AssignmentDisambiguationFeedbackPanel adfp = new AssignmentDisambiguationFeedbackPanel("Original");
		adfp.setOptionsLeading(true);
		adfp.addLine(line, options, 20, null);
		adfp.addLine(line, options, 5, Color.GREEN);
		adfp.addLine(line, options, 30, Color.ORANGE);
		adfp.setMultiSelectEnabledAt(2, true);
		adfp.addButton("OK");
		adfp.addButton("Cancel");
		adfp.setLabel("Please select the meaning of the bold parts in following text snippets:");
		
		adfp.getFeedback();
		
		// test the feedback panel's HTML appearance and functionality
		try {
			FeedbackPanelHtmlTester.testFeedbackPanel(adfp, 0);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		StringWriter sw = new StringWriter();
		adfp.writeData(sw);
		System.out.println(sw.toString());
		
		AssignmentDisambiguationFeedbackPanel adfp2 = new AssignmentDisambiguationFeedbackPanel();
		adfp2.initFields(new StringReader(sw.toString()));
//		adfp2.setSelectedOptionAt(0, "Washington state");
		adfp2.getFeedback();
	}
}

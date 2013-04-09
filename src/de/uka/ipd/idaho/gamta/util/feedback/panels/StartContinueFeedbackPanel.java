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
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;

/**
 * Feedback panel for Start-Continue classification. This style of
 * classification is useful for tasks like producing an overlay of annotations
 * over existing ones, e.g. a completely covering overlay of sections over the
 * paragraphs of a document. In this example task, the paragraphs starting a
 * section would be classified as 'Start XY', those belonging to the same
 * section as the one last started as 'Continue'. The flexibility of the
 * categories (there can be more than one 'Start' category, i.e., different
 * values for 'XY') also allow for further classifying the sections along the
 * way, indentifying, e.g., introduction, acknowledgements, and references
 * section. Given these classifications, one can annotate the sections from the
 * start of every 'Start' paragraph to the end of the last of the sequence of
 * 'Continue' paragraphs following the 'Start' one.
 * 
 * @author sautter
 */
public class StartContinueFeedbackPanel extends FeedbackPanel {
	
	/** Default spacing between two annotations classified differently, 10 pixels */
	public static final int DEFAULT_CHANGE_SPACING = 10;
	int changeSpacing = DEFAULT_CHANGE_SPACING;
	
	/** Default label for the 'Continue' category, namely 'Continue' */
	public static final String DEFAULT_CONTINUE_LABEL = "Continue";
	String continueCategory = DEFAULT_CONTINUE_LABEL;
	
	/** Default spacing between two annotations classified equally, 0 pixels */
	public static final int DEFAULT_CONTINUE_SPACING = 0;
	int continueSpacing = DEFAULT_CONTINUE_SPACING;
	
	/** Default highlight color for annotations, Color.WHITE */
	public static final Color DEFAULT_DEFAULT_COLOR = Color.WHITE;
	Color defaultColor = DEFAULT_DEFAULT_COLOR;
	
	/**
	 * @return the default highlight color.
	 */
	public Color getDefaultColor() {
		return this.defaultColor;
	}

	/**
	 * @param defaultColor the new default highlight color
	 */
	public void setDefaultColor(Color defaultColor) {
		this.defaultColor = defaultColor;
	}
	
	/**
	 * @return the spacing between two lines with different categories
	 */
	public int getChangeSpacing() {
		return this.changeSpacing;
	}

	/**
	 * Set the spacing between two lines with different categories
	 * @param changeSpacing the new spacing between two lines with different
	 *            categories
	 */
	public void setChangeSpacing(int changeSpacing) {
		this.changeSpacing = changeSpacing;
		this.updateSpacing();
	}
	
	/**
	 * @return the spacing between two lines with equal categories
	 */
	public int getContinueSpacing() {
		return this.continueSpacing;
	}

	/**
	 * Set the spacing between two lines with equal categories
	 * @param continueSpacing the new spacing between two lines with equal
	 *            categories
	 */
	public void setContinueSpacing(int continueSpacing) {
		this.continueSpacing = continueSpacing;
		this.updateSpacing();
	}
	
	/**
	 * Retrieve the label for the 'Continue' category.
	 * @return the label for the 'Continue' category
	 */
	public String getContinueCategory() {
		return this.continueCategory;
	}
	
	/**
	 * Change the label of the category indicating that a line continues the
	 * entity the line before it belongs to.
	 * @param continueCategory the new label for the 'Continue' category
	 */
	public void setContinueCategory(String continueCategory) {
		String oldContinueCategory = this.continueCategory;
		this.continueCategory = continueCategory;
		for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
			LinePanel lp = ((LinePanel) lit.next());
			((CfpComboBoxModel) lp.categoryBox.getModel()).fireContentsChanged();
			if (oldContinueCategory.equals(lp.category)) {
				lp.categoryBox.setSelectedItem(this.continueCategory);
				lp.categoryBox.repaint();
			}
		}
	}
	
	private HashMap categoryColors = new HashMap();
	
	/**
	 * Retrieve the highlight color for some category
	 * @param category the category to retrieve the highlight color for
	 * @return the highlight color for the specified category
	 */
	public Color getCategoryColor(String category) {
		return (this.categoryColors.containsKey(category) ? ((Color) this.categoryColors.get(category)) : this.defaultColor);
	}
	
	/**
	 * Set the highlight color for some category
	 * @param category the category to set the highlight color for
	 * @param color the new highlight color for the specified category
	 */
	public void setCategoryColor(String category, Color color) {
		Color oldColor = this.getCategoryColor(category);
		if (!oldColor.equals(color)) {
			this.categoryColors.put(category, color);
			this.updateCategoryColor(category, color);
		}
	}
	
	/**
	 * Add an category to this categorization feedback panel. Adding an category
	 * more than once has no effect.
	 * @param category the category to add
	 */
	public void addCategory(String category) {
		if ((category != null) && this.categories.add(category))
			for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
				LinePanel lp = ((LinePanel) lit.next());
				((CfpComboBoxModel) lp.categoryBox.getModel()).fireContentsChanged();
				if (category.equals(lp.category)) {
					lp.categoryBox.setSelectedItem(category);
					lp.categoryBox.repaint();
				}
			}
	}
	
	/**
	 * Retrieve the categories currently available in this feedback panel.
	 * @return an array holding the categories currently available in this feedback
	 *         panel
	 */
	public String[] getCategories() {
		return ((String[]) this.categories.toArray(new String[this.categories.size()]));
	}
	
	private int getSpacing(String previousCategory, String category) {
		if (previousCategory == null) return 0;
		else return (this.continueCategory.equals(category) ? this.continueSpacing : this.changeSpacing);
	}
	
	private void updateSpacing() {
		boolean doLayout = false;
		for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
			LinePanel lp = ((LinePanel) lit.next());
			int previousDist = this.getSpacing(((lp.previous == null) ? null : lp.previous.category), lp.category);
			if (lp.gbc.insets.top != previousDist) {
				lp.gbc.insets.top = previousDist;
				this.gbl.setConstraints(lp, lp.gbc);
				doLayout = true;
			}
		}
		
		if (doLayout)
			this.gbl.layoutContainer(this);
	}
	
	private void updateCategoryColor(String category, Color color) {
		boolean doLayout = false;
		String lastCategory = null;
		for (int l = 0; l < this.lines.size(); l++) {
			LinePanel lp = ((LinePanel) this.lines.get(l));
			if (category.equals(lp.category)) {
				lp.lineLabel.setBackground(color);
				doLayout = true;
			}
			if (this.continueCategory.equals(lp.category)) {
				if (category.equals(lastCategory))
					lp.lineLabel.setBackground(brighten(color));
			}
			else lastCategory = lp.category;
		}
		if (doLayout) this.repaint();
	}
	
	private TreeSet categories = new TreeSet();
	private ArrayList lines = new ArrayList();
	
	private GridBagLayout gbl = new GridBagLayout();
	private GridBagConstraints gbc = new GridBagConstraints();
	
	/**
	 * Constructor
	 */
	public StartContinueFeedbackPanel() {
		this(null);
	}

	/**
	 * @param title
	 */
	public StartContinueFeedbackPanel(String title) {
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
		return (this.lines.size() * (this.categories.size() + 1));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
	 */
	public int getDecisionComplexity() {
		return (this.categories.size() + 1);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
	 */
	public int getDecisionCount() {
		return this.lines.size();
	}
	
	/**
	 * Append a line to classify to the feedback panel. The category for the new
	 * line will be set to the configured 'continue' category.
	 * @param line the line of text to classify (may contain HTML markup)
	 */
	public void addLine(String line) {
		this.addLine(line, null);
	}
	
	/**
	 * Append a line to classify to the feedback panel. Specifying a category
	 * not yet added through the addCategory() method will result in that
	 * category being added. Specifying null as the category will result in the
	 * category being set to the configured 'continue' category.
	 * @param line the line of text to classify (may contain HTML markup)
	 * @param category the initial classification of the line to classify
	 */
	public void addLine(String line, String category) {
		if (category == null)
			category = this.continueCategory;
		else if (!this.continueCategory.equals(category)) this.addCategory(category);
		
		this.gbc.gridy = this.lines.size();
		LinePanel lp = new LinePanel(line, category, ((GridBagConstraints) this.gbc.clone()));
		if (this.lines.size() != 0) {
			LinePanel previousLp = ((LinePanel) this.lines.get(this.lines.size() - 1));
			previousLp.next = lp;
			lp.previous = previousLp;
			lp.gbc.insets.top = this.getSpacing(previousLp.category, lp.category);
		}
		this.lines.add(lp);
		
		if (this.continueCategory.equals(lp.category) && (lp.previous != null)) {
			if (this.continueCategory.equals(lp.previous.category))
				lp.lineLabel.setBackground(lp.previous.lineLabel.getBackground());
			else lp.lineLabel.setBackground(brighten(lp.previous.lineLabel.getBackground()));
		}
		
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
	public String[] getSelectedCategories() {
		String[] categories = new String[this.lines.size()];
		int l = 0;
		for (Iterator lit = this.lines.iterator(); lit.hasNext();)
			categories[l++] = ((LinePanel) lit.next()).category;
		return categories;
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
	 * Retrieve the category currently assigned to the index-th line in this
	 * feedback panel
	 * @param index the index of the line to look at
	 * @return the category assigned to the index-th line
	 */
	public String getCategoryAt(int index) {
		return ((LinePanel) this.lines.get(index)).category;
	}
	
	/**
	 * Set the category assigned to the index-th line in this feedback panel. If
	 * the specified category is not present in the feedback panel, it is added.
	 * @param index the index of the line to assignd the category to
	 * @param category the category to assign to the index-th line
	 */
	public void setCategoryAt(int index, String category) {
		if (!this.continueCategory.equals(category))
			this.addCategory(category);
		((LinePanel) this.lines.get(index)).categoryBox.setSelectedItem(category);
	}
	
	/**
	 * @return the number of lines in this feedback panel
	 */
	public int lineCount() {
		return this.lines.size();
	}
	
	private class LinePanel extends JPanel {
		String category;
		JComboBox categoryBox;
		
		String line;
		JLabel lineLabel;
		
		LinePanel previous;
		LinePanel next;
		
		GridBagConstraints gbc;
		
		LinePanel(String line, String category, GridBagConstraints gbc) {
			super(new BorderLayout(), true);
			
			this.gbc = gbc;
			
			this.line = line;
			if ((this.line.length() < 6) || !"<html>".equals(this.line.substring(0, 6).toLowerCase()))
				line = ("<HTML>" + line + "</HTML>");
			this.lineLabel = new JLabel(line, JLabel.LEFT);
			this.lineLabel.setOpaque(true);
			this.lineLabel.setBackground(getCategoryColor(category));
			
			this.category = category;
			this.categoryBox = new JComboBox(new CfpComboBoxModel());
			this.categoryBox.setMaximumRowCount(Math.min(20, this.categoryBox.getItemCount()));
			this.categoryBox.setSelectedItem(this.category);
			
			this.categoryBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (ie.getStateChange() == ItemEvent.SELECTED) {
						String newCategory = categoryBox.getSelectedItem().toString();
						doCategoryChange(LinePanel.this, newCategory);
					}
				}
			});
			
			//	make sure drop-down is not expanded to text label height 
			JPanel categoryBoxPanel = new JPanel(new BorderLayout(), true);
			categoryBoxPanel.add(this.categoryBox, BorderLayout.NORTH);
			this.add(categoryBoxPanel, BorderLayout.WEST);
			
			this.add(this.lineLabel, BorderLayout.CENTER);
		}
	}
	
	private class CfpComboBoxModel extends AbstractListModel implements ComboBoxModel {
		private String selectedCategory = null;
		public Object getSelectedItem() {
			return this.selectedCategory;
		}
		public void setSelectedItem(Object anItem) {
			this.selectedCategory = ((anItem == null) ? null : anItem.toString());
		}
		public Object getElementAt(int index) {
			String[] categories = getCategories();
			return ((index == 0) ? continueCategory : categories[index - 1]);
		}
		public int getSize() {
			return (categories.size() + 1);
		}
		public void fireContentsChanged() {
			this.fireContentsChanged(this, 0, this.getSize());
		}
	}
	
	private void doCategoryChange(LinePanel lp, String newCategory) {
		
		String oldCategory = lp.category;
		
		if (oldCategory.equals(newCategory)) return;
		else lp.category = newCategory;
		
		lp.gbc.insets.top = this.getSpacing(((lp.previous == null) ? null : lp.previous.category), lp.category);
		this.gbl.setConstraints(lp, lp.gbc);
		
		Color categoryContinueColor;
		if (this.continueCategory.equals(lp.category)) {
			if (lp.previous == null)
				categoryContinueColor = brighten(this.defaultColor);
			else {
				if (this.continueCategory.equals(lp.previous.category))
					categoryContinueColor = lp.previous.lineLabel.getBackground();
				else categoryContinueColor = brighten(lp.previous.lineLabel.getBackground());
			}
			lp.lineLabel.setBackground(categoryContinueColor);
		}
		else {
			Color categoryColor = this.getCategoryColor(lp.category);
			lp.lineLabel.setBackground(categoryColor);
			categoryContinueColor = brighten(categoryColor);
		}
		
		while ((lp != null) && ((lp = lp.next) != null)) {
			if (this.continueCategory.equals(lp.category))
				lp.lineLabel.setBackground(categoryContinueColor);
			
			else lp = null;
		}
		
		if ((lp != null) && (lp.next != null)) {
			lp.next.gbc.insets.top = this.getSpacing(lp.category, lp.next.category);
			this.gbl.setConstraints(lp.next, lp.next.gbc);
		}
		
		this.gbl.layoutContainer(this);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
	 */
	public void writeData(Writer out) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		super.writeData(bw);
		
		//	write spacing and propagation
		bw.write("" + this.changeSpacing);
		bw.newLine();
		bw.write("" + this.continueSpacing);
		bw.newLine();
		bw.write(URLEncoder.encode(this.continueCategory, "UTF-8"));
		bw.newLine();
		
		//	get categories
		String[] categories = this.getCategories();
		
		//	write categories and colors (colors first, for fix length)
		for (int o = 0; o < categories.length; o++) {
			bw.write(getRGB(this.getCategoryColor(categories[o])) + " " + URLEncoder.encode(categories[o], "UTF-8"));
			bw.newLine();
		}
		bw.newLine();
		
		//	write data
		for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
			LinePanel lp = ((LinePanel) lit.next());
			bw.write(URLEncoder.encode(lp.category, "UTF-8") + " " + URLEncoder.encode(lp.line, "UTF-8"));
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
		this.setChangeSpacing(Integer.parseInt(br.readLine()));
		this.setContinueSpacing(Integer.parseInt(br.readLine()));
		this.setContinueCategory(URLDecoder.decode(br.readLine(), "UTF-8"));
		
		//	read categories and colors (colors first, for fix length)
		String category;
		while (((category = br.readLine()) != null) && (category.length() != 0)) {
			Color categoryColor = getColor(category.substring(0, 6));
			category = URLDecoder.decode(category.substring(7), "UTF-8");
			this.addCategory(category);
			this.setCategoryColor(category, categoryColor);
		}
		
		//	read data
		Iterator lit = this.lines.iterator();
		String line;
		while (((line = br.readLine()) != null) && (line.length() != 0)) {
			int split = line.indexOf(' ');
			category = URLDecoder.decode(line.substring(0, split), "UTF-8");
			line = URLDecoder.decode(line.substring(split + 1), "UTF-8");
			
			//	transferring category status only
			if ((lit != null) && lit.hasNext())
				((LinePanel) lit.next()).categoryBox.setSelectedItem(category);
			
			//	transferring whole content
			else {
				lit = null;
				this.addLine(line, category);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
	 */
	public Properties getFieldStates() {
		Properties fs = new Properties();
		for (int l = 0; l < this.lineCount(); l++)
			fs.setProperty(("category" + l), this.getCategoryAt(l));
		return fs;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
	 */
	public void setFieldStates(Properties states) {
		for (int l = 0; l < this.lineCount(); l++)
			this.setCategoryAt(l, states.getProperty(("category" + l)));
	}
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws Exception {
//		//	set platform L&F
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {}
//		
//		String[] lines = {"<HTML><IMG alt=\"Test 1\" src=\"http://plazi2.cs.umb.edu/GgServer/plaziNaviBooks.gif\"></HTML>", "Test 2", "Test 3", "Test 4"};
//		String[] selectedCategories = {"A", "B", null, "C"};
//		StartContinueFeedbackPanel scFp = new StartContinueFeedbackPanel("Original");
//		for (int t = 0; t < lines.length; t++)
//			scFp.addLine(lines[t], selectedCategories[t]);
//		scFp.addButton("OK");
//		scFp.addButton("Cancel");
//		scFp.setLabel("Please select the categories for the following paragraphs:");
//		scFp.setCategoryColor("A", Color.GREEN);
//		scFp.setCategoryColor("B", Color.RED);
//		scFp.setCategoryColor("C", Color.YELLOW);
//		
////		scFp.getFeedback();
//		FeedbackPanelHtmlTester.testFeedbackPanel(scFp, 0);
//		
//		StringWriter sw = new StringWriter();
//		scFp.writeData(sw);
//		System.out.println(sw.toString());
//		
//		StartContinueFeedbackPanel scFp2 = new StartContinueFeedbackPanel();
//		scFp2.initFields(new StringReader(sw.toString()));
//		scFp2.setCategoryAt(1, "E");
//		scFp2.addCategory("D");
//		scFp2.getFeedback();
//	}
}

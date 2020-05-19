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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;

/**
 * Feedback panel for classification in custom categories. This style of
 * classification is useful for tasks like itentifying caption and footnote
 * paragraphs in the middle of the main text of a document.
 * 
 * @author sautter
 */
public class CategorizationFeedbackPanel extends FeedbackPanel {
	
	//	TODO facilitate selecting multiple options per line (mutually exclusive with update propagation)
	
	/** Default spacing between two annotations classified differently, 10 pixels */
	public static final int DEFAULT_CHANGE_SPACING = 10;
	int changeSpacing = DEFAULT_CHANGE_SPACING;
	
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
	 * Switch on or off propagation of category changes. If switched on,
	 * changing the category of some line to a new value will change the
	 * category of all subsequent lines with the same value to the new value as
	 * well.
	 * @param pcc propagate category changes?
	 */
	public void setPropagateCategoryChanges(boolean pcc) {
		this.propagateCategoryChanges = pcc;
	}
	
	/**
	 * Check whether the propagation of category changes is switched on or off.
	 * By default, this option is switched on.
	 * @return true if and only iff category changes are propagated
	 */
	public boolean isPropagingCategoryChanges() {
		return this.propagateCategoryChanges;
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
		else return (previousCategory.equals(category) ? this.continueSpacing : this.changeSpacing);
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
		for (int l = 0; l < this.lines.size(); l++) {
			LinePanel lp = ((LinePanel) this.lines.get(l));
			if (category.equals(lp.category)) {
				lp.lineLabel.setBackground(color);
				doLayout = true;
			}
		}
		if (doLayout) this.repaint();
	}
	
	private TreeSet categories = new TreeSet();
	private boolean propagateCategoryChanges = true;
	private ArrayList lines = new ArrayList();
	
	private GridBagLayout gbl = new GridBagLayout();
	private GridBagConstraints gbc = new GridBagConstraints();
	
	/**
	 * Constructor
	 */
	public CategorizationFeedbackPanel() {
		this(null);
	}

	/**
	 * @param title
	 */
	public CategorizationFeedbackPanel(String title) {
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
		return (this.lines.size() * this.categories.size());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
	 */
	public int getDecisionComplexity() {
		return this.categories.size();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
	 */
	public int getDecisionCount() {
		return this.lines.size();
	}
	
	/**
	 * Append a line to classify to the feedback panel. The category for the new
	 * line will be the same as for the previous line. If the line is the first
	 * to be added, however, the first category of the ones added so far will be
	 * used. If there are no category so far, an IllegalArgumentException will be
	 * thrown.
	 * @param line the line of text to classify (may contain HTML markup)
	 */
	public void addLine(String line) {
		this.addLine(line, null);
	}
	
	/**
	 * Append a line to classify to the feedback panel. Specifying a category
	 * not yet added through the addCategory() method will result in that
	 * category being added. Specifying null as the category will result in the
	 * category of the previous line being copied. If the line is the first to
	 * be added, however, the first category of the ones added so far will be
	 * used. If there are no category so far, an IllegalArgumentException will
	 * be thrown.
	 * @param line the line of text to classify (may contain HTML markup)
	 * @param category the initial classification of the line to classify
	 * @throws IllegalArgumentException
	 */
	public void addLine(String line, String category) {
		if (category == null) {
			if (this.lines.isEmpty()) {
				Iterator oit = this.categories.iterator();
				if (oit.hasNext())
					category = oit.next().toString();
				else throw new IllegalArgumentException("Cannot use wildcard, no category to copy");
			}
			else category = ((LinePanel) this.lines.get(this.lines.size() - 1)).category;
		}
		this.addCategory(category);
		
		this.gbc.gridy = this.lines.size();
		LinePanel lp = new LinePanel(line, category, ((GridBagConstraints) this.gbc.clone()));
		if (this.lines.size() != 0) {
			LinePanel previousLp = ((LinePanel) this.lines.get(this.lines.size() - 1));
			previousLp.next = lp;
			lp.previous = previousLp;
			lp.gbc.insets.top = this.getSpacing(previousLp.category, lp.category);
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
	 * If category change propagation is activ, the change will be propagated as
	 * described above. To prevent this, switch off category change propagation
	 * prior to invoking this method, and switch it back on afterward if it was
	 * active before.
	 * @param index the index of the line to assignd the category to
	 * @param category the category to assign to the index-th line
	 */
	public void setCategoryAt(int index, String category) {
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
			return getCategories()[index];
		}
		public int getSize() {
			return categories.size();
		}
		public void fireContentsChanged() {
			this.fireContentsChanged(this, 0, this.getSize());
		}
	}
	
	private boolean inProgrammaticCategoryChange = false;
	private void doCategoryChange(LinePanel lp, String newCategory) {
		
		String oldCategory = lp.category;
		
		if (oldCategory.equals(newCategory)) return;
		else lp.category = newCategory;
		
		lp.gbc.insets.top = this.getSpacing(((lp.previous == null) ? null : lp.previous.category), lp.category);
		this.gbl.setConstraints(lp, lp.gbc);
		
		lp.lineLabel.setBackground(this.getCategoryColor(lp.category));
		
		if (this.inProgrammaticCategoryChange) return;
		
		else if (this.propagateCategoryChanges) {
			this.inProgrammaticCategoryChange = true;
			
			System.out.println("Propagating ...");
			
			while ((lp != null) && ((lp = lp.next) != null)) {
				if (oldCategory.equals(lp.category))
					lp.categoryBox.setSelectedItem(newCategory);
				
				else {
					lp.gbc.insets.top = this.getSpacing(((lp.previous == null) ? null : lp.previous.category), lp.category);
					this.gbl.setConstraints(lp, lp.gbc);
					lp = null;
				}
			}
			
			this.inProgrammaticCategoryChange = false;
		}
		
		else if (lp.next != null) {
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
		bw.write("" + this.propagateCategoryChanges);
		bw.newLine();
		
		//	get categories
		String[] categories = this.getCategories();
		
		//	write categories and colors (colors first, for fix length)
		for (int c = 0; c < categories.length; c++) {
			bw.write(getRGB(this.getCategoryColor(categories[c])) + " " + URLEncoder.encode(categories[c], "UTF-8"));
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
		this.setPropagateCategoryChanges(Boolean.parseBoolean(br.readLine()));
		
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
			if ((lit != null) && lit.hasNext()) {
				this.inProgrammaticCategoryChange = true;
				((LinePanel) lit.next()).categoryBox.setSelectedItem(category);
				this.inProgrammaticCategoryChange = false;
			}
			
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		String[] lines = {"Test 1", "Test 2", "Test 3", "Test 4"};
		String[] selectedCategories = {"A", "B", null, "C"};
		CategorizationFeedbackPanel cfp = new CategorizationFeedbackPanel("Original");
		for (int t = 0; t < lines.length; t++)
			cfp.addLine(lines[t], selectedCategories[t]);
		cfp.addButton("OK");
		cfp.addButton("Cancel");
		cfp.setLabel("Please select the categories for the following paragraphs:");
		
//		cfp.setPropagateCategoryChanges(false);
//		cfp.setProperty("test", "test this");
		cfp.getFeedback();
		
		StringWriter sw = new StringWriter();
		cfp.writeData(sw);
		System.out.println(sw.toString());
		
		CategorizationFeedbackPanel cfp2 = new CategorizationFeedbackPanel();
		cfp2.initFields(new StringReader(sw.toString()));
		cfp2.setCategoryAt(1, "E");
		cfp2.addCategory("D");
		cfp2.getFeedback();
	}
}

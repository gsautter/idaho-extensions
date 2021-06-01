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
package de.uka.ipd.idaho.plugins.bibRefs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefTypeSystem.BibRefType;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.AuthorData;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * Editor panel for bibliographic references, specifically for manual data
 * input.
 * 
 * @author sautter
 */
public class BibRefEditorPanel extends JPanel implements BibRefConstants {
	private BibRefTypeSystem typeSystem;
	private JComboBox typeSelector;
	private BibRefEditorField primaryIdField;
	private BibRefEditorField[] fields;
	private AuthorsEditorPanel authorsEditor = null;
	
	/**
	 * Constructor
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 */
	public BibRefEditorPanel(String[] idTypes) {
		this(null, idTypes, null, null);
	}
	
	/**
	 * Constructor
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 * @param authorDetails the names of the author detail attributes to make
	 *            available for editing
	 */
	public BibRefEditorPanel(String[] idTypes, String[] authorDetails) {
		this(null, idTypes, authorDetails, null);
	}
	
	/** Constructor
	 * @param typeSystem the reference type system to use for data validation
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 */
	public BibRefEditorPanel(BibRefTypeSystem typeSystem, String[] idTypes) {
		this(typeSystem, idTypes, null, null);
	}
	
	/** Constructor
	 * @param typeSystem the reference type system to use for data validation
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 * @param authorDetails the names of the author detail attributes to make
	 *            available for editing
	 */
	public BibRefEditorPanel(BibRefTypeSystem typeSystem, String[] idTypes, String[] authorDetails) {
		this(typeSystem, idTypes, authorDetails, null);
	}
	
	/** Constructor
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 * @param ref the reference data set to display initially
	 */
	public BibRefEditorPanel(RefData ref, String[] idTypes) {
		this(null, idTypes, null, ref);
	}
	
	/** Constructor
	 * @param ref the reference data set to display initially
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 * @param authorDetails the names of the author detail attributes to make
	 *            available for editing
	 */
	public BibRefEditorPanel(RefData ref, String[] idTypes, String[] authorDetails) {
		this(null, idTypes, authorDetails, ref);
	}
	
	/** Constructor
	 * @param typeSystem the reference type system to use for data validation
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 * @param ref the reference data set to display initially
	 */
	public BibRefEditorPanel(BibRefTypeSystem typeSystem, String[] idTypes, RefData ref) {
		this(typeSystem, idTypes, null, ref);
	}
	
	/** Constructor
	 * @param typeSystem the reference type system to use for data validation
	 * @param idTypes the identifier types to offer input fields for (the first
	 *            type becomes the primary ID, set it to null to make the
	 *            primary ID optional)
	 * @param authorDetails the names of the author detail attributes to make
	 *            available for editing
	 * @param ref the reference data set to display initially
	 */
	public BibRefEditorPanel(BibRefTypeSystem typeSystem, String[] idTypes, String[] authorDetails, RefData ref) {
		super(new GridBagLayout(), true);
		
		this.typeSystem = ((typeSystem == null) ? BibRefTypeSystem.getDefaultInstance() : typeSystem);
		
		BibRefType[] brts = this.typeSystem.getBibRefTypes();
		SelectableBibRefType[] sbrts = new SelectableBibRefType[brts.length];
		for (int t = 0; t < brts.length; t++)
			sbrts[t] = new SelectableBibRefType(brts[t]);
		
		JButton editAuthorsButton = null;
		if ((authorDetails != null) && (authorDetails.length == 0)) {
			if (ref == null)
				authorDetails = null; // nothing to work with
			else authorDetails = ref.getAuthorAttributeNames(); // use what's there
		}
		if ((authorDetails != null) && (authorDetails.length == 1) && AuthorData.AUTHOR_NAME_ATTRIBUTE.equals(authorDetails[0]))
			authorDetails = null; // nothing to work with but vanilla names
		if (authorDetails != null) {
			editAuthorsButton = new JButton("Edit Authors");
			editAuthorsButton.setBorder(BorderFactory.createRaisedBevelBorder());
			editAuthorsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editAuthors();
				}
			});
		}
		
		ArrayList fieldList = new ArrayList();
		if ((idTypes != null) && (idTypes.length != 0) && (idTypes[0] != null) && (idTypes[0].trim().length() != 0)) {
			String idType = idTypes[0].trim();
			this.primaryIdField = new BibRefEditorFieldSV(("ID-" + idType), (idType + " Identifier")) {
				boolean validateValueInput() {
					String val = this.valueInput.getText();
					return ((val != null) && (val.trim().length() != 0));
				}
				void showError() {
					this.markError(!this.validateValueInput());
				}
			};
			this.primaryIdField.setRequired();
		}
		for (int f = 0; f < BibRefEditor.fieldNames.length; f++) {
//			if (AUTHOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f]) || EDITOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f]))
//				fieldList.add(new BibRefEditorFieldSFMV(BibRefEditor.fieldNames[f], (StringUtils.capitalize(BibRefEditor.fieldNames[f]) + "s (use '&' to separate)"), "&"));
			if (AUTHOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				BibRefEditorFieldSFMV authorsField = new BibRefEditorFieldSFMV(BibRefEditor.fieldNames[f], (StringUtils.capitalize(BibRefEditor.fieldNames[f]) + "s (use '&' to separate)"), "&");
				if (editAuthorsButton != null)
					authorsField.valueInput.setEditable(false);
				fieldList.add(authorsField);
			}
			else if (EDITOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f]))
				fieldList.add(new BibRefEditorFieldSFMV(BibRefEditor.fieldNames[f], (StringUtils.capitalize(BibRefEditor.fieldNames[f]) + "s (use '&' to separate)"), "&"));
			else if (PART_DESIGNATOR_ANNOTATION_TYPE.equals(BibRefEditor.fieldNames[f])) {
				String[] pdTypes = {
					VOLUME_DESIGNATOR_ANNOTATION_TYPE,
					ISSUE_DESIGNATOR_ANNOTATION_TYPE,
					NUMERO_DESIGNATOR_ANNOTATION_TYPE
				};
				fieldList.add(new BibRefEditorFieldMF(BibRefEditor.fieldNames[f], "Part Designators", pdTypes, pdTypes));
			}
			else fieldList.add(new BibRefEditorFieldSV(BibRefEditor.fieldNames[f], BibRefEditor.getFieldLabel(BibRefEditor.fieldNames[f])));
		}
		if (idTypes != null) {
			for (int t = 1; t < idTypes.length; t++) {
				if (idTypes[t] == null)
					continue;
				String idType = idTypes[t].trim();
				if (idType.length() == 0)
					continue;
				fieldList.add(new BibRefEditorFieldSV(("ID-" + idType), (idType + " Identifier")));
			}
		}
		this.fields = ((BibRefEditorField[]) fieldList.toArray(new BibRefEditorField[fieldList.size()]));
		this.typeSelector = new JComboBox(sbrts);
		this.typeSelector.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ie) {
				if (ie.getStateChange() != ItemEvent.SELECTED)
					return;
				if (primaryIdField != null) {
					primaryIdField.setBibRefType(((SelectableBibRefType) ie.getItem()).brt);
					primaryIdField.setRequired();
				}
				for (int f = 0; f < fields.length; f++) {
					if (AUTHOR_ANNOTATION_TYPE.equals(fields[f].name) && (authorsEditor != null))
						authorsEditor.setBibRefType(((SelectableBibRefType) ie.getItem()).brt);
					else fields[f].setBibRefType(((SelectableBibRefType) ie.getItem()).brt);
				}
			}
		});
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets.left = 5;
		gbc.insets.right = 5;
		gbc.insets.top = 2;
		gbc.insets.bottom = 2;
		
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		this.add(new JLabel("Publication Type:", JLabel.RIGHT), gbc.clone());
		if (this.primaryIdField == null) {
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 5;
			this.add(this.typeSelector, gbc.clone());
		}
		else {
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 2;
			this.add(this.typeSelector, gbc.clone());
			gbc.gridx = 3;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(this.primaryIdField.label, gbc.clone());
			gbc.gridx = 4;
			gbc.weightx = 1;
			gbc.gridwidth = 2;
			this.add(this.primaryIdField.getValueInput(), gbc.clone());
		}
		gbc.gridy++;
		
		for (int f = 0; f < this.fields.length; f++) {
			if (((f+2) < this.fields.length) && YEAR_ANNOTATION_TYPE.equals(this.fields[f].name) && PUBLICATION_DATE_ANNOTATION_TYPE.equals(this.fields[f+1].name) && PAGINATION_ANNOTATION_TYPE.equals(this.fields[f+2].name)) {
				gbc.gridx = 0;
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				this.add(this.fields[f].label, gbc.clone());
				gbc.gridx = 1;
				gbc.gridwidth = 1;
				gbc.weightx = 1;
				this.add(this.fields[f].getValueInput(), gbc.clone());
				f++;
				gbc.gridx = 2;
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				this.add(this.fields[f].label, gbc.clone());
				gbc.gridx = 3;
				gbc.gridwidth = 1;
				gbc.weightx = 1;
				this.add(this.fields[f].getValueInput(), gbc.clone());
				f++;
				gbc.gridx = 4;
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				this.add(this.fields[f].label, gbc.clone());
				gbc.gridx = 5;
				gbc.gridwidth = 1;
				gbc.weightx = 1;
				this.add(this.fields[f].getValueInput(), gbc.clone());
			}
			else if (((f+1) < this.fields.length) && YEAR_ANNOTATION_TYPE.equals(this.fields[f].name) && PAGINATION_ANNOTATION_TYPE.equals(this.fields[f+1].name)) {
				gbc.gridx = 0;
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				this.add(this.fields[f].label, gbc.clone());
				gbc.gridx = 1;
				gbc.gridwidth = 2;
				gbc.weightx = 1;
				this.add(this.fields[f].getValueInput(), gbc.clone());
				f++;
				gbc.gridx = 3;
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				this.add(this.fields[f].label, gbc.clone());
				gbc.gridx = 4;
				gbc.gridwidth = 2;
				gbc.weightx = 1;
				this.add(this.fields[f].getValueInput(), gbc.clone());
			}
			else if (AUTHOR_ANNOTATION_TYPE.equals(this.fields[f].name) && (editAuthorsButton != null)) {
				gbc.gridx = 0;
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				this.add(this.fields[f].label, gbc.clone());
				gbc.gridx = 1;
				gbc.gridwidth = 4;
				gbc.weightx = 1;
				this.add(this.fields[f].getValueInput(), gbc.clone());
				gbc.gridx = 5;
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				this.add(editAuthorsButton, gbc.clone());
				this.authorsEditor = new AuthorsEditorPanel(authorDetails, this.fields[f]);
			}
			else if (((f+1) < this.fields.length) && JOURNAL_NAME_ANNOTATION_TYPE.equals(this.fields[f].name) && SERIES_IN_JOURNAL_ANNOTATION_TYPE.equals(this.fields[f+1].name)) {
				gbc.gridx = 0;
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				this.add(this.fields[f].label, gbc.clone());
				gbc.gridx = 1;
				gbc.gridwidth = 3;
				gbc.weightx = 1;
				this.add(this.fields[f].getValueInput(), gbc.clone());
				f++;
				gbc.gridx = 4;
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				this.add(this.fields[f].label, gbc.clone());
				gbc.gridx = 5;
				gbc.gridwidth = 1;
				gbc.weightx = 1;
				this.add(this.fields[f].getValueInput(), gbc.clone());
			}
			else {
				gbc.gridx = 0;
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				this.add(this.fields[f].label, gbc.clone());
				gbc.gridx = 1;
				gbc.gridwidth = 5;
				gbc.weightx = 1;
				this.add(this.fields[f].getValueInput(), gbc.clone());
			}
			gbc.gridy++;
		}
		
		if (this.primaryIdField != null) {
			this.primaryIdField.setBibRefType(brts[0]);
			this.primaryIdField.setRequired();
		}
		for (int f = 0; f < this.fields.length; f++) {
			if (AUTHOR_ANNOTATION_TYPE.equals(this.fields[f].name) && (this.authorsEditor != null))
				this.authorsEditor.setBibRefType(brts[0]);
			else this.fields[f].setBibRefType(brts[0]);
		}
		
		if (ref != null)
			this.setRefData(ref);
	}
	
	void editAuthors() {
		if (this.authorsEditor != null)
			this.authorsEditor.open();
	}
	
	private static class AuthorsEditorPanel extends JPanel {
		JDialog dialog;
		BibRefType brt;
		String[] details;
		BibRefEditorField authorsField;
		JTabbedPane authorTabs = new JTabbedPane();
		AuthorsEditorPanel(String[] details, BibRefEditorField authorsField) {
			super(new BorderLayout(), true);
			this.details = checkDetails(details);
			this.authorsField = authorsField;
			this.authorTabs.setTabPlacement(JTabbedPane.LEFT);
			
			JButton addAuthor = new JButton("Add");
			addAuthor.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					addAuthor();
				}
			});
			JButton removeAuthor = new JButton("Remove");
			removeAuthor.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					removeAuthor();
				}
			});
			JButton moveAuthorUp = new JButton("Move Up");
			moveAuthorUp.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					moveAuthor(-1);
				}
			});
			JButton moveAuthorDown = new JButton("Move Down");
			moveAuthorDown.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					moveAuthor(1);
				}
			});
			
			JButton close = new JButton("Close");
			close.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					close();
				}
			});
			
			JPanel buttonPanel = new JPanel(new FlowLayout());
			buttonPanel.add(addAuthor);
			buttonPanel.add(moveAuthorUp);
			buttonPanel.add(moveAuthorDown);
			buttonPanel.add(removeAuthor);
			buttonPanel.add(close);
			
			this.add(this.authorTabs, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
		}
		private static String[] checkDetails(String[] details) {
			ArrayList detailList = new ArrayList(Arrays.asList(details));
			detailList.remove(AuthorData.AUTHOR_NAME_ATTRIBUTE);
			detailList.add(0, AuthorData.AUTHOR_NAME_ATTRIBUTE);
			if (detailList.contains(AuthorData.AUTHOR_AFFILIATION_ATTRIBUTE)) {
				detailList.remove(AuthorData.AUTHOR_AFFILIATION_ATTRIBUTE);
				detailList.add(1, AuthorData.AUTHOR_AFFILIATION_ATTRIBUTE);
			}
			return ((String[]) detailList.toArray(new String[detailList.size()]));
		}
		void addAuthor() {
			final AuthorDataEditorPanel authorTab = new AuthorDataEditorPanel(this, null, true);
			this.authorTabs.addTab("", authorTab);
			this.authorTabs.setSelectedComponent(authorTab);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					authorTab.nameField.valueInput.requestFocusInWindow();
				}
			});
		}
		void removeAuthor() {
			int index = this.authorTabs.getSelectedIndex();
			if (index == -1)
				return;
			this.authorTabs.removeTabAt(index);
		}
		void moveAuthor(int diff) {
			int index = this.authorTabs.getSelectedIndex();
			if (index == -1)
				return;
			if ((index == 0) && (diff < 0))
				return; // no moving up first author
			if (((index+1) == this.authorTabs.getTabCount()) && (diff > 0))
				return; // no moving down last author
			Component authorTab = this.authorTabs.getComponentAt(index);
			String authorTitle = this.authorTabs.getTitleAt(index);
			String authorToolTip = this.authorTabs.getToolTipTextAt(index);
			this.authorTabs.removeTabAt(index);
			this.authorTabs.insertTab(authorTitle, null, authorTab, authorToolTip, (index + diff));
			this.authorTabs.setSelectedIndex(index + diff);
		}
		void updateAuthorName(AuthorDataEditorPanel adep) {
			int index = this.authorTabs.getSelectedIndex();
			if (index == -1)
				return;
			this.authorTabs.setTitleAt(index, adep.nameField.valueInput.getText());
		}
		private Dimension dialogSize = new Dimension(500, 200);
		void open() {
			if (this.dialog != null)
				return;
			this.dialog = DialogFactory.produceDialog("Edit Authors", true);
			this.dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			this.dialog.setSize(this.dialogSize);
			this.dialog.setResizable(true);
			this.dialog.getContentPane().setLayout(new BorderLayout());
			this.dialog.getContentPane().add(this, BorderLayout.CENTER);
			this.dialog.setLocationRelativeTo(this.dialog.getOwner());
			final AuthorDataEditorPanel firstAuthorTab;
			if (this.authorTabs.getTabCount() == 0) {
				firstAuthorTab = new AuthorDataEditorPanel(this, null, true);
				this.authorTabs.addTab("", firstAuthorTab);
			}
			else firstAuthorTab = ((AuthorDataEditorPanel) this.authorTabs.getComponentAt(0));
			this.authorTabs.setSelectedComponent(firstAuthorTab);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					firstAuthorTab.nameField.valueInput.requestFocusInWindow();
				}
			});
			this.dialog.setVisible(true);
		}
		void close() {
			if (this.dialog == null)
				return;
			this.dialogSize = this.dialog.getSize();
			for (int a = 0; a < this.authorTabs.getTabCount(); a++) {
				AuthorDataEditorPanel authorTab = ((AuthorDataEditorPanel) this.authorTabs.getComponentAt(a));
				if (authorTab.nameField.checkError())
					this.authorTabs.removeTabAt(a--);
			}
			RefData ref = new RefData();
			this.fillRefData(ref);
			this.authorsField.fillValueInput(ref);
			this.authorsField.showError();
			this.dialog.dispose();
			this.dialog = null;
		}
		boolean setBibRefType(BibRefType brt) {
			this.brt = brt;
			boolean enabled;
			if (this.brt.requiresAttribute(AUTHOR_ANNOTATION_TYPE)) {
				this.authorsField.setRequired();
				enabled = true;
			}
			else if (this.brt.canHaveAttribute(AUTHOR_ANNOTATION_TYPE)) {
				this.authorsField.setOptional();
				enabled = true;
			}
			else {
				this.authorsField.setExcluded();
				enabled = false;
			}
			this.authorsField.setBibRefType(this.brt);
			this.authorsField.setValueInputEditable(false);
			return enabled;
		}
		void fillValueInputs(RefData ref) {
			this.authorTabs.removeAll();
			AuthorData[] ads = ref.getAuthorDatas();
			if (ads == null)
				return;
			for (int a = 0; a < ads.length; a++)
				this.authorTabs.addTab(ads[a].name, new AuthorDataEditorPanel(this, ads[a], (a == 0)));
			if (ads.length != 0) {
				this.authorTabs.setSelectedIndex(0);
				((AuthorDataEditorPanel) this.authorTabs.getSelectedComponent()).nameField.valueInput.requestFocusInWindow();
			}
			this.authorsField.fillValueInput(ref);
		}
		void fillRefData(RefData ref) {
			if ((this.brt == null) || this.brt.canHaveAttribute(AUTHOR_ANNOTATION_TYPE)) {
				ref.removeAttribute(AUTHOR_ANNOTATION_TYPE);
				for (int a = 0; a < this.authorTabs.getTabCount(); a++)
					((AuthorDataEditorPanel) this.authorTabs.getComponentAt(a)).fillRefData(ref);
			}
			else ref.removeAttribute(AUTHOR_ANNOTATION_TYPE);
		}
	}
	private static class AuthorDataEditorPanel extends JPanel implements ActionListener, DocumentListener {
		AuthorsEditorPanel parent;
		AuthorDataEditorField nameField;
		AuthorDataEditorField[] fields;
		AuthorDataEditorPanel(AuthorsEditorPanel parent, AuthorData ad, boolean focusName) {
			super(new GridBagLayout(), true);
			this.parent = parent;
			
			this.fields = new AuthorDataEditorField[parent.details.length];
			for (int d = 0; d < this.fields.length; d++) {
				this.fields[d] = new AuthorDataEditorField(parent.details[d], (parent.details[d].substring(0, 1).toUpperCase() + parent.details[d].substring(1)), AuthorData.AUTHOR_NAME_ATTRIBUTE.equals(parent.details[d]));
				if (ad != null)
					this.fields[d].fillValueInput(ad);
				if (AuthorData.AUTHOR_NAME_ATTRIBUTE.equals(parent.details[d]))
					this.nameField = this.fields[d];
				this.fields[d].valueInput.addActionListener(this);
			}
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			
			for (int f = 0; f < this.fields.length; f++) {
				gbc.gridx = 0;
				gbc.gridwidth = 1;
				gbc.weightx = 0;
				this.add(this.fields[f].label, gbc.clone());
				gbc.gridx = 1;
				gbc.gridwidth = 1;
				gbc.weightx = 1;
				this.add(this.fields[f].valueInput, gbc.clone());
				gbc.gridy++;
			}
			
			this.nameField.valueInput.getDocument().addDocumentListener(this);
			this.nameField.checkError();
		}
		public void actionPerformed(ActionEvent ae) {
			this.parent.close();
		}
		public void insertUpdate(DocumentEvent de) {
			this.parent.updateAuthorName(this);
			this.nameField.checkError();
		}
		public void removeUpdate(DocumentEvent de) {
			this.parent.updateAuthorName(this);
			this.nameField.checkError();
		}
		public void changedUpdate(DocumentEvent de) {}
		void fillRefData(RefData ref) {
			String name = this.nameField.valueInput.getText();
			if (name == null)
				return;
			name = name.trim();
			if (name.length() == 0)
				return;
			ref.addAttribute(AUTHOR_ANNOTATION_TYPE, name);
			AuthorData ad = ref.getAuthorData(name);
			for (int f = 0; f < this.fields.length; f++) {
				if (!AuthorData.AUTHOR_NAME_ATTRIBUTE.equals(this.fields[f].name))
					this.fields[f].fillAuthorData(ad);
			}
		}
	}
	private static class AuthorDataEditorField {
		final String name;
		String labelStr;
		JLabel label;
		JTextField valueInput = new JTextField();
		AuthorDataEditorField(String name, String label, boolean required) {
			this.name = name;
			this.labelStr = label;
			this.label = new JLabel((this.labelStr + ": "), JLabel.RIGHT);
			if (required) {
				this.valueInput.setBackground(Color.WHITE);
				this.valueInput.setForeground(Color.BLACK);
			}
			else {
				this.valueInput.setBackground(Color.LIGHT_GRAY);
				this.valueInput.setForeground(Color.BLACK);
			}
		}
		void fillValueInput(AuthorData ad) {
			String value = ad.getAttribute(this.name);
			this.valueInput.setText((value == null) ? "" : value);
		}
		void fillAuthorData(AuthorData ad) {
			String value = this.valueInput.getText();
			if (value == null)
				value = "";
			else value = value.trim();
			if (value.length() == 0)
				ad.removeAttribute(this.name);
			else ad.setAttribute(this.name, value);
		}
		private Border valueInputBorder = null;
		boolean checkError() {
			//	TODO somehow facilitate specifying some validation pattern
			String value = this.valueInput.getText();
			if (value == null)
				value = "";
			else value = value.trim();
			boolean error = (value.length() == 0);
			if (this.valueInputBorder == null)
				this.valueInputBorder = valueInput.getBorder();
			this.valueInput.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder((error ? Color.RED : this.label.getBackground()), 2),
					this.valueInputBorder
				));
			return error;
		}
	}
	
	/**
	 * Obtain the reference data set currently displayed for editing in the
	 * panel. Client code should use either of the getErrors() methods first to
	 * check if the data is valid.
	 * @return the reference data set representing the data currently in the
	 *         panel
	 */
	public RefData getRefData() {
		RefData ref = new RefData();
		this.fillRefData(ref);
		return ref;
	}
	
	/**
	 * Fill a reference data set with the values currently entered in the panel.
	 * @param ref the reference data set to fill
	 */
	public void fillRefData(RefData ref) {
		if (this.primaryIdField != null)
			this.primaryIdField.fillRefData(ref);
		for (int f = 0; f < this.fields.length; f++) {
			if (AUTHOR_ANNOTATION_TYPE.equals(this.fields[f].name) && (this.authorsEditor != null))
				this.authorsEditor.fillRefData(ref);
			else this.fields[f].fillRefData(ref);
		}
		SelectableBibRefType sbrt = ((SelectableBibRefType) this.typeSelector.getSelectedItem());
		if (sbrt != null)
			ref.setAttribute(PUBLICATION_TYPE_ATTRIBUTE, sbrt.brt.name);
	}
	
	/**
	 * Display a reference data set for editing.
	 * @param ref the reference data set to display
	 */
	public void setRefData(RefData ref) {
		if (this.primaryIdField != null)
			this.primaryIdField.fillValueInput(ref);
		for (int f = 0; f < this.fields.length; f++) {
			if (AUTHOR_ANNOTATION_TYPE.equals(this.fields[f].name) && (this.authorsEditor != null))
				this.authorsEditor.fillValueInputs(ref);
			else this.fields[f].fillValueInput(ref);
		}
		String type = ref.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		if (type == null)
			type = this.typeSystem.classify(ref);
		BibRefType brType = this.typeSystem.getBibRefType(type);
		if (brType != null)
			this.typeSelector.setSelectedItem(new SelectableBibRefType(brType));
		this.showErrors(); // we have to do this separately in case type selection does not change
	}
	
	private void showErrors() {
		if (this.primaryIdField != null)
			this.primaryIdField.showError();
		for (int f = 0; f < this.fields.length; f++)
			this.fields[f].showError();
	}
	
	/**
	 * Retrieve the error messages for the reference data currently displayed in
	 * the panel. If the data is valid, this method returns null.
	 * @return an array holding the error messages, or null, if there are no
	 *         errors
	 */
	public String[] getErrors() {
		return this.getErrors(null);
	}
	
	/**
	 * Retrieve the error messages for the reference data currently displayed in
	 * the panel. If the data is valid, this method returns null.
	 * @param lang the language for the error messages
	 * @return an array holding the error messages, or null, if there are no
	 *         errors
	 */
	public String[] getErrors(String lang) {
		SelectableBibRefType sbrt = ((SelectableBibRefType) this.typeSelector.getSelectedItem());
		String[] errors = ((sbrt == null) ? null : sbrt.brt.getErrors(this.getRefData(), lang));
		if ((this.primaryIdField == null) || this.primaryIdField.validateValueInput())
			return errors;
		String[] eErrors;
		if (errors == null)
			eErrors = new String[1];
		else {
			eErrors = new String[errors.length + 1];
			System.arraycopy(errors, 0, eErrors, 1, errors.length);
		}
		eErrors[0] = (this.primaryIdField.labelStr + " is empty or invalid.");
		return eErrors;
	}
	
	private static class SelectableBibRefType {
		private BibRefType brt;
		SelectableBibRefType(BibRefType brt) {
			this.brt = brt;
		}
		public String toString() {
			return this.brt.getLabel();
		}
		public boolean equals(Object obj) {
			return ((obj instanceof SelectableBibRefType) && ((SelectableBibRefType) obj).brt.name.equals(this.brt.name));
		}
		public int hashCode() {
			return this.brt.name.hashCode();
		}
	}
	
	private static abstract class BibRefEditorField {
		final String name;
		String labelStr;
		JLabel label;
		BibRefType brt;
		BibRefEditorField(String name, String label) {
			this.name = name;
			this.labelStr = label;
			this.label = new JLabel((this.labelStr + ": "), JLabel.RIGHT);
		}
		void setRequired() {
			this.label.setEnabled(true);
			JComponent valueInput = this.getValueInput();
			valueInput.setBackground(Color.WHITE);
			valueInput.setForeground(Color.BLACK);
			this.setValueInputEditable(true);
		}
		void setOptional() {
			this.label.setEnabled(true);
			JComponent valueInput = this.getValueInput();
			valueInput.setBackground(Color.LIGHT_GRAY);
			valueInput.setForeground(Color.BLACK);
			this.setValueInputEditable(true);
		}
		void setExcluded() {
			this.label.setEnabled(false);
			JComponent valueInput = this.getValueInput();
			valueInput.setBackground(Color.GRAY);
			valueInput.setForeground(Color.DARK_GRAY);
			this.setValueInputEditable(false);
		}
		void showError() {
			boolean error = !this.validateValueInput();
			System.out.println(this.name + " showing error ==> " + error);
			this.markError(error && ((this.brt == null) || this.brt.canHaveAttribute(this.name)));
		}
		private Border valueInputBorder = null;
		void markError(boolean error) {
			JComponent valueInput = this.getValueInput();
			if (this.valueInputBorder == null)
				this.valueInputBorder = valueInput.getBorder();
			valueInput.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder((error ? Color.RED : this.label.getBackground()), 2),
					this.valueInputBorder
				));
		}
		boolean setBibRefType(BibRefType brt) {
			this.brt = brt;
			boolean enabled;
			if (this.brt.requiresAttribute(this.name)) {
				this.setRequired();
				enabled = true;
			}
			else if (this.brt.canHaveAttribute(this.name)) {
				this.setOptional();
				enabled = true;
			}
			else {
				this.setExcluded();
				enabled = false;
			}
			this.showError();
			return enabled;
		}
		abstract JComponent getValueInput();
		abstract void setValueInputEditable(boolean editable);
		abstract void fillValueInput(RefData ref);
		abstract void fillRefData(RefData ref);
		abstract boolean validateValueInput();
	}
	private static abstract class BibRefEditorFieldSF extends BibRefEditorField {
		JTextField valueInput = new JTextField("");
		BibRefEditorFieldSF(String name, String label) {
			super(name, label);
			this.valueInput.setBorder(BorderFactory.createLoweredBevelBorder());
			this.valueInput.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent fe) {
					showError();
				}
			});
		}
		JComponent getValueInput() {
			return this.valueInput;
		}
		void setValueInputEditable(boolean editable) {
			this.valueInput.setEditable(editable);
		}
		boolean validateValueInput() {
			if (this.brt == null)
				return true;
			String value = this.valueInput.getText();
			if (value == null)
				value = "";
			else value = value.trim();
			return this.brt.checkValue(this.name, value);
		}
	}
	private static class BibRefEditorFieldSV extends BibRefEditorFieldSF {
		BibRefEditorFieldSV(String name, String label) {
			super(name, label);
		}
		void fillValueInput(RefData ref) {
			String value = ref.getAttribute(this.name);
			this.valueInput.setText((value == null) ? "" : value);
		}
		void fillRefData(RefData ref) {
			if ((this.brt == null) || this.brt.canHaveAttribute(this.name)) {
				String value = this.valueInput.getText();
				if (value == null)
					value = "";
				else value = value.trim();
				if (value.length() == 0)
					ref.removeAttribute(this.name);
				else ref.setAttribute(this.name, value);
			}
			else ref.removeAttribute(this.name);
		}
	}
	private static class BibRefEditorFieldSFMV extends BibRefEditorFieldSF {
		private String separator;
		BibRefEditorFieldSFMV(String name, String label, String separator) {
			super(name, label);
			this.separator = separator;
		}
		void fillValueInput(RefData ref) {
			String[] values = ref.getAttributeValues(this.name);
			if (values == null)
				this.valueInput.setText("");
			else {
				StringBuffer value = new StringBuffer(values[0]);
				for (int v = 1; v < values.length; v++) {
					value.append(" " + this.separator + " ");
					value.append(values[v]);
				}
				this.valueInput.setText(value.toString());
			}
		}
		void fillRefData(RefData ref) {
			if ((this.brt == null) || this.brt.canHaveAttribute(this.name)) {
				String valuesString = this.valueInput.getText();
				if (valuesString == null)
					valuesString = "";
				else valuesString = valuesString.trim();
				ref.removeAttribute(this.name);
				if (valuesString.length() != 0) {
					String[] values = valuesString.split("\\s*" + RegExUtils.escapeForRegEx(this.separator) + "\\s*");
					for (int v = 0; v < values.length; v++)
						ref.addAttribute(this.name, values[v]);
				}
			}
			else ref.removeAttribute(this.name);
		}
	}
	private static class BibRefEditorFieldMF extends BibRefEditorField {
		private JPanel valueInput;
		private BibRefEditorFieldSV[] partFields;
		BibRefEditorFieldMF(String name, String label, String[] partNames, String[] partLabels) {
			super(name, label);
			this.valueInput = new JPanel(new GridLayout(1, (partNames.length * 2)), true);
			this.valueInput.setBorder(BorderFactory.createLineBorder(this.valueInput.getBackground(), 2));
			this.partFields = new BibRefEditorFieldSV[partNames.length];
			for (int p = 0; p < partNames.length; p++) {
				this.partFields[p] = new BibRefEditorFieldSV(partNames[p], partLabels[p]);
				this.valueInput.add(this.partFields[p].label);
				this.valueInput.add(this.partFields[p].getValueInput());
				this.partFields[p].valueInput.addFocusListener(new FocusAdapter() {
					public void focusLost(FocusEvent fe) {
						showError();
					}
				});
			}
		}
		JComponent getValueInput() {
			return this.valueInput;
		}
		void setValueInputEditable(boolean editable) {
			for (int p = 0; p < this.partFields.length; p++)
				this.partFields[p].setValueInputEditable(editable);
		}
		void fillValueInput(RefData ref) {
			for (int p = 0; p < this.partFields.length; p++)
				this.partFields[p].fillValueInput(ref);
		}
		void fillRefData(RefData ref) {
			for (int p = 0; p < this.partFields.length; p++)
				this.partFields[p].fillRefData(ref);
		}
		void setRequired() {
			this.label.setEnabled(true);
			for (int p = 0; p < this.partFields.length; p++)
				this.partFields[p].setRequired();
		}
		void setOptional() {
			this.label.setEnabled(true);
			for (int p = 0; p < this.partFields.length; p++)
				this.partFields[p].setOptional();
		}
		void setExcluded() {
			this.label.setEnabled(false);
			for (int p = 0; p < this.partFields.length; p++)
				this.partFields[p].setExcluded();
		}
		boolean setBibRefType(BibRefType brt) {
			this.brt = brt;
			boolean enabled = false;
			for (int p = 0; p < this.partFields.length; p++)
				enabled = (enabled | this.partFields[p].setBibRefType(brt));
			this.label.setEnabled(enabled);
			this.showError();
			return enabled;
		}
		boolean validateValueInput() {
			boolean ok = false;
			int valLengthSum = 0;
			for (int p = 0; p < this.partFields.length; p++) {
				ok = (ok | this.partFields[p].validateValueInput());
				String val = this.partFields[p].valueInput.getText();
				if (val == null)
					val = "";
				else val = val.trim();
				valLengthSum += val.length();
			}
			return (ok && (valLengthSum != 0));
		}
		void showError() {
			boolean fieldAllowed = false;
			for (int p = 0; p < this.partFields.length; p++)
				fieldAllowed = (fieldAllowed || (this.partFields[p].brt == null) || this.partFields[p].brt.canHaveAttribute(this.partFields[p].name));
			this.markError(fieldAllowed & !this.validateValueInput());
		}
	}
	
	/**
	 * Display a dialog for a user to input a reference data set. If the
	 * argument identifier types are null, no fields for identifiers will be
	 * displayed.
	 * @param idTypes the identifier types to display input fields for
	 * @return the newly created reference data set, or null, if the edit dialog
	 *         was cancelled
	 */
	public static RefData createRefData(String[] idTypes) {
		return editRefData(null, null, idTypes, null, "Enter Document Meta Data");
	}
	
	/**
	 * Display a dialog for a user to input a reference data set. If the
	 * argument identifier types are null, no fields for identifiers will be
	 * displayed.
	 * @param idTypes the identifier types to display input fields for
	 * @param authorDetails the names of the author detail attributes to make
	 *            available for editing
	 * @return the newly created reference data set, or null, if the edit dialog
	 *         was cancelled
	 */
	public static RefData createRefData(String[] idTypes, String[] authorDetails) {
		return editRefData(null, null, idTypes, authorDetails, "Enter Document Meta Data");
	}
	
	/**
	 * Display a dialog for a user to input a reference data set. If the
	 * argument identifier types are null, no fields for identifiers will be
	 * displayed. If the argument reference type system is null, the default one
	 * will be used. If the argument identifier types are null, no fields for
	 * identifiers will be displayed.
	 * @param ref the reference data set to edit
	 * @param refTypeSystem the reference type system to use for data validation
	 * @param idTypes the identifier types to display input fields for
	 * @return the newly created or updated reference data set, or null, if the
	 *         edit dialog was cancelled
	 */
	public static RefData createRefData(BibRefTypeSystem refTypeSystem, String[] idTypes) {
		return editRefData(null, refTypeSystem, idTypes, null, "Enter Document Meta Data");
	}
	
	/**
	 * Display a dialog for a user to input a reference data set. If the
	 * argument identifier types are null, no fields for identifiers will be
	 * displayed. If the argument reference type system is null, the default one
	 * will be used. If the argument identifier types are null, no fields for
	 * identifiers will be displayed.
	 * @param ref the reference data set to edit
	 * @param refTypeSystem the reference type system to use for data validation
	 * @param idTypes the identifier types to display input fields for
	 * @param authorDetails the names of the author detail attributes to make
	 *            available for editing
	 * @return the newly created or updated reference data set, or null, if the
	 *         edit dialog was cancelled
	 */
	public static RefData createRefData(BibRefTypeSystem refTypeSystem, String[] idTypes, String[] authorDetails) {
		return editRefData(null, refTypeSystem, idTypes, authorDetails, "Enter Document Meta Data");
	}
	
	/**
	 * Display a reference data set for editing. If the argument data set is
	 * empty, the dialog will be initially empty as well. If it is not null, the
	 * data will be displayed, and the argument object will be updated when the
	 * dialog is closed and not cancelled. If the argument identifier types
	 * are null, no fields for identifiers will be displayed.
	 * @param ref the reference data set to edit
	 * @param idTypes the identifier types to display input fields for
	 * @return the newly created or updated reference data set, or null, if the
	 *         edit dialog was cancelled
	 */
	public static RefData editRefData(RefData ref, String[] idTypes) {
		return editRefData(ref, null, idTypes, null, "Edit Document Meta Data");
	}
	
	/**
	 * Display a reference data set for editing. If the argument data set is
	 * empty, the dialog will be initially empty as well. If it is not null, the
	 * data will be displayed, and the argument object will be updated when the
	 * dialog is closed and not cancelled. If the argument identifier types
	 * are null, no fields for identifiers will be displayed.
	 * @param ref the reference data set to edit
	 * @param idTypes the identifier types to display input fields for
	 * @param authorDetails the names of the author detail attributes to make
	 *            available for editing
	 * @return the newly created or updated reference data set, or null, if the
	 *         edit dialog was cancelled
	 */
	public static RefData editRefData(RefData ref, String[] idTypes, String[] authorDetails) {
		return editRefData(ref, null, idTypes, authorDetails, "Edit Document Meta Data");
	}
	
	/**
	 * Display a reference data set for editing. If the argument data set is
	 * empty, the dialog will be initially empty as well. If it is not null, the
	 * data will be displayed, and the argument object will be updated when the
	 * dialog is closed and not cancelled. If the argument reference type system
	 * is null, the default one will be used. If the argument identifier types
	 * are null, no fields for identifiers will be displayed.
	 * @param ref the reference data set to edit
	 * @param refTypeSystem the reference type system to use for data validation
	 * @param idTypes the identifier types to display input fields for
	 * @return the newly created or updated reference data set, or null, if the
	 *         edit dialog was cancelled
	 */
	public static RefData editRefData(RefData ref, BibRefTypeSystem refTypeSystem, String[] idTypes) {
		return editRefData(ref, refTypeSystem, idTypes, null, "Edit Document Meta Data");
	}
	
	/**
	 * Display a reference data set for editing. If the argument data set is
	 * empty, the dialog will be initially empty as well. If it is not null, the
	 * data will be displayed, and the argument object will be updated when the
	 * dialog is closed and not cancelled. If the argument reference type system
	 * is null, the default one will be used. If the argument identifier types
	 * are null, no fields for identifiers will be displayed.
	 * @param ref the reference data set to edit
	 * @param refTypeSystem the reference type system to use for data validation
	 * @param idTypes the identifier types to display input fields for
	 * @param authorDetails the names of the author detail attributes to make
	 *            available for editing
	 * @return the newly created or updated reference data set, or null, if the
	 *         edit dialog was cancelled
	 */
	public static RefData editRefData(RefData ref, BibRefTypeSystem refTypeSystem, String[] idTypes, String[] authorDetails) {
		return editRefData(ref, refTypeSystem, idTypes, authorDetails, "Edit Document Meta Data");
	}
	
	private static RefData editRefData(RefData ref, BibRefTypeSystem refTypeSystem, String[] idTypes, String[] authorDetails, String dialogTitle) {
		if (refTypeSystem == null)
			refTypeSystem = BibRefTypeSystem.getDefaultInstance();
		final JDialog refEditDialog = DialogFactory.produceDialog(dialogTitle, true);
		final BibRefEditorPanel refEditorPanel = new BibRefEditorPanel(refTypeSystem, idTypes, authorDetails, ref);
		final boolean[] cancelled = {false};
		
		JButton validate = new JButton("Validate");
		validate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] errors = refEditorPanel.getErrors();
				if (errors == null)
					JOptionPane.showMessageDialog(refEditorPanel, "The document meta data is valid.", "Validation Report", JOptionPane.INFORMATION_MESSAGE);
				else displayErrors(errors, refEditorPanel);
			}
		});
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] errors = refEditorPanel.getErrors();
				if (errors == null)
					refEditDialog.dispose();
				else displayErrors(errors, refEditorPanel);
			}
		});
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cancelled[0] = true;
				refEditDialog.dispose();
			}
		});
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
		buttonPanel.add(validate);
		buttonPanel.add(ok);
		buttonPanel.add(cancel);
		
		refEditDialog.getContentPane().setLayout(new BorderLayout());
		refEditDialog.getContentPane().add(refEditorPanel, BorderLayout.CENTER);
		refEditDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		refEditDialog.setSize(600, 600);
		refEditDialog.setLocationRelativeTo(DialogFactory.getTopWindow());
		refEditDialog.setVisible(true);
		
		if (cancelled[0])
			return null;
		
		if (ref == null)
			ref = refEditorPanel.getRefData();
		else refEditorPanel.fillRefData(ref);
		return ref;
	}
	private static final void displayErrors(String[] errors, JPanel parent) {
		StringVector errorMessageBuilder = new StringVector();
		errorMessageBuilder.addContent(errors);
		JOptionPane.showMessageDialog(parent, ("The document meta data is not valid. In particular, there are the following errors:\n" + errorMessageBuilder.concatStrings("\n")), "Validation Report", JOptionPane.ERROR_MESSAGE);
	}
	
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		String[] idTypes = {"", "SMNK-Pub", "HNS-PUB", "TEST"};
		String[] authorDetails = {"ORCID", "name", "eMail", "affiliation", "LSID"};
		
		final BibRefEditorPanel brep = new BibRefEditorPanel(idTypes, authorDetails);
		
		JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
		JButton b;
		b = new JButton("Validate");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String[] errors = brep.getErrors();
				StringBuffer em = new StringBuffer();
				int mt;
				if (errors == null) {
					em.append("The reference is valid.");
					mt = JOptionPane.INFORMATION_MESSAGE;
				}
				else {
					em.append("The reference has errors:");
					for (int e = 0; e < errors.length; e++)
						em.append("\n - " + errors[e]);
					mt = JOptionPane.ERROR_MESSAGE;
				}
				JOptionPane.showMessageDialog(brep, em.toString(), "Reference Validation", mt);
			}
		});
		bp.add(b);
		b = new JButton("Print XML");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				RefData rd = brep.getRefData();
				System.out.println(rd.toXML());
			}
		});
		bp.add(b);
		b = new JButton("Print MODS");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				RefData rd = brep.getRefData();
				System.out.println(brep.typeSystem.toModsXML(rd));
			}
		});
		bp.add(b);
		b = new JButton("Print Origin");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				RefData rd = brep.getRefData();
				System.out.println(brep.typeSystem.getOrigin(rd));
			}
		});
		bp.add(b);
		
		JDialog d = DialogFactory.produceDialog("Test", true);
		d.getContentPane().setLayout(new BorderLayout());
		d.getContentPane().add(brep, BorderLayout.CENTER);
		d.getContentPane().add(bp, BorderLayout.SOUTH);
		d.setSize(600, 600);
		d.setLocationRelativeTo(null);
		d.setVisible(true);
	}
}
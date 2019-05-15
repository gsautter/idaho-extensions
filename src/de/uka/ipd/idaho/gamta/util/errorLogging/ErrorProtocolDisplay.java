///*
// * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *     * Redistributions of source code must retain the above copyright
// *       notice, this list of conditions and the following disclaimer.
// *     * Redistributions in binary form must reproduce the above copyright
// *       notice, this list of conditions and the following disclaimer in the
// *       documentation and/or other materials provided with the distribution.
// *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
// *       names of its contributors may be used to endorse or promote products
// *       derived from this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
// * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
// * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//package de.uka.ipd.idaho.gamta.util.errorLogging;
//
//import java.awt.BorderLayout;
//import java.awt.GridLayout;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.ItemEvent;
//import java.awt.event.ItemListener;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.Iterator;
//import java.util.TreeMap;
//import java.util.Vector;
//
//import javax.swing.AbstractListModel;
//import javax.swing.BorderFactory;
//import javax.swing.DefaultComboBoxModel;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JComboBox;
//import javax.swing.JLabel;
//import javax.swing.JList;
//import javax.swing.JOptionPane;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTabbedPane;
//import javax.swing.ListSelectionModel;
//import javax.swing.UIManager;
//import javax.swing.event.ChangeEvent;
//import javax.swing.event.ChangeListener;
//import javax.swing.event.ListSelectionEvent;
//import javax.swing.event.ListSelectionListener;
//
//import de.uka.ipd.idaho.gamta.Attributed;
//import de.uka.ipd.idaho.gamta.util.CountingSet;
//import de.uka.ipd.idaho.gamta.util.errorLogging.DocumentErrorProtocol.Error;
//import de.uka.ipd.idaho.stringUtils.StringUtils;
//
///**
// * Visualization facility for document error protocols. By default, the panel
// * only contains the error category tabs in <code>BorderLayout.CENTER</code>
// * position. Client code may add other components around it if required.
// * 
// * @author sautter
// */
//public class ErrorProtocolDisplay extends JPanel {
//	private static String ALL_ERRORS_FILTER_TYPE = "<>";
//	private static String ALL_ERRORS_FILTER_LABEL = "<All Errors>";
//	
//	private static class ErrorSeverityFilter extends JCheckBox {
//		private String severity;
//		private String baseLabel;
//		ErrorSeverityFilter(String severity) {
//			super("", true);
//			this.severity = severity;
//			this.baseLabel = (StringUtils.capitalize(severity) + "s");
//			this.setText(this.baseLabel);
//		}
//		void updateCounts(DocumentErrorProtocol dep, String category, ErrorTypeFilter etf) {
//			if (this.isSelected())
//				etf.showingCount += (ALL_ERRORS_FILTER_TYPE.equals(etf.type) ? dep.getErrorSeverityCount(category, this.severity) : dep.getErrorSeverityCount(category, etf.type, this.severity));
//		}
//		void updateLabel(DocumentErrorProtocol dep, String category, String type) {
//			int count = dep.getErrorSeverityCount(category, this.severity);
//			int showingCount = (ALL_ERRORS_FILTER_TYPE.equals(type) ? count : dep.getErrorSeverityCount(category, type, this.severity));
//			this.setText(this.baseLabel + " (" + showingCount + " of " + count + ")");
//		}
//	}
//	
//	private DocumentErrorProtocol dep = null;
//	private boolean depReadOnly = false;
//	private Comparator depErrorOrder = null;
//	private String errorCategoryOrder = null;
//	private String errorTypeOrder = null;
//	
//	private TreeMap errorTabsByCategory = new TreeMap(String.CASE_INSENSITIVE_ORDER);
//	private JTabbedPane errorTabs = new JTabbedPane();
//	private int categoryTabPlacement = JTabbedPane.LEFT;
//	private boolean muteErrorCategoryChanges = false;
//	
//	private JButton resolveButton = new JButton("Resolve Error");
//	private JButton falsePosButton = new JButton("False Positive");
//	private JButton[] customButtons = null;
//	private JPanel errorButtonPanel = new JPanel(new GridLayout(1, 0, 5, 0), true);
//	private boolean showButtons = true;
//	
//	private ErrorSeverityFilter showBlockers = new ErrorSeverityFilter(Error.SEVERITY_BLOCKER);
//	private ErrorSeverityFilter showCriticals = new ErrorSeverityFilter(Error.SEVERITY_CRITICAL);
//	private ErrorSeverityFilter showMajors = new ErrorSeverityFilter(Error.SEVERITY_MAJOR);
//	private ErrorSeverityFilter showMinors = new ErrorSeverityFilter(Error.SEVERITY_MINOR);
//	private JPanel showErrorsPanel = new JPanel(new GridLayout(1, 0), true);
//	private boolean severityFiltersActive = true;
//	
//	/**
//	 * Constructor
//	 * @param dep the document error protocol to show
//	 */
//	public ErrorProtocolDisplay(DocumentErrorProtocol dep) {
//		this(dep, false);
//	}
//	
//	/**
//	 * Constructor
//	 * @param dep the document error protocol to show
//	 * @param readOnly open in read-only mode?
//	 */
//	public ErrorProtocolDisplay(DocumentErrorProtocol dep, boolean readOnly) {
//		super(new BorderLayout(), true);
//		
//		//	initialize buttons ...
//		this.resolveButton.setBorder(BorderFactory.createRaisedBevelBorder());
//		this.resolveButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				removeError(false);
//			}
//		});
//		this.falsePosButton.setBorder(BorderFactory.createRaisedBevelBorder());
//		this.falsePosButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				removeError(true);
//			}
//		});
//		
//		//	... and tray them up
//		this.errorButtonPanel.setBorder(BorderFactory.createMatteBorder(0, 3, 0, 3, this.errorButtonPanel.getBackground()));
//		this.errorButtonPanel.add(this.resolveButton);
//		this.errorButtonPanel.add(this.falsePosButton);
//		
//		//	configure severity filters ...
//		ItemListener showSeverityListener = new ItemListener() {
//			public void itemStateChanged(ItemEvent ie) {
//				applySeverityFilter();
//			}
//		};
//		this.showBlockers.addItemListener(showSeverityListener);
//		this.showCriticals.addItemListener(showSeverityListener);
//		this.showMajors.addItemListener(showSeverityListener);
//		this.showMinors.addItemListener(showSeverityListener);
//		
//		//	... and tray them up
//		this.showErrorsPanel.add(this.showBlockers);
//		this.showErrorsPanel.add(this.showCriticals);
//		this.showErrorsPanel.add(this.showMajors);
//		this.showErrorsPanel.add(this.showMinors);
//		this.showErrorsPanel.setBorder(BorderFactory.createEtchedBorder());
//		
//		//	configure and add error tabs
//		this.errorTabs.setTabPlacement(this.categoryTabPlacement);
//		this.errorTabs.addChangeListener(new ChangeListener() {
//			public void stateChanged(ChangeEvent ce) {
//				moveAccessories();
//				applySeverityFilter();
//				notifyErrorCategorySelected();
//				notifyErrorSelected();
//			}
//		});
//		this.add(this.errorTabs, BorderLayout.CENTER);
//		
//		//	show initial content
//		this.setErrorProtocol(dep, readOnly);
//	}
//	
//	/**
//	 * Set the error protocol to display.
//	 * @param dep the error protocol
//	 */
//	public void setErrorProtocol(DocumentErrorProtocol dep) {
//		this.setErrorProtocol(dep, this.depReadOnly);
//	}
//	
//	/**
//	 * Set the error protocol to display.
//	 * @param dep the error protocol
//	 * @param readOnly show the protocol in read-only mode?
//	 */
//	public void setErrorProtocol(DocumentErrorProtocol dep, boolean readOnly) {
//		this.dep = dep;
//		this.depReadOnly = readOnly;
//		if (this.dep != null)
//			this.depErrorOrder = this.dep.getErrorComparator();
//		
//		//	mute notifications for initialization
//		this.muteErrorCategoryChanges = true;
//		
//		//	enable/disable buttons
//		this.resolveButton.setEnabled(!this.depReadOnly);
//		this.falsePosButton.setEnabled(!this.depReadOnly);
//		if (this.customButtons != null) {
//			for (int b = 0; b < this.customButtons.length; b++)
//				this.customButtons[b].setEnabled(!this.depReadOnly);
//		}
//		
//		//	clear error tabs
//		this.errorTabs.removeAll();
//		this.errorTabsByCategory.clear();
//		if (this.dep == null)
//			return;
//		
//		//	add all errors from current protocol
//		String[] errorCategories = dep.getErrorCategories();
//		for (int c = 0; c < errorCategories.length; c++) {
//			if (this.dep.getErrorCount(errorCategories[c]) == 0)
//				continue;
//			ErrorCategoryDisplay ecd = new ErrorCategoryDisplay(errorCategories[c], this.dep);
//			this.errorTabs.add(this.getErrorTabLabel(errorCategories[c]), ecd);
//			this.errorTabsByCategory.put(errorCategories[c], ecd);
//		}
//		
//		//	make errors show
//		this.validate();
//		this.repaint();
//		
//		//	(re)activate notifications
//		this.muteErrorCategoryChanges = false;
//		
//		//	notify about initial error category
//		this.updateAccessories();
//		this.notifyErrorCategorySelected();
//	}
//	
//	/**
//	 * Check whether or not buttons are showing.
//	 * @return true if buttons are showing
//	 */
//	public boolean isShowingButtons() {
//		return this.showButtons;
//	}
//	
//	/**
//	 * Show or hide buttons that take actions on the currently selected error,
//	 * both built-in and custom.
//	 * @param showButtons show buttons?
//	 */
//	public void setShowButtons(boolean showButtons) {
//		if (showButtons == this.showButtons)
//			return;
//		this.showButtons = showButtons;
//		this.updateAccessories();
//	}
//	
//	/**
//	 * Check whether or not error severity filters are showing.
//	 * @return true if error severity filters are showing
//	 */
//	public boolean isShowingSeverityFilters() {
//		return this.severityFiltersActive;
//	}
//	
//	/**
//	 * Show or hide the checkboxes that filter errors by severity.
//	 * @param showFilters show the severity filters?
//	 */
//	public void setShowSeverityFilters(boolean showFilters) {
//		if (showFilters == this.severityFiltersActive)
//			return;
//		this.severityFiltersActive = showFilters;
//		this.updateAccessories();
//	}
//	
//	/**
//	 * Retrieve the current state of the error severity filters. If the filters
//	 * are not showing, this method return null.
//	 * @return the severity filter state
//	 */
//	public String getSeverityFilterState() {
//		if (!this.severityFiltersActive)
//			return null;
//		StringBuffer sfs = new StringBuffer();
//		if (this.showBlockers.isSelected()) {
//			if (sfs.length() != 0)
//				sfs.append("-");
//			sfs.append("BL");
//		}
//		if (this.showCriticals.isSelected()) {
//			if (sfs.length() != 0)
//				sfs.append("-");
//			sfs.append("CR");
//		}
//		if (this.showMajors.isSelected()) {
//			if (sfs.length() != 0)
//				sfs.append("-");
//			sfs.append("MA");
//		}
//		if (this.showMinors.isSelected()) {
//			if (sfs.length() != 0)
//				sfs.append("-");
//			sfs.append("MI");
//		}
//		return sfs.toString();
//	}
//	
//	/**
//	 * Set the state of the error severity filters. If the argument status is
//	 * null, severity filters are set to not showing.
//	 * @param the severity filter state
//	 */
//	public void setSeverityFilterState(String filterState) {
//		if (filterState == null) {
//			this.setShowSeverityFilters(false);
//			return;
//		}
//		this.showBlockers.setSelected(filterState.indexOf("BL") != -1);
//		this.showCriticals.setSelected(filterState.indexOf("CR") != -1);
//		this.showMajors.setSelected(filterState.indexOf("MA") != -1);
//		this.showMinors.setSelected(filterState.indexOf("MI") != -1);
//	}
//	
//	/**
//	 * Get the placement of error category tabs.
//	 * @return the placement of error category tabs
//	 */
//	public int getCategoryTabPlacement() {
//		return this.categoryTabPlacement;
//	}
//	
//	/**
//	 * Set the placement of the error category tabs (default is on the left).
//	 * @param tabPlacement the new error category tab placement
//	 */
//	public void setCategoryTabPlacement(int tabPlacement) {
//		if (tabPlacement == this.categoryTabPlacement)
//			return;
//		this.categoryTabPlacement = tabPlacement;
//		this.validate();
//		this.repaint();
//	}
//	
//	/**
//	 * Inject an array of custom buttons to take action on the currently
//	 * selected error. These buttons show next to the 'Resolve Error' and
//	 * 'False Positive' buttons.
//	 * @param buttons the an array holding the buttons to set
//	 */
//	public void setCustomButtons(JButton[] buttons) {
//		if (Arrays.deepEquals(buttons, this.customButtons))
//			return;
//		this.customButtons = buttons;
//		this.errorButtonPanel.removeAll();
//		this.errorButtonPanel.add(this.resolveButton);
//		this.errorButtonPanel.add(this.falsePosButton);
//		if (this.customButtons != null) {
//			for (int b = 0; b < this.customButtons.length; b++)
//				this.errorButtonPanel.add(this.customButtons[b]);
//		}
//		this.updateAccessories();
//	}
//	
//	void moveAccessories() {
//		if (this.muteErrorCategoryChanges)
//			return;
//		if (this.dep == null)
//			return;
//		this.updateAccessories();
//	}
//	
//	void updateAccessories() {
//		ErrorCategoryDisplay ecd = ((ErrorCategoryDisplay) this.errorTabs.getSelectedComponent());
//		if (ecd != null)
//			ecd.updateAccessories();
//	}
//	
//	void applySeverityFilter() {
//		ErrorCategoryDisplay ecd = ((ErrorCategoryDisplay) this.errorTabs.getSelectedComponent());
//		if (ecd != null)
//			ecd.applySeverityFilter();
//	}
//	boolean passesSeverityFilter(Error error) {
//		if (Error.SEVERITY_BLOCKER.equals(error.severity))
//			return this.showBlockers.isSelected();
//		else if (Error.SEVERITY_CRITICAL.equals(error.severity))
//			return this.showCriticals.isSelected();
//		else if (Error.SEVERITY_MAJOR.equals(error.severity))
//			return this.showMajors.isSelected();
//		else if (Error.SEVERITY_MINOR.equals(error.severity))
//			return this.showMinors.isSelected();
//		else return false;
//	}
//	
//	void removeError(boolean falsePositive) {
//		ErrorCategoryDisplay ecd = ((ErrorCategoryDisplay) this.errorTabs.getSelectedComponent());
//		if (ecd != null)
//			ecd.removeError(falsePositive);
//	}
//	
//	void notifyErrorCategorySelected() {
//		if (this.muteErrorCategoryChanges)
//			return;
//		if (this.dep == null)
//			this.errorCategorySelected(null, 0);
//		ErrorCategoryDisplay ecd = ((ErrorCategoryDisplay) this.errorTabs.getSelectedComponent());
//		if (ecd == null)
//			this.errorCategorySelected(null, 0);
//		else this.errorCategorySelected(ecd.category, this.dep.getErrorCount(ecd.category));
//	}
//	
//	void errorCategoryChanged(ErrorCategoryDisplay ecd) {
//		int index = this.errorTabs.indexOfComponent(ecd);
//		if (index != -1)
//			this.errorTabs.setTitleAt(index, this.getErrorTabLabel(ecd.category));
//	}
//	
//	void errorCategoryEmpty(ErrorCategoryDisplay ecd) {
//		this.errorTabs.remove(ecd);
//		this.errorTabsByCategory.remove(ecd.category);
//	}
//	
//	String getErrorTabLabel(String category) {
//		return (this.dep.getErrorCategoryLabel(category) + " (" + this.dep.getErrorCount(category) + ")");
//	}
//	
//	void notifyErrorSelected() {
//		ErrorCategoryDisplay ecd = ((ErrorCategoryDisplay) this.errorTabs.getSelectedComponent());
//		if (ecd != null)
//			this.errorSelected(ecd.getSelectedError());
//	}
//	
//	/**
//	 * Retrieve the current error category order.
//	 * @return the error category order
//	 */
//	public String getErrorCategoryOrder() {
//		return this.errorCategoryOrder;
//	}
//	
//	/**
//	 * Set the error category order. Categories occurring earlier in the argument
//	 * string appear before ones occurring later, and those before one that do
//	 * not occur in the argument string at all. This is to allow for error order
//	 * customization by client code.
//	 * @param eco the error category order
//	 */
//	public void setErrorCategoryOrder(String eco) {
//		this.errorCategoryOrder = eco;
//	}
//	
//	/**
//	 * Retrieve the current error type order.
//	 * @return the error type order
//	 */
//	public String getErrorTypeOrder() {
//		return this.errorTypeOrder;
//	}
//	
//	/**
//	 * Set the error type order. Types occurring earlier in the argument string
//	 * appear before ones occurring later, and those before one that do not
//	 * occur in the argument string at all. This is to allow for error order
//	 * customization by client code. Setting the error type order only has an
//	 * effect if the error category order is set as well.
//	 * @param eto the error type order
//	 */
//	public void setErrorTypeOrder(String eto) {
//		this.errorTypeOrder = eto;
//	}
//	
//	/**
//	 * Notify the display that an error was added to the underlying protocol.
//	 * This method is meant for notification by protocol implementations that
//	 * automatically adjust to document edits.
//	 * @param error the error that was added
//	 */
//	public void errorAdded(DocumentErrorProtocol.Error error) {
//		ErrorCategoryDisplay ecd = ((ErrorCategoryDisplay) this.errorTabsByCategory.get(error.category));
//		if (ecd == null) {
//			ecd = new ErrorCategoryDisplay(error.category, this.dep);
//			this.errorTabsByCategory.put(error.category, ecd);
//			this.errorTabs.add(this.getErrorTabLabel(error.category), ecd);
//			this.errorTabs.setSelectedComponent(ecd);
//			ecd.updateAccessories();
//		}
//		else ecd.errorAdded(error);
//	}
//	
//	/**
//	 * Notify the display that an error was removed from the underlying protocol.
//	 * This method is meant for notification by protocol implementations that
//	 * automatically adjust to document edits.
//	 * @param error the error that was removed
//	 */
//	public void errorRemoved(DocumentErrorProtocol.Error error) {
//		ErrorCategoryDisplay ecd = ((ErrorCategoryDisplay) this.errorTabsByCategory.get(error.category));
//		if (ecd != null)
//			ecd.errorRemoved(error);
//	}
//	
//	/**
//	 * Notify an implementing subclass that an error category has been selected.
//	 * The argument category is never null unless the last error was removed
//	 * from the backing protocol. This default implementation does nothing.
//	 * @param category the error category that was selected
//	 * @param errorCount the number of errors in the category and type
//	 */
//	protected void errorCategorySelected(String category, int errorCount) {}
//	
//	/**
//	 * Notify an implementing subclass that an error type has been selected.
//	 * The argument error type is null if the wildcard filter was selected in
//	 * the argument category. The argument category is never null unless the
//	 * last error was removed from the backing protocol. This default
//	 * implementation does nothing.
//	 * @param type the error type that was selected
//	 * @param category the category the selected error type belongs to
//	 * @param errorCount the number of errors in the category and type
//	 */
//	protected void errorTypeSelected(String type, String category, int errorCount) {}
//	
//	/**
//	 * Notify an implementing subclass that an error has been selected. The
//	 * runtime class of the argument error is the one of the errors obtained
//	 * from the current error protocol. This default implementation does
//	 * nothing.
//	 * @param error the error that was selected
//	 */
//	protected void errorSelected(DocumentErrorProtocol.Error error) {}
//	
//	/**
//	 * Notify an implementing subclass that an error has been removed, either
//	 * as resolved or as a false positive. The runtime class of the argument
//	 * error is the one of the errors obtained from the current error protocol.
//	 * This default implementation does nothing.
//	 * @param error the error that was removed
//	 * @param falsePositive was the removed error marked as a false positive?
//	 */
//	protected void errorRemoved(DocumentErrorProtocol.Error error, boolean falsePositive) {}
//	
//	/**
//	 * Retrieve the currently selected error category.
//	 * @return the error category
//	 */
//	public String getErrorCategory() {
//		ErrorCategoryDisplay ecd = ((ErrorCategoryDisplay) this.errorTabs.getSelectedComponent());
//		return ((ecd == null) ? null : ecd.category);
//	}
//	
//	/**
//	 * Select an error category programmatically.
//	 * @param category the error category
//	 */
//	public boolean setErrorCategory(String category) {
//		if (category == null)
//			return false;
//		ErrorCategoryDisplay ecd = ((ErrorCategoryDisplay) this.errorTabsByCategory.get(category));
//		if (ecd == null)
//			return false;
//		this.muteErrorCategoryChanges = true;
//		ecd.updateAccessories();
//		this.errorTabs.setSelectedComponent(ecd);
//		this.muteErrorCategoryChanges = false;
//		return true;
//	}
//	
//	/**
//	 * Retrieve the currently selected error type.
//	 * @return the error type
//	 */
//	public String getErrorType() {
//		ErrorCategoryDisplay ecd = ((ErrorCategoryDisplay) this.errorTabs.getSelectedComponent());
//		return ((ecd == null) ? null : ecd.getErrorType());
//	}
//	
//	/**
//	 * Select an error category and type programatically.
//	 * @param category the error category
//	 * @param type the error type
//	 */
//	public boolean setErrorType(String category, String type) {
//		return (this.setErrorCategory(category) && ((ErrorCategoryDisplay) this.errorTabs.getSelectedComponent()).setErrorType(type));
//	}
//	
//	/**
//	 * Select a given error programatically, also selecting category and type.
//	 * @param error the error to select
//	 */
//	public boolean setError(Error error) {
//		if (error == null)
//			return false;
//		return (this.setErrorCategory(error.category) && ((ErrorCategoryDisplay) this.errorTabs.getSelectedComponent()).setError(error));
//	}
//	
//	/**
//	 * Dispose of the error protocol display, clean up and unregister any
//	 * external references, etc. This default implementation does nothing,
//	 * sub classes are welcome to overwrite it as needed.
//	 */
//	public void dispose() {}
//	
//	private final Comparator errorTrayOrder = new Comparator() {
//		public int compare(Object obj1, Object obj2) {
//			ErrorTray et1 = ((ErrorTray) obj1);
//			ErrorTray et2 = ((ErrorTray) obj2);
//			if ((errorCategoryOrder != null) && !et1.error.category.equals(et2.error.category)) {
//				int pos1 = errorCategoryOrder.indexOf(et1.error.category);
//				int pos2 = errorCategoryOrder.indexOf(et2.error.category);
//				if (pos1 == pos2) {}
//				else if (pos1 == -1)
//					return 1;
//				else if (pos2 == -1)
//					return -1;
//				else return (pos1 - pos2);
//			}
//			if ((errorCategoryOrder != null) && (errorTypeOrder != null) && !et1.error.type.equals(et2.error.type)) {
//				int pos1 = errorTypeOrder.indexOf(et1.error.type);
//				int pos2 = errorTypeOrder.indexOf(et2.error.type);
//				if (pos1 == pos2) {}
//				else if (pos1 == -1)
//					return 1;
//				else if (pos2 == -1)
//					return -1;
//				else return (pos1 - pos2);
//			}
//			return ((depErrorOrder == null) ? 0 : depErrorOrder.compare(et1.error, et2.error));
//		}
//	};
//	
//	private class ErrorCategoryDisplay extends JPanel {
//		final String category;
//		
//		private ErrorTypeFilter allErrorTypesFilter = new ErrorTypeFilter(ALL_ERRORS_FILTER_TYPE, ALL_ERRORS_FILTER_LABEL);
//		private TreeMap errorTypeFiltersByLabel = new TreeMap(String.CASE_INSENSITIVE_ORDER);
//		private Vector errorTypeFilters = new Vector();
//		
//		private JLabel description;
//		
//		private ErrorFilterModel errorTypeFilterModel = new ErrorFilterModel();
//		private JComboBox errorTypeFilterSelector = new JComboBox(this.errorTypeFilterModel);
//		private ErrorTypeFilter errorTypeFilter = null;
//		
//		private JPanel topPanel = new JPanel(new GridLayout(0, 1, 0, 2), true);
//		
//		private Vector errorTrays = new Vector();
//		
//		private Vector listErrorTrays = new Vector();
//		private ErrorListModel errorListModel = new ErrorListModel();
//		private JList errorList = new JList(this.errorListModel);
//		
//		ErrorCategoryDisplay(String category, DocumentErrorProtocol dep) {
//			super(new BorderLayout(), true);
//			this.category = category;
//			this.description = new JLabel(dep.getErrorCategoryDescription(this.category));
//			DocumentErrorProtocol.Error[] errors = dep.getErrors(this.category);
//			
//			//	populate error list and filters
//			this.allErrorTypesFilter.count = dep.getErrorCount(this.category);
//			for (int e = 0; e < errors.length; e++) {
//				this.errorTrays.add(new ErrorTray(errors[e]));
//				String label = dep.getErrorTypeLabel(this.category, errors[e].type);
//				ErrorTypeFilter etf = ((ErrorTypeFilter) this.errorTypeFiltersByLabel.get(label));
//				if (etf == null) {
//					etf = new ErrorTypeFilter(errors[e].type, label);
//					etf.count = dep.getErrorCount(this.category, errors[e].type);
//					this.errorTypeFiltersByLabel.put(label, etf);
//				}
//			}
//			
//			//	compute showing error counts
//			this.applySeverityFilter();
//			
//			//	select 'all' filter for starters
//			this.errorTypeFilter = ((ErrorTypeFilter) this.errorTypeFilters.get(0));
//			
//			//	add functionality
//			this.errorTypeFilterSelector.setEditable(false);
//			this.errorTypeFilterSelector.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(this.getBackground()), BorderFactory.createLoweredBevelBorder()));
//			this.errorTypeFilterSelector.addItemListener(new ItemListener() {
//				public void itemStateChanged(ItemEvent ie) {
//					updateErrorTypeFilter();
//				}
//			});
////			
////			//	TODO_in_parent allow enabling / disabling these buttons (some views don't want to edit the protocol)
////			JButton resolveButton = new JButton("Resolve Error");
////			resolveButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(resolveButton.getBackground()), BorderFactory.createRaisedBevelBorder()));
////			resolveButton.addActionListener(new ActionListener() {
////				public void actionPerformed(ActionEvent ae) {
////					removeError(false);
////				}
////			});
////			JButton falsePosButton = new JButton("False Positive");
////			falsePosButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(resolveButton.getBackground()), BorderFactory.createRaisedBevelBorder()));
////			falsePosButton.addActionListener(new ActionListener() {
////				public void actionPerformed(ActionEvent ae) {
////					removeError(true);
////				}
////			});
//			
//			//	build error list
//			this.errorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//			this.errorList.addListSelectionListener(new ListSelectionListener() {
//				public void valueChanged(ListSelectionEvent lse) {
//					if (!lse.getValueIsAdjusting())
//						selectError();
//				}
//			});
////			this.errorList.addFocusListener(new FocusListener() {
////				public void focusGained(FocusEvent fe) {
////					//	TODO_ne do we really need this ??? ==> mouse click listener appears to work better
////					selectError(); // need to select (if current) error when getting back focus
////				}
////				public void focusLost(FocusEvent fe) {}
////			});
//			this.errorList.addMouseListener(new MouseAdapter() {
//				public void mouseClicked(MouseEvent me) {
//					selectError();
//				}
//			});
//			JScrollPane errorListBox = new JScrollPane(this.errorList);
//			errorListBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//			errorListBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
//			errorListBox.getVerticalScrollBar().setBlockIncrement(50);
//			errorListBox.getVerticalScrollBar().setUnitIncrement(50);
//			errorListBox.setBorder(BorderFactory.createMatteBorder(3, 1, 1, 1, this.getBackground()));
//			
//			//	assemble the whole stuff
////			JPanel buttonPanel = new JPanel(new BorderLayout(), true);
////			buttonPanel.add(resolveButton, BorderLayout.WEST);
////			buttonPanel.add(falsePosButton, BorderLayout.EAST);
////			
////			JPanel functionPanel = new JPanel(new BorderLayout(), true);
////			functionPanel.add(this.errorTypeFilterSelector, BorderLayout.CENTER);
////			functionPanel.add(buttonPanel, BorderLayout.EAST);
////			
////			JPanel topPanel = new JPanel(new BorderLayout(), true);
////			topPanel.add(new JLabel(description), BorderLayout.NORTH);
////			topPanel.add(functionPanel, BorderLayout.SOUTH);
//			this.topPanel.add(this.description);
//			this.topPanel.add(this.errorTypeFilterSelector);
//			
//			this.add(this.topPanel, BorderLayout.NORTH);
//			this.add(errorListBox, BorderLayout.CENTER);
//			
//			//	make data show
//			this.errorTypeFilterModel.fireContentsChanged();
//			this.errorListModel.fireContentsChanged();
//			this.errorTypeFilterSelector.setSelectedIndex(0);
//			
//			//	notify sub class
//			errorTypeSelected((ALL_ERRORS_FILTER_TYPE.equals(this.errorTypeFilter.type) ? null : this.errorTypeFilter.type), this.category, this.errorTypeFilter.count);
//		}
//		
//		void updateAccessories() {
//			this.topPanel.removeAll();
//			this.topPanel.add(this.description);
//			
//			//	add type selector and buttons in same row if there are no custom buttons
//			if (showButtons && ((customButtons == null) || (customButtons.length == 0))) {
//				JPanel functionPanel = new JPanel(new BorderLayout(), true);
//				functionPanel.add(this.errorTypeFilterSelector, BorderLayout.CENTER);
//				functionPanel.add(errorButtonPanel, BorderLayout.EAST);
//				this.topPanel.add(functionPanel);
//			}
//			else this.topPanel.add(this.errorTypeFilterSelector);
//			
//			//	add severity filters
//			if (severityFiltersActive)
//				this.topPanel.add(showErrorsPanel);
//			
//			//	add buttons in separate row if there are custom buttons
//			if (showButtons && ((customButtons != null) && (customButtons.length != 0)))
//				this.topPanel.add(errorButtonPanel);
//			
//			//	show total and visible counts on severities
//			this.updateErrorSeverityFilters();
//			
//			//	make the whole stuff show
//			this.validate();
//			this.repaint();
//		}
//		
//		void applySeverityFilter() {
//			this.recomputeShowingCount(this.allErrorTypesFilter);
//			for (Iterator fit = this.errorTypeFiltersByLabel.keySet().iterator(); fit.hasNext();) {
//				ErrorTypeFilter etf = ((ErrorTypeFilter) this.errorTypeFiltersByLabel.get(fit.next()));
//				this.recomputeShowingCount(etf);
//			}
//			this.updateErrorTypeFilters(true);
//			this.updateErrorList();
//		}
//		
//		private void recomputeShowingCount(ErrorTypeFilter etf) {
//			etf.showingCount = 0;
//			showBlockers.updateCounts(dep, this.category, etf);
//			showCriticals.updateCounts(dep, this.category, etf);
//			showMajors.updateCounts(dep, this.category, etf);
//			showMinors.updateCounts(dep, this.category, etf);
//		}
//		
//		void errorAdded(DocumentErrorProtocol.Error error) {
//			ErrorTray et = new ErrorTray(error);
//			this.errorTrays.add(et);
//			Collections.sort(this.errorTrays, errorTrayOrder);
//			
//			String label = dep.getErrorTypeLabel(error.category, error.type);
//			ErrorTypeFilter etf = ((ErrorTypeFilter) this.errorTypeFiltersByLabel.get(label));
//			if (etf == null) {
//				etf = new ErrorTypeFilter(error.type, label);
//				this.errorTypeFiltersByLabel.put(label, etf);
//			}
//			etf.count++;
//			this.allErrorTypesFilter.count++;
//			if (passesSeverityFilter(error)) {
//				etf.showingCount++;
//				this.allErrorTypesFilter.showingCount++;
//			}
//			this.updateErrorTypeFilters(true);
//			this.updateErrorSeverityFilters();
//			
//			this.errorTypeFilter = ((ErrorTypeFilter) this.errorTypeFilterSelector.getSelectedItem());
//			this.updateErrorList();
//			
//			errorCategoryChanged(this);
//			errorTypeSelected((ALL_ERRORS_FILTER_TYPE.equals(this.errorTypeFilter.type) ? null : this.errorTypeFilter.type), this.category, this.errorTypeFilter.count);
//		}
//		
//		void errorRemoved(DocumentErrorProtocol.Error error) {
//			for (int t = 0; t < this.errorTrays.size(); t++) {
//				ErrorTray et = ((ErrorTray) this.errorTrays.get(t));
//				if (error.equals(et.error)) {
//					this.removeError(t, et, false, false);
//					break;
//				}
//			}
//		}
//		
//		String getErrorType() {
//			return (ALL_ERRORS_FILTER_TYPE.equals(this.errorTypeFilter.type) ? null : this.errorTypeFilter.type);
//		}
//		
//		Error getSelectedError() {
//			int si = this.errorList.getSelectedIndex();
//			if (si < 0)
//				return null;
//			if (this.listErrorTrays.size() <= si)
//				return null;
//			return ((ErrorTray) this.listErrorTrays.get(this.errorList.getSelectedIndex())).error;
//		}
//		
//		boolean setErrorType(String type) {
//			if (type == null)
//				type = ALL_ERRORS_FILTER_TYPE;
//			if ((this.errorTypeFilter != null) && type.equals(this.errorTypeFilter.type))
//				return true;
//			for (int f = 0; f < this.errorTypeFilters.size(); f++) {
//				ErrorTypeFilter etf = ((ErrorTypeFilter) this.errorTypeFilters.get(f));
//				if (type.equals(etf.type)) {
//					this.errorTypeFilterSelector.setSelectedItem(etf);
//					return true;
//				}
//			}
//			return false;
//		}
//		
//		boolean setError(Error error) {
//			this.setErrorType(error.type);
//			for (int e = 0; e < this.listErrorTrays.size(); e++) {
//				ErrorTray et = ((ErrorTray) this.listErrorTrays.get(e));
//				if (et.error.equals(error)) {
//					this.errorList.setSelectedIndex(e);
//					return true;
//				}
//			}
//			return false;
//		}
//		
//		void updateErrorTypeFilter() {
//			this.errorTypeFilter = ((ErrorTypeFilter) this.errorTypeFilterSelector.getSelectedItem());
//			this.updateErrorList();
//			this.updateErrorSeverityFilters();
//			errorTypeSelected((ALL_ERRORS_FILTER_TYPE.equals(this.errorTypeFilter.type) ? null : this.errorTypeFilter.type), this.category, this.errorTypeFilter.count);
//		}
//		
//		private void updateErrorTypeFilters(boolean isChange) {
//			this.errorTypeFilters.clear();
//			ErrorTypeFilter preEtf = ((ErrorTypeFilter) this.errorTypeFilterSelector.getSelectedItem());
//			for (Iterator fit = this.errorTypeFiltersByLabel.keySet().iterator(); fit.hasNext();) {
//				ErrorTypeFilter etf = ((ErrorTypeFilter) this.errorTypeFiltersByLabel.get(fit.next()));
//				if (etf.showingCount != 0)
//					this.errorTypeFilters.add(etf);
//			}
//			if (this.errorTypeFilters.size() != 1)
//				this.errorTypeFilters.add(0, this.allErrorTypesFilter);
//			if (isChange)
//				this.errorTypeFilterModel.fireContentsChanged();
//			if ((this.errorTypeFilter == null) || !this.errorTypeFilters.contains(preEtf)) {
//				this.errorTypeFilter = ((ErrorTypeFilter) this.errorTypeFilters.get(0));
//				this.errorTypeFilterSelector.setSelectedItem(this.errorTypeFilter);
//			}
//		}
//		
//		private void updateErrorSeverityFilters() {
//			showBlockers.updateLabel(dep, this.category, this.errorTypeFilter.type);
//			showCriticals.updateLabel(dep, this.category, this.errorTypeFilter.type);
//			showMajors.updateLabel(dep, this.category, this.errorTypeFilter.type);
//			showMinors.updateLabel(dep, this.category, this.errorTypeFilter.type);
//		}
//		
//		private void updateErrorList() {
//			this.listErrorTrays.clear();
//			for (int e = 0; e < this.errorTrays.size(); e++) {
//				ErrorTray et = ((ErrorTray) this.errorTrays.get(e));
//				if (passesSeverityFilter(et.error)) {
//					if (ALL_ERRORS_FILTER_TYPE.equals(this.errorTypeFilter.type))
//						this.listErrorTrays.add(et);
//					else if (this.errorTypeFilter.type.equals(et.error.type))
//						this.listErrorTrays.add(et);
//				}
//			}
//			Collections.sort(this.errorTrays, errorTrayOrder);
//			this.errorListModel.fireContentsChanged();
//		}
//		
//		void selectError() {
//			int ei = this.errorList.getSelectedIndex();
//			while (this.listErrorTrays.size() <= ei)
//				ei--;
//			if (ei == -1)
//				return;
//			ErrorTray et = ((ErrorTray) this.listErrorTrays.get(ei));
//			errorSelected(et.error);
//		}
//		
//		void removeError(boolean falsePositive) {
//			int ei = this.errorList.getSelectedIndex();
//			if (ei == -1)
//				return;
//			if (ei < this.listErrorTrays.size())
//				this.removeError(ei, ((ErrorTray) this.listErrorTrays.get(ei)), falsePositive, true);
//		}
//		
//		void removeError(int ei, ErrorTray et, boolean falsePositive, boolean uiTriggered) {
//			
//			//	remove error
//			this.errorTrays.remove(et);
//			this.listErrorTrays.remove(et);
//			this.errorListModel.fireContentsChanged();
//			
//			//	remove error from protocol and notify anyone interested (only if we triggered the removal, though)
//			if (uiTriggered) {
//				dep.removeError(et.error);
//				ErrorProtocolDisplay.this.errorRemoved(et.error, falsePositive);
//			}
//			
//			//	anything left?
//			if (this.errorTrays.isEmpty()) {
//				errorCategoryEmpty(this);
//				return;
//			}
//			
//			//	make sure selection is in range
//			if (ei == this.errorTrays.size())
//				this.errorList.setSelectedIndex(ei-1);
//			
//			//	decrement filter counters
//			String el = dep.getErrorTypeLabel(et.error.category, et.error.type);
//			ErrorTypeFilter etf = ((ErrorTypeFilter) this.errorTypeFiltersByLabel.get(el));
//			etf.count--;
//			this.allErrorTypesFilter.count--;
//			if (passesSeverityFilter(et.error)) {
//				etf.showingCount--;
//				this.allErrorTypesFilter.showingCount--;
//			}
//			if (etf.count == 0)
//				this.errorTypeFiltersByLabel.remove(el);
//			this.updateErrorTypeFilters(true);
//			
//			//	update tab label, and notify sub class
//			errorCategoryChanged(this);
//			errorTypeSelected((ALL_ERRORS_FILTER_TYPE.equals(this.errorTypeFilter.type) ? null : this.errorTypeFilter.type), this.category, this.errorTypeFilter.count);
//			errorSelected(this.getSelectedError());
//		}
//		
//		private class ErrorFilterModel extends DefaultComboBoxModel {
//			public Object getElementAt(int index) {
//				return errorTypeFilters.get(index);
//			}
//			public int getSize() {
//				return errorTypeFilters.size();
//			}
//			public void fireContentsChanged() {
//				super.fireContentsChanged(this, 0, this.getSize());
//			}
//		}
//		
//		private class ErrorListModel extends AbstractListModel {
//			public Object getElementAt(int index) {
//				return listErrorTrays.get(index);
//			}
//			public int getSize() {
//				return listErrorTrays.size();
//			}
//			public void fireContentsChanged() {
//				super.fireContentsChanged(this, 0, this.getSize());
//			}
//		}
//	}
//	
//	private static class ErrorTypeFilter {
//		final String type;
//		final String label;
//		int count = 0;
//		int showingCount = 0;
//		ErrorTypeFilter(String type, String label) {
//			this.type = type;
//			this.label = label;
//		}
//		public String toString() {
//			return (this.label + " (" + this.showingCount + " of " + this.count + ")");
//		}
//	}
//	
//	private static class ErrorTray {
//		final DocumentErrorProtocol.Error error;
//		ErrorTray(Error error) {
//			this.error = error;
//		}
//		public String toString() {
//			return (StringUtils.capitalize(this.error.severity) + ": " + this.error.description);
//		}
//	}
//	
//	public static void main(String[] args) throws Exception {
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {}
//		
//		DocumentErrorProtocol dep = new DocumentErrorProtocol() {
//			//	TODO provide this basic counting functionality in AbstractDocumentErrorProtocol in goldengate-quality-control
//			//	TODO and maybe then some, e.g. category and type indexing, and also counting for individual severities
//			//	TODO plus false positive keeping and ID based duplicate prevention, if only based upon abstract getErrorId() method
//			private ArrayList errors = new ArrayList();
//			private CountingSet errorCounts = new CountingSet(new TreeMap(String.CASE_INSENSITIVE_ORDER));
//			public int getErrorCount(String category, String type) {
//				return this.errorCounts.getCount(category + "." + type);
//			}
//			public int getErrorSeverityCount(String category, String type, String severity) {
//				return this.errorCounts.getCount(category + "." + type + ":" + severity);
//			}
//			public Error[] getErrors(String category, String type) {
//				if (category == null)
//					return this.getErrors();
//				if (type == null)
//					return this.getErrors(category);
//				ArrayList errors = new ArrayList();
//				for (int e = 0; e < this.errors.size(); e++) {
//					if (category.equals(((DocumentErrorProtocol.Error) this.errors.get(e)).category))
//						errors.add(this.errors.get(e));
//				}
//				return ((DocumentErrorProtocol.Error[]) errors.toArray(new DocumentErrorProtocol.Error[errors.size()]));
//			}
//			public int getErrorCount(String category) {
//				return this.errorCounts.getCount(category);
//			}
//			public int getErrorSeverityCount(String category, String severity) {
//				return this.errorCounts.getCount(category + ":" + severity);
//			}
//			public Error[] getErrors(String category) {
//				if (category == null)
//					return this.getErrors();
//				ArrayList errors = new ArrayList();
//				for (int e = 0; e < this.errors.size(); e++) {
//					if (category.equals(((DocumentErrorProtocol.Error) this.errors.get(e)).category))
//						errors.add(this.errors.get(e));
//				}
//				return ((DocumentErrorProtocol.Error[]) errors.toArray(new DocumentErrorProtocol.Error[errors.size()]));
//			}
//			public int getErrorCount() {
//				return this.errors.size();
//			}
//			public int getErrorSeverityCount(String severity) {
//				return this.errorCounts.getCount(severity);
//			}
//			public Error[] getErrors() {
//				return ((DocumentErrorProtocol.Error[]) this.errors.toArray(new DocumentErrorProtocol.Error[this.errors.size()]));
//			}
//			public Attributed findErrorSubject(Attributed doc, String[] data) {
//				return null;
//			}
//			public void addError(String source, Attributed subject, Attributed parent, String category, String type, String description, String severity) {
//				this.errors.add(new DocumentErrorProtocol.Error(source, subject, category, type, description, severity) {});
//				this.errorCounts.add(category);
//				this.errorCounts.add(category + ":" + severity);
//				this.errorCounts.add(category + "." + type);
//				this.errorCounts.add(category + "." + type + ":" + severity);
//			}
//			public void removeError(Error error) {
//				this.errors.remove(error);
//				this.errorCounts.remove(error.category);
//				this.errorCounts.remove(error.category + ":" + error.severity);
//				this.errorCounts.remove(error.category + "." + error.type);
//				this.errorCounts.remove(error.category + "." + error.type + ":" + error.severity);
//			}
//			public Comparator getErrorComparator() {
//				return null;
//			}
//		};
//		dep.addErrorCategory("C1", "C1-Label", "C1-Description text");
//		dep.addErrorType("C1", "C1T1", "C1T1-Label", "C1T1-Description text");
//		dep.addError("test", null, null, "C1", "C1T1", "There is an error at 1.", Error.SEVERITY_MAJOR);
//		dep.addError("test", null, null, "C1", "C1T1", "There is an error at 2.", Error.SEVERITY_MINOR);
//		dep.addErrorType("C1", "C1T2", "C1T2-Label", "C1T2-Description text");
//		dep.addError("test", null, null, "C1", "C1T2", "There is an error at 3.", Error.SEVERITY_MAJOR);
//		dep.addError("test", null, null, "C1", "C1T2", "There is an error at 4.", Error.SEVERITY_MAJOR);
//		
//		dep.addErrorCategory("C2", "C2-Label", "C2-Description text");
//		dep.addErrorType("C2", "C2T1", "C2T1-Label", "C2T1-Description text");
//		dep.addError("test", null, null, "C2", "C2T1", "There is an error at 5.", Error.SEVERITY_MAJOR);
//		dep.addError("test", null, null, "C2", "C2T1", "There is an error at 6.", Error.SEVERITY_MAJOR);
//		dep.addErrorType("C2", "C2T2", "C2T2-Label", "C2T2-Description text");
//		dep.addError("test", null, null, "C2", "C2T2", "There is an error at 7.", Error.SEVERITY_MAJOR);
//		dep.addError("test", null, null, "C2", "C2T2", "There is an error at 8.", Error.SEVERITY_MINOR);
//		
//		ErrorProtocolDisplay epd = new ErrorProtocolDisplay(dep) {
//			protected void errorSelected(Error error) {
//				System.out.println("Selected error " + error.description);
//			}
//			protected void errorRemoved(Error error, boolean falsePositive) {
//				System.out.println("Removed error " + error.description);
//			}
//		};
//		JButton[] cbs = {
//			new JButton("Test"),
//			new JButton("Test"),
//			new JButton("Test")
//		};
//		for (int b = 0; b < cbs.length; b++)
//			cbs[b].setBorder(BorderFactory.createRaisedBevelBorder());
//		epd.setCustomButtons(cbs);
//		JOptionPane.showMessageDialog(null, epd, "Error Test", JOptionPane.PLAIN_MESSAGE);
//	}
//}

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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.plugins.tables.analyzers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.constants.TableConstants;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.tables.AnnotationTable;

/**
 * This analyzer opens a dialog in which users can merge tables, e.g. if they
 * ran across multiple pages in a printed document and are split apart after
 * OCR, but for a single logical table.
 * 
 * @author sautter
 */
public class TableMerger extends AbstractAnalyzer implements TableConstants {
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	we're not merging anything without user feedback
		if (!parameters.containsKey(INTERACTIVE_PARAMETER))
			return;
		
		//	get tables
		MutableAnnotation[] tableAnnotations = AnnotationTable.getMutableAnnotationsIgnoreCase(data, TABLE_ANNOTATION_TYPE);
		
		//	anything to merge?
		if (tableAnnotations.length < 2)
			return;
		
		//	get grid representations
		final AnnotationTable[] tables = new AnnotationTable[tableAnnotations.length];
		for (int t = 0; t < tableAnnotations.length; t++)
			tables[t] = AnnotationTable.getAnnotationTable(tableAnnotations[t]);
		
		//	check which table is mergeable with which other (same number of rows or columns)
		final JTabbedPane[] mergeTableSelectors = new JTabbedPane[tables.length];
		int maxMergeableTables = 0;
		for (int t = 0; t < tables.length; t++) {
			JTabbedPane mergeTableSelector = new JTabbedPane();
			for (int m = 0; m < t; m++) {
				if (tables[t].getColumnCount() == tables[m].getColumnCount()) {
					mergeTableSelector.addTab((AnnotationTable.getAnnotationTableLabel(tables[m], false, 4, 12) + " (column-wise)"), new TableDisplay(tables[m], false, m, 6));
				}
				if (tables[t].getRowCount() == tables[m].getRowCount()) {
					mergeTableSelector.addTab((AnnotationTable.getAnnotationTableLabel(tables[m], true, 4, 12) + " (row-wise)"), new TableDisplay(tables[m], true, m, 6));
				}
			}
			maxMergeableTables = Math.max(mergeTableSelector.getTabCount(), maxMergeableTables);
			if (mergeTableSelector.getTabCount() != 0) {
				mergeTableSelector.insertTab("Do Not Merge", null, new TableDisplay(null, false, -1, 0), null, 0);
				mergeTableSelectors[t] = mergeTableSelector;
			}
		}
		
		//	anything to merge?
		if (maxMergeableTables == 0)
			return;
		
		//	prepare feedback panel
		final JDialog feedbackDialog = DialogFactory.produceDialog("Please Check Table Mergers", true);
		final String[] feedback = {"Cancel"};
		feedbackDialog.getContentPane().setLayout(new BorderLayout());
		
		//	create feedback panel
		JPanel feedbackPanel = new JPanel(new GridLayout(0, 2, 5, 5));
		for (int t = 0; t < tables.length; t++) {
			if (mergeTableSelectors[t] == null)
				continue;
			feedbackPanel.add(mergeTableSelectors[t]);
			feedbackPanel.add(new TableDisplay(tables[t], false, -1, 8));
		}
		final TableDisplay[] mergeTables = new TableDisplay[tables.length];
		Arrays.fill(mergeTables, null);
		
		JScrollPane feedbackPanelBox = new JScrollPane(feedbackPanel);
		feedbackPanelBox.getVerticalScrollBar().setUnitIncrement(25);
		
		//	create explanation label
		JLabel explanationLabel = new JLabel("<HTML>Please select the tables to merge the ones on the right with, and whether to merge them column-wise (put one table below the other) or row-wise (put one table to the right of the other).</HTML>", JLabel.LEFT);
		explanationLabel.setOpaque(true);
		explanationLabel.setBackground(Color.WHITE);
		explanationLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.RED)), BorderFactory.createLineBorder(Color.WHITE, 2)));
		
		//	create buttons
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				feedback[0] = "Cancel";
				feedbackDialog.dispose();
			}
		});
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				feedback[0] = "OK";
				for (int t = 0; t < tables.length; t++) {
					if (mergeTableSelectors[t] != null)
						mergeTables[t] = ((TableDisplay) mergeTableSelectors[t].getSelectedComponent());
				}
				feedbackDialog.dispose();
			}
		});
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(cancel);
		buttonPanel.add(ok);
		
		//	assemble and show dialog
		feedbackDialog.getContentPane().add(explanationLabel, BorderLayout.NORTH);
		feedbackDialog.getContentPane().add(feedbackPanelBox, BorderLayout.CENTER);
		feedbackDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		feedbackDialog.setSize(600, 800);
		feedbackDialog.setResizable(true);
		feedbackDialog.setLocationRelativeTo(feedbackDialog.getOwner());
		feedbackDialog.setVisible(true);
		
		//	get feedback
		if ("Cancel".equals(feedback[0]))
			return;
		
		//	merge tables
		for (int t = (tables.length - 1); t >= 0; t--) {
			if ((mergeTables[t] == null) || (mergeTables[t].tableIndex < 0))
				continue;
			
			//	merge second table into first one
			if (!mergeTables(tableAnnotations[mergeTables[t].tableIndex], tableAnnotations[t], mergeTables[t].rowWise))
				continue;
			
			//	delete second table
			data.removeTokens(tableAnnotations[t]);
			
			//	make sure grid representation gets re-generated
			tableAnnotations[mergeTables[t].tableIndex].removeAttribute(AnnotationTable.ANNOTATION_TABLE_ATTRIBUTE);
			tables[mergeTables[t].tableIndex] = AnnotationTable.getAnnotationTable(tableAnnotations[mergeTables[t].tableIndex]);
		}
	}
	
	private boolean mergeTables(MutableAnnotation firstTable, MutableAnnotation secondTable, boolean rowWise) {
		if (rowWise) {
			AnnotationTable firstAnnotationTable = AnnotationTable.getAnnotationTable(firstTable);
			AnnotationTable secondAnnotationTable = AnnotationTable.getAnnotationTable(secondTable);
			if (firstAnnotationTable.getRowCount() != secondAnnotationTable.getRowCount()) {
				System.out.println("Cannot merge tables row-wise due to unequal row count, aborting merge.");
				return false;
			}
			
			MutableAnnotation[] firstTableRows = new MutableAnnotation[firstAnnotationTable.getRowCount()];
			for (int r = 0; r < firstAnnotationTable.getRowCount(); r++)
				firstTableRows[r] = firstAnnotationTable.getRowAt(r).getData();
			MutableAnnotation[] secondTableRows = new MutableAnnotation[secondAnnotationTable.getRowCount()];
			for (int r = 0; r < secondAnnotationTable.getRowCount(); r++)
				secondTableRows[r] = Gamta.copyDocument(secondAnnotationTable.getRowAt(r).getData());
			
			for (int r = (firstTableRows.length - 1); r >= 0; r--) {
				int firstTableRowSize = firstTableRows[r].size();
				firstTableRows[r].addTokens(secondTableRows[r]);
				Annotation[] secondTableRowAnnotations = secondTableRows[r].getAnnotations();
				for (int a = 0; a < secondTableRowAnnotations.length; a++) {
					if (TABLE_ROW_ANNOTATION_TYPE.equalsIgnoreCase(secondTableRowAnnotations[a].getType()) || TABLE_ANNOTATION_TYPE.equalsIgnoreCase(secondTableRowAnnotations[a].getType()) || DocumentRoot.DOCUMENT_TYPE.equals(secondTableRowAnnotations[a].getType()) || MutableAnnotation.PARAGRAPH_TYPE.equals(secondTableRowAnnotations[a].getType()))
						continue;
					firstTableRows[r].addAnnotation(secondTableRowAnnotations[a].getType(), (firstTableRowSize + secondTableRowAnnotations[a].getStartIndex()), secondTableRowAnnotations[a].size()).copyAttributes(secondTableRowAnnotations[a]);
				}
			}
			return true;
		}
		else {
			int firstTableSize = firstTable.size();
			DocumentRoot copyTable = Gamta.copyDocument(secondTable);
			firstTable.addTokens(copyTable);
			Annotation[] secondTableAnnotations = copyTable.getAnnotations();
			for (int a = 0; a < secondTableAnnotations.length; a++) {
				if (TABLE_ANNOTATION_TYPE.equalsIgnoreCase(secondTableAnnotations[a].getType()) || DocumentRoot.DOCUMENT_TYPE.equals(secondTableAnnotations[a].getType()) || MutableAnnotation.PARAGRAPH_TYPE.equals(secondTableAnnotations[a].getType()))
					continue;
				firstTable.addAnnotation(secondTableAnnotations[a].getType(), (firstTableSize + secondTableAnnotations[a].getStartIndex()), secondTableAnnotations[a].size()).copyAttributes(secondTableAnnotations[a]);
			}
			return true;
		}
	}
	
	private static class TableDisplay extends JLabel {
		final boolean rowWise;
		final int tableIndex;
		TableDisplay(AnnotationTable table, boolean rowWise, int tableIndex, int size) {
			super(((table == null) ? doNotMerge : getTableDisplay(table, size)), JLabel.LEFT);
			this.rowWise = rowWise;
			this.tableIndex = tableIndex;
			if (table != null) {
				this.setOpaque(true);
				this.setBackground(Color.WHITE);
			}
		}
	}
	
	private static final String doNotMerge = "Do Not Merge With Any Table";
	
	private static String getTableDisplay(AnnotationTable table, int size) {
		StringBuffer display = new StringBuffer("<HTML><table border=\"1\" frame=\"box\" rules=\"all\">");
		int displayRows = Math.min(table.getRowCount(), size);
		int displayCols = Math.min(table.getColumnCount(), size);
		for (int r = 0; r < displayRows; r++) {
			display.append("<tr>");
			for (int c = 0; c < displayCols; c++) {
				display.append("<td>");
				MutableAnnotation td = table.getCellAt(r, c).getData();
				if ((((r+1) == displayRows) && ((r+1) < table.getRowCount())) || (((c+1) == displayCols) && ((c+1) < table.getColumnCount())))
					display.append("...");
				else display.append(td.hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE) ? "&nbsp;" : AnnotationUtils.escapeForXml(td.getValue()));
				display.append("</td>");
			}
			display.append("</tr>");
		}
		display.append("</table></HTML>");
		return display.toString();
	}
	
	public static void main(String[] args) throws Exception {
		//	E:/Testdaten/EcologyTestbed/Schuster1955.normalized.xml
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
//		final DocumentRoot dr = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Testdaten/EcologyTestbed/Schuster1955.normalized.xml"), "UTF-8"));
		final DocumentRoot dr = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Testdaten/EcologyTestbed/Tarras-Wahlberg1953.normalized.xml"), "UTF-8"));
		TableMerger tm = new TableMerger();
		Properties params = new Properties();
		params.setProperty(INTERACTIVE_PARAMETER, INTERACTIVE_PARAMETER);
		tm.process(dr, params);
		
		MutableAnnotation[] tableAnnotations = AnnotationTable.getMutableAnnotationsIgnoreCase(dr, TABLE_ANNOTATION_TYPE);
		for (int t = 0; t < tableAnnotations.length; t++) {
			tableAnnotations[t].setAttribute("border", "1");
			tableAnnotations[t].setAttribute("frame", "box");
			tableAnnotations[t].setAttribute("rules", "all");
			AnnotationUtils.writeXML(tableAnnotations[t], new PrintWriter(System.out));
		}
	}
}
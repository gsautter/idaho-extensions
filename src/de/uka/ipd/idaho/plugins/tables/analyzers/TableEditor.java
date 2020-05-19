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
 *     * Neither the name of the Universitaet Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.plugins.tables.analyzers;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.constants.TableConstants;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.plugins.tables.AnnotationTable;
import de.uka.ipd.idaho.plugins.tables.AnnotationTablePanel;
import de.uka.ipd.idaho.stringUtils.StringIndex;

/**
 * This analyzer opens a dialog in which users can classify tables as well as
 * their individual rows and columns.
 * 
 * @author sautter
 */
public class TableEditor extends AbstractConfigurableAnalyzer implements TableConstants {
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		if (!parameters.containsKey(INTERACTIVE_PARAMETER))
			return;
		
		//	get tables
		MutableAnnotation[] tableAnnotations = AnnotationTable.getMutableAnnotationsIgnoreCase(data, TABLE_ANNOTATION_TYPE);
		if (tableAnnotations.length == 0)
			return;
		
		//	check which tables are yet to classify (grid representation sets attributes on construction)
		boolean[] tableClassified = new boolean[tableAnnotations.length];
		for (int t = 0; t < tableAnnotations.length; t++)
			tableClassified[t] = tableAnnotations[t].hasAttribute(LiteratureConstants.TYPE_ATTRIBUTE);
		
		//	obtain grid representations
		AnnotationTable[] tables = new AnnotationTable[tableAnnotations.length];
		for (int t = 0; t < tableAnnotations.length; t++)
			tables[t] = AnnotationTable.getAnnotationTable(tableAnnotations[t]);
		
		//	pre-classify tables and their columns and rows
		for (int t = 0; t < tables.length; t++) {
			
			//	this one's been classified before
			if (tableClassified[t])
				continue;
			
			//	count numeric cells
			int nonEmptyCellCount = 0;
			int numericCellCount = 0;
			boolean[] isEmptyRow = new boolean[tables[t].getRowCount()];
			boolean[] isNumericRow = new boolean[tables[t].getRowCount()];
			isEmptyRow[0] = false;
			isNumericRow[0] = false;
			for (int r = 1; r < tables[t].getRowCount(); r++) {
				isEmptyRow[r] = true;
				isNumericRow[r] = true;
				for (int c = 1; c < tables[t].getColumnCount(); c++) {
					if (tables[t].getCellAt(r, c).hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE))
						continue;
					nonEmptyCellCount++;
					isEmptyRow[r] = false;
					if (this.isNumeric(tables[t].getCellAt(r, c).getData()))
						numericCellCount++;
					else isNumericRow[r] = false;
				}
			}
			
			//	mostly numeric rows --> data table
			if ((numericCellCount * 4) > (nonEmptyCellCount * 3)) {
				tables[t].setTableType(AnnotationTable.DATA_TABLE_TYPE);
				
				//	first row --> header, first column --> attribute (distinction does not matter all too much in data tables, as everything comes down into cells anyway)
				tables[t].setRowTypeAt(0, AnnotationTable.HEADER_CR_TYPE);
				tables[t].setColumnTypeAt(0, AnnotationTable.ATTRIBUTE_CR_TYPE);
				
				//	classify rows
				for (int r = 1; r < tables[t].getRowCount(); r++) {
					
					//	empty row --> header
					if (isEmptyRow[r])
						tables[t].setRowTypeAt(r, AnnotationTable.GENERIC_CR_TYPE);
					
					//	numeric row --> data
					else if (isNumericRow[r])
						tables[t].setRowTypeAt(r, AnnotationTable.DATA_CR_TYPE);
					
					//	non-numeric row after header --> attribute
					else tables[t].setRowTypeAt(r, AnnotationTable.ATTRIBUTE_CR_TYPE);
				}
				
				//	all columns --> data
				for (int c = 1; c < tables[t].getColumnCount(); c++)
					tables[t].setColumnTypeAt(c, AnnotationTable.DATA_CR_TYPE);
				
				//	we're done with this one
				tableClassified[t] = true;
			}
			
			//	otherwise --> attribute table
			else {
				tables[t].setTableType(AnnotationTable.ATTRIBUTE_TABLE_TYPE);
				
				/* skip first row and column for now, assessing their semantics
				 * requires comparison to data tables, which might well follow
				 * later in the document and thus might not have been analyzed
				 * yet */
				
				//	all rows/columns --> data
				for (int r = 1; r < tables[t].getRowCount(); r++)
					tables[t].setRowTypeAt(r, AnnotationTable.DATA_CR_TYPE);
				for (int c = 1; c < tables[t].getColumnCount(); c++)
					tables[t].setColumnTypeAt(c, AnnotationTable.DATA_CR_TYPE);
			}
		}
		
		//	count entries in top rows and leading columns of data tables
		StringIndex headerEntries = new StringIndex(false);
		for (int t = 0; t < tables.length; t++) {
			if (!AnnotationTable.DATA_TABLE_TYPE.equals(tables[t].getTableType()))
				continue;
			for (int c = 0; c < tables[t].getColumnCount(); c++) {
				if (!tables[t].getCellAt(0, c).hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE))
					headerEntries.add(tables[t].getCellAt(0, c).getData().getValue());
			}
			for (int r = 0; r < tables[t].getRowCount(); r++) {
				if (!tables[t].getCellAt(r, 0).hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE))
					headerEntries.add(tables[t].getCellAt(r, 0).getData().getValue());
			}
		}
		
		//	figure out orientation of headers in attribute tables
		for (int t = 0; t < tables.length; t++) {
			
			//	this one's been classified before
			if (tableClassified[t])
				continue;
			
			//	score top row and leading column
			int trScore = 0;
			int trSize = 0;
			for (int c = 0; c < tables[t].getColumnCount(); c++)
				if (!tables[t].getCellAt(0, c).hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE)) {
					trScore += headerEntries.getCount(tables[t].getCellAt(0, c).getData().getValue());
					trSize++;
				}
			int lcScore = 0;
			int lcSize = 0;
			for (int r = 0; r < tables[t].getRowCount(); r++)
				if (!tables[t].getCellAt(r, 0).hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE)) {
					lcScore += headerEntries.getCount(tables[t].getCellAt(r, 0).getData().getValue());
					lcSize++;
				}
			
			//	headers in lead column, top row is attribute
			if ((trScore * lcSize) < (lcScore * trSize)) {
				tables[t].setRowTypeAt(0, AnnotationTable.ATTRIBUTE_CR_TYPE);
				tables[t].setColumnTypeAt(0, AnnotationTable.HEADER_CR_TYPE);
			}
			
			//	headers in top row, lead column is attribute
			else {
				tables[t].setRowTypeAt(0, AnnotationTable.HEADER_CR_TYPE);
				tables[t].setColumnTypeAt(0, AnnotationTable.ATTRIBUTE_CR_TYPE);
			}
		}
		
		//	wrap grid representations for display
		AnnotationTablePanel[] tablePanels = new AnnotationTablePanel[tableAnnotations.length];
		for (int t = 0; t < tableAnnotations.length; t++) {
			tablePanels[t] = new AnnotationTablePanel(tables[t], true);
			tablePanels[t].setPropagateChanges(false);
		}
		
		//	prepare displaying
		Container display;
		if (tablePanels.length == 1) {
			JScrollPane tableBox = new JScrollPane(tablePanels[0]);
			tableBox.getVerticalScrollBar().setUnitIncrement(50);
			tableBox.getHorizontalScrollBar().setUnitIncrement(50);
			display = tableBox;
		}
		else {
			JTabbedPane tableTabs = new JTabbedPane();
			for (int t = 0; t < tableAnnotations.length; t++) {
				JScrollPane tableBox = new JScrollPane(tablePanels[t]);
				tableBox.getVerticalScrollBar().setUnitIncrement(50);
				tableBox.getHorizontalScrollBar().setUnitIncrement(50);
				tableTabs.addTab(AnnotationTable.getAnnotationTableLabel(tables[t], true, 4, 12), tableBox);
			}
			display = tableTabs;
		}
		
		//	TODO add some label to dialog explaining what Header, Attribute, and Data mean in Attribute and Data Tables
		
		//	display tables
		final JDialog dialog = DialogFactory.produceDialog("Table Editor", true);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(display, BorderLayout.CENTER);
		JButton button = new JButton("Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				dialog.dispose();
			}
		});
		dialog.getContentPane().add(button, BorderLayout.SOUTH);
		dialog.setSize(1000, 750);
		dialog.setLocationRelativeTo(dialog.getOwner());
		dialog.setVisible(true);
	}
	
	private boolean isNumeric(TokenSequence ts) {
		for (int t = 0; t < ts.size(); t++) {
			String tok = ts.valueAt(t);
			if (Gamta.isNumber(tok))
				continue;
			if (Gamta.isPunctuation(tok))
				continue;
			if (Gamta.isRomanNumber(tok))
				continue;
			return false;
		}
		return true;
	}
}
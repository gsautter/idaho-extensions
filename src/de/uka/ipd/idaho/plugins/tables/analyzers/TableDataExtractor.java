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

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.abbreviations.AbbreviationConstants;
import de.uka.ipd.idaho.plugins.tables.AnnotationTable;
import de.uka.ipd.idaho.plugins.tables.AnnotationTable.AnnotationTableCell;

/**
 * Analyzer for extracting data from tables. In particular, this analyzer
 * explicitly attaches all data implied by a data table cell's position to the
 * very table cell.
 * 
 * @author sautter
 */
public class TableDataExtractor extends TableAnalyzer {
	
	/*
In general:
- make sure headers are either in row or in column
- make sure there is only one (continuous block of) row(s) / column(s) classified as headers

Generic Table:
- do nothing

Data Table (headers in row, so header columns cannot exist, as specified above):
- header cells in data columns are abbreviations whose attributes are resolved against attribute tables
- header cells in attribute columns are ignored
- header cells in generic columns are ignored
- attribute cells in in data columns propagate their values to any data below them
- attribute cells in attribute columns become attribute names for cell values that do not bear any more specific annotations
- attribute cells in generic columns are ignored
- data cells in attribute columns are ignored
- data cells in data columns become actual data items, with attributes imported from (in this precedence)
  - any attribute cells in the same column above them (closest attribute cell wins)
  - column header
  - detail annotations in paragraphs of type parameters
    - add scope attribute to parameter paragraphs, defaulting to global
      ==> helps with attribute table data given in text form (abbreviation marks scope)
- data cells in generic columns are ignored
- generic cells are generally ignored
	 */
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.tableHandling.TableAnalyzer#processTable(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties, de.uka.ipd.idaho.plugins.tableHandling.AnnotationTable, boolean)
	 */
	protected void processTable(MutableAnnotation data, Properties parameters, AnnotationTable table, boolean hasHeadersInColumn) {
		if (!AnnotationTable.DATA_TABLE_TYPE.equals(table.getTableType()))
			return;
		
		//	get headers
		AnnotationTableCell[] headerCells = this.getHeaderCells(table, hasHeadersInColumn);
		
		//	get header parallel attribute cells (same row as data cell, with header at column top)
		AnnotationTableCell[][] headerParallelAttributeCells = this.getHeaderParallelAttributeCells(table, hasHeadersInColumn);
		
		//	get header orthogonal attribute cells (above data cell in same column, with header at row start)
		AnnotationTableCell[][] headerOrthogonalAttributeCells = this.getHeaderOrthogonalAttributeCells(table, hasHeadersInColumn);
		
		for (int p = 0; p < headerParallelAttributeCells.length; p++) {
			
			//	row/column header missing
			if (headerParallelAttributeCells[p] == null)
				continue;
			
			//	go header by header
			for (int h = 0; h < headerCells.length; h++) {
				if (headerCells[h] == null)
					continue;
				
				//	set attributes
				this.setDataAttributes(headerCells[h], headerParallelAttributeCells[p], headerOrthogonalAttributeCells[h], p, table.getCellAt((hasHeadersInColumn ? h : p), (hasHeadersInColumn ? p : h)).getData());
			}
		}
	}
	
	private AnnotationTableCell[][] getHeaderParallelAttributeCells(AnnotationTable table, boolean hasHeadersInColumn) {
		
		//	columns and rows swapped
		if (hasHeadersInColumn) {
			AnnotationTableCell[][] attributeCells = new AnnotationTableCell[table.getColumnCount()][];
			for (int c = 0; c < table.getColumnCount(); c++) {
				if (!AnnotationTable.DATA_CR_TYPE.equals(table.getColumnTypeAt(c))) {
					attributeCells[c] = null;
					continue;
				}
				attributeCells[c] = new AnnotationTableCell[table.getRowCount()];
				for (int r = 0; r < table.getRowCount(); r++) {
					if (AnnotationTable.ATTRIBUTE_CR_TYPE.equals(table.getRowTypeAt(r)))
						attributeCells[c][r] = table.getCellAt(r, c);
					else attributeCells[c][r] = null;
				}
			}
			return attributeCells;
		}
		
		//	data organized column wise, as usual
		else {
			AnnotationTableCell[][] attributeCells = new AnnotationTableCell[table.getRowCount()][];
			for (int r = 0; r < table.getRowCount(); r++) {
				if (!AnnotationTable.DATA_CR_TYPE.equals(table.getRowTypeAt(r))) {
					attributeCells[r] = null;
					continue;
				}
				attributeCells[r] = new AnnotationTableCell[table.getColumnCount()];
				for (int c = 0; c < table.getColumnCount(); c++) {
					if (AnnotationTable.ATTRIBUTE_CR_TYPE.equals(table.getColumnTypeAt(c)))
						attributeCells[r][c] = table.getCellAt(r, c);
					else attributeCells[r][c] = null;
				}
			}
			return attributeCells;
		}
	}
	
	private AnnotationTableCell[][] getHeaderOrthogonalAttributeCells(AnnotationTable table, boolean hasHeadersInColumn) {
		
		//	columns and rows swapped
		if (hasHeadersInColumn) {
			String[] valueAttributeNames = new String[table.getColumnCount()];
			for (int r = 0; r < table.getRowCount(); r++) {
				if (!AnnotationTable.ATTRIBUTE_CR_TYPE.equals(table.getRowTypeAt(r)))
					continue;
				for (int c = 0; c < table.getColumnCount(); c++) {
					if (AnnotationTable.ATTRIBUTE_CR_TYPE.equals(table.getColumnTypeAt(c)))
						valueAttributeNames[c] = sanitizeAttributeName(table.getCellAt(r, c).getData().getValue());
					else valueAttributeNames[c] = null;
				}
			}
			AnnotationTableCell[][] attributeCells = new AnnotationTableCell[table.getRowCount()][];
			for (int r = 0; r < table.getRowCount(); r++) {
				if (!AnnotationTable.DATA_CR_TYPE.equals(table.getRowTypeAt(r))) {
					attributeCells[r] = null;
					continue;
				}
				attributeCells[r] = new AnnotationTableCell[table.getColumnCount()];
				for (int c = 0; c < table.getColumnCount(); c++) {
					if (!AnnotationTable.ATTRIBUTE_CR_TYPE.equals(table.getColumnTypeAt(c))) {
						attributeCells[r][c] = null;
						continue;
					}
					attributeCells[r][c] = table.getCellAt(r, c);
					if (attributeCells[r][c].hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE))
						attributeCells[r][c] = null;
					else if (valueAttributeNames[c] != null)
						attributeCells[r][c].setAttribute(valueAttributeNames[c], attributeCells[r][c].getData().getValue());
				}
			}
			return attributeCells;
		}
		
		//	data organized column wise, as usual
		else {
			String[] valueAttributeNames = new String[table.getRowCount()];
			for (int c = 0; c < table.getColumnCount(); c++) {
				if (!AnnotationTable.ATTRIBUTE_CR_TYPE.equals(table.getColumnTypeAt(c)))
					continue;
				for (int r = 0; r < table.getRowCount(); r++) {
					if (AnnotationTable.ATTRIBUTE_CR_TYPE.equals(table.getRowTypeAt(r)))
						valueAttributeNames[r] = sanitizeAttributeName(table.getCellAt(r, c).getData().getValue());
					else valueAttributeNames[r] = null;
				}
			}
			AnnotationTableCell[][] attributeCells = new AnnotationTableCell[table.getColumnCount()][];
			for (int c = 0; c < table.getColumnCount(); c++) {
				if (!AnnotationTable.DATA_CR_TYPE.equals(table.getColumnTypeAt(c))) {
					attributeCells[c] = null;
					continue;
				}
				attributeCells[c] = new AnnotationTableCell[table.getRowCount()];
				for (int r = 0; r < table.getRowCount(); r++) {
					if (!AnnotationTable.ATTRIBUTE_CR_TYPE.equals(table.getRowTypeAt(r))) {
						attributeCells[c][r] = null;
						continue;
					}
					attributeCells[c][r] = table.getCellAt(r, c);
					if (attributeCells[c][r].hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE))
						attributeCells[c][r] = null;
					else if (valueAttributeNames[r] != null)
						attributeCells[c][r].setAttribute(valueAttributeNames[r], attributeCells[c][r].getData().getValue());
				}
			}
			return attributeCells;
		}
	}
	
	private static String sanitizeAttributeName(String rawAttributeName) {
		return (((rawAttributeName == null) || (rawAttributeName.trim().length() == 0)) ? null : rawAttributeName.trim().replaceAll("[^0-9A-Za-z\\-\\.\\_]", "_"));
	}
	
	private void setDataAttributes(AnnotationTableCell headerCell, AnnotationTableCell[] headerParallelAttributeCells, AnnotationTableCell[] headerOrthogonalAttributeCells, int headerOrthogonalIndex, MutableAnnotation dataCell/*, Object dataRegistry*/) {
		if (dataCell.hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE))
			return;
		
		this.setDetailDataAttributes(dataCell, headerCell.getData());
		for (int p = 0; p < headerParallelAttributeCells.length; p++) {
			if (headerParallelAttributeCells[p] != null) {
				this.setDetailDataAttributes(dataCell, headerParallelAttributeCells[p].getData());
				System.out.println("header parallel data copy form " + headerParallelAttributeCells[p].getData().toXML());
			}
		}
		for (int o = 0; o < headerOrthogonalIndex; o++) {
			if (headerOrthogonalAttributeCells[o] != null) {
				this.setDetailDataAttributes(dataCell, headerOrthogonalAttributeCells[o].getData());
				System.out.println("header orthogonal data copy form " + headerOrthogonalAttributeCells[o].getData().toXML());
			}
		}
	}
	
	private void setDetailDataAttributes(MutableAnnotation dataCell, MutableAnnotation detailBearer) {
		
		//	get detail annotations
		Annotation[] details = detailBearer.getAnnotations();
		ArrayList detailList = new ArrayList();
		for (int d = 0; d < details.length; d++) {
			if (!TABLE_CELL_ANNOTATION_TYPE.equalsIgnoreCase(details[d].getType()))
				detailList.add(details[d]);
		}
		if (detailList.size() != details.length)
			details = ((Annotation[]) detailList.toArray(new Annotation[detailList.size()]));
		
		//	anything left to process?
		if (details.length == 0)
			return;
		
		//	use detail annotations, type by type
		String[] annotationTypes = detailBearer.getAnnotationTypes();
		for (int at = 0; at < annotationTypes.length; at++) {
			if (TABLE_CELL_ANNOTATION_TYPE.equalsIgnoreCase(annotationTypes[at]))
				continue;
			
			//	get type specific details
			details = detailBearer.getAnnotations(annotationTypes[at]);
			
			//	only one annotation of type, no need for numbering
			if (details.length == 1)
				this.setDetailDataAttributes(dataCell, details[0], (AbbreviationConstants.ABBREVIATION_REFERENCE_ANNOTATION_TYPE.equals(annotationTypes[at]) ? null : annotationTypes[at]));
			
			//	multiple annotations of type, number derived attributes
			else for (int d = 0; d < details.length; d++)
				this.setDetailDataAttributes(dataCell, details[d], (AbbreviationConstants.ABBREVIATION_REFERENCE_ANNOTATION_TYPE.equals(annotationTypes[at]) ? null : (annotationTypes[at] + (d+1))));
		}
	}
	
	private void setDetailDataAttributes(MutableAnnotation dataCell, Annotation detail, String namePrefix) {
		if (namePrefix != null)
			dataCell.setAttribute((namePrefix + "--" + AbbreviationConstants.ANNOTATED_VALUE_ATTRIBUTE), detail.getValue());
		String[] detailAttributeNames = detail.getAttributeNames();
		for (int a = 0; a < detailAttributeNames.length; a++) {
			String attributeName = (((namePrefix == null) ? "" : (namePrefix + "--")) + detailAttributeNames[a]);
			dataCell.setAttribute(attributeName, detail.getAttribute(detailAttributeNames[a]));
		}
	}
	
	public static void main(String[] args) throws Exception {
		//	E:/Testdaten/EcologyTestbed/Schuster1955.normalized.xml
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
//		final DocumentRoot dr = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Testdaten/EcologyTestbed/spelda2001.details.xml"), "UTF-8"));
		final DocumentRoot dr = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/SMNK-Projekt/EcologyTestbed/spelda2001.details.xml"), "UTF-8"));
		TableDataExtractor tde = new TableDataExtractor();
		Properties params = new Properties();
		params.setProperty(INTERACTIVE_PARAMETER, INTERACTIVE_PARAMETER);
		tde.process(dr, params);
		
		MutableAnnotation[] tableAnnotations = AnnotationTable.getMutableAnnotationsIgnoreCase(dr, TABLE_ANNOTATION_TYPE);
		for (int t = 0; t < tableAnnotations.length; t++) {
			tableAnnotations[t].setAttribute("border", "1");
			tableAnnotations[t].setAttribute("frame", "box");
			tableAnnotations[t].setAttribute("rules", "all");
			AnnotationUtils.writeXML(tableAnnotations[t], new PrintWriter(System.out));
		}
	}
}
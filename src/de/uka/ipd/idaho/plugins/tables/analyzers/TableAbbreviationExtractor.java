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
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.plugins.abbreviations.AbbreviationConstants;
import de.uka.ipd.idaho.plugins.tables.AnnotationTable;
import de.uka.ipd.idaho.plugins.tables.AnnotationTable.AnnotationTableCell;

/**
 * This analyzer defines abbreviations from the headings of attribute tables,
 * with each heading implying the data in the column or row it belongs to.
 * 
 * @author sautter
 */
public class TableAbbreviationExtractor extends TableAnalyzer {
	
	/*
In general:
- make sure headers are either in row or in column
- make sure there is only one (continuous block of) row(s) / column(s) classified as headers

Generic Table:
- do nothing

Attribute Table (headers in row, so header columns cannot exist, as specified above):
- header cells in data columns are abbreviations which attributes get assigned to
- header cells in attribute columns are ignored
- header cells in generic columns are ignored
- attribute cells in in data columns are ignored
- attribute cells in attribute columns are ignored
- attribute cells in generic columns are ignored
- data cells in attribute columns become attribute names for cell values that do not bear any more specific annotations
- data cells in data columns become attributes of column headers
  - if any more specific annotations exist, attributes of these annotations become attributes of column header
  - if not, cell value becomes attribute value of column header, with attribute name from attribute column
- data cells in generic columns are ignored
- generic cells are generally ignored
	 */
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.tableHandling.TableAnalyzer#processTable(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties, de.uka.ipd.idaho.plugins.tableHandling.AnnotationTable, boolean)
	 */
	protected void processTable(MutableAnnotation data, Properties parameters, AnnotationTable table, boolean hasHeadersInColumn) {
		
		//	check table type
		if (!AnnotationTable.ATTRIBUTE_TABLE_TYPE.equals(table.getTableType()))
			return;
		
		//	get headers
		AnnotationTableCell[] headerCells = this.getHeaderCells(table, hasHeadersInColumn);
		if (headerCells == null)
			return;
		
		//	get attribute names
		String[] valueAttributeNames = this.getValueAttributeNames(table, hasHeadersInColumn);
		if (valueAttributeNames == null)
			return;
		
		//	go header by header
		for (int h = 0; h < headerCells.length; h++) {
			if (headerCells[h] == null)
				continue;
			
			//	annotate header as abbreviation
			MutableAnnotation headerCell = headerCells[h].getData();
			Annotation abbreviation;
			if (headerCell.size() == 1)
				abbreviation = headerCell.addAnnotation(AbbreviationConstants.ABBREVIATION_ANNOTATION_TYPE, 0, 1);
			else {
				int abbreviationStart = 0;
				while (abbreviationStart < headerCell.size()) {
					if (Gamta.isPunctuation(headerCell.valueAt(abbreviationStart)))
						abbreviationStart++;
					else break;
				}
				if (abbreviationStart == headerCell.size())
					continue;
				int abbreviationSize = 1;
				while ((abbreviationStart + abbreviationSize) < headerCell.size()) {
					if (Gamta.isPunctuation(headerCell.valueAt(abbreviationStart + abbreviationSize)))
						break;
					else abbreviationSize++;
				}
				abbreviation = headerCell.addAnnotation(AbbreviationConstants.ABBREVIATION_ANNOTATION_TYPE, abbreviationStart, abbreviationSize);
			}
			
			//	augment abbreviation
			for (int a = 0; a < valueAttributeNames.length; a++) {
				if (valueAttributeNames[a] == null)
					continue;
				MutableAnnotation tableCell = table.getCellAt((hasHeadersInColumn ? h : a), (hasHeadersInColumn ? a : h)).getData();
				if (tableCell.hasAttribute(AnnotationTable.EMPTY_CELL_MARKER_ATTRIBUTE))
					continue;
				this.setHeaderAttributes(abbreviation, tableCell, valueAttributeNames[a]);
			}
		}
	}
	
	private void setHeaderAttributes(Annotation abbreviation, MutableAnnotation dataCell, String valueAttributeName) {
		
		//	get detail annotations
		Annotation[] details = dataCell.getAnnotations();
		ArrayList detailList = new ArrayList();
		for (int d = 0; d < details.length; d++) {
			if (!TABLE_CELL_ANNOTATION_TYPE.equalsIgnoreCase(details[d].getType()))
				detailList.add(details[d]);
		}
		if (detailList.size() != details.length)
			details = ((Annotation[]) detailList.toArray(new Annotation[detailList.size()]));
		
		//	no detail annotations, use value
		if (details.length == 0)
			abbreviation.setAttribute(valueAttributeName, dataCell.getValue());
		
		//	use detail annotations, type by type
		else {
			String[] annotationTypes = dataCell.getAnnotationTypes();
			for (int at = 0; at < annotationTypes.length; at++) {
				if (TABLE_CELL_ANNOTATION_TYPE.equalsIgnoreCase(annotationTypes[at]))
					continue;
				
				//	get type specific details
				details = dataCell.getAnnotations(annotationTypes[at]);
				
				//	only one annotation of type, no need for numbering
				if (details.length == 1)
					this.setDetailHeaderAttributes(abbreviation, details[0], annotationTypes[at]);
				
				//	multiple annotations of type, number derived attributes
				else for (int d = 0; d < details.length; d++)
					this.setDetailHeaderAttributes(abbreviation, details[d], (annotationTypes[at] + (d+1)));
			}
		}
	}
	
	private void setDetailHeaderAttributes(Annotation abbreviation, Annotation detail, String namePrefix) {
		abbreviation.setAttribute((namePrefix + "--" + AbbreviationConstants.ANNOTATED_VALUE_ATTRIBUTE), detail.getValue());
		String[] detailAttributeNames = detail.getAttributeNames();
		for (int a = 0; a < detailAttributeNames.length; a++)
			abbreviation.setAttribute((namePrefix + "--" + detailAttributeNames[a]), detail.getAttribute(detailAttributeNames[a]));
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
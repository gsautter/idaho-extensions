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

import java.util.Properties;

import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.constants.TableConstants;
import de.uka.ipd.idaho.plugins.tables.AnnotationTable;
import de.uka.ipd.idaho.plugins.tables.AnnotationTable.AnnotationTableCell;

/**
 * Analyzer doing something on individual tables. This class exists to share
 * basic functionality between several concrete analyzers.
 * 
 * @author sautter
 */
public abstract class TableAnalyzer extends AbstractConfigurableAnalyzer implements TableConstants {
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	process tables
		MutableAnnotation[] tableAnnotations = AnnotationTable.getMutableAnnotationsIgnoreCase(data, TABLE_ANNOTATION_TYPE);
		for (int t = 0; t < tableAnnotations.length; t++) {
			
			//	get grid representation
			AnnotationTable table = AnnotationTable.getAnnotationTable(tableAnnotations[t]);
			
			//	process table
			this.processTable(data, parameters, table, this.hasHeadersInColumn(table));
		}
	}
	
	/**
	 * Process an individual table.
	 * @param data the document the table lies in
	 * @param parameters the processing parameters
	 * @param table the object representation of the table
	 * @param hasHeadersInColumn are headings at the top of columns or leading the rows?
	 */
	protected abstract void processTable(MutableAnnotation data, Properties parameters, AnnotationTable table, boolean hasHeadersInColumn);
	
	private boolean hasHeadersInColumn(AnnotationTable table) {
		for (int c = 0; c < table.getColumnCount(); c++) {
			if (AnnotationTable.HEADER_CR_TYPE.equals(table.getColumnTypeAt(c)))
				return true;
		}
		return false;
	}
	
	protected AnnotationTableCell[] getHeaderCells(AnnotationTable table, boolean isColumnTable) {
		return (isColumnTable ? this.getRowHeaderCells(table) : this.getColumnHeaderCells(table));
	}
	
	private AnnotationTableCell[] getColumnHeaderCells(AnnotationTable table) {
		for (int r = 0; r < table.getRowCount(); r++) {
			System.out.println("investigating row " + r + ", type is " + table.getRowTypeAt(r));
			if (!AnnotationTable.HEADER_CR_TYPE.equals(table.getRowTypeAt(r)))
				continue;
			AnnotationTableCell[] headerCells = new AnnotationTableCell[table.getColumnCount()];
			for (int c = 0; c < table.getColumnCount(); c++) {
				System.out.println("investigating column " + c + ", type is " + table.getColumnTypeAt(c));
				if (AnnotationTable.DATA_CR_TYPE.equals(table.getColumnTypeAt(c)))
					headerCells[c] = table.getCellAt(r, c);
				else headerCells[c] = null;
			}
			return headerCells;
		}
		return null;
	}
	
	private AnnotationTableCell[] getRowHeaderCells(AnnotationTable table) {
		for (int c = 0; c < table.getColumnCount(); c++) {
			System.out.println("investigating column " + c + ", type is " + table.getColumnTypeAt(c));
			if (!AnnotationTable.HEADER_CR_TYPE.equals(table.getColumnTypeAt(c)))
				continue;
			AnnotationTableCell[] headerCells = new AnnotationTableCell[table.getRowCount()];
			for (int r = 0; r < table.getRowCount(); r++) {
				System.out.println("investigating row " + r + ", type is " + table.getRowTypeAt(r));
				if (AnnotationTable.DATA_CR_TYPE.equals(table.getRowTypeAt(r)))
					headerCells[r] = table.getCellAt(r, c);
				else headerCells[r] = null;
			}
			return headerCells;
		}
		return null;
	}
	
	protected String[] getValueAttributeNames(AnnotationTable table, boolean isColumnTable) {
		return (isColumnTable ? this.getValueColumnAttributeNames(table) : this.getValueRowAttributeNames(table));
	}
	
	private String[] getValueRowAttributeNames(AnnotationTable table) {
		for (int c = 0; c < table.getColumnCount(); c++) {
			if (!AnnotationTable.ATTRIBUTE_CR_TYPE.equals(table.getColumnTypeAt(c)))
				continue;
			String[] rowAttributeNames = new String[table.getRowCount()];
			for (int r = 0; r < table.getRowCount(); r++) {
				if (AnnotationTable.DATA_CR_TYPE.equals(table.getRowTypeAt(r)))
					rowAttributeNames[r] = sanitizeAttributeName(table.getCellAt(r, c).getData().getValue());
				else rowAttributeNames[r] = null;
			}
			return rowAttributeNames;
		}
		return null;
	}
	
	private String[] getValueColumnAttributeNames(AnnotationTable table) {
		for (int r = 0; r < table.getRowCount(); r++) {
			if (!AnnotationTable.ATTRIBUTE_CR_TYPE.equals(table.getRowTypeAt(r)))
				continue;
			String[] columnAttributeNames = new String[table.getColumnCount()];
			for (int c = 0; c < table.getColumnCount(); c++) {
				if (AnnotationTable.DATA_CR_TYPE.equals(table.getColumnTypeAt(c)))
					columnAttributeNames[c] = sanitizeAttributeName(table.getCellAt(r, c).getData().getValue());
				else columnAttributeNames[c] = null;
			}
			return columnAttributeNames;
		}
		return null;
	}
	
	private static String sanitizeAttributeName(String rawAttributeName) {
		return (((rawAttributeName == null) || (rawAttributeName.trim().length() == 0)) ? null : rawAttributeName.trim().replaceAll("[^0-9A-Za-z\\-\\.\\_]", "_"));
	}
}
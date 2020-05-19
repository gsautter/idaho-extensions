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
package de.uka.ipd.idaho.plugins.tables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.constants.TableConstants;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Grid representation of HTML tables in GAMTA documents.
 * 
 * @author sautter
 */
public class AnnotationTable extends AbstractAttributed implements TableConstants {
	
	/** the annotation attribute pointing to the object representation of a table */
	public static final String ANNOTATION_TABLE_ATTRIBUTE = "_annotationTable";
	
	/** the annotation attribute prefix holding the type of a table column */
	public static final String COLUMN_TYPE_ATTRIBUTE = "_columnType_";
	
	/**
	 * Type constant for attribute tables. In an attribute table, the values in
	 * data cells are interpreted as attributes of the table headers.
	 */
	public static final String ATTRIBUTE_TABLE_TYPE = "Attribute Table";
	
	/**
	 * Type constant for data tables. In a data table, the values in data cells
	 * are interpreted as actual data, and any attributes of table headers (e.g.
	 * from an attribute table) and attribute rows are interpreted as also being
	 * attributes of the data in the cell.
	 */
	public static final String DATA_TABLE_TYPE = "Data Table";
	
	/**
	 * Type constant for generic tables. This type is the default. Tables with
	 * this type do not participate in the attribute resolution mechanism that
	 * exists between attribute tables and data tables.
	 */
	public static final String GENERIC_TABLE_TYPE = "Generic Table";
	
	/**
	 * Type constant for columns and rows that contain attribute names. Values
	 * in data columns of attribute rows will be interpreted as attributes of
	 * the header of the data column in an attribute table. Values in attribute
	 * rows of data tables are interpreted as additional attributes for the data
	 * rows below them. All this also applies with columns and rows inverted.
	 */
	public static final String ATTRIBUTE_CR_TYPE = "Attribute";
	
	/**
	 * Type constant for columns and rows that contain data values. Values in
	 * data columns of attribute rows will be interpreted as attributes of the
	 * header of the data column in an attribute table. Values of data columns
	 * in a data table will be interpreted as the actual data. All this also
	 * applies with columns and rows inverted.
	 */
	public static final String DATA_CR_TYPE = "Data";
	
	/**
	 * Type constant for headers of columns and rows. In an attribute table,
	 * headers inherit all the attributes specified in the rows below them. In a
	 * data table, headers import any attributes assigned to them (e.g. in an
	 * attribute table) to the data values below them. All this also applies
	 * with columns and rows inverted.
	 */
	public static final String HEADER_CR_TYPE = "Header";
	
	/**
	 * Type constant for generic columns and rows. This type is the default.
	 * Columns and rows with this type are ignored in data interpretation.
	 */
	public static final String GENERIC_CR_TYPE = "Generic";
	
	
	private MutableAnnotation data;
	private String tableType = GENERIC_TABLE_TYPE;
	
	private AnnotationTableRow[] rows;
	private String[] columnTypes;
	
	private AnnotationTable(MutableAnnotation table, AnnotationTableRow[] rows) {
		this.data = table;
		this.rows = rows;
		this.columnTypes = new String[this.rows[0].cells.length];
		Arrays.fill(this.columnTypes, GENERIC_CR_TYPE);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#hasAttribute(java.lang.String)
	 */
	public boolean hasAttribute(String name) {
		return (super.hasAttribute(name) || this.data.hasAttribute(name));
	}
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		return this.getAttribute(name, null);
	}
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String, java.lang.Object)
	 */
	public Object getAttribute(String name, Object def) {
		Object value = super.getAttribute(name);
		return ((value == null) ? this.data.getAttribute(name, def) : value);
	}
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttributeNames()
	 */
	public String[] getAttributeNames() {
		StringVector ans = new StringVector();
		ans.addContentIgnoreDuplicates(super.getAttributeNames());
		ans.addContentIgnoreDuplicates(this.data.getAttributeNames());
		return ans.toStringArray();
	}
	
	/**
	 * @return the type of the table
	 */
	public String getTableType() {
		return this.tableType;
	}
	
	/**
	 * Set the type of the table.
	 * @param type the new type for the table
	 */
	public void setTableType(String type) {
		this.tableType = type;
		this.data.setAttribute(LiteratureConstants.TYPE_ATTRIBUTE, type);
	}
	
	/**
	 * @param index the index of the column to retrieve the type for
	 * @return the type of the column at the specified index
	 */
	public String getColumnTypeAt(int index) {
		return this.columnTypes[index];
	}
	
	/**
	 * @param index the index of the column to set the type for
	 * @param type the new type for the column
	 */
	public void setColumnTypeAt(int index, String type) {
		this.columnTypes[index] = type;
		this.data.setAttribute((COLUMN_TYPE_ATTRIBUTE + index), type);
	}
	
	/**
	 * @return the number of rows in the table
	 */
	public int getRowCount() {
		return this.rows.length;
	}
	
	/**
	 * @param index the index of the row to retrieve
	 * @return the row at the specified index
	 */
	public AnnotationTableRow getRowAt(int index) {
		return this.rows[index];
	}
	
	/**
	 * Shorthand for getRowAt(index).getType().
	 * @param index the index of the row to retrieve the type for
	 * @return the type of the row at the specified index
	 */
	public String getRowTypeAt(int index) {
		return this.rows[index].getType();
	}
	
	/**
	 * Shorthand for getRowAt(index).setType(type).
	 * @param index the index of the row to set the type for
	 * @param type the new type for the row
	 */
	public void setRowTypeAt(int index, String type) {
		this.rows[index].setType(type);
	}
	
	/**
	 * @return the number of columns in the table
	 */
	public int getColumnCount() {
		return this.rows[0].getCellCount();
	}
	
	/**
	 * @param row the index of the row the cell belongs to
	 * @param col the index of the cell to retrieve
	 * @return the cell at the specified index
	 */
	public AnnotationTableCell getCellAt(int row, int col) {
		return this.rows[row].getCellAt(col);
	}
	
	/**
	 * Representation of an individual cell in an annotation table
	 * 
	 * @author sautter
	 */
	public static class AnnotationTableRow extends AbstractAttributed {
		private MutableAnnotation data;
		private AnnotationTableCell[] cells;
		private boolean isHead;
		private boolean isFoot;
		private String type;
		AnnotationTableRow(MutableAnnotation data, AnnotationTableCell[] cells, boolean isHead, boolean isFoot) {
			this.data = data;
			this.cells = cells;
			this.isHead = isHead;
			this.isFoot = isFoot;
			this.type = ((String) this.data.getAttribute(LiteratureConstants.TYPE_ATTRIBUTE, (this.isHead ? HEADER_CR_TYPE : GENERIC_CR_TYPE)));
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#hasAttribute(java.lang.String)
		 */
		public boolean hasAttribute(String name) {
			return (super.hasAttribute(name) || this.data.hasAttribute(name));
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			return this.getAttribute(name, null);
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String, java.lang.Object)
		 */
		public Object getAttribute(String name, Object def) {
			Object value = super.getAttribute(name);
			return ((value == null) ? this.data.getAttribute(name, def) : value);
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttributeNames()
		 */
		public String[] getAttributeNames() {
			StringVector ans = new StringVector();
			ans.addContentIgnoreDuplicates(super.getAttributeNames());
			ans.addContentIgnoreDuplicates(this.data.getAttributeNames());
			return ans.toStringArray();
		}
		
		/**
		 * @return true if the wrapped row originates from a thead area, false
		 *         otherwise
		 */
		public boolean isHead() {
			return this.isHead;
		}
		
		/**
		 * @return true if the wrapped row originates from a tfoot area, false
		 *         otherwise
		 */
		public boolean isFoot() {
			return this.isFoot;
		}
		
		/**
		 * @return the type of the row
		 */
		public String getType() {
			return this.type;
		}
		
		/**
		 * Set the row type.
		 * @param type the new type for the row
		 */
		public void setType(String type) {
			this.type = type;
			this.data.setAttribute(LiteratureConstants.TYPE_ATTRIBUTE, type);
		}
		
		/**
		 * @return the number of cells in the row
		 */
		public int getCellCount() {
			return this.cells.length;
		}
		
		/**
		 * @param index the index of the cell to retrieve
		 * @return the cell at the specified index
		 */
		public AnnotationTableCell getCellAt(int index) {
			return this.cells[index];
		}
		
		/**
		 * @return the wrapped annotation
		 */
		public MutableAnnotation getData() {
			return this.data;
		}
	}
	
	/**
	 * Representation of an individual cell in an annotation table
	 * 
	 * @author sautter
	 */
	public static class AnnotationTableCell extends AbstractAttributed {
		private MutableAnnotation data;
		AnnotationTableCell(MutableAnnotation data) {
			this.data = data;
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#hasAttribute(java.lang.String)
		 */
		public boolean hasAttribute(String name) {
			return (super.hasAttribute(name) || this.data.hasAttribute(name));
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			return this.getAttribute(name, null);
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String, java.lang.Object)
		 */
		public Object getAttribute(String name, Object def) {
			Object value = super.getAttribute(name);
			return ((value == null) ? this.data.getAttribute(name, def) : value);
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttributeNames()
		 */
		public String[] getAttributeNames() {
			StringVector ans = new StringVector();
			ans.addContentIgnoreDuplicates(super.getAttributeNames());
			ans.addContentIgnoreDuplicates(this.data.getAttributeNames());
			return ans.toStringArray();
		}
		
		/**
		 * @return the wrapped annotation
		 */
		public MutableAnnotation getData() {
			return this.data;
		}
	}
	
	/**
	 * Constant to fill in for missing cells. This constant is used whenever a
	 * table row contains too few cells. The constant annotation table cell has
	 * no attributes, and any attempt of setting an attribute will be ignored.
	 */
	public static final AnnotationTableCell MISSING_CELL = new AnnotationTableCell(Gamta.newDocument(Gamta.newTokenSequence("", null))) {
		public boolean hasAttribute(String name) {
			return false;
		}
		public Object getAttribute(String name) {
			return null;
		}
		public Object getAttribute(String name, Object def) {
			return def;
		}
		public String[] getAttributeNames() {
			return new String[0];
		}
//		public void copyAttributes(Attributed source) {}
		public void clearAttributes() {}
		public Object setAttribute(String name, Object value) {
			return null;
		}
//		public Object removeAttribute(String name) {
//			return null;
//		}
	};
	
	/**
	 * Create the grid representation for a table in the GAMTA data model.
	 * @param table the annotation representing the table to retrieve the grid
	 *            representation of
	 * @return a grid representation of the argument table
	 * @throws IllegalArgumentException if the type of the argument mutable
	 *             annotation is not 'table', compared case insensitively, or if
	 *             the argument mutable annotation does not contain any table
	 *             rows or data cells
	 */
	public static AnnotationTable getAnnotationTable(MutableAnnotation table) {
		if (!TABLE_ANNOTATION_TYPE.equalsIgnoreCase(table.getType()))
			throw new IllegalArgumentException("Only tables can be wrapped.");
		
		//	get table areas (needed for alignment)
		MutableAnnotation[] tHeads = getMutableAnnotationsIgnoreCase(table, "thead");
		MutableAnnotation[] tFoots = getMutableAnnotationsIgnoreCase(table, "tfoot");
		MutableAnnotation[] tBodies = getMutableAnnotationsIgnoreCase(table, "tbody");
		
		//	cache head & foot IDs
		HashSet tHeadIdSet = new HashSet();
		HashSet tFootIdSet = new HashSet();
		
		//	get rows
		MutableAnnotation[] trs = getMutableAnnotationsIgnoreCase(table, TABLE_ROW_ANNOTATION_TYPE);
		
		//	complex table, need to reorganize rows in display order
		if ((tHeads.length + tFoots.length) != 0) {
			ArrayList trList = new ArrayList();
			
			//	check if groupings cover whole table
			int trCount = 0;
			for (int h = 0; h < tHeads.length; h++) {
				MutableAnnotation[] thTrs = getMutableAnnotationsIgnoreCase(tHeads[h], TABLE_ROW_ANNOTATION_TYPE);
				trCount += thTrs.length;
				for (int r = 0; r < thTrs.length; r++)
					tHeadIdSet.add(thTrs[r].getAnnotationID());
			}
			for (int f = 0; f < tFoots.length; f++) {
				MutableAnnotation[] tfTrs = getMutableAnnotationsIgnoreCase(tFoots[f], TABLE_ROW_ANNOTATION_TYPE);
				trCount += tfTrs.length;
				for (int r = 0; r < tfTrs.length; r++)
					tFootIdSet.add(tfTrs[r].getAnnotationID());
			}
			for (int b = 0; b < tBodies.length; b++) {
				MutableAnnotation[] tbTrs = getMutableAnnotationsIgnoreCase(tBodies[b], TABLE_ROW_ANNOTATION_TYPE);
				trCount += tbTrs.length;
			}
			
			//	all rows covered by groupings
			if (trCount == trs.length) {
				for (int h = 0; h < tHeads.length; h++) {
					MutableAnnotation[] thTrs = getMutableAnnotationsIgnoreCase(tHeads[h], TABLE_ROW_ANNOTATION_TYPE);
					trList.addAll(Arrays.asList(thTrs));
				}
				for (int b = 0; b < tBodies.length; b++) {
					MutableAnnotation[] tbTrs = getMutableAnnotationsIgnoreCase(tBodies[b], TABLE_ROW_ANNOTATION_TYPE);
					trList.addAll(Arrays.asList(tbTrs));
				}
				for (int f = 0; f < tFoots.length; f++) {
					MutableAnnotation[] tfTrs = getMutableAnnotationsIgnoreCase(tFoots[f], TABLE_ROW_ANNOTATION_TYPE);
					trList.addAll(Arrays.asList(tfTrs));
				}
			}
			
			//	some rows not covered by groupings
			else {
				HashSet trIdSet = new HashSet();
				for (int h = 0; h < tHeads.length; h++) {
					MutableAnnotation[] thTrs = getMutableAnnotationsIgnoreCase(tHeads[h], TABLE_ROW_ANNOTATION_TYPE);
					trList.addAll(Arrays.asList(thTrs));
					for (int r = 0; r < thTrs.length; r++)
						trIdSet.add(thTrs[r].getAnnotationID());
				}
				for (int f = 0; f < tFoots.length; f++) {
					MutableAnnotation[] tfTrs = getMutableAnnotationsIgnoreCase(tFoots[f], TABLE_ROW_ANNOTATION_TYPE);
					for (int r = 0; r < tfTrs.length; r++)
						trIdSet.add(tfTrs[r].getAnnotationID());
				}
				for (int r = 0; r < trs.length; r++) {
					if (trIdSet.add(trs[r].getAnnotationID()))
						trList.add(trs[r]);
				}
				for (int f = 0; f < tFoots.length; f++) {
					MutableAnnotation[] tfTrs = getMutableAnnotationsIgnoreCase(tFoots[f], TABLE_ROW_ANNOTATION_TYPE);
					trList.addAll(Arrays.asList(tfTrs));
				}
			}
			
			//	refresh rows
			trs = ((MutableAnnotation[]) trList.toArray(new MutableAnnotation[trList.size()]));
		}
		
		//	check if actual rows given
		if (trs.length == 0)
			throw new IllegalArgumentException("Empty tables cannot be wrapped.");
		
		//	get table cells
		MutableAnnotation[][] tds = new MutableAnnotation[trs.length][];
		for (int r = 0; r < trs.length; r++) {
			tds[r] = getMutableAnnotationsIgnoreCase(trs[r], TABLE_CELL_ANNOTATION_TYPE);
			if (tds[r].length == 0)
				tds[r] = getMutableAnnotationsIgnoreCase(trs[r], TABLE_HEADER_ANNOTATION_TYPE);
		}
		
		//	compute width
		int cols = 0;
		for (int r = 0; r < tds.length; r++) {
			int rCols = 0;
			for (int c = 0; c < tds[r].length; c++) {
				int colspan = Integer.parseInt((String) getAttributeIgnoreCase(tds[r][c], COL_SPAN_ATTRIBUTE, "1"));
				rCols += colspan;
			}
			cols = Math.max(cols, rCols);
		}
		
		//	create cell grid
		AnnotationTableCell[][] cells = new AnnotationTableCell[tds.length][cols];
		for (int r = 0; r < trs.length; r++) {
			int col = 0;
			for (int c = 0; c < tds[r].length; c++) {
				
				//	hop over columns that have been filled earlier (with cells spanning more than one row)
				while ((col < cells[r].length) && (cells[r][col] != null))
					col++;
				
				//	this row is full
				if (col == cells[r].length)
					continue;
				
				//	get cell dimensions
				int colspan = Integer.parseInt((String) getAttributeIgnoreCase(tds[r][c], COL_SPAN_ATTRIBUTE, "1"));
				int rowspan = ((colspan == cells[r].length) ? 1 : Integer.parseInt((String) getAttributeIgnoreCase(tds[r][c], ROW_SPAN_ATTRIBUTE, "1")));
				
				//	standard size cell, no need to intialize loops
				if ((colspan * rowspan) == 1) {
					cells[r][col] = new AnnotationTableCell(tds[r][c]);
					col++;
				}
				
				//	iterate over columns and rows for non-standard sized cells
				else for (int cs = 0; cs < colspan; cs++) {
					for (int rs = 0; (rs < rowspan) && ((r+rs) < cells.length); rs++)
						cells[r+rs][col] = new AnnotationTableCell(tds[r][c]);
					col++;
				}
			}
		}
		
		//	fill in missing cells
		for (int r = 0; r < cells.length; r++) {
			for (int c = 0; c < cells[r].length; c++)
				if (cells[r][c] == null)
					cells[r][c] = MISSING_CELL;
		}
		
		//	create rows
		AnnotationTableRow[] rows = new AnnotationTableRow[cells.length];
		for (int r = 0; r < cells.length; r++)
			rows[r] = new AnnotationTableRow(trs[r], cells[r], tHeadIdSet.contains(trs[r].getAnnotationID()), tFootIdSet.contains(trs[r].getAnnotationID()));
		
		//	create actual table
		AnnotationTable at = new AnnotationTable(table, rows);
		at.setTableType((String) table.getAttribute(LiteratureConstants.TYPE_ATTRIBUTE, GENERIC_TABLE_TYPE));
		for (int c = 0; c < at.getColumnCount(); c++)
			at.setColumnTypeAt(c, ((String) table.getAttribute((COLUMN_TYPE_ATTRIBUTE + c), at.getColumnTypeAt(c))));
		
		//	... and return it
		return at;
	}
	
	/**
	 * Case insensitive counterpart of data.getMutableAnnotations(String type).
	 * @param data the parent annotation
	 * @param type the type the annotations of which to retrieve
	 * @return an array holding the child annotations of the specified type,
	 *         case insensitive matching
	 */
	public static MutableAnnotation[] getMutableAnnotationsIgnoreCase(MutableAnnotation data, String type) {
		MutableAnnotation[] mas = data.getMutableAnnotations();
		if ((type == null) || (mas.length == 0))
			return mas;
		ArrayList maList = new ArrayList();
		for (int a = 0; a < mas.length; a++) {
			if (type.equalsIgnoreCase(mas[a].getType()))
				maList.add(mas[a]);
		}
		return ((MutableAnnotation[]) maList.toArray(new MutableAnnotation[maList.size()]));
	}
	
	/**
	 * Case insensitive counterpart of data.getAttribute(String type, Object
	 * def).
	 * @param data the attributed object to retrieve the attribute from
	 * @param name the name of the attribute
	 * @param def the default return value
	 * @return the value of the specified attribute, attribute names matched
	 *         case insensitively
	 */
	private static Object getAttributeIgnoreCase(Attributed data, String name, Object def) {
		Object value = data.getAttribute(name);
		if (value != null)
			return value;
		String[] ans = data.getAttributeNames();
		for (int a = 0; a < ans.length; a++) {
			if (name.equalsIgnoreCase(ans[a]))
				return data.getAttribute(ans[a], def);
		}
		return def;
	}
	
	/**
	 * Generate an HTML visualization of a table.
	 * @param table the table to generate the visualization for
	 * @param rowWise data in rows?
	 * @param size the number of columns and rows to actually render
	 * @param cellLabelLimit the length limit for individual cell values
	 * @return the HTML visualization
	 */
	public static String getAnnotationTableLabel(AnnotationTable table, boolean rowWise, int size, int cellLabelLimit) {
		StringBuffer label = new StringBuffer("<html><table>");
		
		//	render first columns (for row wise)
		if (rowWise) {
			for (int r = 0; r < Math.min(table.getRowCount(), size); r++) {
				label.append("<tr>");
				for (int c = 0; c < Math.min(table.getColumnCount(), size); c++) {
					label.append("<td>");
					if ((r*c) == 0)
						label.append("<b>");
					MutableAnnotation td = table.getCellAt(r, c).getData();
					String tdl = (td.hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE) ? "&nbsp;" : AnnotationUtils.escapeForXml(td.getValue()));
					if ((cellLabelLimit > 0) && (tdl.length() > cellLabelLimit))
						tdl = (tdl.substring(0, cellLabelLimit) + " ...");
					label.append(tdl);
					if ((r*c) == 0)
						label.append("</b>");
					label.append("</td>");
				}
				label.append("</tr>");
			}
		}
		
		//	render first rows (for column wise)
		else {
			for (int c = 0; c < Math.min(table.getColumnCount(), size); c++) {
				label.append("<tr>");
				for (int r = 0; r < Math.min(table.getRowCount(), size); r++) {
					label.append("<td>");
					if ((c*r) == 0)
						label.append("<b>");
					MutableAnnotation td = table.getCellAt(r, c).getData();
					String tdl = (td.hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE) ? "&nbsp;" : AnnotationUtils.escapeForXml(td.getValue()));
					if ((cellLabelLimit > 0) && (tdl.length() > cellLabelLimit))
						tdl = (tdl.substring(0, cellLabelLimit) + " ...");
					label.append(tdl);
					if ((c*r) == 0)
						label.append("</b>");
					label.append("</td>");
				}
				label.append("</tr>");
			}
		}
		
		label.append("</table></html>");
		return label.toString();
	}
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) throws Exception {
//		StringReader sr = new StringReader("<table>" +
//				"<thead>" +
//				"<tr>" +
//				"<th colspan=2>th1</th>" +
//				"<th>th2</th>" +
//				"<th>th3</th>" +
//				"</tr>" +
//				"</thead>" +
//				"<tfoot>" +
//				"<tr>" +
//				"<th colspan=2>tf1</th>" +
//				"<th>tf2</th>" +
//				"<th>tf3</th>" +
//				"</tr>" +
//				"</tfoot>" +
//				"<tr>" +
//				"<td colspan=2>td1.1</td>" +
//				"<td>td1.2</td>" +
//				"<td>td1.3</td>" +
//				"</tr>" +
//				"<tr>" +
//				"<td>td2.1</td>" +
//				"<td colspan=2 rowspan=2>td2.2</td>" +
//				"<td>td2.3</td>" +
//				"</tr>" +
//				"<tr>" +
//				"<td>td3.1</td>" +
//				"<td>td3.2</td>" +
//				"</tr>" +
//				"<tr>" +
//				"<td colspan=3 rowspan=2>td4.1</td>" +
//				"<td>td4.2</td>" +
//				"</tr>" +
//				"<tr>" +
//				"<td>td5.1</td>" +
//				"</tr>" +
//				"<tr>" +
//				"<td>td6.1</td>" +
//				"<td>td6.2</td>" +
//				"<td>td6.3</td>" +
//				"<td>td6.4</td>" +
//				"</tr>" +
//				"</table>");
//		DocumentRoot dr = SgmlDocumentReader.readDocument(sr);
//		AnnotationTable at = buildAnnotationTable(getMutableAnnotationsIgnoreCase(dr, "table")[0]);
//		for (int r = 0; r < at.getRowCount(); r++) {
//			AnnotationTableRow atr = at.getRowAt(r);
//			System.out.print(atr.getType());
//			for (int c = 0; c < atr.getCellCount(); c++)
//				System.out.print("\t" + atr.getCellAt(c).getData().getValue());
//			System.out.println();
//		}
//	}
}
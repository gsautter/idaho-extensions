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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.util.constants.TableConstants;
import de.uka.ipd.idaho.plugins.tables.AnnotationTable.AnnotationTableCell;
import de.uka.ipd.idaho.plugins.tables.AnnotationTable.AnnotationTableRow;

/**
 * Panel for displaying and editing a table in a Swing UI.
 * 
 * @author sautter
 */
public class AnnotationTablePanel extends JPanel implements TableConstants {
	
	private static final String[] crTypes = {
		AnnotationTable.ATTRIBUTE_CR_TYPE,
		AnnotationTable.DATA_CR_TYPE,
		AnnotationTable.HEADER_CR_TYPE,
		AnnotationTable.GENERIC_CR_TYPE,
	};
	
	private static final String[] tableTypes = {
		AnnotationTable.ATTRIBUTE_TABLE_TYPE,
		AnnotationTable.DATA_TABLE_TYPE,
		AnnotationTable.GENERIC_TABLE_TYPE,
	};
	
	private class CrDataPanel extends JPanel {
		private JComboBox typeChooser = new JComboBox(crTypes);
		private CrDataPanel propagateTo = null;
		CrDataPanel(String type, boolean forRow) {
			super((forRow ? ((LayoutManager) new GridBagLayout()) : ((LayoutManager) new BorderLayout())), true);
			this.setBorder(BorderFactory.createLineBorder(Color.GRAY, 5));
			this.setBackground(Color.GRAY);
			this.typeChooser.setSelectedItem(type);
			this.typeChooser.setEditable(false);
			this.typeChooser.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if ((propagateTo == null) || !propagateChanges)
						return;
					String newType = ((String) typeChooser.getSelectedItem());
					propagateTo.typeChooser.setSelectedItem(newType);
				}
			});
			if (forRow) {
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.gridwidth = 1;
				gbc.gridheight = 1;
				gbc.weightx = 1;
				gbc.weighty = 0;
				gbc.insets.top = 0;
				gbc.insets.bottom = 0;
				gbc.insets.left = 0;
				gbc.insets.right = 0;
				gbc.gridx = 0;
				gbc.gridy = 0;
				this.add(this.typeChooser, gbc);
			}
			else this.add(this.typeChooser, BorderLayout.SOUTH);
		}
		void setPropagateTo(CrDataPanel propagateTo) {
			this.propagateTo = propagateTo;
		}
		void addItemListener(ItemListener il) {
			this.typeChooser.addItemListener(il);
		}
		String getSelectedType() {
			return ((String) this.typeChooser.getSelectedItem());
		}
	}
	
	private class AnnotationTableCellLabel extends JLabel {
		private AnnotationTableCellBorder typeBorder;
		AnnotationTableCellLabel(String text, Color hColor, Color vColor, boolean isDataCell) {
			super(text, CENTER);
			this.setOpaque(true);
			this.setBackground(Color.WHITE);
			this.typeBorder = new AnnotationTableCellBorder(2, 3, hColor, vColor);
			this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY), this.typeBorder), BorderFactory.createLineBorder(Color.WHITE, 2)));
			if (isDataCell) {
				int ph = this.getPreferredSize().height;
				int fs = this.getFont().getSize();
				int lc = (text.replaceAll("\\<[^\\>]+\\>", "").length() + 8) / 16;
				ph += ((fs * 7 * (lc - 1)) / 4);
				this.setPreferredSize(new Dimension(Math.min(100, this.getPreferredSize().width), ph));
			}
		}
		void setRowColor(Color hColor) {
			this.typeBorder.hColor = hColor;
		}
		void setColumnColor(Color vColor) {
			this.typeBorder.vColor = vColor;
		}
	}
	
	private class AnnotationTableCellBorder extends EmptyBorder {
		Color hColor;
		Color vColor;
		AnnotationTableCellBorder(int hWidth, int vWidth, Color hColor, Color vColor) {
			super(hWidth, vWidth, hWidth, vWidth);
			this.hColor = hColor;
			this.vColor = vColor;
		}
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Insets insets = this.getBorderInsets(c);
			Color gColor = g.getColor();
			g.translate(x, y);
			
			g.setColor(this.hColor);
			g.fillRect(0, 0, width - insets.right, insets.top); // top line
			g.fillRect(insets.left, height - insets.bottom, width - insets.left, insets.bottom); // bottom line
			
			g.setColor(this.vColor);
			g.fillRect(0, insets.top, insets.left, height - insets.top); // left
			g.fillRect(width - insets.right, 0, insets.right, height - insets.bottom); // right
			
			g.translate(-x, -y);
			g.setColor(gColor);
		}
		public Insets getBorderInsets(Component c) {
			return this.getBorderInsets();
		}
		public Insets getBorderInsets(Component c, Insets insets) {
			return this.computeInsets(insets);
		}
		public Insets getBorderInsets() {
			return this.computeInsets(new Insets(0, 0, 0, 0));
		}
		private Insets computeInsets(Insets insets) {
			insets.left = this.left;
			insets.top = this.top;
			insets.right = this.right;
			insets.bottom = this.bottom;
			return insets;
		}
		public boolean isBorderOpaque() {
			return true;
		}
	}
	
	private boolean propagateChanges = true;
	
	private AnnotationTableCellLabel[][] cells;
	
	/**
	 * Constructor
	 * @param at the annotation table to display
	 */
	public AnnotationTablePanel(final AnnotationTable at, boolean forEditing) {
		super(new GridBagLayout(), true);
		
		this.cells = new AnnotationTableCellLabel[at.getRowCount()][at.getColumnCount()];
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets.top = 0;
		gbc.insets.bottom = 0;
		gbc.insets.left = 0;
		gbc.insets.right = 0;
		gbc.gridy = 0;
		gbc.gridx = 0;
		
		if (forEditing) {
			gbc.weighty = 0;
			gbc.weightx = 0;
			
			final JComboBox typeChooser = new JComboBox(tableTypes);
			typeChooser.setSelectedItem(at.getTableType());
			typeChooser.setEditable(false);
			typeChooser.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					String newType = ((String) typeChooser.getSelectedItem());
					at.setTableType(newType);
				}
			});
			JPanel typeChooserPanel = new JPanel(new BorderLayout(), true);
			typeChooserPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 5));
			typeChooserPanel.setBackground(Color.BLACK);
			typeChooserPanel.add(typeChooser, BorderLayout.SOUTH);
			
			this.add(typeChooserPanel, gbc.clone());
			
			gbc.weightx = 1;
			CrDataPanel lcCrd = null;
			for (int c = 0; c < at.getColumnCount(); c++) {
				final int col = c;
				final CrDataPanel cCrd = new CrDataPanel(at.getColumnTypeAt(c), false);
				cCrd.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						String newType = cCrd.getSelectedType();
						at.setColumnTypeAt(col, newType);
						Color cColor = getColor(newType);
						for (int r = 0; r < cells.length; r++)
							cells[r][col].setColumnColor(cColor);
						repaint();
					}
				});
				if (lcCrd != null)
					lcCrd.setPropagateTo(cCrd);
				lcCrd = cCrd;
				gbc.gridx = (c+1);
				this.add(cCrd, gbc.clone());
			}
			
			gbc.weighty = 1;
			CrDataPanel lrCrd = null;
			for (int r = 0; r < at.getRowCount(); r++) {
				final int row = r;
				final AnnotationTableRow atr = at.getRowAt(r);
				gbc.gridy = (r+1);
				
				final CrDataPanel rCrd = new CrDataPanel(atr.getType(), true);
				rCrd.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						String newType = rCrd.getSelectedType();
						atr.setType(newType);
						Color rColor = getColor(newType);
						for (int c = 0; c < cells[row].length; c++)
							cells[row][c].setRowColor(rColor);
						repaint();
					}
				});
				if (lrCrd != null)
					lrCrd.setPropagateTo(rCrd);
				lrCrd = rCrd;
				gbc.gridx = 0;
				gbc.weightx = 0;
				this.add(rCrd, gbc.clone());
				
				gbc.weightx = 1;
				for (int c = 0; c < atr.getCellCount(); c++) {
					AnnotationTableCell atc = atr.getCellAt(c);
					gbc.gridx = (c+1);
					String value = atc.getData().getValue();
					if (atc.hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE))
						value = "";
					this.cells[r][c] = new AnnotationTableCellLabel(("<HTML><CENTER>" + AnnotationUtils.escapeForXml(value) + "</CENTER></HTML>"), getColor(atr.getType()), getColor(at.getColumnTypeAt(c)), (AnnotationTable.DATA_CR_TYPE.equals(atr.getType()) && AnnotationTable.DATA_CR_TYPE.equals(at.getColumnTypeAt(c))));
					this.add(this.cells[r][c], gbc.clone());
				}
			}
		}
		
		else for (int r = 0; r < at.getRowCount(); r++) {
			AnnotationTableRow atr = at.getRowAt(r);
			gbc.gridy = r;
			for (int c = 0; c < atr.getCellCount(); c++) {
				AnnotationTableCell atc = atr.getCellAt(c);
				gbc.gridx = c;
				String value = atc.getData().getValue();
				if (atc.hasAttribute(EMPTY_CELL_MARKER_ATTRIBUTE))
					value = "";
				this.cells[r][c] = new AnnotationTableCellLabel(("<HTML><CENTER>" + AnnotationUtils.escapeForXml(value) + "</CENTER></HTML>"), Color.GRAY, Color.GRAY, (AnnotationTable.DATA_CR_TYPE.equals(atr.getType()) && AnnotationTable.DATA_CR_TYPE.equals(at.getColumnTypeAt(c))));
				this.add(this.cells[r][c], gbc.clone());
			}
		}
	}
	
	/**
	 * Retrieve the highlight color for a given column / row type. This default
	 * implementation returns Color.RED for header columns and rows, Color.GREEN
	 * for attribute columns and rows, Color.YELLOW for data columns and rows,
	 * and Color.GRAY for generic columns and rows. Sub classes may overwrite
	 * this method to provide other highlight colors.
	 * @param type the column or row type to obtain the highlight color for
	 * @return the highlicht color for the specified column or row type
	 */
	protected Color getColor(String type) {
		if (AnnotationTable.HEADER_CR_TYPE.equals(type))
			return Color.RED;
		else if (AnnotationTable.ATTRIBUTE_CR_TYPE.equals(type))
			return Color.GREEN;
		else if (AnnotationTable.DATA_CR_TYPE.equals(type))
			return Color.YELLOW;
		else return Color.GRAY;
	}
	
	/**
	 * Check if propagating changes to the type of a row or column to subsequent
	 * rows or columns is active.
	 * @return true if propagating changes is switched on, false otherwise
	 */
	public boolean isPropagatingChanges() {
		return this.propagateChanges;
	}
	
	/**
	 * Switch propagating changes to the type of a row or column to subsequent
	 * rows or columns on and off.
	 * @param propagateChanges propagate type changes?
	 */
	public void setPropagateChanges(boolean propagateChanges) {
		this.propagateChanges = propagateChanges;
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
//		StringReader sr2 = new StringReader("<table frame=\"box\" rules=\"all\" border=\"0\"><tr><td><p>abbreviation</p></td><td><p>LXM</p></td><td><p>LXMP</p></td><td><p>LXP</p></td><td><p>LXS</p></td></tr><tr><td><p>location</p></td><td><p>Muellertal,</p><p>Mardelle,</p><p>3 km SW Berdorf,</p><p>7 km WNW Echternach</p></td><td><p>Muellertal,</p><p>Mardelle,</p><p>3 km SW Berdorf,</p><p>7 km WNW Echternach</p></td><td><p>Muellertal,</p><p>Predigtstuhl,</p><p>1 km SW Berdorf,</p><p>6 km WNW Echternach</p></td><td><p>Muellertal,</p><p>„Schluchtwald&quot;,</p><p>3 km SW Berdorf</p><p>7 km WNW Echternach</p></td></tr><tr><td><p>co-ordinates</p></td><td><p>06&deg;19'E 49o48' N</p></td><td><p>06&deg;19'E 49&deg;48'N</p></td><td><p>06&quot;19'E 49o48'N</p></td><td><p>06&deg;19'E 49&deg;48'N</p></td></tr><tr><td><p>exposition</p></td><td><p>&lt;5&deg;</p></td><td><p>-</p></td><td><p>NNW / 5-30&deg;</p></td><td><p>W / 20-40&deg;</p></td></tr><tr><td><p>floristic associations</p></td><td><p>Galio-Fagetum</p></td><td><p>Piceetum</p></td><td><p>Luzulo-Fagetum</p><Galio-Fagetum</p></td></tr><tr><td><p>soil type</p></td><td><p>sandy, somewhat loamy</p></td><td><p>-</p></td><td><p>sandy</p></td><td><p>sandy, somewhat loamy</p></td></tr><tr><td><p>humus type</p></td><td><p>mull-moder to moder</p></td><td><p>-</p></td><td><p>moder</p></td><td><p>mull-moder to moder</p></td></tr><tr><td><p>pH litter (CaCI2)</p></td><td><p>4,2 (3,6-4,0)</p></td><td><p>-</p></td><td><p>3,6 (2,9-4,7)</p></td><td><p>5,3(5,1-5,7)</p></td></tr><tr><td><p>pH upper soil (CaCI2)</p></td><td><p>3,6 (3,3-4,0)</p></td><td><p>-</p></td><td><p>2,9 (2,8-3,2)</p></td><td><p>4,7 (3,8-5,5)</p></td></tr></table>");
//		StringReader sr3 = new StringReader("<table frame=\"box\" rules=\"all\" border=\"0\"><tr><td><p>Site</p></td><td><p>LXM</p></td><td><p>LXMP</p></td><td><p>LXM</p></td><td><p>LXP</p></td><td><p>LXP</p></td><td><p>LXS</p></td><td><p>LXS</p></td></tr><tr><td><p>Date</p></td><td><p>1998</p></td><td><p>1998</p></td><td><p>1999</p></td><td><p>1998</p></td><td><p>1999</p></td><td><p>1998</p></td><td><p>1999</p></td></tr><tr><td><p>Chilopoda</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Lithobiidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Lithobius aeruginosas</p></td><td>EmTaCe</td><td><p>0/0/0/1</p></td><td><p>1/0</p></td><td><p>0/1</p></td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Lithobius crassipes</p></td><td>EmTaCe</td><td><p>1/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td><p>0/2</p></td></tr><tr><td><p>Lithobius dentatus</p></td><td>EmTaCe</td><td><p>0/1</p></td><td><p>0/5</p></td><td><p>0/1</p></td><td><p>2/5</p></td><td>EmTaCe</td><td><p>2/0</p></td></tr><tr><td><p>Lithobius macilentus</p></td><td><p>0/1</p></td><td>EmTaCe</td><td><p>2/3/2/0</p></td><td>EmTaCe</td><td><p>1/0/1/0</p></td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Lithobius mutabilis</p></td><td>EmTaCe</td><td>EmTaCe</td><td><p>1/0</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Lithobius piceus</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td><p>0/1</p></td></tr><tr><td><p>Lithobius tricuspis</p></td><td><p>2/0</p></td><td>EmTaCe</td><td><p>1/1</p></td><td><p>0/3</p></td><td><p>2/0</p></td><td>EmTaCe</td><td><p>1/0</p></td></tr><tr><td><p>Cryptopidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Cryptops parisi</p></td><td><p>5</p></td><td>EmTaCe</td><td><p>2</p></td><td><p>3</p></td><td><p>6</p></td><td><p>3</p></td><td>EmTaCe</td></tr><tr><td><p>Geophilidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Geophilus alpinus</p></td><td><p>0/0/0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td><p>1/2</p></td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Strigamia acuminata</p></td><td><p>0/1</p></td><td>EmTaCe</td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Strigamia crassipes</p></td><td>EmTaCe</td><td><p>1/0</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Diplopoda</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Glomeridae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Glomeris intermedia</p></td><td><p>4/12</p></td><td><p>1/1</p></td><td><p>2/7</p></td><td><p>2/1</p></td><td><p>0/1</p></td><td><p>0/2</p></td><td><p>1/4</p></td></tr><tr><td><p>Glomeris marginata</p></td><td><p>1/6/0/1</p></td><td><p>0/1</p></td><td><p>2/4 2/2/1</p></td><td><p>3/5</p></td><td><p>2/3/2/0</p></td><td><p>1/8</p></td><td>EmTaCe</td></tr><tr><td><p>Julidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Julus scandinavius</p></td><td><p>0/2</p></td><td>EmTaCe</td><td><p>1/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Leptoiulus simplex</p></td><td><p>0/1</p></td><td>EmTaCe</td><td><p>0/1</p></td><td><p>1/0</p></td><td><p>1/2</p></td><td><p>0/4</p></td><td><p>2/1</p></td></tr><tr><td><p>Allajulus nitidus</p></td><td><p>4/8/0/4</p></td><td><p>4/4</p></td><td><p>10/6/0/1</p></td><td><p>0/3/1/0</p></td><td><p>1/6/0/3</p></td><td><p>3/1</p></td><td>EmTaCe</td></tr><tr><td><p>Cylindroiulus punctatus</p></td><td>EmTaCe</td><td><p>0/1</p></td><td><p>1/0</p></td><td>EmTaCe</td><td><p>1/2/1/0</p></td><td>EmTaCe</td><td><p>0/2/1/0</p></td></tr><tr><td><p>Tachypodoiulus niger</p></td><td><p>1/4/3/0</p></td><td><p>4/8/2/0</p></td><td><p>3/4/1/2</p></td><td><p>3/13/9/1</p></td><td><p>2/5/1/0</p></td><td><p>1/3 0/2</p></td><td>EmTaCe</td></tr><tr><td><p>Craspedosomatidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Craspedosoma rawlinsii alemannicum</p></td><td><p>1/1</p></td><td><p>2/1</p></td><td><p>7/2</p></td><td>EmTaCe</td><td><p>1/4/1</p></td><td><p>1/0</p></td><td>EmTaCe</td></tr><tr><td><p>Chordeumatidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Chordeuma sylvestre</p></td><td><p>1/1</p></td><td>EmTaCe</td><td><p>4/8/2</p></td><td><p>1/3</p></td><td><p>0/2/1</p></td><td><p>0/1</p></td><td>EmTaCe</td></tr><tr><td><p>Melogona gallica</p></td><td><p>1/0/0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Orthochordeumella pallida</p></td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Polydesmidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Polydesmus angustus</p></td><td><p>0/1</p></td><td><p>0/1</p></td><td><p>2/0</p></td><td>EmTaCe</td><td><p>1/1</p></td><td><p>0/1</p></td><td><p>1/1</p></td></tr><tr><td><p>Isopoda</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Ligiidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Ligidium hypnorum</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td><p>0/2</p></td><td><p>0/1</p></td><td>EmTaCe</td></tr><tr><td><p>Trichoniscidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Trichoniscus pusillus</p></td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td><p>0/2</p></td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Oniscidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Oniscus asellus</p></td><td>EmTaCe</td><td><p>1/2</p></td><td><p>4/7</p></td><td><p>3/3/1/0</p></td><td><p>3/3 1/1</p></td><td><p>1/0</p></td><td>EmTaCe</td></tr><tr><td><p>Philoscia sp.</p></td><td>EmTaCe</td><td>EmTaCe</td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Porcellionidae</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Porcellio monticola</p></td><td>EmTaCe</td><td><p>1/0</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr><tr><td><p>Porcellium conspersum</p></td><td><p>0/1</p></td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td><td>EmTaCe</td></tr></table>");
//		DocumentRoot dr = SgmlDocumentReader.readDocument(sr3);
//		final AnnotationTable at = AnnotationTable.buildAnnotationTable(AnnotationTable.getMutableAnnotationsIgnoreCase(dr, "table")[0]);
//		
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {}
//		
//		AnnotationTablePanel atp = new AnnotationTablePanel(at, true);
//		JFrame frame = new JFrame();
//		frame.addWindowListener(new WindowAdapter() {
//			public void windowClosed(WindowEvent we) {
//				System.exit(0);
//			}
//			public void windowClosing(WindowEvent we) {
//				System.exit(0);
//			}
//		});
//		
//		JButton b = new JButton("Print Data");
//		b.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent ae) {
//				System.out.print(at.getTableType());
//				for (int c = 0; c < at.getColumnCount(); c++)
//					System.out.print("\t" + at.getColumnTypeAt(c));
//				System.out.println();
//				for (int r = 0; r < at.getRowCount(); r++) {
//					AnnotationTableRow atr = at.getRowAt(r);
//					System.out.print(atr.getType());
//					for (int c = 0; c < atr.getCellCount(); c++)
//						System.out.print("\t" + atr.getCellAt(c).getData().getValue());
//					System.out.println();
//				}
//			}
//		});
//		
//		frame.setSize(500, 800);
//		frame.setResizable(true);
//		frame.setLocationRelativeTo(null);
//		
//		frame.getContentPane().setLayout(new BorderLayout());
//		frame.getContentPane().add(new JScrollPane(atp), BorderLayout.CENTER);
//		frame.getContentPane().add(b, BorderLayout.SOUTH);
//		frame.setVisible(true);
//	}
}
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
package de.uka.ipd.idaho.gamta.util.imaging.analyzers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageStore;

/**
 * @author sautter
 */
public class PageSelector extends AbstractConfigurableAnalyzer implements ImagingConstants {
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	are we allowed to prompt?
		if (!parameters.containsKey(INTERACTIVE_PARAMETER))
			return;
		
		//	get document ID
		String docId = ((String) data.getAttribute(DOCUMENT_ID_ATTRIBUTE));
		if (docId == null)
			docId = data.getDocumentProperty(DOCUMENT_ID_ATTRIBUTE);
		if (docId == null)
			return;
		
		//	get pages
		MutableAnnotation[] pages = data.getMutableAnnotations(PAGE_TYPE);
		
		//	any pages?
		if (pages.length == 0)
			return;
		
		//	build feedback panel
		PageSelectorFeedbackPanel psfp = new PageSelectorFeedbackPanel("Select which Pages to Retain and which to Discard");
		//	TODO set explanation label
		for (int p = 0; p < pages.length; p++) {
			//	TODO consider throwing exception instead of simply returning, so user knows what's wrong
			
			//	get page ID
			String pageIdString = ((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE));
			if ((pageIdString == null) || !pageIdString.matches("[0-9]+"))
				return; // we'll run out of bounds if we don't have an image for each and every page
			int pageId = Integer.parseInt(pageIdString);
			
			//	get page image
			PageImage pi = PageImage.getPageImage(docId, pageId);
			if (pi == null)
				return; // we'll run out of bounds if we don't have an image for each and every page
			
			//	check if we can copy page images for adjusted document ID
			if (!(pi.source instanceof PageImageStore))
				return;
			
			//	add page to panel
			psfp.addLine(docId, pageId, ("Page " + (p+1) + " of " + pages.length), pi.scaleToSize(100, 142).image, true);
		}
		psfp.addButton("OK");
		psfp.addButton("Cancel");
		
		//	get feedback
		String f = psfp.getFeedback();
		if (!"OK".equals(f))
			return;
		
		//	process feedback
		boolean pagesToCut = false;
		StringBuffer pageIdStringBuilder = new StringBuffer();
		TreeSet pageIdRangeCollector = new TreeSet();
		for (int p = 0; p < pages.length; p++) {
			if (psfp.getStateAt(p))
				pageIdRangeCollector.add(new Integer(p));
			else {
				pagesToCut = true;
				if (pageIdRangeCollector.size() == 0)
					continue;
				if (pageIdStringBuilder.length() != 0)
					pageIdStringBuilder.append(',');
				pageIdStringBuilder.append(pageIdRangeCollector.first());
				if (pageIdRangeCollector.size() > 1) {
					pageIdStringBuilder.append('-');
					pageIdStringBuilder.append(pageIdRangeCollector.last());
				}
				pageIdRangeCollector.clear();
			}
		}
		
		//	anything to do?
		if (!pagesToCut)
			return;
		
		//	compute new document ID and name
		String pageIdString = pageIdStringBuilder.toString().trim();
		String pisHash = ("" + Math.abs(pageIdString.hashCode()));
		while (pisHash.length() < 4)
			pisHash = ("0" + pisHash);
		String nDocId = (docId.substring(0, (docId.length() - pisHash.length() - 1)) + "X" + pisHash);
		
		//	copy images of retained pages
		for (int p = 0; p < pages.length; p++) {
			if (psfp.getStateAt(p)) try {
				int pageId = Integer.parseInt((String) pages[p].getAttribute(PAGE_ID_ATTRIBUTE));
				PageImage pi = PageImage.getPageImage(docId, pageId);
				//	TODO check existence of copy first
//				String piName = ((PageImageStore) pi.source).storePageImage(nDocId, pageId, pi);
//				pages[p].setAttribute(IMAGE_NAME_ATTRIBUTE, piName);
				String piName = PageImage.getPageImageName(nDocId, pageId);
				((PageImageStore) pi.source).storePageImage(piName, pi);
				pages[p].setAttribute(IMAGE_NAME_ATTRIBUTE, piName);
			}
			catch (IOException ioe) {
				System.out.println("Error copying image for page " + p + ": " + ioe.getMessage());
				ioe.printStackTrace(System.out);
				return;
			}
		}
		
		//	update document ID and name
		data.setAttribute(DOCUMENT_ID_ATTRIBUTE, nDocId);
		if (data.hasAttribute(DOCUMENT_NAME_ATTRIBUTE)) {
			String docName = ((String) data.getAttribute(DOCUMENT_NAME_ATTRIBUTE));
			data.getAttribute(DOCUMENT_NAME_ATTRIBUTE, (docName + "(" + pageIdString + ")"));
		}
		
		//	cut discarded pages
		for (int p = (pages.length-1); p >= 0; p--) {
			if (!psfp.getStateAt(p))
				data.removeTokens(pages[p]);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	// TODO Auto-generated method stub
	
	}
	
	/**
	 * Feedback panel for classification in custom categories. This style of
	 * classification is useful for tasks like itentifying caption and footnote
	 * paragraphs in the middle of the main text of a document.
	 * 
	 * @author sautter
	 */
	public static class PageSelectorFeedbackPanel extends FeedbackPanel {
		int trueSpacing = 0;
		int falseSpacing = 0;
		Color trueColor = Color.WHITE;
		Color falseColor = Color.DARK_GRAY;
		
		private LinkedList lines = new LinkedList();
		
		private GridBagLayout gbl = new GridBagLayout();
		private GridBagConstraints gbc = new GridBagConstraints();
		
		public PageSelectorFeedbackPanel() {
			this(null);
		}
		public PageSelectorFeedbackPanel(String title) {
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
		public int getComplexity() {
			return (this.lines.size() * 2);
		}
		public int getDecisionComplexity() {
			return 2;
		}
		public int getDecisionCount() {
			return this.lines.size();
		}
		void addLine(String docId, int pageId, String pageLabel, BufferedImage pageImage, boolean state) {
			if (pageImage == null) {
				PageImage pi = PageImage.getPageImage(docId, pageId);
				if (pi == null)
					return; // TODO use dummy if image not found (otherwise, states are in untractable positions)
				pageImage = pi.scaleToSize(100, 142).image;
			}
			this.gbc.gridx = (this.lines.size() % 6);
			this.gbc.gridy = (this.lines.size() / 6);
			PagePanel lp = new PagePanel(docId, pageId, pageLabel, pageImage, state, ((GridBagConstraints) this.gbc.clone()), this.lines.size());
			this.lines.add(lp);
			this.add(lp, lp.gbc);
		}
		boolean getStateAt(int index) {
			return ((PagePanel) this.lines.get(index)).checkBox.isSelected();
		}
		void setStateAt(int index, boolean state) {
			((PagePanel) this.lines.get(index)).checkBox.setSelected(state);
		}
		int lineCount() {
			return this.lines.size();
		}
		private class PagePanelBorder extends LineBorder {
			PagePanelBorder() {
				super(trueColor, 10, false);
			}
			void setLineColor(Color color) {
				this.lineColor = color;
			}
		}
		private class PageImageTray extends JPanel {
			private BufferedImage pageImage;
			PageImageTray(BufferedImage pageImage) {
				this.pageImage = pageImage;
			}
			public void paintComponent(Graphics graphics) {
				super.paintComponent(graphics);
				int pWidth = Math.min(this.getWidth(), this.pageImage.getWidth());
				int pHeight = Math.min(this.getHeight(), this.pageImage.getHeight());
				int px = ((pWidth >= this.getWidth()) ? 0 : ((this.getWidth() - pWidth) / 2));
				int py = ((pHeight >= this.getHeight()) ? 0 : ((this.getHeight() - pHeight) / 2));
				graphics.drawImage(this.pageImage, px, py, pWidth, pHeight, this);
			}
			public Dimension getPreferredSize() {
				return new Dimension(this.pageImage.getWidth(), this.pageImage.getHeight());
			}
		}
		private class PagePanel extends JPanel {
			String docId;
			int pageId;
			String pageLabel;
			JLabel pageLabelTray;
			PageImageTray pageImageTray;
			PagePanelBorder border;
			JCheckBox checkBox;
			GridBagConstraints gbc;
			PagePanel(String docId, int pageId, String pageLabel, BufferedImage pageImage, boolean state, GridBagConstraints gbc, final int position) {
				this.gbc = gbc;
				this.docId = docId;
				this.pageId = pageId;
				
				this.pageLabelTray = new JLabel(this.pageLabel, JLabel.CENTER);
				this.pageLabelTray.setOpaque(true);
				this.pageLabelTray.setBackground(trueColor);
				
				this.pageImageTray = new PageImageTray(pageImage);
				this.pageImageTray.setOpaque(true);
				this.pageImageTray.setBackground(trueColor);
				this.pageImageTray.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						//	TODO show larger page image in sub dialog (maybe undecorated and auto-closing on click elsewhere in panel)
					}
				});
				
				this.checkBox = new JCheckBox("Retain Page");
				this.checkBox.setSelected(true);
				this.checkBox.setOpaque(true);
				this.checkBox.setBackground(trueColor);
				this.checkBox.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						Color newColor = (checkBox.isSelected() ? trueColor : falseColor);
						pageLabelTray.setBackground(newColor);
						pageImageTray.setBackground(newColor);
						checkBox.setBackground(newColor);
						border.setLineColor(newColor);
						checkBox.setText(checkBox.isSelected() ? "Retain Page" : "Discard Page");
						propagateChange((position+1), checkBox.isSelected());
					}
				});
				
				this.add(this.pageLabelTray, BorderLayout.NORTH);
				this.add(this.pageImageTray, BorderLayout.CENTER);
				this.add(this.checkBox, BorderLayout.SOUTH);
				
				this.border = new PagePanelBorder();
				this.setBorder(this.border);
			}
		}
		
		private boolean inPropagation = false;
		void propagateChange(int from, boolean state) {
			if (this.inPropagation)
				return;
			this.inPropagation = true;
			for (int l = from; l < this.lines.size(); l++) {
				PagePanel lp = ((PagePanel) this.lines.get(l));
				if (lp.checkBox.isSelected() == state)
					break;
				lp.checkBox.setSelected(state);
			}
			this.inPropagation = false;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
		 */
		public void writeData(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			super.writeData(bw);
			
			//	write colors
			bw.write(getRGB(this.trueColor));
			bw.newLine();
			bw.write(getRGB(this.falseColor));
			bw.newLine();
			
			//	write spacing
			bw.write("" + this.trueSpacing);
			bw.newLine();
			bw.write("" + this.falseSpacing);
			bw.newLine();
			
			//	write data
			for (Iterator lit = this.lines.iterator(); lit.hasNext();) {
				PagePanel lp = ((PagePanel) lit.next());
				bw.write((lp.checkBox.isSelected() ? "T" : "F") + " " + URLEncoder.encode(lp.docId, "UTF-8") + " " + lp.pageId + " " + URLEncoder.encode(lp.pageLabel, "UTF-8"));
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
			
			//	read colors
			this.trueColor = getColor(br.readLine());
			this.falseColor = getColor(br.readLine());
			
			//	read spacing
			this.trueSpacing = Integer.parseInt(br.readLine());
			this.falseSpacing = Integer.parseInt(br.readLine());
			
			//	read data
			Iterator lit = this.lines.iterator();
			String line;
			while (((line = br.readLine()) != null) && (line.length() != 0)) {
				String[] lineParts = line.trim().split("\\s+");
				line = URLDecoder.decode(line.substring(2), "UTF-8");
				
				//	transferring option status only
				if ((lit != null) && lit.hasNext())
					((PagePanel) lit.next()).checkBox.setSelected("T".equals(lineParts[0]));
				
				//	transferring whole content
				else {
					lit = null;
					this.addLine(URLDecoder.decode(lineParts[1], "UTF-8"), Integer.parseInt(lineParts[2]), URLDecoder.decode(lineParts[3], "UTF-8"), null, "T".equals(lineParts[0]));
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
		 */
		public Properties getFieldStates() {
			Properties fs = new Properties();
			for (int l = 0; l < this.lineCount(); l++)
				fs.setProperty(("state" + l), (this.getStateAt(l) ? "T" : "F"));
			return fs;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
		 */
		public void setFieldStates(Properties states) {
			for (int l = 0; l < this.lineCount(); l++)
				this.setStateAt(l, "T".equals(states.getProperty(("state" + l), "F")));
		}
		
		//	TODO add HTML rendering, including JavaScript
	}
}
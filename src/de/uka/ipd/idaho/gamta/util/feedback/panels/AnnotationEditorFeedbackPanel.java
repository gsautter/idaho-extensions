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
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
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
package de.uka.ipd.idaho.gamta.util.feedback.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;

/**
 * Feedback panel for editing detail annotations of a larger, semantically
 * coarser annotation. An example of this would be identifying details in
 * bibliographic citations, like the authors' names, the year of publication, or
 * the title of the referenced publication. The annotated details cannot overlap
 * in this editor panel.
 * 
 * @author sautter
 */
public class AnnotationEditorFeedbackPanel extends FeedbackPanel {
	
	//	TODO if all tokens of a single annotation selected, offer 'Change Type to XYZ' in context menu
	
	//	TODO allow flagging detail types as 'basic' and 'advanced', the latter only showing in legend, highlights, and context menu if switched on by some toggle button
	
	//	TODO abandon multi-annotation mode, just using a single annotation to work on
	
	//	TODO click on type in legend ==> open (list of) annotated values, allowing for editing
	//	TODO add getAttribute() method, returning (a) value(s) input in above sub window and (b) what's annotated if attribute not given
	//	TODO allow switching annotated value editing on and off
	
	private String fontName = "Verdana";
	private int fontSize = 12;
	private HashMap detailTypeColors = new LinkedHashMap();
	
	private SimpleAttributeSet textStyle = new SimpleAttributeSet();
	private SimpleAttributeSet noTypeStyle = new SimpleAttributeSet();
	private HashMap detailTypeStyles = new HashMap();
	
	private boolean showDetailTypeLegend = true;
	private JPanel detailTypeLegendPanel = new JPanel(new GridLayout(0, 4, 4, 2));
	
	private AnnotationDetailEditor activePanel = null;
	
	/**
	 * @return Returns the fontName.
	 */
	public String getFontName() {
		return this.fontName;
	}
	
	/**
	 * @param fontName The fontName to set.
	 */
	public void setFontName(String fontName) {
		this.fontName = fontName;
		this.textStyle.addAttribute(StyleConstants.FontConstants.Family, this.fontName);
		this.updateStyle(true);
	}
	
	/**
	 * @return Returns the fontSize.
	 */
	public int getFontSize() {
		return this.fontSize;
	}
	
	/**
	 * @param fontSize The fontSize to set.
	 */
	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
		this.textStyle.addAttribute(StyleConstants.FontConstants.Size, new Integer(this.fontSize));
		this.updateStyle(true);
	}
	
	/**
	 * Retrieve the color used for highlighting text snippets identified to
	 * represent a detail of a given type.
	 * @param detailType the type of detail to retrieve the color for
	 * @return the color used for highlighting details of the specified type, or
	 *         null, if no color has been assigned to the specified type
	 */
	public Color getDetailColor(String detailType) {
		return ((Color) this.detailTypeColors.get(detailType));
	}
	
	/**
	 * Set the color used for highlighting text snippets identified to
	 * represent a detail of a given type.
	 * @param detailType the type of detail to retrieve the color for
	 * @param color the new highlight color for the specified detail type
	 * @return the color previously used for highlighting details of the
	 *         specified type, or null, if no color had been assigned to the
	 *         specified type before
	 */
	public Color setDetailColor(String detailType, Color color) {
		Color oldColor = ((Color) this.detailTypeColors.put(detailType, color));
		this.setDetailStyle(detailType, color);
		if (oldColor == null)
			this.updateDetails(false);
		else this.updateStyle(false);
		this.layoutLegend();
		return oldColor;
	}
	
	/**
	 * Add a detail type to the feedback panel.
	 * @param detailType the detail type to add
	 * @param color the color for highlighting the specified detail type
	 */
	public void addDetailType(String detailType, Color color) {
		if ((detailType != null) && (color != null)) {
			this.detailTypeColors.put(detailType, color);
			this.setDetailStyle(detailType, color);
			this.updateDetails(false);
			this.layoutLegend();
		}
	}
	
	/**
	 * Check whether or not to this feedback panel shows a legend of the
	 * available detail types at the top of the feedback panel. This property is
	 * set to true by default.
	 * @return true if the lebend is showing, false otherwise
	 */
	public boolean isShowingDetailTypeLegend() {
		return this.showDetailTypeLegend;
	}
	
	/**
	 * Specify whether or not to show a legend of the available detail types at
	 * the top of the feedback panel.
	 * @param show show it?
	 */
	public void setShowDetailTypeLegend(boolean show) {
		this.showDetailTypeLegend = show;
		this.remove(this.detailTypeLegendPanel);
		if (this.showDetailTypeLegend)
			this.add(this.detailTypeLegendPanel, BorderLayout.NORTH);
		this.validate();
	}
	
	private void updateDetails(boolean text) {
		for (int a = 0; a < this.annotations.size(); a++)
			((AnnotationDetailEditor) this.annotations.get(a)).initTokenStates();
		this.updateStyle(text);
	}
	
	private AttributeSet getDetailStyle(String detailType) {
		Color color = this.getDetailColor(detailType);
		if (color == null) return null;
		
		SimpleAttributeSet highlightAttributes = ((SimpleAttributeSet) this.detailTypeStyles.get(detailType));
		if (highlightAttributes == null) {
			highlightAttributes = new SimpleAttributeSet();
			highlightAttributes.addAttribute(StyleConstants.ColorConstants.Background, color);
			this.detailTypeStyles.put(detailType, highlightAttributes);
		}
		return highlightAttributes;
	}
	
	private void setDetailStyle(String detailType, Color color) {
		SimpleAttributeSet highlightAttributes = ((SimpleAttributeSet) this.detailTypeStyles.get(detailType));
		if (highlightAttributes != null)
			highlightAttributes.addAttribute(StyleConstants.ColorConstants.Background, color);
	}
	
	private void updateStyle(boolean text) {
		for (int a = 0; a < this.annotations.size(); a++)
			((AnnotationDetailEditor) this.annotations.get(a)).applySpans(text);
		this.annotationLinePanel.validate();
	}
	
	/**
	 * Retrieve the types of detail annotations that can be created and edited
	 * in this feedback panel.
	 * @return an array holding the types of detail annotations that can be created
	 * and edited in this feedback panel
	 */
	public String[] getDetailTypes() {
		return ((String[]) this.detailTypeColors.keySet().toArray(new String[this.detailTypeColors.size()]));
	}
	
	private ArrayList annotations = new ArrayList();
	private GridBagConstraints gbc = new GridBagConstraints();
	private JPanel annotationLinePanel = new JPanel(new GridBagLayout());
	
	/**
	 * Constructor
	 */
	public AnnotationEditorFeedbackPanel() {
		this(null);
	}
	
	/**
	 * Constructor
	 * @param title the title of the feedback panel
	 */
	public AnnotationEditorFeedbackPanel(String title) {
		super(title);
		this.setLayout(new BorderLayout());
		
		this.gbc.gridx = 0;
		this.gbc.gridy = 0;
		this.gbc.weightx = 1;
		this.gbc.weighty = 1;
		this.gbc.fill = GridBagConstraints.BOTH;
		this.gbc.insets.left = 5;
		this.gbc.insets.right = 5;
		this.gbc.insets.top = 3;
		this.gbc.insets.bottom = 3;
		
		this.textStyle.addAttribute(StyleConstants.FontConstants.Family, this.fontName);
		this.textStyle.addAttribute(StyleConstants.FontConstants.Size, new Integer(this.fontSize));
		
		this.noTypeStyle.addAttribute(StyleConstants.ColorConstants.Foreground, Color.BLACK);
		this.noTypeStyle.addAttribute(StyleConstants.ColorConstants.Background, Color.WHITE);
		
		this.detailTypeLegendPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		this.layoutLegend();
		this.add(this.detailTypeLegendPanel, BorderLayout.NORTH);
		this.add(this.annotationLinePanel, BorderLayout.CENTER);
	}
	
	private void layoutLegend() {
		this.detailTypeLegendPanel.removeAll();
		
		String[] types = this.getDetailTypes();
		for (int t = 0; t < types.length; t++) {
			Color color = this.getDetailColor(types[t]);
			if (color == null)
				continue;
			
			final String type = types[t];
			int rgb = color.getRGB();
			BufferedImage iconImage = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
			for (int x = 0; x < iconImage.getWidth(); x++)
				for (int y = 0; y < iconImage.getHeight(); y++)
					iconImage.setRGB(x, y, rgb);
			
			JLabel typeLabel = new JLabel(type, new ImageIcon(iconImage), JLabel.LEFT);
			typeLabel.setBorder(BorderFactory.createEtchedBorder());
			typeLabel.setToolTipText("Click to annotate selected text as '" + type + "'");
			typeLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					
					//	nothing selected
					if (activePanel == null)
						return;
					
					//	get offsets
					int startOffset = activePanel.annotationDisplay.getSelectionStart();
					int endOffset = activePanel.annotationDisplay.getSelectionEnd();
					
					//	compute indices
					final int start;
					final int end;
					
					//	no selection
					if (startOffset == endOffset)
						return;
					
					//	swap offsets if inverted
					if (endOffset < startOffset) {
						int temp = endOffset;
						endOffset = startOffset;
						startOffset = temp;
					}
					
					//	get indices
					start = activePanel.indexAtOffset(startOffset, true);
					end = activePanel.indexAtOffset(endOffset, false);
					if (end < start)
						return;
					
					//	check if some annotation in selection
					for (int t = start; t <= end; t++) {
						if (START.equals(activePanel.tokens[t].state) || CONTINUE.equals(activePanel.tokens[t].state))
							return;
					}
					
					//	annotate on left click
					if (me.getButton() == MouseEvent.BUTTON1)
						activePanel.annotate(start, end, type);
					
					//	annotate all on right click
					else activePanel.annotateAll(start, end, type);
				}
			});
			this.detailTypeLegendPanel.add(typeLabel);
		}
		this.detailTypeLegendPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		this.detailTypeLegendPanel.validate();
	}
	
	/**
	 * Add an annotation to the feedback panel to have its details edited.
	 * If the specified annotation already has details annotated, only those
	 * details are affected by the editing whose type is added to the
	 * feedback panel via the addDetailType() method. If the specified
	 * annotation has overlapping detail annotations, only the first one of
	 * each set of overlapping details will be retained.
	 * @param annotation the annotation to add.
	 */
	public void addAnnotation(MutableAnnotation annotation) {
		if (annotation != null) {
			AnnotationDetailEditor line = new AnnotationDetailEditor(annotation);
			this.annotations.add(line);
			this.gbc.gridy = this.annotations.size();
			this.gbc.weighty = annotation.size();
			this.annotationLinePanel.add(line, this.gbc.clone());
		}
	}
	
	/**
	 * Obtain an access wrapper for reading and modifying the states of
	 * individual tokens in the a-th annotation.
	 * @param a the index of the
	 * @return an access wrapper for the states of the individual tokens in the
	 *         a-th annotation
	 */
	public TokenStates getTokenStatesAt(int a) {
		return new TokenStates(this, (AnnotationDetailEditor) this.annotations.get(a));
	}
	
	/**
	 * Container for accessing and modifying the details annotated in some
	 * annotation. For modifying the states and types of the contained tokens
	 * without violating the rules stated for the setStateAt() method, it is
	 * easiest to proceed as follows:
	 * <ul>
	 * <li>First, set all states to OTHER, proceeding from the last token to
	 * the first, or use the clearStates() method for achieving the same goal</li>
	 * <li>Then, insert the actual states and types through the setStateAt()
	 * method, proceeding from the first token to the last</li>
	 * </ul>
	 * While this approach may seem somewhat cumbersome, it is necessary for
	 * preserving the consistency of the underlying data.
	 * 
	 * @author sautter
	 */
	public static class TokenStates {
		AnnotationDetailEditor data;
		AnnotationEditorFeedbackPanel parent;
		TokenStates(AnnotationEditorFeedbackPanel parent, AnnotationDetailEditor data) {
			this.parent = parent;
			this.data = data;
		}
		
		/**
		 * @return the number of tokens in the underlying annotation
		 */
		public int getTokenCount() {
			return this.data.tokens.length;
		}

		/**
		 * Retrieve the value of the t-th token in the underlying annotation.
		 * @param t the index of the desired value
		 * @return the value of the t-th token
		 */
		public String getValueAt(int t) {
			return this.data.tokens[t].value;
		}

		/**
		 * Retrieve the whitespace before the t-th token in the underlying
		 * annotation.
		 * @param t the index of the desired whitespace
		 * @return the whitespace before the t-th token
		 */
		public String getWhitespaceBefore(int t) {
			return this.data.tokens[t].space;
		}
		
		/**
		 * Retrieve the state of the t-th token. The state will be one of the
		 * START, CONTINUE, and OTHER constants.
		 * @param t the index of the desired state
		 * @return the state of the t-th token
		 */
		public String getStateAt(int t) {
			return this.data.tokens[t].state;
		}
		
		/**
		 * Retrieve the detail annotation type of the t-th token.
		 * @param t the index of the desired type
		 * @return the type of the t-th token, or null, if the state of the t-th
		 *         token is OTHER
		 */
		public String getTypeAt(int t) {
			return this.data.tokens[t].type;
		}
		
		/**
		 * Set all token states to OTHER, and all types to null. This method is
		 * intended for preparing the underlying data for writing back states
		 * that have been modified externally without violating the consistency
		 * rules of the setStateAt() method.
		 */
		public void clearStates() {
			for (int t = 0; t < this.data.tokens.length; t++) {
				this.data.tokens[t].state = OTHER;
				this.data.tokens[t].type = null;
			}
		}
		
		/**
		 * Modify the state and type of the t-th token. The state has to be one
		 * of the START, CONTINUE, and OTHER constants. Furthermore, the state
		 * of a token may not be set to CONTINUE if (a) it is the first token,
		 * or (b) the state of the previous token is OTHER. Finally, the state
		 * of a token may not be set to OTHER if the state of the subsequent
		 * token is CONTINUE. Any violation of these rules will result in an
		 * IllegalArgumentException being thrown. The type argument is ignored
		 * unless the value of the state argument is START. Then, the value of
		 * that argument has to be one of the values in the array returned by
		 * getDetailTypes(). If it is not, an IllegalArgumentException will be
		 * thrown.
		 * @param t the index of the token whose state to modify
		 * @param state the new state for the t-th token
		 * @param type the new type for the t-th token
		 * @throws IllegalArgumentException if the new state or type does not
		 *             comply with the rules specified above.
		 */
		public void setStateAt(int t, String state, String type) {
			if (!START.equals(state) && !CONTINUE.equals(state) && !OTHER.equals(state))
				throw new IllegalArgumentException("'" + state + "' is not a valid state for a token.");
			
			if (CONTINUE.equals(state) && ((t == 0) || OTHER.equals(this.data.tokens[t-1].state)))
				throw new IllegalArgumentException("'" + CONTINUE + "' may only occur after '" + START + "' or '" + CONTINUE + "'.");
			else if (OTHER.equals(state) && ((t+1) < this.data.tokens.length) && CONTINUE.equals(this.data.tokens[t+1].state))
				throw new IllegalArgumentException("'" + OTHER + "' may not occur immediately before '" + CONTINUE + "'.");
			
			if (START.equals(state) && !this.parent.detailTypeColors.containsKey(type))
				throw new IllegalArgumentException("'" + type + "' is not a valid type for this feedback panel.");
			
			this.data.tokens[t].state = state;
			
			if (START.equals(state)) {
				this.data.tokens[t].type = type;
				t++;
				while ((t < this.data.tokens.length) && CONTINUE.equals(this.data.tokens[t].state))
					this.data.tokens[t++].type = type;
			}
			
			else if (CONTINUE.equals(state))
				this.data.tokens[t].type = this.data.tokens[t-1].type;
			
			else this.data.tokens[t].type = null;
		}
	}
	
	/** the state indicating that an annotation starts at a token */
	public static final String START = "S";
	
	/** the state indicating a token continues an annotation from the previous token */
	public static final String CONTINUE = "C";
	
	/** the state indicating a token does not belong to any annotation */
	public static final String OTHER = "O";
	
	private static class AnnotationDetailEditorToken {
//		final int index;
		final int offset;
		final String space;
		final String value;
		String state = OTHER;
		String type = null;
		AnnotationDetailEditorToken(/*int index, */int offset, String space, String value) {
//			this.index = index;
			this.offset = offset;
			this.space = space;
			this.value = value;
		}
	}
	
	private static final boolean DEBUG_CONTEXT_MENU = true;
	private class AnnotationDetailEditor extends JPanel {
		private MutableAnnotation annotation;
		private AnnotationDetailEditorToken[] tokens;
		
		private JTextPane annotationDisplay = new JTextPane();
		private StyledDocument annotationDisplayDocument = this.annotationDisplay.getStyledDocument();
		private JScrollPane annotationDisplayBox = new JScrollPane(this.annotationDisplay);
		
		AnnotationDetailEditor(MutableAnnotation annotation) {
			super(new BorderLayout(), true);
			this.annotation = annotation;
			
			//	init token data
			this.tokens = new AnnotationDetailEditorToken[this.annotation.size()];
			for (int t = 0; t < this.annotation.size(); t++)
				this.tokens[t] = new AnnotationDetailEditorToken(/*t, */this.annotation.tokenAt(t).getStartOffset(), ((t == 0) ? "" : this.annotation.getWhitespaceAfter(t-1)), this.annotation.valueAt(t));
			this.initTokenStates();
			
			//	init display
			this.annotationDisplay.setEditable(false);
			
			//	display text
			StringBuffer adText = new StringBuffer();
			for (int t = 0; t < this.tokens.length; t++)
				adText.append(this.tokens[t].space + this.tokens[t].value);
			this.annotationDisplay.setText(adText.toString());
			this.applySpans(true);
			
			//	init context menu
			this.annotationDisplay.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (DEBUG_CONTEXT_MENU) System.out.println("ADE: mouse clicked at " + me.getPoint());
					if (DEBUG_CONTEXT_MENU) System.out.println(" - button is " + me.getButton());
					if (me.getButton() != MouseEvent.BUTTON1)
						displayContextMenu(me);
				}
				public void mouseReleased(MouseEvent me) {
					if (DEBUG_CONTEXT_MENU) System.out.println("ADE: mouse released at " + me.getPoint());
					if (DEBUG_CONTEXT_MENU) System.out.println(" - button is " + me.getButton());
					if (me.getButton() == MouseEvent.BUTTON1)
						activePanel = AnnotationDetailEditor.this;
				}
			});
			
			//	display it
			this.add(this.annotationDisplayBox, BorderLayout.CENTER);
		}
		
		void initTokenStates() {
			Annotation[] annotations = this.annotation.getAnnotations();
			int lastEnd = 0;
			for (int a = 0; a < annotations.length; a++) {
				if (detailTypeColors.containsKey(annotations[a].getType()) && (lastEnd <= annotations[a].getStartIndex())) {
					for (int t = lastEnd; t < annotations[a].getStartIndex(); t++) {
						this.tokens[t].state = OTHER;
						this.tokens[t].type = null;
					}
					for (int t = annotations[a].getStartIndex(); t < annotations[a].getEndIndex(); t++) {
						this.tokens[t].state = ((t == annotations[a].getStartIndex()) ? START : CONTINUE);
						this.tokens[t].type = annotations[a].getType();
					}
					lastEnd = annotations[a].getEndIndex();
				}
			}
		}
		
		void applySpans(boolean text) {
			
			//	reset text if required
			if (text)
				this.annotationDisplayDocument.setCharacterAttributes(0, this.annotationDisplayDocument.getLength(), textStyle, false);
			
			//	reset highlights
			this.annotationDisplayDocument.setCharacterAttributes(0, this.annotationDisplayDocument.getLength(), noTypeStyle, false);
			
			//	highlight annotated tokens
			for (int t = 0; t < this.tokens.length; t++) {
				if (this.tokens[t].type != null) {
					AttributeSet style = getDetailStyle(this.tokens[t].type);
					if (style != null) {
						int o = this.tokens[t].offset - (START.equals(this.tokens[t].state) ? 0 : this.tokens[t].space.length());
						int l = this.tokens[t].value.length() + (START.equals(this.tokens[t].state) ? 0 : this.tokens[t].space.length());
						this.annotationDisplayDocument.setCharacterAttributes(o, l, style, false);
					}
				}
			}
			
			//	make changes visible
			this.annotationDisplay.validate();
		}
		
		int indexAtOffset(int offset, boolean isStart) {
			for (int t = 0; t < this.tokens.length; t++)
				if (offset < (this.tokens[t].offset + (isStart ? this.tokens[t].value.length() : 1)))
					return (t - (isStart ? 0 : 1));
			return (isStart ? -1 : (this.tokens.length - 1));
		}
		
		void displayContextMenu(MouseEvent me) {
			if (DEBUG_CONTEXT_MENU) System.out.println("ADE: preparing context menu at " + me.getPoint());
			
			//	get offsets
			int startOffset = this.annotationDisplay.getSelectionStart();
			int endOffset = this.annotationDisplay.getSelectionEnd();
			if (DEBUG_CONTEXT_MENU) System.out.println(" - selection is " + startOffset + "-" + endOffset);
			
			//	compute indices
			final int start;
			final int end;
			
			//	no selection
			if (startOffset == endOffset) {
				start = this.indexAtOffset(startOffset, true);
				end = this.indexAtOffset(endOffset, false);
				if (DEBUG_CONTEXT_MENU) System.out.println(" - translates to " + start + "-" + end);
				if (start != end) {
					if (DEBUG_CONTEXT_MENU) System.out.println(" ==> invalid");
					return;
				}
				if (DEBUG_CONTEXT_MENU) System.out.println(" ==> valid");
			}
			
			//	some selection
			else {
				
				//	swap offsets if inverted
				if (endOffset < startOffset) {
					int temp = endOffset;
					endOffset = startOffset;
					startOffset = temp;
					if (DEBUG_CONTEXT_MENU) System.out.println(" - inverted to " + startOffset + "-" + endOffset);
				}
				
				//	get indices
				start = this.indexAtOffset(startOffset, true);
				end = this.indexAtOffset(endOffset, false);
				if (DEBUG_CONTEXT_MENU) System.out.println(" - translates to " + start + "-" + end);
				if (end < start) {
					if (DEBUG_CONTEXT_MENU) System.out.println(" ==> invalid");
					return;
				}
				if (DEBUG_CONTEXT_MENU) System.out.println(" ==> valid");
			}
			
			int aCount = 0;
			String aType = null;
			boolean gotOther = false;
			boolean sameType = true;
			int sameTypeCount = 0;
			for (int t = start; t <= end; t++) {
				if (START.equals(this.tokens[t].state) || ((t == start) && CONTINUE.equals(this.tokens[t].state))) {
					aCount++;
					sameType = (sameType && ((aType == null) || aType.equals(this.tokens[t].type)));
					aType = this.tokens[t].type;
				}
				else if (OTHER.equals(this.tokens[t].state))
					gotOther = true;
			}
			final String type = (sameType ? aType : null);
			if (sameType && (type != null))
				for (int t = 0; t <this.tokens.length; t++) {
					if (START.equals(this.tokens[t].state) && type.equals(this.tokens[t].type))
						sameTypeCount ++;
				}
			if (DEBUG_CONTEXT_MENU) {
				System.out.println(" - annotation count is " + aCount);
				System.out.println(" - annotation type is " + aType);
				System.out.println(" - 'other' selected is " + gotOther);
				System.out.println(" - 'sameType' selected is " + sameType);
				System.out.println(" - 'sameType' count is " + sameTypeCount);
			}
			
			JPopupMenu pm = new JPopupMenu();
			JMenuItem mi;
			
			//	annotate (all)
			if (aCount == 0) {
				String[] detailTypes = getDetailTypes();
				
				int sameValueCount = 0;
				String[] values = new String[end + 1 - start];
				for (int v = 0; v < values.length; v++)
					values[v] = this.tokens[start + v].value;
				for (int s = 0; s < (this.tokens.length - values.length); s++) {
					if (this.match(s, values, true, false))
						sameValueCount++;
				}
				
				for (int d = 0; d < detailTypes.length; d++) {
					final String detailType = detailTypes[d];
					mi = new JMenuItem("Annotate as '" + detailType + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							annotate(start, end, detailType);
						}
					});
					pm.add(mi);
					
					if (sameValueCount > 1) {
						mi = new JMenuItem("Annotate all '" + this.getValue(start, end) + "' as '" + detailType + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								annotateAll(start, end, detailType);
							}
						});
						pm.add(mi);
					}
				}
			}
			
			//	extend
			if ((aCount == 1) && gotOther) {
				mi = new JMenuItem("Extend '" + type + "' annotation");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						extend(start, end, type);
					}
				});
				pm.add(mi);
			}
			
			//	remove
			if (aCount == 1) {
				mi = new JMenuItem("Remove '" + type + "' annotation");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						remove(start, end);
					}
				});
				pm.add(mi);
				
				//	remove all
				if (sameTypeCount > 1) {
					final int aStart = this.findStart(start);
					final int aEnd = this.findEnd(end);
					
					int sameValueCount = 0;
					String[] values = new String[aEnd + 1 - aStart];
					for (int v = 0; v < values.length; v++)
						values[v] = this.tokens[aStart + v].value;
					for (int s = 0; s < (this.tokens.length - values.length); s++) {
						if (this.match(s, values, true, true) && type.equals(this.tokens[s].type) && (((s + values.length) == this.tokens.length) || !CONTINUE.equals(this.tokens[s + values.length].state)))
							sameValueCount++;
					}
					
					if (sameValueCount > 1) {
						mi = new JMenuItem("Remove all '" + type + "' annotations with value '" + this.getValue(aStart, aEnd) + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								removeAll(aStart, aEnd, type);
							}
						});
						pm.add(mi);
					}
				}
			}
			
			//	remove by type
			if (sameType && (sameTypeCount > 1)) {
				mi = new JMenuItem("Remove all '" + type + "' annotations");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						removeAll(type);
					}
				});
				pm.add(mi);
			}
			
			//	remove by span
			if (aCount > 1) {
				mi = new JMenuItem("Remove all selected annotations");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						removeAllSpanned(findStart(start), findEnd(end));
					}
				});
				pm.add(mi);
			}
			
			//	merge
			if ((aCount > 1) && sameType) {
				mi = new JMenuItem("Merge '" + type + "' annotations");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						merge(start, end);
					}
				});
				pm.add(mi);
			}
			
			//	split / cut
			if ((aCount == 1) && (start == end)) {
				String value = this.tokens[start].value;
				boolean isContinue = CONTINUE.equals(this.tokens[start].state);
				boolean gotContinue = (((start + 1) < this.tokens.length) && CONTINUE.equals(this.tokens[start + 1].state));
				
				if ((isContinue || gotContinue) && (pm.getComponentCount() != 0))
					pm.addSeparator();
				
				//	on CONTINUE token ==> split before possible, as well as cut from and cut before
				if (isContinue) {
					mi = new JMenuItem("Split '" + type + "' annotation before '" + value + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							split(start, false, true);
						}
					});
					pm.add(mi);
				}
				
				//	next is CONTINUE token ==> split around and after possible, as well as cut up to and cut after
				if (gotContinue) {
					
					//	on CONTINUE token ==> split around possible
					if (isContinue) {
						mi = new JMenuItem("Split '" + type + "' annotation around '" + value + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								split(start, false, false);
							}
						});
						pm.add(mi);
					}
					mi = new JMenuItem("Split '" + type + "' annotation after '" + value + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							split(start, true, false);
						}
					});
					pm.add(mi);
				}
				
				if (isContinue || gotContinue)
					pm.addSeparator();
				
				//	next is CONTINUE token ==> cut up to possible
				if (gotContinue) {
					mi = new JMenuItem("Cut '" + type + "' annotation up to '" + value + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							cutTo(start, true);
						}
					});
					pm.add(mi);
				}
				
				//	on CONTINUE token ==> cut from possible
				if (isContinue) {
					mi = new JMenuItem("Cut '" + type + "' annotation from '" + value + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							cutFrom(start, true);
						}
					});
					pm.add(mi);
				}
				
				if (isContinue || gotContinue)
					pm.addSeparator();
				
				//	on CONTINUE token ==> cut before possible
				if (isContinue) {
					mi = new JMenuItem("Cut '" + type + "' annotation before '" + value + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							cutTo(start, false);
						}
					});
					pm.add(mi);
				}
				
				//	next is CONTINUE token ==> cut after possible
				if (gotContinue) {
					mi = new JMenuItem("Cut '" + type + "' annotation after '" + value + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							cutFrom(start, false);
						}
					});
					pm.add(mi);
				}
			}
			if (DEBUG_CONTEXT_MENU) System.out.println(" - menu component count is " + pm.getComponentCount());
			
			if (pm.getComponentCount() != 0) {
				if (DEBUG_CONTEXT_MENU) System.out.println(" ==> showing context menu");
				pm.show(this.annotationDisplay, me.getX(), me.getY());
			}
			else if (DEBUG_CONTEXT_MENU) System.out.println(" ==> nothing to show");
		}
		
		private int findStart(int start) {
			while (OTHER.equals(this.tokens[start].state))
				start++;
			while (!START.equals(this.tokens[start].state))
				start--;
			return start;
		}
		
		private int findEnd(int end) {
			while (OTHER.equals(this.tokens[end].state))
				end--;
			while (((end + 1) < this.tokens.length) && CONTINUE.equals(this.tokens[end+1].state))
				end++;
			return end;
		}
		
		private boolean match(int start, String[] values, boolean checkStates, boolean mustBeAnnotated) {
			for (int v = 0; v < values.length; v++) {
				if ((start + v) >= this.tokens.length)
					return false;
				
				if (!this.tokens[start + v].value.equals(values[v]))
					return false;
				
				if (checkStates && (mustBeAnnotated ? !this.tokens[start + v].state.equals((v == 0) ? START : CONTINUE) : !this.tokens[start + v].state.equals(OTHER)))
					return false;
			}
			return true;
		}
		
		private String getValue(int start, int end) {
			StringBuffer value = new StringBuffer();
			for (int t = start; t <= end; t++) {
				if ((t != start) && Gamta.insertSpace(this.tokens[t-1].value, this.tokens[t].value))
					value.append(" ");
				value.append(this.tokens[t].value);
			}
			return value.toString();
		}
		
		void annotate(int start, int end, String type) {
			for (int t = start; t <= end; t++) {
				this.tokens[t].state = ((t == start) ? START : CONTINUE);
				this.tokens[t].type = type;
			}
			this.applySpans(false);
		}
		
		void annotateAll(int start, int end, String type) {
			
			//	extract match token sequence
			String[] values = new String[end + 1 - start];
			for (int v = 0; v < values.length; v++)
				values[v] = this.tokens[start + v].value;
			
			//	annotate all matches
			for (int s = 0; s < (this.tokens.length - values.length); s++) {
				if (this.match(s, values, true, false)) {
					
					//	annotate
					for (int t = 0; t < values.length; t++) {
						this.tokens[s + t].state = ((t == 0) ? START : CONTINUE);
						this.tokens[s + t].type = type;
					}
					
					//	fix overwritten start of annotation (if any)
					if (((s + values.length) < this.tokens.length) && CONTINUE.equals(this.tokens[s + values.length].state))
						this.tokens[s + values.length].state = START;
					
					//	jump to last token of added annotation (loop increment will move forward an additional token)
					s += (values.length - 1); 
				}
			}
			this.applySpans(false);
		}
		
		void extend(int start, int end, String type) {
			for (int t = start; t <= end; t++) {
				if (!CONTINUE.equals(this.tokens[t].state))
					this.tokens[t].state = ((t == start) ? START : CONTINUE);
				this.tokens[t].type = type;
			}
			this.applySpans(false);
		}
		
		void merge(int start, int end) {
			while (OTHER.equals(this.tokens[start].state))
				start++;
			while (!START.equals(this.tokens[start].state))
				start--;
			
			while (OTHER.equals(this.tokens[end].state))
				end--;
			
			for (int t = (start + 1); t <= end; t++) {
				this.tokens[t].state = CONTINUE;
				this.tokens[t].type = this.tokens[start].type;
			}
			this.applySpans(false);
		}
		
		void split(int index, boolean includeInFirst, boolean includeInSecond) {
			
			//	split after
			if (includeInFirst)
				this.tokens[index + 1].state = START;
			
			//	split before
			else if (includeInSecond)
				this.tokens[index].state = START;
			
			//	split around
			else {
				this.tokens[index].state = OTHER;
				this.tokens[index].type = null;
				this.tokens[index + 1].state = START;
			}
			
			this.applySpans(false);
		}
		
		void cutTo(int index, boolean include) {
			boolean isStart = START.equals(this.tokens[index].state);
			
			//	cut up to
			if (include) {
				this.tokens[index].state = OTHER;
				this.tokens[index].type = null;
				this.tokens[index + 1].state = START;
			}
			
			//	cut before
			else this.tokens[index].state = START;
			
			//	handle leading part of annotation
			if (!isStart)
				for (int t = (index-1); t >= 0; t--) {
					boolean breakAfter = START.equals(this.tokens[t].state);
					this.tokens[t].state = OTHER;
					this.tokens[t].type = null;
					if (breakAfter)
						t = -1;
				}
			
			this.applySpans(false);
		}
		
		void cutFrom(int index, boolean include) {
			
			//	handle tailing part of annotation
			for (int t = (index + (include ? 0 : 1)); t < this.tokens.length; t++) {
				if (CONTINUE.equals(this.tokens[t].state)) {
					this.tokens[t].state = OTHER;
					this.tokens[t].type = null;
				}
				else t = this.tokens.length;
			}
			
			this.applySpans(false);
		}
		
		void remove(int start, int end) {
			start = this.findStart(start);
			end = this.findEnd(end);
			
			for (int t = start; t <= end; t++) {
				this.tokens[t].state = OTHER;
				this.tokens[t].type = null;
			}
			this.applySpans(false);
		}
		
		void removeAll(int start, int end, String type) {
			
			//	extract match token sequence
			String[] values = new String[end + 1 - start];
			for (int v = 0; v < values.length; v++)
				values[v] = this.tokens[start + v].value;
			
			
			//	de-annotate all matches
			for (int s = 0; s < (this.tokens.length - values.length); s++) {
				if (this.match(s, values, true, true) && type.equals(this.tokens[s].type) && (((s + values.length) == this.tokens.length) || !CONTINUE.equals(this.tokens[s + values.length].state))) {
					
					//	de-annotate
					for (int t = 0; t < values.length; t++) {
						this.tokens[s + t].state = OTHER;
						this.tokens[s + t].type = null;
					}
					
					//	fix overwritten start of annotation (if any)
					if (((s + values.length) < this.tokens.length) && CONTINUE.equals(this.tokens[s + values.length].state))
						this.tokens[s + values.length].state = START;
					
					//	jump to last token of added annotation (loop increment will move forward an additional token)
					s += (values.length - 1); 
				}
			}
			this.applySpans(false);
		}
		
		void removeAll(String type) {
			
			//	de-annotate all matches
			for (int s = 0; s < this.tokens.length; s++) {
				if (type.equals(this.tokens[s].type) && START.equals(this.tokens[s].state)) {
					
					//	de-annotate
					this.tokens[s].state = OTHER;
					this.tokens[s].type = null;
					for (int t = 1; ((s + t) < this.tokens.length) && CONTINUE.equals(this.tokens[s + t].state); t++) {
						this.tokens[s + t].state = OTHER;
						this.tokens[s + t].type = null;
					}
				}
			}
			this.applySpans(false);
		}
		
		void removeAllSpanned(int start, int end) {
			
			//	de-annotate all matches
			for (int t = start; t <= end; t++) {
				this.tokens[t].state = OTHER;
				this.tokens[t].type = null;
			}
			this.applySpans(false);
		}
	}
	
	/**
	 * @return the number of annotations having their details edited in the
	 *         feedback panel
	 */
	public int annotationCount() {
		return this.annotations.size();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getComplexity()
	 */
	public int getComplexity() {
		return (this.getDecisionComplexity() + this.getDecisionCount());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionComplexity()
	 */
	public int getDecisionComplexity() {
		return (this.detailTypeColors.size() + 1); // number of detail types + 'other'
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getDecisionCount()
	 */
	public int getDecisionCount() {
		int dc = 0;
		for (int a = 0; a < this.annotations.size(); a++)
			dc += ((AnnotationDetailEditor) this.annotations.get(a)).tokens.length;
		return dc;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(java.io.Writer)
	 */
	public void writeData(Writer out) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		super.writeData(bw);
		
		//	send font layout
		bw.write(this.fontName);
		bw.newLine();
		bw.write("" + this.fontSize);
		bw.newLine();
		bw.write("" + this.showDetailTypeLegend);
		bw.newLine();
		
		//	get detail types
		String[] detailTypes = this.getDetailTypes();
		
		//	write detail types and colors (colors first, for fix length)
		for (int d = 0; d < detailTypes.length; d++) {
			bw.write(getRGB(this.getDetailColor(detailTypes[d])) + " " + URLEncoder.encode(detailTypes[d], "UTF-8"));
			bw.newLine();
		}
		bw.newLine();
		
		//	produce separator
		String separator = ("%" + Math.random() + "%");
		bw.write(separator);
		bw.newLine();
		
		//	write data
		for (int a = 0; a < this.annotations.size(); a++) {
			AnnotationDetailEditor ade = ((AnnotationDetailEditor) this.annotations.get(a));
			for (int t = 0; t < ade.tokens.length; t++) {
				bw.write(URLEncoder.encode((ade.tokens[t].space + ade.tokens[t].value), "UTF-8") + " " + ade.tokens[t].state + " " + ((ade.tokens[t].type == null) ? "" : URLEncoder.encode(ade.tokens[t].type, "UTF-8")));
				bw.newLine();
			}
			bw.write(separator);
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
		
		//	read font layout
		this.setFontName(br.readLine());
		this.setFontSize(Integer.parseInt(br.readLine()));
		this.setShowDetailTypeLegend(Boolean.parseBoolean(br.readLine()));
		
		//	read detail types and colors (colors first, for fix length)
		String detailType;
		while (((detailType = br.readLine()) != null) && (detailType.length() != 0)) {
			Color detailTypeColor = getColor(detailType.substring(0, 6));
			detailType = URLDecoder.decode(detailType.substring(7), "UTF-8");
			this.addDetailType(detailType, detailTypeColor);
		}
		
		//	get separator
		String separator = br.readLine();
		
		//	read lines
		String line;
		Iterator elit = this.annotations.iterator();
		ArrayList elTokenLineBuffer = new ArrayList();
		while (((line = br.readLine()) != null) && (line.length() != 0)) {
			
			//	separator line, evaluate buffered data lines
			if (separator.equals(line)) {
				
				//	initialize data structures
				StringBuffer elString = new StringBuffer("");
				String[] elTokenStates = new String[elTokenLineBuffer.size()];
				String[] elTokenTypes = new String[elTokenLineBuffer.size()];
				
				//	parse buffered data lines
				for (int t = 0; t < elTokenLineBuffer.size(); t++) {
					String[] elTokenData = ((String) elTokenLineBuffer.get(t)).split("\\s");
					elString.append(URLDecoder.decode(elTokenData[0], "UTF-8"));
					elTokenStates[t] = elTokenData[1];
					elTokenTypes[t] = ((elTokenData.length == 2) ? null : URLDecoder.decode(elTokenData[2], "UTF-8"));
				}
				
				//	get or create editor line
				AnnotationDetailEditor ade;
				if ((elit != null) && elit.hasNext())
					ade = ((AnnotationDetailEditor) elit.next());
				else {
					MutableAnnotation elAnnotation = Gamta.newDocument(Gamta.newTokenSequence(elString, null));
					this.addAnnotation(elAnnotation);
					ade = ((AnnotationDetailEditor) this.annotations.get(this.annotations.size() - 1));
					elit = null;
				}
				
				//	transfer token states
				for (int t = 0; t < ade.tokens.length; t++) {
					ade.tokens[t].state = elTokenStates[t];
					ade.tokens[t].type = elTokenTypes[t];
				}
				ade.applySpans(true);
				
				//	clean up data buffer
				elTokenLineBuffer.clear();
			}
			
			//	normal data line, just buffer it for now
			else elTokenLineBuffer.add(line);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getFieldStates()
	 */
	public Properties getFieldStates() {
		Properties fs = new Properties();
		for (int a = 0; a < this.annotationCount(); a++) {
			TokenStates ts = this.getTokenStatesAt(a);
			for (int t = 0; t < ts.getTokenCount(); t++) {
				fs.setProperty(("part" + a + "_token" + t + "_state"), ts.getStateAt(t));
				if (START.equals(ts.getStateAt(t)))
					fs.setProperty(("part" + a + "_token" + t + "_type"), ts.getTypeAt(t));
			}
		}
		return fs;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#setFieldStates(java.util.Properties)
	 */
	public void setFieldStates(Properties states) {
		for (int a = 0; a < this.annotationCount(); a++) {
			TokenStates ts = this.getTokenStatesAt(a);
			ts.clearStates();
			for (int t = 0; t < ts.getTokenCount(); t++) {
				String state = states.getProperty(("part" + a + "_token" + t + "_state"), OTHER);
				String type = (START.equals(state) ? states.getProperty("part" + a + "_token" + t + "_type") : "");
				ts.setStateAt(t, state, type);
			}
			((AnnotationDetailEditor) this.annotations.get(a)).applySpans(true);
		}
	}
	
	/**
	 * Write the current status of all annotations contained in this feedback
	 * panel through to the actual annotations. This has the same effect as
	 * invoking writeChanges(TokenStates, MutableAnnotation) for every
	 * annotation contained in this feedback panel.
	 */
	public void writeChanges() {
		for (int a = 0; a < this.annotations.size(); a++) {
			AnnotationDetailEditor ade = ((AnnotationDetailEditor) this.annotations.get(a));
			writeChanges(this.getTokenStatesAt(a), ade.annotation);
		}
	}
	
	/**
	 * Write the status of annotations represented in a token states object
	 * through to some target annotation. This will affect only the detail
	 * annotation types present in the specified token states' parent annotation
	 * editor feedback panel. It is the invokers responsibility to make sure
	 * that the two arguments match. Usually, this method will be invoked with
	 * an argument token states that represents the line representing the target
	 * annotation in an annotation editor feedback panel.
	 * @param tss the token states to write to the target annotation
	 * @param target annotation to modify
	 */
	public static void writeChanges(TokenStates tss, MutableAnnotation target) {
		Annotation[] annotations = target.getAnnotations();
		
		//	index existing detail annotations
		HashMap existingAnnotations = new HashMap();
		for (int a = 0; a < annotations.length; a++) {
			if (tss.parent.detailTypeColors.containsKey(annotations[a].getType())) {
				String annotationKey = annotations[a].getType() + "-" + annotations[a].getStartIndex() + "-" + annotations[a].getEndIndex();
				existingAnnotations.put(annotationKey, annotations[a]);
			}
		}
		int aStart = -1;
		String aType = null;
		for (int t = 0; t < target.size(); t++) {
			
			//	start of a new detail annotation
			if (START.equals(tss.getStateAt(t))) {
				
				//	other detail annotation open, store it
				if (aStart != -1) {
					String annotationKey = aType + "-" + aStart + "-" + t;
					
					//	detail annotation already exists, keep it
					if (existingAnnotations.containsKey(annotationKey))
						existingAnnotations.remove(annotationKey);
					
					//	create it otherwise
					else target.addAnnotation(aType, aStart, (t - aStart));
				}
				
				//	mark start of new detail annotation
				aStart = t;
				aType = tss.getTypeAt(t);
			}
			
			//	not a detail annotation
			else if (OTHER.equals(tss.getStateAt(t))) {
				
				//	detail annotation open, store it
				if (aStart != -1) {
					String annotationKey = aType + "-" + aStart + "-" + t;
					
					//	detail annotation already exists, keep it
					if (existingAnnotations.containsKey(annotationKey))
						existingAnnotations.remove(annotationKey);
					
					//	create it otherwise
					else target.addAnnotation(aType, aStart, (t - aStart));
				}
				
				//	empty registers
				aStart = -1;
				aType = null;
			}
		}
		
		//	detail annotation remains open, store it
		if (aStart != -1) {
			String annotationKey = aType + "-" + aStart + "-" + target.size();
			
			//	detail annotation already exists, keep it
			if (existingAnnotations.containsKey(annotationKey))
				existingAnnotations.remove(annotationKey);
			
			//	create it otherwise
			else target.addAnnotation(aType, aStart, (target.size() - aStart));
		}
		
		//	remove remaining old detail annotations
		for (Iterator ait = existingAnnotations.values().iterator(); ait.hasNext();)
			target.removeAnnotation((Annotation) ait.next());
	}
//	
//	public static void main(String[] args) throws Exception {
//		//	set platform L&F
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {}
//		
//		String[] lines = {"Test 1", "Test 2", "Test 3", "Test 4"};
////		String[] types = {"A", "B", "C"};
//		AnnotationEditorFeedbackPanel aeFp = new AnnotationEditorFeedbackPanel("Original");
//		for (int t = 0; t < lines.length; t++) {
//			MutableAnnotation ma = Gamta.newDocument(Gamta.newTokenSequence(lines[t], null));
//			ma.addAnnotation("A", 0, 2);
//			aeFp.addAnnotation(ma);
//		}
//		aeFp.setDetailColor("A", Color.GREEN);
//		aeFp.setDetailColor("B", Color.RED);
//		aeFp.setDetailColor("C", Color.YELLOW);
//		aeFp.addButton("OK");
//		aeFp.addButton("Cancel");
//		aeFp.setLabel("Please annotate the relevant details in the following paragraphs");
//		
////		AnnotationEditorFeedbackPanelRenderer aeFpr = new AnnotationEditorFeedbackPanelRenderer();
////		FeedbackPanelHtmlRendererInstance aeRenderer = aeFpr.getRendererInstance(aeFp);
////		BufferedWriter bw = new BufferedWriter(new PrintWriter(System.out));
////		
////		aeRenderer.writePanelBody(bw);
////		bw.flush();
//		FeedbackPanelHtmlTester.testFeedbackPanel(aeFp, 0);
//		
////		aeFp.getFeedback();
//		
////		StringWriter sw = new StringWriter();
////		aeFp.writeData(sw);
////		System.out.println(sw.toString());
////		
////		AnnotationEditorFeedbackPanel aeFp2 = new AnnotationEditorFeedbackPanel();
////		aeFp2.initFields(new StringReader(sw.toString()));
////		aeFp2.setDetailColor("A", Color.YELLOW);
////		aeFp2.getFeedback();
//	}
}

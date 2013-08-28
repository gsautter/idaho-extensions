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
package de.uka.ipd.idaho.gamta.util.feedback;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;

/**
 * Panel with buttons and show() and dispose() methods, to act as a replacement
 * for dialogs and frames. The FeedbackService will decide (based on the
 * FeedbackProvider installed) how to actually display the panel. This makes
 * user interaction more flexible than it is with hard-coded dialogs or frames.<br>
 * <br>
 * A FeedbackPanel is double buffered and has a BorderLayout installed after
 * construction. It is not recommended to add any buttons to the actual panel,
 * as usually done in a separate button panel in the BorderLayout.SOUTH
 * position. Instead the buttons should be added via the addButton() method,
 * which leaves the actual positioning of the buttons to the FeedbackProvider
 * displaying the panel. If no button is added at all, an OK button is added
 * automatically.<br>
 * <br>
 * A FeedbackPanel is scrollable by default, i.e., it implements the
 * javax.swing.Scrollable interface to help interact with a
 * javax.swing.JScrollPane in a natural as possible way.
 * 
 * @author sautter
 */
public abstract class FeedbackPanel extends JPanel implements Scrollable {
	
	/**
	 * Classes capable of showing feedback panels in some fashion have to
	 * implement this interface and register to FeedbackPanel via the
	 * addFeedbackService() method. There can be multiple feedback services at
	 * the same time. For any given feedback request, they are asked through
	 * their canGetFeedback() method whether or not they want to handle it. This
	 * happens in order of descending priority, until one feedback service
	 * handles the request. How and where the feedback panels are actually
	 * displayed is up to the implementors of this interface.
	 * 
	 * @author sautter
	 */
	public static interface FeedbackService {
		
		/**
		 * Retrieve the priority of the feedback service, out of [0,10]. If
		 * multiple feedback services are registered, this determines the order
		 * in which they are asked to handle a given feedback request. In
		 * general, feedback service implementations that basically handle any
		 * feedback request (canGetFeedback() returns true) should indicate a
		 * low priority. Conversely, feedback service implementations that want
		 * to handle only very specific feedback requests (canGetFeedback() is
		 * rather picky) should indicate a high priority to make sure they get
		 * to handle the feedback requests they are intended for. An exception
		 * are feedback services that act as wrappers and delegate getting the
		 * actual feedback to the other implementations. They should indicate a
		 * priority of 11 or higher, remember having seen a request, and then
		 * recurse to the getFeedback() method. The value returned by this
		 * method must not change over the lifetime of a feedback service
		 * object.
		 * @return the priority of the feedback service
		 */
		public abstract int getPriority();
		
		/**
		 * Test whether or not this feedback service wants to handle a given
		 * feedback request. In general, feedback service implementations that
		 * basically handle any feedback request (canGetFeedback() returns true)
		 * should indicate a low priority. Conversely, feedback service
		 * implementations that want to handle only very specific feedback
		 * requests (canGetFeedback() is rather picky) should indicate a high
		 * priority to make sure they get to handle the feedback requests they
		 * are intended for.
		 * @param fp the feedback panel to handle
		 */
		public abstract boolean canGetFeedback(FeedbackPanel fp);
		
		/**
		 * Get a feedback request answered. The specified feedback panel will be
		 * shown to one or more users in a service-specific way. This method has
		 * to block until feedback is given in some way.
		 * @param fp the feedback panel to display
		 */
		public abstract void getFeedback(FeedbackPanel fp);
		
		/**
		 * Test whether the feedback service is machine-local, i.e., opens the
		 * feedback panels on the local computer in some way. This is to
		 * facilitate acting differently in single user environments than in
		 * multi user environments.
		 * @return true if the feedback service is local
		 */
		public abstract boolean isLocal();
		
		/**
		 * Test whether the feedback service can handle multiple requests at a
		 * time. If this method returns true, the feedback service must support
		 * the getMultiFeedback() method. Otherwise, the latter method should
		 * throw an UnsupportedOperationException.
		 * @return true if the feedback service can handle multiple requests at
		 *         a time
		 */
		public abstract boolean isMultiFeedbackSupported();
		
		/**
		 * Display multiple feedback requests at a time. This method is intended
		 * for feedback services that get feedback from multiple sources
		 * simultaneously, e.g. from a group of users it reaches through a
		 * network. Using this method enables accessors to have a number of
		 * feedback requests that do not build on each other's results answered
		 * simultaneously instead of one by one, e.g. the parts of a very large
		 * request broken down into handier pieces. There need not be a
		 * guarantee that the requests are answered in the order they appear in
		 * the array. This method has to block until all of the feedback
		 * requests are answered.
		 * @param fps an array holding the feedback requests to answer.
		 * @throws UnsupportedOperationException
		 */
		public abstract void getMultiFeedback(FeedbackPanel[] fps) throws UnsupportedOperationException;
		
		/**
		 * Shut down the feedback service. This method is intended for feedback
		 * services that handle requests asynchronously, i.e. that have one
		 * thread waiting for a feedback request being answered, and another
		 * thread taking care of actually getting the answer.
		 */
		public abstract void shutdown();
	}
	
	private static TreeSet feedbackServices = new TreeSet(new Comparator() {
		public int compare(Object o1, Object o2) {
			int p1 = ((FeedbackService) o1).getPriority();
			int p2 = ((FeedbackService) o2).getPriority();
			if (p1 == p2)
				return (o1.hashCode() - o2.hashCode()); // have to distinguish different objects of the same class, as there might be multiple feedback services of the same class, but using different configurable request filters ...
			else return (p1-p2);
		}
	});
	private static FeedbackService defaultFeedbackService = null;
	static {
		try {
			defaultFeedbackService = new DefaultFeedbackService();
		}
		catch (HeadlessException he) {
			System.out.println("FeedbackPanel: could not create default feedback service");
			he.printStackTrace(System.out);
		}
	}
	
	/**
	 * Default implementation of the feedback service. This class does not
	 * support multi feedback. It opens feedback panels in dialogs on the local
	 * machine, modal to the current top window of the surrounding Java
	 * application. Hence, it is no a good idea to use this feedback service in
	 * a headless environment, like a web server.
	 * 
	 * @author sautter
	 */
	public static class DefaultFeedbackService implements FeedbackService {
		private static final boolean DEBUG = false;
		
		private static final int defaultMaxWidth = 700;
		private static final int defaultMaxHeight = 700;
		
		private final int maxWidth;
		private final int maxHeight;
		
		private Map typeDimensions = Collections.synchronizedMap(new HashMap());
		
//		private ZoomPanel zoomPanel = new ZoomPanel();
		private ZoomPanel zoomPanel = null;
		
		private boolean showFullExplanation = true;
		
		private static class ZoomPanel extends JPanel {
			private float zoomFactor = 1;
			private JLabel zoomFactorLabel = new JLabel("1", JLabel.CENTER);
			
			private static Dimension buttonSize = new Dimension(21, 21);
			private JButton larger = new JButton("<HTML><B>+</B></HTML>");
			private JButton smaller = new JButton("<HTML><B>-</B></HTML>");
			
			private JDialog dialog = null;
			
			private HashMap componentFonts = new HashMap();
			private boolean newComponents = false;
			private LinkedHashMap containerListeners = new LinkedHashMap();
			
			ZoomPanel() {
				super(new BorderLayout(), true);
				
				this.larger.setBorder(BorderFactory.createRaisedBevelBorder());
				this.larger.setPreferredSize(buttonSize);
				this.larger.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						larger();
					}
				});
				
				this.smaller.setBorder(BorderFactory.createRaisedBevelBorder());
				this.smaller.setPreferredSize(buttonSize);
				this.smaller.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						smaller();
					}
				});
				
				this.zoomFactorLabel.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getClickCount() > 1)
							doZoom();
					}
				});
				
				this.zoomFactorLabel.setBorder(BorderFactory.createLoweredBevelBorder());
				this.zoomFactorLabel.setOpaque(true);
				this.zoomFactorLabel.setBackground(Color.WHITE);
				
				this.add(new JLabel("<HTML><B>Zoom Control</B></HTML>", JLabel.CENTER), BorderLayout.NORTH);
				this.add(this.smaller, BorderLayout.WEST);
				this.add(this.zoomFactorLabel, BorderLayout.CENTER);
				this.add(this.larger, BorderLayout.EAST);
			}
			
			void larger() {
				
				//	adjust zoom factor
				this.zoomFactor += 0.25f;
				
				//	adjust buttons
				if (this.zoomFactor >= 4)
					this.larger.setEnabled(false);
				this.smaller.setEnabled(true);
				
				//	update labels
				this.doZoom();
			}
			
			void smaller() {
				
				//	adjust zoom factor
				this.zoomFactor -= 0.25f;
				
				//	adjust buttons
				if (this.zoomFactor <= 0.5)
					this.smaller.setEnabled(false);
				this.larger.setEnabled(true);
				
				//	update labels
				this.doZoom();
			}
			
			void doZoom() {
				
				//	refresh component registers
				if (this.newComponents)
					this.refreshRegisters();
				
				//	adjust zoom display
				this.zoomFactorLabel.setText("" + this.zoomFactor);
				
				//	adjust labels based on current zoom factor
				for (Iterator cit = this.componentFonts.keySet().iterator(); cit.hasNext();) {
					JComponent comp = ((JComponent) cit.next());
					Font font = ((Font) this.componentFonts.get(comp));
					if (font != null)
						setComponentFont(comp, font.deriveFont(this.zoomFactor * font.getSize()));
				}
				
				//	re-layout panels
				waitForSwingEventQueue();
				for (Iterator cit = this.containerListeners.keySet().iterator(); cit.hasNext();) {
					Container cont = ((Container) cit.next());
					LayoutManager lm = cont.getLayout();
					if (lm != null)
						lm.layoutContainer(cont);
					cont.validate();
					cont.repaint();
				}
				
				//	make changes visible
				this.dialog.getContentPane().invalidate();
				this.dialog.getContentPane().validate();
				this.dialog.getContentPane().repaint();
			}
			
			void setFeedbackDialog(JDialog dialog) {
				if (this.dialog != null)
					return;
				this.dialog = dialog;
				
				//	refresh component registers
				this.refreshRegisters();
				
				//	adjust labels based on current zoom factor
				this.doZoom();
			}
			
			private void refreshRegisters() {
				LinkedList fontedComponents = new LinkedList();
				LinkedList containers = new LinkedList();
				LinkedList componentQueue = new LinkedList();
				componentQueue.addLast(dialog.getContentPane());
				while (componentQueue.size() != 0) {
					Component comp = ((Component) componentQueue.removeFirst());
					
					if (comp instanceof JLabel)
						fontedComponents.addLast(comp);
					else if (comp instanceof JMenu) {
						fontedComponents.addLast(comp);
						componentQueue.addLast(((JMenu) comp).getPopupMenu());
					}
					else if (comp instanceof AbstractButton)
						fontedComponents.addLast(comp);
					else if (comp instanceof JComboBox)
						fontedComponents.addLast(comp);
					else if (comp instanceof JProgressBar)
						fontedComponents.addLast(comp);
					else if (comp instanceof JTextComponent)
						fontedComponents.addLast(comp);
					
					else if (comp instanceof JScrollPane) {
						Component viewComp = ((JScrollPane) comp).getViewport().getView();
						if (viewComp != null)
							componentQueue.addLast(viewComp);
					}
					else if (comp instanceof JTabbedPane) {
						for (int t = 0; t < ((JTabbedPane) comp).getTabCount(); t++)
							componentQueue.addLast(((JTabbedPane) comp).getComponentAt(t));
					}
					else if (comp instanceof Container) {
						containers.addFirst(comp);
						Component[] subComps = ((Container) comp).getComponents();
						for (int c = 0; c < subComps.length; c++)
							componentQueue.addLast(subComps[c]);
					}
				}
				
				for (Iterator cit = fontedComponents.iterator(); cit.hasNext();) {
					JComponent comp = ((JComponent) cit.next());
					if (!this.componentFonts.containsKey(comp)) {
						Font font = getComponentFont(comp);
						this.componentFonts.put(comp, font);
					}
				}
				
				for (Iterator cit = containers.iterator(); cit.hasNext();) {
					Container cont = ((Container) cit.next());
					if (!this.containerListeners.containsKey(cont)) {
						ContainerListener cl = new ContainerAdapter() {
							public void componentAdded(ContainerEvent ce) {
								if (!componentFonts.containsKey(ce.getChild()) && !containerListeners.containsKey(ce.getChild()))
									newComponents = true;
							}
						};
						cont.addContainerListener(cl);
						this.containerListeners.put(cont, cl);
					}
				}
				
				this.newComponents = false;
			}
			
			void releaseFeedbackPanel() {
				if (this.dialog == null)
					return;
				
				//	reset labels' font to original size
				for (Iterator cit = this.componentFonts.keySet().iterator(); cit.hasNext();) {
					JComponent comp = ((JComponent) cit.next());
					Font font = ((Font) this.componentFonts.get(comp));
					if (font != null)
						setComponentFont(comp, font);
				}
				
				//	remove container listeners
				for (Iterator cit = this.containerListeners.keySet().iterator(); cit.hasNext();) {
					Container cont = ((Container) cit.next());
					ContainerListener cl = ((ContainerListener) this.containerListeners.get(cont));
					if (cl != null)
						cont.removeContainerListener(cl);

				}
				
				//	clear register
				this.dialog = null;
				this.componentFonts.clear();
				this.containerListeners.clear();
			}
			
			private static Font getComponentFont(JComponent comp) {
				if (comp instanceof JTextPane) {
					StyledDocument sd = ((JTextPane) comp).getStyledDocument();
					Element e = sd.getCharacterElement(0);
					if (e != null) {
						AttributeSet as = e.getAttributes();
						SimpleAttributeSet fas = new SimpleAttributeSet();
						if (as.isDefined(StyleConstants.FontConstants.Size))
							fas.addAttribute(StyleConstants.FontConstants.Size, as.getAttribute(StyleConstants.FontConstants.Size));
						else fas.addAttribute(StyleConstants.FontConstants.Size, new Integer(comp.getFont().getSize()));
						return sd.getFont(fas);
					}
				}
				return comp.getFont();
			}
			
			private static void setComponentFont(JComponent comp, Font font) {
				if (comp instanceof JTextPane) {
					StyledDocument sd = ((JTextPane) comp).getStyledDocument();
					SimpleAttributeSet as = new SimpleAttributeSet();
					as.addAttribute(StyleConstants.FontConstants.Size, new Integer(font.getSize()));
					sd.setCharacterAttributes(0, sd.getLength(), as, false);
				}
				else comp.setFont(font);
				
				if (comp instanceof JComboBox)
					comp.updateUI();
				else {
					comp.validate();
					comp.repaint();
				}
			}
		}
		
		/**
		 * Constructor
		 * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns true
		 */
		public DefaultFeedbackService() throws HeadlessException {
			if (GraphicsEnvironment.isHeadless())
				throw new HeadlessException();
			
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			this.maxWidth = Math.max(defaultMaxWidth, ((screenSize.width * 2) / 3));
			this.maxHeight = Math.max(defaultMaxHeight, ((screenSize.height * 2) / 3));
			if (DEBUG) System.out.println("DefaultFeedbackService: max dialog size set to " + this.maxWidth + " x " + this.maxHeight);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#canGetFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
		 */
		public boolean canGetFeedback(FeedbackPanel fp) {
			return true; // we handle all requests
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getPriority()
		 */
		public int getPriority() {
			return -1; // we're even lower than the lowest custom feedback service
		}
		
		/**
		 * This implementation opens the argument feedback panel in a JDialog,
		 * wrapping it in a JScrollPane first. All the button labels the
		 * argument feedback panel has show up as buttons at the bottom of the
		 * dialog; clicking either of the buttons will close the dialog
		 * immediately. The label of the clicked button becomes the status code
		 * of the argument feedback panel. The label of the feedback panel is
		 * displayed above the feedback panel's actual content.
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel)
		 */
		public void getFeedback(final FeedbackPanel fp) {
			Window topWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
//			Disabled due to Java VM bug (InternalError in sun.awt.windows.WToolkit.eventLoop(Native Method) because of JNI flaw)
//			TODO: re-enable this once VM bug is fixed
//			Frame[] frames = Frame.getFrames();
//			LinkedList windows = new LinkedList();
//			for (int f = 0; f < frames.length; f++)
//				windows.addLast(frames[f]);
//			while (windows.size() != 0) {
//				topWindow = ((Window) windows.removeFirst());
//				Window[] subWindows = topWindow.getOwnedWindows();
//				for (int w = 0; w < subWindows.length; w++)
//					windows.add(subWindows[w]);
//			}
			final JDialog dialog;
			
			if (topWindow instanceof Frame)
				dialog = new JDialog(((Frame) topWindow), fp.getTitle(), true);
			else if (topWindow instanceof Dialog)
				dialog = new JDialog(((Dialog) topWindow), fp.getTitle(), true);
			else dialog = new JDialog(((Frame) null), fp.getTitle(), true);
			
			dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			dialog.getContentPane().setLayout(new BorderLayout());
			if (DEBUG) System.out.println("  - dialog produced");
			
			final JLabel label;
			String labelString = fp.getLabel();
			if (labelString != null) {
				
				//	add title to label
				if ((labelString.length() > 6) && "<html>".equals(labelString.substring(0, 6).toLowerCase()))
					labelString = "<HTML>" + "<B>What to do in this dialog?</B>" + " (click to collapse) " + "<BR>" + labelString.substring(6);
				else labelString = "<HTML>" + "<B>What to do in this dialog?</B>" + " (click to collapse) " + "<BR>" + IoTools.prepareForHtml(labelString) + "</HTML>";
				
				//	store full and short (collapsed) explanation
				final String fullLabelString = labelString;
				final String shortLabelString = "<HTML>" + "<B>What to do in this dialog?</B>" + " (click to expand) " + "</HTML>";
				
				//	build and configure label
				label = new JLabel((this.showFullExplanation ? fullLabelString : shortLabelString), JLabel.LEFT);
				label.setOpaque(true);
				label.setBackground(Color.WHITE);
				label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.RED, 1)), BorderFactory.createLineBorder(Color.WHITE, 5)));
				
				//	facilitate showing/hiding explanation
				label.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						showFullExplanation = !showFullExplanation;
						label.setText(showFullExplanation ? fullLabelString : shortLabelString);
					}
				});
				
				//	add label to dialog
				dialog.getContentPane().add(label, BorderLayout.NORTH);
			}
			else label = null;
			if (DEBUG) System.out.println("  - dialog label produced");
			
			if (this.zoomPanel == null)
				this.zoomPanel = new ZoomPanel();
			if (DEBUG) System.out.println("  - zoom panel produced");
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			final String[] buttons = fp.getButtons();
			if (buttons.length == 0) {
				JButton button = new JButton("OK");
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String[] error = fp.checkFeedback("OK");
						if (error == null) {
							fp.setStatusCode("OK");
							dialog.dispose();
						}
						else displayError(error, dialog);
					}
				});
				buttonPanel.add(button);
			}
			else {
				for (int b = 0; b < buttons.length; b++) {
					final String buttonLabel = buttons[b];
					JButton button = new JButton(buttonLabel);
					button.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							String[] error = fp.checkFeedback(buttonLabel);
							if (error == null) {
								fp.setStatusCode(buttonLabel);
								dialog.dispose();
							}
							else displayError(error, dialog);
						}
					});
					buttonPanel.add(button);
				}
			}
			if (DEBUG) System.out.println("  - buttons produced");
			
			final Properties fieldStates = fp.getFieldStates();
			if (fieldStates != null) {
				JButton resetButton = new JButton("Reset");
				resetButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						fp.setFieldStates(fieldStates);
						dialog.validate();
						dialog.repaint();
					}
				});
				buttonPanel.add(resetButton);
				if (DEBUG) System.out.println("  - reset mechanism prepared");
			}
			
			JPanel functionPanel = new JPanel(new BorderLayout(), true);
			functionPanel.add(buttonPanel, BorderLayout.CENTER);
			functionPanel.add(this.zoomPanel, BorderLayout.EAST);
			dialog.getContentPane().add(functionPanel, BorderLayout.SOUTH);
			if (DEBUG) System.out.println("  - functions added");
			
			//	compute type specific max size
			int maxWidth = this.maxWidth;
			int maxHeight = this.maxHeight;
			final String requestType = fp.getProperty(REQUESTER_CLASS_NAME_PROPERTY);
			Dimension maxSize = ((Dimension) this.typeDimensions.get(requestType));
			if (maxSize != null) {
				maxWidth = Math.max(maxWidth, maxSize.width);
				maxHeight = Math.max(maxHeight, maxSize.height);
			}
			if (DEBUG) System.out.println("  - dialog max size computed: " + maxWidth + " x " + maxHeight);
			
			//	prepare recording resizings
			dialog.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
					if (dialog.isVisible()) {
						Dimension dialogSize = dialog.getSize();
						Dimension maxSize = ((Dimension) typeDimensions.get(requestType));
						if (maxSize == null)
							maxSize = dialogSize;
						maxSize = new Dimension(Math.max(dialogSize.width, maxSize.width), Math.max(dialogSize.height, maxSize.height));
						typeDimensions.put(requestType, maxSize);
						if (DEBUG) System.out.println("  - dialog max size for '" + requestType + "' set to " + maxSize.width + " x " + maxSize.height);
					}
					else if (DEBUG) System.out.println("  - dialog max size for '" + requestType + "' remains, dialog not visible yet");
				}
			});
			
			//	put panel in scroll pane (no more if necessary, but always, since FeedbackPanel implements Scrollable now)
			JScrollPane fpBox = new JScrollPane(fp);
			fpBox.getVerticalScrollBar().setUnitIncrement(Math.max(50, fp.scrollUnitIncrement));
			fpBox.getVerticalScrollBar().setBlockIncrement(200); // we need to set this explicitly here, as the feedback panel returns 1 for the block increment to make scrolling position flexible
			dialog.getContentPane().add(fpBox, BorderLayout.CENTER);
			if (DEBUG) System.out.println("  - dialog content boxed");
			
			/*
			 * we have to catch exceptions from here on in order to make sure we
			 * remove the dialog from the zoom controller even in case of layout
			 * exceptions like the '512' ArrayIndexOutOfBounds exception in
			 * GridBagLayout ...
			 */
			try {
				
				//	apply zoom (has to be done before setting dialog size)
				this.zoomPanel.setFeedbackDialog(dialog);
				if (DEBUG) System.out.println("  - zoom applied");
				
				//	compute size
				Dimension lPs = ((label == null) ? new Dimension(0, 0) : label.getPreferredSize());
				Dimension fpPs = fp.getPreferredSize();
				Dimension bpPs = buttonPanel.getPreferredSize();
				
				int width = maxWidth;
				int height = maxHeight;
				
				if ((fpPs != null) && (bpPs != null) && (lPs != null)) {
					width = (Math.max(lPs.width, Math.max(fpPs.width, bpPs.width)) + 10); // add for window borders
					height = (lPs.height + fpPs.height + bpPs.height + 50); // add for window title bar and borders
					if (DEBUG) System.out.println("  - dialog size pre-computed: " + width + " x " + height);
				}
				
				//	adjust size
				width = Math.min(width + 50, maxWidth); // add for scroll bar
				height = Math.min(height, maxHeight);
				if (DEBUG) System.out.println("  - dialog content produced");
			
			
				//	set dialog size
				dialog.pack();
				dialog.setSize(width, height);
				
				//	wait for pack() to complete
				waitForSwingEventQueue();
			
				//	validate zoom (has to be done after initially packing the dialog)
				this.zoomPanel.doZoom();
				if (DEBUG) System.out.println("  - zoom validated");
				
				//	re-compute dialog size (feedback panels with line wrapping content, for instance, may change their preferred height if width is reduced)
				if (fp.getScrollableTracksViewportWidth() && (width == maxWidth)) {
					if (DEBUG) System.out.println("  - resizing dialog content");
					fpPs = fp.getPreferredSize();
					height = (lPs.height + ((fpPs.height * fpPs.width) / width) + bpPs.height + 50); // add for window title bar and borders
					if (DEBUG) System.out.println("  - dialog size re-computed: " + width + " x " + height);
					height = Math.min(height, maxHeight);
					dialog.setSize(width, height);
				}
				else if (fp.getScrollableTracksViewportHeight() && (height == maxHeight)) {
					if (DEBUG) System.out.println("  - resizing dialog content");
					fpPs = fp.getPreferredSize();
					width = (Math.max(lPs.width, Math.max(((fpPs.width * fpPs.height) / height), bpPs.width)) + 10); // add for window borders
					if (DEBUG) System.out.println("  - dialog size re-computed: " + width + " x " + height);
					width = Math.min(width, maxWidth);
					dialog.setSize(width, height);
				}
				
				//	set dialog location
				dialog.setLocationRelativeTo(topWindow);
				if (DEBUG) System.out.println("  - dialog configured");
				
				if (DEBUG) System.out.println("  - showing dialog");
				waitForSwingEventQueue();
				dialog.setVisible(true);
				if (DEBUG) System.out.println("  - dialog closed");
			}
			finally {
				this.zoomPanel.releaseFeedbackPanel();
				if (DEBUG) System.out.println("  - zoom removed");
			}
		}
		
		private static void waitForSwingEventQueue() {
			if (SwingUtilities.isEventDispatchThread()) return;
			
			/*
			 * If we're not on the event dispatch thread, we have to wait until
			 * the latter finishes laying out the dialog. Otherwise, there might
			 * be deadlocks inside synchronized Swing components.
			 */
			final Object layoutLock = new Object();
			synchronized (layoutLock) {
				
				//	enqueue wakeup for main thread at end of event dispatch thread
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						/*
						 * this block can only be entered if the main thread has
						 * reached the wait() command
						 */
						synchronized (layoutLock) {
							if (DEBUG) System.out.println("    - releasing dialog layout lock");
							layoutLock.notify();
							if (DEBUG) System.out.println("    - dialog layout lock released");
						}
					}
				});
				
				//	wait until Swing event dispatcher has completed dialog layout
				try {
					if (DEBUG) System.out.println("  - waiting for Swing event queue ...");
					if (DEBUG) System.out.println("    - waiting on dialog layout lock");
					layoutLock.wait();
				}
				catch (InterruptedException ie) {
					if (DEBUG) System.out.println("    - interrupted while waiting on dialog layout lock");
				}
			}
		}
		
		private void displayError(String[] errorLines, JDialog dialog) {
			final JDialog errorDialog = new JDialog(dialog, "Incomplete or Incorrect Feedback", true);
			errorDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			errorDialog.getContentPane().setLayout(new BorderLayout());
			if (DEBUG) System.out.println("  - error dialog produced");
			
			//	build dialog label
			JLabel label = new JLabel("<HTML><B>The feedback data you have entered in this feedback panel is incomplete or contains errors.<BR>Please see the error message below for details.</B></HTML>", JLabel.LEFT);
			label.setOpaque(true);
			label.setBackground(Color.WHITE);
			label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.RED, 1)), BorderFactory.createLineBorder(Color.WHITE, 5)));
			Font labelFont = label.getFont();
			label.setFont(labelFont.deriveFont(this.zoomPanel.zoomFactor * labelFont.getSize()));
			errorDialog.getContentPane().add(label, BorderLayout.NORTH);
			
			//	build message
			StringBuffer errorLabelString = new StringBuffer("<HTML>");
			for (int e = 0; e < errorLines.length; e++) {
				if (e != 0)
					errorLabelString.append("<BR>");
				String errorLine = errorLines[e];
				if ((errorLine.length() > 6) && "<html>".equals(errorLine.substring(0, 6).toLowerCase())) {
					errorLine = errorLine.substring(6);
					if ((errorLine.length() > 7) && "</html>".equals(errorLine.substring(errorLine.length() - 7).toLowerCase()))
						errorLine = errorLine.substring(errorLine.length() - 7);
					errorLabelString.append(errorLine);
				}
				else errorLabelString.append(IoTools.prepareForHtml(errorLine));
			}
			errorLabelString.append("</HTML>");
			JLabel errorLabel = new JLabel(errorLabelString.toString(), JLabel.LEFT);
			errorLabel.setOpaque(true);
			errorLabel.setBackground(Color.WHITE);
			errorLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.RED, 1)), BorderFactory.createLineBorder(Color.WHITE, 5)));
			Font errorLabelFont = errorLabel.getFont();
			errorLabel.setFont(errorLabelFont.deriveFont(this.zoomPanel.zoomFactor * errorLabelFont.getSize()));
			errorDialog.getContentPane().add(errorLabel, BorderLayout.CENTER);
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton button = new JButton("OK");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					errorDialog.dispose();
				}
			});
			Font buttonFont = button.getFont();
			button.setFont(buttonFont.deriveFont(this.zoomPanel.zoomFactor * buttonFont.getSize()));
			buttonPanel.add(button);
			errorDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			if (DEBUG) System.out.println("  - buttons produced");
			
			//	compute size
			Dimension lPs = label.getPreferredSize();
			Dimension elPs = errorLabel.getPreferredSize();
			Dimension bpPs = buttonPanel.getPreferredSize();
			int width = (Math.max(lPs.width, Math.max(elPs.width, bpPs.width)) + 10); // add for window borders
			int height = (lPs.height + elPs.height + bpPs.height + 50); // add for window title bar and borders
			if (DEBUG) System.out.println("  - error dialog size pre-computed: " + width + " x " + height);
			
			//	adjust size
			width = Math.min(width, this.maxWidth);
			height = Math.min(height, this.maxHeight);
			if (DEBUG) System.out.println("  - error dialog content produced");
			
			//	set dialog size
			errorDialog.pack();
			errorDialog.setSize(width, height);
			
			//	wait for pack() to complete
			waitForSwingEventQueue();
			
			//	set dialog location
			errorDialog.setLocationRelativeTo(dialog);
			if (DEBUG) System.out.println("  - error dialog configured");
			
			if (DEBUG) System.out.println("  - showing error dialog");
			errorDialog.setVisible(true);
			if (DEBUG) System.out.println("  - error dialog closed");
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#isLocal()
		 */
		public boolean isLocal() {
			return true;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#isMultiRequest()
		 */
		public boolean isMultiFeedbackSupported() {
			return false;
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#getMultiFeedback(de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel[])
		 */
		public void getMultiFeedback(FeedbackPanel[] fps) {
			throw new UnsupportedOperationException("This feedback service can handle only one feedback request at a time.");
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel.FeedbackService#shutdown()
		 */
		public void shutdown() {}
	}
	
	
//	private static class FeedbackServiceThread extends Thread {
//		private final Object lock = new Object();
//		private FeedbackPanel fp;
//		private long timeout = 0;
//		public void start() {
//			
//			/*
//			 * this block makes sure that the invoking thread does not return
//			 * from this method until the service thread has reached its
//			 * waiting-for-request state
//			 */
//			synchronized(this.lock) {
//				
//				//	start thread
//				super.start();
//				
//				//	wait until startup complete
//				try {
//					this.lock.wait(); 
//				} catch (InterruptedException ie) {}
//			}
//		}
//		public void run() {
//			
//			/*
//			 * this block can only be entered if starting thread has reached the
//			 * wait() command, and it will block requests from being set while
//			 * there is a request processing
//			 */
//			synchronized(this.lock) {
//				do {
//					
//					//	clear registers
//					this.fp = null;
//					this.timeout = -1;
//					
//					//	notify thread waiting on start() method
//					this.lock.notify();
//					
//					//	wait until request set through getFeedback() method
//					try {
//						this.lock.wait();
//					} catch (InterruptedException ie) {}
//					
//					//	process feedback request if given
//					if (this.fp != null)
//						feedbackService.getFeedback(this.fp, this.timeout);
//				}
//				
//				//	if notified without request, it must be shutdown
//				while (this.fp != null);
//			}
//			
//			//	reset reference so new instance is created (restart is impossible)
//			feedbackServiceThread = null;
//		}
//		void getFeedback(FeedbackPanel fp, long timeout) {
//			
//			/*
//			 * synchronized block will only be entered if feedback thread
//			 * waits on lock, not when processing request
//			 */
//			synchronized(this.lock) {
//				
//				//	fill registers
//				this.fp = fp;
//				this.timeout = timeout;
//				
//				//	wake up service thread
//				this.lock.notify();
//			}
//		}
//	}
//	private static FeedbackServiceThread feedbackServiceThread;
//	
//	/**
//	 * Retrieve the feedback service currently installed
//	 * @return the feedback service currently installed
//	 */
//	public static FeedbackService getFeedbackService() {
//		return feedbackService;
//	}
	
	/**
	 * Retrieve the feedback services currently installed
	 * @return the feedback services currently installed
	 */
	public static FeedbackService[] getFeedbackServices() {
		return ((FeedbackService[]) feedbackServices.toArray(new FeedbackService[feedbackServices.size()]));
	}
	
	/**
	 * Add a feedback service.
	 * @param feedbackService the feedback service to add
	 */
	public static void addFeedbackService(FeedbackService feedbackService) {
		if (feedbackService != null)
			feedbackServices.add(feedbackService);
	}
	
	/**
	 * Remove a feedback service.
	 * @param feedbackService the feedback service to remove
	 */
	public static void removeFeedbackService(FeedbackService feedbackService) {
		if (feedbackService != null)
			feedbackServices.remove(feedbackService);
	}
	
	/**
	 * Get a feedback request answered. The specified feedback panel is looped
	 * through to the backing feedback service. This method blocks until
	 * feedback is given.
	 * @param fp the feedback panel to display
	 */
	public static void getFeedback(FeedbackPanel fp) {
		if (fp.getProperty(REQUESTER_CLASS_NAME_PROPERTY) == null)
			fp.setProperty(REQUESTER_CLASS_NAME_PROPERTY, getFeedbackRequesterClassName());
		
		FeedbackService fs = findFeedbackService(fp);
		if (fs == null) {
			if (defaultFeedbackService == null)
				throw new HeadlessException();
			else fs = defaultFeedbackService;
		}
		Long ftId = new Long(Thread.currentThread().getId());
		try {
			feedbackThreadIDs.add(ftId);
			fs.getFeedback(fp);
		}
		finally {
			feedbackThreadIDs.remove(ftId);
		}
	}
	private static synchronized FeedbackService findFeedbackService(FeedbackPanel fp) {
		for (Iterator fsit = feedbackServices.iterator(); fsit.hasNext();) {
			FeedbackService fs = ((FeedbackService) fsit.next());
			if (fs.canGetFeedback(fp))
				return fs;
		}
		return null;
	}
	
	/**
	 * Test whether or no a given thread is currently blocked on any of the
	 * getFeedback() or getMultiFeedback() methods of any of the registered
	 * feedback services.
	 * @param thread the thread to test
	 * @return true if the specified thread is currently blocked on a feedback
	 *         method, false otherwise
	 */
	public static boolean isGettingFeedback(Thread thread) {
		return feedbackThreadIDs.contains(new Long(thread.getId()));
	}
	private static final Set feedbackThreadIDs = Collections.synchronizedSet(new HashSet());
	
	/**
	 * Free up a thread currently blocked on any of the getFeedback() or
	 * getMultiFeedback() methods of any of the registered feedback services.
	 * @param thread the thread to test
	 */
	public static void cancelFeedback(Thread thread) {
		if (isGettingFeedback(thread))
			thread.interrupt();
	}
	
	/**
	 * Check whether all the feedback services currently installed are
	 * machine-local, i.e., open the feedback panels on the local computer in
	 * some way, usually in the same JVM. This is to facilitate acting
	 * differently in single user environments than in multi user environments.
	 * This method returns false if at least one of the registered feedback
	 * services is non-local.
	 * @return true if the feedback service is local.
	 */
	public static synchronized boolean isLocal() {
		for (Iterator fsit = feedbackServices.iterator(); fsit.hasNext();) {
			FeedbackService fs = ((FeedbackService) fsit.next());
			if (!fs.isLocal())
				return false;
		}
		if (defaultFeedbackService == null) {
			if (feedbackServices.isEmpty())
				throw new HeadlessException();
			else return true;
		}
		else return defaultFeedbackService.isLocal();
	}
	
	/**
	 * Check whether any of the feedback services currently installed supports
	 * multi feedback, i.e. is able to handle multiple requests at a time. If
	 * this method returns true, accessors can use the getMultiFeedback() method
	 * to have multiple feedback requests handled simultaneously. Otherwise, the
	 * latter method is likely to throw throw an UnsupportedOperationException.
	 * This method returns true if at least one of the registered feedback
	 * services supports multi-feedback.
	 * @return true if the feedback service currently installed can handle
	 *         multiple requests at a time
	 */
	public static boolean isMultiFeedbackEnabled() {
		for (Iterator fsit = feedbackServices.iterator(); fsit.hasNext();) {
			FeedbackService fs = ((FeedbackService) fsit.next());
			if (fs.isMultiFeedbackSupported())
				return true;
		}
		if (defaultFeedbackService == null) {
			if (feedbackServices.isEmpty())
				throw new HeadlessException();
			else return false;
		}
		else return defaultFeedbackService.isMultiFeedbackSupported();
	}
	
	/**
	 * Get multiple feedback requests answered at a time. This method enables
	 * accessors to have a number of feedback requests that do not build on each
	 * other's results answered simultaneously instead of one by one, e.g. the
	 * parts of a very large request broken down into handier pieces. There is
	 * no guarantee that the requests are answered in the order they appear in
	 * the array. This method will block until all of the feedback requests
	 * are answered.
	 * @param fps an array holding the feedback requests to answer.
	 * @throws UnsupportedOperationException
	 */
	public static void getMultiFeedback(FeedbackPanel[] fps) throws UnsupportedOperationException {
		if (fps.length == 0)
			return;
		
		String requesterClassName = null;
		for (int f = 0; f < fps.length; f++)
			requesterClassName = fps[f].getProperty(REQUESTER_CLASS_NAME_PROPERTY);
		if (requesterClassName == null)
			requesterClassName = getFeedbackRequesterClassName();
		for (int f = 0; f < fps.length; f++)
			fps[f].setProperty(REQUESTER_CLASS_NAME_PROPERTY, requesterClassName);
		
		FeedbackService fs = findMultiFeedbackService(fps[0]);
		if (fs == null) {
			if (defaultFeedbackService == null)
				throw new HeadlessException();
			fs = defaultFeedbackService;
		}
		Long ftId = new Long(Thread.currentThread().getId());
		try {
			feedbackThreadIDs.add(ftId);
			fs.getMultiFeedback(fps);
		}
		finally {
			feedbackThreadIDs.remove(ftId);
		}
	}
	private static synchronized FeedbackService findMultiFeedbackService(FeedbackPanel fp) {
		for (Iterator fsit = feedbackServices.iterator(); fsit.hasNext();) {
			FeedbackService fs = ((FeedbackService) fsit.next());
			if (fs.isMultiFeedbackSupported() && fs.canGetFeedback(fp))
				return fs;
		}
		return null;
	}
	
	/**
	 * The name of the property that contains the ID of the document a given
	 * feedback request refers to. This property has to be set by the component
	 * issuing the feedback request. While it is not required for the feedback
	 * request in itself to work, it largely improves tractability.
	 */
	public static final String TARGET_DOCUMENT_ID_PROPERTY = "TargetDocumentID";
	
	/**
	 * The name of the property that contains the ID of the annotation a given
	 * feedback request refers to. The idea is to store in this property the ID
	 * of an annotation that encloses (most) of the annotations the feedback
	 * request actually refers to; a feedback request that displays the
	 * paragraphs of a given document page, for instance, would contain the ID
	 * of the page annotation in this property. This property has to be set by
	 * the component issuing the feedback request. While it is not required for
	 * the feedback request in itself to work, it helps categorizing feedback
	 * requests, e.g. regarding the granularity of the question(s) asked or the
	 * level of expertise it takes a user to answer them.
	 */
	public static final String TARGET_ANNOTATION_ID_PROPERTY = "TargetAnnotationID";
	
	/**
	 * The name of the property that contains the annotation type a given
	 * feedback request (mainly) refers to. The idea is to store in this
	 * property the type of the annotations the feedback request actually refers
	 * to; a feedback request that displays the paragraphs of a given document
	 * page, for instance, would contain 'paragraph' this property. This
	 * property has to be set by the component issuing the feedback request.
	 * While it is not required for the feedback request in itself to work, it
	 * helps categorizing feedback requests, e.g. regarding the granularity of
	 * the question(s) asked or the level of expertise it takes a user to answer
	 * them.
	 */
	public static final String TARGET_ANNOTATION_TYPE_PROPERTY = "TargetAnnotationType";
	
	/**
	 * The name of the property that contains the ID of the document page a
	 * given feedback request (mainly) refers to. This property has to be set by
	 * the component issuing the feedback request. While it is not required for
	 * the feedback request in itself to work, it helps categorizing feedback
	 * requests, e.g. for showing users questions about data they have seen
	 * before.
	 */
	public static final String TARGET_PAGE_ID_PROPERTY = "TargetPageID";
	
	/**
	 * The name of the property that contains the number of the document page a
	 * given feedback request (mainly) refers to. This property has to be set by
	 * the component issuing the feedback request. While it is not required for
	 * the feedback request in itself to work, it helps categorizing feedback
	 * requests, e.g. for showing users questions about data they have seen
	 * before.
	 */
	public static final String TARGET_PAGE_NUMBER_PROPERTY = "TargetPageNumber";
	
	/**
	 * The name of the property that contains the number(s) of the document
	 * page(s) a given feedback request refers to. This property has to be set
	 * by the component issuing the feedback request. While it is not required
	 * for the feedback request in itself to work, it helps linking the feedback
	 * request to the backing document, e.g. for showing users images of the
	 * pages. If the feedback request refers to more than one page, individual
	 * page numbers can be separated with commas, with no spaces in between.
	 * Page ranges can be specified as the numbers of the first and the last
	 * page of the range, with a single dash in between and no spaces. Suppose a
	 * feedback request covers pages 92, page 94, pages 96 through 99, and page
	 * 102 of a given document - this should be represented in this property's
	 * value as '92,94,96-99,102'.
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getTargetPageString(Annotation[])
	 */
	public static final String TARGET_PAGES_PROPERTY = "TargetPages";
	
	/**
	 * The name of the property that contains the ID(s) of the document page(s)
	 * a given feedback request refers to. This property has to be set by the
	 * component issuing the feedback request. While it is not required for the
	 * feedback request in itself to work, it helps linking the feedback request
	 * to the backing document, e.g. for showing users images of the pages. If
	 * the feedback request refers to more than one page, individual page IDs
	 * can be separated with commas, with no spaces in between. Page ranges can
	 * be specified as the IDs of the first and the last page of the range, with
	 * a single dash in between and no spaces. Suppose a feedback request covers
	 * pages 92, page 94, pages 96 through 99, and page 102 of a given document
	 * - this should be represented in this property's value as
	 * '92,94,96-99,102'.
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#getTargetPageIdString(Annotation[])
	 */
	public static final String TARGET_PAGE_IDS_PROPERTY = "TargetPageIDs";
	
	/**
	 * The name of the property that contains the name of the class that issued
	 * a given feedback request. A component issuing feedback requests via the
	 * getFeedback() or getMultiFeedback() method may set this property
	 * explicitly. If it does not, it is determined from the current stack trace
	 * on a best effort basis, namely to the name of the class in the stack
	 * directly above the getFeedback() or getMultiFeedback() method, thus the
	 * class invoking the method.
	 */
	public static final String REQUESTER_CLASS_NAME_PROPERTY = "RequesterClassName";
	
	private static final boolean DEBUG_REQUESTER = false;
	private static String getFeedbackRequesterClassName() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		if (DEBUG_REQUESTER) {
			System.out.println("Current JVM Stack:");
			for (int e = 0; e < ste.length; e++)
				System.out.println("  - " + ste[e].getClassName() + ", in " + ste[e].getMethodName() + "()");
		}
		String requesterClassName = null;
		boolean nextIsRequesterClassName = false;
		for (int e = 0; e < ste.length; e++) {
			if (FeedbackPanel.class.getName().equals(ste[e].getClassName()) && ("getFeedback".equals(ste[e].getMethodName()) || "getMultiFeedback".equals(ste[e].getMethodName())))
				nextIsRequesterClassName = true;
			else if (nextIsRequesterClassName) {
				requesterClassName = ste[e].getClassName();
				e = ste.length;
			}
		}
		if (DEBUG_REQUESTER)
			System.out.println("==> Requester Class is " + requesterClassName);
		if (requesterClassName != null)
			requesterClassName = requesterClassName.substring(requesterClassName.lastIndexOf('.') + 1);
		if (DEBUG_REQUESTER)
			System.out.println("==> Requester Class Name parsed to " + requesterClassName);
		return requesterClassName;
	}
	
	/**
	 * The prefix for the properties that contain the number of answers in a
	 * given feedback panel. The value of this property may be higher than the
	 * value returned by getDecisionCount(). In particular, this is the case if
	 * the individual decisions consist of more than one parameter.<BR>
	 * This type of properties is intended for use in a distributed environment,
	 * where multiple users may be involved in answering a feedback request,
	 * allowing each user to receive credit for his individual contribution.
	 * This property is not intended to be set by any component other than
	 * feedback engines. Furthermore, feedback engines are not bound to set this
	 * type of properties. Web-based feedback engines are recommended to do so,
	 * however, and the default ones do.
	 */
	public static final String ANSWER_COUNT_PROPERTY = "Answers";
	
	/**
	 * The prefix for the properties that contain the number of correct answers
	 * each contributing user gave in a given feedback panel. The individual
	 * properties are named
	 * 'CORRECT_ANSWER_COUNT_PROPERTY_PREFIX&lt;userName&gt;', containing the
	 * number of correct answers for user &lt;userName&gt;.<BR>
	 * This type of properties is intended for use in a distributed environment,
	 * where multiple users may be involved in answering a feedback request,
	 * allowing each user to receive credit for his individual contribution.
	 * This property is not intended to be set by any component other than
	 * feedback engines. Furthermore, feedback engines are not bound to set this
	 * type of properties. Web-based feedback engines are recommended to do so,
	 * however, and the default ones do.
	 */
	public static final String CORRECT_ANSWER_COUNT_PROPERTY_PREFIX = "CorrectAnswers_";
	
	/**
	 * The prefix for the properties that contain the time it took each
	 * contributing user to answer a given feedback panel. The individual
	 * properties are named 'ANSWER_TIME_PROPERTY_PREFIX&lt;userName&gt;',
	 * containing the time it took for user &lt;userName&gt; to answer.<BR>
	 * This type of properties is intended for use in a distributed environment,
	 * where multiple users may be involved in answering a feedback request.
	 * This property is not intended to be set by any component other than
	 * feedback engines. Furthermore, feedback engines are not bound to set this
	 * type of properties. Web-based feedback engines are recommended to do so,
	 * however, and the default ones do.
	 */
	public static final String ANSWER_TIME_PROPERTY_PREFIX = "AnswerTime_";

	/**
	 * The name of the property that contains the minimum answer redundancy for
	 * a given feedback request.<BR>
	 * This property is intended for use in a distributed environment, where
	 * multiple users may be involved in answering a feedback request, allowing
	 * to specify exactly how many users have to agree on a specific value for
	 * an answer parameter before it is regarded as secure.
	 */
	public static final String MINIMUM_REDUNDANCY_PROPERTY_NAME = "MinimumRedundancy";
	
	private int scrollUnitIncrement = 25;
	private boolean fitWidth = true;
	private boolean fitHeight = false;
	
	/**
	 * Implements the method specified in javax.swing.Scrollable such that it
	 * always returns the feedback panel's preferred size.
	 * @see javax.swing.Scrollable#getPreferredScrollableViewportSize()
	 */
	public Dimension getPreferredScrollableViewportSize() {
		return this.getPreferredSize();
	}
	
	/**
	 * Implements the method specified in javax.swing.Scrollable such that it
	 * always returns 1, thus allowing any scroll position.
	 * @see javax.swing.Scrollable#getScrollableBlockIncrement(java.awt.Rectangle, int, int)
	 */
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 1;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.Scrollable#getScrollableTracksViewportWidth()
	 */
	public boolean getScrollableTracksViewportWidth() {
		return this.fitWidth;
	}
	
	/**
	 * Specify whether or not the feedback panel should adjust to the width of a
	 * surrounding JScrollPane, making the horizontal scroll bar obsolete. The
	 * default is 'true'.
	 * @param stvw fit the width of a surrounding JScrollPane?
	 * @see javax.swing.Scrollable#getScrollableTracksViewportWidth()
	 */
	public void setScrollableTracksViewportWidth(boolean stvw) {
		this.fitWidth = stvw;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.Scrollable#getScrollableTracksViewportHeight()
	 */
	public boolean getScrollableTracksViewportHeight() {
		return this.fitHeight;
	}
	
	/**
	 * Specify whether or not the feedback panel should adjust to the height of
	 * a surrounding JScrollPane, making the vertical scroll bar obsolete. The
	 * default is 'false'.
	 * @param stvh fit the height of a surrounding JScrollPane?
	 * @see javax.swing.Scrollable#getScrollableTracksViewportHeight()
	 */
	public void setScrollableTracksViewportHeight(boolean stvh) {
		this.fitHeight = stvh;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.Scrollable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)
	 */
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return this.scrollUnitIncrement;
	}
	
	/**
	 * Set the scroll unit increment for the feedback panel in a JScrollPane,
	 * i.e., the number of pixels by which to move the viewport for one step on
	 * the mouse wheel or one click on the direction buttons of a scroll bar.
	 * The default value is 25 pixels.
	 * @param scrollUnitIncrement the new scroll unit increment for the feedback
	 *            panel
	 * @see javax.swing.Scrollable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)
	 */
	public void setScrollableUnitIncrement(int scrollUnitIncrement) {
		this.scrollUnitIncrement = scrollUnitIncrement;
	}
	
	
	
	private String title;
	private String label;
	private long timeout = 0;
	
	private Properties properties;
	
	/**
	 * Constructor
	 */
	public FeedbackPanel() {
		this(null);
	}
	
	/**
	 * Constructor
	 * @param title the title of the feedback panel
	 */
	public FeedbackPanel(String title) {
		super(new BorderLayout(), true);
		this.title = ((title == null) ? "Please Give Feedback" : title);
	}
	
	/**
	 * @return the title of the feedback panel
	 */
	public String getTitle() {
		return this.title;
	}
	
	/**
	 * Set the title of the feedback panel
	 * @param title the new title of the feedback panel
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
	/**
	 * @return the explanation label of the feedback panel
	 */
	public String getLabel() {
		return this.label;
	}
	
	/**
	 * Set the explanation label of the feedback panel (may contain HTML markup)
	 * @param label the new explanation label of the feedback panel
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	
	/**
	 * @return the timeout for this feedback request (in milliseconds)
	 */
	public long getTimeout() {
		return this.timeout;
	}

	/**
	 * Set the timeout for this feedback request (0 means no timeout)
	 * @param timeout the timeout for this feedback request (in milliseconds)
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	/**
	 * Retrieve a property of the feedback panel. The properties are intended to
	 * hold custom request-specific data.
	 * @param name the name of the property
	 * @return the value of the property with the specified name, or null, if
	 *         there is no such property
	 */
	public String getProperty(String name) {
		return this.getProperty(name, null);
	}
	
	/**
	 * Retrieve a property of the feedback panel. The properties are intended to
	 * hold custom request-specific data.
	 * @param name the name of the property
	 * @param def the value to return if the property with the specified name is
	 *            not set
	 * @return the value of the property with the specified name, or the
	 *         specified default value, if there is no such property
	 */
	public String getProperty(String name, String def) {
		return ((this.properties == null) ? def : this.properties.getProperty(name, def));
	}
	
	/**
	 * Retrieve the properties of the feedback panel. The object returned is a
	 * snapshot of the properties currently set for the feedback panel, not
	 * reflecting future changes.
	 * @return the Properties object containing the properties currently set for
	 *         the feedback panel
	 */
	public Properties getProperties() {
		Properties props = new Properties();
		if (this.properties != null)
			props.putAll(this.properties);
		return props;
	}
	
	/**
	 * Set a property for the feedback panel. The properties are intended to
	 * hold custom request-specific data. Setting a property to null will result
	 * in it being removed.
	 * @param name the name of the property to set
	 * @param value the value to set the property to
	 */
	public void setProperty(String name, String value) {
		if (value == null)
			this.removeProperty(name);
		
		else {
			if (this.properties == null)
				this.properties = new Properties();
			this.properties.setProperty(name, value);
		}
	}
	
	/**
	 * Add a bulk of properties to the feedback panel. Properties already set
	 * will not be overwritten. This method is good for filling in default
	 * values for missing properties, based on a central registry of default
	 * values.
	 * @param data the Properties object holding the name/value pairs to add
	 */
	public void addProperties(Properties data) {
		this.setProperties(data, false);
	}
	
	/**
	 * Set a bulk of properties to the feedback panel. Properties already set
	 * will be overwritten. This method is good for setting a set of properties
	 * to default values, no matter what they have been set to before, based on
	 * a central registry of default values.
	 * @param data the Properties object holding the name/value pairs to set
	 */
	public void setProperties(Properties data) {
		this.setProperties(data, true);
	}
	
	private void setProperties(Properties data, boolean overwrite) {
		if (this.properties == null)
			this.properties = new Properties();
		for (Iterator pit = data.keySet().iterator(); pit.hasNext();) {
			String name = pit.next().toString();
			if (overwrite || !this.properties.containsKey(name))
				this.properties.setProperty(name, data.getProperty(name));
		}
	}
	
	/**
	 * Retrieve the names of the properties currently set for the feedback
	 * panel. The properties are intended to hold custom request-specific data.
	 * @return an array holding the names of the properties currently set for
	 *         the feedback panel
	 */
	public String[] getPropertyNames() {
		TreeSet names = new TreeSet();
		if (this.properties != null)
			names.addAll(this.properties.keySet());
		return ((String[]) names.toArray(new String[names.size()]));
	}
	
	/**
	 * Remove a property from the feedback panel. The properties are intended to
	 * hold custom request-specific data.
	 * @param name the name of the properties to remove
	 */
	public void removeProperty(String name) {
		if (this.properties != null)
			this.properties.remove(name);
	}
	
	/**
	 * Retrieve the overall complexity of the feedback panel. The complexity is
	 * intended to provide a measure for how long users may take to answer the
	 * feedback panel. It should be (somehow) related to the product of the
	 * decision complexity and the number of decisions, though need not be
	 * exactly this value. This method is intended, for instance, for
	 * determining the timeout of a dialog shown to a user.
	 * @return the overall complexity of the feedback panel
	 */
	public abstract int getComplexity();
	
	/**
	 * Retrieve the complexity of the feedback panel. The complexity
	 * is intended to provide a measure for how long users may take to
	 * decide on a single feedback parameter residing in the feedback panel.
	 * This method is intended, for instance, for determining the timeout of a
	 * dialog shown to a user.
	 * @return the complexity of the individual feedback decisions a user
	 *         has to make for answering the feedback panel
	 */
	public abstract int getDecisionComplexity();
	
	/**
	 * Retrieve the number of decisions to make for answering the feedback
	 * panel. This method is intended, for instance, for determining the timeout
	 * of a dialog shown to a user.
	 * @return the number of feedback decisions a user has to make for answering
	 *         the feedback panel
	 */
	public abstract int getDecisionCount();

	/**
	 * Check if the feedback data currently entered in the feedback panel is
	 * correct & complete, e.g. if it complies with some request specific
	 * conditions. If not, this method should return an error message packed in
	 * an array of strings. If the strings in the array are plain, i.e. do not
	 * start with &lt;html&gt; and end with &lt;/html&gt;, feedback services
	 * should display data returned by this method using one line per array
	 * element. Feedback panels wishing to format their error messages using
	 * HTML, in turn, should, but are not required to, use a single array entry
	 * for their message. If the data in the feedback panel is correct, this
	 * method has to return null to indicate so. This default implementation
	 * does not perform any checks and simply return null. Sub classes are
	 * welcome to overwrite it as needed.
	 * @param status the return status the feedback panel is being closed with
	 * @return an array holding the lines of an error message, or null if the
	 *         feedback data is correct/complete
	 */
	public String[] checkFeedback(String status) {
		return null;
	}
	
	/**
	 * Show the feedback panel to a user in some way. Invoking this method
	 * enqueues the feedback panel in the FeedbackService to be displayed to
	 * some user in whatever way the installed FeedbackProvider may show it.
	 * This method will block until the feedback is given. However, if the
	 * panels displayed in some other way, e.g. via the getMultiFeedback()
	 * method, this method is not invoked. It therefore is final in order to
	 * prevent putting functionally required code in it.
	 * @return the return status of the feedback panel, i.e., the status string
	 *         set through the setStatus() method
	 * @throws RuntimeException if an exception occurs while displaying the
	 *             feedback panel
	 */
	public final String getFeedback() throws RuntimeException {
		getFeedback(this);
		return this.statusCode;
	}
	
	/**
	 * Retrieve the label of the button the the feedback panel was closed with.
	 * @return the label of the button the feedback panel was closed with, or
	 *         null, if the feedback panel was not yet shown, still is showing,
	 *         or timed out
	 */
	public String getStatusCode() {
		return this.statusCode;
	}
	
	/**
	 * Set the status code of the feedback panel. In order for invoking code to
	 * understand the status code, it has to correspond to the label of the
	 * button the feedback panel was closed with.
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	
	private LinkedHashSet buttons = new LinkedHashSet();
	private String statusCode = null;
	
	/**
	 * Add a button with a specific label to the feedback panel. All the buttons
	 * added through this method will be displayed when the panel is displayed.
	 * @param buttonLabel the text for the button to add
	 */
	public void addButton(final String buttonLabel) {
		if (buttonLabel != null)
			this.buttons.add(buttonLabel);
	}
	
	/**
	 * Retrieve the buttons of this feedback panel. This method is primarily
	 * intended to be used by feedback service implementations displaying the
	 * feedback panel.
	 * @return an array holding the buttons of this feedback panel
	 */
	public String[] getButtons() {
		return ((String[]) this.buttons.toArray(new String[this.buttons.size()]));
	}

	/**
	 * Write the current content of all the data fields in the feedback panel to
	 * some writer. This method exists for transferring the state of a feedback
	 * panel over the network, circumventing Java's native serialization, which
	 * may not work in certain environments (e.g. applets) due to security
	 * issues. Another feedback panel of the same class should have exactly the
	 * same state as this one after reading the output of this method via its
	 * initFields() method. This method writes the scroll behavior control
	 * parameters, title, label, timeout, status code, buttons, and properties,
	 * terminated by a blank line. Therefore, sub classes overwriting this
	 * method must make the super call, preferably as the first write operation
	 * in their own implementation of this method.
	 * @param out the Writer to write to
	 * @throws IOException
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#initFields(Reader)
	 */
	public void writeData(Writer out) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		
		bw.write("" + this.scrollUnitIncrement);
		bw.newLine();
		bw.write("" + this.fitWidth);
		bw.newLine();
		bw.write("" + this.fitHeight);
		bw.newLine();
		bw.write((this.title == null) ? " " : URLEncoder.encode(this.title, "UTF-8"));
		bw.newLine();
		bw.write((this.label == null) ? " " : URLEncoder.encode(this.label, "UTF-8"));
		bw.newLine();
		bw.write("" + this.timeout);
		bw.newLine();
		bw.write(URLEncoder.encode(((this.statusCode == null) ? " " : this.statusCode), "UTF-8"));
		bw.newLine();
		
		for (Iterator bit = this.buttons.iterator(); bit.hasNext();) {
			bw.write(URLEncoder.encode(bit.next().toString(), "UTF-8"));
			bw.newLine();
		}
		bw.newLine();
		
		String[] propertyNames = this.getPropertyNames();
		for (int p = 0; p < propertyNames.length; p++) {
			bw.write(URLEncoder.encode(propertyNames[p], "UTF-8") + " " + URLEncoder.encode(this.getProperty(propertyNames[p]), "UTF-8"));
			bw.newLine();
		}
		bw.newLine();
		
		bw.flush();
	}

	/**
	 * Adjust the content of all data fields in the feedback panel to the data
	 * provided by the argument reader. This method exists for transferring the
	 * state of a feedback panel over the network, circumventing Java's native
	 * serialization, which may not work in certain environments (e.g. applets)
	 * due to security issues. After reading the data, the feedback panel should
	 * have exactly the same state as the instance of the same class the
	 * produced the data via its writeData() method. This method reads the the
	 * scroll behavior control parameters, title, label, timeout, status code,
	 * buttons, properties, and the terminating blank line. Therefore, sub
	 * classes overwriting this method must make the super call, preferably as
	 * the first read operation in their own implementation of this method.
	 * @param in the Reader to read from
	 * @throws IOException
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#writeData(Writer)
	 */
	public void initFields(Reader in) throws IOException {
		BufferedReader br = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
		
		this.scrollUnitIncrement = Integer.parseInt(br.readLine());
		this.fitWidth = Boolean.parseBoolean(br.readLine());
		this.fitHeight = Boolean.parseBoolean(br.readLine());
		
		this.title = URLDecoder.decode(br.readLine(), "UTF-8");
		if (" ".equals(this.title))
			this.title = null;
		this.label = URLDecoder.decode(br.readLine(), "UTF-8");
		if (" ".equals(this.label))
			this.label = null;
		this.timeout = Long.parseLong(br.readLine());
		this.statusCode = URLDecoder.decode(br.readLine(), "UTF-8");
		if (" ".equals(this.statusCode))
			this.statusCode = null;
		
		String button;
		while (((button = br.readLine()) != null) && (button.length() != 0))
			this.addButton(URLDecoder.decode(button, "UTF-8"));
		
		String property;
		while (((property = br.readLine()) != null) && (property.length() != 0)) {
			int split = property.indexOf(' ');
			if (split == -1)
				this.setProperty(URLDecoder.decode(property.trim(), "UTF-8"), "");
			else this.setProperty(URLDecoder.decode(property.substring(0, split).trim(), "UTF-8"), URLDecoder.decode(property.substring(split).trim(), "UTF-8"));
		}
	}
	
	/**
	 * Retrieve the current states of the fields in the feedback panel as a set
	 * of parameter/value pairs. This method differs from writeData() in that it
	 * affects only the fields editable through the UI, not the ones that can be
	 * set programmatically. The current state should be possible to restore
	 * through the setFieldStates() method. This mechanism provides a generic
	 * means of resetting feedback panels. If this method returns null, the
	 * mechanism is disabled. This default implementation does return null, sub
	 * classes are welcome to overwrite it as needed.
	 * @return a Properties object holding the current field states in form of
	 *         parameter/value pairs
	 */
	public Properties getFieldStates() {
		return null;
	}
	
	/**
	 * Set the states of the fields in the feedback panel to the ones provided
	 * as parameter/value pairs. This method differs from initFields() in that
	 * it affects only the fields editable through the UI, not the ones that can
	 * be set programmatically. This method is meant for restoring a field state
	 * from a Properties object retrieved from the getFieldStates() method. This
	 * mechanism provides a generic means of resetting feedback panels. This
	 * default implementation does nothing, sub classes are welcome to overwrite
	 * it as needed.
	 * @param states a Properties object holding the to-restore field states in
	 * 			  form of parameter/value pairs
	 */
	public void setFieldStates(Properties states) {}
	
	/**
	 * Write the body of the JavaScript page initializer function. This method
	 * is needed for displaying feedback panels as HTML pages. This default
	 * implementation does nothing. Sub classes are welcome to overwrite it as
	 * needed.
	 * @param out the Writer to write to
	 * @throws IOException
	 */
	public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {}
	
	/**
	 * Write the body of the JavaScript pre-submission check function. This
	 * method is needed for displaying feedback panels as HTML pages, namely for
	 * for checking the status of HTML feedback forms before submission. This
	 * default implementation does nothing. Sub classes are welcome to overwrite
	 * it as needed.
	 * @param out the Writer to write to
	 * @throws IOException
	 */
	public void writeJavaScriptCheckFeedbackFunctionBody(Writer out) throws IOException {}
	
	/**
	 * Write the body of the JavaScript submission preparation function. This
	 * method is needed for displaying feedback panels as HTML pages, namely to
	 * allow for final pre-submit modifications to HTML feedback forms. This
	 * default implementation does nothing. Sub classes are welcome to overwrite
	 * it as needed.
	 * @param out the Writer to write to
	 * @throws IOException
	 */
	public void writeJavaScriptPrepareSubmitFunctionBody(Writer out) throws IOException {}
	
	/**
	 * Write further JavaScript code (variables and functions). This method
	 * is needed for displaying feedback panels as HTML pages. This default
	 * implementation does nothing. Sub classes are welcome to overwrite it as
	 * needed.
	 * @param out the Writer to write to
	 * @throws IOException
	 */
	public void writeJavaScript(Writer out) throws IOException {}
	
	/**
	 * Write the CSS styles needed for the HTML representation of this feedback
	 * panel. This method is needed for displaying feedback panels as HTML
	 * pages. This default implementation does nothing. Sub classes are welcome
	 * to overwrite it as needed.
	 * @param out the Writer to write to
	 * @throws IOException
	 */
	public void writeCssStyles(Writer out) throws IOException {}
	
	/**
	 * Write the HTML representation of the feedback panel's content. Enclosing
	 * HTML based feedback engines are strongly recommended to enclose the
	 * output of this method in some sort of block, e.g. a p, div, or td. This
	 * method is needed for displaying feedback panels as HTML pages. This
	 * default implementation throws an UnsupportedOperationException. Sub
	 * classes are welcome to overwrite it as needed.<br>
	 * <br>
	 * Sub classes overwriting this method are recommended (though not expected)
	 * to use a table for rendering the feedback panel. Instead of overwriting
	 * this method to provide a sub class specific implementation, it is also
	 * possible to use a specific feedback panel HTML renderer component. In
	 * order to avoid exceptions, HTML feedback engines should therefore first
	 * check if they have a renderer for a specific feedback panel, and only in
	 * case they don't have one use this method for rendering the page body.
	 * @param out the Writer to write to
	 * @throws IOException
	 */
	public void writePanelBody(Writer out) throws IOException {
		throw new UnsupportedOperationException("The HTML rendering of the feedback panel's content has to be implemented specifically by sub classes.");
	}
	
	/**
	 * Read in the response from a form rendered by this feedback panel's
	 * writePanelBody() method. This default implementation throws an
	 * UnsupportedOperationException. Sub classes are welcome to overwrite it as
	 * needed.
	 * @param response a Properties object holding the response parameter/value
	 *            pairs
	 */
	public void readResponse(Properties response) {
		throw new UnsupportedOperationException("Reading in parameters from an HTML form has to be implemented specifically by sub classes.");
	}
	
	/**
	 * Helper method for sub classes. Encodes a color in its hexadecimal RGB
	 * representation.
	 * @param color the color to encode
	 * @return the hexadecimal RGB representation of the specified color
	 */
	public static String getRGB(Color color) {
		return ("" +
				getHex(color.getRed()) + 
				getHex(color.getGreen()) +
				getHex(color.getBlue()) +
				"");
	}
	
	private static final String getHex(int i) {
		int high = (i >>> 4) & 15;
		int low = i & 15;
		String hex = "";
		if (high < 10) hex += ("" + high);
		else hex += ("" + ((char) ('A' + (high - 10))));
		if (low < 10) hex += ("" + low);
		else hex += ("" +  ((char) ('A' + (low - 10))));
		return hex;
	}
	
	/**
	 * Helper method for sub classes. Decodes a color from its hexadecimal RGB
	 * representation.
	 * @param rgb the hexadecimal RGB representation to decode
	 * @return the color corresponding to the specified hexadecimal RGB
	 *         representation
	 */
	public static Color getColor(String rgb) {
		if (rgb.length() == 3) return readHexRGB(rgb.substring(0, 1), rgb.substring(1, 2), rgb.substring(2, 3));
		else if (rgb.length() == 6) return readHexRGB(rgb.substring(0, 2), rgb.substring(2, 4), rgb.substring(4, 6));
		else return null;
	}
	
	private static final Color readHexRGB(String red, String green, String blue) {
		return new Color(
				translateString(red),
				translateString(green),
				translateString(blue)
			);
	}
	
	private static final int translateString(String s) {
		if (s.length() == 0)
			return 0;
		
		int v = 0;
		v += translateChar(s.charAt(0));
		v <<= 4;
		v += translateChar(s.charAt((s.length() > 1) ? 1 : 0));
		return v;
	}
	
	private static final int translateChar(char c) {
		if (('0' <= c) && (c <= '9')) return (((int) c) - '0');
		else if (('a' <= c) && (c <= 'f')) return (((int) c) - 'a' + 10);
		else if (('A' <= c) && (c <= 'F')) return (((int) c) - 'A' + 10);
		else return 0;
	}
	
	/**
	 * Brighten up a color. This method uses a different brightening scheme than
	 * java.awt.Color.brighter(). More specifically, this method halves the
	 * distance of the R, G, and B values to 255, instead of applying a factor.
	 * @param base the color to brighten up
	 * @return a brighter version of the specified base color
	 */
	public static Color brighten(Color base) {
		int r = base.getRed();
		int g = base.getGreen();
		int b = base.getBlue();
		
		float[] hsb = Color.RGBtoHSB(r,g,b,null);
		hsb[2] = 1.0f - ((1.0f - hsb[2]) / 2.0f);
		hsb[1] = (hsb[1] / 3.0f);
		return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
		
//		does not work for gray
//		r = (r + (((255 - r) * 2) / 3));
//		g = (g + (((255 - g) * 2) / 3));
//		b = (b + (((255 - b) * 2) / 3));
//		return new Color(r, g, b);
		
//		works only for gray
//		float[] hsb = Color.RGBtoHSB(r,g,b,null);
//		hsb[2] = 1.0f - ((1.0f - hsb[2]) / 3.0f);
//		return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
		
//		yields too dark colors
//		r = (r + ((255 - r) / 2));
//		g = (g + ((255 - g) / 2));
//		b = (b + ((255 - b) / 2));
//		return new Color(r, g, b);
	}
	
	/**
	 * Darken a color. This method uses a different darkening scheme than
	 * java.awt.Color.darker(). More specifically, this method halves the
	 * distance of the R, G, and B values to 0, instead of applying a factor.
	 * @param base the color to darken
	 * @return a darker version of the specified base color
	 */
	public static Color darken(Color base) {
		int r = base.getRed();
		int g = base.getGreen();
		int b = base.getBlue();
		
		float[] hsb = Color.RGBtoHSB(r,g,b,null);
		hsb[2] = (hsb[2] / 2.0f);
		hsb[1] = 1.0f - ((1.0f - hsb[1]) / 3.0f);
		return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
		
//		does not work for gray
//		r = (r + (((255 - r) * 2) / 3));
//		g = (g + (((255 - g) * 2) / 3));
//		b = (b + (((255 - b) * 2) / 3));
//		return new Color(r, g, b);
		
//		works only for gray
//		float[] hsb = Color.RGBtoHSB(r,g,b,null);
//		hsb[2] = 1.0f - ((1.0f - hsb[2]) / 3.0f);
//		return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
		
//		yields too dark colors
//		r = (r + ((255 - r) / 2));
//		g = (g + ((255 - g) / 2));
//		b = (b + ((255 - b) / 2));
//		return new Color(r, g, b);
	}
	
	/**
	 * Build the string encoding the page numbers of a given set of annotations,
	 * in accordance with the guidelines specified for TARGET_PAGES_PROPERTY. If
	 * none of the argument annotations has a PAGE_NUMBER_ATTRIBUTE set, this
	 * method returns null.
	 * @param annotations the annotations whose page number attributes to encode
	 * @return the string encoding the page numbers of the specified annotations
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#TARGET_PAGES_PROPERTY
	 */
	public static String getTargetPageString(Annotation[] annotations) {
		return getTargetPageString(annotations, 0, annotations.length);
	}
	
	/**
	 * Build the string encoding the page numbers of a given set of annotations,
	 * in accordance with the guidelines specified for TARGET_PAGES_PROPERTY. If
	 * none of the argument annotations has a PAGE_NUMBER_ATTRIBUTE set, this
	 * method returns null.
	 * @param annotations the annotations whose page number attributes to encode
	 * @param start the index of the first annotation to include, inclusive
	 * @param end the index of the last annotation to include, exclusive
	 * @return the string encoding the page numbers of the specified annotations
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#TARGET_PAGES_PROPERTY
	 */
	public static String getTargetPageString(Annotation[] annotations, int start, int end) {
		TreeSet pageNumberDeduplicator = new TreeSet();
		StringBuffer targetPages = new StringBuffer();
		for (int a = start; a < end; a++) {
			String pageNumber = ((String) annotations[a].getAttribute(LiteratureConstants.PAGE_NUMBER_ATTRIBUTE));
			if ((pageNumber != null) && pageNumberDeduplicator.add(pageNumber))
				targetPages.append("," + pageNumber);
		}
		return ((targetPages.length() == 0) ? null : targetPages.substring(1)); 
	}
	
	/**
	 * Build the string encoding the page IDs of a given set of annotations, in
	 * accordance with the guidelines specified for TARGET_PAGE_IDS_PROPERTY. If
	 * none of the argument annotations has a PAGE_ID_ATTRIBUTE set, this method
	 * returns null.
	 * @param annotations the annotations whose page number attributes to encode
	 * @return the string encoding the page numbers of the specified annotations
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#TARGET_PAGE_IDS_PROPERTY
	 */
	public static String getTargetPageIdString(Annotation[] annotations) {
		return getTargetPageString(annotations, 0, annotations.length);
	}
	
	/**
	 * Build the string encoding the page IDs of a given set of annotations, in
	 * accordance with the guidelines specified for TARGET_PAGE_IDS_PROPERTY. If
	 * none of the argument annotations has a PAGE_ID_ATTRIBUTE set, this method
	 * returns null.
	 * @param annotations the annotations whose page number attributes to encode
	 * @param start the index of the first annotation to include, inclusive
	 * @param end the index of the last annotation to include, exclusive
	 * @return the string encoding the page numbers of the specified annotations
	 * @see de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel#TARGET_PAGE_IDS_PROPERTY
	 */
	public static String getTargetPageIdString(Annotation[] annotations, int start, int end) {
		TreeSet pageNumberDeduplicator = new TreeSet();
		StringBuffer targetPages = new StringBuffer();
		for (int a = start; a < end; a++) {
			String pageNumber = ((String) annotations[a].getAttribute(LiteratureConstants.PAGE_ID_ATTRIBUTE));
			if ((pageNumber != null) && pageNumberDeduplicator.add(pageNumber))
				targetPages.append("," + pageNumber);
		}
		return ((targetPages.length() == 0) ? null : targetPages.substring(1)); 
	}
}

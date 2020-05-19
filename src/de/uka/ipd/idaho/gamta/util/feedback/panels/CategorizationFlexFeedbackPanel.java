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
package de.uka.ipd.idaho.gamta.util.feedback.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * @author sautter
 *
 */
public class CategorizationFlexFeedbackPanel extends JPanel {
	
	public static interface PropagationStrategy {
		public abstract String getNewNextOption(String oldOption, String newOption, String oldNextOption);
	}
	
	public static interface DropDownLayout {
		public abstract int getDistance(String previousOption, String option);
		public abstract Color getColor(String option);
	}
	
	public static class DefaultDropDownLayout implements DropDownLayout {
		private HashMap optionColors = new HashMap();
		public Color getColor(String option) {
			Color optionColor = ((Color) optionColors.get(option));
			if (optionColor == null) {
				optionColor = new Color(Color.HSBtoRGB(((float) Math.random()), 0.5f, 1.0f));
				optionColors.put(option, optionColor);
			}
			return optionColor;
		}
		public int getDistance(String previousOption, String option) {
			if ((previousOption == null) || (option == null))
				return 0;
			else return (previousOption.equals(option) ? 0 : 10);
		}
	}
	
	private PropagationStrategy ps;
	private boolean propagating = true;
	
	private DropDownLayout ddl;
	private GridBagLayout gbl = new GridBagLayout();
	
	LinePanel[] lines;
	
	public CategorizationFlexFeedbackPanel(DropDownLayout ddl, PropagationStrategy ps, String[] texts, String[] options, String[] selectedOptions, String defaultOption) {
		super(true);
		this.ddl = ((ddl == null) ? new DefaultDropDownLayout() : ddl);
		this.setLayout(this.gbl);
		
		if (options.length == 0)
			throw new IllegalArgumentException("Option array invalid, must not be empty.");
		
		if (defaultOption == null)
			defaultOption = options[0];
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets.top = 0;
		gbc.insets.bottom = 0;
		gbc.insets.left = 0;
		gbc.insets.right = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		
		this.lines = new LinePanel[texts.length];
		for (int l = 0; l < this.lines.length; l++) {
			GridBagConstraints lpGbc = ((GridBagConstraints) gbc.clone());
			
			this.lines[l] = new LinePanel(texts[l], options, ((selectedOptions[l] == null) ? defaultOption : selectedOptions[l]), lpGbc);
			
			if (l != 0) {
				this.lines[l-1].next = this.lines[l];
				this.lines[l].previous = this.lines[l-1];
			}
			
			this.add(this.lines[l], this.lines[l].gbc);
			gbc.gridy++;
		}
		this.layoutLines(this.lines[0], null);
		
		this.ps = ps;
		this.propagating = false;
	}
	
	void layoutLines(LinePanel startLp, LinePanel stopLp) {
		boolean doLayout = false;
		
		LinePanel lp = startLp;
		while (lp != null) {
			int previousDist = this.ddl.getDistance(((lp.previous == null) ? null : lp.previous.option), lp.option);
			if (lp.gbc.insets.top != previousDist) {
				lp.gbc.insets.top = previousDist;
				this.gbl.setConstraints(lp, lp.gbc);
				doLayout = true;
			}
			lp = ((lp == stopLp) ? null : lp.next);
		}
		
		if (doLayout)
			this.gbl.layoutContainer(this);
	}
	
	void setOption(LinePanel startLp, String option) {
		LinePanel lp = startLp;
		LinePanel stopLp = null;
		this.propagating = true;
		while (lp != null) {
			
			String nextOption;
			if ((lp.next == null) || (this.ps == null))
				nextOption = null;
			else {
				nextOption = this.ps.getNewNextOption(lp.option, option, lp.next.option);
				if (lp.next.option.equals(nextOption))
					nextOption = null;
			}
			lp.option = option;
			lp.optionBox.setSelectedItem(option);
			
			if (nextOption == null) {
				stopLp = lp.next;
				lp = null;
			}
			else {
				lp = lp.next;
				option = nextOption;
			}
		}
		this.propagating = false;
		this.layoutLines(startLp, stopLp);
	}
	
	private class LinePanel extends JPanel {
		String option;
		JComboBox optionBox;
		
		JLabel textLabel;
		
		LinePanel previous;
		LinePanel next;
		
		GridBagConstraints gbc;
		
		LinePanel(String text, String[] options, String selectedOption, GridBagConstraints gbc) {
			super(new BorderLayout(), true);
			
			this.gbc = gbc;
			
			this.textLabel = new JLabel(text, JLabel.LEFT);
			this.textLabel.setOpaque(true);
			this.textLabel.setBackground(ddl.getColor(selectedOption));
			
			this.option = selectedOption;
			
			this.optionBox = new JComboBox(options);
			this.optionBox.setSelectedItem(selectedOption);
			this.optionBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (ie.getStateChange() == ItemEvent.SELECTED) {
						String newOption = optionBox.getSelectedItem().toString();
						
						if (!propagating)
							setOption(LinePanel.this, newOption);
						
						textLabel.setBackground(ddl.getColor(newOption));
						textLabel.repaint();
					}
				}
			});
			this.add(this.optionBox, BorderLayout.WEST);
			this.add(this.textLabel, BorderLayout.CENTER);
		}
	}
	
	public String[] getSelectedOptions() {
		String[] options = new String[this.lines.length];
		for (int l = 0; l < this.lines.length; l++)
			options[l] = this.lines[l].optionBox.getSelectedItem().toString();
		return options;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		final JFrame frame = new JFrame("DropDown Test");
		
//		PropagationStrategy ps = new PropagationStrategy() {
//			public String getNewNextOption(String oldOption, String newOption, String oldNextOption) {
//				return (oldOption.equals(oldNextOption) ? newOption : null);
//			}
//		};
//		
//		String[] texts = {"Test 1", "Test 2", "Test 3", "Test 4"};
//		String[] options = {"A", "B", "C"};
//		String[] selectedOptions = {"C", "A", "B", "C"};
		
		DropDownLayout ddl = new DefaultDropDownLayout() {
			public int getDistance(String previousOption, String option) {
				if ("S".equals(option))
					return 10;
				else if ("C".equals(option))
					return 0;
				else return super.getDistance(previousOption, option);
			}
		};
		
		PropagationStrategy ps = new PropagationStrategy() {
			public String getNewNextOption(String oldOption, String newOption, String oldNextOption) {
				if ("S".equals(oldNextOption))
					return null;
				else if ("S".equals(newOption) || "C".equals(newOption))
					return "C";
				else if ("O".equals(newOption))
					return "O";
				else return null;
//				if (continueLabel.equals(newOption) && ((previous == null) || otherLabel.equals(previous.optionBox.getSelectedItem()))) {
//					JOptionPane.showMessageDialog(LinePanel.this, "Cannot continue without start.", "Invalid Selection", JOptionPane.ERROR_MESSAGE);
//				}
//				return (oldOption.equals(oldNextOption) ? newOption : null);
			}
		};
		
		String[] texts = {"Test 1", "Test 2", "Test 3", "Test 4"};
		String[] options = {"S", "C", "O"};
		String[] selectedOptions = {"O", "S", "C", "O"};
		
		final CategorizationFlexFeedbackPanel fddt = new CategorizationFlexFeedbackPanel(ddl, ps, texts, options, selectedOptions, null);
		
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				String[] options = fddt.getSelectedOptions();
				for (int o = 0; o < options.length; o++)
					System.out.println(options[o]);
			}
		});
		
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(fddt);
		frame.setSize(400, 600);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}

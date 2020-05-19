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
package de.uka.ipd.idaho.plugins.dateTime.analyzers;


import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer;
import de.uka.ipd.idaho.gamta.util.Analyzer;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.SgmlDocumentReader;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.constants.NamedEntityConstants;
import de.uka.ipd.idaho.plugins.dateTime.DateTimeUtils;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This analyzer tags and normalizes dates and ranges thereof. Normalized
 * values have the form YYYY-MM-DD, and ranges have their minimum and maximum
 * value attached in respective attributes.
 * 
 * @author sautter
 */
public class DateTagger extends AbstractConfigurableAnalyzer implements NamedEntityConstants {
	
	//	TODO add default config files to JARs
	
	private static final boolean DEBUG = false;
	
	private static final String DAY = "day";
	private static final String MONTH = "month";
	private static final String YEAR = "year";
	private static final String LAST_PREFIX = "last_";
	
	private static final String monthName = "monthName";
	private static final String datePartOrder = "partOrder";
	private static final String bridgedPair = "bridgedPair";
	private static final String bridgedDate = "bridgedDate";
	
	private String yearRegEx = "(([12][0-9]{3})|(\'?[0-9]{2}))";
	
	private StringVector days = new StringVector();
	private StringVector[] daysOrdered;
	
	private StringVector months = new StringVector();
	private StringVector[] monthsOrdered;
	
	private String[] datePatterns = new String[0];
	private String[] dateParsePatterns = new String[0];
	
	private StringVector rangeIndicators = new StringVector();
	private StringVector enumSeparators = new StringVector();
	
	private StringVector units = new StringVector();
	private Pattern numberPattern = Pattern.compile("[0-9]++((\\.|\\,)[0-9]++)?+");
	private static final String dateSeparators = ".,-/";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AbstractConfigurableAnalyzer#initAnalyzer()
	 */
	public void initAnalyzer() {
		try {
			StringVector dayLines = this.loadList(DAY + ".txt");
			this.daysOrdered = new StringVector[dayLines.size()];
			for (int d = 0; d < dayLines.size(); d++) {
				this.daysOrdered[d] = new StringVector();
				this.daysOrdered[d].parseAndAddElements(dayLines.get(d), ";");
				this.daysOrdered[d].removeAll("");
				this.daysOrdered[d].removeDuplicateElements();
				this.days.addContentIgnoreDuplicates(this.daysOrdered[d]);
			}
		}
		catch (IOException ioe) {
			this.daysOrdered = DateTimeUtils.getDayDictionaries();
			for (int d = 0; d < this.daysOrdered.length; d++)
				this.days.addContentIgnoreDuplicates(this.daysOrdered[d]);
		}
		
		try {
			StringVector monthLines = this.loadList(MONTH + ".txt");
			this.monthsOrdered = new StringVector[monthLines.size()];
			for (int m = 0; m < monthLines.size(); m++) {
				this.monthsOrdered[m] = new StringVector();
				this.monthsOrdered[m].parseAndAddElements(monthLines.get(m), ";");
				this.monthsOrdered[m].removeAll("");
				this.monthsOrdered[m].removeDuplicateElements();
				this.months.addContentIgnoreDuplicates(this.monthsOrdered[m]);
			}
		}
		catch (IOException ioe) {
			this.monthsOrdered = DateTimeUtils.getMonthDictionaries();
			for (int m = 0; m < this.monthsOrdered.length; m++)
				this.months.addContentIgnoreDuplicates(this.monthsOrdered[m]);
		}
		
		try {
			StringVector patternLines = this.loadList("patterns.txt");
			StringVector datePatterns = new StringVector();
			StringVector dateParsePatterns = new StringVector();
			for (int p = 0; p < patternLines.size(); p++) {
				String patternLine = patternLines.get(p);
				if (!patternLine.startsWith("//")) {
					String[] patternParts = patternLine.trim().split("\\s++");
					if (patternParts.length == 2) {
						datePatterns.addElement(patternParts[0]);
						dateParsePatterns.addElement(patternParts[1]);
					}
				}
			}
			this.datePatterns = datePatterns.toStringArray();
			this.dateParsePatterns = dateParsePatterns.toStringArray();
		} catch (IOException ioe) {}
		
		try {
			StringVector unitLines = this.loadList("units.txt");
			for (int u = 0; u < unitLines.size(); u++) {
				String unitLine = unitLines.get(u);
				if (!unitLine.startsWith("//"))
					this.units.addElement(unitLine);
			}
		} catch (IOException ioe) {}
		
		try {
			StringVector rangeIndicatorLines = this.loadList("rangeIndicators.txt");
			for (int r = 0; r < rangeIndicatorLines.size(); r++) {
				String rangeIndicatorLine = rangeIndicatorLines.get(r);
				if (!rangeIndicatorLine.startsWith("//"))
					this.rangeIndicators.addElement(rangeIndicatorLine);
			}
		}
		catch (IOException ioe) {
			this.rangeIndicators.parseAndAddElements("to;through;bis;a;-", ";");
		}
		
		try {
			StringVector enumSeparatorsLines = this.loadList("enumSeparators.txt");
			for (int e = 0; e < enumSeparatorsLines.size(); e++) {
				String enumSeparatorLine = enumSeparatorsLines.get(e);
				if (!enumSeparatorLine.startsWith("//"))
					this.enumSeparators.addElement(enumSeparatorLine);
			}
		}
		catch (IOException ioe) {
			this.enumSeparators.parseAndAddElements(",;&;and;und;et;y", ";");
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		
		//	prepare day lists for document
		StringVector[] docDaysOrdered = new StringVector[this.daysOrdered.length];
		StringVector docDays = new StringVector();
		for (int d = 0; d < this.daysOrdered.length; d++) {
			docDaysOrdered[d] = new StringVector();
			for (int e = 0; e < this.daysOrdered[d].size(); e++) {
				String day = this.daysOrdered[d].get(e);
				docDaysOrdered[d].addElementIgnoreDuplicates(day);
				docDays.addElementIgnoreDuplicates(day);
				
				TokenSequence dayTokens = data.getTokenizer().tokenize(day);
				if (!Gamta.isPunctuation(dayTokens.lastValue()))
					for (int s = 0; s < dateSeparators.length(); s++) {
						String sep = dateSeparators.substring(s, (s+1));
						String sDay = (day + sep);
						TokenSequence sDayTokens = data.getTokenizer().tokenize(sDay);
						if (!sep.equals(sDayTokens.lastValue())) {
							docDaysOrdered[d].addElementIgnoreDuplicates(sDay);
							docDays.addElementIgnoreDuplicates(sDay);
						}
					}
			}
		}
		
		//	prepare month lists for document
		StringVector[] docMonthsOrdered = new StringVector[this.monthsOrdered.length];
		StringVector docMonths = new StringVector();
		for (int m = 0; m < this.monthsOrdered.length; m++) {
			docMonthsOrdered[m] = new StringVector();
			for (int e = 0; e < this.monthsOrdered[m].size(); e++) {
				String month = this.monthsOrdered[m].get(e);
				docMonthsOrdered[m].addElementIgnoreDuplicates(month);
				docMonths.addElementIgnoreDuplicates(month);
				
				TokenSequence monthTokens = data.getTokenizer().tokenize(month);
				if (!Gamta.isPunctuation(monthTokens.lastValue()))
					for (int s = 0; s < dateSeparators.length(); s++) {
						String sep = dateSeparators.substring(s, (s+1));
						String sMonth = (month + sep);
						TokenSequence sMonthTokens = data.getTokenizer().tokenize(sMonth);
						if (!sep.equals(sMonthTokens.lastValue())) {
							docMonthsOrdered[m].addElementIgnoreDuplicates(sMonth);
							docMonths.addElementIgnoreDuplicates(sMonth);
						}
					}
			}
		}
		
		
		//	annotate days, assign normalized value
		Annotation[] days = this.getDaysMonths(data, docDays, DAY, docDaysOrdered);
		
		//	annotate day ranges
		days = this.addRanges(data, days, DAY, AnnotationUtils.getComparator(data.getAnnotationNestingOrder()));
		
		//	index days and day ranges
		HashMap daysByStartIndex = new HashMap();
		HashMap daysByEndIndex = new HashMap();
		this.index(days, daysByStartIndex, daysByEndIndex);
		
		//	annotate months, assign normalized value, and index them
		Annotation[] months = this.getDaysMonths(data, docMonths, MONTH, docMonthsOrdered);
		
		//	annotate and index month ranges
		months = this.addRanges(data, months, MONTH, AnnotationUtils.getComparator(data.getAnnotationNestingOrder()));
		
		//	index months and month ranges
		HashMap monthsByEndIndex = new HashMap();
		this.index(months, null, monthsByEndIndex);
		
		//	annotate years
		Annotation[] years = this.getYears(data);
		
		//	build pairs
		Annotation[] dayMonth = this.buildPairs(data, days, months, "-/");
		Annotation[] monthDay = this.buildPairs(data, months, days, "-/");
		
		Annotation[] monthYear = this.buildPairs(data, months, years, "-/");
		Annotation[] yearMonth = this.buildPairs(data, years, months, "-/");
		
		Annotation[] dayYear = this.buildPairs(data, days, years, ",-/");
		Annotation[] yearDay = this.buildPairs(data, years, days, ",-/");
		
		
		//	add enumerations of days preceding month
		dayMonth = this.addLeadingDayEnums(data, dayMonth, daysByEndIndex, AnnotationUtils.getComparator(data.getAnnotationNestingOrder()));
		
		//	annotate dayMonth ranges
		dayMonth = this.addDayMonthRanges(data, dayMonth, AnnotationUtils.getComparator(data.getAnnotationNestingOrder()));
		
		//	index dayMonths and ranges
		HashMap dayMonthsByEndIndex = new HashMap();
		this.index(dayMonth, null, dayMonthsByEndIndex);
		
		
		//	add enumerations of days succeeding month, and index month/day pairs
		monthDay = this.addTailingDayEnums(data, monthDay, daysByStartIndex, AnnotationUtils.getComparator(data.getAnnotationNestingOrder()));
		
		//	annotate monthDay ranges
		monthDay = this.addMonthDayRanges(data, monthDay, AnnotationUtils.getComparator(data.getAnnotationNestingOrder()));
		
		//	index monthDays and ranges
		HashMap monthDaysByEndIndex = new HashMap();
		this.index(monthDay, null, monthDaysByEndIndex);
		
		//	build dates
		ArrayList dateList = new ArrayList();
		Annotation[] dates;
		
		dates = this.buildDates(data, dayMonth, years, "-/");
		for (int d = 0; d < dates.length; d++) {
			dates[d].setAttribute(datePartOrder, "dmy");
			dateList.add(dates[d]);
		}
		
		dates = this.buildDates(data, monthDay, years, ",-/"); // comma for English 'month day, year' syntax
		for (int d = 0; d < dates.length; d++) {
			dates[d].setAttribute(datePartOrder, "mdy");
			dateList.add(dates[d]);
		}
		
		dates = this.buildDates(data, monthYear, days, "-/");
		for (int d = 0; d < dates.length; d++)
			dateList.add(dates[d]);
		
		dates = this.buildDates(data, yearMonth, days, "-/");
		for (int d = 0; d < dates.length; d++)
			dateList.add(dates[d]);
		
		dates = this.buildDates(data, dayYear, months, "-/");
		for (int d = 0; d < dates.length; d++)
			dateList.add(dates[d]);
		
		dates = this.buildDates(data, yearDay, months, "-/");
		for (int d = 0; d < dates.length; d++)
			dateList.add(dates[d]);
		
		if (DEBUG) System.out.println("Got " + dateList.size() + " raw dates");
		
		//	sort out overlapping matches
		if (!dateList.isEmpty()) {
			Collections.sort(dateList);
			Annotation lastDate = ((Annotation) dateList.get(0));
			for (int d = 1; d < dateList.size(); d++) {
				Annotation date = ((Annotation) dateList.get(d));
				if (AnnotationUtils.equals(lastDate, date))
					dateList.remove(d--);
				else if (AnnotationUtils.overlaps(lastDate, date)) {
					String lastYear = lastDate.getAttribute(YEAR, "").toString();
					String year = date.getAttribute(YEAR, "").toString();
					if (lastYear.length() < year.length()) {
						dateList.remove(--d);
						lastDate = date;
					}
					else if (lastYear.length() > year.length())
						dateList.remove(d--);
					else lastDate = date;
				}
				else lastDate = date;
			}
		}
		
		if (DEBUG) System.out.println("Got " + dateList.size() + " dates after overlap elimination");
		
		//	annotate full dates
		for (int d = 0; d < dateList.size(); d++) {
			Annotation date = ((Annotation) dateList.get(d));
			data.addAnnotation(date);
			
			//	if date is month-day-year or day-month-year, add preceding month/day or day/month pairs
			HashMap prePairsByEndIndex = null;
			if ("mdy".equals(date.getAttribute(datePartOrder)))
				prePairsByEndIndex = monthDaysByEndIndex;
			else if ("dmy".equals(date.getAttribute(datePartOrder)))
				prePairsByEndIndex = dayMonthsByEndIndex;
			
			//	do we have something to work with?
			else continue;
//			HashMap prePairRangesByEndIndex = null;
//			HashMap prePairsByEndIndex = null;
//			if ("mdy".equals(date.getAttribute(datePartOrder))) {
//				prePairRangesByEndIndex = monthDayRangesByEndIndex;
//				prePairsByEndIndex = monthDaysByEndIndex;
//			}
//			else if ("dmy".equals(date.getAttribute(datePartOrder))) {
//				prePairRangesByEndIndex = dayMonthRangesByEndIndex;
//				prePairsByEndIndex = dayMonthsByEndIndex;
//			}
//			
//			//	do we have something to work with?
//			if ((prePairRangesByEndIndex == null) || (prePairsByEndIndex == null))
//				continue;
			
			//	search backward for standalone months of matching style
			int lastEnumElementStart = date.getStartIndex();
			
			//	check if there could be a preceding enumeration
			if (lastEnumElementStart < 2)
				continue;
			
			//	get year for transfer
			String year = ((String) date.getAttribute(YEAR));
			
			//	do search
			while (lastEnumElementStart >= 2) {
				
				//	enumeration does not continue
				if (!this.enumSeparators.contains(data.valueAt(lastEnumElementStart-1)))
					break;
				
				//	try to find month+day range or single month+day
//				Annotation preDate = ((Annotation) prePairRangesByEndIndex.get(new Integer(lastEnumElementStart-1)));
//				if (preDate == null)
//					preDate = ((Annotation) prePairsByEndIndex.get(new Integer(lastEnumElementStart-1)));
				Annotation preDate = ((Annotation) prePairsByEndIndex.get(new Integer(lastEnumElementStart-1)));
				if (preDate == null)
					break;
				
				//	annotate month+day date
				Annotation preMonthDate = data.addAnnotation(DATE_TYPE, preDate.getStartIndex(), preDate.size());
				preMonthDate.setAttribute(DAY, preDate.getAttribute(DAY));
				if (preDate.hasAttribute(LAST_PREFIX + DAY))
					preMonthDate.setAttribute((LAST_PREFIX + DAY), preDate.getAttribute(LAST_PREFIX + DAY));
				preMonthDate.setAttribute(MONTH, preDate.getAttribute(MONTH));
				if (preDate.hasAttribute(LAST_PREFIX + MONTH))
					preMonthDate.setAttribute((LAST_PREFIX + MONTH), preDate.getAttribute(LAST_PREFIX + MONTH));
				preMonthDate.setAttribute(YEAR, year);
				if (DEBUG) System.out.println("Got enumerated date: " + preMonthDate.toXML());
				
				//	move on
				lastEnumElementStart = preMonthDate.getStartIndex();
			}
		}
		if (DEBUG) System.out.println("Full dates annotated");
		
		//	annotate day/month and month/day dates
		for (int d = 0; d < dayMonth.length; d++) {
			TokenSequence month = data.getTokenizer().tokenize(dayMonth[d].getAttribute(monthName, "").toString());
			if (month.size() == 0)
				continue;
			if (Gamta.isNumber(month.firstValue()))
				continue;
			if (dayMonth[d].hasAttribute(bridgedPair) && !Gamta.isRomanNumber(month.firstValue()))
				continue;
			
			dayMonth[d].changeTypeTo(DATE_TYPE);
			data.addAnnotation(dayMonth[d]);
		}
		if (DEBUG) System.out.println("Day/month dates annotated");
		for (int m = 0; m < monthDay.length; m++) {
			TokenSequence month = data.getTokenizer().tokenize(monthDay[m].getAttribute(monthName, "").toString());
			if (month.size() == 0)
				continue;
			if (Gamta.isNumber(month.firstValue()))
				continue;
			if (monthDay[m].hasAttribute(bridgedPair) && !Gamta.isRomanNumber(month.firstValue()))
				continue;
			
			monthDay[m].changeTypeTo(DATE_TYPE);
			data.addAnnotation(monthDay[m]);
		}
		if (DEBUG) System.out.println("Month/day dates annotated");
		
		//	annotate month/year dates
		for (int m = 0; m < monthYear.length; m++) {
			
			//	check month style
			TokenSequence month = data.getTokenizer().tokenize(monthYear[m].getAttribute(monthName, "").toString());
			if (month.size() == 0)
				continue;
			if (Gamta.isNumber(month.firstValue()))
				continue;
			if (monthYear[m].hasAttribute(bridgedPair) && !Gamta.isRomanNumber(month.firstValue()))
				continue;
			
			//	annotate date
			monthYear[m].changeTypeTo(DATE_TYPE);
			data.addAnnotation(monthYear[m]);
			
			//	search backward for standalone months of matching style
			int lastEnumElementStart = monthYear[m].getStartIndex();
			
			//	check if there could be a preceding enumeration
			if (lastEnumElementStart < 2)
				continue;
			
			//	get numeric month, year, and style
			int monthInt;
			try {
				monthInt = Integer.parseInt((String) monthYear[m].getAttribute(MONTH));
			}
			catch (NumberFormatException nfe) {
				continue;
			}
			String year = monthYear[m].lastValue();
			boolean romanNumberStyle = Gamta.isRomanNumber(month.firstValue());
			
			//	do search
			while (lastEnumElementStart >= 2) {
				
				//	enumeration does not continue
				if (!this.enumSeparators.contains(data.valueAt(lastEnumElementStart-1)))
					break;
				
				//	try to find month range or single month
//				Annotation preMonth = ((Annotation) monthRangesByEndIndex.get(new Integer(lastEnumElementStart-1)));
//				if (preMonth == null)
//					preMonth = ((Annotation) monthsByEndIndex.get(new Integer(lastEnumElementStart-1)));
				Annotation preMonth = ((Annotation) monthsByEndIndex.get(new Integer(lastEnumElementStart-1)));
				if (preMonth == null)
					break;
				
				//	get numeric preceding month
				int preMonthInt;
				try {
					preMonthInt = Integer.parseInt((String) preMonth.getAttribute(VALUE_ATTRIBUTE));
					if (preMonth.hasAttribute(VALUE_MAX_ATTRIBUTE))
						preMonthInt = Integer.parseInt((String) preMonth.getAttribute(VALUE_MAX_ATTRIBUTE));
				}
				catch (NumberFormatException nfe) {
					break;
				}
				
				//	check numeric plausibility
				if (monthInt <= preMonthInt)
					break;
				
				//	check style
				if (romanNumberStyle ? !Gamta.isRomanNumber(preMonth.firstValue()) : !Gamta.isWord(preMonth.firstValue()))
					break;
				
				//	annotate month date
				Annotation preMonthDate = data.addAnnotation(DATE_TYPE, preMonth.getStartIndex(), preMonth.size());
				preMonthDate.setAttribute(MONTH, preMonth.getAttribute(VALUE_ATTRIBUTE));
				if (preMonth.hasAttribute(VALUE_MAX_ATTRIBUTE))
					preMonthDate.setAttribute((LAST_PREFIX + MONTH), preMonth.getAttribute(VALUE_MAX_ATTRIBUTE));
				preMonthDate.setAttribute(YEAR, year);
				if (DEBUG) System.out.println("Got enumerated date: " + preMonthDate.toXML());
				
				//	move on
				lastEnumElementStart = preMonthDate.getStartIndex();
				monthInt = preMonthInt;
			}
		}
		if (DEBUG) System.out.println("Month/year dates annotated");
		
		//	annotate year/month dates
		for (int y = 0; y < yearMonth.length; y++) {
			TokenSequence month = data.getTokenizer().tokenize(yearMonth[y].getAttribute(monthName, "").toString());
			if (month.size() == 0)
				continue;
			if (Gamta.isNumber(month.firstValue()))
				continue;
			if (yearMonth[y].hasAttribute(bridgedPair) && !Gamta.isRomanNumber(month.firstValue()))
				continue;
			yearMonth[y].changeTypeTo(DATE_TYPE);
			data.addAnnotation(yearMonth[y]);
		}
		if (DEBUG) System.out.println("Year/month dates annotated");
		
		//	clean up partial dates in full dates
		AnnotationFilter.removeInner(data, DATE_TYPE);
		
		//	clean up and set attributes
		dates = data.getAnnotations(DATE_TYPE);
		for (int d = 0; d < dates.length; d++) {
			
			//	remove month name and part order
			dates[d].removeAttribute(monthName);
			dates[d].removeAttribute(datePartOrder);
			
			//	not normalized so far
			if (!dates[d].hasAttribute(VALUE_ATTRIBUTE) && dates[d].hasAttribute(YEAR) && dates[d].hasAttribute(MONTH)) {
				dates[d].setAttribute(VALUE_ATTRIBUTE, (dates[d].getAttribute(YEAR) + "-" + dates[d].getAttribute(MONTH)));
				if (dates[d].hasAttribute(LAST_PREFIX + MONTH)) {
					dates[d].setAttribute(VALUE_MIN_ATTRIBUTE, (dates[d].getAttribute(YEAR) + "-" + dates[d].getAttribute(MONTH) + "-" + dates[d].getAttribute(DAY, "01")));
					String endMonth = ((String) dates[d].getAttribute(LAST_PREFIX + MONTH));
					String endDay = ((String) dates[d].getAttribute(LAST_PREFIX + DAY));
					if (endDay == null) {
						if ("04;06;09;11".indexOf(endMonth) != -1)
							endDay = "30";
						else if ("02".equals(endMonth)) {
							String yearStr = ((String) dates[d].getAttribute(YEAR));
							try {
								int year = Integer.parseInt(yearStr);
								if ((year % 400) == 0)
									endDay = "29";
								else if ((year % 100) == 0)
									endDay = "28";
								else if ((year % 4) == 0)
									endDay = "29";
								else endDay = "28";
							}
							catch (NumberFormatException nfe) {
								endDay = "28";
							}
						}
						else endDay = "31";
					}
					dates[d].setAttribute(VALUE_MAX_ATTRIBUTE, (dates[d].getAttribute(YEAR) + "-" + endMonth + "-" + endDay));
				}
			}
		}
		if (DEBUG) System.out.println("Got " + dates.length + " dates after cleanup");
		
		//	use specialized patterns for dates that are a single token due to in-number punctuation
		for (int p = 0; p < this.datePatterns.length; p++) {
			/*
			 * Three tokens are enough: number with inner dot, dot between
			 * numbers, and last number; without in-number punctuation, we don't
			 * need this at all, and with four or more tokens, the piece by
			 * piece tagging above kicks in.
			 */
			dates = Gamta.extractAllMatches(data, this.datePatterns[p], 3);
			
			for (int d = 0; d < dates.length; d++) {
				String date = dates[d].getValue();
				date = date.replaceAll("\\s+", "");
				
				if (date.length() == this.dateParsePatterns[p].length()) {
					StringBuffer day = new StringBuffer();
					StringBuffer month = new StringBuffer();
					StringBuffer year = new StringBuffer();
					
					for (int c = 0; c < date.length(); c++) {
						char dc = date.charAt(c);
						char pc = this.dateParsePatterns[p].charAt(c);
						if (pc == 'd') day.append(dc);
						else if (pc == 'm') month.append(dc);
						else if (pc == 'y') year.append(dc);
					}
					
					if (day.length() == 1)
						day.insert(0, "0");
					if (day.length() != 0)
						dates[d].setAttribute(DAY, day.toString());
					
					if (month.length() == 1)
						month.insert(0, "0");
					if (month.length() != 0)
						dates[d].setAttribute(MONTH, month.toString());
					
					if (year.length() != 0)
						dates[d].setAttribute(YEAR, year.toString());
					
					if ((day.length() * month.length() * year.length()) != 0)
						dates[d].setAttribute(VALUE_ATTRIBUTE, (year.toString() + "-" + month.toString() + "-" + day.toString()));
					else if ((month.length() * year.length()) != 0)
						dates[d].setAttribute(VALUE_ATTRIBUTE, (year.toString() + "-" + month.toString()));
					else if (year.length() != 0)
						dates[d].setAttribute(VALUE_ATTRIBUTE, year.toString());
					
					dates[d].changeTypeTo(DATE_TYPE);
					Annotation yearDate = data.addAnnotation(dates[d]);
					
					//	check for year range only if it's a standalone year
					if ((day.length() + month.length()) != 0)
						continue;
					
					//	check only if it's a full year
					if (year.length() != 4)
						continue;
					
					//	get numeric value for comparison
					int yearInt;
					try {
						yearInt = Integer.parseInt(year.toString());
					}
					catch (NumberFormatException nfe) {
						continue;
					}
					
					//	nothing left to check
					if ((yearDate.getEndIndex() + 2) >= data.size())
						continue;
					
					//	this is a year range
					if (this.rangeIndicators.contains(data.valueAt(yearDate.getEndIndex())) && data.valueAt(yearDate.getEndIndex()+1).matches(this.yearRegEx)) try {
						String endYear = data.valueAt(yearDate.getEndIndex()+1);
						if (endYear.length() < year.length())
							endYear = (year.substring(0, (year.length() - endYear.length())) + endYear);
						int endYearInt = Integer.parseInt(endYear);
						if (yearInt < endYearInt) {
							Annotation yearRange = data.addAnnotation(DATE_TYPE, yearDate.getStartIndex(), (yearDate.size() + 2));
							yearRange.setAttribute(VALUE_ATTRIBUTE, year.toString());
							yearRange.setAttribute(VALUE_MAX_ATTRIBUTE, endYear);
						}
						continue;
					}
					catch (NumberFormatException nfe) {
						continue;
					}
					
					//	search forward for standalone two-digit years
					int lastEnumElementEnd = yearDate.getEndIndex();
					
					//	do search
					while ((lastEnumElementEnd + 2) <= data.size()) {
						
						//	enumeration does not continue
						if (!this.enumSeparators.contains(data.valueAt(lastEnumElementEnd)))
							break;
						
						//	next one is not a two-digit year
						if (!data.valueAt(lastEnumElementEnd+1).matches("[0-9]{2}"))
							break;
						
						//	check numeric value and annotate in case of success
						try {
							String enumYearString = data.valueAt(lastEnumElementEnd+1);
							if (enumYearString.length() < year.length())
								enumYearString = (year.substring(0, (year.length() - enumYearString.length())) + enumYearString);
							int enumYearInt = Integer.parseInt(enumYearString);
							if (yearInt < enumYearInt) {
								Annotation enumYear = data.addAnnotation(DATE_TYPE, (lastEnumElementEnd+1), 1);
								enumYear.setAttribute(VALUE_ATTRIBUTE, enumYearString);
								lastEnumElementEnd = enumYear.getEndIndex();
								yearInt = enumYearInt;
								if (DEBUG) System.out.println("Got enumerated year: " + enumYear.toXML());
							}
							else break;
						}
						catch (NumberFormatException nfe) {
							break;
						}
					}
				}
			}
		}
		if (DEBUG) System.out.println("Date patterns done");
		
		//	clean up partial dates nested in full dates
		AnnotationFilter.removeInner(data, DATE_TYPE);
		if (DEBUG) System.out.println("Nested dates removed");
		
		//	clean dates
		AnnotationFilter.removeAnnotationAttribute(data, DATE_TYPE, DAY);
		AnnotationFilter.removeAnnotationAttribute(data, DATE_TYPE, MONTH);
		AnnotationFilter.removeAnnotationAttribute(data, DATE_TYPE, YEAR);
		AnnotationFilter.removeAnnotationAttribute(data, DATE_TYPE, (LAST_PREFIX + DAY));
		AnnotationFilter.removeAnnotationAttribute(data, DATE_TYPE, (LAST_PREFIX + MONTH));
		AnnotationFilter.removeAnnotationAttribute(data, DATE_TYPE, (LAST_PREFIX + YEAR));
		if (DEBUG) System.out.println("Attribute cleanup done");
		
		//	clean up standalone might-be years that are followed by a length or area unit (quantities that happen to have a four-digit number)
		dates = data.getAnnotations(DATE_TYPE);
		for (int d = 0; d < dates.length; d++) {
			
			//	catch year ranges (might be quantity ranges ...)
			if (!dates[d].getValue().matches("[0-9]{4}\\s*\\-\\s*[0-9]{4}")) {
				
				//	this one's large enough to be secure
				if (dates[d].size() > 1)
					continue;
				
				//	this one's not a number, so ambiguity is low
				if (!this.numberPattern.matcher(dates[d].getValue()).matches())
					continue;
			}
			
			//	two digits only, remove it
			if (dates[d].length() < 4) {
				if (DEBUG) System.out.println("Removing two-digit year " + dates[d].getValue());
				data.removeAnnotation(dates[d]);
				continue;
			}
			
			//	we cannot check this one
			if (dates[d].getEndIndex() == data.size())
				continue;
			
			//	this one's probably a quantity ==> remove it
			if (this.units.containsIgnoreCase(data.valueAt(dates[d].getEndIndex()))) {
				if (DEBUG) System.out.println("Removing quantity " + dates[d].getValue() + " " + data.valueAt(dates[d].getEndIndex()));
				data.removeAnnotation(dates[d]);
			}
		}
		if (DEBUG) System.out.println("Quantity cleanup done");
		
		//	check for interleaving dates
		dates = data.getAnnotations(DATE_TYPE);
		boolean interleaving = false;
		int lastDateIndex = 0;
		for (int d = 1; d < dates.length; d++) {
			if (AnnotationUtils.overlaps(dates[lastDateIndex], dates[d])) {
				interleaving = true;
				d = dates.length;
			}
			else lastDateIndex = d;
		}
		
		//	all is fine, we're done
		if (!interleaving) return;
		
		//	count structures of dates
		StringIndex structureCounts = new StringIndex(true);
		String[] structures = new String[dates.length];
		String[] shortStructures = new String[dates.length];
		for (int d = 0; d < dates.length; d++) {
			structures[d] = this.getStructure(dates[d]);
			structureCounts.add(structures[d]);
			shortStructures[d] = this.shortenStructrure(structures[d]);
			structureCounts.add(shortStructures[d]);
		}
		if (DEBUG) System.out.println("Date structures generated");
		
		//	for each pair of interleaving dates, choose the one with the more frequent structure
		lastDateIndex = 0;
		for (int d = 1; d < dates.length; d++) {
			if (AnnotationUtils.overlaps(dates[lastDateIndex], dates[d])) {
				int f1 = structureCounts.getCount(shortStructures[lastDateIndex]);
				int f2 = structureCounts.getCount(shortStructures[d]);
				if (f1 == f2) {
					f1 = structureCounts.getCount(structures[lastDateIndex]);
					f2 = structureCounts.getCount(structures[d]);
				}
				if (f2 > f1) {
					if (DEBUG) System.out.println("Removing date '" + dates[lastDateIndex].getValue() + "' (" + shortStructures[lastDateIndex] + "," + f1 + ") for interleaving with '" + dates[d] + "'(" + shortStructures[d] + "," + f2 + ")");
					data.removeAnnotation(dates[lastDateIndex]);
					structureCounts.remove(structures[lastDateIndex]);
					structureCounts.remove(shortStructures[lastDateIndex]);
					lastDateIndex = d;
				}
				else {
					if (DEBUG) System.out.println("Removing date '" + dates[d].getValue() + "' (" + shortStructures[d] + "," + f2 + ") for interleaving with '" + dates[lastDateIndex] + "'(" + shortStructures[lastDateIndex] + "," + f1 + ")");
					data.removeAnnotation(dates[d]);
					structureCounts.remove(structures[d]);
					structureCounts.remove(shortStructures[d]);
				}
			}
			else lastDateIndex = d;
		}
		if (DEBUG) System.out.println("Overlapping cleanup done");
	}
	
	private Annotation[] getDaysMonths(TokenSequence data, StringVector all, String type, StringVector[] ordered) {
		Annotation[] dms = Gamta.extractAllContained(data, all, false);
		for (int dm = 0; dm < dms.length; dm++) {
			dms[dm].changeTypeTo(type);
			for (int o = 0; o < ordered.length; o++)
				if (ordered[o].containsIgnoreCase(dms[dm].getValue())) {
					dms[dm].setAttribute(VALUE_ATTRIBUTE, ordered[o].firstElement());
					break;
				}
			if (DEBUG) System.out.println("Got " + type + ": " + dms[dm].toXML());
		}
		return dms;
	}
	
	private Annotation[] getYears(Annotation data) {
		
		//	get document year for resolving two-digit years
		int docYear = Calendar.getInstance().get(Calendar.YEAR);
		if (DEBUG) System.out.println("Got calendar year: " + docYear);
		try {
			String docDate = ((String) data.getAttribute(LiteratureConstants.DOCUMENT_DATE_ATTRIBUTE));
			if (docDate != null) {
				docYear = Integer.parseInt(docDate);
				if (DEBUG) System.out.println("Got document year: " + docYear);
			}
			else if (DEBUG) System.out.println("Estimating document year: " + docYear);
		}
		catch (Exception e) {
			System.out.println("Error getting doc year: " + e.getMessage());
			System.out.println(" ==> Estimating doc year: " + docYear);
		}
		
		//	annotate years
		Annotation[] years = Gamta.extractAllMatches(data, this.yearRegEx, 2);
		for (int y = 0; y < years.length; y++) {
			years[y].changeTypeTo(YEAR);
			String value = years[y].getValue();
			if (value.length() < 4) {
				if (value.startsWith("'"))
					value = value.substring(1).trim();
				int numValue = Integer.parseInt(value);
				while ((numValue + 100) < docYear)
					numValue += 100;
				value = ("" + numValue);
			}
			years[y].setAttribute(VALUE_ATTRIBUTE, value);
			if (DEBUG) System.out.println("Got year: " + years[y].toXML());
		}
		
		//	finally ...
		return years;
	}
	
	private Annotation[] addRanges(TokenSequence data, Annotation[] points, String type, Comparator sortOrder) {
		ArrayList list = new ArrayList();
		for (int p = 1; p < points.length; p++) {
			
			//	annotate range if indexes match and intermediate token fits
			if ((points[p-1].getEndIndex() == (points[p].getStartIndex()-1)) && this.rangeIndicators.contains(data.valueAt(points[p-1].getEndIndex()))) {
				if (!points[p-1].hasAttribute(VALUE_ATTRIBUTE) || !points[p].hasAttribute(VALUE_ATTRIBUTE))
					continue;
				Annotation range = Gamta.newAnnotation(data, type, points[p-1].getStartIndex(), (points[p].getEndIndex() - points[p-1].getStartIndex()));
				range.setAttribute(VALUE_ATTRIBUTE, points[p-1].getAttribute(VALUE_ATTRIBUTE));
				range.setAttribute(VALUE_MAX_ATTRIBUTE, points[p].getAttribute(VALUE_ATTRIBUTE));
				if (((String) points[p-1].getAttribute(VALUE_ATTRIBUTE)).compareTo((String) points[p].getAttribute(VALUE_ATTRIBUTE)) < 0) {
					if (DEBUG) System.out.println(type + "Range: " + range.toXML());
					list.add(range);
				}
				else if (DEBUG) System.out.println(type + "Range with broken value order: " + range.toXML());
			}
		}
		
		//	merge up points and ranges
		if (list.size() != 0) {
			list.addAll(Arrays.asList(points));
			points = ((Annotation[]) list.toArray(new Annotation[list.size()]));
			Arrays.sort(points, sortOrder);
		}
		
		//	finally ...
		return points;
	}
	
	private Annotation[] addLeadingDayEnums(TokenSequence data, Annotation[] dayMonth, HashMap daysByEndIndex, Comparator sortOrder) {
		
		//	collect enumerated days with implicit month
		ArrayList dayMonthList = new ArrayList();
		for (int d = 0; d < dayMonth.length; d++) {
			
			//	search backward for standalone days
			int lastEnumElementStart = dayMonth[d].getStartIndex();
			
			//	check if there could be a preceding enumeration
			if (lastEnumElementStart < 2)
				continue;
			
			//	get numeric day, and month
			int dayInt;
			try {
				dayInt = Integer.parseInt((String) dayMonth[d].getAttribute(DAY));
			}
			catch (NumberFormatException nfe) {
				continue;
			}
			String month = ((String) dayMonth[d].getAttribute(MONTH));
			String monthNm = ((String) dayMonth[d].getAttribute(monthName));
			
			//	do search
			while (lastEnumElementStart >= 2) {
				
				//	enumeration does not continue
				if (!this.enumSeparators.contains(data.valueAt(lastEnumElementStart-1)))
					break;
				
				//	try to find day range or single day
				Annotation preDay = ((Annotation) daysByEndIndex.get(new Integer(lastEnumElementStart-1)));
				if (preDay == null)
					break;
				
				//	get numeric preceding day
				int preDayInt;
				try {
					preDayInt = Integer.parseInt((String) preDay.getAttribute(VALUE_ATTRIBUTE));
					if (preDay.hasAttribute(VALUE_MAX_ATTRIBUTE))
						preDayInt = Integer.parseInt((String) preDay.getAttribute(VALUE_MAX_ATTRIBUTE));
				}
				catch (NumberFormatException nfe) {
					break;
				}
				
				//	check numeric plausibility
				if (dayInt <= preDayInt)
					break;
				
				//	annotate day/month pair
				Annotation preDayMonth = Gamta.newAnnotation(data, null, preDay.getStartIndex(), preDay.size());
				preDayMonth.setAttribute(DAY, preDay.getAttribute(VALUE_ATTRIBUTE));
				if (preDay.hasAttribute(VALUE_MAX_ATTRIBUTE))
					preDayMonth.setAttribute((LAST_PREFIX + DAY), preDay.getAttribute(VALUE_MAX_ATTRIBUTE));
				preDayMonth.setAttribute(MONTH, month);
				preDayMonth.setAttribute(monthName, monthNm);
				dayMonthList.add(preDayMonth);
				
				//	move on
				lastEnumElementStart = preDayMonth.getStartIndex();
			}
		}
		if (DEBUG) System.out.println("Marked " + dayMonthList.size() + " enumerated days as day/month pairs");
		
		//	merge in any new annotations
		if (dayMonthList.size() != 0) {
			dayMonthList.addAll(Arrays.asList(dayMonth));
			dayMonth = ((Annotation[]) dayMonthList.toArray(new Annotation[dayMonthList.size()]));
			dayMonthList.clear();
			Arrays.sort(dayMonth, sortOrder);
		}
		
		//	finally ...
		return dayMonth;
	}
	
	private Annotation[] addTailingDayEnums(TokenSequence data, Annotation[] monthDay, HashMap daysByStartIndex, Comparator sortOrder) {
		
		//	collect enumerated days with implicit month
		ArrayList monthDayList = new ArrayList();
		for (int m = 0; m < monthDay.length; m++) {
			
			//	search forward for standalone months of matching style
			int lastEnumElementEnd = monthDay[m].getEndIndex();
			
			//	check if there could be a preceding enumeration
			if ((lastEnumElementEnd + 2) >= data.size())
				continue;
			
			//	get numeric day, and month
			int dayInt;
			try {
				dayInt = Integer.parseInt((String) monthDay[m].getAttribute(DAY));
			}
			catch (NumberFormatException nfe) {
				continue;
			}
			String month = ((String) monthDay[m].getAttribute(MONTH));
			String monthNm = ((String) monthDay[m].getAttribute(monthName));
			
			//	do search
			while ((lastEnumElementEnd + 2) <= data.size()) {
				
				//	enumeration does not continue
				if (!this.enumSeparators.contains(data.valueAt(lastEnumElementEnd)))
					break;
				
				//	try to find day range or single day
				Annotation sucDay = ((Annotation) daysByStartIndex.get(new Integer(lastEnumElementEnd+1)));
				if (sucDay == null)
					break;
				
				//	get numeric preceding day
				int sucDayInt;
				try {
					sucDayInt = Integer.parseInt((String) sucDay.getAttribute(VALUE_ATTRIBUTE));
					if (sucDay.hasAttribute(VALUE_MAX_ATTRIBUTE))
						sucDayInt = Integer.parseInt((String) sucDay.getAttribute(VALUE_MAX_ATTRIBUTE));
				}
				catch (NumberFormatException nfe) {
					break;
				}
				
				//	check numeric plausibility
				if (dayInt >= sucDayInt)
					break;
				
				//	annotate month/day pair
				Annotation sucMonthDay = Gamta.newAnnotation(data, null, sucDay.getStartIndex(), sucDay.size());
				sucMonthDay.setAttribute(DAY, sucDay.getAttribute(VALUE_ATTRIBUTE));
				if (sucDay.hasAttribute(VALUE_MAX_ATTRIBUTE))
					sucMonthDay.setAttribute((LAST_PREFIX + DAY), sucDay.getAttribute(VALUE_MAX_ATTRIBUTE));
				sucMonthDay.setAttribute(MONTH, month);
				sucMonthDay.setAttribute(monthName, monthNm);
				monthDayList.add(sucMonthDay);
				
				//	move on
				lastEnumElementEnd = sucMonthDay.getEndIndex();
			}
		}
		if (DEBUG) System.out.println("Marked " + monthDayList.size() + " enumerated days as month/day pairs");
		
		//	merge in any new annotations
		if (monthDayList.size() != 0) {
			monthDayList.addAll(Arrays.asList(monthDay));
			monthDay = ((Annotation[]) monthDayList.toArray(new Annotation[monthDayList.size()]));
			Arrays.sort(monthDay, sortOrder);
		}
		
		//	finally ...
		return monthDay;
	}
	
	private Annotation[] addDayMonthRanges(TokenSequence data, Annotation[] dayMonth, Comparator sortOrder) {
		
		//	collect ranges
		ArrayList dayMonthList = new ArrayList();
		for (int d = 1; d < dayMonth.length; d++) {
			if (dayMonth[d-1].getEndIndex() != (dayMonth[d].getStartIndex()-(dayMonth[d-1].lastValue().endsWith("-") ? 0 : 1)))
				continue;
			if (!dayMonth[d-1].lastValue().endsWith("-") && !this.rangeIndicators.contains(data.valueAt(dayMonth[d-1].getEndIndex())))
				continue;
			
			String month1 = ((String) dayMonth[d-1].getAttribute(monthName));
			if (month1 == null)
				continue;
			String month2 = ((String) dayMonth[d].getAttribute(monthName));
			if (month2 == null)
				continue;
			if (!this.monthStylesMatch(month1, month2))
				continue;
			
			Annotation dayMonthRange = Gamta.newAnnotation(data, "dayMonthRange", dayMonth[d-1].getStartIndex(), (dayMonth[d].getEndIndex() - dayMonth[d-1].getStartIndex()));
			dayMonthRange.copyAttributes(dayMonth[d-1]);
			dayMonthRange.setAttribute((LAST_PREFIX + DAY), dayMonth[d].getAttribute(DAY));
			dayMonthRange.setAttribute((LAST_PREFIX + MONTH), dayMonth[d].getAttribute(MONTH));
			if (DEBUG) System.out.println("DayMonthRange: " + dayMonthRange.toXML());
			dayMonthList.add(dayMonthRange);
		}
		
		//	merge in any new annotations
		if (dayMonthList.size() != 0) {
			dayMonthList.addAll(Arrays.asList(dayMonth));
			dayMonth = ((Annotation[]) dayMonthList.toArray(new Annotation[dayMonthList.size()]));
			Arrays.sort(dayMonth, sortOrder);
		}
		
		//	finally ...
		return dayMonth;
	}
	
	private Annotation[] addMonthDayRanges(TokenSequence data, Annotation[] monthDay, Comparator sortOrder) {
		
		//	collect ranges
		ArrayList monthDayList = new ArrayList();
		for (int m = 1; m < monthDay.length; m++) {
			if (monthDay[m-1].getEndIndex() != (monthDay[m].getStartIndex()-1))
				continue;
			if (!this.rangeIndicators.contains(data.valueAt(monthDay[m-1].getEndIndex())))
				continue;
			
			String month1 = ((String) monthDay[m-1].getAttribute(monthName));
			if (month1 == null)
				continue;
			String month2 = ((String) monthDay[m].getAttribute(monthName));
			if (month2 == null)
				continue;
			if (!this.monthStylesMatch(month1, month2))
				continue;
			
			Annotation monthDayRange = Gamta.newAnnotation(data, "monthDayRange", monthDay[m-1].getStartIndex(), (monthDay[m].getEndIndex() - monthDay[m-1].getStartIndex()));
			monthDayRange.copyAttributes(monthDay[m-1]);
			monthDayRange.setAttribute((LAST_PREFIX + DAY), monthDay[m].getAttribute(DAY));
			monthDayRange.setAttribute((LAST_PREFIX + MONTH), monthDay[m].getAttribute(MONTH));
			if (DEBUG) System.out.println("MonthDayRange: " + monthDayRange.toXML());
			monthDayList.add(monthDayRange);
		}
		
		//	merge in any new annotations
		if (monthDayList.size() != 0) {
			monthDayList.addAll(Arrays.asList(monthDay));
			monthDay = ((Annotation[]) monthDayList.toArray(new Annotation[monthDayList.size()]));
			Arrays.sort(monthDay, sortOrder);
		}
		
		//	finally ...
		return monthDay;
	}
	
	private Annotation[] buildPairs(TokenSequence data, Annotation[] first, Annotation[] second, String bridgable) {
		ArrayList pairs = new ArrayList();
		int si = 0;
		
		for (int f = 0; f < first.length; f++) {
			
			while ((si < second.length) && (second[si].getStartIndex() <= first[f].getStartIndex()))
				si++;
			
			if ((si < second.length) && 
					(
						(second[si].getStartIndex() == first[f].getEndIndex())
						|| 
						(
							(second[si].getStartIndex() == (first[f].getEndIndex() + 1))
							&&
							(bridgable != null)
							&&
							(bridgable.indexOf(data.valueAt(first[f].getEndIndex())) != -1)
						)
					)
				) {
				Annotation pair = Gamta.newAnnotation(data, null, first[f].getStartIndex(), (second[si].getEndIndex() - first[f].getStartIndex()));
				pair.setAttribute(first[f].getType(), first[f].getAttribute(VALUE_ATTRIBUTE));
				pair.setAttribute(second[si].getType(), second[si].getAttribute(VALUE_ATTRIBUTE));
				if (first[f].hasAttribute(VALUE_MAX_ATTRIBUTE))
					pair.setAttribute((LAST_PREFIX + first[f].getType()), first[f].getAttribute(VALUE_MAX_ATTRIBUTE));
				if (second[si].hasAttribute(VALUE_MAX_ATTRIBUTE))
					pair.setAttribute((LAST_PREFIX + second[si].getType()), second[si].getAttribute(VALUE_MAX_ATTRIBUTE));
				pairs.add(pair);
				
				if (second[si].getStartIndex() != first[f].getEndIndex())
					pair.setAttribute(bridgedPair, data.valueAt(first[f].getEndIndex()));
				
				if (MONTH.equals(first[f].getType())) {
					String month = first[f].getValue();
					while (month.endsWith(".") || month.endsWith("-"))
						month = month.substring(0, (month.length()-1)).trim();
					pair.setAttribute(monthName, month);
				}
				if (MONTH.equals(second[si].getType())) {
					String month = second[si].getValue();
					while (month.endsWith(".") || month.endsWith("-"))
						month = month.substring(0, (month.length()-1)).trim();
					pair.setAttribute(monthName, month);
				}
				if (DEBUG) System.out.println(first[f].getType() + "-" + second[si].getType() + " pair: " + pair.toXML());
			}
		}
		
		return ((Annotation[]) pairs.toArray(new Annotation[pairs.size()]));
	}
	
	private void index(Annotation[] parts, HashMap byStartIndex, HashMap byEndIndex) {
		for (int p = 0; p < parts.length; p++) {
			if (byStartIndex != null) {
				Integer startIndex = new Integer(parts[p].getStartIndex());
				if (!byStartIndex.containsKey(startIndex))
					byStartIndex.put(startIndex, parts[p]);
			}
			if (byEndIndex != null) {
				Integer endIndex = new Integer(parts[p].getEndIndex());
				if (!byEndIndex.containsKey(endIndex))
					byEndIndex.put(endIndex, parts[p]);
			}
		}
	}
	
	private Annotation[] buildDates(TokenSequence data, Annotation[] pairs, Annotation[] thirdParts, String bridgable) {
		ArrayList dates = new ArrayList();
		
		int tpi = 0;
		for (int p = 0; p < pairs.length; p++) {
			if (DEBUG) System.out.println("Trying to build date from pair (start=" + pairs[p].getStartIndex() + ") " + pairs[p].toXML());
			
			while ((tpi != 0) && ((tpi >= thirdParts.length) || (thirdParts[tpi].getStartIndex() > pairs[p].getEndIndex()))) {
				if (DEBUG && (tpi < thirdParts.length))
					System.out.println(" - recovering part (start=" + thirdParts[tpi].getStartIndex() + ") " + thirdParts[tpi].toXML());
				tpi--;
			}
			
			while ((tpi < thirdParts.length) && (thirdParts[tpi].getStartIndex() < pairs[p].getEndIndex())) {
				if (DEBUG) System.out.println(" - jumping part (start=" + thirdParts[tpi].getStartIndex() + ") " + thirdParts[tpi].toXML());
				tpi++;
			}
			
			if (DEBUG && (tpi < thirdParts.length))
				System.out.println(" - trying part (start=" + thirdParts[tpi].getStartIndex() + ") " + thirdParts[tpi].toXML());
			
			if ((tpi < thirdParts.length) && 
					(
						(thirdParts[tpi].getStartIndex() == pairs[p].getEndIndex())
						|| 
						(
							(thirdParts[tpi].getStartIndex() == (pairs[p].getEndIndex() + 1))
							&&
							(bridgable != null)
							&&
							(bridgable.indexOf(data.valueAt(pairs[p].getEndIndex())) != -1)
							&& // bridge only if to bridge part and bridged part of pair are equal, or if either of them or both are empty
							(
								!pairs[p].hasAttribute(bridgedPair)
								||
								data.valueAt(pairs[p].getEndIndex()).equals(pairs[p].getAttribute(bridgedPair))
							)
						)
					)
				) {
				Annotation date = Gamta.newAnnotation(data, null, pairs[p].getStartIndex(), (thirdParts[tpi].getEndIndex() - pairs[p].getStartIndex()));
				date.copyAttributes(pairs[p]);
				date.removeAttribute(monthName);
				date.setAttribute(thirdParts[tpi].getType(), thirdParts[tpi].getAttribute(VALUE_ATTRIBUTE));
				if (thirdParts[tpi].hasAttribute(VALUE_MAX_ATTRIBUTE))
					date.setAttribute((LAST_PREFIX + thirdParts[tpi].getType()), thirdParts[tpi].getAttribute(VALUE_MAX_ATTRIBUTE));
				date.changeTypeTo(DATE_TYPE);
				date.setAttribute(VALUE_ATTRIBUTE, (date.getAttribute(YEAR) + "-" + date.getAttribute(MONTH) + "-" + date.getAttribute(DAY)));
				if (date.hasAttribute(LAST_PREFIX + DAY) || date.hasAttribute(LAST_PREFIX + MONTH)) {
					date.setAttribute(VALUE_MIN_ATTRIBUTE, (date.getAttribute(YEAR) + "-" + date.getAttribute(MONTH) + "-" + date.getAttribute(DAY)));
					date.setAttribute(VALUE_MAX_ATTRIBUTE, (date.getAttribute(YEAR) + "-" + date.getAttribute((LAST_PREFIX + MONTH), date.getAttribute(MONTH)) + "-" + date.getAttribute((LAST_PREFIX + DAY), date.getAttribute(DAY))));
				}
				dates.add(date);
				
				if (thirdParts[tpi].getStartIndex() != pairs[p].getEndIndex())
					date.setAttribute(bridgedDate, data.valueAt(pairs[p].getEndIndex()));
				if (DEBUG) System.out.println(" ==> " + date.toXML());
			}
		}
		
		int pi = 0;
		for (int t = 0; t < thirdParts.length; t++) {
			if (DEBUG) System.out.println("Trying to build date from part (start=" + thirdParts[t].getStartIndex() + ") " + thirdParts[t].toXML());
			
			while ((pi != 0) && ((pi >= pairs.length) || (pairs[pi].getStartIndex() > thirdParts[t].getEndIndex()))) {
				if (DEBUG && (pi < pairs.length))
					System.out.println(" - recovering pair (start=" + pairs[pi].getStartIndex() + ") " + pairs[pi].toXML());
				pi--;
			}
			
			while ((pi < pairs.length) && (pairs[pi].getStartIndex() < thirdParts[t].getEndIndex())) {
				if (DEBUG) System.out.println(" - jumping pair (start=" + pairs[pi].getStartIndex() + ") " + pairs[pi].toXML());
				pi++;
			}
			
			if (DEBUG && (pi < pairs.length))
				System.out.println(" - trying pair (start=" + pairs[pi].getStartIndex() + ") " + pairs[pi].toXML());
			
			if ((pi < pairs.length) && 
					(
						(pairs[pi].getStartIndex() == thirdParts[t].getEndIndex())
						|| 
						(
							(pairs[pi].getStartIndex() == (thirdParts[t].getEndIndex() + 1))
							&&
							(bridgable != null)
							&&
							(bridgable.indexOf(data.valueAt(thirdParts[t].getEndIndex())) != -1)
						)
					)
				) {
				Annotation date = Gamta.newAnnotation(data, null, thirdParts[t].getStartIndex(), (pairs[pi].getEndIndex() - thirdParts[t].getStartIndex()));
				date.copyAttributes(pairs[pi]);
				date.removeAttribute(monthName);
				date.setAttribute(thirdParts[t].getType(), thirdParts[t].getAttribute(VALUE_ATTRIBUTE));
				if (thirdParts[t].hasAttribute(VALUE_MAX_ATTRIBUTE))
					date.setAttribute((LAST_PREFIX + thirdParts[t].getType()), thirdParts[t].getAttribute(VALUE_MAX_ATTRIBUTE));
				date.changeTypeTo(DATE_TYPE);
				date.setAttribute(VALUE_ATTRIBUTE, (date.getAttribute(YEAR) + "-" + date.getAttribute(MONTH) + "-" + date.getAttribute(DAY)));
				if (date.hasAttribute(LAST_PREFIX + DAY) || date.hasAttribute(LAST_PREFIX + MONTH)) {
					date.setAttribute(VALUE_MIN_ATTRIBUTE, (date.getAttribute(YEAR) + "-" + date.getAttribute(MONTH) + "-" + date.getAttribute(DAY)));
					date.setAttribute(VALUE_MAX_ATTRIBUTE, (date.getAttribute(YEAR) + "-" + date.getAttribute((LAST_PREFIX + MONTH), date.getAttribute(MONTH)) + "-" + date.getAttribute((LAST_PREFIX + DAY), date.getAttribute(DAY))));
				}
				dates.add(date);
				
				if (pairs[pi].getStartIndex() != thirdParts[t].getEndIndex())
					date.setAttribute(bridgedDate, data.valueAt(thirdParts[t].getEndIndex()));
				if (DEBUG) System.out.println(" ==> " + date.toXML());
			}
		}
		
		for (int d = 0; d < dates.size(); d++) {
			Annotation date = ((Annotation) dates.get(d));
			String bp = date.getAttribute(bridgedPair, "").toString();
			String bd = date.getAttribute(bridgedDate, "").toString();
			if ((bp.length() != 0) && (bd.length() != 0) && (!bp.equals(bd))) {
				if (DEBUG) System.out.println("Removing date for inconsistent punctuation:");
				if (DEBUG) System.out.println(" " + date.toXML());
				dates.remove(d--);
			}
			else {
				date.removeAttribute(bridgedPair);
				date.removeAttribute(bridgedDate);
			}
		}
		
		Collections.sort(dates);
		return ((Annotation[]) dates.toArray(new Annotation[dates.size()]));
	}
	
	private String getStructure(Annotation date) {
		String dateString = date.getValue();
		StringBuffer dateStructure = new StringBuffer();
		for (int c = 0; c < dateString.length(); c++) {
			char ch = dateString.charAt(c);
			if ((ch >= '0') && (ch <= '9'))
				dateStructure.append('1');
			else if ((ch >= 'a') && (ch <= 'z'))
				dateStructure.append('a');
			else if ((ch >= 'A') && (ch <= 'Z'))
				dateStructure.append('A');
			else if (ch > 32)dateStructure.append(ch);
		}
		return dateStructure.toString();
	}
	
	private String shortenStructrure(String structure) {
		StringBuffer shortStructure = new StringBuffer();
		char lastChar = '\u0000';
		for (int c = 0; c < structure.length(); c++) {
			char ch = structure.charAt(c);
			if (ch != lastChar) {
				shortStructure.append(ch);
				lastChar = ch;
			}
		}
		return shortStructure.toString();
	}
	
	private boolean monthStylesMatch(String month1, String month2) {
		
		//	two Arabic numbers are fine
		if (Gamta.isNumber(month1))
			return Gamta.isNumber(month2);
		
		//	so are two Roman numbers
		else if (Gamta.isRomanNumber(month1))
			return Gamta.isRomanNumber(month2);
		
		//	two words are also fine, unless one is a Roman number
		else return (Gamta.isWord(month2) && !Gamta.isRomanNumber(month2));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/20351_gg2d.xml"), "UTF-8"));
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/21291_gg2g.xml"), "UTF-8"));
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/21282_gg1.xml"), "UTF-8"));
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Testdaten/21330.level2.xml"), "UTF-8"));
		
		//	test for date '31-VIII-1996' ==> overcome tokenization problem '31 - VIII- 1996'
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/8405_temp.xml"), "UTF-8"));
		
		//	test for date ranges
		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("Testing date ranges on July 6th, 2010 ... July 1st - July 5th, 2010 looks fine for now, but how about July 1st - 5th, 2010, which is somewhat different, or 1. - 5. Juli 2010 or 1. Juli - 5. Juli 2010, which is the whole thing in German, or 1. - 5.vii.2010, which is the whole thing in numerical notation, or June - September 2010, a month range."));
//		final MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("Testing simple date range: 29.iii-1.iv.2006."));
		System.out.println("Document loaded");
		
		Analyzer dt = new DateTagger();
		dt.setDataProvider(new AnalyzerDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/AnalyzerData/DateTaggerData/")));
		dt.process(doc, new Properties() {
			public synchronized boolean containsKey(Object key) {
				return Analyzer.INTERACTIVE_PARAMETER.equals(key);
			}
		});
//		
//		AnnotationUtils.writeXML(doc, new PrintWriter(System.out));
		Annotation[] dates = doc.getAnnotations("date");
		for (int d = 0; d < dates.length; d++)
			System.out.println(dates[d].toXML());
	}
}
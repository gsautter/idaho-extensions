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
package de.uka.ipd.idaho.plugins.dateTime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.AnnotationIndex;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTree;
import de.uka.ipd.idaho.gamta.util.AnnotationPatternMatcher.MatchTreeNode;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This class provides utility methods for handling date and time, like
 * generating and parsing timestamps.
 * 
 * @author sautter
 */
public class DateTimeUtils {
	
	private static class UtcDateFormat extends SimpleDateFormat {
		UtcDateFormat(String pattern) {
			super(pattern, Locale.US);
			this.setTimeZone(TimeZone.getTimeZone("UTC")); 
		}
	}
	
	private static final DateFormat YEAR_DATE_FORMAT = new UtcDateFormat("yyyy");
	private static final DateFormat MONTH_DATE_FORMAT = new UtcDateFormat("MM");
	private static final DateFormat DAY_DATE_FORMAT = new UtcDateFormat("dd");
	private static final DateFormat DATE_FORMAT = new UtcDateFormat("yyyy-MM-dd");
	
	private static final DateFormat HTTP_OUT_DATE_FORMAT = new UtcDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'Z");
	private static final DateFormat HTTP_PARSE_DATE_FORMAT = new UtcDateFormat("EEE, dd MMM yyyy HH:mm:ss");
	
	private static final DateFormat LOG_DATE_FORMAT = new UtcDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * Get the current year in the UTC timezone.
	 * @return the current year
	 */
	public static int currentYear() {
		return getYear(System.currentTimeMillis());
	}
	
	/**
	 * Get the current month in the UTC timezone.
	 * @return the current month
	 */
	public static int currentMonth() {
		return getMonth(System.currentTimeMillis());
	}
	
	/**
	 * Get the current day in the UTC timezone.
	 * @return the current day
	 */
	public static int currentDay() {
		return getDay(System.currentTimeMillis());
	}
	
	/**
	 * Get the current date in the UTC timezone, in <code>YYYY-MM-DD</code>
	 * format.
	 * @return the current date
	 */
	public static String currentDate() {
		return getDate(System.currentTimeMillis());
	}
	
	/**
	 * Get the current date and time as an HTTP timestamp.
	 * @return the HTTP timestamp
	 */
	public static String currentHttpTimestamp() {
		return getHttpTimestamp(System.currentTimeMillis());
	}
	
	/**
	 * Get the current date and time as a logging timestamp.
	 * @return the HTTP timestamp
	 */
	public static String currentLogTimestamp() {
		return getLogTimestamp(System.currentTimeMillis());
	}
	
	/**
	 * Extract the year from a UTC timestamp.
	 * @param utc the UTC timestamp to parse
	 * @return the year
	 */
	public static int getYear(long utc) {
		return Integer.parseInt(YEAR_DATE_FORMAT.format(new Date(utc)));
	}
	
	/**
	 * Extract the month from a UTC timestamp.
	 * @param utc the UTC timestamp to parse
	 * @return the month
	 */
	public static int getMonth(long utc) {
		return Integer.parseInt(MONTH_DATE_FORMAT.format(new Date(utc)));
	}
	
	/**
	 * Extract the day from a UTC timestamp.
	 * @param utc the UTC timestamp to parse
	 * @return the day
	 */
	public static int getDay(long utc) {
		return Integer.parseInt(DAY_DATE_FORMAT.format(new Date(utc)));
	}
	
	/**
	 * Get the date for a UTC timestamp, in <code>YYYY-MM-DD</code> format.
	 * @param utc the UTC timestamp to parse
	 * @return the date
	 */
	public static String getDate(long utc) {
		return DATE_FORMAT.format(new Date(utc));
	}
	
	/**
	 * Create an ISO date for a given year, month, and day, in
	 * <code>YYYY-MM-DD</code> format. If the arguments are outside the range
	 * for years, months, or days, respectively, this method returns null.
	 * @param year the year
	 * @param month the month
	 * @param day the day
	 * @return the ISO date
	 */
	public static String getDate(int year, int month, int day) {
		if ((year <= 0) || (9999 < year))
			return null;
		if ((month <= 0) || (12 < month))
			return null;
		if ((day <= 0) || (31 < day))
			return null;
		return ("" + ((year < 1000) ? "0" : "") + ((year < 100) ? "0" : "") + ((year < 10) ? "0" : "") + year + "-" + ((month < 10) ? "0" : "") + month + "-" + ((day < 10) ? "0" : "") + day);
	}
	
	/**
	 * Convert an ISO date in <code>YYYY-MM-DD</code> format into a UTC
	 * timestamp. if the argument string does not match this format, this
	 * method returns -1.
	 * @param date the ISO date to parse
	 * @return the UTC timestamp
	 */
	public static long parseDate(String date) {
		try {
			return DATE_FORMAT.parse(date).getTime();
		}
		catch (ParseException pe) {
			return -1;
		}
	}
	
	/**
	 * Convert a UTC timestamp into an HTTP timestamp.
	 * @param utc the UTC timestamp to convert
	 * @return the HTTP timestamp
	 */
	public static String getHttpTimestamp(long utc) {
		return HTTP_OUT_DATE_FORMAT.format(new Date(utc));
	}
	
	/**
	 * Convert an HTTP timestamp into a UTC timestamp. If the argument string
	 * is not a valid HTTP timestamp, this method returns -1.
	 * @param utc the HTTP timestamp to convert
	 * @return the UTC timestamp
	 */
	public static long parseHttpTimestamp(String http) {
		try {
			return HTTP_PARSE_DATE_FORMAT.parse(http).getTime();
		}
		catch (ParseException e) {
			return -1;
		}
	}
	
	/**
	 * Convert a UTC timestamp into an logging timestamp, formatted as
	 * <code>YYYY-MM-DD hh:mm:ss</code>.
	 * @param utc the UTC timestamp to convert
	 * @return the HTTP timestamp
	 */
	public static String getLogTimestamp(long utc) {
		return LOG_DATE_FORMAT.format(new Date(utc));
	}
	
	/**
	 * Convert an logging timestamp into a UTC timestamp. If the argument
	 * string is not a valid timestamp, this method returns -1.
	 * @param utc the logging timestamp to convert
	 * @return the UTC timestamp
	 */
	public static long parseLogTimestamp(String http) {
		try {
			return LOG_DATE_FORMAT.parse(http).getTime();
		}
		catch (ParseException e) {
			return -1;
		}
	}
	
	/**
	 * Parse a natural language date and convert it into an ISO date. The month
	 * can be an Arabic numeral, textual, an abbreviation, or a Roman numeral,
	 * day and year are expected to be Arabic numbers, with the year given in
	 * all four digits.<br/>
	 * If both month and day are Arabic numerals, and both are in the 1-12
	 * range, this method generally assumes the one closer to the year to be
	 * the month, i.e., day-month-year or year-month-day order.<br/>
	 * If the argument string is null or contains fewer than 3 tokens, this
	 * method returns null.
	 * @param date the date string to parse
	 * @return the UTC timestamp
	 */
	public static String parseTextDate(String date) {
		if (date == null)
			return null;
		date = date.replaceAll("\\p{Punct}", " ");
		
		//	tokenize date, and make sure we have 3 or more tokens
		TokenSequence dateTokens = Gamta.newTokenSequence(date, Gamta.NO_INNER_PUNCTUATION_TOKENIZER);
		if (dateTokens.size() < 3)
			return null;
		
		//	make sure we have the required dictionaries loaded
		ensureDateDictionariesLoaded();
		
		//	get year, month, and day
		int year = -1;
		int yearIndex = -1;
		ArrayList months = new ArrayList(1);
		ArrayList days = new ArrayList(1);
		
		//	evaluate each token for year, month, and day
		for (int t = 0; t < dateTokens.size(); t++) {
			String dateToken = dateTokens.valueAt(t);
			
			//	check year
			if (dateToken.matches("[12][0-9]{3}")) {
				year = Integer.parseInt(dateToken);
				yearIndex = t;
				continue;
			}
			
			//	check month
			for (int m = 0; m < monthDictionaries.length; m++)
				if (monthDictionaries[m].containsIgnoreCase(dateToken)) {
					months.add(new Integer(m+1));
					break;
				}
			
			//	check day
			for (int d = 0; d < dayDictionaries.length; d++)
				if (dayDictionaries[d].containsIgnoreCase(dateToken)) {
					days.add(new Integer(d+1));
					break;
				}
		}
		
		//	do we have all we need?
		if ((year == -1) || months.isEmpty() || days.isEmpty())
			return null;
		
		//	disambiguate day and month based on position of year if required
		if ((months.size() > 1) && (days.size() > 1)) {
			
			//	year towards start, use first number as month
			if (yearIndex < (dateTokens.size() / 2)) {
				while (months.size() > 1)
					months.remove(1);
				days.remove(months.get(0));
				while (days.size() > 1)
					days.remove(1);
			}
			
			//	year towards end, use first number as day
			else {
				while (days.size() > 1)
					days.remove(1);
				months.remove(days.get(0));
				while (months.size() > 1)
					months.remove(1);
			}
		}
		
		//	remove month from possible days
		else if (days.size() > 1) {
			days.remove(months.get(0));
			while (days.size() > 1)
				days.remove(1);
		}
		
		//	remove day from possible months
		else if (months.size() > 1) {
			months.remove(days.get(0));
			while (months.size() > 1)
				months.remove(1);
		}
		
		//	return date
		return getDate(year, ((Integer) months.get(0)).intValue(), ((Integer) days.get(0)).intValue());
	}
	
	/**
	 * Tag the dates in a natural language token sequence. The returned dates
	 * are not attached to the argument token sequence. The returned array is
	 * sorted in nesting order, and duplicate free. However, if the order of
	 * day and month is ambiguous for any match, this method does not make any
	 * attempt at resolving the ambiguity. It is up to client code to determine
	 * how to interpret the matches, which is why the returned annotations do
	 * not bear any <code>value</code> attribute containing their ISO format
	 * interpretation.<br/>
	 * Furthermore, this method only discovers plain dates consisting of a day,
	 * a month, and a year, but no less specific dates (e.g. only consisting
	 * of month and year) or date ranges.<br/>
	 * The matched years are in the range of [0-9999].
	 * @param tokens the token sequence to seek dates in
	 * @return an array holding the date found in the argument token sequence
	 */
	public static Annotation[] getDates(TokenSequence tokens) {
		return getDates(tokens, Integer.MIN_VALUE, Integer.MAX_VALUE, null);
	}
	
	/**
	 * Tag the dates in a natural language token sequence. The returned dates
	 * are not attached to the argument token sequence. The returned array is
	 * sorted in nesting order, and duplicate free. However, if the order of
	 * day and month is ambiguous for any match, this method does not make any
	 * attempt at resolving the ambiguity. It is up to client code to determine
	 * how to interpret the matches, which is why the returned annotations do
	 * not bear any <code>value</code> attribute containing their ISO format
	 * interpretation.<br/>
	 * Furthermore, this method only discovers plain dates consisting of a day,
	 * a month, and a year, but no less specific dates (e.g. only consisting
	 * of month and year) or date ranges.<br/>
	 * The matched years are in the range of [0-9999].
	 * @param tokens the token sequence to seek dates in
	 * @param separators a dictionary holding permitted separators (e.g.
	 *            dashes, slashes, commas, etc.)
	 * @return an array holding the date found in the argument token sequence
	 */
	public static Annotation[] getDates(TokenSequence tokens, Dictionary separators) {
		return getDates(tokens, Integer.MIN_VALUE, Integer.MAX_VALUE, separators);
	}
	
	/**
	 * Tag the dates in a natural language token sequence. The returned dates
	 * are not attached to the argument token sequence. The returned array is
	 * sorted in nesting order, and duplicate free. However, if the order of
	 * day and month is ambiguous for any match, this method does not make any
	 * attempt at resolving the ambiguity. It is up to client code to determine
	 * how to interpret the matches, which is why the returned annotations do
	 * not bear any <code>value</code> attribute containing their ISO format
	 * interpretation.<br/>
	 * Furthermore, this method only discovers plain dates consisting of a day,
	 * a month, and a year, but no less specific dates (e.g. only consisting
	 * of month and year) or date ranges.<br/>
	 * The matched years are in the range of
	 * [<code>Math.max(minYear, 0)</code>-9999].
	 * @param tokens the token sequence to seek dates in
	 * @param minYear the minimum year to accept
	 * @return an array holding the date found in the argument token sequence
	 */
	public static Annotation[] getDates(TokenSequence tokens, int minYear) {
		return getDates(tokens, minYear, Integer.MAX_VALUE, null);
	}
	
	/**
	 * Tag the dates in a natural language token sequence. The returned dates
	 * are not attached to the argument token sequence. The returned array is
	 * sorted in nesting order, and duplicate free. However, if the order of
	 * day and month is ambiguous for any match, this method does not make any
	 * attempt at resolving the ambiguity. It is up to client code to determine
	 * how to interpret the matches, which is why the returned annotations do
	 * not bear any <code>value</code> attribute containing their ISO format
	 * interpretation.<br/>
	 * Furthermore, this method only discovers plain dates consisting of a day,
	 * a month, and a year, but no less specific dates (e.g. only consisting
	 * of month and year) or date ranges.<br/>
	 * The matched years are in the range of
	 * [<code>Math.max(minYear, 0)</code>-9999].
	 * @param tokens the token sequence to seek dates in
	 * @param minYear the minimum year to accept
	 * @param separators a dictionary holding permitted separators (e.g.
	 *            dashes, slashes, commas, etc.)
	 * @return an array holding the date found in the argument token sequence
	 */
	public static Annotation[] getDates(TokenSequence tokens, int minYear, Dictionary separators) {
		return getDates(tokens, minYear, Integer.MAX_VALUE, separators);
	}
	
	/**
	 * Tag the dates in a natural language token sequence. The returned dates
	 * are not attached to the argument token sequence. The returned array is
	 * sorted in nesting order, and duplicate free. However, if the order of
	 * day and month is ambiguous for any match, this method does not make any
	 * attempt at resolving the ambiguity. It is up to client code to determine
	 * how to interpret the matches, which is why the returned annotations do
	 * not bear any <code>value</code> attribute containing their ISO format
	 * interpretation.<br/>
	 * Furthermore, this method only discovers plain dates consisting of a day,
	 * a month, and a year, but no less specific dates (e.g. only consisting
	 * of month and year) or date ranges.
	 * The matched years are in the range of
	 * [<code>Math.max(minYear, 0)</code>-<code>Math.min(maxYear, 9999)</code>].
	 * @param tokens the token sequence to seek dates in
	 * @param minYear the minimum year to accept
	 * @param maxYear the maximum year to accept
	 * @return an array holding the date found in the argument token sequence
	 */
	public static Annotation[] getDates(TokenSequence tokens, int minYear, int maxYear) {
		return getDates(tokens, minYear, maxYear, null);
	}
	
	/**
	 * Tag the dates in a natural language token sequence. The returned dates
	 * are not attached to the argument token sequence. The returned array is
	 * sorted in nesting order, and duplicate free. However, if the order of
	 * day and month is ambiguous for any match, this method does not make any
	 * attempt at resolving the ambiguity. It is up to client code to determine
	 * how to interpret the matches, which is why the returned annotations do
	 * not bear any <code>value</code> attribute containing their ISO format
	 * interpretation.<br/>
	 * Furthermore, this method only discovers plain dates consisting of a day,
	 * a month, and a year, but no less specific dates (e.g. only consisting
	 * of month and year) or date ranges.
	 * The matched years are in the range of
	 * [<code>Math.max(minYear, 0)</code>-<code>Math.min(maxYear, 9999)</code>].
	 * @param tokens the token sequence to seek dates in
	 * @param minYear the minimum year to accept
	 * @param maxYear the maximum year to accept
	 * @param separators a dictionary holding permitted separators (e.g.
	 *            dashes, slashes, commas, etc.)
	 * @return an array holding the date found in the argument token sequence
	 */
	public static Annotation[] getDates(TokenSequence tokens, int minYear, int maxYear, Dictionary separators) {
		
		//	make sure we have the dictionaries loaded
		ensureDateDictionariesLoaded();
		
		//	get and index date components
		AnnotationIndex datePartIndex = new AnnotationIndex();
		Annotation[] days = Gamta.extractAllContained(tokens, allDaysDictionary, false);
		datePartIndex.addAnnotations(days, "day");
		Annotation[] months = Gamta.extractAllContained(tokens, allMonthsDictionary, false);
		datePartIndex.addAnnotations(months, "month");
		Annotation[] years = getYears(tokens, minYear, maxYear);
		datePartIndex.addAnnotations(years, "year");
		
		//	tag dates (independent of separators dictionary, dots are covered by built-in dictionaries)
		ArrayList dateList = new ArrayList();
		HashSet dateSet = new HashSet();
		addDates(tokens, datePartIndex, "<day> <month> <year>", false, dateList, dateSet);
		addDates(tokens, datePartIndex, "<month> <day> <year>", false, dateList, dateSet);
		if ((separators != null) && separators.lookup(","))
			addDates(tokens, datePartIndex, "<month> <day> ',' <year>", false, dateList, dateSet);
		if ((separators != null) && separators.lookup("-")) {
			addDates(tokens, datePartIndex, "<year> '-' <month> '-' <day>", false, dateList, dateSet);
			addDates(tokens, datePartIndex, "<day> '-' <month> '-' <year>", false, dateList, dateSet);
		}
		if ((separators != null) && separators.lookup("/")) {
			addDates(tokens, datePartIndex, "<year> '/' <month> '/' <day>", false, dateList, dateSet);
			addDates(tokens, datePartIndex, "<day> '/' <month> '/' <year>", false, dateList, dateSet);
		}
		
		//	tag custom separator dates (if any), insisting on two equal separators
		if (separators != null) {
			
			//	count default separators
			int defSeparatorCount = 0;
			if (separators.lookup(","))
				defSeparatorCount++;
			if (separators.lookup("-"))
				defSeparatorCount++;
			if (separators.lookup("/"))
				defSeparatorCount++;
			
			//	mark special dates if we have a non-default separator
			if (defSeparatorCount < separators.size()) {
				Annotation[] seps = Gamta.extractAllContained(tokens, separators);
				datePartIndex.addAnnotations(seps, "separator");
				addDates(tokens, datePartIndex, "<day> <separator> <month> <separator> <year>", true, dateList, dateSet);
				addDates(tokens, datePartIndex, "<month> <separator> <day> <separator> <year>", true, dateList, dateSet);
				addDates(tokens, datePartIndex, "<year> <separator> <month> <separator> <day>", true, dateList, dateSet);
				addDates(tokens, datePartIndex, "<year> <separator> <day> <separator> <month>", true, dateList, dateSet);
			}
		}
		
		//	finally ...
		Collections.sort(dateList, AnnotationUtils.ANNOTATION_NESTING_ORDER);
		return ((Annotation[]) dateList.toArray(new Annotation[dateList.size()]));
	}
	
	private static void addDates(TokenSequence tokens, AnnotationIndex datePartIndex, String datePattern, boolean enforceEqualSeparators, ArrayList dateList, HashSet dateSet) {
		//	TODO use match attributes instead of separators
		MatchTree[] dateMatches = AnnotationPatternMatcher.getMatchTrees(tokens, datePartIndex, datePattern);
		for (int d = 0; d < dateMatches.length; d++) {
			if ((!enforceEqualSeparators || hasEqualSeparators(dateMatches[d])) && dateSet.add(dateMatches[d].getMatch().getValue()))
				dateList.add(dateMatches[d].getMatch());
		}
	}
	
	private static boolean hasEqualSeparators(MatchTree date) {
		HashSet separators = new HashSet();
		collectSeparators(date.getChildren(), separators);
		return (separators.size() == 1);
	}
	
	private static void collectSeparators(MatchTreeNode[] mtns, HashSet separators) {
		if (mtns == null)
			return;
		for (int n = 0; n < mtns.length; n++) {
			if (mtns[n].getPattern().equals("<separator>"))
				separators.add(mtns[n].getMatch().getValue());
			else if (!mtns[n].getPattern().startsWith("<")) {}
				collectSeparators(mtns[n].getChildren(), separators);
		}
	}
	
	private static Annotation[] getYears(TokenSequence tokens, int minYear, int maxYear) {
		ArrayList yearList = new ArrayList();
		Annotation[] years = Gamta.extractAllMatches(tokens, "[1-9][0-9]{0,3}");
		for (int y = 0; y < years.length; y++) {
			int year = Integer.parseInt(years[y].firstValue());
			if ((minYear <= year) && (year <= maxYear))
				yearList.add(years[y]);
		}
		return ((Annotation[]) yearList.toArray(new Annotation[yearList.size()]));
	}
	
	private static StringVector[] monthDictionaries = null;
	private static StringVector allMonthsDictionary = null;
	private static StringVector[] dayDictionaries = null;
	private static StringVector allDaysDictionary = null;
	private static synchronized void ensureDateDictionariesLoaded() {
		if (monthDictionaries == null)
			monthDictionaries = getMonthDictionaries();
		if (allMonthsDictionary == null)
			allMonthsDictionary = aggregateDictionaries(monthDictionaries);
		if (dayDictionaries == null)
			dayDictionaries = getDayDictionaries();
		if (allDaysDictionary == null)
			allDaysDictionary = aggregateDictionaries(dayDictionaries);
	}
	private static StringVector aggregateDictionaries(StringVector[] dicts) {
		StringVector dict = new StringVector();
		for (int d = 0; d < dicts.length; d++)
			dict.addContent(dicts[d]);
		return dict;
	}
	
	/**
	 * Representation of the minimum and maximum UTC timestamps falling within
	 * the time span given by a date.
	 * 
	 * @author sautter
	 */
	public static class UtcRange {
		
		/** the minimum UTC timetamp falling within the verbatim string date */
		public final long utcMin;
		
		/** the maximum UTC timetamp falling within the verbatim string date */
		public final long utcMax;
		
		/** the verbatim string date */
		public final String dateString;
		
		UtcRange(long utcMin, long utcMax, String dateString) {
			this.utcMin = utcMin;
			this.utcMax = utcMax;
			this.dateString = dateString;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return (this.dateString + ": [" + this.utcMin + "," + this.utcMax + "]");
		}
	}
	
	/**
	 * Get the UTC range for a date given as a string. If the argument string
	 * cannot be parsed into year, month, and day, this method returns null.
	 * Separators my be dots, slashes, dashes, or spaces. The argument string
	 * can also be only a year, or a year and month. However, the numbers have
	 * to be in the order of year, month, and day.
	 * @param year the date to parse
	 * @return the UTC range for the date
	 */
	public static UtcRange getUtcRange(String date) {
		String[] dateParts = date.split("[\\.\\/\\-\\s]");
		if (dateParts.length == 0)
			return null;
		if (dateParts.length == 1) try {
			return getUtcRange(Integer.parseInt(dateParts[0].trim()), date);
		}
		catch (NumberFormatException nfe) {
			return null;
		}
		if (dateParts.length == 2) try {
			return getUtcRange(Integer.parseInt(dateParts[0].trim()), Integer.parseInt(dateParts[1].trim()), date);
		}
		catch (NumberFormatException nfe) {
			return null;
		}
		try {
			return getUtcRange(Integer.parseInt(dateParts[0].trim()), Integer.parseInt(dateParts[1].trim()), Integer.parseInt(dateParts[2].trim()), date);
		}
		catch (NumberFormatException nfe) {
			return null;
		}
	}
	
	/**
	 * Get the UTC range for a year.
	 * @param year the year
	 * @return the UTC range for the year
	 */
	public static UtcRange getUtcRange(int year) {
		return getUtcRange(year, ("" + year));
	}
	private static UtcRange getUtcRange(int year, String date) {
		long min = parseLogTimestamp(year + "-01-01 00:00:00");
		if (min == -1)
			return null;
		int nextYear = (year+1);
		long max = parseLogTimestamp(nextYear + "-01-01 00:00:00") - 1;
		if (max == -1)
			return null;
		return new UtcRange(min, max, date);
	}
	
	/**
	 * Get the UTC range for a month in a year. If the month is invalid, this
	 * method returns null.
	 * @param year the year
	 * @param month the month
	 * @return the UTC range for the month
	 */
	public static UtcRange getUtcRange(int year, int month) {
		return getUtcRange(year, month, ("" + year + "-" + ((month < 10) ? "0" : "") + month));
	}
	private static UtcRange getUtcRange(int year, int month, String date) {
		if ((month < 1) || (12 < month))
			return null;
		long min = parseLogTimestamp(year + "-" + ((month < 10) ? "0" : "") + month + "-01 00:00:00");
		if (min == -1)
			return null;
		int nextYear = year;
		int nextMonth = (month+1);
		if (12 < nextMonth) {
			nextMonth -= 12;
			nextYear++;
		}
		long max = parseLogTimestamp(nextYear + "-" + ((nextMonth < 10) ? "0" : "") + nextMonth + "-01 00:00:00") - 1;
		if (max == -1)
			return null;
		return new UtcRange(min, max, date);
	}
	
	/**
	 * Get the UTC range for a day in a month in a year. If the month or the
	 * day is invalid, this method returns null.
	 * @param year the year
	 * @param month the month
	 * @param day the day
	 * @return the UTC range for the day
	 */
	public static UtcRange getUtcRange(int year, int month, int day) {
		return getUtcRange(year, month, day, getDate(year, month, day));
	}
	private static UtcRange getUtcRange(int year, int month, int day, String date) {
		long min = parseLogTimestamp(getDate(year, month, day) + " 00:00:00");
		if (min == -1)
			return null;
		long max = (min + (1000 * 60 * 60 * 24) - 1); // days have a fixed length, as opposed to months and years
		return new UtcRange(min, max, date);
	}
	
	/**
	 * Obtain an array of dictionaries containing possible notations for each
	 * day in a month. The first element of each dictionary is the normalized
	 * two-digit representation.
	 * @return an array holding the dictionaries
	 */
	public static StringVector[] getDayDictionaries() {
		StringVector[] dds = new StringVector[dayDictionariyEntries.length];
		for (int d = 0; d < dds.length; d++) {
			dds[d] = new StringVector();
			dds[d].parseAndAddElements(dayDictionariyEntries[d], ";");
		}
		return dds;
	}
	private static String[] dayDictionariyEntries = {
		"01;1;1 st;01 st;1st;01st;01.;1.",
		"02;2;2 nd;02 nd;2nd;02nd;02.;2.",
		"03;3;3 rd;03 rd;3rd;03rd;03.;3.",
		"04;4;4 th;04 th;4th;04th;04.;4.",
		"05;5;5 th;05 th;5th;05th;05.;5.",
		"06;6;6 th;06 th;6th;06th;06.;6.",
		"07;7;7 th;07 th;7th;07th;07.;7.",
		"08;8;8 th;08 th;8th;08th;08.;8.",
		"09;9;9 th;09 th;9th;09th;09.;9.",
		"10;10;10 th;10 th;10th;10th;10.;10.",
		"11;11;11 th;11 th;11th;11th;11.;11.",
		"12;12;12 th;12 th;12th;12th;12.;12.",
		"13;13;13 th;13 th;13th;13th;13.;13.",
		"14;14;14 th;14 th;14th;14th;14.;14.",
		"15;15;15 th;15 th;15th;15th;15.;15.",
		"16;16;16 th;16 th;16th;16th;16.;16.",
		"17;17;17 th;17 th;17th;17th;17.;17.",
		"18;18;18 th;18 th;18th;18th;18.;18.",
		"19;19;19 th;19 th;19th;19th;19.;19.",
		"20;20;20 th;20 th;20th;20th;20.;20.",
		"21;21;21 st;21 st;21st;21st;21.;21.",
		"22;22;22 nd;22 nd;22nd;22nd;22.;22.",
		"23;23;23 rd;23 rd;23rd;23rd;23.;23.",
		"24;24;24 th;24 th;24th;24th;24.;24.",
		"25;25;25 th;25 th;25th;25th;25.;25.",
		"26;26;26 th;26 th;26th;26th;26.;26.",
		"27;27;27 th;27 th;27th;27th;27.;27.",
		"28;28;28 th;28 th;28th;28th;28.;28.",
		"29;29;29 th;29 th;29th;29th;29.;29.",
		"30;30;30 th;30 th;30th;30th;30.;30.",
		"31;31;31 st;31 st;31st;31st;31.;31."
	};
	
	/**
	 * Obtain an array of dictionaries containing possible notations for each
	 * month in a year. The first element of each dictionary is the normalized
	 * two-digit representation.
	 * @return an array holding the dictionaries
	 */
	public static StringVector[] getMonthDictionaries() {
		StringVector[] mds = new StringVector[monthDictionariyEntries.length];
		for (int d = 0; d < mds.length; d++) {
			mds[d] = new StringVector();
			mds[d].parseAndAddElements(monthDictionariyEntries[d], ";");
		}
		return mds;
	}
	private static String[] monthDictionariyEntries = {
		"01;1;01.;1.;i;i.;January;Jan;Jan.;Janvier;Jan;Jan.;Enero;Ene;Ene.;Gennaio;Gen;Gen.;Januar;Jan;Jan.",
		"02;2;02.;2.;ii;ii.;February;Feb;Feb.;Fevrier;Fev;Fev.;Febrero;Feb;Feb.;Febbraio;Feb;Feb.;Februar;Feb;Feb.",
		"03;3;03.;3.;iii;iii.;March;Mar;Mar.;Mars;Mar;Mar.;Marzo;Mar;Mar.;Marzo;Mar;Mar.;Maerz;Mae;Mae.",
		"04;4;04.;4.;iiii;iiii.;iv;iv.;April;Apr;Apr.;Avril;Avr;Avr.;Abril;Abr;Abr.;Aprile;Apr;Apr.;April;Apr;Apr.",
		"05;5;05.;5.;v;v.;May;;;Mai;;;Mayo;May;May.;Maggio;Mag;Mag.;Mai",
		"06;6;06.;6.;vi;vi.;June;Jun;Jun.;Juin;Jui;Jui.;Junio;Jun;Jun.;Giugno;Giu;Giu.;Juni;Jun;Jun.",
		"07;7;07.;7.;vii;vii.;July;Jul;Jul.;Juillet;Jui;Jui.;Julio;Jul;Jul.;Lùglio;Lùg;Lùg.;Juli;Jul;Jul.",
		"08;8;08.;8.;viii;viii.;August;Aug;Aug.;Aout;Aou;Aou.;Agosto;Ago;Ago.;Agosto;Ago;Ago.;August;Aug;Aug.",
		"09;9;09.;9.;viiii;viiii.;ix;ix.;September;Sep;Sep.;Sept;Sept.;Septembre;Sep;Sep.;Septiembre;Sep;Sep.;Settembre;Set;Set.;September;Sep;Sep.",
		"10;10;10.;10.;x;x.;October;Oct;Oct.;Octobre;Oct;Oct.;Octubre;Oct;Oct.;Ottagono;Ott;Ott.;Oktober;Okt;Okt.",
		"11;11;11.;11.;xi;xi.;November;Nov;Nov.;Novembre;Nov;Nov.;Noviembre;Nov;Nov.;Novembre;Nov;Nov.;November;Nov;Nov.",
		"12;12;12.;12.;xii;xii.;December;Dec;Dec.;Decembre;Dec;Dec.;Diciembre;Dic;Dic.;Dicembre;Dic;Dic.;Dezember;Dez;Dez."
	};
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//	tests for basic parsing and formatting
		System.out.println(currentYear());
		System.out.println(currentMonth());
		System.out.println(currentDay());
		System.out.println(currentLogTimestamp());
		System.out.println(currentHttpTimestamp());
		System.out.println(currentDate());
		System.out.println(parseHttpTimestamp("Fri, 10 Jan 2014 00:10:47 GMT+0000"));
		System.out.println(parseHttpTimestamp("Fri, 10 Jan 2014 00:10:47 GMT"));
		System.out.println(parseHttpTimestamp("Fri, 10 Jan 2014 00:10:47 +0000"));
		System.out.println(parseHttpTimestamp("Fri, 10 Jan 2014 00:10:47 CET"));
		System.out.println(parseHttpTimestamp("Fri, 10 Jan 2014 00:10:47 +0100"));
		System.out.println(parseHttpTimestamp("Fri, 10 Jan 1014 00:10:47 +0100"));
		System.out.println(parseLogTimestamp("2013-12-31 12:34:56"));
		
		//	tests for UTC ranges
		System.out.println(getUtcRange("2013-12-31 12:34:56"));
		System.out.println(getUtcRange("2013/12/31"));
		System.out.println(getUtcRange("2013 12"));
		System.out.println(getUtcRange("2013-12-31"));
		System.out.println(getUtcRange("2013-12"));
		System.out.println(getUtcRange("2013"));
		System.out.println(getUtcRange(2013, 12, 30));
		
		//	tests for text date parsing
		System.out.println(parseTextDate("2013/12/31"));
		System.out.println(parseTextDate("2013/5/10"));
		System.out.println(parseTextDate("5/10/2013"));
		System.out.println(parseTextDate("05AUG2017"));
		System.out.println(parseTextDate("5th Aug. 2017"));
	}
}
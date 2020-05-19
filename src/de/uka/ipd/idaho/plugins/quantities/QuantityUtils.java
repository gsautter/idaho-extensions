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
package de.uka.ipd.idaho.plugins.quantities;

import java.util.TreeMap;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.util.constants.NamedEntityConstants;

/**
 * This class provides utility methods for handling quantities like lengths,
 * areas, volumes, etc. The built-in conversion dictionary covers metric and
 * imperial units.
 * 
 * @author sautter
 */
public class QuantityUtils implements NamedEntityConstants {
	
	/**
	 * Representation of a unit, including figures for conversion into the
	 * corresponding metric unit.<br>
	 * For miles, for instance, the symbol is 'mi', the metric unit is 'm', the
	 * conversion factor is '1.609344', and the conversion orders of magnitude
	 * is '3'. This is because 1 mi = 1.609344 * 10³ m.<br>
	 * In general, the conversion factor is always in the 1-10 range, and the
	 * orders of magnitude adjusted accordingly.
	 * 
	 * @author sautter
	 */
	public static class Unit {
		
		/** the standard symbol for the unit */
		public final String symbol;
		
		/** the standard symbol of the corresponding metric/SI unit */
		public final String metricUnit;
		
		/** the conversion factor between quantities given in this unit to the corresponding metric/SI unit */
		public final double factorToMetric;
		
		/** the orders of magnitude between quantities given in this unit to the corresponding metric/SI unit */
		public final int magnitudesToMetric;
		
		Unit(String symbol, String metricUnit, double factorToMetric, int magnitudesToMetric) {
			this.symbol = symbol;
			this.metricUnit = metricUnit;
			this.factorToMetric = factorToMetric;
			this.magnitudesToMetric = magnitudesToMetric;
		}
	}
	
	/**
	 * Specialized unit for temperature, figuring in shifting the point of
	 * reference, in the source unit.
	 * 
	 * @author sautter
	 */
	public static class TemperatureUnit extends Unit {
		
		/** the number to add to quantities in this unit before multiplying in the conversion factors to the corresponding metric/SI unit */
		public final double shiftToMetric;

		TemperatureUnit(String symbol, double factorToMetric, double shiftToMetric) {
			super(symbol, "K", factorToMetric, 0);
			this.shiftToMetric = shiftToMetric;
		}
	}
	
	private static TreeMap unitsDefault = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	static {
		addDefaultUnit("ng", "nanogram, nanograms, nanogramme, nanogrammes, nanogramm", "kg", 1, -12);
		addDefaultUnit("µg", "yg, microgram, micrograms, microgramme, microgrammes, mikrogramm", "kg", 1, -9);
		addDefaultUnit("mg", "milligram, milligrams, milligramme, milligrammes, milligramm", "kg", 1, -6);
		addDefaultUnit("g", "gram, grams, gramme, grammes, gramm", "kg", 1, -3);
		addDefaultUnit("kg", "kilogram, kilograms, kilogramme, kilogrammes, kilogramm", null, 1, 0);
		addDefaultUnit("t", "ton, tons, tonne, tonnes, metric ton, metric tons, metric tonne, metric tonnes", "kg", 1, 3);
		addDefaultUnit("oz", "ounce, ounces", "kg", 28.3495231, -3);
		addDefaultUnit("lb", "lbs, pound, pounds", "kg", 4.5359237, -1);
		addDefaultUnit("CD", "carat, carats", "kg", 5, -4);
		
		addDefaultUnit("nm", "nanometer, nanometers, nanometre, nanometres", "m", 1, -9);
		addDefaultUnit("µm", "ym, micron, micrometer, micrometers, mikrometer, micrometre, micrometres, mikrometer", "m", 1, -6);
		addDefaultUnit("mm", "millimeter, millimeters, millimetre, millimetres", "m", 1, -3);
		addDefaultUnit("cm", "centimeter, centimeters, centimetre, centimetres, zentimeter", "m", 1, -2);
		addDefaultUnit("m", "meter, meters, metre, metres", null, 1, 0);
		addDefaultUnit("km", "kilometer, kilometers, kilometre, kilometres", "m", 1, 3);
		addDefaultUnit("mi", "mile, miles", "m", 1.609344, 3);
		addDefaultUnit("yd", "yds, yard, yards", "m", 9.144, -1);
		addDefaultUnit("ft", "foot, feet, fuss", "m", 3.048, -1);
		addDefaultUnit("in", "inch, inches, zoll", "m", 2.54, -2);
		
		addDefaultUnit("mm²", "mm ², mm2, mm 2, sqmm, sq mm, squaremillimeter, squaremillimeters, square millimeter, square millimeters, squaremillimetre, squaremillimetres, square millimetre, square millimetres, quadratmillimeter", "m²", 1, -6);
		addDefaultUnit("cm²", "cm ², cm2, cm 2, sqcm, sq cm, squarecentimeter, squarecentimeters, square centimeter, square centimeters, squarecentimetre, squarecentimetres, square centimetre, square centimetres, quadratcentimeter, quadratzentimeter", "m²", 1, -4);
		addDefaultUnit("m²", "m ², m2, m 2, sqm, sq m, squaremeter, squaremeters, square meter, square meters, squaremetre, squaremetres, square metre, square metres, qm, quadratmeter", null, 1, 0);
		addDefaultUnit("a", "ar", "m²", 1, 2);
		addDefaultUnit("ha", "hectare, hectares, hektar", "m²", 1, 4);
		addDefaultUnit("km²", "km ², km2, km 2, sqkm, sq km, squarekilometer, squarekilometers, square kilometer, square kilometers, squarekilometre, squarekilometres, square kilometre, square kilometres, quadratkilometer", "m²", 1, 6);
		addDefaultUnit("mi²", "mi ², mi2, mi 2, sqmi, sq mi, squaremile, squaremiles, square mile, square miles", "m²", 2.589988, 6);
		addDefaultUnit("ac", "acre, acres", "m²", 4.0468564224, 3);
		addDefaultUnit("yd²", "yd ², yds², yds ², yd2, yd 2, yds2, yds 2, sqyd, sq yd, sqyds, sq yds, squareyard, squareyards, square yard, square yards", "m²", 8.3612736, -1);
		addDefaultUnit("ft²", "ft ², ft2, ft 2, sqft, sq ft, squarefoot, squarefeet, square foot, square feet", "m²", 9.290304, -2);
		addDefaultUnit("in²", "in ², in2, in 2, sqin, sq in, squareinch, squareinches, square inch, square inches", "m²", 6.4516, -4);
		
		addDefaultUnit("mm³", "mm ³, mm3, mm 3, cmm, cubicmillimeter, cubicmillimeters, cubic millimeter, cubic millimeters, cubicmillimetre, cubicmillimetres, cubic millimetre, cubic millimetres, kubukmillimeter", "m³", 1, -9);
		addDefaultUnit("cm³", "cm ³, cm3, cm 3, ccm, cubiccentimeter, cubiccentimeters, cubic centimeter, cubic centimeters, cubiccentimetre, cubiccentimetres, cubic centimetre, cubic centimetres, ccm, kubikcentimeter, kubikzentimeter", "m³", 1, -6);
		addDefaultUnit("l", "litre, litres, liter, liters", "m³", 1, -3);
		addDefaultUnit("m³", "m ³, m3, m 3, cubicmeter, cubicmeters, cubic meter, cubic meters, cubicmetre, cubicmetres, cubic metre, cubic metres, kubikmeter", null, 1, 0);
		addDefaultUnit("km³", "km ³, km3, km 3, cubickilometer, cubickilometers, cubic kilometer, cubic kilometers, cubickilometre, cubickilometres, cubic kilometre, cubic kilometres, kubikkilometer", "m³", 1, 9);
		addDefaultUnit("mi³", "mi ³, mi3, mi 3, cubicmile, cubicmiles, cubic mile, cubic miles", "m³", 4.168181825, 9);
		addDefaultUnit("yd³", "yd ³, yd3, yd 3, yds³, yds ³, yds3, yds 3, cu yd, cu yds, yard³, yard ³, yard3, yard 3, yards³, yards ³, yard3, yards 3, cubicyard, cubicyards, cubic yard, cubic yards", "m³", 7.64554857984, -1);
		addDefaultUnit("ft³", "ft ³, ft3, ft 3, cu ft, foot³, foot ³, foot3, foot 3, feet³, feet ³, feet3, feet 3, cubicfoot, cubicfeet, cubic foot, cubic feet", "m³", 2.83168, -2);
		addDefaultUnit("gal", "gals, gallon, gallons", "m³", 3.785411784, -3);
		addDefaultUnit("in³", "in ³, in3, in 3, cu in, inch³, inch ³, inch3, inch 3, cubicinch, cubicinches, cubic inch, cubic inches", "m³", 1.6387064, -5);
		
		addDefaultTemperatureUnit("K", "Kelvin", 1, 0);
		addDefaultTemperatureUnit("°C", "° C, degrees Celsius, ° Celsius, °Celsius", 1, 273.15);
		addDefaultTemperatureUnit("°F", "° F, degrees Farenheit, ° Farenheit, °Farenheit", 0.5555556, 459.67);
		addDefaultTemperatureUnit("°R", "° R, degrees Rankine, ° Rankine, °Rankine", 0.5555556, 0);
		addDefaultTemperatureUnit("°N", "° N, degrees Newton, ° Newton, °Newton", 3.03030303, 90.1395);
		addDefaultTemperatureUnit("°Re", "° Re, degrees Reaumur, ° Reaumur, °Reaumur", 1.25, 218.52);
	}
	private static void addDefaultUnit(String symbol, String synonymString, String metricUnit, double factorToMetric, int magnitudesToMetric) {
		
		//	parse synonym list
		String[] synonyms = ((synonymString == null) ? null : synonymString.split("\\s*\\,\\s*"));
		
		//	do we have a base unit?
		if (metricUnit == null) {
			metricUnit = symbol;
			factorToMetric = 1.0;
			magnitudesToMetric = 0;
		}
		
		//	adjust conversion factors
		while (factorToMetric < 1) {
			factorToMetric *= 10;
			magnitudesToMetric--;
		}
		while (10 <= factorToMetric) {
			factorToMetric /= 10;
			magnitudesToMetric++;
		}
		
		//	create unit
		Unit unit = new Unit(symbol, metricUnit, factorToMetric, magnitudesToMetric);
		
		//	register unit
		unitsDefault.put(symbol, unit);
		if (synonyms != null)
			for (int s = 0; s < synonyms.length; s++) {
				String synonym = synonyms[s].trim();
				if (synonym.length() != 0)
					unitsDefault.put(synonym, unit);
			}
	}
	private static void addDefaultTemperatureUnit(String symbol, String synonymString, double factorToMetric, double shiftToMetric) {
		
		//	parse synonym list
		String[] synonyms = ((synonymString == null) ? null : synonymString.split("\\s*\\,\\s*"));
		
		//	create unit
		Unit unit = new TemperatureUnit(symbol, factorToMetric, shiftToMetric);
		
		//	register unit
		unitsDefault.put(symbol, unit);
		if (synonyms != null)
			for (int s = 0; s < synonyms.length; s++) {
				String synonym = synonyms[s].trim();
				if (synonym.length() != 0)
					unitsDefault.put(synonym, unit);
			}
	}
	private static TreeMap unitsCustom = new TreeMap(String.CASE_INSENSITIVE_ORDER); 
	
	/**
	 * Retrieve a unit by its name. The lookup is case insensitive.
	 * @param name the unit name
	 * @return the unit with the argument name
	 */
	public static Unit getUnit(String name) {
		Unit unit = ((Unit) unitsCustom.get(name));
		if (unit == null)
			unit = ((Unit) unitsDefault.get(name));
		return unit;
	}
	
	/**
	 * Add a custom unit. The synonyms may be null. If the argument metric unit
	 * id null, the unit is assumed to <em>be</em> a metric base unit, the name
	 * is used instead, and the conversion factors are set to 1.0 and 0,
	 * respectively. Otherwise, the metric conversion factor must be positive
	 * non-zero. It is automatically normalized to the 1-10 range, adjusting
	 * the orders of magnitude accordingly. The returned boolean indicates
	 * whether or not the unit has been accepted.
	 * @param symbol the standard of the unit
	 * @param synonyms synonyms of the unit, spelled-out names, etc.
	 * @param metricUnit the symbol of the corresponding metric unit
	 * @param factorToMetric the factor for conversion to the metric unit
	 * @param magnitudesToMetric the orders of magnitude to add on metric conversion
	 * @return
	 */
	public static boolean addUnit(String symbol, String[] synonyms, String metricUnit, double factorToMetric, int magnitudesToMetric) {
		
		//	check arguments
		if ((symbol == null) || (symbol.trim().length() == 0) || (factorToMetric <= 0))
			return false;
		
		//	do we have a base unit?
		if (metricUnit == null) {
			metricUnit = symbol;
			factorToMetric = 1.0;
			magnitudesToMetric = 0;
		}
		
		//	adjust conversion factors
		while (factorToMetric < 1) {
			factorToMetric *= 10;
			magnitudesToMetric--;
		}
		while (10 <= factorToMetric) {
			factorToMetric /= 10;
			magnitudesToMetric++;
		}
		
		//	create unit
		Unit unit = new Unit(symbol, metricUnit, factorToMetric, magnitudesToMetric);
		
		//	register unit
		unitsCustom.put(symbol, unit);
		if (synonyms != null)
			for (int s = 0; s < synonyms.length; s++) {
				String synonym = synonyms[s].trim();
				if (synonym.length() != 0)
					unitsCustom.put(synonym, unit);
			}
		
		//	indicate success
		return true;
	}
	
	/**
	 * Get the metric value of a quantity. The value is computed from the
	 * <code>metricValue</code> and <code>metricMagnitude</code> attributes of
	 * the argument annotation. This method expects the former attribute to be
	 * a double and the latter to be an integer. If these criteria are not met
	 * or either attribute is missing, this method throws a
	 * <code>NumberFormatException</code>.
	 * @param quantity the quantity whose metric value to compute
	 * @return the metric value
	 * @throws NumberFormatException
	 */
	public static double getMetricValue(Annotation quantity) throws NumberFormatException {
		
		//	get metric value ...
		double metricValue;
		try {
			metricValue = Double.parseDouble((String) quantity.getAttribute(METRIC_VALUE_ATTRIBUTE));
		}
		catch (NullPointerException npe) {
			throw new NumberFormatException("Could not compute metric value, metric value attribute missing.");
		}
		
		//	... and magnitude
		int metricMagnitude;
		try {
			metricMagnitude = Integer.parseInt((String) quantity.getAttribute(METRIC_MAGNITUDE_ATTRIBUTE));
		}
		catch (NullPointerException npe) {
			throw new NumberFormatException("Could not compute metric value, metric magnitude attribute missing.");
		}
		
		//	multiply magnitude into value
		while (metricMagnitude > 0) {
			metricValue *= 10;
			metricMagnitude--;
		}
		while (metricMagnitude < 0) {
			metricValue /= 10;
			metricMagnitude++;
		}
		
		//	finally ...
		return metricValue;
	}
	
	/**
	 * Convert a quantity to its base metric/SI equivalent. In particular, the
	 * unit the returned value is given in is as follows:<ul>
	 * <li>length: meters</li>
	 * <li>area: square meters</li>
	 * <li>volume: cubic meters</li>
	 * <li>weight: kilograms</li>
	 * <li>temperature: Kelvin</li>
	 * </ul>
	 * If the argument unit is unknown, the argument value is returned as is.
	 * @param value the value to convert
	 * @param unit the unit the value is given in
	 * @return the equivalent metric/SI value
	 */
	public static double getMetricValue(double value, String unit) {
		
		//	get unit
		Unit u = getUnit(unit);
		if (unit == null)
			return value;
		double metricValue = value;
		
		//	handle temperatures ...
		if (u instanceof TemperatureUnit) {
			metricValue += ((TemperatureUnit) u).shiftToMetric;
			metricValue *= u.factorToMetric;
		}
		
		//	... and other quantities
		else {
			metricValue *= u.factorToMetric;
			int metricMagnitude = u.magnitudesToMetric;
			while (metricMagnitude > 0) {
				metricValue *= 10;
				metricMagnitude--;
			}
			while (metricMagnitude < 0) {
				metricValue /= 10;
				metricMagnitude++;
			}
		}
		
		//	finally ...
		return metricValue;
	}
	
	/**
	 * Convert a double to a string, accurate to a specific number of decimal
	 * digits. In particular, this method rounds the argument value to the
	 * specified number of decimal digits, and then truncates any tailing zeros
	 * and, if no decimal digits remain, also the decimal dot.
	 * @param value the value to convert
	 * @param decimalDigits the number of decimal digits to retain
	 * @return the string representation of the argument double
	 */
	public static String toCleanString(double value, int decimalDigits) {
		for (int m = 0; m < decimalDigits; m++)
			value *= 10;
		value = Math.round(value);
		for (int m = 0; m < decimalDigits; m++)
			value /= 10;
		String valueStr = ("" + value);
		while (valueStr.endsWith("0"))
			valueStr = valueStr.substring(0, (valueStr.length()-1));
		if (valueStr.endsWith("."))
			valueStr = valueStr.substring(0, (valueStr.length()-1));
		return valueStr;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(getMetricValue(1, "Gallon"));
		System.out.println(getMetricValue(32, "°F"));
		System.out.println(toCleanString(getMetricValue(32, "°F"), 3));
		System.out.println(toCleanString(getMetricValue(32, "°F"), 5));
		System.out.println(getMetricValue(100, "°F"));
		System.out.println(getMetricValue(212, "°F"));
	}
}

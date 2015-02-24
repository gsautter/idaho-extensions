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
package de.uka.ipd.idaho.plugins.locations;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.TreeMap;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.util.constants.LocationConstants;

/**
 * This class provides utility methods for handling geographical coordinates,
 * like converting between decimal and degree/minute/second representation or
 * computing the distance between to points represented by coordinate pairs.
 * 
 * @author sautter
 */
public class GeoUtils implements LocationConstants {
	
	/**
	 * Convert a geo-coordinate given in degrees and minutes (the latter may
	 * include a fraction) into a decimal coordinate. Please note that this
	 * method always returns a positive (or zero) number, treating both
	 * arguments as absolute. Determining the sign of the decimal coordinate
	 * is up to client code. This is because coordinates are often a positive
	 * value labeled as north, south, east, or west, and the nature and parsing
	 * of the labels id beyond the scope of this method.
	 * @param deg the degree part of the coordinate
	 * @param min the minute part of the coordinate
	 * @return a decimal representation of the coordinate
	 */
	public static double getDecimalDegrees(int deg, double min) {
		return (Math.abs(deg) + Math.abs(min / 60));
	}
	
	/**
	 * Convert a geo-coordinate given in degrees, minutes, and seconds (the
	 * latter may include a fraction) into a decimal coordinate. Please note
	 * that this method always returns a positive (or zero) number, treating
	 * all arguments as absolute. Determining the sign of the decimal
	 * coordinate is up to client code. This is because coordinates are often
	 * a positive value labeled as north, south, east, or west, and the nature
	 * and parsing of the labels id beyond the scope of this method.
	 * @param deg the degree part of the coordinate
	 * @param min the minute part of the coordinate
	 * @param sec the second part of the coordinate
	 * @return a decimal representation of the coordinate
	 */
	public static double getDecimalDegrees(int deg, int min, double sec) {
		return (Math.abs(deg) + Math.abs(min / 60) + Math.abs(sec / 3600));
	}
	
	/**
	 * Convert a decimal geo-coordinate into a representation with degrees and
	 * minutes. The minute part may include a fraction. The argument decimal
	 * coordinate is treated as absolute. Adding a label to indicate north,
	 * south, east, or west is up to client code.
	 * @param deg the decimal coordinate to convert
	 * @param fractLen the maximum number of fraction digits in the minutes
	 * @return the degree/minute/second representation as a string
	 */
	public static String getDegMinDegrees(double dDeg, int fractLen) {
		dDeg = Math.abs(dDeg);
		int deg = ((int) Math.floor(dDeg));
		dDeg -= deg;
		dDeg *= 60;
		double min = dDeg;
		for (int f = 0; f < fractLen; f++)
			min *= 10;
		min = Math.round(min);
		for (int f = 0; f < fractLen; f++)
			min /= 10;
		if (min != 0)
			return (deg + "° " + ((fractLen < 1) ? ("" + ((int) min)) : ("" + min)) + "'");
		else return (deg + "°");
	}
	
	/**
	 * Convert a decimal geo-coordinate into a representation with degrees,
	 * minutes, and seconds. The second part may include a fraction. The
	 * argument decimal coordinate is treated as absolute. Adding a label to
	 * indicate north, south, east, or west is up to client code.
	 * @param deg the decimal coordinate to convert
	 * @param fractLen the maximum number of fraction digits in the seconds
	 * @return the degree/minute/second representation as a string
	 */
	public static String getDegMinSecDegrees(double dDeg, int fractLen) {
		dDeg = Math.abs(dDeg);
		int deg = ((int) Math.floor(dDeg));
		dDeg -= deg;
		dDeg *= 60;
		int min = ((int) Math.floor(dDeg));
		dDeg -= min;
		dDeg *= 60;
		double sec = dDeg;
		for (int f = 0; f < fractLen; f++)
			sec *= 10;
		sec = Math.round(sec);
		for (int f = 0; f < fractLen; f++)
			sec /= 10;
		if (sec != 0)
			return (deg + "° " + min + "' " + ((fractLen < 1) ? ("" + ((int) sec)) : ("" + sec)) + "\"");
		else if (min != 0)
			return (deg + "° " + min + "'");
		else return (deg + "°");
	}
	
	/**
	 * Create a point from a geo-referenced location. In particular, this
	 * method gets the <code>longitude</code> and <code>longitude</code>
	 * attributes from the argument annotation, parses them into doubles, and
	 * returns them wrapped in a point object. The longitude becomes the X
	 * value, the latitude the Y value. If either of the two attributes is not
	 * set, or if its value causes <code>Double.parseDouble()</code> to throw
	 * an exception, this method returns null.
	 * @param location the annotation whose coordinates to wrap
	 * @return a point wrapping the coordinates
	 */
	public static Point2D getPoint(Annotation location) {
		try {
			double x = Double.parseDouble((String) location.getAttribute(LONGITUDE_ATTRIBUTE));
			double y = Double.parseDouble((String) location.getAttribute(LATITUDE_ATTRIBUTE));
			return new Point2D.Double(x, y);
		}
		catch (RuntimeException re) {
			return null; // covers both number format exception for invalid attribute values and null pointer exception for missing values
		}
	}
	
	/**
	 * Set the the <code>longitude</code> and <code>longitude</code> attributes
	 * of an annotation. The attribute values are the X and Y components of the
	 * argument point, respectively.
	 * @param location the annotation to set the attributes
	 * @param point the point representing the coordinates
	 */
	public static void setPoint(Annotation location, Point2D point) {
		location.setAttribute(LONGITUDE_ATTRIBUTE, ("" + point.getX()));
		location.setAttribute(LATITUDE_ATTRIBUTE, ("" + point.getY()));
	}
	
	/**
	 * Compute the beeline distance between two points given in signed decimal
	 * coordinates wrapped in point objects, with the longitude in the X
	 * component and the latitude in the Y component.
	 * @param point1 the first point
	 * @param point2 the second point
	 * @return the distance between the two points, in meters
	 */
	public static double getDistance(Point2D point1, Point2D point2) {
		return getDistance(point1.getX(), point1.getY(), point2.getX(), point2.getY());
	}
	
	/**
	 * Compute the beeline distance between two points given in signed decimal
	 * coordinates.
	 * @param long1 the longitude of the first point
	 * @param lat1 the latitude of the first point
	 * @param long2 the longitude of the second point
	 * @param lat2 the latitude of the second point
	 * @return the distance between the two points, in meters
	 */
	public static double getDistance(double long1, double lat1, double long2, double lat2) {
		
		//	make latitudes angles down from north pole
		lat1 = 90 - lat1;
		lat2 = 90 - lat2;
		
		//	convert degrees to radiants
		double radLong1 = ((long1 * 2 * Math.PI) / 360);
		double radLat1 = ((lat1 * 2 * Math.PI) / 360);
		double radLong2 = ((long2 * 2 * Math.PI) / 360);
		double radLat2 = ((lat2 * 2 * Math.PI) / 360);
		
		//	convert coordinate pairs to Cartesian system
		double x1 = Math.cos(radLong1) * Math.sin(radLat1);
		double y1 = Math.sin(radLong1) * Math.sin(radLat1);
		double z1 = Math.cos(radLat1);
		double x2 = Math.cos(radLong2) * Math.sin(radLat2);
		double y2 = Math.sin(radLong2) * Math.sin(radLat2);
		double z2 = Math.cos(radLat2);
		
		//	use scalar product to compute angle between vectors
		double radDist = Math.acos((x1 * x2) + (y1 * y2) + (z1 * z2));
		
		//	use angle to compute distance as fraction of circumference
		return ((radDist * (1000 * 1000 * 40)) / (Math.PI * 2));
	}
	
	/**
	 * Compute the bearing between two points given in signed decimal
	 * coordinates wrapped in point objects, with the longitude in the X
	 * component and the latitude in the Y component.
	 * @param point1 the first point
	 * @param point2 the second point
	 * @return the bearing between the two points, in degrees
	 */
	public static double getBearing(Point2D point1, Point2D point2) {
		return getBearing(point1.getX(), point1.getY(), point2.getX(), point2.getY());
	}
	
	/**
	 * Compute the bearing between two points given in signed decimal
	 * coordinates.
	 * @param long1 the longitude of the first point
	 * @param lat1 the latitude of the first point
	 * @param long2 the longitude of the second point
	 * @param lat2 the latitude of the second point
	 * @return the beating between the two points, in meters
	 */
	public static double getBearing(double long1, double lat1, double long2, double lat2) {
		
		//	convert degrees to radiants
		double radLong1 = ((long1 * 2 * Math.PI) / 360);
		double radLat1 = ((lat1 * 2 * Math.PI) / 360);
		double radLong2 = ((long2 * 2 * Math.PI) / 360);
		double radLat2 = ((lat2 * 2 * Math.PI) / 360);
		
		//	compute bearing
		double radBearing = Math.atan2(Math.sin(radLong2 - radLong1) * Math.cos(radLat2), ((Math.cos(radLat1) * Math.sin(radLat2)) - (Math.sin(radLat1) * Math.cos(radLat2) * Math.cos(radLong2 - radLong1))));

		//	convert radiant back to degrees
		double bearing = ((radBearing * 360) / (2 * Math.PI));
		
		//	make bearing 0-360°
		if (bearing < 0)
			bearing += 360;
		
		//	finally ...
		return bearing;
	}
	
	/**
	 * Convert a verbalized bearing into degrees, counting clockwise, with zero
	 * pointing north. This method returns the respective constants for strings
	 * like 'north-west', 'north-north-west', 'north by north-west', or 'NNW'.
	 * Lookups are case insensitive. The returned bearing is in the 0-360 range,
	 * or exactly -1, if the argument bearing name cannot be mapped to a numeric
	 * bearing angle. The dictionary backing this method can be extended via
	 * the <code>addBearing()</code> method, e.g. to add named bearings in
	 * multiple languages. 
	 * @param name the name of the bearing
	 * @return the bearing in degrees
	 */
	public static double getBearing(String name) {
		Double degrees = ((Double) namedBearingsCustom.get(name));
		if (degrees == null)
			degrees = ((Double) namedBearingsDefault.get(name));
		return ((degrees == null) ? -1 : degrees.doubleValue());
	}
	
	/**
	 * Add a named bearing. The argument degrees have to be in the 0-360 range.
	 * The returned boolean indicates whether or not the pair of arguments has
	 * been accepted.
	 * @param name the name of the bearing
	 * @param degrees the numeric bearing
	 * @return true if the argument pair has been accepted
	 */
	public static boolean addBearing(String name, double degrees) {
		
		//	check name
		if ((name == null) || (name.trim().length() == 0))
			return false;
		
		//	check degrees
		if ((degrees < 0) || (360 < degrees))
			return false;
		
		//	store named bearing and indicate acceptance
		namedBearingsCustom.put(name, new Double(degrees));
		return true;
	}
	
	/**
	 * Translate a location represented by a coordinate pair by a distance in
	 * a given direction. The starting point must have the longitude in its X
	 * component and the latitude in its Y component. If the argument point is
	 * a <code>Point2D.Float</code>, so is the returned point, otherwise the
	 * returned point is a <code>Point2D.Double</code>. The longitude,
	 * latitude, and bearing must be given in decimal degrees, the bearing
	 * between 0 and 360, counting clockwise with 0 pointing north.
	 * @param start the starting point
	 * @param dist the distance in meters
	 * @param bearing the direction to move to
	 * @return a coordinate pair representing the translated point
	 */
	public static Point2D translate(Point2D start, int dist, double bearing) {
		double[] translated = translate(start.getX(), start.getY(), dist, bearing);
		if (start instanceof Point2D.Float)
			return new Point2D.Float(((float) translated[0]), ((float) translated[1]));
		else return new Point2D.Double(translated[0], translated[1]);
	}
	
	/**
	 * Translate a location represented by a coordinate pair by a distance in
	 * a given direction. The returned array has two elements, the first one
	 * being the translated longitude, the second the translated latitude. The
	 * longitude, latitude, and bearing must be given in decimal degrees, the
	 * bearing between 0 and 360, counting clockwise with 0 pointing north.
	 * @param startLong the longitude of the starting point
	 * @param startLat the latitude of the starting point
	 * @param dist the distance in meters
	 * @param bearing the direction to move to
	 * @return a coordinate pair representing the translated point
	 */
	public static double[] translate(double startLong, double startLat, int dist, double bearing) {
		
		//	compute angular distance
		double radDist = ((2 * Math.PI * dist) / (40 * 1000 * 1000));
		
		//	convert degrees to radiants
		double radStartLong = ((startLong * 2 * Math.PI) / 360);
		double radStartLat = ((startLat * 2 * Math.PI) / 360);
		double radBearing = ((bearing * 2 * Math.PI) / 360);
		
		//	compute translated point
		double radTransLat = Math.asin((Math.sin(radStartLat) * Math.cos(radDist)) + (Math.cos(radStartLat) * Math.sin(radDist) * Math.cos(radBearing)));
		double radTransLong = radStartLong + Math.atan2((Math.sin(radBearing) * Math.sin(radDist) * Math.cos(radStartLat)), (Math.cos(radDist) - (Math.sin(radStartLat) * Math.sin(radTransLat))));
		
		//	convert back to degrees
		double[] translated = {
			((radTransLong * 360) / (2 * Math.PI)),
			((radTransLat * 360) / (2 * Math.PI))
		};
		return translated;
	}
	
	/** straight north bearing */
	public static final double BEARING_NORTH = 0;
	
	/** north by east bearing */
	public static final double BEARING_NORTH_BY_EAST = 11.25;
	
	/** north-north-east bearing */
	public static final double BEARING_NORTH_NORTH_EAST = 22.5;
	
	/** north-east by north bearing */
	public static final double BEARING_NORTH_EAST_BY_NORTH = 33.75;
	
	/** north-east bearing */
	public static final double BEARING_NORTH_EAST = 45;
	
	/** north-east by east bearing */
	public static final double BEARING_NORTH_EAST_BY_EAST = 56.25;
	
	/** east-north-east bearing */
	public static final double BEARING_EAST_NORTH_EAST = 67.5;
	
	/** east by north bearing */
	public static final double BEARING_EAST_BY_NORTH = 78.75;
	
	/** straight east bearing */
	public static final double BEARING_EAST = 90;
	
	/** east by south bearing */
	public static final double BEARING_EAST_BY_SOUTH = 101.25;
	
	/** east-south-east bearing */
	public static final double BEARING_EAST_SOUTH_EAST = 112.5;
	
	/** south-east by east bearing */
	public static final double BEARING_SOUTH_EAST_BY_EAST = 123.75;
	
	/** south-east bearing */
	public static final double BEARING_SOUTH_EAST = 135;
	
	/** south-east by south bearing */
	public static final double BEARING_SOUTH_EAST_BY_SOUTH = 146.25;
	
	/** south-south-east bearing */
	public static final double BEARING_SOUTH_SOUTH_EAST = 157.5;
	
	/** south by east bearing */
	public static final double BEARING_SOUTH_BY_EAST = 168.75;
	
	/** straight south bearing */
	public static final double BEARING_SOUTH = 180;
	
	/** south by west bearing */
	public static final double BEARING_SOUTH_BY_WEST = 191.25;
	
	/** south-south-west bearing */
	public static final double BEARING_SOUTH_SOUTH_WEST = 202.5;
	
	/** south-west by south bearing */
	public static final double BEARING_SOUTH_WEST_BY_SOUTH = 213.75;
	
	/** south-west bearing */
	public static final double BEARING_SOUTH_WEST = 225;
	
	/** south-west by west bearing */
	public static final double BEARING_SOUTH_WEST_BY_WEST = 236.25;
	
	/** west-south-west bearing */
	public static final double BEARING_WEST_SOUTH_WEST = 247.5;
	
	/** west by south bearing */
	public static final double BEARING_WEST_BY_SOUTH = 258.75;
	
	/** straight west bearing */
	public static final double BEARING_WEST = 270;
	
	/** west by north bearing */
	public static final double BEARING_WEST_BY_NORTH = 281.25;
	
	/** west-north-west bearing */
	public static final double BEARING_WEST_NORTH_WEST = 292.5;
	
	/** north-west by west bearing */
	public static final double BEARING_NORTH_WEST_BY_WEST = 303.75;
	
	/** north-west bearing */
	public static final double BEARING_NORTH_WEST = 315;
	
	/** north-west by north bearing */
	public static final double BEARING_NORTH_WEST_BY_NORTH = 326.25;
	
	/** north-north-west bearing */
	public static final double BEARING_NORTH_NORTH_WEST = 337.5;
	
	/** north by west bearing */
	public static final double BEARING_NORTH_BY_WEST = 348.75;
	
	private static TreeMap namedBearingsDefault = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	static {
		addDefaultBearing("N; N; north; north", BEARING_NORTH);
		addDefaultBearing("NbE; NtE; north by east; north by east", BEARING_NORTH_BY_EAST);
		addDefaultBearing("NNE; NNE; north-northeast; north-north-east", BEARING_NORTH_NORTH_EAST);
		addDefaultBearing("NEbN; NEtN; northeast by north; north-east by north", BEARING_NORTH_EAST_BY_NORTH);
		addDefaultBearing("NE; NE; northeast; north-east", BEARING_NORTH_EAST);
		addDefaultBearing("NEbE; NEtE; northeast by east; north-east by east", BEARING_NORTH_EAST_BY_EAST);
		addDefaultBearing("ENE; ENE; east-northeast; east-north-east", BEARING_EAST_NORTH_EAST);
		addDefaultBearing("EbN; EtN; east by north; east by north", BEARING_EAST_BY_NORTH);
		addDefaultBearing("E; E; east; east", BEARING_EAST);
		addDefaultBearing("EbS; EtS; east by south; east by south", BEARING_EAST_BY_SOUTH);
		addDefaultBearing("ESE; ESE; east-southeast; east-south-east", BEARING_EAST_SOUTH_EAST);
		addDefaultBearing("SEbE; SEtE; southeast by east; south-east by east", BEARING_SOUTH_EAST_BY_EAST);
		addDefaultBearing("SE; SE; southeast; south-east", BEARING_SOUTH_EAST);
		addDefaultBearing("SEbS; SEtS; southeast by south; south-east by south", BEARING_SOUTH_EAST_BY_SOUTH);
		addDefaultBearing("SSE; SSE; south-southeast; south-south-east", BEARING_SOUTH_SOUTH_EAST);
		addDefaultBearing("SbE; StE; south by east; south by east", BEARING_SOUTH_BY_EAST);
		addDefaultBearing("S; S; south; south", BEARING_SOUTH);
		addDefaultBearing("SbW; StW; south by west; south by west", BEARING_SOUTH_BY_WEST);
		addDefaultBearing("SSW; SSW; south-southwest; south-south-west", BEARING_SOUTH_SOUTH_WEST);
		addDefaultBearing("SWbS; SWtS; southwest by south; south-west by south", BEARING_SOUTH_WEST_BY_SOUTH);
		addDefaultBearing("SW; SW; southwest; south-west", BEARING_SOUTH_WEST);
		addDefaultBearing("SWbW; SWtW; southwest by west; south-west by west", BEARING_SOUTH_WEST_BY_WEST);
		addDefaultBearing("WSW; WSW; west-southwest; west-south-west", BEARING_WEST_SOUTH_WEST);
		addDefaultBearing("WbS; WtS; west by south; west by south", BEARING_WEST_BY_SOUTH);
		addDefaultBearing("W; W; west; west", BEARING_WEST);
		addDefaultBearing("WbN; WtN; west by north; west by north", BEARING_WEST_BY_NORTH);
		addDefaultBearing("WNW; WNW; west-northwest; west-north-west", BEARING_WEST_NORTH_WEST);
		addDefaultBearing("NWbW; NWtW; northwest by west; north-west by west", BEARING_NORTH_WEST_BY_WEST);
		addDefaultBearing("NW; NW; northwest; north-west", BEARING_NORTH_WEST);
		addDefaultBearing("NWbN; NWtN; northwest by north; north-west by north", BEARING_NORTH_WEST_BY_NORTH);
		addDefaultBearing("NNW; NNW; north-northwest; north-north-west", BEARING_NORTH_NORTH_WEST);
		addDefaultBearing("NbW; NtW; north by west; north by west", BEARING_NORTH_BY_WEST);
	}
	private static void addDefaultBearing(String nameList, double degrees) {
		String[] names = nameList.split("\\s*\\;\\s*");
		Double degreeObj = new Double(degrees);
		for (int n = 0; n < names.length; n++)
			namedBearingsDefault.put(names[n], degreeObj);
	}
	private static TreeMap namedBearingsCustom = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	
	//	!!! TEST ONLY !!!
	public static void main(String[] args) {
		System.out.println(getDegMinSecDegrees(-8.5048, 6));
		System.out.println(getDegMinDegrees(-8.5048, 8));
		System.out.println(getDistance(8.4, 49.0, 8.68, 50.12));
		
		System.out.println(getDistance(0, 0, 0, 1));
		System.out.println(getDistance(0, 0, 0, 2));
		System.out.println(getDistance(0, 1, 0, 2));
		
		System.out.println(getDistance(0, 0, 1, 0));
		System.out.println(getDistance(0, 0, 2, 0));
		System.out.println(getDistance(1, 0, 2, 0));
		
		System.out.println(getDistance(0, 45, 1, 45));
		System.out.println(getDistance(0, 45, 2, 45));
		System.out.println(getDistance(1, 45, 2, 45));
		
		int dist;
		dist = ((int) getDistance(0, 45, 0, 46));
		System.out.println(dist);
		System.out.println(Arrays.toString(translate(0, 45, dist, BEARING_NORTH)));
		
		dist = ((int) getDistance(0, 45, 0, 44));
		System.out.println(dist);
		System.out.println(Arrays.toString(translate(0, 45, dist, BEARING_SOUTH)));
		
		dist = ((int) getDistance(0, 45, 0, 46));
		System.out.println(dist);
		System.out.println(Arrays.toString(translate(0, 45, dist, getBearing(0, 45, 0, 46))));
		
		dist = ((int) getDistance(45, 45, 44, 44));
		System.out.println(dist);
		System.out.println(Arrays.toString(translate(45, 45, dist, getBearing(45, 45, 44, 44))));
		
		dist = ((int) getDistance(45, 45, 46, 46));
		System.out.println(dist);
		System.out.println(Arrays.toString(translate(45, 45, dist, getBearing(45, 45, 46, 46))));
		
		System.out.println(getBearing(1, 1, -1, -1));
		
		System.out.println(getBearing(45, 0, -45, 0));
		System.out.println(getBearing(-45, 0, 45, 0));
		
		System.out.println(Arrays.toString(translate(143.3, -12.75, 11000, BEARING_EAST_NORTH_EAST)));
	}
}
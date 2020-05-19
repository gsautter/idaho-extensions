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
package de.uka.ipd.idaho.gamta.util.markupScript;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.uka.ipd.idaho.easyIO.streams.PeekReader;
import de.uka.ipd.idaho.easyIO.util.JsonParser;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.markupScript.MarkupScriptTypes.MsArray;
import de.uka.ipd.idaho.gamta.util.markupScript.MarkupScriptTypes.MsBoolean;
import de.uka.ipd.idaho.gamta.util.markupScript.MarkupScriptTypes.MsDocument;
import de.uka.ipd.idaho.gamta.util.markupScript.MarkupScriptTypes.MsMap;
import de.uka.ipd.idaho.gamta.util.markupScript.MarkupScriptTypes.MsNumber;
import de.uka.ipd.idaho.gamta.util.markupScript.MarkupScriptTypes.MsObject;
import de.uka.ipd.idaho.gamta.util.markupScript.MarkupScriptTypes.MsString;

/**
 * TODO document this (soon as we've completely hammered out the whole thing ...)
 * 
 * @author sautter
 */
public class MarkupScript {
	
	/**
	 * TODO document this class
	 * 
	 * @author sautter
	 */
	public static interface Resolver {
		
		/**
		 * Retrieve a Markup script by its name. If the resolver does not have
		 * a Markup Script available by the argument name, it should return null
		 * rather than throwing an exception.
		 * @param name the name of the sought Markup Script
		 * @return the Markup Script with the argument name, or null if the
		 *            resolver does not have the sought Markup Script
		 */
		public abstract MarkupScript getMarkupScript(String name);
	}
	
	private static LinkedList resolvers = new LinkedList();
	
	/**
	 * Register a Markup Script resolver.
	 * @param resolver the resolver to register
	 */
	public static synchronized void registerResolver(Resolver resolver) {
		resolvers.addFirst(resolver);
	}
	
	/**
	 * Register a Markup Script resolver.
	 * @param resolver the resolver to unregister
	 */
	public static synchronized void unRegisterResolver(Resolver resolver) {
		while (resolvers.remove(resolver)); // remove ALL occurrences of the resolver
	}
	
	/**
	 * Retrieve a Markup script by its name. If the resolver does not have
	 * a Markup Script available by the argument name, it should return null
	 * rather than throwing an exception.
	 * @param name the name of the sought Markup Script
	 * @return the Markup Script with the argument name, or null if the
	 *            resolver does not have the sought Markup Script
	 */
	public static synchronized MarkupScript getMarkupScriptForName(String name) {
		for (Iterator nrit = resolvers.iterator(); nrit.hasNext();) {
			MarkupScript ms = ((Resolver) nrit.next()).getMarkupScript(name);
			if (ms != null)
				return ms;
		}
		return null;
	}
	
	/**
	 * TODO document this class
	 * 
	 * @author sautter
	 */
	public static interface NameResolver {
		
		/**
		 * Retrieve the namespace prefix this resolver is responsible for.
		 * @return the namespace prefix
		 */
		public abstract String getNamespace();
		
		/**
		 * Retrieve a function by its name. If the resolver does not have a
		 * function available by the argument name, it should return null
		 * rather than throwing an exception, as there might be further
		 * resolvers for the same namespace.
		 * @param name the name of the sought function
		 * @return the function with the argument name, or null if the resolver
		 *            does not have the sought function
		 */
		public abstract Function getFunction(String name);
		
		/**
		 * Retrieve a constant value by its name. If the resolver does not have
		 * a constant available by the argument name, it should return null
		 * rather than throwing an exception, as there might be further
		 * resolvers for the same namespace.
		 * @param name the name of the sought constant
		 * @return the constant with the argument name, or null if the resolver
		 *            does not have the sought constant
		 */
		public abstract MsObject getConstant(String name);
	}
//	
//	/**
//	 * TODO document this class
//	 * 
//	 * @author sautter
//	 */
//	public static class GPathArray extends GPathObject {
//		private ArrayList values = new ArrayList();
//		public GPathString asString() {
//			// TODO concatenate values
//			return null;
//		}
//		public GPathNumber asNumber() {
//			// TODO concatenate values
//			return null;
//		}
//		public GPathBoolean asBoolean() {
//			// TODO concatenate values
//			return null;
//		}
//		GPathObject get(GPathNumber index) {
//			return ((GPathObject) this.values.get((int) Math.round(index.value)));
//		}
//		GPathObject set(GPathNumber index, GPathObject value) {
//			return ((GPathObject) this.values.set(((int) Math.round(index.value)), value));
//		}
//		int size() {
//			return this.values.size();
//		}
//	}
//	
//	/**
//	 * TODO document this class
//	 * 
//	 * @author sautter
//	 */
//	public static class GPathMap extends GPathObject {
//		private HashMap values = new HashMap();
//		public GPathString asString() {
//			// TODO concatenate values
//			return null;
//		}
//		public GPathNumber asNumber() {
//			// TODO concatenate values
//			return null;
//		}
//		public GPathBoolean asBoolean() {
//			// TODO concatenate values
//			return null;
//		}
//		GPathObject get(GPathString key) {
//			return ((GPathObject) this.values.get(key.value));
//		}
//		GPathObject set(GPathString key, GPathObject value) {
//			return ((GPathObject) this.values.put(key.value, value));
//		}
//		int size() {
//			return this.values.size();
//		}
//	}
	
	/* TODO add object-ish syntax:
	 * - <var>.<functionName>(...) ==> vars:<functionName>(<var>, ...):
	 *   - <array>.<functionName>(...) ==> arrays:<functionName>(<array>, ...)
	 *   - <map>.<functionName>(...) ==> maps:<functionName>(<map>, ...)
	 *   - <string>.<functionName>(...) ==> strings:<functionName>(<string>, ...)
	 *   - <number>.<functionName>(...) ==> numbers:<functionName>(<number>, ...)
	 *   - etc.
	 * - imports as <var>s namespace take priority over system functions (polymorphly) ==> way of modifying and extending built-in types
	 * - imports take precedence in reverse order specified, with default system implementations last
	 *   ==> TODO implement function resolver accordingly (mapping namespaces to lists of resolvers)
	 *   ==> TODO observe type inheritance relationships on matching
	 * 
	 * - allow modification of constants only in local namespace, i.e., without explicit namespace prefix ...
	 * - ... and then ... DON'T make that restriction, as globals may 
	 */
	
	private static class MarkupScriptParse {
		private MarkupScript script;
		private LinkedList scopeStack = new LinkedList();
		private Properties globalVarsToTypes = new Properties();
		private Properties scopeVarsToTypes = this.globalVarsToTypes;
		private HashSet importNamespaces = new HashSet();
		MarkupScriptParse(MarkupScript script) {
			this.script = script;
		}
		void setGlobalVarType(String name, String type) {
			if (this.scopeStack.isEmpty())
				this.globalVarsToTypes.setProperty(name, type);
			else throw new RuntimeException("Constant '§" + name + "' can only be defined at top level.");
		}
		String getGlobalVarType(String name) {
			return this.globalVarsToTypes.getProperty(name);
		}
		int getScopeDepth() {
			return this.scopeStack.size();
		}
		void startScope(boolean inherit) {
			this.scopeVarsToTypes = new Properties(inherit ? this.scopeVarsToTypes : this.globalVarsToTypes);
			this.scopeStack.addLast(this.scopeVarsToTypes);
		}
		void endScope() {
			this.scopeStack.removeLast();
			this.scopeVarsToTypes = (this.scopeStack.isEmpty() ? this.globalVarsToTypes : ((Properties) this.scopeStack.getLast()));
		}
		void setScopeVarType(String name, String type) {
			this.scopeVarsToTypes.setProperty(name, type);
		}
		String getScopeVarType(String name) {
			return this.scopeVarsToTypes.getProperty(name);
		}
		void addImportNamespace(String namespace) {
			this.importNamespaces.add(namespace);
		}
		boolean checkImportNamespace(String namespace) {
			return this.importNamespaces.contains(namespace);
		}
		void recordParseException(MarkupScriptParseException mspe) {
			this.script.addParseException(mspe);
		}
	}
	
	private static class MarkupScriptParseException extends RuntimeException {
		final int position;
		final int length;
		MarkupScriptParseException(String message, int position, int length) {
			super(message);
			this.position = position;
			this.length = length;
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.position, this.length, STYLE_ERROR, false);
			//	TODO implement adding red underline
		}
	}
	
	private static class UndeclaredVariableException extends MarkupScriptParseException {
		final String name;
		UndeclaredVariableException(int position, String name) {
			super(("Undeclared variable '$" + name + "' at " + position + "."), position, ("$".length() + name.length()));
			this.name = name;
		}
	}
	
	private static class DuplicateVariableException extends MarkupScriptParseException {
		final String name;
		DuplicateVariableException(int position, String name) {
			super(("Duplicate variable '$" + name + "' at " + position + "."), position, ("$".length() + name.length()));
			this.name = name;
		}
	}
	
	private static class UndeclaredNamespaceException extends MarkupScriptParseException {
		final String name;
		UndeclaredNamespaceException(int position, String name) {
			super(("Undeclared namespace '" + name + "' at " + position + "."), position, name.length());
			this.name = name;
		}
	}
	
	private static class UnresolvableImportException extends MarkupScriptParseException {
		UnresolvableImportException(Import imp) {
			super(("Unresolvable import '" + imp.name + "' at " + imp.start + "."), imp.start, (imp.end - imp.start));
		}
	}
	
	private static class UndeclaredFunctionException extends MarkupScriptParseException {
		UndeclaredFunctionException(FunctionCall funcCall) {
			super(("Undeclared function '" + ((funcCall.funcNamespace == null) ? "" : (funcCall.funcNamespace + ":")) + funcCall.funcName + "' at " + funcCall.start + "."), funcCall.start, (funcCall.end - funcCall.start));
		}
	}
	
	private static class UndeclaredConstantException extends MarkupScriptParseException {
		UndeclaredConstantException(int position, String name) {
			super(("Undeclared constant '§" + name + "' at " + position + "."), position, ("§".length() + name.length()));
		}
		UndeclaredConstantException(VariableReference vRef) {
			super(("Undeclared constant '§" + ((vRef.varNamespace == null) ? "" : (vRef.varNamespace + ":")) + vRef.varName + "' at " + vRef.start + "."), vRef.start, (vRef.end - vRef.start));
		}
	}
	
	private static class DuplicateConstantException extends MarkupScriptParseException {
		final String name;
		DuplicateConstantException(int position, String name) {
			super(("Duplicate constant '§" + name + "' at " + position + "."), position, ("§".length() + name.length()));
			this.name = name;
		}
	}
	
	private static class MissingCharactersException extends MarkupScriptParseException {
		final String foundChars;
		final String expectedChars;
		MissingCharactersException(int position, char foundChar, char expectedChar) {
			super(("Unexpected character '" + foundChar + "' at " + position + ", expected '" + expectedChar + "'."), position, 1);
			this.foundChars = ("" + foundChar);
			this.expectedChars = ("" + expectedChar);
		}
		MissingCharactersException(int position, String foundChars, String expectedChars) {
			super(("Unexpected character '" + foundChars + "' at " + position + ", expected '" + expectedChars + "'."), position, 1);
			this.foundChars = foundChars;
			this.expectedChars = expectedChars;
		}
	}
	
	private static class UnexpectedCharactersException extends MarkupScriptParseException {
		final String foundChars;
		final String expectedChars;
		UnexpectedCharactersException(int position, String foundChars) {
			super(("Unexpected characters '" + foundChars + "' at " + position + "."), position, foundChars.length());
			this.foundChars = foundChars;
			this.expectedChars = null;
		}
		UnexpectedCharactersException(int position, String foundChars, String expectedChars) {
			super(("Unexpected characters '" + foundChars + "' at " + position + ", expected '" + expectedChars + "'."), position, foundChars.length());
			this.foundChars = foundChars;
			this.expectedChars = expectedChars;
		}
		UnexpectedCharactersException(int position, char foundChar) {
			super(("Unexpected character '" + foundChar + "' at " + position + "."), position, 1);
			this.foundChars = ("" + foundChar);
			this.expectedChars = null;
		}
		UnexpectedCharactersException(int position, char foundChar, char expectedChar) {
			super(("Unexpected character '" + foundChar + "' at " + position + ", expected '" + expectedChar + "'."), position, 1);
			this.foundChars = ("" + foundChar);
			this.expectedChars = ("" + expectedChar);
		}
		UnexpectedCharactersException(int position, char foundChar, char expectedChar1, char expectedChar2) {
			super(("Unexpected character '" + foundChar + "' at " + position + ", expected '" + expectedChar1 + "' or '" + expectedChar2 + "'."), position, 1);
			this.foundChars = ("" + foundChar);
			this.expectedChars = (expectedChar1 + "" + expectedChar2);
		}
	}
	
	private static class MarkupScriptExecutionContext {
		private MarkupScript script;
		private MarkupScriptExecutionContext parent;
		private HashMap variableTypes = new HashMap();
		private HashMap variableValues = new HashMap();
		MarkupScriptExecutionContext(MarkupScript script) {
			this(script, null);
		}
		MarkupScriptExecutionContext(MarkupScript script, MarkupScriptExecutionContext parent) {
			this.script = script;
			this.parent = parent;
		}
		Function getFunction(String namespace, String name, VariableDeclaration[] args) {
			//	TODO resolve via wrapped script object (OR MAYBE, do this while parsing)
		}
		MsObject getConstant(String namespace, String name) {
			//	TODO resolve via wrapped script object
		}
		//	TODO do we need methods for setting constants?
		MsObject getVariable(String name) {
			if (this.variableTypes.containsKey(name))
				return ((MsObject) this.variableValues.get(name));
			else if (this.parent != null)
				return this.parent.getVariable(name);
			else return null; // TODO throw exception instead?
		}
		MsObject getVariable(String name, MsObject def) {
			if (this.variableTypes.containsKey(name))
				return ((MsObject) this.variableValues.get(name));
			else if (this.parent != null)
				return this.parent.getVariable(name, def);
			else return def; // TODO throw exception instead?
		}
		public boolean isVariableSet(String name) {
			if (this.variableTypes.containsKey(name))
				return true;
			else if (this.parent != null)
				return this.parent.isVariableSet(name);
			else return false; // TODO throw exception instead?
		}
		void declareVariable(String name, String type) {
			this.variableTypes.put(name, type);
		}
		String getVariableType(String name) {
			return ((String) this.variableTypes.get(name));
		}
		MsObject setVariable(String name, MsObject value) {
			if (this.variableTypes.containsKey(name))
				return ((MsObject) this.variableValues.put(name, value));
			else if (this.parent != null)
				return this.parent.setVariable(name, value);
			else return null; // TODO throw exception instead?
		}
		MsObject removeVariable(String name) {
			if (this.variableTypes.containsKey(name))
				return ((MsObject) this.variableValues.remove(name));
			else if (this.parent != null)
				return this.parent.removeVariable(name);
			else return null; // TODO throw exception instead?
		}
	}
	
	private static abstract class Part implements Comparable {
		final String type; // type of this part
		final int start; // starting position in source script
		final int end; // ending position in source script
		String source; // wherever this part was imported from (null for current script, to be set when resolving imports)
		Part(String type, int start, int end) {
			this.type = type;
			this.start = start;
			this.end = end;
			System.out.println("Part '" + this.type + "' at " + this.start + "-" + this.end);
		}
		abstract void printString(String indent);
		abstract void styleCode(StyledDocument sd);
		public int compareTo(Object obj) {
			Part part = ((Part) obj);
			if (this.start == part.start)
				return (part.end - this.end);
			else return (this.start - part.start);
		}
	}
	
	private static class Import extends Part {
		final String name;
		final String namespace;
		final int nsStart; // starting position of namespace declaration in source script
		MarkupScript script; // the imported script
//		Import(int start, int end, String name) {
//			this(start, end, name, -1, null);
//		}
		Import(int start, int end, String name, int nsStart, String namespace) {
			super("import", start, end);
			this.name = name;
			this.nsStart = nsStart;
			this.namespace = namespace;
		}
		void printString(String indent) {
			System.out.println(indent + "import " + this.name + ((this.namespace == null) ? "" : (" as " + this.namespace)));
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, "import".length(), STYLE_KEYWORD, true);
			if (this.nsStart != -1)
				sd.setCharacterAttributes(this.nsStart, "as".length(), STYLE_KEYWORD, true);
		}
	}
	
	private static class Comment extends Part {
		final String text;
		Comment(int start, int end, String text) {
			super("comment", start, end);
			this.text = text;
		}
		void printString(String indent) {
			System.out.println(indent + "/* comment of " + (this.end - this.start) + " characters */");
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, (this.end - this.start), STYLE_COMMENT, true);
		}
	}
	
	private static abstract class Expression extends Part {
		Expression(String type, int start, int end) {
			super(type, start, end);
		}
		abstract String getReturnType(); // TODO cache return type (helps recursive resolution)
		abstract MsObject evaluate(MsDocument data, MarkupScriptExecutionContext context);
	}
	
	private static abstract class Operator extends Part {
		final String name;
		final int precedence;
		Operator(int start, int end, String name, int precedence) {
			super("operator", start, end);
			this.name = name;
			this.precedence = precedence;
		}
		abstract boolean isApplicableTo(String leftArgType, String rightArgType);
		abstract String getReturnType(String leftArgType, String rightArgType);
		abstract MsObject applyTo(MsObject left, MsObject right);
		void printString(String indent) { /* operators are printed by their parent expressions */ }
		void styleCode(StyledDocument sd) {
			//	no styling here for now
		}
	}
	
	private static abstract class AssigningOperator extends Operator {
		AssigningOperator(int start, int end, String name) {
			super(start, end, name, ASSIGN_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return leftArgType;
		}
		//abstract boolean mark();
	}
	
	private static abstract class Executable extends Part {
		Executable(String type, int start, int end) {
			super(type, start, end);
		}
		abstract MsObject execute(MsDocument data, MarkupScriptExecutionContext context);
	}
	
	private static class VariableDeclaration extends Executable {
		final String varType;
		final String varName;
		final int varNameStart;
		final boolean varIsConstant;
		final Expression value;
		Comment documentation; // documentation, used for constants only
		VariableDeclaration(int start, int end, String type, String name, int nameStart, Expression value, boolean isConstant) {
			super("variabledeclaration", start, end);
			this.varType = type;
			this.varName = name;
			this.varNameStart = nameStart;
			this.varIsConstant = isConstant;
			this.value = value;
		}
		MsObject execute(MsDocument data, MarkupScriptExecutionContext context) {
			context.declareVariable(this.varName, this.varType);
			MsObject value = null;
			if (this.value != null) {
				value = this.value.evaluate(data, context);
				context.setVariable(this.varName, value);
			}
			return value;
		}
		void printString(String indent) {
			if (this.value == null)
				System.out.println(indent + this.varType + " $" + this.varName);
			else {
				System.out.println(indent + this.varType + " $" + this.varName + " =");
				this.value.printString(indent + "  ");
			}
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, this.varType.length(), STYLE_KEYWORD, true);
			sd.setCharacterAttributes(this.varNameStart, ("$".length() + this.varName.length()), STYLE_VARIABLE, true);
			if (this.value != null)
				this.value.styleCode(sd);
		}
	}
	
	private static class VariableAssignment extends Executable {
		final String name;
		final Expression index;
		final Expression value;
		VariableAssignment(int start, int end, String name, Expression index, Expression value) {
			super("variableassignment", start, end);
			this.name = name;
			this.index = index;
			this.value = value;
		}
		MsObject execute(MsDocument data, MarkupScriptExecutionContext context) {
			if (this.name == null) // function call modeled as right side of assignment with missing left side
				return this.value.evaluate(data, context);
			
			MsObject value = this.value.evaluate(data, context);
//			if (this.value == null) {
//				if ("string".equals(this.type))
//					value = new GPathString("");
//				else if ("number".equals(this.type))
//					value = new GPathNumber(0);
//				else if ("boolean".equals(this.type))
//					value = new GPathBoolean(false);
//				else if ("array".equals(this.type))
//					value = new GPathArray();
//				else if ("map".equals(this.type))
//					value = new GPathMap();
//				//	TODO implement other types (annotation, former two with JSON value)
//				else value = new GPathString(""); // TODO throw exception instead? Or simply do nothing?
//			}
//			else value = this.value.evaluate(data, vars);
			
			if (this.index == null)
				return context.setVariable(this.name, value);
			
			MsObject index = this.index.evaluate(data, context);
			MsObject values = context.getVariable(this.name);
			if (values instanceof MsMap) // need to catch this first, as annotations are both attribute map and token array, but mutable only as the former
				return ((MsMap) values).setValue(index.asString(), value);
			else if (values instanceof MsArray)
				return ((MsArray) values).setObject(index.asNumber(), value);
			else throw new RuntimeException("Cannot set " + index + " of " + values + " to " + value);
			//	TODO devise dedicated exception for this case
		}
		void printString(String indent) {
			if (this.name == null)
				this.value.printString(indent);
			else if (this.index == null) {
				System.out.println(indent + "$" + this.name + " =");
				this.value.printString(indent + "  ");
			}
			else {
				System.out.println(indent + "$" + this.name + "[");
				this.index.printString(indent + "  ");
				System.out.println(indent + "] =");
				this.value.printString(indent + "  ");
			}
		}
		void styleCode(StyledDocument sd) {
			if (this.name != null)
				sd.setCharacterAttributes(this.start, ("$".length() + this.name.length()), STYLE_VARIABLE, true);
			if (this.index != null)
				this.index.styleCode(sd);
			if (this.value != null)
				this.value.styleCode(sd);
		}
	}
	
	private static class Literal extends Expression {
		final MsObject value;
		Literal(int start, int end, MsObject value) {
			super("literal", start, end);
			this.value = value;
		}
		String getReturnType() {
			if (this.value instanceof MsBoolean)
				return "boolean";
			else if (this.value instanceof MsNumber)
				return "number";
			else if (this.value instanceof MsString)
				return "string";
			else if (this.value instanceof MsArray)
				return "array";
			else if (this.value instanceof MsMap)
				return "map";
			else return "var";
		}
		MsObject evaluate(MsDocument data, MarkupScriptExecutionContext context) {
			return this.value;
		}
		void printString(String indent) {
			//	TODO revisit this to properly produce JSON
			if (this.value instanceof MsBoolean)
				System.out.println(indent + this.value.asBoolean());
			else if (this.value instanceof MsNumber)
				System.out.println(indent + this.value.asNumber());
			else if (this.value instanceof MsString)
				System.out.println(indent + this.value.asString());
			else if (this.value instanceof MsArray)
				System.out.println(indent + ((MsArray) this.value));
			else if (this.value instanceof MsMap)
				System.out.println(indent + ((MsMap) this.value));
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, (this.end - this.start), STYLE_LITERAL, true);
		}
	}
	
	private static class VariableReference extends Expression {
		final String varName;
		final String varNamespace;
		final boolean varIsConstant;
		final Expression index;
		String varType; // cannot be final, as it will be resolved only after parsing for imported globals
		VariableReference(int start, int end, String namespace, String name, String type, boolean isConstant) {
			this(start, end, namespace, name, null, type, isConstant);
		}
		VariableReference(int start, int end, String namespace, String name, Expression index, String type, boolean isConstant) {
			super("variablereference", start, end);
			this.varNamespace = namespace;
			this.varName = name;
			this.varIsConstant = isConstant;
			this.index = index;
			this.varType = type;
		}
		String getReturnType() {
			return ((this.varType == null) ? "var" : this.varType);
		}
		MsObject evaluate(MsDocument data, MarkupScriptExecutionContext context) {
			//	TODO observe namespace of imported constants
			//	TODO figure out a way of resolving constants as such
			//	TODO ==> resolve constants to literals late in parsing phase
			//	TODO ==> make script object not only resolver for functions, but also for constants
			//	TODO ==> adjust resolver interface accordingly, and rename to NamespaceResolver
			MsObject value = context.getVariable(this.varName);
			if (this.index == null)
				return value;
			MsObject index = this.index.evaluate(data, context);
			if ((value instanceof MsArray) && (index instanceof MsNumber)) // need to catch index type as well, as annotations are both attribute map and token array
				return ((MsArray) value).getObject((MsNumber) index);
			else if (value instanceof MsMap)
				return ((MsMap) value).getValue(index.asString());
			else throw new RuntimeException("Cannot get " + index + " of " + value);
		}
		void printString(String indent) {
			if (this.index == null)
				System.out.println(indent + (this.varIsConstant ? "§" : "$") + this.varName);
			else {
				System.out.println(indent + (this.varIsConstant ? "§" : "$") + this.varName + "[");
				this.index.printString(indent + "  ");
				System.out.println(indent + "]");
			}
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, ("$".length() + this.varName.length()), STYLE_VARIABLE, true);
		}
	}
	
	private static class ParenthesisExpression extends Expression {
		final Expression content;
		ParenthesisExpression(int start, int end, Expression content) {
			super("expression", start, end);
			this.content = content;
		}
		String getReturnType() {
			return this.content.getReturnType();
		}
		MsObject evaluate(MsDocument data, MarkupScriptExecutionContext context) {
			return this.content.evaluate(data, context);
		}
		void printString(String indent) {
			System.out.println(indent + "(");
			this.content.printString(indent + "  ");
			System.out.println(indent + ")");
		}
		void styleCode(StyledDocument sd) {
			this.content.styleCode(sd);
		}
	}
	
	private static class BinaryExpression extends Expression {
		final Expression left;
		final Operator operator;
		final Expression right;
		BinaryExpression(int start, int end, Expression left, Operator operator, Expression right) {
			super("binaryexpression", start, end);
			this.left = left;
			this.operator = operator;
			this.right = right;
		}
		String getReturnType() {
			return this.operator.getReturnType(this.left.getReturnType(), this.right.getReturnType());
		}
		MsObject evaluate(MsDocument data, MarkupScriptExecutionContext context) {
			MsObject left = this.left.evaluate(data, context);
			MsObject right = this.right.evaluate(data, context);
			MsObject result = this.operator.applyTo(left, right);
			if ((this.left instanceof VariableReference) && (this.operator instanceof AssigningOperator))
				context.setVariable(((VariableReference) this.left).varName, result);
			return result;
		}
		void printString(String indent) {
			if (this.operator instanceof BooleanNot) {
				System.out.println(indent + "!");
				this.right.printString(indent + "  ");
			}
			else if (this.operator instanceof NumberMinus) {
				System.out.println(indent + "-");
				this.right.printString(indent + "  ");
			}
			else if (this.operator instanceof Assign)
				this.right.printString(indent + "  ");
			else if (this.operator instanceof AssigningOperator) {
				System.out.println(indent + "  " + this.operator.name);
				this.right.printString(indent + "  ");
			}
			else {
				System.out.println(indent + "(");
				this.left.printString(indent + "  ");
				System.out.println(indent + "  " + this.operator.name);
				this.right.printString(indent + "  ");
				System.out.println(indent + ")");
			}
		}
		void styleCode(StyledDocument sd) {
			this.left.styleCode(sd);
			this.operator.styleCode(sd);
			this.right.styleCode(sd);
		}
	}
	
	private static class FunctionCall extends Expression {
		final String funcNamespace;
		final String funcName;
		Function function = null;
		final Expression[] args;
		FunctionCall(int start, int end, String funcNamespace, String funcName, Expression[] args) {
			super("functioncall", start, end);
			this.funcNamespace = funcNamespace;
			this.funcName = funcName;
			this.args = args;
		}
		String getReturnType() {
			return this.function.getReturnType();
		}
		MsObject evaluate(MsDocument data, MarkupScriptExecutionContext context) {
			if (this.function == null)
				throw new RuntimeException("Unresolved function name '" + this.funcName + "' at " + this.start);
			MarkupScriptExecutionContext funcContext = new MarkupScriptExecutionContext(context.script); // a function doesn't blend into the scope of its call
			for (int a = 0; a < this.function.args.length; a++) {
				MsObject value = this.args[a].evaluate(data, context);
				funcContext.declareVariable(this.function.args[a].varName, this.function.args[a].varType);
				funcContext.setVariable(this.function.args[a].varName, value);
				//	TODO do type conversion here? do we need that at all?
			}
			return this.function.execute(data, funcContext);
		}
		void printString(String indent) {
			if (this.args.length == 0)
				System.out.println(indent + this.funcName + "()");
			else {
				System.out.println(indent + this.funcName + "(");
				for (int a = 0; a < this.args.length; a++) {
					if (a != 0)
						System.out.println(indent + "  ,");
					this.args[a].printString(indent + "  ");
				}
				System.out.println(indent + ")");
			}
		}
		void styleCode(StyledDocument sd) {
			//	TODO style this
			for (int a = 0; a < this.args.length; a++)
				this.args[a].styleCode(sd);
		}
	}
	
	private static abstract class ExecutableSequence extends Executable {
		ArrayList executables = new ArrayList();
//		ExecutableSequence(int start, int end) {
//			this("block", start, end);
//		}
		ExecutableSequence(String type, int start, int end) {
			super(type, start, end);
		}
		void printString(String indent) {
			for (int e = 0; e < this.executables.size(); e++)
				((Executable) this.executables.get(e)).printString(indent);
		}
		void styleCode(StyledDocument sd) {
			for (int e = 0; e < this.executables.size(); e++)
				((Executable) this.executables.get(e)).styleCode(sd);
		}
	}
	
	private static class Function extends ExecutableSequence {
		final String name;
		final VariableDeclaration[] args;
		final String returnType;
		final int returnTypeStart;
		Comment documentation; // this is set when adding to script object
		Function(int start, int end, String name, VariableDeclaration[] args, String returnType, int returnTypeStart) {
			super("function", start, end);
			this.name = name;
			this.args = args;
			this.returnType = returnType;
			this.returnTypeStart = returnTypeStart;
		}
		String getReturnType() {
			return this.returnType;
		}
		MsObject execute(MsDocument data, MarkupScriptExecutionContext context) {
			for (int e = 0; e < this.executables.size(); e++) {
				MsObject result = ((Executable) this.executables.get(e)).execute(data, context);
				if (result instanceof ReturnValue)
					return ((ReturnValue) result).value;
			}
			return null;
		}
		void printString(String indent) {
			if (this.args.length == 0)
				System.out.println(indent + "function " + this.name + "() : " + this.returnType + " {");
			else {
				System.out.println(indent + "function " + this.name + "(");
				for (int a = 0; a < this.args.length; a++) {
					if (a != 0)
						System.out.println(indent + "  ,");
					this.args[a].printString(indent + "  ");
				}
				System.out.println(indent + ") : " + this.returnType + " {");
			}
			super.printString(indent + "  ");
			System.out.println(indent + "}");
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, "function".length(), STYLE_KEYWORD, true);
			for (int a = 0; a < this.args.length; a++)
				this.args[a].styleCode(sd);
			if (this.returnTypeStart != -1)
				sd.setCharacterAttributes(this.returnTypeStart, this.returnType.length(), STYLE_KEYWORD, true);
			super.styleCode(sd);
		}
	}
	
	private static class IfBlock extends ExecutableSequence {
		final Expression test;
		final Executable elseBlock;
		final int elseStart; // starting position of else block in source script
//		IfBlock(int start, int end, Expression test) {
//			this(start, end, test, -1, null);
//		}
		IfBlock(int start, int end, Expression test, int elseStart, Executable elseBlock) {
			super("if", start, end);
			this.test = test;
			this.elseStart = elseStart;
			this.elseBlock = elseBlock;
		}
		MsObject execute(MsDocument data, MarkupScriptExecutionContext context) {
			MarkupScriptExecutionContext ifContext = new MarkupScriptExecutionContext(context.script, context);
			if (this.test.evaluate(data, context).asBoolean().getNativeBoolean().booleanValue()) {
				for (int e = 0; e < this.executables.size(); e++) {
					MsObject result = ((Executable) this.executables.get(e)).execute(data, ifContext);
					if (result instanceof ReturnValue)
						return result; // return from function call ==> we're out of here (return value will be un-wrapped in ancestor function)
					else if (result == BREAK_VALUE)
						return BREAK_VALUE; // break ancestor loop ==> we're done here
					else if (result == CONTINUE_VALUE)
						return CONTINUE_VALUE; // jump to end of ancestor loop body
				}
				return null;
			}
			else if (this.elseBlock == null)
				return null;
			else return this.elseBlock.execute(data, context);
		}
		void printString(String indent) {
			System.out.println(indent + "if (");
			this.test.printString(indent + "  ");
			System.out.println(indent + ") {");
			super.printString(indent + "  ");
			System.out.println(indent + "}");
			if (this.elseBlock != null) {
				System.out.println(indent + "else");
				this.elseBlock.printString(indent);
			}
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, "if".length(), STYLE_KEYWORD, true);
			this.test.styleCode(sd);
			super.styleCode(sd);
			if (this.elseStart != -1)
				sd.setCharacterAttributes(this.elseStart, "else".length(), STYLE_KEYWORD, true);
			if (this.elseBlock != null)
				this.elseBlock.styleCode(sd);
		}
	}
	
	private static class ForLoop extends ExecutableSequence {
		final VariableDeclaration initializer;
		final Expression test;
		final VariableAssignment postBody;
		final int setStart;
		final Expression setRef;
		ForLoop(int start, int end, VariableDeclaration initializer, Expression test, VariableAssignment postBody) {
			super("for", start, end);
			this.initializer = initializer;
			this.test = test;
			this.postBody = postBody;
			this.setStart = -1;
			this.setRef = null;
		}
		ForLoop(int start, int end, VariableDeclaration initializer, int setStart, Expression setRef) {
			super("for", start, end);
			this.initializer = initializer;
			this.test = null;
			this.postBody = null;
			this.setStart = setStart;
			this.setRef = setRef;
		}
		MsObject execute(MsDocument data, MarkupScriptExecutionContext context) {
			MarkupScriptExecutionContext loopContext = new MarkupScriptExecutionContext(context.script, context);
			if (this.initializer != null)
				this.initializer.execute(data, loopContext);
			if (this.setRef == null) {
				while ((this.test == null) || this.test.evaluate(data, loopContext).asBoolean().getValue()) {
					for (int e = 0; e < this.executables.size(); e++) {
						MsObject result = ((Executable) this.executables.get(e)).execute(data, loopContext);
						if (result instanceof ReturnValue)
							return result; // return from function call ==> we're out of here (return value will be un-wrapped in ancestor function)
						else if (result == BREAK_VALUE)
							return null; // break loop ==> we're done here
						else if (result == CONTINUE_VALUE)
							e = this.executables.size(); // jump to end of loop body
					}
					if (this.postBody != null)
						this.postBody.execute(data, loopContext);
				}
				return null;
			}
			else {
				ArrayList setVals = new ArrayList();
				MsObject setObj = this.setRef.evaluate(data, context);
				if (setObj instanceof MsArray)
					setVals.addAll(((MsArray) setObj).getNativeList());
				else if (setObj instanceof MsMap)
					setVals.addAll(((MsMap) setObj).getValueKeys().getNativeList());
				//	TODO wrap keys as MsString objects
				else throw new RuntimeException("Cannot iterate over " + setObj);
				for (int v = 0; v < setVals.size(); v++) {
					loopContext.setVariable(this.initializer.varName, ((MsObject) setVals.get(v)));
					for (int e = 0; e < this.executables.size(); e++) {
						MsObject result = ((Executable) this.executables.get(e)).execute(data, context);
						if (result instanceof ReturnValue)
							return result; // return from function call ==> we're out of here (return value will be un-wrapped in ancestor function)
						else if (result == BREAK_VALUE)
							return null; // break loop ==> we're done here
						else if (result == CONTINUE_VALUE)
							e = this.executables.size(); // jump to end of loop body
					}
				}
				return null;
			}
		}
		void printString(String indent) {
			System.out.println(indent + "for (");
			if (this.setRef == null) {
				if (this.initializer != null)
					this.initializer.printString(indent + "  ");
				System.out.println(indent + ";");
				if (this.test != null)
					this.test.printString(indent + "  ");
				System.out.println(indent + ";");
				if (this.postBody != null)
					this.postBody.printString(indent + "  ");
			}
			else {
				if (this.initializer != null)
					this.initializer.printString(indent + "  ");
				System.out.println(indent + "in");
				if (this.setRef != null)
					this.setRef.printString(indent + "  ");
			}
			System.out.println(indent + ") {");
			super.printString(indent + "  ");
			System.out.println(indent + "}");
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, "for".length(), STYLE_KEYWORD, true);
			if (this.initializer != null)
				this.initializer.styleCode(sd);
			if (this.test != null)
				this.test.styleCode(sd);
			if (this.postBody != null)
				this.postBody.styleCode(sd);
			if (this.setStart != -1)
				sd.setCharacterAttributes(this.setStart, "in".length(), STYLE_KEYWORD, true);
			if (this.setRef != null)
				this.setRef.styleCode(sd);
			super.styleCode(sd);
		}
	}
	
	private static class WhileLoop extends ExecutableSequence {
		final Expression test;
		WhileLoop(int start, int end, Expression test) {
			super("while", start, end);
			this.test = test;
		}
		MsObject execute(MsDocument data, MarkupScriptExecutionContext context) {
			MarkupScriptExecutionContext loopContext = new MarkupScriptExecutionContext(context.script, context);
			while ((this.test == null) || this.test.evaluate(data, loopContext).asBoolean().getValue())
				for (int e = 0; e < this.executables.size(); e++) {
					MsObject result = ((Executable) this.executables.get(e)).execute(data, loopContext);
					if (result instanceof ReturnValue)
						return result; // return from function call ==> we're out of here
					else if (result == BREAK_VALUE)
						return null; // break loop ==> we're done here
					else if (result == CONTINUE_VALUE)
						e = this.executables.size(); // jump to end of loop body
				}
			return null;
		}
		void printString(String indent) {
			System.out.println(indent + "while (");
			this.test.printString(indent + "  ");
			System.out.println(indent + ") {");
			super.printString(indent + "  ");
			System.out.println(indent + "}");
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, "while".length(), STYLE_KEYWORD, true);
			if (this.test != null)
				this.test.styleCode(sd);
			super.styleCode(sd);
		}
	}
	
	private static class ReturnValue implements MsObject {
		final MsObject value;
		ReturnValue(MsObject value) {
			this.value = value;
		}
		public MsBoolean asBoolean() {
			return null;
		}
		public MsNumber asNumber() {
			return null;
		}
		public MsString asString() {
			return null;
		}
		public Object getNativeObject() {
			return null;
		}
	}
	private static final MsObject BREAK_VALUE = MarkupScriptTypes.wrapString("break");
	private static final MsObject CONTINUE_VALUE = MarkupScriptTypes.wrapString("continue");
	
	private static class Empty extends Executable {
		Empty(int start) {
			super("empty", start, start);
		}
		MsObject execute(MsDocument data, MarkupScriptExecutionContext context) {
			return null;
		}
		void printString(String indent) {}
		void styleCode(StyledDocument sd) {}
	}
	
	private static class Break extends Executable {
		Break(int start) {
			super("break", start, (start + "break".length()));
		}
		MsObject execute(MsDocument data, MarkupScriptExecutionContext context) {
			return BREAK_VALUE;
		}
		void printString(String indent) {
			System.out.println(indent + "break");
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, "break".length(), STYLE_KEYWORD, true);
		}
	}
	
	private static class Continue extends Executable {
		Continue(int start) {
			super("continue", start, (start + "continue".length()));
		}
		MsObject execute(MsDocument data, MarkupScriptExecutionContext context) {
			return CONTINUE_VALUE;
		}
		void printString(String indent) {
			System.out.println(indent + "continue");
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, "continue".length(), STYLE_KEYWORD, true);
		}
	}
	
	private static class Return extends Executable {
		final Expression value;
		Return(int start) {
			this(start, null);
		}
		Return(int start, Expression value) {
			super("return", start, (start + "return".length()));
			this.value = value;
		}
		MsObject execute(MsDocument data, MarkupScriptExecutionContext context) {
			return new ReturnValue((this.value == null) ? null : this.value.evaluate(data, context));
		}
		void printString(String indent) {
			System.out.println(indent + "return");
			if (this.value != null)
				this.value.printString(indent + "  ");
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, "return".length(), STYLE_KEYWORD, true);
			if (this.value != null)
				this.value.styleCode(sd);
		}
	}
	
	private static final int UNARY_PRECEDENCE = 0;
	private static final int MULTIPLY_PRECEDENCE = 1;
	private static final int ADD_PRECEDENCE = 2;
	private static final int COMPARISON_PRECEDENCE = 3;
	private static final int AND_PRECEDENCE = 4;
	private static final int OR_PRECEDENCE = 5;
	private static final int ASSIGN_PRECEDENCE = 6;
	
	private static class BooleanNot extends Operator {
		BooleanNot(int start, int end) {
			super(start, end, "!", UNARY_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "boolean";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			return MarkupScriptTypes.wrapBoolean((right == null) || !right.asBoolean().getValue());
		}
	}
	
	private static class NumberMinus extends Operator {
		NumberMinus(int start, int end) {
			super(start, end, "-", UNARY_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return "number".equals(leftArgType);
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "number";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			return MarkupScriptTypes.wrapDouble(-right.asNumber().getValue());
		}
	}
	
	private static class Multiply extends Operator {
		Multiply(int start, int end) {
			super(start, end, "*", MULTIPLY_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return ("number".equals(leftArgType) && "number".equals(leftArgType));
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "number";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			if ((left == null) || (right == null))
				return MarkupScriptTypes.wrapInteger(0);
			else return MarkupScriptTypes.wrapDouble(left.asNumber().getValue() * right.asNumber().getValue());
		}
	}
	
	private static class Divide extends Operator {
		Divide(int start, int end) {
			super(start, end, "div", MULTIPLY_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return ("number".equals(leftArgType) && "number".equals(leftArgType));
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "number";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			if ((left == null) || (right == null))
				return MarkupScriptTypes.wrapInteger(0);
			else return MarkupScriptTypes.wrapDouble(left.asNumber().getValue() / right.asNumber().getValue());
		}
	}
	
	private static class Modulo extends Operator {
		Modulo(int start, int end) {
			super(start, end, "mod", MULTIPLY_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return ("number".equals(leftArgType) && "number".equals(leftArgType));
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "number";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			if ((left == null) || (right == null))
				return MarkupScriptTypes.wrapInteger(0);
			else return MarkupScriptTypes.wrapDouble(left.asNumber().getValue() % right.asNumber().getValue());
		}
	}
	
	private static class Add extends Operator {
		Add(int start, int end) {
			super(start, end, "+", ADD_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			if ("number".equals(leftArgType) && "number".equals(rightArgType))
				return "number";
			else if ("array".equals(leftArgType))
				return "array";
			else if ("map".equals(leftArgType))
				return "map";
			else if ("map".equals(leftArgType))
				return "map";
			else return "string";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			if (left == null)
				return right; // adding anything to null returns whatever was added
			else if (right == null)
				return left; // adding null to anything returns whatever it was added to
			String leftType = MarkupScriptTypes.getObjectType(left);
			String rightType = MarkupScriptTypes.getObjectType(right);
			if ("number".equals(leftType) && "number".equals(rightType))
				return MarkupScriptTypes.wrapDouble(left.asNumber().getValue() + right.asNumber().getValue());
			else if ("array".equals(leftType) && "array".equals(rightType)) {
				((MsArray) left).addObjects((MsArray) right);
				return left;
			}
			else if ("array".equals(leftType)) {
				((MsArray) left).addObject(right);
				return left;
			}
			else if ("map".equals(leftType) && "map".equals(rightType)) {
				((MsMap) left).setValues(((MsMap) right), MarkupScriptTypes.wrapBoolean(false));
				return left;
			}
			else if ("map".equals(leftType) && "array".equals(rightType)) {
				List array = ((MsArray) right).getNativeList();
				for (int a = 0; a < (array.size() - 1); a += 2) {
					MsString key = MarkupScriptTypes.wrapObject(array.get(a)).asString();
					MsObject value = MarkupScriptTypes.wrapObject(array.get(a+1));
					((MsMap) left).setValue(key, value);
				}
				return left;
			}
			else if ("map".equals(leftType)) {
				((MsMap) left).setValue(right.asString(), MarkupScriptTypes.wrapBoolean(true));
				return left;
			}
			else return MarkupScriptTypes.wrapString(left.asString().getNativeString().toString() + right.asString().getNativeString().toString());
		}
	}
	
	private static class Subtract extends Operator {
		Subtract(int start, int end) {
			super(start, end, "-", ADD_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return (false
					|| "boolean".equals(leftArgType) // AND right argument with left boolean
					|| ("number".equals(leftArgType) && "number".equals(leftArgType)) // subtract numbers
					|| "array".equals(leftArgType) // remove right argument from left array
					|| "map".equals(leftArgType) // remove right key from left map
				);
		}
		String getReturnType(String leftArgType, String rightArgType) {
			if ("number".equals(leftArgType) && "number".equals(rightArgType))
				return "number";
			else if ("array".equals(leftArgType))
				return "array";
			else if ("map".equals(leftArgType) && "array".equals(rightArgType))
				return "map";
			else return null;
		}
		MsObject applyTo(MsObject left, MsObject right) {
			if (left == null)
				return null; // subtracting whatever from null doesn't change null 
			else if (right == null)
				return left; // subtracting null from whatever doesn't change whatever
			String leftType = MarkupScriptTypes.getObjectType(left);
			String rightType = MarkupScriptTypes.getObjectType(right);
			if ("number".equals(leftType) && "number".equals(rightType))
				return MarkupScriptTypes.wrapDouble(left.asNumber().getValue() - right.asNumber().getValue());
			else if ("array".equals(leftType)) {
				((MsArray) left).removeObject(right);
				return left;
			}
			else if ("map".equals(leftType) && "map".equals(rightType)) {
				((MsMap) left).removeValues((MsMap) right);
				return left;
			}
			else if ("map".equals(leftType) && "array".equals(rightType)) {
				((MsMap) left).removeKeys((MsArray) right);
				return left;
			}
			else if ("map".equals(leftType)) {
				((MsMap) left).removeValue(right.asString());
				return left;
			}
			else throw new IllegalArgumentException("Cannot subtract " + right + " from " + left);
			//	TODO devise dedicated exception for this case
		}
	}
	
	private static class Equals extends Operator {
		Equals(int start, int end) {
			super(start, end, "==", COMPARISON_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "boolean";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			return MarkupScriptTypes.wrapBoolean(MarkupScriptTypes.equals(left, right));
		}
	}
	
	private static class NotEquals extends Operator {
		NotEquals(int start, int end) {
			super(start, end, "!=", COMPARISON_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "boolean";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			return MarkupScriptTypes.wrapBoolean(!MarkupScriptTypes.equals(left, right));
		}
	}
	
	private static class Less extends Operator {
		Less(int start, int end) {
			super(start, end, "<", COMPARISON_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "boolean";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			return MarkupScriptTypes.wrapBoolean(MarkupScriptTypes.compare(left, right) < 0);
		}
	}
	
	private static class LessEquals extends Operator {
		LessEquals(int start, int end) {
			super(start, end, "<=", COMPARISON_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "boolean";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			return MarkupScriptTypes.wrapBoolean(MarkupScriptTypes.compare(left, right) <= 0);
		}
	}
	
	private static class GreaterEquals extends Operator {
		GreaterEquals(int start, int end) {
			super(start, end, ">=", COMPARISON_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "boolean";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			return MarkupScriptTypes.wrapBoolean(MarkupScriptTypes.compare(left, right) >= 0);
		}
	}
	
	private static class Greater extends Operator {
		Greater(int start, int end) {
			super(start, end, ">", COMPARISON_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "boolean";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			return MarkupScriptTypes.wrapBoolean(MarkupScriptTypes.compare(left, right) > 0);
		}
	}
	
	private static class And extends Operator {
		And(int start, int end) {
			super(start, end, "&", AND_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "boolean";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().getValue());
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().getValue());
			return MarkupScriptTypes.wrapBoolean(leftBoolean && rightBoolean);
		}
	}
	
	private static class Nand extends Operator {
		Nand(int start, int end) {
			super(start, end, "!&", AND_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "boolean";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().getValue());
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().getValue());
			return MarkupScriptTypes.wrapBoolean(!leftBoolean || !rightBoolean);
		}
	}
	
	private static class Or extends Operator {
		Or(int start, int end) {
			super(start, end, "|", OR_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "boolean";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().getValue());
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().getValue());
			return MarkupScriptTypes.wrapBoolean(leftBoolean || rightBoolean);
		}
	}
	
	private static class Nor extends Operator {
		Nor(int start, int end) {
			super(start, end, "!|", OR_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return "boolean";
		}
		MsObject applyTo(MsObject left, MsObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().getValue());
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().getValue());
			return MarkupScriptTypes.wrapBoolean(!leftBoolean && !rightBoolean);
		}
	}
	
	private static class Assign extends AssigningOperator {
		Assign(int start, int end) {
			super(start, end, "=");
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return true;
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return rightArgType;
		}
		MsObject applyTo(MsObject left, MsObject right) {
			return right;
		}
	}
	
	private static class AddAssign extends AssigningOperator {
		AddAssign(int start, int end) {
			super(start, end, "+=");
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return (false
				|| "boolean".equals(leftArgType) // OR right argument with left boolean
				|| ("number".equals(leftArgType) && "number".equals(leftArgType)) // add numbers
				|| "string".equals(leftArgType) // append right argument to left string
				|| ("array".equals(leftArgType) && "array".equals(leftArgType)) // copy content of right array into left map
				|| "array".equals(leftArgType) // append right argument to left array
				|| ("map".equals(leftArgType) && "array".equals(leftArgType)) // add right array as key/value pairs to left map
				|| ("map".equals(leftArgType) && "map".equals(leftArgType)) // copy key/value pairs from right map into left map
				|| "map".equals(leftArgType) // add right key to left map with value true
			);
		}
		String getReturnType(String leftArgType, String rightArgType) {
			return leftArgType;
		}
		MsObject applyTo(MsObject left, MsObject right) {
			if (left == null)
				return right; // adding anything to null returns whatever was added
			else if (right == null)
				return left; // adding null to anything returns whatever it was added to
			String leftType = MarkupScriptTypes.getObjectType(left);
			String rightType = MarkupScriptTypes.getObjectType(right);
			//	TODO observe type inheritance
			if ("boolean".equals(leftType))
				return MarkupScriptTypes.wrapBoolean(left.asBoolean().getValue() || right.asBoolean().getValue());
			else if ("number".equals(leftType) && "number".equals(rightType))
				return MarkupScriptTypes.wrapDouble(left.asNumber().getValue() + right.asNumber().getValue());
			else if ("array".equals(leftType) && "array".equals(rightType)) {
				((MsArray) left).addObjects(((MsArray) right));
				return left;
			}
			else if ("array".equals(leftType)) {
				((MsArray) left).addObject(right);
				return left;
			}
			else if ("map".equals(leftType) && "map".equals(rightType)) {
				((MsMap) left).setValues(((MsMap) right), MarkupScriptTypes.wrapBoolean(false));
				return left;
			}
			else if ("map".equals(leftType) && "array".equals(rightType)) {
				List array = ((MsArray) right).getNativeList();
				for (int a = 0; a < (array.size() - 1); a += 2) {
					MsString key = MarkupScriptTypes.wrapObject(array.get(a)).asString();
					MsObject value = MarkupScriptTypes.wrapObject(array.get(a+1));
					((MsMap) left).setValue(key, value);
				}
				return left;
			}
			else if ("map".equals(leftType)) {
				((MsMap) left).setValue(right.asString(), MarkupScriptTypes.wrapBoolean(true));
				return left;
			}
			else return MarkupScriptTypes.wrapString(left.asString().getValue().toString() + right.asString().getValue().toString());
		}
	}
	
	private static class SubtractAssign extends AssigningOperator {
		SubtractAssign(int start, int end) {
			super(start, end, "-=");
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return (false
					|| "boolean".equals(leftArgType) // AND right argument with left boolean
					|| ("number".equals(leftArgType) && "number".equals(leftArgType)) // subtract numbers
					|| "array".equals(leftArgType) // remove right argument from left array
					|| "map".equals(leftArgType) // remove right key from left map
				);
		}
		String getReturnType(String leftArgType, String rightArgType) {
			if ("number".equals(leftArgType) && "number".equals(rightArgType))
				return "number";
			else if ("array".equals(leftArgType))
				return "array";
			else if ("map".equals(leftArgType) && "array".equals(rightArgType))
				return "map";
			else return null;
		}
		MsObject applyTo(MsObject left, MsObject right) {
			if (left == null)
				return null; // subtracting whatever from null doesn't change null 
			else if (right == null)
				return left; // subtracting null from whatever doesn't change whatever
			String leftType = MarkupScriptTypes.getObjectType(left);
			String rightType = MarkupScriptTypes.getObjectType(right);
			if ("number".equals(leftType) && "number".equals(rightType))
				return MarkupScriptTypes.wrapDouble(left.asNumber().getValue() - right.asNumber().getValue());
			else if ("array".equals(leftType)) {
				((MsArray) left).removeObject(right);
				return left;
			}
			else if ("map".equals(leftType) && "map".equals(rightType)) {
				((MsMap) left).removeValues((MsMap) right);
				return left;
			}
			else if ("map".equals(leftType) && "array".equals(rightType)) {
				((MsMap) left).removeKeys((MsArray) right);
				return left;
			}
			else if ("map".equals(leftType)) {
				((MsMap) left).removeValue(right.asString());
				return left;
			}
			else throw new IllegalArgumentException("Cannot subtract " + rightType + " from " + leftType);
			//	TODO devise dedicated exception for this case
		}
	}
	
	private static class MultiplyAssign extends AssigningOperator {
		MultiplyAssign(int start, int end) {
			super(start, end, "*=");
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return ("number".equals(leftArgType) && "number".equals(leftArgType));
		}
		MsObject applyTo(MsObject left, MsObject right) {
			if ((left == null) || (right == null))
				return MarkupScriptTypes.wrapInteger(0);
			else return MarkupScriptTypes.wrapDouble(left.asNumber().getValue() * right.asNumber().getValue());
		}
	}
	
	private static class DivideAssign extends AssigningOperator {
		DivideAssign(int start, int end) {
			super(start, end, "/=");
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return ("number".equals(leftArgType) && "number".equals(leftArgType));
		}
		MsObject applyTo(MsObject left, MsObject right) {
			if ((left == null) || (right == null))
				return MarkupScriptTypes.wrapInteger(0);
			else return MarkupScriptTypes.wrapDouble(left.asNumber().getValue() / right.asNumber().getValue());
		}
	}
	
	private static class AndAssign extends AssigningOperator {
		AndAssign(int start, int end) {
			super(start, end, "&=");
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return "boolean".equals(leftArgType);
		}
		MsObject applyTo(MsObject left, MsObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().getValue());
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().getValue());
			return MarkupScriptTypes.wrapBoolean(leftBoolean && rightBoolean);
		}
	}
	
	private static class OrAssign extends AssigningOperator {
		OrAssign(int start, int end) {
			super(start, end, "|=");
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return "boolean".equals(leftArgType);
		}
		MsObject applyTo(MsObject left, MsObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().getValue());
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().getValue());
			return MarkupScriptTypes.wrapBoolean(leftBoolean || rightBoolean);
		}
	}
	
	private static class MarkupScriptParseReader extends PeekReader {
		private MarkupScript scipt;
		private int position = 0;
		MarkupScriptParseReader(Reader in, MarkupScript script) throws IOException {
			super(in, 1024); // let's keep some lookahead for parsing error recovery
			this.scipt = script;
		}
		public int read() throws IOException {
			int r = super.read();
			if (r != -1)
				this.position++;
			return r;
		}
		public int read(char[] cbuf, int off, int len) throws IOException {
			int r = super.read(cbuf, off, len);
			if (r != -1)
				this.position += r;
			return r;
		}
		public long skip(long n) throws IOException {
			long s = super.skip(n);
			if (s != -1)
				this.position += ((int) s);
			return s;
		}
		public int skipSpace() throws IOException {
//			int s = super.skipSpace();
////			super class uses peek() and read() to get rid of spaces, don't count them twice
////			if (s != -1)
////				this.position += s;
//			return s;
			int start = this.position;
			while (this.peek() != -1) {
				super.skipSpace();
				if (this.startsWith("//", true))
					this.scipt.addComment(cropLineComment(this));
				else if (this.startsWith("/*", true))
					this.scipt.addComment(cropBlockComment(this, this.scipt));
				else break;
			}
			return (this.position - start);
		}
		int getPosition() {
			return this.position;
		}
	}
	
	private static Comment cropLineComment(MarkupScriptParseReader pr) throws IOException {
		int start = pr.getPosition();
		StringBuffer textBuffer = new StringBuffer();
		textBuffer.append((char) pr.read());
		textBuffer.append((char) pr.read());
		while (pr.peek() != -1) {
			if (pr.peek() == '\r') {
				pr.read();
				if (pr.peek() == '\n')
					pr.read();
				break;
			}
			else if (pr.peek() == '\n') {
				pr.read();
				break;
			}
			else textBuffer.append((char) pr.read());
		}
		return new Comment(start, pr.getPosition(), textBuffer.toString());
	}
	
	private static Comment cropBlockComment(MarkupScriptParseReader pr, MarkupScript script) throws IOException {
		int start = pr.getPosition();
		StringBuffer textBuffer = new StringBuffer();
		textBuffer.append((char) pr.read());
		textBuffer.append((char) pr.read());
		while (pr.peek() != -1) {
			if (pr.startsWith("*/", false)) {
				textBuffer.append((char) pr.read());
				textBuffer.append((char) pr.read());
				break;
			}
			else textBuffer.append((char) pr.read());
		}
		String text = textBuffer.toString();
		if (!text.endsWith("*/"))
			script.addParseException(new MissingCharactersException(pr.getPosition(), "", "*/"));
		return new Comment(start, pr.getPosition(), text);
	}
	
	public static MarkupScript parse(Reader in) throws IOException {
		MarkupScript ms = new MarkupScript();
		MarkupScriptParseReader pr = new MarkupScriptParseReader(in, ms);
		MarkupScriptParse parse = new MarkupScriptParse(ms);
		while (pr.peek() != -1) {
			cropNext(pr, ms, parse);
			pr.skipSpace();
		}
		ms.assortDocComments();
		ms.resolveImports();
		ms.bindFunctionsAndConstants();
		//	TODO resolve imports (recording errors for non-resolving ones)
		//	TODO set types for references to imported constants and calls to imported functions (recording errors for non-resolving ones)
		return ms;
	}
	
	private static void skipUnexpectedChars(MarkupScriptParseReader pr, MarkupScriptParse parse, String expected) throws IOException {
		while (pr.peek() != -1) {
			char ch = ((char) pr.peek());
			if (expected.indexOf(ch) != -1)
				break;
			else if (Character.isLetter(ch) || (ch == '_')) {
				int start = pr.getPosition();
				parse.recordParseException(new UnexpectedCharactersException(start, cropAlphanumeric(pr, ":_", expected)));
				pr.skipSpace();
			}
			else if (Character.isDigit(ch)) {
				int start = pr.getPosition();
				parse.recordParseException(new UnexpectedCharactersException(start, cropNumeric(pr, ".", expected)));
				pr.skipSpace();
			}
			else if (ch <= ' ') {
				int start = pr.getPosition();
				pr.skipSpace();
				parse.recordParseException(new UnexpectedCharactersException(start, ' '));
			}
			else throw new UnexpectedCharactersException(pr.getPosition(), ((char) pr.read()));
		}
	}
	
	private static String cropName(MarkupScriptParseReader pr, MarkupScriptParse parse, boolean allowNamespacePrefix, String nonAlphaNumericChars) throws IOException {
		pr.skipSpace();
		StringBuffer name = new StringBuffer();
		while (pr.peek() != -1) {
			char ch = ((char) pr.peek());
			if (ch <= ' ')
				break;
			else if (('a' <= ch) && (ch <= 'z'))
				name.append(ch);
			else if (('A' <= ch) && (ch <= 'Z'))
				name.append(ch);
			else if ('_' == ch)
				name.append(ch);
			else if ((name.length() != 0) && (nonAlphaNumericChars != null) && (nonAlphaNumericChars.indexOf(ch) != -1))
				name.append(ch);
			else if ((name.length() != 0) && ('0' <= ch) && (ch <= '9'))
				name.append(ch);
			else if ((name.length() != 0) && (ch == ':') && allowNamespacePrefix) {
				name.append(ch);
				allowNamespacePrefix = false;
			}
			else if (name.length() == 0)
				parse.recordParseException(new UnexpectedCharactersException(pr.getPosition(), ch));
			else break;
			pr.read(); // consume last appended (or reported as unexpected) character
		}
		System.out.println("Name: '" + name + "'");
		return name.toString();
	}
	
	private static String cropAlphanumeric(MarkupScriptParseReader pr, String allowedPunctuation, String stopAt) throws IOException {
		StringBuffer alphanumeric = new StringBuffer();
		while (pr.peek() != -1) {
			char ch = ((char) pr.peek());
			if (stopAt.indexOf(ch) != -1)
				break;
			else if (Character.isLetterOrDigit(ch))
				alphanumeric.append((char) pr.read());
			else if (allowedPunctuation.indexOf(ch) != -1)
				alphanumeric.append((char) pr.read());
			else break;
		}
		return alphanumeric.toString();
	}
	
	private static String cropAlpha(MarkupScriptParseReader pr, String allowedPunctuation) throws IOException {
		StringBuffer alpha = new StringBuffer();
		while (pr.peek() != -1) {
			char ch = ((char) pr.peek());
			if (('a' <= ch) && (ch <= 'z'))
				alpha.append((char) pr.read());
			else if (('A' <= ch) && (ch <= 'Z'))
				alpha.append(ch);
			else if (allowedPunctuation.indexOf(ch) != -1)
				alpha.append((char) pr.read());
			else break;
		}
		return alpha.toString();
	}
	
	private static String cropNumeric(MarkupScriptParseReader pr, String allowedPunctuation, String stopAt) throws IOException {
		StringBuffer numeric = new StringBuffer();
		while (pr.peek() != -1) {
			char ch = ((char) pr.peek());
			if (stopAt.indexOf(ch) != -1)
				break;
			else if (Character.isDigit(ch))
				numeric.append((char) pr.read());
			else if (allowedPunctuation.indexOf(ch) != -1)
				numeric.append((char) pr.read());
			else break;
		}
		return numeric.toString();
	}
	
	private static void cropNext(MarkupScriptParseReader pr, MarkupScript ms, MarkupScriptParse parse) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		String keyWord = getAlpha(pr, "_:", "");
		if ("import".equals(keyWord)) {
			pr.skip(keyWord.length());
			ms.addImport(cropImport(pr, parse, start));
		}
		//	TODO enforce imports standing at top of file?
		else if ("function".equals(keyWord)) {
			pr.skip(keyWord.length());
			ms.addFunction(cropFunction(pr, parse, start));
		}
		else ms.addExecutable(cropExecutable(pr, parse));
	}
	
	private static String getAlpha(MarkupScriptParseReader pr, String allowedPunctuation, String stopAt) throws IOException {
		StringBuffer alpha = new StringBuffer();
		for (int l = 0; pr.peek(l) != -1; l++) {
			char ch = ((char) pr.peek(l));
			if (('a' <= ch) && (ch <= 'z'))
				alpha.append(ch);
			else if (('A' <= ch) && (ch <= 'Z'))
				alpha.append(ch);
			else if (allowedPunctuation.indexOf(ch) != -1)
				alpha.append(ch);
			else break;
		}
		System.out.println("Alpha is " + alpha);
		return alpha.toString();
	}
	
	private static Import cropImport(MarkupScriptParseReader pr, MarkupScriptParse parse, int start) throws IOException {
		pr.skipSpace();
		String name = cropName(pr, parse, false, "_.");
		pr.skipSpace();
		String namespace = null;
		int nsStart = -1;
		if (pr.startsWith("as ", true)) {
			nsStart = pr.getPosition();
			pr.skip("as ".length());
			pr.skipSpace();
			namespace = cropName(pr, parse, false, "");
		}
		pr.skipSpace();
		if (namespace != null)
			parse.addImportNamespace(namespace);
		if (pr.peek() != ';')
			skipUnexpectedChars(pr, parse, ";");
		pr.read();
		return new Import(start, pr.getPosition(), name, nsStart, namespace);
	}
	
	private static Function cropFunction(MarkupScriptParseReader pr, MarkupScriptParse parse, int start) throws IOException {
		pr.skipSpace();
		String name = cropName(pr, parse, false, "_");
		//	TODO allow (variable) namespace here to modify local behavior of types
		pr.skipSpace();
		if (pr.peek() != '(')
			skipUnexpectedChars(pr, parse, "(");
		pr.read();
		
		//	read parameters
		parse.startScope(false);
		pr.skipSpace();
		ArrayList args = new ArrayList();
		while ((pr.peek() != ')') && (pr.peek() != -1)) {
			int argStart = pr.getPosition();
			String argType = cropType(pr, parse);
			pr.skipSpace();
			int argNameStart = pr.getPosition();
			if (pr.peek() != '$')
				skipUnexpectedChars(pr, parse, "$");
			pr.read();
			if (pr.peek() <= ' ') {
				int ueSpaceStart = pr.getPosition();
				parse.recordParseException(new UnexpectedCharactersException(ueSpaceStart, ' '));
				pr.skipSpace();
			}
			String argName = cropName(pr, parse, false, "_");
			pr.skipSpace();
			args.add(new VariableDeclaration(argStart, pr.getPosition(), argType, argName, argNameStart, null, false));
			parse.setScopeVarType(argName, argType);
			if (pr.peek() == ')')
				break; // end of argument list
			if (pr.peek() != ',')
				skipUnexpectedChars(pr, parse, "),");
			int commaStart = pr.getPosition();
			pr.read(); // consume comma
			pr.skipSpace();
			if (pr.peek() == ')') // just record that dangling comma
				parse.recordParseException(new UnexpectedCharactersException(commaStart, ','));
		}
		if (pr.peek() != ')')
			skipUnexpectedChars(pr, parse, "),");
		pr.read();
		pr.skipSpace();
		
		//	read return type
		if (!pr.startsWith("=>", true))
			skipUnexpectedChars(pr, parse, "=");
		pr.read();
		pr.read();
		int returnTypeStart = pr.getPosition();
		String returnType = cropType(pr, parse);
		
		//	read function body
		pr.skipSpace();
		if (pr.peek() != '{')
			skipUnexpectedChars(pr, parse, "{");
		pr.read();
		ArrayList execs = new ArrayList();
		while ((pr.peek() != '}') && (pr.peek() != -1)) {
			Executable exec = cropExecutable(pr, parse);
			execs.add(exec);
			pr.skipSpace();
		}
		
		//	make sure function is closed properly
		if (pr.peek() == '}') {
			pr.read();
			parse.endScope();
		}
		else throw new MissingCharactersException(pr.getPosition(), ((char) pr.peek()), '}');
		
		//	create function
		Function func = new Function(start, pr.getPosition(), name, ((VariableDeclaration[]) args.toArray(new VariableDeclaration[args.size()])), returnType, returnTypeStart);
		func.executables.addAll(execs);
		return func;
	}
	
	private static Executable cropExecutable(MarkupScriptParseReader pr, MarkupScriptParse parse) throws IOException {
		pr.skipSpace();
		Executable exec;
		if (pr.peek() == '$') {
			exec = cropVariableAssignment(pr, ";", parse);
			pr.skipSpace();
			if (pr.peek() != ';')
				skipUnexpectedChars(pr, parse, ";");
			pr.read();
			return exec;
		}
		//	TODOnot we might want to execute a function like '§<global>.<functionName>(...)' to modify variables passed from a calling script
		//	TODOnot model just like variable assignment below
		//	we're _NOT_ modifying any constants, cross invocation learning is to be implemented in custom system functions
		
		if (pr.peek() == ';') {
			pr.read();
			return new Empty(pr.getPosition());
		}
		if ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_".indexOf(pr.peek()) == -1)
			throw new UnexpectedCharactersException(pr.getPosition(), ((char) pr.peek()));
		
		int start = pr.getPosition();
		String keyWord = cropAlpha(pr, ":_");
		if ("".equals(keyWord))
			throw new UnexpectedCharactersException(pr.getPosition(), ((char) pr.peek()));
		pr.skipSpace(); // skip whatever space follows keyword
		
		boolean expectSemicolon = true;
		if ("for".equals(keyWord)) {
			exec = cropForLoop(pr, parse, start);
			expectSemicolon = false;
		}
		else if ("while".equals(keyWord)) {
			exec = cropWhileLoop(pr, parse, start);
			expectSemicolon = false;
		}
		else if ("if".equals(keyWord)) {
			exec = cropIfBlock(pr, parse, start);
			expectSemicolon = false;
		}
		else if (pr.peek() == ';')
			exec = new Empty(pr.getPosition());
		else if ("break".equals(keyWord))
			exec = new Break(start);
		else if ("continue".equals(keyWord))
			exec = new Continue(start);
		else if ("return".equals(keyWord)) {
			if (pr.peek() == ';')
				exec = new Return(start);
			else exec = new Return(start, cropExpression(pr, false, ";", parse));
		}
		else if ("boolean".equals(keyWord))
			exec = cropVariableDeclaration(pr, parse, start, keyWord);
		else if ("number".equals(keyWord))
			exec = cropVariableDeclaration(pr, parse, start, keyWord);
		else if ("string".equals(keyWord))
			exec = cropVariableDeclaration(pr, parse, start, keyWord);
		else if ("array".equals(keyWord))
			exec = cropVariableDeclaration(pr, parse, start, keyWord);
		else if ("map".equals(keyWord))
			exec = cropVariableDeclaration(pr, parse, start, keyWord);
		else if ("annot".equals(keyWord))
			exec = cropVariableDeclaration(pr, parse, start, keyWord);
		else if ("var".equals(keyWord))
			exec = cropVariableDeclaration(pr, parse, start, keyWord);
		else if ((('a' <= pr.peek()) && (pr.peek() <= 'z')) || ('A' <= pr.peek()) && (pr.peek() <= 'Z') || (pr.peek() == '_')) {
			String funcNamespace = null;
			String funcName = keyWord;
			if (funcName.indexOf(':') != -1) {
				funcNamespace = funcName.substring(0, funcName.indexOf(':'));
				funcName = funcName.substring(funcName.indexOf(':') + ":".length());
				if (!parse.checkImportNamespace(funcNamespace))
					parse.recordParseException(new UndeclaredNamespaceException(start, funcNamespace));
			}
			pr.skipSpace();
			if (pr.peek() != '(')
				skipUnexpectedChars(pr, parse, "(");
			pr.read();
			pr.skipSpace();
			ArrayList args = new ArrayList();
			while ((pr.peek() != -1) && (pr.peek() != ')')) {
				Expression arg = cropExpression(pr, true, ",)", parse);
				if (arg != null)
					args.add(arg);
				pr.skipSpace();
				if (pr.peek() == ')')
					break;
				if (pr.peek() != ',')
					skipUnexpectedChars(pr, parse, "),");
				pr.read();
			}
			if (pr.peek() == -1)
				throw new MissingCharactersException(pr.getPosition(), ((char) 0), ')');
			pr.read(); // only way of breaking loop is finding ')', throwing exception, or end of input, which is caught right above
			pr.skipSpace();
			exec = new VariableAssignment(-1, -1, null, null, new FunctionCall(start, pr.getPosition(), funcNamespace, funcName, ((Expression[]) args.toArray(new Expression[args.size()]))));
		}
		else throw new UnexpectedCharactersException(pr.getPosition(), ((char) pr.peek())); // should never happen unless we get to end of stream
		
		if (expectSemicolon) {
			pr.skipSpace();
			if (pr.peek() != ';')
				skipUnexpectedChars(pr, parse, ";");
			pr.read();
		}
		return exec;
	}
	
	private static VariableDeclaration cropVariableDeclaration(MarkupScriptParseReader pr, MarkupScriptParse parse, int start, String type) throws IOException {
		pr.skipSpace();
		if ((pr.peek() != '$') && (pr.peek() != '§'))
			skipUnexpectedChars(pr, parse, "$§");
		
		boolean isConstant;
		if (pr.peek() == '$')
			isConstant = false;
		else if (pr.peek() == '§')
			isConstant = true;
		else isConstant = false; // never gonna get here, but Java don't know
		int nameStart = pr.getPosition();
		pr.read();
		if (pr.peek() <= ' ') {
			int ueSpaceStart = pr.getPosition();
			pr.skipSpace();
			parse.recordParseException(new UnexpectedCharactersException(ueSpaceStart, ' '));
		}
		String name = cropName(pr, parse, false, "_");
		pr.skipSpace();
		if (isConstant && (parse.getGlobalVarType(name) != null))
			parse.recordParseException(new DuplicateConstantException(start, name));
		else if (!isConstant && (parse.getScopeVarType(name) != null))
			parse.recordParseException(new DuplicateVariableException(start, name));
		
		Expression value = null;
		if (pr.peek() != ';') {
			if (pr.peek() != '=')
				throw new UnexpectedCharactersException(pr.getPosition(), ((char) pr.peek()), '=');
			pr.read();
			value = cropExpression(pr, false, ";", parse);
			pr.skipSpace();
		}
		if (pr.peek() != ';')
			skipUnexpectedChars(pr, parse, ";");
		//	do not consume semicolon, happens in calling code
		if (isConstant)
			parse.setGlobalVarType(name, type);
		else parse.setScopeVarType(name, type);
		return new VariableDeclaration(start, pr.getPosition(), type, name, nameStart, value, isConstant);
	}
	
	private static VariableAssignment cropVariableAssignment(MarkupScriptParseReader pr, String stopChars, MarkupScriptParse parse) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		pr.skipSpace();
		if (pr.peek() != '$')
			throw new UnexpectedCharactersException(pr.getPosition(), ((char) pr.peek()), '$');
		//	TODO also consider object-ish function call on constant
		pr.read();
		if (pr.peek() <= ' ') {
			int ueSpaceStart = pr.getPosition();
			pr.skipSpace();
			parse.recordParseException(new UnexpectedCharactersException(ueSpaceStart, ' '));
		}
		String name = cropName(pr, parse, false, "_");
		pr.skipSpace();
		
		Expression index = null;
		if (pr.peek() == '[') {
			pr.read();
			index = cropExpression(pr, false, "]", parse);
			pr.skipSpace();
			if (pr.peek() != ']')
				skipUnexpectedChars(pr, parse, "]");
			pr.read();
		}
		int nameEnd = pr.getPosition();
		pr.skipSpace();
		
		String type = parse.getScopeVarType(name);
		if (type == null)
			parse.recordParseException(new UndeclaredVariableException(start, name));
		
		AssigningOperator aop = cropAssigningOperator(pr, parse);
		pr.skipSpace();
		
		Expression value = cropExpression(pr, false, stopChars, parse);
		pr.skipSpace();
		if (stopChars.indexOf(pr.peek()) == -1)
			skipUnexpectedChars(pr, parse, stopChars);
		return new VariableAssignment(start, pr.getPosition(), name, index, new BinaryExpression(start, pr.getPosition(), new VariableReference(start, nameEnd, null, name, index, type, false), aop, value));
	}
	
	private static ArrayList cropBlock(MarkupScriptParseReader pr, MarkupScriptParse parse) throws IOException {
		pr.skipSpace();
		ArrayList execs = new ArrayList();
		parse.startScope(true);
		if (pr.peek() == '{') {
			pr.read();
			pr.skipSpace();
			while ((pr.peek() != -1)) {
				pr.skipSpace();
				if (pr.peek() == '}') {
					pr.read();
					break;
				}
				Executable exec = cropExecutable(pr, parse);
				execs.add(exec);
				pr.skipSpace();
			}
		}
		else {
			Executable exec = cropExecutable(pr, parse);
			execs.add(exec);
			pr.skipSpace();
		}
		parse.endScope();
		return execs;
	}
	
	private static IfBlock cropIfBlock(MarkupScriptParseReader pr, MarkupScriptParse parse, int start) throws IOException {
		pr.skipSpace();
		if (pr.peek() != '(')
			skipUnexpectedChars(pr, parse, "(");
		pr.read();
		pr.skipSpace();
		
		Expression ifTest = cropExpression(pr, false, ")", parse);
		
		if (pr.peek() != ')')
			skipUnexpectedChars(pr, parse, ")");
		pr.read();
		pr.skipSpace();
		
		parse.startScope(true);
		ArrayList ifExecs = cropBlock(pr, parse);
		parse.endScope();
		
		pr.skipSpace();
		int elseStart = -1;
		Executable elseBlock = null;
		if (pr.startsWith("else ", false)) {
			elseStart = pr.getPosition();
			pr.skip("else".length());
			pr.skipSpace();
			elseBlock = cropExecutable(pr, parse);
		}
		IfBlock ifBlock = new IfBlock(start, pr.getPosition(), ifTest, elseStart, elseBlock);
		ifBlock.executables.addAll(ifExecs);
		return ifBlock;
	}
	
	private static ForLoop cropForLoop(MarkupScriptParseReader pr, MarkupScriptParse parse, int start) throws IOException {
		pr.skipSpace();
		if (pr.peek() != '(')
			skipUnexpectedChars(pr, parse, "(");
		pr.read();
		pr.skipSpace();
		
		int forTypeStart = pr.getPosition();
		String forType = cropType(pr, parse);
		pr.skipSpace();
		if (pr.peek() != '$')
			skipUnexpectedChars(pr, parse, "$");
		int forNameStart = pr.getPosition();
		pr.read();
		if (pr.peek() <= ' ') {
			int ueSpaceStart = pr.getPosition();
			pr.skipSpace();
			parse.recordParseException(new UnexpectedCharactersException(ueSpaceStart, ' '));
		}
		String forName = cropName(pr, parse, false, "_");
		pr.skipSpace();
		
		parse.startScope(true);
		parse.setScopeVarType(forName, forType);
		
		VariableDeclaration forInitializer;
		Expression forStartValue;
		Expression forTest;
		VariableAssignment forPostBody;
		int forSetStart;
		Expression forSet;
		
		if (pr.startsWith("in ", true)) {
			forInitializer = new VariableDeclaration(forTypeStart, pr.getPosition(), forType, forName, forNameStart, null, false);
			forTest = null;
			forPostBody = null;
			
			forSetStart = pr.getPosition();
			pr.skip("in".length());
			pr.skipSpace();
			forSet = cropExpression(pr, false, ")", parse);
		}
		else {
			pr.skipSpace();
			if (pr.peek() == '=') {
				pr.read();
				forStartValue = cropExpression(pr, false, ";", parse);
				forInitializer = new VariableDeclaration(forTypeStart, pr.getPosition(), forType, forName, forNameStart, forStartValue, false);
			}
			else forInitializer = null;
			pr.skipSpace();
			if (pr.peek() != ';')
				skipUnexpectedChars(pr, parse, ";");
			pr.read();
			
			pr.skipSpace();
			if (pr.peek() == ';')
				forTest = null;
			else forTest = cropExpression(pr, true, ";", parse);
			pr.skipSpace();
			if (pr.peek() != ';')
				skipUnexpectedChars(pr, parse, ";");
			pr.read();
			
			pr.skipSpace();
			if (pr.peek() == ')')
				forPostBody = null;
			else forPostBody = cropVariableAssignment(pr, ")", parse);
			
			forSetStart = -1;
			forSet = null;
		}
		
		pr.skipSpace();
		if (pr.peek() != ')')
			skipUnexpectedChars(pr, parse, ")");
		pr.read();
		
		pr.skipSpace();
		ArrayList forExecs = cropBlock(pr, parse);
		ForLoop forLoop = ((forSet == null) ? new ForLoop(start, pr.getPosition(), forInitializer, forTest, forPostBody) : new ForLoop(start, pr.getPosition(), forInitializer, forSetStart, forSet));
		forLoop.executables.addAll(forExecs);
		parse.endScope();
		return forLoop;
	}
	
	private static WhileLoop cropWhileLoop(MarkupScriptParseReader pr, MarkupScriptParse parse, int start) throws IOException {
		pr.skipSpace();
		if (pr.peek() != '(')
			skipUnexpectedChars(pr, parse, "(");
		pr.read();
		Expression whileCond = cropExpression(pr, false, ")", parse);
		if (pr.peek() != ')')
			skipUnexpectedChars(pr, parse, ")");
		pr.read();
		
		pr.skipSpace();
		ArrayList whileExecs = cropBlock(pr, parse);
		
		WhileLoop whileLoop = new WhileLoop(start, pr.getPosition(), whileCond);
		whileLoop.executables.addAll(whileExecs);
		return whileLoop;
	}
	
	private static String cropType(MarkupScriptParseReader pr, MarkupScriptParse parse) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		String type = cropAlpha(pr, "");
		if (false) {}
		else if ("boolean".equals(type)) {}
		else if ("number".equals(type)) {}
		else if ("string".equals(type)) {}
		else if ("array".equals(type)) {}
		else if ("map".equals(type)) {}
		else if ("node".equals(type)) {}
		else if ("annot".equals(type)) {}
		else if ("var".equals(type)) {}
		else if (type.matches("[a-zA-Z]+")) {
			parse.recordParseException(new UnexpectedCharactersException(start, type));
			return cropType(pr, parse);
		}
		else throw new UnexpectedCharactersException(pr.getPosition(), type);
		System.out.println("Type '" + type + "'");
		return type;
	}
	
	private static Expression cropExpression(MarkupScriptParseReader pr, boolean allowEmpty, String stopChars, MarkupScriptParse parse) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		
		//	check for empty expression
		pr.skipSpace();
		if (stopChars.indexOf(pr.peek()) != -1) {
			if (allowEmpty)
				return null;
			else throw new UnexpectedCharactersException(pr.getPosition(), ((char) pr.peek()));
		}
		
		//	crop first unary expression (also handles boolean not and numerical minus) ...
		Expression leftExp = cropUnaryExpression(pr, parse);
		pr.skipSpace();
		
		//	... and simply return it if we hit the overall end
		if (stopChars.indexOf(pr.peek()) != -1)
			return leftExp;
		
		//	crop operator
		Operator op = cropOperator(pr, parse, stopChars);
		pr.skipSpace();
		
		//	encountered stop character before actual operator due to skipping unexpected
		if (op == null)
			return leftExp;
		
		//	recurse to crop right expression
		Expression rightExp = cropExpression(pr, false, stopChars, parse);
		pr.skipSpace();
		
		//	build tree of binary expressions reflecting operator precedence
		if (rightExp instanceof BinaryExpression) {
			BinaryExpression binRightExp = ((BinaryExpression) rightExp);
			
			//	right precedence higher than left
			if (binRightExp.operator.precedence < op.precedence)
				return new BinaryExpression(start, pr.getPosition(), leftExp, op, rightExp);
			
			//	shift expression tree to left
			BinaryExpression binLeftExp = new BinaryExpression(start, binRightExp.left.end, leftExp, op, binRightExp.left);
			return new BinaryExpression(start, pr.getPosition(), binLeftExp, binRightExp.operator, binRightExp.right);
		}
		
		//	no operator precedences to evaluate
		else return new BinaryExpression(start, pr.getPosition(), leftExp, op, rightExp);
	}
	
	private static Expression cropUnaryExpression(MarkupScriptParseReader pr, MarkupScriptParse parse) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		if (pr.peek() == '$') {
			pr.read();
			String name = cropName(pr, parse, false, "_");
			String type = parse.getScopeVarType(name);
			if (type == null)
				parse.recordParseException(new UndeclaredVariableException(start, name));
			if (pr.peek() == '[') {
				Expression index = cropExpression(pr, false, "]", parse);
				if (pr.peek() != ']')
					skipUnexpectedChars(pr, parse, "]");
				pr.read();
				return new VariableReference(start, pr.getPosition(), null, name, index, type, false);
			}
			else return new VariableReference(start, pr.getPosition(), null, name, type, false);
		}
		else if (pr.peek() == '§') {
			pr.read();
			String namespace = null;
			String name = cropName(pr, parse, true, "_");
			if (name.indexOf(':') != -1) {
				namespace = name.substring(0, name.indexOf(':'));
				name = name.substring(name.indexOf(':') + ":".length());
			}
			String type = parse.getGlobalVarType(name);
			if ((type == null) && (name.indexOf(':') == -1)) // we'll resolve imports later
				parse.recordParseException(new UndeclaredConstantException(start, name));
			if (pr.peek() == '[') {
				Expression index = cropExpression(pr, false, "]", parse);
				if (pr.peek() != ']')
					skipUnexpectedChars(pr, parse, "]");
				pr.read();
				return new VariableReference(start, pr.getPosition(), namespace, name, index, type, true);
			}
			else return new VariableReference(start, pr.getPosition(), namespace, name, type, true);
		}
		else if (pr.peek() == '\'') {
			String str = ((String) JsonParser.parseJson(pr));
			return new Literal(start, pr.getPosition(), MarkupScriptTypes.wrapString(str));
		}
		else if (pr.peek() == '"') {
			String str = ((String) JsonParser.parseJson(pr));
			return new Literal(start, pr.getPosition(), MarkupScriptTypes.wrapString(str));
		}
		else if (pr.peek() == '(') {
			pr.read();
			pr.skipSpace();
			Expression exp = cropExpression(pr, false, ")", parse);
			pr.skipSpace();
			if (pr.peek() != ')')
				skipUnexpectedChars(pr, parse, ")");
			pr.read();
			return new ParenthesisExpression(start, pr.getPosition(), exp);
		}
		else if (pr.peek() == '[') {
			List array = ((List) JsonParser.parseJson(pr));
			return new Literal(start, pr.getPosition(), MarkupScriptTypes.wrapList(array));
		}
		else if (pr.peek() == '{') {
			Map map = ((Map) JsonParser.parseJson(pr));
			return new Literal(start, pr.getPosition(), MarkupScriptTypes.wrapMap(map));
		}
		else if (pr.peek() == '!') {
			Operator op = new BooleanNot(pr.getPosition(), (pr.getPosition() + "!".length()));
			pr.read();
			Expression toNegate = cropUnaryExpression(pr, parse);
			return new BinaryExpression(start, pr.getPosition(), new Literal(-1, -1, MarkupScriptTypes.wrapBoolean(false)), op, toNegate);
		}
		else if (pr.startsWith("false", true) && terminatesBoolean(pr.peek("false".length()))) {
			pr.skip("false".length());
			return new Literal(start, pr.getPosition(), MarkupScriptTypes.wrapBoolean(false));
		}
		else if (pr.startsWith("true", true) && terminatesBoolean(pr.peek("true".length()))) {
			pr.skip("true".length());
			return new Literal(start, pr.getPosition(), MarkupScriptTypes.wrapBoolean(true));
		}
		else if (pr.peek() == '-') {
			Operator op = new NumberMinus(pr.getPosition(), (pr.getPosition() + "-".length()));
			pr.read();
			Expression toNegate = cropUnaryExpression(pr, parse);
			return new BinaryExpression(start, pr.getPosition(), new Literal(-1, -1, MarkupScriptTypes.wrapInteger(0)), op, toNegate);
		}
		else if (('0' <= pr.peek()) && (pr.peek() <= '9')) {
			Number number = ((Number) JsonParser.parseJson(pr));
			return new Literal(start, pr.getPosition(), MarkupScriptTypes.wrapObject(number));
		}
		else if ((('a' <= pr.peek()) && (pr.peek() <= 'z')) || (('A' <= pr.peek()) && (pr.peek() <= 'Z')) || (pr.peek() == '_')) {
			String funcNamespace = null;
			String funcName = cropName(pr, parse, true, "_");
			if (funcName.indexOf(':') != -1) {
				funcNamespace = funcName.substring(0, funcName.indexOf(':'));
				funcName = funcName.substring(funcName.indexOf(':') + ":".length());
				if (!parse.checkImportNamespace(funcNamespace))
					parse.recordParseException(new UndeclaredNamespaceException(start, funcNamespace));
			}
			pr.skipSpace();
			if (pr.peek() != '(')
				skipUnexpectedChars(pr, parse, "(");
			pr.read();
			pr.skipSpace();
			ArrayList args = new ArrayList();
			while ((pr.peek() != ')') && (pr.peek() != -1)) {
				Expression arg = cropExpression(pr, false, ",)", parse);
				args.add(arg);
				pr.skipSpace();
				if ((pr.peek() != ')') && (pr.peek() != ','))
					skipUnexpectedChars(pr, parse, "),");
				if (pr.peek() == ')')
					break; // we'll consume the bracket after the loop
				int commaStart = pr.getPosition();
				pr.read(); // consume comma
				pr.skipSpace();
				if (pr.peek() == ')') // just record that dangling comma
					parse.recordParseException(new UnexpectedCharactersException(commaStart, ','));
			}
			pr.skipSpace();
			if (pr.peek() != ')') // only way this can be true is end of input
				throw new MissingCharactersException(pr.getPosition(), "", ")");
			pr.read();
			return new FunctionCall(start, pr.getPosition(), funcNamespace, funcName, ((Expression[]) args.toArray(new Expression[args.size()])));
		}
		else throw new UnexpectedCharactersException(pr.getPosition(), ((char) pr.peek()));
	}
	
	private static final boolean terminatesBoolean(int ch) {
		if (ch <= ' ')
			return true;
		else if (('a' <= ch) && (ch <= 'z'))
			return false;
		else if (('A' <= ch) && (ch <= 'Z'))
			return false;
		else if (('0' <= ch) && (ch <= '9'))
			return false;
		else if ("_([{".indexOf(ch) != -1)
			return false;
		else return true;
	}
	
	private static Operator cropOperator(MarkupScriptParseReader pr, MarkupScriptParse parse, String stopChars) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		if (pr.startsWith("!=", true)) {
			pr.skip("!=".length());
			return new NotEquals(start, pr.getPosition());
		}
		else if (pr.startsWith("==", true)) {
			pr.skip("==".length());
			return new Equals(start, pr.getPosition());
		}
		else if (pr.peek() == '=') {
			pr.skip("=".length());
			return new Assign(start, pr.getPosition());
		}
		else if (pr.startsWith("+=", true)) {
			pr.skip("+=".length());
			return new AddAssign(start, pr.getPosition());
		}
		else if (pr.peek() ==  '+') {
			pr.skip("+".length());
			return new Add(start, pr.getPosition());
		}
		else if (pr.startsWith("-=", true)) {
			pr.skip("-=".length());
			return new SubtractAssign(start, pr.getPosition());
		}
		else if (pr.peek() ==  '-') {
			pr.skip("-".length());
			return new Subtract(start, pr.getPosition());
		}
		else if (pr.startsWith("*=", true)) {
			pr.skip("*=".length());
			return new MultiplyAssign(start, pr.getPosition());
		}
		else if (pr.peek() ==  '*') {
			pr.skip("*".length());
			return new Multiply(start, pr.getPosition());
		}
		else if (pr.startsWith("/", true)) {
			pr.skip("/".length());
			return new Divide(start, pr.getPosition());
		}
		else if (pr.startsWith("%", true)) {
			pr.skip("%".length());
			return new Modulo(start, pr.getPosition());
		}
		else if (pr.startsWith("&=", true)) {
			pr.skip("&=".length());
			return new AndAssign(start, pr.getPosition());
		}
		else if (pr.startsWith("&&", true)) {
			pr.skip("&&".length());
			return new And(start, pr.getPosition());
		}
		else if (pr.peek() ==  '&') {
			pr.skip("&".length());
			return new And(start, pr.getPosition());
		}
		else if (pr.startsWith("!&", true)) {
			pr.skip("!&".length());
			return new Nand(start, pr.getPosition());
		}
		else if (pr.startsWith("|=", true)) {
			pr.skip("|=".length());
			return new OrAssign(start, pr.getPosition());
		}
		else if (pr.startsWith("||", true)) {
			pr.skip("||".length());
			return new Or(start, pr.getPosition());
		}
		else if (pr.peek() ==  '|') {
			pr.skip("|".length());
			return new Or(start, pr.getPosition());
		}
		else if (pr.startsWith("!|", true)) {
			pr.skip("!|".length());
			return new Nor(start, pr.getPosition());
		}
		else if (pr.startsWith("<=", true)) {
			pr.skip("<=".length());
			return new LessEquals(start, pr.getPosition());
		}
		else if (pr.peek() ==  '<') {
			pr.skip("<".length());
			return new Less(start, pr.getPosition());
		}
		else if (pr.startsWith(">=", true)) {
			pr.skip(">=".length());
			return new GreaterEquals(start, pr.getPosition());
		}
		else if (pr.peek() ==  '>') {
			pr.skip(">".length());
			return new Greater(start, pr.getPosition());
		}
		else {
			skipUnexpectedChars(pr, parse, (stopChars + "!=+-*/%&|<>"));
			if (stopChars.indexOf(pr.peek()) != -1)
				return null;
			else return cropOperator(pr, parse, stopChars);
		}
	}
	
	private static AssigningOperator cropAssigningOperator(MarkupScriptParseReader pr, MarkupScriptParse parse) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		if (pr.peek() == '=') {
			pr.skip("=".length());
			return new Assign(start, pr.getPosition());
		}
		else if (pr.startsWith("+=", true)) {
			pr.skip("+=".length());
			return new AddAssign(start, pr.getPosition());
		}
		else if (pr.startsWith("-=", true)) {
			pr.skip("-=".length());
			return new SubtractAssign(start, pr.getPosition());
		}
		else if (pr.startsWith("*=", true)) {
			pr.skip("*=".length());
			return new MultiplyAssign(start, pr.getPosition());
		}
		else if (pr.startsWith("/=", true)) {
			pr.skip("/=".length());
			return new DivideAssign(start, pr.getPosition());
		}
		else if (pr.startsWith("&=", true)) {
			pr.skip("&=".length());
			return new AndAssign(start, pr.getPosition());
		}
		else if (pr.startsWith("|=", true)) {
			pr.skip("|=".length());
			return new OrAssign(start, pr.getPosition());
		}
		else {
			skipUnexpectedChars(pr, parse, "=+-*/&|");
			return cropAssigningOperator(pr, parse);
		}
	}
	
	private HashMap functionsByName = new HashMap();
	
	private HashMap constantsByName = new HashMap();
	
	private ArrayList imports = new ArrayList();
	private HashMap importsByNamespaces = new HashMap();
	
	private ArrayList executables = new ArrayList();
	
	private ArrayList comments = new ArrayList();
	
	private ArrayList parts = new ArrayList();
	
	private ArrayList parseExceptions = new ArrayList();
	
	private MarkupScript() {}
	
	void addComment(Comment comment) {
		this.comments.add(comment);
		this.parts.add(comment);
	}
	
	void addImport(Import imp) {
		this.imports.add(imp);
		this.parts.add(imp);
	}
	
	void addFunction(Function func) {
		Object exObj = this.functionsByName.get(func.name);
		if (exObj == null)
			this.functionsByName.put(func.name, func);
		else if (exObj instanceof ArrayList)
			((ArrayList) exObj).add(func);
		else {
			ArrayList funcList = new ArrayList();
			funcList.add(exObj);
			funcList.add(func);
			this.functionsByName.put(func.name, funcList);
		}
		this.parts.add(func);
	}
	
	void addExecutable(Executable exec) {
		this.executables.add(exec);
		if (exec instanceof VariableDeclaration) {
			VariableDeclaration vDec = ((VariableDeclaration) exec);
			if (vDec.varIsConstant && !this.constantsByName.containsKey(vDec.varName))
				this.constantsByName.put(vDec.varName, vDec); // duplicates are reported by parser code
		}
		this.parts.add(exec);
	}
	
	void addParseException(MarkupScriptParseException mspe) {
		this.parseExceptions.add(mspe);
	}
	
	void assortDocComments() {
		Collections.sort(this.parts);
		Comment docComment = null;
		for (int p = 0; p < this.parts.size(); p++) {
			Part part = ((Part) this.parts.get(p));
			if (part instanceof Comment) {
				if (((Comment) part).text.startsWith("/**"))
					docComment = ((Comment) part);
			}
			else {
				if (part instanceof Function)
					((Function) part).documentation = docComment;
				else if ((part instanceof VariableDeclaration) && ((VariableDeclaration) part).varIsConstant)
					((VariableDeclaration) part).documentation = docComment;
				docComment = null;
			}
		}
	}
	
	void resolveImports() {
		for (int i = 0; i < this.imports.size(); i++) {
			Import imp = ((Import) this.imports.get(i));
			imp.script = getMarkupScriptForName(imp.name);
			if (imp.script == null) {
				this.addParseException(new UnresolvableImportException(imp));
				continue;
			}
			ArrayList nsImss = ((ArrayList) this.importsByNamespaces.get(imp.namespace));
			if (nsImss == null) {
				nsImss = new ArrayList();
				this.importsByNamespaces.put(imp.namespace, nsImss);
			}
			nsImss.add(0, imp);
		}
	}
	
	void bindFunctionsAndConstants() {
		for (int p = 0; p < this.parts.size(); p++) {
			Part part = ((Part) this.parts.get(p));
			if (part instanceof FunctionCall) {
				FunctionCall fCall = ((FunctionCall) part);
				ArrayList functions = this.getFunctions(fCall.funcNamespace, fCall.funcName);
				for (int f = 0; f < functions.size(); f++) {
					Function func = ((Function) functions.get(f));
					if (argsMatch(fCall.args, func.args)) {
						fCall.function = func;
						break;
					}
				}
				if (fCall.function == null)
					this.addParseException(new UndeclaredFunctionException(fCall));
			}
			else if ((part instanceof VariableReference) && ((VariableReference) part).varIsConstant) {
				VariableReference vRef = ((VariableReference) part);
				VariableDeclaration vDec = this.getConstantDeclaration(vRef.varNamespace, vRef.varName);
				if (vDec == null) {
					this.addParseException(new UndeclaredConstantException(vRef));
					continue;
				}
				vRef.varType = vDec.varType;
			}
		}
	}
	
	private ArrayList getFunctions(String namespace, String name) {
		ArrayList functions = new ArrayList();
		if (namespace == null) {
			Object funcObj = this.functionsByName.get(name);
			if (funcObj instanceof Function)
				functions.add(funcObj);
			else if (funcObj instanceof ArrayList)
				functions.addAll((ArrayList) funcObj);
		}
		else {
			ArrayList nsImps = ((ArrayList) this.importsByNamespaces.get(namespace));
			for (int i = 0; (nsImps != null) && (i < nsImps.size()); i++) {
				Import imp = ((Import) nsImps.get(i));
				ArrayList impFuncs = imp.script.getFunctions(null, name);
				for (int f = 0; f < impFuncs.size(); f++) {
					((Function) impFuncs.get(f)).source = imp.name;
					functions.add(impFuncs.get(f));
				}
			}
		}
		return functions;
	}
	
	private VariableDeclaration getConstantDeclaration(String namespace, String name) {
		if (namespace == null)
			return ((VariableDeclaration) this.constantsByName.get(name));
		else {
			ArrayList nsImps = ((ArrayList) this.importsByNamespaces.get(namespace));
			for (int i = 0; (nsImps != null) && (i < nsImps.size()); i++) {
				Import imp = ((Import) nsImps.get(i));
				VariableDeclaration vDec = imp.script.getConstantDeclaration(null, name);
				if (vDec != null) {
					vDec.source = imp.name;
					return vDec;
				}
			}
			return null;
		}
	}
	
	private static boolean argsMatch(Expression[] callArgs, VariableDeclaration[] funcArgs) {
		if (callArgs.length != funcArgs.length)
			return false;
		for (int a = 0; a < funcArgs.length; a++) {
			//	TODO observe 'var' takes everything
			//	TODO observe 'map' takes 'annot' and 'doc'
			//	TODO observe 'annot' takes 'doc'
			if (!funcArgs[a].varType.equals(callArgs[a].getReturnType()))
				return false;
		}
		return true;
	}
	
	void printString() {
		for (int p = 0; p < this.parts.size(); p++)
			((Part) this.parts.get(p)).printString("");
	}
	
	void styleCode(StyledDocument sd) {
		for (int p = 0; p < this.parts.size(); p++)
			((Part) this.parts.get(p)).styleCode(sd);
		for (int e = 0; e < this.parseExceptions.size(); e++) {
			((MarkupScriptParseException) this.parseExceptions.get(e)).styleCode(sd);
			System.out.println(((MarkupScriptParseException) this.parseExceptions.get(e)).getMessage());
		}
	}
	
	/**
	 * Execute the markup script on a GAMTA document or part thereof
	 * @param data the document to execute the script upon
	 */
	public void execute(MutableAnnotation data) {
		//	TODO implement this
		//	TODO pass data as constant '§doc'
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String msStr = "" +
				"import something.ms;\r\n" +
				"import something.further.ms as frt;\r\n" +
				"/** this should explain below function ... */\r\n" +
				"// while this one should not get in the way ...\r\n" +
				"function factorial(number $n) => number {\r\n" +
				"  if ($n <= 1)\r\n" +
				"    return $n;\r\n" +
				"  else return ($n * factorial($n - 1));\r\n" +
				"}\r\n" +
				"/* ... and this should \r\nexplain above function */\r\n" +
				"number $n1 = factorial(1);\r\n" +
				"$n1 = frt:factorial(2);\r\n" +
				"$n1 = frs:factorial(3);\r\n" +
				"for (number $n = 0; $n < 5; $n += 1) {\r\n" +
				"  $n = factorial($n - 1);\r\n" +
				"}\r\n" +
				"string $strSq = 'this is a string in single quotes';\r\n" +
				"string $strDq = \"this is a string in double quotes\";\r\n" +
				"number $n = 0;\r\n" +
				"while ($n1 <= 10)\r\n" +
				"  $n = ($n + 1);\r\n" +
				"if ($a && $b || $c & $d)\r\n" +
				"  return;\r\n" +
				"if ($a || $b && $c || $d)\r\n" +
				"  return;\r\n" +
				"if ($a && $b && $c && $d)\r\n" +
				"  return;\r\n" +
				"if (($a && $b) && ($c && $d))\r\n" +
				"  return;\r\n" +
				"if ($a && $b && $c)\r\n" +
				"  return;\r\n" +
				"";
		MarkupScript ms = parse(new StringReader(msStr));
		ms.printString();
		JTextPane tp = new JTextPane();
		tp.setFont(Font.getFont("Courier"));
		StyledDocument sd = tp.getStyledDocument();
		sd.insertString(sd.getLength(), msStr, null); // this way we get no penalty from the cross-platform line breaks
		ms.styleCode(tp.getStyledDocument());
		JOptionPane.showMessageDialog(null, tp);
	}
	
	private static class HashtableAttributeSet implements AttributeSet {
		Hashtable attrs = new Hashtable();
		public int getAttributeCount() {
			return this.attrs.size();
		}
		public boolean isDefined(Object attrName) {
			return this.attrs.containsKey(attrName);
		}
		public boolean isEqual(AttributeSet attr) {
			return false;
		}
		public AttributeSet copyAttributes() {
			return this;
		}
		public Object getAttribute(Object key) {
			return this.attrs.get(key);
		}
		public Enumeration getAttributeNames() {
			return this.attrs.keys();
		}
		public boolean containsAttribute(Object name, Object value) {
			return value.equals(this.attrs.get(name));
		}
		public boolean containsAttributes(AttributeSet attributes) {
			return false;
		}
		public AttributeSet getResolveParent() {
			return null;
		}
	}
	
	private static HashtableAttributeSet STYLE_ERROR = new HashtableAttributeSet();
	static {
		STYLE_ERROR.attrs.put(StyleConstants.Background, new Color(255, 128, 128));
	}
	
	private static HashtableAttributeSet STYLE_KEYWORD = new HashtableAttributeSet();
	static {
		STYLE_KEYWORD.attrs.put(StyleConstants.Foreground, new Color(192, 0, 192));
		STYLE_KEYWORD.attrs.put(StyleConstants.Bold, Boolean.TRUE);
	}
	
	private static HashtableAttributeSet STYLE_COMMENT = new HashtableAttributeSet();
	static {
		STYLE_COMMENT.attrs.put(StyleConstants.Foreground, new Color(0, 128, 64));
	}
	
	private static HashtableAttributeSet STYLE_VARIABLE = new HashtableAttributeSet();
	static {
		STYLE_VARIABLE.attrs.put(StyleConstants.Foreground, Color.BLUE);
	}
	
	private static HashtableAttributeSet STYLE_LITERAL = new HashtableAttributeSet();
	static {
		STYLE_LITERAL.attrs.put(StyleConstants.Foreground, new Color(192, 0, 0));
	}
	
	/*
Variables (extending upon XPath, names like '$<qName>' for variables and '§<qName>' for constants):
- boolean (type keyword 'bool', in-line keywords 'true' and 'false')
- number (type keyword 'num', in-line any non-quoted number)
- string (type keyword 'str', in-line enclosed in single or double quotes, escaped with backslash)
- array (type keyword 'array', indexed zero-based, index access via number in square brackets)
- map (type keyword 'map' (intentionally _not_ 'obj'), associative, access via string or number keys in square brackets (intentionally _not_ via dot syntax))
- node/annotation (type keyword 'node', map with specific properties, like value, start index, end index, size, array-based access to tokens, etc.)
- set of node/annotation (array of nodes)
- var (type keyword 'var', mapped based on XPath rules)

Control structures (akin to JavaScript):
- code blocks (enclosed in curly brackets, with variable scoping)
- functions (keyword 'function', with typed parameter list and typed return value with keyword 'return', body code block)
- for loops (keyword 'for', header 3-part or 'var <x> in <array>/<map>' with keyword 'in', body single statement or code block)
- while loops (keyword 'while', header with conditional expression, body single statement or code block)
- if/else (keywords 'if' and 'else', cascades allowed, terminal 'else' optional, bodies single statements or code blocks)
- line comments (starting with double slash)
- block comments (enclosed in usual slash-asterisk combinations)
- scripts (whole files, functions registered to default namespace, top level statements executed sequentially after all functions registered)
- imports of other scripts (keyword 'import', resolved via statically registered resolvers, disambiguated via namespace prefix specified after keyword 'as', conflation of namepaces permitted to create kind of facades, transitive imports permitted, function namespaces handled locally in each script to avoid collisions, top level statements ignored)

Operands:
- like in JavaScript, and always lazy
- operand conversion akin to XPath ...
- ... with respective conversions for arrays and maps

Statements (terminated by semicolon):
- variable declarations (type keyword followed by dollar-prefixed variable name)
- variable assignments (combinable with declaration as usual, left side variable name, right side expression)
- continuing and breaking of loops (keywords 'continue' and 'break')
- function calls (simply by name, prefixed with namespace and colon for imported functions)
- calls to other scripts (keyword 'call', executes top level statements, allowing use of functions from there, but not importing functions)
- system function calls (simply by name, resolved by engine via extensible function providers, optionally disambiguated via namespaces)
- evaluatable expressions (literals, variable references, arithmetics in parentheses, function calls with return value)

Parsing (like JSON or PDF):
- linear with lookahead
- use crop<Xyz> as usual
- carry some memory:
  - did we see any printable character since the last line break? (should help with line comments)
  - how many characters did we consume since the start of the script? (should help with syntax highlighting)
  - in which line of the script are we? (should help with error messages)
  - did we see an error anywhere (best keep list of errors, including error proper as well as positional information)
- convert script into executable AST consisting of Java object nodes representing each control structure, statement, variable, and literal
- retain data memorized above in AST to support both syntax highlighting and error reporting

Execution (pretty much like parsed GScript):
- call 'execute' function of script AST root
- _no_ exceptions, just type XPath style conversion
- cascade variable resolvers when entering blocks, defaulting outward as usual
- detect endless loops in loop AST objects via variable resolver staying untouched over multiple rounds (throwing execution exception reporting the problem, rationale: at least loop local variable has to change for loop to ever terminate)

System functions (extending upon GScript):
- (basic) arithmetics (as built into XPath engine already)
- strings (as built into XPath and GScript engines already) ...
- ... maybe extended upon by some similarity measures ...
- ... and regular expressions
- characters (based upon both Character and StringUtils)
- annotating (adding annotation to current context annotation, relative to latter)
- content extraction (returning node/annotation arrays, with elements initially unattached to context):
  - using dictionary lookups (also some column of a CSV, with the other columns becoming attributes)
    - dictionaries either locally created maps ...
    - ... or ones called upon by name (resolved by extensible resolver)
  - using pattern matching
  - using annotation pattern matching
- running Analyzers (by name, resolved by extensible resolver)
- custom functions added by custom providers/resolvers

Editing:
- use JTextPane
- use positional information from AST for syntax highlighting ...
- ... always re-parsing after half a second (? ... adjustable?) without key stroke
- to do that in presence of errors:
  - keep 'invalid' or 'erroneous' flag in AST (and refuse execution if set)
  - if possible proceed as if variable or function name was valid
- to proceed as sensible as possible:
  - inherently finish strings at end of line
  - inherently close all open brackets at semicolons, as well as at quotes
  - ignore closing brackets if none are open

Default file extension:
- 'gms' for 'GAMTA Markup Script'
	 */
}
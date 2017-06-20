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
package de.uka.ipd.idaho.gamta.util.markupScript;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.uka.ipd.idaho.easyIO.streams.PeekInputStream;
import de.uka.ipd.idaho.easyIO.streams.PeekReader;
import de.uka.ipd.idaho.easyIO.util.JsonParser;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathBoolean;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathNumber;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathString;

/**
 * TODO document this (soon as we've hammered out the syntax ...)
 * 
 * @author sautter
 */
public class MarkupScript {
	
	/**
	 * TODO document this class
	 * 
	 * @author sautter
	 */
	public static class GPathArray extends GPathObject {
		private ArrayList values = new ArrayList();
		public GPathString asString() {
			// TODO concatenate values
			return null;
		}
		public GPathNumber asNumber() {
			// TODO concatenate values
			return null;
		}
		public GPathBoolean asBoolean() {
			// TODO concatenate values
			return null;
		}
		GPathObject get(GPathNumber index) {
			return ((GPathObject) this.values.get((int) Math.round(index.value)));
		}
		GPathObject set(GPathNumber index, GPathObject value) {
			return ((GPathObject) this.values.set(((int) Math.round(index.value)), value));
		}
		int size() {
			return this.values.size();
		}
	}
	
	/**
	 * TODO document this class
	 * 
	 * @author sautter
	 */
	public static class GPathMap extends GPathObject {
		private HashMap values = new HashMap();
		public GPathString asString() {
			// TODO concatenate values
			return null;
		}
		public GPathNumber asNumber() {
			// TODO concatenate values
			return null;
		}
		public GPathBoolean asBoolean() {
			// TODO concatenate values
			return null;
		}
		GPathObject get(GPathString key) {
			return ((GPathObject) this.values.get(key.value));
		}
		GPathObject set(GPathString key, GPathObject value) {
			return ((GPathObject) this.values.put(key.value, value));
		}
		int size() {
			return this.values.size();
		}
	}
	
	private static abstract class Part {
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
	}
	
	private static class Import extends Part {
		final String name;
		final String namespace;
		final int nsStart; // starting position of namespace declaration in source script
		Import(int start, int end, String name) {
			this(start, end, name, -1, null);
		}
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
		Comment(int start, int end) {
			super("comment", start, end);
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
		abstract String getReturnType();
		abstract GPathObject evaluate(MutableAnnotation data, VariableResolver vars);
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
		abstract GPathObject applyTo(GPathObject left, GPathObject right);
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
	}
	
	private static abstract class Executable extends Part {
		Executable(String type, int start, int end) {
			super(type, start, end);
		}
		abstract GPathObject execute(MutableAnnotation data, VariableResolver vars);
	}
	
	private static class VariableDeclaration extends Executable {
		final String varType;
		final String varName;
		final int varNameStart;
		final Expression value;
		VariableDeclaration(int start, int end, String type, String name, int nameStart, Expression value) {
			super("variabledeclaration", start, end);
			this.varType = type;
			this.varName = name;
			this.varNameStart = nameStart;
			this.value = value;
		}
		GPathObject execute(MutableAnnotation data, VariableResolver vars) {
			GPathObject value;
			if (this.value == null) {
				if ("string".equals(this.varType))
					value = new GPathString("");
				else if ("number".equals(this.varType))
					value = new GPathNumber(0);
				else if ("boolean".equals(this.varType))
					value = new GPathBoolean(false);
				else if ("array".equals(this.varType))
					value = new GPathArray();
				else if ("map".equals(this.varType))
					value = new GPathMap();
				//	TODO implement other types (annotation, former two with JSON value)
				else value = new GPathString(""); // TODO throw exception instead? Or simply do nothing?
			}
			else value = this.value.evaluate(data, vars);
			
			vars.declareVariable(this.varName, this.varType);
			
			return vars.setVariable(this.varName, value);
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
		GPathObject execute(MutableAnnotation data, VariableResolver vars) {
			if (this.name == null) // function call modeled as right side of assignment with missing left side
				return this.value.evaluate(data, vars);
			
			GPathObject value = this.value.evaluate(data, vars);
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
				return vars.setVariable(this.name, value);
			
			GPathObject index = this.index.evaluate(data, vars);
			GPathObject values = vars.getVariable(this.name);
			if (values instanceof GPathArray)
				return ((GPathArray) values).set(index.asNumber(), value);
			else if (values instanceof GPathMap)
				return ((GPathMap) values).set(index.asString(), value);
			else throw new RuntimeException("Cannot set " + index + " of " + values + " to " + value);
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
		final GPathObject value;
		Literal(int start, int end, GPathObject value) {
			super("literal", start, end);
			this.value = value;
		}
		String getReturnType() {
			if (this.value instanceof GPathBoolean)
				return "boolean";
			else if (this.value instanceof GPathNumber)
				return "number";
			else if (this.value instanceof GPathString)
				return "string";
			else if (this.value instanceof GPathArray)
				return "array";
			else if (this.value instanceof GPathMap)
				return "map";
			else return "var";
		}
		GPathObject evaluate(MutableAnnotation data, VariableResolver vars) {
			return this.value;
		}
		void printString(String indent) {
			if (this.value instanceof GPathBoolean)
				System.out.println(indent + this.value.asBoolean().value);
			else if (this.value instanceof GPathNumber)
				System.out.println(indent + this.value.asNumber().value);
			else if (this.value instanceof GPathString)
				System.out.println(indent + this.value.asString().value);
			else if (this.value instanceof GPathArray)
				System.out.println(indent + ((GPathArray) this.value).values);
			else if (this.value instanceof GPathMap)
				System.out.println(indent + ((GPathMap) this.value).values);
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, (this.end - this.start), STYLE_LITERAL, true);
		}
	}
	
	private static class VariableReference extends Expression {
		final String name;
		final Expression index;
		VariableReference(int start, int end, String name) {
			this(start, end, name, null);
		}
		VariableReference(int start, int end, String name, Expression index) {
			super("variablereference", start, end);
			this.name = name;
			this.index = index;
		}
		String getReturnType() {
			// TODO Auto-generated method stub
			return null;
		}
		GPathObject evaluate(MutableAnnotation data, VariableResolver vars) {
			GPathObject value = vars.getVariable(this.name);
			if (this.index == null)
				return value;
			GPathObject index = this.index.evaluate(data, vars);
			if (value instanceof GPathArray)
				return ((GPathArray) value).get(index.asNumber());
			else if (value instanceof GPathMap)
				return ((GPathMap) value).get(index.asString());
			else throw new RuntimeException("Cannot get " + index + " of " + value);
		}
		void printString(String indent) {
			if (this.index == null)
				System.out.println(indent + "$" + this.name);
			else {
				System.out.println(indent + "$" + this.name + "[");
				this.index.printString(indent + "  ");
				System.out.println(indent + "]");
			}
		}
		void styleCode(StyledDocument sd) {
			sd.setCharacterAttributes(this.start, ("$".length() + this.name.length()), STYLE_VARIABLE, true);
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
		GPathObject evaluate(MutableAnnotation data, VariableResolver vars) {
			return this.content.evaluate(data, vars);
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
		GPathObject evaluate(MutableAnnotation data, VariableResolver vars) {
			GPathObject left = this.left.evaluate(data, vars);
			GPathObject right = this.right.evaluate(data, vars);
			GPathObject result = this.operator.applyTo(left, right);
			if ((this.left instanceof VariableReference) && (this.operator instanceof AssigningOperator))
				vars.setVariable(((VariableReference) this.left).name, result);
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
		final String functionName;
		Function function = null;
		final Expression[] args;
		FunctionCall(int start, int end, String functionName, Expression[] args) {
			super("functioncall", start, end);
			this.functionName = functionName;
			this.args = args;
		}
		String getReturnType() {
			return this.function.getReturnType();
		}
		GPathObject evaluate(MutableAnnotation data, VariableResolver vars) {
			if (this.function == null)
				throw new RuntimeException("Unresolved function name '" + this.functionName + "' at " + this.start);
			VariableResolver funcVars = new VariableResolver(vars.globals, null); // a function doesn't blend into the scope of its call
			for (int a = 0; a < this.function.args.length; a++) {
				GPathObject value = this.args[a].evaluate(data, vars);
				funcVars.declareVariable(this.function.args[a].varName, this.function.args[a].varType);
				funcVars.setVariable(this.function.args[a].varName, value);
				//	TODO do type conversion here? do we need that at all?
			}
			return this.function.execute(data, funcVars);
		}
		void printString(String indent) {
			if (this.args.length == 0)
				System.out.println(indent + this.functionName + "()");
			else {
				System.out.println(indent + this.functionName + "(");
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
		ExecutableSequence(int start, int end) {
			this("block", start, end);
		}
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
		GPathObject execute(MutableAnnotation data, VariableResolver vars) {
			for (int e = 0; e < this.executables.size(); e++) {
				GPathObject result = ((Executable) this.executables.get(e)).execute(data, vars);
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
		IfBlock(int start, int end, Expression test) {
			this(start, end, test, -1, null);
		}
		IfBlock(int start, int end, Expression test, int elseStart, Executable elseBlock) {
			super("if", start, end);
			this.test = test;
			this.elseStart = elseStart;
			this.elseBlock = elseBlock;
		}
		GPathObject execute(MutableAnnotation data, VariableResolver vars) {
			VariableResolver ifVars = new VariableResolver(vars.globals, vars);
			if (this.test.evaluate(data, vars).asBoolean().value) {
				for (int e = 0; e < this.executables.size(); e++) {
					GPathObject result = ((Executable) this.executables.get(e)).execute(data, ifVars);
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
			else return this.elseBlock.execute(data, vars);
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
		GPathObject execute(MutableAnnotation data, VariableResolver vars) {
			VariableResolver loopVars = new VariableResolver(vars.globals, vars);
			if (this.initializer != null)
				this.initializer.execute(data, loopVars);
			if (this.setRef == null) {
				while ((this.test == null) || this.test.evaluate(data, loopVars).asBoolean().value) {
					for (int e = 0; e < this.executables.size(); e++) {
						GPathObject result = ((Executable) this.executables.get(e)).execute(data, vars);
						if (result instanceof ReturnValue)
							return result; // return from function call ==> we're out of here (return value will be un-wrapped in ancestor function)
						else if (result == BREAK_VALUE)
							return null; // break loop ==> we're done here
						else if (result == CONTINUE_VALUE)
							e = this.executables.size(); // jump to end of loop body
					}
					if (this.postBody != null)
						this.postBody.execute(data, loopVars);
				}
				return null;
			}
			else {
				ArrayList setVals = new ArrayList();
				GPathObject setObj = this.setRef.evaluate(data, vars);
				if (setObj instanceof GPathArray)
					setVals.addAll(((GPathArray) setObj).values);
				else if (setObj instanceof GPathMap)
					setVals.addAll(((GPathMap) setObj).values.keySet());
				else throw new RuntimeException("Cannot iterate over " + setObj);
				for (int v = 0; v < setVals.size(); v++) {
					loopVars.setVariable(this.initializer.varName, ((GPathObject) setVals.get(v)));
					for (int e = 0; e < this.executables.size(); e++) {
						GPathObject result = ((Executable) this.executables.get(e)).execute(data, vars);
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
		GPathObject execute(MutableAnnotation data, VariableResolver vars) {
			VariableResolver loopVars = new VariableResolver(vars.globals, vars);
			while ((this.test == null) || this.test.evaluate(data, loopVars).asBoolean().value)
				for (int e = 0; e < this.executables.size(); e++) {
					GPathObject result = ((Executable) this.executables.get(e)).execute(data, vars);
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
	
	private static class ReturnValue extends GPathString {
		final GPathObject value;
		ReturnValue(GPathObject value) {
			super("return");
			this.value = value;
		}
	}
	private static final GPathObject BREAK_VALUE = new GPathString("break");
	private static final GPathObject CONTINUE_VALUE = new GPathString("continue");
	
	private static class Empty extends Executable {
		Empty(int start) {
			super("empty", start, start);
		}
		GPathObject execute(MutableAnnotation data, VariableResolver vars) {
			return null;
		}
		void printString(String indent) {}
		void styleCode(StyledDocument sd) {}
	}
	
	private static class Break extends Executable {
		Break(int start, int end) {
			super("break", start, end);
		}
		GPathObject execute(MutableAnnotation data, VariableResolver vars) {
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
		Continue(int start, int end) {
			super("continue", start, end);
		}
		GPathObject execute(MutableAnnotation data, VariableResolver vars) {
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
		Return(int start, int end, Expression value) {
			super("return", start, end);
			this.value = value;
		}
		GPathObject execute(MutableAnnotation data, VariableResolver vars) {
			return new ReturnValue((this.value == null) ? null : this.value.evaluate(data, vars));
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			return new GPathBoolean((right == null) || !right.asBoolean().value);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			return new GPathNumber(-right.asNumber().value);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			if ((left == null) || (right == null))
				return new GPathNumber(0);
			else return new GPathNumber(left.asNumber().value * right.asNumber().value);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			if ((left == null) || (right == null))
				return new GPathNumber(0);
			else return new GPathNumber(left.asNumber().value / right.asNumber().value);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			if ((left == null) || (right == null))
				return new GPathNumber(0);
			else return new GPathNumber(left.asNumber().value % right.asNumber().value);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			//	TODO handle null
			String leftType = getType(left);
			String rightType = getType(right);
			if ("number".equals(leftType) && "number".equals(rightType))
				return new GPathNumber(left.asNumber().value + right.asNumber().value);
			else if ("array".equals(leftType) && "array".equals(rightType)) {
				((GPathArray) left).values.addAll(((GPathArray) right).values);
				return left;
			}
			else if ("array".equals(leftType)) {
				((GPathArray) left).values.add(right);
				return left;
			}
			else if ("map".equals(leftType) && "map".equals(rightType)) {
				for (Iterator kit = ((GPathMap) right).values.keySet().iterator(); kit.hasNext();) {
					Object key = kit.next();
					if (!((GPathMap) left).values.containsKey(key)) // don't overwrite keys
						((GPathMap) left).values.put(key, ((GPathMap) right).values.get(key));
				}
//				((GPathMap) left).values.putAll(((GPathMap) right).values); // TODO do overwrite values?
				return left;
			}
			else if ("map".equals(leftType) && "array".equals(rightType)) {
				for (int a = 0; a < (((GPathArray) right).values.size() - 1); a += 2) {
					GPathString key = ((GPathObject) ((GPathArray) right).values.get(a)).asString();
					GPathObject value = ((GPathObject) ((GPathArray) right).values.get(a+1));
					((GPathMap) left).values.put(key.value, value);
				}
				return left;
			}
			else if ("map".equals(leftType)) {
				((GPathMap) left).values.put(right.asString().value, new GPathBoolean(true));
				return left;
			}
			else return new GPathString(left.asString().value + right.asString().value);
		}
	}
	
	private static class Subtract extends Operator {
		Subtract(int start, int end) {
			super(start, end, "-", ADD_PRECEDENCE);
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return (false
				|| ("number".equals(leftArgType) && "number".equals(leftArgType))
				|| "array".equals(leftArgType)
				|| ("map".equals(leftArgType) && "array".equals(leftArgType))
				|| "map".equals(leftArgType)
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			//	TODO handle null
			String leftType = getType(left);
			String rightType = getType(right);
			if ("number".equals(leftType) && "number".equals(rightType))
				return new GPathNumber(left.asNumber().value - right.asNumber().value);
			else if ("array".equals(leftType)) {
				((GPathArray) left).values.remove(right);
				return left;
			}
			else if ("map".equals(leftType) && "map".equals(rightType)) {
				for (Iterator kit = ((GPathMap) right).values.keySet().iterator(); kit.hasNext();) {
					String key = ((String) kit.next());
					((GPathMap) left).values.remove(key);
				}
				return left;
			}
			else if ("map".equals(leftType) && "array".equals(rightType)) {
				for (int a = 0; a < ((GPathArray) right).values.size(); a++) {
					GPathString key = ((GPathObject) ((GPathArray) right).values.get(a)).asString();
					((GPathMap) left).values.remove(key.value);
				}
				return left;
			}
			else if ("map".equals(leftType)) {
				((GPathMap) left).values.remove(right.asString().value);
				return left;
			}
			else return new GPathString(left.asString().value + right.asString().value);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			return new GPathBoolean(MarkupScript.equals(left, right));
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			return new GPathBoolean(!MarkupScript.equals(left, right));
		}
	}
	
	private static boolean equals(GPathObject left, GPathObject right) {
		String leftType = getType(left);
		String rightType = getType(right);
		if (leftType.equals(rightType)) {
			if ("boolean".equals(leftType))
				return (left.asBoolean().value == right.asBoolean().value);
			else if ("number".equals(leftType))
				return (left.asNumber().value == right.asNumber().value);
			else if ("array".equals(leftType)) {
				GPathArray leftArray = ((GPathArray) left);
				GPathArray rightArray = ((GPathArray) right);
				if (leftArray.values.size() != rightArray.values.size())
					return false;
				for (int i = 0; i < leftArray.values.size(); i++) {
					if (!equals(((GPathObject) leftArray.values.get(i)), ((GPathObject) rightArray.values.get(i))))
						return false;
				}
				return true;
			}
			else if ("map".equals(leftType)) {
				GPathMap leftMap = ((GPathMap) left);
				GPathMap rightMap = ((GPathMap) right);
				if (leftMap.values.size() != rightMap.values.size())
					return false;
				if (leftMap.values.keySet().containsAll(rightMap.values.keySet()))
					return false;
				if (rightMap.values.keySet().containsAll(leftMap.values.keySet()))
					return false;
				for (Iterator kit = leftMap.values.keySet().iterator(); kit.hasNext();) {
					String key = ((String) kit.next());
					if (!equals(((GPathObject) leftMap.values.get(key)), ((GPathObject) rightMap.values.get(key))))
						return false;
				}
				return true;
			}
		}
		//	TODO handle null
		return left.asString().value.equals(right.asString().value);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			return new GPathBoolean(MarkupScript.compare(left, right) < 0);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			return new GPathBoolean(MarkupScript.compare(left, right) <= 0);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			return new GPathBoolean(MarkupScript.compare(left, right) >= 0);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			return new GPathBoolean(MarkupScript.compare(left, right) > 0);
		}
	}
	
	private static int compare(GPathObject left, GPathObject right) {
		String leftType = getType(left);
		String rightType = getType(right);
		if (leftType.equals(rightType)) {
			if ("boolean".equals(leftType)) {
				if (left.asBoolean().value)
					return (right.asBoolean().value ? 0 : 1);
				else return (right.asBoolean().value ? -1 : 0);
			}
			else if ("number".equals(leftType)) {
				double diff = (left.asNumber().value - right.asNumber().value);
				if (diff < 0)
					return -1;
				else if (diff == 0)
					return 0;
				else return 1;
			}
			else if ("array".equals(leftType))
				return (((GPathArray) left).values.size() - ((GPathArray) right).values.size());
			else if ("map".equals(leftType))
				return (((GPathMap) left).values.size() - ((GPathMap) right).values.size());
		}
		if ("map".equals(leftType) || "array".equals(leftType))
			throw new IllegalArgumentException("Cannot compare " + leftType + " to " + rightType);
		//	TODO handle null
		return left.asString().value.compareTo(right.asString().value);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().value);
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().value);
			return new GPathBoolean(leftBoolean && rightBoolean);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().value);
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().value);
			return new GPathBoolean(!leftBoolean || !rightBoolean);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().value);
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().value);
			return new GPathBoolean(leftBoolean || rightBoolean);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().value);
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().value);
			return new GPathBoolean(!leftBoolean && !rightBoolean);
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
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
		GPathObject applyTo(GPathObject left, GPathObject right) {
			//	TODO handle null
			String leftType = getType(left);
			String rightType = getType(right);
			if ("boolean".equals(leftType))
				return new GPathBoolean(left.asBoolean().value || right.asBoolean().value);
			else if ("number".equals(leftType) && "number".equals(rightType))
				return new GPathNumber(left.asNumber().value + right.asNumber().value);
			else if ("array".equals(leftType) && "array".equals(rightType)) {
				((GPathArray) left).values.addAll(((GPathArray) right).values);
				return left;
			}
			else if ("array".equals(leftType)) {
				((GPathArray) left).values.add(right);
				return left;
			}
			else if ("map".equals(leftType) && "map".equals(rightType)) {
				for (Iterator kit = ((GPathMap) right).values.keySet().iterator(); kit.hasNext();) {
					Object key = kit.next();
					if (!((GPathMap) left).values.containsKey(key)) // don't overwrite keys
						((GPathMap) left).values.put(key, ((GPathMap) right).values.get(key));
				}
//				((GPathMap) left).values.putAll(((GPathMap) right).values); // TODO do overwrite values?
				return left;
			}
			else if ("map".equals(leftType) && "array".equals(rightType)) {
				for (int a = 0; a < (((GPathArray) right).values.size() - 1); a += 2) {
					GPathString key = ((GPathObject) ((GPathArray) right).values.get(a)).asString();
					GPathObject value = ((GPathObject) ((GPathArray) right).values.get(a+1));
					((GPathMap) left).values.put(key.value, value);
				}
				return left;
			}
			else if ("map".equals(leftType)) {
				((GPathMap) left).values.put(right.asString().value, new GPathBoolean(true));
				return left;
			}
			else return new GPathString(left.asString().value + right.asString().value);
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
			return "number";
		}
		GPathObject applyTo(GPathObject left, GPathObject right) {
			//	TODO handle null
			String leftType = getType(left);
			String rightType = getType(right);
			if ("number".equals(leftType) && "number".equals(rightType))
				return new GPathNumber(left.asNumber().value - right.asNumber().value);
			else if ("array".equals(leftType)) {
				((GPathArray) left).values.remove(right);
				return left;
			}
			else if ("map".equals(leftType) && "map".equals(rightType)) {
				for (Iterator kit = ((GPathMap) right).values.keySet().iterator(); kit.hasNext();) {
					String key = ((String) kit.next());
					((GPathMap) left).values.remove(key);
				}
				return left;
			}
			else if ("map".equals(leftType) && "array".equals(rightType)) {
				for (int a = 0; a < ((GPathArray) right).values.size(); a++) {
					GPathString key = ((GPathObject) ((GPathArray) right).values.get(a)).asString();
					((GPathMap) left).values.remove(key.value);
				}
				return left;
			}
			else if ("map".equals(leftType)) {
				((GPathMap) left).values.remove(right.asString().value);
				return left;
			}
			else throw new IllegalArgumentException("Cannot subtract " + rightType + " from " + leftType);
		}
	}
	
	private static class MultiplyAssign extends AssigningOperator {
		MultiplyAssign(int start, int end) {
			super(start, end, "*=");
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return ("number".equals(leftArgType) && "number".equals(leftArgType));
		}
		GPathObject applyTo(GPathObject left, GPathObject right) {
			if ((left == null) || (right == null))
				return new GPathNumber(0);
			else return new GPathNumber(left.asNumber().value * right.asNumber().value);
		}
	}
	
	private static class AndAssign extends AssigningOperator {
		AndAssign(int start, int end) {
			super(start, end, "&=");
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return "boolean".equals(leftArgType);
		}
		GPathObject applyTo(GPathObject left, GPathObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().value);
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().value);
			return new GPathBoolean(leftBoolean && rightBoolean);
		}
	}
	
	private static class OrAssign extends AssigningOperator {
		OrAssign(int start, int end) {
			super(start, end, "|=");
		}
		boolean isApplicableTo(String leftArgType, String rightArgType) {
			return "boolean".equals(leftArgType);
		}
		GPathObject applyTo(GPathObject left, GPathObject right) {
			boolean leftBoolean = ((left == null) ? false : left.asBoolean().value);
			boolean rightBoolean = ((right == null) ? false : right.asBoolean().value);
			return new GPathBoolean(leftBoolean || rightBoolean);
		}
	}
	
	private static class VariableResolver extends GPathVariableResolver {
		final VariableResolver globals;
		private VariableResolver parent;
		private HashMap declaredVariables = new HashMap();
		VariableResolver(VariableResolver globals, VariableResolver parent) {
			this.globals = globals;
			this.parent = parent;
		}
		public GPathObject getVariable(String name) {
			if (this.declaredVariables.containsKey(name))
				return super.getVariable(name);
			else if (this.parent != null)
				return this.parent.getVariable(name);
			else if (this.globals != null)
				return this.globals.getVariable(name);
			else return null; // TODO throw exception instead?
		}
		public GPathObject getVariable(String name, GPathObject def) {
			if (this.declaredVariables.containsKey(name))
				return super.getVariable(name, def);
			else if (this.parent != null)
				return this.parent.getVariable(name, def);
			else if (this.globals != null)
				return this.globals.getVariable(name, def);
			else return null; // TODO throw exception instead?
		}
		public boolean isVariableSet(String name) {
			if (this.declaredVariables.containsKey(name))
				return super.isVariableSet(name);
			else if (this.parent != null)
				return this.parent.isVariableSet(name);
			else if (this.globals != null)
				return this.globals.isVariableSet(name);
			else return false; // TODO throw exception instead?
		}
		void declareVariable(String name, String type) {
			this.declaredVariables.put(name, type);
		}
		String getVariableType(String name) {
			return ((String) this.declaredVariables.get(name));
		}
		public GPathObject setVariable(String name, GPathObject value) {
			if (this.declaredVariables.containsKey(name))
				return super.setVariable(name, value);
			else if (this.parent != null)
				return this.parent.setVariable(name, value);
			else return null; // TODO throw exception instead?
		}
		public GPathObject removeVariable(String name) {
			if (this.declaredVariables.containsKey(name))
				return super.removeVariable(name);
			else if (this.parent != null)
				return this.parent.removeVariable(name);
			else return null; // TODO throw exception instead?
		}
	}
	
	private static class PositionAwarePeekReader extends PeekReader {
		private int position = 0;
		PositionAwarePeekReader(Reader in, int lookahead) throws IOException {
			super(in, lookahead);
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
			int s = super.skipSpace();
//			super class uses peek() and read() to get rid of spaces, don't count them twice
//			if (s != -1)
//				this.position += s;
			return s;
		}
		int getPosition() {
			return this.position;
		}
	}

	private static class PositionAwarePeekInputStream extends PeekInputStream {
		private int position = 0;
		PositionAwarePeekInputStream(InputStream in, int lookahead) throws IOException {
			super(in, lookahead);
		}
		public int read() throws IOException {
			int r = super.read();
			if (r != -1)
				this.position++;
			return r;
		}
		public int read(byte[] cbuf, int off, int len) throws IOException {
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
			int s = super.skipSpace();
			if (s != -1)
				this.position += s;
			return s;
		}
		int getPosition() {
			return this.position;
		}
	}
	
	public static MarkupScript parse(Reader in) throws IOException {
		PositionAwarePeekReader pr = new PositionAwarePeekReader(in, 32);
		MarkupScript ms = new MarkupScript();
		while (pr.peek() != -1) {
			cropNext(pr, ms);
			pr.skipSpace();
		}
		return ms;
	}
	
	private static void cropNext(PositionAwarePeekReader pr, MarkupScript ms) throws IOException {
		pr.skipSpace();
		if (pr.startsWith("//", false))
			ms.addComment(cropLineComment(pr));
		else if (pr.startsWith("/*", false))
			ms.addComment(cropBlockComment(pr));
		else if (pr.startsWith("import ", false))
			ms.addImport(cropImport(pr));
		//	TODO enforce imports standing at top of file?
		else if (pr.startsWith("function ", false))
			ms.addFunction(cropFunction(pr));
		else ms.addExecutable(cropExecutable(pr));
		//	TODO allow cropping comments from within other structures
	}
	
	private static Comment cropLineComment(PositionAwarePeekReader pr) throws IOException {
		int start = pr.getPosition();
		pr.read();
		pr.read();
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
			else pr.read();
		}
		return new Comment(start, pr.getPosition());
	}
	
	private static Comment cropBlockComment(PositionAwarePeekReader pr) throws IOException {
		int start = pr.getPosition();
		pr.read();
		pr.read();
		while (pr.peek() != -1) {
			if (pr.startsWith("*/", false)) {
				pr.read();
				pr.read();
				break;
			}
			else pr.read();
		}
		return new Comment(start, pr.getPosition());
		//	TODO throw exception if stream ends without comment end marker
		//	TODO collect errors in list to facilitate highlighting more than one error
	}
	
	private static Import cropImport(PositionAwarePeekReader pr) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		pr.skip("import ".length());
		String name = cropName(pr, false, "_.");
		pr.skipSpace();
		String namespace = null;
		int nsStart = -1;
		if (pr.startsWith("as ", true)) {
			nsStart = pr.getPosition();
			pr.skip("as ".length());
			pr.skipSpace();
			namespace = cropName(pr, false, "");
		}
		pr.skipSpace();
		if (pr.peek() == ';') {
			pr.read();
			return new Import(start, pr.getPosition(), name, nsStart, namespace);
		}
		else throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
		//	TODO collect errors in list to facilitate highlighting more than one error
	}
	
	private static Function cropFunction(PositionAwarePeekReader pr) throws IOException {
		int start = pr.getPosition();
		pr.skip("function ".length());
		String name = cropName(pr, false, "_");
		pr.skipSpace();
		if (pr.peek() != '(')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO use dedicated exception classes for errors (derived from some RuntimeException derived MarkupScriptParseError class)
		
		//	read variables
		pr.read();
		pr.skipSpace();
		ArrayList args = new ArrayList();
		while ((pr.peek() != ')') && (pr.peek() != -1)) {
			int argStart = pr.getPosition();
			String argType = cropType(pr);
			pr.skipSpace();
			if (pr.peek() != '$')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
				//	TODO collect errors in list to facilitate highlighting more than one error
			int argNameStart = pr.getPosition();
			pr.read();
			if (pr.peek() <= ' ')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
				//	TODO collect errors in list to facilitate highlighting more than one error
			String argName = cropName(pr, false, "_");
			pr.skipSpace();
			args.add(new VariableDeclaration(argStart, pr.getPosition(), argType, argName, argNameStart, null));
			if (pr.peek() == ')')
				break;
			if (pr.peek() != ',')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			pr.read();
			pr.skipSpace();
		}
		if (pr.peek() != ')')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
		pr.read();
		pr.skipSpace();
		
		//	read return type
		pr.skipSpace();
		if (!pr.startsWith("=>", true))
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
		pr.read();
		pr.read();
		pr.skipSpace();
		int returnTypeStart = pr.getPosition();
		String returnType = cropType(pr);
		
		//	read function body
		pr.skipSpace();
		if (pr.peek() != '{')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
		pr.read();
		ArrayList execs = new ArrayList();
		while ((pr.peek() != '}') && (pr.peek() != -1)) {
			Executable exec = cropExecutable(pr);
			execs.add(exec);
			pr.skipSpace();
		}
		if (pr.peek() != '}')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
		pr.read();
		
		//	create function
		Function func = new Function(start, pr.getPosition(), name, ((VariableDeclaration[]) args.toArray(new VariableDeclaration[args.size()])), returnType, returnTypeStart);
		func.executables.addAll(execs);
		return func;
	}
	
	private static Executable cropExecutable(PositionAwarePeekReader pr) throws IOException {
		pr.skipSpace();
		Executable exec;
		boolean expectSemicolon = true;
		if (pr.startsWith("for ", true)) {
			exec = cropForLoop(pr);
			expectSemicolon = false;
		}
		else if (pr.startsWith("while ", true)) {
			exec = cropWhileLoop(pr);
			expectSemicolon = false;
		}
		else if (pr.startsWith("if ", true)) {
			exec = cropIfBlock(pr);
			expectSemicolon = false;
		}
		else if (pr.peek() == '$')
			exec = cropVariableAssignment(pr, ";");
		else if (pr.peek() == ';')
			exec = new Empty(pr.getPosition());
		else if (pr.startsWith("break ", true) || pr.startsWith("break;", true)) {
			int start = pr.getPosition();
			pr.skip("break".length());
			exec = new Break(start, pr.getPosition());
			pr.skipSpace();
		}
		else if (pr.startsWith("continue ", true) || pr.startsWith("continue;", true)) {
			int start = pr.getPosition();
			pr.skip("continue".length());
			exec = new Continue(start, pr.getPosition());
		}
		else if (pr.startsWith("return;", true)) {
			int start = pr.getPosition();
			pr.skip("return".length());
			exec = new Return(start, pr.getPosition(), null);
		}
		else if (pr.startsWith("return ", true)) {
			int start = pr.getPosition();
			pr.skip("return".length());
			pr.skipSpace();
			if (pr.peek() == ';')
				exec = new Return(start, pr.getPosition(), null);
			else exec = new Return(start, pr.getPosition(), cropExpression(pr, false, ";"));
		}
		else if (pr.startsWith("boolean ", true))
			exec = cropVariableDeclaration(pr);
		else if (pr.startsWith("number ", true))
			exec = cropVariableDeclaration(pr);
		else if (pr.startsWith("string ", true))
			exec = cropVariableDeclaration(pr);
		else if (pr.startsWith("array ", true))
			exec = cropVariableDeclaration(pr);
		else if (pr.startsWith("map ", true))
			exec = cropVariableDeclaration(pr);
		else if (pr.startsWith("node ", true))
			exec = cropVariableDeclaration(pr); // TODO this or 'annot'?
		else if (pr.startsWith("annot ", true))
			exec = cropVariableDeclaration(pr); // TODO this or 'node'?
		else if (pr.startsWith("var ", true))
			exec = cropVariableDeclaration(pr);
		else if ((('a' <= pr.peek()) && (pr.peek() <= 'z')) || ('A' <= pr.peek()) && (pr.peek() <= 'Z')) {
			int start = pr.getPosition();
			String funcName = cropName(pr, true, "_");
			pr.skipSpace();
			if (pr.peek() != '(')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			pr.read();
			pr.skipSpace();
			ArrayList args = new ArrayList();
			while (pr.peek() != -1) {
				Expression arg = cropExpression(pr, true, ",)");
				if (arg != null)
					args.add(arg);
				pr.skipSpace();
				if (pr.peek() == ')')
					break;
				if (pr.peek() != ',')
					throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			}
			pr.skipSpace();
			if (pr.peek() != ')')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			pr.read();
			if (pr.peek() != ';')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			exec = new VariableAssignment(-1, -1, null, null, new FunctionCall(start, pr.getPosition(), funcName, ((Expression[]) args.toArray(new Expression[args.size()]))));
		}
		else throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
		
		if (expectSemicolon) {
			pr.skipSpace();
			if (pr.peek() != ';')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			pr.read();
		}
		return exec;
	}
	
	private static VariableDeclaration cropVariableDeclaration(PositionAwarePeekReader pr) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		String type = cropType(pr);
		pr.skipSpace();
		if (pr.peek() != '$')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		int nameStart = pr.getPosition();
		pr.read();
		if (pr.peek() <= ' ')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		String name = cropName(pr, false, "_");
		pr.skipSpace();
		Expression value = null;
		if (pr.peek() != ';') {
			if (pr.peek() != '=')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
				//	TODO collect errors in list to facilitate highlighting more than one error
			pr.read();
			value = cropExpression(pr, false, ";");
			pr.skipSpace();
		}
		if (pr.peek() != ';')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		return new VariableDeclaration(start, pr.getPosition(), type, name, nameStart, value);
	}
	
	private static VariableAssignment cropVariableAssignment(PositionAwarePeekReader pr, String stopChars) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		pr.skipSpace();
		if (pr.peek() != '$')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		pr.read();
		if (pr.peek() <= ' ')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		String name = cropName(pr, false, "_");
		pr.skipSpace();
		
		Expression index = null;
		if (pr.peek() == '[') {
			pr.read();
			index = cropExpression(pr, false, "]");
			pr.skipSpace();
			if (pr.peek() != ']')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
				//	TODO collect errors in list to facilitate highlighting more than one error
			pr.read();
		}
		int nameEnd = pr.getPosition();
		pr.skipSpace();
		
		AssigningOperator aop = cropAssigningOperator(pr);
		pr.skipSpace();
		
		Expression value = cropExpression(pr, false, stopChars);
		pr.skipSpace();
		if (stopChars.indexOf(pr.peek()) == -1)
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		return new VariableAssignment(start, pr.getPosition(), name, index, new BinaryExpression(start, pr.getPosition(), new VariableReference(start, nameEnd, name, index), aop, value));
	}
	
	private static ArrayList cropBlock(PositionAwarePeekReader pr) throws IOException {
		pr.skipSpace();
		ArrayList execs = new ArrayList();
		if (pr.peek() == '{') {
			pr.read();
			pr.skipSpace();
			while ((pr.peek() != -1)) {
				pr.skipSpace();
				if (pr.peek() == '}') {
					pr.read();
					break;
				}
				Executable exec = cropExecutable(pr);
				execs.add(exec);
				pr.skipSpace();
			}
		}
		else {
			Executable exec = cropExecutable(pr);
			execs.add(exec);
			pr.skipSpace();
		}
		return execs;
	}
	
	private static IfBlock cropIfBlock(PositionAwarePeekReader pr) throws IOException {
		int start = pr.getPosition();
		pr.skip("if".length());
		pr.skipSpace();
		if (pr.peek() != '(')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		pr.read();
		pr.skipSpace();
		
		Expression ifTest = cropExpression(pr, false, ")");
		
		if (pr.peek() != ')')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		pr.read();
		pr.skipSpace();
		
		ArrayList ifExecs = cropBlock(pr);
		
		pr.skipSpace();
		int elseStart = -1;
		Executable elseBlock = null;
		if (pr.startsWith("else ", false)) {
			elseStart = pr.getPosition();
			pr.skip("else".length());
			pr.skipSpace();
			elseBlock = cropExecutable(pr);
		}
		IfBlock ifBlock = new IfBlock(start, pr.getPosition(), ifTest, elseStart, elseBlock);
		ifBlock.executables.addAll(ifExecs);
		return ifBlock;
	}
	
	private static ForLoop cropForLoop(PositionAwarePeekReader pr) throws IOException {
		int start = pr.getPosition();
		pr.skip("for".length());
		pr.skipSpace();
		if (pr.peek() != '(')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		pr.read();
		pr.skipSpace();
		
		int forTypeStart = pr.getPosition();
		String forType = cropType(pr);
		pr.skipSpace();
		if (pr.peek() != '$')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		int forNameStart = pr.getPosition();
		pr.read();
		if (pr.peek() <= ' ')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		String forName = cropName(pr, false, "_");
		pr.skipSpace();
		
		VariableDeclaration forInitializer;
		Expression forStartValue;
		Expression forTest;
		VariableAssignment forPostBody;
		int forSetStart;
		Expression forSet;
		
		if (pr.startsWith("in ", true)) {
			forInitializer = new VariableDeclaration(forTypeStart, pr.getPosition(), forType, forName, forNameStart, null);
			forTest = null;
			forPostBody = null;
			
			forSetStart = pr.getPosition();
			pr.skip("in".length());
			pr.skipSpace();
			forSet = cropExpression(pr, false, ")");
		}
		else {
			pr.skipSpace();
			if (pr.peek() == '=') {
				pr.read();
				forStartValue = cropExpression(pr, false, ";");
				forInitializer = new VariableDeclaration(forTypeStart, pr.getPosition(), forType, forName, forNameStart, forStartValue);
			}
			else forInitializer = null;
			pr.skipSpace();
			if (pr.peek() != ';')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
				//	TODO collect errors in list to facilitate highlighting more than one error
			pr.read();
			
			pr.skipSpace();
			if (pr.peek() == ';')
				forTest = null;
			else forTest = cropExpression(pr, true, ";");
			pr.skipSpace();
			if (pr.peek() != ';')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
				//	TODO collect errors in list to facilitate highlighting more than one error
			pr.read();
			
			pr.skipSpace();
			if (pr.peek() == ')')
				forPostBody = null;
			else forPostBody = cropVariableAssignment(pr, ")");
			
			forSetStart = -1;
			forSet = null;
		}
		
		pr.skipSpace();
		if (pr.peek() != ')')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		pr.read();
		
		pr.skipSpace();
		ArrayList forExecs = cropBlock(pr);
		ForLoop forLoop = ((forSet == null) ? new ForLoop(start, pr.getPosition(), forInitializer, forTest, forPostBody) : new ForLoop(start, pr.getPosition(), forInitializer, forSetStart, forSet));
		forLoop.executables.addAll(forExecs);
		return forLoop;
	}
	
	private static WhileLoop cropWhileLoop(PositionAwarePeekReader pr) throws IOException {
		int start = pr.getPosition();
		pr.skip("while".length());
		pr.skipSpace();
		if (pr.peek() != '(')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		pr.read();
		Expression whileCond = cropExpression(pr, false, ")");
		if (pr.peek() != ')')
			throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		pr.read();
		
		pr.skipSpace();
		ArrayList whileExecs = cropBlock(pr);
		
		WhileLoop whileLoop = new WhileLoop(start, pr.getPosition(), whileCond);
		whileLoop.executables.addAll(whileExecs);
		return whileLoop;
	}
	
	private static String cropName(PositionAwarePeekReader pr, boolean allowNamespacePrefix, String nonAlphaNumericChars) throws IOException {
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
			else if ((name.length() != 0) && (nonAlphaNumericChars != null) && (nonAlphaNumericChars.indexOf(ch) != -1))
				name.append(ch);
			else if ((name.length() != 0) && ('0' <= ch) && (ch <= '9'))
				name.append(ch);
			else if ((name.length() != 0) && (ch == ':') && allowNamespacePrefix) {
				name.append(ch);
				allowNamespacePrefix = false;
			}
			else break;
			pr.read(); // consume last appended character
		}
		System.out.println("Name: '" + name + "'");
		return name.toString();
	}
	
	private static String cropType(PositionAwarePeekReader pr) throws IOException {
		pr.skipSpace();
		String type;
		if (pr.startsWith("boolean ", true))
			type = "boolean";
		else if (pr.startsWith("number ", true))
			type = "number";
		else if (pr.startsWith("string ", true))
			type = "string";
		else if (pr.startsWith("array ", true))
			type = "array";
		else if (pr.startsWith("map ", true))
			type = "map";
		else if (pr.startsWith("node ", true))
			type = "node"; // TODO this or 'annot'?
		else if (pr.startsWith("annot ", true))
			type = "annot"; // TODO this or 'node'?
		else if (pr.startsWith("var ", true))
			type = "var";
		else throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
		//	TODO collect errors in list to facilitate highlighting more than one error
		pr.skip(type.length());
		System.out.println("Type '" + type + "'");
		return type;
	}
	
	private static String getType(GPathObject obj) {
		if (obj instanceof GPathArray)
			return "array";
//		else if (obj instanceof GPathAnnotationSet) // TODO build that class in the first place
//			return "node"; // TODO this or 'annot'?
		else if (obj instanceof GPathMap)
			return "map";
		else if (obj instanceof GPathString)
			return "string";
		else if (obj instanceof GPathNumber)
			return "number";
		else if (obj instanceof GPathBoolean)
			return "boolean";
		else if (obj instanceof GPathMap)
			return "map";
		else return "var";
	}
	
	private static Expression cropExpression(PositionAwarePeekReader pr, boolean allowEmpty, String stopChars) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		
		//	check for empty expression
		pr.skipSpace();
		if (stopChars.indexOf(pr.peek()) != -1) {
			if (allowEmpty)
				return null;
			else throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		}
		
		//	crop first unary expression (also handles boolean not and numerical minus) ...
		Expression leftExp = cropUnaryExpression(pr);
		pr.skipSpace();
		
		//	... and simply return it if we hit the overall end
		if (stopChars.indexOf(pr.peek()) != -1)
			return leftExp;
		
		//	crop operator
		Operator op = cropOperator(pr);
		pr.skipSpace();
		
		//	recurse to crop right expression
		Expression rightExp = cropExpression(pr, false, stopChars);
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
	
	private static Expression cropUnaryExpression(PositionAwarePeekReader pr) throws IOException {
		pr.skipSpace();
		int start = pr.getPosition();
		if (pr.peek() == '$') {
			pr.read();
			String name = cropName(pr, false, "_");
			if (pr.peek() == '[') {
				Expression index = cropExpression(pr, false, "]");
				if (pr.peek() == ']') {
					pr.read();
					return new VariableReference(start, pr.getPosition(), name, index);
				}
				else throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
				//	TODO collect errors in list to facilitate highlighting more than one error
			}
			else return new VariableReference(start, pr.getPosition(), name);
		}
		else if (pr.peek() == '\'') {
			String str = ((String) JsonParser.parseJson(pr));
			return new Literal(start, pr.getPosition(), new GPathString(str));
		}
		else if (pr.peek() == '"') {
			String str = ((String) JsonParser.parseJson(pr));
			return new Literal(start, pr.getPosition(), new GPathString(str));
		}
		else if (pr.peek() == '(') {
			pr.read();
			pr.skipSpace();
			Expression exp = cropExpression(pr, false, ")");
			pr.skipSpace();
			if (pr.peek() == ')') {
				pr.read();
				return new ParenthesisExpression(start, pr.getPosition(), exp);
			}
			else throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
			//	TODO collect errors in list to facilitate highlighting more than one error
		}
		else if (pr.peek() == '[') {
			List array = ((List) JsonParser.parseJson(pr));
			return new Literal(start, pr.getPosition(), map(array));
		}
		else if (pr.peek() == '{') {
			Map map = ((Map) JsonParser.parseJson(pr));
			return new Literal(start, pr.getPosition(), map(map));
		}
		else if (pr.peek() == '!') {
			Operator op = new BooleanNot(pr.getPosition(), (pr.getPosition() + "!".length()));
			pr.read();
			Expression toNegate = cropUnaryExpression(pr);
			return new BinaryExpression(start, pr.getPosition(), new Literal(-1, -1, new GPathBoolean(false)), op, toNegate);
		}
		else if (pr.startsWith("false", true) && terminatesBoolean(pr.peek("false".length()))) {
			pr.skip("false".length());
			return new Literal(start, pr.getPosition(), new GPathBoolean(false));
		}
		else if (pr.startsWith("true", true) && terminatesBoolean(pr.peek("true".length()))) {
			pr.skip("true".length());
			return new Literal(start, pr.getPosition(), new GPathBoolean(true));
		}
		else if (pr.peek() == '-') {
			Operator op = new NumberMinus(pr.getPosition(), (pr.getPosition() + "-".length()));
			pr.read();
			Expression toNegate = cropUnaryExpression(pr);
			return new BinaryExpression(start, pr.getPosition(), new Literal(-1, -1, new GPathNumber(0)), op, toNegate);
		}
		else if (('0' <= pr.peek()) && (pr.peek() <= '9')) {
			Number number = ((Number) JsonParser.parseJson(pr));
			return new Literal(start, pr.getPosition(), new GPathNumber(number.doubleValue()));
		}
		else if ((('a' <= pr.peek()) && (pr.peek() <= 'z')) || ('A' <= pr.peek()) && (pr.peek() <= 'Z')) {
			String funcName = cropName(pr, true, "_");
			pr.skipSpace();
			if (pr.peek() != '(')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
				//	TODO collect errors in list to facilitate highlighting more than one error
			pr.read();
			pr.skipSpace();
			ArrayList args = new ArrayList();
			while ((pr.peek() != ')') && (pr.peek() != -1)) {
				Expression arg = cropExpression(pr, false, ",)");
				args.add(arg);
				pr.skipSpace();
				if (pr.peek() == ')')
					break;
				if (pr.peek() != ',')
					throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
					//	TODO collect errors in list to facilitate highlighting more than one error
				pr.read();
				pr.skipSpace();
				if (pr.peek() == ')')
					throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
					//	TODO collect errors in list to facilitate highlighting more than one error
			}
			pr.skipSpace();
			if (pr.peek() != ')')
				throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
				//	TODO collect errors in list to facilitate highlighting more than one error
			pr.read();
			return new FunctionCall(start, pr.getPosition(), funcName, ((Expression[]) args.toArray(new Expression[args.size()])));
		}
		else throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
		//	TODO collect errors in list to facilitate highlighting more than one error
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
	
	private static Operator cropOperator(PositionAwarePeekReader pr) throws IOException {
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
		else if (pr.startsWith("div ", true)) {
			pr.skip("div".length()); // TODO consider supporting Java syntax as well
			return new Divide(start, pr.getPosition());
		}
		else if (pr.startsWith("mod ", true)) {
			pr.skip("mod".length()); // TODO consider supporting Java syntax as well
			return new Modulo(start, pr.getPosition());
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
		else throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
		//	TODO collect errors in list to facilitate highlighting more than one error
	}
	
	private static AssigningOperator cropAssigningOperator(PositionAwarePeekReader pr) throws IOException {
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
		else if (pr.startsWith("&=", true)) {
			pr.skip("&=".length());
			return new AndAssign(start, pr.getPosition());
		}
		else if (pr.startsWith("|=", true)) {
			pr.skip("|=".length());
			return new OrAssign(start, pr.getPosition());
		}
		else throw new RuntimeException("Unexpected character '" + ((char) pr.peek()) + "' at " + pr.getPosition());
		//	TODO collect errors in list to facilitate highlighting more than one error
	}
	
	private static GPathObject map(Object obj) {
		if (obj instanceof String)
			return new GPathString((String) obj);
		else if (obj instanceof Integer)
			return new GPathNumber(((Number) obj).intValue());
		else if (obj instanceof Number)
			return new GPathNumber(((Number) obj).doubleValue());
		else if (obj instanceof Boolean)
			return new GPathBoolean(((Boolean) obj).booleanValue());
		else if (obj instanceof List) {
			GPathArray array = new GPathArray();
			for (int i = 0; i < ((List) obj).size(); i++)
				array.values.set(i, map(((List) obj).get(i)));
			return array;
		}
		else if (obj instanceof Map) {
			GPathMap map = new GPathMap();
			for (Iterator kit = ((Map) obj).keySet().iterator(); kit.hasNext();) {
				Object key = kit.next();
				map.values.put(key.toString(), map(((Map) obj).get(key)));
			}
			return map;
		}
		else throw new RuntimeException("Cannot map " + obj);
	}
	
	private HashMap functions = new HashMap();
	private ArrayList functionDuplicates = new ArrayList();
	
	private ArrayList imports = new ArrayList();
	
	private ArrayList executables = new ArrayList();
	
	private ArrayList comments = new ArrayList();
	
	private ArrayList parts = new ArrayList();
	
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
		Function eFunc = ((Function) this.functions.get(func.name));
		if (eFunc == null)
			this.functions.put(func.name, func);
		else {
			this.functionDuplicates.add(eFunc);
			this.functionDuplicates.add(func);
		}
		this.parts.add(func);
	}
	
	void addExecutable(Executable exec) {
		this.executables.add(exec);
		this.parts.add(exec);
	}
	
	void printString() {
		for (int p = 0; p < this.parts.size(); p++)
			((Part) this.parts.get(p)).printString("");
	}
	
	void styleCode(StyledDocument sd) {
		for (int p = 0; p < this.parts.size(); p++)
			((Part) this.parts.get(p)).styleCode(sd);
	}
	
	/**
	 * Execute the markup script on a GAMTA document or part thereof
	 * @param data the document to execute the script upon
	 */
	public void execute(MutableAnnotation data) {
		//	TODO implement this
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String msStr = "" +
				"import something.ms;\r\n" +
				"import something.further.ms as frt;\r\n" +
				"// this should explain below function ...\r\n" +
				"function factorial(number $n) => number {\r\n" +
				"  if ($n <= 1)\r\n" +
				"    return $n;\r\n" +
				"  else return ($n * factorial($n - 1));\r\n" +
				"}\r\n" +
				"/* ... and this should \r\nexplain above function */\r\n" +
				"number $n1 = factorial(1);\r\n" +
				"$n1 = factorial(2);\r\n" +
				"$n1 = factorial(3);\r\n" +
				"for (number $n = 0; $n < 5; $n += 1) {\r\n" +
				"  $n = factorial($n - 1);\r\n" +
				"}\r\n" +
				"string $strSq = 'this is a string in single quotes';\r\n" +
				"string $strDq = \"this is a string in double quotes\";\r\n" +
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
//		MarkupScript ms = parse(new StringReader(msStr.replaceAll("\\r\\n", "\r")));
		MarkupScript ms = parse(new StringReader(msStr));
		ms.printString();
		JTextPane tp = new JTextPane();
		tp.setFont(Font.getFont("Courier"));
		StyledDocument sd = tp.getStyledDocument();
		sd.insertString(sd.getLength(), msStr, null); // this way we get no penalty from the cross-platform line breaks
//		tp.setText(msStr);
		ms.styleCode(tp.getStyledDocument());
//		tp.getStyledDocument().setCharacterAttributes(0, "import".length(), STYLE_KEYWORD, true);
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
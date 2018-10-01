package uk.ac.bham.cs.schimp.exec;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;

public class VariableScopeFrame implements Cloneable {
	
	public enum Type {
		GLOBAL,
		FUNCTION,
		BLOCK
	}
	
	private Type type;
	private Map<String, ArithmeticConstant> scopeFrame = new HashMap<String, ArithmeticConstant>();
	
	public VariableScopeFrame(Type type) {
		this.type = type;
	}
	
	private VariableScopeFrame(Type type, Map<String, ArithmeticConstant> scopeFrame) {
		this.type = type;
		this.scopeFrame = scopeFrame;
	}
	
	public Type getType() {
		return type;
	}
	
	public boolean isDefined(String variableName) {
		return scopeFrame.containsKey(variableName);
	}
	
	public void define(String variableName, ArithmeticConstant value) throws ProgramExecutionException {
		if (scopeFrame.containsKey(variableName)) {
			throw new ProgramExecutionException("cannot declare variable '" + variableName + "': variable is already defined in this scope");
		} else {
			scopeFrame.put(variableName, value);
		}
	}
	
	public void assign(String variableName, ArithmeticConstant value) throws ProgramExecutionException {
		if (scopeFrame.containsKey(variableName)) {
			scopeFrame.put(variableName, value);
		} else {
			throw new ProgramExecutionException("cannot assign value to variable '" + variableName + "': variable is undefined here");
		}
	}
	
	public void clear() {
		scopeFrame.clear();
	}
	
	public ArithmeticConstant evaluate(String variableName) throws ProgramExecutionException {
		if (scopeFrame.containsKey(variableName)) {
			return scopeFrame.get(variableName);
		} else {
			throw new ProgramExecutionException("cannot assign value to variable '" + variableName + "': variable is undefined here");
		}
	}
	
	@Override
	public VariableScopeFrame clone() {
		return new VariableScopeFrame(
			type, // VariableScopeFrame.Type - safe to reuse reference
			scopeFrame.entrySet().stream()
				.collect(Collectors.<Map.Entry<String, ArithmeticConstant>, String, ArithmeticConstant>toMap(
					e -> e.getKey(), // String - safe to reuse reference
					e -> e.getValue().clone() // ArithmeticConstant - needs to be cloned
				))
		);
	}
	
	private String indentation(int indent) {
		return StringUtils.repeat("  ", indent);
	}
	
	@Override
	public String toString() {
		return this.toString(0);
	}
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("VariableScopeFrame[" + type + "]: {");
		s.append(
			scopeFrame.keySet().stream()
				.sorted()
				.map(v -> v + "=" + scopeFrame.get(v).toSourceString())
				.collect(Collectors.joining(" "))
		);
		s.append("}");
		
		return s.toString();
	}
	
	public String toShortString() {
		return "{" +
			scopeFrame.keySet().stream()
				.sorted()
				.map(v -> v + "=" + scopeFrame.get(v).toSourceString())
				.collect(Collectors.joining(" "))
			+ "}";
	}

}

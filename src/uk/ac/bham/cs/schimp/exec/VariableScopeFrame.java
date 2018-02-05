package uk.ac.bham.cs.schimp.exec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class VariableScopeFrame {
	
	public enum Type {
		GLOBAL,
		FUNCTION,
		BLOCK
	}
	
	private Type type;
	private Map<String, Integer> scopeFrame = new HashMap<String, Integer>();
	
	public VariableScopeFrame(Type type) {
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
	
	public boolean isDefined(String variableName) {
		return scopeFrame.containsKey(variableName);
	}
	
	public void define(String variableName, int stateIndex) throws ProgramExecutionException {
		if (scopeFrame.containsKey(variableName)) {
			throw new ProgramExecutionException("cannot declare variable '" + variableName + "': variable is already defined in this scope");
		} else {
			scopeFrame.put(variableName, stateIndex);
		}
	}
	
	public int getStateIndex(String variableName) throws ProgramExecutionException {
		if (scopeFrame.containsKey(variableName)) {
			return scopeFrame.get(variableName);
		} else {
			throw new ProgramExecutionException("cannot get state index of variable '" + variableName + "': variable is undefined here");
		}
	}
	
	public int[] getStateIndices() {
		int[] indices = scopeFrame.values().stream()
			.mapToInt(i -> i)
			.toArray();
		
		Arrays.sort(indices);
		return indices;
	}
	
	/*
	public void assign(String variableName, int stateIndex) throws ProgramExecutionException {
		if (scopeFrame.containsKey(variableName)) {
			scopeFrame.put(variableName, stateIndex);
		} else {
			throw new ProgramExecutionException("cannot assign value to variable '" + variableName + "': variable is undefined here");
		}
	}
	*/
	
	public void clear() {
		scopeFrame.clear();
	}
	
	/*
	public ArithmeticConstant evaluate(String variableName) throws ProgramExecutionException {
		if (scopeFrame.containsKey(variableName)) {
			return scopeFrame.get(variableName);
		} else {
			throw new ProgramExecutionException("cannot assign value to variable '" + variableName + "': variable is undefined here");
		}
	}
	*/
	
	public String toString() {
		return
			"  VariableScopeFrame[" + type + "]: {" +
			scopeFrame.keySet().stream()
				.sorted()
				.map(v -> v + "=" + scopeFrame.get(v))
				.collect(Collectors.joining(" ")) +
			"}";
	}

}

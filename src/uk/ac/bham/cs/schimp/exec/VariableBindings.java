package uk.ac.bham.cs.schimp.exec;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class VariableBindings {
	
	private List<VariableScopeFrame> scopeFrames = new LinkedList<VariableScopeFrame>();
	private List<VariableScopeFrame> currentScope;
	private int nextStateIndex;
	
	public VariableBindings(int startStateIndex) {
		nextStateIndex = startStateIndex;
		createGlobalScopeFrame();
	}
	
	private void createGlobalScopeFrame() {
		// the global scope frame has no parent
		VariableScopeFrame frame = new VariableScopeFrame(VariableScopeFrame.Type.GLOBAL);
		scopeFrames.add(frame);
		currentScope = new LinkedList<>(Arrays.asList(frame));
	}
	
	// TODO: nothing else can be called after this method has been called for the first time
	public int[] destroyGlobalScopeFrame() {
		// remove the global scope frame, as well as any remaining function or block scope frames (this should never
		// happen while executing a schimp program: if it does, there's a bug in some code that uses this class)
		// TODO: actually handle this possibility by throwing an exception
		while (scopeFrames.get(0).getType() == VariableScopeFrame.Type.BLOCK) {
			scopeFrames.remove(0);
		}
		return scopeFrames.remove(0).getStateIndices(); // VariableScopeFrame.Type.GLOBAL
	}
	
	public void createFunctionScopeFrame() {
		VariableScopeFrame frame = new VariableScopeFrame(VariableScopeFrame.Type.FUNCTION);
		scopeFrames.add(0, frame);
		
		// creating a new function scope frame changes the current scope to the global scope and the new function scope
		currentScope = new LinkedList<>(Arrays.asList(frame, scopeFrames.get(scopeFrames.size() - 1)));
	}
	
	public int[] destroyFunctionScopeFrame() {
		// remove the current function scope frame, as well as any block scope frames that belong to that function (this
		// should never happen while executing a schimp program: if it does, there's a bug in some code that uses this
		// class)
		// TODO: actually handle this possibility by throwing an exception
		while (scopeFrames.get(0).getType() == VariableScopeFrame.Type.BLOCK) {
			scopeFrames.remove(0);
		}
		VariableScopeFrame destroyedScopeFrame = scopeFrames.remove(0); // VariableScopeFrame.Type.FUNCTION
		
		// destroying the current function scope frame changes the current scope to that of the previously-called
		// function
		currentScope = new LinkedList<>();
		int i = -1;
		while (scopeFrames.get(++i).getType() == VariableScopeFrame.Type.BLOCK) {
			currentScope.add(scopeFrames.get(i));
		}
		if (scopeFrames.get(i).getType() == VariableScopeFrame.Type.FUNCTION) currentScope.add(scopeFrames.get(i)); // VariableScopeFrame.Type.FUNCTION
		currentScope.add(scopeFrames.get(scopeFrames.size() - 1)); // VariableScopeFrame.Type.GLOBAL
		
		return destroyedScopeFrame.getStateIndices();
	}
	
	public void createBlockScopeFrame() {
		VariableScopeFrame frame = new VariableScopeFrame(VariableScopeFrame.Type.BLOCK);
		scopeFrames.add(0, frame);
		currentScope.add(0, frame);
	}
	
	public int[] destroyBlockScopeFrame() {
		scopeFrames.remove(0);
		return currentScope.remove(0).getStateIndices();
	}
	
	public boolean isDefined(String variableName) {
		return currentScope.stream()
			.filter(f -> f.isDefined(variableName))
			.findFirst()
			.isPresent();
	}
	
	public int define(String variableName) throws ProgramExecutionException {
		currentScope.get(0).define(variableName, nextStateIndex);
		return nextStateIndex++;
	}
	
	public int getTotalStateIndices() {
		return nextStateIndex - 1;
	}
	
	public int getStateIndex(String variableName) throws ProgramExecutionException {
		try {
			return currentScope.stream()
				.filter(f -> f.isDefined(variableName))
				.findFirst()
				.get() // scope frame
				.getStateIndex(variableName); // variable mapping within scope frame (String)
		} catch (NoSuchElementException e) {
			throw new ProgramExecutionException("cannot refer to variable '" + variableName + "': variable is undefined here");
		}
	}
	
	/*
	public void assign(String variableName, ArithmeticConstant value) throws ProgramExecutionException {
		try {
			currentScope.stream()
				.filter(f -> f.isDefined(variableName))
				.findFirst()
				.get() // scope frame
				.assign(variableName, value); // variable mapping within scope frame (String)
		} catch (NoSuchElementException e) {
			throw new ProgramExecutionException("cannot assign value to variable '" + variableName + "': variable is undefined here");
		}
	}
	*/
	
	/*
	public ArithmeticConstant evaluate(String variableName) throws ProgramExecutionException {
		try {
			return currentScope.stream()
				.filter(f -> f.isDefined(variableName))
				.findFirst()
				.get() // scope frame
				.evaluate(variableName); // variable mapping within scope frame (String)
		} catch (NoSuchElementException e) {
			throw new ProgramExecutionException("cannot evaluate variable '" + variableName + "': variable is undefined here");
		}
	}
	*/
	
	public String toString() {
		return
			"VariableBindings: [\n" +
			currentScope.stream().map(s -> s.toString()).collect(Collectors.joining("\n")) +
			"\n]";
	}

}

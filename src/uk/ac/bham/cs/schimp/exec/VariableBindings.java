package uk.ac.bham.cs.schimp.exec;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;

public class VariableBindings implements Cloneable {
	
	private List<VariableScopeFrame> scopeFrames = new LinkedList<VariableScopeFrame>();
	private List<VariableScopeFrame> currentScope;
	
	public VariableBindings() {
		createGlobalScopeFrame();
	}
	
	private VariableBindings(List<VariableScopeFrame> scopeFrames, List<VariableScopeFrame> currentScope) {
		this.scopeFrames = scopeFrames;
		this.currentScope = currentScope;
	}
	
	private void createGlobalScopeFrame() {
		// the global scope frame has no parent
		VariableScopeFrame frame = new VariableScopeFrame(VariableScopeFrame.Type.GLOBAL);
		scopeFrames.add(frame);
		currentScope = new LinkedList<>(Arrays.asList(frame));
	}
	
	/*
	// TODO: nothing else can be called after this method has been called for the first time
	public void destroyGlobalScopeFrame() {
		// remove the global scope frame, as well as any remaining function or block scope frames (this should never
		// happen while executing a schimp program: if it does, there's a bug in some code that uses this class)
		// TODO: actually handle this possibility by throwing an exception
		while (scopeFrames.get(0).getType() == VariableScopeFrame.Type.BLOCK) {
			scopeFrames.remove(0);
		}
		scopeFrames.remove(0); // VariableScopeFrame.Type.GLOBAL
	}
	*/
	
	public void createFunctionScopeFrame() {
		VariableScopeFrame frame = new VariableScopeFrame(VariableScopeFrame.Type.FUNCTION);
		scopeFrames.add(0, frame);
		
		// creating a new function scope frame changes the current scope to the global scope and the new function scope
		currentScope = new LinkedList<>(Arrays.asList(frame, scopeFrames.get(scopeFrames.size() - 1)));
	}
	
	public void destroyFunctionScopeFrame() {
		// remove the current function scope frame, as well as any block scope frames that belong to that function (this
		// should never happen while executing a schimp program: if it does, there's a bug in some code that uses this
		// class)
		// TODO: actually handle this possibility by throwing an exception
		while (scopeFrames.get(0).getType() == VariableScopeFrame.Type.BLOCK) {
			scopeFrames.remove(0);
		}
		scopeFrames.remove(0); // VariableScopeFrame.Type.FUNCTION
		
		// destroying the current function scope frame changes the current scope to that of the previously-called
		// function
		currentScope = new LinkedList<>();
		int i = -1;
		while (scopeFrames.get(++i).getType() == VariableScopeFrame.Type.BLOCK) {
			currentScope.add(scopeFrames.get(i));
		}
		if (scopeFrames.get(i).getType() == VariableScopeFrame.Type.FUNCTION) currentScope.add(scopeFrames.get(i)); // VariableScopeFrame.Type.FUNCTION
		currentScope.add(scopeFrames.get(scopeFrames.size() - 1)); // VariableScopeFrame.Type.GLOBAL
	}
	
	public void createBlockScopeFrame() {
		VariableScopeFrame frame = new VariableScopeFrame(VariableScopeFrame.Type.BLOCK);
		scopeFrames.add(0, frame);
		currentScope.add(0, frame);
	}
	
	public void destroyBlockScopeFrame() {
		scopeFrames.remove(0);
		currentScope.remove(0);
	}
	
	public boolean isDefined(String variableName) {
		return currentScope.stream()
			.filter(f -> f.isDefined(variableName))
			.findFirst()
			.isPresent();
	}
	
	public void define(String variableName, ArithmeticConstant value) throws ProgramExecutionException {
		currentScope.get(0).define(variableName, value);
	}
	
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
	
	public ArithmeticConstant evaluateInitial(String variableName) throws ProgramExecutionException {
		try {
			return scopeFrames
				.get(scopeFrames.size() - 1) // initial variables are always declared in the global scope frame
				.evaluate(variableName);
		} catch (NoSuchElementException e) {
			throw new ProgramExecutionException("cannot evaluate initial variable '" + variableName + "': variable is undefined here");
		}
	}
	
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
	
	@Override
	public VariableBindings clone() {
		List<VariableScopeFrame> clonedScopeFrames = new LinkedList<VariableScopeFrame>();
		List<VariableScopeFrame> clonedCurrentScope = new LinkedList<VariableScopeFrame>();
		
		int c = 0;
		for (int s = 0; s < scopeFrames.size(); s++) {
			clonedScopeFrames.add(scopeFrames.get(s).clone());
			
			if ((c < currentScope.size()) && (scopeFrames.get(s) == currentScope.get(c))) {
				clonedCurrentScope.add(clonedScopeFrames.get(s));
				c++;
			}
		}
		
		return new VariableBindings(clonedScopeFrames, clonedCurrentScope);
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
		s.append("VariableBindings: [\n");
		
		s.append(indentation(indent + 1));
		s.append("scopeFrames: [\n");
		s.append(scopeFrames.stream().map(sf -> sf.toString(indent + 2)).collect(Collectors.joining("\n")));
		s.append("\n");
		s.append(indentation(indent + 1));
		s.append("]\n");
		
		s.append(indentation(indent + 1));
		s.append("currentScope: [\n");
		s.append(currentScope.stream().map(sf -> sf.toString(indent + 2)).collect(Collectors.joining("\n")));
		s.append("\n");
		s.append(indentation(indent + 1));
		s.append("]\n");
		
		s.append(indentation(indent));
		s.append("]");
		
		return s.toString();
	}
	
	/*
	public static void main(String[] args) throws ProgramExecutionException {
		VariableBindings b = new VariableBindings();
		b.define("g1", new ArithmeticConstant(1));
		b.define("g2", new ArithmeticConstant(2));
		b.createFunctionScopeFrame();
		b.define("f1_1", new ArithmeticConstant(11));
		b.define("f1_2", new ArithmeticConstant(12));
		b.createBlockScopeFrame();
		b.define("f1_block1_1", new ArithmeticConstant(111));
		b.define("f1_block1_2", new ArithmeticConstant(112));
		b.createBlockScopeFrame();
		b.define("f1_block2_1", new ArithmeticConstant(121));
		b.define("f1_block2_2", new ArithmeticConstant(122));
		b.createFunctionScopeFrame();
		b.define("f2_1", new ArithmeticConstant(11));
		b.define("f2_2", new ArithmeticConstant(12));
		b.createBlockScopeFrame();
		b.define("f2_block1_1", new ArithmeticConstant(211));
		b.define("f2_block1_2", new ArithmeticConstant(212));
		b.createBlockScopeFrame();
		b.define("f2_block2_1", new ArithmeticConstant(221));
		b.define("f2_block2_2", new ArithmeticConstant(222));
		
		System.out.println(b.toString());
		
		VariableBindings bClone = b.clone();
		System.out.println(bClone.toString());
	}
	*/

}

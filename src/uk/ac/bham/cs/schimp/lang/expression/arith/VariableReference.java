package uk.ac.bham.cs.schimp.lang.expression.arith;

import parser.State;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class VariableReference extends ArithmeticExpression {
	
	private String name;
	private int stateIndex = Integer.MIN_VALUE;
	
	public VariableReference(String name) {
		super();
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		// a variable with this name must be in scope here; if one is, record its index in the State array so its
		// current value can be looked up during execution
		try {
			stateIndex = context.variableBindings.getStateIndex(name);
		} catch (ProgramExecutionException e) {
			throw new SyntaxException("variable '" + name + "' is undefined here");
		}
	}
	
	@Override
	public ArithmeticConstant evaluate(State state) {
		return new ArithmeticConstant((int)state.varValues[stateIndex]);
	}
	
	public String toString(int indent) {
		return indentation(indent) + name + "<" + stateIndex + ">";
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + name;
	}
	
}

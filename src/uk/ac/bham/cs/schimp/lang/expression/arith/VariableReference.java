package uk.ac.bham.cs.schimp.lang.expression.arith;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class VariableReference extends ArithmeticExpression {
	
	private String name;
	
	public VariableReference(String name) {
		super();
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		// a variable with this name must be in scope here
		if (!context.variableBindings.isDefined(name)) {
			throw new SyntaxException("variable '" + name + "' is undefined here");
		}
	}
	
	@Override
	public ArithmeticConstant evaluate(ProgramExecutionContext context) throws EvaluationException {
		try {
			return context.variableBindings.evaluate(name);
		} catch (ProgramExecutionException e) {
			// this should never happen: if the syntax-checking phase succeeds, it guarantees that variables are always
			// in scope
			throw new EvaluationException("variable '" + name + "' cannot be evaluated here");
		}
	}
	
	public String toString(int indent) {
		return indentation(indent) + name;
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + name;
	}
	
}

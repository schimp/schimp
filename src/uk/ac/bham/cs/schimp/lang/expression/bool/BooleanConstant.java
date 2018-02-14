package uk.ac.bham.cs.schimp.lang.expression.bool;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class BooleanConstant extends BooleanExpression {
	
	private boolean constant;
	
	public BooleanConstant(boolean constant) {
		super();
		this.constant = constant;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {}
	
	@Override
	public BooleanConstant evaluate(ProgramExecutionContext context) throws EvaluationException {
		return this;
	}
	
	public boolean toBoolean() {
		return constant;
	}
	
	public String toString(int indent) {
		return indentation(indent) + String.valueOf(constant);
	}

	public String toSourceString(int indent) {
		return indentation(indent) + String.valueOf(constant);
	}

}

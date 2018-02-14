package uk.ac.bham.cs.schimp.lang.expression.bool;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class BooleanNotOperation extends BooleanExpression {
	
	private BooleanExpression exp;
	
	public BooleanNotOperation(BooleanExpression exp) {
		super();
		this.exp = exp;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		exp.check(context);
	}
	
	@Override
	public BooleanConstant evaluate(ProgramExecutionContext context) throws EvaluationException {
		return new BooleanConstant(!exp.evaluate(context).toBoolean());
	}
	
	public String toString(int indent) {
		return indentation(indent) + "(not " + exp.toString() + ")";
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "(not " + exp.toSourceString() + ")";
	}
	
}

package uk.ac.bham.cs.schimp.lang.expression.bool;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class BooleanOrOperation extends BooleanExpression {
	
	private BooleanExpression left;
	private BooleanExpression right;
	
	public BooleanOrOperation(BooleanExpression left, BooleanExpression right) {
		super();
		this.left = left;
		this.right = right;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		left.check(context);
		right.check(context);
	}
	
	@Override
	public BooleanConstant evaluate(ProgramExecutionContext context) throws EvaluationException {
		return new BooleanConstant(left.evaluate(context).toBoolean() || right.evaluate(context).toBoolean());
	}
	
	public String toString(int indent) {
		return indentation(indent) + "(" + left.toString() + " or " + right.toString() + ")";
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "(" + left.toSourceString() + " or " + right.toSourceString() + ")";
	}
	
}

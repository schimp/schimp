package uk.ac.bham.cs.schimp.lang.expression.arith;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class SubtractOperation extends ArithmeticExpression {
	
	private ArithmeticExpression left;
	private ArithmeticExpression right;
	
	public SubtractOperation(ArithmeticExpression left, ArithmeticExpression right) {
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
	public ArithmeticConstant evaluate(ProgramExecutionContext context) throws EvaluationException {
		return new ArithmeticConstant(left.evaluate(context).toFraction().subtract(right.evaluate(context).toFraction()));
	}
	
	public String toString(int indent) {
		return indentation(indent) + "(" + left.toString() + " - " + right.toString() + ")";
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "(" + left.toSourceString() + " - " + right.toSourceString() + ")";
	}
	
}

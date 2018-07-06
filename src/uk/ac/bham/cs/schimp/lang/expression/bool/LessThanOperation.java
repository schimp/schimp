package uk.ac.bham.cs.schimp.lang.expression.bool;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpression;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class LessThanOperation extends BooleanExpression {
	
	private ArithmeticExpression left;
	private ArithmeticExpression right;
	
	public LessThanOperation(ArithmeticExpression left, ArithmeticExpression right) {
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
		return new BooleanConstant(left.evaluate(context).toFraction().compareTo(right.evaluate(context).toFraction()) == -1);
	}
	
	public String toString(int indent) {
		return indentation(indent) + "(" + left.toString() + " < " + right.toString() + ")";
	}

	public String toSourceString(int indent) {
		return indentation(indent) + "(" + left.toSourceString() + " < " + right.toSourceString() + ")";
	}

}

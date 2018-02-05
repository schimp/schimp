package uk.ac.bham.cs.schimp.lang.expression.bool;

import parser.State;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class BooleanAndOperation extends BooleanExpression {
	
	private BooleanExpression left;
	private BooleanExpression right;
	
	public BooleanAndOperation(BooleanExpression left, BooleanExpression right) {
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
	public BooleanConstant evaluate(State state) {
		return new BooleanConstant(left.evaluate(state).toBoolean() && right.evaluate(state).toBoolean());
	}
	
	public String toString(int indent) {
		return indentation(indent) + "(" + left.toString() + " and " + right.toString() + ")";
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "(" + left.toSourceString() + " and " + right.toSourceString() + ")";
	}
	
}

package uk.ac.bham.cs.schimp.lang.expression.arith;

import parser.State;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class AddOperation extends ArithmeticExpression {
	
	private ArithmeticExpression left;
	private ArithmeticExpression right;
	
	public AddOperation(ArithmeticExpression left, ArithmeticExpression right) {
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
	public ArithmeticConstant evaluate(State state) {
		return new ArithmeticConstant(left.evaluate(state).toInteger() + right.evaluate(state).toInteger());
	}
	
	public String toString(int indent) {
		return indentation(indent) + "(" + left.toString() + " + " + right.toString() + ")";
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "(" + left.toSourceString() + " + " + right.toSourceString() + ")";
	}
	
}

package uk.ac.bham.cs.schimp.lang.expression.arith;

import parser.State;
import uk.ac.bham.cs.schimp.lang.expression.Expression;

public abstract class ArithmeticExpression extends Expression {
	
	public ArithmeticExpression() {
		super();
	}
	
	public abstract ArithmeticConstant evaluate(State state);
	
}

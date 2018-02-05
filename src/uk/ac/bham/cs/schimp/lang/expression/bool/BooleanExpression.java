package uk.ac.bham.cs.schimp.lang.expression.bool;

import parser.State;
import uk.ac.bham.cs.schimp.lang.expression.Expression;

public abstract class BooleanExpression extends Expression {
	
	public BooleanExpression() {
		super();
	}
	
	public abstract BooleanConstant evaluate(State state);
	
}

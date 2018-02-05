package uk.ac.bham.cs.schimp.lang.expression.bool;

import parser.State;
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
	public BooleanConstant evaluate(State state) {
		return new BooleanConstant(!exp.evaluate(state).toBoolean());
	}
	
	public String toString(int indent) {
		return indentation(indent) + "(not " + exp.toString() + ")";
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "(not " + exp.toSourceString() + ")";
	}
	
}

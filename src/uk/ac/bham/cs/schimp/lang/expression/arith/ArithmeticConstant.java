package uk.ac.bham.cs.schimp.lang.expression.arith;

import parser.State;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class ArithmeticConstant extends ArithmeticExpression {
	
	private int constant;
	
	public ArithmeticConstant(int constant) {
		super();
		this.constant = constant;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {}
	
	@Override
	public ArithmeticConstant evaluate(State state) {
		return this;
	}
	
	public int toInteger() {
		return constant;
	}
	
	public String toString(int indent) {
		return indentation(indent) + String.valueOf(constant);
	}

	public String toSourceString(int indent) {
		return indentation(indent) + String.valueOf(constant);
	}

}

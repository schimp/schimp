package uk.ac.bham.cs.schimp.lang.expression.arith;

import org.apache.commons.math3.fraction.Fraction;

import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.VariableScopeFrame;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class ArithmeticConstant extends ArithmeticExpression {
	
	private Fraction constant;
	
	public ArithmeticConstant(Fraction constant) {
		super();
		this.constant = constant;
	}
	
	public ArithmeticConstant(int integer) {
		super();
		this.constant = new Fraction(integer);
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {}
	
	@Override
	public ArithmeticConstant evaluate(ProgramExecutionContext context) {
		return this;
	}
	
	@Override
	public ArithmeticConstant evaluate(VariableScopeFrame frame) {
		return this;
	}
	
	public ArithmeticConstant clone() {
		return new ArithmeticConstant(new Fraction(constant.getNumerator(), constant.getDenominator()));
	}
	
	public Fraction toFraction() {
		return constant;
	}
	
	public String toString(int indent) {
		return indentation(indent) + constant.toString();
	}

	public String toSourceString(int indent) {
		return indentation(indent) + constant.toString();
	}

}

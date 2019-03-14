package uk.ac.bham.cs.schimp.lang.expression.arith;

import java.math.BigInteger;

import org.apache.commons.math3.fraction.BigFraction;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.VariableScopeFrame;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class ExponentOperation extends ArithmeticExpression {
	
	private ArithmeticExpression left;
	private ArithmeticExpression right;
	
	public ExponentOperation(ArithmeticExpression left, ArithmeticExpression right) {
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
		BigFraction leftFraction = left.evaluate(context).toFraction();
		BigInteger rightAsInteger = new BigInteger(Integer.toString(right.evaluate(context).toFraction().intValue()));
		return new ArithmeticConstant(leftFraction.pow(rightAsInteger));
	}
	
	@Override
	public ArithmeticConstant evaluate(VariableScopeFrame frame) throws EvaluationException {
		BigFraction leftFraction = left.evaluate(frame).toFraction();
		BigInteger rightAsInteger = new BigInteger(Integer.toString(right.evaluate(frame).toFraction().intValue()));
		return new ArithmeticConstant(leftFraction.pow(rightAsInteger));
	}
	
	public String toString(int indent) {
		return indentation(indent) + "(" + left.toString() + " ^ " + right.toString() + ")";
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "(" + left.toSourceString() + " ^ " + right.toSourceString() + ")";
	}
	
}

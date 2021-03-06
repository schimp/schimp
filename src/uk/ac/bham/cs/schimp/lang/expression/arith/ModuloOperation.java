package uk.ac.bham.cs.schimp.lang.expression.arith;

import org.apache.commons.math3.fraction.BigFraction;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.VariableScopeFrame;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class ModuloOperation extends ArithmeticExpression {
	
	private ArithmeticExpression left;
	private ArithmeticExpression right;
	
	public ModuloOperation(ArithmeticExpression left, ArithmeticExpression right) {
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
		BigFraction rightFraction = right.evaluate(context).toFraction();
		return new ArithmeticConstant(
			leftFraction.subtract(
				rightFraction.multiply(
					leftFraction.divide(rightFraction).intValue()
				)
			)
		);
	}
	
	@Override
	public ArithmeticConstant evaluate(VariableScopeFrame frame) throws EvaluationException {
		BigFraction leftFraction = left.evaluate(frame).toFraction();
		BigFraction rightFraction = right.evaluate(frame).toFraction();
		return new ArithmeticConstant(
			leftFraction.subtract(
				rightFraction.multiply(
					leftFraction.divide(rightFraction).intValue()
				)
			)
		);
	}
	
	public String toString(int indent) {
		return indentation(indent) + "(" + left.toString() + " mod " + right.toString() + ")";
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "(" + left.toSourceString() + " mod " + right.toSourceString() + ")";
	}
	
}

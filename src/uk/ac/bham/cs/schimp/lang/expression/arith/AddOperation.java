package uk.ac.bham.cs.schimp.lang.expression.arith;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.VariableScopeFrame;
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
	public ArithmeticConstant evaluate(ProgramExecutionContext context) throws EvaluationException {
		return new ArithmeticConstant(left.evaluate(context).toFraction().add(right.evaluate(context).toFraction()));
	}
	
	@Override
	public ArithmeticConstant evaluate(VariableScopeFrame frame) throws EvaluationException {
		return new ArithmeticConstant(left.evaluate(frame).toFraction().add(right.evaluate(frame).toFraction()));
	}
	
	public String toString(int indent) {
		return indentation(indent) + "(" + left.toString() + " + " + right.toString() + ")";
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "(" + left.toSourceString() + " + " + right.toSourceString() + ")";
	}
	
}

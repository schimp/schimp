package uk.ac.bham.cs.schimp.lang.expression.arith;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.VariableScopeFrame;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class FloorOperation extends ArithmeticExpression {
	
	private ArithmeticExpression exp;
	
	public FloorOperation(ArithmeticExpression exp) {
		super();
		this.exp = exp;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		exp.check(context);
	}
	
	@Override
	public ArithmeticConstant evaluate(ProgramExecutionContext context) throws EvaluationException {
		return new ArithmeticConstant((int)Math.floor(exp.evaluate(context).toFraction().doubleValue()));
	}
	
	@Override
	public ArithmeticConstant evaluate(VariableScopeFrame frame) throws EvaluationException {
		return new ArithmeticConstant((int)Math.floor(exp.evaluate(frame).toFraction().doubleValue()));
	}
	
	public String toString(int indent) {
		return indentation(indent) + "(floor " + exp.toString() + ")";
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "(floor " + exp.toString() + ")";
	}
	
}

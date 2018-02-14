package uk.ac.bham.cs.schimp.lang.expression.arith;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.lang.expression.Expression;

public abstract class ArithmeticExpression extends Expression {
	
	public ArithmeticExpression() {
		super();
	}
	
	public abstract ArithmeticConstant evaluate(ProgramExecutionContext context) throws EvaluationException;
	
}

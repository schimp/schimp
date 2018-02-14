package uk.ac.bham.cs.schimp.lang.expression.bool;

import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.lang.expression.Expression;

public abstract class BooleanExpression extends Expression {
	
	public BooleanExpression() {
		super();
	}
	
	public abstract BooleanConstant evaluate(ProgramExecutionContext context) throws EvaluationException;
	
}

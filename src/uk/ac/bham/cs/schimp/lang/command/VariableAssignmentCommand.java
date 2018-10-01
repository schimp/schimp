package uk.ac.bham.cs.schimp.lang.command;

import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpressionProbabilityMassFunction;
import uk.ac.bham.cs.schimp.lang.expression.arith.VariableReference;

public abstract class VariableAssignmentCommand extends Command {
	
	public abstract VariableReference getVariableReference();
	
	public abstract ArithmeticExpressionProbabilityMassFunction getArithmeticExpressionProbabilityMassFunction();

}

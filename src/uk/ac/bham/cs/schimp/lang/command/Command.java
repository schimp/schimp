package uk.ac.bham.cs.schimp.lang.command;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.lang.Syntax;
import uk.ac.bham.cs.schimp.source.ControlFlowContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public abstract class Command extends Syntax {
	
	protected int id = Integer.MIN_VALUE;
	
	/**
	 * A reference to the next command to be executed in the function that this command belongs to, or null if this is
	 * the final command to be executed in the function (e.g., if control returns to a function higher in the call
	 * stack)
	 */
	protected Command nextCommand = null;
	
	protected int destroyBlockScopeFrames = 0;
	
	public Command() {
		super();
	}
	
	public int getID() {
		return id;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		id = context.program.getCommandTable().addCommand(this);
	}
	
	public void resolveControlFlow(ControlFlowContext context) {
		nextCommand = context.nextCommand;
		
		while (!context.blockStack.isEmpty() && context.blockStack.peekFirst() == false) {
			destroyBlockScopeFrames++;
			context.blockStack.pop();
		}
	}
	
	public abstract ProbabilityMassFunction<ProgramExecutionContext> execute(ProgramExecutionContext context) throws ProgramExecutionException;
	
}

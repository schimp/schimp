package uk.ac.bham.cs.schimp.lang.command;

import parser.State;
import uk.ac.bham.cs.schimp.exec.SucceedingStates;
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
	//protected Command nextCommand = null;
	
	public Command() {
		super();
	}
	
	public int getID() {
		return id;
	}
	
	/*
	public void setNextCommand(Command next) {
		nextCommand = next;
	}
	*/
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		id = context.program.getCommandTable().addCommand(this);
	}
	
	public abstract void resolveControlFlow(ControlFlowContext context);
	
	public abstract SucceedingStates execute(State state);
	
}

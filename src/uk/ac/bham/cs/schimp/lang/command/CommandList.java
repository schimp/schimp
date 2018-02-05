package uk.ac.bham.cs.schimp.lang.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.bham.cs.schimp.lang.Syntax;
import uk.ac.bham.cs.schimp.source.ControlFlowContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class CommandList extends Syntax {
	
	protected List<Command> commands;
	
	public CommandList(List<Command> commands) {
		super();
		this.commands = commands;
	}
	
	public CommandList(Command... commands) {
		super();
		this.commands = Arrays.asList(commands);
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		checkCommandList(context);
	}
	
	public void resolveControlFlow(ControlFlowContext context) {
		
	}
	
	protected void checkCommandList(SyntaxCheckContext context) throws SyntaxException {
		try {
			for (int i = 0; i < commands.size(); i++) {
				commands.get(i).check(context);
			}
		} catch (SyntaxException e) {
			throw e;
		}
	}
	
	public String toString(int indent) {
		return commands.stream().map(cmd -> cmd.toString(indent)).collect(Collectors.joining(";\n"));
	}
	
	public String toSourceString(int indent) {
		return commands.stream().map(cmd -> cmd.toSourceString(indent)).collect(Collectors.joining(";\n"));
	}
	
}

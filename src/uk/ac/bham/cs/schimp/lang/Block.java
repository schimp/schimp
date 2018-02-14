package uk.ac.bham.cs.schimp.lang;

import java.util.List;
import java.util.stream.Collectors;

import uk.ac.bham.cs.schimp.lang.command.Command;
import uk.ac.bham.cs.schimp.lang.command.CommandList;
import uk.ac.bham.cs.schimp.source.ControlFlowContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class Block extends CommandList {
	
	public Block(List<Command> commands) {
		super(commands);
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		// a Block is a special type of CommandList that creates a new scope frame before its commands are executed and
		// destroys the scope frame after its commands are executed - we need to mimic this behaviour when syntax-
		// checking the Block
		context.variableBindings.createBlockScopeFrame();
		
		checkCommandList(context);
		
		context.variableBindings.destroyBlockScopeFrame();
	}
	
	@Override
	public void resolveControlFlow(ControlFlowContext context) {		
		Command nextCommandAfterBlock = context.nextCommand;
		
		context.blockStack.push(true);
		
		for (int i = 0; i < commands.size(); i++) {
			if (i == commands.size() - 1) {
				context.blockStack.pop();
				context.blockStack.push(false);
				context.nextCommand = nextCommandAfterBlock;
			} else {
				context.nextCommand = commands.get(i + 1);
			}
			
			commands.get(i).resolveControlFlow(context);
		}
	}
	
	public String toString(int indent) {
		return commands.stream().map(cmd -> cmd.toString(indent)).collect(Collectors.joining(";\n"));
	}
	
	public String toSourceString(int indent) {
		return commands.stream().map(cmd -> cmd.toSourceString(indent)).collect(Collectors.joining(";\n"));
	}
	
}

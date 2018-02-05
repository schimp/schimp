package uk.ac.bham.cs.schimp.lang;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.bham.cs.schimp.exec.SucceedingStates;
import uk.ac.bham.cs.schimp.lang.command.Command;
import uk.ac.bham.cs.schimp.lang.command.CommandList;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class Block extends CommandList {
	
	protected int[] scopedStateIndices = new int[0];
	
	public Block(List<Command> commands) {
		super(commands);
	}
	
	public int[] getScopedStateIndices() {
		return scopedStateIndices;
	}
	
	public void destroyScopedStateIndices(SucceedingStates states) {
		if (scopedStateIndices.length == 0) return;
		
		states.getStates().parallelStream()
			.forEach(s -> {
				for (int i = 0; i < scopedStateIndices.length; i++) {
					s.varValues[i] = Integer.MIN_VALUE;
				}
			});
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		// a Block is a special type of CommandList that creates a new scope frame before its commands are executed and
		// destroys the scope frame after its commands are executed - we need to mimic this behaviour when syntax-
		// checking the Block
		context.variableBindings.createBlockScopeFrame();
		
		checkCommandList(context);
		
		// we also need to keep a reference of which indices in the prism State object representing this program's
		// current execution state were created in this Block's scope, so their values can be erased after this Block
		// has finished executing (mimicking the variables going out of scope)
		scopedStateIndices = context.variableBindings.destroyBlockScopeFrame();
	}
	
	/*
	public void continueExecution(State state, SucceedingStates postCommandStates) {
		// find the Command with the id currently stored in the State's program counter, and use it to look up the
		// Command to be executed following this one - if there is no next Command, this Block has finished executing,
		// so any variables defined in its scope should be destroyed and control should return to this Block's own
		// parent Block
		int currentCommandID = (int)state.varValues[0];
		
		Command next = null;
		for (int i = 0; i < commands.size() - 1; i++) {
			if (commands.get(i).getID() == currentCommandID) {
				next = commands.get(i + 1);
				break;
			}
		}
		
		if (next == null) {
			destroyScopedStateIndices(postCommandStates);
			parentBlock.continueExecution(state, postCommandStates);
		} else {
			
		}
	}
	*/
	
	public String toString(int indent) {
		return indentation(indent) +
			"<scope:" + Arrays.stream(scopedStateIndices).mapToObj(i -> Integer.toString(i)).collect(Collectors.joining(",")) + ">\n" +
			commands.stream().map(cmd -> cmd.toString(indent)).collect(Collectors.joining(";\n"));
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) +
			commands.stream().map(cmd -> cmd.toSourceString(indent)).collect(Collectors.joining(";\n"));
	}
	
}

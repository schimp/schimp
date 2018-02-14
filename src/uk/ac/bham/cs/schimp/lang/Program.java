package uk.ac.bham.cs.schimp.lang;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.exec.CommandTable;
import uk.ac.bham.cs.schimp.lang.Block;
import uk.ac.bham.cs.schimp.lang.command.FunctionCommand;
import uk.ac.bham.cs.schimp.lang.command.InitialCommand;
import uk.ac.bham.cs.schimp.lang.command.InvokeCommand;
import uk.ac.bham.cs.schimp.source.ControlFlowContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class Program extends Block {
	
	private Map<Pair<String, Integer>, Function> functions = new HashMap<Pair<String, Integer>, Function>();
	private CommandTable commandTable = new CommandTable();
	
	private List<InitialCommand> initialCommands;
	private List<FunctionCommand> functionCommands;
	private InvokeCommand initialInvokeCommand;
	
	public Program(List<InitialCommand> initialCommands, List<FunctionCommand> functionCommands, InvokeCommand initialInvokeCommand) {
		super(
			Stream.concat(
				(initialCommands == null ? Collections.<InitialCommand>emptyList() : initialCommands).stream(),
				Stream.concat(
					(functionCommands == null ? Collections.<FunctionCommand>emptyList() : functionCommands).stream(),
					Arrays.asList(initialInvokeCommand).stream()
				)
			)
			.collect(Collectors.toList())
		);
		
		this.initialCommands = initialCommands;
		this.functionCommands = functionCommands;
		this.initialInvokeCommand = initialInvokeCommand;
	}
	
	public CommandTable getCommandTable() {
		return commandTable;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		context.program = this;
		
		// before checking each top-level command in the program, populate the function map - this will allow
		// FunctionReferences in commands checked later to be resolved to point to their correct Functions
		for (int i = 0; i < commands.size(); i++) {
			if (commands.get(i) instanceof FunctionCommand) {
				FunctionCommand c = (FunctionCommand)commands.get(i);
				Optional<Pair<String, Integer>> existingFunction;
				if (
					(existingFunction = context.functions.keySet().stream()
						.filter(f -> f.equals(new Pair<String, Integer>(c.getName(), c.getArity())))
						.findFirst()
					).isPresent()
				) {
					throw new SyntaxException("function '" + existingFunction.get().getValue0() + "/" + existingFunction.get().getValue1() + "' is already defined");
				} else {
					Pair<String, Integer> signature = new Pair<String, Integer>(c.getName(), c.getArity());
					Function f = new Function(c.getName(), c.getParameters(), c.getBody());
					context.functions.put(signature, f);
					functions.put(signature, f);
				}
			}
		}
		
		checkCommandList(context);
	}
	
	@Override
	public void resolveControlFlow(ControlFlowContext context) {
		context.program = this;
		
		// resolve the control flow of FunctionCommands first, in time for the initial InvokeCommand at the end of the
		// Program
		functionCommands.stream().forEach(f -> f.resolveControlFlow(context));
		
		// the control flow of a Program is the execution of the InitialCommands followed by the execution of the
		// initial InvokeCommand, so treat the Program as a CommandList containing the InitialCommands and InvokeCommand
		for (int i = 0; i < initialCommands.size(); i++) {
			if (i == initialCommands.size() - 1) {
				context.nextCommand = initialInvokeCommand;
			} else {
				context.nextCommand = initialCommands.get(i + 1);
			}
			
			initialCommands.get(i).resolveControlFlow(context);
		}
		context.nextCommand = null;
		initialInvokeCommand.resolveControlFlow(context);
	}
	
}

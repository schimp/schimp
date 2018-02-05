package uk.ac.bham.cs.schimp.lang;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.lang.command.Command;
import uk.ac.bham.cs.schimp.lang.expression.arith.VariableReference;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class Function extends Block {
	
	private String name;
	private List<VariableReference> parameters;
	
	public Function(String name, List<VariableReference> parameters, List<Command> body) {
		super(body);
		this.name = name;
		this.parameters = parameters;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		// a Function is a special type of Block that creates a new function-level scope frame before its commands are
		// executed, populates this scope frame with variable bindings per the function parameters, and destroys the
		// scope frame after its commands are executed - we need to mimic this behaviour when syntax-checking the
		// Function
		context.variableBindings.createFunctionScopeFrame();
		for (VariableReference v : parameters) {
			try {
				context.variableBindings.define(v.getName());
				v.check(context);
			} catch (ProgramExecutionException e) {
				throw new SyntaxException("parameter name '" + v.getName() + "' is already used in this function definition");
			}
		}
		
		checkCommandList(context);
		
		// we also need to keep a reference of which indices in the prism State object representing this program's
		// current execution state were created in this Function's scope, so their values can be erased after this
		// Function has finished executing (mimicking the variables going out of scope)
		scopedStateIndices = context.variableBindings.destroyFunctionScopeFrame();
	}
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("function ");
		s.append(name);
		s.append("(");
		s.append(parameters.stream().map(parameter -> parameter.toString()).collect(Collectors.joining(", ")));
		s.append(") {\n");
		s.append(indentation(indent) + "<scope:" + Arrays.stream(scopedStateIndices).mapToObj(i -> Integer.toString(i)).collect(Collectors.joining(",")) + ">\n");
		s.append(commands.stream().map(cmd -> cmd.toString(indent + 1)).collect(Collectors.joining("\n")));
		s.append("\n");
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
	public String toSourceString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("function ");
		s.append(name);
		s.append("(");
		s.append(parameters.stream().map(parameter -> parameter.toSourceString()).collect(Collectors.joining(", ")));
		s.append(") {\n");
		s.append(commands.stream().map(cmd -> cmd.toSourceString(indent + 1)).collect(Collectors.joining("\n")));
		s.append("\n");
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
}

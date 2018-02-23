package uk.ac.bham.cs.schimp.lang;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.lang.command.Command;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpression;
import uk.ac.bham.cs.schimp.lang.expression.arith.VariableReference;
import uk.ac.bham.cs.schimp.source.ControlFlowContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public abstract class Function extends Block {
	
	private String name;
	private List<VariableReference> parameters;
	
	protected Function(String name, List<VariableReference> parameters, List<Command> body) {
		super(body);
		this.name = name;
		this.parameters = parameters;
	}
	
	public String getName() {
		return name;
	}
	
	public List<VariableReference> getParameters() {
		return parameters;
	}
	
	public abstract ProbabilityMassFunction<Pair<Integer, Integer>> getResourceConsumption(List<ArithmeticConstant> invokeParameters);
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		// a Function is a special type of Block that creates a new function-level scope frame before its commands are
		// executed, populates this scope frame with variable bindings per the function parameters, and destroys the
		// scope frame after its commands are executed - we need to mimic this behaviour when syntax-checking the
		// Function
		context.variableBindings.createFunctionScopeFrame();
		for (VariableReference v : parameters) {
			try {
				context.variableBindings.define(v.getName(), new ArithmeticConstant(0));
				v.check(context);
			} catch (ProgramExecutionException e) {
				throw new SyntaxException("parameter name '" + v.getName() + "' is already used in this function definition");
			}
		}
		
		checkCommandList(context);
		
		context.variableBindings.destroyFunctionScopeFrame();
	}
	
	@Override
	public void resolveControlFlow(ControlFlowContext context) {
		// when a Function begins executing, the only Blocks that should be destroyed after Commands have executed are
		// ones created within the scope of that function
		Deque<Boolean> parentBlockStack = context.blockStack;
		context.blockStack = new ArrayDeque<>();
		
		for (int i = 0; i < commands.size(); i++) {
			context.nextCommand = (i == commands.size() - 1 ? null : commands.get(i + 1));
			commands.get(i).resolveControlFlow(context);
		}
		
		context.blockStack = parentBlockStack;
	}
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("function ");
		s.append(name);
		s.append("(");
		s.append(parameters.stream().map(parameter -> parameter.toString()).collect(Collectors.joining(", ")));
		s.append(") {\n");
		s.append(commands.stream().map(cmd -> cmd.toString(indent + 1)).collect(Collectors.joining(";\n")));
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
		s.append(commands.stream().map(cmd -> cmd.toSourceString(indent + 1)).collect(Collectors.joining(";\n")));
		s.append("\n");
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
}

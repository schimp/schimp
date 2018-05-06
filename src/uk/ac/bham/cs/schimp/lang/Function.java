package uk.ac.bham.cs.schimp.lang;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.FunctionModel;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.lang.command.Command;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;
import uk.ac.bham.cs.schimp.lang.expression.arith.VariableReference;
import uk.ac.bham.cs.schimp.source.ControlFlowContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class Function extends Block {
	
	public static ProbabilityMassFunction<Pair<Integer, Integer>> getAtomicFunctionModel() {
		ProbabilityMassFunction<Pair<Integer, Integer>> pmf = new ProbabilityMassFunction<>();
		pmf.add(new Pair<Integer, Integer>(0, 0), "1");
		pmf.finalise();
		return pmf;
	}
	
	public enum ResourceConsumptionType {
		ATOMIC,
		NON_ATOMIC
	}
	
	private String name;
	private List<VariableReference> parameters;
	private ResourceConsumptionType type;
	private FunctionModel resourceConsumptionModel;
	
	protected Function(String name, List<VariableReference> parameters, List<Command> body) {
		super(body);
		this.name = name;
		this.parameters = parameters;
	}
	
	public String getName() {
		return name;
	}
	
	public int getArity() {
		return parameters.size();
	}
	
	public List<VariableReference> getParameters() {
		return parameters;
	}
	
	public ResourceConsumptionType getType() {
		return type;
	}
	
	public ProbabilityMassFunction<Pair<Integer, Integer>> getResourceConsumption(List<ArithmeticConstant> invokeParameters) {
		if (type == ResourceConsumptionType.ATOMIC) {
			return getAtomicFunctionModel();
		} else {
			return resourceConsumptionModel.getResourceConsumption(invokeParameters);
		}
	}
	
	public void setResourceConsumptionModel(FunctionModel model) {
		resourceConsumptionModel = model;
		
		// if a function has a resource consumption model, it is implicitly non-atomic
		type = ResourceConsumptionType.NON_ATOMIC;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		if (context.functionModels != null) {
			// if this Function has an associated resource consumption model, store it
			context.functionModels.keySet().stream()
				.filter(sig -> sig.equals(new Pair<String, Integer>(name, parameters.size())))
				.findFirst()
				.ifPresent(sig -> setResourceConsumptionModel(context.functionModels.get(sig)));
		}
		
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

package uk.ac.bham.cs.schimp.exec;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import uk.ac.bham.cs.schimp.lang.Program;
import uk.ac.bham.cs.schimp.lang.command.Command;
import uk.ac.bham.cs.schimp.lang.command.InvokeCommand;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;

public class ProgramExecutionContext implements Cloneable {
	
	public Command executingCommand = null;
	public ArrayDeque<InvokeCommand> invocationStack = new ArrayDeque<>();
	public boolean executingNonAtomicFunction = false;
	public VariableBindings variableBindings = new VariableBindings();
	public VariableScopeFrame initialVariableBindings = new VariableScopeFrame(VariableScopeFrame.Type.BLOCK);
	public int elapsedTime = 0;
	public Map<Integer, Integer> powerConsumption = new LinkedHashMap<>();
	public Map<Integer, List<ArithmeticConstant>> outputs = new LinkedHashMap<>();
	
	public static ProgramExecutionContext initialContext(Program program) {
		ProgramExecutionContext context = new ProgramExecutionContext();
		
		context.executingCommand = program.getFirstCommand();
		
		return context;
	}
	
	private ProgramExecutionContext() {}
	
	public void setNextCommand(Command nextCommand) {
		if (nextCommand == null) {
			try {
				executingCommand = nextCommand;
				while (executingCommand == null) {
					executingCommand = invocationStack.pop().endInvocation(this);
				}
			} catch (NoSuchElementException e) {
				// if there are no more InvokeCommands on the function invocation stack, the program's initial function
				// has finished executing, and there are no more commands left to be executed - enter a terminating
				// state
				executingCommand = null;
			}
		} else {
			executingCommand = nextCommand;
		}
	}
	
	public void destroyBlockScopeFrames(int count) {
		for (int i = 0; i < count; i++) {
			variableBindings.destroyBlockScopeFrame();
		}
	}
	
	@Override
	public ProgramExecutionContext clone() {
		ProgramExecutionContext clonedContext = new ProgramExecutionContext();
		
		clonedContext.executingCommand = executingCommand;
		clonedContext.invocationStack = invocationStack.clone();
		clonedContext.executingNonAtomicFunction = executingNonAtomicFunction;
		clonedContext.variableBindings = variableBindings.clone();
		clonedContext.initialVariableBindings = initialVariableBindings.clone();
		clonedContext.elapsedTime = elapsedTime;
		clonedContext.powerConsumption.putAll(powerConsumption);
		// we can shallow-clone ArithmeticConstant objects, because they're never supposed to change
		outputs.entrySet().stream().forEach(x -> clonedContext.outputs.put(x.getKey(), new LinkedList<>(x.getValue())));
		
		return clonedContext;
	}
	
	public String toHash() {
		return DigestUtils.md5Hex(toString());
	}
	
	@Override
	public String toString() {
		return this.toString(0);
	}
	
	private String indentation(int indent) {
		return StringUtils.repeat("  ", indent);
	}
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("ProgramExecutionContext: {\n");
		
		s.append(indentation(indent + 1));
		s.append("executingCommand: ");
		s.append(executingCommand == null ? "terminated" : executingCommand.getID());
		s.append("\n");
		
		s.append(indentation(indent + 1));
		s.append("invocationStack: [");
		s.append(invocationStack.stream().map(i -> String.valueOf(i.getID())).collect(Collectors.joining(",")));
		s.append("]\n");
		
		s.append(indentation(indent + 1));
		s.append("executingNonAtomicFunction: ");
		s.append(executingNonAtomicFunction);
		s.append("\n");
		
		s.append(variableBindings.toString(indent + 1));
		s.append("\n");
		
		s.append(indentation(indent + 1));
		s.append("initialVariableBindings: ");
		s.append(initialVariableBindings.toString());
		s.append("\n");
		
		s.append(indentation(indent + 1));
		s.append("elapsedTime: ");
		s.append(elapsedTime);
		s.append("\n");
		
		s.append(indentation(indent + 1));
		s.append("powerConsumption: {");
		s.append(powerConsumption.keySet().stream().map(t -> t + "=" + powerConsumption.get(t)).collect(Collectors.joining(",")));
		s.append("}\n");
		
		s.append(indentation(indent + 1));
		s.append("outputs: {");
		s.append(outputs.keySet().stream().map(t -> t + "=[" + outputs.get(t).stream().map(ac -> ac.toSourceString()).collect(Collectors.joining(",")) + "]").collect(Collectors.joining(",")));
		s.append("}\n");
		
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}

}

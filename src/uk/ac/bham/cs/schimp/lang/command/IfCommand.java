package uk.ac.bham.cs.schimp.lang.command;

import java.util.ArrayDeque;
import java.util.stream.Collectors;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.lang.Block;
import uk.ac.bham.cs.schimp.lang.expression.bool.BooleanConstant;
import uk.ac.bham.cs.schimp.lang.expression.bool.BooleanExpression;
import uk.ac.bham.cs.schimp.source.ControlFlowContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class IfCommand extends Command {
	
	private BooleanExpression conditional;
	private Block trueBody;
	private Block falseBody;
	private Command trueNextCommand;
	private Command falseNextCommand;
	
	public IfCommand(BooleanExpression conditional, Block trueBody, Block falseBody) {
		super();
		this.conditional = conditional;
		this.trueBody = trueBody;
		this.falseBody = falseBody;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		super.check(context);
		
		conditional.check(context);
		trueBody.check(context);
		if (falseBody != null) falseBody.check(context);
	}
	
	@Override
	public void resolveControlFlow(ControlFlowContext context) {
		// we need to resolve the control flow for the true and false Blocks in the same way, so we need to ensure
		// before resolving the control flow for each Block that:
		// - the next command to be executed after this Block
		// - the stack of Blocks currently in scope in this Function
		// are the same for each Block
		Command nextCommandAfterIf = context.nextCommand;
		ArrayDeque<Boolean> previousBlockStack = new ArrayDeque<>(
			context.blockStack.stream()
				.map(p -> p.booleanValue())
				.collect(Collectors.toList())
		);
		
		trueNextCommand = trueBody.getFirstCommand();
		if (falseBody == null) {
			nextCommand = nextCommandAfterIf;
		} else {
			falseNextCommand = falseBody.getFirstCommand();
		}
		
		trueBody.resolveControlFlow(context);
		
		context.blockStack = previousBlockStack;
		context.nextCommand = nextCommandAfterIf;
		if (falseBody == null) {
			super.resolveControlFlow(context);
		} else {
			falseBody.resolveControlFlow(context);
		}
	}
	
	@Override
	public ProbabilityMassFunction<ProgramExecutionContext> execute(ProgramExecutionContext context) throws ProgramExecutionException {
		BooleanConstant conditionalValue = null;
		try {
			conditionalValue = conditional.evaluate(context);
		} catch (EvaluationException e) {
			// TODO: wrap this exception properly
			throw new ProgramExecutionException(e.getMessage());
		}
		
		ProgramExecutionContext succeedingContext = context.clone();
		
		if (conditionalValue.toBoolean() == true) {
			succeedingContext.variableBindings.createBlockScopeFrame();
			succeedingContext.setNextCommand(trueNextCommand);
		} else {
			if (falseNextCommand == null) {
				if (destroyBlockScopeFrames > 0) context.destroyBlockScopeFrames(destroyBlockScopeFrames);
				succeedingContext.setNextCommand(nextCommand);
			} else {
				succeedingContext.variableBindings.createBlockScopeFrame();
				succeedingContext.setNextCommand(falseNextCommand);
			}
		}
		
		ProbabilityMassFunction<ProgramExecutionContext> pmf = new ProbabilityMassFunction<>();
		pmf.add(succeedingContext, "1");
		pmf.finalise();
		
		return pmf;
	}
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("[");
		s.append(id);
		s.append("->t:");
		s.append(trueNextCommand.getID());
		s.append("/f:");
		s.append(
			falseNextCommand != null ?
			falseNextCommand.getID() :
			(
				(destroyBlockScopeFrames != 0 ? "dblock:" + destroyBlockScopeFrames + "," : "") +
				(nextCommand != null ? nextCommand.getID() : "popfn")
			)
		);
		s.append("] if ");
		s.append(conditional.toString());
		s.append(" {\n");
		s.append(trueBody.toString(indent + 1));
		s.append("\n");
		
		if (falseBody != null) {
			s.append(indentation(indent));
			s.append("} else {\n");
			s.append(falseBody.toString(indent + 1));
			s.append("\n");
		}
		
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
	public String toSourceString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("if ");
		s.append(conditional.toSourceString());
		s.append(" {\n");
		s.append(trueBody.toSourceString(indent + 1));
		s.append("\n");
		
		if (falseBody != null) {
			s.append(indentation(indent));
			s.append("} else {\n");
			s.append(falseBody.toSourceString(indent + 1));
			s.append("\n");
		}
		
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
}

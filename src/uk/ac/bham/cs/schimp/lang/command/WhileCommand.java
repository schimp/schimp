package uk.ac.bham.cs.schimp.lang.command;

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

public class WhileCommand extends Command {
	
	private BooleanExpression conditional;
	private Block body;
	private Command bodyNextCommand;
	
	public WhileCommand(BooleanExpression conditional, Block body) {
		super();
		this.conditional = conditional;
		this.body = body;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		super.check(context);
		
		conditional.check(context);
		body.check(context);
	}
	
	@Override
	public void resolveControlFlow(ControlFlowContext context) {
		super.resolveControlFlow(context);
		
		bodyNextCommand = body.getFirstCommand();
		
		context.nextCommand = this;
		body.resolveControlFlow(context);
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
			succeedingContext.setNextCommand(bodyNextCommand);
		} else {
			succeedingContext.setNextCommand(nextCommand);
		}
		
		ProbabilityMassFunction<ProgramExecutionContext> pmf = new ProbabilityMassFunction<>();
		pmf.add(succeedingContext, 1);
		
		return pmf;
	}
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("[");
		s.append(id);
		s.append("->t:");
		s.append(bodyNextCommand.getID());
		s.append("/f:");
		if (destroyBlockScopeFrames != 0) {
			s.append("dblock:" + destroyBlockScopeFrames + ",");
		}
		s.append(nextCommand == null ? "popfn" : nextCommand.getID());
		s.append("] while ");
		s.append(conditional.toString());
		s.append(" {\n");
		s.append(body.toString(indent + 1));
		s.append("\n");
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
	public String toSourceString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("while ");
		s.append(conditional.toSourceString());
		s.append(" {\n");
		s.append(body.toSourceString(indent + 1));
		s.append("\n");
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
}

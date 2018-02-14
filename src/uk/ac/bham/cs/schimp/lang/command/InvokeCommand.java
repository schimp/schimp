package uk.ac.bham.cs.schimp.lang.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.lang.FunctionReference;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpression;
import uk.ac.bham.cs.schimp.source.ControlFlowContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class InvokeCommand extends Command {
	
	private FunctionReference functionRef;
	private List<ArithmeticExpression> exps;
	private Command functionFirstCommand;
	
	public InvokeCommand(String functionName, List<ArithmeticExpression> exps) {
		super();
		this.exps = exps;
		this.functionRef = new FunctionReference(functionName, this.exps.size());
	}
	
	public InvokeCommand(String functionName, ArithmeticExpression... exps) {
		super();
		this.exps = Arrays.asList(exps);
		this.functionRef = new FunctionReference(functionName, this.exps.size());
	}
	
	public Command endInvocation(ProgramExecutionContext context) {
		int d = destroyBlockScopeFrames;
		while (d-- > 0) context.variableBindings.destroyBlockScopeFrame();
		
		context.variableBindings.destroyFunctionScopeFrame();
		
		return nextCommand;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		super.check(context);
		
		functionRef.check(context);
		
		try {
			for (int i = 0; i < exps.size(); i++) {
				exps.get(i).check(context);
			}
		} catch (SyntaxException e) {
			throw e;
		}
	}
	
	@Override
	public void resolveControlFlow(ControlFlowContext context) {
		super.resolveControlFlow(context);
		functionFirstCommand = functionRef.getFunction().getFirstCommand();
	}
	
	@Override
	public ProbabilityMassFunction<ProgramExecutionContext> execute(ProgramExecutionContext context) throws ProgramExecutionException {
		ProgramExecutionContext succeedingContext = context.clone();
		
		succeedingContext.variableBindings.createFunctionScopeFrame();
		succeedingContext.setNextCommand(functionFirstCommand);
		
		for (int i = 0; i < exps.size(); i++) {
			try {
				succeedingContext.variableBindings.define(
					functionRef.getFunction().getParameters().get(i).getName(),
					exps.get(i).evaluate(context)
				);
			} catch (EvaluationException e) {
				// TODO: wrap this exception properly
				throw new ProgramExecutionException(e.getMessage());
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
		s.append("->");
		s.append("pushfn:" + functionFirstCommand.getID() + ",rtnfn");
		if (destroyBlockScopeFrames != 0) {
			s.append(",dblock:" + destroyBlockScopeFrames);
		}
		s.append(",");
		s.append(nextCommand == null ? "popfn" : nextCommand.getID());
		s.append("] ");
		s.append(functionRef.toString());
		s.append("(");
		s.append(exps.stream().map(exp -> exp.toString()).collect(Collectors.joining(", ")));
		s.append(")");
		
		return s.toString();
	}
	
	public String toSourceString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append(functionRef.toString());
		s.append("(");
		s.append(exps.stream().map(exp -> exp.toSourceString()).collect(Collectors.joining(", ")));
		s.append(")");
		
		return s.toString();
	}
	
}

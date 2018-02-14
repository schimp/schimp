package uk.ac.bham.cs.schimp.lang.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpression;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class OutputCommand extends Command {
	
	private List<ArithmeticExpression> exps;
	
	public OutputCommand(List<ArithmeticExpression> exps) {
		super();
		this.exps = exps;
	}
	
	public OutputCommand(ArithmeticExpression... exps) {
		super();
		this.exps = Arrays.asList(exps);
	}
	
	@Override
	public ProbabilityMassFunction<ProgramExecutionContext> execute(ProgramExecutionContext context) throws ProgramExecutionException {
		ProgramExecutionContext succeedingContext = context.clone();
		
		// TODO: outputs are time-sensitive in the formal semantics
		exps.stream().forEachOrdered(e -> succeedingContext.outputs.add(e.evaluate(succeedingContext)));
		
		succeedingContext.setNextCommand(nextCommand);
		
		ProbabilityMassFunction<ProgramExecutionContext> pmf = new ProbabilityMassFunction<>();
		pmf.add(succeedingContext, "1");
		pmf.finalise();
		
		return pmf;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		super.check(context);
		
		try {
			for (int i = 0; i < exps.size(); i++) {
				exps.get(i).check(context);
			}
		} catch (SyntaxException e) {
			throw e;
		}
	}
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("[");
		s.append(id);
		s.append("->");
		if (destroyBlockScopeFrames != 0) {
			s.append("dblock:" + destroyBlockScopeFrames + ",");
		}
		s.append(nextCommand == null ? "popfn" : nextCommand.getID());
		s.append("] output ");
		s.append(exps.stream().map(exp -> exp.toString()).collect(Collectors.joining(", ")));
		
		return s.toString();
	}
	
	public String toSourceString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("output ");
		s.append(exps.stream().map(exp -> exp.toSourceString()).collect(Collectors.joining(", ")));
		
		return s.toString();
	}
	
}

package uk.ac.bham.cs.schimp.lang.command;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;
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
		
		// when an output command is executed, the values need to be associated in the program observations with the
		// current time point, which may not exist as an observation if no additional power has been consumed since the
		// previous time point
		List<ArithmeticConstant> currentOutputs = succeedingContext.observations.computeIfAbsent(
			succeedingContext.elapsedTime,
			i -> new Pair<>(succeedingContext.totalPowerConsumption, new LinkedList<>())
		).getValue1();
		exps.stream().forEachOrdered(e -> currentOutputs.add(e.evaluate(succeedingContext)));
		
		if (destroyBlockScopeFrames > 0) succeedingContext.destroyBlockScopeFrames(destroyBlockScopeFrames);
		succeedingContext.setNextCommand(nextCommand);
		
		ProbabilityMassFunction<ProgramExecutionContext> pmf = new ProbabilityMassFunction<>();
		pmf.add(succeedingContext, 1);
		
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

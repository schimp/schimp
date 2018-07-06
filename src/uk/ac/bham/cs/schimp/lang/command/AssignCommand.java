package uk.ac.bham.cs.schimp.lang.command;

import java.util.Iterator;
import java.util.stream.Collectors;

import org.apache.commons.math3.fraction.Fraction;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpression;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpressionProbabilityMassFunction;
import uk.ac.bham.cs.schimp.lang.expression.arith.VariableReference;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class AssignCommand extends Command {
	
	private VariableReference v;
	private ArithmeticExpressionProbabilityMassFunction pmf;
	
	public AssignCommand(VariableReference v, ArithmeticExpressionProbabilityMassFunction pmf) {
		super();
		this.v = v;
		this.pmf = pmf;
	}
	
	public AssignCommand(VariableReference v, ArithmeticExpression exp) {
		super();
		this.v = v;
		pmf = new ArithmeticExpressionProbabilityMassFunction();
		pmf.add(exp, new ArithmeticConstant(1));
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		super.check(context);
		
		// the VariableReference on the left-hand side of the assignment must already be in scope here
		v.check(context);
		
		// ArithmeticExpressions in the domain and range of the pmf must also be checked
		try {
			Iterator<ArithmeticExpression> elements = pmf.elements().iterator();
			while (elements.hasNext()) {
				ArithmeticExpression ae = elements.next();
				ae.check(context);
				pmf.probabilityOf(ae).check(context);
			}
		} catch (SyntaxException e) {
			throw e;
		}
	}
	
	@Override
	public ProbabilityMassFunction<ProgramExecutionContext> execute(ProgramExecutionContext context) throws ProgramExecutionException {
		ProbabilityMassFunction<ProgramExecutionContext> succeedingPMF = new ProbabilityMassFunction<>();
		
		pmf.elements().stream().forEach(e -> {
			ProgramExecutionContext succeedingContext = context.clone();
			
			Fraction succeedingContextProbability;
			try {
				succeedingContext.variableBindings.assign(v.getName(), e.evaluate(succeedingContext));
				succeedingContextProbability = pmf.probabilityOf(e).evaluate(context).toFraction();
			} catch (EvaluationException ex) {
				// TODO: wrap this exception properly
				throw new ProgramExecutionException(ex.getMessage());
			};
			
			if (destroyBlockScopeFrames > 0) succeedingContext.destroyBlockScopeFrames(destroyBlockScopeFrames);
			succeedingContext.setNextCommand(nextCommand);
			
			succeedingPMF.add(succeedingContext, succeedingContextProbability);
		});
		
		return succeedingPMF;
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
		s.append("] ");
		s.append(v.toString());
		s.append(" := {\n");
		s.append(
			pmf.elements().stream()
			.map((e -> e.toString(indent + 1) + " -> " + pmf.probabilityOf(e).toString()))
			.collect(Collectors.joining(",\n"))
		);
		s.append("\n");
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
	public String toSourceString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append(v.toSourceString());
		s.append(" := {\n");
		s.append(
			pmf.elements().stream()
			.map((e -> e.toSourceString(indent + 1) + " -> " + pmf.probabilityOf(e).toString()))
			.collect(Collectors.joining(",\n"))
		);
		s.append("\n");
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
}

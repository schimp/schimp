package uk.ac.bham.cs.schimp.lang.command;

import java.util.Iterator;
import java.util.stream.Collectors;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpression;
import uk.ac.bham.cs.schimp.lang.expression.arith.VariableReference;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class InitialCommand extends Command {
	
	private VariableReference v;
	private ProbabilityMassFunction<ArithmeticExpression> pmf;
	
	public InitialCommand(VariableReference v, ProbabilityMassFunction<ArithmeticExpression> pmf) {
		super();
		this.v = v;
		this.pmf = pmf;
		this.pmf.finalise();
	}
	
	public InitialCommand(VariableReference v, ArithmeticExpression exp) {
		super();
		this.v = v;
		pmf = new ProbabilityMassFunction<ArithmeticExpression>();
		pmf.add(exp, "1");
		pmf.finalise();
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		super.check(context);
		
		// ArithmeticExpressions in the domain of the pmf must be checked first (before the VariableReference on the
		// left-hand side is recorded in the SyntaxCheckContext)
		try {
			Iterator<ArithmeticExpression> elements = pmf.elements().iterator();
			while (elements.hasNext()) {
				elements.next().check(context);
			}
		} catch (SyntaxException e) {
			throw e;
		}
		
		// the VariableReference on the left-hand side of the assignment must *not* already have been defined in the
		// current scope frame - try to declare a variable with this name and a dummy value, and fail if a
		// ProgramExecutionException is thrown (indicating that the given variable name is already defined in the
		// current narrowest scope block)
		try {
			context.variableBindings.define(v.getName(), new ArithmeticConstant(0));
		} catch (ProgramExecutionException e) {
			throw new SyntaxException("variable '" + v.getName() + "' is already defined in this block");
		}
		
		// finally, the VariableReference itself can be checked
		v.check(context);
	}
	
	@Override
	public ProbabilityMassFunction<ProgramExecutionContext> execute(ProgramExecutionContext context) throws ProgramExecutionException {
		ProbabilityMassFunction<ProgramExecutionContext> succeedingPMF = new ProbabilityMassFunction<>();
		
		pmf.elements().stream().forEach(e -> {
			ProgramExecutionContext succeedingContext = context.clone();
			
			try {
				ArithmeticConstant a = e.evaluate(succeedingContext);
				succeedingContext.variableBindings.define(v.getName(), a);
				succeedingContext.initialVariableBindings.define(v.getName(), a);
			} catch (EvaluationException ex) {
				// TODO: wrap this exception properly
				throw new ProgramExecutionException(ex.getMessage());
			};
			
			if (destroyBlockScopeFrames > 0) succeedingContext.destroyBlockScopeFrames(destroyBlockScopeFrames);
			succeedingContext.setNextCommand(nextCommand);
			
			succeedingPMF.add(succeedingContext, pmf.probabilityOf(e));
		});
		
		succeedingPMF.finalise();
		
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
		s.append("] initial ");
		s.append(v.toString());
		s.append(" := {\n");
		s.append(
			pmf.elements().stream()
			.map((e -> e.toString(indent + 1) + " -> " + pmf.probabilityOf(e).toString(true)))
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
		s.append("initial ");
		s.append(v.toSourceString());
		s.append(" := {\n");
		s.append(
			pmf.elements().stream()
			.map((e -> e.toSourceString(indent + 1) + " -> " + pmf.probabilityOf(e).toString(true)))
			.collect(Collectors.joining(",\n"))
		);
		s.append("\n");
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
}

package uk.ac.bham.cs.schimp.lang.command;

import java.util.Iterator;
import java.util.stream.Collectors;

import parser.State;
import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.SucceedingStates;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpression;
import uk.ac.bham.cs.schimp.lang.expression.arith.VariableReference;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class AssignCommand extends Command {
	
	private VariableReference v;
	private ProbabilityMassFunction<ArithmeticExpression> pmf;
	
	public AssignCommand(VariableReference v, ProbabilityMassFunction<ArithmeticExpression> pmf) {
		super();
		this.v = v;
		this.pmf = pmf;
	}
	
	public AssignCommand(VariableReference v, ArithmeticExpression exp) {
		super();
		this.v = v;
		pmf = new ProbabilityMassFunction<ArithmeticExpression>();
		pmf.add(exp, "1");
		pmf.finalise();
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		super.check(context);
		
		// the VariableReference on the left-hand side of the assignment must already be in scope here
		v.check(context);
		
		// ArithmeticExpressions in the domain of the pmf must also be checked
		try {
			Iterator<ArithmeticExpression> elements = pmf.elements().iterator();
			while (elements.hasNext()) {
				elements.next().check(context);
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
		//if (nextCommand != null) s.append("->" + nextCommand.getID());
		s.append("] ");
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

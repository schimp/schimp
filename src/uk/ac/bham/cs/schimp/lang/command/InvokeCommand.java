package uk.ac.bham.cs.schimp.lang.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.bham.cs.schimp.lang.FunctionReference;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpression;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class InvokeCommand extends Command {
	
	private FunctionReference functionRef;
	private List<ArithmeticExpression> exps;
	
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
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("[");
		s.append(id);
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

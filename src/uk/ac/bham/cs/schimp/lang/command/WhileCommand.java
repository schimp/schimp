package uk.ac.bham.cs.schimp.lang.command;

import uk.ac.bham.cs.schimp.lang.Block;
import uk.ac.bham.cs.schimp.lang.expression.bool.BooleanExpression;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class WhileCommand extends Command {
	
	private BooleanExpression conditional;
	private Block body;
	
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
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("[");
		s.append(id);
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

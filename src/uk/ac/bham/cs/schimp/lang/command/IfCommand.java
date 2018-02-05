package uk.ac.bham.cs.schimp.lang.command;

import uk.ac.bham.cs.schimp.lang.Block;
import uk.ac.bham.cs.schimp.lang.expression.bool.BooleanExpression;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class IfCommand extends Command {
	
	private BooleanExpression conditional;
	private Block trueBody;
	private Block falseBody;
	
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
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("[");
		s.append(id);
		//if (nextCommand != null) s.append("->" + nextCommand.getID());
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

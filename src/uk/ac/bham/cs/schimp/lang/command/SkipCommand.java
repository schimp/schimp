package uk.ac.bham.cs.schimp.lang.command;

import parser.State;
import uk.ac.bham.cs.schimp.exec.SucceedingStates;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class SkipCommand extends Command {
	
	public SkipCommand() {
		super();
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		super.check(context);
	}
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("[");
		s.append(id);
		//if (nextCommand != null) s.append("->" + nextCommand.getID());
		s.append("] skip");
		
		return s.toString();
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "skip";
	}
	
}

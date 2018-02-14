package uk.ac.bham.cs.schimp.lang.command;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
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
	
	@Override
	public ProbabilityMassFunction<ProgramExecutionContext> execute(ProgramExecutionContext context) throws ProgramExecutionException {
		ProgramExecutionContext succeedingContext = context.clone();
		
		succeedingContext.setNextCommand(nextCommand);
		
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
		if (destroyBlockScopeFrames != 0) {
			s.append("dblock:" + destroyBlockScopeFrames + ",");
		}
		s.append(nextCommand == null ? "popfn" : nextCommand.getID());
		s.append("] skip");
		
		return s.toString();
	}
	
	public String toSourceString(int indent) {
		return indentation(indent) + "skip";
	}
	
}

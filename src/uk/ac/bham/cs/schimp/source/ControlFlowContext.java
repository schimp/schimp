package uk.ac.bham.cs.schimp.source;

import java.util.ArrayDeque;
import java.util.Deque;

import uk.ac.bham.cs.schimp.lang.Program;
import uk.ac.bham.cs.schimp.lang.command.Command;

public class ControlFlowContext {
	
	public Program program;
	public Deque<Boolean> blockStack = new ArrayDeque<>(); // <Commands remaining in Block?>
	public Command nextCommand = null;

}

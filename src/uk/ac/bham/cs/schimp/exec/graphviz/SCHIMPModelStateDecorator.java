package uk.ac.bham.cs.schimp.exec.graphviz;

import java.util.Iterator;
import java.util.List;

import explicit.graphviz.Decoration;
import explicit.graphviz.Decorator;
import parser.State;
import uk.ac.bham.cs.schimp.exec.PRISMModelGenerator;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;

public class SCHIMPModelStateDecorator implements Decorator {
	
	private List<State> stateList;
	private PRISMModelGenerator modelGenerator;
	private List<String> initialVariableNames;
	private boolean showOutputLists = false;
	
	public SCHIMPModelStateDecorator(List<State> stateList, PRISMModelGenerator modelGenerator, boolean showOutputLists) {
		this.stateList = stateList;
		this.modelGenerator = modelGenerator;
		initialVariableNames = modelGenerator.stateInitialVariableNames();
		this.showOutputLists = showOutputLists;
	}
	
	public Decoration decorateState(int state, Decoration d) {
		State prismState = stateList.get(state);
		
		// set the label for this node to contain:
		StringBuilder label = new StringBuilder();
		
		int schimpExecutionContextID = (int)prismState.varValues[0];
		ProgramExecutionContext context = modelGenerator.getSCHIMPExecutionContext(schimpExecutionContextID);
		
		// - the prism State id and unique SCHIMPExecutionContext id
		label.append("P: " + state + " / C: " + schimpExecutionContextID + "\n");
		
		// - the id of the command being executed
		if (context.isTerminating()) {
			// give prism States representing terminating SCHIMPExecutionContexts a double outline
			d.attributes().put("peripheries", "2");
		} else {
			label.append("→ " + context.executingCommand.getID() + "\n");
		}
		
		// - the current value of each initial variable in this SCHIMPExecutionContext
		Iterator<String> n = initialVariableNames.iterator();
		for (int v = prismState.varValues.length - initialVariableNames.size(); v < prismState.varValues.length; v++) {
			label.append(
				n.next() + " = " +
				((int)prismState.varValues[v] == Integer.MIN_VALUE ? "undef" : prismState.varValues[v]) +
				"\n"
			);
		}
		
		// - the outputs that the program has produced in this SCHIMPExecutionContext, either as the actual list of
		//   outputs (if showOutputLists is true) or a unique id representing a particular list of outputs (if
		//   showOutputLists is false)
		if (showOutputLists) {
			label.append("out: " + modelGenerator.getOutputList((int)prismState.varValues[modelGenerator.getStateOutputIDIndex()]) + "\n");
		} else {
			label.append("out: " + prismState.varValues[modelGenerator.getStateOutputIDIndex()] + "\n");
		}
		
		// - the elapsed time and power consumption by the time this SCHIMPExecutionContext is reached
		label.append("◷ " + context.elapsedTime + "  ⚡ " + context.totalPowerConsumption);
		
		d.setLabel(label.toString());
		
		return d;
	}

}

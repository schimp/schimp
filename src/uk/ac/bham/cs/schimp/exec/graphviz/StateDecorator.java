package uk.ac.bham.cs.schimp.exec.graphviz;

import java.util.Iterator;
import java.util.List;

import explicit.graphviz.Decoration;
import explicit.graphviz.Decorator;
import parser.State;
import uk.ac.bham.cs.schimp.exec.PRISMModelGenerator;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;

public class StateDecorator implements Decorator {
	
	private List<State> stateList;
	private PRISMModelGenerator modelGenerator;
	private List<String> initialVariableNames;
	
	public StateDecorator(List<State> stateList, PRISMModelGenerator modelGenerator) {
		this.stateList = stateList;
		this.modelGenerator = modelGenerator;
		initialVariableNames = modelGenerator.getInitialVariableNames();
	}
	
	public Decoration decorateState(int state, Decoration d) {
		State prismState = stateList.get(state);
		int schimpExecutionContextID = (int)prismState.varValues[0];
		ProgramExecutionContext context = modelGenerator.getSCHIMPExecutionContext(schimpExecutionContextID);
		
		// set the label for this node to contain:
		StringBuilder label = new StringBuilder();
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
		// - the elapsed time and power consumption by the time this SCHIMPExecutionContext is reached
		label.append("◷ " + context.elapsedTime + "  ⚡ " + context.totalPowerConsumption);
		
		d.setLabel(label.toString());
		
		return d;
	}

}

package uk.ac.bham.cs.schimp.exec.graphviz;

import java.util.List;

import explicit.graphviz.Decoration;
import explicit.graphviz.Decorator;
import parser.State;
import uk.ac.bham.cs.schimp.exec.PRISMModelGenerator;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;

public class StateDecorator implements Decorator {
	
	private List<State> stateList;
	private PRISMModelGenerator modelGenerator;
	
	public StateDecorator(List<State> stateList, PRISMModelGenerator modelGenerator) {
		this.stateList = stateList;
		this.modelGenerator = modelGenerator;
	}
	
	public Decoration decorateState(int state, Decoration d) {
		State prismState = stateList.get(state);
		int schimpExecutionContextID = (int)prismState.varValues[0];
		ProgramExecutionContext context = modelGenerator.getSCHIMPExecutionContext(schimpExecutionContextID);
		
		// set the label for this node to contain:
		d.setLabel(
			// - the unique SCHIMPExecutionContext id
			String.valueOf(schimpExecutionContextID) + "\n" +
			// - the elapsed time and power consumption by the time this SCHIMPExecutionContext is reached
			"◷ " + context.elapsedTime + "  ⚡ " + context.totalPowerConsumption
		);
		
		// give prism States representing terminating SCHIMPExecutionContexts a double outline
		if (context.isTerminating()) {
			d.attributes().put("peripheries", "2");
		}
		
		return d;
	}

}

package uk.ac.bham.cs.schimp.exec.graphviz;

import java.util.List;

import explicit.graphviz.Decoration;
import explicit.graphviz.Decorator;
import parser.State;
import uk.ac.bham.cs.schimp.exec.PRISMModelGenerator;

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
		
		// set the label for this node to be the unique SCHIMPExecutionContext id
		d.setLabel(String.valueOf(schimpExecutionContextID));
		
		// give prism States representing terminating SCHIMPExecutionContexts a double outline
		if (modelGenerator.getSCHIMPExecutionContext(schimpExecutionContextID).isTerminating()) {
			d.attributes().put("peripheries", "2");
		}
		
		return d;
	}

}

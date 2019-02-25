package uk.ac.bham.cs.schimp.exec.graphviz;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import explicit.graphviz.Decoration;
import explicit.graphviz.Decoration.LabelType;
import explicit.graphviz.Decorator;
import parser.State;
import uk.ac.bham.cs.schimp.exec.AttackerModelGenerator;

public class AttackerModelStateDecorator implements Decorator {
	
	private List<State> stateList;
	private AttackerModelGenerator modelGenerator;
	private List<String> initialVariableNames;
	private int initialVariablesOffset;
	private int initialVariableGuessesOffset;
	private boolean showObservations = false;
	
	public AttackerModelStateDecorator(List<State> stateList, AttackerModelGenerator modelGenerator, boolean showObservations) {
		this.stateList = stateList;
		this.modelGenerator = modelGenerator;
		initialVariableNames = modelGenerator.stateInitialVariableNames();
		initialVariablesOffset = modelGenerator.getStateInitialVariablesOffset();
		initialVariableGuessesOffset = modelGenerator.getStateInitialVariableGuessesOffset();
		this.showObservations = showObservations;
	}
	
	public Decoration decorateState(int state, Decoration d) {
		State prismState = stateList.get(state);
		int phase = (int)prismState.varValues[0];
		
		StringBuilder label = new StringBuilder();
		
		// the label for this node depends on which phase the state belongs to:
		switch (phase) {
			// - if _phase = 0, this is the initial state; there's no useful information stored in this state, so leave
			//   the label empty
			case 0:
				break;
			// - if _phase = 1, this is a schimp terminating state
			case 1:
				// give the state a double outline, to emphasise that it represents a terminating SCHIMPExecutionContext
				d.attributes().put("peripheries", "2");
				
				// add information about the SCHIMPExecutionContext to this state's label:
				// - the value of each initial variable in this SCHIMPExecutionContext
				Iterator<String> n = initialVariableNames.iterator();
				for (int v = initialVariablesOffset; v < initialVariableGuessesOffset; v++) {
					label.append(
						n.next() + " = " +
						((int)prismState.varValues[v] == Integer.MIN_VALUE ? "undef" : prismState.varValues[v]) +
						"\n"
					);
				}
				
				// - the observations that the program produced in this SCHIMPExecutionContext, either as the actual
				//   observations (if showObservations is true) or a unique id representing particular observations (if
				//   showObservations is false)
				if (showObservations) {
					label.append("obs: " + modelGenerator.getObservations((int)prismState.varValues[modelGenerator.getStateObservationsIDIndex()]));
				} else {
					label.append("obs: " + prismState.varValues[modelGenerator.getStateObservationsIDIndex()]);
				}
				
				// - the total elapsed time and power consumption (if present in the state)
				if (modelGenerator.stateHasTime() || modelGenerator.stateHasPower()) {
					List<String> pieces = new ArrayList<>();
					if (modelGenerator.stateHasTime()) pieces.add("◷ " + prismState.varValues[modelGenerator.getStateTimeIndex()]);
					if (modelGenerator.stateHasPower()) pieces.add("⚡ " + prismState.varValues[modelGenerator.getStatePowerIndex()]);
					label.append("\n" + String.join("  ", pieces));
				}
				
				break;
			// - if _phase = 2, the attacker has finished guessing
			case 2:
				d.setLabelType(LabelType.HTML);
				
				// add information about the correctness of each of the attacker's guesses to this state's label (green
				// if correct, red if incorrect)
				List<String> pieces = new ArrayList<>();
				Iterator<String> it = initialVariableNames.iterator();
				for (int v = initialVariableGuessesOffset; v < prismState.varValues.length; v++) {
					pieces.add(
						"<font color='" +
						((int)prismState.varValues[v] == 1 ? "green" : "red") +
						"'>" +
						((int)prismState.varValues[v] == 1 ? "✓ " : "✗ ") +
						it.next() + // variable name
						"</font>"
					);
				}
				label.append(String.join("<br/>", pieces));
				
				break;
		}
		
		d.setLabel(label.toString());
		
		return d;
	}

}

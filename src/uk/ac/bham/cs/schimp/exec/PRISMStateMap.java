package uk.ac.bham.cs.schimp.exec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import parser.State;

public class PRISMStateMap {
	
	private ArrayList<State> states = new ArrayList<>();
	private LinkedHashMap<String, Double> stateProbabilities = new LinkedHashMap<>();
	
	public void add(State state, double probability) {
		String stateString = state.toStringNoParentheses();
		
		if (stateProbabilities.containsKey(stateString)) {
			stateProbabilities.put(stateString, stateProbabilities.get(stateString) + probability);
		} else {
			stateProbabilities.put(stateString, probability);
			states.add(state);
		}
	}
	
	public List<State> getStates() {
		return states;
	}
	
	public List<Double> getProbabilities() {
		return new ArrayList<>(stateProbabilities.values());
	}

}

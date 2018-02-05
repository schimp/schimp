package uk.ac.bham.cs.schimp.exec;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import parser.State;

public class SucceedingStates {
	
	private Map<State, Float> succeedingStatePMF = new LinkedHashMap<State, Float>();
	
	public SucceedingStates() {
		
	}
	
	public void add(State state, float probability) {
		succeedingStatePMF.put(state, probability);
	}
	
	public Set<State> getStates() {
		return succeedingStatePMF.keySet();
	}

}

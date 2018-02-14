package uk.ac.bham.cs.schimp.source;

import java.util.HashMap;
import java.util.Map;

import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.exec.VariableBindings;
import uk.ac.bham.cs.schimp.lang.Function;
import uk.ac.bham.cs.schimp.lang.Program;

public class SyntaxCheckContext {
	
	public Program program;
	public VariableBindings variableBindings = new VariableBindings();
	public Map<Pair<String, Integer>, Function> functions = new HashMap<Pair<String, Integer>, Function>();

}

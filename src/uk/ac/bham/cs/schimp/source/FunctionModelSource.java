package uk.ac.bham.cs.schimp.source;

import java.io.IOException;
import java.util.Map;

import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.exec.FunctionModel;

public abstract class FunctionModelSource {
	
	public abstract Map<Pair<String, Integer>, FunctionModel> parse() throws IOException, SyntaxException;

}

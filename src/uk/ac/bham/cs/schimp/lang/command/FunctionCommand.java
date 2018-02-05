package uk.ac.bham.cs.schimp.lang.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.lang.Function;
import uk.ac.bham.cs.schimp.lang.expression.arith.VariableReference;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class FunctionCommand extends Command {
	
	private String name;
	private List<VariableReference> parameters;
	private List<Command> body;
	private Function function;
	
	public FunctionCommand(String name, List<VariableReference> parameters, List<Command> body) {
		super();
		this.name = name;
		this.parameters = parameters;
		this.body = body;
	}
	
	public String getName() {
		return name;
	}
	
	public List<VariableReference> getParameters() {
		return parameters;
	}
	
	public int getArity() {
		return parameters.size();
	}
	
	public List<Command> getBody() {
		return body;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		function = context.functions.get(
			context.functions.keySet().stream()
				.filter(f -> f.equals(new Pair<String, Integer>(getName(), getArity())))
				.findFirst()
				.get()
		);
		
		// this FunctionCommand has already been mapped to a Function in context.functions in the Program check, so we
		// already know the function definition is legal - we just need to check the body of the function
		function.check(context);
	}
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("function ");
		s.append(name);
		s.append("(");
		s.append(parameters.stream().map(parameter -> parameter.toString()).collect(Collectors.joining(", ")));
		s.append(") {\n");
		s.append(indentation(indent + 1) + "<scope:" + Arrays.stream(function.getScopedStateIndices()).mapToObj(i -> Integer.toString(i)).collect(Collectors.joining(",")) + ">\n");
		s.append(body.stream().map(cmd -> cmd.toString(indent + 1)).collect(Collectors.joining("\n")));
		s.append("\n");
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
	public String toSourceString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("function ");
		s.append(name);
		s.append("(");
		s.append(parameters.stream().map(parameter -> parameter.toSourceString()).collect(Collectors.joining(", ")));
		s.append(") {\n");
		s.append(indentation(indent + 1) + "<scope:" + Arrays.stream(function.getScopedStateIndices()).mapToObj(i -> Integer.toString(i)).collect(Collectors.joining(",")) + ">\n");
		s.append(body.stream().map(cmd -> cmd.toSourceString(indent + 1)).collect(Collectors.joining("\n")));
		s.append("\n");
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}
	
}

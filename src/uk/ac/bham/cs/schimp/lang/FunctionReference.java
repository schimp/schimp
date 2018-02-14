package uk.ac.bham.cs.schimp.lang;

import java.util.Optional;

import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class FunctionReference extends Syntax {

	private String name;
	private int arity;
	private Function function;
	
	public FunctionReference(String name, int arity) {
		this.name = name;
		this.arity = arity;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		// find the function with the given name and arity in context.functions (which was populated when the top-level
		// Program was syntax-checked) - if we can't, fail
		Optional<Pair<String, Integer>> functionSignature;
		if (
			(functionSignature = context.functions.keySet().stream()
				.filter(f -> f.equals(new Pair<String, Integer>(name, arity)))
				.findFirst()
			).isPresent()
		) {
			function = context.functions.get(functionSignature.get());
		} else {
			throw new SyntaxException("function '" + name + "/" + arity + "' is undefined");
		}
		
		// after the FunctionReference has been resolved to a Function, check that it can legally be invoked here
		//function.check(context);
	}
	
	public Function getFunction() {
		return function;
	}

	@Override
	public String toString(int indent) {
		return name;
	}

	@Override
	public String toSourceString(int indent) {
		return name;
	}
	
}

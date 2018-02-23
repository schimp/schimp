package uk.ac.bham.cs.schimp.lang;

import java.util.List;

import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.lang.command.Command;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;
import uk.ac.bham.cs.schimp.lang.expression.arith.VariableReference;

//non-atomic functions consume a defined amount of time and power when they execute, and cannot call other non-atomic
// functions
public class NonAtomicFunction extends Function {
	
	private ProbabilityMassFunction<Pair<Integer, Integer>> resourceConsumption;
	
	public NonAtomicFunction(String name, List<VariableReference> parameters, ProbabilityMassFunction<Pair<Integer, Integer>> resourceConsumption, List<Command> body) {
		super(name, parameters, body);
		this.resourceConsumption = resourceConsumption;
	}

	@Override
	public ProbabilityMassFunction<Pair<Integer, Integer>> getResourceConsumption(List<ArithmeticConstant> invokeParameters) {
		// TODO Auto-generated method stub
		return null;
	}

}

package uk.ac.bham.cs.schimp.lang;

import java.util.List;

import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.lang.command.Command;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;
import uk.ac.bham.cs.schimp.lang.expression.arith.VariableReference;

//atomic functions execute instantaneously, consume no power, and can call non-atomic or other atomic functions
public class AtomicFunction extends Function {
	
	public AtomicFunction(String name, List<VariableReference> parameters, List<Command> body) {
		super(name, parameters, body);
	}

	@Override
	public ProbabilityMassFunction<Pair<Integer, Integer>> getResourceConsumption(List<ArithmeticConstant> invokeParameters) {
		// TODO Auto-generated method stub
		return null;
	}

}

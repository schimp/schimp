package uk.ac.bham.cs.schimp.exec;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.commons.math3.fraction.Fraction;
import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.lang.Syntax;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class FunctionModel extends Syntax {
	
	private static ProbabilityMassFunction<Pair<Integer, Integer>> noConsumptionPMF;
	
	static {
		noConsumptionPMF = new ProbabilityMassFunction<>();
		noConsumptionPMF.add(new Pair<Integer, Integer>(0, 0), new Fraction(1));
	}
	
	private String name;
	private int arity;
	private List<Pair<List<ArithmeticConstant>, ProbabilityMassFunction<Pair<Integer, Integer>>>> model = new LinkedList<>();
	
	public FunctionModel(String name, int arity) {
		this.name = name;
		this.arity = arity;
	}
	
	public String getName() {
		return name;
	}
	
	public int getArity() {
		return arity;
	}
	
	public void add(List<ArithmeticConstant> invokeParameters, ProbabilityMassFunction<Pair<Integer, Integer>> resourceUsage) {
		model.add(new Pair<>(invokeParameters, resourceUsage));
	}

	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		// TODO: can't have duplicate invokeParameters on lhs
	}
	
	public ProbabilityMassFunction<Pair<Integer, Integer>> getResourceConsumption(List<ArithmeticConstant> invokeParameters) {
		try {
			return model.stream()
				.filter(tp -> {
					for (int i = 0; i < invokeParameters.size(); i++) {
						if (tp.getValue0().get(i) != null && tp.getValue0().get(i).toFraction().compareTo(invokeParameters.get(i).toFraction()) != 0) {
							return false;
						}
					}
					return true;
				})
				.findFirst()
				.get()
				.getValue1();
		} catch (NoSuchElementException e) {
			return noConsumptionPMF;
		}
	}

	@Override
	public String toString(int indent) {
		return toSourceString(indent);
	}

	@Override
	public String toSourceString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("model ");
		s.append(name);
		s.append("/");
		s.append(arity);
		s.append(" := {\n");
		
		model.stream()
			.forEach(p -> {
				s.append(indentation(indent + 1));
				s.append("(");
				s.append(p.getValue0().stream().map(aconst -> aconst != null ? aconst.toSourceString() : "_").collect(Collectors.joining(", ")));
				s.append(") -> {\n");
				s.append(
					p.getValue1().elements().stream()
						.map(e -> indentation(indent + 2) + "(" + e.getValue0() + ", " + e.getValue1() + ") -> " + p.getValue1().probabilityOf(e).toString())
						.collect(Collectors.joining(",\n"))
				);
				s.append("\n");
				s.append(indentation(indent + 1));
				s.append("}\n");
			});
		
		s.append(indentation(indent));
		s.append("}");
		
		return s.toString();
	}

}

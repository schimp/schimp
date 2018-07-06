package uk.ac.bham.cs.schimp.lang.expression.arith;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class ArithmeticExpressionProbabilityMassFunction {
	
	private Map<ArithmeticExpression, ArithmeticExpression> pmf;
	
	public ArithmeticExpressionProbabilityMassFunction() {
		pmf = new LinkedHashMap<ArithmeticExpression, ArithmeticExpression>();
	}
	
	public void add(ArithmeticExpression aexp, ArithmeticExpression probability) {
		// TODO: don't allow existing elements to be overwritten: this is a syntax error
		pmf.put(aexp, probability);
	}
	
	public Set<ArithmeticExpression> elements() {
		return pmf.keySet();
	}
	
	public ArithmeticExpression probabilityOf(ArithmeticExpression element) {
		return pmf.getOrDefault(element, new ArithmeticConstant(0));
	}
	
	private String indentation(int indent) {
		return StringUtils.repeat("  ", indent);
	}
	
	@Override
	public String toString() {
		return this.toString(0);
	}
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append(pmf.keySet().stream().map(e -> e.toString() + " -> " + pmf.get(e).toString()).collect(Collectors.joining(", ")));
		
		return s.toString();
	}
	
}

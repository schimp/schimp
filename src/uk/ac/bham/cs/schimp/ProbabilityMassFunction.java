package uk.ac.bham.cs.schimp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.BigFraction;

public class ProbabilityMassFunction<T> {
	
	private Map<T, BigFraction> pmf;
	
	public ProbabilityMassFunction() {
		pmf = new LinkedHashMap<T, BigFraction>();
	}
	
	public void add(T element, BigFraction probability) {
		// TODO: don't allow existing elements to be overwritten: this is a syntax error
		pmf.put(element, probability);
	}
	
	public void add(T element, int probability) {
		// TODO: don't allow existing elements to be overwritten: this is a syntax error
		pmf.put(element, new BigFraction(probability));
	}
	
	public void add(T element, int probabilityNumerator, int probabilityDenominator) {
		// TODO: don't allow existing elements to be overwritten: this is a syntax error
		pmf.put(element, new BigFraction(probabilityNumerator, probabilityDenominator));
	}
	
	public Set<T> elements() {
		return pmf.keySet();
	}
	
	public BigFraction probabilityOf(T element) {
		return pmf.getOrDefault(element, new BigFraction(0));
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

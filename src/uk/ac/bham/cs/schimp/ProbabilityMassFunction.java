package uk.ac.bham.cs.schimp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apfloat.Apfloat;

public class ProbabilityMassFunction<T> {
	
	private Map<T, Apfloat> pmf;
	
	public ProbabilityMassFunction() {
		pmf = new LinkedHashMap<T, Apfloat>();
	}
	
	public void add(T element, Apfloat probability) {
		// TODO: don't allow existing elements to be overwritten: this is a syntax error
		pmf.put(element, probability);
	}
	
	public void add(T element, String probability) {
		// TODO: don't allow existing elements to be overwritten: this is a syntax error
		pmf.put(element, new Apfloat(probability, Apfloat.INFINITE));
	}
	
	public void finalise() {
		// TODO: check that the probabilities sum to 1
	}
	
	public Set<T> elements() {
		return pmf.keySet();
	}
	
	public Apfloat probabilityOf(T element) {
		return pmf.getOrDefault(element, new Apfloat(0));
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
		s.append(pmf.keySet().stream().map(e -> e.toString() + " -> " + pmf.get(e).toString(true)).collect(Collectors.joining(", ")));
		
		return s.toString();
	}
	
}

package uk.ac.bham.cs.schimp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
	
}

package uk.ac.bham.cs.schimp.exec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.bham.cs.schimp.lang.Program;
import uk.ac.bham.cs.schimp.lang.command.VariableAssignmentCommand;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpression;
import uk.ac.bham.cs.schimp.source.SourceFile;
import uk.ac.bham.cs.schimp.source.SyntaxException;

// based on http://phrogz.net/lazy-cartesian-product

public class VariableValueCartesianProduct {
	
	private List<String> varNames;
	private List<List<ArithmeticExpression>> aexps;
	private int size;
	private int[] dividends;
	private int[] moduli;
	
	public VariableValueCartesianProduct(List<? extends VariableAssignmentCommand> assignmentCommands) {
		varNames = assignmentCommands.stream().map(c -> c.getVariableReference().getName()).collect(Collectors.toList());
		aexps = new ArrayList<>(Collections.nCopies(assignmentCommands.size(), null));
		dividends = new int[assignmentCommands.size()];
		moduli = new int[assignmentCommands.size()];
		
		int dividend = 1;
		for (int i = assignmentCommands.size() - 1; i >= 0; i--) {
			aexps.set(i, new ArrayList<>(assignmentCommands.get(i).getArithmeticExpressionProbabilityMassFunction().elements()));
			dividends[i] = dividend;
			moduli[i] = aexps.get(i).size();
			dividend *= aexps.get(i).size();
		}
		
		size = dividend;
	}
	
	public int size() {
		return size;
	}
	
	public VariableScopeFrame get(int i) {
		VariableScopeFrame frame = new VariableScopeFrame(VariableScopeFrame.Type.BLOCK);
		
		try {
			for (int j = 0; j < varNames.size(); j++) {
				int aexpIndex = Math.floorDiv(i, dividends[j]) % moduli[j];
				frame.define(
					varNames.get(j),
					aexps.get(j).get(aexpIndex).evaluate(frame)
				);
			}
		} catch (EvaluationException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return frame;
	}
	
	public static void main(String[] args) throws IOException, SyntaxException {
		SourceFile source = new SourceFile(new File("examples/simple.schimp"));
		Program p = source.parse(null);
		
		VariableValueCartesianProduct cp = new VariableValueCartesianProduct(p.getInitialCommands());
		
		int size = cp.size();
		System.out.println("Total: " + size);
		
		for (int i = 0; i < size; i++) {
			System.out.println(cp.get(i).toShortString());
		}
	}

}

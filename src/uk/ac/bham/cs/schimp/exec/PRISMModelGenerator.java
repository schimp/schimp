package uk.ac.bham.cs.schimp.exec;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import parser.ast.RewardStruct;
import parser.type.Type;
import parser.type.TypeInt;
import prism.ModelGenerator;
import prism.ModelType;
import prism.Prism;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLangException;
import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.lang.Program;
import uk.ac.bham.cs.schimp.source.SourceFile;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class PRISMModelGenerator implements ModelGenerator {
	
	// the schimp program being executed by this model generator 
	private Program program;
	
	// a Map from ids representing unique prism State objects to corresponding schimp ProgramExecutionContext objects
	private Map<Integer, ProgramExecutionContext> schimpExecutionContexts = new HashMap<>();
	
	private Map<String, Integer> schimpExecutionContextHashes = new HashMap<>();
	
	// the last allocated id for representing prism State objects in schimpExecutionContexts
	private int lastStateID = 0;
	
	// the prism State object that is currently being explored
	private State exploringState;
	//private ProgramExecutionContext executingContext;
	//private int exploringStateID;
	
	private State[] succeedingStates;
	private double[] succeedingStateProbabilities;
	
	// the probability distribution over the ProgramExecutionStates that succeed the one currently being executed
	//private ProbabilityMassFunction<ProgramExecutionContext> succeedingContexts;
	
	private static List<String> prismVarNames = Arrays.asList("id");
	private static List<Type> prismVarTypes = Arrays.asList(TypeInt.getInstance());
	
	public PRISMModelGenerator(Program program) {
		this.program = program;
	}
	
	//==========================================================================
	// the execution of schimp programs is modelled as a discrete-time markov chain
	
	@Override
	public ModelType getModelType() {
		return ModelType.DTMC;
	}
	
	//==========================================================================
	// the prism State object representing a schimp program execution consists of a single integer: a unique id that
	// can be used to map back to a more detailed ProgramExecutionContext object describing the state of the schimp
	// program in more detail
	
	@Override
	public int getNumVars() {
		return 1;
	}
	
	@Override
	public List<String> getVarNames() {
		return prismVarNames;
	}

	@Override
	public List<Type> getVarTypes() {
		return prismVarTypes;
	}
	
	@Override
	public int getVarIndex(String name) {
		return getVarNames().indexOf(name);
	}

	@Override
	public String getVarName(int i) {
		return getVarNames().get(i);
	}
	
	@Override
	public VarList createVarList() {
		VarList varList = new VarList();
		try {
			varList.addVar(new Declaration("id", new DeclarationInt(Expression.Int(1), Expression.Int(Integer.MAX_VALUE))), 0, null);
		} catch (PrismLangException e) {}
		return varList;
	}
	
	@Override
	public boolean containsUnboundedVariables() {
		// prism States don't contain any unbounded variables
		return false;
	}

	@Override
	public Values getConstantValues() {
		// prism States don't contain any constant values
		return new Values();
	}
	
	@Override
	public void setSomeUndefinedConstants(Values someValues) throws PrismException {
		// prism States don't contain any constant values
		if (someValues != null && someValues.getNumValues() > 0) {
			throw new PrismException("This model has no constants to set");
		}
	}
	
	//==========================================================================
	// we don't currently use labels
	
	@Override
	public int getNumLabels() {
		return 0;
	}
	
	@Override
	public List<String> getLabelNames() {
		return Collections.<String>emptyList();
	}
	
	@Override
	public int getLabelIndex(String name) {
		return -1;
	}

	@Override
	public String getLabelName(int i) throws PrismException {
		throw new PrismException("Label number \"" + i + "\" not defined");
	}
	
	@Override
	public boolean isLabelTrue(String label) throws PrismException {
		throw new PrismException("Label \"" + label + "\" not defined");
	}

	@Override
	public boolean isLabelTrue(int i) throws PrismException {
		throw new PrismException("Label number \"" + i + "\" not defined");
	}
	
	//==========================================================================
	
	@Override
	public boolean hasSingleInitialState() throws PrismException {
		return true;
	}
	
	@Override
	public State getInitialState() throws PrismException {
		// this method is expected to return a fresh copy of the initial state, so create a new ProgramExecutionContext
		// object to tie to the prism State object
		lastStateID++;
		
		ProgramExecutionContext context = ProgramExecutionContext.initialContext(program);
		
		schimpExecutionContexts.put(lastStateID, context);
		schimpExecutionContextHashes.put(context.toHash(), lastStateID);
		return new State(1).setValue(0, lastStateID);
	}
	
	@Override
	public List<State> getInitialStates() throws PrismException {
		return Collections.singletonList(getInitialState());
	}

	@Override
	public void exploreState(State exploreState) throws PrismException {
		// get the schimp ProgramExecutionContext associated with this prism State via the id in the prism State
		exploringState = exploreState;
		int exploringStateID = (int)exploreState.varValues[0];
		//System.out.println("exploreState: " + exploringStateID);
		ProgramExecutionContext executingContext = schimpExecutionContexts.get(exploringStateID);
		//System.out.println(executingContext.toString());
		
		// execute the next command in the ProgramExecutionContext to discover the probability distribution over its
		// succeeding ProgramExecutionContexts; if the next command is null, the schimp program has terminated in this
		// ProgramExecutionContext, and its only succeeding ProgramExecutionContext is itself (i.e. a self-loop)
		ProbabilityMassFunction<ProgramExecutionContext> succeedingContexts;
		if (executingContext.executingCommand == null) {
			//System.out.println(exploringStateID + ": terminating state");
			succeedingContexts = new ProbabilityMassFunction<>();
			succeedingContexts.add(executingContext, "1");
			succeedingContexts.finalise();
		} else {
			try {
				succeedingContexts = executingContext.executingCommand.execute(executingContext);
			} catch (ProgramExecutionException e) {
				// TODO: wrap this properly
				e.printStackTrace(System.err);
				throw new PrismException(e.getMessage());
			}
			
			// now that this State has been explored and its succeeding states can be mapped, its corresponding
			// ProgramExecutionContext object isn't needed any more - remove it from schimpExecutionContexts to free up
			// some memory
			schimpExecutionContexts.remove(exploringStateID);
		}
		
		// assign a prism State object to each succeeding ProgramExecutionContext; if a particular
		// ProgramExecutionContext has been encountered before, reuse the previous id when creating the succeeding prism
		// State object
		succeedingStates = new State[succeedingContexts.elements().size()];
		succeedingStateProbabilities = new double[succeedingContexts.elements().size()];
		int index = 0;
		for (ProgramExecutionContext c : succeedingContexts.elements()) {
			//System.out.println("succeeding state " + index + ", p=" + succeedingContexts.probabilityOf(c).doubleValue() + ":");
			//System.out.println(c.toString());
			String contextHash = c.toHash();
			
			int succeedingStateID;
			if (schimpExecutionContextHashes.containsKey(contextHash)) {
				succeedingStateID = schimpExecutionContextHashes.get(contextHash);
				//System.out.println("state already seen, id " + succeedingStateID);
			} else {
				succeedingStateID = ++lastStateID;
				//System.out.println("new state, id " + succeedingStateID);
				schimpExecutionContexts.put(succeedingStateID, c);
				schimpExecutionContextHashes.put(contextHash, succeedingStateID);
			}
			
			succeedingStates[index] = new State(1).setValue(0, succeedingStateID);
			succeedingStateProbabilities[index] = succeedingContexts.probabilityOf(c).doubleValue();
			
			index++;
		}
	}

	@Override
	public State getExploreState() {
		return exploringState;
	}
	
	//==========================================================================
	
	@Override
	public int getNumTransitions() throws PrismException {
		return succeedingStates.length;
	}
	
	@Override
	public int getNumTransitions(int i) throws PrismException {
		// the value of i is irrelevant here, as the execution of schimp programs doesn't involve nondeterministic
		// choice
		return getNumTransitions();
	}
	
	@Override
	public State computeTransitionTarget(int i, int offset) throws PrismException {
		// the value of i is irrelevant here, as the execution of schimp programs doesn't involve nondeterministic
		// choice
		return succeedingStates[offset];
	}
	
	@Override
	public double getTransitionProbability(int i, int offset) throws PrismException {
		// the value of i is irrelevant here, as the execution of schimp programs doesn't involve nondeterministic
		// choice
		return succeedingStateProbabilities[offset];
	}

	@Override
	public Object getTransitionAction(int i) throws PrismException {
		// we don't currently use transition actions
		return null;
	}

	@Override
	public Object getTransitionAction(int i, int offset) throws PrismException {
		// we don't currently use transition actions
		return null;
	}
	
	//==========================================================================
	// the execution of schimp programs doesn't involve nondeterministic choice
	
	@Override
	public int getNumChoices() throws PrismException {
		return 1;
	}
	
	@Override
	public Object getChoiceAction(int i) throws PrismException {
		return null;
	}
	
	//==========================================================================
	// we don't currently use reward structures

	@Override
	public int getNumRewardStructs() {
		return 0;
	}

	@Override
	public RewardStruct getRewardStruct(int i) {
		return null;
	}

	@Override
	public int getRewardStructIndex(String name) {
		return -1;
	}

	@Override
	public List<String> getRewardStructNames() {
		return Collections.<String>emptyList();
	}

	@Override
	public boolean rewardStructHasTransitionRewards(int i) {
		return false;
	}
	
	@Override
	public double getStateReward(int r, State state) throws PrismException {
		return 0;
	}

	@Override
	public double getStateActionReward(int r, State state, Object action) throws PrismException {
		return 0;
	}
	
	//==========================================================================
	
	public static void main(String[] args) {
		try {
			SourceFile source = new SourceFile(new File(args[0]));
			Program p = source.parse();
			System.out.println(p.toString());
			
			Prism prism = new Prism(new PrismFileLog("stdout"));
			prism.initialise();
			prism.setEngine(Prism.EXPLICIT);
			
			PRISMModelGenerator modelGenerator = new PRISMModelGenerator(p);
			prism.loadModelGenerator(modelGenerator);
			prism.exportTransToFile(true, Prism.EXPORT_DOT_STATES, new File(args[0] + ".dot"));
		} catch (IOException e) {
			// TODO: handle this correctly
			e.printStackTrace();
			System.exit(1);
		} catch (SyntaxException e) {
			// TODO: handle this correctly
			e.printStackTrace();
			System.exit(1);
		} catch (PrismException e) {
			// TODO: handle this correctly
			e.printStackTrace();
			System.exit(1);
		}
	}

}

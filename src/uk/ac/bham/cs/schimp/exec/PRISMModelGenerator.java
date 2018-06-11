package uk.ac.bham.cs.schimp.exec;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javatuples.Pair;

import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
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
import uk.ac.bham.cs.schimp.source.FunctionModelSourceFile;
import uk.ac.bham.cs.schimp.source.SourceFile;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class PRISMModelGenerator implements ModelGenerator {
	
	// the schimp program being executed by this model generator 
	private Program program;
	
	// if set to true, deterministic transitions between states are not represented in the generated model
	private boolean collapseDeterministicTransitions;
	
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
	
	private Map<Integer, Integer> stateTimeConsumptions = new HashMap<>();
	private Map<Integer, Integer> statePowerConsumptions = new HashMap<>();
	
	// the probability distribution over the ProgramExecutionStates that succeed the one currently being executed
	//private ProbabilityMassFunction<ProgramExecutionContext> succeedingContexts;
	
	private static List<String> prismVarNames = Arrays.asList("id");
	private static List<Type> prismVarTypes = Arrays.asList(TypeInt.getInstance());
	
	public PRISMModelGenerator(Program program, boolean collapseDeterministicTransitions) {
		this.program = program;
		this.collapseDeterministicTransitions = collapseDeterministicTransitions;
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
		// succeeding ProgramExecutionContexts
		ProbabilityMassFunction<ProgramExecutionContext> succeedingContexts;
		if (executingContext.isTerminating()) {
			System.out.println(exploringStateID + ": terminating state");
			System.out.println(executingContext.toString());
			
			// if the schimp program has terminated in this ProgramExecutionContext, its only succeeding
			// ProgramExecutionContext is itself (i.e. a self-loop)
			succeedingContexts = new ProbabilityMassFunction<>();
			succeedingContexts.add(executingContext, "1");
			succeedingContexts.finalise();
		} else {
			try {
				// if there is a single succeeding ProgramExecutionContext in which the
				// schimp program hasn't terminated and if we are collapsing deterministic transitions, continue executing
				// succeeding ProgramExecutionContexts until we encounter multiple succeeding ProgramExecutionContexts or a
				// terminating ProgramExecutionContext
				if (collapseDeterministicTransitions) {
					ProgramExecutionContext[] c = { executingContext };
					do {
						succeedingContexts = c[0].executingCommand.execute(c[0]);
					} while (
						succeedingContexts.elements().size() == 1 &&
						!(c = succeedingContexts.elements().toArray(c))[0].isTerminating()
					);
				} else {
					succeedingContexts = executingContext.executingCommand.execute(executingContext);
				}
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
		int succeedingStateCount = succeedingContexts.elements().size();
		succeedingStates = new State[succeedingStateCount];
		succeedingStateProbabilities = new double[succeedingStateCount];
		
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
			
			stateTimeConsumptions.put(succeedingStateID, c.elapsedTime - executingContext.elapsedTime);
			statePowerConsumptions.put(succeedingStateID, c.totalPowerConsumption - executingContext.totalPowerConsumption);
			
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
	// reward structures are used to represent the time and power consumption of schimp programs:
	// 0 -> time consumption
	// 1 -> power consumption

	@Override
	public int getNumRewardStructs() {
		return 2;
	}

	@Override
	public List<String> getRewardStructNames() {
		return Arrays.asList("time", "power");
	}

	@Override
	public boolean rewardStructHasTransitionRewards(int i) {
		// rewards are tied to states, not transitions
		return false;
	}
	
	@Override
	public double getStateReward(int r, State state) throws PrismException {
		int stateID = (int)state.varValues[0];
		
		return r == 0 ? stateTimeConsumptions.get(stateID) : statePowerConsumptions.get(stateID);
	}

	/*
	@Override
	public double getStateActionReward(int r, State state, Object action) throws PrismException {
		return 0;
	}
	*/
	
	//==========================================================================
	
	public static void main(String[] args) {
		try {
			Map<Pair<String, Integer>, FunctionModel> functionModels = null;
			if (args.length > 1) {
				FunctionModelSourceFile functionModelSource = new FunctionModelSourceFile(new File(args[1]));
				functionModels = functionModelSource.parse();
			}
			
			SourceFile source = new SourceFile(new File(args[0]));
			Program p = source.parse(functionModels);
			System.out.println(p.toString());
			
			Prism prism = new Prism(new PrismFileLog("stdout"));
			prism.initialise();
			prism.setEngine(Prism.EXPLICIT);
			
			PRISMModelGenerator modelGenerator = new PRISMModelGenerator(p, false);
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

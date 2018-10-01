package uk.ac.bham.cs.schimp.exec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import prism.PrismException;
import prism.PrismLangException;
import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.lang.Program;

public class PRISMModelGenerator implements ModelGenerator {
	
	// the execution phase of a particular schimp program, designed for storage in a variable in prism State objects:
	public enum Phase {
		// 1: the schimp program is executing
		PROGRAM_EXECUTING,
		// 2: the schimp program has terminated
		PROGRAM_TERMINATED,
		// 3: the attacker has guessed values for the tracked initial variables
		ATTACKER_GUESSED
	}
	
	public static final EnumMap<Phase, Integer> PHASE_IDS = new EnumMap<>(Phase.class);
	static {
		PHASE_IDS.put(Phase.PROGRAM_EXECUTING, 1);
		PHASE_IDS.put(Phase.PROGRAM_TERMINATED, 2);
		PHASE_IDS.put(Phase.ATTACKER_GUESSED, 3);
	}
	
	//==========================================================================
	
	// the schimp program being executed by this model generator 
	private Program program;
	
	// if set to true, cumulative elapsed time and consumed power respectively are stored as variables in the prism
	// State object as well as in reward structures
	private boolean stateTime = false;
	private boolean statePower = false;
	
	// the names of the initial variables whose values should be tracked in prism State objects
	private List<String> stateInitialVars;
	private int stateInitialVarsOffset = 3;
	
	// if set to true, deterministic transitions between states are not represented in the generated model
	private boolean collapseDeterministicTransitions;
	
	// a map from ids to unique schimp ProgramExecutionContext objects
	private Map<Integer, ProgramExecutionContext> schimpExecutionContexts = new HashMap<>();
	private int lastContextID = 0;
	// a reverse map for schimpExecutionContexts, but based on (much shorter) hashes of ProgramExecutionContext strings
	private Map<String, Integer> schimpExecutionContextHashes = new HashMap<>();
	
	// a map from unique (stringified) lists of schimp program outputs to ids
	private Map<String, Integer> schimpExecutionContextOutputLists = new HashMap<>();
	private int lastOutputListID = 0;
	
	// the prism State object that is currently being explored
	private State exploringState;
	
	// various information relating to the State objects that succeed the one currently being explored
	private State[] succeedingStates;
	private double[] succeedingStateProbabilities;
	private int succeedingChoices;
	private int succeedingTransitions;
	
	private Map<Integer, Integer> stateTimeConsumptions = new HashMap<>();
	private Map<Integer, Integer> statePowerConsumptions = new HashMap<>();
	
	// the names of the variables defined in each prism State object
	private List<String> prismVarNames = new ArrayList<String>();
	
	// the names of the observable variables defined in each prism State object (a subset of prismVarNames)
	private List<String> prismObservableVarNames = new ArrayList<String>();
	
	// the types of the variables defined in each prism State object
	private List<Type> prismVarTypes;
	
	private State emptyAttackerGuessedState;
	
	// a cartesian product iterator for the possible values of initial variables recorded in prism State objects
	private VariableValueCartesianProduct varValueProduct;
	
	//==========================================================================
	
	public PRISMModelGenerator(Program program, boolean stateTime, boolean statePower, List<String> stateInitialVars, boolean collapseDeterministicTransitions) {
		this.program = program;
		this.stateTime = stateTime;
		this.statePower = statePower;
		this.stateInitialVars = stateInitialVars;
		this.collapseDeterministicTransitions = collapseDeterministicTransitions;
		
		varValueProduct = new VariableValueCartesianProduct(
			program.getInitialCommands().stream()
				.filter(c -> stateInitialVars.indexOf(c.getVariableReference().getName()) != -1)
				.collect(Collectors.toList())
		);
		
		// the names of the variables defined in each prism State object are:
		// - the unique id representing the ProgramExecutionContext associated with this State
		prismVarNames.add("[cid]");
		// - the execution phase of the schimp program, by its phase id (see PRISMModelGenerator.Phase)
		prismVarNames.add("[phase]");
		prismObservableVarNames.add("[phase]");
		// - the unique id representing the list of outputs observed so far from the schimp program
		prismVarNames.add("[oid]");
		prismObservableVarNames.add("[oid]");
		// - the cumulative elapsed time of the schimp program (if stateTime is true)
		if (stateTime) {
			prismVarNames.add("[time]");
			prismObservableVarNames.add("[time]");
			stateInitialVarsOffset++;
		}
		// - the cumulative power consumption of the schimp program (if statePower is true)
		if (statePower) {
			prismVarNames.add("[power]");
			prismObservableVarNames.add("[power]");
			stateInitialVarsOffset++;
		}
		// - the names of the schimp program's initial variables to record in the prism state, so they can be included
		//   in prism property queries
		prismVarNames.addAll(stateInitialVars);
		
		// these variables are all integers
		prismVarTypes = prismVarNames.stream()
			.map(v -> TypeInt.getInstance())
			.collect(Collectors.toList());
		
		emptyAttackerGuessedState = new State(prismVarNames.size());
		emptyAttackerGuessedState.varValues = new Object[prismVarNames.size()];
		Arrays.fill(emptyAttackerGuessedState.varValues, -1);
		emptyAttackerGuessedState.varValues[1] = PHASE_IDS.get(Phase.ATTACKER_GUESSED);
		System.out.println(emptyAttackerGuessedState.toString());
	}
	
	public boolean stateTime() {
		return stateTime;
	}
	
	public boolean statePower() {
		return statePower;
	}
	
	public List<String> stateInitialVariableNames() {
		return stateInitialVars;
	}
	
	public ProgramExecutionContext getSCHIMPExecutionContext(int i) {
		return schimpExecutionContexts.get(i);
	}
	
	//==========================================================================
	// the execution of schimp programs is modelled as a partially-observable markov decision process
	
	@Override
	public ModelType getModelType() {
		return ModelType.POMDP;
	}
	
	//==========================================================================
	// the prism State object representing a schimp program execution contains the following variables (all integers):
	// - "[cid]": a unique id that maps to a ProgramExecutionContext object (stored in schimpExecutionContexts)
	//            describing the state of the schimp program in more detail
	// - "[phase]": the id representing the execution phase of the schimp program (see PRISMModelGenerator.Phase)
	// - "[oid]": a unique id representing a (stringified) list of outputs (stored in schimpExecutionContextOutputLists)
	//            observed so far from the schimp program
	// - "[time]": the cumulative elapsed time of the schimp program (if stateTime is true)
	// - "[power]": the cumulative power consumption of the schimp program (if statePower is true)
	// - "i1".."in": one variable representing the value of each initial variable declared in the schimp program whose
	//               value is to be recorded in the prism State object, in the order in which the initial variables were
	//               declared in the schimp program; the value of an initial variable is Integer.MIN_VALUE if it has not
	//               yet been declared at this point in the schimp program
	
	@Override
	public int getNumVars() {
		return prismVarTypes.size();
	}
	
	@Override
	public List<String> getVarNames() {
		return prismVarNames;
	}

	@Override
	public List<Type> getVarTypes() {
		return prismVarTypes;
	}
	
	public List<String> getObservableVars() {
		return prismObservableVarNames;
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
			varList.addVar(new Declaration("[cid]", new DeclarationInt(Expression.Int(-1), Expression.Int(Integer.MAX_VALUE))), 0, null);
			varList.addVar(new Declaration("[phase]", new DeclarationInt(Expression.Int(1), Expression.Int(PHASE_IDS.size()))), 0, null);
			varList.addVar(new Declaration("[oid]", new DeclarationInt(Expression.Int(-1), Expression.Int(Integer.MAX_VALUE))), 0, null);
			if (stateTime) varList.addVar(new Declaration("[time]", new DeclarationInt(Expression.Int(-1), Expression.Int(Integer.MAX_VALUE))), 0, null);
			if (statePower) varList.addVar(new Declaration("[power]", new DeclarationInt(Expression.Int(-1), Expression.Int(Integer.MAX_VALUE))), 0, null);
			for (int i = stateInitialVarsOffset; i < prismVarNames.size(); i++) {
				varList.addVar(new Declaration(prismVarNames.get(i), new DeclarationInt(Expression.Int(Integer.MIN_VALUE), Expression.Int(Integer.MAX_VALUE))), 0, null);
			}
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
	// labels are not used
	
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
		return createStateFromProgramExecutionContextID(getProgramExecutionContextID(ProgramExecutionContext.initialContext(program)));
	}
	
	@Override
	public List<State> getInitialStates() throws PrismException {
		return Collections.singletonList(getInitialState());
	}
	
	@Override
	public State getExploreState() {
		return exploringState;
	}
	
	@Override
	public void exploreState(State exploreState) throws PrismException {
		exploringState = exploreState;
		
		// how this State should be explored depends on what phase the schimp program is in:
		int phase = (int)exploreState.varValues[1];
		// - if the program is still executing, find the succeeding ProgramExecutionContexts for the
		//   ProgramExecutionContext associated with this State and map those ProgramExecutionContexts to State objects
		if (phase == PHASE_IDS.get(Phase.PROGRAM_EXECUTING)) {
			exploreExecutingProgramExecutionContext(exploreState);
		// - if the program has terminated, the succeeding States represent the success of the attacker in
		//   guessing the value of each initial variable (modelled as non-deterministic choices) - rather than
		//   precomputing these choices/states and caching them in succeedingStates for later retrieval (as above),
		//   compute the cartesian product of the possible initial variable values on demand in getChoiceAction(), since
		//   the possible combinations can be computed quickly by their index (see VariableValueCartesianProduct) and
		//   precomputing them would potentially consume an enormous amount of memory
		} else if (phase == PHASE_IDS.get(Phase.PROGRAM_TERMINATED)) {
			succeedingStates = null;
			succeedingStateProbabilities = null;
			succeedingChoices = varValueProduct.size();
			succeedingTransitions = 1;
			
		// - if this State represents the outcome of the attacker guessing the value of each initial variable, there are
		//   no more states to be explored here; create a self-loop
		} else {
			succeedingStates = new State[] { new State(exploreState) };
			succeedingStateProbabilities = new double[] { 1 };
			succeedingChoices = 1;
			succeedingTransitions = 1;
		}
	}

	private void exploreExecutingProgramExecutionContext(State exploreState) throws PrismException {
		int exploringContextID = (int)exploreState.varValues[0];
		ProgramExecutionContext exploringContext = schimpExecutionContexts.get(exploringContextID);
		
		// discover the probability distribution over the ProgramExecutionContexts succeeding this one - this process
		// differs depending on whether we need to collapse deterministic transitions between ProgramExecutionContexts:
		ProbabilityMassFunction<ProgramExecutionContext> succeedingContexts;
		// - if we need to collapse deterministic transitions, discover the succeeding ProgramExecutionContexts by
		//   executing the commands in as many deterministic succeeding ProgramExecutionContexts as possible, or until a
		//   terminating ProgramExecutionContext is reached
		if (collapseDeterministicTransitions) {
			//int advancedStates = 0;
			succeedingContexts = new ProbabilityMassFunction<>();
			succeedingContexts.add(exploringContext, 1);
			
			while (succeedingContexts.elements().size() == 1) {
				ProgramExecutionContext c = succeedingContexts.elements().toArray(new ProgramExecutionContext[1])[0];
				if (c.isTerminating()) break;
				
				//System.out.println("executing: " + c.executingCommand.toString());
				try {
					succeedingContexts = c.executingCommand.execute(c);
				} catch (ProgramExecutionException e) {
					// TODO: wrap this properly
					e.printStackTrace(System.err);
					throw new PrismException(e.getMessage());
				}
				//System.out.println("succeeding states:");
				//System.out.println(succeedingContexts.toString());
				
				//advancedStates++;
			}
			
			//System.out.println(exploringStateID + ": advanced " + advancedStates + " state(s)");
			//System.out.println(exploringStateID + ": succeeding states:");
			//System.out.println(succeedingContexts.toString());
		// - if we don't need to collapse deterministic transitions, discover the succeeding ProgramExecutionContexts by
		//   executing the next command in the current ProgramExecutionContext
		} else {
			try {
				succeedingContexts = exploringContext.executingCommand.execute(exploringContext);
			} catch (ProgramExecutionException e) {
				// TODO: wrap this properly
				e.printStackTrace(System.err);
				throw new PrismException(e.getMessage());
			}
			
			// now that this State has been explored and its succeeding states can be mapped, its corresponding
			// ProgramExecutionContext object isn't needed any more - remove it from schimpExecutionContexts to free up
			// some memory
			//schimpExecutionContexts.remove(exploringStateID);
		}
		
		// create a State from each succeeding ProgramExecutionContext
		succeedingChoices = 1;
		succeedingTransitions = succeedingContexts.elements().size();
		succeedingStates = new State[succeedingTransitions];
		succeedingStateProbabilities = new double[succeedingTransitions];
		
		int index = 0;
		for (ProgramExecutionContext c : succeedingContexts.elements()) {
			int succeedingContextID = getProgramExecutionContextID(c);
			
			succeedingStates[index] = createStateFromProgramExecutionContextID(succeedingContextID);
			succeedingStateProbabilities[index] = succeedingContexts.probabilityOf(c).doubleValue();
			
			// for the state reward information, store the instantaneous (rather than cumulative) elapsed time and power
			// consumption (i.e., the time elapsed and power consumed solely as a result of transitioning into this new
			// state)
			stateTimeConsumptions.put(succeedingContextID, c.elapsedTime - exploringContext.elapsedTime);
			statePowerConsumptions.put(succeedingContextID, c.totalPowerConsumption - exploringContext.totalPowerConsumption);
			
			index++;
		}
	}
	
	private State createStateFromProgramExecutionContextID(int contextID) {
		ProgramExecutionContext context = schimpExecutionContexts.get(contextID);
		
		State state = new State(prismVarTypes.size());
		int nextIndex = 0;
		
		// - "[cid]"
		state.setValue(nextIndex++, contextID);
		// - "[phase]" (but never Phase.ATTACKER_GUESSED, as this can't be derived from a ProgramExecutionContext)
		state.setValue(nextIndex++, context.isTerminating() ? PHASE_IDS.get(Phase.PROGRAM_TERMINATED) :PHASE_IDS.get(Phase.PROGRAM_EXECUTING));
		// - "[oid]"
		state.setValue(nextIndex++, getOutputsID(context.outputsToString()));
		// - "[time]" (if stateTime is true)
		if (stateTime) state.setValue(nextIndex++, context.elapsedTime);
		// - "[power]" (if statePower is true)
		if (statePower) state.setValue(nextIndex++, context.totalPowerConsumption);
		// - "i1".."in"
		for (int i = nextIndex; i < prismVarNames.size(); i++) {
			try {
				state.setValue(i, context.initialVariableBindings.evaluate(prismVarNames.get(i)).toFraction().intValue());
			} catch (ProgramExecutionException e) {
				// evaluate() throws a ProgramExecutionException if the given string is not a defined variable name - in
				// this case, set the value of this variable to Integer.MIN_VALUE, indicating that the variable is
				// undefined at this point
				state.setValue(i, Integer.MIN_VALUE);
			}
		}
		
		return state;
	}
	
	private State createStateFromAttackerGuesses(int contextID, VariableScopeFrame guesses) {
		ProgramExecutionContext context = schimpExecutionContexts.get(contextID);
		
		State state = new State(emptyAttackerGuessedState);
		
		for (int i = stateInitialVarsOffset; i < prismVarNames.size(); i++) {
			try {
				state.setValue(i,
					guesses.evaluate(prismVarNames.get(i)).toFraction().intValue() == context.initialVariableBindings.evaluate(prismVarNames.get(i)).toFraction().intValue() ?
					1 : // correct guess for the value of this initial variable
					0   // incorrect guess for the value of this initial variable
				);
			} catch (ProgramExecutionException e) {
				// evaluate() throws a ProgramExecutionException if the given string is not a defined variable name, but
				// this should never happen here
			}
		}
		
		return state;
	}
	
	private int getProgramExecutionContextID(ProgramExecutionContext context) {
		String contextHash = context.toHash();
		
		if (schimpExecutionContextHashes.containsKey(contextHash)) {
			return schimpExecutionContextHashes.get(contextHash);
		} else {
			schimpExecutionContexts.put(++lastContextID, context);
			schimpExecutionContextHashes.put(contextHash, lastContextID);
			return lastContextID;
		}
	}
	
	private int getOutputsID(String outputsList) {
		if (schimpExecutionContextOutputLists.containsKey(outputsList)) {
			return schimpExecutionContextOutputLists.get(outputsList);
		} else {
			schimpExecutionContextOutputLists.put(outputsList, ++lastOutputListID);
			return lastOutputListID;
		}
	}
	
	//==========================================================================
	// non-deterministic choice is only used when exploring succeeding states to ones in which the schimp program is in
	// a terminating ProgramExecutionContext - in this case, the non-deterministic choice is the attacker's guess for
	// the value of each initial variable recorded in the prism State object, and the action label for each choice is a
	// stringified VariableScopeFrame representing the attacker's guesses made in that choice
	
	@Override
	public int getNumChoices() throws PrismException {
		return succeedingChoices;
	}
	
	@Override
	public Object getChoiceAction(int i) throws PrismException {
		return (int)exploringState.varValues[1] == PHASE_IDS.get(Phase.PROGRAM_TERMINATED) ?
			varValueProduct.get(i).toShortString() :
			null;
	}
	
	//==========================================================================
	
	@Override
	public int getNumTransitions() throws PrismException {
		return succeedingTransitions;
	}
	
	@Override
	public int getNumTransitions(int i) throws PrismException {
		// this has been precomputed in one of the explore methods above, so the value of i has no special significance
		// here
		return succeedingTransitions;
	}
	
	@Override
	public State computeTransitionTarget(int i, int offset) throws PrismException {
		return exploringState.varValues[1] == PHASE_IDS.get(Phase.PROGRAM_TERMINATED) ?
			createStateFromAttackerGuesses((int)exploringState.varValues[0], varValueProduct.get(i)) :
			succeedingStates[offset];
	}
	
	@Override
	public double getTransitionProbability(int i, int offset) throws PrismException {
		return (int)exploringState.varValues[1] == PHASE_IDS.get(Phase.PROGRAM_TERMINATED) ?
			1.0 :
			succeedingStateProbabilities[offset];
	}

	@Override
	public Object getTransitionAction(int i) throws PrismException {
		return (int)exploringState.varValues[1] == PHASE_IDS.get(Phase.PROGRAM_TERMINATED) ?
			varValueProduct.get(i).toShortString() :
			null;
	}

	@Override
	public Object getTransitionAction(int i, int offset) throws PrismException {
		return getTransitionAction(i);
	}
	
	//==========================================================================
	// reward structures are used to represent the elapsed time and power consumption of schimp programs:
	// 0 -> elapsed time
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
		int contextID = (int)state.varValues[0];
		
		return r == 0 ? stateTimeConsumptions.get(contextID) : statePowerConsumptions.get(contextID);
	}

}
package uk.ac.bham.cs.schimp.exec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	
	// the schimp program being executed by this model generator 
	private Program program;
	
	// the indices of various pieces of information in the prism State object's variables array; cumulative elapsed time
	// and consumed power are only present as variables if stateTime and statePower respectively are set to true in the
	// call to the constructor, otherwise they will be omitted and their indices will be -1
	private int stateOutputIDIndex = 1;
	private int stateTimeIndex = -1;
	private int statePowerIndex = -1;
	
	// the names of the initial variables whose values should be tracked in prism State objects
	private List<String> stateInitialVars;
	private int stateInitialVarsOffset = 2;
	
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
	
	private Map<Integer, Integer> stateTimeConsumptions = new HashMap<>();
	private Map<Integer, Integer> statePowerConsumptions = new HashMap<>();
	
	// the names of the variables defined in each prism State object
	private List<String> prismVarNames = new ArrayList<String>();
	
	// the types of the variables defined in each prism State object
	private List<Type> prismVarTypes;
	
	//==========================================================================
	
	public PRISMModelGenerator(Program program, boolean stateTime, boolean statePower, List<String> stateInitialVars, boolean collapseDeterministicTransitions) {
		this.program = program;
		this.stateInitialVars = stateInitialVars;
		this.collapseDeterministicTransitions = collapseDeterministicTransitions;
		
		// the variables defined in each prism State object are:
		int varIndex = 1; // 0 = "_cid", 1 = "_oid"; always present
		// - the unique id representing the ProgramExecutionContext associated with this State
		prismVarNames.add("_cid");
		// - the unique id representing the list of outputs observed so far from the schimp program
		prismVarNames.add("_oid");
		// - the cumulative elapsed time of the schimp program (if stateTime is true)
		if (stateTime) {
			prismVarNames.add("_time");
			stateTimeIndex = ++varIndex;
		} else {
			stateTimeIndex = -1;
		}
		// - the cumulative power consumption of the schimp program (if statePower is true)
		if (statePower) {
			prismVarNames.add("_power");
			statePowerIndex = ++varIndex;
		} else {
			statePowerIndex = -1;
		}
		// - the names of the schimp program's initial variables to record in the prism state, so they can be included
		//   in prism property queries
		prismVarNames.addAll(stateInitialVars);
		stateInitialVarsOffset = ++varIndex;
		
		// these variables are all integers
		prismVarTypes = prismVarNames.stream()
			.map(v -> TypeInt.getInstance())
			.collect(Collectors.toList());
	}
	
	public Program getProgram() {
		return program;
	}
	
	public int getStateOutputIDIndex() {
		return stateOutputIDIndex;
	}
	
	public boolean stateHasTime() {
		return stateTimeIndex != -1;
	}
	
	public int getStateTimeIndex() {
		return stateTimeIndex;
	}
	
	public boolean stateHasPower() {
		return statePowerIndex != -1;
	}
	
	public int getStatePowerIndex() {
		return statePowerIndex;
	}
	
	public List<String> stateInitialVariableNames() {
		return stateInitialVars;
	}
	
	public ProgramExecutionContext getSCHIMPExecutionContext(int i) {
		return schimpExecutionContexts.get(i);
	}
	
	//==========================================================================
	// the execution of schimp programs is modelled as a discrete-time markov chain
	
	@Override
	public ModelType getModelType() {
		return ModelType.DTMC;
	}
	
	//==========================================================================
	// the prism State object representing a schimp program execution contains the following variables (all integers):
	// - "_cid": a unique id that maps to a ProgramExecutionContext object (stored in schimpExecutionContexts)
	//           describing the state of the schimp program in more detail
	// - "_oid": a unique id representing a (stringified) list of outputs (stored in schimpExecutionContextOutputLists)
	//           observed so far from the schimp program
	// - "_time": the cumulative elapsed time of the schimp program (if stateTime is true)
	// - "_power": the cumulative power consumption of the schimp program (if statePower is true)
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
			varList.addVar(new Declaration("_cid", new DeclarationInt(Expression.Int(-1), Expression.Int(Integer.MAX_VALUE))), 0, null);
			varList.addVar(new Declaration("_oid", new DeclarationInt(Expression.Int(-1), Expression.Int(Integer.MAX_VALUE))), 0, null);
			if (stateTimeIndex != -1) varList.addVar(new Declaration("_time", new DeclarationInt(Expression.Int(-1), Expression.Int(Integer.MAX_VALUE))), 0, null);
			if (statePowerIndex != -1) varList.addVar(new Declaration("_power", new DeclarationInt(Expression.Int(-1), Expression.Int(Integer.MAX_VALUE))), 0, null);
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
		int exploringContextID = (int)exploreState.varValues[0];
		ProgramExecutionContext exploringContext = schimpExecutionContexts.get(exploringContextID);
		
		if (exploringContext.isTerminating()) {
			succeedingStates = new State[] { new State(exploreState) };
			succeedingStateProbabilities = new double[] { 1 };
		} else {
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
			int succeedingContextTotal = succeedingContexts.elements().size();
			succeedingStates = new State[succeedingContextTotal];
			succeedingStateProbabilities = new double[succeedingContextTotal];
			
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
	}
	
	private State createStateFromProgramExecutionContextID(int contextID) {
		ProgramExecutionContext context = schimpExecutionContexts.get(contextID);
		
		State state = new State(prismVarTypes.size());
		int nextIndex = 0;
		
		// - "[cid]"
		state.setValue(nextIndex++, contextID);
		// - "[oid]"
		state.setValue(nextIndex++, getOutputsID(context.outputsToString()));
		// - "[time]" (if stateTime is true)
		if (stateTimeIndex != -1) state.setValue(nextIndex++, context.elapsedTime);
		// - "[power]" (if statePower is true)
		if (statePowerIndex != -1) state.setValue(nextIndex++, context.totalPowerConsumption);
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
		return 1;
	}
	
	@Override
	public Object getChoiceAction(int i) throws PrismException {
		return null;
	}
	
	//==========================================================================
	
	@Override
	public int getNumTransitions() throws PrismException {
		return succeedingStates.length;
	}
	
	@Override
	public int getNumTransitions(int i) throws PrismException {
		return succeedingStates.length;
	}
	
	@Override
	public State computeTransitionTarget(int i, int offset) throws PrismException {
		return succeedingStates[offset];
	}
	
	@Override
	public double getTransitionProbability(int i, int offset) throws PrismException {
		return succeedingStateProbabilities[offset];
	}

	@Override
	public Object getTransitionAction(int i) throws PrismException {
		return null;
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
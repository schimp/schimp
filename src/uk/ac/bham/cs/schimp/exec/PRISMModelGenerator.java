package uk.ac.bham.cs.schimp.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import explicit.graphviz.Decorator;
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
import prism.PrismLog;
import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.graphviz.StateDecorator;
import uk.ac.bham.cs.schimp.lang.Program;
import uk.ac.bham.cs.schimp.source.FunctionModelSourceFile;
import uk.ac.bham.cs.schimp.source.SourceFile;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class PRISMModelGenerator implements ModelGenerator {
	
	// the schimp program being executed by this model generator 
	private Program program;
	
	// if set to true, cumulative elapsed time and consumed power respectively are stored as variables in the prism
	// State object as well as in reward structures
	private boolean prismStateHasTime = false;
	private boolean prismStateHasPower = false;
	
	// if set to true, deterministic transitions between states are not represented in the generated model
	private boolean collapseDeterministicTransitions;
	
	// a Map from ids representing unique prism State objects to corresponding schimp ProgramExecutionContext objects
	private Map<Integer, ProgramExecutionContext> schimpExecutionContexts = new HashMap<>();
	
	private Map<String, Integer> schimpExecutionContextHashes = new HashMap<>();
	
	// the last allocated id for representing prism State objects in schimpExecutionContexts
	private int lastStateID = 0;
	
	// the prism State object that is currently being explored
	private State exploringState;
	private ProgramExecutionContext executingContext;
	//private int exploringStateID;
	
	private State[] succeedingStates;
	private double[] succeedingStateProbabilities;
	
	private Map<Integer, Integer> stateTimeConsumptions = new HashMap<>();
	private Map<Integer, Integer> statePowerConsumptions = new HashMap<>();
	
	// the probability distribution over the ProgramExecutionStates that succeed the one currently being executed
	//private ProbabilityMassFunction<ProgramExecutionContext> succeedingContexts;
	
	// the names of the variables defined in each prism State object - these are the unique id representing the State
	// object followed by the schimp Program's initial variables in the order in which they are declared
	private List<String> prismVarNames = new ArrayList<String>();
	
	// the types of the variables defined in each prism State object - one integer (representing the State's unique id)
	// followed by one integer per initial variable declared in the schimp Program
	private List<Type> prismVarTypes;
	
	private static List<String> prismLabelNames = Arrays.asList("terminate");
	
	//==========================================================================
	
	public PRISMModelGenerator(Program program, boolean prismStateHasTime, boolean prismStateHasPower, boolean collapseDeterministicTransitions) {
		this.program = program;
		this.prismStateHasTime = prismStateHasTime;
		this.prismStateHasPower = prismStateHasPower;
		this.collapseDeterministicTransitions = collapseDeterministicTransitions;
		
		// the names of the variables defined in each prism State object are:
		// - the unique id representing the State object
		prismVarNames.add("[stateid]");
		// - the cumulative elapsed time (if prismStateHasTime is true)
		if (prismStateHasTime) prismVarNames.add("[time]");
		// - the cumulative power consumption (if prismStateHasPower is true)
		if (prismStateHasPower) prismVarNames.add("[power]");
		// - the schimp Program's initial variables in the order in which they are declared
		prismVarNames.addAll(program.getInitialVariableNames());
		
		// the types of the variables defined in each prism State object are all integers
		prismVarTypes = prismVarNames.stream()
			.map(v -> TypeInt.getInstance())
			.collect(Collectors.toList());
	}
	
	public boolean prismStateHasTime() {
		return prismStateHasTime;
	}
	
	public boolean prismStateHasPower() {
		return prismStateHasPower;
	}
	
	public ProgramExecutionContext getSCHIMPExecutionContext(int i) {
		return schimpExecutionContexts.get(i);
	}
	
	public List<String> getInitialVariableNames() {
		int firstVariableIndex = 1;
		if (prismStateHasTime) firstVariableIndex++;
		if (prismStateHasPower) firstVariableIndex++;
		return prismVarNames.subList(firstVariableIndex, prismVarNames.size());
	}
	
	//==========================================================================
	// the execution of schimp programs is modelled as a discrete-time markov chain
	
	@Override
	public ModelType getModelType() {
		return ModelType.DTMC;
	}
	
	//==========================================================================
	// the prism State object representing a schimp program execution contains the following variables (all integers):
	// - "[state_id]": a unique id that can be used to map back to a more detailed ProgramExecutionContext object
	//   describing the state of the schimp program in more detail
	// - one variable representing the current value of each initial variable declared in the schimp program, in the
	//   order in which the initial variables were declared in the program; the value of an initial variable in the
	//   prism State object is Integer.MIN_VALUE if the variable has not yet been declared at this point in the schimp
	//   program
	
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
			int firstVariableIndex = 1;
			
			varList.addVar(new Declaration("[stateid]", new DeclarationInt(Expression.Int(1), Expression.Int(Integer.MAX_VALUE))), 0, null);
			
			if (prismStateHasTime) {
				varList.addVar(new Declaration("[time]", new DeclarationInt(Expression.Int(0), Expression.Int(Integer.MAX_VALUE))), 0, null);
				firstVariableIndex++;
			}
			
			if (prismStateHasPower) {
				varList.addVar(new Declaration("[power]", new DeclarationInt(Expression.Int(0), Expression.Int(Integer.MAX_VALUE))), 0, null);
				firstVariableIndex++;
			}
			
			for (int i = firstVariableIndex; i < prismVarNames.size(); i++) {
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
	// labels:
	// - "terminate": this ProgramExecutionContext represents a terminating state in the schimp program
	
	@Override
	public int getNumLabels() {
		return prismLabelNames.size();
	}
	
	@Override
	public List<String> getLabelNames() {
		return prismLabelNames;
	}
	
	@Override
	public int getLabelIndex(String name) {
		return prismLabelNames.indexOf(name);
	}

	@Override
	public String getLabelName(int i) throws PrismException {
		try {
			return prismLabelNames.get(i);
		} catch (IndexOutOfBoundsException e) {
			throw new PrismException("Label number \"" + i + "\" not defined");
		}
	}
	
	@Override
	public boolean isLabelTrue(String name) throws PrismException {
		return isLabelTrue(getLabelIndex(name));
	}

	@Override
	public boolean isLabelTrue(int i) throws PrismException {
		switch (i) {
			case 0: // "terminate"
				return executingContext.isTerminating();
		}
		
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
		
		return createStateFromProgramExecutionContextID(lastStateID);
	}
	
	@Override
	public List<State> getInitialStates() throws PrismException {
		return Collections.singletonList(getInitialState());
	}

	@Override
	public void exploreState(State exploreState) throws PrismException {
		// get the schimp ProgramExecutionContext associated with this prism State via the id in the prism State
		exploringState = exploreState;
		int exploringContextID = (int)exploreState.varValues[0];
		//System.out.println("exploreState: " + exploringStateID);
		executingContext = schimpExecutionContexts.get(exploringContextID);
		//System.out.println(executingContext.toString());
		
		// discover the probability distribution over the ProgramExecutionContexts succeeding this one - this process
		// differs depending on whether we need to collapse deterministic transitions between ProgramExecutionContexts:
		ProbabilityMassFunction<ProgramExecutionContext> succeedingContexts;
		// - if we need to collapse deterministic transitions, discover the succeeding ProgramExecutionContexts by
		//   executing the commands in as many deterministic succeeding ProgramExecutionContexts as possible, or until a
		//   terminating ProgramExecutionContext is reached
		if (collapseDeterministicTransitions) {
			//int advancedStates = 0;
			succeedingContexts = new ProbabilityMassFunction<>();
			succeedingContexts.add(executingContext, 1);
			
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
			if (executingContext.isTerminating()) {
				//System.out.println("Context ID " + exploringContextID + ": terminating context");
				
				// if the schimp program has terminated in this ProgramExecutionContext, its only succeeding
				// ProgramExecutionContext is itself (i.e. a self-loop)
				succeedingContexts = new ProbabilityMassFunction<>();
				succeedingContexts.add(executingContext, 1);
			} else {
				//System.out.println("Context ID " + exploringContextID + ": non-terminating context");
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
				//schimpExecutionContexts.remove(exploringStateID);
			}
		}
		
		// create a State from each succeeding ProgramExecutionContext
		int succeedingStateCount = succeedingContexts.elements().size();
		succeedingStates = new State[succeedingStateCount];
		succeedingStateProbabilities = new double[succeedingStateCount];
		
		int index = 0;
		for (ProgramExecutionContext c : succeedingContexts.elements()) {
			//System.out.println("Context ID " + exploringContextID + ": succeeding context #" + index + ", p=" + succeedingContexts.probabilityOf(c).doubleValue() + ":");
			//System.out.println(c.toString());
			String contextHash = c.toHash();
			
			int succeedingContextID;
			if (schimpExecutionContextHashes.containsKey(contextHash)) {
				succeedingContextID = schimpExecutionContextHashes.get(contextHash);
				//System.out.println("Context already seen; reusing context ID " + succeedingContextID);
			} else {
				succeedingContextID = ++lastStateID;
				//System.out.println("New context; assigning context ID " + succeedingContextID);
				schimpExecutionContexts.put(succeedingContextID, c);
				schimpExecutionContextHashes.put(contextHash, succeedingContextID);
			}
			
			succeedingStates[index] = createStateFromProgramExecutionContextID(succeedingContextID);
			succeedingStateProbabilities[index] = succeedingContexts.probabilityOf(c).doubleValue();
			
			// for the state reward information, store the instantaneous (rather than cumulative) elapsed time and power
			// consumption (i.e., the time elapsed and power consumed solely as a result of transitioning into this new
			// state)
			stateTimeConsumptions.put(succeedingContextID, c.elapsedTime - executingContext.elapsedTime);
			statePowerConsumptions.put(succeedingContextID, c.totalPowerConsumption - executingContext.totalPowerConsumption);
			
			index++;
		}
	}
	
	private State createStateFromProgramExecutionContextID(int contextID) {
		ProgramExecutionContext context = schimpExecutionContexts.get(contextID);
		
		State state = new State(prismVarTypes.size());
		int nextIndex = 0;
		
		// the first variable in the State is always the unique id of the ProgramExecutionContext
		state.setValue(nextIndex++, contextID);
		
		// if we're required to store time information in the State, the next variable is the cumulative elapsed time in
		// this ProgramExecutionContext
		if (prismStateHasTime) state.setValue(nextIndex++, context.elapsedTime);
		
		// if we're required to store power information in the State, the next variable is the cumulative consumed power
		// in this ProgramExecutionContext
		if (prismStateHasPower) state.setValue(nextIndex++, context.totalPowerConsumption);
		
		// the remaining variables in the State are the values of the initial variables in this ProgramExecutionContext
		// at the point at which they were declared, in the order in which they are declared in the program
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
		int contextID = (int)state.varValues[0];
		
		return r == 0 ? stateTimeConsumptions.get(contextID) : statePowerConsumptions.get(contextID);
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
			
			PrismLog prismStdout = new PrismFileLog("stdout");
			Prism prism = new Prism(prismStdout);
			prism.initialise();
			prism.setEngine(Prism.EXPLICIT);
			
			PRISMModelGenerator modelGenerator = new PRISMModelGenerator(p, true, true, true);
			prism.loadModelGenerator(modelGenerator);
			prism.buildModelIfRequired();
			
			//prism.exportTransToFile(false, Prism.EXPORT_DOT_STATES, new File(args[0] + ".dot"));
			ArrayList<Decorator> decorators = new ArrayList<Decorator>();
			decorators.add(new StateDecorator(prism.getBuiltModelExplicit().getStatesList(), modelGenerator));
			PrismLog dotFile = new PrismFileLog(args[0] + ".dot");
			prism.getBuiltModelExplicit().exportToDotFile(dotFile, decorators);
			dotFile.flush();
			
			BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));
			String in;
			System.out.print( "check> " );
			while ((in = stdinReader.readLine()) != null) {
				try {
					System.out.println(prism.modelCheck(in).getResult());
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.print( "check> " );
			}
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
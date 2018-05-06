package uk.ac.bham.cs.schimp.lang.command;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.EvaluationException;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionException;
import uk.ac.bham.cs.schimp.lang.Function;
import uk.ac.bham.cs.schimp.lang.FunctionReference;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticExpression;
import uk.ac.bham.cs.schimp.source.ControlFlowContext;
import uk.ac.bham.cs.schimp.source.SyntaxCheckContext;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class InvokeCommand extends Command {
	
	private FunctionReference functionRef;
	private List<ArithmeticExpression> exps;
	private Command functionFirstCommand;
	
	public InvokeCommand(String functionName, List<ArithmeticExpression> exps) {
		super();
		this.exps = exps;
		this.functionRef = new FunctionReference(functionName, this.exps.size());
	}
	
	public InvokeCommand(String functionName, ArithmeticExpression... exps) {
		super();
		this.exps = Arrays.asList(exps);
		this.functionRef = new FunctionReference(functionName, this.exps.size());
	}
	
	public Command endInvocation(ProgramExecutionContext context) {
		context.variableBindings.destroyFunctionScopeFrame();
		
		if (destroyBlockScopeFrames > 0) context.destroyBlockScopeFrames(destroyBlockScopeFrames);
		
		if (functionRef.getFunction().getType() == Function.ResourceConsumptionType.NON_ATOMIC) {
			context.executingNonAtomicFunction = false;
		}
		
		return nextCommand;
	}
	
	@Override
	public void check(SyntaxCheckContext context) throws SyntaxException {
		super.check(context);
		
		functionRef.check(context);
		
		try {
			for (int i = 0; i < exps.size(); i++) {
				exps.get(i).check(context);
			}
		} catch (SyntaxException e) {
			throw e;
		}
	}
	
	@Override
	public void resolveControlFlow(ControlFlowContext context) {
		super.resolveControlFlow(context);
		functionFirstCommand = functionRef.getFunction().getFirstCommand();
	}
	
	@Override
	public ProbabilityMassFunction<ProgramExecutionContext> execute(ProgramExecutionContext context) throws ProgramExecutionException {
		// if the function being invoked is non-atomic, it can only be invoked if a non-atomic function is not already
		// in the process of being executed (i.e. all the way down the invocation stack)
		if (context.executingNonAtomicFunction && functionRef.getFunction().getType() == Function.ResourceConsumptionType.NON_ATOMIC) {
			throw new ProgramExecutionException("Cannot invoke non-atomic function " + functionRef.getFunction().getName() + "/" + functionRef.getFunction().getArity() + " while already executing a non-atomic function");
		}
		
		ProgramExecutionContext succeedingContext = context.clone();
		
		succeedingContext.invocationStack.push(this);
		succeedingContext.variableBindings.createFunctionScopeFrame();
		succeedingContext.setNextCommand(functionFirstCommand);
		
		List<ArithmeticConstant> consts = new LinkedList<>();
		for (int i = 0; i < exps.size(); i++) {
			try {
				ArithmeticConstant c = exps.get(i).evaluate(context);
				consts.add(c);
				succeedingContext.variableBindings.define(functionRef.getFunction().getParameters().get(i).getName(), c);
			} catch (EvaluationException e) {
				// TODO: wrap this exception properly
				throw new ProgramExecutionException(e.getMessage());
			}
		}
		
		ProbabilityMassFunction<ProgramExecutionContext> succeedingPMF = new ProbabilityMassFunction<>();
		
		if (functionRef.getFunction().getType() == Function.ResourceConsumptionType.NON_ATOMIC) {
			// indicate in the ProgramExecutionContext that we're now executing a non-atomic function; this will be
			// reset to false when endInvocation() is called for this function invocation
			succeedingContext.executingNonAtomicFunction = true;
			
			ProbabilityMassFunction<Pair<Integer, Integer>> powerConsumptionPMF = functionRef.getFunction().getResourceConsumption(consts);
			for (Pair<Integer, Integer> tp : powerConsumptionPMF.elements()) {
				ProgramExecutionContext nonAtomicSucceedingContext = succeedingContext.clone();
				
				nonAtomicSucceedingContext.elapsedTime += tp.getValue0();
				nonAtomicSucceedingContext.totalPowerConsumption += tp.getValue1();
				nonAtomicSucceedingContext.powerConsumption.merge(nonAtomicSucceedingContext.elapsedTime, tp.getValue1(), (existingP, newP) -> existingP + newP);
				
				succeedingPMF.add(nonAtomicSucceedingContext, powerConsumptionPMF.probabilityOf(tp));
			}
		} else { // functionRef.getFunction().getType() == Function.ResourceConsumptionType.ATOMIC
			succeedingPMF.add(succeedingContext, "1");
		}
		
		succeedingPMF.finalise();
		
		return succeedingPMF;
	}
	
	public String toString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append("[");
		s.append(id);
		s.append("->");
		s.append("pushfn:" + functionFirstCommand.getID() + ",rtnfn");
		if (destroyBlockScopeFrames != 0) {
			s.append(",dblock:" + destroyBlockScopeFrames);
		}
		s.append(",");
		s.append(nextCommand == null ? "popfn" : nextCommand.getID());
		s.append("] ");
		s.append(functionRef.toString());
		s.append("(");
		s.append(exps.stream().map(exp -> exp.toString()).collect(Collectors.joining(", ")));
		s.append(")");
		
		return s.toString();
	}
	
	public String toSourceString(int indent) {
		StringBuilder s = new StringBuilder();
		
		s.append(indentation(indent));
		s.append(functionRef.toString());
		s.append("(");
		s.append(exps.stream().map(exp -> exp.toSourceString()).collect(Collectors.joining(", ")));
		s.append(")");
		
		return s.toString();
	}
	
}

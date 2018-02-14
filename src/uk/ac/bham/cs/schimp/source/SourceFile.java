package uk.ac.bham.cs.schimp.source;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.ProgramExecutionContext;
import uk.ac.bham.cs.schimp.lang.*;
import uk.ac.bham.cs.schimp.lang.command.*;
import uk.ac.bham.cs.schimp.lang.expression.arith.*;
import uk.ac.bham.cs.schimp.lang.expression.bool.*;
import uk.ac.bham.cs.schimp.parser.SCHIMPBaseVisitor;
import uk.ac.bham.cs.schimp.parser.SCHIMPLexer;
import uk.ac.bham.cs.schimp.parser.SCHIMPParser;

public class SourceFile {
	
	public File file;
	
	public SourceFile(File file) {
		this.file = file;
	}
	
	public String toString() {
		return file.toString();
	}
	
	public Program parse() throws IOException, SyntaxException {
		CharStream charStream = CharStreams.fromPath(file.toPath());
		SCHIMPLexer lexer = new SCHIMPLexer(charStream);
		TokenStream tokens = new CommonTokenStream(lexer);
		SCHIMPParser parser = new SCHIMPParser(tokens);
		
		ProgramVisitor programVisitor = new ProgramVisitor();
		Program program = programVisitor.visit(parser.program());
		program.check(new SyntaxCheckContext());
		program.resolveControlFlow(new ControlFlowContext());
		
		return program;
	}
	
	private static SourceRange contextToSourceRange(ParserRuleContext ctx) {
		return new SourceRange(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine());
	}
	
	private static class ProgramVisitor extends SCHIMPBaseVisitor<Program> {
		@Override
		public Program visitProgram(SCHIMPParser.ProgramContext ctx) {
			InitialCommandVisitor initialCommandVisitor = new InitialCommandVisitor();
			List<InitialCommand> initialCommands = ctx.cmdinitial().stream().map(c -> c.accept(initialCommandVisitor)).collect(Collectors.toList());
			
			FunctionCommandVisitor functionCommandVisitor = new FunctionCommandVisitor();
			List<FunctionCommand> functions = ctx.cmdfunction().stream().map(c -> c.accept(functionCommandVisitor)).collect(Collectors.toList());
			
			// TODO: check that this is actually an invoke command
			CommandVisitor invokeCommandVisitor = new CommandVisitor();
			InvokeCommand invokeCommand = (InvokeCommand)ctx.cmdinvoke().accept(invokeCommandVisitor);
			
			return new Program(initialCommands, functions, invokeCommand);
		}
	}
    
    private static class InitialCommandVisitor extends SCHIMPBaseVisitor<InitialCommand> {
		@Override
		public InitialCommand visitCmdinitial(SCHIMPParser.CmdinitialContext ctx) {
			String variableName = ctx.IDENTIFIER().getText();
			VariableReference variable = new VariableReference(variableName);
			
			ProbabilityMassFunction<ArithmeticExpression> pmfValue;
			if (ctx.aexp() != null) {
				ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
				ArithmeticExpression aexpValue = ctx.aexp().accept(aexpVisitor);
				pmfValue = new ProbabilityMassFunction<ArithmeticExpression>();
				pmfValue.add(aexpValue, "1");
				pmfValue.finalise();
			} else { // if (ctx.pmf() != null)
				ProbabilityMassFunctionVisitor pmfVisitor = new ProbabilityMassFunctionVisitor();
				pmfValue = ctx.pmf().accept(pmfVisitor);
			} // TODO: else throw exception
			
			InitialCommand c = new InitialCommand(variable, pmfValue);
			c.setSourceRange(contextToSourceRange(ctx));
			return c;
		}
	}
	
	private static class FunctionCommandVisitor extends SCHIMPBaseVisitor<FunctionCommand> {
		@Override
		public FunctionCommand visitCmdfunction(SCHIMPParser.CmdfunctionContext ctx) {
			String name = ctx.IDENTIFIER().getText();
			
			VarnamelistVisitor varnamelistVisitor = new VarnamelistVisitor();
			List<VariableReference> parameters = ctx.varnamelist() == null ? Collections.<VariableReference>emptyList() : ctx.varnamelist().accept(varnamelistVisitor);
			
			CmdlistVisitor cmdlistVisitor = new CmdlistVisitor();
			CommandVisitor cmdVisitor = new CommandVisitor();
			List<Command> body = Stream.of(
					ctx.cmdlist().accept(cmdlistVisitor),
					(ctx.cmdoutput() == null ? Collections.<Command>emptyList() : Arrays.asList(ctx.cmdoutput().accept(cmdVisitor)))
				)
				.flatMap(c -> c.stream())
				.collect(Collectors.toList());
			
			FunctionCommand c = new FunctionCommand(name, parameters, body);
			c.setSourceRange(contextToSourceRange(ctx));
			return c;
		}
	}
	
	private static class CommandVisitor extends SCHIMPBaseVisitor<Command> {
		@Override
		public Command visitCmdoutput(SCHIMPParser.CmdoutputContext ctx) {
			AexplistVisitor aexplistVisitor = new AexplistVisitor();
			List<ArithmeticExpression> aexplist = ctx.aexplist().accept(aexplistVisitor);
			
			OutputCommand c = new OutputCommand(aexplist);
			c.setSourceRange(contextToSourceRange(ctx));
			return c;
		}
		
		@Override
		public Command visitCmdnew(SCHIMPParser.CmdnewContext ctx) {
			String variableName = ctx.IDENTIFIER().getText();
			VariableReference variable = new VariableReference(variableName);
			
			ProbabilityMassFunction<ArithmeticExpression> pmfValue;
			if (ctx.aexp() != null) {
				ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
				ArithmeticExpression aexpValue = ctx.aexp().accept(aexpVisitor);
				pmfValue = new ProbabilityMassFunction<ArithmeticExpression>();
				pmfValue.add(aexpValue, "1");
				pmfValue.finalise();
			} else { // if (ctx.pmf() != null)
				ProbabilityMassFunctionVisitor pmfVisitor = new ProbabilityMassFunctionVisitor();
				pmfValue = ctx.pmf().accept(pmfVisitor);
			} // TODO: else throw exception
			
			NewCommand c = new NewCommand(variable, pmfValue);
			c.setSourceRange(contextToSourceRange(ctx));
			return c;
		}
		
		@Override
		public Command visitCmdassign(SCHIMPParser.CmdassignContext ctx) {
			String variableName = ctx.IDENTIFIER().getText();
			VariableReference variable = new VariableReference(variableName);
			
			ProbabilityMassFunction<ArithmeticExpression> pmfValue;
			if (ctx.aexp() != null) {
				ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
				ArithmeticExpression aexpValue = ctx.aexp().accept(aexpVisitor);
				pmfValue = new ProbabilityMassFunction<ArithmeticExpression>();
				pmfValue.add(aexpValue, "1");
				pmfValue.finalise();
			} else { // if (ctx.pmf() != null)
				ProbabilityMassFunctionVisitor pmfVisitor = new ProbabilityMassFunctionVisitor();
				pmfValue = ctx.pmf().accept(pmfVisitor);
			} // TODO: else throw exception
			
			AssignCommand c = new AssignCommand(variable, pmfValue);
			c.setSourceRange(contextToSourceRange(ctx));
			return c;
		}
		
		@Override
		public Command visitCmdinvoke(SCHIMPParser.CmdinvokeContext ctx) {
			// TODO: store source locations
			String functionName = ctx.IDENTIFIER().getText();
			
			AexplistVisitor aexplistVisitor = new AexplistVisitor();
			List<ArithmeticExpression> aexplist = ctx.aexplist() == null ?
				Collections.<ArithmeticExpression>emptyList() :
					ctx.aexplist().accept(aexplistVisitor);
			
			InvokeCommand c = new InvokeCommand(functionName, aexplist);
			c.setSourceRange(contextToSourceRange(ctx));
			return c;
		}
		
		@Override
		public Command visitCmdskip(SCHIMPParser.CmdskipContext ctx) {
			SkipCommand c = new SkipCommand();
			c.setSourceRange(contextToSourceRange(ctx));
			return c;
		}
		
		@Override
		public Command visitCmdif(SCHIMPParser.CmdifContext ctx) {
			BooleanExpressionVisitor bexpVisitor = new BooleanExpressionVisitor();
			CmdlistVisitor cmdlistVisitor = new CmdlistVisitor();
			
			BooleanExpression conditional = ctx.bexp().accept(bexpVisitor);
			Block trueBody = new Block(ctx.cmdlist(0).accept(cmdlistVisitor));
			Block falseBody = ctx.cmdlist().size() == 1 ?
				null :
				new Block(ctx.cmdlist(1).accept(cmdlistVisitor));
			
			IfCommand c = new IfCommand(conditional, trueBody, falseBody);
			c.setSourceRange(contextToSourceRange(ctx));
			return c;
		}
		
		@Override
		public Command visitCmdwhile(SCHIMPParser.CmdwhileContext ctx) {
			BooleanExpressionVisitor bexpVisitor = new BooleanExpressionVisitor();
			CmdlistVisitor cmdlistVisitor = new CmdlistVisitor();
			
			BooleanExpression conditional = ctx.bexp().accept(bexpVisitor);
			Block body = new Block(ctx.cmdlist().accept(cmdlistVisitor));
			
			WhileCommand c = new WhileCommand(conditional, body);
			c.setSourceRange(contextToSourceRange(ctx));
			return c;
		}
	}
	
	private static class ProbabilityMassFunctionVisitor extends SCHIMPBaseVisitor<ProbabilityMassFunction<ArithmeticExpression>> {
		@Override
		public ProbabilityMassFunction<ArithmeticExpression> visitPmf(SCHIMPParser.PmfContext ctx) {
			ProbabilityMassFunction<ArithmeticExpression> pmf = new ProbabilityMassFunction<ArithmeticExpression>();
			
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			List<ArithmeticExpression> aexps = ctx.aexp().stream().map(c -> c.accept(aexpVisitor)).collect(Collectors.toList());
			List<String> probabilities = ctx.NUMBER().stream().map(f -> f.getSymbol().getText()).collect(Collectors.toList());
			
			for (int i = 0; i < aexps.size(); i++) {
				pmf.add(aexps.get(i), probabilities.get(i));
			}
			
			pmf.finalise();
			
			return pmf;
		}
	}
	
	private static class AexplistVisitor extends SCHIMPBaseVisitor<List<ArithmeticExpression>> {
		@Override
		public List<ArithmeticExpression> visitAexplist(SCHIMPParser.AexplistContext ctx) {
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			return ctx.aexp().stream().map(a -> a.accept(aexpVisitor)).collect(Collectors.toList());
		}
	}
	
	private static class VarnamelistVisitor extends SCHIMPBaseVisitor<List<VariableReference>> {
		@Override
		public List<VariableReference> visitVarnamelist(SCHIMPParser.VarnamelistContext ctx) {
			// TODO: set source range for each variable name token
			return ctx.IDENTIFIER().stream().map(v -> new VariableReference(v.getText())).collect(Collectors.toList());
		}
	}
	
	private static class CmdlistVisitor extends SCHIMPBaseVisitor<List<Command>> {
		@Override
		public List<Command> visitCmdlist(SCHIMPParser.CmdlistContext ctx) {
			CommandVisitor cmdVisitor = new CommandVisitor();
			return ctx.cmd().stream().map(c -> c.accept(cmdVisitor)).collect(Collectors.toList());
		}
	}
	
	private static class ArithmeticExpressionVisitor extends SCHIMPBaseVisitor<ArithmeticExpression> {
		@Override
		public ArithmeticExpression visitAexpParens(SCHIMPParser.AexpParensContext ctx) {
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			
			ArithmeticExpression aexp = ctx.aexp().accept(aexpVisitor);
			aexp.setSourceRange(contextToSourceRange(ctx));
			return aexp;
		}
		
		@Override
		public ArithmeticExpression visitAexpMultiply(SCHIMPParser.AexpMultiplyContext ctx) {
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			
			MultiplyOperation op = new MultiplyOperation(ctx.aexp(0).accept(aexpVisitor), ctx.aexp(1).accept(aexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public ArithmeticExpression visitAexpDivide(SCHIMPParser.AexpDivideContext ctx) {
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			
			DivideOperation op = new DivideOperation(ctx.aexp(0).accept(aexpVisitor), ctx.aexp(1).accept(aexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public ArithmeticExpression visitAexpAdd(SCHIMPParser.AexpAddContext ctx) {
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			
			AddOperation op = new AddOperation(ctx.aexp(0).accept(aexpVisitor), ctx.aexp(1).accept(aexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public ArithmeticExpression visitAexpSubtract(SCHIMPParser.AexpSubtractContext ctx) {
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			
			SubtractOperation op = new SubtractOperation(ctx.aexp(0).accept(aexpVisitor), ctx.aexp(1).accept(aexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public ArithmeticExpression visitAexpModulo(SCHIMPParser.AexpModuloContext ctx) {
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			
			ModuloOperation op = new ModuloOperation(ctx.aexp(0).accept(aexpVisitor), ctx.aexp(1).accept(aexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public ArithmeticExpression visitAexpXor(SCHIMPParser.AexpXorContext ctx) {
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			
			XorOperation op = new XorOperation(ctx.aexp(0).accept(aexpVisitor), ctx.aexp(1).accept(aexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public ArithmeticExpression visitAexpConst(SCHIMPParser.AexpConstContext ctx) {
			// TODO: this must be an integer, throw exception if not
			ArithmeticConstant aconst = new ArithmeticConstant(Integer.parseInt(ctx.getText()));
			aconst.setSourceRange(contextToSourceRange(ctx));
			return aconst;
		}
		
		@Override
		public ArithmeticExpression visitAexpVarname(SCHIMPParser.AexpVarnameContext ctx) {
			VariableReference v = new VariableReference(ctx.getText());
			v.setSourceRange(contextToSourceRange(ctx));
			return v;
		}
	}
	
	private static class BooleanExpressionVisitor extends SCHIMPBaseVisitor<BooleanExpression> {
		@Override
		public BooleanExpression visitBexpParens(SCHIMPParser.BexpParensContext ctx) {
			BooleanExpressionVisitor bexpVisitor = new BooleanExpressionVisitor();
			
			BooleanExpression bexp = ctx.bexp().accept(bexpVisitor);
			bexp.setSourceRange(contextToSourceRange(ctx));
			return bexp;
		}
		
		@Override
		public BooleanExpression visitBexpNot(SCHIMPParser.BexpNotContext ctx) {
			BooleanExpressionVisitor bexpVisitor = new BooleanExpressionVisitor();
			
			BooleanNotOperation op = new BooleanNotOperation(ctx.bexp().accept(bexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public BooleanExpression visitBexpAnd(SCHIMPParser.BexpAndContext ctx) {
			BooleanExpressionVisitor bexpVisitor = new BooleanExpressionVisitor();
			
			BooleanAndOperation op = new BooleanAndOperation(ctx.bexp(0).accept(bexpVisitor), ctx.bexp(1).accept(bexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public BooleanExpression visitBexpOr(SCHIMPParser.BexpOrContext ctx) {
			BooleanExpressionVisitor bexpVisitor = new BooleanExpressionVisitor();
			
			BooleanOrOperation op = new BooleanOrOperation(ctx.bexp(0).accept(bexpVisitor), ctx.bexp(1).accept(bexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public BooleanExpression visitBexpEquals(SCHIMPParser.BexpEqualsContext ctx) {
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			
			EqualsOperation op = new EqualsOperation(ctx.aexp(0).accept(aexpVisitor), ctx.aexp(1).accept(aexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public BooleanExpression visitBexpLess(SCHIMPParser.BexpLessContext ctx) {
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			
			LessThanOperation op = new LessThanOperation(ctx.aexp(0).accept(aexpVisitor), ctx.aexp(1).accept(aexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public BooleanExpression visitBexpGreater(SCHIMPParser.BexpGreaterContext ctx) {
			ArithmeticExpressionVisitor aexpVisitor = new ArithmeticExpressionVisitor();
			
			GreaterThanOperation op = new GreaterThanOperation(ctx.aexp(0).accept(aexpVisitor), ctx.aexp(1).accept(aexpVisitor));
			op.setSourceRange(contextToSourceRange(ctx));
			return op;
		}
		
		@Override
		public BooleanExpression visitBexpConst(SCHIMPParser.BexpConstContext ctx) {
			// TODO: this must be a boolean, throw exception if not
			BooleanConstant bconst = new BooleanConstant(Boolean.parseBoolean(ctx.getText()));
			bconst.setSourceRange(contextToSourceRange(ctx));
			return bconst;
		}
	}
	
	public static void main(String[] args) throws IOException, SyntaxException {
		SourceFile source = new SourceFile(new File(args[0]));
		Program p = source.parse();
		System.out.println(p.toString());
		System.out.println(ProgramExecutionContext.initialContext(p));
	}
	
}

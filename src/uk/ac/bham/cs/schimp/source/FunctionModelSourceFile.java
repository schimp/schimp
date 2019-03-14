package uk.ac.bham.cs.schimp.source;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.math3.fraction.BigFraction;
import org.javatuples.Pair;

import uk.ac.bham.cs.schimp.ProbabilityMassFunction;
import uk.ac.bham.cs.schimp.exec.FunctionModel;
import uk.ac.bham.cs.schimp.lang.expression.arith.ArithmeticConstant;
import uk.ac.bham.cs.schimp.parser.SCHIMPFunctionModelBaseVisitor;
import uk.ac.bham.cs.schimp.parser.SCHIMPFunctionModelLexer;
import uk.ac.bham.cs.schimp.parser.SCHIMPFunctionModelParser;

public class FunctionModelSourceFile extends FunctionModelSource {
	
	public File file;
	
	public FunctionModelSourceFile(File file) {
		this.file = file;
	}

	@Override
	public Map<Pair<String, Integer>, FunctionModel> parse() throws IOException, SyntaxException {
		CharStream charStream = CharStreams.fromPath(file.toPath());
		SCHIMPFunctionModelLexer lexer = new SCHIMPFunctionModelLexer(charStream);
		TokenStream tokens = new CommonTokenStream(lexer);
		SCHIMPFunctionModelParser parser = new SCHIMPFunctionModelParser(tokens);
		
		FunctionModelListVisitor modelListVisitor = new FunctionModelListVisitor();
		Map<Pair<String, Integer>, FunctionModel> functionModels = modelListVisitor.visit(parser.functionmodellist());
		// TODO: syntax-check each model
		//functionModels.check(new SyntaxCheckContext());
		
		return functionModels;
	}
	
	private static class FunctionModelListVisitor extends SCHIMPFunctionModelBaseVisitor<Map<Pair<String, Integer>, FunctionModel>> {
		@Override
		public Map<Pair<String, Integer>, FunctionModel> visitFunctionmodellist(SCHIMPFunctionModelParser.FunctionmodellistContext ctx) {
			
			FunctionModelVisitor functionModelVisitor = new FunctionModelVisitor();
			
			
			// TODO: reject duplicate function models for a single function (i.e. when IllegalStateException is thrown
			// below)
			Map<Pair<String, Integer>, FunctionModel> functionModels = ctx.functionmodel().stream()
				.map(c -> c.accept(functionModelVisitor))
				.collect(Collectors.<FunctionModel, Pair<String, Integer>, FunctionModel>toMap(
						fm -> new Pair<>(fm.getName(), fm.getArity()),
						fm -> fm
					));
			
			return functionModels;
		}
	}
	
	private static class FunctionModelVisitor extends SCHIMPFunctionModelBaseVisitor<FunctionModel> {
		@Override
		public FunctionModel visitFunctionmodel(SCHIMPFunctionModelParser.FunctionmodelContext ctx) {
			ArithmeticConstantListMapVisitor aconstlistmapVisitor = new ArithmeticConstantListMapVisitor();
			
			String functionName = ctx.IDENTIFIER().getText();
			int functionArity = Integer.parseInt(ctx.INTEGER().getText());
			FunctionModel model = new FunctionModel(functionName, functionArity);
			
			ctx.aconstlistmap().accept(aconstlistmapVisitor)
				.forEach(c -> model.add(c.getValue0(), c.getValue1()));
			
			return model;
		}
	}
	
	private static class ArithmeticConstantListMapVisitor extends SCHIMPFunctionModelBaseVisitor<List<Pair<List<ArithmeticConstant>, ProbabilityMassFunction<Pair<Integer, Integer>>>>> {
		@Override
		public List<Pair<List<ArithmeticConstant>, ProbabilityMassFunction<Pair<Integer, Integer>>>> visitAconstlistmap(SCHIMPFunctionModelParser.AconstlistmapContext ctx) {
			ArithmeticConstantListVisitor aconstlistVisitor = new ArithmeticConstantListVisitor();
			TimePowerTupleExpressionVisitor tptupleexpVisitor = new TimePowerTupleExpressionVisitor();
			
			List<Pair<List<ArithmeticConstant>, ProbabilityMassFunction<Pair<Integer, Integer>>>> aconstlistmap = new LinkedList<>();
			
			List<List<ArithmeticConstant>> aconsts = ctx.aconstlist().stream()
				.map(c -> c.accept(aconstlistVisitor))
				.collect(Collectors.toList());
			List<ProbabilityMassFunction<Pair<Integer, Integer>>> tptupleexps = ctx.tptupleexp().stream()
					.map(c -> c.accept(tptupleexpVisitor))
					.collect(Collectors.toList());
			
			for (int i = 0; i < aconsts.size(); i++) {
				aconstlistmap.add(new Pair<>(aconsts.get(i), tptupleexps.get(i)));
			}
			
			return aconstlistmap;
		}
	}
	
	private static class ArithmeticConstantListVisitor extends SCHIMPFunctionModelBaseVisitor<List<ArithmeticConstant>> {
		@Override
		public List<ArithmeticConstant> visitAconstlist(SCHIMPFunctionModelParser.AconstlistContext ctx) {
			ArithmeticConstantVisitor aconstVisitor = new ArithmeticConstantVisitor();
			return ctx.aconst().stream()
				.map(c -> c.accept(aconstVisitor))
				.collect(Collectors.toList());
		}
	}
	
	private static class TimePowerTupleExpressionVisitor extends SCHIMPFunctionModelBaseVisitor<ProbabilityMassFunction<Pair<Integer, Integer>>> {
		public ProbabilityMassFunction<Pair<Integer, Integer>> visitTptupleexp(SCHIMPFunctionModelParser.TptupleexpContext ctx) {
			TimePowerTupleVisitor tptupleVisitor = new TimePowerTupleVisitor();
			ProbabilityMassFunctionVisitor pmfVisitor = new ProbabilityMassFunctionVisitor();
			
			if (ctx.tptuple() != null) {
				Pair<Integer, Integer> tptuple = ctx.tptuple().accept(tptupleVisitor);
				
				ProbabilityMassFunction<Pair<Integer, Integer>> pmf = new ProbabilityMassFunction<>();
				pmf.add(tptuple, 1);
				
				return pmf;
			} else { // if (ctx.pmf() != null)
				return ctx.pmf().accept(pmfVisitor);
			} // TODO: else throw exception
		}
	}
	
	private static class ProbabilityMassFunctionVisitor extends SCHIMPFunctionModelBaseVisitor<ProbabilityMassFunction<Pair<Integer, Integer>>> {
		@Override
		public ProbabilityMassFunction<Pair<Integer, Integer>> visitPmf(SCHIMPFunctionModelParser.PmfContext ctx) {
			ProbabilityMassFunction<Pair<Integer, Integer>> pmf = new ProbabilityMassFunction<>();
			
			TimePowerTupleVisitor tptupleVisitor = new TimePowerTupleVisitor();
			List<Pair<Integer, Integer>> tptuples = ctx.tptuple().stream()
				.map(c -> c.accept(tptupleVisitor))
				.collect(Collectors.toList());
			
			RationalVisitor rationalVisitor = new RationalVisitor();
			List<BigFraction> rationals = ctx.rational().stream()
				.map(c -> c.accept(rationalVisitor))
				.collect(Collectors.toList());
			
			for (int i = 0; i < tptuples.size(); i++) {
				pmf.add(tptuples.get(i), rationals.get(i));
			}
			
			return pmf;
		}
	}
	
	private static class TimePowerTupleVisitor extends SCHIMPFunctionModelBaseVisitor<Pair<Integer, Integer>> {
		@Override
		public Pair<Integer, Integer> visitTptuple(SCHIMPFunctionModelParser.TptupleContext ctx) {
			return new Pair<>(
				Integer.parseInt(ctx.INTEGER(0).getText()),
				Integer.parseInt(ctx.INTEGER(1).getText())
			);
		}
	}
	
	private static class ArithmeticConstantVisitor extends SCHIMPFunctionModelBaseVisitor<ArithmeticConstant> {
		@Override
		public ArithmeticConstant visitAconstRational(SCHIMPFunctionModelParser.AconstRationalContext ctx) {
			RationalVisitor rationalVisitor = new RationalVisitor();
			return new ArithmeticConstant(ctx.rational().accept(rationalVisitor));
		}
		
		@Override
		public ArithmeticConstant visitAconstAny(SCHIMPFunctionModelParser.AconstAnyContext ctx) {
			return null;
		}
	}
	
	private static class RationalVisitor extends SCHIMPFunctionModelBaseVisitor<BigFraction> {
		@Override
		public BigFraction visitRationalParens(SCHIMPFunctionModelParser.RationalParensContext ctx) {
			RationalVisitor rationalVisitor = new RationalVisitor();
			
			BigFraction aconst = ctx.rational().accept(rationalVisitor);
			return aconst;
		}
		
		@Override
		public BigFraction visitRationalMultiply(SCHIMPFunctionModelParser.RationalMultiplyContext ctx) {
			RationalVisitor rationalVisitor = new RationalVisitor();
			
			BigFraction left = ctx.rational(0).accept(rationalVisitor);
			BigFraction right = ctx.rational(1).accept(rationalVisitor);
			
			return left.multiply(right);
		}
		
		@Override
		public BigFraction visitRationalDivide(SCHIMPFunctionModelParser.RationalDivideContext ctx) {
			RationalVisitor rationalVisitor = new RationalVisitor();
			
			BigFraction left = ctx.rational(0).accept(rationalVisitor);
			BigFraction right = ctx.rational(1).accept(rationalVisitor);
			
			return left.divide(right);
		}
		
		@Override
		public BigFraction visitRationalAdd(SCHIMPFunctionModelParser.RationalAddContext ctx) {
			RationalVisitor rationalVisitor = new RationalVisitor();
			
			BigFraction left = ctx.rational(0).accept(rationalVisitor);
			BigFraction right = ctx.rational(1).accept(rationalVisitor);
			
			return left.add(right);
		}
		
		@Override
		public BigFraction visitRationalSubtract(SCHIMPFunctionModelParser.RationalSubtractContext ctx) {
			RationalVisitor rationalVisitor = new RationalVisitor();
			
			BigFraction left = ctx.rational(0).accept(rationalVisitor);
			BigFraction right = ctx.rational(1).accept(rationalVisitor);
			
			return left.subtract(right);
		}
		
		@Override
		public BigFraction visitRationalInteger(SCHIMPFunctionModelParser.RationalIntegerContext ctx) {
			// TODO: this must be an integer, throw exception if not
			return new BigFraction(Integer.parseInt(ctx.getText()));
		}
	}
	
	public static void main(String[] args) throws IOException, SyntaxException {
		FunctionModelSourceFile source = new FunctionModelSourceFile(new File(args[0]));
		Map<Pair<String, Integer>, FunctionModel> models = source.parse();
		models.forEach((sig, fm) -> System.out.println(sig.getValue0() + "/" + sig.getValue1() + ":\n" + fm.toSourceString()));
	}

}

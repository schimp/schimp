package uk.ac.bham.cs.schimp.exec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;

import explicit.graphviz.Decorator;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.ValueConverter;
import prism.Prism;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import prism.PrismSettings;
import prism.Result;
import uk.ac.bham.cs.schimp.exec.graphviz.AttackerModelStateDecorator;
import uk.ac.bham.cs.schimp.exec.graphviz.SCHIMPModelStateDecorator;
import uk.ac.bham.cs.schimp.lang.Program;
import uk.ac.bham.cs.schimp.source.FunctionModelSourceFile;
import uk.ac.bham.cs.schimp.source.SourceFile;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class CommandLine {
	
	static {
		// suppress awt guis created by prism
		System.setProperty("java.awt.headless", "true");
	}
	
	public static void main(String[] args) {
		// parse and validate the command line options we were given
		OptionSet options = null;
		try {
			options = parseOptions(args);
			
			// just print the help or version text if it was specifically requested
			/*
			if (options.has("help")) {
				printHelp();
				System.exit(0);
			} else if (options.has("version")) {
				printVersion();
				System.exit(0);
			}
			*/
			
			// otherwise, one command line argument is required (the schimp source code path): make sure we have it
			if (options.nonOptionArguments().size() != 1) throw new MissingArgumentException();
		} catch (OptionException e) {
			//printHelp();
			System.exit(1);
		} catch (MissingArgumentException e) {
			System.err.println("Usage: schimp.jar [OPTION].. [FILE]");
			System.err.println("Try `schimp.jar --help' for more information.");
			System.exit(1);
		} catch (OptionValueException e) {
			System.err.println("schimp.jar: " + e.getMessage());
			System.err.println("Try `schimp.jar --help' for more information.");
			System.exit(1);
		}
		
		File sourceArg = (File)options.nonOptionArguments().get(0);
		Program program = null;
		try {
			SourceFile source = new SourceFile(sourceArg);
			program = options.has("fnmodel-file") ?
				source.parse(((FunctionModelSourceFile)options.valueOf("fnmodel-file")).parse()) :
				source.parse(null);
		} catch (IOException | SyntaxException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println(program.toString());
		
		List<String> requestedInitialVariables = options.valuesOf("initial-vars").stream()
			.map(v -> v.toString())
			.collect(Collectors.toList());
		List<String> trackedInitialVariables = options.hasArgument("initial-vars") ?
			ListUtils.intersection(program.getInitialVariableNames(), requestedInitialVariables) :
			program.getInitialVariableNames();
		if (options.hasArgument("initial-vars")) {
			List<String> missingInitialVariables = ListUtils.subtract(requestedInitialVariables, trackedInitialVariables);
			if (missingInitialVariables.size() != 0) {
				System.err.println(
					options.nonOptionArguments().get(0) +
					": program does not define initial variable " +
					(missingInitialVariables.size() == 1 ? "" : "s") +
					missingInitialVariables.stream()
						.map(v -> "'" + v + "'")
						.collect(Collectors.joining(", "))
				);
				System.exit(1);
			}
		}
		
		PrismLog prismStdout = new PrismFileLog("stdout");
		
		Prism prismSchimpExecution = new Prism(prismStdout);
		try {
			prismSchimpExecution.initialise();
			prismSchimpExecution.setEngine(Prism.EXPLICIT);
			prismSchimpExecution.getSettings().set(PrismSettings.PRISM_SORT_STATES, false);
		} catch (PrismException e) {
			e.printStackTrace();
			System.exit(1);
		}
			
		PRISMModelGenerator schimpModelGenerator = new PRISMModelGenerator(
			program,
			options.has("time-var"),
			options.has("power-var"),
			trackedInitialVariables,
			!options.has("show-all-transitions")
		);
		prismSchimpExecution.loadModelGenerator(schimpModelGenerator);
		try {
			prismSchimpExecution.buildModelIfRequired();
		} catch (PrismException e) {
			e.printStackTrace();
			System.exit(1);
		}
			
		Prism prismAttackerGuesses = new Prism(prismStdout);
		try {
			prismAttackerGuesses.initialise();
			prismAttackerGuesses.setEngine(Prism.EXPLICIT);
			prismAttackerGuesses.getSettings().set(PrismSettings.PRISM_SORT_STATES, false);
			prismAttackerGuesses.getSettings().set(PrismSettings.PRISM_GRID_RESOLUTION, options.valueOf("grid-resolution"));
			//prismAttackerGuesses.getSettings().set(PrismSettings.PRISM_EXPORT_ADV, "MDP");
			//prismAttackerGuesses.getSettings().set(PrismSettings.PRISM_EXPORT_ADV_FILENAME, "examples/adv.tra");
		} catch (PrismException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		AttackerModelGenerator attackerModelGenerator = AttackerModelGenerator.fromSCHIMPModel(
			prismSchimpExecution,
			schimpModelGenerator
		);
		prismAttackerGuesses.loadModelGenerator(attackerModelGenerator);
		try {
			prismAttackerGuesses.buildModelIfRequired();
		} catch (PrismException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		if (options.has("dot-file")) {
			List<String> dotFilePaths = options.hasArgument("dot-file") ?
				options.valuesOf("dot-file").stream().map(f -> ((File)f).getPath()).collect(Collectors.toList()) :
				Stream.of(new String[] { ".exec.dot", ".attacker.dot" }).map(f -> sourceArg.getPath() + f).collect(Collectors.toList());
				
			ArrayList<Decorator> schimpExecutionDecorators = new ArrayList<>();
			schimpExecutionDecorators.add(new SCHIMPModelStateDecorator(prismSchimpExecution.getBuiltModelExplicit().getStatesList(), schimpModelGenerator, options.has("show-outputs")));
			PrismLog schimpExecutionDotFile = new PrismFileLog(dotFilePaths.get(0));
			prismSchimpExecution.getBuiltModelExplicit().exportToDotFile(schimpExecutionDotFile, schimpExecutionDecorators);
			schimpExecutionDotFile.flush();
			
			ArrayList<Decorator> attackerGuessesDecorators = new ArrayList<>();
			attackerGuessesDecorators.add(new AttackerModelStateDecorator(prismAttackerGuesses.getBuiltModelExplicit().getStatesList(), attackerModelGenerator, options.has("show-outputs")));
			PrismLog attackerGuessesDotFile = new PrismFileLog(dotFilePaths.get(1));
			prismAttackerGuesses.getBuiltModelExplicit().exportToDotFile(attackerGuessesDotFile, attackerGuessesDecorators);
			attackerGuessesDotFile.flush();
		}
		
		try {
			Result result = prismAttackerGuesses.modelCheck(attackerModelGenerator.getPmaxProperty());
			if (result.getStrategy() != null) {
				result.getStrategy().exportActions(prismStdout);
			} else {
				System.out.println("No attacker strategy provided by PRISM");
			}
		} catch (PrismException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		/*
		try {
			BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));
			String in;
			System.out.print( "check> " );
			while ((in = stdinReader.readLine()) != null) {
				try {
					Result result = prismAttackerGuesses.modelCheck(in);
					if (result.getStrategy() != null) {
						result.getStrategy().exportActions(prismStdout);
					} else {
						System.out.println("No attacker strategy provided by PRISM");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.print( "check> " );
			}
		} catch (IOException e) {
			System.exit(0);
		}
		*/
	}
	
	public static OptionParser getOptionParser() {
		OptionParser parser = new OptionParser();
		
		parser.nonOptions().ofType(File.class);
		
		parser.acceptsAll(Arrays.asList("f", "fnmodel-file")).withRequiredArg().withValuesConvertedBy(new ValueConverter<FunctionModelSourceFile>() {

			@Override
			public FunctionModelSourceFile convert(String arg) {
				return new FunctionModelSourceFile(new File(arg));
			}

			@Override
			public String valuePattern() {
				return null;
			}

			@Override
			public Class<FunctionModelSourceFile> valueType() {
				return FunctionModelSourceFile.class;
			}
			
		});
		
		parser.acceptsAll(Arrays.asList("d", "dot-file")).withOptionalArg().ofType(File.class).withValuesSeparatedBy(':');
		
		parser.acceptsAll(Arrays.asList("T", "time-var"));
		
		parser.acceptsAll(Arrays.asList("P", "power-var"));
		
		parser.acceptsAll(Arrays.asList("I", "initial-vars")).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		
		parser.acceptsAll(Arrays.asList("o", "show-outputs"));
		
		parser.acceptsAll(Arrays.asList("a", "show-all-transitions"));
		
		parser.acceptsAll(Arrays.asList("g", "grid-resolution")).withRequiredArg().ofType(Integer.class).defaultsTo(8);
		
		// --help (optional): show program help and exit
		parser.accepts("help");
		
		// --version (optional): show program version and exit
		parser.accepts("version");
		
		return parser;
	}
	
	public static OptionSet parseOptions(String[] args) throws OptionException, OptionValueException {
		// parse() throws an OptionException if an invalid option was supplied, an argument of the wrong type was
		// supplied for a valid option, or required options were missing
		OptionSet options = getOptionParser().parse(args);
		
		return options;
	}

}

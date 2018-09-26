package uk.ac.bham.cs.schimp.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import uk.ac.bham.cs.schimp.exec.graphviz.StateDecorator;
import uk.ac.bham.cs.schimp.lang.Program;
import uk.ac.bham.cs.schimp.source.FunctionModelSourceFile;
import uk.ac.bham.cs.schimp.source.SourceFile;
import uk.ac.bham.cs.schimp.source.SyntaxException;

public class CommandLine {
	
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
		
		Program program = null;
		try {
			SourceFile source = new SourceFile((File)options.nonOptionArguments().get(0));
			program = options.has("fnmodel-file") ?
				source.parse(((FunctionModelSourceFile)options.valueOf("fnmodel-file")).parse()) :
				source.parse(null);
		} catch (IOException | SyntaxException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println(program.toString());
		
		// TODO: parse -I
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
		Prism prism = new Prism(prismStdout);
		try {
			prism.initialise();
			prism.setEngine(Prism.EXPLICIT);
		} catch (PrismException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		PRISMModelGenerator modelGenerator = new PRISMModelGenerator(
			program,
			options.has("time-var"),
			options.has("power-var"),
			trackedInitialVariables,
			!options.has("all-transitions")
		);
		prism.loadModelGenerator(modelGenerator);
		try {
			prism.buildModelIfRequired();
		} catch (PrismException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		if (options.has("dot-file")) {
			String dotFilePath = options.hasArgument("dot-file") ?
				((File)options.valueOf("dot-file")).getPath() :
				((File)options.nonOptionArguments().get(0)).getPath() + ".dot";
			ArrayList<Decorator> decorators = new ArrayList<>();
			decorators.add(new StateDecorator(prism.getBuiltModelExplicit().getStatesList(), modelGenerator));
			PrismLog dotFile = new PrismFileLog(dotFilePath);
			prism.getBuiltModelExplicit().exportToDotFile(dotFile, decorators);
			dotFile.flush();
		}
		
		try {
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
			System.exit(0);
		}
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
		
		parser.acceptsAll(Arrays.asList("d", "dot-file")).withOptionalArg().ofType(File.class);
		
		parser.acceptsAll(Arrays.asList("T", "time-var"));
		
		parser.acceptsAll(Arrays.asList("P", "power-var"));
		
		parser.acceptsAll(Arrays.asList("I", "initial-vars")).withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		
		parser.acceptsAll(Arrays.asList("a", "all-transitions"));
		
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

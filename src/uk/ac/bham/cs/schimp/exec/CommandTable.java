package uk.ac.bham.cs.schimp.exec;

import java.util.HashMap;
import java.util.Map;

import uk.ac.bham.cs.schimp.lang.command.Command;

public class CommandTable {
	
	private Map<Integer, Command> commands = new HashMap<Integer, Command>();
	private int lastCommandID = 0;
	
	public CommandTable() {
		
	}
	
	public int addCommand(Command command) {
		if (commands.containsValue(command)) {
			return commands.entrySet().stream()
				.filter(e -> e.getValue() == command)
				.findFirst()
				.get()
				.getKey();
		} else {
			commands.put(++lastCommandID, command);
			return lastCommandID;
		}
	}
	
	public String toString() {
		return "CommandTable (lastCommandID=" + lastCommandID + ") {\n" +
			commands.keySet().stream()
				.sorted()
				.map(i -> "  " + i + " -> " + commands.get(i).toSourceString() + "\n") +
			"}";
	}

}

package adris.altoclef.commandsystem;


import adris.altoclef.AltoClef;

import java.security.InvalidKeyException;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;


public class CommandExecutor {
    private final HashMap<String, Command> commandSheet = new HashMap<>();
    private final AltoClef mod;
    private final String commandPrefix;
    
    public CommandExecutor(AltoClef mod, String commandPrefix) {
        this.mod = mod;
        this.commandPrefix = commandPrefix;
    }
    
    public void RegisterNewCommand(Command command) throws InvalidKeyException {
        if (commandSheet.containsKey(command.getName())) {
            throw new InvalidKeyException("Command with name " + command.getName() + " already exists! Can't register that name twice.");
        }
        commandSheet.put(command.getName(), command);
    }
    
    public boolean isClientCommand(String line) {
        return line.startsWith(commandPrefix);
    }
    
    public void Execute(String line, Consumer onFinish) throws CommandException {
        if (!isClientCommand(line)) return;
        line = line.substring(commandPrefix.length());
        Command c = GetCommand(line);
        if (c != null) {
            try {
                c.Run(mod, line, onFinish);
            } catch (CommandException ae) {
                throw new CommandException(ae.getMessage() + "\nUsage: " + c.GetHelpRepresentation(), ae);
            }
        }
    }
    
    public void Execute(String line) throws CommandException {
        Execute(line, null);
    }
    
    private Command GetCommand(String line) throws CommandException {
        
        if (line.length() != 0) {
            String command = line;
            int firstSpace = line.indexOf(' ');
            if (firstSpace != -1) {
                command = line.substring(0, firstSpace);
            }
            
            if (!commandSheet.containsKey(command)) {
                throw new CommandException("Command " + command + " does not exist.");
            }
            
            return commandSheet.get(command);
        }
        return null;
        
    }
    
    public Collection<Command> AllCommands() {
        return commandSheet.values();
    }
    
    public Command Get(String name) {
        return (commandSheet.getOrDefault(name, null));
    }
}

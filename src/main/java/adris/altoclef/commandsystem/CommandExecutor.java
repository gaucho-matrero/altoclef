package adris.altoclef.commandsystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

import java.security.InvalidKeyException;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;

public class CommandExecutor {

    private final HashMap<String, Command> _commandSheet = new HashMap<>();
    private final AltoClef _mod;
    private final String _commandPrefix;

    public CommandExecutor(AltoClef mod, String commandPrefix) {
        _mod = mod;
        _commandPrefix = commandPrefix;
    }

    public void RegisterNewCommand(Command ...commands) {
        for (Command command : commands) {
            if (_commandSheet.containsKey(command.getName())) {
                Debug.logInternal("Command with name " + command.getName() + " already exists! Can't register that name twice.");
                continue;
            }
            _commandSheet.put(command.getName(), command);
        }
    }

    public boolean isClientCommand(String line) {
        return line.startsWith(_commandPrefix);
    }

    public void Execute(String line, Consumer onFinish) throws CommandException {
        if (!isClientCommand(line)) return;
        line = line.substring(_commandPrefix.length());
        Command c = GetCommand(line);
        if (c != null) {
            try {
                c.Run(_mod, line, onFinish);
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

            if (!_commandSheet.containsKey(command)) {
                throw new CommandException("Command " + command + " does not exist.");
            }

            return _commandSheet.get(command);
        }
        return null;

    }

    public Collection<Command> AllCommands() {
        return _commandSheet.values();
    }

    public Command Get(String name) {
        return (_commandSheet.getOrDefault(name, null));
    }
}

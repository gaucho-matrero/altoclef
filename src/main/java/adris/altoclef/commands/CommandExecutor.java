package adris.altoclef.commands;

import adris.altoclef.AltoClef;

import java.security.InvalidKeyException;
import java.util.Collection;
import java.util.HashMap;

public class CommandExecutor {

    private AltoClef _mod;

    private String _commandPrefix;

    private final HashMap<String, Command> _commandSheet = new HashMap<>();

    public CommandExecutor(AltoClef mod, String commandPrefix) {
        _mod = mod;
        _commandPrefix = commandPrefix;
    }

    public void RegisterNewCommand(Command command) throws InvalidKeyException {
        if (_commandSheet.containsKey(command.getName())) {
            throw new InvalidKeyException("Command with name " + command.getName() + " already exists! Can't register that name twice.");
        }
        _commandSheet.put(command.getName(), command);
    }

    public boolean isClientCommand(String line) {
        return line.startsWith(_commandPrefix);
    }

    public void Execute(String line) throws Exception {
        if (!isClientCommand(line)) return;
        line = line.substring(_commandPrefix.length());
        Command c = GetCommand(line);
        if (c != null)
        {
            try
            {
                c.Run(_mod, line);
            }
            catch (Exception ae)
            {
                throw new Exception(ae.getMessage() + "\nUsage: " + c.GetHelpRepresentation(), ae);
            }
        }
    }
    private Command GetCommand(String line) throws Exception {

        if (line.length() != 0)
        {
            String command = line;
            int firstSpace = line.indexOf(' ');
            if (firstSpace != -1)
            {
                command = line.substring(0, firstSpace);
            }

            if (!_commandSheet.containsKey(command))
            {
                throw new Exception("Command " + command + " does not exist.");
            }

            return _commandSheet.get(command);
        }
        return null;

    }

    public Collection<Command> AllCommands()
    {
        return _commandSheet.values();
    }

    public Command Get(String name)
    {
        return (_commandSheet.getOrDefault(name, null));
    }
}

package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

/// This structure was copied from a C# project. Fuck java. All my homies hate java.
public abstract class Command {

    private AltoClef _mod;

    private ArgParser parser;

    private String _name;
    private String _description;

    public Command(String name, String description, ArgBase ...args) {
        _name = name;
        _description = description;
        parser = new ArgParser(args);
    }

    public void Run(AltoClef mod, String line) throws CommandException {
        _mod = mod;
        parser.LoadArgs(line);
        Call(mod, parser);
    }

    public String GetHelpRepresentation()
    {
        StringBuilder sb = new StringBuilder(_name);
        for (ArgBase arg : parser.getArgs()) {
            sb.append(" ");
            sb.append(arg.GetHelpRepresentation());
        }
        return sb.toString();
    }

    protected void Log(Object message)
    {
        Debug.logMessage(message.toString());
    }

    protected void LogError(Object message)
    {
        Debug.logError(message.toString());
    }

    protected abstract void Call(AltoClef mod, ArgParser parser) throws CommandException;

    public String getName() {
        return _name;
    }

    public String getDescription() { return _description; }
}

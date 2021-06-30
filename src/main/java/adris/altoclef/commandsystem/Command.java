package adris.altoclef.commandsystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

import java.util.function.Consumer;

/// This structure was copied from a C# project. Fuck java. All my homies hate java.
public abstract class Command {

    private AltoClef _mod;

    private final ArgParser parser;

    private final String _name;
    private final String _description;

    private Consumer _onFinish = null;

    public Command(String name, String description, ArgBase... args) {
        _name = name;
        _description = description;
        parser = new ArgParser(args);
    }

    public void Run(AltoClef mod, String line, Consumer onFinish) throws CommandException {
        _onFinish = onFinish;
        _mod = mod;
        parser.LoadArgs(line, true);
        Call(mod, parser);
    }

    protected void finish() {
        if (_onFinish != null)
            //noinspection unchecked
            _onFinish.accept(null);
        _onFinish = null;
    }

    public String GetHelpRepresentation() {
        StringBuilder sb = new StringBuilder(_name);
        for (ArgBase arg : parser.getArgs()) {
            sb.append(" ");
            sb.append(arg.GetHelpRepresentation());
        }
        return sb.toString();
    }

    protected void Log(Object message) {
        Debug.logMessage(message.toString());
    }

    protected void LogError(Object message) {
        Debug.logError(message.toString());
    }

    protected abstract void Call(AltoClef mod, ArgParser parser) throws CommandException;

    public String getName() {
        return _name;
    }

    public String getDescription() {
        return _description;
    }
}

package adris.altoclef.commandsystem;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

import java.util.function.Consumer;


/// This structure was copied from a C# project. Fuck java. All my homies hate java.
public abstract class Command {
    private final ArgParser parser;
    private final String name;
    private final String description;
    private AltoClef mod;
    private Consumer onFinish;

    public Command(String name, String description, ArgBase... args) {
        this.name = name;
        this.description = description;
        parser = new ArgParser(args);
    }

    public void Run(AltoClef mod, String line, Consumer onFinish) throws CommandException {
        this.onFinish = onFinish;
        this.mod = mod;
        parser.LoadArgs(line);
        Call(mod, parser);
    }

    protected void finish() {
        if (onFinish != null)
        //noinspection unchecked
        {
            onFinish.accept(null);
        }
        onFinish = null;
    }

    public String GetHelpRepresentation() {
        StringBuilder sb = new StringBuilder(name);
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
        return name;
    }

    public String getDescription() {
        return description;
    }
}

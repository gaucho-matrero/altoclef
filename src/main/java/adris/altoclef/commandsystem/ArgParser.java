package adris.altoclef.commandsystem;


import java.util.ArrayList;
import java.util.List;


/// This structure was copied from a C# project. Fuck java. All my homies hate java.
public class ArgParser {
    private final ArgBase[] args;
    private int argCounter;
    private int unitCounter;
    private String[] argUnits;

    public ArgParser(ArgBase... args) {
        this.args = args;
        setArgCounter(0);
        setUnitCounter(0);
    }

    // Given a single line as a String, parse it into a list of keywords
    public static List<String> SplitLineIntoKeywords(String line) {
        List<String> result = new ArrayList<String>();
        // By default, it's just spaces. But sometimes we want to count quotes. So do it manually.
        String last_kword = "";
        boolean open_quote = false;
        char prev_char = '\0';
        for (char c : line.toCharArray()) {
            // We found a quote, update our "quote" state.
            if (c == '\"') {
                open_quote = !open_quote;
            }
            if (prev_char == '\\') {
                if (c == '#' || c == '"') {
                    // We escaped this pound sign, so ignore the escaping backslash
                    last_kword = last_kword.substring(0, last_kword.length() - 1);
                }
            } else {
                if (c == '#') {
                    // Bail! Everything beyond this is part of a comment, so ignore.
                    break;
                }
            }
            if (c == ' ' && !open_quote) {
                // If it's empty, just ignore.
                if (!last_kword.isEmpty()) {
                    // Remove trailing whitespace
                    result.add(last_kword.trim());
                }
                last_kword = "";
            } else {
                // We don't care about speed here.
                //noinspection StringConcatenationInLoop
                last_kword += c;
            }
            prev_char = c;
        }
        // Add the remainder
        if (!last_kword.isEmpty()) {
            result.add(last_kword.trim());
        }
        return result;
    }

    public void LoadArgs(String line) {
        List<String> units = SplitLineIntoKeywords(line);
        // Discard the first element since, well, it will always be the name of the command.
        if (!units.isEmpty()) {
            units.remove(0);
        }
        setArgUnits(new String[units.size()]);
        units.toArray(getArgUnits());
        setArgCounter(0);
        setUnitCounter(0);
    }

    // Get the next argument.
    public <T> T Get(Class<T> type) throws CommandException {

        if (argCounter >= args.length) {
            throw new CommandException("You tried grabbing more arguments than you had... Bad move.");
        }
        if (getArgUnits().length > args.length) {
            throw new CommandException(
                    String.format("Too many arguments provided %d. The maximum is %d.", getArgUnits().length, args.length));
        }

        // Current values from arrays
        ArgBase arg = args[argCounter];
        setArgCounter(argCounter + 1);
        if (arg.isArray()) {
            setArgCounter(args.length);
        }

        // If this can be default and we don't have enough (unit) args provided to use this arg, use the default value instead of reading
        // from our arg list.
        int givenArgs = getArgUnits().length;
        if (arg.hasDefault() && arg.getMinArgCountToUseDefault() >= givenArgs) {
            return arg.GetDefault(type);
        }

        if (unitCounter >= getArgUnits().length) {
            throw new CommandException(String.format("Not enough arguments supplied: You supplied %d.", getArgUnits().length));
        }

        String unit = getArgUnits()[unitCounter];
        String[] unitPlusRemaining = new String[getArgUnits().length - unitCounter];
        System.arraycopy(getArgUnits(), unitCounter, unitPlusRemaining, 0, unitPlusRemaining.length);
        //Array.Copy(argUnits, unitCounter, unitPlusRemaining, 0, unitPlusRemaining.Length);
        //argUnits.CopyTo(unitPlusRemaining, unitCounter);

        setUnitCounter(unitCounter + 1);

        // If our type is not valid, try um handling the defaults.

        return arg.ParseUnit(unit, unitPlusRemaining);
    }

    public ArgBase[] getArgs() {
        return args;
    }

    public int getArgCounter() {
        return argCounter;
    }

    public ArgParser setArgCounter(int argCounter) {
        this.argCounter = argCounter;
        return this;
    }

    public int getUnitCounter() {
        return unitCounter;
    }

    public ArgParser setUnitCounter(int unitCounter) {
        this.unitCounter = unitCounter;
        return this;
    }

    public String[] getArgUnits() {
        return argUnits;
    }

    public ArgParser setArgUnits(String[] argUnits) {
        this.argUnits = argUnits;
        return this;
    }
}

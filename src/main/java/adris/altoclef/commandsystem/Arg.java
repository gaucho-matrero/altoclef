package adris.altoclef.commandsystem;

import java.util.HashMap;

/// This structure was copied from a C# project. Fuck java. All my homies hate java.
public class Arg<T> extends ArgBase {
    public T Default;
    private final Class<T> _tType;
    private boolean _isArray = false;
    private HashMap<String, T> _enumValues = null;
    private String _name = "";
    private boolean _showDefault;

    // Regular Constructor
    public Arg(Class<T> type, String name) throws CommandException {
        _name = name;
        // I really hate java
        _tType = type;

        _showDefault = true;
        _hasDefault = false;
        // If enum, take action.
        if (_tType.isEnum()) {
            _enumValues = new HashMap<String, T>();
            for (T v : _tType.getEnumConstants()) {
                _enumValues.put(v.toString().toLowerCase(), v);
            }
        } else {
            // Make sure as an extra precaution that we only use (non enum) types we can handle
            if (!IsInstancesOf(_tType, String.class, Float.class, Integer.class, Double.class, Long.class)) {
                throw new CommandException("Arguments are not programmed to parse the following type: {typeof(T)}. This is either not implemented intentionally or by accident somehow.");
            }
        }
    }

    // Constructor with default value
    public Arg(Class<T> type, String name, T defaultValue, int minArgCountToUseDefault, boolean showDefault) throws CommandException {
        this(type, name);
        _hasDefault = true;
        Default = defaultValue;
        _minArgCountToUseDefault = minArgCountToUseDefault;
        _showDefault = showDefault;
    }

    public Arg(Class<T> type, String name, T defaultValue, int minArgCountToUseDefault) throws CommandException {
        this(type, name, defaultValue, minArgCountToUseDefault, true);
    }

    // This is important cause if it is, it will stop parsing further variables and end here as it is a params.
    @Override
    public boolean isArray() {
        return _isArray;
    }

    private boolean isEnum() {
        return _enumValues != null;
    }

    // Horrendous chain syntax that I'm only using here.
    public Arg<T> AsArray() {
        _isArray = true;
        return this;
    }

    /// <summary>
///     Return the "help" command representation of this argument.
///     For instance, in a "dialogue" command it looks like this:
///         dialogue <name = ""> [text]
///     name is optional and defaults to "", while text is non optional.
/// </summary>
    @Override
    public String GetHelpRepresentation() {
        if (hasDefault()) {
            if (_showDefault) {
                return "<" + _name + "=" + Default + ">";
            }
            return "<" + _name + ">";
        }
        return "[" + _name + "]";
    }

    @SuppressWarnings("unchecked")
    private <V> boolean IsInstanceOf(Class<V> vType, Class<?> t) {
        return vType == t || vType.isAssignableFrom(t);
    }

    private <V> boolean IsInstancesOf(Class<V> vType, Class<?>... types) {
        // I really hate java

        for (Class<?> t : types) {
            if (IsInstanceOf(vType, t)) {
                return true;
            }
        }
        return false;
    }

    private void ParseErrorCheck(boolean good, Object value, String type) throws CommandException {
        if (!good)
            throw new CommandException("Failed to parse the following argument into type " + type + ": " + value + ".");
    }


    private <V> V ParseUnitUtil(Class<V> vType, String unit, String[] unitPlusRemainder) throws CommandException {
        // If enum, check from our cached enum dictionary.
        if (isEnum()) {
            unit = unit.toLowerCase();
            if (!_enumValues.containsKey(unit)) {
                StringBuilder res = new StringBuilder();
                for (String type : _enumValues.keySet()) {
                    res.append(type);
                    res.append("|");
                }
                res.delete(res.length() - 1, res.length()); // Remove the last "|"
                throw new CommandException("Invalid argument found: " + unit + ". Accepted values are: " + res);
            }
            return GetConverted(vType, _enumValues.get(unit));
        }

        // Do number parsing.
        if (IsInstanceOf(vType, Float.class)) {
            try {
                return GetConverted(vType, Float.parseFloat(unit));
            } catch (NumberFormatException e) {
                ParseErrorCheck(false, unit, "float");
            }
        }
        if (IsInstanceOf(vType, Double.class)) {
            try {
                return GetConverted(vType, Double.parseDouble(unit));
            } catch (NumberFormatException e) {
                ParseErrorCheck(false, unit, "double");
            }
        }
        if (IsInstanceOf(vType, Integer.class)) {
            try {
                return GetConverted(vType, Integer.parseInt(unit));
            } catch (NumberFormatException e) {
                ParseErrorCheck(false, unit, "int");
            }
        }
        if (IsInstanceOf(vType, Long.class)) {
            try {
                return GetConverted(vType, Long.parseLong(unit));
            } catch (NumberFormatException e) {
                ParseErrorCheck(false, unit, "long");
            }
        }

        // Now do String parsing.
        if (IsInstanceOf(vType, String.class)) {
            // Remove quotes
            if (unit.length() >= 2) {
                if (unit.charAt(0) == '\"' && unit.charAt(unit.length() - 1) == '\"') {
                    unit = unit.substring(1, unit.length() - 1);
                }
            }
            return GetConverted(vType, unit);
        }

        // TODO: Array
        /*
        // For arrays, parse them uh individually.
        if (IsInstanceOf(vType, List)) {
        // Get the type of the individual array generic by creating a dummy. Not a smart idea but whatever.
        Array dummy = GetConverted<Array>(Activator.CreateInstance<V>());
        Type subType = dummy.GetType().GetElementType();

        // Call the generic method with reflection. TODO: This is kinda bad but whatever.
        var thisMethod = typeof(Arg<T>).GetMethod("ParseUnitUtil");
        var thisMethodRef = thisMethod.MakeGenericMethod(subType);

        List<object> result = new List<object>();

        String[] remainingUnits = new String[unitPlusRemainder.Length];
        unitPlusRemainder.CopyTo(remainingUnits, 0);
        foreach(String subUnit in unitPlusRemainder) {

        // Sanity check
        if (remainingUnits.Length == 0 || subUnit != remainingUnits[0]) {
        Debug.LogError("SANITY CHECK FAILED!");
        Debug.LogError($"This shouldn't happen: {remainingUnits.Length}");
        Debug.LogError($"This shouldn't happen: {subUnit} != {(remainingUnits.Length != 0? remainingUnits[0] : null)}");
        break;
        }

        // Parse the first value and add to our array.
        object subValue = thisMethodRef.Invoke(this, new object[] { subUnit, remainingUnits });
        result.Add(subValue);

        Debug.Log($"MID ARRAY DEBUG: Sub: {subUnit} = {subValue}");

        // Pop off the first unit
        String[] copy = new String[remainingUnits.Length - 1];
        remainingUnits.CopyTo(copy, 1);
        remainingUnits = copy;
        }

        return GetConverted(vType,  result.ToArray() );
        }
         */
        throw new CommandException("Arguments are not programmed to parse the following type: {typeof(T)}. This is either not implemented intentionally or by accident somehow.");
    }

    @Override
    public Object ParseUnit(String unit, String[] unitPlusRemainder) throws CommandException {
        return ParseUnitUtil(_tType, unit, unitPlusRemainder);
    }

    public boolean CheckValidUnit(String arg, StringBuilder errorMsg) {
        errorMsg.delete(0, errorMsg.length());
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V GetDefault(Class<V> vType) {
        return GetConverted(vType, Default);
    }
}

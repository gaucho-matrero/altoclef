package adris.altoclef.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    /**
     * Regular Expression to validate a string.
     *
     * @param val the string to be validated.
     * @return if the string is valid or not.
     */
    public static boolean isValidFormattedString(final String val) {
        final Pattern pattern = Pattern.compile("[^a-zA-Z0-9._ +]");
        final Matcher matcher = pattern.matcher(val);
        return !matcher.find();
    }

    /**
     * Validates a string through different tests.
     *
     * @param val string to be validated.
     * @return if string is valid.
     */
    public static boolean isValidString(final String val) {
        return val != null && !val.isEmpty() && !val.isBlank();
    }

    /**
     * Checks if a string is valid for file naming.
     *
     * @param val the string to be validated.
     * @return if string is valid.
     */
    public static boolean isValidName(final String val) {
        return val != null && !val.isEmpty() && !val.isBlank() && isValidFormattedString(val);
    }

    /**
     * Raises an exception if name is not formatted properly.
     *
     * @param name name to be validated.
     */
    public static void riseInvalidName(final String name) throws IllegalArgumentException, NullPointerException {
        riseNull(name);

        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
    }

    /**
     * Rise an exception if name is invalid.
     *
     * @param name to be validated.
     */
    public static void riseInvalidString(final String name) throws IllegalArgumentException, NullPointerException {
        riseNull(name);

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is empty");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("Name is blank");
        }
    }

    /**
     * Throw an exception if object is null.
     *
     * @param object the object.
     */
    public static void riseNull(final Object object) throws NullPointerException {
        if (object == null) {
            throw new NullPointerException("object is null");
        }
    }

    public static boolean isNull(final Object object) {
        return object == null;
    }

    public static boolean isSet(final Object object) {return !isNull(object);}
}

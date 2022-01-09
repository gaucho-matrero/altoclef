package adris.altoclef.util;

import java.util.HashMap;
import java.util.Map;

public class Store {
    private final Map<String, Object> storage = new HashMap<>();

    /**
     * Rise an exception if name or object is invalid.
     *
     * @param name to be validated.
     * @param object to be validated.
     * @throws IllegalArgumentException if either input is invalid.
     */
    public final void rise(final String name, final Object object) throws IllegalArgumentException {
        Utils.riseInvalidString(name);

        if (object == null) {
            throw new IllegalArgumentException("object is null");
        }
    }

    /**
     * Rise an exception if name is invalid.
     *
     * @param name to be validated.
     * @throws IllegalArgumentException if name is invalid.
     */
    public final void rise(final String name) throws IllegalArgumentException {
        Utils.riseInvalidString(name);

        if (!hasAttribute(name)) {
            throw new IllegalArgumentException("no object is registered under the given name: " + name);
        }
    }

    /**
     * Sets an attribute in the store.
     *
     * @param name of the attribute.
     * @param object the object corresponding.
     * @throws IllegalArgumentException if either input is invalid.
     */
    public void setAttribute(final String name, final Object object) throws IllegalArgumentException {
        rise(name, object);
        storage.put(name, object);
    }

    /**
     * Return an attribute with given name.
     *
     * @param name of the attribute to be returned.
     * @return the object corresponding to the attribute.
     * @throws IllegalArgumentException if name is invalid.
     */
    public Object getAttribute(final String name) throws IllegalArgumentException {
        rise(name);
        return storage.get(name);
    }

    /**
     * If an attribute exists in the store.
     *
     * @param name of the attribute to be looked for.
     * @return if the attribute exists.
     */
    public final boolean hasAttribute(final String name) {
        return storage.containsKey(name);
    }

    public final boolean removeAttribute(final String name) {
        return Utils.isSet(storage.remove(name));
    }

    public final boolean clearStore() {
        storage.clear();
        return storage.size() < 1;
    }

    /**
     * Returns a casted object from the store.
     *
     * @param key key for the attribute.
     * @param type type of the attribute.
     * @param <T> the class of the object to be returned.
     * @return the casted object.
     */
    public final <T> T fromStorage(final String key, final Class<T> type) {
        final Object obj = getAttribute(key);
        assertInstance(obj, type);
        return (T) obj;
    }

    /**
     * Checks if an object is of certain class.
     *
     * @param object the object to be validated.
     * @param c the class type to be tested against.
     */
    public final void assertInstance(final Object object, Class c) {
        if (object == null) {
            throw new IllegalArgumentException("object is null");
        }

        if (c == null) {
            throw new IllegalArgumentException("class is null");
        }

        if (!object.getClass().equals(c)) {
            throw new IllegalStateException("");
        }
    }
}

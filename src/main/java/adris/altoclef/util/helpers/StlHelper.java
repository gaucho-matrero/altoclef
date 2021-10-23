package adris.altoclef.util.helpers;

import java.util.*;
import java.util.function.Function;

public interface StlHelper {
    static <T> Comparator<T> compareValues(Function<T, Double> getValue) {
        return (left, right) -> (int) Math.signum(getValue.apply(left) - getValue.apply(right));
    }

    static <T> String toString(Collection<T> thing, Function<T, String> toStringFunc) {
        StringBuilder result = new StringBuilder();
        result.append("[");
        int i = 0;
        for (T item : thing) {
            result.append(toStringFunc.apply(item));
            if (i != thing.size() - 1) {
                result.append(",");
            }
            ++i;
        }
        result.append("]");
        return result.toString();
    }

    static <T> String toString(T[] thing, Function<T, String> toStringFunc) {
        return toString(Arrays.asList(thing), toStringFunc);
    }

    @SafeVarargs
    static <T> Collection<T> combine(Collection<T>... collections) {
        int size = Arrays.stream(collections).reduce(0, (count, collection) -> count + collection.size(), Integer::sum);
        Collection<T> result = new ArrayList<>(size);
        for (Collection<T> collection : collections) {
            result.addAll(collection);
        }
        return result;
    }
    @SafeVarargs
    static <T> T[] combine(T[] ... collections) {
        int size = Arrays.stream(collections).reduce(0, (count, collection) -> count + collection.length, Integer::sum);
        List<T> result = new ArrayList<>(size);
        for (T[] collection : collections) {
            Collections.addAll(result, collection);
        }
        // Can't initialize generic arrays I guess.
        @SuppressWarnings("unchecked")
        T[] arr = (T[])new Object[result.size()];
        return result.toArray(arr);
    }
}
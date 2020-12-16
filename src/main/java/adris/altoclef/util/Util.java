package adris.altoclef.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

public interface Util {

    static <T> T[] toArray(Class<T> type, List<T> collection) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, collection.size());
        collection.toArray(result);
        return result;
    }

}

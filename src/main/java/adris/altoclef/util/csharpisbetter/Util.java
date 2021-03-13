package adris.altoclef.util.csharpisbetter;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.List;
import java.util.function.BiPredicate;

public interface Util {

    static <T> T[] toArray(Class<T> type, List<T> collection) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, collection.size());
        collection.toArray(result);
        return result;
    }

    static Vec3d toVec3d(BlockPos pos) {
        if (pos == null) return null;
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    static <T> boolean arraysEqual(T[] a1, T[] a2, BiPredicate<T, T> equals) {
        if (a1.length != a2.length) return false;
        for (int i = 0; i < a1.length; ++i) {
            if (!equals.test(a1[i], a2[i])) return false;
        }
        return true;
    }
    static <T> boolean arraysEqual(T[] a1, T[] a2) {
        return arraysEqual(a1, a2, (left, right) -> {
            if (left == null) {
                return (right == null);
            }
            return left.equals(right);
        });
    }

    static <T> String arrayToString(T[] arr) {
        return ArrayUtils.toString(arr);
    }

}

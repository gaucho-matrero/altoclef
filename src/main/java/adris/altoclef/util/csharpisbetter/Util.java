package adris.altoclef.util.csharpisbetter;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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

    static Vec3d toVec3d(BlockPos pos) {
        if (pos == null) return null;
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

}

package adris.altoclef.util.csharpisbetter;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.Function;

public interface Util {

    static <T> T[] toArray(Class<T> type, Collection<T> collection) {
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

    static <T> boolean arrayContains(T[] array, T item, BiPredicate<T, T> equals) {
        for (T check : array) {
            if (equals.test(check, item)) return true;
        }
        return false;
    }
    static <T> boolean arrayContains(T[] array, T item) {
        return arrayContains(array, item, (left, right) -> {
            if (left == null) {
                return (right == null);
            }
            return left.equals(right);
        });
    }

    static <T> boolean arrayContainsAny(T[] array, T[] items, BiPredicate<T, T> equals) {
        for (T item : items) {
            if (arrayContains(array, item, equals)) return true;
        }
        return false;
    }
    static <T> boolean arrayContainsAny(T[] array, T[] items) {
        for (T item : items) {
            if (arrayContains(array, item)) return true;
        }
        return false;
    }

    static Block[] itemsToBlocks(Item[] items) {
        Block[] result = new Block[items.length];
        for(int i = 0; i < items.length; ++i) {
            result[i] = Block.getBlockFromItem(items[i]);
        }
        return result;
    }
    static Item[] blocksToItems(Block[] blocks) {
        Item[] result = new Item[blocks.length];
        for(int i = 0; i < blocks.length; ++i) {
            result[i] = blocks[i].asItem();
        }
        return result;
    }
    static <T> T maxItem(Collection<T> items, Comparator<T> comparatorRightMinusLeft) {
        if (items.size() == 0) return null;
        T best = items.stream().findFirst().get();
        for (T item : items) {
            // Is item bigger?
            int comparison = comparatorRightMinusLeft.compare(best, item);
            if (comparison > 0) {
                best = item;
            }
        }
        return best;
    }
    static <T> T minItem(Collection<T> items, Comparator<T> comparatorRightMinusLeft) {
        return maxItem(items, (left, right) -> -comparatorRightMinusLeft.compare(left, right));
    }

    static <T> String arrayToString(T[] arr) {
        return arrayToString(arr, elem -> elem == null? "(null)" : elem.toString());
    }
    static <T> String arrayToString(T[] arr, Function<T, String> toString) {
        StringBuilder result = new StringBuilder();
        result.append("[");
        for(int i = 0; i < arr.length; ++i) {
            result.append(toString.apply(arr[i]));
            if (i != arr.length - 1) {
                result.append(",");
            }
        }
        result.append("]");
        return result.toString();
    }

}

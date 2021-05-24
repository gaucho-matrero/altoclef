package adris.altoclef.util.csharpisbetter;


import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.Function;


public final class Util {
    private static final String[] possibilities = { "item.minecraft.", "block.minecraft." };
    
    private Util() {
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Class<T> type, Collection<T> collection) {
        T[] result = (T[]) Array.newInstance(type, collection.size());
        collection.toArray(result);
        return result;
    }
    
    public static Vec3d toVec3d(BlockPos pos) {
        if (pos == null) return null;
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }
    
    public static String stripItemName(Item item) {
        
        for (String possible : possibilities) {
            if (item.getTranslationKey().startsWith(possible)) {
                return item.getTranslationKey().substring(possible.length());
            }
        }
        return item.getTranslationKey();
    }
    
    public static <T> boolean arraysEqual(T[] a1, T[] a2, BiPredicate<T, T> equals) {
        if (a1.length != a2.length) return false;
        for (int i = 0; i < a1.length; ++i) {
            if (!equals.test(a1[i], a2[i])) return false;
        }
        return true;
    }
    
    public static <T> boolean arraysEqual(T[] a1, T[] a2) {
        return arraysEqual(a1, a2, (left, right) -> {
            if (left == null) {
                return (right == null);
            }
            return left.equals(right);
        });
    }
    
    public static <T> boolean arrayContains(T[] array, T item, BiPredicate<T, T> equals) {
        for (T check : array) {
            if (equals.test(check, item)) return true;
        }
        return false;
    }
    
    public static <T> boolean arrayContains(T[] array, T item) {
        return arrayContains(array, item, (left, right) -> {
            if (left == null) {
                return (right == null);
            }
            return left.equals(right);
        });
    }
    
    public static <T> boolean arrayContainsAny(T[] array, T[] items, BiPredicate<T, T> equals) {
        for (T item : items) {
            if (arrayContains(array, item, equals)) return true;
        }
        return false;
    }
    
    public static <T> boolean arrayContainsAny(T[] array, T[] items) {
        for (T item : items) {
            if (arrayContains(array, item)) return true;
        }
        return false;
    }
    
    public static Block[] itemsToBlocks(Item[] items) {
        ArrayList<Block> result = new ArrayList<>();
        for (Item item : items) {
            if (item instanceof BlockItem) {
                Block b = Block.getBlockFromItem(item);
                if (b != null && b != Blocks.AIR) {
                    result.add(b);
                }
            }
            //result[i] = Block.getBlockFromItem(items[i]);
        }
        return toArray(Block.class, result);
    }
    
    public static Item[] blocksToItems(Block[] blocks) {
        Item[] result = new Item[blocks.length];
        for (int i = 0; i < blocks.length; ++i) {
            result[i] = blocks[i].asItem();
        }
        return result;
    }
    
    public static <T> T maxItem(Collection<T> items, Comparator<T> comparatorRightMinusLeft) {
        if (items.isEmpty()) return null;
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
    
    public static <T> T minItem(Collection<? extends T> items, Comparator<? super T> comparatorRightMinusLeft) {
        return maxItem(items, (left, right) -> -comparatorRightMinusLeft.compare(left, right));
    }
    
    public static <T> T maxItem(Collection<? extends T> items, Function<? super T, Double> conversionValue) {
        return maxItem(items, (left, right) -> {
            double leftVal = conversionValue.apply(left), rightVal = conversionValue.apply(right);
            return (int) (rightVal - leftVal);
        });
    }
    
    public static <T> T minItem(Collection<? extends T> items, Function<? super T, Double> conversionValue) {
        return maxItem(items, toConvert -> -1 * conversionValue.apply(toConvert));
    }
    
    public static <T> String arrayToString(T[] arr) {
        return arrayToString(arr, elem -> elem == null ? "(null)" : elem.toString());
    }
    
    public static <T> String arrayToString(T[] arr, Function<? super T, String> toString) {
        StringBuilder result = new StringBuilder();
        result.append("[");
        for (int i = 0; i < arr.length; ++i) {
            result.append(toString.apply(arr[i]));
            if (i != arr.length - 1) {
                result.append(",");
            }
        }
        result.append("]");
        return result.toString();
    }
    
}

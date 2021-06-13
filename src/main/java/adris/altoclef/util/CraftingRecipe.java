package adris.altoclef.util;

import adris.altoclef.Debug;
import net.minecraft.item.Item;

import java.util.Arrays;

public class CraftingRecipe {

    private ItemTarget[] _slots;

    private int _width, _height;

    private boolean _shapeless;

    private String _shortName;

    private int _outputCount;

    // Every item in this list MUST match.
    // Used for beds where the wood can be anything
    // but the wool MUST be the same color.
    //private final Set<Integer> _mustMatch = new HashSet<>();

    private CraftingRecipe() {}

    public ItemTarget getSlot(int index) {

        return _slots[index];
    }

    public int getSlotCount() {
        return _slots.length;
    }

    public int getWidth() {
        return _width;
    }
    public int getHeight() {
        return _height;
    }

    public boolean isShapeless() {
        return _shapeless;
    }

    public boolean isBig() {
        return _slots.length > 4;
    }

    public int outputCount() {return _outputCount; }


    /*
    public boolean mustMatch(int index) {
        return _mustMatch.contains(index);
    }

    public Collection<Integer> getMustMatchCollection() { return _mustMatch; }

    //public int mustMatchCount() {
        return _mustMatch.size();
    }

    /*
    public CraftingRecipe withMustMatch(Integer[] matchingSlotIndices) {
        Collections.addAll(_mustMatch, matchingSlotIndices);

        // Assert they are all the same (because this is assumed later. If this assumption is broken,
        //      you will have to modify InventoryTracker's recipe method and whatever other method we
        //      have for crafting)
        int prev = -1;
        for (int index : matchingSlotIndices) {
            if (prev == -1) {
                prev = index;
                continue;
            }

            ItemTarget currentSlot = _slots[index];
            ItemTarget prevSlot = _slots[prev];
            // Assert they are the same
            if (!currentSlot.equals(prevSlot)) {
                Debug.logError("Invalid \"Matching Slots\" provided. They are not the same: Slot " + index + " vs Slot " + prev);
                return null;
            }

            prev = index;
        }
        return this;
    }
     */

    public static CraftingRecipe newShapedRecipe(Item[][] items, int outputCount) {
        return newShapedRecipe(null, items, outputCount);
    }
    public static CraftingRecipe newShapedRecipe(ItemTarget[] slots, int outputCount) {
        return newShapedRecipe(null, slots, outputCount);
    }
    public static CraftingRecipe newShapedRecipe(String shortName, Item[][] items, int outputCount) {
        return newShapedRecipe(shortName, createSlots(items), outputCount);
    }

    public static CraftingRecipe newShapedRecipe(String shortName, ItemTarget[] slots, int outputCount) {
        if (slots.length != 4 && slots.length != 9) {
            Debug.logError("Invalid shaped crafting recipe, must be either size 4 or 9. Size given: " + slots.length);
            return null;
        }
        /*
        for (ItemTarget slot : slots) {
            if (slot == null) {
                Debug.logError("Null crafting slot detected. Use ItemTarget.EMPTY!");
            }
        }
         */
        CraftingRecipe result = new CraftingRecipe();
        result._shortName = shortName;
        result._slots = slots;
        result._outputCount = outputCount;
        if (slots.length == 4) {
            result._width = 2;
            result._height = 2;
        } else {
            result._width = 3;
            result._height = 3;
        }
        result._shapeless = false;

        return result;
    }

    /*
    public static CraftingRecipe newShapelessRecipe(ItemTarget[] slots) {
        if (slots.length > 9) {
            Debug.logError("Invalid shapeless crafting recipe, must have at most 9 slots. Size given: " + slots.length);
            return null;
        }
        CraftingRecipe result = new CraftingRecipe();
        result._slots = createSlots(slots);
        result._shapeless = true;

        return result;
    }
     */

    private static ItemTarget[] createSlots(ItemTarget[] slots) {
        ItemTarget[] result = new ItemTarget[slots.length];
        System.arraycopy(slots, 0, result, 0, slots.length);
        return result;
    }
    private static ItemTarget[] createSlots(Item[][] slots) {
        ItemTarget[] result = new ItemTarget[slots.length];
        for (int i = 0; i < slots.length; ++i) {
            if (slots[i] == null) {
                result[i] = ItemTarget.EMPTY;
            } else {
                result[i] = new ItemTarget(slots[i]);
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CraftingRecipe) {
            CraftingRecipe other = (CraftingRecipe) o;
            if (other._shapeless != _shapeless) return false;
            if (other._outputCount != _outputCount) return false;
            if (other._height != _height) return false;
            if (other._width != _width) return false;
            //if (other._mustMatch.size() != _mustMatch.size()) return false;
            if (other._slots.length != _slots.length) return false;
            for (int i = 0; i < _slots.length; ++i) {
                if ( (other._slots[i] == null) != (_slots[i] == null) ) return false;
                if (other._slots[i] != null && !other._slots[i].equals(_slots[i])) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String name = "CraftingRecipe{";
            if (_shortName != null) {
                name += "craft " + _shortName;
            } else {
                name += "_slots=" + Arrays.toString(_slots) +
                        ", _width=" + _width +
                        ", _height=" + _height +
                        ", _shapeless=" + _shapeless;
            }
            name += "}";
            return name;
    }

    /*
    public static class ItemTarget {
        private final List<Item> _targetItems = new ArrayList<>();

        public ItemTarget(ItemTarget target) {
            this(target.getMatches());
        }
        public ItemTarget(Item ...items) {
            if (items != null) {
                Collections.addAll(_targetItems, items);
            }
        }

        public boolean matches(Item item) {
            for (Item search : _targetItems) {
                if (!ItemTarget.itemEquals(item, search)) return true;
            }
            return false;
        }
        public Collection<Item> getTargetItems() {
            return _targetItems;
        }

        public boolean isEmpty() {
            return _targetItems.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemTarget other = (ItemTarget) o;
            if (_targetItems.size() != other._targetItems.size()) return false;
            for (int i = 0; i < _targetItems.size(); ++i) {
                if (!ItemTarget.itemEquals(_targetItems.get(i), other._targetItems.get(i))) return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(_targetItems);
        }
    }
     */

    //public static ItemTarget EMPTY = new ItemTarget();
}

package adris.altoclef.util;

import adris.altoclef.Debug;
import adris.altoclef.trackers.InventoryTracker;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.*;

public class CraftingRecipe {

    private CraftingSlot[] _slots;

    private int _width, _height;

    private boolean _shapeless;

    // Every item in this list MUST match.
    // Used for beds where the wood can be anything
    // but the wool MUST be the same color.
    private Set<Integer> _mustMatch = new HashSet<>();

    private CraftingRecipe() {}

    public CraftingSlot getSlot(int index) {
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

    public boolean mustMatch(int index) {
        return _mustMatch.contains(index);
    }

    public int mustMatchCount() {
        return _mustMatch.size();
    }

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

            CraftingSlot currentSlot = _slots[index];
            CraftingSlot prevSlot = _slots[prev];
            // Assert they are the same
            if (!currentSlot.equals(prevSlot)) {
                Debug.logError("Invalid \"Matching Slots\" provided. They are not the same: Slot " + index + " vs Slot " + prev);
                return null;
            }

            prev = index;
        }
        return this;
    }

    public static CraftingRecipe newShapedRecipe(CraftingSlot[] slots) {
        if (slots.length != 4 && slots.length != 9) {
            Debug.logError("Invalid shaped crafting recipe, must be either size 4 or 9. Size given: " + slots.length);
            return null;
        }
        for (CraftingSlot slot : slots) {
            if (slot == null) {
                Debug.logError("Null crafting slot detected. Use CraftingRecipe.EMPTY!");
            }
        }
        CraftingRecipe result = new CraftingRecipe();
        result._slots = slots;
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

    public static CraftingRecipe newShapelessRecipe(CraftingSlot[] slots) {
        if (slots.length > 9) {
            Debug.logError("Invalid shapeless crafting recipe, must have at most 9 slots. Size given: " + slots.length);
            return null;
        }
        CraftingRecipe result = new CraftingRecipe();
        result._slots = slots;
        result._shapeless = true;

        return result;
    }


    public static class CraftingSlot {
        private final List<Item> _targetItems = new ArrayList<>();

        public CraftingSlot(Item ...items) {
            Collections.addAll(_targetItems, items);
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
            CraftingSlot other = (CraftingSlot) o;
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

    // Multi-meaning items that might be useful
    public static CraftingSlot PLANKS = new CraftingSlot(Items.ACACIA_PLANKS, Items.BIRCH_PLANKS, Items.CRIMSON_PLANKS, Items.DARK_OAK_PLANKS, Items.OAK_PLANKS, Items.JUNGLE_PLANKS, Items.SPRUCE_PLANKS, Items.WARPED_PLANKS);
    public static CraftingSlot LOG = new CraftingSlot(Items.ACACIA_LOG, Items.BIRCH_LOG, Items.DARK_OAK_LOG, Items.OAK_LOG, Items.JUNGLE_LOG, Items.SPRUCE_LOG);
    public static CraftingSlot WOOL = new CraftingSlot(Items.WHITE_WOOL, Items.BLACK_WOOL, Items.BLUE_WOOL, Items.BROWN_WOOL, Items.CYAN_WOOL, Items.GRAY_WOOL, Items.GREEN_WOOL, Items.LIGHT_BLUE_WOOL, Items.LIGHT_GRAY_WOOL, Items.LIME_WOOL, Items.MAGENTA_WOOL, Items.ORANGE_WOOL, Items.PINK_WOOL, Items.PURPLE_WOOL, Items.RED_WOOL, Items.YELLOW_WOOL);
    public static CraftingSlot BED = new CraftingSlot(Items.WHITE_BED, Items.BLACK_BED, Items.BLUE_BED, Items.BROWN_BED, Items.CYAN_BED, Items.GRAY_BED, Items.GREEN_BED, Items.LIGHT_BLUE_BED, Items.LIGHT_GRAY_BED, Items.LIME_BED, Items.MAGENTA_BED, Items.ORANGE_BED, Items.PINK_BED, Items.PURPLE_BED, Items.RED_BED, Items.YELLOW_BED);
    public static CraftingSlot CARPET = new CraftingSlot(Items.WHITE_CARPET, Items.BLACK_CARPET, Items.BLUE_CARPET, Items.BROWN_CARPET, Items.CYAN_CARPET, Items.GRAY_CARPET, Items.GREEN_CARPET, Items.LIGHT_BLUE_CARPET, Items.LIGHT_GRAY_CARPET, Items.LIME_CARPET, Items.MAGENTA_CARPET, Items.ORANGE_CARPET, Items.PINK_CARPET, Items.PURPLE_CARPET, Items.RED_CARPET, Items.YELLOW_CARPET);

    public static CraftingSlot EMPTY = new CraftingSlot();
}

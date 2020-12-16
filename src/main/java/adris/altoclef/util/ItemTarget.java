package adris.altoclef.util;

import adris.altoclef.AltoClef;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ItemTarget {
    private Item[] _itemMatches;
    public int targetCount;

    public ItemTarget(Item[] items, int targetCount) {
        _itemMatches = items;
        this.targetCount = targetCount;
    }

    public ItemTarget(Item item, int targetCount) {
        this(new Item[] {item}, targetCount);
    }

    public Item[] getMatches() {
        return _itemMatches;
    }

    public boolean matches(Item item) {
        for (Item match : _itemMatches) {
            if (itemEquals(item, match)) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemTarget) {
            ItemTarget other = (ItemTarget) obj;
            if (targetCount != other.targetCount) return false;
            if (_itemMatches.length != other._itemMatches.length) return false;
            for (int i = 0; i < _itemMatches.length; ++i) {
                if (!itemEquals(_itemMatches[i], other._itemMatches[i])) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {

        StringBuilder result = new StringBuilder("[");
        int counter = 0;
        for (Item item : _itemMatches) {
            result.append( trimItemName(item.getTranslationKey()) );
            if (++counter != _itemMatches.length) {
                result.append(",");
            }
        }
        result.append("]").append(" x ").append(targetCount);

        return result.toString();
    }

    public static boolean itemEquals(Item item1, Item item2) {
        return Item.getRawId(item1) == Item.getRawId(item2);
    }

    public static Item[] PLANKS = new Item[]{ Items.ACACIA_PLANKS, Items.BIRCH_PLANKS, Items.CRIMSON_PLANKS, Items.DARK_OAK_PLANKS, Items.OAK_PLANKS, Items.JUNGLE_PLANKS, Items.SPRUCE_PLANKS, Items.WARPED_PLANKS};
    public static Item[] LOG = new Item[]{ Items.ACACIA_LOG, Items.BIRCH_LOG, Items.DARK_OAK_LOG, Items.OAK_LOG, Items.JUNGLE_LOG, Items.SPRUCE_LOG};
    public static Item[] WOOL = new Item[]{ Items.WHITE_WOOL, Items.BLACK_WOOL, Items.BLUE_WOOL, Items.BROWN_WOOL, Items.CYAN_WOOL, Items.GRAY_WOOL, Items.GREEN_WOOL, Items.LIGHT_BLUE_WOOL, Items.LIGHT_GRAY_WOOL, Items.LIME_WOOL, Items.MAGENTA_WOOL, Items.ORANGE_WOOL, Items.PINK_WOOL, Items.PURPLE_WOOL, Items.RED_WOOL, Items.YELLOW_WOOL};
    public static Item[] BED = new Item[]{ Items.WHITE_BED, Items.BLACK_BED, Items.BLUE_BED, Items.BROWN_BED, Items.CYAN_BED, Items.GRAY_BED, Items.GREEN_BED, Items.LIGHT_BLUE_BED, Items.LIGHT_GRAY_BED, Items.LIME_BED, Items.MAGENTA_BED, Items.ORANGE_BED, Items.PINK_BED, Items.PURPLE_BED, Items.RED_BED, Items.YELLOW_BED};
    public static Item[] CARPET = new Item[]{ Items.WHITE_CARPET, Items.BLACK_CARPET, Items.BLUE_CARPET, Items.BROWN_CARPET, Items.CYAN_CARPET, Items.GRAY_CARPET, Items.GREEN_CARPET, Items.LIGHT_BLUE_CARPET, Items.LIGHT_GRAY_CARPET, Items.LIME_CARPET, Items.MAGENTA_CARPET, Items.ORANGE_CARPET, Items.PINK_CARPET, Items.PURPLE_CARPET, Items.RED_CARPET, Items.YELLOW_CARPET};


    public static String trimItemName(String name) {
        if (name.startsWith("block.minecraft.")) {
            name = name.substring("block.minecraft.".length());
        } else if (name.startsWith("item.minecraft.")) {
            name = name.substring("item.minecraft.".length());
        }
        return name;
    }
    
    /*
    public static Item[] getItemArray(AltoClef mod, Collection<ItemTarget> targets) {
        List<Item> targetItems = new ArrayList<>();
        for(ItemTarget target : targets) {
            if (mod.getInventoryTracker().targetReached(target)) continue;
            targetItems.add(target.item);
        }
        Item[] targetItemsArray = new Item[targetItems.size()];
        targetItems.toArray(targetItemsArray);

        return targetItemsArray;
    }
     */
}

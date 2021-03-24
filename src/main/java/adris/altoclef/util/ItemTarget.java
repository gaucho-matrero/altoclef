package adris.altoclef.util;

import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ItemTarget {
    private Item[] _itemMatches;
    public int targetCount;

    private String _catalogueName = null;

    private boolean _infinite = false;

    public ItemTarget(Item[] items, int targetCount) {
        _itemMatches = items;
        this.targetCount = targetCount;
    }

    public ItemTarget(String catalogueName, int targetCount) {
        if (catalogueName == null) return;
        _catalogueName = catalogueName;
        _itemMatches = TaskCatalogue.getItemMatches(catalogueName);
        this.targetCount = targetCount;
        if (_itemMatches == null) {
            Debug.logError("Invalid catalogue name for item target: \"" + catalogueName + "\". Something isn't robust!");
        }
    }

    public ItemTarget(String catalogueName) {
        this(catalogueName, 99999999); _infinite = true;
    }

    public ItemTarget(Item item, int targetCount) {
        this(new Item[] {item}, targetCount);
    }

    public ItemTarget(Item[] items) {
        this(items, 9999999);
        _infinite = true;
    }
    public ItemTarget(Item item) {
        this(item, 9999999);
        _infinite = true;
    }

    public ItemTarget(ItemTarget toCopy) {
        _itemMatches = new Item[toCopy._itemMatches.length];
        System.arraycopy(toCopy._itemMatches, 0, _itemMatches,  0, toCopy._itemMatches.length);
        _catalogueName = toCopy._catalogueName;
        targetCount = toCopy.targetCount;
        _infinite = toCopy._infinite;
    }

    public Item[] getMatches() {
        return _itemMatches;
    }

    public boolean matches(Item item) {
        for (Item match : _itemMatches) {
            if (match == null) continue;
            if (match.equals(item)) return true;
        }
        return false;
    }

    public boolean isCatalogueItem() {
        return _catalogueName != null;
    }
    public String getCatalogueName() {
        return _catalogueName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemTarget) {
            ItemTarget other = (ItemTarget) obj;
            if (_infinite) {
                if (!other._infinite) return false;
            } else {
                // Neither are infinite
                if (targetCount != other.targetCount) return false;
            }
            if ((other._itemMatches == null) != (_itemMatches == null)) return false;
            boolean isNull = (other._itemMatches == null);
            if (isNull) return true;
            if (_itemMatches.length != other._itemMatches.length) return false;
            for (int i = 0; i < _itemMatches.length; ++i) {
                if (other._itemMatches[i] == null) {
                    if ((other._itemMatches[i] == null) != (_itemMatches[i] == null)) return false;
                } else {
                    if (!other._itemMatches[i].equals(_itemMatches[i])) return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {

        StringBuilder result = new StringBuilder();
        if (isEmpty()) {
            result.append("(empty)");
        } else if (isCatalogueItem()) {
            result.append(_catalogueName);
        } else {
            result.append("[");
            int counter = 0;
            for (Item item : _itemMatches) {
                if (item == null) {
                    result.append("(null??)");
                } else {
                    result.append(trimItemName(item.getTranslationKey()));
                }
                if (++counter != _itemMatches.length) {
                    result.append(",");
                }
            }
            result.append("]");
        }
        if (!_infinite && !isEmpty()) {
            result.append(" x ").append(targetCount);
        }

        return result.toString();
    }

    public static ItemTarget EMPTY = new ItemTarget(new Item[0], 0);

    public static Item[] PLANKS = new Item[]{ Items.ACACIA_PLANKS, Items.BIRCH_PLANKS, Items.CRIMSON_PLANKS, Items.DARK_OAK_PLANKS, Items.OAK_PLANKS, Items.JUNGLE_PLANKS, Items.SPRUCE_PLANKS, Items.WARPED_PLANKS};
    public static Item[] WOOD_BUTTON = new Item[]{ Items.ACACIA_BUTTON, Items.BIRCH_BUTTON, Items.CRIMSON_BUTTON, Items.DARK_OAK_BUTTON, Items.OAK_BUTTON, Items.JUNGLE_BUTTON, Items.SPRUCE_BUTTON, Items.WARPED_BUTTON};
    public static Item[] WOOD_SIGN = new Item[]{ Items.ACACIA_SIGN, Items.BIRCH_SIGN, Items.CRIMSON_SIGN, Items.DARK_OAK_SIGN, Items.OAK_SIGN, Items.JUNGLE_SIGN, Items.SPRUCE_SIGN, Items.WARPED_SIGN};
    public static Item[] WOOD_PRESSURE_PLATE = new Item[]{ Items.ACACIA_PRESSURE_PLATE, Items.BIRCH_PRESSURE_PLATE, Items.CRIMSON_PRESSURE_PLATE, Items.DARK_OAK_PRESSURE_PLATE, Items.OAK_PRESSURE_PLATE, Items.JUNGLE_PRESSURE_PLATE, Items.SPRUCE_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE};
    public static Item[] WOOD_BOAT = new Item[]{ Items.ACACIA_BOAT, Items.BIRCH_BOAT, Items.DARK_OAK_BOAT, Items.OAK_BOAT, Items.JUNGLE_BOAT, Items.SPRUCE_BOAT};
    public static Item[] WOOD_DOOR = new Item[]{ Items.ACACIA_DOOR, Items.BIRCH_DOOR, Items.CRIMSON_DOOR, Items.DARK_OAK_DOOR, Items.OAK_DOOR, Items.JUNGLE_DOOR, Items.SPRUCE_DOOR, Items.WARPED_DOOR};
    public static Item[] WOOD_TRAPDOOR = new Item[]{ Items.ACACIA_TRAPDOOR, Items.BIRCH_TRAPDOOR, Items.CRIMSON_TRAPDOOR, Items.DARK_OAK_TRAPDOOR, Items.OAK_TRAPDOOR, Items.JUNGLE_TRAPDOOR, Items.SPRUCE_TRAPDOOR, Items.WARPED_TRAPDOOR};
    public static Item[] LOG = new Item[]{ Items.ACACIA_LOG, Items.BIRCH_LOG, Items.DARK_OAK_LOG, Items.OAK_LOG, Items.JUNGLE_LOG, Items.SPRUCE_LOG,
            Items.ACACIA_WOOD, Items.BIRCH_WOOD, Items.DARK_OAK_WOOD, Items.OAK_WOOD, Items.JUNGLE_WOOD, Items.SPRUCE_WOOD,
            Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_BIRCH_LOG, Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_OAK_LOG, Items.STRIPPED_JUNGLE_LOG, Items.STRIPPED_SPRUCE_LOG,
    Items.CRIMSON_STEM, Items.WARPED_STEM, Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_WARPED_STEM, Items.STRIPPED_CRIMSON_HYPHAE, Items.STRIPPED_WARPED_HYPHAE};

    public static Item[] DYE = new Item[]{ Items.WHITE_DYE, Items.BLACK_DYE, Items.BLUE_DYE, Items.BROWN_DYE, Items.CYAN_DYE, Items.GRAY_DYE, Items.GREEN_DYE, Items.LIGHT_BLUE_DYE, Items.LIGHT_GRAY_DYE, Items.LIME_DYE, Items.MAGENTA_DYE, Items.ORANGE_DYE, Items.PINK_DYE, Items.PURPLE_DYE, Items.RED_DYE, Items.YELLOW_DYE};
    public static Item[] WOOL = new Item[]{ Items.WHITE_WOOL, Items.BLACK_WOOL, Items.BLUE_WOOL, Items.BROWN_WOOL, Items.CYAN_WOOL, Items.GRAY_WOOL, Items.GREEN_WOOL, Items.LIGHT_BLUE_WOOL, Items.LIGHT_GRAY_WOOL, Items.LIME_WOOL, Items.MAGENTA_WOOL, Items.ORANGE_WOOL, Items.PINK_WOOL, Items.PURPLE_WOOL, Items.RED_WOOL, Items.YELLOW_WOOL};
    public static Item[] BED = new Item[]{ Items.WHITE_BED, Items.BLACK_BED, Items.BLUE_BED, Items.BROWN_BED, Items.CYAN_BED, Items.GRAY_BED, Items.GREEN_BED, Items.LIGHT_BLUE_BED, Items.LIGHT_GRAY_BED, Items.LIME_BED, Items.MAGENTA_BED, Items.ORANGE_BED, Items.PINK_BED, Items.PURPLE_BED, Items.RED_BED, Items.YELLOW_BED};
    public static Item[] CARPET = new Item[]{ Items.WHITE_CARPET, Items.BLACK_CARPET, Items.BLUE_CARPET, Items.BROWN_CARPET, Items.CYAN_CARPET, Items.GRAY_CARPET, Items.GREEN_CARPET, Items.LIGHT_BLUE_CARPET, Items.LIGHT_GRAY_CARPET, Items.LIME_CARPET, Items.MAGENTA_CARPET, Items.ORANGE_CARPET, Items.PINK_CARPET, Items.PURPLE_CARPET, Items.RED_CARPET, Items.YELLOW_CARPET};

    public static Block[] WOOD_SIGNS_ALL = new Block[]{ Blocks.ACACIA_SIGN, Blocks.BIRCH_SIGN, Blocks.DARK_OAK_SIGN, Blocks.OAK_SIGN, Blocks.JUNGLE_SIGN, Blocks.SPRUCE_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.DARK_OAK_WALL_SIGN, Blocks.OAK_WALL_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.SPRUCE_WALL_SIGN};

    public static String trimItemName(String name) {
        if (name.startsWith("block.minecraft.")) {
            name = name.substring("block.minecraft.".length());
        } else if (name.startsWith("item.minecraft.")) {
            name = name.substring("item.minecraft.".length());
        }
        return name;
    }

    public static Block[] itemsToBlocks(Item ...items) {
        Block[] result = new Block[items.length];
        for (int i = 0; i < items.length; ++i) {
            result[i] = Block.getBlockFromItem(items[i]);
        }
        return result;
    }

    public static Item[] getMatches(ItemTarget ...targets) {
        Set<Item> result = new HashSet<>();
        for (ItemTarget target : targets) {
            result.addAll(Arrays.asList(target.getMatches()));
        }
        return Util.toArray(Item.class, result);
    }

    public boolean isEmpty() {
        return _itemMatches == null || _itemMatches.length == 0;
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

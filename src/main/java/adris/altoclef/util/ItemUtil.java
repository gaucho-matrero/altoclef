package adris.altoclef.util;


import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MaterialColor;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class ItemUtil {
    public static final Item[] PLANKS = {
            Items.ACACIA_PLANKS, Items.BIRCH_PLANKS, Items.CRIMSON_PLANKS, Items.DARK_OAK_PLANKS, Items.OAK_PLANKS, Items.JUNGLE_PLANKS,
            Items.SPRUCE_PLANKS, Items.WARPED_PLANKS
    };
    
    public static final Item[] LEAVES = {
            Items.ACACIA_LEAVES, Items.BIRCH_LEAVES, Items.DARK_OAK_LEAVES, Items.OAK_LEAVES, Items.JUNGLE_LEAVES, Items.SPRUCE_LEAVES
    };
    
    public static final Item[] WOOD_BUTTON = {
            Items.ACACIA_BUTTON, Items.BIRCH_BUTTON, Items.CRIMSON_BUTTON, Items.DARK_OAK_BUTTON, Items.OAK_BUTTON, Items.JUNGLE_BUTTON,
            Items.SPRUCE_BUTTON, Items.WARPED_BUTTON
    };
    
    public static final Item[] WOOD_SIGN = {
            Items.ACACIA_SIGN, Items.BIRCH_SIGN, Items.CRIMSON_SIGN, Items.DARK_OAK_SIGN, Items.OAK_SIGN, Items.JUNGLE_SIGN,
            Items.SPRUCE_SIGN, Items.WARPED_SIGN
    };
    
    public static final Item[] WOOD_PRESSURE_PLATE = {
            Items.ACACIA_PRESSURE_PLATE, Items.BIRCH_PRESSURE_PLATE, Items.CRIMSON_PRESSURE_PLATE, Items.DARK_OAK_PRESSURE_PLATE,
            Items.OAK_PRESSURE_PLATE, Items.JUNGLE_PRESSURE_PLATE, Items.SPRUCE_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE
    };
    
    public static final Item[] WOOD_FENCE = {
            Items.ACACIA_FENCE, Items.BIRCH_FENCE, Items.DARK_OAK_FENCE, Items.OAK_FENCE, Items.JUNGLE_FENCE, Items.SPRUCE_FENCE,
            Items.CRIMSON_FENCE, Items.WARPED_FENCE
    };
    
    public static final Item[] WOOD_FENCE_GATE = {
            Items.ACACIA_FENCE_GATE, Items.BIRCH_FENCE_GATE, Items.DARK_OAK_FENCE_GATE, Items.OAK_FENCE_GATE, Items.JUNGLE_FENCE_GATE,
            Items.SPRUCE_FENCE_GATE, Items.CRIMSON_FENCE_GATE, Items.WARPED_FENCE_GATE
    };
    
    public static final Item[] WOOD_BOAT = {
            Items.ACACIA_BOAT, Items.BIRCH_BOAT, Items.DARK_OAK_BOAT, Items.OAK_BOAT, Items.JUNGLE_BOAT, Items.SPRUCE_BOAT
    };
    
    public static final Item[] WOOD_DOOR = {
            Items.ACACIA_DOOR, Items.BIRCH_DOOR, Items.CRIMSON_DOOR, Items.DARK_OAK_DOOR, Items.OAK_DOOR, Items.JUNGLE_DOOR,
            Items.SPRUCE_DOOR, Items.WARPED_DOOR
    };
    
    public static final Item[] WOOD_SLAB = {
            Items.ACACIA_SLAB, Items.BIRCH_SLAB, Items.CRIMSON_SLAB, Items.DARK_OAK_SLAB, Items.OAK_SLAB, Items.JUNGLE_SLAB,
            Items.SPRUCE_SLAB, Items.WARPED_SLAB
    };
    
    public static final Item[] WOOD_STAIRS = {
            Items.ACACIA_STAIRS, Items.BIRCH_STAIRS, Items.CRIMSON_STAIRS, Items.DARK_OAK_STAIRS, Items.OAK_STAIRS, Items.JUNGLE_STAIRS,
            Items.SPRUCE_STAIRS, Items.WARPED_STAIRS
    };
    
    public static final Item[] WOOD_TRAPDOOR = {
            Items.ACACIA_TRAPDOOR, Items.BIRCH_TRAPDOOR, Items.CRIMSON_TRAPDOOR, Items.DARK_OAK_TRAPDOOR, Items.OAK_TRAPDOOR,
            Items.JUNGLE_TRAPDOOR, Items.SPRUCE_TRAPDOOR, Items.WARPED_TRAPDOOR
    };
    
    public static final Item[] LOG = {
            Items.ACACIA_LOG, Items.BIRCH_LOG, Items.DARK_OAK_LOG, Items.OAK_LOG, Items.JUNGLE_LOG, Items.SPRUCE_LOG, Items.ACACIA_WOOD,
            Items.BIRCH_WOOD, Items.DARK_OAK_WOOD, Items.OAK_WOOD, Items.JUNGLE_WOOD, Items.SPRUCE_WOOD, Items.STRIPPED_ACACIA_LOG,
            Items.STRIPPED_BIRCH_LOG, Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_OAK_LOG, Items.STRIPPED_JUNGLE_LOG,
            Items.STRIPPED_SPRUCE_LOG, Items.CRIMSON_STEM, Items.WARPED_STEM, Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_WARPED_STEM,
            Items.STRIPPED_CRIMSON_HYPHAE, Items.STRIPPED_WARPED_HYPHAE
    };
    
    public static final Item[] DYE = {
            Items.WHITE_DYE, Items.BLACK_DYE, Items.BLUE_DYE, Items.BROWN_DYE, Items.CYAN_DYE, Items.GRAY_DYE, Items.GREEN_DYE,
            Items.LIGHT_BLUE_DYE, Items.LIGHT_GRAY_DYE, Items.LIME_DYE, Items.MAGENTA_DYE, Items.ORANGE_DYE, Items.PINK_DYE,
            Items.PURPLE_DYE, Items.RED_DYE, Items.YELLOW_DYE
    };
    
    public static final Item[] WOOL = {
            Items.WHITE_WOOL, Items.BLACK_WOOL, Items.BLUE_WOOL, Items.BROWN_WOOL, Items.CYAN_WOOL, Items.GRAY_WOOL, Items.GREEN_WOOL,
            Items.LIGHT_BLUE_WOOL, Items.LIGHT_GRAY_WOOL, Items.LIME_WOOL, Items.MAGENTA_WOOL, Items.ORANGE_WOOL, Items.PINK_WOOL,
            Items.PURPLE_WOOL, Items.RED_WOOL, Items.YELLOW_WOOL
    };
    
    public static final Item[] BED = {
            Items.WHITE_BED, Items.BLACK_BED, Items.BLUE_BED, Items.BROWN_BED, Items.CYAN_BED, Items.GRAY_BED, Items.GREEN_BED,
            Items.LIGHT_BLUE_BED, Items.LIGHT_GRAY_BED, Items.LIME_BED, Items.MAGENTA_BED, Items.ORANGE_BED, Items.PINK_BED,
            Items.PURPLE_BED, Items.RED_BED, Items.YELLOW_BED
    };
    
    public static final Item[] CARPET = {
            Items.WHITE_CARPET, Items.BLACK_CARPET, Items.BLUE_CARPET, Items.BROWN_CARPET, Items.CYAN_CARPET, Items.GRAY_CARPET,
            Items.GREEN_CARPET, Items.LIGHT_BLUE_CARPET, Items.LIGHT_GRAY_CARPET, Items.LIME_CARPET, Items.MAGENTA_CARPET,
            Items.ORANGE_CARPET, Items.PINK_CARPET, Items.PURPLE_CARPET, Items.RED_CARPET, Items.YELLOW_CARPET
    };
    
    public static final Item[] FLOWER = {
            Items.ALLIUM, Items.AZURE_BLUET, Items.BLUE_ORCHID, Items.CORNFLOWER, Items.DANDELION, Items.LILAC, Items.LILY_OF_THE_VALLEY,
            Items.ORANGE_TULIP, Items.OXEYE_DAISY, Items.PINK_TULIP, Items.POPPY, Items.PEONY, Items.RED_TULIP, Items.ROSE_BUSH,
            Items.SUNFLOWER, Items.WHITE_TULIP
    };
    
    public static final Block[] WOOD_SIGNS_ALL = {
            Blocks.ACACIA_SIGN, Blocks.BIRCH_SIGN, Blocks.DARK_OAK_SIGN, Blocks.OAK_SIGN, Blocks.JUNGLE_SIGN, Blocks.SPRUCE_SIGN,
            Blocks.ACACIA_WALL_SIGN, Blocks.BIRCH_WALL_SIGN, Blocks.DARK_OAK_WALL_SIGN, Blocks.OAK_WALL_SIGN, Blocks.JUNGLE_WALL_SIGN,
            Blocks.SPRUCE_WALL_SIGN
    };
    
    public static final Map<Item, Item> _logToPlanks = new HashMap<Item, Item>() {
        {
            put(Items.ACACIA_LOG, Items.ACACIA_PLANKS);
            put(Items.BIRCH_LOG, Items.BIRCH_PLANKS);
            put(Items.CRIMSON_STEM, Items.CRIMSON_PLANKS);
            put(Items.DARK_OAK_LOG, Items.DARK_OAK_PLANKS);
            put(Items.OAK_LOG, Items.OAK_PLANKS);
            put(Items.JUNGLE_LOG, Items.JUNGLE_PLANKS);
            put(Items.SPRUCE_LOG, Items.SPRUCE_PLANKS);
            put(Items.WARPED_STEM, Items.WARPED_PLANKS);
            List<Item> logsToReverse = new ArrayList<>(keySet());
            // Other way around
            for (Item log : logsToReverse) {
                put(get(log), log);
            }
        }
    };
    
    public static final Map<MaterialColor, ColorfulItems> colorMap
            = ImmutableMap.<MaterialColor, ColorfulItems>builder()
                          .put(MaterialColor.WHITE,
                               new ColorfulItems(DyeColor.WHITE, "white", Items.WHITE_DYE, Items.WHITE_WOOL, Items.WHITE_BED,
                                                 Items.WHITE_CARPET, Items.WHITE_STAINED_GLASS, Items.WHITE_STAINED_GLASS_PANE,
                                                 Items.WHITE_TERRACOTTA, Items.WHITE_GLAZED_TERRACOTTA, Items.WHITE_CONCRETE,
                                                 Items.WHITE_CONCRETE_POWDER, Items.WHITE_BANNER, Items.WHITE_SHULKER_BOX,
                                                 Blocks.WHITE_WALL_BANNER))
                          .put(MaterialColor.BLACK,
                               new ColorfulItems(DyeColor.BLACK, "black", Items.BLACK_DYE, Items.BLACK_WOOL, Items.BLACK_BED,
                                                 Items.BLACK_CARPET, Items.BLACK_STAINED_GLASS, Items.BLACK_STAINED_GLASS_PANE,
                                                 Items.BLACK_TERRACOTTA, Items.BLACK_GLAZED_TERRACOTTA, Items.BLACK_CONCRETE,
                                                 Items.BLACK_CONCRETE_POWDER, Items.BLACK_BANNER, Items.BLACK_SHULKER_BOX,
                                                 Blocks.BLACK_WALL_BANNER))
                          .put(MaterialColor.BLUE,
                               new ColorfulItems(DyeColor.BLUE, "blue", Items.BLUE_DYE, Items.BLUE_WOOL, Items.BLUE_BED, Items.BLUE_CARPET,
                                                 Items.BLUE_STAINED_GLASS, Items.BLUE_STAINED_GLASS_PANE, Items.BLUE_TERRACOTTA,
                                                 Items.BLUE_GLAZED_TERRACOTTA, Items.BLUE_CONCRETE, Items.BLUE_CONCRETE_POWDER,
                                                 Items.BLUE_BANNER, Items.BLUE_SHULKER_BOX, Blocks.BLUE_WALL_BANNER))
                          .put(MaterialColor.BROWN,
                               new ColorfulItems(DyeColor.BROWN, "brown", Items.BROWN_DYE, Items.BROWN_WOOL, Items.BROWN_BED,
                                                 Items.BROWN_CARPET, Items.BROWN_STAINED_GLASS, Items.BROWN_STAINED_GLASS_PANE,
                                                 Items.BROWN_TERRACOTTA, Items.BROWN_GLAZED_TERRACOTTA, Items.BROWN_CONCRETE,
                                                 Items.BROWN_CONCRETE_POWDER, Items.BROWN_BANNER, Items.BROWN_SHULKER_BOX,
                                                 Blocks.BROWN_WALL_BANNER))
                          .put(MaterialColor.CYAN,
                               new ColorfulItems(DyeColor.CYAN, "cyan", Items.CYAN_DYE, Items.CYAN_WOOL, Items.CYAN_BED, Items.CYAN_CARPET,
                                                 Items.CYAN_STAINED_GLASS, Items.CYAN_STAINED_GLASS_PANE, Items.CYAN_TERRACOTTA,
                                                 Items.CYAN_GLAZED_TERRACOTTA, Items.CYAN_CONCRETE, Items.CYAN_CONCRETE_POWDER,
                                                 Items.CYAN_BANNER, Items.CYAN_SHULKER_BOX, Blocks.CYAN_WALL_BANNER))
                          .put(MaterialColor.GRAY,
                               new ColorfulItems(DyeColor.GRAY, "gray", Items.GRAY_DYE, Items.GRAY_WOOL, Items.GRAY_BED, Items.GRAY_CARPET,
                                                 Items.GRAY_STAINED_GLASS, Items.GRAY_STAINED_GLASS_PANE, Items.GRAY_TERRACOTTA,
                                                 Items.GRAY_GLAZED_TERRACOTTA, Items.GRAY_CONCRETE, Items.GRAY_CONCRETE_POWDER,
                                                 Items.GRAY_BANNER, Items.GRAY_SHULKER_BOX, Blocks.GRAY_WALL_BANNER))
                          .put(MaterialColor.GREEN,
                               new ColorfulItems(DyeColor.GREEN, "green", Items.GREEN_DYE, Items.GREEN_WOOL, Items.GREEN_BED,
                                                 Items.GREEN_CARPET, Items.GREEN_STAINED_GLASS, Items.GREEN_STAINED_GLASS_PANE,
                                                 Items.GREEN_TERRACOTTA, Items.GREEN_GLAZED_TERRACOTTA, Items.GREEN_CONCRETE,
                                                 Items.GREEN_CONCRETE_POWDER, Items.GREEN_BANNER, Items.GREEN_SHULKER_BOX,
                                                 Blocks.GREEN_WALL_BANNER))
                          .put(MaterialColor.LIGHT_BLUE,
                               new ColorfulItems(DyeColor.LIGHT_BLUE, "light_blue", Items.LIGHT_BLUE_DYE, Items.LIGHT_BLUE_WOOL,
                                                 Items.LIGHT_BLUE_BED, Items.LIGHT_BLUE_CARPET, Items.LIGHT_BLUE_STAINED_GLASS,
                                                 Items.LIGHT_BLUE_STAINED_GLASS_PANE, Items.LIGHT_BLUE_TERRACOTTA,
                                                 Items.LIGHT_BLUE_GLAZED_TERRACOTTA, Items.LIGHT_BLUE_CONCRETE,
                                                 Items.LIGHT_BLUE_CONCRETE_POWDER, Items.LIGHT_BLUE_BANNER, Items.LIGHT_BLUE_SHULKER_BOX,
                                                 Blocks.LIGHT_BLUE_WALL_BANNER))
                          .put(MaterialColor.LIGHT_GRAY,
                               new ColorfulItems(DyeColor.LIGHT_GRAY, "light_gray", Items.LIGHT_GRAY_DYE, Items.LIGHT_GRAY_WOOL,
                                                 Items.LIGHT_GRAY_BED, Items.LIGHT_GRAY_CARPET, Items.LIGHT_GRAY_STAINED_GLASS,
                                                 Items.LIGHT_GRAY_STAINED_GLASS_PANE, Items.LIGHT_GRAY_TERRACOTTA,
                                                 Items.LIGHT_GRAY_GLAZED_TERRACOTTA, Items.LIGHT_GRAY_CONCRETE,
                                                 Items.LIGHT_GRAY_CONCRETE_POWDER, Items.LIGHT_GRAY_BANNER, Items.LIGHT_GRAY_SHULKER_BOX,
                                                 Blocks.LIGHT_GRAY_WALL_BANNER))
                          .put(MaterialColor.LIME,
                               new ColorfulItems(DyeColor.LIME, "lime", Items.LIME_DYE, Items.LIME_WOOL, Items.LIME_BED, Items.LIME_CARPET,
                                                 Items.LIME_STAINED_GLASS, Items.LIME_STAINED_GLASS_PANE, Items.LIME_TERRACOTTA,
                                                 Items.LIME_GLAZED_TERRACOTTA, Items.LIME_CONCRETE, Items.LIME_CONCRETE_POWDER,
                                                 Items.LIME_BANNER, Items.LIME_SHULKER_BOX, Blocks.LIME_WALL_BANNER))
                          .put(MaterialColor.MAGENTA,
                               new ColorfulItems(DyeColor.MAGENTA, "magenta", Items.MAGENTA_DYE, Items.MAGENTA_WOOL, Items.MAGENTA_BED,
                                                 Items.MAGENTA_CARPET, Items.MAGENTA_STAINED_GLASS, Items.MAGENTA_STAINED_GLASS_PANE,
                                                 Items.MAGENTA_TERRACOTTA, Items.MAGENTA_GLAZED_TERRACOTTA, Items.MAGENTA_CONCRETE,
                                                 Items.MAGENTA_CONCRETE_POWDER, Items.MAGENTA_BANNER, Items.MAGENTA_SHULKER_BOX,
                                                 Blocks.MAGENTA_WALL_BANNER))
                          .put(MaterialColor.ORANGE,
                               new ColorfulItems(DyeColor.ORANGE, "orange", Items.ORANGE_DYE, Items.ORANGE_WOOL, Items.ORANGE_BED,
                                                 Items.ORANGE_CARPET, Items.ORANGE_STAINED_GLASS, Items.ORANGE_STAINED_GLASS_PANE,
                                                 Items.ORANGE_TERRACOTTA, Items.ORANGE_GLAZED_TERRACOTTA, Items.ORANGE_CONCRETE,
                                                 Items.ORANGE_CONCRETE_POWDER, Items.ORANGE_BANNER, Items.ORANGE_SHULKER_BOX,
                                                 Blocks.ORANGE_WALL_BANNER))
                          .put(MaterialColor.PINK,
                               new ColorfulItems(DyeColor.PINK, "pink", Items.PINK_DYE, Items.PINK_WOOL, Items.PINK_BED, Items.PINK_CARPET,
                                                 Items.PINK_STAINED_GLASS, Items.PINK_STAINED_GLASS_PANE, Items.PINK_TERRACOTTA,
                                                 Items.PINK_GLAZED_TERRACOTTA, Items.PINK_CONCRETE, Items.PINK_CONCRETE_POWDER,
                                                 Items.PINK_BANNER, Items.PINK_SHULKER_BOX, Blocks.PINK_WALL_BANNER))
                          .put(MaterialColor.PURPLE,
                               new ColorfulItems(DyeColor.PURPLE, "purple", Items.PURPLE_DYE, Items.PURPLE_WOOL, Items.PURPLE_BED,
                                                 Items.PURPLE_CARPET, Items.PURPLE_STAINED_GLASS, Items.PURPLE_STAINED_GLASS_PANE,
                                                 Items.PURPLE_TERRACOTTA, Items.PURPLE_GLAZED_TERRACOTTA, Items.PURPLE_CONCRETE,
                                                 Items.PURPLE_CONCRETE_POWDER, Items.PURPLE_BANNER, Items.PURPLE_SHULKER_BOX,
                                                 Blocks.PURPLE_WALL_BANNER))
                          .put(MaterialColor.RED,
                               new ColorfulItems(DyeColor.RED, "red", Items.RED_DYE, Items.RED_WOOL, Items.RED_BED, Items.RED_CARPET,
                                                 Items.RED_STAINED_GLASS, Items.RED_STAINED_GLASS_PANE, Items.RED_TERRACOTTA,
                                                 Items.RED_GLAZED_TERRACOTTA, Items.RED_CONCRETE, Items.RED_CONCRETE_POWDER,
                                                 Items.RED_BANNER, Items.RED_SHULKER_BOX, Blocks.RED_WALL_BANNER))
                          .put(MaterialColor.YELLOW,
                               new ColorfulItems(DyeColor.YELLOW, "yellow", Items.YELLOW_DYE, Items.YELLOW_WOOL, Items.YELLOW_BED,
                                                 Items.YELLOW_CARPET, Items.YELLOW_STAINED_GLASS, Items.YELLOW_STAINED_GLASS_PANE,
                                                 Items.YELLOW_TERRACOTTA, Items.YELLOW_GLAZED_TERRACOTTA, Items.YELLOW_CONCRETE,
                                                 Items.YELLOW_CONCRETE_POWDER, Items.YELLOW_BANNER, Items.YELLOW_SHULKER_BOX,
                                                 Blocks.YELLOW_WALL_BANNER))
                          .build();
    
    public static final Map<WoodType, WoodItems> woodMap
            = ImmutableMap.<WoodType, WoodItems>builder()
                          .put(WoodType.ACACIA, new WoodItems("acacia", Items.ACACIA_PLANKS, Items.ACACIA_LOG, Items.STRIPPED_ACACIA_LOG,
                                                              Items.STRIPPED_ACACIA_WOOD, Items.ACACIA_WOOD, Items.ACACIA_SIGN,
                                                              Items.ACACIA_DOOR, Items.ACACIA_BUTTON, Items.ACACIA_STAIRS,
                                                              Items.ACACIA_SLAB, Items.ACACIA_FENCE, Items.ACACIA_FENCE_GATE,
                                                              Items.ACACIA_BOAT, Items.ACACIA_SAPLING, Items.ACACIA_LEAVES,
                                                              Items.ACACIA_PRESSURE_PLATE, Items.ACACIA_TRAPDOOR))
                          .put(WoodType.BIRCH, new WoodItems("birch", Items.BIRCH_PLANKS, Items.BIRCH_LOG, Items.STRIPPED_BIRCH_LOG,
                                                             Items.STRIPPED_BIRCH_WOOD, Items.BIRCH_WOOD, Items.BIRCH_SIGN,
                                                             Items.BIRCH_DOOR, Items.BIRCH_BUTTON, Items.BIRCH_STAIRS, Items.BIRCH_SLAB,
                                                             Items.BIRCH_FENCE, Items.BIRCH_FENCE_GATE, Items.BIRCH_BOAT,
                                                             Items.BIRCH_SAPLING, Items.BIRCH_LEAVES, Items.BIRCH_PRESSURE_PLATE,
                                                             Items.BIRCH_TRAPDOOR))
                          .put(WoodType.CRIMSON, new WoodItems("crimson", Items.CRIMSON_PLANKS, Items.CRIMSON_ROOTS,
                                                               Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_CRIMSON_HYPHAE,
                                                               Items.CRIMSON_HYPHAE, Items.CRIMSON_SIGN, Items.CRIMSON_DOOR,
                                                               Items.CRIMSON_BUTTON, Items.CRIMSON_STAIRS, Items.CRIMSON_SLAB,
                                                               Items.CRIMSON_FENCE, Items.CRIMSON_FENCE_GATE, null, Items.CRIMSON_FUNGUS,
                                                               Items.NETHER_WART_BLOCK, Items.CRIMSON_PRESSURE_PLATE,
                                                               Items.CRIMSON_TRAPDOOR))
                          .put(WoodType.DARK_OAK, new WoodItems("dark_oak", Items.DARK_OAK_PLANKS, Items.DARK_OAK_LOG,
                                                                Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_DARK_OAK_WOOD,
                                                                Items.DARK_OAK_WOOD, Items.DARK_OAK_SIGN, Items.DARK_OAK_DOOR,
                                                                Items.DARK_OAK_BUTTON, Items.DARK_OAK_STAIRS, Items.DARK_OAK_SLAB,
                                                                Items.DARK_OAK_FENCE, Items.DARK_OAK_FENCE_GATE, Items.DARK_OAK_BOAT,
                                                                Items.DARK_OAK_SAPLING, Items.DARK_OAK_LEAVES,
                                                                Items.DARK_OAK_PRESSURE_PLATE, Items.DARK_OAK_TRAPDOOR))
                          .put(WoodType.OAK, new WoodItems("oak", Items.OAK_PLANKS, Items.OAK_LOG, Items.STRIPPED_OAK_LOG,
                                                           Items.STRIPPED_OAK_WOOD, Items.OAK_WOOD, Items.OAK_SIGN, Items.OAK_DOOR,
                                                           Items.OAK_BUTTON, Items.OAK_STAIRS, Items.OAK_SLAB, Items.OAK_FENCE,
                                                           Items.OAK_FENCE_GATE, Items.OAK_BOAT, Items.OAK_SAPLING, Items.OAK_LEAVES,
                                                           Items.OAK_PRESSURE_PLATE, Items.OAK_TRAPDOOR))
                          .put(WoodType.JUNGLE, new WoodItems("jungle", Items.JUNGLE_PLANKS, Items.JUNGLE_LOG, Items.STRIPPED_JUNGLE_LOG,
                                                              Items.STRIPPED_JUNGLE_WOOD, Items.JUNGLE_WOOD, Items.JUNGLE_SIGN,
                                                              Items.JUNGLE_DOOR, Items.JUNGLE_BUTTON, Items.JUNGLE_STAIRS,
                                                              Items.JUNGLE_SLAB, Items.JUNGLE_FENCE, Items.JUNGLE_FENCE_GATE,
                                                              Items.JUNGLE_BOAT, Items.JUNGLE_SAPLING, Items.JUNGLE_LEAVES,
                                                              Items.JUNGLE_PRESSURE_PLATE, Items.JUNGLE_TRAPDOOR))
                          .put(WoodType.SPRUCE, new WoodItems("spruce", Items.SPRUCE_PLANKS, Items.SPRUCE_LOG, Items.STRIPPED_SPRUCE_LOG,
                                                              Items.STRIPPED_SPRUCE_WOOD, Items.SPRUCE_WOOD, Items.SPRUCE_SIGN,
                                                              Items.SPRUCE_DOOR, Items.SPRUCE_BUTTON, Items.SPRUCE_STAIRS,
                                                              Items.SPRUCE_SLAB, Items.SPRUCE_FENCE, Items.SPRUCE_FENCE_GATE,
                                                              Items.SPRUCE_BOAT, Items.SPRUCE_SAPLING, Items.SPRUCE_LEAVES,
                                                              Items.SPRUCE_PRESSURE_PLATE, Items.SPRUCE_TRAPDOOR))
                          .put(WoodType.WARPED, new WoodItems("warped", Items.WARPED_PLANKS, Items.WARPED_ROOTS, Items.STRIPPED_WARPED_STEM,
                                                              Items.STRIPPED_WARPED_HYPHAE, Items.WARPED_HYPHAE, Items.WARPED_SIGN,
                                                              Items.WARPED_DOOR, Items.WARPED_BUTTON, Items.WARPED_STAIRS,
                                                              Items.WARPED_SLAB, Items.WARPED_FENCE, Items.WARPED_FENCE_GATE, null,
                                                              Items.WARPED_FUNGUS, Items.NETHER_WART_BLOCK, Items.WARPED_PRESSURE_PLATE,
                                                              Items.WARPED_TRAPDOOR))
                          .build();
    
    private ItemUtil() {
    }

    /* Logs:

        ACACIA
        BIRCH
        CRIMSON
        DARK_OAK
        OAK
        JUNGLE
        SPRUCE
        WARPED
     */

    /* Colors:

        WHITE
        BLACK
        BLUE
        BROWN
        CYAN
        GRAY
        GREEN
        LIGHT_BLUE
        LIGHT_GRAY
        LIME
        MAGENTA
        ORANGE
        PINK
        PURPLE
        RED
        YELLOW
     */
    
    public static Item logToPlanks(Item logItem) {
        if (_logToPlanks.containsKey(logItem)) {
            return _logToPlanks.get(logItem);
        }
        return null;
    }
    
    public static Item planksToLog(Item plankItem) {
        return logToPlanks(plankItem);
    }
    
    public static ColorfulItems getColorfulItems(MaterialColor color) {
        return colorMap.get(color);
    }
    
    public static ColorfulItems getColorfulItems(DyeColor color) {
        return getColorfulItems(color.getMaterialColor());
    }
    
    public static WoodItems getWoodItems(WoodType type) {
        return woodMap.get(type);
    }
    
    public static String trimItemName(String name) {
        String trimmed = name;
        if (name.startsWith("block.minecraft.")) {
            trimmed = name.substring("block.minecraft.".length());
        } else if (name.startsWith("item.minecraft.")) {
            trimmed = name.substring("item.minecraft.".length());
        }
        return trimmed;
    }
    
    public static Block[] itemsToBlocks(Item... items) {
        Block[] result = new Block[items.length];
        for (int i = 0; i < items.length; ++i) {
            result[i] = Block.getBlockFromItem(items[i]);
        }
        return result;
    }
    
    public static class ColorfulItems {
        public DyeColor color;
        public String colorName;
        public Item dye;
        public Item wool;
        public Item bed;
        public Item carpet;
        public Item stainedGlass;
        public Item stainedGlassPane;
        public Item terracotta;
        public Item glazedTerracotta;
        public Item concrete;
        public Item concretePowder;
        public Item banner;
        public Item shulker;
        public Block wallBanner;
        
        public ColorfulItems(DyeColor color, String colorName, Item dye, Item wool, Item bed, Item carpet, Item stainedGlass,
                             Item stainedGlassPane, Item terracotta, Item glazedTerracotta, Item concrete, Item concretePowder, Item banner,
                             Item shulker, Block wallBanner) {
            this.color = color;
            this.colorName = colorName;
            this.dye = dye;
            this.wool = wool;
            this.bed = bed;
            this.carpet = carpet;
            this.stainedGlass = stainedGlass;
            this.stainedGlassPane = stainedGlassPane;
            this.terracotta = terracotta;
            this.glazedTerracotta = glazedTerracotta;
            this.concrete = concrete;
            this.concretePowder = concretePowder;
            this.banner = banner;
            this.shulker = shulker;
            this.wallBanner = wallBanner;
        }
    }
    
    
    public static class WoodItems {
        public String prefix;
        public Item planks;
        public Item log;
        public Item strippedLog;
        public Item strippedWood;
        public Item wood;
        public Item sign;
        public Item door;
        public Item button;
        public Item stairs;
        public Item slab;
        public Item fence;
        public Item fenceGate;
        public Item boat;
        public Item sapling;
        public Item leaves;
        public Item pressurePlate;
        public Item trapdoor;
        
        public WoodItems(String prefix, Item planks, Item log, Item strippedLog, Item strippedWood, Item wood, Item sign, Item door,
                         Item button, Item stairs, Item slab, Item fence, Item fenceGate, Item boat, Item sapling, Item leaves,
                         Item pressurePlate, Item trapdoor) {
            this.prefix = prefix;
            this.planks = planks;
            this.log = log;
            this.strippedLog = strippedLog;
            this.strippedWood = strippedWood;
            this.wood = wood;
            this.sign = sign;
            this.door = door;
            this.button = button;
            this.stairs = stairs;
            this.slab = slab;
            this.fence = fence;
            this.fenceGate = fenceGate;
            this.boat = boat;
            this.sapling = sapling;
            this.leaves = leaves;
            this.pressurePlate = pressurePlate;
            this.trapdoor = trapdoor;
        }
        
        public boolean isNetherWood() {
            return planks == Items.CRIMSON_PLANKS || planks == Items.WARPED_PLANKS;
        }
    }
    
}

package adris.altoclef;


import adris.altoclef.tasks.CataloguedResourceTask;
import adris.altoclef.tasks.CollectFlintTask;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.CraftInTableTask;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.SmeltInFurnaceTask;
import adris.altoclef.tasks.misc.speedrun.CollectBlazeRodsTask;
import adris.altoclef.tasks.resources.CarveThenCollectTask;
import adris.altoclef.tasks.resources.CollectBedTask;
import adris.altoclef.tasks.resources.CollectBucketLiquidTask;
import adris.altoclef.tasks.resources.CollectCobblestoneTask;
import adris.altoclef.tasks.resources.CollectCocoaBeansTask;
import adris.altoclef.tasks.resources.CollectCropTask;
import adris.altoclef.tasks.resources.CollectEggsTask;
import adris.altoclef.tasks.resources.CollectFlowerTask;
import adris.altoclef.tasks.resources.CollectGoldIngotTask;
import adris.altoclef.tasks.resources.CollectGoldNuggetsTask;
import adris.altoclef.tasks.resources.CollectHayBlockTask;
import adris.altoclef.tasks.resources.CollectMagmaCreamTask;
import adris.altoclef.tasks.resources.CollectMilkTask;
import adris.altoclef.tasks.resources.CollectNetherBricksTask;
import adris.altoclef.tasks.resources.CollectObsidianTask;
import adris.altoclef.tasks.resources.CollectPlanksTask;
import adris.altoclef.tasks.resources.CollectSandstoneTask;
import adris.altoclef.tasks.resources.CollectWheatSeedsTask;
import adris.altoclef.tasks.resources.CollectWheatTask;
import adris.altoclef.tasks.resources.CollectWoolTask;
import adris.altoclef.tasks.resources.KillAndLootTask;
import adris.altoclef.tasks.resources.ShearAndCollectBlockTask;
import adris.altoclef.tasks.resources.wood.CollectBoatTask;
import adris.altoclef.tasks.resources.wood.CollectFenceGateTask;
import adris.altoclef.tasks.resources.wood.CollectFenceTask;
import adris.altoclef.tasks.resources.wood.CollectSignTask;
import adris.altoclef.tasks.resources.wood.CollectWoodenDoorTask;
import adris.altoclef.tasks.resources.wood.CollectWoodenPressurePlateTask;
import adris.altoclef.tasks.resources.wood.CollectWoodenSlabTask;
import adris.altoclef.tasks.resources.wood.CollectWoodenStairsTask;
import adris.altoclef.tasks.resources.wood.CollectWoodenTrapDoorTask;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.ItemUtil;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.WoodType;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.block.MaterialColor;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SalmonEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.minecraft.block.Blocks.ANDESITE;
import static net.minecraft.block.Blocks.BEETROOTS;
import static net.minecraft.block.Blocks.BROWN_MUSHROOM;
import static net.minecraft.block.Blocks.BROWN_MUSHROOM_BLOCK;
import static net.minecraft.block.Blocks.CARROTS;
import static net.minecraft.block.Blocks.CARVED_PUMPKIN;
import static net.minecraft.block.Blocks.CLAY;
import static net.minecraft.block.Blocks.COAL_ORE;
import static net.minecraft.block.Blocks.COBWEB;
import static net.minecraft.block.Blocks.DIAMOND_ORE;
import static net.minecraft.block.Blocks.DIORITE;
import static net.minecraft.block.Blocks.DIRT;
import static net.minecraft.block.Blocks.EMERALD_ORE;
import static net.minecraft.block.Blocks.FERN;
import static net.minecraft.block.Blocks.GLOWSTONE;
import static net.minecraft.block.Blocks.GOLD_ORE;
import static net.minecraft.block.Blocks.GRANITE;
import static net.minecraft.block.Blocks.GRASS;
import static net.minecraft.block.Blocks.GRASS_BLOCK;
import static net.minecraft.block.Blocks.GRASS_PATH;
import static net.minecraft.block.Blocks.GRAVEL;
import static net.minecraft.block.Blocks.IRON_ORE;
import static net.minecraft.block.Blocks.LAPIS_ORE;
import static net.minecraft.block.Blocks.LARGE_FERN;
import static net.minecraft.block.Blocks.LILY_PAD;
import static net.minecraft.block.Blocks.MAGMA_BLOCK;
import static net.minecraft.block.Blocks.MELON;
import static net.minecraft.block.Blocks.NETHERRACK;
import static net.minecraft.block.Blocks.NETHER_QUARTZ_ORE;
import static net.minecraft.block.Blocks.OAK_LEAVES;
import static net.minecraft.block.Blocks.POTATOES;
import static net.minecraft.block.Blocks.PUMPKIN;
import static net.minecraft.block.Blocks.REDSTONE_ORE;
import static net.minecraft.block.Blocks.RED_MUSHROOM;
import static net.minecraft.block.Blocks.RED_MUSHROOM_BLOCK;
import static net.minecraft.block.Blocks.SAND;
import static net.minecraft.block.Blocks.TALL_GRASS;
import static net.minecraft.block.Blocks.VINE;
import static net.minecraft.item.Items.*;


@SuppressWarnings({ "rawtypes", "CodeBlock2Expr", "UnusedReturnValue" })
public class TaskCatalogue {

    private static final HashMap<String, Item[]> NAME_TO_ITEM_MAP = new HashMap<>();
    private static final HashMap<String, CataloguedResource> NAME_TO_RESOURCE_MAP = new HashMap<>();
    private static final HashSet<Item> OBTAINABLE_RESOURCES = new HashSet<>();

    static {
        /// DEFINE RESOURCE TASKS HERE
        // TODO: 2021-05-22 move resources to json file or yaml or some shit
        // TODO: 2021-05-22 I HATE static stuff
        String plan = "planks";
        String stic = "stick";

        /// RAW RESOURCES
        mine("log", MiningRequirement.HAND, ItemUtil.LOG, ItemUtil.LOG).anyDimension();
        woodTasks("log", wood -> wood.log, (wood, count) -> {
            return new MineAndCollectTask(wood.log, count, new Block[]{ Block.getBlockFromItem(wood.log) }, MiningRequirement.HAND);
        });
        mine("dirt", MiningRequirement.HAND, new Block[]{ DIRT, GRASS_BLOCK, GRASS_PATH }, Items.DIRT);
        simple("cobblestone", COBBLESTONE, CollectCobblestoneTask::new).dontMineIfPresent();
        mine("andesite", MiningRequirement.WOOD, ANDESITE, Items.ANDESITE);
        mine("granite", MiningRequirement.WOOD, GRANITE, Items.GRANITE);
        mine("diorite", MiningRequirement.WOOD, DIORITE, Items.DIORITE);
        mine("netherrack", MiningRequirement.WOOD, NETHERRACK, Items.NETHERRACK).forceDimension(Dimension.NETHER);
        mine("magma_block", MiningRequirement.WOOD, MAGMA_BLOCK, Items.MAGMA_BLOCK).forceDimension(Dimension.NETHER);
        mine("soul_sand", SOUL_SAND).forceDimension(Dimension.NETHER);
        mine("soul_soil", SOUL_SOIL).forceDimension(Dimension.NETHER);
        mine("glowstone_dust", GLOWSTONE, GLOWSTONE_DUST).forceDimension(Dimension.NETHER);
        mine("coal", MiningRequirement.WOOD, COAL_ORE, COAL);
        mine("iron_ore", MiningRequirement.STONE, IRON_ORE, Items.IRON_ORE);
        mine("gold_ore", MiningRequirement.IRON, GOLD_ORE, Items.GOLD_ORE);
        mine("diamond", MiningRequirement.IRON, DIAMOND_ORE, DIAMOND);
        mine("emerald", MiningRequirement.IRON, EMERALD_ORE, EMERALD);
        mine("redstone", MiningRequirement.IRON, REDSTONE_ORE, REDSTONE);
        mine("lapis_lazuli", MiningRequirement.IRON, LAPIS_ORE, LAPIS_LAZULI);
        alias("lapis", "lapis_lazuli");
        mine("sand", SAND, Items.SAND);
        mine("gravel", GRAVEL, Items.GRAVEL);
        mine("clay_ball", CLAY, CLAY_BALL);
        simple("sandstone", SANDSTONE, CollectSandstoneTask::new).dontMineIfPresent();
        simple("flint", FLINT, CollectFlintTask::new);
        simple("obsidian", OBSIDIAN, CollectObsidianTask::new).dontMineIfPresent();
        simple("wool", ItemUtil.WOOL, CollectWoolTask::new);
        simple("egg", EGG, CollectEggsTask::new);
        mob("bone", BONE, SkeletonEntity.class);
        mob("gunpowder", GUNPOWDER, CreeperEntity.class);
        mob("ender_pearl", ENDER_PEARL, EndermanEntity.class).anyDimension();
        mob("spider_eye", SPIDER_EYE, SpiderEntity.class);
        mob("leather", LEATHER, CowEntity.class);
        mob("feather", FEATHER, ChickenEntity.class);
        mob("rotten_flesh", ROTTEN_FLESH, ZombieEntity.class);
        mob("rabbit_foot", RABBIT_FOOT, RabbitEntity.class);
        mob("rabbit_hide", RABBIT_HIDE, RabbitEntity.class);
        mob("ender_pearl", ENDER_PEARL, EndermanEntity.class);
        mob("slime_ball", SLIME_BALL, SlimeEntity.class);
        mob("ink_sac", INK_SAC, SquidEntity.class); // Warning, this probably won't work.
        mob("string", STRING, SpiderEntity.class); // Warning, this probably won't work.
        mine("sugar_cane", SUGAR_CANE);
        mine("brown_mushroom", MiningRequirement.HAND, new Block[]{ BROWN_MUSHROOM, BROWN_MUSHROOM_BLOCK }, Items.BROWN_MUSHROOM);
        mine("red_mushroom", MiningRequirement.HAND, new Block[]{ RED_MUSHROOM, RED_MUSHROOM_BLOCK }, Items.RED_MUSHROOM);
        mine("mushroom", MiningRequirement.HAND, new Block[]{ BROWN_MUSHROOM, BROWN_MUSHROOM_BLOCK, RED_MUSHROOM, RED_MUSHROOM_BLOCK },
             Items.BROWN_MUSHROOM, Items.RED_MUSHROOM);
        mine("melon_slice", MiningRequirement.HAND, MELON, MELON_SLICE);
        mine("pumpkin", MiningRequirement.HAND, PUMPKIN, Items.PUMPKIN);
        simple("blaze_rod", BLAZE_ROD, CollectBlazeRodsTask::new).forceDimension(Dimension.NETHER); // Not super simple tbh lmao
        //simple("quartz", Items.QUARTZ, CollectQuartzTask::new);
        mine("quartz", MiningRequirement.WOOD, NETHER_QUARTZ_ORE, QUARTZ).forceDimension(Dimension.NETHER);
        simple("cocoa_beans", COCOA_BEANS, CollectCocoaBeansTask::new);
        shear("cobweb", COBWEB, Items.COBWEB).dontMineIfPresent();
        colorfulTasks("wool", color -> color.wool, (color, count) -> new CollectWoolTask(color.color, count));
        // Misc greenery
        shear("leaves", Util.itemsToBlocks(ItemUtil.LEAVES), ItemUtil.LEAVES).dontMineIfPresent();
        woodTasks("leaves", woodItems -> woodItems.leaves,
                  (woodItems, count) -> new ShearAndCollectBlockTask(woodItems.leaves, count, Block.getBlockFromItem(woodItems.leaves)));
        shear("vine", VINE, Items.VINE).dontMineIfPresent();
        shear("grass", GRASS, Items.GRASS).dontMineIfPresent();
        shear("lily_pad", LILY_PAD, Items.LILY_PAD).dontMineIfPresent();
        shear("tall_grass", TALL_GRASS, Items.TALL_GRASS).dontMineIfPresent();
        shear("fern", FERN, Items.FERN).dontMineIfPresent();
        shear("large_fern", LARGE_FERN, Items.LARGE_FERN).dontMineIfPresent();
        // Flowers
        simple("flower", ItemUtil.FLOWER, CollectFlowerTask::new);
        mine("allium", ALLIUM);
        mine("azure_bluet", AZURE_BLUET);
        mine("blue_orchid", BLUE_ORCHID);
        mine("cactus", CACTUS);
        mine("cornflower", CORNFLOWER);
        mine("dandelion", DANDELION);
        mine("lilac", LILAC);
        mine("lily_of_the_valley", LILY_OF_THE_VALLEY);
        mine("orange_tulip", ORANGE_TULIP);
        mine("oxeye_daisy", OXEYE_DAISY);
        mine("pink_tulip", PINK_TULIP);
        mine("poppy", POPPY);
        mine("peony", PEONY);
        mine("red_tulip", RED_TULIP);
        mine("rose_bush", ROSE_BUSH);
        mine("sunflower", SUNFLOWER);
        mine("white_tulip", WHITE_TULIP);
        // Crops
        simple("wheat", WHEAT, CollectWheatTask::new);
        crop("carrot", CARROT, CARROTS, CARROT);
        crop("potato", POTATO, POTATOES, POTATO);
        crop("poisonous_potato", POISONOUS_POTATO, POTATOES, POTATO);
        crop("beetroot", BEETROOT, BEETROOTS, BEETROOT_SEEDS);
        simple("wheat_seeds", WHEAT_SEEDS, CollectWheatSeedsTask::new);
        crop("beetroot_seeds", BEETROOT_SEEDS, BEETROOTS, BEETROOT_SEEDS);


        // MATERIALS
        simple("planks", ItemUtil.PLANKS, CollectPlanksTask::new).dontMineIfPresent();
        for (CataloguedResource woodCatalogue : woodTasks("planks", wood -> wood.planks,
                                                          (wood, count) -> new CollectPlanksTask(wood.planks, count), true)) {
            // Don't mine individual planks either!! Handled internally.
            woodCatalogue.dontMineIfPresent();
        }
        shapedRecipe2x2("stick", STICK, 4, plan, null, plan, null);
        smelt("stone", STONE, "cobblestone").dontMineIfPresent();
        smelt("smooth_stone", SMOOTH_STONE, "stone");
        smelt("glass", GLASS, "sand").dontMineIfPresent();
        smelt("iron_ingot", IRON_INGOT, "iron_ore");
        smelt("charcoal", CHARCOAL, "log");
        smelt("brick", BRICK, "clay_ball");
        smelt("nether_brick", NETHER_BRICK, "netherrack");
        smelt("green_dye", GREEN_DYE, "cactus");
        simple("gold_ingot", GOLD_INGOT, CollectGoldIngotTask::new).anyDimension(); // accounts for nether too
        shapedRecipe3x3Block("iron_block", IRON_BLOCK, "iron_ingot");
        shapedRecipe3x3Block("gold_block", GOLD_BLOCK, "gold_ingot");
        shapedRecipe3x3Block("diamond_block", DIAMOND_BLOCK, "diamond");
        shapedRecipe3x3Block("redstone_block", REDSTONE_BLOCK, "redstone");
        shapedRecipe3x3Block("coal_block", COAL_BLOCK, "coal");
        shapedRecipe3x3Block("emerald_block", EMERALD_BLOCK, "emerald");
        shapedRecipe3x3Block("slime_block", SLIME_BLOCK, "slime_ball");
        shapedRecipe2x2("glowstone", Items.GLOWSTONE, 1, "glowstone_dust", "glowstone_dust", "glowstone_dust",
                        "glowstone_dust").dontMineIfPresent();
        simple("gold_nugget", GOLD_NUGGET, CollectGoldNuggetsTask::new);
        {
            String g = "gold_nugget";
            shapedRecipe3x3("glistering_melon_slice", GLISTERING_MELON_SLICE, 1, g, g, g, g, "melon_slice", g, g, g, g);
        }
        shapedRecipe2x2("sugar", SUGAR, 1, "sugar_cane", null, null, null);
        shapedRecipe2x2("bone_meal", BONE_MEAL, 3, "bone", null, null, null);
        simple("hay_block", HAY_BLOCK, CollectHayBlockTask::new).dontMineIfPresent();
        shapedRecipe2x2("polished_andesite", POLISHED_ANDESITE, 4, "andesite", "andesite", "andesite", "andesite");
        shapedRecipe2x2("polished_diorite", POLISHED_DIORITE, 4, "diorite", "diorite", "diorite", "diorite");
        shapedRecipe2x2("polished_granite", POLISHED_GRANITE, 4, "granite", "granite", "granite", "granite");
        shapedRecipe2x2("cut_sandstone", CUT_SANDSTONE, 4, "sandstone", "sandstone", "sandstone", "sandstone");
        shapedRecipe2x2("stone_bricks", STONE_BRICKS, 4, "stone", "stone", "stone", "stone");
        shapedRecipe2x2("mossy_stone_bricks", MOSSY_STONE_BRICKS, 1, "stone_bricks", "vine", null, null);
        simple("nether_bricks", NETHER_BRICKS, CollectNetherBricksTask::new).dontMineIfPresent();
        smelt("cracked_stone_bricks", CRACKED_STONE_BRICKS, "stone_bricks");
        smelt("cracked_nether_bricks", CRACKED_NETHER_BRICKS, "nether_bricks");
        smelt("smooth_sandstone", SMOOTH_SANDSTONE, "sandstone");
        {
            String B = "nether_bricks";
            String b = "nether_brick";
            shapedRecipe3x3("nether_brick_fence", NETHER_BRICK_FENCE, 6, null, null, null, B, b, B, B, b, B);
        }
        shapedRecipe3x3("paper", PAPER, 3, "sugar_cane", "sugar_cane", "sugar_cane", null, null, null, null, null, null);
        shapedRecipe2x2("book", BOOK, 1, "paper", "paper", "paper", "leather");
        shapedRecipe2x2("book_and_quill", WRITABLE_BOOK, 1, "book", "ink_sac", null, "feather");
        shapedRecipe3x3("bowl", BOWL, 4, plan, null, plan, null, plan, null, null, null, null);
        shapedRecipe2x2("blaze_powder", BLAZE_POWDER, 2, "blaze_rod", null, null, null);
        shapedRecipe2x2("ender_eye", ENDER_EYE, 1, "blaze_powder", "ender_pearl", null, null);
        alias("eye_of_ender", "ender_eye");
        shapedRecipe2x2("fermented_spider_eye", SPIDER_EYE, 1, "brown_mushroom", "sugar", null, "spider_eye");
        shapedRecipe3x3("fire_charge", FIRE_CHARGE, 3, null, "blaze_powder", null, null, "coal", null, null, "gunpowder", null);
        simple("magma_cream", MAGMA_CREAM, CollectMagmaCreamTask::new);
        // DYES
        smelt("green_dye", GREEN_DYE, "cactus");
        // Slabs + Stairs + Walls
        shapedRecipeSlab("cobblestone_slab", COBBLESTONE_SLAB, "cobblestone");
        shapedRecipeStairs("cobblestone_stairs", COBBLESTONE_STAIRS, "cobblestone");
        shapedRecipeWall("cobblestone_wall", COBBLESTONE_WALL, "cobblestone");
        shapedRecipeSlab("stone_slab", STONE_SLAB, "stone");
        shapedRecipeStairs("stone_stairs", STONE_STAIRS, "stone");
        shapedRecipeSlab("smooth_stone_slab", SMOOTH_STONE_SLAB, "smooth_stone");
        shapedRecipeSlab("stone_brick_slab", STONE_BRICK_SLAB, "stone_bricks");
        shapedRecipeStairs("stone_brick_stairs", STONE_BRICK_STAIRS, "stone_bricks");
        shapedRecipeWall("stone_brick_wall", STONE_BRICK_WALL, "stone_bricks");
        shapedRecipeSlab("mossy_stone_brick_slab", MOSSY_STONE_BRICK_SLAB, "mossy_stone_bricks");
        shapedRecipeStairs("mossy_stone_brick_stairs", MOSSY_STONE_BRICK_STAIRS, "mossy_stone_bricks");
        shapedRecipeWall("mossy_stone_brick_wall", MOSSY_STONE_BRICK_WALL, "mossy_stone_bricks");
        shapedRecipeSlab("andesite_slab", ANDESITE_SLAB, "andesite");
        shapedRecipeStairs("andesite_stairs", ANDESITE_STAIRS, "andesite");
        shapedRecipeWall("andesite_wall", ANDESITE_WALL, "andesite");
        shapedRecipeSlab("granite_slab", GRANITE_SLAB, "granite");
        shapedRecipeStairs("granite_stairs", GRANITE_STAIRS, "granite");
        shapedRecipeWall("granite_wall", GRANITE_WALL, "granite");
        shapedRecipeSlab("diorite_slab", DIORITE_SLAB, "diorite");
        shapedRecipeStairs("diorite_stairs", DIORITE_STAIRS, "diorite");
        shapedRecipeWall("diorite_wall", DIORITE_WALL, "diorite");
        shapedRecipeSlab("polished_andesite_slab", POLISHED_ANDESITE_SLAB, "polished_andesite");
        shapedRecipeStairs("polished_andesite_stairs", POLISHED_ANDESITE_STAIRS, "polished_andesite");
        shapedRecipeSlab("polished_granite_slab", POLISHED_GRANITE_SLAB, "polished_granite");
        shapedRecipeStairs("polished_granite_stairs", POLISHED_GRANITE_STAIRS, "polished_granite");
        shapedRecipeSlab("polished_diorite_slab", POLISHED_DIORITE_SLAB, "polished_diorite");
        shapedRecipeStairs("polished_diorite_stairs", POLISHED_DIORITE_STAIRS, "polished_diorite");
        shapedRecipeSlab("sandstone_slab", SANDSTONE_SLAB, "sandstone");
        shapedRecipeStairs("sandstone_stairs", SANDSTONE_STAIRS, "sandstone");
        shapedRecipeWall("sandstone_wall", SANDSTONE_WALL, "sandstone");
        shapedRecipeSlab("cut_sandstone_slab", CUT_SANDSTONE_SLAB, "cut_sandstone");
        shapedRecipeSlab("smooth_sandstone_slab", SMOOTH_SANDSTONE_SLAB, "smooth_sandstone");
        shapedRecipeStairs("smooth_sandstone_stairs", SMOOTH_SANDSTONE_STAIRS, "smooth_sandstone");
        shapedRecipeSlab("nether_brick_slab", NETHER_BRICK_SLAB, "nether_bricks");
        shapedRecipeStairs("nether_brick_stairs", NETHER_BRICK_STAIRS, "nether_bricks");
        shapedRecipeWall("nether_brick_wall", NETHER_BRICK_WALL, "nether_bricks");
        shapedRecipe2x2("chiseled_sandstone", CHISELED_SANDSTONE, 1, "sandstone_slab", "sandstone_slab", null, null);
        shapedRecipe2x2("chiseled_stone_bricks", CHISELED_STONE_BRICKS, 1, "stone_brick_slab", null, "stone_brick_slab", null);
        shapedRecipe2x2("chiseled_nether_bricks", CHISELED_NETHER_BRICKS, 1, "nether_brick_slab", null, "nether_brick_slab", null);


        /// TOOLS
        tools("wooden", "planks", WOODEN_PICKAXE, WOODEN_SHOVEL, WOODEN_SWORD, WOODEN_AXE, WOODEN_HOE);
        tools("stone", "cobblestone", STONE_PICKAXE, STONE_SHOVEL, STONE_SWORD, STONE_AXE, STONE_HOE);
        tools("iron", "iron_ingot", IRON_PICKAXE, IRON_SHOVEL, IRON_SWORD, IRON_AXE, IRON_HOE);
        tools("golden", "gold_ingot", GOLDEN_PICKAXE, GOLDEN_SHOVEL, GOLDEN_SWORD, GOLDEN_AXE, GOLDEN_HOE);
        tools("diamond", "diamond", DIAMOND_PICKAXE, DIAMOND_SHOVEL, DIAMOND_SWORD, DIAMOND_AXE, DIAMOND_HOE);
        armor("leather", "leather", LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS);
        armor("iron", "iron_ingot", IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS);
        armor("golden", "gold_ingot", GOLDEN_HELMET, GOLDEN_CHESTPLATE, GOLDEN_LEGGINGS, GOLDEN_BOOTS);
        armor("diamond", "diamond", DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS);
        shapedRecipe3x3("bow", BOW, 1, "string", stic, null, "string", null, stic, "string", stic, null);
        shapedRecipe3x3("arrow", ARROW, 4, "flint", null, null, stic, null, null, "feather", null, null);
        {
            String i = "iron_ingot";
            shapedRecipe3x3("bucket", BUCKET, 1, i, null, i, null, i, null, null, null, null);
            shapedRecipe2x2("flint_and_steel", FLINT_AND_STEEL, 1, i, null, null, "flint");
            shapedRecipe2x2("shears", SHEARS, 1, i, null, null, i);
            shapedRecipe3x3("compass", COMPASS, 1, null, i, null, i, "redstone", i, null, i, null);
            shapedRecipe3x3("shield", SHEARS, 1, plan, i, plan, plan, plan, plan, null, plan, null);
            String g = "gold_ingot";
            shapedRecipe3x3("clock", CLOCK, 1, null, g, null, g, "redstone", g, null, g, null);
        }
        simple("water_bucket", WATER_BUCKET, CollectBucketLiquidTask.CollectWaterBucketTask::new);
        simple("lava_bucket", LAVA_BUCKET, CollectBucketLiquidTask.CollectLavaBucketTask::new);
        {
            String a = "paper";
            shapedRecipe3x3("map", MAP, 1, a, a, a, a, "compass", a, a, a, a);
        }
        shapedRecipe3x3("fishing_rod", FISHING_ROD, 1, null, null, stic, null, stic, "string", stic, null, "string");
        shapedRecipe2x2("carrot_on_a_stick", CARROT_ON_A_STICK, 1, "fishing_rod", "carrot", null, null);
        shapedRecipe3x3("glass_bottle", GLASS_BOTTLE, 3, "glass", null, "glass", null, "glass", null, null, null, null);
        alias("wooden_pick", "wooden_pickaxe");
        alias("stone_pick", "stone_pickaxe");
        alias("iron_pick", "iron_pickaxe");
        alias("gold_pick", "gold_pickaxe");
        alias("diamond_pick", "diamond_pickaxe");
        simple("boat", ItemUtil.WOOD_BOAT, CollectBoatTask::new);
        woodTasks("boat", woodItems -> woodItems.boat,
                  (woodItems, count) -> new CollectBoatTask(woodItems.boat, woodItems.prefix + "_planks", count));
        shapedRecipe3x3("lead", LEAD, 1, "string", "string", null, "string", "slime_ball", null, null, null, "string");


        // FURNITURE
        shapedRecipe2x2("crafting_table", CRAFTING_TABLE, 1, plan, plan, plan, plan).dontMineIfPresent();
        simple("wooden_pressure_plate", ItemUtil.WOOD_SIGN, CollectWoodenPressurePlateTask::new);
        woodTasks("pressure_plate", woodItems -> woodItems.pressurePlate,
                  (woodItems, count) -> new CollectWoodenPressurePlateTask(woodItems.pressurePlate, woodItems.prefix + "_planks", count));
        shapedRecipe2x2("wooden_button", ItemUtil.WOOD_BUTTON, 1, plan, null, null, null);
        woodTasks("button", woodItems -> woodItems.button,
                  (woodItems, count) -> new CraftInInventoryTask(new ItemTarget(woodItems.button, 1),
                                                                 CraftingRecipe.newShapedRecipe(woodItems.prefix + "_button",
                                                                                                new ItemTarget[]{
                                                                                                        new ItemTarget(woodItems.planks, 1),
                                                                                                        null, null, null
                                                                                                }, 1)));
        shapedRecipe2x2("stone_pressure_plate", STONE_PRESSURE_PLATE, 1, null, null, "stone", "stone");
        shapedRecipe2x2("stone_button", STONE_BUTTON, 1, "stone", null, null, null);
        simple("sign", ItemUtil.WOOD_SIGN, CollectSignTask::new).dontMineIfPresent(); // By default, we save signs round these parts.
        woodTasks("sign", woodItems -> woodItems.sign,
                  (woodItems, count) -> new CollectSignTask(woodItems.sign, woodItems.prefix + "_planks", count));
        {
            String c = "cobblestone";
            shapedRecipe3x3("furnace", FURNACE, 1, c, c, c, c, null, c, c, c, c).dontMineIfPresent();
            shapedRecipe3x3("dropper", DISPENSER, 1, c, c, c, c, null, c, c, "redstone", c);
            shapedRecipe3x3("dispenser", DISPENSER, 1, c, c, c, c, "bow", c, c, "redstone", c);
            shapedRecipe3x3("brewing_stand", BREWING_STAND, 1, null, null, null, null, "blaze_rod", null, c, c, c);
            shapedRecipe3x3("piston", PISTON, 1, plan, plan, plan, c, "iron_ingot", c, c, "redstone", c);
            shapedRecipe3x3("observer", OBSERVER, 1, c, c, c, "redstone", "redstone", "quartz", c, c, c);
            shapedRecipe2x2("lever", LEVER, 1, stic, null, c, null);
        }
        shapedRecipe3x3("chest", CHEST, 1, plan, plan, plan, plan, null, plan, plan, plan, plan).dontMineIfPresent();
        shapedRecipe2x2("torch", TORCH, 4, "coal", null, stic, null);
        simple("bed", ItemUtil.BED, CollectBedTask::new);
        colorfulTasks("bed", colors -> colors.bed, (colors, count) -> new CollectBedTask(colors.bed, colors.colorName + "_wool", count));
        {
            String i = "iron_ingot";
            String b = "iron_block";
            shapedRecipe3x3("anvil", ANVIL, 1, b, b, b, null, i, null, i, i, i);
            shapedRecipe3x3("cauldron", CAULDRON, 1, i, null, i, i, null, i, i, i, i);
            shapedRecipe3x3("minecart", MINECART, 1, null, null, null, i, null, i, i, i, i);
            shapedRecipe3x3("iron_door", IRON_DOOR, 3, i, i, null, i, i, null, i, i, null);
            shapedRecipe3x3("iron_bars", IRON_BARS, 16, i, i, i, i, i, i, null, null, null);
            shapedRecipe2x2("iron_trapdoor", IRON_TRAPDOOR, 1, i, i, i, i);
        }
        shapedRecipe3x3("armor_stand", ARMOR_STAND, 1, stic, stic, stic, null, stic, null, stic, "smooth_stone_slab", stic);
        {
            String b = "obsidian";
            shapedRecipe3x3("enchanting_table", ENCHANTING_TABLE, 1, null, "book", null, "diamond", b, "diamond", b, b, b);
            shapedRecipe3x3("ender_chest", ENDER_CHEST, 1, null, null, null, null, "ender_eye", null, null, null, null).dontMineIfPresent();
        }
        {
            String b = "brick";
            shapedRecipe3x3("flower_pot", FLOWER_POT, 1, b, null, b, null, b, null, null, null, null);
            shapedRecipe2x2("bricks", BRICKS, 1, b, b, b, b);
            shapedRecipeSlab("brick_slab", BRICK_SLAB, b);
            shapedRecipeStairs("brick_stairs", BRICK_STAIRS, b);
            shapedRecipeStairs("brick_wall", BRICK_WALL, "brick");
        }
        shapedRecipe3x3("ladder", LADDER, 3, stic, null, stic, stic, stic, stic, stic, null, stic);
        shapedRecipe3x3("jukebox", JUKEBOX, 1, plan, plan, plan, plan, "diamond", plan, plan, plan, plan);
        shapedRecipe3x3("note_block", NOTE_BLOCK, 1, plan, plan, plan, plan, "redstone", plan, plan, plan, plan);
        shapedRecipe3x3("bookshelf", BOOKSHELF, 1, plan, plan, plan, "book", "book", "book", plan, plan, plan);
        {
            String g = "glass";
            shapedRecipe3x3("glass_pane", GLASS_PANE, 16, g, g, g, g, g, g, null, null, null).dontMineIfPresent();
        }
        simple("carved_pumpkin", Items.CARVED_PUMPKIN, count -> {
            return new CarveThenCollectTask(Items.CARVED_PUMPKIN, count, CARVED_PUMPKIN, Items.PUMPKIN, PUMPKIN, SHEARS);
        });
        shapedRecipe2x2("jack_o_lantern", JACK_O_LANTERN, 1, "carved_pumpkin", null, "torch", null);
        shapedRecipe3x3("target", TARGET, 1, null, "redstone", null, "redstone", "hay_block", "redstone", null, "redstone", null);

        // A BUNCH OF WOODEN STUFF
        simple("wooden_stairs", ItemUtil.WOOD_STAIRS, CollectWoodenStairsTask::new);
        woodTasks("stairs", woodItems -> woodItems.stairs, (woodItems, count) -> {
            return new CollectWoodenStairsTask(woodItems.stairs, woodItems.prefix + "_planks", count);
        });
        simple("wooden_slab", ItemUtil.WOOD_SLAB, CollectWoodenSlabTask::new);
        woodTasks("slab", woodItems -> woodItems.slab, (woodItems, count) -> {
            return new CollectWoodenSlabTask(woodItems.slab, woodItems.prefix + "_planks", count);
        });
        simple("wooden_door", ItemUtil.WOOD_DOOR, CollectWoodenDoorTask::new);
        woodTasks("door", woodItems -> woodItems.door, (woodItems, count) -> {
            return new CollectWoodenDoorTask(woodItems.door, woodItems.prefix + "_planks", count);
        });
        simple("wooden_trapdoor", ItemUtil.WOOD_TRAPDOOR, CollectWoodenTrapDoorTask::new);
        woodTasks("trapdoor", woodItems -> woodItems.trapdoor, (woodItems, count) -> {
            return new CollectWoodenTrapDoorTask(woodItems.trapdoor, woodItems.prefix + "_planks", count);
        });
        simple("wooden_fence", ItemUtil.WOOD_FENCE, CollectFenceTask::new);
        woodTasks("fence", woodItems -> woodItems.fence, (woodItems, count) -> {
            return new CollectFenceTask(woodItems.fence, woodItems.prefix + "_planks", count);
        });
        simple("wooden_fence_gate", ItemUtil.WOOD_FENCE_GATE, CollectFenceGateTask::new);
        woodTasks("fence_gate", woodItems -> woodItems.fenceGate, (woodItems, count) -> {
            return new CollectFenceGateTask(woodItems.fenceGate, woodItems.prefix + "_planks", count);
        });
        {
            String r = "wooden_slab";
            shapedRecipe3x3("lectern", LECTERN, 1, r, r, r, null, "bookshelf", null, null, r, null);
        }            // Most people will always think "wooden door" when they say "door".
        alias("door", "wooden_door");
        alias("trapdoor", "wooden_trapdoor");
        alias("fence", "wooden_fence");
        alias("fence_gate", "wooden_fence_gate");

        shapedRecipe2x2("stone_pressure_plate", STONE_PRESSURE_PLATE, 1, "stone", "stone", null, null);
        shapedRecipe2x2("heavy_weighted_pressure_plate", HEAVY_WEIGHTED_PRESSURE_PLATE, 1, "iron_ingot", "iron_ingot", null, null);
        shapedRecipe2x2("light_weighted_pressure_plate", LIGHT_WEIGHTED_PRESSURE_PLATE, 1, "gold_ingot", "gold_ingot", null, null);

        shapedRecipe3x3("daylight_detector", DAYLIGHT_DETECTOR, 1, "glass", "glass", "glass", "quartz", "quartz", "quartz",
                        "wooden_slab", "wooden_slab", "wooden_slab");
        shapedRecipe3x3("tripwire_hook", TRIPWIRE_HOOK, 2, "iron_ingot", null, null, "stick", null, null, "planks", null, null);
        shapedRecipe2x2("trapped_chest", TRAPPED_CHEST, 1, "chest", "tripwire_hook", null, null);
        shapedRecipe3x3("crossbow", CROSSBOW, 1, stic, "iron_ingot", stic, "string", "tripwire_hook", "string", null, stic, null);
        {
            String t = "gunpowder";
            String n = "sand";
            shapedRecipe3x3("tnt", TNT, 1, t, n, t, n, t, n, t, n, t);
        }
        shapedRecipe2x2("sticky_piston", STICKY_PISTON, 1, "slime_ball", null, "piston", null);
        shapedRecipe2x2("redstone_torch", REDSTONE_TORCH, 1, "redstone", null, stic, null);
        shapedRecipe3x3("repeater", REPEATER, 1, "redstone_torch", "redstone", "redstone_torch", "stone", "stone", "stone", null, null,
                        null);
        shapedRecipe3x3("comparator", COMPARATOR, 1, null, "redstone_torch", null, "redstone_torch", "quartz", "redstone_torch", "stone",
                        "stone", "stone");
        {
            // Some rails
            String i = "iron_ingot";
            String g = "gold_ingot";
            shapedRecipe3x3("rail", RAIL, 16, i, null, i, i, stic, i, i, null, i);
            shapedRecipe3x3("powered_rail", POWERED_RAIL, 6, g, null, g, g, stic, g, g, "redstone", g);
            shapedRecipe3x3("detector_rail", DETECTOR_RAIL, 6, i, null, i, i, "stone_pressure_plate", i, i, "redstone", i);
            shapedRecipe3x3("activator_rail", ACTIVATOR_RAIL, 6, i, stic, i, i, "redstone_torch", i, i, stic, i);
            shapedRecipe3x3("hopper", HOPPER, 1, i, null, i, i, "chest", i, null, i, null);
        }
        shapedRecipe3x3("painting", PAINTING, 1, stic, stic, stic, stic, "wool", stic, stic, stic, stic);
        shapedRecipe3x3("item_frame", ITEM_FRAME, 1, stic, stic, stic, stic, "leather", stic, stic, stic, stic);
        shapedRecipe2x2("chest_minecart", CHEST_MINECART, 1, "chest", null, "minecart", null);
        shapedRecipe2x2("furnace_minecart", FURNACE_MINECART, 1, "furnace", null, "minecart", null);
        shapedRecipe2x2("hopper_minecart", HOPPER_MINECART, 1, "hopper", null, "minecart", null);
        shapedRecipe2x2("tnt_minecart", TNT_MINECART, 1, "tnt", null, "minecart", null);
        alias("minecart_with_chest", "chest_minecart");
        alias("minecart_with_furnace", "furnace_minecart");
        alias("minecart_with_hopper", "hopper_minecart");
        alias("minecart_with_tnt", "tnt_minecart");


        /// FOOD
        mobCook("porkchop", PORKCHOP, COOKED_PORKCHOP, PigEntity.class);
        mobCook("beef", BEEF, COOKED_BEEF, CowEntity.class);
        mobCook("chicken", CHICKEN, COOKED_CHICKEN, ChickenEntity.class);
        mobCook("mutton", MUTTON, COOKED_MUTTON, SheepEntity.class);
        mobCook("rabbit", RABBIT, COOKED_RABBIT, RabbitEntity.class);
        mobCook("salmon", SALMON, COOKED_SALMON, SalmonEntity.class);
        mobCook("cod", COD, COOKED_COD, CodEntity.class);
        simple("milk", MILK_BUCKET, CollectMilkTask::new);
        mine("apple", OAK_LEAVES, APPLE);
        smelt("baked_potato", BAKED_POTATO, "potato");
        shapedRecipe2x2("mushroom_stew", MUSHROOM_STEW, 1, "red_mushroom", "brown_mushroom", "bowl", null);
        shapedRecipe2x2("suspicious_stew", SUSPICIOUS_STEW, 1, "red_mushroom", "brown_mushroom", "bowl", "flower");
        shapedRecipe3x3("bread", BREAD, 1, "wheat", "wheat", "wheat", null, null, null, null, null, null);
        shapedRecipe3x3("cookie", COOKIE, 8, "wheat", "cocoa_beans", "wheat", null, null, null, null, null, null);
        shapedRecipe2x2("pumpkin_pie", PUMPKIN_PIE, 1, "pumpkin", "sugar", null, "egg");
        shapedRecipe3x3("cake", CAKE, 1, "milk", "milk", "milk", "sugar", "egg", "sugar", "wheat", "wheat", "wheat").dontMineIfPresent();
        {
            String g = "gold_nugget";
            shapedRecipe3x3("golden_carrot", GOLDEN_CARROT, 1, g, g, g, g, "carrot", g, g, g, g);
            String i = "gold_ingot";
            shapedRecipe3x3("golden_apple", GOLDEN_APPLE, 1, i, i, i, i, "apple", i, i, i, i);
        }
        shapedRecipe3x3("rabbit_stew", RABBIT_STEW, 1, null, "cooked_rabbit", null, "carrot", "baked_potato", "mushroom", null, "bowl",
                        null);
        {
            String b = "beetroot";
            shapedRecipe3x3("beetroot_soup", BEETROOT_SOUP, 1, b, b, b, b, b, b, null, "bowl", null);
        }
    }

    private static CataloguedResource put(String name, Item[] matches, Function<Integer, ResourceTask> getTask) {
        CataloguedResource result = new CataloguedResource(matches, getTask);
        Block[] blocks = Util.itemsToBlocks(matches);
        // DEFAULT BEHAVIOUR: Mine if present & assume overworld is required!
        if (blocks.length != 0) {
            result.mineIfPresent();
        }
        result.forceDimension(Dimension.OVERWORLD);
        NAME_TO_RESOURCE_MAP.put(name, result);
        NAME_TO_ITEM_MAP.put(name, matches);
        OBTAINABLE_RESOURCES.addAll(Arrays.asList(matches));
        return result;
    }

    /*private static void put(String name, Item match, TaskFactory factory) {
        put(name, new Item[]{match}, factory);
    }*/

    /*
    static ResourceTask getItemTask(Item item, int count) {
        return getItemTask(ItemTarget.trimItemName(item.getTranslationKey()), count);
    }
    */

    // This is here so that we can use strings for item targets (optionally) and stuff like that.
    public static Item[] getItemMatches(String name) {
        if (!NAME_TO_ITEM_MAP.containsKey(name)) {
            return null;
        }
        return NAME_TO_ITEM_MAP.get(name);
    }

    public static boolean isObtainable(Item item) {
        return OBTAINABLE_RESOURCES.contains(item);
    }

    public static ItemTarget getItemTarget(String name, int count) {
        return new ItemTarget(name, count);
    }

    public static CataloguedResourceTask getSquashedItemTask(ItemTarget... targets) {
        return new CataloguedResourceTask(true, targets);
    }

    public static ResourceTask getItemTask(String name, int count) {

        if (!taskExists(name)) {
            Debug.logWarning("Task " + name + " does not exist. Error possibly.");
            Debug.logStack();
            return null;
        }

        CataloguedResource catalogueValue = NAME_TO_RESOURCE_MAP.get(name);
        return catalogueValue.getResource(count);
    }

    public static ResourceTask getItemTask(ItemTarget target) {
        return getItemTask(target.getCatalogueName(), target.targetCount);
    }

    public static boolean taskExists(String name) {
        return NAME_TO_RESOURCE_MAP.containsKey(name);
    }

    public static Collection<String> resourceNames() {
        return NAME_TO_RESOURCE_MAP.keySet();
    }

    private static CataloguedResource simple(String name, Item[] matches, Function<Integer, ResourceTask> getTask) {
        return put(name, matches, getTask);
    }

    private static CataloguedResource simple(String name, Item matches, Function<Integer, ResourceTask> getTask) {
        return simple(name, new Item[]{ matches }, getTask);
    }

    private static CataloguedResource mine(String name, MiningRequirement requirement, Item[] toMine, Item... targets) {
        Block[] toMineBlocks = new Block[toMine.length];
        for (int i = 0; i < toMine.length; ++i)
            toMineBlocks[i] = Block.getBlockFromItem(toMine[i]);
        return mine(name, requirement, toMineBlocks, targets);
    }

    private static CataloguedResource mine(String name, MiningRequirement requirement, Block[] toMine, Item... targets) {
        return put(name, targets, count -> new MineAndCollectTask(new ItemTarget(targets, count), toMine,
                                                                  requirement)).dontMineIfPresent(); // Mining already taken care of!!
    }

    private static CataloguedResource mine(String name, MiningRequirement requirement, Block toMine, Item target) {
        return mine(name, requirement, new Block[]{ toMine }, target);
    }

    private static CataloguedResource mine(String name, Block toMine, Item target) {
        return mine(name, MiningRequirement.HAND, toMine, target);
    }

    private static CataloguedResource mine(String name, Item target) {
        return mine(name, Block.getBlockFromItem(target), target);
    }

    private static CataloguedResource shear(String name, Block[] toShear, Item... targets) {
        return put(name, targets,
                   count -> new ShearAndCollectBlockTask(new ItemTarget[]{ new ItemTarget(targets, count) }, toShear)).dontMineIfPresent();
    }

    private static CataloguedResource shear(String name, Block toShear, Item... targets) {
        return shear(name, new Block[]{ toShear }, targets);
    }

    private static CataloguedResource shapedRecipe2x2(String name, Item[] matches, int outputCount, String s0, String s1, String s2,
                                                      String s3) {
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(name, new ItemTarget[]{ t(s0), t(s1), t(s2), t(s3) }, outputCount);
        return put(name, matches, count -> new CraftInInventoryTask(new ItemTarget(matches, count), recipe));
    }

    private static CataloguedResource shapedRecipe3x3(String name, Item[] matches, int outputCount, String s0, String s1, String s2,
                                                      String s3, String s4, String s5, String s6, String s7, String s8) {
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(name, new ItemTarget[]{
                t(s0), t(s1), t(s2), t(s3), t(s4), t(s5), t(s6), t(s7), t(s8)
        }, outputCount);
        return put(name, matches, count -> new CraftInTableTask(new ItemTarget(matches, count), recipe));
    }

    private static CataloguedResource shapedRecipe2x2(String name, Item match, int craftCount, String s0, String s1, String s2, String s3) {
        return shapedRecipe2x2(name, new Item[]{ match }, craftCount, s0, s1, s2, s3);
    }

    private static CataloguedResource shapedRecipe3x3(String name, Item match, int craftCount, String s0, String s1, String s2, String s3,
                                                      String s4, String s5, String s6, String s7, String s8) {
        return shapedRecipe3x3(name, new Item[]{ match }, craftCount, s0, s1, s2, s3, s4, s5, s6, s7, s8);
    }

    private static CataloguedResource shapedRecipe3x3Block(String name, Item match, String material) {
        return shapedRecipe3x3(name, match, 1, material, material, material, material, material, material, material, material, material);
    }

    private static CataloguedResource shapedRecipeSlab(String name, Item match, String material) {
        return shapedRecipe3x3(name, match, 6, null, null, null, null, null, null, material, material, material);
    }

    private static CataloguedResource shapedRecipeStairs(String name, Item match, String material) {
        return shapedRecipe3x3(name, match, 4, material, null, null, material, material, null, material, material, material);
    }

    private static CataloguedResource shapedRecipeWall(String name, Item match, String material) {
        return shapedRecipe3x3(name, match, 6, material, material, material, material, material, material, null, null, null);
    }

    private static CataloguedResource smelt(String name, Item[] matches, String materials) {
        return put(name, matches,
                   count -> new SmeltInFurnaceTask(new SmeltTarget(new ItemTarget(matches, count), new ItemTarget(materials, count))));
    }

    private static CataloguedResource smelt(String name, Item match, String materials) {
        return smelt(name, new Item[]{ match }, materials);
    }

    private static CataloguedResource mob(String name, Item[] matches, Class mobClass) {
        return put(name, matches, count -> new KillAndLootTask(mobClass, new ItemTarget(matches, count)));
    }

    private static CataloguedResource mob(String name, Item match, Class mobClass) {
        return mob(name, new Item[]{ match }, mobClass);
    }

    private static void mobCook(String uncookedName, String cookedName, Item uncooked, Item cooked, Class mobClass) {
        mob(uncookedName, uncooked, mobClass);
        smelt(cookedName, cooked, uncookedName);
    }

    private static void mobCook(String uncookedName, Item uncooked, Item cooked, Class mobClass) {
        mobCook(uncookedName, "cooked_" + uncookedName, uncooked, cooked, mobClass);
    }

    private static CataloguedResource crop(String name, Item[] matches, Block[] cropBlocks, Item[] cropSeeds) {
        return put(name, matches, count -> new CollectCropTask(new ItemTarget(matches, count), cropBlocks, cropSeeds));
    }

    public static CataloguedResource crop(String name, Item match, Block cropBlock, Item cropSeed) {
        return crop(name, new Item[]{ match }, new Block[]{ cropBlock }, new Item[]{ cropSeed });
    }

    private static void colorfulTasks(String baseName, Function<? super ItemUtil.ColorfulItems, ? extends Item> getMatch,
                                      BiFunction<? super ItemUtil.ColorfulItems, ? super Integer, ? extends ResourceTask> getTask) {
        for (DyeColor dcol : DyeColor.values()) {
            MaterialColor mcol = dcol.getMaterialColor();
            ItemUtil.ColorfulItems color = ItemUtil.getColorfulItems(mcol);
            String prefix = color.colorName;
            put(prefix + "_" + baseName, new Item[]{ getMatch.apply(color) }, count -> getTask.apply(color, count));
        }
    }

    private static CataloguedResource[] woodTasks(String baseName, Function<? super ItemUtil.WoodItems, ? extends Item> getMatch,
                                                  BiFunction<? super ItemUtil.WoodItems, ? super Integer, ? extends ResourceTask> getTask,
                                                  boolean requireNetherForNetherStuff) {
        List<CataloguedResource> result = new ArrayList<>();
        for (WoodType woodType : WoodType.values()) {
            ItemUtil.WoodItems woodItems = ItemUtil.getWoodItems(woodType);
            String prefix = woodItems.prefix;
            CataloguedResource t = put(prefix + "_" + baseName, new Item[]{ getMatch.apply(woodItems) },
                                       count -> getTask.apply(woodItems, count));
            if (requireNetherForNetherStuff) {
                if (woodItems.isNetherWood()) {
                    t.forceDimension(Dimension.NETHER);
                }
            }
            result.add(t);
        }
        return Util.toArray(CataloguedResource.class, result);
    }

    private static CataloguedResource[] woodTasks(String baseName, Function<? super ItemUtil.WoodItems, ? extends Item> getMatch,
                                                  BiFunction<? super ItemUtil.WoodItems, ? super Integer, ? extends ResourceTask> getTask) {
        return woodTasks(baseName, getMatch, getTask, false);
    }

    private static void tools(String toolMaterialName, String material, Item pickaxeItem, Item shovelItem, Item swordItem, Item axeItem,
                              Item hoeItem) {
        String s = "stick";
        shapedRecipe3x3(toolMaterialName + "_pickaxe", pickaxeItem, 1, material, material, material, null, s, null, null, s, null);
        shapedRecipe3x3(toolMaterialName + "_shovel", shovelItem, 1, null, material, null, null, s, null, null, s, null);
        shapedRecipe3x3(toolMaterialName + "_sword", swordItem, 1, null, material, null, null, material, null, null, s, null);
        shapedRecipe3x3(toolMaterialName + "_axe", axeItem, 1, material, material, null, material, s, null, null, s, null);
        shapedRecipe3x3(toolMaterialName + "_hoe", hoeItem, 1, material, material, null, null, s, null, null, s, null);
    }

    private static void armor(String armorMaterialName, String material, Item helmetItem, Item chestplateItem, Item leggingsItem,
                              Item bootsItem) {
        shapedRecipe3x3(armorMaterialName + "_helmet", helmetItem, 1, material, material, material, material, null, material, null, null,
                        null);
        shapedRecipe3x3(armorMaterialName + "_chestplate", chestplateItem, 1, material, null, material, material, material, material,
                        material, material, material);
        shapedRecipe3x3(armorMaterialName + "_leggings", leggingsItem, 1, material, material, material, material, null, material, material,
                        null, material);
        shapedRecipe3x3(armorMaterialName + "_boots", bootsItem, 1, null, null, null, material, null, material, material, null, material);
    }

    private static void alias(String newName, String original) {
        NAME_TO_RESOURCE_MAP.put(newName, NAME_TO_RESOURCE_MAP.get(original));
        NAME_TO_ITEM_MAP.put(newName, NAME_TO_ITEM_MAP.get(original));
    }

    private static ItemTarget t(String cataloguedName) {
        return new ItemTarget(cataloguedName);
    }

    public static class CataloguedResource {
        private final Item[] targets;
        private final Function<? super Integer, ? extends ResourceTask> resourceFunction;

        private boolean shouldMine;
        private boolean shouldForceDimension;
        private Dimension targetDimension;

        public CataloguedResource(Item[] targets, Function<? super Integer, ? extends ResourceTask> resourceFunction) {
            this.targets = targets;
            this.resourceFunction = resourceFunction;
        }

        public CataloguedResource mineIfPresent() {
            shouldMine = true;
            return this;
        }

        public CataloguedResource dontMineIfPresent() {
            shouldMine = false;
            return this;
        }

        public CataloguedResource forceDimension(Dimension dimension) {
            shouldForceDimension = true;
            targetDimension = dimension;
            return this;
        }

        public CataloguedResource anyDimension() {
            shouldForceDimension = false;
            return this;
        }

        public ResourceTask getResource(int count) {
            ResourceTask result = resourceFunction.apply(count);
            if (shouldMine) {
                result = result.mineIfPresent(Util.itemsToBlocks(targets));
            }
            if (shouldForceDimension) {
                result = result.forceDimension(targetDimension);
            }
            return result;
        }
    }
}

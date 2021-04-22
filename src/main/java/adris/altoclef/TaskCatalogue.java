package adris.altoclef;

import adris.altoclef.tasks.*;
import adris.altoclef.tasks.misc.speedrun.CollectBlazeRodsTask;
import adris.altoclef.tasks.resources.*;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

@SuppressWarnings({"rawtypes"})
public class TaskCatalogue {

    private static final HashMap<String, Item[]> _nameToItemMatches = new HashMap<>();
    private static final HashMap<String, TaskFactory> _nameToResourceTask = new HashMap<>();
    private static final HashSet<Item> _resourcesObtainable = new HashSet<>();
    static {
        /// DEFINE RESOURCE TASKS HERE
        {
            String p = "planks";
            String s = "stick";
            String o = null;

            /// RAW RESOURCES
            mine("log", MiningRequirement.HAND, ItemTarget.LOG, ItemTarget.LOG);
            mine("dirt", MiningRequirement.HAND, new Block[]{Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.GRASS_PATH}, Items.DIRT);
            simple("cobblestone", Items.COBBLESTONE, CollectCobblestoneTask.class);
            mine("netherrack",  MiningRequirement.WOOD, Blocks.NETHERRACK, Items.NETHERRACK);
            mine("coal",  MiningRequirement.WOOD, Blocks.COAL_ORE, Items.COAL);
            mine("iron_ore", MiningRequirement.STONE, Blocks.IRON_ORE, Items.IRON_ORE);
            mine("gold_ore", MiningRequirement.IRON, Blocks.GOLD_ORE, Items.GOLD_ORE);
            mine("diamond", MiningRequirement.IRON, Blocks.DIAMOND_ORE, Items.DIAMOND);
            mine("emerald", MiningRequirement.IRON, Blocks.EMERALD_ORE, Items.EMERALD);
            mine("redstone", MiningRequirement.IRON, Blocks.REDSTONE_ORE, Items.REDSTONE);
            mine("lapis_lazuli", MiningRequirement.IRON, Blocks.LAPIS_ORE, Items.LAPIS_LAZULI);
            alias("lapis", "lapis_lazuli");
            mine("sand", Blocks.SAND, Items.SAND);
            mine("gravel", Blocks.GRAVEL, Items.GRAVEL);
            mine("clay_ball", Blocks.CLAY, Items.CLAY_BALL);
            simple("flint", Items.FLINT, CollectFlintTask.class);
            simple("obsidian", Items.OBSIDIAN, CollectObsidianTask.class);
            simple("wool", ItemTarget.WOOL, CollectWoolTask.class);
            mob("bone", Items.BONE, SkeletonEntity.class);
            mob("gunpowder", Items.GUNPOWDER, CreeperEntity.class);
            mob("ender_pearl", Items.ENDER_PEARL, EndermanEntity.class);
            mob("spider_eye", Items.SPIDER_EYE, SpiderEntity.class);
            mob("leather", Items.LEATHER, CowEntity.class);
            mob("feather", Items.FEATHER, ChickenEntity.class);
            mob("ender_pearl", Items.ENDER_PEARL, EndermanEntity.class);
            mob("slimeball", Items.SLIME_BALL, SlimeEntity.class);
            mob("ink_sac", Items.INK_SAC, SquidEntity.class); // Warning, this probably won't work.
            mine("sugar_cane", Blocks.SUGAR_CANE, Items.SUGAR_CANE);
            mine("brown_mushroom", MiningRequirement.HAND, new Block[] {Blocks.BROWN_MUSHROOM, Blocks.BROWN_MUSHROOM_BLOCK}, Items.BROWN_MUSHROOM);
            mine("red_mushroom", MiningRequirement.HAND, new Block[] {Blocks.RED_MUSHROOM, Blocks.RED_MUSHROOM_BLOCK}, Items.RED_MUSHROOM);
            simple("blaze_rod", Items.BLAZE_ROD, CollectBlazeRodsTask.class); // Not super simple tbh lmao
            simple("quartz", Items.QUARTZ, CollectQuartzTask.class);
            // Flowers
            mine("allium", Blocks.ALLIUM, Items.ALLIUM);
            mine("azure_bluet", Blocks.AZURE_BLUET, Items.AZURE_BLUET);
            mine("blue_orchid", Blocks.BLUE_ORCHID, Items.BLUE_ORCHID);
            mine("dandelion", Blocks.DANDELION, Items.DANDELION);
            mine("poppy", Blocks.POPPY, Items.POPPY);
            mine("red_tulip", Blocks.RED_TULIP, Items.RED_TULIP);
            mine("cactus", Blocks.CACTUS, Items.CACTUS);


            // MATERIALS
            simple("planks", ItemTarget.PLANKS, CollectPlanksTask.class);
            shapedRecipe2x2("stick", Items.STICK, 4, p, o, p, o);
            smelt("stone", Items.STONE, "cobblestone");
            smelt("smooth_stone", Items.SMOOTH_STONE, "stone");
            smelt("glass", Items.GLASS, "sand");
            smelt("iron_ingot", Items.IRON_INGOT, "iron_ore");
            smelt("charcoal", Items.CHARCOAL, "log");
            smelt("brick", Items.BRICK, "clay_ball");
            smelt("green_dye", Items.GREEN_DYE, "cactus");
            simple("gold_ingot", Items.GOLD_INGOT, CollectGoldIngotTask.class); // accounts for nether too
            shapedRecipe3x3Block("iron_block", Items.IRON_BLOCK, "iron_ingot");
            shapedRecipe3x3Block("gold_block", Items.GOLD_BLOCK, "gold_ingot");
            shapedRecipe3x3Block("diamond_block", Items.DIAMOND_BLOCK, "diamond");
            shapedRecipe3x3Block("redstone_block", Items.REDSTONE_BLOCK, "redstone");
            shapedRecipe3x3Block("coal_block", Items.COAL_BLOCK, "coal");
            shapedRecipe3x3Block("emerald_block", Items.EMERALD_BLOCK, "emerald");
            shapedRecipe2x2("bone_meal", Items.BONE_MEAL, 3, "bone", o, o, o);
            shapedRecipeSlab("smooth_stone_slab", Items.SMOOTH_STONE_SLAB, "smooth_stone");
            shapedRecipe3x3("paper", Items.PAPER, 3, "sugar_cane", "sugar_cane", "sugar_cane", o, o, o, o, o, o);
            shapedRecipe2x2("book", Items.BOOK, 1, "paper", "paper", "paper", "leather");
            shapedRecipe2x2("book_and_quill", Items.WRITABLE_BOOK, 1, "book", "ink_sac", o, "feather");
            shapedRecipe3x3("bowl", Items.BOWL, 4, p,o,p, o,p,o, o,o,o);
            shapedRecipe2x2("blaze_powder", Items.BLAZE_POWDER, 2, "blaze_rod", o, o, o);
            shapedRecipe2x2("ender_eye", Items.ENDER_EYE, 1, "blaze_powder", "ender_pearl", o, o);
            alias("eye_of_ender", "ender_eye");
            shapedRecipe2x2("fermented_spider_eye", Items.SPIDER_EYE, 1, "brown_mushroom", "sugar", o, "spider_eye");
            shapedRecipe3x3("fire_charge", Items.FIRE_CHARGE, 3, o, "blaze_powder", o, o, "coal", o, o, "gunpowder", o);
            /*
            {
                String g = "gold_nugget";
            }t
             */


            // FURNITURE
            shapedRecipe2x2("crafting_table", Items.CRAFTING_TABLE, 1, p, p, p, p);
            shapedRecipe2x2("wooden_pressure_plate", ItemTarget.WOOD_PRESSURE_PLATE, 1, o, o, p, p);
            shapedRecipe2x2("wooden_button", ItemTarget.WOOD_BUTTON, 1, p, o, o, o);
            shapedRecipe2x2("stone_pressure_plate", Items.STONE_PRESSURE_PLATE, 1, o, o, "stone", "stone");
            shapedRecipe2x2("stone_button", Items.STONE_BUTTON, 1, "stone", o, o, o);
            simple("sign", ItemTarget.WOOD_SIGN, CollectSignTask.class);
            {
                String c = "cobblestone";
                shapedRecipe3x3("furnace", Items.FURNACE, 1, c, c, c, c, o, c, c, c, c);
                shapedRecipe3x3("dropper", Items.DISPENSER, 1, c,c,c, c,o,c, c,"redstone",c);
                shapedRecipe3x3("dispenser", Items.DISPENSER, 1, c,c,c, c,"bow",c, c,"redstone",c);
            }
            shapedRecipe3x3("chest", Items.CHEST, 1, p, p, p, p, o, p, p, p, p);
            shapedRecipe2x2("torch", Items.TORCH, 4, "coal", o, s, o);
            simple("bed", ItemTarget.BED, CollectBedTask.class);
            {
                String i = "iron_ingot";
                String b = "iron_block";
                shapedRecipe3x3("anvil", Items.ANVIL, 1, b, b, b, o, i, o, i, i, i);
                shapedRecipe3x3("cauldron", Items.CAULDRON, 1, i, o, i, i, o, i, i, i, i);
                shapedRecipe3x3("minecart", Items.MINECART, 1, o, o, o, i, o, i, i, i, i);
            }
            shapedRecipe3x3("armor_stand", Items.ARMOR_STAND, 1, s, s, s, o, s, o, s, "smooth_stone_slab", s);
            {
                String b = "obsidian";
                shapedRecipe3x3("enchanting_table", Items.ENCHANTING_TABLE, 1, o, "book", o, "diamond", b, "diamond", b, b, b);
                shapedRecipe3x3("ender_chest", Items.ENDER_CHEST, 1, o, o, o, o, "ender_eye", o, o, o, o);
            }
            {
                String b = "brick";
                shapedRecipe3x3("flower_pot", Items.FLOWER_POT,1, b, o, b, o, b, o, o, o, o);
            }
            //shapedRecipe3x3("daylight_detector", Items.DAYLIGHT_DETECTOR, 1, ); <- NEED WOOD SLABS

            /// TOOLS
            tools("wooden", "planks", Items.WOODEN_PICKAXE, Items.WOODEN_SHOVEL, Items.WOODEN_SWORD, Items.WOODEN_AXE, Items.WOODEN_HOE);
            tools("stone", "cobblestone", Items.STONE_PICKAXE, Items.STONE_SHOVEL, Items.STONE_SWORD, Items.STONE_AXE, Items.STONE_HOE);
            tools("iron", "iron_ingot", Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_HOE);
            tools("golden", "gold_ingot", Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_HOE);
            tools("diamond", "diamond", Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_HOE);
            armor("leather", "leather", Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
            armor("iron", "iron_ingot", Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
            armor("golden", "gold_ingot", Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS);
            armor("diamond", "diamond", Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
            shapedRecipe3x3("bow", Items.BOW, 1, s, o, o, o, s, o, s, o, o);
            shapedRecipe3x3("arrow", Items.ARROW, 4, "flint", o, o, s, o, o, "feather", o, o);
            {
                String i = "iron_ingot";
                shapedRecipe3x3("bucket", Items.BUCKET, 1, i, o, i, o, i, o, o, o, o);
                shapedRecipe2x2("flint_and_steel", Items.FLINT_AND_STEEL, 1, i, o, o, "flint");
                shapedRecipe2x2("shears", Items.SHEARS, 1, i, o, o, i);
                shapedRecipe3x3("compass", Items.COMPASS, 1, o, i, o, i, "redstone", i, o, i, o);
                String g = "gold_ingot";
                shapedRecipe3x3("clock", Items.CLOCK, 1, o, g, o, g, "redstone", g, o, g, o);
            }
            simple("water_bucket", Items.WATER_BUCKET, CollectBucketLiquidTask.CollectWaterBucketTask.class);
            simple("lava_bucket", Items.LAVA_BUCKET, CollectBucketLiquidTask.CollectLavaBucketTask.class);
            {
                String a = "paper";
                shapedRecipe3x3("map", Items.MAP, 1, a,a,a, a,"compass",a, a,a,a);
            }
            shapedRecipe3x3("fishing_rod", Items.FISHING_ROD, 1, o, o, s, o, s, "string", s, o, "string");
            shapedRecipe3x3("glass_bottle", Items.GLASS_BOTTLE, 3, "glass", o, "glass", o, "glass", o, o, o, o);
            alias("wooden_pick", "wooden_pickaxe");
            alias("stone_pick", "stone_pickaxe");
            alias("iron_pick", "iron_pickaxe");
            alias("gold_pick", "gold_pickaxe");
            alias("diamond_pick", "diamond_pickaxe");

            /// FOOD
            mobCook("porkchop", Items.PORKCHOP, Items.COOKED_PORKCHOP, PigEntity.class);
            mobCook("beef", Items.BEEF, Items.COOKED_BEEF, CowEntity.class);
            mobCook("chicken", Items.CHICKEN, Items.COOKED_CHICKEN, ChickenEntity.class);
            mobCook("mutton", Items.MUTTON, Items.COOKED_MUTTON, SheepEntity.class);
            mobCook("rabbit", Items.RABBIT, Items.COOKED_RABBIT, RabbitEntity.class);
            mobCook("salmon", Items.SALMON, Items.COOKED_SALMON, SalmonEntity.class);
            mobCook("cod", Items.COD, Items.COOKED_COD, CodEntity.class);
            mine("apple", Blocks.OAK_LEAVES, Items.APPLE);
        }
    }

    private static void put(String name, Item[] matches, TaskFactory factory) {
        _nameToResourceTask.put(name, factory);
        _nameToItemMatches.put(name, matches);
        _resourcesObtainable.addAll(Arrays.asList(matches));
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
        if (!_nameToItemMatches.containsKey(name)) {
            return null;
        }
        return _nameToItemMatches.get(name);
    }

    public static boolean isObtainable(Item item) {
        return _resourcesObtainable.contains(item);
    }

    public static ItemTarget getItemTarget(String name, int count) {
        return new ItemTarget(name, count);
    }
    public static CataloguedResourceTask getSquashedItemTask(ItemTarget ...targets) {
        return new CataloguedResourceTask(true, targets);
    }

    public static ResourceTask getItemTask(String name, int count) {

        if (!taskExists(name)) {
            Debug.logWarning("Task " + name + " does not exist. Error possibly.");
            Debug.logStack();
            return null;
        }

        TaskFactory creator = _nameToResourceTask.get(name);
        return creator.createResourceTask(name, count);
    }

    public static ResourceTask getItemTask(ItemTarget target) {
        return getItemTask(target.getCatalogueName(), target.targetCount);
    }

    public static boolean taskExists(String name) {
        return _nameToResourceTask.containsKey(name);
    }

    public static Collection<String> resourceNames() {
        return _nameToResourceTask.keySet();
    }

    private static <T> void simple(String name, Item[] matches, Class<T> type) {
        put(name, matches, new SimpleTaskFactory(type));
    }
    private static <T> void simple(String name, Item matches, Class<T> type) {
        simple(name, new Item[] {matches}, type);
    }
    private static void mine(String name, MiningRequirement requirement, Item[] toMine, Item ...targets) {
        Block[] toMineBlocks = new Block[toMine.length];
        for (int i = 0; i < toMine.length; ++i) toMineBlocks[i] = Block.getBlockFromItem(toMine[i]);
        mine(name, requirement, toMineBlocks, targets);
    }
    private static void mine(String name, MiningRequirement requirement, Block[] toMine, Item ...targets) {
        put(name, targets, new MineTaskFactory(MineAndCollectTask.class, targets, toMine, requirement));
    }
    private static void mine(String name, MiningRequirement requirement, Block toMine, Item target) {
        mine(name, requirement, new Block[]{toMine}, target);
    }
    private static void mine(String name, Block toMine, Item target) {
        mine(name, MiningRequirement.HAND, toMine, target);
    }

    private static void shapedRecipe2x2(String name, Item[] matches, int outputCount, String s0, String s1, String s2, String s3) {
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(name, new ItemTarget[] {t(s0), t(s1), t(s2), t(s3)}, outputCount);
        put(name, matches, new CraftTaskFactory(CraftInInventoryTask.class, name, recipe));
    }
    private static void shapedRecipe3x3(String name, Item[] matches, int outputCount, String s0, String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) {
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(name, new ItemTarget[] {t(s0), t(s1), t(s2), t(s3), t(s4), t(s5), t(s6), t(s7), t(s8)}, outputCount);
        put(name, matches, new CraftTaskFactory(CraftInTableTask.class, name, recipe));
    }
    private static void shapedRecipe2x2(String name, Item match, int craftCount, String s0, String s1, String s2, String s3) {
        shapedRecipe2x2(name, new Item[]{match}, craftCount, s0, s1, s2, s3);
    }
    private static void shapedRecipe3x3(String name, Item match, int craftCount, String s0, String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) {
        shapedRecipe3x3(name, new Item[]{match}, craftCount, s0, s1, s2, s3, s4, s5, s6, s7, s8);
    }
    private static void shapedRecipe3x3Block(String name, Item match, String material) {
        shapedRecipe3x3(name, match, 1, material,material,material,material,material,material,material,material,material);
    }
    private static void shapedRecipeSlab(String name, Item match, String material) {
        shapedRecipe3x3(name, match, 6, null, null, null, null, null, null, material, material, material);
    }
    private static void shapedRecipeStairs(String name, Item match, String material) {
        shapedRecipe3x3(name, match, 4, material, null, null, material, material, null, material, material, material);
    }

    private static void smelt(String name, Item[] matches, String materials) {
        put(name, matches, new SmeltTaskFactory(SmeltInFurnaceTask.class, name, materials));
    }
    private static void smelt(String name, Item match, String materials) {
        smelt(name, new Item[]{match}, materials);
    }

    private static void mob(String name, Item[] matches, Class mobClass) {
        put(name, matches, new MobTaskFactory(mobClass, matches));
    }
    private static void mob(String name, Item match, Class mobClass) {
        mob(name, new Item[] {match}, mobClass);
    }

    private static void mobCook(String uncookedName, String cookedName, Item uncooked, Item cooked, Class mobClass) {
        mob(uncookedName, uncooked, mobClass);
        smelt(cookedName, cooked, uncookedName);
    }
    private static void mobCook(String uncookedName, Item uncooked, Item cooked, Class mobClass) {
        mobCook(uncookedName, "cooked_" + uncookedName, uncooked, cooked, mobClass);
    }

    private static void tools(String toolMaterialName, String material, Item pickaxeItem, Item shovelItem, Item swordItem, Item axeItem, Item hoeItem) {
        String s = "stick";
        String o = null;
        //noinspection UnnecessaryLocalVariable
        String m = material;
        shapedRecipe3x3(toolMaterialName + "_pickaxe", pickaxeItem, 1, m, m, m, o, s, o, o, s, o);
        shapedRecipe3x3(toolMaterialName + "_shovel", shovelItem, 1, o, m, o, o, s, o, o, s, o);
        shapedRecipe3x3(toolMaterialName + "_sword", swordItem, 1, o, m, o, o, m, o, o, s, o);
        shapedRecipe3x3(toolMaterialName + "_axe", axeItem, 1, m, m, o, m, s, o, o, s, o);
        shapedRecipe3x3(toolMaterialName + "_hoe", hoeItem, 1, m, m, o, o, s, o, o, s, o);
    }

    private static void armor(String armorMaterialName, String material, Item helmetItem, Item chestplateItem, Item leggingsItem, Item bootsItem) {
        String o = null;
        //noinspection UnnecessaryLocalVariable
        String m = material;
        shapedRecipe3x3(armorMaterialName + "_helmet", helmetItem, 1, m, m, m, m, o, m, o, o, o);
        shapedRecipe3x3(armorMaterialName + "_chestplate", chestplateItem, 1, m, o, m, m, m, m, m, m, m);
        shapedRecipe3x3(armorMaterialName + "_leggings", leggingsItem, 1, m, m, m, m, o, m, m, o, m);
        shapedRecipe3x3(armorMaterialName + "_boots", bootsItem, 1, o, o, o, m, o, m, m, o, m);
    }

    private static void alias(String newName, String original) {
        _nameToResourceTask.put(newName, _nameToResourceTask.get(original));
        _nameToItemMatches.put(newName, _nameToItemMatches.get(original));

    }





    private static ItemTarget t(String cataloguedName) {
        return new ItemTarget(cataloguedName);
    }


    /// TASK FACTORIES (I think I'm using the term "factory" wrong here but screw OOP I'll call it whatever I want)

    // Basically the issue is that tasks usually accept ItemTargets, which are pairs of items and their COUNTS.
    // These factories let you create these tasks with a count passed in LATER

    static class SimpleTaskFactory extends TaskFactory {
        public SimpleTaskFactory(Class type) {
            super(type);
        }

        @Override
        protected ResourceTask createResourceTaskInternal(String name, int count) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            Constructor constructor = _class.getConstructor(int.class);
            return (ResourceTask) constructor.newInstance(count);
        }
    }

    static class SmeltTaskFactory extends TaskFactory {

        private String _targetName;
        private String _materials;

        public SmeltTaskFactory(Class type, String targetName, String materials) {
            super(type);
            _targetName = targetName;
            _materials = materials;
        }

        @Override
        protected ResourceTask createResourceTaskInternal(String name, int count) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            Constructor constructor = _class.getConstructor(SmeltTarget.class);
            return (ResourceTask) constructor.newInstance(new SmeltTarget(new ItemTarget(_targetName, count), new ItemTarget(_materials, count)));
        }
    }

    static class CraftTaskFactory extends TaskFactory {
        // Generic Resource Task
        private String _targetName;

        // Craft task
        private CraftingRecipe _recipe;

        public CraftTaskFactory(Class type, String targetName, CraftingRecipe recipe) {
            super(type);
            _targetName = targetName;
            _recipe = recipe;

        }

        @Override
        protected ResourceTask createResourceTaskInternal(String name, int count) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            Constructor constructor = _class.getConstructor(ItemTarget.class, CraftingRecipe.class);
            return (ResourceTask) constructor.newInstance(new ItemTarget(_targetName, count), _recipe);
        }
    }

    static class MineTaskFactory extends TaskFactory {

        // Mine task
        private Block[] _toMine;
        private MiningRequirement _requirement;
        private Item[] _target;


        public MineTaskFactory(Class type, Item[] target, Block[] toMine, MiningRequirement requirement) {
            super(type);
            _target = target;
            _toMine = toMine;
            _requirement = requirement;
        }

        @Override
        protected ResourceTask createResourceTaskInternal(String name, int count) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            Constructor constructor = _class.getConstructor(ItemTarget.class, Block[].class, MiningRequirement.class);
            return (ResourceTask) constructor.newInstance(new ItemTarget(_target,  count), _toMine, _requirement);
        }
    }

    static class MobTaskFactory extends TaskFactory {
        private Item[] _target;
        private Class _mob;
        public MobTaskFactory(Class mob, Item[] target) {
            super(KillAndLootTask.class);
            _mob = mob;
            _target = target;
        }
        @Override
        protected ResourceTask createResourceTaskInternal(String name, int count) {
            //Constructor constructor = _class.getConstructor(ItemTarget.class, Block[].class, MiningRequirement.class);
            ItemTarget[] targets = new ItemTarget[_target.length];
            for (int i = 0; i < targets.length; ++i) targets[i] = new ItemTarget(_target[i], count);
            return new KillAndLootTask(_mob, targets);//(ResourceTask) constructor.newInstance(new ItemTarget(_target,  count), _toMine, _requirement);
        }
    }

    static abstract class TaskFactory {
        protected final Class _class;

        public TaskFactory(Class type) {
            _class = type;
        }
        protected abstract ResourceTask createResourceTaskInternal(String name, int count) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException;

        public ResourceTask createResourceTask(String name, int count) {
            try {
                return createResourceTaskInternal(name, count);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                Debug.logWarning(e.getMessage() + "Couldn't find standard resource constructor for task for \"" + name + "\".");
                return null;
            }
        }
    }

}

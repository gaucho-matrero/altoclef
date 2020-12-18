package adris.altoclef.util;

import adris.altoclef.Debug;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.CraftInTableTask;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.resources.CollectCobblestoneTask;
import adris.altoclef.tasks.resources.CollectPlanksTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;

public class TaskCatalogue {

    private static HashMap<String, Item[]> _nameToItemMatches = new HashMap<String, Item[]>();
    private static HashMap<String, TaskFactory> _nameToResourceTask = new HashMap<String, TaskFactory>();
    static {
        /// DEFINE RESOURCE TASKS HERE
        {
            String p = "planks";
            String s = "stick";
            String o = null;

            simple("planks", ItemTarget.PLANKS, CollectPlanksTask.class);
            shapedRecipe2x2("stick", Items.STICK, p, o, p, o);
            mine("log", ItemTarget.LOG);
            mine("dirt", new Item[]{Items.DIRT}, Items.GRASS_BLOCK, Items.GRASS_PATH);
            shapedRecipe2x2("crafting_table", Items.CRAFTING_TABLE, p, p, p, p);
            shapedRecipe2x2("wooden_pressure_plate", ItemTarget.WOOD_PRESSURE_PLATE, o, o, p, p);
            shapedRecipe2x2("wooden_button", ItemTarget.WOOD_BUTTON, p, o, o, o);

            // TODO: Neat function to map Pickaxe, Axe, Shovel, Sword, Hoe, etc...
            shapedRecipe3x3("wooden_pickaxe", Items.WOODEN_PICKAXE, p, p, p, o, s, o, o, s, o);
            simple("cobblestone", Items.COBBLESTONE, CollectCobblestoneTask.class);
            {
                String c = "cobblestone";
                shapedRecipe3x3("stone_pickaxe", Items.STONE_PICKAXE, c, c, c, o, s, o, o, s, o);
            }
        }
    };

    private static void put(String name, Item[] matches, TaskFactory factory) {
        _nameToResourceTask.put(name, factory);
        _nameToItemMatches.put(name, matches);
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

    public static ResourceTask getItemTask(String name, int count) {

        if (!_nameToResourceTask.containsKey(name)) {
            return null;
        }

        TaskFactory creator = _nameToResourceTask.get(name);
        return creator.createResourceTask(name, count);
    }

    public static Collection<String> resourceNames() {
        return _nameToResourceTask.keySet();
    }

    private static <T> void simple(String name, Item[] matches, Class<T> type) {
        put(name, matches, new TaskFactory(type));
    }
    private static <T> void simple(String name, Item matches, Class<T> type) {
        simple(name, new Item[] {matches}, type);
    }
    private static <T> void mine(String name, Item[] matches, Item ...target) {
        put(name, matches, new TaskFactory(MineAndCollectTask.class, target));
    }
    private static <T> void mine(String name, Item[] matches, Item target) {
        mine(name, matches, new Item[]{target});
    }

    private static <T> void shapedRecipe2x2(String name, Item[] matches, String s0, String s1, String s2, String s3) {
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(name, new ItemTarget[] {t(s0), t(s1), t(s2), t(s3)});
        put(name, matches, new TaskFactory(CraftInInventoryTask.class, matches, recipe));
    }
    private static <T> void shapedRecipe3x3(String name, Item[] matches, String s0, String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) {
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(name, new ItemTarget[] {t(s0), t(s1), t(s2), t(s3), t(s4), t(s5), t(s6), t(s7), t(s8)});
        put(name, matches, new TaskFactory(CraftInTableTask.class, matches, recipe));
    }
    private static <T> void shapedRecipe2x2(String name, Item match, String s0, String s1, String s2, String s3) {
        shapedRecipe2x2(name, new Item[]{match}, s0, s1, s2, s3);
    }
    private static <T> void shapedRecipe3x3(String name, Item match, String s0, String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) {
        shapedRecipe3x3(name, new Item[]{match}, s0, s1, s2, s3, s4, s5, s6, s7, s8);
    }

    private static ItemTarget t(String cataloguedName) {
        return new ItemTarget(cataloguedName);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static class TaskFactory {
        private final Class _class;

        private CraftingRecipe _recipe;
        private Item[] _target;

        private Type _type;

        enum Type {
            SIMPLE,
            MINING,
            CRAFTING
        }

        public TaskFactory(Class type) {
            _class = type;
            _type = Type.SIMPLE;
        }

        public TaskFactory(Class type, Item[] target, CraftingRecipe recipe) {
            this(type);
            _target = target;
            _recipe = recipe;
            _type = Type.CRAFTING;
        }
        public TaskFactory(Class type, Item[] target) {
            this(type);
            _target = target;
            _type = Type.MINING;
        }

        public ResourceTask createResourceTask(String name, int count) {
            try {
                switch (_type) {
                    case SIMPLE: {
                        Constructor constructor = _class.getConstructor(int.class);
                        return (ResourceTask) constructor.newInstance(count);
                    }
                    case CRAFTING: {
                        Constructor constructor = _class.getConstructor(ItemTarget.class, CraftingRecipe.class);
                        return (ResourceTask) constructor.newInstance(new ItemTarget(_target, count), _recipe);
                    }
                    case MINING: {
                        Constructor constructor = _class.getConstructor(ItemTarget.class);
                        return (ResourceTask) constructor.newInstance(new ItemTarget(_target, count));
                    }
                    default:
                        Debug.logError("Missed a spot");
                        return null;
                }
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                Debug.logWarning(e.getMessage() + "Couldn't find standard resource constructor for task for \"" + name + "\".");
                return null;
            }
        }
    }

}

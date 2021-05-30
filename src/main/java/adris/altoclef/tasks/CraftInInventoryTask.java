package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.slots.Slot;


public class CraftInInventoryTask extends ResourceTask {
    private final CraftingRecipe recipe;
    private final boolean collect;
    private final boolean ignoreUncataloguedSlots;
    private boolean fullCheckSucceeded = true;

    public CraftInInventoryTask(ItemTarget target, CraftingRecipe recipe, boolean collect, boolean ignoreUncataloguedSlots) {
        super(target);
        this.recipe = recipe;
        this.collect = collect;
        this.ignoreUncataloguedSlots = ignoreUncataloguedSlots;
    }

    public CraftInInventoryTask(ItemTarget target, CraftingRecipe recipe) {
        this(target, recipe, true, false);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        fullCheckSucceeded = true;
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        ItemTarget toGet = itemTargets[0];
        if (collect && !mod.getInventoryTracker().hasRecipeMaterialsOrTarget(new RecipeTarget(toGet, recipe))) {
            // Collect recipe materials
            setDebugState("Collecting materials");
            return collectRecipeSubTask(mod);
        }

        // Free up inventory
        if (mod.getInventoryTracker().isInventoryFull()) {
            // Throw away!
            Slot toThrow = mod.getInventoryTracker().getGarbageSlot();
            if (toThrow != null) {
                // Equip then throw
                mod.getInventoryTracker().throwSlot(toThrow);
            } else {
                if (fullCheckSucceeded) {
                    Debug.logWarning("Failed to free up inventory as no throwaway-able slot was found. Awaiting user input.");
                }
                fullCheckSucceeded = false;
            }
        }

        setDebugState("Crafting in inventory... for " + toGet);
        return new CraftGenericTask(recipe);
        //craftInstant(mod, _recipe);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CraftInInventoryTask) {
            CraftInInventoryTask t = (CraftInInventoryTask) other;
            if (!t.recipe.equals(recipe)) return false;
            return isCraftingEqual(t);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return toCraftingDebugStringName() + " " + recipe;
    }

    // virtual. By default assumes subtasks are CATALOGUED (in TaskCatalogue.java)
    protected Task collectRecipeSubTask(AltoClef mod) {
        return new CollectRecipeCataloguedResourcesTask(ignoreUncataloguedSlots, new RecipeTarget(itemTargets[0], recipe));
    }

    protected String toCraftingDebugStringName() {
        return "Craft 2x2 Task";
    }

    protected boolean isCraftingEqual(CraftInInventoryTask other) {
        return true;
    }
}

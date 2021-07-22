package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.slots.Slot;

public class CraftInInventoryTask extends ResourceTask {

    private final CraftingRecipe _recipe;
    private final boolean _collect;
    private final boolean _ignoreUncataloguedSlots;
    private boolean _fullCheckFailed = false;

    public CraftInInventoryTask(ItemTarget target, CraftingRecipe recipe, boolean collect, boolean ignoreUncataloguedSlots) {
        super(target);
        _recipe = recipe;
        _collect = collect;
        _ignoreUncataloguedSlots = ignoreUncataloguedSlots;
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
        _fullCheckFailed = false;
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        ItemTarget toGet = _itemTargets[0];
        if (_collect && !mod.getInventoryTracker().hasRecipeMaterialsOrTarget(new RecipeTarget(toGet, _recipe))) {
            // Collect recipe materials
            setDebugState("Collecting materials");
            return collectRecipeSubTask(mod);
        }

        // Free up inventory
        if (!mod.getInventoryTracker().ensureFreeInventorySlot()) {
            if (!_fullCheckFailed) {
                Debug.logWarning("Failed to free up inventory as no throwaway-able slot was found. Awaiting user input.");
            }
            _fullCheckFailed = true;
        }

        setDebugState("Crafting in inventory... for " + toGet);
        return new CraftGenericTask(_recipe);
        //craftInstant(mod, _recipe);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CraftInInventoryTask) {
            CraftInInventoryTask t = (CraftInInventoryTask) other;
            if (!t._recipe.equals(_recipe)) return false;
            return isCraftingEqual(t);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return toCraftingDebugStringName() + " " + _recipe;
    }

    // virtual. By default assumes subtasks are CATALOGUED (in TaskCatalogue.java)
    protected Task collectRecipeSubTask(AltoClef mod) {
        return new CollectRecipeCataloguedResourcesTask(_ignoreUncataloguedSlots, new RecipeTarget(_itemTargets[0], _recipe));
    }

    protected String toCraftingDebugStringName() {
        return "Craft 2x2 Task";
    }

    protected boolean isCraftingEqual(CraftInInventoryTask other) {
        return true;
    }
}

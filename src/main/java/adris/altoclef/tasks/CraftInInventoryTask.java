package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.csharpisbetter.Timer;

public class CraftInInventoryTask extends ResourceTask {

    private CraftingRecipe _recipe;

    private Timer _craftTimer = new Timer(0.5);

    public CraftInInventoryTask(ItemTarget target, CraftingRecipe recipe) {
        super(target);
        _recipe = recipe;
    }
    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (!mod.getInventoryTracker().hasRecipeMaterials(_recipe)) {
            // Collect recipe materials
            return collectRecipeSubTask(mod);
        }

        // Delay our crafting so the server has time to give us our item back.
        if (_craftTimer.elapsed()) {
            _craftTimer.reset();
            craftInstant(mod, _recipe);
        }

        return null;
    }

    private void craftInstant(AltoClef mod, CraftingRecipe recipe) {
        mod.getInventoryTracker().craftInstant(recipe);
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
        return new CollectRecipeCataloguedResourcesTask(new RecipeTarget(_itemTargets.get(0), _recipe));
    }

    protected String toCraftingDebugStringName() {
        return "Craft Task";
    }
    protected boolean isCraftingEqual(CraftInInventoryTask other) {
        return true;
    }
}

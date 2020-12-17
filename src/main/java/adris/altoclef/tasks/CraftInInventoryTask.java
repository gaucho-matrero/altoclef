package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.TaskCatalogue;
import net.minecraft.item.Item;

import java.util.HashMap;

public class CraftInInventoryTask extends ResourceTask {

    private CraftingRecipe _recipe;

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

        craftInstant(mod, _recipe);

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

        // TODO: Cache this instead of doing it every frame.
        HashMap<String, Integer> catalogueCount = new HashMap<String, Integer>();

        // Default, just go through the recipe slots and collect the first one.
        for (int i = 0; i < _recipe.getSlotCount(); ++i) {
            ItemTarget slot = _recipe.getSlot(i);
            if (!slot.isCatalogueItem()) {
                Debug.logWarning("Recipe collection for recipe " + _recipe + " slot " + i
                        + " is not catalogued. Please define an explicit"
                        + " collectRecipeSubTask() function for this task."
                );
            } else {
                String targetName = slot.getCatalogueName();
                if (!catalogueCount.containsKey(targetName)) {
                    catalogueCount.put(targetName, 0);
                }
                catalogueCount.put(targetName, catalogueCount.get(targetName) + 1);
            }
        }

        // (Cache this with the above stuff!!)
        for (String catalogueName : catalogueCount.keySet()) {
            int count = catalogueCount.get(catalogueName);
            ItemTarget target = new ItemTarget(TaskCatalogue.getItemMatches(catalogueName), count);
            if (!mod.getInventoryTracker().targetReached(target)) {
                return TaskCatalogue.getItemTask(catalogueName, count);
            }
        }

        return null;
    }

    protected String toCraftingDebugStringName() {
        return "Craft Task";
    }
    protected boolean isCraftingEqual(CraftInInventoryTask other) {
        return true;
    }
}

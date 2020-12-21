package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.TaskCatalogue;

import java.util.HashMap;

// Collects everything that's catalogued for a recipe.
public class CollectRecipeCataloguedResourcesTask extends Task {

    private CraftingRecipe _recipe;

    public CollectRecipeCataloguedResourcesTask(CraftingRecipe recipe) {
        _recipe = recipe;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        // TODO: Cache this instead of doing it every frame.
        HashMap<String, Integer> catalogueCount = new HashMap<>();

        // Default, just go through the recipe slots and collect the first one.
        for (int i = 0; i < _recipe.getSlotCount(); ++i) {
            ItemTarget slot = _recipe.getSlot(i);
            if (slot == null || slot.isEmpty()) continue;
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
            if (!mod.getInventoryTracker().targetMet(target)) {
                return TaskCatalogue.getItemTask(catalogueName, count);
            }
        }
        return null;
    }


    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof CollectRecipeCataloguedResourcesTask) {
            CollectRecipeCataloguedResourcesTask other = (CollectRecipeCataloguedResourcesTask) obj;
            return other._recipe.equals(_recipe);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Collect Recipe Resources: " + _recipe;
    }
}

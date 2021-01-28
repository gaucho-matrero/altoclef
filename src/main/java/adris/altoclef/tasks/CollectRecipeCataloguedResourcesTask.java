package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.util.RecipeTarget;
import net.minecraft.item.Item;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;

// Collects everything that's catalogued for a recipe.
public class CollectRecipeCataloguedResourcesTask extends Task {

    private RecipeTarget[] _targets;

    private boolean _finished = false;

    public CollectRecipeCataloguedResourcesTask(RecipeTarget ...targets) {
        _targets = targets;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _finished = false;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // TODO: Cache this once instead of doing it every frame.

        HashMap<String, Integer> catalogueCount = new HashMap<>();

        for (RecipeTarget target : _targets) {
            // Ignore this recipe if we have its item.
            if (mod.getInventoryTracker().targetMet(target.getItem())) continue;

            CraftingRecipe recipe = target.getRecipe();
            // Default, just go through the recipe slots and collect the first one.
            for (int i = 0; i < recipe.getSlotCount(); ++i) {
                ItemTarget slot = recipe.getSlot(i);
                if (slot == null || slot.isEmpty()) continue;
                if (!slot.isCatalogueItem()) {
                    Debug.logWarning("Recipe collection for recipe " + recipe + " slot " + i
                            + " is not catalogued. Please define an explicit"
                            + " collectRecipeSubTask() function for this task."
                    );
                } else {
                    String targetName = slot.getCatalogueName();
                    if (!catalogueCount.containsKey(targetName)) {
                        catalogueCount.put(targetName, 0);
                    }
                    // How many "repeats" of a recipe we will need.
                    int numberOfRepeats = (int)Math.floor(0.1 + (double)target.getItem().targetCount / target.getRecipe().outputCount()) + 1;
                    catalogueCount.put(targetName, catalogueCount.get(targetName) + numberOfRepeats);
                }
            }

            // (Cache this with the above stuff!!)
            for (String catalogueName : catalogueCount.keySet()) {
                int count = catalogueCount.get(catalogueName);
                ItemTarget itemTarget = new ItemTarget(catalogueName, count);
                if (!mod.getInventoryTracker().targetMet(itemTarget)) {
                    setDebugState("Getting " + itemTarget);
                    return TaskCatalogue.getItemTask(catalogueName, count);
                }
            }
        }

        _finished = true;

        return null;
    }


    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof CollectRecipeCataloguedResourcesTask) {
            CollectRecipeCataloguedResourcesTask other = (CollectRecipeCataloguedResourcesTask) obj;
            if (other._targets.length != _targets.length) return false;
            for (int i = 0; i < _targets.length; ++i) {
                if (!other._targets[i].equals(_targets[i])) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Collect Recipe Resources: " + ArrayUtils.toString(_targets);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _finished;
    }
}

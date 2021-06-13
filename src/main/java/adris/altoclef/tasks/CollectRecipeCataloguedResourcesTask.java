package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.csharpisbetter.Util;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;

// Collects everything that's catalogued for a recipe.
public class CollectRecipeCataloguedResourcesTask extends Task {

    private final RecipeTarget[] _targets;
    private final boolean _ignoreUncataloguedSlots;
    private boolean _finished = false;

    public CollectRecipeCataloguedResourcesTask(boolean ignoreUncataloguedSlots, RecipeTarget... targets) {
        _targets = targets;
        _ignoreUncataloguedSlots = ignoreUncataloguedSlots;
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
            //if (mod.getInventoryTracker().targetMet(target.getItem())) continue;

            // null = empty which is always met.
            if (target == null) continue;

            int weNeed = target.getItem().getTargetCount() - mod.getInventoryTracker().getItemCountIncludingTable(target.getItem());

            if (weNeed > 0) {
                CraftingRecipe recipe = target.getRecipe();
                // Default, just go through the recipe slots and collect the first one.
                for (int i = 0; i < recipe.getSlotCount(); ++i) {
                    ItemTarget slot = recipe.getSlot(i);
                    if (slot == null || slot.isEmpty()) continue;
                    if (!slot.isCatalogueItem()) {
                        if (!_ignoreUncataloguedSlots) {
                            Debug.logWarning("Recipe collection for recipe " + recipe + " slot " + i
                                    + " is not catalogued. Please define an explicit"
                                    + " collectRecipeSubTask() function for this task."
                            );
                        }
                    } else {
                        String targetName = slot.getCatalogueName();
                        if (!catalogueCount.containsKey(targetName)) {
                            catalogueCount.put(targetName, 0);
                        }
                        // How many "repeats" of a recipe we will need.
                        int numberOfRepeats = (int) Math.floor(-0.1 + (double) weNeed / target.getRecipe().outputCount()) + 1;
                        catalogueCount.put(targetName, catalogueCount.get(targetName) + numberOfRepeats);
                    }
                }
            }
        }


        // (Cache this with the above stuff!!)
        // Grab materials
        for (String catalogueMaterialName : catalogueCount.keySet()) {
            int count = catalogueCount.get(catalogueMaterialName);
            if (count > 0) {
                ItemTarget itemTarget = new ItemTarget(catalogueMaterialName, count);
                if (!mod.getInventoryTracker().targetMet(itemTarget)) {
                    setDebugState("Getting " + itemTarget);
                    return TaskCatalogue.getItemTask(catalogueMaterialName, count);
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
            return Util.arraysEqual(other._targets, _targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Collect Recipe Resources: " + ArrayUtils.toString(_targets);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if (_finished) {
            if (!mod.getInventoryTracker().hasRecipeMaterialsOrTarget(this._targets)) {
                _finished = false;
                Debug.logMessage("Invalid collect recipe \"finished\" state, resetting.");
            }
        }
        return _finished;
    }
}

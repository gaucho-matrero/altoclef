package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import net.minecraft.item.Item;

import java.util.Optional;

public abstract class CraftInInventoryTask extends Task {

    private CraftingRecipe _recipe;

    public CraftInInventoryTask(CraftingRecipe recipe) {
        _recipe = recipe;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!mod.getInventoryTracker().hasRecipeMaterials(_recipe)) {
            // Collect recipe materials
            return collectRecipeSubTask();
        }
        // Craft!
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CraftInInventoryTask) {
            CraftInInventoryTask t = (CraftInInventoryTask) other;
            if (!t._recipe.equals(_recipe)) return false;
            return isTaskEqual(t);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return toTaskDebugString() + " " + _recipe;
    }

    // virtual
    protected Task collectRecipeSubTask() {
        // Default, just go through the recipe slots and collect the first one.
        for (int i = 0; i < _recipe.getSlotCount(); ++i) {
            CraftingRecipe.CraftingSlot slot = _recipe.getSlot(i);
            if (slot.getTargetItems().size() > 1) {
                Debug.logWarning("Recipe collection for recipe " + _recipe + " slot " + i
                        + " has multiple options, picking the first. Please define an explicit"
                        + " collectRecipeSubTask() function for this task."
                );
            }
            Optional<Item> item = slot.getTargetItems().stream().findFirst();
            if (item.isPresent()) {
                item.get();
            }
        }

        return null;
    }
    protected abstract String toTaskDebugString();
    protected abstract boolean isTaskEqual(CraftInInventoryTask other);
}

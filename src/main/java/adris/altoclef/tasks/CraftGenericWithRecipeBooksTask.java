package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.slot.ReceiveOutputSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;

public class CraftGenericWithRecipeBooksTask extends Task {

    private final RecipeTarget _target;

    public CraftGenericWithRecipeBooksTask(RecipeTarget target) {
        _target = target;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        boolean bigCrafting = StorageHelper.isBigCraftingOpen();

        if (!bigCrafting && !StorageHelper.isPlayerInventoryOpen()) {
            // Make sure we're not in another screen before we craft,
            // otherwise crafting won't work
            StorageHelper.closeScreen();
            // Just to be safe
        }

        Slot outputSlot = bigCrafting ? CraftingTableSlot.OUTPUT_SLOT : PlayerSlot.CRAFT_OUTPUT_SLOT;
        if (!StorageHelper.getItemStackInSlot(outputSlot).isEmpty()) {
            setDebugState("Getting output");
            return new ReceiveOutputSlotTask(outputSlot, _target.getTargetCount());
        }

        // Request to fill in a recipe. Just piggy back off of the slot delay system.
        if (mod.getSlotHandler().canDoSlotAction()) {
            mod.getSlotHandler().registerSlotAction();
            StorageHelper.instantFillRecipeViaBook(mod, _target.getRecipe(), _target.getOutputItem(), true);
        }

        setDebugState("Waiting for recipe book click...");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CraftGenericWithRecipeBooksTask task) {
            return task._target.equals(_target);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Crafting (w/ RECIPE): " + _target;
    }
}

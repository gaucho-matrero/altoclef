package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.slot.ClickSlotTask;
import adris.altoclef.tasks.slot.ReceiveCraftingOutputSlotTask;
import adris.altoclef.tasks.slot.ThrowCursorTask;
import adris.altoclef.tasksystem.ITaskUsesCraftingGrid;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.Optional;

public class CraftGenericWithRecipeBooksTask extends Task implements ITaskUsesCraftingGrid {

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
        ItemStack output = StorageHelper.getItemStackInSlot(outputSlot);
        if (_target.getOutputItem() == output.getItem() && mod.getItemStorage().getItemCount(_target.getOutputItem()) < _target.getTargetCount()) {
            setDebugState("Getting output");
            return new ReceiveCraftingOutputSlotTask(outputSlot, _target.getTargetCount());
        }  // TODO Migrate this back to Craft In Inventory

        // If a material is found in cursor, move it to the inventory.
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();


        // Crafting book REQUIRES that all materials be in the inventory. Materials CANNOT be in the cursor
        if (Arrays.stream(_target.getRecipe().getSlots()).anyMatch(target -> target.matches(cursor.getItem()))) {
            setDebugState("CURSOR HAS MATERIAL! Moving out.");
            Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false).or(() -> StorageHelper.getGarbageSlot(mod));
            if (toMoveTo.isPresent()) {
                return new ClickSlotTask(toMoveTo.get());
            } else {
                // If our inventory is full, but the material will be used for crafting, put it in the crafting grid.
                // TODO: Add as an alternative method to EnsureCursorSlotTask
                for (int recSlot = 0; recSlot < _target.getRecipe().getSlotCount(); ++recSlot) {
                    if (_target.getRecipe().getSlot(recSlot).matches(cursor.getItem())) {
                        Slot toMoveToPotential = bigCrafting? CraftingTableSlot.getInputSlot(recSlot, _target.getRecipe().isBig()) : PlayerSlot.getCraftInputSlot(recSlot);
                        ItemStack inRecipe = StorageHelper.getItemStackInSlot(toMoveToPotential);
                        if (ItemHelper.canStackTogether(cursor, inRecipe)) {
                            return new ClickSlotTask(toMoveToPotential);
                        }
                    }
                }
                // Crafting grid is full, and we still have the item, throw it away.
                return new ThrowCursorTask();
            }
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

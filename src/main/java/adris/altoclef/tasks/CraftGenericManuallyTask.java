package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.slot.ClickSlotTask;
import adris.altoclef.tasks.slot.MoveItemToSlotFromInventoryTask;
import adris.altoclef.tasks.slot.ReceiveCraftingOutputSlotTask;
import adris.altoclef.tasks.slot.ThrowCursorTask;
import adris.altoclef.tasksystem.ITaskUsesCraftingGrid;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Optional;

/**
 * Assuming a crafting screen is open, crafts a recipe.
 *
 * Not useful for custom tasks.
 */
public class CraftGenericManuallyTask extends Task implements ITaskUsesCraftingGrid {

    private final RecipeTarget _target;

    public CraftGenericManuallyTask(RecipeTarget target) {
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

        // Example:
        // We need 9 sticks
        // plank recipe results in 4 sticks
        // this means 3 planks per slot
        int requiredPerSlot = (int)Math.ceil((double) _target.getTargetCount() / _target.getRecipe().outputCount());

        // For each slot in table
        for (int craftSlot = 0; craftSlot < _target.getRecipe().getSlotCount(); ++craftSlot) {
            ItemTarget toFill = _target.getRecipe().getSlot(craftSlot);
            Slot currentCraftSlot;
            if (bigCrafting) {
                // Craft in table
                currentCraftSlot = CraftingTableSlot.getInputSlot(craftSlot, _target.getRecipe().isBig());
            } else {
                // Craft in window
                currentCraftSlot = PlayerSlot.getCraftInputSlot(craftSlot);
            }
            ItemStack present = StorageHelper.getItemStackInSlot(currentCraftSlot);
            if (toFill == null || toFill.isEmpty()) {
                if (present.getItem() != Items.AIR) {
                    // Move this item OUT if it should be empty
                    setDebugState("Found INVALID slot");
                    return new ClickSlotTask(currentCraftSlot);
                }
            } else {
                boolean correctItem = toFill.matches(present.getItem());
                boolean isSatisfied = correctItem && present.getCount() >= requiredPerSlot;
                if (!isSatisfied) {
                    // We have items that satisfy, but we CAN NOT fill in the current slot!
                    // In that case, just grab from the output.
                    if (!mod.getItemStorage().hasItemInventoryOnly(present.getItem())) {
                        if (!StorageHelper.getItemStackInSlot(outputSlot).isEmpty()) {
                            setDebugState("NO MORE to fit: grabbing from output.");
                            return new ReceiveCraftingOutputSlotTask(outputSlot, _target.getTargetCount());
                        } else {
                            // Move on to the NEXT slot, we can't fill this one anymore.
                            continue;
                        }
                    }

                    setDebugState("Moving item to slot...");
                    return new MoveItemToSlotFromInventoryTask(new ItemTarget(toFill, requiredPerSlot), currentCraftSlot);
                }
                // We could be OVER satisfied
                boolean oversatisfies = present.getCount() > requiredPerSlot;
                if (oversatisfies) {
                    setDebugState("OVER SATISFIED slot! Right clicking slot to extract half and spread it out more.");
                    return new ClickSlotTask(currentCraftSlot, 1);
                }
            }
        }

        // Ensure our cursor is empty/can receive our item
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (!ItemHelper.canStackTogether(StorageHelper.getItemStackInSlot(outputSlot), cursor)) {
            Optional<Slot> toFit = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false).or(() -> StorageHelper.getGarbageSlot(mod));
            if (toFit.isPresent()) {
                return new ClickSlotTask(toFit.get());
            } else {
                // Eh screw it
                return new ThrowCursorTask();
            }
        }

        if (!StorageHelper.getItemStackInSlot(outputSlot).isEmpty()) {
            return new ReceiveCraftingOutputSlotTask(outputSlot, _target.getTargetCount());
        } else {
            // Wait
            return null;
        }
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CraftGenericManuallyTask task) {
            return task._target.equals(_target);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Crafting: " + _target;
    }
}

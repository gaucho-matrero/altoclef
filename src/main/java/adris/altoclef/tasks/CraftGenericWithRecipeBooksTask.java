package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.slot.EnsureFreePlayerCraftingGridTask;
import adris.altoclef.tasks.slot.ReceiveCraftingOutputSlotTask;
import adris.altoclef.tasksystem.ITaskUsesCraftingGrid;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

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
            ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
            if (!cursorStack.isEmpty()) {
                Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
                if (moveTo.isPresent()) {
                    mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                    return null;
                }
                if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    return null;
                }
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
                // Try throwing away cursor slot if it's garbage
                if (garbage.isPresent()) {
                    mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                    return null;
                }
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            } else {
                StorageHelper.closeScreen();
            }
        }
        Slot outputSlot = bigCrafting ? CraftingTableSlot.OUTPUT_SLOT : PlayerSlot.CRAFT_OUTPUT_SLOT;
        ItemStack output = StorageHelper.getItemStackInSlot(outputSlot);
        if (_target.getOutputItem() == output.getItem() && mod.getItemStorage().getItemCount(_target.getOutputItem()) <
                _target.getTargetCount()) {
            setDebugState("Getting output.");
            return new ReceiveCraftingOutputSlotTask(outputSlot, _target.getTargetCount());
        }
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            if (moveTo.isPresent()) {
                mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return null;
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            if (garbage.isPresent()) {
                mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            return null;
        }
        if (!bigCrafting) {
            PlayerSlot[] playerInputSlot = PlayerSlot.CRAFT_INPUT_SLOTS;
            for (PlayerSlot PlayerInputSlot : playerInputSlot) {
                ItemStack playerInput = StorageHelper.getItemStackInSlot(PlayerInputSlot);
                if (!playerInput.isEmpty()) {
                    return new EnsureFreePlayerCraftingGridTask();
                }
            }
        }
        setDebugState("Crafting.");
        if (mod.getSlotHandler().canDoSlotAction()) {
            StorageHelper.instantFillRecipeViaBook(mod, _target.getRecipe(), _target.getOutputItem(), true);
            mod.getSlotHandler().registerSlotAction();
        }
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

package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;

public class ReceiveOutputSlotTask extends Task {

    private final int _toTake;
    private final Slot _slot;

    public ReceiveOutputSlotTask(Slot slot, int toTake) {
        _slot = slot;
        _toTake = toTake;
    }
    public ReceiveOutputSlotTask(Slot slot, boolean all) {
        this(slot, all? Integer.MAX_VALUE : 1);
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        ItemStack inOutput = StorageHelper.getItemStackInSlot(_slot);
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (!cursor.isEmpty() && !ItemHelper.canStackTogether(inOutput, cursor)) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
            if (moveTo.isPresent()) {
                setDebugState("Moving cursor stack back");
                return new ClickSlotTask(moveTo.get());
            }
            if (ItemHelper.canThrowAwayStack(mod, cursor)) {
                setDebugState("Incompatible cursor stack, throwing");
                return new ThrowCursorTask();
            } else {
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
                if (garbage.isPresent()) {
                    setDebugState("Picking up garbage");
                    return new ClickSlotTask(garbage.get());
                } else {
                    setDebugState("STUCK! Everything's protected.");
                    return null;
                }
            }
        }
        int craftCount = inOutput.getCount() * getCraftMultipleCount(mod);
        int weWantToAddToInventory = _toTake - mod.getItemStorage().getItemCountInventoryOnly(inOutput.getItem());
        boolean takeAll = weWantToAddToInventory >= craftCount;
        if (takeAll && mod.getItemStorage().getSlotThatCanFitInPlayerInventory(inOutput, true).isPresent()) {
            setDebugState("Quick moving output");
            return new ClickSlotTask(_slot, SlotActionType.QUICK_MOVE);
        }
        setDebugState("Picking up output");
        return new ClickSlotTask(_slot);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof ReceiveOutputSlotTask task) {
            return task._slot.equals(_slot) && task._toTake == _toTake;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Receiving output";
    }

    // How many multiples of the current crafting recipe can we craft?
    private static int getCraftMultipleCount(AltoClef mod) {
        int minNonZero = Integer.MAX_VALUE;
        boolean found = false;
        for (Slot check : (StorageHelper.isBigCraftingOpen()? CraftingTableSlot.INPUT_SLOTS :PlayerSlot.CRAFT_INPUT_SLOTS)) {
            ItemStack stack = StorageHelper.getItemStackInSlot(check);
            if (!stack.isEmpty()) {
                minNonZero = Math.min(stack.getCount(), minNonZero);
                found = true;
            }
        }
        if (!found)
            return 0;
        return minNonZero;
    }
}

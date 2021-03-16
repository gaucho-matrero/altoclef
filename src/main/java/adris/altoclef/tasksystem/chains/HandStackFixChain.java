package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.slots.PlayerInventorySlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class HandStackFixChain extends TaskChain {

    private ItemStack _lastHandStack = null;
    private final Timer _stackHeldTimeout = new Timer(8);

    private final Timer _generalDuctTapeSwapTimeout = new Timer(30);

    public HandStackFixChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onStop(AltoClef mod) {

    }

    @Override
    public void onInterrupt(AltoClef mod, TaskChain other) {

    }

    @Override
    protected void onTick(AltoClef mod) {
    }

    @Override
    public float getPriority(AltoClef mod) {

        if (_generalDuctTapeSwapTimeout.elapsed()) {
            if (!mod.getController().isBreakingBlock()) {
                Debug.logMessage("Refreshed inventory...");
                mod.getInventoryTracker().refreshInventory();
                _generalDuctTapeSwapTimeout.reset();
                return Float.NEGATIVE_INFINITY;
            }
        }

        ItemStack currentStack = mod.getPlayer().inventory.getCursorStack();

        if (currentStack != null && !currentStack.isEmpty()) {
            //noinspection PointlessNullCheck
            if (_lastHandStack == null || !ItemStack.areEqual(currentStack, _lastHandStack)) {
                // We're holding a new item in our stack!
                _stackHeldTimeout.reset();
                _lastHandStack = currentStack;
            }
        } else {
            _lastHandStack = null;
        }

        // If we have something in our hand for a period of time...
        if (_lastHandStack != null && _stackHeldTimeout.elapsed()) {
            Debug.logMessage("Cursor stack is held for too long, will move back to inventory.");
            if (mod.getInventoryTracker().isInventoryFull()) {
                if (!ResourceTask.ensureInventoryFree(mod)) {
                    Debug.logWarning("Failed to free full inventory. This could be problematic.");
                }
            }
            int slotToMoveTo;
            List<Integer> slots = mod.getInventoryTracker().getEmptyInventorySlots();
            if (slots.size() == 0) {
                Debug.logWarning("No free slot found, moving item stack to last inventory slot.");
                slotToMoveTo = 35;
            } else {
                slotToMoveTo = slots.get(0);
            }
            mod.getInventoryTracker().clickSlot(PlayerInventorySlot.getFromInventory(slotToMoveTo));
        }

        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String getName() {
        return "Hand Stack Fix Chain";
    }
}

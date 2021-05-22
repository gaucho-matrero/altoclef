package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.slots.PlayerInventorySlot;
import adris.altoclef.util.slots.Slot;
import baritone.utils.ToolSet;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;

import java.util.List;


public class PlayerInteractionFixChain extends TaskChain {
    
    private final Timer _stackHeldTimeout = new Timer(8);
    private final Timer _generalDuctTapeSwapTimeout = new Timer(30);
    private final Timer _shiftDepressTimeout = new Timer(10);
    private final Timer _betterToolTimer = new Timer(0.5);
    private ItemStack _lastHandStack = null;
    
    public PlayerInteractionFixChain(TaskRunner runner) {
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
        
        if (!mod.inGame()) return Float.NEGATIVE_INFINITY;
        
        if (_betterToolTimer.elapsed()) {
            // Equip the right tool for the job if we're not using one.
            _betterToolTimer.reset();
            if (mod.getControllerExtras().isBreakingBlock()) {
                BlockState state = mod.getWorld().getBlockState(mod.getControllerExtras().getBreakingBlockPos());
                Slot bestToolSlot = null;
                double highestSpeed = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < InventoryTracker.INVENTORY_SIZE; ++i) {
                    Slot slot = PlayerInventorySlot.getFromInventory(i);
                    ItemStack stack = mod.getInventoryTracker().getItemStackInSlot(slot);
                    if (stack.getItem() instanceof ToolItem) {
                        double speed = ToolSet.calculateSpeedVsBlock(stack, state);
                        if (speed > highestSpeed) {
                            highestSpeed = speed;
                            bestToolSlot = slot;
                        }
                    }
                    if (stack.getItem() == Items.SHEARS) {
                        // Shears take priority over leaf blocks.
                        if (ToolSet.areShearsEffective(state.getBlock())) {
                            bestToolSlot = slot;
                            break;
                        }
                    }
                }
                
                // Only accept tools OUTSIDE OF HOTBAR!
                // Baritone will take care of tools inside the hotbar.
                if (bestToolSlot != null && bestToolSlot.getInventorySlot() >= 9) {
                    Debug.logMessage("Found better tool in inventory, equipping.");
                    mod.getInventoryTracker().equipSlot(bestToolSlot);
                }
            }
        }
        
        // Unpress shift (it gets stuck for some reason???)
        if (MinecraftClient.getInstance().options.keySneak.isPressed()) {
            if (_shiftDepressTimeout.elapsed()) {
                Debug.logMessage("Unpressing shift/sneak");
                MinecraftClient.getInstance().options.keySneak.setPressed(false);
            }
        } else {
            _shiftDepressTimeout.reset();
        }
        
        // Refresh inventory
        if (_generalDuctTapeSwapTimeout.elapsed()) {
            if (!mod.getControllerExtras().isBreakingBlock()) {
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

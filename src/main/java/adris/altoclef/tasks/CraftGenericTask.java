package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.slot.ClickSlotTask;
import adris.altoclef.tasks.slot.MoveItemToSlotTask;
import adris.altoclef.tasks.slot.ThrowSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Assuming a crafting screen is open, crafts a recipe.
 *
 * Not useful for custom tasks.
 */
public class CraftGenericTask extends Task {

    private final CraftingRecipe _recipe;
    private TimerGame _invTimer;

    public CraftGenericTask(CraftingRecipe recipe) {
        _recipe = recipe;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_invTimer == null) {
            _invTimer = new TimerGame(mod.getModSettings().getContainerItemMoveDelay());
        } else {
            _invTimer.setInterval(mod.getModSettings().getContainerItemMoveDelay());
        }
        boolean delayedCraft = (_invTimer.getDuration() > 0);

        if (!_invTimer.elapsed()) {
            // Each "tick" past here is one operation.
            // Wait until timer comes back.
            return null;
        } else {
            _invTimer.reset();
        }

        boolean bigCrafting = (mod.getPlayer().currentScreenHandler instanceof CraftingScreenHandler);

        if (!bigCrafting) {
            if (!(mod.getPlayer().currentScreenHandler instanceof PlayerScreenHandler)) {
                // Make sure we're not in another screen before we craft,
                // otherwise crafting will be むだな、ぞ
                mod.getControllerExtras().closeScreen();
                // Just to be safe
                if (delayedCraft) return null;
            }
        }

        // For each slot in table
        for (int craftSlot = 0; craftSlot < _recipe.getSlotCount(); ++craftSlot) {
            ItemTarget toFill = _recipe.getSlot(craftSlot);
            Slot currentCraftSlot;
            if (bigCrafting) {
                // Craft in table
                currentCraftSlot = CraftingTableSlot.getInputSlot(craftSlot, _recipe.isBig());
            } else {
                // Craft in window
                currentCraftSlot = PlayerSlot.getCraftInputSlot(craftSlot);
            }
            ItemStack present = mod.getInventoryTracker().getItemStackInSlot(currentCraftSlot);
            if (toFill == null || toFill.isEmpty()) {
                if (present.getItem() != Items.AIR) {
                    // Move this item OUT if it should be empty
                    return new ThrowSlotTask(currentCraftSlot);
                }
            } else {
                boolean isSatisfied = toFill.matches(present.getItem());
                if (!isSatisfied) {
                    return new MoveItemToSlotTask(new ItemTarget(toFill, 1), currentCraftSlot);
                }
            }
        }

        Slot outputSlot = bigCrafting ? CraftingTableSlot.OUTPUT_SLOT : PlayerSlot.CRAFT_OUTPUT_SLOT;

        return new ClickSlotTask(outputSlot, 0, SlotActionType.QUICK_MOVE);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CraftGenericTask) {
            return ((CraftGenericTask) other)._recipe.equals(_recipe);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Crafting " + _recipe.toString();
    }
}

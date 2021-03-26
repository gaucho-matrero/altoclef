package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.CraftInTableTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;

import java.util.List;

public class CollectSignTask extends ResourceTask {

    private final int _count;

    public CollectSignTask(int count) {
        super(new ItemTarget(ItemTarget.WOOD_SIGN, count));
        _count = count;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        int signsCurrent = mod.getInventoryTracker().getItemCount(new ItemTarget("sign"));
        int neededSticks = (int)(Math.floor((float)(_count - signsCurrent) / 3 - 0.1)) + 1;
        int neededPlanks = (int)(Math.floor((float)(_count - signsCurrent) / 3 - 0.1) + 1) * 6;

        //Debug.logMessage("(" + signsCurrent + ") :" + neededPlanks + " : " + neededSticks);

        // These will be squashed together
        ItemTarget stickGet = null;

        // Collect sticks.
        if (getItemCountIncludingTable(mod, Items.STICK) < neededSticks) {
            //Debug.logMessage("NEED " + neededSticks + " STICKS (has " + mod.getInventoryTracker().getItemCount(Items.STICK) + ")");
            stickGet = TaskCatalogue.getItemTarget("stick", neededSticks);
            //return TaskCatalogue.getItemTask("stick", neededSticks);
        }

        // Collect planks
        Item fittingPlankCandidate = null;
        Item fittingLogCandidate = null;
        int bestPotential = 0;
        for (Item plankType : ItemTarget.PLANKS) {
            int potentialCount = getItemCountIncludingTable(mod, plankType);
            Item logCandidate = ItemTarget.planksToItem(plankType);
            potentialCount += getItemCountIncludingTable(mod, logCandidate) * 4;
            if (potentialCount >= neededPlanks) {
                if (potentialCount > bestPotential) {
                    fittingPlankCandidate = plankType;
                    fittingLogCandidate = logCandidate;
                    bestPotential = potentialCount;
                }
            }
        }

        if (fittingPlankCandidate == null) {
            // We need planks in general. Keep collecting logs.
            return TaskCatalogue.getItemTask(new ItemTarget("log"));
        } else {
            // We have a candidate to go with that has enough planks to craft our target.
            // If we don't have enough planks, keep crafting them.
            if (getItemCountIncludingTable(mod, fittingPlankCandidate) < neededPlanks) {
                if (getItemCountIncludingTable(mod, fittingLogCandidate) > 0) {
                    // Craft logs to planks
                    ItemTarget empty = null;
                    setDebugState("Converting planks into logs");
                    return new CraftInInventoryTask(new ItemTarget(fittingPlankCandidate, 1), CraftingRecipe.newShapedRecipe("planks", new ItemTarget[]{new ItemTarget(fittingLogCandidate, 1), empty, empty, empty}, 4), false);
                } else {
                    Debug.logWarning("This shouldn't happen: We claim to have enough wood resources but don't have enough planks and have NO logs.");
                }
            }
            //Debug.logMessage("NEED " + neededPlanks + " PLANKS (has " + mod.getInventoryTracker().getItemCount(fittingPlankCandidate));
        }

        // If we need resources, get em.
        if (stickGet != null) {
            setDebugState("Collecting sticks");
            return TaskCatalogue.getItemTask(stickGet);
        }

        // If we do have it, return craft in inventory task for a generated recipe of that type of plank.

        Item p = fittingPlankCandidate;
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(fittingPlankCandidate.getTranslationKey() + " sign", new ItemTarget[] {t(p), t(p), t(p), t(p), t(p), t(p), null, t("stick"), null}, 3);

        setDebugState("Crafting sign in table");
        return new CraftInTableTask(new ItemTarget("sign", _count), recipe, false);
    }

    private static ItemTarget t(Item item) {
        return new ItemTarget(item, 1);
    }
    private static ItemTarget t(String item) {
        return new ItemTarget(item, 1);
    }

    // TODO: Collect bed will also use this. Make this a method in inventoryTracker or something?
    private int getItemCountIncludingTable(AltoClef mod, Item item) {
        int result = mod.getInventoryTracker().getItemCount(item);
        ScreenHandler screen = mod.getPlayer().currentScreenHandler;
        if (screen instanceof PlayerScreenHandler || screen instanceof CraftingScreenHandler) {
            boolean bigCrafting = (screen instanceof CraftingScreenHandler);
            for (int craftSlotIndex = 0; craftSlotIndex < (bigCrafting ? 9 : 4); ++craftSlotIndex) {
                Slot craftSlot = bigCrafting ? CraftingTableSlot.getInputSlot(craftSlotIndex, true) : PlayerSlot.getCraftInputSlot(craftSlotIndex);
                ItemStack stack = mod.getInventoryTracker().getItemStackInSlot(craftSlot);
                if (stack.getItem() == item) {
                    result += stack.getCount();
                }
            }
        }
        return result;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        if (obj instanceof CollectSignTask) {
            CollectSignTask st = (CollectSignTask) obj;
            return st._count == _count;
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect " + _count + " signs";
    }
}

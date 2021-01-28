package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.CraftInTableTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

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

        Debug.logMessage("(" + signsCurrent + ") :" + neededPlanks + " : " + neededSticks);

        // These will be squashed together
        ItemTarget stickGet = null;
        ItemTarget plankGet = null;

        // Collect sticks.
        if (mod.getInventoryTracker().getItemCount(Items.STICK) < neededSticks) {
            Debug.logMessage("NEED " + neededSticks + " STICKS");
            stickGet = TaskCatalogue.getItemTarget("stick", neededSticks);
            //return TaskCatalogue.getItemTask("stick", neededSticks);
        }

        // Collect planks
        Item hasEnough = null;
        for (Item plankType : ItemTarget.PLANKS) {
            if (mod.getInventoryTracker().getItemCount(plankType) >= neededPlanks) {
                hasEnough = plankType;
                break;
            }
        }
        if (hasEnough == null) {
            Debug.logMessage("NEED " + neededPlanks + " PLANKS");
            // We need planks!
            plankGet = new ItemTarget("planks"); // get infinity cause we will catch our target above.
        }

        // If we need resources, get em.
        if (stickGet != null || plankGet != null) {
            return TaskCatalogue.getSquashedItemTask(stickGet, plankGet);
        }

        // If we do have it, return craft in inventory task for a generated recipe of that type of plank.

        Item p = hasEnough;
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(hasEnough.getTranslationKey() + " sign", new ItemTarget[] {t(p), t(p), t(p), t(p), t(p), t(p), null, t("stick"), null}, 3);

        return new CraftInTableTask(new ItemTarget("sign", _count), recipe, false);
    }

    private static ItemTarget t(Item item) {
        return new ItemTarget(item, 1);
    }
    private static ItemTarget t(String item) {
        return new ItemTarget(item, 1);
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

package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.CraftInTableTask;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.item.Item;


public class CollectBedTask extends ResourceTask {

    private final int _count;

    public static final Block[] BEDS = Util.itemsToBlocks(ItemTarget.BED);

    public CollectBedTask(int count) {
        super(new ItemTarget(ItemTarget.BED, count));
        _count = count;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(BEDS);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Override
    protected Task onResourceTick(AltoClef mod) {

        // Break beds from the world if possible, that would be pretty fast.
        if (mod.getBlockTracker().anyFound(BEDS)) {
            // Failure + blacklisting is encapsulated within THIS task
            return new MineAndCollectTask(new ItemTarget(ItemTarget.BED, 1), BEDS, MiningRequirement.HAND);
        }

        int bedsCurrent = mod.getInventoryTracker().getItemCount(new ItemTarget("bed"));
        int neededPlanks = (_count - bedsCurrent) * 3;
        int neededWool = neededPlanks;

        ItemTarget plankGet = null;
        ItemTarget woolGet = null;

        // Collect planks.
        if (mod.getInventoryTracker().getItemCount(ItemTarget.PLANKS) < neededPlanks) {
            Debug.logMessage("NEED " + neededPlanks + " PLANKS");
            plankGet = TaskCatalogue.getItemTarget("planks", neededPlanks);
            //return TaskCatalogue.getItemTask("stick", neededSticks);
        }

        // Collect planks
        Item hasEnough = null;
        for (Item woolType : ItemTarget.WOOL) {
            if (mod.getInventoryTracker().getItemCount(woolType) >= neededWool) {
                hasEnough = woolType;
                break;
            }
        }
        if (hasEnough == null) {
            Debug.logMessage("NEED " + neededWool + " WOOL");
            // We need planks!
            woolGet = new ItemTarget("wool"); // get infinity cause we will catch our target above.
        }

        // If we need resources, get em.
        if (plankGet != null || woolGet != null) {
            return TaskCatalogue.getSquashedItemTask(plankGet, woolGet);
        }

        Item w = hasEnough;
        ItemTarget p = t("planks");
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe(hasEnough.getTranslationKey() + " bed", new ItemTarget[] {t(w), t(w), t(w), p, p, p, null, null, null}, 1);

        return new CraftInTableTask(new ItemTarget("bed", _count), recipe, false);
    }
    private static ItemTarget t(Item item) {
        return new ItemTarget(item, 1);
    }
    private static ItemTarget t(String item) {
        return new ItemTarget(item, 1);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(BEDS);
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        return obj instanceof CollectBedTask && ((CollectBedTask) obj)._count == _count;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect " + _count + " beds";
    }
}

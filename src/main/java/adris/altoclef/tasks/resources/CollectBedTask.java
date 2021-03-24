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
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;


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
            //Debug.logMessage("NEED " + neededPlanks + " PLANKS");
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
            // Check crafting table, we may have the wool in there already.
            ScreenHandler screen = mod.getPlayer().currentScreenHandler;
            int craftCount = 0;
            if (screen instanceof CraftingScreenHandler) {
                // Check crafting slots
                for (int craftSlotIndex = 0; craftSlotIndex < 9; ++craftSlotIndex) {
                    Slot craftSlot = CraftingTableSlot.getInputSlot(craftSlotIndex, true);
                    ItemStack stack = mod.getInventoryTracker().getItemStackInSlot(craftSlot);
                    if (Util.arrayContains(ItemTarget.WOOL, stack.getItem())) {
                        if (hasEnough == null) {
                            hasEnough = stack.getItem();
                        } else {
                            if (hasEnough != stack.getItem()) {
                                // We tried mixing wool, this is bad.
                                //Debug.logMessage("FAIL: Mixed " + hasEnough.getTranslationKey() + " : " + stack.getItem().getTranslationKey());
                                hasEnough = null;
                                break;
                            }
                        }
                        craftCount++;
                    }
                }
                if (hasEnough != null) {
                    // make sure our inventory has the right remaining number of wool!
                    int invCount = mod.getInventoryTracker().getItemCount(hasEnough);
                    if (invCount + craftCount < 3) {
                        //Debug.logMessage("FAIL: craft:" + craftCount + " + inv:" + invCount + " < 3");
                        hasEnough = null;
                    }
                }
            }
            //Debug.logMessage("NEED " + neededWool + " WOOL");
            // We need planks!
            woolGet = new ItemTarget("wool"); // get infinity cause we will catch our target above.
        }

        Item w = hasEnough;
        ItemTarget p = t("planks");
        CraftingRecipe recipe = CraftingRecipe.newShapedRecipe("bed", new ItemTarget[] {t(w), t(w), t(w), p, p, p, null, null, null}, 1);


        // If we need resources, get em.
        if (plankGet != null || woolGet != null) {
            RecipeTarget target = new RecipeTarget(new ItemTarget("bed", 1), recipe);
            if (!mod.getInventoryTracker().hasRecipeMaterialsOrTarget(target)) {
                // ^ Above check must be made because we may craft a crafting table in the midst of this.
                return TaskCatalogue.getSquashedItemTask(plankGet, woolGet);
            }
        }

        return new CraftInTableTask(new ItemTarget("bed", _count), recipe, false);
    }
    private static ItemTarget t(Item item) {
        assert item != null;
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

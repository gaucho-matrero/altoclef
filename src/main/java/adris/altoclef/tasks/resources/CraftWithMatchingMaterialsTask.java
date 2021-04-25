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
import net.minecraft.item.Item;

public abstract class CraftWithMatchingMaterialsTask extends ResourceTask {

    private final ItemTarget _target;
    private final CraftingRecipe _recipe;
    private final boolean[] _sameMask;

    private final ItemTarget _sameResourceTarget;
    private final int _sameResourceRequiredCount;
    private final int _sameResourcePerRecipe;

    public CraftWithMatchingMaterialsTask(ItemTarget target, CraftingRecipe recipe, boolean[] sameMask) {
        super(target);
        _target = target;
        _recipe = recipe;
        _sameMask = sameMask;
        int sameResourceRequiredCount = 0;
        ItemTarget sameResourceTarget = null;
        if (recipe.getSlotCount() != sameMask.length) {
            Debug.logError("Invalid CraftWithMatchingMaterialsTask constructor parameters: Recipe size must equal \"sameMask\" size.");
        }
        for (int i = 0; i < recipe.getSlotCount(); ++i) {
            if (sameMask[i]) {
                sameResourceRequiredCount++;
                ItemTarget t = recipe.getSlot(i);
                sameResourceTarget = t;
            }
        }
        _sameResourceTarget = sameResourceTarget;
        int craftsNeeded = (int)(1 + Math.floor((double)target.targetCount / recipe.outputCount() - 0.001));
        _sameResourcePerRecipe = sameResourceRequiredCount;
        _sameResourceRequiredCount = sameResourceRequiredCount * craftsNeeded;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        // TODO: Scenario of
        //      Command: Get 3 beds
        //
        //      We have 3 red wool
        //      We have 6 white wool
        //
        //      The system should craft 1 red bed + 2 white beds
        //      BUT since it needs 9 of the --SAME WOOL-- it keeps going.
        //      You should MAP for each type how many can fit into --_sameResourcePerRecipe-- and grab from THAT list.

        /*
         * 0) Figure out the "same resource" item target
         * 1) Get the "same resource" item matches array
         * 2) Figure out how many of the "same resource" we need
         * 3) Get the most frequent occurrence of said material
         * 4) If the most frequent occurrence is NOT met, return TaskCatalogue.getItemTask("same resource" item target)
         * 5) If the most frequent occurrence IS met, run CraftInTable with a custom recipe that only has the frequent material.`
         */

        Item mostCommonSameResource = null;
        int sameResourceCount = 0;
        for (Item sameCheck : _sameResourceTarget.getMatches()) {
            int count = getExpectedTotalCountOfSameItem(mod, sameCheck);
            if (count > sameResourceCount) {
                sameResourceCount = count;
                mostCommonSameResource = sameCheck;
            }
        }

        // If we already have some of our target, we need less "same" materials.
        int currentTargetCount = mod.getInventoryTracker().getItemCount(_target);
        int currentlyRequired = _sameResourceRequiredCount - currentTargetCount*_sameResourcePerRecipe;

        if (sameResourceCount >= currentlyRequired) {
            // We have enough of the same resource!!!
            // Handle crafting normally.

            int trueCount = mod.getInventoryTracker().getItemCountIncludingTable(false, mostCommonSameResource);
            if (trueCount < currentlyRequired) {
                return getSpecificSameResourceTask(mod, mostCommonSameResource);
            }

            CraftingRecipe samedRecipe = generateSamedRecipe(_recipe, mostCommonSameResource, _sameMask);
            return _recipe.isBig()? new CraftInTableTask(_target, samedRecipe, true, true) : new CraftInInventoryTask(_target, samedRecipe, true, true);
        }
        // Collect SAME resources first!!!
        return getAllSameResourcesTask(mod);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    // Virtual
    protected Task getAllSameResourcesTask(AltoClef mod) {
        if (_sameResourceTarget.isCatalogueItem()) {
            ItemTarget infinityVersion = new ItemTarget(_sameResourceTarget.getCatalogueName());
            return TaskCatalogue.getItemTask(infinityVersion);
        }
        Debug.logWarning("ItemTarget for same resource is not catalogued: " + _sameResourceTarget.toString());
        return null;
    }

    // Virtual
    protected int getExpectedTotalCountOfSameItem(AltoClef mod, Item sameItem) {
        return mod.getInventoryTracker().getItemCountIncludingTable(sameItem);
    }

    // Virtual
    protected Task getSpecificSameResourceTask(AltoClef mod, Item toGet) {
        Debug.logError("Uh oh!!! getSpecificSameResourceTask should be implemented!!!! Now we're stuck.");
        return null;
    }

    private static CraftingRecipe generateSamedRecipe(CraftingRecipe diverseRecipe, Item sameItem, boolean[] sameMask) {
        ItemTarget[] result = new ItemTarget[diverseRecipe.getSlotCount()];
        for (int i = 0; i < result.length; ++i) {
            if (sameMask[i]) {
                result[i] = new ItemTarget(sameItem, 1);
            } else {
                result[i] = diverseRecipe.getSlot(i);
            }
        }
        return CraftingRecipe.newShapedRecipe(result, diverseRecipe.outputCount());
    }
}

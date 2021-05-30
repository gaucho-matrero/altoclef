package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.ItemUtil;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.screen.CraftingScreenHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


class DoCraftInTableTask extends DoStuffInContainerTask {
    private final RecipeTarget[] targets;
    private final boolean collect;
    private final CollectRecipeCataloguedResourcesTask collectTask;
    private final Timer craftResetTimer = new Timer(10);
    private boolean fullCheckSucceeded = true;
    private int craftCount;

    public DoCraftInTableTask(RecipeTarget[] targets, boolean collect, boolean ignoreUncataloguedSlots) {
        super(Blocks.CRAFTING_TABLE, "crafting_table");
        collectTask = new CollectRecipeCataloguedResourcesTask(ignoreUncataloguedSlots, targets);
        this.targets = targets;
        this.collect = collect;
    }

    public DoCraftInTableTask(RecipeTarget[] targets) {
        this(targets, true, false);
    }

    @Override
    protected void onStart(AltoClef mod) {
        super.onStart(mod);
        craftCount = 0;
        mod.getPlayer().closeHandledScreen();
        mod.getConfigState().push();
        mod.getConfigState().addProtectedItems(getMaterialsArray());
        fullCheckSucceeded = true;

        // Reset our "finished" value in the collect recipe thing.
        collectTask.reset();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // TODO: This shouldn't be here.
        // This is duct tape for the following scenario:
        //
        //      The Collect Recipe Resources task does NOT actually grab all of the resources we "claim" to need.
        //      It will finish while we STILL need resources.
        //
        //
        //      When is this OK?
        //
        //      Only if we ASSUME that hasRecipeMaterials is TOO STRICT and the Collect Task is CORRECT.
        //
        if (collect) {
            if (!collectTask.isFinished(mod)) {

                if (!mod.getInventoryTracker().hasRecipeMaterialsOrTarget(targets)) {
                    setDebugState("craft does NOT have RECIPE MATERIALS: " + Util.arrayToString(targets));
                    return collectTask;
                }
            }
        }
        /*
        // Collect recipe materials first
        for(RecipeTarget target : _targets) {
            if (!mod.getInventoryTracker().hasRecipeMaterials(target.getRecipe())) {
                setDebugState("Collecting materials");
                return new CollectRecipeCataloguedResourcesTask(target.getRecipe());
            }
        }
         */

        if (!isContainerOpen(mod)) {
            craftResetTimer.reset();
        }

        return super.onTick(mod);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        super.onStop(mod, interruptTask);
        mod.getConfigState().pop();
        if (mod.inGame()) {
            mod.getPlayer().closeHandledScreen();
        }
    }

    @Override
    protected boolean isSubTaskEqual(DoStuffInContainerTask obj) {
        if (obj instanceof DoCraftInTableTask) {
            DoCraftInTableTask other = (DoCraftInTableTask) obj;

            return Util.arraysEqual(other.targets, targets);
        }
        return false;
    }

    @Override
    protected boolean isContainerOpen(AltoClef mod) {
        return (mod.getPlayer().currentScreenHandler instanceof CraftingScreenHandler);
    }

    @Override
    protected Task containerSubTask(AltoClef mod) {
        //Debug.logMessage("GOT TO TABLE. Crafting...");

        // Already handled above...
        /*
        if (_collect) {
            for (RecipeTarget target : _targets) {
                if (!mod.getInventoryTracker().hasRecipeMaterialsOrTarget(target)) {
                    // Collect recipe materials
                    setDebugState("Collecting materials");
                    return new CollectRecipeCataloguedResourcesTask(_targets);
                }
            }
        }
         */

        if (craftResetTimer.elapsed()) {
            Debug.logMessage("Refreshing crafting table.");
            mod.getPlayer().closeHandledScreen();
            return null;
        }


        for (RecipeTarget target : targets) {

            if (!mod.getInventoryTracker().targetMet(target.getTargetItem())) {
                // Free up inventory
                if (mod.getInventoryTracker().isInventoryFull()) {
                    // Throw away!
                    Slot toThrow = mod.getInventoryTracker().getGarbageSlot();
                    if (toThrow != null) {
                        // Equip then throw
                        mod.getInventoryTracker().throwSlot(toThrow);
                    } else {
                        if (fullCheckSucceeded) {
                            Debug.logWarning("Failed to free up inventory as no throwaway-able slot was found. Awaiting user input.");
                        }
                        fullCheckSucceeded = false;
                    }
                }

                //Debug.logMessage("Crafting: " + target.getRecipe());
                return new CraftGenericTask(target.getRecipe());
                //craftInstant(mod, target.getRecipe());
            }
        }

        return null;
    }

    @Override
    protected double getCostToMakeNew(AltoClef mod) {
        // TODO: If we have an axe, lower the cost.
        if (mod.getInventoryTracker().hasItem(ItemUtil.LOG) || mod.getInventoryTracker().getItemCount(ItemUtil.PLANKS) >= 4) {
            // We can craft it right now, so it's real cheap
            return 150;
        }
        // TODO: If cached and the closest log is really far away, strike the price UP
        return 300;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return craftCount >= targets.length;//_crafted;
    }

    private Item[] getMaterialsArray() {
        List<Item> result = new ArrayList<>();
        for (RecipeTarget target : targets) {
            for (int i = 0; i < target.getRecipe().getSlotCount(); ++i) {
                ItemTarget materialTarget = target.getRecipe().getSlot(i);
                if (materialTarget == null || materialTarget.getMatches() == null) continue;
                Collections.addAll(result, materialTarget.getMatches());
            }
        }
        Item[] returnthing = new Item[result.size()];
        result.toArray(returnthing);
        return returnthing;
    }

}

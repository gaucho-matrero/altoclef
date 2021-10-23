package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.resources.CollectRecipeCataloguedResourcesTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.screen.CraftingScreenHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Crafts an item in a crafting table, obtaining and placing the table down if none was found.
 */
public class CraftInTableTask extends ResourceTask {

    private final RecipeTarget[] _targets;

    private final DoCraftInTableTask _craftTask;

    public CraftInTableTask(RecipeTarget[] targets) {
        super(extractItemTargets(targets));
        _targets = targets;
        _craftTask = new DoCraftInTableTask(_targets);
    }

    public CraftInTableTask(ItemTarget target, CraftingRecipe recipe, boolean collect, boolean ignoreUncataloguedSlots) {
        super(target);
        _targets = new RecipeTarget[]{new RecipeTarget(target, recipe)};
        _craftTask = new DoCraftInTableTask(_targets, collect, ignoreUncataloguedSlots);
    }

    public CraftInTableTask(ItemTarget target, CraftingRecipe recipe) {
        this(target, recipe, true, false);
    }

    public CraftInTableTask(Item item, int count, CraftingRecipe recipe) {
        this(new ItemTarget(item, count), recipe);
    }

    private static ItemTarget[] extractItemTargets(RecipeTarget[] recipeTargets) {
        List<ItemTarget> result = new ArrayList<>(recipeTargets.length);
        for (RecipeTarget target : recipeTargets) {
            result.add(target.getItem());
        }
        return result.toArray(ItemTarget[]::new);
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
        return _craftTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        // Close the crafting table screen
        if (mod.getPlayer() != null) {
            mod.getControllerExtras().closeScreen();
        }
        //mod.getControllerExtras().closeCurrentContainer();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CraftInTableTask task) {
            return _craftTask.isEqual(task._craftTask);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return _craftTask.toDebugString();
    }

    public RecipeTarget[] getRecipeTargets() {
        return _targets;
    }
}


class DoCraftInTableTask extends DoStuffInContainerTask {

    private final float CRAFT_RESET_TIMER_BONUS_SECONDS = 10;

    private final RecipeTarget[] _targets;

    private final boolean _collect;

    private final CollectRecipeCataloguedResourcesTask _collectTask;
    private final TimerGame _craftResetTimer = new TimerGame(CRAFT_RESET_TIMER_BONUS_SECONDS);
    private int _craftCount;

    public DoCraftInTableTask(RecipeTarget[] targets, boolean collect, boolean ignoreUncataloguedSlots) {
        super(Blocks.CRAFTING_TABLE, new ItemTarget("crafting_table"));
        _collectTask = new CollectRecipeCataloguedResourcesTask(ignoreUncataloguedSlots, targets);
        _targets = targets;
        _collect = collect;
    }

    public DoCraftInTableTask(RecipeTarget[] targets) {
        this(targets, true, false);
    }

    @Override
    protected void onStart(AltoClef mod) {
        super.onStart(mod);
        _craftCount = 0;
        mod.getControllerExtras().closeScreen();
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(getMaterialsArray());

        // Reset our "finished" value in the collect recipe thing.
        _collectTask.reset();
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        super.onStop(mod, interruptTask);
        mod.getBehaviour().pop();
        if (AltoClef.inGame()) {
            mod.getControllerExtras().closeScreen();
        }
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
        if (_collect) {
            if (!_collectTask.isFinished(mod)) {

                if (!mod.getInventoryTracker().hasRecipeMaterialsOrTarget(_targets)) {
                    setDebugState("craft does NOT have RECIPE MATERIALS: " + Arrays.toString(_targets));
                    return _collectTask;
                }
            }
        }

        if (!isContainerOpen(mod)) {
            _craftResetTimer.reset();
        }

        return super.onTick(mod);
    }

    @Override
    protected boolean isSubTaskEqual(DoStuffInContainerTask other) {
        if (other instanceof DoCraftInTableTask task) {
            return Arrays.equals(task._targets, _targets);
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

        _craftResetTimer.setInterval(mod.getModSettings().getContainerItemMoveDelay() * 10 + CRAFT_RESET_TIMER_BONUS_SECONDS);
        if (_craftResetTimer.elapsed()) {
            Debug.logMessage("Refreshing crafting table.");
            mod.getControllerExtras().closeScreen();
            return null;
        }

        for (RecipeTarget target : _targets) {
            if (mod.getInventoryTracker().targetsMet(target.getItem())) continue;
            if (mod.getInventoryTracker().isInventoryFull()) {
                setDebugState("Freeing inventory before crafting...");
                return new EnsureFreeInventorySlotTask();
            }
            setDebugState("Crafting");
            return new CraftGenericTask(target.getRecipe());
        }

        return null;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _craftCount >= _targets.length;//_crafted;
    }

    @Override
    protected double getCostToMakeNew(AltoClef mod) {
        // TODO: If we have an axe, lower the cost.
        if (mod.getInventoryTracker().hasItem(ItemHelper.LOG) || mod.getInventoryTracker().getItemCount(ItemHelper.PLANKS) >= 4) {
            // We can craft it right now, so it's real cheap
            return 15;
        }
        // TODO: If cached and the closest log is really far away, strike the price UP
        return 300;
    }

    private Item[] getMaterialsArray() {
        List<Item> result = new ArrayList<>();
        for (RecipeTarget target : _targets) {
            for (int i = 0; i < target.getRecipe().getSlotCount(); ++i) {
                ItemTarget materialTarget = target.getRecipe().getSlot(i);
                if (materialTarget == null || materialTarget.getMatches() == null) continue;
                Collections.addAll(result, materialTarget.getMatches());
            }
        }

        return result.toArray(Item[]::new);
    }

}

package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.resources.CollectRecipeCataloguedResourcesTask;
import adris.altoclef.tasks.slot.ClickSlotTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasks.slot.MoveItemToSlotTask;
import adris.altoclef.tasks.slot.ThrowSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.util.*;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.book.RecipeBook;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;

/**
 * Crafts an item within the 2x2 inventory crafting grid.
 */
public class CraftInInventoryTask extends ResourceTask {

    private final CraftingRecipe _recipe;
    private final boolean _collect;
    private final boolean _ignoreUncataloguedSlots;
    private boolean _fullCheckFailed = false;
    private long idleTicks = 0;
    private boolean lackingMatierals = false;
    private boolean invChanged = false;
    private long missingTicks = 0;

    public CraftInInventoryTask(ItemTarget target, CraftingRecipe recipe, boolean collect, boolean ignoreUncataloguedSlots) {
        super(target);
        _recipe = recipe;
        _collect = collect;
        _ignoreUncataloguedSlots = ignoreUncataloguedSlots;
    }

    public CraftInInventoryTask(ItemTarget target, CraftingRecipe recipe) {
        this(target, recipe, true, false);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        _fullCheckFailed = false;
    }

    int ix = 0;
    @Override
    protected Task onResourceTick(AltoClef mod) {
        ItemTarget toGet = _itemTargets[0];

        //System.out.println(ix++);
        //missingCount(mod);
/*
        //RecipeManager.deserialize()
        Recipe r = RecipesUtils.getRecipeWithOutput(new ItemStack(Items.OAK_PLANKS));
        DefaultedList<Ingredient> l = r.getIngredients();
        l.forEach(e -> Arrays.stream(e.getMatchingStacks()).forEach(a -> System.out.println(a.getItem().getName().toString())));
*/
        //if (_collect && !mod.getInventoryTracker().hasRecipeMaterialsOrTarget(new RecipeTarget(toGet, _recipe)) || idleTicks > 300 && lackingMatierals && !invChanged) {
        //System.out.println(isFullyCapableToCraft(mod, _recipe));
        if (!mod.getInventoryTracker().isFullyCapableToCraft(mod, _recipe) && mod.getInventoryTracker().hasRecipeMaterialsOrTarget(new RecipeTarget(toGet, _recipe))) {
            this.missingTicks++;
        } else {
            this.missingTicks = 0;
        }

        //System.out.println(this.missingTicks + " --- " + mod.getInventoryTracker().hasRecipeMaterialsOrTarget(new RecipeTarget(toGet, _recipe)));

        /*
        if (this.missingTicks > 150) {
            System.out.println("H: " + (Utils.isNull(getMissingItemTarget(mod)) ? "null" : (Utils.isNull(getMissingItemTarget(mod).getMatches()) ? "null2" : getMissingItemTarget(mod).getMatches()[0].getName().toString())));
            return null;
            //return TaskCatalogue.getItemTask(new ItemTarget(Items.OAK_PLANKS, 2));
        }*/

        if (this.missingTicks > 150) {
            //System.out.println("H: " + (Utils.isNull(getMissingItemTarget(mod)) ? "null" : (Utils.isNull(getMissingItemTarget(mod).getMatches()) ? "null2" : getMissingItemTarget(mod).getMatches()[0].getName().toString())));
            if (Utils.isNull(mod.getInventoryTracker().getMissingItemTarget(mod, _recipe))) {
                this.missingTicks = 0;
                return null;
            }

            if (Utils.isNull(mod.getInventoryTracker().getMissingItemTarget(mod, _recipe).getMatches())) throw new IllegalStateException("why are missing matches null?");

            return TaskCatalogue.getItemTask(mod.getInventoryTracker().getMissingItemTarget(mod, _recipe));
        }

        if (_collect && !mod.getInventoryTracker().hasRecipeMaterialsOrTarget(new RecipeTarget(toGet, _recipe)) /*|| this.missingTicks > 250*//*!isFullyCapableToCraft(mod, _recipe)*/) {
            // Collect recipe materials
            //System.out.println("Y");
            //if (this.missingTicks > 150) return TaskCatalogue.getItemTask(new ItemTarget(Items.OAK_LOG));

            setDebugState("Collecting materials");
            return collectRecipeSubTask(mod);
        }

        // Free up inventory
        if (mod.getInventoryTracker().isInventoryFull()) {
            return new EnsureFreeInventorySlotTask();
        }

        setDebugState("Crafting in inventory... for " + toGet);
        //System.out.println("CraftGenericTask");

        return new CraftGenericTask(_recipe);
        //return new CraftGenericTask(_recipe, collectRecipeSubTask(mod));
        //craftInstant(mod, _recipe);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CraftInInventoryTask task) {
            if (!task._recipe.equals(_recipe)) return false;
            return isCraftingEqual(task);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return toCraftingDebugStringName() + " " + _recipe;
    }

    // virtual. By default assumes subtasks are CATALOGUED (in TaskCatalogue.java)
    protected Task collectRecipeSubTask(AltoClef mod) {
        return new CollectRecipeCataloguedResourcesTask(_ignoreUncataloguedSlots, new RecipeTarget(_itemTargets[0], _recipe));
    }

    protected String toCraftingDebugStringName() {
        return "Craft 2x2 Task";
    }

    protected boolean isCraftingEqual(CraftInInventoryTask other) {
        return true;
    }
}

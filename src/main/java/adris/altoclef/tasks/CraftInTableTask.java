package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;


public class CraftInTableTask extends ResourceTask {
    private final RecipeTarget[] targets;
    private final DoCraftInTableTask craftTask;
    
    public CraftInTableTask(RecipeTarget[] targets) {
        super(extractItemTargets(targets));
        this.targets = targets;
        craftTask = new DoCraftInTableTask(this.targets);
    }
    
    public CraftInTableTask(ItemTarget target, CraftingRecipe recipe, boolean collect, boolean ignoreUncataloguedSlots) {
        super(target);
        targets = new RecipeTarget[]{ new RecipeTarget(target, recipe) };
        craftTask = new DoCraftInTableTask(targets, collect, ignoreUncataloguedSlots);
    }
    
    public CraftInTableTask(ItemTarget target, CraftingRecipe recipe) {
        this(target, recipe, true, false);
    }
    
    public CraftInTableTask(Item[] items, int count, CraftingRecipe recipe) {
        this(new ItemTarget(items, count), recipe);
    }
    
    public CraftInTableTask(Item item, int count, CraftingRecipe recipe) {
        this(new ItemTarget(item, count), recipe);
    }
    
    private static ItemTarget[] extractItemTargets(RecipeTarget[] recipeTargets) {
        List<ItemTarget> result = new ArrayList<>(recipeTargets.length);
        for (RecipeTarget target : recipeTargets) {
            result.add(target.getTargetItem());
        }
        return Util.toArray(ItemTarget.class, result);
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
        return craftTask;
    }
    
    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        // Close the crafting table screen
        if (mod.getPlayer() != null) {
            mod.getPlayer().closeHandledScreen();
        }
        //mod.getControllerExtras().closeCurrentContainer();
    }
    
    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        if (obj instanceof CraftInTableTask) {
            CraftInTableTask other = (CraftInTableTask) obj;
            return craftTask.isEqual(other.craftTask);
        }
        return false;
    }
    
    @Override
    protected String toDebugStringName() {
        return craftTask.toDebugString();
    }
    
    public RecipeTarget[] getRecipeTargets() {
        return targets;
    }
}



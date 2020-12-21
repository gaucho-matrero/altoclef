package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.csharpisbetter.Timer;
import jdk.internal.loader.Resource;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.CraftingScreenHandler;

import java.util.ArrayList;
import java.util.List;


public class CraftInTableTask extends ResourceTask {

    private List<RecipeTarget> _targets;

    private DoCraftInTableTask _craftTask;

    public CraftInTableTask(List<RecipeTarget> targets) {
        super(extractItemTargets(targets));
        _targets = targets;
        _craftTask = new DoCraftInTableTask(_targets);
    }
    public CraftInTableTask(ItemTarget target, CraftingRecipe recipe) {
        super(target);
        _targets = new ArrayList<>(1);
        _targets.add(new RecipeTarget(target, recipe));
        _craftTask = new DoCraftInTableTask(_targets);
    }
    public CraftInTableTask(Item[] items, int count, CraftingRecipe recipe) {
        this(new ItemTarget(items, count), recipe);
    }
    public CraftInTableTask(Item item, int count, CraftingRecipe recipe) {
        this(new ItemTarget(item, count), recipe);
    }

    private static List<ItemTarget> extractItemTargets(List<RecipeTarget> recipeTargets) {
        List<ItemTarget> result = new ArrayList<>(recipeTargets.size());
        for(RecipeTarget target : recipeTargets) {
            result.add(target.getItem());
        }
        return result;
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
        mod.getPlayer().closeHandledScreen();
        //mod.getControllerExtras().closeCurrentContainer();
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        if (obj instanceof CraftInTableTask) {
            CraftInTableTask other = (CraftInTableTask) obj;
            return _craftTask.isEqual(other._craftTask);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return _craftTask.toDebugString();
    }
}


class DoCraftInTableTask extends DoStuffInContainerTask {

    private List<RecipeTarget> _targets;

    private boolean _crafted;

    private Timer _craftTimer;

    private int _craftCount;

    public DoCraftInTableTask(List<RecipeTarget> targets) {
        super(Blocks.CRAFTING_TABLE, "crafting_table");
        _targets = targets;
        _craftTimer = new Timer(0.5);
    }

    @Override
    protected void onStart(AltoClef mod) {
        super.onStart(mod);
        _crafted = false;
        _craftCount = 0;
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // Collect recipe materials first
        for(RecipeTarget target : _targets) {
            if (!mod.getInventoryTracker().hasRecipeMaterials(target.getRecipe())) {
                setDebugState("Collecting materials");
                return new CollectRecipeCataloguedResourcesTask(target.getRecipe());
            }
        }

        return super.onTick(mod);
    }

    @Override
    protected boolean isSubTaskEqual(DoStuffInContainerTask obj) {
        if (obj instanceof DoCraftInTableTask) {
            DoCraftInTableTask other = (DoCraftInTableTask) obj;

            if (other._targets.size() != _targets.size()) return false;
            for (int i = 0; i < _targets.size(); ++i) {
                if (!other._targets.get(i).getRecipe().equals(_targets.get(i).getRecipe())) {
                    //Debug.logInternal(other._targets.get(i).getRecipe() + " != " + _targets.get(i).getRecipe());
                    return false;
                }
            }
            return true;
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

        // Have a delay between the crafting
        if (!_craftTimer.elapsed()) return null;
        _craftTimer.reset();

        // Craft everything
        int i = 0;
        boolean succeeded = false;
        for (RecipeTarget target : _targets) {
            if (i == _craftCount) {
                setDebugState("Crafting: " + target.getRecipe());
                if (craftInstant(mod, target.getRecipe())) {
                    succeeded = true;
                }
            }
            i++;
        }
        if (succeeded) {
            _craftCount++;
        }

        return null;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _craftCount >= _targets.size();//_crafted;
    }

    @Override
    protected double getCostToMakeNew(AltoClef mod) {
        // TODO: If we have an axe, lower the cost.
        if (mod.getInventoryTracker().hasItem(ItemTarget.LOG) || mod.getInventoryTracker().getItemCount(ItemTarget.PLANKS) >= 4) {
            // We can craft it right now, so it's real cheap
            return 150;
        }
        // TODO: If cached and the closest log is really far away, strike the price UP
        return 300;
    }

    private boolean craftInstant(AltoClef mod, CraftingRecipe recipe) {
        if (!mod.getInventoryTracker().craftInstant(recipe)) {
            Debug.logWarning("Failed to craft recipe: " + recipe);
            return false;
        }
        return true;
    }

}

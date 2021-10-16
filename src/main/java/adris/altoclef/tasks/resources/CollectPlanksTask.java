package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Item;

import java.util.ArrayList;

public class CollectPlanksTask extends CraftInInventoryTask {

    private final Item[] _logs;
    private boolean _logsInNether;

    public CollectPlanksTask(Item[] planks, Item[] logs, int count, boolean logsInNether) {
        super(new ItemTarget(planks, count), generatePlankRecipe(logs));
        _logs = logs;
        _logsInNether = logsInNether;
    }

    public CollectPlanksTask(int count) {
        this(ItemHelper.PLANKS, ItemHelper.LOG, count, false);
    }

    public CollectPlanksTask(Item plank, Item log, int count) {
        this(new Item[]{plank}, new Item[]{log}, count, false);
    }

    public CollectPlanksTask(Item plank, int count) {
        this(plank, ItemHelper.planksToLog(plank), count);
    }

    public CollectPlanksTask logsInNether() {
        _logsInNether = true;
        return this;
    }

    private static CraftingRecipe generatePlankRecipe(Item[] logs) {
        return CraftingRecipe.newShapedRecipe(
                "planks",
                new Item[][]{
                        logs, null,
                        null, null
                },
                4
        );
    }

    @Override
    protected Task collectRecipeSubTask(AltoClef mod) {
        // Collect planks and logs
        ArrayList<ItemTarget> blocksTomine = new ArrayList<>(2);
        blocksTomine.add(new ItemTarget(_logs));
        // Ignore planks if we're told to.
        if (!mod.getBehaviour().exclusivelyMineLogs()) {
            // TODO: Add planks back in, but with a heuristic check (so we don't go for abandoned mineshafts)
            //blocksTomine.add(new ItemTarget(ItemUtil.PLANKS));
        }

        ResourceTask mineTask = new MineAndCollectTask(blocksTomine.toArray(ItemTarget[]::new), MiningRequirement.HAND);
        // Kinda jank
        if (_logsInNether) {
            mineTask.forceDimension(Dimension.NETHER);
        }
        return mineTask;
    }
}

package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.ItemUtil;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.item.Item;

import java.util.ArrayList;

public class CollectPlanksTask extends CraftInInventoryTask {

    private final Item[] _logs;

    public CollectPlanksTask(Item[] planks, Item[] logs, int count) {
        super(new ItemTarget(planks, count), generatePlankRecipe(logs));
        _logs = logs;
    }

    public CollectPlanksTask(int count) {
        this(ItemUtil.PLANKS, ItemUtil.LOG, count);
    }

    public CollectPlanksTask(Item plank, Item log, int count) {
        this(new Item[]{plank}, new Item[]{log}, count);
    }

    public CollectPlanksTask(Item plank, int count) {
        this(plank, ItemUtil.planksToLog(plank), count);
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
            //blocksTomine.add(new ItemTarget(ItemUtil.PLANKS));
        }
        return new MineAndCollectTask(Util.toArray(ItemTarget.class, blocksTomine), MiningRequirement.HAND);
    }
}

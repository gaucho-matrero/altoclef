package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

public class CollectPlanksTask extends CraftInInventoryTask {

    private static final CraftingRecipe PLANK_RECIPE = CraftingRecipe.newShapedRecipe(
            "planks",
            new Item[][]{
                ItemTarget.LOG, null,
                null, null
            },
            4
    );

    public CollectPlanksTask(int count) {
        super(new ItemTarget(ItemTarget.PLANKS, count), PLANK_RECIPE);
    }

    @Override
    protected Task collectRecipeSubTask(AltoClef mod) {
        // Collect planks and logs
        ArrayList<ItemTarget> blocksTomine = new ArrayList<>(2);
        blocksTomine.add(new ItemTarget(ItemTarget.LOG));
        // Ignore planks if we're told to.
        if (!mod.getConfigState().exclusivelyMineLogs()) {
            blocksTomine.add(new ItemTarget(ItemTarget.PLANKS));
        }
        return new MineAndCollectTask(Util.toArray(ItemTarget.class, blocksTomine), MiningRequirement.HAND);
    }
}

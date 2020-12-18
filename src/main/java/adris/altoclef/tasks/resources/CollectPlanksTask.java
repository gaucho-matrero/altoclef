package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import jdk.internal.loader.Resource;
import net.minecraft.item.Item;

import java.util.Arrays;

public class CollectPlanksTask extends CraftInInventoryTask {

    private static final CraftingRecipe PLANK_RECIPE = CraftingRecipe.newShapedRecipe(
            new Item[][]{
                ItemTarget.LOG, null,
                null, null
            }
    );

    public CollectPlanksTask(int count) {
        super(new ItemTarget(ItemTarget.PLANKS, count), PLANK_RECIPE);
    }

    @Override
    protected Task collectRecipeSubTask(AltoClef mod) {
        // Collect planks and logs
        return new MineAndCollectTask(Arrays.asList(
                new ItemTarget(ItemTarget.LOG), new ItemTarget(ItemTarget.PLANKS)
        ), MiningRequirement.HAND);
    }
}

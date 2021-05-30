package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.DefaultGoToDimensionTask;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;

public class CollectQuartzTask extends ResourceTask {

    private final int _count;

    public CollectQuartzTask(int count) {
        super(Items.QUARTZ, count);
        _count = count;
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
        if (mod.getCurrentDimension() != Dimension.NETHER) {
            setDebugState("Going to nether");
            return new DefaultGoToDimensionTask(Dimension.NETHER);
        }

        setDebugState("Mining");
        return new MineAndCollectTask(new ItemTarget("quartz", _count), new Block[]{Blocks.NETHER_QUARTZ_ORE}, MiningRequirement.WOOD);
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        return obj instanceof CollectQuartzTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collecting " + _count + " quartz";
    }
}

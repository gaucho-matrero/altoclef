package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

public class LocateWarpedBiomeTask extends Task {

    private BlockPos _finalPos=null;


    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.WARPED_NYLIUM);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        BlockPos warpedBiomeStart = doTheThing(mod);
        if(mod.getCurrentDimension()!=Dimension.NETHER){
            setDebugState("Getting to Nether");
            return new DefaultGoToDimensionTask(Dimension.NETHER);
        }
        if (warpedBiomeStart != null) {
            _finalPos = warpedBiomeStart;
        }
        if (_finalPos != null) {
            if (mod.getInventoryTracker().getItemCount(Items.NETHERRACK) < 64) {
                setDebugState("Getting some extra blocks in case we need " +
                        "to build. " + mod.getInventoryTracker().getItemCount(Items.NETHERRACK) + "/" + "64");
                return TaskCatalogue.getItemTask(Items.NETHERRACK, 5);
            }
            setDebugState("Getting to warped biome at: " + _finalPos);
            return new GetToBlockTask(_finalPos, false);
        }
        return new SearchWithinBiomeTask(Biome.Category.NETHER);

    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.WARPED_NYLIUM);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof LocateWarpedBiomeTask;
    }

    @Override
    protected String toDebugString() {
        return "Searching for warped biome";
    }

    /**
     * Find the closest warped Nylium in the nether
     * @param mod Needs access to minecraft
     * @return location of warped nylium (no one places this right?) null if
     * it isn't found.
     */
    protected BlockPos doTheThing(AltoClef mod){
        if(mod.getCurrentDimension()!= Dimension.NETHER){
            return null;
        }else{
            if(mod.getBlockTracker().getKnownLocations(Blocks.WARPED_NYLIUM).isEmpty()){
                return null;
            }else {
                return mod.getBlockTracker().getKnownLocations(Blocks.WARPED_NYLIUM).get(0);
            }
        }
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getPlayer().getBlockPos().equals(_finalPos);
    }
}

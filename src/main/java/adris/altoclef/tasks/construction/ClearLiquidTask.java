package adris.altoclef.tasks.construction;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.InteractItemWithBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;


public class ClearLiquidTask extends Task {
    private final BlockPos liquidPos;
    
    public ClearLiquidTask(BlockPos liquidPos) {
        this.liquidPos = liquidPos;
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        if (mod.getChunkTracker().isChunkLoaded(liquidPos)) {
            return mod.getWorld().getBlockState(liquidPos).getFluidState().isEmpty();
        }
        return false;
    }
    
    @Override
    protected void onStart(AltoClef mod) {
    
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getInventoryTracker().hasItem(Items.BUCKET)) {
            mod.getConfigState().setRayTracingFluidHandling(RaycastContext.FluidHandling.SOURCE_ONLY);
            return new InteractItemWithBlockTask(new ItemTarget("bucket", 1), liquidPos, false);
        }
        
        return new PlaceStructureBlockTask(liquidPos);
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof ClearLiquidTask) {
            ClearLiquidTask task = (ClearLiquidTask) obj;
            return task.liquidPos.equals(liquidPos);
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Clear liquid at " + liquidPos;
    }
}

package adris.altoclef.tasks.misc;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.InteractItemWithBlockTask;
import adris.altoclef.tasksystem.Task;
import baritone.api.utils.input.Input;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;


/**
 * Given a block position with fire in it, extinguish the fire at that position
 */
public class PutOutFireTask extends Task {
    private final BlockPos firePosition;
    
    public PutOutFireTask(BlockPos firePosition) {
        this.firePosition = firePosition;
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        BlockState s = mod.getWorld().getBlockState(firePosition);
        return (s.getBlock() != Blocks.FIRE && s.getBlock() != Blocks.SOUL_FIRE);
    }
    
    @Override
    protected void onStart(AltoClef mod) {
    
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        return new InteractItemWithBlockTask(null, Direction.UP, firePosition.down(), Input.CLICK_LEFT, false, false);
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof PutOutFireTask) {
            PutOutFireTask task = (PutOutFireTask) obj;
            return (task.firePosition.equals(firePosition));
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Putting out fire at " + firePosition;
    }
}

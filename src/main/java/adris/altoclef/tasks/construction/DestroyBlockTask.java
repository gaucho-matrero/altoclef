package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.baritone.PlaceBlockSchematic;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.utils.input.Input;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class DestroyBlockTask extends Task implements ITaskRequiresGrounded {

    private final BlockPos _pos;
    private final MovementProgressChecker _moveChecker = new MovementProgressChecker(10, 0.1, 4, 0.01);
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(5, true);
    private boolean _failedFirstTry;


    public DestroyBlockTask(BlockPos pos) {
        _pos = pos;
    }

    @Override
    protected void onStart(AltoClef mod) {
        startBreakBuild(mod);
        _wanderTask.resetWander();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // Wander and check
        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            _moveChecker.reset();
            return _wanderTask;
        }
        if (!_moveChecker.check(mod)) {
            _failedFirstTry = !_failedFirstTry;
            _moveChecker.reset();
            // Only when we've tried both outcomes and have looped back to the beginning do we wander.
            if (!_failedFirstTry) {
                Debug.logMessage("Failed both ways, wandering for a bit...");
                mod.getBlockTracker().requestBlockUnreachable(_pos);
                return _wanderTask;
            } else {
                Debug.logMessage("Switching methods of breaking, may work better.");
            }
        }

        if (_failedFirstTry) {
            if (mod.getClientBaritone().getBuilderProcess().isActive()) {
                mod.getClientBaritone().getBuilderProcess().onLostControl();
            }
            setDebugState("Going to destroy block to destroy the block");
            // This will destroy the target block.
            return new GetToBlockTask(_pos, false);
        } else if (!mod.getClientBaritone().getBuilderProcess().isActive()) {
            Debug.logMessage("Break Block: Restarting builder process");
            startBreakBuild(mod);
        }

        setDebugState("Breaking block via baritone...");

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        if (!AltoClef.inGame()) return;
        mod.getClientBaritone().getBuilderProcess().onLostControl();
        // Do not keep breaking.
        // Can lead to trouble, for example, if lava is right above the NEXT block.
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, false);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return WorldUtil.isAir(mod, _pos);//;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof DestroyBlockTask) {
            DestroyBlockTask task = (DestroyBlockTask) obj;
            return task._pos.equals(_pos);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Destroy block at " + _pos.toShortString();
    }

    private void startBreakBuild(AltoClef mod) {
        mod.getClientBaritone().getBuilderProcess().build("destroy block", new PlaceBlockSchematic(Blocks.AIR), _pos);
    }
}

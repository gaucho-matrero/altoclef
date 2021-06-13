package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FillStrongholdPortalTask extends Task {

    private final boolean _destroySilverfishSpawner;

    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(10);
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker(3);

    public FillStrongholdPortalTask(boolean destroySilverfishSpawner) {
        _destroySilverfishSpawner = destroySilverfishSpawner;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().setPreferredStairs(false);
        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL_FRAME, Blocks.END_PORTAL);
        if (_destroySilverfishSpawner) {
            mod.getBlockTracker().trackBlock(Blocks.SPAWNER);
        }
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // If we encounter that weird back+forth bug, this might fix.
        // Overkill, but it would REALLY suck if the run stopped here.
        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            _progressChecker.reset();
            setDebugState("Wandering");
            return _wanderTask;
        }

        if (_destroySilverfishSpawner) {
            BlockPos silverfishSpawner = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), test -> !(WorldUtil.getSpawnerEntity(mod, test) instanceof SilverfishEntity), Blocks.SPAWNER);
            if (silverfishSpawner != null) {
                setDebugState("Destroy silverfish spawner");
                return new DestroyBlockTask(silverfishSpawner);
            }
        }

        setDebugState("Filling in Portal");
        if (!_progressChecker.check(mod)) {
            _progressChecker.reset();
            return _wanderTask;
        }
        return new DoToClosestBlockTask(
                () -> mod.getPlayer().getPos(),
                pos -> new InteractWithBlockTask(new ItemTarget(Items.ENDER_EYE, 1), Direction.UP, pos, true),
                pos -> mod.getBlockTracker().getNearestTracking(pos, test -> BeatMinecraftTask.isEndPortalFrameFilled(mod, test) || !mod.getBlockTracker().blockIsValid(test, Blocks.END_PORTAL_FRAME), Blocks.END_PORTAL_FRAME)
        );
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL_FRAME, Blocks.END_PORTAL);
        if (_destroySilverfishSpawner) {
            mod.getBlockTracker().stopTracking(Blocks.SPAWNER);
        }
        mod.getBehaviour().pop();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        BlockPos closest = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), Blocks.END_PORTAL);
        return closest != null && mod.getChunkTracker().isChunkLoaded(closest);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof FillStrongholdPortalTask) {
            return ((FillStrongholdPortalTask) obj)._destroySilverfishSpawner == _destroySilverfishSpawner;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Fill Stronghold Portal";
    }
}

package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractItemWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.WorldUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class FillStrongholdPortalTask extends Task {

    private final boolean _destroySilverfishSpawner;

    public FillStrongholdPortalTask(boolean destroySilverfishSpawner) {
        _destroySilverfishSpawner = destroySilverfishSpawner;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getConfigState().push();
        mod.getConfigState().setPreferredStairs(false);
        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL_FRAME, Blocks.END_PORTAL);
        if (_destroySilverfishSpawner) {
            mod.getBlockTracker().trackBlock(Blocks.SPAWNER);
        }
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_destroySilverfishSpawner) {
            BlockPos silverfishSpawner = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), test -> !(WorldUtil.getSpawnerEntity(mod, test) instanceof SilverfishEntity), Blocks.SPAWNER);
            if (silverfishSpawner != null) {
                setDebugState("Destroy silverfish spawner");
                return new DestroyBlockTask(silverfishSpawner);
            }
        }
        // Delay each portal so that we don't accidentally throw the eye like a dumbass
        return new DoToClosestBlockTask(
            () -> mod.getPlayer().getPos(),
            pos -> new InteractItemWithBlockTask(new ItemTarget(Items.ENDER_EYE, 1), Direction.UP, pos, true),
            pos -> mod.getBlockTracker().getNearestTracking(pos, test -> BeatMinecraftTask.isEndPortalFrameFilled(mod, test) || !mod.getBlockTracker().blockIsValid(test, Blocks.END_PORTAL_FRAME), Blocks.END_PORTAL_FRAME)
        );
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL_FRAME, Blocks.END_PORTAL);
        if (_destroySilverfishSpawner) {
            mod.getBlockTracker().stopTracking(Blocks.SPAWNER);
        }
        mod.getConfigState().pop();
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

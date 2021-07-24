package adris.altoclef.tasks.misc.anarchysurvive;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.misc.EnterNetherPortalTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.WorldUtil;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.function.Predicate;

public class EscapeSpawnTask extends Task {

    public float spawnInnerRadius;
    public float spawnPortalDangerousRadius;
    public float spawnOuterRadius;

    public HighwayAxis axis;

    private final Task _collectBuildMaterialsTask = TaskCatalogue.getItemTask("dirt",10);

    private Task _portalExitTask;

    public EscapeSpawnTask(HighwayAxis axis, float spawnInnerRadius, float spawnPortalDangerousRadius, float spawnOuterRadius) {
        this.axis = axis;
        this.spawnInnerRadius = spawnInnerRadius;
        this.spawnPortalDangerousRadius = spawnPortalDangerousRadius;
        this.spawnOuterRadius = spawnOuterRadius;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.NETHER_PORTAL);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        /*
        * If we in the overworld with NO building blocks, try to get some dirt.
        * run "TravelOutwardHighwayAxis" passing in the spawn portals dangerous radius
        * If we're in the nether and we've passed a certain "overworld threshold" point, we're good.
         */

        // Get building materials (dirt)
        if (mod.getCurrentDimension() == Dimension.OVERWORLD && _collectBuildMaterialsTask.isActive() && !_collectBuildMaterialsTask.isFinished(mod)) {
            return _collectBuildMaterialsTask;
        }
        if (mod.getCurrentDimension() == Dimension.OVERWORLD && !mod.getInventoryTracker().hasItem(Items.DIRT, Items.COBBLESTONE, Items.NETHERRACK)) {
            return _collectBuildMaterialsTask;
        }

        if (_portalExitTask != null && _portalExitTask.isActive() && !_portalExitTask.isFinished(mod)) {
            return _portalExitTask;
        }

        Predicate<BlockPos> badPortal = blockpos -> WorldUtil.getOverworldPosition(mod, blockpos).isWithinDistance(Vec3i.ZERO, spawnPortalDangerousRadius);

        // We want to escape the nether to go back to the overworld
        if (mod.getOverworldPosition().lengthSquared() > spawnOuterRadius*spawnOuterRadius && mod.getCurrentDimension() == Dimension.NETHER) {
            Predicate<BlockPos> badPortalEscape = blockpos -> badPortal.test(blockpos) || WorldUtil.getOverworldPosition(mod, blockpos).isWithinDistance(Vec3i.ZERO, spawnInnerRadius);
            if (mod.getBlockTracker().anyFound(badPortalEscape, Blocks.NETHER_PORTAL)) {
                _portalExitTask = new EnterNetherPortalTask(Dimension.OVERWORLD, badPortalEscape);
                return _portalExitTask;
            }
        }

        // Travel along axis, avoiding portals too close to spawn.
        return new TravelAlongHighwayAxis(axis, badPortal);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.NETHER_PORTAL);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getOverworldPosition().lengthSquared() > spawnOuterRadius*spawnOuterRadius && mod.getCurrentDimension() == Dimension.OVERWORLD;
    }

    private static boolean closeEnough(float a, float b) {
        return Math.abs(a - b) < 0.01;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof EscapeSpawnTask) {
            EscapeSpawnTask task = (EscapeSpawnTask) obj;
            return task.axis == axis && closeEnough(task.spawnInnerRadius, spawnInnerRadius) && closeEnough(task.spawnOuterRadius, spawnOuterRadius) && closeEnough(task.spawnPortalDangerousRadius, spawnPortalDangerousRadius);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Escaping Spawn along " + axis;
    }
}

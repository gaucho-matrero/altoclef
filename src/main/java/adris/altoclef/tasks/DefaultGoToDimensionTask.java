package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.misc.EnterNetherPortalTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasks.construction.compound.ConstructNetherPortalBucketTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 * Some generic tasks require us to go to the nether.
 * <p>
 * The user should be able to specify how this should be done in settings
 * (ex, craft a new portal from scratch or check particular portal areas first or highway or whatever)
 */
public class DefaultGoToDimensionTask extends Task {

    private final Dimension _target;

    public DefaultGoToDimensionTask(Dimension target) {
        _target = target;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.NETHER_PORTAL);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getCurrentDimension() == _target) return null;

        switch (_target) {
            case OVERWORLD:
                switch (mod.getCurrentDimension()) {
                    case NETHER:
                        return goToOverworldFromNetherTask(mod);
                    case END:
                        return goToOverworldFromEndTask(mod);
                }
                break;
            case NETHER:
                switch (mod.getCurrentDimension()) {
                    case OVERWORLD:
                        return goToNetherFromOverworldTask(mod);
                    case END:
                        // First go to the overworld
                        return goToOverworldFromEndTask(mod);
                }
                break;
            case END:
                switch (mod.getCurrentDimension()) {
                    case NETHER:
                        // First go to the overworld
                        return goToOverworldFromNetherTask(mod);
                    case OVERWORLD:
                        return goToEndTask(mod);
                }
                break;
        }

        setDebugState(mod.getCurrentDimension() + " -> " + _target + " is NOT IMPLEMENTED YET!");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.NETHER_PORTAL);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof DefaultGoToDimensionTask) {
            DefaultGoToDimensionTask task = (DefaultGoToDimensionTask) obj;
            return task._target == _target;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Going to dimension: " + _target + " (default version)";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getCurrentDimension() == _target;
    }

    private Task goToOverworldFromNetherTask(AltoClef mod) {
        if (netherPortalIsClose(mod)) {
            setDebugState("Going to nether portal");
            return new EnterNetherPortalTask(Dimension.NETHER);
        }

        BlockPos closest = mod.getMiscBlockTracker().getLastNetherPortal(Dimension.NETHER);
        if (closest != null) {
            setDebugState("Going to last nether portal pos");
            return new GetToBlockTask(closest);
        }

        setDebugState("We're totally lost, wandering to shoot in the dark.");
        return new TimeoutWanderTask();
    }

    private Task goToOverworldFromEndTask(AltoClef mod) {
        setDebugState("TODO: Go to center portal (at 0,0). If it doesn't exist, kill ender dragon lol");
        return null;
    }

    private Task goToNetherFromOverworldTask(AltoClef mod) {
        if (netherPortalIsClose(mod)) {
            setDebugState("Going to nether portal");
            return new EnterNetherPortalTask(Dimension.NETHER);
        }
        switch (mod.getModSettings().getOverworldToNetherBehaviour()) {
            case BUILD_PORTAL_VANILLA:
                return new ConstructNetherPortalBucketTask();
            case GO_TO_HOME_BASE:
                return new GetToBlockTask(mod.getModSettings().getHomeBasePosition());
        }
        setDebugState("Overworld->Nether Behaviour " + mod.getModSettings().getOverworldToNetherBehaviour() + " is NOT IMPLEMENTED YET!");
        return null;
    }

    private Task goToEndTask(AltoClef mod) {
        // Keep in mind that getting to the end requires going to the nether first.
        setDebugState("TODO: Get to End, Same as BeatMinecraft");
        return null;
    }

    private boolean netherPortalIsClose(AltoClef mod) {
        if (mod.getBlockTracker().anyFound(Blocks.NETHER_PORTAL)) {
            BlockPos closest = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), Blocks.NETHER_PORTAL);
            return closest.isWithinDistance(mod.getPlayer().getPos(), 2000);
        }
        return false;
    }

    public enum OVERWORLD_TO_NETHER_BEHAVIOUR {
        BUILD_PORTAL_VANILLA,
        GO_TO_HOME_BASE
    }
}

package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.KillEntityTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class CollectBlazeRodsTask extends ResourceTask {

    private static final double SPAWNER_BLAZE_RADIUS = 16;

    private static final int TOO_MANY_BLAZES = 5;
    private static final double TOO_LITTLE_HEALTH_BLAZE = 4;

    private BlockPos _foundBlazeSpawner = null;

    private final int _count;

    private final SearchNetherFortressTask _searcher = new SearchNetherFortressTask();

    public CollectBlazeRodsTask(int count) {
        super(Items.BLAZE_ROD, count);
        _count = count;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.SPAWNER);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        // Fail if we're in nether.
        if (mod.getCurrentDimension() != Dimension.NETHER) {
            Debug.logWarning("Can't get blaze if we're not in the nether...");
        }

        // If there is a blaze, kill it.
        if (mod.getEntityTracker().mobFound(BlazeEntity.class)) {

            // If we're in danger and there are too many blazes, run away.
            if (mod.getEntityTracker().getTrackedMobs(BlazeEntity.class).size() >= TOO_MANY_BLAZES && mod.getPlayer().getHealth() <= TOO_LITTLE_HEALTH_BLAZE) {
                return new TimeoutWanderTask();
            }

            Entity toKill = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), BlazeEntity.class);
            if (_foundBlazeSpawner != null) {
                Vec3d nearest = toKill.getPos();
                double sqDistanceToSpawner = nearest.squaredDistanceTo(_foundBlazeSpawner.getX(), _foundBlazeSpawner.getY(), _foundBlazeSpawner.getZ());
                // Ignore if the blaze is too far away.
                if (sqDistanceToSpawner > SPAWNER_BLAZE_RADIUS) {
                    toKill = null;
                }
            }
            if (toKill != null) {
                setDebugState("Killing blaze");
                return new KillEntityTask(toKill);
            }
        }

        // If we have a blaze spawner, go near it.
        if (_foundBlazeSpawner != null) {
            if (!_foundBlazeSpawner.isWithinDistance(mod.getPlayer().getPos(), 4)) {
                setDebugState("Going to blaze spawner");
                return new GetToBlockTask(_foundBlazeSpawner.up(), false);
            } else {
                setDebugState("Waiting near blaze spawner for blazes to spawn");
                return null;
            }
        } else {
            // Search for blaze
            for(BlockPos pos : mod.getBlockTracker().getKnownLocations(Blocks.SPAWNER)) {
                BlockState state = mod.getWorld().getBlockState(pos);
                if (state.getBlock() instanceof SpawnerBlock) {
                    BlockEntity be = mod.getWorld().getBlockEntity(pos);
                    if (be instanceof MobSpawnerBlockEntity) {
                        MobSpawnerBlockEntity blockEntity = (MobSpawnerBlockEntity) be;
                        if (blockEntity.getLogic().getRenderedEntity() instanceof BlazeEntity) {
                            Debug.logMessage("(Found blaze spawner)");
                            _foundBlazeSpawner = pos;
                        } else {
                            assert blockEntity.getLogic().getRenderedEntity() != null;
                            Debug.logMessage("FAILED ENTITY SPAWNER: " + blockEntity.getLogic().getRenderedEntity().getEntityName());
                        }
                    }
                }
            }
        }

        // We need to find our fortress.
        setDebugState("Searching for fortress/Traveling around fortress");
        return _searcher;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.SPAWNER);
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        return obj instanceof CollectBlazeRodsTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect " + _count + " blaze rods";
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }
}

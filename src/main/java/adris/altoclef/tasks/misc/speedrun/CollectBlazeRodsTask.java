package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.DefaultGoToDimensionTask;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.KillEntitiesTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.misc.PutOutFireTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.WorldUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class CollectBlazeRodsTask extends ResourceTask {

    private static final double SPAWNER_BLAZE_RADIUS = 32;

    private static final int TOO_MANY_BLAZES = 5;
    private static final double TOO_LITTLE_HEALTH_BLAZE = 5;
    private final int _count;
    private final SearchNetherFortressTask _searcher = new SearchNetherFortressTask();

    // Why was this here???
    //private Entity _toKill;
    private BlockPos _foundBlazeSpawner = null;

    public CollectBlazeRodsTask(int count) {
        super(Items.BLAZE_ROD, count);
        _count = count;
    }

    private static boolean isHoveringAboveLavaOrTooHigh(AltoClef mod, Entity entity) {
        int MAX_HEIGHT = 23;
        for (BlockPos check = entity.getBlockPos(); entity.getBlockPos().getY() - check.getY() < MAX_HEIGHT; check = check.down()) {
            if (mod.getWorld().getBlockState(check).getBlock() == Blocks.LAVA) return true;
            if (WorldUtil.isSolid(mod, check)) return false;
        }
        return true;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.SPAWNER);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        // We must go to the nether.
        if (mod.getCurrentDimension() != Dimension.NETHER) {
            setDebugState("Going to nether");
            return new DefaultGoToDimensionTask(Dimension.NETHER);
        }

        Entity toKill = null;
        // If there is a blaze, kill it.
        if (mod.getEntityTracker().entityFound(BlazeEntity.class)) {

            // If we're in danger and there are too many blazes, run away.
            if (mod.getEntityTracker().getTrackedEntities(BlazeEntity.class).size() >= TOO_MANY_BLAZES && mod.getPlayer().getHealth() <= TOO_LITTLE_HEALTH_BLAZE) {
                setDebugState("Running away as there are too many blazes nearby.");
                return new TimeoutWanderTask();
            }

            toKill = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), BlazeEntity.class);
            if (_foundBlazeSpawner != null && toKill != null) {
                Vec3d nearest = toKill.getPos();

                double sqDistanceToPlayer = nearest.squaredDistanceTo(mod.getPlayer().getPos());//_foundBlazeSpawner.getX(), _foundBlazeSpawner.getY(), _foundBlazeSpawner.getZ());
                // Ignore if the blaze is too far away.
                if (sqDistanceToPlayer > SPAWNER_BLAZE_RADIUS * SPAWNER_BLAZE_RADIUS) {
                    // If the blaze can see us it needs to go lol
                    BlockHitResult hit = mod.getWorld().raycast(new RaycastContext(mod.getPlayer().getCameraPosVec(1.0F), toKill.getCameraPosVec(1.0F), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mod.getPlayer()));
                    if (hit != null && hit.getBlockPos().getSquaredDistance(mod.getPlayer().getPos(), false) < sqDistanceToPlayer) {
                        toKill = null;
                    }
                }
            }
        }

        if (toKill != null && toKill.isAlive()) {
            setDebugState("Killing blaze");
            return new KillEntitiesTask(entity -> isHoveringAboveLavaOrTooHigh(mod, entity), BlazeEntity.class);
            //return new DoToClosestEntityTask(() -> mod.getPlayer().getPos(), KillEntitiesTask::new, BlazeEntity.class);
            //return new KillEntityTask(toKill);
        }


        // If the blaze spawner somehow isn't valid
        if (_foundBlazeSpawner != null && mod.getChunkTracker().isChunkLoaded(_foundBlazeSpawner) && !isValidBlazeSpawner(mod, _foundBlazeSpawner)) {
            Debug.logMessage("Blaze spawner at " + _foundBlazeSpawner + " too far away or invalid. Re-searching.");
            _foundBlazeSpawner = null;
        }

        // If we have a blaze spawner, go near it.
        if (_foundBlazeSpawner != null) {
            if (!_foundBlazeSpawner.isWithinDistance(mod.getPlayer().getPos(), 4)) {
                setDebugState("Going to blaze spawner");
                return new GetToBlockTask(_foundBlazeSpawner.up(), false);
            } else {

                // Put out fire that might mess with us.
                BlockPos nearestFire = mod.getBlockTracker().getNearestWithinRange(_foundBlazeSpawner, 5, Blocks.FIRE);
                if (nearestFire != null) {
                    setDebugState("Clearing fire around spawner to prevent loss of blaze rods.");
                    return new PutOutFireTask(nearestFire);
                }

                setDebugState("Waiting near blaze spawner for blazes to spawn");
                return null;
            }
        } else {
            // Search for blaze
            for (BlockPos pos : mod.getBlockTracker().getKnownLocations(Blocks.SPAWNER)) {
                if (isValidBlazeSpawner(mod, pos)) {
                    _foundBlazeSpawner = pos;
                    break;
                }
            }
        }

        // We need to find our fortress.
        setDebugState("Searching for fortress/Traveling around fortress");
        return _searcher;
    }

    private boolean isValidBlazeSpawner(AltoClef mod, BlockPos pos) {
        if (!mod.getChunkTracker().isChunkLoaded(pos)) {
            // If unloaded, go to it. Unless it's super far away.
            return false;
            //return pos.isWithinDistance(mod.getPlayer().getPos(),3000);
        }
        return WorldUtil.getSpawnerEntity(mod, pos) instanceof BlazeEntity;
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

package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.TimerGame;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class EscapeFromLavaTask extends Task {

    private final TimerGame _scanTimer = new TimerGame(5);
    private BlockPos target;

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().allowWalkThroughLava(true);
        target = null;
        _scanTimer.forceElapse();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_scanTimer.elapsed() && target == null) {
            target = findSafePos(mod);
            _scanTimer.reset();
        }
        if (target != null) {
            setDebugState("Traveling to safe block at " + target);
            return new GetToBlockTask(target, false);
        }
        // Just keep on moving!!!
        setDebugState("Couldn't find safe spot. This is bad news.");
        return new TimeoutWanderTask();
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof EscapeFromLavaTask;
    }

    private BlockPos findSafePos(AltoClef mod) {
        // What constitutes a safe pos?
        // For each block within a 50 block radius:
        int radius = 50;
        BlockPos p = mod.getPlayer().getBlockPos();
        BlockPos best = null;
        double smallestScore = Double.POSITIVE_INFINITY;
        for (int xx = p.getX() - radius; xx < p.getX() + radius; ++xx) {
            for (int zz = p.getZ() - radius; zz < p.getZ() + radius; ++zz) {
                blockLoop:
                for (int yy = p.getY() - radius; yy < p.getY() + radius; ++yy) {
                    BlockPos check = new BlockPos(xx, yy, zz);
                    BlockState state = mod.getWorld().getBlockState(check);
                    // Below and above can't be lava.
                    final Vec3i[] noLavaPls = new Vec3i[]{
                            new Vec3i(0, 0, 0),
                            new Vec3i(0, 1, 0),
                            new Vec3i(0, -1, 0),
                            new Vec3i(1, 0, 0),
                            new Vec3i(-1, 0, 0),
                            new Vec3i(0, 0, 1),
                            new Vec3i(0, 0, -1)
                    };
                    for (Vec3i noLava : noLavaPls) {
                        if (mod.getWorld().getBlockState(check.add(noLava)).getBlock() == Blocks.LAVA) {
                            continue blockLoop;
                        }
                    }
                    // Below must be solid.
                    if (WorldUtil.isSolid(mod, check.down())) {
                        double scoreMultiplier = 1;
                        if (WorldUtil.isAir(mod, check)) {
                            scoreMultiplier *= 0.7;
                        }
                        if (WorldUtil.isAir(mod, check.up())) {
                            scoreMultiplier *= 0.8;
                        }
                        double score = check.getSquaredDistance(mod.getPlayer().getPos(), false) * scoreMultiplier;
                        if (score < smallestScore) {
                            smallestScore = score;
                            best = check;
                        }
                    }
                }
            }
        }
        return best;
    }

    @Override
    protected String toDebugString() {
        return "Escaping lava";
    }
}

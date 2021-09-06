package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.TimerGame;
import baritone.api.pathing.goals.Goal;
import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class EscapeFromLavaTask extends CustomBaritoneGoalTask {

    private final float _strength;

    public EscapeFromLavaTask(float strength) {
        _strength = strength;
    }
    public EscapeFromLavaTask() {
        this(100);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().allowSwimThroughLava(true);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new EscapeFromLavaGoal();
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof EscapeFromLavaTask;
    }

    @Override
    protected String toDebugString() {
        return "Escaping lava";
    }

    private class EscapeFromLavaGoal implements Goal {

        private static boolean isLava(int x, int y, int z) {
            if (MinecraftClient.getInstance().world == null) return false;
            return MovementHelper.isLava(MinecraftClient.getInstance().world.getBlockState(new BlockPos(x, y, z)));
        }
        private static boolean isWater(int x, int y, int z) {
            if (MinecraftClient.getInstance().world == null) return false;
            return MovementHelper.isWater(MinecraftClient.getInstance().world.getBlockState(new BlockPos(x, y, z)));
        }

        @Override
        public boolean isInGoal(int x, int y, int z) {
            return !isLava(x, y, z);
        }

        @Override
        public double heuristic(int x, int y, int z) {
            if (isLava(x, y, z)) {
                return _strength;
            }
            if (isWater(x, y, z)) {
                return -100;
            }
            return 0;
        }
    }
}

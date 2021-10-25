package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.ProjectileHelper;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ThrowEnderPearlSimpleProjectileTask extends Task {

    private final TimerGame _thrownTimer = new TimerGame(5);
    private final BlockPos _target;

    public ThrowEnderPearlSimpleProjectileTask(BlockPos target) {
        _target = target;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _thrownTimer.forceElapse();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // TODO: Unlikely/minor nitpick, but there could be other people throwing ender pearls, which would delay the bot.
        if (mod.getEntityTracker().entityFound(EnderPearlEntity.class)) {
            _thrownTimer.reset();
        }
        if (_thrownTimer.elapsed()) {
            if (mod.getSlotHandler().forceEquipItem(Items.ENDER_PEARL)) {
                Rotation lookTarget = calculateThrowLook(mod, _target);
                LookHelper.lookAt(mod, lookTarget);
                if (LookHelper.isLookingAt(mod, lookTarget)) {
                    mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                    _thrownTimer.reset();
                }
            }
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof ThrowEnderPearlSimpleProjectileTask task) {
            return task._target.equals(_target);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Simple Ender Pearling to " + _target;
    }

    private static Rotation calculateThrowLook(AltoClef mod, BlockPos end) {
        Vec3d start = ProjectileHelper.getThrowOrigin(mod.getPlayer());
        Vec3d endCenter = WorldHelper.toVec3d(end);
        double gravity = ProjectileHelper.THROWN_ENTITY_GRAVITY_ACCEL;
        double speed = 1.5;//1.775; // 1.5 + gaussian garbage
        float yaw = LookHelper.getLookRotation(mod, end).getYaw();
        double flatDistance = WorldHelper.distanceXZ(start, endCenter);
        double pitch = ProjectileHelper.calculateAngleForSimpleProjectileMotion(start.y - endCenter.y, flatDistance, speed, gravity);
        return new Rotation(yaw, -1 * (float)pitch);
    }
}

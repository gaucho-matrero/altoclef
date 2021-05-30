package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class FollowPlayerTask extends Task {

    private final String _playerName;

    public FollowPlayerTask(String playerName) {
        _playerName = playerName;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {

        Vec3d target = mod.getEntityTracker().getPlayerMostRecentPosition(_playerName);
        if (target == null) {
            mod.logWarning("Failed to get to player \"" + _playerName + "\" because we have no idea where they are.");
            stop(mod);
            return null;
        }

        if (target.isInRange(mod.getPlayer().getPos(), 1) && !mod.getEntityTracker().isPlayerLoaded(_playerName)) {
            mod.logWarning("Failed to get to player \"" + _playerName + "\". We moved to where we last saw them but now have no idea where they are.");
            stop(mod);
            return null;
        }

        if (!mod.getEntityTracker().isPlayerLoaded(_playerName)) {
            // Go to last location
            return new GetToBlockTask(new BlockPos((int) target.x, (int) target.y, (int) target.z), false);
        }
        return new GetToEntityTask(mod.getEntityTracker().getPlayerEntity(_playerName), 2);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof FollowPlayerTask) {
            FollowPlayerTask task = (FollowPlayerTask) obj;
            return task._playerName.equals(_playerName);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Going to player " + _playerName;
    }
}

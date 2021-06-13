package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.AbstractDoToEntityTask;
import adris.altoclef.tasks.AbstractKillEntityTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.IProgressChecker;
import adris.altoclef.util.progresscheck.LinearProgressChecker;
import adris.altoclef.util.progresscheck.ProgressCheckerRetry;
import net.minecraft.entity.Entity;

public class KillPlayerTask extends AbstractKillEntityTask {

    private final String _playerName;

    private final IProgressChecker<Double> _distancePlayerCheck = new ProgressCheckerRetry<>(new LinearProgressChecker(5, -2), 3);

    public KillPlayerTask(String name) {
        super(5, 7, 1);
        _playerName = name;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // If we're closer to the player, our task isn't bad.
        Entity player = getEntityTarget(mod);
        if (player == null) {
            _distancePlayerCheck.reset();
        } else {
            double distSq = player.squaredDistanceTo(mod.getPlayer());
            if (distSq < 10 * 10) {
                _distancePlayerCheck.reset();
            }
            _distancePlayerCheck.setProgress(-1 * distSq);
            if (!_distancePlayerCheck.failed()) {
                _progress.reset();
            }
        }
        return super.onTick(mod);
    }

    @Override
    protected boolean isSubEqual(AbstractDoToEntityTask other) {
        if (other instanceof KillPlayerTask) {
            return ((KillPlayerTask) other)._playerName.equals(_playerName);
        }
        return false;
    }

    @Override
    protected Entity getEntityTarget(AltoClef mod) {
        if (mod.getEntityTracker().isPlayerLoaded(_playerName)) {
            setDebugState("Killing player...");
            return mod.getEntityTracker().getPlayerEntity(_playerName);
        }
        setDebugState("Player not found");
        return null;
    }

    @Override
    protected String toDebugString() {
        return "Punking " + _playerName;
    }
}

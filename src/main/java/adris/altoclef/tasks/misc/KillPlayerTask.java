package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.AbstractDoToEntityTask;
import adris.altoclef.tasks.AbstractKillEntityTask;
import net.minecraft.entity.Entity;

public class KillPlayerTask extends AbstractKillEntityTask {

    private final String _playerName;

    public KillPlayerTask(String name) {
        _playerName = name;
    }

    @Override
    protected boolean isSubEqual(AbstractDoToEntityTask other) {
        if (other instanceof KillPlayerTask) {
            return ((KillPlayerTask)other)._playerName.equals(_playerName);
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

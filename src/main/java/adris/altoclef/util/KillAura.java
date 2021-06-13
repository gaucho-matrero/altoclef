package adris.altoclef.util;

import adris.altoclef.AltoClef;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireballEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls and applies killaura
 */
public class KillAura {

    // Smart aura data
    private final List<Entity> _targets = new ArrayList<>();
    private final TimerGame _hitDelay = new TimerGame(0.2);
    private double _forceFieldRange = Double.POSITIVE_INFINITY;
    private Entity _forceHit = null;

    public void tickStart(AltoClef mod) {
        _targets.clear();
        _forceHit = null;
    }

    public void applyAura(AltoClef mod, Entity entity) {
        _targets.add(entity);
        // Always hit ghast balls.
        if (entity instanceof FireballEntity) _forceHit = entity;
    }

    public void tickEnd(AltoClef mod) {
        // Run force field on map
        switch (mod.getModSettings().getForceFieldStrategy()) {
            case FASTEST:
                // Just attack whenever you can
                for (Entity entity : _targets) {
                    attack(mod, entity);
                }
                break;
            case SMART:
                // Attack force mobs ALWAYS.
                if (_forceHit != null) {
                    attack(mod, _forceHit);
                    break;
                }
                if (_hitDelay.elapsed()) {
                    _hitDelay.reset();
                    Entity toHit = Util.minItem(_targets, (left, right) -> {
                        double distComp = right.squaredDistanceTo(mod.getPlayer()) - left.squaredDistanceTo(mod.getPlayer());
                        return (int) Math.signum(distComp);
                    });
                    attack(mod, toHit);
                }
                break;
            case OFF:
                break;
        }
    }

    public void setRange(double range) {
        _forceFieldRange = range;
    }

    private boolean attack(AltoClef mod, Entity entity) {
        if (entity == null) return false;
        if (Double.isInfinite(_forceFieldRange) || entity.squaredDistanceTo(mod.getPlayer()) < _forceFieldRange * _forceFieldRange) {
            // Equip non-tool
            mod.getInventoryTracker().deequipHitTool();
            mod.getControllerExtras().attack(entity);
            return true;
        }
        return false;
    }

    public enum Strategy {
        OFF,
        FASTEST,
        SMART
    }

}

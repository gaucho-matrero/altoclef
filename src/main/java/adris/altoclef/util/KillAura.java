package adris.altoclef.util;


import adris.altoclef.AltoClef;
import adris.altoclef.util.csharpisbetter.Timer;
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
    private final List<Entity> targets = new ArrayList<>();
    private final Timer hitDelay = new Timer(0.2);
    private double forceFieldRange = Double.POSITIVE_INFINITY;
    private Entity forceHit;

    public void tickStart(AltoClef mod) {
        targets.clear();
        forceHit = null;
    }

    public void applyAura(AltoClef mod, Entity entity) {
        targets.add(entity);
        // Always hit ghast balls.
        if (entity instanceof FireballEntity) forceHit = entity;
    }

    public void tickEnd(AltoClef mod) {
        // Run force field on map
        switch (mod.getModSettings().getForceFieldStrategy()) {
            case FASTEST:
                // Just attack whenever you can
                for (Entity entity : targets) {
                    attack(mod, entity);
                }
                break;
            case SMART:
                // Attack force mobs ALWAYS.
                if (forceHit != null) {
                    attack(mod, forceHit);
                    break;
                }
                if (hitDelay.elapsed()) {
                    hitDelay.reset();
                    Entity toHit = Util.minItem(targets, (left, right) -> {
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
        forceFieldRange = range;
    }

    private boolean attack(AltoClef mod, Entity entity) {
        if (entity == null)
            return false;
        if (Double.isInfinite(forceFieldRange) || entity.squaredDistanceTo(mod.getPlayer()) < forceFieldRange * forceFieldRange) {
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

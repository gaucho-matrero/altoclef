package adris.altoclef.control;

import adris.altoclef.AltoClef;
import adris.altoclef.util.time.TimerGame;
import adris.altoclef.util.helpers.StlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
                performFastestAttack(mod);
                break;
            case SMART:

                if (_targets.size() <= 2 || _targets.stream().allMatch(entity -> entity instanceof SkeletonEntity) ) {
                    performDelayedAttack(mod);
                } else {
                    // Attack force mobs ALWAYS.
                    if (_forceHit != null) {
                        attack(mod, _forceHit);
                        break;
                    }
                    if (_hitDelay.elapsed()) {
                        _hitDelay.reset();

                        Optional<Entity> toHit = _targets.stream().min(StlHelper.compareValues(entity -> entity.squaredDistanceTo(mod.getPlayer())));

                        toHit.ifPresent(entity -> attack(mod, entity));
                    }
                }
                break;
            case DELAY:
                performDelayedAttack(mod);
                break;
            case OFF:
                break;
        }
    }

    public void setRange(double range) {
        _forceFieldRange = range;
    }

    private void performDelayedAttack(AltoClef mod) {
        // wait for the attack delay
        if (_targets.isEmpty()) {
            return;
        }

        Optional<Entity> toHit = _targets.stream().min(StlHelper.compareValues(entity -> entity.squaredDistanceTo(mod.getPlayer())));

        if (mod.getPlayer() == null || mod.getPlayer().getAttackCooldownProgress(0) < 1) {
            return;
        }

        toHit.ifPresent(entity -> attack(mod, entity, true));
    }

    private void performFastestAttack(AltoClef mod) {
        // Just attack whenever you can
        for (Entity entity : _targets) {
            attack(mod, entity);
        }
    }

    private void attack(AltoClef mod, Entity entity) {
        attack(mod, entity, false);
    }
    private void attack(AltoClef mod, Entity entity, boolean equipSword) {
        if (entity == null) return;
        if (Double.isInfinite(_forceFieldRange) || entity.squaredDistanceTo(mod.getPlayer()) < _forceFieldRange * _forceFieldRange) {
            boolean canAttack;
            if (equipSword) {
                // Equip sword, or if we don't have one just use our fists.
                Item[] swordsTopPriorityFirst = new Item[] {Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.STONE_SWORD, Items.WOODEN_SWORD};
                if (mod.getItemStorage().hasItem(swordsTopPriorityFirst)) {
                    canAttack = mod.getSlotHandler().forceEquipItem(swordsTopPriorityFirst);
                } else {
                    canAttack = mod.getSlotHandler().forceDeequipHitTool();
                }
            } else {
                // Equip non-tool
                canAttack = mod.getSlotHandler().forceDeequipHitTool();
            }
            if (canAttack) {
                mod.getControllerExtras().attack(entity);
            }
        }
    }

    public enum Strategy {
        OFF,
        FASTEST,
        DELAY,
        SMART
    }

}

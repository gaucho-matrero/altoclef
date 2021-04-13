package adris.altoclef.util;

import adris.altoclef.AltoClef;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.PlayerInventorySlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls and applies killaura
 */
public class KillAura {

    public enum Strategy {
        FASTEST,
        SMART
    }

    private double _forceFieldRange = Double.POSITIVE_INFINITY;

    // Smart aura data
    private final List<Entity> _targets = new ArrayList<>();
    private final Timer _hitDelay = new Timer(0.2);
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
                        return (int)Math.signum(distComp);
                    });
                    attack(mod, toHit);
                }
                break;
        }
    }

    public void setRange(double range) {
        _forceFieldRange = range;
    }

    private boolean attack(AltoClef mod, Entity entity) {
        if (entity == null) return false;
        if (Double.isInfinite(_forceFieldRange) || entity.squaredDistanceTo(mod.getPlayer()) < _forceFieldRange*_forceFieldRange) {
            // Equip non-tool
            deequipTool(mod);
            mod.getControllerExtras().attack(entity);
            return true;
        }
        return false;
    }

    private void deequipTool(AltoClef mod) {
        boolean toolEquipped = false;
        Item equip = mod.getInventoryTracker().getItemStackInSlot(PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND)).getItem();
        if (equip instanceof ToolItem) {
            // Pick non tool item or air
            if (mod.getInventoryTracker().getEmptySlotCount() == 0) {
                for (int i = 0; i < 35; ++i) {
                    Slot s = Slot.getFromInventory(i);
                    Item item = mod.getInventoryTracker().getItemStackInSlot(s).getItem();
                    if (!(item instanceof ToolItem)) {
                        mod.getInventoryTracker().equipSlot(s);
                        break;
                    }
                }
            } else {
                mod.getInventoryTracker().equipItem(Items.AIR);
            }
        }
    }

}

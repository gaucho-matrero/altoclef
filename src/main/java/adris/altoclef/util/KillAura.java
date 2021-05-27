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

    public enum Strategy {
        OFF,
        FASTEST,
        SMART
    }
}

package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public abstract class AbstractKillEntityTask extends AbstractDoToEntityTask {

    private static final double OTHER_FORCE_FIELD_RANGE = 2;

    // Not the "striking" distance, but the "ok we're close enough, lower our guard for other mobs and focus on this one" range.
    private static final double CONSIDER_COMBAT_RANGE = 10;

    private static final Item[] WEAPON_ITEMS = new Item[]{
            Items.DIAMOND_SWORD,
            Items.IRON_SWORD,
            Items.STONE_SWORD,
            Items.WOODEN_SWORD,
            Items.GOLDEN_SWORD,
            Items.DIAMOND_AXE,
            Items.IRON_AXE,
            Items.STONE_AXE,
            Items.WOODEN_AXE,
            Items.GOLDEN_AXE
    };

    private static final double MAINTAIN_DISTANCE = 3;

    public AbstractKillEntityTask() {
        this(MAINTAIN_DISTANCE, CONSIDER_COMBAT_RANGE, OTHER_FORCE_FIELD_RANGE);
    }

    public AbstractKillEntityTask(double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        super(maintainDistance, combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    public static void equipWeapon(AltoClef mod) {
        for (Item item : WEAPON_ITEMS) {
            if (mod.getInventoryTracker().hasItem(item)) {
                mod.getInventoryTracker().equipItem(item);
                return;
            }
        }
    }

    @Override
    protected Task onEntityInteract(AltoClef mod, Entity entity) {
        float hitProg = mod.getPlayer().getAttackCooldownProgress(0);

        // Equip weapon
        equipWeapon(mod);
        if (hitProg >= 0.99) {
            mod.getController().attackEntity(mod.getPlayer(), entity);
        }
        return null;
    }
}

package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.slot.MoveItemToSlotFromInventoryTask;
import adris.altoclef.tasks.squashed.CataloguedResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.screen.PlayerScreenHandler;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

public class EquipArmorTask extends Task {

    private final ItemTarget[] _toEquip;

    public EquipArmorTask(ItemTarget... toEquip) {
        _toEquip = toEquip;
    }
    public EquipArmorTask(Item... toEquip) {
        this(Arrays.stream(toEquip).map(ItemTarget::new).toArray(ItemTarget[]::new));
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        ItemTarget[] armorsNotEquipped = Arrays.stream(_toEquip).filter(target -> !StorageHelper.itemTargetsMetInventory(mod, target) && !StorageHelper.isArmorEquipped(mod, target.getMatches())).toArray(ItemTarget[]::new);
        boolean armorMet = armorsNotEquipped.length == 0;
        if (!armorMet) {
            setDebugState("Obtaining armor");
            return new CataloguedResourceTask(armorsNotEquipped);
        }

        setDebugState("Equipping armor");

        // Now equip
        for (ItemTarget targetArmor : _toEquip) {
            ArmorItem item = (ArmorItem) Objects.requireNonNull(targetArmor.getMatches())[0];
            if (item == null) {
                Debug.logWarning("Item " + targetArmor + " is not armor! Will not equip.");
            } else {
                if (!StorageHelper.isArmorEquipped(mod, item)) {
                    if (!(mod.getPlayer().currentScreenHandler instanceof PlayerScreenHandler)) {
                        StorageHelper.closeScreen();
                    }
                    Slot toMove = PlayerSlot.getEquipSlot(item.getSlotType());
                    if (toMove == null) {
                        Debug.logWarning("Invalid armor equip slot for item " + item.getTranslationKey() + ": " + item.getSlotType());
                    }
                    return new MoveItemToSlotFromInventoryTask(targetArmor, toMove);
                }
            }
        }

        return null;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return armorEquipped(mod);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof EquipArmorTask task) {
            return Arrays.equals(task._toEquip, _toEquip);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Equipping armor " + ArrayUtils.toString(_toEquip);
    }

    private boolean armorTestAll(Predicate<Item> armorSatisfies) {
        // If ALL item target has any match that is equipped...
        return Arrays.stream(_toEquip).allMatch(
                target -> Arrays.stream(target.getMatches()).anyMatch(armorSatisfies)
        );
    }

    public boolean armorEquipped(AltoClef mod) {
        return armorTestAll(item -> StorageHelper.isArmorEquipped(mod, item));
    }

}

package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.squashed.CataloguedResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Objects;
import java.util.function.Predicate;

public class EquipArmorTask extends Task {

    private final String[] _toEquip;

    private final TimerGame _moveTimer = new TimerGame(0.5f);

    public EquipArmorTask(String... toEquip) {
        _toEquip = toEquip;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {

        ItemTarget[] targets = new ItemTarget[_toEquip.length];
        int i = 0;
        boolean armorMet = true;
        for (String armor : _toEquip) {
            ItemTarget target = new ItemTarget(armor, 1);
            targets[i] = target;
            if (!mod.getInventoryTracker().targetMet(target)) {
                armorMet = false;
            }
            ++i;
        }
        if (!armorMet) {
            setDebugState("Obtaining armor");
            return new CataloguedResourceTask(targets);
        }

        setDebugState("Equipping armor");

        // Now equip

        if (_moveTimer.elapsed()) {
            _moveTimer.reset();
            for (String armor : _toEquip) {
                ArmorItem item = (ArmorItem) TaskCatalogue.getItemMatches(armor)[0];
                if (item == null) {
                    Debug.logWarning("Item " + armor + " is not armor! Will not equip.");
                } else {
                    if (!mod.getInventoryTracker().isArmorEquipped(item)) {
                        mod.getPlayer().closeHandledScreen();
                        Slot toMove = PlayerSlot.getEquipSlot(item.getSlotType());
                        if (toMove == null) {
                            Debug.logWarning("Invalid armor equip slot for item " + item.getTranslationKey() + ": " + item.getSlotType());
                        }
                        mod.getInventoryTracker().moveItemToSlot(item, 1, toMove);
                    }
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
    protected boolean isEqual(Task obj) {
        if (obj instanceof EquipArmorTask) {
            EquipArmorTask other = (EquipArmorTask) obj;
            return Util.arraysEqual(other._toEquip, _toEquip);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Equipping armor " + ArrayUtils.toString(_toEquip);
    }

    private boolean armorTestAll(AltoClef mod, Predicate<Item> armorSatisfies) {
        for (String armor : _toEquip) {
            assert TaskCatalogue.taskExists(armor);
            boolean found = false;
            for (Item item : Objects.requireNonNull(TaskCatalogue.getItemMatches(armor))) {
                if (armorSatisfies.test(item)) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }
        return true;
    }

    public boolean hasArmor(AltoClef mod) {
        return armorTestAll(mod, item -> mod.getInventoryTracker().isArmorEquipped(item) || mod.getInventoryTracker().hasItem(item));
    }

    public boolean armorEquipped(AltoClef mod) {
        return armorTestAll(mod, item -> mod.getInventoryTracker().isArmorEquipped(item));
    }

}

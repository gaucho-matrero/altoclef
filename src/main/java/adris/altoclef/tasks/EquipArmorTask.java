package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ArmorItem;

import org.apache.commons.lang3.ArrayUtils;
import sun.security.util.ArrayUtil;

public class EquipArmorTask extends Task {

    private final String[] _toEquip;

    private final Timer _moveTimer = new Timer(0.5f);

    public EquipArmorTask(String ...toEquip) {
        _toEquip = toEquip;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        for (String armor : _toEquip) {
            // TODO: Merge tasks with resource task multiple group thing
            ItemTarget target = new ItemTarget(armor, 1);
            if (!mod.getInventoryTracker().targetMet(target)) {
                setDebugState("Getting " + armor);
                return TaskCatalogue.getItemTask(armor, 1);
            }
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
        for (String armor : _toEquip) {
            ArmorItem item = (ArmorItem) TaskCatalogue.getItemMatches(armor)[0];
            if (item == null) {
                Debug.logWarning("Item " + armor + " is not armor! Will not equip.");
            } else {
                if (!mod.getInventoryTracker().isArmorEquipped(item)) return false;
            }
        }
        return true;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof EquipArmorTask) {
            EquipArmorTask other = (EquipArmorTask) obj;
            if (other._toEquip.length != _toEquip.length) return false;
            for (int i = 0; i < _toEquip.length; ++i) {
                if (!other._toEquip[i].equals(_toEquip[i])) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Equipping armor " + ArrayUtils.toString(_toEquip);
    }
}

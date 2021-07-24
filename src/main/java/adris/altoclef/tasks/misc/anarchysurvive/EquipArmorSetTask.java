package adris.altoclef.tasks.misc.anarchysurvive;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.item.Item;

import java.util.Objects;

/**
 * USE EquipArmorTask instead!!!
 */
@Deprecated
public class EquipArmorSetTask extends Task {

    private String[] _armors;

    public EquipArmorSetTask(String[] armorNames) {
        _armors = armorNames;
    }
    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        //if (hasArmor(mod)) {
//            return new EquipArmorTask(_armors);
//        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof EquipArmorSetTask) {
            EquipArmorSetTask task = (EquipArmorSetTask) obj;
            return Util.arraysEqual(task._armors, _armors);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Equipping armors: " + Util.arrayToString(_armors);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        for (String armor : _armors) {
            boolean found = false;
            assert TaskCatalogue.taskExists(armor);
            for (Item item : Objects.requireNonNull(TaskCatalogue.getItemMatches(armor))) {
                if (mod.getInventoryTracker().isArmorEquipped(item)) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }
        return true;
    }

}

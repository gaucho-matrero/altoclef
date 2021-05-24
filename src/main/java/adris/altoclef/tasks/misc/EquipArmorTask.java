package adris.altoclef.tasks.misc;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.CataloguedResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ArmorItem;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Objects;


public class EquipArmorTask extends Task {
    private final String[] toEquip;
    private final Timer moveTimer = new Timer(0.5f);
    
    public EquipArmorTask(String... toEquip) {
        this.toEquip = toEquip;
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        for (String armor : toEquip) {
            ArmorItem item = (ArmorItem) Objects.requireNonNull(TaskCatalogue.getItemMatches(armor))[0];
            if (item == null) {
                Debug.logWarning("Item " + armor + " is not armor! Will not equip.");
            } else {
                if (!mod.getInventoryTracker().isArmorEquipped(item)) return false;
            }
        }
        return true;
    }
    
    @Override
    protected void onStart(AltoClef mod) {
    
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        
        ItemTarget[] targets = new ItemTarget[toEquip.length];
        int i = 0;
        boolean armorMet = true;
        for (String armor : toEquip) {
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
        
        if (moveTimer.elapsed()) {
            moveTimer.reset();
            for (String armor : toEquip) {
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
    protected void onStop(AltoClef mod, Task interruptTask) {
    
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof EquipArmorTask) {
            EquipArmorTask other = (EquipArmorTask) obj;
            return Util.arraysEqual(other.toEquip, toEquip);
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Equipping armor " + ArrayUtils.toString(toEquip);
    }
}

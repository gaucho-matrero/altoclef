package adris.altoclef.tasks.misc;


import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ArmorRequirement;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Items;

import java.util.Arrays;

public class GatherStrongGearTask extends Task {


    private final Task _getPickTask = TaskCatalogue.getItemTask(new ItemTarget(Items.DIAMOND_PICKAXE));
    private final Task _equipArmorTask = new EquipArmorTask(Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS, Items.DIAMOND_HELMET,
            Items.DIAMOND_BOOTS);
    private final Task _getToolsTask = TaskCatalogue.getSquashedItemTask(new ItemTarget(Items.DIAMOND_SWORD),
            new ItemTarget(Items.DIAMOND_SHOVEL),
            new ItemTarget(Items.DIAMOND_AXE),
            new ItemTarget(Items.DIAMOND_HOE));

    public GatherStrongGearTask(AltoClef mod) {
        super();
    }

    @Override
    protected void onStart(AltoClef mod) {

    }
        //Get all three



    @Override
    protected Task onTick(AltoClef mod) {
        int TaskOrderVariable =  (mod.getInventoryTracker().hasItem(new ItemTarget(Items.DIAMOND_PICKAXE))
                ?3:0)
                +(Arrays.stream(ItemHelper.DIAMOND_ARMORS).allMatch(armor -> mod.getInventoryTracker().isArmorEquipped(armor))?5:0)
                +(mod.getInventoryTracker().hasAllItems(Items.DIAMOND_HOE,
                Items.DIAMOND_SWORD,Items.DIAMOND_SHOVEL,Items.DIAMOND_AXE)?7
                :0); // Clever boolean check
        switch(TaskOrderVariable){
            case 0, 5, 7, 12 -> { // We have nothing...fuck
                return _getPickTask;

            }
            case 3, 10 -> { // We have only the pick
                return _equipArmorTask;

            }

            case 8 -> { //We have the pick and the armor but still need tools
                return _getToolsTask;
            }
            default -> {
                stop(mod);
            }
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        return false;
    }

    @Override
    protected String toDebugString() {
        return null;
    }
}

package adris.altoclef.tasks.misc;


import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ArmorRequirement;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.function.Predicate;

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
        setDebugState("Getting better gear");
        int TaskOrderVariable =  (mod.getInventoryTracker().hasItem(new ItemTarget(Items.DIAMOND_PICKAXE))
                ?3:0)
                +(Arrays.stream(ItemHelper.DIAMOND_ARMORS).allMatch(armor -> mod.getInventoryTracker().isArmorEquipped(armor))?5:0)
                +(mod.getInventoryTracker().hasAllItems(Items.DIAMOND_HOE,
                Items.DIAMOND_SWORD,Items.DIAMOND_SHOVEL,Items.DIAMOND_AXE)?7
                :0); // Clever boolean check
        ItemEntity closest = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), Items.CRAFTING_TABLE);

        switch(TaskOrderVariable){

            case 0, 5, 7, 12 -> { // We have nothing...fuck
                setDebugState("Getting better pickaxe");
                if(mod.getInventoryTracker().hasItem(Items.CRAFTING_TABLE)) {
                    return _getPickTask;
                }else{
                    return TaskCatalogue.getSquashedItemTask(new ItemTarget(Items.CRAFTING_TABLE));
                }

            }
            case 3, 10 -> { // We have only the pick or we have everything
                // but armor
                setDebugState("Getting better armor");


                if (mod.getInventoryTracker().hasItem(Items.CRAFTING_TABLE) && !closest.isInRange(mod.getPlayer(), 10)){
                    // TODO Place crafting table instead of going after it.
                    return _equipArmorTask;
                }else{
                    return TaskCatalogue.getSquashedItemTask(new ItemTarget(Items.CRAFTING_TABLE));
                }

            }

            case 8 -> { //We have the pick and the armor but still need tools
                setDebugState("Getting better tools");
                if(mod.getInventoryTracker().hasItem(Items.CRAFTING_TABLE)) {
                    return _getToolsTask;
                }else{
                    return TaskCatalogue.getSquashedItemTask(new ItemTarget(Items.CRAFTING_TABLE));
                }
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

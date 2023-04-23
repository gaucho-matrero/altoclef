package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ItemStorageTracker;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class SelfCareTask extends Task {
    private static final Item[] armorSet = new Item[]{
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS
    };
    private static final ItemTarget[] toolSet = new ItemTarget[]{
            new ItemTarget(Items.IRON_SWORD, 1),
            new ItemTarget(Items.IRON_PICKAXE, 1),
            new ItemTarget(Items.IRON_AXE, 1),
            new ItemTarget(Items.IRON_SHOVEL, 1),
    };
    private static Task getToolSet;
    private static boolean isTaskNotFinished(AltoClef mod, Task task){
        return task != null && task.isActive() && !task.isFinished(mod);
    }
    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        boolean hasToolSet = mod.getItemStorage().hasItem(toolSet);
        boolean hasArmorSet = StorageHelper.isArmorEquippedAll(mod, armorSet);
        if (isTaskNotFinished(mod, getToolSet)){
            setDebugState("Getting tools");
            return getToolSet;
        }
        getToolSet = null;
        if (!hasToolSet){
            getToolSet = TaskCatalogue.getSquashedItemTask(toolSet);
            return getToolSet;
        }
        if (!mod.getFoodChain().hasFood()){

        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof SelfCareTask;
    }

    @Override
    protected String toDebugString() {
        return "Caring self";
    }
}

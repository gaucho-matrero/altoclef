package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ItemStorageTracker;
import adris.altoclef.util.helpers.StorageHelper;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class SelfCareTask extends Task {
    private static final Item[] armorSet = new Item[]{
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS
    };
    private static Item[] toolSet = new Item[]{
            Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL
    };
    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        boolean hasToolSet = mod.getItemStorage().hasItem(toolSet);
        boolean hasArmorSet = StorageHelper.isArmorEquippedAll(mod, armorSet);
        if (!hasToolSet){}
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

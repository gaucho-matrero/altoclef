package adris.altoclef.util;

import adris.altoclef.Debug;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.resources.CollectPlanksTask;
import net.minecraft.item.Item;

public interface TaskCatalogue {

    static ResourceTask getItemTask(Item item, int count) {
        return getItemTask(ItemTarget.trimItemName(item.getTranslationKey()), count);
    }

    static ResourceTask getItemTask(String name, int count) {
        Debug.logMessage("GET: " + name);

        switch (name) {
            case "planks":
                return new CollectPlanksTask(count);
        }

        // CATALOGUE
        return null;
    }

}

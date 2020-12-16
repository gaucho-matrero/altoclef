package adris.altoclef.util;

import adris.altoclef.Debug;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.ResourceTask;

public interface TaskCatalogue {

    /*
    static ResourceTask getItemTask(ItemTarget target) {
        return getItemTask(trimItemName(target.item.getTranslationKey()), target.targetCount);
    }
     */

    static ResourceTask getItemTask(String name, int count) {
        Debug.logMessage("GET: " + name);

        /*
        switch (name) {
            case "planks":
                return new MineAndCollectTask()
        }
        */

        // CATALOGUE
        return null;
    }

}

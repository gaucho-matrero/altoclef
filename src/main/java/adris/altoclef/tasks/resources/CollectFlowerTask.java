package adris.altoclef.tasks.resources;

import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.ItemUtil;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.csharpisbetter.Util;

public class CollectFlowerTask extends MineAndCollectTask {
    public CollectFlowerTask(int count) {
        super(new ItemTarget(ItemUtil.FLOWER, count), Util.itemsToBlocks(ItemUtil.FLOWER), MiningRequirement.HAND);
    }
}

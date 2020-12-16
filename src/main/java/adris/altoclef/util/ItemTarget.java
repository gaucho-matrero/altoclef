package adris.altoclef.util;

import adris.altoclef.AltoClef;
import baritone.api.utils.BlockUtils;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ItemTarget {
    public Item item;
    public int targetCount;

    public ItemTarget(Item item, int targetCount) {
        this.item = item;
        this.targetCount = targetCount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemTarget) {
            ItemTarget other = (ItemTarget) obj;
            if (targetCount != other.targetCount) return false;
            return itemEquals(item, other.item);
        }
        return false;
    }

    @Override
    public String toString() {
        return item.getTranslationKey() + " x " + targetCount;
    }

    public static boolean itemEquals(Item item1, Item item2) {
        return Item.getRawId(item1) == Item.getRawId(item2);
    }

    public static Item[] getItemArray(AltoClef mod, Collection<ItemTarget> targets) {
        List<Item> targetItems = new ArrayList<>();
        for(ItemTarget target : targets) {
            if (mod.getInventoryTracker().targetReached(target)) continue;
            targetItems.add(target.item);
        }
        Item[] targetItemsArray = new Item[targetItems.size()];
        targetItems.toArray(targetItemsArray);

        return targetItemsArray;

    }
}

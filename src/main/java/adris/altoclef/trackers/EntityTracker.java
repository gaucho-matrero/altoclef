package adris.altoclef.trackers;

import adris.altoclef.Debug;
import adris.altoclef.util.ItemTarget;
import baritone.Baritone;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class EntityTracker extends Tracker {

    private HashMap<Item, List<ItemEntity>> _itemDropLocations = new HashMap<>();

    public EntityTracker(TrackerManager manager) {
        super(manager);
    }

    public ItemEntity getClosestItemDrop(Vec3d position, Collection<ItemTarget> targets) {
        return getClosestItemDrop(position, ItemTarget.getItemArray(_mod, targets));
    }

    public ItemEntity getClosestItemDrop(Vec3d position, Item ...items) {
        ensureUpdated();
        if (items.length == 0) {
            Debug.logError("You asked for the drop position of zero items... Most likely a typo.");
            return null;
        }
        if (!itemDropped(items)) {
            Debug.logError("You forgot to check for whether item (example): " + items[0].getTranslationKey() + " was dropped before finding its drop location.");
            return null;
        }

        ItemEntity closestEntity = null;
        float minCost = Float.POSITIVE_INFINITY;
        for (Item item : items) {
            if (!itemDropped(item)) continue;
            for (ItemEntity entity : _itemDropLocations.get(item)) {
                float cost = (float) position.squaredDistanceTo(entity.getPos());
                if (cost < minCost) {
                    minCost = cost;
                    closestEntity = entity;
                }
            }
        }
        return closestEntity;
    }

    public boolean itemDropped(Item ...items) {
        ensureUpdated();
        for(Item item : items) {
            if (_itemDropLocations.containsKey(item)) return true;
        }
        return false;
    }

    @Override
    protected void updateState() {
        _itemDropLocations.clear();
        if (MinecraftClient.getInstance().world == null) return;

        // Loop through all entities and track 'em
        for(Entity entity : MinecraftClient.getInstance().world.getEntities()) {
            if (entity instanceof ItemEntity) {
                ItemEntity ientity = (ItemEntity) entity;
                Item droppedItem = ientity.getStack().getItem();

                if (!_itemDropLocations.containsKey(droppedItem)) {
                    _itemDropLocations.put(droppedItem, new ArrayList<>());
                }
                _itemDropLocations.get(droppedItem).add(ientity);
            }
        }
    }
}

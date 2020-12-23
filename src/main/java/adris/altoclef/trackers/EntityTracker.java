package adris.altoclef.trackers;

import adris.altoclef.Debug;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.ItemTarget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EntityTracker extends Tracker {

    private HashMap<Item, List<ItemEntity>> _itemDropLocations = new HashMap<>();

    private List<Vec3d> _blacklist = new ArrayList<>();

    public EntityTracker(TrackerManager manager) {
        super(manager);
    }

    public ItemEntity getClosestItemDrop(Vec3d position, Item ...items) {
        ItemTarget[] tempTargetList = new ItemTarget[items.length];
        for (int i = 0; i < items.length; ++i) {
            tempTargetList[i] = new ItemTarget(items[i], 9999999);
        }
        return getClosestItemDrop(position, tempTargetList);
        //return getClosestItemDrop(position, ItemTarget.getItemArray(_mod, targets));
    }

    public ItemEntity getClosestItemDrop(Vec3d position, ItemTarget ...targets) {
        ensureUpdated();
        if (targets.length == 0) {
            Debug.logError("You asked for the drop position of zero items... Most likely a typo.");
            return null;
        }
        if (!itemDropped(targets)) {
            Debug.logError("You forgot to check for whether item (example): " + targets[0].getMatches()[0].getTranslationKey() + " was dropped before finding its drop location.");
            return null;
        }

        ItemEntity closestEntity = null;
        float minCost = Float.POSITIVE_INFINITY;
        for (ItemTarget target : targets) {
            for (Item item : target.getMatches()) {
                if (!itemDropped(item)) continue;
                for (ItemEntity entity : _itemDropLocations.get(item)) {
                    if (isBlackListed(entity)) continue;
                    if (!entity.getStack().getItem().equals(item)) continue;

                    float cost = (float) BaritoneHelper.calculateGenericHeuristic(position, entity.getPos());
                    if (cost < minCost) {
                        minCost = cost;
                        closestEntity = entity;
                    }
                }
            }
        }
        return closestEntity;
    }
    private boolean isBlackListed(Entity entity) {
        if (entity == null) return false;
        return isBlackListed(entity.getPos());
    }
    private boolean isBlackListed(Vec3d pos) {
        for (Vec3d item : _blacklist) {
            double distSq = pos.squaredDistanceTo(item);
            if (distSq < 1) {
                return true;
            }
        }
        return false;
    }

    public void blacklist(Vec3d position) {
        _blacklist.add(position);
    }

    public void clearBlacklist() {
        _blacklist.clear();
    }

    public boolean itemDropped(Item ...items) {
        ensureUpdated();
        for(Item item : items) {
            if (_itemDropLocations.containsKey(item)) {
                // Find a non-blacklisted item
                for (ItemEntity entity : _itemDropLocations.get(item)) {
                    if (!isBlackListed(entity)) return true;
                }
            }
        }
        return false;
    }

    public boolean itemDropped(ItemTarget ...targets) {
        for (ItemTarget target : targets) {
            if (itemDropped(target.getMatches())) return true;
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

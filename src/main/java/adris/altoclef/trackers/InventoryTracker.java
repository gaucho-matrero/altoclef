package adris.altoclef.trackers;

import adris.altoclef.AltoClef;
import adris.altoclef.util.ItemTarget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InventoryTracker extends Tracker {

    private HashMap<Item, Integer> _itemCounts = new HashMap<>();
    private HashMap<Item, List<Integer>> _itemSlots = new HashMap<>();

    private int _emptySlots = 0;

    public InventoryTracker(TrackerManager manager) {
        super(manager);
    }


    public int getEmptySlotCount() {
        ensureUpdated();
        return _emptySlots;
    }
    public boolean hasItem(Item item) {
        ensureUpdated();
        return _itemCounts.containsKey(item);
    }
    public int getItemCount(Item item) {
        ensureUpdated();
        if (!hasItem(item)) return 0;
        return _itemCounts.get(item);
    }

    public boolean targetReached(ItemTarget ...targets) {
        for(ItemTarget target : targets) {
            if (getItemCount(target.item) < target.targetCount) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void updateState() {
        _itemCounts.clear();
        _itemSlots.clear();
        _emptySlots = 0;

        if (MinecraftClient.getInstance().player == null) {
            // No updating needed, we have nothing.
            return;
        }
        PlayerInventory inventory = MinecraftClient.getInstance().player.inventory;

        for (int slot = 0; slot < inventory.size(); ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                _emptySlots++;
                continue;
            }
            Item item = stack.getItem();
            int count = stack.getCount();
            if (!_itemCounts.containsKey(item)) {
                _itemCounts.put(item, 0);
            }
            if (!_itemSlots.containsKey(item)) {
                _itemSlots.put(item, new ArrayList<>());
            }
            _itemCounts.put(item, _itemCounts.get(item) + count);
            _itemSlots.get(item).add(slot);
        }
    }
}

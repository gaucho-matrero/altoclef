package adris.altoclef.trackers;

import adris.altoclef.Debug;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.InventorySlot;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.PlayerInventorySlot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

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
        ensureUpdated();

        for(ItemTarget target : targets) {
            if (getItemCount(target.item) < target.targetCount) {
                return false;
            }
        }
        return true;
    }

    public boolean hasRecipeMaterials(CraftingRecipe recipe) {
        ensureUpdated();

        // Matching
        List<CraftingRecipe.CraftingSlot> matchingSlots = new ArrayList<>(recipe.mustMatchCount());

        for (int i = 0; i < recipe.getSlotCount(); ++i) {
            CraftingRecipe.CraftingSlot slot = recipe.getSlot(i);

            if (recipe.mustMatch(i)) {
                matchingSlots.add(slot);
            }

            boolean foundItem = false;
            // Make sure we have at least one of the requirements
            for (Item item : slot.getTargetItems()) {
                if (hasItem(item)) {
                    foundItem = true;
                }
            }
            if (!foundItem) {
                // Failure to find item required for this slot.
                return false;
            }
        }

        // Now handle matching
        if (!matchingSlots.isEmpty()) {
            CraftingRecipe.CraftingSlot first = matchingSlots.get(0);
            int requiredCount = recipe.mustMatchCount();
            for (Item item : first.getTargetItems()) {
                // We found an item that fits that match.
                if (getItemCount(item) >= requiredCount) {
                    return true;
                }
            }
            // No combination of all items matching
            return false;
        }

        // We passed through the rings of fire
        return true;
    }

    public ItemStack clickSlot(InventorySlot slot, int mouseButton, SlotActionType type) {

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return null;
        }
        int syncId = player.currentScreenHandler.syncId;

        return _mod.getController().clickSlot(syncId, slot.getWindowSlot(), mouseButton, type, player);
    }
    public ItemStack clickSlot(InventorySlot slot, int mouseButton) {
        return clickSlot(slot, mouseButton, SlotActionType.PICKUP);
    }
    public ItemStack clickSlot(InventorySlot slot, SlotActionType type) {
        return clickSlot(slot, 0, type);
    }
    public ItemStack clickSlot(InventorySlot slot) {
        return clickSlot(slot, 0);
    }

    /**
     * @param from   Slot to start moving from
     * @param to     Slot to move items to
     * @param amount How many to move
     * @return       The number of items successfully transported
     */
    public int moveItems(InventorySlot from, InventorySlot to, int amount) {
        to.ensureWindowOpened();

        ItemStack fromStack = getItemStackInSlot(from);

        if (fromStack == null || fromStack.isEmpty()) {
            Debug.logInternal("(From stack is empty or null)");
            return 0;
        }

        ItemStack toStack = getItemStackInSlot(to);
        if (toStack != null && !toStack.isEmpty()) {
            if (!toStack.isItemEqual(fromStack)) {
                //Debug.logMessage("To was occupied, moved it elsewhere.");
                // We have stuff in our target slot. Move it out somewhere.
                clickSlot(to, SlotActionType.QUICK_MOVE);
            }
        }

        // Pickup
        ItemStack pickedUp = clickSlot(from);
        Debug.logMessage("Picked Up " + pickedUp.getCount() + " from slot " + from.getWindowSlot());

        int dropped;

        // Drop
        if (amount >= pickedUp.getCount()) {
            // We don't have enough/exactly enough, drop everything there

            clickSlot(to);
            Debug.logMessage("Dropped it all from slot " + to.getWindowSlot());

            dropped = pickedUp.getCount();
        } else {
            // We have too much in our stack, only move what we need.
            //j = 1;
            for (int i = 0; i < amount; ++i) {
                clickSlot(to, 1);
            }
            // We've picked up our stack, put it back
            clickSlot(from);
            dropped = amount;
        }
        setDirty();
        return dropped;
    }

    public void swapItems(InventorySlot slot1, InventorySlot slot2) {

        // Pick up slot1
        clickSlot(slot1);
        // Pick up slot2
        ItemStack placed = clickSlot(slot2);

        // slot 1 is now in slot 2

        // If slot 2 is not empty, move it back to slot 1
        if (placed != null && !placed.isEmpty()) {
            clickSlot(slot1);
        }
    }

    private ItemStack getItemStackInSlot(InventorySlot slot) {

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return null;

        //Debug.logMessage("FOOF WINDOW SLOT: " + slot.getWindowSlot() + ", " + slot.getInventorySlot());
        return player.currentScreenHandler.getSlot(slot.getWindowSlot()).getStack();
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

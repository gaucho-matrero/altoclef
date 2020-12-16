package adris.altoclef.util;

// Very helpful links
// Container Window Slots (used to move stuff around all containers, including player):
//      https://wiki.vg/Inventory
// Player Inventory Slots (used to grab inventory items only):
//      https://minecraft.gamepedia.com/Inventory
public abstract class InventorySlot {
    private int _inventorySlot;

    public InventorySlot(int windowSlot) {
        _inventorySlot = windowSlotToInventorySlot(windowSlot);
    }

    public int getInventorySlot() {
        return _inventorySlot;
    }

    public int getWindowSlot() {
        return inventorySlotToWindowSlot(_inventorySlot);
    }

    public void ensureWindowOpened() {}

    protected abstract int inventorySlotToWindowSlot(int inventorySlot);

    protected abstract int windowSlotToInventorySlot(int windowSlot);
}

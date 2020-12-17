package adris.altoclef.util.slots;

// Very helpful links
// Container Window Slots (used to move stuff around all containers, including player):
//      https://wiki.vg/Inventory
// Player Inventory Slots (used to grab inventory items only):
//      https://minecraft.gamepedia.com/Inventory
public abstract class Slot {
    private int _inventorySlot;
    private int _windowSlot;

    public Slot(int slot, boolean inventory) {
        if (inventory) {
            _inventorySlot = slot;
            _windowSlot = inventorySlotToWindowSlot(slot);
        } else {
            _inventorySlot = windowSlotToInventorySlot(slot);
            _windowSlot = slot;
        }
    }

    public int getInventorySlot() {
        return _inventorySlot;
    }

    public int getWindowSlot() {
        return _windowSlot;
    }

    public void ensureWindowOpened() {}

    protected abstract int inventorySlotToWindowSlot(int inventorySlot);

    protected abstract int windowSlotToInventorySlot(int windowSlot);

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "inventory slot = " + _inventorySlot +
                ", window slot = " + getWindowSlot() +
                '}';
    }
}

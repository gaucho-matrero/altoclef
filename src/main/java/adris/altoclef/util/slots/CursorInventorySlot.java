package adris.altoclef.util.slots;

public class CursorInventorySlot extends Slot {
    public CursorInventorySlot() {
        super(Slot.CURSOR_SLOT_INDEX, true);
    }

    @Override
    protected int inventorySlotToWindowSlot(int inventorySlot) {
        return Slot.CURSOR_SLOT_INDEX;
    }

    @Override
    protected int windowSlotToInventorySlot(int windowSlot) {
        return Slot.CURSOR_SLOT_INDEX;
    }

    @Override
    protected String getName() {
        return "Cursor Slot";
    }
}

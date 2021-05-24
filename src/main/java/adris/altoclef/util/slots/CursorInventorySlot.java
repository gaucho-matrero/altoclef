package adris.altoclef.util.slots;


public class CursorInventorySlot extends Slot {
    public CursorInventorySlot() {
        super(-1, true);
    }
    
    @Override
    protected int inventorySlotToWindowSlot(int inventorySlot) {
        return -1;
    }
    
    @Override
    protected int windowSlotToInventorySlot(int windowSlot) {
        return -1;
    }
    
    @Override
    protected String getName() {
        return "Cursor Slot";
    }
}

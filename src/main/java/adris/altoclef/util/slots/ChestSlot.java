package adris.altoclef.util.slots;


public class ChestSlot extends Slot {
    
    private final boolean isBig;
    
    public ChestSlot(int slot, boolean big) {
        this(slot, big, false);
    }
    
    public ChestSlot(int slot, boolean big, boolean inventory) {
        super(slot, inventory);
        isBig = big;
    }
    
    @Override
    public int inventorySlotToWindowSlot(int inventorySlot) {
        if (inventorySlot < 9) {
            return inventorySlot + (isBig ? 81 : 54);
        }
        return (inventorySlot - 9) + (isBig ? 54 : 27);
    }
    
    @Override
    protected int windowSlotToInventorySlot(int windowSlot) {
        int bottomStart = (isBig ? 81 : 54);
        if (windowSlot >= bottomStart) {
            return windowSlot - bottomStart;
        }
        return (windowSlot + 9) - (isBig ? 54 : 27);
    }
    
    @Override
    protected String getName() {
        return "Chest";
    }
}

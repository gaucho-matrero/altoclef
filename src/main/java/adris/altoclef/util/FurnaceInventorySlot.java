
package adris.altoclef.util;

public class FurnaceInventorySlot extends InventorySlot {
    public FurnaceInventorySlot(int windowSlot) {
        super(windowSlot);
    }

    @Override
    public int inventorySlotToWindowSlot(int inventorySlot) {
        if (inventorySlot < 9) {
            return inventorySlot + 30;
        }
        return inventorySlot - 6;
    }

    @Override
    protected int windowSlotToInventorySlot(int windowSlot) {
        if (windowSlot >= 30) {
            return windowSlot - 30;
        }
        return windowSlot + 6;
    }

    public static final FurnaceInventorySlot INPUT_SLOT_FUEL = new FurnaceInventorySlot(1);
    public static final FurnaceInventorySlot INPUT_SLOT_MATERIALS = new FurnaceInventorySlot(0);
    public static final FurnaceInventorySlot OUTPUT_SLOT = new FurnaceInventorySlot(2);
}

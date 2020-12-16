
package adris.altoclef.util;

public class CraftingTableInventorySlot extends InventorySlot {
    public CraftingTableInventorySlot(int windowSlot) {
        super(windowSlot);
    }

    @Override
    public int inventorySlotToWindowSlot(int inventorySlot) {
        if (inventorySlot < 9) {
            return inventorySlot + 37;
        }
        return inventorySlot + 1;
    }

    @Override
    protected int windowSlotToInventorySlot(int windowSlot) {
        if (windowSlot >= 37) {
            return windowSlot - 37;
        }
        return windowSlot - 1;
    }

    public static CraftingTableInventorySlot getInputSlot(int x, int y) {
        int index = 1 + (y * 3 + x);
        return new CraftingTableInventorySlot(index);
    }

    public static final CraftingTableInventorySlot OUTPUT_SLOT = new CraftingTableInventorySlot(0);
}

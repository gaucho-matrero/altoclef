package adris.altoclef.util.slots;

import adris.altoclef.Debug;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.SmithingScreen;
import net.minecraft.screen.GenericContainerScreenHandler;

// Very helpful links
// Container Window Slots (used to move stuff around all containers, including player):
//      https://wiki.vg/Inventory
// Player Inventory Slots (used to grab inventory items only):
//      https://minecraft.gamepedia.com/Inventory
public abstract class Slot {

    // -1 means cursor slot, the slot of the cursor when it holds an item.
    public static final int CURSOR_SLOT_INDEX = -1;
    private static final int UNDEFINED_SLOT_INDEX = -999;

    private final int _inventorySlot;
    private final int _windowSlot;

    private final boolean _isInventory;

    public Slot(int slot, boolean inventory) {
        _isInventory = inventory;
        if (inventory) {
            _inventorySlot = slot;
            _windowSlot = UNDEFINED_SLOT_INDEX;
            //_windowSlot = inventorySlotToWindowSlot(slot);
        } else {
            //_inventorySlot = windowSlotToInventorySlot(slot);
            _inventorySlot = UNDEFINED_SLOT_INDEX;
            _windowSlot = slot;
        }
    }

    public static Slot getFromInventory(int inventorySlot) {
        // -1 means cursor.
        if (inventorySlot == CURSOR_SLOT_INDEX) {
            return new CursorInventorySlot();
        }
        switch (getCurrentType()) {
            case PLAYER:
                return new PlayerInventorySlot(inventorySlot);
            case CRAFTING_TABLE:
                return new CraftingTableInventorySlot(inventorySlot);
            case FURNACE_OR_SMITH:
                return new FurnaceInventorySlot(inventorySlot);
            case CHEST_LARGE:
                return new ChestInventorySlot(inventorySlot, true);
            case CHEST_SMALL:
                return new ChestInventorySlot(inventorySlot, false);
        }
        Debug.logWarning("Unhandled slot for inventory check: " + getCurrentType());
        return null;
    }

    //@SuppressWarnings("CopyConstructorMissesField")
    /*public Slot(Slot other) {
        this(other._inventorySlot, true);
    }*/

    private static ContainerType getCurrentType() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen instanceof FurnaceScreen || screen instanceof SmithingScreen) {
            return ContainerType.FURNACE_OR_SMITH;
        }
        if (screen instanceof GenericContainerScreen) {
            GenericContainerScreenHandler handler = ((GenericContainerScreen) screen).getScreenHandler();
            boolean big = (handler.getRows() == 6);
            return big ? ContainerType.CHEST_LARGE : ContainerType.CHEST_SMALL;
        }
        if (screen instanceof CraftingScreen) {
            return ContainerType.CRAFTING_TABLE;
        }
        return ContainerType.PLAYER;
    }

    public int getInventorySlot() {
        if (!_isInventory) {
            return windowSlotToInventorySlot(_windowSlot);
        }
        return _inventorySlot;
    }

    public int getWindowSlot() {
        if (_isInventory) {
            return inventorySlotToWindowSlot(_inventorySlot);
        }
        return _windowSlot;
    }

    public void ensureWindowOpened() {
    }

    protected abstract int inventorySlotToWindowSlot(int inventorySlot);

    protected abstract int windowSlotToInventorySlot(int windowSlot);

    protected abstract String getName();

    @Override
    public String toString() {
        return getName() + (_isInventory ? "InventorySlot" : "Slot") + "{" +
                "inventory slot = " + getInventorySlot() +
                ", window slot = " + getWindowSlot() +
                '}';
    }

    enum ContainerType {
        PLAYER,
        CRAFTING_TABLE,
        CHEST_SMALL,
        CHEST_LARGE,
        FURNACE_OR_SMITH
    }
}

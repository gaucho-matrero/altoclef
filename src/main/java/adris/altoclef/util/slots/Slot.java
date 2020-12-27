package adris.altoclef.util.slots;

import adris.altoclef.Debug;
import adris.altoclef.tasks.MineAndCollectTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;

// Very helpful links
// Container Window Slots (used to move stuff around all containers, including player):
//      https://wiki.vg/Inventory
// Player Inventory Slots (used to grab inventory items only):
//      https://minecraft.gamepedia.com/Inventory
public abstract class Slot {
    private final int _inventorySlot;
    private final int _windowSlot;

    private final boolean _isInventory;

    enum ContainerType {
        PLAYER,
        CRAFTING_TABLE,
        CHEST_SMALL,
        CHEST_LARGE,
        FURNACE
    }

    public Slot(int slot, boolean inventory) {
        _isInventory = inventory;
        if (inventory) {
            _inventorySlot = slot;
            _windowSlot = inventorySlotToWindowSlot(slot);
        } else {
            _inventorySlot = windowSlotToInventorySlot(slot);
            _windowSlot = slot;
        }
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public Slot(Slot other) {
        this(other._inventorySlot, true);
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

    public static Slot getFromInventory(int inventorySlot) {
        // -1 means cursor.
        if (inventorySlot == -1) {
            return new CursorInventorySlot();
        }
        switch (getCurrentType()) {
            case PLAYER:
                return new PlayerInventorySlot(inventorySlot);
            case CRAFTING_TABLE:
                return new CraftingTableInventorySlot(inventorySlot);
            case CHEST_SMALL:
                return null;
            case CHEST_LARGE:
                return null;
            case FURNACE:
                return new FurnaceInventorySlot(inventorySlot);
        }
        return null;
    }

    private static ContainerType getCurrentType() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen instanceof FurnaceScreen) {
            return ContainerType.FURNACE;
        }
        if (screen instanceof GenericContainerScreen) {
            GenericContainerScreenHandler handler = ((GenericContainerScreen)screen).getScreenHandler();
            int size = handler.slots.size();
            Debug.logMessage("TODO PLEASE: CHECK: " + size);
            //int a = 1 / 0;
            return ContainerType.CHEST_LARGE;
        }
        if (screen instanceof CraftingScreen) {
            return ContainerType.CRAFTING_TABLE;
        }
        Debug.logInternal("SCREEN: " + screen);
        return ContainerType.PLAYER;
    }

    protected abstract String getName();

    @Override
    public String toString() {
        return getName() + (_isInventory? "InventorySlot" : "Slot") +  "{" +
                "inventory slot = " + _inventorySlot +
                ", window slot = " + getWindowSlot() +
                '}';
    }
}

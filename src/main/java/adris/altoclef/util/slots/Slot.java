package adris.altoclef.util.slots;


import adris.altoclef.Debug;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.GenericContainerScreenHandler;


// Very helpful links
// Container Window Slots (used to move stuff around all containers, including player):
//      https://wiki.vg/Inventory
// Player Inventory Slots (used to grab inventory items only):
//      https://minecraft.gamepedia.com/Inventory
public abstract class Slot {
    private final int inventorySlot;
    private final int windowSlot;

    private final boolean isInventory;


    public Slot(int slot, boolean inventory) {
        isInventory = inventory;
        if (inventory) {
            inventorySlot = slot;
            windowSlot = -1;
            //_windowSlot = inventorySlotToWindowSlot(slot);
        } else {
            //_inventorySlot = windowSlotToInventorySlot(slot);
            inventorySlot = -1;
            windowSlot = slot;
        }
    }

    public static Slot getFromInventory(int inventorySlot) {
        // -1 means cursor. // specify this somewhere, please
        if (inventorySlot == -1) {
            return new CursorInventorySlot();
        }
        switch (getCurrentType()) {
            case PLAYER:
                return new PlayerInventorySlot(inventorySlot);
            case CRAFTING_TABLE:
                return new CraftingTableInventorySlot(inventorySlot);
            case FURNACE:
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
        if (screen instanceof FurnaceScreen) {
            return ContainerType.FURNACE;
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
        if (!isInventory) {
            return windowSlotToInventorySlot(windowSlot);
        }
        return inventorySlot;
    }

    public int getWindowSlot() {
        if (isInventory) {
            return inventorySlotToWindowSlot(inventorySlot);
        }
        return windowSlot;
    }

    public void ensureWindowOpened() {
    }

    protected abstract int inventorySlotToWindowSlot(int inventorySlot);

    protected abstract int windowSlotToInventorySlot(int windowSlot);

    protected abstract String getName();

    @Override
    public String toString() {
        return getName() + (isInventory ? "InventorySlot" : "Slot") + "{" + "inventory slot = " + getInventorySlot() + ", window slot = " +
               getWindowSlot() + '}';
    }

    enum ContainerType {
        PLAYER,
        CRAFTING_TABLE,
        CHEST_SMALL,
        CHEST_LARGE,
        FURNACE
    }
}

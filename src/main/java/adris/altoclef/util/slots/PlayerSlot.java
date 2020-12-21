
package adris.altoclef.util.slots;

import adris.altoclef.Debug;

public class PlayerSlot extends Slot {
    public PlayerSlot(int windowSlot) {
        this(windowSlot, false);
    }
    protected PlayerSlot(int slot, boolean inventory) {
        super(slot, inventory);
    }

    @Override
    public int inventorySlotToWindowSlot(int inventorySlot) {
        if (inventorySlot < 9) {
            return inventorySlot + 36;
        }
        return inventorySlot;
    }

    @Override
    protected int windowSlotToInventorySlot(int windowSlot) {
        if (windowSlot >= 36) {
            return windowSlot - 36;
        }
        return windowSlot;
    }

    @Override
    public void ensureWindowOpened() {
        //Debug.logMessage("PLAYER INVENTORY OPENED");


        //ClientPlayerInteractionManager controller = MinecraftClient.getInstance().interactionManager;

        //MinecraftClient.getInstance().openScreen(new InventoryScreen(MinecraftClient.getInstance().player));

        //controller.clickButton();

        // Nope. Maybe you gotta send packets?
        //player.inventory.onOpen(player);

        /*
        Screen screen = new InventoryScreen(player);
        player.currentScreenHandler = ((ScreenHandlerProvider<PlayerScreenHandler>) screen).getScreenHandler();
        MinecraftClient.getInstance().openScreen(screen);
         */
    }

    public static PlayerSlot getCraftInputSlot(int x, int y) {
        return getCraftInputSlot(y * 2 + x);
    }
    public static PlayerSlot getCraftInputSlot(int index) {
        index += 1;
        return new PlayerSlot(index);
    }

    public static final PlayerSlot CRAFT_OUTPUT_SLOT = new PlayerSlot(0);
}

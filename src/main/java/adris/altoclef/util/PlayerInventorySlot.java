
package adris.altoclef.util;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import baritone.utils.accessor.IPlayerControllerMP;
import net.fabricmc.loader.game.MinecraftGameProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.MinecraftClientGame;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.MinecraftServer;

public class PlayerInventorySlot extends InventorySlot {
    public PlayerInventorySlot(int windowSlot) {
        super(windowSlot);
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
}

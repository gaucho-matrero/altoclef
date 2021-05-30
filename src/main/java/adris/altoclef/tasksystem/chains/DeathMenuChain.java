package adris.altoclef.tasksystem.chains;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.csharpisbetter.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;


public class DeathMenuChain extends TaskChain {
    // Sometimes we fuck up, so we might want to retry considering the death screen.
    private final Timer deathRetryTimer = new Timer(8);
    private final Timer reconnectTimer = new Timer(1);
    ServerInfo prevServerEntry;
    private boolean reconnecting;
    private int deathCount;
    private Class<? extends Screen> prevScreen;

    public DeathMenuChain(TaskRunner runner) {
        super(runner);
    }

    private boolean shouldAutoRespawn(AltoClef mod) {
        return mod.getModSettings().isAutoRespawn();
    }

    private boolean shouldAutoReconnect(AltoClef mod) {
        return mod.getModSettings().isAutoReconnect();
    }

    @Override
    protected void onStop(AltoClef mod) {

    }

    @Override
    public void onInterrupt(AltoClef mod, TaskChain other) {

    }

    @Override
    protected void onTick(AltoClef mod) {

    }

    @Override
    public float getPriority(AltoClef mod) {
        //MinecraftClient.getInstance().getCurrentServerEntry().address;
        //        MinecraftClient.getInstance().
        Screen screen = MinecraftClient.getInstance().currentScreen;

        // This might fix Weird fail to respawn that happened only once
        if (prevScreen == DeathScreen.class) {
            if (deathRetryTimer.elapsed()) {
                Debug.logMessage("(RESPAWN RETRY WEIRD FIX...)");
                deathRetryTimer.reset();
                prevScreen = null;
            }
        } else {
            deathRetryTimer.reset();
        }

        if (screen != null && screen.getClass() != prevScreen) {

            // Keep track of the last server we were on so we can re-connect.
            if (mod.inGame()) {
                prevServerEntry = MinecraftClient.getInstance().getCurrentServerEntry();
            }

            if (screen instanceof DeathScreen) {
                if (shouldAutoRespawn(mod)) {
                    deathCount++;
                    Debug.logMessage("RESPAWNING... (this is death #" + deathCount + ")");
                    MinecraftClient.getInstance().player.requestRespawn();
                    MinecraftClient.getInstance().openScreen(null);
                } else {
                    // Cancel if we die and are not auto-respawning.
                    mod.cancelUserTask();
                }
            } else if (screen instanceof DisconnectedScreen) {
                if (shouldAutoReconnect(mod)) {
                    Debug.logMessage("RECONNECTING: Going to Multiplayer Screen");
                    reconnecting = true;
                    MinecraftClient.getInstance().openScreen(new MultiplayerScreen(new TitleScreen()));
                } else {
                    // Cancel if we disconnect and are not auto-reconnecting.
                    mod.cancelUserTask();
                }
            } else if (screen instanceof MultiplayerScreen && reconnecting && reconnectTimer.elapsed()) {
                reconnectTimer.reset();
                Debug.logMessage("RECONNECTING: Going ");
                reconnecting = false;

                if (prevServerEntry == null) {
                    Debug.logWarning("Failed to re-connect to server, no server entry cached.");
                } else {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.openScreen(new ConnectScreen(screen, client, prevServerEntry));
                }
            }
            prevScreen = screen.getClass();
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String getName() {
        return "Death Menu Respawn Handling";
    }
}

package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.csharpisbetter.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;

import java.lang.reflect.Type;

public class DeathMenuChain extends TaskChain {

    private boolean shouldAutoRespawn(AltoClef mod) { return mod.getModSettings().isAutoRespawn(); }
    private boolean shouldAutoReconnect(AltoClef mod) {
        return mod.getModSettings().isAutoReconnect();
    }

    public DeathMenuChain(TaskRunner runner) {
        super(runner);
    }

    private boolean _reconnecting = false;

    ServerInfo _prevServerEntry = null;

    private Timer _reconnectTimer = new Timer(1);

    private int _deathCount = 0;

    private Class _prevScreen = null;

    // Sometimes we fuck up, so we might want to retry considering the death screen.
    private final Timer _deathRetryTimer = new Timer(8);

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
        if (_prevScreen == DeathScreen.class) {
            if (_deathRetryTimer.elapsed()) {
                Debug.logMessage("(RESPAWN RETRY WEIRD FIX...)");
                _deathRetryTimer.reset();
                _prevScreen = null;
            }
        } else {
            _deathRetryTimer.reset();
        }

        if (screen != null && screen.getClass() != _prevScreen) {

            // Keep track of the last server we were on so we can re-connect.
            if (mod.inGame()) {
                _prevServerEntry = MinecraftClient.getInstance().getCurrentServerEntry();
            }

            if (screen instanceof DeathScreen) {
                if (shouldAutoRespawn(mod)) {
                    _deathCount++;
                    Debug.logMessage("RESPAWNING... (this is death #" + _deathCount + ")");
                    assert MinecraftClient.getInstance().player != null;
                    MinecraftClient.getInstance().player.requestRespawn();
                    MinecraftClient.getInstance().openScreen(null);
                } else {
                    // Cancel if we die and are not auto-respawning.
                    mod.cancelUserTask();
                }
            } else if (screen instanceof DisconnectedScreen) {
                if (shouldAutoReconnect(mod)) {
                    Debug.logMessage("RECONNECTING: Going to Multiplayer Screen");
                    _reconnecting = true;
                    MinecraftClient.getInstance().openScreen(new MultiplayerScreen(new TitleScreen()));
                } else {
                    // Cancel if we disconnect and are not auto-reconnecting.
                    mod.cancelUserTask();
                }
            } else if (screen instanceof MultiplayerScreen && _reconnecting && _reconnectTimer.elapsed()) {
                _reconnectTimer.reset();
                Debug.logMessage("RECONNECTING: Going ");
                _reconnecting = false;

                if (_prevServerEntry == null) {
                    Debug.logWarning("Failed to re-connect to server, no server entry cached.");
                } else {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.openScreen(new ConnectScreen(screen, client, _prevServerEntry));
                }
            }
            _prevScreen = screen.getClass();
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

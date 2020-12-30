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
import net.minecraft.screen.ScreenHandler;
import org.graalvm.compiler.nodes.memory.MemoryCheckpoint;

public class DeathMenuChain extends TaskChain {

    // TODO: Implement settings and add auto-respawn to the list.
    private boolean shouldAutoRespawn(AltoClef mod) {
        return true;
    }
    private boolean shouldAutoReconnect(AltoClef mod) { return true; }

    public DeathMenuChain(TaskRunner runner) {
        super(runner);
    }

    private boolean _reconnecting = false;

    ServerInfo _prevServerEntry = null;

    private Timer _reconnectTimer = new Timer(1);

    private int _deathCount = 0;

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

        // Keep track of the last server we were on so we can re-connect.
        if (mod.inGame()) {
            _prevServerEntry = MinecraftClient.getInstance().getCurrentServerEntry();
        }

        if (screen instanceof DeathScreen && shouldAutoRespawn(mod)) {
            _deathCount++;
            Debug.logMessage("RESPAWNING... (this is death #" + _deathCount + ")");
            assert MinecraftClient.getInstance().player != null;
            MinecraftClient.getInstance().player.requestRespawn();
            MinecraftClient.getInstance().openScreen(null);
        } else if (screen instanceof DisconnectedScreen && shouldAutoReconnect(mod)) {
            Debug.logMessage("RECONNECTING: Going to Multiplayer Screen");
            _reconnecting = true;
            MinecraftClient.getInstance().openScreen(new MultiplayerScreen(new TitleScreen()));
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
        return 0;
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

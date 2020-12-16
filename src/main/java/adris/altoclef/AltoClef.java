package adris.altoclef;

import adris.altoclef.commands.AltoClefCommands;
import adris.altoclef.commands.CommandExecutor;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.tasksystem.UserTaskChain;
import adris.altoclef.trackers.EntityTracker;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.trackers.TrackerManager;
import adris.altoclef.util.ConfigState;
import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.event.events.ChatEvent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;

public class AltoClef implements ModInitializer {

    // Singleton pattern.
    // PLEASE AVOID RELYING ON THIS! The only place
    // where this is used is mixins. Later this will be removed entirely
    // once I figure out how to initialize mixins non-statically.
    private static AltoClef _instance;
    public static AltoClef getInstance() {
        return _instance;
    }

    // Central Managers
    private CommandExecutor _commandExecutor;
    private TaskRunner _taskRunner;
    private TrackerManager _trackerManager;
    private ConfigState _configState;

    // Task chains
    private UserTaskChain _userTaskChain;

    // Trackers
    private InventoryTracker _inventoryTracker;
    private EntityTracker _entityTracker;

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // As such, nothing will be loaded here.
        _instance = this;
    }

    public void onInitializeLoad() {
        // This code should be run after Minecraft loads everything else in.
        // This is the actual start point, controlled by a mixin.

        // Central Managers
        _commandExecutor = new CommandExecutor(this, "@");
        _taskRunner = new TaskRunner(this);
        _trackerManager = new TrackerManager(this);
        _configState = new ConfigState(this);

        // Task chains
        _userTaskChain = new UserTaskChain(_taskRunner);

        // Trackers
        _inventoryTracker = new InventoryTracker(_trackerManager);
        _entityTracker = new EntityTracker(_trackerManager);

        initializeCommands();
    }

    // Every chat message can be interrupted by us
    public void onChat(ChatEvent e) {
        String line = e.getMessage();
        if (_commandExecutor.isClientCommand(line)) {
            e.cancel();
            try {
                _commandExecutor.Execute(line);
            } catch (Exception ex) {
                Debug.logWarning(ex.getMessage());
                //ex.printStackTrace();
            }
        }
    }

    // Client tick
    public void onClientTick() {
        _trackerManager.tick();
        _taskRunner.tick();
    }

    private void initializeCommands() {
        try {
            // This creates the commands. If you want any more commands feel free to initialize new command lists.
            new AltoClefCommands(_commandExecutor);
        } catch (Exception e) {
            /// ppppbbbbttt
            e.printStackTrace();
        }
    }

    /// GETTERS AND SETTERS

    public CommandExecutor getCommandExecutor() {
        return _commandExecutor;
    }
    public TaskRunner getTaskRunner() {
        return _taskRunner;
    }
    public UserTaskChain getUserTaskChain() { return _userTaskChain; }
    public ConfigState getConfigState() { return _configState; }

    public InventoryTracker getInventoryTracker() { return _inventoryTracker; }
    public EntityTracker getEntityTracker() { return _entityTracker; }

    public Baritone getClientBaritone() {
        if (getPlayer() == null) {
            return null;
        }
        return (Baritone) BaritoneAPI.getProvider().getBaritoneForPlayer(getPlayer());
    }
    public Settings getClientBaritoneSettings() {
        return Baritone.settings();
    }

    public ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }
    public ClientPlayerInteractionManager getController() { return MinecraftClient.getInstance().interactionManager; }

    public void runUserTask(Task task) {
        _userTaskChain.runTask(this, task);
    }
}

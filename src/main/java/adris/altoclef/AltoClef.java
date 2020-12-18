package adris.altoclef;

import adris.altoclef.commands.AltoClefCommands;
import adris.altoclef.commands.CommandException;
import adris.altoclef.commands.CommandExecutor;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.tasksystem.UserTaskChain;
import adris.altoclef.trackers.BlockTracker;
import adris.altoclef.trackers.EntityTracker;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.trackers.TrackerManager;
import adris.altoclef.util.ConfigState;
import adris.altoclef.util.PlayerExtraController;
import adris.altoclef.util.baritone.BaritoneCustom;
import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.event.events.ChatEvent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;

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
    private BaritoneCustom _baritoneCustom;
    private PlayerExtraController _extraController;

    // Task chains
    private UserTaskChain _userTaskChain;

    // Trackers
    private InventoryTracker _inventoryTracker;
    private EntityTracker _entityTracker;
    private BlockTracker _blockTracker;

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
        _baritoneCustom = new BaritoneCustom(this, (Baritone)BaritoneAPI.getProvider().getPrimaryBaritone());
        _extraController = new PlayerExtraController(this);

        // Task chains
        _userTaskChain = new UserTaskChain(_taskRunner);

        // Trackers
        _inventoryTracker = new InventoryTracker(_trackerManager);
        _entityTracker = new EntityTracker(_trackerManager);
        _blockTracker = new BlockTracker(_trackerManager);

        initializeCommands();
    }

    // Every chat message can be interrupted by us
    public void onChat(ChatEvent e) {
        String line = e.getMessage();
        if (_commandExecutor.isClientCommand(line)) {
            e.cancel();
            try {
                _commandExecutor.Execute(line);
            } catch (CommandException ex) {
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

    // Block tracking
    public void onBlockAdd(BlockState newBlock, BlockPos pos) {
        //Debug.logMessage("NEW BLOCK: " + newBlock.getBlock().getTranslationKey());
        _blockTracker.onBlockPlace(pos, newBlock);
    }
    public void onBlockRemove(BlockState oldBlock, BlockPos pos) {
        //Debug.logMessage("POOF BLOCK: " + newBlock.getBlock().getTranslationKey());
        _blockTracker.onBlockRemove(pos, oldBlock);
    }
    public void onBlockChange(BlockState oldBlock, BlockState newBlock, BlockPos pos) {
        //Debug.logMessage("CHANGE BLOCK: " + oldBlock.getBlock().getTranslationKey() + " -> " + newBlock.getBlock().getTranslationKey());
        _blockTracker.onBlockRemove(pos, oldBlock);
        _blockTracker.onBlockPlace(pos, newBlock);
    }
    public void onBlockBreaking(BlockPos pos, double progress) {
        _extraController.onBlockBreak(pos, progress);
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

    // Main handlers access
    public CommandExecutor getCommandExecutor() {
        return _commandExecutor;
    }
    public TaskRunner getTaskRunner() {
        return _taskRunner;
    }
    //public UserTaskChain getUserTaskChain() { return _userTaskChain; }
    public ConfigState getConfigState() { return _configState; }
    public BaritoneCustom getCustomBaritone() {return _baritoneCustom; }

    // Trackers access
    public InventoryTracker getInventoryTracker() { return _inventoryTracker; }
    public EntityTracker getEntityTracker() { return _entityTracker; }
    public BlockTracker getBlockTracker() { return _blockTracker; }

    // Baritone access
    public Baritone getClientBaritone() {
        if (getPlayer() == null) {
            return null;
        }
        return (Baritone) BaritoneAPI.getProvider().getBaritoneForPlayer(getPlayer());
    }
    public Settings getClientBaritoneSettings() {
        return Baritone.settings();
    }

    // Minecraft access
    public ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }
    public ClientPlayerInteractionManager getController() { return MinecraftClient.getInstance().interactionManager; }
    public PlayerExtraController getControllerExtras() {return _extraController; }
    // Extra control
    public void runUserTask(Task task) {
        _userTaskChain.runTask(this, task);
    }
}

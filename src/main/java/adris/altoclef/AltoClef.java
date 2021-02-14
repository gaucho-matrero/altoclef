package adris.altoclef;

import adris.altoclef.commands.CommandExecutor;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.tasksystem.chains.DeathMenuChain;
import adris.altoclef.tasksystem.chains.FoodChain;
import adris.altoclef.tasksystem.chains.MobDefenseChain;
import adris.altoclef.tasksystem.chains.UserTaskChain;
import adris.altoclef.trackers.*;
import adris.altoclef.ui.CommandStatusOverlay;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.PlayerExtraController;
import adris.altoclef.util.baritone.BaritoneCustom;
import adris.altoclef.util.csharpisbetter.Action;
import baritone.Baritone;
import baritone.altoclef.AltoClefSettings;
import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AltoClef implements ModInitializer {

    // Central Managers
    private CommandExecutor _commandExecutor;
    private TaskRunner _taskRunner;
    private TrackerManager _trackerManager;
    private ConfigState _configState;
    private BaritoneCustom _baritoneCustom;
    private PlayerExtraController _extraController;

    // Task chains
    private UserTaskChain _userTaskChain;
    private FoodChain _foodChain;
    private MobDefenseChain _mobDefenseChain;

    // Trackers
    private InventoryTracker _inventoryTracker;
    private EntityTracker _entityTracker;
    private BlockTracker _blockTracker;
    private ContainerTracker _containerTracker;
    private SimpleChunkTracker _chunkTracker;

    // Renderers
    private CommandStatusOverlay _commandStatusOverlay;

    // Settings
    private adris.altoclef.Settings _settings;

    // I forget why this is here somebody help
    private final Action<WorldChunk> _onChunkLoad = new Action<>();

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // As such, nothing will be loaded here but basic initialization.
        StaticMixinHookups.hookupMod(this);
    }

    public void onInitializeLoad() {
        // This code should be run after Minecraft loads everything else in.
        // This is the actual start point, controlled by a mixin.

        initializeBaritoneSettings();

        // Load settings
        _settings = adris.altoclef.Settings.load();

        // Central Managers
        _commandExecutor = new CommandExecutor(this, "@");
        _taskRunner = new TaskRunner(this);
        _trackerManager = new TrackerManager(this);
        _configState = new ConfigState(this);
        _baritoneCustom = new BaritoneCustom(this, (Baritone)BaritoneAPI.getProvider().getPrimaryBaritone());
        _extraController = new PlayerExtraController(this);

        // Task chains
        _userTaskChain = new UserTaskChain(_taskRunner);
        _mobDefenseChain = new MobDefenseChain(_taskRunner);
        new DeathMenuChain(_taskRunner);
        _foodChain = new FoodChain(_taskRunner);

        // Trackers
        _inventoryTracker = new InventoryTracker(_trackerManager);
        _entityTracker = new EntityTracker(_trackerManager);
        _blockTracker = new BlockTracker(_trackerManager);
        _containerTracker = new ContainerTracker(this, _trackerManager);
        _chunkTracker = new SimpleChunkTracker(this);

        // Renderers
        _commandStatusOverlay = new CommandStatusOverlay(_taskRunner);

        initializeCommands();

    }

    // Client tick
    public void onClientTick() {
        // TODO: should this go here?
        _containerTracker.onServerTick();

        _trackerManager.tick();
        _taskRunner.tick();
    }

    public void onClientRenderOverlay(MatrixStack matrixStack) {
        _commandStatusOverlay.render(matrixStack);
    }

    public void onChunkLoad(WorldChunk chunk) {
        _chunkTracker.onLoad(chunk.getPos());
        _onChunkLoad.invoke(chunk);
    }
    public void onChunkUnload(ChunkPos chunkPos) {
        _chunkTracker.onUnload(chunkPos);
    }

    private void initializeBaritoneSettings() {
        // Let baritone move items to hotbar to use them
        getClientBaritoneSettings().allowInventory.value = true;
        // Pretty safe, minor risk.
        getClientBaritoneSettings().allowDiagonalAscend.value = true;
        // Reduces a bit of far rendering to save FPS
        getClientBaritoneSettings().fadePath.value = true;
        // Don't let baritone scan dropped items, we handle that ourselves.
        getClientBaritoneSettings().mineScanDroppedItems.value = false;
        // Don't let baritone wait for drops, we handle that ourselves.
        getClientBaritoneSettings().mineDropLoiterDurationMSThanksLouca.value = 0L;
    }

    // List all command sources here.
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
    public ContainerTracker getContainerTracker() {return _containerTracker;}
    public SimpleChunkTracker getChunkTracker() {return _chunkTracker;}

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

    public AltoClefSettings getExtraBaritoneSettings() {
        return Baritone.getAltoClefSettings();
    }

    public adris.altoclef.Settings getModSettings() {return _settings; }

    public adris.altoclef.Settings reloadModSettings() {
        adris.altoclef.Settings result = adris.altoclef.Settings.load();
        if (result != null) {
            _settings = result;
        }
        return result;
    }

    public int getTicks() {
        try {
            ClientConnection con = Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).getConnection();
            Field tickField = ClientConnection.class.getDeclaredField("ticks");
            tickField.setAccessible(true);
            return tickField.getInt(con);
        } catch (NoSuchFieldException | NullPointerException | IllegalAccessException e) {
            return 0;
        }
    }

    // Minecraft access
    public ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }
    public ClientWorld getWorld() {return MinecraftClient.getInstance().world; }
    public ClientPlayerInteractionManager getController() { return MinecraftClient.getInstance().interactionManager; }
    public PlayerExtraController getControllerExtras() {return _extraController; }


    // Extra control
    public void runUserTask(Task task) {
        _userTaskChain.runTask(this, task);
    }
    public void cancelUserTask() {_userTaskChain.cancel(this);}
    public FoodChain getFoodChain() {
        return _foodChain;
    }
    public MobDefenseChain getMobDefenseChain() {
        return _mobDefenseChain;
    }

    // Are we in game (playing in a server/world)
    public boolean inGame() {
        return getPlayer() != null;
    }

    public Dimension getCurrentDimension() {
        if (getWorld().getDimension().isUltrawarm()) return Dimension.NETHER;
        if (getWorld().getDimension().isNatural()) return Dimension.OVERWORLD;
        return Dimension.END;
    }

    public Action<WorldChunk> getOnChunkLoad() {
        return _onChunkLoad;
    }

}

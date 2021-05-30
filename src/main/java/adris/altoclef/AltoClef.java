package adris.altoclef;

import adris.altoclef.butler.Butler;
import adris.altoclef.mixins.ClientConnectionAccessor;
import adris.altoclef.tasks.misc.IdleTask;
import adris.altoclef.ui.MessagePriority;
import adris.altoclef.commandsystem.CommandExecutor;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.tasksystem.chains.*;
import adris.altoclef.trackers.*;
import adris.altoclef.ui.CommandStatusOverlay;
import adris.altoclef.ui.MessageSender;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.PlayerExtraController;
import adris.altoclef.util.baritone.BaritoneCustom;
import adris.altoclef.util.csharpisbetter.Action;
import adris.altoclef.util.csharpisbetter.ActionListener;
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

import java.util.Objects;
import java.util.function.Consumer;

public class AltoClef implements ModInitializer {

    // Central Managers
    private CommandExecutor _commandExecutor;
    private TaskRunner _taskRunner;
    private TrackerManager _trackerManager;
    private BotBehaviour _botBehaviour;
    private BaritoneCustom _baritoneCustom;
    private PlayerExtraController _extraController;

    // Task chains
    private UserTaskChain _userTaskChain;
    private FoodChain _foodChain;
    private MobDefenseChain _mobDefenseChain;
    private MLGBucketFallChain _mlgBucketChain;

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

    // Misc managers
    private MessageSender _messageSender;

    // Butler
    private Butler _butler;

    // I forget why this is here somebody help
    private final Action<WorldChunk> _onChunkLoad = new Action<>();

    public final Action<String> onGameMessage = new Action<>();
    public final Action<String> onGameOverlayMessage = new Action<>();

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
        _botBehaviour = new BotBehaviour(this);
        _baritoneCustom = new BaritoneCustom(this, (Baritone)BaritoneAPI.getProvider().getPrimaryBaritone());
        _extraController = new PlayerExtraController(this);

        // Task chains
        _userTaskChain = new UserTaskChain(_taskRunner);
        _mobDefenseChain = new MobDefenseChain(_taskRunner);
        new DeathMenuChain(_taskRunner);
        new PlayerInteractionFixChain(_taskRunner);
        _mlgBucketChain = new MLGBucketFallChain(_taskRunner);
        new WorldSurvivalChain(_taskRunner);
        _foodChain = new FoodChain(_taskRunner);

        // Trackers
        _inventoryTracker = new InventoryTracker(_trackerManager);
        _entityTracker = new EntityTracker(_trackerManager);
        _blockTracker = new BlockTracker(this, _trackerManager);
        _containerTracker = new ContainerTracker(this, _trackerManager);
        _chunkTracker = new SimpleChunkTracker(this);

        // Renderers
        _commandStatusOverlay = new CommandStatusOverlay();

        // Misc managers
        _messageSender = new MessageSender();

        _butler = new Butler(this);

        // Misc wiring
        // When we place a block and might be tracking it, make the change immediate.
        _extraController.onBlockPlaced.addListener(new ActionListener<PlayerExtraController.BlockPlaceEvent>() {
            @Override
            public void invoke(PlayerExtraController.BlockPlaceEvent value) {
                _blockTracker.addBlock(value.blockState.getBlock(), value.blockPos);
            }
        });


        initializeCommands();

        AltoClefCommands.IDLE_TEST_INIT_FUNCTION(this);
    }

    // Client tick
    public void onClientTick() {
        // TODO: should this go here?
        _containerTracker.onServerTick();

        _trackerManager.tick();
        _taskRunner.tick();

        _butler.tick();
        _messageSender.tick();
    }

    public void onClientRenderOverlay(MatrixStack matrixStack) {
        _commandStatusOverlay.render(this, matrixStack);
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
        // Pretty safe, minor risk EXCEPT in the nether, where it is a huge risk.
        getClientBaritoneSettings().allowDiagonalAscend.value = true;
        // Reduces a bit of far rendering to save FPS
        getClientBaritoneSettings().fadePath.value = true;
        // Don't let baritone scan dropped items, we handle that ourselves.
        getClientBaritoneSettings().mineScanDroppedItems.value = false;
        // Don't let baritone wait for drops, we handle that ourselves.
        getClientBaritoneSettings().mineDropLoiterDurationMSThanksLouca.value = 0L;

        // Really avoid mobs if we're in danger.
        getClientBaritoneSettings().mobAvoidanceCoefficient.value = 2.0;
        getClientBaritoneSettings().mobAvoidanceRadius.value = 12;

        // Don't break blocks or place blocks where we are explicitly protected.
        getExtraBaritoneSettings().avoidBlockBreak(blockPos -> _settings.isPositionExplicitelyProtected(blockPos));
        getExtraBaritoneSettings().avoidBlockPlace(blockPos -> _settings.isPositionExplicitelyProtected(blockPos));

        // Water bucket placement will be handled by us exclusively
        getExtraBaritoneSettings().configurePlaceBucketButDontFall(true);

        // By default don't use shears.
        getExtraBaritoneSettings().allowShears(false);

        // Give baritone more time to calculate paths. Sometimes they can be really far away.
        // Was: 2000L
        getClientBaritoneSettings().failureTimeoutMS.value = 6000L;
        // Was: 5000L
        getClientBaritoneSettings().planAheadFailureTimeoutMS.value = 10000L;
        // Was 100
        getClientBaritoneSettings().movementTimeoutTicks.value = 200;
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
    public UserTaskChain getUserTaskChain() { return _userTaskChain; }
    public BotBehaviour getBehaviour() { return _botBehaviour; }
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
        // If we weren't running anything and are now "idling", idle.
        if (getModSettings().shouldIdleWhenNotActive()) {
            runUserTask(new IdleTask());
        }

        return result;
    }

    public Butler getButler() {
        return _butler;
    }

    public MessageSender getMessageSender() {return _messageSender;}

    public int getTicks() {
        ClientConnection con = Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).getConnection();
        return ((ClientConnectionAccessor)con).getTicks();
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
        runUserTask(task, (nothing) -> {});
    }
    @SuppressWarnings("rawtypes")
    public void runUserTask(Task task, Consumer onFinish) {
        _userTaskChain.runTask(this, task, onFinish);
    }
    public void cancelUserTask() {_userTaskChain.cancel(this);}

    // Chains
    public FoodChain getFoodChain() {
        return _foodChain;
    }
    public MobDefenseChain getMobDefenseChain() {
        return _mobDefenseChain;
    }
    public MLGBucketFallChain getMLGBucketChain() {return _mlgBucketChain;}


    // Are we in game (playing in a server/world)
    public boolean inGame() {
        return getPlayer() != null;
    }

    public Dimension getCurrentDimension() {
        if (!inGame()) return Dimension.OVERWORLD;
        if (getWorld().getDimension().isUltrawarm()) return Dimension.NETHER;
        if (getWorld().getDimension().isNatural()) return Dimension.OVERWORLD;
        return Dimension.END;
    }

    public void log(String message) {
        log(message, MessagePriority.TIMELY);
    }
    public void log(String message, MessagePriority priority) {
        Debug.logMessage(message);
        _butler.onLog(message, priority);
    }
    public void logWarning(String message) {
        logWarning(message, MessagePriority.TIMELY);
    }
    public void logWarning(String message, MessagePriority priority) {
        Debug.logWarning(message);
        _butler.onLogWarning(message, priority);
    }

    public Action<WorldChunk> getOnChunkLoad() {
        return _onChunkLoad;
    }

}

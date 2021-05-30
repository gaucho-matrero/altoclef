package adris.altoclef;


import adris.altoclef.butler.Butler;
import adris.altoclef.commandsystem.CommandExecutor;
import adris.altoclef.mixins.ClientConnectionAccessor;
import adris.altoclef.tasks.misc.IdleTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.tasksystem.chains.DeathMenuChain;
import adris.altoclef.tasksystem.chains.FoodChain;
import adris.altoclef.tasksystem.chains.MLGBucketFallChain;
import adris.altoclef.tasksystem.chains.MobDefenseChain;
import adris.altoclef.tasksystem.chains.PlayerInteractionFixChain;
import adris.altoclef.tasksystem.chains.UserTaskChain;
import adris.altoclef.tasksystem.chains.WorldSurvivalChain;
import adris.altoclef.trackers.BlockTracker;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.trackers.EntityTracker;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.trackers.SimpleChunkTracker;
import adris.altoclef.trackers.TrackerManager;
import adris.altoclef.ui.CommandStatusOverlay;
import adris.altoclef.ui.MessagePriority;
import adris.altoclef.ui.MessageSender;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.PlayerExtraController;
import adris.altoclef.util.baritone.BaritoneCustom;
import adris.altoclef.util.csharpisbetter.Action;
import adris.altoclef.util.csharpisbetter.ActionListener;
import baritone.Baritone;
import baritone.altoclef.AltoClefSettings;
import baritone.api.BaritoneAPI;
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

    public final Action<String> onGameMessage = new Action<>();
    public final Action<String> onGameOverlayMessage = new Action<>();
    // I forget why this is here somebody help
    private final Action<WorldChunk> onChunkLoad = new Action<>();
    // Central Managers
    private CommandExecutor commandExecutor;
    private TaskRunner taskRunner;
    private TrackerManager trackerManager;
    private ConfigState configState;
    private BaritoneCustom baritoneCustom;
    private PlayerExtraController playerExtraController;
    // Task chains
    private UserTaskChain userTaskChain;
    private FoodChain foodTaskChain;
    private MobDefenseChain mobDefenseTaskChain;
    private MLGBucketFallChain mlgBucketTaskChain;
    // Trackers
    private InventoryTracker inventoryTracker;
    private EntityTracker entityTracker;
    private BlockTracker blockTracker;
    private ContainerTracker containerTracker;
    private SimpleChunkTracker chunkTracker;
    // Renderers
    private CommandStatusOverlay commandStatusOverlay;
    // Settings
    private Settings altoClefSettings;
    // Misc managers
    private MessageSender messageSender;
    // Butler
    private Butler butler;

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
        altoClefSettings = Settings.load();

        // Central Managers
        commandExecutor = new CommandExecutor(this, "@");
        taskRunner = new TaskRunner(this);
        trackerManager = new TrackerManager(this);
        configState = new ConfigState(this);
        baritoneCustom = new BaritoneCustom(this, (Baritone) BaritoneAPI.getProvider().getPrimaryBaritone());
        playerExtraController = new PlayerExtraController(this);

        // Task chains
        userTaskChain = new UserTaskChain(taskRunner);
        mobDefenseTaskChain = new MobDefenseChain(taskRunner);
        new DeathMenuChain(taskRunner);
        new PlayerInteractionFixChain(taskRunner);
        mlgBucketTaskChain = new MLGBucketFallChain(taskRunner);
        new WorldSurvivalChain(taskRunner);
        foodTaskChain = new FoodChain(taskRunner);

        // Trackers
        inventoryTracker = new InventoryTracker(trackerManager);
        entityTracker = new EntityTracker(trackerManager);
        blockTracker = new BlockTracker(this, trackerManager);
        containerTracker = new ContainerTracker(this, trackerManager);
        chunkTracker = new SimpleChunkTracker(this);

        // Renderers
        commandStatusOverlay = new CommandStatusOverlay();

        // Misc managers
        messageSender = new MessageSender();

        butler = new Butler(this);

        // Misc wiring
        // When we place a block and might be tracking it, make the change immediate.
        playerExtraController.onBlockPlaced.addListener(new ActionListener<PlayerExtraController.BlockPlaceEvent>() {
            @Override
            public void invoke(PlayerExtraController.BlockPlaceEvent value) {
                blockTracker.addBlock(value.blockState.getBlock(), value.blockPos);
            }
        });


        initializeCommands();

        AltoClefCommands.IDLE_TEST_INIT_FUNCTION(this);
    }

    // Client tick
    public void onClientTick() {
        // TODO: should this go here?
        containerTracker.onServerTick();

        trackerManager.tick();
        taskRunner.tick();

        butler.tick();
        messageSender.tick();
    }

    public void onClientRenderOverlay(MatrixStack matrixStack) {
        commandStatusOverlay.render(this, matrixStack);
    }

    public void onChunkLoad(WorldChunk chunk) {
        chunkTracker.onLoad(chunk.getPos());
        onChunkLoad.invoke(chunk);
    }

    public void onChunkUnload(ChunkPos chunkPos) {
        chunkTracker.onUnload(chunkPos);
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
        getExtraBaritoneSettings().avoidBlockBreak(blockPos -> altoClefSettings.isPositionExplicitelyProtected(blockPos));
        getExtraBaritoneSettings().avoidBlockPlace(blockPos -> altoClefSettings.isPositionExplicitelyProtected(blockPos));

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
            new AltoClefCommands(commandExecutor);
        } catch (Exception e) {
            /// ppppbbbbttt
            e.printStackTrace();
        }
    }

    /// GETTERS AND SETTERS

    // Main handlers access
    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public TaskRunner getTaskRunner() {
        return taskRunner;
    }

    public UserTaskChain getUserTaskChain() {
        return userTaskChain;
    }

    public ConfigState getConfigState() {
        return configState;
    }

    public BaritoneCustom getCustomBaritone() {
        return baritoneCustom;
    }

    // Trackers access
    public InventoryTracker getInventoryTracker() {
        return inventoryTracker;
    }

    public EntityTracker getEntityTracker() {
        return entityTracker;
    }

    public BlockTracker getBlockTracker() {
        return blockTracker;
    }

    public ContainerTracker getContainerTracker() {
        return containerTracker;
    }

    public SimpleChunkTracker getChunkTracker() {
        return chunkTracker;
    }

    // Baritone access
    public Baritone getClientBaritone() {
        if (getPlayer() == null) {
            return null;
        }
        return (Baritone) BaritoneAPI.getProvider().getBaritoneForPlayer(getPlayer());
    }

    public baritone.api.Settings getClientBaritoneSettings() {
        return Baritone.settings();
    }

    public AltoClefSettings getExtraBaritoneSettings() {
        return Baritone.getAltoClefSettings();
    }

    public Settings getModSettings() {
        return altoClefSettings;
    }

    public Settings reloadModSettings() {
        Settings result = Settings.load();
        if (result != null) {
            altoClefSettings = result;
        }
        // If we weren't running anything and are now "idling", idle.
        if (altoClefSettings.shouldIdleWhenNotActive()) {
            runUserTask(new IdleTask());
        }

        return result;
    }

    public Butler getButler() {
        return butler;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    @SuppressWarnings("CastToIncompatibleInterface")
    public int getTicks() {
        ClientConnection con = Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).getConnection();
        return ((ClientConnectionAccessor) con).getTicks();
    }

    // Minecraft access
    public ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }

    public ClientWorld getWorld() {
        return MinecraftClient.getInstance().world;
    }

    public ClientPlayerInteractionManager getController() {
        return MinecraftClient.getInstance().interactionManager;
    }

    public PlayerExtraController getControllerExtras() {
        return playerExtraController;
    }


    // Extra control
    public void runUserTask(Task task) {
        runUserTask(task, (nothing) -> {
        });
    }

    @SuppressWarnings("rawtypes")
    public void runUserTask(Task task, Consumer onFinish) {
        userTaskChain.runTask(this, task, onFinish);
    }

    public void cancelUserTask() {
        userTaskChain.cancel(this);
    }

    // Chains
    public FoodChain getFoodTaskChain() {
        return foodTaskChain;
    }

    public MobDefenseChain getMobDefenseChain() {
        return mobDefenseTaskChain;
    }

    public MLGBucketFallChain getMLGBucketChain() {
        return mlgBucketTaskChain;
    }


    // Are we in game (playing in a server/world)
    public boolean inGame() {
        return getPlayer() != null;
    }

    public Dimension getCurrentDimension() {
        if (!inGame())
            return Dimension.OVERWORLD;
        if (getWorld().getDimension().isUltrawarm())
            return Dimension.NETHER;
        if (getWorld().getDimension().isNatural())
            return Dimension.OVERWORLD;
        return Dimension.END;
    }

    public void log(String message) {
        log(message, MessagePriority.TIMELY);
    }

    public void log(String message, MessagePriority priority) {
        Debug.logMessage(message);
        butler.onLog(message, priority);
    }

    public void logWarning(String message) {
        logWarning(message, MessagePriority.TIMELY);
    }

    public void logWarning(String message, MessagePriority priority) {
        Debug.logWarning(message);
        butler.onLogWarning(message, priority);
    }

    public Action<WorldChunk> getOnChunkLoad() {
        return onChunkLoad;
    }

}

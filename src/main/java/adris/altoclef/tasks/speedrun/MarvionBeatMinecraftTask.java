package adris.altoclef.tasks.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.container.DoStuffInContainerTask;
import adris.altoclef.tasks.container.LootContainerTask;
import adris.altoclef.tasks.container.SmeltInSmokerTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.misc.LootDesertTempleTask;
import adris.altoclef.tasks.misc.PlaceBedAndSetSpawnTask;
import adris.altoclef.tasks.misc.SleepThroughNightTask;
import adris.altoclef.tasks.movement.*;
import adris.altoclef.tasks.resources.*;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.BlockTracker;
import adris.altoclef.trackers.EntityTracker;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.*;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static net.minecraft.client.MinecraftClient.getInstance;

@SuppressWarnings("ALL")
public class MarvionBeatMinecraftTask extends Task {
    private static final Block[] TRACK_BLOCKS = new Block[]{
            Blocks.BLAST_FURNACE,
            Blocks.FURNACE,
            Blocks.SMOKER,
            Blocks.END_PORTAL_FRAME,
            Blocks.END_PORTAL,
            Blocks.CRAFTING_TABLE, // For pearl trading + gold crafting
            Blocks.CHEST, // For ruined portals
            Blocks.SPAWNER, // For silverfish,
            Blocks.STONE_PRESSURE_PLATE // For desert temples
    };
    private static final Item[] COLLECT_EYE_ARMOR = new Item[]{
            Items.GOLDEN_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS
    };
    private static final ItemTarget[] COLLECT_STONE_GEAR = combine(
            toItemTargets(Items.STONE_SWORD, 1),
            toItemTargets(Items.STONE_PICKAXE, 2),
            toItemTargets(Items.STONE_HOE),
            toItemTargets(Items.COAL, 13)
    );
    private static final Item COLLECT_SHIELD = Items.SHIELD;
    private static final Item[] COLLECT_IRON_ARMOR = ItemHelper.IRON_ARMORS;
    private static final Item[] COLLECT_EYE_ARMOR_END = ItemHelper.DIAMOND_ARMORS;
    private static final ItemTarget[] COLLECT_IRON_GEAR = combine(
            toItemTargets(Items.IRON_SWORD, 2),
            toItemTargets(Items.STONE_SHOVEL),
            toItemTargets(Items.STONE_AXE),
            toItemTargets(Items.DIAMOND_PICKAXE)
    );
    private static final ItemTarget[] COLLECT_EYE_GEAR = combine(
            toItemTargets(Items.DIAMOND_SWORD),
            toItemTargets(Items.DIAMOND_PICKAXE, 3),
            toItemTargets(Items.BUCKET, 2),
            toItemTargets(Items.CRAFTING_TABLE)
    );
    private static final ItemTarget[] COLLECT_IRON_GEAR_MIN = combine(
            toItemTargets(Items.IRON_SWORD),
            toItemTargets(Items.DIAMOND_PICKAXE)
    );
    private static final ItemTarget[] COLLECT_EYE_GEAR_MIN = combine(
            toItemTargets(Items.DIAMOND_SWORD),
            toItemTargets(Items.DIAMOND_PICKAXE)
    );
    private static final ItemTarget[] IRON_GEAR = combine(
            toItemTargets(Items.IRON_SWORD, 2),
            toItemTargets(Items.STONE_SHOVEL),
            toItemTargets(Items.STONE_AXE),
            toItemTargets(Items.DIAMOND_PICKAXE),
            toItemTargets(Items.SHIELD)
    );
    private static final ItemTarget[] IRON_GEAR_MIN = combine(
            toItemTargets(Items.IRON_SWORD, 2),
            toItemTargets(Items.DIAMOND_PICKAXE),
            toItemTargets(Items.SHIELD)
    );
    private static final int END_PORTAL_FRAME_COUNT = 12;
    private static final double END_PORTAL_BED_SPAWN_RANGE = 8;

    private static final int TWISTING_VINES_COUNT = 28;
    private static final int TWISTING_VINES_COUNT_MIN = 14;
    // We don't want curse of binding
    private static final Predicate<ItemStack> _noCurseOfBinding = stack -> {
        for (NbtElement elm : stack.getEnchantments()) {
            NbtCompound comp = (NbtCompound) elm;
            if (comp.getString("id").equals("minecraft:binding_curse")) {
                return false;
            }
        }
        return true;
    };
    private static BeatMinecraftConfig _config;
    private static GoToStrongholdPortalTask _locateStrongholdTask;
    private static boolean openingEndPortal = false;

    static {
        ConfigHelper.loadConfig("configs/beat_minecraft.json", BeatMinecraftConfig::new, BeatMinecraftConfig.class, newConfig -> _config = newConfig);
    }

    private final HashMap<Item, Integer> _cachedEndItemDrops = new HashMap<>();
    // For some reason, after death there's a frame where the game thinks there are NO items in the end.
    private final TimerGame _cachedEndItemNothingWaitTime = new TimerGame(10);
    private final Task _buildMaterialsTask;
    private final PlaceBedAndSetSpawnTask _setBedSpawnTask = new PlaceBedAndSetSpawnTask();
    private final Task _goToNetherTask = new DefaultGoToDimensionTask(Dimension.NETHER); // To keep the portal build cache.
    private final Task _getOneBedTask = TaskCatalogue.getItemTask("bed", 1);
    private final Task _sleepThroughNightTask = new SleepThroughNightTask();
    private final Task _killDragonBedStratsTask = new KillEnderDragonWithBedsTask(new WaitForDragonAndPearlTask());
    // End specific dragon breath avoidance
    private final DragonBreathTracker _dragonBreathTracker = new DragonBreathTracker();
    private final TimerGame _timer1 = new TimerGame(5);
    private final TimerGame _timer2 = new TimerGame(35);
    private final TimerGame _timer3 = new TimerGame(60);
    boolean _weHaveEyes;
    private static boolean dragonIsDead;
    private BlockPos _endPortalCenterLocation;
    private boolean _isEquippingDiamondArmor;
    private boolean _ranStrongholdLocator;
    private boolean _endPortalOpened;
    private BlockPos _bedSpawnLocation;
    private List<BlockPos> _notRuinedPortalChests = new ArrayList<>();
    private int _cachedFilledPortalFrames = 0;
    // Controls whether we CAN walk on the end portal.
    private static boolean enteringEndPortal;
    private Task _foodTask;
    private Task _gearTask;
    private Task _lootTask;
    private boolean _collectingEyes;
    private boolean _escapingDragonsBreath = false;
    private boolean isGettingBlazeRods = false;
    private boolean isGettingEnderPearls = false;
    private Task searchBiomeTask;
    private Task _getPorkchopTask;
    private Task _stoneGearTask;
    private Task _logsTask;
    private Task _starterGearTask;
    private Task _ironGearTask;
    private Task _shieldTask;
    private Task _smeltTask;
    private Task getBedTask;
    private Task getTwistingVines;

    public MarvionBeatMinecraftTask() {
        _locateStrongholdTask = new GoToStrongholdPortalTask(_config.targetEyes);
        _buildMaterialsTask = new GetBuildingMaterialsTask(_config.buildMaterialCount);
    }

    /**
     * Retrieves the BeatMinecraftConfig instance, creating a new one if it doesn't exist.
     * @return the BeatMinecraftConfig instance
     */
    public static BeatMinecraftConfig getConfig() {
        if (_config == null) {
            _config = new BeatMinecraftConfig();
        }
        return _config;
    }

    /**
     * Retrieves the frame blocks surrounding the end portal center.
     * @param endPortalCenter the center position of the end portal
     * @return the list of frame blocks
     */
    private static List<BlockPos> getFrameBlocks(BlockPos endPortalCenter) {
        // Create a list to store the frame blocks
        List<BlockPos> frameBlocks = new ArrayList<>();

        // Check if the end portal center is not null
        if (endPortalCenter != null) {
            // Define the offsets for the frame blocks
            int[][] frameOffsets = {
                    {2, 0, 1},
                    {2, 0, 0},
                    {2, 0, -1},
                    {-2, 0, 1},
                    {-2, 0, 0},
                    {-2, 0, -1},
                    {1, 0, 2},
                    {0, 0, 2},
                    {-1, 0, 2},
                    {1, 0, -2},
                    {0, 0, -2},
                    {-1, 0, -2}
            };

            // Iterate over each offset
            for (int[] offset : frameOffsets) {
                // Calculate the frame block position by adding the offset to the end portal center
                BlockPos frameBlock = endPortalCenter.add(offset[0], offset[1], offset[2]);

                // Add the frame block to the list
                frameBlocks.add(frameBlock);
            }
        }

        // Return the list of frame blocks
        return frameBlocks;
    }

    /**
     * Converts an array of Item objects to an array of ItemTarget objects.
     *
     * @param items the array of Item objects to convert
     * @return an array of ItemTarget objects
     */
    private static ItemTarget[] toItemTargets(Item... items) {
        return Arrays.stream(items)
                .map(ItemTarget::new)
                .toArray(ItemTarget[]::new);
    }

    /**
     * Converts an Item and count into an array of ItemTargets.
     *
     * @param item The item to convert
     * @param count The count of the item
     * @return An array of ItemTargets
     */
    private static ItemTarget[] toItemTargets(Item item, int count) {
        // Create a new array of ItemTargets with a length of 1.
        ItemTarget[] itemTargets = {new ItemTarget(item, count)};

        // Return the array of ItemTargets.
        return itemTargets;
    }

    /**
     * Combines multiple arrays of ItemTarget objects into a single array.
     *
     * @param targets The arrays of ItemTarget objects to be combined
     * @return The combined array of ItemTarget objects
     */
    private static ItemTarget[] combine(ItemTarget[]... targets) {
        List<ItemTarget> combinedTargets = new ArrayList<>();

        // Iterate over each array of ItemTarget objects
        for (ItemTarget[] targetArray : targets) {
            if (targetArray != null) {
                // Add all elements of the array to the combinedTargets list
                combinedTargets.addAll(Arrays.asList(targetArray));
            }
        }

        // Convert the combinedTargets list to an array and log it
        ItemTarget[] combinedArray = combinedTargets.toArray(new ItemTarget[0]);

        // Return the combined array
        return combinedArray;
    }

    /**
     * Check if the end portal frame at the specified position is filled with an eye of ender.
     * @param mod the AltoClef mod
     * @param pos the position of the end portal frame
     * @return true if the end portal frame is filled with an eye of ender, false otherwise
     */
    private static boolean isEndPortalFrameFilled(AltoClef mod, BlockPos pos) {
        // Check if the chunk at the specified position is loaded
        if (!mod.getChunkTracker().isChunkLoaded(pos)) {
            return false;
        }

        // Get the block state at the specified position
        BlockState blockState = mod.getWorld().getBlockState(pos);

        // Check if the block at the specified position is an end portal frame
        if (blockState.getBlock() != Blocks.END_PORTAL_FRAME) {
            return false;
        }

        // Return true if the end portal frame is filled with an eye of ender, false otherwise
        return blockState.get(EndPortalFrameBlock.EYE);
    }

    /**
     * Check if the task should be forced for the given alto clef mod.
     * @param mod the alto clef mod
     * @param task the task to check
     * @return true if the task should be forced, false otherwise
     */
    private static boolean shouldForce(AltoClef mod, Task task) {
        // Task must not be null, must be active, and must not be finished for the given mod
        return task != null && task.isActive() && !task.isFinished(mod);
    }

    /**
     * Check if the condition for finishing the game is met.
     *
     * @param mod the AltoClef object
     * @return true if the game is finished, false otherwise
     */
    @Override
    public boolean isFinished(AltoClef mod) {
        // Check if the current screen is the CreditsScreen
        if (getInstance().currentScreen instanceof CreditsScreen) {
            return true;
        }

        // Check if the dragon is dead in the Overworld
        if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD && dragonIsDead) {
            return true;
        }
        return false;
    }

    /**
     * Check if the bot needs more building materials to start building.
     *
     * @param mod the AltoClef mod instance
     * @return true if the bot needs more building materials, false otherwise
     */
    private boolean needsBuildingMaterials(AltoClef mod) {
        // Get the count of building materials in storage
        int materialCount = StorageHelper.getBuildingMaterialCount(mod);

        // Check if the bot should force building or if the material count is below the minimum required
        boolean shouldForce = shouldForce(mod, _buildMaterialsTask);

        // Return true if the material count is below the minimum required or if the bot should force building
        return materialCount < _config.minBuildMaterialCount || shouldForce;
    }

    /**
     * Update the cached end items based on the dropped items from the entity tracker.
     *
     * @param mod The AltoClef mod
     */
    private void updateCachedEndItems(AltoClef mod) {
        // Get the list of dropped items from the entity tracker.
        List<ItemEntity> droppedItems = mod.getEntityTracker().getDroppedItems();

        // Reset the cache wait time and clear the cached end item drops.
        _cachedEndItemNothingWaitTime.reset();
        _cachedEndItemDrops.clear();

        // Iterate over the dropped items to update the cached end item drops.
        for (ItemEntity entity : droppedItems) {
            if (entity.getStack().isEmpty()) {
                continue;
            }
            Item item = entity.getStack().getItem();
            int count = entity.getStack().getCount();

            // Add the dropped item to the cached end item drops.
            _cachedEndItemDrops.put(item, _cachedEndItemDrops.getOrDefault(item, 0) + count);
        }
    }

    /**
     * Retrieves the count of a specific item from the cached end item drops.
     * If the item does not exist in the cache, it returns 0.
     *
     * @param item The item to retrieve the count for
     * @return The count of the specified item in the cached end item drops
     */
    private int getEndCachedCount(Item item) {
        // Check if the item exists in the cachedEndItemDrops map before retrieving the count
        if (_cachedEndItemDrops.containsKey(item)) {
            return _cachedEndItemDrops.get(item);
        } else {
            return 0; // Return 0 if the item is not found in the map
        }
    }

    /**
     * Checks if the given item has been dropped in the end.
     *
     * @param item The item to check
     * @return True if the item has been dropped in the end, false otherwise
     */
    private boolean droppedInEnd(Item item) {
        // Get the cached count from the end.
        Integer cachedCount = getEndCachedCount(item);

        return cachedCount != null && cachedCount > 0;
    }

    /**
     * Check if the item is present in the item storage or if it has been dropped in the end.
     *
     * @param mod the AltoClef object
     * @param item the Item object
     * @return true if the item is present in the item storage or if it has been dropped in the end, false otherwise
     * @throws IllegalArgumentException if mod or item is null
     */
    private boolean hasItemOrDroppedInEnd(AltoClef mod, Item item) {
        if (mod == null || item == null) {
            throw new IllegalArgumentException("mod and item must not be null");
        }

        // Check if the item is present in the item storage.
        boolean hasItem = mod.getItemStorage().hasItem(item);

        // Check if the item has been dropped in the end.
        boolean droppedInEnd = droppedInEnd(item);

        // Return true if the item is present in the item storage or if it has been dropped in the end.
        return hasItem || droppedInEnd;
    }

    /**
     * Generates a list of lootable items based on certain conditions.
     *
     * @param mod the AltoClef mod
     * @return a list of lootable items
     */
    private List<Item> lootableItems(AltoClef mod) {
        List<Item> lootable = new ArrayList<>();

        // Add initial lootable items
        lootable.addAll(Arrays.asList(
                Items.GOLDEN_APPLE,
                Items.ENCHANTED_GOLDEN_APPLE,
                Items.GLISTERING_MELON_SLICE,
                Items.GOLDEN_CARROT,
                Items.OBSIDIAN
        ));

        // Check if golden helmet is equipped or available in inventory
        boolean isGoldenHelmetEquipped = StorageHelper.isArmorEquipped(mod, Items.GOLDEN_HELMET);
        boolean hasGoldenHelmet = mod.getItemStorage().hasItemInventoryOnly(Items.GOLDEN_HELMET);

        // Check if there are enough gold ingots
        boolean hasEnoughGoldIngots = mod.getItemStorage().getItemCountInventoryOnly(Items.GOLD_INGOT) >= 5;

        // Add golden helmet if not equipped or available in inventory
        if (!isGoldenHelmetEquipped && !hasGoldenHelmet) {
            lootable.add(Items.GOLDEN_HELMET);
        }

        // Add gold ingot if enough gold ingots are available or if barterPearlsInsteadOfEndermanHunt is true
        if ((hasEnoughGoldIngots && !isGoldenHelmetEquipped && !hasGoldenHelmet) || _config.barterPearlsInsteadOfEndermanHunt) {
            lootable.add(Items.GOLD_INGOT);
        }

        // Add flint and steel and fire charge if not available in inventory
        if (!mod.getItemStorage().hasItemInventoryOnly(Items.FLINT_AND_STEEL)) {
            lootable.add(Items.FLINT_AND_STEEL);
        }
        if (!mod.getItemStorage().hasItemInventoryOnly(Items.FIRE_CHARGE)) {
            lootable.add(Items.FIRE_CHARGE);
        }

        // Add iron ingot if neither bucket nor water bucket is available in inventory
        if (!mod.getItemStorage().hasItemInventoryOnly(Items.BUCKET) && !mod.getItemStorage().hasItemInventoryOnly(Items.WATER_BUCKET)) {
            lootable.add(Items.IRON_INGOT);
        }

        // Add diamond if item targets for eye gear are not met in inventory
        if (!StorageHelper.itemTargetsMetInventory(mod, COLLECT_EYE_GEAR_MIN)) {
            lootable.add(Items.DIAMOND);
        }

        // Add flint if not available in inventory
        if (!mod.getItemStorage().hasItemInventoryOnly(Items.FLINT)) {
            lootable.add(Items.FLINT);
        }

        return lootable;
    }

    /**
     * This method is called when the task is interrupted
     *
     * @param mod The AltoClef mod instance
     * @param interruptTask The task that is interrupting the current task
     */
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Disable walking on end portal
        mod.getExtraBaritoneSettings().canWalkOnEndPortal(false);

        // Pop the top behaviour from the stack
        mod.getBehaviour().pop();

        // Stop tracking bed blocks
        if (mod.getBlockTracker() != null) {
            mod.getBlockTracker().stopTracking(ItemHelper.itemsToBlocks(ItemHelper.BED));
        }

        // Stop tracking custom blocks
        if (mod.getBlockTracker() != null) {
            mod.getBlockTracker().stopTracking(TRACK_BLOCKS);
        }
    }

    /**
     * Checks if the given task is equal to this MarvionBeatMinecraftTask.
     * @param other the task to compare
     * @return true if the tasks are equal, false otherwise
     */
    @Override
    protected boolean isEqual(Task other) {
        if (other == null || !(other instanceof MarvionBeatMinecraftTask)) {
            return false;
        }
        return true;
    }

    /**
     * Returns a debug string representing the action of beating the game in the Marvion version.
     */
    @Override
    protected String toDebugString() {
        return "Beating the game (Marvion version)";
    }

    /**
     * Checks if the end portal is found at the specified center position.
     *
     * @param mod The AltoClef mod instance
     * @param endPortalCenter The center position of the end portal
     * @return true if the end portal is found, false otherwise
     */
    private boolean endPortalFound(AltoClef mod, BlockPos endPortalCenter) {
        if (endPortalCenter == null) {
            return false;
        }

        if (endPortalOpened(mod, endPortalCenter)) {
            return true;
        }

        // Get the frame blocks of the end portal
        List<BlockPos> frameBlocks = getFrameBlocks(endPortalCenter);

        // Check if any of the frame blocks is a valid end portal frame block
        return frameBlocks.stream()
                .anyMatch(frame -> mod.getBlockTracker().blockIsValid(frame, Blocks.END_PORTAL_FRAME));
    }

    /**
     * Check if the end portal is already opened and the center position is provided
     *
     * @param mod The AltoClef mod instance
     * @param endPortalCenter The center position of the end portal
     * @return Whether the end portal is opened at the provided center position
     */
    private boolean endPortalOpened(AltoClef mod, BlockPos endPortalCenter) {
        // Check if the end portal is already opened and the center position is provided
        if (_endPortalOpened && endPortalCenter != null) {
            // Get the block tracker from the mod instance
            BlockTracker blockTracker = mod.getBlockTracker();
            // Check if the block tracker is available and the end portal block at the center position is valid
            return blockTracker != null && blockTracker.blockIsValid(endPortalCenter, Blocks.END_PORTAL);
        }
        return false;
    }

    /**
     * Checks if the bed spawn location is near the end portal.
     *
     * @param mod the AltoClef mod
     * @param endPortalCenter the BlockPos of the end portal center
     * @return true if the bed spawn location is near the end portal, false otherwise
     */
    private boolean spawnSetNearPortal(AltoClef mod, BlockPos endPortalCenter) {
        if (_bedSpawnLocation == null) {
            return false;
        }

        BlockTracker blockTracker = mod.getBlockTracker();

        try {
            // Check if the bed spawn location is a valid bed block
            return blockTracker.blockIsValid(_bedSpawnLocation, ItemHelper.itemsToBlocks(ItemHelper.BED));
        } catch (Exception e) {
            // Handle the exception here, for example log it or return a default value
            return false;
        }
    }

    /**
     * Locates the closest unopened ruined portal chest in the overworld.
     * If not in the overworld, returns an empty optional.
     *
     * @param mod The game modification instance
     * @return Optional<BlockPos> The position of the closest unopened ruined portal chest, or empty if not found
     */
    private Optional<BlockPos> locateClosestUnopenedRuinedPortalChest(AltoClef mod) {
        // Check if the current dimension is not the overworld
        if (!WorldHelper.getCurrentDimension().equals(Dimension.OVERWORLD)) {
            return Optional.empty();
        }

        // Find the nearest tracking block position
        try {
            return mod.getBlockTracker().getNearestTracking(blockPos -> {
                boolean isNotRuinedPortalChest = !_notRuinedPortalChests.contains(blockPos);
                boolean isUnopenedChest = WorldHelper.isUnopenedChest(mod, blockPos);
                boolean isWithinDistance = mod.getPlayer().getBlockPos().isWithinDistance(blockPos, 150);
                boolean isLootablePortalChest = canBeLootablePortalChest(mod, blockPos);

                // Return true if all conditions are met
                return isNotRuinedPortalChest && isUnopenedChest && isWithinDistance && isLootablePortalChest;
            }, Blocks.CHEST);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * This method is called when the bot starts.
     * It resets timers, pushes initial behavior onto the stack,
     * adds warning for throwaway items, tracks blocks in the world,
     * adds protected items, allows walking on the end portal,
     * avoids dragon breath, and avoids breaking the bed.
     * Any exceptions are caught and printed.
     */
    @Override
    protected void onStart(AltoClef mod) {
        try {
            dragonIsDead = false;
            enteringEndPortal = false;
            // Reset all timers
            resetTimers();

            // Push the initial behaviour onto the stack
            pushBehaviour(mod);

            // Add warning for throwaway items
            addThrowawayItemsWarning(mod);

            // Track blocks in the world
            trackBlocks(mod);

            // Add protected items
            addProtectedItems(mod);

            // Allow walking on the end portal
            allowWalkingOnEndPortal(mod);

            // Avoid dragon breath
            avoidDragonBreath(mod);

            // Avoid breaking the bed
            avoidBreakingBed(mod);
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception here
        }
    }

    /**
     * Resets all timers to their initial state.
     */
    private void resetTimers() {
        try {
            // Reset timer 1
            _timer1.reset();

            // Reset timer 2
            _timer2.reset();

            // Reset timer 3
            _timer3.reset();
        } catch (Exception e) {
            // Handle any unanticipated exceptions
            e.printStackTrace();
        }
    }

    /**
     * Pushes the behaviour of the given AltoClef object.
     *
     * @param mod the AltoClef object
     */
    private void pushBehaviour(AltoClef mod) {
        // Check if the behaviour is not null and push it
        if (mod.getBehaviour() != null) {
            mod.getBehaviour().push();
        } else {
            // Handle the case where the behaviour is null
        }
    }

    /**
     * Adds warning messages for missing configuration settings.
     * @param mod The AltoClef mod instance.
     */
    private void addThrowawayItemsWarning(AltoClef mod) {
        // Warning message tail that will be appended to the warning message.
        String settingsWarningTail = "in \".minecraft/altoclef_settings.json\". @gamer may break if you don't add this! (sorry!)";

        // Check if "end_stone" is not part of the "throwawayItems" list and log a warning.
        if (!ArrayUtils.contains(mod.getModSettings().getThrowawayItems(mod), Items.END_STONE)) {
            Debug.logWarning("\"end_stone\" is not part of your \"throwawayItems\" list " + settingsWarningTail);
        }

        // Check if "throwawayUnusedItems" is not set to true and log a warning.
        if (!mod.getModSettings().shouldThrowawayUnusedItems()) {
            Debug.logWarning("\"throwawayUnusedItems\" is not set to true " + settingsWarningTail);
        }
    }

    /**
     * Tracks specific blocks using the provided mod's BlockTracker.
     *
     * @param mod The AltoClef mod instance.
     */
    private void trackBlocks(AltoClef mod) {
        // Get the BlockTracker from the mod
        BlockTracker blockTracker = mod.getBlockTracker();

        // Track the BED block
        if (blockTracker != null) {
            blockTracker.trackBlock(ItemHelper.itemsToBlocks(ItemHelper.BED));
            blockTracker.trackBlock(TRACK_BLOCKS);
        } else {
            // Handle the case when blockTracker is null
        }
    }

    /**
     * Adds protected items to the specified AltoClef mod.
     *
     * @param mod the AltoClef mod to add protected items to
     */
    private void addProtectedItems(AltoClef mod) {
        // Add individual protected items
        mod.getBehaviour().addProtectedItems(
                Items.ENDER_EYE, // Ender Eye
                Items.BLAZE_ROD, // Blaze Rod
                Items.ENDER_PEARL, // Ender Pearl
                Items.CRAFTING_TABLE, // Crafting Table
                Items.IRON_INGOT, // Iron Ingot
                Items.WATER_BUCKET, // Water Bucket
                Items.FLINT_AND_STEEL, // Flint and Steel
                Items.SHIELD, // Shield
                Items.SHEARS, // Shears
                Items.BUCKET, // Bucket
                Items.GOLDEN_HELMET, // Golden Helmet
                Items.SMOKER, // Smoker
                Items.FURNACE, // Furnace
                Items.BLAST_FURNACE // Blast Furnace
        );

        // Add protected items using helper classes
        mod.getBehaviour().addProtectedItems(ItemHelper.BED); // Bed
        mod.getBehaviour().addProtectedItems(ItemHelper.IRON_ARMORS); // Iron Armors
        mod.getBehaviour().addProtectedItems(ItemHelper.LOG); // Log
    }

    /**
     * Allows walking on the end portal if entering the end portal and the chunk at the block position is loaded.
     *
     * @param mod The AltoClef mod instance
     */
    private void allowWalkingOnEndPortal(AltoClef mod) {
        mod.getBehaviour().allowWalkingOn(blockPos -> {
            if (enteringEndPortal && mod.getChunkTracker().isChunkLoaded(blockPos)) {
                BlockState blockState = mod.getWorld().getBlockState(blockPos);
                return blockState.getBlock() == Blocks.END_PORTAL;
            }
            return false;
        });
    }

    /**
     * Prevents the player from walking through dragon breath in the specified dimension.
     *
     * @param mod the AltoClef mod instance
     */
    private void avoidDragonBreath(AltoClef mod) {
        mod.getBehaviour().avoidWalkingThrough(blockPos -> {
            Dimension currentDimension = WorldHelper.getCurrentDimension();
            boolean isEndDimension = currentDimension == Dimension.END;
            boolean isTouchingDragonBreath = _dragonBreathTracker.isTouchingDragonBreath(blockPos);

            return isEndDimension && !_escapingDragonsBreath && isTouchingDragonBreath;
        });
    }

    /**
     * Avoid breaking the bed by preventing block breaking at the bed location.
     *
     * @param mod The AltoClef mod instance
     */
    private void avoidBreakingBed(AltoClef mod) {
        // Check if the bed spawn location is not null
        if (_bedSpawnLocation != null) {
            // Get the head and foot position of the bed
            BlockPos bedHead = WorldHelper.getBedHead(mod, _bedSpawnLocation);
            BlockPos bedFoot = WorldHelper.getBedFoot(mod, _bedSpawnLocation);

            // Prevent block breaking at the bed location
            mod.getBehaviour().avoidBlockBreaking(blockPos -> blockPos.equals(bedHead) || blockPos.equals(bedFoot));
        }
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getPlayer().getMainHandStack().getItem() instanceof EnderEyeItem &&
                !openingEndPortal) {
            List<ItemStack> itemStacks = mod.getItemStorage().getItemStacksPlayerInventory(true);
            for (ItemStack itemStack : itemStacks) {
                Item item = itemStack.getItem();
                if (item instanceof SwordItem) {
                    mod.getSlotHandler().forceEquipItem(item);
                }
            }
        }
        boolean eyeGearSatisfied = StorageHelper.isArmorEquippedAll(mod, COLLECT_EYE_ARMOR);
        boolean ironGearSatisfied = StorageHelper.isArmorEquippedAll(mod, COLLECT_IRON_ARMOR);
        if (mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE)) {
            mod.getBehaviour().setBlockBreakAdditionalPenalty(0);
        } else {
            mod.getBehaviour().setBlockBreakAdditionalPenalty(mod.getClientBaritoneSettings().blockBreakAdditionalPenalty.defaultValue);
        }
        Predicate<Task> isCraftingTableTask = task -> {
            if (task instanceof DoStuffInContainerTask cont) {
                return cont.getContainerTarget().matches(Items.CRAFTING_TABLE);
            }
            return false;
        };
        List<BlockPos> craftingTables = mod.getBlockTracker().getKnownLocations(Blocks.CRAFTING_TABLE);
        if (!craftingTables.isEmpty()) {
            for (BlockPos craftingTable : craftingTables) {
                if (mod.getItemStorage().hasItem(Items.CRAFTING_TABLE) && !thisOrChildSatisfies(isCraftingTableTask)) {
                    if (!mod.getBlockTracker().unreachable(craftingTable)) {
                        Debug.logMessage("Blacklisting extra crafting table.");
                        mod.getBlockTracker().requestBlockUnreachable(craftingTable, 0);
                    }
                }
                if (!mod.getBlockTracker().unreachable(craftingTable)) {
                    BlockState craftingTablePosUp = mod.getWorld().getBlockState(craftingTable.up(2));
                    if (mod.getEntityTracker().entityFound(WitchEntity.class)) {
                        Optional<Entity> witch = mod.getEntityTracker().getClosestEntity(WitchEntity.class);
                        if (witch.isPresent()) {
                            if (craftingTable.isWithinDistance(witch.get().getPos(), 15)) {
                                Debug.logMessage("Blacklisting witch crafting table.");
                                mod.getBlockTracker().requestBlockUnreachable(craftingTable, 0);
                            }
                        }
                    }
                    if (craftingTablePosUp.getBlock() == Blocks.WHITE_WOOL) {
                        Debug.logMessage("Blacklisting pillage crafting table.");
                        mod.getBlockTracker().requestBlockUnreachable(craftingTable, 0);
                    }
                }
            }
        }
        List<BlockPos> smokers = mod.getBlockTracker().getKnownLocations(Blocks.SMOKER);
        if (!smokers.isEmpty()) {
            for (BlockPos smoker : smokers) {
                if (mod.getItemStorage().hasItem(Items.SMOKER) && _smeltTask == null && _foodTask == null) {
                    if (!mod.getBlockTracker().unreachable(smoker)) {
                        Debug.logMessage("Blacklisting extra smoker.");
                        mod.getBlockTracker().requestBlockUnreachable(smoker, 0);
                    }
                }
            }
        }
        List<BlockPos> furnaces = mod.getBlockTracker().getKnownLocations(Blocks.FURNACE);
        if (!furnaces.isEmpty()) {
            for (BlockPos furnace : furnaces) {
                if ((mod.getItemStorage().hasItem(Items.FURNACE) || mod.getItemStorage().hasItem(Items.BLAST_FURNACE)) &&
                        _starterGearTask == null && _shieldTask == null && _ironGearTask == null && _gearTask == null &&
                        !_goToNetherTask.isActive() && !_ranStrongholdLocator) {
                    if (!mod.getBlockTracker().unreachable(furnace)) {
                        Debug.logMessage("Blacklisting extra furnace.");
                        mod.getBlockTracker().requestBlockUnreachable(furnace, 0);
                    }
                }
            }
        }
        List<BlockPos> blastFurnaces = mod.getBlockTracker().getKnownLocations(Blocks.BLAST_FURNACE);
        if (!blastFurnaces.isEmpty()) {
            for (BlockPos blastFurnace : blastFurnaces) {
                if (mod.getItemStorage().hasItem(Items.BLAST_FURNACE) && _starterGearTask == null && _shieldTask == null &&
                        _ironGearTask == null && _gearTask == null && !_goToNetherTask.isActive() && !_ranStrongholdLocator) {
                    if (!mod.getBlockTracker().unreachable(blastFurnace)) {
                        Debug.logMessage("Blacklisting extra blast furnace.");
                        mod.getBlockTracker().requestBlockUnreachable(blastFurnace, 0);
                    }
                }
            }
        }
        List<BlockPos> logs = mod.getBlockTracker().getKnownLocations(ItemHelper.itemsToBlocks(ItemHelper.LOG));
        if (!logs.isEmpty()) {
            for (BlockPos log : logs) {
                Iterable<Entity> entities = mod.getWorld().getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof PillagerEntity) {
                        if (!mod.getBlockTracker().unreachable(log)) {
                            if (log.isWithinDistance(entity.getPos(), 40)) {
                                Debug.logMessage("Blacklisting pillage log.");
                                mod.getBlockTracker().requestBlockUnreachable(log, 0);
                            }
                        }
                    }
                }
                if (log.getY() < 62) {
                    if (!mod.getBlockTracker().unreachable(log)) {
                        if (!ironGearSatisfied && !eyeGearSatisfied) {
                            Debug.logMessage("Blacklisting dangerous log.");
                            mod.getBlockTracker().requestBlockUnreachable(log, 0);
                        }
                    }
                }
            }
        }
        if (mod.getBlockTracker().isTracking(Blocks.DEEPSLATE_COAL_ORE)) {
            Optional<BlockPos> deepslateCoalOre = mod.getBlockTracker().getNearestTracking(Blocks.DEEPSLATE_COAL_ORE);
            if (deepslateCoalOre.isPresent()) {
                Iterable<Entity> entities = mod.getWorld().getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof HostileEntity) {
                        if (!mod.getBlockTracker().unreachable(deepslateCoalOre.get())) {
                            if (mod.getPlayer().squaredDistanceTo(entity.getPos()) < 150 &&
                                    deepslateCoalOre.get().isWithinDistance(entity.getPos(), 30)) {
                                if (!ironGearSatisfied && !eyeGearSatisfied) {
                                    Debug.logMessage("Blacklisting dangerous coal ore.");
                                    mod.getBlockTracker().requestBlockUnreachable(deepslateCoalOre.get(), 0);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (mod.getBlockTracker().isTracking(Blocks.COAL_ORE)) {
            Optional<BlockPos> coalOrePos = mod.getBlockTracker().getNearestTracking(Blocks.COAL_ORE);
            if (coalOrePos.isPresent()) {
                Iterable<Entity> entities = mod.getWorld().getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof HostileEntity) {
                        if (!mod.getBlockTracker().unreachable(coalOrePos.get())) {
                            if (mod.getPlayer().squaredDistanceTo(entity.getPos()) < 150 &&
                                    coalOrePos.get().isWithinDistance(entity.getPos(), 30)) {
                                if (!ironGearSatisfied && !eyeGearSatisfied) {
                                    Debug.logMessage("Blacklisting dangerous coal ore.");
                                    mod.getBlockTracker().requestBlockUnreachable(coalOrePos.get(), 0);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (mod.getBlockTracker().isTracking(Blocks.DEEPSLATE_IRON_ORE)) {
            Optional<BlockPos> deepslateIronOrePos = mod.getBlockTracker().getNearestTracking(Blocks.DEEPSLATE_IRON_ORE);
            if (deepslateIronOrePos.isPresent()) {
                Iterable<Entity> entities = mod.getWorld().getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof HostileEntity) {
                        if (!mod.getBlockTracker().unreachable(deepslateIronOrePos.get())) {
                            if (mod.getPlayer().squaredDistanceTo(entity.getPos()) < 150 &&
                                    deepslateIronOrePos.get().isWithinDistance(entity.getPos(), 30)) {
                                if (!ironGearSatisfied && !eyeGearSatisfied) {
                                    Debug.logMessage("Blacklisting dangerous iron ore.");
                                    mod.getBlockTracker().requestBlockUnreachable(deepslateIronOrePos.get(), 0);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (mod.getBlockTracker().isTracking(Blocks.IRON_ORE)) {
            Optional<BlockPos> ironOrePos = mod.getBlockTracker().getNearestTracking(Blocks.IRON_ORE);
            if (ironOrePos.isPresent()) {
                Iterable<Entity> entities = mod.getWorld().getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof HostileEntity) {
                        if (!mod.getBlockTracker().unreachable(ironOrePos.get())) {
                            if (mod.getPlayer().squaredDistanceTo(entity.getPos()) < 150 &&
                                    ironOrePos.get().isWithinDistance(entity.getPos(), 30)) {
                                if (!ironGearSatisfied && !eyeGearSatisfied) {
                                    Debug.logMessage("Blacklisting dangerous iron ore.");
                                    mod.getBlockTracker().requestBlockUnreachable(ironOrePos.get(), 0);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!mod.getItemStorage().hasItem(Items.NETHERRACK) &&
                WorldHelper.getCurrentDimension() == Dimension.NETHER && !isGettingBlazeRods &&
                !isGettingEnderPearls) {
            setDebugState("Getting netherrack.");
            if (mod.getEntityTracker().itemDropped(Items.NETHERRACK)) {
                return new PickupDroppedItemTask(Items.NETHERRACK, 1, true);
            }
            return TaskCatalogue.getItemTask(Items.NETHERRACK, 1);
        }
        if (_locateStrongholdTask.isActive()) {
            if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD) {
                if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                    if (_timer1.elapsed()) {
                        if (_config.renderDistanceManipulation) {
                            getInstance().options.getViewDistance().setValue(12);
                        }
                        _timer1.reset();
                    }
                }
            }
        }
        if ((_logsTask != null || _foodTask != null || _getOneBedTask.isActive() || _stoneGearTask != null ||
                (_sleepThroughNightTask.isActive() && !mod.getItemStorage().hasItem(ItemHelper.BED))) &&
                getBedTask == null) {
            if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                if (_timer3.getDuration() >= 30) {
                    if (_config.renderDistanceManipulation) {
                        getInstance().options.getViewDistance().setValue(12);
                        getInstance().options.getEntityDistanceScaling().setValue(1.0);
                    }
                }
                if (_timer3.elapsed()) {
                    if (_config.renderDistanceManipulation) {
                        getInstance().options.getViewDistance().setValue(32);
                        getInstance().options.getEntityDistanceScaling().setValue(5.0);
                    }
                    _timer3.reset();
                }
            }
        }
        if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD && _foodTask == null && !_getOneBedTask.isActive()
                && !_locateStrongholdTask.isActive() && _logsTask == null && _stoneGearTask == null &&
                _getPorkchopTask == null && searchBiomeTask == null && _config.renderDistanceManipulation &&
                !_ranStrongholdLocator && getBedTask == null && !_sleepThroughNightTask.isActive()) {
            if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                if (_timer1.elapsed()) {
                    if (_config.renderDistanceManipulation) {
                        getInstance().options.getViewDistance().setValue(2);
                        getInstance().options.getEntityDistanceScaling().setValue(0.5);
                    }
                    _timer1.reset();
                }
            }
        }
        if (WorldHelper.getCurrentDimension() == Dimension.NETHER) {
            if (!mod.getClientBaritone().getExploreProcess().isActive() && !_locateStrongholdTask.isActive() &&
                    _config.renderDistanceManipulation) {
                if (_timer1.elapsed()) {
                    if (_config.renderDistanceManipulation) {
                        getInstance().options.getViewDistance().setValue(12);
                        getInstance().options.getEntityDistanceScaling().setValue(1.0);
                    }
                    _timer1.reset();
                }
            }
        }
        List<Slot> torches = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.TORCH);
        List<Slot> beds = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                ItemHelper.BED);
        List<Slot> excessWaterBuckets = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.WATER_BUCKET);
        List<Slot> excessLighters = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.FLINT_AND_STEEL);
        List<Slot> sands = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.SAND);
        List<Slot> gravels = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.GRAVEL);
        List<Slot> furnaceSlots = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.FURNACE);
        List<Slot> shears = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.SHEARS);
        if (!StorageHelper.isBigCraftingOpen() && !StorageHelper.isFurnaceOpen() &&
                !StorageHelper.isSmokerOpen() && !StorageHelper.isBlastFurnaceOpen()) {
            if (!shears.isEmpty() && !needsBeds(mod)) {
                for (Slot shear : shears) {
                    if (Slot.isCursor(shear)) {
                        if (!mod.getControllerExtras().isBreakingBlock()) {
                            LookHelper.randomOrientation(mod);
                        }
                        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    } else {
                        mod.getSlotHandler().clickSlot(shear, 0, SlotActionType.PICKUP);
                    }
                }
            }
            if (!furnaceSlots.isEmpty() && mod.getItemStorage().hasItem(Items.SMOKER) &&
                    mod.getItemStorage().hasItem(Items.BLAST_FURNACE) && mod.getModSettings().shouldUseBlastFurnace()) {
                for (Slot furnace : furnaceSlots) {
                    if (Slot.isCursor(furnace)) {
                        if (!mod.getControllerExtras().isBreakingBlock()) {
                            LookHelper.randomOrientation(mod);
                        }
                        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    } else {
                        mod.getSlotHandler().clickSlot(furnace, 0, SlotActionType.PICKUP);
                    }
                }
            }
            if (!sands.isEmpty()) {
                for (Slot sand : sands) {
                    if (Slot.isCursor(sand)) {
                        if (!mod.getControllerExtras().isBreakingBlock()) {
                            LookHelper.randomOrientation(mod);
                        }
                        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    } else {
                        mod.getSlotHandler().clickSlot(sand, 0, SlotActionType.PICKUP);
                    }
                }
            }
            if (mod.getItemStorage().hasItem(Items.FLINT) || mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL)) {
                if (!gravels.isEmpty()) {
                    for (Slot gravel : gravels) {
                        if (Slot.isCursor(gravel)) {
                            if (!mod.getControllerExtras().isBreakingBlock()) {
                                LookHelper.randomOrientation(mod);
                            }
                            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                        } else {
                            mod.getSlotHandler().clickSlot(gravel, 0, SlotActionType.PICKUP);
                        }
                    }
                }
            }
            if (!torches.isEmpty()) {
                for (Slot torch : torches) {
                    if (Slot.isCursor(torch)) {
                        if (!mod.getControllerExtras().isBreakingBlock()) {
                            LookHelper.randomOrientation(mod);
                        }
                        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    } else {
                        mod.getSlotHandler().clickSlot(torch, 0, SlotActionType.PICKUP);
                    }
                }
            }
            if (mod.getItemStorage().getItemCount(Items.WATER_BUCKET) > 1) {
                if (!excessWaterBuckets.isEmpty()) {
                    for (Slot excessWaterBucket : excessWaterBuckets) {
                        if (Slot.isCursor(excessWaterBucket)) {
                            if (!mod.getControllerExtras().isBreakingBlock()) {
                                LookHelper.randomOrientation(mod);
                            }
                            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                        } else {
                            mod.getSlotHandler().clickSlot(excessWaterBucket, 0, SlotActionType.PICKUP);
                        }
                    }
                }
            }
            if (mod.getItemStorage().getItemCount(Items.FLINT_AND_STEEL) > 1) {
                if (!excessLighters.isEmpty()) {
                    for (Slot excessLighter : excessLighters) {
                        if (Slot.isCursor(excessLighter)) {
                            if (!mod.getControllerExtras().isBreakingBlock()) {
                                LookHelper.randomOrientation(mod);
                            }
                            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                        } else {
                            mod.getSlotHandler().clickSlot(excessLighter, 0, SlotActionType.PICKUP);
                        }
                    }
                }
            }
            if (mod.getItemStorage().getItemCount(ItemHelper.BED) > getTargetBeds(mod) &&
                    !endPortalFound(mod, _endPortalCenterLocation) && WorldHelper.getCurrentDimension() != Dimension.END) {
                if (!beds.isEmpty()) {
                    for (Slot bed : beds) {
                        if (Slot.isCursor(bed)) {
                            if (!mod.getControllerExtras().isBreakingBlock()) {
                                LookHelper.randomOrientation(mod);
                            }
                            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                        } else {
                            mod.getSlotHandler().clickSlot(bed, 0, SlotActionType.PICKUP);
                        }
                    }
                }
            }
        }
        /*
        if in the overworld:
          if end portal found:
            if end portal opened:
              @make sure we have iron gear and enough beds to kill the dragon first, considering whether that gear was dropped in the end
              @enter end portal
            else if we have enough eyes of ender:
              @fill in the end portal
          else if we have enough eyes of ender:
            @locate the end portal
          else:
            if we don't have diamond gear:
              if we have no food:
                @get a little bit of food
              @get diamond gear
            @go to the nether
        if in the nether:
          if we don't have enough blaze rods:
            @kill blazes till we do
          else if we don't have enough pearls:
            @kill enderman till we do
          else:
            @leave the nether
        if in the end:
          if we have a bed:
            @do bed strats
          else:
            @just hit the dragon normally
         */

        // By default, don't walk over end portals.
        enteringEndPortal = false;

        // End stuff.
        if (WorldHelper.getCurrentDimension() == Dimension.END) {
            if (!mod.getWorld().isChunkLoaded(0, 0)) {
                setDebugState("Waiting for chunks to load");
                return null;
            }
            if (_config.renderDistanceManipulation) {
                getInstance().options.getViewDistance().setValue(12);
                getInstance().options.getEntityDistanceScaling().setValue(1.0);
            }
            // If we have bed, do bed strats, otherwise punk normally.
            updateCachedEndItems(mod);
            // Grab beds
            if (mod.getEntityTracker().itemDropped(ItemHelper.BED) && (needsBeds(mod) ||
                    WorldHelper.getCurrentDimension() == Dimension.END))
                return new PickupDroppedItemTask(new ItemTarget(ItemHelper.BED), true);
            // Grab tools
            if (!mod.getItemStorage().hasItem(Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE)) {
                if (mod.getEntityTracker().itemDropped(Items.IRON_PICKAXE))
                    return new PickupDroppedItemTask(Items.IRON_PICKAXE, 1);
                if (mod.getEntityTracker().itemDropped(Items.DIAMOND_PICKAXE))
                    return new PickupDroppedItemTask(Items.DIAMOND_PICKAXE, 1);
            }
            if (!mod.getItemStorage().hasItem(Items.WATER_BUCKET) && mod.getEntityTracker().itemDropped(Items.WATER_BUCKET))
                return new PickupDroppedItemTask(Items.WATER_BUCKET, 1);
            // Grab armor
            for (Item armorCheck : COLLECT_EYE_ARMOR_END) {
                if (!StorageHelper.isArmorEquipped(mod, armorCheck)) {
                    if (mod.getItemStorage().hasItem(armorCheck)) {
                        setDebugState("Equipping armor.");
                        return new EquipArmorTask(armorCheck);
                    }
                    if (mod.getEntityTracker().itemDropped(armorCheck)) {
                        return new PickupDroppedItemTask(armorCheck, 1);
                    }
                }
            }
            // Dragons breath avoidance
            _dragonBreathTracker.updateBreath(mod);
            for (BlockPos playerIn : WorldHelper.getBlocksTouchingPlayer(mod)) {
                if (_dragonBreathTracker.isTouchingDragonBreath(playerIn)) {
                    setDebugState("ESCAPE dragons breath");
                    _escapingDragonsBreath = true;
                    return _dragonBreathTracker.getRunAwayTask();
                }
            }
            _escapingDragonsBreath = false;

            // If we find an ender portal, just GO to it!!!
            if (mod.getBlockTracker().anyFound(Blocks.END_PORTAL)) {
                setDebugState("WOOHOO");
                dragonIsDead = true;
                enteringEndPortal = true;
                if (!mod.getExtraBaritoneSettings().isCanWalkOnEndPortal()) {
                    mod.getExtraBaritoneSettings().canWalkOnEndPortal(true);
                }
                return new DoToClosestBlockTask(
                        blockPos -> new GetToBlockTask(blockPos.up()),
                        Blocks.END_PORTAL
                );
            }
            if (mod.getItemStorage().hasItem(ItemHelper.BED) ||
                    mod.getBlockTracker().anyFound(ItemHelper.itemsToBlocks(ItemHelper.BED))) {
                setDebugState("Bed strats");
                return _killDragonBedStratsTask;
            }
            setDebugState("No beds, regular strats.");
            return new KillEnderDragonTask();
        } else {
            // We're not in the end so reset our "end cache" timer
            _cachedEndItemNothingWaitTime.reset();
        }

        // Check for end portals. Always.
        if (!endPortalOpened(mod, _endPortalCenterLocation) && WorldHelper.getCurrentDimension() == Dimension.OVERWORLD) {
            Optional<BlockPos> endPortal = mod.getBlockTracker().getNearestTracking(Blocks.END_PORTAL);
            if (endPortal.isPresent()) {
                _endPortalCenterLocation = endPortal.get();
                _endPortalOpened = true;
            } else {
                // TODO: Test that this works, for some reason the bot gets stuck near the stronghold and it keeps "Searching" for the portal
                _endPortalCenterLocation = doSimpleSearchForEndPortal(mod);
            }
        }
        if (getBedTask != null) {
            // for smoker
            _smeltTask = null;
            _foodTask = null;
            // for furnace
            _starterGearTask = null;
            _shieldTask = null;
            _ironGearTask = null;
            _gearTask = null;
        }
        // Portable crafting table.
        // If we're NOT using our crafting table right now and there's one nearby, grab it.
        if (!_endPortalOpened && WorldHelper.getCurrentDimension() != Dimension.END && _config.rePickupCraftingTable &&
                !mod.getItemStorage().hasItem(Items.CRAFTING_TABLE) && !thisOrChildSatisfies(isCraftingTableTask)
                && (mod.getBlockTracker().anyFound(blockPos -> WorldHelper.canBreak(mod, blockPos) &&
                WorldHelper.canReach(mod, blockPos), Blocks.CRAFTING_TABLE) ||
                mod.getEntityTracker().itemDropped(Items.CRAFTING_TABLE))) {
            setDebugState("Picking up the crafting table while we are at it.");
            return new MineAndCollectTask(Items.CRAFTING_TABLE, 1, new Block[]{Blocks.CRAFTING_TABLE}, MiningRequirement.HAND);
        }
        if (_config.rePickupSmoker && !_endPortalOpened && WorldHelper.getCurrentDimension() != Dimension.END &&
                !mod.getItemStorage().hasItem(Items.SMOKER) &&
                (mod.getBlockTracker().anyFound(blockPos -> WorldHelper.canBreak(mod, blockPos) &&
                        WorldHelper.canReach(mod, blockPos), Blocks.SMOKER)
                        || mod.getEntityTracker().itemDropped(Items.SMOKER)) && _smeltTask == null &&
                _foodTask == null) {
            setDebugState("Picking up the smoker while we are at it.");
            return new MineAndCollectTask(Items.SMOKER, 1, new Block[]{Blocks.SMOKER}, MiningRequirement.WOOD);
        }
        if (_config.rePickupFurnace && !_endPortalOpened && WorldHelper.getCurrentDimension() != Dimension.END &&
                !mod.getItemStorage().hasItem(Items.FURNACE) &&
                (mod.getBlockTracker().anyFound(blockPos -> WorldHelper.canBreak(mod, blockPos) &&
                        WorldHelper.canReach(mod, blockPos), Blocks.FURNACE) ||
                        mod.getEntityTracker().itemDropped(Items.FURNACE)) && _starterGearTask == null &&
                _shieldTask == null && _ironGearTask == null && _gearTask == null && !_goToNetherTask.isActive() &&
                !_ranStrongholdLocator && !mod.getModSettings().shouldUseBlastFurnace()) {
            setDebugState("Picking up the furnace while we are at it.");
            return new MineAndCollectTask(Items.FURNACE, 1, new Block[]{Blocks.FURNACE}, MiningRequirement.WOOD);
        }
        if (_config.rePickupFurnace && !_endPortalOpened && WorldHelper.getCurrentDimension() != Dimension.END &&
                !mod.getItemStorage().hasItem(Items.BLAST_FURNACE) &&
                (mod.getBlockTracker().anyFound(blockPos -> WorldHelper.canBreak(mod, blockPos) &&
                        WorldHelper.canReach(mod, blockPos), Blocks.BLAST_FURNACE) ||
                        mod.getEntityTracker().itemDropped(Items.BLAST_FURNACE)) && _starterGearTask == null &&
                _shieldTask == null && _ironGearTask == null && _gearTask == null && !_goToNetherTask.isActive() &&
                !_ranStrongholdLocator && mod.getModSettings().shouldUseBlastFurnace()) {
            setDebugState("Picking up the blast furnace while we are at it.");
            return new MineAndCollectTask(Items.BLAST_FURNACE, 1, new Block[]{Blocks.BLAST_FURNACE}, MiningRequirement.WOOD);
        }

        // Sleep through night.
        if (_config.sleepThroughNight && !_endPortalOpened && WorldHelper.getCurrentDimension() == Dimension.OVERWORLD) {
            if (WorldHelper.canSleep()) {
                // for smoker
                _smeltTask = null;
                _foodTask = null;
                // for furnace
                _starterGearTask = null;
                _shieldTask = null;
                _ironGearTask = null;
                _gearTask = null;
                if (_config.renderDistanceManipulation && mod.getItemStorage().hasItem(ItemHelper.BED)) {
                    if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                        if (_timer1.elapsed()) {
                            getInstance().options.getViewDistance().setValue(2);
                            getInstance().options.getEntityDistanceScaling().setValue(0.5);
                            _timer1.reset();
                        }
                    }
                }
                if (_timer2.elapsed()) {
                    _timer2.reset();
                }
                if (_timer2.getDuration() >= 30 &&
                        !mod.getPlayer().isSleeping()) {
                    if (mod.getEntityTracker().itemDropped(ItemHelper.BED) && needsBeds(mod)) {
                        setDebugState("Resetting sleep through night task.");
                        return new PickupDroppedItemTask(new ItemTarget(ItemHelper.BED), true);
                    }
                    if (anyBedsFound(mod)) {
                        setDebugState("Resetting sleep through night task.");
                        return new DoToClosestBlockTask(DestroyBlockTask::new, ItemHelper.itemsToBlocks(ItemHelper.BED));
                    }
                }
                setDebugState("Sleeping through night");
                return _sleepThroughNightTask;
            }
            if (!mod.getItemStorage().hasItem(ItemHelper.BED)) {
                if (mod.getBlockTracker().anyFound(blockPos -> WorldHelper.canBreak(mod, blockPos), ItemHelper.itemsToBlocks(ItemHelper.BED))
                        || shouldForce(mod, _getOneBedTask)) {
                    setDebugState("Getting one bed to sleep in at night.");
                    return _getOneBedTask;
                }
            }
        }
        if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD) {
            if (needsBeds(mod) && anyBedsFound(mod)) {
                setDebugState("A bed was found, getting it.");
                if (_config.renderDistanceManipulation) {
                    if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                        if (_timer1.elapsed()) {
                            getInstance().options.getViewDistance().setValue(2);
                            getInstance().options.getEntityDistanceScaling().setValue(0.5);
                            _timer1.reset();
                        }
                    }
                }
                getBedTask = getBedTask(mod);
                return getBedTask;
            } else {
                getBedTask = null;
            }
        }

        // Do we need more eyes?
        boolean noEyesPlease = (endPortalOpened(mod, _endPortalCenterLocation) || WorldHelper.getCurrentDimension() == Dimension.END);
        int filledPortalFrames = getFilledPortalFrames(mod, _endPortalCenterLocation);
        int eyesNeededMin = noEyesPlease ? 0 : _config.minimumEyes - filledPortalFrames;
        int eyesNeeded = noEyesPlease ? 0 : _config.targetEyes - filledPortalFrames;
        int eyes = mod.getItemStorage().getItemCount(Items.ENDER_EYE);
        if (eyes < eyesNeededMin || (!_ranStrongholdLocator && _collectingEyes && eyes < eyesNeeded)) {
            _collectingEyes = true;
            _weHaveEyes = false;
            return getEyesOfEnderTask(mod, eyesNeeded);
        } else {
            _weHaveEyes = true;
            _collectingEyes = false;
        }

        // We have eyes. Locate our portal + enter.
        switch (WorldHelper.getCurrentDimension()) {
            case OVERWORLD -> {
                if (mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE)) {
                    Item[] throwGearItems = {Items.STONE_SWORD, Items.STONE_PICKAXE, Items.IRON_SWORD, Items.IRON_PICKAXE};
                    List<Slot> ironArmors = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                            COLLECT_IRON_ARMOR);
                    List<Slot> throwGears = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                            throwGearItems);
                    if (!StorageHelper.isBigCraftingOpen() && !StorageHelper.isFurnaceOpen() &&
                            !StorageHelper.isSmokerOpen() && !StorageHelper.isBlastFurnaceOpen() &&
                            (mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL) ||
                                    mod.getItemStorage().hasItem(Items.FIRE_CHARGE))) {
                        if (!throwGears.isEmpty()) {
                            for (Slot throwGear : throwGears) {
                                if (Slot.isCursor(throwGear)) {
                                    if (!mod.getControllerExtras().isBreakingBlock()) {
                                        LookHelper.randomOrientation(mod);
                                    }
                                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                                } else {
                                    mod.getSlotHandler().clickSlot(throwGear, 0, SlotActionType.PICKUP);
                                }
                            }
                        }
                        if (!ironArmors.isEmpty()) {
                            for (Slot ironArmor : ironArmors) {
                                if (Slot.isCursor(ironArmor)) {
                                    if (!mod.getControllerExtras().isBreakingBlock()) {
                                        LookHelper.randomOrientation(mod);
                                    }
                                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                                } else {
                                    mod.getSlotHandler().clickSlot(ironArmor, 0, SlotActionType.PICKUP);
                                }
                            }
                        }
                    }
                }
                // If we found our end portal...
                if (endPortalFound(mod, _endPortalCenterLocation)) {
                    // Destroy silverfish spawner
                    if (StorageHelper.miningRequirementMetInventory(mod, MiningRequirement.WOOD)) {
                        Optional<BlockPos> silverfish = mod.getBlockTracker().getNearestTracking(blockPos -> {
                            return WorldHelper.getSpawnerEntity(mod, blockPos) instanceof SilverfishEntity;
                        }, Blocks.SPAWNER);
                        if (silverfish.isPresent()) {
                            setDebugState("Breaking silverfish spawner.");
                            return new DestroyBlockTask(silverfish.get());
                        }
                    }
                    if (endPortalOpened(mod, _endPortalCenterLocation)) {
                        openingEndPortal = false;
                        if (needsBuildingMaterials(mod)) {
                            setDebugState("Collecting building materials.");
                            return _buildMaterialsTask;
                        }
                        if (_config.placeSpawnNearEndPortal && mod.getItemStorage().hasItem(ItemHelper.BED)) {
                            if (!spawnSetNearPortal(mod, _endPortalCenterLocation)) {
                                setDebugState("Setting spawn near end portal");
                                return setSpawnNearPortalTask(mod);
                            }
                        }
                        // We're as ready as we'll ever be, hop into the portal!
                        setDebugState("Entering End");
                        enteringEndPortal = true;
                        if (!mod.getExtraBaritoneSettings().isCanWalkOnEndPortal()) {
                            mod.getExtraBaritoneSettings().canWalkOnEndPortal(true);
                        }
                        return new DoToClosestBlockTask(
                                blockPos -> new GetToBlockTask(blockPos.up()),
                                Blocks.END_PORTAL
                        );
                    } else {
                        // Open the portal! (we have enough eyes, do it)
                        setDebugState("Opening End Portal");
                        openingEndPortal = true;
                        return new DoToClosestBlockTask(
                                blockPos -> new InteractWithBlockTask(Items.ENDER_EYE, blockPos),
                                blockPos -> !isEndPortalFrameFilled(mod, blockPos),
                                Blocks.END_PORTAL_FRAME
                        );
                    }
                } else {
                    _ranStrongholdLocator = true;
                    // Get beds before starting our portal location.
                    if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD && needsBeds(mod)) {
                        setDebugState("Getting beds before stronghold search.");
                        if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                            if (_timer1.elapsed()) {
                                if (_config.renderDistanceManipulation) {
                                    getInstance().options.getViewDistance().setValue(32);
                                    getInstance().options.getEntityDistanceScaling().setValue(5.0);
                                }
                                _timer1.reset();
                            }
                        }
                        getBedTask = getBedTask(mod);
                        return getBedTask;
                    } else {
                        getBedTask = null;
                    }
                    if (!mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
                        setDebugState("Getting water bucket.");
                        return TaskCatalogue.getItemTask(Items.WATER_BUCKET, 1);
                    }
                    if (!mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL)) {
                        setDebugState("Getting flint and steel.");
                        return TaskCatalogue.getItemTask(Items.FLINT_AND_STEEL, 1);
                    }
                    if (needsBuildingMaterials(mod)) {
                        setDebugState("Collecting building materials.");
                        return _buildMaterialsTask;
                    }
                    // Portal Location
                    setDebugState("Locating End Portal...");
                    return _locateStrongholdTask;
                }
            }
            case NETHER -> {
                Item[] throwGearItems = {Items.STONE_SWORD, Items.STONE_PICKAXE, Items.IRON_SWORD, Items.IRON_PICKAXE};
                List<Slot> ironArmors = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                        COLLECT_IRON_ARMOR);
                List<Slot> throwGears = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                        throwGearItems);
                if (!StorageHelper.isBigCraftingOpen() && !StorageHelper.isFurnaceOpen() &&
                        !StorageHelper.isSmokerOpen() && !StorageHelper.isBlastFurnaceOpen() &&
                        (mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL) ||
                                mod.getItemStorage().hasItem(Items.FIRE_CHARGE))) {
                    if (!throwGears.isEmpty()) {
                        for (Slot throwGear : throwGears) {
                            if (Slot.isCursor(throwGear)) {
                                if (!mod.getControllerExtras().isBreakingBlock()) {
                                    LookHelper.randomOrientation(mod);
                                }
                                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                            } else {
                                mod.getSlotHandler().clickSlot(throwGear, 0, SlotActionType.PICKUP);
                            }
                        }
                    }
                    if (!ironArmors.isEmpty()) {
                        for (Slot ironArmor : ironArmors) {
                            if (Slot.isCursor(ironArmor)) {
                                if (!mod.getControllerExtras().isBreakingBlock()) {
                                    LookHelper.randomOrientation(mod);
                                }
                                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                            } else {
                                mod.getSlotHandler().clickSlot(ironArmor, 0, SlotActionType.PICKUP);
                            }
                        }
                    }
                }
                // Portal Location
                setDebugState("Locating End Portal...");
                return _locateStrongholdTask;
            }
        }
        return null;
    }

    /**
     * Sets the spawn point near the portal.
     *
     * @param mod the AltoClef mod
     * @return the task for setting the spawn point near the portal
     */
    private Task setSpawnNearPortalTask(AltoClef mod) {
        // Check if bed spawn is set
        if (_setBedSpawnTask.isSpawnSet()) {
            _bedSpawnLocation = _setBedSpawnTask.getBedSleptPos();
        } else {
            _bedSpawnLocation = null;
        }

        // Check if spawn should be forced
        if (shouldForce(mod, _setBedSpawnTask)) {
            setDebugState("Setting spawnpoint now.");
            return _setBedSpawnTask;
        }

        // Check if player is near the end portal
        if (WorldHelper.inRangeXZ(mod.getPlayer(), WorldHelper.toVec3d(_endPortalCenterLocation), END_PORTAL_BED_SPAWN_RANGE)) {
            return _setBedSpawnTask;
        } else {
            setDebugState("Approaching portal (to set spawnpoint)");
            return new GetToXZTask(_endPortalCenterLocation.getX(), _endPortalCenterLocation.getZ());
        }
    }

    /**
     * Retrieves a task related to collecting blaze rods.
     *
     * @param mod The AltoClef mod instance
     * @param count The count of blaze rods to collect
     * @return The task related to collecting blaze rods
     */
    private Task getBlazeRodsTask(AltoClef mod, int count) {
        EntityTracker entityTracker = mod.getEntityTracker();

        // Check if blaze rods are dropped, if so, pick them up
        if (entityTracker.itemDropped(Items.BLAZE_ROD)) {
            return new PickupDroppedItemTask(Items.BLAZE_ROD, 1);
        } else if (entityTracker.itemDropped(Items.BLAZE_POWDER)) {
            // Check if blaze powder is dropped, if so, pick it up
            return new PickupDroppedItemTask(Items.BLAZE_POWDER, 1);
        } else {
            // If no blaze rods or powder is dropped, collect blaze rods
            return new CollectBlazeRodsTask(count);
        }
    }

    /**
     * This method retrieves a task for obtaining Ender Pearls.
     * @param mod the AltoClef mod instance
     * @param count the number of Ender Pearls to obtain
     * @return the task for obtaining Ender Pearls
     */
    private Task getEnderPearlTask(AltoClef mod, int count) {
        isGettingEnderPearls = true;

        // Check if we should force getting Twisting Vines.
        if (shouldForce(mod, getTwistingVines)) {
            return getTwistingVines;
        }

        // Check if Ender Pearls have been dropped as items.
        if (mod.getEntityTracker().itemDropped(Items.ENDER_PEARL)) {
            return new PickupDroppedItemTask(Items.ENDER_PEARL, 1);
        }

        // Check if we should barter Pearls instead of hunting Endermen.
        if (_config.barterPearlsInsteadOfEndermanHunt) {
            // Check if Golden Helmet is not equipped, and equip it.
            if (!StorageHelper.isArmorEquipped(mod, Items.GOLDEN_HELMET)) {
                return new EquipArmorTask(Items.GOLDEN_HELMET);
            }
            // Trade with Piglins for Ender Pearls.
            return new TradeWithPiglinsTask(32, Items.ENDER_PEARL, count);
        }

        boolean endermanFound = mod.getEntityTracker().entityFound(EndermanEntity.class);
        boolean pearlDropped = mod.getEntityTracker().itemDropped(Items.ENDER_PEARL);
        boolean hasTwistingVines = mod.getItemStorage().getItemCount(Items.TWISTING_VINES) > TWISTING_VINES_COUNT_MIN;

        // Check if we have found an Enderman or Ender Pearl and have enough Twisting Vines.
        if ((endermanFound || pearlDropped) && hasTwistingVines) {
            Optional<Entity> toKill = mod.getEntityTracker().getClosestEntity(EndermanEntity.class);
            if (toKill.isPresent() && mod.getEntityTracker().isEntityReachable(toKill.get())) {
                return new KillEndermanTask(count);
            }
        }

        boolean hasEnoughTwistingVines = mod.getItemStorage().getItemCount(Items.TWISTING_VINES) >= TWISTING_VINES_COUNT_MIN;

        // Check if we need to obtain more Twisting Vines.
        if (!hasEnoughTwistingVines) {
            BlockTracker blockTracker = mod.getBlockTracker();
            if (!blockTracker.isTracking(Blocks.TWISTING_VINES) || !blockTracker.isTracking(Blocks.TWISTING_VINES_PLANT)) {
                blockTracker.trackBlock(Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT);
            }

            boolean vinesFound = blockTracker.anyFound(Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT);
            if (vinesFound) {
                getTwistingVines = TaskCatalogue.getItemTask(Items.TWISTING_VINES, TWISTING_VINES_COUNT);
                return getTwistingVines;
            } else {
                return new SearchChunkForBlockTask(Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT, Blocks.WARPED_HYPHAE, Blocks.WARPED_NYLIUM);
            }
        }

        // Search for Ender Pearls within the warped forest biome.
        return new SearchWithinBiomeTask(BiomeKeys.WARPED_FOREST);
    }

    /**
     * Calculates the target number of beds based on the current configuration and the number of beds in the end.
     *
     * @param mod The AltoClef instance.
     * @return The target number of beds.
     */
    private int getTargetBeds(AltoClef mod) {
        // Check if the spawn needs to be set near the end portal
        boolean needsToSetSpawn = _config.placeSpawnNearEndPortal
                && (!spawnSetNearPortal(mod, _endPortalCenterLocation)
                && !shouldForce(mod, _setBedSpawnTask));

        // Calculate the number of beds in the end
        int bedsInEnd = Arrays.stream(ItemHelper.BED)
                .mapToInt(bed -> _cachedEndItemDrops.getOrDefault(bed, 0))
                .sum();

        // Calculate the target number of beds
        int targetBeds = _config.requiredBeds + (needsToSetSpawn ? 1 : 0) - bedsInEnd;

        return targetBeds;
    }

    /**
     * Check if the given AltoClef mod needs more beds based on the item count and cached end item drops.
     *
     * @param mod The AltoClef mod to check.
     * @return True if more beds are needed, false otherwise.
     */
    private boolean needsBeds(AltoClef mod) {
        // Calculate the total end items dropped
        int totalEndItems = 0;
        for (Item bed : ItemHelper.BED) {
            totalEndItems += _cachedEndItemDrops.getOrDefault(bed, 0);
        }

        // Get the current count of beds in the item storage
        int itemCount = mod.getItemStorage().getItemCount(ItemHelper.BED);

        // Get the target number of beds needed
        int targetBeds = getTargetBeds(mod);

        // Check if more beds are needed
        return (itemCount + totalEndItems) < targetBeds;
    }

    /**
     * Get the task related to obtaining beds.
     *
     * @param mod The AltoClef instance
     * @return The task related to obtaining beds
     */
    private Task getBedTask(AltoClef mod) {
        // Get the target number of beds
        int targetBeds = getTargetBeds(mod);

        // Check if shears are not available and no beds are found
        if (!mod.getItemStorage().hasItem(Items.SHEARS) && !anyBedsFound(mod)) {
            // Return the task to obtain shears
            return TaskCatalogue.getItemTask(Items.SHEARS, 1);
        }

        // Return the task to obtain the target number of beds
        return TaskCatalogue.getItemTask("bed", targetBeds);
    }

    /**
     * Checks if any beds are found in the world.
     *
     * @param mod The AltoClef mod instance
     * @return true if beds are found either in blocks or entities
     */
    private boolean anyBedsFound(AltoClef mod) {
        // Get the block and entity trackers from the mod instance.
        BlockTracker blockTracker = mod.getBlockTracker();
        EntityTracker entityTracker = mod.getEntityTracker();

        // Check if any beds are found in blocks.
        boolean bedsFoundInBlocks = blockTracker.anyFound(ItemHelper.itemsToBlocks(ItemHelper.BED));

        // Check if any beds are dropped by entities.
        boolean bedsFoundInEntities = entityTracker.itemDropped(ItemHelper.BED);

        // Return true if beds are found either in blocks or entities.
        return bedsFoundInBlocks || bedsFoundInEntities;
    }

    /**
     * Perform a simple search for the end portal.
     * @param mod The AltoClef mod instance
     * @return The average position of the end portal frames, or null if not found
     */
    private BlockPos doSimpleSearchForEndPortal(AltoClef mod) {
        // Get the locations of the end portal frames
        List<BlockPos> frames = mod.getBlockTracker().getKnownLocations(Blocks.END_PORTAL_FRAME);

        // Check if enough frames are found
        if (frames.size() >= END_PORTAL_FRAME_COUNT) {
            // Calculate the average position of the frames
            Optional<BlockPos> average = frames.stream()
                    .reduce((bpos1, bpos2) -> new BlockPos((bpos1.getX() + bpos2.getX()) / 2, (bpos1.getY() + bpos2.getY()) / 2, (bpos1.getZ() + bpos2.getZ()) / 2));

            return average.orElse(null);
        }

        return null;
    }

    /**
     * Get the count of filled portal frames around the end portal center.
     *
     * @param mod the AltoClef mod
     * @param endPortalCenter the center of the end portal
     * @return the count of filled portal frames, or 0 if the end portal is not found or the frame blocks are not loaded
     */
    private int getFilledPortalFrames(AltoClef mod, BlockPos endPortalCenter) {
        // If the end portal is found, return the constant count.
        if (endPortalFound(mod, endPortalCenter)) {
            return END_PORTAL_FRAME_COUNT;
        }

        // Get all the frame blocks around the end portal center.
        List<BlockPos> frameBlocks = getFrameBlocks(endPortalCenter);

        // Check if all the frame blocks are loaded.
        if (frameBlocks.stream().allMatch(blockPos -> mod.getChunkTracker().isChunkLoaded(blockPos))) {
            // Calculate the sum of filled frames using a stream and mapToInt.
            int filledFramesCount = frameBlocks.stream()
                    .mapToInt(blockPos -> {
                        boolean isFilled = isEndPortalFrameFilled(mod, blockPos);
                        return isFilled ? 1 : 0;
                    })
                    .sum();
            return filledFramesCount;
        }

        return 0;
    }

    /**
     * Checks if a chest at the specified position can be looted from a portal
     * @param mod The game mod
     * @param blockPos The position of the chest
     * @return true if the chest is lootable, false otherwise
     */
    private boolean canBeLootablePortalChest(AltoClef mod, BlockPos blockPos) {
        // Check if the chest is under water or below y level 50
        if (mod.getWorld().getBlockState(blockPos.up()).getBlock() == Blocks.WATER ||
                blockPos.getY() < 50) {
            return false;
        }

        // Define the minimum and maximum positions for scanning
        BlockPos minPos = blockPos.add(-4, -2, -4);
        BlockPos maxPos = blockPos.add(4, 2, 4);

        try {
            // Scan the region around the chest
            for (BlockPos checkPos : WorldHelper.scanRegion(mod, minPos, maxPos)) {
                // Check if the chest is on netherrack
                if (mod.getWorld().getBlockState(checkPos).getBlock() == Blocks.NETHERRACK) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception or log it as needed
        }

        // Add the chest to the list of non-ruined portal chests
        _notRuinedPortalChests.add(blockPos);

        return false;
    }

    private Task getEyesOfEnderTask(AltoClef mod, int targetEyes) {
        if (mod.getEntityTracker().itemDropped(Items.ENDER_EYE)) {
            setDebugState("Picking up Dropped Eyes");
            return new PickupDroppedItemTask(Items.ENDER_EYE, targetEyes);
        }

        int eyeCount = mod.getItemStorage().getItemCount(Items.ENDER_EYE);

        int blazePowderCount = mod.getItemStorage().getItemCount(Items.BLAZE_POWDER);
        int blazeRodCount = mod.getItemStorage().getItemCount(Items.BLAZE_ROD);
        int blazeRodTarget = (int) Math.ceil(((double) targetEyes - eyeCount - blazePowderCount) / 2.0);
        int enderPearlTarget = targetEyes - eyeCount;
        boolean needsBlazeRods = blazeRodCount < blazeRodTarget;
        boolean needsBlazePowder = eyeCount + blazePowderCount < targetEyes;
        boolean needsEnderPearls = mod.getItemStorage().getItemCount(Items.ENDER_PEARL) < enderPearlTarget;

        if (needsBlazePowder && !needsBlazeRods) {
            // We have enough blaze rods.
            setDebugState("Crafting blaze powder");
            return TaskCatalogue.getItemTask(Items.BLAZE_POWDER, targetEyes - eyeCount);
        }

        if (!needsBlazePowder && !needsEnderPearls) {
            // Craft ender eyes
            setDebugState("Crafting Ender Eyes");
            return TaskCatalogue.getItemTask(Items.ENDER_EYE, targetEyes);
        }

        // Get blaze rods + pearls...
        switch (WorldHelper.getCurrentDimension()) {
            case OVERWORLD -> {
                // If we happen to find beds...
                if (needsBeds(mod) && anyBedsFound(mod)) {
                    setDebugState("A bed was found, getting it.");
                    if (_config.renderDistanceManipulation) {
                        if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                            if (_timer1.elapsed()) {
                                MinecraftClient.getInstance().options.getViewDistance().setValue(2);
                                MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(0.5);
                                _timer1.reset();
                            }
                        }
                    }
                    getBedTask = getBedTask(mod);
                    return getBedTask;
                } else {
                    getBedTask = null;
                }
                if (shouldForce(mod, _logsTask)) {
                    setDebugState("Getting logs for later.");
                    return _logsTask;
                } else {
                    _logsTask = null;
                }
                if (shouldForce(mod, _stoneGearTask)) {
                    setDebugState("Getting stone gear for later.");
                    return _stoneGearTask;
                } else {
                    _stoneGearTask = null;
                }
                if (shouldForce(mod, _getPorkchopTask)) {
                    setDebugState("Getting pork chop just for fun.");
                    if (_config.renderDistanceManipulation) {
                        if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                            MinecraftClient.getInstance().options.getViewDistance().setValue(32);
                            MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(5.0);
                        }
                    }
                    return _getPorkchopTask;
                } else {
                    _getPorkchopTask = null;
                }
                if (shouldForce(mod, _starterGearTask)) {
                    setDebugState("Getting starter gear.");
                    return _starterGearTask;
                } else {
                    _starterGearTask = null;
                }
                if (shouldForce(mod, _foodTask)) {
                    setDebugState("Getting food for ender eye journey.");
                    return _foodTask;
                } else {
                    _foodTask = null;
                }
                if (shouldForce(mod, _smeltTask)) {
                    if (_config.renderDistanceManipulation) {
                        if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                            if (_timer1.elapsed()) {
                                MinecraftClient.getInstance().options.getViewDistance().setValue(2);
                                MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(0.5);
                                _timer1.reset();
                            }
                        }
                    }
                    return _smeltTask;
                } else {
                    _smeltTask = null;
                }
                // Smelt remaining raw food
                if (_config.alwaysCookRawFood) {
                    for (Item raw : ItemHelper.RAW_FOODS) {
                        if (mod.getItemStorage().hasItem(raw)) {
                            Optional<Item> cooked = ItemHelper.getCookedFood(raw);
                            if (cooked.isPresent()) {
                                int targetCount = mod.getItemStorage().getItemCount(cooked.get()) + mod.getItemStorage().getItemCount(raw);
                                setDebugState("Smelting raw food: " + ItemHelper.stripItemName(raw));
                                _smeltTask = new SmeltInSmokerTask(new SmeltTarget(new ItemTarget(cooked.get(), targetCount), new ItemTarget(raw, targetCount)));
                                return _smeltTask;
                            }
                        } else {
                            _smeltTask = null;
                        }
                    }
                }
                // Make sure we have gear, then food.
                if (shouldForce(mod, _lootTask)) {
                    setDebugState("Looting chest for goodies");
                    return _lootTask;
                }
                if (shouldForce(mod, _shieldTask) && !StorageHelper.isArmorEquipped(mod, COLLECT_SHIELD)) {
                    setDebugState("Getting shield for defense purposes only.");
                    return _shieldTask;
                } else {
                    _shieldTask = null;
                }
                if (shouldForce(mod, _ironGearTask) && !StorageHelper.isArmorEquipped(mod, COLLECT_IRON_ARMOR)) {
                    setDebugState("Getting iron gear before diamond gear for defense purposes only.");
                    return _ironGearTask;
                } else {
                    _ironGearTask = null;
                }
                if (shouldForce(mod, _gearTask) && !StorageHelper.isArmorEquipped(mod, COLLECT_EYE_ARMOR)) {
                    setDebugState("Getting diamond gear for ender eye journey.");
                    return _gearTask;
                } else {
                    _gearTask = null;
                }

                boolean eyeGearSatisfied = StorageHelper.itemTargetsMet(mod, COLLECT_EYE_GEAR_MIN) && StorageHelper.isArmorEquippedAll(mod, COLLECT_EYE_ARMOR);
                boolean ironGearSatisfied = StorageHelper.itemTargetsMet(mod, COLLECT_IRON_GEAR_MIN) && StorageHelper.isArmorEquippedAll(mod, COLLECT_IRON_ARMOR);
                boolean shieldSatisfied = StorageHelper.isArmorEquipped(mod, COLLECT_SHIELD);
                // Search for a better place
                if (!mod.getItemStorage().hasItem(Items.PORKCHOP) &&
                        !mod.getItemStorage().hasItem(Items.COOKED_PORKCHOP) &&
                        !StorageHelper.itemTargetsMet(mod, IRON_GEAR_MIN) && !ironGearSatisfied && !eyeGearSatisfied) {
                    if (mod.getItemStorage().getItemCount(ItemHelper.LOG) < 12 && !StorageHelper.itemTargetsMet(mod, COLLECT_STONE_GEAR) &&
                            !StorageHelper.itemTargetsMet(mod, IRON_GEAR_MIN) && !eyeGearSatisfied &&
                            !ironGearSatisfied) {
                        _logsTask = TaskCatalogue.getItemTask("log", 18);
                        return _logsTask;
                    } else {
                        _logsTask = null;
                    }
                    if (!StorageHelper.itemTargetsMet(mod, COLLECT_STONE_GEAR) &&
                            !StorageHelper.itemTargetsMet(mod, IRON_GEAR_MIN) && !eyeGearSatisfied &&
                            !ironGearSatisfied) {
                        if (mod.getItemStorage().getItemCount(Items.STICK) < 7) {
                            _stoneGearTask = TaskCatalogue.getItemTask(Items.STICK, 15);
                            return _stoneGearTask;
                        }
                        _stoneGearTask = TaskCatalogue.getSquashedItemTask(COLLECT_STONE_GEAR);
                        return _stoneGearTask;
                    } else {
                        _stoneGearTask = null;
                    }
                    if (mod.getEntityTracker().entityFound(PigEntity.class) && (StorageHelper.itemTargetsMet(mod,
                            COLLECT_STONE_GEAR) || StorageHelper.itemTargetsMet(mod, IRON_GEAR_MIN) ||
                            eyeGearSatisfied || ironGearSatisfied)) {
                        Predicate<Entity> notBaby = entity -> entity instanceof LivingEntity livingEntity && !livingEntity.isBaby();
                        _getPorkchopTask = new KillAndLootTask(PigEntity.class, notBaby, new ItemTarget(Items.PORKCHOP, 1));
                        return _getPorkchopTask;
                    } else {
                        _getPorkchopTask = null;
                    }
                    setDebugState("Searching a better place to start with.");
                    if (_config.renderDistanceManipulation) {
                        if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                            if (_timer1.elapsed()) {
                                MinecraftClient.getInstance().options.getViewDistance().setValue(32);
                                MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(5.0);
                                _timer1.reset();
                            }
                        }
                    }
                    searchBiomeTask = new SearchWithinBiomeTask(BiomeKeys.PLAINS);
                    return searchBiomeTask;
                } else {
                    searchBiomeTask = null;
                }
                // Then get one bed
                if (!mod.getItemStorage().hasItem(ItemHelper.BED) && _config.sleepThroughNight) {
                    return _getOneBedTask;
                }
                // Then starter gear
                if (!StorageHelper.itemTargetsMet(mod, IRON_GEAR_MIN) && !eyeGearSatisfied &&
                        !ironGearSatisfied && !StorageHelper.isArmorEquipped(mod, Items.SHIELD)) {
                    _starterGearTask = TaskCatalogue.getSquashedItemTask(IRON_GEAR);
                    return _starterGearTask;
                } else {
                    _starterGearTask = null;
                }
                // Then get food
                if (StorageHelper.calculateInventoryFoodScore(mod) < _config.minFoodUnits) {
                    _foodTask = new CollectFoodTask(_config.foodUnits);
                    return _foodTask;
                } else {
                    _foodTask = null;
                }
                // Then loot chest if there is any
                if (_config.searchRuinedPortals) {
                    // Check for ruined portals
                    Optional<BlockPos> chest = locateClosestUnopenedRuinedPortalChest(mod);
                    if (chest.isPresent()) {
                        _lootTask = new LootContainerTask(chest.get(), lootableItems(mod), _noCurseOfBinding);
                        return _lootTask;
                    }
                }
                if (_config.searchDesertTemples && StorageHelper.miningRequirementMetInventory(mod, MiningRequirement.WOOD)) {
                    // Check for desert temples
                    BlockPos temple = WorldHelper.getADesertTemple(mod);
                    if (temple != null) {
                        _lootTask = new LootDesertTempleTask(temple, lootableItems(mod));
                        return _lootTask;
                    }
                }
                // Then get shield
                if (_config.getShield && !shieldSatisfied && !mod.getFoodChain().needsToEat()) {
                    ItemTarget shield = new ItemTarget(COLLECT_SHIELD);
                    if (mod.getItemStorage().hasItem(shield) && !StorageHelper.isArmorEquipped(mod, COLLECT_SHIELD)) {
                        setDebugState("Equipping shield.");
                        return new EquipArmorTask(COLLECT_SHIELD);
                    }
                    _shieldTask = TaskCatalogue.getItemTask(shield);
                    return _shieldTask;
                } else {
                    _shieldTask = null;
                }
                // Then get iron
                if (_config.ironGearBeforeDiamondGear && !ironGearSatisfied && !eyeGearSatisfied &&
                        !_isEquippingDiamondArmor) {
                    for (Item iron : COLLECT_IRON_ARMOR) {
                        if (mod.getItemStorage().hasItem(iron) && !StorageHelper.isArmorEquipped(mod, iron)) {
                            setDebugState("Equipping armor.");
                            return new EquipArmorTask(COLLECT_IRON_ARMOR);
                        }
                    }
                    _ironGearTask = TaskCatalogue.getSquashedItemTask(Stream.concat(Arrays.stream(COLLECT_IRON_ARMOR).filter(item -> !mod.getItemStorage().hasItem(item) && !StorageHelper.isArmorEquipped(mod, item)).map(item -> new ItemTarget(item, 1)), Arrays.stream(COLLECT_IRON_GEAR)).toArray(ItemTarget[]::new));
                    return _ironGearTask;
                } else {
                    _ironGearTask = null;
                }
                // Then get diamond
                if (!eyeGearSatisfied) {
                    for (Item diamond : COLLECT_EYE_ARMOR) {
                        if (mod.getItemStorage().hasItem(diamond) && !StorageHelper.isArmorEquipped(mod, diamond)) {
                            setDebugState("Equipping armor.");
                            _isEquippingDiamondArmor = true;
                            return new EquipArmorTask(COLLECT_EYE_ARMOR);
                        }
                    }
                    _gearTask = TaskCatalogue.getSquashedItemTask(Stream.concat(Arrays.stream(COLLECT_EYE_ARMOR).filter(item -> !mod.getItemStorage().hasItem(item) && !StorageHelper.isArmorEquipped(mod, item)).map(item -> new ItemTarget(item, 1)), Arrays.stream(COLLECT_EYE_GEAR)).toArray(ItemTarget[]::new));
                    return _gearTask;
                } else {
                    _gearTask = null;
                    Item[] throwGearItems = {Items.STONE_SWORD, Items.STONE_PICKAXE, Items.IRON_SWORD, Items.IRON_PICKAXE};
                    List<Slot> ironArmors = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                            COLLECT_IRON_ARMOR);
                    List<Slot> throwGears = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                            throwGearItems);
                    if (!StorageHelper.isBigCraftingOpen() && !StorageHelper.isFurnaceOpen() &&
                            !StorageHelper.isSmokerOpen() && !StorageHelper.isBlastFurnaceOpen() &&
                            (mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL) ||
                                    mod.getItemStorage().hasItem(Items.FIRE_CHARGE))) {
                        if (!throwGears.isEmpty()) {
                            for (Slot throwGear : throwGears) {
                                if (Slot.isCursor(throwGear)) {
                                    if (!mod.getControllerExtras().isBreakingBlock()) {
                                        LookHelper.randomOrientation(mod);
                                    }
                                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                                } else {
                                    mod.getSlotHandler().clickSlot(throwGear, 0, SlotActionType.PICKUP);
                                }
                            }
                        }
                        if (!ironArmors.isEmpty()) {
                            for (Slot ironArmor : ironArmors) {
                                if (Slot.isCursor(ironArmor)) {
                                    if (!mod.getControllerExtras().isBreakingBlock()) {
                                        LookHelper.randomOrientation(mod);
                                    }
                                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                                } else {
                                    mod.getSlotHandler().clickSlot(ironArmor, 0, SlotActionType.PICKUP);
                                }
                            }
                        }
                    }
                }
                if (needsBuildingMaterials(mod)) {
                    setDebugState("Collecting building materials.");
                    return _buildMaterialsTask;
                }
                // Then go to the nether.
                setDebugState("Going to Nether");
                return _goToNetherTask;
            }
            case NETHER -> {
                if (needsEnderPearls) {
                    setDebugState("Getting Ender Pearls");
                    return getEnderPearlTask(mod, enderPearlTarget);
                }
                setDebugState("Getting Blaze Rods");
                return getBlazeRodsTask(mod, blazeRodTarget);
            }
            case END -> throw new UnsupportedOperationException("You're in the end. Don't collect eyes here.");
        }
        return null;
    }
}

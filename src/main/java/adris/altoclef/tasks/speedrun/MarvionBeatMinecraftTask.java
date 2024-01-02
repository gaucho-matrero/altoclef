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
import net.minecraft.util.math.Vec3i;
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
    private boolean _dragonIsDead = false;
    private BlockPos _endPortalCenterLocation;
    private boolean _isEquippingDiamondArmor;
    private boolean _ranStrongholdLocator;
    private boolean _endPortalOpened;
    private BlockPos _bedSpawnLocation;
    private List<BlockPos> _notRuinedPortalChests = new ArrayList<>();
    private int _cachedFilledPortalFrames = 0;
    // Controls whether we CAN walk on the end portal.
    private boolean _enterindEndPortal = false;
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
     * Returns the BeatMinecraftConfig instance.
     * If it is not already initialized, it initializes and returns a new instance.
     *
     * @return the BeatMinecraftConfig instance
     */
    public static BeatMinecraftConfig getConfig() {
        if (_config == null) {
            Debug.logInternal("Initializing BeatMinecraftConfig");
            _config = new BeatMinecraftConfig();
        }
        return _config;
    }

    /**
     * Retrieves the frame blocks surrounding the end portal center.
     *
     * @param endPortalCenter the center position of the end portal
     * @return a list of block positions representing the frame blocks
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

        // Log the frame blocks for debugging
        Debug.logInternal("Frame blocks: " + frameBlocks);

        // Return the list of frame blocks
        return frameBlocks;
    }

    /**
     * Converts an array of `Item` objects into an array of `ItemTarget` objects.
     *
     * @param items the array of `Item` objects to convert
     * @return the array of `ItemTarget` objects
     */
    private static ItemTarget[] toItemTargets(Item... items) {
        // Use the `Arrays.stream()` method to create a stream of `Item` objects
        return Arrays.stream(items)
                // Use the `map()` method to convert each `Item` object into an `ItemTarget` object
                .map(item -> {
                    // Add logging statement to print the item being converted
                    Debug.logInternal("Converting item: " + item);
                    return new ItemTarget(item);
                })
                // Use the `toArray()` method to convert the stream of `ItemTarget` objects into an array
                .toArray(ItemTarget[]::new);
    }

    /**
     * Convert an item and count into an array of ItemTargets.
     *
     * @param item  The item to be converted.
     * @param count The count of the item.
     * @return An array of ItemTargets containing the item and count.
     */
    private static ItemTarget[] toItemTargets(Item item, int count) {
        // Add a logging statement to indicate the start of the method.
        Debug.logInternal("Converting item to ItemTargets...");

        // Create a new array of ItemTargets with a length of 1.
        ItemTarget[] itemTargets = new ItemTarget[1];

        // Create a new ItemTarget with the given item and count.
        itemTargets[0] = new ItemTarget(item, count);

        // Add a logging statement to indicate the completion of the method.
        Debug.logInternal("Conversion to ItemTargets complete.");

        // Return the array of ItemTargets.
        return itemTargets;
    }

    /**
     * Combines multiple arrays of ItemTarget objects into a single array.
     *
     * @param targets The arrays of ItemTarget objects to combine.
     * @return The combined array of ItemTarget objects.
     */
    private static ItemTarget[] combine(ItemTarget[]... targets) {
        List<ItemTarget> combinedTargets = new ArrayList<>();

        // Iterate over each array of ItemTarget objects
        for (ItemTarget[] targetArray : targets) {
            // Add all elements of the array to the combinedTargets list
            combinedTargets.addAll(Arrays.asList(targetArray));
        }

        // Log the combinedTargets list
        Debug.logInternal("Combined Targets: " + combinedTargets);

        // Convert the combinedTargets list to an array and log it
        ItemTarget[] combinedArray = combinedTargets.toArray(new ItemTarget[combinedTargets.size()]);
        Debug.logInternal("Combined Array: " + Arrays.toString(combinedArray));

        // Return the combined array
        return combinedArray;
    }

    /**
     * Checks if the End Portal Frame at the given position is filled with an Eye of Ender.
     *
     * @param mod The AltoClef mod instance.
     * @param pos The position of the End Portal Frame.
     * @return True if the End Portal Frame is filled, false otherwise.
     */
    private static boolean isEndPortalFrameFilled(AltoClef mod, BlockPos pos) {
        // Check if the chunk is loaded
        if (!mod.getChunkTracker().isChunkLoaded(pos)) {
            Debug.logInternal("Chunk is not loaded");
            return false;
        }

        // Check the block state at the given position
        BlockState blockState = mod.getWorld().getBlockState(pos);
        if (blockState.getBlock() != Blocks.END_PORTAL_FRAME) {
            Debug.logInternal("Block is not an End Portal Frame");
            return false;
        }

        // Check if the End Portal Frame is filled
        boolean isFilled = blockState.get(EndPortalFrameBlock.EYE);
        Debug.logInternal("End Portal Frame is " + (isFilled ? "filled" : "not filled"));
        return isFilled;
    }

    /**
     * Checks if a task should be forced.
     *
     * @param mod  The AltoClef mod.
     * @param task The task to check.
     * @return True if the task should be forced, false otherwise.
     */
    private static boolean shouldForce(AltoClef mod, Task task) {
        // Check if the task is not null
        boolean isTaskNotNull = task != null;

        // Check if the task is active
        boolean isTaskActive = isTaskNotNull && task.isActive();

        // Check if the task is not finished
        boolean isTaskNotFinished = isTaskNotNull && !task.isFinished(mod);

        // Print task status for debugging purposes
        if (isTaskNotNull) {
            Debug.logInternal("Task is not null");
        } else {
            Debug.logInternal("Task is null");
        }

        if (isTaskActive) {
            Debug.logInternal("Task is active");
        } else {
            Debug.logInternal("Task is not active");
        }

        if (isTaskNotFinished) {
            Debug.logInternal("Task is not finished");
        } else {
            Debug.logInternal("Task is finished");
        }

        return isTaskNotNull && isTaskActive && isTaskNotFinished;
    }

    /**
     * Checks if the task is finished.
     *
     * @param mod The instance of the AltoClef mod.
     * @return True if the task is finished, false otherwise.
     */
    @Override
    public boolean isFinished(AltoClef mod) {
        // Check if the current screen is the CreditsScreen
        if (getInstance().currentScreen instanceof CreditsScreen) {
            Debug.logInternal("isFinished - Current screen is CreditsScreen");
            return true;
        }

        // Check if the dragon is dead in the Overworld
        if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD && _dragonIsDead) {
            Debug.logInternal("isFinished - Dragon is dead in the Overworld");
            return true;
        }

        // The task is not finished
        Debug.logInternal("isFinished - Returning false");
        return false;
    }

    /**
     * Checks if the mod needs building materials.
     *
     * @param mod The AltoClef mod instance.
     * @return True if building materials are needed, false otherwise.
     */
    private boolean needsBuildingMaterials(AltoClef mod) {
        int materialCount = StorageHelper.getBuildingMaterialCount(mod);
        boolean shouldForce = shouldForce(mod, _buildMaterialsTask);

        // Check if the material count is below the minimum required count
        // or if the build materials task should be forced.
        if (materialCount < _config.minBuildMaterialCount || shouldForce) {
            Debug.logInternal("Building materials needed: " + materialCount);
            Debug.logInternal("Force build materials: " + shouldForce);
            return true;
        } else {
            Debug.logInternal("Building materials not needed");
            return false;
        }
    }

    /**
     * Updates the cached end items based on the dropped items in the entity tracker.
     *
     * @param mod The AltoClef mod instance.
     */
    private void updateCachedEndItems(AltoClef mod) {
        // Get the list of dropped items from the entity tracker.
        List<ItemEntity> droppedItems = mod.getEntityTracker().getDroppedItems();

        // If there are no dropped items and the cache wait time has not elapsed, return.
        if (droppedItems.isEmpty() && !_cachedEndItemNothingWaitTime.elapsed()) {
            Debug.logInternal("No dropped items and cache wait time not elapsed.");
            return;
        }

        // Reset the cache wait time and clear the cached end item drops.
        _cachedEndItemNothingWaitTime.reset();
        _cachedEndItemDrops.clear();

        // Iterate over the dropped items to update the cached end item drops.
        for (ItemEntity entity : droppedItems) {
            Item item = entity.getStack().getItem();
            int count = entity.getStack().getCount();

            // Add the dropped item to the cached end item drops.
            _cachedEndItemDrops.put(item, _cachedEndItemDrops.getOrDefault(item, 0) + count);
            Debug.logInternal("Added dropped item: " + item + " with count: " + count);
        }
    }

    /**
     * Retrieves the cached count of the given item in the end.
     *
     * @param item The item to retrieve the count for.
     * @return The cached count of the item.
     */
    private int getEndCachedCount(Item item) {
        // Retrieve the count of the item from the cachedEndItemDrops map
        int count = _cachedEndItemDrops.getOrDefault(item, 0);

        // Log the retrieved count for debugging purposes
        Debug.logInternal("EndCachedCount: " + count);

        // Return the retrieved count
        return count;
    }

    /**
     * Checks if an item is dropped in the end.
     *
     * @param item The item to check.
     * @return True if the item is dropped in the end, false otherwise.
     */
    private boolean droppedInEnd(Item item) {
        // Get the cached count from the end.
        int cachedCount = getEndCachedCount(item);

        if (cachedCount > 0) {
            // Log the cached count when the item is dropped in the end.
            Debug.logInternal("Item dropped in end. Cached count: " + cachedCount);
            return true;
        } else {
            // Log the cached count when the item is not dropped in the end.
            Debug.logInternal("Item not dropped in end. Cached count: 0");
            return false;
        }
    }

    /**
     * Checks if the given item is present in the item storage or if it has been dropped in the end.
     *
     * @param mod  The AltoClef mod instance.
     * @param item The item to check.
     * @return True if the item is present in the item storage or if it has been dropped in the end, false otherwise.
     */
    private boolean hasItemOrDroppedInEnd(AltoClef mod, Item item) {
        // Check if the item is present in the item storage.
        boolean hasItem = mod.getItemStorage().hasItem(item);

        // Check if the item has been dropped in the end.
        boolean droppedInEnd = droppedInEnd(item);

        // Log the values for debugging purposes.
        Debug.logInternal("hasItem: " + hasItem);
        Debug.logInternal("droppedInEnd: " + droppedInEnd);

        // Return true if the item is present in the item storage or if it has been dropped in the end.
        return hasItem || droppedInEnd;
    }

    /**
     * Retrieves a list of lootable items based on certain conditions.
     *
     * @param mod The AltoClef mod instance.
     * @return The list of lootable items.
     */
    private List<Item> lootableItems(AltoClef mod) {
        List<Item> lootable = new ArrayList<>();

        // Add initial lootable items
        lootable.add(Items.GOLDEN_APPLE);
        lootable.add(Items.ENCHANTED_GOLDEN_APPLE);
        lootable.add(Items.GLISTERING_MELON_SLICE);
        lootable.add(Items.GOLDEN_CARROT);
        lootable.add(Items.OBSIDIAN);

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
            if (!mod.getItemStorage().hasItemInventoryOnly(Items.FIRE_CHARGE)) {
                lootable.add(Items.FIRE_CHARGE);
            }
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

        Debug.logInternal("Lootable items: " + lootable); // Logging statement

        return lootable;
    }

    /**
     * Overrides the onStop method.
     * Performs necessary cleanup and logging when the task is interrupted or stopped.
     *
     * @param mod           The AltoClef mod instance.
     * @param interruptTask The task that interrupted the current task.
     */
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Disable walking on end portal
        mod.getExtraBaritoneSettings().canWalkOnEndPortal(false);

        // Pop the top behaviour from the stack
        mod.getBehaviour().pop();

        // Stop tracking bed blocks
        mod.getBlockTracker().stopTracking(ItemHelper.itemsToBlocks(ItemHelper.BED));

        // Stop tracking custom blocks
        mod.getBlockTracker().stopTracking(TRACK_BLOCKS);

        // Log method stop
        Debug.logInternal("Stopped onStop method");

        // Log canWalkOnEndPortal status
        Debug.logInternal("canWalkOnEndPortal set to false");

        // Log behaviour pop
        Debug.logInternal("Behaviour popped");

        // Log stop tracking bed blocks
        Debug.logInternal("Stopped tracking BED blocks");

        // Log stop tracking custom blocks
        Debug.logInternal("Stopped tracking TRACK_BLOCKS");
    }

    /**
     * Check if the given task is equal to this MarvionBeatMinecraftTask.
     *
     * @param other The task to compare.
     * @return True if the tasks are equal, false otherwise.
     */
    @Override
    protected boolean isEqual(Task other) {
        // Check if the given task is of type MarvionBeatMinecraftTask
        boolean isSameTask = other != null && other instanceof MarvionBeatMinecraftTask;
        if (!isSameTask) {
            // Log a message if the given task is not of type MarvionBeatMinecraftTask
            Debug.logInternal("The 'other' task is not of type MarvionBeatMinecraftTask");
        }
        return isSameTask;
    }

    /**
     * Returns a debug string for the object.
     *
     * @return The debug string.
     */
    @Override
    protected String toDebugString() {
        return "Beating the game (Marvion version).";
    }

    /**
     * Checks if the end portal has been found.
     *
     * @param mod             The instance of the AltoClef mod.
     * @param endPortalCenter The center position of the end portal.
     * @return True if the end portal has been found, false otherwise.
     */
    private boolean endPortalFound(AltoClef mod, BlockPos endPortalCenter) {
        // Check if the end portal center is null
        if (endPortalCenter == null) {
            Debug.logInternal("End portal center is null");
            return false;
        }

        // Check if the end portal is already opened
        if (endPortalOpened(mod, endPortalCenter)) {
            Debug.logInternal("End portal is already opened");
            return true;
        }

        // Get the frame blocks of the end portal
        List<BlockPos> frameBlocks = getFrameBlocks(endPortalCenter);
        for (BlockPos frame : frameBlocks) {
            // Check if the frame block is a valid end portal frame
            if (mod.getBlockTracker().blockIsValid(frame, Blocks.END_PORTAL_FRAME)) {
                Debug.logInternal("Found valid end portal frame at " + frame.toString());
                return true;
            }
        }

        // No valid end portal frame found
        Debug.logInternal("No valid end portal frame found");
        return false;
    }

    /**
     * Checks if the end portal is opened.
     *
     * @param mod             The AltoClef mod instance.
     * @param endPortalCenter The center position of the end portal.
     * @return True if the end portal is opened, false otherwise.
     */
    private boolean endPortalOpened(AltoClef mod, BlockPos endPortalCenter) {
        // Check if the end portal is already opened and the center position is provided
        if (_endPortalOpened && endPortalCenter != null) {
            // Get the block tracker from the mod instance
            BlockTracker blockTracker = mod.getBlockTracker();
            // Check if the block tracker is available
            if (blockTracker != null) {
                // Check if the end portal block at the center position is valid
                boolean isValid = blockTracker.blockIsValid(endPortalCenter, Blocks.END_PORTAL);
                // Log the result of the end portal validity
                Debug.logInternal("End Portal is " + (isValid ? "valid" : "invalid"));
                return isValid;
            }
        }
        // Log that the end portal is not opened yet
        Debug.logInternal("End Portal is not opened yet");
        return false;
    }

    /**
     * Checks if the bed spawn location is near the given end portal center.
     *
     * @param mod             The AltoClef mod instance.
     * @param endPortalCenter The center position of the end portal.
     * @return True if the bed spawn location is near the end portal, false otherwise.
     */
    private boolean spawnSetNearPortal(AltoClef mod, BlockPos endPortalCenter) {
        // Check if the bed spawn location is null
        if (_bedSpawnLocation == null) {
            Debug.logInternal("Bed spawn location is null");
            return false;
        }

        // Get the block tracker instance
        BlockTracker blockTracker = mod.getBlockTracker();

        // Check if the bed spawn location is valid by comparing it with the bed block
        boolean isValid = blockTracker.blockIsValid(_bedSpawnLocation, ItemHelper.itemsToBlocks(ItemHelper.BED));

        // Log the result of the spawn set near portal check
        Debug.logInternal("Spawn set near portal: " + isValid);

        // Return the result of the check
        return isValid;
    }

    /**
     * Finds the closest unopened ruined portal chest.
     *
     * @param mod The AltoClef mod instance.
     * @return An Optional containing the closest BlockPos of the unopened ruined portal chest, or empty if not found.
     */
    private Optional<BlockPos> locateClosestUnopenedRuinedPortalChest(AltoClef mod) {
        // Check if the current dimension is not the overworld
        if (!WorldHelper.getCurrentDimension().equals(Dimension.OVERWORLD)) {
            return Optional.empty();
        }

        // Find the nearest tracking block position
        return mod.getBlockTracker().getNearestTracking(blockPos -> {
            boolean isNotRuinedPortalChest = !_notRuinedPortalChests.contains(blockPos);
            boolean isUnopenedChest = WorldHelper.isUnopenedChest(mod, blockPos);
            boolean isWithinDistance = mod.getPlayer().getBlockPos().isWithinDistance(blockPos, 150);
            boolean isLootablePortalChest = canBeLootablePortalChest(mod, blockPos);

            Debug.logInternal("isNotRuinedPortalChest: " + isNotRuinedPortalChest);
            Debug.logInternal("isUnopenedChest: " + isUnopenedChest);
            Debug.logInternal("isWithinDistance: " + isWithinDistance);
            Debug.logInternal("isLootablePortalChest: " + isLootablePortalChest);

            // Return true if all conditions are met
            return isNotRuinedPortalChest && isUnopenedChest && isWithinDistance && isLootablePortalChest;
        }, Blocks.CHEST);
    }

    /**
     * This method is called when the mod starts.
     * It performs several tasks to set up the mod.
     */
    @Override
    protected void onStart(AltoClef mod) {
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
    }

    /**
     * Resets the timers.
     */
    private void resetTimers() {
        // Reset timer 1
        _timer1.reset();

        // Reset timer 2
        _timer2.reset();

        // Reset timer 3
        _timer3.reset();
    }

    /**
     * Pushes the current behaviour onto the behaviour stack.
     * Logs the process for internal debugging.
     *
     * @param mod The AltoClef instance.
     */
    private void pushBehaviour(AltoClef mod) {
        // Log the start of the push process
        Debug.logInternal("Pushing behaviour...");

        // Push the current behaviour onto the stack
        mod.getBehaviour().push();

        // Log the successful push process
        Debug.logInternal("Behaviour pushed successfully.");
    }

    /**
     * Adds a warning message if certain conditions are not met.
     *
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
     * Tracks specific blocks using the BlockTracker.
     *
     * @param mod The AltoClef mod instance.
     */
    private void trackBlocks(AltoClef mod) {
        BlockTracker blockTracker = mod.getBlockTracker();
        blockTracker.trackBlock(ItemHelper.itemsToBlocks(ItemHelper.BED));
        blockTracker.trackBlock(TRACK_BLOCKS);

        // Add logging statements
        Debug.logInternal("Tracking blocks...");
        Debug.logInternal("BlockTracker: " + blockTracker);
        Debug.logInternal("Bed block: " + ItemHelper.itemsToBlocks(ItemHelper.BED));
        Debug.logInternal("TRACK_BLOCKS: " + TRACK_BLOCKS);
    }

    /**
     * Adds protected items to the behaviour of the given AltoClef instance.
     *
     * @param mod The AltoClef instance.
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
        mod.getBehaviour().addProtectedItems(ItemHelper.BED);
        mod.getBehaviour().addProtectedItems(ItemHelper.IRON_ARMORS);
        mod.getBehaviour().addProtectedItems(ItemHelper.LOG);

        Debug.logInternal("Protected items added successfully.");
    }

    /**
     * Allows the player to walk on an end portal block.
     *
     * @param mod The AltoClef mod instance.
     */
    private void allowWalkingOnEndPortal(AltoClef mod) {
        mod.getBehaviour().allowWalkingOn(blockPos -> {
            if (_enterindEndPortal) {
                if (mod.getChunkTracker().isChunkLoaded(blockPos)) {
                    BlockState blockState = mod.getWorld().getBlockState(blockPos);
                    boolean isEndPortal = blockState.getBlock() == Blocks.END_PORTAL;
                    if (isEndPortal) {
                        Debug.logInternal("Walking on End Portal at " + blockPos.toString());
                    }
                    return isEndPortal;
                }
            }
            return false;
        });
    }

    /**
     * Avoids walking through dragon breath in the End dimension.
     *
     * @param mod The AltoClef mod instance.
     */
    private void avoidDragonBreath(AltoClef mod) {
        mod.getBehaviour().avoidWalkingThrough(blockPos -> {
            Dimension currentDimension = WorldHelper.getCurrentDimension();
            boolean isEndDimension = currentDimension == Dimension.END;
            boolean isTouchingDragonBreath = _dragonBreathTracker.isTouchingDragonBreath(blockPos);

            if (isEndDimension && !_escapingDragonsBreath && isTouchingDragonBreath) {
                Debug.logInternal("Avoiding dragon breath at blockPos: " + blockPos);
                return true;
            } else {
                return false;
            }
        });
    }

    /**
     * Avoid breaking the bed by adding a behavior to avoid breaking specific block positions.
     *
     * @param mod The AltoClef mod instance.
     */
    private void avoidBreakingBed(AltoClef mod) {
        mod.getBehaviour().avoidBlockBreaking(blockPos -> {
            // Check if the bed spawn location is set
            if (_bedSpawnLocation != null) {
                // Get the head and foot positions of the bed
                BlockPos bedHead = WorldHelper.getBedHead(mod, _bedSpawnLocation);
                BlockPos bedFoot = WorldHelper.getBedFoot(mod, _bedSpawnLocation);

                // Check if the current block position is either the head or the foot of the bed
                boolean shouldAvoidBreaking = blockPos.equals(bedHead) || blockPos.equals(bedFoot);

                // Log a debug message if the block position should be avoided
                if (shouldAvoidBreaking) {
                    Debug.logInternal("Avoiding breaking bed at block position: " + blockPos);
                }

                return shouldAvoidBreaking;
            }

            // Return false if the bed spawn location is not set
            return false;
        });
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
        _enterindEndPortal = false;

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
                _dragonIsDead = true;
                _enterindEndPortal = true;
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
                        _enterindEndPortal = true;
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
     * @param mod The AltoClef mod instance.
     * @return The task to set the spawn point near the portal.
     */
    private Task setSpawnNearPortalTask(AltoClef mod) {
        // Check if the bed spawn is set
        if (_setBedSpawnTask.isSpawnSet()) {
            _bedSpawnLocation = _setBedSpawnTask.getBedSleptPos();
        } else {
            _bedSpawnLocation = null;
        }

        // Check if the spawn point should be forced
        if (shouldForce(mod, _setBedSpawnTask)) {
            setDebugState("Setting spawnpoint now.");
            return _setBedSpawnTask;
        }

        // Check if the player is within range of the portal
        if (WorldHelper.inRangeXZ(mod.getPlayer(), WorldHelper.toVec3d(_endPortalCenterLocation), END_PORTAL_BED_SPAWN_RANGE)) {
            return _setBedSpawnTask;
        } else {
            setDebugState("Approaching portal (to set spawnpoint)");
            return new GetToXZTask(_endPortalCenterLocation.getX(), _endPortalCenterLocation.getZ());
        }
    }

    /**
     * Returns a Task to handle Blaze Rods based on the given count.
     *
     * @param mod   The AltoClef mod instance.
     * @param count The desired count of Blaze Rods.
     * @return A Task to handle Blaze Rods.
     */
    private Task getBlazeRodsTask(AltoClef mod, int count) {
        EntityTracker entityTracker = mod.getEntityTracker();

        if (entityTracker.itemDropped(Items.BLAZE_ROD)) {
            Debug.logInternal("Blaze Rod dropped, picking it up.");
            return new PickupDroppedItemTask(Items.BLAZE_ROD, 1);
        } else if (entityTracker.itemDropped(Items.BLAZE_POWDER)) {
            Debug.logInternal("Blaze Powder dropped, picking it up.");
            return new PickupDroppedItemTask(Items.BLAZE_POWDER, 1);
        } else {
            Debug.logInternal("No Blaze Rod or Blaze Powder dropped, collecting Blaze Rods.");
            return new CollectBlazeRodsTask(count);
        }
    }

    /**
     * Returns a Task to obtain Ender Pearls.
     *
     * @param mod   The mod instance.
     * @param count The desired number of Ender Pearls.
     * @return The Task to obtain Ender Pearls.
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
     * Calculates the target number of beds based on the configuration settings.
     *
     * @param mod The AltoClef mod instance.
     * @return The target number of beds.
     */
    private int getTargetBeds(AltoClef mod) {
        // Check if spawn needs to be set near the end portal
        boolean needsToSetSpawn = _config.placeSpawnNearEndPortal
                && (!spawnSetNearPortal(mod, _endPortalCenterLocation)
                && !shouldForce(mod, _setBedSpawnTask));

        // Calculate the number of beds in the end
        int bedsInEnd = Arrays.stream(ItemHelper.BED)
                .mapToInt(bed -> _cachedEndItemDrops.getOrDefault(bed, 0))
                .sum();

        // Calculate the target number of beds
        int targetBeds = _config.requiredBeds + (needsToSetSpawn ? 1 : 0) - bedsInEnd;

        // Output debug information
        Debug.logInternal("needsToSetSpawn: " + needsToSetSpawn);
        Debug.logInternal("bedsInEnd: " + bedsInEnd);
        Debug.logInternal("targetBeds: " + targetBeds);

        return targetBeds;
    }

    /**
     * Checks if the player needs to acquire more beds.
     *
     * @param mod The instance of the AltoClef mod.
     * @return True if the player needs more beds, false otherwise.
     */
    private boolean needsBeds(AltoClef mod) {
        // Calculate the total number of end items obtained from breaking beds
        int totalEndItems = 0;
        for (Item bed : ItemHelper.BED) {
            totalEndItems += _cachedEndItemDrops.getOrDefault(bed, 0);
        }

        // Get the current number of beds in the player's inventory
        int itemCount = mod.getItemStorage().getItemCount(ItemHelper.BED);

        // Get the target number of beds to have
        int targetBeds = getTargetBeds(mod);

        // Log the values for debugging purposes
        Debug.logInternal("Total End Items: " + totalEndItems);
        Debug.logInternal("Item Count: " + itemCount);
        Debug.logInternal("Target Beds: " + targetBeds);

        // Check if the player needs to acquire more beds
        boolean needsBeds = (itemCount + totalEndItems) < targetBeds;

        // Log the result for debugging purposes
        Debug.logInternal("Needs Beds: " + needsBeds);

        // Return whether the player needs more beds
        return needsBeds;
    }

    /**
     * Retrieves a task to obtain the desired number of beds.
     *
     * @param mod The AltoClef mod instance.
     * @return The task to obtain the beds.
     */
    private Task getBedTask(AltoClef mod) {
        int targetBeds = getTargetBeds(mod);
        if (!mod.getItemStorage().hasItem(Items.SHEARS) && !anyBedsFound(mod)) {
            Debug.logInternal("Getting shears.");
            return TaskCatalogue.getItemTask(Items.SHEARS, 1);
        }
        Debug.logInternal("Getting beds.");
        return TaskCatalogue.getItemTask("bed", targetBeds);
    }

    /**
     * Checks if any beds are found in the game.
     *
     * @param mod The AltoClef mod instance.
     * @return true if beds are found either in blocks or entities, false otherwise.
     */
    private boolean anyBedsFound(AltoClef mod) {
        // Get the block and entity trackers from the mod instance.
        BlockTracker blockTracker = mod.getBlockTracker();
        EntityTracker entityTracker = mod.getEntityTracker();

        // Check if any beds are found in blocks.
        boolean bedsFoundInBlocks = blockTracker.anyFound(ItemHelper.itemsToBlocks(ItemHelper.BED));

        // Check if any beds are dropped by entities.
        boolean bedsFoundInEntities = entityTracker.itemDropped(ItemHelper.BED);

        // Log a message if beds are found in blocks.
        if (bedsFoundInBlocks) {
            Debug.logInternal("Beds found in blocks");
        }

        // Log a message if beds are found in entities.
        if (bedsFoundInEntities) {
            Debug.logInternal("Beds found in entities");
        }

        // Return true if beds are found either in blocks or entities.
        return bedsFoundInBlocks || bedsFoundInEntities;
    }

    /**
     * Searches for the position of an end portal frame by averaging the known locations of the frames.
     * Returns the center position of the frames if enough frames are found, otherwise returns null.
     *
     * @param mod The AltoClef instance.
     * @return The position of the end portal frame, or null if not enough frames are found.
     */
    private BlockPos doSimpleSearchForEndPortal(AltoClef mod) {
        List<BlockPos> frames = mod.getBlockTracker().getKnownLocations(Blocks.END_PORTAL_FRAME);

        if (frames.size() >= END_PORTAL_FRAME_COUNT) {
            // Calculate the average position of the frames.
            Vec3i average = frames.stream()
                    .reduce(Vec3i.ZERO, (accum, bpos) -> accum.add((int) Math.round(bpos.getX() + 0.5), (int) Math.round(bpos.getY() + 0.5), (int) Math.round(bpos.getZ() + 0.5)), Vec3i::add)
                    .multiply(1 / frames.size());

            // Log the average position.
            Debug.logInternal("Average Position: " + average);

            return new BlockPos(average);
        }

        // Log that there are not enough frames.
        Debug.logInternal("Not enough frames");

        return null;
    }

    /**
     * Returns the number of filled portal frames around the end portal center.
     * If the end portal is found, it returns the constant END_PORTAL_FRAME_COUNT.
     * Otherwise, it checks each frame block around the end portal center and counts the filled frames.
     * The count is cached for subsequent calls.
     *
     * @param mod             The AltoClef mod instance.
     * @param endPortalCenter The center position of the end portal.
     * @return The number of filled portal frames.
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
            _cachedFilledPortalFrames = frameBlocks.stream()
                    .mapToInt(blockPos -> {
                        boolean isFilled = isEndPortalFrameFilled(mod, blockPos);
                        // Log whether the frame is filled or not.
                        if (isFilled) {
                            Debug.logInternal("Portal frame at " + blockPos + " is filled.");
                        } else {
                            Debug.logInternal("Portal frame at " + blockPos + " is not filled.");
                        }
                        return isFilled ? 1 : 0;
                    })
                    .sum();
        }

        return _cachedFilledPortalFrames;
    }

    /**
     * Checks if a chest at the given block position can be looted as a portal chest.
     *
     * @param mod      The instance of the mod.
     * @param blockPos The block position of the chest to check.
     * @return True if the chest can be looted as a portal chest, false otherwise.
     */
    private boolean canBeLootablePortalChest(AltoClef mod, BlockPos blockPos) {
        // Check if the block above is water or if the y-coordinate is below 50
        if (mod.getWorld().getBlockState(blockPos.up()).getBlock() == Blocks.WATER ||
                blockPos.getY() < 50) {
            return false;
        }

        // Define the minimum and maximum positions to scan for NETHERRACK blocks
        BlockPos minPos = blockPos.add(-4, -2, -4);
        BlockPos maxPos = blockPos.add(4, 2, 4);

        // Log the scanning region
        Debug.logInternal("Scanning region from " + minPos + " to " + maxPos);

        // Scan the region defined by minPos and maxPos
        for (BlockPos checkPos : WorldHelper.scanRegion(mod, minPos, maxPos)) {
            // Check if the block at checkPos is NETHERRACK
            if (mod.getWorld().getBlockState(checkPos).getBlock() == Blocks.NETHERRACK) {
                return true;
            }
        }

        // Log that the blockPos is added to the list of not ruined portal chests
        Debug.logInternal("Adding blockPos " + blockPos + " to the list of not ruined portal chests");

        // Add the blockPos to the list of not ruined portal chests
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
                        !ironGearSatisfied) {
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

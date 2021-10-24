package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.misc.PlaceBedAndSetSpawnTask;
import adris.altoclef.tasks.movement.*;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StlHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class BeatMinecraft2Task extends Task {

    private static final Block[] TRACK_BLOCKS = new Block[] {
            Blocks.END_PORTAL_FRAME,
            Blocks.END_PORTAL
    };

    private static final int FOOD_UNITS = 30;
    private static final int MIN_FOOD_UNITS = 10;

    private static final ItemTarget[] COLLECT_EYE_GEAR = StlHelper.combine(
            toItemTargets(ItemHelper.DIAMOND_ARMORS),
            toItemTargets(Items.GOLDEN_BOOTS),
            toItemTargets(Items.DIAMOND_SWORD),
            toItemTargets(Items.DIAMOND_PICKAXE, 3)
    );
    private static final ItemTarget[] COLLECT_EYE_GEAR_MIN = StlHelper.combine(
            toItemTargets(ItemHelper.DIAMOND_ARMORS),
            toItemTargets(Items.GOLDEN_BOOTS),
            toItemTargets(Items.DIAMOND_SWORD),
            toItemTargets(Items.DIAMOND_PICKAXE, 1)
    );
    private static final ItemTarget[] IRON_GEAR = StlHelper.combine(
            toItemTargets(Items.IRON_SWORD),
            toItemTargets(Items.IRON_PICKAXE, 2)
    );
    private static final ItemTarget[] IRON_GEAR_MIN = StlHelper.combine(
            toItemTargets(Items.IRON_SWORD)
    );

    private static final int END_PORTAL_FRAME_COUNT = 12;

    private static final double END_PORTAL_BED_SPAWN_RANGE = 8;

    private final boolean _shouldSetSpawnNearEndPortal;

    private BlockPos _endPortalCenterLocation;
    private boolean _endPortalOpened;
    private BlockPos _bedSpawnLocation;

    private int _cachedFilledPortalFrames = 0;

    private final HashMap<Item, Integer> _cachedEndItemDrops = new HashMap<>();

    private Task _foodTask;
    private Task _gearTask;
    private final PlaceBedAndSetSpawnTask _setBedSpawnTask = new PlaceBedAndSetSpawnTask();
    private final Task _locateStrongholdTask;
    private boolean _collectingEyes;

    private final int _targetEyesMin;
    private final int _targetEyes;
    private final int _bedsToCollect;

    public BeatMinecraft2Task(boolean setSpawnNearEndPortal, int targetEnderEyesMin, int targetEnderEyes, int bedsToCollect) {
        _shouldSetSpawnNearEndPortal = setSpawnNearEndPortal;
        _targetEyesMin = targetEnderEyesMin;
        _targetEyes = targetEnderEyes;
        _bedsToCollect = bedsToCollect;
        _locateStrongholdTask = new LocateStrongholdTask(_targetEyes);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(TRACK_BLOCKS);
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(Items.ENDER_EYE, Items.BLAZE_ROD, Items.ENDER_PEARL);
        mod.getBehaviour().addProtectedItems(ItemHelper.BED);
        // Allow walking on end portal
        mod.getBehaviour().allowWalkingOn(blockPos -> mod.getChunkTracker().isChunkLoaded(blockPos) && mod.getWorld().getBlockState(blockPos).getBlock() == Blocks.END_PORTAL);
    }

    @Override
    protected Task onTick(AltoClef mod) {
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
            @barter with piglins till we do
          else:
            @leave the nether
        if in the end:
          if we have a bed:
            @do bed strats
          else:
            @just hit the dragon normally
         */

        // End stuff.
        if (mod.getCurrentDimension() == Dimension.END) {
            // If we have bed, do bed strats, otherwise punk normally.
            updateCachedEndItems(mod);
            if (mod.getInventoryTracker().hasItem(ItemHelper.BED)) {
                setDebugState("Bed strats");
                return new KillEnderDragonWithBedsTask();
            }
            setDebugState("No beds, regular strats.");
            return new KillEnderDragonTask();
        }

        // Check for end portals. Always.
        if (!endPortalFound(mod, _endPortalCenterLocation) && mod.getCurrentDimension() == Dimension.OVERWORLD) {
            BlockPos endPortal = mod.getBlockTracker().getNearestTracking(Blocks.END_PORTAL);
            if (endPortal != null) {
                _endPortalCenterLocation = endPortal;
                _endPortalOpened = true;
            } else {
                _endPortalCenterLocation = doSimpleSearchForEndPortal(mod);
            }
        }

        // Do we need more eyes?
        boolean noEyesPlease = (endPortalFound(mod, _endPortalCenterLocation) || mod.getCurrentDimension() == Dimension.END);
        int filledPortalFrames = getFilledPortalFrames(mod, _endPortalCenterLocation);
        int eyesNeededMin = noEyesPlease ? 0 : _targetEyesMin - filledPortalFrames;
        int eyesNeeded    = noEyesPlease ? 0 : _targetEyes    - filledPortalFrames;
        int eyes = mod.getInventoryTracker().getItemCount(Items.ENDER_EYE);
        if (eyes < eyesNeededMin || (_collectingEyes && eyes < eyesNeeded)) {
            _collectingEyes = true;
            return getEyesOfEnderTask(mod, eyesNeeded);
        } else {
            _collectingEyes = false;
        }

        // Collect beds
        boolean placingOrPlacedSpawn = _shouldSetSpawnNearEndPortal &&
            (
                spawnSetNearPortal(mod, _endPortalCenterLocation)
                || (_setBedSpawnTask.isActive() && !_setBedSpawnTask.isFinished(mod))
            );
        int targetBeds = _bedsToCollect + (placingOrPlacedSpawn? 1 : 0);
        if (mod.getInventoryTracker().getItemCount(ItemHelper.BED) < targetBeds) {
            if (!mod.getInventoryTracker().hasItem(Items.SHEARS)) {
                return TaskCatalogue.getItemTask(Items.SHEARS, 1);
            }
            return TaskCatalogue.getItemTask("bed", targetBeds);
        }

        // We have eyes. Locate our portal + enter.
        switch (mod.getCurrentDimension()) {
            case OVERWORLD -> {
                // If we found our end portal...
                if (endPortalFound(mod, _endPortalCenterLocation)) {
                    if (_shouldSetSpawnNearEndPortal) {
                        if (!spawnSetNearPortal(mod, _endPortalCenterLocation)) {
                            setDebugState("Setting spawn near end portal");
                            return setSpawnNearPortalTask(mod);
                        }
                    }
                    if (endPortalOpened(mod, _endPortalCenterLocation)) {
                        // Does our (current inventory) + (end dropped items inventory) satisfy (base requirements)?
                        //      If not, obtain (base requirements) - (end dropped items).
                        setDebugState("Getting equipment for End");
                        if (!hasItemOrDroppedInEnd(mod, Items.IRON_SWORD) && !hasItemOrDroppedInEnd(mod, Items.DIAMOND_SWORD)) {
                            return TaskCatalogue.getItemTask(Items.IRON_SWORD, 1);
                        }
                        if (!hasItemOrDroppedInEnd(mod, Items.WATER_BUCKET) && !hasItemOrDroppedInEnd(mod, Items.BUCKET)) {
                            return TaskCatalogue.getItemTask(Items.BUCKET, 1);
                        }
                        if (!hasItemOrDroppedInEnd(mod, Items.IRON_PICKAXE) && !hasItemOrDroppedInEnd(mod, Items.DIAMOND_PICKAXE)) {
                            return TaskCatalogue.getItemTask(Items.DIAMOND_PICKAXE, 1);
                        }
                        // Building blocks
                        if (mod.getInventoryTracker().getItemCount(mod.getModSettings().getThrowawayItems(mod)) < 40) {
                            setDebugState("Getting Building Blocks");
                            return new MineAndCollectTask(new ItemTarget[] {new ItemTarget(new Item[]{Items.DIRT, Items.COBBLESTONE}, 40)}, MiningRequirement.WOOD);
                        }
                        // We're as ready as we'll ever be, hop into the portal!
                        setDebugState("Entering End");
                        return new DoToClosestBlockTask(
                                blockPos -> new GetToBlockTask(blockPos.up()),
                                Blocks.END_PORTAL
                        );
                    } else {
                        // Open the portal! (we have enough eyes, do it)
                        setDebugState("Opening End Portal");
                        return new DoToClosestBlockTask(
                                blockPos -> new InteractWithBlockTask(Items.ENDER_EYE, blockPos),
                                blockPos -> !isEndPortalFrameFilled(mod, blockPos),
                                Blocks.END_PORTAL_FRAME
                        );
                    }
                } else {
                    // Portal Location
                    setDebugState("Locating End Portal...");
                    return _locateStrongholdTask;
                }
            }
            case NETHER -> {
                // Portal Location
                setDebugState("Locating End Portal...");
                return _locateStrongholdTask;
            }
        }

        return null;
    }

    private void updateCachedEndItems(AltoClef mod) {
        _cachedEndItemDrops.clear();
        for (ItemEntity entity : mod.getEntityTracker().getDroppedItems()) {
            Item item = entity.getStack().getItem();
            int count = entity.getStack().getCount();
            _cachedEndItemDrops.put(item, _cachedEndItemDrops.getOrDefault(item, 0) + count);
        }
    }
    private int getEndCachedCount(Item item) {
        return _cachedEndItemDrops.getOrDefault(item, 0);
    }
    private boolean droppedInEnd(Item item) {
        return getEndCachedCount(item) > 0;
    }
    private boolean hasItemOrDroppedInEnd(AltoClef mod, Item item) {
        return mod.getInventoryTracker().hasItem(item) || droppedInEnd(item);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(TRACK_BLOCKS);
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof BeatMinecraft2Task;
    }

    @Override
    protected String toDebugString() {
        return "Beating the Game.";
    }

    private static boolean endPortalFound(AltoClef mod, BlockPos endPortalCenter) {
        if (endPortalCenter == null) {
            return false;
        }
        return getFrameBlocks(endPortalCenter).allMatch(frame -> mod.getBlockTracker().blockIsValid(frame, Blocks.END_PORTAL_FRAME));
    }
    private boolean endPortalOpened(AltoClef mod, BlockPos endPortalCenter) {
        return _endPortalOpened && endPortalCenter != null && mod.getBlockTracker().blockIsValid(endPortalCenter, Blocks.END_PORTAL);
    }
    private boolean spawnSetNearPortal(AltoClef mod, BlockPos endPortalCenter) {
        return _bedSpawnLocation != null && mod.getBlockTracker().blockIsValid(endPortalCenter, ItemHelper.itemsToBlocks(ItemHelper.BED));
    }
    private int getFilledPortalFrames(AltoClef mod, BlockPos endPortalCenter) {
        if (endPortalFound(mod, endPortalCenter)) {
            Stream<BlockPos> frameBlocks = getFrameBlocks(endPortalCenter);
            // If EVERY portal frame is loaded, consider updating our cached filled portal count.
            if (frameBlocks.allMatch(blockPos -> mod.getChunkTracker().isChunkLoaded(blockPos))) {
                _cachedFilledPortalFrames = frameBlocks.reduce(0, (count, blockPos) ->
                        count + (isEndPortalFrameFilled(mod, blockPos) ? 1 : 0),
                        Integer::sum);
            }
            return _cachedFilledPortalFrames;
        }
        return 0;
    }
    private static Stream<BlockPos> getFrameBlocks(BlockPos endPortalCenter) {
        Vec3i[] frameOffsets = new Vec3i[] {
                new Vec3i(2, 0, 1),
                new Vec3i(2, 0, 0),
                new Vec3i(2, 0, -1),
                new Vec3i(-2, 0, 1),
                new Vec3i(-2, 0, 0),
                new Vec3i(-2, 0, -1),
                new Vec3i(1, 0, 2),
                new Vec3i(0, 0, 2),
                new Vec3i(-1, 0, 2),
                new Vec3i(1, 0, -2),
                new Vec3i(0, 0, -2),
                new Vec3i(-1, 0, -2)
        };
        return Arrays.stream(frameOffsets).map(endPortalCenter::add);
    }

    private Task getEyesOfEnderTask(AltoClef mod, int targetEyes) {
        if (mod.getEntityTracker().itemDropped(Items.ENDER_EYE)) {
            setDebugState("Picking up Dropped Eyes");
            return new PickupDroppedItemTask(Items.ENDER_EYE, targetEyes);
        }

        int eyeCount = mod.getInventoryTracker().getItemCount(Items.ENDER_EYE);

        int blazePowderCount = mod.getInventoryTracker().getItemCount(Items.BLAZE_POWDER);
        int blazeRodCount = mod.getInventoryTracker().getItemCount(Items.BLAZE_ROD);
        int blazeRodTarget = (int)Math.ceil(((double)targetEyes - eyeCount - blazePowderCount) / 2.0);
        int enderPearlTarget = targetEyes - eyeCount;
        boolean needsBlazeRods = blazeRodCount < blazeRodTarget;
        boolean needsBlazePowder = eyeCount + blazePowderCount < targetEyes;
        boolean needsEnderPearls = mod.getInventoryTracker().getItemCount(Items.ENDER_PEARL) < enderPearlTarget;

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
        switch (mod.getCurrentDimension()) {
            case OVERWORLD -> {
                // Make sure we have gear, then food.
                if (shouldForce(_gearTask, mod)) {
                    setDebugState("Getting gear for Ender Eye journey");
                    return _gearTask;
                }
                if (shouldForce(_foodTask, mod)) {
                    setDebugState("Getting Food for Ender Eye journey");
                    _foodTask = new CollectFoodTask(FOOD_UNITS);
                    return _foodTask;
                }
                // Start with iron
                if (!mod.getInventoryTracker().targetsMet(IRON_GEAR_MIN) && !mod.getInventoryTracker().targetsMet(COLLECT_EYE_GEAR_MIN)) {
                    _gearTask = TaskCatalogue.getSquashedItemTask(IRON_GEAR);
                    return _gearTask;
                }
                // Then get food
                if (mod.getInventoryTracker().totalFoodScore() < MIN_FOOD_UNITS) {
                    return _foodTask;
                }
                // Then get diamond
                if (!mod.getInventoryTracker().targetsMet(COLLECT_EYE_GEAR_MIN)) {
                    _gearTask = TaskCatalogue.getSquashedItemTask(COLLECT_EYE_GEAR);
                    return _gearTask;
                }
                if (!mod.getInventoryTracker().isArmorEquipped(ItemHelper.DIAMOND_ARMORS)) {
                    setDebugState("Equipping diamond armor...");
                    return new EquipArmorTask(ItemHelper.DIAMOND_ARMORS);
                }
                // Then go to the nether.
                setDebugState("Going to Nether");
                return new EnterNetherPortalTask(Dimension.NETHER);
            }
            case NETHER -> {
                if (needsBlazeRods) {
                    setDebugState("Getting Blaze Rods");
                    return getBlazeRodsTask(mod, blazeRodTarget);
                }
                setDebugState("Getting Ender Pearls");
                return getEnderPearlTask(mod, enderPearlTarget);
            }
            case END -> throw new UnsupportedOperationException("You're in the end. Don't collect eyes here.");
        }
        return null;
    }

    private Task setSpawnNearPortalTask(AltoClef mod) {
        _bedSpawnLocation = null;
        if (_setBedSpawnTask.isSpawnSet()) {
            _bedSpawnLocation = _setBedSpawnTask.getBedSleptPos();
        }
        if (shouldForce(_setBedSpawnTask, mod)) {
            // Set spawnpoint and set our bed spawn when it happens.
            setDebugState("Setting spawnpoint now.");
            return _setBedSpawnTask;
        }
        // Get close to portal. If we're close enough, set our bed spawn somewhere nearby.
        if (_endPortalCenterLocation.isWithinDistance(mod.getPlayer().getPos(), END_PORTAL_BED_SPAWN_RANGE)) {
            return _setBedSpawnTask;
        } else {
            setDebugState("Approaching portal");
            return new GetCloseToBlockTask(_endPortalCenterLocation);
        }
    }

    private Task getBlazeRodsTask(AltoClef mod, int count) {
        return new CollectBlazeRodsTask(count);
    }
    private Task getEnderPearlTask(AltoClef mod, int count) {
        // Equip golden boots before trading...
        if (!mod.getInventoryTracker().isArmorEquipped(Items.GOLDEN_BOOTS)) {
            return new EquipArmorTask(Items.GOLDEN_BOOTS);
        }
        return new TradeWithPiglinsTask(32, Items.ENDER_PEARL, count);
    }

    private BlockPos doSimpleSearchForEndPortal(AltoClef mod) {
        List<BlockPos> frames = mod.getBlockTracker().getKnownLocations(Blocks.END_PORTAL_FRAME);
        if (frames.size() >= END_PORTAL_FRAME_COUNT) {
            // Get the center of the frames.
            Vec3d average = frames.stream()
                    .reduce(Vec3d.ZERO, (accum, bpos) -> accum.add(bpos.getX(), bpos.getY(), bpos.getZ()), Vec3d::add)
                    .multiply(1.0f / frames.size());
            return new BlockPos(average.x, average.y, average.z);
        }
        return null;
    }

    private static boolean isEndPortalFrameFilled(AltoClef mod, BlockPos pos) {
        if (!mod.getChunkTracker().isChunkLoaded(pos))
            return false;
        BlockState state = mod.getWorld().getBlockState(pos);
        if (state.getBlock() != Blocks.END_PORTAL_FRAME) {
            Debug.logWarning("BLOCK POS " + pos + " DOES NOT CONTAIN END PORTAL FRAME! This is probably due to a bug/incorrect assumption.");
        }
        return state.get(EndPortalFrameBlock.EYE);
    }

    // Just a helpful utility to reduce reuse recycle.
    private static boolean shouldForce(Task task, AltoClef mod) {
        return task != null && task.isActive() && !task.isFinished(mod);
    }
    private static ItemTarget[] toItemTargets(Item ...items) {
        return Arrays.stream(items).map(item -> new ItemTarget(item, 1)).toArray(ItemTarget[]::new);
    }
    private static ItemTarget[] toItemTargets(Item item, int count) {
        return new ItemTarget[] {new ItemTarget(item, count)};
    }
}

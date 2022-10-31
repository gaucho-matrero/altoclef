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
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS,
            Items.GOLDEN_BOOTS
    };
    private static final ItemTarget[] COLLECT_STONE_GEAR = combine(
            toItemTargets(Items.STONE_SWORD, 1),
            toItemTargets(Items.STONE_PICKAXE, 2));
    private static final Item COLLECT_SHIELD = Items.SHIELD;
    private static final Item[] COLLECT_IRON_ARMOR = ItemHelper.IRON_ARMORS;
    private static final Item[] COLLECT_EYE_ARMOR_END = ItemHelper.DIAMOND_ARMORS;
    private static final ItemTarget[] COLLECT_IRON_GEAR = combine(
            toItemTargets(Items.IRON_SWORD, 2),
            toItemTargets(Items.IRON_PICKAXE, 3)
    );
    private static final ItemTarget[] COLLECT_EYE_GEAR = combine(
            toItemTargets(Items.DIAMOND_SWORD),
            toItemTargets(Items.DIAMOND_PICKAXE, 3),
            toItemTargets(Items.CRAFTING_TABLE)
    );
    private static final ItemTarget[] COLLECT_IRON_GEAR_MIN = combine(
            toItemTargets(Items.IRON_SWORD, 2)
    );
    private static final ItemTarget[] COLLECT_EYE_GEAR_MIN = combine(
            toItemTargets(Items.DIAMOND_SWORD),
            toItemTargets(Items.DIAMOND_PICKAXE, 3)
    );
    private static final ItemTarget[] IRON_GEAR = combine(
            toItemTargets(Items.IRON_SWORD, 2),
            toItemTargets(Items.IRON_PICKAXE, 3)
    );
    private static final ItemTarget[] IRON_GEAR_MIN = combine(
            toItemTargets(Items.IRON_SWORD, 2)
    );
    private static final int END_PORTAL_FRAME_COUNT = 12;
    private static final double END_PORTAL_BED_SPAWN_RANGE = 8;
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

    static {
        ConfigHelper.loadConfig("configs/beat_minecraft.json", BeatMinecraftConfig::new, BeatMinecraftConfig.class, newConfig -> _config = newConfig);
    }

    private final HashMap<Item, Integer> _cachedEndItemDrops = new HashMap<>();
    // For some reason, after death there's a frame where the game thinks there are NO items in the end.
    private final TimerGame _cachedEndItemNothingWaitTime = new TimerGame(10);
    private final Task _buildMaterialsTask;
    private final PlaceBedAndSetSpawnTask _setBedSpawnTask = new PlaceBedAndSetSpawnTask();
    private final GoToStrongholdPortalTask _locateStrongholdTask;
    private final Task _goToNetherTask = new DefaultGoToDimensionTask(Dimension.NETHER); // To keep the portal build cache.
    private final Task _getOneBedTask = TaskCatalogue.getItemTask("bed", 1);
    private final Task _sleepThroughNightTask = new SleepThroughNightTask();
    private final Task _killDragonBedStratsTask = new KillEnderDragonWithBedsTask(new WaitForDragonAndPearlTask());
    // End specific dragon breath avoidance
    private final DragonBreathTracker _dragonBreathTracker = new DragonBreathTracker();
    int timer1;
    int timer2;
    int timer3;
    boolean _weHaveEyes;
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
    private boolean _escapingDragonsBreath;
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

    public static BeatMinecraftConfig getConfig() {
        return _config;
    }

    private static List<BlockPos> getFrameBlocks(BlockPos endPortalCenter) {
        Vec3i[] frameOffsets = new Vec3i[]{
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
        return Arrays.stream(frameOffsets).map(endPortalCenter::add).toList();
    }

    private static ItemTarget[] toItemTargets(Item... items) {
        return Arrays.stream(items).map(item -> new ItemTarget(item, 1)).toArray(ItemTarget[]::new);
    }

    private static ItemTarget[] toItemTargets(Item item, int count) {
        return new ItemTarget[]{new ItemTarget(item, count)};
    }

    private static ItemTarget[] combine(ItemTarget[]... targets) {
        List<ItemTarget> result = new ArrayList<>();
        for (ItemTarget[] ts : targets) {
            result.addAll(Arrays.asList(ts));
        }
        return result.toArray(ItemTarget[]::new);
    }

    private static boolean isEndPortalFrameFilled(AltoClef mod, BlockPos pos) {
        if (!mod.getChunkTracker().isChunkLoaded(pos))
            return false;
        BlockState state = mod.getWorld().getBlockState(pos);
        if (state.getBlock() != Blocks.END_PORTAL_FRAME) {
            Debug.logWarning("BLOCK POS " + pos + " DOES NOT CONTAIN END PORTAL FRAME! This is probably due to a bug/incorrect assumption.");
            return false;
        }
        return state.get(EndPortalFrameBlock.EYE);
    }

    // Just a helpful utility to reduce reuse recycle.
    private static boolean shouldForce(AltoClef mod, Task task) {
        return task != null && task.isActive() && !task.isFinished(mod);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return getInstance().currentScreen instanceof CreditsScreen;
    }

    private boolean needsBuildingMaterials(AltoClef mod) {
        return StorageHelper.getBuildingMaterialCount(mod) < _config.minBuildMaterialCount || shouldForce(mod, _buildMaterialsTask);
    }

    private void updateCachedEndItems(AltoClef mod) {
        List<ItemEntity> droppedItems = mod.getEntityTracker().getDroppedItems();
        // If we have no items, it COULD be because we're dead. Wait a little.
        if (droppedItems.isEmpty()) {
            if (!_cachedEndItemNothingWaitTime.elapsed()) {
                return;
            }
        } else {
            _cachedEndItemNothingWaitTime.reset();
        }
        _cachedEndItemDrops.clear();
        for (ItemEntity entity : droppedItems) {
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
        return mod.getItemStorage().hasItem(item) || droppedInEnd(item);
    }

    private List<Item> lootableItems(AltoClef mod) {
        List<Item> lootable = new ArrayList<>();
        lootable.add(Items.GOLDEN_APPLE);
        lootable.add(Items.ENCHANTED_GOLDEN_APPLE);
        lootable.add(Items.GLISTERING_MELON_SLICE);
        lootable.add(Items.GOLDEN_CARROT);
        lootable.add(Items.OBSIDIAN);
        if (!StorageHelper.isArmorEquipped(mod, Items.GOLDEN_BOOTS) && !mod.getItemStorage().hasItemInventoryOnly(Items.GOLDEN_BOOTS)) {
            lootable.add(Items.GOLDEN_BOOTS);
        }
        if ((mod.getItemStorage().getItemCountInventoryOnly(Items.GOLD_INGOT) < 4 && !StorageHelper.isArmorEquipped(mod, Items.GOLDEN_BOOTS) && !mod.getItemStorage().hasItemInventoryOnly(Items.GOLDEN_BOOTS)) || _config.barterPearlsInsteadOfEndermanHunt) {
            lootable.add(Items.GOLD_INGOT);
        }
        if (!mod.getItemStorage().hasItemInventoryOnly(Items.FLINT_AND_STEEL)) {
            lootable.add(Items.FLINT_AND_STEEL);
            if (!mod.getItemStorage().hasItemInventoryOnly(Items.FIRE_CHARGE)) {
                lootable.add(Items.FIRE_CHARGE);
            }
        }
        if (!mod.getItemStorage().hasItemInventoryOnly(Items.BUCKET) && !mod.getItemStorage().hasItemInventoryOnly(Items.WATER_BUCKET)) {
            lootable.add(Items.IRON_INGOT);
        }
        if (!StorageHelper.itemTargetsMetInventory(mod, COLLECT_EYE_GEAR_MIN)) {
            lootable.add(Items.DIAMOND);
        }
        if (!mod.getItemStorage().hasItemInventoryOnly(Items.FLINT)) {
            lootable.add(Items.FLINT);
        }
        return lootable;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
        mod.getBlockTracker().stopTracking(ItemHelper.itemsToBlocks(ItemHelper.BED));
        mod.getBlockTracker().stopTracking(TRACK_BLOCKS);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof MarvionBeatMinecraftTask;
    }

    @Override
    protected String toDebugString() {
        return "Beating the game (Marvion version).";
    }

    private boolean endPortalFound(AltoClef mod, BlockPos endPortalCenter) {
        if (endPortalCenter == null) {
            return false;
        }
        if (endPortalOpened(mod, endPortalCenter)) {
            return true;
        }
        return getFrameBlocks(endPortalCenter).stream().allMatch(frame -> mod.getBlockTracker().blockIsValid(frame, Blocks.END_PORTAL_FRAME));
    }

    private boolean endPortalOpened(AltoClef mod, BlockPos endPortalCenter) {
        return _endPortalOpened && endPortalCenter != null && mod.getBlockTracker().blockIsValid(endPortalCenter, Blocks.END_PORTAL);
    }

    private boolean spawnSetNearPortal(AltoClef mod, BlockPos endPortalCenter) {
        return _bedSpawnLocation != null && mod.getBlockTracker().blockIsValid(_bedSpawnLocation, ItemHelper.itemsToBlocks(ItemHelper.BED));
    }

    private Optional<BlockPos> locateClosestUnopenedRuinedPortalChest(AltoClef mod) {
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            return Optional.empty();
        }
        return mod.getBlockTracker().getNearestTracking(blockPos -> !_notRuinedPortalChests.contains(blockPos) && WorldHelper.isUnopenedChest(mod, blockPos) && mod.getPlayer().getBlockPos().isWithinDistance(blockPos, 150) && canBeLootablePortalChest(mod, blockPos), Blocks.CHEST);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        // Add a warning to make sure the user at least knows to change the settings.
        String settingsWarningTail = "in \".minecraft/altoclef_settings.json\". @gamer may break if you don't add this! (sorry!)";
        if (!ArrayUtils.contains(mod.getModSettings().getThrowawayItems(mod), Items.END_STONE)) {
            Debug.logWarning("\"end_stone\" is not part of your \"throwawayItems\" list " + settingsWarningTail);
        }
        if (!mod.getModSettings().shouldThrowawayUnusedItems()) {
            Debug.logWarning("\"throwawayUnusedItems\" is not set to true " + settingsWarningTail);
        }

        mod.getBlockTracker().trackBlock(ItemHelper.itemsToBlocks(ItemHelper.BED));
        mod.getBlockTracker().trackBlock(TRACK_BLOCKS);
        mod.getBehaviour().addProtectedItems(Items.ENDER_EYE, Items.BLAZE_ROD, Items.ENDER_PEARL, Items.CRAFTING_TABLE,
                Items.IRON_INGOT, Items.WATER_BUCKET, Items.FLINT_AND_STEEL, Items.SHIELD, Items.SHEARS, Items.BUCKET,
                Items.GOLDEN_BOOTS, Items.SMOKER, Items.FURNACE, Items.BLAST_FURNACE);
        mod.getBehaviour().addProtectedItems(ItemHelper.BED);
        mod.getBehaviour().addProtectedItems(ItemHelper.IRON_ARMORS);
        mod.getBehaviour().addProtectedItems(ItemHelper.LOG);
        // Allow walking on end portal
        mod.getBehaviour().allowWalkingOn(blockPos -> _enterindEndPortal && mod.getChunkTracker().isChunkLoaded(blockPos) && mod.getWorld().getBlockState(blockPos).getBlock() == Blocks.END_PORTAL);

        // Avoid dragon breath
        mod.getBehaviour().avoidWalkingThrough(blockPos -> {
            return WorldHelper.getCurrentDimension() == Dimension.END && !_escapingDragonsBreath && _dragonBreathTracker.isTouchingDragonBreath(blockPos);
        });

        // Don't break the bed we placed near the end portal
        mod.getBehaviour().avoidBlockBreaking(blockPos -> {
            if (_bedSpawnLocation != null) {
                return blockPos.equals(WorldHelper.getBedHead(mod, _bedSpawnLocation)) || blockPos.equals(WorldHelper.getBedFoot(mod, _bedSpawnLocation));
            }
            return false;
        });
    }

    @Override
    protected Task onTick(AltoClef mod) {
        boolean eyeGearSatisfied = StorageHelper.isArmorEquippedAll(mod, COLLECT_EYE_ARMOR);
        boolean ironGearSatisfied = StorageHelper.isArmorEquippedAll(mod, COLLECT_IRON_ARMOR);
        if (mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE)) {
            mod.getBehaviour().setBlockBreakAdditionalPenalty(0);
        } else {
            mod.getBehaviour().setBlockBreakAdditionalPenalty(2);
        }
        Predicate<Task> isCraftingTableTask = task -> {
            if (task instanceof DoStuffInContainerTask cont) {
                return cont.getContainerTarget().matches(Items.CRAFTING_TABLE);
            }
            return false;
        };
        List<BlockPos> craftingTablePos = mod.getBlockTracker().getKnownLocations(Blocks.CRAFTING_TABLE);
        if (!craftingTablePos.isEmpty()) {
            if (mod.getItemStorage().hasItem(Items.CRAFTING_TABLE) && !thisOrChildSatisfies(isCraftingTableTask)) {
                if (!mod.getBlockTracker().unreachable(craftingTablePos.get(0))) {
                    Debug.logMessage("Blacklisting extra crafting table.");
                    mod.getBlockTracker().requestBlockUnreachable(craftingTablePos.get(0), 0);
                }
            }
            if (!mod.getBlockTracker().unreachable(craftingTablePos.get(0))) {
                BlockState craftingTablePosUp = mod.getWorld().getBlockState(craftingTablePos.get(0).up(2));
                if (mod.getEntityTracker().entityFound(WitchEntity.class)) {
                    Optional<Entity> witch = mod.getEntityTracker().getClosestEntity(WitchEntity.class);
                    if (witch.isPresent()) {
                        if (craftingTablePos.get(0).isWithinDistance(witch.get().getPos(), 15)) {
                            Debug.logMessage("Blacklisting witch crafting table.");
                            mod.getBlockTracker().requestBlockUnreachable(craftingTablePos.get(0), 0);
                        }
                    }
                }
                if (craftingTablePosUp.getBlock() == Blocks.WHITE_WOOL) {
                    Debug.logMessage("Blacklisting pillage crafting table.");
                    mod.getBlockTracker().requestBlockUnreachable(craftingTablePos.get(0), 0);
                }
            }
        }
        List<BlockPos> smokerPos = mod.getBlockTracker().getKnownLocations(Blocks.SMOKER);
        if (!smokerPos.isEmpty()) {
            if (mod.getItemStorage().hasItem(Items.SMOKER) && _smeltTask == null && _foodTask == null) {
                if (!mod.getBlockTracker().unreachable(smokerPos.get(0))) {
                    Debug.logMessage("Blacklisting extra smoker.");
                    mod.getBlockTracker().requestBlockUnreachable(smokerPos.get(0), 0);
                }
            }
        }
        List<BlockPos> furnacePos = mod.getBlockTracker().getKnownLocations(Blocks.FURNACE);
        if (!furnacePos.isEmpty()) {
            if ((mod.getItemStorage().hasItem(Items.FURNACE) || mod.getItemStorage().hasItem(Items.BLAST_FURNACE)) &&
                    _starterGearTask == null && _shieldTask == null && _ironGearTask == null && _gearTask == null &&
                    !_goToNetherTask.isActive() && !_ranStrongholdLocator) {
                if (!mod.getBlockTracker().unreachable(furnacePos.get(0))) {
                    Debug.logMessage("Blacklisting extra furnace.");
                    mod.getBlockTracker().requestBlockUnreachable(furnacePos.get(0), 0);
                }
            }
        }
        List<BlockPos> blastFurnacePos = mod.getBlockTracker().getKnownLocations(Blocks.BLAST_FURNACE);
        if (!blastFurnacePos.isEmpty()) {
            if (mod.getItemStorage().hasItem(Items.BLAST_FURNACE) && _starterGearTask == null && _shieldTask == null &&
                    _ironGearTask == null && _gearTask == null && !_goToNetherTask.isActive() && !_ranStrongholdLocator) {
                if (!mod.getBlockTracker().unreachable(blastFurnacePos.get(0))) {
                    Debug.logMessage("Blacklisting extra blast furnace.");
                    mod.getBlockTracker().requestBlockUnreachable(blastFurnacePos.get(0), 0);
                }
            }
        }
        Block[] logs = ItemHelper.itemsToBlocks(ItemHelper.LOG);
        for (Block log : logs) {
            if (mod.getBlockTracker().isTracking(log)) {
                Optional<BlockPos> logPos = mod.getBlockTracker().getNearestTracking(log);
                if (logPos.isPresent()) {
                    Iterable<Entity> entities = mod.getWorld().getEntities();
                    for (Entity entity : entities) {
                        if (entity instanceof PillagerEntity) {
                            if (!mod.getBlockTracker().unreachable(logPos.get())) {
                                if (logPos.get().isWithinDistance(entity.getPos(), 30)) {
                                    Debug.logMessage("Blacklisting pillage log.");
                                    mod.getBlockTracker().requestBlockUnreachable(logPos.get(), 0);
                                }
                            }
                        }
                    }
                }
            }
        }
        List<BlockPos> logPos = mod.getBlockTracker().getKnownLocations(ItemHelper.itemsToBlocks(ItemHelper.LOG));
        if (!logPos.isEmpty()) {
            if (logPos.get(0).getY() < 62) {
                if (!mod.getBlockTracker().unreachable(logPos.get(0))) {
                    if (!ironGearSatisfied && !eyeGearSatisfied) {
                        Debug.logMessage("Blacklisting dangerous log.");
                        mod.getBlockTracker().requestBlockUnreachable(logPos.get(0), 0);
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
            return TaskCatalogue.getItemTask(Items.NETHERRACK, 1);
        }
        if (_locateStrongholdTask.isActive()) {
            if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD) {
                if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                    timer1++;
                    if (timer1 >= 500) {
                        if (_config.renderDistanceManipulation) {
                            MinecraftClient.getInstance().options.getViewDistance().setValue(12);
                        }
                        timer1 = 0;
                    }
                }
            }
        }
        if ((_logsTask != null || _foodTask != null || _getOneBedTask.isActive() || _stoneGearTask != null ||
                (_sleepThroughNightTask.isActive() && !mod.getItemStorage().hasItem(ItemHelper.BED))) &&
                getBedTask == null) {
            if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                timer3++;
                if (timer3 >= 1000) {
                    if (_config.renderDistanceManipulation) {
                        MinecraftClient.getInstance().options.getViewDistance().setValue(32);
                        MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(5.0);
                    }
                    timer3 = 0;
                }
                if (timer3 >= 500) {
                    if (_config.renderDistanceManipulation) {
                        MinecraftClient.getInstance().options.getViewDistance().setValue(12);
                        MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(1.0);
                    }
                }
            }
        }
        if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD && _foodTask == null && !_getOneBedTask.isActive()
                && !_locateStrongholdTask.isActive() && _logsTask == null && _stoneGearTask == null &&
                _getPorkchopTask == null && searchBiomeTask == null && _config.renderDistanceManipulation &&
                !_ranStrongholdLocator && getBedTask == null && !_sleepThroughNightTask.isActive()) {
            if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                timer1++;
                if (timer1 >= 500) {
                    if (_config.renderDistanceManipulation) {
                        MinecraftClient.getInstance().options.getViewDistance().setValue(2);
                        MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(0.5);
                    }
                    timer1 = 0;
                }
            }
        }
        if (WorldHelper.getCurrentDimension() == Dimension.NETHER) {
            if (!mod.getClientBaritone().getExploreProcess().isActive() && !_locateStrongholdTask.isActive() &&
                    _config.renderDistanceManipulation) {
                timer1++;
                if (timer1 >= 500) {
                    if (_config.renderDistanceManipulation) {
                        MinecraftClient.getInstance().options.getViewDistance().setValue(12);
                        MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(1.0);
                    }
                    timer1 = 0;
                }
            }
        }
        List<Slot> hastorch = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.TORCH);
        List<Slot> hasbed = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                ItemHelper.BED);
        List<Slot> excessWaterBucket = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.WATER_BUCKET);
        List<Slot> excessLighter = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.FLINT_AND_STEEL);
        List<Slot> hasSand = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.SAND);
        List<Slot> hasGravel = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.GRAVEL);
        List<Slot> hasFurnace = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.FURNACE);
        List<Slot> hasShears = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                Items.SHEARS);
        if (!StorageHelper.isBigCraftingOpen() && !StorageHelper.isFurnaceOpen() &&
                !StorageHelper.isSmokerOpen() && !StorageHelper.isBlastFurnaceOpen()) {
            if (!hasShears.isEmpty() && !needsBeds(mod)) {
                Slot shearsInSlot = hasShears.get(0);
                if (Slot.isCursor(shearsInSlot)) {
                    if (!mod.getControllerExtras().isBreakingBlock()) {
                        LookHelper.randomOrientation(mod);
                    }
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                } else {
                    mod.getSlotHandler().clickSlot(shearsInSlot, 0, SlotActionType.PICKUP);
                }
            }
            if (!hasFurnace.isEmpty() && mod.getItemStorage().hasItem(Items.SMOKER) &&
                    mod.getItemStorage().hasItem(Items.BLAST_FURNACE) && mod.getModSettings().shouldUseBlastFurnace()) {
                Slot furnaceInSlot = hasFurnace.get(0);
                if (Slot.isCursor(furnaceInSlot)) {
                    if (!mod.getControllerExtras().isBreakingBlock()) {
                        LookHelper.randomOrientation(mod);
                    }
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                } else {
                    mod.getSlotHandler().clickSlot(furnaceInSlot, 0, SlotActionType.PICKUP);
                }
            }
            if (!hasSand.isEmpty()) {
                Slot sandInSlot = hasSand.get(0);
                if (Slot.isCursor(sandInSlot)) {
                    if (!mod.getControllerExtras().isBreakingBlock()) {
                        LookHelper.randomOrientation(mod);
                    }
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                } else {
                    mod.getSlotHandler().clickSlot(sandInSlot, 0, SlotActionType.PICKUP);
                }
            }
            if (mod.getItemStorage().hasItem(Items.FLINT) || mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL)) {
                if (!hasGravel.isEmpty()) {
                    Slot gravelInSlot = hasGravel.get(0);
                    if (Slot.isCursor(gravelInSlot)) {
                        if (!mod.getControllerExtras().isBreakingBlock()) {
                            LookHelper.randomOrientation(mod);
                        }
                        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    } else {
                        mod.getSlotHandler().clickSlot(gravelInSlot, 0, SlotActionType.PICKUP);
                    }
                }
            }
            if (!hastorch.isEmpty()) {
                Slot torchinslot = hastorch.get(0);
                if (Slot.isCursor(torchinslot)) {
                    if (!mod.getControllerExtras().isBreakingBlock()) {
                        LookHelper.randomOrientation(mod);
                    }
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                } else {
                    mod.getSlotHandler().clickSlot(torchinslot, 0, SlotActionType.PICKUP);
                }
            }
            if (mod.getItemStorage().getItemCount(Items.WATER_BUCKET) > 1) {
                if (!excessWaterBucket.isEmpty()) {
                    Slot waterBucketInSlot = excessWaterBucket.get(0);
                    if (Slot.isCursor(waterBucketInSlot)) {
                        if (!mod.getControllerExtras().isBreakingBlock()) {
                            LookHelper.randomOrientation(mod);
                        }
                        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    } else {
                        mod.getSlotHandler().clickSlot(waterBucketInSlot, 0, SlotActionType.PICKUP);
                    }
                }
            }
            if (mod.getItemStorage().getItemCount(Items.FLINT_AND_STEEL) > 1) {
                if (!excessLighter.isEmpty()) {
                    Slot lighterInSlot = excessLighter.get(0);
                    if (Slot.isCursor(lighterInSlot)) {
                        if (!mod.getControllerExtras().isBreakingBlock()) {
                            LookHelper.randomOrientation(mod);
                        }
                        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    } else {
                        mod.getSlotHandler().clickSlot(lighterInSlot, 0, SlotActionType.PICKUP);
                    }
                }
            }
            if (mod.getItemStorage().getItemCount(ItemHelper.BED) > getTargetBeds(mod) &&
                    !endPortalFound(mod, _endPortalCenterLocation) && WorldHelper.getCurrentDimension() != Dimension.END) {
                if (!hasbed.isEmpty()) {
                    Slot bedinslot = hasbed.get(0);
                    if (Slot.isCursor(bedinslot)) {
                        if (!mod.getControllerExtras().isBreakingBlock()) {
                            LookHelper.randomOrientation(mod);
                        }
                        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                    } else {
                        mod.getSlotHandler().clickSlot(bedinslot, 0, SlotActionType.PICKUP);
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
            if (_config.renderDistanceManipulation) {
                MinecraftClient.getInstance().options.getViewDistance().setValue(12);
                MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(1.0);
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
            if (!mod.getEntityTracker().entityFound(EnderDragonEntity.class)) {
                if (mod.getPlayer().getBlockPos().getX() == 0 && mod.getPlayer().getBlockPos().getZ() == 0) {
                    getInstance().player.setPitch(-90);
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
                _enterindEndPortal = true;
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
                        timer1++;
                        if (timer1 >= 500) {
                            MinecraftClient.getInstance().options.getViewDistance().setValue(2);
                            MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(0.5);
                            timer1 = 0;
                        }
                    }
                }
                timer2++;
                if (timer2 >= 1100) {
                    timer2 = 0;
                }
                if (timer2 >= 1000) {
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
            } else {
                timer2 = 0;
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
                        timer1++;
                        if (timer1 >= 500) {
                            MinecraftClient.getInstance().options.getViewDistance().setValue(2);
                            MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(0.5);
                            timer1 = 0;
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
                    Item[] throwGear = {Items.STONE_SWORD, Items.STONE_PICKAXE, Items.IRON_SWORD, Items.IRON_PICKAXE};
                    List<Slot> hasIronArmor = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                            COLLECT_IRON_ARMOR);
                    List<Slot> hasThrowGear = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                            throwGear);
                    if (!StorageHelper.isBigCraftingOpen() && !StorageHelper.isFurnaceOpen() &&
                            !StorageHelper.isSmokerOpen() && !StorageHelper.isBlastFurnaceOpen() &&
                            (mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL) ||
                                    mod.getItemStorage().hasItem(Items.FIRE_CHARGE))) {
                        if (!hasThrowGear.isEmpty()) {
                            Slot throwGearInSlot = hasThrowGear.get(0);
                            if (Slot.isCursor(throwGearInSlot)) {
                                if (!mod.getControllerExtras().isBreakingBlock()) {
                                    LookHelper.randomOrientation(mod);
                                }
                                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                            } else {
                                mod.getSlotHandler().clickSlot(throwGearInSlot, 0, SlotActionType.PICKUP);
                            }
                        }
                        if (!hasIronArmor.isEmpty()) {
                            Slot ironArmorInSlot = hasIronArmor.get(0);
                            if (Slot.isCursor(ironArmorInSlot)) {
                                if (!mod.getControllerExtras().isBreakingBlock()) {
                                    LookHelper.randomOrientation(mod);
                                }
                                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                            } else {
                                mod.getSlotHandler().clickSlot(ironArmorInSlot, 0, SlotActionType.PICKUP);
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
                    _ranStrongholdLocator = true;
                    // Get beds before starting our portal location.
                    if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD && needsBeds(mod)) {
                        setDebugState("Getting beds before stronghold search.");
                        if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                            timer1++;
                            if (timer1 >= 500) {
                                if (_config.renderDistanceManipulation) {
                                    MinecraftClient.getInstance().options.getViewDistance().setValue(32);
                                    MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(5.0);
                                }
                                timer1 = 0;
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
                Item[] throwGear = {Items.STONE_SWORD, Items.STONE_PICKAXE, Items.IRON_SWORD, Items.IRON_PICKAXE};
                List<Slot> hasIronArmor = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                        COLLECT_IRON_ARMOR);
                List<Slot> hasThrowGear = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                        throwGear);
                if (!StorageHelper.isBigCraftingOpen() && !StorageHelper.isFurnaceOpen() &&
                        !StorageHelper.isSmokerOpen() && !StorageHelper.isBlastFurnaceOpen() &&
                        (mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL) ||
                                mod.getItemStorage().hasItem(Items.FIRE_CHARGE))) {
                    if (!hasThrowGear.isEmpty()) {
                        Slot throwGearInSlot = hasThrowGear.get(0);
                        if (Slot.isCursor(throwGearInSlot)) {
                            if (!mod.getControllerExtras().isBreakingBlock()) {
                                LookHelper.randomOrientation(mod);
                            }
                            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                        } else {
                            mod.getSlotHandler().clickSlot(throwGearInSlot, 0, SlotActionType.PICKUP);
                        }
                    }
                    if (!hasIronArmor.isEmpty()) {
                        Slot ironArmorInSlot = hasIronArmor.get(0);
                        if (Slot.isCursor(ironArmorInSlot)) {
                            if (!mod.getControllerExtras().isBreakingBlock()) {
                                LookHelper.randomOrientation(mod);
                            }
                            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                        } else {
                            mod.getSlotHandler().clickSlot(ironArmorInSlot, 0, SlotActionType.PICKUP);
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

    private Task setSpawnNearPortalTask(AltoClef mod) {
        if (_setBedSpawnTask.isSpawnSet()) {
            _bedSpawnLocation = _setBedSpawnTask.getBedSleptPos();
        } else {
            _bedSpawnLocation = null;
        }
        if (shouldForce(mod, _setBedSpawnTask)) {
            // Set spawnpoint and set our bed spawn when it happens.
            setDebugState("Setting spawnpoint now.");
            return _setBedSpawnTask;
        }
        // Get close to portal. If we're close enough, set our bed spawn somewhere nearby.
        if (WorldHelper.inRangeXZ(mod.getPlayer(), WorldHelper.toVec3d(_endPortalCenterLocation), END_PORTAL_BED_SPAWN_RANGE)) {
            return _setBedSpawnTask;
        } else {
            setDebugState("Approaching portal (to set spawnpoint)");
            return new GetToXZTask(_endPortalCenterLocation.getX(), _endPortalCenterLocation.getZ());
        }
    }

    private Task getBlazeRodsTask(AltoClef mod, int count) {
        isGettingBlazeRods = true;
        if (mod.getEntityTracker().itemDropped(Items.BLAZE_ROD)) {
            return new PickupDroppedItemTask(Items.BLAZE_ROD, 1);
        }
        if (mod.getEntityTracker().itemDropped(Items.BLAZE_POWDER)) {
            return new PickupDroppedItemTask(Items.BLAZE_POWDER, 1);
        }
        return new CollectBlazeRodsTask(count);
    }

    private Task getEnderPearlTask(AltoClef mod, int count) {
        isGettingEnderPearls = true;
        if (shouldForce(mod, getTwistingVines)) {
            setDebugState("Getting twisting vines for MLG purposes.");
            return getTwistingVines;
        }
        if (mod.getEntityTracker().itemDropped(Items.ENDER_PEARL)) {
            return new PickupDroppedItemTask(Items.ENDER_PEARL, 1);
        }
        if (_config.barterPearlsInsteadOfEndermanHunt) {
            // Equip golden boots before trading...
            if (!StorageHelper.isArmorEquipped(mod, Items.GOLDEN_BOOTS)) {
                return new EquipArmorTask(Items.GOLDEN_BOOTS);
            }
            return new TradeWithPiglinsTask(32, Items.ENDER_PEARL, count);
        } else {
            if ((mod.getEntityTracker().entityFound(EndermanEntity.class) ||
                    mod.getEntityTracker().itemDropped(Items.ENDER_PEARL)) &&
                    mod.getItemStorage().getItemCount(Items.TWISTING_VINES) > 14) {
                Optional<Entity> toKill = mod.getEntityTracker().getClosestEntity(EndermanEntity.class);
                if (toKill.isPresent()) {
                    if (mod.getEntityTracker().isEntityReachable(toKill.get())) {
                        return new KillAndLootTask(toKill.get().getClass(), new ItemTarget(Items.ENDER_PEARL, count));
                    }
                }
            }
            if (mod.getItemStorage().getItemCount(Items.TWISTING_VINES) < 14) {
                getTwistingVines = TaskCatalogue.getItemTask(Items.TWISTING_VINES, 28);
                return getTwistingVines;
            }
            // Search for warped forests this way...
            setDebugState("Searching Warped Forest");
            return new SearchWithinBiomeTask(BiomeKeys.WARPED_FOREST);
        }
    }

    private int getTargetBeds(AltoClef mod) {
        boolean needsToSetSpawn = _config.placeSpawnNearEndPortal &&
                (
                        !spawnSetNearPortal(mod, _endPortalCenterLocation)
                                && !shouldForce(mod, _setBedSpawnTask)
                );
        int bedsInEnd = 0;
        for (Item bed : ItemHelper.BED) {
            bedsInEnd += _cachedEndItemDrops.getOrDefault(bed, 0);
        }

        return _config.requiredBeds + (needsToSetSpawn ? 1 : 0) - bedsInEnd;
    }

    private boolean needsBeds(AltoClef mod) {
        int inEnd = 0;
        for (Item item : ItemHelper.BED) {
            inEnd += _cachedEndItemDrops.getOrDefault(item, 0);
        }
        return (mod.getItemStorage().getItemCount(ItemHelper.BED) + inEnd) < getTargetBeds(mod);
    }

    private Task getBedTask(AltoClef mod) {
        int targetBeds = getTargetBeds(mod);
        // Collect beds. If we want to set our spawn, collect 1 more.
        if (!mod.getItemStorage().hasItem(Items.SHEARS) && !anyBedsFound(mod)) {
            return TaskCatalogue.getItemTask(Items.SHEARS, 1);
        }
        return TaskCatalogue.getItemTask("bed", targetBeds);
    }

    private boolean anyBedsFound(AltoClef mod) {
        return mod.getBlockTracker().anyFound(ItemHelper.itemsToBlocks(ItemHelper.BED)) ||
                mod.getEntityTracker().itemDropped(ItemHelper.BED);
    }

    private BlockPos doSimpleSearchForEndPortal(AltoClef mod) {
        List<BlockPos> frames = mod.getBlockTracker().getKnownLocations(Blocks.END_PORTAL_FRAME);
        if (frames.size() >= END_PORTAL_FRAME_COUNT) {
            // Get the center of the frames.
            Vec3d average = frames.stream()
                    .reduce(Vec3d.ZERO, (accum, bpos) -> accum.add(bpos.getX() + 0.5, bpos.getY() + 0.5, bpos.getZ() + 0.5), Vec3d::add)
                    .multiply(1.0f / frames.size());
            return new BlockPos(average.x, average.y, average.z);
        }
        return null;
    }

    private int getFilledPortalFrames(AltoClef mod, BlockPos endPortalCenter) {
        // If we have our end portal, this doesn't matter.
        if (endPortalFound(mod, endPortalCenter)) {
            return END_PORTAL_FRAME_COUNT;
        }
        if (endPortalFound(mod, endPortalCenter)) {
            List<BlockPos> frameBlocks = getFrameBlocks(endPortalCenter);
            // If EVERY portal frame is loaded, consider updating our cached filled portal count.
            if (frameBlocks.stream().allMatch(blockPos -> mod.getChunkTracker().isChunkLoaded(blockPos))) {
                _cachedFilledPortalFrames = frameBlocks.stream().reduce(0, (count, blockPos) ->
                                count + (isEndPortalFrameFilled(mod, blockPos) ? 1 : 0),
                        Integer::sum);
            }
            return _cachedFilledPortalFrames;
        }
        return 0;
    }

    private boolean canBeLootablePortalChest(AltoClef mod, BlockPos blockPos) {
        if (mod.getWorld().getBlockState(blockPos.up(1)).getBlock() == Blocks.WATER || blockPos.getY() < 50) {
            return false;
        }
        for (BlockPos check : WorldHelper.scanRegion(mod, blockPos.add(-4, -2, -4), blockPos.add(4, 2, 4))) {
            if (mod.getWorld().getBlockState(check).getBlock() == Blocks.NETHERRACK) {
                return true;
            }
        }
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
                            timer1++;
                            if (timer1 >= 500) {
                                MinecraftClient.getInstance().options.getViewDistance().setValue(2);
                                MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(0.5);
                                timer1 = 0;
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
                    setDebugState("Getting porkchop just for fun.");
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
                            timer1++;
                            if (timer1 >= 500) {
                                MinecraftClient.getInstance().options.getViewDistance().setValue(2);
                                MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(0.5);
                                timer1 = 0;
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
                        _getPorkchopTask = new KillAndLootTask(PigEntity.class, new
                                ItemTarget(Items.PORKCHOP, 1));
                        return _getPorkchopTask;
                    } else {
                        _getPorkchopTask = null;
                    }
                    setDebugState("Searching a better place to start with.");
                    if (_config.renderDistanceManipulation) {
                        if (!mod.getClientBaritone().getExploreProcess().isActive()) {
                            timer1++;
                            if (timer1 >= 500) {
                                MinecraftClient.getInstance().options.getViewDistance().setValue(32);
                                MinecraftClient.getInstance().options.getEntityDistanceScaling().setValue(5.0);
                                timer1 = 0;
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
                if (_config.getShield && !shieldSatisfied && !mod.getFoodChain().isTryingToEat()) {
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
                    Item[] throwGear = {Items.STONE_SWORD, Items.STONE_PICKAXE, Items.IRON_SWORD, Items.IRON_PICKAXE};
                    List<Slot> hasIronArmor = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                            COLLECT_IRON_ARMOR);
                    List<Slot> hasThrowGear = mod.getItemStorage().getSlotsWithItemPlayerInventory(true,
                            throwGear);
                    if (!StorageHelper.isBigCraftingOpen() && !StorageHelper.isFurnaceOpen() &&
                            !StorageHelper.isSmokerOpen() && !StorageHelper.isBlastFurnaceOpen() &&
                            (mod.getItemStorage().hasItem(Items.FLINT_AND_STEEL) ||
                                    mod.getItemStorage().hasItem(Items.FIRE_CHARGE))) {
                        if (!hasThrowGear.isEmpty()) {
                            Slot throwGearInSlot = hasThrowGear.get(0);
                            if (Slot.isCursor(throwGearInSlot)) {
                                if (!mod.getControllerExtras().isBreakingBlock()) {
                                    LookHelper.randomOrientation(mod);
                                }
                                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                            } else {
                                mod.getSlotHandler().clickSlot(throwGearInSlot, 0, SlotActionType.PICKUP);
                            }
                        }
                        if (!hasIronArmor.isEmpty()) {
                            Slot ironArmorInSlot = hasIronArmor.get(0);
                            if (Slot.isCursor(ironArmorInSlot)) {
                                if (!mod.getControllerExtras().isBreakingBlock()) {
                                    LookHelper.randomOrientation(mod);
                                }
                                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                            } else {
                                mod.getSlotHandler().clickSlot(ironArmorInSlot, 0, SlotActionType.PICKUP);
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
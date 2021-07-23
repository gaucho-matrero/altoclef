package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.GetToYTask;
import adris.altoclef.tasks.chest.PickupFromChestTask;
import adris.altoclef.tasks.chest.StoreInAnyChestTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.construction.compound.ConstructNetherPortalBucketTask;
import adris.altoclef.tasks.misc.EnterNetherPortalTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.misc.PlaceBedAndSetSpawnTask;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasks.resources.KillAndLootTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.util.*;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the big kahoona. Plays the whole game.
 */
public class BeatMinecraftTask extends Task {

    /// TUNABLE PROPERTIES
    private static final String[] DIAMOND_ARMORS = new String[]{"diamond_chestplate", "diamond_leggings", "diamond_helmet", "diamond_boots"};

    private static final boolean STORE_BLAZE_RODS_IN_CHEST = true; // If true, will store blaze rods in chest while collecting ender pearls.

    private static final int PRE_NETHER_FOOD = 5 * 40;
    private static final int PRE_NETHER_FOOD_MIN = 5 * 20;
    private static final int PRE_END_FOOD = 5 * 20;
    private static final int PRE_END_FOOD_MIN = 5 * 3;

    private static final int TARGET_BLAZE_RODS = 7;
    private static final int TARGET_ENDER_PEARLS = 14;
    private static final int TARGET_ENDER_EYES = 14;
    private static final int PIGLIN_BARTER_GOLD_INGOT_BUFFER = 32;
    private final CollectBlazeRodsTask _blazeCollection = new CollectBlazeRodsTask(TARGET_BLAZE_RODS);
    private final LocateStrongholdTask _strongholdLocater = new LocateStrongholdTask(TARGET_ENDER_EYES);
    // Get 3 diamond picks, because the nether SUCKS
    private final Task _prepareEquipmentTask = TaskCatalogue.getSquashedItemTask(
            new ItemTarget("diamond_chestplate", 1),
            new ItemTarget("diamond_leggings", 1),
            new ItemTarget("diamond_helmet", 1),
            new ItemTarget("diamond_boots", 1),
            new ItemTarget("diamond_pickaxe", 3),
            new ItemTarget("diamond_sword", 1),
            new ItemTarget("log", 20)
    );
    private final Task _prepareForDiamondCollectionTask = TaskCatalogue.getSquashedItemTask(
            new ItemTarget("iron_pickaxe", 3)
    );
    private final Task _netherPrepareTaskJustPick = TaskCatalogue.getItemTask("wooden_pickaxe", 1);
    private final Task _netherPrepareTaskWood = TaskCatalogue.getSquashedItemTask(
            new ItemTarget("wooden_pickaxe", 1),
            new ItemTarget("log", 10)
    );
    private final PlaceBedAndSetSpawnTask _placeBedSpawnTask = new PlaceBedAndSetSpawnTask();
    // Kinda jank ngl
    private final Task _collectBuildMaterialsTask = PlaceBlockTask.getMaterialTask(100);
    private BlockPos _safetyBlazeRodChestPos = null;
    // A flag to determine whether we should continue doing something.
    // TODO: I handled this poorly. Either LEAN INTO it hard or throw this out completely.
    private ForceState _forceState = ForceState.NONE;
    private BlockPos _cachedPortalInNether;
    private BlockPos _netherPortalPos;
    // End game stuff
    private BlockPos _endBedSpawnPos = null;
    // If true, we were near the end portal, don't do traveling.
    private boolean _wasNearEndPortal = false;
    private List<BlockPos> _endPortalFrame = null;
    private BlockPos _cachedEndPortal = null;
    private int _cachedEndPearlsInFrame = 0;
    private Dimension _prevDimension = Dimension.OVERWORLD;

    public static boolean diamondArmorEquipped(AltoClef mod) {
        for (String armor : DIAMOND_ARMORS) {
            //noinspection ConstantConditions
            if (!mod.getInventoryTracker().isArmorEquipped(TaskCatalogue.getItemMatches(armor)[0])) return false;
        }
        return true;
    }

    public static boolean hasDiamondArmor(AltoClef mod) {
        for (String armor : DIAMOND_ARMORS) {
            //noinspection ConstantConditions
            Item item = TaskCatalogue.getItemMatches(armor)[0];
            if (mod.getInventoryTracker().isArmorEquipped(item)) continue;
            if (!mod.getInventoryTracker().hasItem(item)) return false;
        }
        return true;
    }

    public static boolean isEndPortalFrameFilled(AltoClef mod, BlockPos pos) {
        if (!mod.getChunkTracker().isChunkLoaded(pos)) return false;
        BlockState state = mod.getWorld().getBlockState(pos);
        if (state.getBlock() != Blocks.END_PORTAL_FRAME) {
            Debug.logWarning("BLOCK POS " + pos + " DOES NOT CONTAIN END PORTAL FRAME! This is probably due to a bug/incorrect assumption.");
        }
        return state.get(EndPortalFrameBlock.EYE);
    }

    @Override
    protected void onStart(AltoClef mod) {
        _forceState = ForceState.NONE;
        mod.getBehaviour().push();
        // Add some protections so we don't throw these away at any point.
        mod.getBehaviour().addProtectedItems(Items.ENDER_EYE, Items.BLAZE_ROD, Items.ENDER_PEARL, Items.DIAMOND);
        mod.getBehaviour().addProtectedItems(ItemUtil.BED);

        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL);
        // Allow walking on end portal
        mod.getBehaviour().allowWalkingOn(blockPos -> mod.getChunkTracker().isChunkLoaded(blockPos) && mod.getWorld().getBlockState(blockPos).getBlock() == Blocks.END_PORTAL);

        // Dodge ALL projectiles in the end.
        mod.getBehaviour().avoidDodgingProjectile(proj -> mod.getCurrentDimension() == Dimension.END);

        // Don't break blocks around our bed, can lead to problems.
        mod.getBehaviour().avoidBlockBreaking(pos -> {
            if (_endBedSpawnPos != null) {
                return _endBedSpawnPos.isWithinDistance(pos, 2);
            }
            return false;
        });
    }

    @Override
    protected Task onTick(AltoClef mod) {
        /*
         * ROUGH PLAN:
         * 1) Get full diamond armor
         * 2) Get lots of food
         * 3) Get to nether
         * 4) Find blaze spawner
         * 5) Kill blazes
         * 6) Get pearls (trade or enderman)
         * 7) Find stronghold + place spawn nearby
         * 8) Destroy all crystals individually
         * 9) Punk dragon till defeat
         */

        // do NOT diagonally ascend in the nether.
        Dimension currentDimension = mod.getCurrentDimension();
        if (currentDimension != _prevDimension) {
            mod.getBehaviour().setAllowDiagonalAscend(currentDimension != Dimension.NETHER);
            _prevDimension = currentDimension;
        }

        switch (currentDimension) {
            case OVERWORLD:
                return overworldTick(mod);
            case NETHER:
                return netherTick(mod);
            case END:
                return endTick(mod);
        }
        throw new IllegalStateException("Shouldn't ever happen.");
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return MinecraftClient.getInstance().currentScreen instanceof CreditsScreen;
    }

    private Task overworldTick(AltoClef mod) {

        int eyes = mod.getInventoryTracker().getItemCountIncludingTable(Items.ENDER_EYE) + portalEyesInFrame(mod);
        int rodsNeeded = TARGET_BLAZE_RODS - (mod.getInventoryTracker().getItemCountIncludingTable(Items.BLAZE_POWDER) + eyes) / 2;
        int rodsInPosession = getBlazeRodsInPosession(mod);

        //int pearlsNeeded = TARGET_ENDER_PEARLS - eyes;
        boolean needsToGoToNether = rodsInPosession < rodsNeeded;// || mod.getInventoryTracker().getItemCountIncludingTable(Items.ENDER_PEARL) < pearlsNeeded;

        if (STORE_BLAZE_RODS_IN_CHEST && !isEndPortalOpened(mod)) {
            // If we have a place with blaze rods, set our "safety" position to that chest.
            if (_safetyBlazeRodChestPos == null) {
                List<BlockPos> blazeContainers = mod.getContainerTracker().getChestMap().getBlocksWithItem(Items.BLAZE_ROD);
                if (blazeContainers.size() > 0) {
                    _safetyBlazeRodChestPos = blazeContainers.get(0);
                    Debug.logMessage("Blaze rods stored at " + _safetyBlazeRodChestPos);
                }
            }
        }

        if (!isEndPortalOpened(mod)) {

            if (_prepareForDiamondCollectionTask.isActive() && !_prepareForDiamondCollectionTask.isFinished(mod)) {
                setDebugState("Collecting extra iron gear before getting diamonds.");
                return _prepareForDiamondCollectionTask;
            }

            // Equip diamond armor asap
            if (hasDiamondArmor(mod) && !diamondArmorEquipped(mod)) {
                return new EquipArmorTask(DIAMOND_ARMORS);
            }
            // Get diamond armor + gear first
            if (!hasDiamondArmor(mod) || !mod.getInventoryTracker().hasItem(Items.DIAMOND_PICKAXE) || !mod.getInventoryTracker().hasItem(Items.DIAMOND_SWORD) || (needsToGoToNether && mod.getInventoryTracker().getItemCountIncludingTable(ItemUtil.LOG) <= 0)) {
                // Get two iron pickaxes first.
                double ironDurability = 0;
                if (mod.getInventoryTracker().getItemCount(Items.IRON_PICKAXE) >= 3) {
                    ironDurability = 1;
                } else {
                    for (int invslot : mod.getInventoryTracker().getInventorySlotsWithItem(Items.IRON_PICKAXE)) {
                        ItemStack stack = mod.getInventoryTracker().getItemStackInSlot(Slot.getFromInventory(invslot));
                        ironDurability += 1 - ((double) stack.getDamage() / (double) stack.getMaxDamage());
                    }
                }
                // ALWAYS reserve 0.5 iron pickaxes for leaving.
                if (ironDurability < 0.5 && !mod.getInventoryTracker().hasItem(Items.DIAMOND_PICKAXE)) {
                    return _prepareForDiamondCollectionTask;
                }

                setDebugState("Getting equipment");
                return _prepareEquipmentTask;
            }
        }

        // Stronghold portal located.
        if (endPortalLocated() || isEndPortalOpened(mod)) {

            // Reset close to end on death.
            if (MinecraftClient.getInstance().currentScreen instanceof DeathScreen) {
                _wasNearEndPortal = false;
            }

            if (endPortalChunksLoaded(mod) || _wasNearEndPortal) {
                _wasNearEndPortal = true;
                int eyesNeeded = 12 - portalEyesInFrame(mod);
                if (isEndPortalOpened(mod) || mod.getInventoryTracker().getItemCount(Items.ENDER_EYE) >= eyesNeeded) {
                    // We have ENOUGH Eyes OR the portal is open.
                    return foundPortalGetToEndTask(mod);
                }
            } else if (isEndPortalOpened(mod)) {
                // Portal is not loaded but is open, go to portal
                setDebugState("Going back to end portal...");
                return new GetToBlockTask(_cachedEndPortal, false);
            }
        }

        // Locate stronghold portal if we can.
        if (!endPortalLocated()) {
            if (mod.getInventoryTracker().getItemCountIncludingTable(Items.ENDER_EYE) > 1 && (_strongholdLocater.isActive() || _strongholdLocater.isSearching()) && !_strongholdLocater.isFinished(mod)) {
                setDebugState("Locating end portal.");
                return _strongholdLocater;
            } else {
                if (!endPortalLocated() && _strongholdLocater.portalFound()) {
                    Debug.logMessage("Now we have our portal position.");
                    _endPortalFrame = _strongholdLocater.getPortalFrame();
                }
            }
            if (!endPortalLocated() && mod.getInventoryTracker().getItemCount(Items.ENDER_EYE) >= TARGET_ENDER_EYES) {
                return _strongholdLocater;
            }
        }


        // Get food, less if we're going to the end.
        int preFood = needsToGoToNether ? PRE_NETHER_FOOD : PRE_END_FOOD,
                preFoodMin = needsToGoToNether ? PRE_NETHER_FOOD_MIN : PRE_END_FOOD_MIN;
        if (mod.getInventoryTracker().totalFoodScore() < preFoodMin) {
            _forceState = ForceState.GETTING_FOOD;
        }
        if (_forceState == ForceState.GETTING_FOOD) {
            if (mod.getInventoryTracker().totalFoodScore() < preFood) {
                setDebugState("Getting food");
                return new CollectFoodTask(preFood);
            } else {
                _forceState = ForceState.NONE;
            }
        }

        if (!isEndPortalOpened(mod)) {
            // Get blaze rods by going to nether
            if (needsToGoToNether) {
                //Debug.logInternal(mod.getInventoryTracker().getItemCount(Items.ENDER_PEARL) + "< " + TARGET_ENDER_PEARLS + " : " + mod.getInventoryTracker().getItemCount(Items.BLAZE_ROD) + " < " + rodsNeeded);
                // Go to nether
                if (_netherPortalPos != null) {
                    if (mod.getBlockTracker().isTracking(Blocks.NETHER_PORTAL)) {
                        if (!mod.getBlockTracker().blockIsValid(_netherPortalPos, Blocks.NETHER_PORTAL)) {
                            double MAX_PORTAL_DISTANCE = 2000;
                            // Reset portal if it's far away or we confirmed it being incorrect in this chunk.
                            if (mod.getChunkTracker().isChunkLoaded(_netherPortalPos) || _netherPortalPos.getSquaredDistance(mod.getPlayer().getPos(), false) > MAX_PORTAL_DISTANCE * MAX_PORTAL_DISTANCE) {
                                Debug.logMessage("Invalid portal position detected at " + _netherPortalPos + ", finding new nether portal.");
                                _netherPortalPos = null;
                            }
                        }
                    }
                    if (_netherPortalPos != null) {
                        setDebugState("Going to previously created nether portal...");
                        return new EnterNetherPortalTask(new GetToBlockTask(_netherPortalPos, false), Dimension.NETHER);
                    }
                } else {
                    if (mod.getBlockTracker().isTracking(Blocks.NETHER_PORTAL)) {
                        if (mod.getBlockTracker().anyFound(Blocks.NETHER_PORTAL)) {
                            _netherPortalPos = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), Blocks.NETHER_PORTAL);
                            Debug.logMessage("Tracked portal at " + _netherPortalPos);
                        }
                    }
                }

                setDebugState("Build nether portal + go to nether");
                return new EnterNetherPortalTask(new ConstructNetherPortalBucketTask(), Dimension.NETHER);
            } else {
                int targetEyes = TARGET_ENDER_EYES;
                int targetPearls = TARGET_ENDER_PEARLS;
                if (endPortalLocated()) {
                    targetEyes = 12;
                    targetPearls = targetEyes;
                }
                // eyes already accounts for portal eyes in frame.
                int pearlsNeeded = targetPearls - eyes;
                if (mod.getInventoryTracker().getItemCountIncludingTable(Items.ENDER_PEARL) < pearlsNeeded) {
                    if (STORE_BLAZE_RODS_IN_CHEST) {
                        // Chest handling: Store blaze rods in a chest if we have any.
                        if (mod.getInventoryTracker().hasItem(Items.BLAZE_ROD)) {
                            setDebugState("Storing Blaze rods (for death safety)");
                            return new StoreInAnyChestTask(new ItemTarget(Items.BLAZE_ROD, mod.getInventoryTracker().getItemCount(Items.BLAZE_ROD)));
                        }
                    }
                    setDebugState("Collecting ender pearls");
                    return new KillAndLootTask(EndermanEntity.class, new ItemTarget(Items.ENDER_PEARL, pearlsNeeded));
                }
                setDebugState("Crafting our blaze powder + eyes");
                int powderNeeded = (targetEyes - eyes);
                if (mod.getInventoryTracker().getItemCount(Items.BLAZE_POWDER) < powderNeeded) {
                    if (STORE_BLAZE_RODS_IN_CHEST) {
                        // Chest handling: Grab blaze rods if we don't have enough.
                        if (mod.getInventoryTracker().getItemCountIncludingTable(Items.BLAZE_ROD) < TARGET_BLAZE_RODS && _safetyBlazeRodChestPos != null) {
                            int needed = TARGET_BLAZE_RODS - mod.getInventoryTracker().getItemCountIncludingTable(Items.BLAZE_ROD);
                            setDebugState("Picking up stored blaze rods");
                            return new PickupFromChestTask(_safetyBlazeRodChestPos, new ItemTarget(Items.BLAZE_ROD, needed));
                        }
                    }
                    return new CraftInInventoryTask(new ItemTarget(Items.BLAZE_POWDER, powderNeeded), CraftingRecipe.newShapedRecipe("blaze_powder",
                            new ItemTarget[]{new ItemTarget(Items.BLAZE_ROD, 1), null, null, null}, 2));
                }
                // Craft
                return new CraftInInventoryTask(new ItemTarget(Items.ENDER_EYE, targetEyes), CraftingRecipe.newShapedRecipe("ender_eye",
                        new ItemTarget[]{new ItemTarget(Items.ENDER_PEARL, 1), new ItemTarget(Items.BLAZE_POWDER, 1), null, null}, 1));
            }
        }
        // The end portal is opened. Ummmmm... We shouldn't be here.
        Debug.logError("THIS SHOULDN'T HAPPEN OH NO");
        return null;
    }

    private Task netherTick(AltoClef mod) {

        // Keep track of our portal so we may return to it.
        if (_cachedPortalInNether == null) {
            _cachedPortalInNether = mod.getPlayer().getBlockPos();
        }

        // Collect tools while we're here

        if (_netherPrepareTaskWood.isActive() && !_netherPrepareTaskWood.isFinished(mod)) {
            //return _netherPrepareTaskWood;
        }
        assert _netherPrepareTaskJustPick != null;
        if (_netherPrepareTaskJustPick.isActive() && !_netherPrepareTaskJustPick.isFinished(mod)) {
            //return _netherPrepareTaskJustPick;
        }

        // Make sure we have at least a wooden pickaxe at all times
        // AND materials to craft a new one, so we aren't stuck in a cavern somewhere.
        int planksCount = 4 * mod.getInventoryTracker().getItemCount(ItemUtil.LOG) + mod.getInventoryTracker().getItemCount(ItemUtil.PLANKS);
        int planksNeeded = 3 + (mod.getInventoryTracker().hasItem(Items.CRAFTING_TABLE) ? 0 : 4) + (mod.getInventoryTracker().getItemCount(Items.STICK) >= 2 ? 0 : 2);
        if (!mod.getInventoryTracker().miningRequirementMet(MiningRequirement.WOOD) || planksCount < planksNeeded) {
            // If we ran out of wood, go get more.
            if (planksCount >= planksNeeded) {
                //return _netherPrepareTaskJustPick;
            } else {
                //return _netherPrepareTaskWood;
            }
        }

        // Blaze rods
        int powderCount = mod.getInventoryTracker().getItemCountIncludingTable(Items.BLAZE_POWDER) + getBlazeRodsInPosession(mod) * 2;
        if (powderCount < TARGET_BLAZE_RODS * 2) {
            setDebugState("Collecting Blaze Rods");
            return _blazeCollection;
        }

        setDebugState("Getting the hell out of here");
        return new EnterNetherPortalTask(new GetToBlockTask(_cachedPortalInNether, false), Dimension.OVERWORLD);
    }

    private Task endTick(AltoClef mod) {

        setDebugState("Defeating the ender dragon.");
        return new KillEnderDragonTask();
    }

    // Code duplication below, kinda bad but I'm in a hurry lol
    private boolean needToGetMaterialsBeforeEnd(AltoClef mod) {
        return !mod.getInventoryTracker().miningRequirementMet(MiningRequirement.IRON) || !mod.getInventoryTracker().hasItem(Items.IRON_SWORD, Items.DIAMOND_SWORD) || !mod.getInventoryTracker().hasItem(Items.WATER_BUCKET);
    }

    private Task getMaterialsBeforeEndTask(AltoClef mod) {
        List<ItemTarget> toGet = new ArrayList<>();
        if (!mod.getInventoryTracker().hasItem(Items.IRON_SWORD, Items.DIAMOND_SWORD)) {
            toGet.add(new ItemTarget("iron_sword", 1));
        }
        if (!mod.getInventoryTracker().hasItem(Items.BUCKET, Items.WATER_BUCKET)) {
            toGet.add(new ItemTarget("bucket", 1));
        }
        if (!mod.getInventoryTracker().miningRequirementMet(MiningRequirement.IRON)) {
            toGet.add(new ItemTarget("iron_pickaxe", 1));
        }
        if (toGet.size() != 0) {
            return TaskCatalogue.getSquashedItemTask(Util.toArray(ItemTarget.class, toGet));//new SatisfyMiningRequirementTask(MiningRequirement.IRON);
        }
        // Collect water if we don't have it.
        if (!mod.getInventoryTracker().hasItem(Items.WATER_BUCKET)) {
            return TaskCatalogue.getItemTask("water_bucket", 1);
        }
        return null;
    }

    private Task foundPortalGetToEndTask(AltoClef mod) {

        // Make sure we have some TOOLS for navigation in end.
        if (needToGetMaterialsBeforeEnd(mod)) {
            setDebugState("Getting supplies before going to end");
            return getMaterialsBeforeEndTask(mod);
        }

        if (_endBedSpawnPos != null) {
            if (mod.getChunkTracker().isChunkLoaded(_endBedSpawnPos)) {
                if (!mod.getBlockTracker().blockIsValid(_endBedSpawnPos, Util.itemsToBlocks(ItemUtil.BED))) {
                    Debug.logMessage("BED DESTRUCTION DETECTED: Will assume we need to place a new one.");
                    _endBedSpawnPos = null;
                    _placeBedSpawnTask.resetSleep();
                }
            }
        }

        if (_endBedSpawnPos == null) {
            boolean closeEnoughToPortal = false;
            if (_endPortalFrame.size() != 0) {
                closeEnoughToPortal = _endPortalFrame.get(0).isWithinDistance(mod.getPlayer().getPos(), 200);
            }
            if (_placeBedSpawnTask.isFinished(mod)) {
                _endBedSpawnPos = _placeBedSpawnTask.getBedSleptPos();
            }
            if (_endBedSpawnPos == null && _placeBedSpawnTask.isActive() && !_placeBedSpawnTask.isFinished(mod)) {
                return _placeBedSpawnTask;
            }
            if (_endBedSpawnPos == null && closeEnoughToPortal) {
                setDebugState("Trying to set spawnpoint closer to end portal");
                // Lock in to placing a bed.

                // If we're too low, move up so we're near the surface.
                if (mod.getPlayer().getPos().y < 63) {
                    return new GetToYTask(64);
                }
                return _placeBedSpawnTask;
            }
        } else {
            // Make sure we have BUILDING supplies.
            int MINIMUM_BUILDING_BLOCKS = 32;
            if (mod.getInventoryTracker().getItemCount(Items.DIRT, Items.COBBLESTONE, Items.NETHERRACK) < MINIMUM_BUILDING_BLOCKS && _collectBuildMaterialsTask.isActive() && !_collectBuildMaterialsTask.isFinished(mod)) {
                mod.getBehaviour().addProtectedItems(Items.DIRT, Items.COBBLESTONE);
                setDebugState("Getting building materials before going to end.");
                return _collectBuildMaterialsTask;
            } else {
                mod.getBehaviour().removeProtectedItems(Items.DIRT, Items.COBBLESTONE);
            }
        }

        // If the end portal is lit. Assume kamakazee mode, and enter the end portal.
        BlockPos nearestPortal = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), Blocks.END_PORTAL);
        boolean endPortalLit = (nearestPortal != null);
        if (endPortalLit) {
            _cachedEndPortal = nearestPortal;

            setDebugState("ENTERING PORTAL");
            return new DoToClosestBlockTask(
                    () -> mod.getPlayer().getPos(),
                    blockPos -> new GetToBlockTask(blockPos.up(), true),
                    pos -> mod.getBlockTracker().getNearestTracking(pos, Blocks.END_PORTAL),
                    Blocks.END_PORTAL
            );
        }

        setDebugState("Filling stronghold portal");
        // End portal is not lit
        return new FillStrongholdPortalTask(true);
    }

    private int getBlazeRodsInPosession(AltoClef mod) {
        int rodsInPosession = mod.getInventoryTracker().getItemCountIncludingTable(Items.BLAZE_ROD);
        if (STORE_BLAZE_RODS_IN_CHEST) {
            if (_safetyBlazeRodChestPos != null) {
                ContainerTracker.ChestData chest = mod.getContainerTracker().getChestMap().getCachedChestData(_safetyBlazeRodChestPos);
                if (chest == null || !chest.hasItem(Items.BLAZE_ROD)) {
                    Debug.logMessage("Blaze Rod Chest no longer has any rods: " + _safetyBlazeRodChestPos + ".");
                    _safetyBlazeRodChestPos = null;
                } else {
                    rodsInPosession += chest.getItemCount(Items.BLAZE_ROD);
                }
            }
        }
        return rodsInPosession;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Most likely we have failed or cancelled at this point.
        // But one day this will actually trigger after the game is completed. Just you wait.
        mod.getBehaviour().pop();
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL);
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof BeatMinecraftTask;
    }

    @Override
    protected String toDebugString() {
        return "Beating the game";
    }

    private boolean endPortalLocated() {
        return _endPortalFrame != null;
    }

    private boolean isEndPortalOpened(AltoClef mod) {
        return _cachedEndPortal != null;
    }

    private boolean endPortalChunksLoaded(AltoClef mod) {
        if (endPortalLocated()) {
            for (BlockPos frame : _endPortalFrame) {
                if (!mod.getChunkTracker().isChunkLoaded(frame)) return false;
            }
        }
        return true;
    }

    private int portalEyesInFrame(AltoClef mod) {
        int count = 0;
        if (endPortalLocated()) {
            for (BlockPos b : _endPortalFrame) {
                if (!mod.getChunkTracker().isChunkLoaded(b)) {
                    return _cachedEndPearlsInFrame;
                }
                boolean filled = isEndPortalFrameFilled(mod, b);
                if (filled) {
                    count++;
                }
            }
            _cachedEndPearlsInFrame = count;
        }
        return _cachedEndPearlsInFrame;
    }

    private enum ForceState {
        NONE,
        GETTING_DIAMOND_GEAR,
        GETTING_FOOD
    }
}

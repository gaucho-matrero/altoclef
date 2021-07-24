package adris.altoclef.tasks.misc.anarchysurvive;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.misc.EnterNetherPortalTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.misc.PlaceBedAndSetSpawnTask;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ArmorRequirement;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.ActionListener;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;

import java.util.function.Predicate;

public abstract class AnarchyBaseSurvivalTask extends Task {

    private final boolean _avoidOceans;
    private final float _radiusIncreasePerOcean;
    private final float _dangerFoodAmount;
    private final float _axisDangerDistance;
    private final float _axisSafeDistance;
    private final float _distanceBetweenBedSpawns;
    private final int _obsidianMinimum;

    private BlockPos _bedSpawn = null;

    private static final String[] IRON_ARMORS = new String[]{"iron_chestplate", "iron_leggings", "iron_helmet", "iron_boots"};
    private static final String[] DIAMOND_ARMORS = new String[]{"diamond_chestplate", "diamond_leggings", "diamond_helmet", "diamond_boots"};
    private static final String[] NETHERITE_ARMORS = new String[]{"netherite_chestplate", "netherite_leggings", "netherite_helmet", "netherite_boots"};
    private final EquipArmorTask _equipIronArmor = new EquipArmorTask(IRON_ARMORS);
    private final EquipArmorTask _equipDiamondArmor = new EquipArmorTask(DIAMOND_ARMORS);
    private final EquipArmorTask _equipNetheriteArmor = new EquipArmorTask(NETHERITE_ARMORS);

    private final EscapeSpawnTask _escapeSpawnTask;
    private TravelAwayFromHighwayAxisTask _escapeAxisTask;
    private final CollectFoodTask _collectFoodTask;
    private final PlaceBedAndSetSpawnTask _placeBedSpawnTask = new PlaceBedAndSetSpawnTask();
    private final ResourceTask _collectObsidianTask;

    private Task _forceGearTask;

    public AnarchyBaseSurvivalTask(float spawnInnerRadius, float spawnPortalDangerousRadius, float spawnOuterRadius, boolean avoidOceans, float radiusIncreasePerOcean, float dangerFoodAmount, float safeFoodAmount, float axisDangerDistance, float axisSafeDistance, float distanceBetweenBedSpawns, int obsidianMinimum, int obsidianPreferred) {
        _axisDangerDistance = axisDangerDistance;
        _axisSafeDistance = axisSafeDistance;
        _avoidOceans = avoidOceans;
        _radiusIncreasePerOcean = radiusIncreasePerOcean;
        _dangerFoodAmount = dangerFoodAmount;
        _collectFoodTask = new CollectFoodTask(safeFoodAmount);
        _distanceBetweenBedSpawns = distanceBetweenBedSpawns;
        _obsidianMinimum = obsidianMinimum;
        _collectObsidianTask = TaskCatalogue.getItemTask("obsidian", obsidianPreferred);
        _escapeSpawnTask = new EscapeSpawnTask(HighwayAxis.POSITIVE_Z, spawnInnerRadius, spawnPortalDangerousRadius, spawnOuterRadius);
    }

    private static boolean inSpawnRange(Vec3d pos, float range) {
        return pos.lengthSquared() < range*range;
    }
    private static boolean inSpawnRange(AltoClef mod, float range) {
        return inSpawnRange(mod.getPlayer().getPos(), range);
    }

    private static boolean isKindaCloseToAnAxis(AltoClef mod) {
        Vec3d pos = mod.getPlayer().getPos();
        double smallest = Math.min(Math.abs(pos.x), Math.abs(pos.z));
        return smallest < 80;
    }

    private static boolean weStuckInsideAnOcean(AltoClef mod) {
        Predicate<BlockPos> isOcean = bpos -> mod.getWorld().getBiome(bpos).getCategory() == Biome.Category.OCEAN;
        BlockPos center = mod.getPlayer().getBlockPos();
        int radius = 100;
        return (isOcean.test(center)
                && isOcean.test(center.add(radius, 0, 0))
                && isOcean.test(center.add(-radius, 0, 0))
                && isOcean.test(center.add(0, 0, radius))
                && isOcean.test(center.add(0, 0, -radius)));
    }

    private boolean isTooCloseToAxis(AltoClef mod) {
        double axisDistance = AnarchyUtil.getClosestAxis(mod).getDistanceFrom(mod.getPlayer().getPos());
        if (_escapeAxisTask != null && _escapeAxisTask.isActive() && axisDistance < _axisSafeDistance) {
            return true;
        }
        if (axisDistance < _axisDangerDistance) {
            _escapeAxisTask = new TravelAwayFromHighwayAxisTask(AnarchyUtil.getClosestAxis(mod));
            return true;
        }
        return false;
    }

    private final ActionListener<String> onMessageSpawnDestructionCheck = new ActionListener<>(value -> {
        if (value.toLowerCase().contains("missing or obstructed") || value.toLowerCase().contains("you have no home bed")) {
            _bedSpawn = null;
        }
    });

    @Override
    protected void onStart(AltoClef mod) {
        mod.onGameMessage.addListener(onMessageSpawnDestructionCheck);
        mod.getBlockTracker().trackBlock(Blocks.NETHER_PORTAL);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        runInBackground(mod);

        // Do we have at least diamond gear and some food?
        boolean isPrepared =
                   mod.getInventoryTracker().armorRequirementMet(ArmorRequirement.NETHERITE)
                && mod.getInventoryTracker().totalFoodScore() > _dangerFoodAmount
                && mod.getInventoryTracker().hasItem(Items.NETHERITE_PICKAXE)
                && mod.getInventoryTracker().hasItem(Items.NETHERITE_SWORD);

        // We may want some food at any time.
        if (_collectFoodTask.isActive() && !_collectFoodTask.isFinished(mod)) {
            setDebugState("Getting Food");
            return _collectFoodTask;
        }

        if (_placeBedSpawnTask.isSpawnSet()) {
            _bedSpawn = _placeBedSpawnTask.getBedSleptPos();
        }

        if (!isPrepared) {
            // Escape spawn
            if (inSpawnRange(mod, _escapeSpawnTask.spawnInnerRadius) || (_escapeSpawnTask.isActive() && !_escapeSpawnTask.isFinished(mod))) {
                if (!_escapeSpawnTask.isActive()) {
                    HighwayAxis targetAxis = AnarchyUtil.getClosestAxis(mod);
                    Debug.logMessage("Now escaping along " + targetAxis);
                    _escapeSpawnTask.axis = targetAxis;
                }
                setDebugState("Escaping spawn");
                return _escapeSpawnTask;
            }
            // We're in an ocean, don't stay here.
            if (_avoidOceans
                    && isKindaCloseToAnAxis(mod)
                    && mod.getCurrentDimension() == Dimension.OVERWORLD
                    && weStuckInsideAnOcean(mod)) {
                _escapeSpawnTask.spawnInnerRadius += _radiusIncreasePerOcean;
                _escapeSpawnTask.spawnOuterRadius = Math.max(_escapeSpawnTask.spawnInnerRadius + 100, _escapeSpawnTask.spawnOuterRadius);
                Debug.logMessage("Found an ocean, we will keep moving to get to dry land.");
                return _escapeSpawnTask;
            }

            // To prepare, go away from an axis
            if (isTooCloseToAxis(mod)) {
                setDebugState("Moving away from axis for GEAR");
                return _escapeAxisTask;
            }

            // Get iron armor if we have NO armor
            // TODO: Code duplication w/ iron + diamond + netherite
            if ((!mod.getInventoryTracker().armorRequirementMet(ArmorRequirement.IRON)
                    && !mod.getInventoryTracker().hasItem(DIAMOND_ARMORS)
                    && !mod.getInventoryTracker().hasItem(NETHERITE_ARMORS))
                    || !mod.getInventoryTracker().hasItem(Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE)
                    || !mod.getInventoryTracker().hasItem(Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD)) {
                String pre = "Getting Iron Gear: ";
                if (!mod.getInventoryTracker().hasItem(Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE)) {
                    setDebugState(pre + "Pickaxe");
                    _forceGearTask = TaskCatalogue.getItemTask("iron_pickaxe", 2);
                    return _forceGearTask;
                }
                if (!mod.getInventoryTracker().armorRequirementMet(ArmorRequirement.IRON)) {
                    setDebugState(pre + "Armor");
                    return _equipIronArmor;
                }
                if (!mod.getInventoryTracker().hasItem(Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD)) {
                    setDebugState(pre + "Sword");
                    return TaskCatalogue.getItemTask("iron_sword", 1);
                }
            }
            // Food?
            if (mod.getInventoryTracker().totalFoodScore() < _dangerFoodAmount) {
                return _collectFoodTask;
            }
            // We may want to collect more gear...
            if (_forceGearTask != null && _forceGearTask.isActive() && !_forceGearTask.isFinished(mod)) {
                setDebugState("Finish collecting gear...");
                return _forceGearTask;
            }
            // At least diamond gear if we don't have it yet
            // TODO: Code duplication w/ iron + diamond + netherite
            if ((!mod.getInventoryTracker().armorRequirementMet(ArmorRequirement.DIAMOND)
                    && !mod.getInventoryTracker().hasItem(NETHERITE_ARMORS))
                    || !mod.getInventoryTracker().hasItem(Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE)
                    || !mod.getInventoryTracker().hasItem(Items.DIAMOND_SWORD, Items.NETHERITE_SWORD)) {
                String pre = "Getting Diamond Gear: ";
                if (!mod.getInventoryTracker().hasItem(Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE)) {
                    setDebugState(pre + "Pickaxe");
                    return TaskCatalogue.getItemTask("diamond_pickaxe", 2);
                }
                if (!mod.getInventoryTracker().armorRequirementMet(ArmorRequirement.DIAMOND)) {
                    setDebugState(pre + "Armor");
                    return _equipDiamondArmor;
                }
                if (!mod.getInventoryTracker().hasItem(Items.DIAMOND_SWORD, Items.NETHERITE_SWORD)) {
                    setDebugState(pre + "Sword");
                    return TaskCatalogue.getItemTask("diamond_sword", 1);
                }
            }

            Debug.logInternal("AnarchySurvival: YOU MISSED A SPOT HERE!");
        }

        // If we're setting spawn, set it.
        if (_placeBedSpawnTask.isActive() && !_placeBedSpawnTask.isFinished(mod)) {
            setDebugState("Setting a spawnpoint");
            return _placeBedSpawnTask;
        }

        // Set our spawn position from time to time
        boolean wantsToResetSpawn = _bedSpawn == null || !_bedSpawn.isWithinDistance(mod.getOverworldPosition(), _distanceBetweenBedSpawns);

        if (wantsToResetSpawn) {
            if (mod.getCurrentDimension() == Dimension.NETHER) {
                // We're in the nether, get out.
                Predicate<BlockPos> badPortal = blockPos -> {
                    BlockPos overworldPos = new BlockPos(8 * blockPos.getX(), blockPos.getY(), 8 * blockPos.getZ());
                    if (_bedSpawn != null) {
                        return overworldPos.isWithinDistance(_bedSpawn, _distanceBetweenBedSpawns - 100);
                    }
                    return false;
                };
                if (mod.getBlockTracker().anyFound(badPortal, Blocks.NETHER_PORTAL)) {
                    return new EnterNetherPortalTask(Dimension.OVERWORLD, badPortal);
                }
                // Keep moving.
                // TODO: "inNetherWantToSetSpawnTask()" virtual method
                return inNetherWantToSetSpawnTask(mod);
            } else if (mod.getCurrentDimension() == Dimension.OVERWORLD) {
                // To prepare, go away from an axis
                if (isTooCloseToAxis(mod)) {
                    setDebugState("Moving away from axis for BED");
                    return _escapeAxisTask;
                }
                // Get shears first, to make things better.
                if (!mod.getInventoryTracker().hasItem(Items.SHEARS)) {
                    return TaskCatalogue.getItemTask("shears", 1);
                }
                // Place our spawn.
                return _placeBedSpawnTask;
            }
        }


        // Netherite tools if we don't have them already.
        // TODO: Code duplication w/ iron + diamond + netherite
        if (!mod.getInventoryTracker().armorRequirementMet(ArmorRequirement.NETHERITE)
                || !mod.getInventoryTracker().hasItem(Items.NETHERITE_PICKAXE)
                || !mod.getInventoryTracker().hasItem(Items.NETHERITE_SWORD)) {
            setDebugState("Getting Netherite Gear: ");
            if (!mod.getInventoryTracker().hasItem(Items.NETHERITE_PICKAXE) || !mod.getInventoryTracker().hasItem(Items.NETHERITE_SWORD) || !_equipNetheriteArmor.hasArmor(mod)) {
                return TaskCatalogue.getSquashedItemTask(
                        new ItemTarget("netherite_pickaxe", 1),
                        new ItemTarget("netherite_sword", 1),
                        new ItemTarget("netherite_helmet", 1),
                        new ItemTarget("netherite_chestplate", 1),
                        new ItemTarget("netherite_leggings", 1),
                        new ItemTarget("netherite_boots", 1));
            }
            if (!mod.getInventoryTracker().armorRequirementMet(ArmorRequirement.NETHERITE)) {
                return _equipNetheriteArmor;
            }
        }

        // Get a flint and steel
        if (!mod.getInventoryTracker().hasItem(Items.FLINT_AND_STEEL)) {
            return TaskCatalogue.getItemTask("flint_and_steel", 1);
        }

        // Get some obsidian
        if (_collectObsidianTask.isActive() && !_collectObsidianTask.isFinished(mod)) {
            return _collectObsidianTask;
        }
        if (mod.getInventoryTracker().getItemCount(Items.OBSIDIAN) < _obsidianMinimum) {
            return _collectObsidianTask;
        }


        return doFunStuff(mod);

        /*
         * THE PLAN
         *
         * input:
         *      - "spawn inner radius"
         *      - "spawn portals dangerous radius"
         *      - "spawn outer radius"
         *      - "avoid oceans"
         *      - "radius increase per ocean"
         *
         * Keep track of our last "slept" position
         * Keep track of a "wants to sleep" flag
         * on sleep:
         *      Set "slept" position
         *      Reset "wants to sleep" flag
         * if we ever get "your bed is missing or obstructed" message, reset "slept" position and "wants to sleep"
         *
         * if we're inside the "inner radius" and we're UNPREPARED, run "Escape Spawn" task passing in spawn portals dangerous radius + outer radius
         * if we're in the overworld and inside an ocean, set our "spawn outer radius" to our current "radius" plus the ocean penalty and run the same escape spawn task again.
         *
         * If we don't have diamond gear and need food:
         *     if we're too close to an "inner threshold" of a highway: move out a bit to an "outer threshold"
         *     if we do NOT have a full set of armor equipped (and NO DIAMOND+ ARMOR IN INVENTORY EITHER):
         *          Get stacked with iron + iron tools
         *     if we are below a food threshold:
         *          Get food
         *     if we're not stacked with diamonds + diamond tools:
         *          Get stacked with diamonds + diamond tools
         *
         * if we want to sleep:
         *     make sure we're in the overworld
         *     if we have a bed:
         *         if we're below a "sleep danger" threshold distance from the highway:
         *             Move out away from the highway until we get to a "sleep safe" threshold distance from highway
         *     if we don't have shears:
         *         Get shears
         *     Place bed + spawn task
         *
         * if we're not stacked with netherite + netherite tools:
         *     Get stacked with netherite + netherite tools
         *
         * // Now move on to the fun stuff! I think?
         * Return a separate method and determine what the "fun stuff" is
         *
         * "Escape Spawn" task
             * If we in the overworld with NO building blocks, try to get some dirt.
             * run "TravelOutwardHighwayAxis" passing in the spawn portals dangerous radius
             * If we're in the nether and we've passed a certain "overworld threshold" point, we're good.
         * "TravelOutwardHighwayAxis" task
             * Go to any nether portal that's NOT within a passed "threshold" to 0,0
             * if we're NOT near the axis highway, go to the highway
             * Travel along the highway away from 0,0
         */

        //return null;
    }

    protected abstract Task inNetherWantToSetSpawnTask(AltoClef mod);
    protected abstract Task doFunStuff(AltoClef mod);
    protected abstract void runInBackground(AltoClef mod);

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.onGameMessage.removeListener(onMessageSpawnDestructionCheck);
        mod.getBlockTracker().stopTracking(Blocks.NETHER_PORTAL);
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof AnarchyBaseSurvivalTask;
    }

    @Override
    protected String toDebugString() {
        return "Surviving Anarchy";
    }
}

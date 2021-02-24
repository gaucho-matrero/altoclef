package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.misc.EnterNetherPortalTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * This is the big kahoona. Plays the whole game.
 */
public class BeatMinecraftTask extends Task {

    /// TUNABLE PROPERTIES
    private static final String[] DIAMOND_ARMORS = new String[] {"diamond_chestplate", "diamond_leggings", "diamond_helmet", "diamond_boots"};
    private static final int PRE_NETHER_FOOD = 5 * 40;
    private static final int PRE_NETHER_FOOD_MIN = 5 * 20;

    private static final int TARGET_BLAZE_RODS = 7;
    private static final int TARGET_ENDER_PEARLS = 14;
    private static final int TARGET_ENDER_EYES = 14;
    private static final int PIGLIN_BARTER_GOLD_INGOT_BUFFER = 32;

    private List<BlockPos> _endPortalFrame = null;

    // A flag to determine whether we should continue doing something.
    private ForceState _forceState = ForceState.NONE;

    private BlockPos _cachedPortalInNether;
    private CollectBlazeRodsTask _blazeCollection = new CollectBlazeRodsTask(TARGET_BLAZE_RODS);

    private LocateStrongholdTask _strongholdLocater = new LocateStrongholdTask(TARGET_ENDER_EYES);

    private int _cachedEndPearlsInFrame = 0;

    @Override
    protected void onStart(AltoClef mod) {
        _forceState = ForceState.NONE;
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
         * 6) ??? How to get ender pearls automated and fast...
         */

        switch (mod.getCurrentDimension()) {
            case OVERWORLD:
                return overworldTick(mod);
            case NETHER:
                return netherTick(mod);
            case END:
                return endTick(mod);
        }
        throw new IllegalStateException("Shouldn't ever happen.");
    }

    private Task overworldTick(AltoClef mod) {

        // Get diamond armor + gear first
        if (!hasDiamondArmor(mod) || !mod.getInventoryTracker().hasItem(Items.DIAMOND_PICKAXE) || !mod.getInventoryTracker().hasItem(Items.DIAMOND_SWORD)) {
            return TaskCatalogue.getSquashedItemTask(
                    new ItemTarget("diamond_chestplate", 1),
                    new ItemTarget("diamond_leggings", 1),
                    new ItemTarget("diamond_helmet", 1),
                    new ItemTarget("diamond_boots", 1),
                    new ItemTarget("diamond_pickaxe", 1),
                    new ItemTarget("diamond_sword", 1)
                    );
        }
        if (!diamondArmorEquipped(mod)) {
            return new EquipArmorTask(DIAMOND_ARMORS);
        }

        // Stronghold portal located.
        if (portalFound()) {
            setDebugState("Filling in portal...");
            int eyes = portalEyesInFrame(mod);
            Debug.logInternal("EYES: " + eyes);
            return null;
        }

        // Locate stronghold portal
        if (_strongholdLocater.isActive() && !_strongholdLocater.isFinished(mod)) {
            setDebugState("Locating end portal.");
            return _strongholdLocater;
        } else {
            if (_endPortalFrame == null && _strongholdLocater.portalFound()) {
                Debug.logMessage("Now we have our portal position.");
                _endPortalFrame = _strongholdLocater.getPortalFrame();
            }
        }
        if (_endPortalFrame == null && mod.getInventoryTracker().getItemCount(Items.ENDER_EYE) >= TARGET_ENDER_EYES) {
            return _strongholdLocater;
        }

        /*
        if (!diamondArmorEquipped(mod) || !mod.getInventoryTracker().hasItem(Items.DIAMOND_PICKAXE) || !mod.getInventoryTracker().hasItem(Items.DIAMOND_SWORD)) {
            if (mod.getInventoryTracker().miningRequirementMet(MiningRequirement.IRON) && _forceState != ForceState.GETTING_DIAMOND_GEAR) {
                // Get a crafting table first before mining below
                if (!mod.getInventoryTracker().hasItem(Items.CRAFTING_TABLE)) {
                    setDebugState("Getting crafting table before going down for diamonds");
                    return TaskCatalogue.getItemTask("crafting_table", 1);
                } else {
                    _forceState = ForceState.GETTING_DIAMOND_GEAR;
                }
            }

            if (!diamondArmorEquipped(mod)) {
                setDebugState("Equipping diamond armor");
                return new EquipArmorTask(DIAMOND_ARMORS);
            } else {
                if (!mod.getInventoryTracker().hasItem(Items.DIAMOND_PICKAXE)) {
                    setDebugState("Getting diamond pickaxe");
                    return TaskCatalogue.getItemTask("diamond_pickaxe", 1);
                } else if (!mod.getInventoryTracker().hasItem(Items.DIAMOND_SWORD)) {
                    setDebugState("Getting diamond sword");
                    return TaskCatalogue.getItemTask("diamond_sword", 1);
                }
            }
        } else if (_forceState == ForceState.GETTING_DIAMOND_GEAR) {
            // We got our gear.
            _forceState = ForceState.NONE;
        }*/

        // Get food
        if (mod.getInventoryTracker().totalFoodScore() < PRE_NETHER_FOOD_MIN) {
            _forceState = ForceState.GETTING_FOOD;
        }
        if (_forceState == ForceState.GETTING_FOOD) {
            if (mod.getInventoryTracker().totalFoodScore() < PRE_NETHER_FOOD) {
                setDebugState("Getting food");
                return new CollectFoodTask(PRE_NETHER_FOOD);
            } else {
                _forceState = ForceState.NONE;
            }
        }

        int eyes = mod.getInventoryTracker().getItemCount(Items.ENDER_EYE);
        int rodsNeeded = TARGET_BLAZE_RODS - (mod.getInventoryTracker().getItemCount(Items.BLAZE_POWDER) / 2) - eyes;
        int pearlsNeeded = TARGET_ENDER_PEARLS - eyes;

        // Get blaze rods by going to nether
        if (mod.getInventoryTracker().getItemCount(Items.BLAZE_ROD) < rodsNeeded || mod.getInventoryTracker().getItemCount(Items.ENDER_PEARL) < pearlsNeeded) {
            setDebugState("Going to nether!");
            //Debug.logInternal(mod.getInventoryTracker().getItemCount(Items.ENDER_PEARL) + "< " + TARGET_ENDER_PEARLS + " : " + mod.getInventoryTracker().getItemCount(Items.BLAZE_ROD) + " < " + rodsNeeded);
            // Go to nether
            return new EnterNetherPortalTask(new ConstructNetherPortalSpeedrunTask(), Dimension.NETHER);
        } else {
            setDebugState("Crafting our blaze powder + eyes");
            int powderNeeded = (TARGET_ENDER_EYES - eyes);
            if (mod.getInventoryTracker().getItemCount(Items.BLAZE_POWDER) < powderNeeded) {
                return new CraftInInventoryTask(new ItemTarget(Items.BLAZE_POWDER, powderNeeded), CraftingRecipe.newShapedRecipe("blaze_powder",
                        new ItemTarget[] {new ItemTarget(Items.BLAZE_ROD, 1), null, null, null}, 2));
            }
            // Craft
            return new CraftInInventoryTask(new ItemTarget(Items.ENDER_EYE, TARGET_ENDER_EYES), CraftingRecipe.newShapedRecipe("ender_eye",
                    new ItemTarget[] {new ItemTarget(Items.ENDER_PEARL, 1), new ItemTarget(Items.BLAZE_POWDER, 1), null, null}, 1));
        }
    }

    private Task netherTick(AltoClef mod) {

        // Keep track of our portal so we may return to it.
        if (_cachedPortalInNether == null) {
            _cachedPortalInNether = mod.getPlayer().getBlockPos();
        }

        // Blaze rods
        if (mod.getInventoryTracker().getItemCount(Items.BLAZE_ROD) < TARGET_BLAZE_RODS) {
            setDebugState("Collecting Blaze Rods");
            return _blazeCollection;
        }

        // Piglin Barter
        if (mod.getInventoryTracker().getItemCount(Items.ENDER_PEARL) < TARGET_ENDER_PEARLS) {
            setDebugState("Collecting Ender Pearls");
            return new TradeWithPiglinsTask(PIGLIN_BARTER_GOLD_INGOT_BUFFER, new ItemTarget(Items.ENDER_PEARL, TARGET_ENDER_PEARLS));
        }

        setDebugState("Getting the hell out of here");
        return new EnterNetherPortalTask(new GetToBlockTask(_cachedPortalInNether, false), Dimension.OVERWORLD);
    }

    private Task endTick(AltoClef mod) {
        return null;
    }

    private boolean diamondArmorEquipped(AltoClef mod) {
        for (String armor : DIAMOND_ARMORS) {
            if (!mod.getInventoryTracker().isArmorEquipped(TaskCatalogue.getItemMatches(armor)[0])) return false;
        }
        return true;
    }
    private boolean hasDiamondArmor(AltoClef mod) {
        for (String armor : DIAMOND_ARMORS) {
            Item item = TaskCatalogue.getItemMatches(armor)[0];
            if (mod.getInventoryTracker().isArmorEquipped(item)) continue;
            if (!mod.getInventoryTracker().hasItem(item)) return false;
        }
        return true;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Most likely we have failed or cancelled at this point.
        // But one day this will actually trigger after the game is completed. Just you wait.

    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof BeatMinecraftTask;
    }

    @Override
    protected String toDebugString() {
        return "Beating the game";
    }

    private boolean portalFound() {
        return _endPortalFrame != null;
    }

    private int portalEyesInFrame(AltoClef mod) {
        int count = 0;
        for (BlockPos b : _endPortalFrame) {
            if (!mod.getChunkTracker().isChunkLoaded(b)) {
                return _cachedEndPearlsInFrame;
            }
            BlockState state = mod.getWorld().getBlockState(b);
            if (state.getBlock() != Blocks.END_PORTAL_FRAME) {
                Debug.logWarning("BLOCK POS " + b + " DOES NOT CONTAIN END PORTAL FRAME! This is probably due to a bug/incorrect assumption.");
            }
            boolean filled = state.get(EndPortalFrameBlock.EYE);
            if (filled) {
                count++;
            }
        }
        _cachedEndPearlsInFrame = count;
        return count;
    }

    private enum ForceState {
        NONE,
        GETTING_DIAMOND_GEAR,
        GETTING_FOOD
    }
}

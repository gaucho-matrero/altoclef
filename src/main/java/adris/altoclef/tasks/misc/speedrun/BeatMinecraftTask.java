package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.misc.EnterNetherPortalTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.MiningRequirement;
import net.minecraft.item.Items;

/**
 * This is the big kahoona. Plays the whole game.
 */
public class BeatMinecraftTask extends Task {

    /// TUNABLE PROPERTIES
    private static final String[] DIAMOND_ARMORS = new String[] {"diamond_chestplate", "diamond_leggings", "diamond_helmet", "diamond_boots"};
    private static final int PRE_NETHER_FOOD = 5 * 40;
    private static final int PRE_NETHER_FOOD_MIN = 5 * 20;

    // A flag to determine whether we should continue doing something.
    private ForceState _forceState = ForceState.NONE;

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


        // Get diamond armor first
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
        }

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

        setDebugState("Going to nether!");
        // Go to nether
        return new EnterNetherPortalTask(new ConstructNetherPortalSpeedrunTask(), Dimension.NETHER);
    }

    private Task netherTick(AltoClef mod) {
        Debug.logMessage("IN NETHER! AYEEEE");
        return null;
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

    private enum ForceState {
        NONE,
        GETTING_DIAMOND_GEAR,
        GETTING_FOOD
    }
}

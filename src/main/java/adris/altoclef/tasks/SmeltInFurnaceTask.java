package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.resources.CollectFuelTask;
import adris.altoclef.tasksystem.ITaskWithDowntime;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.progresscheck.IProgressChecker;
import adris.altoclef.util.progresscheck.LinearProgressChecker;
import adris.altoclef.util.slots.FurnaceSlot;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Ref
// https://minecraft.gamepedia.com/Smelting

public class SmeltInFurnaceTask extends ResourceTask {

    private List<SmeltTarget> _targets;

    private DoSmeltInFurnaceTask _doTask;

    public SmeltInFurnaceTask(List<SmeltTarget> targets) {
        super(extractItemTargets(targets));
        // TODO: Do them in order.
        _doTask = new DoSmeltInFurnaceTask(targets.get(0));
    }
    public SmeltInFurnaceTask(SmeltTarget target) {
        this(Collections.singletonList(target));
    }

    private static List<ItemTarget> extractItemTargets(List<SmeltTarget> recipeTargets) {
        List<ItemTarget> result = new ArrayList<>(recipeTargets.size());
        for (SmeltTarget target : recipeTargets) {
            result.add(target.getItem());
        }
        return result;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        return _doTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        // Close furnace screen
        mod.getPlayer().closeHandledScreen();
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {

        if (obj instanceof SmeltInFurnaceTask) {
            SmeltInFurnaceTask other = (SmeltInFurnaceTask) obj;
            return other._doTask.isEqual(_doTask);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return _doTask.toDebugString();
    }

    static class DoSmeltInFurnaceTask extends DoStuffInContainerTask implements ITaskWithDowntime {

        private SmeltTarget _target;

        private ContainerTracker.FurnaceData _currentFurnace;

        // When we're expected to run out of fuel.
        private int _runOutOfFuelExpectedTick;

        private IProgressChecker<Double> _smeltProgressChecker = new LinearProgressChecker(5, 0.1);


        public DoSmeltInFurnaceTask(SmeltTarget target) {
            super(Blocks.FURNACE, "furnace");
            _target = target;
        }

        @Override
        protected boolean isSubTaskEqual(DoStuffInContainerTask obj) {
            if (obj instanceof DoSmeltInFurnaceTask) {
                DoSmeltInFurnaceTask other = (DoSmeltInFurnaceTask)obj;
                return other._target.equals(_target);
            }
            return false;
        }

        @Override
        protected boolean isContainerOpen(AltoClef mod) {
            //Debug.logMessage("FURNACE OPEN? " + open);
            return (mod.getPlayer().currentScreenHandler instanceof FurnaceScreenHandler);
        }

        @Override
        protected Task onTick(AltoClef mod) {

            // Check for materials.
            // If materials are already in the furnace, we need less of them.
            ItemTarget neededMaterials = new ItemTarget(_target.getMaterial());
            if (_currentFurnace != null) {
                if (neededMaterials.matches(_currentFurnace.materials.getItem())) {
                    neededMaterials.targetCount -= _currentFurnace.materials.getCount();
                }// else {
                    //Debug.logMessage("Material does NOT match " + _currentFurnace.materials.getItem().getTranslationKey());
                //}
                //Debug.logMessage("Material Matches: %d - (%d + %d + %d)", neededMaterials.targetCount, _currentFurnace.materials.getCount(), _currentFurnace.output.getCount(), mod.getInventoryTracker().getItemCount(_target.getItem()));
                neededMaterials.targetCount -= _currentFurnace.output.getCount();
                neededMaterials.targetCount -= mod.getInventoryTracker().getItemCount(_target.getItem());
            }
            if (!mod.getInventoryTracker().targetMet(neededMaterials)) {
                setDebugState("Collecting materials: " + neededMaterials);
                _smeltProgressChecker.reset();
                return getMaterialTask(neededMaterials);
            }
            // Check for fuel.
            // If fuel is already in the furnace, we need less of it.
            double fuelNeeded = _target.getMaterial().targetCount - mod.getInventoryTracker().getItemCount(_target.getItem());
            if (_currentFurnace != null) {
                fuelNeeded = _currentFurnace.getRemainingFuelNeededToBurnMaterials();
            }
            if (fuelNeeded > mod.getInventoryTracker().getTotalFuel()) {
                setDebugState("Collecting fuel. Needs " + fuelNeeded + ", has " + mod.getInventoryTracker().getTotalFuel());
                _smeltProgressChecker.reset();
                return new CollectFuelTask(fuelNeeded);
            }

            // We have fuel and materials. Get to our container and smelt!
            return super.onTick(mod);
        }

        // Override this if our materials must be acquired in a special way.
        // virtual
        protected Task getMaterialTask(ItemTarget target) {
            if (target.isCatalogueItem()) {
                return TaskCatalogue.getItemTask(target.getCatalogueName(), target.targetCount);
            } else {
                Debug.logWarning("Smelt in furnace: material target is not catalogued: " + target + ". Override getMaterialTask or make sure the given material is catalogued!");
                return null;
            }
        }

        @Override
        protected Task containerSubTask(AltoClef mod) {
            FurnaceScreenHandler handler = (FurnaceScreenHandler) mod.getPlayer().currentScreenHandler;

            ContainerTracker.FurnaceMap furnaceMap = mod.getContainerTracker().getFurnaceMap();

            _currentFurnace = furnaceMap.getFurnaceData(getTargetContainerPosition());
            if (_currentFurnace == null) {
                Debug.logWarning("(Weird behaviour) Tried grabbing furnace at pos " + getTargetContainerPosition() + " but it was untracked. Re-updating...");
                furnaceMap.updateContainer(getTargetContainerPosition(), handler);
                _currentFurnace = furnaceMap.getFurnaceData(getTargetContainerPosition());
            }

            // Move materials

            ItemStack output = mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.OUTPUT_SLOT);
            int outputCount = _target.getItem().matches(output.getItem())?  output.getCount() : 0;
            int materialCount = mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.INPUT_SLOT_MATERIALS).getCount();
            int currentlyHeld = mod.getInventoryTracker().getItemCount(_target.getItem());
            int targetCount = _target.getItem().targetCount;
            // How many MORE materials do we need in the slot to end up with the correct number of items?
            int toMove = targetCount - (outputCount + materialCount + currentlyHeld);
            if (toMove > 0) {
                ItemTarget toMoveTarget = new ItemTarget(_target.getMaterial());
                toMoveTarget.targetCount = toMove;
                int moved = mod.getInventoryTracker().moveItemToSlot(toMoveTarget, FurnaceSlot.INPUT_SLOT_MATERIALS);

                if (moved != toMove) {
                    Debug.logWarning("Failed to move " + toMove + " materials to the furnace materials slot. Only moved " + moved + ". Will proceed anyway.");
                }
            }

            // Move fuel
            ItemStack fuelStack = mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.INPUT_SLOT_FUEL);

            Item fuelToUse = getBestFuelSource(mod, fuelStack, _currentFurnace.getRemainingFuelNeededToBurnMaterials());

            if (fuelToUse != null) {
                double fuelPowerPerItem = InventoryTracker.getFuelAmount(fuelToUse);
                double fuelNeeded = _currentFurnace.getRemainingFuelNeededToBurnMaterials();
                int targetFuelItemCount = (int) Math.ceil(fuelNeeded / fuelPowerPerItem);

                // If we already have fuel in the slot, add to it.
                if (fuelStack.getItem().equals(fuelToUse)) {
                    targetFuelItemCount -= fuelStack.getCount();
                }
                int moved = mod.getInventoryTracker().moveItemToSlot(fuelToUse, targetFuelItemCount, FurnaceSlot.INPUT_SLOT_FUEL);
                if (targetFuelItemCount > 0 && moved != targetFuelItemCount) {
                    Debug.logWarning("Failed to move " + targetFuelItemCount + " units of the fuel " + fuelToUse.getTranslationKey() + ". Only moved " + moved + ". Proceeding anyway.");
                }
            }

            // Grab from the output slot
            ItemStack outputSlot = mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.OUTPUT_SLOT);
            if (!outputSlot.isEmpty()) {
                mod.getInventoryTracker().grabItem(FurnaceSlot.OUTPUT_SLOT);
                _smeltProgressChecker.reset();
                //Debug.logMessage("Should have grabbed from furnace output: " + outputSlot.getCount());
            }

            // Re-update furnace tracking since we moved some things around.
            mod.getContainerTracker().getFurnaceMap().updateContainer(getTargetContainerPosition(), handler);

            setDebugState("Smelting...");

            _smeltProgressChecker.setProgress(InventoryTracker.getFurnaceCookPercent(handler));

            // If we made no progress
            if (_smeltProgressChecker.failed()) {
                Debug.logMessage("Smelting failed, hopefully re-opening the container will fix this.");
                mod.getPlayer().closeHandledScreen();
                _smeltProgressChecker.reset();
            }

            return null;
        }

        @Override
        protected double getCostToMakeNew(AltoClef mod) {
            if (_currentFurnace != null) {
                // TODO: If we're already smelting, get cost for materials (for now just set to really high number or something)
                return 9999999;
            }
            // We got stone
            if (mod.getInventoryTracker().getItemCount(Items.COBBLESTONE) > 8) {
                double cost = 300 - (50 * (double)mod.getInventoryTracker().getItemCount(Items.COBBLESTONE) / 8);
                return Math.max(cost, 60);
            }
            // We got pick
            if (mod.getInventoryTracker().miningRequirementMet(MiningRequirement.WOOD)) {
                return 200;
            }
            // We gotta make pick and mine stone
            return 400;
        }

        @Override
        protected BlockPos overrideContainerPosition(AltoClef mod) {
            // If we have a valid container position, KEEP it.
            // TODO: Maybe use containertracker to check for places that have our materials?
            return getTargetContainerPosition();
        }

        @Override
        public boolean isInDowntime(AltoClef mod) {

            // TODO:!! only if we're smelting and have all of our materials & fuel met!!
            Debug.logError("TODO: Implement this! I was too lazy to do it last time. Check above TODO.");
            // We're down while our furnace is expected to be burning.
            int currentTicks = mod.getTicks();
            if (_currentFurnace != null) {
                if (_currentFurnace.getRemainingFuelNeededToBurnMaterials() <= 0) {
                    // Our furnace is ready to go
                    return (_currentFurnace.getExpectedTicksRemaining(currentTicks) <= 1);
                } else {
                    // We will need to go back to refuel
                    return currentTicks >= _runOutOfFuelExpectedTick - 1;
                }
            }
            return false;
        }

        private Item getBestFuelSource(AltoClef mod, ItemStack currentFuel, double fuelStillNeeded) {
            List<Item> candidates = mod.getInventoryTracker().getFuelItems();
            // This is basically 0-1 knapsack right
            // no actually it isn't, this can be done via linear search HUZZAH
            // Pick the candidate that fits within our required "fuel still needed" values.

            double fuelNeededWithoutCurrentFuel = fuelStillNeeded + InventoryTracker.getFuelAmount(currentFuel);

            Item bestFit = null;
            double minError = Double.POSITIVE_INFINITY;

            for (Item item : candidates) {
                double errorFuel;
                double itemCanProvide = mod.getInventoryTracker().getItemCount(item) * InventoryTracker.getFuelAmount(item);
                if (item.equals(currentFuel.getItem())) {
                    errorFuel = fuelStillNeeded - itemCanProvide;
                } else {
                    errorFuel = fuelNeededWithoutCurrentFuel - itemCanProvide;
                }

                if (Math.abs(errorFuel) < minError) {
                    bestFit = item;
                    minError = Math.abs(errorFuel);
                }
            }
            return bestFit;
        }
    }
}

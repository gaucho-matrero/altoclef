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
import adris.altoclef.util.ItemUtil;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.progresscheck.IProgressChecker;
import adris.altoclef.util.progresscheck.LinearProgressChecker;
import adris.altoclef.util.slots.FurnaceSlot;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

// Ref
// https://minecraft.gamepedia.com/Smelting


public class SmeltInFurnaceTask extends ResourceTask {
    private final SmeltTarget[] targets;
    private final DoSmeltInFurnaceTask doTask;

    public SmeltInFurnaceTask(SmeltTarget[] targets) {
        super(extractItemTargets(targets));
        this.targets = targets;
        // TODO: Do them in order.
        doTask = new DoSmeltInFurnaceTask(targets[0]);
    }

    public SmeltInFurnaceTask(SmeltTarget target) {
        this(new SmeltTarget[]{ target });
    }

    private static ItemTarget[] extractItemTargets(SmeltTarget[] recipeTargets) {
        List<ItemTarget> result = new ArrayList<>(recipeTargets.length);
        for (SmeltTarget target : recipeTargets) {
            result.add(target.getSourceItem());
        }
        return Util.toArray(ItemTarget.class, result);
    }

    public void ignoreMaterials() {
        doTask.ignoreMaterials();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return super.isFinished(mod) || doTask.isFinished(mod);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        if (targets.length != 1) {
            Debug.logWarning("Tried smelting multiple targets, only one target is supported at a time!");
        }
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        return doTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        // Close furnace screen
        if (mod.inGame()) {
            mod.getPlayer().closeHandledScreen();
        }
    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {

        if (obj instanceof SmeltInFurnaceTask) {
            SmeltInFurnaceTask other = (SmeltInFurnaceTask) obj;
            return other.doTask.isEqual(doTask);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return doTask.toDebugString();
    }

    public static class DoSmeltInFurnaceTask extends DoStuffInContainerTask implements ITaskWithDowntime {
        private final SmeltTarget target;
        private final IProgressChecker<Double> smeltProgressChecker = new LinearProgressChecker(5, 0.1);
        private ContainerTracker.FurnaceData currentFurnace;
        // When we're expected to run out of fuel.
        private int runOutOfFuelExpectedTick;
        private boolean ignoreMaterials;
        private boolean ranOutOfMaterials;

        DoSmeltInFurnaceTask(SmeltTarget target) {
            super(Blocks.FURNACE, "furnace");
            this.target = target;
        }

        public void ignoreMaterials() {
            ignoreMaterials = true;
        }

        @Override
        protected void onStart(AltoClef mod) {
            super.onStart(mod);
            ranOutOfMaterials = false;
        }

        @Override
        protected Task onTick(AltoClef mod) {

            // Check for materials.
            // If materials are already in the furnace, we need less of them.
            if (!ignoreMaterials) {
                ItemTarget neededMaterials = new ItemTarget(target.getTargetItem());
                neededMaterials.targetCount -= mod.getInventoryTracker().getItemCount(target.getSourceItem());
                if (currentFurnace != null) {
                    if (neededMaterials.matches(currentFurnace.materials.getItem())) {
                        neededMaterials.targetCount -= currentFurnace.materials.getCount();
                    }// else {
                    //Debug.logMessage("Material does NOT match " + _currentFurnace.materials.getItem().getTranslationKey());
                    //}
                    //Debug.logMessage("Material Matches: %d - (%d + %d + %d)", neededMaterials.targetCount, _currentFurnace.materials
                    // .getCount(), _currentFurnace.output.getCount(), mod.getInventoryTracker().getItemCount(_target.getItem()));
                    neededMaterials.targetCount -= currentFurnace.output.getCount();
                }
                if (!mod.getInventoryTracker().targetMet(neededMaterials)) {
                    setDebugState("Collecting materials: " + neededMaterials);
                    smeltProgressChecker.reset();
                    return getMaterialTask(neededMaterials);
                }
            }
            // Check for fuel.
            // If fuel is already in the furnace, we need less of it.

            // SPECIAL CASE: If we are OR WERE searching for a crafting table, we need to hide planks as a source of fuel!
            // Otherwise, planks will be protected/unprotected and fuel will be needed/not needed in an infinite back+forth.

            boolean needsNewFurnace = getTargetContainerPosition() == null;

            double fuelNeeded = target.getTargetItem().targetCount - mod.getInventoryTracker().getItemCount(target.getSourceItem());

            double hasFuel = mod.getInventoryTracker().getTotalFuelNormal();

            if (needsNewFurnace) {
                // Special case: Allocate 4 wood for the crafting of the table,
                // and 2 sticks if we want to make a wooden pickaxe.
                boolean planksProtected = mod.getConfigState().isProtected(Items.OAK_PLANKS);
                boolean sticksProtected = mod.getConfigState().isProtected(Items.STICK);
                int plankCount = planksProtected ? 0 : Math.min(mod.getInventoryTracker().getItemCountIncludingTable(ItemUtil.PLANKS), 4);
                int stickCount = sticksProtected ? 0 : Math.min(mod.getInventoryTracker().getItemCountIncludingTable(Items.STICK), 2);
                hasFuel -= InventoryTracker.getFuelAmount(Items.OAK_PLANKS) * plankCount;
                hasFuel -= InventoryTracker.getFuelAmount(Items.STICK) * stickCount;
            }

            if (ignoreMaterials) {
                // Start our fuel off at just one until we find our furnace.
                fuelNeeded = 1;
            }
            if (currentFurnace != null) {
                fuelNeeded = currentFurnace.getRemainingFuelNeededToBurnMaterials();
            }

            if (fuelNeeded > hasFuel) {
                setDebugState("Collecting fuel. Needs " + fuelNeeded + ", has " + hasFuel);
                smeltProgressChecker.reset();
                return new CollectFuelTask(fuelNeeded);
            }

            // We have fuel and materials. Get to our container and smelt!
            return super.onTick(mod);
        }

        @Override
        protected BlockPos overrideContainerPosition(AltoClef mod) {
            // If we have a valid container position, KEEP it.
            // TODO: Maybe use containertracker to check for places that have our materials?
            return getTargetContainerPosition();
        }

        @Override
        protected boolean isSubTaskEqual(DoStuffInContainerTask obj) {
            if (obj instanceof DoSmeltInFurnaceTask) {
                DoSmeltInFurnaceTask other = (DoSmeltInFurnaceTask) obj;
                return other.target.equals(target) && other.ignoreMaterials == ignoreMaterials;
            }
            return false;
        }

        @Override
        protected boolean isContainerOpen(AltoClef mod) {
            //Debug.logMessage("FURNACE OPEN? " + open);
            return (mod.getPlayer().currentScreenHandler instanceof FurnaceScreenHandler);
        }

        @Override
        protected Task containerSubTask(AltoClef mod) {
            FurnaceScreenHandler handler = (FurnaceScreenHandler) mod.getPlayer().currentScreenHandler;

            ContainerTracker.FurnaceMap furnaceMap = mod.getContainerTracker().getFurnaceMap();

            currentFurnace = furnaceMap.getFurnaceData(getTargetContainerPosition());
            if (currentFurnace == null) {
                Debug.logWarning("(Weird behaviour) Tried grabbing furnace at pos " + getTargetContainerPosition() +
                                 " but it was untracked. Re-updating...");
                furnaceMap.updateContainer(getTargetContainerPosition(), handler);
                currentFurnace = furnaceMap.getFurnaceData(getTargetContainerPosition());
            }

            // Move materials

            ItemStack output = mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.OUTPUT_SLOT);
            int outputCount = target.getSourceItem().matches(output.getItem()) ? output.getCount() : 0;
            int materialCount = mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.INPUT_SLOT_MATERIALS).getCount();

            int currentlyHeld = mod.getInventoryTracker().getItemCount(target.getSourceItem());
            int targetCount = target.getSourceItem().targetCount;
            // How many MORE materials do we need in the slot to end up with the correct number of items?
            int toMove = targetCount - (outputCount + materialCount + currentlyHeld);
            if (ignoreMaterials) {
                toMove = mod.getInventoryTracker().getItemCount(this.target.getTargetItem());

                if (toMove == 0 && materialCount == 0 && outputCount == 0) {
                    Debug.logMessage("We ran out of materials.");
                    ranOutOfMaterials = true;
                    return null;
                }

                //Debug.logInternal("OOF: " + toMove + " : " + this._target.getMaterial().getMatches()[0].getTranslationKey());
            }
            if (toMove > 0) {
                ItemTarget toMoveTarget = new ItemTarget(target.getTargetItem());
                toMoveTarget.targetCount = toMove;
                int moved = mod.getInventoryTracker().moveItemToSlot(toMoveTarget, FurnaceSlot.INPUT_SLOT_MATERIALS);

                if (moved != toMove && !ignoreMaterials) {
                    Debug.logWarning("Failed to move " + toMove + " materials to the furnace materials slot. Only moved " + moved +
                                     ". Will proceed anyway.");
                }
            }

            // Move fuel
            ItemStack fuelStack = mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.INPUT_SLOT_FUEL);

            Item fuelToUse = getBestFuelSource(mod, fuelStack, currentFurnace.getRemainingFuelNeededToBurnMaterials());

            // Debug.logInternal(((fuelToUse != null) ? fuelToUse.getTranslationKey() : "(null)") + " : " + _currentFurnace
            // .getRemainingFuelNeededToBurnMaterials());
            if (fuelToUse != null) {
                double fuelPowerPerItem = InventoryTracker.getFuelAmount(fuelToUse);
                double fuelNeeded = currentFurnace.getRemainingFuelNeededToBurnMaterials();
                int targetFuelItemCount = (int) Math.ceil(fuelNeeded / fuelPowerPerItem);
                //Debug.logInternal("( " + fuelNeeded + " / " + fuelPowerPerItem + " = " + targetFuelItemCount + ", fuelstack: " +
                // fuelStack.getCount() + ")");

                // If we already have fuel in the slot, add to it.
                if (fuelStack.getItem().equals(fuelToUse)) {
                    targetFuelItemCount -= fuelStack.getCount();
                }
                int moved = mod.getInventoryTracker().moveItemToSlot(fuelToUse, targetFuelItemCount, FurnaceSlot.INPUT_SLOT_FUEL);
                int canMove = mod.getInventoryTracker().getItemCount(fuelToUse);
                //Debug.logInternal("moved: " + moved);
                /*if (canMove > 0 && moved != canMove) {
                    Debug.logWarning("Failed to move " + canMove + " units of the fuel " + fuelToUse.getTranslationKey() + ". Only moved
                    " + moved + ". Proceeding anyway.");
                }*/
            }

            // Grab from the output slot
            ItemStack outputSlot = mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.OUTPUT_SLOT);
            if (!outputSlot.isEmpty()) {
                if (!ResourceTask.ensureInventoryFree(mod)) {
                    Debug.logWarning("FAILED TO FREE INVENTORY for furnace smelting. This is bad.");
                } else {
                    mod.getInventoryTracker().grabItem(FurnaceSlot.OUTPUT_SLOT);
                    smeltProgressChecker.reset();
                }
                //Debug.logMessage("Should have grabbed from furnace output: " + outputSlot.getCount());
            }

            // Re-update furnace tracking since we moved some things around.
            mod.getContainerTracker().getFurnaceMap().updateContainer(getTargetContainerPosition(), handler);

            setDebugState("Smelting...");

            smeltProgressChecker.setProgress(InventoryTracker.getFurnaceCookPercent(handler));

            // If we made no progress
            if (smeltProgressChecker.failed()) {
                Debug.logMessage("Smelting failed, hopefully re-opening the container will fix this.");
                mod.getPlayer().closeHandledScreen();
                smeltProgressChecker.reset();
            }

            return null;
        }

        @Override
        protected double getCostToMakeNew(AltoClef mod) {
            if (currentFurnace != null) {
                // TODO: If we're already smelting, get cost for materials (for now just set to really high number or something)
                return 9999999;
            }
            // We got stone
            if (mod.getInventoryTracker().getItemCount(Items.COBBLESTONE) > 8) {
                double cost = 300 - (50 * (double) mod.getInventoryTracker().getItemCount(Items.COBBLESTONE) / 8);
                return Math.max(cost, 60);
            }
            // We got pick
            if (mod.getInventoryTracker().miningRequirementMet(MiningRequirement.WOOD)) {
                return 200;
            }
            // We gotta make pick and mine stone
            return 400;
        }

        // Override this if our materials must be acquired in a special way.
        // virtual
        protected Task getMaterialTask(ItemTarget target) {
            if (target.isCatalogueItem()) {
                return TaskCatalogue.getItemTask(target.getCatalogueName(), target.targetCount);
            } else {
                Debug.logWarning("Smelt in furnace: material target is not catalogued: " + target +
                                 ". Override getMaterialTask or make sure the given material is catalogued!");
                return null;
            }
        }

        @Override
        public boolean isFinished(AltoClef mod) {
            return ranOutOfMaterials;
        }

        @Override
        public boolean isInDowntime(AltoClef mod) {

            // TODO:!! only if we're smelting and have all of our materials & fuel met!!
            Debug.logError("TODO: Implement this! I was too lazy to do it last time. Check above TODO.");
            // We're down while our furnace is expected to be burning.
            int currentTicks = mod.getTicks();
            if (currentFurnace != null) {
                if (currentFurnace.getRemainingFuelNeededToBurnMaterials() <= 0) {
                    // Our furnace is ready to go
                    return (currentFurnace.getExpectedTicksRemaining(currentTicks) <= 1);
                } else {
                    // We will need to go back to refuel
                    return currentTicks >= runOutOfFuelExpectedTick - 1;
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
                if (item.equals(currentFuel.getItem()) || currentFuel.isEmpty()) {
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

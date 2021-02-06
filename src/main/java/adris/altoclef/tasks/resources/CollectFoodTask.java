package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.*;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.slots.FurnaceSlot;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.util.math.BlockPos;

public class CollectFoodTask extends Task {

    // Represents order of preferred mobs to least preferred
    private static final CookableFoodTarget[] COOKABLE_FOODS = new CookableFoodTarget[] {
            new CookableFoodTarget("beef", CowEntity.class),
            new CookableFoodTarget("porkchop", PigEntity.class),
            new CookableFoodTarget("mutton", SheepEntity.class),
            new CookableFoodTarget("salmon", SalmonEntity.class),
            new CookableFoodTarget("chicken", ChickenEntity.class),
            new CookableFoodTarget("cod", CodEntity.class),
            new CookableFoodTarget("rabbit", RabbitEntity.class)
    };

    private static final Item[] ITEMS_TO_PICK_UP = new Item[] {
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_APPLE,
            Items.GOLDEN_CARROT,
            Items.BREAD,
            Items.BAKED_POTATO
    };

    private static final CropTarget[] CROPS = new CropTarget[] {
            new CropTarget(Items.WHEAT, Blocks.WHEAT),
            new CropTarget(Items.CARROT, Blocks.CARROTS),
            new CropTarget(Items.POTATO, Blocks.POTATOES),
            new CropTarget(Items.BEETROOT, Blocks.BEETROOTS),
            new CropTarget(Items.SWEET_BERRIES, Blocks.SWEET_BERRY_BUSH)
    };

    private double _unitsNeeded;

    private Item _currentlySmeltingRawFood = null;
    private Item _currentlySmeltingTarget = null;
    private SmeltInFurnaceTask _currentlySmelting = null;

    public CollectFoodTask(double unitsNeeded) {
        _unitsNeeded = unitsNeeded;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.HAY_BLOCK);
        mod.getBlockTracker().trackBlock(Blocks.SWEET_BERRY_BUSH);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // If we're currently smelting, keep doing so until we can't.
        if (_currentlySmelting != null) {
            // Check if smelting is valid.
            if (mod.getPlayer().currentScreenHandler instanceof FurnaceScreenHandler) {
                // We're inside the furnace...
                // We fail if:
                // - There is nothing in the material slot AND
                // - There is nothing in the output slot AND
                // - we have no materials left
                ItemStack materials = mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.INPUT_SLOT_MATERIALS),
                         output = mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.OUTPUT_SLOT);
                if (!mod.getInventoryTracker().hasItem(_currentlySmeltingRawFood) &&
                        (materials.isEmpty() || materials.getItem() != _currentlySmeltingRawFood) &&
                        (output.isEmpty() || output.getItem() != _currentlySmeltingTarget)
                ) {
                    Debug.logMessage("Stop smelting as we can no longer smelt...");
                    _currentlySmelting = null;
                }
            }
            if (_currentlySmelting != null) {
                return _currentlySmelting;
            }
        }

        // Food Pickup
        for(Item item : ITEMS_TO_PICK_UP) {
            Task t = this.pickupTaskOrNull(mod, item);
            if (t != null) {
                setDebugState("Picking up Food: " + item.getTranslationKey());
                return t;
            }
        }

        int breadPoints = 5;
        int potentialHayUnits = mod.getInventoryTracker().totalFoodScore() + 3 * breadPoints * mod.getInventoryTracker().getItemCount(Items.HAY_BLOCK) + (mod.getInventoryTracker().getItemCount(Items.WHEAT) / 3);
        // We can still collect more hay.
        if (potentialHayUnits < _unitsNeeded) {
            Task hayTask = this.pickupBlockTaskOrNull(mod, Blocks.HAY_BLOCK, Items.HAY_BLOCK);
            if (hayTask != null) {
                setDebugState("Collecting Hay");
                return hayTask;
            }
        }

        // PREPARE FOODS IF WE HAVE MATERIALS

        // Bread
        if (mod.getInventoryTracker().getItemCount(Items.WHEAT) > 3) {
            setDebugState("Crafting Bread");
            Item[] w = new Item[]{Items.WHEAT};
            Item[] o = null;
            return new CraftInTableTask(new ItemTarget(Items.BREAD), CraftingRecipe.newShapedRecipe("bread", new Item[][]{w, w, w, o, o, o, o, o, o}, 1));
        }
        if (mod.getInventoryTracker().hasItem(Items.HAY_BLOCK)) {
            setDebugState("Crafting Wheat");
            Item[] o = null;
            return new CraftInInventoryTask(new ItemTarget(Items.WHEAT), CraftingRecipe.newShapedRecipe("wheat", new Item[][]{new Item[] {Items.HAY_BLOCK}, o, o, o}, 9));
        }

        // Cookable food
        for (CookableFoodTarget target : COOKABLE_FOODS) {
            boolean raw = target.hasRaw(mod);
            boolean near = target.mobNear(mod);
            if (near) {
                // Keep getting food until we got it.
                setDebugState("Getting Cookable Food: " + target.cookedFood);
                return target.getTask(mod, _unitsNeeded);
            } else if (raw) {
                setDebugState("Getting Cookable Food: (smelting phase) " + target.cookedFood);
                // Cook what we have.
                _currentlySmeltingRawFood = TaskCatalogue.getItemMatches(target.rawFood)[0];
                _currentlySmeltingTarget = TaskCatalogue.getItemMatches(target.cookedFood)[0];
                _currentlySmelting = new SmeltInFurnaceTask(new SmeltTarget(new ItemTarget(target.cookedFood), new ItemTarget(target.rawFood)));
                _currentlySmelting.ignoreMaterials();
                return _currentlySmelting;
            }
        }

        // Crops
        for (CropTarget target : CROPS) {
            // If crops are nearby. Do not replant cause we don't care.
            Task t = pickupBlockTaskOrNull(mod, target.cropBlock, target.cropItem);
            if (t != null) {
                setDebugState("Harvesting " + target.cropItem.getTranslationKey());
                return t;
            }
        }

        // Look for food.
        setDebugState("Searching...");
        return new TimeoutWanderTask(Float.POSITIVE_INFINITY);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.HAY_BLOCK);
        mod.getBlockTracker().stopTracking(Blocks.SWEET_BERRY_BUSH);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof CollectFoodTask) {
            CollectFoodTask task = (CollectFoodTask) obj;
            return task._unitsNeeded == _unitsNeeded;
        }
        return false;
    }


    @Override
    protected String toDebugString() {
        return "Collect " + _unitsNeeded + " units of food.";
    }

    /**
     * Returns a task that mines a block and picks up its output.
     * Returns null if task cannot reasonably run.
     */
    private Task pickupBlockTaskOrNull(AltoClef mod, Block blockToCheck, Item itemToGrab) {
        BlockPos nearestBlock = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), blockToCheck);

        ItemEntity nearestDrop = null;
        if (mod.getEntityTracker().itemDropped(itemToGrab)) {
            nearestDrop = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemToGrab);
        }
        if (!mod.getBlockTracker().isTracking(blockToCheck)) mod.getBlockTracker().trackBlock(blockToCheck);
        boolean spotted = nearestBlock != null || nearestDrop != null;
        // Collect hay until we have enough.
        if (spotted) {
                if (nearestDrop != null) {
                    return new GetToEntityTask(nearestDrop);
                } else {
                    return new DestroyBlockTask(nearestBlock);
                }
        }
        return null;
    }

    /**
     * Returns a task that picks up a dropped item.
     * Returns null if task cannot reasonably run.
     */
    private Task pickupTaskOrNull(AltoClef mod, Item itemToGrab) {
        ItemEntity nearestDrop = null;
        if (mod.getEntityTracker().itemDropped(itemToGrab)) {
            nearestDrop = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemToGrab);
        }
        if (nearestDrop != null) {
            return new GetToBlockTask(nearestDrop.getBlockPos(), false);
        }
        return null;
    }

    private static class CookableFoodTarget {
        public String rawFood;
        public String cookedFood;
        public Class mobToKill;
        public CookableFoodTarget(String rawFood, String cookedFood, Class mobToKill) {
            this.rawFood = rawFood;
            this.cookedFood = cookedFood;
            this.mobToKill = mobToKill;
        }
        public CookableFoodTarget(String rawFood, Class mobToKill) {
            this(rawFood, "cooked_" + rawFood, mobToKill);
        }

        private Item getRaw() {
            return TaskCatalogue.getItemMatches(rawFood)[0];
        }

        public boolean hasRaw(AltoClef mod) {
            if (mod.getPlayer().currentScreenHandler instanceof FurnaceScreenHandler) {
                if (mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.INPUT_SLOT_MATERIALS).getItem() == getRaw()) {
                    return true;
                }
            }
            return mod.getInventoryTracker().hasItem(rawFood);
        }
        public int getRawCount(AltoClef mod) {
            int count = mod.getInventoryTracker().getItemCount(new ItemTarget(rawFood));
            if (mod.getPlayer().currentScreenHandler instanceof FurnaceScreenHandler) {
                ItemStack s = mod.getInventoryTracker().getItemStackInSlot(FurnaceSlot.INPUT_SLOT_MATERIALS);
                if (s.getItem() == getRaw()) {
                    count += s.getCount();
                }
            }
            return count;
        }
        public boolean mobNear(AltoClef mod) {
            return mod.getEntityTracker().mobFound(mobToKill);
        }
        public Task getTask(AltoClef mod, double totalFoodPoints) {
            return TaskCatalogue.getItemTask(new ItemTarget(cookedFood, getCookedFoodNeed(mod, totalFoodPoints)));
        }

        // How many raw resources do we need?
        public int getCookedFoodNeed(AltoClef mod, double totalFoodPoints) {
            int rawCount = mod.getInventoryTracker().getItemCount(new ItemTarget(rawFood)),
                cookedCount = mod.getInventoryTracker().getItemCount(new ItemTarget(cookedFood));
            Item raw = getRaw(),
                cooked = TaskCatalogue.getItemMatches(cookedFood)[0];
            double needPoints = totalFoodPoints - mod.getInventoryTracker().totalFoodScore();
            if (needPoints > 0) {
                assert raw.getFoodComponent() != null;
                assert cooked.getFoodComponent() != null;
                int rawScore = raw.getFoodComponent().getHunger() * rawCount;
                // Ignoring the raw food how much food do we need
                double ignoreRaws = needPoints + rawScore;
                return (int)Math.floor(ignoreRaws / (double)(cooked.getFoodComponent().getHunger()) - 0.1) + 1;
            }
            return 0;
        }
    }

    private static class CropTarget {
        public Item cropItem;
        public Block cropBlock;

        public CropTarget(Item cropItem, Block cropBlock) {
            this.cropItem = cropItem;
            this.cropBlock = cropBlock;
        }
    }

}


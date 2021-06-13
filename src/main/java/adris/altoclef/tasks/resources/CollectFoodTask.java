package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.*;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.TimerGame;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.function.Predicate;

public class CollectFoodTask extends Task {

    // Actually screw fish baritone does NOT play nice underwater.
    // Fish kinda suck to harvest so heavily penalize them.
    private static final double FISH_PENALTY = 0 * 0.03;

    // Represents order of preferred mobs to least preferred
    private static final CookableFoodTarget[] COOKABLE_FOODS = new CookableFoodTarget[]{
            new CookableFoodTarget("beef", CowEntity.class),
            new CookableFoodTarget("porkchop", PigEntity.class),
            new CookableFoodTarget("mutton", SheepEntity.class),
            new CookableFoodTargetFish("salmon", SalmonEntity.class),
            new CookableFoodTarget("chicken", ChickenEntity.class),
            new CookableFoodTargetFish("cod", CodEntity.class),
            new CookableFoodTarget("rabbit", RabbitEntity.class)
    };

    private static final Item[] ITEMS_TO_PICK_UP = new Item[]{
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_APPLE,
            Items.GOLDEN_CARROT,
            Items.BREAD,
            Items.BAKED_POTATO
    };

    private static final CropTarget[] CROPS = new CropTarget[]{
            new CropTarget(Items.WHEAT, Blocks.WHEAT),
            new CropTarget(Items.CARROT, Blocks.CARROTS),
            new CropTarget(Items.POTATO, Blocks.POTATOES),
            new CropTarget(Items.BEETROOT, Blocks.BEETROOTS)
    };

    private final double _unitsNeeded;
    private final TimerGame _checkNewOptionsTimer = new TimerGame(3);
    private SmeltInFurnaceTask _smeltTask = null;
    private Task _currentResourceTask = null;

    public CollectFoodTask(double unitsNeeded) {
        _unitsNeeded = unitsNeeded;
    }

    // Gets the units of food if we were to convert all of our raw resources to food.
    @SuppressWarnings("RedundantCast")
    private static double calculateFoodPotential(AltoClef mod) {
        double potentialFood = 0;
        for (ItemStack food : mod.getInventoryTracker().getAvailableFoods()) {
            int count = food.getCount();
            boolean cookedFound = false;
            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                if (food.getItem() == cookable.getRaw()) {
                    assert cookable.getCooked().getFoodComponent() != null;
                    potentialFood += count * cookable.getCooked().getFoodComponent().getHunger();
                    cookedFound = true;
                    break;
                }
            }
            if (cookedFound) continue;
            // We're just an ordinary item.
            if (food.getItem().isFood()) {
                assert food.getItem().getFoodComponent() != null;
                potentialFood += count * food.getItem().getFoodComponent().getHunger();
            }
        }
        int potentialBread = (int) (mod.getInventoryTracker().getItemCount(Items.WHEAT) / 3) + mod.getInventoryTracker().getItemCount(Items.HAY_BLOCK) * 3;
        potentialFood += Objects.requireNonNull(Items.BREAD.getFoodComponent()).getHunger() * potentialBread;
        return potentialFood;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        // Protect ALL food
        mod.getBehaviour().addProtectedItems(ITEMS_TO_PICK_UP);
        for (CookableFoodTarget food : COOKABLE_FOODS)
            mod.getBehaviour().addProtectedItems(food.getRaw(), food.getCooked());
        for (CropTarget crop : CROPS) mod.getBehaviour().addProtectedItems(crop.cropItem);
        mod.getBehaviour().addProtectedItems(Items.HAY_BLOCK, Items.SWEET_BERRIES);

        mod.getBlockTracker().trackBlock(Blocks.HAY_BLOCK);
        mod.getBlockTracker().trackBlock(Blocks.SWEET_BERRY_BUSH);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // If we were previously smelting, keep on smelting.
        if (_smeltTask != null && _smeltTask.isActive() && !_smeltTask.isFinished(mod)) {
            // TODO: If we don't have cooking materials, cancel.
            setDebugState("Cooking...");
            return _smeltTask;
        }

        if (_checkNewOptionsTimer.elapsed()) {
            // Try a new resource task
            _checkNewOptionsTimer.reset();
            _currentResourceTask = null;
        }

        if (_currentResourceTask != null && _currentResourceTask.isActive() && !_currentResourceTask.isFinished(mod) && !_currentResourceTask.thisOrChildAreTimedOut()) {
            return _currentResourceTask;
        }

        // Calculate potential
        double potentialFood = calculateFoodPotential(mod);
        if (potentialFood >= _unitsNeeded) {
            // Convert our raw foods
            // PLAN:
            // - If we have hay/wheat, make it into bread
            // - If we have raw foods, smelt all of them

            // Convert Hay+Wheat -> Bread
            if (mod.getInventoryTracker().getItemCount(Items.WHEAT) > 3) {
                setDebugState("Crafting Bread");
                Item[] w = new Item[]{Items.WHEAT};
                Item[] o = null;
                _currentResourceTask = new CraftInTableTask(new ItemTarget(Items.BREAD), CraftingRecipe.newShapedRecipe("bread", new Item[][]{w, w, w, o, o, o, o, o, o}, 1), false, false);
                return _currentResourceTask;
            }
            if (mod.getInventoryTracker().hasItem(Items.HAY_BLOCK)) {
                setDebugState("Crafting Wheat");
                Item[] o = null;
                _currentResourceTask = new CraftInInventoryTask(new ItemTarget(Items.WHEAT), CraftingRecipe.newShapedRecipe("wheat", new Item[][]{new Item[]{Items.HAY_BLOCK}, o, o, o}, 9), false, false);
                return _currentResourceTask;
            }
            // Convert raw foods -> cooked foods

            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                int rawCount = mod.getInventoryTracker().getItemCount(cookable.getRaw());
                if (rawCount > 0) {
                    //Debug.logMessage("STARTING COOK OF " + cookable.getRaw().getTranslationKey());
                    int toSmelt = rawCount + mod.getInventoryTracker().getItemCount(cookable.getCooked());
                    _smeltTask = new SmeltInFurnaceTask(new SmeltTarget(new ItemTarget(cookable.cookedFood, toSmelt), new ItemTarget(cookable.rawFood, rawCount)));
                    _smeltTask.ignoreMaterials();
                    return _smeltTask;
                }
            }
        } else {
            // Pick up food items from ground
            for (Item item : ITEMS_TO_PICK_UP) {
                Task t = this.pickupTaskOrNull(mod, item);
                if (t != null) {
                    setDebugState("Picking up Food: " + item.getTranslationKey());
                    _currentResourceTask = t;
                    return _currentResourceTask;
                }
            }
            // Pick up raw/cooked foods on ground
            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                Task t = this.pickupTaskOrNull(mod, cookable.getRaw(), 20);
                if (t == null) t = this.pickupTaskOrNull(mod, cookable.getCooked(), 40);
                if (t != null) {
                    setDebugState("Picking up Cookable food");
                    _currentResourceTask = t;
                    return _currentResourceTask;
                }
            }
            // Hay
            Task hayTask = this.pickupBlockTaskOrNull(mod, Blocks.HAY_BLOCK, Items.HAY_BLOCK, 300);
            if (hayTask != null) {
                setDebugState("Collecting Hay");
                _currentResourceTask = hayTask;
                return _currentResourceTask;
            }
            // Crops
            for (CropTarget target : CROPS) {
                // If crops are nearby. Do not replant cause we don't care.
                Task t = pickupBlockTaskOrNull(mod, target.cropBlock, target.cropItem, (blockPos -> {
                    BlockState s = mod.getWorld().getBlockState(blockPos);
                    Block b = s.getBlock();
                    if (b instanceof CropBlock) {
                        boolean isWheat = !(b instanceof PotatoesBlock || b instanceof CarrotsBlock || b instanceof BeetrootsBlock);
                        if (isWheat) {

                            // Chunk needs to be loaded for wheat maturity to be checked.
                            if (!mod.getChunkTracker().isChunkLoaded(blockPos)) {
                                return true;
                            }
                            // Prune if we're not mature/fully grown wheat.
                            CropBlock crop = (CropBlock) b;
                            return !crop.isMature(s);
                        }
                    }
                    // Unbreakable.
                    return !WorldUtil.canBreak(mod, blockPos);
                    // We're not wheat so do NOT reject.
                }), 100);
                if (t != null) {
                    setDebugState("Harvesting " + target.cropItem.getTranslationKey());
                    _currentResourceTask = t;
                    return _currentResourceTask;
                }
            }
            // Cooked foods
            double bestScore = 0;
            Entity bestEntity = null;
            Item bestRawFood = null;
            for (CookableFoodTarget cookable : COOKABLE_FOODS) {
                if (!mod.getEntityTracker().entityFound(cookable.mobToKill)) continue;
                Entity nearest = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), cookable.mobToKill);
                if (nearest == null) continue; // ?? This crashed once?
                if (nearest instanceof LivingEntity) {
                    // Peta
                    if (((LivingEntity) nearest).isBaby()) continue;
                }
                int hungerPerformance = cookable.getCookedUnits();
                double sqDistance = nearest.squaredDistanceTo(mod.getPlayer());
                double score = (double) 100 * hungerPerformance / (sqDistance);
                if (cookable.isFish()) {
                    score *= FISH_PENALTY;
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestEntity = nearest;
                    bestRawFood = cookable.getRaw();
                }
            }
            if (bestEntity != null) {
                setDebugState("Killing " + bestEntity.getEntityName());
                _currentResourceTask = killTaskOrNull(mod, bestEntity, bestRawFood);
                return _currentResourceTask;
            }

            // Sweet berries (separate from crops because they should have a lower priority than everything else cause they suck)
            Task berryPickup = pickupBlockTaskOrNull(mod, Blocks.SWEET_BERRY_BUSH, Items.SWEET_BERRIES, 100);
            if (berryPickup != null) {
                setDebugState("Getting sweet berries (no better foods are present)");
                _currentResourceTask = berryPickup;
                return _currentResourceTask;
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
        mod.getBehaviour().pop();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getInventoryTracker().totalFoodScore() >= _unitsNeeded;
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
    private Task pickupBlockTaskOrNull(AltoClef mod, Block blockToCheck, Item itemToGrab, Predicate<BlockPos> reject, double maxRange) {
        Predicate<BlockPos> rejectPlus = (blockPos) -> {
            if (!WorldUtil.canBreak(mod, blockPos)) return true;
            return reject.test(blockPos);
        };
        BlockPos nearestBlock = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(), rejectPlus, blockToCheck);

        if (nearestBlock != null && !nearestBlock.isWithinDistance(mod.getPlayer().getPos(), maxRange)) {
            nearestBlock = null;
        }

        ItemEntity nearestDrop = null;
        if (mod.getEntityTracker().itemDropped(itemToGrab)) {
            nearestDrop = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemToGrab);
        }
        if (!mod.getBlockTracker().isTracking(blockToCheck)) mod.getBlockTracker().trackBlock(blockToCheck);
        boolean spotted = nearestBlock != null || nearestDrop != null;
        // Collect hay until we have enough.
        if (spotted) {
            if (nearestDrop != null) {
                return new PickupDroppedItemTask(itemToGrab, Integer.MAX_VALUE, true);
                //new DoToClosestEntityTask(() -> mod.getPlayer().getPos(), GetToEntityTask::new,)
                //return new GetToEntityTask(nearestDrop);
            } else {
                return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(), DestroyBlockTask::new, pos -> mod.getBlockTracker().getNearestTracking(pos, rejectPlus, blockToCheck), blockToCheck);
                //return new DestroyBlockTask(nearestBlock);
            }
        }
        return null;
    }

    private Task pickupBlockTaskOrNull(AltoClef mod, Block blockToCheck, Item itemToGrab, double maxRange) {
        return pickupBlockTaskOrNull(mod, blockToCheck, itemToGrab, (toReject) -> false, maxRange);
    }

    private Task killTaskOrNull(AltoClef mod, Entity entity, Item itemToGrab) {
        //Task itemPickup = pickupTaskOrNull(mod, itemToGrab);
        //if (itemPickup != null) return itemPickup;
        //return new DoToClosestEntityTask(() -> mod.getPlayer().getPos(), KillEntityTask::new, entity.getClass());
        //return new KillEntityTask(entity);
        return new KillAndLootTask(entity.getClass(), new ItemTarget(itemToGrab, 1));
    }

    /**
     * Returns a task that picks up a dropped item.
     * Returns null if task cannot reasonably run.
     */
    private Task pickupTaskOrNull(AltoClef mod, Item itemToGrab, double maxRange) {
        ItemEntity nearestDrop = null;
        if (mod.getEntityTracker().itemDropped(itemToGrab)) {
            nearestDrop = mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemToGrab);
        }
        if (nearestDrop != null) {
            if (nearestDrop.isInRange(mod.getPlayer(), maxRange)) {
                return new PickupDroppedItemTask(new ItemTarget(itemToGrab), true);
            }
            //return new GetToBlockTask(nearestDrop.getBlockPos(), false);
        }
        return null;
    }

    private Task pickupTaskOrNull(AltoClef mod, Item itemToGrab) {
        return pickupTaskOrNull(mod, itemToGrab, Double.POSITIVE_INFINITY);
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

        private Item getCooked() {
            return TaskCatalogue.getItemMatches(cookedFood)[0];
        }

        public int getCookedUnits() {
            assert getCooked().getFoodComponent() != null;
            return getCooked().getFoodComponent().getHunger();
        }

        public boolean isFish() {
            return false;
        }
    }

    private static class CookableFoodTargetFish extends CookableFoodTarget {

        public CookableFoodTargetFish(String rawFood, String cookedFood, Class mobToKill) {
            super(rawFood, cookedFood, mobToKill);
        }

        public CookableFoodTargetFish(String rawFood, Class mobToKill) {
            super(rawFood, mobToKill);
        }

        @Override
        public boolean isFish() {
            return true;
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

package adris.altoclef.trackers;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.mixins.AbstractFurnaceScreenHandlerAccessor;
import adris.altoclef.util.*;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.slots.*;
import baritone.utils.ToolSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.screen.*;

import java.util.*;

/**
 * Keeps track of the player's inventory items
 */
public class InventoryTracker extends Tracker {

    // https://minecraft.gamepedia.com/Inventory.
    // inventory.size goes to 40, including armor + shield slot which we will ignore.
    public static final int INVENTORY_SIZE = 36;

    private static final Item[] NORMAL_ACCEPTED_FUEL = new Item[]{Items.COAL, Items.CHARCOAL};
    private static Map<Item, Integer> _fuelTimeMap = null;
    private final HashMap<Item, Integer> _itemCounts = new HashMap<>();
    private final HashMap<Item, List<Integer>> _itemSlots = new HashMap<>();
    private final List<Integer> _foodSlots = new ArrayList<>();
    private int _emptySlots = 0;

    private int _foodPoints = 0;

    public InventoryTracker(TrackerManager manager) {
        super(manager);
    }

    private static Map<Item, Integer> getFuelTimeMap() {
        if (_fuelTimeMap == null) {
            _fuelTimeMap = AbstractFurnaceBlockEntity.createFuelTimeMap();
        }
        return _fuelTimeMap;
    }

    public static double getFuelAmount(ItemStack... stacks) {
        double total = 0;
        for (ItemStack stack : stacks) {
            if (getFuelTimeMap().containsKey(stack.getItem())) {
                total += stack.getCount() * getFuelAmount(stack.getItem());
            }
        }
        return total;
    }

    public static double getFuelAmount(Item... items) {
        double total = 0;
        for (Item item : items) {
            if (getFuelTimeMap().containsKey(item)) {
                int timeTicks = getFuelTimeMap().get(item);
                // 300 ticks of wood -> 1.5 operations
                // 200 ticks -> 1 operation
                total += (double) timeTicks / 200.0;
            }
        }
        return total;
    }

    public static boolean isFuel(Item item) {
        return getFuelTimeMap().containsKey(item);
    }

    public static double getFurnaceFuel(AbstractFurnaceScreenHandler handler) {
        PropertyDelegate d = ((AbstractFurnaceScreenHandlerAccessor) handler).getPropertyDelegate();
        return (double) d.get(0) / 200.0;
    }

    public static double getFurnaceCookPercent(AbstractFurnaceScreenHandler handler) {
        return (double) handler.getCookProgress() / 24.0;
    }
    public static double getFurnaceCookPercent() {
        if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.currentScreenHandler instanceof AbstractFurnaceScreenHandler furnace) {
            return getFurnaceCookPercent(furnace);
        }
        return -1;
    }

    public int getEmptyInventorySlotCount() {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            return _emptySlots;
        }
    }

    public boolean isInventoryFull() {
        return getEmptyInventorySlotCount() <= 0;
    }

    public boolean hasItem(Item item) {
        return getItemCount(item) > 0;
    }

    public boolean hasItem(ItemTarget target) {
        return hasItem(target.getMatches());
    }
    public boolean hasItem(Item... items) {
        ensureUpdated();
        for (Item item : items) {
            if (hasItem(item)) return true;
        }
        return false;
    }

    public boolean hasItem(String ...catalogueNames) {
        for (String catalogueName : catalogueNames) {
            Item[] items = TaskCatalogue.getItemMatches(catalogueName);
            assert items != null;
            if (hasItem(items)) return true;
        }
        return false;
    }

    private int getInventoryItemCount(Item item) {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            int count = 0;
            if (_itemCounts.containsKey(item)) {
                count += _itemCounts.get(item);
            }
            if (item instanceof ArmorItem) {
                if (isArmorEquipped(item)) {
                    // TODO: Impractical but theoretically speaking,
                    // can we have the same armor equipped in more than one armor slot?
                    // If so, this will need to update the NUMBER of times this armor is equipped.
                    count += 1;
                }
            }
            return count;
        }
    }

    private int getInventoryItemCount(Item... items) {
        ensureUpdated();
        int sum = 0;
        for (Item match : items) {
            sum += getInventoryItemCount(match);
        }
        return sum;

    }

    public int getItemCount(ItemTarget target) {
        return getItemCount(target.getMatches());
    }

    public int getItemCount(Item... items) {
        int result = getInventoryItemCount(items);
        ScreenHandler screen = _mod.getPlayer().currentScreenHandler;
        if (screen instanceof PlayerScreenHandler || screen instanceof CraftingScreenHandler) {
            boolean bigCrafting = (screen instanceof CraftingScreenHandler);
            for (int craftSlotIndex = 0; craftSlotIndex < (bigCrafting ? 9 : 4); ++craftSlotIndex) {
                Slot craftSlot = bigCrafting ? CraftingTableSlot.getInputSlot(craftSlotIndex, true) : PlayerSlot.getCraftInputSlot(craftSlotIndex);
                ItemStack stack = getItemStackInSlot(craftSlot);
                for (Item item : items) {
                    if (stack.getItem() == item) {
                        result += stack.getCount();
                    }
                }
            }
        }
        return result;
    }

    public int getMaxItemCount(ItemTarget target) {
        ensureUpdated();
        int max = 0;
        for (Item match : target.getMatches()) {
            int count = getItemCount(match);
            if (count > max) max = count;
        }
        return max;
    }

    public List<Slot> getInventorySlotsWithItem(Item... items) {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            List<Slot> result = new ArrayList<>();
            for (Item item : items) {
                if (_itemSlots.containsKey(item)) {
                    for (int invSlot : _itemSlots.get(item)) {
                        result.add(Slot.getFromInventory(invSlot));
                    }
                }
                // If item is in our cursor, consider that a valid slot.
                if (item == getItemStackInCursorSlot().getItem()) {
                    result.add(new CursorInventorySlot());
                }
            }
            return result;
        }
    }
    public List<Slot> getInventorySlotsWithItem(ItemTarget target) {
        return getInventorySlotsWithItem(target.getMatches());
    }

    public List<Slot> getEmptyInventorySlots() {
        return getInventorySlotsWithItem(Items.AIR);
    }

    public Slot getEmptyInventorySlot() {
        List<Slot> slots = getEmptyInventorySlots();
        if (slots.isEmpty()) {
            return null;
        }
        return slots.get(0);
    }

    public boolean targetsMet(ItemTarget... targets) {
        ensureUpdated();

        for (ItemTarget target : targets) {
            if (getItemCount(target.getMatches()) < target.getTargetCount()) {
                return false;
            }
        }
        return true;
    }

    public boolean miningRequirementMet(MiningRequirement requirement) {
        switch (requirement) {
            case HAND:
                return true;
            case WOOD:
                return hasItem(Items.WOODEN_PICKAXE) || hasItem(Items.STONE_PICKAXE) || hasItem(Items.IRON_PICKAXE) || hasItem(Items.GOLDEN_PICKAXE) || hasItem(Items.DIAMOND_PICKAXE) || hasItem(Items.NETHERITE_PICKAXE);
            case STONE:
                return hasItem(Items.STONE_PICKAXE) || hasItem(Items.IRON_PICKAXE) || hasItem(Items.GOLDEN_PICKAXE) || hasItem(Items.DIAMOND_PICKAXE) || hasItem(Items.NETHERITE_PICKAXE);
            case IRON:
                return hasItem(Items.IRON_PICKAXE) || hasItem(Items.GOLDEN_PICKAXE) || hasItem(Items.DIAMOND_PICKAXE) || hasItem(Items.NETHERITE_PICKAXE);
            case DIAMOND:
                return hasItem(Items.DIAMOND_PICKAXE) || hasItem(Items.NETHERITE_PICKAXE);
            default:
                Debug.logError("You missed a spot");
                return false;
        }
    }

    /**
     * Whether an armor set (or a strictly better version) is FULLY equipped
     */
    public boolean armorRequirementMet(ArmorRequirement requirement) {
        switch (requirement) {
            case NONE:
                return true;
            case LEATHER:
                return ArmorRequirement.LEATHER.requirementMet(_mod) || ArmorRequirement.IRON.requirementMet(_mod) || ArmorRequirement.DIAMOND.requirementMet(_mod) || ArmorRequirement.NETHERITE.requirementMet(_mod);
            case IRON:
                return ArmorRequirement.IRON.requirementMet(_mod) || ArmorRequirement.DIAMOND.requirementMet(_mod) || ArmorRequirement.NETHERITE.requirementMet(_mod);
            case DIAMOND:
                return ArmorRequirement.DIAMOND.requirementMet(_mod) || ArmorRequirement.NETHERITE.requirementMet(_mod);
            case NETHERITE:
                return ArmorRequirement.NETHERITE.requirementMet(_mod);
            default:
                Debug.logError("You missed a spot");
                return false;
        }
    }

    private static boolean isNormalFuel(Item item) {
        return Arrays.asList(NORMAL_ACCEPTED_FUEL).contains(item);
    }
    private static boolean shouldCountAsFuel(AltoClef mod, boolean forceNormalFuel, Item item) {
        if (forceNormalFuel) {
            // Knowingly ignore "protected" items here, as we _only_ care about specific fuel sources.
            return isNormalFuel(item);
        }
        return !mod.getBehaviour().isProtected(item) && getFuelAmount(item) > 0;
    }

    public double getTotalFuel(boolean forceNormalFuel) {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            double total = 0;
            for (Item item : _itemCounts.keySet()) {
                if (shouldCountAsFuel(_mod, forceNormalFuel, item)) {
                    total += getFuelAmount(item) * _itemCounts.get(item);
                }
            }

            // Add fuel from crafting table/output
            ScreenHandler screen = _mod.getPlayer().currentScreenHandler;
            if (screen instanceof PlayerScreenHandler || screen instanceof CraftingScreenHandler) {
                boolean bigCrafting = (screen instanceof CraftingScreenHandler);
                for (int craftSlotIndex = 0; craftSlotIndex < (bigCrafting ? 9 : 4); ++craftSlotIndex) {
                    Slot craftSlot = bigCrafting ? CraftingTableSlot.getInputSlot(craftSlotIndex, true) : PlayerSlot.getCraftInputSlot(craftSlotIndex);
                    ItemStack stack = getItemStackInSlot(craftSlot);
                    if (shouldCountAsFuel(_mod, forceNormalFuel, stack.getItem())) {
                        total += getFuelAmount(stack.getItem()) * stack.getCount();
                    }
                }
                // Also check output slot
                Slot outputSlot = bigCrafting ? CraftingTableSlot.OUTPUT_SLOT : PlayerSlot.CRAFT_OUTPUT_SLOT;
                ItemStack stack = getItemStackInSlot(outputSlot);
                if (shouldCountAsFuel(_mod, forceNormalFuel, stack.getItem())) {
                    total += getFuelAmount(stack.getItem()) * stack.getCount();
                }
            }

            return total;
        }
    }

    public double getTotalFuelNormal() {
        return getTotalFuel(true);
    }

    /*public double getTotalFuel() {
        return getTotalFuel(false, false);
    }*/
    public List<Item> getFuelItems() {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            List<Item> fuel = new ArrayList<>();
            for (Item item : _itemCounts.keySet()) {
                if (!_mod.getBehaviour().isProtected(item)) {
                    if (isFuel(item)) {
                        fuel.add(item);
                    }
                }
            }
            return fuel;
        }
    }

    public List<ItemStack> getAvailableFoods() {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            List<ItemStack> result = new ArrayList<>(_foodSlots.size());
            for (int slot : _foodSlots) {
                ItemStack stack = getItemStackInSlot(Slot.getFromInventory(slot));
                if (stack != null) result.add(stack);
            }
            return result;
        }
    }

    public int getBuildingMaterialCount() {
        return getItemCount(_mod.getModSettings().getThrowawayItems(_mod, true));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasRecipeMaterialsOrTarget(RecipeTarget... targets) {
        ensureUpdated();
        HashMap<Integer, Integer> slotUsedCounts = new HashMap<>();
        for (RecipeTarget target : targets) {
            CraftingRecipe recipe = target.getRecipe();
            int need = 0;
            if (target.getItem() != null) {
                need = target.getItem().getTargetCount();
                if (target.getItem().getMatches() != null) {
                    need -= getItemCount(target.getItem());
                }
            }
            // need holds how many items we need to CRAFT
            // However, a crafting recipe can output more than 1 of an item.
            int materialsPerSlotNeeded = (int) Math.ceil((float) need / target.getRecipe().outputCount());
            for (int i = 0; i < materialsPerSlotNeeded; ++i) {
                for (int slot = 0; slot < recipe.getSlotCount(); ++slot) {
                    ItemTarget needs = recipe.getSlot(slot);

                    // Satisfied by default.
                    if (needs == null || needs.isEmpty()) continue;

                    List<Slot> slotsWithItem = getInventorySlotsWithItem(needs.getMatches());

                    // Other slots may have our crafting supplies.
                    ScreenHandler screen = _mod.getPlayer().currentScreenHandler;
                    if (screen instanceof PlayerScreenHandler || screen instanceof CraftingScreenHandler) {
                        // Check crafting slots
                        boolean bigCrafting = (screen instanceof CraftingScreenHandler);
                        boolean bigRecipe = recipe.isBig();
                        for (int craftSlotIndex = 0; craftSlotIndex < (bigCrafting ? 9 : 4); ++craftSlotIndex) {
                            Slot craftSlot = bigCrafting ? CraftingTableSlot.getInputSlot(craftSlotIndex, bigRecipe) : PlayerSlot.getCraftInputSlot(craftSlotIndex);
                            ItemStack stack = getItemStackInSlot(craftSlot);
                            if (needs.matches(stack.getItem())) {
                                slotsWithItem.add(craftSlot);
                            }
                        }
                    }

                    // Try to satisfy THIS slot.
                    boolean satisfied = false;
                    for (Slot checkSlot : slotsWithItem) {
                        int windowSlot = checkSlot.getWindowSlot();
                        if (!slotUsedCounts.containsKey(windowSlot)) {
                            slotUsedCounts.put(windowSlot, 0);
                        }
                        int usedFromSlot = slotUsedCounts.get(windowSlot);
                        ItemStack stack = getItemStackInSlot(checkSlot);

                        if (usedFromSlot < stack.getCount()) {
                            slotUsedCounts.put(windowSlot, slotUsedCounts.get(windowSlot) + 1);
                            //Debug.logMessage("Satisfied " + slot + " with " + checkInvSlot);
                            satisfied = true;
                            break;
                        }
                    }

                    if (!satisfied) {
                        //Debug.logMessage("FAILED TO SATISFY " + slot + " : needs " + needs);
                        // We couldn't satisfy this slot in either the inventory or crafting output.
                        return false;
                    }
                }
            }
        }
        return true;
        //return getRecipeMapping(Collections.emptyMap(), recipe, count) != null;
    }

    public boolean isArmorEquipped(Item ...matches) {
        ensureUpdated();
        for (Item item : matches) {
            if (item instanceof ArmorItem) {
                for (ItemStack stack : _mod.getPlayer().getArmorItems()) {
                    if (stack.getItem() == item) return true;
                }
            }
        }
        return false;
    }

    public boolean isEquipped(Item ...matches) {
        return Arrays.asList(matches).contains(getItemStackInSlot(PlayerInventorySlot.getEquipSlot()).getItem());
    }

    @SuppressWarnings("ConstantConditions")
    public Slot getGarbageSlot() {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            // Get stuff that's throwaway by default
            List<Slot> throwawaySlots = this.getInventorySlotsWithItem(_mod.getModSettings().getThrowawayItems(_mod));
            if (throwawaySlots.size() != 0) {
                Slot best = throwawaySlots.stream().min((leftSlot, rightSlot) -> {
                    ItemStack left = getItemStackInSlot(leftSlot),
                            right = getItemStackInSlot(rightSlot);
                    return left.getCount() - right.getCount();
                }).get();
                Debug.logInternal("THROWING AWAY throwawayable ITEM AT SLOT " + best);
                return best;
            }

            // Downgrade pickaxe maybe?
            MiningRequirement[] order = new MiningRequirement[]{
                    MiningRequirement.DIAMOND, MiningRequirement.IRON, MiningRequirement.STONE, MiningRequirement.WOOD
            };
            MiningRequirement currentReq = getCurrentMiningRequirement();
            for (MiningRequirement check : order) {
                if (check != currentReq && miningRequirementMet(check)) {
                    // Throw away if we have this item since we already have a BETTER one.
                    Item item = check.getMinimumPickaxe();
                    if (!_mod.getBehaviour().isProtected(item)) {
                        if (hasItem(item)) {
                            //Debug.logInternal("Throwing away: " + item.getTranslationKey());
                            return getInventorySlotsWithItem(item).get(0);
                        }
                    }
                }
            }

            // Now we're getting desparate
            if (_mod.getModSettings().shouldThrowawayUnusedItems()) {
                // Get the first non-important item. For now there is no measure of value.
                List<Integer> possibleSlots = new ArrayList<>();
                for (Item item : this._itemSlots.keySet()) {
                    if (!_mod.getBehaviour().isProtected(item) && !_mod.getModSettings().isImportant(item)) {
                        possibleSlots.addAll(this._itemSlots.get(item));
                    }
                }

                if (possibleSlots.size() != 0) {
                    int best = possibleSlots.stream().min((leftSlot, rightSlot) -> {
                        ItemStack left = getItemStackInSlot(Slot.getFromInventory(leftSlot)),
                                right = getItemStackInSlot(Slot.getFromInventory(rightSlot));
                        boolean leftIsTool = left.getItem() instanceof ToolItem;
                        boolean rightIsTool = right.getItem() instanceof ToolItem;
                        // Prioritize tools over materials.
                        if (rightIsTool && !leftIsTool) {
                            return -1;
                        } else if (leftIsTool && !rightIsTool) {
                            return 1;
                        }
                        if (rightIsTool && leftIsTool) {
                            // Prioritize material type, then durability.
                            ToolItem leftTool = (ToolItem) left.getItem();
                            ToolItem rightTool = (ToolItem) right.getItem();
                            if (leftTool.getMaterial().getMiningLevel() != rightTool.getMaterial().getMiningLevel()) {
                                return leftTool.getMaterial().getMiningLevel() - rightTool.getMaterial().getMiningLevel();
                            }
                            // We want less damage.
                            return left.getDamage() - right.getDamage();
                        }

                        // Prioritize food over other things if we lack food.
                        boolean lacksFood = totalFoodScore() < 8;
                        boolean leftIsFood = left.getItem().isFood() && left.getItem() != Items.SPIDER_EYE;
                        boolean rightIsFood = right.getItem().isFood() && right.getItem() != Items.SPIDER_EYE;
                        if (lacksFood) {
                            if (rightIsFood && !leftIsFood) {
                                return -1;
                            } else if (leftIsFood && !rightIsFood) {
                                return 1;
                            }
                        }
                        // If both are food, pick the better cost.
                        if (leftIsFood && rightIsFood) {
                            assert left.getItem().getFoodComponent() != null;
                            assert right.getItem().getFoodComponent() != null;
                            int leftCost = left.getItem().getFoodComponent().getHunger() * left.getCount(),
                                    rightCost = right.getItem().getFoodComponent().getHunger() * right.getCount();
                            return -1 * (leftCost - rightCost);
                        }

                        // Just discard the one with the smallest quantity, but this doesn't really matter.
                        return left.getCount() - right.getCount();
                    }).get();
                    Debug.logInternal("THROWING AWAY unused ITEM AT SLOT " + best);
                    return Slot.getFromInventory(best);
                } else {
                    Debug.logWarning("No unused items to throw away found. Every item is protected.");
                }
            }
        }

        return null;
    }

    public MiningRequirement getCurrentMiningRequirement() {
        MiningRequirement[] order = new MiningRequirement[]{
                MiningRequirement.DIAMOND, MiningRequirement.IRON, MiningRequirement.STONE, MiningRequirement.WOOD
        };
        for (MiningRequirement check : order) {
            if (miningRequirementMet(check)) {
                return check;
            }
        }
        return MiningRequirement.HAND;
    }

    public Slot getBestToolSlot(BlockState state) {
        Slot bestToolSlot = null;
        double highestSpeed = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < InventoryTracker.INVENTORY_SIZE; ++i) {
            Slot slot = PlayerInventorySlot.getFromInventory(i);
            ItemStack stack = _mod.getInventoryTracker().getItemStackInSlot(slot);
            if (stack.getItem() instanceof ToolItem) {
                if (stack.getItem().isSuitableFor(state)) {
                    double speed = ToolSet.calculateSpeedVsBlock(stack, state);
                    if (speed > highestSpeed) {
                        highestSpeed = speed;
                        bestToolSlot = slot;
                    }
                }
            }
            if (stack.getItem() == Items.SHEARS) {
                // Shears take priority over leaf blocks.
                if (ToolSet.areShearsEffective(state.getBlock())) {
                    bestToolSlot = slot;
                    break;
                }
            }
        }
        return bestToolSlot;
    }

    private HashMap<Integer, Integer> getRecipeMapping(CraftingRecipe recipe) {
        return getRecipeMapping(Collections.emptyMap(), recipe, 1);
    }

    // Less garbo version
    private HashMap<Integer, Integer> getRecipeMapping(Map<Item, Integer> alreadyUsed, CraftingRecipe recipe, int count) {
        ensureUpdated();

        HashMap<Integer, Integer> result = new HashMap<>();

        HashMap<Item, Integer> usedUp = new HashMap<>(alreadyUsed);

        // Go through each craft slot
        for (int craftSlot = 0; craftSlot < recipe.getSlotCount(); ++craftSlot) {
            ItemTarget item = recipe.getSlot(craftSlot);
            if (item == null || item.isEmpty()) continue;

            // Repeat this collection "count" number of times.
            for (int i = 0; i < count; ++i) {
                boolean foundMatch = false;
                //noinspection SpellCheckingInspection
                itemsearch:
                // Check for an item that meets the requirement
                for (Item match : item.getMatches()) {
                    // Ensure we have a default used up of zero if not used up yet.
                    if (!usedUp.containsKey(match)) usedUp.put(match, 0);

                    int toSkip = usedUp.get(match);
                    for (Slot invSlot : getInventorySlotsWithItem(match)) {
                        ItemStack stack = getItemStackInSlot(invSlot);
                        // Skip over items we already used.
                        // Edge case: We may skip over the entire stack. In that case this stack is used up.
                        if (toSkip != 0 && toSkip >= stack.getCount()) {
                            toSkip -= stack.getCount();
                        } else {
                            // If we skip over all the items in THIS stack, we will have at least one left over.
                            // That means we found our guy.

                            result.put(craftSlot, invSlot.getInventorySlot());
                            usedUp.put(match, usedUp.get(match) + 1);
                            foundMatch = true;
                            break itemsearch;
                        }
                    }
                }
                if (!foundMatch) {
                    //Debug.logWarning("Failed to find match for craft slot " + craftSlot);
                    return null;
                }
            }
        }

        return result;
    }

    public int totalFoodScore() {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            return _foodPoints;
        }
    }

    public boolean isInHotBar(Item... items) {
        for (Slot invSlot : getInventorySlotsWithItem(items)) {
            if (0 <= invSlot.getInventorySlot() && invSlot.getInventorySlot() < 9) {
                return true;
            }
        }
        return false;
    }

    public ItemStack getItemStackInSlot(Slot slot) {

        if (slot == null) {
            Debug.logError("Null slot checked.");
            return ItemStack.EMPTY;
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return null;

        if (Slot.isCursor(slot)) {
            return player.currentScreenHandler.getCursorStack().copy();
        }

        //Debug.logMessage("FOOF WINDOW SLOT: " + slot.getWindowSlot() + ", " + slot.getInventorySlot());
        net.minecraft.screen.slot.Slot mcSlot = player.currentScreenHandler.getSlot(slot.getWindowSlot());
        return (mcSlot != null) ? mcSlot.getStack().copy() : ItemStack.EMPTY;
    }

    public ItemStack getItemStackInCursorSlot() {
        return getItemStackInSlot(new CursorInventorySlot());
    }

    @Override
    protected void updateState() {
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            _itemCounts.clear();
            _itemSlots.clear();
            _foodSlots.clear();
            _emptySlots = 0;
            _foodPoints = 0;

            if (MinecraftClient.getInstance().player == null) {
                // No updating needed, we have nothing.
                return;
            }
            PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
            ItemStack cursorStack = MinecraftClient.getInstance().player.currentScreenHandler.getCursorStack().copy();

            // - 1. idk
            for (int slot = -1; slot < INVENTORY_SIZE; ++slot) {
                boolean isCursorStack = (slot == -1);
                ItemStack stack;
                if (isCursorStack) {
                    // Add our cursor stack as well to the list.
                    stack = cursorStack;
                } else {
                    stack = inventory.getStack(slot);
                }
                Item item = stack.getItem();
                int count = stack.getCount();
                if (stack.isEmpty()) {
                    // If our cursor slot is empty, IGNORE IT as we don't want to treat it as a valid slot.
                    if (isCursorStack) {
                        continue;
                    }
                    _emptySlots++;
                    item = Items.AIR;
                }
                if (!_itemCounts.containsKey(item)) {
                    _itemCounts.put(item, 0);
                }
                if (!_itemSlots.containsKey(item)) {
                    _itemSlots.put(item, new ArrayList<>());
                }
                if (item.isFood()) {
                    _foodSlots.add(slot);
                    assert item.getFoodComponent() != null;
                    _foodPoints += item.getFoodComponent().getHunger() * count;
                }
                _itemCounts.put(item, _itemCounts.get(item) + count);
                _itemSlots.get(item).add(slot);
            }
        }
    }

    @Override
    protected void reset() {
        // Dirty clears everything
    }
}

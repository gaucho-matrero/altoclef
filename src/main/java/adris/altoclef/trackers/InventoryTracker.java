package adris.altoclef.trackers;

import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.mixins.AbstractFurnaceScreenHandlerAccessor;
import adris.altoclef.util.*;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.*;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;
import java.util.function.Predicate;

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

    private static boolean slotIsCursor(Slot slot) {
        return slot instanceof CursorInventorySlot;
    }

    public int getEmptySlotCount() {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            return _emptySlots;
        }
    }

    public boolean isInventoryFull() {
        return getEmptySlotCount() <= 0;
    }

    public boolean hasItem(Item item) {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            if (item instanceof ArmorItem) {
                if (isArmorEquipped(item)) return true;
            }
            return _itemCounts.containsKey(item);
        }
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

    public int getItemCount(Item item) {
        ensureUpdated();
        if (!hasItem(item)) return 0;
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

    public int getItemCount(Item... items) {
        ensureUpdated();
        int sum = 0;
        for (Item match : items) {
            sum += getItemCount(match);
        }
        return sum;

    }

    public int getItemCount(ItemTarget target) {
        return getItemCount(target.getMatches());
    }

    public int getItemCountIncludingTable(ItemTarget... targets) {
        int sum = 0;
        for (ItemTarget target : targets) {
            sum += getItemCountIncludingTable(target.getMatches());
        }
        return sum;
    }

    public int getItemCountIncludingTable(Item... items) {
        return getItemCountIncludingTable(true, items);
    }

    public int getItemCountIncludingTable(boolean includeOutput, Item... items) {
        int result = getItemCount(items);
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
            if (includeOutput) {
                // Also check output slot
                Slot outputSlot = bigCrafting ? CraftingTableSlot.OUTPUT_SLOT : PlayerSlot.CRAFT_OUTPUT_SLOT;
                ItemStack stack = getItemStackInSlot(outputSlot);
                for (Item item : items) {
                    if (stack.getItem() == item) result += stack.getCount();
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

    public List<Integer> getInventorySlotsWithItem(Item... items) {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            List<Integer> result = new ArrayList<>();
            for (Item item : items) {
                if (_itemSlots.containsKey(item)) {
                    result.addAll(_itemSlots.get(item));
                }
            }
            return result;
        }
    }

    public List<Integer> getEmptyInventorySlots() {
        return getInventorySlotsWithItem(Items.AIR);
    }

    public boolean targetMet(ItemTarget... targets) {
        ensureUpdated();

        for (ItemTarget target : targets) {
            if (getItemCountIncludingTable(false, target.getMatches()) < target.getTargetCount()) {
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

    public double getTotalFuel(boolean forceNormalFuel) {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            double total = 0;
            for (Item item : _itemCounts.keySet()) {
                boolean normalGood = (forceNormalFuel && Arrays.asList(NORMAL_ACCEPTED_FUEL).contains(item));
                if ((!forceNormalFuel || normalGood) && (forceNormalFuel || !_mod.getBehaviour().isProtected(item))) {
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
                    total += getFuelAmount(stack.getItem()) * stack.getCount();
                }
                // Also check output slot
                Slot outputSlot = bigCrafting ? CraftingTableSlot.OUTPUT_SLOT : PlayerSlot.CRAFT_OUTPUT_SLOT;
                ItemStack stack = getItemStackInSlot(outputSlot);
                total += getFuelAmount(stack.getItem()) * stack.getCount();
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

                    List<Integer> invSlotsWithItem = getInventorySlotsWithItem(needs.getMatches());
                    List<Slot> slotsWithItem = new ArrayList<>();
                    for (int invSlot : invSlotsWithItem) {
                        slotsWithItem.add(Slot.getFromInventory(invSlot));
                    }

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
                ArmorItem armor = (ArmorItem) item;
                for (ItemStack stack : _mod.getPlayer().getArmorItems()) {
                    if (stack.getItem() == item) return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("ConstantConditions")
    public Slot getGarbageSlot() {
        ensureUpdated();
        synchronized (BaritoneHelper.MINECRAFT_LOCK) {
            // Get stuff that's throwaway by default
            List<Integer> throwawaySlots = this.getInventorySlotsWithItem(_mod.getModSettings().getThrowawayItems(_mod));
            if (throwawaySlots.size() != 0) {
                int best = Util.minItem(throwawaySlots, (leftSlot, rightSlot) -> {
                    ItemStack left = getItemStackInSlot(Slot.getFromInventory(leftSlot)),
                            right = getItemStackInSlot(Slot.getFromInventory(rightSlot));
                    return right.getCount() - left.getCount();
                });
                Debug.logInternal("THROWING AWAY throwawayable ITEM AT SLOT " + best);
                return Slot.getFromInventory(best);
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
                            return Slot.getFromInventory(getInventorySlotsWithItem(item).get(0));
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
                    int best = Util.minItem(possibleSlots, (leftSlot, rightSlot) -> {
                        ItemStack left = getItemStackInSlot(Slot.getFromInventory(leftSlot)),
                                right = getItemStackInSlot(Slot.getFromInventory(rightSlot));
                        boolean leftIsTool = left.getItem() instanceof ToolItem;
                        boolean rightIsTool = right.getItem() instanceof ToolItem;
                        // Prioritize tools over materials.
                        if (rightIsTool && !leftIsTool) {
                            return 1;
                        } else if (leftIsTool && !rightIsTool) {
                            return -1;
                        }
                        if (rightIsTool && leftIsTool) {
                            // Prioritize material type, then durability.
                            ToolItem leftTool = (ToolItem) left.getItem();
                            ToolItem rightTool = (ToolItem) right.getItem();
                            if (leftTool.getMaterial().getMiningLevel() != rightTool.getMaterial().getMiningLevel()) {
                                return rightTool.getMaterial().getMiningLevel() - leftTool.getMaterial().getMiningLevel();
                            }
                            // We want less damage.
                            return -1 * (right.getDamage() - left.getDamage());
                        }

                        // Prioritize food over other things if we lack food.
                        boolean lacksFood = totalFoodScore() < 8;
                        boolean leftIsFood = left.getItem().isFood() && left.getItem() != Items.SPIDER_EYE;
                        boolean rightIsFood = right.getItem().isFood() && right.getItem() != Items.SPIDER_EYE;
                        if (lacksFood) {
                            if (rightIsFood && !leftIsFood) {
                                return 1;
                            } else if (leftIsFood && !rightIsFood) {
                                return -1;
                            }
                        }
                        // If both are food, pick the better cost.
                        if (leftIsFood && rightIsFood) {
                            assert left.getItem().getFoodComponent() != null;
                            assert right.getItem().getFoodComponent() != null;
                            int leftCost = left.getItem().getFoodComponent().getHunger() * left.getCount(),
                                    rightCost = right.getItem().getFoodComponent().getHunger() * right.getCount();
                            return rightCost - leftCost;
                        }

                        // Just keep the one with the most quantity, but this doesn't really matter.
                        return right.getCount() - left.getCount();
                    });
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
                    for (int invSlot : getInventorySlotsWithItem(match)) {
                        ItemStack stack = getItemStackInSlot(Slot.getFromInventory(invSlot));
                        // Skip over items we already used.
                        // Edge case: We may skip over the entire stack. In that case this stack is used up.
                        if (toSkip != 0 && toSkip >= stack.getCount()) {
                            toSkip -= stack.getCount();
                        } else {
                            // If we skip over all the items in THIS stack, we will have at least one left over.
                            // That means we found our guy.

                            result.put(craftSlot, invSlot);
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

    public ItemStack clickSlot(Slot slot, int mouseButton, SlotActionType type) {

        if (slot.getWindowSlot() == -1) {
            Debug.logWarning("Tried to click the cursor slot. Shouldn't do this!");
            return null;
        }

        // NOT THE CASE! We may have something in the cursor slot to place.
        //if (getItemStackInSlot(slot).isEmpty()) return getItemStackInSlot(slot);

        return clickWindowSlot(slot.getWindowSlot(), mouseButton, type);
    }

    private ItemStack clickWindowSlot(int windowSlot, int mouseButton, SlotActionType type) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return null;
        }
        setDirty();
        int syncId = player.currentScreenHandler.syncId;

        return _mod.getController().clickSlot(syncId, windowSlot, mouseButton, type, player);

    }

    public ItemStack clickSlot(Slot slot, SlotActionType type) {
        return clickSlot(slot, 0, type);
    }

    public ItemStack clickSlot(Slot slot, int mouseButton) {
        return clickSlot(slot, mouseButton, SlotActionType.PICKUP);
    }

    public ItemStack clickSlot(Slot slot) {
        return clickSlot(slot, 0);
    }

    /**
     * @param from   Slot to start moving from
     * @param to     Slot to move items to
     * @param amount How many to move
     * @return The number of items successfully transported
     */
    public int moveItems(Slot from, Slot to, int amount) {
        to.ensureWindowOpened();

        ItemStack fromStack = getItemStackInSlot(from);

        if (fromStack == null || fromStack.isEmpty()) {
            Debug.logInternal("(From stack is empty or null)");
            return 0;
        }

        boolean moveFromCursor = slotIsCursor(from);

        ItemStack toStack = getItemStackInSlot(to);
        if (toStack != null && !toStack.isEmpty()) {
            if (!toStack.isItemEqual(fromStack)) {
                //Debug.logMessage("To was occupied, moved it elsewhere.");
                // We have stuff in our target slot. Move it out somewhere.
                clickSlot(to, SlotActionType.QUICK_MOVE);
                // If we're moving from a cursor slot, the cursor slot should already be moved to "to" after clicking.
                if (moveFromCursor) {
                    setDirty();
                    return getItemStackInSlot(from).getCount();
                }
            }
        }

        // Pickup
        ItemStack pickedUp;
        if (moveFromCursor) {
            // our item is already picked up.
            pickedUp = getItemStackInSlot(from);
        } else {
            pickedUp = clickSlot(from);
        }
        //Debug.logMessage("Picked Up " + pickedUp.getCount() + " from slot " + from.getWindowSlot());

        int dropped;

        // Drop
        if (amount >= pickedUp.getCount()) {
            // We don't have enough/we have exactly enough, drop everything here

            clickSlot(to);
            //Debug.logMessage("Dropped it all from slot " + to.getWindowSlot());

            dropped = pickedUp.getCount();
        } else {
            // We have too much in our stack, only move what we need.
            //j = 1;
            for (int i = 0; i < amount; ++i) {
                clickSlot(to, 1);
            }
            // We've picked up our stack, put it back
            clickSlot(from);
            dropped = amount;
        }
        setDirty();
        return dropped;
    }

    public void swapItems(Slot slot1, Slot slot2) {

        // Pick up slot1
        if (!slotIsCursor(slot1)) {
            clickSlot(slot1);
        }
        // Pick up slot2
        ItemStack second = clickSlot(slot2);

        // slot 1 is now in slot 2
        // slot 2 is now in cursor

        // If slot 2 is not empty, move it back to slot 1
        //if (second != null && !second.isEmpty()) {
        if (!slotIsCursor(slot1)) {
            clickSlot(slot1);
        }
        setDirty();
        //}
    }

    public ItemStack throwSlot(Slot slot) {
        ensureUpdated();
        ItemStack pickup = clickSlot(slot);
        clickWindowSlot(-999, 0, SlotActionType.PICKUP);
        setDirty();
        return pickup;
    }

    public void grabItem(Slot slot) {
        clickSlot(slot, 1, SlotActionType.QUICK_MOVE);
    }

    public int moveItemToSlot(ItemTarget toMove, Slot moveTo) {
        for (Item item : toMove.getMatches()) {
            if (getItemCount(item) >= toMove.getTargetCount()) {
                return moveItemToSlot(item, toMove.getTargetCount(), moveTo);
            }
        }
        return 0;
    }

    // These names aren't confusing
    public int moveItemToSlot(Item toMove, int moveCount, Slot moveTo) {
        List<Integer> itemSlots = getInventorySlotsWithItem(toMove);
        int needsToMove = moveCount;
        for (Integer slotIndex : itemSlots) {
            if (needsToMove <= 0) break;
            Slot current = Slot.getFromInventory(slotIndex);
            //Debug.logStack();
            ItemStack stack = getItemStackInSlot(current);
            //Debug.logMessage("(DEBUG ONLY) index=" + slotIndex + ", " + stack.getItem().getTranslationKey() + ", " + stack.getCount());
            int moveSize = stack.getCount();
            if (moveSize > needsToMove) {
                moveSize = needsToMove;
            }
            //Debug.logMessage("MOVING: " + current + " -> " + moveTo);
            needsToMove -= moveItems(current, moveTo, moveSize);
        }
        return moveCount - needsToMove;
    }

    public boolean isEquipped(Item item) {
        Slot target = PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND);
        return getItemStackInSlot(target).getItem() == item;
    }

    public boolean equipItem(Item toEquip) {
        ensureUpdated();

        // Always equip to the second slot. First + last is occupied by baritone.
        _mod.getPlayer().inventory.selectedSlot = 1;

        Slot target = PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND);

        // Already equipped
        if (getItemStackInSlot(target).getItem() == toEquip) return true;

        List<Integer> itemSlots = getInventorySlotsWithItem(toEquip);
        if (itemSlots.size() != 0) {
            int slot = itemSlots.get(0);
            assert target != null;
            int hotbar = target.getInventorySlot();
            if (0 <= hotbar && hotbar < 9) {
                clickSlot(Objects.requireNonNull(Slot.getFromInventory(slot)), hotbar, SlotActionType.SWAP);
                //swapItems(Slot.getFromInventory(slot), target);
                return true;
            } else {
                Debug.logWarning("Tried to swap to hotbar that's not a hotbar position! " + hotbar + " (target=" + target + ")");
                return false;
            }
        }

        Debug.logWarning("Failed to equip item " + toEquip.getTranslationKey());
        return false;
    }

    public void deequipHitTool() {
        deequip(item -> item instanceof ToolItem, true);
    }

    public void deequipRightClickableItem() {
        deequip(item ->
                        item instanceof BucketItem // water,lava,milk,fishes
                                || item instanceof EnderEyeItem
                                || item == Items.BOW
                                || item == Items.CROSSBOW
                                || item == Items.FLINT_AND_STEEL || item == Items.FIRE_CHARGE
                                || item == Items.ENDER_PEARL
                                || item instanceof FireworkItem
                                || item instanceof SpawnEggItem
                                || item == Items.END_CRYSTAL
                                || item == Items.EXPERIENCE_BOTTLE
                                || item instanceof PotionItem // also includes splash/lingering
                                || item == Items.TRIDENT
                                || item == Items.WRITABLE_BOOK
                                || item == Items.WRITTEN_BOOK
                                || item instanceof FishingRodItem
                                || item instanceof OnAStickItem
                                || item == Items.COMPASS
                                || item instanceof EmptyMapItem
                                || item instanceof Wearable
                                || item == Items.SHIELD
                                || item == Items.LEAD
                ,
                true
        );
    }

    /**
     * Tries to de-equip any item that we don't want equipped.
     *
     * @param isBad: Whether an item is bad/shouldn't be equipped
     * @return Whether we successfully de-equipped, or if we didn't have the item equipped at all.
     */
    public boolean deequip(Predicate<Item> isBad, boolean preferEmpty) {
        boolean toolEquipped = false;
        Item equip = getItemStackInSlot(PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND)).getItem();
        if (isBad.test(equip)) {
            // Pick non tool item or air
            if (!preferEmpty || getEmptySlotCount() == 0) {
                for (int i = 0; i < 35; ++i) {
                    Slot s = Slot.getFromInventory(i);
                    if (!isBad.test(getItemStackInSlot(s).getItem())) {
                        equipSlot(s);
                        return true;
                    }
                }
                return false;
            } else {
                equipItem(Items.AIR);
            }
        }
        return true;
    }

    public void equipSlot(Slot slot) {
        Slot target = PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND);
        swapItems(slot, target);
    }

    public boolean equipItem(ItemTarget toEquip) {
        if (toEquip == null) return false;
        ensureUpdated();

        Slot target = PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND);
        // Already equipped
        if (toEquip.matches(getItemStackInSlot(target).getItem())) return true;

        for (Item item : toEquip.getMatches()) {
            if (hasItem(item)) {
                if (equipItem(item)) return true;
            }
        }
        return false;
    }

    public boolean ensureFreeInventorySlot() {
        if (isInventoryFull()) {
            // Throw away!
            Slot toThrow = getGarbageSlot();
            if (toThrow != null) {
                // Equip then throw
                throwSlot(toThrow);
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean isInHotBar(Item... items) {
        for (int invSlot : getInventorySlotsWithItem(items)) {
            if (0 <= invSlot && invSlot < 9) {
                return true;
            }
        }
        return false;
    }

    public void moveToNonEquippedHotbar(Item item, int offset) {

        if (!hasItem(item)) return;

        assert MinecraftClient.getInstance().player != null;
        int equipSlot = MinecraftClient.getInstance().player.inventory.selectedSlot;

        int otherSlot = (equipSlot + 1 + offset) % 9;

        int found = getInventorySlotsWithItem(item).get(0);
        swapItems(Slot.getFromInventory(found), Slot.getFromInventory(otherSlot));
    }

    public void refreshInventory() {
        for (int i = 0; i < INVENTORY_SIZE; ++i) {
            Slot slot = Slot.getFromInventory(i);
            clickSlot(slot);
            clickSlot(slot);
        }
    }

    public ItemStack getItemStackInSlot(Slot slot) {

        if (slot == null) {
            Debug.logError("Null slot checked.");
            return ItemStack.EMPTY;
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return null;

        if (slotIsCursor(slot)) {
            return player.inventory.getCursorStack();
        }

        //Debug.logMessage("FOOF WINDOW SLOT: " + slot.getWindowSlot() + ", " + slot.getInventorySlot());
        net.minecraft.screen.slot.Slot mcSlot = player.currentScreenHandler.getSlot(slot.getWindowSlot());
        return (mcSlot != null) ? mcSlot.getStack() : ItemStack.EMPTY;
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
            PlayerInventory inventory = MinecraftClient.getInstance().player.inventory;

            // - 1. idk
            for (int slot = -1; slot < INVENTORY_SIZE; ++slot) {
                boolean isCursorStack = (slot == -1);
                ItemStack stack;
                if (isCursorStack) {
                    // Add our cursor stack as well to the list.
                    stack = inventory.getCursorStack();
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

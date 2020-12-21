package adris.altoclef.trackers;

import adris.altoclef.Debug;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.util.slots.*;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Pair;

import java.lang.reflect.Field;
import java.util.*;

public class InventoryTracker extends Tracker {

    private HashMap<Item, Integer> _itemCounts = new HashMap<>();
    private HashMap<Item, List<Integer>> _itemSlots = new HashMap<>();

    private static Map<Item, Integer> _fuelTimeMap = null;

    private int _emptySlots = 0;

    public InventoryTracker(TrackerManager manager) {
        super(manager);
    }

    public int getEmptySlotCount() {
        ensureUpdated();
        return _emptySlots;
    }
    public boolean hasItem(Item item) {
        ensureUpdated();
        return _itemCounts.containsKey(item);
    }
    public boolean hasItem(Item ...items) {
        ensureUpdated();
        for (Item item : items) {
            if (hasItem(item)) return true;
        }
        return false;
    }
    public boolean hasItem(String catalogueName) {
        Item[] items = TaskCatalogue.getItemMatches(catalogueName);
        return hasItem(items);
    }
    public int getItemCount(Item item) {
        ensureUpdated();
        if (!hasItem(item)) return 0;
        return _itemCounts.get(item);
    }
    public int getItemCount(Item ...items) {
        int sum = 0;
        for (Item match : items) {
            sum += getItemCount(match);
        }
        return sum;

    }

    public int getItemCount(ItemTarget target) {
        return getItemCount(target.getMatches());
    }

    public int getMaxItemCount(ItemTarget target) {
        int max = 0;
        for (Item match : target.getMatches()) {
            int count = getItemCount(match);
            if (count > max) max = count;
        }
        return max;
    }

    public List<Integer> getInventorySlotsWithItem(Item ...items) {
        List<Integer> result = new ArrayList<>();
        for (Item item : items) {
            if (_itemSlots.containsKey(item)) {
                result.addAll(_itemSlots.get(item));
            }
        }
        return result;
    }

    public boolean targetMet(ItemTarget ...targets) {
        ensureUpdated();

        for(ItemTarget target : targets) {
            if (getItemCount(target) < target.targetCount) {
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

    public double getTotalFuel(boolean includeThrowawayProtected) {
        double total = 0;
        for (Item item : _itemCounts.keySet()) {
            if (includeThrowawayProtected || !_mod.getConfigState().isProtected(item)) {
                total += getFuelAmount(item) * _itemCounts.get(item);
            }
        }
        return total;
    }
    public double getTotalFuel() {
        return getTotalFuel(false);
    }
    public List<Item> getFuelItems() {
        List<Item> fuel = new ArrayList<>();
        for (Item item : _itemCounts.keySet()) {
            if (!_mod.getConfigState().isProtected(item)) {
                if (isFuel(item)) {
                    fuel.add(item);
                }
            }
        }
        return fuel;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasRecipeMaterials(CraftingRecipe recipe) {
        return hasRecipeMaterials(recipe, 1);
    }

    public boolean hasRecipeMaterials(CraftingRecipe recipe, int count) {
        ensureUpdated();
        return getRecipeMapping(recipe, count) != null;
    }

    public boolean isArmorEquipped(Item item) {
        if (item instanceof ArmorItem) {
            ArmorItem armor = (ArmorItem) item;
            Slot slot = PlayerSlot.getEquipSlot(armor.getSlotType());
            ItemStack target = getItemStackInSlot(slot);
            return target.getItem().equals(item);
        }
        Debug.logWarning("Non armor item provided, it is not equipped: " + item.getTranslationKey());
        return false;
    }

    private HashMap<Integer, Integer> getRecipeMapping(CraftingRecipe recipe) {
        return getRecipeMapping(recipe, 1);
    }

    private HashMap<Integer, Integer> getRecipeMapping(CraftingRecipe recipe, int count) {
        ensureUpdated();

        HashMap<Integer, Integer> craftSlotToInventorySlot = new HashMap<>();

        // Matching
        List<ItemTarget> matchingSlots = new ArrayList<>(recipe.mustMatchCount());

        // How many of each item we used
        HashMap<Item, Integer> usedUp = new HashMap<>();

        for (int craftPos = 0; craftPos < recipe.getSlotCount(); ++craftPos) {
            ItemTarget slot = recipe.getSlot(craftPos);
            if (slot == null || slot.isEmpty()) continue;

            //Debug.logMessage(craftPos + " => " + slot);

            boolean mustMatchItem = recipe.mustMatch(craftPos);
            if (mustMatchItem) {
                matchingSlots.add(slot);
            }

            boolean slotSatisfied = false;
            // Make sure we have at least one of the requirements
            for (Item item : slot.getMatches()) {
                if (!usedUp.containsKey(item)) {
                    usedUp.put(item, 0);
                }

                //Debug.logMessage("Check: " + item.getTranslationKey());

                // "Spread Down" our items
                int toSkip = usedUp.get(item);
                int toFind = count;
                //Debug.logMessage("Start toSkip = " + toSkip);

                for (int invSlotPosition : getInventorySlotsWithItem(item)) {
                    ItemStack stack = _mod.getPlayer().inventory.getStack(invSlotPosition);

                    //Debug.logMessage("(exists in slot " + invSlotPosition + ")");

                    if (toSkip >= stack.getCount()) {
                        // We skip through this stack
                        toSkip -= stack.getCount();
                    } else {
                        //Debug.logMessage("Found one in inv slot " + invSlotPosition);
                        toSkip = 0;
                        for (int used = 0; used < count; ++used) {
                            toFind -= stack.getCount();
                            if (toFind < 0) toFind = 0;
                            if (!mustMatchItem) {
                                // Use one up but only if we can use it straight away (not if we're expecting a matching)
                                usedUp.put(item, usedUp.get(item) + 1);
                                if (toFind == 0) {
                                    // Only keep track of the final item, as the mapping pertains to ONE recipe.
                                    craftSlotToInventorySlot.put(craftPos, invSlotPosition);
                                    slotSatisfied = true;
                                }
                            }
                        }
                        // Stop when we've found enough items to fill this slot ("count" checks for multiple times a recipe)
                        if (toFind == 0) {
                            break;
                        }
                    }
                }
                // We ran out of items
                if (toSkip != 0) {
                    //Debug.logWarning("TEMP A: " + toSkip);
                    return null;
                }

                // We found all items for THIS slot/satisfied it.
                if (slotSatisfied) {
                    break;
                }

                /*
                int amountLeft = getItemCount(item) - usedUp.get(item);
                if (amountLeft > 0) {
                    foundItem = true;
                    // Use one up but only if we can use it straight away.
                    if (!mustMatchItem) {
                        inventoryToSlot.put()
                        usedUp.put(item, usedUp.get(item) + 1);
                    }
                }
                 */
            }
            if (!slotSatisfied) {
                //Debug.logWarning("TEMP B");
                // Failure to find item required for this slot.
                return null;
            }
        }

        // TODO: Debug the hell out of this.
        // Now handle matching
        if (!recipe.getMustMatchCollection().isEmpty()) {
            ItemTarget exampleFirst = matchingSlots.get(0);
            int requiredCount = recipe.mustMatchCount() * count;
            for (Item item : exampleFirst.getMatches()) {
                // We found an item that fits that match.
                // At this point, `usedUp` reflects how many items we DEFINITELY used.
                int itemsRemaining = getItemCount(item) - usedUp.get(item);
                if (itemsRemaining >= requiredCount) {
                    // "Spread down" just like we did the items above.
                    int toSkip = usedUp.get(item);

                    Iterator<Integer> matchingSlotIterator = recipe.getMustMatchCollection().iterator();

                    for (int invSlotPosition : getInventorySlotsWithItem(item)) {
                        ItemStack stack = _mod.getPlayer().inventory.getStack(invSlotPosition);

                        if (toSkip > stack.getCount()) {
                            toSkip -= stack.getCount();
                        } else {
                            if (!matchingSlotIterator.hasNext()) {
                                // We exhausted our matching slot requirements. We're done!
                                return craftSlotToInventorySlot;
                            }
                            int index = matchingSlotIterator.next();
                            Debug.logWarning("Matching found one: " + invSlotPosition + " -> " + index);
                            craftSlotToInventorySlot.put(index, invSlotPosition);
                            usedUp.put(item, usedUp.get(item) + 1);
                        }
                    }
                    if (toSkip != 0) {
                        //Debug.logWarning("TEMP C");
                        // We ran out of spaces.
                        return null;
                    }

                    return craftSlotToInventorySlot;
                }
            }
            //Debug.logWarning("TEMP D");
            // No combination of all items matching
            return null;
        }

        // We passed through the rings of fire
        return craftSlotToInventorySlot;

    }

    public ItemStack clickSlot(Slot slot, int mouseButton, SlotActionType type) {
        setDirty();

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return null;
        }
        int syncId = player.currentScreenHandler.syncId;

        return _mod.getController().clickSlot(syncId, slot.getWindowSlot(), mouseButton, type, player);
    }
    public ItemStack clickSlot(Slot slot, int mouseButton) {
        return clickSlot(slot, mouseButton, SlotActionType.PICKUP);
    }
    public ItemStack clickSlot(Slot slot, SlotActionType type) {
        return clickSlot(slot, 0, type);
    }
    public ItemStack clickSlot(Slot slot) {
        return clickSlot(slot, 0);
    }

    /**
     * @param from   Slot to start moving from
     * @param to     Slot to move items to
     * @param amount How many to move
     * @return       The number of items successfully transported
     */
    public int moveItems(Slot from, Slot to, int amount) {
        to.ensureWindowOpened();

        ItemStack fromStack = getItemStackInSlot(from);

        if (fromStack == null || fromStack.isEmpty()) {
            Debug.logInternal("(From stack is empty or null)");
            return 0;
        }

        ItemStack toStack = getItemStackInSlot(to);
        if (toStack != null && !toStack.isEmpty()) {
            if (!toStack.isItemEqual(fromStack)) {
                //Debug.logMessage("To was occupied, moved it elsewhere.");
                // We have stuff in our target slot. Move it out somewhere.
                clickSlot(to, SlotActionType.QUICK_MOVE);
            }
        }

        // Pickup
        ItemStack pickedUp = clickSlot(from);
        //Debug.logMessage("Picked Up " + pickedUp.getCount() + " from slot " + from.getWindowSlot());

        int dropped;

        // Drop
        if (amount >= pickedUp.getCount()) {
            // We don't have enough/exactly enough, drop everything there

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
        clickSlot(slot1);
        // Pick up slot2
        ItemStack placed = clickSlot(slot2);

        // slot 1 is now in slot 2

        // If slot 2 is not empty, move it back to slot 1
        if (placed != null && !placed.isEmpty()) {
            clickSlot(slot1);
        }
    }

    public void grabItem(Slot slot) {
        clickSlot(slot, 1, SlotActionType.QUICK_MOVE);
    }

    // Crafts a recipe. Returns whether it succeeded.
    public boolean craftInstant(CraftingRecipe recipe) {

        Debug.logInternal("CRAFTING... " + recipe);

        boolean bigCrafting = (_mod.getPlayer().currentScreenHandler instanceof CraftingScreenHandler);

        if (recipe.isBig() && !bigCrafting) {
            Debug.logWarning("Tried crafting a 3x3 recipe without a crafting table. Sadly this won't work.");
            return false;
        }

        // Get the position of each item we will use for crafting. Map player inventory/window Slot => crafting slot
        HashMap<Integer, Integer> craftPositionToInvSlot = getRecipeMapping(recipe);

        if (craftPositionToInvSlot == null) {
            Debug.logWarning("Unable to craft");
            return false;
        }

        List<Pair<Slot, Slot>> moveSlotToCraftSlot = new ArrayList<>();

        for (int craftPos : craftPositionToInvSlot.keySet()) {
            Slot itemSlot;
            Slot craftSlot;
            int invSlot = craftPositionToInvSlot.get(craftPos);
            //Debug.logMessage("WHAT? " + invSlot + " -> " + craftPos);
            if (bigCrafting) {
                // Craft in table
                itemSlot = new CraftingTableInventorySlot(invSlot);
                craftSlot = CraftingTableSlot.getInputSlot(craftPos);
            } else {
                // Craft in window
                itemSlot = new PlayerInventorySlot(invSlot);
                craftSlot = PlayerSlot.getCraftInputSlot(craftPos);
            }
            moveSlotToCraftSlot.add(new Pair<>(itemSlot, craftSlot));
        }

        // Move everything
        for (Pair<Slot, Slot> movement : moveSlotToCraftSlot) {
            // moveItems( item slot, craft slot)
            int moved = moveItems(movement.getLeft(), movement.getRight(), 1);
            if (moved != 1) {
                Debug.logWarning("Failed to move item from slot " + movement.getLeft() + " to slot " + movement.getRight() + ". Moved " + moved);
                return false;
            }
        }

        // Receive output
        Slot outputSlot = bigCrafting? CraftingTableSlot.OUTPUT_SLOT : PlayerSlot.CRAFT_OUTPUT_SLOT;
        // TODO: This should be only one call, but it's two. The latter is a temporary fix too.
        clickSlot(outputSlot, 0, SlotActionType.QUICK_MOVE);
        clickSlot(outputSlot, 0, SlotActionType.SWAP);

        // Grab back TODO: This shouldn't be necessary
        for (Pair<Slot, Slot> movement : moveSlotToCraftSlot) {
            Slot craftSlot = movement.getRight();
            clickSlot(craftSlot, 0, SlotActionType.PICKUP);
            clickSlot(craftSlot, 0, SlotActionType.QUICK_MOVE);
        }

        setDirty();
        return true;
    }

    public int moveItemToSlot(ItemTarget toMove, Slot moveTo) {
        for (Item item : toMove.getMatches()) {
            if (getItemCount(item) >= toMove.targetCount) {
                return moveItemToSlot(item, toMove.targetCount, moveTo);
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

    private static Map<Item, Integer> getFuelTimeMap() {
        if (_fuelTimeMap == null) {
            _fuelTimeMap = AbstractFurnaceBlockEntity.createFuelTimeMap();
        }
        return _fuelTimeMap;
    }

    public static double getFuelAmount(ItemStack ...stacks) {
        double total = 0;
        for (ItemStack stack : stacks) {
            if (getFuelTimeMap().containsKey(stack.getItem())) {
                total += stack.getCount() * getFuelAmount(stack.getItem());
            }
        }
        return total;
    }
    public static double getFuelAmount(Item ...items) {
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
        try {
            Field propertyField = AbstractFurnaceScreenHandler.class.getDeclaredField("propertyDelegate");
            propertyField.setAccessible(true);

            PropertyDelegate propertyDelegate = (PropertyDelegate) propertyField.get(handler);
            return (double)propertyDelegate.get(0) / 200.0;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }
    public static double getFurnaceCookPercent(AbstractFurnaceScreenHandler handler) {
        return (double) handler.getCookProgress() / 24.0;
    }


    public ItemStack getItemStackInSlot(Slot slot) {

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return null;

        //Debug.logMessage("FOOF WINDOW SLOT: " + slot.getWindowSlot() + ", " + slot.getInventorySlot());
        return player.currentScreenHandler.getSlot(slot.getWindowSlot()).getStack();
    }

    @Override
    protected void updateState() {
        _itemCounts.clear();
        _itemSlots.clear();
        _emptySlots = 0;

        if (MinecraftClient.getInstance().player == null) {
            // No updating needed, we have nothing.
            return;
        }
        PlayerInventory inventory = MinecraftClient.getInstance().player.inventory;

        for (int slot = 0; slot < inventory.size(); ++slot) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                _emptySlots++;
                continue;
            }
            Item item = stack.getItem();
            int count = stack.getCount();
            if (!_itemCounts.containsKey(item)) {
                _itemCounts.put(item, 0);
            }
            if (!_itemSlots.containsKey(item)) {
                _itemSlots.put(item, new ArrayList<>());
            }
            _itemCounts.put(item, _itemCounts.get(item) + count);
            _itemSlots.get(item).add(slot);
        }
    }
}

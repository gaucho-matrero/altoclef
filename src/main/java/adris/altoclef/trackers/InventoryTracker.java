package adris.altoclef.trackers;

import adris.altoclef.Debug;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.slots.*;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
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
    private List<Integer> _foodSlots = new ArrayList<>();

    private static Map<Item, Integer> _fuelTimeMap = null;

    private int _emptySlots = 0;

    private int _foodPoints = 0;

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

    public int getMaxItemCount(ItemTarget target) {
        ensureUpdated();
        int max = 0;
        for (Item match : target.getMatches()) {
            int count = getItemCount(match);
            if (count > max) max = count;
        }
        return max;
    }

    public List<Integer> getInventorySlotsWithItem(Item ...items) {
        ensureUpdated();
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

    /*
    public double getTotalFoodAmount() {
        ensureUpdated();
        double total = 0;
        for (int slot : _foodSlots) {
            ItemStack stack = getItemStackInSlot(Slot.getFromInventory(slot));
            if (stack.getItem().getFoodComponent() == null) continue;
            total += stack.getItem().getFoodComponent().getHunger() * stack.getCount();
        }
        return total;
    }
     */

    public List<ItemStack> getAvailableFoods() {
        ensureUpdated();
        List<ItemStack> result = new ArrayList<>(_foodSlots.size());
        for(int slot : _foodSlots) {
            result.add(getItemStackInSlot(Slot.getFromInventory(slot)));
        }
        return result;
    }

    public boolean hasRecipeMaterials(HashMap<Item, Integer> usedCount, RecipeTarget ...targets) {
        for (RecipeTarget target : targets) {
            CraftingRecipe recipe = target.getRecipe();
            ItemTarget itemTarget = target.getItem();
            // If we already have the item, we're good.
            if (targetMet(itemTarget)) continue;
            // Check for mapping
            Map<Integer, Integer> mapping = getRecipeMapping(usedCount, recipe, itemTarget.targetCount);
            if (mapping == null) return false;
            // Indicate we've used this item.
            for (int invSlot : mapping.values()) {
                Item item = getItemStackInSlot(Slot.getFromInventory(invSlot)).getItem();
                if (!usedCount.containsKey(item)) {
                    usedCount.put(item, 0);
                }
                usedCount.put(item, usedCount.get(item) + 1);
            }
        }
        return true;
    }

    public boolean hasRecipeMaterials(RecipeTarget...targets) {
        return hasRecipeMaterials(new HashMap<>(), targets);
    }

    public boolean hasRecipeMaterials(CraftingRecipe recipe) {
        return hasRecipeMaterials(recipe, 1);
    }

    public boolean hasRecipeMaterials(CraftingRecipe recipe, int count) {
        ensureUpdated();
        return getRecipeMapping(Collections.emptyMap(), recipe, count) != null;
    }

    public boolean isArmorEquipped(Item item) {
        ensureUpdated();
        if (item instanceof ArmorItem) {
            ArmorItem armor = (ArmorItem) item;
            for(ItemStack stack : _mod.getPlayer().getArmorItems()) {
                if (stack.getItem() == item) return true;
            }
            return false;
            /*
            Slot slot = PlayerSlot.getEquipSlot(armor.getSlotType());
            ItemStack target = getItemStackInSlot(slot);
            return target.getItem().equals(item);
             */
        }
        Debug.logWarning("Non armor item provided, it is not equipped: " + item.getTranslationKey());
        return false;
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
        return _foodPoints;
    }

    public ItemStack clickSlot(Slot slot, int mouseButton, SlotActionType type) {
        setDirty();

        if (slot.getWindowSlot() == -1) {
            Debug.logWarning("Tried to click the cursor slot. Shouldn't do this!");
            return null;
        }

        // NOT THE CASE! We may have something in the cursor slot to place.
        //if (getItemStackInSlot(slot).isEmpty()) return getItemStackInSlot(slot);

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return null;
        }
        int syncId = player.currentScreenHandler.syncId;

        return _mod.getController().clickSlot(syncId, slot.getWindowSlot(), mouseButton, type, player);
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
     * @return       The number of items successfully transported
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
        //}
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
            itemSlot = Slot.getFromInventory(invSlot);
            //Debug.logMessage("WHAT? " + invSlot + " -> " + craftPos);
            if (bigCrafting) {
                // Craft in table
                craftSlot = CraftingTableSlot.getInputSlot(craftPos, recipe.isBig());
            } else {
                // Craft in window
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
                Debug.logStack();
                return false;
            }
        }


        // Receive output
        Slot outputSlot = bigCrafting? CraftingTableSlot.OUTPUT_SLOT : PlayerSlot.CRAFT_OUTPUT_SLOT;

        // This returns false positives all the time if left here.
        /*
        if (getItemStackInSlot(outputSlot).isEmpty()) {
            Debug.logWarning("Craft Output slot is empty");
            return false;
        }
         */

        // TODO: This should be only one call, but it's two. The latter is a temporary fix too.
        clickSlot(outputSlot, 0, SlotActionType.QUICK_MOVE);
        clickSlot(outputSlot, 0, SlotActionType.SWAP);

        // Grab back. This shouldn't be necessary
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
        Slot target = PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND);

        // Already equipped
        if (getItemStackInSlot(target).getItem() == toEquip) return true;

        List<Integer> itemSlots = getInventorySlotsWithItem(toEquip);
        if (itemSlots.size() != 0) {
            int slot = itemSlots.get(0);
            swapItems(Slot.getFromInventory(slot), target);
            return true;
        }

        Debug.logWarning("Failed to equip item " + toEquip.getTranslationKey());
        return false;
    }

    public boolean equipItem(ItemTarget toEquip) {
        if (toEquip == null) return false;

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

    public boolean isInHotBar(Item ...items) {
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

        if (slotIsCursor(slot)) {
            return player.inventory.getCursorStack();
        }

        //Debug.logMessage("FOOF WINDOW SLOT: " + slot.getWindowSlot() + ", " + slot.getInventorySlot());
        return player.currentScreenHandler.getSlot(slot.getWindowSlot()).getStack();
    }

    private static boolean slotIsCursor(Slot slot) {
        return slot instanceof CursorInventorySlot;
    }

    @Override
    protected void updateState() {
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

        for (int slot = -1; slot < inventory.size(); ++slot) {
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

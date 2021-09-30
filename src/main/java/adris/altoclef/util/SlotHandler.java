package adris.altoclef.util;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.slots.PlayerInventorySlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;


public class SlotHandler {

    private final AltoClef _mod;

    private final TimerGame _slotActionTimer = new TimerGame(0);

    public SlotHandler(AltoClef mod) {
        _mod = mod;
    }

    private InventoryTracker inv() {
        return _mod.getInventoryTracker();
    }

    public boolean canDoSlotAction() {
        _slotActionTimer.setInterval(_mod.getModSettings().getContainerItemMoveDelay());
        return _slotActionTimer.elapsed();
    }
    public void registerSlotAction() {
        _mod.getInventoryTracker().setDirty();
        _slotActionTimer.reset();
    }


    public void clickSlot(Slot slot, int mouseButton, SlotActionType type) {
        if (!canDoSlotAction()) return;

        if (slot.getWindowSlot() == -1) {
            Debug.logWarning("Tried to click the cursor slot. Shouldn't do this!");
            return;
        }

        // NOT THE CASE! We may have something in the cursor slot to place.
        //if (getItemStackInSlot(slot).isEmpty()) return getItemStackInSlot(slot);

        clickWindowSlot(slot.getWindowSlot(), mouseButton, type);
    }

    private void clickWindowSlot(int windowSlot, int mouseButton, SlotActionType type) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        inv().setDirty();
        int syncId = player.currentScreenHandler.syncId;

        _mod.getController().clickSlot(syncId, windowSlot, mouseButton, type, player);
    }

    public boolean forceEquipItem(Item toEquip) {

        if (canDoSlotAction()) {

            // Always equip to the second slot. First + last is occupied by baritone.
            _mod.getPlayer().getInventory().selectedSlot = 1;

            Slot target = PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND);

            // Already equipped
            if (inv().getItemStackInSlot(target).getItem() == toEquip) return true;

            List<Slot> itemSlots = inv().getInventorySlotsWithItem(toEquip);
            if (itemSlots.size() != 0) {
                Slot slot = itemSlots.get(0);
                assert target != null;
                int hotbar = target.getInventorySlot();
                if (0 <= hotbar && hotbar < 9) {
                    clickSlot(Objects.requireNonNull(slot), hotbar, SlotActionType.SWAP);
                    registerSlotAction();
                    return true;
                } else {
                    Debug.logWarning("Tried to swap to hotbar that's not a hotbar position! " + hotbar + " (target=" + target + ")");
                    return false;
                }
            }

            Debug.logWarning("Failed to equip item " + toEquip.getTranslationKey());
            return false;
        }
        return true;
    }
    public boolean forceDeequipHitTool() {
        return forceDeequip(item -> item instanceof ToolItem, true);
    }
    public void forceDeequipRightClickableItem() {
        forceDeequip(item ->
                        item instanceof BucketItem // water,lava,milk,fishes
                                || item instanceof EnderEyeItem
                                || item == Items.BOW
                                || item == Items.CROSSBOW
                                || item == Items.FLINT_AND_STEEL || item == Items.FIRE_CHARGE
                                || item == Items.ENDER_PEARL
                                || item instanceof FireworkRocketItem
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
    public boolean forceDeequip(Predicate<Item> isBad, boolean preferEmpty) {
        boolean toolEquipped = false;
        Item equip = inv().getItemStackInSlot(PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND)).getItem();
        if (isBad.test(equip)) {
            // Pick non tool item or air
            if (!preferEmpty || inv().getEmptySlotCount() == 0) {
                for (int i = 0; i < 35; ++i) {
                    Slot s = Slot.getFromInventory(i);
                    if (!isBad.test(inv().getItemStackInSlot(s).getItem())) {
                        forceEquipSlot(s);
                        return true;
                    }
                }
                return false;
            } else {
                forceEquipItem(Items.AIR);
            }
        }
        return true;
    }
    public void forceEquipSlot(Slot slot) {
        Slot target = PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND);
        forceSwapItems(slot, target);
    }

    public boolean forceEquipItem(Item ...matches) {
        return forceEquipItem(new ItemTarget(matches, 1));
    }
    public boolean forceEquipItem(ItemTarget toEquip) {
        if (toEquip == null) return false;

        Slot target = PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND);
        // Already equipped
        if (toEquip.matches(inv().getItemStackInSlot(target).getItem())) return true;

        for (Item item : toEquip.getMatches()) {
            if (inv().hasItem(item)) {
                if (forceEquipItem(item)) return true;
            }
        }
        return false;
    }

    /*

    public boolean ensureFreeInventorySlot() {
        if (inv().isInventoryFull()) {
            // Throw away!
            Slot toThrow = inv().getGarbageSlot();
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

*/
    public void refreshInventory() {
        for (int i = 0; i < InventoryTracker.INVENTORY_SIZE; ++i) {
            Slot slot = Slot.getFromInventory(i);
            clickSlot(slot, 0, SlotActionType.PICKUP);
            clickSlot(slot, 0, SlotActionType.PICKUP);
        }
    }

    private void forceSwapItems(Slot slot1, Slot slot2) {

        if (canDoSlotAction()) {
            // Pick up slot1
            if (!Slot.isCursor(slot1)) {
                clickSlot(slot1, 0, SlotActionType.PICKUP);
            }
            // Pick up slot2
            clickSlot(slot2, 0, SlotActionType.PICKUP);

            // slot 1 is now in slot 2
            // slot 2 is now in cursor

            // If slot 2 is not empty, move it back to slot 1
            //if (second != null && !second.isEmpty()) {
            if (Slot.isCursor(slot1)) {
                clickSlot(slot1, 0, SlotActionType.PICKUP);
            }
            inv().setDirty();
            registerSlotAction();
        }
        //}
    }
/*

    /**
     * @param from   Slot to start moving from
     * @param to     Slot to move items to
     * @param amount How many to move
     * @return The number of items successfully transported
     * /
    public int moveItems(Slot from, Slot to, int amount) {
        to.ensureWindowOpened();

        ItemStack fromStack = inv().getItemStackInSlot(from);

        if (fromStack == null || fromStack.isEmpty()) {
            Debug.logInternal("(From stack is empty or null)");
            return 0;
        }

        boolean moveFromCursor = Slot.isCursor(from);

        ItemStack toStack = inv().getItemStackInSlot(to);
        if (toStack != null && !toStack.isEmpty()) {
            if (!toStack.isItemEqual(fromStack)) {
                //Debug.logMessage("To was occupied, moved it elsewhere.");
                // We have stuff in our target slot. Move it out somewhere.
                clickSlot(to, SlotActionType.QUICK_MOVE);
                // If we're moving from a cursor slot, the cursor slot should already be moved to "to" after clicking.
                if (moveFromCursor) {
                    inv().setDirty();
                    return inv().getItemStackInSlot(from).getCount();
                }
            }
        }

        // Pickup
        ItemStack pickedUp;
        if (moveFromCursor) {
            // our item is already picked up.
            pickedUp = inv().getItemStackInSlot(from);
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
        inv().setDirty();
        return dropped;
    }


    public void grabItem(Slot slot) {
        clickSlot(slot, 1, SlotActionType.QUICK_MOVE);
    }

    public int moveItemToSlot(ItemTarget toMove, Slot moveTo) {
        for (Item item : toMove.getMatches()) {
            if (inv().getItemCount(item) >= toMove.getTargetCount()) {
                return moveItemToSlot(item, toMove.getTargetCount(), moveTo);
            }
        }
        return 0;
    }



    // These names aren't confusing
    public int moveItemToSlot(Item toMove, int moveCount, Slot moveTo) {
        List<Integer> itemSlots = inv().getInventorySlotsWithItem(toMove);
        int needsToMove = moveCount;
        for (Integer slotIndex : itemSlots) {
            if (needsToMove <= 0) break;
            Slot current = Slot.getFromInventory(slotIndex);
            //Debug.logStack();
            ItemStack stack = inv().getItemStackInSlot(current);
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



    public void moveToNonEquippedHotbar(Item item, int offset) {

        if (!inv().hasItem(item)) return;

        assert MinecraftClient.getInstance().player != null;
        int equipSlot = MinecraftClient.getInstance().player.inventory.selectedSlot;

        int otherSlot = (equipSlot + 1 + offset) % 9;

        int found = inv().getInventorySlotsWithItem(item).get(0);
        swapItems(Slot.getFromInventory(found), Slot.getFromInventory(otherSlot));
    }

    */
}

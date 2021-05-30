package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;

import java.util.List;

public class EquipCommand extends Command {
    public EquipCommand() throws CommandException {
        super("equip", "Equip an item or toggle armor equip", new Arg(String.class, "item"));
    }

    @Override
    protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
        String item = parser.Get(String.class);
        Item[] items = TaskCatalogue.getItemMatches(item);
        if (items == null || items.length == 0) {
            mod.logWarning("Item \"" + item + "\" not catalogued/not recognized.");
            finish();
            return;
        }
        boolean found = false;
        for (Item tryEquip : items) {
            if (mod.getInventoryTracker().hasItem(tryEquip)) {
                if (tryEquip instanceof ArmorItem) {
                    ArmorItem armor = (ArmorItem) tryEquip;
                    if (mod.getInventoryTracker().isArmorEquipped(armor)) {
                        // Ensure we have the player inventory accessible, not possible when another screen is open.
                        mod.getPlayer().closeHandledScreen();
                        // Deequip armor
                        //Debug.logInternal("DE-EQUIPPING ARMOR");
                        List<Integer> emptyInv = mod.getInventoryTracker().getEmptyInventorySlots();
                        if (emptyInv.size() == 0) {
                            mod.logWarning("Can't de-equip armor because inventory is full.");
                            finish();
                            return;
                        }
                        Slot targetEmpty = Slot.getFromInventory(emptyInv.get(0));
                        for (Slot armorSlot : PlayerSlot.ARMOR_SLOTS) {
                            if (mod.getInventoryTracker().getItemStackInSlot(armorSlot).getItem().equals(tryEquip)) {
                                found = true;
                                // armorSlot contains our armor.
                                // targetEmpty contains an empty spot.
                                assert targetEmpty != null;
                                mod.getInventoryTracker().moveItems(armorSlot, targetEmpty, 1);
                            }
                        }
                        //mod.getInventoryTracker().moveToNonEquippedHotbar(armor, 0);
                    } else {
                        // Equip armor
                        Slot toMove = PlayerSlot.getEquipSlot(armor.getSlotType());
                        if (toMove == null) {
                            Debug.logWarning("Invalid armor equip slot for item " + armor.getTranslationKey() + ": " + armor.getSlotType());
                        } else {
                            found = true;
                            mod.getInventoryTracker().moveItemToSlot(armor, 1, toMove);
                        }
                    }
                } else {
                    // Equip item
                    found = mod.getInventoryTracker().equipItem(tryEquip);
                }
                break;
            }
        }
        if (!found) {
            mod.logWarning("Failed to equip/deequip item: " + item);
        }
        finish();
    }
}
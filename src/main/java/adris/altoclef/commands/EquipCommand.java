package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.util.ItemTarget;
import net.minecraft.item.Items;

public class EquipCommand extends Command {
    /**
     * Constructor for this command.
     * @throws CommandException
     */
    public EquipCommand() throws CommandException {
        super("equip", "Equips armor", new Arg(ItemList.class,
                "[armors]"));
    }
    /**
     * Executes or runs a task when called. Accepts parameters from [parser]
     * @param mod Needs access to Minecraft
     * @param parser Grabs user input from command line
     * @throws CommandException
     */
    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        ItemTarget[] items = parser.get(ItemList.class).items;
        if(items[0].matches(Items.DIAMOND)){ //shortcut for full armor sets
            mod.runUserTask(new EquipArmorTask(Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_HELMET, Items.DIAMOND_BOOTS),this::finish);
        }else if(items[0].matches(Items.NETHERITE_INGOT)){
            mod.runUserTask(new EquipArmorTask(Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_HELMET, Items.NETHERITE_BOOTS),this::finish);
        }else if(items[0].matches(Items.IRON_INGOT)){
            mod.runUserTask(new EquipArmorTask(Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_HELMET, Items.IRON_BOOTS),this::finish);
        }else if(items[0].matches(Items.LEATHER)){
            mod.runUserTask(new EquipArmorTask(Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_HELMET, Items.LEATHER_BOOTS),this::finish);
        }else{
            for(ItemTarget item:items){ // Check for bad items
                if(!item.matches(Items.DIAMOND_CHESTPLATE) && !item.matches(Items.DIAMOND_LEGGINGS) && !item.matches(Items.DIAMOND_HELMET) && !item.matches(Items.DIAMOND_BOOTS) &&
                        !item.matches(Items.LEATHER_CHESTPLATE) && !item.matches(Items.NETHERITE_LEGGINGS) && !item.matches(Items.NETHERITE_HELMET) && !item.matches(Items.NETHERITE_BOOTS) &&
                        !item.matches(Items.IRON_CHESTPLATE) && !item.matches(Items.IRON_LEGGINGS) && !item.matches(Items.IRON_HELMET) && !item.matches(Items.IRON_BOOTS) &&
                        !item.matches(Items.NETHERITE_CHESTPLATE) && !item.matches(Items.LEATHER_LEGGINGS) && !item.matches(Items.LEATHER_HELMET) && !item.matches(Items.LEATHER_BOOTS)){
                    Debug.logMessage("Cannot do that with this item");
                }else{
                    mod.runUserTask(new EquipArmorTask(items), this::finish);

                }
            }
        }
    }
}

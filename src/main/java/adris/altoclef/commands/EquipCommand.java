package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.util.ItemTarget;

public class EquipCommand extends Command {
    public EquipCommand() throws CommandException {
        super("equip", "Equips armor", new Arg(ItemList.class, "[armors]"));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        ItemTarget[] items = parser.get(ItemList.class).items;
        mod.runUserTask(new EquipArmorTask(items), this::finish);
    }
}

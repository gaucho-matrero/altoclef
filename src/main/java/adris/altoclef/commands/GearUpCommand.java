package adris.altoclef.commands;
import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasks.misc.GatherStrongGearTask;
import adris.altoclef.tasks.movement.FollowPlayerTask;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Items;
import org.lwjgl.system.CallbackI;

public class GearUpCommand extends Command {

    public GearUpCommand() throws CommandException {
        super("gearup", "Gets all missing diamond tools/armor");
    }


    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.runUserTask(new GatherStrongGearTask(mod),this::finish);
    }
    }

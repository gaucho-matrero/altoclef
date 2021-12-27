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

//        if(mod.getInventoryTracker().hasItem(items1[0])){
//            if(mod.getInventoryTracker().hasItem(items2[0]) && mod.getInventoryTracker().hasItem(items2[1]) && mod.getInventoryTracker().hasItem(items2[2]) && mod.getInventoryTracker().hasItem(items2[3])) {
//                if (mod.getInventoryTracker().isArmorEquipped(Items.DIAMOND_HELMET
//                        , Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS,
//                        Items.DIAMOND_BOOTS)) {
//                    if (mod.getInventoryTracker().hasItem(items3[0]) && mod.getInventoryTracker().hasItem(items3[1]) && mod.getInventoryTracker().hasItem(items3[2])) {
//                        finish();
//                    } else {
//                        mod.runUserTask(TaskCatalogue.getSquashedItemTask(items3));
//                    }
//                }else{
//                    mod.runUserTask(new EquipArmorTask(Items.DIAMOND_CHESTPLATE,
//                            Items.DIAMOND_LEGGINGS, Items.DIAMOND_HELMET,
//                            Items.DIAMOND_BOOTS));
//                }
//            }else{
//                mod.runUserTask(TaskCatalogue.getSquashedItemTask(items2));
//            }
//        }else{
//            mod.runUserTask(TaskCatalogue.getSquashedItemTask(items1));
//        }

//            equipItems(mod);6
//        String username = parser.get(String.class);
//        if (username == null) {
//            if (mod.getButler().hasCurrentUser()) {
//                username = mod.getButler().getCurrentUser();
//            } else {
//                mod.logWarning("No butler user currently present. Running this command with no user argument can ONLY be done via butler.");
//                finish();
//                return;
//            }
//        }
//        mod.runUserTask(new FollowPlayerTask(username), this::finish);
    }

package adris.altoclef.commands;
import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.misc.EquipArmorTask;
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
        ItemTarget[] items1 = {new ItemTarget(Items.DIAMOND_PICKAXE)};
        ItemTarget[] items2 = {new ItemTarget(Items.DIAMOND_HELMET),
                new ItemTarget(Items.DIAMOND_CHESTPLATE),
                new ItemTarget(Items.DIAMOND_LEGGINGS),
                new ItemTarget(Items.DIAMOND_BOOTS)};

        ItemTarget[] items3 = {new ItemTarget(Items.DIAMOND_SWORD),
                new ItemTarget(Items.DIAMOND_SHOVEL),
                new ItemTarget(Items.DIAMOND_AXE),
                new ItemTarget(Items.DIAMOND_HOE)};

        //Get all three

        int TaskOrderVariable = (mod.getInventoryTracker().hasItem(items1[0])
                ?3:0)
                +(mod.getInventoryTracker().isArmorEquipped(Items.DIAMOND_HELMET
                ,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,
                Items.DIAMOND_BOOTS)?5:0)
                +(mod.getInventoryTracker().hasAllItems(Items.DIAMOND_HOE,
                Items.DIAMOND_SWORD,Items.DIAMOND_SHOVEL,Items.DIAMOND_AXE)?7
                :0); // Clever boolean check

        Debug.logMessage("Task Order Variable: " + TaskOrderVariable+"");
        switch(TaskOrderVariable){
            case 0 -> { // We have nothing...fuck
                    mod.runUserTask(TaskCatalogue.getItemTask(items1[0]), () -> {
                        mod.runUserTask(new EquipArmorTask(Items.DIAMOND_CHESTPLATE,
                                Items.DIAMOND_LEGGINGS, Items.DIAMOND_HELMET,
                                Items.DIAMOND_BOOTS), () -> {
                            mod.runUserTask(TaskCatalogue.getSquashedItemTask(items3)
                                    , this::finish);
                        });
                    });


            }
            case 3 -> { // We have only the pick
                    mod.runUserTask(new EquipArmorTask(Items.DIAMOND_CHESTPLATE,
                            Items.DIAMOND_LEGGINGS, Items.DIAMOND_HELMET,
                            Items.DIAMOND_BOOTS),() ->{
                        mod.runUserTask(TaskCatalogue.getSquashedItemTask(items3)
                                ,this::finish);
                    });

            }

            case 5 -> { // We have only the armor
                mod.runUserTask(TaskCatalogue.getItemTask(items1[0]),() ->{
                    mod.runUserTask(TaskCatalogue.getSquashedItemTask(items3)
                            ,this::finish);
                        });
            }

            case 7 -> { // We have tools but no pick and no armor.
                mod.runUserTask(TaskCatalogue.getItemTask(items1[0]),() ->{
                    mod.runUserTask(TaskCatalogue.getSquashedItemTask(items2),() -> {
                        mod.runUserTask(new EquipArmorTask(Items.DIAMOND_CHESTPLATE,
                                Items.DIAMOND_LEGGINGS, Items.DIAMOND_HELMET,
                                Items.DIAMOND_BOOTS),this::finish);
                        });
                    });
            }
            case 8 -> { //We have the pick and the armor but still need tools
                mod.runUserTask(TaskCatalogue.getSquashedItemTask(items3)
                        ,this::finish);
            }
            case 10 -> { // We have pick and tools but no armor
                mod.runUserTask(new EquipArmorTask(Items.DIAMOND_CHESTPLATE,
                        Items.DIAMOND_LEGGINGS, Items.DIAMOND_HELMET,
                        Items.DIAMOND_BOOTS),this::finish);
                        }

            case 12 -> { // We have no pick but somehow have everything else
                mod.runUserTask(TaskCatalogue.getItemTask(items1[0]),
                        this::finish);
            }
            }
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

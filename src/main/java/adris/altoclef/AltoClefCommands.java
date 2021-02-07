package adris.altoclef;

import adris.altoclef.commands.*;
import adris.altoclef.tasks.*;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.misc.*;
import adris.altoclef.tasks.misc.speedrun.BeatMinecraftTask;
import adris.altoclef.tasks.misc.speedrun.CollectBlazeRodsTask;
import adris.altoclef.tasks.misc.speedrun.ConstructNetherPortalSpeedrunTask;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasks.stupid.BeeMovieTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.slots.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// This structure was copied from a C# project. Fuck java. All my homies hate java.
@SuppressWarnings({"unchecked", "rawtypes"})
public class AltoClefCommands extends CommandList {

    private static void TEMP_TEST_FUNCTION(AltoClef mod, String arg) {
        //mod.runUserTask();
        Debug.logMessage("Running test...");

        /*
        mod.getBlockTracker().trackBlock(Blocks.CRAFTING_TABLE);
        BlockPos target = mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos());

        mod.getCustomBaritone().getInteractWithBlockPositionProcess().getToBlock(target, true);
        */
        //mod.runUserTask(new PickupDroppedItemTask(Arrays.asList(new ItemTarget(ItemTarget.LOG))));
        //mod.runUserTask(new MineAndCollectTask(Arrays.asList(new ItemTarget(ItemTarget.LOG))));
        ItemTarget B = new ItemTarget("planks");
        ItemTarget s = new ItemTarget("stick");
        ItemTarget o = null;
        CraftingRecipe testRecipe = CraftingRecipe.newShapedRecipe("wooden_pickaxe",new ItemTarget[]{B, B, B, o, s, o, o, s, o}, 1);
        ItemTarget targetItem = new ItemTarget(Items.WOODEN_PICKAXE, 1);
        CraftingRecipe testRecipe2 = CraftingRecipe.newShapedRecipe("wooden_sword",new ItemTarget[]{ o, B, o, o, B, o, o, s, o}, 1);
        ItemTarget targetItem2 = new ItemTarget(Items.WOODEN_SWORD, 1);

        switch (arg) {
            case "":
                // Idle
                mod.runUserTask(new IdleTask());
                break;
            case "sign":
                mod.runUserTask(new PlaceSignTask("Hello there!"));
                break;
            case "sign2":
                mod.runUserTask(new PlaceSignTask(new BlockPos(10, 3, 10),"Hello there!"));
                break;
            case "pickup":
                mod.runUserTask(new PickupDroppedItemTask(Collections.singletonList(new ItemTarget(Items.IRON_ORE, 3))));
                break;
            case "structure":
                mod.runUserTask(new PlaceStructureBlockTask(new BlockPos(10, 6, 10)));
                break;
            case "place": {
                BlockPos targetPos = new BlockPos(0, 6, 0);
                //mod.runUserTask(new PlaceSignTask(targetPos, "Hello"));
                Direction direction = Direction.UP;
                mod.runUserTask(new InteractItemWithBlockTask(TaskCatalogue.getItemTarget("lava_bucket", 1), direction, targetPos));
                //mod.runUserTask(new PlaceBlockNearbyTask(new Block[] {Blocks.GRAVEL}));
                break;
            }
            case "deadmeme":
                File file = new File("test.txt");
                try {
                    FileReader reader = new FileReader(file);
                    mod.runUserTask(new BeeMovieTask("bruh", mod.getPlayer().getBlockPos(), reader));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case "stacked":
                mod.runUserTask(new EquipArmorTask("diamond_chestplate", "diamond_leggings", "diamond_helmet", "diamond_boots"));
                break;
            case "smelt":
                ItemTarget target = new ItemTarget("iron_ingot", 4);
                ItemTarget material = new ItemTarget("iron_ore", 4);
                mod.runUserTask(new SmeltInFurnaceTask(Collections.singletonList(new SmeltTarget(target, material))));
                break;
            case "avoid":
                // Test block break predicate
                mod.getConfigState().avoidBlockBreaking((BlockPos b) -> (-1000 < b.getX() && b.getX() < 1000)
                        && (-1000 < b.getY() && b.getY() < 1000)
                        && (-1000 < b.getZ() && b.getZ() < 1000));
                Debug.logMessage("Testing avoid from -1000, -1000, -1000 to 1000, 1000, 1000");
                break;
            case "portal":
                mod.runUserTask(new EnterNetherPortalTask(new ConstructNetherPortalSpeedrunTask(), Dimension.NETHER));
                break;
            case "kill":
                List<ZombieEntity> zombs = mod.getEntityTracker().getTrackedMobs(ZombieEntity.class);
                if (zombs.size() == 0) {
                    Debug.logWarning("No zombs found.");
                } else {
                    LivingEntity entity = zombs.get(0);
                    mod.runUserTask(new KillEntityTask(entity));
                }
                break;
            case "equip":
                // Test de-equip
                new Thread() {
                    @Override
                    public void run() {
                        for (int i = 3; i > 0; --i) {
                            Debug.logMessage(i + "...");
                            sleepSec(1);
                        }

                        Item toEquip = Items.AIR;
                        Slot target = PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND);

                        // Already equipped
                        if (mod.getInventoryTracker().getItemStackInSlot(target).getItem() == toEquip) return;

                        List<Integer> itemSlots = mod.getInventoryTracker().getInventorySlotsWithItem(toEquip);
                        if (itemSlots.size() != 0) {
                            int slot = itemSlots.get(0);
                            swap(Slot.getFromInventory(slot), target);
                        }
                    }

                    private void swap(Slot slot1, Slot slot2) {
                        mod.getInventoryTracker().clickSlot(slot1);

                        Debug.logMessage("MOVE 1...");
                        sleepSec(1);
                        // Pick up slot2
                        ItemStack second = mod.getInventoryTracker().clickSlot(slot2);
                        Debug.logMessage("MOVE 2...");
                        sleepSec(1);

                        // slot 1 is now in slot 2
                        // slot 2 is now in cursor

                        // If slot 2 is not empty, move it back to slot 1
                        //if (second != null && !second.isEmpty()) {
                        mod.getInventoryTracker().clickSlot(slot1);
                        Debug.logMessage("MOVE 3!");
                    }

                    private void sleepSec(double seconds) {
                        try {
                            Thread.sleep((int) (1000 * seconds));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
                //mod.getInventoryTracker().equipItem(Items.AIR);
                break;
            case "food":
                mod.runUserTask(new CollectFoodTask(20));
                break;
            case "temple":
                mod.runUserTask(new SearchForDesertPyramidTask());
                break;
            case "blaze":
                mod.runUserTask(new CollectBlazeRodsTask(7));
                break;
        }
    }

    public AltoClefCommands(CommandExecutor executor) throws CommandException {
        super(executor,
            // List commands here
            new HelpCommand(),
            new GetCommand(),
            new StopCommand(),
            new TestCommand(),
            new GamerCommand()
            //new TestMoveInventoryCommand(),
            //    new TestSwapInventoryCommand()
        );
    }

    static class HelpCommand extends Command {

        public HelpCommand() {
            super("help", "Lists all commands");
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) {
            Debug.logMessage("########## HELP: ##########");
            int padSize = 10;
            for(Command c : mod.getCommandExecutor().AllCommands()) {
                StringBuilder line = new StringBuilder();
                //line.append("");
                line.append(c.getName());
                int toAdd = padSize - c.getName().length();
                for (int i = 0; i < toAdd; ++i) {
                    line.append(" ");
                }
                line.append(" ");
                line.append(c.getDescription());
                Debug.logMessage(line.toString());
            }
            Debug.logMessage("###########################");
        }
    }

    static class StopCommand extends Command {

        public StopCommand() {
            super("stop", "Stop task runner (stops all automation)");
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) {
            mod.getTaskRunner().disable();
        }
    }

    static class GetCommand extends Command {

        public GetCommand() throws CommandException {
            super("get", "Get an item/resource",
                    new Arg(String.class, "name"),
                    new Arg(Integer.class, "count", 1, 1));
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            String resourceName = parser.Get(String.class);
            int count = parser.Get(Integer.class);

            if (TaskCatalogue.taskExists(resourceName)) {
                Task targetTask = TaskCatalogue.getItemTask(resourceName, count);
                mod.runUserTask(targetTask);
            } else {
                Debug.logWarning("\"" + resourceName + "\" is not a catalogued resource. Can't get it yet, sorry! If it's a generic block try using baritone.");
                Debug.logWarning("Here's a list of everything we can get for you though:");
                Debug.logWarning(Arrays.toString(TaskCatalogue.resourceNames().toArray()));
            }
        }
    }

    static class GamerCommand extends Command {
        public GamerCommand() {
            super("gamer", "Beats the game");
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) {
            mod.runUserTask(new BeatMinecraftTask());
        }
    }

    static class FoodCommand extends Command {
        public FoodCommand() throws CommandException {
            super("food", "Collects a certain amount of food", new Arg(Integer.class, "count"));
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            mod.runUserTask(new CollectFoodTask(parser.Get(Integer.class)));
        }
    }

    static class TestCommand extends Command {

        public TestCommand() throws CommandException {
            super("test", "Generic command for testing", new Arg(String.class, "extra", "", 0));
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            TEMP_TEST_FUNCTION(mod, parser.Get(String.class));
        }
    }

    static class TestMoveInventoryCommand extends Command {

        public TestMoveInventoryCommand() throws Exception {
            super("testmoveinv", "Test command to move items around in inventory",
                    new Arg(Integer.class, "from"),
                    new Arg(Integer.class, "to"),
                    new Arg(Integer.class, "amount", 1, 2)
            );
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            int from = parser.Get(Integer.class);
            int to = parser.Get(Integer.class);
            int amount = parser.Get(Integer.class);

            int moved = mod.getInventoryTracker().moveItems(new PlayerSlot(from), new PlayerSlot(to), amount);
            Debug.logMessage("Successfully moved " + moved + " items.");
        }
    }
    static class TestSwapInventoryCommand extends Command {

        public TestSwapInventoryCommand() throws CommandException {
            super("testswapinv", "Test command to swap two slots in the inventory",
                    new Arg(Integer.class, "slot1"),
                    new Arg(Integer.class, "slot2")
            );
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            int slot1 = parser.Get(Integer.class);
            int slot2 = parser.Get(Integer.class);

            mod.getInventoryTracker().swapItems(new PlayerSlot(slot1), new PlayerSlot(slot2));
            Debug.logMessage("Successfully swapped.");
        }
    }
}

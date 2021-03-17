package adris.altoclef;

import adris.altoclef.butler.WhisperPriority;
import adris.altoclef.commands.*;
import adris.altoclef.tasks.*;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.misc.*;
import adris.altoclef.tasks.misc.speedrun.*;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasks.stupid.BeeMovieTask;
import adris.altoclef.tasks.stupid.TerminatorTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.slots.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.EmptyChunk;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
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
                mod.runUserTask(new PickupDroppedItemTask(new ItemTarget(Items.IRON_ORE, 3), true));
                break;
            case "chunk": {
                // We may have missed a chunk that's far away...
                BlockPos p = new BlockPos(100000, 3, 100000);
                Debug.logMessage("LOADED? " + (!(mod.getWorld().getChunk(p) instanceof EmptyChunk)));
                break;
            }
            case "structure":
                mod.runUserTask(new PlaceStructureBlockTask(new BlockPos(10, 6, 10)));
                break;
            case "place": {
                //BlockPos targetPos = new BlockPos(0, 6, 0);
                //mod.runUserTask(new PlaceSignTask(targetPos, "Hello"));
                //Direction direction = Direction.WEST;
                //mod.runUserTask(new InteractItemWithBlockTask(TaskCatalogue.getItemTarget("lava_bucket", 1), direction, targetPos, false));
                //mod.runUserTask(new PlaceBlockNearbyTask(new Block[] {Blocks.CRAFTING_TABLE}));
                mod.runUserTask(new PlaceStructureBlockTask(new BlockPos(472, 24, -324)));
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
                // It should only need:
                // 24 (armor) + 3*3 (pick) + 2 = 35 diamonds
                // 2*3 (pick) + 1 = 7 sticks
                // 4 planks
                mod.runUserTask(TaskCatalogue.getSquashedItemTask(
                        new ItemTarget("diamond_chestplate", 1),
                        new ItemTarget("diamond_leggings", 1),
                        new ItemTarget("diamond_helmet", 1),
                        new ItemTarget("diamond_boots", 1),
                        new ItemTarget("diamond_pickaxe", 3),
                        new ItemTarget("diamond_sword", 1),
                        new ItemTarget("crafting_table", 1)
                ));
                //mod.runUserTask(new EquipArmorTask("diamond_chestplate", "diamond_leggings", "diamond_helmet", "diamond_boots"));
                break;
            case "smelt":
                ItemTarget target = new ItemTarget("iron_ingot", 4);
                ItemTarget material = new ItemTarget("iron_ore", 4);
                mod.runUserTask(new SmeltInFurnaceTask(new SmeltTarget(target, material)));
                break;
            case "avoid":
                // Test block break predicate
                mod.getConfigState().avoidBlockBreaking((BlockPos b) -> (-1000 < b.getX() && b.getX() < 1000)
                        && (-1000 < b.getY() && b.getY() < 1000)
                        && (-1000 < b.getZ() && b.getZ() < 1000));
                Debug.logMessage("Testing avoid from -1000, -1000, -1000 to 1000, 1000, 1000");
                break;
            case "portal":
                mod.runUserTask(new EnterNetherPortalTask(new ConstructNetherPortalBucketTask(), Dimension.NETHER));
                break;
            case "kill":
                List<ZombieEntity> zombs = mod.getEntityTracker().getTrackedEntities(ZombieEntity.class);
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

                        Item toEquip = Items.BUCKET;//Items.AIR;
                        Slot target = PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND);

                        InventoryTracker t = mod.getInventoryTracker();

                        // Already equipped
                        if (t.getItemStackInSlot(target).getItem() == toEquip) {
                            Debug.logMessage("Already equipped.");
                        } else {
                            List<Integer> itemSlots = t.getInventorySlotsWithItem(toEquip);
                            if (itemSlots.size() != 0) {
                                int slot = itemSlots.get(0);
                                t.swapItems(Slot.getFromInventory(slot), target);
                                Debug.logMessage("Equipped via swap");
                            } else {
                                Debug.logWarning("Failed to equip item " + toEquip.getTranslationKey());
                            }
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

                }.start();
                //mod.getInventoryTracker().equipItem(Items.AIR);
                break;
            case "throw":
                new Thread(() -> {
                    for (int i = 3; i > 0; --i) {
                        Debug.logMessage(i + "...");
                        sleepSec(1);
                    }
                    mod.getControllerExtras().dropCurrentStack(true);
                }).start();
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
            case "flint":
                mod.runUserTask(new CollectFlintTask(5));
                break;
            case "unobtainable":
                String fname = "unobtainables.txt";
                try {
                    int unobtainable = 0;
                    int total = 0;
                    File f=new File(fname);
                    FileWriter fw = new FileWriter(f);
                    for (Identifier id : Registry.ITEM.getIds()) {
                        Item item = Registry.ITEM.get(id);
                        if (!TaskCatalogue.isObtainable(item)) {
                            ++unobtainable;
                            fw.write(item.getTranslationKey() + "\n");
                        }
                        total ++;
                    }
                    fw.flush();
                    fw.close();
                    Debug.logMessage(unobtainable + " / " + total + " unobtainable items. Wrote a list of items to \"" + f.getAbsolutePath() + "\".");
                } catch (IOException e) {
                    Debug.logWarning(e.toString());
                }
                break;
            case "piglin":
                mod.runUserTask(new TradeWithPiglinsTask(32, new ItemTarget(Items.ENDER_PEARL, 12)));
                break;
            case "throwaway":
                Slot toThrow = mod.getInventoryTracker().getGarbageSlot();
                if (toThrow != null) {
                    mod.getInventoryTracker().throwSlot(toThrow);
                    // Equip then throw
                    //mod.getInventoryTracker().equipSlot(toThrow);
                    //mod.getInventoryTracker().equipItem(mod.getInventoryTracker().getItemStackInSlot(toThrow).getItem());
                    /*int count = mod.getInventoryTracker().getItemStackInSlot(toThrow).getCount();
                    for (int i = 0; i < count; ++i) {
                        mod.getControllerExtras().dropCurrentStack(true);
                    }
                     */
                }
                break;
            case "stronghold":
                mod.runUserTask(new LocateStrongholdTask(12));
                break;
            case "terminate":
                mod.runUserTask(new TerminatorTask(mod.getPlayer().getBlockPos(), 400));
                break;
        }
    }

    public AltoClefCommands(CommandExecutor executor) throws CommandException {
        super(executor,
            // List commands here
            new HelpCommand(),
            new GetCommand(),
            new FollowCommand(),
            new GiveCommand(),
            new EquipCommand(),
            new GotoCommand(),
            new CoordsCommand(),
            new StatusCommand(),
            new InventoryCommand(),
            new StopCommand(),
            new TestCommand(),
            new FoodCommand(),
            new ReloadSettingsCommand(),
            new GamerCommand(),
            new PunkCommand()
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
            mod.log("########## HELP: ##########", WhisperPriority.OPTIONAL);
            int padSize = 10;
            for(Command c : mod.getCommandExecutor().AllCommands()) {
                StringBuilder line = new StringBuilder();
                //line.append("");
                line.append(c.getName()).append(": ");
                int toAdd = padSize - c.getName().length();
                for (int i = 0; i < toAdd; ++i) {
                    line.append(" ");
                }
                line.append(c.getDescription());
                mod.log(line.toString(), WhisperPriority.OPTIONAL);
            }
            mod.log("###########################", WhisperPriority.OPTIONAL);
            finish();
        }
    }

    static class StopCommand extends Command {

        public StopCommand() {
            super("stop", "Stop task runner (stops all automation)");
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) {
            mod.getTaskRunner().disable();
            finish();
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
                mod.runUserTask(targetTask, nothing -> finish());
            } else {
                mod.log("\"" + resourceName + "\" is not a catalogued resource. Can't get it yet, sorry! If it's a generic block try using baritone.", WhisperPriority.OPTIONAL);
                mod.log("Here's a list of everything we can get for you though:", WhisperPriority.OPTIONAL);
                mod.log(Arrays.toString(TaskCatalogue.resourceNames().toArray()), WhisperPriority.OPTIONAL);
                finish();
            }
        }
    }

    static class GamerCommand extends Command {
        public GamerCommand() {
            super("gamer", "Beats the game");
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) {
            mod.runUserTask(new BeatMinecraftTask(), nothing -> finish());
        }
    }

    static class ReloadSettingsCommand extends Command {
        public ReloadSettingsCommand() {super("reload_settings", "Reloads bot settings and butler whitelist/blacklist.");}
        @Override
        protected void Call(AltoClef mod, ArgParser parser) {
            mod.getButler().reloadLists();
            if (mod.reloadModSettings() != null) {
                mod.log("Reload successful!");
            } else {
                mod.logWarning("Failed to reload some settings. Check Minecraft log for Exception.");
            }
            finish();
        }
    }

    static class FoodCommand extends Command {
        public FoodCommand() throws CommandException {
            super("food", "Collects a certain amount of food", new Arg(Integer.class, "count"));
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            mod.runUserTask(new CollectFoodTask(parser.Get(Integer.class)), nothing -> finish());
        }
    }

    static class GiveCommand extends Command {
        public GiveCommand() throws CommandException {
            super("give", "Collects an item and gives it to you or someone else", new Arg(String.class, "username", null, 2), new Arg(String.class, "item"), new Arg(Integer.class, "count", 1, 1));
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            String username = parser.Get(String.class);
            if (username == null) {
                if (mod.getButler().hasCurrentUser()) {
                    username = mod.getButler().getCurrentUser();
                } else {
                    mod.logWarning("No butler user currently present. Running this command with no user argument can ONLY be done via butler.");
                    finish();
                    return;
                }
            }
            String item = parser.Get(String.class);
            int count = parser.Get(Integer.class);
            if (TaskCatalogue.taskExists(item)) {
                ItemTarget target = TaskCatalogue.getItemTarget(item, count);
                Debug.logMessage("USER: " + username + " : ITEM: " + item + " x " + count);
                mod.runUserTask(new GiveItemToPlayerTask(username, target), nothing -> finish());
            } else {
                mod.log("Task for item does not exist: " + item);
                finish();
            }
        }
    }

    static class FollowCommand extends Command {
        public FollowCommand() throws CommandException {
            super("follow", "Follows you or someone else", new Arg(String.class, "username", null, 0));
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            String username = parser.Get(String.class);
            if (username == null) {
                if (mod.getButler().hasCurrentUser()) {
                    username = mod.getButler().getCurrentUser();
                } else {
                    mod.logWarning("No butler user currently present. Running this command with no user argument can ONLY be done via butler.");
                    finish();
                    return;
                }
            }
            mod.runUserTask(new FollowPlayerTask(username), nothing -> finish());
        }
    }

    static class EquipCommand extends Command {
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

    static class StatusCommand extends Command {
        public StatusCommand() {
            super("status", "Get status of currently executing command");
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) {
            List<Task> tasks = mod.getUserTaskChain().getTasks();
            if (tasks.size() == 0) {
                mod.log("No tasks currently running.");
            } else {
                mod.log("CURRENT TASK: " + tasks.get(0).toString());
            }
            finish();
        }
    }
    static class CoordsCommand extends Command {
        public CoordsCommand() {
            super("coords", "Get bot's current coordinates");
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) {
            mod.log("CURRENT COORDINATES: " + mod.getPlayer().getBlockPos().toShortString() + " (Current dimension: " + mod.getCurrentDimension() + ")");
            finish();
        }
    }
    static class InventoryCommand extends Command {
        public InventoryCommand() throws CommandException {
            super("inventory", "Prints the bot's inventory OR returns how many of an item the bot has", new Arg(String.class, "item", null, 1));
        }
        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            String item = parser.Get(String.class);
            if (item == null) {
                // Print inventory
                // Get item counts
                HashMap<String, Integer> counts = new HashMap<>();
                for (int i = 0; i < mod.getPlayer().inventory.size(); ++i) {
                    ItemStack stack = mod.getPlayer().inventory.getStack(i);
                    if (!stack.isEmpty()) {
                        String name = stack.getItem().getTranslationKey();
                        if (!counts.containsKey(name)) counts.put(name, 0);
                        counts.put(name, counts.get(name) + stack.getCount());
                    }
                }
                // Print
                mod.log("INVENTORY: ", WhisperPriority.OPTIONAL);
                for (String name : counts.keySet()) {
                    mod.log(name + " : " + counts.get(name), WhisperPriority.OPTIONAL);
                }
                mod.log("(inventory list sent) ", WhisperPriority.OPTIONAL);
            } else {
                // Print item quantity
                Item[] matches = TaskCatalogue.getItemMatches(item);
                if (matches == null || matches.length == 0) {
                    mod.logWarning("Item \"" + item + "\" is not catalogued/recognized.");
                    finish();
                    return;
                }
                int count = mod.getInventoryTracker().getItemCount(matches);
                if (count == 0) {
                    mod.log(item + " COUNT: (none)");
                } else {
                    mod.log(item + " COUNT: " + count);
                }
            }
            finish();
        }
    }

    static class GotoCommand extends Command {
        private static final int EMPTY = -1;
        public GotoCommand() throws CommandException {
            super("goto", "Tell bot to travel to a set of coordinates.", new Arg(Integer.class, "x"), new Arg(Integer.class, "y", EMPTY, 2), new Arg(Integer.class, "z"));
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            int x = parser.Get(Integer.class),
                y = parser.Get(Integer.class),
                z = parser.Get(Integer.class);
            if (y != EMPTY) {
                mod.runUserTask(new GetToBlockTask(new BlockPos(x, y, z), false), nothing -> finish());
            } else {
                mod.runUserTask(new GetToXZTask(x, z), nothing -> finish());
            }
        }
    }


    static class TestCommand extends Command {

        public TestCommand() throws CommandException {
            super("test", "Generic command for testing", new Arg(String.class, "extra", "", 0));
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            TEMP_TEST_FUNCTION(mod, parser.Get(String.class));
            finish();
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
            finish();
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
            finish();
        }
    }

    static class PunkCommand extends Command {
        public PunkCommand() throws CommandException {
            super("punk", "Punk 'em", new Arg(String.class, "playerName"));
        }

        @Override
        protected void Call(AltoClef mod, ArgParser parser) throws CommandException {
            String playerName = parser.Get(String.class);
            mod.runUserTask(new KillPlayerTask(playerName), nothing -> finish());
        }
    }

    private static void sleepSec(double seconds) {
        try {
            Thread.sleep((int) (1000 * seconds));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

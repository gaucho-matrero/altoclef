package adris.altoclef;

import adris.altoclef.commands.*;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.commandsystem.CommandExecutor;
import adris.altoclef.commandsystem.CommandList;
import adris.altoclef.tasks.*;
import adris.altoclef.tasks.chest.StoreInAnyChestTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.examples.ExampleTask2;
import adris.altoclef.tasks.misc.*;
import adris.altoclef.tasks.misc.speedrun.*;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasks.stupid.BeeMovieTask;
import adris.altoclef.tasks.stupid.ReplaceBlocksTask;
import adris.altoclef.tasks.stupid.SCP173Task;
import adris.altoclef.tasks.stupid.TerminatorTask;
import adris.altoclef.util.*;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.EmptyChunk;

import java.io.*;
import java.util.List;

/// This structure was copied from a C# project. Fuck java. All my homies hate java.
public class AltoClefCommands extends CommandList {

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
            new LocateStructureCommand(),
            new StopCommand(),
            new TestCommand(),
            new FoodCommand(),
            new ReloadSettingsCommand(),
            new GamerCommand(),
            new PunkCommand(),
                new SetGammaCommand()
            //new TestMoveInventoryCommand(),
            //    new TestSwapInventoryCommand()
        );
    }

}

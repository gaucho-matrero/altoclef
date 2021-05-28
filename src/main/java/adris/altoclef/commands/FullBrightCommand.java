package adris.altoclef.commands;

import net.minecraft.client.MinecraftClient;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgBase;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;

public class FullBrightCommand extends Command
{

    public FullBrightCommand() throws CommandException
    {
        super("fullBright", "sets gamma really high", new Arg(String.class, "multiplier", "1", 0 ));
    }

    @Override protected void Call(AltoClef mod, ArgParser parser) throws CommandException
    {
        try{

            float multiplier = Float.parseFloat(parser.Get(String.class));
            MinecraftClient.getInstance().options.gamma = 1000 * multiplier;

        }catch (NumberFormatException e){
            Debug.logMessage("Only numbers!");
        }


    }


}

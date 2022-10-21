package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.mixins.GameOptionsAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

import static net.minecraft.client.option.GameOptions.getGenericValueText;

public class SetGammaCommand extends Command {

    public SetGammaCommand() throws CommandException {
        super("gamma", "sets the brightness to a value", new Arg(Double.class, "gamma", 1.0, 0));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        double gammaValue = parser.get(Double.class);
        Debug.logMessage("Gamma set to " + gammaValue);

        ((GameOptionsAccessor) MinecraftClient.getInstance().options).setGamma(new SimpleOption("options.gamma", SimpleOption.emptyTooltip(), (optionText, value) -> {return getGenericValueText(optionText, Text.translatable("options.gamma.min"));}, SimpleOption.DoubleSliderCallbacks.INSTANCE, gammaValue, (value) -> {}));
    }

}

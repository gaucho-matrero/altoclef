package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;

public class ReloadSettingsCommand extends Command {
    public ReloadSettingsCommand() {
        super("reload_settings", "Reloads bot settings and butler whitelist/blacklist.");
    }

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
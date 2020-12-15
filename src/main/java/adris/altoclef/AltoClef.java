package adris.altoclef;

import adris.altoclef.commands.AltoClefCommands;
import adris.altoclef.commands.CommandExecutor;
import baritone.api.event.events.ChatEvent;
import net.fabricmc.api.ModInitializer;

public class AltoClef implements ModInitializer {

    // Singleton pattern.
    // PLEASE AVOID RELYING ON THIS! The only place
    // where this is used is mixins. Later this will be removed entirely
    // once I figure out how to initialize mixins non-statically.
    private static AltoClef _instance;
    public static AltoClef getInstance() {
        return _instance;
    }

    private CommandExecutor _commandExecutor;

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // As such, nothing will be loaded here.
        _instance = this;
    }

    // This is the actual start point, controlled by a mixin.
    public void onInitializeLoad() {
        initializeCommands();
    }
    public void onChat(ChatEvent e) {
        String line = e.getMessage();
        System.out.println("LINE SENT: " + line);
        if (_commandExecutor.isClientCommand(line)) {
            e.cancel();
            try {
                _commandExecutor.Execute(line);
            } catch (Exception ex) {
                Debug.logWarning(ex.getMessage());
                //ex.printStackTrace();
            }
        }
    }

    private void initializeCommands() {
        _commandExecutor = new CommandExecutor("@", this);
        try {
            // This creates the commands. If you want any more commands feel free to initialize new command lists.
            new AltoClefCommands(_commandExecutor);
        } catch (Exception e) {
            /// ppppbbbbttt
            e.printStackTrace();
        }
    }
}

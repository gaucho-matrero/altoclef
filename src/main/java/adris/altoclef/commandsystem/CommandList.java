package adris.altoclef.commandsystem;

import java.security.InvalidKeyException;

public class CommandList {

    public CommandList(CommandExecutor executor, Command... commands) {
        for (Command c : commands) {
            try {
                executor.RegisterNewCommand(c);
            } catch (InvalidKeyException e) {
                // ppbbbbttt
                e.printStackTrace();
            }
        }
    }
}

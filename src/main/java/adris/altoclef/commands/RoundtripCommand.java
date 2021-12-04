package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.util.filestream.RoundtripMacroListFile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RoundtripCommand extends Command {
    protected static final Queue<String> queue = new ConcurrentLinkedQueue<>();
    private static boolean running = false;
    private static BlockPos startPos;
    private static String startDimension;
    private static final String EMPTY = "EMPTY";
    private static Map<String, List<String>> macros;
    private static Queue<String> macroQueue = new ConcurrentLinkedQueue<>();

    public RoundtripCommand() throws CommandException {
        //TODO: Do as in FillTargetChestCommand
        super("roundtrip", "Tell bot to travel to a set of coordinates.",
                new Arg(String.class, "subcommand", EMPTY, 1, false),
                new Arg(String.class, "arg3", EMPTY, 2, false),
                new Arg(String.class, "arg4", EMPTY, 3, false),
                new Arg(String.class, "arg5", EMPTY, 4, false),
                new Arg(String.class, "arg6", EMPTY, 5, false),
                new Arg(String.class, "arg7", EMPTY, 6, false)
        );
    }

    private void executeNextViaThread(final AltoClef mod) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1000);
                executeNext(mod);
            } catch (InterruptedException | CommandException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    private void executeNext(final AltoClef mod) throws CommandException {
        if (!queue.isEmpty() && running) {
            final String nextCommand = queue.poll();
            Debug.logMessage("Executing next task: " + nextCommand);
            mod.getCommandExecutor().execute(nextCommand, (nothing) -> {
                if (queue.isEmpty()) {
                    running = false;
                } else {
                    executeNextViaThread(mod);
                }
            });
        } else {
            running = false;
            return;
        }
    }

    private void start(final AltoClef mod) {
        startPos = MinecraftClient.getInstance().player.getBlockPos();
        startPos.add(0.5f, 0.5f, 0.5f);
        startDimension = MinecraftClient.getInstance().player.getEntityWorld().getRegistryKey().getValue().getPath();
        queue.add("@goto " + startPos.getX() + " " + startPos.getY() + " " + startPos.getZ() + " " + startDimension);
        running = true;
        Debug.logMessage("Starting roundtrip...");
        executeNextViaThread(mod);
    }

    private String getCommandSpecLine(final String[] list) {
        String command = "";
        for(int i = 1; i < list.length; i++) {
            if (command != "") {
                command += " ";
            }

            command += list[i];
        }

        return command;
    }

    private void addTreeToQueue(final String subcommand) {
        for (final String commandline : macros.get(subcommand)) {
            final String[] keywords = commandline.split(" ");
            if (keywords.length > 1 && macros.containsKey(keywords[1])) {
                if (keywords.length > 2) Debug.logMessage("too many parameter found for macro " + keywords[1]);
                addTreeToQueue(keywords[1]);
            } else {
                Debug.logMessage(commandline);
                queue.add("@" + commandline);
            }
        }
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        final String message = parser.get(String.class);
        if (message.isBlank() || message.isEmpty()) {
            Debug.logMessage("No parameter set.");
        }

        if (!RoundtripMacroListFile.isLoaded()) {
            macros = RoundtripMacroListFile.get(); //no need to remove "macros" since reference passed
        }

        String command = "@";
        String subcommand = "";

        if (parser.getArgUnits().length > 0) {
            subcommand = parser.getArgUnits()[0];
            command += subcommand;
        } else {
            Debug.logMessage("Missing arguments for roundtrip...");
            return;
        }

        if (macros.keySet().contains(subcommand)) {
            addTreeToQueue(subcommand);
            /*for (final String commandline : macros.get(subcommand)) {
                //Debug.logMessage(commandline);
                //queue.add("@" + commandline);
                addTreeToQueue(commandline);
            }*/

            /*
            for (Iterator<String> it = macros.get(subcommand).listIterator(); it.hasNext();) {
                String commandline = it.next();

                Debug.logMessage(commandline);
                queue.add("@" + commandline);
            }*/
            start(mod);
        } else if (subcommand.equals("start")) {
            start(mod);
        } else if (subcommand.equals("stop")) {
            running = false;
            Debug.logMessage("No new tasks will be started.");
        } else if (subcommand.equals("clear")) {
            queue.clear();
            Debug.logMessage("Tasks cleared!");
        } else if (subcommand.equals("reset")) {
            running = false;
            queue.clear();
            Debug.logMessage("Reset done!");
        } else if (subcommand.equals("list")) {
            Debug.logMessage("Queued tasks:");
            queue.forEach(e -> Debug.logMessage(e));
        } else if (subcommand.equals("push")) {
            String macro = getCommandSpecLine(parser.getArgUnits());
            macroQueue.add(macro);
            Debug.logMessage("New macro queued: " + macro);
        } else if (subcommand.equals("flush")) {
            macroQueue.clear();
            Debug.logMessage("Queue cleared out!");
        } else if (subcommand.equals("remove")) {
            macros.remove(parser.getArgUnits()[1]);
            RoundtripMacroListFile.overrideWithVirtual();
            Debug.logMessage("Removed macro " + parser.getArgUnits()[1]);
        } else if (subcommand.equals("flushas")) { //TODO: If more params are given, abort execution.
            final String macroName = parser.getArgUnits()[1];
            if (macros.keySet().contains(macroName)) {
                Debug.logError("Name already in use.");
                return;
            }
            final Map<String, Queue<String>> macroQueuedMap = new HashMap<>();
            macroQueuedMap.put(macroName, macroQueue);
            RoundtripMacroListFile.append(macroQueuedMap, true); // no need to clear queue
            Debug.logMessage("New macro created: " + macroName);
        } else {
            command += " " + getCommandSpecLine(parser.getArgUnits());
            Debug.logMessage("Milestone added: " + command);
            queue.add(command);
        }
    }
}

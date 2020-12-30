package adris.altoclef.tasks.stupid;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.CataloguedResourceTask;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.misc.PlaceSignTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes a stream input, like from a file, and places signs in a line that say the contents
 * of that stream. Use for absurd bullshit like making a bot that prints out the ENTIRE bee movie script
 * with signs.
 */
public class BeeMovieTask extends Task {

    private final BlockPos _start;
    private final BlockPos _direction = new BlockPos(0, 0, -1);

    private final StreamedSignStringParser _textParser;

    private final String _uniqueId;

    private boolean _finished = false;

    private List<String> _cachedStrings = new ArrayList<>();

    private PlaceSignTask _currentPlace = null;

    // Grab extra resources and acquire extra tools for speed
    private boolean _sharpenTheAxe = true;

    private Task _extraSignAcquireTask = null;

    public BeeMovieTask(String uniqueId, BlockPos start, InputStreamReader input) {
        _uniqueId = uniqueId;
        _start = start;
        _textParser = new StreamedSignStringParser(input);

        _extraSignAcquireTask = new CataloguedResourceTask(new ItemTarget("sign", 32));//TaskCatalogue.getItemTask("sign", 32);
    }

    @Override
    protected void onStart(AltoClef mod) {
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (_currentPlace != null && _currentPlace.isActive() && !_currentPlace.isFinished(mod)) {
            return _currentPlace;
        }

        // TODO: If we have no signs and an "Sharpen Your Axe Preparation" flag is set, get diamond axe (or try to).

        // NOTE: This only checks for the EXISTANCE of signs, NOT that they have the proper text.

        // Get building blocks
        if (!mod.getInventoryTracker().hasItem(Items.DIRT)) {
            return TaskCatalogue.getItemTask("dirt", 64);
        }

        if (_sharpenTheAxe) {
            setDebugState("Sharpening the axe (if you don't want this, disable it)");
            if (!mod.getInventoryTracker().hasItem(Items.DIAMOND_AXE) || !mod.getInventoryTracker().hasItem(Items.DIAMOND_PICKAXE)) {
                return new CataloguedResourceTask(new ItemTarget("diamond_axe", 1), new ItemTarget("diamond_pickaxe", 1));
            }
            if (_extraSignAcquireTask.isActive() && !_extraSignAcquireTask.isFinished(mod)) {
                return _extraSignAcquireTask;
            }
            if (!mod.getInventoryTracker().hasItem(ItemTarget.WOOD_SIGN)) {
                // Get a bunch of signs in bulk
                return _extraSignAcquireTask;
            }
        }

        int signCounter = 0;
        BlockPos currentSignPos = _start;
        while (true) {
            Debug.logMessage("loop: " + signCounter + " = " + currentSignPos);
            assert MinecraftClient.getInstance().world != null;

            /*
            //noinspection deprecation
            if (!MinecraftClient.getInstance().world.isChunkLoaded(currentSignPos)) {
                // We're not in the same chunk as this block, get there.
                return new GetToBlockTask(currentSignPos, false);
            }
             */

            BlockState blockAt = MinecraftClient.getInstance().world.getBlockState(currentSignPos);

            // Need a sign at this point.
            while (_cachedStrings.size() <= signCounter) {
                // Load up if we're at a new index.
                if (!_textParser.hasNextSign()) {
                    Debug.logMessage("DONE!!!!");
                    _finished = true;
                    return null;
                }
                String next = _textParser.getNextSignString();
                Debug.logMessage("NEXT SIGN: " + next);
                _cachedStrings.add(next);
            }


            if (!isSign(blockAt.getBlock())) {
                // INVALID! place.
                _currentPlace = new PlaceSignTask(currentSignPos, _cachedStrings.get(signCounter));
                return _currentPlace;
            }

            currentSignPos = currentSignPos.add(_direction);
            signCounter++;
        }
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Nothing
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof BeeMovieTask) {
            return ((BeeMovieTask)obj)._uniqueId.equals(_uniqueId);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Dead Meme \"" + _uniqueId + "\" at " + _start;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _finished;
    }

    private static boolean isSign(Block block) {
        if (block == null) return false;
        Block[] candidates = ItemTarget.WOOD_SIGNS_ALL;
        for(Block candidate : candidates) {
            if (block.is(candidate)) return true;
        }
        return false;
    }

    static class StreamedSignStringParser {
        private final BufferedReader _reader;

        private boolean _done = false;

        public StreamedSignStringParser(InputStreamReader source) {
            _reader = new BufferedReader(source);
        }

        public void open() {
            try {
                _reader.reset();
                _done = false;
            } catch (IOException e) {
                // Failed.
                e.printStackTrace();
            }
        }

        public boolean hasNextSign() {
            return !_done;
        }

        public String getNextSignString() {

            final double SIGN_TEXT_MAX_WIDTH = 90;
            int lineCount = 0;
            StringBuilder line = new StringBuilder();

            StringBuilder result = new StringBuilder();

            while (true) {
                int in;
                try {
                    _reader.mark(1);
                    in = _reader.read();
                } catch (IOException e) {
                    e.printStackTrace();
                    _done = true;
                    break;
                }
                if (in == -1) {
                    _done = true;
                    break;
                }
                char c = (char) in;
                Debug.logMessage("Read " + c);

                line.append(c);

                boolean done = false;

                // Can be a special delimiter for a new sign.
                if (c == '\0') done = true;

                if ( c == '\n' || MinecraftClient.getInstance().textRenderer.getWidth(line.toString()) > SIGN_TEXT_MAX_WIDTH) {
                    line.delete(0, line.length());
                    line.append(c);
                    lineCount++;
                    if (lineCount >= 4) {
                        // We're out of bounds. Put the last character back.
                        try {
                            _reader.reset();
                        } catch (IOException e) {
                            // Not much to do honestly...
                            e.printStackTrace();
                        }

                        done = true;
                    }
                }

                if (done) {
                    break;
                }

                result.append(c);
            }

            return result.toString();
        }
    }

}

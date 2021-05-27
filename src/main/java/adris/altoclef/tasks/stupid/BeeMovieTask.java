package adris.altoclef.tasks.stupid;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.CataloguedResourceTask;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.misc.PlaceSignTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.ItemUtil;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Takes a stream input, like from a file, and places signs in a line that say the contents of that stream. Use for absurd bullshit like
 * making a bot that prints out the ENTIRE bee movie script with signs. 😎
 */
public class BeeMovieTask extends Task {
    // How many building materials to collect/buffer up
    private static final int STRUCTURE_MATERIALS_BUFFER = 64;
    private final BlockPos start;
    private final BlockPos direction = new BlockPos(0, 0, -1);
    private final StreamedSignStringParser textParser;
    private final String uniqueId;
    private final Task extraSignAcquireTask;
    private final Task structureMaterialsTask;
    private final List<String> cachedStrings = new ArrayList<>();
    // Grab extra resources and acquire extra tools for speed
    private final boolean sharpenTheAxe = true;
    private boolean finished;
    private PlaceSignTask currentPlace;

    public BeeMovieTask(String uniqueId, BlockPos start, InputStreamReader input) {
        this.uniqueId = uniqueId;
        this.start = start;
        textParser = new StreamedSignStringParser(input);

        extraSignAcquireTask = new CataloguedResourceTask(new ItemTarget("sign", 256));//TaskCatalogue.getItemTask("sign", 32);
        structureMaterialsTask = new MineAndCollectTask(
                new ItemTarget(new Item[]{ Items.DIRT, Items.COBBLESTONE }, STRUCTURE_MATERIALS_BUFFER),
                new Block[]{ Blocks.STONE, Blocks.COBBLESTONE, Blocks.DIRT, Blocks.GRASS, Blocks.GRASS_BLOCK }, MiningRequirement.WOOD);
    }

    private static int sign(int num) {
        return Integer.compare(num, 0);
    }

    private static boolean isSign(Block block) {
        if (block == null) return false;
        Block[] candidates = ItemUtil.WOOD_SIGNS_ALL;
        for (Block candidate : candidates) {
            if (block.is(candidate)) return true;
        }
        return false;
    }

    // Whether a block pos is on the path of its signs.
    private boolean isOnPath(BlockPos pos) {
        BlockPos bottomStart = start.down();
        BlockPos delta = pos.subtract(bottomStart);
        return sign(delta.getX()) == sign(direction.getX()) && sign(delta.getY()) == sign(direction.getY()) && sign(delta.getZ()) == sign(
                direction.getZ());
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return finished;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getConfigState().push();
        // Prevent mineshaft garbage
        mod.getConfigState().setExclusivelyMineLogs(true);

        // Avoid breaking the ground below the signs.
        mod.getConfigState().avoidBlockBreaking(this::isOnPath);
        // Avoid placing blocks where the signs should be placed.
        mod.getConfigState().avoidBlockPlacing(block -> isOnPath(block.down()));
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (currentPlace != null && currentPlace.isActive() && !currentPlace.isFinished(mod)) {
            setDebugState("Placing...");
            return currentPlace;
        }

        if (sharpenTheAxe) {
            if (!mod.getInventoryTracker().hasItem(Items.DIAMOND_AXE) || !mod.getInventoryTracker().hasItem(Items.DIAMOND_SHOVEL) ||
                !mod.getInventoryTracker().hasItem(Items.DIAMOND_PICKAXE)) {
                setDebugState("Sharpening the axe: Tools");
                return new CataloguedResourceTask(new ItemTarget("diamond_axe", 1), new ItemTarget("diamond_shovel", 1),
                                                  new ItemTarget("diamond_pickaxe", 1));
            }
            if (extraSignAcquireTask.isActive() && !extraSignAcquireTask.isFinished(mod)) {
                setDebugState("Sharpening the axe: Signs");
                return extraSignAcquireTask;
            }
            if (!mod.getInventoryTracker().hasItem(ItemUtil.WOOD_SIGN)) {
                // Get a bunch of signs in bulk
                return extraSignAcquireTask;
            }
        }

        // Get building blocks
        int buildCount = mod.getInventoryTracker().getItemCount(Items.DIRT, Items.COBBLESTONE);
        if (buildCount < STRUCTURE_MATERIALS_BUFFER && (buildCount == 0 || structureMaterialsTask.isActive())) {
            setDebugState("Collecting structure blocks...");
            return structureMaterialsTask;
        }

        int signCounter = 0;
        // NOTE: This only checks for the EXISTANCE of signs, NOT that they have the proper text.
        BlockPos currentSignPos = start;
        while (true) {

            /*
            //noinspection deprecation
            if (!MinecraftClient.getInstance().world.isChunkLoaded(currentSignPos)) {
                // We're not in the same chunk as this block, get there.
                return new GetToBlockTask(currentSignPos, false);
            }
             */

            boolean loaded = mod.getChunkTracker().isChunkLoaded(currentSignPos);

            // Clear above
            BlockState above = Objects.requireNonNull(MinecraftClient.getInstance().world).getBlockState(currentSignPos.up());
            if (loaded && !above.isAir() && above.getBlock() != Blocks.WATER) {
                setDebugState("Clearing block above to prevent hanging...");
                return new DestroyBlockTask(currentSignPos.up());
            }

            // Fortify below
            //BlockState below = MinecraftClient.getInstance().world.getBlockState(currentSignPos.down());
            boolean cannotPlace = !WorldUtil.isSolid(mod, currentSignPos.down());
            //isSideSolidFullSquare(MinecraftClient.getInstance().world, currentSignPos.down(), Direction.UP);
            if (loaded && cannotPlace) {
                setDebugState("Placing block below for sign placement...");
                return new PlaceStructureBlockTask(currentSignPos.down());
            }

            // Need a sign at this point.
            while (cachedStrings.size() <= signCounter) {
                // Load up if we're at a new index.
                if (!textParser.hasNextSign()) {
                    Debug.logMessage("DONE!!!!");
                    finished = true;
                    return null;
                }
                String next = textParser.getNextSignString();
                Debug.logMessage("NEXT SIGN: " + next);
                cachedStrings.add(next);
            }


            BlockState blockAt = MinecraftClient.getInstance().world.getBlockState(currentSignPos);


            if (loaded && !isSign(blockAt.getBlock())) {
                // INVALID! place.
                currentPlace = new PlaceSignTask(currentSignPos, cachedStrings.get(signCounter));
                return currentPlace;
            }

            currentSignPos = currentSignPos.add(direction);
            signCounter++;
        }
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getConfigState().pop();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof BeeMovieTask) {
            return ((BeeMovieTask) obj).uniqueId.equals(uniqueId);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Dead Meme \"" + uniqueId + "\" at " + start;
    }


    public static class StreamedSignStringParser {
        private final BufferedReader _reader;
        private boolean notDone = true;

        StreamedSignStringParser(InputStreamReader source) {
            _reader = new BufferedReader(source);
        }

        public void open() {
            try {
                _reader.reset();
                notDone = true;
            } catch (IOException e) {
                // Failed.
                e.printStackTrace();
            }
        }

        public boolean hasNextSign() {
            return notDone;
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
                    notDone = false;
                    break;
                }
                if (in == -1) {
                    notDone = false;
                    break;
                }
                char c = (char) in;
                //Debug.logMessage("Read " + c);

                line.append(c);

                boolean done = c == '\0';

                // Can be a special delimiter for a new sign.

                if (c == '\n' || MinecraftClient.getInstance().textRenderer.getWidth(line.toString()) > SIGN_TEXT_MAX_WIDTH) {
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

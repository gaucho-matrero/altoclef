package adris.altoclef.tasks.stupid;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.PlayerExtraController;
import adris.altoclef.util.csharpisbetter.ActionListener;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Stack;


public class ReplaceBlocksTask extends Task {

    // We won't be asked to collect more materials than this at a single time.
    private static final int MAX_MATERIALS_NEEDED_AT_A_TIME = 64;
    private final Block[] toFind;
    private final ItemTarget toReplace;
    private final BlockPos from;
    private final BlockPos to;
    private final Stack<BlockPos> forceReplace = new Stack<>();
    private final ActionListener<PlayerExtraController.BlockBrokenEvent> blockBrokenListener
            = new ActionListener<PlayerExtraController.BlockBrokenEvent>() {
        @Override
        public void invoke(PlayerExtraController.BlockBrokenEvent evt) {
            if (evt.player.equals(MinecraftClient.getInstance().player)) {
                if (isWithinRange(evt.blockPos)) {
                    boolean wasAReplacable = Util.arrayContains(toFind, evt.blockState.getBlock());
                    if (wasAReplacable) {
                        Debug.logMessage("ADDED REPLACABLE FORCE: " + evt.blockPos);
                        forceReplace.push(evt.blockPos);
                    } else {
                        Debug.logMessage("Destroyed a non replacable block (delete this print if things are good lol)");
                    }
                } else {
                    Debug.logMessage("Not within range (TODO: DELETE THIS PRINT)");
                }
            } else {
                Debug.logMessage("INEQUAL PLAYER (delete this print if things are good lol)");
            }
        }
    };
    private Task collectMaterialsTask;
    private Task replaceTask;

    public ReplaceBlocksTask(ItemTarget toReplace, BlockPos from, BlockPos to, Block... toFind) {
        this.toFind = toFind;
        this.toReplace = toReplace;
        this.from = from;
        this.to = to;
    }

    public ReplaceBlocksTask(ItemTarget toReplace, Block... toFind) {
        this(toReplace, null, null, toFind);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getConfigState().push();
        mod.getConfigState().addProtectedItems(toReplace.getMatches());
        // TODO: Bug: We may want to replace a block that's considered a CONSTRUCTION block.
        // If that's the case, we are in trouble.

        mod.getBlockTracker().trackBlock(toFind);

        //_forceReplace.clear();

        mod.getControllerExtras().onBlockBroken.addListener(blockBrokenListener);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (collectMaterialsTask != null && collectMaterialsTask.isActive() && !collectMaterialsTask.isFinished(mod)) {
            setDebugState("Collecting materials...");
            return collectMaterialsTask;
        }

        if (replaceTask != null && replaceTask.isActive() && !replaceTask.isFinished(mod)) {
            setDebugState("Replacing a block");
            return replaceTask;
        }

        // Get to replace item
        if (!mod.getInventoryTracker().hasItem(toReplace.getMatches())) {
            List<BlockPos> locations = mod.getBlockTracker().getKnownLocations(toFind);
            int need = 0;
            for (BlockPos loc : locations) if (isWithinRange(loc) && need < MAX_MATERIALS_NEEDED_AT_A_TIME) need++;
            if (need == 0) {
                setDebugState("No replaceable blocks found, wandering.");
                return new TimeoutWanderTask();
            }
            collectMaterialsTask = TaskCatalogue.getItemTask(toReplace.getCatalogueName(), need);
            return collectMaterialsTask;
            //return TaskCatalogue.getItemTask(_toReplace);
        }

        Block[] blocksToPlace = Util.itemsToBlocks(toReplace.getMatches());

        // If we are forced to replace something we broke, do it now.
        while (!forceReplace.empty()) {
            BlockPos toReplace = forceReplace.pop();
            if (!Util.arrayContains(blocksToPlace, mod.getWorld().getBlockState(toReplace).getBlock())) {
                replaceTask = new PlaceBlockTask(toReplace, blocksToPlace);
                return replaceTask;
            }
        }

        // Now replace
        setDebugState("Searching for blocks to replace...");
        return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(), whereToPlace -> {
            replaceTask = new PlaceBlockTask(whereToPlace, blocksToPlace);
            return replaceTask;
        }, pos -> mod.getBlockTracker().getNearestTracking(pos, ignore -> !isWithinRange(ignore), toFind));
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getControllerExtras().onBlockBroken.removeListener(blockBrokenListener);
        mod.getConfigState().pop();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof ReplaceBlocksTask) {
            ReplaceBlocksTask task = (ReplaceBlocksTask) obj;
            return task.toReplace.equals(toReplace) && Util.arraysEqual(task.toFind, toFind);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Replacing " + Util.arrayToString(toFind) + " with " + toReplace;
    }

    private boolean isWithinRange(BlockPos pos) {
        if (from != null) {
            if (from.getX() > pos.getX() || from.getY() > pos.getY() || from.getZ() > pos.getZ()) {
                return false;
            }
        }
        if (to != null) {
            return to.getX() >= pos.getX() && to.getY() >= pos.getY() && to.getZ() >= pos.getZ();
        }
        return true;
    }
}

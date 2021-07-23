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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ReplaceBlocksTask extends Task {

    // We won't be asked to collect more materials than this at a single time.
    private static final int MAX_MATERIALS_NEEDED_AT_A_TIME = 64;

    private final Block[] _toFind;
    private final ItemTarget _toReplace;

    private final BlockPos _from;
    private final BlockPos _to;
    private final Deque<BlockPos> _forceReplace = new ArrayDeque<>();
    private final ActionListener<PlayerExtraController.BlockBrokenEvent> blockBrokenListener;
    private Task _collectMaterialsTask;
    private Task _replaceTask;

    public ReplaceBlocksTask(ItemTarget toReplace, BlockPos from, BlockPos to, Block... toFind) {
        _toFind = toFind;
        _toReplace = toReplace;
        _from = from;
        _to = to;

        blockBrokenListener = new ActionListener<>(evt -> {
            if (evt.player.equals(MinecraftClient.getInstance().player)) {
                if (isWithinRange(evt.blockPos)) {
                    boolean wasAReplacable = Util.arrayContains(_toFind, evt.blockState.getBlock());
                    if (wasAReplacable) {
                        Debug.logMessage("ADDED REPLACABLE FORCE: " + evt.blockPos);
                        _forceReplace.push(evt.blockPos);
                    } else {
                        Debug.logMessage("Destroyed a non replacable block (delete this print if things are good lol)");
                    }
                } else {
                    Debug.logMessage("Not within range (TODO: DELETE THIS PRINT)");
                }
            } else {
                Debug.logMessage("INEQUAL PLAYER (delete this print if things are good lol)");
            }
        });
    }

    public ReplaceBlocksTask(ItemTarget toReplace, Block... toFind) {
        this(toReplace, null, null, toFind);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(_toReplace.getMatches());
        // TODO: Bug: We may want to replace a block that's considered a CONSTRUCTION block.
        // If that's the case, we are in trouble.

        mod.getBlockTracker().trackBlock(_toFind);

        //_forceReplace.clear();

        mod.getControllerExtras().onBlockBroken.addListener(blockBrokenListener);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (_collectMaterialsTask != null && _collectMaterialsTask.isActive() && !_collectMaterialsTask.isFinished(mod)) {
            setDebugState("Collecting materials...");
            return _collectMaterialsTask;
        }

        if (_replaceTask != null && _replaceTask.isActive() && !_replaceTask.isFinished(mod)) {
            setDebugState("Replacing a block");
            return _replaceTask;
        }

        // Get to replace item
        if (!mod.getInventoryTracker().hasItem(_toReplace.getMatches())) {
            List<BlockPos> locations = mod.getBlockTracker().getKnownLocations(_toFind);
            int need = 0;
            for (BlockPos loc : locations) if (isWithinRange(loc) && need < MAX_MATERIALS_NEEDED_AT_A_TIME) need++;
            if (need == 0) {
                setDebugState("No replaceable blocks found, wandering.");
                return new TimeoutWanderTask();
            }
            _collectMaterialsTask = TaskCatalogue.getItemTask(_toReplace.getCatalogueName(), need);
            return _collectMaterialsTask;
            //return TaskCatalogue.getItemTask(_toReplace);
        }

        Block[] blocksToPlace = Util.itemsToBlocks(_toReplace.getMatches());

        // If we are forced to replace something we broke, do it now.
        while (!_forceReplace.isEmpty()) {
            BlockPos toReplace = _forceReplace.pop();
            if (!Util.arrayContains(blocksToPlace, mod.getWorld().getBlockState(toReplace).getBlock())) {
                _replaceTask = new PlaceBlockTask(toReplace, blocksToPlace);
                return _replaceTask;
            }
        }

        // Now replace
        setDebugState("Searching for blocks to replace...");
        return new DoToClosestBlockTask(
                () -> mod.getPlayer().getPos(), whereToPlace -> {
            _replaceTask = new PlaceBlockTask(whereToPlace, blocksToPlace);
            return _replaceTask;
        },
                pos -> mod.getBlockTracker().getNearestTracking(pos, ignore -> !isWithinRange(ignore), _toFind)
        );
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getControllerExtras().onBlockBroken.removeListener(blockBrokenListener);
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof ReplaceBlocksTask) {
            ReplaceBlocksTask task = (ReplaceBlocksTask) obj;
            return task._toReplace.equals(_toReplace) && Util.arraysEqual(task._toFind, _toFind);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Replacing " + Util.arrayToString(_toFind) + " with " + _toReplace;
    }

    private boolean isWithinRange(BlockPos pos) {
        if (_from != null) {
            if (_from.getX() > pos.getX() || _from.getY() > pos.getY() || _from.getZ() > pos.getZ()) {
                return false;
            }
        }
        if (_to != null) {
            return _to.getX() >= pos.getX() && _to.getY() >= pos.getY() && _to.getZ() >= pos.getZ();
        }
        return true;
    }
}

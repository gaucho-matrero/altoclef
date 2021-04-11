package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.LookUtil;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class PlaceBlockNearbyTask extends Task {

    // This would've been nice
    // Action<BlockPos, Block>
    //private Consumer<Pair<BlockPos, BlockState>> _onPlace;

    private final Block[] _toPlace;

    private AltoClef _mod;

    private boolean _placing;

    private final Timer _placeTimer = new Timer(5.0);

    private final Timer _randomRotationTimer = new Timer(0.5);

    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(2);

    public PlaceBlockNearbyTask(Block[] toPlace) {
        _toPlace = toPlace;
    }
    public PlaceBlockNearbyTask(Block toPlace) {
        this(new Block[] {toPlace});
    }

    @Override
    protected void onStart(AltoClef mod) {
        _mod = mod;

        mod.getCustomBaritone().getPlaceBlockNearbyProcess().place(_toPlace);
        //Debug.logInternal("DONE? %b %b", isFinished(mod), mod.getCustomBaritone().getPlaceBlockNearbyProcess().isActive() );
        _placeTimer.reset();
        _placing = true;
        _wanderTask.resetWander();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Baritone takes care of this.

        // If we're wandering, keep wandering.
        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            setDebugState("Timed out: Wandering");
            return _wanderTask;
        }

        // Random rotation
        if (mod.getControllerExtras().isBreakingBlock()) {
            _randomRotationTimer.reset();
        }
        if (_randomRotationTimer.elapsed()) {
            LookUtil.randomOrientation(mod);
            _randomRotationTimer.reset();
        }

        if (!mod.getCustomBaritone().getPlaceBlockNearbyProcess().isActive()) {
            _placeTimer.reset();
            mod.getCustomBaritone().getPlaceBlockNearbyProcess().place(_toPlace);
        }

        if (_placeTimer.elapsed()) {
            Debug.logMessage("Failed to place timeout. Wandering...");
            return _wanderTask;
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getCustomBaritone().getPlaceBlockNearbyProcess().onLostControl();
        _placing = false;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof PlaceBlockNearbyTask) {
            PlaceBlockNearbyTask other = (PlaceBlockNearbyTask) obj;
            return Util.arraysEqual(other._toPlace, _toPlace);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Place " + ItemTarget.trimItemName(_toPlace[0].getTranslationKey()) + " nearby";
    }

    // Also used to determine when we placed the block
    @Override
    public boolean isFinished(AltoClef mod) {
        return _placing && !mod.getCustomBaritone().getPlaceBlockNearbyProcess().isActive();// && mod.getCustomBaritone().getPlaceBlockNearbyProcess().placedBlock() != null;
    }

    // Used to determine where we placed the block
    public BlockPos getPlaced() {
        if (_mod == null) return null;
        return _mod.getCustomBaritone().getPlaceBlockNearbyProcess().placedBlock();
    }

}

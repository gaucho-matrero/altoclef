package adris.altoclef.tasks.stupid;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.PlaceBlockNearbyTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.PlaceBlockNearbySchematic;
import adris.altoclef.util.baritone.PlaceEverywhereSchematic;
import adris.altoclef.util.csharpisbetter.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class PlaceEverywhereTask extends Task {

    private Block[] _toPlace;

    private final Timer _placeTimer = new Timer(5.0);

    private boolean _placing = false;

    private final Task _wanderTask = new TimeoutWanderTask(2);

    private PlaceEverywhereSchematic _schematic;


    public PlaceEverywhereTask(Block[] toPlace) {
        _toPlace = toPlace;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Baritone takes care of this.

        // If we're wandering, keep wandering.
        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            setDebugState("Timed out: Wandering");
            _placing = false;
            return _wanderTask;
        }

        if (!_placing) {
            _placeTimer.reset();
            _placing = true;

            /*
            // Guess we gotta use pairs to pack things. Can't wait for eventually when I need to send three or more arguments.
            _onPlace = (Pair<BlockPos, BlockState> blockStateBlockPosPair) -> {
                if (blockStateBlockPosPair.getRight().getBlock().is(_toPlace)) {
                    // Our target block has been placed somewhere.
                    onFinishPlacing(blockStateBlockPosPair.getLeft());
                }
            };
             */

            //mod.getBlockTracker().getOnBlockPlace().addListener(_onPlace);

            BlockPos origin = mod.getPlayer().getBlockPos();

            _schematic = new PlaceEverywhereSchematic(_toPlace);

            mod.getClientBaritone().getBuilderProcess().build("Place " + _toPlace[0].getTranslationKey() + " nearby", _schematic, origin);
        }

        setDebugState("Placing...");

        // We're placing. Handle timeout and start wandering.
        if (_placeTimer.elapsed()) {
            Debug.logMessage("PlaceBlock PLACE TIMEOUT. Wandering.");
            return _wanderTask;
        }


        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof PlaceEverywhereTask) {
            PlaceEverywhereTask other = (PlaceEverywhereTask) obj;
            if (other._toPlace.length != _toPlace.length) return false;
            for (int i = 0; i < _toPlace.length; ++i) {
                if (!other._toPlace[i].is(_toPlace[i])) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Place some blocks everywhere idk";
    }
}

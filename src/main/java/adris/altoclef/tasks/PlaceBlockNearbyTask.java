package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.TaskCatalogue;
import adris.altoclef.util.baritone.PlaceBlockNearbySchematic;
import baritone.api.schematic.ISchematic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.function.Consumer;

public class PlaceBlockNearbyTask extends Task {

    // This would've been nice
    // Action<BlockPos, Block>
    private Consumer<Pair<BlockPos, BlockState>> _onPlace;

    private Block _toPlace;

    private boolean _finished;

    private AltoClef _mod;

    private BlockPos _placed;

    public PlaceBlockNearbyTask(Block toPlace) {
        _toPlace = toPlace;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _mod = mod;
        _finished = false;
        _placed = null;

        Debug.logMessage("PLACEBLOCK START " + this);

        // Guess we gotta use pairs to pack things. Can't wait for eventually when I need to send three or more arguments.
        _onPlace = (Pair<BlockPos, BlockState> blockStateBlockPosPair) -> {
            if (blockStateBlockPosPair.getRight().getBlock().is(_toPlace)) {
                // Our target block has been placed somewhere.
                onFinishPlacing(blockStateBlockPosPair.getLeft());
            }
        };

        mod.getBlockTracker().getOnBlockPlace().addListener(_onPlace);

        PlaceBlockNearbySchematic schematic = new PlaceBlockNearbySchematic(_toPlace);
        schematic.reset();

        Vec3i origin = mod.getPlayer().getBlockPos();
        mod.getClientBaritone().getBuilderProcess().build("Place " + _toPlace.getTranslationKey() + " nearby", schematic, origin);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Baritone takes care of this.
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().getOnBlockPlace().removeListener(_onPlace);
        _mod.getClientBaritone().getBuilderProcess().onLostControl();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof PlaceBlockNearbyTask) {
            PlaceBlockNearbyTask other = (PlaceBlockNearbyTask) obj;
            return other._toPlace.is(_toPlace);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Place " + ItemTarget.trimItemName(_toPlace.getTranslationKey()) + " nearby";
    }

    // Also used to determine when we placed the block
    @Override
    public boolean isFinished(AltoClef mod) {
        return _finished;
    }

    // Used to determine where we placed the block
    public BlockPos getPlaced() {
        return _placed;
    }

    private void onFinishPlacing(BlockPos placed) {
        _finished = true;
        //Debug.logMessage("TARGET BLOCK PLACED!");
        _mod.getClientBaritone().getBuilderProcess().onLostControl();
        _placed = placed;
    }

}

package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.baritone.PlaceBlockNearbySchematic;
import adris.altoclef.util.csharpisbetter.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class PlaceBlockNearbyTask extends Task {

    // This would've been nice
    // Action<BlockPos, Block>
    //private Consumer<Pair<BlockPos, BlockState>> _onPlace;

    private Block[] _toPlace;

    private boolean _finished;

    private AltoClef _mod;

    private BlockPos _placed;

    private final Timer _placeTimer = new Timer(5.0);

    private boolean _placing = false;

    private final Task _wanderTask = new TimeoutWanderTask(2);

    private PlaceBlockNearbySchematic _schematic;

    public PlaceBlockNearbyTask(Block[] toPlace) {
        _toPlace = toPlace;
    }
    public PlaceBlockNearbyTask(Block toPlace) {
        this(new Block[] {toPlace});
    }

    @Override
    protected void onStart(AltoClef mod) {
        _mod = mod;
        _finished = false;
        _placed = null;

        _placing = false;
        Debug.logInternal("PlaceBlock START!");

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

            _schematic = new PlaceBlockNearbySchematic(origin, _toPlace);
            _schematic.reset();

            mod.getClientBaritone().getBuilderProcess().build("Place " + _toPlace[0].getTranslationKey() + " nearby", _schematic, origin);
        }

        // We're placing. Check if we successfully placed the block.
        if (_schematic.foundSpot()) {
            setDebugState("Spot found!");
            BlockPos shouldBePlacedHere = _schematic.getFoundSpot();
            assert MinecraftClient.getInstance().world != null;
            BlockState state = MinecraftClient.getInstance().world.getBlockState(shouldBePlacedHere);
            //Debug.logMessage("(delete this lol) TARGET POS: " + shouldBePlacedHere + ", " + (state != null? state.getBlock().getTranslationKey() : "(null)"));
            if (state != null && isCorrectBlock(state.getBlock())) {
                // We good!
                onFinishPlacing(shouldBePlacedHere);
                return null;
            }
        } else {
            setDebugState("Placing...");
        }

        // We're placing. Handle timeout and start wandering.
        if (!_finished && _placeTimer.elapsed()) {
            Debug.logMessage("PlaceBlock PLACE TIMEOUT. Wandering.");
            return _wanderTask;
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        _mod.getClientBaritone().getBuilderProcess().onLostControl();
        _placing = false;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof PlaceBlockNearbyTask) {
            PlaceBlockNearbyTask other = (PlaceBlockNearbyTask) obj;
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
        return "Place " + ItemTarget.trimItemName(_toPlace[0].getTranslationKey()) + " nearby";
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
        _placed = placed;
        _mod.getClientBaritone().getBuilderProcess().onLostControl();
        assert MinecraftClient.getInstance().world != null;
        _mod.getBlockTracker().addBlock(MinecraftClient.getInstance().world.getBlockState(placed).getBlock(), placed);
    }

    private boolean isCorrectBlock(Block block) {
        for (Block check : _toPlace) {
            if (check.is(block)) return true;
        }
        return false;
    }

}

package adris.altoclef.util.baritone;

import baritone.api.schematic.AbstractSchematic;
import baritone.api.schematic.ISchematic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.List;

//@Deprecated
public class PlaceBlockSchematic extends AbstractSchematic {

    private static final int RANGE = 1;

    private boolean _done;
    private final Block[] _blockToPlace;
    private BlockState _targetPlace;

    private boolean _skipIfAlreadyThere;

    public PlaceBlockSchematic(Block[] blocksToPlace, boolean skipIfAlreadyThere) {
        super(RANGE, RANGE, RANGE);
        _blockToPlace = blocksToPlace;
        _done = false;
        _targetPlace = null;
        _skipIfAlreadyThere = skipIfAlreadyThere;
    }
    public PlaceBlockSchematic(Block[] blocksToPlace) {
        this(blocksToPlace, true);
    }

    public PlaceBlockSchematic(Block blockToPlace) {
        this(new Block[] {blockToPlace});
    }

    /*
    public void reset() {
        _targetPlace = null;
    }
     */

    public boolean foundSpot() {
        return _targetPlace != null;
    }

    // No restrictions.
    //@Override
    //public boolean inSchematic(int x, int y, int z, BlockState currentState) {
    //    return true;
    //}

    @Override
    public BlockState desiredState(int x, int y, int z, BlockState blockState, List<BlockState> list) {
        // Only place at the origin.
        if (x != 0 || y != 0 || z != 0) {
            return blockState;
        }
        // If a block already exists there, place it.
        if (_skipIfAlreadyThere && blockIsTarget(blockState.getBlock())) {
            //System.out.println("PlaceBlockNearbySchematic (already exists)");
            _targetPlace = blockState;
        }
        boolean isDone = (_targetPlace != null);
        if (isDone) {
            return _targetPlace;
        }
        //System.out.print("oof: [");
        for (BlockState possible : list) {
            //System.out.print(possible.getBlock().getTranslationKey() + " ");
            if (blockIsTarget(possible.getBlock())) {
                //System.out.print("PlaceBlockNearbySchematic  ( FOUND! )");
                _targetPlace = possible;
                return possible;
            }
        }
        //System.out.println("] ( :(((((( )");
        return blockState;
    }


    private boolean blockIsTarget(Block block) {
        for (Block check : _blockToPlace) {
            if (check.is(block)) return true;
        }
        return false;
    }
}

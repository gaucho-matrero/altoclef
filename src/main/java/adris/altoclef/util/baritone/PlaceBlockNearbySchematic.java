package adris.altoclef.util.baritone;

import baritone.api.schematic.AbstractSchematic;
import baritone.api.schematic.ISchematic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class PlaceBlockNearbySchematic extends AbstractSchematic {

    private static final int RANGE = 10;

    private boolean _done;
    private final Block _blockToPlace;

    private BlockPos _targetPos;
    private BlockState _targetPlace;

    public PlaceBlockNearbySchematic(Block blockToPlace) {
        super(RANGE, RANGE, RANGE);
        _blockToPlace = blockToPlace;
        _done = false;
    }

    public void reset() {
        _done = false;
    }

    // No restrictions.
    //@Override
    //public boolean inSchematic(int x, int y, int z, BlockState currentState) {
    //    return true;
    //}

    @Override
    public BlockState desiredState(int x, int y, int z, BlockState blockState, List<BlockState> list) {
        // If a block already exists there, place it.
        if (blockState.getBlock().is(_blockToPlace)) {
            _done = true;
        }
        if (_done) {
            if (_targetPos.getX() == x && _targetPos.getY() == y && _targetPos.getZ() == z) {
                return _targetPlace;
            }
            return blockState;
        }
        //System.out.print("oof: [");
        for (BlockState possible : list) {
            //System.out.print(possible.getBlock().getTranslationKey() + " ");
            if (possible.getBlock().is(_blockToPlace)) {
                //System.out.print("] ( FOUND! )");
                _done = true;
                _targetPos = new BlockPos(x, y, z);
                _targetPlace = possible;
                return possible;
            }
        }
        //System.out.println("] ( :(((((( )");
        return blockState;
    }
}

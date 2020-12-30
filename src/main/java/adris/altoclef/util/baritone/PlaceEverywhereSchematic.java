package adris.altoclef.util.baritone;

import baritone.api.schematic.AbstractSchematic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

import java.util.List;

/// Kinda chaotic garbage, will need to figure out how to make this actually useful.
public class PlaceEverywhereSchematic extends AbstractSchematic {

    private static final int RANGE = 5;

    private Block[] _toPlace;

    public PlaceEverywhereSchematic(Block[] toPlace) {
        super(RANGE, RANGE, RANGE);
        _toPlace = toPlace;
    }

    @Override
    public boolean inSchematic(int x, int y, int z, BlockState currentState) {
        return true; // wreap havoc boys
    }

    @Override
    public BlockState desiredState(int i, int i1, int i2, BlockState blockState, List<BlockState> list) {




        for (BlockState possible : list) {
            if (possible != null && isValid(possible.getBlock())) return possible;
        }

        return blockState;
    }

    private boolean isValid(Block block) {
        for (Block check : _toPlace) {
            if (check.is(block)) return true;
        }
        return false;
    }
}

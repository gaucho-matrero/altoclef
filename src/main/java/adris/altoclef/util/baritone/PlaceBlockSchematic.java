package adris.altoclef.util.baritone;

import baritone.api.schematic.AbstractSchematic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

import java.util.List;

//@Deprecated
public class PlaceBlockSchematic extends AbstractSchematic {

    private static final int RANGE = 1;
    private final Block[] _blockToPlace;
    private final boolean _skipIfAlreadyThere;
    private final boolean _done;
    private BlockState _targetPlace;

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
        this(new Block[]{blockToPlace});
    }


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
        if (!list.isEmpty()) {
            for (BlockState possible : list) {
                if (possible == null) {
                /*
                if (ToolSet.areShearsEffective(blockState.getBlock()) || BlockTags.FLOWERS.contains(blockState.getBlock())) {
                    // Sheering items/flowers results in this issue, but it works fine!
                } else {
                    Debug.logWarning("Weird issue, given possible state is null. Will ignore.");
                }
                 */
                    continue;
                }
                //System.out.print(possible.getBlock().getTranslationKey() + " ");
                if (blockIsTarget(possible.getBlock())) {
                    //System.out.print("PlaceBlockNearbySchematic  ( FOUND! )");
                    _targetPlace = possible;
                    return possible;
                }
            }
        }
        //System.out.println("] ( :(((((( )");
        return blockState;
    }


    private boolean blockIsTarget(Block block) {
        if (_blockToPlace != null) {
            for (Block check : _blockToPlace) {
                if (check == block) return true;
            }
        }
        return false;
    }
}

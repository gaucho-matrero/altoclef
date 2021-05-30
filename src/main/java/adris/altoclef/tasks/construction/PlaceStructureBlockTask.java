package adris.altoclef.tasks.construction;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class PlaceStructureBlockTask extends PlaceBlockTask {

    public PlaceStructureBlockTask(BlockPos target) {
        super(target, new Block[]{}, true, true);
    }
}
package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

public class LocateDesertTempleTask extends Task {

    private BlockPos _finalPos;

    @Override
    protected void onStart(AltoClef mod) {
        // Track desert pyramid blocks
        mod.getBlockTracker().trackBlock(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        BlockPos desertTemplePos = desertTemplePosOrNull(mod);
        if (desertTemplePos != null) {
            _finalPos = desertTemplePos;
        }
        if (_finalPos != null) {
            setDebugState("Going to found desert temple");
            return new GetToBlockTask(_finalPos, false);
        }
        return new SearchWithinBiomeTaks(Biome.Category.DESERT);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof LocateDesertTempleTask;
    }

    @Override
    protected String toDebugString() {
        return "Searchin' for temples";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getPlayer().getBlockPos().equals(_finalPos);
    }

    private BlockPos desertTemplePosOrNull(AltoClef mod) {
        for (BlockPos pos : mod.getBlockTracker().getKnownLocations(Blocks.STONE_PRESSURE_PLATE)) {
            if (b(mod, pos.down()) == Blocks.CUT_SANDSTONE &&
                    b(mod, pos.down().down()) == Blocks.TNT) {
                // 14 blocks up is where the teracotta is.
                return pos.add(0, 14, 0);
            }
        }
        return null;
    }

    private Block b(AltoClef mod, BlockPos pos) {
        return mod.getWorld().getBlockState(pos).getBlock();
    }

}

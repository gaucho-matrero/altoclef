package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.PlaceBlockSchematic;
import baritone.api.schematic.AbstractSchematic;
import baritone.api.schematic.ISchematic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;

public class PlaceStructureBlockTask extends Task {

    private final BlockPos _target;

    public PlaceStructureBlockTask(BlockPos target) {
        _target = target;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!mod.getClientBaritone().getBuilderProcess().isActive()) {

            ISchematic schematic = new PlaceStructureSchematic(mod);

            mod.getClientBaritone().getBuilderProcess().build("structure", schematic, _target);
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getBuilderProcess().onLostControl();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof PlaceStructureBlockTask) {
            PlaceStructureBlockTask task = (PlaceStructureBlockTask) obj;
            return task._target.equals(_target);
        }
        return false;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        assert MinecraftClient.getInstance().world != null;
        return MinecraftClient.getInstance().world.getBlockState(_target).isSolidBlock(MinecraftClient.getInstance().world, _target);
    }

    @Override
    protected String toDebugString() {
        return "Place structure at " + _target.toShortString();
    }


    private static class PlaceStructureSchematic extends AbstractSchematic {

        private final AltoClef _mod;


        public PlaceStructureSchematic(AltoClef mod) {
            super(1, 1, 1);
            _mod = mod;
        }

        @Override
        public BlockState desiredState(int x, int y, int z, BlockState blockState, List<BlockState> available) {
            if (x == 0 && y == 0 && z == 0) {
                // Place!!
                for (BlockState possible : available) {
                    if ( _mod.getClientBaritoneSettings().acceptableThrowawayItems.value.contains(possible.getBlock().asItem())) {
                        return possible;
                    }
                }
                Debug.logInternal("Failed to find throwaway block");
                // No throwaways available!!
                Optional<BlockState> o = available.stream().findAny();
                if (o.isPresent()) return o.get();
                Debug.logInternal("Failed to find ANY block");
                // No blocks available period!
                return blockState;
            }
            // Don't care.
            return blockState;
        }
    }
}

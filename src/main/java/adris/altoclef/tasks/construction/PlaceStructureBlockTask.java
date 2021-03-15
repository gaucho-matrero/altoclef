package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.progresscheck.LinearProgressChecker;
import baritone.api.schematic.AbstractSchematic;
import baritone.api.schematic.ISchematic;
import baritone.api.utils.BlockOptionalMeta;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class PlaceStructureBlockTask extends Task {

    private final BlockPos _target;

    private LinearProgressChecker _distanceChecker = new LinearProgressChecker(5, 0.1);
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(6);

    private Task _materialTask;

    private int MIN_MATERIALS = 16;
    private int PREFERRED_MATERIALS = 32;

    public PlaceStructureBlockTask(BlockPos target) {
        _target = target;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _distanceChecker.setProgress(Double.NEGATIVE_INFINITY);
        _distanceChecker.reset();
        _wanderTask.resetWander();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // Perform timeout wander
        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            setDebugState("Wandering.");
            _distanceChecker.reset();
            return _wanderTask;
        }

        if (_materialTask != null && _materialTask.isActive() && !_materialTask.isFinished(mod)) {
            setDebugState("No structure items, collecting cobblestone + dirt as default.");
            if (getMaterialCount(mod) < PREFERRED_MATERIALS) {
                return _materialTask;
            } else {
                _materialTask = null;
            }
        }

        //Item[] items = Util.toArray(Item.class, mod.getClientBaritoneSettings().acceptableThrowawayItems.value);
        if (getMaterialCount(mod) < MIN_MATERIALS) {
            // TODO: Mine items, extract their resource key somehow.
            _materialTask = TaskCatalogue.getSquashedItemTask(new ItemTarget("dirt", PREFERRED_MATERIALS), new ItemTarget("cobblestone", PREFERRED_MATERIALS));
            return _materialTask;
        }

        // Perform baritone placement
        if (!mod.getClientBaritone().getBuilderProcess().isActive()) {
            Debug.logInternal("Run Structure Build");
            ISchematic schematic = new PlaceStructureSchematic(mod);
            mod.getClientBaritone().getBuilderProcess().build("structure", schematic, _target);
        }

        // Check if we're approaching our point. If we fail, wander for a bit.
        double sqDist = mod.getPlayer().squaredDistanceTo(_target.getX(), _target.getY(), _target.getZ());
        _distanceChecker.setProgress(-1 * sqDist);
        if (_distanceChecker.failed()) {
            Debug.logMessage("Failed to place, wandering timeout.");
            return _wanderTask;
        }

        setDebugState("Letting baritone place a block.");
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
        return WorldUtil.isSolid(mod, _target);
    }

    @Override
    protected String toDebugString() {
        return "Place structure at " + _target.toShortString();
    }

    private static int getMaterialCount(AltoClef mod) {
        return mod.getInventoryTracker().getItemCount(Items.DIRT, Items.COBBLESTONE);
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
                    if (possible == null) continue;
                    if ( _mod.getClientBaritoneSettings().acceptableThrowawayItems.value.contains(possible.getBlock().asItem())) {
                        return possible;
                    }
                }
                Debug.logInternal("Failed to find throwaway block");
                // No throwaways available!!
                return new BlockOptionalMeta(Blocks.COBBLESTONE).getAnyBlockState();
            }
            // Don't care.
            return blockState;
        }
    }
}

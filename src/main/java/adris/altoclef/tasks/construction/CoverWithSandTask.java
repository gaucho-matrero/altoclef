package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Predicate;

public class CoverWithSandTask extends Task {
    private static final TimerGame _timer = new TimerGame(30);
    private static final Task _getSand = TaskCatalogue.getItemTask(Items.SAND, 128);
    private static final Task _goToNether = new DefaultGoToDimensionTask(Dimension.NETHER);
    private static final Task _goToOverworld = new DefaultGoToDimensionTask(Dimension.OVERWORLD);
    private BlockPos _lavaPos;

    @Override
    protected void onStart(AltoClef mod) {
        _timer.reset();
        mod.getBlockTracker().trackBlock(Blocks.LAVA);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_getSand != null && _getSand.isActive() && !_getSand.isFinished(mod)) {
            setDebugState("Getting sands to cover nether lava.");
            _timer.reset();
            return _getSand;
        }
        if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD &&
                mod.getItemStorage().getItemCount(Items.SAND) < 64) {
            return _getSand;
        }
        if (WorldHelper.getCurrentDimension() == Dimension.OVERWORLD &&
                mod.getItemStorage().getItemCount(Items.SAND) > 64) {
            setDebugState("Going to nether.");
            _timer.reset();
            return _goToNether;
        }
        if (WorldHelper.getCurrentDimension() == Dimension.NETHER &&
                !mod.getItemStorage().hasItem(Items.SAND)) {
            setDebugState("Going to overworld to get sand.");
            _timer.reset();
            return _goToOverworld;
        }
        if (coverLavaWithSand(mod) == null) {
            setDebugState("Searching valid lava.");
            _timer.reset();
            return new TimeoutWanderTask();
        }
        setDebugState("Covering lava with sand");
        return coverLavaWithSand(mod);
    }

    private Task coverLavaWithSand(AltoClef mod) {
        Predicate<BlockPos> validLava = blockPos ->
                WorldHelper.isAir(mod, blockPos.up()) &&
                        (!WorldHelper.isBlock(mod, blockPos.north(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(mod, blockPos.south(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(mod, blockPos.east(), Blocks.LAVA) ||
                                !WorldHelper.isBlock(mod, blockPos.west(), Blocks.LAVA));
        Optional<BlockPos> lava = mod.getBlockTracker().getNearestTracking(validLava, Blocks.LAVA);
        if (lava.isPresent()) {
            if (_lavaPos == null) {
                _lavaPos = lava.get();
            }
            if (_timer.elapsed()) {
                _lavaPos = lava.get();
                _timer.reset();
            }
            if (!WorldHelper.isBlock(mod, _lavaPos, Blocks.LAVA)) {
                _lavaPos = lava.get();
            }
            if (!WorldHelper.isBlock(mod, _lavaPos.down(), Blocks.LAVA)) {
                return new PlaceBlockTask(_lavaPos, Blocks.SAND);
            }
            return new PlaceBlockTask(_lavaPos.up(), Blocks.SAND);
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.LAVA);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof CoverWithSandTask;
    }

    @Override
    protected String toDebugString() {
        return "Covering nether lava with sand";
    }
}

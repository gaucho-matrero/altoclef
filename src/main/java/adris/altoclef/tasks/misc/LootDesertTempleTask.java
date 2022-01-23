package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.container.LootContainerTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.trackers.storage.ContainerType;
import adris.altoclef.util.helpers.StorageHelper;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.List;
import java.util.Optional;

public class LootDesertTempleTask extends Task {
    private final BlockPos _temple;
    private final List<Item> _wanted;
    private Task _lootTask;
    private Task _interactTask;
    private short _looted = 0;
    private boolean _isLooting = false;
    public final Vec3i[] CHEST_POSITIONS_RELATIVE = {
            new Vec3i(2, 0, 0),
            new Vec3i(-2, 0, 0),
            new Vec3i(0, 0, 2),
            new Vec3i(0, 0, -2)
    };

    public LootDesertTempleTask(BlockPos temple, List<Item> wanted) {
        _temple = temple;
        _wanted = wanted;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getClientBaritoneSettings().blocksToAvoid.value.add(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_interactTask != null) {
            if (!ContainerType.screenHandlerMatches(ContainerType.CHEST)) return _interactTask;
            _interactTask = null;
            _looted++;
        }
        if (_lootTask != null && !_lootTask.isFinished(mod)) {
            setDebugState("Looting a desert temple chest");
            return _lootTask;
        }
        for (Item lootable : _wanted) {
            Optional<ContainerCache> closest = mod.getItemStorage().getClosestContainerWithItem(mod.getPlayer().getPos(), lootable);
            if (closest.isPresent()) {
                setDebugState("Looting a desert temple chest");
                _isLooting = true;
                _lootTask = new LootContainerTask(closest.get().getBlockPos(), lootable);
                return _lootTask;
            }
        }
        _isLooting = false;
        if (mod.getWorld().getBlockState(_temple).getBlock() == Blocks.STONE_PRESSURE_PLATE) {
            setDebugState("Breaking pressure plate");
            return new DestroyBlockTask(_temple);
        }
        if (_looted == 4) {
            setDebugState("Why is this still running?");
            return null;
        }
        StorageHelper.closeScreen();
        setDebugState("Interacting with chest");
        _interactTask = new InteractWithBlockTask(_temple.add(CHEST_POSITIONS_RELATIVE[_looted]));
        return _interactTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task task) {
        mod.getClientBaritoneSettings().blocksToAvoid.value.remove(Blocks.STONE_PRESSURE_PLATE);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof LootDesertTempleTask && ((LootDesertTempleTask) other).getTemplePos() == _temple;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _looted == 4 && !_isLooting;
    }

    @Override
    protected String toDebugString() {
        return "Looting Desert Temple";
    }

    public BlockPos getTemplePos() {
        return _temple;
    }
}
package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.slot.MoveInaccessibleItemToInventoryTask;
import adris.altoclef.tasks.slot.MoveItemToSlotFromContainerTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class DumpInContainerTask extends AbstractDoToStorageContainerTask {

    private final BlockPos _targetContainer;
    private final String _uniqueId;
    private final Function<AltoClef, Optional<ItemTarget>> _getNextItemTargetToDump;
    private final Function<AltoClef, Task> _onFullContainer;

    private boolean _finished = false;

    public DumpInContainerTask(BlockPos targetContainer, String uniqueId, Function<AltoClef, Optional<ItemTarget>> getNextItemTargetToDump, Function<AltoClef, Task> onFullContainer) {
        _targetContainer = targetContainer;
        _uniqueId = uniqueId;
        _getNextItemTargetToDump = getNextItemTargetToDump;
        _onFullContainer = onFullContainer;
    }

    @Override
    protected Optional<BlockPos> getContainerTarget() {
        return Optional.of(_targetContainer);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // TODO: calculate how much we must have.
        // neededInInventory = amount in chest - actual chest targets (grab from cache if null)
        // If we don't have `neededInInventory`, get the item.

        // Make sure our next item is accessible in our inventory
        Optional<ItemTarget> toDump = _getNextItemTargetToDump.apply(mod);
        if (toDump.isPresent() && StorageHelper.isItemInaccessibleToContainer(mod, toDump.get())) {
            return new MoveInaccessibleItemToInventoryTask(toDump.get());
        }

        return super.onTick(mod);
    }

    @Override
    protected Task onContainerOpenSubtask(AltoClef mod, ContainerCache containerCache) {
        Optional<ItemTarget> toMove = _getNextItemTargetToDump.apply(mod);
        if (toMove.isPresent()) {
            ItemTarget target = toMove.get();
            setDebugState("Dumping " + target);
            _finished = false;
            // Grab the item from the current chest that most closely matches our requirements
            List<Slot> potentials = mod.getItemStorage().getSlotsWithItemPlayerInventory(false, target.getMatches());

            // Pick the best slot to grab from.
            Optional<Slot> bestPotential = PickupFromContainerTask.getBestSlotToTransfer(
                    mod,
                    target,
                    mod.getItemStorage().getItemCountContainer(target.getMatches()),
                    potentials,
                    stack -> mod.getItemStorage().getSlotThatCanFitInOpenContainer(stack, false).isPresent());
            if (bestPotential.isPresent()) {
                ItemStack stackIn = StorageHelper.getItemStackInSlot(bestPotential.get());
                Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInOpenContainer(stackIn, false);
                if (toMoveTo.isEmpty()) {
                    return _onFullContainer.apply(mod);
                }
                setDebugState("Moving to slot...");
                return new MoveItemToSlotFromContainerTask(target, toMoveTo.get());
            }
            setDebugState("SHOULD NOT HAPPEN! No valid items detected.");
        } else {
            _finished = true;
        }
        return null;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _finished;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DumpInContainerTask task) {
            return Objects.equals(task._targetContainer, _targetContainer) && Objects.equals(task._uniqueId, _uniqueId);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Storing in container[" + _targetContainer.toShortString() + "] (" + _uniqueId + ")";
    }
}

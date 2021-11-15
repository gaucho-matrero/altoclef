package adris.altoclef.tasks.chest;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.slot.MoveItemToSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.ContainerTracker;
import adris.altoclef.util.Blacklist;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.TaskDataPackage;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.slots.ChestSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.Item;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class StoreInTargetChestTask extends AbstractDoInChestTask {

    private final ItemTarget _targets;
    private final TimerGame _actionTimer = new TimerGame(0);
    private final BlockPos _targetChest;
    private TaskDataPackage taskDataPackage;
    private boolean finished;
    private final Map<ItemTarget, Integer> goalChestData;
    private final Map<ItemTarget, Integer> feedbackProgressData;
    private boolean chestDataCollected;
    private boolean stillSpace;
    private int targetItemIndexCounter;

    public StoreInTargetChestTask(BlockPos targetChest, TaskDataPackage taskDataPackage, ItemTarget targets) {
        this(targetChest, targets);
        this.taskDataPackage = taskDataPackage;
    }

    public StoreInTargetChestTask(BlockPos targetChest, ItemTarget targets) {
        super(targetChest);
        _targets = targets;
        _targetChest = targetChest;
        finished = false;
        this.taskDataPackage = new TaskDataPackage();
        goalChestData = new HashMap<>();
        chestDataCollected = false;

        feedbackProgressData = new HashMap<>();
        this.taskDataPackage.putExtra(TaskDataPackage.ExtraData.PROGRESS_DATA, feedbackProgressData);
        this.stillSpace = true;
        this.targetItemIndexCounter = 0;
    }

    /*
    private void handleInit(final AltoClef mod, final ContainerTracker.ChestData data) {
        goalChestData.clear();
        feedbackProgressData.clear();
        Arrays.stream(_targets).forEach(target -> {
            Arrays.stream(target.getMatches()).forEach(match -> {
                final int countInChest = data.getItemCount(match);
                final int inventoryCount = mod.getInventoryTracker().getItemCount(target);
                final int transferableCount = (target.getTargetCount() > inventoryCount)
                        ? inventoryCount : target.getTargetCount();
                goalChestData.put(target, countInChest + transferableCount);
            });
        });
    }*/

    @Override
    protected Task doToOpenChestTask(AltoClef mod, GenericContainerScreenHandler handler) {
        _actionTimer.setInterval(mod.getModSettings().getContainerItemMoveDelay());
        if (_actionTimer.elapsed()) {
            _actionTimer.reset();

            final Map<Item, Integer> complementaryMap = new HashMap<>();

            if (taskDataPackage.isFinished()) {
                return null;
            }

            final ContainerTracker.ChestData data = mod.getContainerTracker().getChestMap().getCachedChestData(_targetChest);

            if (isActuallyFull(data, handler)) {
                taskDataPackage.setFeedback(TaskDataPackage.Feedback.CONTAINER_FULL);
                Blacklist.removeBlacklisted(_targetChest);
            }

            int end = data.isBig() ? 53 : 26;
            for (int slot = 0; slot <= end; ++slot) {
                net.minecraft.screen.slot.Slot cSlot = handler.getSlot(slot);

                for (final Item match : _targets.getMatches()) {
                    List<Slot> inventorySlotsWithItem = mod.getInventoryTracker().getInventorySlotsWithItem(match);
                    final int stillCanGet = cSlot.getStack().getMaxCount() - cSlot.getStack().getCount();

                    if (stillCanGet > 0 && !inventorySlotsWithItem.isEmpty()) {
                        if (!cSlot.hasStack() || cSlot.getStack().isEmpty() || (_targets.matches(cSlot.getStack().getItem()))) {
                            Slot slotTo = new ChestSlot(slot, data.isBig());
                            int invCount = mod.getInventoryTracker().getItemCount(match);
                            int toMove = Math.min(stillCanGet, invCount);
                            return new MoveItemToSlotTask(new ItemTarget(match, toMove), slotTo);
                        }
                    }
                }

                final int additional = cSlot.getStack().getMaxCount() - cSlot.getStack().getCount();
                Item item = cSlot.getStack().getItem();

                if (item instanceof AirBlockItem) {
                    if (_targets.getMatches().length < 1) {
                        throw new IllegalStateException("No target specified...");
                    }

                    if (targetItemIndexCounter >= _targets.getMatches().length) {
                        targetItemIndexCounter = 0;
                    }

                    item = _targets.getMatches()[targetItemIndexCounter++];
                }

                if (complementaryMap.containsKey(item)) {
                    complementaryMap.put(item, complementaryMap.get(item) + additional);
                } else {
                    complementaryMap.put(item, additional);
                }

            }

            taskDataPackage.putExtra(TaskDataPackage.ExtraData.PROGRESS_DATA, complementaryMap);
            taskDataPackage.setFinished(true);
            chestDataCollected = false;
        }
        return null;
    }

    /**
     * TODO: If inventory item count fits chest slots and the remaining space < default sourcing count, then sourcing count should equal space count, but its just getting it all right now.
     */
    private int getChestItemCountModulo(final ContainerTracker.ChestData data, final Item item) {
        return data.getItemCount(item) % item.getMaxCount();
    }

    /**
     * TODO: Please undo loop dupes... I am sure this function can be calculated in the loops of doToOpenChestTask
     * */
    private boolean isActuallyFull(final ContainerTracker.ChestData data, final GenericContainerScreenHandler handler) {
        if (data.isFull()) {
            int end = data.isBig() ? 53 : 26;
            for (int slot = 0; slot <= end; ++slot) {
                net.minecraft.screen.slot.Slot cSlot = handler.getSlot(slot);
                if (hasItem(cSlot.getStack().getItem(), _targets)) {
                    int spaceCount = cSlot.getStack().getMaxCount() - cSlot.getStack().getCount();
                    if (spaceCount > 0) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private boolean hasItem(final Item slotItem, final ItemTarget target) {
        return target.matches(slotItem);
        /*for (final ItemTarget goalTarget : _targets) {
            if (goalTarget.matches(slotItem)) {
                return true;
            }
        }
        return false;*/
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return taskDataPackage.isFinished();
    }

    @Override
    protected boolean isSubEqual(AbstractDoInChestTask other) {
        if (other instanceof StoreInTargetChestTask task) {
            //return Arrays.equals(task._targets, _targets);
            return task._targets == _targets;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Storing in chest: " + _targets;
    }
}

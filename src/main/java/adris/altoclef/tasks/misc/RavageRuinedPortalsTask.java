package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.container.LootContainerTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerType;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Blocks;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RavageRuinedPortalsTask extends Task {
    private List<BlockPos> _notRuinedPortalChests = new ArrayList<>();
    private Task _lootTask;
    private Task _interactTask;
    private boolean _inContainer = false;
    private BlockPos _currentContainer;

    private final Item[] LOOT = {
            Items.IRON_NUGGET,
            Items.FLINT,
            Items.OBSIDIAN,
            Items.FIRE_CHARGE,
            Items.FLINT_AND_STEEL,
            Items.GOLD_NUGGET
    };

    public RavageRuinedPortalsTask() {

    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBlockTracker().trackBlock(Blocks.CHEST);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if(_lootTask != null && !_lootTask.isFinished(mod)) {
            return _lootTask;
        }
        if(ContainerType.screenHandlerMatches(ContainerType.CHEST)) {
            _interactTask = null;
            for (Item lootable : LOOT) {
                if (mod.getItemStorage().getContainerAtPosition(_currentContainer).get().hasItem(lootable)) {
                    setDebugState("Looting this chest of " + lootable.toString());
                    return new LootContainerTask(_currentContainer, lootable);
                }
            }
            _inContainer = false;
            _currentContainer = null;
        }
        if(_interactTask != null) {
            return _interactTask;
        }
        Optional<BlockPos> closest = locateClosestUnopenedRuinedPortalChest(mod);
        if (closest.isPresent()) {
            _currentContainer = closest.get();
            _interactTask = new InteractWithBlockTask(closest.get());
            _inContainer = true;
            setDebugState("Ruined portal chest found, interacting...");
            return _interactTask;
        }
        return new TimeoutWanderTask();
    }

    @Override
    protected void onStop(AltoClef mod, Task task) {
        mod.getBlockTracker().stopTracking(Blocks.CHEST);
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task task) {
        return task instanceof RavageRuinedPortalsTask;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Ravaging Ruined Portals";
    }

    private boolean canBeLootablePortalChest(AltoClef mod, BlockPos blockPos) {
        if (mod.getWorld().getBlockState(blockPos.up(1)).getBlock() == Blocks.WATER || blockPos.getY() < 50) {
            return false;
        }
        for (BlockPos check : WorldHelper.scanRegion(mod, blockPos.add(-4, -2, -4), blockPos.add(4, 2, 4))) {
            Debug.logMessage("WE GOT HERE BUT WHAT THE HECK");
            if (mod.getWorld().getBlockState(check).getBlock() == Blocks.NETHERRACK) {
                return true;
            }
        }
        _notRuinedPortalChests.add(blockPos);
        return false;
    }

    boolean isChestNotOpened(AltoClef mod, BlockPos pos) {
        return mod.getItemStorage().getContainerAtPosition(pos).isEmpty();
    }

    private Optional<BlockPos> locateClosestUnopenedRuinedPortalChest(AltoClef mod) {
        if (WorldHelper.getCurrentDimension() != Dimension.OVERWORLD) {
            return Optional.empty();
        }
        Debug.logMessage("Chests: " + mod.getBlockTracker().getKnownLocations(Blocks.CHEST).size());
        return mod.getBlockTracker().getNearestTracking(blockPos -> !_notRuinedPortalChests.contains(blockPos) && isChestNotOpened(mod, blockPos) && canBeLootablePortalChest(mod, blockPos), Blocks.CHEST);
    }
}

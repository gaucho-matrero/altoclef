package adris.altoclef.trackers.storage;

import adris.altoclef.Debug;
import adris.altoclef.trackers.Tracker;
import adris.altoclef.trackers.TrackerManager;
import adris.altoclef.util.Dimension;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.function.Predicate;

/**
 * Keeps track of items in containers
 */
public class ContainerSubTracker extends Tracker {

    private boolean _containerOpen;
    private BlockPos _lastBlockPosInteraction;
    private Block _lastBlockInteraction;
    private final HashMap<Dimension, HashMap<BlockPos, ContainerCache>> _containerCaches = new HashMap<>();
    private ContainerCache _enderChestCache;

    public ContainerSubTracker(TrackerManager manager) {
        super(manager);
        for (Dimension dimension : Dimension.values()) {
            _containerCaches.put(dimension, new HashMap<>());
        }
    }

    public void onBlockInteract(BlockPos pos, Block block) {
        if (block instanceof AbstractFurnaceBlock ||
            block instanceof ChestBlock ||
            block.equals(Blocks.ENDER_CHEST) ||
            block instanceof HopperBlock ||
            block instanceof ShulkerBoxBlock ||
            block instanceof DispenserBlock) {
            _lastBlockPosInteraction = pos;
            _lastBlockInteraction = block;
        }
    }
    public void onScreenOpenFirstTick(final Screen screen) {
        _containerOpen = screen instanceof FurnaceScreen
                || screen instanceof GenericContainerScreen
                || screen instanceof SmokerScreen
                || screen instanceof BlastFurnaceScreen
                || screen instanceof HopperScreen
                || screen instanceof ShulkerBoxScreen;
    }
    public void onScreenClose() {
        _containerOpen = false;
        _lastBlockPosInteraction = null;
        _lastBlockInteraction = null;
    }
    public void onServerTick() {
        if (MinecraftClient.getInstance().player == null)
            return;
        if (_containerOpen && _lastBlockPosInteraction != null && _lastBlockInteraction != null) {
            BlockPos containerPos = _lastBlockPosInteraction;
            ScreenHandler handler = MinecraftClient.getInstance().player.currentScreenHandler;
            if (handler == null)
                return;

            HashMap<BlockPos, ContainerCache> dimCache = _containerCaches.get(_mod.getCurrentDimension());

            // Container Type Mismatch, reset.
            if (dimCache.containsKey(containerPos)) {
                ContainerType currentType = dimCache.get(containerPos).getContainerType();
                if (!ContainerType.screenHandlerMatches(currentType, handler)) {
                    Debug.logMessage("Mismatched container screen at " + containerPos.toShortString() + ", will overwrite container data: " + handler.getType() + " ?=> " + currentType);
                    dimCache.remove(containerPos);
                }
            }

            // New container found
            if (!dimCache.containsKey(containerPos)) {
                Block containerBlock = _lastBlockInteraction;
                ContainerType interactType = ContainerType.getFromBlock(containerBlock);
                ContainerCache newCache = new ContainerCache(_mod.getCurrentDimension(), containerPos, interactType);
                dimCache.put(containerPos, newCache);
                // Special ender chest cache
                if (interactType == ContainerType.ENDER_CHEST) {
                    _enderChestCache = newCache;
                }
            }

            ContainerCache toUpdate = dimCache.get(containerPos);
            toUpdate.update(handler, stack -> {

            });
        }
    }

    public Optional<ContainerCache> getContainerAtPosition(Dimension dimension, BlockPos pos) {
        return Optional.ofNullable(_containerCaches.get(dimension).getOrDefault(pos, null));
    }
    public Optional<ContainerCache> getContainerAtPosition(BlockPos pos) {
        return getContainerAtPosition(_mod.getCurrentDimension(), pos);
    }
    public Optional<ContainerCache> getEnderChestStorage() {
        return Optional.ofNullable(_enderChestCache);
    }

    public List<ContainerCache> getCachedContainers(Predicate<ContainerCache> accept) {
        List<ContainerCache> result = new ArrayList<>();
        for (HashMap<BlockPos, ContainerCache> map : _containerCaches.values()) {
            for (ContainerCache cache : map.values()) {
                if (accept.test(cache))
                    result.add(cache);
            }
        }
        return result;
    }
    public List<ContainerCache> getCachedContainers(ContainerType ...types) {
        Set<ContainerType> typeSet = new HashSet<ContainerType>(Arrays.asList(types));
        return getCachedContainers(cache -> typeSet.contains(cache.getContainerType()));
    }

    public Optional<ContainerCache> getClosestTo(Vec3d pos, Predicate<ContainerCache> accept) {
        double bestDist = Double.POSITIVE_INFINITY;
        ContainerCache bestCache = null;
        for (ContainerCache cache : _containerCaches.get(_mod.getCurrentDimension()).values()) {
            double dist = cache.getBlockPos().getSquaredDistance(pos, true);
            if (dist < bestDist) {
                if (accept.test(cache)) {
                    bestDist = dist;
                    bestCache = cache;
                }
            }
        }
        return Optional.ofNullable(bestCache);
    }
    public Optional<ContainerCache> getClosestTo(Vec3d pos, ContainerType ...types) {
        Set<ContainerType> typeSet = new HashSet<ContainerType>(Arrays.asList(types));
        return getClosestTo(pos, cache -> typeSet.contains(cache.getContainerType()));
    }

    public List<ContainerCache> getContainersWithItem(Item ...items) {
        return getCachedContainers(cache -> cache.hasItem(items));
    }
    public Optional<ContainerCache> getClosestWithItem(Vec3d pos, Item ...items) {
        return getClosestTo(pos, cache -> cache.hasItem(items));
    }

    public boolean hasItem(Predicate<ContainerCache> accept, Item ...items) {
        for (HashMap<BlockPos, ContainerCache> map : _containerCaches.values()) {
            for (ContainerCache cache : map.values()) {
                if (cache.hasItem(items) && accept.test(cache))
                    return true;
            }
        }
        return false;
    }
    public boolean hasItem(Item ...items) {
        return hasItem(cache -> true, items);
    }

    public BlockPos getLastBlockPosInteraction() {
        return _lastBlockPosInteraction;
    }

    @Override
    protected void updateState() {
        // umm lol
    }

    @Override
    protected void reset() {
        for (Dimension key : _containerCaches.keySet()) {
            _containerCaches.get(key).clear();
        }
    }

}

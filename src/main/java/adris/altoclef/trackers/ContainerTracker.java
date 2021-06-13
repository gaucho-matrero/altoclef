package adris.altoclef.trackers;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.slots.ChestSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Keeps track of items that are in containers. Uses the blocktracker to verify container existance.
 */

public class ContainerTracker extends Tracker {

    private final ChestMap _chestMap;
    private final FurnaceMap _furnaceMap;

    private final TimerGame _updateTimer = new TimerGame(10);

    // We can't get the contents of the screen until the server ticks once.
    private Screen _awaitingScreen = null;

    public ContainerTracker(AltoClef mod, TrackerManager manager) {
        super(manager);
        _chestMap = new ChestMap(mod);
        _furnaceMap = new FurnaceMap(mod);
    }

    @Override
    protected void updateState() {
        if (_updateTimer.elapsed()) {
            _updateTimer.reset();
            _chestMap.updateBlocks();
            _furnaceMap.updateBlocks();
        }
    }

    @Override
    protected void reset() {
        _chestMap.clear();
        _furnaceMap.clear();
    }

    public void onBlockInteract(BlockPos pos, Block block) {
        if (block.is(Blocks.CHEST) || block.is(Blocks.TRAPPED_CHEST)) {
            _chestMap.setInteractBlock(pos);
        } else if (block.is(Blocks.FURNACE)) {
            _furnaceMap.setInteractBlock(pos);
        }
    }

    public void onScreenOpenFirstTick(Screen screen) {
        _awaitingScreen = screen;
    }

    public void onServerTick() {
        if (_awaitingScreen != null) {
            if (_awaitingScreen instanceof FurnaceScreen) {
                onFurnaceScreenOpen(((FurnaceScreen) _awaitingScreen).getScreenHandler());
            } else if (_awaitingScreen instanceof GenericContainerScreen) {
                onChestScreenOpen(((GenericContainerScreen) _awaitingScreen).getScreenHandler());
            }
            //_awaitingScreen = null;
        }

    }

    public void onScreenClose() {
        _chestMap.onScreenClose();
        _furnaceMap.onScreenClose();
        _awaitingScreen = null;
    }

    public void onFurnaceScreenOpen(FurnaceScreenHandler screenHandler) {
        if (_furnaceMap.getBlockPos() != null) {
            onFurnaceOpen(_furnaceMap.getBlockPos(), screenHandler);
        }
    }

    public void onChestScreenOpen(GenericContainerScreenHandler screenHandler) {
        if (_chestMap.getBlockPos() != null) {
            onChestOpen(_chestMap.getBlockPos(), screenHandler);
        }
    }

    public void onFurnaceOpen(BlockPos pos, FurnaceScreenHandler screenHandler) {
        _furnaceMap.setInteractBlock(pos);
        _furnaceMap.openContainer(screenHandler);
    }

    public void onChestOpen(BlockPos pos, GenericContainerScreenHandler screenHandler) {
        _chestMap.setInteractBlock(pos);
        _chestMap.openContainer(screenHandler);
    }

    public FurnaceMap getFurnaceMap() {
        return _furnaceMap;
    }

    public ChestMap getChestMap() {
        return _chestMap;
    }

    abstract static class ContainerMap<T extends ScreenHandler> {

        protected BlockPos _blockPos;

        protected T _screenHandler;

        public BlockPos getBlockPos() {
            return _blockPos;
        }

        void setInteractBlock(BlockPos pos) {
            _blockPos = pos;
        }

        void openContainer(T screenHandler) {
            _screenHandler = screenHandler;
            updateContainer(_blockPos, _screenHandler);
        }

        void onScreenClose() {
            if (_screenHandler != null) {
                updateContainer(_blockPos, _screenHandler);
                _screenHandler = null;
            }
        }

        protected abstract void updateContainer(BlockPos pos, T screenHandler);

        public abstract void updateBlocks();

        public abstract void deleteBlock(BlockPos pos);

        public abstract void clear();
    }

    public static class ChestMap extends ContainerMap<GenericContainerScreenHandler> {

        private final AltoClef _mod;
        private final HashMap<BlockPos, ChestData> _blockData = new HashMap<>();
        //private final HashMap<Item, List<BlockPos>> _chestsWithItem = new HashMap<>();

        public ChestMap(AltoClef mod) {
            _mod = mod;
        }

        @Override
        public void updateContainer(BlockPos pos, GenericContainerScreenHandler screenHandler) {
            BlockPos leftSide = WorldUtil.getChestLeft(_mod, pos);
            if (leftSide == null) {
                Debug.logInternal("PROBLEM: (could not find chest left side?)");
                return;
            }

            boolean big = screenHandler.getRows() >= 6;

            _blockData.putIfAbsent(pos, new ChestData(big));
            ChestData data = _blockData.get(pos);

            data.clear();
            data.setBig(big);

            int start = 0;
            int end = big ? 53 : 26;
            int occupied = 0;
            for (int slotIndex = start; slotIndex <= end; ++slotIndex) {
                ItemStack stack = screenHandler.getInventory().getStack(slotIndex);
                if (!stack.isEmpty()) {
                    data.addItem(stack.getItem(), stack.getCount(), slotIndex);
                    occupied++;
                }
            }
            data.setOccupiedSlots(occupied);
        }

        @Override
        public void updateBlocks() {
            // Check for deleted blocks and delete if they no longer exist
            for (BlockPos blockToCheck : _blockData.keySet()) {
                if (!_mod.getBlockTracker().blockIsValid(blockToCheck, Blocks.CHEST)) {
                    deleteBlock(blockToCheck);
                }
                if (_mod.getChunkTracker().isChunkLoaded(blockToCheck)) {
                    ChestData data = _blockData.get(blockToCheck);
                    if (data._big && !WorldUtil.isChestBig(_mod, blockToCheck)) {
                        Debug.logMessage("Cached chest size at " + blockToCheck.toShortString() + " reduced, will delete chest info/uncache.");
                        deleteBlock(blockToCheck);
                    }
                }
            }
        }

        @Override
        public void deleteBlock(BlockPos pos) {
            _blockData.remove(pos);
        }

        @Override
        public void clear() {
            _blockData.clear();
            //_chestsWithItem.clear();
        }

        private void validateItemChestMap(Item item) {
            //if (_chestsWithItem.containsKey(item)) {
            // Remove if we're not tracking this block anymore.
            //    _chestsWithItem.get(item).removeIf(blockPos -> !_blockData.containsKey(blockPos));
            //}
        }

        public ChestData getCachedChestData(BlockPos pos) {
            return _blockData.getOrDefault(pos, null);
        }

        public List<BlockPos> getBlocksWithItem(ItemTarget[] targets, boolean requireMinAmount) {
            List<BlockPos> result = new ArrayList<>();
            int count = 0;
            for (BlockPos pos : _blockData.keySet()) {
                ChestData data = getCachedChestData(pos);
                for (ItemTarget target : targets) {
                    for (Item item : target.getMatches()) {
                        if (data.getItemCount(item) > 0) {
                            result.add(pos);
                            count += data.getItemCount(item);
                        }
                    }
                }
            }
            return result;
            //return new ArrayList<>();
        }

        public List<BlockPos> getBlocksWithItem(ItemTarget... targets) {
            return getBlocksWithItem(targets, false);
        }

        public List<BlockPos> getBlocksWithItem(Item... items) {
            return getBlocksWithItem(new ItemTarget(items));
        }
    }

    public static class ChestData {
        public Instant _lastOpened;

        private final HashMap<Item, Integer> _itemCounts = new HashMap<>();
        private final HashMap<Item, List<Slot>> _itemSlots = new HashMap<>();

        private boolean _big;

        private int _occupiedSlots;

        public ChestData(boolean big) {
            _big = big;
        }

        public boolean isBig() {
            return _big;
        }

        public void setBig(boolean big) {
            _big = big;
        }

        public void onOpen() {
            _lastOpened = Instant.now();
        }

        public long openedHowManyMillisecondsAgo() {
            return Instant.now().toEpochMilli() - _lastOpened.toEpochMilli();
        }

        public boolean hasItem(Item... items) {
            for (Item item : items) {
                if (_itemCounts.containsKey(item)) return true;
            }
            return false;
        }

        public boolean hasItem(ItemTarget... targets) {
            for (ItemTarget target : targets) {
                if (hasItem(target.getMatches())) return true;
            }
            return false;
        }

        public int getItemCount(Item item) {
            return _itemCounts.getOrDefault(item, 0);
        }

        public List<Slot> getItemSlotsWithItem(Item item) {
            return _itemSlots.getOrDefault(item, new ArrayList<>());
        }

        public void clear() {
            _itemCounts.clear();
            _itemSlots.clear();
            _occupiedSlots = 0;
        }

        public void addItem(Item item, int count, int slotIndex) {
            _itemCounts.putIfAbsent(item, 0);
            _itemCounts.put(item, _itemCounts.get(item) + count);
            _itemSlots.putIfAbsent(item, new ArrayList<>());
            _itemSlots.get(item).add(new ChestSlot(slotIndex, _big));
        }

        public int getOccupiedSlots() {
            return _occupiedSlots;
        }

        public void setOccupiedSlots(int slotCount) {
            _occupiedSlots = slotCount;
        }

        public boolean isFull() {
            return _occupiedSlots >= (_big ? 9 * 3 * 2 : 9 * 3);
        }
    }

    public static class FurnaceMap extends ContainerMap<FurnaceScreenHandler> {
        private final HashMap<BlockPos, FurnaceData> _blockData = new HashMap<>();
        private final HashMap<Item, List<BlockPos>> _materialMap = new HashMap<>();

        private final AltoClef _mod;

        public FurnaceMap(AltoClef mod) {
            _mod = mod;
        }

        public boolean furnaceExists(BlockPos pos) {
            return _blockData.containsKey(pos);
        }

        public FurnaceData getFurnaceData(BlockPos pos) {
            return _blockData.get(pos);
        }

        public List<BlockPos> getFurnacesWithMaterial(Item item) {
            if (_materialMap.containsKey(item)) {
                return _materialMap.get(item);
            }
            return Collections.emptyList();
        }

        @Override
        public void updateContainer(BlockPos pos, FurnaceScreenHandler screenHandler) {
            // Keep track of the items at this block.


            if (!_blockData.containsKey(pos)) {
                _blockData.put(pos, new FurnaceData());
            }
            FurnaceData dat = _blockData.get(pos);

            int materialSlot = 0,
                    fuelSlot = 1,
                    outputSlot = 2;

            ItemStack materials = screenHandler.getSlot(materialSlot).getStack(),
                    fuel = screenHandler.getSlot(fuelSlot).getStack(),
                    output = screenHandler.getSlot(outputSlot).getStack();

            //Debug.logMessage("CONTAINER UPDATE: " + materials.getItem().getTranslationKey() + " x " + materials.getCount());

            dat.fuelStored = InventoryTracker.getFuelAmount(fuel);
            dat.materials = materials;
            dat.output = output;

            dat._wasBurning = screenHandler.isBurning();

            // Get when we expect the furnace to finish cooking.

            int remaining = materials.getCount();
            int remainingTicks = (int) ((double) remaining - InventoryTracker.getFurnaceCookPercent(screenHandler)) * 200;


            int currentTicks = AltoClef.getTicks();
            dat._tickExpectedEnd = currentTicks + remainingTicks;

            double fuelNeededToBurnAll = dat.materials.getCount() - dat.fuelStored;
            // This Considers: Fuel in the hood + progress (progress aka "da arrow" should be considered as stored fuel, so it's ADDED to the total and not added)
            fuelNeededToBurnAll -= InventoryTracker.getFurnaceFuel(screenHandler) - InventoryTracker.getFurnaceCookPercent(screenHandler);
            dat._fuelNeededToBurnMaterials = fuelNeededToBurnAll;

            //Debug.logMessage("Furnace updated. Has " + materials.getItem().getTranslationKey() + " as its materials.");
        }

        @Override
        public void updateBlocks() {
            // Check for deleted blocks and delete if they no longer exist
            for (BlockPos blockToCheck : _blockData.keySet()) {
                if (!_mod.getBlockTracker().blockIsValid(blockToCheck, Blocks.FURNACE)) {
                    deleteBlock(blockToCheck);
                }
            }
        }

        @Override
        public void deleteBlock(BlockPos pos) {
            if (_blockData.containsKey(pos)) {
                FurnaceData toDelete = _blockData.get(pos);
                if (!toDelete.materials.isEmpty()) {
                    Item item = toDelete.materials.getItem();
                    if (_materialMap.containsKey(item)) {
                        //Debug.logMessage("CONTAINER DELETE: " + pos);
                        _materialMap.get(item).remove(pos);
                    } else {
                        Debug.logWarning("Inconsistent tracking of FurnaceMap for item " + item.getTranslationKey() + ". Please report this bug!");
                        Debug.logStack();
                    }
                }
            }
        }

        @Override
        public void clear() {
            _blockData.clear();
            _materialMap.clear();
        }
    }

    public static class FurnaceData {
        public double fuelStored;
        public ItemStack materials;
        public ItemStack output;

        private int _tickExpectedEnd;
        private boolean _wasBurning;

        private double _fuelNeededToBurnMaterials;

        public boolean wasBurningLastChecked() {
            return _wasBurning;
        }

        public double getRemainingFuelNeededToBurnMaterials() {
            return _fuelNeededToBurnMaterials;
        }

        public int getExpectedTicksRemaining(int currentTick) {
            return _tickExpectedEnd - currentTick;
        }

        public double getExpectedSecondsRemaining(int currentTick, double tps) {
            return getExpectedTicksRemaining(currentTick) * tps;
        }
    }

}

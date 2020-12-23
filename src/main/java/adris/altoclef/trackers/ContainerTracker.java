package adris.altoclef.trackers;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.csharpisbetter.Timer;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Keeps track of items that are in containers. Uses the blocktracker to verify container existance.
 *
 */

public class ContainerTracker extends Tracker {

    private final ChestMap _chestMap;
    private final FurnaceMap _furnaceMap;

    private final Timer _updateTimer = new Timer(10);

    // We can't get the contents of the screen until the server ticks once.
    private Screen _awaitingScreen = null;

    public ContainerTracker(AltoClef mod, TrackerManager manager) {
        super(manager);
        _chestMap = new ChestMap();
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
    }

    static class ChestMap extends ContainerMap<GenericContainerScreenHandler> {
        @Override
        public void updateContainer(BlockPos pos, GenericContainerScreenHandler screenHandler) {

        }

        @Override
        public void updateBlocks() {

        }

        @Override
        public void deleteBlock(BlockPos pos) {

        }
    }

    public static class FurnaceMap extends ContainerMap<FurnaceScreenHandler> {
        private HashMap<BlockPos, FurnaceData> _blockData = new HashMap<>();
        private HashMap<Item, List<BlockPos>> _materialMap = new HashMap<>();

        private AltoClef _mod;

        public FurnaceMap(AltoClef mod) {
            _mod = mod;
        }

        public boolean furnaceExists(BlockPos pos) {
            return _blockData.containsKey(pos);
        }

        public FurnaceData getFurnaceData(BlockPos pos) { return _blockData.get(pos); }

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
            int remainingTicks = (int) ((double)remaining - InventoryTracker.getFurnaceCookPercent(screenHandler) ) * 200;


            int currentTicks = _mod.getTicks();
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
                if (BlockTracker.blockIsInvalid(Blocks.FURNACE, blockToCheck)) {
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

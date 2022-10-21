package adris.altoclef;

import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class AltoClefSettings {

    // woo singletons
    private static AltoClefSettings _instance = new AltoClefSettings();
    public static AltoClefSettings getInstance() {
        return _instance;
    }

    private final Object breakMutex = new Object();
    private final Object placeMutex = new Object();

    private final Object propertiesMutex = new Object();

    private final Object globalHeuristicMutex = new Object();

    private final HashSet<BlockPos> _blocksToAvoidBreaking = new HashSet<>();
    private final List<Predicate<BlockPos>> _breakAvoiders = new ArrayList<>();

    private final List<Predicate<BlockPos>> _placeAvoiders = new ArrayList<>();

    private final List<Predicate<BlockPos>> _forceCanWalkOn = new ArrayList<>();

    private final List<Predicate<BlockPos>> _forceAvoidWalkThrough = new ArrayList<>();

    private final List<BiPredicate<BlockState, ItemStack>> _forceUseTool = new ArrayList<>();

    private final List<BiFunction<Double, BlockPos, Double>> _globalHeuristics = new ArrayList<>();

    private final HashSet<Item> _protectedItems = new HashSet<>();

    private boolean _allowFlowingWaterPass;

    private boolean _pauseInteractions;

    private boolean _dontPlaceBucketButStillFall;

    private boolean _allowSwimThroughLava = false;

    private boolean _treatSoulSandAsOrdinaryBlock = false;

    public void avoidBlockBreak(BlockPos pos) {
        synchronized (breakMutex) {
            _blocksToAvoidBreaking.add(pos);
        }
    }
    public void avoidBlockBreak(Predicate<BlockPos> avoider) {
        synchronized (breakMutex) {
            _breakAvoiders.add(avoider);
        }
    }

    public void configurePlaceBucketButDontFall(boolean allow) {
        synchronized (propertiesMutex) {
            _dontPlaceBucketButStillFall = allow;
        }
    }

    public void treatSoulSandAsOrdinaryBlock(boolean enable) {
        synchronized (propertiesMutex) {
            _treatSoulSandAsOrdinaryBlock = enable;
        }
    }

    public void avoidBlockPlace(Predicate<BlockPos> avoider) {
        synchronized (placeMutex) {
            _placeAvoiders.add(avoider);
        }
    }

    public boolean shouldAvoidBreaking(int x, int y, int z) {
        return shouldAvoidBreaking(new BlockPos(x, y, z));
    }
    public boolean shouldAvoidBreaking(BlockPos pos) {
        synchronized (breakMutex) {
            if (_blocksToAvoidBreaking.contains(pos))
                return true;
            return (_breakAvoiders.stream().anyMatch(pred -> pred.test(pos)));
        }
    }
    public boolean shouldAvoidPlacingAt(BlockPos pos) {
        synchronized (placeMutex) {
            return _placeAvoiders.stream().anyMatch(pred -> pred.test(pos));
        }
    }
    public boolean shouldAvoidPlacingAt(int x, int y, int z) {
        return shouldAvoidPlacingAt(new BlockPos(x, y, z));
    }

    public boolean canWalkOnForce(int x, int y, int z) {
        synchronized (propertiesMutex) {
            return _forceCanWalkOn.stream().anyMatch(pred -> pred.test(new BlockPos(x, y, z)));
        }
    }

    public boolean shouldAvoidWalkThroughForce(BlockPos pos) {
        synchronized (propertiesMutex) {
            return _forceAvoidWalkThrough.stream().anyMatch(pred -> pred.test(pos));
        }
    }
    public boolean shouldAvoidWalkThroughForce(int x, int y, int z) {
        return shouldAvoidWalkThroughForce(new BlockPos(x, y, z));
    }

    public boolean shouldForceUseTool(BlockState state, ItemStack tool) {
        synchronized (propertiesMutex) {
            return _forceUseTool.stream().anyMatch(pred -> pred.test(state, tool));
        }
    }

    public boolean shouldNotPlaceBucketButStillFall() {
        synchronized (propertiesMutex) {
            return _dontPlaceBucketButStillFall;
        }
    }

    public boolean shouldTreatSoulSandAsOrdinaryBlock() {
        synchronized (propertiesMutex) {
            return _treatSoulSandAsOrdinaryBlock;
        }
    }

    public boolean isInteractionPaused() {
        synchronized (propertiesMutex) {
            return _pauseInteractions;
        }
    }
    public boolean isFlowingWaterPassAllowed() {
        synchronized (propertiesMutex) {
            return _allowFlowingWaterPass;
        }
    }
    public boolean canSwimThroughLava() {
        synchronized (propertiesMutex) {
            return _allowSwimThroughLava;
        }
    }

    public void setInteractionPaused(boolean paused) {
        synchronized (propertiesMutex) {
            _pauseInteractions = paused;
        }
    }
    public void setFlowingWaterPass(boolean pass) {
        synchronized (propertiesMutex) {
            _allowFlowingWaterPass = pass;
        }
    }

    public void allowSwimThroughLava(boolean allow) {
        synchronized (propertiesMutex) {
            _allowSwimThroughLava = allow;
        }
    }

    public double applyGlobalHeuristic(double prev, int x, int y, int z) {
        return prev;
        /*
        synchronized (globalHeuristicMutex) {
            BlockPos p = new BlockPos(x, y, z);
            for (BiFunction<Double, BlockPos, Double> toApply : _globalHeuristics) {
                prev = toApply.apply(prev, p);
            }
        }
        return prev;
         */
    }

    public HashSet<BlockPos> getBlocksToAvoidBreaking() {
        return _blocksToAvoidBreaking;
    }
    public List<Predicate<BlockPos>> getBreakAvoiders() {
        return _breakAvoiders;
    }
    public List<Predicate<BlockPos>> getPlaceAvoiders() {
        return _placeAvoiders;
    }
    public List<Predicate<BlockPos>> getForceWalkOnPredicates() {
        return _forceCanWalkOn;
    }
    public List<Predicate<BlockPos>> getForceAvoidWalkThroughPredicates() {
        return _forceAvoidWalkThrough;
    }
    public List<BiPredicate<BlockState, ItemStack>> getForceUseToolPredicates() {
        return _forceUseTool;
    }
    public List<BiFunction<Double, BlockPos, Double>> getGlobalHeuristics() {return _globalHeuristics;}

    public boolean isItemProtected(Item item) {
        return _protectedItems.contains(item);
    }
    public HashSet<Item> getProtectedItems() {
        return _protectedItems;
    }
    public void protectItem(Item item) {
        _protectedItems.add(item);
    }
    public void stopProtectingItem(Item item) {
        _protectedItems.remove(item);
    }

    public Object getBreakMutex() {
        return breakMutex;
    }
    public Object getPlaceMutex() {
        return placeMutex;
    }
    public Object getPropertiesMutex() {return propertiesMutex;}
    public Object getGlobalHeuristicMutex() { return globalHeuristicMutex;}
}

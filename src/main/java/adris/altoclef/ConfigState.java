package adris.altoclef;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.ItemTarget;
import baritone.altoclef.AltoClefSettings;
import baritone.api.Settings;
import baritone.process.MineProcess;
import baritone.api.utils.RayTraceUtils;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;

import java.util.*;
import java.util.function.Predicate;

/**
 * Represents a state of global config. It can be copied and reset
 * so that behaviour across tasks is consistent.
 */
public class ConfigState {

    private AltoClef _mod;

    Stack<State> _states = new Stack<>();

    public ConfigState(AltoClef mod) {
        _mod = mod;

        // Start with one state.
        push();
    }

    /// Parameters

    public void setFollowDistance(double distance) {
        current().followOffsetDistance = distance;
        current().applyState();
    }

    public void setMineScanDroppedItems(boolean value) {
        current().mineScanDroppedItems = value;
        current().applyState();
    }

    public void addThrowawayItems(Item ...items) {
        Collections.addAll(current().throwawayItems, items);
        current().applyState();
    }

    public void removeThrowawayItems(Item ...items) {
        // No removeAll huh. Nice one Java.
        for (Item item : items) {
            current().throwawayItems.remove(item);
        }
        current().applyState();
    }
    public void removeThrowawayItems(ItemTarget...targets) {
        // Just to be safe we remove ALL items that we may want to use.
        for (ItemTarget target : targets) {
            removeThrowawayItems(target.getMatches());
        }
        current().applyState();
    }

    public boolean exclusivelyMineLogs() {
        return current().exclusivelyMineLogs;
    }
    public void setExclusivelyMineLogs(boolean value) {
        current().exclusivelyMineLogs = value;
        current().applyState();
    }

    public void avoidBlockBreaking(BlockPos pos) {
        current().blocksToAvoidBreaking.add(pos);
        current().applyState();
    }
    public void avoidBlockBreaking(Predicate<BlockPos> pred) {
        current().toAvoidBreaking.add(pred);
        current().applyState();
    }

    public void avoidBlockPlacing(Predicate<BlockPos> pred) {
        current().toAvoidPlacing.add(pred);
        current().applyState();
    }

    public void setRayTracingFluidHandling(RaycastContext.FluidHandling fluidHandling) {
        current().rayFluidHandling = fluidHandling;
        //Debug.logMessage("OOF: " + fluidHandling);
        current().applyState();
    }

    public void setSearchAnywhereFlag(boolean value) {
        current().mineProcSearchAnyFlag = value;
        current().applyState();
    }

    public void setAllowWalkThroughFlowingWater(boolean value) {
        current()._allowWalkThroughFlowingWater = value;
        current().applyState();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isProtected(Item item) {
        // For now nothing is protected.
        return false;//current().throwawayItems.contains(item);
    }

    /// Stack management
    public void push() {
        if (_states.empty()) {
            _states.push(new State());
        } else {
            // Make copy and push that
            _states.push(new State(current()));
        }
    }
    public void pop() {
        if (_states.empty()) {
            Debug.logError("State stack is empty. This shouldn't be happening.");
            return;
        }
        State s = _states.pop();
        s.applyState();
    }

    private State current() {
        if (_states.empty()) {
            Debug.logError("STATE EMPTY, UNEMPTIED!");
            push();
        }
        return _states.peek();
    }

    class State {
        /// Baritone Params
        public double followOffsetDistance;
        public List<Item> throwawayItems = new ArrayList<>();
        public boolean mineScanDroppedItems;

        // Alto Clef params
        public boolean exclusivelyMineLogs;

        // Extra Baritone Settings
        public HashSet<BlockPos> blocksToAvoidBreaking = new HashSet<>();
        public List<Predicate<BlockPos>> toAvoidBreaking = new ArrayList<>();
        public List<Predicate<BlockPos>> toAvoidPlacing = new ArrayList<>();
        public boolean _allowWalkThroughFlowingWater = false;

        // Hard coded stuff
        public RaycastContext.FluidHandling rayFluidHandling;
        public boolean mineProcSearchAnyFlag;

        public State() {
            this(null);
        }

        public State(State toCopy) {
            // Read in current state
            readState(_mod.getClientBaritoneSettings());

            readExtraState(_mod.getExtraBaritoneSettings());

            if (toCopy != null) {
                // Copy over stuff from old one
                exclusivelyMineLogs = toCopy.exclusivelyMineLogs;
            }
        }

        /**
         * Make the current state match our copy
         */
        public void applyState() {
            applyState(_mod.getClientBaritoneSettings(), _mod.getExtraBaritoneSettings());
        }

        /**
         * Read in a copy of the current state
         */
        private void readState(Settings s) {
            throwawayItems.clear();
            throwawayItems.addAll(s.acceptableThrowawayItems.value);
            followOffsetDistance = s.followOffsetDistance.value;
            mineScanDroppedItems = s.mineScanDroppedItems.value;
        }

        private void readExtraState(AltoClefSettings settings) {
            synchronized (settings.getBreakMutex()) {
                synchronized (settings.getPlaceMutex()) {
                    blocksToAvoidBreaking = new HashSet<>(settings.getBlocksToAvoidBreaking());
                    toAvoidBreaking = new ArrayList<>(settings.getBreakAvoiders());
                    toAvoidPlacing = new ArrayList<>(settings.getPlaceAvoiders());
                }
            }
            _allowWalkThroughFlowingWater = settings.isFlowingWaterPassAllowed();

            rayFluidHandling = RayTraceUtils.fluidHandling;
            mineProcSearchAnyFlag = MineProcess.searchAnyFlag;
        }

        /**
         * Make the current state match our copy
         */
        private void applyState(Settings s, AltoClefSettings sa) {
            s.acceptableThrowawayItems.value.clear();
            s.acceptableThrowawayItems.value.addAll(throwawayItems);
            s.followOffsetDistance.value = followOffsetDistance;
            s.mineScanDroppedItems.value = mineScanDroppedItems;

            // Kinda jank but it works.
            synchronized (sa.getBreakMutex()) {
                synchronized (sa.getPlaceMutex()) {
                    sa.getBreakAvoiders().clear();
                    sa.getBreakAvoiders().addAll(toAvoidBreaking);
                    sa.getBlocksToAvoidBreaking().clear();
                    sa.getBlocksToAvoidBreaking().addAll(blocksToAvoidBreaking);
                    sa.getPlaceAvoiders().clear();
                    sa.getPlaceAvoiders().addAll(toAvoidPlacing);
                }
            }

            sa.setFlowingWaterPass(_allowWalkThroughFlowingWater);

            RayTraceUtils.fluidHandling = rayFluidHandling;
            MineProcess.searchAnyFlag = mineProcSearchAnyFlag;
        }
    }
}

package adris.altoclef;

import baritone.altoclef.AltoClefSettings;
import baritone.api.Settings;
import baritone.api.utils.RayTraceUtils;
import baritone.process.MineProcess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;

import java.util.*;
import java.util.function.Predicate;

/**
 * Represents the current behaviour of the bot. It can be copied and reset
 * so that behaviour across tasks is consistent.
 */
public class BotBehaviour {

    Deque<State> _states = new ArrayDeque<>();
    private final AltoClef _mod;

    public BotBehaviour(AltoClef mod) {
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

    public void addThrowawayItems(Item... items) {
        Collections.addAll(current().throwawayItems, items);
        current().applyState();
    }

    public boolean exclusivelyMineLogs() {
        return current().exclusivelyMineLogs;
    }

    public void setExclusivelyMineLogs(boolean value) {
        current().exclusivelyMineLogs = value;
        current().applyState();
    }

    public boolean shouldExcludeFromForcefield(Entity entity) {
        for (Predicate<Entity> pred : current().excludeFromForceField) {
            if (pred.test(entity)) return true;
        }
        return false;
    }

    public void addForceFieldExclusion(Predicate<Entity> pred) {
        current().excludeFromForceField.add(pred);
        // Not needed, as excludeFromForceField isn't applied anywhere else.
        // current.applyState();
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

    public void allowWalkingOn(Predicate<BlockPos> pred) {
        current().allowWalking.add(pred);
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

    public void setPauseOnLostFocus(boolean pauseOnLostFocus) {
        current().pauseOnLostFocus = pauseOnLostFocus;
        current().applyState();
    }

    public void addProtectedItems(Item... items) {
        Collections.addAll(current().protectedItems, items);
        current().applyState();
    }

    public void removeProtectedItems(Item... items) {
        current().protectedItems.removeAll(Arrays.asList(items));
        current().applyState();
    }

    public boolean isProtected(Item item) {
        // For now nothing is protected.
        return current().protectedItems.contains(item);
    }

    public boolean shouldForceFieldPlayers() {
        return current().forceFieldPlayers;
    }

    public void setForceFieldPlayers(boolean forceFieldPlayers) {
        current().forceFieldPlayers = forceFieldPlayers;
        // Not needed, nothing changes.
        // current.applyState()
    }

    public void allowWalkThroughLava(boolean allow) {
        current().walkThroughLava = allow;
        current().applyState();
    }

    public void setPreferredStairs(boolean allow) {
        current().preferredStairs = allow;
        current().applyState();
    }

    public void setAllowDiagonalAscend(boolean allow) {
        current().allowDiagonalAscend = allow;
        current().applyState();
    }

    public void avoidDodgingProjectile(Predicate<Entity> whenToDodge) {
        current().avoidDodgingProjectile.add(whenToDodge);
        // Not needed, nothing changes.
        // current().applyState();
    }

    public boolean shouldAvoidDodgingProjectile(Entity entity) {
        for (Predicate<Entity> test : current().avoidDodgingProjectile) {
            if (test.test(entity)) return true;
        }
        return false;
    }

    public void allowShears(boolean allow) {
        current().allowShears = allow;
        current().applyState();
    }

    /// Stack management
    public void push() {
        if (_states.isEmpty()) {
            _states.push(new State());
        } else {
            // Make copy and push that
            _states.push(new State(current()));
        }
    }

    public void push(State customState) {
        _states.push(customState);
    }

    public State pop() {
        if (_states.isEmpty()) {
            Debug.logError("State stack is empty. This shouldn't be happening.");
            return null;
        }
        State popped = _states.pop();
        if (_states.isEmpty()) {
            Debug.logError("State stack is empty after pop. This shouldn't be happening.");
            return null;
        }
        _states.peek().applyState();
        return popped;
    }

    private State current() {
        if (_states.isEmpty()) {
            Debug.logError("STATE EMPTY, UNEMPTIED!");
            push();
        }
        return _states.peek();
    }

    class State {
        /// Baritone Params
        public double followOffsetDistance;
        public List<Item> throwawayItems = new ArrayList<>();
        public List<Item> protectedItems = new ArrayList<>();
        public boolean mineScanDroppedItems;
        public boolean walkThroughLava;
        public boolean allowDiagonalAscend;
        public boolean preferredStairs;

        // Alto Clef params
        public boolean exclusivelyMineLogs;
        public boolean forceFieldPlayers;
        public List<Predicate<Entity>> avoidDodgingProjectile = new ArrayList<>();

        public List<Predicate<Entity>> excludeFromForceField = new ArrayList<>();

        // Extra Baritone Settings
        public HashSet<BlockPos> blocksToAvoidBreaking = new HashSet<>();
        public List<Predicate<BlockPos>> toAvoidBreaking = new ArrayList<>();
        public List<Predicate<BlockPos>> toAvoidPlacing = new ArrayList<>();
        public List<Predicate<BlockPos>> allowWalking = new ArrayList<>();
        public boolean _allowWalkThroughFlowingWater = false;
        public boolean allowShears;

        // Minecraft config
        public boolean pauseOnLostFocus = true;

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

            readMinecraftState();

            if (toCopy != null) {
                // Copy over stuff from old one
                exclusivelyMineLogs = toCopy.exclusivelyMineLogs;
                avoidDodgingProjectile.addAll(toCopy.avoidDodgingProjectile);
                excludeFromForceField.addAll(toCopy.excludeFromForceField);
                forceFieldPlayers = toCopy.forceFieldPlayers;
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
            walkThroughLava = s.assumeWalkOnLava.value;
            allowDiagonalAscend = s.allowDiagonalAscend.value;
            //preferredStairs = s.allowDownward.value;
        }

        private void readExtraState(AltoClefSettings settings) {
            synchronized (settings.getBreakMutex()) {
                synchronized (settings.getPlaceMutex()) {
                    blocksToAvoidBreaking = new HashSet<>(settings.getBlocksToAvoidBreaking());
                    toAvoidBreaking = new ArrayList<>(settings.getBreakAvoiders());
                    toAvoidPlacing = new ArrayList<>(settings.getPlaceAvoiders());
                    protectedItems = new ArrayList<>(settings.getProtectedItems());
                    synchronized (settings.getPropertiesMutex()) {
                        allowWalking = new ArrayList<>(settings.getForceWalkOnPredicates());
                    }
                }
            }
            _allowWalkThroughFlowingWater = settings.isFlowingWaterPassAllowed();
            allowShears = settings.areShearsAllowed();

            rayFluidHandling = RayTraceUtils.fluidHandling;
            mineProcSearchAnyFlag = MineProcess.searchAnyFlag;
        }

        private void readMinecraftState() {
            pauseOnLostFocus = MinecraftClient.getInstance().options.pauseOnLostFocus;
        }

        /**
         * Make the current state match our copy
         */
        private void applyState(Settings s, AltoClefSettings sa) {
            s.acceptableThrowawayItems.value.clear();
            s.acceptableThrowawayItems.value.addAll(throwawayItems);
            s.followOffsetDistance.value = followOffsetDistance;
            s.mineScanDroppedItems.value = mineScanDroppedItems;
            s.assumeWalkOnLava.value = walkThroughLava;
            s.allowDiagonalAscend.value = allowDiagonalAscend;

            // We need an alternrative method to handle this, this method makes navigation much less reliable.
            //s.allowDownward.value = preferredStairs;


            // Kinda jank but it works.
            synchronized (sa.getBreakMutex()) {
                synchronized (sa.getPlaceMutex()) {
                    sa.getBreakAvoiders().clear();
                    sa.getBreakAvoiders().addAll(toAvoidBreaking);
                    sa.getBlocksToAvoidBreaking().clear();
                    sa.getBlocksToAvoidBreaking().addAll(blocksToAvoidBreaking);
                    sa.getPlaceAvoiders().clear();
                    sa.getPlaceAvoiders().addAll(toAvoidPlacing);
                    sa.getProtectedItems().clear();
                    sa.getProtectedItems().addAll(protectedItems);
                    synchronized (sa.getPropertiesMutex()) {
                        sa.getForceWalkOnPredicates().clear();
                        sa.getForceWalkOnPredicates().addAll(allowWalking);
                    }
                }
            }

            sa.setFlowingWaterPass(_allowWalkThroughFlowingWater);
            sa.allowShears(allowShears);

            // Extra / hard coded
            RayTraceUtils.fluidHandling = rayFluidHandling;
            MineProcess.searchAnyFlag = mineProcSearchAnyFlag;

            // Minecraft
            MinecraftClient.getInstance().options.pauseOnLostFocus = pauseOnLostFocus;
        }
    }
}

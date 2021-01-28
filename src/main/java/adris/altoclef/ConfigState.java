package adris.altoclef;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.ItemTarget;
import baritone.api.Settings;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

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
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isProtected(Item item) {
        // For now nothing is protected.
        return false;//current().throwawayItems.contains(item);
    }

    /// Stack management
    public void push() {
        _states.push(new State());
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

        public State() {
            Settings s = _mod.getClientBaritoneSettings();

            // Read in current state on creation
            readState(s);
        }

        /**
         * Make the current state match our copy
         */
        public void applyState() {
            applyState(_mod.getClientBaritoneSettings());
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

        /**
         * Make the current state match our copy
         */
        private void applyState(Settings s) {
            s.acceptableThrowawayItems.value.clear();
            s.acceptableThrowawayItems.value.addAll(throwawayItems);
            s.followOffsetDistance.value = followOffsetDistance;
            s.mineScanDroppedItems.value = mineScanDroppedItems;
        }
    }
}

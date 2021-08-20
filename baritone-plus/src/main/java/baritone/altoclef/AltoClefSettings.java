/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.altoclef;

import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

public class AltoClefSettings {

    private final Object breakMutex = new Object();
    private final Object placeMutex = new Object();

    private final Object propertiesMutex = new Object();

    private final HashSet<BlockPos> _blocksToAvoidBreaking = new HashSet<>();
    private final List<Predicate<BlockPos>> _breakAvoiders = new ArrayList<>();

    private final List<Predicate<BlockPos>> _placeAvoiders = new ArrayList<>();

    private final List<Predicate<BlockPos>> _forceCanWalkOn = new ArrayList<>();

    private final HashSet<Item> _protectedItems = new HashSet<>();

    private boolean _allowFlowingWaterPass;

    private boolean _pauseInteractions;

    private boolean _dontPlaceBucketButStillFall;

    private boolean _allowShears = true;

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
            if (_blocksToAvoidBreaking.contains(pos)) return true;
            for (Predicate<BlockPos> pred : _breakAvoiders) {
                if (pred.test(pos)) return true;
            }
            return false;
        }
    }
    public boolean shouldAvoidPlacingAt(BlockPos pos) {
        return shouldAvoidPlacingAt(pos.getX(), pos.getY(), pos.getZ());
    }
    public boolean shouldAvoidPlacingAt(int x, int y, int z) {
        synchronized (placeMutex) {
            for (Predicate<BlockPos> pred : _placeAvoiders) {
                if (pred.test(new BlockPos(x, y, z))) return true;
            }
            return false;
        }
    }

    public boolean canWalkOnForce(int x, int y, int z) {
        synchronized (propertiesMutex) {
            for (Predicate<BlockPos> pred : _forceCanWalkOn) {
                if (pred.test(new BlockPos(x, y, z))) return true;
            }
            return false;
        }
    }

    public boolean shouldNotPlaceBucketButStillFall() {
        synchronized (propertiesMutex) {
            return _dontPlaceBucketButStillFall;
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
    public boolean areShearsAllowed() {
        synchronized (propertiesMutex) {
            return _allowShears;
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

    public void allowShears(boolean allow) {
        synchronized (propertiesMutex) {
            _allowShears = allow;
        }
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
}

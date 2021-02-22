package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.AbstractDoToEntityTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.EntityTracker;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Timer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.HashSet;
import java.util.List;

public class TradeWithPiglinsTask extends ResourceTask {

    private final int _goldBuffer;

    private Task _goldTask = null;

    // If we're too far away from a trading piglin, we risk deloading them and losing the trade.
    private static final double TRADING_PIGLIN_TOO_FAR_AWAY = 64 + 8;

    public TradeWithPiglinsTask(int goldBuffer, List<ItemTarget> itemTargets) {
        super(itemTargets);
        _goldBuffer = goldBuffer;
    }

    public TradeWithPiglinsTask(int goldBuffer, ItemTarget target) {
        super(target);
        _goldBuffer = goldBuffer;
    }

    public TradeWithPiglinsTask(int goldBuffer, Item item, int targetCount) {
        super(item, targetCount);
        _goldBuffer = goldBuffer;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // Collect gold if we don't have it.
        if (_goldTask != null && _goldTask.isActive() && !_goldTask.isFinished(mod)) {
            setDebugState("Collecting gold");
            return _goldTask;
        }
        if (!mod.getInventoryTracker().hasItem(Items.GOLD_INGOT)) {
            if (_goldTask == null) _goldTask = TaskCatalogue.getItemTask("gold_ingot", _goldBuffer);
            return _goldTask;
        }

        // If we have no piglin nearby, explore until we find piglin.
        if (!mod.getEntityTracker().mobFound(PiglinEntity.class)) {
            setDebugState("Wandering");
            return new TimeoutWanderTask(false);
        }

        // If we have a trading piglin that's too far away, get closer to it.

        // Find gold and trade with a piglin
        setDebugState("Trading with Piglin");
        return new PerformTradeWithPiglin();
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        return obj instanceof TradeWithPiglinsTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Trading with Piglins";
    }

    static class PerformTradeWithPiglin extends AbstractDoToEntityTask {

        private static final double PIGLIN_NEARBY_RADIUS = 10;

        private Entity _currentlyBartering = null;
        private final Timer _barterTimeout = new Timer(5);
        private final Timer _intervalTimeout = new Timer(3);

        private final HashSet<Entity> _blacklisted = new HashSet<>();

        public PerformTradeWithPiglin() {
            super(3);
        }

        @Override
        protected void onStart(AltoClef mod) {
            super.onStart(mod);
            //_blacklisted.clear();
        }

        @Override
        protected boolean isSubEqual(AbstractDoToEntityTask other) {
            return other instanceof PerformTradeWithPiglin;
        }

        @Override
        protected Task onEntityInteract(AltoClef mod, Entity entity) {

            // If we didn't run this in a while, we can retry bartering.
            if (_intervalTimeout.elapsed()) {
                // We didn't interact for a while, continue bartering as usual.
                _barterTimeout.reset();
                _intervalTimeout.reset();
            }

            // We're trading so reset the barter timeout
            if (EntityTracker.isTradingPiglin(_currentlyBartering)) {
                _barterTimeout.reset();
            }

            // We're bartering a new entity.
            if (!entity.equals(_currentlyBartering)) {
                _currentlyBartering = entity;
                _barterTimeout.reset();
            }

            if (_barterTimeout.elapsed()) {
                // We failed bartering.
                Debug.logMessage("Failed bartering with current piglin, blacklisting.");
                _blacklisted.add(_currentlyBartering);
                _barterTimeout.reset();
                _currentlyBartering = null;
                return null;
            }

            setDebugState("Trading with piglin");

            mod.getInventoryTracker().equipItem(Items.GOLD_INGOT);
            mod.getController().interactEntity(mod.getPlayer(), entity, Hand.MAIN_HAND);
            _intervalTimeout.reset();
            return null;
        }

        @Override
        protected Entity getEntityTarget(AltoClef mod) {
            // Ignore trading piglins
            Entity found = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(),
                    (entity) ->
                            _blacklisted.contains(entity)
                                    || EntityTracker.isTradingPiglin(entity)
                                    || (entity instanceof LivingEntity && ((LivingEntity)entity).isBaby())
                                    || (_currentlyBartering != null && !entity.isInRange(_currentlyBartering, PIGLIN_NEARBY_RADIUS)),
                    PiglinEntity.class);
            if (found == null) {
                if (_blacklisted.contains(_currentlyBartering)) {
                    _currentlyBartering = null;
                }
                return _currentlyBartering;
            }
            return found;
        }

        @Override
        protected String toDebugString() {
            return "Trading with piglin";
        }
    }

}

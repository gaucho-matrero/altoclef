package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.LookUtil;
import adris.altoclef.util.PlayerExtraController;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.ActionListener;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.Baritone;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class PlaceBlockNearbyTask extends Task {

    private final Block[] _toPlace;

    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private final TimeoutWanderTask _wander = new TimeoutWanderTask(2);

    private final TimerGame _randomlookTimer = new TimerGame(0.25);
    private final Predicate<BlockPos> _cantPlaceHere;
    private BlockPos _justPlaced; // Where we JUST placed a block.
    private BlockPos _tryPlace;   // Where we should TRY placing a block.
    // Oof, necesarry for the onBlockPlaced action.
    private AltoClef _mod;
    private final ActionListener<PlayerExtraController.BlockPlaceEvent> onBlockPlaced;

    public PlaceBlockNearbyTask(Predicate<BlockPos> cantPlaceHere, Block... toPlace) {
        _toPlace = toPlace;
        _cantPlaceHere = cantPlaceHere;
        onBlockPlaced = new ActionListener<>(value ->
        {
            if (Util.arrayContains(_toPlace, value.blockState.getBlock())) {
                stopPlacing(_mod);
            }
        });
    }

    public PlaceBlockNearbyTask(Block... toPlace) {
        this(blockPos -> false, toPlace);
    }

    @Override
    protected void onStart(AltoClef mod) {
        _mod = mod;
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
        mod.getControllerExtras().onBlockPlaced.addListener(onBlockPlaced);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Method:
        // - If looking at placable block
        //      Place immediately
        // Find a spot to place
        // - Prefer flat areas (open space, block below) closest to player
        // -

        // Close screen first
        mod.getPlayer().closeHandledScreen();


        // Try placing where we're looking right now.
        BlockPos current = getCurrentlyLookingBlockPlace(mod);
        if (current != null && !_cantPlaceHere.test(current)) {
            if (place(mod, current)) {
                return null;
            }
        }

        // Wander while we can.
        if (_wander.isActive() && !_wander.isFinished(mod)) {
            setDebugState("Wandering, will try to place again later.");
            _progressChecker.reset();
            return _wander;
        }
        // Fail check
        if (!_progressChecker.check(mod)) {
            Debug.logMessage("Failed placing, wandering and trying again.");
            LookUtil.randomOrientation(mod);
            if (_tryPlace != null) {
                mod.getBlockTracker().requestBlockUnreachable(_tryPlace);
                _tryPlace = null;
            }
            return _wander;
        }

        // Try to place at a particular spot.
        if (_tryPlace == null || mod.getBlockTracker().unreachable(_tryPlace)) {
            _tryPlace = locateClosePlacePos(mod);
        }
        if (_tryPlace != null) {
            setDebugState("Trying to place at " + _tryPlace);
            _justPlaced = _tryPlace;
            return new PlaceBlockTask(_tryPlace, _toPlace);
        }

        // Look in random places to maybe get a random hit
        if (_randomlookTimer.elapsed()) {
            _randomlookTimer.reset();
            LookUtil.randomOrientation(mod);
        }

        setDebugState("Wandering until we randomly place or find a good place spot.");
        return new TimeoutWanderTask();
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        stopPlacing(mod);
        mod.getControllerExtras().onBlockPlaced.removeListener(onBlockPlaced);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof PlaceBlockNearbyTask) {
            PlaceBlockNearbyTask task = (PlaceBlockNearbyTask) obj;
            return Util.arraysEqual(task._toPlace, _toPlace);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Place " + Util.arrayToString(_toPlace) + " nearby";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _justPlaced != null && Util.arrayContains(_toPlace, mod.getWorld().getBlockState(_justPlaced).getBlock());
    }

    public BlockPos getPlaced() {
        return _justPlaced;
    }

    private BlockPos getCurrentlyLookingBlockPlace(AltoClef mod) {
        HitResult hit = MinecraftClient.getInstance().crosshairTarget;
        if (hit instanceof BlockHitResult) {
            BlockHitResult bhit = (BlockHitResult) hit;
            BlockPos bpos = bhit.getBlockPos();//.subtract(bhit.getSide().getVector());
            //Debug.logMessage("TEMP: A: " + bpos);
            IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
            if (MovementHelper.canPlaceAgainst(ctx, bpos)) {
                BlockPos placePos = bhit.getBlockPos().add(bhit.getSide().getVector());
                // Don't place inside the player.
                if (WorldUtil.isInsidePlayer(mod, placePos)) {
                    return null;
                }
                //Debug.logMessage("TEMP: B (actual): " + placePos);
                if (!Baritone.getAltoClefSettings().shouldAvoidPlacingAt(placePos.getX(), placePos.getY(), placePos.getZ())) {
                    return placePos;
                }
            }
        }
        return null;
    }

    private boolean equipBlock(AltoClef mod) {
        for (Block block : _toPlace) {
            if (!mod.getExtraBaritoneSettings().isInteractionPaused() && mod.getInventoryTracker().hasItem(block.asItem())) {
                if (mod.getInventoryTracker().equipItem(block.asItem())) return true;
            }
        }
        return false;
    }

    private boolean place(AltoClef mod, BlockPos targetPlace) {
        if (equipBlock(mod)) {
            // Shift click just for 100% container security.
            mod.getInputControls().hold(Input.SNEAK);

            //mod.getInputControls().tryPress(Input.CLICK_RIGHT);
            // This appears to work on servers...
            // TODO: Helper lol
            HitResult mouseOver = MinecraftClient.getInstance().crosshairTarget;
            if (mouseOver == null || mouseOver.getType() != HitResult.Type.BLOCK) {
                return false;
            }
            Hand hand = Hand.MAIN_HAND;
            if (MinecraftClient.getInstance().interactionManager.interactBlock(mod.getPlayer(), mod.getWorld(), hand, (BlockHitResult) mouseOver)  == ActionResult.SUCCESS) {
                mod.getPlayer().swingHand(hand);
                Debug.logMessage("PRESSED");
                return true;
            }

            //mod.getControllerExtras().mouseClickOverride(1, true);
            //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
            _justPlaced = targetPlace;
            return true;
        }
        return false;
    }

    private void stopPlacing(AltoClef mod) {
        mod.getInputControls().release(Input.SNEAK);
        //mod.getControllerExtras().mouseClickOverride(1, false);
        // Oof, these sometimes cause issues so this is a bit of a duct tape fix.
        mod.getClientBaritone().getBuilderProcess().onLostControl();
    }

    private BlockPos locateClosePlacePos(AltoClef mod) {
        int range = 7;
        BlockPos best = null;
        double smallestScore = Double.POSITIVE_INFINITY;
        BlockPos start = mod.getPlayer().getBlockPos().add(-range, -range, -range);
        BlockPos end = mod.getPlayer().getBlockPos().add(range, range, range);

        for (BlockPos blockPos : WorldUtil.scanRegion(mod, start, end)) {
            boolean solid = WorldUtil.isSolid(mod, blockPos);
            boolean inside = WorldUtil.isInsidePlayer(mod, blockPos);
            // We can't break this block.
            if (solid && !WorldUtil.canBreak(mod, blockPos)) {
                continue;
            }
            // We can't place here as defined by user.
            if (!_cantPlaceHere.test(blockPos)) {
                continue;
            }
            // We can't place here.
            if (mod.getBlockTracker().unreachable(blockPos) || !WorldUtil.canPlace(mod, blockPos)) {
                continue;
            }
            boolean hasBelow = WorldUtil.isSolid(mod, blockPos.down());
            double distSq = blockPos.getSquaredDistance(mod.getPlayer().getPos(), false);

            double score = distSq + (solid ? 4 : 0) + (hasBelow ? 0 : 10) + (inside ? 3 : 0);

            if (score < smallestScore) {
                best = blockPos;
                smallestScore = score;
            }
        }

        return best;
    }
}

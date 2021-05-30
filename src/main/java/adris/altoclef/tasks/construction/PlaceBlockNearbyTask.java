package adris.altoclef.tasks.construction;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.LookUtil;
import adris.altoclef.util.PlayerExtraController;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.ActionListener;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.Baritone;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;


public class PlaceBlockNearbyTask extends Task {
    private final Block[] toPlace;
    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private final TimeoutWanderTask wander = new TimeoutWanderTask(2);
    private final Timer randomlookTimer = new Timer(0.25);
    private final Predicate<BlockPos> _cantPlaceHere;
    private BlockPos justPlaced; // Where we JUST placed a block.
    private BlockPos tryPlace;   // Where we should TRY placing a block.
    // Oof, necesarry for the onBlockPlaced action.
    private AltoClef mod;
    private final ActionListener<PlayerExtraController.BlockPlaceEvent> onBlockPlaced
            = new ActionListener<PlayerExtraController.BlockPlaceEvent>() {
        @Override
        public void invoke(PlayerExtraController.BlockPlaceEvent value) {
            if (Util.arrayContains(toPlace, value.blockState.getBlock())) {
                stopPlacing(mod);
            }
        }
    };

    public PlaceBlockNearbyTask(Predicate<BlockPos> cantPlaceHere, Block... toPlace) {
        this.toPlace = toPlace;
        _cantPlaceHere = cantPlaceHere;
    }

    public PlaceBlockNearbyTask(Block... toPlace) {
        this(blockPos -> false, toPlace);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return justPlaced != null && Util.arrayContains(toPlace, mod.getWorld().getBlockState(justPlaced).getBlock());
    }

    @Override
    protected void onStart(AltoClef mod) {
        this.mod = mod;
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
        if (current != null) {
            if (place(mod, current)) {
                return null;
            }
        }

        // Wander while we can.
        if (wander.isActive() && !wander.isFinished(mod)) {
            setDebugState("Wandering, will try to place again later.");
            progressChecker.reset();
            return wander;
        }
        // Fail check
        if (!progressChecker.check(mod)) {
            Debug.logMessage("Failed placing, wandering and trying again.");
            LookUtil.randomOrientation(mod);
            if (tryPlace != null) {
                mod.getBlockTracker().requestBlockUnreachable(tryPlace);
                tryPlace = null;
            }
            return wander;
        }

        // Try to place at a particular spot.
        if (tryPlace == null || mod.getBlockTracker().unreachable(tryPlace)) {
            tryPlace = locateClosePlacePos(mod);
        }
        if (tryPlace != null) {
            setDebugState("Trying to place at " + tryPlace);
            justPlaced = tryPlace;
            return new PlaceBlockTask(tryPlace, toPlace);
        }

        // Look in random places to maybe get a random hit
        if (randomlookTimer.elapsed()) {
            randomlookTimer.reset();
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
            return Util.arraysEqual(task.toPlace, toPlace);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Place " + Util.arrayToString(toPlace) + " nearby";
    }

    public BlockPos getPlaced() {
        return justPlaced;
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
        for (Block block : toPlace) {
            if (!mod.getExtraBaritoneSettings().isInteractionPaused() && mod.getInventoryTracker().hasItem(block.asItem())) {
                if (mod.getInventoryTracker().equipItem(block.asItem())) return true;
            }
        }
        return false;
    }

    private boolean place(AltoClef mod, BlockPos targetPlace) {
        if (equipBlock(mod)) {
            // Shift click just for 100% container security.
            MinecraftClient.getInstance().options.keySneak.setPressed(true);
            mod.getControllerExtras().mouseClickOverride(1, true);
            //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
            justPlaced = targetPlace;
            return true;
        }
        return false;
    }

    private void stopPlacing(AltoClef mod) {
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.SNEAK, false);
        mod.getControllerExtras().mouseClickOverride(1, false);
        MinecraftClient.getInstance().options.keySneak.setPressed(false);
        // Oof, these sometimes cause issues so this is a bit of a duct tape fix.
        mod.getClientBaritone().getBuilderProcess().onLostControl();
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, false);
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

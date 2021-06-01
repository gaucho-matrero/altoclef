package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.misc.MLGBucketTask;
import adris.altoclef.tasksystem.ITaskOverridesGrounded;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Timer;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

public class MLGBucketFallChain extends SingleTaskChain implements ITaskOverridesGrounded {

    private final Timer _tryCollectWaterTimer = new Timer(4);
    private MLGBucketTask _lastMLG = null;
    private boolean _wasPickingUp = false;
    private final Timer _pickupRepeatTimer = new Timer(1);

    public MLGBucketFallChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        //_lastMLG = null;
    }

    @Override
    public float getPriority(AltoClef mod) {
        if (!mod.inGame()) return Float.NEGATIVE_INFINITY;
        // Won't work in the nether, duh
        if (mod.getCurrentDimension() == Dimension.NETHER) return Float.NEGATIVE_INFINITY;

        if (isFallingOhNo(mod)) {
            _tryCollectWaterTimer.reset();
            setTask(new MLGBucketTask());
            _lastMLG = (MLGBucketTask)_mainTask;
            return 100;
        } else if (!_tryCollectWaterTimer.elapsed() && mod.getPlayer().getVelocity().y >= -0.5) { // Why -0.5? Cause it's slower than -0.7.
            // We just placed water, try to collect it.
            if (mod.getInventoryTracker().hasItem(Items.BUCKET) && !mod.getInventoryTracker().hasItem(Items.WATER_BUCKET)) {

                if (_lastMLG != null) {
                    BlockPos placed = _lastMLG.getWaterPlacedPos();
                    //Debug.logInternal("PLACED: " + placed);
                    if (placed != null && placed.isWithinDistance(mod.getPlayer().getPos(), 5.5)) {
                        BlockPos toInteract = placed.down();
                        Optional<Rotation> reach = InteractWithBlockTask.getReach(toInteract, Direction.UP);
                        if (reach.isPresent()) {
                            mod.getClientBaritone().getLookBehavior().updateTarget(reach.get(), true);
                            if (mod.getClientBaritone().getPlayerContext().isLookingAt(toInteract)) {
                                if (!mod.getInventoryTracker().equipItem(new ItemTarget(Items.BUCKET, 1))) {
                                    Debug.logWarning("Failed to equip bucket to pick up water post MLG.");
                                } else {
                                    if (_pickupRepeatTimer.elapsed()) {
                                        // Pick up
                                        //Debug.logMessage("PICK");
                                        _pickupRepeatTimer.reset();
                                        mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                                        _wasPickingUp = true;
                                    } else if (_wasPickingUp) {
                                        // Stop picking up, wait and try again.
                                        _wasPickingUp = false;
                                    }
                                }
                            }
                        }
                        setTask(null);
                        return 60;
                    }
                }
            }
        }
        if (_wasPickingUp) {
            _wasPickingUp = false;
            _lastMLG = null;
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public String getName() {
        return "MLG Water Bucket Fall Chain";
    }

    @Override
    public boolean isActive() {
        // We're always checking for mlg.
        return true;
    }

    public boolean isFallingOhNo(AltoClef mod) {
        if (!mod.getModSettings().shouldAutoMLGBucket()) {
            return false;
        }
        if (!mod.getInventoryTracker().hasItem(Items.WATER_BUCKET)) {
            // No bucket, no point.
            return false;
        }
        if (mod.getPlayer().isSwimming() || mod.getPlayer().isTouchingWater() || mod.getPlayer().isOnGround() || mod.getPlayer().isClimbing()) {
            // We're grounded.
            //Debug.logMessage(mod.getPlayer().isSwimming() + ", " + mod.getPlayer().isSubmergedInWater() + ", " + mod.getPlayer().isOnGround() + ", " + mod.getPlayer().isClimbing());
            return false;
        }
        double ySpeed = mod.getPlayer().getVelocity().y;
        return ySpeed < -0.7;
    }
}

package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.misc.MLGBucketTask;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.csharpisbetter.Timer;
import net.minecraft.item.Items;

import java.sql.Time;

public class MLGBucketFallChain extends SingleTaskChain {

    private final Timer _tryCollectWaterTimer = new Timer(1);

    public MLGBucketFallChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {

    }

    @Override
    public float getPriority(AltoClef mod) {
        // Won't work in the nether, duh
        if (mod.getCurrentDimension() == Dimension.NETHER) return Float.NEGATIVE_INFINITY;

        if (isFallingOhNo(mod)) {
            _tryCollectWaterTimer.reset();
            setTask(new MLGBucketTask());
            return 100;
        } else if (!_tryCollectWaterTimer.elapsed()) {
            // We just placed water, try to collect it.
            if (mod.getInventoryTracker().hasItem(Items.BUCKET) && !mod.getInventoryTracker().hasItem(Items.WATER_BUCKET)) {
                setTask(TaskCatalogue.getItemTask("water_bucket", 1));
                return 60;
            }
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

    private boolean isFallingOhNo(AltoClef mod) {
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

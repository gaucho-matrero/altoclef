package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Optional;

public class MLGBucketTask extends Task {

    private boolean _clicked;

    private BlockPos _placedPos;

    @Override
    protected void onStart(AltoClef mod) {
        _clicked = false;
        _placedPos = null;
        // hold shift while falling.
        // Look down at first, usually does the trick.
        mod.getPlayer().pitch = 90;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Check AROUND player instead of directly under.
        // We may crop the edge of a block or wall.
        Vec3d[] offsets = new Vec3d[]{
                new Vec3d(0, 0, 0),
                new Vec3d(-0.5, 0, 0),
                new Vec3d(0, 0, 0.5),
                new Vec3d(0.5, 0, 0),
                new Vec3d(0, 0, -0.5),
                new Vec3d(-0.5, 0, -0.5),
                new Vec3d(-0.5, 0, 0.5),
                new Vec3d(0.5, 0, -0.5),
                new Vec3d(0.5, 0, 0.5),
        };
        BlockHitResult result = null;
        double bestSqDist = Double.POSITIVE_INFINITY;
        for (Vec3d offset : offsets) {
            RaycastContext rctx = test(mod.getPlayer(), offset);
            BlockHitResult hit = mod.getWorld().raycast(rctx);
            if (hit.getType() == HitResult.Type.BLOCK) {
                double curDis = hit.squaredDistanceTo(mod.getPlayer());
                if (curDis < bestSqDist) {
                    result = hit;
                    bestSqDist = curDis;
                }
            }
        }

        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            BlockPos toPlaceOn = result.getBlockPos();

            BlockPos willLandIn = toPlaceOn.up();
            // If we're water, we're ok. Do nothing.
            BlockState willLandInState = mod.getWorld().getBlockState(willLandIn);
            if (willLandInState.getBlock() == Blocks.WATER) {
                _placedPos = willLandIn;
                // We good.
                setDebugState("Waiting to fall into water");
                mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
                return null;
            }

            if (!mod.getInventoryTracker().equipItem(Items.WATER_BUCKET)) {
                Debug.logWarning("Failed to equip bucket for mlg. Oh shit.");
                mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
            }

            IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
            Optional<Rotation> reachable = RotationUtils.reachable(ctx.player(), toPlaceOn, ctx.playerController().getBlockReachDistance());
            if (reachable.isPresent()) {
                setDebugState("Performing MLG");
                mod.getClientBaritone().getLookBehavior().updateTarget(reachable.get(), true);
                if (mod.getClientBaritone().getPlayerContext().isLookingAt(toPlaceOn)) {
                    Debug.logMessage("HIT: " + willLandIn);
                    _placedPos = willLandIn;
                    mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                    //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                }
            } else {
                setDebugState("Waiting to reach target block...");
                // Look down by default
                //mod.getClientBaritone().getLookBehavior().updateTarget(new Rotation(0f, 90f), true);
                mod.getPlayer().pitch = 90;
            }
            //player.rotationPitch = 90f
            //playerController.processRightClick(player, world, hand)
        } else {
            setDebugState("Wait for it...");
        }
        return null;
    }

    private RaycastContext test(Entity player, Vec3d offset) {
        Vec3d pos = player.getPos();
        return new RaycastContext(pos, pos.add(0, -5.33, 0).add(offset), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if (mod.getCurrentDimension() == Dimension.NETHER) return true;
        return !mod.getInventoryTracker().hasItem(Items.WATER_BUCKET) || mod.getPlayer().isSwimming() || mod.getPlayer().isTouchingWater() || mod.getPlayer().isOnGround() || mod.getPlayer().isClimbing();
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof MLGBucketTask;
    }

    @Override
    protected String toDebugString() {
        return "Epic gaemer moment";
    }


    public BlockPos getWaterPlacedPos() {
        return _placedPos;
    }

}

package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.WorldHelper;
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
import net.minecraft.util.math.Box;
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
        mod.getPlayer().setPitch(90);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Check AROUND player instead of directly under.
        // We may crop the edge of a block or wall.
        Vec3d velCheck = mod.getPlayer().getVelocity();
        // Flatten and slightly exaggerate the velocity
        velCheck.multiply(10,0,10);
        Box b = mod.getPlayer().getBoundingBox().offset(velCheck);
        Vec3d c = b.getCenter();
        Vec3d[] coords = new Vec3d[]{
                c,
                new Vec3d(b.minX, c.y, b.minZ),
                new Vec3d(b.maxX, c.y, b.minZ),
                new Vec3d(b.minX, c.y, b.maxZ),
                new Vec3d(b.maxX, c.y, b.maxZ),
        };
        BlockHitResult result = null;
        double bestSqDist = Double.POSITIVE_INFINITY;
        for (Vec3d rayOrigin : coords) {
            RaycastContext rctx = test(mod.getPlayer(), rayOrigin);
            BlockHitResult hit = mod.getWorld().raycast(rctx);
            if (hit.getType() == HitResult.Type.BLOCK) {
                double curDis = hit.getPos().squaredDistanceTo(rayOrigin);
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
                // We good.
                setDebugState("Waiting to fall into water");
                mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
                return null;
            }

            if (!mod.getSlotHandler().forceEquipItem(Items.WATER_BUCKET)) {
                mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
            }

            IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
            Optional<Rotation> reachable = RotationUtils.reachable(ctx.player(), toPlaceOn, ctx.playerController().getBlockReachDistance());
            if (reachable.isPresent()) {
                setDebugState("Performing MLG");
                LookHelper.lookAt(mod, reachable.get());
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
                //mod.getPlayer().setPitch(90);
            }
            //player.rotationPitch = 90f
            //playerController.processRightClick(player, world, hand)
        } else {
            setDebugState("Wait for it...");
        }
        return null;
    }

    private RaycastContext test(Entity player, Vec3d origin) {
        return new RaycastContext(origin, origin.add(0, -6, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if (WorldHelper.getCurrentDimension() == Dimension.NETHER) return true;
        return !mod.getItemStorage().hasItem(Items.WATER_BUCKET) || mod.getPlayer().isSwimming() || mod.getPlayer().isTouchingWater() || mod.getPlayer().isOnGround() || mod.getPlayer().isClimbing();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof MLGBucketTask;
    }

    @Override
    protected String toDebugString() {
        return "Epic gaemer moment";
    }


    public BlockPos getWaterPlacedPos() {
        return _placedPos;
    }

}

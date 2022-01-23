package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.EntityHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
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
    private BlockPos _placedPos;

    private BlockPos _movingTorwards;

    private static final double CAST_DOWN_DISTANCE = 30;
    private static final double EPIC_CLUTCH_CONE_CAST_HEIGHT = 20; // how high the "epic clutch" ray cone is
    private static final double EPIC_CLUTCH_CONE_PITCH_ANGLE = 25; // how wide (degrees) the "epic clutch" ray cone is
    private static final int EPIC_CLUTCH_CONE_PITCH_RESOLUTION = 8; // how many divisions in each direction the cone's pitch has
    private static final int EPIC_CLUTCH_CONE_YAW_DIVISION_START = 6; // How many divisions to start the cone clutch at in the center
    private static final int EPIC_CLUTCH_CONE_YAW_DIVISION_END = 20; // How many divisions to move the cone clutch at torwars the end
    private static boolean isDangerousToLandOn(BlockPos pos) {
        return MinecraftClient.getInstance().world.getBlockState(pos).getBlock() == Blocks.LAVA;
    }
    private static boolean safeToLandIn(BlockPos pos) {
        return MinecraftClient.getInstance().world.getBlockState(pos).getBlock() == Blocks.WATER;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _placedPos = null;
        // hold shift while falling.
        // Look down at first, might help
        mod.getPlayer().setPitch(90);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Check AROUND player instead of directly under.
        // We may crop the edge of a block or wall.
        _movingTorwards = null;
        Task result = onTickInternal(mod);
        if (_movingTorwards != null) {
            Debug.logMessage("Going to " + _movingTorwards.toShortString());
            LookHelper.lookAt(mod, _movingTorwards);
            mod.getInputControls().hold(Input.MOVE_FORWARD);
        } else {
            mod.getInputControls().release(Input.MOVE_FORWARD);
        }
        return result;
    }

    private Task onTickInternal(AltoClef mod) {
        Optional<BlockPos> willLandOn = getBlockWeWillLandOn(mod);
        if (willLandOn.isPresent()) {
            Optional<BlockPos> bestClutchPos = getBestConeClutchBlock(mod);
            // Move torwards our best "clutch" position
            if (bestClutchPos.isPresent()) {
                _movingTorwards = bestClutchPos.get();
            }
            return placeMLGBucketTask(mod, willLandOn.get());
        } else {
            setDebugState("Wait for it...");
            return null;
        }
    }

    private Task placeMLGBucketTask(AltoClef mod, BlockPos toPlaceOn) {
        // If our raycast hit a non-solid block, go DOWN one.
        if (!WorldHelper.isSolid(mod, toPlaceOn)) {
            toPlaceOn = toPlaceOn.down();
        }
        BlockPos willLandIn = toPlaceOn.up();
        // If we're water, we're ok. Do nothing.
        BlockState willLandInState = mod.getWorld().getBlockState(willLandIn);
        if (willLandInState.getBlock() == Blocks.WATER) {
            // We good.
            setDebugState("Waiting to fall into water");
            mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
            return null;
        }

        IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
        Optional<Rotation> reachable = RotationUtils.reachable(ctx.player(), toPlaceOn, ctx.playerController().getBlockReachDistance());
        if (reachable.isPresent()) {
            _movingTorwards = null; // No longer try moving torwards, it's time to ACT
            setDebugState("Performing MLG");
            LookHelper.lookAt(mod, reachable.get());
            boolean hasClutch = (!mod.getWorld().getDimension().isUltrawarm() && mod.getSlotHandler().forceEquipItem(Items.WATER_BUCKET)) || mod.getSlotHandler().forceEquipItem(Items.HAY_BLOCK) || mod.getSlotHandler().forceEquipItem(Items.TWISTING_VINES);
            if (hasClutch && mod.getClientBaritone().getPlayerContext().isLookingAt(toPlaceOn)) {
                Debug.logMessage("HIT: " + willLandIn);
                _placedPos = willLandIn;
                mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
            } else {
                setDebugState("NO CLUTCH ITEMS (uh oh)");
            }
        } else {
            setDebugState("Waiting to reach target block...");
        }
        return null;
    }

    private RaycastContext castDown(Vec3d origin) {
        Entity player = MinecraftClient.getInstance().player;
        return new RaycastContext(origin, origin.add(0, -1 * CAST_DOWN_DISTANCE, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player);
    }

    private RaycastContext castCone(double yaw, double pitch) {
        Entity player = MinecraftClient.getInstance().player;
        Vec3d origin = player.getPos();
        double dy = EPIC_CLUTCH_CONE_CAST_HEIGHT;
        double dH = dy * Math.sin(Math.toRadians(pitch)); // horizontal distance
        double yawRad = Math.toRadians(yaw);
        double dx = dH * Math.cos(yawRad);
        double dz = dH * Math.sin(yawRad);
        Vec3d end = origin.add(dx, -1 * dy, dz);
        return new RaycastContext(origin, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player);
    }

    private Optional<BlockPos> getBlockWeWillLandOn(AltoClef mod) {
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
            RaycastContext rctx = castDown(rayOrigin);
            BlockHitResult hit = mod.getWorld().raycast(rctx);
            if (hit.getType() == HitResult.Type.BLOCK) {
                double curDis = hit.getPos().squaredDistanceTo(rayOrigin);
                if (curDis < bestSqDist) {
                    result = hit;
                    bestSqDist = curDis;
                }
            }
        }

        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        return Optional.ofNullable(result.getBlockPos());
    }

    private Optional<BlockPos> getBestConeClutchBlock(AltoClef mod) {
        double dpitchStart = EPIC_CLUTCH_CONE_PITCH_ANGLE / EPIC_CLUTCH_CONE_PITCH_RESOLUTION;

        // Our priority is:
        // - Safe to land (water)
        // - Highest block

        double highestY = Double.NEGATIVE_INFINITY;
        boolean bestBlockIsSafe = false;
        BlockPos bestBlock = null;

        for (double pitch = dpitchStart; pitch <= EPIC_CLUTCH_CONE_PITCH_ANGLE; pitch += EPIC_CLUTCH_CONE_PITCH_ANGLE / EPIC_CLUTCH_CONE_PITCH_RESOLUTION) {
            double pitchProgress = (pitch - dpitchStart) / (EPIC_CLUTCH_CONE_PITCH_ANGLE - dpitchStart);
            double yawResolution = EPIC_CLUTCH_CONE_YAW_DIVISION_START + pitchProgress * (EPIC_CLUTCH_CONE_YAW_DIVISION_END - EPIC_CLUTCH_CONE_YAW_DIVISION_START); // lerp from start to end
            for (double yaw = 0; yaw < 360; yaw += 360.0 / yawResolution) {
                RaycastContext rctx = castCone(yaw, pitch);
                BlockHitResult hit = mod.getWorld().raycast(rctx);
                BlockPos check = hit.getBlockPos();
                // For now, REQUIRE we land on this
                if (hit.getSide().getOffsetY() <= 0)
                    continue;
                // Do NOT go to lava lol
                if (isDangerousToLandOn(check))
                    continue;
                boolean safe = safeToLandIn(check);
                // Prioritize safe blocks
                if (bestBlockIsSafe && !safe)
                    continue;
                double height = check.getY();
                boolean highestSoFar = height > highestY;
                // We found a new contender
                if ((safe && !bestBlockIsSafe) || highestSoFar) {
                    if (canTravelToInAir(check)) {
                        if (highestSoFar) {
                            highestY = height;
                        }
                        if (safe) {
                            bestBlockIsSafe = safe;
                        }
                        bestBlock = check;
                    }
                }
            }
        }

        return Optional.ofNullable(bestBlock);
    }

    /**
     * Can we reach this block while falling, or will gravity pull us too far?
     */
    private static boolean canTravelToInAir(BlockPos pos) {
        Entity player = MinecraftClient.getInstance().player;
        double verticalDist = player.getPos().getY() - pos.getY() - 1;
        double verticalVelocity = -1 * player.getVelocity().y;
        double grav = EntityHelper.ENTITY_GRAVITY;
        double movementSpeedPerTick = 0.18; // Calculated, but also somewhat conservative
        // 1d projectile motion
        double ticksToTravelSq = (-verticalVelocity + Math.sqrt(verticalVelocity*verticalVelocity + 2*grav*verticalDist)) / grav;
        double maxMoveDistanceSq = movementSpeedPerTick * movementSpeedPerTick * ticksToTravelSq * ticksToTravelSq;
        double horizontalDistanceSq = WorldHelper.distanceXZSquared(player.getPos(), WorldHelper.toVec3d(pos));
        return maxMoveDistanceSq > horizontalDistanceSq;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getPlayer().isSwimming() || mod.getPlayer().isTouchingWater() || mod.getPlayer().isOnGround() || mod.getPlayer().isClimbing();
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

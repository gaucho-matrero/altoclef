package adris.altoclef.util.helpers;

import adris.altoclef.AltoClef;
import adris.altoclef.util.slots.Slot;
import baritone.api.BaritoneAPI;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.RayTraceUtils;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.Optional;

/**
 * Helper functions to interpret and change our player's look direction
 */
public interface LookHelper {

    static Optional<Rotation> getReach(BlockPos target, Direction side) {
        Optional<Rotation> reachable;
        IPlayerContext ctx = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext();
        if (side == null) {
            assert MinecraftClient.getInstance().player != null;
            reachable = RotationUtils.reachable(ctx.player(), target, ctx.playerController().getBlockReachDistance());
        } else {
            Vec3i sideVector = side.getVector();
            Vec3d centerOffset = new Vec3d(0.5 + sideVector.getX() * 0.5, 0.5 + sideVector.getY() * 0.5, 0.5 + sideVector.getZ() * 0.5);

            Vec3d sidePoint = centerOffset.add(target.getX(), target.getY(), target.getZ());

            //reachable(this.ctx.player(), _target, this.ctx.playerController().getBlockReachDistance());
            reachable = RotationUtils.reachableOffset(ctx.player(), target, sidePoint, ctx.playerController().getBlockReachDistance(), false);

            // Check for right angle
            if (reachable.isPresent()) {
                // Note: If sneak, use RotationUtils.inferSneakingEyePosition
                Vec3d camPos = ctx.player().getCameraPosVec(1.0F);
                Vec3d vecToPlayerPos = camPos.subtract(sidePoint);

                double dot = vecToPlayerPos.normalize().dotProduct(new Vec3d(sideVector.getX(), sideVector.getY(), sideVector.getZ()));
                if (dot < 0) {
                    // We're perpendicular and cannot face.
                    return Optional.empty();
                }
            }
        }
        return reachable;
    }

    static Optional<Rotation> getReach(BlockPos target) {
        return getReach(target, null);
    }

    static EntityHitResult raycast(Entity from, Entity to, double reachDistance) {
        Vec3d fromPos = getCameraPos(from),
                toPos = getCameraPos(to);
        Vec3d direction = (toPos.subtract(fromPos).normalize().multiply(reachDistance));
        Box box = to.getBoundingBox();
        return ProjectileUtil.raycast(from, fromPos, fromPos.add(direction), box, entity -> entity.equals(to), 0);
    }

    static boolean seesPlayer(Entity entity, Entity player, double maxRange, Vec3d entityOffs, Vec3d playerOffs) {
        return seesPlayerOffset(entity, player, maxRange, entityOffs, playerOffs) || seesPlayerOffset(entity, player, maxRange, entityOffs, new Vec3d(0, -1, 0).add(playerOffs));
    }

    static boolean seesPlayer(Entity entity, Entity player, double maxRange) {
        return seesPlayer(entity, player, maxRange, Vec3d.ZERO, Vec3d.ZERO);
    }

    static boolean cleanLineOfSight(Entity entity, Vec3d start, Vec3d end, double maxRange) {
        return raycast(entity, start, end, maxRange).getType() == HitResult.Type.MISS;
    }

    static boolean cleanLineOfSight(Entity entity, Vec3d end, double maxRange) {
        Vec3d start = getCameraPos(entity);
        return cleanLineOfSight(entity, start, end, maxRange);
    }

    static boolean cleanLineOfSight(Vec3d end, double maxRange) {
        return cleanLineOfSight(MinecraftClient.getInstance().player, end, maxRange);
    }

    static boolean cleanLineOfSight(Entity entity, BlockPos block, double maxRange) {
        Vec3d center = WorldHelper.toVec3d(block);
        BlockHitResult hit = raycast(entity, getCameraPos(entity), center, maxRange);
        if (hit == null) return true;
        return switch (hit.getType()) {
            case MISS -> true;
            case BLOCK -> hit.getBlockPos().equals(block);
            case ENTITY -> false;
        };
    }

    static Vec3d toVec3d(Rotation rotation) {
        return RotationUtils.calcVector3dFromRotation(rotation);
    }

    static BlockHitResult raycast(Entity entity, Vec3d start, Vec3d end, double maxRange) {
        Vec3d delta = end.subtract(start);
        if (delta.lengthSquared() > maxRange * maxRange) {
            end = start.add(delta.normalize().multiply(maxRange));
        }
        return entity.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
    }

    static BlockHitResult raycast(Entity entity, Vec3d end, double maxRange) {
        Vec3d start = getCameraPos(entity);
        return raycast(entity, start, end, maxRange);
    }

    static Rotation getLookRotation(Entity entity) {
        float pitch = entity.getPitch();
        float yaw = entity.getYaw();
        return new Rotation(yaw, pitch);
    }

    static Rotation getLookRotation() {
        if (MinecraftClient.getInstance().player == null) {
            return new Rotation(0, 0);
        }
        return getLookRotation(MinecraftClient.getInstance().player);
    }

    static Vec3d getCameraPos(Entity entity) {
        boolean isSneaking = false;
        if (entity instanceof PlayerEntity player) {
            isSneaking = player.isSneaking();
        }
        return isSneaking ? RayTraceUtils.inferSneakingEyePosition(entity) : entity.getCameraPosVec(1.0F);
    }

    static Vec3d getCameraPos(AltoClef mod) {
        IPlayerContext ctx = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext();
        return ctx.player().getCameraPosVec(1);
    }

    //  1: Looking straight at pos
    //  0: pos is 90 degrees to the side
    // -1: pos is 180 degrees away (looking away completely)
    static double getLookCloseness(Entity entity, Vec3d pos) {
        Vec3d rotDirection = entity.getRotationVecClient();
        Vec3d lookStart = getCameraPos(entity);
        Vec3d deltaToPos = pos.subtract(lookStart);
        Vec3d deltaDirection = deltaToPos.normalize();
        return rotDirection.dotProduct(deltaDirection);
    }

    static boolean tryAvoidingInteractable(AltoClef mod) {
        if (isCollidingInteractable(mod)) {
            randomOrientation(mod);
            return false;
        }
        return true;
    }

    private static boolean seesPlayerOffset(Entity entity, Entity player, double maxRange, Vec3d offsetEntity, Vec3d offsetPlayer) {
        Vec3d start = getCameraPos(entity).add(offsetEntity);
        Vec3d end = getCameraPos(player).add(offsetPlayer);
        return cleanLineOfSight(entity, start, end, maxRange);
    }

    private static boolean isCollidingInteractable(AltoClef mod) {

        if (!(mod.getPlayer().currentScreenHandler instanceof PlayerScreenHandler)) {
            ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
            if (!cursorStack.isEmpty()) {
                Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
                moveTo.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
                if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                    mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                }
                Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
                // Try throwing away cursor slot if it's garbage
                garbage.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            } else {
                StorageHelper.closeScreen();
            }
            return true;
        }

        HitResult result = MinecraftClient.getInstance().crosshairTarget;
        if (result == null) return false;
        if (result.getType() == HitResult.Type.BLOCK) {
            return WorldHelper.isInteractableBlock(mod, new BlockPos(result.getPos()));
        } else if (result.getType() == HitResult.Type.ENTITY) {
            if (result instanceof EntityHitResult) {
                Entity entity = ((EntityHitResult) result).getEntity();
                return entity instanceof MerchantEntity;
            }
        }
        return false;
    }

    static void randomOrientation(AltoClef mod) {
        Rotation r = new Rotation((float) Math.random() * 360f, -90 + (float) Math.random() * 180f);
        lookAt(mod, r);
    }

    static boolean isLookingAt(AltoClef mod, Rotation rotation) {
        return rotation.isReallyCloseTo(getLookRotation());
    }

    static boolean isLookingAt(AltoClef mod, BlockPos blockPos) {
        return mod.getClientBaritone().getPlayerContext().isLookingAt(blockPos);
    }

    static void lookAt(AltoClef mod, Rotation rotation) {
        mod.getClientBaritone().getLookBehavior().updateTarget(rotation, true);
        mod.getPlayer().setYaw(rotation.getYaw());
        mod.getPlayer().setPitch(rotation.getPitch());
    }

    static void lookAt(AltoClef mod, Vec3d toLook) {
        Rotation targetRotation = getLookRotation(mod, toLook);
        lookAt(mod, targetRotation);
    }

    static void lookAt(AltoClef mod, BlockPos toLook, Direction side) {
        Vec3d target = new Vec3d(toLook.getX() + 0.5, toLook.getY() + 0.5, toLook.getZ() + 0.5);
        if (side != null) {
            target.add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5);
        }
        lookAt(mod, target);
    }

    static void lookAt(AltoClef mod, BlockPos toLook) {
        lookAt(mod, toLook, null);
    }

    static Rotation getLookRotation(AltoClef mod, Vec3d toLook) {
        return RotationUtils.calcRotationFromVec3d(mod.getClientBaritone().getPlayerContext().playerHead(), toLook, mod.getClientBaritone().getPlayerContext().playerRotations());
    }

    static Rotation getLookRotation(AltoClef mod, BlockPos toLook) {
        return getLookRotation(mod, WorldHelper.toVec3d(toLook));
    }

}

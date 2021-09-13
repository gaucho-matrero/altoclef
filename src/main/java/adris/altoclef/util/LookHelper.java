package adris.altoclef.util;

import adris.altoclef.AltoClef;
import baritone.api.utils.RayTraceUtils;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public interface LookHelper {

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

    static boolean cleanLineOfSight(Entity entity, BlockPos block, double maxRange) {
        Vec3d center = WorldHelper.blockCenter(block);
        BlockHitResult hit = raycast(entity, getCameraPos(entity), center, maxRange);
        if (hit == null) return true;
        return switch (hit.getType()) {
            case MISS -> true;
            case BLOCK -> hit.getBlockPos().equals(block);
            case ENTITY -> false;
        };
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

    static Vec3d getCameraPos(Entity entity) {
        boolean isSneaking = false;
        if (entity instanceof PlayerEntity player) {
            isSneaking = player.isSneaking();
        }
        return isSneaking ? RayTraceUtils.inferSneakingEyePosition(entity) : entity.getCameraPosVec(1.0F);
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
        if (isCollidingContainer(mod)) {
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

    private static boolean isCollidingContainer(AltoClef mod) {

        if (!(mod.getPlayer().currentScreenHandler instanceof PlayerScreenHandler)) {
            mod.getPlayer().closeHandledScreen();
            return true;
        }

        HitResult result = MinecraftClient.getInstance().crosshairTarget;
        if (result == null) return false;
        if (result.getType() == HitResult.Type.BLOCK) {
            return WorldHelper.isContainerBlock(mod, new BlockPos(result.getPos()));
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
        mod.getClientBaritone().getLookBehavior().updateTarget(r, true);
    }

    static void lookAt(AltoClef mod, Vec3d toLook) {
        Rotation targetRotation = RotationUtils.calcRotationFromVec3d(mod.getClientBaritone().getPlayerContext().playerHead(), toLook, mod.getClientBaritone().getPlayerContext().playerRotations());
        mod.getClientBaritone().getLookBehavior().updateTarget(targetRotation, true);
    }
}

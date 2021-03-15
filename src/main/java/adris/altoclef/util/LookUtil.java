package adris.altoclef.util;

import adris.altoclef.AltoClef;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class LookUtil {

    public static EntityHitResult raycast(Entity from, Entity to, double reachDistance) {
        Vec3d fromPos = from.getCameraPosVec(1f),
                toPos = to.getCameraPosVec(1f);
        Vec3d direction = (toPos.subtract(fromPos).normalize().multiply(reachDistance));
        Box box = to.getBoundingBox();
        return ProjectileUtil.raycast(from, fromPos, fromPos.add(direction), box, entity -> entity.equals(to), 0);
    }

    public static boolean tryAvoidingInteractable(AltoClef mod) {
        if (isCollidingContainer(mod)) {
            randomOrientation(mod);
            return false;
        }
        return true;
    }

    private static boolean isCollidingContainer(AltoClef mod) {

        if (!(mod.getPlayer().currentScreenHandler instanceof PlayerScreenHandler)) {
            mod.getPlayer().closeHandledScreen();
            return true;
        }

        IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
        HitResult result = MinecraftClient.getInstance().crosshairTarget;
        if (result == null) return false;
        if (result.getType() == HitResult.Type.BLOCK) {
            Block block = mod.getWorld().getBlockState(new BlockPos(result.getPos())).getBlock();
            if (block instanceof ChestBlock
                    || block instanceof EnderChestBlock
                    || block instanceof CraftingTableBlock
                    || block instanceof AbstractFurnaceBlock
                    || block instanceof LoomBlock
                    || block instanceof CartographyTableBlock
                    || block instanceof EnchantingTableBlock
            ) {
                return true;
            }
        } else if (result.getType() == HitResult.Type.ENTITY) {
            if (result instanceof EntityHitResult) {
                Entity entity = ((EntityHitResult) result).getEntity();
                if (entity instanceof MerchantEntity) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void randomOrientation(AltoClef mod) {
        Rotation r = new Rotation((float)Math.random() * 360f, (float)Math.random() * 360f);
        mod.getClientBaritone().getLookBehavior().updateTarget(r, true);
    }
}

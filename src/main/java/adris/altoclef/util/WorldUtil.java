package adris.altoclef.util;

import adris.altoclef.AltoClef;
import baritone.api.utils.RayTraceUtils;
import baritone.api.utils.RotationUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;

public interface WorldUtil {

    static Vec3d toVec3d(Vec3i pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }
    static Vec3i toVec3i(Vec3d pos) {
        return new Vec3i(pos.getX(), pos.getY(), pos.getZ());
    }

    static boolean isSourceBlock(AltoClef mod, BlockPos pos) {
        BlockState s = mod.getWorld().getBlockState(pos);
        if (s.getBlock() instanceof FluidBlock) {
            // Only accept still fluids.
            if (!s.getFluidState().isStill()) return false;
            int level = s.getFluidState().getLevel();
            // Ignore if there's liquid above, we can't tell if it's a source block or not.
            BlockState above = mod.getWorld().getBlockState(pos.up());
            if (above.getBlock() instanceof FluidBlock) return false;
            return level == 8;
        }
        return false;
    }

    static boolean isSolid(AltoClef mod, BlockPos pos) {
        return mod.getWorld().getBlockState(pos).isSolidBlock(mod.getWorld(), pos);
    }
}

package adris.altoclef.util;


import adris.altoclef.AltoClef;
import baritone.api.BaritoneAPI;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CartographyTableBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.LoomBlock;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;


public final class WorldUtil {

    private WorldUtil() {
    }

    public static Vec3d toVec3d(Vec3i pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3i toVec3i(Vec3d pos) {
        return new Vec3i(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean isSourceBlock(AltoClef mod, BlockPos pos, boolean onlyAcceptStill) {
        BlockState s = mod.getWorld().getBlockState(pos);
        if (s.getBlock() instanceof FluidBlock) {
            // Only accept still fluids.
            if (!s.getFluidState().isStill() && onlyAcceptStill)
                return false;
            int level = s.getFluidState().getLevel();
            // Ignore if there's liquid above, we can't tell if it's a source block or not.
            BlockState above = mod.getWorld().getBlockState(pos.up());
            if (above.getBlock() instanceof FluidBlock)
                return false;
            return level == 8;
        }
        return false;
    }

    public static boolean isSolid(AltoClef mod, BlockPos pos) {
        return mod.getWorld().getBlockState(pos).isSolidBlock(mod.getWorld(), pos);
    }

    public static BlockPos getBedHead(AltoClef mod, BlockPos posWithBed) {
        BlockState state = mod.getWorld().getBlockState(posWithBed);
        if (state.getBlock() instanceof BedBlock) {
            Direction facing = state.get(BedBlock.FACING);
            if (mod.getWorld().getBlockState(posWithBed).get(BedBlock.PART) == BedPart.HEAD) {
                return posWithBed;
            }
            return posWithBed.offset(facing);
        }
        return null;
    }

    // Get the left side of a chest, given a block pos.
    // Used to consistently identify whether a double chest is part of the same chest.
    public static BlockPos getChestLeft(AltoClef mod, BlockPos posWithChest) {
        BlockState state = mod.getWorld().getBlockState(posWithChest);
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            if (type == ChestType.SINGLE || type == ChestType.LEFT) {
                return posWithChest;
            }
            Direction facing = state.get(ChestBlock.FACING);
            return posWithChest.offset(facing.rotateYCounterclockwise());
        }
        return null;
    }

    public static boolean isChestBig(AltoClef mod, BlockPos posWithChest) {
        BlockState state = mod.getWorld().getBlockState(posWithChest);
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            return (type == ChestType.RIGHT || type == ChestType.LEFT);
        }
        return false;
    }

    public static int getGroundHeight(AltoClef mod, int x, int z) {
        for (int y = 255; y >= 0; --y) {
            BlockPos check = new BlockPos(x, y, z);
            if (isSolid(mod, check))
                return y;
        }
        return -1;
    }

    public static int getGroundHeight(AltoClef mod, int x, int z, Block... groundBlocks) {
        Set<Block> possibleBlocks = new HashSet<>(Arrays.asList(groundBlocks));
        for (int y = 255; y >= 0; --y) {
            BlockPos check = new BlockPos(x, y, z);
            if (possibleBlocks.contains(mod.getWorld().getBlockState(check).getBlock()))
                return y;

        }
        return -1;
    }

    public static boolean canBreak(AltoClef mod, BlockPos pos) {
        return mod.getWorld().getBlockState(pos).getHardness(mod.getWorld(), pos) >= 0 &&
               !mod.getExtraBaritoneSettings().shouldAvoidBreaking(pos);
    }

    public static boolean canPlace(AltoClef mod, BlockPos pos) {
        return !mod.getExtraBaritoneSettings().shouldAvoidPlacingAt(pos);
    }

    public static boolean isAir(AltoClef mod, BlockPos pos) {
        return mod.getBlockTracker().blockIsValid(pos, Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR);
        //return state.isAir() || isAir(state.getBlock());
    }

    public static boolean isAir(Block block) {
        return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
    }

    public static boolean isContainerBlock(AltoClef mod, BlockPos pos) {
        Block block = mod.getWorld().getBlockState(pos).getBlock();
        return (block instanceof ChestBlock || block instanceof EnderChestBlock || block instanceof CraftingTableBlock ||
                block instanceof AbstractFurnaceBlock || block instanceof LoomBlock || block instanceof CartographyTableBlock ||
                block instanceof EnchantingTableBlock);
    }

    public static boolean isInsidePlayer(AltoClef mod, BlockPos pos) {
        return pos.isWithinDistance(mod.getPlayer().getPos(), 2);
    }

    public static Iterable<BlockPos> scanRegion(AltoClef mod, BlockPos start, BlockPos end) {
        return () -> new Iterator<BlockPos>() {
            int x = start.getX(); // TODO: 2021-05-22 what the shit?
            int y = start.getY();
            int z = start.getZ();

            @Override
            public boolean hasNext() {
                return y <= end.getX() && z <= end.getZ() && x <= end.getX();
            }

            @Override
            public BlockPos next() {
                BlockPos result = new BlockPos(x, y, z);
                ++x;
                if (x > end.getX()) {
                    x = start.getX();
                    ++z;
                    if (z > end.getZ()) {
                        z = start.getZ();
                        ++y;
                    }
                }
                return result;
            }
        };
    }

    public static boolean fallingBlockSafeToBreak(BlockPos pos) {
        BlockPos blockPos = pos;
        BlockStateInterface bsi = new BlockStateInterface(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext());
        World w = MinecraftClient.getInstance().world;
        while (isFallingBlock(blockPos)) {
            if (MovementHelper.avoidBreaking(bsi, blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                                             Objects.requireNonNull(w).getBlockState(blockPos)))
                return false;
            blockPos = blockPos.up();
        }
        return true;
    }

    public static boolean isFallingBlock(BlockPos pos) {
        World w = MinecraftClient.getInstance().world;
        return FallingBlock.class.isAssignableFrom(Objects.requireNonNull(w).getBlockState(pos).getBlock().getClass());
    }

    public static Entity getSpawnerEntity(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        if (state.getBlock() instanceof SpawnerBlock) {
            BlockEntity be = mod.getWorld().getBlockEntity(pos);
            if (be instanceof MobSpawnerBlockEntity) {
                MobSpawnerBlockEntity blockEntity = (MobSpawnerBlockEntity) be;
                return blockEntity.getLogic().getRenderedEntity();
            }
        }
        return null;
    }
}

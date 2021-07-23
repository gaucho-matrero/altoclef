package adris.altoclef.util;

import adris.altoclef.AltoClef;
import baritone.api.BaritoneAPI;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.process.MineProcess;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.*;
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
import java.util.Set;

public interface WorldUtil {

    static Vec3d toVec3d(Vec3i pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }

    static Vec3i toVec3i(Vec3d pos) {
        return new Vec3i(pos.getX(), pos.getY(), pos.getZ());
    }

    static boolean isSourceBlock(AltoClef mod, BlockPos pos, boolean onlyAcceptStill) {
        BlockState s = mod.getWorld().getBlockState(pos);
        if (s.getBlock() instanceof FluidBlock) {
            // Only accept still fluids.
            if (!s.getFluidState().isStill() && onlyAcceptStill) return false;
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

    static BlockPos getBedHead(AltoClef mod, BlockPos posWithBed) {
        BlockState state = mod.getWorld().getBlockState(posWithBed);
        if (state.getBlock() instanceof BedBlock) {
            Direction facing = state.get(BedBlock.FACING);
            if (mod.getWorld().getBlockState(posWithBed).get(BedBlock.PART).equals(BedPart.HEAD)) {
                return posWithBed;
            }
            return posWithBed.offset(facing);
        }
        return null;
    }

    // Get the left side of a chest, given a block pos.
    // Used to consistently identify whether a double chest is part of the same chest.
    static BlockPos getChestLeft(AltoClef mod, BlockPos posWithChest) {
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

    static boolean isChestBig(AltoClef mod, BlockPos posWithChest) {
        BlockState state = mod.getWorld().getBlockState(posWithChest);
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            return (type == ChestType.RIGHT || type == ChestType.LEFT);
        }
        return false;
    }

    static int getGroundHeight(AltoClef mod, int x, int z) {
        for (int y = 255; y >= 0; --y) {
            BlockPos check = new BlockPos(x, y, z);
            if (isSolid(mod, check)) return y;
        }
        return -1;
    }

    static int getGroundHeight(AltoClef mod, int x, int z, Block... groundBlocks) {
        Set<Block> possibleBlocks = new HashSet<>(Arrays.asList(groundBlocks));
        for (int y = 255; y >= 0; --y) {
            BlockPos check = new BlockPos(x, y, z);
            if (possibleBlocks.contains(mod.getWorld().getBlockState(check).getBlock())) return y;

        }
        return -1;
    }

    static boolean canBreak(AltoClef mod, BlockPos pos) {
        return mod.getWorld().getBlockState(pos).getHardness(mod.getWorld(), pos) >= 0
                && !mod.getExtraBaritoneSettings().shouldAvoidBreaking(pos)
                && MineProcess.plausibleToBreak(new CalculationContext(mod.getClientBaritone()), pos);
    }

    static boolean canPlace(AltoClef mod, BlockPos pos) {
        return !mod.getExtraBaritoneSettings().shouldAvoidPlacingAt(pos);
    }

    static boolean isAir(AltoClef mod, BlockPos pos) {
        return mod.getBlockTracker().blockIsValid(pos, Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR);
        //return state.isAir() || isAir(state.getBlock());
    }

    static boolean isAir(Block block) {
        return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
    }

    static boolean isContainerBlock(AltoClef mod, BlockPos pos) {
        Block block = mod.getWorld().getBlockState(pos).getBlock();
        return (block instanceof ChestBlock
                || block instanceof EnderChestBlock
                || block instanceof CraftingTableBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof LoomBlock
                || block instanceof CartographyTableBlock
                || block instanceof EnchantingTableBlock
        );
    }

    static boolean isInsidePlayer(AltoClef mod, BlockPos pos) {
        return pos.isWithinDistance(mod.getPlayer().getPos(), 2);
    }

    static Iterable<BlockPos> scanRegion(AltoClef mod, BlockPos start, BlockPos end) {
        return () -> new Iterator<BlockPos>() {
            int x = start.getX(), y = start.getY(), z = start.getZ();

            @Override
            public boolean hasNext() {
                return y <= end.getY() && z <= end.getZ() && x <= end.getX();
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

    static boolean fallingBlockSafeToBreak(BlockPos pos) {
        BlockStateInterface bsi = new BlockStateInterface(BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext());
        World w = MinecraftClient.getInstance().world;
        assert w != null;
        while (isFallingBlock(pos)) {
            if (MovementHelper.avoidBreaking(bsi, pos.getX(), pos.getY(), pos.getZ(), w.getBlockState(pos)))
                return false;
            pos = pos.up();
        }
        return true;
    }

    static boolean isFallingBlock(BlockPos pos) {
        World w = MinecraftClient.getInstance().world;
        assert w != null;
        return w.getBlockState(pos).getBlock() instanceof FallingBlock;
    }

    static Entity getSpawnerEntity(AltoClef mod, BlockPos pos) {
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

    static Vec3d blockCenter(BlockPos block) {
        return new Vec3d(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
    }

    static Vec3d getOverworldPosition(AltoClef mod, Vec3d pos) {
        if (mod.getCurrentDimension() == Dimension.NETHER) {
            pos = pos.multiply(8.0, 1, 8.0);
        }
        return pos;
    }
    static BlockPos getOverworldPosition(AltoClef mod, BlockPos pos) {
        if (mod.getCurrentDimension() == Dimension.NETHER) {
            pos = new BlockPos(pos.getX()*8, pos.getY(), pos.getZ()*8);
        }
        return pos;
    }
}

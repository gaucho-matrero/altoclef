package adris.altoclef.util;


import adris.altoclef.AltoClef;
import adris.altoclef.mixins.ClientPlayerInteractionAccessor;
import adris.altoclef.mixins.MinecraftMouseInputAccessor;
import adris.altoclef.util.csharpisbetter.Action;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Objects;


public class PlayerExtraController {
    private static final double INTERACT_RANGE = 6;
    public final Action<BlockBrokenEvent> onBlockBroken = new Action<>();
    public final Action<BlockPlaceEvent> onBlockPlaced = new Action<>();
    private final AltoClef mod;
    private ClientPlayNetworkHandler networkHandler;
    private BlockPos blockBreakPos;
    private double blockBreakProgress;


    public PlayerExtraController(AltoClef mod) {
        this.mod = mod;
    }

    public void onBlockBreak(BlockPos pos, double progress) {
        blockBreakPos = pos;
        blockBreakProgress = progress;
    }

    public void onBlockStopBreaking() {
        blockBreakPos = null;
        blockBreakProgress = 0;
    }

    public void onBlockBroken(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world == mod.getWorld()) {
            BlockBrokenEvent evt = new BlockBrokenEvent();
            evt.blockPos = pos;
            evt.blockState = state;
            evt.player = player;
            onBlockBroken.invoke(evt);
        }
    }

    public void onBlockPlaced(BlockPos pos, BlockState state) {
        BlockPlaceEvent evt = new BlockPlaceEvent();
        evt.blockPos = pos;
        evt.blockState = state;
        onBlockPlaced.invoke(evt);
    }

    public BlockPos getBreakingBlockPos() {
        return blockBreakPos;
    }

    public boolean isBreakingBlock() {
        return blockBreakPos != null;
    }

    public double getBreakingBlockProgress() {
        return blockBreakProgress;
    }

    public boolean inRange(Entity entity) {
        return mod.getPlayer().isInRange(entity, INTERACT_RANGE);
    }

    public void attack(Entity entity) {
        if (inRange(entity)) {
            mod.getController().attackEntity(mod.getPlayer(), entity);
        }
    }

    @SuppressWarnings("CastToIncompatibleInterface")
    public void dropCurrentStack(boolean single) {
        ((ClientPlayerInteractionAccessor) Objects.requireNonNull(MinecraftClient.getInstance().interactionManager)).doSendPlayerAction(
                single ? PlayerActionC2SPacket.Action.DROP_ITEM : PlayerActionC2SPacket.Action.DROP_ALL_ITEMS, new BlockPos(0, 0, 0),
                Direction.fromRotation(0));
        mod.getInventoryTracker().setDirty();
    }

    @SuppressWarnings("CastToIncompatibleInterface")
    public void mouseClickOverride(int button, boolean down) {
        MinecraftMouseInputAccessor mouse = (MinecraftMouseInputAccessor) MinecraftClient.getInstance().mouse;
        mouse.mouseClick(MinecraftClient.getInstance().getWindow().getHandle(), button, down ? 1 : 0, 0);
    }


    public static class BlockBrokenEvent {
        public BlockPos blockPos;
        public BlockState blockState;
        public PlayerEntity player;
    }


    public static class BlockPlaceEvent {
        public BlockPos blockPos;
        public BlockState blockState;
    }
}

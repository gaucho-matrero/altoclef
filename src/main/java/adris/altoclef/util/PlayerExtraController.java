package adris.altoclef.util;

import adris.altoclef.AltoClef;
import adris.altoclef.mixins.ClientPlayerInteractionAccessor;
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

public class PlayerExtraController {

    private static final double INTERACT_RANGE = 6;
    public final Action<BlockBrokenEvent> onBlockBroken = new Action<>();
    public final Action<BlockPlaceEvent> onBlockPlaced = new Action<>();
    private final AltoClef _mod;
    private ClientPlayNetworkHandler _networkHandler;
    private BlockPos _blockBreakPos;
    private double _blockBreakProgress;

    public PlayerExtraController(AltoClef mod) {
        _mod = mod;
    }

    public void onBlockBreak(BlockPos pos, double progress) {
        _blockBreakPos = pos;
        _blockBreakProgress = progress;
    }

    public void onBlockStopBreaking() {
        _blockBreakPos = null;
        _blockBreakProgress = 0;
    }

    public void onBlockBroken(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world == _mod.getWorld()) {
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
        return _blockBreakPos;
    }

    public boolean isBreakingBlock() {
        return _blockBreakPos != null;
    }

    public double getBreakingBlockProgress() {
        return _blockBreakProgress;
    }

    public boolean inRange(Entity entity) {
        return _mod.getPlayer().isInRange(entity, INTERACT_RANGE);
    }

    public void attack(Entity entity) {
        if (inRange(entity)) {
            _mod.getController().attackEntity(_mod.getPlayer(), entity);
        }
    }

    public void dropCurrentStack(boolean single) {
        assert MinecraftClient.getInstance().interactionManager != null;
        ((ClientPlayerInteractionAccessor) MinecraftClient.getInstance().interactionManager).doSendPlayerAction(
                single ? PlayerActionC2SPacket.Action.DROP_ITEM : PlayerActionC2SPacket.Action.DROP_ALL_ITEMS,
                new BlockPos(0, 0, 0), Direction.fromRotation(0)
        );
        _mod.getInventoryTracker().setDirty();
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

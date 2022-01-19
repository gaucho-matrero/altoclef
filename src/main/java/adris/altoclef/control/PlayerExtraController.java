package adris.altoclef.control;

import adris.altoclef.AltoClef;
import adris.altoclef.util.csharpisbetter.Action;
import adris.altoclef.util.csharpisbetter.TimerGame;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PlayerExtraController {

    public final Action<BlockBrokenEvent> onBlockBroken = new Action<>();
    public final Action<BlockPlaceEvent> onBlockPlaced = new Action<>();
    private final AltoClef _mod;
    private BlockPos _blockBreakPos;
    private double _blockBreakProgress;

    // TODO: Settings Parameters?
    private final TimerGame _placeTimer = new TimerGame(0.2);

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
        return _mod.getPlayer().isInRange(entity, _mod.getModSettings().getEntityReachRange());
    }

    public void attack(Entity entity) {
        if (inRange(entity)) {
            _mod.getController().attackEntity(_mod.getPlayer(), entity);
            _mod.getPlayer().swingHand(Hand.MAIN_HAND);
        }
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

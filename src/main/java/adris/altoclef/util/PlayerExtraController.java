package adris.altoclef.util;

import adris.altoclef.AltoClef;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class PlayerExtraController {

    private AltoClef _mod;

    private ClientPlayNetworkHandler _networkHandler;

    private BlockPos _blockBreakPos;
    private double _blockBreakProgress;

    private static final double INTERACT_RANGE = 6;

    public PlayerExtraController(AltoClef mod) {
        _mod = mod;
    }

    public void onBlockBreak(BlockPos pos, double progress) {
        _blockBreakPos = pos;
        _blockBreakProgress = progress;
    }

    public BlockPos getBreakingBlockPos() {
        return _blockBreakPos;
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

}

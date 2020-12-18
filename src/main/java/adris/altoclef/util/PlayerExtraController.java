package adris.altoclef.util;

import adris.altoclef.AltoClef;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.math.BlockPos;

public class PlayerExtraController {

    private AltoClef _mod;

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

    public BlockPos getBreakingBlockPos() {
        return _blockBreakPos;
    }
    public double getBreakingBlockProgress() {
        return _blockBreakProgress;
    }

}

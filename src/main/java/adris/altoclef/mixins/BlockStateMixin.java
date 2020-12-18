package adris.altoclef.mixins;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractBlock.class)
public final class BlockStateMixin {
    @Inject(
            method = "onBlockAdded",
            at = @At("HEAD")
    )
    private void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
        boolean placedNew = (oldState.isAir() || oldState.getBlock() instanceof FluidBlock) && !state.isAir();
        boolean brokenNew = state.isAir() && !oldState.isAir();

        boolean same = (oldState.getBlock().is(state.getBlock()));

        // Ignore if we're the same block.
        if (same) {
            return;
        }

        if (placedNew) {
            AltoClef.getInstance().onBlockAdd(state, pos);
        } else if (brokenNew) {
            AltoClef.getInstance().onBlockRemove(oldState, pos);
        } else {
            // A replacement was done.
            AltoClef.getInstance().onBlockChange(oldState, state, pos);
        }
    }
}
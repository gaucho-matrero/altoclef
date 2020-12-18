package adris.altoclef.mixins;

import adris.altoclef.AltoClef;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public final class ClientBlockBreakMixin {

    @Shadow
    private float currentBreakingProgress;

    @Inject(
            method = "updateBlockBreakingProgress",
            at = @At("HEAD")
    )
    private void onBreakUpdate(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> ci) {
        AltoClef.getInstance().onBlockBreaking(pos, currentBreakingProgress);
    }
}
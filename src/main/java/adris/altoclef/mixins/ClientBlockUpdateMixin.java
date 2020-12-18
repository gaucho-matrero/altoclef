package adris.altoclef.mixins;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Deprecated
@Mixin(ClientPlayNetworkHandler.class)
public final class ClientBlockUpdateMixin {
    @Inject(
            method = "onBlockUpdate",
            at = @At("HEAD")
    )
    private void onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        //Debug.logInternal("BLOCK UPDATED: " + packet.getState().getBlock().getTranslationKey());
    }
}
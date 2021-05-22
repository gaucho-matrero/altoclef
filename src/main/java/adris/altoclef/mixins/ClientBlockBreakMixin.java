package adris.altoclef.mixins;

import adris.altoclef.StaticMixinHookups;
import net.minecraft.client.network.ClientPlayerInteractionManager;
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
    
    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onBreakUpdate(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> ci) {
        StaticMixinHookups.onBlockBreaking(pos, currentBreakingProgress);
    }
    
    @Inject(method = "cancelBlockBreaking", at = @At("HEAD"))
    private void cancelBlockBreaking(CallbackInfo ci) {
        StaticMixinHookups.onBlockCancelBreaking();
    }
}
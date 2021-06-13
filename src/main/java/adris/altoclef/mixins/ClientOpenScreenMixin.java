package adris.altoclef.mixins;

import adris.altoclef.StaticMixinHookups;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(MinecraftClient.class)
public final class ClientOpenScreenMixin {
    @Inject(
            method = "openScreen",
            at = @At("HEAD")
    )
    private void onScreenOpenBegin(@Nullable Screen screen, CallbackInfo ci) {
        StaticMixinHookups.onScreenOpenBegin(screen);
    }

    @Inject(
            method = "openScreen",
            at = @At("TAIL")
    )
    private void onScreenOpenEnd(@Nullable Screen screen, CallbackInfo ci) {
        StaticMixinHookups.onScreenOpenEnd(screen);
    }
}

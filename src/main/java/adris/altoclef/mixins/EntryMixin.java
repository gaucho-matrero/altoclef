package adris.altoclef.mixins;


import adris.altoclef.Debug;
import adris.altoclef.StaticMixinHookups;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TitleScreen.class)
public class EntryMixin {
    private static boolean initialized;
    
    @Inject(at = @At("HEAD"), method = "init()V")
    private void init(CallbackInfo info) {
        if (!initialized) {
            initialized = true;
            Debug.logMessage("Global Init");
            StaticMixinHookups.onInitializeLoad();
        }
    }
}


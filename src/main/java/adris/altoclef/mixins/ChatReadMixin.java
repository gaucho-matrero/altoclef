package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ChatMessageEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPlayerEntity.class)
public final class ChatReadMixin {

    @Inject(
            method = "sendChatMessage",
            at = @At("HEAD")
    )
    private void onChatMessage(String message, Text preview, CallbackInfo ci) {
        ChatMessageEvent evt = new ChatMessageEvent(message, preview);
        EventBus.publish(evt);
    }
}
package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ChatMessageEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPlayNetworkHandler.class)
public final class ChatReadMixin {
    @Inject(
            method = "onChatMessage",
            at = @At("HEAD")
    )
    private void onChatMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
        ChatMessageEvent evt = new ChatMessageEvent(packet);
        EventBus.publish(evt);
    }
}
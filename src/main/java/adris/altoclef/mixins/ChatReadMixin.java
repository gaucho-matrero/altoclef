package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ChatMessageEvent;
import net.minecraft.client.gui.hud.ChatHudListener;
import net.minecraft.network.MessageType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;


@Mixin(ChatHudListener.class)
public final class ChatReadMixin {

    @Inject(
            method = "onChatMessage",
            at = @At("HEAD")
    )
    private void onChatMessage(MessageType messageType, Text message, UUID senderUuid, CallbackInfo ci) {
        ChatMessageEvent evt = new ChatMessageEvent(messageType, message);
        EventBus.publish(evt);
    }
}
package adris.altoclef.mixins;

import adris.altoclef.Debug;
import adris.altoclef.StaticMixinHookups;
import adris.altoclef.util.csharpisbetter.Timer;
import baritone.api.event.events.ChatEvent;
import jdk.internal.dynalink.beans.StaticClass;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPlayNetworkHandler.class)
public final class ChatReadMixin {

    private static final String MIDDLE_PART = " whispers to you: ";

    private static final Timer _repeatTimer = new Timer(0.1);

    private static String _lastUser = null, _lastMessage = null;

    @Inject(
            method = "onGameMessage",
            at = @At("HEAD")
    )
    private void onGameMessage(GameMessageS2CPacket msgPacket, CallbackInfo ci) {
        if (msgPacket.isNonChat() && msgPacket.getLocation() == MessageType.SYSTEM) {

            // Format: <USER> whispers to you: <MESSAGE>
            String msg = msgPacket.getMessage().getString();
            int index = msg.indexOf(MIDDLE_PART);
            if (index != -1) {
                String user = msg.substring(0, index);
                String message = msg.substring(index + MIDDLE_PART.length());

                //noinspection ConstantConditions
                if (user == null || message == null) return;
                boolean duplicate = (user.equals(_lastUser) && message.equals(_lastMessage));

                if (duplicate && !_repeatTimer.elapsed()) {
                    // It's probably an actual duplicate. IDK why we get those but yeah.
                    return;
                }

                _lastUser = user;
                _lastMessage = message;
                _repeatTimer.reset();

                Debug.logInternal("USER: \"" + user + "\" MESSAGE: \"" + message + "\" " + msgPacket.isWritingErrorSkippable() + " : " + msgPacket.getSenderUuid());
                StaticMixinHookups.onWhisperReceive(user, message);
            }
        }
    }
}
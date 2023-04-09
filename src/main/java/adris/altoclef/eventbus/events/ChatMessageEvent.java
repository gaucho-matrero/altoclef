package adris.altoclef.eventbus.events;

import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import java.lang.String;

/**
 * Whenever chat appears
 */
public class ChatMessageEvent {
    ChatMessageS2CPacket packet;


    public ChatMessageEvent(ChatMessageS2CPacket packet) {
        this.packet = packet;
    
    public String messageContent() {
        return packet.body().content();
    }
    }
}

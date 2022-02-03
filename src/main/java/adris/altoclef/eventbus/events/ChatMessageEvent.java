package adris.altoclef.eventbus.events;

import net.minecraft.network.MessageType;
import net.minecraft.text.Text;

/**
 * Whenever chat appears
 */
public class ChatMessageEvent {
    public MessageType messageType;
    public Text message;

    public ChatMessageEvent(MessageType messageType, Text message) {
        this.messageType = messageType;
        this.message = message;
    }
}

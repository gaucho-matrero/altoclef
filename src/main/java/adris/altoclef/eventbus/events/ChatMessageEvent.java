package adris.altoclef.eventbus.events;

import net.minecraft.text.Text;

/**
 * Whenever chat appears
 */
public class ChatMessageEvent {
    public String message;
    public Text preview;

    public ChatMessageEvent(String message, Text preview) {
        this.message = message;
        this.preview = preview;
    }
}

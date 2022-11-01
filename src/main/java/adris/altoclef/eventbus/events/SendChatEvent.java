package adris.altoclef.eventbus.events;

import net.minecraft.text.Text;

public class SendChatEvent {
    public String message;
    public Text preview;
    private boolean _cancelled;

    public SendChatEvent(String message, Text preview) {
        this.message = message;
        this.preview = preview;
    }

    public void cancel() {
        _cancelled = true;
    }

    public boolean isCancelled() {
        return _cancelled;
    }
}

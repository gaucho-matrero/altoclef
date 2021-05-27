package adris.altoclef.ui;


import adris.altoclef.Debug;
import adris.altoclef.util.csharpisbetter.Timer;
import net.minecraft.client.MinecraftClient;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;


/**
 * We can't send messages immediately as the server will kick us. As such, we will send messages in a delayed queued fashion.
 */
public class MessageSender {
    // TODO: 2021-05-22 god, this class is horrible (no offense)
    // How many messages can we send quickly before giving a little pause?
    private static final int FAST_LIMIT = 6;
    private static final int SLOW_LIMIT = 3;

    private final PriorityQueue<BaseMessage> whisperQueue = new PriorityQueue<>(
            Comparator.comparingInt((BaseMessage msg) -> msg.priority.getImportance()).thenComparingInt(msg -> msg.index));
    //private final Queue<Whisper> _whisperQueue = new ArrayDeque<>();

    private final Timer fastSendTimer = new Timer(0.3f);
    private final Timer bigSendTimer = new Timer(3.5);
    private final Timer bigBigSendTimer = new Timer(10);

    private int messageCounter;

    private int fastCount;
    private int slowCount;

    public void tick() {
        if (bigBigSendTimer.elapsed()) { // what the fuck
            if (bigSendTimer.elapsed()) {
                if (fastSendTimer.elapsed()) {
                    if (!whisperQueue.isEmpty()) {
                        fastSendTimer.reset();
                        BaseMessage msg = whisperQueue.poll();
                        sendChatInstant(Objects.requireNonNull(msg).getChatInput());
                        fastCount++;
                        if (fastCount >= FAST_LIMIT) {
                            bigSendTimer.reset();
                            fastCount = 0;
                            slowCount++;
                            if (slowCount >= SLOW_LIMIT) {
                                bigBigSendTimer.reset();
                                slowCount = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    public void enqueueWhisper(String username, String message, MessagePriority priority) {
        whisperQueue.add(new Whisper(username, message, priority, messageCounter++));
    }

    public void enqueueChat(String message, MessagePriority priority) {
        whisperQueue.add(new ChatMessage(message, priority, messageCounter++));
    }


    private void sendChatInstant(String message) {
        if (MinecraftClient.getInstance().player == null) {
            Debug.logError("Failed to send chat message as no client loaded.");
            return;
        }
        MinecraftClient.getInstance().player.sendChatMessage(message);
    }

    private abstract static class BaseMessage {
        public MessagePriority priority;
        public int index;

        public BaseMessage(MessagePriority priority, int index) {
            this.priority = priority;
            this.index = index;
        }

        public abstract String getChatInput();
    }


    private static class Whisper extends BaseMessage {
        public String username;
        public String message;

        public Whisper(String username, String message, MessagePriority priority, int index) {
            super(priority, index);
            this.username = username;
            this.message = message;
        }

        @Override
        public String getChatInput() {
            return "/msg " + username + " " + message;
        }
    }


    private static class ChatMessage extends BaseMessage {

        public String message;

        public ChatMessage(String message, MessagePriority priority, int index) {
            super(priority, index);
            this.message = message;

        }

        @Override
        public String getChatInput() {
            return message;
        }
    }
}

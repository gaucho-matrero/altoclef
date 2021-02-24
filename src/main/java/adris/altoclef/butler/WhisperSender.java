package adris.altoclef.butler;

import adris.altoclef.Debug;
import adris.altoclef.util.csharpisbetter.Timer;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * We can't send whispers immediately as the server will kick us.
 * As such, we will send whispers in a delayed queued fashion.
 */
public class WhisperSender {

    // How many messages can we send quickly before giving a little pause?
    private static final int FAST_LIMIT = 6;
    private static final int SLOW_LIMIT = 3;

    private final PriorityQueue<Whisper> _whisperQueue = new PriorityQueue<>(
            (left, right) -> {
                int deltaImportance = right.priority.getImportance() - left.priority.getImportance();
                if (deltaImportance != 0) {
                    return deltaImportance;
                }
                return right.index - left.index;
            }
    );
    //private final Queue<Whisper> _whisperQueue = new ArrayDeque<>();

    private final Timer _fastSendTimer = new Timer(0.3f);
    private final Timer _bigSendTimer = new Timer(3.5);
    private final Timer _bigBigSendTimer = new Timer(10);

    private int _whisperCounter = 0;

    private int _fastCount;
    private int _slowCount;

    public void tick() {
        if (_bigBigSendTimer.elapsed()) {
            if (_bigSendTimer.elapsed()) {
                if (_fastSendTimer.elapsed()) {
                    if (!_whisperQueue.isEmpty()) {
                        _fastSendTimer.reset();
                        Whisper msg = _whisperQueue.poll();
                        assert msg != null;
                        sendWhisperInstant(msg.username, msg.message);
                        _fastCount++;
                        if (_fastCount >= FAST_LIMIT) {
                            _bigSendTimer.reset();
                            _fastCount = 0;
                            _slowCount++;
                            if (_slowCount >= SLOW_LIMIT) {
                                _bigBigSendTimer.reset();
                                _slowCount = 0;
                            }
                        }
                    }
                }
            }
        }
    }
    public void enqueueWhisper(String username, String message, WhisperPriority priority) {
        _whisperQueue.add(new Whisper(username, message, priority, _whisperCounter++));
    }


    private void sendWhisperInstant(String username, String message) {
        if (MinecraftClient.getInstance().player == null) {
            Debug.logError("Failed to send chat message as no client loaded.");
            return;
        }
        MinecraftClient.getInstance().player.sendChatMessage("/msg " + username + " " + message);
    }

    private static class Whisper {
        public String username;
        public String message;
        public WhisperPriority priority;
        public int index;

        public Whisper(String username, String message, WhisperPriority priority, int index) {
            this.username = username;
            this.message = message;
            this.priority = priority;
            this.index = index;
        }

    }
}

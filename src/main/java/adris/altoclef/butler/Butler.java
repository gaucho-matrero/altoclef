package adris.altoclef.butler;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.ui.MessagePriority;
import adris.altoclef.util.csharpisbetter.ActionListener;
import net.minecraft.client.MinecraftClient;

/**
 * The butler system lets authorized players send commands to the bot to execute.
 * <p>
 * This effectively makes the bot function as a servant, or butler.
 * <p>
 * Authorization is defined in "altoclef_butler_whitelist.txt" and "altoclef_butler_blacklist.txt"
 * and depends on the "useButlerWhitelist" and "useButlerBlacklist" settings in "altoclef_settings.json"
 */
public class Butler {

    private static final String BUTLER_MESSAGE_START = "` ";

    private final AltoClef _mod;

    private final WhisperChecker _whisperChecker = new WhisperChecker();

    private final UserAuth _userAuth;

    private String _currentUser = null;

    // Utility variables for command logic
    private boolean _commandInstantRan = false;
    private boolean _commandFinished = false;

    public Butler(AltoClef mod) {
        _mod = mod;
        _userAuth = new UserAuth(mod);
        _mod.getUserTaskChain().onTaskFinish.addListener(
                new ActionListener<>(msg -> {
                    if (_currentUser != null) {
                        //sendWhisper("Finished. " + msg);
                        _currentUser = null;
                    }
                })
        );
    }

    public void reloadLists() {
        _userAuth.reloadLists();
    }

    public void receiveMessage(String msg) {
        // Format: <USER> whispers to you: <MESSAGE>
        // Format: <USER> whispers: <MESSAGE>
        String ourName = MinecraftClient.getInstance().getName();
        WhisperChecker.MessageResult result = this._whisperChecker.receiveMessage(_mod, ourName, msg);
        if (result != null) {
            this.receiveWhisper(result.from, result.message);
        }
    }

    public void receiveWhisper(String username, String message) {

        // Ignore messages from other bots.
        if (message.startsWith(BUTLER_MESSAGE_START)) return;

        if (_userAuth.isUserAuthorized(username)) {
            executeWhisper(username, message);
        } else {
            sendWhisper(username, "Sorry, you're not authorized!", MessagePriority.UNAUTHORIZED);
        }
    }

    public boolean isUserAuthorized(String username) {
        return _userAuth.isUserAuthorized(username);
    }

    public void onLog(String message, MessagePriority priority) {
        if (_currentUser != null) {
            sendWhisper(message, priority);
        }
    }

    public void onLogWarning(String message, MessagePriority priority) {
        if (_currentUser != null) {
            sendWhisper("[WARNING:] " + message, priority);
        }
    }

    public void tick() {
        // Nothing for now.
    }

    public String getCurrentUser() {
        return _currentUser;
    }

    public boolean hasCurrentUser() {
        return _currentUser != null;
    }

    private void executeWhisper(String username, String message) {
        String prevUser = _currentUser;
        try {
            _commandInstantRan = true;
            _commandFinished = false;
            _currentUser = username;
            sendWhisper("Command Executing: " + message, MessagePriority.TIMELY);
            _mod.getCommandExecutor().Execute("@" + message, (nothing) -> {
                // On finish
                sendWhisper("Command Finished: " + message, MessagePriority.TIMELY);
                if (!_commandInstantRan) {
                    _currentUser = null;
                }
                _commandFinished = true;
            });
            _commandInstantRan = false;
        } catch (CommandException e) {
            sendWhisper("TASK FAILED: " + e.getMessage(), MessagePriority.ASAP);
            _currentUser = null;
            e.printStackTrace();
        }
        // Only set the current user if we're still running.
        if (_commandFinished) {
            _currentUser = prevUser;
        }
    }

    private void sendWhisper(String message, MessagePriority priority) {
        if (_currentUser != null) {
            sendWhisper(_currentUser, message, priority);
        } else {
            Debug.logWarning("Failed to send butler message as there are no users present: " + message);
        }
    }

    private void sendWhisper(String username, String message, MessagePriority priority) {
        _mod.getMessageSender().enqueueWhisper(username, BUTLER_MESSAGE_START + message, priority);
    }
}

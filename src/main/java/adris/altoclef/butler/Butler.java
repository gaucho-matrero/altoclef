package adris.altoclef.butler;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commands.CommandException;
import adris.altoclef.util.csharpisbetter.ActionListener;

public class Butler {

    private final AltoClef _mod;

    private final UserAuth _userAuth;

    private final WhisperSender _whisperer;

    private String _currentUser = null;

    // Utility variables for command logic
    private boolean _commandInstantRan = false;
    private boolean _commandFinished = false;

    public Butler(AltoClef mod) {
        _mod = mod;
        _userAuth = new UserAuth(mod);
        _whisperer = new WhisperSender();
        _mod.getUserTaskChain().onTaskFinish.addListener(
                new ActionListener<String>() {
                    @Override
                    public void invoke(String msg) {
                        if (_currentUser != null) {
                            //sendWhisper("Finished. " + msg);
                            _currentUser = null;
                        }
                    }
                }
        );
    }

    public void reloadLists() {
        _userAuth.reloadLists();
    }

    public void receiveWhisper(String username, String message) {
        if (_userAuth.isUserAuthorized(username)) {
            executeWhisper(username, message);
        } else {
            sendWhisper(username, "Sorry, you're not authorized!", WhisperPriority.UNAUTHORIZED);
        }
    }

    public void onLog(String message, WhisperPriority priority) {
        if (_currentUser != null) {
            sendWhisper(message, priority);
        }
    }
    public void onLogWarning(String message, WhisperPriority priority) {
        if (_currentUser != null) {
            sendWhisper("[WARNING:] " + message, priority);
        }
    }

    public void tick() {
        _whisperer.tick();
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
            sendWhisper("Command Executing: " + message, WhisperPriority.TIMELY);
            _mod.getCommandExecutor().Execute("@" + message, (nothing) -> {
                // On finish
                sendWhisper("Command Finished: " + message, WhisperPriority.TIMELY);
                if (!_commandInstantRan) {
                    _currentUser = null;
                }
                _commandFinished = true;
            });
            _commandInstantRan = false;
        } catch (CommandException e) {
            sendWhisper("TASK FAILED: " + e.getMessage(), WhisperPriority.ASAP);
            _currentUser = null;
            e.printStackTrace();
        }
        // Only set the current user if we're still running.
        if (_commandFinished) {
            _currentUser = prevUser;
        }
    }

    private void sendWhisper(String message, WhisperPriority priority) {
        if (_currentUser != null) {
            sendWhisper(_currentUser, message, priority);
        } else {
            Debug.logWarning("Failed to send butler message as there are no users present: " + message);
        }
    }
    private void sendWhisper(String username, String message, WhisperPriority priority) {
        _whisperer.enqueueWhisper(username, message, priority);
    }
}

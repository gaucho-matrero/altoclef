package adris.altoclef.util;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Sometimes we want to trigger a "press" for one frame, or do other input forcing.
 * <p>
 * Dealing with keeping track of a press and timing each time you do this is annoying.
 * <p>
 * For some reason using baritone's "Forcestate" doesn't always work, perhaps that's my bad.
 * <p>
 * But this will alleviate all confusion.
 */
public class InputControls {

    private final Queue<Input> _toUnpress = new ArrayDeque<>();
    private final Set<Input> _waitForRelease = new HashSet<>(); // a click requires a release.


    public void tryPress(Input input) {
        // We just pressed, so let us release.
        if (_waitForRelease.contains(input)) {
            return;
        }
        input.getKeyBinding().setPressed(true);
        _toUnpress.add(input);
        _waitForRelease.add(input);
    }

    public void hold(Input input) {
        input.getKeyBinding().setPressed(true);
    }

    public void release(Input input) {
        input.getKeyBinding().setPressed(false);
    }

    public boolean isHeldDown(Input input) {
        return input.getKeyBinding().isPressed();
    }

    public void forceLook(float yaw, float pitch) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.setYaw(yaw);
            MinecraftClient.getInstance().player.setPitch(pitch);
        }
    }

    // Before the user calls input commands for the frame
    public void onTickPre() {
        while (!_toUnpress.isEmpty()) {
            _toUnpress.remove().getKeyBinding().setPressed(false);
        }
    }

    // After the user calls input commands for the frame
    public void onTickPost() {
        _waitForRelease.clear();
    }
}

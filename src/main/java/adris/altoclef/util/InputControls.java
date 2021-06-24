package adris.altoclef.util;

import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;

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

    private static KeyBinding inputToKeyBinding(Input input) {
        GameOptions o = MinecraftClient.getInstance().options;
        switch (input) {
            case MOVE_FORWARD:
                return o.keyForward;
            case MOVE_BACK:
                return o.keyBack;
            case MOVE_LEFT:
                return o.keyLeft;
            case MOVE_RIGHT:
                return o.keyRight;
            case CLICK_LEFT:
                return o.keyAttack;
            case CLICK_RIGHT:
                return o.keyUse;
            case JUMP:
                return o.keyJump;
            case SNEAK:
                return o.keySneak;
            case SPRINT:
                return o.keySprint;
            default:
                throw new IllegalArgumentException("Invalid key input/not accounted for: " + input);
        }
    }

    public void tryPress(Input input) {
        // We just pressed, so let us release.
        if (_waitForRelease.contains(input)) {
            return;
        }
        inputToKeyBinding(input).setPressed(true);
        _toUnpress.add(input);
        _waitForRelease.add(input);
    }

    public void hold(Input input) {
        inputToKeyBinding(input).setPressed(true);
    }

    public void release(Input input) {
        inputToKeyBinding(input).setPressed(false);
    }

    public boolean isHeldDown(Input input) {
        return inputToKeyBinding(input).isPressed();
    }

    public void forceLook(float yaw, float pitch) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.yaw = yaw;
            MinecraftClient.getInstance().player.pitch = pitch;
        }
    }

    // Before the user calls input commands for the frame
    public void onTickPre() {
        while (!_toUnpress.isEmpty()) {
            inputToKeyBinding(_toUnpress.remove()).setPressed(false);
        }
    }

    // After the user calls input commands for the frame
    public void onTickPost() {
        _waitForRelease.clear();
    }
}

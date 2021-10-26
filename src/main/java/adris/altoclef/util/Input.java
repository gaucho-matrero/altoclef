package adris.altoclef.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

/**
 * Basically baritone's input, but with custom/extra features.
 */
public enum Input {
    MOVE_FORWARD,
    MOVE_BACK,
    MOVE_LEFT,
    MOVE_RIGHT,
    CLICK_LEFT,
    CLICK_RIGHT,
    JUMP,
    SNEAK,
    SPRINT,
    SWAP_HANDS;

    public KeyBinding getKeyBinding() {
        GameOptions o = MinecraftClient.getInstance().options;
        return switch (this) {
            case MOVE_FORWARD -> o.keyForward;
            case MOVE_BACK -> o.keyBack;
            case MOVE_LEFT -> o.keyLeft;
            case MOVE_RIGHT -> o.keyRight;
            case CLICK_LEFT -> o.keyAttack;
            case CLICK_RIGHT -> o.keyUse;
            case JUMP -> o.keyJump;
            case SNEAK -> o.keySneak;
            case SPRINT -> o.keySprint;
            case SWAP_HANDS -> o.keySwapHands;
            default -> throw new IllegalArgumentException("Invalid key input/not accounted for: " + this);
        };
    }

    public static Input fromBaritone(baritone.api.utils.input.Input input) {
        switch (input) {
            case MOVE_FORWARD:
                return MOVE_FORWARD;
            case MOVE_BACK:
                return MOVE_BACK;
            case MOVE_LEFT:
                return MOVE_LEFT;
            case MOVE_RIGHT:
                return MOVE_RIGHT;
            case CLICK_LEFT:
                return CLICK_LEFT;
            case CLICK_RIGHT:
                return CLICK_RIGHT;
            case JUMP:
                return JUMP;
            case SNEAK:
                return SNEAK;
            case SPRINT:
                return SPRINT;
            default:
                throw new IllegalArgumentException("Invalid key input/not accounted for: " + input);
        }
    }
}

package adris.altoclef.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Input {

    public static boolean isKeyPressed(int code) {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), code);
    }
}

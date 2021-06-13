package adris.altoclef.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class Input {

    public static boolean isKeyPressed(int code) {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), code);
    }
}

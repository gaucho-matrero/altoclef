package adris.altoclef.util;


import net.minecraft.client.MinecraftClient;


public final class InputUtil {

    private InputUtil() {
    }

    public static boolean isKeyPressed(int code) {
        return net.minecraft.client.util.InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), code);
    }
}

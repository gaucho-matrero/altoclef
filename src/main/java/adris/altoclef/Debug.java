package adris.altoclef;

import net.fabricmc.loader.launch.common.FabricMixinBootstrap;
import net.fabricmc.loom.util.FabricApiExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class Debug {

    public static void logInternal(String message) {
        System.out.println("ALTO CLEF: " + message);
    }

    public static void logMessage(String message) {
        logInternal(message);
        if (MinecraftClient.getInstance().player != null) {
            String msg = "\u00A72\u00A7l\u00A7o[Alto Clef] \u00A7r" + message.toString();
            MinecraftClient.getInstance().player.sendMessage(Text.of(msg), false);
            //MinecraftClient.getInstance().player.sendChatMessage(msg);
        }
    }

    public static void logWarning(String message) {
        logInternal("WARNING: " + message);
        if (MinecraftClient.getInstance().player != null) {
            String msg = "\u00A72\u00A7l\u00A7o[Alto Clef] \u00A7c" + message.toString() + "\u00A7r";
            MinecraftClient.getInstance().player.sendMessage(Text.of(msg), false);
            //MinecraftClient.getInstance().player.sendChatMessage(msg);
        }
    }

    public static void logError(String message) {
        StringBuilder stacktrace = new StringBuilder();
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            stacktrace.append(ste.toString());
        }
        System.err.println(message);
        System.err.println("at:");
        System.err.println(stacktrace);
        if (MinecraftClient.getInstance().player != null) {
            String msg = "\u00A72\u00A7l\u00A7c[Alto Clef ERROR]" + message.toString() + "\nat:\n" + stacktrace + "\u00A7r";
            MinecraftClient.getInstance().player.sendMessage(Text.of(msg), false);
            //MinecraftClient.getInstance().player.sendChatMessage(msg);
        }
    }
}

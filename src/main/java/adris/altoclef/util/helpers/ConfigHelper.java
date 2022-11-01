package adris.altoclef.util.helpers;

import adris.altoclef.Debug;
import adris.altoclef.util.serialization.*;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Helps load settings/configuration files
 */
public class ConfigHelper {

    private static final String ALTO_FOLDER = "altoclef";
    // For reloading
    private static final HashMap<String, Runnable> _loadedConfigs = new HashMap<>();

    private static File getConfigFile(String path) {
        return Paths.get(ALTO_FOLDER, path).toFile();
    }

    public static void reloadAllConfigs() {
        for (Runnable reload : _loadedConfigs.values()) {
            reload.run();
        }
    }

    private static <T> T getConfig(String path, Supplier<T> getDefault, Class<T> classToLoad) {
        T result = getDefault.get();
        File loadFrom = getConfigFile(path);
        if (!loadFrom.exists()) {
            saveConfig(path, result);
            return result;
        }

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Vec3d.class, new Vec3dDeserializer());
        module.addDeserializer(ChunkPos.class, new ChunkPosDeserializer());
        module.addDeserializer(BlockPos.class, new BlockPosDeserializer());
        mapper.registerModule(module);

        boolean failed = false;
        try {
            result = mapper.readValue(loadFrom, classToLoad);
        } catch (JsonMappingException ex) {
            Debug.logError("Failed to parse Config file of type " + classToLoad.getSimpleName() + "at " + path + ". JSON Error Message: " + ex.getMessage() + ".\n JSON Error STACK TRACE:\n\n");
            ex.printStackTrace();
            if (result instanceof IFailableConfigFile failable)
                failable.failedToLoad();
            failed = true;
        } catch (IOException e) {
            Debug.logError("Failed to read Config at " + path + ".");
            e.printStackTrace();
            if (result instanceof IFailableConfigFile failable)
                failable.failedToLoad();
            failed = true;
        }

        // Save over to include NEW settings
        // but only if a load was successful. Don't want to override user settings!
        if (!failed) {
            saveConfig(path, result);
        }

        return result;
    }

    public static <T> void loadConfig(String path, Supplier<T> getDefault, Class<T> classToLoad, Consumer<T> onReload) {
        T result = getConfig(path, getDefault, classToLoad);
        _loadedConfigs.put(path, () -> onReload.accept(getConfig(path, getDefault, classToLoad)));
        onReload.accept(result);
    }

    public static <T> void saveConfig(String path, T config) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Vec3d.class, new Vec3dSerializer());
        module.addSerializer(BlockPos.class, new BlockPosSerializer());
        module.addSerializer(ChunkPos.class, new ChunkPosSerializer());
        mapper.registerModule(module);

        File toSave = getConfigFile(path);
        if (!toSave.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            toSave.getParentFile().mkdirs();
        }

        try {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Pretty print and indent arrays too.
            DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
            prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

            mapper.writer(prettyPrinter).writeValue(toSave, config);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static <T extends IListConfigFile> T getListConfig(String path, Supplier<T> getDefault) {
        T result = getDefault.get();

        result.onLoadStart();

        File loadFrom = getConfigFile(path);
        if (!loadFrom.exists()) {
            // Empty
            Debug.logInternal("Lists file not found at " + path);
            return result;
        }

        try {
            FileInputStream fis = new FileInputStream(loadFrom);
            Scanner sc = new Scanner(fis);    //file to be scanned
            //returns true if there is another line to read
            while (sc.hasNextLine()) {
                String line = trimComment(sc.nextLine()).trim();
                if (line.length() == 0) continue;
                result.addLine(line);
            }
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    public static <T extends IListConfigFile> void loadListConfig(String path, Supplier<T> getDefault, Consumer<T> onReload) {
        T result = getListConfig(path, getDefault);
        _loadedConfigs.put(path, () -> onReload.accept(getListConfig(path, getDefault)));
        onReload.accept(result);
    }

    private static String trimComment(String line) {
        int pound = line.indexOf('#');
        if (pound == -1) {
            return line;
        }
        return line.substring(0, pound);
    }

    public static void ensureCommentedListFileExists(String path, String startingComment) {
        File loadFrom = getConfigFile(path);
        if (loadFrom.exists()) {
            // Already exists, don't make a new one.
            return;
        }
        StringBuilder result = new StringBuilder();
        for (String line : startingComment.split("\\r?\\n")) {
            if (line.length() != 0) {
                result.append("# ").append(line).append("\n");
            }
        }
        try {
            Files.write(loadFrom.toPath(), result.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

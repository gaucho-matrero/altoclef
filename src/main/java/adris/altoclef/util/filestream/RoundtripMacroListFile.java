package adris.altoclef.util.filestream;

import adris.altoclef.Debug;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RoundtripMacroListFile {
    private static final Map<String, List<String>> rawMap = new HashMap<>();
    private static final String ROUNDTRIP_MACROS_PATH = "altoclef_roundtrip_macros.txt";
    private static boolean loaded = false;

    public static RoundtripMacroListFile load(String path) {
        RoundtripMacroListFile result = new RoundtripMacroListFile();

        File loadFrom = new File(path);
        if (!loadFrom.exists()) {
            // Empty
            Debug.logInternal("Macro lists file not found at " + path);
            return new RoundtripMacroListFile();
        }

        try {
            FileInputStream fis = new FileInputStream(loadFrom);
            Scanner sc = new Scanner(fis);    //file to be scanned
            //returns true if there is another line to read
            String key = null;
            while (sc.hasNextLine()) {
                String line = trimComment(sc.nextLine()).trim();
                if (line.length() == 0) continue;

                //result.rawSet.add(line);

                if (line.contains(":")) {
                    key = line.replace(":", "");
                    rawMap.put(key, new ArrayList<>());
                    continue;
                } else if (key == null){
                    Debug.logError("An fatal error occurred in file handling (id 1)");
                    return null;
                } else {
                    rawMap.get(key).add(line);
                }
            }
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        loaded = true;
        return result;
    }

    public static void append(final Map<String,Queue<String>> macro) {
        StringBuilder result = new StringBuilder();

        macro.forEach((k, q) -> {
            result.append(k + ":\n");
            while (!q.isEmpty()) {
                result.append(q.poll() + "\n");
            }
        });

        try {
            OutputStream os = new FileOutputStream(ROUNDTRIP_MACROS_PATH, true);
            os.write(result.toString().getBytes(), 0, result.toString().length());
            os.close();
            //Files.write(Paths.get(path), result.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void append(final Map<String,Queue<String>> macro, boolean reload) {
        append(macro);

        if (reload) {
            loaded = false;
            rawMap.clear();
            get();
        }
    }

    private static String trimComment(String line) {
        int pound = line.indexOf('#');
        if (pound == -1) {
            return line;
        }
        return line.substring(0, pound);
    }

    public static void ensureExists(String path, String startingComment) {
        File loadFrom = new File(path);
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
            Files.write(Paths.get(path), result.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final boolean isLoaded() {
        return loaded;
    }

    public static boolean isEmpty() {
        if (!isLoaded()) {
            load(ROUNDTRIP_MACROS_PATH);
        }

        return rawMap.isEmpty();
    }

    public static Map<String, List<String>> get() {
        if (!isLoaded()) {
            ensureExists(ROUNDTRIP_MACROS_PATH, "Add roundtrip macros here.");
            load(ROUNDTRIP_MACROS_PATH);
        }

        return rawMap;
    }

    public static final void overrideWithVirtual() {
        StringBuilder result = new StringBuilder();

        rawMap.forEach((k, q) -> {
            result.append(k + ":\n");
            q.forEach(e -> result.append(e + "\n"));
        });

        try {
            Files.write(Paths.get(ROUNDTRIP_MACROS_PATH), result.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


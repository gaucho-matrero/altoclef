package adris.altoclef.util.filestream;

import adris.altoclef.Debug;
import adris.altoclef.util.CubeBounds;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AvoidanceFile {
    private static final List<Predicate<BlockPos>> list = new ArrayList<>();
    private static final String AVOIDANCE_PATH = "avoidance.txt";
    private static boolean loaded = false;

    public static boolean load(String path) {
        //AvoidanceFile result = new AvoidanceFile();

        File loadFrom = new File(path);
        if (!loadFrom.exists()) {
            Debug.logInternal("Avoidance list file not found at " + path);
            //return new AvoidanceFile();
            return false;
        }

        try {
            FileInputStream fis = new FileInputStream(loadFrom);
            Scanner sc = new Scanner(fis);
            while (sc.hasNextLine()) {
                String line = trimComment(sc.nextLine()).trim();
                if (line.length() == 0) continue;

                if (line.contains(":")) {
                    final String[] regionStrArr = line.split(":");

                    final String[] lowPointStrArr = regionStrArr[0].split(",");
                    final String[] highPointStrArr = regionStrArr[1].split(",");

                    final List<Integer> lowPointIntList = Arrays.stream(lowPointStrArr)
                            .map(e -> Integer.parseInt(e)).collect(Collectors.toList());

                    final List<Integer> highPointIntList = Arrays.stream(highPointStrArr)
                            .map(e -> Integer.parseInt(e)).collect(Collectors.toList());

                    final Predicate<BlockPos> pred = (BlockPos e) ->
                        lowPointIntList.get(0) <= e.getX() &&
                        lowPointIntList.get(1) <= e.getY() &&
                        lowPointIntList.get(2) <= e.getZ() &&
                        e.getX() <= highPointIntList.get(0) &&
                        e.getY() <= highPointIntList.get(1) &&
                        e.getZ() <= highPointIntList.get(2);

                    list.add(pred);
                }

            }
            sc.close();
        } catch (IOException e) {
            throw new IllegalArgumentException("Wrong format in" + AVOIDANCE_PATH);
        }

        loaded = true;
        return true;
    }

    //TODO: Make "appendMany" for optimization if needed.
    /*public static void append(final Vec3i low, final Vec3i high) {
        StringBuilder result = new StringBuilder();

        result.append(low.getX() + "," + low.getY() + "," + low.getZ() + ":" + high.getX() + "," + high.getY() + "," + high.getZ());

        try {
            OutputStream os = new FileOutputStream(AVOIDANCE_PATH, true);
            os.write(result.toString().getBytes(), 0, result.toString().length());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //loaded = false;
        //list.clear();
        //get();
    }*/

    //TODO: This method stores every line in memory. Better remember via temp file instead.
    private static void removeLine(String lineContent, final File file) throws IOException {
        List<String> out = Files.lines(file.toPath())
                .filter(line -> !line.contains(lineContent))
                .collect(Collectors.toList());
        Files.write(file.toPath(), out, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    //TODO: uh, duplicate code... Sorry ^^'.
    public static boolean remove(final CubeBounds bounds) {
        final BlockPos low = bounds.getLow();
        final BlockPos high = bounds.getHigh();

        File loadFrom = new File(AVOIDANCE_PATH);
        if (!loadFrom.exists()) {
            Debug.logInternal("Avoidance list file not found at " + AVOIDANCE_PATH);
            return false;
        }

        try {
            FileInputStream fis = new FileInputStream(loadFrom);
            Scanner sc = new Scanner(fis);
            while (sc.hasNextLine()) {
                final String line = trimComment(sc.nextLine()).trim();
                if (line.length() == 0) continue;

                if (line.contains(":")) {
                    final String subline = line.replace("\n", "");
                    final String[] regionStrArr = subline.split(":");

                    final String[] lowPointStrArr = regionStrArr[0].split(",");
                    final String[] highPointStrArr = regionStrArr[1].split(",");

                    final List<Integer> lowPointIntList = Arrays.stream(lowPointStrArr)
                            .map(e -> Integer.parseInt(e)).collect(Collectors.toList());

                    final List<Integer> highPointIntList = Arrays.stream(highPointStrArr)
                            .map(e -> Integer.parseInt(e)).collect(Collectors.toList());

                    if (lowPointIntList.get(0) == low.getX() &&
                        lowPointIntList.get(1) == low.getY() &&
                        lowPointIntList.get(2) == low.getZ() &&
                        high.getX() == highPointIntList.get(0) &&
                        high.getY() == highPointIntList.get(1) &&
                        high.getZ() == highPointIntList.get(2)) {
                        sc.close();

                        removeLine(line, loadFrom);
                        break;
                    }
                }

            }
            sc.close();
        } catch (IOException e) {
            throw new IllegalArgumentException("Wrong format in" + AVOIDANCE_PATH);
        }

        return load(AVOIDANCE_PATH);
    }

    public static void append(final CubeBounds bounds) {
        //append(bounds.getLow(), bounds.getHigh());
        remove(bounds);
        StringBuilder result = new StringBuilder();
        final BlockPos low = bounds.getLow();
        final BlockPos high = bounds.getHigh();

        result.append(low.getX() + "," + low.getY() + "," + low.getZ() + ":" + high.getX() + "," + high.getY() + "," + high.getZ() + "\n");

        try {
            OutputStream os = new FileOutputStream(AVOIDANCE_PATH, true);
            os.write(result.toString().getBytes(), 0, result.toString().length());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        list.add(bounds.getPredicate());
    }

    /*
    public static boolean remove(final Predicate<BlockPos> originalRef) {
        if (list.contains(originalRef)) {
            list.remove(originalRef);
            return true;
        }

        return true;
    }*/

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
            load(AVOIDANCE_PATH);
        }

        return list.isEmpty();
    }

    public static List<Predicate<BlockPos>> get() {
        if (!isLoaded()) {
            ensureExists(AVOIDANCE_PATH, "Add roundtrip macros here.\n");
            load(AVOIDANCE_PATH);
        }

        return list;
    }
}
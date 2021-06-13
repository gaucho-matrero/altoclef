package adris.altoclef.butler;

import adris.altoclef.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;

public class UserListFile {

    private final HashSet<String> _users = new HashSet<>();

    public static UserListFile load(String path) {
        UserListFile result = new UserListFile();

        File loadFrom = new File(path);
        if (!loadFrom.exists()) {
            // Empty
            Debug.logInternal("User lists file not found at " + path);
            return new UserListFile();
        }

        try {
            FileInputStream fis = new FileInputStream(loadFrom);
            Scanner sc = new Scanner(fis);    //file to be scanned
            //returns true if there is another line to read
            while (sc.hasNextLine()) {
                String line = trimComment(sc.nextLine()).trim();
                if (line.length() == 0) continue;
                result._users.add(line);
            }
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
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

    public boolean containsUser(String username) {
        return _users.contains(username);
    }
}

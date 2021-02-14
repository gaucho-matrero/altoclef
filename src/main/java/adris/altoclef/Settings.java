package adris.altoclef;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Settings {

    public static final String SETTINGS_PATH = "altoclef_settings.json";

    /**
     * Whenever we're moving, speed up our client by a multiple of this number.
     * Useful when traveling long distances, and only is enabled when we're moving and not mining.
     *
     * Set to 1 for this to have no effect.
     */
    private float speedHack = 1.0f;


    /**
     * Some larger special tasks will perform extra preparation work to ensure your player
     * has the most efficient tools for the job at hand.
     *
     * For instance, the Place Signs task might first acquire a diamond axe to ease the collection of wood.
     * if "sharpenAxe" is disabled, it won't grab the axe and will collect wood with the best tool it currently has.
     */
    private boolean sharpenAxe = true;

    /**
     * Uses killaura to move mobs away and performs survival moves including:
     * - Running away from hostile mobs when your health is low
     * - Run away from creepers about to blow up
     * - Avoid wither skeletons and other really dangerous mobs
     * - Attempting to dodge arrows and other projectiles
     */
    private boolean mobDefense = true;

    /**
     * Only applies if mobDefense is on.
     *
     * If enabled, will attempt to dodge all incoming projectiles
     */
    private boolean dodgeProjectiles = true;

    /**
     * If true, eat when we're hungry or in danger.
     */
    private boolean autoEat = true;

    /**
     * If true, will automatically reconnect to the last open server if you get disconnected.
     *
     * If disabled, the bot will stop running when you disconnect from a server.
     */
    private boolean autoReconnect = true;

    /**
     * If true, will automatically respawn instantly if you die.
     *
     * If disabled, the bot will stop running when you die.
     */
    private boolean autoRespawn = true;

    // Internal tracking of whether we're dirty or not.
    private transient boolean _dirty;

    public void setSpeedHack(float value) {
        speedHack = value; markDirty();
    }
    public float getSpeedHack() {
        return speedHack;
    }

    public boolean isSharpenAxe() {
        return sharpenAxe;
    }
    public void setSharpenAxe(boolean sharpenAxe) {
        this.sharpenAxe = sharpenAxe; markDirty();
    }

    public boolean isMobDefense() {
        return mobDefense;
    }
    public void setMobDefense(boolean mobDefense) {
        this.mobDefense = mobDefense; markDirty();
    }

    public boolean isDodgeProjectiles() {
        return dodgeProjectiles;
    }
    public void setDodgeProjectiles(boolean dodgeProjectiles) {
        this.dodgeProjectiles = dodgeProjectiles; markDirty();
    }

    public boolean isAutoEat() {
        return autoEat;
    }
    public void setAutoEat(boolean autoEat) {
        this.autoEat = autoEat;
        markDirty();
    }
    public boolean isAutoReconnect() {
        return autoReconnect;
    }
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public boolean isAutoRespawn() {
        return autoRespawn;
    }
    public void setAutoRespawn(boolean autoRespawn) {
        this.autoRespawn = autoRespawn;
    }



    // Dirty managing
    private void markDirty() {
        _dirty = true;
    }
    public boolean isDirty() {
        return _dirty;
    }

    public void save() {
        if (!_dirty) return;
        save(this);
        _dirty = false;
    }

    public static Settings load() {

        File loadFrom = new File(SETTINGS_PATH);
        if (!loadFrom.exists()) {
            Settings result = new Settings();
            result.markDirty();
            result.save();
            return result;
        }

        String data;
        try {
            data = new String(Files.readAllBytes(Paths.get(SETTINGS_PATH)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Gson gson = new Gson();

        return gson.fromJson(data, Settings.class);
    }

    private static void save(Settings settings) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String userJson = gson.toJson(settings);

        try {
            Files.write(Paths.get(SETTINGS_PATH), userJson.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

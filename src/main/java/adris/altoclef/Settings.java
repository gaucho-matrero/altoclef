package adris.altoclef;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

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

    /**
     * If true, will use blacklist for rejecting users from using your player as a butler
     */
    private boolean useButlerBlacklist = true;
    /**
     * If true, will use whitelist to only accept users from said whitelist.
     */
    private boolean useButlerWhitelist = true;

    /**
     * If we need to throw away something, throw away these items first.
     */
    private int[] throwawayItems = new int[] {
            // Overworld junk
            Item.getRawId(Items.DIORITE),
            Item.getRawId(Items.ANDESITE),
            Item.getRawId(Items.GRANITE),
            Item.getRawId(Items.COBBLESTONE),
            Item.getRawId(Items.DIRT),
            Item.getRawId(Items.GRAVEL),
            // Nether junk, to be fair it's mostly tuned for the "beat game" task
            Item.getRawId(Items.NETHERRACK),
            Item.getRawId(Items.MAGMA_BLOCK),
            Item.getRawId(Items.SOUL_SOIL),
            Item.getRawId(Items.SOUL_SAND),
            Item.getRawId(Items.NETHER_BRICKS),
            Item.getRawId(Items.NETHER_BRICK)
    };

    /**
     * If we need to throw away something but we don't have any "throwaway Items",
     * throw away any unimportant item that's not currently needed in our task chain.
     *
     * Careful with this! If true, any item not in "importantItems" is liable to be thrown away.
     */
    private boolean throwAwayUnusedItems = false;

    /**
     * We will NEVER throw away these items.
     * Even if "throwAwayUnusedItems" is true and one of these items is not used in a task.
     */
    private int[] importantItems = new int[] {
            Item.getRawId(Items.ENCHANTED_GOLDEN_APPLE),
            Item.getRawId(Items.ENDER_EYE),
            // Don't throw away shulker boxes that would be pretty bad lol
            Item.getRawId(Items.SHULKER_BOX),
            Item.getRawId(Items.BLACK_SHULKER_BOX),
            Item.getRawId(Items.BLUE_SHULKER_BOX),
            Item.getRawId(Items.BROWN_SHULKER_BOX),
            Item.getRawId(Items.CYAN_SHULKER_BOX),
            Item.getRawId(Items.GRAY_SHULKER_BOX),
            Item.getRawId(Items.GREEN_SHULKER_BOX),
            Item.getRawId(Items.LIGHT_BLUE_SHULKER_BOX),
            Item.getRawId(Items.LIGHT_GRAY_SHULKER_BOX),
            Item.getRawId(Items.LIME_SHULKER_BOX),
            Item.getRawId(Items.MAGENTA_SHULKER_BOX),
            Item.getRawId(Items.ORANGE_SHULKER_BOX),
            Item.getRawId(Items.PINK_SHULKER_BOX),
            Item.getRawId(Items.PURPLE_SHULKER_BOX),
            Item.getRawId(Items.RED_SHULKER_BOX),
            Item.getRawId(Items.WHITE_SHULKER_BOX),
            Item.getRawId(Items.YELLOW_SHULKER_BOX)
    };

    // Internal tracking of whether we're dirty or not.
    private transient boolean _dirty;

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

        Settings result = gson.fromJson(data, Settings.class);
        result.save();
        return result;
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

    public boolean isUseButlerBlacklist() {
        return useButlerBlacklist;
    }
    public void setUseButlerBlacklist(boolean useButlerBlacklist) {
        this.useButlerBlacklist = useButlerBlacklist;
    }

    public boolean isUseButlerWhitelist() {
        return useButlerWhitelist;
    }
    public void setUseButlerWhitelist(boolean useButlerWhitelist) {
        this.useButlerWhitelist = useButlerWhitelist;
    }

    public boolean isThrowaway(Item item) {
        return idArrayContainsItem(item, throwawayItems);
    }
    public boolean isImportant(Item item) {
        return idArrayContainsItem(item, importantItems);
    }
    public boolean shouldThrowawayUnusedItems() {
        return this.throwAwayUnusedItems;
    }
    public Item[] getThrowawayItems() {
        Item[] result = new Item[throwawayItems.length];
        for (int i = 0; i < throwawayItems.length; ++i) {
            result[i] = Item.byRawId(throwawayItems[i]);
        }
        return result;
    }

    private static boolean idArrayContainsItem(Item item, int[] ids) {
        int id = Item.getRawId(item);
        for (int check : ids) {
            if (check == id) return true;
        }
        return false;
    }
}

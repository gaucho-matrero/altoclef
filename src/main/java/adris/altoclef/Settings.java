package adris.altoclef;

import adris.altoclef.tasks.DefaultGoToDimensionTask;
import adris.altoclef.util.KillAura;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.serialization.BlockPosDeserializer;
import adris.altoclef.util.serialization.BlockPosSerializer;
import adris.altoclef.util.serialization.ItemDeserializer;
import adris.altoclef.util.serialization.ItemSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Settings {

    public static final String SETTINGS_PATH = "altoclef_settings.json";

    // Internal only.
    // If settings failed to load, this will be set to warn the user.
    @JsonIgnore
    private transient boolean _failedToLoad = false;

    /**
     * If true, text will appear on the top left showing the current
     * task chain.
     */
    private boolean showTaskChains = true;

    /**
     * Whenever we're moving, speed up our client by a multiple of this number.
     * Useful when traveling long distances, and only is enabled when we're moving and not mining.
     * <p>
     * Set to 1 for this to have no effect.
     */
    private float speedHack = 1.0f;

    /**
     * The delay between moving items for crafting/furnace/any kind of inventory movement.
     */
    private float containerItemMoveDelay = 0.08f;

    /**
     * If a dropped resource item is further than this from the player, don't pick it up.
     * <p>
     * -1 (or less than 0) to disable.
     */
    private float resourcePickupDropRange = -1;


    /**
     * minumum amount of food to have in the inventory.
     * if we have less food than this value the bot will go pickup some more.
     */
    private int minimumFoodAllowed = 0;


    /**
     * amount of food to collect when the food in inventory
     * is lower than the value of foodUnitsThreshold
     */
    private int foodUnitsToCollect = 0;


    /**
     * Chests are cached for their contents.
     * <p>
     * If the bot is collecting a resource and finds a chest within this range,
     * it will grab the resource from the chest.
     * <p>
     * Set this to 0 to disable chest pickups.
     * <p>
     * Don't set this too high, as the bot will prioritize chests even if the resource
     * is easily accesible now.
     */
    private float resourceChestLocateRange = 500;

    /**
     * Some block resources are obtained through non-mining means
     * (like a crafting table or stone block, which can be crafted or smelted).
     * <p>
     * However, if the block resource is found within this range it will be mined first.
     * <p>
     * Set this to 0 to disable this feature
     * (keep in mind, this will not affect blocks like "dirt" and "cobblestone"
     * that can only be obtained through mining)
     * <p>
     * Set this to -1 to ALWAYS mine a block if it's catalogued.
     * This is not recommended. For example, if the bot happens to track a
     * crafting table 10000 blocks away, and it then tries obtaining one
     * it will travel 10000 blocks to mine that table, even if it finds
     * itself in a forest where the wood is abundant.
     */
    private float resourceMineRange = 100;

    /**
     * When going to the nearest chest to store items, the bot may normally
     * dig up dungeons constantly. If this is set to true, the bot will
     * search around each chest to make sure it's not in a dungeon.
     */
    private boolean avoidSearchingDungeonChests = true;


    /**
     * Some larger special tasks will perform extra preparation work to ensure your player
     * has the most efficient tools for the job at hand.
     *
     * For instance, the Place Signs task might first acquire a diamond axe to ease the collection of wood.
     * if "sharpenAxe" is disabled, it won't grab the axe and will collect wood with the best tool it currently has.
     */
    //private boolean sharpenAxe = true;

    /**
     * Before grabbing ANYTHING, get a pickaxe.
     * <p>
     * Will help with navigation as sometimes dropped items will be underground,
     * but this behaviour only makes sense in regular minecraft worlds.
     */
    private boolean collectPickaxeFirst = true;

    /**
     * If set to true, crops broken when collecting food will be replanted.
     */
    private boolean replantCrops = true;

    /**
     * Uses killaura to move mobs away and performs survival moves including:
     * - Running away from hostile mobs when your health is low
     * - Run away from creepers about to blow up
     * - Avoid wither skeletons and other really dangerous mobs
     * - Attempting to dodge arrows and other projectiles
     */
    private boolean mobDefense = true;

    /**
     * Defines how killaura behaves when "mobDefense" is set to true.
     * <p>
     * <p>
     * Strategies:
     * <p>
     * FASTEST: All hostiles are attacked at every possible moment, every frame.
     * SMART: Closest hostile is attacked at max every 0.2 seconds.
     * OFF: Off
     */
    private KillAura.Strategy forceFieldStrategy = KillAura.Strategy.FASTEST;

    /**
     * Only applies if mobDefense is on.
     * <p>
     * If enabled, will attempt to dodge all incoming projectiles
     */
    private boolean dodgeProjectiles = true;

    /**
     * Skeletons and large groups of mobs are a huge pain.
     * <p>
     * With this set to true, the bot may either
     * kill or run away from mobs that stay too close for too long.
     */
    private boolean killOrAvoidAnnoyingHostiles = true;

    /**
     * If enabled, the bot will avoid going underwater if baritone
     * isn't giving the bot movement instructions.
     * <p>
     * Baritone doesn't know how to move underwater so this should cause
     * no problems, but disable it if you want the bot to be able to sink.
     */
    private boolean avoidDrowning = true;

    /**
     * If true, eat when we're hungry or in danger.
     */
    private boolean autoEat = true;

    /**
     * If true, MLG/No Fall Bucket if we're knocked off course and falling.
     */
    private boolean autoMLGBucket = true;

    /**
     * If true, will automatically reconnect to the last open server if you get disconnected.
     * <p>
     * If disabled, the bot will stop running when you disconnect from a server.
     */
    private boolean autoReconnect = true;

    /**
     * If true, will automatically respawn instantly if you die.
     * <p>
     * If disabled, the bot will stop running when you die.
     */
    private boolean autoRespawn = true;

    /**
     * This setting lets you configure what the bot should do if it needs to go to the nether
     * but can't find a nether portal immediately.
     * <p>
     * Options:
     * BUILD_PORTAL_VANILLA: Builds a nether portal, either with obsidian or with a water bucket and lava pool.
     * GO_TO_HOME_BASE: Travel to the set home coordinates and assume there's a portal there.
     */
    private DefaultGoToDimensionTask.OVERWORLD_TO_NETHER_BEHAVIOUR overworldToNetherBehaviour = DefaultGoToDimensionTask.OVERWORLD_TO_NETHER_BEHAVIOUR.BUILD_PORTAL_VANILLA;

    /**
     * If true, will use blacklist for rejecting users from using your player as a butler
     */
    private boolean useButlerBlacklist = true;
    /**
     * If true, will use whitelist to only accept users from said whitelist.
     */
    private boolean useButlerWhitelist = true;

    /**
     * Servers have different messaging plugins that change the way messages are displayed.
     * Rather than attempt to implement all of them and introduce a big security risk,
     * you may define custom whisper formats that the butler will watch out for.
     * <p>
     * Within curly brackets are three special parts:
     * <p>
     * {from}: Who the message was sent from
     * {to}: Who the message was sent to, butler will ignore if this is not your username.
     * {message}: The message.
     * <p>
     * <p>
     * WARNING: The butler will only accept non-chat messages as commands, but don't make this too lenient,
     * else you may risk unauthorized control to the bot. Basically, make sure that only whispers can
     * create the following messages.
     */
    private String[] whisperFormats = new String[]{
            "{from} whispers to you: {message}",
            "{from} whispers: {message}",
            "\\[{from} -> {to}\\] {message}"
    };

    /**
     * If true, the bot will perform basic survival tasks when no commands are in progress
     * (eat food, force field mobs, etc.)
     * It will only perform survival tasks allowed by other parameters in the settings file.
     */
    private boolean idleWhenNotActive = false;

    /**
     * If we need to throw away something, throw away these items first.
     */
    @JsonSerialize(using = ItemSerializer.class)
    @JsonDeserialize(using = ItemDeserializer.class)
    private List<Item> throwawayItems = Arrays.asList(
            // Overworld junk
            Items.DIORITE,
            Items.ANDESITE,
            Items.GRANITE,
            Items.COBBLESTONE,
            Items.DIRT,
            Items.GRAVEL,
            // Nether junk, to be fair it's mostly tuned for the "beat game" task
            Items.NETHERRACK,
            Items.MAGMA_BLOCK,
            Items.SOUL_SOIL,
            Items.SOUL_SAND,
            Items.NETHER_BRICKS,
            Items.NETHER_BRICK
    );

    /**
     * If we need to throw away something but we don't have any "throwaway Items",
     * throw away any unimportant item that's not currently needed in our task chain.
     * <p>
     * Careful with this! If true, any item not in "importantItems" is liable to be thrown away.
     */
    private boolean throwAwayUnusedItems = false;

    /**
     * We will NEVER throw away these items.
     * Even if "throwAwayUnusedItems" is true and one of these items is not used in a task.
     */
    @JsonSerialize(using = ItemSerializer.class)
    @JsonDeserialize(using = ItemDeserializer.class)
    private List<Item> importantItems = Arrays.asList(
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.ENDER_EYE,
            // Don't throw away shulker boxes that would be pretty bad lol
            Items.SHULKER_BOX,
            Items.BLACK_SHULKER_BOX,
            Items.BLUE_SHULKER_BOX,
            Items.BROWN_SHULKER_BOX,
            Items.CYAN_SHULKER_BOX,
            Items.GRAY_SHULKER_BOX,
            Items.GREEN_SHULKER_BOX,
            Items.LIGHT_BLUE_SHULKER_BOX,
            Items.LIGHT_GRAY_SHULKER_BOX,
            Items.LIME_SHULKER_BOX,
            Items.MAGENTA_SHULKER_BOX,
            Items.ORANGE_SHULKER_BOX,
            Items.PINK_SHULKER_BOX,
            Items.PURPLE_SHULKER_BOX,
            Items.RED_SHULKER_BOX,
            Items.WHITE_SHULKER_BOX,
            Items.YELLOW_SHULKER_BOX
    );

    /**
     * Where "home base" is for the bot.
     * Some settings use this value, but by default
     * this value goes unused, so don't worry
     * about setting this unless you need it.
     */
    private BlockPos homeBasePosition = new BlockPos(0, 64, 0);

    /**
     * These areas will not be mined.
     * Used to prevent griefing
     * or to define a "spawn protection" zone so
     * the bot doesn't keep trying to break spawn protected
     * blocks.
     */
    private List<ProtectionRange> areasToProtect = Collections.emptyList();

    public static Settings load() {

        File loadFrom = new File(SETTINGS_PATH);
        if (!loadFrom.exists()) {
            Settings result = new Settings();
            result.save();
            return result;
        }

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(BlockPos.class, new BlockPosDeserializer());
        mapper.registerModule(module);

        Settings result = new Settings(); // Defaults
        try {
            result = mapper.readValue(Paths.get(SETTINGS_PATH).toFile(), Settings.class);
        } catch (JsonMappingException ex) {
            Debug.logError("Failed to read Settings at " + SETTINGS_PATH + ". JSON Error Message: " + ex.getMessage() + ".\n JSON Error STACK TRACE:\n\n");
            result._failedToLoad = true;
            ex.printStackTrace();
        } catch (IOException e) {
            Debug.logError("Failed to read Settings at " + SETTINGS_PATH + ". IOException.");
            result._failedToLoad = true;
            e.printStackTrace();
        }

        // Save over to include NEW settings
        // but only if a load was successful. Don't want to override user settings!
        if (!result.failedToLoad()) {
            result.save();
        }

        return result;
    }

    private static void save(Settings settings) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(BlockPos.class, new BlockPosSerializer());
        mapper.registerModule(module);

        try {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Pretty print and indent arrays too.
            DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
            prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

            mapper.writer(prettyPrinter).writeValue(Paths.get(SETTINGS_PATH).toFile(), settings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean idArrayContainsItem(Item item, int[] ids) {
        int id = Item.getRawId(item);
        for (int check : ids) {
            if (check == id) return true;
        }
        return false;
    }

    public void save() {
        save(this);
    }

    public boolean failedToLoad() {
        return _failedToLoad;
    }

    public boolean shouldShowTaskChain() {
        return showTaskChains;
    }

    public float getSpeedHack() {
        return speedHack;
    }

    public float getResourcePickupRange() {
        return resourcePickupDropRange;
    }

    public float getResourceChestLocateRange() {
        return resourceChestLocateRange;
    }

    public float getResourceMineRange() {
        return resourceMineRange;
    }

    public float getContainerItemMoveDelay() {
        return containerItemMoveDelay;
    }

    public int getFoodUnitsToCollect() {
        return foodUnitsToCollect;
    }

    public int getMinimumFoodAllowed() {
        return minimumFoodAllowed;
    }

    public boolean isMobDefense() {
        return mobDefense;
    }

    public boolean isDodgeProjectiles() {
        return dodgeProjectiles;
    }

    public boolean isAutoEat() {
        return autoEat;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public boolean isAutoRespawn() {
        return autoRespawn;
    }

    public boolean shouldReplantCrops() {
        return replantCrops;
    }

    public boolean isUseButlerBlacklist() {
        return useButlerBlacklist;
    }

    public boolean isUseButlerWhitelist() {
        return useButlerWhitelist;
    }

    public boolean shouldDealWithAnnoyingHostiles() {
        return killOrAvoidAnnoyingHostiles;
    }

    public KillAura.Strategy getForceFieldStrategy() {
        return forceFieldStrategy;
    }

    public boolean shouldIdleWhenNotActive() {
        return idleWhenNotActive;
    }

    public boolean shouldAutoMLGBucket() {
        return autoMLGBucket;
    }

    public boolean shouldCollectPickaxeFirst() {
        return collectPickaxeFirst;
    }

    public boolean shouldAvoidDrowning() {
        return avoidDrowning;
    }

    public boolean shouldAvoidSearchingForDungeonChests() {
        return avoidSearchingDungeonChests;
    }

    public boolean isThrowaway(Item item) {
        return throwawayItems.contains(item);
    }

    public boolean isImportant(Item item) {
        return importantItems.contains(item);
    }

    public boolean shouldThrowawayUnusedItems() {
        return this.throwAwayUnusedItems;
    }

    public Item[] getThrowawayItems(AltoClef mod) {
        List<Item> result = new ArrayList<>();
        for (Item throwawayItem : throwawayItems) {
            if (!mod.getBehaviour().isProtected(throwawayItem)) {
                result.add(throwawayItem);
            }
        }
        return Util.toArray(Item.class, result);
    }

    public String[] getWhisperFormats() {
        return whisperFormats;
    }

    public boolean isPositionExplicitelyProtected(BlockPos pos) {
        for (ProtectionRange protection : areasToProtect) {
            if (protection.includes(pos)) return true;
        }
        return false;
    }

    public DefaultGoToDimensionTask.OVERWORLD_TO_NETHER_BEHAVIOUR getOverworldToNetherBehaviour() {
        return overworldToNetherBehaviour;
    }

    public BlockPos getHomeBasePosition() {
        return homeBasePosition;
    }

    private static class ProtectionRange {
        public BlockPos start;
        public BlockPos end;

        public boolean includes(BlockPos pos) {
            return (start.getX() <= pos.getX() && pos.getX() <= end.getX() &&
                    start.getZ() <= pos.getZ() && pos.getZ() <= end.getZ() &&
                    start.getY() <= pos.getY() && pos.getY() <= end.getY());
        }

        public String toString() {
            return "[" + start.toShortString() + " -> " + end.toShortString() + "]";
        }
    }
}

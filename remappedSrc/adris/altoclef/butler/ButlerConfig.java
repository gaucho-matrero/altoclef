package adris.altoclef.butler;

import adris.altoclef.util.helpers.ConfigHelper;

public class ButlerConfig {

    private static ButlerConfig _instance = new ButlerConfig();
    static {
        ConfigHelper.loadConfig("configs/butler.json", ButlerConfig::new, ButlerConfig.class, newConfig -> _instance = newConfig);
    }
    public static ButlerConfig getInstance() {
        return _instance;
    }

    /**
     * If true, will use blacklist for rejecting users from using your player as a butler
     */
    public boolean useButlerBlacklist = true;
    /**
     * If true, will use whitelist to only accept users from said whitelist.
     */
    public boolean useButlerWhitelist = true;

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
    public String[] whisperFormats = new String[]{
            "{from} whispers to you: {message}",
            "{from} whispers: {message}",
            "\\[{from} -> {to}\\] {message}"
    };

    /**
     * If set to true, will print information about whispers that are parsed and those
     * that have failed parsing.
     *
     * Enable this if you need help setting up the whisper format.
     */
    public boolean whisperFormatDebug = false;

}

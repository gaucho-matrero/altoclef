package adris.altoclef.butler;


import adris.altoclef.AltoClef;


public class UserAuth {
    private static final String BLACKLIST_PATH = "altoclef_butler_blacklist.txt";
    private static final String WHITELIST_PATH = "altoclef_butler_whitelist.txt";
    private final AltoClef mod;
    private UserListFile blacklist;
    private UserListFile whitelist;
    
    public UserAuth(AltoClef mod) {
        this.mod = mod;
        
        UserListFile.ensureExists(BLACKLIST_PATH, "Add butler blacklisted players here.\n" +
                                                  "Make sure useButlerBlacklist is set to true in the settings file.\n" +
                                                  "Anything after a pound sign (#) will be ignored.");
        UserListFile.ensureExists(WHITELIST_PATH, "Add butler whitelisted players here.\n" +
                                                  "Make sure useButlerWhitelist is set to true in the settings file.\n" +
                                                  "Anything after a pound sign (#) will be ignored.");
        
        reloadLists();
    }
    
    public void reloadLists() {
        blacklist = UserListFile.load(BLACKLIST_PATH);
        whitelist = UserListFile.load(WHITELIST_PATH);
    }
    
    public boolean isUserAuthorized(String username) {
        
        // Blacklist gets first priority.
        if (mod.getModSettings().isUseButlerBlacklist() && blacklist.containsUser(username)) {
            return false;
        }
        if (mod.getModSettings().isUseButlerWhitelist()) {
            return whitelist.containsUser(username);
        }
        
        // By default accept everyone.
        return true;
    }
    
}

package com.dominicfeliton.chatpolls;

import com.dominicfeliton.chatpolls.configuration.ConfigurationHandler;
import com.dominicfeliton.chatpolls.util.GenericCommandSender;
import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.PlayerRecord;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatPolls extends JavaPlugin {

    public static final int bStatsID = 00000;

    public static final String messagesConfigVersion = "010524-1";

    public static ChatPolls instance;

    private BukkitAudiences adventure;

    private TextComponent pluginPrefix = Component.text().content("[").color(NamedTextColor.DARK_RED)
            .append(Component.text().content("ChP").color(NamedTextColor.BLUE).decoration(TextDecoration.BOLD, true))
            .append(Component.text().content("]").color(NamedTextColor.DARK_RED))
            .build();

    private ConfigurationHandler configurationManager;

    private int updateCheckerDelay = 86400;

    private int syncUserDataDelay = 7200;

    private boolean enableSounds = true;

    private String platformType;

    private ComparableVersion platformVersion;

    private CommonRefs commonRefs;

    private AdapterFactory adapterFactory;

    private CommonRefs refs;

    private GenericCommandSender cmdSender;

    private ChatPollsHelper helper;

    private Map<String, PlayerRecord> playerRecords = new ConcurrentHashMap<>();

    private volatile String globalState = "Starting";

    public @NotNull BukkitAudiences adventure() {
        if (adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return adventure;
    }

    private void initStableObjs() {
        instance = this;
        adapterFactory = new AdapterFactory();
        platformType = adapterFactory.getServerInfo().getKey();
        platformVersion = new ComparableVersion(adapterFactory.getServerInfo().getValue());

        if (platformType == null) {
            throw new IllegalStateException("Platform unsupported! Disabling...");
        }

        refs = adapterFactory.getCommonRefs();
        cmdSender = adapterFactory.getCommandSender();
        helper = adapterFactory.getCHPHelper();

        // Set config manager
        configurationManager = new ConfigurationHandler();
    }

    private void takedownStableObjs() {
        adapterFactory = null;
        platformType = null;
        platformVersion = null;

        refs = null;
        cmdSender = null;
        helper = null;

        configurationManager = null;
        instance = null;
    }

    @Override
    public void onEnable() {
        // Should run first
        initStableObjs();

        doStartupTasks();

        // We made it!
        getLogger().info(refs.getPlainMsg("chpEnabled",
                "&6" + getPluginVersion(),
                "&a"));
    }

    @Override
    public void onDisable() {
        doTakedownTasks();

        getLogger().info(refs.getPlainMsg("chpDisabled",
                "&6" + getPluginVersion(),
                "&a"));

        // Run last
        takedownStableObjs();
    }

    public void doStartupTasks() {
        // Assuming config manager object != null
        configurationManager.initMainConfig();
        configurationManager.initMessagesConfigs();

        configurationManager.loadMainSettings();
        //configurationManager.loadStorageSettings();

        globalState = "Enabled";
    }

    public void doTakedownTasks() {
        globalState = "Stopping";

        // TODO: Everything
    }

    // Setters
    public void addPlayerRecord(PlayerRecord i) {
        playerRecords.put(i.getUUID(), i);
    }

    // Getters
    public String getPluginVersion() {
        return this.getDescription().getVersion();
    }

    public String getPlatformType() {
        return platformType;
    }

    public ComparableVersion getPlatformVersion() {
        return platformVersion;
    }

    public Component getPluginPrefix() {
        return pluginPrefix;
    }

    public CommonRefs getCommonRefs() {
        return commonRefs;
    }

    public GenericCommandSender getCommandSender() {
        return cmdSender;
    }

    public ChatPollsHelper getHelper() {
        return helper;
    }

    public ConfigurationHandler getConfigManager() {
        return configurationManager;
    }

    public PlayerRecord getPlayerRecord(Player inPlayer, boolean createNewIfNotExisting) {
        return getPlayerRecord(inPlayer.getUniqueId().toString(), createNewIfNotExisting);
    }

    public PlayerRecord getPlayerRecord(String uuid, boolean createNewIfNotExisting) {
        PlayerRecord outRecord = playerRecords.get(uuid);
        if (outRecord != null) {
            return outRecord;
        }
        if (createNewIfNotExisting) {
            // Create + add new record
            PlayerRecord newRecord = new PlayerRecord("--------", uuid);
            addPlayerRecord(newRecord);
            return newRecord;
        }
        return null;
    }

    /**
     * Checks if a given player has a player record.
     *
     * @param in - A player
     * @return true if PlayerRecord, false otherwise
     */
    public boolean isPlayerRecord(Player in) {
        return isPlayerRecord(in.getUniqueId());
    }

    /**
     * Checks if a given name has a player record.
     *
     * @param in - A player UUID
     * @return true if PlayerRecord, false otherwise
     */
    public boolean isPlayerRecord(UUID in) {
        return isPlayerRecord(in.toString());
    }

    /**
     * Checks if a given name has a player record.
     *
     * @param in - A player UUID as a String
     * @return true if PlayerRecord, false otherwise
     */
    public boolean isPlayerRecord(String in) {
        return getPlayerRecord(in, false) != null;
    }
}
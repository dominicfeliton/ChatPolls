package com.dominicfeliton.chatpolls;

import com.dominicfeliton.chatpolls.commands.CHPLocalizeBukkit;
import com.dominicfeliton.chatpolls.configuration.ConfigurationHandler;
import com.dominicfeliton.chatpolls.runnables.UpdateChecker;
import com.dominicfeliton.chatpolls.util.*;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.dominicfeliton.chatpolls.ChatPollsHelper.SchedulerType.ASYNC;

public class ChatPolls extends JavaPlugin {

    public static final int bStatsID = 00000;

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

        // TODO: Move adventure init to ChatPollsHelper, spigot only
        this.adventure = BukkitAudiences.create(this);

        doStartupTasks();

        refs.debugMsg(platformType + " | " + platformVersion);

        // We made it!
        refs.log(refs.getPlainMsg("chpEnabled",
                "&6" + getPluginVersion(),
                "&a"));
    }

    @Override
    public void onDisable() {
        doTakedownTasks();

        // TODO: Move to Helper, spigot only
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }

        refs.log(refs.getPlainMsg("chpDisabled",
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

        // Check for updates
        GenericRunnable update = new GenericRunnable() {
            @Override
            protected void execute() {
                new UpdateChecker(refs, getPluginVersion(), new BukkitCommandSender());
            }
        };
        helper.runAsyncRepeating(true, 0, updateCheckerDelay * 20, update, ASYNC, null);

        globalState = "Enabled";
    }

    public void doTakedownTasks() {
        globalState = "Stopping";

        // TODO: Everything
    }

    /* Init all commands */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        /* Commands that run regardless of translator settings, but not during restarts */
        if (globalState.equals("Enabled")) {
            switch (command.getName()) {
                case "chp":
                    // ChP version
                    BukkitCommandSender s = new BukkitCommandSender(sender);
                    final TextComponent versionNotice = Component.text()
                            .content(refs.getPlainMsg("chpVersion", s)).color(NamedTextColor.RED)
                            .append((Component.text().content(" " + getPluginVersion())).color(NamedTextColor.LIGHT_PURPLE))
                            .append((Component.text().content(" (Made with love by ")).color(NamedTextColor.GOLD))
                            .append((Component.text().content("Dominic Feliton")).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                            .append((Component.text().content(")").resetStyle()).color(NamedTextColor.GOLD)).build();
                    refs.sendMsg(s, versionNotice, true);
                    //refs.playSound(CHP_VERSION, sender);
                    return true;
                case "chpl":
                    // Change localization
                    CHPLocalizeBukkit c = new CHPLocalizeBukkit(new BukkitCommandSender(sender), label, args, refs);
                    return c.processCommand();
            }
        }
        return true;
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
        return refs;
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
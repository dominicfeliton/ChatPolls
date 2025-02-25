package com.dominicfeliton.chatpolls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.dominicfeliton.chatpolls.commands.CHPLocalizeBukkit;
import com.dominicfeliton.chatpolls.commands.CHPPersonalBukkit;
import com.dominicfeliton.chatpolls.configuration.ConfigurationHandler;
import com.dominicfeliton.chatpolls.runnables.UpdateChecker;
import com.dominicfeliton.chatpolls.util.*;
import com.dominicfeliton.chatpolls.util.BukkitCommandSender;
import com.dominicfeliton.chatpolls.util.BukkitPollObject;
import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericCommandSender;
import com.dominicfeliton.chatpolls.util.GenericRunnable;
import com.dominicfeliton.chatpolls.util.PlayerRecord;
import com.dominicfeliton.chatpolls.util.PollObject;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.dominicfeliton.chatpolls.ChatPollsHelper.SchedulerType.ASYNC;
import static com.dominicfeliton.chatpolls.util.CommonRefs.SoundType.CHP_VERSION;

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

    /**
     * personalPolls is a mapping of player UUID => (pollId => PollObject).
     * This is an in-memory store for personal polls.
     */
    private final Map<UUID, Map<String, PollObject>> personalPolls = new ConcurrentHashMap<>();

    private volatile String globalState = "Starting";
    
    private ObjectMapper objectMapper;
    private static final String POLLS_SAVE_PATH = "plugins/ChatPolls/polls/";

    public @NotNull BukkitAudiences adventure() {
        if (adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return adventure;
    }

    private void initObjectMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // Force serializer/deserializer to use the same format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));

        objectMapper.registerModule(javaTimeModule);
    }

    private void setupAutoSave() {
        // Auto-save polls every 5 minutes
        GenericRunnable autoSave = new GenericRunnable() {
            @Override
            protected void execute() {
                savePolls();
                refs.debugMsg("Auto-saved polls");
            }
        };
        helper.runAsyncRepeating(true, 300 * 20, 300 * 20, autoSave, ASYNC, null);
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

    public synchronized void savePolls() {
        try {
            // Create directory if it doesn't exist
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(POLLS_SAVE_PATH));
            
            // Save each player's polls
            for (Map.Entry<UUID, Map<String, PollObject>> entry : personalPolls.entrySet()) {
                UUID playerId = entry.getKey();
                Map<String, PollObject> polls = entry.getValue();
                
                String playerFile = POLLS_SAVE_PATH + playerId.toString() + ".json";
                
                // Log the JSON content before writing
                String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(polls);
                refs.debugMsg("Writing JSON for player " + playerId + ":\n" + jsonContent);
                
                // Write the file
                objectMapper.writeValue(new java.io.File(playerFile), polls);
                
                // Log the file content after writing
                String writtenContent = new String(
                    java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(playerFile)),
                    java.nio.charset.StandardCharsets.UTF_8
                );
                refs.debugMsg("File content after write for player " + playerId + ":\n" + writtenContent);
            }
        } catch (Exception e) {
            refs.debugMsg("Error saving polls: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadPolls() {
        try {
            java.io.File pollsDir = new java.io.File(POLLS_SAVE_PATH);
            if (!pollsDir.exists()) return;

            java.io.File[] pollFiles = pollsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (pollFiles == null) return;

            refs.debugMsg("Loading polls...");
            refs.debugMsg("Found " + pollFiles.length + " poll files");

            for (java.io.File file : pollFiles) {
                refs.debugMsg("Loading poll file " + file.getName());

                try {
                    String playerId = file.getName().replace(".json", "");
                    UUID playerUUID = UUID.fromString(playerId);
                    
                    // Use TypeReference to handle the complex map type
                    Map<String, BukkitPollObject> polls = objectMapper.readValue(file, 
                        objectMapper.getTypeFactory().constructMapType(
                            Map.class, 
                            String.class, 
                            BukkitPollObject.class
                        )
                    );
                    
                    personalPolls.put(playerUUID, new ConcurrentHashMap<>(polls));
                } catch (Exception e) {
                    refs.debugMsg("Error loading poll file " + file.getName() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            refs.debugMsg("Error loading polls: " + e.getMessage());
        }
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

        initObjectMapper();
        doStartupTasks();
        loadPolls();
        setupAutoSave();

        refs.debugMsg(platformType + " | " + platformVersion);

        // We made it!
        refs.log(refs.getPlainMsg("chpEnabled",
                "&6" + getPluginVersion(),
                "&a"));
    }

    @Override
    public void onDisable() {
        refs.debugMsg("Saving polls before shutdown...");
        try {
            savePolls();
            refs.debugMsg("Successfully saved polls");
        } catch (Exception e) {
            refs.debugMsg("Failed to save polls: " + e.getMessage());
        }
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
            BukkitCommandSender s = new BukkitCommandSender(sender);
            switch (command.getName()) {
                case "chp":
                    // If no arguments, show version. Otherwise, handle as a poll command
                    if (args.length == 0) {
                        // ChP version
                        final TextComponent versionNotice = Component.text()
                                .content(refs.getPlainMsg("chpVersion", s)).color(NamedTextColor.RED)
                                .append((Component.text().content(" " + getPluginVersion())).color(NamedTextColor.LIGHT_PURPLE))
                                .append((Component.text().content(" (Made with love by ")).color(NamedTextColor.GOLD))
                                .append((Component.text().content("Dominic Feliton")).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                                .append((Component.text().content(" and with support from ")).color(NamedTextColor.GOLD))
                                .append((Component.text().content("Hanzen Shou")).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                                .append((Component.text().content(")").resetStyle()).color(NamedTextColor.GOLD)).build();
                        refs.sendMsg(s, versionNotice, true);
                        refs.playSound(CHP_VERSION, new BukkitCommandSender(sender));
                        return true;
                    } else {
                        // Handle as a poll command
                        if (!sender.hasPermission("chatpolls.chp.polls")) {
                            refs.sendMsg("chpBadPerms", "chatpolls.chp.polls", s);
                            return true;
                        }
                        // Personal polls
                        CHPPersonalBukkit personalCmd = new CHPPersonalBukkit(s, label, args, refs);
                        return personalCmd.processCommand();
                    }
                case "chpl":
                    // Change localization
                    CHPLocalizeBukkit c = new CHPLocalizeBukkit(s, label, args, refs);
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

    public Map<UUID, Map<String, PollObject>> getPersonalPolls() {
        return personalPolls;
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
            refs.debugMsg("Adding new player record " + uuid);
            PlayerRecord newRecord = new PlayerRecord(uuid);
            addPlayerRecord(newRecord);
            refs.debugMsg("Size of records: " + playerRecords.size());
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

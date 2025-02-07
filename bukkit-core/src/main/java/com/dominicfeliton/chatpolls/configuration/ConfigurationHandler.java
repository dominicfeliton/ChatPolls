package com.dominicfeliton.chatpolls.configuration;

import com.dominicfeliton.chatpolls.ChatPolls;
import com.dominicfeliton.chatpolls.ChatPollsHelper;
import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericRunnable;
import com.dominicfeliton.chatpolls.util.SupportedLang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.dominicfeliton.chatpolls.ChatPollsHelper.SchedulerType.ASYNC;
import static com.dominicfeliton.chatpolls.util.CommonRefs.supportedPluginLangCodes;

public class ConfigurationHandler {

    private final ChatPolls main = ChatPolls.instance;
    private final CommonRefs refs = main.getCommonRefs();
    private final ChatPollsHelper chpHelper = main.getHelper();
    private ConfigurationGenerator configGen = new ConfigurationGenerator(this);

    private File configFile;
    private YamlConfiguration mainConfig;

    private final ConcurrentHashMap<String, YamlConfiguration> pluginLangConfigs = new ConcurrentHashMap<>();

    /* Init Main Config Method */
    public void initMainConfig() {
        /* Init config file */
        String name = "config.yml";
        configFile = new File(main.getDataFolder(), name);
        mainConfig = configGen.setupConfig(configFile);
        saveMainConfig(false);
        YamlConfiguration templateConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(main.getResource(name)), StandardCharsets.UTF_8));

        /* Get plugin lang */
        if (refs.isSupportedLang(mainConfig.getString("General.pluginLang"), CommonRefs.LangType.LOCAL)) {
            refs.log(ChatColor.LIGHT_PURPLE + "Detected language " + mainConfig.getString("General.pluginLang") + ".");
            return;
        }

        mainConfig.set("General.pluginLang", "en");
        refs.warnLog("Unable to detect a valid language in your config.yml. Defaulting to en...");
    }

    /* Init Messages Method */
    public void initMessagesConfigs() {
        // Init ALL message configs
        refs.warnLog("Importing/upgrading localization files...");
        Set<SupportedLang> uniqueLangs = new HashSet<>(supportedPluginLangCodes.values());
        for (SupportedLang eaLang : uniqueLangs) {
            String eaStr = eaLang.getLangCode();
            refs.debugMsg("Checking " + eaStr + "...");
            YamlConfiguration currConfig = generateMessagesConfig(eaStr);
            if (currConfig == null) {
                refs.warnLog(refs.getPlainMsg("chpLangNotLoadedConsole",
                        new String[]{"&c" + eaStr},
                        "&e"));
                continue;
            }

            pluginLangConfigs.put(eaStr, generateMessagesConfig(eaStr));
        }
        refs.warnLog("Done.");
    }

    public YamlConfiguration generateMessagesConfig(String inLocalLang) {
        File msgFolder = new File(main.getDataFolder(), "locals");

        /* Init config file */
        File msgFile = new File(msgFolder, "messages-" + inLocalLang + ".yml");
        if (main.getResource("locals" + File.separator + "messages-" + inLocalLang + ".yml") == null) {
            refs.debugMsg("!!! Skipping " + inLocalLang + ", not found in default resources??");
            return null;
        }

        /* Save default messages file if it does not exist */
        if (!msgFile.exists()) {
            main.saveResource("locals" + File.separator + "messages-" + inLocalLang + ".yml", true);

            YamlConfiguration tempConfig = YamlConfiguration.loadConfiguration(msgFile);

            tempConfig.set("DoNotTouchThis.Version", CommonRefs.messagesConfigVersion);

            saveCustomConfig(tempConfig, msgFile, false);
        }

        /* Load config */
        YamlConfiguration msgConfig = YamlConfiguration.loadConfiguration(msgFile);

        /* Check if version value is out of date...*/
        if (msgConfig.getString("DoNotTouchThis.Version") == null || !msgConfig.getString("DoNotTouchThis.Version").equals(CommonRefs.messagesConfigVersion)) {
            refs.debugMsg("Upgrading out-of-date messages config!");
            HashMap<String, String> oldOverrides = new HashMap<>();

            /* Copy overrides section */
            if (msgConfig.getConfigurationSection("Overrides") != null) {
                for (String eaKey : Objects.requireNonNull(msgConfig.getConfigurationSection("Overrides")).getKeys(true)) {
                    oldOverrides.put(eaKey, msgConfig.getString("Overrides." + eaKey));
                }
            }

            /* Delete old config */
            if (msgFile.exists() && !msgFile.delete()) {
                refs.debugMsg("Could not delete old messages file for " + inLocalLang + "! Perhaps bad permissions?");
                return null;
            }

            /* Copy newest config */
            main.saveResource("locals" + File.separator + "messages-" + inLocalLang + ".yml", true);
            msgConfig = YamlConfiguration.loadConfiguration(msgFile);
            msgConfig.set("DoNotTouchThis.Version", CommonRefs.messagesConfigVersion);

            /* Paste overrides section */
            if (!oldOverrides.isEmpty()) {
                for (Map.Entry<String, String> entry : oldOverrides.entrySet()) {
                    msgConfig.set("Overrides." + entry.getKey(), entry.getValue());
                }
            }

            /* Save messages config */
            saveCustomConfig(msgConfig, msgFile, false);

            /* Success :) */
            refs.debugMsg("Upgrade successful.");
        }

        return msgConfig;
    }

    /* Load Main Settings Method */
    public void loadMainSettings() {
        /* General Settings */
        // Debug Mode
        // Not stored in main, since we want debug MSGs ASAP
        if (mainConfig.getBoolean("General.enableDebugMode")) {
            refs.warnLog(refs.getPlainMsg("chpConfigDebugEnabled"));
        }
    }

    /* Main config save method */
    public void saveMainConfig(boolean async) {
        if (async) {
            refs.debugMsg("Saving main config async!");
            GenericRunnable out = new GenericRunnable() {
                @Override
                protected void execute() {
                    saveMainConfig(false);
                }
            };
            chpHelper.runAsync(out, ASYNC, null);
            return;
        }
        refs.debugMsg("Saving main config sync!");
        saveCustomConfig(mainConfig, configFile, false);
    }

    /* Specific message config save method */
    public void saveMessagesConfig(String inLang, boolean async) {
        if (async) {
            refs.debugMsg("Saving messages config async!");
            GenericRunnable out = new GenericRunnable() {
                @Override
                protected void execute() {
                    saveMessagesConfig(inLang, false);
                }
            };
            chpHelper.runAsync(out, ASYNC, null);
            return;
        }
        refs.debugMsg("Saving messages config sync!");
        saveCustomConfig(pluginLangConfigs.get(inLang), new File(main.getDataFolder(), "messages-" + inLang + ".yml"), false);
    }

    /* Custom config save method */
    public synchronized void saveCustomConfig(YamlConfiguration inConfig, File dest, boolean async) {
        if (async && main.isEnabled()) {
            refs.debugMsg("Saving custom config async!");
            GenericRunnable out = new GenericRunnable() {
                @Override
                protected void execute() {
                    saveCustomConfig(inConfig, dest, false);
                }
            };
            chpHelper.runAsync(out, ASYNC, null);
            return;
        }
        if (inConfig != null && dest != null) {
            refs.debugMsg("Saving custom config sync!");
            try {
                inConfig.save(dest);
            } catch (IOException e) {
                e.printStackTrace();
                Bukkit.getPluginManager().disablePlugin(main);
                return;
            }
        }
    }

    /* Getters */
    public YamlConfiguration getCustomMessagesConfig(String inLocalLang) {
        return pluginLangConfigs.get(inLocalLang);
    }

    public YamlConfiguration getMainConfig() {
        return mainConfig;
    }

    public YamlConfiguration getMsgsConfig() {
        return pluginLangConfigs.get(mainConfig.getString("General.pluginLang"));
    }

    public File getConfigFile() {
        return configFile;
    }

    public ConcurrentHashMap<String, YamlConfiguration> getPluginLangConfigs() {
        return pluginLangConfigs;
    }
}

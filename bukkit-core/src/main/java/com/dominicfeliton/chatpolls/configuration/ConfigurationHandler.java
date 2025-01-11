package com.dominicfeliton.chatpolls.configuration;

import com.dominicfeliton.chatpolls.ChatPolls;
import com.dominicfeliton.chatpolls.ChatPollsHelper;
import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericRunnable;
import com.dominicfeliton.chatpolls.util.SupportedLang;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dominicfeliton.chatpolls.ChatPollsHelper.SchedulerType.ASYNC;
import static com.dominicfeliton.chatpolls.util.CommonRefs.supportedPluginLangCodes;

public class ConfigurationHandler {

    private ChatPolls main = ChatPolls.instance;
    private CommonRefs refs = main.getCommonRefs();
    private ChatPollsHelper chpHelper = main.getHelper();
    private ConfigurationGenerator configGen = new ConfigurationGenerator(this);

    private File configFile;
    private YamlConfiguration mainConfig;

    private final ConcurrentHashMap<String, YamlConfiguration> pluginLangConfigs = new ConcurrentHashMap<>();

    /* Init Messages Method */
    public void initMessagesConfigs() {
        // Init ALL message configs
        main.getLogger().warning("Importing/upgrading localization files...");
        Set<SupportedLang> uniqueLangs = new HashSet<>(supportedPluginLangCodes.values());
        for (SupportedLang eaLang : uniqueLangs) {
            String eaStr = eaLang.getLangCode();
            refs.debugMsg("Checking " + eaStr + "...");
            YamlConfiguration currConfig = generateMessagesConfig(eaStr);
            if (currConfig == null) {
                main.getLogger().warning(refs.getPlainMsg("chpLangNotLoadedConsole",
                        new String[]{"&c" + eaStr},
                        "&e"));
                continue;
            }

            pluginLangConfigs.put(eaStr, generateMessagesConfig(eaStr));
        }
        main.getLogger().warning("Done.");
    }

    public YamlConfiguration generateMessagesConfig(String inLocalLang) {
        /* Init config file */
        File msgFile = new File(main.getDataFolder(), "messages-" + inLocalLang + ".yml");
        if (main.getResource("messages-" + inLocalLang + ".yml") == null) {
            refs.debugMsg("!!! Skipping " + inLocalLang + ", not found in default resources??");
            return null;
        }

        /* Save default messages file if it does not exist */
        if (!msgFile.exists()) {
            main.saveResource("messages-" + inLocalLang + ".yml", true);

            YamlConfiguration tempConfig = YamlConfiguration.loadConfiguration(msgFile);

            tempConfig.set("DoNotTouchThis.Version", ChatPolls.messagesConfigVersion);

            saveCustomConfig(tempConfig, msgFile, false);
        }

        /* Load config */
        YamlConfiguration msgConfig = YamlConfiguration.loadConfiguration(msgFile);

        /* Check if version value is out of date...*/
        if (msgConfig.getString("DoNotTouchThis.Version") == null || !msgConfig.getString("DoNotTouchThis.Version").equals(ChatPolls.messagesConfigVersion)) {
            refs.debugMsg("Upgrading out-of-date messages config!");
            HashMap<String, String> oldOverrides = new HashMap<>();

            /* Copy overrides section */
            if (msgConfig.getConfigurationSection("Overrides") != null) {
                for (String eaKey : msgConfig.getConfigurationSection("Overrides").getKeys(true)) {
                    oldOverrides.put(eaKey, msgConfig.getString("Overrides." + eaKey));
                }
            }

            /* Delete old config */
            if (msgFile.exists() && !msgFile.delete()) {
                refs.debugMsg("Could not delete old messages file for " + inLocalLang + "! Perhaps bad permissions?");
                return null;
            }

            /* Copy newest config */
            main.saveResource("messages-" + inLocalLang + ".yml", true);
            msgConfig = YamlConfiguration.loadConfiguration(msgFile);
            msgConfig.set("DoNotTouchThis.Version", ChatPolls.messagesConfigVersion);

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

package com.dominicfeliton.chatpolls.util;

import com.dominicfeliton.chatpolls.ChatPolls;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;

public class BukkitCommonRefs extends CommonRefs {

    /* Important vars */
    protected static ChatPolls main = ChatPolls.instance;

    /**
     * Sends a debug message to console. Will only work when debug mode is set to true in the Console.
     *
     * @param inMessage - The debug message that will be sent to the Console.
     */
    @Override
    public void debugMsg(String inMessage) {
        if (main.getConfigManager() == null) {
            return;
        }

        if (main.getConfigManager().getMainConfig().getBoolean("General.enableDebugMode")) {
            warnLog("DEBUG: " + inMessage);
        }
    }

    @Override
    public String getPlainMsg(String messageName) {
        return serial(getCompMsg(messageName, new String[]{}, "", null));
    }

    @Override
    public String getPlainMsg(String messageName, GenericCommandSender sender) {
        return serial(getCompMsg(messageName, new String[]{}, "", sender));
    }

    @Override
    public String getPlainMsg(String messageName, String replacement) {
        return serial(getCompMsg(messageName, new String[]{replacement}, "", null));
    }

    @Override
    public String getPlainMsg(String messageName, String replacement, String resetCode) {
        return serial(getCompMsg(messageName, new String[]{replacement}, resetCode, null));
    }

    @Override
    public String getPlainMsg(String messageName, String replacement, GenericCommandSender sender) {
        return serial(getCompMsg(messageName, new String[]{replacement}, "", sender));
    }

    @Override
    public String getPlainMsg(String messageName, String[] replacements, GenericCommandSender sender) {
        return serial(getCompMsg(messageName, replacements, "", sender));
    }

    @Override
    public String getPlainMsg(String messageName, String replacement, String resetCode, GenericCommandSender sender) {
        return serial(getCompMsg(messageName, new String[]{replacement}, resetCode, sender));
    }

    @Override
    public String getPlainMsg(String messageName, String[] replacements, String resetCode) {
        return serial(getCompMsg(messageName, replacements, resetCode, null));
    }

    /**
     * Gets a plaintext message from the config with proper formatting
     *
     * @param messageName
     * @param replacements
     * @param resetCode
     * @param sender
     * @return
     */
    @Override
    public String getPlainMsg(String messageName, String[] replacements, String resetCode, GenericCommandSender sender) {
        return serial(getCompMsg(messageName, replacements, resetCode, sender));
    }

    /**
     * Gets a message from the currently selected messages-XX.yml.
     *
     * @param messageName  - The name of the message from messages-XX.yml.
     * @param replacements - The list of replacement values that replace variables in the selected message. There is no sorting system; the list must be already sorted.
     * @param resetCode    - The color code sequence (&4&l, etc.) that the rest of the message should use besides the replacement values.
     * @param sender       - The person/entity to be sent this message. Can be null for nobody in particular.
     * @return String - The formatted message from messages-XX.yml. A warning will be returned instead if messageName is missing from messages-XX.yml.
     */
    @Override
    public TextComponent getCompMsg(String messageName, String[] replacements, String resetCode, GenericCommandSender sender) {
        YamlConfiguration messagesConfig = main.getConfigManager().getMsgsConfig();
        String code = "";
        String globalCode = main.getConfigManager().getMainConfig().getString("General.pluginLang");
        CommandSender bukkitSender = sender != null ? ((BukkitCommandSender) sender).getBukkitSender() : null;

        if (bukkitSender instanceof Player && main.isPlayerRecord((Player) bukkitSender)) {
            code = main.getPlayerRecord((Player) bukkitSender, false).getLocalizationCode();
            if (!code.isEmpty()) {
                messagesConfig = main.getConfigManager().getCustomMessagesConfig(code);
            }
        }

        for (int i = 0; i < replacements.length; i++) {
            // Translate color codes in replacements
            replacements[i] = ChatColor.translateAlternateColorCodes('&', replacements[i] + resetCode);
        }

        /* Get message from messages.yml */
        String convertedOriginalMessage = resetCode;
        if (messagesConfig.getString("Overrides." + ChatColor.stripColor(messageName)) != null) {
            convertedOriginalMessage += ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("Overrides." + ChatColor.stripColor(messageName)));
        } else {
            if (messagesConfig.getString("Messages." + ChatColor.stripColor(messageName)) == null) {
                if (code.isEmpty()) {
                    severeLog("Bad message (" + messageName + ")! Please fix your messages-" + globalCode + ".yml.");
                    return Component.text().content(ChatColor.RED + "Bad message (" + messageName + ")! Please fix your messages-" + globalCode + ".yml.").build();
                } else {
                    severeLog("Bad message (" + messageName + ")! Please fix your messages-" + code + ".yml.");
                    return Component.text().content(ChatColor.RED + "Bad message (" + messageName + ")! Please fix your messages-" + code + ".yml.").build();
                }
            }
            convertedOriginalMessage += messagesConfig.getString("Messages." + ChatColor.stripColor(messageName));
        }

        // Translate color codes in the original message
        convertedOriginalMessage = ChatColor.translateAlternateColorCodes('&', convertedOriginalMessage);

        // Escape single quotes for MessageFormat
        convertedOriginalMessage = convertedOriginalMessage.replace("'", "''").trim();

        // Return fixedMessage with replaced vars
        return Component.text().content(MessageFormat.format(convertedOriginalMessage, (Object[]) replacements)).build();
    }

    @Override
    public void log(String message) {
        main.getLogger().info(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void warnLog(String message) {
        main.getLogger().warning(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void severeLog(String message) {
        main.getLogger().severe(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void sendMsg(String messageName, GenericCommandSender sender) {
        sendMsg(sender, getCompMsg(messageName, new String[]{}, "&r&d", sender), true);
    }

    @Override
    public void sendMsg(String messageName, String[] replacements, GenericCommandSender sender) {
        // Default CHP color is usually LIGHT_PURPLE (&d)
        sendMsg(sender, getCompMsg(messageName, replacements, "&r&d", sender), true);
    }

    @Override
    public void sendMsg(String messageName, String replacement, GenericCommandSender sender) {
        sendMsg(sender, getCompMsg(messageName, new String[]{replacement}, "&r&d", sender), true);
    }

    @Override
    public void sendMsg(String messageName, String replacement, String resetCode, GenericCommandSender sender) {
        sendMsg(sender, getCompMsg(messageName, new String[]{replacement}, "&r" + resetCode, sender), true);
    }

    @Override
    public void sendMsg(String messageName, String[] replacements, String resetCode, GenericCommandSender sender) {
        sendMsg(sender, getCompMsg(messageName, replacements, "&r" + resetCode, sender), true);
    }

    /**
     * Sends the user a properly formatted message through our adventure instance.
     *
     * @param sender          - The target sender. Can be any entity that can receive messages. CANNOT BE NULL.
     * @param originalMessage - The unformatted Component that should be sent to sender.
     * @param addPrefix       - Whether the plugin prefix should be appended or not.
     * @return
     */
    @Override
    public void sendMsg(GenericCommandSender sender, Component originalMessage, boolean addPrefix) {

        try {
            Audience adventureSender = main.adventure().sender(((BukkitCommandSender) sender).getBukkitSender());
            final TextComponent outMessage;
            if (addPrefix) {
                outMessage = Component.text().append(main.getPluginPrefix().asComponent())
                        .append(Component.space())
                        .append(originalMessage.asComponent())
                        .build();
            } else {
                outMessage = Component.text().append(originalMessage.asComponent()).build();
            }
            adventureSender.sendMessage(outMessage);
        } catch (IllegalStateException e) {
        }
    }

    @Override
    public void sendMsg(GenericCommandSender sender, Component originalMessage) { sendMsg(sender, originalMessage, true); }

    @Override
    public void sendMsg(GenericCommandSender sender, String stringMsg) {
        sendMsg(sender, deserial(stringMsg), true);
    }

    @Override
    public void sendMsg(GenericCommandSender sender, String stringMsg, boolean addPrefix) {
        sendMsg(sender, deserial(stringMsg), addPrefix);
    }

    /**
     * Shorthand for component to str
     *
     * @param comp - TextComponent
     * @return string version
     */
    public String serial(Component comp) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(comp);
    }

    /**
     * Shorthand for str to component
     *
     * @param str - String
     * @return textcomponent version
     */
    public Component deserial(String str) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(str);
    }

    public boolean serverIsStopping() {
        boolean stopping = !main.isEnabled();
        debugMsg("Bukkit initial stop check: " + stopping);
        if (stopping) return true;

        try {
            Bukkit.getScheduler().runTaskLater(main, () -> {
            }, 0L);
        } catch (IllegalPluginAccessException | IllegalStateException e) {
            debugMsg("Server is stopping! Don't run a task/do any weird stuff.");
            return true;
        }

        debugMsg("Bukkit final stop check: false");
        return false;
    }

    @Override
    public Object getPlayerObjExact(String inName) {
        return Bukkit.getPlayerExact(inName);
    }

    @Override
    public Object getPlayer(String inName) {
        return Bukkit.getPlayer(inName);
    }

    /**
     * Compares two strings to check if they are the same language under the current translator.
     *
     * @param first    - A valid language name
     * @param second   - A valid language name
     * @param langType - 'local' for local langs
     * @return Boolean - Whether languages are the same or not
     */
    public boolean isSameLang(String first, String second, LangType langType) {
        return isSupportedLang(first, langType) && isSupportedLang(second, langType)
                && getSupportedLang(first, langType).equals(getSupportedLang(second, langType));
    }

    /**
     * Gets a supported language under the current translator.
     *
     * @param langName - A valid language name
     * @param langType - 'local' for local langs
     * @return SupportedLanguageObject - Will be completely empty if the language is invalid
     */
    public SupportedLang getSupportedLang(String langName, LangType langType) {
        // Setup vars
        SupportedLang invalidLang = new SupportedLang("", "", "");
        SupportedLang outLang;

        // Return None if none
        /*
        if (langName.equalsIgnoreCase("None") || langName.equalsIgnoreCase("auto")) {
            outLang = new SupportedLang("auto", "None", "None");
            return outLang;
        }
         */

        // Check langType using enum
        switch (langType) {
            case LOCAL:
                outLang = supportedPluginLangCodes.get(langName);
                break;
            default:
                debugMsg("Invalid langType for getSupportedTranslatorLang()! langType: " + langType + " ...returning invalid, not checking language. Fix this!!!");
                outLang = null;
                break;
        }

        if (outLang == null) {
            debugMsg("Lang " + langName + " not found in " + langType + "!");
            return invalidLang;
        }

        return outLang;
    }

    /**
     * Checks if a language is supported under the current translator.
     *
     * @param in       - A valid language name
     * @param langType - 'local' for local langs
     * @return true if supported, false otherwise
     */
    public boolean isSupportedLang(String in, LangType langType) {
        return !getSupportedLang(in, langType).getLangCode().isEmpty();
    }

    /**
     * Gets a list of properly formatted, supported language codes.
     *
     * @param langType - 'local' for local langs
     * @return String - Formatted language codes
     */
    public String getFormattedLangCodes(String langType) {
        /* Setup vars */
        Map<String, SupportedLang> langMap;
        StringBuilder out = new StringBuilder("\n");

        /* Check langType */
        switch (langType.toLowerCase()) {
            case "local":
                langMap = CommonRefs.supportedPluginLangCodes;
                break;
            default:
                debugMsg("Invalid langType for getFormattedValidLangCodes()! langType: " + langType + " ...returning invalid, not checking language. Fix this!!!");
                return "&cInvalid language type specified";
        }

        /* Use a TreeSet to eliminate duplicates and sort */
        TreeSet<SupportedLang> sortedUniqueLangs = new TreeSet<>(langMap.values());

        /* Format the output nicely */
        for (SupportedLang lang : sortedUniqueLangs) {
            if (lang == null) {
                debugMsg("Lang codes not set for " + langType + "! FIX THIS");
                out.append("N/A");
                break;
            }
            out.append("&b").append(lang.getLangCode())
                    .append(" &f- ")
                    .append("&e").append(lang.getLangName()).append("&6/&e").append(lang.getNativeLangName())
                    .append("&r, ");
        }

        // Remove the last comma and space if present
        if (out.length() > 2) {
            out.setLength(out.length() - 2);
        }

        return out.toString();
    }
}

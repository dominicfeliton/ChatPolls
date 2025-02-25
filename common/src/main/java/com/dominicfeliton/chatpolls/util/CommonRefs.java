package com.dominicfeliton.chatpolls.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.kyori.adventure.text.Component;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;

/**
 * Abstract class defining platform-independent common references and utilities.
 * Platform-specific subclasses should provide concrete implementations.
 */
public abstract class CommonRefs {
    /* Important variables */
    public static final String messagesConfigVersion = "022425-1";

    // Supported Minecraft Versions
    public static final String[] supportedMCVersions = {
            "1.21.4", "1.21.3", "1.21.2", "1.21.1",
            "1.20", "1.19", "1.18", "1.17",
            "1.16", "1.15", "1.14", "1.13"
    };

    // Supported Language Codes
    public static final Map<String, SupportedLang> supportedPluginLangCodes = new LinkedHashMap<>();

    static {
        // Initialize supported language codes
        List.of(
                "af", "am", "ar", "az", "bg", "bn", "bs", "ca",
                "cs", "cy", "da", "de", "el", "en", "es", "es-MX",
                "et", "fa", "fa-AF", "fi", "fr", "fr-CA", "ga", "gu",
                "ha", "he", "hi", "hr", "ht", "hu", "hy", "id",
                "is", "it", "ja", "ka", "kk", "kn", "ko", "lt",
                "lv", "mk", "ml", "mn", "mr", "ms", "mt", "nl",
                "no", "pa", "pl", "ps", "pt", "pt-PT", "ro", "ru",
                "si", "sk", "sl", "so", "sq", "sr", "sv", "sw",
                "ta", "te", "th", "tl", "tr", "uk", "ur", "uz",
                "vi", "zh", "zh-TW"
        ).forEach(langCode -> supportedPluginLangCodes.put(langCode, new SupportedLang(langCode, "", "")));

        // Fix language names using a generic method
        Map<String, SupportedLang> fixedMap = fixLangNames(
                new LinkedHashMap<>(supportedPluginLangCodes), false, true
        );
        supportedPluginLangCodes.putAll(fixedMap);
    }

    public enum LangType {
        LOCAL("local");

        private final String type;

        LangType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public static LangType fromString(String type) {
            for (LangType langType : LangType.values()) {
                if (langType.type.equalsIgnoreCase(type)) {
                    return langType;
                }
            }
            throw new IllegalArgumentException("Unknown langType: " + type);
        }
    }

    /**
     * Sends a debug message to the console.
     * Implementation depends on the platform.
     *
     * @param inMessage The debug message to send.
     */
    public abstract void debugMsg(String inMessage);

    /**
     * Retrieves a plain text message without formatting.
     *
     * @param messageName The key of the message.
     * @return The plain text message.
     */
    public abstract String getPlainMsg(String messageName);

    /**
     * Retrieves a plain text message without formatting for a specific sender.
     *
     * @param messageName The key of the message.
     * @param sender      The command sender.
     * @return The plain text message.
     */
    public abstract String getPlainMsg(String messageName, GenericCommandSender sender);

    /**
     * Retrieves a plain text message with a single replacement.
     *
     * @param messageName The key of the message.
     * @param replacement The replacement string.
     * @return The plain text message.
     */
    public abstract String getPlainMsg(String messageName, String replacement);

    /**
     * Retrieves a plain text message with a single replacement and a reset code.
     *
     * @param messageName The key of the message.
     * @param replacement The replacement string.
     * @param resetCode   The reset code.
     * @return The plain text message.
     */
    public abstract String getPlainMsg(String messageName, String replacement, String resetCode);

    /**
     * Retrieves a plain text message with a single replacement for a specific sender.
     *
     * @param messageName The key of the message.
     * @param replacement The replacement string.
     * @param sender      The command sender.
     * @return The plain text message.
     */
    public abstract String getPlainMsg(String messageName, String replacement, GenericCommandSender sender);

    /**
     * Retrieves a plain text message with multiple replacements for a specific sender.
     *
     * @param messageName  The key of the message.
     * @param replacements The array of replacement strings.
     * @param sender       The command sender.
     * @return The plain text message.
     */
    public abstract String getPlainMsg(String messageName, String[] replacements, GenericCommandSender sender);

    /**
     * Retrieves a plain text message with a single replacement, reset code, and specific sender.
     *
     * @param messageName The key of the message.
     * @param replacement The replacement string.
     * @param resetCode   The reset code.
     * @param sender      The command sender.
     * @return The plain text message.
     */
    public abstract String getPlainMsg(String messageName, String replacement, String resetCode, GenericCommandSender sender);

    /**
     * Retrieves a plain text message with multiple replacements and a reset code.
     *
     * @param messageName  The key of the message.
     * @param replacements The array of replacement strings.
     * @param resetCode    The reset code.
     * @return The plain text message.
     */
    public abstract String getPlainMsg(String messageName, String[] replacements, String resetCode);

    /**
     * Retrieves a plain text message with multiple replacements, reset code, and specific sender.
     *
     * @param messageName  The key of the message.
     * @param replacements The array of replacement strings.
     * @param resetCode    The reset code.
     * @param sender       The command sender.
     * @return The plain text message.
     */
    public abstract String getPlainMsg(String messageName, String[] replacements, String resetCode, GenericCommandSender sender);

    /**
     * Retrieves a formatted text component message.
     *
     * @param messageName  The key of the message.
     * @param replacements The array of replacement strings.
     * @param resetCode    The reset code.
     * @param sender       The command sender.
     * @return The formatted text component.
     */
    public abstract Component getCompMsg(String messageName, String[] replacements, String resetCode, GenericCommandSender sender);

    public abstract void log(String message);

    public abstract void warnLog(String message);

    public abstract void severeLog(String message);

    /**
     * Sends a message to the specified sender.
     *
     * @param messageName The key of the message.
     * @param sender      The command sender.
     */
    public abstract void sendMsg(String messageName, GenericCommandSender sender);

    /**
     * Sends a message with replacements to the specified sender.
     *
     * @param messageName  The key of the message.
     * @param replacements The array of replacement strings.
     * @param sender       The command sender.
     */
    public abstract void sendMsg(String messageName, String[] replacements, GenericCommandSender sender);

    /**
     * Sends a message with a single replacement to the specified sender.
     *
     * @param messageName The key of the message.
     * @param replacement The replacement string.
     * @param sender      The command sender.
     */
    public abstract void sendMsg(String messageName, String replacement, GenericCommandSender sender);

    /**
     * Sends a message with a single replacement and reset code to the specified sender.
     *
     * @param messageName The key of the message.
     * @param replacement The replacement string.
     * @param resetCode   The reset code.
     * @param sender      The command sender.
     */
    public abstract void sendMsg(String messageName, String replacement, String resetCode, GenericCommandSender sender);

    /**
     * Sends a message with multiple replacements and a reset code to the specified sender.
     *
     * @param messageName  The key of the message.
     * @param replacements The array of replacement strings.
     * @param resetCode    The reset code.
     * @param sender       The command sender.
     */
    public abstract void sendMsg(String messageName, String[] replacements, String resetCode, GenericCommandSender sender);

    /**
     * Sends a formatted text component message to the specified sender.
     *
     * @param sender          The command sender.
     * @param originalMessage The text component to send.
     * @param addPrefix       Whether to add a prefix to the message.
     */
    public abstract void sendMsg(GenericCommandSender sender, Component originalMessage, boolean addPrefix);

    /**
     * Sends a formatted text component message with prefix to the specified sender.
     *
     * @param sender          The command sender.
     * @param originalMessage The text component to send.
     */
    public abstract void sendMsg(GenericCommandSender sender, Component originalMessage);

    /**
     * Sends a string message to the specified sender.
     *
     * @param sender     The command sender.
     * @param stringMsg  The message string.
     */
    public abstract void sendMsg(GenericCommandSender sender, String stringMsg);

    /**
     * Sends a string message to the specified sender with prefix control.
     *
     * @param sender     The command sender.
     * @param stringMsg  The message string.
     * @param addPrefix  Whether to add a prefix to the message.
     */
    public abstract void sendMsg(GenericCommandSender sender, String stringMsg, boolean addPrefix);

    /**
     * Serializes a text component to a string.
     *
     * @param comp The text component.
     * @return The serialized string.
     */
    public abstract String serial(Component comp);

    /**
     * Deserializes a string to a text component.
     *
     * @param str The string to deserialize.
     * @return The text component.
     */
    public abstract Component deserial(String str);

    /**
     * Checks if the server is stopping
     * @return boolean - If the server is stopping or not
     */
    public abstract boolean serverIsStopping();

    public abstract Object getPlayerObjExact(String inName);

    public abstract Object getPlayer(String inName);

    /**
     * Sends a bad permissions error
     *
     * @param correctPerm
     * @param sender
     */
    public void badPermsMessage(String correctPerm, GenericCommandSender sender) {
        sendMsg("chpBadPerms", "&6" + correctPerm, "&c", sender);
    }

    /**
     * Compares two strings to check if they are the same language under the current translator.
     *
     * @param first    - A valid language name
     * @param second   - A valid language name
     * @param langType - 'local' for local langs
     * @return Boolean - Whether languages are the same or not
     */
    public abstract boolean isSameLang(String first, String second, LangType langType);

    /**
     * Gets a supported language under the current translator.
     *
     * @param langName - A valid language name
     * @param langType - 'local' for local langs
     * @return SupportedLanguageObject - Will be completely empty if the language is invalid
     */
    public abstract SupportedLang getSupportedLang(String langName, LangType langType);

    /**
     * Checks if a language is supported under the current translator.
     *
     * @param in       - A valid language name
     * @param langType - 'local' for local langs
     * @return true if supported, false otherwise
     */
    public abstract boolean isSupportedLang(String in, LangType langType);

    /**
     * Gets a list of properly formatted, supported language codes.
     *
     * @param langType - 'local' for local langs
     * @return String - Formatted language codes
     */
    public abstract String getFormattedLangCodes(String langType);

    /**
     * A platform-agnostic enum storing "sound names" and pitch/volume values
     * without directly depending on org.bukkit.Sound.
     */
    public enum SoundType {
        SUBMENU_TOGGLE_ON("SUBMENU_TOGGLE_ON", "BLOCK_NOTE_BLOCK_HAT", 0.5f, 1.0f),
        SUBMENU_TOGGLE_OFF("SUBMENU_TOGGLE_OFF", "BLOCK_NOTE_BLOCK_SNARE", 0.5f, 1.0f),
        START_TRANSLATION("START_TRANSLATION", "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f),
        STOP_TRANSLATION("STOP_TRANSLATION", "BLOCK_WOODEN_BUTTON_CLICK_OFF", 1.0f, 1.0f),
        RELOAD_SUCCESS("RELOAD_SUCCESS", "BLOCK_BEACON_ACTIVATE", 1.0f, 1.0f),
        RELOAD_ERROR("RELOAD_ERROR", "BLOCK_DISPENSER_FAIL", 1.0f, 1.0f),

        // If you previously had logic referencing version-specific sounds, you can keep it simple here:
        STATS_SUCCESS("STATS_SUCCESS", "ITEM_BOOK_PAGE_TURN", 1.0f, 1.0f),
        STATS_FAIL("STATS_FAIL", "BLOCK_NOTE_BLOCK_BASS", 1.0f, 1.0f),
        CHP_VERSION("CHP_VERSION", "ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f),
        PENDING_RELOAD("PENDING_RELOAD", "BLOCK_NOTE_BLOCK_XYLOPHONE", 1.0f, 1.0f);

        private final String alias;         // Your internal name (e.g. "SUBMENU_TOGGLE_ON")
        private final String soundKey;      // The string key representing the sound (e.g. "BLOCK_NOTE_BLOCK_HAT")
        private final float volume;
        private final float pitch;

        SoundType(String alias, String soundKey, float volume, float pitch) {
            this.alias = alias;
            this.soundKey = soundKey;
            this.volume = volume;
            this.pitch = pitch;
        }

        public String getAlias() {
            return alias;
        }

        /**
         * The raw string identifier. On Bukkit, you'll parse this to a Sound.
         */
        public String getSoundKey() {
            return soundKey;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }

        public static SoundType fromString(String name) {
            for (SoundType st : values()) {
                if (st.alias.equalsIgnoreCase(name)) {
                    return st;
                }
            }
            throw new IllegalArgumentException("Unknown SoundType alias: " + name);
        }
    }

    public abstract void playSound(SoundType soundType, GenericCommandSender sender);

    /**
     * Returns a SupportedLang obj with nativeLang/langName fields filled out
     *
     * @param code - the language code to search for
     * @return Supported Lang Object, or an unfixed obj if it was not found in the JSON
     */
    public static SupportedLang fixLangName(String code) {
        String isoJsonFilePath = "ISO-CHP.json";
        ObjectMapper objectMapper = new ObjectMapper();
        SupportedLang out = new SupportedLang(code);

        try (InputStream in = CommonRefs.class.getClassLoader().getResourceAsStream(isoJsonFilePath)) {
            if (in == null) {
                // The resource wasn't found - handle error
                return out;
            }
            Map<String, ISOLanguage> languageMap = objectMapper.readValue(in,
                    new TypeReference<Map<String, ISOLanguage>>() {}
            );

            // hashSet means less dupes
            //debugMsg("Trying to fix " + code + " from JSON...");
            ISOLanguage jsonLang = languageMap.get(code);

            if (jsonLang != null) {
                out.setLangName(jsonLang.getIntName());
                out.setNativeLangName(jsonLang.getNativeName());
                return out;
            } else {
                //debugMsg("Could not find " + code + " in JSON!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            //main.getLogger().warning(getPlainMsg("chpISOJSONFail"));
        }

        // Return original
        return out;
    }

    /**
     * Fixes a given map of SupportedLangs to include native names/language names
     *
     * @param in          - Map of SupportedLang objs
     * @param nativesOnly - Whether we should add regular lang names as well as native langs
     * @param preInit     - Whether we are initializing or not (if not, do not send ANY messages; will not work in this state)
     * @return - The fixed map of supportedLang objs
     */
    public static Map<String, SupportedLang> fixLangNames(Map<String, SupportedLang> in, boolean nativesOnly, boolean preInit) {
        // Adjust the file path as necessary
        String isoJsonFilePath = "/ISO-CHP.json";
        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream inStream = CommonRefs.class.getResourceAsStream(isoJsonFilePath)) {
            if (inStream == null) {
                throw new FileNotFoundException("Can't find ISO reference! This is bad news...");
            }

            // Read the ISO language data from the JSON
            Map<String, ISOLanguage> languageMap =
                    objectMapper.readValue(inStream, new TypeReference<Map<String, ISOLanguage>>() {});

            // hashSet means less dupes
            for (SupportedLang currLang : new HashSet<>(in.values())) {
                String currCode = currLang.getLangCode();
                ISOLanguage jsonLang = languageMap.get(currCode);

                if (jsonLang != null) {
                    currLang.setNativeLangName(jsonLang.getNativeName());
                    in.put(currLang.getNativeLangName(), currLang);
                    if (!nativesOnly) {
                        // If we want to fix language names as well...
                        currLang.setLangName(jsonLang.getIntName());
                        in.put(currLang.getLangName(), currLang);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //if (!preInit) System.out.println(getPlainMsg("chpISOJSONFail"));
        }

        return in;
    }

    static class ISOLanguage {
        @JsonProperty("int")
        private List<String> intNames;
        @JsonProperty("native")
        private List<String> nativeNames;

        // Getters and Setters
        public List<String> getIntNames() {
            return intNames;
        }

        public void setIntNames(List<String> intNames) {
            this.intNames = intNames;
        }

        public List<String> getNativeNames() {
            return nativeNames;
        }

        public void setNativeNames(List<String> nativeNames) {
            this.nativeNames = nativeNames;
        }

        public String getIntName() {
            return intNames.get(0);
        }

        public String getNativeName() {
            return nativeNames.get(0);
        }
    }
}

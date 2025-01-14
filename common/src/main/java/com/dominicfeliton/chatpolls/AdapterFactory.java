package com.dominicfeliton.chatpolls;

import com.dominicfeliton.chatpolls.util.GenericCommandSender;
import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.RewardObject;
import org.apache.commons.lang3.tuple.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdapterFactory {

    private final String packageName = getClass().getPackage().getName();

    private String currPlatform = getServerInfo().getKey();

    /* Init server */
    public LinkedHashMap<String, String> getSupportedServerTypes() {
        // ALWAYS KEEP NEWEST FORKS FIRST
        // EX: Bukkit -> Spigot -> Paper
        LinkedHashMap<String, String> serverTypes = new LinkedHashMap<>();
        serverTypes.put("Bukkit", "org.bukkit.Bukkit");
        serverTypes.put("Spigot", "org.spigotmc.SpigotConfig");
        serverTypes.put("Paper", "com.destroystokyo.paper.PaperWorldConfig");
        serverTypes.put("Folia", "io.papermc.paper.plugin.configuration.PluginMeta");

        //serverTypes.put("BungeeCord", "net.md_5.bungee.api.ProxyServer");
        //serverTypes.put("Velocity", "com.velocitypowered.proxy.Velocity");

        return serverTypes;
    }

    /**
     * Check server type/version
     */
    public Pair<String, String> getServerInfo() {
        String serverPlatform = "Unknown";
        String serverVersion = "";

        /* Find specific server */
        for (Map.Entry<String, String> entry : getSupportedServerTypes().entrySet()) {
            try {
                Class.forName(entry.getValue());

                // If we are Folia, check if we have isFoliaSupported in PluginMeta class
                if (entry.getKey().equals("Folia"))
                    Class.forName("io.papermc.paper.plugin.configuration.PluginMeta").getMethod("isFoliaSupported");

                // We found class but continue loop, may be a fork (Bukkit -> Spigot -> Paper)
                serverPlatform = entry.getKey();
            } catch (Exception e) {
            }
        }

        /* Version check */
        switch (serverPlatform) {
            case "Bukkit":
            case "Spigot":
            case "Paper":
            case "Folia":
                serverVersion = getBukkitVersionReflectively();
                break;
            default:
                serverVersion = "Bukkit"; // TODO: Default to oldest version supported
                break;
        }
        if (serverVersion == null) {
            return null;
        }

        /* Additional checks */
        if (serverPlatform.equals("Paper") && (serverVersion.contains("1.13") || serverVersion.contains("1.14") || serverVersion.contains("1.15"))) {
            // These versions are so old that they lack much of what we take for granted in later versions of Paper.
            // Paper on these versions is unsupported. Use the spigot version of the plugin instead.
            // sendmsg(?) paper too old, default to spig
            serverPlatform = "Spigot";
        }

        /*
        // TODO
        if (serverPlatform.equals("Paper")) {
            try {
                Class.forName("com.dominicfeliton.chatpolls.PaperChatPollsHelper");
            } catch (ClassNotFoundException e) {
                // On paper but using the spigot JAR
                // sendmsg(?) using spigot on paper, you are missing out on features...
                serverPlatform = "Spigot";
            }
        }
         */

        return Pair.of(serverPlatform, serverVersion);
    }

    private String getBukkitVersionReflectively() {
        try {
            // 1. Get the Bukkit class
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");

            // 2. Invoke the static getServer() method
            //    - null is used for the 'obj' parameter since getServer() is a static method
            Object serverInstance = bukkitClass.getMethod("getServer").invoke(null);

            // 3. Invoke getVersion() on the returned server object
            return (String) serverInstance.getClass().getMethod("getVersion").invoke(serverInstance);
        } catch (ClassNotFoundException e) {
            // org.bukkit.Bukkit not found in the classpath
            return null;
        } catch (NoSuchMethodException
                 | IllegalAccessException
                 | InvocationTargetException e) {
            // One of the reflection calls failed
            e.printStackTrace();
            return null;
        }
    }

    public CommonRefs getCommonRefs() {
        HashMap<String, String> commonRefsDefs = new HashMap<String, String>();
        commonRefsDefs.put("Spigot", packageName + ".util.BukkitCommonRefs");
        commonRefsDefs.put("Bukkit", packageName + ".util.BukkitCommonRefs");
        //commonRefsDefs.put("Paper", packageName + ".util.PaperCommonRefs");
        commonRefsDefs.put("Paper", packageName + ".util.BukkitCommonRefs");
        commonRefsDefs.put("Folia", packageName + ".util.FoliaCommonRefs");

        return (CommonRefs) getInstance(commonRefsDefs);
    }

    public GenericCommandSender getCommandSender() {
        HashMap<String, String> cmdSenderDefs = new HashMap<String, String>();
        cmdSenderDefs.put("Spigot", packageName + ".util.BukkitCommandSender");
        cmdSenderDefs.put("Bukkit", packageName + ".util.BukkitCommandSender");
        cmdSenderDefs.put("Paper", packageName + ".util.BukkitCommandSender");
        cmdSenderDefs.put("Folia", packageName + ".util.BukkitCommandSender");

        return (GenericCommandSender) getInstance(cmdSenderDefs);
    }

    public ChatPollsHelper getCHPHelper() {
        HashMap<String, String> chpHelperDefs = new HashMap<String, String>();
        chpHelperDefs.put("Spigot", packageName + ".BukkitChatPollsHelper");
        chpHelperDefs.put("Bukkit", packageName + ".BukkitChatPollsHelper");
        //chpHelperDefs.put("Paper", packageName + ".PaperChatPollsHelper");
        chpHelperDefs.put("Paper", packageName + ".BukkitChatPollsHelper");
        chpHelperDefs.put("Folia", packageName + ".FoliaChatPollsHelper");

        return (ChatPollsHelper) getInstance(chpHelperDefs);
    }

    public <P, I> RewardObject<P, I> getRewardObject(
            RewardObject.RewardType type,
            Class<P> playerClass,
            Class<I> itemClass
    ) {
        HashMap<String, String> rewardObjDefs = new HashMap<>();
        rewardObjDefs.put("Spigot", packageName + ".util.BukkitRewardObject");
        rewardObjDefs.put("Bukkit", packageName + ".util.BukkitRewardObject");
        rewardObjDefs.put("Paper",  packageName + ".util.BukkitRewardObject");
        rewardObjDefs.put("Folia",  packageName + ".util.BukkitRewardObject");

        // same constructor call, ignoring the fact that we have P,I
        Object instance = getInstance(
                rewardObjDefs,
                new Class<?>[] { RewardObject.RewardType.class },
                type
        );
        if (instance == null) return null;

        return (RewardObject<P, I>) instance;
    }

    private Object getInstance(HashMap<String, String> platformAndClass) {
        return getInstance(platformAndClass, null);
    }

    private Object getInstance(HashMap<String, String> platformAndClass, Class<?>... parameterTypes) {
        try {
            for (Map.Entry<String, String> entry : platformAndClass.entrySet()) {
                // Return a class with parameters in constructor
                if (parameterTypes != null && entry.getKey().equals(currPlatform)) {
                    Class clazz = Class.forName(entry.getValue());
                    Constructor<?> constClazz = clazz.getConstructor(parameterTypes);
                    return constClazz.newInstance();
                    // Return a class with no parameters in constructor (parameterTypes == null)
                } else if (entry.getKey().equals(currPlatform)) {
                    Class clazz = Class.forName(entry.getValue());
                    Constructor<?> constClazz = clazz.getConstructor();
                    return constClazz.newInstance();
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            e.printStackTrace();
        }

        // Shouldn't ever get here, 99% likely to be FATAL
        System.out.println("Failed to initialize definition from ServerAdapterFactory!");
        System.out.println("Please contact the developer if your server platform is supported!");
        return null;
    }

    private Object getInstance(
            Map<String, String> platformAndClass,
            Class<?>[] parameterTypes,
            Object... constructorArgs
    ) {
        try {
            for (Map.Entry<String, String> entry : platformAndClass.entrySet()) {
                if (entry.getKey().equals(currPlatform)) {
                    Class<?> clazz = Class.forName(entry.getValue());
                    if (parameterTypes != null && constructorArgs != null) {
                        Constructor<?> constClazz = clazz.getConstructor(parameterTypes);
                        return constClazz.newInstance(constructorArgs);
                    } else {
                        // fallback to no-arg
                        Constructor<?> constClazz = clazz.getConstructor();
                        return constClazz.newInstance();
                    }
                }
            }
        } catch (ClassNotFoundException
                 | NoSuchMethodException
                 | InstantiationException
                 | IllegalAccessException
                 | InvocationTargetException e) {
            e.printStackTrace();
        }

        System.out.println("Failed to initialize definition from AdapterFactory!");
        return null;
    }

}

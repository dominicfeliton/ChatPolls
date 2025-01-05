package com.dominicfeliton.chatpolls;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ChatPolls extends JavaPlugin {

    public static final int bStatsID = 00000;

    public static final String messagesConfigVersion = "010524-1";

    public static ChatPolls instance;

    private BukkitAudiences adventure;

    private TextComponent pluginPrefix = Component.text().content("[").color(NamedTextColor.DARK_RED)
            .append(Component.text().content("WWC").color(NamedTextColor.BLUE).decoration(TextDecoration.BOLD, true))
            .append(Component.text().content("]").color(NamedTextColor.DARK_RED))
            .build();

    private int updateCheckerDelay = 86400;

    private int syncUserDataDelay = 7200;

    private boolean enableSounds = true;

    public @NotNull BukkitAudiences adventure() {
        if (adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return adventure;
    }

    @Override
    public void onEnable() {
        instance = this;

        // We made it!
        //getLogger().info(refs.getPlainMsg("wwcEnabled",
        //        "&6" + getPluginVersion(),
        //        "&a"));
    }

    @Override
    public void onDisable() {
        instance = null;
    }
}
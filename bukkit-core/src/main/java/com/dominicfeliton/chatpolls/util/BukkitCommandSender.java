package com.dominicfeliton.chatpolls.util;

import org.bukkit.command.CommandSender;

/**
 * Adapts Bukkit's CommandSender to our platform-agnostic CommandSender interface.
 */
public class BukkitCommandSender implements GenericCommandSender {
    private final CommandSender bukkitSender;

    public BukkitCommandSender(CommandSender bukkitSender) {
        this.bukkitSender = bukkitSender;
    }

    /**
     * Gets the underlying Bukkit CommandSender.
     * @return The Bukkit CommandSender
     */
    public CommandSender getBukkitSender() {
        return bukkitSender;
    }
}

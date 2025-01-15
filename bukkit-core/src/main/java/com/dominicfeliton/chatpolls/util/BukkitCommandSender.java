package com.dominicfeliton.chatpolls.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

/**
 * Adapts Bukkit's CommandSender to our platform-agnostic CommandSender interface.
 */
public class BukkitCommandSender implements GenericCommandSender {
    private CommandSender bukkitSender;

    public BukkitCommandSender() {
        setConsoleCommandSender();
    }

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

    @Override
    public void setConsoleCommandSender() {
        bukkitSender = Bukkit.getServer().getConsoleSender();
    }

    @Override
    public String getName() {
        if (bukkitSender instanceof ConsoleCommandSender) return null;
        return bukkitSender.getName();
    }
}

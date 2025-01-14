package com.dominicfeliton.chatpolls.util;

/**
 * Platform-agnostic class for command senders.
 * Each platform implementation (Bukkit, Velocity, etc.) must provide their own implementation.
 */
public interface GenericCommandSender {

    void setConsoleCommandSender();

}

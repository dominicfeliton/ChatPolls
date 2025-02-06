package com.dominicfeliton.chatpolls.util;

import org.bukkit.entity.Player;

/**
 * A concrete "personal poll" class for the Bukkit module.
 * This extends the abstract PollObject from common,
 * providing Bukkit-specific functionality.
 */
public class BukkitPollObject extends PollObject {

    /**
     * Constructor
     *
     * @param title     The poll title
     * @param options   The list of possible choices
     * @param desc      A description of the poll
     * @param delaySec  Start delay in seconds
     * @param durationSec Duration in seconds
     */
    public BukkitPollObject(String title,
                            List<String> options,
                            String desc,
                            long delaySec,
                            long durationSec) {
        super(title, options, desc, delaySec, durationSec);
    }

    // Add any Bukkit-specific methods here if needed
    // For example, methods that work with Bukkit's Player class
}

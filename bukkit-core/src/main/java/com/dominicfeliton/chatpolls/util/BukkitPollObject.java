package com.dominicfeliton.chatpolls.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.bukkit.entity.Player;

import java.util.List;

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
    @JsonCreator
    public BukkitPollObject(@JsonProperty("title") String title,
                           @JsonProperty("options") List<String> options,
                           @JsonProperty("description") String desc,
                           @JsonProperty("delaySeconds") long delaySec,
                           @JsonProperty("durationSeconds") long durationSec) {
        super(title, options, desc, delaySec, durationSec);
    }

    // Add any Bukkit-specific methods here if needed
    // For example, methods that work with Bukkit's Player class
}

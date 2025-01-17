package com.dominicfeliton.chatpolls.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A concrete "personal poll" class for the Bukkit module.
 * This extends the abstract PollObject from common,
 * providing fields and methods to handle poll creation, voting, etc.
 */
public class BukkitPollObject extends PollObject {

    private final String title;
    private final String description;
    private final List<String> options;

    /**
     *  key: option string
     *  val: total vote count
     */
    private final ConcurrentMap<String, Integer> optionVotes = new ConcurrentHashMap<>();

    /**
     *  key: player UUID
     *  val: the option they chose
     */
    private final ConcurrentMap<UUID, String> userVotes = new ConcurrentHashMap<>();

    /**
     * Start and end times for the poll
     */
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

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

        super();  // from PollObject abstract
        this.title = title;
        this.description = desc;

        // defensive copy + unmodifiable
        this.options = Collections.unmodifiableList(new ArrayList<>(options));

        // initialize vote counters
        for (String opt : this.options) {
            optionVotes.put(opt, 0);
        }

        // set start/end
        this.startTime = getCurrentDateTime().plusSeconds(delaySec);
        this.endTime = startTime.plusSeconds(durationSec);
    }

    // ----------------------------------------------------------------------
    //  Concrete methods
    // ----------------------------------------------------------------------

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getOptionsDisplay() {
        return String.join(", ", options);
    }

    public String getDateTimeStart() {
        return startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getDateTimeEnd() {
        return endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public boolean hasStarted() {
        return getCurrentDateTime().isAfter(startTime);
    }

    public boolean hasEnded() {
        return getCurrentDateTime().isAfter(endTime);
    }

    /**
     * Retrieve a map of option => voteCount (unmodifiable).
     */
    public Map<String, Integer> getOptionVotes() {
        return Collections.unmodifiableMap(optionVotes);
    }

    public boolean hasOption(String optionKey) {
        return options.contains(optionKey);
    }

    public boolean hasVoted(UUID playerUuid) {
        return userVotes.containsKey(playerUuid);
    }

    public String getPlayerVote(UUID playerUuid) {
        return userVotes.get(playerUuid);
    }

    /**
     * Attempt to cast a vote for the given player.
     *
     * @param playerUuid The player's UUID
     * @param option The option they're voting for
     * @return true if successful, false otherwise
     */
    public boolean castVote(UUID playerUuid, String option) {
        // Validate
        if (!hasOption(option)) return false;
        if (!hasStarted() || hasEnded()) return false;
        if (hasVoted(playerUuid)) return false;

        synchronized (this) {
            userVotes.put(playerUuid, option);
            int currentCount = optionVotes.getOrDefault(option, 0);
            optionVotes.put(option, currentCount + 1);
        }
        return true;
    }
}

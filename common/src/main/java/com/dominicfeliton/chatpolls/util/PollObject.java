package com.dominicfeliton.chatpolls.util;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

enum PollType {
    SINGLE,      // Single-choice poll
    MULTIPLE,    // Multiple-choice poll
    RANKED;      // Ranked-choice poll
}

public abstract class PollObject {
    // Basic poll properties
    protected final String title;
    protected final String description;
    protected final List<String> options;
    protected final LocalDateTime startTime;
    protected final LocalDateTime endTime;

    // Vote tracking
    protected final ConcurrentMap<String, Integer> optionVotes = new ConcurrentHashMap<>();
    protected final ConcurrentMap<UUID, String> userVotes = new ConcurrentHashMap<>();

    // Advanced settings (from original)
    private boolean active;
    private boolean anonymousVoting;
    private boolean allowMultipleChoices;
    private boolean autoTieBreaker;
    private boolean showResultsDuringPoll;
    private boolean allowVoteUndo;

    private String uuid;
    private String templateUuid; // null if DNE
    private String creatorUuid;
    private PollType pollType; // e.g., "single", "multiple", "ranked"
    private Component beginChatMessage;
    private Component endChatMessage;
    private LocalDateTime creationDate;
    private String cancellationReason;

    private boolean sendExpirationWarning;
    private List<Integer> warningThresholdsInSeconds;

    private Sound onBegin;
    private Sound onVote;
    private Sound onUndoVote;
    private Sound onCancel;
    private Sound onEnd;

    private Set<String> requiredPerms;
    private Set<String> recipients; // UUIDs
    private Set<String> tags;
    private List<RewardObject> rewards;
    private Queue<String> votingLog;

    private Map<Component, String> cosmeticToInternal;
    private Map<String, AtomicInteger> votes;

    private int maxVotesPerUser;

    protected PollObject(String title, List<String> options, String description, long delaySec, long durationSec) {
        // Store basic properties
        this.title = title;
        this.description = description;
        this.options = Collections.unmodifiableList(new ArrayList<>(options));
        
        // Set times
        LocalDateTime now = getCurrentDateTime();
        this.startTime = now.plusSeconds(delaySec);
        this.endTime = startTime.plusSeconds(durationSec);
        
        // Initialize vote tracking
        for (String opt : this.options) {
            optionVotes.put(opt, 0);
        }

        // Initialize other fields
        active = false;
        uuid = UUID.randomUUID().toString();
        pollType = PollType.SINGLE;
        creationDate = now;
        cancellationReason = null;

        sendExpirationWarning = false;
        warningThresholdsInSeconds = new CopyOnWriteArrayList<>();

        onBegin = null;
        onVote = null;
        onUndoVote = null;
        onCancel = null;
        onEnd = null;

        requiredPerms = ConcurrentHashMap.newKeySet();
        recipients = ConcurrentHashMap.newKeySet();
        tags = ConcurrentHashMap.newKeySet();
        rewards = new CopyOnWriteArrayList<>();
        votingLog = new ConcurrentLinkedQueue<>();

        cosmeticToInternal = new ConcurrentHashMap<>();
        votes = new ConcurrentHashMap<>();
    }

    // Common methods that work for all platforms
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

    public String getCreationDate() {
        return creationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public boolean hasStarted() {
        return getCurrentDateTime().isAfter(startTime);
    }

    public boolean hasEnded() {
        return getCurrentDateTime().isAfter(endTime);
    }

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

    /**
     * Retrieves the current date and time.
     *
     * @return The current LocalDateTime.
     */
    public LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }
}

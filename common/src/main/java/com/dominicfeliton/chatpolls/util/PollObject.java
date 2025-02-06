package com.dominicfeliton.chatpolls.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class PollObject {
    @JsonProperty("title")
    // Basic poll properties
    protected final String title;
    @JsonProperty("description")
    protected final String description;
    @JsonProperty("options")
    protected final List<String> options;
    @JsonProperty("startTime")
    protected final LocalDateTime startTime;
    @JsonProperty("endTime")
    protected final LocalDateTime endTime;

    // Vote tracking
    @JsonProperty("optionVotes")
    protected final ConcurrentMap<String, Integer> optionVotes = new ConcurrentHashMap<>();
    
    @JsonProperty("userVotes")
    protected final ConcurrentMap<UUID, String> userVotes = new ConcurrentHashMap<>();
    
    @JsonProperty("userRankedVotes")
    protected final ConcurrentMap<UUID, List<String>> userRankedVotes = new ConcurrentHashMap<>();

    // Advanced settings (from original)
    @JsonProperty("active")
    private boolean active;
    @JsonProperty("anonymousVoting")
    private boolean anonymousVoting;
    @JsonProperty("allowMultipleChoices")
    private boolean allowMultipleChoices;
    @JsonProperty("autoTieBreaker")
    private boolean autoTieBreaker;
    @JsonProperty("showResultsDuringPoll")
    private boolean showResultsDuringPoll;
    @JsonProperty("allowVoteUndo")
    private boolean allowVoteUndo;

    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("templateUuid")
    private String templateUuid;
    @JsonProperty("creatorUuid")
    private String creatorUuid;
    @JsonProperty("pollType")
    private PollType pollType;
    
    @JsonIgnore // Complex type, handle separately if needed
    private Component beginChatMessage;
    @JsonIgnore // Complex type, handle separately if needed
    private Component endChatMessage;
    
    @JsonProperty("creationDate")
    private LocalDateTime creationDate;
    private String cancellationReason;

    @JsonProperty("sendExpirationWarning")
    private boolean sendExpirationWarning;
    @JsonProperty("warningThresholdsInSeconds")
    private List<Integer> warningThresholdsInSeconds;

    @JsonIgnore // Complex type, handle separately if needed
    private Sound onBegin;
    @JsonIgnore
    private Sound onVote;
    @JsonIgnore
    private Sound onUndoVote;
    @JsonIgnore
    private Sound onCancel;
    @JsonIgnore
    private Sound onEnd;

    @JsonProperty("requiredPerms")
    private Set<String> requiredPerms;
    @JsonProperty("recipients")
    private Set<String> recipients;
    @JsonProperty("tags")
    private Set<String> tags;
    @JsonProperty("rewards")
    private List<RewardObject> rewards;
    @JsonProperty("votingLog")
    private Queue<String> votingLog;

    @JsonIgnore // Complex key type (Component)
    private Map<Component, String> cosmeticToInternal;
    @JsonProperty("votes")
    private Map<String, AtomicInteger> votes;

    @JsonProperty("maxVotesPerUser")
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

    @JsonIgnore
    public String getOptionsDisplay() {
        return String.join(", ", options);
    }

    @JsonIgnore
    public String getDateTimeStart() {
        return startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @JsonIgnore
    public String getDateTimeEnd() {
        return endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getCreationDate() {
        return creationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void setCreationDate(LocalDateTime date) {
        this.creationDate = date;
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
        if (pollType == PollType.RANKED) {
            return userRankedVotes.containsKey(playerUuid);
        }
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
        if (pollType == PollType.RANKED) return false; // Ranked polls use castRankedVote

        synchronized (this) {
            userVotes.put(playerUuid, option);
            int currentCount = optionVotes.getOrDefault(option, 0);
            optionVotes.put(option, currentCount + 1);
        }
        return true;
    }

    public boolean castRankedVote(UUID voter, List<String> ranking) {
        // Validate
        if (!hasStarted() || hasEnded()) return false;
        if (pollType != PollType.RANKED) return false;
        if (userRankedVotes.containsKey(voter) && !allowVoteUndo) return false;

        // Validate that each option in ranking is allowed and no duplicates
        Set<String> seen = new HashSet<>();
        for (String option : ranking) {
            if (!options.contains(option)) return false;
            if (!seen.add(option)) return false; // Duplicate found
        }

        synchronized (this) {
            userRankedVotes.put(voter, new ArrayList<>(ranking));
        }
        return true;
    }

    public String calculateRankedWinner() {
        if (pollType != PollType.RANKED) return null;

        // Copy all ballots into a mutable list
        List<List<String>> ballots = new ArrayList<>(userRankedVotes.values());
        
        // While there is more than one candidate remaining:
        while (true) {
            // Count first-choice votes:
            Map<String, Integer> firstChoiceCounts = new HashMap<>();
            for (List<String> ballot : ballots) {
                if (!ballot.isEmpty()) {
                    String firstChoice = ballot.get(0);
                    firstChoiceCounts.put(firstChoice, 
                        firstChoiceCounts.getOrDefault(firstChoice, 0) + 1);
                }
            }
            
            int totalVotes = ballots.size();
            // Check for majority:
            for (Map.Entry<String, Integer> entry : firstChoiceCounts.entrySet()) {
                if (entry.getValue() > totalVotes / 2) {
                    return entry.getKey(); // Winner found
                }
            }
            
            // If no votes remain, return null
            if (firstChoiceCounts.isEmpty()) {
                return null;
            }
            
            // Identify candidate(s) with the fewest first-choice votes
            int minVotes = Collections.min(firstChoiceCounts.values());
            List<String> toEliminate = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : firstChoiceCounts.entrySet()) {
                if (entry.getValue() == minVotes) {
                    toEliminate.add(entry.getKey());
                }
            }
            
            // Eliminate the candidate(s) from every ballot
            for (List<String> ballot : ballots) {
                ballot.removeAll(toEliminate);
            }
            
            // If only one candidate remains across all ballots, return it
            Set<String> remaining = new HashSet<>();
            for (List<String> ballot : ballots) {
                if (!ballot.isEmpty()) {
                    remaining.add(ballot.get(0));
                }
            }
            if (remaining.size() == 1) {
                return remaining.iterator().next();
            }
        }
    }

    /**
     * Retrieves the current date and time.
     *
     * @return The current LocalDateTime.
     */
    @JsonIgnore
    public LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    public void setPollType(PollType type) {
        this.pollType = type;
    }

    public PollType getPollType() {
        return this.pollType;
    }

    public List<String> getRankedVotes(UUID playerUuid) {
        return userRankedVotes.get(playerUuid);
    }
}

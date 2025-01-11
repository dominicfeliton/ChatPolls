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
    private Component title;
    private Component description;
    private Component beginChatMessage;
    private Component endChatMessage;
    private LocalDateTime creationDate;
    private LocalDateTime dateTimeStart; // accounts for delays etc.
    private LocalDateTime dateTimeEnd; // will be adjusted if cancelled prematurely (active=false)
    private String cancellationReason;

    private boolean sendExpirationWarning;
    private List<Integer> warningThresholdsInSeconds;

    //private SmartInventory gui;
    private Sound onBegin;
    private Sound onVote;
    private Sound onUndoVote;
    private Sound onCancel;
    private Sound onEnd;

    private Set<String> requiredPerms;
    private Set<String> recipients; // UUIDs
    private Set<String> tags;
    private List<RewardObject> rewards;
    private Queue<String> votingLog; // User X performed ACTION !

    private Map<Component, String> cosmeticToInternal; // Cosmetic Name -> Internal Identifier
    private Map<String, AtomicInteger> votes; // Internal Identifier -> Number of Votes

    private int maxVotesPerUser;

    private void initPollObj(String templateUuid, String creatorUuid) {
        active = false;

        uuid = UUID.randomUUID().toString();
        this.templateUuid = templateUuid;
        this.creatorUuid = creatorUuid;
        pollType = PollType.SINGLE;
        creationDate = getCurrentDateTime();
        dateTimeStart = getCurrentDateTime();
        dateTimeEnd = getCurrentDateTime();
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

    public PollObject() {
        // New Poll, No Template, No Creator (Console), Very Simple
        initPollObj(null, null);
    }

    public String getCreationDate() {
       return creationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getDateTimeStart() {
        return dateTimeStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getDateTimeEnd() {
        return dateTimeEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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

package com.dominicfeliton.chatpolls.commands;

import com.dominicfeliton.chatpolls.ChatPolls;
import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericCommandSender;
import com.dominicfeliton.chatpolls.util.PollObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class CHPPersonalBukkit extends BasicCommand implements TabCompleter {

    private static final long DEFAULT_DURATION = 24 * 60 * 60; // 24 hours in seconds
    
    private final ChatPolls main;
    private final CommonRefs refs;

    public CHPPersonalBukkit(GenericCommandSender sender, String label, String[] args, CommonRefs refs) {
        super(sender, label, args, refs);
        this.main = ChatPolls.instance;
        this.refs = refs;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();

        if (args.length == 1) {
            return Arrays.asList("create", "list", "delete", "vote").stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "vote":
                case "delete":
                    Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
                    if (userPolls != null) {
                        return userPolls.keySet().stream()
                                .filter(id -> id.startsWith(args[1]))
                                .collect(Collectors.toList());
                    }
                    break;
            }
        }

        if (args.length == 3 && args[0].toLowerCase().equals("vote")) {
            Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
            if (userPolls != null && userPolls.containsKey(args[1])) {
                PollObject poll = userPolls.get(args[1]);
                if (poll instanceof PersonalPollObject) {
                    PersonalPollObject personalPoll = (PersonalPollObject) poll;
                    return personalPoll.getOptions().stream()
                            .filter(opt -> opt.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }

        return Collections.emptyList();
    }

    @Override
    public boolean processCommand() {
        if (args.length == 0) {
            refs.sendMsg("chppUsage", sender);
            return true;
        }

        if (sender.getName() == null) {
            refs.sendMsg("chppConsoleDenied", sender);
            return true;
        }

        Player bukkitPlayer = Bukkit.getPlayerExact(sender.getName());
        if (bukkitPlayer == null) {
            refs.sendMsg("chppPlayerNotFound", sender);
            return true;
        }

        UUID playerUuid = bukkitPlayer.getUniqueId();

        switch (args[0].toLowerCase()) {
            case "create":
                return handleCreate(playerUuid);
            case "list":
                return handleList(playerUuid);
            case "delete":
                return handleDelete(playerUuid);
            case "vote":
                return handleVote(playerUuid);
            default:
                refs.sendMsg("chppUsage", sender);
                return true;
        }
    }

    private String stripQuotes(String str) {
        if (str == null || str.length() < 2) return str;
        if (str.charAt(0) == '"' && str.charAt(str.length() - 1) == '"') {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private long parseTimeString(String timeStr) {
        // Basic validation
        if (timeStr == null || timeStr.length() < 2) return -1;

        // Get unit from last character
        char unitChar = timeStr.charAt(timeStr.length() - 1);
        String unit = String.valueOf(unitChar).toLowerCase();

        // Get number part
        String numberPart = timeStr.substring(0, timeStr.length() - 1);
        
        // Try to parse number
        long value;
        try {
            value = Long.parseLong(numberPart);
        } catch (NumberFormatException e) {
            return -1;
        }

        // Must be positive
        if (value <= 0) return -1;

        // Convert to seconds based on unit
        switch (unit) {
            case "s": return value;
            case "m": return value * 60;
            case "h": return value * 60 * 60;
            case "d": return value * 24 * 60 * 60;
            default: return -1;
        }
    }

    private boolean handleCreate(UUID playerUuid) {
        if (args.length < 3) {
            refs.sendMsg("chppCreateUsage", sender);
            return true;
        }

        // Join remaining args to handle quoted strings properly
        String fullCommand = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        // Parse quoted strings
        for (int i = 0; i < fullCommand.length(); i++) {
            char c = fullCommand.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }

        // Need at least title and options
        if (parts.size() < 2) {
            refs.sendMsg("chppCreateUsage", sender);
            return true;
        }

        // Get title and options
        String pollTitle = stripQuotes(parts.get(0));
        String optionsStr = stripQuotes(parts.get(1));
        
        // Parse options first to validate
        List<String> options = Arrays.stream(optionsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // Validate options count
        if (options.size() < 2) {
            refs.sendMsg("chppTooFewOptions", sender);
            return true;
        }
        if (options.size() > 5) {
            refs.sendMsg("chppTooManyOptions", sender);
            return true;
        }

        // Parse delay, duration, and description
        long delaySeconds = 0;
        long durationSeconds = DEFAULT_DURATION;
        String pollDescription = "";
        
        if (parts.size() > 2) {
            String timeArg = stripQuotes(parts.get(2));
            delaySeconds = parseTimeString(timeArg);
            
            // If delay is valid and we have more parts
            if (delaySeconds >= 0 && parts.size() > 3) {
                String durationArg = stripQuotes(parts.get(3));
                durationSeconds = parseTimeString(durationArg);
                
                // If duration is valid and we have more parts
                if (durationSeconds >= 0 && parts.size() > 4) {
                    pollDescription = String.join(" ", parts.subList(4, parts.size()));
                    pollDescription = stripQuotes(pollDescription);
                } else if (durationSeconds < 0) {
                    // Invalid duration, treat rest as description
                    pollDescription = String.join(" ", parts.subList(3, parts.size()));
                    pollDescription = stripQuotes(pollDescription);
                    durationSeconds = DEFAULT_DURATION;
                }
            } else if (delaySeconds < 0) {
                // Invalid delay, treat rest as description
                pollDescription = String.join(" ", parts.subList(2, parts.size()));
                pollDescription = stripQuotes(pollDescription);
                delaySeconds = 0;
            }
        }

        String pollId = generatePollId();
        PersonalPollObject personalPoll = new PersonalPollObject(pollTitle, options, pollDescription, delaySeconds, durationSeconds);

        main.getPersonalPolls().putIfAbsent(playerUuid, new ConcurrentHashMap<>());
        main.getPersonalPolls().get(playerUuid).put(pollId, personalPoll);

        refs.sendMsg("chppCreateSuccess", 
            new String[]{pollId, pollTitle, personalPoll.getOptionsDisplay(), 
                personalPoll.getDateTimeStart(), personalPoll.getDateTimeEnd(), 
                pollDescription}, 
            "&r&d", sender);
        return true;
    }

    private boolean handleList(UUID playerUuid) {
        Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
        if (userPolls == null || userPolls.isEmpty()) {
            refs.sendMsg("chppListEmpty", sender);
            return true;
        }

        refs.sendMsg("chppListHeader", sender);

        for (Map.Entry<String, PollObject> entry : userPolls.entrySet()) {
            String pollId = entry.getKey();
            PollObject poll = entry.getValue();

            if (!(poll instanceof PersonalPollObject)) continue;
            PersonalPollObject personalPoll = (PersonalPollObject) poll;

            // Build hover text
            StringBuilder hoverText = new StringBuilder();
            hoverText.append(refs.getPlainMsg("chppListHoverPoll", personalPoll.getTitle(), sender)).append("\n");
            if (!personalPoll.getDescription().isEmpty()) {
                hoverText.append(refs.getPlainMsg("chppListHoverDesc", personalPoll.getDescription(), sender)).append("\n");
            }
            hoverText.append(refs.getPlainMsg("chppListHoverOptions", personalPoll.getOptionsDisplay(), sender)).append("\n\n");
            
            String playerVote = personalPoll.getPlayerVote(playerUuid);
            if (playerVote != null) {
                hoverText.append(refs.getPlainMsg("chppListHoverVoted", playerVote, sender)).append("\n");
            }
            
            Map<String, Integer> votes = personalPoll.getOptionVotes();
            hoverText.append(refs.getPlainMsg("chppListHoverVotes", sender)).append("\n");
            for (Map.Entry<String, Integer> vote : votes.entrySet()) {
                hoverText.append(refs.getPlainMsg("chppListHoverVoteLine", 
                    new String[]{vote.getKey(), String.valueOf(vote.getValue())}, "&r&d", sender)).append("\n");
            }
            
            hoverText.append("\n").append(refs.getPlainMsg("chppListHoverVoteAction", sender));

            // Create clickable components
            Component voteButton = Component.text()
                .append(Component.text("[" + pollId + "] ", NamedTextColor.AQUA))
                .append(Component.text(personalPoll.getTitle(), NamedTextColor.WHITE))
                .append(playerVote != null 
                    ? Component.text(" ✓", NamedTextColor.GREEN)
                    : Component.empty())
                .clickEvent(ClickEvent.suggestCommand("/chpp vote " + pollId + " "))
                .hoverEvent(HoverEvent.showText(Component.text(hoverText.toString())))
                .build();

            Component deleteButton = Component.text()
                .append(Component.text(" [✖]", NamedTextColor.RED))
                .clickEvent(ClickEvent.suggestCommand("/chpp delete " + pollId))
                .hoverEvent(HoverEvent.showText(Component.text(refs.getPlainMsg("chppListHoverDeleteAction", sender))))
                .build();

            Component message = Component.empty()
                .append(voteButton)
                .append(deleteButton)
                .append(Component.text(refs.getPlainMsg("chppListActions", sender), NamedTextColor.GRAY));

            refs.sendMsg(sender, message, true);
        }
        return true;
    }

    private boolean handleDelete(UUID playerUuid) {
        if (args.length < 2) {
            refs.sendMsg("chppDeleteUsage", sender);
            return true;
        }
        String pollId = args[1];

        Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
        if (userPolls == null || !userPolls.containsKey(pollId)) {
            refs.sendMsg("chppDeleteNotFound", new String[]{pollId}, "&r&d", sender);
            return true;
        }

        userPolls.remove(pollId);
        refs.sendMsg("chppDeleteSuccess", pollId, sender);
        return true;
    }

    private String generatePollId() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    private boolean handleVote(UUID playerUuid) {
        if (args.length < 2) {
            refs.sendMsg("chppVoteUsage", sender);
            return true;
        }

        String pollId = args[1];

        Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
        if (userPolls == null || !userPolls.containsKey(pollId)) {
            refs.sendMsg("chppVoteNotFound", new String[]{pollId}, "&r&d", sender);
            return true;
        }

        PollObject poll = userPolls.get(pollId);
        if (!(poll instanceof PersonalPollObject)) {
            refs.debugMsg("Poll was not a PersonalPollObject? Strange.");
            refs.sendMsg("chppVoteFail", sender);
            return true;
        }

        PersonalPollObject personalPoll = (PersonalPollObject) poll;

        // Check if poll has started
        if (!personalPoll.hasStarted()) {
            refs.sendMsg("chppPollNotStarted", personalPoll.getDateTimeStart(), "&r&d", sender);
            return true;
        }

        // Check if poll has ended
        if (personalPoll.hasEnded()) {
            refs.sendMsg("chppPollEnded", personalPoll.getDateTimeEnd(), "&r&d", sender);
            return true;
        }

        // If no option provided, show poll info
        if (args.length == 2) {
            // Show poll info and options
            refs.sendMsg("chppVoteInfo", 
                new String[]{personalPoll.getTitle(), personalPoll.getDescription(), 
                    personalPoll.getDateTimeStart(), personalPoll.getDateTimeEnd()}, 
                "&r&d", sender);

            String playerVote = personalPoll.getPlayerVote(playerUuid);
            if (playerVote != null) {
                refs.sendMsg("chppVoteInfoVoted", playerVote, "&r&d", sender);
            } else {
                // Show available options
                for (String option : personalPoll.getOptions()) {
                    refs.sendMsg("chppVoteInfoOption", option, "&r&d", sender);
                }
            }
            return true;
        }

        String option = args[2];

        // Check if option is valid
        if (!personalPoll.hasOption(option)) {
            refs.sendMsg("chppInvalidOption", sender);
            return true;
        }

        // Check if player has already voted
        if (personalPoll.hasVoted(playerUuid)) {
            refs.sendMsg("chppAlreadyVoted", new String[]{personalPoll.getPlayerVote(playerUuid)}, "&r&d", sender);
            return true;
        }

        // Cast vote
        if (!personalPoll.castVote(playerUuid, option)) {
            refs.sendMsg("chppVoteFail", sender);
            return true;
        }

        // Show success message
        refs.sendMsg("chppVoteSuccess", new String[]{option, pollId}, "&r&d", sender);

        // Show vote tally
        Map<String, Integer> updatedVotes = personalPoll.getOptionVotes();
        for (Map.Entry<String, Integer> entry : updatedVotes.entrySet()) {
            refs.sendMsg("chppVoteTallyLine", 
                new String[]{entry.getKey(), String.valueOf(entry.getValue())}, 
                "&r&d", sender
            );
        }

        return true;
    }

    private static class PersonalPollObject extends PollObject {
        private final String title;
        private final String description;
        private final Map<String, Integer> optionVotes = new ConcurrentHashMap<>();
        private final Map<UUID, String> userVotes = new ConcurrentHashMap<>();
        private final List<String> options;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        public PersonalPollObject(String title, List<String> options, String description, long delaySeconds, long durationSeconds) {
            super();
            this.title = title;
            this.options = Collections.unmodifiableList(new ArrayList<>(options));
            this.description = description;
            
            // Set start and end times
            this.startTime = getCurrentDateTime().plusSeconds(delaySeconds);
            this.endTime = startTime.plusSeconds(durationSeconds);
            
            // Initialize vote counts for each option
            for (String option : options) {
                optionVotes.put(option, 0);
            }
        }

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
            // Validate option
            if (!options.contains(option)) {
                return false;
            }

            // Check if poll is active
            if (!hasStarted() || hasEnded()) {
                return false;
            }

            // Check if player has already voted
            if (hasVoted(playerUuid)) {
                return false;
            }

            synchronized (this) {
                // Record the player's vote
                userVotes.put(playerUuid, option);
                
                // Increment vote count for chosen option
                int currentCount = optionVotes.getOrDefault(option, 0);
                optionVotes.put(option, currentCount + 1);
            }
            return true;
        }
    }
}

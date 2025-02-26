package com.dominicfeliton.chatpolls.commands;

import com.dominicfeliton.chatpolls.ChatPolls;
import com.dominicfeliton.chatpolls.commands.BasicCommand;
import com.dominicfeliton.chatpolls.util.BukkitPollObject;
import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericCommandSender;
import com.dominicfeliton.chatpolls.util.BukkitCommandSender;
import com.dominicfeliton.chatpolls.util.PollObject;
import com.dominicfeliton.chatpolls.util.PollType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A command for creating, listing, deleting, and voting on personal polls (here named "BukkitPollObject").
 */
public class CHPPersonalBukkit extends BasicCommand implements TabCompleter {

    private static final long DEFAULT_DURATION = 24 * 60 * 60; // 24 hours in seconds

    private final ChatPolls main;
    private final CommonRefs refs;

    public CHPPersonalBukkit(GenericCommandSender sender, String label, String[] args, CommonRefs refs) {
        super(sender, label, args, refs);
        this.main = ChatPolls.instance;
        this.refs = refs;
    }

    /**
     * Tab completion logic
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();

        if (args.length == 1) {
            return Arrays.asList("create", "list", "delete", "vote", "save", "end").stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "vote":
                case "delete":
                case "end":
                    Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
                    if (userPolls != null) {
                        return userPolls.keySet().stream()
                                .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    break;
            }
        }

        // Option suggestions for vote command
        if (args.length == 3 && args[0].equalsIgnoreCase("vote")) {
            Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
            if (userPolls != null && userPolls.containsKey(args[1])) {
                PollObject poll = userPolls.get(args[1]);
                // Check if poll is a BukkitPollObject
                if (poll instanceof BukkitPollObject) {
                    BukkitPollObject bukkitPoll = (BukkitPollObject) poll;
                    return bukkitPoll.getOptions().stream()
                            .filter(opt -> opt.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Entry point for /chp or /chpp command
     */
    @Override
    public boolean processCommand() {
        if (args.length == 0) {
            // Reuse message key but change the displayed command
            String usageMsg = refs.getPlainMsg("chppUsage", sender)
                    .replace("/chpp", "/chp");
            refs.sendMsg(sender, usageMsg, false);
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
            case "save":
                return handleSave(playerUuid);
            case "end":
                return handleEnd(playerUuid);
            default:
                refs.sendMsg("chppUsage", sender);
                return true;
        }
    }

    // ----------------------------------------------------------------------
    //  CREATE
    // ----------------------------------------------------------------------

    private boolean handleCreate(UUID playerUuid) {
        if (args.length < 2) {
            String usageMsg = refs.getPlainMsg("chppCreateUsage", sender)
                    .replace("/chpp", "/chp");
            refs.sendMsg(sender, usageMsg, false);
            return true;
        }

        // parse arguments from index=1 onward
        List<String> parsedArgs = parseQuotedArgs(args, 1);
        if (parsedArgs.isEmpty()) {
            String usageMsg = refs.getPlainMsg("chppCreateUsage", sender)
                    .replace("/chpp", "/chp");
            refs.sendMsg(sender, usageMsg, false);
            return true;
        }

        // Must have [title, options]
        if (parsedArgs.size() < 2) {
            String usageMsg = refs.getPlainMsg("chppCreateUsage", sender)
                    .replace("/chpp", "/chp");
            refs.sendMsg(sender, usageMsg, false);
            return true;
        }

        String pollTitle = parsedArgs.get(0).trim();
        String optionsStr = parsedArgs.get(1).trim();

        List<String> options = Arrays.stream(optionsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (options.size() < 2) {
            refs.sendMsg("chppTooFewOptions", sender);
            return true;
        }
        if (options.size() > 5) {
            refs.sendMsg("chppTooManyOptions", sender);
            return true;
        }

        long delaySeconds = 0;
        long durationSeconds = DEFAULT_DURATION;
        String pollDescription = "";

        // parse optional 3rd => delay
        if (parsedArgs.size() >= 3) {
            long maybeDelay = parseTimeString(parsedArgs.get(2));
            if (maybeDelay >= 0) {
                delaySeconds = maybeDelay;
            } else {
                pollDescription = buildRemainingDescription(parsedArgs, 2);
                maybeDelay = -1;
            }

            // parse optional 4th => duration
            if (maybeDelay >= 0 && parsedArgs.size() >= 4) {
                long maybeDuration = parseTimeString(parsedArgs.get(3));
                if (maybeDuration >= 0) {
                    durationSeconds = maybeDuration;
                } else {
                    pollDescription = buildRemainingDescription(parsedArgs, 3);
                    maybeDuration = -1;
                }
                // parse optional 5th => rest as description
                if (maybeDuration >= 0 && parsedArgs.size() > 4) {
                    pollDescription = buildRemainingDescription(parsedArgs, 4);
                }
            }
        }

        String pollId = generatePollId();
        // Build a BukkitPollObject
        BukkitPollObject bukkitPoll = new BukkitPollObject(
                pollTitle,
                options,
                pollDescription,
                delaySeconds,
                durationSeconds
        );
        
        // Check if this should be a ranked poll (if title starts with [RANKED])
        //TODO: Make poll type just a param
        if (pollTitle.toUpperCase().startsWith("[RANKED]")) {
            bukkitPoll.setPollType(PollType.RANKED);
            pollTitle = pollTitle.substring(8).trim(); // Remove [RANKED] prefix
        }

        main.getPersonalPolls().putIfAbsent(playerUuid, new ConcurrentHashMap<>());
        main.getPersonalPolls().get(playerUuid).put(pollId, bukkitPoll);

        // placeholders => [pollId, pollTitle, optionsDisplay, dtStart, dtEnd, description]
        refs.sendMsg("chppCreateSuccess",
                new String[]{
                        pollId,
                        pollTitle,
                        bukkitPoll.getOptionsDisplay(),
                        bukkitPoll.getDateTimeStart(),
                        bukkitPoll.getDateTimeEnd(),
                        pollDescription
                },
                "&r&d",
                sender
        );
        return true;
    }

    private String buildRemainingDescription(List<String> parts, int startIndex) {
        if (startIndex >= parts.size()) {
            return "";
        }
        List<String> sub = parts.subList(startIndex, parts.size());
        return String.join(" ", sub).trim();
    }

    /**
     * Parse arguments that may contain quotes. Returns empty list if unclosed quotes or no data.
     */
    private List<String> parseQuotedArgs(String[] args, int offset) {
        if (offset >= args.length) return Collections.emptyList();
        StringBuilder joined = new StringBuilder();
        for (int i = offset; i < args.length; i++) {
            joined.append(args[i]);
            if (i < args.length - 1) {
                joined.append(" ");
            }
        }

        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < joined.length(); i++) {
            char c = joined.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            refs.debugMsg("Unclosed quotes in arguments");
            return Collections.emptyList();
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private String generatePollId() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    private long parseTimeString(String timeStr) {
        if (timeStr == null) return -1;
        timeStr = timeStr.trim().toLowerCase();
        if (timeStr.isEmpty()) return -1;
        if (timeStr.equals("0")) return 0; // immediate start

        Pattern p = Pattern.compile("^([0-9]+)([smhd])$");
        Matcher m = p.matcher(timeStr);
        if (m.matches()) {
            long value = Long.parseLong(m.group(1));
            if (value <= 0) return -1;
            char unit = m.group(2).charAt(0);
            switch (unit) {
                case 's': return value;
                case 'm': return value * 60;
                case 'h': return value * 3600;
                case 'd': return value * 86400;
            }
        }
        return -1;
    }

    // ----------------------------------------------------------------------
    //  LIST
    // ----------------------------------------------------------------------

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
            if (!(poll instanceof BukkitPollObject)) {
                continue;
            }
            BukkitPollObject bukkitPoll = (BukkitPollObject) poll;

            // Build hover text
            StringBuilder hoverText = new StringBuilder();
            hoverText.append(refs.getPlainMsg("chppListHoverPoll", bukkitPoll.getTitle(), sender)).append("\n");
            if (!bukkitPoll.getDescription().isEmpty()) {
                hoverText.append(refs.getPlainMsg("chppListHoverDesc", bukkitPoll.getDescription(), sender)).append("\n");
            }
            hoverText.append(refs.getPlainMsg("chppListHoverOptions", bukkitPoll.getOptionsDisplay(), sender))
                     .append("\n\n");

            if (bukkitPoll.getPollType() == PollType.RANKED) {
                // Show user's ranking if they voted
                List<String> ranking = bukkitPoll.getRankedVotes(playerUuid);
                if (ranking != null) {
                    hoverText.append(refs.getPlainMsg("chppListHoverRankedVoted", 
                        String.join(" > ", ranking), sender)).append("\n");
                }
                // Show current winner
                String winner = bukkitPoll.calculateRankedWinner();
                if (winner != null) {
                    hoverText.append(refs.getPlainMsg("chppRankedWinner", winner, sender)).append("\n");
                }
            } else {
                // Regular poll - show user's vote and vote counts
                String playerVote = bukkitPoll.getPlayerVote(playerUuid);
                if (playerVote != null) {
                    hoverText.append(refs.getPlainMsg("chppListHoverVoted", playerVote, sender)).append("\n");
                }
                hoverText.append(refs.getPlainMsg("chppListHoverVotes", sender)).append("\n");
                Map<String, Integer> voteMap = bukkitPoll.getOptionVotes();
                for (Map.Entry<String, Integer> v : voteMap.entrySet()) {
                    hoverText.append(
                            refs.getPlainMsg(
                                    "chppListHoverVoteLine",
                                    new String[]{v.getKey(), String.valueOf(v.getValue())},
                                    "&r&d",
                                    sender
                            )
                    ).append("\n");
                }
            }
            hoverText.append("\n").append(refs.getPlainMsg("chppListHoverVoteAction", sender));

            // Clickable text => "[123456] Title"
            Component voteButton = Component.text()
                    .append(Component.text("[" + pollId + "] ", NamedTextColor.AQUA))
                    .append(Component.text(bukkitPoll.getTitle(), NamedTextColor.WHITE))
                    .append(
                            bukkitPoll.hasVoted(playerUuid)
                            ? Component.text(" ✓", NamedTextColor.GREEN)
                            : Component.empty()
                    )
                    .clickEvent(ClickEvent.suggestCommand("/chp vote " + pollId + " "))
                    .hoverEvent(HoverEvent.showText(Component.text(hoverText.toString().replace("/chpp", "/chp"))))
                    .build();

            Component deleteButton = Component.text()
                    .append(Component.text(" [✖]", NamedTextColor.RED, TextDecoration.BOLD))
                    .clickEvent(ClickEvent.suggestCommand("/chp delete " + pollId))
                    .hoverEvent(HoverEvent.showText(Component.text(refs.getPlainMsg("chppListHoverDeleteAction", sender))))
                    .build();

            Component msgLine = Component.empty()
                    .append(voteButton)
                    .append(deleteButton)
                    .append(Component.text(" "))
                    .append(Component.text(refs.getPlainMsg("chppListActions", sender), NamedTextColor.GRAY));

            refs.sendMsg(sender, msgLine, true);
        }
        return true;
    }

    // ----------------------------------------------------------------------
    //  DELETE
    // ----------------------------------------------------------------------

    private boolean handleDelete(UUID playerUuid) {
        if (args.length < 2) {
            String usageMsg = refs.getPlainMsg("chppDeleteUsage", sender)
                    .replace("/chpp", "/chp");
            refs.sendMsg(sender, usageMsg, false);
            return true;
        }
        String pollId = args[1].trim();

        Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
        if (userPolls == null || !userPolls.containsKey(pollId)) {
            refs.sendMsg("chppDeleteNotFound", new String[]{pollId}, "&r&d", sender);
            return true;
        }
        userPolls.remove(pollId);
        refs.sendMsg("chppDeleteSuccess", pollId, sender);
        return true;
    }

    
    // ----------------------------------------------------------------------
    //  SAVE
    // ----------------------------------------------------------------------
    
    private boolean handleSave(UUID playerUuid) {
        if (!((BukkitCommandSender) sender).getBukkitSender().hasPermission("chatpolls.chp.polls.save")) {
            refs.sendMsg("chpBadPerms", "chatpolls.chp.polls.save", sender);
            return true;
        }

        try {
            main.savePolls();
            refs.sendMsg("chppSaveSuccess", sender);
            return true;
        } catch (Exception e) {
            refs.sendMsg("chppSaveFail", e.getMessage(), sender);
            return false;
        }
    }
    
    // ----------------------------------------------------------------------
    //  VOTE
    // ----------------------------------------------------------------------
    private boolean handleVote(UUID playerUuid) {
        if (args.length < 2) {
            String usageMsg = refs.getPlainMsg("chppVoteUsage", sender)
                    .replace("/chpp", "/chp");
            refs.sendMsg(sender, usageMsg, false);
            return true;
        }
        String pollId = args[1].trim();

        Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
        if (userPolls == null || !userPolls.containsKey(pollId)) {
            refs.sendMsg("chppVoteNotFound", new String[]{pollId}, "&r&d", sender);
            return true;
        }

        PollObject poll = userPolls.get(pollId);
        if (!(poll instanceof BukkitPollObject)) {
            refs.debugMsg("Poll is not a BukkitPollObject? ID=" + pollId);
            refs.sendMsg("chppVoteFail", sender);
            return true;
        }
        BukkitPollObject bukkitPoll = (BukkitPollObject) poll;

        if (!bukkitPoll.hasStarted()) {
            refs.sendMsg("chppPollNotStarted", bukkitPoll.getDateTimeStart(), "&r&d", sender);
            return true;
        }
        if (bukkitPoll.hasEnded()) {
            refs.sendMsg("chppPollEnded", bukkitPoll.getDateTimeEnd(), "&r&d", sender);
            return true;
        }

        // no option => show poll info
        if (args.length == 2) {
            refs.sendMsg("chppVoteInfo",
                    new String[]{
                            bukkitPoll.getTitle(),
                            bukkitPoll.getDescription(),
                            bukkitPoll.getDateTimeStart(),
                            bukkitPoll.getDateTimeEnd()
                    },
                    "&r&d",
                    sender
            );
            if (bukkitPoll.getPollType() == PollType.RANKED) {
                List<String> ranking = bukkitPoll.getRankedVotes(playerUuid);
                if (ranking != null) {
                    refs.sendMsg("chppVoteInfoVoted", String.join(" > ", ranking), "&r&d", sender);
                } else {
                    refs.sendMsg("chppRankedVoteInfo", sender);
                    for (String opt : bukkitPoll.getOptions()) {
                        refs.sendMsg("chppVoteInfoOption", opt, "&r&d", sender);
                    }
                }
            } else {
                String playerVote = bukkitPoll.getPlayerVote(playerUuid);
                if (playerVote != null) {
                    refs.sendMsg("chppVoteInfoVoted", playerVote, "&r&d", sender);
                } else {
                    for (String opt : bukkitPoll.getOptions()) {
                        refs.sendMsg("chppVoteInfoOption", opt, "&r&d", sender);
                    }
                }
            }
            return true;
        }

        // Handle ranked or regular voting
        if (bukkitPoll.getPollType() == PollType.RANKED) {
            // Parse comma-separated ranking
            List<String> ranking = Arrays.stream(args[2].split(","))
                .map(String::trim)
                .collect(Collectors.toList());
                
            // Validate each option
            for (String opt : ranking) {
                if (!bukkitPoll.hasOption(opt)) {
                    refs.sendMsg("chppInvalidOption", sender);
                    return true;
                }
            }

            if (bukkitPoll.hasVoted(playerUuid)) {
                refs.sendMsg("chppAlreadyVoted",
                        new String[]{String.join(" > ", ranking)},
                        "&r&d",
                        sender
                );
                return true;
            }

            if (!bukkitPoll.castRankedVote(playerUuid, ranking)) {
                refs.sendMsg("chppVoteFail", sender);
                return true;
            }

            // Show the user's ranked choices
            refs.sendMsg("chppVoteSuccess", 
                new String[]{String.join(" > ", ranking), pollId}, 
                "&r&d", 
                sender
            );

            // If poll has ended, calculate and show winner
            if (bukkitPoll.hasEnded()) {
                String winner = bukkitPoll.calculateRankedWinner();
                if (winner != null) {
                    refs.sendMsg("chppRankedWinner", 
                        new String[]{winner}, 
                        "&r&d", 
                        sender
                    );
                }
            }
        } else {
            // Regular single-choice voting
            String option = args[2].trim();
            if (!bukkitPoll.hasOption(option)) {
                refs.sendMsg("chppInvalidOption", sender);
                return true;
            }

            if (bukkitPoll.hasVoted(playerUuid)) {
                refs.sendMsg("chppAlreadyVoted",
                        new String[]{bukkitPoll.getPlayerVote(playerUuid)},
                        "&r&d",
                        sender
                );
                return true;
            }

            if (!bukkitPoll.castVote(playerUuid, option)) {
                refs.sendMsg("chppVoteFail", sender);
                return true;
            }

            // success => show tallies
            refs.sendMsg("chppVoteSuccess", new String[]{option, pollId}, "&r&d", sender);
            Map<String, Integer> updatedVotes = bukkitPoll.getOptionVotes();
            for (Map.Entry<String, Integer> e : updatedVotes.entrySet()) {
                refs.sendMsg("chppVoteTallyLine",
                        new String[]{e.getKey(), String.valueOf(e.getValue())},
                        "&r&d",
                        sender
                );
            }
        }
        return true;
    }

    // ----------------------------------------------------------------------
    //  END
    // ----------------------------------------------------------------------
    private boolean handleEnd(UUID playerUuid) {
        if (args.length < 2) {
            String usageMsg = refs.getPlainMsg("chppEndUsage", sender)
                    .replace("/chpp", "/chp");
            refs.sendMsg(sender, usageMsg, false);
            return true;
        }
        String pollId = args[1].trim();

        Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
        if (userPolls == null || !userPolls.containsKey(pollId)) {
            refs.sendMsg("chppEndNotFound", new String[]{pollId}, "&r&d", sender);
            return true;
        }

        PollObject poll = userPolls.get(pollId);
        if (!(poll instanceof BukkitPollObject)) {
            refs.debugMsg("Poll is not a BukkitPollObject? ID=" + pollId);
            return true;
        }
        BukkitPollObject bukkitPoll = (BukkitPollObject) poll;

        if (bukkitPoll.hasEnded()) {
            refs.sendMsg("chppEndAlreadyEnded", sender);
            return true;
        }

        // Force end the poll by setting endTime to now
        bukkitPoll.forceEnd();

        // Show results
        refs.sendMsg("chppEndSuccess", new String[]{pollId}, "&r&d", sender);

        if (bukkitPoll.getPollType() == PollType.RANKED) {
            String winner = bukkitPoll.calculateRankedWinner();
            if (winner != null) {
                refs.sendMsg("chppRankedWinner", winner, "&r&d", sender);
            } else {
                refs.sendMsg("chppEndNoVotes", sender);
            }
        } else {
            Map<String, Integer> finalVotes = bukkitPoll.getOptionVotes();
            if (finalVotes.values().stream().mapToInt(Integer::intValue).sum() == 0) {
                refs.sendMsg("chppEndNoVotes", sender);
            } else {
                for (Map.Entry<String, Integer> e : finalVotes.entrySet()) {
                    refs.sendMsg("chppVoteTallyLine",
                            new String[]{e.getKey(), String.valueOf(e.getValue())},
                            "&r&d",
                            sender
                    );
                }
            }
        }

        // Save the polls
        try {
            main.savePolls();
        } catch (Exception e) {
            refs.debugMsg("Failed to save polls after ending: " + e.getMessage());
        }

        return true;
    }
}

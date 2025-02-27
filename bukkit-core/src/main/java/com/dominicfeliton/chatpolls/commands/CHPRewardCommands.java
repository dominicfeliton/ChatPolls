package com.dominicfeliton.chatpolls.commands;

import com.dominicfeliton.chatpolls.ChatPolls;
import com.dominicfeliton.chatpolls.ChatPollsHelper;
import com.dominicfeliton.chatpolls.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.dominicfeliton.chatpolls.ChatPollsHelper.SchedulerType.SYNC;

/**
 * This class contains the reward command handler methods for CHPPersonalBukkit.
 * These methods should be integrated into the CHPPersonalBukkit class.
 */
public class CHPRewardCommands {

    /**
     * Processes a reward command
     * 
     * @param sender Command sender
     * @param playerUuid UUID of the poll creator
     * @param args Command arguments
     * @return true if successful
     */
    public static boolean handleReward(GenericCommandSender sender, UUID playerUuid, String[] args) {
        ChatPolls main = ChatPolls.instance;
        CommonRefs refs = main.getCommonRefs();

        if (args.length < 3) {
            String usageMsg = refs.getPlainMsg("chppRewardUsage", sender);
            // Convert to Adventure component properly to maintain colors
            Component usageComponent = refs.deserial(usageMsg.replace("/chpp", "/chp"));
            refs.sendMsg(sender, usageComponent, false);
            return true;
        }

        String pollId = args[1].trim();
        String action = args[2].toLowerCase();

        Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
        if (userPolls == null || !userPolls.containsKey(pollId)) {
            refs.sendMsg("chppRewardPollNotFound", new String[]{pollId}, "&r&d", sender);
            return true;
        }

        PollObject poll = userPolls.get(pollId);
        if (!(poll instanceof BukkitPollObject)) {
            refs.debugMsg("Poll is not a BukkitPollObject? ID=" + pollId);
            return true;
        }
        
        BukkitPollObject bukkitPoll = (BukkitPollObject) poll;

        switch (action) {
            case "add":
                return handleAddReward(sender, bukkitPoll, args);
            case "remove":
                return handleRemoveReward(sender, bukkitPoll, args);
            case "list":
                return handleListRewards(sender, bukkitPoll);
            case "distribute":
                return handleDistributeRewards(sender, bukkitPoll);
            case "winners":
                return handleToggleWinnersOnly(sender, bukkitPoll, args);
            default:
                refs.sendMsg("chppRewardInvalidAction", action, "&r&d", sender);
                return true;
        }
    }

    /**
     * Handles adding a reward to a poll
     */
    private static boolean handleAddReward(GenericCommandSender sender, BukkitPollObject poll, String[] args) {
        ChatPolls main = ChatPolls.instance;
        CommonRefs refs = main.getCommonRefs();
        ChatPollsHelper helper = main.getHelper();

        if (args.length < 4) {
            Component usageComponent = refs.deserial(refs.getPlainMsg("chppRewardAddUsage", sender));
            refs.sendMsg(sender, usageComponent, true);
            return true;
        }

        String rewardType = args[3].toLowerCase();
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        
        // Use sync task to safely query player data
        helper.runSync(new GenericRunnable() {
            @Override
            protected void execute() {
                if (sender.getName() == null) {
                    refs.sendMsg("chppPlayerNotFound", sender);
                    result.set(true);
                    return;
                }
                
                Player bukkitPlayer = main.getServer().getPlayerExact(sender.getName());
                if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                    refs.sendMsg("chppPlayerNotFound", sender);
                    result.set(true);
                    return;
                }

                BukkitRewardObject reward = null;
                
                switch (rewardType) {
                    case "item":
                        // Get item from player's hand (must be done on main thread)
                        ItemStack heldItem = bukkitPlayer.getInventory().getItemInMainHand();
                        if (heldItem == null || heldItem.getType() == Material.AIR) {
                            refs.sendMsg("chppRewardNoItem", sender);
                            result.set(true);
                            return;
                        }
                        
                        // Parse amount if provided
                        int amount = heldItem.getAmount();
                        if (args.length > 4) {
                            try {
                                amount = Integer.parseInt(args[4]);
                                if (amount <= 0) throw new NumberFormatException();
                            } catch (NumberFormatException e) {
                                refs.sendMsg("chppRewardInvalidAmount", sender);
                                result.set(true);
                                return;
                            }
                        }
                        
                        // Create a copy of the held item with the specified amount
                        ItemStack rewardItem = heldItem.clone();
                        rewardItem.setAmount(amount);
                        reward = BukkitRewardObject.createItemReward(rewardItem);
                        break;
                        
                    case "xp":
                    case "exp":
                    case "experience":
                        if (args.length < 5) {
                            refs.sendMsg("chppRewardExpUsage", sender);
                            result.set(true);
                            return;
                        }
                        
                        try {
                            int expAmount = Integer.parseInt(args[4]);
                            if (expAmount <= 0) throw new NumberFormatException();
                            reward = BukkitRewardObject.createExperienceReward(expAmount);
                        } catch (NumberFormatException e) {
                            refs.sendMsg("chppRewardInvalidAmount", sender);
                            result.set(true);
                            return;
                        }
                        break;
                        
                    case "command":
                        if (args.length < 5) {
                            refs.sendMsg("chppRewardCommandUsage", sender);
                            result.set(true);
                            return;
                        }
                        
                        // Join the rest of the args as the command
                        StringBuilder commandStr = new StringBuilder();
                        for (int i = 4; i < args.length; i++) {
                            commandStr.append(args[i]).append(" ");
                        }
                        
                        String command = commandStr.toString().trim();
                        boolean asConsole = true; // Default to console for safety
                        
                        reward = BukkitRewardObject.createCommandReward(command, asConsole);
                        
                        // Add common placeholders
                        reward.addPlaceholder("%player%", "Player's name");
                        reward.addPlaceholder("%uuid%", "Player's UUID");
                        break;
                        
                    default:
                        refs.sendMsg("chppRewardInvalidType", rewardType, "&r&d", sender);
                        result.set(true);
                        return;
                }
                
                if (reward != null) {
                    poll.addReward(reward);
                    refs.sendMsg("chppRewardAdded", new String[]{rewardType, poll.getTitle()}, "&r&d", sender);
                    
                    // Save polls to persist the changes
                    try {
                        main.savePolls();
                    } catch (Exception e) {
                        refs.debugMsg("Failed to save polls after adding reward: " + e.getMessage());
                    }
                    
                    result.set(true);
                    return;
                }
                
                result.set(false);
            }
        }, SYNC, null);
        
        return result.get();
    }

    /**
     * Handles removing a reward from a poll
     */
    private static boolean handleRemoveReward(GenericCommandSender sender, BukkitPollObject poll, String[] args) {
        ChatPolls main = ChatPolls.instance;
        CommonRefs refs = main.getCommonRefs();
        
        if (args.length < 4) {
            Component usageComponent = refs.deserial(refs.getPlainMsg("chppRewardRemoveUsage", sender));
            refs.sendMsg(sender, usageComponent, true);
            return true;
        }
        
        try {
            int index = Integer.parseInt(args[3]) - 1; // Convert to 0-based index
            if (poll.removeReward(index)) {
                refs.sendMsg("chppRewardRemoved", new String[]{String.valueOf(index + 1), poll.getTitle()}, "&r&d", sender);
                
                // Save polls to persist the changes
                try {
                    main.savePolls();
                } catch (Exception e) {
                    refs.debugMsg("Failed to save polls after removing reward: " + e.getMessage());
                }
                
                return true;
            } else {
                refs.sendMsg("chppRewardInvalidIndex", String.valueOf(index + 1), "&r&d", sender);
                return true;
            }
        } catch (NumberFormatException e) {
            refs.sendMsg("chppRewardInvalidIndex", args[3], "&r&d", sender);
            return true;
        }
    }

    /**
     * Handles listing rewards for a poll
     */
    private static boolean handleListRewards(GenericCommandSender sender, BukkitPollObject poll) {
        ChatPolls main = ChatPolls.instance;
        CommonRefs refs = main.getCommonRefs();
        
        List<RewardObject<Player, ?>> rewards = poll.getBukkitRewards();
        
        if (rewards.isEmpty()) {
            refs.sendMsg("chppRewardListEmpty", poll.getTitle(), "&r&d", sender);
            return true;
        }
        
        // Use proper component for header to ensure colors are preserved
        Component headerComponent = Component.text()
            .append(Component.text("=== Rewards for Poll: ", NamedTextColor.GOLD))
            .append(Component.text(poll.getTitle(), NamedTextColor.YELLOW))
            .append(Component.text(" (", NamedTextColor.GOLD))
            .append(Component.text(poll.isRewardOnlyWinners() ? "winning voters only" : "all voters", NamedTextColor.GRAY))
            .append(Component.text(") ===", NamedTextColor.GOLD))
            .build();
            
        refs.sendMsg(sender, headerComponent, true);
        
        // Format each reward with proper colors
        String rewardsDisplay = poll.getRewardsDisplay();
        Component rewardsComponent = Component.text(rewardsDisplay, NamedTextColor.WHITE);
        refs.sendMsg(sender, rewardsComponent, true);
        
        return true;
    }

    /**
     * Handles distributing rewards to eligible voters
     */
    private static boolean handleDistributeRewards(GenericCommandSender sender, BukkitPollObject poll) {
        ChatPolls main = ChatPolls.instance;
        CommonRefs refs = main.getCommonRefs();

        if (!poll.hasEnded()) {
            refs.sendMsg("chppRewardDistributePollActive", sender);
            return true;
        }
        
        if (poll.getBukkitRewards().isEmpty()) {
            refs.sendMsg("chppRewardNoRewards", sender);
            return true;
        }
        
        List<String> recipients = poll.distributeRewards();
        
        if (recipients.isEmpty()) {
            refs.sendMsg("chppRewardNoRecipients", sender);
        } else {
            String playerList = String.join(", ", recipients);
            refs.sendMsg("chppRewardDistributed", 
                    new String[]{String.valueOf(recipients.size()), playerList}, 
                    "&r&d", sender);
            
            // Save polls to persist the rewarded users list
            try {
                main.savePolls();
            } catch (Exception e) {
                refs.debugMsg("Failed to save polls after distributing rewards: " + e.getMessage());
            }
        }
        
        return true;
    }

    /**
     * Handles toggling whether rewards go to winners only or all voters
     */
    private static boolean handleToggleWinnersOnly(GenericCommandSender sender, BukkitPollObject poll, String[] args) {
        ChatPolls main = ChatPolls.instance;
        CommonRefs refs = main.getCommonRefs();

        boolean winnersOnly = true; // Default
        
        if (args.length > 3) {
            String setting = args[3].toLowerCase();
            if (setting.equals("false") || setting.equals("no") || setting.equals("all")) {
                winnersOnly = false;
            } else if (setting.equals("true") || setting.equals("yes") || setting.equals("winners")) {
                winnersOnly = true;
            } else {
                refs.sendMsg("chppRewardWinnersUsage", sender);
                return true;
            }
        }
        
        poll.setRewardOnlyWinners(winnersOnly);
        
        if (winnersOnly) {
            refs.sendMsg("chppRewardWinnersOnly", poll.getTitle(), "&r&d", sender);
        } else {
            refs.sendMsg("chppRewardAllVoters", poll.getTitle(), "&r&d", sender);
        }
        
        // Save polls to persist the change
        try {
            main.savePolls();
        } catch (Exception e) {
            refs.debugMsg("Failed to save polls after changing reward distribution setting: " + e.getMessage());
        }
        
        return true;
    }

    /**
     * Tab completer for reward commands
     */
    public static List<String> tabCompleteReward(Player player, String[] args) {
        ChatPolls main = ChatPolls.instance;
        UUID playerUuid = player.getUniqueId();
        
        if (args.length == 2) {
            // Return poll IDs
            Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
            if (userPolls != null) {
                return userPolls.keySet().stream()
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            // Return reward actions
            return Arrays.asList("add", "remove", "list", "distribute", "winners").stream()
                    .filter(action -> action.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 4) {
            // Context-sensitive completion based on action
            String action = args[2].toLowerCase();
            
            if (action.equals("add")) {
                return Arrays.asList("item", "experience", "xp", "command").stream()
                        .filter(type -> type.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (action.equals("remove")) {
                Map<String, PollObject> userPolls = main.getPersonalPolls().get(playerUuid);
                if (userPolls != null && userPolls.containsKey(args[1])) {
                    PollObject poll = userPolls.get(args[1]);
                    if (poll instanceof BukkitPollObject) {
                        BukkitPollObject bukkitPoll = (BukkitPollObject) poll;
                        int rewardCount = bukkitPoll.getBukkitRewards().size();
                        List<String> indices = new ArrayList<>();
                        for (int i = 1; i <= rewardCount; i++) {
                            indices.add(String.valueOf(i));
                        }
                        return indices.stream()
                                .filter(idx -> idx.startsWith(args[3]))
                                .collect(Collectors.toList());
                    }
                }
            } else if (action.equals("winners")) {
                return Arrays.asList("true", "false", "yes", "no", "winners", "all").stream()
                        .filter(opt -> opt.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
}
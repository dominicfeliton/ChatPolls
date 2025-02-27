package com.dominicfeliton.chatpolls.util;

import com.dominicfeliton.chatpolls.ChatPolls;
import com.dominicfeliton.chatpolls.ChatPollsHelper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static com.dominicfeliton.chatpolls.ChatPollsHelper.SchedulerType.SYNC;

/**
 * A concrete "personal poll" class for the Bukkit module.
 * This extends the abstract PollObject from common,
 * providing Bukkit-specific functionality with reward support.
 */
public class BukkitPollObject extends PollObject {

    // Tracks which users have received rewards
    @JsonProperty("rewardedUsers")
    private final Set<UUID> rewardedUsers = new HashSet<>();

    // Flag for rewarding all voters or just the winners
    @JsonProperty("rewardOnlyWinners")
    private boolean rewardOnlyWinners = false;

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

    /**
     * Adds a reward to this poll
     * 
     * @param reward The reward to add
     * @return true if successful
     */
    public boolean addReward(RewardObject<Player, ?> reward) {
        if (rewards == null) {
            rewards = new CopyOnWriteArrayList<>();
        }
        return rewards.add(reward);
    }

    /**
     * Removes a reward from this poll
     * 
     * @param index The index of the reward to remove
     * @return true if successful
     */
    public boolean removeReward(int index) {
        if (rewards == null || index < 0 || index >= rewards.size()) {
            return false;
        }
        rewards.remove(index);
        return true;
    }

    /**
     * Gets all rewards for this poll
     * 
     * @return List of rewards
     */
    @SuppressWarnings("unchecked")
    @JsonIgnore
    public List<RewardObject<Player, ?>> getBukkitRewards() {
        if (rewards == null) {
            return Collections.emptyList();
        }
        // Safe cast because in Bukkit all rewards are BukkitRewardObjects
        return (List<RewardObject<Player, ?>>) (List<?>) rewards;
    }

    /**
     * Sets whether to reward only winners or all voters
     */
    public void setRewardOnlyWinners(boolean rewardOnlyWinners) {
        this.rewardOnlyWinners = rewardOnlyWinners;
    }

    /**
     * Gets the reward winners only flag
     */
    public boolean isRewardOnlyWinners() {
        return rewardOnlyWinners;
    }

    /**
     * Distributes rewards to poll participants safely using the scheduler
     * 
     * @return A list of player names who received rewards
     */
    public List<String> distributeRewards() {
        final ChatPolls main = ChatPolls.instance;
        final CommonRefs refs = main.getCommonRefs();
        final ChatPollsHelper helper = main.getHelper();
        
        if (rewards == null || rewards.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> rewardRecipients = new ArrayList<>();
        
        // Determine eligible players
        final Set<UUID> eligiblePlayers = new HashSet<>();
        
        if (rewardOnlyWinners && getPollType() != PollType.RANKED) {
            // For regular polls, find the winning option
            String winningOption = findWinningOption();
            if (winningOption != null) {
                // Add players who voted for the winning option
                for (Map.Entry<UUID, String> entry : userVotes.entrySet()) {
                    if (entry.getValue().equals(winningOption)) {
                        eligiblePlayers.add(entry.getKey());
                    }
                }
            }
        } else if (rewardOnlyWinners && getPollType() == PollType.RANKED) {
            // For ranked polls, calculate the winner
            String winner = calculateRankedWinner();
            if (winner != null) {
                // Find players who had this option ranked highly
                for (Map.Entry<UUID, List<String>> entry : userRankedVotes.entrySet()) {
                    List<String> ranking = entry.getValue();
                    if (!ranking.isEmpty() && ranking.get(0).equals(winner)) {
                        eligiblePlayers.add(entry.getKey());
                    }
                }
            }
        } else {
            // Reward all voters
            eligiblePlayers.addAll(userVotes.keySet());
            eligiblePlayers.addAll(userRankedVotes.keySet());
        }
        
        // Exclude already rewarded users
        eligiblePlayers.removeAll(rewardedUsers);
        
        // Process rewards synchronously to safely access Bukkit API
        AtomicReference<List<String>> syncRewardRecipients = new AtomicReference<>(new ArrayList<>());
        
        helper.runSync(new GenericRunnable() {
            @Override
            protected void execute() {
                // Process rewards for all eligible players
                List<String> recipients = new ArrayList<>();
                
                for (UUID uuid : eligiblePlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        // Give each reward to the player
                        for (Object rawReward : rewards) {
                            if (rawReward instanceof BukkitRewardObject) {
                                BukkitRewardObject reward = (BukkitRewardObject) rawReward;
                                try {
                                    reward.giveReward(player);
                                } catch (Exception e) {
                                    // Log error but continue processing
                                    refs.debugMsg("Error giving reward to player " + player.getName() + ": " + e.getMessage());
                                }
                            }
                        }
                        
                        // Mark as rewarded and add to recipients list
                        rewardedUsers.add(uuid);
                        recipients.add(player.getName());
                    }
                }
                
                syncRewardRecipients.set(recipients);
            }
        }, SYNC, null);
        
        return syncRewardRecipients.get();
    }
    
    /**
     * Manually mark a player as having received rewards
     */
    public void markRewarded(UUID playerUuid) {
        rewardedUsers.add(playerUuid);
    }
    
    /**
     * Check if a player has been rewarded
     */
    public boolean hasBeenRewarded(UUID playerUuid) {
        return rewardedUsers.contains(playerUuid);
    }
    
    /**
     * Gets a formatted list of rewards for display
     */
    @JsonIgnore
    public String getRewardsDisplay() {
        if (rewards == null || rewards.isEmpty()) {
            return "None";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rewards.size(); i++) {
            Object reward = rewards.get(i);
            if (reward instanceof BukkitRewardObject) {
                BukkitRewardObject bukkitReward = (BukkitRewardObject) reward;
                sb.append(i + 1).append(". ");
                
                switch (bukkitReward.getType()) {
                    case ITEM:
                        if (bukkitReward.getItem() != null) {
                            sb.append(bukkitReward.getItem().getAmount())
                              .append("x ")
                              .append(formatMaterialName(bukkitReward.getItem().getType().name()));
                        } else {
                            sb.append("Unknown Item");
                        }
                        break;
                    case EXPERIENCE:
                        sb.append(bukkitReward.getExpAmt()).append(" XP");
                        break;
                    case CURRENCY:
                        sb.append(bukkitReward.getCurrencyAmt()).append(" coins");
                        break;
                    case COMMAND:
                        sb.append("Command Reward");
                        break;
                }
                
                if (i < rewards.size() - 1) {
                    sb.append(", ");
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Helper method to find the winning option in a regular poll
     */
    private String findWinningOption() {
        String winningOption = null;
        int highestVotes = -1;
        
        for (Map.Entry<String, Integer> entry : optionVotes.entrySet()) {
            if (entry.getValue() > highestVotes) {
                highestVotes = entry.getValue();
                winningOption = entry.getKey();
            }
        }
        
        return winningOption;
    }
    
    /**
     * Helper to format material names for display
     */
    private String formatMaterialName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
}
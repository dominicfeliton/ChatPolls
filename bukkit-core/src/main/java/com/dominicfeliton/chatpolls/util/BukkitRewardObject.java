package com.dominicfeliton.chatpolls.util;

import com.dominicfeliton.chatpolls.ChatPolls;
import com.dominicfeliton.chatpolls.ChatPollsHelper;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dominicfeliton.chatpolls.ChatPollsHelper.SchedulerType.SYNC;

/**
 * Bukkit implementation of RewardObject that handles giving rewards to players.
 */
public class BukkitRewardObject extends RewardObject<Player, ItemStack> {

    /**
     * Constructor creates a reward of the specified type
     */
    public BukkitRewardObject(RewardType type) {
        super(type);
    }

    /**
     * Creates an item reward
     * 
     * @param item The ItemStack to give as a reward
     * @return A configured BukkitRewardObject
     */
    public static BukkitRewardObject createItemReward(ItemStack item) {
        BukkitRewardObject reward = new BukkitRewardObject(RewardType.ITEM);
        reward.setItem(item);
        return reward;
    }

    /**
     * Creates a simple item reward using a material type and amount
     * 
     * @param material The material type
     * @param amount The amount to give
     * @return A configured BukkitRewardObject
     */
    public static BukkitRewardObject createItemReward(Material material, int amount) {
        return createItemReward(new ItemStack(material, amount));
    }

    /**
     * Creates an experience reward
     * 
     * @param expAmount The amount of experience to award
     * @return A configured BukkitRewardObject
     */
    public static BukkitRewardObject createExperienceReward(int expAmount) {
        BukkitRewardObject reward = new BukkitRewardObject(RewardType.EXPERIENCE);
        reward.setExpAmt(expAmount);
        return reward;
    }

    /**
     * Creates a currency reward (requires Vault)
     * 
     * @param amount The amount of currency to award
     * @return A configured BukkitRewardObject
     */
    public static BukkitRewardObject createCurrencyReward(double amount) {
        BukkitRewardObject reward = new BukkitRewardObject(RewardType.CURRENCY);
        reward.setCurrencyAmt(amount);
        return reward;
    }

    /**
     * Creates a command reward
     * 
     * @param commands List of commands to execute
     * @param runAsConsole Whether to run as console (true) or as player (false)
     * @return A configured BukkitRewardObject
     */
    public static BukkitRewardObject createCommandReward(List<String> commands, boolean runAsConsole) {
        BukkitRewardObject reward = new BukkitRewardObject(RewardType.COMMAND);
        reward.setCommands(commands);
        reward.setRunCommandAsConsole(runAsConsole);
        return reward;
    }

    /**
     * Creates a command reward with a single command
     * 
     * @param command The command to execute
     * @param runAsConsole Whether to run as console (true) or as player (false)
     * @return A configured BukkitRewardObject
     */
    public static BukkitRewardObject createCommandReward(String command, boolean runAsConsole) {
        List<String> commands = new ArrayList<>();
        commands.add(command);
        return createCommandReward(commands, runAsConsole);
    }

    @Override
    public void giveReward(Player player) {
        final ChatPolls main = ChatPolls.instance;
        final CommonRefs refs = main.getCommonRefs();
        final ChatPollsHelper helper = main.getHelper();
        
        // Ensure we run Bukkit API operations on the main thread
        helper.runSync(player.isOnline(), new GenericRunnable() {
            @Override
            protected void execute() {
                switch (getType()) {
                    case ITEM:
                        if (getItem() != null && player.isOnline()) {
                            // Check if the player's inventory is full
                            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(getItem().clone());
                            if (!leftovers.isEmpty()) {
                                // Drop items that didn't fit in the inventory
                                for (ItemStack item : leftovers.values()) {
                                    player.getWorld().dropItem(player.getLocation(), item);
                                }
                                refs.sendMsg("chppRewardItemsDropped", player.getName(), "&r&6", 
                                        new GenericCommandSender() {
                                            @Override
                                            public void setConsoleCommandSender() {}
                                            
                                            @Override
                                            public String getName() {
                                                return player.getName();
                                            }
                                        });
                            }
                        }
                        break;

                    case EXPERIENCE:
                        if (player.isOnline()) {
                            player.giveExp(getExpAmt());
                        }
                        break;

                    case CURRENCY:
                        // Check if Vault is available
                        boolean vaultSuccess = false;
                        try {
                            // This would need Vault integration
                            // For now we just log a warning
                            refs.sendMsg("chppRewardCurrencyNotImplemented", 
                                    player.getName(), "&r&c", 
                                    new GenericCommandSender() {
                                        @Override
                                        public void setConsoleCommandSender() {}
                                        
                                        @Override
                                        public String getName() {
                                            return player.getName();
                                        }
                                    });
                            vaultSuccess = false;
                        } catch (Exception e) {
                            vaultSuccess = false;
                        }
                        
                        if (!vaultSuccess && player.isOnline()) {
                            // Fallback to giving XP if Vault is not available
                            int xpEquivalent = (int) (getExpAmt() * 10);
                            player.giveExp(xpEquivalent);
                            refs.sendMsg("chppRewardCurrencyConverted", 
                                    String.valueOf(xpEquivalent), "&r&6", 
                                    new GenericCommandSender() {
                                        @Override
                                        public void setConsoleCommandSender() {}
                                        
                                        @Override
                                        public String getName() {
                                            return player.getName();
                                        }
                                    });
                        }
                        break;

                    case COMMAND:
                        if (getCommands() != null && player.isOnline()) {
                            for (String command : getCommands()) {
                                String processedCommand = replacePlaceholders(command, player);
                                if (isRunCommandAsConsole()) {
                                    player.getServer().dispatchCommand(
                                            player.getServer().getConsoleSender(),
                                            processedCommand
                                    );
                                } else {
                                    player.performCommand(processedCommand);
                                }
                            }
                        }
                        break;

                    default:
                        break;
                }
            }
        }, SYNC, null);
    }

    @Override
    public String replacePlaceholders(String input, Player player) {
        String result = input;
        
        // Replace common placeholders
        result = result.replace("%player%", player.getName());
        result = result.replace("%uuid%", player.getUniqueId().toString());
        result = result.replace("%world%", player.getWorld().getName());
        result = result.replace("%x%", String.valueOf(player.getLocation().getBlockX()));
        result = result.replace("%y%", String.valueOf(player.getLocation().getBlockY()));
        result = result.replace("%z%", String.valueOf(player.getLocation().getBlockZ()));
        
        // Process custom placeholders
        if (getPlaceholders() != null) {
            for (Map.Entry<String, String> entry : getPlaceholders().entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return result;
    }
    
    // Setter methods
    public void setItem(ItemStack item) {
        super.item = item;
    }
    
    public void setExpAmt(int expAmt) {
        super.expAmt = expAmt;
    }
    
    public void setCurrencyAmt(double currencyAmt) {
        super.currencyAmt = currencyAmt;
    }
    
    public void setCommands(List<String> commands) {
        super.commands = commands;
    }
    
    public void setPlaceholders(Map<String, String> placeholders) {
        super.placeholders = placeholders;
    }
    
    public void setRunCommandAsConsole(boolean runAsConsole) {
        super.runCommandAsConsole = runAsConsole;
    }
    
    public void addPlaceholder(String key, String value) {
        if (super.placeholders == null) {
            super.placeholders = new HashMap<>();
        }
        super.placeholders.put(key, value);
    }
}
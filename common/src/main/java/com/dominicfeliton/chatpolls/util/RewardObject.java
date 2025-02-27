package com.dominicfeliton.chatpolls.util;

import java.util.List;
import java.util.Map;

/**
 * A generic abstract class for handling rewards.
 * P represents the player type (e.g., Player in Bukkit, ServerPlayer in Forge)
 * I represents the item type (e.g., ItemStack in Bukkit, ItemStack in Forge)
 */
public abstract class RewardObject<P, I> {

    /**
     * Enum defining the types of rewards available.
     */
    public enum RewardType {
        ITEM,       // Physical item rewards
        COMMAND,    // Command execution rewards
        EXPERIENCE, // Experience point rewards
        CURRENCY;   // Virtual currency rewards (requires economy plugin)
    }

    private final RewardType type;

    // The item to give as a reward (if type == ITEM)
    protected I item;

    // The amount of experience to give (if type == EXPERIENCE)
    protected int expAmt;

    // The amount of currency to give (if type == CURRENCY)
    protected double currencyAmt;

    // List of commands to execute (if type == COMMAND)
    protected List<String> commands;

    // Placeholders to be replaced in commands
    protected Map<String, String> placeholders;

    // Whether to run commands as console (true) or as player (false)
    protected boolean runCommandAsConsole;

    /**
     * Constructor defines the type of reward
     * 
     * @param type The reward type (ITEM, COMMAND, EXPERIENCE, CURRENCY)
     */
    public RewardObject(RewardType type) {
        this.type = type;
    }

    /**
     * Core method to actually give the reward to the provided player.
     * Each platform implementation must override this.
     *
     * @param player The player who should receive the reward
     */
    public abstract void giveReward(P player);

    /**
     * A utility method to replace placeholders in commands or messages
     * (e.g., replacing %player% with the player's name).
     * Each platform implementation must override this.
     * 
     * @param input The string containing placeholders
     * @param player The player to extract data from
     * @return The string with placeholders replaced
     */
    public abstract String replacePlaceholders(String input, P player);

    /**
     * @return the reward type
     */
    public RewardType getType() {
        return type;
    }

    /**
     * @return the item reward (if type == ITEM)
     */
    public I getItem() {
        return item;
    }

    /**
     * @return the experience amount (if type == EXPERIENCE)
     */
    public int getExpAmt() {
        return expAmt;
    }

    /**
     * @return the currency amount (if type == CURRENCY)
     */
    public double getCurrencyAmt() {
        return currencyAmt;
    }

    /**
     * @return the command list (if type == COMMAND)
     */
    public List<String> getCommands() {
        return commands;
    }

    /**
     * @return the placeholder map
     */
    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    /**
     * @return whether commands run as console
     */
    public boolean isRunCommandAsConsole() {
        return runCommandAsConsole;
    }

    /**
     * Generic string representation of the reward
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Reward[type=").append(type);
        
        switch (type) {
            case ITEM:
                sb.append(", item=").append(item != null ? item.toString() : "null");
                break;
            case EXPERIENCE:
                sb.append(", exp=").append(expAmt);
                break;
            case CURRENCY:
                sb.append(", currency=").append(currencyAmt);
                break;
            case COMMAND:
                sb.append(", commands=").append(commands);
                sb.append(", asConsole=").append(runCommandAsConsole);
                break;
        }
        
        sb.append("]");
        return sb.toString();
    }
}
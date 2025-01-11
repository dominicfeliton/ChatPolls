package com.dominicfeliton.chatpolls.util;

import java.util.List;
import java.util.Map;

public abstract class RewardObject<P, I> {

    public enum RewardType {
        ITEM,
        COMMAND,
        EXPERIENCE,
        CURRENCY;
    }

    private RewardType type;

    private I item;

    private int expAmt;

    private double currencyAmt;

    private List<String> commands;

    private Map<String, String> placeholders;

    private boolean runCommandAsConsole;

    //TODO: Maybe? Conditional award distrib, should be handled by pollobj no? private String requiredChoiceKey;

    public RewardObject(RewardType type) {
        this.type = type;
    }

    /**
     * Core method to actually give the reward to the provided player.
     * This method is now abstract and should be implemented by child modules.
     *
     * @param player The player who should receive the reward
     */
    public abstract void giveReward(P player);

    /**
     * A utility method to replace placeholders in commands or messages
     * (e.g., replacing %player% with the player's name). Expand as needed.
     */
    public abstract String replacePlaceholders(String input, P player);

    // Getters for the private fields

    public RewardType getType() {
        return type;
    }

    public I getItem() {
        return item;
    }

    public int getExpAmt() {
        return expAmt;
    }

    public double getCurrencyAmt() {
        return currencyAmt;
    }

    public List<String> getCommands() {
        return commands;
    }

    public Map<String, String> getPlaceholders() {
        return placeholders;
    }

    public boolean isRunCommandAsConsole() {
        return runCommandAsConsole;
    }
}

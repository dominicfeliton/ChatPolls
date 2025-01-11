package com.dominicfeliton.chatpolls.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class BukkitRewardObject extends RewardObject<Player, ItemStack> {

    public BukkitRewardObject(RewardType type) {
        super(type);
    }

    @Override
    public void giveReward(Player player) {
        switch (getType()) {
            case ITEM:
                if (getItem() != null) {
                    player.getInventory().addItem(getItem().clone());
                }
                break;

            case EXPERIENCE:
                player.giveExp(getExpAmt());
                break;

            case CURRENCY:
                // Implement currency handling using Vault or other APIs
                break;

            case COMMAND:
                if (getCommands() != null) {
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

    @Override
    public String replacePlaceholders(String input, Player player) {
        // Simple approach:
        if (input.contains("%player%")) {
            input = input.replace("%player%", player.getName());
        }
        if (getPlaceholders() != null) {
            for (Map.Entry<String, String> entry : getPlaceholders().entrySet()) {
                input = input.replace(entry.getKey(), entry.getValue());
            }
        }
        return input;
    }
}

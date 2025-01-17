package com.dominicfeliton.chatpolls.commands;

import com.dominicfeliton.chatpolls.ChatPolls;
import com.dominicfeliton.chatpolls.configuration.ConfigurationHandler;
import com.dominicfeliton.chatpolls.util.BukkitCommandSender;
import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericCommandSender;

import com.dominicfeliton.chatpolls.util.PlayerRecord;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CHPLocalizeBukkit extends CHPLocalize {

    private ChatPolls main = ChatPolls.instance;
    private ConfigurationHandler configs = main.getConfigManager();

    private boolean isConsoleSender;

    public CHPLocalizeBukkit(GenericCommandSender sender, String label, String[] args, CommonRefs refs) {
        super(sender, label, args, refs);
        isConsoleSender = sender.getName() == null;
    }

    @Override
    public boolean changeLocalization(String inName, String locale) {
        Player inPlayer = Bukkit.getPlayerExact(inName);
        BukkitCommandSender bukkitSender = ((BukkitCommandSender) sender);

        if (inPlayer == null) {
            playerNotFoundMsg(bukkitSender, inName);
            return false;
        }

        if (!refs.isSupportedLang(locale, CommonRefs.LangType.LOCAL) && !locale.equalsIgnoreCase("stop")) {
            refs.sendMsg("chplLangInvalid", new String[]{"&6" + locale, "&6" + refs.getFormattedLangCodes("local")}, sender);
            return false;
        }

        if (!inName.equalsIgnoreCase(sender.getName()) && !bukkitSender.getBukkitSender().hasPermission("chatpolls.chpl.otherplayers")) {
            refs.badPermsMessage("chatpolls.chpl.otherplayers", sender);
            return false;
        }

        PlayerRecord currRecord = main.getPlayerRecord(inPlayer, true);
        if (!isConsoleSender && inPlayer.getName().equalsIgnoreCase(sender.getName())) {
            // Changing our own localization
            if (!locale.equalsIgnoreCase("stop")) {
                changeLangMsg(bukkitSender, inName, locale);
                refs.playSound(CommonRefs.SoundType.SUBMENU_TOGGLE_ON, sender);
            } else {
                if (!currRecord.getLocalizationCode().isEmpty()) {
                    stopLangMsg(bukkitSender);
                    refs.playSound(CommonRefs.SoundType.SUBMENU_TOGGLE_OFF, sender);
                } else {
                    alreadyStoppedMsg(bukkitSender);
                }
            }
        } else {
            // Changing someone else's
            if (!locale.equalsIgnoreCase("stop")) {
                refs.sendMsg("chplLangChangedOtherPlayerSender", new String[]{"&6" + inName, "&6" + locale}, sender);
                changeLangMsg(new BukkitCommandSender(inPlayer), inName, locale);
                refs.playSound(CommonRefs.SoundType.SUBMENU_TOGGLE_OFF, sender);
            } else {
                if (!currRecord.getLocalizationCode().isEmpty()) {
                    refs.sendMsg("chplLangStoppedOtherPlayerSender", "&6" + inName, sender);
                    stopLangMsg(new BukkitCommandSender(inPlayer));
                    refs.playSound(CommonRefs.SoundType.SUBMENU_TOGGLE_OFF, sender);
                } else {
                    refs.sendMsg("chplLangAlreadyStoppedOtherPlayerSender", "&6" + inName, sender);
                }
            }
        }

        // Convert to lang code
        locale = !locale.equalsIgnoreCase("stop") ? refs.getSupportedLang(locale, CommonRefs.LangType.LOCAL).getLangCode() : "";

        if (!locale.isEmpty() && configs.getPluginLangConfigs().get(locale) == null) {
            refs.debugMsg("Not found in our YamlConfigs...check localization init logs!");
            refs.sendMsg("chpLangNotLoaded", new String[]{"&c" + locale, "&6" + refs.getFormattedLangCodes("local")}, "&e", sender);
            main.getLogger().warning(refs.getPlainMsg("chpLangNotLoadedConsole", locale));
            return false;
        }
        currRecord.setLocalizationCode(locale);

        return true;
    }

    private void playerNotFoundMsg(BukkitCommandSender sender, String inName) {
        refs.sendMsg("chplLangPlayerNotValid", "&6" + args[0], "&c", sender);
    }

    private void changeLangMsg(BukkitCommandSender sender, String inName, String locale) {
        refs.sendMsg("chplLangChanged", "&6" + locale, sender);
    }

    private void stopLangMsg(BukkitCommandSender sender) {
        refs.sendMsg("chplLangStopped", sender);
    }

    private void alreadyStoppedMsg(BukkitCommandSender sender) {
        refs.sendMsg("chplLangAlreadyStopped", sender);
    }
}

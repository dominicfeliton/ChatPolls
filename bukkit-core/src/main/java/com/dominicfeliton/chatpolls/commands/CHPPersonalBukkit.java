package com.dominicfeliton.chatpolls.commands;

import com.dominicfeliton.chatpolls.ChatPolls;
import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericCommandSender;
import com.dominicfeliton.chatpolls.util.PollObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class CHPPersonalBukkit extends BasicCommand {

    private final ChatPolls main;
    private final CommonRefs refs;

    public CHPPersonalBukkit(GenericCommandSender sender, String label, String[] args, CommonRefs refs) {
        super(sender, label, args, refs);
        this.main = ChatPolls.instance;
        this.refs = refs;
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
            default:
                refs.sendMsg("chppUsage", sender);
                return true;
        }
    }

    private boolean handleCreate(UUID playerUuid) {
        if (args.length < 2) {
            refs.sendMsg("chppCreateUsage", sender);
            return true;
        }

        String pollTitle = args[1];
        String pollDescription = "";
        if (args.length > 2) {
            StringBuilder descBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                descBuilder.append(args[i]).append(" ");
            }
            pollDescription = descBuilder.toString().trim();
        }

        String pollId = generatePollId();
        PersonalPollObject personalPoll = new PersonalPollObject(pollTitle, pollDescription);

        main.getPersonalPolls().putIfAbsent(playerUuid, new ConcurrentHashMap<>());
        main.getPersonalPolls().get(playerUuid).put(pollId, personalPoll);

        refs.sendMsg("chppCreateSuccess", new String[]{pollId, pollTitle, pollDescription}, "&a", sender);
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

            String pollTitle = "";
            if (poll instanceof PersonalPollObject) {
                pollTitle = ((PersonalPollObject) poll).getTitle();
            }

            refs.sendMsg("chppListItem", new String[]{pollId, pollTitle}, "&b", sender);
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
            refs.sendMsg("chppDeleteNotFound", new String[]{pollId}, "&c", sender);
            return true;
        }

        userPolls.remove(pollId);
        refs.sendMsg("chppDeleteSuccess", pollId, "&a", sender);
        return true;
    }

    private String generatePollId() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    private static class PersonalPollObject extends PollObject {
        private final String title;
        private final String description;

        public PersonalPollObject(String title, String description) {
            super();
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }
}

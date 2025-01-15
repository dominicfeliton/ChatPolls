package com.dominicfeliton.chatpolls.commands;

import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericCommandSender;

public abstract class CHPLocalize extends BasicCommand {

    private final boolean isConsoleSender;

    public CHPLocalize(GenericCommandSender sender, String label, String[] args, CommonRefs refs) {
        super(sender, label, args, refs);
        isConsoleSender = sender.getName() == null;
    }

    @Override
    public boolean processCommand() {
        // Simple command to set yours or a user's localization.
        if (args.length > 2) {
            refs.sendMsg("chpInvalidArgs", "", "&c", sender);
            return false;
        }

        if (args.length == 0) {
            if (isConsoleSender) {
                refs.sendMsg("chpInvalidArgs", "", "&c", sender);
                return false;
            }
            return changeLocalization(sender.getName(), "stop");
        } else if (args.length == 1) {
            if (isConsoleSender) {
                return changeLocalization(args[0], "stop");
            }
            if (refs.getPlayerObjExact(args[0]) != null) {
                return changeLocalization(args[0], "stop");
            }
            return changeLocalization(sender.getName(), args[0]);
        } else if (args.length == 2) {
            return changeLocalization(args[0], args[1]);
        }
        return false;
    }

    public abstract boolean changeLocalization(String inName, String locale);
}

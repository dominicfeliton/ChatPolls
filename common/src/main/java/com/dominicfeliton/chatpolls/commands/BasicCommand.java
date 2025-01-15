package com.dominicfeliton.chatpolls.commands;

import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericCommandSender;

public class BasicCommand {

    protected GenericCommandSender sender;
    protected String label;
    protected String[] args;
    protected CommonRefs refs;


    public BasicCommand(GenericCommandSender sender, String label, String[] args, CommonRefs refs) {
        this.sender = sender;
        this.label = label;
        this.args = args;
        this.refs = refs;
    }

    public boolean processCommand() {
        return true;
    }

}

package com.dominicfeliton.chatpolls.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.kyori.adventure.text.Component;

import java.io.InputStream;
import java.util.*;

public class FoliaCommonRefs extends BukkitCommonRefs {
    
     /**
     * Checks if the server is stopping or reloading, by attempting to register a scheduler task.
     * This will throw an IllegalPluginAccessException if we are on Bukkit or one of its derivatives.
     *
     * @return Boolean - Whether the server is reloading/stopping or not
     */
    @Override
    public boolean serverIsStopping() {
        boolean stopping = !main.isEnabled() && main.getServer().isStopping();
        debugMsg("Folia stop check: " + stopping);
        return stopping;
    }

}
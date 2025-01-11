package com.dominicfeliton.chatpolls.util;

import java.util.HashSet;
import java.util.Set;

public class PlayerRecord {

    private String playerUUID = "";
    private String localizationCode = "";

    // poll UUIDs
    private Set<String> pollHistory = new HashSet<>();

    private boolean hasBeenSaved = false;

    public PlayerRecord(String playerUUID, String localizationCode) {
        this.playerUUID = playerUUID;
        this.localizationCode = localizationCode;
    }

    public String getUUID() {
        return playerUUID;
    }

    public String getLocalizationCode() {
        return localizationCode;
    }

}

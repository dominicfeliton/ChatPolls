package com.dominicfeliton.chatpolls.runnables;

import com.dominicfeliton.chatpolls.util.CommonRefs;
import com.dominicfeliton.chatpolls.util.GenericCommandSender;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class UpdateChecker {

    // Indicates whether the plugin is up to date
    private boolean upToDate = false;

    // Stores the latest version retrieved from the remote source
    protected String latestVersion = "";

    // Common references utility
    protected final CommonRefs refs;

    protected final GenericCommandSender sender;

    // URL to fetch the latest version information
    protected static final String VERSION_URL = "https://raw.githubusercontent.com/dominicfeliton/ChatPolls/main/latestVersion.txt";

    /**
     * Constructor initializes the UpdateChecker and starts the update check.
     */
    public UpdateChecker(CommonRefs refs, String currentVersion, GenericCommandSender sender) {
        this.refs = refs;
        this.sender = sender;
        checkForUpdates(currentVersion);
    }

    private void checkForUpdates(String currentVersion) {
        refs.debugMsg("Starting UpdateChecker!!!");

        latestVersion = fetchLatestVersion(currentVersion);
        compareVersions(currentVersion, sender);
    }

    private String fetchLatestVersion(String currentVersion) {
        try (InputStream in = new URL(VERSION_URL).openStream()) {
            List<String> lines = IOUtils.readLines(in, StandardCharsets.UTF_8);

            if (lines.size() > 1) {
                return lines.get(1).trim();
            } else if (!lines.isEmpty()) {
                return lines.get(0).trim();
            } else {
                return currentVersion;
            }

        } catch (IOException e) {
            return currentVersion;
        }
    }

    private boolean compareVersions(String currentVersion, GenericCommandSender sender) {
        try {
            ComparableVersion current = new ComparableVersion(currentVersion);
            ComparableVersion latest = new ComparableVersion(latestVersion);
            refs.debugMsg("latest: "+ latestVersion);

            int comparison = current.compareTo(latest);

            if (comparison == 0) {
                upToDate = true;
                refs.log(refs.getPlainMsg("chpUpdaterUpToDate", "", "&d"));
            } else if (comparison > 0) {
                refs.warnLog(refs.getPlainMsg("chpUpdaterFutureDate", "", "&e"));
            } else {
                refs.warnLog(refs.getPlainMsg("chpUpdaterOutOfDate", "&d" + latestVersion, "&e"));
                refs.warnLog("https://github.com/dominicfeliton/ChatPolls/releases/tag/v" + latestVersion);
            }

        } catch (Exception e) {
            refs.warnLog(refs.getPlainMsg("chpUpdaterFailedGeneric"));
        }

        return upToDate;
    }
}
package com.notpatch.nOrder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.notpatch.nlib.util.NLogger;
import lombok.Data;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private final NOrder main;

    public UpdateChecker(NOrder main) {
        this.main = main;
    }

    public CompletableFuture<UpdateInfo> checkUpdates() {
        CompletableFuture<UpdateInfo> future = new CompletableFuture<>();

        main.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
            try {
                URL url = new URL("https://raw.githubusercontent.com/NotPatch/NOrder/refs/heads/master/version.json");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream())
                    );

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();


                    String jsonResponse = response.toString().trim().replaceAll("\\s+", " ");

                    JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();

                    String latestVersion = json.get("version").getAsString();
                    String message = json.get("message").getAsString();
                    boolean critical = json.has("critical") &&
                            json.get("critical").getAsBoolean();

                    String currentVersion = main.getDescription().getVersion().split("-")[0];
                    VersionComparison comparison = compareVersions(currentVersion, latestVersion);

                    UpdateInfo updateInfo = new UpdateInfo(currentVersion, latestVersion, message, critical, comparison);


                    future.complete(updateInfo);

                } else {
                    future.completeExceptionally(
                            new Exception("HTTP Error: " + responseCode)
                    );
                }

                connection.disconnect();

            } catch (Exception e) {
                future.completeExceptionally(e);
                NLogger.warn("Failed to check for updates: " + e.getMessage());
            }

        });

        return future.thenApply(updateInfo -> {
            sendUpdateNotification(updateInfo);
            return updateInfo;
        });

    }

    private void sendUpdateNotification(UpdateInfo updateInfo) {
        if (updateInfo.isCritical()) {
            NLogger.error("A critical update is available! Please update immediately!");
        }
        switch (updateInfo.getVersionComparison()) {
            case MAJOR_UPDATE -> {
                NLogger.warn("A major update is available! Current version: " + updateInfo.getCurrentVersion() + ", Latest version: " + updateInfo.getLatestVersion());
                NLogger.warn("Update Info: " + updateInfo.getMessage());
            }
            case MINOR_UPDATE -> {
                NLogger.warn("A minor update is available! Current version: " + updateInfo.getCurrentVersion() + ", Latest version: " + updateInfo.getLatestVersion());
                NLogger.warn("Update Info: " + updateInfo.getMessage());
            }
            case UP_TO_DATE -> {
                NLogger.info("You are running the latest version: " + updateInfo.getCurrentVersion());
            }
            case OUTDATED -> {
                NLogger.warn("You are running an outdated version: " + updateInfo.getCurrentVersion() + ". Latest version: " + updateInfo.getLatestVersion());
                NLogger.warn("Update Info: " + updateInfo.getMessage());
            }
            case UNKNOWN -> {
                NLogger.warn("Could not determine update status for version: " + updateInfo.getCurrentVersion());
            }
        }
    }

    private VersionComparison compareVersions(String current, String latest) {
        try {
            String[] currentParts = current.replace("v", "").split("\\.");
            String[] latestParts = latest.replace("v", "").split("\\.");

            int length = Math.max(currentParts.length, latestParts.length);

            for (int i = 0; i < length; i++) {
                int currentPart = i < currentParts.length ?
                        Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ?
                        Integer.parseInt(latestParts[i]) : 0;

                if (latestPart > currentPart) {
                    if (i == 0) {
                        return VersionComparison.MAJOR_UPDATE;
                    }
                    return VersionComparison.MINOR_UPDATE;
                } else if (latestPart < currentPart) {
                    return VersionComparison.OUTDATED;
                }
            }

            return VersionComparison.UP_TO_DATE;

        } catch (NumberFormatException e) {
            return VersionComparison.UNKNOWN;
        }
    }

    @Data
    @Getter
    public static class UpdateInfo {
        private final String currentVersion;
        private final String latestVersion;
        private final String message;
        private final boolean critical;
        private final VersionComparison versionComparison;

        private boolean isUpdateAvailable() {
            return versionComparison == VersionComparison.MAJOR_UPDATE ||
                    versionComparison == VersionComparison.MINOR_UPDATE;
        }

    }

    public enum VersionComparison {
        UP_TO_DATE,
        MINOR_UPDATE,
        MAJOR_UPDATE,
        OUTDATED,
        UNKNOWN
    }

}

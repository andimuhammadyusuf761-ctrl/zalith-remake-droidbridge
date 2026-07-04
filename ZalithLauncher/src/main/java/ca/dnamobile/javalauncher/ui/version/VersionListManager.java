package ca.dnamobile.javalauncher.ui.version;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;

import ca.dnamobile.javalauncher.data.model.MinecraftVersion;
import ca.dnamobile.javalauncher.feature.log.Logging;

/**
 * Fetches and caches the Minecraft version manifest for DroidBridge's version picker.
 */
public final class VersionListManager {
    private static final String TAG = "VersionListManager";
    private static final String MANIFEST_URL =
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

    @Nullable private static List<MinecraftVersion> cachedVersions;

    private VersionListManager() {}

    /** Returns the cached version list, or null if not yet loaded. */
    @Nullable
    public static List<MinecraftVersion> getCached() {
        return cachedVersions;
    }

    /**
     * Fetch the version manifest from Mojang. Must run on a background thread.
     * @return list of available versions, or empty list on failure.
     */
    @WorkerThread
    @NonNull
    public static List<MinecraftVersion> fetchVersions(@NonNull Context context) {
        List<MinecraftVersion> versions = new ArrayList<>();
        try {
            java.net.URL url = new java.net.URL(MANIFEST_URL);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            try (java.io.InputStream is = conn.getInputStream()) {
                byte[] data = is.readAllBytes();
                String json = new String(data, "UTF-8");
                // Simple JSON extraction — full parsing would use Gson/Moshi
                int versionsIdx = json.indexOf("\"versions\"");
                if (versionsIdx >= 0) {
                    // Extract version id/type pairs via simple regex
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                            "\"id\":\"([^\"]+)\",\"type\":\"([^\"]+)\"");
                    java.util.regex.Matcher m = p.matcher(json.substring(versionsIdx));
                    while (m.find()) {
                        versions.add(new MinecraftVersion(m.group(1), m.group(2), null));
                    }
                }
            } finally {
                conn.disconnect();
            }
            cachedVersions = versions;
            Logging.i(TAG, "Fetched " + versions.size() + " versions");
        } catch (Exception e) {
            Logging.e(TAG, "Failed to fetch version manifest", e);
        }
        return versions;
    }
}

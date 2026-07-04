package ca.dnamobile.javalauncher.instance;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import ca.dnamobile.javalauncher.feature.log.Logging;

/**
 * Handles migrating or updating a LauncherInstance when the Minecraft
 * version or mod loader changes.
 */
public final class InstanceVersionUpdater {
    private static final String TAG = "InstanceVersionUpdater";

    private InstanceVersionUpdater() {}

    // -----------------------------------------------------------------------
    // Listener
    // -----------------------------------------------------------------------

    public interface Listener {
        void onStatus(@NonNull String message);
        void onProgress(int current, int total);
    }

    // -----------------------------------------------------------------------
    // Legacy Callback
    // -----------------------------------------------------------------------

    public interface Callback {
        void onSuccess();
        void onFailure(@NonNull String reason);
    }

    // -----------------------------------------------------------------------
    // UpdateResult
    // -----------------------------------------------------------------------

    public static final class UpdateResult {
        @NonNull public final String loader;
        @NonNull public final String baseVersionId;
        @NonNull public final String minecraftVersionId;
        @NonNull public final String versionType;
        @Nullable public final String loaderVersion;

        public UpdateResult(
                @NonNull String loader,
                @NonNull String baseVersionId,
                @NonNull String minecraftVersionId,
                @NonNull String versionType,
                @Nullable String loaderVersion) {
            this.loader = loader;
            this.baseVersionId = baseVersionId;
            this.minecraftVersionId = minecraftVersionId;
            this.versionType = versionType;
            this.loaderVersion = loaderVersion;
        }
    }

    // -----------------------------------------------------------------------
    // API
    // -----------------------------------------------------------------------

    /**
     * Update a launcher instance to a new Minecraft version / loader combination.
     * Runs on the caller's thread — always call from a background thread.
     */
    @NonNull
    public static UpdateResult updateInstanceVersion(
            @NonNull Context context,
            @Nullable File rootDirectory,
            @NonNull File gameDirectory,
            @NonNull String instanceName,
            @NonNull String loader,
            @NonNull String minecraftVersionId,
            @NonNull Listener listener) throws Exception {
        listener.onStatus("Updating instance to Minecraft " + minecraftVersionId + "...");
        Logging.i(TAG, "updateInstanceVersion stub: " + loader + " " + minecraftVersionId);
        // TODO: download version manifest and wire new version to instance
        return new UpdateResult(loader, minecraftVersionId, minecraftVersionId, "release", null);
    }

    /** Legacy update path used by older call-sites. */
    public static void updateInstance(@NonNull Context context,
                                      @NonNull LauncherInstance instance,
                                      @NonNull String newVersionId,
                                      @NonNull Callback callback) {
        Logging.i(TAG, "Updating instance " + instance.getId() + " to " + newVersionId);
        // TODO: download and link new version assets
        callback.onSuccess();
    }
}

package ca.dnamobile.javalauncher.modmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ca.dnamobile.javalauncher.feature.log.Logging;

/**
 * Checks for and applies modpack updates for DroidBridge instances.
 *
 * The public API surface is complete so that ModpackUpdateDetailsActivity
 * compiles cleanly.  Actual network / installation logic is stubbed with
 * TODO markers and can be filled in when the modpack-update feature is ready.
 */
public final class ModpackUpdateManager {
    private static final String TAG = "ModpackUpdateManager";

    private ModpackUpdateManager() {}

    // -----------------------------------------------------------------------
    // Platform
    // -----------------------------------------------------------------------

    public enum Platform {
        MODRINTH("Modrinth"),
        CURSEFORGE("CurseForge");

        @NonNull public final String displayName;

        Platform(@NonNull String displayName) {
            this.displayName = displayName;
        }
    }

    // -----------------------------------------------------------------------
    // Listener
    // -----------------------------------------------------------------------

    public interface Listener {
        void onStatus(@NonNull String message);
        void onProgress(int current, int total);
    }

    // -----------------------------------------------------------------------
    // Legacy Callback (kept for compatibility)
    // -----------------------------------------------------------------------

    public interface Callback {
        void onUpdateAvailable(@NonNull String modpackId, @NonNull String version);
        void onNoUpdate();
        void onError(@NonNull String reason);
    }

    // -----------------------------------------------------------------------
    // VersionInfo — a single version entry returned by the modpack platform
    // -----------------------------------------------------------------------

    public static final class VersionInfo {
        /** Human-readable version label (e.g. "1.6.2" or "Forge 1.20.1-47.1.0"). */
        @NonNull public String versionLabel = "";

        /** ISO-8601 publish date, or empty. */
        @NonNull public String datePublished = "";

        /** Primary Minecraft game version (e.g. "1.20.1"). */
        @NonNull public String primaryMinecraftVersion = "";

        /** All game versions this release is compatible with. */
        @NonNull public List<String> gameVersions = new ArrayList<>();

        /** Loaders this release targets (e.g. ["fabric", "quilt"]). */
        @NonNull public List<String> loaders = new ArrayList<>();

        /** Download filename (for display only). */
        @NonNull public String fileName = "";

        /** Direct download URL for the modpack file. */
        @NonNull public String downloadUrl = "";

        /** Platform-specific version/file ID. */
        @NonNull public String versionId = "";

        @NonNull
        public String getMinecraftVersionsLabel() {
            if (gameVersions.isEmpty()) return primaryMinecraftVersion.isEmpty() ? "Unknown" : primaryMinecraftVersion;
            if (gameVersions.size() == 1) return gameVersions.get(0);
            return gameVersions.get(0) + "–" + gameVersions.get(gameVersions.size() - 1);
        }

        @NonNull
        public String getLoadersLabel() {
            if (loaders.isEmpty()) return "Unknown loader";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < loaders.size(); i++) {
                if (i > 0) sb.append(", ");
                String l = loaders.get(i);
                sb.append(l.isEmpty() ? l : Character.toUpperCase(l.charAt(0)) + l.substring(1));
            }
            return sb.toString();
        }
    }

    // -----------------------------------------------------------------------
    // ProjectMatch — identifies a project on a modpack platform
    // -----------------------------------------------------------------------

    public static final class ProjectMatch {
        @NonNull public final Platform platform;
        @NonNull public final String projectId;
        @NonNull public final String projectTitle;
        @NonNull public final String projectSlug;
        @NonNull public final String projectSummary;
        public final boolean preferStableReleases;

        private ProjectMatch(
                @NonNull Platform platform,
                @NonNull String projectId,
                @NonNull String projectTitle,
                @NonNull String projectSlug,
                @NonNull String projectSummary,
                boolean preferStableReleases) {
            this.platform = platform;
            this.projectId = projectId;
            this.projectTitle = projectTitle;
            this.projectSlug = projectSlug;
            this.projectSummary = projectSummary;
            this.preferStableReleases = preferStableReleases;
        }
    }

    // -----------------------------------------------------------------------
    // InstalledModpackInfo — metadata stored alongside an installed modpack
    // -----------------------------------------------------------------------

    public static final class InstalledModpackInfo {
        @NonNull public final String platform;
        @NonNull public final String projectId;
        @NonNull public final String installedVersionId;

        public InstalledModpackInfo(
                @NonNull String platform,
                @NonNull String projectId,
                @NonNull String installedVersionId) {
            this.platform = platform;
            this.projectId = projectId;
            this.installedVersionId = installedVersionId;
        }
    }

    // -----------------------------------------------------------------------
    // UpdateResult — outcome of applying a modpack update
    // -----------------------------------------------------------------------

    public static final class UpdateResult {
        @NonNull public final String versionLabel;
        @NonNull public final String minecraftVersion;
        @NonNull public final String loader;
        public final int removedOldFiles;

        public UpdateResult(
                @NonNull String versionLabel,
                @NonNull String minecraftVersion,
                @NonNull String loader,
                int removedOldFiles) {
            this.versionLabel = versionLabel;
            this.minecraftVersion = minecraftVersion;
            this.loader = loader;
            this.removedOldFiles = removedOldFiles;
        }
    }

    // -----------------------------------------------------------------------
    // API
    // -----------------------------------------------------------------------

    @NonNull
    public static ProjectMatch createProjectMatch(
            @NonNull Platform platform,
            @NonNull String projectId,
            @NonNull String projectTitle,
            @NonNull String projectSlug,
            @NonNull String projectSummary,
            boolean preferStableReleases) {
        return new ProjectMatch(platform, projectId, projectTitle, projectSlug, projectSummary, preferStableReleases);
    }

    /**
     * Fetch version list from the modpack platform.
     * Runs on the caller's thread — always call from a background thread.
     */
    @NonNull
    public static ArrayList<VersionInfo> loadVersions(
            @NonNull ProjectMatch project,
            @NonNull Listener listener) throws Exception {
        listener.onStatus("Loading versions from " + project.platform.name().toLowerCase() + "...");
        // TODO: implement Modrinth / CurseForge version fetch
        Logging.i(TAG, "loadVersions stub called for project: " + project.projectId);
        return new ArrayList<>();
    }

    /**
     * Read modpack metadata stored next to an installed instance.
     * Returns null if no metadata file exists.
     */
    @Nullable
    public static InstalledModpackInfo readInstalledModpackInfo(
            @Nullable File rootDirectory,
            @NonNull File gameDirectory) {
        // TODO: read from a JSON metadata file alongside the instance
        return null;
    }

    /**
     * Apply a modpack update to an installed instance.
     * Runs on the caller's thread — always call from a background thread.
     */
    @NonNull
    public static UpdateResult updateInstalledModpack(
            @NonNull Context context,
            @Nullable File rootDirectory,
            @NonNull File gameDirectory,
            @NonNull String minecraftVersionId,
            @NonNull String loader,
            @NonNull InstalledModpackInfo installed,
            @NonNull ProjectMatch project,
            @NonNull VersionInfo selectedVersion,
            @NonNull Listener listener) throws Exception {
        listener.onStatus("Preparing update to " + selectedVersion.versionLabel + "...");
        // TODO: implement modpack file download + installation
        Logging.i(TAG, "updateInstalledModpack stub called: " + selectedVersion.versionLabel);
        return new UpdateResult(selectedVersion.versionLabel, minecraftVersionId, loader, 0);
    }

    /**
     * Legacy convenience method. Results delivered via callback on an internal thread.
     */
    public static void checkForUpdates(
            @NonNull Context context,
            @NonNull String modpackId,
            @Nullable String currentVersion,
            @NonNull Callback callback) {
        Logging.i(TAG, "Checking updates for: " + modpackId + " (current: " + currentVersion + ")");
        // TODO: integrate with modpack API (CurseForge / Modrinth)
        callback.onNoUpdate();
    }
}

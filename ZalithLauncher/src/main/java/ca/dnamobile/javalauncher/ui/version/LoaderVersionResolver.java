package ca.dnamobile.javalauncher.ui.version;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves available mod-loader versions for a given loader + Minecraft version pair.
 *
 * Currently returns stub data; hook up to the Fabric / Forge / NeoForge / Quilt
 * meta APIs when loader-version fetching is implemented.
 */
public final class LoaderVersionResolver {

    private LoaderVersionResolver() {}

    // -----------------------------------------------------------------------
    // Data model
    // -----------------------------------------------------------------------

    public static final class LoaderVersionOption {
        @NonNull public final String displayName;
        @Nullable public final String loaderVersion;

        public LoaderVersionOption(@NonNull String displayName, @Nullable String loaderVersion) {
            this.displayName = displayName;
            this.loaderVersion = loaderVersion;
        }

        @NonNull
        @Override
        public String toString() {
            return displayName;
        }
    }

    // -----------------------------------------------------------------------
    // Resolution
    // -----------------------------------------------------------------------

    /**
     * Resolve available loader versions for the given loader + Minecraft version.
     * Runs on whatever thread the caller provides — callers are responsible for
     * dispatching to a background thread.
     *
     * @param loader    Loader identifier, e.g. "fabric", "forge", "neoforge", "quilt"
     * @param mcVersion Minecraft version string, e.g. "1.21.1"
     * @return List of available options (may be empty)
     */
    @NonNull
    public static List<LoaderVersionOption> resolveVersions(
            @NonNull String loader,
            @NonNull String mcVersion) throws Exception {
        // TODO: fetch from Fabric meta / Forge maven / NeoForge meta / Quilt meta
        return new ArrayList<>();
    }
}

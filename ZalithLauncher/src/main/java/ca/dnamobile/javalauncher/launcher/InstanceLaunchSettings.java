package ca.dnamobile.javalauncher.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Per-instance launch settings (JVM args, resolution, renderer override). */
public final class InstanceLaunchSettings {

    public static final String RENDERER_DEFAULT = "default";
    public static final int RAM_DEFAULT = -1;

    private static final String PREFS_NAME = "instance_launch_settings";
    private static final String KEY_RENDERER = "_renderer";
    private static final String KEY_GRAPHICS_API = "_graphics_api";
    private static final String KEY_RUNTIME = "_runtime";
    private static final String KEY_JVM_ARGS = "_jvm_args";
    private static final String KEY_RAM_MB = "_ram_mb";

    // Legacy fields kept for compatibility
    @Nullable public final String jvmArgs;
    @Nullable public final String rendererOverride;
    public final int resolutionScalePercent;
    public final boolean forceFullscreen;

    public InstanceLaunchSettings(
            @Nullable String jvmArgs,
            @Nullable String rendererOverride,
            int resolutionScalePercent,
            boolean forceFullscreen) {
        this.jvmArgs = jvmArgs;
        this.rendererOverride = rendererOverride;
        this.resolutionScalePercent = resolutionScalePercent;
        this.forceFullscreen = forceFullscreen;
    }

    public static InstanceLaunchSettings defaults() {
        return new InstanceLaunchSettings(null, null, 100, false);
    }

    @NonNull @Override
    public String toString() {
        return "InstanceLaunchSettings{jvmArgs=" + jvmArgs
                + ", renderer=" + rendererOverride
                + ", scale=" + resolutionScalePercent + "%, fullscreen=" + forceFullscreen + "}";
    }

    // -----------------------------------------------------------------------
    // Settings inner class — mutable per-instance overrides
    // -----------------------------------------------------------------------

    public static final class Settings {
        @NonNull public String rendererIdentifier = RENDERER_DEFAULT;
        @NonNull public String graphicsApiMode = "auto";
        @NonNull public String runtimeName = "default";
        @Nullable public String customJvmArgs = "";
        public int ramMb = RAM_DEFAULT;

        public boolean hasRamOverride() {
            return ramMb > 0;
        }
    }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    @NonNull
    public static Settings load(@NonNull Context context, @NonNull String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Settings s = new Settings();
        s.rendererIdentifier = prefs.getString(key + KEY_RENDERER, RENDERER_DEFAULT);
        s.graphicsApiMode    = prefs.getString(key + KEY_GRAPHICS_API, "auto");
        s.runtimeName        = prefs.getString(key + KEY_RUNTIME, "default");
        s.customJvmArgs      = prefs.getString(key + KEY_JVM_ARGS, "");
        s.ramMb              = prefs.getInt(key + KEY_RAM_MB, RAM_DEFAULT);
        return s;
    }

    public static void save(@NonNull Context context, @NonNull String key, @NonNull Settings settings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(key + KEY_RENDERER, settings.rendererIdentifier)
                .putString(key + KEY_GRAPHICS_API, settings.graphicsApiMode)
                .putString(key + KEY_RUNTIME, settings.runtimeName)
                .putString(key + KEY_JVM_ARGS, settings.customJvmArgs != null ? settings.customJvmArgs : "")
                .putInt(key + KEY_RAM_MB, settings.ramMb)
                .apply();
    }

    public static void clear(@NonNull Context context, @NonNull String key) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(key + KEY_RENDERER)
                .remove(key + KEY_GRAPHICS_API)
                .remove(key + KEY_RUNTIME)
                .remove(key + KEY_JVM_ARGS)
                .remove(key + KEY_RAM_MB)
                .apply();
    }

    // -----------------------------------------------------------------------
    // Graphics API mode helpers
    // -----------------------------------------------------------------------

    private static final String[] GRAPHICS_API_MODES  = { "auto", "opengl", "vulkan" };
    private static final String[] GRAPHICS_API_LABELS = { "Auto (Minecraft default)", "OpenGL", "Vulkan" };

    @NonNull
    public static String[] getGraphicsApiModeLabels() {
        return GRAPHICS_API_LABELS.clone();
    }

    @NonNull
    public static String graphicsApiModeForIndex(int index) {
        if (index < 0 || index >= GRAPHICS_API_MODES.length) return GRAPHICS_API_MODES[0];
        return GRAPHICS_API_MODES[index];
    }

    public static int graphicsApiModeIndex(@Nullable String mode) {
        if (mode == null) return 0;
        for (int i = 0; i < GRAPHICS_API_MODES.length; i++) {
            if (GRAPHICS_API_MODES[i].equalsIgnoreCase(mode)) return i;
        }
        return 0;
    }

    // -----------------------------------------------------------------------
    // Runtime helpers
    // -----------------------------------------------------------------------

    private static final String[] RUNTIME_NAMES  = { "default", "jre-8", "jre-17", "jre-21" };
    private static final String[] RUNTIME_LABELS = { "Default (auto-select)", "Java 8", "Java 17", "Java 21" };

    @NonNull
    public static String[] getRuntimeDisplayLabels() {
        return RUNTIME_LABELS.clone();
    }

    @NonNull
    public static String runtimeNameForIndex(int index) {
        if (index < 0 || index >= RUNTIME_NAMES.length) return RUNTIME_NAMES[0];
        return RUNTIME_NAMES[index];
    }

    public static int runtimeIndexForName(@Nullable String name) {
        if (name == null) return 0;
        for (int i = 0; i < RUNTIME_NAMES.length; i++) {
            if (RUNTIME_NAMES[i].equalsIgnoreCase(name)) return i;
        }
        return 0;
    }

    /**
     * Resolve a raw instance key to a canonical, stable form.
     *
     * <p>Instance keys are used to namespace per-instance settings in SharedPreferences.
     * If the raw key is null or empty the {@code fallback} value is returned unchanged.</p>
     *
     * @param rawKey   The key as stored (may contain path separators or special characters).
     * @param fallback Value to return when {@code rawKey} is null or empty.
     * @return A canonical key safe for use as a SharedPreferences prefix.
     */
    @NonNull
    public static String resolveInstanceKey(@Nullable String rawKey, @NonNull String fallback) {
        if (rawKey == null || rawKey.trim().isEmpty()) return fallback;
        // Replace filesystem separators with underscores to produce a flat key.
        return rawKey.replace('/', '_').replace('\\', '_').trim();
    }
}

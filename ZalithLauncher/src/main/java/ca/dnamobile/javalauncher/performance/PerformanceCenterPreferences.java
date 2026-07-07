/*
 * Copyright (c) 2026 DNA Mobile Applications. All rights reserved.
 * DroidBridge project code — Performance Center persistent preferences.
 */
package ca.dnamobile.javalauncher.performance;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * SharedPreferences facade for the DroidBridge Performance Center.
 * Stores the active preset, individual overrides, benchmark scores,
 * and auto-optimize results so they survive app restarts.
 */
public final class PerformanceCenterPreferences {

    private static final String PREFS_NAME = "performance_center_prefs";

    // Keys
    private static final String KEY_ACTIVE_PRESET        = "active_preset";
    private static final String KEY_CUSTOM_RAM_MB        = "custom_ram_mb";
    private static final String KEY_CUSTOM_RESOLUTION    = "custom_resolution_scale";
    private static final String KEY_BENCHMARK_SCORE      = "benchmark_score";
    private static final String KEY_BENCHMARK_TIMESTAMP  = "benchmark_timestamp_ms";
    private static final String KEY_AUTO_OPTIMIZED       = "auto_optimized";
    private static final String KEY_FPS_COUNTER_ENABLED  = "fps_counter_enabled";
    private static final String KEY_SUSTAINED_PERF       = "sustained_performance_mode";
    private static final String KEY_BIG_CORE_AFFINITY    = "big_core_affinity";
    private static final String KEY_BACKGROUND_TRIM      = "background_mem_trim";
    private static final String KEY_ULTRA_FPS_GL_FLAGS   = "ultra_fps_gl_flags";

    private static final String DEFAULT_PRESET = PerformancePreset.BALANCED.name();

    private PerformanceCenterPreferences() {}

    @NonNull
    private static SharedPreferences prefs(@NonNull Context ctx) {
        return ctx.getApplicationContext()
                  .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Active preset ──────────────────────────────────────────────────────

    @NonNull
    public static PerformancePreset getActivePreset(@NonNull Context ctx) {
        String name = prefs(ctx).getString(KEY_ACTIVE_PRESET, DEFAULT_PRESET);
        try {
            return PerformancePreset.valueOf(name);
        } catch (IllegalArgumentException e) {
            return PerformancePreset.BALANCED;
        }
    }

    public static void setActivePreset(@NonNull Context ctx, @NonNull PerformancePreset preset) {
        prefs(ctx).edit().putString(KEY_ACTIVE_PRESET, preset.name()).apply();
    }

    // ── Custom RAM override (-1 = use preset default) ──────────────────────

    public static int getCustomRamMb(@NonNull Context ctx) {
        return prefs(ctx).getInt(KEY_CUSTOM_RAM_MB, -1);
    }

    public static void setCustomRamMb(@NonNull Context ctx, int ramMb) {
        prefs(ctx).edit().putInt(KEY_CUSTOM_RAM_MB, ramMb).apply();
    }

    public static void clearCustomRamMb(@NonNull Context ctx) {
        prefs(ctx).edit().remove(KEY_CUSTOM_RAM_MB).apply();
    }

    // ── Custom resolution override (-1 = use preset default) ──────────────

    public static int getCustomResolutionScale(@NonNull Context ctx) {
        return prefs(ctx).getInt(KEY_CUSTOM_RESOLUTION, -1);
    }

    public static void setCustomResolutionScale(@NonNull Context ctx, int scalePct) {
        prefs(ctx).edit().putInt(KEY_CUSTOM_RESOLUTION, scalePct).apply();
    }

    public static void clearCustomResolutionScale(@NonNull Context ctx) {
        prefs(ctx).edit().remove(KEY_CUSTOM_RESOLUTION).apply();
    }

    // ── Benchmark ─────────────────────────────────────────────────────────

    /** Returns last benchmark score, or 0 if no benchmark has been run. */
    public static int getBenchmarkScore(@NonNull Context ctx) {
        return prefs(ctx).getInt(KEY_BENCHMARK_SCORE, 0);
    }

    public static long getBenchmarkTimestampMs(@NonNull Context ctx) {
        return prefs(ctx).getLong(KEY_BENCHMARK_TIMESTAMP, 0L);
    }

    public static void saveBenchmarkResult(@NonNull Context ctx, int score) {
        prefs(ctx).edit()
                  .putInt(KEY_BENCHMARK_SCORE, score)
                  .putLong(KEY_BENCHMARK_TIMESTAMP, System.currentTimeMillis())
                  .apply();
    }

    // ── Auto-optimize ─────────────────────────────────────────────────────

    public static boolean isAutoOptimized(@NonNull Context ctx) {
        return prefs(ctx).getBoolean(KEY_AUTO_OPTIMIZED, false);
    }

    public static void setAutoOptimized(@NonNull Context ctx, boolean value) {
        prefs(ctx).edit().putBoolean(KEY_AUTO_OPTIMIZED, value).apply();
    }

    // ── Toggles ───────────────────────────────────────────────────────────

    public static boolean isFpsCounterEnabled(@NonNull Context ctx) {
        return prefs(ctx).getBoolean(KEY_FPS_COUNTER_ENABLED, true);
    }

    public static void setFpsCounterEnabled(@NonNull Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_FPS_COUNTER_ENABLED, enabled).apply();
    }

    public static boolean isSustainedPerformanceModeEnabled(@NonNull Context ctx) {
        return prefs(ctx).getBoolean(KEY_SUSTAINED_PERF, false);
    }

    public static void setSustainedPerformanceModeEnabled(@NonNull Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_SUSTAINED_PERF, enabled).apply();
    }

    public static boolean isBigCoreAffinityEnabled(@NonNull Context ctx) {
        return prefs(ctx).getBoolean(KEY_BIG_CORE_AFFINITY, false);
    }

    public static void setBigCoreAffinityEnabled(@NonNull Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_BIG_CORE_AFFINITY, enabled).apply();
    }

    public static boolean isBackgroundMemTrimEnabled(@NonNull Context ctx) {
        return prefs(ctx).getBoolean(KEY_BACKGROUND_TRIM, true);
    }

    public static void setBackgroundMemTrimEnabled(@NonNull Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_BACKGROUND_TRIM, enabled).apply();
    }

    public static boolean isUltraFpsGlFlagsEnabled(@NonNull Context ctx) {
        return prefs(ctx).getBoolean(KEY_ULTRA_FPS_GL_FLAGS, false);
    }

    public static void setUltraFpsGlFlagsEnabled(@NonNull Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_ULTRA_FPS_GL_FLAGS, enabled).apply();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Applies a preset and syncs all derived preferences atomically. */
    public static void applyPreset(@NonNull Context ctx, @NonNull PerformancePreset preset) {
        SharedPreferences.Editor ed = prefs(ctx).edit();
        ed.putString(KEY_ACTIVE_PRESET, preset.name());
        ed.putBoolean(KEY_SUSTAINED_PERF, preset.sustainedPerformanceMode);
        ed.putBoolean(KEY_BIG_CORE_AFFINITY, preset.preferBigCores);
        // Clear custom overrides so preset values take effect
        ed.remove(KEY_CUSTOM_RAM_MB);
        ed.remove(KEY_CUSTOM_RESOLUTION);
        ed.apply();
    }
}
